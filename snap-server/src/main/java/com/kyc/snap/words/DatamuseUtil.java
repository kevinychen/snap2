package com.kyc.snap.words;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import feign.Feign;
import feign.QueryMap;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;

public enum DatamuseUtil {
    ;

    public static List<WordResult> getCommonWordsAfter(String word) {
        return getCommonWordsAfter(word, "*");
    }

    public static List<WordResult> getCommonWordsAfter(String word, String pattern) {
        DatamuseService datamuse = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(DatamuseService.class, "https://api.datamuse.com");
        return datamuse.getWords(Map.of("sp", pattern, "lc", word));
    }

    public static List<WordResult> getCommonWordsBefore(String word) {
        return getCommonWordsBefore(word, "*");
    }

    public static List<WordResult> getCommonWordsBefore(String word, String pattern) {
        DatamuseService datamuse = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(DatamuseService.class, "https://api.datamuse.com");
        return datamuse.getWords(Map.of("sp", pattern, "rc", word));
    }

    public static Set<String> getCommonWordsBetween(String before, String after) {
        return Sets.intersection(
            getCommonWordsAfter(before).stream().map(WordResult::word).collect(Collectors.toSet()),
            getCommonWordsBefore(after).stream().map(WordResult::word).collect(Collectors.toSet()));
    }

    public record WordResult(String word, int score) {}

    interface DatamuseService {

        @RequestLine("GET /words")
        List<WordResult> getWords(@QueryMap Map<String, String> queryMap);
    }
}
