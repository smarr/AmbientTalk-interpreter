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
import edu.vub.at.exceptions.XTypeMismatch;
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
public class NATBoolean extends NATNil {
	
	/**
	 * Returns the corresponding ATBoolean given a java truth value
	 */
	public static ATBoolean atValue(boolean b) {
		if(b) {
			return NATTrue._INSTANCE_;
		} else {
			return NATFalse._INSTANCE_;
		}
	}
	
	public final boolean javaValue;
	
	public NATBoolean(boolean b) {
		javaValue = b;
	}
	
	public NATBoolean asNativeBoolean() {
		return this;
	}
	
	private static class NATTrue extends NATBoolean implements ATBoolean {
		
		public static NATTrue _INSTANCE_ = new NATTrue();
		
		public NATTrue() { super(true); }
		
		public ATObject ifTrue_(ATClosure clo) throws NATException {
			return clo.meta_apply(NATTable.EMPTY);
		}

		public ATObject ifFalse_(ATClosure clo) throws NATException {
			return NATNil._INSTANCE_;
		}
		
		public ATObject ifTrue_ifFalse_(ATClosure cons, ATClosure alt) throws NATException {
			return cons.meta_apply(NATTable.EMPTY);
		}
		
		public NATText meta_print() throws XTypeMismatch { return NATText.atValue("<true>"); }
		
	}

	private static class NATFalse extends NATBoolean implements ATBoolean {
		
		public static NATFalse _INSTANCE_ = new NATFalse();
		
		public NATFalse() { super(false); }
		
		public ATObject ifTrue_(ATClosure clo) throws NATException {
			return NATNil._INSTANCE_;
		}

		public ATObject ifFalse_(ATClosure clo) throws NATException {
			return clo.meta_apply(NATTable.EMPTY);
		}
		
		public ATObject ifTrue_ifFalse_(ATClosure cons, ATClosure alt) throws NATException {
			return alt.meta_apply(NATTable.EMPTY);
		}
		
		public NATText meta_print() throws XTypeMismatch { return NATText.atValue("<false>"); }
	}
	
	public static final NATTrue _TRUE_ = NATTrue._INSTANCE_;
	public static final NATFalse _FALSE_ = NATFalse._INSTANCE_;

}
