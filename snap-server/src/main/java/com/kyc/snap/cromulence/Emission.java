package com.kyc.snap.cromulence;

import lombok.Data;

/**
 * An array with 52 entries, with probs[i] = probability of letter ('A' + i) for i≤25, and probs[i]
 * = probability of letter ('A' + i % 26) being the last letter of the word if i≥26.
 */
@Data
public class Emission {

    public static final int NUM_LETTERS = 26;
    public static final int SIZE = 2 * NUM_LETTERS;

    private final double[] probs;

    public Emission(double[] probs) {
        if (probs.length != SIZE)
            throw new IllegalArgumentException("Invalid size of emission array: " + probs.length);
        this.probs = probs;
    }
}
