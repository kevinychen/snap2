package com.kyc.snap.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public abstract class Scraper {

    private ScrapeOperation[] operations;
    private int operationIndex;
    private List<Object> currentOutput;
    private List<List<Object>> output;

    protected synchronized List<List<Object>> scrape(String rootUrl, ScrapeOperation... operations) {
        this.operations = operations;
        this.operationIndex = 0;
        this.currentOutput = new ArrayList<>();
        this.output = new ArrayList<>();
        scrapeHelper(getRootElement(rootUrl));
        return output;
    }

    protected final ScrapeOperation select(String selector) {
        return el -> {
            for (Element child : el.select(selector))
                process(child);
        };
    }

    protected final ScrapeOperation emit(Function<Element, Object> emitFunction) {
        return el -> {
            currentOutput.add(emitFunction.apply(el));
            process(el);
            currentOutput.remove(currentOutput.size() - 1);
        };
    }

    protected final ScrapeOperation emitAll(Function<Element, List<Object>> emitFunction) {
        return el -> {
            int prevSize = currentOutput.size();
            currentOutput.addAll(emitFunction.apply(el));
            process(el);
            currentOutput.subList(prevSize, currentOutput.size()).clear();
        };
    }

    protected final ScrapeOperation followLink() {
        return el -> {
            process(getRootElement(el.attr("abs:href")));
        };
    }

    protected final void process(Element el) {
        operationIndex++;
        scrapeHelper(el);
        operationIndex--;
    }

    protected final void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final Element getRootElement(String url) {
        if (url == null)
            return null;
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final void scrapeHelper(Element el) {
        if (operationIndex == operations.length) {
            output.add(new ArrayList<>(currentOutput));
            return;
        }
        operations[operationIndex].scrape(el);
    }

    @FunctionalInterface
    protected interface ScrapeOperation {

        void scrape(Element el);
    }
}
