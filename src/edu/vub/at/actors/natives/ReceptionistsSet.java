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

import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATObject;
import edu.vub.util.MultiMap;

import java.util.HashMap;

/**
 * An NATActorMirror's ReceptionistsSet keeps a mapping between identifiers and local objects
 * which allows resolving far objects to local ones. The ReceptionistsSet also stores
 * which actors refer to an object, which is the basis for a distributed garbage 
 * collection algorithm.
 *
 * @author smostinc
 */
public class ReceptionistsSet {

	/** object id (ATObjectID) -> local object (ATObject) */
	HashMap exportedObjectsTable_;	
	
	/** local object (ATObject) -> remote clients pointing to this object (Set of ATFarReference) */
	MultiMap objectToClients_;
	
	NATActorMirror owner_;
	
	public ReceptionistsSet(NATActorMirror forActor) {
		exportedObjectsTable_ = new HashMap();
		objectToClients_ = new MultiMap();
		owner_ = forActor;
	}
	
	/**
	 * Export a local object such that it now has a unique global identifier which
	 * can be distributed to other actors. Only near references may be exported.
	 * 
	 * @param object - the local object to export to the outside world
	 * @return a unique identifier denoting the local object
	 * @throws XIllegalOperation - if object is a far reference
	 */
	public ATObjectID exportObject(ATObject object) throws InterpreterException {
		if (object.base_isFarReference()) {
			throw new XIllegalOperation("Cannot export a far reference to " + object);
		}
		
		// get the current VM
		ELVirtualMachine currentVM = owner_.getProcessor().getHost();

		// combine VM guid, actor hash and object hash into an ATObjectID
		ATObjectID objId = new ATObjectID(currentVM.getGUID(), owner_.hashCode(), object.hashCode());
		
		// store the object if it was not previously exported */ 
		if(!exportedObjectsTable_.containsKey(objId)) {
		   exportedObjectsTable_.put(objId, object);
		}
		
		return objId;
	}
	
	/**
	 * Try to resolve a remote object reference into a local (near) reference.
	 * 
	 * @param objectId the identifier of the remote object
	 * @return either the local object corresponding to that identifier, or null if there is no match
	 * @throws InterpreterException
	 */
	public ATObject resolveObject(ATObjectID objectId) throws InterpreterException {
		return (ATObject) exportedObjectsTable_.get(objectId);
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
	public void addClient(ATObject service, ATFarReference client) throws InterpreterException {
		objectToClients_.put(service, client);
	}

	/**
	 * Called upon the collection of a service on a remote device, or possibly when 
	 * the conditions negotiated for the use of the service (e.g. its lease period)
	 * have expired.
	 */
	public void removeClient(ATObject service, ATFarReference client) throws InterpreterException {
		objectToClients_.removeValue(service, client);
	}
}
