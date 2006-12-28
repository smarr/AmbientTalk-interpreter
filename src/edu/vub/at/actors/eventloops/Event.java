/**
 * AmbientTalk/2 Project
 * Event.java created on 27-dec-2006 at 15:37:27
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
package edu.vub.at.actors.eventloops;

/**
 * An Event object is the main communication channel between different event loops.
 * Events are the only kind of objects scheduled in event queues of the event loops.
 *
 * @author tvcutsem
 */
public abstract class Event {

	/**
	 * For debugging purposes only.
	 */
	private final String description_;
	
	public Event() {
		description_ = "anonymous event: " + this;
	}
	
	public Event(String description) {
		description_ = "event: " + description;
	}
	
	/**
	 * This method <i>may</i> be invoked by the receiving event loop such that the
	 * event can process itself.
	 * 
	 * @param owner the object designated by the receiving event loop to be the owner of this event.
	 */
	public abstract void process(Object owner);
	
	public String toString() {
		return description_;
	}
	
}
