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

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.ExtendedReceiverAdapter;
import org.jgroups.MembershipListener;
import org.jgroups.View;

import edu.vub.at.actors.natives.DiscoveryManager;
import edu.vub.util.MultiMap;

public class MembershipNotifier extends ExtendedReceiverAdapter implements MembershipListener {
    
	/**
	 * Members of the previously accepted view are stored to evaluate the difference with any
	 * new view that may be signalled to this class. 
	 */
	protected final Vector members_ = new Vector();
	
	/**
	 * A collection of ConnectionListeners which are interested in the (dis)appearance of a single
	 * node in the JGroups overlay network.
	 */
	protected final MultiMap connectionListeners_ = new MultiMap();
	
	/**
	 * The general manager for service discovery for the entire virtual machine. Whenever a new node
	 * is encountered, this manager needs to be notified such that it can exchange the necessary 
	 * service descriptions.
	 */
	protected final DiscoveryManager discoveryManager_;
	
	/**
	 * Created a new MembershipNotifier on which ConnectionListeners monitoring the (dis)appearance
	 * of a single address can register to. 
	 * @param discoveryManager - the service discovery manager for the current address.
	 */
	public MembershipNotifier(DiscoveryManager discoveryManager) {
		discoveryManager_ = discoveryManager;
	}
	
	/**
	 * Registers <code>listener</code> to be notified whenever a virtual machine becomes (un)reachable.
	 * 
	 * @param virtualMachine - an address of the virtual machine hosting the object the listener is interested in
	 * @param listener - a listener which will be notified whenever the said address connects or disconnects
	 */
	public synchronized void addConnectionListener(Address virtualMachine, ConnectionListener listener) {
		connectionListeners_.put(virtualMachine, listener);
	}
	
	/**
	 * Unregisters <code>listener</code> such that it will no longer be notified whenever a 
	 * particular virtual machine becomes (un)reachable.
	 */
	public synchronized void removeConnectionListener(Address virtualMachine, ConnectionListener listener) {
		connectionListeners_.removeValue(virtualMachine, listener);
	}

    /**
     * Notify membership listener that new view was accepted. This method in 
     * turn passes new view to all registered membership listeners.
     * 
     * 
     * TODO optimize set difference operations
     */
	public synchronized void viewAccepted(View new_view) {
		Vector joined_mbrs, left_mbrs, tmp;
		Object tmp_mbr;

		if (new_view == null)
			return;
		tmp = new_view.getMembers();

		synchronized (members_) {
			// get new members
			joined_mbrs = new Vector();
			for (int i = 0; i < tmp.size(); i++) {
				tmp_mbr = tmp.elementAt(i);
				if (!members_.contains(tmp_mbr))
					joined_mbrs.addElement(tmp_mbr);
			}

			// get members that left
			left_mbrs = new Vector();
			for (int i = 0; i < members_.size(); i++) {
				tmp_mbr = members_.elementAt(i);
				if (!tmp.contains(tmp_mbr))
					left_mbrs.addElement(tmp_mbr);
			}

			// adjust our own membership
			members_.removeAllElements();
			members_.addAll(tmp);
		}

		
		for (Iterator newMembers = joined_mbrs.iterator(); newMembers.hasNext();) {
			Address member = (Address) newMembers.next();
			
			discoveryManager_.memberJoined(member);
			
			Set listeners = (Set)connectionListeners_.get(member);
			if(listeners != null) {
				for (Iterator i = listeners.iterator(); i.hasNext();) {
					ConnectionListener listener = (ConnectionListener) i.next();
					listener.connected();
				}
			}
		}
		
		for (Iterator leftMembers = joined_mbrs.iterator(); leftMembers.hasNext();) {
			Address member = (Address) leftMembers.next();
			
			discoveryManager_.memberLeft(member);
			
			Set listeners = (Set)connectionListeners_.get(member);
			if(listeners != null) {
				for (Iterator i = listeners.iterator(); i.hasNext();) {
					ConnectionListener listener = (ConnectionListener) i.next();
					listener.disconnected();
				}
			}
		}

	}
}
