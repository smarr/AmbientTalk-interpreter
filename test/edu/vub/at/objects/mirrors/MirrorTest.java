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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.natives.grammar.AGSymbol;

public class MirrorTest extends ReflectiveAccessTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirrorTest.class);
	}	
	
	/**
	 * This test goes over all abstract grammar elements and tests their accessors
	 * for fields. If all works well, the test transforms one abstract syntax tree
	 * into another one. The former does not care for stratified access whereas the
	 * latter one does. 
	 */
	public void notestAGMirrorInterface() {
		System.out.println(" `( def [ mirror, statified ] := [ ]  })");
	}
	
	/**
	 * This test creates a mirror and attempts to use it in a non-stratified way.
	 * The test assumes failure if these attempts succeed, and continues if they
	 * result in the proper exception. The following tests are performed :
	 * 
	 * - Invoking base-level reflectee behaviour on a mirror
	 * - Return values of meta_operations are mirrors
	 * - Field selection from a mirror results in a mirror
	 * - Field assignment on a mirror with a non-mirror value
	 *
	 */
	public void testStratification() { 
		
		try {
			// Test setup : create a new scope and define a mirror inside it.
			NATCallframe testScope = new NATCallframe(lexicalRoot);
			
			try {
				evaluateInput(
						"def mirror  := at.mirrors.Factory.createMirror(true);",
						new NATContext(testScope, lexicalRoot, NATNil._INSTANCE_));
			} catch (InterpreterException e) {
				fail("exception : could not create a mirror : " + e);
			}
			
			// Invoking base-level reflectee behaviour on a mirror.
			try {
				evaluateInput(
						"mirror.ifTrue: fail;" +
						"fail()",
						new NATContext(testScope, lexicalRoot, NATNil._INSTANCE_));
			} catch (XSelectorNotFound e) {
				// the method meta_ifTrue on NATBoolean does not exist
				// the method base_ifTrue on NATMirror does not exist
				// success
			}

			// Mirror consistency : return values are mirrors too.
			try {
				evaluateInput(
						"def responds    := mirror.respondsTo( symbol(\"ifTrue:\") );" +
						"(responds.isMirror())" +
						"   .ifTrue: success ifFalse: fail;" +
						"responds.ifTrue: fail ifFalse: fail",
						new NATContext(testScope, lexicalRoot, NATNil._INSTANCE_));
			} catch (XSelectorNotFound e) {
				// meta_ifTrue_ifFalse_ is not a method of NATBoolean
				// base_ifTrue_ifFalse_ is not a method of NATMirror
				// success
			}
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
		
	};
	
	
	/**
	 * Tests the correctness of the up-down relation in Java : 
	 * - down(up(o)) == o
	 */
	public void testJavaMirrorBaseRelation() {
		ATMirror mirror = NATMirrorFactory._INSTANCE_.createMirror(True);
		try {
			assertEquals(True, mirror.base_getBase());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests the correctness of the up-down relation in AmbientTalk : 
	 * - down(up(o)) == o
	 */
	public void testMirrorBaseRelation() {
		try {
			evaluateInput(
					"def mirror  := at.mirrors.Factory.createMirror(true);" +
					// "echo: (\"testMirrorBaseRelation mirror is \".+(mirror));" +
					"(true == mirror.getBase())" +
					"  .ifTrue: success ifFalse: fail;" +
					"(true == mirror.base)" +
					"  .ifTrue: success ifFalse: fail",
					new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}	
	
	public void testJavaMirrorInvocation() {
		try {
			ATMirror trueMirror = NATMirrorFactory._INSTANCE_.createMirror(True);
			ATMirror responds = (ATMirror)trueMirror.meta_invoke(
					trueMirror,
					AGSymbol.alloc("respondsTo"),
					new NATTable(new ATObject[] { AGSymbol.alloc("ifTrue:") }));
			responds.base_getBase().base_asBoolean().base_ifTrue_ifFalse_(success, fail);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
			
	public void testMirrorInvocation() {
		try {
			evaluateInput(
					"def trueMirror  := at.mirrors.Factory.createMirror(true);" +
					"def responds    := trueMirror.respondsTo( symIfTrue );" +
					"def base        := responds.base;" +
					"base.ifTrue: success ifFalse: fail",
					new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
	
	public void testJavaMirrorFieldAccess() {
		try {
			ATMethod emptyExtension  = 
				new NativeAnonymousMethod(MirrorTest.class) {
					public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
						return NATNil._INSTANCE_;
					};
				};
			
			ATMethod invokeSuccess = 
				new NativeAnonymousMethod(MirrorTest.class) {
					public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
						evaluateInput(
								"def invoke(@args) { \"ok\" };",
								ctx);
						return NATNil._INSTANCE_;
					};
				};
			
			ATObject extendedSuccess =
				OBJLexicalRoot._INSTANCE_.base_extend_with_mirroredBy_(
					success,
					new NativeClosure(success, emptyExtension),
					(NATIntercessiveMirror)OBJMirrorRoot._INSTANCE_.meta_extend(
							new NativeClosure(success, emptyExtension)));
			
			
			ATMirror extendedSuccessMirror = NATMirrorFactory._INSTANCE_.createMirror(extendedSuccess);
			
			ATMirror receiver = (ATMirror)extendedSuccessMirror.meta_select(
					extendedSuccessMirror,
					AGSymbol.alloc("dynamicParent"));
			
			receiver.base_getBase().base_asClosure().base_apply(NATTable.EMPTY);
			
			extendedSuccessMirror.meta_assignField(
					extendedSuccessMirror,
					AGSymbol.alloc("mirror"), 
					extendedSuccessMirror.meta_extend(
							new NativeClosure(extendedSuccessMirror, invokeSuccess)));
			
			extendedSuccess.meta_invoke(
					extendedSuccess,
					AGSymbol.alloc("whatever"),
					NATTable.EMPTY);
			
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
			
	public void testMirrorFieldAccess() {
		try {
			evaluateInput(
					"def extendedSuccess := \n" +
					"  extend: success with: { nil } \n" +
					"  mirroredBy: (mirror: { nil }); \n" +
					"def extendedSuccessMirror := \n" +
					"  reflect: extendedSuccess; \n" +
					"\n" +
					"extendedSuccessMirror.dynamicParent.base.apply([]); \n" +
					"extendedSuccessMirror.mirror :=  \n" +
					"  extend: extendedSuccessMirror with: { \n" +
					"    def invoke(@args) { reflect: \"ok\"}; \n" +
					"  }; \n" +
					" \n" +
					"echo: extendedSuccess.whatever()",
					new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
	
	/* following a bug report by tom */
	public void testListMethods() {
		try {
			evaluateInput(
					"def test := object: { \n" +
					"  def hello() { echo: \"hello\"; self }; \n" +
					"  def world() { echo: \"world\"; self }; \n" +
					"}; \n" +
					"echo: (reflect: test).listMethods(); \n" +
					"def testMirrored := object: { \n" +
					"  def hello() { echo: \"hello\"; self }; \n" +
					"  def world() { echo: \"world\"; self }; \n" +
					"} mirroredBy: (mirror: { nil }); \n" +
					"echo: (reflect: testMirrored).listMethods();",
					new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}	
}
