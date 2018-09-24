
package com.kyc.snap.crossword;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.form.FormEncoder;
import lombok.Data;

public class WordplaysUtil {

    /**
     * The minimum amount of time in milliseconds to wait between queries in order to not be rate
     * limited by the Wordplays site.
     */
    public static final int MIN_QUERY_WAIT_TIME_MILLIS = 9000;

    private static long nextQueryTime = 0;

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
    public static synchronized List<ClueSuggestion> fetchCrosswordClueSuggestions(String clue, int numLetters) {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime < nextQueryTime)
                Thread.sleep(nextQueryTime - currentTime);

            WordplaysService wordplays = Feign.builder()
                .encoder(new FormEncoder())
                .target(WordplaysService.class, "https://www.wordplays.com");
            String page = wordplays.fetchCrosswordClueSuggestionsPage(clue, Joiner.on("").join(Collections.nCopies(numLetters, "?")));
            Element html = Jsoup.parse(page);
            Element wordList = html.getElementById("wordlists");
            return Streams.concat(wordList.getElementsByClass("even").stream(), wordList.getElementsByClass("odd").stream())
                .map(row -> {
                    String suggestion = Iterables.getOnlyElement(row.getElementsByTag("a")).text();
                    int confidence = Iterables.getOnlyElement(row.getElementsByClass("stars")).children().size();
                    return new ClueSuggestion(suggestion, confidence);
                })
                .sorted(Comparator.comparing(solvedClue -> -solvedClue.confidence))
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            nextQueryTime = System.currentTimeMillis() + MIN_QUERY_WAIT_TIME_MILLIS;
        }
    }

    interface WordplaysService {

        @RequestLine("POST /crossword-solver")
        @Headers("Content-type: application/x-www-form-urlencoded")
        String fetchCrosswordClueSuggestionsPage(@Param("clue") String clue, @Param("pattern") String pattern);
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
