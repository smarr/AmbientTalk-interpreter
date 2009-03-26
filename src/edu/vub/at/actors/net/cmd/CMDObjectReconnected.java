/**
 * AmbientTalk/2 Project
 * CMDObjectTakenOffline.java
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

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.actors.net.comm.CommunicationBus;

/**
 * A CMDObjectTakenOfflineSoft command is sent asynchronously to all connected VMs when 
 * an actor on the sender VM takes one of its objects offline. Connected VMs are notified
 * of this event so that they can check if they had a far remote reference to the offline object.
 * 
 * SENDER: the VM that reconnects an object
 * RECEIVER: all connected VMs 
 * MODE: ASYNCHRONOUS, MULTICAST
 * PROPERTIES: objectID of the reconnected object
 * REPLY: none
 * 
 * @author kpinte
 */
public class CMDObjectReconnected extends VMCommand{
	
	private static final long serialVersionUID = -2928210552460460485L;
	
	private final ATObjectID senderObjectId_;
	
	public CMDObjectReconnected(ATObjectID senderObjectId) {
		super("objectReconnected");
		senderObjectId_ = senderObjectId;
	}
	
	public void send(CommunicationBus dispatcher, Address recipient) {
		dispatcher.sendAsyncUnicast(this, recipient);
	}
	
	public void broadcast(CommunicationBus dispatcher) {
		dispatcher.sendAsyncMulticast(this);
	}
	
	public void uponReceiptBy(ELVirtualMachine remoteHost, Address senderAddress) {
		remoteHost.connectionManager_.notifyObjectReconnected(senderObjectId_);
	}
}
