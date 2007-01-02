/**
 * AmbientTalk/2 Project
 * NATActorMirror.java created on Oct 16, 2006 at 1:55:05 PM
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
import edu.vub.at.actors.ATMailbox;
import edu.vub.at.actors.ATResolution;
import edu.vub.at.actors.ATServiceDescription;
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.events.ActorEmittedEvents;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The NATActorMirror class implements the concurrency model of ambienttalk. It continually
 * consumes meta-events which are written to a synchronized queue. This way the actor
 * is notified of incoming messages, discovered and lost services, etc. When no meta-
 * messages are available, the actor consumes base-level messages as they are stored
 * in its inbox. These messages have a receiver object internal to the actor and they 
 * will be invoked on this receiver by the actor's thread.
 *
 * @author smostinc
 */
public class NATActorMirror extends NATByRef implements ATActorMirror {
	
	// Observable event name
	public static final ATSymbol _PROCESSED_ = AGSymbol.jAlloc("messageProcessed");

	// INSTANCE VARIABLES
	
	/*
	 * Actors have a classical combination of an inbox and outbox which reify the
	 * future communication state of an actor. A reification of past communication
	 * can be obtained by installing a custom <code>receivedbox</code> and <code>
	 * sentbox</code> which are filled upon reception of the meta-level events 
	 * <code>process</code> and <code>delivered</code> respectively.
	 */
	final ATMailbox inbox_ = new NATMailbox(this, ATActorMirror._IN_);
	final ATMailbox outbox_ = new NATMailbox(this, ATActorMirror._OUT_);
	final ATMailbox providedbox_ = new NATMailbox(this, ATActorMirror._PROVIDED_);
	final ATMailbox requiredbox_ = new NATMailbox(this, ATActorMirror._REQUIRED_);
	
	private final ELVirtualMachine host_;
	
	/**
	 * Creates a new actor on the specified host Virtual Machine. The actor its behaviour
	 * is intialized to the passed isolate. The calling thread is **blocked** until the
	 * actor has been constructed. However, actor root initialization etc. is carried
	 * out by the newly created actor itself.
	 * 
	 * @param host the VM hosting this actor - after creation, the actor registers itself with this VM
	 * @param behaviourPkt the serialized version of this actor's behaviour
	 * @param actorMirror this actor's mirror
	 * @return a far reference to the behaviour of the actor
	 * @throws InterpreterException
	 */
	public static NATLocalFarRef atValue(ELVirtualMachine host, Packet behaviourPkt, ATActorMirror actorMirror) throws InterpreterException {
		BlockingFuture future = new BlockingFuture();
		ELActor processor = new ELActor(actorMirror, host);
		
		// schedule special 'init' message which will:
		// A) create a new behaviour and will unblock creating actor (by passing it a far ref via the future)
		// B) initialize the behaviour with the given closure
		processor.event_init(future, behaviourPkt);
		
		// notify host VM about my creation
		host.event_actorCreated(processor);
	    
		try {
			return (NATLocalFarRef) future.get();
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
	public static NATLocalFarRef atValue(ELVirtualMachine host, Packet behaviourPkt, Packet actorMirrorPkt) throws InterpreterException {
		return NATActorMirror.atValue(host, behaviourPkt, actorMirrorPkt.unpack().base_asActorMirror());
	}
	
	
	/**
	 * When initializing a new actor, do not forget that this constructor is still executed by the
	 * creating actor, not by the created actor. To make the created actor perform something, it is
	 * necessary to use meta_receive to send itself messages for later execution.
	 */
	public NATActorMirror(ELVirtualMachine host) throws InterpreterException {
		host_ = host;
	}
	
    /* ------------------------------------------
     * -- Language Construct to Actor Protocol --
     * ------------------------------------------ */

	public ATAsyncMessage base_createMessage(ATObject sender, ATSymbol selector, ATTable arguments) {
		return new NATAsyncMessage(sender, selector, arguments);
	}
	
	public ATClosure base_provide(final ATServiceDescription description, final ATObject service) throws InterpreterException {
		host_.event_servicePublished(this, service);
		final NATActorMirror self = this;
		return new NativeClosure(self) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				host_.event_cancelPublication(self, service);
				return NATNil._INSTANCE_;
			}
		};
	}
	
	public ATClosure base_require(final ATServiceDescription description, final ATObject client) throws InterpreterException {
		host_.event_clientSubscribed(this, client);
		final NATActorMirror self = this;
		return new NativeClosure(self) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				host_.event_cancelSubscription(self, client);
				return NATNil._INSTANCE_;
			}
		};
	}
	
    /* --------------------------
     * -- VM to Actor Protocol --
     * -------------------------- */

	public ATObject base_accept(ATAsyncMessage message) throws InterpreterException {
		inbox_.base_enqueue(message);
		return meta_receive(ActorEmittedEvents.processMessage(message));
	}

	public ATObject base_process() throws InterpreterException {
		if(! inbox_.base_isEmpty().asNativeBoolean().javaValue) {
			ATAsyncMessage msg = inbox_.base_dequeue().base_asAsyncMessage();
			
			msg = msg.meta_resolve().base_asAsyncMessage();
			ATObject result = msg.base_process(this);
			
			//base_fire_withArgs_(_PROCESSED_, NATTable.atValue(new ATObject[] { msg, result }));
			return result;
		} else {
			return NATNil._INSTANCE_;
		}
	}
	
	// Notifications are different from ordinary messages in that they
	// 1) do not trigger messageProcessed observers
	// 2) are not put in the inbox of an actor (they are processed immediately)
	public ATObject base_notifyObserver(ATAsyncMessage notification) throws InterpreterException {
		notification = notification.meta_resolve().base_asAsyncMessage();
		return notification.base_getReceiver().meta_receive(notification);
	}
	
	/**
	 * Signals the successful transmission of a message. Can be used to store messages
	 * in for instance a sentbox. 
	 */
	public ATNil base_delivered(ATAsyncMessage message) throws InterpreterException {
		return NATNil._INSTANCE_;
	}

	/**
	 * Signals the inability of the interpreter to deliver a series of messages. The
	 * default response to this event is to re-enqueue these messages in the outbox.
	 * To preserve the correct order of message they need to be added to the front,
	 * to ensure that any messages which were recently put in the outbox are after
	 * the ones that the virtual machine already tried to send. As a consequence the
	 * messages need to be batched as they will otherwise be added to the front one
	 * after the other yielding an incorrect order.
	 * 
	 * TODO document this with a drawing in our report
	 */
	public ATNil base_failedDelivery(ATTable messages) throws InterpreterException {
		// TODO Silently (without triggering observers) add table of messages to front of outbox
		return null;
	}

	/**
	 * Offers all far object references a chance to claim whether they are hosted by 
	 * the given actor, thereby filtering out all messages that can be sent to the 
	 * provided destination and schedule them for transmission with the VM
	 * @param receiver - a host actor which has become available
	 * @param destination - an object which will receive the forwarded messages
	 */
	/*public ATNil base_transmit(final ATActorMirror receiver, final ATObject destination) throws InterpreterException {
		for (Iterator farObjectIterator = farObjects_.iterator(); farObjectIterator.hasNext();) {
			ATFarReference farObject = (ATFarReference) farObjectIterator.next();
			
			farObject.meta_isHostedBy_(receiver).base_ifTrue_(
					new NativeClosure(farObject) {
						public ATObject base_apply(ATTable arguments) throws InterpreterException {
							return scope_.base_asFarReference().meta_transmit(destination);
						}
					});
		}

		return NATNil._INSTANCE_;		
	}*/

	public ATNil base_foundResolution(ATResolution found) throws InterpreterException {
		//TODO(service discovery) Implement this method
		return NATNil._INSTANCE_;		
	}

	public ATNil base_lostResolution(ATResolution lost) throws InterpreterException {
		//TODO(service discovery) Implement this method
		return NATNil._INSTANCE_;		
	}

    public ATObject meta_clone() throws InterpreterException {
        throw new XIllegalOperation("Cannot clone actor " + toString());
    }

    /**
     * actor.new(behaviour)
     *  => create a new actor with the given behaviour and mirror. Both should be isolates.
     *  Similar to actor: { behaviour's code }
     */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		int length = initargs.base_getLength().asNativeNumber().javaValue;
		if(length != 1)
			throw new XArityMismatch("newInstance", 1, length);
		
		ATObject isolate = initargs.base_at(NATNumber.ONE);
		Packet serializedIsolate = new Packet("behaviour", isolate);
		ELVirtualMachine host = ELVirtualMachine.currentVM();
		return NATActorMirror.atValue(host, serializedIsolate, new NATActorMirror(host));
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<actormirror:" + this.hashCode() + ">");
	}

	/* -----------------------------
	 * -- Object Passing Protocol --
	 * ----------------------------- */
	
	/*
	
	// TODO: what should the signature of base_export look like? with or without client parameter?
	// returns far ref or ATObjectID?
	// should it be implemented by NATActorMirror or ELActor? (native or reified?)
	public ATFarReference base_export(ATObject object) throws InterpreterException {
		// receptionist set will check whether ATObject is really local to me
		ATObjectID localObjectId = processor_.receptionists_.exportObject(object);
		ATFarReference farRef;
		
		if (client.asNativeFarReference().getObjectId().isRemote()) {
			farRef = NATFarReference.createRemoteFarRef(localObjectId);
			processor_.receptionists_.addClient(object, client);
		} else {
			farRef = NATFarReference.createLocalFarRef(this, localObjectId);
		}
		return farRef;
	}
	
	public ATObject base_resolve(ATFarReference farReference) throws InterpreterException {
		return processor_.receptionists_.resolveObject(farReference.asNativeFarReference().getObjectId());
	}
	
	public ATNil base_farReferencePassed(ATFarReference passed, ATFarReference client) {
		// TODO(dgc) store the new client of the object somehow...
		return NATNil._INSTANCE_;
	}
	
	
	*/
	
	/**
	 * To send a message msg to a receiver object rcv:
	 *  - if rcv is a local reference, schedule accept(msg) in my incoming event queue
	 *  - if rcv is a far reference, schedule msg in far reference's outbox
	 */
	public ATObject meta_send(ATAsyncMessage msg) throws InterpreterException {
		ATObject rcv = msg.base_getReceiver();
		if (rcv.base_isFarReference()) {
			return rcv.meta_receive(msg);
		} else {
			return this.meta_receive(msg);
		}
	}
	
	public ATObject meta_receive(ATAsyncMessage msg) throws InterpreterException {
		ELActor.currentActor().event_acceptSelfSend(msg);
		return NATNil._INSTANCE_;
	}
	
	/**
	 * When default base-level objects send an asynchronous message, they delegate
	 * this responsibility to their actor by means of this base-level method. The actor's
	 * base-level 'send' operation dispatches to its meta-level 'send' operation. In effect,
	 * the semantics of an object sending an async message are the same as those of an actor
	 * sending an async message directly.
	 * 
	 * TODO(discuss) is this the desirable semantics for the base-level hook?
	 */
	public ATObject base_send(ATAsyncMessage message) throws InterpreterException {
		return meta_send(message);
	}
	
    public ATActorMirror base_asActorMirror() throws XTypeMismatch {
    	return this;
    }
		
}
