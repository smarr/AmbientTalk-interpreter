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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Vector;

/**
 * Instances of the class NATMessage represent first-class messages.
 * A NATMessage is an abstract class, as it can be either a synchronous method invocation or an asynchronous message send.
 * 
 * NATMessages subclass from NATIsolate, such that they represent true AmbientTalk objects.
 * 
 * @author tvc
 */
public abstract class NATMessage extends NATObject implements ATMessage {
	
	private final static AGSymbol _SELECTOR_ = AGSymbol.jAlloc("selector");
	private final static AGSymbol _ARGUMENTS_ = AGSymbol.jAlloc("arguments");
	
	/** def sendTo(receiver, sender) { nil } */
	private static final PrimitiveMethod _PRIM_SND_ = new PrimitiveMethod(
			AGSymbol.jAlloc("sendTo"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("receiver"), AGSymbol.jAlloc("sender") })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			int arity = arguments.base_getLength().asNativeNumber().javaValue;
			if (arity != 2) {
				throw new XArityMismatch("sendTo", 2, arity);
			}
			return ctx.base_getLexicalScope().asMessage().prim_sendTo(
					ctx.base_getSelf().asMessage(),
					arguments.base_at(NATNumber.ONE), arguments.base_at(NATNumber.atValue(2)));
		}
	};
	
	
    /**
     * Converts the given table of annotations into an ATStripe array.
     * Each element of the annotations table is converted into a stripe.
     * Moreover, because messages are isolates, the Isolate stripe is automatically appended to the resulting array.
     * Also, each type of message has its own type of stripe, so a substripe of Message is also added.
     */
    protected static ATStripe[] annotationsToStripes(ATStripe msgStripe, ATTable annotations) throws InterpreterException {
		if (annotations == NATTable.EMPTY) {
			return new ATStripe[] { NativeStripes._ISOLATE_, msgStripe };
		}
    	
        ATObject[] unwrapped = annotations.asNativeTable().elements_;
		ATStripe[] fullstripes = new ATStripe[unwrapped.length+2];
		for (int i = 0; i < unwrapped.length; i++) {
			fullstripes[i] = unwrapped[i].asStripe();
		}
		fullstripes[unwrapped.length] = NativeStripes._ISOLATE_;
		fullstripes[unwrapped.length+1] = msgStripe;
        return fullstripes;
    }
	
	/**
	 * Construct a new message from the given selector, arguments and annotations.
	 * The annotations become this message's stripes.
	 * 
	 * @param annotations a table of objects that should be convertible to stripes
	 * @param msgStripe a substripe of the Message stripe, added by subclasses to mark which kind of native message is created
	 */
	protected NATMessage(ATSymbol sel, ATTable arg, ATTable annotations, ATStripe msgStripe) throws InterpreterException {
		super(annotationsToStripes(msgStripe, annotations));
		super.meta_defineField(_SELECTOR_, sel);
		super.meta_defineField(_ARGUMENTS_, arg);
		super.meta_addMethod(_PRIM_SND_);
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
            ATStripe[] stripes) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, stripes);
    }
	
	public ATSymbol base_getSelector() throws InterpreterException {
		return super.meta_select(this, _SELECTOR_).asSymbol();
	}

	public ATTable base_getArguments() throws InterpreterException {
		return super.meta_select(this, _ARGUMENTS_).asTable();
	}
	
	public ATNil base_setArguments(ATTable arguments) throws InterpreterException {
		super.meta_assignField(this, _ARGUMENTS_, arguments);
		return NATNil._INSTANCE_;
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
