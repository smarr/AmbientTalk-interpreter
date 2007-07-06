/**
 * AmbientTalk/2 Project
 * ATMultiDefinition.java created on 18-aug-2006 at 10:12:03
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
 * The public interface to a multiple definition AG element.
 * 
 * <p>
 * Example:<code>def [x, y] := [1, 2]</code>
 * </p><p>
 * <p>
 * By using splicing in the left-hand expression, this can be
 * used to split up tables: <code>def [x, @y] := [1, 2, 3, 4]</code>
 * </p>
 *  
 *  @author tvc
 */
public interface ATMultiDefinition extends ATDefinition {
	
	/**
	 * The left-hand side of the definition must be a literal table.
	 * Slicing is allowed at the end.
	 * Example: <code>`{ def [ x, y ] := [ y, x ] }.statements[1].parameters == `[x, y]</code>
	 * @return a table with valid left-expressions
	 */
	public ATTable base_parameters();
	
	/**
	 * The right-hand side of the definition may be any valid AmbientTalk expression
	 * that evaluates to a native table
	 * Example: <code>`{ def [ x, y ] := [ y, x ] }.statements[1].valueExpression == `[y, x]</code>
	 * @return the value expression
	 */
	public ATExpression base_valueExpression();
	
}
