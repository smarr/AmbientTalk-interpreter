/**
 * AmbientTalk/2 Project
 * (c) Software Languages Lab, 2006 - 2010
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
 * 
**/
package edu.vub.at.actors.natives;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.ATLetter;
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.cmd.CMDTransmitATMessage;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.actors.net.comm.CommunicationBus;
import edu.vub.at.actors.net.comm.NetworkException;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.util.logging.Logging;

/**
 * An instance of the class FarReferencesThreadPool represents a pool of threads for
 * all remote far references belonging to an ELVirtualMachine. 
 * 
 * ELDiscoveryActor is initialized without a FarReferencesThreadPool as it only 
 * holds local far references to closures corresponding to when(er):discovered listeners.
 * 
 * FarReferencesThreadPool works together with NATRemoteFarRef to ensure that messages are
 * sent in the order they are received by a NATFarReference. More concretely:
 *   -ELActor owner of the far reference enqueues a message to be transmitted in its outbox when
 *   calls NATFarReference.meta_receive().
 *   -This will trigger an event_serve in FarReferencesThreadPool and a thread on it will dequeue
 *   a letter from the outbox and transmits its message. 
 *   -After transmitting a message, event_serve() is called to check if there are other letters to be served.
 *   -After a reconnect, event_serve() is also called to serve letters buffered by ELActor during a disconnection.
 *   
 *  This behaviour is similar to an EventLoop, just that it may not be the same thread
 *  removing from the outbox, but one of the thread pool. 
 *  Removals from the NATFarReference outbox will be always executed by a thread of the pool 
 *  while additions can be executed either by ELActor owner (due to meta_receive) 
 *  or a thread of the pool if the message transmission failed and needs to be put back to the outbox.
 */
public final class FarReferencesThreadPool {
	
	
	/**
	 * The virtual machine to which this far ref pool belongs.
	 * One can get the dispatcher from ELVirtualMachine, but keep for optimization.
	 */
	private final ELVirtualMachine host_;

	/** the network layer used to actually transmit an AmbientTalk message */ 
	private final CommunicationBus dispatcher_;
	
	/** the pool of threads**/
	private final ExecutorService pool;
	
	
	/**
	 *  map of reference (NATRemoteFarREf) -> future for retract requests (BlockingFuture) 
	 *  to be able to synchronize a thread performing a retract operation, and 
	 *  a thread transmitting a message when the request is processed.
	 *  See {@link sync_event_retractUnsentMessages}
	 */
	private final HashMap<ATFarReference, BlockingFuture>  retractFutures_;
	

	public FarReferencesThreadPool(ELVirtualMachine host) {
		host_ = host;
		dispatcher_ = host_.communicationBus_;
		pool = Executors.newCachedThreadPool();
		retractFutures_ = new HashMap();
	}
			
	protected final void receive(Event event) {
		Handle h = new Handle(event);
	   	pool.execute( h);
	}
	
	// helper class to wrap the AT event to be
	// processed.
	class Handle implements Runnable{
		private Event event_;
		public Handle( Event event){
			event_ = event;
		}
		public void run() {
			event_.process(host_);
		}	
	}
	

	/**
	 * This is a named subclass of event, which allows access to the letter
	 * that is being transmitted. The constructor is executed by:
	 * ELActor sending the message when reference is connected, or
	 * a thread in the pool that schedules this transmission event after a reconnection.  
	 */
	class TransmissionEvent extends Event{
		/** the letter containing the serialized and original message */	
		public final ATLetter letter_;
		/** the <i>wire representation</i> of the remote receiver of this message */
		public final ATObjectID destination_;
		/* the first-class AT reference sending this message */
		public final ATFarReference reference_;
		
		public TransmissionEvent(ATFarReference reference, ATLetter letter) throws InterpreterException {
			super ("transmit( ["+ letter.base_receiver().asFarReference() + ","+ letter.base_message().asAsyncMessage() +"])");
			letter_ = letter;
			reference_ = reference;
			destination_ =  reference_.asNativeFarReference().impl_getObjectId();
		}
		public void process(Object owner){
			Address destAddress = getDestinationVMAddress();
			if (destAddress != null) {
				try {		
					new CMDTransmitATMessage(destination_.getActorId(), letter_.asNativeOutboxLetter().impl_getSerializedMessage()).send(
							dispatcher_, destAddress);
					// getting here means the message was succesfully transmitted
					reference_.asNativeRemoteFarReference().setTransmitting(false);
					// check if 1) there is a retract request for this reference
					// and afterwards 2) if another message to be transmitted.
					// Needed now because pool is not an event loop.
					handleRetractRequest(reference_);
					reference_.asNativeRemoteFarReference().impl_transmit();
				} catch (NetworkException e) {
					// TODO: the message MAY have been transmitted! (i.e. an orphan might have
					// been created: should make this more explicit to the AT programmer)
					// To solve this add message ids, and check you don't process twice the same message.
					Logging.RemoteRef_LOG.warn(reference_
							+ ": timeout while trying to transmit message, retrying");
					// try to send it again, if the remote VM went offline, 
					// next time this message is processed destAddress == null.
					this.process(owner);
				} catch (XTypeMismatch e) {
					Logging.RemoteRef_LOG.warn(reference_
							+ ": unexpected type mismatch: " + e.getMessage());
					e.printStackTrace();
				} catch (InterpreterException e) {
					Logging.RemoteRef_LOG.warn(reference_
							+ ": unexpected error while handling retract request after a successful transmission: " + e.getMessage());
					e.printStackTrace();
				} 
			} else {
				Logging.RemoteRef_LOG.info(reference_ + ": suspected a disconnection from " +
						destination_ + " because destination VM ID was not found in address book");
				// destAddress is null is because it was removed from a event_memberLeft();	
				try {
					reference_.asNativeRemoteFarReference().impl_transmitFailed(letter_);
					handleRetractRequest(reference_); 
				} catch (XTypeMismatch e) {
					Logging.RemoteRef_LOG.warn(reference_
							+ ": unexpected type mismatch: " + e.getMessage());
					e.printStackTrace();
				} catch (InterpreterException e) {
					Logging.RemoteRef_LOG.warn(reference_
							+ ": unexpected error while handling retract request after transmission failed: " + e.getMessage());
					e.printStackTrace();
				}					
			}
		}
		private Address getDestinationVMAddress() {
			return host_.vmAddressBook_.getAddressOf(destination_.getVirtualMachineId());
		}
	}
	/** transmitting is embedded first in another Event so that only 
	  * a thread from this thread pool will dequeue messages from the mailbox.
	  * 
	  */
	public void event_serve(final ATFarReference reference) {
		receive(new Event("serve()") {
			public void process(Object owner) {
				try {
					ATObject result = reference.asNativeRemoteFarReference().impl_serve();
					// do not span a new thread to transmit, try to transmit it itself. 
					// Not comparing to Evaluator.getNil() because it will return a different instance.
					if (!( result instanceof NATNil)) {
						TransmissionEvent transmit = new TransmissionEvent(reference, result.asNativeOutboxLetter());
						transmit.process(owner);
					}
				} catch (InterpreterException e) {
					Logging.RemoteRef_LOG.warn(this + ": serve() failed ", e);
				}
			}
		} );
	}
	
	/**
	 * Signals that the owning actor of a far reference has requested to retract unsent
	 * messages. The request will be handled as soon as the transmission of the current
	 * letter has finished.
	 * 
	 * Note that it cannot happen that two consecutives retracts requests arrive for the
	 * same reference since the ELActor is blocked on a future waiting for the request 
	 * to be processed.
	 * 
	 * @return a blocking future the ELActor thread can wait on.
	 */
	private BlockingFuture setRetractingFuture(ATFarReference reference) {
		// first assign future to a local variable to avoid race conditions on writing to the outboxFuture_ variable!
		final BlockingFuture future = new BlockingFuture();
		synchronized(this) { 
			// synchronized access because different thread in the pool could
			// be processing retractUnsentMessages requests for different references.  
			retractFutures_.put(reference, future);
		}
		return future;
	}
	
	/**
	 * Checks in the {@link #retractFutures_} for pending retractUnsentMessages request 
	 * for a given reference.
	 * 
	 * After handling the retract request, the entry in the {@link #retractFutures_} for 
	 * the given reference is removed. This is extremely important because it signifies
	 * that there is no more pending retract request for this reference.
	 */
	private synchronized void handleRetractRequest(ATFarReference reference) throws XTypeMismatch, InterpreterException{	
		BlockingFuture retractFuture = retractFutures_.remove(reference);
		if (retractFuture != null) {
			retractFuture.resolve(reference.asNativeFarReference().impl_retractUnsentMessages());
		}
	}
	
	public ATTable sync_event_retractUnsentMessages(final ATFarReference reference) throws InterpreterException {
		try {
			return (ATTable) pool.submit(new Callable() {
				public Object call() throws Exception {
					/** we need to synchronize the whole block to  make sure that getTransmitting and 
					 * adding the retract future to the map is done atomically to avoid that the
					 * ELActor is block till the next transmission event because the transmission
					 * thread didn't see the request because:
					 * 		Transmission -thread 1		Retract Request - thread 2
					 *	T1									getTransmit() -> true
					 *	T2   	setTransmit(false)	
					 *	T3		handleRetractRequest()
					 *	T4									setRetractingFuture(reference);
					 * 
					*/
					synchronized (this) {
						if ( reference.asNativeRemoteFarReference().getTransmitting()){
							// if there is a thread transmitting a message for this reference:
							// resolve the future with a BlockingFuture to wait for the result of the transmission.
							BlockingFuture future = setRetractingFuture(reference);
							return future.get();
						} else {
							// if there is no thread transmitting a message for this reference:
							// resolve the future immediately with the content of its oubox.
							return reference.asNativeFarReference().impl_retractUnsentMessages();
						}
					}
				}
			}).get();
		} catch (Exception e) {
			if (e instanceof InterpreterException) {
				throw (InterpreterException) e;
			} else {
				Logging.RemoteRef_LOG.fatal("Unexpected Java exception: "+e.getMessage(), e);
				throw new RuntimeException("Unexpected exception: "+e);
			}
		}
	}
}
