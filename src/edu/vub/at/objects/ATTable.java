/**
 * AmbientTalk/2 Project
 * ATTable.java created on 26-jul-2006 at 15:19:44
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
 * The public interface to a native AmtientTalk table (an array).
 * Extends the ATExpression interface as a Table may also be output by the parser as a literal.
 * 
 * @author tvc
 */
public interface ATTable extends ATExpression {

	// base-level interface
	
	public ATNumber base_getLength() throws InterpreterException;
	public ATObject base_at(ATNumber index) throws InterpreterException;
	public ATObject base_atPut(ATNumber index, ATObject value) throws InterpreterException;
	public ATBoolean base_isEmpty() throws InterpreterException;
	
	/**
	 * Apply a closure to each element of the table.
	 * [ tab ].each: { |v| ... }
	 * 
	 * @return nil, always
	 */
	public ATNil base_each_(ATClosure clo) throws InterpreterException;
	
	/**
	 * Map a closure over each element of the table, resulting in a new table.
	 * result := [ tab ].map: { |v| ... }
	 */
	public ATTable base_map_(ATClosure clo) throws InterpreterException;

	/**
	 * Collect all elements of the table by combining them using the given closure.
	 * The first time closure is called, the initialization element is passed as first argument.
	 * result := [ tab ].with: 0 collect: { |total, next| total + next }
	 */
	public ATObject base_with_collect_(ATObject init, ATClosure clo) throws InterpreterException;
	
	/**
	 * Keep only those elements of the table for which the closure evaluates to true.
	 * result := [ tabl ].filter: { |elt| booleanCondition(elt) }
	 */
	public ATTable base_filter_(ATClosure clo) throws InterpreterException;
	
	/**
	 * Return the index of the first element for which the given predicate returns true.
	 * Returns nil if no element satisfying the closure can be found.
	 * result := [ tabl ].find: { |elt| booleanCondition(elt) }
	 */
	public ATObject base_find_(ATClosure clo) throws InterpreterException;
	
	/**
	 * Implode the receiver table of characters into a text string
	 */
	public ATText base_implode() throws InterpreterException;
	
	/**
	 * Join all the text elements of the receiver table into a text string
	 * where the argument is used as a separator
	 */
	public ATText base_join(ATText txt) throws InterpreterException;
	
	/**
	 * Select a subrange of the table:
	 * idx: 1  2  3  4  5
	 *     [a, b, c, d, e].select(2,4) => [b, c, d]
	 */
	public ATTable base_select(ATNumber start, ATNumber stop) throws InterpreterException;
	
	/**
	 * [1,2,3] + [4,5] => [1,2,3,4,5]
	 */
	public ATTable base__oppls_(ATTable other) throws InterpreterException;
	
}
