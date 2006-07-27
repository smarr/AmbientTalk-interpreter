package edu.vub.at.parser.test;

import edu.vub.at.parser.ATLexer;
import edu.vub.at.parser.ATParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import antlr.CommonAST;
import junit.framework.TestCase;

public class ATParserTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ATParserTest.class);
	}

	private void testParse(String parserInput, String expectedOutput) {
		try {
			InputStream input = new ByteArrayInputStream(parserInput.getBytes());
			ATLexer lexer = new ATLexer(input);
			ATParser parser = new ATParser(lexer);
			parser.program();

			CommonAST parseTree = (CommonAST)parser.getAST();
			System.out.println(parseTree.toStringList());
			assertEquals((expectedOutput + "null").replace(" ",""), parseTree.toStringList().replace(" ",""));
		} catch(Exception e) {
			fail("Exception: "+e); 
		}
	}
	
	/**
	 * Tests for the validity of all statement abstract grammar elements.
	 * @covers all individual statement abstract grammar elements
	 */
	public void testStatementGrammar() {
		testParse("a;b;c",
				 "(begin (symbol a) (symbol b) (symbol c))");
		testParse("def x := 5",
				 "(begin (define-field (symbol x) (number 5)))");
		testParse("def f(a,b) { 5 }",
				 "(begin (define-method (apply (symbol f) (table (symbol a) (symbol b))) (begin (number 5))))");
		testParse("def foo: x bar: y { 5 }",
				 "(begin (define-method (apply (symbol foo:bar:) (table (symbol x) (symbol y))) (begin (number 5))))");
		testParse("def t[5] { a }",
				 "(begin (define-table (symbol t) (number 5) (symbol a)))");
		testParse("x := 7",
				 "(begin (field-set (symbol x) (number 7)))");
		testParse("x[5] := 7",
		          "(begin (table-set (table-get ( symbol x ) ( number 5 ) ) (number 7)))");
	}
	
	/**
	 * Tests for the validity of all expression abstract grammar elements.
	 * @covers all individual expression abstract grammar elements
	 */
	public void testExpressionGrammar() {
		testParse("o.m(a,b)",
				 "(begin (send (symbol o) (apply (symbol m) (table (symbol a) (symbol b)))))");
		testParse("o.foo: a bar: b",
				 "(begin (send (symbol o) (apply (symbol foo:bar:) (table (symbol a) (symbol b)))))");
		testParse("super.m(a)",
				 "(begin (send super (apply (symbol m) (table (symbol a)))))");
		testParse("m(a,b)",
				 "(begin (apply (symbol m) (table (symbol a) (symbol b))))");
		testParse("o.m",
		          "(begin (select (symbol o) (symbol m)))");
		testParse(".m(a,b)",
				 "(begin (message (apply (symbol m) (table (symbol a) (symbol b)))))");
		testParse("t[a]",
		          "(begin (table-get (symbol t) (symbol a)))");
		testParse("f()[a+b]",
                  "(begin (table-get (apply (symbol f) (table)) (+ (symbol a) (symbol b))))");
		testParse("a",
                   "(begin (symbol a))");
		testParse("`(t[a])",
                   "(begin (quote (table-get (symbol t) (symbol a))))");
		testParse("#(t[a])",
                   "(begin (unquote (table-get (symbol t) (symbol a))))");
		testParse("#@(t[a])",
                   "(begin (unquote-splice (table-get (symbol t) (symbol a))))");
	}
	
	/**
	 * Tests for the validity of infix operator expressions and operator symbols in general
	 * @covers infix operators
	 */
	public void testOperatorGrammar() {
		testParse("1 + 2 + 3",
				 "(begin (+ (+ (number 1) (number 2)) (number 3)))");
		testParse("a * b^3 + c < d / e - f",
		          "(begin (< (+ (* (symbol a) (^ (symbol b) (number 3))) (symbol c)) (- (/ (symbol d) (symbol e)) (symbol f)))) ");
	    testParse("+(1,2)",
	    		     "(begin (apply (symbol +) (table (number 1) (number 2))))");
	    testParse("a.+(2)",
	              "(begin (send (symbol a) (apply (symbol +) (table (number 2)))))");
	    
        // the following expression is very tricky!
	    // == +(.m(1)) , i.e. + applied to a first-class message, and NOT a message send
	    // if unary operators can be invoked, then syntax like '-1' will fail as numbers are not invocations
	    testParse("+.m(1)",
                  "(begin (apply (symbol +) (table (message (apply (symbol m) (table (number 1)))))))");
	    testParse("-5 + a",
                  "(begin (+ (apply (symbol -) (table (number 5))) (symbol a)))");
	}
	
	/**
	 * Tests syntax for literals
	 * @covers literal numbers, fractions, text, tables and closures
	 */
	public void testLiteralGrammar() {
		testParse("-12345",
				 "(begin (apply (symbol -) (table (number 12345))))");
		testParse("1.05",
		          "(begin (fraction 1.05))");
		testParse("-5.04e-10",
                   "(begin (apply (symbol -) (table (fraction 5.04e-10))))");
		testParse("\"hello  \\tworld\"",
				 "(begin (text \"hello  \\tworld\"))");
		testParse("[a,b,c]",
				 "(begin (table (symbol a) (symbol b) (symbol c)))");
		testParse("[]",
				 "(begin (table))");
		testParse("{ x, y | x + y }",
				 "(begin (closure (table (symbol x) (symbol y)) (begin (+ (symbol x) (symbol y)))))");
		testParse("{ a := 2; b }",
				 "(begin (closure (table) (begin (field-set (symbol a) (number 2)) (symbol b))))");
	}

	/**
	 * Tests grammar support for message sends. 
	 * @covers selection invocation exp
	 * @covers canonical send invocation exp
	 * @covers keywordlist send invocation exp 
	 */
	public void testMessageSending() {
	    testParse(
	    		"object.no().demeter().law",
	    		" ( begin ( select ( send ( send (symbol object) ( apply (symbol no) (table)) ) ( apply (symbol demeter) (table) ) ) (symbol law) ) )");
	    testParse(
	    		"object.keyworded: message send: test",
	    		" ( begin ( send (symbol object) ( apply ( symbol keyworded:send:) (table (symbol message) (symbol test) ) ) ) )");
	}
	
	/**
	 * Tests grammar support for currying invocations - e.g. following references
	 * with an arbitrary amount of send expressions.
	 * @covers table
	 * @covers tabulation send exp
	 * @covers canonical application
	 */
	public void testCurrying() {
	    testParse(
	    		"[ { display: \"test\" }, { x, y | x < y } ][2](a ,b)",
	    		" ( begin ( apply ( table-get ( table ( closure (table ) ( begin ( apply (symbol display:) (table (text \"test\" ) ) ) ) ) ( closure ( table (symbol x) (symbol y) ) ( begin ( < (symbol x) (symbol y) ) ) ) ) (number 2) ) ( table (symbol a) (symbol b) ) ) )");		
	}
	
	/**
	 * Test default behaviour for trailing keywords, and tests with correct nesting.
	 * @covers keywordlist application
	 */
	public void testTrailingKeywords() {
	    testParse(
	    		"if: c1 then: if: c2 then: a else: b", 
	    		" ( begin ( apply (symbol if:then:) (table (symbol c1) ( apply (symbol if:then:else:) (table (symbol c2) (symbol a) (symbol b) ) ) ) ) )");
	    testParse(
	    		"if: c1 then: ( if: c2 then: a ) else: b", 
	    		" ( begin ( apply (symbol if:then:else:) (table (symbol c1) ( apply (symbol if:then:) (table (symbol c2) (symbol a) ) ) (symbol b) ) ) )");
	}

	/**
	 * Tests the definition of a prototype point object.
	 * @covers definition
	 * @covers assignment
	 */
	public void testPointDefinition() {
		testParse(
				"def point := object: { x, y | \n" +
				"  def getX() { x }; \n" +
				"  def getY() { y }; \n" +
				"  def withX: anX Y: aY { \n" +
				"    x := anX; \n" +
				"    y := anY \n" +
				"  } \n" +
				"} \n",
				"(begin" +
				  "(define-field (symbol point)" +
				                "(apply (symbol object:) (table (closure (table (symbolx) (symboly))" +
				                                               "(begin (define-method (apply (symbol getX) (table)) (begin (symbol x)))" +
				                                                      "(define-method (apply (symbol getY) (table)) (begin (symbol y)))" +
				                                                      "(define-method (apply (symbol withX:Y:) (table (symbol anX) (symbol aY)))" +
				                                                                      "(begin (field-set (symbol x) (symbol anX))" +
				                                                                             "(field-set (symbol y) (symbol anY))))))))))");
	}
	
}
