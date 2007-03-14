/**
 * AmbientTalk/2 Project
 * NATObjectClosureTest.java created on Jul 25, 2006 at 10:51:44 AM
 * (c) Programming Technology Lab, 2006 - 2007
 * Authors: Tom Van Cutsem & Stijn Mostinckx
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGDefFunction;
import edu.vub.at.objects.natives.grammar.AGSelf;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * AmbientTalk/2 is a dually scoped programming language, providing access to both the lexical
 * scope methods and objects are defined in, as well as a dynamic scope which follows the 
 * parent chain of an object. Moreover, the language features the notion of closures and methods
 * which have important semantic differences, and a few additional concepts. 
 * 
 * This test suite documents the proper binding semantics for variables, self and super inside
 * methods and closures. 
 * 
 * @author smostinc
 */
public class NATObjectClosureTest extends AmbientTalkTest {

	/**
	 * This class is a special statement class used to test the correct scoping of method 
	 * invocation from the java level, rather than by executing ambienttalk code directly.
	 * It is to be instantiated with the expected values and then passed into a method.
	 */
	private class AGScopeTest extends NATNil implements ATStatement {
		
		private ATObject scope_;
		private ATObject self_;
		private ATObject super_;
		
		public AGScopeTest(ATObject scope, ATObject self, ATObject zuper) {
			scope_ = scope;
			self_ = self;
			super_ = zuper;
		}

		public ATObject meta_eval(ATContext ctx) throws InterpreterException {
			// SCOPE-test
			// Is the current callframe lexically connected to the expected scope
			ATObject lexEnv = ctx.base_getLexicalScope();
			while (lexEnv != scope_) {
				if(lexEnv == NATNil._INSTANCE_) {
					fail("Lexical scope not found");
					break;
				}
				lexEnv = lexEnv.meta_getLexicalParent();
			}
			
			// SELF-tests
			// Is the current value of self consistent with our expectations
			assertEquals(self_, ctx.base_getSelf());
			// Is the expected value of self accessible through the pseudovariable
			assertEquals(self_, AGSelf._INSTANCE_.meta_eval(ctx));	
			
			// SUPER-tests
			// Is the current value of super consistent with our expectations
			assertEquals(super_, ctx.base_getLexicalScope().meta_lookup(NATObject._SUPER_NAME_));

			return this;
		}
		
		public ATStatement asStatement() {
			return this;
		}
		
		public ATMethod transformToMethodNamed(ATSymbol name) throws InterpreterException {
			return new NATMethod(
					name, 
					NATTable.EMPTY, 
					new AGBegin(NATTable.atValue(new ATObject[] { this })));
		}
	
	}
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATObjectClosureTest.class);
	}

	/**
	 * When defining an object, the programmer can choose to create either a method or a closure. 
	 * In the context of an object which is not part of a dynamic hierarchy, there is no scoping
	 * difference between both solutions: both prefer the definitions in the object to the outer
	 * lexical scope, and in this particular case, their self and super bindings are identical.
	 * 
	 * Note that the next test illustrates the esential difference between closures (who capture
	 * self and super) and methods (who leave them late bound).
	 */
	public void testOrphanObjectScope() {
		evalAndReturn(
				"def scope := \"outer\"; \n" +
				"def orphan := object: {" +
				"  def scope := \"inner\";" +
				"  def method() { \n" +
				"    if: !(scope == \"inner\") then: { fail() }; \n" +
				"    if:  (self  == nil) then: { fail() }; \n" +
				"    if: !(super == nil) then: { fail() }; \n" +
				"  }; \n" +
				"  def closure := { \n" +
				"    if: !(scope == \"inner\") then: { fail() }; \n" +
				"    if:  (self  == nil) then: { fail() }; \n" +
				"    if: !(super == nil) then: { fail() }; \n" +
				"  }; \n" +
				"};" +
				"orphan.method(); \n" +
				"orphan.closure(); \n");
	}
	
	/**
	 * When defining an object, the programmer can choose to create either a method or a closure.
	 * The fundamental difference between both is the way they treat self and super. A closure
	 * will create bindings for these variables upon creation, whereas a method leaves them late
	 * bound. This implies that the binding for self in a closure is never late bound, as this 
	 * test illustrates. Note that super is statically bound in both cases.
	 */
	public void testParentObjectScope() {
		evalAndReturn(
				"def scope := \"outer\"; \n" +
				"def parent := object: { \n" +
				"  def scope := \"parent\"; \n" +
				"  def parent := self; \n" +
				"  def method() { \n" +
				"    if:  (self.scope == \"parent\") then: { fail() }; \n" +
				"    if:  (self       == parent) then: { fail() }; \n" +
				
				// without prefix : use lexical binding
				"    if: !(scope      == \"parent\") then: { fail() }; \n" +
				"    if: !(super      == nil) then: { fail() }; \n" +
				"  }; \n" +
				"  def closure := { \n" +
				"    if: !(self.scope == \"parent\") then: { fail() }; \n" +
				"    if: !(self       == parent) then: { fail() }; \n" +
					
				// without prefix : use lexical binding
				"    if: !(scope      == \"parent\") then: { fail() }; \n" +
				"    if: !(super      == nil) then: { fail() }; \n" +
				"  }; \n" +
				"}; \n" +
				"def child := extend: parent with: { \n" +
				"  def scope := \"child\"; \n" +
				"}; \n" +
				"child.method(); \n" +
				"child.closure(); \n");
	}
	
	/**
	 * Closures are created when defining a function inside an object's method as well. This 
	 * test illustrates that these closures also capture their self and super bindings at 
	 * creation time. In this case, note that the closure is created only when the method
	 * is executed, yielding behaviour somewhat similar to late binding.
	 */
	public void testNestedClosureScope() {
		evalAndReturn(
				"def scope := \"outer\"; \n" +
				"def parent := object: { \n" +
				"  def scope := \"parent\"; \n" +
				"  def method(invoker) { \n" +
				"    def scope := \"method\"; \n" +
				"    def nestedClosure() { \n" +
				"      if:  !(self.scope == invoker.scope) then: { fail() }; \n" +
				"      if:  !(self       == invoker) then: { fail() }; \n" +
				
				// without prefix : use lexical binding
				"      if: !(scope      == \"method\") then: { fail() }; \n" +
				"      if: !(super      == nil) then: { fail() }; \n" +
				"    }; \n" +
				
				// return the first class closure 
				"    nestedClosure; \n" +
				"  }; \n" +
				"}; \n" +
				"def child := extend: parent with: { \n" +
				"  def scope := \"child\"; \n" +
				"}; \n" +
				"parent.method(parent)(); \n" +
				"child.method(child)(); \n");		
	}
	
	/**
	 * When objects are lexically nested, the nested object has lexical access to the methods
	 * of the enclosing object. This test method illustrates that such a lexical invocation
	 * is equivalent to a direct invocation on the enclosing object, with respect to the 
	 * bindings for the self and super variables. 
	 * 
	 * Design Principle : through lexical access the self binding cannot be set to objects 
	 * which are not part of the dynamic object chain.
	 */
	public void testInvokeLexicallyVisibleMethod() {
		evalAndReturn(
				"def outer := object: { \n" +
				"  def scope := \"outer\"; \n" +
				"  def outer := self; \n" +
				"  def method() { \n" +
				"    if: !(scope == \"outer\") then: { fail() }; \n" +
				"    if: !(self  == outer) then: { fail() }; \n" +
				"    if: !(super == nil) then: { fail() }; \n" +
				"  }; \n" +
				"  def inner := object: { \n" +
				"    def test() { \n" +
				"      method(); \n" +
				"    }; \n" +
				"  }; \n" +
				"}; \n" +
				"outer.inner.test(); \n");		
	}
	
	/**
	 * The previous test illustrated that it is possible to perform a lexical invocation
	 * of methods in an enclosing object. This test tries to perform a similar feat, yet
	 * calls a method which is not defined by the enclosing object, but by its parent. 
	 * This invocation will fail.
	 * 
	 * Design Principle : lexical invocation is strictly limited to methods and closures 
	 * which are lexically visible. 
	 *
	 */
	public void testLexicallyInvokeInheritedMethod() {
		evalAndTestException(
				"def outer := object: { \n" +
				"  def scope := \"outer\"; \n" +
				"  def outer := self; \n" +
				"  def method() { \n" +
				"    if: !(scope == \"outer\") then: { fail() }; \n" +
				"    if: !(self  == outer) then: { fail() }; \n" +
				"    if: !(super == nil) then: { fail() }; \n" +
				"  }; \n" +
				"}; \n" +
				"def outerChild := extend: outer with: { \n" +
				"  def inner := object: { \n" +
				"    def test() { \n" +
				"      method(); \n" +
				"    }; \n" +
				"  }; \n" +
				"}; \n" +
				"outerChild.inner.test(); \n",
				XUndefinedField.class);		
	}
	
	/**
	 * AmbientTalk introduces first class delegation using the ^ symbol. This feature ensures
	 * that the self of a message is late bound. This test illustrates the late binding of self
	 * for delegated invocations, as opposed to oridinary invocations.
	 */
	public void testDelegatedMethodScope() {
		evalAndReturn(
				"def scope := \"outer\"; \n" +
				"def parent := object: { \n" +
				"  def scope := \"parent\"; \n" +
				"  def method(invoker) { \n" +
				"    if: !(self.scope == invoker.scope) then: { fail() }; \n" +
				"    if: !(self       == invoker) then: { fail() }; \n" +
				
				// without prefix : use lexical binding
				"    if: !(scope      == \"parent\") then: { fail() }; \n" +
				"    if: !(super      == nil) then: { fail() }; \n" +
				"  }; \n" +
				"}; \n" +
				"def child := extend: parent with: { \n" +
				"  def scope := \"child\"; \n" +
				"  def test() { \n" +
				"    super.method( super ); \n" +
				"    super^method( self ); \n" +
				"  }; \n" +
				"}; \n" +
				"child.test(); \n");		
	}
	
	/**
	 * Methods can be added to an object using external method definitions. These definitions
	 * provide access to the dynamic parent chain of objects, as any normal method would. The
	 * difference is that the external method has access to its own lexical scope and not to the
	 * one of the object it is being inserted to.
	 * 
	 * Design Principle: self and super in an external methods are confined to the dynamich 
	 * inheritance chain of the object they are being inserted to, and are subject to the 
	 * same constraints as those bindings in internal methods.
	 * Design Principle: Methods (including external ones) have unqualified access to their 
	 * surrounding lexical scope, and to this scope only.
	 */
	public void testExternalMethodScope() {
		evalAndReturn(
				"def scope := \"outer\"; \n" +
				"def parent := object: { \n" +
				"  def scope := \"parent\"; \n" +
				"}; \n" +
				"def child := extend: parent with: { \n" +
				"  def scope := \"child\"; \n" +
				"}; \n" +
				// isolates have no scope transfer
				"def extender := isolate: {" +
				"  def scope := \"extender\"; \n" +
				"  def extend(object) {" +
				"    def object.method(invoker) { \n" +
				"      if: !(self.scope == invoker.scope) then: { fail() }; \n" +
				"      if: !(self       == invoker) then: { fail() }; \n" +
				
				// without prefix : use lexical binding
				"      if: !(scope      == \"extender\") then: { fail() }; \n" +
				"      if: !(super      == object.super) then: { fail() }; \n" +
				"    }; \n" +
				"  }; \n" +
				"}; \n" +
				
				"extender.extend(parent); \n" +
				"child.method(child); \n" +
				"extender.extend(child); \n" +
				"child.method(child); \n");
	}
	
	/**
	 * Isolates are objects which have no access to variables in their lexical scope (hence 
	 * their name). This kind of object can thus be safely passed by copy to another actor.
	 * This test method illustrates that isolates have no access to their lexical but that
	 * they can access copies of outlying variables using ad hoc syntax. 
	 */
	public void testIsolateScope() {
		evalAndReturn(
				"def invisible := \"can't see me\"; \n" +
				"def copiedVar := 42; \n" +
				"def test := isolate: { | copiedVar | \n" +
				"  def testLexicalVariableCopy() { \n" +
				"    copiedVar; \n" +
				"  }; \n" +				
				"  def attemptLexicalVisiblity() { \n" +
				"    invisible; \n" +
				"  }; \n" +
				"}; \n" +
				
				// as the variables are copied, subsequent assignments are not observed
				"copiedVar := 23; \n" +
				"if: !(test.testLexicalVariableCopy() == 42) then: { fail(); }; \n");
		
		// attempting to use a variable that was not copied will fail
		evalAndTestException(
				"test.attemptLexicalVisiblity()",
				XUndefinedField.class);
	}
	
	/**
	 * Since external definitions inherently are equipped access to their lexical scope,
	 * and isolates are prohibited access to any form of lexical scope so that they can 
	 * be copied between actors, these mechanisms are irreconcilable.
	 *
	 * Design Principle: isolates have a sealed scope which can not be extended from the
	 * outside by means of external method definitions.
	 */
	public void testExternalDefinitionOnIsolates() {
		evalAndTestException(
				"def i := isolate: { nil }; \n" +
				"def i.method() { nil }; \n",
				XIllegalOperation.class);
	}
	

	/**
	 * NATIVE TEST: Tests the validity of the various scope pointers in a context object when 
	 * applying a method defined in and invoked upon an orphan object. 
	 * 
	 * - covers meta_invoke & meta_select for method lookup
	 * - covers closure creation in meta_select
	 * - covers context initialisation at closure creation
	 * - covers closure application
	 */
	public void testMethodInvocation() {
		try {
			ATObject object = new NATObject(ctx_.base_getLexicalScope());

			AGScopeTest expectedValues = 
				new AGScopeTest(object, object, object.base_getSuper());
			
			ATSymbol scopeTest = AGSymbol.jAlloc("scopeTest");
			
			object.meta_addMethod(expectedValues.transformToMethodNamed(scopeTest));
				
			object.meta_invoke(object, scopeTest, NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * NATIVE TEST: Tests the validity of the various scope pointers in a context object when 
	 * applying a method in a simple hierarchy of objects. 
	 * 
	 * - covers meta_invoke & meta_select for method lookup with dynamic chains
	 * - covers proper self semantics at closure creation 
	 * - covers super semantics during method application
	 */
	public void testDelegatedMethodInvocation() {
		try {
			NATObject parent = new NATObject(ctx_.base_getLexicalScope());
			
			NATObject child = new NATObject(parent, ctx_.base_getLexicalScope(), NATObject._IS_A_);
			
			AGScopeTest lateBoundSelfTest		= new AGScopeTest(parent, child, parent.base_getSuper());
			AGScopeTest superSemanticsTest	= new AGScopeTest(child, child, child.base_getSuper());
			
			ATSymbol lateBoundSelf = AGSymbol.alloc(NATText.atValue("lateBoundSelf"));
			ATSymbol superSemantics = AGSymbol.alloc(NATText.atValue("superSemantics"));
			
			parent.meta_addMethod(lateBoundSelfTest.transformToMethodNamed(lateBoundSelf));
			child.meta_addMethod(superSemanticsTest.transformToMethodNamed(superSemantics));
			
			child.meta_invoke(child, lateBoundSelf, NATTable.EMPTY);
			child.meta_invoke(child, superSemantics, NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * NATIVE TEST: Makes a simple extension of an orphan object using a closure. Tests the 
	 * correct scoping of methods with objects created using meta_extend
	 * 
	 * - covers meta_extend for object extension.
	 * - covers method definition using AGDefMethod
	 */
	public void testExtend() {
		try {
			NATObject parent = new NATObject(ctx_.base_getLexicalScope());
			
			ATSymbol superSemantics = AGSymbol.alloc(NATText.atValue("superSemantics"));

			AGScopeTest superSemanticsTest = new AGScopeTest(null, null, null);
			
			// We explicitly need to write out the construction of this object extension
			// extend: parent with: { def superSemantics() { #superSemanticsTest } };
			ATObject child = OBJLexicalRoot._INSTANCE_.base_extend_with_(parent,
					new NATClosure(
							new NATMethod(AGSymbol.alloc(NATText.atValue("lambda")), NATTable.EMPTY,
									new AGBegin(NATTable.atValue(new ATObject[] {
											new AGDefFunction(superSemantics, NATTable.EMPTY, 
													new AGBegin(
															NATTable.atValue(new ATObject[] { superSemanticsTest })))}))),
															ctx_.base_getLexicalScope(),
															ctx_.base_getLexicalScope()));
			
			superSemanticsTest.scope_ = child;
			superSemanticsTest.self_ = child;
			superSemanticsTest.super_ = child.base_getSuper();
			
			ATSymbol lateBoundSelf = AGSymbol.alloc(NATText.atValue("lateBoundSelf"));
			AGScopeTest lateBoundSelfTest = new AGScopeTest(parent, child, parent.base_getSuper());
			
			parent.meta_addMethod(lateBoundSelfTest.transformToMethodNamed(lateBoundSelf));
			
			child.meta_invoke(child, lateBoundSelf, NATTable.EMPTY);
			child.meta_invoke(child, superSemantics, NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
	/**
	 * NATIVE TEST: Tests whether the definition of an external method refers to the correct 
	 * bindings for:
	 * 
	 *  - lexically accessed variables
	 *  - the value of 'self'
	 *  - the value of 'super'
	 */
	public void testExternalMethodBindings() throws InterpreterException {
		/*
		 * Test code:
		 * 
		 *  def hostParent := object: { nil }
		 *  def host := extend: hostParent with: { nil }
		 *  def extender := object: {
		 *    def extend(o) {
		 *      def x := 5;
		 *      def o.m() { assert(lex==extender); assert(self==host); assert(super==hostParent) }
		 *    }
		 *  }
		 */
		ATObject hostParent = new NATObject();
		ATObject host = new NATObject(hostParent, Evaluator.getGlobalLexicalScope(), NATObject._IS_A_);
		ATObject extender = new NATObject();
		
		ctx_.base_getLexicalScope().meta_defineField(AGSymbol.jAlloc("scopetest"), new AGScopeTest(extender, host, hostParent));
		ATObject methodBody = evalAndReturn("`{def o.m() { #scopetest }}");
		
		extender.meta_addMethod(new NATMethod(AGSymbol.jAlloc("extend"),
				                              NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("o")}),
				                              new AGBegin(NATTable.of(methodBody))));
	}

	
}
