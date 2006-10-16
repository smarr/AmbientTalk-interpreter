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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XUserDefined;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;

import java.util.Iterator;
import java.util.Vector;

/**
 * A NATClosure instance represents a first-class AmbientTalk closure.
 * A closure is modelled as a pair (method, context), where the method
 * contains the pure function (function name, arguments and body).
 *
 * The single most important operation to be performed on a closure is applying it.
 * This will give rise to the application of its underlying method within the context
 * wrapped by the closure.
 * 
 * @author smostinc
 */
public class NATClosure extends NATNil implements ATClosure {

	/**
	 * This flag determines whether the closure has a non-empty handler set associated
	 * to it. In this case exceptions may need to be caught (at the java-level) during
	 * the application of the closure. We distinguish two cases:
	 *  - 1: exceptions should be caught.
	 *  - 0: empty handler set, do not catch exceptions.
	 */
	private static final byte _HASHANDLERS_FLAG_ = 1<<0;
	
	/**
	 * This flag determines whether the closure has handlers for the default exceptions
	 * thrown by the interpreter rather than user code. We distinguish two cases:
	 *  - 1: native exceptions should be caught.
	 *  - 0: all caught exceptions are native exceptions.
	 */
	private static final byte _HASNATIVEHANDLERS_FLAG_ = 1<<1;
	
	/**
	 * The flags of an AmbientTalk closure encode the following boolean information:
	 *  Format: 0b000000nh where
	 *   h = handlers flag: if set, the closure has associated exception handlers
	 *   n = native handlers flag: if set, the closure has handlers for interpreter exceptions
	 */
	private byte flags_;
	
	private Vector		handlers_ = new Vector();
	
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
	public NATClosure(ATMethod method, ATObject implementor, ATObject receiver) throws InterpreterException {
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
	 * 
	 * TODO Discuss handling policy when Tom gets back
	 * Exceptions are now handled as follows: 
	 * - When the user registers no handlers on a closure, the exception is not intercepted
	 *   at the java-level. This is done purely not to register Java handlers on the stack
	 *   when it is certain they will never be useful.
	 * - When the user catches exceptions, we distinguish between user and interpreter
	 *   exceptions. This implies that if we can derive that the latter will not be caught,
	 *   they are also not caught at the Java-level. This decision may percolate to the 
	 *   ambienttalk-level is custom canHandle code shows which exceptions are evaluated.
	 *   In this case the code would log more exceptions is an empty handler which rethrows
	 *   its parameters was added. Whereas this may seem strange, this design also has its 
	 *   benefits : when users do not wish to interfere with the interpreter semantics cannot
	 *   inadvertently be confronted with its internal exceptions.
	 *   
	 */
	public ATObject base_apply(ATTable arguments) throws InterpreterException {
		NATCallframe scope = new NATCallframe(context_.base_getLexicalScope());
		if(isFlagSet(_HASHANDLERS_FLAG_) && isFlagSet(_HASNATIVEHANDLERS_FLAG_)) {
			try{
				method_.base_apply(arguments, context_.base_withLexicalEnvironment(scope));
			} catch (InterpreterException e) {
				ATClosure replacementCode = getAppropriateHandler(e);
				if(replacementCode != null) {
					return replacementCode.base_apply(new NATTable(new ATObject[] { e.getAmbientTalkRepresentation() }));
				} else {
					throw e;
				}
			}
		} else if(isFlagSet(_HASHANDLERS_FLAG_)) {
			try{
				method_.base_apply(arguments, context_.base_withLexicalEnvironment(scope));
			} catch (XUserDefined e) {
				ATClosure replacementCode = getAppropriateHandler(e);
				if(replacementCode != null) {
					return replacementCode.base_apply(new NATTable(new ATObject[] { e.getAmbientTalkRepresentation() }));
				} else {
					throw e;
				}
			}
		} /* else */
		return method_.base_apply(arguments, context_.base_withLexicalEnvironment(scope));
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
	public ATObject base_whileTrue_(final ATClosure body) throws InterpreterException {
		/* ATObject result = NATNil._INSTANCE_;
		while (this.meta_apply(NATTable.EMPTY).asNativeBoolean().javaValue) {
			result = body.meta_apply(NATTable.EMPTY);
		}
		return result; */
		
		ATBoolean cond;
		while (true) {
			// cond = self.apply()
			cond = this.base_apply(NATTable.EMPTY).asBoolean();
			if(cond.isNativeBoolean()) {
				// cond is a native boolean, perform the conditional ifTrue: test natively
				if (cond.asNativeBoolean().javaValue) {
					// execute body and continue while loop
					body.base_apply(NATTable.EMPTY);
					continue;
				} else {
					// return nil
					return NATNil._INSTANCE_;
				}
			} else {
				// cond is a user-defined boolean, do a recursive send
				return cond.base_ifTrue_(new JavaClosure(this) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						// if user-defined bool is true, execute body and recurse
						body.base_apply(NATTable.EMPTY);
						return base_whileTrue_(body);
					}
				});
			}
		}
		
	}
	
	public ATClosure base_withHandler_(ATHandler handler) throws InterpreterException {
		setFlag(_HASHANDLERS_FLAG_);
		if (! (handler.base_getFilter().asInterpreterException()  
				instanceof XUserDefined)) {
			setFlag( _HASNATIVEHANDLERS_FLAG_);
		};
		handlers_.add(handler);
		return this;
	}

	public ATContext base_getContext() throws InterpreterException {
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
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<closure:"+method_.base_getName()+">");
	}

	// private methods
	private boolean isFlagSet(byte flag) {
		return (flags_ & flag) != 0;
	}

	private void setFlag(byte flag) {
		flags_ = (byte) (flags_ | flag);
	}

	private ATClosure getAppropriateHandler(InterpreterException e) throws InterpreterException {
		for (Iterator handlerIterator = handlers_.iterator(); handlerIterator.hasNext();) {
			ATHandler handler = (ATHandler) handlerIterator.next();
			if(handler.base_canHandle(e.getAmbientTalkRepresentation()).asNativeBoolean().javaValue) {
				return handler.base_getHandler();
			};
		};
		return null;
	}
}
