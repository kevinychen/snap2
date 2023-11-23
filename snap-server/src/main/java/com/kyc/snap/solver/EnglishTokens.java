package com.kyc.snap.solver;

public enum EnglishTokens {
    ;

    public static final int NUM_LETTERS = 26;
    private static final double[][] IS = new double[NUM_LETTERS][NUM_LETTERS + 1];
    private static final double[][] PROBABLY = new double[NUM_LETTERS][NUM_LETTERS + 1];
    private static final double[] WILDCARD = new double[NUM_LETTERS + 1];
    private static final double[] WORD_DELIMITER = new double[NUM_LETTERS + 1];
    static {
        for (char c = 'A'; c <= 'Z'; c++) {
            IS[c - 'A'][c - '@'] = 1;
            PROBABLY[c - 'A'][c - '@'] = .8;
            for (char cc = 'A'; cc <= 'Z'; cc++)
                if (cc != c)
                    PROBABLY[c - 'A'][cc - '@'] = .2 / (NUM_LETTERS - 1);
            WILDCARD[c - '@'] = 1. / NUM_LETTERS;
        }
        WORD_DELIMITER[0] = 1;
    }

    public static double[] is(char c) {
        assert c >= 'A' && c <= 'Z';
        return IS[c - 'A'];
    }

    public static double[] probably(char c) {
        assert c >= 'A' && c <= 'Z';
        return PROBABLY[c - 'A'];
    }

    public static double[] wildcard() {
        return WILDCARD;
    }

    public static double[] wordDelimiter() {
        return WORD_DELIMITER;
    }
}
