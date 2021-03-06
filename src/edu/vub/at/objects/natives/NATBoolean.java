/**
 * AmbientTalk/2 Project
 * NATBoolean.java created on Jul 23, 2006 at 12:52:29 PM
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

import java.util.HashMap;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.util.TempFieldGenerator;

/**
 * NATBoolean is simply a container class for ambienttalk booleans. The native 
 * implementations of true and false can be accessed using the class's atValue
 * method.
 * 
 * @author smostinc
 */
public abstract class NATBoolean extends NATByCopy implements ATBoolean {
	
	/**
	 * Returns the corresponding ATBoolean given a java truth value. This constructor
	 * function is to be used only when the result to be given out depends on a 
	 * dynamic test, else the static fields _TRUE_ and _FALSE_ should be used instead.
	 */
	public static ATBoolean atValue(boolean b) {
		if (b) {
			return _TRUE_;
		} else {
			return _FALSE_;
		}
	}
	
	public final boolean javaValue;
	
	private NATBoolean(boolean b) {
		javaValue = b;
	}
	
	public boolean isNativeBoolean() {
		return true;
	}

	public ATBoolean asBoolean() {
		return this;
	}
	
	public NATBoolean asNativeBoolean() {
		return this;
	}
	
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._BOOLEAN_, NativeTypeTags._ISOLATE_);
    }
	
	public static class NATTrue extends NATBoolean {
		
		public static final NATTrue _INSTANCE_ = new NATTrue();
		
		public NATTrue() { super(true); }
		
		public NATText meta_print() throws InterpreterException { return NATText.atValue("true"); }
		
		public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
			if (objectMap.contains(this)) {
				return objectMap.getName(this);
			}
			NATText name = objectMap.put(this, NATText.atValue("true"));
			return name;
		}
		
		// base interface for true
		
		public ATObject base_ifTrue_(ATClosure clo) throws InterpreterException {
			return clo.base_apply(NATTable.EMPTY);
		}

		public ATObject base_ifFalse_(ATClosure clo) throws InterpreterException {
			return Evaluator.getNil();
		}
		
		public ATObject base_ifTrue_ifFalse_(ATClosure consequent, ATClosure alternative) throws InterpreterException {
			return consequent.base_apply(NATTable.EMPTY);
		}
		
		public ATBoolean base_and_(ATClosure other) throws InterpreterException {
			return other.base_apply(NATTable.EMPTY).asBoolean();
		}
		
		public ATBoolean base_or_(ATClosure other) throws InterpreterException {
			return this;
		}
		
		public ATBoolean base_and_and_(ATClosure b1, ATClosure b2) throws InterpreterException {
			return b1.base_apply(NATTable.EMPTY).asBoolean().base_and_(b2);
		}

		public ATBoolean base_or_or_(ATClosure b1, ATClosure b2) throws InterpreterException {
			return this;
		}

		public ATBoolean base_not() {
			return NATFalse._INSTANCE_;
		}
		
		public ATObject meta_resolve() throws InterpreterException {
			return NATTrue._INSTANCE_;
		}
		
	}

	public static class NATFalse extends NATBoolean {
		
		public static final NATFalse _INSTANCE_ = new NATFalse();
		
		public NATFalse() { super(false); }
		
		public NATText meta_print() throws InterpreterException { return NATText.atValue("false"); }
		
		public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
			if (objectMap.contains(this)) {
				return objectMap.getName(this);
			}
			NATText name = objectMap.put(this, NATText.atValue("false"));
			return name; 
		}

		// base interface for false
		
		public ATObject base_ifTrue_(ATClosure clo) throws InterpreterException {
			return Evaluator.getNil();
		}

		public ATObject base_ifFalse_(ATClosure clo) throws InterpreterException {
			return clo.base_apply(NATTable.EMPTY);
		}
		
		public ATObject base_ifTrue_ifFalse_(ATClosure consequent, ATClosure alternative) throws InterpreterException {
			return alternative.base_apply(NATTable.EMPTY);
		}
		
		public ATBoolean base_and_(ATClosure other) throws InterpreterException {
			return this;
		}
		
		public ATBoolean base_or_(ATClosure other) throws InterpreterException {
			return other.base_apply(NATTable.EMPTY).asBoolean();
		}
		
		public ATBoolean base_not() {
			return NATTrue._INSTANCE_;
		}
		
		public ATBoolean base_and_and_(ATClosure b1, ATClosure b2) throws InterpreterException {
			return this;
		}

		public ATBoolean base_or_or_(ATClosure b1, ATClosure b2) throws InterpreterException {
			return b1.base_apply(NATTable.EMPTY).asBoolean().base_or_(b2);
		}
		
		public ATObject meta_resolve() throws InterpreterException {
			return NATFalse._INSTANCE_;
		}
		
	}
	
	public static final NATTrue _TRUE_ = NATTrue._INSTANCE_;
	public static final NATFalse _FALSE_ = NATFalse._INSTANCE_;
	
}
