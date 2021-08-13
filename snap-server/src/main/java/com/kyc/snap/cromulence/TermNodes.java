package com.kyc.snap.cromulence;

import com.kyc.snap.antlr.PregexParser.AnagramContext;
import com.kyc.snap.antlr.PregexParser.ChoiceContext;
import com.kyc.snap.antlr.PregexParser.ListContext;
import com.kyc.snap.antlr.PregexParser.QuoteContext;
import com.kyc.snap.antlr.PregexParser.SymbolContext;
import com.kyc.snap.antlr.PregexParser.TermContext;
import com.kyc.snap.cromulence.TermStates.AnagramState;
import com.kyc.snap.cromulence.TermStates.ChoiceState;
import com.kyc.snap.cromulence.TermStates.ListState;
import com.kyc.snap.cromulence.TermStates.QuoteState;
import com.kyc.snap.cromulence.TermStates.SymbolState;
import com.kyc.snap.cromulence.TermStates.TermState;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

class TermNodes {

    interface TermNode {
        TermState toTermState(TermState parent);
    }

    @Data
    static class SymbolNode implements TermNode {
        final char c;

        @Override
        public TermState toTermState(TermState parent) {
            return new SymbolState(parent, c);
        }
    }

    @Data
    static class AnagramNode implements TermNode {
        final List<TermNode> children;

        @Override
        public TermState toTermState(TermState parent) {
            return AnagramState.of(parent, children);
        }
    }

    @Data
    static class ChoiceNode implements TermNode {
        final List<TermNode> children;

        @Override
        public TermState toTermState(TermState parent) {
            if (children.size() > 60)
                throw new IllegalArgumentException("Too many anagram components");
            return new ChoiceState(parent, children, false);
        }
    }

    @Data
    static class ListNode implements TermNode {
        final List<TermNode> children;

        ListNode(List<TermNode> children) {
            this.children = children;
        }

        @Override
        public TermState toTermState(TermState parent) {
            return new ListState(parent, children, 0);
        }
    }

    @Data
    static class QuoteNode implements TermNode {
        final List<TermNode> children;

        @Override
        public TermState toTermState(TermState parent) {
            return new QuoteState(parent, children, 0);
        }
    }

    static TermNode fromAntlr(TermContext context) {
        if (context instanceof SymbolContext) {
            return new SymbolNode(((SymbolContext) context).SYMBOL().getSymbol().getText().charAt(0));
        }
        if (context instanceof AnagramContext) {
            return new AnagramNode(((AnagramContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        if (context instanceof ChoiceContext) {
            return new ChoiceNode(((ChoiceContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        if (context instanceof ListContext) {
            return new ListNode(((ListContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        if (context instanceof QuoteContext) {
            return new QuoteNode(((QuoteContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        return new SymbolNode('-'); // fallback
    }
}
