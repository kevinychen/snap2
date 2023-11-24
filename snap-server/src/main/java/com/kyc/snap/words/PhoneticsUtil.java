package com.kyc.snap.words;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

/**
 * All phones are represented by ARPABET codes, which uses ASCII characters instead of IPA symbols.
 * See https://en.wikipedia.org/wiki/ARPABET.
 */
public enum PhoneticsUtil {
    ;

    private static final String PHONES_FILE = "./data/cmudict.0.7a_SPHINX_40";

    private static final int CONSONANT = 0;
    private static final int BILABIAL = 4;
    private static final int DENTAL = 6;
    private static final int VELAR = 8;
    private static final int PALATAL = 10;
    private static final int VOICED = 12;
    private static final int FRICATIVE = 13;
    private static final int FRICATIVE2 = 14;
    private static final int NASAL = 15;
    private static final int LIQUID = 17;
    private static final int IS_L = 18;
    private static final int IS_R = 19;
    private static final int IS_Y = 20;
    private static final int IS_W = 21;
    private static final int IS_HH = 22;
    private static final int FRONTNESS = 23;
    private static final int LOWNESS = 25;

    private static final Map<String, Integer> PHONE_FEATURE_BITSET_MAP = ImmutableMap.<String, Integer> builder()
            .put("", 0)
            .put("AA", 3 << LOWNESS)
            .put("AE", 3 << FRONTNESS | 3 << LOWNESS)
            .put("AH", 1 << FRONTNESS | 1 << LOWNESS)
            .put("AO", 1 << LOWNESS)
            .put("AW", 3 << LOWNESS)
            .put("AY", 3 << LOWNESS)
            .put("B", 15 << CONSONANT | 3 << BILABIAL | 1 << VOICED)
            .put("CH", 15 << CONSONANT | 3 << DENTAL | 1 << FRICATIVE2)
            .put("D", 15 << CONSONANT | 3 << DENTAL | 1 << VOICED)
            .put("DH", 15 << CONSONANT | 3 << DENTAL | 1 << VOICED | 1 << FRICATIVE)
            .put("EH", 3 << FRONTNESS | 1 << LOWNESS)
            .put("ER", 1 << FRONTNESS)
            .put("EY", 3 << FRONTNESS | 1 << LOWNESS)
            .put("F", 15 << CONSONANT | 3 << BILABIAL | 1 << FRICATIVE)
            .put("G", 15 << CONSONANT | 3 << VELAR | 1 << VOICED)
            .put("HH", 1 << LIQUID | 1 << IS_HH)
            .put("IH", 3 << FRONTNESS)
            .put("IY", 3 << FRONTNESS)
            .put("JH", 15 << CONSONANT | 3 << DENTAL | 1 << VOICED | 1 << FRICATIVE2)
            .put("K", 15 << CONSONANT | 3 << VELAR)
            .put("L", 1 << LIQUID | 1 << IS_L)
            .put("M", 15 << CONSONANT | 1 << BILABIAL | 3 << NASAL)
            .put("N", 15 << CONSONANT | 1 << DENTAL | 3 << NASAL)
            .put("NG", 15 << CONSONANT | 1 << VELAR | 3 << NASAL)
            .put("OW", 1 << LOWNESS)
            .put("OY", 1 << LOWNESS)
            .put("P", 15 << CONSONANT | 3 << BILABIAL)
            .put("R", 1 << LIQUID | 1 << IS_R)
            .put("S", 15 << CONSONANT | 3 << PALATAL | 1 << FRICATIVE)
            .put("SH", 15 << CONSONANT | 3 << PALATAL | 1 << FRICATIVE2)
            .put("T", 15 << CONSONANT | 3 << DENTAL)
            .put("TH", 15 << CONSONANT | 3 << DENTAL | 1 << FRICATIVE)
            .put("UH", 0)
            .put("UW", 0)
            .put("V", 15 << CONSONANT | 3 << BILABIAL | 1 << VOICED | 1 << FRICATIVE)
            .put("W", 1 << LIQUID | 1 << IS_W)
            .put("Y", 1 << LIQUID | 1 << IS_Y)
            .put("Z", 15 << CONSONANT | 3 << PALATAL | 1 << VOICED | 1 << FRICATIVE)
            .put("ZH", 15 << CONSONANT | 3 << PALATAL | 1 << VOICED | 1 << FRICATIVE2)
            .build();

    /**
     * A set of all phones used in the CMU Pronouncing Dictionary.
     */
    public static final Set<String> PHONES = PHONE_FEATURE_BITSET_MAP.keySet();

    /**
     * Returns all words in the CMU Pronouncing Dictionary and their pronunciations, described as a
     * list of phones.
     */
    public static Map<String, List<String>> getAllWords() {
        try (Scanner scanner = new Scanner(new File(PHONES_FILE))) {
            Map<String, List<String>> allWords = new TreeMap<>();
            while (scanner.hasNext()) {
                String word = scanner.next();
                List<String> phones = Arrays.asList(scanner.nextLine().trim().split(" "));
                if (StringUtils.isAlpha(word))
                    allWords.put(word, phones);
            }
            return allWords;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the difference between two phones. Both phones must be keys in PHONE_FEATURE_MAP, or
     * empty to represent the null phone, e.g. difference("EY", "AE") or difference("EY", "").
     * <p>
     * Identical or very similar phones (e.g. UH and UW) have a difference of zero. Similar phones
     * (e.g. B and P, or AE and EY) have a difference of 1, and somewhat similar phones (e.g. B and
     * F, or AA and AE) have a difference of 2. Dissimilar phones have a difference of larger than
     * 2, and very different phones have differences of 5 or larger.
     */
    public static double difference(String phone1, String phone2) {
        return Integer.bitCount(PHONE_FEATURE_BITSET_MAP.get(phone1) ^ PHONE_FEATURE_BITSET_MAP.get(phone2));
    }
}
