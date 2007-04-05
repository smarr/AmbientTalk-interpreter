package edu.vub.at.actors.net.cmd;

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.actors.net.comm.CommunicationBus;

/**
 * A CMDObjectTakenOffline command is sent asynchronously to all connected VMs when 
 * an actor on the sender VM takes one of its objects offline. Connected VMs are notified
 * of this event so that they can check if they had a far remote reference to the offline object.
 * 
 * SENDER: the VM that takes an object offline
 * RECEIVER: all connected VMs 
 * MODE: ASYNCHRONOUS, MULTICAST
 * PROPERTIES: objectID of the offline object
 * REPLY: none
 * 
 * @author egonzale
 */
public class CMDObjectTakenOffline extends VMCommand{
	
	private final ATObjectID senderObjectId_;
	
	public CMDObjectTakenOffline(ATObjectID senderObjectId) {
		super("objectTakenOffline");
		senderObjectId_ = senderObjectId;
	}
	
	public void send(CommunicationBus dispatcher, Address recipient) {
		dispatcher.sendAsyncUnicast(this, recipient);
	}
	
	public void broadcast(CommunicationBus dispatcher) {
		dispatcher.sendAsyncMulticast(this);
	}
	
	public void uponReceiptBy(ELVirtualMachine remoteHost, Address senderAddress) {
		remoteHost.connectionManager_.notifyObjectExpired(senderObjectId_);
	}
}
