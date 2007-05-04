/**
 * AmbientTalk/2 Project
 * ATStripe.java created on 18-feb-2007 at 15:55:09
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
 * The public interface to a native stripe object.
 * <p>
 * Stripes consist of two properties:
 * <ul>
 * <li>they have a unique name by which they can be identified across the network.
 *    In other words, the identity of a stripe is its name.
 * <li>they have a list of parent stripes: a stripe is then a 'substripe' of these parents.
 * </ul>
 * <p>
 * Stripes have one important operation: one stripe can be tested to be a substripe of
 * another stripe.
 *  <p>
 * Stripes are very similar to empty Java-like interface types, and their main purpose
 * lies in the *classification* of objects. AmbientTalk Objects can be striped with zero
 * or more stripes.
 *
 * @author tvcutsem
 */
public interface ATStripe extends ATObject {

	/**
	 * Returns the name of this stripe.
	 * 
	 * @return an {@link ATSymbol} representing the unique name by which the stripe can be identified.
	 */
	public ATSymbol base_getStripeName() throws InterpreterException;
	
	/**
	 * Returns a table with the stripes of the parent of this stripe.
	 * 
	 * @return an {@link ATTable} with the stripes of the parent of the receiver stripe.
	 */
	public ATTable base_getParentStripes() throws InterpreterException;
	
	/**
	 * Returns true if this stripe is a substripe of a given stripe.
	 * <p>
	 * More specifically, what the native implementation (expressed in AmbientTalk syntax) does is:
	 * <p>	 
	 *	<code>
	 *  def isSubstripeOf(superstripe) {
	 *		  (superstripe.name() == name).or:
	 *			  { (superstripes.find: { |sstripe| sstripe.isSubstripeOf(superstripe) }) != nil }
	 *	};
	 *  </code>
	 *  
	 * @param other a stripe.
	 * @return true if the receiver stripe is a substripe of the other stripe.
	 */
	public ATBoolean base_isSubstripeOf(ATStripe other) throws InterpreterException;
	
}
