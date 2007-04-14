/**
 * AmbientTalk/2 Project
 * XSelectorNotFound.java created on Jul 23, 2006 at 3:20:22 PM
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
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * XSelectorNotFound is thrown during lookup when a particular slot cannot be found. It is 
 * equipped with a dedicated constructor to allow diagnosing the underlying problem.
 * 
 * @author smostinc
 */
public class XSelectorNotFound extends InterpreterException {

	private static final long serialVersionUID = -9186247764999498158L;
	
	private final ATSymbol selector_;
	private final ATObject inObject_;
	
	/**
	 * Constructor reporting that a particular slot could not be found.
	 * @param selector the name of the slot being sought for.
	 * @param inObject the object from which the slot was being selected.
	 */
	public XSelectorNotFound(ATSymbol selector, ATObject inObject) {
		selector_ = selector;
		inObject_ = inObject;
	}
	
	public String getMessage() {
		String obj;
		try {
			obj = inObject_.meta_print().javaValue;
		} catch(InterpreterException e) {
			obj = "<unprintable object: "+e.getMessage()+">";
		}
		return "Lookup failure : selector " + selector_ + " could not be found in "+ obj;
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._SELECTORNOTFOUND_;
	}
	
	/**
	 * Rethrows the exception unless the missing selector equals the one given in this method. Rationale:
	 * Several meta_* methods actually catch this exception to recover from a failed lookup by continuing
	 * the search in an other object (e.g. parent objects, native objects, ...). When the lookup would
	 * succeed and an XSelectorNotFound exception is raised because of another failed lookup, this alternate
	 * behaviour should not be tried. Hence, when catching an XSelectorNotFound with the intention of redirecting
	 * lookup, the exception handler should invoke this method to ensure that he is dealing with the right exception.
	 */
	public void catchOnlyIfSelectorEquals(ATSymbol sym) throws InterpreterException {
		if (!selector_.base__opeql__opeql_(sym).asNativeBoolean().javaValue) {
			throw this;
		}
	}

	/**
	 * @return the selector that could not be located.
	 */
	public ATSymbol getSelector() {
		return selector_;
	}

	/**
	 * @return the object in which the selector could not be located.
	 */
	public ATObject getInObject() {
		return inObject_;
	}
	
}
