/**
 * AmbientTalk/2 Project
 * ATNumber.java created on 26-jul-2006 at 15:15:59
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
 * ATNumber is the public interface to an AmbientTalk native number (an integer value).
 * 
 * @author tvc
 */
public interface ATNumber extends ATNumeric {

	// base-level interface
	
	//	Arithmetic operations
	/**
	 * Returns the number plus 1.
	 *
	 * @return an ATNumber resulting of adding 1 to the receiver.
	 */	
	public ATNumber base_inc() throws InterpreterException;
	
	/**
	 * Returns the number minus 1.
	 *
	 * @return an ATNumber resulting of subtracting 1 to the receiver.
	 */	
	public ATNumber base_dec() throws InterpreterException;
	
	/**
	 * Returns the absolute value of a number.
	 * <p>
	 * More specifically: 
	 * <ul>
	 * <li>If the receiver >= 0, the receiver is returned. 
	 * <li>If the receiver < 0, the negation of the receiver is returned. 
	 * </ul>
	 *  
	 * @return the absolute value of the receiver.
	 */	
	public ATNumber base_abs() throws InterpreterException;
	
	/**
	 * Iterates as many times as the value of the number applying a closure passed as argument. 
	 * <p>
	 * More specifically, the equivalent pseudo-code for this construct is:
	 * <code>for i = 1 to receiver do code.eval(i); nil </code>
	 * <p>
	 * Here comes an example about how the doTimes: construct can be used to calculate the factorial of a number.
	 * def fact(n) { 
	 *   def res := 1; 
	 *   n.doTimes: { |i| res := res * i}; 
	 *   res
	 * }
	 * 
	 * @param code a closure expected to take one argument to be applied in each iteration
	 * @return nil
	 * @throws InterpreterException if raised inside the iterator block.
	 */	
	public ATNil base_doTimes_(ATClosure code) throws InterpreterException;
	
	/**
	 * Iterates from the value of the receiver to the one passed as argument applying a closure. 
	 * <p>
	 * More specifically, this method calls:
	 * <code>receiver.to: end step: 1 do: { |i| code } </code> 
	 * <p>
	 * Note that if end is bigger than the receiver, the construct behaves as a down-to operation. 
	 * If receiver is equals to end, the code is not executed.
	 * 
	 * @param end a number representing the stop value of the loop.
	 * @param code a closure expected to take one argument to be applied in each iteration.
	 * @return nil
	 * @throws InterpreterException if raised inside the iterator block.
	 */	
	public ATNil base_to_do_(ATNumber end, ATClosure code) throws InterpreterException;
	
	/**
	 * Iterates from the value of the receiver to the one passed as argument applying a closure. 
	 * <p>
	 * More specifically, the equivalent pseudo-code for this method is:
	 * <p>
	 * <code>for (i = receiver; i < end ; i := i + inc) do code.eval(i); nil</code> 
	 * <p>
	 * Note that if end is bigger than the receiver, the construct behaves as a down-to operation. 
	 * If receiver is equals to end, the code is not executed.
	 *  
	 * @param end a number representing the stop value of the loop.
	 * @param inc a number representing the step value of the loop.
	 * @param code a closure expected to take one argument to be applied in each iteration.
	 * @return nil
	 * @throws InterpreterException if raised inside the iterator block.
	 */	
	public ATNil base_to_step_do_(ATNumber end, ATNumber inc, ATClosure code) throws InterpreterException;
	
	/**
	 * Returns a table containing the exclusive range from the number to a number passed as argument.
	 * <p>
	 * Usage example:
	 * <p>
	 * <code> 2 ** 5 => [ 2, 3, 4 ]</code> 
	 * <code> 5 ** 2 => [ 5, 4, 3 ]</code> 
	 *  
	 * @param end a number representing the stop value of the range.
	 * @return a {@link ATTable} representing [ receiver, ..., end [
	 */
	public ATTable base__optms__optms_(ATNumber end) throws InterpreterException;
	
	/**
	 * Returns a table containing the inclusive range from the number to a number passed as argument.
	 * <p>
	 * Usage example:
	 * <p>
	 * <code> 2 *** 5 => [ 2, 3, 4, 5 ]</code> 
	 * <code> 5 *** 2 => [ 5, 4, 3, 2 ]</code> 
	 *  
	 * @param end a number representing the stop value of the range.
	 * @return a {@link ATTable} representing [ receiver, ..., end ]
	 */
	public ATTable base__optms__optms__optms_(ATNumber end) throws InterpreterException;
	
	/**
	 * Returns a random fraction in the exclusive range from the number to a number passed as argument.
	 * 
	 * @param nbr a number representing the stop value of the range.
	 * @return the {@link ATFraction} chosen randomly in [ receiver, ..., nbr [
	 */
	public ATFraction base__opque__opque_(ATNumber nbr) throws InterpreterException;
	
	/**
	 * Returns the modular arithmetic between the number and another number passed as argument.
	 * 
	 * @param nbr a number.
	 * @return an {@link ATNumber} resulting of the remainder of the division (of receiver by nbr) that truncates towards zero.
	 */
	public ATNumber base__oprem_(ATNumber nbr) throws InterpreterException;
	
	/**
	 * Returns the floor division between the number and another number passed as argument.
	 * 
	 * @param nbr a number.
	 * @return a {@link ATNumber} resulting of the floor division of receiver by nbr.
	 */
	public ATNumber base__opdiv__opmns_(ATNumber nbr) throws InterpreterException;
	
	/**
	 * Converts an AmbientTalk number representing a time period in milliseconds
	 * into a Java long representing the same time period in milliseconds.
	 * 
	 * @return a Java long representation of self.
	 */
	public long base_millisec() throws InterpreterException;
	
	/**
	 * Converts an AmbientTalk number representing a time period in seconds
	 * into a Java long * 1000 representing a time period in milliseconds.
	 * 
	 * @return a Java long representation of self * 1000.
	 */
	public long base_seconds() throws InterpreterException;
	
	/**
	 * Converts an AmbientTalk number representing a minute time period
	 * into a Java long * 1000 * 60 representing a
	 * millisecond time period.
	 * 
	 * @return a Java long representation of self * 1000 * 60.
	 */
	public long base_minutes() throws InterpreterException;
	
}
