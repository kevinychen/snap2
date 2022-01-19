package com.kyc.snap.cromulence;

import static com.kyc.snap.cromulence.CromulenceSolver.NUM_LETTERS;

import com.kyc.snap.antlr.PregexParser.AnagramContext;
import com.kyc.snap.antlr.PregexParser.ChoiceContext;
import com.kyc.snap.antlr.PregexParser.CountContext;
import com.kyc.snap.antlr.PregexParser.InterleaveContext;
import com.kyc.snap.antlr.PregexParser.ListContext;
import com.kyc.snap.antlr.PregexParser.MaybeContext;
import com.kyc.snap.antlr.PregexParser.OneOrMoreContext;
import com.kyc.snap.antlr.PregexParser.OrContext;
import com.kyc.snap.antlr.PregexParser.QuoteContext;
import com.kyc.snap.antlr.PregexParser.SymbolContext;
import com.kyc.snap.antlr.PregexParser.TermContext;
import com.kyc.snap.cromulence.StateTransitions.AnagramState;
import com.kyc.snap.cromulence.StateTransitions.ChoiceState;
import com.kyc.snap.cromulence.StateTransitions.CountState;
import com.kyc.snap.cromulence.StateTransitions.InterleaveState;
import com.kyc.snap.cromulence.StateTransitions.ListState;
import com.kyc.snap.cromulence.StateTransitions.MaybeState;
import com.kyc.snap.cromulence.StateTransitions.OrMoreState;
import com.kyc.snap.cromulence.StateTransitions.QuoteState;
import com.kyc.snap.cromulence.StateTransitions.SymbolState;
import com.kyc.snap.cromulence.StateTransitions.TermState;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

class TermNodes {

    interface TermNode {
        int complexity();
        TermState toTermState(TermState parent);
    }

    @Data
    static class SymbolNode implements TermNode {
        final char c;

        @Override
        public int complexity() {
            return c >= 'A' && c <= 'Z' ? 1 : NUM_LETTERS;
        }

        @Override
        public TermState toTermState(TermState parent) {
            return new SymbolState(parent, c);
        }
    }

    @Data
    static class AnagramNode implements TermNode {
        final List<TermNode> children;

        @Override
        public int complexity() {
            return children.stream().mapToInt(TermNode::complexity).sum();
        }

        @Override
        public TermState toTermState(TermState parent) {
            return AnagramState.of(parent, children);
        }
    }

    @Data
    static class ChoiceNode implements TermNode {
        final Set<TermNode> children;

        @Override
        public int complexity() {
            return children.stream().mapToInt(TermNode::complexity).sum();
        }

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
        public int complexity() {
            return child.complexity();
        }

        @Override
        public TermState toTermState(TermState parent) {
            return new CountState(parent, child, count);
        }
    }

    @Data
    static class InterleaveNode implements TermNode {
        final List<TermNode> children;

        @Override
        public int complexity() {
            return children.stream().mapToInt(TermNode::complexity).sum();
        }

        @Override
        public TermState toTermState(TermState parent) {
            return InterleaveState.of(parent, children);
        }
    }

    @Data
    static class ListNode implements TermNode {
        final List<TermNode> children;

        @Override
        public int complexity() {
            return children.stream().mapToInt(TermNode::complexity).max().orElse(0);
        }

        @Override
        public TermState toTermState(TermState parent) {
            return new ListState(parent, children, 0);
        }
    }

    @Data
    static class MaybeNode implements TermNode {
        final TermNode child;

        @Override
        public int complexity() {
            return child.complexity();
        }

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
        public int complexity() {
            return child.complexity();
        }

        @Override
        public TermState toTermState(TermState parent) {
            return new OrMoreState(parent, child, atLeast);
        }
    }

    @Data
    static class QuoteNode implements TermNode {
        final List<TermNode> children;

        @Override
        public int complexity() {
            return children.stream().mapToInt(TermNode::complexity).max().orElse(0);
        }

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
                .collect(Collectors.toSet()));
        }
        if (context instanceof CountContext) {
            CountContext countContext = (CountContext) context;
            return new CountNode(fromAntlr(countContext.term()),
                Integer.parseInt(countContext.COUNT().getSymbol().getText()));
        }
        if (context instanceof InterleaveContext) {
            InterleaveContext interleaveContext = (InterleaveContext) context;
            return new InterleaveNode(List.of(
                fromAntlr(interleaveContext.term(0)),
                fromAntlr(interleaveContext.term(1))));
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
            return new ChoiceNode(Set.of(fromAntlr(orContext.term(0)), fromAntlr(orContext.term(1))));
        }
        if (context instanceof QuoteContext) {
            return new QuoteNode(((QuoteContext) context).terms().term().stream()
                .map(TermNodes::fromAntlr)
                .collect(Collectors.toList()));
        }
        return new SymbolNode('-'); // fallback
    }
}
