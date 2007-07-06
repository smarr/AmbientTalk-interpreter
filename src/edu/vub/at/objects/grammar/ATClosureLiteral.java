/**
 * AmbientTalk/2 Project
 * ATClosureLiteral.java created on 26-jul-2006 at 16:57:46
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
 * The public interface to a closure literal AG element.
 * 
 * Example: <code>{ |a, b| a + b }</code> where <code>|a, b|</code>
 * represents the formal arguments list. If there are no formal arguments,
 * the vertical bars may be omitted. The body may not be empty. 
 * 
 * @author tvc
 */
public interface ATClosureLiteral extends ATExpression {
	
	/**
	 * A literal closure may contain zero, one or more formal arguments.
	 * Splicing is allowed in the argument list.
	 * Example: <code>`({ |a, b| a + b }).arguments == `[a, b]</code>
	 * @return The formal argument list
	 */
	public ATTable base_arguments();
	
	/**
	 * The body of a literal closure may not be empty.
	 * Example: <code>`({ |a, b| a + b }).bodyExpression == `(a.+(b))</code>
	 * @return The body of the literal closure
	 */
	public ATBegin base_bodyExpression();

}
