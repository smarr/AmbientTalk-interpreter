/**
 * AmbientTalk/2 Project
 * NATFraction.java created on 26-jul-2006 at 16:42:48
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATNumeric;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;

/**
 * The native implementation of an AmbientTalk fraction.
 * A fraction is implemented by a Java double.
 * 
 * @author tvc
 */
public final class NATFraction extends NATNumeric implements ATFraction {

	public static final NATFraction INFTY = new NATFraction(Double.POSITIVE_INFINITY);
	
	public final double javaValue;
	
	/**
	 * This method currently serves as a hook for fraction creation.
	 * Currently fraction objects are not reused, but this might change in the future.
	 */
	public static final NATFraction atValue(double javaFrc) {
		return new NATFraction(javaFrc);
	}
	
	private NATFraction(double javaFrc) {
		javaValue = javaFrc;
	}
	
	public NATFraction asNativeFraction() {
		return this;
	}
    
    public ATBoolean base__opeql__opeql_(ATObject comparand) {
		return NATBoolean.atValue(this.equals(comparand));
    }
	
    public boolean equals(Object comparand) {
		return (comparand instanceof NATFraction) &&
		       (javaValue == ((NATFraction) comparand).javaValue);
    }
	
	public NATText meta_print() throws InterpreterException {
        return NATText.atValue(String.valueOf(javaValue));
	}
	
    public ATTable meta_getTypeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._FRACTION_);
    }
	
	// contract with NATNumeric
	protected double getJavaValue() { return javaValue; }
	
	/* -------------------------------------
	 * - base-level interface to fractions -
	 * ------------------------------------- */
	
	// Fraction arithmetic operations
	
	/**
	 * FRC(n).inc() => FRC(n+1)
	 */
	public ATFraction base_inc() {
		return NATFraction.atValue(javaValue+1);
	}
	
	/**
	 * FRC(n).dec() => FRC(n-1)
	 */
	public ATFraction base_dec() {
		return NATFraction.atValue(javaValue-1);
	}
	
	/**
	 * FRC(n).abs() => FRC(abs(n))
	 */
	public ATFraction base_abs() {
		return NATFraction.atValue(Math.abs(javaValue));
	}
	
	/**
	 * FRC(n).round() => NBR(round(n))
	 */
	public ATNumber base_round() {
		return NATNumber.atValue(Math.round((float) javaValue));
	}
	
	/**
	 * FRC(n).floor() => NBR(floor(n))
	 */
	public ATNumber base_floor() {
		return NATNumber.atValue(Math.round((float) Math.floor(javaValue)));
	}
	
	/**
	 * FRC(n).ceiling() => NBR(ceil(n))
	 */
	public ATNumber base_ceiling() {
		return NATNumber.atValue(Math.round((float) Math.ceil(javaValue)));
	}
	
    // Numeric arithmetic operations
	
	// addition +
	public ATNumeric base__oppls_(ATNumeric other) throws InterpreterException {
		return other.base_addFraction(this);
	}
	public ATNumeric base_addNumber(ATNumber other) throws InterpreterException {
		return NATFraction.atValue(javaValue + other.asNativeNumber().javaValue);
	}
	public ATNumeric base_addFraction(ATFraction other) throws InterpreterException {
		return NATFraction.atValue(javaValue + other.asNativeFraction().javaValue);
	}
	
	// subtraction -
	public ATNumeric base__opmns_(ATNumeric other) throws InterpreterException {
		return other.base_subtractFraction(this);
	}
	public ATNumeric base_subtractNumber(ATNumber other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeNumber().javaValue - javaValue);
	}
	public ATNumeric base_subtractFraction(ATFraction other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeFraction().javaValue - javaValue);
	}
	
	// multiplication *
	public ATNumeric base__optms_(ATNumeric other) throws InterpreterException {
		return other.base_timesFraction(this);
	}
	public ATNumeric base_timesNumber(ATNumber other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeNumber().javaValue * javaValue);
	}
	public ATNumeric base_timesFraction(ATFraction other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeFraction().javaValue * javaValue);
	}
	
	// division /
	public ATNumeric base__opdiv_(ATNumeric other) throws InterpreterException {
		return other.base_divideFraction(this);
	}
	public ATNumeric base_divideNumber(ATNumber other) throws InterpreterException {
		if (javaValue == 0)
			throw new XIllegalArgument("Division by zero: " + other);
		return NATFraction.atValue((other.asNativeNumber().javaValue * 1.0) / javaValue);
	}
	public ATNumeric base_divideFraction(ATFraction other) throws InterpreterException {
		if (javaValue == 0)
			throw new XIllegalArgument("Division by zero: " + other);
		return NATFraction.atValue(other.asNativeFraction().javaValue / javaValue);
	}
	
	// comparison: generalized equality <=>
	public ATNumeric base__opltx__opeql__opgtx_(ATNumeric other) throws InterpreterException {
		return other.base_gequalsFraction(this);
	}
	public ATNumeric base_gequalsNumber(ATNumber other) throws InterpreterException {
		int n = other.asNativeNumber().javaValue;
		if (n < javaValue) {
			return NATNumber.MONE; // -1
		} else if (n > javaValue) {
			return NATNumber.ONE;  // +1
		} else {
			return NATNumber.ZERO; // 0
		}
	}
	public ATNumeric base_gequalsFraction(ATFraction other) throws InterpreterException {
		double n = other.asNativeFraction().javaValue;
		if (n < javaValue) {
			return NATNumber.MONE; // -1
		} else if (n > javaValue) {
			return NATNumber.ONE;  // +1
		} else {
			return NATNumber.ZERO; // 0
		}
	}
}
