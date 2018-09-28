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

public class Wikinet {

    private static final String WIKIDUMP_URL = "https://dumps.wikimedia.org/enwiki/latest/";
    private static final int NUM_PARTITIONS = 256;
    private static final Map<String, String> WIKITEXT_PREPROCESSOR_SUBSTITUTIONS = ImmutableMap.<String, String> builder()
        .put("{{", "<wtemplate>")
        .put("}}", "</wtemplate>")
        .put("[[", "<wlink>")
        .put("]]", "</wlink>")
        .put("{|", "<wtable>")
        .put("|}", "</wtable>")
        .put("/>", " />")
        .build();

    private final File downloadDir;
    private final File indexDir;

    public Wikinet(File baseDir) {
        downloadDir = new File(baseDir, "download");
        indexDir = new File(baseDir, "index");
    }

    public void download(String articleLink) throws IOException {
        downloadDir.mkdirs();
        FileUtils.copyURLToFile(new URL(WIKIDUMP_URL + articleLink), new File(downloadDir, articleLink));
    }

    public void decompress(String articleLink) throws InterruptedException, IOException {
        String[] cmdarray = { "bunzip2", new File(downloadDir, articleLink).getAbsolutePath() };
        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    public void createNet(String articleLink) throws IOException {
        String decompressedName = articleLink.replace(".bz2", "");

        Multimap<Integer, Article> articles = ArrayListMultimap.create();
        parseArticles(new FileInputStream(new File(downloadDir, decompressedName)),
            article -> articles.put(Math.abs(article.title.hashCode()) % NUM_PARTITIONS, article));

        indexDir.mkdirs();
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            File file = new File(indexDir, String.format("%04x", i));
            try (FileWriter fileWriter = new FileWriter(file, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    PrintWriter out = new PrintWriter(bufferedWriter)) {
                for (Article article : articles.get(i))
                    out.println(article.toTsv());
            }
        }
    }

    public List<Article> find(String title) throws IOException {
        int hash = Math.abs(title.hashCode()) % NUM_PARTITIONS;
        return Files.lines(new File(indexDir, String.format("%04x", hash)).toPath())
                .filter(line -> line.startsWith(title + "\t"))
                .map(Article::fromTsv)
                .collect(Collectors.toList());
    }

    public static List<String> getArticleLinks() throws IOException {
        Document doc = Jsoup.parse(new URL(WIKIDUMP_URL), 5000);
        return doc.getElementsByTag("a").stream()
                .map(el -> el.attr("href"))
                .filter(href -> href.matches("enwiki-latest-pages-articles\\d.*bz2"))
                .collect(Collectors.toList());
    }

    public static void parseArticles(InputStream articles, Consumer<Article> processor) {
        try {
            Article.ArticleBuilder builder = Article.builder();
            for (XMLStreamReader input = XMLInputFactory.newInstance().createXMLStreamReader(articles); input.hasNext(); input.next()) {
                if (input.isStartElement()) {
                    String name = input.getLocalName();
                    if (name.equals("title")) {
                        String text = input.getElementText();
                        int disambiguationStart = text.indexOf(" (");
                        if (disambiguationStart != -1)
                            builder.title = text.substring(0, disambiguationStart);
                        else
                            builder.title = text;
                    } else if (name.equals("text")) {
                        String text = input.getElementText();
                        if (isRedirect(text)) {
                            int redirectStart = text.indexOf("[[");
                            if (redirectStart != -1)
                                builder.redirect = text.substring(redirectStart + "[[".length(), text.indexOf("]]", redirectStart));
                        } else {
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
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isRedirect(String html) {
        return html.length() >= "#REDIRECT".length() && html.substring(0, "#REDIRECT".length()).equalsIgnoreCase("#REDIRECT");
    }

    private static boolean processNode(Node node, StringBuilder summary) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode) {
                String wholeText = ((TextNode) child).getWholeText();
                for (int index = 0; index < wholeText.length(); index++) {
                    if ((wholeText.startsWith("\n\n", index) || wholeText.startsWith("\n*", index - 1)) && summary.length() > 0)
                        return true;
                    else if (wholeText.startsWith("''", index))
                        while (wholeText.startsWith("'", index + 1))
                            index++;
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
                        if (!link.startsWith("File:") && !link.startsWith("Image:"))
                            summary.append(link.substring(link.lastIndexOf("|") + 1));
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
        Wikinet wikinet = new Wikinet(new File("data/wikinet"));
        for (String articleLink : Wikinet.getArticleLinks()) {
            wikinet.download(articleLink);
            wikinet.decompress(articleLink);
            wikinet.createNet(articleLink);
        }
        for (Article article : wikinet.find("Anarchism"))
            System.out.println(article.toTsv());
        System.out.println(System.currentTimeMillis() - time);
    }
}
