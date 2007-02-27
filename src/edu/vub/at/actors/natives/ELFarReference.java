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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.ConnectionListener;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.actors.net.cmd.CMDTransmitATMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATTable;

import java.util.Iterator;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.MessageDispatcher;

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
	
	// When the Far Reference needs to be interrupted, this field will be set to a non-null value
	// The handle tests for the presence of such an interrupt and will call the handleInterrupt() 
	// method if necessary
	private BlockingFuture interrupt_ = null;
	
	/**
	 * Signals the far reference that its owning actor has requested to retract unsent messages.
	 * The interrupt will be handled as soon as the processing of the current event has finished
	 * @return a blocking future the ELActor thread can wait on.
	 */
	public BlockingFuture setInterrupt() {
		interrupt_ = new BlockingFuture();
		
		// the reception of a new interrupt may awaken a sleeping ELFarReference thread, so we notify
		this.notify();
		
		return interrupt_;
	}
	
	/**
	 * Resolves the current interrupt's future with the vector of transmission events that are 
	 * in the event queue of the far reference. This is used to retrieve copies of all messages
	 * being sent through this far reference. Note that this method performs no deserialisation
	 * of the events into (copied) messages. Such deserialisation needs to be done by an ELActor,
	 * not the ELFarReference which will execute this method.
	 */
	public void handleInterrupt() {
		interrupt_.resolve(eventQueue_.flush());
	}
	
	private final ELActor owner_;
	private final ATObjectID destination_;
	private final MessageDispatcher dispatcher_;
	
	private boolean connected_;
	
	private Vector disconnectedListeners_; // lazy initialization
	private Vector reconnectedListeners_; // lazy initialization
	
	public synchronized void addDisconnectionListener(ATObject listener) {
		if (disconnectedListeners_ == null) {
			disconnectedListeners_ = new Vector(1);
		}
		disconnectedListeners_.add(listener);
	}
	
	public synchronized void addReconnectionListener(ATObject listener) {
		if (reconnectedListeners_ == null) {
			reconnectedListeners_ = new Vector(1);
		}
		reconnectedListeners_.add(listener);
	}

	public synchronized void removeDisconnectionListener(ATObject listener) {
		if (disconnectedListeners_ != null) {
			disconnectedListeners_.remove(listener);
		}
	}
	
	public synchronized void removeReconnectionListener(ATObject listener) {
		if (reconnectedListeners_ != null) {
			reconnectedListeners_.remove(listener);
		}
	}
		
	public ELFarReference(ATObjectID destination, ELActor owner) {
		super("far reference " + destination, false);
		
		destination_ = destination;
		owner_ = owner;
		
		connected_ = true;
		// register the remote reference with the MembershipNotifier to keep track
		// of the state of the connection with the remote VM
		owner_.getHost().membershipNotifier_.addConnectionListener(getDestinationVMAddress(), this);
		
		dispatcher_ = owner_.getHost().messageDispatcher_;
		
		setCustomEventLoop(new EventProcessingLoop());
	}
	
	/**
	 * Process message transmission events only when the remote reference
	 * is connected. Otherwise, wait until notified by the <tt>connected</tt> callback.
	 */
	public void handle(Event event) {
		synchronized (this) {
			while (!connected_ && interrupt_ == null) {
				try {
					this.wait();
				} catch (InterruptedException e) { }
			}
			
			if(interrupt_ != null) {
				// re-enqueue the current event in its proper position
				receivePrioritized(event);
				
				// flush the queue
				handleInterrupt();
				
			// else is strictly necessary as the handleInterrupt method has side effects, 
			// removing the current event from the queue, such that it would be incorrect 
			// to still send it 
			} else { // if (connected_) {
				event.process(this);
			}
		}
	}
	
	/**
	 * TransmissionEvent is a named subclass of event, which allows access to the message the
	 * packet it is trying to send which is used, should the event be retracted. Moreover, the
	 * notion of an explicit constructor which is called by the ELActor scheduling it, ensures
	 * that the serialization of the message happens in the correct thread.
	 *
	 * @author smostinc
	 */
	private class TransmissionEvent extends Event {
		public final Packet serializedMessage_;
		
		// Called by ELActor
		public TransmissionEvent(ATAsyncMessage msg) throws XIOProblem {
			super("transmit("+msg+")");
			serializedMessage_ = new Packet(msg.toString(), msg);
		}
		
		// Called by ELFarReference
		public void process(Object owner) {
			try {
				// JGROUPS:MessageDispatcher.sendMessage(destination, message, mode, timeout)
				Object ack = new CMDTransmitATMessage(destination_.getActorId(), serializedMessage_).send(
						           dispatcher_,
						           getDestinationVMAddress());

				// non-null return value indicates an exception
				if (ack != null) {
					Logging.RemoteRef_LOG.error(this + ": non-null acknowledgement: " + ack);
				}
			} catch (TimeoutException e) {
				Logging.RemoteRef_LOG.warn(this + ": timeout while trying to transmit message, retrying");
				receivePrioritized(this);
			} catch (SuspectedException e) {
				Logging.RemoteRef_LOG.warn(this + ": remote object suspected: " + destination_);
				receivePrioritized(this);
			} catch (Exception e) {
				Logging.RemoteRef_LOG.error(this + ": error upon message transmission:", e);
			} 
		}
	}
	
	public void event_transmit(final ATAsyncMessage msg) throws XIOProblem {
		receive(new TransmissionEvent(msg));
		
		// the reception of a new event may awaken a sleeping ELFarReference thread, so we notify
		this.notify();
	}
	
	public ATTable retractUnsentMessages() throws InterpreterException {
		
		BlockingFuture eventVectorF = setInterrupt();
		
		
		try {
			
			Vector events = (Vector)eventVectorF.get();
			
			for(int i = 0; i < events.size(); i++) {
				TransmissionEvent current = (TransmissionEvent)events.get(i);
				events.set(i, current.serializedMessage_.unpack());
			}
			
			return NATTable.atValue((ATObject[])events.toArray());

		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
	/* ========================================================
	 * == Implementation of the ConnectionListener interface ==
	 * ========================================================
	 */

	public synchronized void connected() {
		Logging.RemoteRef_LOG.info(this + ": reconnected to " + destination_);
		connected_ = true;
		this.notify();
		
		if (reconnectedListeners_ != null) {
			for (Iterator reconnectedIter = reconnectedListeners_.iterator(); reconnectedIter.hasNext();) {
				ATObject listener = (ATObject) reconnectedIter.next();
				try {
					owner_.event_acceptSelfSend(
							new NATAsyncMessage(listener, Evaluator._APPLY_, NATTable.EMPTY));
				} catch (InterpreterException e) {
					Logging.RemoteRef_LOG.error("error invoking when:reconnected: listener", e);
				}
			}	
		}
	}

	public synchronized void disconnected() {
		// Will only take effect when next trying to send a message
		// If currently sending, the message will time out first.
		Logging.RemoteRef_LOG.info(this + ": disconnected from " + destination_);
		connected_ = false;
		
		if (disconnectedListeners_ != null) {
			for (Iterator disconnectedIter = disconnectedListeners_.iterator(); disconnectedIter.hasNext();) {
				ATObject listener = (ATObject) disconnectedIter.next();
				try {
					owner_.event_acceptSelfSend(
							new NATAsyncMessage(listener, Evaluator._APPLY_, NATTable.EMPTY));
				} catch (InterpreterException e) {
					Logging.RemoteRef_LOG.error("error invoking when:disconnected: listener", e);
				}
			}	
		}
	}
	
	private Address getDestinationVMAddress() {
		return owner_.getHost().getAddressOf(destination_.getVirtualMachineId());
	}
	
	
	private final class EventProcessingLoop implements Runnable {
		public void run() {
			synchronized (this) {
				while (eventQueue_.isEmpty() && interrupt_ == null) {
					try {
						this.wait();
					} catch (InterruptedException e) { }
				}

				if (interrupt_ != null) {
					handleInterrupt();
				} else { // if(! eventQueue_.isEmpty()) {
					try {
						handle(eventQueue_.dequeue());
					} catch (InterruptedException e) { }
				}
			}
		}
	}

}
