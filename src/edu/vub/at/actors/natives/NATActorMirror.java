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
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.natives.DiscoveryManager.Publication;
import edu.vub.at.actors.natives.DiscoveryManager.Subscription;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NATIntrospectiveMirror;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.OBJNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.natives.grammar.AGBegin;
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
	
	// INSTANCE VARIABLES
	
	private final ELDiscoveryActor discoveryActor_;
	
	/**
	 * Creates a new actor on the specified host Virtual Machine. The actor its behaviour
	 * is intialized by means of the passed parameters and initialization code. The calling
	 * thread is **blocked** until the actor has been constructed. However, actor behaviour
	 * and root initialization is carried out by the newly created actor itself.
	 * 
	 * @param host the VM hosting this actor - after creation, the actor registers itself with this VM
	 * @param parametersPkt the serialized parameters used to invoke the initialization code
	 * @param initcodePkt the serialized initialization code used to initialize the actor behaviour
	 * @param actorMirror this actor's mirror
	 * @return a far reference to the behaviour of the actor
	 * @throws InterpreterException
	 */
	public static NATLocalFarRef createActor(ELVirtualMachine host,
			                                 Packet parametersPkt,
			                                 Packet initcodePkt,
			                                 ATActorMirror actorMirror) throws InterpreterException {
		
		BlockingFuture future = new BlockingFuture();
		ELActor processor = new ELActor(actorMirror, host);
		
		// schedule special 'init' message which will:
		// A) create a new behaviour and will unblock creating actor (by passing it a far ref via the future)
		// B) unpack the parameters used to invoke the initializatin code
		// C) unpack the init code to initialize the behaviour
		// D) initialize the root and lobby objects of this actor
		processor.event_init(future, parametersPkt, initcodePkt);
	    
		try {
			return (NATLocalFarRef) future.get();
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
	/**
	 * Auxiliary creation method to create an actor with an empty behaviour.
	 * Equivalent to evaluating:
	 * 
	 * actor: { nil }
	 */
	public static NATLocalFarRef createEmptyActor(ELVirtualMachine host, ATActorMirror actorMirror) throws InterpreterException {
		Packet noParams = new Packet(NATTable.EMPTY);
		Packet noinitcode = new Packet(new NATMethod(Evaluator._ANON_MTH_NAM_, NATTable.EMPTY, new AGBegin(NATTable.of(OBJNil._INSTANCE_))));
		return createActor(host, noParams, noinitcode, actorMirror);
	}
	
	
	/**
	 * When initializing a new actor, do not forget that this constructor is still executed by the
	 * creating actor, not by the created actor. To make the created actor perform something, it is
	 * necessary to use meta_receive to send itself messages for later execution.
	 */
	public NATActorMirror(ELVirtualMachine host) {
		discoveryActor_ = host.discoveryActor_;
	}
	
    /* ------------------------------------------
     * -- Language Construct to Actor Protocol --
     * ------------------------------------------ */

	public ATAsyncMessage base_createMessage(ATSymbol selector, ATTable arguments, ATTable types) throws InterpreterException {
		return new NATAsyncMessage(selector, arguments, types);
	}
	
	public ATObject base_createMirror(ATObject reflectee) throws InterpreterException {
		return NATIntrospectiveMirror.atValue(reflectee);
	}
	
	/**
	 * A publication object is defined as:
	 * object: {
	 *   def topic := //topic under which service is published;
	 *   def service := //the exported service object;
	 *   def cancel() { //unexport the service object }
	 * }
	 */
	public static class NATPublication extends NATObject {
		private static final AGSymbol _TOPIC_ = AGSymbol.jAlloc("topic");
		private static final AGSymbol _SERVICE_ = AGSymbol.jAlloc("service");
		private static final AGSymbol _CANCEL_ = AGSymbol.jAlloc("cancel");
		public NATPublication(final ELDiscoveryActor discoveryActor,
				              ATTypeTag topic, ATObject service,
				              final Publication pub) throws InterpreterException {
			meta_defineField(_TOPIC_, topic);
			meta_defineField(_SERVICE_, service);
			meta_defineField(_CANCEL_, 	new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					discoveryActor.event_cancelPublication(pub);
					return OBJNil._INSTANCE_;
				}
			});
		}
		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("<publication:"+impl_invokeAccessor(this, _TOPIC_, NATTable.EMPTY)+">");
		}
	}
	
	/**
	 * A subscription object is defined as:
	 * object: {
	 *   def topic := //topic subscribed to;
	 *   def handler := //the closure to be triggered;
	 *   def cancel() { //unsubscribe the handler }
	 * }
	 */
	public static class NATSubscription extends NATObject {
		private static final AGSymbol _TOPIC_ = AGSymbol.jAlloc("topic");
		private static final AGSymbol _HANDLER_ = AGSymbol.jAlloc("handler");
		private static final AGSymbol _CANCEL_ = AGSymbol.jAlloc("cancel");
		public NATSubscription(final ELDiscoveryActor discoveryActor,
				               ATTypeTag topic, ATClosure handler,
				               final Subscription sub) throws InterpreterException {
			meta_defineField(_TOPIC_, topic);
			meta_defineField(_HANDLER_, handler);
			meta_defineField(_CANCEL_, 	new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					discoveryActor.event_cancelSubscription(sub);
					return OBJNil._INSTANCE_;
				}
			});
		}
		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("<subscription:"+impl_invokeAccessor(this, _TOPIC_, NATTable.EMPTY)+">");
		}
	}
	
	public ATObject base_provide(final ATTypeTag topic, final ATObject service) throws InterpreterException {
		Publication pub = new Publication(ELActor.currentActor(),
				                          new Packet(topic),
				                          new Packet(service));
		discoveryActor_.event_servicePublished(pub);
		return new NATPublication(discoveryActor_, topic, service, pub);
	}
	
	public ATObject base_require(final ATTypeTag topic, final ATClosure handler, ATBoolean isPermanent) throws InterpreterException {
		Subscription sub = new Subscription(ELActor.currentActor(),
				                            new Packet(topic),
				                            new Packet(handler),
				                            isPermanent.asNativeBoolean().javaValue);
		discoveryActor_.event_clientSubscribed(sub);
		return new NATSubscription(discoveryActor_, topic, handler, sub);
	}
	
    /* --------------------------
     * -- VM to Actor Protocol --
     * -------------------------- */

    public ATObject meta_clone() throws InterpreterException {
        throw new XIllegalOperation("Cannot clone actor " + toString());
    }

    /**
     * actor.new(closure)
     *  => same effect as evaluating 'actor: closure'
     */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		int length = initargs.base_length().asNativeNumber().javaValue;
		if(length != 1)
			throw new XArityMismatch("newInstance", 1, length);
		
		ATClosure closure = initargs.base_at(NATNumber.ONE).asClosure();
		return OBJLexicalRoot._INSTANCE_.base_actor_(closure);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<actormirror:" + this.hashCode() + ">");
	}

	/* -----------------------------
	 * -- Object Passing Protocol --
	 * ----------------------------- */
	
	/**
	 * To send a message msg to a receiver object rcv:
	 *  - if rcv is a local reference, schedule accept(msg) in my incoming event queue
	 *  - if rcv is a far reference, schedule msg in far reference's outbox
	 */
	public ATObject meta_send(ATObject receiver, ATAsyncMessage msg) throws InterpreterException {
		if (receiver.isFarReference()) {
			return receiver.meta_receive(msg);
		} else {
			//return this.meta_receive(msg);
			ELActor.currentActor().event_acceptSelfSend(receiver, msg);
			return OBJNil._INSTANCE_;
		}
	}
	
	public ATObject meta_receive(ATAsyncMessage msg) throws InterpreterException {
		ELActor.currentActor().event_acceptSelfSend(this, msg);
		return OBJNil._INSTANCE_;
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._ACTORMIRROR_);
    }
	
	/**
	 * When default base-level objects send an asynchronous message, they delegate
	 * this responsibility to their actor by means of this base-level method. The actor's
	 * base-level 'send' operation dispatches to its meta-level 'send' operation. In effect,
	 * the semantics of an object sending an async message are the same as those of an actor
	 * sending an async message directly.
	 */
	public ATObject base_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException {
		return meta_send(receiver, message);
	}
	
	/**
	 * def install: protocol
	 *  => returns the old installed protocol
	 * 
	 * @see ATActorMirror#base_install_(ATClosure)
	 */
	public ATObject base_install_(ATActorMirror newActorMirror) throws InterpreterException {
		ELActor myEventLoop = ELActor.currentActor();
		ATActorMirror oldMirror = myEventLoop.getActorMirror();
		myEventLoop.setActorMirror(newActorMirror);
		return oldMirror;
	}
	
    public ATActorMirror asActorMirror() throws XTypeMismatch {
    	return this;
    }
		
}
