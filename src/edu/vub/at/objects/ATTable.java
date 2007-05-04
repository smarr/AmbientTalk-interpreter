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
 * ATTable is the public interface to a native AmtientTalk table (an array).
 * Extends the ATExpression interface as a Table may also be output by the parser as a literal.
 * 
 * An important distinction between ATTables and other languages such as Java is that
 * ATTable objects are indexed from [1..size] 
 * 
 * @author tvcutsem
 */
public interface ATTable extends ATExpression {

	// base-level interface
	
	/**
	 * Returns the length of the table
	 *
	 * @return the length of the table as an {@link ATNumber}.
	 */	
	public ATNumber base_getLength() throws InterpreterException;
	
	/**
	 * Returns the value of the element at the index passed as argument. 
	 *
	 * @param index a position in the table.
	 * @return ATObject representing the value of the element at the specified index.
	 * @throws XTypeMismatch if the index is not an {@link ATNumber}.
	 * @throws XIndexOutOfBounds if the index is negative or if it is greater than the length of the table.
	 */	
	public ATObject base_at(ATNumber index) throws InterpreterException;
	/**
	 * Sets the value of the element at the specified index to the new value passed as argument.
	 * 
	 * @param index a position in the table at which the given element is to be stored.
	 * @param value the element to be stored.
	 * @return value an {@link ATObject} representing the value stored in the table at the given index - i.e. the value argument.
	 * @throws XTypeMismatch if the index is not an {@link ATNumber} or if the value is not an {@link ATObject}.
	 * @throws XIndexOutOfBounds if the index is negative or if it is greater than to the length of the table
	 */	
	public ATObject base_atPut(ATNumber index, ATObject value) throws InterpreterException;
	
	/**
	 * Checks if the table is empty
	 *
	 * @return true if the size of the table is zero. Otherwise, false.
	 */	
	public ATBoolean base_isEmpty() throws InterpreterException;
	
	/**
	 * Applies a closure to each element of the table.
	 * <p>
	 * Usage example:
	 * <code>[1,2,3].each: { |i| system.println(i) }</code>
	 * 
	 * @param code a closure that takes one argument and is applied to each element of the table.
	 * @return nil
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATNil base_each_(ATClosure code) throws InterpreterException;
	
	/**
	 * Maps a closure over each element of the table, resulting in a new table.
	 * <p>
	 * Usage example:
	 * <code>[1,2,3].map: { |i| i + 1 } </code> returns <code>"[2, 3, 4]"</code> 
	 * 
	 * @param code a closure that takes one argument and is applied to each element of the table.
	 * @return nil
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATTable base_map_(ATClosure code) throws InterpreterException;

	/**
	 * Collects all elements of the table by combining them using the given closure.
	 * The first time closure is called, the given initializing element is passed as first argument.
	 * <p>
	 * Usage example:
	 * <code>result := [1,2,3].inject: 0 into: { |total, next| total + next }</code> where the value of <code>result</code> is 6.
	 *
	 * @param init an {@link ATObject} passed as first argument the first time the closure is called.
	 * @param code a closure that takes one argument and is applied to each element of the table.
	 * @return {@link ATObject} representing the value of the last applied closure.
	 * @throws XIndexOutOfBounds if the index is negative or if it is greater than the length of the table
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATObject base_inject_into_(ATObject init, ATClosure code) throws InterpreterException;
	//result := [ tabl ].filter: { |elt| booleanCondition(elt) }
	
	/**
	 * Returns a new table containing only those elements of the table for which the closure evaluates to true.
	 * <p>
	 * Usage example:
	 * <code>[1,2,3].filter: {|e| e != 2 }</code> returns <code>[1, 3]</code>
	 * 
	 * @param code a closure that takes one argument and is applied to each element of the table.
	 * @return ATTable containing those elements of the table for which the closure evaluates to true.
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATTable base_filter_(ATClosure code) throws InterpreterException;
	//result := [ tabl ].find: { |elt| booleanCondition(elt) }
	
	/**
	 * Returns the index of the first element for which the given predicate returns true.
	 * <p>
	 * Returns nil if no element satisfying the closure can be found.
	 * <p>
	 * Usage example:
	 * <code>[`a, `b, `c].find: { |e| e == `d }</code> returns <code>nil</code>
	 * 
	 * @param code a closure that takes one argument and is applied to each element of the table.
	 * @return {@link ATNumber} representing the index of the first element for which the given closure evaluates to true. Otherwise, nil. 
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATObject base_find_(ATClosure clo) throws InterpreterException;

	/**
	 * Returns true if and only if there exists an element e in the table for which
	 *  'obj == e' evaluates to true.
	 * <p>
	 * Usage example:
	 * <code>[`a, `b, `c].contains(`d)</code> returns <code>false</code>
	 * 
	 * @param obj the element to find in the table
	 * @return true if exists an element in the table for which 'obj == e' evaluates to true. Otherwise, false.
	 */
	public ATBoolean base_contains(ATObject obj) throws InterpreterException;
	
	/**
	 * Implodes the receiver table of characters into a text string.
	 * 
	 * @return the {@link ATText} resulting of the implosion of the table.
	 */
	public ATText base_implode() throws InterpreterException;
	
	/**
	 * Joins all the text elements of the receiver table into a text string where the given text is used as a separator.
	 *
	 * @param sep a text used as separator in the resulting text string.
	 * @return an {@link ATText} resulting of the join of all the text elements of the receiver using the sep text as separator.
	 *
	 */
	public ATText base_join(ATText sep) throws InterpreterException;
	
	/**
	 * Selects the subrange of the table specified by the given limits.
	 * <p>
	 * Usage example:
	 * <code>[a, b, c, d, e].select(2,4)</code> returns <code>[b, c, d]</code>
	 * 
	 * @param start a number representing the lower limit of the range.
	 * @param stop a number representing the upper limit of the range.
	 * @retun an ATTable resulting of the selection.
	 */
	public ATTable base_select(ATNumber start, ATNumber stop) throws InterpreterException;
	
	/**
	 * Concatenation infix operator. Returns the concatenation of the receiver table and the given table.
	 * <p>
	 * Usage example:
	 * <code>[1,2,3] + [4,5] </code> returns <code>[1,2,3,4,5]</code>
	 * 
	 * @param other a table to concatenate to the receiver table.
	 * @return an ATTable containing the elements of the receiver table and then the elements of the other table.
	 */
	public ATTable base__oppls_(ATTable other) throws InterpreterException;
	
}
