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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.grammar.ATExpression;

/**
 * The common interface to both numbers and fractions.
 * This interface extends ATExpression as a number or fraction can also be output by the parser as a literal.
 * 
 * @author tvc
 */
public interface ATNumeric extends ATExpression {

	// base-level interface to both numbers and fractions
	
	public ATFraction base_cos();
	public ATFraction base_sin();
	public ATFraction base_tan();
	
	public ATFraction base_log();
	public ATFraction base_sqrt();
	public ATFraction base_expt(ATNumeric pow) throws NATException;	
	
	// addition +
	public ATNumeric base__oppls_(ATNumeric other) throws NATException;
	public ATNumeric base_addNumber(ATNumber other) throws NATException;
	public ATNumeric base_addFraction(ATFraction other) throws NATException;
	
	// subtraction -
	public ATNumeric base__opmns_(ATNumeric other) throws NATException;
	public ATNumeric base_subtractNumber(ATNumber other) throws NATException;
	public ATNumeric base_subtractFraction(ATFraction other) throws NATException;
	
	// multiplication *
	public ATNumeric base__optms_(ATNumeric other) throws NATException;
	public ATNumeric base_timesNumber(ATNumber other) throws NATException;
	public ATNumeric base_timesFraction(ATFraction other) throws NATException;
	
	// division /
	public ATNumeric base__opdiv_(ATNumeric other) throws NATException;
	public ATNumeric base_divideNumber(ATNumber other) throws NATException;
	public ATNumeric base_divideFraction(ATFraction other) throws NATException;
	
	// comparison: generalized equality <=>
	public ATNumeric base__opltx__opeql__opgtx_(ATNumeric other) throws NATException;
	public ATNumeric base_gequalsNumber(ATNumber other) throws NATException;
	public ATNumeric base_gequalsFraction(ATFraction other) throws NATException;
	
	// Comparable mixin based on <=>
	
	public ATBoolean base__opltx_(ATNumeric other) throws NATException; // <
	public ATBoolean base__opgtx_(ATNumeric other) throws NATException; // >
	public ATBoolean base__opltx__opeql_(ATNumeric other) throws NATException; // <=
	public ATBoolean base__opgtx__opeql_(ATNumeric other) throws NATException; // >=
	public ATBoolean base__opeql_(ATNumeric other) throws NATException; // =
	public ATBoolean base__opnot__opeql_(ATNumeric other) throws NATException; // !=
}
