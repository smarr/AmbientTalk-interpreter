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
import edu.vub.at.actors.net.Logging;
import edu.vub.at.objects.ATStripe;
import edu.vub.util.MultiMap;

import java.util.Set;

import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.MessageDispatcher;

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

	private final ATStripe topic_;
	
	public CMDRequireService(ATStripe topic) {
		super("requireService");
		topic_ = topic;
	}
	
	public void send(MessageDispatcher dispatcher) {
		try {
			super.sendAsyncMulticast(dispatcher);
		} catch (TimeoutException e) {
			Logging.VirtualMachine_LOG.warn(this + ": timeout while trying to broadcast require query, dropping");
		} catch (SuspectedException e) {
			Logging.VirtualMachine_LOG.warn(this + ": remote VM suspected while broadcasting require query");
		}
	}
	
	/**
	 * When a connected VM receives a CMDRequireService request, it queries its local publications
	 * to see if there is a match. If so, all matching local publications are replied.
	 */
	public Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception {
		// query local discoverymanager for matching topic
    	Set matchingServices = remoteHost.discoveryManager_.getLocalPublications(topic_);
		if (matchingServices != null) {
			// maps topics to sets of objects that are published under this topic
			MultiMap matchingTopics = new MultiMap();
			matchingTopics.putValues(topic_, matchingServices);
    		// send all matching topics back to the requestor
    		new CMDJoinServices(matchingTopics).send(remoteHost.messageDispatcher_, wrapper.getSrc());	
    	}
    	return null;
	}
	
}
