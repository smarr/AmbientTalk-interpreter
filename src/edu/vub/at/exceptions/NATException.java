/**
 * AmbientTalk/2 Project
 * ATException.java created on Jul 13, 2006 at 8:24:20 PM
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
package edu.vub.at.exceptions;

import edu.vub.at.eval.InvocationStack;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;

import java.io.PrintStream;

/**
 * ATException is the superclass of all exceptions thrown by the AmbientTalk interpreter. 
 * 
 * TODO talk to Tom about maintaining a parallel stack inside the interpreter (vat)
 * and attaching this information to certain 'runtime' exceptions (e.g. those not raised
 * while parsing).
 * 
 * TODO tvcutsem Should NATException not implement the interface ATException, such
 * that all of our code can be typed by 'throws ATException' clauses instead of
 * 'throws NATException' clauses. The latter will cause problems with reflection.
 * 
 * TODO tvcutsem Shouldn't we parameterize NATExceptions with an ATContext and possibly
 * also an ATAbstractGrammar for evaluation errors. This allows the user to inspect
 * both which expression was evaluated at exception-raising-time and allows him to inspect
 * the context at exception-raising-time.
 * 
 * @author smostinc
 */
public class NATException extends Exception {

	private final InvocationStack runtimeStack_;
	
	public NATException() {
		super();
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	public NATException(String message, Throwable cause) {
		super(message, cause);
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	public NATException(String message) {
		super(message);
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	public NATException(Throwable cause) {
		super(cause);
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}
	
	public void printAmbientTalkStackTrace(PrintStream out) {
		runtimeStack_.printStackTrace(out);
	}
	
	public ATObject getAmbientTalkRepresentation() {
		return NATNil._INSTANCE_;
	}
	
	public ATObject mayBeSignalledFrom(ATClosure tryBlock, ATClosure replacementCode) 
		throws NATException {
		
		try{
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch (NATException e) {
			return replacementCode.base_apply(new NATTable(new ATObject[] { e.getAmbientTalkRepresentation() }));
		}
	}
	
//	public ATBoolean base_match(ATObject filter) {
//		return NATBoolean.atValue(
//				(filter.getClass().isAssignableFrom(this.getClass())));
//	}
		
}
