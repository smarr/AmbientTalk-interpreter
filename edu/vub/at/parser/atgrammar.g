header { package edu.vub.at.parser; }

class ATParser extends Parser;

options {
  k = 2;
  buildAST = true;
}

{ /* begin Parser class preamble */

// The keywords2canonical auxiliary function transforms keyworded message sends or parameter lists into
// their canonical equivalent.
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
AST keywords2canonical(AST keywordparameterlist) {
	AST currentKey = keywordparameterlist;
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
	// return #([AGAPL, "apply"], #([AGSYM,"symbol"], composedSelectorToken), #([AGTAB,"table"], arguments));
	return (AST)astFactory.make( (new ASTArray(3)).add(astFactory.create(AGAPL,"apply")).add((AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(AGSYM,"symbol")).add(composedSelectorToken))).add((AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(AGTAB,"table")).add(arguments))));
};

} /* end Parser class preamble */

// Ambienttalk/2 programs consist of statements separated by semicolons
program : semicolonlist EOF;

// Any list of statements separated by semicolons can be seen as arguments to a begin native.
// TODO: allow an optional final semicolon
semicolonlist : statement (SMC! statement)* { #semicolonlist = #([AGBEGIN,"begin"], #semicolonlist); };

// Statements can be either definitions assignments or ordinary expressions
statement: ("def"! definition)
         | (variable EQL) => fieldassignment
         | (invocation EQL) => tableassignment
         | expression;

// Definitions start with ambienttalk/2's only reserved word def
// def <name> := <expression> defines a variable which can be assigned later.
// def <apl> { <body> } defines an immutable function.
// def <name>[size-exp] { <init-expression> } defines and initializes a new table of a given size
definition!: nam:variable EQL val:expression { #definition = #([AGDEFFIELD,"define-field"], nam, val); }
           | inv:parameterlist LBC bdy:semicolonlist RBC { #definition = #([AGDEFMETH,"define-method"], inv, bdy); }
           | tbl:variable LBR siz:expression RBR LBC init:expression RBC { #definition = #([AGDEFTABLE,"define-table"], tbl, siz, init); };

// Parameter lists can be either canonical lists of the form <fun(a,b,c)>
// or keyworded lists of the form <foo: a bar: b>
parameterlist: canonicalparameterlist
             | keywordparameterlist;

canonicalparameterlist!: var:variable LPR (pars:variablelist)? RPR { #canonicalparameterlist = #([AGAPL,"apply"], var, pars); }; 

// See the documentation at the keywordlist rule for more information. The difference between
// keywordparameterlist and keywordlist lies in the ability to either parse a varlist or a commalist.
keywordparameterlist: (keywordparam)+ {
	#keywordparameterlist = keywords2canonical(#keywordparameterlist);
};

keywordparam: KEY^ variable;

// Assignment of a variable is similar to its definition albeit without the word def.
// TODO tabulation assignment requires the parser to look ahead arbitrarily far for a ':=' -> inefficient 
fieldassignment!: var:variable EQL val:expression { #fieldassignment = #([AGASSFIELD, "field-set"], var, val); };

tableassignment!: tbl:invocation EQL ass:expression { #tableassignment = #([AGASSTABLE,"table-set"], tbl, ass); };

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
invocation!: r:reference c:curried_invocation[#r] { #invocation = #c; };

// References are the most fundamental elements of the language, namely primitive 
// values (numbers and  strings), variables, blocks, inline tables and subexpressions
// A reference can also be a quotation or a first-class message creation operation
reference:! nbr:NBR { #reference = #([AGNBR,"number"],nbr); }
         |! frc:FRC { #reference = #([AGFRC,"fraction"],frc); }
         |! txt:TXT { #reference = #([AGTXT,"text"],txt); }
         | quotation
         | variable
         | DOT! message
         | LPR! subexpression
         | LBC! block
         | LBR! table;

// A quotation is a quoted, unquoted or unquote-spliced piece of source code:
// `( statement )
// #( expression )
// #@( expression )
// TODO: appears to introduce ambiguity for blocks
quotation!: BQU LPR stmt:statement RPR { #quotation = #([AGQUO,"quote"],stmt); }
         | HSH LPR uexp:expression RPR { #quotation = #([AGUNQ,"unquote"], uexp); }
         | HSH CAT LPR usexp:expression RPR { #quotation = #([AGUQS,"unquote-splice"], usexp); };

// Curried invocations eagerly consume all subsequent ( [ . tokens. If such tokens are
// available a single invocation is parsed passing on the received functor (which will
// be applied, tabulated, or sent a message). The result of this parsing step is passed
// to be curried even further. When no appropriate tokens are left, the passed functor 
// is returned.
curried_invocation![AST functor]:
      (LPR|LBR|DOT) => i:invoke_expression[functor] c:curried_invocation[#i] { #curried_invocation = #c; }
	| {#curried_invocation = #functor; };

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
			 } : singlekeyword)* { #keywordlist = keywords2canonical(#keywordlist); };

// This rule groups a keyword and the adjoined expression into a single tree element.
singlekeyword: KEY^ expression;

// First-class message creation syntax: .m() or .key:val
message!: apl:application { #message = #([AGMSG,"message"], apl); };

// This rule unwraps an expression of its delimiter parentheses.
subexpression!: e:expression RPR { #subexpression = #e; };

// Inline syntax for nameless functions (lambdas or blocks)
block!: pars:variablelist PIP body:semicolonlist RBC
		 { #block = #([AGCLO, "closure"], pars, body); }
	  | no_args_body:semicolonlist RBC
		 { #block = #([AGCLO, "closure"], #([AGTAB,"table"], #([AGTAB]) ), no_args_body); };

// Inline syntax for table expressions
// TODO: test whether this definition works with the empty table
table!: (slots:commalist)? RBR { #table = #slots; };

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
protected AGASSTABLE: "table-set";     // AGAssignTable (INV tbl, EXP idx, EXP val)
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

BQU: '`';
HSH: '#';
CAT: '@';


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
	
{ import edu.vub.at.objects.ATObject;
  import edu.vub.at.objects.natives.*;
  import edu.vub.at.objects.grammar.*;
  import edu.vub.at.objects.natives.grammar.*; }
class ATTreeWalker extends TreeParser;
{ // begin TreeWalker preamble

// this auxiliary function converts operator syntax such as <a+b> into a message send of the form <a.+(b)>
public AGMessageSend operatorToSend(AST opr, ATExpression receiver, ATExpression operand) {
	return new AGMessageSend(receiver,
	                         AGSymbol.alloc(NATText.atValue(opr.getText())),
	                         new NATTable(new ATObject[] { operand }));
}

// transforms a sibling AST into an object array
// e.g. ((symbol a) (symbol b) (symbol c))
//  => ATObject[] { (symbol a) (symbol b) (symbol c) }
public ATObject[] transformAstToArray(AST list) {
	AST next = list;
	int size = 1;
	// first, calculate the number of sibling nodes
	while (next.getNextSibling() != null) {
	  next = next.getNextSibling();
	  size++;
	}
	// once size is known, create array and fill it
	ATObject[] arr = new ATObject[size];
	next = list;
	for (int i=0; i < size; i++) {
		// FIXME arr[i] = next;
		next = next.getNextSibling();
	}
	return arr;
}
	
} // end TreeWalker preamble

program returns [NATAbstractGrammar ag] { ag = null; }
          : ag=begin
          ;

statement returns [ATStatement stmt] { stmt = null; }
          : stmt=definition
          | stmt=assignment
          | stmt=expression
          ;

definition returns [ATDefinition def]
  { def = null;
  	ATSymbol nam;
  	NATTable pars;
  	ATExpression idx, val;
  	ATBegin bdy; }
          : #(AGDEFFIELD nam=symbol val=expression) { def = new AGDefField(nam, val); }
          | #(AGDEFMETH #(AGAPL nam=symbol pars=table) bdy=begin) { def = new AGDefMethod(nam, pars, bdy); }
          | #(AGDEFTABLE #(AGTBL nam=symbol idx=expression) val=expression) { def = new AGDefTable(nam,idx,val); }
          ;

assignment returns [ATAssignment ass]
  { ass = null;
    ATSymbol nam;
    ATExpression tbl, val, idx; }
          : #(AGASSFIELD nam=symbol val=expression) { ass = new AGAssignField(nam, val); }
          | #(AGASSTABLE #(AGTBL tbl=expression idx=expression) val=expression) { ass = new AGAssignTable(tbl, idx, val); }
          ;

expression returns [ATExpression exp]
  { exp = null;
  	ATExpression rcv, idx, qexp;
  	ATStatement qstmt;
  	ATSymbol sel;
  	NATTable arg; }
          : #(AGSND rcv=expression sel=symbol arg=table) { exp = new AGMessageSend(rcv,sel,arg); }
          | #(AGSUP sel=symbol arg=table) { exp = new AGSuperSend(sel,arg); }
          | #(AGAPL sel=symbol arg=table) { exp = new AGApplication(sel, arg); }
          | #(AGSEL rcv=expression sel=symbol) { exp = new AGSelection(rcv, sel); }
          | #(AGMSG #(AGAPL sel=symbol arg=table)) { exp = new AGMessageCreation(sel, arg); }
          | #(AGTBL rcv=expression idx=expression) { exp = new AGTabulation(rcv, idx); }
          | #(AGQUO qstmt=statement) { exp = new AGQuote(qstmt); }
          | #(AGUNQ qexp=expression) { exp = new AGUnquote(qexp); }
          | #(AGUQS qexp=expression) { exp = new AGUnquoteSplice(qexp); }
          | exp=symbol
          | exp=binop
          | exp=literal
          ;
          
binop returns [ATMessageSend snd]
  { snd = null;
    ATExpression exp1, exp2; }
          : #(cmp:CMP exp1=expression exp2=expression) { snd = operatorToSend(cmp, exp1, exp2); }
          | #(add:ADD exp1=expression exp2=expression) { snd = operatorToSend(add, exp1, exp2); }
          | #(mul:MUL exp1=expression exp2=expression) { snd = operatorToSend(mul, exp1, exp2); }
          | #(pow:POW exp1=expression exp2=expression) { snd = operatorToSend(pow, exp1, exp2); }
          ;
          
literal returns[ATExpression lit]
  { lit = null;
  	NATTable par;
  	ATBegin body; }
          : #(AGNBR nbr:INT) { lit = NATNumber.atValue(Integer.parseInt(nbr.getText())); }
          | #(AGFRC frc:FRC) { lit = NATFraction.atValue(Double.parseDouble(frc.getText())); }
          | #(AGTXT txt:TXT) { lit = NATText.atValue(txt.getText()); }
          | lit=table
          | #(AGCLO par=table body=begin) { lit = new AGClosureLiteral(par, body); }
          ;
          
symbol returns [AGSymbol sym] { sym = null; }
          : #(AGSYM txt:NAM) { sym = AGSymbol.alloc(NATText.atValue(txt.getText())); }
          ;

// TODO: find a way to extract an array from a (...)* or (...)+ construct
table returns [NATTable tab]
  { tab = null; ATExpression expr; }
          : #(AGTAB (expr=expression)* ) { 
          	System.out.println(#table.toStringList());
          	tab = new NATTable(null); }
          ;
          
begin returns [AGBegin bgn] { bgn = null; ATStatement stmt; }
          : #(AGBEGIN (stmt=statement)+ ) {
          	System.out.println(#begin.toStringList());
          	bgn = new AGBegin(new NATTable(null)); }
          ;