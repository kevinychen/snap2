package com.kyc.snap.wikinet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import lombok.Builder;
import lombok.Data;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

/**
 * Stores summaries for all Wikipedia articles and indexes them for quick lookup.
 * <p>
 * Wikinet parses the Wikitext entries for all Wikipedia articles and determines a short summary for
 * each article. For example,
 * <code>(Title: ANARCHISM) Anarchism is a political philosophy that advocates
 * self-governed societies based on voluntary institutions</code> (the actual parsed summary is a
 * little longer). It also indexes these summaries to support a fast lookup function
 * {@link #find(String)} that returns the summary given an article title.
 * <p>
 * For example, with a dictionary it is possible to answer the question "What 9 letter word starting
 * with A is a philosophy?" with the following code:
 *
 * <pre>
 * for (String word : dictionary)
 *     if (word.length() == 9 && word.charAt(0) == 'A'
 *             && wikinet.find(word).stream().anyMatch(article -> article.summary.contains("philosophy")))
 *         print(word);
 * </pre>
 */
public class Wikinet {

    /**
     * The base directory of Wikinet. After the Wikinet is created, the directory has the following
     * file structure:
     *
     * <pre>
     * {@link #WIKINET_BASE_DIR}
     *   download
     *     enwiki-latest-all-titles-in-ns0 (after decompressing)
     *     enwiki-latest-pages-articles1.xml-p10p30302 (after decompressing)
     *     enwiki-latest-pages-articles1.xml-p10p30302.done (signals that the corresponding file has been processed)
     *     ...
     *   index
     *     titles-cleaned (contain titles with only [a-z0-9] tokens separated by spaces)
     *     titles-letters-only (contain titles with only [a-z] characters without spaces)
     *   partitions
     *     0000 (TSV formatted partition file containing all articles with |hash value| ≡ 0 (mod {@link #NUM_PARTITIONS})
     *     0001
     *     ...
     *     ffff
     * </pre>
     */
    public static final String WIKINET_BASE_DIR = "./data/wikinet";

    /**
     * The page containing links to all Wikipedia articles in raw Wikitext format.
     */
    public static final String WIKIDUMP_URL = "https://dumps.wikimedia.org/enwiki/latest/";

    /**
     * The link pointing to a gzipped file of all Wikipedia article titles.
     */
    public static final String TITLES_LINK = "enwiki-latest-all-titles-in-ns0.gz";

    /**
     * The number of partitions of all articles. This number is large enough such that the average
     * partition size (~5GB for all Wikinets divided by 65536 partitions = ~80KB) is a number small
     * enough so that it is easy to search for any particular article by title. Currently partition
     * filenames are 4 hex digits, so this number should not exceed 16^4 = 65536.
     */
    public static final int NUM_PARTITIONS = 65536;

    /**
     * Substitutions to make in the raw Wikitext before parsing the articles.
     * <p>
     * These are mainly for convenience; Java's Jsoup HTML parser only understands HTML tags in
     * angle brackets, so we replace Wikitext templates ({{...template...}}), links ([[...link...]])
     * and tables ({|...table...|}) so that they can be parsed at the same time as the other HTML
     * tags.
     */
    public static final Map<String, String> WIKITEXT_PREPROCESSOR_SUBSTITUTIONS = ImmutableMap.<String, String> builder()
        .put("{{", "<wtemplate>")
        .put("}}", "</wtemplate>")
        .put("[[", "<wlink>")
        .put("]]", "</wlink>")
        .put("{|", "<wtable>")
        .put("|}", "</wtable>")
        /*
         * Wikitext contains tags like <ref name=NAME/> which would normally be incorrectly parsed by Jsoup as an open tag
         * <ref name="NAME/">. Adding a space before the slash allows it to be correctly parsed as the void tag <ref name="NAME" />.
         */
        .put("/>", " />")
        .build();

    private static final Logger log = LoggerFactory.getLogger(Wikinet.class);

    private final File downloadDir;
    private final File rawDir;
    private final File indexDir;
    private final File partitionsDir;

    private final File rawArticleFrequencies;

    private final File cleanedTitles;
    private final File letterOnlyTitles;
    private final File cleanedTitlesWithFrequencies;
    private final File letterOnlyTitlesWithFrequencies;

    public Wikinet() {
        downloadDir = new File(WIKINET_BASE_DIR, "download");
        rawDir = new File(WIKINET_BASE_DIR, "raw");
        indexDir = new File(WIKINET_BASE_DIR, "index");
        partitionsDir = new File(WIKINET_BASE_DIR, "partitions");

        rawArticleFrequencies = new File(rawDir, "article-frequencies");

        cleanedTitles = new File(indexDir, "titles-cleaned");
        letterOnlyTitles = new File(indexDir, "titles-letters-only");
        cleanedTitlesWithFrequencies = new File(indexDir, "titles-cleaned-with-frequencies");
        letterOnlyTitlesWithFrequencies = new File(indexDir, "titles-letters-only-with-frequencies");
    }

    /**
     * Downloads and decompresses a gz file of all normal Wikipedia article titles. The link
     * contains only articles in namespace 0, which does not include other article types such as
     * templates and files.
     */
    public void downloadTitles() throws InterruptedException, IOException {
        File downloadFile = new File(downloadDir, TITLES_LINK);
        File decompressedFile = new File(downloadDir, TITLES_LINK.replace(".gz", ""));
        if (decompressedFile.exists())
            return;

        log.info("Downloading {}...", TITLES_LINK);
        downloadDir.mkdirs();
        FileUtils.copyURLToFile(new URL(WIKIDUMP_URL + TITLES_LINK), downloadFile);

        log.info("Decompressing {}...", TITLES_LINK);
        String[] cmdarray = { "gunzip", downloadFile.getAbsolutePath() };
        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    /**
     * Downloads and decompresses a bz2 file to {@link #downloadDir}. The argument must be a link
     * returned by {@link #getArticlesLinks()}.
     */
    public void download(String articlesLink) throws InterruptedException, IOException {
        File downloadFile = new File(downloadDir, articlesLink);
        File decompressedFile = new File(downloadDir, articlesLink.replace(".bz2", ""));
        if (decompressedFile.exists())
            return;

        log.info("Downloading {}...", articlesLink);
        downloadDir.mkdirs();
        FileUtils.copyURLToFile(new URL(WIKIDUMP_URL + articlesLink), downloadFile);

        log.info("Decompressing {}...", articlesLink);
        String[] cmdarray = { "bunzip2", downloadFile.getAbsolutePath() };
        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    /**
     * Parses the file containing a list of Wikipedia article titles and outputs two files; one with
     * each title split into words and one with only letters retained. This method requires that
     * {@link #downloadTitles()} has already been called.
     * <p>
     * For example, for the title "Korea_K-Pop_Hot_100", the cleaned title is "korea k pop hot 100"
     * and the letter-only title is "koreakpophot".
     */
    public void processTitles() throws IOException {
        File decompressedFile = new File(downloadDir, TITLES_LINK.replace(".gz", ""));
        if (letterOnlyTitles.exists())
            return;

        log.info("Writing cleaned article titles to {}...", cleanedTitles.getName());
        indexDir.mkdirs();
        Files.write(cleanedTitles.toPath(), new TreeSet<>(Files.lines(decompressedFile.toPath())
            .map(title -> StringUtils.stripAccents(stripFrom(title, "_(")).toLowerCase().replaceAll("[^a-z0-9]+", " ").trim())
            .collect(Collectors.toList())), StandardCharsets.UTF_8);
        log.info("Writing article titles with only letters retained to {}...", letterOnlyTitles.getName());
        Files.write(letterOnlyTitles.toPath(), new TreeSet<>(getCleanedTitles()
            .map(title -> title.replaceAll("[^a-z]", ""))
            .collect(Collectors.toList())), StandardCharsets.UTF_8);
    }

    /**
     * Parses the Wikitext file into a list of articles, and indexes them into different partition
     * files. The {@link #download(String)} method must already have been called for this
     * argument. Multiple calls to this method make only additive changes to the partition files;
     * use the {@link #resetNet()} method to remove them.
     */
    public void createNet(String articlesLink) throws IOException, XMLStreamException {
        File decompressedFile = new File(downloadDir, articlesLink.replace(".bz2", ""));
        File completionFile = new File(downloadDir, decompressedFile.getName() + ".done");
        if (completionFile.exists())
            return;

        log.info("Processing Wikitext file {}...", decompressedFile.getName());
        Multimap<Integer, Article> articles = ArrayListMultimap.create();
        parseArticles(new FileInputStream(decompressedFile), article -> {
            String normalizedTitle = normalize(article.title);
            if (!normalizedTitle.isEmpty())
                articles.put(Math.abs(normalizedTitle.hashCode()) % NUM_PARTITIONS, article);
        });

        log.info("Writing to raw article frequencies file...");
        rawDir.mkdirs();
        try (FileWriter fileWriter = new FileWriter(rawArticleFrequencies, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    PrintWriter out = new PrintWriter(bufferedWriter)) {
            for (Article article : articles.values())
                out.println(article.toTsv1());
        }

        log.info("Writing to partition files...");
        partitionsDir.mkdirs();
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            File file = new File(partitionsDir, String.format("%04x", i));
            try (FileWriter fileWriter = new FileWriter(file, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    PrintWriter out = new PrintWriter(bufferedWriter)) {
                for (Article article : articles.get(i))
                    out.println(article.toTsv2());
            }
        }
        completionFile.createNewFile();
        log.info("Wikinet created and marker file {} written.", completionFile.getName());
    }

    /**
     * Constructs a file containing each article title and its frequency (estimated by the article
     * length). Also constructs a file for article titles with all non-alphabetic characters
     * removed. This method requires that {@link #processTitles()} and {@link #createNet(String)}
     * have been called.
     */
    public void buildTitleFrequencies() throws IOException {
        if (letterOnlyTitlesWithFrequencies.exists())
            return;

        log.info("Preparing to write frequency files...");
        List<Article> articles = new ArrayList<>();
        try (FileReader fileReader = new FileReader(rawArticleFrequencies);
                BufferedReader in = new BufferedReader(fileReader)) {
            while (true) {
                String line = in.readLine();
                if (line == null)
                    break;
                articles.add(Article.fromTsv1(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("blah");
        Map<String, Integer> frequencyMap = articles.stream()
            .filter(article -> article.redirect == null)
            .collect(Collectors.toMap(Article::getTitle, Article::getFrequency, Math::max));
        for (Article article : articles)
            if (frequencyMap.containsKey(article.redirect)) {
                int frequency = frequencyMap.get(article.redirect);
                if (!frequencyMap.containsKey(article.title) || frequencyMap.get(article.title) < frequency)
                    frequencyMap.put(article.title, frequency);
            }

        log.info("Writing cleaned article titles with frequencies to {}...", cleanedTitlesWithFrequencies.getName());
        Files.write(cleanedTitlesWithFrequencies.toPath(), getCleanedTitles()
            .filter(title -> frequencyMap.containsKey(normalize(title)))
            .map(title -> title + "\t" + frequencyMap.get(normalize(title)))
            .collect(Collectors.toList()), StandardCharsets.UTF_8);
        log.info("Writing article titles with only letters retained with frequencies to {}...", letterOnlyTitlesWithFrequencies.getName());
        Files.write(letterOnlyTitlesWithFrequencies.toPath(), getLetterOnlyTitles()
            .filter(title -> frequencyMap.containsKey(title))
            .map(title -> title + "\t" + frequencyMap.get(title))
            .collect(Collectors.toList()), StandardCharsets.UTF_8);
    }

    /**
     * Removes all partition files.
     */
    public void resetNet() throws IOException {
        log.info("Deleting Wikinet");
        for (File completionFile : downloadDir.listFiles((file, name) -> name.endsWith(".done")))
            completionFile.delete();
        FileUtils.deleteDirectory(partitionsDir);
    }

    /**
     * Returns a stream of all Wikipedia titles as is.
     */
    public Stream<String> getTitles() {
        File decompressedFile = new File(downloadDir, TITLES_LINK.replace(".gz", ""));
        try {
            return Files.lines(decompressedFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a stream of all Wikipedia titles, with only lowercase letters and digits in words
     * separated by spaces.
     */
    public Stream<String> getCleanedTitles() {
        try {
            return Files.lines(cleanedTitles.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a stream of all Wikipedia titles, with only lowercase letters (not separated by
     * spaces).
     */
    public Stream<String> getLetterOnlyTitles() {
        try {
            return Files.lines(letterOnlyTitles.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns cleaned titles, mapped to a number correlated to its frequency/popularity. A
     * reasonable rule of thumb is that common titles have a frequency of over 10,000.
     */
    public EntryStream<String, Integer> getCleanedTitlesWithFrequencies() {
        try {
            return StreamEx.ofLines(cleanedTitlesWithFrequencies.toPath())
                    .map(line -> line.split("\t"))
                    .mapToEntry(split -> split[0], split -> Integer.parseInt(split[1]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns letter only titles, mapped to a number correlated to its frequency/popularity. A
     * reasonable rule of thumb is that common titles have a frequency of over 10,000.
     */
    public EntryStream<String, Integer> getLetterOnlyTitlesWithFrequencies() {
        try {
            return StreamEx.ofLines(letterOnlyTitlesWithFrequencies.toPath())
                    .map(line -> line.split("\t"))
                    .mapToEntry(split -> split[0], split -> Integer.parseInt(split[1]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a set of all articles with the given name (not case sensitive).
     */
    public Set<Article> find(String title, boolean exact) {
        Set<Article> articles = new HashSet<>();
        for (Article article : directFind(title, exact)) {
            if (article.redirect != null) {
                // follow redirect ignoring anchors, e.g. (Title: FOLD) #REDIRECT [[Betting in poker#Fold]] -> "Betting in power"
                Set<Article> redirectArticles = directFind(stripFrom(article.redirect, "#"), true);
                if (redirectArticles.isEmpty())
                    break;
                article = redirectArticles.iterator().next();
            }
            if (article.summary != null)
                articles.add(article);
        }
        return articles;
    }

    /**
     * Returns a set of all articles with the given name, where redirects are not resolved (i.e.
     * redirect articles have summary = null).
     *
     * @param exact
     *            if true, only articles with the exact case are returned; otherwise a case
     *            insensitive search is performed
     */
    public Set<Article> directFind(String title, boolean exact) {
        String normalizedTitle = normalize(title);
        int hash = Math.abs(normalizedTitle.hashCode()) % NUM_PARTITIONS;
        try (FileReader fileReader = new FileReader(new File(partitionsDir, String.format("%04x", hash)));
                BufferedReader in = new BufferedReader(fileReader)) {
            String prefix = normalizedTitle + "\t";
            Set<Article> articles = new HashSet<>();
            while (true) {
                String line = in.readLine();
                if (line == null)
                    break;
                else if (line.startsWith(prefix)) {
                    Article article = Article.fromTsv2(line);
                    if (!exact || article.title.equals(title))
                        articles.add(article);
                }
            }
            return articles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of links to all bz2 files.
     */
    public static List<String> getArticlesLinks() throws IOException {
        log.info("Fetching articles links...");
        Document doc = Jsoup.connect(WIKIDUMP_URL)
            .validateTLSCertificates(false)
            .get();
        return doc.getElementsByTag("a").stream()
            .map(el -> el.attr("href"))
            .filter(href -> href.matches("enwiki-latest-pages-articles\\d.*bz2"))
            .collect(Collectors.toList());
    }

    /**
     * Helper method to parse Wikitext content as a stream and output to an arbitrary article
     * consumer. Normal users should use {@link #createNet(String)} instead.
     * <p>
     * A Wikitext file contains one or more pages, in XML, each of which looks like:
     *
     * <pre>
     * &lt;page&gt;
     *     &lt;title&gt;Anarchism&lt;/title&gt;
     *     &lt;ns&gt;0&lt;/ns&gt;
     *     &lt;revision&gt;
     *         &lt;text xml:space="preserve"&gt;content&lt;/text&gt;
     *     &lt;/revision&gt;
     * &lt;/page&gt;
     * </pre>
     *
     * The files fetched by {@link #download(String)} only contain one revision (the latest) per
     * page. The content is an HTML encoded string in Wikitext format representing the contents of
     * the page.
     */
    public static void parseArticles(InputStream articles, Consumer<Article> processor) throws XMLStreamException {
        Article.ArticleBuilder builder = Article.builder();
        String namespace = "0";
        for (XMLStreamReader input = XMLInputFactory.newInstance().createXMLStreamReader(articles); input.hasNext(); input.next()) {
            if (input.isStartElement()) {
                String name = input.getLocalName();
                if (name.equals("title")) {
                    builder.title = input.getElementText();
                } else if (name.equals("ns")) {
                    namespace = input.getElementText();
                } else if (name.equals("text")) {
                    String text = input.getElementText();
                    builder.frequency = text.length();
                    if (!namespace.equals("0"))
                        continue;
                    /*
                     * Text for redirect pages start with "#REDIRECT" and are case insensitive
                     * (https://en.wikipedia.org/wiki/Wikipedia:Redirect). Take the redirect title that follows in square brackets, e.g.
                     * "#REDIRECT [[Amoeba]]" -> "Amoeba".
                     */
                    else if (startsWithIgnoreCase(text, "#REDIRECT")) {
                        int redirectStart = text.indexOf("[[");
                        int redirectEnd = text.indexOf("]]", redirectStart);
                        // check for corrupted redirect texts, e.g. "#redirect [[Wikipedia:Cleanup"
                        if (redirectStart != -1 && redirectEnd != -1)
                            builder.redirect = text.substring(redirectStart + "[[".length(), redirectEnd);
                    } else {
                        /*
                         * unescapeHtml4 is slow, and we usually need to parse only the beginning of the article to find the summary text.
                         * Start by parsing only 4096 characters, then repeatedly increase the limit until the entire summary text is
                         * found. This resulted in a roughly 5x improvement.
                         */
                        int limit = 4096;
                        while (true) {
                            String html = StringEscapeUtils.unescapeHtml4(StringUtils.abbreviate(text, limit));
                            for (String wikitext : WIKITEXT_PREPROCESSOR_SUBSTITUTIONS.keySet())
                                html = html.replace(wikitext, WIKITEXT_PREPROCESSOR_SUBSTITUTIONS.get(wikitext));
                            StringBuilder summary = new StringBuilder();
                            boolean done = processNode(Jsoup.parseBodyFragment(html).body(), summary);
                            if (limit < text.length() && !done)
                                limit *= 2;
                            else {
                                builder.summary = summary.toString();
                                break;
                            }
                        }
                    }
                }
            } else if (input.isEndElement()) {
                if (input.getLocalName().equals("page")) {
                    if (namespace.equals("0"))
                        processor.accept(builder.build());
                    builder = Article.builder();
                }
            }
        }
    }

    private static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.length() >= prefix.length() && str.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    private static String normalize(String title) {
        return StringUtils.stripAccents(stripFrom(title, " (")).toLowerCase().replaceAll("[^a-z]", "");
    }

    private static String stripFrom(String str, String mark) {
        int index = str.indexOf(mark);
        if (index != -1)
            str = str.substring(0, index);
        return str;
    }

    /**
     * Given a node representing the Wikitext HTML, finds the summary text by taking all the content
     * up to the first paragraph block (marked by two consecutive newlines), and ignoring extraneous
     * elements such as templates, tables, and comments.
     *
     * @return whether the entire summary text has been found.
     */
    private static boolean processNode(Node node, StringBuilder summary) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode) {
                String wholeText = ((TextNode) child).getWholeText();
                for (int index = 0; index < wholeText.length(); index++) {
                    if (wholeText.startsWith("\n\n", index) && summary.length() > 0)
                        return true;
                    /*
                     * List items start with an asterisk (*) on a newline (https://en.wikipedia.org/wiki/Help:List) and are all part of one
                     * block, so stop immediately instead of adding all of them.
                     */
                    else if (wholeText.startsWith("\n*", index - 1))
                        return true;
                    // ignore indented lines (starting with a colon): https://en.wikibooks.org/wiki/Editing_Wikitext/Indents_and_Lists
                    else if (wholeText.startsWith("\n:", index - 1)) {
                        index = wholeText.indexOf("\n", index);
                        if (index == -1)
                            break;
                    /*
                     * Italic and bold formatting is denoted by two or more single quotes (https://www.mediawiki.org/wiki/Help:Formatting)
                     * but should be treated identically to normal text by Wikinet.
                     */
                    } else if (wholeText.startsWith("''", index))
                        while (wholeText.startsWith("'", index + 1))
                            index++;
                    /*
                     * "__NOTOC__" is a "no context" template and should be ignored by Wikinet instead of treated as the first block.
                     * https://en.wikipedia.org/wiki/Template:NOTOC
                     */
                    else if (wholeText.startsWith("__NOTOC__", index))
                        index += "__NOTOC__".length() - 1;
                    else {
                        char c = wholeText.charAt(index);
                        if (!Character.isWhitespace(c))
                            summary.append(c);
                        else if (summary.length() > 0)
                            summary.append(' ');
                    }
                }
            } else if (child instanceof Element) {
                Element el = (Element) child;
                switch (el.tagName().toLowerCase()) {
                    case "wlink":
                        String link = el.text();
                        /*
                         * Links starting with "File:" (or the legacy "Image:" prefix) represent media files
                         * (https://en.wikipedia.org/wiki/Help:Files) that are ignored by Wikinet when searching for summary text.
                         */
                        if (!link.startsWith("File:") && !link.startsWith("Image:"))
                            /*
                             * Links containing a pipe render only as the text after the pipe character (see
                             * https://en.wikipedia.org/wiki/Help:Wikitext#Free_links).
                             */
                            summary.append(link.substring(link.lastIndexOf("|") + 1));
                        break;
                    case "big":
                    case "center":
                    case "i":
                    case "small":
                        if (processNode(el, summary))
                            return true;
                        break;
                    default:
                        // templates, tables, and other HTML tags are ignored by Wikinet
                        break;
                }
            }
        }
        return false;
    }

    @Data
    @Builder
    public static class Article {

        private String title;
        private String redirect;
        private String summary;
        private int frequency;

        public String toTsv1() {
            if (redirect != null)
                return normalize(title) + "\t" + "\tREDIRECT\t" + redirect;
            else
                return normalize(title) + "\t" + "\tFREQ\t" + frequency;
        }

        public static Article fromTsv1(String tsv) {
            StringTokenizer tokenizer = new StringTokenizer(tsv, "\t");
            ArticleBuilder builder = Article.builder();
            builder.title = tokenizer.nextToken();
            boolean isRedirect = tokenizer.nextToken().equals("REDIRECT");
            String msg = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
            if (isRedirect)
                builder.redirect = msg;
            else
                builder.frequency = Integer.parseInt(msg);
            return builder.build();
        }

        /**
         * A more human-friendly way of serializing the Article object in Wikinet's index files. The
         * normalized title (only lowercase characters) is prepended at the beginning for quick
         * filtering by title with a startsWith check without having to deserialize the article.
         */
        public String toTsv2() {
            if (redirect != null)
                return normalize(title) + "\t" + title + "\tREDIRECT\t" + redirect;
            else
                return normalize(title) + "\t" + title + "\tSUMMARY\t" + summary;
        }

        public static Article fromTsv2(String tsv) {
            StringTokenizer tokenizer = new StringTokenizer(tsv, "\t");
            tokenizer.nextToken();
            ArticleBuilder builder = Article.builder();
            builder.title = tokenizer.nextToken();
            boolean isRedirect = tokenizer.nextToken().equals("REDIRECT");
            String msg = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
            if (isRedirect)
                builder.redirect = msg;
            else
                builder.summary = msg;
            return builder.build();
        }
    }

    public static void main(String[] args) throws Exception {
        Wikinet wikinet = new Wikinet();
        wikinet.downloadTitles();
        wikinet.processTitles();
        for (String articleLink : Wikinet.getArticlesLinks()) {
            wikinet.download(articleLink);
            wikinet.createNet(articleLink);
        }
        wikinet.buildTitleFrequencies();
    }
}
