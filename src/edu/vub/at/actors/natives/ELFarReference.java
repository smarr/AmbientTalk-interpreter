/**
 * AmbientTalk/2 Project
 * ELFarReference.java created on 28-dec-2006 at 10:45:41
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.eventloops.Event;
import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATTable;

/**
 * An instance of the class ELFarReference represents the event loop processor for
 * a remote far reference. That is, the event queue of this event loop serves as 
 * an 'outbox' which is dedicated to a certain receiver object hosted by a remote virtual machine.
 * 
 * This event loop handles event from its event queue by trying to transmit them to a remote virtual machine.
 * 
 * TODO: integrate this class with the JGroups framework.
 * 
 * @author tvcutsem
 */
public final class ELFarReference extends EventLoop {

	private final NATRemoteFarRef owner_;
	
	public ELFarReference(NATRemoteFarRef owner) {
		super("far reference " + owner);
		owner_ = owner;
	}

	public void handle(Event event) {
		event.process(owner_);
	}
	
	public void event_transmit(final ATAsyncMessage msg) {
		receive(new Event("transmit("+msg+")") {
			public void process(Object owner) {
				// TODO: try to transmit the msg
			}
		});
	}
	
	public ATTable sync_event_retractUnsentMessages() throws InterpreterException {
		try {
			return (ATTable) receiveAndWait("retractUnsentMessages()", new Callable() {
				public Object call(Object owner) throws Exception {
					final NATRemoteFarRef me = (NATRemoteFarRef) owner;
					// TODO: return outgoing unsent messages
					return null;
				}
			});
		} catch (Exception e) {
			throw (InterpreterException) e;
		}
	}

}
