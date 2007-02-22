/**
 * AmbientTalk/2 Project
 * CMDJoinServices.java created on 22-feb-2007 at 15:51:02
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
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.util.MultiMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A CMDJoinServices message is sent asynchronously as a reply to:
 *  - an initial discovery request from a VM
 *  - a subsequent discovery request when a VM subscribes to a new service
 * SENDER: the VM that provides some service objects
 * RECEIVER: the VM that previously sent a request for required services
 * MODE: ASYNCHRONOUS, UNICAST
 * PROPERTIES: a map of subscription topic to published objects
 * REPLY: none
 * 
 * @author tvcutsem
 */

public class CMDJoinServices extends VMCommand {

	/** A map from ATStripe to a set of ATObject references that provide that service */
	private final MultiMap providedServices_;
	
	public CMDJoinServices(MultiMap providedServices) {
		super("joinServices");
		providedServices_ = providedServices;
	}
	
	public void send(MessageDispatcher dispatcher, Address recipientVM) {
		try {
			super.sendAsyncUnicast(dispatcher, recipientVM);
		} catch (TimeoutException e) {
			Logging.VirtualMachine_LOG.warn(this + ": timeout while trying to transmit joined services, dropping");
		} catch (SuspectedException e) {
			Logging.VirtualMachine_LOG.warn(this + ": remote VM suspected while sending joined services");
		}
	}
	
	public Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception {
		// notify subscribers of the provided services
		Set entries = providedServices_.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			remoteHost.discoveryManager_.notifyOfExternalPublication((ATStripe) entry.getKey(),
					                                                 (ATObject) entry.getValue());
		}
    	return null;
	}
	
}
