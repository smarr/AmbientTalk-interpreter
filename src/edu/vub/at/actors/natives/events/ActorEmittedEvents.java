/**
 * AmbientTalk/2 Project
 * ActorEmittedEvents.java created on Dec 5, 2006 at 1:39:01 PM
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
package edu.vub.at.actors.natives.events;

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATServiceDescription;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATAsyncMessage;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * ActorEmittedEvents is a factory class used to easily produce the various events 
 * that an actor may send to its ambienttalk virtual machine. It contains a number of 
 * static methods which create asynchronous messages to be placed in an event queue.
 *
 * @author smostinc
 */
public class ActorEmittedEvents {

    /* --------------------------
     * -- Actor to VM Protocol --
     * -------------------------- */
	
	public static final ATSymbol _NEW_ACTOR_ = AGSymbol.jAlloc("newActor");
	public static final ATSymbol _TRANSMIT_ = AGSymbol.jAlloc("transmit");
	public static final ATSymbol _PUBLISH_ = AGSymbol.jAlloc("servicePublished");
	public static final ATSymbol _CANCEL_PUBLISHING_ = AGSymbol.jAlloc("cancelPublishing");
	public static final ATSymbol _SUBSCRIBE_ = AGSymbol.jAlloc("serviceSubscription");
	public static final ATSymbol _CANCEL_SUBSCRIPTION_ = AGSymbol.jAlloc("cancelSubscription");
	
	/**
	 * @see edu.vub.at.actors.natives.NATVirtualMachine#base_newActor(ATActor)
	 */
	public static final ATAsyncMessage newlyCreated(ATActor actor) {
		return new NATAsyncMessage(actor, _NEW_ACTOR_, NATTable.atValue(new ATObject[] { actor }));
	}

	/**
	 * @see edu.vub.at.actors.natives.NATVirtualMachine#base_transmit(ATAsyncMessage)
	 */
	public static final ATAsyncMessage transmitMessage(ATAsyncMessage msg) {
		return new NATAsyncMessage(msg.base_getSender(), _TRANSMIT_,  NATTable.atValue(new ATObject[] { msg }));
	}
		
	/**
	 * @see edu.vub.at.actors.natives.NATVirtualMachine#base_servicePublished(ATServiceDescription, ATObject)
	 */
	public static final ATAsyncMessage publishService(ATServiceDescription description, ATObject service) {
		return new NATAsyncMessage(service, _PUBLISH_,  NATTable.atValue(new ATObject[] { description, service }));
	}
	
	/**
	 * @see edu.vub.at.actors.natives.NATVirtualMachine#base_cancelPublishing(ATServiceDescription, ATObject)
	 */
	public static final ATAsyncMessage cancelPublishedService(ATServiceDescription description, ATObject service) {
		return new NATAsyncMessage(service, _CANCEL_PUBLISHING_,  NATTable.atValue(new ATObject[] { description, service }));
	}
	
	/**
	 * @see edu.vub.at.actors.natives.NATVirtualMachine#base_serviceSubscription(ATServiceDescription, ATObject)
	 */
	public static final ATAsyncMessage subscribeToService(ATServiceDescription description, ATObject client) {
		return new NATAsyncMessage(client, _SUBSCRIBE_,  NATTable.atValue(new ATObject[] { description, client }));
	}
	
	/**
	 * @see edu.vub.at.actors.natives.NATVirtualMachine#base_cancelSubscription(ATServiceDescription, ATObject)
	 */
	public static final ATAsyncMessage cancelSubscription(ATServiceDescription description, ATObject client) {
		return new NATAsyncMessage(client, _CANCEL_SUBSCRIPTION_,  NATTable.atValue(new ATObject[] { description, client }));
	}
}