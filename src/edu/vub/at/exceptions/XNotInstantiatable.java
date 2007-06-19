/**
 * AmbientTalk/2 Project
 * XNotInstantiatable.java created on 16-nov-2006 at 16:12:43
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;

/**
 * Raised whenever a wrapped Java class cannot be instantiated because:
 *  - it has no publicly accessible constructors
 *  - it is abstract
 * 
 * @author tvcutsem
 */
public final class XNotInstantiatable extends InterpreterException {

	private static final long serialVersionUID = -300947695107039019L;
	
	/**
	 * Constructor signalling the class has no public constructors.
	 */
	public XNotInstantiatable(Class c) {
		super("Java class " + Evaluator.getSimpleName(c) + " has no public constructors");
	}
	
	/**
	 * Constructor signalling the class is abstract.
	 */
	public XNotInstantiatable(Class c, Exception cause) {
		super("Java class " + Evaluator.getSimpleName(c) + " is abstract", cause);
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._NOTINSTANTIATABLE_;
	}
	
}
