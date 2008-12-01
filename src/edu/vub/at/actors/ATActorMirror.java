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
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * The class ATActorMirror prescribes the minimal set of methods to be provided by the default 
 * implementation of an actor mirror. Since new actor mirrors can be installed dynamically, 
 * ATActorMirror defines the dependencies of the lexically scoped objects on the dynamic 'actor 
 * context' to perform their duties. The hooks defined in this class relate to:
 * 
 * <ul>
 *   <li>Asynchronous message creation and sending to influence the communication protocol</li>
 *   <li>Service advertising and requesting to influence the service discovery protocol</li>
 *   <li>Mirror creation to provide support for true stratification</li>
 *   <li>Actor protocol installation to deal with actor mirror interaction and to allow freezing
 *   an actor with a given protocol</li>
 * </ul>
 *
 * @author smostinc
 */
public interface ATActorMirror extends ATObject {
		
    /* ---------------------------------------------------
     * -- Language Construct to NATActorMirror Protocol --
     * --------------------------------------------------- */

	/**
	 * Creates a first-class message in the language. Note that upon creation the
	 * message does not have a receiver yet. This field will be set once the message
	 * is actually being sent, a fact which can be intercepted by overriding the sendTo
	 * base-level method.
	 * 
	 * @param selector the name of the method to trigger remotely
	 * @param arguments the actual arguments of the message
	 * @param types the types with which the message will be born
	 */
	public ATAsyncMessage base_createMessage(ATSymbol selector, ATTable arguments, ATTable types) throws InterpreterException;
	
	/**
	 * Creates a mirror on the given object. This method serves as the 'mirror factory'
	 * for the current actor.
	 */
	public ATObject base_createMirror(ATObject onObject) throws InterpreterException;
	
	/**
	 * This method implements the default asynchronous message sending semantics for
	 * this particular actor. In addition to the ability to override the send meta-
	 * operation on a single object to have specific adaptions, this hook allows the
	 * programmer to modify the message sending semantics for all objects inside an 
	 * actor. The default implementation ensures the correct passing of messages when
	 * they transgress the boundaries of the sending actor. 
	 * 
	 * @throws InterpreterException
	 */
	public ATObject base_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException;
	
	/**
	 * When an actor receives an asynchronous message for a given receiver, it will delegate this
	 * to the meta-level 'receive' operation of the designated object. This operation is introduced
	 * as a mechanism to alter the semantics of message reception for all objects contained in an
	 * actor. It can be used e.g. to keep track of all succesfully processed messages. 
	 */
	public ATObject base_receive(ATObject receiver, ATAsyncMessage message) throws InterpreterException;
	
	/**
	 * This method provides access to a snapshot of the inbox of an actor. It is however not causally
	 * connected to the inbox; adding/removing elements to/from this snapshot will not affect the inbox of
	 * the actor.
	 * @return a table containing all letters that are scheduled to be processed
	 */
	public ATTable base_listIncomingLetters() throws InterpreterException;
	
	/**
	 * This mechanism allows for changing the scheduling semantics of the actor's inbox.
	 * Note: this method is responsible for calling the <tt>serve()</tt> method for each
	 * scheduled message, which should be executed at a later point in time.
	 * 
	 * @return a letter, which can be canceled again
	 */
	public ATObject base_schedule(ATObject receiver, ATAsyncMessage message) throws InterpreterException;
	
	/**
	 * This method fetches and processes the next letter from the inbox.
	 * It should take into account the possibility that the inbox is empty.
	 */
	public ATObject base_serve() throws InterpreterException;

	/**
	 * This method provides access to a snapshot of the current published services of an actor.
	 * The result is not causally connected; adding/removing elements to/from this snapshot will
	 * not affect the current publications.
	 * @return a table containing all publications of this actor
	 */
	public ATTable base_listPublications() throws InterpreterException;

	/**
	 * This method provides access to a snapshot of the current subscriptions of an actor.
	 * The result is not causally connected; adding/removing elements to/from this snapshot will
	 * not affect the current subscriptions.
	 * @return a table containing all subscriptions of this actor
	 */
	public ATTable base_listSubscriptions() throws InterpreterException;
	
	/**
	 * This mechanism is the most basic mechanism to provide a service. It requires 
	 * a separate service description and an object offering the service. The return
	 * value is a publication object which allows cancelling the service offer.
	 */
	public ATObject base_provide(ATTypeTag topic, ATObject service) throws InterpreterException;
	
	/**
	 * This mechanism is the most basic mechanism to require a service. The return
	 * value is a subscription object which allows cancelling the service offer.
	 * @param bool - if true, the subscription is permanent, if false, once the subscription
	 * has been satisfied, it is automatically cancelled.
	 */
	public ATObject base_require(ATTypeTag topic, ATClosure handler, ATBoolean bool) throws InterpreterException;
	
	/**
	 * Create a far reference to a local object. Custom actor mirrors may override
	 * this method in order to return different kinds of object references, e.g.
	 * leased object references.
	 * 
	 * @param toObject a **near** reference to the object to export
	 * @return a local far reference to the object being exported
	 * @throws XIllegalOperation if the passed object is a far reference, i.e. non-local
	 */
	public ATObject base_createReference(ATObject toObject) throws InterpreterException;
	
	/**
	 * def oldprotocol := actor.becomeMirroredBy: newprotocol
	 * 
	 * Installs a new meta-object protocol into this actor.
	 * 
	 * @param protocol meta-level code that overrides an actor's MOP methods
	 * @return the previously installed meta-object protocol
	 */
	public ATObject base_becomeMirroredBy_(ATActorMirror protocol) throws InterpreterException;
	
	/**
	 * def aM := implicitActorMirror.getExplicitActorMirror()
	 * 
	 * This method serves as the 'mirror factory' for explicit actor mirrors.
	 * 
	 * @return an explicit actor mirror for the current actor.
	 */
	public ATActorMirror base_getExplicitActorMirror() throws InterpreterException;
	
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
	 * @return a newly created far object reference
	 * 
	 * @see edu.vub.at.objects.ATObject#meta_pass(ATFarReference)
	 */
	//public ATFarReference base_export(ATObject object) throws InterpreterException;
	
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
	//public ATObject base_resolve(ATFarReference farReference) throws InterpreterException;

}
