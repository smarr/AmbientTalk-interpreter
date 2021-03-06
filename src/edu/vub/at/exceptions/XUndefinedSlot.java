/**
 * AmbientTalk/2 Project
 * XUndefinedField.java created on 11-aug-2006 at 14:49:30
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
 * An XUndefinedSlot exception is raised whenever a variable reference lookup fails,
 * or a field assignment lookup fails. It generally means a variable was accessed which
 * is not visible in the lexical scope.
 * 
 * @author tvc
 */
public final class XUndefinedSlot extends InterpreterException {

	private static final long serialVersionUID = -219804926254934101L;

	private final String fieldName_;
	
	/**
	 * Creates an XUndefinedSlot exception with a given message and slot name.
	 * @param message "method" or "field" depending on whether the lexical lookup algorithm was currently considering the method dictionary or the state vector of an object 
	 * @param fieldName the name of slot one was searching for
	 */
	public XUndefinedSlot(String message, String fieldName) {
		super("Undefined " + message + ": " + fieldName);
		fieldName_ = fieldName;
	}
	
	public String getFieldName() { return fieldName_; }

	public ATTypeTag getType() {
		return NativeTypeTags._UNDEFINEDSLOT_;
	}
}
