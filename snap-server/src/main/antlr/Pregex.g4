grammar Pregex;

@header{
package com.kyc.snap.antlr;
}

fragment CHAR : [.A-Za-z];
fragment DIGIT : [0-9];

SYMBOL : CHAR;
COUNT : DIGIT+;

term
  : SYMBOL              #Symbol
  | '<' terms '>'       #Anagram
  | '[' terms ']'       #Choice
  | term '{' COUNT '}'  #Count
  | '(' terms ')'       #List
  | term '?'            #Maybe
  | term '+'            #OneOrMore
  | term '|' term       #Or
  | '"' terms '"'       #Quote
  | term '*'            #ZeroOrMore
  ;
terms : term+;
