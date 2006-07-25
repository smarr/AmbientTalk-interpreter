/**
 * AmbientTalk/2 Project
 * NATObject.java created on Jul 13, 2006 at 3:52:15 PM
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
import edu.vub.at.exceptions.SelectorNotFound;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATSymbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @author smostinc
 *
 * Native implementation of a default ambienttalk object. 
 */
public class NATObject extends NATNil implements ATObject{

	private static final boolean _IS_A_ 		= true;
	private static final boolean _SHARES_A_ 	= false;
	
	private Map 		variableMap_ 			= new HashMap();
	private Vector	stateVector_ 			= new Vector();
	private Map		methodDictionary_  		= new HashMap();
	private boolean 	parentPointerType_ 		= _IS_A_;
	final ATObject dynamicParent_;
	final ATObject lexicalParent_;
	
	
	/**
	 * Constructs a new ambienttalk object parametrised by a lexical scope. The 
	 * object is thus not equipped with a pointer to a dynamic parent.
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 */
	public NATObject(ATObject lexicalParent) {
		this(NATNil.instance(), lexicalParent);
	}	
	
	/**
	 * Constructs a new ambienttalk object based on a set of parent pointers
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent) {
		dynamicParent_ = dynamicParent;
		lexicalParent_ = lexicalParent;
	}
	
	public static ATObject cast(ATObject o) { return o; }
	
	public static ATObject cast(Object o) { 
		// TODO Wrapping
		throw new RuntimeException("Not just yet");
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */

	/**
	 * The default behaviour of meta_invoke for most ambienttalk language values is
	 * to check whether the requested functionality is provided in the interface 
	 * they export to the base-level. Therefore the BaseInterfaceAdaptor will try to 
	 * invoke the requested message, based on the passed selector and arguments.
	 */
	public ATObject meta_invoke(ATMessage msg) throws NATException {
		return meta_select(msg).asClosure().meta_apply(msg.getArguments());
	}

	/**
	 * An ambienttalk language value can respond to a message if this message is found
	 * in the interface it exports to the base-level.
	 */
	public ATBoolean meta_respondsTo(ATMessage msg) throws NATException {
		try {
			
			meta_getMethod(msg.getSelector());
			return NATBoolean.instance(true);
			
		} catch(SelectorNotFound exception) {
			if (msg.hasReceiver().isTrue()) {
				
				if(dynamicParent_ != null) {
					return dynamicParent_.meta_respondsTo(msg);
				} else {
					return NATBoolean.instance(false);
				}
				
			} else {
				return lexicalParent_.meta_respondsTo(msg);
			}
		}
	}


	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	public ATObject meta_select(ATMessage msg) throws NATException {
		if(msg.hasReceiver().isTrue()) {
			// Receiverfull selection 
			//  -> only consider methods
			//  -> traverse dynamic parent chain
			try{
				ATMethod method = meta_getMethod(msg.getSelector());
				ATClosure result = new NATClosure(
						/* code to be executed */
						method, 
						/* implementor - parent for lexical scoping */ 
						this,
						/* self pseudo-variable - late bound receiver of the message */
						msg.getReceiver()
						/* super pseudo-variable = implementor.getDynamicParent() */);
				return result;
			} catch(SelectorNotFound exception) {
				if(dynamicParent_ != null) {
					return dynamicParent_.meta_select(msg);
				} else {
					// If no dynamic parent is available rethrow the exception.
					throw exception;
				}
			}
		} else {
			// Receiverless selection
			// -> consider first variables, then methods
			// -> traverse lexical parent chain
			ATObject result;
			try {
				result = meta_getField(msg.getSelector());
			} catch (SelectorNotFound exception) {
				try {
					ATMethod method = meta_getMethod(msg.getSelector());
					result = new NATClosure(method, this);
				} catch (SelectorNotFound exception2) {
					result = lexicalParent_.meta_select(msg);
				}
				
			}
			return result;
		}
	}

	public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException {
		Integer index = (Integer)variableMap_.get(name);
		if(index != null) {
			stateVector_.set(index.intValue(), value);
		} else {
			// TODO discuss which parent link to follow
			// lexical parent thereby also disallowing direct assignment on dynamic parents
			// alternative, instead of a symbol take a message which allows distinction rcv+/-
			lexicalParent_.meta_assignField(name, value);
		}
		return NATNil.instance();
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	public ATObject meta_clone() throws NATException {
		ATObject dynamicParent;
		if(parentPointerType_) {
			// IS-A Relation : clone the dynamic parent.
			dynamicParent = dynamicParent_.meta_clone();
		} else {
			// SHARES_A Relation : share the parent.
			dynamicParent = dynamicParent_;
		}
		
		NATObject clone = new NATObject(dynamicParent.asClosure(), lexicalParent_);
		
		// Using class-based encapsulation to initialize the clone.
		clone.parentPointerType_ = parentPointerType_;
		clone.methodDictionary_ = methodDictionary_;
		clone.stateVector_ = (Vector)stateVector_.clone();
		clone.variableMap_ = variableMap_;

		// When copying the variable bindings (but not their values!!!) care must be
		// taken to update the implicit self pointer in the object to point to the
		// newly created clone.
		Integer selfIndex = (Integer)variableMap_.get(NATSymbol._SELF_);
		if(selfIndex == null) {
			clone.stateVector_.set(selfIndex.intValue(), clone);
		}
		

		return clone;
	}

	private ATObject extend(ATClosure code, boolean parentPointerType) {
		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.getContext().getLexicalEnvironment());
		
		// Add a self variable to every new extension
		extension.variableMap_.put(NATSymbol._SELF_, new Integer(0));
		extension.stateVector_.set(0, extension);
		
		// Adjust the parent pointer type
		extension.parentPointerType_ = parentPointerType;
		
		ATAbstractGrammar body = code.getMethod().getBody();
		body.meta_eval(new NATContext(extension, extension, this));
		
		return extension;
	}
	
	public ATObject meta_extend(ATClosure code) throws NATException {
		return extend(code, _IS_A_);
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		return extend(code, _SHARES_A_);
	}


	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */
	
	// TODO structural access protocol for objects.

	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		ATMethod result = (ATMethod)methodDictionary_.get(selector);
		if(result == null) {
			throw new SelectorNotFound(selector.toString());
		} else {
			return result;
		}
	}
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject getDynamicParent() {
		return dynamicParent_;
	};
	
	public ATObject getLexicalParent(){
		return lexicalParent_;
	}


}
