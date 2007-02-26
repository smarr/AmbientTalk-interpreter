/**
 * AmbientTalk/2 Project
 * XJavaException.java created on 3-nov-2006 at 11:43:15
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Member;

/**
 * An XJavaException AmbientTalk native exception wraps a Java exception.
 * 
 * @author tvc
 */
public final class XJavaException extends InterpreterException {

	private static final long serialVersionUID = 328306238980169679L;

	private final Throwable wrappedJavaException_;
	private final transient Object originatingObject_;
	private final transient Member originatingMethod_; // method or constructor in which exception originated
	
	public XJavaException(Object jObj, Member jMeth, Throwable exc) {
		wrappedJavaException_ = exc;
		originatingObject_ = jObj;
		originatingMethod_ = jMeth;
	}
	
	public XJavaException(Throwable exc) {
		wrappedJavaException_ = exc;
		originatingObject_ = null;
		originatingMethod_ = null;
	}
	
	// throwable interface implemented through composition with wrappedJavaException_

	public Throwable getCause() {
		return wrappedJavaException_.getCause();
	}

	public String getLocalizedMessage() {
		return wrappedJavaException_.getLocalizedMessage();
	}
	
	public Throwable getWrappedJavaException() {
		return wrappedJavaException_;
	}

	public String getMessage() {
		if (originatingObject_ != null) {
			return "Java exception from " + originatingObject_.toString() + "."
			           + originatingMethod_.getName() + ": " + wrappedJavaException_.getMessage();
		} else if (originatingMethod_ != null) {
			return "Java exception from constructor " + originatingMethod_.getName()
			   + ": " + wrappedJavaException_.getMessage();
		} else {
			return wrappedJavaException_.getMessage();
		}
	}

	public StackTraceElement[] getStackTrace() {
		return wrappedJavaException_.getStackTrace();
	}

	public synchronized Throwable initCause(Throwable arg) {
		return wrappedJavaException_.initCause(arg);
	}

	public void printStackTrace() {
		wrappedJavaException_.printStackTrace();
	}

	public void printStackTrace(PrintStream arg) {
		wrappedJavaException_.printStackTrace(arg);
	}

	public void printStackTrace(PrintWriter arg) {
		wrappedJavaException_.printStackTrace(arg);
	}

	public void setStackTrace(StackTraceElement[] arg) {
		wrappedJavaException_.setStackTrace(arg);
	}

	public String toString() {
		return wrappedJavaException_.toString();
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._JAVAEXCEPTION_;
	}
	
}
