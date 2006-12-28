/**
 * AmbientTalk/2 Project
 * ATAbstractActor.java created on Dec 1, 2006
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
package edu.vub.at.actors;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;

/**
 * ATAbstractActor describes the common interface of all logical processes in the 
 * AmbientTalk/2 implementation. All such processes are implemented as actors which
 * continually consume meta-level events from a synchronized event queue. Communication
 * between all processes is thus strictly organized by passing events.
 *
 * @deprecated abstract actors are no longer reified
 * @author smostinc
 */
public interface ATAbstractActor extends ATObservable {

	/**
	 * Accept an incoming asynchronous message. By default, such messages are scheduled
	 * in an inbox.
	 * @param message - the async base-level message to accept
	 */
	public ATObject base_accept(ATAsyncMessage message) throws InterpreterException;

	/**
	 * Processes a message from the base-level inbox if it is non-empty.
	 */
	public ATObject base_process() throws InterpreterException;

}