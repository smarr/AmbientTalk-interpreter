header { package edu.vub.at.parser; }

class ATParser extends Parser;

options {
  k = 3;
  buildAST = true;
}

// Ambienttalk/2 programs consist of statements separated by semicolons
program : semicolonlist EOF;

// Any list of statements separated by semicolons can be seen as arguments to a begin native.
// TODO: allow an optional final semicolon
semicolonlist : statement (SMC! statement)* { #semicolonlist = #([AGBEGIN,"begin"], #semicolonlist); };

// Statements can be either definitions assignments or ordinary expressions
statement: definition | assignment | expression;

// Definitions start with ambienttalk/2's only reserved word def
// def <name> := <expression> defines a variable which can be assigned later.
// def <apl> { <body> } defines an immutable function.
// def <name>[size-exp] { <init-expression> } defines and initializes a new table of a given size
definition!: "def" nam:variable EQL val:expression { #definition = #([AGDEFFIELD,"define-field"], nam, val); }
           | "def" inv:parameterlist LBC bdy:semicolonlist RBC { #definition = #([AGDEFMETH,"define-method"], inv, bdy); }
           | "def" tbl:variable LBR siz:expression RBR LBC init:expression RBC { #definition = #([AGDEFTABLE,"define-table"], tbl, siz, init); };

// Parameter lists can be either canonical lists of the form <fun(a,b,c)>
// or keyworded lists of the form <foo: a bar: b>
parameterlist: canonicalparameterlist
             | keywordparameterlist;

canonicalparameterlist!: var:variable LPR (pars:variablelist)? RPR { #canonicalparameterlist = #([AGAPL,"apply"], var, pars); }; 

// See the documentation at the keywordlist rule for more information. The difference between
// keywordparameterlist and keywordlist lies in the ability to either parse a varlist or a commalist.
// EXAMPLE:
//  <foo: x bar: y> is parsed as:
// p = ( foo: ( symbol x ) ) ( bar: ( symbol y ) )
//  where p.getText() = foo:
//        p.getFirstChild() = (symbol x)
//        p.getNextSibling() = (bar: (symbol y))
// at each step in the algorithm, the current keyword is appended to the previously parsed keywords,
// while the only child of the current tree (the argument variable) is added to the arguments
// After processing all keywords, an APL tree is returned whose selector is the concatenation of the keywords and whose
// argument table contains the argument variables, e.g.:
// (apply (symbol foo:bar:) (table (symbol x) (symbol y)))
keywordparameterlist: (keywordparam)+ {
	AST currentKey = #keywordparameterlist;
	AST arguments = currentKey.getFirstChild(); // a pointer to the very first argument to which subsequent arguments are attached as siblings
	AST lastArgument = arguments;
	AST composedSelectorToken = new antlr.CommonAST();
	composedSelectorToken.setType(KEY);
	java.lang.StringBuffer composedSelector = new java.lang.StringBuffer(currentKey.getText());
	while (currentKey.getNextSibling() != null) {
	  currentKey = currentKey.getNextSibling();
	  composedSelector.append(currentKey.getText());
	  lastArgument.setNextSibling(currentKey.getFirstChild());
	  lastArgument = lastArgument.getNextSibling();
	}
	composedSelectorToken.setText(composedSelector.toString());
	#keywordparameterlist = #([AGAPL, "apply"], #([AGSYM,"symbol"], composedSelectorToken),
	    										  #([AGTAB,"table"], arguments));
};

keywordparam: KEY^ variable;

// Assignment of a variable is similar to its definition albeit without the word def.
// TODO the lhs of an assignment can also be (at least) a tabulation. 
assignment!: var:variable EQL val:expression { #assignment = #([AGASSFIELD, "field-set"], var, val); };

// Expressions are split up according to precedence. Ambienttalk/2's keyworded message
// sends have lowest priority and are therefore the highest applicable rule.
expression: keywordlist
          | rcv:comparand (opr:CMP^ arg:comparand)*;
// { #expression = #([AGSND,"send"], rcv, opr, arg); };

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
reference:! nbr:NBR { #reference = #([AGNBR,"number"],nbr); }
         |! frc:FRC { #reference = #([AGFRC,"fraction"],frc); }
         |! txt:TXT { #reference = #([AGTXT,"text"],txt); }
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
invoke_expression![AST functor]:
	  LPR (args:commalist)? RPR  { #invoke_expression = #([AGAPL,"apply"], functor, args); }
	| LBR idx:expression RBR { #invoke_expression = #([AGTBL,"table-get"], functor, idx); }
	| (DOT variable LPR | DOT KEY) => DOT apl:application { #invoke_expression = #([AGSND,"send"], functor, apl); }
	| DOT var:variable { #invoke_expression = #([AGSEL,"select"], functor, var);};

// Function application can be done using two distinct mechanisms, either using a 
// canonical format ( foobar( a1, a2 ) ) or using keywordlists (foo: a1 bar: a2).
// The latter format is used often in conjunction with blocks.
application: canonical
           | keywordlist;

canonical!: var:variable LPR (args:commalist)? RPR { #canonical = #([AGAPL,"apply"], var, args); }; 

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
subexpression!: LPR e:expression RPR { #subexpression = #e; };

// Inline syntax for nameless functions (lambdas or blocks)
block!:
      LBC pars:variablelist PIP body:semicolonlist RBC
		{ #block = #([AGCLO, "closure"], pars, body); }
	| LBC no_args_body:semicolonlist RBC
		{ #block = #([AGCLO, "closure"], null, no_args_body); };

// Inline syntax for table expressions
table!: LBR (slots:commalist)? RBR { #table = #slots; };

// Parses a list of expressions separated by commas. 
// USAGE: canonical function application (arguments) and inline tables
// @param generateImaginaryNode - generate an additional tree node?
commalist: expression (COM! expression)* 
	{ #commalist = #([AGTAB,"table"], #commalist); };

// Parses a list of variables
variablelist: variable (COM! variable)* { #variablelist = #([AGTAB, "table"], #variablelist); };

variable!: var:NAM { #variable = #([AGSYM,"symbol"], var); }
         | opr:operator { #variable = #([AGSYM, "symbol"], opr); };

operator: CMP | ADD | MUL | POW;



class ATLexer extends Lexer;

options {
  k = 3;
}

// OUTPUT TOKENS
// These tokens are never produced by the scanner itself as they are protected. 
// However they are used to annotate the resulting ANTLR tree, so that the walker
// can easily produce the correct Java ATAbstractGrammar elements.
// Each token definition aligns the token with its printed representation.
// Statements
protected AGBEGIN   : "begin";         // AGBegin(EXP[] exps)
// Definitions
protected AGDEFFIELD: "define-field";  // AGDefField(REF nam, EXP val)
protected AGDEFMETH : "define-method"; // AGDefMethod(REF sel, TBL arg, BGN bdy)
protected AGDEFTABLE: "define-table";  // AGDefTable(REF tbl, EXP siz, EXP ini)
// Assignments
protected AGASSFIELD: "field-set";     // AGAssignField (REF nam, EXP val)
protected AGASSTABLE: "table-set";     // AGAssignTable (REF nam, EXP idx, EXP val)
// Expressions
protected AGSND     : "send";          // AGMessageSend (EXP? rcv, REF sel, TBL arg)
protected AGSUP     : "super-send";    // AGSuperSend (REF sel, TBL arg)
protected AGAPL     : "apply";         // AGApplication (REF sel, TBL arg)
protected AGSEL     : "select";        // AGSelection (EXP? rcv, REF sel)
protected AGMSG     : "message";       // AGMessage (REF sel, TBL arg)
protected AGTBL     : "table-get";     // AGTabulation (EXP? tbl, EXP idx)
protected AGSYM     : "symbol";        // AGSymbol (TXT nam)
protected AGQUO     : "quote";         // AGQuote (EXP exp)
protected AGUNQ     : "unquote";       // AGUnquote (EXP exp)
protected AGUQS     : "unquote-splice";// AGUnquoteSplice (EXP exp)
// Literals
protected AGNBR     : "number";        // NATNumber (<nbr>)
protected AGFRC     : "fraction";      // NATFraction (<frc>)
protected AGTXT     : "text";          // NATText (<txt>)
protected AGTAB     : "table";         // NATTable (<tbl>)
protected AGCLO     : "closure";       // NATClosure (TBL arg, BGN bdy)

//add AGMeta/AGBase?

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

statement : #(AGBEGIN (statement)+ )
          | definition
          | assignment
          ;

definition: #(AGDEFFIELD nam:AGSYM val:expression)
          | #(AGDEFMETH #(AGAPL sel:AGSYM pars:AGTAB) bdy:AGBEGIN)
          | #(AGDEFTABLE tbl:AGSYM siz:expression ini:expression)
          ;

assignment: #(AGASSFIELD nam:AGSYM val:expression)
          | #(AGASSTABLE tbl:expression idx:expression val2:expression)
          ;

expression: #(AGSND sndrcv:expression sndsel:AGSYM sndargs:AGTAB)
          | #(AGSUP supsel:AGSYM supargs:AGTAB)
          | #(AGAPL aplsel:AGSYM aplargs:AGTAB)
          | #(AGSEL selrcv:expression selsel:AGSYM)
          | #(AGMSG msgsel:AGSYM msgargs:AGTAB)
          | #(AGTBL tblnam:expression tblidx:expression)
          | #(AGSYM symnam:AGTXT)
          | #(AGQUO quoexp:expression)
          | #(AGUNQ unqexp:expression)
          | #(AGUQS uqsexp:expression)
          | binop
          | literal
          ;

binop:      #(CMP cop1:expression cop2:expression)
          | #(ADD aop1:expression aop2:expression)
          | #(MUL mop1:expression mop2:expression)
          | #(POW pop1:expression pop2:expression)
          ;
          
literal:    #(AGNBR nbr:INT)
          | #(AGFRC frc:FRC)
          | #(AGTXT txt:TXT)
          | #(AGTAB (expression)* )
          | #(AGCLO par:AGTAB body:AGBEGIN)
          ;