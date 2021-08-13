package com.kyc.snap.cromulence;

import com.kyc.snap.antlr.PregexParser.AnagramContext;
import com.kyc.snap.antlr.PregexParser.ChoiceContext;
import com.kyc.snap.antlr.PregexParser.CountContext;
import com.kyc.snap.antlr.PregexParser.ListContext;
import com.kyc.snap.antlr.PregexParser.MaybeContext;
import com.kyc.snap.antlr.PregexParser.OneOrMoreContext;
import com.kyc.snap.antlr.PregexParser.OrContext;
import com.kyc.snap.antlr.PregexParser.QuoteContext;
import com.kyc.snap.antlr.PregexParser.SymbolContext;
import com.kyc.snap.antlr.PregexParser.TermContext;
import com.kyc.snap.antlr.PregexParser.ZeroOrMoreContext;
import com.kyc.snap.cromulence.TermStates.AnagramState;
import com.kyc.snap.cromulence.TermStates.ChoiceState;
import com.kyc.snap.cromulence.TermStates.CountState;
import com.kyc.snap.cromulence.TermStates.ListState;
import com.kyc.snap.cromulence.TermStates.MaybeState;
import com.kyc.snap.cromulence.TermStates.OrMoreState;
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
            if (children.size() > 60)
                throw new IllegalArgumentException("Too many anagram components");
            return AnagramState.of(parent, children);
        }
    }

    @Data
    static class ChoiceNode implements TermNode {
        final List<TermNode> children;

        @Override
        public TermState toTermState(TermState parent) {
            return new ChoiceState(parent, children, false);
        }
    }

    @Data
    static class CountNode implements TermNode {
        final TermNode child;
        final int count;

        @Override
        public TermState toTermState(TermState parent) {
            if (count > 60)
                throw new IllegalArgumentException("Count too high");
            return new CountState(parent, child, count);
        }
    }

    @Data
    static class ListNode implements TermNode {
        final List<TermNode> children;

        @Override
        public TermState toTermState(TermState parent) {
            return new ListState(parent, children, 0);
        }
    }

    @Data
    static class MaybeNode implements TermNode {
        final TermNode child;

        @Override
        public TermState toTermState(TermState parent) {
            return new MaybeState(parent, child, false);
        }
    }

    @Data
    static class OrMoreNode implements TermNode {
        final TermNode child;
        final int atLeast;

        @Override
        public TermState toTermState(TermState parent) {
            return new OrMoreState(parent, child, atLeast);
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
        if (context instanceof CountContext) {
            CountContext countContext = (CountContext) context;
            return new CountNode(fromAntlr(countContext.term()),
                Integer.parseInt(countContext.COUNT().getSymbol().getText()));
        }
        if (context instanceof ListContext) {
            return new ListNode(((ListContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        if (context instanceof MaybeContext) {
            return new MaybeNode(fromAntlr(((MaybeContext) context).term()));
        }
        if (context instanceof OneOrMoreContext) {
            return new OrMoreNode(fromAntlr(((OneOrMoreContext) context).term()), 1);
        }
        if (context instanceof OrContext) {
            OrContext orContext = (OrContext) context;
            return new ChoiceNode(List.of(fromAntlr(orContext.term(0)), fromAntlr(orContext.term(1))));
        }
        if (context instanceof QuoteContext) {
            return new QuoteNode(((QuoteContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        if (context instanceof ZeroOrMoreContext) {
            return new OrMoreNode(fromAntlr(((ZeroOrMoreContext) context).term()), 0);
        }
        return new SymbolNode('-'); // fallback
    }
}
