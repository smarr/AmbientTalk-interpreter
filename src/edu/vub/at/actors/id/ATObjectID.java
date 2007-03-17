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


import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.exceptions.InterpreterException;

import java.io.Serializable;
import java.util.Random;

/**
 * An ATObjectID instance represents a globally unique identifier denoting
 * an AmbientTalk/2 object. It is represented by a triplet:
 *  - object ID (uniquely identifying an object within one actor)
 *  - actor ID (uniquely identifying an object within 
 *  - virtual machine ID (uniquely identifying a virtual machine in the network)
 *
 * @author tvcutsem
 */
public class ATObjectID implements Serializable {

	private static final long serialVersionUID = -2108704271907943887L;

	private static final Random generator_ = new Random(System.currentTimeMillis());
	
	private final VirtualMachineID virtualMachineId_;
	private final ActorID actorId_;
	private final long objectId_;
	private final String description_;
	
	/**
	 * Creates a new unique object identifier for an object that lives on the given
	 * VM and is hosted by the given actor. The description parameter is used in
	 * the printed representation of far references.
	 */
	public ATObjectID(VirtualMachineID vmId, ActorID actorId, String description) {
		virtualMachineId_ = vmId;
		actorId_ = actorId;
		objectId_ = generator_.nextLong();
		description_ = description;
	}
	
	/*
	 * Three cases to consider:
	 *  an object can be:
	 *   - local to an actor (isFar -> false, isRemote -> false)
	 *   - hosted by another local actor (isFar -> true, isRemote -> false)
	 *   - hosted by a remote actor (isFar -> true, isRemote -> true)
	 */
	public boolean isFar() throws InterpreterException {
		return (!actorId_.equals(ELActor.currentActor().getActorID()));
	}
	
	public boolean isRemote() {
		return (!virtualMachineId_.equals(ELVirtualMachine.currentVM().getGUID()));
	}
	
	/* -----------------------
	 * -- Structural Access --
	 * ----------------------- */
	
	public ActorID getActorId() {
		return actorId_;
	}

	public VirtualMachineID getVirtualMachineId() {
		return virtualMachineId_;
	}
	
	public int hashCode() {
		return virtualMachineId_.hashCode() |
		       actorId_.hashCode() |
		       (int) objectId_;
	}
	
	public boolean equals(Object other) {
		if (other instanceof ATObjectID) {
			ATObjectID id = (ATObjectID) other;
			return (id.getVirtualMachineId().equals(virtualMachineId_)
					&& (id.getActorId().equals(actorId_))
					&& (id.objectId_ == objectId_));
		} else {
			return false;
		}
	}
	
	public String getDescription() {
		return description_;
	}
	
	public String toString() {
		return virtualMachineId_ + "|" + actorId_ + "|" + objectId_;
	}
	
}
