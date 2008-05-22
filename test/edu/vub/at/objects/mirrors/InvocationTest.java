/**
 * AmbientTalk/2 Project
 * InvocationTest.java created on Jul 31, 2006 at 11:12:57 PM
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATMethodInvocation;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGAssignmentSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author smostinc
 * 
 * InvocationTest tests the various reflective machinery provided in the mirrors
 * package such as to see whether they work accurately. Three ATTypes are used in the
 * course of this tests, namely ATBoolean (which provides easy cases to test),
 * ATTable (which is used amongst others to test field access), and ATMessage (which
 * has fields that can be set at the base level).
 */
public class InvocationTest extends ReflectiveAccessTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(InvocationTest.class);
	}

	/**
	 * Tests the invocation of methods on natively implemented objects which thus
	 * adhere to the expected interfaces. This test checks the semantics of the
	 * classes under test such that faults can be traced back to either the 
	 * NAT-objects or the reflective infrastructure.
	 */
	public void testJavaBaseMethodInvocation() {
		try {
			True.base_ifTrue_(success);
			True.base_ifFalse_(fail);
			True.base_ifTrue_ifFalse_(success, fail);
			
			False.base_ifTrue_(fail);
			False.base_ifFalse_(success);
			False.base_ifTrue_ifFalse_(fail, success);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
	}
	
	/**
	 * Simulates invocation from the base-level by manually calling meta_invoke.
	 * This test determines in case of failures whether they are due to a fault 
	 * in the meta_invoke implementation of NATNil or in the semantics of method
	 * invocation as a result of meta_eval on an AG-component.
	 */
	public void testSimulatedBaseInvocation() {
		try {
			True.meta_invoke(
					True, AGSymbol.jAlloc("ifTrue:"),
					NATTable.atValue(new ATObject[] { success }));
			True.meta_invoke(
					True, AGSymbol.jAlloc("ifFalse:"),
					NATTable.atValue(new ATObject[] { fail }));
			True.meta_invoke(
					True, AGSymbol.jAlloc("ifTrue:ifFalse:"),
					NATTable.atValue(new ATObject[] { success, fail }));

			False.meta_invoke(
					False, AGSymbol.jAlloc("ifTrue:"),
					NATTable.atValue(new ATObject[] { fail }));
			False.meta_invoke(
					False, AGSymbol.jAlloc("ifFalse:"),
					NATTable.atValue(new ATObject[] { success }));
			False.meta_invoke(
					False, AGSymbol.jAlloc("ifTrue:ifFalse:"),
					NATTable.atValue(new ATObject[] { fail, success }));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
	}
	
	/**
	 * This test initialises a lexical root with the values success, fail, true and
	 * false. Then it invokes methods from base-level ambienttalk. This test 
	 * concludes the first installment of test which test the plain invocation of
	 * base-level methods on native types in AmbientTalk.
	 */
	public void testBaseInvocation() {
		try {
			evaluateInput(
					"true.ifTrue: &success;" +
					"true.ifFalse: &fail;" +
					"true.ifTrue: &success ifFalse: &fail;" +
					"false.ifTrue: &fail;" +
					"false.ifFalse: &success;" +
					"false.ifTrue: &fail ifFalse: &success",
					new NATContext(lexicalRoot, lexicalRoot));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}

	/**
	 * Tests the accessing of fields on a natively implemented table. This test 
	 * calls the methods from java and thus will fail only when the corresponding
	 * implementation is corrupted.
	 */
	public void testJavaBaseFieldAccess() {
		try {
			ATObject element = closures.base_at(closures.base_length());
			element.asClosure().base_apply(NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
		
	}

	/**
	 * Tests the accessing of fields on a natively implemented table. If this test
	 * succeeds and the next test fails, the fault is due to the implementation of
	 * the AG-objects for tabulation and or application.
	 */
	public void testSimulatedBaseFieldAccess() {
		try {
			ATClosure accessor = closures.meta_select(closures, AGSymbol.jAlloc("at"));
			ATObject element = accessor.base_apply(
					NATTable.atValue(new ATObject[] {
							closures.meta_invoke(closures, AGSymbol.jAlloc("length"), NATTable.EMPTY)}));
			element.asClosure().base_apply(NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
	}
	
	/**
	 * Tests the accessing of fields on a natively implemented table. This test
	 * uses ambienttalk code for the evaluation, so 
	 *
	 */
	public void testBaseFieldAccess() {
		try {
			evaluateInput(
					"def accessor := closures.&at;" +
					"def expanded := accessor(closures.length);" +
					"def coated := closures[closures.length];" +
					"expanded();" +
					"coated()",
					new NATContext(lexicalRoot, lexicalRoot));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
		
		try {
			evaluateInput(
					"closures.at(closures.length)();" +
					"closures[closures.length]()",
					new NATContext(lexicalRoot, lexicalRoot));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
		
	}

	/**
	 * Tests the assignment of fields on a natively implemented message send parse
	 * tree element. This test calls the methods from java and thus will fail only 
	 * when the corresponding implementation is corrupted.
	 */
	public void testJavaBaseFieldAssignment() {
		try {
			ATMessage message = new NATMethodInvocation(
					AGSymbol.jAlloc("at"), 
					NATTable.EMPTY,
					NATTable.EMPTY);
			
			message.base_arguments__opeql_(NATTable.of(closures.base_length()));

			ATObject element = message.base_sendTo(closures, Evaluator.getNil());
			
			element.asClosure().base_apply(NATTable.EMPTY);

		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
		
	}

	/**
	 * Tests the assignment of fields on a natively implemented message send parse
	 * tree element. If this test succeeds and the next test fails, the fault is 
	 * due to the implementation of the AG-objects for assignment, quotation and or 
	 * method invocation.
	 */
	public void testSimulatedBaseFieldAssignment() {
		try {
			ATMessage message = new NATMethodInvocation(
					AGSymbol.jAlloc("at"), 
					NATTable.EMPTY,
					NATTable.EMPTY);
			
			// message.arguments := [3]
			message.impl_call(
					AGAssignmentSymbol.jAlloc("arguments:="), 
					NATTable.of(NATTable.of(closures.base_length())));	

			ATObject element = message.base_sendTo(closures, Evaluator.getNil());
			
			element.asClosure().base_apply(NATTable.EMPTY);
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
	}
	
	/**
	 * Tests the accessing of fields on a natively implemented table. This test
	 * uses ambienttalk code for the evaluation, so 
	 *
	 */
	public void testBaseFieldAssignment() {
		try {
			evaluateInput(
					"def message       := .at();" +
					"message.arguments := [closures.length];" +
					"def result        := closures <+ message;" +
					"result()",
					new NATContext(lexicalRoot, lexicalRoot));
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("exception: "+ e);
		}
	}
}
