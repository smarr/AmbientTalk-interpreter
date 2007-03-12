/**
 * AmbientTalk/2 Project
 * MembershipNotifier.java created on Feb 16, 2007 at 1:14:08 PM
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

import edu.vub.at.actors.id.GUID;
import edu.vub.util.MultiMap;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.ExtendedReceiverAdapter;
import org.jgroups.MembershipListener;
import org.jgroups.View;

/**
 * An instance of the class MembershipNotifier is registered with an instance of the JGroups
 * MessageDispatcher class as its MembershipListener. Whenever virtual machines connect or
 * disconnect from the multicast group, this object is notified. Its role is to propagate
 * these notifications to:
 *  A) The AT/2 DiscoveryManager which is interested in contacting newly joined virtual machines
 *     to see whether they provide some services that this VM requires.
 *  B) All connected ConnectionListeners, which will usually be remote references pointing to
 *     objects hosted by the connecting/disconnecting VM.
 *
 *
 * TODO: change Address keys of ConnectionListeners to GUIDs to support changing addresses.
 *
 * @author tvcutsem
 */
public class MembershipNotifier extends ExtendedReceiverAdapter implements MembershipListener {
    
	/**
	 * Members of the previously accepted view are stored to evaluate the difference with any
	 * new view that may be signalled to this class. 
	 */
	private final Vector knownMembers_;
	
	/**
	 * A collection of ConnectionListeners which are interested in the (dis)appearance of a single
	 * node in the JGroups overlay network.
	 */
	private final MultiMap connectionListeners_;
	
	/**
	 * The general manager for service discovery for the entire virtual machine. Whenever a new VM
	 * is encountered, this manager needs to be notified such that it can exchange the necessary 
	 * service descriptions. Likewise, VM disconnections should be delegated to this listener.
	 */
	private final DiscoveryListener discoveryManager_;
	
	/**
	 * Creates a new MembershipNotifier on which ConnectionListeners monitoring the (dis)appearance
	 * of a single address can register to. 
	 * @param discoveryManager - the service discovery manager for the current address.
	 */
	public MembershipNotifier(DiscoveryListener discoveryManager) {
		discoveryManager_ = discoveryManager;
		connectionListeners_ = new MultiMap();
		knownMembers_ = new Vector();
	}
	
	/**
	 * Registers <code>listener</code> to be notified whenever a virtual machine becomes (un)reachable.
	 * 
	 * TODO: store only WEAK references to the remote references
	 * 
	 * @param virtualMachine - an address of the virtual machine hosting the object the listener is interested in
	 * @param listener - a listener which will be notified whenever the said address connects or disconnects
	 */
	public synchronized void addConnectionListener(GUID virtualMachine, ConnectionListener listener) {
		connectionListeners_.put(virtualMachine, listener);
	}
	
	/**
	 * Unregisters <code>listener</code> such that it will no longer be notified whenever a 
	 * particular virtual machine becomes (un)reachable.
	 */
	public synchronized void removeConnectionListener(GUID virtualMachine, ConnectionListener listener) {
		connectionListeners_.removeValue(virtualMachine, listener);
	}
	
    /**
     * This method is a callback from the JGroups framework that is invoked whenever the
     * set of connected group members has changed. The callback responds to such an event
     * by comparing the new set of members with the previously known set of members to calculate
     * which members joined and left. For each of these joined and left members, the corresponding
     * connection listeners are notified. Also, the discovery manager is always notified when
     * a member has joined.
     */
	public synchronized void viewAccepted(View newView) {
		Vector newMembers = newView.getMembers();
		Logging.VirtualMachine_LOG.debug("received new view: " + newView);
		
		// for each new member, check whether that new member was present in previous view
		for (Iterator iter = newMembers.iterator(); iter.hasNext();) {
			Address member = (Address) iter.next();
			if (!knownMembers_.contains(member)) {
				// member who is in new view but not in old view: joined
				
				// notify discovery manager
				discoveryManager_.memberJoined(member);
				
			}
		}
		
		// for each old member, check whether the old member is still present in new view
		for (Iterator iter = knownMembers_.iterator(); iter.hasNext();) {
			Address member = (Address) iter.next();
			if (!newMembers.contains(member)) {
				// member who is in old view but not in new view: left
				
				// notify discovery manager
				discoveryManager_.memberLeft(member);
				
			}
		}
		
		// new view becomes previous view
		knownMembers_.clear();
		knownMembers_.addAll(newMembers);
	}

	public synchronized void notifyConnected(GUID vmId){
		//notify all connectionlisteners for this member
		Set listeners = (Set)connectionListeners_.get(vmId);
		if(listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				ConnectionListener listener = (ConnectionListener) i.next();
				listener.connected();
			}
		}
	}
	
	public synchronized void notifyDisconnected(GUID vmId){
		
		//notify all connectionlisteners for this member
		Set listeners = (Set)connectionListeners_.get(vmId);
		if(listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				ConnectionListener listener = (ConnectionListener) i.next();
				listener.disconnected();
			}
		}
	}
	
	// Called by the VM when it has disconnected from the underlying channel
	public synchronized void channelDisconnected() {
		for (Iterator membersI = knownMembers_.iterator(); membersI.hasNext();) {
			// for all members who were in the view
			Address member = (Address) membersI.next();
			
			// notify discovery manager
			discoveryManager_.memberLeft(member);

		}
		
		// clear the set of known members
		knownMembers_.clear();
	}
}
