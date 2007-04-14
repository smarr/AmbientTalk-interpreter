/**
 * AmbientTalk/2 Project
 * XIllegalParameter.java created on 25-dec-2006 at 17:36:39
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
 * An XIllegalParameter exception is raised when the interpreter detects a faulty
 * formal parameter list of a function or method when it is invoked. The detection
 * of such faulty parameter lists is done when the function is defined since its
 * paramter list is then partially evaluated to produce a correct argument binding 
 * function.
 * <p>
 * Illegal parameter lists can be formed when the rest-parameter (@arg) is not the
 * last parameter or when optional parameters are followed by mandatory parameters.
 *
 * @see edu.vub.at.eval.PartialBinder#calculateResidual(String, ATTable)
 * 
 * @author tvcutsem
 */
public class XIllegalParameter extends InterpreterException {

	private static final long serialVersionUID = -8300108453776535995L;
	
	/**
	 * Constructor taking the name of the function whose parameter list is being partially 
	 * evaluated and the message detailing the precise error.
	 */
	public XIllegalParameter(String ofFun, String msg) {
		super("Illegal parameter list for " + ofFun + ": "+msg);
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._ILLPARAM_;
	}

}
