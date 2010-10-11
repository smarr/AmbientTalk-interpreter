/**
 * AmbientTalk/2 Project
 * ELDiscoveryActor.java created on 23-feb-2007 at 11:45:46
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.natives.DiscoveryManager.Publication;
import edu.vub.at.actors.natives.DiscoveryManager.Subscription;
import edu.vub.at.actors.net.cmd.CMDInitRequireServices;
import edu.vub.at.actors.net.cmd.CMDJoinServices;
import edu.vub.at.actors.net.cmd.CMDProvideService;
import edu.vub.at.actors.net.cmd.CMDRequireService;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.MultiMap;

/**
 * Every VM has an associated DiscoveryBus Actor. This is a regular actor (with a native Actor Mirror)
 * which is responsible for matching local publications with local and remote subscriptions.
 *
 * @author tvcutsem
 */
public final class ELDiscoveryActor extends ELActor {

	/** manages subscriptions and publications */
	private final DiscoveryManager discoveryManager_;
	
	public ELDiscoveryActor(ELVirtualMachine host) {
		super(host);
		discoveryManager_ = new DiscoveryManager();
	}
	
	public void initializeActorMirror() {
		// initilializing the actor mirror of the ELDiscoveryActor after creation so that 
		// this actor mirror points to the ELDiscoveryActor.
		NATActorMirror mirror = new NATActorMirror(host_);
		mirror.setActor(this);
		this.setActorMirror(mirror);
	}
	
	/**
	 * A dedicated initialization procedure for the discovery actor
	 */
	protected void event_init() {
		receive(new Event("initDiscovery("+this+")") {
			public void process(Object byMyself) {
				try {
					// !! CODE DUPLICATED FROM ELActor's event_init !!
					
					// initialize lexically visible fields
					initSharedFields();

					// Note that ELDiscoveryActor does not initialize the root and all lexically visible fields.
					// In short: the init.at file is NOT loaded for the discovery actor.
					
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error("error while initializing discovery actor", e);
				}
			}
		});
	}
	
	/**
     * This event is fired whenever an object
     * is being offered as a service provide using the provide: language construct. 
     * The discovery actor keeps track of such services and is responsible for the 
     * matching between services and clients. When such matches are detected the VM
     * will send a foundResolution event to both involved actors. If the VM detects 
     * that one partner has become unavailable it will send the lostResolution event
     * 
     * @param pub - a publication containing the serialized forms of the topic and the exported service object
     */
	public void event_servicePublished(final Publication pub) {
		this.receive(new Event("servicePublished("+pub.providedTypeTag_+")") {
			public void process(Object myself) {
				try {
					pub.deserializedTopic_ = pub.providedTypeTag_.unpack().asTypeTag();
					discoveryManager_.addLocalPublication(pub);
					// broadcast the new publication to all currently connected VMs
					new CMDProvideService(pub.providedTypeTag_, pub.exportedService_).send(host_.communicationBus_);
				} catch (InterpreterException e) {
					Logging.VirtualMachine_LOG.error("error while publishing service " + pub.providedTypeTag_,e);
				}
			}
		});
	}
	
    /**
     * This event is fired whenever an object
     * requests a service using the require: language construct. The discovery manager 
     * keeps track of such requests and is responsible matching services and clients. 
     * When such matches are detected the VM will send a foundResolution event to both
     * involved actors. If the VM detects that one partner has become unavailable it 
     * will send the lostResolution event
     * 
     * @param sub - a subscription containing the serialized forms of the topic and the subscription handler
     */
	public void event_clientSubscribed(final Subscription sub) {
		this.receive(new Event("clientSubscribed("+sub.requiredTypeTag_+")") {
			public void process(Object myself) {
				try {
					sub.deserializedTopic_ = sub.requiredTypeTag_.unpack().asTypeTag();
					sub.deserializedHandler_ = sub.registeredHandler_.unpack();
					discoveryManager_.addLocalSubscription(sub);
					// broadcast the new subscription to all currently connected VMs
					new CMDRequireService(sub.requiredTypeTag_).send(host_.communicationBus_);
				} catch (InterpreterException e) {
					Logging.VirtualMachine_LOG.error("error while subscribing to service " + sub.requiredTypeTag_,e);
				}
			}
		});
	}

    /**
     * This event is fired whenever a service
     * offer is being revoked. In this case, the discovery manager ensures that the 
     * object is no longer discoverable to new clients. However, it will not send 
     * disconnected events as these signal that an object has become unreachable.
     * In other words: remote objects that had already discovered the object linked
     * to this publication will maintain their connection.
     * 
     * @param pub - the original publication object to cancel
     */
	public void event_cancelPublication(final Publication pub) {
		this.receive(new Event("cancelPublication("+pub.providedTypeTag_+")") {
			public void process(Object myself) {
				discoveryManager_.deleteLocalPublication(pub);
			}
		});
	}
	
	/**
	 * This event is fired whenever a service
	 * offer is being disconnected. In this case, the discovery manager ensures that the 
	 * object is no longer discoverable to new clients.
	 * 
	 * @param obj - the object whose publications should be disconnected
	 */
	public void event_disconnectPublications(final ATObject obj) {
		this.receive(new Event("disconnectPublications("+obj+")") {
			public void process(Object myself) {
				discoveryManager_.disconnectLocalPublications(obj);
			}
		});
	}
	
	/**
	 * This event is fired to reconnect disconnected publications. The discovery manager
	 * will make the object discoverable to new clients. All other VMs are signaled the re-publication
	 * of publications associated with this object.
	 * 
	 * @param obj
	 */
	public void event_reconnectPublications(final ATObject obj) {
		this.receive(new Event("reconnectPublications("+obj+")") {
			public void process(Object myself) {
				Set matchingPubs = discoveryManager_.getLocalDisconnectedPublications(obj);
				// broadcast the new publication to all currently connected VMs
				for (Iterator iter = matchingPubs.iterator(); iter.hasNext();) {
					Publication pub = (Publication) iter.next();
					try {
						pub.deserializedTopic_ = pub.providedTypeTag_.unpack().asTypeTag();
						// put disconnected publications back in the local publications list.
						discoveryManager_.addLocalPublication(pub);
						// broadcast the new publication to all currently connected VMs
						new CMDProvideService(pub.providedTypeTag_, pub.exportedService_).send(host_.communicationBus_);
						Logging.VirtualMachine_LOG.debug("reconnected "+matchingPubs.size()+" publications");
					} catch (InterpreterException e) {
						Logging.VirtualMachine_LOG.error("error while publishing service " + pub.providedTypeTag_ + "of a reconnected object " + obj,e );
					}
				}
			}
		});
	}

    /**
     * This event is fired whenever a service
     * request is being revoked. In this case, the discovery manager ensures that the 
     * object will no longer discover new services. However, it will not send 
     * lostResolution events as these signal that the client has become unreachable.
     * 
     * @param sub - the original subscription object to cancel
     */
	public void event_cancelSubscription(final Subscription sub) {
		this.receive(new Event("cancelSubscription("+sub.requiredTypeTag_+")") {
			public void process(Object myself) {
				discoveryManager_.deleteLocalSubscription(sub);
			}
		});
	}
	
	
    /**
     * Received in response to the CMDProvideService command of a remote VM
     */
	public void event_remotePublication(final Packet serializedProvidedTopic, final Packet serializedProvidedService) {
		this.receive(new Event("remotePublication("+serializedProvidedTopic+")") {
			public void process(Object myself) {
				try {
					ATTypeTag providedTopic = serializedProvidedTopic.unpack().asTypeTag();
					ATObject providedService = serializedProvidedService.unpack();
					// notify subscribers of the new provided service
					Logging.VirtualMachine_LOG.debug("notifyOfExternalPublication("+providedTopic+","+providedService+")");
					discoveryManager_.notifyOfExternalPublication(providedTopic, providedService);
				} catch (InterpreterException e) {
					Logging.VirtualMachine_LOG.error("error while unserializing remote published service",e);
				}
			}
		});
	}
	
    /**
     * Received in response to the CMDJoinServices command of a remote VM
     * 
     * @param matchingPublications - a map from serialized ATTypeTag topics to Sets of serialized
     * ATObjects that provide the serialized topic.
     */
	public void event_batchRemotePublications(final MultiMap matchingPublications) {
		this.receive(new Event("batchRemotePublications") {
			public void process(Object myself) {
				Set topics = matchingPublications.keySet();
				Logging.VirtualMachine_LOG.debug("batchRemotePublications: incoming topics = "+topics+" ("+topics.size()+" items)");
				// for each topic in the map
				for (Iterator iter = topics.iterator(); iter.hasNext();) {
					try {
						Packet serializedTopic = (Packet) iter.next();
						ATTypeTag unserializedTopic = serializedTopic.unpack().asTypeTag();
						Set matchingServices = (Set) matchingPublications.get(serializedTopic);
						Logging.VirtualMachine_LOG.debug("matchingPublications.get("+serializedTopic+") = "+matchingServices);
						// for each serialized object exported under the topic
						for (Iterator iterator = matchingServices.iterator(); iterator.hasNext();) {
							Packet serializedService = (Packet) iterator.next();
							ATObject unserializedService = serializedService.unpack();
							Logging.VirtualMachine_LOG.debug("notifyOfExternalPublication("+unserializedTopic+","+unserializedService+")");
							discoveryManager_.notifyOfExternalPublication(unserializedTopic, unserializedService);
						}
					} catch (InterpreterException e) {
						Logging.VirtualMachine_LOG.error("error while unserializing remote published service",e);
					}
				}	
			}
		});
	}
	
	
    /**
     * Received in response to the CMDRequireService command of a remote VM
     * 
     * TODO: perhaps transform this into a sync_event and let CMDRequireService perform the reply
     */
	public void event_remoteSubscription(final Packet serializedRequiredTopic, final Address replyTo) {
		this.receive(new Event("remoteSubscription("+serializedRequiredTopic+")") {
			public void process(Object myself) {
				try {
					ATTypeTag requiredTopic = serializedRequiredTopic.unpack().asTypeTag();
					// query local discoverymanager for matching topic
			    	Set matchingServices = discoveryManager_.getLocalPublishedServicesMatching(requiredTopic);
			    	Logging.VirtualMachine_LOG.debug("getLocalPubServMatching("+requiredTopic+") = "+matchingServices+" ("+matchingServices.size()+" items)");
					if (!matchingServices.isEmpty()) {
						// maps serialized topics to sets of serialized objects that are published under this topic
						MultiMap matchingTopics = new MultiMap();
						matchingTopics.putValues(serializedRequiredTopic, matchingServices);
			    		// send all matching topics back to the requestor
			    		new CMDJoinServices(matchingTopics).send(host_.communicationBus_, replyTo);	
			    	}
				} catch (InterpreterException e) {
					Logging.VirtualMachine_LOG.error("error while unserializing remote subscription topic",e);
				}
			}
		});
	}
	
	
    /**
     * When a new VM has been discovered, the discovery agent is responsible for sending
     * all outstanding subscription topics to that VM, such that it can be checked whether
     * the newcomer has some publications that can resolve outstanding requests.
     */
	public void event_sendAllSubscriptionsTo(final Address newMember) {
		this.receive(new Event("sendAllSubscriptionsTo("+newMember+")") {
			public void process(Object myself) {
				// check if this VM has some outstanding subscriptions
				Set subscriptionTopics = discoveryManager_.getAllLocalSubscriptionTopics();
				Logging.VirtualMachine_LOG.debug("getAllLocalSubTopics() ="+subscriptionTopics+" ("+subscriptionTopics.size()+" items)");
				// only send a discovery query if this VM requires some services
				if (!subscriptionTopics.isEmpty()) {
					// send a discovery query message to the remote VM
					new CMDInitRequireServices(subscriptionTopics).send(host_.communicationBus_, newMember);
				}
			}
		});
	}
	
    /**
     * When a VM is discovered by another VM, that VM can send its outstanding subscriptions
     * to this VM. This event is received by an incoming CMDInitRequireServices command.
     * The local discovery manager should, for each incoming subscription topic, assemble all matching
     * local publication objects. A map of topic -> Set of publication objects is then returned
     * to the sender VM.
     * 
     * @param subscriptionTopics - a Set of Packet objects representing serialized ATTypeTag topics
     */
	public void event_receiveNewSubscriptionsFrom(final Set subscriptionTopics, final Address fromMember) {
		this.receive(new Event("receiveNewSubscriptionsFrom("+fromMember+")") {
			public void process(Object myself) {
				// maps topics to sets of objects that are published under this topic
				MultiMap matchingTopics = new MultiMap();
				
				// query local discoverymanager for matching topics
		    	for (Iterator iter = subscriptionTopics.iterator(); iter.hasNext();) {
					try {
						Packet serializedTopic = (Packet) iter.next();
						ATTypeTag topic = serializedTopic.unpack().asTypeTag();
						Set matchingServices = discoveryManager_.getLocalPublishedServicesMatching(topic);
						Logging.VirtualMachine_LOG.debug("getLocalPubServMatching("+topic+") ="+matchingServices+" ("+matchingServices.size()+" items)");
						if (!matchingServices.isEmpty()) {
							matchingTopics.putValues(serializedTopic, matchingServices);
						}
					} catch (InterpreterException e) {
						Logging.VirtualMachine_LOG.error("error while unserializing remote subscription topic",e);
					}
				}
				
		    	if (!matchingTopics.isEmpty()) {
		    		// send all matching topics back to the requestor
		    		new CMDJoinServices(matchingTopics).send(host_.communicationBus_, fromMember);	
		    	}
			}
		});
	}

	public Publication[] sync_event_listPublications(final ELActor actor) throws InterpreterException {
		try {
			return (Publication[]) this.receiveAndWait("currentPublications("+actor+")", new Callable() {
				public Object call(Object argument) {
					return discoveryManager_.listPublications(actor);
				}
			});
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}

	public Subscription[] sync_event_listSubscriptions(final ELActor actor) throws InterpreterException {
		try {
			return (Subscription[]) this.receiveAndWait("currentPublications("+actor+")", new Callable() {
				public Object call(Object argument) {
					return discoveryManager_.listSubscriptions(actor);
				}
			});
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
}
