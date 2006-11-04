/**
 * AmbientTalk/2 Project
 * ATField.java created on Jul 23, 2006 at 11:52:56 AM
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
package edu.vub.at.objects;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * ATFields provide a mapping from an immutable name to a potentially mutable value.
 * 
 * @author smostinc
 */
public interface ATField extends ATObject {
	
	/**
	 * @return a string by which the slot can be identified.
	 */
	public ATSymbol base_getName() throws InterpreterException;
	
	/**
	 * @return the current value of the field.
	 * @throws InterpreterException 
	 */
	public ATObject base_getValue() throws InterpreterException;
	
	/**
	 * Sets the value of the field if possible
	 * @param newValue - the value the field should hold.
	 * @return - the value the field had before.
	 * @throws InterpreterException - if the field cannot be modified.
	 */
	public ATObject base_setValue(ATObject newValue) throws InterpreterException;

}
