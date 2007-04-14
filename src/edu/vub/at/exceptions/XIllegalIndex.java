/**
 * AmbientTalk/2 Project
 * XIllegalIndex.java created on 30-jul-2006 at 10:27:28
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
 * An XIllegalIndex exception is thrown by the evaluator whenever an invalid
 * value is used as the index into a table (e.g. when defining, assigning or referencing it).
 * This problem is detected when coercing the index to a number. 
 * 
 * @author tvc
 */
public final class XIllegalIndex extends InterpreterException {

	private static final long serialVersionUID = 2358853871410563584L;
	
	private static final String _MESSAGE_ = "Illegal index: ";

	/**
	 * Constructor taking a message describing why the coercion of the index failed.
	 */
	public XIllegalIndex(String msg) {
		super(_MESSAGE_ + msg);
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._ILLIDX_;
	}

}
