package com.kyc.snap.slack;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.hubspot.algebra.Result;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.SlackClientModule;
import com.hubspot.slack.client.SlackClientRuntimeConfig;
import com.hubspot.slack.client.SlackWebClient;
import com.hubspot.slack.client.SlackWebClient.Factory;
import com.hubspot.slack.client.methods.SlackMethods;
import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.methods.params.conversations.ConversationCreateParams;
import com.hubspot.slack.client.methods.params.conversations.ConversationInviteParams;
import com.hubspot.slack.client.methods.params.users.UserEmailParams;
import com.hubspot.slack.client.models.SlackChannel;
import com.hubspot.slack.client.models.response.ResponseMetadata;
import com.hubspot.slack.client.models.response.SlackError;
import com.hubspot.slack.client.models.response.SlackResponse;
import com.hubspot.slack.client.models.response.users.UsersInfoResponse;
import com.hubspot.slack.client.models.teams.SlackTeam;

import lombok.Data;

public class SlackApiManager {

    public static final int MAX_SLACK_CHANNEL_LENGTH = 21;

    private final SlackClient slack;

    public SlackApiManager(String token) {
        Factory clientFactory = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new SlackClientModule());
            }

        }).getInstance(SlackWebClient.Factory.class);

        slack = clientFactory.build(
            SlackClientRuntimeConfig.builder()
            .setTokenSupplier(() -> token)
            .build());
    }

    /**
     * Creates a channel for the given puzzle title and returns the channel ID.
     */
    public String createChannel(String title) {
        String channelName = StringUtils.truncate(
            "x-" + title.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim().replace(' ', '-'),
            MAX_SLACK_CHANNEL_LENGTH);
        SlackChannel channel = slack.createConversation(ConversationCreateParams.builder()
            .setName(channelName)
            .setIsPrivate(false)
            .build())
            .join()
            .unwrapOrElseThrow()
            .getChannel();
        return channel.getId();
    }

    public void inviteUsers(String channelId, List<String> emailAddresses) {
        // start async calls in parallel
        List<CompletableFuture<Result<UsersInfoResponse, SlackError>>> futures = emailAddresses.stream()
            .map(emailAddress -> slack.lookupUserByEmail(UserEmailParams.of(emailAddress)))
            .collect(Collectors.toList());

        slack.inviteToConversation(ConversationInviteParams.builder()
            .setChannelId(channelId)
            .setUsers(futures.stream()
                .map(future -> future.join().unwrapOrElseThrow().getUser().getId())
                .collect(Collectors.toList()))
            .build())
            .join();
    }

    public String getChannelLink(String channelId) {
        return String.format("https://%s.slack.com/messages/%s", getTeamDomain(), channelId);
    }

    public String getChannelId(String channelLink) {
        return channelLink.substring(channelLink.lastIndexOf('/') + 1);
    }

    public void postMessage(String channelId, String message) {
        slack.postMessage(ChatPostMessageParams.builder()
            .setChannelId(channelId)
            .setText(message)
            .build())
            .join();
    }

    private String getTeamDomain() {
        /*
         * HubSpot's API is incorrect; the second argument should be a serializable map instead of
         * an unserializable object, and the field in TeamInfoResponse should be "team", not
         * "slackTeam".
         */
        return slack.postSlackCommand(SlackMethods.team_info, ImmutableMap.of(), TeamInfoResponse.class)
            .join()
            .unwrapOrElseThrow()
            .getTeam()
            .getDomain();
    }

    @Data
    private static class TeamInfoResponse implements SlackResponse {

        final boolean ok;
        final SlackTeam team;
        final Optional<ResponseMetadata> responseMetadata;
    }
}
