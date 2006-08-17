/**
 * AmbientTalk/2 Project
 * NATNumber.java created on 26-jul-2006 at 16:32:54
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.grammar.AGExpression;

/**
 * @author tvc
 *
 * The native implementation of an AmbientTalk number.
 * A number is implemented by a Java int.
 */
public final class NATNumber extends AGExpression implements ATNumber {
	
	public static final NATNumber ZERO = new NATNumber(0);
	public static final NATNumber ONE = new NATNumber(1);
	
	public final int javaValue;
	
	/**
	 * This method currently serves as a hook for number creation.
	 * Currently number objects are not reused, but this might change in the future.
	 */
	public static final NATNumber atValue(int javaNumber) {
		return new NATNumber(javaNumber);
	}
	
	private NATNumber(int javaNumber) {
		javaValue = javaNumber;
	}
	
	
	public boolean equals(Object other) {
		return (other instanceof NATNumber) &&
			   (javaValue == ((NATNumber) other).javaValue);
	}
	
	public ATNumber asNumber() throws XTypeMismatch { return this; }
	
	public NATNumber asNativeNumber() { return this; }
	
	public NATText meta_print() throws XTypeMismatch {
        return NATText.atValue(String.valueOf(javaValue));
	}
	
	/* -----------------------------------
	 * - base-level interface to numbers -
	 * ----------------------------------- */
	
	// trigonometric functions
	
	/**
	 * NBR(n).cos() => FRC(Math.cos(n))
	 */
	public ATFraction base_cos() {
		return NATFraction.atValue(Math.cos(javaValue));
	}
	
	/**
	 * NBR(n).sin() => FRC(Math.sin(n))
	 */
	public ATFraction base_sin() {
		return NATFraction.atValue(Math.sin(javaValue));
	}
	
	/**
	 * NBR(n).tan() => FRC(Math.tan(n))
	 */
	public ATFraction base_tan() {
		return NATFraction.atValue(Math.tan(javaValue));
	}
	
	// iteration constructs
	
	/**
	 * NBR(n).doTimes: { |i| code } => for i = 1 to n do code.eval(i) ; nil
	 */
	public ATNil base_doTimes_(ATClosure code) throws NATException {
		for (int i = 1; i < javaValue; i++) {
			code.meta_apply(new NATTable(new ATObject[] { NATNumber.atValue(i) }));
		}
		return NATNil._INSTANCE_;
	}
	
	/**
	 * NBR(start).to: NBR(stop) do: { |i| code } => for i = start to stop do code.eval(i) ; nil
	 * Also works if stop > start, in which case it becomes a downTo.
	 */
	public ATNil base_to_do_(ATNumber end, ATClosure code) throws NATException {
		return this.base_to_step_do_(end, NATNumber.ONE, code);
	}
	
	/**
	 * NBR(start).to: NBR(stop) step: NBR(inc) do: { |i| code } =>
	 *   for i = start <= stop do code.eval(i) ; nil
	 * Also works if stop > start, in which case it becomes a downTo.
	 */
	public ATNil base_to_step_do_(ATNumber end, ATNumber inc, ATClosure code) throws NATException {
		int stop = end.asNativeNumber().javaValue;
		int step = inc.asNativeNumber().javaValue;
		int start = javaValue;
		if (stop > start) {
			for (int i = stop; i >= start; i -= step) {
				code.meta_apply(new NATTable(new ATObject[] { NATNumber.atValue(i) }));
			}
		} else {
			for (int i = start; i <= stop; i+= step) {
				code.meta_apply(new NATTable(new ATObject[] { NATNumber.atValue(i) }));
			}
		}
		return NATNil._INSTANCE_;
	}
	
	/**
	 * NBR(start) ** NBR(stop) => [ start, ..., stop [
	 * 
	 * Example:
	 *  2 ** 5 => [ 2, 3, 4 ]
	 *  5 ** 2 => [ 5, 4, 3 ]
	 */
	public ATTable base__opmul__opmul_(ATNumber end) throws NATException {
		int stop = end.asNativeNumber().javaValue;
		int start = javaValue;
		if (start < stop) {
			ATObject[] tbl = new ATObject[stop - start];
			for (int i = 0; i < tbl.length; i++) {
				tbl[i] = NATNumber.atValue(start + i);
			}
			return NATTable.atValue(tbl);
		} else {
			ATObject[] tbl = new ATObject[start - stop];
			for (int i = tbl.length; i > 0; i--) {
				tbl[i] = NATNumber.atValue(stop + i);
			}
			return NATTable.atValue(tbl);
		}
	}
	
	/**
	 * NBR(start) *** NBR(stop) => [ start, ..., stop ]
	 * 
	 * Example:
	 *  2 *** 5 => [ 2, 3, 4, 5 ]
	 *  5 *** 2 => [ 5, 4, 3, 2 ]
	 */
	public ATTable base__opmul__opmul__opmul_(ATNumber end) throws NATException {
		// x *** y == x ** y+1
		int stop = end.asNativeNumber().javaValue;
		if (javaValue < stop)
		    return this.base__opmul__opmul_(end.base_inc());
		else
			return this.base__opmul__opmul_(end.base_dec());
	}

	// miscellaneous arithmetic functions
	
	/**
	 * NBR(n).inc() => NBR(n+1)
	 */
	public ATNumber base_inc() {
		return NATNumber.atValue(javaValue+1);
	}
	
	/**
	 * NBR(n).dec() => NBR(n-1)
	 */
	public ATNumber base_dec() {
		return NATNumber.atValue(javaValue-1);
	}
	
	/**
	 * NBR(n).abs() => NBR(abs(n))
	 */
	public ATNumber base_abs() {
		return NATNumber.atValue(Math.abs(javaValue));
	}
	
	/**
	 * NBR(n).log() => FRC(log(e,n))
	 */
	public ATFraction base_log() {
		return NATFraction.atValue(Math.log(javaValue));
	}
	
	/**
	 * NBR(n).sqrt() => FRC(Math.sqrt(n))
	 */
	public ATFraction base_sqrt() {
		return NATFraction.atValue(Math.sqrt(javaValue));
	}
	
	/**
	 * NBR(n).expt(NBR(e)) => FRC(Math.pow(n,e))
	 */
	public ATFraction base_expt(ATFraction frc) throws NATException {
		return NATFraction.atValue(Math.pow(javaValue, frc.asNativeFraction().javaValue));
	}
	
	/**
	 * NBR(start) ?? NBR(stop) => FRC(n) where n chosen randomly in [ start, stop [
	 */
	public ATFraction base__opque__opque_(ATNumber nbr) throws NATException {
		int stop = nbr.asNativeNumber().javaValue;
		double rnd = Math.random(); // 0 <= rnd < 1.0
		double frc = (rnd * (stop - javaValue)) + javaValue;
		return NATFraction.atValue(frc);
	}
	
}
