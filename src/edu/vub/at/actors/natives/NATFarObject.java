/**
 * AmbientTalk/2 Project
 * NATFarObject.java created on Dec 6, 2006 at 9:53:20 AM
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

import java.net.InetSocketAddress;

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarObject;
import edu.vub.at.actors.natives.events.ActorEmittedEvents;
import edu.vub.at.actors.natives.events.VMEmittedEvents;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * 
 * TODO document the class NATFarObject
 *
 * @author smostinc
 */
public class NATFarObject extends NATNil implements ATFarObject {
	
	public static final ATSymbol _OUT_ = AGSymbol.jAlloc("outbox");
	
	// Contains the messages buffered by the far object reference, waiting for transmission by the virtual machine
	private final NATMailbox buffered_;
	
	// Virtual Machines are identified by IP address + port
	private final InetSocketAddress virtualMachineId_;
	
	// Inside a Virtual Machine actors (which accept the messages for this far object) are denoted by their hashcode
	private final int actorId_;
	
	// Inside an Actor, objects are denoted by their hashcode
	private final int objectId_;
	
	public NATFarObject(ATActor actor, int objectId) {
		
		buffered_ = new NATMailbox(actor, _OUT_);
		
		virtualMachineId_ = null /* actor.base_getVirtualMachine(). */;
		actorId_ = actor.hashCode();
		objectId_ = objectId;
	}
	
	public ATNil meta_flush(ATObject destination) {
		// TODO(new conc model) implement flushing of the far object buffer
		return NATNil._INSTANCE_;		
	}
	
	public ATObject meta_resolve() throws InterpreterException {
		
		ATObject resolved = super.meta_getActor().base_resolveFarReference(this);
		
		if(resolved.base_isFarReference().asNativeBoolean().javaValue) {
			super.meta_getActor().base_registerFarReference(resolved.base_asFarReference());
		}
		
		return resolved;		
	}
	
	public ATBoolean meta_isHostedBy_(ATActor host) throws InterpreterException {
		// TODO(implement) far actors
//		return NATBoolean.atValue(meta_getActor().equals(far));
		return NATBoolean._FALSE_;
	}

	/* ------------------------------
     * -- Message Sending Protocol --
     * ------------------------------ */

	public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
		message = message.meta_pass(this).base_asAsyncMessage();
		
		buffered_.base_enqueue(message);
		
		// TODO(new conc model) document
		// NOTE This event is emitted by both the VM and far objects on behalf of 
		// the actors themselves - for 
		// clarity the definition belongs to VMEmittedEvents as the VM will use this
		// event whenever detecting the presence of an actor. It's presence is also
		// documented in ActorEmittedEvents in the Actor to Self Protocol section.
		// super.meta_getActor().base_scheduleEvent(VMEmittedEvents.attemptTransmission(meta_getActor()));
		
		return NATNil._INSTANCE_;
	}
	
	/**
	 * When passing a far reference to another actor, notify the surrounding actor to
	 * allow for implementing a distributed garbage collection strategy.
	 */
	public ATObject meta_pass(ATFarObject client) throws InterpreterException {
		if(client.base_isFarReference().asNativeBoolean().javaValue)
			super.meta_getActor().base_scheduleEvent(ActorEmittedEvents.passingFarReference(this, client.base_asFarReference()));
		return this;
	}

	/**
	 * @throws XIllegalOperation Cannot synchronously invoke a method on a far reference
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
		throw new XIllegalOperation("Cannot synchronously invoke a method on a far reference");
	}

	/**
	 * @return true if and only if the far object is queried for responses to basic operations such as ==
	 */
	public ATBoolean meta_respondsTo(ATSymbol atSelector) throws InterpreterException {
		return super.meta_respondsTo(atSelector);
	}

	/**
	 * @throws XSelectorNotFound to ensure proper semantics should the interpreter be
	 * extended such that it allows extending a far reference in the future.
	 */
	public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		return super.meta_doesNotUnderstand(selector);
	}

	/* ------------------------------------
     * -- Extension and cloning protocol --
     * ------------------------------------ */

	/**
	 * References to objects hosted by another actor are forced to be unique. Therefore
	 * cloning them throws an XIllegalOperation to avoid inconsistencies by performing
	 * state updates (through sent messages) after a clone operation. 
	 * 
	 * TODO(discuss) clone: farObject may create a clone on the other actor.
	 */
	public ATObject meta_clone() throws InterpreterException {
		throw new XIllegalOperation("Cannot clone a far reference to an object");
	}

	/**
	 * Cannot create a new instance using a farObject, this should be done either by 
	 * sending rather than invoking new(args) such that the correct method is triggered
	 * or by invoking newInstance on a farMirror, which will send the call as well. 
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		throw new XIllegalOperation("Cannot create a new instance from a far reference");
	}

	/**
	 * TODO(discuss) Think about extending objects of another actor. The result should be
	 * a far reference, although the child object may be hosted on your actor. This enforces
	 * correct use of the sending operation the only concern is then to check no super 
	 * invocations are made by the child.
	 */
	public ATObject meta_extend(ATClosure code) throws InterpreterException {
		throw new XIllegalOperation("Extending a far reference is currently not supported.");
	}

	/**
	 * TODO(discuss) Think about sharing objects of another actor. The result should be
	 * a far reference, although the child object may be hosted on your actor. This enforces
	 * correct use of the sending operation the only concern is then to check no super 
	 * invocations are made by the child.
	 */
	public ATObject meta_share(ATClosure code) throws InterpreterException {
		throw new XIllegalOperation("Sharing a far reference is currently not supported.");
	}
	
	/* ------------------------------------------
     * -- Slot accessing and mutating protocol --
     * ------------------------------------------ */
	
	/**
	 * @throws XIllegalOperation - cannot select in objects hosted by another actor.
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		throw new XIllegalOperation("Cannot select variables or methods on an object of type " + this.getClass().getName());
	}

	/**
	 * @throws XIllegalOperation - cannot lookup in objects hosted by another actor.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
		throw new XIllegalOperation("Cannot lookup variables or methods on an object of type " + this.getClass().getName());
	}

	/**
	 * @throws XIllegalOperation - cannot define in objects hosted by another actor.
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		throw new XIllegalOperation("Cannot define variables in an object of type " + this.getClass().getName());
	}

	/**
	 * @throws XIllegalOperation - cannot assign in objects hosted by another actor.
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
		throw new XIllegalOperation("Cannot assign fields on an object of type " + this.getClass().getName());
	}

	/**
	 * @throws XIllegalOperation - cannot assign in objects hosted by another actor.
	 */
	public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
		throw new XIllegalOperation("Cannot assign fields on an object of type " + this.getClass().getName());
	}

    /* ----------------------------------------
     * -- Object Relation Testing Protocol   --
     * ---------------------------------------- */

    /**
     * @return false unless this == original
     */
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return NATBoolean.atValue(this == original);
	}

    /**
     * @return false unless this == original
     */
	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return this.meta_isCloneOf(object);
	}

    /* ---------------------------------
     * -- Structural Access Protocol  --
     * --------------------------------- */
	
	/**
	 * @throws XIllegalOperation - cannot add fields to an object in another actor.
	 */
	public ATNil meta_addField(ATField field) throws InterpreterException {
		return super.meta_addField(field);
	}

	/**
	 * @throws XIllegalOperation - cannot add methods to an object in another actor.
	 */
	public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
		return super.meta_addMethod(method);
	}

	/**
	 * @throws XSelectorNotFound - as the far object has no fields of its own
	 */
	public ATField meta_grabField(ATSymbol fieldName) throws InterpreterException {
		return super.meta_grabField(fieldName);
	}

	/**
	 * @return a method if and only if the requested selector is a default operator such as == 
	 * @throws XSelectorNotFound otherwise
	 */
	public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
		return super.meta_grabMethod(methodName);
	}

	/**
	 * @return an empty table
	 */
	public ATTable meta_listFields() throws InterpreterException {
		return super.meta_listFields();
	}

	/**
	 * @return a table of default methods
	 */
	public ATTable meta_listMethods() throws InterpreterException {
		return super.meta_listMethods();
	}

    /* ----------------------
     * -- Output Protocol  --
     * ---------------------- */
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<far reference:"+this.hashCode()+">");
	}
	
    /* --------------------
     * -- Mirror Fields  --
     * -------------------- */

	public ATObject meta_getDynamicParent() throws InterpreterException {
		throw new XSelectorNotFound(AGSymbol.jAlloc("dynamicParent"), this);
	}

	public ATObject meta_getLexicalParent() throws InterpreterException {
		throw new XSelectorNotFound(AGSymbol.jAlloc("lexicalParent"), this);
	}
	
	public ATActor meta_getActor() {
		// TODO(implement) far actors, so a far object can return one
		// FIXME return actor_;
		return null;
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

	public InetSocketAddress getVirtualMachineId() {
		return virtualMachineId_;
	}

}
