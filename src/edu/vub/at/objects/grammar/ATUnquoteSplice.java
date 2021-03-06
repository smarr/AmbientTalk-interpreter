/**
 * AmbientTalk/2 Project
 * ATUnquoteSplice.java created on 26-jul-2006 at 15:13:44
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
 * The public interface to an unquote-splice AG element.
 * 
 * An unquote-splice may only appear in a quoted table or
 * argument list. The expression must evaluate to a native table.
 * Example: #@exp
 * 
 * @author tvc
 */
public interface ATUnquoteSplice extends ATExpression {

	/**
	 * The expression must evaluate to a native table
	 * Example: <code>`(`(#@(m()))).statement.expression == `(m())</code>
	 * @return the expression that must be evaluated and spliced
	 */
	public ATExpression base_expression();
	
}
