/**
 * AmbientTalk/2 Project
 * ATAssignVariable.java created on 26-jul-2006 at 14:55:05
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
 * The public interface to a variable assignment AG element.
 * 
 * <p>
 * Example: <code>x := 5</code>
 * </p><p>
 * The left-hand side must be a literal symbol available in the
 * lexical scope.
 * </p>
 *  
 *  @author tvc
 */
public interface ATAssignVariable extends ATAssignment {

	/**
	 * The name of the variable must be a literal symbol
	 * Example: <code>`{ x := 5}.statements[1].name == `x</code>
	 * @return The name of the variable
	 */
	public ATSymbol base_getName();
	
	/**
	 * The value expression may be any valid AmbientTalk expression.
	 * Example: <code>`{ x := 5}.statements[1].valueExpression == `5</code>
	 * @return The expression for the value that will be assigned to the variable
	 */
	public ATExpression base_getValueExpression();
	
}
