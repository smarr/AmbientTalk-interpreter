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

import java.util.Iterator;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.ConnectionListener;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;

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
	
	private final ELActor owner_;
	private final ATObjectID destination_;
	private final MessageDispatcher dispatcher_;
	
	private boolean connected_;
	
	private Vector disconnectedListeners_;
	private Vector reconnectedListeners_;
	
	public void addDisconnectionListener(ATObject listener) {
		disconnectedListeners_.add(listener);
	}
	
	public void addReconnectionListener(ATObject listener) {
		reconnectedListeners_.add(listener);
	}

	public void removeDisconnectionListener(ATObject listener) {
		disconnectedListeners_.remove(listener);
	}
	
	public void removeReconnectionListener(ATObject listener) {
		reconnectedListeners_.remove(listener);
	}
		
	public ELFarReference(ATObjectID destination, ELActor owner) {
		super("far reference " + destination);
		destination_ = destination;
		owner_ = owner;
		
		connected_ = true;
		// register the remote reference with the MembershipNotifier to keep track
		// of the state of the connection with the remote VM
		Address vmId = destination_.getVirtualMachineAddress();
		owner_.getHost().membershipNotifier_.addConnectionListener(vmId, this);
		
		dispatcher_ = owner_.getHost().messageDispatcher_;
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
					// JGROUPS:MessageDispatcher.sendMessage(destination, message, mode, timeout)
					Object returnVal = dispatcher_.sendMessage(
							// JGROUPS:Message.new(destination, source, Serializable)
							new Message(destination_.getVirtualMachineAddress(), null, new Packet(destination_.getActorId(), msg.toString(), msg)),
							GroupRequest.GET_FIRST,
							_TRANSMISSION_TIMEOUT_);
					
					// non-null return value indicates an exception
					if (returnVal != null) {
						Logging.RemoteRef_LOG.error(this + ": error upon message transmission:", (Exception) returnVal);
					}
				} catch (XIOProblem e) {
					// TODO Error serializing the message, drop it? 
					Logging.RemoteRef_LOG.error(this + ": error while serializing message:", e);
				} catch (TimeoutException e) {
					Logging.RemoteRef_LOG.warn(this + ": timeout while trying to transmit message, retrying");
					receivePrioritized(this);
				} catch (SuspectedException e) {
					Logging.RemoteRef_LOG.warn(this + ": remote object suspected: " + destination_);
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
		Logging.RemoteRef_LOG.info(this + ": connected to " + destination_);
		connected_ = true;
		this.notifyAll();
		
		for (Iterator reconnectedIter = reconnectedListeners_.iterator(); reconnectedIter.hasNext();) {
			ATObject listener = (ATObject) reconnectedIter.next();
			try {
				owner_.event_acceptSelfSend(
						new NATAsyncMessage(NATNil._INSTANCE_, listener, Evaluator._APPLY_, NATTable.EMPTY));
			} catch (InterpreterException e) {
				// TODO Errors during the invocation of connection listeners should be logged
			}
		}
	}

	public synchronized void disconnected() {
		// Will only take effect when next trying to send a message
		// If currently sending, the message will time out first.
		Logging.RemoteRef_LOG.info(this + ": disconnected from " + destination_);
		connected_ = false;
		
		for (Iterator disconnectedIter = disconnectedListeners_.iterator(); disconnectedIter.hasNext();) {
			ATObject listener = (ATObject) disconnectedIter.next();
			try {
				owner_.event_acceptSelfSend(
						new NATAsyncMessage(NATNil._INSTANCE_, listener, Evaluator._APPLY_, NATTable.EMPTY));
			} catch (InterpreterException e) {
				// TODO Errors during the invocation of connection listeners should be logged
			}
		}
	}

}
