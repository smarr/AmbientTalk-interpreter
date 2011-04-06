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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATNumeric;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.DirectNativeMethod;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.util.TempFieldGenerator;

import java.util.HashMap;

/**
 * The native implementation of an AmbientTalk number.
 * A number is implemented by a Java int.
 * 
 * @author smostinc
 */
public final class NATNumber extends NATNumeric implements ATNumber {
	
	public static final NATNumber ZERO = new NATNumber(0);
	public static final NATNumber ONE = new NATNumber(1);
	public static final NATNumber MONE = new NATNumber(-1);
	
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

    public ATBoolean base__opeql__opeql_(ATObject comparand) throws XTypeMismatch {
    	if (comparand.isNativeNumber()) {
    		return NATBoolean.atValue(javaValue == comparand.asNativeNumber().javaValue);
    	} else {
    		return NATBoolean._FALSE_;
    	}
    }
	
	public ATNumber asNumber() throws XTypeMismatch { return this; }
	
	public boolean isNativeNumber() { return true; }
	
	public NATNumber asNativeNumber() { return this; }
	
	public NATText meta_print() throws InterpreterException {
        return NATText.atValue(String.valueOf(javaValue));
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue(String.valueOf(javaValue));
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._NUMBER_, NativeTypeTags._ISOLATE_);
    }
	
	// contract with NATNumeric
	protected double getJavaValue() { return javaValue; }
	
	public int hashCode() { return javaValue; }
	
	/* -----------------------------------
	 * - base-level interface to numbers -
	 * ----------------------------------- */
	
	// iteration constructs
	
	/**
	 * NBR(n).doTimes: { |i| code } => for i = 1 to n do code.eval(i) ; nil
	 */
	public ATNil base_doTimes_(ATClosure code) throws InterpreterException {
		for (int i = 1; i <= javaValue; i++) {
			code.base_apply(NATTable.atValue(new ATObject[] { NATNumber.atValue(i) }));
		}
		return Evaluator.getNil();
	}
	
	/**
	 * NBR(start).to: NBR(stop) do: { |i| code } => for i = start to stop do code.eval(i) ; nil
	 * Also works if stop > start, in which case it becomes a downTo.
	 * 
	 * If start = stop, the code is not executed.
	 */
	public ATNil base_to_do_(ATNumber end, ATClosure code) throws InterpreterException {
		return this.base_to_step_do_(end, NATNumber.ONE, code);
	}
	
	/**
	 * NBR(start).to: NBR(stop) step: NBR(inc) do: { |i| code } =>
	 *   for i = start; i < stop; i++ do code.eval(i) ; nil
	 * Also works if stop > start, in which case it becomes a downTo.
	 */
	public ATNil base_to_step_do_(ATNumber end, ATNumber inc, ATClosure code) throws InterpreterException {
		int stop = end.asNativeNumber().javaValue;
		int step = inc.asNativeNumber().javaValue;
		int start = javaValue;
		if (start > stop) {
			for (int i = start; i > stop; i -= step) {
				code.base_apply(NATTable.atValue(new ATObject[] { NATNumber.atValue(i) }));
			}
		} else {
			for (int i = start; i < stop; i+= step) {
				code.base_apply(NATTable.atValue(new ATObject[] { NATNumber.atValue(i) }));
			}
		}
		return Evaluator.getNil();
	}
	
	/**
	 * NBR(start) ** NBR(stop) => [ start, ..., stop [
	 * 
	 * Example:
	 *  2 ** 5 => [ 2, 3, 4 ]
	 *  5 ** 2 => [ 5, 4, 3 ]
	 */
	public ATTable base__optms__optms_(ATNumber end) throws InterpreterException {
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
			for (int i = 0; i < tbl.length; i++) {
				tbl[i] = NATNumber.atValue(start - i);
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
	public ATTable base__optms__optms__optms_(ATNumber end) throws InterpreterException {
		// x *** y == x ** y+1 iff x < y
		// x *** y == x ** y-1 iff y > x
		int stop = end.asNativeNumber().javaValue;
		if (javaValue <= stop)
		    return this.base__optms__optms_(end.base_inc().asNumber());
		else
			return this.base__optms__optms_(end.base_dec().asNumber());
	}

	// Number arithmetic operations
	
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
	
	public ATNumber base_ceiling() throws InterpreterException {
		return this;
	}

	public ATNumber base_floor() throws InterpreterException {
		return this;
	}

	public ATNumber base_round() throws InterpreterException {
		return this;
	}

	/**
	 * NBR(start) ?? NBR(stop) => FRC(n) where n chosen randomly in [ start, stop [
	 */
	public ATFraction base__opque__opque_(ATNumber nbr) throws InterpreterException {
		int stop = nbr.asNativeNumber().javaValue;
		double rnd = Math.random(); // 0 <= rnd < 1.0
		double frc = (rnd * (stop - javaValue)) + javaValue;
		return NATFraction.atValue(frc);
	}
	
	/**
	 * NBR(n) % NBR(r) => NBR(n % r)
	 */
	public ATNumber base__oprem_(ATNumber n) throws InterpreterException {
		return NATNumber.atValue(javaValue % n.asNativeNumber().javaValue);
	}
	
	/**
	 * NBR(n) /- NBR(d) => NBR(n / d)
	 */
	public ATNumber base__opdiv__opmns_(ATNumber n) throws InterpreterException {
		return NATNumber.atValue(javaValue / n.asNativeNumber().javaValue);
	}
	
	// Numeric arithmetic operations
	
	// addition +
	public ATNumeric base__oppls_(ATNumeric other) throws InterpreterException {
		return other.base_addNumber(this);
	}
	public ATNumeric base_addNumber(ATNumber other) throws InterpreterException {
		return NATNumber.atValue(other.asNativeNumber().javaValue + javaValue);
	}
	public ATNumeric base_addFraction(ATFraction other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeFraction().javaValue + javaValue);
	}
	
	// subtraction -
	public ATNumeric base__opmns_(ATNumeric other) throws InterpreterException {
		return other.base_subtractNumber(this);
	}
	public ATNumeric base_subtractNumber(ATNumber other) throws InterpreterException {
		return NATNumber.atValue(other.asNativeNumber().javaValue - javaValue);
	}
	public ATNumeric base_subtractFraction(ATFraction other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeFraction().javaValue - javaValue);
	}
	
	// multiplication *
	public ATNumeric base__optms_(ATNumeric other) throws InterpreterException {
		return other.base_timesNumber(this);
	}
	public ATNumeric base_timesNumber(ATNumber other) throws InterpreterException {
		return NATNumber.atValue(other.asNativeNumber().javaValue * javaValue);
	}
	public ATNumeric base_timesFraction(ATFraction other) throws InterpreterException {
		return NATFraction.atValue(other.asNativeFraction().javaValue * javaValue);
	}
	
	// division /
	public ATNumeric base__opdiv_(ATNumeric other) throws InterpreterException {
		return other.base_divideNumber(this);
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
		return other.base_gequalsNumber(this);
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
	
	/**
	 * Converts an AmbientTalk number representing a time period in milliseconds
	 * into a Java long representing the same time period in milliseconds.
	 * 
	 * @return a Java long representation of self.
	 */
	public long base_millisec() throws InterpreterException {
		return javaValue;
	}
	
	/**
	 * Converts an AmbientTalk number representing a time period in seconds
	 * into a Java long * 1000 representing a time period in milliseconds.
	 * 
	 * @return a Java long representation of self * 1000.
	 */
	public long base_seconds() throws InterpreterException {
		return javaValue * 1000;
	}
	
	/**
	 * Converts an AmbientTalk number representing a minute time period
	 * into a Java long * 1000 * 60 representing a
	 * millisecond time period.
	 * 
	 * @return a Java long representation of self * 1000 * 60.
	 */
	public long base_minutes() throws InterpreterException {
		return javaValue * 60 * 1000;
	}
	
	/**
	 * NUM(n).toText() = TXT(String.valueOf(n))
	 */
	public ATText base_toText() throws InterpreterException {
		return NATText.atValue(String.valueOf(javaValue));
	}
	
	/**
	 * This hashmap stores all native methods of native AmbientTalk numbers.
	 * It is populated when this class is loaded, and shared between all
	 * AmbientTalk actors on this VM. This is safe, since {@link DirectNativeMethod}
	 * instances are all immutable.
	 */
	private static final HashMap<String, ATMethod> _meths = new HashMap<String, ATMethod>();
	
	// initialize NATNumber methods
	static {
		_meths.put("doTimes:", new DirectNativeMethod("doTimes:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATClosure code = get(args, 1).asClosure();
				return self.base_doTimes_(code);
			}
		});
		_meths.put("to:do:", new DirectNativeMethod("to:do:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 2);
				ATNumber nbr = get(args, 1).asNumber();
				ATClosure code = get(args, 2).asClosure();
				return self.base_to_do_(nbr, code);
			}
		});
		_meths.put("to:step:do:", new DirectNativeMethod("to:step:do:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 3);
				ATNumber nbr = get(args, 1).asNumber();
				ATNumber step = get(args, 2).asNumber();
				ATClosure code = get(args, 3).asClosure();
				return self.base_to_step_do_(nbr, step, code);
			}
		});
		_meths.put("**", new DirectNativeMethod("**") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber end = get(args, 1).asNumber();
				return self.base__optms__optms_(end);
			}
		});
		_meths.put("***", new DirectNativeMethod("***") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber end = get(args, 1).asNumber();
				return self.base__optms__optms__optms_(end);
			}
		});
		_meths.put("inc", new DirectNativeMethod("inc") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_inc();
			}
		});
		_meths.put("dec", new DirectNativeMethod("dec") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_dec();
			}
		});
		_meths.put("abs", new DirectNativeMethod("abs") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_abs();
			}
		});
		_meths.put("ceiling", new DirectNativeMethod("ceiling") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_ceiling();
			}
		});
		_meths.put("floor", new DirectNativeMethod("floor") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_floor();
			}
		});
		_meths.put("round", new DirectNativeMethod("round") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_round();
			}
		});
		_meths.put("??", new DirectNativeMethod("??") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base__opque__opque_(nbr);
			}
		});
		_meths.put("%", new DirectNativeMethod("%") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base__oprem_(nbr);
			}
		});
		_meths.put("/-", new DirectNativeMethod("/-") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base__opdiv__opmns_(nbr);
			}
		});
		_meths.put("+", new DirectNativeMethod("+") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumeric nbr = get(args,1).asNativeNumeric();
				return self.base__oppls_(nbr);
			}
		});
		_meths.put("addNumber", new DirectNativeMethod("addNumber") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base_addNumber(nbr);
			}
		});
		_meths.put("addFraction", new DirectNativeMethod("addFraction") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATFraction frc = get(args,1).asNativeFraction();
				return self.base_addFraction(frc);
			}
		});
		_meths.put("-", new DirectNativeMethod("-") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumeric nbr = get(args,1).asNativeNumeric();
				return self.base__opmns_(nbr);
			}
		});
		_meths.put("subtractNumber", new DirectNativeMethod("subtractNumber") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base_subtractNumber(nbr);
			}
		});
		_meths.put("subtractFraction", new DirectNativeMethod("subtractFraction") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATFraction frc = get(args,1).asNativeFraction();
				return self.base_subtractFraction(frc);
			}
		});
		_meths.put("*", new DirectNativeMethod("*") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumeric nbr = get(args,1).asNativeNumeric();
				return self.base__optms_(nbr);
			}
		});
		_meths.put("timesNumber", new DirectNativeMethod("timesNumber") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base_timesNumber(nbr);
			}
		});
		_meths.put("timesFraction", new DirectNativeMethod("timesFraction") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATFraction frc = get(args,1).asNativeFraction();
				return self.base_timesFraction(frc);
			}
		});
		_meths.put("/", new DirectNativeMethod("/") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumeric nbr = get(args,1).asNativeNumeric();
				return self.base__opdiv_(nbr);
			}
		});
		_meths.put("divideNumber", new DirectNativeMethod("divideNumber") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base_divideNumber(nbr);
			}
		});
		_meths.put("divideFraction", new DirectNativeMethod("divideFraction") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATFraction frc = get(args,1).asNativeFraction();
				return self.base_divideFraction(frc);
			}
		});
		_meths.put("<=>", new DirectNativeMethod("<=>") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumeric nbr = get(args,1).asNativeNumeric();
				return self.base__opltx__opeql__opgtx_(nbr);
			}
		});
		_meths.put("gequalsNumber", new DirectNativeMethod("gequalsNumber") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATNumber nbr = get(args,1).asNumber();
				return self.base_gequalsNumber(nbr);
			}
		});
		_meths.put("gequalsFraction", new DirectNativeMethod("gequalsFraction") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATFraction frc = get(args,1).asNativeFraction();
				return self.base_gequalsFraction(frc);
			}
		});
		_meths.put("==", new DirectNativeMethod("==") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 1);
				ATObject comparand = get(args, 1);
				return self.base__opeql__opeql_(comparand);
			}
		});
		_meths.put("toText", new DirectNativeMethod("toText") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumber self = ctx.base_receiver().asNativeNumber();
				checkArity(args, 0);
				return self.base_toText();
			}
		});
	}
	
	/**
	 * Overrides the default AmbientTalk native object behavior of extracting native
	 * methods based on the 'base_' naming convention. Instead, native AT numbers use
	 * an explicit hashmap of native methods. This is much faster than the default
	 * behavior, which requires reflection.
	 */
	protected boolean hasLocalMethod(ATSymbol atSelector) throws InterpreterException {
		if  (_meths.containsKey(atSelector.base_text().asNativeText().javaValue)) {
			return true;
		} else {
			return super.hasLocalMethod(atSelector);
		}
	}
	
	/**
	 * @see NATNumber#hasLocalMethod(ATSymbol)
	 */
	protected ATMethod getLocalMethod(ATSymbol selector) throws InterpreterException {
		ATMethod val = _meths.get(selector.base_text().asNativeText().javaValue);
		if (val == null) {
			return super.getLocalMethod(selector);
			//throw new XSelectorNotFound(selector, this);			
		}
		return val;
	}
	
}
