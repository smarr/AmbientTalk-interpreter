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
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @author smostinc
 *
 * Native implementation of a default ambienttalk object. 
 */
public class NATObject extends NATCallframe implements ATObject{

	private static final boolean _IS_A_ 		= true;
	private static final boolean _SHARES_A_ 	= false;
	
	private Map 		variableMap_ 			= new HashMap();
	private Vector	stateVector_ 			= new Vector();
	private Map		methodDictionary_  		= new HashMap();
	private boolean 	parentPointerType_ 		= _IS_A_;
	final ATObject dynamicParent_;
	
	
	/**
	 * Constructs a new ambienttalk object parametrised by a lexical scope. The 
	 * object is thus not equipped with a pointer to a dynamic parent.
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 */
	public NATObject(ATObject lexicalParent) {
		this(NATNil.instance(), lexicalParent);
	}	
	
	/**
	 * Constructs a new ambienttalk object based on a set of parent pointers. 
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent) {
		super(lexicalParent);
		dynamicParent_ = dynamicParent;
	}
	
	/**
	 * Allows converting an ordinary Object into an ATObject, wrapping it if necessary.
	 */ 
	public static ATObject cast(Object o) {
		// Our own "dynamic dispatch"
		if(o instanceof ATObject) {
			return (ATObject)o;
		} else {
			// TODO Wrapping
			throw new RuntimeException("Ordinary java objects are not wrapped yet.");			
		}
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */

	/**
	 * Asynchronous messages sent to an object ( o<-m( args )) are handled by the 
	 * actor in which the object is contained. The actor first creates a first-
	 * class message object and will subsequently send it. This method is a hook 
	 * allowing intercepting such message sends at the granularity of an object, 
	 * rather than for all objects inside an actor.
	 */
	public ATNil meta_send(ATObject sender, ATSymbol selector, ATTable arguments) throws NATException {
		// we can just reuse the basic behaviour defined in NATNil
		// TODO maybe here some events need to be fired??
		return super.meta_send(sender, selector, arguments);
	}
	
	/**
	 * Invocations on an object ( o.m( args ) ) are handled by looking up the requested
	 * selector along the dynamic parent chain of o. The select operation should yield 
	 * a closure (a method along with a proper context for evaluating its body). This 
	 * closure is then applied with the passed arguments. 
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		return meta_select(this, selector).asClosure().meta_apply(arguments);
	}
	
	/**
	 * An ambienttalk object can respond to a message if a corresponding method exists
	 * along the dynamic parent chain.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		try {
			
			meta_getMethod(selector);
			return NATBoolean.atValue(true);
			
		} catch(XSelectorNotFound exception) {
			if(dynamicParent_ != null) {
				return dynamicParent_.meta_respondsTo(selector);
			} else {
				return NATBoolean.atValue(false);
			}
		}
	}


	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * This method corresponds to code of the form ( o.m ). It searches for the 
	 * requested selector among the methods of the object and its dynamic parents.
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		try{
			ATMethod method = meta_getMethod(selector);
			ATClosure result = new NATClosure(
					/* code to be executed */
					method, 
					/* implementor - parent for lexical scoping */ 
					this,
					/* self pseudo-variable - late bound receiver of the message */
					receiver
			/* super pseudo-variable = implementor.getDynamicParent() */);
			return result;
		} catch(XSelectorNotFound exception) {
			if(dynamicParent_ != null) {
				// we forward the message and do not put in a static loop to allow
				// objects represented by other mirrors to correctly intercept all
				// calls. 
				return dynamicParent_.meta_select(receiver, selector);
			} else {
				// If no dynamic parent is available rethrow the exception.
				throw exception;
			}
		}
	} 
	
	/**
	 * This method corresponds to code of the form ( x ) within the scope of this 
	 * object. It searches for the requested selector among the methods and fields 
	 * of the object and its dynamic parents.
	 * 
	 * Overridden from NATCallframe to take methods into account as well.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		ATObject result;
		try {
			result = getField(selector);
		} catch (XSelectorNotFound exception) {
			try {
				ATMethod method = meta_getMethod(selector);
				result = new NATClosure(method, this);
			} catch (XSelectorNotFound exception2) {
				result = lexicalParent_.meta_lookup(selector);
			}
			
		}
		return result;
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
		
		NATObject clone = new NATObject(dynamicParent, lexicalParent_);
		
		// Using class-based encapsulation to initialize the clone.
		clone.parentPointerType_ = parentPointerType_;
		clone.methodDictionary_ = methodDictionary_;
		clone.stateVector_ = (Vector)stateVector_.clone();
		clone.variableMap_ = variableMap_;
		
		return clone;
	}

	private ATObject extend(ATClosure code, boolean parentPointerType) throws NATException {
		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.getContext().getLexicalScope());
		
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

	public ATNil meta_addMethod(ATMethod method) throws NATException {
		ATSymbol name = method.getName();
		if(methodDictionary_.containsKey(name)) {
			throw new XIllegalOperation("Trying to add an already existing method " + name);			
		} else {
			methodDictionary_.put(name, method);
		}
		return NATNil.instance();
	}

	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		ATMethod result = (ATMethod)methodDictionary_.get(selector);
		if(result == null) {
			throw new XSelectorNotFound(selector, this);
		} else {
			return result;
		}
	}

	public ATTable meta_listMethods() throws NATException {
		return new NATTable(methodDictionary_.values().toArray());
	}

	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject getDynamicParent() {
		return dynamicParent_;
	};
}
