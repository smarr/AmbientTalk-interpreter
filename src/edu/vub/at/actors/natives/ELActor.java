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
import edu.vub.at.objects.coercion.Coercer;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.symbiosis.Symbiosis;
import edu.vub.at.util.logging.Logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EventListener;

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
						Evaluator.getNil(),
						new SharedActorField[] { },
						ELVirtualMachine._DEFAULT_GROUP_NAME_,
						ELVirtualMachine._DEFAULT_IP_ADDRESS_,
						System.out);
				return host.createEmptyActor().getFarHost();
			} catch (InterpreterException e) {
				throw new RuntimeException("Failed to initialize default actor: " + e.getMessage());
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
	protected NATObject behaviour_;
	
	public ELActor(ATActorMirror mirror, ELVirtualMachine host) {
		super("actor " + mirror.toString());
		this.start();
		id_ = new ActorID();
		mirror_ = mirror;
		host_ = host;
		receptionists_ = new ReceptionistsSet(this);
	}
	
	/**constructor dedicated to initialization with stack size for android*/
	public ELActor(ATActorMirror mirror, ELVirtualMachine host, int stackSize) {
		super("actor " + mirror.toString(), stackSize);
		this.start();
		id_ = new ActorID();
		mirror_ = mirror;
		host_ = host;
		receptionists_ = new ReceptionistsSet(this);
	}
	
	/** constructor dedicated to initialization of discovery actor */
	protected ELActor(ELVirtualMachine host) {
		super("discovery actor");
		this.start();
		id_ = new ActorID();
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
	
	public ATActorMirror getImplicitActorMirror() { return mirror_; }

	public void setActorMirror(ATActorMirror mirror) { mirror_ = mirror; }
	
	public ELVirtualMachine getHost() {
		return host_;
	}
	
	public ActorID getActorID() {
		return id_;
	}
	
	public Thread getExecutor() {
		return processor_;
	}
	
	/**
	 * Takes offline a given local object such that it is no longer remotely accessible.
	 * @param object a near reference to the local object to unexport
	 * @throws XIllegalOperation if the passed object is not part of the export table - i.e. non-remotely accessible.
	 */
	public void takeOffline(ATObject object) throws InterpreterException {
		// receptionist set will check whether ATObject is really remote to me
		receptionists_.takeOfflineObject(object);
	}
	
	/**
	 * Disconnects a given local object such that it is no longer remotely accessible.
	 * @param object a near reference to the local object to disconnect
	 * @throws XIllegalOperation if the passed object is not part of the export table - i.e. non-remotely accessible.
	 */
	public ATObject disconnect(final ATObject object) throws InterpreterException {
		// receptionist set will check whether ATObject is really remote to me
		return receptionists_.disconnect(object);
	}
	
	/**
	 * Resolve the given object id into a local reference. There are three cases to
	 * consider:
	 *  A) The given id designates an object local to this actor: the returned object
	 *     will be a **near** reference to the object (i.e. the object itself)
	 *  B) The given id designates a far (non-local) object that lives in the same
	 *     address space as this actor: the returned object will be a **far** reference
	 *     to the object.
	 *  C) The given id designates a far object that lives on a remote machine: the
	 *     returned object will be a **far** and **remote** reference to the object.
	 *     
	 * @param id the identifier of the object to resolve
	 * @return a near or far reference to the object, depending on where the designated object lives
	 */
	public ATObject resolve(ATObjectID id, ATTypeTag[] types, boolean isConnected) throws XObjectOffline {
		return receptionists_.resolveObject(id, types, isConnected);
	}
	
	/* -----------------------------
	 * -- Initialisation Protocol --
	 * ----------------------------- */

	/**
	 * Initialises the root using the contents of the init.at file configured in
	 * the hosting virtual machine.
	 * 
	 * @throws InterpreterException
	 */
	protected void initRootObject() throws InterpreterException {
		ATAbstractGrammar initialisationCode = host_.getInitialisationCode();
		
		// evaluate the initialization code in the context of the global scope
		NATObject globalScope = Evaluator.getGlobalLexicalScope();		
		NATContext initCtx = new NATContext(globalScope, globalScope);
		
		// note: the return value of the init.at file is ignored
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
					future.resolve(receptionists_.exportObject(behaviour_,"behaviour of "+byMyself));
					
					// !! WARNING: the following code is also duplicated in
					// ELDiscoveryActor's event_init. If this code is modified, don't
					// forget to modify that of the discovery actor as well !!
					
					// initialize lexically visible fields
					initSharedFields();

					// go on to initialize the root and all lexically visible fields
					initRootObject();

					ATObject params = parametersPkt.unpack();
					ATMethod initCode = initcodePkt.unpack().asMethod();					
				
					if (!params.isTable()) {
						// actor initialized as actor: { ... } => free vars automatically added to a private lexical scope
						// in this case, params refers to an object that will play the role of lexical scope of the actor's behaviour
						params.asAmbientTalkObject().setLexicalParent(Evaluator.getGlobalLexicalScope());
						behaviour_.setLexicalParent(params);
						params = NATTable.EMPTY;
					}/* else {
						// actor initialized as actor: { |vars| ... } => vars become publicly accessible in the actor
					}*/
					
					// initialize the behaviour using the parameters and the code
					try {
						//adding location of the closure to the bhv
						behaviour_.impl_setLocation(initCode.impl_getLocation());
						initCode.base_applyInScope(params.asTable(), new NATContext(behaviour_, behaviour_));
					} catch (InterpreterException e) {
						host_.getOutput().println(">>> Exception while initializing actor " + Evaluator.trunc(initCode.base_bodyExpression().toString(),20) + ":\n"+e.getMessage());
						e.printAmbientTalkStackTrace(host_.getOutput());
						Logging.Actor_LOG.error(behaviour_ + ": could not initialize actor behaviour", e);
					}
				} catch (InterpreterException e) {
					host_.getOutput().println(">>> Exception while creating actor: " + e.getMessage());
					e.printAmbientTalkStackTrace(host_.getOutput());
					Logging.Actor_LOG.error(behaviour_ + ": could not initialize actor behaviour", e);
				}
			}
		});
	}
	
	/**
	 * The main entry point for any asynchronous self-sends.
	 * Asynchronous self-sends (i.e. intra-actor sends) do not undergo any form of parameter passing,
	 * there is no need to serialize and deserialize the message parameter in a Packet.
	 * 
	 * When an actor receives an asynchronous message for a given receiver, it delegates control
	 * to the message itself by means of the message's <tt>process</tt> method.
	 * 
	 * This method should only be invoked directly this actor's event loop thread.
	 * 
	 * @throws InterpreterException 
	 */
	public void acceptSelfSend(final ATObject receiver, final ATAsyncMessage msg) throws InterpreterException {
		// This is the only place where messages are scheduled
		// The receiver is always a local object, receive has
		// already been invoked.
    	mirror_.base_schedule(receiver, msg);
	}
	
	/**
	 * This method makes the actor perform:
	 * <code>closure&lt;-apply(arguments)@[]</code>
	 * This receiver actor must be the owner of the closure.
	 */
	public void event_trigger(final ATObject closure, final ATTable arguments, final String type) {
		final ELActor owner = this;
		receive(new Event("trigger("+closure+")") {
			public void process(Object myActorMirror) {
				try {
					owner.acceptSelfSend(closure, 
							NATAsyncMessage.createExternalAsyncMessage(Evaluator._APPLY_, 
																		NATTable.of(arguments), 
																		NATTable.EMPTY));
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error(myActorMirror + ": error triggering "+ type + " handler with args " + arguments, e);
				}	
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
				// receive a pair [receiver, message]
				ATObject[] pair = serializedMessage.unpack().asNativeTable().elements_;
				ATObject receiver = pair[0];
				ATAsyncMessage msg = pair[1].asAsyncMessage();
				performAccept(receiver, msg);
			  } catch (XObjectOffline e) {
				host_.event_objectTakenOffline(e.getObjectId(), sender);
				Logging.Actor_LOG.warn(mirror_ + ": could not process "+ serializedMessage + ", object offline: " + e.getObjectId());
			  } catch (InterpreterException e) {
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
				// receive a pair [receiver, message]
				ATObject[] pair = serializedMessage.unpack().asNativeTable().elements_;
				ATObject receiver = pair[0];
				ATAsyncMessage msg = pair[1].asAsyncMessage();
				performAccept(receiver, msg);
			  } catch (XObjectOffline e) {
				  ref.notifyTakenOffline();
				  Logging.Actor_LOG.warn(mirror_ + ": could not process "+ serializedMessage + ", object offline: " + e.getObjectId());
			  } catch (InterpreterException e) {
				  Logging.Actor_LOG.error(mirror_ + ": error unpacking "+ serializedMessage, e);
			  } 
		    }
		});
	}
	
	public void event_serve() {
		receive(new Event("serve()") {
			public void process(Object myActorMirror) {
				try {
					ATObject result = mirror_.base_serve();
					Logging.Actor_LOG.debug(mirror_ + ": serve() returned " + result);
				} catch (InterpreterException e) {
					host_.getOutput().println(">>> Exception in actor " + myActorMirror + ": "+e.getMessage());
					e.printAmbientTalkStackTrace(host_.getOutput());
					Logging.Actor_LOG.error(mirror_ + ": serve() failed ", e);
				}
			}
		});
	}
	
	private void performAccept(ATObject receiver, ATAsyncMessage msg) {
		try {
			ATObject result = mirror_.base_receive(receiver, msg);
			Logging.Actor_LOG.debug(mirror_ + ": scheduling "+ msg + " returned " + result);
			
			// signal a serve event for every message that is accepted
			// event_serve();
		} catch (InterpreterException e) {
			host_.getOutput().println(">>> Exception in actor " + getImplicitActorMirror() + ": "+e.getMessage());
			e.printAmbientTalkStackTrace(host_.getOutput());
			Logging.Actor_LOG.error(mirror_ + ": scheduling "+ msg + " failed ", e);
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
	 * @param jargs the raw arguments to the Java method call, not yet converted into AmbientTalk values
	 */
	public void event_symbioticInvocation(final ATObject principal, final Method method, final Object[] jargs) {
		receive(new Event("asyncSymbioticInv of "+method.getName()) {
			public void process(Object actorMirror) {
				try {
					ATObject[] atArgs = Coercer.convertArguments(jargs);
					Reflection.downInvocation(principal, method, atArgs);
				} catch (InterpreterException e) {
					host_.getOutput().println(">>> Exception in actor " + actorMirror + ": "+e.getMessage());
					e.printAmbientTalkStackTrace(host_.getOutput());
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
	 * The future returned by this method makes the calling (Java) thread <b>block</b> upon
	 * accessing its value, waiting until the actor has processed the symbiotic invocation.
	 * 
	 * @param principal the AmbientTalk object owned by this actor on which to invoke the method
	 * @param meth the Java method that was symbiotically invoked on the principal
	 * @param jargs the raw arguments to the Java method call, not yet converted into AmbientTalk values
	 * @return a Java future that is resolved with the result of the symbiotic invocation
	 * @throws Exception if the symbiotic invocation fails
	 */
	public BlockingFuture sync_event_symbioticInvocation(final ATObject principal, final Method meth, final Object[] jargs) throws Exception {
		return receiveAndReturnFuture("syncSymbioticInv of " + meth.getName(), new Callable() {
			public Object call(Object actorMirror) throws Exception {
				Class targetType = meth.getReturnType();
				
				ATObject[] atArgs = Coercer.convertArguments(jargs);
				// if the return type is BlockingFuture, the first argument should specify the type
				// of the value with which BlockingFuture will be resolved
				if (targetType.equals(BlockingFuture.class)) {
					if ((meth.getParameterTypes().length > 0) && (meth.getParameterTypes()[0].equals(Class.class))) {
						targetType = atArgs[0].asJavaClassUnderSymbiosis().getWrappedClass();
						// drop first argument, it only exists to specify the targetType
						ATObject[] newArgs = new ATObject[atArgs.length-1];
						System.arraycopy(atArgs, 1, newArgs, 0, newArgs.length);
						atArgs = newArgs;
					}
				}
				
				ATObject result = Reflection.downInvocation(principal, meth, atArgs);
				// SUPPORT FOR FUTURES
				if (Symbiosis.isAmbientTalkFuture(result)) {
					Logging.Actor_LOG.debug("Symbiotic futures: symbiotic call to " + meth.getName() + " returned an AT future");
					return Symbiosis.ambientTalkFutureToJavaFuture(result, targetType);
				} else {
					// return the proper value immediately
					return Symbiosis.ambientTalkToJava(result, targetType);
				}
			}
		});
	}
	
	/**
	 * This method is invoked by a coercer in order to schedule a symbiotic invocation
	 * of a method from java.lang.Object from the Java world, which should be synchronous
	 * to the Java thread, but which
	 * must be scheduled asynchronously to comply with the AT/2 actor model.
	 * 
	 * The future returned by this method makes the calling (Java) thread <b>block</b> upon
	 * accessing its value, waiting until the actor has processed the symbiotic invocation.
	 * 
	 * Note: the parameter meth must be a method declared on the class java.lang.Object
	 * (i.e. toString, hashCode and equals). The invocation is simply forwarded directly
	 * to the principal with no conversion to an AmbientTalk invocation.
	 * 
	 * @param principal the AmbientTalk object owned by this actor on which to invoke the method
	 * @param meth the Java method that was symbiotically invoked on the principal
	 * @param args the arguments to the Java method call, already converted into AmbientTalk values
	 * @return a Java future that is resolved with the result of the symbiotic invocation
	 * @throws Exception if the symbiotic invocation fails
	 */
	public BlockingFuture sync_event_symbioticForwardInvocation(final ATObject principal, final Method meth, final Object[] args) throws Exception {
		return receiveAndReturnFuture("syncSymbioticInv of " + meth.getName(), new Callable() {
			public Object call(Object actorMirror) throws Exception {
				try {
					return meth.invoke(principal, args);		
				} catch (InvocationTargetException e) {
					if (e instanceof Exception) { 
						throw (Exception) e.getTargetException();
					} else {
						throw e;
					}
				}
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
			if (e instanceof InterpreterException) {
				throw (InterpreterException) e;
			} else {
				Logging.Actor_LOG.fatal("Unexpected Java exception: "+e.getMessage(), e);
				throw new RuntimeException("Unexpected exception: "+e);
			}
		}
	}
	
	/**
	 * This method should only be used for purposes such as the IAT shell or unit testing.
	 * It allows an external thread to make this actor evaluate an arbitrary expression and to
	 * print that expression (into a String).
	 * 
	 * @param ast an abstract syntax tree to be evaluated by the receiving actor (in the
	 * scope of its behaviour).
	 * @return the printed result of the evaluation as a String
	 * @throws InterpreterException if the evaluation fails
	 */
	public String sync_event_evalAndPrint(final ATAbstractGrammar ast) throws InterpreterException {
		try {
			return (String) receiveAndWait("nativeEval("+ast+")", new Callable() {
				public Object call(Object inActor) throws Exception {
				    return OBJLexicalRoot._INSTANCE_.base_eval_in_(ast, behaviour_).toString();
				}
			});
		} catch (Exception e) {
			if (e instanceof InterpreterException) {
				throw (InterpreterException) e;
			} else {
				Logging.Actor_LOG.fatal("Unexpected Java exception: "+e.getMessage(), e);
				throw new RuntimeException("Unexpected exception: "+e);
			}
		}
	}
	
	/**
	 * This method should only be used for purposes of unit testing. It allows
	 * arbitary code to be scheduled by external threads such as unit testing frameworks.
	 */
	public Object sync_event_performTest(Callable c) throws Exception {
		return (ATObject) receiveAndWait("performTest("+c+")", c);
	}
	
	/**
	 * When the discovery manager receives a publication from another local actor or
	 * another remote VM, it compares the incoming publication against a subscription that
	 *  it had announced previously. For  each matching subscription ELDiscoveryActor then 
	 *  calls event_serviceJoined to make ELActor deserialize the remote service object and
	 *  apply the when:discovered: listeners passing a (remote/local) far reference to it.
	 *  
	 * @param myHandler the closure specified as a handler for the actor's subscription
	 * @param remoteServicePkt serialized form of the reference to the remote discovered service
	 */
	public void event_serviceJoined(final ATObject myHandler, final Packet remoteServicePkt) {
		final ELActor owner = this;
		receive(new Event("serviceJoined") {
			public void process(Object myActorMirror) {
				ATObject unserializedRemoteService = null;
				try {
					// we unserialize the remote service in the receiver actor, so that 
					// a remote reference gets the correct pool.
					unserializedRemoteService = remoteServicePkt.unpack();
					//make handle receive this message.
					Evaluator.trigger(myHandler, NATTable.of(unserializedRemoteService));
				} catch (XIOProblem e) {
					Logging.Actor_LOG.error("Error deserializing remote published service: ", e.getCause());
				} catch (InterpreterException e) {
					Logging.Actor_LOG.error(myActorMirror + ": error triggering when:discovered: handler with arg " + unserializedRemoteService, e);
				}
			}
		});
	}
	
	public void event_reset() {
		this.receive(new Event("reset") {
			public void process(Object myself) {
				// clean the data structures for publications/subscriptions and listeners.
				try {
					receptionists_.softReset();
				} catch (XIllegalOperation e) {
					Logging.Actor_LOG.error(this + ": actor reset failed");
				}
				currentActor().stopProcessing();
				Logging.Actor_LOG.info(this + ": actor reset sucessfully completed");
			}
		});
	}
	
}
