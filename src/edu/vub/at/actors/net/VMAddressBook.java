/**
 * AmbientTalk/2 Project
 * Actorscript.java created on 2-mrt-2007 at 11:14:27
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
package edu.vub.at.actors.net;

import java.util.Hashtable;

import org.jgroups.Address;

import edu.vub.at.actors.id.GUID;

/**
 * 
 * The VMAddressBook encapsulates the bi-directional mapping:
 *  AmbientTalk VM GUID  -->  JGroups Address
 *  JGroups Address      -->  AmbientTalk VM GUID
 *
 * It is necessary to maintain such a mapping to correctly identify and restore
 * the connection between AmbientTalk VMs. A JGroups address cannot be used as
 * a good identification key because an AmbientTalk VM may be known under different
 * addresses as it goes offline and online.
 * 
 * When a new VM joins, after handshaking, an entry is added to the address book.
 * When a VM disjoins, the entry corresponding to the address that went offline is removed.
 * 
 * @author elgonzal
 * @author tvcutsem
 */
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
	 * Resolve a concrete network address to a remote VM's unique identifier.
	 */
	public synchronized GUID getGUIDOf(Address vmAddress) {
			return (GUID) addressToGuid_.get(vmAddress);
//			if (g == null) {
//				Logging.VirtualMachine_LOG.error("Asked for the GUID of an unknown vmAddress: " + vmAddress);
//				throw new RuntimeException("Asked for the GUID of an unknown vmAddress: " + vmAddress);
//			}
//			return g;
	}
	
	

}
