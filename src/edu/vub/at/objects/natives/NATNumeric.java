/**
 * AmbientTalk/2 Project
 * NATNumeric.java created on 18-aug-2006 at 11:06:11
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
package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.ATNumeric;
import edu.vub.at.objects.natives.grammar.AGExpression;

/**
 * A common superclass of both numbers and fractions to factor out common base-level behaviour.
 * 
 * @author tvc
 */
public abstract class NATNumeric extends AGExpression implements ATNumeric {

	/**
	 * Template method that should return the value of the underlying number or fraction as a double
	 */
	protected abstract double getJavaValue();
	
	public NATNumeric asNativeNumeric() throws XTypeMismatch {
		return this;
	}
	
	// trigonometric functions
	
	/**
	 * NUM(n).cos() => FRC(Math.cos(n))
	 */
	public ATFraction base_cos() {
		return NATFraction.atValue(Math.cos(getJavaValue()));
	}
	
	/**
	 * NUM(n).sin() => FRC(Math.sin(n))
	 */
	public ATFraction base_sin() {
		return NATFraction.atValue(Math.sin(getJavaValue()));
	}
	
	/**
	 * NUM(n).tan() => FRC(Math.tan(n))
	 */
	public ATFraction base_tan() {
		return NATFraction.atValue(Math.tan(getJavaValue()));
	}

	/**
	 * NUM(n).log() => FRC(log(e,n))
	 */
	public ATFraction base_log() {
		return NATFraction.atValue(Math.log(getJavaValue()));
	}
	
	/**
	 * NUM(n).sqrt() => FRC(Math.sqrt(n))
	 */
	public ATFraction base_sqrt() {
		return NATFraction.atValue(Math.sqrt(getJavaValue()));
	}
	
	/**
	 * NUM(n).expt(NUM(e)) => FRC(Math.pow(n,e))
	 */
	public ATFraction base_expt(ATNumeric pow) throws NATException {
		return NATFraction.atValue(Math.pow(getJavaValue(), pow.asNativeNumeric().getJavaValue()));
	}
	
	// Comparable 'mixin' based on <=>
	
	/**
	 * a < b iff (a <=> b) == -1
	 */
	public ATBoolean base__opltx_(ATNumeric other) throws NATException {
		return NATBoolean.atValue(this.base__opltx__opeql__opgtx_(other).equals(NATNumber.MONE));
	}
	/**
	 * a > b iff (a <=> b) == +1
	 */
	public ATBoolean base__opgtx_(ATNumeric other) throws NATException {
		return NATBoolean.atValue(this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ONE));
	}
	/**
	 * a <= b iff (a <=> b) != +1
	 */
	public ATBoolean base__opltx__opeql_(ATNumeric other) throws NATException {
		return NATBoolean.atValue(! this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ONE));
	}
	/**
	 * a >= b iff (a <=> b) != -1
	 */
	public ATBoolean base__opgtx__opeql_(ATNumeric other) throws NATException {
		return NATBoolean.atValue(! this.base__opltx__opeql__opgtx_(other).equals(NATNumber.MONE));
	}
	/**
	 * a = b iff (a <=> b) == 0
	 */
	public ATBoolean base__opeql_(ATNumeric other) throws NATException {
		return NATBoolean.atValue(this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ZERO));
	}
	/**
	 * a != b iff (a <=> b) != 0
	 */
	public ATBoolean base__opnot__opeql_(ATNumeric other) throws NATException {
		return NATBoolean.atValue(! this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ZERO));
	}

}
