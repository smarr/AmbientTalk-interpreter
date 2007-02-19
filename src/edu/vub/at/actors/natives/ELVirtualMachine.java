/**
 * AmbientTalk/2 Project
 * ELVirtualMachine.java created on Nov 1, 2006 at 8:32:31 PM
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

import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.GUID;
import edu.vub.at.actors.net.MembershipNotifier;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATStripe;

import java.io.File;
import java.util.Hashtable;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;

/**
 * A ELVirtualMachine represents a virtual machine which hosts several actors. The 
 * virtual machine is in charge of creating connections with other virtual machines 
 * and orchestrates the broadcasting of its presence, the exchange of service 
 * descriptions and messages. It also contains a set of runtime parameters (such as
 * the objectpath and initfile) which are needed to initialise a new actor.
 *
 * @author smostinc
 */
public final class ELVirtualMachine extends EventLoop implements RequestHandler {
	
	public static final ELVirtualMachine currentVM() {
		return ELActor.currentActor().getHost();
	}
	
	/** startup parameter to the VM: which directories to include in the lobby */
	private final File[] objectPathRoots_;
	
	/** startup parameter to the VM: the code of the init.at file to use */
	private final ATAbstractGrammar initialisationCode_;
	
	/** the GUID of this VM */
	private final GUID vmId_;

	/** the JGroups Address of the VM */
	private Address vmAddress_;
	
	/** a table mapping actor IDs to local native actors (int -> ELActor) */
	private final Hashtable localActors_;
	
	/** manages subscriptions and publications */
	private final DiscoveryManager discoveryManager_;
	
	/** the JGroups communication bus for this Virtual Machine */
	protected MessageDispatcher messageDispatcher_;
	
	/** the JGroups discovery bus for this Virtual Machine */
	protected MembershipNotifier membershipNotifier_;
	
	public ELVirtualMachine(File[] objectPathRoots, ATAbstractGrammar initCode) {
		super("virtual machine");
		objectPathRoots_ = objectPathRoots;
		initialisationCode_ = initCode;
		vmId_ = new GUID();
		localActors_ = new Hashtable();
		discoveryManager_ = new DiscoveryManager(this);
		membershipNotifier_ = new MembershipNotifier(discoveryManager_);
		
		// initialize the message dispatcher using a JChannel
		this.event_init();
	}
	
	public GUID getGUID() { return vmId_; }
	
	public ATAbstractGrammar getInitialisationCode() {
		return initialisationCode_;
	}

	public File[] getObjectPathRoots() {
		return objectPathRoots_;
	}
	
	public ELVirtualMachine getHost() { return this; }

	/**
	 * An event loop handles events by dispatching to the event itself.
	 * Not to be confused with this class' handle(Message) method, which
	 * enables the VM event loop to handle incoming JGroups Messages!
	 */
	public void handle(Event event) {
		// make the event process itself
		event.process(this);
	}
	
	public ELActor getActor(int id) {
		ELActor entry = (ELActor) localActors_.get(new Integer(id));
		if (entry != null) {
			return entry;
		} else {
			throw new RuntimeException("Asked for unknown actor id: " + id);
		}
	}
	
	public Address getAddress() {
		return vmAddress_;
	}
	
	/* ==========================
	 * == Actor -> VM Protocol ==
	 * ========================== */
	
	// All methods prefixed by event_ denote asynchronous message sends that will be
	// scheduled in the receiving event loop's event queue
	
	/**
	 * Event that signals the creation of a new actor on this virtual machine.
	 */
	public void event_actorCreated(final ELActor actor) {
		this.receive(new Event("actorCreated("+actor+")") {
			public void process(Object myself) {
				localActors_.put(new Integer(actor.hashCode()), actor);
			}
		});
	}
	
    /**
     * This event is fired whenever an object
     * is being offered as a service provide using the provide: language construct. 
     * The virtual machine keeps track of such services and is responsible for the 
     * matching between services and clients. When such matches are detected the VM
     * will send a foundResolution event to both involved actors. If the VM detects 
     * that one partner has become unavailable it will send the lostResolution event
     * 
     * @param topic - the abstract category in which this service is published
     * @param service - a far reference to object providing the service
     */
	public void event_servicePublished(final ATStripe topic, final NATFarReference service) {
		this.receive(new Event("servicePublished("+topic+","+service+")") {
			public void process(Object myself) {
				discoveryManager_.addPublication(topic, service);
			}
		});
	}
	
    /**
     * This event is fired whenever an object
     * requests a service using the require: language construct. The virtual machine 
     * keeps track of such requests and is responsible matching services and clients. 
     * When such matches are detected the VM will send a foundResolution event to both
     * involved actors. If the VM detects that one partner has become unavailable it 
     * will send the lostResolution event
     * 
     * @param topic - the abstract category in which this service is published
     * @param handler - a far reference to the closure acting as a callback upon discovery
     */
	public void event_clientSubscribed(final ATStripe topic, final NATFarReference handler) {
		this.receive(new Event("clientSubscribed("+topic+","+handler+")") {
			public void process(Object myself) {
				discoveryManager_.addSubscription(topic, handler);
			}
		});
	}

    /**
     * This event is fired whenever a service
     * offer is being revoked. In this case, the virtual machine ensures that the 
     * object is no longer discoverable to new clients. However, it will not send 
     * lostResolution events as these signal that an object has become unreachable.
     * 
     * @param topic - the abstract category in which this service is published
     * @param service - a far reference to object providing the service
     */
	public void event_cancelPublication(final ATStripe topic, final NATFarReference service) {
		this.receive(new Event("cancelPublication("+topic+","+service+")") {
			public void process(Object myself) {
				discoveryManager_.deletePublication(topic, service);
			}
		});
	}

    /**
     * This event is fired whenever a service
     * request is being revoked. In this case, the virtual machine ensures that the 
     * object will no longer discover new services. However, it will not send 
     * lostResolution events as these signal that the client has become unreachable.
     * 
     * @param topic - the abstract category in which this service is published
     * @param handler - a far reference to the closure acting as a callback upon discovery
     */
	public void event_cancelSubscription(final ATStripe topic, final NATFarReference handler) {
		this.receive(new Event("cancelSubscription("+topic+","+handler+")") {
			public void process(Object myself) {
				discoveryManager_.deleteSubscription(topic, handler);
			}
		});
	}
	
	/* ============================
	 * == JGroups -> VM Protocol ==
	 * ============================ */
	
	public Object handle(Message message) {
		return sync_event_handle(message);
	}
	
	public Object sync_event_handle(final Message message) {
		try {
			return this.receiveAndWait(
					"Handling a remote message",
					new Callable() {
						public Object call(Object argument) throws Exception {
							// receiving the message [DEST, PACKET]
							Packet serializedForm = (Packet)message.getObject();
							
							// localActors_[DEST].event_accept(PACKET)
							ELActor processor = getActor(serializedForm.getDestination());
                            processor.event_accept(serializedForm);
							return null; // null return value means OK
						};
					});
		} catch (Exception exception) {
			return exception;
		}
	}
	
	/** the protocol stack underlying the JGroups JChannel */
	private static final String _JGROUPS_PROTOCOL_ = null;
	/** the name of the multicast group joined by all AmbientTalk VMs */
	private static final String _GROUP_NAME_ = "AmbientTalk";
	
	public void event_init() {
		receive(new Event("init("+this+")") {
			public void process(Object byMyself) {
				try {
					ELVirtualMachine processor = (ELVirtualMachine)byMyself;
					
					Channel channel = new JChannel(_JGROUPS_PROTOCOL_);
					
					processor.messageDispatcher_ = new MessageDispatcher(
							channel,
							null, // the MessageListener
							processor.membershipNotifier_,  // the MembershipListener
							processor,  // the RequestHandler
							false, // deadlock detection is disabled
							true); // concurrent processing is enabled
					channel.connect(_GROUP_NAME_);
					
					// don't receive my own messages 
					channel.setOpt(JChannel.LOCAL, Boolean.FALSE);
					
					channel.addChannelListener(new ChannelListener() {
						public void channelClosed(Channel c) {
							System.err.println("Channel closed: " +c);
						}
						public void channelConnected(Channel c) {
							System.err.println("Channel connected: " +c);

						}
						public void channelDisconnected(Channel c) {
							System.err.println("Channel disconnected: " +c);

						}
						public void channelReconnected(Address a) {
							System.err.println("Channel reconnected: " +a);
						}
						public void channelShunned() {}
					});
					
					System.err.println(this + ": successfully created connection");
				} catch (Exception e) {
					// TODO ???
					System.err.println(this + ": error creating connection:");
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * Sent by the discovery manager whenever a discovery query has to be sent to
	 * a remote VM when that remote VM has joined the network.
	 * @param remoteVM - the address of the remote VM to contact
	 * @param outstandingTopics - the topics in which this VM is interested (in serialized form)
	 */
	public void event_sendDiscoveryQuery(Address remoteVM, byte[] outstandingTopics) {
		receive(new Event("sendDiscoveryQuery("+remoteVM+")") {
			public void process(Object byMyself) {
				
			}
		});
	}
}
