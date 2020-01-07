
package com.kyc.snap.scraper;

import java.io.IOException;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kklisura.cdt.protocol.commands.DOM;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
import com.github.kklisura.cdt.services.types.ChromeTab;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class Scraper implements AutoCloseable {

    private final ChromeService chromeService;
    private ChromeDevToolsService devToolsService;
    private DOM dom;

    public Scraper() {
        chromeService = new ChromeServiceImpl(9222);
    }

    void newTab(String url) {
        initTab(chromeService.createTab());
        navigate(url);
    }

    void useTab(String tabName) {
        for (ChromeTab tab : chromeService.getTabs()) {
            if (tab.getTitle().contains(tabName) || tab.getUrl().contains(tabName)) {
                initTab(tab);
                return;
            }
        }
        throw new RuntimeException("Tab not found: " + tabName);
    }

    void navigate(String url) {
        devToolsService.getPage().navigate(normalizeUrl(url));
    }

    Element html() {
        int documentNodeId = dom.getDocument().getNodeId();
        return Jsoup.parse(dom.getOuterHTML(documentNodeId, null, null));
    }

    Element html(String url) {
        try {
            Response response = new OkHttpClient()
                .newCall(new Request.Builder().url(normalizeUrl(url)).get().build())
                .execute();
            return Jsoup.parse(response.body().string(), url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void click(String selector) {
        js(String.format("document.querySelector('%s').click()", StringEscapeUtils.escapeEcmaScript(selector)));
    }

    Object js(String expression) {
        Evaluate result = devToolsService.getRuntime().evaluate(expression);
        if (result.getExceptionDetails() != null) {
            try {
                throw new RuntimeException(
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result.getExceptionDetails()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result.getResult().getValue();
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (devToolsService != null) {
            devToolsService.close();
        }
    }

    private void initTab(ChromeTab tab) {
        close();
        devToolsService = chromeService.createDevToolsService(tab);
        dom = devToolsService.getDOM();
    }

    private static String normalizeUrl(String url) {
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        return url;
    }
}
