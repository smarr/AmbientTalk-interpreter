/**
 * AmbientTalk/2 Project
 * NATCallframe.java created on Jul 28, 2006 at 11:26:17 AM
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
package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

import java.util.Vector;

/**
 * NATCallframe is a native implementation of a callframe. A callframe differs from
 * an ordinary object in the following regards:
 * - it has no dynamic parent
 * - it treats method definition as the addition of a closure to its variables.
 * - it cannot be reified such that send, invoke and select are impossible
 * - it cannot be reified such that it cannot be extended or explicitly shared
 * - since a call frame its lexical parent is shared, cloning it indicates faulty behaviour  
 * 
 * @author smostinc
 */
public class NATCallframe extends NATNil implements ATObject {
	
	protected FieldMap 		variableMap_;
	protected final Vector	stateVector_;
	protected final ATObject 	lexicalParent_;
	
	public NATCallframe(ATObject lexicalParent) {
		variableMap_   = new FieldMap();
		stateVector_   = new Vector();
		lexicalParent_ = lexicalParent;
	}
	
	/**
	 * Used internally for cloning a callframe/object.
	 */
	protected NATCallframe(FieldMap varMap, Vector stateVector, ATObject lexicalParent) {
		variableMap_ = varMap;
		stateVector_ = stateVector;
		lexicalParent_ = lexicalParent;
	}

	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * Asynchronous messages sent to an object or a callframe ( o<-m( args )) are handled by the 
	 * actor in which the object is contained. This method is a hook 
	 * allowing intercepting of asynchronous message sends at the granularity of an object, 
	 * rather than at the level of an actor.
	 */
	public ATNil meta_send(ATAsyncMessage message) throws NATException {
		// assert(this == message.getReceiver());
//		 TODO Implement the ATActor interface     
//				ATActor actor = NATActor.currentActor();
//				ATAsyncMessageCreation message = actor.createMessage(msg.sender, this, msg.selector, msg.arguments);
//				actor.send(message);
		throw new RuntimeException("Not yet implemented: async message sending");
	}
	
	/**
	 * Normally, call frames are not used in receiverful method invocation expressions.
	 * That is, normally, the content of call frames is accessed via the meta_lookup operation.
	 * 
	 * A meta_invoke operation on call frames is much more ad hoc than on real objects.
	 * A call frame responds to an invocation by looking up the selector in its own fields (without delegating!)
	 * and by applying the closure bound to that field.
	 * 
	 * The 'receiver' argument should always equal 'this' because call frames do not delegate!
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		// assert(this == receiver)
		return this.getLocalField(selector).asClosure().base_apply(arguments);
	}
	
	/**
	 * respondsTo is a mechanism to ask any object o whether it would respond to the
	 * selection o.selector. A call frame implements respondsTo by checking whether
	 * it contains a public field corresponding to the selector.
	 * 
	 * A call frame does not delegate to other objects to check
	 * whether it can respond to a certain selector.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		return NATBoolean.atValue(this.hasLocalField(selector));
	}

	/**
	 * By default, when a selection is not understood by an AmbientTalk object or call frame, an error is raised.
	 * 
	 * Warning: this method overrides its parent method which has the exact same implementation.
	 * This is done for purposes of clarity, by making NATCallframe implement all ATObject methods directly,
	 * even if NATNil already provides a suitable implementation for these.
	 */
	public ATObject meta_doesNotUnderstand(ATSymbol selector) throws NATException {
		throw new XSelectorNotFound(selector, this);
	}
	
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * This method is used in the evaluation of the code <tt>o.m</tt>.
	 * When o is a call frame, the call frame is searched for a field 'm'.
	 * If it is not found, a call frame does not delegate to any dynamic parent, and yields an error.
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector);
		} else {
			throw new XSelectorNotFound(selector, this);
		}
	}
	
	/**
	 * This method is used to evaluate code of the form <tt>selector</tt> within the scope
	 * of this call frame. A call frame resolves such a lookup request by checking whether
	 * a field corresponding to the selector exists locally. If it does, the result is
	 * returned. If it does not, the search continues recursively in the call frame's
	 * lexical parent.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector);
		} else {
			return lexicalParent_.meta_lookup(selector);
		}
	}

	/**
	 * A field can be added to either a call frame or an object.
	 * In both cases, it is checked whether the field does not already exist.
	 * If it does not, a new field is created and its value set to the given initial value.
	 * @throws NATException 
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws NATException {
		boolean fieldAdded = variableMap_.put(name);
		if (!fieldAdded) {
			// field already exists...
			throw new XDuplicateSlot("field ", name.base_getText().asNativeText().javaValue);
		} else {
			// field now defined, add its value to the state vector
			stateVector_.add(value);
		}
		return NATNil._INSTANCE_;
	}
	
	/**
	 * A field can be assigned in either a call frame or an object.
	 * In both cases, if the field exists locally, it is set to the new value.
	 * If it does not exist locally, the assignment is performed on the lexical parent.
	 */
	public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws NATException {
		if (this.setLocalField(name, value)) {
			// field found and set locally
			return NATNil._INSTANCE_;
		} else {
			// The lexical parent chain is followed for assignments. This implies
			// that assignments on dynamic parents are disallowed.
			return lexicalParent_.meta_assignVariable(name, value);
		}
	}
	
	/**
	 * Assigning a call frame's field externally is possible and is treated
	 * as if it were a variable assignment. Hence, if <tt>o</tt> is a call frame,
	 * then <tt>o.m := x</tt> follows the same evaluation semantics as those of
	 * <tt>m := x</tt> when performed in the scope of <tt>o</tt>.
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws NATException {
		return this.meta_assignVariable(name, value);
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	public ATObject meta_clone() throws NATException {
		throw new XIllegalOperation("Cannot clone a call frame, clone its owning object instead.");
	}

	public ATObject meta_newInstance(ATTable initargs) throws NATException {
		throw new XIllegalOperation("Cannot create a new instance of a call frame, new its owning object instead.");
	}
	
	public ATObject meta_extend(ATClosure code) throws NATException {
		throw new XIllegalOperation("Cannot extend a call frame, extend its owning object instead.");
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		throw new XIllegalOperation("Cannot share a call frame, share its owning object instead.");
	}

	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */
	
	public ATNil meta_addField(ATField field) throws NATException {
		return this.meta_defineField(field.base_getName(), field.base_getValue());
	}
	
	public ATNil meta_addMethod(ATMethod method) throws NATException {
		throw new XIllegalOperation("Cannot add method "+
								   method.base_getName().base_getText().asNativeText().javaValue +
				                    " to a call frame. Add it as a closure field instead.");
	}
	
	public ATField meta_getField(ATSymbol selector) throws NATException {
		if (this.hasLocalField(selector)) {
			return new NATField(selector, this);
		} else {
			throw new XSelectorNotFound(selector, this);
		}
	}

	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		throw new XSelectorNotFound(selector, this);
	}

	public ATTable meta_listFields() throws NATException {
		ATObject[] fields = new ATObject[stateVector_.size()];
		ATSymbol[] fieldNames = variableMap_.listFields();
		for (int i = 0; i < fieldNames.length; i++) {
			fields[i] = new NATField(fieldNames[i], this);
		}
		return new NATTable(fields);
	}

	public ATTable meta_listMethods() throws NATException {
		return NATTable.EMPTY;
	}
	
	public NATText meta_print() throws NATException {
		return NATText.atValue("<callframe>");
	}
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject meta_getDynamicParent() throws NATException {
		return NATNil._INSTANCE_;
	};
	
	public ATObject meta_getLexicalParent() throws NATException {
		return lexicalParent_;
	}

	/* --------------------------
	 * -- Conversion Protocol  --
	 * -------------------------- */
	
	public boolean isCallFrame() {
		return true;
	}
	
	// protected methods, only to be used by NATCallframe and NATObject
	
	protected boolean hasLocalField(ATSymbol selector) {
		return variableMap_.get(selector) != -1;
	}
	
	/**
	 * This variant returns the actual value of a field not a reification of the 
	 * field itself.
	 */
	protected ATObject getLocalField(ATSymbol selector) throws XSelectorNotFound {
		int index = variableMap_.get(selector);
		if(index != -1) {
			return (ATObject) (stateVector_.get(index));
		} else {
			throw new XSelectorNotFound(selector, this);
		}
	}
	
	/**
	 * Set a given field if it exists.
	 * @return whether the field existed (and the assignment has been performed)
	 */
	protected boolean setLocalField(ATSymbol selector, ATObject value) {
		int index = variableMap_.get(selector);
		if(index != -1) {
			// field exists, modify the state vector
			stateVector_.set(index, value);
			return true;
		} else {
			return false;
		}
	}

	public ATBoolean meta_isCloneOf(ATObject original) throws NATException {
		if(	(original instanceof NATCallframe) &
			! (original instanceof NATObject)) {
			FieldMap originalVariables = ((NATCallframe)original).variableMap_;
			
			return NATBoolean.atValue(
					variableMap_.isDerivedFrom(originalVariables));
		} else {
			return NATBoolean._FALSE_;
		}
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws NATException {
		return super.meta_isRelatedTo(object);
	}
	
}
