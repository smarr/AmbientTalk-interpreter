/**
 * AmbientTalk/2 Project
 * ATFraction.java created on 26-jul-2006 at 15:17:23
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


/**
 * ATFraction is the public interface to a native AmbientTalk fraction (a floating point value).
 * 
 * @author tvc
 */
public interface ATFraction extends ATNumeric {


	//arithmetic operations
	/**
	 * Returns the fraction plus 1.
	 *
	 * @return an ATFraction resulting of adding 1 to the receiver.
	 */	
	public ATFraction base_inc() throws InterpreterException;
	
	/**
	 * Returns the fraction minus 1.
	 *
	 * @return an ATFraction resulting of subtracting 1 to the receiver.
	 */	
	public ATFraction base_dec() throws InterpreterException;
	
	/**
	 * Returns the absolute value of a fraction.
	 * <p>
	 * More specifically: 
	 * <ul>
	 * <li>If the receiver >= 0, the receiver is returned. 
	 * <li>If the receiver < 0, the negation of the receiver is returned. 
	 * </ul>
	 * 
	 * @return the absolute value of the receiver.
	 */	
	public ATFraction base_abs() throws InterpreterException;
	
	// base-level interface
	
}
