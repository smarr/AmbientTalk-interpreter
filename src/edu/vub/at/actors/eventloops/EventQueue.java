/**
 * AmbientTalk/2 Project
 * EventQueue.java created on 21-dec-2006 at 11:30:59
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

import java.util.Vector;

/**
 * A simple synchronized blocking FIFO queue implementation.
 * The buffer will grow dynamically to accomodate more events when necessary.
 *
 * @author tvcutsem
 */
public final class EventQueue {

	private static final int _DEFAULT_QUEUE_SIZE_ = 10;
	
	private final Vector elements_;
	
	public EventQueue() {
		elements_ = new Vector(_DEFAULT_QUEUE_SIZE_);
	}
	
	/**
	 * Enqueue an event in the buffer. This method wakes up any
	 * waiting consumer threads.
	 */
	public void enqueue(Event event) {
		synchronized(this) {
			elements_.add(event);
			notifyAll();
		}
	}
	
	/**
	 * Enqueue an event as the first to be executed in the buffer. 
	 * This method wakes up any waiting consumer threads.
	 */
	public void enqueueFirst(Event event) {
		synchronized(this) {
			elements_.add(0, event);
			notifyAll();
		}
	}
	
	/**
	 * Dequeue an event from the buffer. This method will block when the
	 * buffer is empty!
	 * 
	 * @return the dequeued event.
	 * @throws InterruptedException if the thread is interrupted while waiting on an empty buffer.
	 */
	public Event dequeue() throws InterruptedException {
		synchronized(this) {
			while(elements_.isEmpty()) {
				wait();
			}
			return (Event) elements_.remove(0);
		}
	}
	
	/**
	 * Clears the content of the buffer and returns the old contents.
	 * @return the buffer's content at the time it was flushed
	 */
	public Vector flush() {
		synchronized(this) {
			Vector copy = (Vector) elements_.clone();
			elements_.clear();
			return copy;
		}
	}
	
	/**
	 * Allows for restoring the buffer's old contents by putting
	 * the elements in contents in front of the buffer.
	 */
	public void merge(Vector contents) {
		synchronized(this) {
			elements_.addAll(0, contents);
		}
	}
	
}
