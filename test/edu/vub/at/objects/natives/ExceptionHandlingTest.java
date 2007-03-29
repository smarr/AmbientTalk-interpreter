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

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.AmbientTalkTestCase;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * This test documents and tests the behaviour of the exception handling primitives 
 * provided in Ambienttalk. In AmbientTalk any object can be used and thrown as an 
 * exception. Moreover, by relying on in stripes, all subtyping issues regarding to
 * handler selection are handled by the isStripedWith meta operation.
 *
 * @author smostinc
 */
public class ExceptionHandlingTest extends AmbientTalkTestCase {
	
	/**
	 * Any AmbientTalk Language value can be used as an exception, proving there is nothing 
	 * special about exceptions. This test demonstrates this for a number of language values,
	 * such as numbers, tables, closures and stripes themselves. 
	 * 
	 * This test also shows that objects can be caught using the stripe they are branded with.
	 * Note that most often, exceptions will be isolates, as these are objects (which can be
	 * made arbitrarily complex) which are passed by copy between actors. However, as the 
	 * stripes of a far reference are identical to the one of the original object, this 
	 * semantics is a default upon which can be varied.
	 */
	public void testAllObjectsCanBeUsedAsExceptions() {
		try {
			evaluateInput(
					"def testCount := 0; \n" +
					"raise: 42 catch: Number do: { | exc | testCount := testCount + 1 }; \n" +
					"raise: [ 4, 8, 15, 16, 23, 42 ] catch: Table do: { | exc | testCount := testCount + 1 }; \n" +
					"raise: { testCount := testCount + 1 } catch: Closure do: { | exc | exc.apply([]) }; \n" +
					"raise: Number catch: Stripe do: { | exc | testCount := testCount + 1 }; \n" +
					"if: testCount != 4 then: { fail() }", ctx_);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	};
	
	/**
	 * Handler selection in AmbientTalk is purely based on the stripes an object is branded with.
	 * As such the correct handler selection can be delegated to the stripe system which ensures
	 * correct handler selection.
	 */
	public void testStripeBasedHandlerSelection() {
		try {
			evaluateInput(
					"defstripe MyExceptions; \n" +
					"defstripe IncorrectArguments <: MyExceptions; \n" +
					"def testCount := 0; \n" +
					"try: { \n" +
					"  raise: (object: { nil } stripedWith: [ IncorrectArguments ]) catch: IncorrectArguments do: { | exc | testCount := testCount + 1 }; \n" +
					"  raise: (object: { nil } stripedWith: [ IncorrectArguments ]) catch: MyExceptions do: { | exc | testCount := testCount + 1 }; \n" +
					"  raise: (object: { nil } stripedWith: [ MyExceptions ]) catch: MyExceptions do: { | exc | testCount := testCount + 1 }; \n" +
					"  raise: (object: { nil } stripedWith: [ MyExceptions ]) catch: IncorrectArguments do: { | exc | testCount := testCount + 1 }; \n" +
					"} catch: MyExceptions using: { | exc | \n" +
					// the last test will result in an uncaught exception, but all previous ones should have been handled.
					"  if: testCount != 3 then: { fail() } \n" +
					"}", ctx_);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
		
	}
	
	/**
	 * The exceptions thrown by the interpreter can be intercepted by a program as they are also
	 * striped objects, striped to identify their function. This test illustrates how to use this 
	 * mechanism to build a python-like object model where non-existant field are silently added
	 * to the object upon use.
	 * 
	 * Note that an altogether cleaner mechanism can be conceived by using the doesNotUnderstand 
	 * meta hook to achieve similar behaviour.
	 */
	public void testInterpreterExceptionHandling() {
		try {
			evaluateInput(
					"def defaultValue := 42;" +
					"def pythonObjectMirror := \n" +
					"  mirror: { \n" +
					"    def select( receiver, symbol ) { \n" +
					"      try: { \n" +
					"        super^select( receiver, symbol ); \n" +
					"      } catch: SelectorNotFound using: { | e | \n" +
					"        super^defineField( symbol, defaultValue ); \n" +
					"        defaultValue; \n" +
					"      } \n" +
					"    } \n" +
					"  }; \n" +
					"def test := object: { nil } \n" +
					"  mirroredBy: pythonObjectMirror; \n" +
					"if: (test.x != defaultValue) then: { fail(); };",
					ctx_);			
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
	
	/**
	 * To avoid improper interference with the interpreter, user code should never throw  
	 * interpreter exceptions. However, in the light that various components of the language 
	 * may be reimplemented in the language itself, this functionality is supported by the 
	 * interpreter anyhow.
	 * 
	 * Note that given the ability to catch interpreter exceptions, the programmer automatically
	 * has the right to throw them as well, either by rethrowing them, or by storing it as a 
	 * prototype, of which new clones can be instatiated whenever he feels like it.
	 * 
	 * This test consist of an object model where access to a field can be forbidden by throwing
	 * an interpreter exception (striped with SelectorNotFound)
	 */
	public void testInterpreterExceptionThrowing() {
		try {
			AmbientTalkTest.evalSnippet(ExceptionHandlingTest.class, "snippet1", ctx_);
			// fail if no exception was thrown by the code. 
			fail();
		} catch (XSelectorNotFound e) {
			// 1. Raising a Java Exception Successfull
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
	
	/**
	 * When rethrowing an exception from a handler, the expected semantics apply : no handlers
	 * from the same try block are tried, even if they also match the thrown exception. Handlers
	 * from a try block higher up the stack can however apply.
	 *
	 */
	public void testRethrownExceptions() {
		try {
			evaluateInput(
					"defstripe MyExceptions; \n" +
					"defstripe IncorrectArguments <: MyExceptions; \n" +
					"\n" +
					"def result := false;" +
					"def test := object: { \n" +
					"    def test := 0; \n" +
					"} stripedWith: [ IncorrectArguments ]; \n" +
					"\n" +
					"try: { \n" +
					"  try: { \n" +
					"    raise: test; \n" +
					"  } catch: MyExceptions using: { | exc | \n" +
					"      result := true; \n" +
					"      raise: exc; \n" +
					"  } catch: IncorrectArguments using: { | exc |" +
					"      fail();" +
					"  }" +
					"} catch: SelectorNotFound using: { | exc | \n" +
					"  fail(); \n" +
					"} catch: IncorrectArguments using: { | exc | \n" +
					"  result := result.and: { true }; \n" +
					"}; \n" +
					"\n" +
					"if: (! result) then: { fail(); }",					
					ctx_);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ExceptionHandlingTest.class);
	}
		
	// For testing purposes we need access to a set of native stripes
	// These are introduced into the global lexical scope of the test 
	private void setUpTestStripes(ATObject testScope) throws Exception {
		
		// Primitive type stripes used to test all objects can be thrown
		testScope.meta_defineField(
				AGSymbol.jAlloc("Number"),
				NativeStripes._NUMBER_);

		testScope.meta_defineField(
				AGSymbol.jAlloc("Table"),
				NativeStripes._TABLE_);

		testScope.meta_defineField(
				AGSymbol.jAlloc("Closure"),
				NativeStripes._CLOSURE_);

		testScope.meta_defineField(
				AGSymbol.jAlloc("Stripe"),
				NativeStripes._STRIPE_);
		
		testScope.meta_defineField(
				AGSymbol.jAlloc("SelectorNotFound"),
				NativeStripes._SELECTORNOTFOUND_);
	}
	
	public void setUp() throws Exception {
		ATObject globalLexScope = Evaluator.getGlobalLexicalScope();
		ATObject testScope = new NATCallframe(globalLexScope);
				
		setUpTestStripes(testScope);

		// For throwing InterpreterExceptions, we provide a useful prototype to start from
		testScope.meta_defineField(
				AGSymbol.jAlloc("doesNotUnderstandX"),
				new NATException(new XSelectorNotFound(
						AGSymbol.jAlloc("nativeException"),
						globalLexScope)));
		
		ctx_ = new NATContext(testScope, globalLexScope);

		// Aux method to aid in shortening the test code
		evaluateInput(
				"def raise: exception catch: stripe do: closure { \n" +
				"  try: {" +
				"    raise: exception;" +
				"  } catch: stripe using: closure; \n" +
				"}",
				ctx_);
	}
	
	
	

}