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
 * They encapsulate a selector symbol and a table of arguments.
 * Additionally, a message -- being a first-class object, may be extended with extra fields and behaviour (attachments).
 * 
 * The <i>receiver</i> of an asynchronous message is determined only at message sending
 * time. The receiver is not stored directly within the asynchronous message, but is rather
 * passed as an extra parameter in the {@link ATObject#meta_send(ATObject, ATAsyncMessage)}
 * meta-level method.
 * 
 * @author tvcutsem
 */
public interface ATAsyncMessage extends ATMessage {
	
    /**
     * This method is responsible for processing the message in a certain actor.
     * By default, process invokes the method corresponding to its selector:
     * 
     *  def process(receiver) {
     *    (reflect: receiver).invoke(receiver, self.selector, self.arguments)
     *  }
     * 
     * @param receiver the object that has been designated to receive the message
     */
    public ATObject base_process(ATObject receiver) throws InterpreterException;

    /**
     * The primitive implementation of base_process.
     * @param self the ambienttalk object that originally received the 'process' message.
     */
    public ATObject prim_process(ATAsyncMessage self, ATObject receiver) throws InterpreterException;
}
