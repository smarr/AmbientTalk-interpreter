/**
 * AmbientTalk/2 Project
 * XClassNotFound.java created on 19-nov-2006 at 14:33:53
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
 * Instances of XClassNotFound are raised when the AmbientTalk interpreter's attempt to load a
 * class failed. This occurs either when using the <tt>jlobby</tt> construct to explicitly load
 * a class, but it may also occur during the deserialisation of a {@link Packet}, when the 
 * contents of that Packet contained an unknown class. 
 * 
 * @author tvcutsem
 */
public final class XClassNotFound extends InterpreterException {

	private static final long serialVersionUID = 131014866438710913L;

	/**
	 * Constructor reporting that no class could be loaded with a given name.
	 * @param qualifiedClassname The fully qualified name of the class (i.e. including the package) being loaded
	 * @param cause The underlying exception documenting why the class could not be loaded (e.g. visibility constraints)
	 */
	public XClassNotFound(String qualifiedClassname, Throwable cause) {
		super("Could not find class " + qualifiedClassname, cause);
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._CLASSNOTFOUND_;
	}
	
}
