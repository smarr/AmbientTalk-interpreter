package edu.vub.at.actors.net;

import java.util.Hashtable;

import org.jgroups.Address;

import edu.vub.at.actors.id.GUID;

public class VMAddressBook {
	
	private final Hashtable guidToAddress_;
	private final Hashtable addressToGuid_;
	
	public VMAddressBook(){
		guidToAddress_ = new Hashtable();
		addressToGuid_ = new Hashtable();
	}
	
	public synchronized void addEntry( GUID vmId, Address vmAddress ){
		guidToAddress_.put(vmId, vmAddress);
		addressToGuid_.put(vmAddress, vmId);
	}

	/**
	 * Remove all entries that map to this VM address from the VM Address book.
	 */
	public synchronized void removeEntry(Address vmAddress ){
		
		GUID vmId = (GUID) addressToGuid_.get(vmAddress);
		Logging.VirtualMachine_LOG.debug("Removed VM binding " + vmAddress + " -> " + vmId);

		guidToAddress_.remove(vmId);
		addressToGuid_.remove(vmAddress);
	}
	
	
	/**
	 * Resolve a remote VM's unique identifier to a concrete network address.
	 */
	public synchronized Address getAddressOf(GUID vmId) {
			Address a = (Address) guidToAddress_.get(vmId);
			if (a == null) {
				Logging.VirtualMachine_LOG.error("Asked for the address of an unknown vmId: " + vmId);
				throw new RuntimeException("Asked for the address of an unknown vmId: " + vmId);
			}
			return a;
	}

	/**
	 * Resolve a remote VM's unique identifier to a concrete network address.
	 */
	public synchronized GUID getGUIDOf(Address vmAddress) {
			GUID g = (GUID) addressToGuid_.get(vmAddress);
			if (g == null) {
				Logging.VirtualMachine_LOG.error("Asked for the GUID of an unknown vmAddress: " + vmAddress);
				throw new RuntimeException("Asked for the GUID of an unknown vmAddress: " + vmAddress);
			}
			return g;
	}
	
	

}
