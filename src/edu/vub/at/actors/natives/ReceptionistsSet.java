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
import edu.vub.at.actors.net.ConnectionListener;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XObjectOffline;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.util.MultiMap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * An NATActorMirror's ReceptionistsSet keeps a mapping between identifiers and local objects
 * which allows resolving far objects to local ones. The ReceptionistsSet also stores
 * which actors refer to an object, which is the basis for a distributed garbage 
 * collection algorithm.
 *
 * @author smostinc
 */
public class ReceptionistsSet {

    // TODO: these maps are not garbage-collected: obsolete entries are never removed
	
	/** object id (ATObjectID) -> local object (ATObject) */
	private final HashMap exportedObjectsTable_;	
	
	/**
	 * local object (ATObject) -> remote clients pointing to this object (Set of ATFarReference)
	 * @deprecated currently not in use
	 */
	private final MultiMap objectToClients_;
	
	private final ELActor owner_;
	
	/**
	 * a pool of remote object references: ensures that each ATObjectID is paired
	 * with a unique remote object reference (and corresponding outbox)
	 */
	private final Hashtable remoteReferences_;
	
	/**
	 * a pool of far object references: ensures that each ATObjectID is paired
	 * with a unique far object reference
	 */
	private final Hashtable farReferences_;
	
	public ReceptionistsSet(ELActor forActor) {
		owner_ = forActor;
		exportedObjectsTable_ = new HashMap();
		objectToClients_ = new MultiMap();
		remoteReferences_ = new Hashtable();
		farReferences_ = new Hashtable();
	}
	
	private NATRemoteFarRef createRemoteFarRef(ATObjectID objectId, ATStripe[] stripes) {
		NATRemoteFarRef farref;
		WeakReference pooled = (WeakReference) remoteReferences_.get(objectId);
		if (pooled != null) {
			farref = (NATRemoteFarRef) pooled.get();
			if (farref != null) {
				return farref;
			}
		}
		farref = new NATRemoteFarRef(objectId, owner_, stripes);
		remoteReferences_.put(objectId, new WeakReference(farref));
		return farref;
	}
	
	private NATLocalFarRef createLocalFarRef(ELActor actor, ATObjectID objectId, ATStripe[] stripes) {
		NATLocalFarRef farref;
		WeakReference pooled = (WeakReference) farReferences_.get(objectId);
		if (pooled != null) {
			farref = (NATLocalFarRef) pooled.get();
			if (farref != null) {
				return farref;
			}
		}

		farref = new NATLocalFarRef(actor, objectId, stripes, owner_);
		farReferences_.put(objectId, new WeakReference(farref));
		return farref;
	}
	
	/**
	 * Export a local object such that it now has a unique global identifier which
	 * can be distributed to other actors. Only near references may be exported.
	 * 
	 * @param object - the local object to export to the outside world
	 * @return a local remote reference denoting the local object
	 * @throws XIllegalOperation - if object is a far reference
	 */
	public NATLocalFarRef exportObject(ATObject object) throws InterpreterException {
		if (object.isFarReference()) {
			throw new XIllegalOperation("Cannot export a far reference to " + object);
		}
		
		// get the host VM
		ELVirtualMachine currentVM = owner_.getHost();

		// create a new unique ID for the exported object, but make sure that the unique identity
		// remembers the VM id and actor ID from which the object originated
		ATObjectID objId = new ATObjectID(currentVM.getGUID(), owner_.getActorID(), object.toString());
		
		// store the object if it was not previously exported */ 
		if(!exportedObjectsTable_.containsKey(objId)) {
			exportedObjectsTable_.put(objId, object);
		}
		
		// copy stripes of local object
		ATObject[] origStripes = object.meta_getStripes().asNativeTable().elements_;
		ATStripe[] stripes = new ATStripe[origStripes.length];
		System.arraycopy(origStripes, 0, stripes, 0, origStripes.length);
		return createLocalFarRef(owner_, objId, stripes);
	}
	
	/**
	 * Take offline a local object which was exported to the outside world. 
	 *  
	 * @param object - the local object exported to the outside world
	 * @throws XIllegalOperation if the object was not found locally
	 */
	public void takeOfflineObject(ATObject object) throws XIllegalOperation {
		
		if (exportedObjectsTable_.containsValue(object)){
			
			for (Iterator i = exportedObjectsTable_.entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				ATObject obj = (ATObject) entry.getValue();
				
				if (obj.equals(object)){
					i.remove();
					//notify the rest of VM that this object was taken offline
					owner_.getHost().event_objectTakenOffline((ATObjectID) entry.getKey(), null);
	
				}
			}
		}else{
			//the object was not previously exported or it has already been taken offline
			//I don't know the objectId => throw XIllegalOperation
			
			throw new XIllegalOperation("Cannot take offline an object that is not online: " + object);
		}
	}
	
	/**
	 * Try to resolve a remote object reference into a local (near) reference.
	 * 
	 * @param objectId the identifier of the remote object
	 * @return either the local object corresponding to that identifier, or a far reference designating the id
	 * @throws XObjectOffline if the object was not found locally
	 */
	public ATObject resolveObject(ATObjectID objectId, ATStripe[] stripes) throws XObjectOffline {
		if (objectId.getActorId().equals(owner_.getActorID())) { // does objectId denote a local object?
			
			// object should be found in this actor
			ATObject localObject = (ATObject) exportedObjectsTable_.get(objectId);
			if (localObject == null) {
				throw new XObjectOffline(objectId); // could not find the object locally
			} else {
				return localObject; // far ref now resolved to near ref
			}
			
		} else if (objectId.isRemote()) { // the resolved object is not local
			// the designated object does not live in this VM
			return createRemoteFarRef(objectId, stripes);
		} else {
			// the designated object lives in the same VM as this actor
			ELActor localActor = owner_.getHost().getActor(objectId.getActorId());
			return createLocalFarRef(localActor, objectId, stripes);
		}
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
	 * @deprecated currently not in use
	 */
	public void addClient(ATObject service, ATFarReference client) throws InterpreterException {
		objectToClients_.put(service, client);
	}

	/**
	 * Called upon the collection of a service on a remote device, or possibly when 
	 * the conditions negotiated for the use of the service (e.g. its lease period)
	 * have expired.
	 * @deprecated currently not in use
	 */
	public void removeClient(ATObject service, ATFarReference client) throws InterpreterException {
		objectToClients_.removeValue(service, client);
	}
}
