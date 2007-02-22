/**
 * AmbientTalk/2 Project
 * VMInitialDiscovery.java created on 22-feb-2007 at 13:20:13
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

import edu.vub.at.actors.natives.DiscoveryManager;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.objects.ATStripe;
import edu.vub.util.MultiMap;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A CMDInitRequireServices message is sent asynchronously to any VM just discovered in the environment
 * if thhe discovering VM has some open subscriptions. This command is sent to the discovered VM to ask
 * it whether it has any publications matching the outstanding subscriptions at the sender VM.
 * 
 * SENDER: the discovering VM
 * RECEIVER: the discovered VM
 * MODE: ASYNCHRONOUS, UNICAST
 * PROPERTIES: outstanding subscription topics of sender
 * REPLY: potential CMDJoinServices
 * 
 * @author tvcutsem
 */

public class CMDInitRequireServices extends VMCommand {

	private final Vector topics_;
	
	public CMDInitRequireServices(Vector topics) {
		super("initRequireServices");
		topics_ = topics;
	}
	
	public void send(MessageDispatcher dispatcher, Address recipientVM) {
		try {
			super.sendAsyncUnicast(dispatcher, recipientVM);
		} catch (TimeoutException e) {
			Logging.VirtualMachine_LOG.warn(this + ": timeout while trying to transmit discovery query, dropping");
		} catch (SuspectedException e) {
			Logging.VirtualMachine_LOG.warn(this + ": remote VM suspected while sending discovery query");
		}
	}
	
	public Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception {
		DiscoveryManager remoteDM = remoteHost.discoveryManager_;
        
		// maps topics to sets of objects that are published under this topic
		MultiMap matchingTopics = new MultiMap();
		
		// query local discoverymanager for matching topics
    	for (Iterator iter = topics_.iterator(); iter.hasNext();) {
			ATStripe topic = (ATStripe) iter.next();
			Set matchingServices = remoteDM.getLocalPublications(topic);
			if (matchingServices != null) {
				matchingTopics.putValues(topic, matchingServices);
			}	
		}
		
    	if (!matchingTopics.isEmpty()) {
    		// send all matching topics back to the requestor
    		new CMDJoinServices(matchingTopics).send(remoteHost.messageDispatcher_, wrapper.getSrc());	
    	}
		
    	return null;
	}
	
}
