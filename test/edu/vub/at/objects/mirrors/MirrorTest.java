/**
 * AmbientTalk/2 Project
 * MirrorTest.java created on Aug 11, 2006 at 11:27:03 PM
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

package edu.vub.at.objects.mirrors;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUserDefined;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.natives.grammar.AGSymbol;

public class MirrorTest extends AmbientTalkTest {
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirrorTest.class);
	}	
	
	protected void setUp() throws Exception {
		super.setUp();
		
		ctx_.base_getLexicalScope().meta_defineField(AGSymbol.jAlloc("Mirror"), NativeStripes._MIRROR_);
		
		evalAndReturn(
				"def at := object: { \n" +
				"  def mirrors := object: { \n" +
				"    def Factory := object: {" +
				"       def createMirror(o) { reflect: o }" +
				"    }" +
				"  }; \n" +
				"  def unit := object: { \n" +
				"    def XUnitFailed := object: { \n" +
				"      def message := \"Unittest Failed\"; \n" +
				"      def init(@args) { \n" +
				"        if: (args.length > 0) then: { \n" +
				"			message := args[1]; \n" +
				"        } \n" +
				"      } \n" +
				"    }; \n" +
				"    def fail( message ) { raise: XUnitFailed.new( message ) }; \n" +
				"  }; \n" +
				"}; \n" +
				"\n" +
				"def symbol( text ) { jlobby.edu.vub.at.objects.natives.grammar.AGSymbol.alloc( text ) }; \n");
	}

	/**
	 * This test tests invariants with respect to the cloning of both intercessive mirrors and 
	 * mirages in all possible forms.
	 */
	public void testMirageCloning() throws InterpreterException {
		ATObject meta    = evalAndReturn(
				"def meta := mirror: { nil }"); 
		ATObject subject = evalAndReturn(
				"def subject := object: { \n" +
				"  def field := `field; \n" +
				"  def canonical() { nil }; \n" +
				"  def keyworded: arg1 message: arg2 { nil }; \n" +
				"} mirroredBy: meta; \n");
		// test whether the new mirage has a clone of the mirror as parent
		ATObject result = evalAndReturn(
				"meta := reflect: subject;");
		
		assertNotSame(meta, result);
		assertTrue(result.meta_isCloneOf(meta).asNativeBoolean().javaValue);
		
		// For future comparisons wrt to cloning use the current mirror
		meta = result;
		
		// Sending clone to an intercessive mirror clones that mirror
		// as well as cloning the base object in that mirror
		result = evalAndReturn(
				"def cloneMirrored := meta.clone()");
		
		// Stratification & Cloning : result is a mirror, cloned from meta
		assertTrue(result.meta_isStripedWith(NativeStripes._MIRROR_).asNativeBoolean().javaValue);
		assertNotSame(result, meta);
		assertTrue(result.meta_isCloneOf(meta).asNativeBoolean().javaValue);
		
		// For future comparisons wrt to cloning use the current mirror
		meta = result;
		
		result = evalAndReturn(
				"cloneMirrored.base");
		
		// Cloning : Base should be a mirage, cloned from subject
		assertTrue(result instanceof NATMirage);
		assertNotSame(result, subject);
		assertTrue(result.meta_isCloneOf(subject).asNativeBoolean().javaValue);
		
		// For future comparisons wrt to cloning use the current base object
		subject = result;
		
		// Cloning a mirror, created a clone of the mirror with an empty mirage
		result = evalAndReturn(
				"clone: cloneMirrored");
		
		// Stratification & Cloning : result is a mirror, cloned from meta
		assertTrue(result.meta_isStripedWith(NativeStripes._MIRROR_).asNativeBoolean().javaValue);
		assertNotSame(result, meta);
		assertTrue(result.meta_isCloneOf(meta).asNativeBoolean().javaValue);

		// For future comparisons wrt to cloning use the current mirror
		meta = result;

		result = evalAndReturn(
				"subject := cloneMirrored.base");

		// Mirror Cloning : Base should be a mirage, yet not cloned from subject
		assertTrue(result instanceof NATMirage);
		assertNotSame(result, subject);
		assertFalse(result.meta_isCloneOf(subject).asNativeBoolean().javaValue);
		
		// For future comparisons wrt to cloning use the current base object
		subject = result;

		result = evalAndReturn(
				"subject := subject.new();");
		
		// Mirage.new() : Base should be a mirage, cloned from subject
		assertTrue(result instanceof NATMirage);
		assertNotSame(result, subject);
		assertTrue(result.meta_isCloneOf(subject).asNativeBoolean().javaValue);
		
		result = evalAndReturn(
				"reflect: subject;");

		// Mirage.new() : Mirror should be a NATObject, with the mirror stripe which is cloned from the original mirror
		assertTrue(result.meta_isStripedWith(NativeStripes._MIRROR_).asNativeBoolean().javaValue);
		assertNotSame(result, meta);
		assertTrue(result.meta_isCloneOf(meta).asNativeBoolean().javaValue);	


	}
	
	/**
	 * This test invokes all meta-level operations defined on objects and tests whether they 
	 * return the proper results. As all these meta-level operations should return mirrors on
	 * the 'actual' return values, this test also covers the stratification with respect to
	 * return values. A full test of stratified mirror access is provided below.
	 */
	public void testObjectMetaOperations() {
		ATObject subject = evalAndReturn(
				"def subject := object: { \n" +
				"  def field := `field; \n" +
				"  def canonical() { nil }; \n" +
				"  def keyworded: arg1 message: arg2 { nil }; \n" +
				"}; \n");
		evalAndCompareTo(
				"def mirror := reflect: subject;",
				"<mirror on:" + subject.toString() + ">");
		evalAndCompareTo(
				"mirror.dynamicParent;",
				"<mirror on:nil>");
		evalAndCompareTo(
				"mirror.print();",
				"<mirror on:\"" + subject.toString() + "\">");

	}

	/**
	 * In order to build a full reflective tower, it is necessary to be able to create and use 
	 * mirrors on mirrors as well. This test covers the creation and use of default introspective
	 * mirrors on mirrors
	 */
	public void testReflectingOnIntrospectiveMirrors() {
		evalAndCompareTo(
				"def meta := reflect: false; \n" +
				"def metaMeta := reflect: meta;",
				"<mirror on:<mirror on:false>>");
		evalAndCompareTo(
				"def select := metaMeta.select(meta, `select).base",
				"<native closure:select>");
		evalAndCompareTo(
				"def succeeded := select(false, `not)(); \n",
				"true");

	}
	
	/**
	 * In order to build a full reflective tower, it is necessary to be able to create and use 
	 * mirrors on mirrors as well. This test covers the creation and use of default introspective
	 * mirrors on custom intercessive mirrors
	 */
	public void testReflectingOnIntercessiveMirrors() {
		ATObject meta = evalAndReturn(
				"def meta := mirror: { nil }; \n");
		assertTrue(meta.toString().endsWith("[<stripe:Mirror>]>"));
		evalAndCompareTo(
				"def metaMeta := reflect: meta;",
				"<mirror on:"+ meta +">");
		evalAndCompareTo(
				"def defineField := metaMeta.select(meta, `defineField).base",
				"<native closure:defineField>");
		evalAndCompareTo(
				"defineField(`boolValue, true); \n" +
				"meta.base.boolValue",
				"true");
	}
	
	/** 
	 * To uphold stratification, values returned from invocations on a mirror should be 
	 * automatically wrapped in a mirror. When returning a value that is itself a mirror, this
	 * property should be upheld as otherwise it is impossible at the meta-level to distinguish
	 * whether the value of a field is a base or meta-level entity. This test illustrates this
	 * possible source for confusion.
	 */ 
	public void testMirrorWrapping() {
		evalAndReturn(
				"def subject := object: { \n" +
				"  def thisBase := nil; \n" +
				"  def thisMeta := nil; \n" +
				"}; \n" +
				"def mirror := reflect: subject; \n" +
				"subject.thisBase := subject; \n" +
				"subject.thisMeta := mirror;");
		ATObject base = evalAndReturn(
				"mirror.select(subject, `thisBase);");
		ATObject meta = evalAndReturn(
				"mirror.select(subject, `thisMeta);");
		
		assertNotSame(base, meta);
		assertEquals("<mirror on:"+ base.toString() + ">", meta.toString());
		
	}
	/**
	 * This test creates a mirror and attempts to use it in a non-stratified way.
	 * The test assumes failure if these attempts succeed, and continues if they
	 * result in the proper exception. The following tests are performed :
	 * 
	 * - Invoking base-level reflectee behaviour on a mirror
	 * - Return values of meta_operations are mirrors
	 * - Field selection from a mirror results in a mirror
	 * - TODO Field assignment on a mirror with a non-mirror value
	 *
	 */
	public void testStratification() { 
		
		evalAndReturn("def mirror  := reflect: true;");
		
		// Invoking base-level reflectee behaviour on a mirror. 
		// Throws XSelectorNotFound as: 
		// - the method meta_ifTrue on NATBoolean does not exist
		// - the method base_ifTrue on NATMirror does not exist
		evalAndTestException(
				"mirror.ifTrue: { nil }; \n",
				XSelectorNotFound.class);
		
		// Mirror consistency : return values are mirrors too.
		evalAndReturn(
				"def responds    := mirror.respondsTo( symbol(\"ifTrue:\") );" +
				"(is: responds stripedWith: Mirror)" +
				"   .ifFalse: { at.unit.fail(\"Return value is not a mirror\") };");
		
		// Throws XSelectorNotFound as responds is a mirror and: 
		// - the method meta_ifTrue on NATBoolean does not exist
		// - the method base_ifTrue on NATMirror does not exist
		evalAndTestException(
				"responds.ifTrue: { at.unit.fail(\"Can invoke base-level methods through a mirror\") };",
				XSelectorNotFound.class);		
	};
	
	
//	/**
//	 * Tests the correctness of the up-down relation in Java : 
//	 * - down(up(o)) == o
//	 */
//	public void testJavaMirrorBaseRelation() {
//		try {
//			ATObject[] objects 		= new ATObject[] { 
//					NATNil._INSTANCE_, NATBoolean._TRUE_, NATNumber.ZERO, new NATObject(),
//					NATTable.EMPTY, NATIntrospectiveMirror.atValue(NATNil._INSTANCE_),
//					new NATIntercessiveMirror(Evaluator.getGlobalLexicalScope(), true)
//			};
//			ATMirror[] mirrors 		= new ATMirror[objects.length];
//			
//			for (int i = 0; i < objects.length; i++) {
//				mirrors[i] = NATIntrospectiveMirror.atValue(objects[i]);
//			}
//			
//			for (int i = 0; i < objects.length; i++) {
//				assertEquals(objects[i], mirrors[i].base_getBase());
//			}
//
////			TODO(discuss) should NATIntrospectiveMirrors be unique? 
////			(requires a map or every object has a lazily initialised pointer)
////			for (int i = 0; i < objects.length; i++) {
////				assertEquals(mirrors[i], NATMirrorFactory._INSTANCE_.createMirror(objects[i]));
////			}
//		} catch (InterpreterException e) {
//			fail(e.getMessage());
//		}
//	}
	
	/**
	 * Tests the correctness of the up-down relation in AmbientTalk : 
	 * - down(up(o)) == o
	 */
	public void testMirrorBaseRelation() {
		evalAndReturn(
				"def emptyObject := object: { def getSuper() { super } }; \n" +
				"def objects := [ nil, true, 1, emptyObject, emptyObject.getSuper(), reflect: nil, mirror: { nil } ]; \n" +
				"def mirrors[objects.length] { nil }; \n" +
				"\n" +
				"1.to: objects.length do: { | i | \n" +
				"  mirrors[i] := reflect: objects[i]; \n" +
				"}; \n" +
				"1.to: objects.length do: { | i | \n" +
				"  (objects[i] == mirrors[i].getBase()) \n" +
				"    .ifFalse: { at.unit.fail(\"down(up(\" + objects[i] + \")) != \" + objects[i]); }; \n" +
				"  (objects[i] == mirrors[i].base) \n" +
				"    .ifFalse: { at.unit.fail(\"down(up(\" + objects[i] + \")) != \" + objects[i]); } \n" +
				"} \n");
	}	
		
//	public void testJavaMirrorInvocation() {
//		try {
//			ATMirror trueMirror = NATIntrospectiveMirror.atValue(NATBoolean._TRUE_);
//			ATMirror responds = (ATMirror)trueMirror.meta_invoke(
//					trueMirror,
//					AGSymbol.jAlloc("respondsTo"),
//					NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("ifTrue:") }));
//			responds.base_getBase().base_asBoolean().base_ifFalse_(new NativeClosure(NATNil._INSTANCE_) {
//				public ATObject base_apply(ATTable arguments) throws InterpreterException {
//					throw new XUserDefined(NATNil._INSTANCE_);
//				}
//			});
//		} catch (InterpreterException e) {
//			e.printStackTrace();
//			fail("exception: "+ e);
//		}
//	}
			
	public void testMirrorInvocation() {
		evalAndReturn(
				"def trueMirror  := at.mirrors.Factory.createMirror(true);" +
				"def responds    := trueMirror.respondsTo( symbol( \"ifTrue:\" ) );" +
				"def base        := responds.base;" +
				"base.ifFalse: { at.unit.fail(\"Incorrect Mirror Invocation\"); }");
	}
			
	public void testMirrorFieldAccess() {
		evalAndReturn(
				"def basicClosure := { raise: (object: { nil }) }; \n" +
				"def extendedMirroredClosure := \n" +
				"  object: { super := basicClosure } \n" +
				"    mirroredBy: (mirror: { nil }); \n" +
				"def intercessiveMirror := \n" +
				"  reflect: extendedMirroredClosure");
		
		evalAndTestException(
				"intercessiveMirror.dynamicParent.base.apply([]); \n",
				XUserDefined.class);

//		Can no longer set the mirror of a mirage the final 1-1 mapping is now stricly enforced
//		// Cannot assign a base-level entity to a meta-level variable
//		evalAndTestException(
//				"intercessiveMirror.mirror := \n" +
//				"  object: { \n" +
//				"    def invoke(@args) { reflect: \"ok\"}; \n" +
//				"  };\n" +
//				"extendedMirroredClosure.whatever()",
//				XTypeMismatch.class);
//		
//		// Cannot assign a base-level entity to a meta-level variable
//		evalAndReturn(
//				"intercessiveMirror.mirror := \n" +
//				"  mirror: { \n" +
//				"    def invoke(@args) { reflect: \"ok\"}; \n" +
//				"  };\n" +
//				"extendedMirroredClosure.whatever()");
	}
	
	/**
	 * This test tests the listMethods meta operations and ensures the following properties:
	 * Empty objects contain three primitive methods: namely new, init and ==. These methods
	 * can be overridden with custom behaviour which shadows the primitive implementation.
	 * Also external method definitions are closures which properly inserted into the table 
	 * of methods. Whether an object has a intercessive or introspective mirror does not matter
	 * unless the intercessive mirror intercepts the listMethods operation.
	 */
	public void testListMethods() {
		evalAndCompareTo(
				"def test := object: { nil }; \n" +
				"(reflect: test).listMethods(); \n",
				"<mirror on:[<primitive method:new>, <primitive method:init>, <primitive method:==>]>");
		evalAndCompareTo(
				"def testMirrored := object: { nil } mirroredBy: (mirror: { nil }); \n" +
				"(reflect: testMirrored).listMethods(); \n",
				"<mirror on:[<primitive method:new>, <primitive method:init>, <primitive method:==>]>");
		evalAndCompareTo(
				"test := object: { \n" +
				"  def init(); \n" +
				"}; \n" +
				"(reflect: test).listMethods(); \n",
				"<mirror on:[<primitive method:new>, <method:init>, <primitive method:==>]>");
		evalAndCompareTo(
				"testMirrored := object: { \n" +
				"  def init(); \n" +
				"} mirroredBy: (mirror: { nil });  \n" +
				"(reflect: testMirrored).listMethods(); \n",
				"<mirror on:[<primitive method:new>, <method:init>, <primitive method:==>]>");
		evalAndCompareTo(
				"def test.hello() { \"hello world\" }; \n" +
				"(reflect: test).listMethods(); \n",
				"<mirror on:[<primitive method:new>, <method:init>, <primitive method:==>, <closure:hello>]>");
		evalAndCompareTo(
				"def testMirrored.hello() { \"hello world\" }; \n" +
				"(reflect: test).listMethods(); \n",
				"<mirror on:[<primitive method:new>, <method:init>, <primitive method:==>, <closure:hello>]>");
//		evalAndCompareTo(
//				"(reflect: test).defineMethod(`hola, `(\"hola mundo, estoy aqui\")); \n",
//				"<mirror on:[<primitive method:new>, <method:init>, <primitive method:==>, <closure:hello>]>");
	}
	
	/**
	 * Following Bug report 0000001: Test whether the correct selector is returned when invoking 
	 * meta_operations on a custom mirror which themselves may throw XSelectorNotFound exceptions.
	 * @throws InterpreterException
	 */
	public void testNotFoundReporting() throws InterpreterException {
		// The meta operation select will be found, but the field x will not be found.
		try {
			evalAndThrowException(
					"def emptyMirror := mirror: { nil }; \n" +
					"def emptyObject := object: { nil }; \n" +
					"emptyObject.x;",
					XSelectorNotFound.class);
		} catch (XSelectorNotFound e) {
			assertEquals(e.selector_, AGSymbol.jAlloc("x"));
		}
		// Intentionally misspelled the name of a meta operation to test whether failures to
		// find meta operations are still properly reported.
		try {
			evalAndThrowException(
					"def emptyObject.x := 42; \n" +
					"emptyMirror.selct(emptyObject, `x);",
					XSelectorNotFound.class);
		} catch (XSelectorNotFound e) {
			assertEquals(e.selector_, AGSymbol.jAlloc("selct"));
		}
	}
}
