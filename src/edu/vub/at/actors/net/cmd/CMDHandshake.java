/**
 * AmbientTalk/2 Project
 * CMDHandshake.java created on 22-feb-2007 at 20:26:21
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

import edu.vub.at.actors.id.GUID;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.Logging;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A handshake command is sent when a VM discovers another VM in its environment.
 * It sends its own GUID to the newly discovered VM, which can then add this GUID
 * to its VM address book.
 *
 * SENDER: the discovering VM
 * RECEIVER: the discovered VM
 * MODE: ASYNCHRONOUS, UNICAST
 * PROPERTIES: GUID of sender
 * REPLY: none
 * 
 * @author tvcutsem
 */
public class CMDHandshake extends VMCommand {

	private final GUID senderVMId_;
	
	public CMDHandshake(GUID senderVMId) {
		super("handshake");
		senderVMId_ = senderVMId;
	}
	
	public void send(MessageDispatcher dispatcher, Address recipientVM) {
		try {
			super.sendAsyncUnicast(dispatcher, recipientVM);
		} catch (TimeoutException e) {
			Logging.VirtualMachine_LOG.warn(this + ": timeout while trying to handshake, dropping");
		} catch (SuspectedException e) {
			Logging.VirtualMachine_LOG.warn(this + ": remote VM suspected while handshaking");
		}
	}
	
	public Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception {
		remoteHost.vmAddressBook_.addEntry(senderVMId_, wrapper.getSrc());
		// ask my discovery actor to send outstanding subscriptions to the newcomer
		remoteHost.discoveryActor_.event_sendAllSubscriptionsTo(wrapper.getSrc());
		//notify all remote references registered for the new VM.
		remoteHost.membershipNotifier_.notifyConnected(senderVMId_);
    	return null;
	}
	
}
