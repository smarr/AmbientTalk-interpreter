/**
 * AmbientTalk/2 Project
 * ATDefMethod.java created on 26-jul-2006 at 14:50:35
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
 * The public interface to a method definition AG element.
 * 
 * Example: <code>def m(a, b) { a + b }</code>
 * 
 * @author tvcutsem
 */
public interface ATDefMethod extends ATDefinition {

	/**
	 * The selector must be a literal symbol.
	 * Example: <code>`{ def m() { 5 } }.statements[1].selector == `m</code>
	 * @return the selector of the method
	 */
	public ATSymbol base_selector();
	
	/**
	 * A method may have zero, one or more formal arguments.
	 * Slicing is allowed in the argument list.
	 * Example: <code>`{ def m(a, @b) { b } }.statements[1].arguments == `[a, @b]</code>
	 * @return the formal argument list
	 */
	public ATTable base_arguments();
	
	/**
	 * The body of a method may not be empty.
	 * Example: <code>`{ def m(a, b) { a.n(); b+1 } }.statements[1].bodyExpression == `{ a.n(); b.+(1) }</code>
	 * @return The body of the method
	 */
	public ATBegin base_bodyExpression();
	
	/**
	 * A method may have zero or more annotations.
	 * Example; <code>`{def m() @[Getter] { x }}.statements[1].annotations == `[Getter]</code>
	 * @return The annotations of the method
	 */
	public ATExpression base_annotationExpression();
}
