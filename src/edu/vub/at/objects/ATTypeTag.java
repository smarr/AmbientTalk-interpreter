/**
 * AmbientTalk/2 Project
 * ATTypeTag.java created on 18-feb-2007 at 15:55:09
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
 * The public interface to a native type tag object.
 * <p>
 * Type tags consist of two properties:
 * <ul>
 * <li>they have a unique name by which they can be identified across the network.
 *    In other words, the identity of a type is its name (= nominal typing).
 * <li>they have a list of supertypes: a type is then a subtype of these supertypes.
 * </ul>
 * <p>
 * Types have one important operation: one type can be tested to be a subtype of
 * another type.
 *  <p>
 * Type tags are very similar to empty Java-like interface types, and their main purpose
 * lies in the *classification* of objects. AmbientTalk Objects can be tagged with zero
 * or more type tags.
 *
 * @author tvcutsem
 */
public interface ATTypeTag extends ATObject {

	/**
	 * Returns the name of this type tag.
	 * 
	 * @return an {@link ATSymbol} representing the unique name by which the type can be identified.
	 */
	public ATSymbol base_getTypeName() throws InterpreterException;
	
	/**
	 * Returns a table with the supertypes of this type tag.
	 * 
	 * @return an {@link ATTable} with the super types of the receiver type tags.
	 */
	public ATTable base_getSuperTypes() throws InterpreterException;
	
	/**
	 * Returns true if this type tag is a subtype of a given type tag.
	 * <p>
	 * More specifically, what the native implementation (expressed in AmbientTalk syntax) does is:
	 * <p>	 
	 *	<code>
	 *  def isSubtypeOf(supertype) {
	 *		  (supertype.name() == name).or:
	 *			  { (supertypes.find: { |stype| stype.isSubtypeOf(supertype) }) != nil }
	 *	};
	 *  </code>
	 *  
	 * @param other a type.
	 * @return true if the receiver type is a subtype of the other type.
	 */
	public ATBoolean base_isSubtypeOf(ATTypeTag other) throws InterpreterException;
	
}
