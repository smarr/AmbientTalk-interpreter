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
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
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
 * @author smostinc
 *
 * NATCallframe is a native implementation of a callframe. A callframe differs from
 * an ordinary object in the following regards:
 * - it has no dynamic parent
 * - it treats method definition as the addition of a closure to its variables.
 * - it cannot be reified such that send, invoke and select are impossible
 * - it cannot be reified such that it cannot be extended or explicitly shared
 * - since lexical parents are shared, cloning it indicates faulty behaviour  
 */
public class NATCallframe extends NATNil implements ATObject {
	
	protected final FieldMap 	variableMap_;
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
	 * Invocations on an object ( o.m( args ) ) are not allowed on callframes since
	 * they cannot be reified in the language (other than as lexical parents at the
	 * meta-level).
	 * 
	 * @throws XIllegalOperation
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		throw new XIllegalOperation("Call frames cannot be reified in the language " +
		"and therefore no methods can be invoked on them.");
	}
	
	/**
	 * respondsTo is a mechanism to ask any object o whether it would respond to the
	 * method o.selector( args ). Since callframes cannot be reified in the language
	 * using base-level mechanisms (they can be visible as lexical parents to the 
	 * meta-level), callframes are said not to respond to any method invocation.
	 * 
	 * @throws XIllegalOperation
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		throw new XIllegalOperation("Call frames cannot be reified in the language " +
		"and therefore it cannot respond to any method invocation.");
	}
	
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * This method corresponds to code of the form ( o.m ). As callframes can not
	 * be seen as first-class objects, the cannot be the target of this type of 
	 * expressions. 
	 * 
	 * @throws XIllegalOperation
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		throw new XIllegalOperation("Call frames cannot be reified in the language " +
		"and therefore no fields or methods can be selected from them.");
	} 
	
	/**
	 * This method corresponds to code of the form ( x ) within the scope of this 
	 * object. It searches for the requested selector among the methods and fields 
	 * of the object and its dynamic parents.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		ATObject result;
		try {
			result = getField(selector);
		} catch (XSelectorNotFound exception2) {
			result = lexicalParent_.meta_lookup(selector);
		}
		return result;
	}

	public ATNil meta_defineField(ATSymbol name, ATObject value) throws NATException {
		boolean fieldAdded = variableMap_.put(name);
		if (!fieldAdded) {
			// field already exists...
			throw new XIllegalOperation("Definition of duplicate field " + name);
		} else {
			// field now defined, add it to the state vector
			stateVector_.add(value);
		}
		return NATNil._INSTANCE_;
	}
	
	public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException {
		int index = variableMap_.get(name);
		if(index != -1) {
			stateVector_.set(index, value);
		} else {
			// The lexical parent chain is followed for assignments. This implies
			// that assignments on dynamic parents are disallowed.
			lexicalParent_.meta_assignField(name, value);
		}
		return NATNil._INSTANCE_;
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	public ATObject meta_clone() throws NATException {
		throw new XIllegalOperation("Call frames should not be cloned as they " +
				"may only be part of an object hierarchy through the lexical " +
				"parent, which is never cloned.");
	}

	public ATObject meta_extend(ATClosure code) throws NATException {
		throw new XIllegalOperation("Call frames cannot be reified in the language " +
				"and therefore cannot be extended.");
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		throw new XIllegalOperation("Call frames cannot be reified in the language " +
				"and therefore cannot be shared as a dynamic parent.");
	}

	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */
	
	public ATNil meta_addField(ATField field) throws NATException {
		return meta_defineField(field.getName(), field.getValue());
	}
	
	public ATNil meta_addMethod(ATMethod method) throws NATException {
		throw new XIllegalOperation("Adding a method to a callframe is not possible " +
				"since the parser will instead add a closure as a field.");
	}

	/**
	 * This variant returns the actual value of a field not a reification of the 
	 * field itself.
	 */
	public ATObject getField(ATSymbol selector) throws NATException {
		int index = variableMap_.get(selector);
		if(index != -1) {
			return (ATObject) (stateVector_.get(index));
		} else {
			throw new XSelectorNotFound(selector, this);
		}
	}
	
	public ATField meta_getField(ATSymbol selector) throws NATException {
		int index = variableMap_.get(selector);
		if(index != -1) {
			return new NATField(selector, this);
		} else {
			throw new XSelectorNotFound(selector, this);
		}
	}

	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		// TODO should this not always return nil?
		try {
			return getField(selector).asClosure().getMethod();
		} catch (XTypeMismatch e) {
			throw new XSelectorNotFound(selector, this);
		}
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
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject getDynamicParent() {
		return NATNil._INSTANCE_;
	};
	
	public ATObject getLexicalParent() {
		return lexicalParent_;
	}

	/* --------------------------
	 * -- Conversion Protocol  --
	 * -------------------------- */
	
	public boolean isCallFrame() {
		return true;
	}
	
}
