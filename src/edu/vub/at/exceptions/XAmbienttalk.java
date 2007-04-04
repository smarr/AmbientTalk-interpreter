/**
 * AmbientTalk/2 Project
 * XUserDefined.java created on Oct 10, 2006 at 9:31:40 PM
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

import java.io.PrintWriter;
import java.io.StringWriter;

import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The XAmbienttalk exception class is used to wrap non-primitive exceptions in the interpreter.
 * These exceptions are objects striped with the Exception stripe which should contain a message
 * field, which this class will read out, as well as an assignable field stackTrace which this
 * class will initialise with the correct ambienttalk stack trace.
 *
 * @author smostinc
 */
public class XAmbienttalk extends InterpreterException {

	private static final ATSymbol _STACKTRACE_SYM_ = AGSymbol.jAlloc("stackTrace");
	private static final ATSymbol _MESSAGE_SYM_ = AGSymbol.jAlloc("message");
	
	private static final long serialVersionUID = -2859841280138142649L;

	public final ATObject customException_;
	
	public XAmbienttalk(ATObject customException) throws InterpreterException {
		customException_ = customException;
		
		StringWriter buffer = new StringWriter();
		
		runtimeStack_.printStackTrace(new PrintWriter(buffer, /* autoflush = */ true));
		customException_.meta_assignField(customException_, _STACKTRACE_SYM_, NATText.atValue(buffer.toString()));
	}

	public ATObject getAmbientTalkRepresentation() {
		return customException_;
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._CUSTOMEXCEPTION_;
	}
	
	public String getMessage() {
		try {
			return customException_.meta_select(customException_, _MESSAGE_SYM_).asNativeText().javaValue;
		} catch (InterpreterException e) {
			return "A custom exception was thrown";
		}
	}
}
