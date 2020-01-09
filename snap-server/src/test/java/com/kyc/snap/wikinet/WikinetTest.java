package com.kyc.snap.wikinet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.kyc.snap.wikinet.Wikinet.Article;

public class WikinetTest {

    private Wikinet wikinet = new Wikinet();

    @Test
    public void testDirectFind() {
        assertThat(wikinet.directFind("Anarchism", true))
            .anySatisfy(article -> article.getTitle().equals("Anarchism"));
    }

    @Test
    public void testSearch() {
        Set<Article> articles = wikinet.getTitles()
            .filter(title -> title.length() == 9 && title.toLowerCase().startsWith("an"))
            .flatMap(title -> wikinet.find(title).stream())
            .filter(article -> article.getSummary().contains("philosophy"))
            .collect(Collectors.toSet());
        assertThat(articles)
            .anySatisfy(article -> article.getTitle().equals("Anarchism"));
    }

    @Test
    public void testFuzzySearch() {
        Set<Article> articles = wikinet.getLetterOnlyTitles()
            .filter(title -> title.length() == 12 && title.startsWith("ea"))
            .flatMap(title -> wikinet.find(title).stream())
            .filter(article -> article.getSummary().contains("perfume"))
            .collect(Collectors.toSet());
        assertThat(articles)
            .anySatisfy(article -> article.getTitle().equals("Eau de Cologne"));
    }
}
