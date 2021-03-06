/**
 * AmbientTalk/2 Project
 * XUnassignableField.java created on 14-nov-2006 at 11:54:27
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
 * An XUnassignableField exception is raised whenever a variable assignment fails because the variable/field is immutable.
 * This may happen when dealing with native AT objects or with symbiotic Java objects that have final fields.
 * 
 * @author tvcutsem
 */
public final class XUnassignableField extends InterpreterException {

	private static final long serialVersionUID = 2259235392369197805L;

	/**
	 * Creates an unassignable field exception given a particular name.
	 * @param fieldName the name of the field that was being assigned
	 */
	public XUnassignableField(String fieldName) {
		super("Unassignable field: " + fieldName);
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._UNASSIGNABLEFIELD_;
	}

}