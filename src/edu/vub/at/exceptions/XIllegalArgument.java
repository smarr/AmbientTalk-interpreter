/**
 * AmbientTalk/2 Project
 * XIllegalArgument.java created on 29-aug-2006 at 16:23:43
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

import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;

/**
 * An XIllegalArgument exception is thrown when primitive operations on native types
 * get passed an illegal argument value.
 * 
 * @author tvc
 */
public final class XIllegalArgument extends InterpreterException {

	private static final long serialVersionUID = -192095892552458214L;

	/**
	 * Constructor taking a message detailing the actual problem.
	 */
	public XIllegalArgument(String message) {
		super(message);
	}
	
	/**
	 * Constructor taking a descriptive message and an exception which indicates the underlying problem.
	 */
	public XIllegalArgument(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._ILLARG_;
	}

}
