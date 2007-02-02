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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGDefFunction;
import edu.vub.at.objects.natives.grammar.AGSelf;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import junit.framework.TestCase;

/**
 * NATObjectClosureTest tests the semantics of self & super inside methods and nested
 * closures.
 * 
 * @author smostinc
 */
public class NATObjectClosureTest extends TestCase {

	private class AGScopeTest extends NATNil implements ATStatement {
		
		private ATObject scope_;
		private ATObject self_;
		
		public AGScopeTest(ATObject scope, ATObject self) {
			scope_ = scope;
			self_ = self;
		}

		public ATObject meta_eval(ATContext ctx) throws InterpreterException {
			// SCOPE-test
			// Is the current callframe lexically connected to the expected scope
			ATObject lexEnv = ctx.base_getLexicalScope();
			while (lexEnv != scope_) {
				if(lexEnv == NATNil._INSTANCE_) {
					fail();
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
			//assertEquals(super_, ctx.base_getSuper());
			// Is the expected value of super accessible through the pseudovariable
			//assertEquals(super_, AGSuper._INSTANCE_.meta_eval(ctx));

			return this;
		}
		
		public ATStatement base_asStatement() {
			return this;
		}

		
	
	}
	
//	private abstract class AGEqualityTest extends NATNil implements ATStatement {
//		
//		public abstract Object getExpectedResult(ATContext ctx);
//		
//		public abstract Object getActualValue(ATContext ctx);
//		
//		public boolean equal(Object expected, Object actual) {
//			return actual == null ? 
//						expected == null:
//						expected.equals(actual);
//		}
//		
//		public ATObject meta_eval(ATContext ctx) throws NATException {
//			if(! equal(getExpectedResult(ctx), getActualValue(ctx)))
//				fail();
//			
//			return this;
//		}
//		
//		public ATStatement asStatement() {
//			return this;
//		}
//
//
//	}

	private NATObject  lexicalRoot_;
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATObjectClosureTest.class);
	}

	/**
	 * Initializes the lexical root object with a series of auxiliary definitions.
	 */
	protected void setUp() throws Exception {
		lexicalRoot_ = new NATObject(NATNil._INSTANCE_);
	}
	
	/**
	 * Tests the validity of the various scope pointers in a context object when 
	 * applying a method defined in and invoked upon an orphan object. 
	 * 
	 * - covers meta_invoke & meta_select for method lookup
	 * - covers closure creation in meta_select
	 * - covers context initialisation at closure creation
	 * - covers closure application
	 */
	public void testMethodInvocation() {
		try {
			NATObject object = new NATObject(lexicalRoot_);

			ATSymbol scopeTest = AGSymbol.alloc(NATText.atValue("scopeTest"));
			ATMethod scopeTestMethod = new NATMethod(
					scopeTest, 
					NATTable.EMPTY, 
					new AGBegin(NATTable.atValue(new ATObject[] {
							new AGScopeTest(object, object)})));
			object.meta_addMethod(scopeTestMethod);
			
			object.meta_invoke(object, scopeTest, NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * Tests the validity of the various scope pointers in a context object when 
	 * applying a method in a simple hierarchy of objects. 
	 * 
	 * - covers meta_invoke & meta_select for method lookup with dynamic chains
	 * - covers proper self semantics at closure creation 
	 * - covers super semantics during method application
	 */
	public void testDelegatedMethodInvocation() {
		try {
			NATObject parent = new NATObject(lexicalRoot_);
			
			NATObject child = new NATObject(parent, lexicalRoot_, NATObject._IS_A_);
			
			ATSymbol lateBoundSelf = AGSymbol.alloc(NATText.atValue("lateBoundSelf"));
			ATMethod lateBoundSelfTestMethod = new NATMethod(
					lateBoundSelf, 
					NATTable.EMPTY, 
					new AGBegin(NATTable.atValue(new ATObject[] {
							new AGScopeTest(parent, child)})));
			
			ATSymbol superSemantics = AGSymbol.alloc(NATText.atValue("superSemantics"));
			ATMethod superSemanticsTestMethod = new NATMethod(
					superSemantics, 
					NATTable.EMPTY,
//					new AGEqualityTest() {
//						public Object getExpectedResult(ATContext ctx) {
//							return ctx.getLexicalEnvironment().getDynamicParent();
//						}
//						
//						public Object getActualValue(ATContext ctx) {
//							return ctx.getParentObject();
//						}
//					}
					new AGBegin(NATTable.atValue(new ATObject[] {
							new AGScopeTest(child, child)})));
			
			parent.meta_addMethod(lateBoundSelfTestMethod);
			child.meta_addMethod(superSemanticsTestMethod);
			
			child.meta_invoke(child, lateBoundSelf, NATTable.EMPTY);
			child.meta_invoke(child, superSemantics, NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * Makes a simple extension of an orphan object using a closure.
	 * - covers meta_extend for object extension.
	 * - covers method definition using AGDefMethod
	 */
	public void testExtend() {
		try {
			NATObject parent = new NATObject(lexicalRoot_);
			
			ATSymbol superSemantics = AGSymbol.alloc(NATText.atValue("superSemantics"));

			AGScopeTest test = new AGScopeTest(null, null);
			
			NATObject child = (NATObject)parent.meta_extend(
					new NATClosure(
							new NATMethod(AGSymbol.alloc(NATText.atValue("lambda")), NATTable.EMPTY,
									new AGBegin(NATTable.atValue(new ATObject[] {
											new AGDefFunction(superSemantics, NATTable.EMPTY, 
													new AGBegin(
															NATTable.atValue(new ATObject[] { test })))}))),
																	lexicalRoot_,
																	lexicalRoot_));
			
			test.scope_ = child;
			test.self_ = child;
			
			ATSymbol lateBoundSelf = AGSymbol.alloc(NATText.atValue("lateBoundSelf"));
			ATMethod lateBoundSelfTestMethod = new NATMethod(
					lateBoundSelf, 
					NATTable.EMPTY, 
					new AGBegin(NATTable.atValue(new ATObject[] {
							new AGScopeTest(parent, child)})));
			
			parent.meta_addMethod(lateBoundSelfTestMethod);
			
			child.meta_invoke(child, lateBoundSelf, NATTable.EMPTY);
			child.meta_invoke(child, superSemantics, NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail();
		}
		
	}

	
}
