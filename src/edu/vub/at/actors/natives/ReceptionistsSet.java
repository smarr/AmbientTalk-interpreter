/**
 * AmbientTalk/2 Project
 * ReceptionistsSet.java created on Dec 5, 2006 at 10:58:44 PM
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

import java.util.HashMap;

import edu.vub.at.actors.ATFarObject;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.util.MultiMap;

/**
 * An Actor's ReceptionistsSet keeps a mapping between identifiers and local objects
 * which allows resolving far objects to local ones. The ReceptionistsSet also stores
 * which actors refer to an object, which is the basis for a distributed garbage 
 * collection algorithm.
 *
 * @author smostinc
 */
public class ReceptionistsSet {

	HashMap hashcodeToObject_;	
	MultiMap objectToClients_;
	
	public ReceptionistsSet(int expectedSize) {
		hashcodeToObject_ = new HashMap(expectedSize);
		objectToClients_ = new MultiMap();
	}
	
	
	public int exportObject(ATObject object) {
		int hashcode = object.hashCode();
		
		/*if object was not previously exported */ 
		if(hashcodeToObject_.get(hashcodeToObject_) == null) {
			hashcodeToObject_.put(new Integer(hashcode), object);
		}
		
		return hashcode;
	}
	
	public ATObject resolveObject(ATFarObject farReference) throws InterpreterException {
		Integer objectId = new Integer(farReference.getObjectId());
		
		/* 
		 * check if we have an object matching the new incoming description. 
		 * -> the object may be local and handed out using exportObject
		 * -> the object may be remote and already have a unique local representation
		 */
		ATObject result = (ATObject)hashcodeToObject_.get(objectId);

		if(result == null) { // This is a previously unknown object
			// Store it as the unique representation of this object in this actor
			hashcodeToObject_.put(objectId, farReference);
			result = farReference;
		}
		
		return result;
	}
	
	/**
	 * Invoked whenever another actor is passed a reference to an object offering a
	 * service on this actor. This is automatically done when serialising a local 
	 * object during parameter or result passing. Upon serialisation of a far 
	 * reference pointing to a service, a command object should be sent at the lowest
	 * level to invoke this method as well.
	 * 
	 * This method is related to a simple reference listing strategy which should be 
	 * the basis for a distributed garbage collector.
	 */
	public void addClient(ATObject service, ATFarObject client) throws InterpreterException {
		objectToClients_.put(service, client.meta_getActor());
	}

	/**
	 * Called upon the collection of a service on a remote device, or possibly when 
	 * the conditions negotiated for the use of the service (e.g. its lease period)
	 * have expired.
	 */
	public void removeClient(ATObject service, ATFarObject client) throws InterpreterException {
		objectToClients_.removeValue(service, client.meta_getActor());
	}
}
