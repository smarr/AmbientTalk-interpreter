/**
 * AmbientTalk/2 Project
 * ELActor.java created on 27-dec-2006 at 16:17:23
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
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.OBJLexicalRoot;

/**
 * An instance of the class ELActor represents a programmer-defined
 * AmbientTalk/2 actor. The event queue of the actor event loop serves as the
 * actor's 'meta-level' queue.
 *
 * The events in the 'meta-level' queue are handled by the actor's mirror object.
 * This mirror is normally an instance of NATActorMirror, but it can be any
 * programmer-defined object that adheres to the ATActorMirror interface.
 *
 * @author tvcutsem
 */
public class ELActor extends EventLoop {
	
	public static final ELActor currentActor() {
		try {
			return ((ELActor) EventLoop.currentEventLoop());
		} catch (ClassCastException e) {
			Logging.Actor_LOG.fatal("Asked for an actor in a non-event loop thread?", e);
			throw new RuntimeException("Asked for an actor outside of an event loop");
		}
	}

	private ATActorMirror mirror_;
	protected final ELVirtualMachine host_;
	protected final ReceptionistsSet receptionists_;
	
	/*
	 * This object is created when the actor is initialized: i.e. it is the passed
	 * version of the isolate that was passed to the actor: primitive by the creating actor.
	 */
	private ATObject behaviour_;
	
	public ELActor(ATActorMirror mirror, ELVirtualMachine host) {
		super("actor " + mirror.toString());
		mirror_ = mirror;
		host_ = host;
		receptionists_ = new ReceptionistsSet(this);
	}
	
	/** constructor dedicated to initialization of discovery actor */
	protected ELActor(ELVirtualMachine host) {
		super("discovery actor");
		mirror_ = new NATActorMirror(host);
		host_ = host;
		receptionists_ = new ReceptionistsSet(this);
	}

	/**
	 * Actor event loops handle events by allowing the meta-level events to
	 * process themselves.
	 */
	public void handle(Event event) {
		event.process(mirror_);
	}
	
	public ATActorMirror getActorMirror() { return mirror_; }

	public void setActorMirror(ATActorMirror mirror) { mirror_ = mirror; }
	
	public ELVirtualMachine getHost() {
		return host_;
	}
	
	/**
	 * Export the given local object such that it is now remotely accessible via the
	 * returned object id.
	 * @param object a **near** reference to the object to export
	 * @return a unique identifier by which this object can be retrieved via the resolve method.
	 * @throws XIllegalOperation if the passed object is a far reference, i.e. non-local
	 */
	public NATLocalFarRef export(ATObject object) throws InterpreterException {
		// receptionist set will check whether ATObject is really local to me
		return receptionists_.exportObject(object);
	}
	
	/**
	 * Resolve the given object id into a local reference. There are three cases to
	 * consider:
	 *  A) The given id designates an object local to this actor: the returned object
	 *     will be a **near** reference to the object (i.e. the object itself)
	 *  B) The given id designates a far (non-local) object that lives in the same
	 *     address space as this actor: the returned object wil be a **far** reference
	 *     to the object.
	 *  C) The given id designates a far object that lives on a remote machine: the
	 *     returned object will be a **far** and **remote** reference to the object.
	 *     
	 * @param id the identifier of the object to resolve
	 * @return a near or far reference to the object, depending on where the designated object lives
	 */
	public ATObject resolve(ATObjectID id, ATStripe[] stripes) {
		return receptionists_.resolveObject(id, stripes);
	}
	
	
	
	/* -----------------------------
	 * -- Initialisation Protocol --
	 * ----------------------------- */

	/**
	 * Initialises the root using the contents of the init file stored by
	 * the hosting virtual machine.
	 * @throws InterpreterException
	 */
	protected void initRootObject() throws InterpreterException {
		ATAbstractGrammar initialisationCode = host_.getInitialisationCode();
		
		// evaluate the initialization code in the context of the global scope
		NATObject globalScope = Evaluator.getGlobalLexicalScope();
		NATContext initCtx = new NATContext(globalScope, globalScope);
		
		initialisationCode.meta_eval(initCtx);
	}
	
	/**
	 * Initialises various fields in the lexical root of the actor, which are defined in the 
	 * context of every actor. Candidates are a "system" field which allows the program to 
	 * perform IO operations or a "~" field denoting the current working directory.
	 * 
	 * @throws InterpreterException when initialisation of a field fails
	 */
	protected void initSharedFields() throws InterpreterException {
		SharedActorField[] fields = host_.getFieldsToInitialize();
		NATObject globalScope = Evaluator.getGlobalLexicalScope();
		
		for (int i = 0; i < fields.length; i++) {
			SharedActorField field = fields[i];
			ATObject value = field.initialize();
			if (value != null) {
				// TODO(discuss) As the behaviour of an actor is now an isolate, we can no longer simply install anything in the lexical scope, but we must also do this in the behaviour of the actor itself
				globalScope.meta_defineField(field.getName(), value);
				behaviour_.meta_defineField(field.getName(), value);
			}
		}
	}
	
	// Events to be processed by the actor event loop
	
	/**
	 * The initial event sent by the actor mirror to its event loop to intialize itself.
	 * @param e
	 */
	protected void event_init(final BlockingFuture future, final Packet behaviourPkt) {
		receive(new Event("init("+this+")") {
			public void process(Object byMyself) {
				try {
					behaviour_ = behaviourPkt.unpack();
					
					// pass far ref to behaviour to creator actor who is waiting for this
					future.resolve(receptionists_.exportObject(behaviour_));
					
					// go on to initialize the root and all lexically visible fields
					initRootObject();
					initSharedFields();
				} catch (InterpreterException e) {
					if (!future.isDetermined())
					  future.ruin(e);
				}
			}
		});
	}
	
	/**
	 * The main entry point for any asynchronous self-sends.
	 * Asynchronous self-sends do not undergo any form of parameter passing, there is no need
	 * to serialize and deserialize the message parameter in a Packet.
	 */
	public void event_acceptSelfSend(final ATAsyncMessage msg) {
		receive(new Event("accept("+msg+")") {
			public void process(Object myActorMirror) {
				try {
					ATObject result = msg.base_process(mirror_);
					// TODO what to do with return value?
					Logging.Actor_LOG.info(this + ": accept("+msg+") returned " + result);
				} catch (InterpreterException e) {
					// TODO what to do with exception?
					Logging.Actor_LOG.info(this + ": accept("+msg+") failed ", e);
				}
			}
		});
	}
	
	/**
	 * The main entry point for any asynchronous messages sent to this actor
	 * by external sources (e.g. the VM or other local actors).
	 * @param msg the asynchronous AmbientTalk base-level message to enqueue
	 */
	public void event_accept(final Packet serializedMessage) {
		receive(new Event("accept("+serializedMessage+")") {
			public void process(Object myActorMirror) {
				try {
					ATAsyncMessage msg = serializedMessage.unpack().base_asAsyncMessage();
					ATObject result = msg.base_process(mirror_);
					// TODO what to do with return value?
					Logging.Actor_LOG.info(myActorMirror + ": "+this + " returned " + result);
				} catch (InterpreterException e) {
					// TODO what to do with exception?
					Logging.Actor_LOG.info(myActorMirror + ": "+this + " failed ", e);
				}
			}
		});
	}
	
	/**
	 * This method is invoked by a coercer in order to schedule a symbiotic invocation
	 * from the Java world, which should be synchronous to the Java thread, but which
	 * must be scheduled asynchronously to comply with the AT/2 actor model.
	 * @param invocation a functor object that will perform the symbiotic invocation
	 * @return the result of the symbiotic invocation
	 * @throws Exception if the symbiotic invocation fails
	 */
	public Object sync_event_symbioticInvocation(Callable invocation) throws Exception {
		return receiveAndWait("symbioticInvocation", invocation);
	}
	
	/**
	 * This method should only be used for purposes such as the IAT shell or unit testing.
	 * It allows an external thread to make this actor evaluate an arbitrary expression.
	 * 
	 * @param ast an abstract syntax tree to be evaluated by the receiving actor (in the
	 * scope of its behaviour).
	 * @return the result of the evaluation
	 * @throws InterpreterException if the evaluation fails
	 */
	public ATObject sync_event_eval(final ATAbstractGrammar ast) throws InterpreterException {
		try {
			return (ATObject) receiveAndWait("nativeEval("+ast+")", new Callable() {
				public Object call(Object inActor) throws Exception {
				    return OBJLexicalRoot._INSTANCE_.base_eval_in_(ast, behaviour_);
				}
			});
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}
	
	
	/**
	 * This method should only be used for purposes of unit testing. It allows
	 * arbitary code to be scheduled by external threads such as unit testing frameworks.
	 */
	public Object sync_event_performTest(final Callable c) throws Exception {
		return (ATObject) receiveAndWait("performTest("+c+")", c);
	}
	
	/**
	 * When the discovery manager receives a publication from another local actor or
	 * another remote VM, the actor is asked to compare the incoming publication against
	 * a subscription that it had announced previously.
	 * 
	 * @param requiredStripe serialized form of the stripe attached to the actor's subscription
	 * @param myHandler the closure specified as a handler for the actor's subscription
	 * @param discoveredStripe serialized form of the stripe attached to the new publication
	 * @param remoteService serialized form of the reference to the remote discovered service
	 */
	public void event_serviceJoined(final Packet requiredStripePkt, final ATFarReference myHandler,
			                        final Packet discoveredStripePkt, final Packet remoteServicePkt) {
		receive(new Event("serviceJoined") {
			public void process(Object myActorMirror) {
				try {
					ATStripe requiredStripe = requiredStripePkt.unpack().base_asStripe();
					ATStripe discoveredStripe = discoveredStripePkt.unpack().base_asStripe();
					// is there a match?
					if (discoveredStripe.base_isSubstripeOf(requiredStripe).asNativeBoolean().javaValue) {
						ATObject remoteService = remoteServicePkt.unpack();
						// myhandler<-apply([remoteService])
						myHandler.meta_receive(
							new NATAsyncMessage(myHandler, myHandler, Evaluator._APPLY_,
								NATTable.atValue(new ATObject[] {
									NATTable.atValue(new ATObject[] { remoteService })
								})
							)
						);
					}
				} catch (XIOProblem e) {
					Logging.Actor_LOG.error("Error deserializing joined stripes or services: ", e.getCause());
				} catch (XClassNotFound e) {
					Logging.Actor_LOG.fatal("Could not find class while deserializing joined stripes or services: ", e.getCause());
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error("Error while joining services: ", e);
				}
			}
		});
	}
}