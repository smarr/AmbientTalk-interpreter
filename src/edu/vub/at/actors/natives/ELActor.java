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


import java.lang.reflect.Method;
import java.util.EventListener;

import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.eventloops.BlockingFuture;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.id.ActorID;
import edu.vub.at.actors.net.comm.Address;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XObjectOffline;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.natives.OBJNil;
import edu.vub.at.objects.symbiosis.Symbiosis;
import edu.vub.at.util.logging.Logging;

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
	
	/**
	 * A thread-local variable that contains the 'default actor' to use
	 * when there is currently no ELActor event loop thread running.
	 * This is primarily useful for performing unit tests where an actor
	 * is automatically created when actor semantics is required.
	 * 
	 * A warning is printed to the log because using the default actor should
	 * only be used for testing purposes.
	 */
	private static final ThreadLocal _DEFAULT_ACTOR_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	    	Logging.Actor_LOG.warn("Creating a default actor for thread " + Thread.currentThread());
	    	try {
				ELVirtualMachine host = new ELVirtualMachine(
				  OBJNil._INSTANCE_,
				  new SharedActorField[] { },
				  ELVirtualMachine._DEFAULT_GROUP_NAME_);
				return NATActorMirror.createEmptyActor(host, new NATActorMirror(host)).getFarHost();
			} catch (InterpreterException e) {
			  // Backport from JDK 1.4 to 1.3
              // throw new RuntimeException("Failed to initialize default actor",e);
              throw new RuntimeException("Failed to initialize default actor");
			}
	    }
	};
	
	/**
	 * Retrieves the currently running actor. If there is no running actor thread,
	 * this returns the value stored in the thread-local default actor field.
	 */
	public static final ELActor currentActor() {
		try {
			return ((ELActor) EventLoop.currentEventLoop());
		} catch (ClassCastException e) {
			// current event loop is not an actor event loop
		} catch (IllegalStateException e) {
			// current thread is not an event loop
		}
		Logging.Actor_LOG.warn("Asked for an actor in non-actor thread " + Thread.currentThread());
		return (ELActor) _DEFAULT_ACTOR_.get();
	}

	private ATActorMirror mirror_;
	private final ActorID id_;
	protected final ELVirtualMachine host_;
	protected final ReceptionistsSet receptionists_;
	
	/*
	 * This object is created when the actor is initialized: i.e. it is the passed
	 * version of the isolate that was passed to the actor: primitive by the creating actor.
	 */
	private ATObject behaviour_;
	
	public ELActor(ATActorMirror mirror, ELVirtualMachine host) {
		super("actor " + mirror.toString());
		id_ = new ActorID();
		mirror_ = mirror;
		host_ = host;
		receptionists_ = new ReceptionistsSet(this);
	}
	
	/** constructor dedicated to initialization of discovery actor */
	protected ELActor(ELVirtualMachine host) {
		super("discovery actor");
		id_ = new ActorID();
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

	public ATObject getBehaviour() {
		return behaviour_;
	}
	
	public ActorID getActorID() {
		return id_;
	}
	
	public Thread getExecutor() {
		return processor_;
	}
	
	/**
	 * Export the given local object such that it is now remotely accessible via the
	 * returned object id.
	 * @param object a **near** reference to the object to export
	 * @return a unique identifier by which this object can be retrieved via the resolve method.
	 * @throws XObjectOffline if the passed object is a far reference, i.e. non-local
	 */
	public NATLocalFarRef export(ATObject object) throws InterpreterException {
		// receptionist set will check whether ATObject is really local to me
		return receptionists_.exportObject(object);
	}
	
	/**
	 * Takes offline a given remote object such that it is no longer remotely accessible.
	 * @param object a **far?** reference to the object to export
	 * @throws XIllegalOperation if the passed object is not part of the export table - i.e. non-remotely accessible.
	 */
	public void takeOffline(ATObject object) throws InterpreterException {
		// receptionist set will check whether ATObject is really remote to me
		receptionists_.takeOfflineObject(object);
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
	public ATObject resolve(ATObjectID id, ATTypeTag[] types) throws XObjectOffline {
		return receptionists_.resolveObject(id, types);
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
				globalScope.meta_defineField(field.getName(), value);
			}
		}
	}
	
	// Events to be processed by the actor event loop
	
	/**
	 * The initial event sent by the actor mirror to its event loop to intialize itself.
	 * @param future the synchronization point with the creating actor, needs to be fulfilled with a far ref to the behaviour.
	 * @param parametersPkt the serialized parameters for the initialization code
	 * @param initcodePkt the serialized initialization code (e.g. the code in 'actor: { code }')
	 */
	protected void event_init(final BlockingFuture future, final Packet parametersPkt, final Packet initcodePkt) {
		receive(new Event("init("+this+")") {
			public void process(Object byMyself) {
				try {
					behaviour_ = new NATObject();
					
					// pass far ref to behaviour to creator actor who is waiting for this
					future.resolve(receptionists_.exportObject(behaviour_));
					
					// initialize lexically visible fields
					initSharedFields();

					// go on to initialize the root and all lexically visible fields
					initRootObject();

					ATTable params = parametersPkt.unpack().asTable();
					ATMethod initCode = initcodePkt.unpack().asMethod();					
					
					// initialize the behaviour using the parameters and the code
					initCode.base_applyInScope(params, new NATContext(behaviour_, behaviour_));
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error(behaviour_ + ": could not initialize actor behaviour", e);
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
		receive(new Event("selfAccept("+msg+")") {
			public void process(Object myActorMirror) {
				performAccept(msg);
			}
		});
	}
	
	/**
	 * The main entry point for any asynchronous messages sent to this actor
	 * by external sources.
	 * @param sender address of the sending actor, used to notify when the receiver has gone offline.
	 * @param serializedMessage the asynchronous AmbientTalk base-level message to enqueue
	 */
	public void event_remoteAccept(final Address sender, final Packet serializedMessage) {
		receive(new Event("remoteAccept("+serializedMessage+")") {
			public void process(Object myActorMirror) {
			  try {
				ATAsyncMessage msg = serializedMessage.unpack().asAsyncMessage();
				performAccept(msg);
			  } catch (XObjectOffline e) {
				 host_.event_objectTakenOffline(e.getObjectId(), sender);
				Logging.Actor_LOG.error(mirror_ + ": error unpacking "+ serializedMessage, e);
			  }  catch (InterpreterException e) {
				Logging.Actor_LOG.error(mirror_ + ": error unpacking "+ serializedMessage, e);
			  } 
		    }
		});
	}
	
	/**
	 * The main entry point for any asynchronous messages sent to this actor 
	 * by local actors.
	 * @param ref the local reference of the sending actor, used to notify when the receiver has gone offline.
	 * @param serializedMessage the asynchronous AmbientTalk base-level message to enqueue
	 */
	public void event_localAccept(final NATLocalFarRef ref, final Packet serializedMessage) {
		receive(new Event("localAccept("+serializedMessage+")") {
			public void process(Object myActorMirror) {
			  try {
				ATAsyncMessage msg = serializedMessage.unpack().asAsyncMessage();
				performAccept(msg);
			  } catch (XObjectOffline e) {
				  ref.notifyTakenOffline();
				  Logging.Actor_LOG.error(mirror_ + ": error unpacking "+ serializedMessage, e);
			  } catch (InterpreterException e) {
				  Logging.Actor_LOG.error(mirror_ + ": error unpacking "+ serializedMessage, e);
			  } 
		    }
		});
	}
	
	private void performAccept(ATAsyncMessage msg) {
		try {
			ATObject result = msg.base_receiver().meta_receive(msg);
			// TODO what to do with return value?
			Logging.Actor_LOG.info(mirror_ + ": "+ msg + " returned " + result);
			} catch (InterpreterException e) {
			// TODO what to do with exception?
			Logging.Actor_LOG.error(mirror_ + ": "+ msg + " failed ", e);
		}
	}
	
	/**
	 * This method is invoked by a coercer in order to schedule a purely asynchronous symbiotic invocation
	 * from the Java world.
	 * 
	 * This method schedules the call for asynchronous execution. Its return value and or raised exceptions
	 * are ignored. This method should only be used for {@link Method} objects whose return type is <tt>void</tt>
	 * and whose declaring class is a subtype of {@link EventListener}. It represents asynchronous method
	 * invocations from the Java world to the AmbientTalk world.
	 * 
	 * @param principal the AmbientTalk object owned by this actor on which to invoke the method
	 * @param method the Java method that was symbiotically invoked on the principal
	 * @param args the arguments to the Java method call, already converted into AmbientTalk values
	 */
	public void event_symbioticInvocation(final ATObject principal, final Method method, final ATObject[] args) {
		receive(new Event("asyncSymbioticInv of "+method.getName()) {
			public void process(Object actorMirror) {
				try {
					Reflection.downInvocation(principal, method, args);
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error("asynchronous symbiotic invocation of "+method.getName()+" failed", e);
				}
			}
		});
	}
	
	/**
	 * This method is invoked by a coercer in order to schedule a symbiotic invocation
	 * from the Java world, which should be synchronous to the Java thread, but which
	 * must be scheduled asynchronously to comply with the AT/2 actor model.
	 * 
	 * This method makes the calling (Java) thread <b>block</b>, waiting until the actor
	 * has processed the symbiotic invocation.
	 * 
	 * @param principal the AmbientTalk object owned by this actor on which to invoke the method
	 * @param meth the Java method that was symbiotically invoked on the principal
	 * @param args the arguments to the Java method call, already converted into AmbientTalk values
	 * @return the result of the symbiotic invocation
	 * @throws Exception if the symbiotic invocation fails
	 */
	public Object sync_event_symbioticInvocation(final ATObject principal, final Method meth, final ATObject[] args) throws Exception {
		return receiveAndWait("syncSymbioticInv of " + meth.getName(), new Callable() {
			public Object call(Object actorMirror) throws Exception {
				ATObject result = Reflection.downInvocation(principal, meth, args);
				return Symbiosis.ambientTalkToJava(result, meth.getReturnType());
			}
		});
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
	 * @param requiredTypePkt serialized form of the type attached to the actor's subscription
	 * @param myHandler the closure specified as a handler for the actor's subscription
	 * @param discoveredTypePkt serialized form of the type attached to the new publication
	 * @param remoteServicePkt serialized form of the reference to the remote discovered service
	 */
	public void event_serviceJoined(final Packet requiredTypePkt, final ATFarReference myHandler,
			                        final Packet discoveredTypePkt, final Packet remoteServicePkt) {
		receive(new Event("serviceJoined") {
			public void process(Object myActorMirror) {
				try {
					ATTypeTag requiredType = requiredTypePkt.unpack().asTypeTag();
					ATTypeTag discoveredType = discoveredTypePkt.unpack().asTypeTag();
					// is there a match?
					if (discoveredType.base_isSubtypeOf(requiredType).asNativeBoolean().javaValue) {
						ATObject remoteService = remoteServicePkt.unpack();
						// myhandler<-apply([remoteService])
						myHandler.meta_receive(
							new NATAsyncMessage(myHandler, Evaluator._APPLY_,
								NATTable.atValue(new ATObject[] {
									NATTable.atValue(new ATObject[] { remoteService })
								}),
								NATTable.EMPTY));
					}
				} catch (XIOProblem e) {
					Logging.Actor_LOG.error("Error deserializing joined types or services: ", e.getCause());
				} catch (XClassNotFound e) {
					Logging.Actor_LOG.fatal("Could not find class while deserializing joined types or services: ", e.getCause());
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error("Error while joining services: ", e);
				}
			}
		});
	}
}
