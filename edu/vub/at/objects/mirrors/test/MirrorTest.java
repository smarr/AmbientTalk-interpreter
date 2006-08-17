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

package edu.vub.at.objects.mirrors.test;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.mirrors.NATMirrorFactory;
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
	
	public void testJavaMirrorBaseRelation() {
		ATMirror mirror = NATMirrorFactory._INSTANCE_.
			base_createMirror(True);
		assertEquals(True, mirror.base_getBase());
	}
	
	public void testMirrorBaseRelation() {
		evaluateInput(
				"def mirror  := at.mirrors.Factory.createMirror(true);" +
				"(true == mirror.getBase())" +
				"  .ifTrue: success ifFalse: fail;" +
				"(true == mirror.base)" +
				"  .ifTrue: success ifFalse: fail",
				new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));		
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
			
//	public void testMirrorInvocation() {
//		evaluateInput(
//				"def trueMirror  := at.mirrors.Factory.createMirror(true);" +
//				"def responds    := trueMirror.respondsTo(`(ifTrue:));" +
//				"responds.ifTrue: success ifFalse: fail",
//				new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));		
//	}
	
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
			
			receiver.base_getBase().asClosure().meta_apply(NATTable.EMPTY);
			
			msgSendMirror.meta_assignField(AGSymbol.alloc("receiver"), closures);
			
			ATMirror result = (ATMirror)msgSendMirror.meta_invoke(
					msgSendMirror,
					AGSymbol.alloc("eval"),
					new NATTable(new ATObject[] { new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_) }));
			
			result.base_getBase().asClosure().meta_apply(NATTable.EMPTY);
			
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
			
//	public void testMirrorFieldAccess() {
//		evaluateInput(
//				"def msgSendMirror  := at.mirrors.Factory.createMirror(" +
//				"  `(success.at(3)));" +
//				"def receiver       := msgSendMirror.receiver;" +
//				"msgSendMirror.receiver := closures;",
//				new NATContext(lexicalRoot, lexicalRoot, NATNil._INSTANCE_));		
//	}
	
}