package edu.vub.at;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.NATParser;

import junit.framework.TestCase;


public class AmbientTalkTestCase extends TestCase {

	protected ATContext ctx_ = null; 
	
	protected ATClosure unittest_ = new JavaClosure(null) {
		public ATObject base_apply(ATTable arguments) throws NATException {
			ATClosure body = arguments.base_at(NATNumber.ONE).asClosure();
			return OBJUnit._INSTANCE_.base_unittest_(body);
		};
	};
	
	protected void evaluateInput(String input, ATContext ctx) throws NATException {
		try {
			ATAbstractGrammar ag = NATParser._INSTANCE_.base_parse(NATText.atValue(input));
			
			// Evaluate the corresponding tree of ATAbstractGrammar objects
			ag.meta_eval(ctx);
		} catch(XParseError e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
	}
	
	protected void setUp() throws Exception {
		ATObject root = new NATObject(NATNil._INSTANCE_); // object with no dyn or lex parent
		ATObject supr = new NATObject(root); // supr has root as lex parent
		ATObject self = new NATObject(supr, root, NATObject._SHARES_A_); // self has root as lex parent and supr as dyn parent
		ATObject scope = new NATObject(self); // scope has no dyn parent and is nested within self
		
		
		self.meta_defineField(AGSymbol.alloc("unittest:"), unittest_);
		
		self.meta_defineField(AGSymbol.alloc("unit"), OBJUnit._INSTANCE_);
				
		ctx_ = new NATContext(scope, self, supr);
	}
	
	protected void tearDown() throws Exception {
		ctx_ = null;
	}
	
//	public void testUnitTestFramework() {
//		try {
//			evaluateInput("unit.fail: \"This test should fail.\"", ctx_);
//		} catch (NATException e) {
//			fail("exception : " + e);
//		} catch (AssertionFailedError e) {
//			// ok. this test is supposed to fail
//		}
//
//		try {
//			evaluateInput("unittest: { self.fail: \"This test should fail.\" }", ctx_);
//		} catch (NATException e) {
//			e.printStackTrace();
//			fail("exception : " + e);
//		} catch (AssertionFailedError e) {
//			// ok. this test is supposed to fail
//		}
//	
//	}
	
}
