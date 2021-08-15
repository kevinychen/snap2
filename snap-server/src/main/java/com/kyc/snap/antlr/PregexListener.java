// Generated from Pregex.g4 by ANTLR 4.9.2

package com.kyc.snap.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PregexParser}.
 */
public interface PregexListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by the {@code Quote}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterQuote(PregexParser.QuoteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Quote}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitQuote(PregexParser.QuoteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OneOrMore}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterOneOrMore(PregexParser.OneOrMoreContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OneOrMore}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitOneOrMore(PregexParser.OneOrMoreContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Maybe}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterMaybe(PregexParser.MaybeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Maybe}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitMaybe(PregexParser.MaybeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Choice}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterChoice(PregexParser.ChoiceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Choice}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitChoice(PregexParser.ChoiceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Or}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterOr(PregexParser.OrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Or}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitOr(PregexParser.OrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Symbol}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterSymbol(PregexParser.SymbolContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Symbol}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitSymbol(PregexParser.SymbolContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Anagram}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterAnagram(PregexParser.AnagramContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Anagram}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitAnagram(PregexParser.AnagramContext ctx);
	/**
	 * Enter a parse tree produced by the {@code List}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterList(PregexParser.ListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code List}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitList(PregexParser.ListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Count}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void enterCount(PregexParser.CountContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Count}
	 * labeled alternative in {@link PregexParser#term}.
	 * @param ctx the parse tree
	 */
	void exitCount(PregexParser.CountContext ctx);
	/**
	 * Enter a parse tree produced by {@link PregexParser#terms}.
	 * @param ctx the parse tree
	 */
	void enterTerms(PregexParser.TermsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PregexParser#terms}.
	 * @param ctx the parse tree
	 */
	void exitTerms(PregexParser.TermsContext ctx);
}