/**
 * AmbientTalk/2 Project
 * ATObjectID.java created on 21-dec-2006 at 12:04:03
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
package edu.vub.at.actors.id;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.jgroups.Address;

import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.natives.OBJLexicalRoot;

/**
 * An ATObjectID instance represents a globally unique identifier denoting
 * an AmbientTalk/2 object. It is represented by a triplet:
 *  ( VM id, Host actor id, object id )
 *
 * @author tvcutsem
 */
public class ATObjectID implements Serializable {

	private static final long serialVersionUID = -2108704271907943887L;

	// Virtual Machines are identified by IP address + port
	private final GUID virtualMachineId_;
	
	// Inside a Virtual Machine actors (which accept the messages for this far object)
	// are denoted by their hashcode
	private final int actorId_;
	
	// Inside an NATActorMirror, objects are denoted by their hashcode
	private final int objectId_;
	
	// For communication, Virtual Machines are denoted by an address, this field is filled in while serializing
	private Address virtualMachineAddress_ = null;
	
	public ATObjectID(GUID vmId, int actorId, int objectId) {
		virtualMachineId_ = vmId;
		actorId_ = actorId;
		objectId_ = objectId;
	}
	
	/*
	 * Three cases to consider:
	 *  an object can be:
	 *   - local to an actor (isFar -> false, isRemote -> false)
	 *   - hosted by another local actor (isFar -> true, isRemote -> false)
	 *   - hosted by a remote actor (isFar -> true, isRemote -> true)
	 */
	public boolean isFar() throws InterpreterException {
		return (actorId_ != OBJLexicalRoot._INSTANCE_.base_getActor().hashCode());
	}
	
	public boolean isRemote() {
		return (!virtualMachineId_.equals(ELVirtualMachine.currentVM().getGUID()));
	}
	
	/* -----------------------
	 * -- Structural Access --
	 * ----------------------- */
	
	public int getActorId() {
		return actorId_;
	}

	public int getObjectId() {
		return objectId_;
	}

	public GUID getVirtualMachineId() {
		return virtualMachineId_;
	}
	
	public int hashCode() {
		return virtualMachineId_.hashCode() | actorId_ | objectId_;
	}
	
	public boolean equals(Object other) {
		if (other instanceof ATObjectID) {
			ATObjectID id = (ATObjectID) other;
			return (id.getVirtualMachineId().equals(virtualMachineId_)
					&& (id.getActorId() == actorId_)
					&& (id.getObjectId() == objectId_));
		} else {
			return false;
		}
	}
	
	public String toString() {
		return virtualMachineId_.toString() + ":" + actorId_ + ":" + objectId_;
	}
	
	public Object writeReplace() throws ObjectStreamException {
		if(! this.isRemote())
			virtualMachineAddress_ = ELVirtualMachine.currentVM().getAddress();
		return this;
	}
	
}
