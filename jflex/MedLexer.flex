\
/* MedLexer.flex
 * JFlex specification for MedScript.
 * (Used to generate MedLexer.java)
 */
package medscript.compiler;

import java.io.Reader;

%%

%public
%class MedLexer
%unicode
%line
%column
%type Token
%function nextToken

%{
  private Token token(TokenType type, String lexeme) {
    return new Token(type, lexeme, yyline+1, yycolumn+1);
  }
%}

WHITESPACE   = [ \t\f\r\n]+
ID           = [A-Za-z][A-Za-z0-9_-]*
NUMBER       = ([0-9]+(\.[0-9]+)?)|([0-9]+\/[0-9]+)
UNIT         = (mg|g|ml|mcg|IU|%|drops)
DURUNIT      = (d|w|m)
FREQ         = (od|bd|tds|qid|hs|stat|prn|sos|q[0-9]+h)
ROUTE        = (po|iv|im|sc|sl|pr|topical|inhale)
FOODMOD      = (ac|pc|with_meals|after_food|before_food)

FORM         = (Tab|Cap|Syr|Inj|Oint|Drops|Cream|Neb)
SECTION_RX   = rx:
SECTION_PT   = patient
SECTION_ALL  = allergy
SECTION_NOT  = notes:

COMMENT      = \#.*

%%

{WHITESPACE}             { /* skip */ }
{COMMENT}                { /* skip */ }

{SECTION_PT}             { return token(TokenType.SECTION_PATIENT, yytext()); }
{SECTION_ALL}            { return token(TokenType.SECTION_ALLERGY, yytext()); }
{SECTION_RX}             { return token(TokenType.SECTION_RX, yytext()); }
{SECTION_NOT}            { return token(TokenType.SECTION_NOTES, yytext()); }

":"                       { return token(TokenType.COLON, yytext()); }

{FORM}                    { return token(TokenType.FORM, yytext()); }
{ROUTE}                   { return token(TokenType.ROUTE, yytext()); }
{FREQ}                    { return token(TokenType.FREQUENCY, yytext()); }
{FOODMOD}                 { return token(TokenType.FOOD_MOD, yytext()); }

{UNIT}                    { return token(TokenType.UNIT, yytext()); }
{DURUNIT}                 { return token(TokenType.DURATION_UNIT, yytext()); }
{NUMBER}                  { return token(TokenType.NUMBER, yytext()); }
{ID}                      { return token(TokenType.ID, yytext()); }

.                         { return token(TokenType.UNKNOWN, yytext()); }

<<EOF>>                   { return token(TokenType.EOF, "<EOF>"); }
