package com.kyc.snap.wikinet;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.google.common.collect.ImmutableMap;

import lombok.Builder;
import lombok.Data;

public class WikinetGenerator {

    private static final Map<String, String> WIKITEXT_PREPROCESSOR_SUBSTITUTIONS = ImmutableMap.<String, String> builder()
        .put("{{", "<wtemplate>")
        .put("}}", "</wtemplate>")
        .put("[[", "<wlink>")
        .put("]]", "</wlink>")
        .put("{|", "<wtable>")
        .put("|}", "</wtable>")
        .put("/>", " />")
        .build();

    public void parseArticles(InputStream articles, PrintStream out) {
        try {
            Article.ArticleBuilder builder = Article.builder();
            for (XMLStreamReader input = XMLInputFactory.newInstance().createXMLStreamReader(articles); input.hasNext(); input.next()) {
                if (input.isStartElement()) {
                    String name = input.getLocalName();
                    if (name.equals("title"))
                        builder.title(input.getElementText());
                    else if (name.equals("text")) {
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
                        out.println(builder.build().toTsv());
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
    private static class Article {

        private String title;
        private String redirect;
        private String summary;

        String toTsv() {
            if (redirect != null)
                return title + "\tREDIRECT\t" + redirect;
            else
                return title + "\tSUMMARY\t" + summary;
        }
    }
}
