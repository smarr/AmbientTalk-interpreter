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
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUserDefined;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.NATParser;

public class MirrorTest extends AmbientTalkTest {
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirrorTest.class);
	}	
	
	protected void setUp() throws Exception {
		super.setUp();
		
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

//	/**
//	 * Following a bug report by Stijn. Intercessive mirror creation and extension do 
//	 * not always behave properly (e.g. they return mirages or superfluous introspective
//	 * mirrors wrapping the desired result)
//	 */
//	public void testMirrorCreation() {
//		ATObject mirror = evalAndReturn("def simpleMirror := mirror: { nil }; simpleMirror;");
//		
//		if(! (mirror instanceof NATIntercessiveMirror) ) {
//			fail("Return value of mirror: { ... } is not an intercessive mirror.");
//		};
//		
//		mirror = evalAndReturn("extend: simpleMirror with: { nil }");
//		
//		if(! (mirror instanceof NATIntercessiveMirror) ) {
//			fail("Extensions of a mirror: { ... } are not intercessive mirrors. " + mirror.getClass());
//		};
//	}
	
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
		
		evalAndReturn("def mirror  := at.mirrors.Factory.createMirror(true);");
		
		// Invoking base-level reflectee behaviour on a mirror. 
		// Throws XSelectorNotFound as: 
		// - the method meta_ifTrue on NATBoolean does not exist
		// - the method base_ifTrue on NATMirror does not exist
		evalAndTestException(
				"mirror.ifTrue: { nil }; \n",
				XSelectorNotFound.class);
		
		// Mirror consistency : return values are mirrors too.
		// Throws XSelectorNotFound as responds is a mirror and: 
		// - the method meta_ifTrue on NATBoolean does not exist
		// - the method base_ifTrue on NATMirror does not exist
		evalAndTestException(
				"def responds    := mirror.respondsTo( symbol(\"ifTrue:\") );" +
				"(responds.isMirror())" +
				"   .ifFalse: { at.unit.fail(\"Return value is not a mirror\") };" +
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
