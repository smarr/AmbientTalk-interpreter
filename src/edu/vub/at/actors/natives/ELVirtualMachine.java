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

import java.net.URL;
import java.util.Hashtable;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;

import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.id.ActorID;
import edu.vub.at.actors.id.VirtualMachineID;
import edu.vub.at.actors.net.DiscoveryListener;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.actors.net.MembershipNotifier;
import edu.vub.at.actors.net.VMAddressBook;
import edu.vub.at.actors.net.cmd.CMDHandshake;
import edu.vub.at.actors.net.cmd.CMDObjectTakenOffline;
import edu.vub.at.actors.net.cmd.VMCommand;
import edu.vub.at.objects.ATAbstractGrammar;

/**
 * A ELVirtualMachine represents a virtual machine which hosts several actors. The 
 * virtual machine is in charge of creating connections with other virtual machines 
 * and orchestrates the broadcasting of its presence, the exchange of service 
 * descriptions and messages. It also contains a set of runtime parameters (such as
 * the objectpath and initfile) which are needed to initialise a new actor.
 *
 * TODO: use pure JChannel to send pure async messages rather than using a MessageDispatcher?
 * Or use dispatcher only for message transmission.
 *
 * @author tvcutsem
 * @author smostinc
 */
public final class ELVirtualMachine extends EventLoop implements RequestHandler, DiscoveryListener {
	
	
	/** the name of the multicast group joined by all AmbientTalk VMs */
	private static final String _GROUP_NAME_ = "AmbientTalk";
		
	/** startup parameter to the VM: the code of the init.at file to use */
	private final ATAbstractGrammar initialisationCode_;
	
	/** startup parameter to the VM: the list of fields to be initialized in every hosted actor */
	private final SharedActorField[] sharedFields_;
	
	/** the VirtualMachineID of this VM */
	private final VirtualMachineID vmId_;

	/** the JGroups Address of the VM */
	private Address vmAddress_;
	
	/**
	 * A table mapping VM GUIDs to Address objects.
	 * Each time a VM connects, it sends its VirtualMachineID and an entry
	 * mapping that VirtualMachineID to its current Address is registered in this table. When a remote reference
	 * needs to send a message to the remote object, the VM is contacted based on its VirtualMachineID and this
	 * table. When a VM disconnects, the disconnecting address is removed from this table. 
	 */
	public final VMAddressBook vmAddressBook_;
	
	/** a table mapping actor IDs to local native actors (int -> ELActor) */
	private final Hashtable localActors_;
	
	/** the JGroups communication bus for this Virtual Machine */
	public MessageDispatcher messageDispatcher_;
	
	/** the JGroups discovery bus for this Virtual Machine */
	public MembershipNotifier membershipNotifier_;
	
	/** the actor responsible for hosting the publications and subscriptions of this VM's actors */
	public final ELDiscoveryActor discoveryActor_;
	
	public ELVirtualMachine(ATAbstractGrammar initCode, SharedActorField[] fields) {
		super("virtual machine");
		
		// used to initialize actors
		initialisationCode_ = initCode;
		sharedFields_ = fields;
		
		// used to allow actors to send messages to remote vms/actors
		vmAddressBook_ = new VMAddressBook();

		vmId_ = new VirtualMachineID();
		localActors_ = new Hashtable();
		discoveryActor_ = new ELDiscoveryActor(this);
		
		Logging.VirtualMachine_LOG.info(this + ": VM created, initializing network connection");
		
		// initialize the message dispatcher using a JChannel
		initializeNetwork();
	}
	
	public static final ELVirtualMachine currentVM() {
		return ELActor.currentActor().getHost();
	}
		
	public VirtualMachineID getGUID() { return vmId_; }
		
	public ATAbstractGrammar getInitialisationCode() {
		return initialisationCode_;
	}

	public SharedActorField[] getFieldsToInitialize() {
		return sharedFields_;
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
	
	/**
	 * returns the local actor corresponding to the given actor Id.
	 * This method synchronizes on the localActors_ table to ensure that
	 * insertion and lookup are properly synchronized.
	 */
	public ELActor getActor(ActorID id) {
		ELActor entry;
		synchronized (localActors_) {
			entry = (ELActor) localActors_.get(id);
		}
		if (entry != null) {
			return entry;
		} else {
			throw new RuntimeException("Asked for unknown actor id: " + id);
		}
	}
	
	public Address getLocalVMAddress() {
		return vmAddress_;
	}
	
	/**
	 * Signals the creation of a new actor on this virtual machine.
	 */
	public void actorCreated(ELActor actor) {
		// lock the localActors_ table first to ensure addition is
		// atomic w.r.t. lookup in getActor
		synchronized (localActors_) {
			localActors_.put(actor.getActorID(), actor);
		}
	}
	
	/* =================================
	 * == DiscoveryListener interface ==
	 * ================================= */
	
	public void memberJoined(Address virtualMachine) {
		this.event_memberJoined(virtualMachine);
	}
	
	public void memberLeft(Address virtualMachine) {
		this.event_memberLeft(virtualMachine);
	}
	
	
	/**
	 * Signals that this VM can connect to the underlying network channel
	 * and can start distributed interaction.
	 */
	public void event_goOnline() {
		this.receive(new Event("goOnline") {
			public void process(Object myself) {
				try {
					Channel channel = messageDispatcher_.getChannel();
					channel.connect(_GROUP_NAME_);
					vmAddress_ = channel.getLocalAddress();
					Logging.VirtualMachine_LOG.info(this + ": interpreter online, address = " + vmAddress_);
				} catch (Exception e) {
					Logging.VirtualMachine_LOG.fatal(this + ": could not connect to network:", e);
				}
			}
		});
	}
	
	/**
	 * Signals that this VM must disconnect from the underlying network channel
	 */
	public void event_goOffline() {
		this.receive(new Event("goOffline") {
			public void process(Object myself) {
				try {
					Channel channel = messageDispatcher_.getChannel();
					channel.disconnect();
					vmAddress_ = null;
					membershipNotifier_.channelDisconnected();
					Logging.VirtualMachine_LOG.info(this + ": interpreter offline");
				} catch (Exception e) {
					Logging.VirtualMachine_LOG.fatal(this + ": error while going offline:", e);
				}
			}
		});
	}
	
	/**
	 * Notifies the discovery manager that a VM has joined the network.
	 * This VM may be a first-time participant or it may be a previously
	 * disconnected VM that has become reconnected.
	 * 
	 * The VM asks the newly joined VM whether it has any
	 * services that match the type of an outstanding subscription on this VM.
	 */
	public void event_memberJoined(final Address remoteVMAddress) {
		this.receive(new Event("memberJoined("+remoteVMAddress+")") {
			public void process(Object myself) {
				// if this VM is no longer connected, ignore the memberJoined event
				// as there is nothing useful that can be done in response anyway
				if(vmAddress_ == null)
					return;
				
				// filter out discovery of myself
				if (!vmAddress_.equals(remoteVMAddress)) {
					Logging.VirtualMachine_LOG.info(this + ": VM connected: " + remoteVMAddress);
					
					// send a handshake message to exchange IDs
					new CMDHandshake(vmId_).send(messageDispatcher_, remoteVMAddress);
					
			
				}
			}
		});
	}
	
	public void event_memberLeft(final Address virtualMachine) {
		this.receive(new Event("memberLeft("+virtualMachine+")") {
			public void process(Object myself) {
				Logging.VirtualMachine_LOG.info(this + ": VM disconnected: " + virtualMachine);
				
				// Identify the VirtualMachineID that corresponds to this address
				VirtualMachineID disconnected = vmAddressBook_.getGUIDOf(virtualMachine);
				
				// disconnected may be null if the memberJoined event was ignored because this VM 
				// was already offline when the event was beiing processed.
				if(disconnected != null) {
					// delete entries mapping to Address from the vm Address Book table first, 
					// so sending threads may have 'premonitions' that they are no longer connected
					vmAddressBook_.removeEntry(virtualMachine);
					
					// properly (but synchronously) notify all remote references of a disconnection 
					membershipNotifier_.notifyDisconnected(disconnected);
				}

			}
		});
	}
	
	
	/* ==========================
	 * == Actor -> VM Protocol ==
	 * ========================== */
	
	// All methods prefixed by event_ denote asynchronous message sends that will be
	// scheduled in the receiving event loop's event queue
	
	/**
	 * Event that signals the deletion of an object from the export table of an actor on this virtual machine.
	 */
	public void event_objectTakenOffline(final ATObjectID objId, final Address receiver) {
		 this.receive( new Event("objectTakenOffline(" + objId +")") {
			 public void process(Object myself){
				 if ( receiver == null){
					 //broadcast to other virtual machines that an object has gone offline.
					 new CMDObjectTakenOffline(objId).broadcast(messageDispatcher_);
				 } else{
					 //sending to a known virtual machine in response to an XObjectOffline exception.
					 new CMDObjectTakenOffline(objId).send(messageDispatcher_, receiver);
				 }
				 
			 }
		 });
	}
	
	/* ============================
	 * == JGroups -> VM Protocol ==
	 * ============================ */
	
	public Object handle(Message message) {
		try {
            // receiving a VM command object
		    VMCommand cmd = (VMCommand) message.getObject();
		    
		    Logging.VirtualMachine_LOG.info("handling incoming command: " + cmd);
		    
			// allow the command to execute itself
		    return cmd.uponReceiptBy(this, message);
		} catch (Exception exception) {
			return exception;
		}
	}
	
	private void initializeNetwork() {
		try {
			// load the protocol stack to be used by JGroups
			URL protocol = MembershipNotifier.class.getResource("jgroups-protocol.xml");

			Channel channel = new JChannel(protocol);

			membershipNotifier_ = new MembershipNotifier(this);
			messageDispatcher_ = new MessageDispatcher(
					channel,
					null, // the MessageListener
					membershipNotifier_,  // the MembershipListener
					this,  // the RequestHandler
					false, // deadlock detection is disabled
					true); // concurrent processing is enabled

			// don't receive my own messages 
			channel.setOpt(JChannel.LOCAL, Boolean.FALSE);

			channel.addChannelListener(new ChannelListener() {
				public void channelClosed(Channel c) {
					Logging.VirtualMachine_LOG.warn(this + ": channel closed: " + c);
				}
				public void channelConnected(Channel c) {
					Logging.VirtualMachine_LOG.warn(this + ": channel connected: " + c);
				}
				public void channelDisconnected(Channel c) {
					Logging.VirtualMachine_LOG.warn(this + ": channel disconnected: " + c);
				}
				public void channelReconnected(Address a) {
					Logging.VirtualMachine_LOG.warn(this + ": channel reconnected. Address = " + a);
					vmAddress_ = a;
				}
				public void channelShunned() {
					Logging.VirtualMachine_LOG.warn(this + ": channel shunned");
				}
			});
		} catch (ChannelException e) {
			Logging.VirtualMachine_LOG.fatal(this + ": could not initialize network connection: ", e);
		}
	}
	
}
