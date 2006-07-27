/**
 * AmbientTalk/2 Project
 * JavaMethod.java created on Jul 27, 2006 at 1:35:19 AM
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;

import java.lang.reflect.Method;

/**
 * @author smostinc
 *
 * JavaMethod is a wrapper class around Java methods allowing them to be selected 
 * from base-level objects and passed around as ordinary methods.
 */
public abstract class JavaMethod extends NATNil implements ATClosure, ATMethod {

	protected final ATObject receiver_;
	
	protected JavaMethod(ATObject receiver) {
		receiver_ = receiver;
	}
	
	public ATMethod getMethod() {
		return this;
	}

	public ATContext getContext() {
		return new NATContext(receiver_, receiver_, NATNil.instance());
	}

	public boolean isClosure() {
		return true;
	}

	public ATClosure asClosure() throws XTypeMismatch {
		return this;
	}

	public static class Simple extends JavaMethod {
		
		private Method m_;
		
		public Simple(ATObject receiver, Method m) {
			super(receiver);
			m_ = m;
		}

		public ATObject meta_apply(ATTable arguments) throws NATException {
			try {
				return NATObject.cast(
						m_.invoke(receiver_, arguments.asNativeTable().elements_));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new NATException("Invocation on a Java Method failed", e);
			}
		}

		public ATTable getArguments() {
			// TODO Auto-generated method stub
			return null;
		}

		public ATAbstractGrammar getBody() {
			// TODO Auto-generated method stub
			return null;
		}

		public ATSymbol getName() {
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}
}
