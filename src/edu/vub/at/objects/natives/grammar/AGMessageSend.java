/**
 * AmbientTalk/2 Project
 * AGMessageSend.java created on 26-jul-2006 at 16:03:06
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageSend;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a synchronous message send AG element.
 */
public final class AGMessageSend extends AGExpression implements ATMessageSend {

	private ATExpression rcvExp_;
	private final ATExpression message_;
	
	public AGMessageSend(ATExpression rcv, ATExpression msg) {
		rcvExp_ = rcv;
		message_ = msg;
	}
	
	public ATExpression base_getReceiverExpression() { return rcvExp_; }
	
	public ATNil base_setReceiverExpression(ATExpression rcv) { 
		rcvExp_ = rcv;
		return NATNil._INSTANCE_;
	}

	public ATExpression base_getMessageExpression() { return message_; }

	/**
	 * To evaluate a message send, evaluate the receiver expression into an object.
	 * Next, evaluate the message expression into a first-class message (asynchronous message or method invocation).
	 * The evaluation of the message send is delegated to the first-class message itself.
	 * 
	 * AGMSGSEND(rcv,msg).eval(ctx) = msg.eval(ctx).meta_sendTo(rcv.eval(ctx))
	 * 
	 * @return the value of the invoked method or NIL in the case of an asynchronous message send.
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		return message_.meta_eval(ctx).asMessage().base_sendTo(rcvExp_.meta_eval(ctx));
	}

	/**
	 * Quoting a message send returns a new quoted message send.
	 * 
	 * AGMSGSEND(rcv,msg).quote(ctx) = AGMSGSEND(rcv.quote(ctx), msg.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGMessageSend(rcvExp_.meta_quote(ctx).asExpression(),
				                message_.meta_quote(ctx).asMessageCreation());
	}
	
	public NATText meta_print() throws NATException {
		return NATText.atValue(rcvExp_.meta_print().javaValue +
				               ((message_.isMessageCreation()) ? "" : "<+") + message_.meta_print().javaValue);
	}

}
