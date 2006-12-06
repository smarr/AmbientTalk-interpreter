/**
 * AmbientTalk/2 Project
 * NATVirtualMachine.java created on Nov 1, 2006 at 8:32:31 PM
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATDevice;
import edu.vub.at.actors.ATServiceDescription;
import edu.vub.at.actors.ATVirtualMachine;
import edu.vub.at.actors.natives.events.VMEmittedEvents;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.NATParser;

/**
 * A NATVirtualMachine represents a virtual machine which hosts several actors. The 
 * virtual machine is in charge of creating connections with other virtual machines 
 * and orchestrates the broadcasting of its presence, the exchange of service 
 * descriptions and messages. It also contains a set of runtime parameters (such as
 * the objectpath and initfile) which are needed to initialise a new actor.
 *
 * @author smostinc
 */
public final class NATVirtualMachine extends NATAbstractActor implements ATVirtualMachine {
	
	public static final ATSymbol _NEW_ACTOR_ = AGSymbol.jAlloc("actorCreated");
	
	protected File[] objectPathRoots_; 
	protected ATAbstractGrammar initialisationCode_;
	protected ATDevice device_;
	
	public NATVirtualMachine(NATText objectPath, NATText initPath) throws InterpreterException {
		super(new ATObject[] { objectPath, initPath});
	}

	private NATVirtualMachine(File[] objectPathRoots, ATAbstractGrammar initCode, ATObject[] initArgs) throws InterpreterException {
		super(initArgs);
		// Avoids race conditions between the constructor and base_init which will be executed by the newly created ActorThread
		synchronized (this) {
			objectPathRoots_ = objectPathRoots;
			initialisationCode_ = initCode;
		}
	}

	
	public ATObject base_init(ATObject[] initArgs) throws InterpreterException {
		if(initArgs.length < 2) {
			// Avoids race conditions between base_init and the constructor executing in another (Actor)Thread
			synchronized (this) {
				ATObject objectPath	= initArgs[0];
				ATObject initPath	= initArgs[1];
				if(objectPath	!= NATNil._INSTANCE_) setObjectPath(objectPath.asNativeText());
				if(initPath 		!= NATNil._INSTANCE_) setInitPath(initPath.asNativeText());
			}
		} else {
			throw new XArityMismatch("init", 2, initArgs.length);
		}
		
		return NATNil._INSTANCE_;
	}
	
	/*
	 * Given a textual object path computes and verifies all passed entries to see
	 * whether they exist and whether they are proper directories.
	 */
	private void setObjectPath(NATText objectPath) throws InterpreterException {
		// split the object path using its ';' separator
		String[] roots = objectPath.javaValue.split(";");
		objectPathRoots_ = new File[roots.length];
		
		// for each path to the lobby, add an entry for each directory in the path
		for (int i = 0; i < roots.length; i++) {
			File pathfile = new File(roots[i]);
			
			// check whether the given pathfile is a directory
			if (!pathfile.isDirectory()) {
			    throw new XIllegalArgument("Error: non-directory file on objectpath: " + pathfile.getAbsolutePath());
			}
			
			if (!pathfile.isAbsolute()) {
				try {
					pathfile = pathfile.getCanonicalFile();
				} catch (IOException e) {
					throw new XIllegalOperation("Fatal error while constructing objectpath: " + e.getMessage());
				}
			}
			
			objectPathRoots_[i] = pathfile;
		}
		
	}

	/*
	 * Given a path from where to retrieve an init file, creates a parsetree of the 
	 * code in this file.
	 */
	private void setInitPath(NATText initPath) throws InterpreterException {
		File initFile;
		
		try {
			if (initPath != null) {
				// the user specified a custom init file
				initFile = new File(initPath.javaValue);
				if (!initFile.exists()) {
					throw new XIllegalOperation("Unknown init file: "+initPath.javaValue);
				}
			} else {
				// use the default init file provided with the distribution
				initFile = new File(new URI(NATVirtualMachine.class.getResource("/edu/vub/at/init/init.at").toString()));
			}
			
			initialisationCode_ = NATParser.parse(initFile.getName(), Evaluator.loadContentOfFile(initFile));

		} catch (URISyntaxException e) {
			// should not happen as the default init.at file should exist at a valid location
			throw new XIllegalOperation("Could not locate default init file: "+e.getMessage());
		} catch (IOException e) {
			throw new XIllegalOperation("Error reading the init file: "+e.getMessage());
		}
	}
			
	public ATDevice base_getDevice() {
		return device_;
	}
	
	/**
	 * Determines whether the object is a proxy for an object hosted on another virtual 
	 * machine. The default semantics is as follows : all objects are local except 
	 * possibly for far references. Their GUID is examined to see whether they were 
	 * produced by another Virtual Machine
	 * 
	 * @param object - the object being tested
	 * @return
	 */
	public ATBoolean base_isLocal(ATObject object) {
		// TODO If the object is far, derive locality from its GUID
		return NATBoolean._TRUE_;
	}
	
	// Signals observers of newly created actor
	// CALLBACK : none
	public ATObject base_newActor(ATActor actor) {
		try {
			base_fire_withArgs_(_NEW_ACTOR_, NATTable.atValue(new ATObject[] { actor }));
		} catch (InterpreterException e) {
			// Ignore exceptions raised while triggering observers
		}
		return NATNil._INSTANCE_;		
	}
	
	// Attempts to transmit a message
	// CALLS : accept (destination actor) | send (communication channel)
	// CALLBACK : delivered | failedDelivery
	public ATNil base_transmit(ATAsyncMessage msg) throws InterpreterException {
		System.out.println("Attempting to transmit message : " + msg.base_getSelector());
		ATObject receiver = msg.base_getReceiver();
		
		if(base_isLocal(receiver).asNativeBoolean().javaValue) {
			receiver.meta_getActor().base_scheduleEvent(VMEmittedEvents.acceptMessage(msg));
			
			msg.base_getSender().meta_getActor().base_scheduleEvent(VMEmittedEvents.deliveredMessage(msg));
		} else {
			// TODO(distribution) What if the object is not local?
		}
		return NATNil._INSTANCE_;		
	}
	
	public ATNil base_servicePublished(ATServiceDescription description, ATObject service) {
		//TODO(service discovery) Implement this method
		return NATNil._INSTANCE_;		
	}
	
	public ATNil base_serviceSubscription(ATServiceDescription description, ATObject client) {
		//TODO(service discovery) Implement this method
		return NATNil._INSTANCE_;		
	}

	public ATNil base_cancelPublishing(ATServiceDescription description, ATObject service) {
		//TODO(service discovery) Implement this method
		return null;
	}

	public ATNil base_cancelSubscription(ATServiceDescription description, ATObject client) {
		//TODO(service discovery) Implement this method
		return null;
	}

	protected NATNil base_dispose() throws InterpreterException {
		return NATNil._INSTANCE_;		
	}

	// Creates a new Virtual Machine with identical parameters
	public ATObject meta_clone() throws InterpreterException {
		return new NATVirtualMachine(objectPathRoots_, initialisationCode_, new ATObject[] { NATNil._INSTANCE_, NATNil._INSTANCE_ });
	}

	// Creates a new Virtual Machine : default paramters are current values, may be modified by initArgs
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		return new NATVirtualMachine(objectPathRoots_, initialisationCode_, initargs.asNativeTable().elements_);
	}		
}
