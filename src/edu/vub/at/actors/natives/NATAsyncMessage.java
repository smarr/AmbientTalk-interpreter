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

import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATMessage;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * Instances of the class NATAsyncMessage represent first-class asynchronous messages.
 * 
 * TODO: Possibly convert this class to a subclass of NATObject to allow adding methods and fields to it without having to make an object extension.
 * It is a primitive object which, for all other purposes, should be extensible like a regular object with extra fields and methods.
 * 
 * @author tvcutsem
 */
public class NATAsyncMessage extends NATMessage implements ATAsyncMessage {

    private final ATObject sender_;
    private ATObject receiver_ = NATNil._INSTANCE_;

    /**
     * @param sdr the sender of the asynchronous message
     * @param sel the selector of the asynchronous message
     * @param arg the arguments of the asynchronous message
     */
    public NATAsyncMessage(ATObject sdr, ATSymbol sel, ATTable arg) {
        super(sel, arg);
        sender_ = sdr;
    }
    
    public NATAsyncMessage(ATObject sdr, ATObject rcv, ATSymbol sel, ATTable arg) {
        this(sdr, sel, arg);
        receiver_ = rcv;
    }

    public ATObject base_getSender() {
        return sender_;
    }

    public ATObject base_getReceiver() {
        return receiver_;
    }
    
    public ATObject base_process(ATActorMirror inActor) throws InterpreterException {
    	return receiver_.meta_receive(this);
    }

    /**
     * To evaluate an asynchronous message send, an asynchronous invoke is performed
     * on the receiver object.
     *
     * @return NIL, by default. Overridable by the receiver.
     */
    public ATObject base_sendTo(ATObject receiver) throws InterpreterException {
        // fill in the receiver first
        this.receiver_ = receiver;
        return sender_.meta_send(this);
    }
    
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<asynchronous message:"+selector_+Evaluator.printAsList(arguments_).javaValue+">");
	}

    public ATAsyncMessage base_asAsyncMessage() throws XTypeMismatch {
  	    return this;
  	}

}
