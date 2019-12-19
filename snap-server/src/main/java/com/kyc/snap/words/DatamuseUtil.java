package com.kyc.snap.words;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import feign.Feign;
import feign.QueryMap;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import lombok.Data;

public class DatamuseUtil {

    public static List<WordResult> getCommonWordsAfter(String word) {
        return getCommonWordsAfter(word, "");
    }

    public static List<WordResult> getCommonWordsAfter(String word, String pattern) {
        DatamuseService datamuse = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(DatamuseService.class, "https://api.datamuse.com");
        return datamuse.getWords(ImmutableMap.of("lc", word));
    }

    public static List<WordResult> getCommonWordsBefore(String word) {
        return getCommonWordsBefore(word, "");
    }

    public static List<WordResult> getCommonWordsBefore(String word, String pattern) {
        DatamuseService datamuse = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(DatamuseService.class, "https://api.datamuse.com");
        return datamuse.getWords(ImmutableMap.of("rc", word));
    }

    public static Set<String> getCommonWordsBetween(String before, String after) {
        return Sets.intersection(
            getCommonWordsAfter(before).stream().map(WordResult::getWord).collect(Collectors.toSet()),
            getCommonWordsBefore(after).stream().map(WordResult::getWord).collect(Collectors.toSet()));
    }

    @Data
    public static class WordResult {

        private final String word;
        private final int score;
    }

    interface DatamuseService {

        @RequestLine("GET /words")
        List<WordResult> getWords(@QueryMap Map<String, String> queryMap);
    }
}
