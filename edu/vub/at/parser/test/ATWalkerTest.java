package edu.vub.at.parser.test;

import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;
import edu.vub.at.parser.ATLexer;
import edu.vub.at.parser.ATParser;
import edu.vub.at.parser.ATTreeWalker;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;
import antlr.CommonAST;

public class ATWalkerTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ATWalkerTest.class);
	}
	
	private void testWalker(String walkerInput) {
        try {
            ATLexer lexer = new ATLexer(new ByteArrayInputStream(walkerInput.getBytes()));
            ATParser parser = new ATParser(lexer);
            // Parse the input expression
            parser.program();
            CommonAST t = (CommonAST)parser.getAST();
            // Print the resulting tree out in LISP notation
            System.out.println(t.toStringList());
            ATTreeWalker walker = new ATTreeWalker();
            // Traverse the tree created by the parser
            NATAbstractGrammar ag = walker.program(t);
            System.out.println("final = "+ag);
        } catch(Exception e) {
            fail("exception: "+e);
        }
	}
	
	public void testSimple() {
		testWalker("a ; b; c");
	}

}
