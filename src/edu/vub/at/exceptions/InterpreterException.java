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
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.natives.NATException;

import java.io.PrintStream;
import java.io.PrintWriter;

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

	// The ambienttalk stack trace of the exception
	protected final InvocationStack runtimeStack_;

	private final Throwable cause_;

	/**
	 * Default constructor which only captures the AmbientTalk invocation stack.
	 */
	public InterpreterException() {
		super();
		runtimeStack_ = InvocationStack.captureInvocationStack();
		cause_ = null;
	}

	/**
	 * Contructor which reports an exception with a given message and an underlying exception. 
	 * This constructor also captures the AmbientTalk invocation stack relating the exception
	 * to the AmbientTalk code that triggered it.
	 */
	public InterpreterException(String message, Throwable cause) {
		super(message);
		// Backport from JDK 1.4 to 1.3
		// super(message, cause);
		cause_ = cause;
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	/**
	 * Constructor which reports an exception with a given message which cannot be related to a
	 * previously raised exception. This constructor captures the AmbientTalk invocation 
	 * stack to identify which code produced the error.
	 */
	public InterpreterException(String message) {
		super(message);
		runtimeStack_ = InvocationStack.captureInvocationStack();
		cause_ = null;
	}

	/**
	 * Constructor which creates a wrapper for an underlying exception. This constructor 
	 * captures the AmbientTalk invocation stack to relate the exception to the AmbientTalk
	 * that it corresponds to.
	 */
	public InterpreterException(Throwable cause) {
		// Backport from JDK 1.4 to 1.3
		// super(cause);
		cause_ = cause;
		runtimeStack_ = InvocationStack.captureInvocationStack();
	}

	
	/**
	 * @param out
	 */
	public void printAmbientTalkStackTrace(PrintStream out) {
		runtimeStack_.printStackTrace(out);
	}

	/**
	 * Returns an ambienttalk representation of the exception. The returned object is a wrapper
	 * object which provides access to the exception's message and the AmbientTalk stack trace.
	 */
	public ATObject getAmbientTalkRepresentation() {
		return new NATException(this);
	}

	/**
	 * Returns the native type tag corresponding to the exception class. The NativeTypeTags class 
	 * defines types for all native data types, including exceptions. For exceptions, these 
	 * types can be used to catch native exceptions and handle them. Therefore every exception
	 * class should override this abstract method to supply the type equivalent of its class.
	 */
	public abstract ATTypeTag getType();

	public String getMessage() {
		if (cause_ == null) {
			return super.getMessage();
		} else {
			return super.getMessage() + " caused by " + cause_.getMessage();
		}
	}

	/* backport from 1.4 interface to 1.3 */
	/**
	 * As AmbientTalk targets Java 1.3, Interpreter exceptions need to provide explicit support
	 * to report the exception that caused them. The default support for such a method was only 
	 * introduced in Java 1.4. This method returns the exception that caused a particular error
	 * which can possibly be null.
	 */
	public Throwable getCause() {
		return cause_;
	}

	public void printStackTrace(PrintStream out) {
		if (cause_ == null) {
			super.printStackTrace(out);
		} else {
			super.printStackTrace(out);
			out.print(" caused by:");
			cause_.printStackTrace(out);
		}
	}

	public void printStackTrace(PrintWriter out) {
		if (cause_ == null) {
			super.printStackTrace(out);
		} else {
			super.printStackTrace(out);
			out.print(" caused by:");
			cause_.printStackTrace(out);
		}
	}

	public String toString() {
		if (cause_ == null) {
			return super.toString();
		} else {
			return super.toString() + " caused by " + cause_.toString();
		}
	}
}
