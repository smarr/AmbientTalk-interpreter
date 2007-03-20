/**
 * AmbientTalk/2 Project
 * CMDTransmitATMessage.java created on 22-feb-2007 at 14:23:11
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
package edu.vub.at.actors.net.cmd;

import edu.vub.at.actors.id.ActorID;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.natives.Packet;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A CMDTransmitATMessage message encapsulates an AmbientTalk message being sent
 * from a sending actor to a recipient actor.
 * 
 * SENDER: a remote far reference
 * RECEIVER: the VM hosting the object that the far reference denotes
 * MODE: SYNCHRONOUS, UNICAST
 * PROPERTIES:
 *  - Packet representing serialized AT message,
 *  - id of the actor that should unserialize and process the message
 * REPLY: no other VM command, but should return synchronous acknowledgement
 * 
 * @author tvcutsem
 */
public class CMDTransmitATMessage extends VMCommand {

	private final Packet serializedATMessage_;
	private final ActorID destinationActorId_;
	
	public CMDTransmitATMessage(ActorID destinationActorId, Packet atMessage) {
		super("transmitATMessage("+atMessage+")");
		serializedATMessage_ = atMessage;
		destinationActorId_ = destinationActorId;
	}
	
	public Object send(MessageDispatcher dispatcher, Address recipientVM) throws Exception {
		Object returnValue = super.sendSynchronousUnicast(dispatcher, recipientVM);
		if (returnValue instanceof Exception) {
			throw (Exception) returnValue;
		} else 
			return returnValue;
	}
	
	public Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception {
		remoteHost.getActor(destinationActorId_).event_remoteAccept(wrapper.getSrc(), serializedATMessage_);
		return null; // null return value means OK
	}
}