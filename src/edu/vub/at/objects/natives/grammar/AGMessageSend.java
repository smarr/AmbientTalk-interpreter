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

import edu.vub.at.eval.InvocationStack;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageSend;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.parser.SourceLocation;
import edu.vub.util.TempFieldGenerator;

import java.util.Set;

/**
 * @author tvc
 *
 * The native implementation of a synchronous message send AG element.
 */
public final class AGMessageSend extends AGExpression implements ATMessageSend {

	private final ATExpression rcvExp_;
	private final ATExpression message_;
	
	public AGMessageSend(ATExpression rcv, ATExpression msg) {
		rcvExp_ = rcv;
		message_ = msg;
	}
	
	public ATExpression base_receiverExpression() { return rcvExp_; }

	public ATExpression base_messageExpression() { return message_; }

	/**
	 * To evaluate a message send, evaluate the receiver expression into an object.
	 * Next, evaluate the message expression into a first-class message (asynchronous message or method invocation).
	 * The evaluation of the message send is delegated to the first-class message itself.
	 * 
	 * AGMSGSEND(rcv,msg).eval(ctx) = msg.eval(ctx).meta_sendTo(rcv.eval(ctx), ctx.self)
	 * 
	 * @return the value of the invoked method or NIL in the case of an asynchronous message send.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATMessage msg = message_.meta_eval(ctx).asMessage();
		ATObject rcvr = rcvExp_.meta_eval(ctx);
		ATObject result = null;
		InvocationStack stack = InvocationStack.getInvocationStack();
		ATTable args = msg.base_arguments();
		try {
			stack.methodInvoked(this, rcvr, args);
			result = msg.base_sendTo(rcvr, ctx.base_receiver());
		} finally {
			stack.methodReturned(result);
		}
		return result;
	}

	/**
	 * Quoting a message send returns a new quoted message send.
	 * 
	 * AGMSGSEND(rcv,msg).quote(ctx) = AGMSGSEND(rcv.quote(ctx), msg.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGMessageSend(rcvExp_.meta_quote(ctx).asExpression(),
				                 message_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(rcvExp_.meta_print().javaValue +
				               ((message_.isMessageCreation()) ? "" : "<+") + message_.meta_print().javaValue);
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue(rcvExp_.impl_asUnquotedCode(objectMap).javaValue +
				               ((message_.isMessageCreation()) ? "" : "<+") + message_.impl_asUnquotedCode(objectMap).javaValue);
	}
	
	/**
	 * FV(objExp <+ msgExp) = FV(objExp) U FV(msgExp)
	 */
	public Set impl_freeVariables() throws InterpreterException {
        Set fvObjExp = rcvExp_.impl_freeVariables();
        fvObjExp.addAll(message_.impl_freeVariables());
        return fvObjExp;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = rcvExp_.impl_quotedFreeVariables();
		qfv.addAll(message_.impl_quotedFreeVariables());
		return qfv;
	}

}
