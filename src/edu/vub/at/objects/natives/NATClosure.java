/**
 * AmbientTalk/2 Project
 * NATClosure.java created on Jul 23, 2006 at 3:22:23 PM
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
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;

/**
 * @author smostinc
 *
 * A NATClosure instance represents a first-class AmbientTalk closure.
 * A closure is modelled as a pair (method, context), where the method
 * contains the pure function (function name, arguments and body).
 *
 * The single most important operation to be performed on a closure is applying it.
 * This will give rise to the application of its underlying method within the context
 * wrapped by the closure.
 */
public class NATClosure extends NATNil implements ATClosure {

	// these instance variables are inherited and used by a JavaClosure as well.
	protected ATMethod 	method_;
	protected ATContext	context_;
	
	/**
	 * This constructor creates a closure with a bound dynamic receiver, and it is
	 * called after the succesful lookup of a receiverful message.
	 * @param method the method being wrapped into a closure.
	 * @param implementor the object in which the definition is nested.
	 * @param receiver the object where the lookup was initiated.
	 */
	public NATClosure(ATMethod method, ATObject implementor, ATObject receiver) {
		this(method, new NATContext(
				/* scope = implementor (to be extended with a callframe upon closure application) */
				implementor, 
				/* self = ` start of lookup ` */
				receiver, 
				/* super = implementor.getNext() */
				implementor.meta_getDynamicParent()));
	}
	
	public NATClosure(ATMethod method, ATContext context) {
		method_	= method;
		context_	= context;
	}

	/**
	 * To apply a closure, apply its underlying method with the context of the closure,
	 * rather than the runtime context of the invoker.
	 */
	public ATObject base_apply(ATObject[] arguments) throws NATException {
		return method_.base_apply(new NATTable(arguments), context_);
	}
	
	public ATObject base_applyWithArgs(ATTable arguments) throws NATException {
		return method_.base_apply(arguments, context_);
	}

	/**
	 * receiver is a zero-argument block closure returning a boolean
	 * @param body a zero-argument block closure
	 * 
	 * def whileTrue: body {
	 *   self.apply().ifTrue: {
	 *     body();
	 *     self.whileTrue: body
	 *   }
	 * }
	 */
	public ATObject base_whileTrue_(final ATClosure body) throws NATException {
		/* ATObject result = NATNil._INSTANCE_;
		while (this.meta_apply(NATTable.EMPTY).asNativeBoolean().javaValue) {
			result = body.meta_apply(NATTable.EMPTY);
		}
		return result; */
		
		ATBoolean cond;
		while (true) {
			// cond = self.apply()
			cond = this.base_apply(NATTable.EMPTY.elements_).asBoolean();
			if(cond.isNativeBoolean()) {
				// cond is a native boolean, perform the conditional ifTrue: test natively
				if (cond.asNativeBoolean().javaValue) {
					// execute body and continue while loop
					body.base_apply(NATTable.EMPTY.elements_);
					continue;
				} else {
					// return nil
					return NATNil._INSTANCE_;
				}
			} else {
				// cond is a user-defined boolean, do a recursive send
				return cond.base_ifTrue_(new JavaClosure(this) {
					public ATObject base_apply(ATTable args) throws NATException {
						// if user-defined bool is true, execute body and recurse
						body.base_apply(NATTable.EMPTY.elements_);
						return base_whileTrue_(body);
					}
				});
			}
		}
		
	}
	
	public ATContext base_getContext() {
		return context_;
	}

	public ATMethod base_getMethod() {
		return method_;
	}

	public boolean isClosure() {
		return true;
	}

	public ATClosure asClosure() {
		return this;
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<closure:"+method_.base_getName()+">");
	}

	
}
