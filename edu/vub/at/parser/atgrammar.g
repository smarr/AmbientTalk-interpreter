header { package edu.vub.at.parser; }

class ATParser extends Parser;

options {
  k = 3;
  buildAST = true;
}

// Ambienttalk/2 programs consist of statements separated by semicolons
program : semicolonlist EOF;

// Any list of statements separated by semicolons can be seen as arguments to a begin native.
semicolonlist : statement (SMC! statement)* { #semicolonlist = #([BEGIN, "begin"], #semicolonlist); };

// Statements can be either definitions assignments or ordinary expressions
statement: definition | assignment | expression;

// Definitions start with ambienttalk/2's only reserved word def
// def <name> := <expression> defines a variable which can be assigned later.
// def <apl> { <body> } defines an immutable function.
definition: "def"^ (NAM EQL! expression | application LBC! semicolonlist RBC!);

// Function application can be done in two distinct mechanisms, either using a 
// canonical format ( foobar( a1, a2 ) ) or using keywordlists (foo: a1 bar: a2).
// The latter format is used often in conjunction with blocks.
application: canonical
           | keywordlist;

// Assignment of a variable is similar to its definition albeit without the word def.
// TODO the lhs of an assignment can also be (at least) a tabulation. 
assignment: NAM EQL^ expression;

// Expressions are split up according to precedence. Ambienttalk/2's keyworded message
// sends have lowest priority and are therefore the highest applicable rule.
expression: keywordlist
          | comparand (CMP^ comparand)*;

// Comparands are expression types delimited by comparators so that they can 
// be composed of additive expressions or any higher ranking operations.
comparand: term (ADD^ term)*;

// Terms are expression types delimited by additive operators so that they can 
// be composed of multiplicative expressions or any higher ranking operations.
term: factor (MUL^ factor)*;

// Factors are expression types delimited by multiplicative operators so that they can
// be composed of exponential expressions or any higher ranking operations.
factor: invocation (POW^ invocation)*;

// Terms are expression types delimited by exponential  operators so that they can
// be composed of curried invocations only. To allow them to intervene the result 
// of parsing the reference is passed to the curried invocation.
invocation!: r:reference c:curried_invocations[#r] { #invocation = #c; };

// References are the most fundamental elements of the language, namely primitive 
// values (numbers and  strings), variables, blocks, inline tables and subexpressions
reference: NBR^
         | FRC^
         | TXT^
         | variable
         | subexpression
         | block
         | table;

// Curried invocations eagerly consume all subsequent ( [ . tokens. If such tokens are
// available a single invocation is parsed passing on the received functor (which will
// be applied, tabulated, or sent a message). The result of this parsing step is passed
// to be curried even further. When no appropriate tokens are left, the passed functor 
// is returned.
curried_invocations![AST functor]:
	(LPR|LBR|DOT) => i:invoke_expression[functor] c:curried_invocations[#i] 
	{ #curried_invocations = #c; } 
	| {#curried_invocations = #functor; };

// Invocation expressions are a single curried expression whether to apply, tabulate or
// invoke its functor. 
invoke_expression[AST functor] 
	: LPR! (commalist[true])? RPR!  { #invoke_expression = #([APPLY,"apply"], #functor, #invoke_expression); }
	| LBR! expression RBR! { #invoke_expression = #([SMC,"table-get"], #functor, #invoke_expression); }
	| (DOT variable LPR | DOT KEY) => DOT! application { #invoke_expression = #([SMC,"invocation"], #functor, #invoke_expression); }
	| DOT! variable { #invoke_expression = #([SMC,"selection"], #functor, #invoke_expression);};


canonical: variable LPR! (commalist[true])? RPR! { #canonical = #([SMC,"apply"], #canonical); }; 

// Keyworded message sends are an alternation of keywords (names ending with a colon)
// and ordinary expressions. They allow for elegant ways to write control structures
// as well as custom language constructs. The keyworded messages suffer from a generalised
// version of the dangling else problem. Keywords are chained by the parser to form the
// longest possible chain. As a consequence, nested keyworded message consume all keywords
// unless they are delimited using e.g. subexpressions.
// TECH: The grammar is (inevitably?) ambiguous here, as we are aware of the problem, we
// switch off the warning for this grammar rule. 
keywordlist: singlekeyword
			 (options {
				warnWhenFollowAmbig = false;
			 } : singlekeyword)* { #keywordlist = #([KEY,"keywordlist"], #keywordlist); };

// This rule groups a keyword and the adjoined expression into a single tree element.
singlekeyword: (KEY^ expression);

// This rule unwraps an expression of its delimiter parentheses.
subexpression!
	: LPR e:expression RPR { #subexpression = #e; };

// Inline syntax for nameless functions (lambdas or blocks)
block!
	: LBC! vars:variablelist PIP! body:semicolonlist RBC!
		{ #block = #([SMC, "block"], #vars, #body); }
	| LBC! no_args_body:semicolonlist RBC! 
		{ #block = #([SMC, "block"], #no_args_body); };

// Inline syntax for table expressions
table: LBR! (commalist[false])? RBR! { #table = #([COM, "table"], #table); };

// Parses a list of expressions separated by commas. 
// USAGE: canonical function application (arguments) and inline tables
// @param generateImaginaryNode - generate an additional tree node?
commalist[boolean generateImaginaryNode]: expression (COM! expression)* 
	{ if(generateImaginaryNode)
		#commalist = #([LIST,"list"], #commalist);
	};

// Parses a list of variables
variablelist: variable (COM! variable)* { #variablelist = #([COM, "vars"], #variablelist); };

variable: NAM | operator;

operator: CMP | ADD | MUL | POW;



class ATLexer extends Lexer;

options {
  k = 3;
}

// OUTPUT TOKENS
// These tokens are never produced by the scanner itself as they are protected. 
// However they are used to annotate the resulting ANTLR tree, so that the walker
// can easily produce the correct Java ATParsetree elements.
// Each token definition aligns the token with its printed representation.
protected BEGIN: "begin"
	; 

protected APPLY: "apply"
	; 

protected LIST: "list"
	; 

// Protected Scanner Tokens
protected DIGIT: '0'..'9'
    ;
    
protected LETTER: ('a'..'z'|'A'..'Z' )
    ;

protected EXPONENT: ('e' | 'E')
	;
	
protected CMPCHAR: ('<' | '=' | '>' )
	;

protected ADDCHAR: ( '+' | '-' )
	;
	
protected MULCHAR: ( '*' | '/' | '\\' | '&' )
	;
	
protected POWCHAR: ( '^' )
	;
	
protected OPRCHAR: CMPCHAR | ADDCHAR | MULCHAR | POWCHAR
	;
	
protected SIGN: ('+' | '-' )
	;
	
protected SCALE: EXPONENT (SIGN)? NBR
	;
	
protected COLON: ':'
	;
	
protected NBR: (DIGIT)+
	;

protected FRC: NBR (SCALE | DOT NBR (SCALE)?)
	;
	
NBR_OR_FRC: ( NBR EXPONENT ) => FRC  { $setType(FRC); }
          | ( NBR DOT ) => FRC       { $setType(FRC); }
          |   NBR                    { $setType(NBR); }
    ;

CMP: CMPCHAR (OPRCHAR)*
	;

ADD: ADDCHAR (OPRCHAR)*
	;

MUL: MULCHAR (OPRCHAR)*
	;

POW: POWCHAR (OPRCHAR)*
	;

protected NAM: LETTER (DIGIT | LETTER)*
	;
	
protected KEY: NAM COLON
    ;

NAM_OR_KEY: ( NAM COLON ) => KEY  { $setType(KEY); }
          |   NAM                 { $setType(NAM); }
    ;

WHITESPACE: ('\t' |  ' ')
           { $setType(Token.SKIP); }
    ;
    
NEWLINE:  ( "\r\n" | '\r' | '\n')
          { newline(); 
            $setType(Token.SKIP); }
    ;
    
LPR: '(';
RPR: ')';

LBR: '[';
RBR: ']';

LBC: '{';
RBC: '}';

COM: ',';
SMC: ';';

EQL: ":=";
DOT: '.';
PIP: '|';

TXT : '"' (ESC|~('"'|'\\'|'\n'|'\r'))* '"'
	;

    // Single-line comments
SL_COMMENT
	:	"//" WHITESPACE
		(~('\n'|'\r'))* ('\n'|'\r'('\n')?)
		{$setType(Token.SKIP); newline();}
	;

// multiple-line comments
ML_COMMENT
	:	"/*" WHITESPACE
		(	/*	'\r' '\n' can be matched in one alternative or by matching
				'\r' in one iteration and '\n' in another. I am trying to
				handle any flavor of newline that comes in, but the language
				that allows both "\r\n" and "\r" and "\n" to all be valid
				newline is ambiguous. Consequently, the resulting grammar
				must be ambiguous. I'm shutting this warning off.
			 */
			options {
				generateAmbigWarnings=false;
			}
		:
			{ LA(2)!='/' }? '*'
		|	'\r''\n'		{newline();}
		|	'\r'			{newline();}
		|	'\n'			{newline();}
		|	~('*'|'\n'|'\r')
		)*
		"*/"
		{$setType(Token.SKIP);}
	;
	
protected ESC
	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	'0'..'3'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:	'0'..'7'
				)?
			)?
		|	'4'..'7'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
			)?
		)
	;
	
class ATTreeWalker extends TreeParser;

prog : #( BEGIN (expr)+ );

expr : #( APPLY fun:expr args:list )
	 | NAM
	 | NBR;

list : #( LIST (expr)+ );