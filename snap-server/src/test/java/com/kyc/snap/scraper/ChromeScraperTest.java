package com.kyc.snap.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ChromeScraperTest extends ChromeScraper {

    @Test
    public void testScrapeWaypoints() {
        List<List<Object>> result = scrape(null,
            navigate("https://www.faa.gov/air_traffic/flight_info/aeronav/aero_data/Loc_ID_Search/Fixes_Waypoints/", 2000),
            el -> {
                for (int i = 0; i < 5; i++) {
                    processCurrent();
                    click(".next", 500);
                }
            },
            select("#contentTable > tbody > tr"),
            emitAll(el -> el.getElementsByTag("td").stream()
                .map(td -> td.text())
                .collect(Collectors.toList())));

        // page 1 first row
        assertThat(result).contains(ImmutableList.of("AAALL", "MASSACHUSETTS", "42-07-12.6800N 071-08-30.3400W"));
        // page 5 last row
        assertThat(result).contains(ImmutableList.of("ACERT", "OKLAHOMA", "35-59-03.5700N 096-07-58.4300W"));
        // no results from page 6 onward
        assertThat(result).noneMatch(row -> row.get(0).toString().compareTo("ACESI") >= 0);
    }
}
