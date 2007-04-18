/**
 * AmbientTalk/2 Project
 * FutureEvent.java created on 27-dec-2006 at 15:50:33
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
 * A Future Event has an associated Future object to which the event
 * will pass the return value or exception of a certain computation to be
 * specified by a subclass.
 *
 * @author tvcutsem
 */
public abstract class FutureEvent extends Event {

	private final Future future_;

	/**
	 * Verbode constructor which takes a future to be resolved by this event as well as a 
	 * descriptive string describing this event for debugging purposes. 
	 * @param description a description of the event for debugging purposes.
	 * @param reply the future which will be resolved when this event has been executed.
	 */
	public FutureEvent(String description, Future reply) {
		super(description);
		future_ = reply;
	}
	
	/**
	 * Default constructor which takes a future to be resolved by this event. 
	 * @param reply the future which will be resolved when this event has been executed.
	 */
	public FutureEvent(Future reply) {
		future_ = reply;
	}
	
	public void process(Object owner) {
		try {
			Object result = this.execute(owner);
			future_.resolve(result);
		} catch(Exception e) {
			future_.ruin(e);
		}
	}
	
	/**
	 * Template method to be overwritten by concrete instantiations of this class. This method
	 * is called by the {@link FutureEvent#process(Object)} method and its outcome is used to
	 * resolve or ruin the Future attached to this event.
	 */
	public abstract Object execute(Object owner) throws Exception;

}
