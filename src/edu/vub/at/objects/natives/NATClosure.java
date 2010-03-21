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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.signals.SignalEscape;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.parser.SourceLocation;

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
public class NATClosure extends NATByRef implements ATClosure {

	// these instance variables are inherited and used by a NativeClosure as well.
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
				receiver));
	}
	
	public NATClosure(ATMethod method, ATContext context) {
		method_	= method;
		context_ = context;
	}

	/**
	 * To apply a closure, apply its underlying method with the context of the closure,
	 * rather than the runtime context of the invoker.
	 */
	public ATObject base_apply(ATTable arguments) throws InterpreterException {
		return method_.base_apply(arguments, this.base_context());
	}

	/**
	 * To apply a closure in a given scope, apply its underlying method with a new context
	 * constructed from the scope object.
	 */
	public ATObject base_applyInScope(ATTable args, ATObject scope) throws InterpreterException {
		return method_.base_applyInScope(args, new NATContext(scope, scope));
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
					return Evaluator.getNil();
				}
			} else {
				// cond is a user-defined boolean, do a recursive send
				return cond.base_ifTrue_(new NativeClosure(this) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						// if user-defined bool is true, execute body and recurse
						body.base_apply(NATTable.EMPTY);
						return base_whileTrue_(body);
					}
				});
			}
		}
		
	}
	
	/**
	 * The following is a pseudo-code implementation of escape. The important difference
	 * between the native implementation and this pseudo-code is that the 'escaping exception'
	 * can *not* be caught at the AmbientTalk level. The SignalEscape is a truly native exception.
	 * 
	 * def block.escape() {
	 *   def returned := false;
	 *   def quit(@args) {
	 *     if: (returned) then: {
	 *       raise: XIllegalOperation.new("Cannot quit, escape activation already returned")
	 *     } else: {
	 *       raise: SignalEscape.new(block, if: (args.isEmpty()) then: nil else: args[1])
	 *     }
	 *   };
	 *   
	 *   try: {
	 *    block(quit);
	 *   } catch: SignalEscape using: {|e|
	 *     if: (e.block == block) then: {
	 *       e.val
	 *     } else: {
	 *       raise: e
	 *     }
	 *   } finally: {
	 *     returned := true;
	 *   }
	 * }
	 */
	public ATObject base_escape() throws InterpreterException {		
		final QuitClosureFrame f = new QuitClosureFrame();
		NativeClosure quit = new NativeClosure(this) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				if (f.alreadyReturned) {
					throw new XIllegalOperation("Cannot quit, escape activation already returned");
				} else {
					ATObject val;
					if (args.base_isEmpty().asNativeBoolean().javaValue) {
						val = Evaluator.getNil(); 
					} else {
						val = get(args, 1);
					}
					throw new SignalEscape(this.scope_.asClosure(), val);
				}
			}
		};
		
		try {
			return this.base_apply(NATTable.atValue(new ATObject[] { quit }));
		} catch(SignalEscape e) {
			if (e.originatingBlock == this) {
				return e.returnedValue;
			} else {
				// propagate the signal, it did not originate from this block
				throw e;
			}
		} finally {
			f.alreadyReturned = true;
		}
	}
	
	// helper class to get around the fact that Java has no true closures and hence
	// does not allow access to mutable lexically scoped free variables
	static private class QuitClosureFrame {
		/** if true, the escape block has already been exited */
		public boolean alreadyReturned = false;
	}

	public ATContext base_context() throws InterpreterException {
		return context_;
	}

	public ATMethod base_method() {
		return method_;
	}

	public ATClosure asClosure() {
		return this;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<closure:"+method_.base_name()+">");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._CLOSURE_);
    }
    
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}
	
	// Debugging API:
	
    private SourceLocation loc_;
    public SourceLocation impl_getLocation() { return loc_; }
    public void impl_setLocation(SourceLocation loc) {
    	// overriding the source location of an AmbientTalk object
    	// is probably the sign of a bug: locations should be single-assignment
    	// to prevent mutable shared-state. That is, loc_ is effectively 'final'
    	if (loc_ == null) {
        	loc_ = loc;  		
    	} else {
    		throw new RuntimeException("Trying to override source location of "+this.toString()+" from "+loc_+" to "+loc);
    	}
    }
    public SourceLocation impl_getSourceOf(ATSymbol sel) throws InterpreterException {
    	if (sel == Evaluator._APPLY_) {
    		return method_.impl_getLocation();
    	} else {
    		return super.impl_getLocation();
    	}
    }
    
}
