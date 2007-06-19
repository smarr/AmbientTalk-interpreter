/**
 * AmbientTalk/2 Project
 * XIllegalOperation.java created on Jul 23, 2006 at 1:19:44 PM
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
 * XIllegalOperation is thrown whenever an operation is requested that is a violation
 * of the expected semantics of the interpreter. When originating from base-level code
 * these exceptions signal a fault in either the interpreter or in the custom meta
 * behaviour.
 * 
 * @author smostinc
 */
public class XIllegalOperation extends InterpreterException {

	private static final long serialVersionUID = -6486826773203553716L;

	/**
	 * Constructor which takes a descriptive message alongside an exception which caused this
	 * exception to be thrown.
	 */
	public XIllegalOperation(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor which takes a message documenting how the expected semantics of the 
	 * interpreter were being violated.
	 */
	public XIllegalOperation(String message) {
		super(message);
	}

	public ATTypeTag getType() {
		return NativeTypeTags._ILLOP_;
	}

}
