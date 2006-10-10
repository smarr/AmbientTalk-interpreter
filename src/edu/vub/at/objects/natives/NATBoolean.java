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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;

/**
 * @author smostinc
 *
 * NATBoolean is simply a container class for ambienttalk booleans. The native 
 * implementations of true and false can be accessed using the class's atValue
 * method.
 */
public abstract class NATBoolean extends NATNil implements ATBoolean {
	
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
	
	public NATBoolean(boolean b) {
		javaValue = b;
	}
	
	public boolean isBoolean() {
		return true;
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
	
	public static class NATTrue extends NATBoolean {
		
		public static NATTrue _INSTANCE_ = new NATTrue();
		
		public NATTrue() { super(true); }
		
		public NATText meta_print() throws NATException { return NATText.atValue("true"); }
		
		// base interface for true
		
		public ATObject base_ifTrue_(ATClosure clo) throws NATException {
			return clo.base_apply(NATTable.EMPTY);
		}

		public ATObject base_ifFalse_(ATClosure clo) throws NATException {
			// TODO Changed this 
			// return NATNil._INSTANCE_;
			return this;
		}
		
		public ATObject base_ifTrue_ifFalse_(ATClosure consequent, ATClosure alternative) throws NATException {
			return consequent.asClosure().base_apply(NATTable.EMPTY);
		}
		
		public ATBoolean base__opamp_(ATBoolean other) throws NATException {
			return other; // true & something = something
		}

		public ATBoolean base__oppls_(ATBoolean other) throws NATException {
			return this; // true | something = true
		}
		
		public ATBoolean base_and_(ATClosure other) throws NATException {
			return other.base_apply(NATTable.EMPTY).asBoolean();
		}
		
		public ATBoolean base_or_(ATClosure other) throws NATException {
			return this;
		}
		
		public ATBoolean base_not() {
			return NATFalse._INSTANCE_;
		}
		
	}

	public static class NATFalse extends NATBoolean {
		
		public static NATFalse _INSTANCE_ = new NATFalse();
		
		public NATFalse() { super(false); }
		
		public NATText meta_print() throws NATException { return NATText.atValue("false"); }

		// base interface for false
		
		public ATObject base_ifTrue_(ATClosure clo) throws NATException {
			// TODO Changed this 
			// return NATNil._INSTANCE_;
			return this;
		}

		public ATObject base_ifFalse_(ATClosure clo) throws NATException {
			return clo.base_apply(NATTable.EMPTY);
		}
		
		public ATObject base_ifTrue_ifFalse_(ATClosure consequent, ATClosure alternative) throws NATException {
			return alternative.asClosure().base_apply(NATTable.EMPTY);
		}
		
		public ATBoolean base__opamp_(ATBoolean other) throws NATException {
			return this; // false & something = false
		}

		public ATBoolean base__oppls_(ATBoolean other) throws NATException {
			return other; // false | something = something
		}
		
		public ATBoolean base_and_(ATClosure other) throws NATException {
			return this;
		}
		
		public ATBoolean base_or_(ATClosure other) throws NATException {
			return other.base_apply(NATTable.EMPTY).asBoolean();
		}
		
		public ATBoolean base_not() {
			return NATTrue._INSTANCE_;
		}
		
	}
	
	public static final NATTrue _TRUE_ = NATTrue._INSTANCE_;
	public static final NATFalse _FALSE_ = NATFalse._INSTANCE_;

}
