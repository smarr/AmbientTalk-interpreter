package edu.vub.at;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.parser.NATParser;

import junit.framework.TestCase;

public abstract class AmbientTalkTest extends TestCase {

	protected ATContext ctx_;
	
	public AmbientTalkTest() {
		ATObject supr = new NATObject();
		ATObject self =
			new NATObject(supr, NATObject._SHARES_A_); // self has root as lex parent and supr as dyn parent
		ATObject scope = new NATObject(self); // scope has no dyn parent and is nested within self
		ctx_ = new NATContext(scope, self, supr);
	}

	public ATObject evalAndReturn(String input) {
        try {
			ATAbstractGrammar ptree = 
				NATParser._INSTANCE_.base_parse(NATText.atValue(input));
			return ptree.meta_eval(ctx_);
		} catch (XParseError e) {
			fail("Parse error: "+e.getMessage());
		} catch (NATException e) {
			e.printStackTrace();
			fail("Eval error: "+e.getMessage());
		}
		return null;
	}
	
	public void evalAndCompareTo(String input, ATObject output) {
		ATObject result = evalAndReturn(input);
		if (result != null) {
			assertEquals(output, result);
		}
	}
	
	public void evalAndCompareTo(String input, String output) {
		try {
			ATObject result = evalAndReturn(input);
			if (result != null) {
				assertEquals(output, result.meta_print().javaValue);
			}
		} catch (XTypeMismatch e) {
			fail(e.getMessage());
		}
	}
	
	public void printedEquals(ATObject input, String expected) {
		try {
			assertEquals(expected, input.meta_print().javaValue);
		} catch (XTypeMismatch e) {
			fail(e.getMessage());
		}
	}
	
}
