package edu.vub.at.parser.test;

import edu.vub.at.parser.ATLexer;
import edu.vub.at.parser.ATParser;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import antlr.CommonAST;
import antlr.debug.misc.ASTFrame;
import junit.framework.TestCase;

public class ATParserTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ATParserTest.class);
	}

	/*
	 * Test method for 'edu.vub.at.parser.ATParser.program()'
	 */
	public void testProgram() {
	    try {
	        //DataInputStream input = new DataInputStream(System.in);
	      	 InputStream input = new StringBufferInputStream("1 + 2");
	      	
	        ATLexer lexer = new ATLexer(input); 
	        ATParser parser = new ATParser(lexer);
	        parser.program();

	        CommonAST parseTree = (CommonAST)parser.getAST();
	        System.out.println(parseTree.toStringList());
	        assertEquals(" ( semicolonlist ( + 1 2 ) ) null", parseTree.toStringList());
	        
	        //ASTFrame frame = new ASTFrame("The tree", parseTree);
	        //frame.setVisible(true);

	        //TinyTreeWalker walker = new TinyTreeWalker();
	        //double r = walker.expression(parseTree);
	        //System.out.println("Value: "+r);
	      } catch(Exception e) { System.err.println("Exception: "+e); }
	}

}
