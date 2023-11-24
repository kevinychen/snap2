
package com.kyc.snap.scraper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Element;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tips for writing a scraper.
 *
 * <p>Ensure that Chrome is running with the remote debugging port on:
 *
 * <pre>
 * "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --remote-debugging-port=9222
 * </pre>
 *
 * To get a selector: right-click on an element, click Inspect, right click again, and click "Copy
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

    String getHtml() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e",
                "tell application \"Google Chrome\" to set sourceHTML to execute "
                    + "front window's active tab javascript \"document.documentElement.outerHTML\""});
            return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
