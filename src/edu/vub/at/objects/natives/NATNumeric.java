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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNumeric;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.DirectNativeMethod;
import edu.vub.at.objects.mirrors.JavaInterfaceAdaptor;
import edu.vub.at.objects.natives.grammar.AGExpression;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

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
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._NUMERIC_);
    }
    
    // numbers and fractions are singletons
	public ATObject meta_clone() throws InterpreterException {
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
	public ATFraction base_expt(ATNumeric pow) throws InterpreterException {
		return NATFraction.atValue(Math.pow(getJavaValue(), pow.asNativeNumeric().getJavaValue()));
	}
	
	// Comparable 'mixin' based on <=>
	
	/**
	 * a < b iff (a <=> b) == -1
	 */
	public ATBoolean base__opltx_(ATNumeric other) throws InterpreterException {
		return NATBoolean.atValue(this.base__opltx__opeql__opgtx_(other).equals(NATNumber.MONE));
	}
	/**
	 * a > b iff (a <=> b) == +1
	 */
	public ATBoolean base__opgtx_(ATNumeric other) throws InterpreterException {
		return NATBoolean.atValue(this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ONE));
	}
	/**
	 * a <= b iff (a <=> b) != +1
	 */
	public ATBoolean base__opltx__opeql_(ATNumeric other) throws InterpreterException {
		return NATBoolean.atValue(! this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ONE));
	}
	/**
	 * a >= b iff (a <=> b) != -1
	 */
	public ATBoolean base__opgtx__opeql_(ATNumeric other) throws InterpreterException {
		return NATBoolean.atValue(! this.base__opltx__opeql__opgtx_(other).equals(NATNumber.MONE));
	}
	/**
	 * a = b iff (a <=> b) == 0
	 */
	public ATBoolean base__opeql_(ATNumeric other) throws InterpreterException {
		return NATBoolean.atValue(this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ZERO));
	}
	/**
	 * a != b iff (a <=> b) != 0
	 */
	public ATBoolean base__opnot__opeql_(ATNumeric other) throws InterpreterException {
		return NATBoolean.atValue(! this.base__opltx__opeql__opgtx_(other).equals(NATNumber.ZERO));
	}
	
	/**
	 * This hashmap stores all native methods of native AmbientTalk numerics.
	 * It is populated when this class is loaded, and shared between all
	 * AmbientTalk actors on this VM. This is safe, since {@link DirectNativeMethod}
	 * instances are all immutable.
	 */
	private static final HashMap<String, ATMethod> _meths = new HashMap<String, ATMethod>();
	
	// initialize NATNumeric methods
	static {
		_meths.put("cos", new DirectNativeMethod("cos") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 0);
				return self.base_cos();
			}
		});
		_meths.put("sin", new DirectNativeMethod("sin") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 0);
				return self.base_sin();
			}
		});
		_meths.put("tan", new DirectNativeMethod("tan") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 0);
				return self.base_tan();
			}
		});
		_meths.put("log", new DirectNativeMethod("log") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 0);
				return self.base_log();
			}
		});
		_meths.put("sqrt", new DirectNativeMethod("sqrt") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 0);
				return self.base_sqrt();
			}
		});
		_meths.put("expt", new DirectNativeMethod("expt") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric pow = get(args, 1).asNativeNumeric();
				return self.base_expt(pow);
			}
		});
		_meths.put("<", new DirectNativeMethod("<") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric other = get(args, 1).asNativeNumeric();
				return self.base__opltx_(other);
			}
		});
		_meths.put(">", new DirectNativeMethod(">") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric other = get(args, 1).asNativeNumeric();
				return self.base__opgtx_(other);
			}
		});
		_meths.put("<=", new DirectNativeMethod("<=") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric other = get(args, 1).asNativeNumeric();
				return self.base__opltx__opeql_(other);
			}
		});
		_meths.put(">=", new DirectNativeMethod(">=") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric other = get(args, 1).asNativeNumeric();
				return self.base__opgtx__opeql_(other);
			}
		});
		_meths.put("=", new DirectNativeMethod("=") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric other = get(args, 1).asNativeNumeric();
				return self.base__opeql_(other);
			}
		});
		_meths.put("!=", new DirectNativeMethod("!=") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATNumeric self = ctx.base_receiver().asNativeNumeric();
				checkArity(args, 1);
				NATNumeric other = get(args, 1).asNativeNumeric();
				return self.base__opnot__opeql_(other);
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
	 * @see NATNumeric#hasLocalMethod(ATSymbol)
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
