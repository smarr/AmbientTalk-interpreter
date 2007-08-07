/**
 * AmbientTalk/2 Project
 * CMDProvideService.java created on 22-feb-2007 at 20:24:31
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
 * A CMDProvideService message is sent asynchronously to all connected VMs when
 * an actor on the sender VM provided a new publication. Connected VMs are notified
 * of this such that they can match this new publication with local subscriptions. 
 * 
 * SENDER: the VM that provides a new publication
 * RECEIVER: all connected VMs
 * MODE: ASYNCHRONOUS, MULTICAST
 * PROPERTIES: the subscription info: topic and provided service object
 * REPLY: none
 * 
 * @author tvcutsem
 */
public class CMDProvideService extends VMCommand {

	private static final long serialVersionUID = 6460962904701864474L;
	
	private final Packet serializedTopic_;
	private final Packet serializedService_;
	
	public CMDProvideService(Packet topic, Packet service) {
		super("provideService");
		serializedTopic_ = topic;
		serializedService_ = service;
	}
	
	public void send(CommunicationBus dispatcher) {
		dispatcher.sendAsyncMulticast(this);
	}
	
	/**
	 * When a remote VM receives the notification, it checks whether any local subscriptions
	 * can be matched with the new published service.
	 */
	public void uponReceiptBy(ELVirtualMachine remoteHost, Address senderAddress) {
		// notify local discovery actor of the new provided service
		remoteHost.discoveryActor_.event_remotePublication(serializedTopic_, serializedService_);
	}
	
}