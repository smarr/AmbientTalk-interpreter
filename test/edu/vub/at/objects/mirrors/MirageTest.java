/**
 * AmbientTalk/2 Project
 * MirageTest.java created on Aug 11, 2006 at 10:54:29 PM
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
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * MirageTest tests the creation of Mirages (objects with custom meta-behaviour) given
 * a NATIntercessiveMirror instance.
 * 
 * @author smostinc
 */
public class MirageTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirageTest.class);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		
		evalAndReturn("def let: clo { clo(); };");
	}
	
	
	/**
	 * This test verifies invariants with respect to the creation of mirages in AmbientTalk.
	 * First of all, it tests the relationship between the original mirror and the mirage's
	 * mirror (they should be non-identical yet cloned from each other).
	 */
	public void testMirageCreation() throws InterpreterException {
		ATObject mirrorP = evalAndReturn(
				"def mirrorP := mirror: { \n" +
				"  def iAm := \"the original\"; \n" +
				"  def init(@args) { \n" +
				"    iAm := \"a clone\"; \n" +
				"    super^init(@args); \n" +
				"  } \n" +
				"} \n");
		ATObject baseP   = evalAndReturn(
				"def baseP   := mirrorP.base;");
		
		// when creating a mirror, its base field is initialised to an empty mirage
		assertEquals(baseP.getClass(), NATMirage.class);
		evalAndCompareTo("mirrorP.listMethods().length", "0");
		evalAndCompareTo( // default objects have one primitive field super
				"mirrorP.listFields()",
				"[<field:super>]");
				
		// when creating an ex nihilo object, the init is not called
		evalAndCompareTo(
				"mirrorP.iAm",
				"\"the original\"");

		ATObject subject = evalAndReturn(
				"def subject := object: { \n" +
				"  def field := `field; \n" +
				"  def canonical() { nil }; \n" +
				"  def keyworded: arg1 message: arg2 { nil }; \n" +
				"} mirroredBy: mirrorP; \n");
		ATObject mirror  = evalAndReturn(
				"def mirror  := reflect: subject;");

		// mirror should be a clone of the mirrorP prototype
		assertNotSame(mirrorP, mirror);
		assertTrue(mirror.meta_isCloneOf(mirrorP).asNativeBoolean().javaValue);
		
		// test for identical methods by printing the table of methods (both return a new table)
		assertEquals( 
				mirrorP.meta_listMethods().toString(),
				mirror.meta_listMethods().toString());
		
		// the init method should have been called, which has set the iAm field
		evalAndCompareTo(
				"mirror.iAm",
				"\"a clone\"");
	}
	

	/**
	 * This test verifies invariants with respect to the cloning of custom mirrors in AmbientTalk.
	 * First of all, it tests the relationship between the original mirror and the clone to ensure
	 * they are proper clones of each other. Secondly, when cloning custom mirrors their base field
	 * is also cloned.
	 *  
	 * @throws InterpreterException
	 */	public void testCustomMirrorCloning() throws InterpreterException {
		ATObject subject = evalAndReturn(
				"def subject  := object: { \n" +
				"  def field  := `field; \n" +
				"  def canonical() { nil }; \n" +
				"  def keyworded: arg1 message: arg2 { nil }; \n" +
				"} mirroredBy: (mirror: { nil }); \n");
		ATObject mirror  = evalAndReturn(
				"def mirror   := reflect: subject;");
		
		// clone the mirror
		ATObject mirrorC  = evalAndReturn(
				"def mirrorC  := clone: mirror;");

		// mirror should be a clone of the mirrorP prototype
		assertNotSame(mirror, mirrorC);
		assertTrue(mirrorC.meta_isCloneOf(mirror).asNativeBoolean().javaValue);
		
		// test for identical methods by printing the table of methods (both return a new table)
		assertEquals( 
				mirror.meta_listMethods().toString(),
				mirrorC.meta_listMethods().toString());
		
		ATObject subjectC = evalAndReturn(
				"def subjectC := mirrorC.base;");
		
		// mirror should be a clone of the mirrorP prototype
		assertNotSame(subject, subjectC);
		assertTrue(subjectC.meta_isCloneOf(subject).asNativeBoolean().javaValue);
		
		// test for identical methods by printing the table of methods (both return a new table)
		assertEquals( 
				subject.meta_listMethods().toString(),
				subjectC.meta_listMethods().toString());

	}
	
	/**
	 * This test ensures that default extensions of mirrors share the base object of the parent.
	 */
	public void testChildMirrorSharesBase() throws InterpreterException {
		ATObject result = evalAndReturn(
				"def meta := mirror:  { nil }; \n" +
				"def base := object:  { nil } mirroredBy: meta; \n" +
				"    meta := reflect: base; \n" +
				"def extension := extend: meta with: { nil }; \n" +
				"(extension.base == meta.base);"); 
		assertTrue(result.asNativeBoolean().javaValue);
	}
	
	/**
	 * This test ensures that the 'base' field of a mirror is not modified
	 * when the mirror is used to create a new mirage.
	 */
	public void testMirrorBaseNotModifiedOnClone() {
		ATObject result = evalAndReturn(
				"def origMirror := mirror:  { nil };\n" +
				"def origBase := origMirror.base;\n" +
				"def newBase := object: { nil } mirroredBy: origMirror; \n" +
				"(origMirror.base) == origBase\n"); 
		assertEquals(NATBoolean._TRUE_, result);
	}
	
	public void testMirageInvocation() throws InterpreterException {
		class BufferedEcho extends NativeClosure {
			
			StringBuffer output;
			
			public BufferedEcho() {
				super(Evaluator.getNil());
				initialize();
			}
			
			public ATObject base_apply(ATTable arguments) throws InterpreterException {
				output.append(arguments.base_at(NATNumber.ONE).asNativeText().javaValue + "\n");
				return scope_;
			}
			
			public void initialize() {
				output = new StringBuffer();
			}
			
			public String readOutput() {
				return output.toString();
			}
		}
		
		BufferedEcho buffer = new BufferedEcho();
		
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("echo:"), buffer);

		
		// define a simple logging mirror which uses a shared lexical scope to logs method invocations with proper indentation
		evalAndReturn(
				"def loggingMirror := let: { | defaultSpacing := 2 | \n" +
				"	def indentLevel := 0;" +
				"	def spaces() {" +
				"		def result := \"\";" +
				"		indentLevel.doTimes: { | i |" +
				"			defaultSpacing.doTimes: { | j |" +
				"				result := result + \" \";" +
				"			}" +
				"		};" +
				"		result;" +
				"	};" +
				"" +
				"	mirror: {" +
				"		def invoke(receiver, inv) {" +
				"			echo: (spaces() + \"Invocation of method \" + inv.selector + \" with arguments \" + inv.arguments + \" on \" + receiver + \"(\" + super.base + \")\");" +
				"			indentLevel := indentLevel + 1;" +
				"			def result := super^invoke(receiver, inv);" +
				"			indentLevel := indentLevel - 1;" +
				"			echo: (spaces() + \"Invocation of method \" + inv.selector + \" yielded \" + result );" +
				"			result;" +
				"		}" +
				"	};" +
				"};");
		
		// Test setup 1: parent with logging mirror
		ATObject parent = evalAndReturn(
				"def mirroredParent := object: {" +
				"  def m() { self.n() };" +
				"  def n() { \"ok\" };" +
				"} mirroredBy: loggingMirror;");
		
		evalAndReturn("echo: mirroredParent.m();");
		
		assertEquals(
				"Invocation of method m with arguments [] on " + parent + "(" + parent + ")\n" +
				"  Invocation of method n with arguments [] on " + parent + "(" + parent + ")\n" +
				"  Invocation of method n yielded ok\n" +
				"Invocation of method m yielded ok\n" +
				"ok\n",
				buffer.readOutput());
		
		buffer.initialize();
		
		ATObject child = evalAndReturn(
				"def unmirroredChild := object: {" +
				"  super := mirroredParent; " + 
				"  def m() { " +
				"    echo: \"My parent will start logging now\";" +
				"    super^m();" + 
				"  };" + 
				"};");
		
		evalAndReturn("echo: unmirroredChild.m();");
		
		assertEquals(
				"My parent will start logging now\n" +
				"Invocation of method m with arguments [] on " + child + "(" + parent + ")\n" +
				"  Invocation of method n with arguments [] on " + child + "(" + parent + ")\n" +
				"  Invocation of method n yielded ok\n" +
				"Invocation of method m yielded ok\n" +
				"ok\n",
				buffer.readOutput());
		
		buffer.initialize();
		
//		child = evalAndReturn(
//				"def mirroredChild := object: {\n" +
//				"  super := mirroredParent;\n" +
//				"  def n() { \n" +
//				"    echo: \"    Indentation of this call should be correct as the lexical scope is shared by both mirrors\"; \n" +
//				"    super^n(); \n" +
//				"  }; \n" +
//				"} mirroredBy: loggingMirror;");
//		
//		evalAndReturn("echo: mirroredChild.m();");
//		
//		assertEquals("", buffer.readOutput());
	}
	
}
