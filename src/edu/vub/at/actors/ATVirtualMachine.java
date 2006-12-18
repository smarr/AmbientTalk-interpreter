/**
 * AmbientTalk/2 Project
 * ATVirtualMachine.java created on Aug 21, 2006
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

import java.io.File;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;

/**
 * A virtual machine is deployed on top of a physical device and may host a set of actors.
 */
public interface ATVirtualMachine extends ATAbstractActor {
	
	
    public ATDevice base_getDevice();

    /* --------------------------
     * -- Actor Initialisation --
     * -------------------------- */
	
	public File[] getObjectPathRoots();
	
	public ATAbstractGrammar getInitialisationCode();
	
    /* --------------------------
     * -- Actor to VM Protocol --
     * -------------------------- */
        
	/**
	 * Called indirectly (by scheduling an event) to signal the creation of a
	 * new actor. This method then notifies possible observers.
	 */
	public ATNil base_newActor(ATActor actor);

    /**
     * This method is implicitly called (by scheduling an event) whenever an actor 
     * attempts to send a message which goes beyond its own boundaries. The virtual
     * machine is expected to schedule the message for transmission and should send
     * a delivered of failedDelivery event back to the actor. 
     */
    public ATNil base_transmit(ATAsyncMessage message) throws InterpreterException;
    
    /**
     * This method is implicitly called (by scheduling an event) whenever an object
     * is being offered as a service provide using the provide: language construct. 
     * The virtual machine keeps track of such services and is responsible for the 
     * matching between services and clients. When such matches are detected the VM
     * will send a foundResolution event to both involved actors. If the VM detects 
     * that one partner has become unavailable it will send the lostResolution event
     * 
     * @param description - a pattern allowing the service to be discovered
     * @param service - the object providing the service
     * @return nil
     */
    public ATNil base_servicePublished(ATServiceDescription description, ATObject service);
    
    /**
     * This method is implicitly called (by scheduling an event) whenever a service
     * offer is being revoked. In this case, the virtual machine ensures that the 
     * object is no longer discoverable to new clients. However, it will not send 
     * lostResolution events as these signal that an object has become unreachable.
     * 
     * @param description - a pattern allowing the service to be discovered
     * @param service - the object providing the service
     * @return nil
     */
    public ATNil base_cancelPublishing(ATServiceDescription description, ATObject service);
    
    
    /**
     * This method is implicitly called (by scheduling an event) whenever an object
     * requests a service using the require: language construct. The virtual machine 
     * keeps track of such requests and is responsible matching services and clients. 
     * When such matches are detected the VM will send a foundResolution event to both
     * involved actors. If the VM detects that one partner has become unavailable it 
     * will send the lostResolution event
     * 
     * @param description - a pattern describing the service to be discovered
     * @param client - the object requesting the service
     * @return nil
     */
    public ATNil base_serviceSubscription(ATServiceDescription description, ATObject client);

    /**
     * This method is implicitly called (by scheduling an event) whenever a service
     * request is being revoked. In this case, the virtual machine ensures that the 
     * object will no longer discover new services. However, it will not send 
     * lostResolution events as these signal that the client has become unreachable.
    * 
     * @param description - a pattern describing the service to be discovered
     * @param client - the object requesting the service
     * @return nil
     */
    public ATNil base_cancelSubscription(ATServiceDescription description, ATObject client);
	
    
}
