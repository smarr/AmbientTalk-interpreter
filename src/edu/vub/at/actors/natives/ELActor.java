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
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNamespace;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.OBJLexicalRoot;

import java.io.File;
import java.io.FilenameFilter;

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
public final class ELActor extends EventLoop {
	
	public static final ELActor currentActor() {
		try {
			return ((ELActor) EventLoop.currentEventLoop());
		} catch (ClassCastException e) {
			System.err.println("Asked for an actor in a non-event loop thread?");
			e.printStackTrace();
			throw new RuntimeException("Asked for an actor outside of an event loop");
		}
	}

	private final ATActorMirror mirror_;
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

	/**
	 * Actor event loops handle events by allowing the meta-level events to
	 * process themselves.
	 */
	public void handle(Event event) {
		event.process(mirror_);
	}
	
	public ATActorMirror getActorMirror() { return mirror_; }

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
	public ATFarReference export(ATObject object) throws InterpreterException {
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
	public ATObject resolve(ATObjectID id) {
		return receptionists_.resolveObject(id);
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
					
					// go on to initialize the root and the behaviour
					initLobbyUsingObjectPath();
					initRootObject();
				} catch (InterpreterException e) {
					if (!future.isDetermined())
					  future.ruin(e);
				}
			}
		});
	}
	
	/**
	 * The main entry point for any asynchronous self-sends.
	 * Asynchronous self-sends do not undergo any form of parameter passing.
	 */
	public void event_acceptSelfSend(final ATAsyncMessage msg) {
		receive(new Event("accept("+msg+")") {
			public void process(Object myActorMirror) {
				try {
					ATObject result = msg.base_process(mirror_);
					// TODO what to do with return value?
					System.err.println("accept("+msg+") returned " + result);
				} catch (InterpreterException e) {
					// TODO what to do with exception?
					System.err.println("accept("+msg+") failed with " + e.getMessage());
				}
			}
		});
	}
	
	/**
	 * The main entry point for any asynchronous messages sent to this actor,
	 * be it 'self-sends' or external sends.
	 * @param msg the asynchronous AmbientTalk base-level message to enqueue
	 */
	public void event_accept(final Packet serializedMessage) {
		receive(new Event("accept("+serializedMessage.getDescription()+")") {
			public void process(Object myActorMirror) {
				try {
					ATAsyncMessage msg = serializedMessage.unpack().base_asAsyncMessage();
					ATObject result = msg.base_process(mirror_);
					// TODO what to do with return value?
					System.err.println("accept("+serializedMessage.getDescription()+") returned " + result);
				} catch (InterpreterException e) {
					// TODO what to do with exception?
					System.err.println("accept("+serializedMessage.getDescription()+") failed with " + e.getMessage());
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
}
