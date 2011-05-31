/**
 * AmbientTalk/2 Project
 * NATRemoteFarRef.java created on 22-dec-2006 at 11:35:01
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

import edu.vub.at.actors.ATLetter;
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATTable;

/**
 * Instances of NATRemoteFarRef represent far references to physically remote actors.
 * By 'physically remote', we mean in a separate address space.
 * 
 * @author tvcutsem
 */
public class NATRemoteFarRef extends NATFarReference {
	
	/**
	 * When a remote far reference is passed on to another virtual machine, the sendLoop
	 * is not taken with it. At the remote end, the sendLoop is bound to the 
	 * FarReferencesThreadPool of the receiving actor.
	 */
	private transient final FarReferencesThreadPool sendLoop_;
	
	/** boolean that keeps track if there is a thread of the FarReferencesThreadPool
	 *  currently transmitting a message. It is used to make meta_retract() wait  
	 *  for the success/failure of the message currently being sent.
	 */
    private transient boolean transmitting_;
    
	public NATRemoteFarRef(ATObjectID objectId, ELActor hostActor, ATTypeTag[] types, boolean isConnected) {
		super(objectId, types, hostActor, isConnected);
		sendLoop_ = hostActor.getHost().farReferencesThreadPool_; 
		transmitting_ = false;
	}
	
	/**
	 * Inserts an AmbientTalk message into this far reference's outbox
	 * and signals a transmit event to the corresponding FarReferencesThreadPool
	 * to schedule its transmission.
	 */
	protected void transmit(ATLetter letter) throws InterpreterException {
		synchronized(this){
			 outbox_.addLast(letter);
		}
		impl_transmit();
	}
	
	/**
	 * In pseudocode this method does the following:
	 * outbox.each: { |letter|  letter.cancel(); };
	 * def messages := outbox.map: { |letter| letter.message };
	 * messages;
	 * 
	 * In contrast to the implementation in NATLocalFarReference, the retract is split into parts because 
	 * it needs to be synchronized with FarReferenceThreadPool (which may be transmitting a message when retract is issued)
	 * 
	 * So, First we ask FarReferencesThreadPool to retract the letters from the outbox and cancel them.
	 * Second, the ELActor issuing the retract will extract the asynchronous message from them.
	 * It is important that ELActor does this, because AT code should only be evaluated within an EventLoop.
	 * Otherwise the coerser thinks a non-event loop is trying to evaluate AT code.
	 */
	public ATTable meta_retractUnsentMessages() throws InterpreterException {
		ATTable outgoingLetters = sendLoop_.sync_event_retractUnsentMessages(this);
		return outgoingLetters.base_map_(new NativeClosure(this){
			public ATObject base_apply(ATTable args) throws InterpreterException {
				ATLetter letter = this.get(args,1).asLetter();
				return letter.base_message().asAsyncMessage();
			}
			
		});
	}
	// cancels and returns a copy of the outbox.
	public ATTable impl_retractOutgoingLetters() throws InterpreterException {
		synchronized (this) {
			if (outbox_.size() > 0 ) {
				ATObject[] outgoing = (ATObject[]) outbox_.toArray(new ATObject[outbox_.size()]);
				outbox_.clear(); // empty the outbox
				return NATTable.atValue(outgoing);	
			}
		}
		// if you arrive here outbox_.size == 0 thus it returns [];
		return NATTable.EMPTY;	
	}
	
	protected synchronized void notifyStateToSendLoop(boolean state){
		//if notifying reconnection, start flushing the outbox serially
		if (state) { impl_transmit(); }
	}
	
	public NATRemoteFarRef asNativeRemoteFarReference() throws XTypeMismatch { return this;}

	/* Following methods are called by a thread within FarReferencesThreadPool */
	
	public ATObject impl_serve() throws InterpreterException{
		synchronized(this) {
			if (outbox_.size() > 0 && connected_) {
				NATOutboxLetter next = (NATOutboxLetter) outbox_.removeLast();
				//setTransmitting(true);
				transmitting_ = true;
				return next;
			}
		}
		return Evaluator.getNil();
	}
    
	// called from a FarReferencesThreadPool#TransmissionEvent 
	// after successfully sending a message to 
	// cause the transmission of next message if any.
	// Also called from ELActor after adding a message in this outbox.
	public void impl_transmit() {
		sendLoop_.event_serve(this);
	}	
	// called from a FarReferencesThreadPool#TransmissionEvent 
	// after the message being transmitted failed.
	public void impl_transmitFailed(ATLetter letter) {
		disconnected();
		// add the message back to the outbox.
		// it cannot happen that this event_transmit is followed by an event_transmit for another message, 
		// so the order will be preserved.
		synchronized(this){
		  outbox_.addFirst(letter);
		}
		setTransmitting(false);
	}
	
	public synchronized void setTransmitting(boolean status) {
		transmitting_ = status;
	}
	
	public synchronized boolean getTransmitting(){
		return transmitting_;
	}
}
