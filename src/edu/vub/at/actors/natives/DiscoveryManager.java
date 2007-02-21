/**
 * AmbientTalk/2 Project
 * DiscoveryManager.java created on 18-jan-2007 at 16:03:30
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

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.util.MultiMap;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.jgroups.Address;

/**
 * The DiscoveryManager is responsible for coupling subscriptions to
 * corresponding publications.
 *
 * @author tvcutsem
 */
public final class DiscoveryManager {

	/**
	 * Map of publication topic to Set of published objects
	 */
	private final MultiMap publications_;
	
	/**
	 * Map of subscription topic to Set of subscribed closures
	 */
	private final MultiMap subscriptions_;
	
	/**
	 * A reference to the VM to which this discovery manager belongs,
	 * necessary to be able to send messages to query remote VMs for
	 * the services they offer.
	 */
	private final ELVirtualMachine hostVM_;
	
	public DiscoveryManager(ELVirtualMachine hostVM) {
		publications_ = new MultiMap();
		subscriptions_ = new MultiMap();
		hostVM_ = hostVM;
	}
	
	public void addPublication(ATStripe topic, NATFarReference object) {
		publications_.put(topic, object);
		notifySubscribers(topic, object);
	}
	
	public void deletePublication(ATStripe topic, NATFarReference object) {
		publications_.removeValue(topic, object);
	}
	
	public synchronized void addSubscription(ATStripe topic, NATFarReference subscriber) {
		subscriptions_.put(topic, subscriber);
		checkPublishers(topic, subscriber);
	}
	
	public synchronized void deleteSubscription(ATStripe topic, NATFarReference subscriber) {
		subscriptions_.removeValue(topic, subscriber);
	}
	
	private void notifySubscribers(ATStripe topic, NATFarReference published) {
		Set subscribersForTopic = (Set) subscriptions_.get(topic);
		if (subscribersForTopic != null) {
			for (Iterator iter = subscribersForTopic.iterator(); iter.hasNext();) {
				notify((NATFarReference) iter.next(), published);
			}	
		}
	}
	
	private void checkPublishers(ATStripe topic, NATFarReference subscriber) {
		Set publishersOfTopic = (Set) publications_.get(topic);
		if (publishersOfTopic != null) {
			for (Iterator iter = publishersOfTopic.iterator(); iter.hasNext();) {
				notify(subscriber, (NATFarReference) iter.next());
			}	
		}
	}
	
	private void notify(NATFarReference subscriber, NATFarReference published) {
		try {
			// only notify if subscriber is hosted by another actor than publisher
			ATObjectID subId = subscriber.getObjectId();
			ATObjectID pubId = published.getObjectId();
			if ((subId.getVirtualMachineId() != pubId.getVirtualMachineId()) ||
				(subId.getActorId() != pubId.getActorId())) {
				// subscriber<-apply([ published ])
				subscriber.meta_receive(
						new NATAsyncMessage(subscriber, subscriber, Evaluator._APPLY_,
							NATTable.atValue(new ATObject[] {
								NATTable.atValue(new ATObject[] { published })
							})
						)
				);
			}
		} catch (InterpreterException e) {
			// a far reference's receive operation should not throw an exception
			Logging.VirtualMachine_LOG.error("DiscoveryManager: error notifying subscriber closure: ", e);
		}
	}
	
	/**
	 * Notifies the discovery manager that a VM has joined the network.
	 * This VM may be a first-time participant or it may be a previously
	 * disconnected VM that has become reconnected.
	 * 
	 * The discoverymanager asks the newly joined VM whether it has any
	 * services that match the type of an outstanding subscription on this VM.
	 */
	public synchronized void memberJoined(Address virtualMachine) {
		Logging.VirtualMachine_LOG.info(hostVM_ + ": VM connected: " + virtualMachine);
		Set subscriptionTopics = subscriptions_.keySet();
		if (!subscriptionTopics.isEmpty()) {
		    try {
				// only send a discovery query if this VM requires some services
		    	Vector topics = new Vector(subscriptionTopics);
				hostVM_.event_sendDiscoveryQuery(virtualMachine, Packet.serialize(topics));
			} catch (Exception e) {
			    Logging.VirtualMachine_LOG.error(hostVM_ + ": error serializing topics for discovery query: ", e);
		    }
		}
	}
	
	public void memberLeft(Address virtualMachine) {
		Logging.VirtualMachine_LOG.info(hostVM_ + ": VM disconnected: " + virtualMachine);
	}
	
}
