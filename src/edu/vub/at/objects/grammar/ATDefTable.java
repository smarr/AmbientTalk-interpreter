/**
 * AmbientTalk/2 Project
 * ATDefTable.java created on 26-jul-2006 at 14:52:25
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

/**
 * The public interface to a table definition AG element.
 * 
 * Example: <code>def tab[size] { init }</code> where <code>tab</code>
 * is a literal symbol and <code>size</code> has to evaluate to a number.
 * The <code>init</code> expression is evaluated <code>size</code> times.
 * 
 * @author tvc
 */
public interface ATDefTable extends ATDefinition {

	/**
	 * The name of the table must be a literal symbol
	 * Example: <code>`{ def tab[5] { m() } }.statements[1].name == `tab</code>
	 * @return the name of the table
	 */
	public ATSymbol base_getName();
	
	/**
	 * The size may be any valid AmbientTalk expression that evaluates to a number
	 * Example: <code>`{ def tab[5] { m() } }.statements[1].sizeExpression == `5</code>
	 * @return the size expression
	 */
	public ATExpression base_getSizeExpression();
	
	/**
	 * The initializer is at least one expression and may be a sequence of expressions
	 * Example: <code>`{ def tab[5] { m() } }.statements[1].initializer == `{ m() }</code>
	 * @return the initializer
	 */
	public ATBegin base_getInitializer();
	
}
