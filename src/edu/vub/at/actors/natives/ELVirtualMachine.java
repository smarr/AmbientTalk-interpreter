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
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.io.File;

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
		try {
			return ((ELActor) EventLoop.currentEventLoop()).getHost();
		} catch (ClassCastException e) {
			System.err.println("Asked for a VM outside of an actor?");
			e.printStackTrace();
			throw new RuntimeException("Asked for a VM outside of an actor?");
		}
	}
	
	public static final ATSymbol _NEW_ACTOR_ = AGSymbol.jAlloc("actorCreated");
	
	
	/** startup parameter to the VM: which directories to include in the lobby */
	private final File[] objectPathRoots_;
	
	/** startup parameter to the VM: the code of the init.at file to use */
	private final ATAbstractGrammar initialisationCode_;
	
	/** the GUID of this VM */
	private GUID vmId_;
	
	public ELVirtualMachine(File[] objectPathRoots, ATAbstractGrammar initCode) {
		super("virtual machine");
		objectPathRoots_ = objectPathRoots;
		initialisationCode_ = initCode;
		vmId_ = new GUID();
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
	
	/* ==========================
	 * == Actor -> VM Protocol ==
	 * ========================== */
	
	// All methods prefixed by event_ denote asynchronous message sends that will be
	// scheduled in the receiving event loop's event queue
	
	public void event_actorCreated(final ATActorMirror actor) {
		this.receive(new Event("actorCreated("+actor+")") {
			public void process(Object myself) {
				
			}
		});
	}
	
	public void event_servicePublished(final ATActorMirror actor, final ATObject service) {
		this.receive(new Event("servicePublished("+actor+","+service+")") {
			public void process(Object myself) {
				
			}
		});
	}
	
	public void event_clientSubscribed(final ATActorMirror actor, final ATObject client) {
		this.receive(new Event("clientSubcribed("+actor+","+client+")") {
			public void process(Object myself) {
				
			}
		});
	}

	public void event_cancelPublication(final ATActorMirror actor, final ATObject service) {
		this.receive(new Event("cancelPublication("+actor+","+service+")") {
			public void process(Object myself) {
				
			}
		});
	}

	public void event_cancelSubscription(final ATActorMirror actor, final ATObject client) {
		this.receive(new Event("cancelSubscription("+actor+","+client+")") {
			public void process(Object myself) {
				
			}
		});
	}

}
