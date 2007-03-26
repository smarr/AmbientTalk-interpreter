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
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.natives.NATException;

import java.io.PrintStream;

/**
 * TODO tvcutsem Shouldn't we parameterize NATExceptions with an ATContext and possibly
 * also an ATAbstractGrammar for evaluation errors. This allows the user to inspect
 * both which expression was evaluated at exception-raising-time and allows him to inspect
 * the context at exception-raising-time.
 * 
 * @author smostinc
 */
public abstract class InterpreterException extends Exception {

	private static final long serialVersionUID = 511962997881825680L;

	private final InvocationStack runtimeStack_;
	
	public InterpreterException() {
		super();
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	public InterpreterException(String message, Throwable cause) {
		super(message, cause);
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	public InterpreterException(String message) {
		super(message);
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	public InterpreterException(Throwable cause) {
		super(cause);
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}
	
	public void printAmbientTalkStackTrace(PrintStream out) {
		runtimeStack_.printStackTrace(out);
	}
	
	public ATObject getAmbientTalkRepresentation() {
		return new NATException(this);
	}
	
	public abstract ATStripe getStripeType();
	
}
