/**
 * AmbientTalk/2 Project
 * VMEmittedEvents.java created on Dec 5, 2006 at 12:50:13 PM
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATResolution;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATAsyncMessage;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * VMEmittedEvents is a factory class used to easily produce the various events that
 * may be sent out by an ambienttalk virtual machine. It contains a number of static
 * methods which create asynchronous messages to be placed in an event queue.
 *
 * @author smostinc
 */
public class VMEmittedEvents {

    /* ---------------------------
     * -- VM to Actor  Protocol --
     * --------------------------- */
	
	public static final ATSymbol _ACCEPT_ = AGSymbol.jAlloc("accept");
	public static final ATSymbol _DELIVERED_ = AGSymbol.jAlloc("delivered");
	public static final ATSymbol _FAILED_DELIVERY_ = AGSymbol.jAlloc("failedDelivery");
	public static final ATSymbol _FOUND_RESOLUTION_ = AGSymbol.jAlloc("foundResolution");
	public static final ATSymbol _LOST_RESOLUTION_ = AGSymbol.jAlloc("lostResolution");
	

	/**
	 * @see edu.vub.at.actors.natives.NATActor#base_accept(ATAsyncMessage)
	 */
	public static final ATAsyncMessage acceptMessage(ATAsyncMessage msg) {
		return new NATAsyncMessage(msg.base_getSender(), _ACCEPT_,  NATTable.atValue(new ATObject[] { msg }));
	}
	
	/**
	 * @see edu.vub.at.actors.natives.NATActor#base_delivered(ATAsyncMessage)
	 */
	public static final ATAsyncMessage deliveredMessage(ATAsyncMessage msg) {
		return new NATAsyncMessage(msg.base_getReceiver(), _DELIVERED_,  NATTable.atValue(new ATObject[] { msg }));
	}
	
	/**
	 * TODO(discuss) a table of messages?
	 * @see edu.vub.at.actors.natives.NATActor#base_failedDelivery(ATAsyncMessage)
	 */
public static final ATAsyncMessage couldNotDeliverMessage(ATAsyncMessage msg) {
		return new NATAsyncMessage(msg.base_getReceiver(), _FAILED_DELIVERY_,  NATTable.atValue(new ATObject[] { msg }));
	}
	
	/**
	 * @see edu.vub.at.actors.natives.NATActor#base_foundResolution(ATResolution)
	 */
	public static final ATAsyncMessage foundResolution(ATResolution res) {
		return new NATAsyncMessage(res.base_getActor(), _FOUND_RESOLUTION_,  NATTable.atValue(new ATObject[] { res }));
	}
	
	/**
	 * @see edu.vub.at.actors.natives.NATActor#base_lostResolution(ATResolution)
	 */
	public static final ATAsyncMessage lostResolution(ATResolution res) {
		return new NATAsyncMessage(res.base_getActor(), _LOST_RESOLUTION_,  NATTable.atValue(new ATObject[] { res }));
	}
}
