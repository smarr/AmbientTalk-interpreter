/**
 * AmbientTalk/2 Project
 * CMDRequireService.java created on 22-feb-2007 at 20:17:06
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

import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.natives.Packet;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.actors.net.comm.CommunicationBus;

/**
 * A CMDRequireService message is sent asynchronously to all connected VMs when an actor
 * on the sender VM has issued a new subscription. This command is sent to all VMs to ask
 * them whether they have any publications matching the outstanding subscriptions at the sender VM.
 * The reply comes asynchronously in the form of a CMDJoinServices command.
 * 
 * SENDER: the VM that issued a new subscription on a topic
 * RECEIVER: all connected VMs
 * MODE: ASYNCHRONOUS, MULTICAST
 * PROPERTIES: the new subscription topic of the sender
 * REPLY: potential CMDJoinServices
 * 
 * @author tvcutsem
 */
public class CMDRequireService extends VMCommand {

	private final Packet serializedTopic_;
	
	public CMDRequireService(Packet topic) {
		super("requireService");
		serializedTopic_ = topic;
	}
	
	public void send(CommunicationBus dispatcher) {
		dispatcher.sendAsyncMulticast(this);
	}
	
	/**
	 * When a connected VM receives a CMDRequireService request, it queries its local publications
	 * to see if there is a match. If so, all matching local publications are replied.
	 */
	public void uponReceiptBy(ELVirtualMachine remoteHost, Address senderAddress) {
		// query local discovery actor for matching topic
    	remoteHost.discoveryActor_.event_remoteSubscription(serializedTopic_, senderAddress);
	}
	
}
