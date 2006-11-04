/**
 * AmbientTalk/2 Project
 * CoercionTest.java created on Oct 04, 2006 at 11:12:57 PM
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import junit.framework.TestCase;

public class CoercionTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(CoercionTest.class);
	}
	
	private NATObject customClosure_;
	private NATObject customContext_;
	
	/*
	 * def customContext := object: {
	 *   def self := 24
	 * }
	 * 
	 * def customClosure := object: {
	 *   def apply := { |args|
	 *     nil
	 *   }
	 *   def method := <NATMETHOD foo() { nil }>;
	 *   def context := customContext;
	 * }
	 */
	public void setUp() {
		try {
			customClosure_ = new NATObject();
			customClosure_.meta_defineField(AGSymbol.alloc("apply"), new NativeClosure(customClosure_) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					ATTable apply_args = get(args, 1).base_asTable();
					assertEquals(42, getNbr(apply_args, 1));
					return NATNil._INSTANCE_;
				}
			});
			customClosure_.meta_defineField(AGSymbol.alloc("method"), new NATMethod(AGSymbol.alloc("foo"), NATTable.EMPTY, new AGBegin(NATTable.EMPTY)));
			customContext_ = new NATObject();
			customContext_.meta_defineField(AGSymbol.alloc("self"), NATNumber.atValue(24));
			customClosure_.meta_defineField(AGSymbol.alloc("context"), customContext_);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testCoercedBaselevelInvocation() {
		try {
			ATClosure coercedObject = customClosure_.base_asClosure();
			coercedObject.base_apply(new NATTable(new ATObject[] { NATNumber.atValue(42) }));
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testCoercedMetalevelInvocation() {
		try {
			ATClosure coercedObject = customClosure_.base_asClosure();
			assertTrue(coercedObject.meta_respondsTo(AGSymbol.alloc("apply")).asNativeBoolean().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testCoercedPrimitiveInvocation() {
		try {
			ATClosure coercedObject = customClosure_.base_asClosure();
			assertEquals(customClosure_.hashCode(), coercedObject.hashCode());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testCoercedBaselevelFieldAccess() {
		try {
			ATClosure coercedObject = customClosure_.base_asClosure();
			ATMethod m = coercedObject.base_getMethod();
			assertEquals("foo", m.base_getName().base_getText().asNativeText().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}

	public void testCoercedReturnValue() {
		try {
			ATClosure coercedObject = customClosure_.base_asClosure();
			ATContext coercedContext = coercedObject.base_getContext();
			assertEquals(24, coercedContext.base_getSelf().asNativeNumber().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
}
