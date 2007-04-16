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
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.eventloops.EventQueue;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.ConnectionListener;
import edu.vub.at.actors.net.cmd.CMDTransmitATMessage;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.actors.net.comm.CommunicationBus;
import edu.vub.at.actors.net.comm.NetworkException;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.util.logging.Logging;

import java.util.Vector;

/**
 * An instance of the class ELFarReference represents the event loop processor for
 * a remote far reference. That is, the event queue of this event loop serves as 
 * an 'outbox' which is dedicated for storing all messages sent to a particular
 * receiver object hosted by a remote virtual machine.
 * 
 * This event loop handles events from its event queue by trying to transmit them
 * to a remote virtual machine.
 * 
 * Concurrent processing behaviour using a CSP-like nondeterministic select:
 *  execute = select {
 *       ?receive(event) => handle(event)
 *    [] ?interrupted() => retractOutbox()
 *  }
 *  handle(e) = select {
 *       connected => transmit(e)
 *    [] ?interrupted() => putBack(e); retractOutbox()
 *  }
 * 
 * @author tvcutsem
 */
public final class ELFarReference extends EventLoop implements ConnectionListener {
	
	/**
	 * When the {@link ELActor} owning this far reference wants to reify the event queue
	 * as an outbox, it will 'interrupt' this event loop by setting this field to a
	 * non-null value.
	 * <p>
	 * The {@link #handle(Event)} method tests for the presence of such an interrupt and
	 * will call the {@link #handleRetractRequest()} method if this field is set.
	 * <p>
	 * Marked volatile because this variable is read/written by multiple threads without
	 * synchronizing.
	 */
	private volatile BlockingFuture outboxFuture_ = null;
	
	/**
	 * the actor to which this far ref belongs, i.e. the only event loop that will schedule
	 * transmission events into this event loop's event queue
	 */
	private final ELActor owner_;
	
	/** the first-class AT language value wrapping this event loop */
	private final NATRemoteFarRef farRef_;
	
	/** the <i>wire representation</i> of the remote receiver of my messages */
	private final ATObjectID destination_;
	
	/** the network layer used to actually transmit an AmbientTalk message */
	private final CommunicationBus dispatcher_;
	
	/**
	 * A state variable denoting whether the far reference denoted by this
	 * event loop is 'connected' or 'disconnected' from its remote object.
	 * 
	 * If this variable is set to <tt>false</tt>, the event loop will block
	 * and stop processing new events until it is set to <tt>true</tt> again.
	 */
	private boolean connected_;
		
	public ELFarReference(ATObjectID destination, ELActor owner, NATRemoteFarRef ref) {
		super("far reference " + destination);
		
		farRef_ = ref;
		destination_ = destination;
		owner_ = owner;
		
		connected_ = true;
		// register the remote reference with the MembershipNotifier to keep track
		// of the state of the connection with the remote VM
		owner_.getHost().connectionManager_.addConnectionListener(destination_.getVirtualMachineId(), this);
		
		dispatcher_ = owner_.getHost().communicationBus_;
	}
	
	/**
	 * Acknowledges the interrupt set by this far reference's owning actor.
	 * Resolves the {@link #outboxFuture_} interrupt future with the vector of transmission
	 * events that are  in the event queue of the far reference. This is used to retrieve copies
	 * of all messages being sent through this far reference. Note that this method performs no
	 * deserialisation of the events into (copied) messages. Such deserialisation needs to be
	 * done by an {@link ELActor}, not the ELFarReference which will execute this method.
	 * 
	 * After handling the retract request, the {@link #outboxFuture_} is reset to <tt>null</tt>.
	 * This is extremely important because it signifies that there is no more pending retract request.
	 */
	public void handleRetractRequest() {
		outboxFuture_.resolve(eventQueue_.flush());
		// if the future is not reset to null, the event loop would continually
		// resolve the future from this point on!
		outboxFuture_ = null;
	}
	
	/**
	 * Process message transmission events only when the remote reference
	 * is connected. Otherwise, wait until notified by the <tt>connected</tt>
	 * callback.
	 * 
	 * When handling a transmission event from the event queue, the event
	 * loop can only process the event if either:
	 * <ul>
	 *  <li>the far reference is currently <i>connected</i>. In this case,
	 *      the incoming event is simply processed.
	 *  <li>the far reference is disconnected, so blocked waiting to become
	 *      reconnected, but is being interrupted by its owner to flush
	 *      its outbox. In this case, the current event on which the event
	 *      loop is blocked is re-inserted into the event queue (in front!)
	 *      and the interrupt is honoured.
	 * </ul>
	 * 
	 */
	public void handle(Event event) {
		synchronized (this) {
			while (!connected_ && outboxFuture_ == null) {
				try {
					this.wait();
				} catch (InterruptedException e) { }
			}
			
			if(outboxFuture_ != null) {
				// re-enqueue the current event in its proper position
				receivePrioritized(event);
				
				// flush the queue
				handleRetractRequest();
				
			// else is strictly necessary as the handleInterrupt method has side effects, 
			// removing the current event from the queue, such that it would be incorrect 
			// to still send it 
			} else { // if (connected_) {
				event.process(this);
			}
		}
	}
	
	/**
	 * This is a named subclass of event, which allows access to the AmbientTalk message
	 * that is being transmitted. The constructor is still executed by the {@link ELActor}
	 * that schedules this transmission event. This ensures
	 * that the serialization of the message happens in the correct thread.
	 * 
	 * @see {@link ELFarReference#event_transmit(ATAsyncMessage)}
	 * @author smostinc
	 */
	private class TransmissionEvent extends Event {
		public final Packet serializedMessage_;
		
		// Called by ELActor
		public TransmissionEvent(ATAsyncMessage msg) throws XIOProblem {
			super("transmit("+msg+")");
			serializedMessage_ = new Packet(msg.toString(), msg);
		}
		
		/**
		 * This code is executed by the {@link ELFarReference} event loop.
		 */
		public void process(Object owner) {
			Address destAddress = getDestinationVMAddress();

			if (destAddress != null) {
				try {
					new CMDTransmitATMessage(destination_.getActorId(), serializedMessage_).send(
							dispatcher_, destAddress);

					// getting here means the message was succesfully transmitted
					
				} catch (NetworkException e) {
					// TODO: the message MAY have been transmitted! (i.e. an orphan might have
					// been created: should make this more explicit to the AT programmer)
					Logging.RemoteRef_LOG.warn(this
									+ ": timeout while trying to transmit message, retrying");
					receivePrioritized(this);
				}
			} else {
				Logging.RemoteRef_LOG.info(this + ": suspected a disconnection from " +
						destination_ + " because destination VM ID was not found in address book");
				connected_ = false;
				receivePrioritized(this);
			}
		}
	}
	
	/**
	 * Inserts an AmbientTalk message into this far reference's outbox.
	 */
	public void event_transmit(final ATAsyncMessage msg) throws XIOProblem {
		// the message will still be serialized in the actor's thread
		receive(new TransmissionEvent(msg));
		
		// the reception of a new event may awaken a sleeping ELFarReference thread, 
		// so we interrupt the processor, forcing it to reevaluate its conditions
		// we don't use wait/notify because in order to notify the event loop, we
		// would require a lock on it, which might cause this actor to block on
		// remote communication. Therefore, we use the interrupt mechanism instead.
		processor_.interrupt();
	}
	
	/**
	 * Interrupts this event loop by issuing a request for flushing
	 * its event queue.
	 * 
	 * This code is executed by the event loop thread of the {@link #owner_}!
	 * 
	 * @return a table of copies for the messages currently in the outbox
	 */
	public ATTable retractUnsentMessages() throws InterpreterException {
		BlockingFuture eventVectorF = setRetractingFuture();
		Vector events = null;
		
		try {
			// actor has to wait a bit until the event loop has stopped processing
			events = (Vector) eventVectorF.get();
		} catch(Exception e) {
			// should never occur!
			e.printStackTrace();
			return NATTable.EMPTY;
		}
		
		ATObject[] messages = new ATObject[events.size()];
		
		for(int i = 0; i < events.size(); i++) {
			TransmissionEvent current = (TransmissionEvent)events.get(i);
			messages[i] = current.serializedMessage_.unpack();
		}
			
		return NATTable.atValue(messages);
	}
	
	public ATObjectID getDestination() {
		return destination_;
	}
	
	/* ========================================================
	 * == Implementation of the ConnectionListener interface ==
	 * ========================================================
	 */

	public synchronized void connected() {
		// sanity check: don't connect  twice
		if (!connected_) {
			Logging.RemoteRef_LOG.info(this + ": reconnected to " + destination_);
			connected_ = true;
			this.notify();
			farRef_.notifyConnected();	
		}
	}

	public synchronized void disconnected() {
		// sanity check: don't disconnect twice
		if (connected_) {
			// Will only take effect when next trying to send a message
			// If currently sending, the message will time out first.
			Logging.RemoteRef_LOG.info(this + ": disconnected from " + destination_);
			connected_ = false;
			farRef_.notifyDisconnected();	
		}
	}
	
	public synchronized void expired(){
		Logging.RemoteRef_LOG.info(this + ": " + destination_ + " expired");
		connected_ = false;
		farRef_.notifyExpired();
	}
	
	/**
	 * Overrides the default event handling strategy of this event loop.
	 * It is no longer possible to simply block and wait for a new event
	 * by performing {@link EventQueue#dequeue()} because this event loop
	 * can be triggered by two kinds of wake-up calls, either:
	 * <ul>
	 *  <li>a new event arrives in the event queue, or
	 *  <li>the actor owning this far reference interrupts it to flush
	 *  its event queue into a reified outbox representation
	 * </ul>
	 * 
	 * Therefore, this event loop synchronises on two different boolean
	 * conditions. When exiting the <tt>wait</tt>-loop, the event loop has
	 * to check which condition woke it up:
	 * <ul>
	 *  <li>if it was an incoming event, handle it
	 *  <li>if it was interrupted by the owning actor, honor the interrupt
	 * </ul>
	 * 
	 * Note that while executing, the event loop takes a lock on itself!
	 * This synchronizes event processing with state transition notifications
	 * via the {@link #connected()} and {@link #disconnected()} methods.
	 */
	protected void execute() {
		synchronized (this) {
			while (eventQueue_.isEmpty() && outboxFuture_ == null) {
				try {
					this.wait();
				} catch (InterruptedException e) { }
			}

			if (outboxFuture_ != null) {
				handleRetractRequest();
			} else { // if(! eventQueue_.isEmpty()) {
				try {
					handle(eventQueue_.dequeue());
				} catch (InterruptedException e) { }
			}
		}
	}
	
	private Address getDestinationVMAddress() {
		return owner_.getHost().vmAddressBook_.getAddressOf(destination_.getVirtualMachineId());
	}
	
	/**
	 * Signals the far reference that its owning actor has requested to retract unsent
	 * messages. The interrupt will be handled as soon as the processing of the current
	 * event has finished.
	 * @return a blocking future the ELActor thread can wait on.
	 */
	private BlockingFuture setRetractingFuture() {
		outboxFuture_ = new BlockingFuture();
		
		// the reception of a new interrupt may awaken a sleeping ELFarReference 
		// thread, so we interrupt them, forcing them to reevaluate their conditions
		processor_.interrupt();
		return outboxFuture_;
	}

}
