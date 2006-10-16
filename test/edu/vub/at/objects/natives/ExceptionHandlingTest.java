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
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XSelectorNotFound;
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
		
	private ATObject globalLexScope;
	private ATObject testScope;
	private ATContext testCtx;

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ExceptionHandlingTest.class);
	}
	
	public void setUp() throws Exception {
		globalLexScope = Evaluator.getGlobalLexicalScope();
		testScope = new NATCallframe(globalLexScope);
		
		final NATClosure symbol = new JavaClosure(NATNil._INSTANCE_) {
			public ATObject base_apply(ATTable arguments) throws InterpreterException {
				return AGSymbol.alloc(arguments.base_at(NATNumber.ONE).asNativeText());
			}				
		};
		
		testScope.meta_defineField(AGSymbol.alloc("symbol"), symbol);

		testScope.meta_defineField(
				AGSymbol.alloc("echo:"),
				new JavaClosure(NATNil._INSTANCE_) {
					public ATObject base_apply(ATTable arguments) throws InterpreterException {
						System.out.println(arguments.base_at(NATNumber.ONE).meta_print().javaValue);
						return NATNil._INSTANCE_;
					}						
				});
		
		testScope.meta_defineField(
				AGSymbol.alloc("doesNotUnderstandX"),
				new NATException(new XSelectorNotFound(
						AGSymbol.alloc("nativeException"),
						globalLexScope)));
		
		testCtx = new NATContext(
				testScope, globalLexScope, globalLexScope.meta_getDynamicParent());
	}
	
	/**
	 * Tests AT Code raising an interpreter exception
	 */
	public void testRaiseInterpreterException() throws InterpreterException {
		try {
			evaluateInput("raise: doesNotUnderstandX; \n", testCtx);
		} catch (XSelectorNotFound e) {
			// 1. Raising a Java Exception Successfull
		}
	}
	
	/**
	 * Tests AT Code raising an newly create (cloned) interpreter exception
	 */
	public void testRaiseNewInterpreterException() throws InterpreterException {
		try {			
			evaluateInput(
					"raise: doesNotUnderstandX.new(symbol(\"at\") , object: { nil }); \n" +
					"fail()", testCtx);
		} catch (XSelectorNotFound e) {
			// 1b. Raising a Java Exception Successfull
		}
	}
	
	/**
	 * Tests AT Code catching an interpreter exception. It implements a sketchy 
	 * object model where fields can be removed as well.
	 *
	 */
	public void testInterpreterExceptionThrowing() {
		try {						
			
			//1. (REMOVABLE_FIELDS) AT Code throwing an interpreter exception
			evaluateInput(
					"def removableFieldsMirror := \n" +
					"  mirror: { \n" +
					"    // vectors are not loaded with these unit tests \n" +
					"    def removedField := nil;" +
					"    def removeField( symbol ) { \n" +
					"      removedField := symbol; \n" +
					"    }; \n" +
					"    def select( receiver, symbol ) { \n" +
					"      if: (symbol == removedField) then: { \n" +
					"        raise: doesNotUnderstandX.new(symbol , self); \n" +
					"      } else: { \n" +
					"        super.select( receiver, symbol ); \n" +
					"      } \n" +
					"    } \n" +
					"  }; \n" +
					"def test := object: { \n" +
					"  def visible := nil \n" +
					"} mirroredBy: removableFieldsMirror; \n" +
					"\n" +
					"(reflect: test).removeField(symbol(\"visible\")); \n" +
					"(test.visible == nil).ifTrue: { fail(); };",
					testCtx);			
		} catch (XSelectorNotFound e) {
			// 1. Raising a Java Exception Successfull
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}	
	
	/**
	 * Tests AT Code catching an interpreter exception. It implements a sketchy python
	 * object model where unfound fields are silently added to AT object.
	 *
	 */
	public void testInterpreterExceptionHandling() {
		try {
			ATObject globalLexScope = Evaluator.getGlobalLexicalScope();
			ATObject testScope = new NATCallframe(globalLexScope);
			
			testScope.meta_defineField(
					AGSymbol.alloc("echo:"),
					new JavaClosure(NATNil._INSTANCE_) {
						public ATObject base_apply(ATTable arguments) throws InterpreterException {
							System.out.println(arguments.base_at(NATNumber.ONE).meta_print().javaValue);
							return NATNil._INSTANCE_;
						}						
					});			testScope.meta_defineField(
					AGSymbol.alloc("doesNotUnderstandX"),
					new NATException(new XSelectorNotFound(
							AGSymbol.alloc("nativeException"),
							globalLexScope)));
			
			ATContext testCtx = new NATContext(
					testScope, globalLexScope, globalLexScope.meta_getDynamicParent());
			
			//3. (PYTHON_OBJECT) AT Code catching an interpreter exception
			evaluateInput(
					"def pythonObjectMirror := \n" +
					"  mirror: { \n" +
					"    def select( receiver, symbol ) { \n" +
					"      try: { \n" +
					"        super.select( receiver, symbol ); \n" +
					"      } catch: doesNotUnderstandX using: { | e | \n" +
					"        super.defineField( symbol, nil ); \n" +
					"      } \n" +
					"    } \n" +
					"  }; \n" +
					"def test := object: { nil } \n" +
					"  mirroredBy: pythonObjectMirror; \n" +
					"(test.x == nil).ifFalse: { fail(); };",
					testCtx);			
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
	
	public void testCustomObjectHandling() {
		try {			
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
					
					 // + "tryTest(XNotFound, extend: XNotFound with: { nil }, { fail() });",
					testCtx);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
	
	public void testRethrownExceptions() {
		try {
			evaluateInput(
					"def closure := { | e | \n" +
					"  raise: e; \n" +
					"}; \n" +
					"\n" +
					"def exception: code { object: code }; \n" +
					"def XNotFound := \n" +
					"  exception: { \n" +
					"    def test := 0; \n" +
					"  }; \n" +
					"\n" +
					"closure.withHandler: ( handle: XNotFound with: { | e |" +
					"    echo: \"5a. Rethrown exceptions should not be catched in the same handler block\";" +
					// TODO(wtf?) if the following line is commented out the program prints the above string twice  
					"    raise: e; \n" +
					"} ); \n" +
					"closure.withHandler: ( handle: XNotFound with: { | e | fail() } );" +
					"\n" +
					"try: { \n" +
					"  closure(XNotFound); \n" +
					"} catch: XNotFound using: { | e | \n" +
					"  echo: \"5b. Rethrown exceptions are not catched in the same handler block\" \n" +
					"}; \n" +
					"\n",					
					 // + "tryTest(XNotFound, extend: XNotFound with: { nil }, { fail() });",
					testCtx);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
	
	public void testHandlerSelection() {
		try {
			evaluateInput(
					"def closure := { | e | \n" +
					"  raise: e; \n" +
					"}; \n" +
					"\n" +
					"def exception: code { object: code }; \n" +
					"def XNotFound := \n" +
					"  exception: { \n" +
					"    def test := 0; \n" +
					"  }; \n" +
					"\n" +
					"def XSelectorNotFound := \n" +
					"  extend: XNotFound with: { \n" +
					"    def selector := \"\"; \n" +
					"    def init(s) { \n" +
					"      selector := s; \n" +
					"    }; \n" +
					"  }; \n" +
					"\n" +
					"closure.withHandler: ( handle: XSelectorNotFound with: { | e |" +
					"    fail(); \n" +
					"} ); \n" +
					"closure.withHandler: ( handle: XNotFound with: { | e | \n" +
					"    echo: \"6. Handler Selection Correct.\" \n } );" +
					"\n" +
					"closure(XNotFound); \n" +
					"\n",					
					testCtx);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
}