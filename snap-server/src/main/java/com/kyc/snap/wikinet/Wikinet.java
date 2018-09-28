package com.kyc.snap.wikinet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import lombok.Builder;
import lombok.Data;

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
     *     enwiki-latest-pages-articles1.xml-p10p30302.bz2 (before decompressing)
     *     enwiki-latest-pages-articles1.xml-p10p30302 (after decompressing)
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

    private final File downloadDir;
    private final File partitionsDir;

    public Wikinet() {
        downloadDir = new File(WIKINET_BASE_DIR, "download");
        partitionsDir = new File(WIKINET_BASE_DIR, "partitions");
    }

    /**
     * Downloads a bz2 file to {@link #downloadDir}. The argument must be a link returned by
     * {@link #getArticlesLinks()}.
     */
    public void download(String articlesLink) throws IOException {
        downloadDir.mkdirs();
        FileUtils.copyURLToFile(new URL(WIKIDUMP_URL + articlesLink), new File(downloadDir, articlesLink));
    }

    /**
     * Decompresses a downloaded bz2 file into a Wikitext file. The {@link #download(String)} method
     * must have already been called for this argument. This function removes the original bz2 file.
     */
    public void decompress(String articlesLink) throws InterruptedException, IOException {
        String[] cmdarray = { "bunzip2", new File(downloadDir, articlesLink).getAbsolutePath() };
        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    /**
     * Parses the Wikitext file into a list of articles, and indexes them into different partition
     * files. The {@link #decompress(String)} method must already have been called for this
     * argument. Multiple calls to this method make only additive changes to the partition files;
     * use the {@link #resetNet()} method to remove them.
     */
    public void createNet(String articlesLink) throws IOException, XMLStreamException {
        String decompressedName = articlesLink.replace(".bz2", "");

        Multimap<Integer, Article> articles = ArrayListMultimap.create();
        parseArticles(new FileInputStream(new File(downloadDir, decompressedName)),
            article -> articles.put(Math.abs(article.title.toUpperCase().hashCode()) % NUM_PARTITIONS, article));

        partitionsDir.mkdirs();
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            File file = new File(partitionsDir, String.format("%04x", i));
            try (FileWriter fileWriter = new FileWriter(file, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    PrintWriter out = new PrintWriter(bufferedWriter)) {
                for (Article article : articles.get(i))
                    out.println(article.toTsv());
            }
        }
    }

    /**
     * Removes all partition files.
     */
    public void resetNet() throws IOException {
        FileUtils.deleteDirectory(partitionsDir);
    }

    /**
     * Returns a list of all articles with the given name (not case sensitive).
     */
    public List<Article> find(String title) throws IOException {
        int hash = Math.abs(title.toUpperCase().hashCode()) % NUM_PARTITIONS;
        return Files.lines(new File(partitionsDir, String.format("%04x", hash)).toPath())
                .filter(line -> startsWithIgnoreCase(line, title + "\t"))
                .map(Article::fromTsv)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of links to all bz2 files.
     */
    public static List<String> getArticlesLinks() throws IOException {
        Document doc = Jsoup.parse(new URL(WIKIDUMP_URL), 5000);
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
        for (XMLStreamReader input = XMLInputFactory.newInstance().createXMLStreamReader(articles); input.hasNext(); input.next()) {
            if (input.isStartElement()) {
                String name = input.getLocalName();
                if (name.equals("title")) {
                    String text = input.getElementText();
                    // ignore disambiguating references in titles, e.g. "Animalia (book)" -> "Animalia"
                    int disambiguationStart = text.indexOf(" (");
                    if (disambiguationStart != -1)
                        text = text.substring(0, disambiguationStart);
                    builder.title = text;
                } else if (name.equals("text")) {
                    String text = input.getElementText();
                    /*
                     * Text for redirect pages start with "#REDIRECT" and are case insensitive
                     * (https://en.wikipedia.org/wiki/Wikipedia:Redirect). Take the redirect title that follows in square brackets, e.g.
                     * "#REDIRECT [[Amoeba]]" -> "Amoeba".
                     */
                    if (startsWithIgnoreCase(text, "#REDIRECT")) {
                        int redirectStart = text.indexOf("[[");
                        if (redirectStart != -1)
                            builder.redirect = text.substring(redirectStart + "[[".length(), text.indexOf("]]", redirectStart));
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
                    processor.accept(builder.build());
                    builder = Article.builder();
                }
            }
        }
    }

    private static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.length() >= prefix.length() && str.substring(0, prefix.length()).equalsIgnoreCase(prefix);
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
                    /*
                     * Italic and bold formatting is denoted by two or more single quotes (https://www.mediawiki.org/wiki/Help:Formatting)
                     * but should be treated identically to normal text by Wikinet.
                     */
                    else if (wholeText.startsWith("''", index))
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
        private String summary = "";

        /**
         * A more human-friendly way of serializing the Article object in Wikinet's index files.
         */
        public String toTsv() {
            if (redirect != null)
                return title + "\tREDIRECT\t" + redirect;
            else
                return title + "\tSUMMARY\t" + summary;
        }

        public static Article fromTsv(String tsv) {
            StringTokenizer tokenizer = new StringTokenizer(tsv, "\t");
            ArticleBuilder builder = Article.builder();
            builder.title = tokenizer.nextToken();
            if (tokenizer.nextToken().equals("REDIRECT"))
                builder.redirect = tokenizer.nextToken();
            else
                builder.summary = tokenizer.nextToken();
            return builder.build();
        }
    }

    public static void main(String[] args) throws Exception {
        long time = System.currentTimeMillis();
        Wikinet wikinet = new Wikinet();
        wikinet.resetNet();
        for (String articleLink : Wikinet.getArticlesLinks()) {
            wikinet.download(articleLink);
            wikinet.decompress(articleLink);
            wikinet.createNet(articleLink);
        }
        for (Article article : wikinet.find("Anarchism"))
            System.out.println(article.toTsv());
        System.out.println(System.currentTimeMillis() - time);
    }
}
