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

import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;

/**
 * Instances of the XUserDefined act as wrappers for ATObjects which are thrown at the
 * ambienttalk level using the raise: primitive. Since we reuse the Java exception 
 * propagation, we need to wrap these in a custom class which extends the proper class.
 *
 * @author smostinc
 */
public class XUserDefined extends InterpreterException {

	private static final long serialVersionUID = -2859841280138142649L;

	public final ATObject customException_;
	
	public XUserDefined(ATObject customException) {
		super("A custom exception object was thrown");
		customException_ = customException;
	}

	public ATObject getAmbientTalkRepresentation() {
		return customException_;
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._CUSTOMEXCEPTION_;
	}
}