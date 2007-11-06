package edu.vub.at.objects.natives.grammar;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.OBJNil;

/**
 * Tests the ATObject.meta_eval(ATContext) method for different kinds of abstract grammar elements.
 */
public class TestEval extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(TestEval.class);
	}
	
	private final NATNumber atThree_ = NATNumber.atValue(3);
	private final AGSymbol atX_ = AGSymbol.alloc(NATText.atValue("x"));
	private final AGSymbol atY_ = AGSymbol.alloc(NATText.atValue("y"));
	private final AGSymbol atZ_ = AGSymbol.alloc(NATText.atValue("z"));
	private final AGSymbol atM_ = AGSymbol.alloc(NATText.atValue("m"));
	private final AGSymbol atFooBar_ = AGSymbol.alloc(NATText.atValue("foo:bar:"));

	// statements
	
	public void testBegin() {
        evalAndCompareTo("1; 2; 3", atThree_);
        evalAndCompareTo("3", atThree_);
	}
	
	// definitions
	
	public void testDefField() throws InterpreterException {
        evalAndCompareTo("def x := 3", atThree_);
        assertEquals(atThree_, ctx_.base_lexicalScope().impl_call(atX_, NATTable.EMPTY));
	}
	
	public void testDefFunction() throws InterpreterException {
        evalAndCompareTo("def x() { 3 }", "<closure:x>");
        try {
        	  ATClosure clo = ctx_.base_lexicalScope().impl_lookup(atX_).asClosure();
        	  assertEquals(atX_, clo.base_method().base_name());
        } catch(XSelectorNotFound e) {
        	  fail("broken definition:"+e.getMessage());
        }
	}
	
	public void testDefTable() throws InterpreterException {
        evalAndCompareTo("def x[3] { 1; 2; 3 }", "[3, 3, 3]");
        try {
        	  ATObject tab = ctx_.base_lexicalScope().impl_call(atX_, NATTable.EMPTY);
        	  assertEquals(atThree_, tab.asTable().base_length());
        	  assertEquals(atThree_, tab.asTable().base_at(NATNumber.ONE));
        } catch(XSelectorNotFound e) {
        	  fail("broken definition:"+e.getMessage());
        }
	}
	
	public void testDefExternalMethod() throws InterpreterException {
		ATObject rcvr = new NATObject();
		AGSymbol rcvnam = AGSymbol.jAlloc("o");
		ctx_.base_lexicalScope().meta_defineField(rcvnam, rcvr);
        evalAndCompareTo("def o.x() { self }", "<closure:x>");
        try {
        	  ATClosure clo = rcvr.impl_lookup(atX_).asClosure();
        	  assertEquals(atX_, clo.base_method().base_name());
        	  assertEquals(rcvr, rcvr.meta_invoke(rcvr, atX_, NATTable.EMPTY));
        } catch(XSelectorNotFound e) {
        	  fail("broken external method definition:"+e.getMessage());
        }
	}
	
	public void testDefExternalField() throws InterpreterException {
		ATObject rcvr = new NATObject();
		AGSymbol rcvnam = AGSymbol.jAlloc("o2");
		ctx_.base_lexicalScope().meta_defineField(rcvnam, rcvr);
        evalAndCompareTo("def o2.x := 3", atThree_);
        try {
        	  assertEquals(atThree_, rcvr.impl_invokeAccessor(rcvr, atX_, NATTable.EMPTY));
        } catch(XUndefinedSlot e) {
        	  fail("broken external field definition:"+e.getMessage());
        }
	}
	
	// assignments
	
	public void testAssignVariable() throws InterpreterException {
		// def x := nil
		ctx_.base_lexicalScope().meta_defineField(atX_, OBJNil._INSTANCE_);
		
        evalAndCompareTo("x := 3", atThree_);
        assertEquals(atThree_, ctx_.base_lexicalScope().impl_call(atX_, NATTable.EMPTY));
	}
	
	public void testAssignTable() throws InterpreterException {
		// def x[2] { nil }
		ATTable table = NATTable.atValue(new ATObject[] { OBJNil._INSTANCE_, OBJNil._INSTANCE_ });
		ctx_.base_lexicalScope().meta_defineField(atX_, table);
		
        evalAndCompareTo("x[1] := 3", atThree_);
        assertEquals(atThree_, table.base_at(NATNumber.ONE));
	}
	
	public void testAssignField() throws InterpreterException {
		// def x := object: { def y := 0 }
		ATObject x = new NATObject(ctx_.base_lexicalScope());
		x.meta_defineField(atY_, NATNumber.ZERO);
		ctx_.base_lexicalScope().meta_defineField(atX_, x);
		
         evalAndCompareTo("x.y := 3", atThree_);
         assertEquals(atThree_, x.impl_invokeAccessor(x, atY_, NATTable.EMPTY));
	}
	
	public void testMultiAssignment() throws InterpreterException {
		// def x := 1; def y := 3
		ctx_.base_lexicalScope().meta_defineField(atX_, NATNumber.ONE);
		ctx_.base_lexicalScope().meta_defineField(atY_, atThree_);
		
         evalAndCompareTo("[x, y] := [ y, x ]", "[3, 1]");
         assertEquals(atThree_, ctx_.base_lexicalScope().impl_call(atX_, NATTable.EMPTY));
         assertEquals(NATNumber.ONE, ctx_.base_lexicalScope().impl_call(atY_, NATTable.EMPTY));
	}
	
	// expressions
	
	public void testSymbolReference() throws InterpreterException {
		// def x := 3
		ctx_.base_lexicalScope().meta_defineField(atX_, atThree_);
        evalAndCompareTo("x", atThree_);
	}
	
	public void testTabulation() throws InterpreterException {
		// def x := [3,1]
		ATTable table = NATTable.atValue(new ATObject[] { atThree_, NATNumber.ONE });
		ctx_.base_lexicalScope().meta_defineField(atX_, table);
		
        evalAndCompareTo("x[1]", atThree_);
        evalAndCompareTo("x[2]", NATNumber.ONE);
	}
	
	public void testClosureLiteral() throws InterpreterException {
	  ATClosure clo = evalAndReturn("{| x, y | 3 }").asClosure();
	  ATSymbol nam = clo.base_method().base_name();
	  ATTable arg = clo.base_method().base_parameters();
	  ATAbstractGrammar bdy = clo.base_method().base_bodyExpression();
	  ATContext ctx = clo.base_context();
	  assertEquals(AGSymbol.alloc(NATText.atValue("lambda")), nam);
	  assertEquals(atX_, arg.base_at(NATNumber.ONE));
	  assertEquals(atY_, arg.base_at(NATNumber.atValue(2)));
	  assertEquals(atThree_, bdy.asBegin().base_statements().base_at(NATNumber.ONE));
	  assertEquals(ctx_, ctx);
	}
	
	public void testSelfReference() throws InterpreterException {
        evalAndCompareTo("self", ctx_.base_self());
	}
	
	public void testSuperReference() throws InterpreterException {
        assertEquals(ctx_.base_lexicalScope().base_super(), evalAndReturn("super"));
	}
	
	public void testSelection() throws InterpreterException {
		// def x := object: { def y() { 3 } }
		NATObject x = new NATObject(ctx_.base_self());
		NATMethod y = new NATMethod(atY_, NATTable.EMPTY, new AGBegin(NATTable.atValue(new ATObject[] { atThree_ })));
		x.meta_addMethod(y);
		ctx_.base_lexicalScope().meta_defineField(atX_, x);

		assertEquals(evalAndReturn("x.&y").asClosure().base_method(), y);
	}
	
	public void testFirstClassMessage() throws InterpreterException {
		ATMessage methInv = evalAndReturn(".m(3)").asMessage();

		assertEquals(atM_, methInv.base_selector());
		assertEquals(atThree_, methInv.base_arguments().base_at(NATNumber.ONE));
		assertTrue(methInv instanceof ATMethodInvocation);
	}
	
	public void testFirstClassAsyncMessage() throws Exception {
		ATMessage asyncMsg = evalAndReturn("<-m(3)").asMessage();

		assertEquals(atM_, asyncMsg.base_selector());
		assertEquals(atThree_, asyncMsg.base_arguments().base_at(NATNumber.ONE));
		assertTrue(asyncMsg instanceof ATAsyncMessage);
		// following is removed because async msges no longer encapsulate their receiver
		// assertEquals(OBJNil._INSTANCE_, ((ATAsyncMessage) asyncMsg).base_receiver());	
	}
	
	public void testMethodApplication() throws InterpreterException {
         // def x := 3
		ctx_.base_lexicalScope().meta_defineField(atX_, atThree_);
		
		// def identity(x) { x }
		ATSymbol identityS = AGSymbol.alloc(NATText.atValue("identity"));
		ATTable pars = NATTable.atValue(new ATObject[] { atX_ });
		NATMethod identity = new NATMethod(identityS, pars, new AGBegin(NATTable.atValue(new ATObject[] { atX_ })));
		ctx_.base_lexicalScope().meta_addMethod(identity);
		
		evalAndCompareTo("identity(1)", NATNumber.ONE);
	}
	
	public void testClosureApplication() throws InterpreterException {
        // def x := 3
		ctx_.base_lexicalScope().meta_defineField(atX_, atThree_);
		
		// def identity := { | x | x }
		ATSymbol identityS = AGSymbol.alloc(NATText.atValue("identity"));
		ATTable pars = NATTable.atValue(new ATObject[] { atX_ });
		NATClosure identity = new NATClosure(new NATMethod(identityS, pars, new AGBegin(NATTable.atValue(new ATObject[] { atX_ }))), ctx_);
		ctx_.base_lexicalScope().meta_defineField(identityS, identity);
		
		evalAndCompareTo("identity(1)", NATNumber.ONE);
	}
	
	public void testMethodInvocation() throws InterpreterException {
         // def x := object: { def x := 3; def m(y) { y; x } }
		NATObject x = new NATObject(ctx_.base_lexicalScope());
		NATMethod m = new NATMethod(atM_, NATTable.atValue(new ATObject[] { atY_ }),
				                         new AGBegin(NATTable.atValue(new ATObject[] { atY_, atX_ })));
		x.meta_defineField(atX_, atThree_);
		x.meta_addMethod(m);
		ctx_.base_lexicalScope().meta_defineField(atX_, x);
		
		evalAndCompareTo("x.m(1)", atThree_);
	}
	
	public void testDelegation() throws InterpreterException {
        // def x := object: { def m() { self.y + 1 } } ; def y := 2
		NATObject x = new NATObject(ctx_.base_lexicalScope());
		ATMethod m = evalAndReturn("def m() { self.y + 1 }").asClosure().base_method();
		x.meta_addMethod(m);
		ctx_.base_self().meta_defineField(atY_, NATNumber.atValue(2));
		ctx_.base_lexicalScope().meta_defineField(atX_, x);
		
		evalAndCompareTo("x^m()", atThree_);
	}
	
	public void testQuotation() throws InterpreterException {
		evalAndCompareTo("`3", atThree_);
		evalAndCompareTo("`x", atX_);
		evalAndCompareTo("`(o.x)", "o.x");
		evalAndCompareTo("`(o.&x)", "o.&x");
		evalAndCompareTo("`{def x := 3}", "def x := 3");
		evalAndCompareTo("`{def x := `3}", "def x := `(3)");
		evalAndCompareTo("`{def x := #3}", "def x := 3");
		evalAndCompareTo("`{def foo(a) { #([1,2,3]) }}", "def foo(a) { [1, 2, 3] }");
		evalAndCompareTo("`{def foo(#@(`([a,b,c]))) { #@([1,2,3]) }}", "def foo(a, b, c) { 1; 2; 3 }");
		evalAndCompareTo("`{def foo: x bar: #@(`([y,z])) { 1 }}", "def foo:bar:(x, y, z) { 1 }");
	}
	
	public void testArgumentSplicing() throws InterpreterException {
		evalAndCompareTo("[1, @[2,[3]], [4], @[5], @[], 6]", "[1, 2, [3], [4], 5, 6]");
		
		// def m(x,y,z) { z }
		NATMethod m = new NATMethod(atM_, NATTable.atValue(new ATObject[] { atX_, atY_, atZ_ }),
                                           new AGBegin(NATTable.atValue(new ATObject[] { atZ_ })));
        ctx_.base_self().meta_addMethod(m);
		
		evalAndCompareTo("m(1,@[2,3])", "3");
		evalAndCompareTo("self.m(1,@[2,3])", "3");
	}
	
	public void testVariableArguments() throws InterpreterException {
		// def m(x,y,@z) { [x, y, z] }
		NATMethod m = new NATMethod(atM_, NATTable.atValue(new ATObject[] { atX_, atY_, new AGSplice(atZ_) }),
                                           new AGBegin(NATTable.atValue(new ATObject[] {
                                        		   NATTable.atValue(new ATObject[] { atX_, atY_, atZ_ })
                                           })));
        ctx_.base_self().meta_addMethod(m);
		
		evalAndCompareTo("m(1,2,3,4,5)", "[1, 2, [3, 4, 5]]");
		evalAndCompareTo("m(1,2,3)", "[1, 2, [3]]");
		evalAndCompareTo("m(1,2)", "[1, 2, []]");
		evalAndCompareTo("m(1,2,@[3,4,5])", "[1, 2, [3, 4, 5]]");
		evalAndCompareTo("m(@[1,2,3])", "[1, 2, [3]]");
	}
	
	public void testVariableKeywordArguments() throws InterpreterException {
		// def foo:bar:(x,@y) { y }
		NATMethod fooBar = new NATMethod(atFooBar_, NATTable.atValue(new ATObject[] { atX_, new AGSplice(atY_) }),
                                                new AGBegin(NATTable.atValue(new ATObject[] { atY_ })));
        ctx_.base_self().meta_addMethod(fooBar);
		
		evalAndCompareTo("foo: 1 bar: 2", "[2]");
		evalAndCompareTo("foo: 1 bar: @[2,3]", "[2, 3]");
		evalAndCompareTo("foo: 1 bar: @[2]", "[2]");
		evalAndCompareTo("foo: 1 bar: @[]", "[]");
	}
	
	public void testVariableMultiDefinition() throws InterpreterException {
		// def [x, @y ] := [1, 2, 3, @[4, 5]
		// => x = 1; y = [2, 3, 4, 5]
		evalAndCompareTo("def [x, @y ] := [1, 2, 3, @[4, 5]]", "[1, 2, 3, 4, 5]");
		assertEquals(NATNumber.ONE, ctx_.base_lexicalScope().impl_call(atX_, NATTable.EMPTY));
		assertEquals("[2, 3, 4, 5]", ctx_.base_lexicalScope().impl_call(atY_, NATTable.EMPTY).meta_print().javaValue);
	}
	
	/** test whether applying an empty closure {} yields nil */
	public void testEmptyClosureApplication() throws InterpreterException {
		evalAndCompareTo("({})()", "nil");
	}
	
}
