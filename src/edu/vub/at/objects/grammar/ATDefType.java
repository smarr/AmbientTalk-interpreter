/**
 * AmbientTalk/2 Project
 * ATDefType.java created on 18-feb-2007 at 14:06:09
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
package edu.vub.at.objects.grammar;

import edu.vub.at.objects.ATTable;

/**
 * The public interface to a type tag definition AG element.
 * 
 * Example: <code>deftype name</code>
 * where <code>name</code> is a valid symbol.<br>
 * A type tag can have multiple supertypes:
 * <code>deftype c <: a, b</code> where <code>a</code>
 * and <code>b</code> must evaluate to type tags
 * 
 * @author tvcutsem
 */
public interface ATDefType extends ATDefinition {
	
	/**
	 * The name of the type to be defined must be a literal symbol
	 * Example: <code>`{ deftype foo }.statements[1].typeName == `foo</code>
	 * @return the name of the type
	 */
	public ATSymbol base_typeName();
	
	/**
	 * A type can have zero, one or more parent types. 
	 * Example: <code>`{ deftype foo <: bar, o.baz() }.statements[1].parentTypeExpressions == `[ bar, o.baz() ]</code>
	 * @return a table with expressions that each must evaluate to a type
	 */
	public ATTable base_parentTypeExpressions();
	
}
