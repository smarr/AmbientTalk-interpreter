package edu.vub.at.objects.natives.grammar.test;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATAsyncMessage;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATSuperObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGSplice;
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
	private final AGSymbol atY_ = AGSymbol.alloc(NATText.atValue("y"));
	private final AGSymbol atZ_ = AGSymbol.alloc(NATText.atValue("z"));
	private final AGSymbol atM_ = AGSymbol.alloc(NATText.atValue("m"));
	private final AGSymbol atFooBar_ = AGSymbol.alloc(NATText.atValue("foo:bar:"));
	
	private ATContext ctx_;
	
	protected void setUp() throws Exception {
		ATObject root = new NATObject(NATNil._INSTANCE_); // object with no dyn or lex parent
		ATObject supr = new NATObject(root); // supr has root as lex parent
		ATObject self = new NATObject(supr, root, NATObject._SHARES_A_); // self has root as lex parent and supr as dyn parent
		ATObject scope = new NATObject(self); // scope has no dyn parent and is nested within self
		ctx_ = new NATContext(scope, self, supr);
	}

	protected void tearDown() throws Exception {
		ctx_ = null;
	}

	public ATObject evalAndReturn(String input) {
        try {
			ATAbstractGrammar ptree = ATParserTest.parseProgram(input);
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

	// statements
	
	public void testBegin() {
        evalAndCompareTo("1; 2; 3", atThree_);
        evalAndCompareTo("3", atThree_);
	}
	
	// definitions
	
	public void testDefField() throws NATException {
        evalAndCompareTo("def x := 3", NATNil._INSTANCE_);
        assertEquals(atThree_, ctx_.getLexicalScope().meta_lookup(atX_));
	}
	
	public void testDefFunction() throws NATException {
        evalAndCompareTo("def x() { 3 }", NATNil._INSTANCE_);
        try {
        	  ATClosure clo = ctx_.getLexicalScope().meta_lookup(atX_).asClosure();
        	  assertEquals(atX_, clo.getMethod().getName());
        } catch(XSelectorNotFound e) {
        	  fail("broken definition:"+e.getMessage());
        }
	}
	
	public void testDefTable() throws NATException {
        evalAndCompareTo("def x[3] { 3 }", NATNil._INSTANCE_);
        try {
        	  ATObject tab = ctx_.getLexicalScope().meta_lookup(atX_);
        	  assertEquals(atThree_, tab.asTable().base_getLength());
        	  assertEquals(atThree_, tab.asTable().base_at(NATNumber.ONE));
        } catch(XSelectorNotFound e) {
        	  fail("broken definition:"+e.getMessage());
        }
	}
	
	// assignments
	
	public void testAssignField() throws NATException {
		// def x := nil
		ctx_.getLexicalScope().meta_defineField(atX_, NATNil._INSTANCE_);
		
        evalAndCompareTo("x := 3", NATNil._INSTANCE_);
        assertEquals(atThree_, ctx_.getLexicalScope().meta_lookup(atX_));
	}
	
	public void testAssignTable() throws NATException {
		// def x[2] { nil }
		ATTable table = new NATTable(new ATObject[] { NATNil._INSTANCE_, NATNil._INSTANCE_ });
		ctx_.getLexicalScope().meta_defineField(atX_, table);
		
        evalAndCompareTo("x[1] := 3", NATNil._INSTANCE_);
        assertEquals(atThree_, table.base_at(NATNumber.ONE));
	}
	
	// expressions
	
	public void testSymbolReference() throws NATException {
		// def x := 3
		ctx_.getLexicalScope().meta_defineField(atX_, atThree_);
        evalAndCompareTo("x", atThree_);
	}
	
	public void testTabulation() throws NATException {
		// def x := [3,1]
		ATTable table = new NATTable(new ATObject[] { atThree_, NATNumber.ONE });
		ctx_.getLexicalScope().meta_defineField(atX_, table);
		
        evalAndCompareTo("x[1]", atThree_);
        evalAndCompareTo("x[2]", NATNumber.ONE);
	}
	
	public void testClosureLiteral() throws NATException {
	  ATClosure clo = evalAndReturn("{| x, y | 3 }").asClosure();
	  ATSymbol nam = clo.getMethod().getName();
	  ATTable arg = clo.getMethod().getArguments();
	  ATAbstractGrammar bdy = clo.getMethod().getBody();
	  ATContext ctx = clo.getContext();
	  assertEquals(AGSymbol.alloc(NATText.atValue("lambda")), nam);
	  assertEquals(atX_, arg.base_at(NATNumber.ONE));
	  assertEquals(atY_, arg.base_at(NATNumber.atValue(2)));
	  assertEquals(atThree_, bdy.asBegin().getStatements().base_at(NATNumber.ONE));
	  assertEquals(ctx_, ctx);
	}
	
	public void testSelfReference() throws NATException {
        evalAndCompareTo("self", ctx_.getSelf());
	}
	
	public void testSuperReference() throws NATException {
        NATSuperObject supref = (NATSuperObject) evalAndReturn("super");
        assertEquals(ctx_.getSelf(), supref.getReceiver());
        assertEquals(ctx_.getSuper(), supref.getLookupFrame());
	}
	
	public void testSelection() throws NATException {
		// def x := object: { def y() { 3 } }
		NATObject x = new NATObject(ctx_.getSelf());
		NATMethod y = new NATMethod(atY_, NATTable.EMPTY, new AGBegin(new NATTable(new ATObject[] { atThree_ })));
		x.meta_addMethod(y);
		ctx_.getLexicalScope().meta_defineField(atX_, x);

		assertEquals(evalAndReturn("x.y").asClosure().getMethod(), y);
	}
	
	public void testFirstClassMessages() throws NATException {
		ATMessage methInv = evalAndReturn(".m(3)").asMessage();
		ATMessage asyncMsg = evalAndReturn("<-m(3)").asMessage();

		assertEquals(atM_, methInv.getSelector());
		assertEquals(atThree_, methInv.getArguments().base_at(NATNumber.ONE));
		assertTrue(methInv instanceof ATMethodInvocation);
		
		assertEquals(atM_, asyncMsg.getSelector());
		assertEquals(atThree_, asyncMsg.getArguments().base_at(NATNumber.ONE));
		assertTrue(asyncMsg instanceof ATAsyncMessage);
		assertEquals(ctx_.getSelf(), ((ATAsyncMessage) asyncMsg).getSender());
	}
	
	public void testMethodApplication() throws NATException {
         // def x := 3
		ctx_.getLexicalScope().meta_defineField(atX_, atThree_);
		
		// def identity(x) { x }
		ATSymbol identityS = AGSymbol.alloc(NATText.atValue("identity"));
		ATTable pars = new NATTable(new ATObject[] { atX_ });
		NATMethod identity = new NATMethod(identityS, pars, new AGBegin(new NATTable(new ATObject[] { atX_ })));
		ctx_.getLexicalScope().meta_addMethod(identity);
		
		evalAndCompareTo("identity(1)", NATNumber.ONE);
	}
	
	public void testClosureApplication() throws NATException {
        // def x := 3
		ctx_.getLexicalScope().meta_defineField(atX_, atThree_);
		
		// def identity := { | x | x }
		ATSymbol identityS = AGSymbol.alloc(NATText.atValue("identity"));
		ATTable pars = new NATTable(new ATObject[] { atX_ });
		NATClosure identity = new NATClosure(new NATMethod(identityS, pars, new AGBegin(new NATTable(new ATObject[] { atX_ }))), ctx_);
		ctx_.getLexicalScope().meta_defineField(identityS, identity);
		
		evalAndCompareTo("identity(1)", NATNumber.ONE);
	}
	
	public void testMethodInvocation() throws NATException {
         // def x := object: { def x := 3; def m(y) { y; x } }
		NATObject x = new NATObject(ctx_.getLexicalScope());
		NATMethod m = new NATMethod(atM_, new NATTable(new ATObject[] { atY_ }),
				                         new AGBegin(new NATTable(new ATObject[] { atY_, atX_ })));
		x.meta_defineField(atX_, atThree_);
		x.meta_addMethod(m);
		ctx_.getLexicalScope().meta_defineField(atX_, x);
		
		evalAndCompareTo("x.m(1)", atThree_);
	}
	
	public void testQuotation() throws NATException {
		evalAndCompareTo("`(3)", atThree_);
		evalAndCompareTo("`(x)", atX_);
		evalAndCompareTo("`(def x := 3)", "def x := 3");
		evalAndCompareTo("`(def x := `(3))", "def x := `(3)");
		evalAndCompareTo("`(def x := #(3))", "def x := 3");
		evalAndCompareTo("`(def foo(a) { #([1,2,3]) })", "def foo(a) { [1, 2, 3] }");
		evalAndCompareTo("`(def foo(#@(`([a,b,c]))) { #@([1,2,3]) })", "def foo(a, b, c) { 1; 2; 3 }");
		evalAndCompareTo("`(def foo: x bar: #@(`([y,z])) { 1 })", "def foo:bar:(x, y, z) { 1 }");
	}
	
	public void testArgumentSplicing() throws NATException {
		evalAndCompareTo("[1, @[2,[3]], [4], @[5], @[], 6]", "[1, 2, [3], [4], 5, 6]");
		
		// def m(x,y,z) { z }
		NATMethod m = new NATMethod(atM_, new NATTable(new ATObject[] { atX_, atY_, atZ_ }),
                                           new AGBegin(new NATTable(new ATObject[] { atZ_ })));
        ctx_.getSelf().meta_addMethod(m);
		
		evalAndCompareTo("m(1,@[2,3])", "3");
		evalAndCompareTo("self.m(1,@[2,3])", "3");
	}
	
	public void testVariableArguments() throws NATException {
		// def m(x,y,@z) { [x, y, z] }
		NATMethod m = new NATMethod(atM_, new NATTable(new ATObject[] { atX_, atY_, new AGSplice(atZ_) }),
                                           new AGBegin(new NATTable(new ATObject[] {
                                        		   new NATTable(new ATObject[] { atX_, atY_, atZ_ })
                                           })));
        ctx_.getSelf().meta_addMethod(m);
		
		evalAndCompareTo("m(1,2,3,4,5)", "[1, 2, [3, 4, 5]]");
		evalAndCompareTo("m(1,2,3)", "[1, 2, [3]]");
		evalAndCompareTo("m(1,2)", "[1, 2, []]");
		evalAndCompareTo("m(1,2,@[3,4,5])", "[1, 2, [3, 4, 5]]");
		evalAndCompareTo("m(@[1,2,3])", "[1, 2, [3]]");
	}
	
	public void testVariableKeywordArguments() throws NATException {
		// def foo:bar:(x,@y) { y }
		NATMethod fooBar = new NATMethod(atFooBar_, new NATTable(new ATObject[] { atX_, new AGSplice(atY_) }),
                                                new AGBegin(new NATTable(new ATObject[] { atY_ })));
        ctx_.getSelf().meta_addMethod(fooBar);
		
		evalAndCompareTo("foo: 1 bar: 2", "[2]");
		evalAndCompareTo("foo: 1 bar: @[2,3]", "[2, 3]");
		evalAndCompareTo("foo: 1 bar: @[2]", "[2]");
		evalAndCompareTo("foo: 1 bar: @[]", "[]");
	}
	
}
