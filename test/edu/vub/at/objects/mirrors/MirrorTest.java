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
import edu.vub.at.exceptions.XAmbienttalk;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

public class MirrorTest extends AmbientTalkTest {
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirrorTest.class);
	}	
	
	protected void setUp() throws Exception {
		super.setUp();
		
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("Mirror"), NativeTypeTags._MIRROR_);
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("Closure"), NativeTypeTags._CLOSURE_);
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("context"), ctx_);
		
		
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
	 * Tests to see whether all symbol names defined in 'names' are present in the table of symbols resulting
	 * from the evaluation of 'toEval'.
	 */
	private void evalAndTestContainsAll(String toEval, String[] names) throws InterpreterException {
		ATTable methodNames = evalAndReturn(toEval).asTable();
		for (int i = 0; i < names.length; i++) {
			assertTrue(methodNames.base_contains(AGSymbol.jAlloc(names[i])).asNativeBoolean().javaValue);
		}
	}
	
	/**
	 * This test invokes all meta-level operations defined on objects and tests whether they 
	 * return the proper results. As all these meta-level operations should return mirrors on
	 * the 'actual' return values, this test also covers the stratification with respect to
	 * return values. A full test of stratified mirror access is provided below.
	 */
	public void testObjectMetaOperations() throws InterpreterException {
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
				"mirror.base.super;",
				Evaluator.getNil());
		
		/*evalAndCompareTo( => bad unit test: order of field names is undefined
				"mirror.listFields();",
				"[<field:super>, <field:field>]");*/
		
		evalAndTestContainsAll("mirror.listFields().map: { |field| field.name };",
		                   new String[] { "super", "field" });
		
		evalAndTestContainsAll("mirror.listMethods().map: { |meth| meth.name };",
				           new String[] { "keyworded:message:", "canonical" });
		// methodNames should equal the following table (apart from the ordering of the elements):
        // [<method:keyworded:message:>, <method:canonical>]
		
		evalAndCompareTo(
				"mirror.eval(context);",
				subject);
		evalAndCompareTo(
				"mirror.quote(context);",
				subject);
		evalAndCompareTo(
				"mirror.print();",
				"\"" + subject.toString() + "\"");
		evalAndCompareTo(
				"mirror.isRelatedTo(nil);",
				NATBoolean._TRUE_);
		evalAndCompareTo(
				"mirror.isCloneOf(object: { nil });",
				NATBoolean._FALSE_);
		evalAndCompareTo(
				"mirror.typeTags;",
				NATTable.EMPTY);
		evalAndCompareTo(
				"mirror.isTaggedAs(Mirror);",
				NATBoolean._FALSE_);

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
				"def select := metaMeta.select(meta, `select)",
				"<native closure:select>");
		
		// this test fails since a select on a mirror first tries to select with a meta prefix
		// if this fails it performs the select using the nil implementation yet this one does not
		// take the receiver into account.
		evalAndCompareTo(
				"def succeeded := select(false, `not)(); \n",
				"true");

	}
	
	/**
	 * In order to build a full reflective tower, it is necessary to be able to create and use 
	 * mirrors on mirrors as well. This test covers the creation and use of default introspective
	 * mirrors on custom intercessive mirrors
	 */
	public void testReflectingOnIntercessiveMirrors() throws InterpreterException {
		
		
		// When they are never instantiated the mirror is a direct child of the mirror root
		// implying that their base object is the default base object, changes to this object
		// can corrupt future mirrors. Therefore we explicitly instantiate the mirror which 
		// clones the ObjectMirrorRoot and gives the mirror its own base object. 
		ATObject meta = evalAndReturn(
		//		"def meta := mirror: { nil }; \n");
		 		"def meta := reflect: (object: { nil } mirroredBy: (mirror: { nil })); \n");
		assertTrue(meta.meta_isTaggedAs(NativeTypeTags._MIRROR_).asNativeBoolean().javaValue);
		evalAndCompareTo(
				"def metaMeta := reflect: meta;",
				"<mirror on:"+ meta +">");
		evalAndCompareTo(
				"def defineField := metaMeta.select(meta, `defineField)",
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
				"mirror.invoke(subject, `thisBase, []);");
		ATObject meta = evalAndReturn(
				"mirror.invoke(subject, `thisMeta, []);");
		
		assertNotSame(base, meta);
		assertEquals("<mirror on:"+ base.toString() + ">", meta.toString());
		
	}
	
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
				"  (objects[i] == mirrors[i].base) \n" +
				"    .ifFalse: { at.unit.fail(\"down(up(\" + objects[i] + \")) != \" + objects[i]); } \n" +
				"} \n");
	}
			
	public void testMirrorInvocation() {
		evalAndReturn(
				"def trueMirror  := at.mirrors.Factory.createMirror(true);" +
				"def responds    := trueMirror.respondsTo( symbol( \"ifTrue:\" ) );" +
				"responds.ifFalse: { at.unit.fail(\"Incorrect Mirror Invocation\"); }");
	}
			
	public void testMirrorFieldAccess() {
		evalAndReturn(
				"def basicClosure := { raise: (object: { def [ message, stackTrace ] := [ nil, nil ] }) }; \n" +
				"def extendedMirroredClosure := \n" +
				"  object: { super := basicClosure } \n" +
				"    taggedAs: [ Closure ] \n" +
				"    mirroredBy: (mirror: { nil }); \n"  +
				"def intercessiveMirror := \n" +
				"  reflect: extendedMirroredClosure");
		
		evalAndTestException(
				"intercessiveMirror.base.super.apply([]); \n",
				XAmbienttalk.class);

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
	public void testListMethods() throws InterpreterException {
		
		evalAndTestContainsAll(
				"def test := object: { }; \n" +
				"(reflect: test).listMethods().map: { |meth| meth.name }; \n",
				new String[] { });
		evalAndTestContainsAll(
				"def testMirrored := object: { nil } mirroredBy: (mirror: { nil }); \n" +
				"(reflect: testMirrored).listMethods().map: { |meth| meth.name }; \n",
				new String[] { });
		evalAndTestContainsAll(
				"test := object: { \n" +
				"  def init(); \n" +
				"}; \n" +
				"(reflect: test).listMethods().map: { |meth| meth.name }; \n",
				new String[] { "init" });
		evalAndTestContainsAll(
				"testMirrored := object: { \n" +
				"  def init(); \n" +
				"} mirroredBy: (mirror: { nil });  \n" +
				"(reflect: testMirrored).listMethods().map: { |meth| meth.name }; \n",
				new String[] { "init" });
		evalAndTestContainsAll(
				"def test.hello() { \"hello world\" }; \n" +
				"(reflect: test).listMethods().map: { |meth| meth.name }; \n",
				new String[] { "hello" });
		evalAndTestContainsAll(
				"def testMirrored.hello() { \"hello world\" }; \n" +
				"(reflect: test).listMethods().map: { |meth| meth.name }; \n",
				new String[] { "hello" });
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
			assertEquals(e.getSelector(), AGSymbol.jAlloc("x"));
		}
		// Intentionally misspelled the name of a meta operation to test whether failures to
		// find meta operations are still properly reported.
		try {
			evalAndThrowException(
					"def emptyObject.x := 42; \n" +
					"emptyMirror.selct(emptyObject, `x);",
					XSelectorNotFound.class);
		} catch (XSelectorNotFound e) {
			assertEquals(e.getSelector(), AGSymbol.jAlloc("selct"));
		}
	}
}
