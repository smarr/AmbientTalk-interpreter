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
package edu.vub.at.objects.natives.test;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATSuperObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSelf;
import edu.vub.at.objects.natives.grammar.AGSuper;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import junit.framework.TestCase;

/**
 * @author smostinc
 *
 * TODO document the class NATObjectClosureTest
 */
public class NATObjectClosureTest extends TestCase {

	private class AGScopeTest extends NATNil {
		
		private final ATObject scope_;
		private final ATObject self_;
		private final ATObject super_;
		
		public AGScopeTest(ATObject scope, ATObject self, ATObject zuper) {
			scope_ = scope;
			self_ = self;
			super_ = zuper;
		}

		public ATObject meta_eval(ATContext ctx) throws NATException {
			// SCOPE-test
			// Is the current callframe lexically connected to the expected scope
			ATObject lexEnv = ctx.getLexicalScope();
			while (lexEnv != scope_) {
				if(lexEnv == NATNil._INSTANCE_) {
					fail();
					break;
				}
				lexEnv = lexEnv.getLexicalParent();
			}
			
			// SELF-tests
			// Is the current value of self consistent with our expectations
			assertEquals(self_, ctx.getSelf());
			// Is the expected value of self accessible through the pseudovariable
			assertEquals(self_, AGSelf._INSTANCE_.meta_eval(ctx));	
			
			// SUPER-tests
			// Is the current value of super consistent with our expectations
			assertEquals(super_, ctx.getSuper());
			// Is the expected value of super accessible through the pseudovariable
			assertEquals(super_, ((NATSuperObject)AGSuper._INSTANCE_.meta_eval(ctx)).getLookupFrame());

			return this;
		}

		
	
	}
	
	private abstract class AGEqualityTest extends NATNil {
		
		public abstract Object getExpectedResult(ATContext ctx);
		
		public abstract Object getActualValue(ATContext ctx);
		
		public boolean equal(Object expected, Object actual) {
			return actual == null ? 
						expected == null:
						expected.equals(actual);
		}
		public ATObject meta_eval(ATContext ctx) throws NATException {
			if(! equal(getExpectedResult(ctx), getActualValue(ctx)))
				fail();
			
			return this;
		}
	}

	private NATObject  lexicalRoot_;
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATObjectClosureTest.class);
	}

	protected void setUp() throws Exception {
		lexicalRoot_ = new NATObject(NATNil._INSTANCE_);
	}
	
	public void testMethodInvocation() {
		try {
			NATObject object = new NATObject(lexicalRoot_);

			ATSymbol scopeTest = AGSymbol.alloc(NATText.atValue("scopeTest"));
			ATMethod scopeTestMethod = new NATMethod(
					scopeTest, 
					NATTable.EMPTY, 
					new AGScopeTest(object, object,NATNil._INSTANCE_));
			object.meta_addMethod(scopeTestMethod);
			
			object.meta_invoke(object, scopeTest, NATTable.EMPTY);
		} catch (NATException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testDelegatedMethodInvocation() {
		try {
			NATObject parent = new NATObject(lexicalRoot_);
			
			NATObject child = new NATObject(parent, lexicalRoot_);
			
			ATSymbol lateBoundSelf = AGSymbol.alloc(NATText.atValue("lateBoundSelf"));
			ATMethod lateBoundSelfTestMethod = new NATMethod(
					lateBoundSelf, 
					NATTable.EMPTY, 
					new AGScopeTest(parent, child, NATNil._INSTANCE_));
			
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
					new AGScopeTest(child, child, parent)
					);
			
			parent.meta_addMethod(lateBoundSelfTestMethod);
			child.meta_addMethod(superSemanticsTestMethod);
			
			child.meta_invoke(child, lateBoundSelf, NATTable.EMPTY);
			child.meta_invoke(child, superSemantics, NATTable.EMPTY);
		} catch (NATException e) {
			e.printStackTrace();
			fail();
		}
	}

}
