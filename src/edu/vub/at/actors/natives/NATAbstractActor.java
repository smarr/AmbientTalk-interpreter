/**
 * AmbientTalk/2 Project
 * NATAbstractActor.java created on Nov 23, 2006 at 1:52:28 PM
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

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.vub.at.actors.ATAbstractActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATNil;

/**
 * NATAbstractActor specifies the reusable behaviour of an actor class which has an
 * event queue containing Messages. Creating an Abstract Actor also involves creating
 * a thread which will continually take these messages from the queue and meta_invoke
 * them.
 *
 * @author smostinc
 */
public abstract class NATAbstractActor extends NATObservable implements ATAbstractActor {
	
	/*
	 * The event queue is a synchronized queue of events which the actor needs to 
	 * process. As both visible actors, as well as the Virtual Machine and the 
	 * default interfaces to AmbientTalk are implemented as actors, these events are
	 * the prime communication mechanism between these components.
	 */
	final LinkedBlockingQueue eventQueue_ = new LinkedBlockingQueue();
	
	/* 
	 * Each Actor (at least conceptually) has a dedicated thread processing its events
	 */
	protected final ActorThread executor_;
	
	protected NATAbstractActor() {
		executor_ = new ActorThread(this);
		executor_.start();
	}
	
	public ATNil base_scheduleEvent(ATAsyncMessage event) {
		eventQueue_.offer(event);
		return NATNil._INSTANCE_;
	}
	
	/**
	 * Hook called by the ActorThread before attempting to invoke any of the events
	 * in the queue. Should be implemented to ensure correct initialisation. 
	 */
	public abstract ATObject base_init(ATObject[] initArgs) throws InterpreterException;
	
	/**
	 * Hook called by the ActorThread before stopping.Should be implemented to ensure
	 * correct stopping. 
	 */
	protected abstract NATNil base_dispose() throws InterpreterException;

	// Should be overridden
	public abstract ATObject meta_clone() throws InterpreterException;

	// Should be overridden
	public abstract ATObject meta_newInstance(ATTable initargs) throws InterpreterException;
	
}
