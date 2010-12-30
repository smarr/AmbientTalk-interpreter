/**
 * AmbientTalk/2 Project
 * ATSymbol.java created on 26-jul-2006 at 15:10:15
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGAssignmentSymbol;
import edu.vub.util.TempFieldGenerator;

/**
 * The public interface to a symbol reference AG element.
 * 
 * @author tvcutsem
 */
public interface ATSymbol extends ATExpression {

	/**
	 * Transform a symbol into a string.
	 * Example: <code>`foo.text == "foo"</code>
	 * @return the text of the symbol
	 */
	public ATText base_text() throws InterpreterException;
	
	/**
	 * Returns whether or not the receiver denotes a symbol ending in <tt>:=</tt>.
	 */
	public boolean isAssignmentSymbol() throws InterpreterException;
	
	/**
	 * Example: `(foo).asAssignmentSymbol => `foo:=
	 * @return a symbol denoting the assignment selector for the receiver field name
	 */
	public AGAssignmentSymbol asAssignmentSymbol() throws InterpreterException;
	
}
