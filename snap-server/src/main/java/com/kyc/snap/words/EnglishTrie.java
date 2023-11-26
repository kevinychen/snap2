package com.kyc.snap.words;

import java.util.Collection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.kyc.snap.solver.EnglishTokens;

public class EnglishTrie {

    private final Multimap<Integer, String> wordsByNodeIndex = ArrayListMultimap.create();
    private int[][] nodes;
    private int size = 1;

    public static EnglishTrie of(Dictionary dictionary, int maxNumDeletions) {
        EnglishTrie trie = new EnglishTrie();
        for (String word : dictionary.getWords()) {
            trie.add(word, word);

            if (maxNumDeletions >= 1)
                for (int i = 0; i < word.length(); i++)
                    trie.add(word.substring(0, i) + word.substring(i + 1), word);
        }
        return trie;
    }

    private EnglishTrie() {
        this.nodes = new int[1 << 20][EnglishTokens.NUM_LETTERS];
    }

    public int startNodeIndex() {
        return 1;
    }

    public int getNodeIndex(int nodeIndex, char c) {
        return nodeIndex < nodes.length ? nodes[nodeIndex][c - 'A'] : 0;
    }

    public Collection<String> getWords(int nodeIndex) {
        return wordsByNodeIndex.get(nodeIndex);
    }

    private void add(String fuzzyWord, String word) {
        int nodeIndex = startNodeIndex();
        for (int i = 0; i < fuzzyWord.length(); i++) {
            int index = fuzzyWord.charAt(i) - 'A';
            ensureSize(nodeIndex);
            if (nodes[nodeIndex][index] == 0)
                nodes[nodeIndex][index] = ++size;
            nodeIndex = nodes[nodeIndex][index];
        }
        wordsByNodeIndex.put(nodeIndex, word);
    }

    private void ensureSize(int nodeIndex) {
        if (nodeIndex >= nodes.length) {
            int[][] prevNodes = nodes;
            nodes = new int[nodes.length * 2][nodes[0].length];
            for (int i = 0; i < prevNodes.length; i++)
                System.arraycopy(prevNodes[i], 0, nodes[i], 0, nodes[0].length);
        }
    }
}
