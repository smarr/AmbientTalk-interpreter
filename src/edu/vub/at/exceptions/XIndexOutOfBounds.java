/**
 * AmbientTalk/2 Project
 * XIndexOutOfBounds.java created on 31-jul-2006 at 16:26:14
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
 * An XIndexOutOfBounds exception is raised when an attempt is made to access or modify an 
 * AGTable with an index <= 0 or beyond its capacity.
 * 
 * @author tvc
 */
public final class XIndexOutOfBounds extends InterpreterException {

	private static final long serialVersionUID = 6549583509969990663L;

	/**
	 * Constructor signalling that an attempt is made to access or modify a table with an
	 * incorrect index (Correct indices are [1 .. size]).
	 * @param atIndex the requested index
	 * @param atLength the size of the table
	 */
	public XIndexOutOfBounds(int atIndex, int atLength) {
		super("Index out of bounds: "+ atIndex + " (size = " + atLength + ")");
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._IDXOUTOFBOUNDS_;
	}

}
