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

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarObject;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.util.GUID;

/**
 * 
 * TODO document the class NATFarObject
 *
 * @author smostinc
 */
public class NATFarObject extends NATNil implements ATFarObject {
	
	private GUID actorId_;
	private int objectId_;
	private ATActor actor_;
	
	public ATText base_getActorId() {
		return NATText.atValue(actorId_.toString());
	}

	public ATNumber base_getObjectId() {
		return NATNumber.atValue(objectId_);
	}

	public NATFarObject(ATActor actor, GUID actorId, int objectId) {
		actor_ = actor;
		actorId_ = actorId;
		objectId_ = objectId;
	}
	
	/* ------------------------------
     * -- Message Sending Protocol --
     * ------------------------------ */

	public ATObject meta_send(ATAsyncMessage message) throws InterpreterException {
		// The super implementation of meta_getActor returns the executing one.
		return super.meta_getActor().base_send(message);
	}
	
	public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
		throw new XIllegalOperation("Received an asynchronous message for a far object reference which could not be resolved.");
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
		return actor_;
	}

}
