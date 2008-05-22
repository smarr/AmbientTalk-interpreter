/**
 * AmbientTalk/2 Project
 * NATNil.java created on 15 jul 2007 at 18:33:28
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
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.util.logging.Logging;

/**
 * This class encapsulates the behaviour of the native
 * <tt>nil</tt> AmbientTalk object.
 * 
 * def nil := object: {
 *   super := dynamicSentinel;
 *   def ==(other); // native implementation
 *   def !=(other) { self.==(other).not }
 *   def new(@args) { def c := (reflect: self).clone(); c.init(@args); c } 
 *   def init(@args) { }
 * }
 * 
 * @author tvcutsem
 */
public class NATNil extends NATObject implements ATNil {
	
	/**
	 * Nil has a special parent object which ends the recursion
	 * along the dynamic delegation chain. These methods cannot be implemented
	 * directly in this class because this class still implements useful
	 * <tt>base_</tt> Java methods which have to be invoked by means of the
	 * implementations defined in {@link NativeATObject}.
	 * 
	 * This object is shared by all actors, but it is constant so introduces
	 * no concurrency issues.
	 */
	private static final NativeATObject dynamicSentinel_ = new NATByCopy() {

      private static final long serialVersionUID = -1307795172754062220L;
		// METHODS THAT END THE DYNAMIC DELEGATION CHAIN
		
		public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
			// no more delegation
			return NATBoolean._FALSE_;
		}
		
		/**
		 * When performing <tt>o.m()</tt> and <tt>m</tt> is not found, invoke
		 * <tt>doesNotUnderStand</tt> and apply the resulting closure to the given arguments.
		 */
		public ATObject impl_invokeAccessor(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector).base_apply(arguments);
		}
		
		public ATObject impl_invokeMutator(ATObject receiver, ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector).base_apply(arguments);
		}

		/**
		 * When performing <tt>o.x</tt> and <tt>x</tt> is not found, invoke
		 * <tt>doesNotUnderStand</tt> and apply the corresponding closure with zero arguments.
		 */
		public ATObject meta_invokeField(ATObject receiver, ATSymbol selector) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector).base_apply(NATTable.EMPTY);
		}
	    
	    public ATClosure impl_selectAccessor(ATObject receiver, final ATSymbol selector) throws InterpreterException {
	    	return receiver.meta_doesNotUnderstand(selector);
	    }

		public ATClosure impl_selectMutator(ATObject receiver, final ATAssignmentSymbol selector) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector);
		}

		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("dynamicsentinel");
		}
	};
	
	/** def !=(other) { self.==(other).not } */
	private static final ATSymbol _NEQ_NAME_ = AGSymbol.jAlloc("!=");
	private static final PrimitiveMethod _PRIM_NEQ_ = new PrimitiveMethod(        
			_NEQ_NAME_, NATTable.of(AGSymbol.jAlloc("other"))) {
      public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			int arity = arguments.base_length().asNativeNumber().javaValue;
			if (arity != 1) {
				throw new XArityMismatch("!=", 1, arity);
			}
			ATObject other = arguments.base_at(NATNumber.ONE);
			return ctx.base_receiver().base__opeql__opeql_(other).base_not();
		}
	};
	
	/** def init(@initargs) { self } */
	private static final PrimitiveMethod _PRIM_INI_ = new PrimitiveMethod(
			_INI_NAME_, NATTable.atValue(new ATObject[] { new AGSplice(AGSymbol.jAlloc("initargs")) })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ctx.base_receiver();
		}
	};
	
	/**
	 * Construct and initialize a new instance of <tt>nil</tt>. Only one instance
	 * of <tt>nil</tt> should be created per actor.
	 */
	public NATNil() {
		// super(dynamicParent, lexicalParent, parentType)

		// super := dynamicSentinel
		// the lexical root of nil is set to dynamicSentinel_ because,
		// if we would make it refer to Evaluator.getGlobalLexicalRoot(),
		// we get an infinite recursion: nil requires root, root requires nil, ...
		super(dynamicSentinel_, dynamicSentinel_, NATObject._SHARES_A_);

		// cannot use meta_addMethod because this method returns nil,
		// thus constructing nil requires already having a constructed nil...
		// super.meta_addMethod(_PRIM_NEQ_);
		
		// def !=(other) { self.==(other).not }
		methodDictionary_.put(_NEQ_NAME_, _PRIM_NEQ_);
		
		// def init(@args) { self }
		methodDictionary_.put(_INI_NAME_, _PRIM_INI_);
	}
	
	public ATBoolean base__opnot__opeql_(ATObject other) throws InterpreterException {
		// immediately hard-wired implementation, because we know 'self' is really
		// bound to the Java 'this'.
		// return this.meta_invoke(this, _PRIM_NEQ_.base_name(), NATTable.of(other));
		return this.base__opeql__opeql_(other).base_not();
	}

	public ATObject base_super() {
		return dynamicSentinel_;
	}
	
	public ATObject meta_pass() throws InterpreterException {
	    return this;
	}
	
    /**
     * After deserialization, ensure that nil becomes rebound to the nil
     * object of the new actor.
     */
    public ATObject meta_resolve() throws InterpreterException {
    	return Evaluator.getNil();
    }
    
    /** nil is a singleton */
    public ATObject meta_clone() throws InterpreterException {
    	return this;
    }

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("nil");
	}
	
}