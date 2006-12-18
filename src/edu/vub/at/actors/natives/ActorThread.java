/**
 * AmbientTalk/2 Project
 * ActorThread.java created on Nov 3, 2006 at 9:39:19 PM
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
import edu.vub.at.exceptions.InterpreterException;

/**
 * An ActorThread is a thread which is parametrised with an actor for which it will
 * continually process meta-messages from its synchronised queue.
 *
 * @author smostinc
 */
public final class ActorThread extends Thread {

	private NATAbstractActor		owner_;
	private boolean				askedToStop_ = false;
	
	public ActorThread(NATAbstractActor owner) {
		owner_ 		= owner;
	}
	
	public NATAbstractActor currentlyServing() {
		return owner_;
	}
	
	public void stopProcessing() {
		askedToStop_ = true;
	}
		
	public void run() {
		
		while(! askedToStop_) {
			
			try {
				ATAsyncMessage event = (ATAsyncMessage)owner_.eventQueue_.take();
				
				System.out.println("Handling an event: " + event.base_getSelector());
				
				owner_.meta_invoke(owner_, event.base_getSelector(), event.base_getArguments());
				
			} catch (InterruptedException e) {
				// If interrupted, we may be asked to stop.
				continue;
			} catch (InterpreterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
