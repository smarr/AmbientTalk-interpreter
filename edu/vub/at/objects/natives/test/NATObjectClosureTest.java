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
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSelf;
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
				if(lexEnv == NATNil.instance()) {
					fail();
					break;
				}
				lexEnv = lexEnv.getLexicalParent();
			}
			
			// SELF-tests
			// Is the current value of self consistent with our expectations
			assertEquals(self_, ctx.getSelf());
			// Is the expected value of self inserted in the lexical root of the scope
			assertEquals(self_, scope_.meta_lookup(AGSelf._INSTANCE_));	
			// Has self not been overridden in the currently active scope
			assertEquals(self_, ctx.getLexicalScope().meta_lookup(AGSelf._INSTANCE_));
			
			// SUPER-tests
			// Is the current value of super consistent with our expectations
			assertEquals(super_, ctx.getDynamicParent());
			// Is the expected value of super the dynamic parent of the call frame
			assertEquals(super_, scope_.getDynamicParent());
			// Has the current call-frame been constructed correctly
			assertEquals(super_, ctx.getLexicalScope().getDynamicParent());

			return this;
		}

		
	
	}	

	private NATObject  lexicalRoot_;
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATObjectClosureTest.class);
	}

	protected void setUp() throws Exception {
		lexicalRoot_ = new NATObject(NATNil.instance());
		lexicalRoot_.addField(AGSelf._INSTANCE_, lexicalRoot_);
	}
	
	public void testMethodInvocation() {
		try {
			NATObject manualExtension = new NATObject(lexicalRoot_);
			manualExtension.addField(AGSelf._INSTANCE_, manualExtension);
			ATSymbol methodName = AGSymbol.alloc(NATText.atValue("test"));
			ATMethod scopeTestMethod = new NATMethod(
					methodName, 
					NATTable.EMPTY, 
					new AGScopeTest(manualExtension, manualExtension,NATNil.instance()));
			manualExtension.meta_addMethod(scopeTestMethod);
			manualExtension.meta_invoke(methodName, NATTable.EMPTY);
		} catch (NATException e) {
			e.printStackTrace();
			fail();
		}
	}
	

}
