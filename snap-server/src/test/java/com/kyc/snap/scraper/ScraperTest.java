
package com.kyc.snap.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.junit.Test;

/**
 * Tips for writing a scraper.
 *
 * Ensure that Chrome is running with the remote debugging port on:
 *
 * <pre>
 * "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --remote-debugging-port=9222
 * </pre>
 *
 * To get a selector: right click on an element, click Inspect, right click again, and click "Copy
 * as selector".
 */
public class ScraperTest extends Scraper {

    @Test
    public void test() {
        newTab("https://www.faa.gov/air_traffic/flight_info/aeronav/aero_data/Loc_ID_Search/Fixes_Waypoints/");
        sleep(1000);
        js("$('select').val(1000)");
        Set<String> ids = new HashSet<>();
        while (true) {
            Set<String> newIds = new HashSet<>(ids);
            for (Element el : html().select("div > main > article > div > #contentTable > tbody > tr"))
                newIds.add(el.select("td:nth-child(1)").text());
            if (newIds.equals(ids))
                break;
            click("#pageBar > li.next > a");
            sleep(500);
            ids = newIds;
        }
        assertThat(ids.size()).isGreaterThan(60000);
        assertThat(ids).contains("AAALL", "LAMBY", "ZZYZX");
    }
}
