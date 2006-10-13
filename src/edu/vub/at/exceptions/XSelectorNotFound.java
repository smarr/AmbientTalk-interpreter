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
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * XSelectorNotFound is thrown during lookup when a particular field cannot be
 * found. It is equipped with a dedicated constructor to allow diagnosing the
 * underlying problem.
 * 
 * @author smostinc
 */
public class XSelectorNotFound extends InterpreterException {

	private static final long serialVersionUID = -9186247764999498158L;
	
	public ATSymbol selector_;
	public ATObject inObject_;
	
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
	
}
