/**
 * AmbientTalk/2 Project
 * EventLoop.java created on 27-dec-2006 at 15:14:24
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

import java.util.Properties;

import edu.vub.at.util.logging.Logging;



/**
 * The EventLoop is the basic concurrency primitive for the AmbientTalk/2 implementation.
 * An Event Loop consists of an event queue, an event processing thread and an event handler.
 * The event loop's thread perpetually takes the next event from the event queue and makes
 * the event loop process it. Hence, the event loop is the event handler in this architecture.
 * 
 * Event Loops form the reusable core of both actors, remote references and even the virtual machine
 * itself (i.e. it is the core of both programmer-defined and native actors).
 * 
 * This is an abstract class. To be usable, subclasses have to provide a meaningful
 * implemementation strategy for handling events by overriding the handle method.
 *
 * @author tvcutsem
 * @author smostinc
 */
public abstract class EventLoop {

	/**
	 * The event loop's event queue is a synchronized queue of Event objects.
	 * It is the sole communication channel between different event loops.
	 * As such, it is the means by which different actors - and event actors and
	 * their virtual machines - communicate.
	 */
	protected final EventQueue eventQueue_;
	
	/**
	 * Each event loop has an event processor, which is a thread responsible
	 * for perpetually dequeuing events from the event queue and passing them
	 * on to this event loop's handle method.
	 */
	protected Thread processor_;
	
	protected volatile boolean askedToStop_;
	
	private final String name_;

	private static final String _ENV_AT_STACK_SIZE_ = "AT_STACK_SIZE";
	
	/**
	 * Constructs a new event loop with the default processing behaviour.
	 * Note: the creator must explicitly call {@link this#start()} to start
	 * the event loop!
	 * 
	 * @param name used for debugging purposes
	 */
	public EventLoop(String name) {
		eventQueue_ = new EventQueue();
		askedToStop_ = false;
		name_ = name;
		processor_ = new EventProcessor();
	}
	
	public EventLoop(String name, int stackSize) {
		eventQueue_ = new EventQueue();
		askedToStop_ = false;
		name_ = name;
		processor_ = new EventProcessor(stackSize);
	}
	
	/**
	 * Starts the execution of this event loop.
	 */
	public void start() {
		processor_.start();
	}
		
	public String toString() {
		return name_;
	}
	
	/**
	 * Attempts to cast a {@link Thread} to an {@link EventLoop}. This code performs error checking and should
	 * therefore be used whenever a Thread (typically the current thread) needs to be cast into
	 * an EventLoop
	 * @param t a Java Thread to be cast into an event loop
	 * @return t as an EventLoop
	 * @throws IllegalStateException when the cast failed.
	 */
	public static EventLoop toEventLoop(Thread t) throws IllegalStateException {
		try {
		  EventProcessor processor = (EventProcessor) t;
	      return processor.serving();
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new IllegalStateException("Asked to transform a non-event loop thread to an event loop");
		}
	}
	
	/**
	 * Allows access to the currently running event loop.
	 * @return the currently serving event loop
	 * @throws IllegalStateException if the current thread is not the thread of an event loop
	 */
	public static EventLoop currentEventLoop() throws IllegalStateException {
		Thread current = Thread.currentThread();
		try {
		  EventProcessor processor = (EventProcessor) current;
	      return processor.serving();
		} catch (ClassCastException e) {
			throw new IllegalStateException("Asked for current event loop when none was active");
		}
	}
	
	/**
	 * Method to interrupt the EventLoop before it starts processing its next event. Note that
	 * this method consequently does not help when en event loop is stuck in an endless loop
	 * while evaluating a single event.
	 */
	public final void stopProcessing() {
		askedToStop_ = true;
		// explicitly interrupt my event processor because it
		// may be blocked waiting on other events
		processor_.interrupt();
	}
	
	/**
	 * When an event loop receives an asynchronously emitted event, this message is
	 * immediately placed into its incoming event queue and will be processed later.
	 * 
	 * This method is declared protected such that subclasses can provide a cleaner
	 * interface as to what kind of events can be received by this event loop.
	 * The convention is that a subclass provides a number of methods prefixed with
	 * event_ which call this protected method to schedule a certain event.
	 */
	protected final void receive(Event event) {
		eventQueue_.enqueue(event);
	}
	
	/**
	 * Schedules an event in this event loop's queue which will execute the provided
	 * callable object at a later point in time. Moreover, this method immediately
	 * makes the calling thread WAIT for the return value or resulting exception
	 * of the callable.
	 * 
	 * Caller must ensure that the thread invoking this method is not this event
	 * loop its own thread, which inevitably leads to deadlock.
	 * 
	 * This method is declared protected such that subclasses can provide a cleaner
	 * interface as to what kind of tasks may be scheduled in this event loop.
	 * The convention is that a subclass provides a number of methods prefixed with
	 * sync_event_ which call this protected method to schedule a certain task.
	 * 
	 * @param description a description of the task being scheduled, for debugging purposes
	 * @param callable the functor object encapsulating the task to be performed inside the event loop
	 */
	protected final Object receiveAndWait(String description, final Callable callable) throws Exception {
		/*if (Thread.currentThread() == processor_) {
			throw new RuntimeException("Potential deadlock detected: "
					+ processor_ + " tried to perform a synchronous operation on itself");
		}
		
		BlockingFuture future = new BlockingFuture();
		eventQueue_.enqueue(new FutureEvent(description, future) {
			private static final long serialVersionUID = 1672724382106164388L;

			public Object execute(Object owner) throws Exception {
				return callable.call(owner);
			}
		});*/
		BlockingFuture future = receiveAndReturnFuture(description, callable);
		return future.get();
	}
	
	protected final BlockingFuture receiveAndReturnFuture(String description, final Callable callable) throws Exception {
		if (Thread.currentThread() == processor_) {
			throw new RuntimeException("Potential deadlock detected: "
					+ processor_ + " tried to perform a synchronous operation on itself");
		}
		
		BlockingFuture future = new BlockingFuture();
		eventQueue_.enqueue(new FutureEvent(description, future) {
			private static final long serialVersionUID = 1672724382106164388L;

			public Object execute(Object owner) throws Exception {
				return callable.call(owner);
			}
		});
		return future;
	}
	
	/**
	 * When an event loop receives an asynchronously emitted event, this message is
	 * immediately placed into its incoming event queue and will be processed later.
	 * Using this method, events are scheduled first in the queue of upcoming events.
	 * 
	 * This method is declared protected such that subclasses can provide a cleaner
	 * interface as to what kind of events can be received by this event loop.
	 * The convention is that this method is only used to put back events that could
	 * not be processed due to problems outside of the influence of the interpreter 
	 * (e.g. host unreachability). If a subclass provides direct access to this 
	 * primitive it should do so in methods prefixed with prioritized_event_ which 
	 * call this protected method to schedule a certain event.
	 */
	protected final void receivePrioritized(Event event) {
		eventQueue_.enqueueFirst(event);
	}
	
	/**
	 * Subclasses are responsible for defining a meaningful implementation
	 * strategy to handle events from the event queue.
	 * 
	 * @param event the event object which was dequeued and which should be processed
	 */
	public abstract void handle(Event event);
	
	protected final EventLoop owner() { return this; }
	
	/**
	 * Invoked by the event processor thread continually while
	 * the event loop is alive.
	 * 
	 * The default behaviour is to continually dequeue events
	 * and make a subclass handle the event.
	 * 
	 * Overridable by subclasses to customize event processing.
	 */
	protected void execute() {
		try {
			Event event = eventQueue_.dequeue();

			Logging.EventLoop_LOG.debug(owner() + " is processing " + event);

			handle(event);
		} catch (InterruptedException e) {
			// If interrupted, we may be asked to stop
		}
	}
	
	/**
	 * EventProcessor is a thread subclass whose primary goal is to keep a reference to the 
	 * associated event loop, which is used to transform threads into the more manageable 
	 * event loops
	 */
	public final class EventProcessor extends Thread {
		
		protected EventProcessor(int stackSize) {
			super(new ThreadGroup("EventLoopGroup"), null, "Event Loop", stackSize);
			setName(toString());
		}
		
		protected EventProcessor(){
			setName(toString());
		}
		
        protected EventLoop serving() { return owner(); }
		
		public final void run() {
			while(!askedToStop_) {
				execute();
				
				// give other event loops a chance to process an event
				Thread.yield();
			}
		}
        
		public String toString() {
			return "Event Loop " + serving().toString();
		}
		
	}
	
}
