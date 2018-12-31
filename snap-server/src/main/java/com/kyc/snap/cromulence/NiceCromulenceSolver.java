
package com.kyc.snap.cromulence;

import java.util.List;
import java.util.stream.Collectors;

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

    public CromulenceSolverResult solveSlug(String slug) {
        return solver.solveSlug(toEmissions(slug));
    }

    public CromulenceSolverResult solveRearrangement(List<String> parts) {
        return solver.solveRearrangement(parts.stream()
            .map(str -> toEmissions(str))
            .collect(Collectors.toList()));
    }

    private static List<Emission> toEmissions(String str) {
        return str.chars()
            .mapToObj(c -> toEmission((char) c))
            .collect(Collectors.toList());
    }

    private static Emission toEmission(char c) {
        double[] probs = new double[Emission.SIZE];
        if (c == '*') {
            for (int i = 0; i < Emission.NUM_LETTERS; i++)
                probs[i] = probs[i + Emission.NUM_LETTERS] = 1.0 / Emission.NUM_LETTERS;
        } else if (Character.isUpperCase(c)) {
            probs[c - 'A'] = probs[c - 'A' + Emission.NUM_LETTERS] = 1.0;
        } else if (Character.isLowerCase(c)) {
            for (int i = 0; i < Emission.NUM_LETTERS; i++)
                probs[i] = probs[i + Emission.NUM_LETTERS] = 0.2 / (Emission.NUM_LETTERS - 1);
            probs[c - 'a'] = probs[c - 'a' + Emission.NUM_LETTERS] = 0.8;
        } else {
            throw new IllegalArgumentException("Invalid emission code: " + c);
        }
        return new Emission(probs);
    }
}
