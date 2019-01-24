
package com.kyc.snap.cromulence;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.Data;

/**
 * Provides a simple way to specify probability distributions for characters. The distribution is
 * encoded by one of the following:
 * <ul>
 * <li>An upper-case letter: 100% sure.</li>
 * <li>A lower-case letter: 80% sure, probability distributed evenly among other letters.</li>
 * <li>An asterisk (*): uniform distribution over all letters.</li>
 * </ul>
 */
@Data
public class NiceCromulenceSolver {

    private final CromulenceSolver solver;

    public List<CromulenceSolverResult> solveSlug(String slug) {
        List<Emission> emissions = toEmissions(slug);
        return solver.solveSlug(emissions, defaultEndOfWordProbs(emissions.size()));
    }

    public List<CromulenceSolverResult> solveSlug(String slug, List<Integer> wordLens) {
        return solver.solveSlug(toEmissions(slug), toEndOfWordProbs(wordLens));
    }

    public List<CromulenceSolverResult> anagramSingleWord(String anagram) {
        return solveRearrangement(
            anagram.chars().mapToObj(c -> "" + (char) c).collect(Collectors.toList()),
            ImmutableList.of(anagram.length()));
    }

    public List<CromulenceSolverResult> anagramPhrase(String anagram) {
        return solveRearrangement(
            anagram.chars().mapToObj(c -> "" + (char) c).collect(Collectors.toList()));
    }

    public List<CromulenceSolverResult> solveRearrangement(List<String> parts) {
        List<List<Emission>> emissions = parts.stream()
                .map(str -> toEmissions(str))
                .collect(Collectors.toList());
        return solver.solveRearrangement(emissions, defaultEndOfWordProbs(emissions.stream().mapToInt(List::size).sum()));
    }

    public List<CromulenceSolverResult> solveRearrangement(List<String> parts, List<Integer> wordLens) {
        return solver.solveRearrangement(parts.stream()
            .map(str -> toEmissions(str))
            .collect(Collectors.toList()), toEndOfWordProbs(wordLens));
    }

    private static List<Double> defaultEndOfWordProbs(int len) {
        List<Double> endOfWordProbs = new ArrayList<>();
        for (int i = 0; i < len - 1; i++)
            endOfWordProbs.add(0.5);
        endOfWordProbs.add(1.0);
        return endOfWordProbs;
    }

    private static List<Double> toEndOfWordProbs(List<Integer> wordLens) {
        List<Double> endOfWordProbs = new ArrayList<>();
        for (int wordLen : wordLens) {
            for (int i = 0; i < wordLen - 1; i++)
                endOfWordProbs.add(0.0);
            endOfWordProbs.add(1.0);
        }
        return endOfWordProbs;
    }

    private static List<Emission> toEmissions(String str) {
        List<Emission> emissions = new ArrayList<>();
        for (int index = 0; index < str.length(); index++) {
            char c = str.charAt(index);
            if (c == '[') {
                int endIndex = str.indexOf(']', index);
                if (endIndex == -1)
                    endIndex = str.length();
                emissions.add(toEmission(Lists.charactersOf(str.substring(index + 1, endIndex))));
                index = endIndex;
            } else
                emissions.add(toEmission(ImmutableList.of(c)));
        }
        return emissions;
    }

    private static Emission toEmission(List<Character> letters) {
        letters = letters.stream()
            .filter(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')
            .map(letters.stream().anyMatch(Character::isLowerCase) ? Character::toLowerCase : c -> c)
            .distinct()
            .collect(Collectors.toList());
        double[] probs = new double[Emission.SIZE];
        if (letters.isEmpty()) {
            for (int i = 0; i < Emission.NUM_LETTERS; i++)
                probs[i] = 1. / Emission.NUM_LETTERS;
        } else if (Character.isLowerCase(letters.get(0))) {
            if (letters.size() < Emission.NUM_LETTERS)
                for (int i = 0; i < Emission.NUM_LETTERS; i++)
                    probs[i] = 0.2 / (Emission.NUM_LETTERS - letters.size());
            for (char c : letters)
                probs[c - 'a'] = 0.8 / letters.size();
        } else {
            for (char c : letters)
                probs[c - 'A'] = 1. / letters.size();
        }
        for (int i = 0; i < Emission.NUM_LETTERS; i++)
            probs[i + Emission.NUM_LETTERS] = probs[i];
        return new Emission(probs);
    }
}
