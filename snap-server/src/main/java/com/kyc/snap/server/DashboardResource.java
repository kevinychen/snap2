package com.kyc.snap.server;

import java.util.List;

import javax.ws.rs.BadRequestException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.kyc.snap.api.DashboardService;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.slack.SlackApiManager;

import lombok.Data;

@Data
public class DashboardResource implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardResource.class);

    private final GoogleAPIManager googleApi;

    @Override
    public GenerateSheetResponse generateSheet(String dashboardId, GenerateSheetRequest request) {
        List<String> parents = googleApi.getParents(dashboardId);
        if (parents.size() != 1)
            throw new BadRequestException("Dashboard doesn't have exactly one parent: " + parents);

        String title = request.getTitle();
        if (title.isEmpty()) {
            try {
                Document doc = Jsoup.connect(request.getPuzzleUrl())
                    .get();
                title = doc.head().getElementsByTag("title").get(0).text();
            } catch (Exception e) {
                throw new BadRequestException("Error fetching title given puzzle URL: " + request.getPuzzleUrl(), e);
            }
        }

        String sheetUrl = googleApi.createSheet(title, Iterables.getOnlyElement(parents)).getUrl();

        String slackLink = "";
        if (!request.getSlackToken().isEmpty()) {
            SlackApiManager slack = new SlackApiManager(request.getSlackToken());
            try {
                String channelId = slack.createChannel(title);
                slack.inviteUsers(channelId, request.getSlackEmailAddresses());
                slackLink = slack.getChannelLink(channelId);
            } catch (RuntimeException e) {
                // log error but continue with response
                log.warn("Error when creating Slack channel '{}'", title, e);
            }
        }

        return new GenerateSheetResponse(title, sheetUrl, slackLink);
    }

    @Override
    public boolean solvePuzzle(String dashboardId, SolvePuzzleRequest request) {
        String newName = String.format("[SOLVED: %s] %s", request.getAnswer(), request.getTitle());
        googleApi.rename(request.getSheetId(), newName);

        if (!request.getSlackToken().isEmpty()) {
            SlackApiManager slack = new SlackApiManager(request.getSlackToken());
            slack.postMessage(
                slack.getChannelId(request.getSlackLink()),
                "Solved! The answer is " + request.getAnswer());
        }

        return true;
    }
}
