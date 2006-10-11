/**
 * AmbientTalk/2 Project
 * XArityMismatch.java created on 31-jul-2006 at 13:54:15
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

/**
 * XArityMismatch is thrown during function application when actual arguments are bound to formal parameters
 * and there are either too many or too few actual arguments supplied.
 * 
 * @author tvc
 */
public final class XArityMismatch extends NATException {

    private static final String _TOO_MANY_ = "Too many arguments supplied for ";
    private static final String _TOO_FEW_ = "Too few arguments supplied for ";
    
	/**
	 * @param funnam the name of the function to be invoked (for debugging purposes only)
	 * @param numParameters the number of parameters supplied
	 * @param numArguments the number of arguments supplied
	 */
	public XArityMismatch(String funnam, int numParameters, int numArguments) {
		super( ((numParameters < numArguments) ? _TOO_MANY_ : _TOO_FEW_)
				 + funnam + "; expected " + numParameters + ", given " + numArguments);
	}

}
