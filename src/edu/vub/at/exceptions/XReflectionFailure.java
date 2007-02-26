/**
 * AmbientTalk/2 Project
 * XReflectionFailure.java created on Jul 13, 2006 at 8:32:03 PM
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

import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;

/**
 * An instance of the class XReflectionFailure is thrown when something goes wrong
 * when dealing with mirror or mirage operations. By extension this also covers 
 * failures when symbiotically accessing Java objects.
 * 
 * @author smostinc
 */
public class XReflectionFailure extends InterpreterException {

	private static final long serialVersionUID = 4082945147643295718L;

	public XReflectionFailure() {
		super();
	}

	public XReflectionFailure(String message, Throwable cause) {
		super(message, cause);
	}

	public XReflectionFailure(String message) {
		super(message);
	}

	public XReflectionFailure(Throwable cause) {
		super(cause);
	}
	
	public String getMessage() {
		if (getCause() != null) {
			return super.getMessage() + ": " + getCause().getMessage();
		} else {
		    return super.getMessage();
		}
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._REFLECTIONFAILURE_;
	}

}
