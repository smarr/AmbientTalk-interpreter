/**
 * AmbientTalk/2 Project
 * ExceptionHandlingTest.java created on Oct 10, 2006 at 10:34:10 PM
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

import edu.vub.at.AmbientTalkTestCase;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The ExceptionHandlingTest testcase tests the behaviour of the exception handling
 * primitives in Ambienttalk.
 *
 * @author smostinc
 */
public class ExceptionHandlingTest extends AmbientTalkTestCase {
		
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ExceptionHandlingTest.class);
	}
	
	public void testCustomObjectHandling() {
		try {
			ATObject globalLexScope = Evaluator.getGlobalLexicalScope();
			ATObject testScope = new NATCallframe(globalLexScope);
			
			testScope.meta_defineField(
					AGSymbol.alloc("echo:"),
					new JavaClosure(NATNil._INSTANCE_) {
						public ATObject base_apply(ATTable arguments) throws NATException {
							System.out.println(arguments.base_at(NATNumber.ONE).meta_print().javaValue);
							return NATNil._INSTANCE_;
						}						
					});
			
			ATContext testCtx = new NATContext(
					testScope, globalLexScope, globalLexScope.meta_getDynamicParent());
			
			evaluateInput(
					"def exception: code { object: code }; \n" +
					"def XNotFound := \n" +
					"  exception: { \n" +
					"    def test := 0; \n" +
					"  }; \n" +
					"\n" +
					"def tryTest(raised, caught, ifCaught) { \n" +
					"  try: { \n" +
					"    raise: raised; \n" +
					"  } catch: caught using: { | e | \n" +
					"    ifCaught(); \n" +
					"  }; \n" +
					"}; \n" +
					"\n" +
					"tryTest(XNotFound, XNotFound, { echo: \"1. Basic Test succeeded\"}); \n" +
					"tryTest(clone: XNotFound, XNotFound, { echo: \"2. Can raise a cloned exception\"}); \n" +
					"tryTest(XNotFound, clone: XNotFound, { echo: \"3. Can catch with a cloned exception\"}); \n" +
					"tryTest(extend: XNotFound with: { nil }, XNotFound, { echo: \"4. Can catch extensions\"}); \n",
					testCtx);
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}

}