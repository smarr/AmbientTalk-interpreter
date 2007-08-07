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
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
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

	// The primitive methods of a native asynchronous message
	
	/** def process(bhv) { nil } */
	private static final PrimitiveMethod _PRIM_PRO_ = new PrimitiveMethod(
			AGSymbol.jAlloc("process"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("bhv")})) {
      
	    private static final long serialVersionUID = -1307795172754072220L;

		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			int arity = arguments.base_length().asNativeNumber().javaValue;
			if (arity != 1) {
				throw new XArityMismatch("process", 1, arity);
			}
			return ctx.base_lexicalScope().asAsyncMessage().prim_process(
					ctx.base_self().asAsyncMessage(),
					arguments.base_at(NATNumber.ONE));
		}
	};
	
    /**
     * Create a new asynchronous message.
     * @param sel the selector of the asynchronous message
     * @param arg the arguments of the asynchronous message
     * @param types the types for the message. Isolate and AsyncMessage types are automatically added.
     */
    public NATAsyncMessage(ATSymbol sel, ATTable arg, ATTable types) throws InterpreterException {
    	super(sel, arg, types, NativeTypeTags._ASYNCMSG_);
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
            ATTypeTag[] types) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, types);
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
    		                        ATTypeTag[] types) throws InterpreterException {
		return new NATAsyncMessage(map,
				                   state,
				                   originalCustomFields,
				                   methodDict,
				                   dynamicParent,
				                   lexicalParent,
				                   flags,
				                   types);
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
    	// receiver is not necessarily equal to base_receiver() anymore
    	return receiver.meta_invoke(receiver, self.base_selector(), self.base_arguments());
    }

    /**
     * To evaluate an asynchronous message send, the asynchronous
     * message object is asked to be <i>sent</t> by the sender object.
     * I.e.: <tt>(reflect: sender).send(receiver, asyncmsg)</tt>
     * 
     * @return NIL, by default. Overridable by the sender.
     */
    public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException {
        return sender.meta_send(receiver, self.asAsyncMessage());
    }
    
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<asynchronous message:"+base_selector()+Evaluator.printAsList(base_arguments()).javaValue+">");
	}

    public ATAsyncMessage asAsyncMessage() throws XTypeMismatch {
  	    return this;
  	}

}
