package edu.vub.at.parser.test;

import edu.vub.at.parser.ATLexer;
import edu.vub.at.parser.ATParser;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import antlr.CommonAST;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.debug.misc.ASTFrame;
import junit.framework.TestCase;

public class ATParserTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ATParserTest.class);
	}

	private void testParse(String parserInput, String expectedOutput) {
		try {
			InputStream input = new StringBufferInputStream(parserInput);
			
			ATLexer lexer = new ATLexer(input);
			ATParser parser = new ATParser(lexer);
			parser.program();

			CommonAST parseTree = (CommonAST)parser.getAST();
			System.out.println(parseTree.toStringList());
			assertEquals(expectedOutput, parseTree.toStringList());
		} catch(Exception e) { 
			fail("Exception: "+e); 
		}
	}
	
	/**
	 * Basic canonical function application test.
	 * @covers canonical application
	 */
	public void testSimple() {
		testParse(
				"f(1, 2)",
				" ( begin ( apply f ( list 1 2 ) ) ) null");
	}
	/**
	 * Tests grammar support for invocations. 
	 * @covers selection invocation exp
	 * @covers canonical send invocation exp
	 * @covers keywordlist send invocation exp 
	 */
	public void testInvocation() {
	    testParse(
	    		"object.no().demeter().law",
	    		" ( begin ( selection ( invocation ( invocation object ( apply no ) ) ( apply demeter ) ) law ) ) null");
	    testParse(
	    		"object.keyworded: message send: test",
	    		" ( begin ( invocation object ( keywordlist ( keyworded: message ) ( send: test ) ) ) ) null");
	}
	
	/**
	 * Tests grammar support for currying invocations - e.g. following references
	 * with an arbitrary amount of invocation expressions.
	 * @covers table
	 * @covers tabulation invocation exp
	 * @covers canonical application
	 */
	public void testCurrying() {
	    testParse(
	    		"[ { display: \"test\" }, { x, y | x < y } ][2](a ,b)",
	    		" ( begin ( apply ( table-get ( table ( block ( begin ( keywordlist ( display: \"test\" ) ) ) ) ( block ( vars x y ) ( begin ( < x y ) ) ) ) 2 ) ( list a b ) ) ) null");		
	}
	
	/**
	 * Test default behaviour for trailing keywords, and tests with correct nesting.
	 * @covers keywordlist application
	 */
	public void testTrailingKeywords() {
	    testParse(
	    		"if: c1 then: if: c2 then: a else: b", 
	    		" ( begin ( keywordlist ( if: c1 ) ( then: ( keywordlist ( if: c2 ) ( then: a ) ( else: b ) ) ) ) ) null");
	    testParse(
	    		"if: c1 then: ( if: c2 then: a ) else: b", 
	    		" ( begin ( keywordlist ( if: c1 ) ( then: ( keywordlist ( if: c2 ) ( then: a ) ) ) ( else: b ) ) ) null");
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
				" ( begin ( def point ( keywordlist ( object: ( block ( vars x y ) ( begin ( def ( apply getX ) ( begin x ) ) ( def ( apply getY ) ( begin y ) ) ( def ( keywordlist ( withX: anX ) ( Y: aY ) ) ( begin ( := x anX ) ( := y anY ) ) ) ) ) ) ) ) ) null");
	}
	
}
