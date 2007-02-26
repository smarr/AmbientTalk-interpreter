/**
 * AmbientTalk/2 Project
 * XIllegalSplice.java created on 1-aug-2006 at 21:20:36
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
import edu.vub.at.objects.grammar.ATExpression;

/**
 * XIllegalSplice is thrown when an @ token was not encountered in one of its legal
 * positions namely when unquoting a table or in the argument-list of an application. 
 * 
 * @author tvc
 */
public final class XIllegalSplice extends InterpreterException {

	private static final long serialVersionUID = -1363939697987417869L;

	private static final String _MESSAGE_ = "Spliced expression in illegal expression";
	
	private final ATExpression spliceExpression_;
	
	public XIllegalSplice(ATExpression spliceExp) {
		super(_MESSAGE_);
		spliceExpression_ = spliceExp;
	}
	
	public ATExpression getSpliceExpression() { return spliceExpression_; }

	public ATStripe getStripeType() {
		return NativeStripes._ILLSPLICE_;
	}
}
