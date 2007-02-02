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
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Instances of the class NATAsyncMessage represent first-class asynchronous messages.
 * 
 * @author tvcutsem
 */
public class NATAsyncMessage extends NATMessage implements ATAsyncMessage {

	private final static AGSymbol _SENDER_ = AGSymbol.jAlloc("sender");
	private final static AGSymbol _RECEIVER_ = AGSymbol.jAlloc("receiver");

    /**
     * @param sdr the sender of the asynchronous message
     * @param sel the selector of the asynchronous message
     * @param arg the arguments of the asynchronous message
     */
    public NATAsyncMessage(ATObject sdr, ATSymbol sel, ATTable arg) throws InterpreterException {
        super(sel, arg);
        super.meta_defineField(_SENDER_, sdr);
        super.meta_defineField(_RECEIVER_, NATNil._INSTANCE_);
    }
    
    public NATAsyncMessage(ATObject sdr, ATObject rcv, ATSymbol sel, ATTable arg) throws InterpreterException {
        this(sdr, sel, arg);
        super.meta_assignField(this, _RECEIVER_, rcv);
    }

    public ATObject base_getSender() throws InterpreterException {
        return super.meta_select(this, _SENDER_);
    }

    public ATObject base_getReceiver() throws InterpreterException {
    	return super.meta_select(this, _RECEIVER_);
    }
    
    public ATObject base_process(ATActorMirror inActor) throws InterpreterException {
    	return base_getReceiver().meta_receive(this);
    }

    /**
     * To evaluate an asynchronous message send, an asynchronous invoke is performed
     * on the receiver object.
     *
     * @return NIL, by default. Overridable by the receiver.
     */
    public ATObject base_sendTo(ATObject receiver) throws InterpreterException {
        // fill in the receiver first
        super.meta_assignField(this, _RECEIVER_, receiver);
        return base_getSender().meta_send(this);
    }
    
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<asynchronous message:"+base_getSelector()+Evaluator.printAsList(base_getArguments()).javaValue+">");
	}

    public ATAsyncMessage base_asAsyncMessage() throws XTypeMismatch {
  	    return this;
  	}

}
