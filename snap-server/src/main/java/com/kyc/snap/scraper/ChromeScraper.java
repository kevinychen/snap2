package com.kyc.snap.scraper;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import io.github.ejif.chromej.ChromeJ;
import io.github.ejif.chromej.ConnectedTarget;
import io.github.ejif.chromej.WsTarget;
import io.github.ejif.chromej.protocol.WsProtocol;
import io.github.ejif.chromej.protocol.dom.GetDocumentRequest;
import io.github.ejif.chromej.protocol.dom.GetOuterHTMLRequest;
import io.github.ejif.chromej.protocol.dom.NodeId;
import io.github.ejif.chromej.protocol.page.NavigateRequest;
import io.github.ejif.chromej.protocol.runtime.EvaluateRequest;
import io.github.ejif.chromej.protocol.target.CloseTargetRequest;
import io.github.ejif.chromej.protocol.target.TargetID;

public class ChromeScraper extends Scraper {

    protected WsProtocol ws;

    private ChromeJ chromej = ChromeJ.create();

    @Override
    protected synchronized List<List<Object>> scrape(String rootUrl, ScrapeOperation... operations) {
        WsTarget target = chromej.getHttpProtocol().newTab();
        try (ConnectedTarget connectedTarget = ConnectedTarget.initialize(target.getWebSocketDebuggerUrl(), 10_000)) {
            ws = connectedTarget.getProtocol();
            List<List<Object>> result = super.scrape(rootUrl, operations);
            ws.getTarget().closeTarget(CloseTargetRequest.builder()
                .targetId(TargetID.of(target.getId()))
                .build());
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final ScrapeOperation navigate(String url, int waitMillis) {
        return el -> {
            ws.getPage()
                .navigate(NavigateRequest.builder()
                    .url(url)
                    .build());
            wait(waitMillis);
            process(el);
        };
    }

    protected final void processCurrent() {
        NodeId nodeId = ws.getDOM()
            .getDocument(GetDocumentRequest.builder()
                .build())
            .getRoot()
            .getNodeId();
        String outerHTML = ws.getDOM()
            .getOuterHTML(GetOuterHTMLRequest.builder()
                .nodeId(nodeId)
                .build())
            .getOuterHTML();
        process(Jsoup.parse(outerHTML));
    }

    protected final void click(String selector, int waitMillis) {
        ws.getRuntime()
            .evaluate(EvaluateRequest.builder()
                .expression(String.format("document.querySelector('%s').click()", StringEscapeUtils.escapeEcmaScript(selector)))
                .build());
        wait(waitMillis);
    }
}
