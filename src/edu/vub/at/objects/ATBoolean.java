/**
 * AmbientTalk/2 Project
 * ATBoolean.java created on Jul 26, 2006 at 9:53:31 PM
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

/**
 * The ATBoolean represents the public interface of a boolean object.
 * 
 * @author smostinc
 * @author tvcutsem
 */
public interface ATBoolean extends ATObject {

	/**
	 * Returns an ATObject representing the result of evaluating the code to execute
	 * if the boolean condition is true. Returns nil if the boolean expression is false.
	 * <p>
	 * Usage:
	 * <code>booleanCondition.ifTrue: { code }</code>
	 *  
	 * @param consequent a closure containing the code to execute if the boolean is true.
	 * @return the result of evaluating the closure or nil.
	 * @throws InterpreterException if raised in the evaluation of the closure. 
	 */
	public ATObject base_ifTrue_(ATClosure consequent) throws InterpreterException;
	
	/**
	 * Returns an {@link ATObject} representing the result of evaluating the code to execute
	 * if the boolean condition is false. Returns nil if the boolean expression is true.
	 * <p>
	 * Usage:
	 * <code>booleanCondition.ifFalse: { code } </code>
	 *  
	 * @param alternative a closure containing the code to execute if the boolean is false.
	 * @return the result of evaluating the closure or nil.
	 * @throws InterpreterException if raised in the evaluation of the closure. 
	 */
	public ATObject base_ifFalse_(ATClosure alternative) throws InterpreterException;
	
	/**
	 * Returns an {@link ATObject} representing the result of evaluating either the code to execute
	 * if the boolean condition is false or the one to execute if the boolean condition is true.
	 * <p>
	 * Usage:
	 * <code>booleanCondition.ifTrue: { consequent } ifFalse: { alternative } </code>
	 *  
	 * @param consequent a closure containing the code to execute if the boolean is true.
	 * @param alternative a closure containing the code to execute if the boolean is false.
	 * @return the result of the consequent or alternative closure. 
	 * @throws InterpreterException if raised in the evaluation of the consequent or alternative closure. 
	 */	
	public ATObject base_ifTrue_ifFalse_(ATClosure consequent, ATClosure alternative) throws InterpreterException;
	
	/**
	 * Returns false if the receiver is false or the result of the evaluation of the other
	 * boolean expression passed as argument if the receiver is true.
	 * <p>
	 * Usage: <code>boolean.and: { other }</code>
	 *  
	 * @param other a closure whose evaluation returns a boolean.
	 * @return false or the result of evaluating the other boolean. 
	 * @throws InterpreterException if raised in the evaluation of the other boolean. 
	 */
	public ATBoolean base_and_(ATClosure other) throws InterpreterException;
	
	/**
	 * Auxiliary function:
	 * <code>
	 * def boolean.and: b1 and: b2 {
	 *   (boolean.and: b1).and: b2
	 * }
	 * </code>
	 */
	public ATBoolean base_and_and_(ATClosure b1, ATClosure b2) throws InterpreterException;
	
	/**
	 * Returns true if the receiver is true or the result of the evaluation of the other
	 * boolean expression passed as argument if the receiver is false.
	 * <p>
	 * Usage: <code>boolean.or: { other }</code>
	 *  
	 * @param other a closure whose evaluation returns a boolean.
	 * @return true or the result of evaluating the other boolean. 
	 * @throws InterpreterException if raised in the evaluation of the other boolean. 
	 */
	public ATBoolean base_or_(ATClosure other) throws InterpreterException;
	
	/**
	 * Auxiliary function:
	 * <code>
	 * def boolean.or: b1 or: b2 {
	 *   (boolean.or: b1).or: b2
	 * }
	 * </code>
	 */
	public ATBoolean base_or_or_(ATClosure b1, ATClosure b2) throws InterpreterException;
	
	/**
	 * Returns true if the receiver is false or false if the receiver is true.
	 *  
	 * @return a boolean resulting of the negation of the receiver. 
	 */
	public ATBoolean base_not() throws InterpreterException;
	
}
