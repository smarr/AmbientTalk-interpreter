class ATParser extends Parser;

options {
  k = 3;
  buildAST = true;
}

program : exps:semicolonlist EOF;

semicolonlist : statement (SMC! statement)* { #semicolonlist = #([SMC,"semicolonlist"], #semicolonlist); };

statement: definition | assignment | expression;

definition: "def"^ (NAM EQL! expression | application LBR! semicolonlist RBR!);

assignment: NAM EQL^ expression;

expression: comparand (CMP^ comparand)*;

comparand: term (ADD^ term)*;

term: factor (MUL^ factor)*;

factor: invocation (POW^ invocation)*;

invocation: reference (invoke_expression)*;

reference: NBR^
         | FRC^
         | TXT^
         | ((variable LPR) | KEY) => application
         | variable
         | subexpression
         | block
         | table;

invoke_expression: LPR (commalist)? RPR
                 | LBC expression RBC
                 | (DOT variable LPR) => DOT application
                 | DOT variable;

application: variable LPR (commalist)? RPR
		   | keywordlist;

keywordlist: (KEY expression)
			 (options {
				warnWhenFollowAmbig = false;
			 } : KEY expression)* { #keywordlist = #([KEY,"keywordlist"], #keywordlist); };

subexpression!: LPR e:expression RPR { #subexpression = #e; };

block: (LBR (variablelist PIP)) => LBR variablelist PIP semicolonlist RBR
     | LBR sclist2:semicolonlist RBR;

table: LBC^ (commalist)? RBC!;

commalist: expression (COM expression)*;

variablelist: variable (COM variable)*;

variable: NAM | operator;

operator: CMP | ADD | MUL | POW;



class ATLexer extends Lexer;

options {
  k = 3;
}

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

LBC: '[';
RBC: ']';

LBR: '{';
RBR: '}';

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