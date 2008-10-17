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
import edu.vub.at.actors.natives.NATFarReference;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.MultiMap;

import java.lang.ref.WeakReference;
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
	 * @param virtualMachine - an address of the virtual machine hosting the object the listener is interested in
	 * @param listener - a listener which will be notified whenever the said address connects or disconnects
	 */
	public synchronized void addConnectionListener(VirtualMachineID virtualMachine, ConnectionListener listener) {
		connectionListeners_.put(virtualMachine, new WeakReference(listener));
	}
	
	/**
	 * Unregisters <code>listener</code> such that it will no longer be notified whenever a 
	 * particular virtual machine becomes (un)reachable.
	 */
	public synchronized void removeConnectionListener(VirtualMachineID virtualMachine, ConnectionListener listener) {

		Set listeners = (Set)connectionListeners_.get(virtualMachine);
		if(listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				WeakReference pooled = (WeakReference) i.next();
				if (pooled != null) {
					ConnectionListener list = (ConnectionListener) pooled.get();
					if( list != null){
						if (list.equals(listener)) {
							Logging.VirtualMachine_LOG.info("Removing ELFarReference from CLM " + this);
							i.remove();
						}
					}else{
						// the listener referenced by the WeakReference was already gced => remove the pointer to WeakReference.
						i.remove();
					}
				}
			}
		}
	}

	/**
	 * Notify all connection listeners for the given VM id that that VM has come online
	 */
	public synchronized void notifyConnected(VirtualMachineID vmId) {
		//notify all connectionlisteners for this member
		Set listeners = (Set)connectionListeners_.get(vmId);
		if (listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				WeakReference pooled = (WeakReference) i.next();
				if (pooled != null) {
					ConnectionListener listener = (ConnectionListener) pooled.get();
					if (listener != null){
						listener.connected();
					}else{
						// the listener referenced by the WeakReference was already gced => remove the pointer to WeakReference.
						i.remove();
					}
				}
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
				WeakReference pooled = (WeakReference) i.next();
				if (pooled != null) {
					ConnectionListener listener = (ConnectionListener) pooled.get();
					if (listener != null){
						listener.disconnected();
					}else{
						// the listener referenced by the WeakReference was already gced => remove the pointer to WeakReference.
						i.remove();
					}
				}
			}
		}
	}
	
	/**
	 * Notify all connection listeners registered on the given remote object
	 */
	public synchronized void notifyObjectTakenOffline(ATObjectID objId){
		//notify only the connectionlisteners for this objId
		Set listeners = (Set)connectionListeners_.get(objId.getVirtualMachineId());
		if (listeners != null) {
			for (Iterator i = listeners.iterator(); i.hasNext();) {
				WeakReference pooled = (WeakReference) i.next();
				if (pooled != null) {
					ConnectionListener listener = (ConnectionListener) pooled.get();
					if (listener instanceof NATFarReference) {
						ATObjectID destination = ((NATFarReference)listener).getObjectId();
						if (destination.equals(objId)){
							listener.takenOffline();
							//The entry on the table is removed so that the remote far reference is never 
							//notified when the vmid hosting the offline object becomes (un)reachable.
							//In fact, the reference doesn't care about the such notifications because 
							//an offline object will never become online.
							i.remove();
						}
					}else{
						// the listener referenced by the WeakReference was already gced => remove the pointer to WeakReference.
						i.remove();
					}
				}	
			}
		}
	}
	
}
