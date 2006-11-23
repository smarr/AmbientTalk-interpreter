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
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XNotInstantiatable;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XSymbiosisFailure;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
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

import javax.swing.JTextField;

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
		/*Test test= new SymbiosisTest() {
		        public void runTest() throws Exception {
		        	  testBugfixOverloadedConstructor();
		        }
		 };
		 junit.textui.TestRunner.run(test);*/
	}
	
	// test fixture
	private Class jTestClass;
	private JavaClass atTestClass;
	
	private SymbiosisTest jTestObject;
	private JavaObject atTestObject;
	
	private JavaPackage jLobby_;
	
	// these fields and methods will be reflectively invoked from within AmbientTalk
	public int xtest;
	public static String ytest = "AmbientTalk";
	
	public SymbiosisTest(int xval) {
		xtest = xval;
	}
	
	public SymbiosisTest() {
		xtest = 0;
	}

	public SymbiosisTest(SymbiosisTest t) {
		xtest = -1;
	}
	
	public SymbiosisTest(AmbientTalkTest t) {
		xtest = -1;
	}
	
	private static class ExceptionTest extends Exception {
		private static final long serialVersionUID = 1L;
	}

	public SymbiosisTest(JavaClass c) throws ExceptionTest {
		throw new ExceptionTest();
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
		
		jLobby_ = new JavaPackage("");
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
		try {
			// def result := (reflect: atTestObject).grabField("xtest")
			ATField result = atTestObject.meta_grabField(AGSymbol.alloc("xtest")).base_asField();
			assertEquals("xtest", result.base_getName().toString());
			assertEquals(TEST_OBJECT_INIT, result.base_getValue().asNativeNumber().javaValue);
			
			// result := (reflect: atTestClass).grabField("ytest")
			result = atTestClass.meta_grabField(AGSymbol.alloc("ytest")).base_asField();
			assertEquals("ytest", result.base_getName().toString());
			assertEquals(ytest, result.base_getValue().asNativeText().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests first-class method access for both instances and classes
	 */
	public void testFirstClassMethods() {
		try {
			// def result := (reflect: atTestObject).grabMethod("gettertest")
			ATMethod result = atTestObject.meta_grabMethod(AGSymbol.alloc("gettertest")).base_asMethod();
			assertEquals("gettertest", result.base_getName().toString());
			// assert (42 == result())
			assertEquals(TEST_OBJECT_INIT, result.base_apply(NATTable.EMPTY, null).asNativeNumber().javaValue);
			
			// result := (reflect: atTestClass).grabMethod("prefix")
			result = atTestClass.meta_grabMethod(AGSymbol.alloc("prefix")).base_asMethod();
			assertEquals("prefix", result.base_getName().toString());
			// assert ("AmbientTalk" == result(""))
			assertEquals(ytest, result.base_apply(new NATTable(new ATObject[] { NATText.atValue("") }), null).asNativeText().javaValue);	
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests casting to manually resolve overloaded method invocations
	 * FIXME: test no longer works because methods are wrapped in closures, and they don't
	 * understand cast. Rethink casting, because current scheme also does not work for constructors.
	 * Suggested solution: use future annotations on message sends to specify the types
	 */
	public void testCasting() {
		try {
			// invokes overloadedmatch2(SymbiosisTest) via explicit casting
			ATObject method = atTestObject.meta_select(atTestObject, AGSymbol.alloc("overloadedmatch2"));
			ATObject castedMethod = method.meta_invoke(method, AGSymbol.alloc("cast"), new NATTable(new ATObject[] { atTestClass }));
			castedMethod.base_asMethod().base_apply(new NATTable(new ATObject[] { atTestObject }), null);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether the parent pointers of the AT symbionts refer to the proper objects.
	 */
	public void testSymbiontParents() {
		try {
			// the dynamic parent of atTestObject is atTestClass
			assertEquals(atTestClass, atTestObject.meta_getDynamicParent());
			// the dynamic parent of atTestClass is nil
			assertEquals(NATNil._INSTANCE_, atTestClass.meta_getDynamicParent());
			
			// the lexical parent of atTestObject is the lexical root
			assertEquals(Evaluator.getGlobalLexicalScope(), atTestObject.meta_getLexicalParent());
			// the lexical parent of atTestClass is the lexical root
			assertEquals(Evaluator.getGlobalLexicalScope(), atTestClass.meta_getLexicalParent());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether new per-instance methods and fields can be added to a wrapped Java object.
	 */
	public void testSymbiontInstanceAdditions() {
		try {
			// (reflect: atTestObject).defineField("x", 1)
			atTestObject.meta_defineField(AGSymbol.alloc("x"), NATNumber.ONE);
			// assert(atTestObject.x == 1)
			assertEquals(NATNumber.ONE, atTestObject.meta_select(atTestObject, AGSymbol.alloc("x")));
			
			// (reflect: atTestObject).addMethod(<method:"foo",[x],{x}>)
			ATMethod foo = evalAndReturn("def foo(x) { x }; foo").base_asClosure().base_getMethod();
			atTestObject.meta_addMethod(foo);
			// assert(atTestObject.foo(0) == 0)
			assertEquals(NATNumber.ZERO, atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("foo"),
					new NATTable(new ATObject[] { NATNumber.ZERO })));
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether no duplicate methods or fields can be added to a wrapped Java object.
	 */
	public void testSymbiontDuplicates() {
		try {
			try {
				// def atTestObject.xtest := 1
				atTestObject.meta_defineField(AGSymbol.alloc("xtest"), NATNumber.ONE);
				fail("expected a duplicate slot exception");
			} catch (XDuplicateSlot e) {
			    // expected exception: success
			}
			try {
				// def atTestObject.gettertest() { nil }
				ATMethod getter = evalAndReturn("def gettertest() { nil }; gettertest").base_asClosure().base_getMethod();
				atTestObject.meta_addMethod(getter);
				fail("expected a duplicate slot exception");
			} catch (XDuplicateSlot e) {
			    // expected exception: success
			}
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests whether new per-class methods and fields can be added to a wrapped Java class.
	 * Also tests whether existing instances can make use of these newly added methods
	 */
	public void testSymbiontClassAdditions() {
		try {
			// (reflect: atTestClass).defineField("z", 1)
			atTestClass.meta_defineField(AGSymbol.alloc("z"), NATNumber.ONE);
			// assert(atTestClass.z == 1)
			assertEquals(NATNumber.ONE, atTestClass.meta_select(atTestClass, AGSymbol.alloc("z")));
			// assert(aTestObject.z == 1) -> delegation to class
			assertEquals(NATNumber.ONE, atTestObject.meta_select(atTestObject, AGSymbol.alloc("z")));
			
			// (reflect: atTestClass).addMethod(<method:"get",[],{self.xtest}>)
			ATMethod get = evalAndReturn("def get() { self.xtest }; get").base_asClosure().base_getMethod();
			atTestClass.meta_addMethod(get);
			// assert(atTestObject.xtest == atTestObject.get())
			assertEquals(atTestObject.meta_select(atTestObject, AGSymbol.alloc("xtest")),
					     atTestObject.meta_invoke(atTestObject, AGSymbol.alloc("get"), NATTable.EMPTY));
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests cloning behaviour for both wrapped class instances and classes.
	 */
	public void testCloning() {
		try {
			// cloning a class results in the same class
			assertEquals(atTestClass, atTestClass.meta_clone());
			
			try {
				// cloning a java object results in an error
				atTestObject.meta_clone();
				fail("expected an illegal operation exception");
			} catch (XIllegalOperation e) {
				// expected exception: success
			}
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests the invocation of new on a wrapped Java Class.
	 * Instantiates the Java class via the default init implementation.
	 */
	public void testWorkingInstanceCreation() {
		try {
			// def instance := atTestClass.new(1)
			ATObject instance = atTestClass.meta_newInstance(new NATTable(new ATObject[] { NATNumber.ONE }));
			assertEquals(JavaObject.class, instance.getClass());
			assertEquals(NATNumber.ONE, instance.meta_select(instance, AGSymbol.alloc("xtest")));
			
			Object realInstance = instance.asJavaObjectUnderSymbiosis().getWrappedObject();
			assertEquals(SymbiosisTest.class, realInstance.getClass());
			assertEquals(1, ((SymbiosisTest) realInstance).xtest);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether classes with private constructors terminate cleanly.
	 */
	public void testNonInstantiatableCreation() {
		try {
			// def instance := JavaObject.new(1)
			JavaClass.wrapperFor(JavaObject.class).meta_newInstance(new NATTable(new ATObject[] { NATNumber.ONE }));
			fail("expected a not instantiatable exception");
		} catch (XNotInstantiatable e) {
			// expected exception: success
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether incorrect arguments passed to constructor terminate cleanly.
	 */
	public void testIllegalArgsInstanceCreation() {
		try {
			// def instance := atTestClass.new(1.0)
			atTestClass.meta_newInstance(new NATTable(new ATObject[] { NATFraction.atValue(1.0) }));
			fail("expected a symbiosis failure with 0 matches");
		} catch (XSymbiosisFailure e) {
			// expected exception: success
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether overloaded constructors which cannot be resolved terminates cleanly.
	 */
	public void testOverloadedInstanceCreation() {
		try {
			// def instance := atTestClass.new(atTestObject) => 2 matches
			atTestClass.meta_newInstance(new NATTable(new ATObject[] { atTestObject }));
			fail("expected a symbiosis failure with 2 matches");
		} catch (XSymbiosisFailure e) {
			// expected exception: success
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests an instance creation that raises an exception
	 */
	public void testExceptionInInstanceCreation() {
		try {
			// def instance := atTestClass.new(atTestClass)
			atTestClass.meta_newInstance(new NATTable(new ATObject[] { atTestClass }));
			fail("expected the constructor to throw an exception");
		} catch (XJavaException e) {
			// expected exception: success if it was an ExceptionTest instance
			assertEquals(ExceptionTest.class, e.getWrappedJavaException().getClass());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests the invocation of new on a wrapped Java Object, rather than on a Java Class.
	 */
	public void testCreationViaJavaObject() {
		try {
			// def instance := atTestObject.new(55)
			ATObject instance = atTestObject.meta_newInstance(
					new NATTable(new ATObject[] { NATNumber.atValue(55) }));
			
			assertEquals(55, instance.meta_select(instance, AGSymbol.alloc("xtest")).asNativeNumber().javaValue);
			assertEquals(atTestClass, instance.meta_getDynamicParent());
			assertEquals(jTestObject.xtest, atTestObject.meta_select(atTestObject,
					AGSymbol.alloc("xtest")).asNativeNumber().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests the invocation of new on a wrapped Java Class.
	 * Instantiates the Java class via a custom init implementation.
	 * 
	 * BEWARE: this test should be the last for testing symbiotic instance creation as it
	 * MODIFIES the test fixture (the JavaClass wrapper object)! Ths is because the JavaClass wrapper
	 * is pooled and reused throughout subsequent tests.
	 */
	public void testCustomInstanceCreation() {
		try {
			// def atTestClass.init(x) { def o := super.init(x); def o.ytest := y; o }
			ATClosure init = evalAndReturn("def init(x,y) { def o := super.init(x); def o.ytest := y; o }; init").base_asClosure();
			atTestClass.meta_addMethod(init.base_getMethod());
			
			// def instance := atTestClass.new(10, 11)
			ATObject instance = atTestClass.meta_newInstance(new NATTable(new ATObject[] { NATNumber.atValue(10), NATNumber.atValue(11) }));
			
			assertEquals(10, instance.meta_select(instance, AGSymbol.alloc("xtest")).asNativeNumber().javaValue);
			assertEquals(11, instance.meta_select(instance, AGSymbol.alloc("ytest")).asNativeNumber().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests whether jlobby.java results in a new JavaPackage.
	 * Tests whether jlobby.java.lang results in a new JavaPackage.
	 * Tests whether jlobby.java.lang.Object results in the proper loading of that class
	 */
	public void testJLobbyPackageLoading() throws InterpreterException {
		ATObject jpkg = jLobby_.meta_select(jLobby_, AGSymbol.alloc("java"));
		assertEquals(JavaPackage.class, jpkg.getClass());
		assertTrue(jLobby_.meta_respondsTo(AGSymbol.alloc("java")).asNativeBoolean().javaValue);
		ATObject jlpkg = jpkg.meta_select(jpkg, AGSymbol.alloc("lang"));
		assertEquals(JavaPackage.class, jlpkg.getClass());
		assertTrue(jpkg.meta_respondsTo(AGSymbol.alloc("lang")).asNativeBoolean().javaValue);
		ATObject jObject = jlpkg.meta_select(jlpkg, AGSymbol.alloc("Object"));
		assertEquals(JavaClass.class, jObject.getClass());
		assertTrue(jlpkg.meta_respondsTo(AGSymbol.alloc("Object")).asNativeBoolean().javaValue);
	}
	
	/**
	 * Tests whether lowercase classes can be loaded via the class method of a JavaPackage.
	 */
	public void testJLobbyExplicitClassLoading() throws InterpreterException {
		ATObject eduVubAtObjectsSymbiosisPkg = new JavaPackage("edu.vub.at.objects.symbiosis.");

		// load the class manually: invoke pkg.class("lowercaseClassTest")
		ATObject cls = eduVubAtObjectsSymbiosisPkg.meta_invoke(
				eduVubAtObjectsSymbiosisPkg,
				AGSymbol.alloc("class"),
				new NATTable(new ATObject[] { AGSymbol.alloc("lowercaseClassTest") }));
		assertEquals(JavaClass.class, cls.getClass());
		assertTrue(eduVubAtObjectsSymbiosisPkg.meta_respondsTo(
				    AGSymbol.alloc("lowercaseClassTest")).asNativeBoolean().javaValue);
	}
	
	/**
	 * Tests whether access to a nonexistent class gives rise to a selector not found exception.
	 */
	public void testJLobbyNonexistentClassLoading() throws InterpreterException {
		try {
			jLobby_.meta_select(jLobby_, AGSymbol.alloc("Foo"));
			fail("expected a class not found exception");
		} catch (XClassNotFound e) {
			// success: expected exception
		}
	}
	
	/**
	 * Tests whether the uppercase package 'foo.Bar' can be loaded via the package method of a JavaPackage.
	 */
	public void testJLobbyExplicitPackageLoading() throws InterpreterException {
		// def fooPkg := jLobby.foo;
		ATObject fooPkg = jLobby_.meta_select(jLobby_, AGSymbol.alloc("foo"));
		// def BarPkg := foo.package(`Bar);
		ATObject BarPkg = fooPkg.meta_invoke(fooPkg,
										   AGSymbol.alloc("package"),
										   new NATTable(new ATObject[] { AGSymbol.alloc("Bar") }));
		assertEquals(JavaPackage.class, BarPkg.getClass());
		assertTrue(fooPkg.meta_respondsTo(AGSymbol.alloc("Bar")).asNativeBoolean().javaValue);
	}
	
	/**
	 * BUGFIX TEST: jlobby.javax.swing.JTextField.new(80) failed to discriminate between constructors
	 * JTextField(String) and JTextField(int), reason was that anything native was convertible to
	 * NATText and also to String. Fixed by reimplementing asNativeText in NATNil to throw a type
	 * exception as usual.
	 */
	public void testBugfixOverloadedConstructor() throws InterpreterException {
		// def jTextField := jLobby.javax.swing.JTextField;
		ATObject jTextField = JavaClass.wrapperFor(JTextField.class);
		// jTextField.new(80)
		jTextField.meta_invoke(jTextField, AGSymbol.alloc("new"), new NATTable(new ATObject[] { NATNumber.atValue(80) }));
	}
	
}

class lowercaseClassTest { }