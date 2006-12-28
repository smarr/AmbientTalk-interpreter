/**
 * AmbientTalk/2 Project
 * ATActorMirror.java created on Aug 21, 2006
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 *  
 */
public interface ATActorMirror extends ATObject {
	
	public static final ATSymbol _IN_ = AGSymbol.jAlloc("inbox");

	public static final ATSymbol _OUT_ = AGSymbol.jAlloc("outbox");

	public static final ATSymbol _REQUIRED_ = AGSymbol.jAlloc("required");

	public static final ATSymbol _PROVIDED_ = AGSymbol.jAlloc("provided");

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
	
    /* ---------------------------------------------------
     * -- Language Construct to NATActorMirror Protocol --
     * --------------------------------------------------- */

	/**
	 * Creates a first-class message in the language. Note that upon creation the
	 * message does not have a receiver yet. This field will be set once the message
	 * is actually being sent, a fact which can be intercepted by overriding the sendTo
	 * base-level method.
	 */
	public ATAsyncMessage base_createMessage(ATObject sender, ATSymbol selector, ATTable arguments);
	
	/**
	 * This method implements the default asynchronous message sending semantics for
	 * this particular actor. In addition to the ability to override the send meta-
	 * operation on a single object to have specific adaptions, this hook allows the
	 * programmer to modify the message sending semantics for all objects inside an 
	 * actor. The default implementation ensures the correct passing of messages when
	 * they transgress the boundaries of the sending actor. 
	 * @throws InterpreterException
	 */
	public ATObject base_send(ATAsyncMessage message) throws InterpreterException;
	
	/**
	 * This mechanism is the most basic mechanism to provide a service. It requires 
	 * a separate service description and an object offering the service. The return
	 * value is a closure which allows cancelling the service offer.
	 */
	public ATClosure base_provide(ATServiceDescription description, ATObject service) throws InterpreterException;
	
	/**
	 * This mechanism is the most basic mechanism to provide a service. It requires 
	 * a separate service description and an object offering the service. The return
	 * value is a closure which allows cancelling the service offer.
	 * @throws InterpreterException 
	 */
	public ATClosure base_require(ATServiceDescription description, ATObject client) throws InterpreterException;
	
    /* -------------------------------------
     * -- Object Passing Protocol Support --
     * ------------------------------------- */

	/**
	 * This mechanism interacts with the built-in receptionists set of the actor to 
	 * produce a new far object reference to a local object. The created far object
	 * is by no means unique within the actor that created it. 
	 * 
	 * Creating such far references is performed when passing objects by reference
	 * in the meta_pass method of scoped objects such as closures, objects and fields.
	 * 
	 * @param object the local object to be given a reference to
	 * @param client the remote object to which it will be sent
	 * @return a newly created far object reference
	 * 
	 * @see edu.vub.at.objects.ATObject#meta_pass(ATFarReference)
	 */
	public ATFarReference base_reference_for_(ATObject object, ATFarReference client) throws InterpreterException;
	
	/**
	 * This mechanism interacts with the built-in receptionists set of the actor to 
	 * resolve far references (which were received as part of an async message). The
	 * method returns a local object whenever the far object denotes an object 
	 * hosted by this actor. 
	 * 
	 * If the denoted object is not hosted by this actor, a far object (possibly but
	 * not necessarily the passed one) is returned which is the local and unique 
	 * representative of the remote object. This object will contain the queue of 
	 * messages to be transmitted to the remote object. 
	 * 
	 * @param farReference the far reference to be resolved
	 * @return a local object | a unique far reference for this actor
	 */
	public ATObject base_resolveFarReference(ATFarReference farReference) throws InterpreterException;

}
