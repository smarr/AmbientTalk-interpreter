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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * NATCallframe is a native implementation of a callframe. A callframe differs from
 * an ordinary object in the following regards:
 * - it has no dynamic parent
 * - it treats method definition as the addition of a closure to its variables.
 * - it cannot be extended nor cloned
 * 
 * Callframes can be regarded as 'field-only' objects. Fields are implemented as follows:
 *  - native fields are implemented efficiently using a 'map': the map datastructure maps
 *    selectors to indices into a state vector, such that field names can be shared efficiently
 *    across clones.
 *  - custom fields are collected in a linked list. Their lookup and assignment is slower,
 *    and when an object is cloned, the custom field objects are re-instantiated.
 *    The new clone is passed as the sole argument to 'new'.
 * 
 * @author tvcutsem
 * @author smostinc
 */
public class NATCallframe extends NATByRef implements ATObject {
	
	protected FieldMap 		variableMap_;
	protected final Vector	stateVector_;
	
    /**
     * The lexical parent 'scope' of this call frame/object.
     * A lexical scope should never travel along with an object when it is serialized,
     * hence it is declared transient. Serializable isolate objects will have to reset
     * this field upon deserialization.
     */
	protected transient ATObject lexicalParent_;
	
	protected LinkedList customFields_;
	
	public NATCallframe(ATObject lexicalParent) {
		variableMap_   = new FieldMap();
		stateVector_   = new Vector();
		lexicalParent_ = lexicalParent;
		customFields_ = null;
	}
	
	/**
	 * Used internally for cloning a callframe/object.
	 */
	protected NATCallframe(FieldMap varMap, Vector stateVector, ATObject lexicalParent, LinkedList customFields) {
		variableMap_ = varMap;
		stateVector_ = stateVector;
		lexicalParent_ = lexicalParent;
		customFields_ = customFields;
	}

	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
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
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		// assert(this == receiver)
		return this.getLocalField(selector).base_asClosure().base_apply(arguments);
	}
	
	/**
	 * respondsTo is a mechanism to ask any object o whether it would respond to the
	 * selection o.selector. A call frame implements respondsTo by checking whether
	 * it contains a public field corresponding to the selector.
	 * 
	 * A call frame does not delegate to other objects to check
	 * whether it can respond to a certain selector.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
		return NATBoolean.atValue(this.hasLocalField(selector));
	}

	/**
	 * By default, when a selection is not understood by an AmbientTalk object or call frame, an error is raised.
	 * 
	 * Warning: this method overrides its parent method which has the exact same implementation.
	 * This is done for purposes of clarity, by making NATCallframe implement all ATObject methods directly,
	 * even if NATNil already provides a suitable implementation for these.
	 */
	public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
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
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
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
	public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
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
	 * @throws InterpreterException 
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		if (this.hasLocalField(name)) {
			// field already exists...
			throw new XDuplicateSlot(XDuplicateSlot._FIELD_, name);			
		} else {
			boolean fieldAdded = variableMap_.put(name);
			if (!fieldAdded) {
				throw new RuntimeException("Assertion failed: field not added to map while not duplicate");
			}
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
	public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
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
	public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
		return this.meta_assignVariable(name, value);
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	public ATObject meta_clone() throws InterpreterException {
		throw new XIllegalOperation("Cannot clone a call frame, clone its owning object instead.");
	}

	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		throw new XIllegalOperation("Cannot create a new instance of a call frame, new its owning object instead.");
	}

	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */
	
	public ATNil meta_addField(ATField field) throws InterpreterException {
		// when adding a native field, revert to the more optimized implementation using the map
		if (field.isNativeField()) {
			return this.meta_defineField(field.base_getName(), field.base_readField());
		}
		
		ATSymbol name = field.base_getName();
		if (this.hasLocalField(name)) {
			// field already exists...
			throw new XDuplicateSlot(XDuplicateSlot._FIELD_, name);			
		} else {
			// add a clone of the field initialized with its new host
			field = field.base_new(new ATObject[] { this }).base_asField();
			
			// add the field to the list of custom fields, which is created lazily
			if (customFields_ == null) {
				customFields_ = new LinkedList();
			}
			// append the custom field object
			customFields_.add(field);
		}
		return NATNil._INSTANCE_;
	}
	
	public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
		throw new XIllegalOperation("Cannot add method "+
								   method.base_getName().base_getText().asNativeText().javaValue +
				                    " to a call frame. Add it as a closure field instead.");
	}
	
	public ATField meta_grabField(ATSymbol selector) throws InterpreterException {
		if (this.hasLocalNativeField(selector)) {
			return new NATField(selector, this);
		} else {
			ATField fld = this.getLocalCustomField(selector);
			if (fld != null) {
				return fld;
			} else {
				throw new XUndefinedField("field grabbed", selector.toString());
			}
		}
	}

	public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException {
		throw new XSelectorNotFound(selector, this);
	}

	public ATTable meta_listFields() throws InterpreterException {
		ATObject[] nativeFields = new ATObject[stateVector_.size()];
		ATSymbol[] fieldNames = variableMap_.listFields();
		// native fields first
		for (int i = 0; i < fieldNames.length; i++) {
			nativeFields[i] = new NATField(fieldNames[i], this);
		}
		if (customFields_ == null) {
			// no custom fields
			return NATTable.atValue(nativeFields);
		} else {
			ATObject[] customFields = (ATObject[]) customFields_.toArray(new ATObject[customFields_.size()]);
			return NATTable.atValue(NATTable.collate(nativeFields, customFields));
		}
	}

	public ATTable meta_listMethods() throws InterpreterException {
		return NATTable.EMPTY;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<callframe>");
	}
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject meta_getDynamicParent() throws InterpreterException {
		return meta_select(this, NATObject._SUPER_NAME_);
	};
	
	public ATObject meta_getLexicalParent() throws InterpreterException {
		return lexicalParent_;
	}

	/* --------------------------
	 * -- Conversion Protocol  --
	 * -------------------------- */
	
	public boolean base_isCallFrame() {
		return true;
	}
	
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		if(	(original instanceof NATCallframe) &
			! (original instanceof NATObject)) {
			FieldMap originalVariables = ((NATCallframe)original).variableMap_;
			
			return NATBoolean.atValue(
					variableMap_.isDerivedFrom(originalVariables));
		} else {
			return NATBoolean._FALSE_;
		}
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return super.meta_isRelatedTo(object);
	}
	
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */
	
	// protected methods, only to be used by NATCallframe and NATObject

	protected boolean hasLocalField(ATSymbol selector) throws InterpreterException {
		return hasLocalNativeField(selector) || hasLocalCustomField(selector);
	}
	
	protected boolean hasLocalNativeField(ATSymbol selector) {
		return variableMap_.get(selector) != -1;
	}
	
	protected boolean hasLocalCustomField(ATSymbol selector) throws InterpreterException {
		if (customFields_ == null) {
			return false;
		} else {
			Iterator it = customFields_.iterator();
			while (it.hasNext()) {
				ATField field = (ATField) it.next();
				if (field.base_getName().equals(selector)) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Reads out the value of either a native or a custom field.
	 * @throws XSelectorNotFound if no native or custom field with the given name exists locally.
	 */
	protected ATObject getLocalField(ATSymbol selector) throws InterpreterException {
		int index = variableMap_.get(selector);
		if(index != -1) {
			return (ATObject) (stateVector_.get(index));
		} else {
			ATField fld = getLocalCustomField(selector);
			if (fld != null) {
				return fld.base_readField();
			} else {
				throw new XSelectorNotFound(selector, this);
			}
		}
	}
	
	/**
	 * @return a custom field matching the given selector or null if such a field does not exist
	 */
	protected ATField getLocalCustomField(ATSymbol selector) throws InterpreterException {
		if (customFields_ == null) {
			return null;
		} else {
			Iterator it = customFields_.iterator();
			while (it.hasNext()) {
				ATField field = (ATField) it.next();
				if (field.base_getName().equals(selector)) {
					return field;
				}
			}
			return null;
		}
	}
	
	
	
	/**
	 * Set a given field if it exists.
	 * @return whether the field existed (and the assignment has been performed)
	 */
	protected boolean setLocalField(ATSymbol selector, ATObject value) throws InterpreterException {
		int index = variableMap_.get(selector);
		if(index != -1) {
			// field exists, modify the state vector
			stateVector_.set(index, value);
			return true;
		} else {
			ATField fld = getLocalCustomField(selector);
			if (fld != null) {
				fld.base_writeField(value);
				return true;
			} else {
				return false;
			}
		}
	}
	
}
