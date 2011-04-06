/**
 * AmbientTalk/2 Project
 * ATNumeric.java created on 18-aug-2006 at 11:00:01
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
import edu.vub.at.objects.natives.NATText;

/**
 * ATNumeric is the public interface common to numbers and fractions.
 * 
 * This interface extends ATExpression as a number or fraction can also be output by the parser as a literal.
 * 
 * @author tvc
 */
public interface ATNumeric extends ATExpression {

	// base-level interface to both numbers and fractions

	/**
	 * Returns the trigonometric cosine of the numeric data type representing an angle in radians.
	 *  
	 * @return the cosine of the receiver.
	 */
	public ATFraction base_cos() throws InterpreterException;
	
	/**
	 * Returns the trigonometric sine of the numeric data type representing an angle in radians.
	 *  
	 * @return the sine of the receiver.
	 */
	public ATFraction base_sin() throws InterpreterException;
	
	/**
	 * Returns the trigonometric tangent of the numeric data type representing an angle in radians.
	 *  
	 * @return the tangent of the receiver.
	 */
	public ATFraction base_tan() throws InterpreterException;
	
	/**
	 * Returns the natural logarithm (base e) of the numeric data type.
	 *   
	 * @return the natural logarithm of the receiver or NaN if the receiver is smaller than 0.0.
	 */
	public ATFraction base_log() throws InterpreterException;
	
	/**
	 * Returns the positive square root of the numeric data type.
	 * 
	 * @return the correctly rounded positive square root of a or NaN if the receiver is smaller than zero. 
	 */
	public ATFraction base_sqrt() throws InterpreterException;
	
	/**
	 * Returns the closest number to the fraction.
	 * <p>
	 * More specifically, rounding a number is equivalent to <code> (fraction + 0.5).floor() </code>
	 *
	 * @return an ATNumber resulting of rounding the receiver to the closest number value.
	 */	
	public ATNumber base_round() throws InterpreterException;
	
	/**
	 * Returns the closest number to positive infinity that is smaller than the fraction.
	 *
	 * @return the closest number to positive infinity that is smaller than the fraction.
	 */	
	public ATNumber base_floor() throws InterpreterException;
	
	/**
	 * Returns the closest number to negative infinity that is greater than the fraction.
	 *
	 * @return the closest number to negative infinity that is greater than the fraction.
	 */	
	public ATNumber base_ceiling() throws InterpreterException;
	
	/**
	 * Returns the numeric data type raised to the power of the argument.
	 * 
	 * @param pow a ATNumeric data type representing the exponent.
	 * @return the receiver raised to the power of the argument.
	 * @throws XTypeMismatch if the exponent is not an {@link ATNumeric} object.
	 */
	public ATFraction base_expt(ATNumeric pow) throws InterpreterException;	
	
	// addition +
	/**
	 * Addition infix operator. Returns the value resulting of the sum of the receiver and 
	 * another numeric data type passed as argument.
	 * <p>
	 * More specifically, this operator actually calls:
	 * <ul>
	 * <li><code>other.addNumber(this) if the receiver is a {@link ATNumber}</code>
	 * <li><code>other.addFraction(this) if the receiver is a {@link ATFraction}</code>
	 * </ul>
	 *  
	 * @param other a numeric data type.
	 * @return a ATNumeric resulting of the sum of the receiver and other. 
	 */	
	public ATNumeric base__oppls_(ATNumeric other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of the sum of the receiver and a number passed as argument.
	 * <p>
	 * More specifically, this method returns a {@link ATNumber} if both numeric data types to sum are {@link ATNumber} objects.
	 * Otherwise, an {@link ATFraction} is returned. 
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the addition operator.
	 * 
	 * @param other a number.
	 * @return a ATNumeric resulting of the sum of the receiver and other. 
	 * @throws XTypeMismatch if other is not an {@link ATNumber} object.
	 */	
	public ATNumeric base_addNumber(ATNumber other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of the sum of the receiver and a fraction passed as argument.
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the addition operator.
	 * 
	 * @param other a fraction.
	 * @return a {@link ATFraction} resulting of the sum of the receiver and other. 
	 * @throws XTypeMismatch if other is not an {@link ATFraction} object.
	 */	
	public ATNumeric base_addFraction(ATFraction other) throws InterpreterException;
	
	// subtraction -
	/**
	 * Subtraction infix operator. Returns the value resulting of subtracting 
	 * a numeric data type passed as argument from the receiver.
	 * <p>
	 * More specifically, this operator actually calls:
	 * <ul>
	 * <li><code>other.subtractNumber(this) if the receiver is a {@link ATNumber}</code>
	 * <li><code>other.subtractFraction(this) if the receiver is a {@link ATFraction}</code>
	 * </ul>
	 * 
	 * @param other a numeric data type.
	 * @return a ATNumeric resulting of subtracting other from the receiver. 
	 */	
	public ATNumeric base__opmns_(ATNumeric other) throws InterpreterException;
	
	/** 
	 * Returns the value resulting of subtracting the receiver from a number passed as argument.
	 * <p>
	 * More specifically, this method returns a {@link ATNumber} if both numeric data types to subtract are {@link ATNumber} objects.
	 * Otherwise, an {@link ATFraction} is returned. 
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the subtraction operator.
	 * 
	 * @param other a number.
	 * @return a ATNumeric resulting of subtracting other from the receiver.
	 * @throws XTypeMismatch if other is not an {@link ATNumber} object.
	 */	
	public ATNumeric base_subtractNumber(ATNumber other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of the subtracting the receiver from a fraction passed as argument.
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the subtraction operator.
	 *  
	 * @param other a fraction.
	 * @return a {@link ATFraction} resulting of subtracting other from the receiver. 
	 * @throws XTypeMismatch if other is not an {@link ATFraction} object.
	 */	
	public ATNumeric base_subtractFraction(ATFraction other) throws InterpreterException;
	
	// multiplication *
	/**
	 * Multiplication infix operator. Returns the value resulting of multiplying the receiver by 
	 * another numeric data type passed as argument.
	 * <p>
	 * More specifically, this operator actually calls:
	 * <ul>
	 * <li><code>other.timesNumber(this) if the receiver is a {@link ATNumber}</code>
	 * <li><code>other.timesFraction(this) if the receiver is a {@link ATFraction}</code>
	 * </ul>
	 * 
	 * @param other a numeric data type.
	 * @return a ATNumeric resulting of multiplying the receiver by other. 
	 */	
	public ATNumeric base__optms_(ATNumeric other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of multiplying the receiver by a number passed as argument.
	 * <p>
	 * More specifically, this method returns a {@link ATNumber} if both numeric data types to multiply are {@link ATNumber} objects.
	 * Otherwise, an {@link ATFraction} is returned. 
	 * <p>
     * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the multiplication operator.
     * 
	 * @param other a number.
	 * @return a ATNumeric resulting of multiplying the receiver by other. 
	 * @throws XTypeMismatch if other is not an {@link ATNumber} object.
	 */	
	public ATNumeric base_timesNumber(ATNumber other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of multiplying the receiver by a fraction passed as argument.
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the multiplication operator.
	 *  
	 * @param other a fraction.
	 * @return a {@link ATFraction} resulting of multiplying the receiver by other. 
	 * @throws XTypeMismatch if other is not an {@link ATFraction} object.
	 */	
	public ATNumeric base_timesFraction(ATFraction other) throws InterpreterException;
	
	// division /
	/**
	 * Division infix operator ("/"). Returns the value resulting of dividing the receiver by 
	 * another numeric data type passed as argument.
	 * <p>
	 * More specifically, this operator actually calls:
	 * <ul>
	 * <li><code>other.divideNumber(this) if the receiver is a {@link ATNumber}</code>
	 * <li><code>other.divideFraction(this) if the receiver is a {@link ATFraction}</code>
	 * </ul>
	 * 
	 * @param other a numeric data type.
	 * @return a ATNumeric resulting of dividing the receiver by other. 
	 */	
	public ATNumeric base__opdiv_(ATNumeric other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of dividing a number passed as argument by the receiver.
	 * <p>
	 * More specifically, this method returns a {@link ATNumber} if both numeric data types to multiply are {@link ATNumber} objects.
	 * Otherwise, an {@link ATFraction} is returned. 
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the division operator.
	 * 
	 * @param other a number.
	 * @return a ATNumeric resulting of dividing a given number by the receiver. 
	 * @throws XTypeMismatch if other is not an {@link ATNumber} object.
	 * @throws XIllegalArgument if the receiver is 0.
	 */	
	public ATNumeric base_divideNumber(ATNumber other) throws InterpreterException;
	
	/**
	 * Returns the value resulting of dividing a number passed as argument by the receiver.
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the division operator.
	 * 
	 * @param other a fraction.
	 * @return a {@link ATFraction} resulting of dividing a given number by the receiver. 
	 * @throws XTypeMismatch if other is not an {@link ATFraction} object.
	 * @throws XIllegalArgument if the receiver is 0.
	 */
	public ATNumeric base_divideFraction(ATFraction other) throws InterpreterException;
	
	// comparison: generalized equality <=>
	/**
	 * Generalized equality infix operator. Returns:
	 * <ul>
	 * <li>-1 if the receiver is smaller than the numeric data type passed as argument.
	 * <li>1 if the receiver is greater than the numeric data type passed as argument.
	 * <li>0 if the receiver is equal to the numeric data type passed as argument.
	 * </ul>
	 * <p>
	 * This method actually calls:
	 * <ul>
	 * <li><code>other.gequalsNumber(this) if the receiver is a {@link ATNumber}</code>
	 * <li><code>other.gequalsFraction(this) if the receiver is a {@link ATFraction}</code>
	 * </ul>
	 * 
	 * @param other a numeric data type.
	 * @return a ATNumber resulting of evaluating the generalized equality between the receiver and other. 
	 */	
	public ATNumeric base__opltx__opeql__opgtx_(ATNumeric other) throws InterpreterException;
	
	/**
	 * Returns the value of evaluating the generalized equality between the numeric data type and a number.
	 * <p>
	 * The generalized equality returns:
	 * <ul>
	 * <li>-1 if the receiver is greater than the number passed as argument.
	 * <li>1 if the receiver is smaller than the number passed as argument.
	 * <li>0 if the receiver is equal to the number passed as argument.
	 * </ul>
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the generalized equality operator.
	 *
	 * @param other a number.
	 * @return a {@link ATNumber} resulting of applying the generalized equality between the receiver and other. 
	 * @throws XTypeMismatch if other is not an {@link ATNumber} object.
	 */	
	public ATNumeric base_gequalsNumber(ATNumber other) throws InterpreterException;
	
	/**
	 * Returns the value of evaluating the generalized equality between the numeric data type and a fraction.
	 * <p>
	 * The generalized equality returns:
	 * <ul>
	 * <li>-1 if the receiver is greater than the fraction passed as argument.
	 * <li>1 if the receiver is smaller than the fraction passed as argument.
	 * <li>0 if the receiver is equal to the fraction passed as argument.
	 * </ul>
	 * <p>
	 * Note that this is a double-dispatch method used to determine the correct runtime type of both arguments of the generalized equality operator.
	 * 
	 * @param other a number.
	 * @return a {@link ATNumber} resulting of applying the generalized equality between the receiver and other. 
	 * @throws XTypeMismatch if other is not an {@link ATFraction} object.
	 */	
	public ATNumeric base_gequalsFraction(ATFraction other) throws InterpreterException;
	
	// Comparable mixin based on <=>
	/**
	 * Returns true if the receiver is smaller than the numeric data type passed as argument.
	 *
	 * @param other a numeric data type.
	 * @return a {@link ATBoolean} resulting of evaluating <code> (receiver <=> other) == -1</code> 
	 * @throws XTypeMismatch if other is not an {@link ATNumeric} object.
	 */	
	public ATBoolean base__opltx_(ATNumeric other) throws InterpreterException; // <
	
	/**
	 * Returns true if the receiver is greater than the numeric data type passed as argument.
	 *
	 * @param other a numeric data type.
	 * @return a {@link ATBoolean} resulting of evaluating <code> (receiver <=> other) == 1</code> 
	 * @throws XTypeMismatch if other is not an {@link ATNumeric} object.
	 */	
	public ATBoolean base__opgtx_(ATNumeric other) throws InterpreterException; // >
	
	/**
	 * Returns true if the receiver is smaller than or equal to the numeric data type passed as argument.
	 *
	 * @param other a numeric data type.
	 * @return a {@link ATBoolean} resulting of evaluating <code> (receiver <=> other) != 1</code> 
	 * @throws XTypeMismatch if other is not an {@link ATNumeric} object.
	 */	
	public ATBoolean base__opltx__opeql_(ATNumeric other) throws InterpreterException; // <=
	
	/**
	 * Returns true if the receiver is greater than or equal to the numeric data type passed as argument.
	 *
	 * @param other a numeric data type.
	 * @return a {@link ATBoolean} resulting of evaluating <code> (receiver <=> other) != -1</code> 
	 * @throws XTypeMismatch if other is not an {@link ATNumeric} object.
	 */	
	public ATBoolean base__opgtx__opeql_(ATNumeric other) throws InterpreterException; // >=
	
	/**
	 * Returns true if the receiver is equal to the numeric data type passed as argument.
	 *
	 * @param other a numeric data type.
	 * @return a {@link ATBoolean} resulting of evaluating <code> (receiver <=> other) == 0</code> 
	 * @throws XTypeMismatch if other is not an {@link ATNumeric} object.
	 */	
	public ATBoolean base__opeql_(ATNumeric other) throws InterpreterException; // =
	
	/**
	 * Returns true if the receiver is different than the numeric data type passed as argument.
	 *
	 * @param other an object which compared to this one only if it is a numeric data type.
	 * @return a {@link ATBoolean} resulting of evaluating <code> (receiver <=> other) != 0</code> 
	 * @throws XTypeMismatch if other is not an {@link ATNumeric} object.
	 */	
	public ATBoolean base__opnot__opeql_(ATObject other) throws InterpreterException; // !=

	/**
	 * Converts the numeric value into a text string.
	 * @return a text (an AmbientTalk string) encoding a decimal representation of the numeric value.
	 */
	public ATText base_toText() throws InterpreterException;
	
}
