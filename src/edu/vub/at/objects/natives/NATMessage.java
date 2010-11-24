/**
 * AmbientTalk/2 Project
 * NATMessage.java created on 31-jul-2006 at 12:31:31
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

/**
 * Instances of this class represent first-class messages.
 * This is an abstract class, as it can be either a synchronous method invocation or an asynchronous message send.
 * 
 * This class is a subclass of {@link NATObject}, such that its instances represent true AmbientTalk objects.
 * 
 * @author tvcutsem
 */
public abstract class NATMessage extends NATObject implements ATMessage {
	
	private final static AGSymbol _SELECTOR_ = AGSymbol.jAlloc("selector");
	private final static AGSymbol _ARGUMENTS_ = AGSymbol.jAlloc("arguments");
	
	/** def sendTo(receiver, sender) { nil } */
	private static final PrimitiveMethod _PRIM_SND_ = new PrimitiveMethod(        
			AGSymbol.jAlloc("sendTo"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("receiver"), AGSymbol.jAlloc("sender") })) {
      private static final long serialVersionUID = -3475956316807558583L;

      public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			int arity = arguments.base_length().asNativeNumber().javaValue;
			if (arity != 2) {
				throw new XArityMismatch("sendTo", 2, arity);
			}
			return ctx.base_lexicalScope().asMessage().prim_sendTo(
					ctx.base_receiver().asMessage(),
					arguments.base_at(NATNumber.ONE), arguments.base_at(NATNumber.atValue(2)));
		}
	};
	
	/** def from(sender) { nil } */
	private static final PrimitiveMethod _PRIM_FRM_ = new PrimitiveMethod(        
			AGSymbol.jAlloc("from"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("sender") })) {
	  private static final long serialVersionUID = -5721508425469755751L;
		
      public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			int arity = arguments.base_length().asNativeNumber().javaValue;
			if (arity != 1) {
				throw new XArityMismatch("from", 1, arity);
			}
			return ctx.base_lexicalScope().asMessage().base_from(arguments.base_at(NATNumber.ONE));
	  }
	};
	
	
    /**
     * Converts the given table of annotations into an ATTypeTag array.
     * Each element of the annotations table is converted into a type.
     * Moreover, because messages are isolates, the Isolate type is automatically appended to the resulting array.
     * Also, each type of message has its own type of type, so a subtype of Message is also added.
     */
    protected static ATTypeTag[] annotationsToTypes(ATTypeTag msgType, ATTable annotations) throws InterpreterException {
		if (annotations == NATTable.EMPTY) {
			return new ATTypeTag[] { NativeTypeTags._ISOLATE_, msgType };
		}
    	
        ATObject[] unwrapped = annotations.asNativeTable().elements_;
		ATTypeTag[] fulltypes = new ATTypeTag[unwrapped.length+2];
		for (int i = 0; i < unwrapped.length; i++) {
			fulltypes[i] = unwrapped[i].asTypeTag();
		}
		fulltypes[unwrapped.length] = NativeTypeTags._ISOLATE_;
		fulltypes[unwrapped.length+1] = msgType;
        return fulltypes;
    }
	
	/**
	 * Construct a new message from the given selector, arguments and annotations.
	 * The annotations become this message's types.
	 * 
	 * @param annotations a table of objects that should be convertible to types
	 * @param msgType a subtype of the Message type, added by subclasses to mark which kind of native message is created
	 */
	protected NATMessage(ATSymbol sel, ATTable arg, ATTable annotations, ATTypeTag msgType) throws InterpreterException {
		super(annotationsToTypes(msgType, annotations));
		super.meta_defineField(_SELECTOR_, sel);
		super.meta_defineField(_ARGUMENTS_, arg);
		super.meta_addMethod(_PRIM_SND_);
		super.meta_addMethod(_PRIM_FRM_);
	}

    /**
     * Copy constructor.
     */
    protected NATMessage(FieldMap map,
            Vector state,
            LinkedList originalCustomFields,
            MethodDictionary methodDict,
            ATObject dynamicParent,
            ATObject lexicalParent,
            byte flags,
            ATTypeTag[] types,
            Set freeVars) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, types, freeVars);
    }
	
	public ATSymbol base_selector() throws InterpreterException {
		return super.meta_invokeField(this, _SELECTOR_).asSymbol();
	}

	public ATTable base_arguments() throws InterpreterException {
		return super.meta_invokeField(this, _ARGUMENTS_).asTable();
	}
	
	public ATNil base_arguments__opeql_(ATTable arguments) throws InterpreterException {
		super.impl_invokeMutator(this, _ARGUMENTS_.asAssignmentSymbol(), NATTable.of(arguments));
		return Evaluator.getNil();
	}
	
	public ATClosure base_from(final ATObject sender) {
		final NATMessage msg = this;
		return new NativeClosure(this) {
			private static final long serialVersionUID = -5978871207209804505L;
			public ATObject base_apply(ATTable args) throws InterpreterException {
				ATObject[] arguments = args.asNativeTable().elements_;
				if (arguments.length != 1) {
					throw new XArityMismatch(msg.toString(), 1, arguments.length);
				}
				return msg.base_sendTo(arguments[0], sender);
			}
		};
	}
	
	/**
	 * If sendTo is invoked from the Java-level directly, use 'this' as the dynamic receiver.
	 */
	public ATObject base_sendTo(ATObject receiver, ATObject sender) throws InterpreterException {
		return this.prim_sendTo(this, receiver, sender);
	}

	public ATMessage asMessage() {
		return this;
	}
	
}
