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

import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.id.ActorID;
import edu.vub.at.actors.id.VirtualMachineID;
import edu.vub.at.actors.natives.DiscoveryManager.Subscription;
import edu.vub.at.actors.net.ConnectionListenerManager;
import edu.vub.at.actors.net.VMAddressBook;
import edu.vub.at.actors.net.cmd.CMDHandshake;
import edu.vub.at.actors.net.cmd.CMDObjectTakenOffline;
import edu.vub.at.actors.net.cmd.CMDObjectDisconnected;
import edu.vub.at.actors.net.cmd.CMDObjectReconnected;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.actors.net.comm.CommunicationBus;
import edu.vub.at.actors.net.comm.NetworkException;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.util.logging.Logging;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * A ELVirtualMachine represents a virtual machine which hosts several actors. The 
 * virtual machine is in charge of creating connections with other virtual machines 
 * and orchestrates the broadcasting of its presence, the exchange of service 
 * descriptions and messages. It also contains a set of runtime parameters (such as
 * the objectpath and initfile) which are needed to initialise a new actor.
 *
 * @author tvcutsem
 * @author smostinc
 */
public final class ELVirtualMachine extends EventLoop {
	
	public static final String _DEFAULT_GROUP_NAME_ = "AmbientTalk";
	public static final String _DEFAULT_IP_ADDRESS_ = "0.0.0.0";
	private static final String _ENV_AT_STACK_SIZE_ = "AT_STACK_SIZE";
		
	/** startup parameter to the VM: the code of the init.at file to use */
	private ATAbstractGrammar initialisationCode_;
	
	/** startup parameter to the VM: the list of fields to be initialized in every hosted actor */
	private final SharedActorField[] sharedFields_;
	
	/** the VirtualMachineID of this VM */
	private final VirtualMachineID vmId_;
	
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
	
	/** the communication bus for this Virtual Machine */
	public final CommunicationBus communicationBus_;
	
	/** manager for disconnection and reconnection observers */
	public final ConnectionListenerManager connectionManager_;
	
	/** the actor responsible for hosting the publications and subscriptions of this VM's actors */
	public final ELDiscoveryActor discoveryActor_;
	
	public final FarReferencesThreadPool farReferencesThreadPool_;

	/**
	 * Construct a new AmbientTalk virtual machine where...
	 * @param initCode is the code to be executed in each new created actor (the content of the init.at file)
	 * @param fields are all of the fields that should be present in each new created actor (e.g. the 'system' object of IAT)
	 * @param groupName is the name of the overlay network to join
	 */
	public ELVirtualMachine(ATAbstractGrammar initCode, SharedActorField[] fields, String groupName, String ipAddress) {
		super("virtual machine");
		this.start();
		
		// used to initialize actors
		initialisationCode_ = initCode;
		sharedFields_ = fields;
		
		// used to allow actors to send messages to remote vms/actors
		vmAddressBook_ = new VMAddressBook();

		vmId_ = new VirtualMachineID();
		localActors_ = new Hashtable();
		discoveryActor_ = new ELDiscoveryActor(this);
		// Initilization of the actor mirror must be done before the initialization of init.at file in the actor!
		discoveryActor_.initializeActorMirror();
		localActors_.put(discoveryActor_.getActorID(), discoveryActor_);
		discoveryActor_.event_init();
		
		// initialize the message dispatcher using a JChannel
		connectionManager_ = new ConnectionListenerManager();
		communicationBus_ = new CommunicationBus(this, groupName, ipAddress);
		
		farReferencesThreadPool_ = new FarReferencesThreadPool(this);
		
		Logging.VirtualMachine_LOG.info(this + ": VM created on network " + groupName);
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
	
	/**
	 * Signals that this VM can connect to the underlying network channel
	 * and can start distributed interaction.
	 */
	public void event_goOnline() {
		this.receive(new Event("goOnline") {
			public void process(Object myself) {
				try {
					Address myAddress = communicationBus_.connect();
					Logging.VirtualMachine_LOG.info(this + ": interpreter online, address = " + myAddress);
				} catch (NetworkException e) {
					Logging.VirtualMachine_LOG.fatal(this + ": could not connect to network:", e);
				}
			}
		});
	}
	
	/**
	 * Signals that this VM must disconnect from the underlying discovery channel and communication bus
	 */
	public void event_goOffline() {
		this.receive(new Event("goOffline") {
			public void process(Object myself) {
				try {
					communicationBus_.disconnect();
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
	 * This VM will handshake with the connected VM to exchange their actual
	 * {@link VirtualMachineID}s rather than their network addresses.
	 */
	public void event_memberJoined(final Address remoteVMAddress) {
		this.receive(new Event("memberJoined("+remoteVMAddress+")") {
			public void process(Object myself) {
				Logging.VirtualMachine_LOG.info(this + ": VM connected: " + remoteVMAddress);
				// send a handshake message to exchange IDs
				new CMDHandshake(vmId_).send(communicationBus_, remoteVMAddress);
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
				// was already offline when the event was being processed.
				if(disconnected != null) {
					// delete entries mapping to Address from the vm Address Book table first, 
					// so sending threads may have 'premonitions' that they are no longer connected
					vmAddressBook_.removeEntry(virtualMachine);
					
					// properly (but synchronously) notify all remote references of a disconnection 
					connectionManager_.notifyDisconnected(disconnected);
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
	 * Event that signals the deletion of an object from the export table of an
	 * actor on this virtual machine.
	 */
	public void event_objectTakenOffline(final ATObjectID objId, final Address receiver) {
		 this.receive( new Event("objectTakenOffline(" + objId +")") {
			 public void process(Object myself){
				 if ( receiver == null){
					 //notify myself in case local remote references in this machine register a listener
					 connectionManager_.notifyObjectTakenOffline(objId);
					 //broadcast to other virtual machines that an object has gone offline.
					 new CMDObjectTakenOffline(objId).broadcast(communicationBus_);
				 } else{
					 //sending to a known virtual machine in response to an XObjectOffline exception.
					 new CMDObjectTakenOffline(objId).send(communicationBus_, receiver);
				 }
				 
			 }
		 });
	}
	
	/**
	 * Event that signals the manual disconnect of a previously exported and 
	 * object on this VM.
	 */
	public void event_objectDisconnected(final ATObjectID objId) {
		 this.receive( new Event("objectDisconnected(" + objId +")") {
			 public void process(Object myself){
				 //notify myself in case local remote references in this machine register a listener
				 connectionManager_.notifyObjectDisconnected(objId);
				 //broadcast to other virtual machines that an object has disconnected.
				 new CMDObjectDisconnected(objId).broadcast(communicationBus_);
			 }
		 });
	}
	
	/**
	 * Event that signals the manual reconnect of a previously exported and 
	 * disconnected object on this VM.
	 */
	public void event_objectReconnected(final ATObjectID objId) {
		 this.receive( new Event("objectReconnected(" + objId +")") {
			 public void process(Object myself){
				//notify myself in case local remote references in this machine register a listener
				connectionManager_.notifyObjectReconnected(objId);
				//broadcast to other virtual machines that an object has reconnected.
				new CMDObjectReconnected(objId).broadcast(communicationBus_); 
			 }
		 });
	}

	/**
	 * Auxiliary creation method to create an actor with an empty behaviour.
	 * Equivalent to evaluating:
	 * 
	 * actor: { nil }
	 */
	public NATLocalFarRef createEmptyActor() throws InterpreterException {
		Packet noParams = new Packet(NATTable.EMPTY);
		Packet noinitcode = new Packet(new NATMethod(Evaluator._ANON_MTH_NAM_, NATTable.EMPTY, new AGBegin(NATTable.of(Evaluator.getNil())), NATTable.EMPTY));
		return createActor(noParams, noinitcode);
	}

	/**
	 * Creates a new actor on this Virtual Machine. The actor its behaviour
	 * is intialized by means of the passed parameters and initialization code. The calling
	 * thread is **blocked** until the actor has been constructed. However, actor behaviour
	 * and root initialization is carried out by the newly created actor itself.
	 * 
	 * @param parametersPkt the serialized parameters used to invoke the initialization code
	 * @param initcodePkt the serialized initialization code used to initialize the actor behaviour
	 * @param actorMirror this actor's mirror
	 * @return a far reference to the behaviour of the actor
	 * @throws InterpreterException
	 */
	public NATLocalFarRef createActor(Packet parametersPkt,
			                          Packet initcodePkt) throws InterpreterException {
		
		BlockingFuture future = new BlockingFuture();
		NATActorMirror mirror = new NATActorMirror(this);
		ELActor processor;
		Integer stackSize = Integer.getInteger(_ENV_AT_STACK_SIZE_);
		if (stackSize != null) {
			processor = new ELActor(mirror, this, stackSize.intValue());
		} else{
			processor = new ELActor(mirror, this);
		}
		mirror.setActor(processor); // make mirror refer to the created actor
		
		// lock the localActors_ table first to ensure addition is
		// atomic w.r.t. lookup in getActor
		synchronized (localActors_) {
			localActors_.put(processor.getActorID(), processor);
		}
		
		// schedule special 'init' message which will:
		// A) create a new behaviour and will unblock creating actor (by passing it a far ref via the future)
		// B) unpack the parameters used to invoke the initialization code
		// C) unpack the init code to initialize the behaviour
		// D) initialize the root and lobby objects of this actor
		processor.event_init(future, parametersPkt, initcodePkt);
	    
		try {
			return (NATLocalFarRef) future.get();
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
	/**Does a soft reset of the virtual machine: 
	 * It removes the VM from the network, restarts data structures 
	 * (cleans the actors and discoveryActor) and puts it back online.
	 * 
	 * This event is synchronous because after its execution the user may need
	 * to create new actors and we should ensure that it happens in the new environment.
	 * Currently it is used from iat, and after the reset it re-initializes the evaluator.
	 * 
	 * @param initCode is the code to be executed in each new created actor (the content of the init.at file)
	 * @return nil if it succeeds.
	 * @throws Exception 
	 * @throws InterpreterException
	 */
	public ATObject sync_event_softReset(final ATAbstractGrammar initCode) throws Exception{	
		try {
			return (ATObject) this.receiveAndWait("reset", new Callable() {
				public Object call(Object argument) throws Exception {
					// disconnect VM from the network so that old actors 
					// d to this VM re-handshake.
					communicationBus_.disconnect();
					// go over all actors and make them stop processing, except for the discovery actor.
					ELDiscoveryActor discoveryActor;
					for (Enumeration e = localActors_.elements(); e.hasMoreElements();) {
						ELActor actor = (ELActor) e.nextElement();
						if (!(actor instanceof ELDiscoveryActor)){
							actor.stopProcessing();
						} else{
							discoveryActor = (ELDiscoveryActor) actor;
							// reset the tables of discoveryActor.
							discoveryActor.event_reset();
						}
					}
					//clear from the data structure all actors.
					localActors_.clear();
					// add the discovery actor back to the tables.
					localActors_.put(discoveryActor_.getActorID(), discoveryActor_);
					// reinitialize the init code used to initialize actors.
					initialisationCode_ = initCode;
					// reset the environment.
					Evaluator.resetEnvironment();
					// put the VM back online
					try {
						communicationBus_.connect();
					} catch (NetworkException e) {
						Logging.VirtualMachine_LOG.fatal(this + ": could not connect to network during reset: ", e);
						throw e;
					}
					Logging.VirtualMachine_LOG.info(this + ": interpreter reset sucessfully completed");
					return Evaluator.getNil();
				}
			});
		} catch (Exception e) {
			Logging.VirtualMachine_LOG.fatal(this + ": error while reseting:", e);
			throw e;
		}
	}
	
}
