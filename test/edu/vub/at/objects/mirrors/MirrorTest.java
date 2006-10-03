/**
 * AmbientTalk/2 Project
 * ReflectionTest.java created on Aug 11, 2006 at 11:27:03 PM
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.mirrors.NATMirrorFactory;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGMessageSend;
import edu.vub.at.objects.natives.grammar.AGMethodInvocationCreation;
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
	public void testAGMirrorInterface() {
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
			} catch (NATException e) {
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
		} catch (NATException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	};
	
	
	/**
	 * Tests the correctness of the up-down relation in Java : 
	 * - down(up(o)) == o
	 */
	public void testJavaMirrorBaseRelation() {
		ATMirror mirror = NATMirrorFactory._INSTANCE_.
			base_createMirror(True);
		assertEquals(True, mirror.base_getBase());
	}
	
	/**
	 * Tests the correctness of the up-down relation in AmbientTalk : 
	 * - down(up(o)) == o
	 */
	public void testMirrorBaseRelation() {
		try {
			evaluateInput(
					"def mirror  := at.mirrors.Factory.createMirror(true);" +
					"(true == mirror.getBase())" +
					"  .ifTrue: success ifFalse: fail;" +
					"(true == mirror.base)" +
					"  .ifTrue: success ifFalse: fail",
					new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}	
	
	public void testJavaMirrorInvocation() {
		try {
			ATObject trueMirror = NATMirrorFactory._INSTANCE_.
				base_createMirror(True);
			ATMirror responds = (ATMirror)trueMirror.meta_invoke(
					trueMirror,
					AGSymbol.alloc("respondsTo"),
					new NATTable(new ATObject[] { AGSymbol.alloc("ifTrue:") }));
			responds.base_getBase().asBoolean().base_ifTrue_ifFalse_(success, fail);
		} catch (NATException e) {
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
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
	
	public void testJavaMirrorFieldAccess() {
		try {
			AGMessageSend msgSend = new AGMessageSend(
			success,
			new AGMethodInvocationCreation(
					AGSymbol.alloc("at"), 
					new NATTable(new ATObject[] {
							closures.base_getLength()	
					})));
			ATObject msgSendMirror = NATMirrorFactory._INSTANCE_.
				base_createMirror(msgSend);
			
			ATMirror receiver = (ATMirror)msgSendMirror.meta_select(
					msgSendMirror,
					AGSymbol.alloc("receiver"));
			
			receiver.base_getBase().asClosure().base_apply(NATTable.EMPTY);
			
			msgSendMirror.meta_assignField(AGSymbol.alloc("receiver"), 
					NATMirrorFactory._INSTANCE_.base_createMirror(closures));
			
			ATMirror result = (ATMirror)msgSendMirror.meta_invoke(
					msgSendMirror,
					AGSymbol.alloc("eval"),
					new NATTable(new ATObject[] { new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_) }));
			
			result.base_getBase().asClosure().base_apply(NATTable.EMPTY);
			
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
			
	public void testMirrorFieldAccess() {
		try {
			evaluateInput(
					"def msgSendMirror  := at.mirrors.Factory.createMirror(" +
					"  `(success.at(3)));" +
					"def receiver       := msgSendMirror.receiver;" +
					"msgSendMirror.receiver := at.mirrors.Factory.createMirror(closures);",
					new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}		
	}
	
}
