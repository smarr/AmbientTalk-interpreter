/**
 * AmbientTalk/2 Project
 * ATBegin.java created on 26-jul-2006 at 15:24:27
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
 * The public interface to a sequence of statements.
 * 
 * <p>
 * Example: <code>a; b; c</code>
 * </p><p>
 * This AG element is used to represent the body of a method or a literal closure.
 * <p>
 * 
 * @author tvcutsem
 */
public interface ATBegin extends ATStatement {

	/**
	 * The statements table contains zero or more statements.
	 * Example: <code>`{ x := 5; 1 + 2}.statements == `[x := 5, 1.+(2)]</code>
	 * @return A table of statements
	 */
	public ATTable base_statements();
	
}
