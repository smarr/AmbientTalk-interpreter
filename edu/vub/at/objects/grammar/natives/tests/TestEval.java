package edu.vub.at.objects.grammar.natives.tests;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.test.ATParserTest;

import junit.framework.TestCase;

/**
 * Tests the ATObject.meta_eval(ATContext) method for different kinds of abstract grammar elements.
 */
public class TestEval extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(TestEval.class);
	}
	
	private final NATNumber atThree_ = NATNumber.atValue(3);
	private final AGSymbol atX_ = AGSymbol.alloc(NATText.atValue("x"));
	
	private ATContext ctx_;
	
	protected void setUp() throws Exception {
		ATObject root = new NATObject(NATNil._INSTANCE_); // object with no dyn or lex parent
		ATObject supr = new NATObject(root); // supr has root as lex parent
		ATObject self = new NATObject(supr, root); // self has root as lex parent and supr as dyn parent
		ATObject scope = new NATObject(self); // scope has no dyn parent and is nested within self
		ctx_ = new NATContext(scope, self, supr);
	}

	protected void tearDown() throws Exception {
		ctx_ = null;
	}
	
	public void evalAndCompareTo(String input, ATObject output) {
        try {
			ATAbstractGrammar ptree = ATParserTest.parseProgram(input);
			assertEquals(ptree.meta_eval(ctx_), output);
		} catch (XParseError e) {
			fail("Parse error: "+e.getMessage());
		} catch (NATException e) {
			e.printStackTrace();
			fail("Eval error: "+e.getMessage());
		}
	}

	public void testBegin() {
        evalAndCompareTo("1; 2; 3", atThree_);
        evalAndCompareTo("3", atThree_);
	}
	
	public void testDefField() throws NATException {
        evalAndCompareTo("def x := 3", atThree_);
        assertEquals(atThree_, ctx_.getLexicalScope().meta_lookup(atX_));
	}

}
