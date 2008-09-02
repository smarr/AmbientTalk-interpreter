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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;

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
     */
	protected ATObject lexicalParent_;
	
	protected LinkedList customFields_;
	
	/**
	 * Default constructor: creates a new call frame with a given scope pointer.
	 */
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
	
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */

	/**
	 * A field can be added to either a call frame or an object.
	 * In both cases, it is checked whether the field does not already exist.
	 * If it does not, a new field is created and its value set to the given initial value.
	 * @throws InterpreterException 
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		if (this.hasLocalField(name) || this.hasLocalMethod(name)) {
			// field already exists...
			throw new XDuplicateSlot(name);			
		} else {
			boolean fieldAdded = variableMap_.put(name);
			if (!fieldAdded) {
				throw new RuntimeException("Assertion failed: field not added to map while not duplicate");
			}
			// field now defined, add its value to the state vector
			stateVector_.add(value);
		}
		return Evaluator.getNil();
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
			return this.meta_defineField(field.base_name(), field.base_readField());
		}
		
		ATSymbol name = field.base_name();
		if (this.hasLocalField(name)) {
			// field already exists...
			throw new XDuplicateSlot(name);			
		} else {
			// add a clone of the field initialized with its new host
			field = field.meta_newInstance(NATTable.of(this)).asField();
			
			// add the field to the list of custom fields, which is created lazily
			if (customFields_ == null) {
				customFields_ = new LinkedList();
			}
			// append the custom field object
			customFields_.add(field);
		}
		return Evaluator.getNil();
	}
	
	public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
		throw new XIllegalOperation("Cannot add method "+
								   method.base_name().base_text().asNativeText().javaValue +
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
				throw new XUndefinedSlot("field grabbed", selector.toString());
			}
		}
	}

	public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException {
		throw new XSelectorNotFound(selector, this);
	}
	
	public ATObject meta_removeSlot(ATSymbol selector) throws InterpreterException {
		return this.removeLocalField(selector);
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
	
	/**
	 * Auxiliary method to dynamically select the 'super' field from this object.
	 * Note that this method is part of the base-level interface to an object.
	 * 
	 * Also note that this method performs the behaviour equivalent to evaluating
	 * 'super' and not 'self.super', which could lead to infinite loops.
	 */
	public ATObject base_super() throws InterpreterException {
		return this.impl_call(NATObject._SUPER_NAME_, NATTable.EMPTY);
	};
	
	public ATObject impl_lexicalParent() throws InterpreterException {
		return lexicalParent_;
	}

	/* --------------------------
	 * -- Conversion Protocol  --
	 * -------------------------- */
	
	public boolean isCallFrame() {
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
				if (field.base_name().equals(selector)) {
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
				if (field.base_name().equals(selector)) {
					return field;
				}
			}
			return null;
		}
	}
	
	/**
	 * Set a given field if it exists.
	 */
	protected void setLocalField(ATSymbol selector, ATObject value) throws InterpreterException {
		int index = variableMap_.get(selector);
		if(index != -1) {
			// field exists, modify the state vector
			stateVector_.set(index, value);
			// ok
		} else {
			ATField fld = getLocalCustomField(selector);
			if (fld != null) {
				fld.base_writeField(value);
				// ok
			} else {
				// fail
				throw new XSelectorNotFound(selector, this);
			}
		}
	}
	
	/**
	 * Remove a given field if it exists.
	 * @param selector the field to be removed
	 * @return the value to which the field was bound
	 * @throws XSelectorNotFound if the field could not be found
	 */
	protected ATObject removeLocalField(ATSymbol selector) throws InterpreterException {
		int index = variableMap_.remove(selector);
		if (index != -1) {
			// field exists, remove from state vector as well
			ATObject val = (ATObject) stateVector_.get(index);
			stateVector_.removeElementAt(index);
			// ok
			return val;
		} else {
			ATField fld = getLocalCustomField(selector);
			if (fld != null) {
				customFields_.remove(fld);
				// ok
				return fld.base_readField();
			} else {
				// fail
				throw new XSelectorNotFound(selector, this);
			}
		}
	}

	/**
	 * A call frame has no methods.
	 */
	protected boolean hasLocalMethod(ATSymbol atSelector) throws InterpreterException {
        return false;
	}
	
	/**
	 * A call frame has no methods.
	 */
	protected ATMethod getLocalMethod(ATSymbol selector) throws InterpreterException {
		throw new XSelectorNotFound(selector, this);
	}

	
}
