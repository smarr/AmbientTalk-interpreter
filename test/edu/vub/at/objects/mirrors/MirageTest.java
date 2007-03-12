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
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
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
	 * This test ensures that extensions of mirrors have their own base object, which ensures
	 * that they are properly cloned, each child mirror clone having their own base. 
	 */
	public void testEachMirrorHasItsOwnBase() {
		ATObject result = evalAndReturn(
				"def meta := mirror:  { nil }; \n" +
				"def base := object:  { nil } mirroredBy: meta; \n" +
				"    meta := reflect: base; \n" +
				"def extension := extend: meta with: { nil }; \n" +
				"(extension.base == meta.base);"); 
		assertEquals(NATBoolean._FALSE_, result);
	}
	
	public void testMirageInvocation() throws InterpreterException {
		class BufferedEcho extends NativeClosure {
			
			StringBuffer output;
			
			public BufferedEcho() {
				super(NATNil._INSTANCE_);
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
		
		ctx_.base_getLexicalScope().meta_defineField(AGSymbol.jAlloc("echo:"), buffer);

		
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
				"		def invoke(receiver, selector, args) {" +
				"			echo: (spaces() + \"Invocation of method \" + selector + \" with arguments \" + args + \" on \" + receiver + \"(\" + base + \")\");" +
				"			indentLevel := indentLevel + 1;" +
				"			def result := super^invoke(receiver, selector, args);" +
				"			indentLevel := indentLevel - 1;" +
				"			echo: (spaces() + \"Invocation of method \" + selector + \" yielded \" + result.print().base );" +
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
				"  Invocation of method n yielded \"ok\"\n" +
				"Invocation of method m yielded \"ok\"\n" +
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
				"  Invocation of method n yielded \"ok\"\n" +
				"Invocation of method m yielded \"ok\"\n" +
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
