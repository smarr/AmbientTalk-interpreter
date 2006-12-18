/**
 * AmbientTalk/2 Project
 * NATActor.java created on Oct 16, 2006 at 1:55:05 PM
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarObject;
import edu.vub.at.actors.ATMailbox;
import edu.vub.at.actors.ATResolution;
import edu.vub.at.actors.ATServiceDescription;
import edu.vub.at.actors.ATVirtualMachine;
import edu.vub.at.actors.natives.events.ActorEmittedEvents;
import edu.vub.at.actors.natives.events.CommonEmittedEvents;
import edu.vub.at.actors.natives.events.VMEmittedEvents;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATAsyncMessage;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNamespace;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The NATActor class implements the concurrency model of ambienttalk. It continually
 * consumes meta-events which are written to a synchronized queue. This way the actor
 * is notified of incoming messages, discovered and lost services, etc. When no meta-
 * messages are available, the actor consumes base-level messages as they are stored
 * in its inbox. These messages have a receiver object internal to the actor and they 
 * will be invoked on this receiver by the actor's thread.
 *
 * @author smostinc
 */
public class NATActor extends NATAbstractActor implements ATActor {
	
	// Observable event name
	public static final ATSymbol _PROCESSED_ = AGSymbol.jAlloc("messageProcessed");

	
    /* ------------------------------------------
     * -- Language Construct to Actor Protocol --
     * ------------------------------------------ */

	public ATAsyncMessage base_createMessage(ATObject sender, ATSymbol selector, ATTable arguments) {
		return new NATAsyncMessage(sender, selector, arguments);
	}
	
	public ATObject base_send(ATAsyncMessage message) throws InterpreterException {
		/*
		 * By construction all message sends are local ones, which implies that at
		 * the point the actor is asked to send them (and far references have thus
		 * already invoked meta_pass on the message), all that needs to be done is
		 * accept the message ourselves
		 */ 
		return this.base_scheduleEvent(VMEmittedEvents.acceptMessage(message));
	}
	
	public ATClosure base_provide(final ATServiceDescription description, final ATObject service) {
		host_.base_scheduleEvent(ActorEmittedEvents.publishService(description, service));
		return new NativeClosure(this) {
			public ATObject base_apply(ATTable args) {
				host_.base_scheduleEvent(ActorEmittedEvents.cancelPublishedService(description, service));
				return NATNil._INSTANCE_;
			}
		};
	}
	
	public ATClosure base_require(final ATServiceDescription description, final ATObject client) {
		host_.base_scheduleEvent(ActorEmittedEvents.subscribeToService(description, client));
		return new NativeClosure(this) {
			public ATObject base_apply(ATTable args) {
				host_.base_scheduleEvent(ActorEmittedEvents.cancelSubscription(description, client));
				return NATNil._INSTANCE_;
			}
		};
	}
	
    /* --------------------------
     * -- VM to Actor Protocol --
     * -------------------------- */

	public ATNil base_accept(ATAsyncMessage message) throws InterpreterException {
		inbox_.base_enqueue(message);
		base_scheduleEvent(ActorEmittedEvents.processMessage(message));
		return NATNil._INSTANCE_;
	}

	public ATObject base_process() throws InterpreterException {
		if(! inbox_.base_isEmpty().asNativeBoolean().javaValue) {
			ATAsyncMessage msg = (ATAsyncMessage)inbox_.base_dequeue();
			
			msg = msg.meta_resolve().base_asAsyncMessage();
			ATObject result = msg.base_getReceiver().meta_receive(msg);
			
			base_fire_withArgs_(_PROCESSED_, NATTable.atValue(new ATObject[] { msg, result }));
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
	public ATNil base_transmit(final ATActor receiver, final ATObject destination) throws InterpreterException {
		for (Iterator farObjectIterator = farObjects_.iterator(); farObjectIterator.hasNext();) {
			ATFarObject farObject = (ATFarObject) farObjectIterator.next();
			
			farObject.meta_isHostedBy_(receiver).base_ifTrue_(
					new NativeClosure(farObject) {
						public ATObject base_apply(ATTable arguments) throws InterpreterException {
							return scope_.base_asFarReference().meta_flush(destination);
						}
					});
		}

		return NATNil._INSTANCE_;		
	}

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
	
	
	/*
	 * Estimate of the number of services (or objects) an actor usually exposes to 
	 * the outside, hence the number of external references to this actor (used to
	 * optimize the behaviour of the ExportedObjectSet in NATActor). 
	 */
	private static final int _AVERAGE_SERVICES_PER_ACTOR_ = 3;
		
	// INSTANCE VARIABLES
		
	/*
	 * Each actor has a reference to the virtual machine that hosts it. This reference
	 * is transient so that it is not passed when an actor would be serialised and 
	 * transported to another virtual machine.
	 */
	protected transient ATVirtualMachine host_;
	
	/*
	 * This object is created when the actor is initialized: i.e. it is the object 
	 * that results from evaluating the closure passed when the actor was constructed.
	 */
	private ATObject behaviour_ = null; 
	
	/*
	 * Actors have a classical combination of an inbox and outbox which reify the
	 * future communication state of an actor. A reification of past communication
	 * can be obtained by installing a custom <code>receivedbox</code> and <code>
	 * sentbox</code> which are filled upon reception of the meta-level events 
	 * <code>process</code> and <code>delivered</code> respectively.
	 */
	final ATMailbox inbox_ = new NATMailbox(this, ATActor._IN_);
	final ATMailbox outbox_ = new NATMailbox(this, ATActor._OUT_);
	final ATMailbox providedbox_ = new NATMailbox(this, ATActor._PROVIDED_);
	final ATMailbox requiredbox_ = new NATMailbox(this, ATActor._REQUIRED_);
		
	ReceptionistsSet receptionists_ = new ReceptionistsSet(_AVERAGE_SERVICES_PER_ACTOR_);
	Set farObjects_ = new TreeSet();
	
	
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
		File[] objectPathRoots = host_.getObjectPathRoots();
		
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
		ATAbstractGrammar initialisationCode = host_.getInitialisationCode();
		
		// evaluate the initialization code in the context of the global scope
		NATObject globalScope = Evaluator.getGlobalLexicalScope();
		NATContext initCtx = new NATContext(globalScope, globalScope, globalScope.meta_getDynamicParent());
		
		initialisationCode.meta_eval(initCtx);
	}
	
	public NATActor(ATVirtualMachine host, ATClosure body) throws InterpreterException {
		host_ = host;
		
		this.base_scheduleEvent(ActorEmittedEvents.publishBehaviour(this));
		this.base_scheduleEvent(CommonEmittedEvents.initializeObject(this, NATTable.atValue(new ATObject[] { body })));
		host.base_scheduleEvent(ActorEmittedEvents.newlyCreated(this));
	}
	
	/**
	 * Called from the actor's executor, prior to the initialisation to ensure that
	 * the creating actor can continue processing immediately without having to wait
	 * for the initialisation to finish.
	 */
	public ATNil base_publishBehaviour() {
		synchronized (this) {
			int objectId = receptionists_.exportObject(Evaluator.getGlobalLexicalScope());
			behaviour_ = new NATFarObject(this, objectId);
			this.notify();
			return NATNil._INSTANCE_;
		}
	}

	/**
	 * Called from the NATActor's executor to ensure the correctness of the tread-
	 * local variable Evaluator.getGlobalLexicalScope().
	 */
	public ATObject base_init(ATObject[] initArgs) throws InterpreterException {
		if(initArgs.length < 1)
			throw new XIllegalArgument("Too few arguments for call to init");
		
		ATClosure initialisationCode = initArgs[0].base_asClosure();
		
		initLobbyUsingObjectPath();
		initRootObject();
		
		ATObject beh = Evaluator.getGlobalLexicalScope();
		initialisationCode.base_applyInScope(NATTable.EMPTY, beh);
				
		return NATNil._INSTANCE_;
	}

	public ATObject meta_clone() throws InterpreterException {
		// FIXME
//		return new NATActor(host_, executor_.initArgs_[1].base_asClosure());
		return null;
	}

	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		int length = initargs.base_getLength().asNativeNumber().javaValue;
		if(length < 1)
			throw new XArityMismatch("newInstance", 1, length);
		
		return new NATActor(host_, initargs.base_at(NATNumber.ONE).base_asClosure());
	}
	
	public ATObject base_getBehaviour() {
		synchronized (this) {
			while(behaviour_ == null) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					// Continue waiting
				}
			}
		}
		return behaviour_;
	}

	/* -----------------------------
	 * -- Object Passing Protocol --
	 * ----------------------------- */
	
	public ATFarObject base_reference_for_(ATObject object, ATFarObject client) throws InterpreterException {
		int objectId = receptionists_.exportObject(object);
		receptionists_.addClient(object, client);
		return new NATFarObject(this, objectId);
	}
	
	public ATObject base_resolveFarReference(ATFarObject farReference) throws InterpreterException {
		return receptionists_.resolveObject(farReference);
	}
	
	public ATNil base_farReferencePassed(ATFarObject passed, ATFarObject client) {
		// TODO(dgc) store the new client of the object somehow...
		return NATNil._INSTANCE_;
	}
	
	public ATNil base_registerFarReference(ATFarObject farObject) {
		farObjects_.add(farObject);
		return NATNil._INSTANCE_;
	}
	

	
	
	
	
	
	
	
	public ATVirtualMachine base_getVirtualMachine() {
		return host_;
	}

	public ATBoolean base_isLocal() {
		return NATBoolean._TRUE_;
	}

	public ATBoolean base_isRemote() {
		return NATBoolean._FALSE_;
	}

	public ATActor asActor() throws XTypeMismatch {
		return this;
	}
		
}
