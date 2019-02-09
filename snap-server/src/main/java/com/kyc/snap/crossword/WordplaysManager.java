
package com.kyc.snap.crossword;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.form.FormEncoder;
import lombok.Data;

public class WordplaysManager {

    private final String apiKey;
    private final ObjectMapper mapper;

    public WordplaysManager(String apiKey) {
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
    }

    /**
     * Scrapes the Wordplays site to fetch Crossword clue answer suggestions.
     * <p>
     * The Wordplays site expects two arguments in the form data body: (1) the "clue" string, and
     * (2) a "pattern" string, which consists of a question mark for each unknown letter. The
     * response HTML looks like the following:
     *
     * <pre>
     * ...
     * &lt;table id="wordlists">
     *   &lt;tbody>
     *     &lt;tr class="odd">
     *       &lt;td>
     *         &lt;div class="stars">
     *           &lt;div>&lt;/div>
     *           &lt;div>&lt;/div>
     *           &lt;div>&lt;/div>
     *           &lt;div>&lt;/div>
     *           &lt;div>&lt;/div>
     *         &lt;/div>
     *       &lt;/td>
     *       &lt;td>
     *         &lt;a>RAT&lt;/a>
     *       &lt;/td>
     *     &lt;/tr>
     *     &lt;tr class="even">
     *       ...
     *     &lt;/tr>
     *     ...
     *   &lt;/tbody>
     * &lt;/table>
     * ...
     * </pre>
     */
    public List<ClueSuggestion> fetchCrosswordClueSuggestions(String clue, int numLetters) {
        ProxiedWordplaysService wordplays = Feign.builder()
            .encoder(new FormEncoder())
            .target(ProxiedWordplaysService.class, "http://api.scraperapi.com");
        Response page = wordplays.fetchCrosswordClueSuggestionsPage(
            apiKey, "https://www.wordplays.com/crossword-solver", true,
            clue, Joiner.on("").join(Collections.nCopies(numLetters, "?")));
        Element html;
        try {
            html = Jsoup.parse(mapper.readValue(IOUtils.toString(page.body().asInputStream(), StandardCharsets.UTF_8), String.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Element wordList = html.getElementById("wordlists");
        if (wordList == null)
            return ImmutableList.of();
        return Streams.concat(wordList.getElementsByClass("even").stream(), wordList.getElementsByClass("odd").stream())
            .map(row -> {
                String suggestion = Iterables.getOnlyElement(row.getElementsByTag("a")).text();
                int confidence = Iterables.getOnlyElement(row.getElementsByClass("stars")).children().size();
                return new ClueSuggestion(suggestion, confidence);
            })
            .sorted(Comparator.comparing(solvedClue -> -solvedClue.confidence))
            .collect(Collectors.toList());
    }

    interface ProxiedWordplaysService {

        @RequestLine("POST ?key={key}&url={url}&keep_headers={keep_headers}")
        @Headers("Content-type: application/x-www-form-urlencoded")
        Response fetchCrosswordClueSuggestionsPage(@Param("key") String apiKey, @Param("url") String url,
                @Param("keep_headers") boolean keepHeaders, @Param("clue") String clue, @Param("pattern") String pattern);
    }

    @Data
    public static class ClueSuggestion {

        private final String suggestion;

        /**
         * A confidence level, from 1 (lowest) to 5 (highest).
         */
        private final int confidence;
    }
}
