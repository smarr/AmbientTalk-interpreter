/**
 * AmbientTalk/2 Project
 * ATAsyncMessage.java created on 31-jul-2006 at 12:16:22
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
package edu.vub.at.actors;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;

/**
 * Instances of the class ATAsyncMessage represent first-class asynchronous message sends.
 * They encapsulate a sender object, a receiver object, a selector symbol and a table of arguments.
 * Additionally, a message -- being a first-class object, may be extended with extra fields and behaviour (attachments).
 * 
 * @author tvc
 */
public interface ATAsyncMessage extends ATMessage {

    /**
     * Each message has a sender, namely the object on whose behalf the message was
     * sent. In other words the sender of a message corresponds to the self at the
     * site where the message was sent.
     */
    public ATObject base_getSender() throws InterpreterException;

    /**
     * Messages also have an explicitly named receiver, which may either be a local
     * object, or a representative of an object inside another actor.
     * @return the receiver of the message
     */
    public ATObject base_getReceiver() throws InterpreterException;
    
    /**
     * This method is responsible for processing the message in a certain actor.
     * By default, process dispatches to the actor by means of receive. In pseudo-code:
     *  def process(act) {
     *    (reflect: act).receive(self)
     *  }
     */
    public ATObject base_process(ATActorMirror inActor) throws InterpreterException;

}
