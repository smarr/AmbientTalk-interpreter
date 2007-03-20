package edu.vub.at.actors.net.cmd;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.MessageDispatcher;

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.Logging;

/**
 * 
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
	
	public void send(MessageDispatcher dispatcher, Address recipient) {
		try {
			super.sendAsyncUnicast(dispatcher, recipient);
		} catch (TimeoutException e) {
			Logging.VirtualMachine_LOG.warn(this + ": timeout while trying to send the objectId of a new offline object");
		} catch (SuspectedException e) {
			Logging.VirtualMachine_LOG.warn(this + ": remote VM suspected while sending new offline object");
		}
	}
	
	public void broadcast(MessageDispatcher dispatcher) {
		try {
			super.sendAsyncMulticast(dispatcher);
		} catch (TimeoutException e) {
			Logging.VirtualMachine_LOG.warn(this + ": timeout while trying to broadcast the objectId of a new offline object");
		} catch (SuspectedException e) {
			Logging.VirtualMachine_LOG.warn(this + ": remote VM suspected while broadcasting new offline object");
		}
	}
	
	public Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception {
		
		remoteHost.membershipNotifier_.notifyObjectExpired(senderObjectId_);
		return null;
	}
}
