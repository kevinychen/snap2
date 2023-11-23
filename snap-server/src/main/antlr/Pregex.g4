grammar Pregex;

@header{
package com.kyc.snap.antlr;
}

// '*' and '.' are the same and both mean "any letter"
fragment CHAR : [*.A-Za-z];
fragment DIGIT : [0-9];

SYMBOL : CHAR;
COUNT : DIGIT+;

term
  : SYMBOL                  #Symbol
  | '<' terms '>'           #Anagram
  | term '&' term           #And
  | '\\chain(' terms ')'    #Chain
  | '[' terms ']'           #Choice
  | term '{' COUNT '}'      #Count
  | '(' terms '~' terms ')' #Interleave
  | '(' terms ')'           #List
  | term '?'                #Maybe
  | term '+'                #OneOrMore
  | term '|' term           #Or
  | '"' terms '"'           #Quote
  | '\\b'                   #WordBoundary
  ;
terms : term+;
