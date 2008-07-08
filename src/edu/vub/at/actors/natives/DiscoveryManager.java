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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.util.logging.Logging;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * The DiscoveryManager is responsible for coupling subscriptions to
 * corresponding publications.
 *
 * @author tvcutsem
 */
public final class DiscoveryManager {

	/**
	 * Small container class that represents an entry in the publications list.
	 */
	public static class Publication {
		public final ELActor providerActor_;
		public final Packet providedTypeTag_;
		public final Packet exportedService_;
		public ATTypeTag deserializedTopic_;
		public Publication(ELActor provider, Packet type, Packet exportedService) {
			providerActor_ = provider;
			providedTypeTag_ = type;
			exportedService_ = exportedService;
		}
	}
	
	/**
	 * Small container class that represents an entry in the subscriptions list.
	 */
	public static class Subscription {
		public final ELActor subscriberActor_;
		public final Packet requiredTypeTag_;
		public final Packet registeredHandler_;
		public final boolean isPermanentSubscription_;
		public ATTypeTag deserializedTopic_;
		public ATObject deserializedHandler_;
		public Subscription(ELActor subscriber, Packet type, Packet registeredHandler, boolean permanent) {
			subscriberActor_ = subscriber;
			requiredTypeTag_ = type;
			registeredHandler_ = registeredHandler;
			isPermanentSubscription_ = permanent;
		}
	}
	
	/**
	 * A list of Publication objects that represent locally exported service objects.
	 */
	private final LinkedList publications_;
	
	/**
	 * A list of Subscription objects that represent local subscription handlers.
	 */
	private final LinkedList subscriptions_;
	
	public DiscoveryManager() {
		publications_ = new LinkedList();
		subscriptions_ = new LinkedList();
	}
	
	/**
	 * A new local publication:
	 *  - is stored locally
	 *  - is checked against local subscriptions, which fire immediately
	 *  - is broadcast to all currently connected members (done by VM)
	 */
	public void addLocalPublication(Publication pub) {
		publications_.add(pub);
		notifyLocalSubscribers(pub);
	}
	
	/**
	 * A deleted local publication is simply deleted locally. No further actions
	 * are required because remote VMs do not cache publications.
	 */
	public void deleteLocalPublication(Publication pub) {
		publications_.remove(pub);
	}
	
	/**
	 * A new local subscription:
	 *  - is stored locally
	 *  - is checked against local publications, which may cause the subscription
	 *    to fire immediately
	 *  - is broadcast to all currently connected members (done by VM)
	 */
	public void addLocalSubscription(Subscription sub) {
		subscriptions_.add(sub);
		checkLocalPublishers(sub);
	}
	
	/**
	 * A deleted local subscription is simply deleted locally. No further actions
	 * are required because remote VMs do not cache subscriptions.
	 */
	public void deleteLocalSubscription(Subscription sub) {
		subscriptions_.remove(sub);
	}
	
	/**
	 * Returns all local publications matching the given topic. This method is used
	 * when a remote VM has broadcast a subscription request or when two VMs discover
	 * one another.
	 * 
	 * @return a Set of Packet objects representing the serialized form of objects
	 * published under a topic matching the argument topic.
	 */
	public Set getLocalPublishedServicesMatching(ATTypeTag topic) {
		HashSet matchingPubs = new HashSet();
		for (Iterator iter = publications_.iterator(); iter.hasNext();) {
			Publication pub = (Publication) iter.next();
			try {
				if (pub.deserializedTopic_.base_isSubtypeOf(topic).asNativeBoolean().javaValue) {
					matchingPubs.add(pub.exportedService_);
				}
			} catch (InterpreterException e) {
				Logging.Actor_LOG.error("error matching types while querying local publications:",e);
			}
		}
		return matchingPubs;
	}
	
	/**
	 * @return a Set of Packet objects denoting the serialized form of all topics for which
	 * a local subscription is still open.
	 */
	public Set getAllLocalSubscriptionTopics() {
		
		// Following Bugfix #54, in order to ensure that the returned set
		// contains no duplicate type tags, we maintain a second hashset
		// to filter upon deserialized type tags, because packets containing
		// the same serialized type tag are not equal!
		
		HashSet openSubs = new HashSet();
		HashSet encounteredSubTopics = new HashSet();
		for (Iterator iter = subscriptions_.iterator(); iter.hasNext();) {
			Subscription sub = (Subscription) iter.next();
			if (!encounteredSubTopics.contains(sub.deserializedTopic_)) {
				encounteredSubTopics.add(sub.deserializedTopic_);
				openSubs.add(sub.requiredTypeTag_);
			}
		}
		return openSubs;
	}
	
	/**
	 * When a remote VM hears the request of the local VM for services it requires,
	 * it returns its own matching services, using a CMDJoinServices command. Via this
	 * command, the local discovery manager is notified of external matches.
	 * 
	 * @param topic an outstanding subscription topic of this VM
	 * @param remoteService the remote service matching the topic
	 */
	public void notifyOfExternalPublication(ATTypeTag pubTopic, ATObject remoteService) {
		for (Iterator iter = subscriptions_.iterator(); iter.hasNext();) {
			Subscription sub = (Subscription) iter.next();
			try {
				// publication type Tp <: subscription type Ts
				if (pubTopic.base_isSubtypeOf(sub.deserializedTopic_).asNativeBoolean().javaValue) {
					// no need to test for separate actors, publisher is remote to this VM, so surely different actors
					notify(sub.deserializedHandler_, remoteService);
					// if the subscription is not permanent, cancel it
					if (!sub.isPermanentSubscription_) {
						iter.remove();
					}
				}
			} catch (InterpreterException e) {
				Logging.Actor_LOG.error("error matching types during external notification:",e);
			}
		}
	}
	
	/**
	 * When a new publication is added locally, it is first checked whether this publication
	 * can already satisfy some outstanding subscriptions on this VM (but from different actors)
	 */
	private void notifyLocalSubscribers(Publication pub) {
		ATObject deserializedService = null; // only deserialize once we have a match
		for (Iterator iter = subscriptions_.iterator(); iter.hasNext();) {
			Subscription sub = (Subscription) iter.next();
			try {
                // publication type Tp <: subscription type Ts
				if (pub.deserializedTopic_.base_isSubtypeOf(sub.deserializedTopic_).asNativeBoolean().javaValue) {
					
					// only notify if subscriber is hosted by another actor than publisher
					if (sub.subscriberActor_ != pub.providerActor_) {
						if (deserializedService == null) {
							// first deserialize publisher
							deserializedService = pub.exportedService_.unpack();
						}
						
						notify(sub.deserializedHandler_, deserializedService);
						
						// if the subscription is not permanent, cancel it
						if (!sub.isPermanentSubscription_) {
							iter.remove();
						}
					}
				}
			} catch (InterpreterException e) {
				Logging.Actor_LOG.error("error matching types during local notification:",e);
			}
		}
	}
	
	/**
	 * When a new subscription is added locally, it is first checked whether this subscription
	 * can already be satisfied by some local publications on this VM (but from different actors)
	 */
	private void checkLocalPublishers(Subscription sub) {
		for (Iterator iter = publications_.iterator(); iter.hasNext();) {
			Publication pub = (Publication) iter.next();
			try {
                // publication type Tp <: subscription type Ts
				if (pub.deserializedTopic_.base_isSubtypeOf(sub.deserializedTopic_).asNativeBoolean().javaValue) {
					
					// only notify if subscriber is hosted by another actor than publisher
					if (sub.subscriberActor_ != pub.providerActor_) {
						
						notify(sub.deserializedHandler_, pub.exportedService_.unpack());
						
						// if the subscription is not permanent, cancel it
						if (!sub.isPermanentSubscription_) {
							this.deleteLocalSubscription(sub);
						}
					}
				}
			} catch (InterpreterException e) {
				Logging.Actor_LOG.error("error matching types during local notification:",e);
			}
		}
	}

	/**
	 * Performs <code>handler&lt;-apply([ service ])@[]</code>
	 */
	private void notify(ATObject handler, ATObject service) {
		Logging.VirtualMachine_LOG.debug("notifying: "+handler+"<-(["+service+"])");
		try {
			Evaluator.trigger(handler, NATTable.of(service));
		} catch (InterpreterException e) {
			Logging.VirtualMachine_LOG.error("DiscoveryManager: error notifying subscriber closure:", e);
		}
	}
	
	public Publication[] listPublications(ELActor actor) {
		LinkedList matches = new LinkedList();
        for (Iterator iter = publications_.iterator(); iter.hasNext();) {
                Publication pub = (Publication) iter.next();
                if (pub.providerActor_ == actor) {
                        matches.add(pub);
                }
        }
        return (Publication[]) matches.toArray(new Publication[matches.size()]);
	}

	public Subscription[] listSubscriptions(ELActor actor) {
		LinkedList matches = new LinkedList();
        for (Iterator iter = subscriptions_.iterator(); iter.hasNext();) {
                Subscription sub = (Subscription) iter.next();
                if (sub.subscriberActor_ == actor) {
                        matches.add(sub);
                }
        }
        return (Subscription[]) matches.toArray(new Subscription[matches.size()]);
	}
}
