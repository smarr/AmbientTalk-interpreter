/**
 * AmbientTalk/2 Project
 * SymbiosisTest.java created on 13-nov-2006 at 15:10:58
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XSymbiosisFailure;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.objects.natives.grammar.TestEval;

import java.awt.event.ActionListener;

/**
 * Tests the symbiosis with Java. This is primarily done by wrapping
 * the SymbiosisTest class and instances itself and invoking some of
 * their non-test prefixed methods.
 * 
 * @author tvcutsem
 */
public class SymbiosisTest extends AmbientTalkTest {
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(TestEval.class);
	}
	
	// test fixture
	private Class jTestClass;
	private JavaClass atTestClass;
	
	private SymbiosisTest jTestObject;
	private JavaObject atTestObject;
	
	// these fields and methods will be reflectively invoked from within AmbientTalk
	public int xtest;
	public static String ytest = "AmbientTalk";
	
	public SymbiosisTest(int xval) {
		xtest = xval;
	}
	
	public SymbiosisTest() {
		xtest = 0;
	}
	
	public static final int TEST_OBJECT_INIT = 42;
	
	public int gettertest() { return xtest; }
	public void settertest(int xval) { xtest = xval; }
	public static String prefix(String msg) { return msg + ytest; }
	public boolean identitytest(Object obj) { return obj == this; }
	
	public String overloadedtest() { return "()"; }
	public String overloadedtest(int x) { return "(int)"; }
	public String overloadedtest(Object[] vals) { return "(Object[])"; }
	public String overloadedtest(double x) { return "(double)"; }
	public String overloadedtest(SymbiosisTest x) { return "(SymbiosisTest)"; }
	
	public Object overloadedvararg(ATObject[] varargs) { return null; }
	public int overloadedvararg(int x) { return x; }
	
	public String overloadedmatch2(Object x) { return "(Object)"; }
	public String overloadedmatch2(SymbiosisTest x) { return "(SymbiosisTest)"; }
	
	public void setUp() {
		jTestClass = SymbiosisTest.class;
		atTestClass = JavaClass.wrapperFor(SymbiosisTest.class);
		
		jTestObject = new SymbiosisTest(TEST_OBJECT_INIT);
		atTestObject = JavaObject.wrapperFor(jTestObject);
	}
	
	/**
	 * Test the conversion function Symbiosis.ambientTalkToJava for various kinds of input.
	 */
	public void testAT2JavaConversion() {
		try {
			// -- WRAPPED JAVA OBJECTS --
			assertEquals(jTestObject, Symbiosis.ambientTalkToJava(atTestObject, SymbiosisTest.class));
			// -- PRIMITIVE TYPES --
			assertEquals(new Integer(5), Symbiosis.ambientTalkToJava(NATNumber.atValue(5), int.class));
			// -- STRINGS --
			assertEquals(ytest, Symbiosis.ambientTalkToJava(NATText.atValue(ytest), String.class));
			// -- ARRAYS --
			assertEquals(0.5, ((double[]) Symbiosis.ambientTalkToJava(new NATTable(new ATObject[] { NATFraction.atValue(0.5) }), double[].class))[0], 0.0);
			// -- EXCEPTIONS --
			assertEquals(XIllegalOperation.class, Symbiosis.ambientTalkToJava(new NATException(new XIllegalOperation()), Exception.class).getClass());
			// -- CLASS OBJECTS --
			assertEquals(jTestClass, Symbiosis.ambientTalkToJava(atTestClass, Class.class));
			// -- nil => NULL --
			assertEquals(null, Symbiosis.ambientTalkToJava(NATNil._INSTANCE_, ATObject.class));
			// -- INTERFACE TYPES AND NAT CLASSES --
			assertTrue(Symbiosis.ambientTalkToJava(new NATObject(), ActionListener.class) instanceof ActionListener);
			try {
				Symbiosis.ambientTalkToJava(new NATObject(), Symbiosis.class);
				fail();
			} catch (XTypeMismatch e) {
				// expected: coercion does not work for non-interface class types
			}
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test the conversion function Symbiosis.javaToAmbientTalk for various kinds of input.
	 */
	public void testJava2ATConversion() {
		try {
			// -- WRAPPED JAVA OBJECTS --
			assertEquals(atTestObject, Symbiosis.javaToAmbientTalk(jTestObject));
			// -- PRIMITIVE TYPES --
			assertEquals(NATNumber.atValue(5), Symbiosis.javaToAmbientTalk(new Integer(5)));
			// -- STRINGS --
			assertEquals(NATText.atValue(ytest), Symbiosis.javaToAmbientTalk(ytest));
			// -- ARRAYS --
			assertEquals(NATFraction.atValue(0.5), Symbiosis.javaToAmbientTalk(new double[] { 0.5 }).asNativeTable().elements_[0]);
			// -- EXCEPTIONS --
			assertEquals(XIllegalOperation.class, Symbiosis.javaToAmbientTalk(new XIllegalOperation()).asNativeException().getWrappedException().getClass());
			// -- CLASS OBJECTS --
			assertEquals(atTestClass, Symbiosis.javaToAmbientTalk(jTestClass));
			// -- nil => NULL --
			assertEquals(NATNil._INSTANCE_, Symbiosis.javaToAmbientTalk(null));
			// -- INTERFACE TYPES AND NAT CLASSES --
			ATObject orig = new NATObject();
			Object proxy = Symbiosis.ambientTalkToJava(orig, ActionListener.class);
			assertEquals(orig, Symbiosis.javaToAmbientTalk(proxy));
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Invokes the two instance methods gettertest and settertest on atTestObject.
	 * Also performs a selection of the field 'xtest'
	 * 
	 * Also invokes the method 'identitytest' to see whether AT->Java conversion does proper unwrapping
	 */
	public void testWorkingInstanceInvocation() {
		try {
			// def result := atTestObject.gettertest(); assert(42 == result)
			ATObject result = atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("gettertest"), NATTable.EMPTY);
			assertEquals(TEST_OBJECT_INIT, result.asNativeNumber().javaValue);
			
			// result := atTestObject.settertest(1); assert(result == nil); assert(atTestObject.xtest == 1)
			result = atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("settertest"), new NATTable(new ATObject[] { NATNumber.ONE }));
			assertEquals(NATNil._INSTANCE_, result);
			assertEquals(1, jTestObject.xtest);
			result = atTestObject.meta_select(atTestObject, AGSymbol.alloc("xtest"));
			assertEquals(NATNumber.ONE, result);
			
			assertTrue(atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("identitytest"),
					                           new NATTable(new ATObject[] { atTestObject })).asNativeBoolean().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Invokes the class method 'prefix' and performs a selection and assignment of the static 'ytest' field
	 */
	public void testWorkingClassInvocation() {
		try {
			// def result := atTestClass.prefix("Hello, "); assert("Hello, " + ytest == result)
			String txt = "Hello, ";
			NATText prefix = NATText.atValue(txt);
			ATObject result = atTestClass.meta_invoke(atTestClass, AGSymbol.alloc("prefix"), new NATTable(new ATObject[] { prefix }));
			assertEquals(txt + ytest, result.asNativeText().javaValue);
			
			// result := atTestClass.ytest; assert(result == ytest);
			result = atTestClass.meta_select(atTestClass, AGSymbol.alloc("ytest"));
			assertEquals(ytest, result.asNativeText().javaValue);
			
			// atTestClass.ytest := "Hello, "; assert(ytest == "Hello, ")
			result = atTestClass.meta_assignField(atTestClass, AGSymbol.alloc("ytest"), prefix);
			assertEquals(NATNil._INSTANCE_, result);
			assertEquals(txt, ytest);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}		
	}
	
	/**
	 * Invokes the method 'gettertest' with one argument instead of zero.
	 */
	public void testFaultyArity() {
		try {
			atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("gettertest"), new NATTable(new ATObject[] { NATNil._INSTANCE_ }));
			fail("Expected an arity mismatch exception");
		} catch(XArityMismatch e) {
			// expected exception: success
		} catch(InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Invokes the method 'settertest' with a double (fraction) instead of an int (number)
	 */
	public void testIllegalArgs() {
		try {
			atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("settertest"), new NATTable(new ATObject[] { NATFraction.atValue(0.1) }));
			fail("Expected an illegal argument exception");
		} catch(XTypeMismatch e) {
			// Java expects an int, so AT expects a native number, but is given a native fraction
			if (e.getExpectedType() == NATNumber.class) {
				// expected exception: success
			} else {
				fail(e.getMessage());
			}
		} catch(InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tries to assign to a final public field
	 */
	public void testIllegalAssignment() {
		try {
			atTestClass.meta_assignField(atTestClass, AGSymbol.alloc("TEST_OBJECT_INIT"), NATNumber.atValue(0));
			fail("Expected an illegal assignment exception");
		} catch(XUnassignableField e) {
			// expected exception: success
		} catch(InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test whether variable arguments work
	 */
	public void testVarArgInvocation() {
		try {
			ATObject result = atTestObject.meta_invoke(atTestObject,
													AGSymbol.alloc("overloadedvararg"),
													new NATTable(new ATObject[] { NATNumber.ZERO, NATNumber.ONE }));
			assertEquals(NATNil._INSTANCE_, result);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether overloaded methods can be properly invoked if they can be resolved
	 * to one method using the actual arguments.
	 */
	public void testOverloadedInvWithOneMatch() {
		try {
			// invokes overloadedtest(int)
			ATObject result = atTestObject.meta_invoke(atTestObject,
													AGSymbol.alloc("overloadedtest"),
													new NATTable(new ATObject[] { NATNumber.ZERO }));
			assertEquals("(int)", result.asNativeText().javaValue);
			// invokes overloadedtest(SymbiosisTest)
			result = atTestObject.meta_invoke(atTestObject,
											AGSymbol.alloc("overloadedtest"),
											new NATTable(new ATObject[] { atTestObject }));
			assertEquals("(SymbiosisTest)", result.asNativeText().javaValue);
			// invokes overloadedtest()
			result = atTestObject.meta_invoke(atTestObject,
											AGSymbol.alloc("overloadedtest"),
											NATTable.EMPTY);
			assertEquals("()", result.asNativeText().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Invokes an overloaded method where the symbiosis cannot disambiguate automatically.
	 */
	public void testOverloadedInvWithMultipleMatches() {
		try {
			// invokes overloadedmatch2(Object|SymbiosisTest) => error
			atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("overloadedmatch2"),
											     new NATTable(new ATObject[] { atTestObject }));
			fail("Expected a symbiosis exception");
		} catch (XSymbiosisFailure e) {
			// success: expected exception
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Invokes an overloaded method that does not match the specified argument type
	 */
	public void testOverloadedInvWithNoMatch() {
		try {
			// invokes overloadedtest(NATObject) => error
			atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("overloadedtest"),
											     new NATTable(new ATObject[] { new NATObject() }));
			fail("Expected a symbiosis exception");
		} catch (XSymbiosisFailure e) {
			// success: expected exception
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Invokes a method that is not defined in the class.
	 */
	public void testNonExistentMethod() {
		try {
			// invokes foo(1) => error
			atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("foo"),
											     new NATTable(new ATObject[] { NATNumber.ONE }));
			fail("Expected a selector not found exception");
		} catch (XSelectorNotFound e) {
			// success: expected exception
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests first-class field access for both instances and classes
	 */
	public void testFirstClassFields() {
		
	}
	
	/**
	 * Tests first-class method access for both instances and classes
	 */
	public void testFirstClassMethods() {
		
	}
	
	/**
	 * Tests casting to manually resolve overloaded method invocations
	 */
	public void testCasting() {
		
	}
	
	/**
	 * Tests whether the parent pointers of the AT symbionts refer to the proper objects.
	 */
	public void testSymbiontParents() {
		try {
			// the parent of atTestObject is atTestClass
			assertEquals(atTestClass, atTestObject.meta_getDynamicParent());
			// the parent of atTestClass is a wrapper for the class AmbientTalkTest
			assertEquals(JavaClass.wrapperFor(AmbientTalkTest.class), atTestClass.meta_getDynamicParent());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether new per-instance methods and fields can be added to a wrapped Java object.
	 */
	public void testSymbiontInstanceAdditions() {
		
	}

	/**
	 * Tests whether new per-class methods and fields can be added to a wrapped Java class.
	 * Also tests whether existing instances can make use of these newly added methods
	 */
	public void testSymbiontClassAdditions() {
		
	}
	
	/**
	 * Tests the invocation of new on wrapped classes.
	 */
	public void testInstanceCreation() {
		
	}
	
	/**
	 * Tests cloning behaviour for both wrapped class instances and classes.
	 */
	public void testCloning() {
		
	}
}
