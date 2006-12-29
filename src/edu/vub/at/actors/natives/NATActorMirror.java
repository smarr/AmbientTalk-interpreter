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
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.events.ActorEmittedEvents;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNamespace;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.io.File;
import java.io.FilenameFilter;

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
	 * This object is created when the actor is initialized: i.e. it is the object 
	 * that results from evaluating the closure passed when the actor was constructed.
	 */
	private ATObject behaviour_; 
	
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
	
	private final ELActor processor_;
	
	public static BlockingFuture atValue(ELVirtualMachine host, ATClosure body) throws InterpreterException {
		BlockingFuture future = new BlockingFuture();
	    NATActorMirror newActor = new NATActorMirror(future, host, body);
		return future;
	}
	
	/**
	 * When initializing a new actor, do not forget that this constructor is still executed by the
	 * creating actor, not by the created actor. To make the created actor perform something, it is
	 * necessary to use meta_receive to send itself messages for later execution.
	 */
	private NATActorMirror(final BlockingFuture f, ELVirtualMachine host, final ATClosure body) throws InterpreterException {
		processor_ = new ELActor(this, host);
		
		// schedule special 'init' message which will:
		// A) create a new behaviour and will unblock creating actor (by passing it a far ref via the future)
		// B) initialize the behaviour with the given closure
		processor_.event_init(new Event("init("+body+")") {
			public void process(Object byMyself) {
				behaviour_ = new NATObject(); // behaviour is an empty object
				
				try {
					// pass far ref to behaviour to creator actor who is waiting for this
					f.resolve(processor_.receptionists_.exportObject(behaviour_));
					
					// go on to initialize the root and the behaviour
					initLobbyUsingObjectPath();
					initRootObject();
					body.base_applyInScope(NATTable.EMPTY, behaviour_);
				} catch (InterpreterException e) {
					if (!f.isDetermined())
					  f.ruin(e);
				}
			}
		});
		
		// notify host VM about my creation
		processor_.host_.event_actorCreated(processor_);
	}
	
    /* ------------------------------------------
     * -- Language Construct to Actor Protocol --
     * ------------------------------------------ */

	public ATAsyncMessage base_createMessage(ATObject sender, ATSymbol selector, ATTable arguments) {
		return new NATAsyncMessage(sender, selector, arguments);
	}
	
	public ATClosure base_provide(final ATServiceDescription description, final ATObject service) throws InterpreterException {
		processor_.host_.event_servicePublished(this, service);
		final NATActorMirror self = this;
		return new NativeClosure(self) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				processor_.host_.event_cancelPublication(self, service);
				return NATNil._INSTANCE_;
			}
		};
	}
	
	public ATClosure base_require(final ATServiceDescription description, final ATObject client) throws InterpreterException {
		processor_.host_.event_clientSubscribed(this, client);
		final NATActorMirror self = this;
		return new NativeClosure(self) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				processor_.host_.event_cancelSubscription(self, client);
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

	protected NATNil base_dispose() throws InterpreterException {
		// TODO(clean-up, gc) Revoke services published and subscribed, deal with references
		return NATNil._INSTANCE_;
	}
	
	/* -----------------------------
	 * -- Initialisation Protocol --
	 * ----------------------------- */
	
	/**
	 * Initializes the lobby namespace with a slot for each directory in the object path.
	 * The slot name corresponds to the last name of the directory. The slot value corresponds
	 * to a namespace object initialized with the directory.
	 * 
	 * If the user did not specify an objectpath, the default is .;$AT_OBJECTPATH;$AT_HOME
	 */
	protected void initLobbyUsingObjectPath() throws InterpreterException {
		File[] objectPathRoots = processor_.host_.getObjectPathRoots();
		
		NATObject lobby = Evaluator.getLobbyNamespace();
		
		// for each path to the lobby, add an entry for each directory in the path
		for (int i = 0; i < objectPathRoots.length; i++) {
			File pathRoot = objectPathRoots[i];
							
			File[] filesInDirectory = pathRoot.listFiles(new FilenameFilter() {
				// filter out all hidden files (starting with .)
				public boolean accept(File parent, String name) {
					return !(name.startsWith("."));
				}
			});
			for (int j = 0; j < filesInDirectory.length; j++) {
				File subdir = filesInDirectory[j];
				if (subdir.isDirectory()) {
					// convert the filename into an AmbientTalk selector
					ATSymbol selector = Reflection.downSelector(subdir.getName());
					try {
						lobby.meta_defineField(selector, new NATNamespace("/"+subdir.getName(), subdir));
					} catch (XDuplicateSlot e) {
						// TODO(review) Should throw dedicated exceptions (difference warning - abort)
						// TODO(warn)
						throw new XIllegalOperation("shadowed path on classpath: "+subdir.getAbsolutePath());
					} catch (InterpreterException e) {
						// should not happen as the meta_defineField is native
						// TODO(abort)
						throw new XIllegalOperation("Fatal error while constructing objectpath: " + e.getMessage());
					}	
				} else {
					// TODO(warn)
					//throw new XIllegalOperation("skipping non-directory file on classpath: " + subdir.getName());
				}
			}
		}

	}

	/**
	 * Initialises the root using the contents of the init file's contents stored by
	 * the hosting virtual machine.
	 * @throws InterpreterException
	 */
	protected void initRootObject() throws InterpreterException {
		ATAbstractGrammar initialisationCode = processor_.host_.getInitialisationCode();
		
		// evaluate the initialization code in the context of the global scope
		NATObject globalScope = Evaluator.getGlobalLexicalScope();
		NATContext initCtx = new NATContext(globalScope, globalScope, globalScope.meta_getDynamicParent());
		
		initialisationCode.meta_eval(initCtx);
	}

    public ATObject meta_clone() throws InterpreterException {
        throw new XIllegalOperation("Cannot clone actor " + toString());
    }

	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		int length = initargs.base_getLength().asNativeNumber().javaValue;
		if(length != 1)
			throw new XArityMismatch("newInstance", 1, length);
		
		// FIXME: passed closure should be deep-copied before handed to new actor!
		BlockingFuture bhvFuture = NATActorMirror.atValue(processor_.host_, initargs.base_at(NATNumber.ONE).base_asClosure());
		try {
			return (ATObject) bhvFuture.get();
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
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
	
	public ATObject getBehaviour() {
		return behaviour_;
	}

	public ELActor getProcessor() {
		return processor_;
	}
	
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
		processor_.event_acceptSelfSend(msg);
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
		
}
