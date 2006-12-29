/**
 * AmbientTalk/2 Project
 * ELVirtualMachine.java created on Nov 1, 2006 at 8:32:31 PM
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
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.id.GUID;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;

import java.io.File;
import java.util.Hashtable;

/**
 * A ELVirtualMachine represents a virtual machine which hosts several actors. The 
 * virtual machine is in charge of creating connections with other virtual machines 
 * and orchestrates the broadcasting of its presence, the exchange of service 
 * descriptions and messages. It also contains a set of runtime parameters (such as
 * the objectpath and initfile) which are needed to initialise a new actor.
 *
 * @author smostinc
 */
public final class ELVirtualMachine extends EventLoop {
	
	public static final ELVirtualMachine currentVM() {
		return ELActor.currentActor().getHost();
	}
	
	/** startup parameter to the VM: which directories to include in the lobby */
	private final File[] objectPathRoots_;
	
	/** startup parameter to the VM: the code of the init.at file to use */
	private final ATAbstractGrammar initialisationCode_;
	
	/** the GUID of this VM */
	private final GUID vmId_;
	
	/** a table mapping actor IDs to local native actors (int -> ELActor) */
	private final Hashtable localActors_;
	
	public ELVirtualMachine(File[] objectPathRoots, ATAbstractGrammar initCode) {
		super("virtual machine");
		objectPathRoots_ = objectPathRoots;
		initialisationCode_ = initCode;
		vmId_ = new GUID();
		localActors_ = new Hashtable();
	}
	
	public GUID getGUID() { return vmId_; }
	
	public ATAbstractGrammar getInitialisationCode() {
		return initialisationCode_;
	}

	public File[] getObjectPathRoots() {
		return objectPathRoots_;
	}
	
	public ELVirtualMachine getHost() { return this; }

	public void handle(Event event) {
		// make the event process itself
		event.process(this);
	}
	
	public ELActor getActor(int id) {
		ELActor entry = (ELActor) localActors_.get(new Integer(id));
		if (entry != null) {
			return entry;
		} else {
			throw new RuntimeException("Asked for unknown actor id: " + id);
		}
	}
	
	/* ==========================
	 * == Actor -> VM Protocol ==
	 * ========================== */
	
	// All methods prefixed by event_ denote asynchronous message sends that will be
	// scheduled in the receiving event loop's event queue
	
	/**
	 * Event that signals the creation of a new actor on this virtual machine.
	 */
	public void event_actorCreated(final NATActorMirror actor) {
		this.receive(new Event("actorCreated("+actor+")") {
			public void process(Object myself) {
				localActors_.put(new Integer(actor.hashCode()), actor.getProcessor());
			}
		});
	}
	
    /**
     * This event is fired whenever an object
     * is being offered as a service provide using the provide: language construct. 
     * The virtual machine keeps track of such services and is responsible for the 
     * matching between services and clients. When such matches are detected the VM
     * will send a foundResolution event to both involved actors. If the VM detects 
     * that one partner has become unavailable it will send the lostResolution event
     * 
     * @param actor - the actor of the object being provided
     * @param service - the object providing the service
     */
	public void event_servicePublished(final ATActorMirror actor, final ATObject service) {
		this.receive(new Event("servicePublished("+actor+","+service+")") {
			public void process(Object myself) {
				
			}
		});
	}
	
    /**
     * This event is fired whenever an object
     * requests a service using the require: language construct. The virtual machine 
     * keeps track of such requests and is responsible matching services and clients. 
     * When such matches are detected the VM will send a foundResolution event to both
     * involved actors. If the VM detects that one partner has become unavailable it 
     * will send the lostResolution event
     * 
     * @param actor - the actor of the object requesting the service
     * @param client - the object requesting the service
     */
	public void event_clientSubscribed(final ATActorMirror actor, final ATObject client) {
		this.receive(new Event("clientSubcribed("+actor+","+client+")") {
			public void process(Object myself) {
				
			}
		});
	}

    /**
     * This event is fired whenever a service
     * offer is being revoked. In this case, the virtual machine ensures that the 
     * object is no longer discoverable to new clients. However, it will not send 
     * lostResolution events as these signal that an object has become unreachable.
     * 
     * @param actor - the actor of the object providing the service to cancel
     * @param service - the object providing the service to cancel
     * @return nil
     */
	public void event_cancelPublication(final ATActorMirror actor, final ATObject service) {
		this.receive(new Event("cancelPublication("+actor+","+service+")") {
			public void process(Object myself) {
				
			}
		});
	}

    /**
     * This event is fired whenever a service
     * request is being revoked. In this case, the virtual machine ensures that the 
     * object will no longer discover new services. However, it will not send 
     * lostResolution events as these signal that the client has become unreachable.
     * 
     * @param actor - the actor of the client losing interest
     * @param client - the client whose subscription to cancel
     */
	public void event_cancelSubscription(final ATActorMirror actor, final ATObject client) {
		this.receive(new Event("cancelSubscription("+actor+","+client+")") {
			public void process(Object myself) {
				
			}
		});
	}

}
