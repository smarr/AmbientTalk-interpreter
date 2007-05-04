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
 * ATField provides a mapping from an immutable name to a potentially mutable value.
 * <p>
 * Note that when field objects are added to an {@link ATObject} and then that object is cloned,
 * the field object will be re-instantiated (i.e. its 'new' method is invoked).
 * This implies that any object implementing ATField should provide a meta_newInstance
 * method whose sole initarg is the new host for the field.
 * 
 * @author smostinc
 */
public interface ATField extends ATObject {
	
	/**
	 * Returns a string by which the slot can be identified.
	 * 
	 * @return a {@link ATSymbol} representing the string by which the slot can be identified.
	 */
	public ATSymbol base_getName() throws InterpreterException;
	
	/**
	 * Returns the current value of the field.
	 * 
	 * @return an {@link ATObject} representing the current value of the field.
	 * @throws XIllegalOperation if the field accessed is not found.
	 */
	public ATObject base_readField() throws InterpreterException;
	
	/**
	 * Sets the value of the field if possible.
	 * 
	 * @param newValue the value the field should hold.
	 * @return nil.
	 * @throws InterpreterException if the field cannot be modified.
	 */
	public ATNil base_writeField(ATObject newValue) throws InterpreterException;

}
