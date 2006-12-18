/**
 * AmbientTalk/2 Project
 * ATActor.java created on Aug 21, 2006
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
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 *  
 */
public interface ATActor extends ATAbstractActor
{
	public static final ATSymbol _IN_ = AGSymbol.jAlloc("inbox");

	public static final ATSymbol _OUT_ = AGSymbol.jAlloc("outbox");

	public static final ATSymbol _REQUIRED_ = AGSymbol.jAlloc("required");

	public static final ATSymbol _PROVIDED_ = AGSymbol.jAlloc("provided");

    /* ------------------------------------------
     * -- Language Construct to Actor Protocol --
     * ------------------------------------------ */

	/**
	 * Creates a first-class message in the language. Note that upon creation the
	 * message does not have a receiver yet. This field wield be set once the message
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
	public ATClosure base_provide(ATServiceDescription description, ATObject service);
	
	/**
	 * This mechanism is the most basic mechanism to provide a service. It requires 
	 * a separate service description and an object offering the service. The return
	 * value is a closure which allows cancelling the service offer.
	 */
	public ATClosure base_require(ATServiceDescription description, ATObject client);
	
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
	 * @see edu.vub.at.objects.ATObject#meta_pass(ATFarObject)
	 */
	public ATFarObject base_reference_for_(ATObject object, ATFarObject client) throws InterpreterException;
	
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
	 * @param farReference the reference to be resolved
	 * @return a local object | a unique far reference for this actor
	 */
	public ATObject base_resolveFarReference(ATFarObject farReference) throws InterpreterException;

	/**
	 * Registers an object as a far reference denoting one or possibly more objects 
	 * on another actor. Registering is required to be consulted whenever a try-to-
	 * transmit command is issued by the virtual machine for a particular actor. The
	 * far objects can then schedule messages for transmission by the virtual machine
	 * provided that they claim to be hosted on the said actor.
	 *  
	 * @param farObject - object accumulating messages for an object hosted by another actor
	 * @return nil
	 */
	public ATNil base_registerFarReference(ATFarObject farObject) throws InterpreterException;
	
	public ATVirtualMachine base_getVirtualMachine();

    public ATObject base_getBehaviour();
    
    public ATBoolean base_isLocal();
    public ATBoolean base_isRemote();
    
}
