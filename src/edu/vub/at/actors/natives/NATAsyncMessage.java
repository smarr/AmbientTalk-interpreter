/**
 * AmbientTalk/2 Project
 * NATAsyncMessage.java created on 31-jul-2006 at 12:34:20
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
package edu.vub.at.actors.natives;

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.MethodDictionary;
import edu.vub.at.objects.natives.NATMessage;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Vector;

/**
 * Instances of the class NATAsyncMessage represent first-class asynchronous messages.
 * 
 * @author tvcutsem
 */
public class NATAsyncMessage extends NATMessage implements ATAsyncMessage {

	private final static AGSymbol _RECEIVER_ = AGSymbol.jAlloc("receiver");
	
	// The primitive methods of a native asynchronous message
	
	/** def process(bhv) { nil } */
	private static final PrimitiveMethod _PRIM_PRO_ = new PrimitiveMethod(
			AGSymbol.jAlloc("process"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("bhv")})) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			int arity = arguments.base_getLength().asNativeNumber().javaValue;
			if (arity != 1) {
				throw new XArityMismatch("process", 1, arity);
			}
			return ctx.base_getLexicalScope().asAsyncMessage().prim_process(
					ctx.base_getSelf().asAsyncMessage(),
					arguments.base_at(NATNumber.ONE));
		}
	};
	
    /**
     * Create a new asynchronous message.
     * @param rcv the receiver of the message, fill in 'nil' if none is determined yet
     * @param sel the selector of the asynchronous message
     * @param arg the arguments of the asynchronous message
     * @param stripes the stripes for the message. Isolate and AsyncMessage stripes are automatically added.
     */
    public NATAsyncMessage(ATObject rcv, ATSymbol sel, ATTable arg, ATTable stripes) throws InterpreterException {
    	super(sel, arg, stripes, NativeStripes._ASYNCMSG_);
        super.meta_defineField(_RECEIVER_, rcv);
        super.meta_addMethod(_PRIM_PRO_);
    }
    
    /**
     * Copy constructor.
     */
    private NATAsyncMessage(FieldMap map,
            Vector state,
            LinkedList originalCustomFields,
            MethodDictionary methodDict,
            ATObject dynamicParent,
            ATObject lexicalParent,
            byte flags,
            ATStripe[] stripes) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, stripes);
    }
    
    /**
     * If cloning is not adapted for asynchronous messages, the result of cloning a
     * NATAsyncMessage is a NATObject, which is fine except that NATObject does not know
     * of prim_sendTo!
     */
    protected NATObject createClone(FieldMap map,
    		                        Vector state,
    		                        LinkedList originalCustomFields,
    		                        MethodDictionary methodDict,
    		                        ATObject dynamicParent,
    		                        ATObject lexicalParent,
    		                        byte flags,
    		                        ATStripe[] stripes) throws InterpreterException {
		return new NATAsyncMessage(map,
				                   state,
				                   originalCustomFields,
				                   methodDict,
				                   dynamicParent,
				                   lexicalParent,
				                   flags,
				                   stripes);
	}
    
    public ATObject base_getReceiver() throws InterpreterException {
    	return super.meta_select(this, _RECEIVER_);
    }
    
    /**
     * When process is invoked from the Java-level, invoke the primitive implementation
     * with self bound to 'this'.
     */
    public ATObject base_process(ATObject receiver) throws InterpreterException {
    	return this.prim_process(this, receiver);
    }
    
    /**
     * The default implementation is to invoke the method corresponding to this message's selector.
     */
    public ATObject prim_process(ATAsyncMessage self, ATObject receiver) throws InterpreterException {
    	// receiver is not necessarily equal to base_getReceiver() anymore
    	return receiver.meta_invoke(receiver, self.base_getSelector(), self.base_getArguments());
    }

    /**
     * To evaluate an asynchronous message send, an asynchronous invoke is performed
     * on the receiver object.
     *
     * @return NIL, by default. Overridable by the receiver.
     */
    public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException {
        // fill in the receiver first
        super.meta_assignField(self, _RECEIVER_, receiver);
        return sender.meta_send(self.asAsyncMessage());
    }
    
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<asynchronous message:"+base_getSelector()+Evaluator.printAsList(base_getArguments()).javaValue+">");
	}

    public ATAsyncMessage asAsyncMessage() throws XTypeMismatch {
  	    return this;
  	}

}
