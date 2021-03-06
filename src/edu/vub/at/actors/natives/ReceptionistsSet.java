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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XObjectOffline;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

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
	 * local object (ATObject) -> object id (ATObjectID)
	 * under which this object is currently exported. This map
	 * and the previous map maintain a bi-directional mapping
	 * between objects and globally unique object IDs.
	 */
	private final HashMap exportedObjectIds_;

	
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
	
	/** Create a fresh receptionist owned by a given actor */
	public ReceptionistsSet(ELActor forActor) {
		owner_ = forActor;
		exportedObjectsTable_ = new HashMap();
		exportedObjectIds_ = new HashMap();
		remoteReferences_ = new Hashtable();
		farReferences_ = new Hashtable();
	}
	
	private NATRemoteFarRef createRemoteFarRef(ATObjectID objectId, ATTypeTag[] types, boolean isConnected) {
		NATRemoteFarRef farref;
		WeakReference pooled = (WeakReference) remoteReferences_.get(objectId);
		if (pooled != null) {
			farref = (NATRemoteFarRef) pooled.get();
			if (farref != null) {
				return farref;
			}
		}
		farref = new NATRemoteFarRef(objectId, owner_, types, isConnected);
		remoteReferences_.put(objectId, new WeakReference(farref));
		return farref;
	}
	
	private NATLocalFarRef createLocalFarRef(ELActor actor, ATObjectID objectId, ATTypeTag[] types, boolean isConnected) {
		NATLocalFarRef farref;
		WeakReference pooled = (WeakReference) farReferences_.get(objectId);
		if (pooled != null) {
			farref = (NATLocalFarRef) pooled.get();
			if (farref != null) {
				return farref;
			}
		}

		farref = new NATLocalFarRef(actor, objectId, types, owner_, isConnected);
		farReferences_.put(objectId, new WeakReference(farref));
		return farref;
	}
	
	/**
	 * Export a local object such that it now has a unique global identifier which
	 * can be distributed to other actors. Only near references may be exported.
	 * 
	 * @param object - the local object to export to the outside world
	 * @param description - a globally human-readable meaningful description of the object
	 * @return a local remote reference denoting the local object
	 * @throws XIllegalOperation - if object is a far reference
	 */
	public NATLocalFarRef exportObject(ATObject object, String description) throws InterpreterException {
		if (object.isNativeFarReference()) {
			throw new XIllegalOperation("Cannot export a far reference to " + object);
		}
		

		ATObjectID objId = null;

		// if this object was already exported, return a far ref with the same ID
		// as the one already stored
		if (exportedObjectIds_.containsKey(object)) {
			objId = (ATObjectID) exportedObjectIds_.get(object);
		} else {
			// get the host VM
			ELVirtualMachine currentVM = owner_.getHost();
			
			// create a new unique ID for the exported object, but make sure that the unique identity
			// remembers the VM id and actor ID from which the object originated
			objId = new ATObjectID(currentVM.getGUID(), owner_.getActorID(), description);
			
			exportedObjectsTable_.put(objId, object);
			exportedObjectIds_.put(object, objId);
		}
				
		// copy types of local object
		ATObject[] origTypes = object.meta_typeTags().asNativeTable().elements_;
		ATTypeTag[] types = new ATTypeTag[origTypes.length];
		System.arraycopy(origTypes, 0, types, 0, origTypes.length);
		return createLocalFarRef(owner_, objId, types, true);
	}
	
	public NATLocalFarRef exportObject(ATObject object) throws InterpreterException {
		return exportObject(object, object.toString());
	}
	
	/**
	 * Take offline a local object which was exported to the outside world. 
	 *  
	 * @param object - the local object exported to the outside world
	 * @throws XIllegalOperation if the object was not found locally
	 */
	public void takeOfflineObject(ATObject object) throws XIllegalOperation {
		if (exportedObjectIds_.containsKey(object)) {
			ATObjectID objId = (ATObjectID) exportedObjectIds_.get(object);
			exportedObjectsTable_.remove(objId);
			exportedObjectIds_.remove(object);
			//notify the rest of VM that this object was taken offline
			owner_.getHost().event_objectTakenOffline(objId, null);
		} else{
			
			//the object was not previously exported or it has already been taken offline
			//I don't know the objectId => throw XIllegalOperation
			throw new XIllegalOperation("Cannot take offline an object that is not online: " + object);
		}
	}
	
	public void softReset() throws XIllegalOperation {
		takeOfflineAll();
		exportedObjectsTable_.clear();
		exportedObjectIds_.clear();
		remoteReferences_.clear();
		farReferences_.clear();
	}
	
	private void takeOfflineAll() throws XIllegalOperation {
		List<ATObject> obj = new ArrayList<ATObject>(exportedObjectsTable_.values());
		for (ATObject atObject : obj) {
			takeOfflineObject(atObject);		
		}
		
	}
	
	/**
	 * A disconnected object is defined as:
	 * object: {
	 *   def object := //disconnected object;
	 *   def reconnect() { //reconnect & re-publish the disconnected object }
	 * }
	 */
	public class NATDisconnected extends NATObject {
		private final AGSymbol _OBJECT_ = AGSymbol.jAlloc("object");
		private final AGSymbol _RECONNECT_ = AGSymbol.jAlloc("reconnect");
		public NATDisconnected(final ATObject object, final ATObjectID objectId) throws InterpreterException {
			meta_defineField(_OBJECT_, object);
			meta_defineField(_RECONNECT_, 	new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					owner_.getHost().discoveryActor_.event_reconnectPublications(object);
					owner_.getHost().event_objectReconnected(objectId);
					return Evaluator.getNil();
				}
			});
		}
		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("<disconnected:"+impl_invokeAccessor(this, _OBJECT_, NATTable.EMPTY)+">");
		}
	}
	
	/**
	 * Disconnect a local object which was exported to the outside world. 
	 *  
	 * @param object - the local object exported to the outside world
	 * @throws XIllegalOperation if the object was not found locally
	 */
	public ATObject disconnect(final ATObject object) throws InterpreterException {
		if (exportedObjectIds_.containsKey(object)) {
			ATObjectID objId = (ATObjectID) exportedObjectIds_.get(object);
			NATDisconnected disco = new NATDisconnected(object, objId);
			//notify the rest of VM that this object was taken offline
			owner_.getHost().discoveryActor_.event_disconnectPublications(object);
			owner_.getHost().event_objectDisconnected(objId);
			return disco;
		} else{
			//check whether the object to disconnect is a far reference.
			if(object instanceof NATFarReference) {
				NATFarReference reference = (NATFarReference)object;
				ATObjectID objId = reference.impl_getObjectId();
				NATDisconnected disco = new NATDisconnected(object, objId);
				// notify locally the far reference, no need to broadcast the
				// 'disconnect' event, neither disconnect publications.
				owner_.getHost().connectionManager_.notifyObjectDisconnected(objId);
				return disco;
			} else{ 
			  //the object was not previously exported or it has already been disconnected
			  //I don't know the objectId => throw XIllegalOperation
			  throw new XIllegalOperation("Attempt to disconnect neither a local exported object nor a far reference: "+object);
			}
		}
	}
	
	/**
	 * Try to resolve a remote object reference into a local (near) reference.
	 * 
	 * @param objectId the identifier of the remote object
	 * @return either the local object corresponding to that identifier, or a far reference designating the id
	 * @throws XObjectOffline if the object was not found locally
	 */
	public ATObject resolveObject(ATObjectID objectId, ATTypeTag[] types, boolean isConnected) throws XObjectOffline {
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
			return createRemoteFarRef(objectId, types, isConnected);
		} else {
			// the designated object lives in the same VM as this actor
			ELActor localActor = owner_.getHost().getActor(objectId.getActorId());
			return createLocalFarRef(localActor, objectId, types, isConnected);
		}
	}
	
}
