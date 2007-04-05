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

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.id.VirtualMachineID;
import edu.vub.at.actors.natives.ELFarReference;
import edu.vub.util.MultiMap;

import java.util.Iterator;
import java.util.Set;

/**
 * An instance of this class manages disconnection and reconnection subscriptions and
 * notifications for far references. Whenever virtual machines connect or
 * disconnect from the multicast group, this object is notified. Its role is to propagate
 * these notifications to all registered ConnectionListeners, which will usually be remote
 * references pointing to objects hosted by the connecting/disconnecting VM.
 *
 * @author tvcutsem
 */
public class ConnectionListenerManager {
	
	/**
	 * A collection of ConnectionListeners which are interested in the (dis)appearance of a single
	 * node in the overlay network.
	 */
	private final MultiMap connectionListeners_;
	
	/**
	 * Creates a new manager on which ConnectionListeners monitoring the (dis)appearance
	 * of a single address can register to.
	 */
	public ConnectionListenerManager() {
		connectionListeners_ = new MultiMap();
	}
	
	/**
	 * Registers <code>listener</code> to be notified whenever a virtual machine becomes (un)reachable.
	 * 
	 * TODO: store only WEAK references to the remote references
	 * 
	 * @param virtualMachine - an address of the virtual machine hosting the object the listener is interested in
	 * @param listener - a listener which will be notified whenever the said address connects or disconnects
	 */
	public synchronized void addConnectionListener(VirtualMachineID virtualMachine, ConnectionListener listener) {
		connectionListeners_.put(virtualMachine, listener);
	}
	
	/**
	 * Unregisters <code>listener</code> such that it will no longer be notified whenever a 
	 * particular virtual machine becomes (un)reachable.
	 */
	public synchronized void removeConnectionListener(VirtualMachineID virtualMachine, ConnectionListener listener) {
		connectionListeners_.removeValue(virtualMachine, listener);
	}

	/**
	 * Notify all connection listeners for the given VM id that that VM has come online
	 */
	public synchronized void notifyConnected(VirtualMachineID vmId) {
		//notify all connectionlisteners for this member
		Set listeners = (Set)connectionListeners_.get(vmId);
		if (listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				ConnectionListener listener = (ConnectionListener) i.next();
				listener.connected();
			}
		}
	}
	
	/**
	 * Notify all connection listeners for the given VM id that that VM has gone offline
	 */
	public synchronized void notifyDisconnected(VirtualMachineID vmId){
		//notify all connectionlisteners for this member
		Set listeners = (Set)connectionListeners_.get(vmId);
		if (listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				ConnectionListener listener = (ConnectionListener) i.next();
				listener.disconnected();
			}
		}
	}
	
	/**
	 * Notify all connection listeners registered on the given remote object
	 * TODO: should refactor this: add expired(ATObjectID) method to ConnectionListener interface?
	 */
	public synchronized void notifyObjectExpired(ATObjectID objId){
		//notify only the connectionlisteners for this objId
		Set listeners = (Set)connectionListeners_.get(objId.getVirtualMachineId());
		if (listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				ConnectionListener listener = (ConnectionListener) i.next();
				if (listener instanceof ELFarReference) {
					ATObjectID destination = ((ELFarReference)listener).getDestination();
					if (destination.equals(objId)){
						((ELFarReference)listener).expired();
					}
				}
			}
		}
	}
	
}
