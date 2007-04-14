/**
 * AmbientTalk/2 Project
 * XIllegalQuote.java created on 31-jul-2006 at 15:10:27
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
 * An XIllegalQuote exception is raised whenever an unquote-splice unquotation is discovered in an 
 * AG node where the resulting table cannot be spliced. This is detected when attempting to quote
 * an AGUnquoteSplice. An explicit check for unquote splaces is made by grammar elements who 
 * expect a table, such that this operation should never be used.
 * 
 * @author tvc
 */
public final class XIllegalQuote extends InterpreterException {

	private static final long serialVersionUID = -1407445555815270192L;

	private static final String _MESSAGE_ = "Illegal position for unquote-splice: ";

	/**
	 * Constructor taking a String representation of the incorrectly placed unquote splice grammar element.
	 * 
	 * @see edu.vub.at.objects.natives.grammar.AGUnquoteSplice#meta_quote(ATContext)
	 */
	public XIllegalQuote(String uqsExp) {
		super(_MESSAGE_ + uqsExp);
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._ILLQUOTE_;
	}
	
}
