/**
 * AmbientTalk/2 Project
 * NATRemoteFarRef.java created on 22-dec-2006 at 11:35:01
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
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;

/**
 * Instances of NATRemoteFarRef represent far references to physically remote actors.
 * By 'physically remote', we mean in a separate address space.
 * 
 * @author tvcutsem
 */
public class NATRemoteFarRef extends NATFarReference {
	
	/**
	 * When a remote far reference is passed on to another virtual machine, the event loop
	 * is not taken with it. At the remote end, a new far reference will be created with the
	 * appropriate event loop.
	 */
	private transient final ELFarReference sendLoop_;

	public NATRemoteFarRef(ATObjectID objectId, ELActor hostActor, ATTypeTag[] types, boolean isConnected) {
		super(objectId, types, hostActor, isConnected);
		sendLoop_ = new ELFarReference(objectId, hostActor, this, isConnected);
	}
	
	protected void transmit(ATAsyncMessage message) throws InterpreterException {
		sendLoop_.event_transmit(this, message);
	}
	
	public ATTable meta_retractUnsentMessages() throws InterpreterException {
		return sendLoop_.retractUnsentMessages();
	}
	
	protected synchronized void notifyStateToSendLoop(boolean state){
		sendLoop_.setConnected(state);
	}

}
