/**
 * AmbientTalk/2 Project
 * ELFarReference.java created on 28-dec-2006 at 10:45:41
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

import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.ConnectionListener;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATTable;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.GroupRequest;

/**
 * An instance of the class ELFarReference represents the event loop processor for
 * a remote far reference. That is, the event queue of this event loop serves as 
 * an 'outbox' which is dedicated to a certain receiver object hosted by a remote virtual machine.
 * 
 * This event loop handles event from its event queue by trying to transmit them to a remote virtual machine.
 * 
 * @author tvcutsem
 */
public final class ELFarReference extends EventLoop implements ConnectionListener {

	private static final int _TRANSMISSION_TIMEOUT_ = 5000; // in milliseconds
	
	private final NATRemoteFarRef owner_;
	private final ELVirtualMachine host_;
	private boolean connected_;
	
	public ELFarReference(NATRemoteFarRef owner, ELVirtualMachine host) {
		super("far reference " + owner);
		owner_ = owner;
		host_ = host;

		connected_ = true;
		// register the remote reference with the MembershipNotifier to keep track
		// of the state of the connection with the remote VM
		Address vmId = owner.getObjectId().getVirtualMachineAddress();
		host_.membershipNotifier_.addConnectionListener(vmId, this);
	}

	/**
	 * Process message transmission events only when the remote reference
	 * is connected. Otherwise, wait until notified by the <tt>connected</tt> callback.
	 */
	public void handle(Event event) {
		synchronized (this) {
			while (!connected_) {
				try {
					this.wait();
				} catch (InterruptedException e) { }
			}
			event.process(owner_);
		}
	}
	
	public void event_transmit(final ATAsyncMessage msg) {
		receive(new Event("transmit("+msg+")") {
			public void process(Object owner) {
				try {
					ATObjectID id = owner_.getObjectId();
					
					// JGROUPS:MessageDispatcher.sendMessage(destination, message, mode, timeout)
					Object returnVal = host_.messageDispatcher_.sendMessage(
							// JGROUPS:Message.new(destination, source, Serializable)
							new Message(id.getVirtualMachineAddress(), null, new Packet(id.getActorId(), msg.toString(), msg)),
							GroupRequest.GET_FIRST,
							_TRANSMISSION_TIMEOUT_);
					
					// non-null return value indicates an exception
					if (returnVal != null) {
						System.err.println(this + ": error upon transmission:");
						((Exception) returnVal).printStackTrace();
					}
				} catch (XIOProblem e) {
					// TODO Error serializing the message, drop it? 
					System.err.println(this + ": error while serializing message:");
					e.printStackTrace();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					System.err.println(this + ": timeout while trying to transmit message:");
					e.printStackTrace();
					receivePrioritized(this);
				} catch (SuspectedException e) {
					// TODO Auto-generated catch block
					System.err.println(this + ": remote object suspected of having gone offline:");
					e.printStackTrace();
					receivePrioritized(this);
				}
			}
		});
	}
	
	public ATTable sync_event_retractUnsentMessages() throws InterpreterException {
		try {
			// TODO: shouldn't this event be sheduled in a prioritized fashion?
			return (ATTable) receiveAndWait("retractUnsentMessages()", new Callable() {
				public Object call(Object owner) throws Exception {
					//final NATRemoteFarRef me = (NATRemoteFarRef) owner;
					// TODO: return outgoing unsent messages
					return NATTable.EMPTY;
				}
			});
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
	/* ========================================================
	 * == Implementation of the ConnectionListener interface ==
	 * ========================================================
	 */

	public synchronized void connected() {
		System.err.println(this + ": connected");
		connected_ = true;
		this.notifyAll();
	}

	public synchronized void disconnected() {
		// Will only take effect when next trying to send a message
		// If currently sending, the message will time out first.
		System.err.println(this + ": disconnected");
		connected_ = false;
	}

}
