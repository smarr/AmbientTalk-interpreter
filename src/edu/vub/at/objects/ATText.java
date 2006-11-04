/**
 * AmbientTalk/2 Project
 * ATText.java created on 26-jul-2006 at 15:18:43
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
import edu.vub.at.objects.grammar.ATExpression;

/**
 * The public interface to a native AmbientTalk string (a string of characters).
 * Extends the ATExpression interface as a Text can also be output by the parser as a literal.
 * 
 * @author tvc
 */
public interface ATText extends ATExpression {

	// base-level interface
	
	public ATTable base_explode() throws InterpreterException;
	public ATTable base_split(ATText separator) throws InterpreterException;
	public ATNil base_find_do_(ATText regexp, ATClosure consumer) throws InterpreterException;
	public ATText base_replace_by_(ATText regexp, ATClosure transformer) throws InterpreterException;
	
	public ATText base_toUpperCase() throws InterpreterException;
	public ATText base_toLowerCase() throws InterpreterException;
	public ATNumber base_length() throws InterpreterException;
	
	public ATText base__oppls_(ATObject other) throws InterpreterException;
	public ATNumber base__opltx__opeql__opgtx_(ATText other) throws InterpreterException;
	public ATBoolean base__optil__opeql_(ATText other) throws InterpreterException;
}
