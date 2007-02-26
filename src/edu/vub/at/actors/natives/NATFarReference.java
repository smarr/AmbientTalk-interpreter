/**
 * AmbientTalk/2 Project
 * NATFarReference.java created on Dec 6, 2006 at 9:53:20 AM
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * 
 * NATFarReference is the root of the native classes that represent native far references.
 * The AmbientTalk/2 implementation distinguishes between two kinds of far references:
 * local and remote far references. The former denote far references to objects hosted by
 * actors on the same virtual machine. The latter ones denote far references to remote objects
 * that are hosted on a separate virtual (and usually even physical) machine.
 * 
 * This abstract superclass encapsulates all of the code that these two kinds of far references
 * have in common. The variabilities are delegated to the subclasses. Subclasses should implement
 * an abstract method (transmit) which is invoked by this class when the far reference receives
 * a message to be forwarded to the remote principal.
 * 
 * Note that far references are pass by copy and resolve to either a near or a new
 * actor-local far reference.
 * 
 * Far references encapsulate the same stripes as the remote object they represent.
 * As such it becomes possible to perform a stripe test on a far reference as if it
 * was performed on the local object itself!
 * 
 * @author tvcutsem
 * @author smostinc
 */
public abstract class NATFarReference extends NATByCopy implements ATFarReference {
	
	// encodes the identity of the far object pointed at
	private final ATObjectID objectId_;
	
	// the stripes with which the remote object is tagged
	private final ATStripe[] stripes_;

	protected NATFarReference(ATObjectID objectId, ATStripe[] stripes) {
		objectId_ = objectId;
		stripes_ = stripes;
	}
	
	public ATObjectID getObjectId() {
		return objectId_;
	}
	
	public NATFarReference asNativeFarReference() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * After deserialization, ensure that only one unique remote reference exists for
	 * my target.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		// it may be that the once local target object is now remote!
		return ELActor.currentActor().resolve(getObjectId(), stripes_);
	}

	/* ------------------------------
     * -- Message Sending Protocol --
     * ------------------------------ */

	public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
		return this.transmit(message);
	}
	
	protected abstract ATObject transmit(ATAsyncMessage passedMessage) throws InterpreterException;

	/**
	 * @throws XIllegalOperation Cannot synchronously invoke a method on a far reference
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
		throw new XIllegalOperation("Cannot invoke " + atSelector + " on far reference " + this);
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
		throw new XIllegalOperation("Cannot clone far reference " + this);
	}

	/**
	 * Cannot create a new instance using a farObject, this should be done either by 
	 * sending rather than invoking new(args) such that the correct method is triggered
	 * or by invoking newInstance on a farMirror, which will send the call as well. 
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		throw new XIllegalOperation("Cannot create new instance of far reference " + this);
	}

	/**
	 * TODO(discuss) Think about extending objects of another actor. The result should be
	 * a far reference, although the child object may be hosted on your actor. This enforces
	 * correct use of the sending operation the only concern is then to check no super 
	 * invocations are made by the child.
	 */
	public ATObject meta_extend(ATClosure code) throws InterpreterException {
		throw new XIllegalOperation("Cannot extend far reference " + this + " with " + code);
	}

	/**
	 * TODO(discuss) Think about sharing objects of another actor. The result should be
	 * a far reference, although the child object may be hosted on your actor. This enforces
	 * correct use of the sending operation the only concern is then to check no super 
	 * invocations are made by the child.
	 */
	public ATObject meta_share(ATClosure code) throws InterpreterException {
		throw new XIllegalOperation("Cannot share far reference " + this + " with " + code);
	}
	
	/* ------------------------------------------
     * -- Slot accessing and mutating protocol --
     * ------------------------------------------ */
	
	/**
	 * @throws XIllegalOperation - cannot select in objects hosted by another actor.
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		throw new XIllegalOperation("Cannot select " + selector + " from far reference " + this);
	}

	/**
	 * @throws XIllegalOperation - cannot lookup in objects hosted by another actor.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
		throw new XIllegalOperation("Cannot lookup " + selector + " from far reference " + this);
	}

	/**
	 * @throws XIllegalOperation - cannot define in objects hosted by another actor.
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		throw new XIllegalOperation("Cannot define field " + name + " in far reference " + this);
	}

	/**
	 * @throws XIllegalOperation - cannot assign in objects hosted by another actor.
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
		throw new XIllegalOperation("Cannot assign field " + name + " in far reference " + this);
	}

	/**
	 * @throws XIllegalOperation - cannot assign in objects hosted by another actor.
	 */
	public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
		throw new XIllegalOperation("Cannot assign variable " + name + " in far reference " + this);
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
		return NATText.atValue("<far ref:"+objectId_+">");
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
	
	/**
	 * The stripes of a far reference are the stripes of the remote object
	 * it points to, plus the FarReference stripe.
	 */
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.atValue(stripes_);
    }
	
	public boolean base_isFarReference() {
		return true;
	}

    public ATFarReference base_asFarReference() throws XTypeMismatch {
  	    return this;
  	}
    
	public static class NATDisconnectionSubscription extends NATObject {
		private static final AGSymbol _REFERENCE_ = AGSymbol.jAlloc("reference");
		private static final AGSymbol _HANDLER_ = AGSymbol.jAlloc("handler");
		private static final AGSymbol _CANCEL_ = AGSymbol.jAlloc("cancel");
		public NATDisconnectionSubscription(final NATFarReference reference, ATClosure handler) throws InterpreterException {
			this.meta_defineField(_REFERENCE_, reference);
			this.meta_defineField(_HANDLER_, handler);
			this.meta_defineField(_CANCEL_, 	new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					NATFarReference reference = scope_.meta_select(scope_, _REFERENCE_).asNativeFarReference();
					if(reference instanceof NATRemoteFarRef) {
						NATRemoteFarRef remote = (NATRemoteFarRef)reference;
						ATObject handler = scope_.meta_select(scope_, _HANDLER_);
						remote.sendLoop_.removeDisconnectionListener(handler);
					}
					return NATNil._INSTANCE_;
				}
			});
		}
		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("<disconnection subscription:"+ this.meta_select(this, _REFERENCE_)+">");
		}
	}
	
	public static class NATReconnectionSubscription extends NATObject {
		private static final AGSymbol _REFERENCE_ = AGSymbol.jAlloc("reference");
		private static final AGSymbol _HANDLER_ = AGSymbol.jAlloc("handler");
		private static final AGSymbol _CANCEL_ = AGSymbol.jAlloc("cancel");
		public NATReconnectionSubscription(final NATFarReference reference, ATClosure handler) throws InterpreterException {
			this.meta_defineField(_REFERENCE_, reference);
			this.meta_defineField(_HANDLER_, handler);
			this.meta_defineField(_CANCEL_, 	new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					NATFarReference reference = scope_.meta_select(scope_, _REFERENCE_).asNativeFarReference();
					if(reference instanceof NATRemoteFarRef) {
						NATRemoteFarRef remote = (NATRemoteFarRef)reference;
						ATObject handler = scope_.meta_select(scope_, _HANDLER_);
						remote.sendLoop_.removeReconnectionListener(handler);
					}
					return NATNil._INSTANCE_;
				}
			});
		}
		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("<reconnection subscription:"+ this.meta_select(this, _REFERENCE_)+">");
		}
	}
	
}