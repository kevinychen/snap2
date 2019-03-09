package com.kyc.snap.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ScraperTest extends Scraper {

    @Test
    public void testScrapeConstellations() {
        List<List<Object>> result = scrape("https://en.wikipedia.org/wiki/IAU_designated_constellations",
            select("#mw-content-text > div > table.wikitable.sortable > tbody > tr"),
            select("td:nth-child(1) > a"),
            emit(el -> el.text()),
            followLink(),
            select("#mw-content-text > div > table > tbody"),
            el -> {
                for (Element child : el.getElementsByTag("tr")) {
                    Elements th = child.getElementsByTag("th");
                    if (!th.isEmpty() && th.get(0).text().contains("Bordering"))
                        process(child);
                }
            },
            select("td a[title]"),
            emit(el -> el.text()));

        for (List<Object> row : result) {
            assertThat(row).hasSize(2);
            assertThat(result.contains(ImmutableList.of(row.get(1), row.get(0))));
        }
        assertThat(result.stream()
            .map(row -> row.get(0))
            .collect(Collectors.toSet())).hasSize(88);
        assertThat(result).contains(ImmutableList.of("Andromeda", "Perseus"));
        assertThat(result).contains(ImmutableList.of("Vulpecula", "Pegasus"));
        assertThat(result).doesNotContain(ImmutableList.of("Andromeda", "Vulpecula"));
    }
}
