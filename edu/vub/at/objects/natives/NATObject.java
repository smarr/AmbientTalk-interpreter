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
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

import java.util.HashMap;
import java.util.Vector;

/**
 * @author smostinc
 *
 * Native implementation of a default ambienttalk object.
 * 
 * Although a native AmbientTalk object is implemented as a subtype of callframes,
 * the reality is that call frames are a special kind of object.
 * 
 * This is a pure form of implementation subclassing: we subclass NATCallframe only
 * for reusing the field definition/assignment protocol and for inheriting the
 * variable map, the state vector and the lexical parent.
 */
public class NATObject extends NATCallframe implements ATObject{

	// Auxiliary static methods
	
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
	
	
	public static final boolean _IS_A_ 		= true;
	public static final boolean _SHARES_A_ 	= false;
	
	// inherited from NATCallframe:
	// private Map 	    variableMap_ 			= new HashMap();
	// private Vector	stateVector_ 			= new Vector();
	
	/**
	 * The method dictionary of this object. It maps method selectors to ATMethod objects.
	 */
	private final HashMap methodDictionary_;
	
	/**
	 * The type of parent pointer of this object. We distinguish two cases:
	 *  - an is-a link, which results in a recursive cloning of the parent when this object is cloned.
	 *  - a shares-a link, which ensures that clones of this object share the same parent.
	 */
	private final boolean parentPointerType_;
	
	/**
	 * The dynamic parent of this object (i.e. the delegation link).
	 * Note that the parent of an object is immutable.
	 */
	protected final ATObject dynamicParent_;
	
	/* ------------------
	 * -- Constructors --
	 * ------------------ */
	
	/**
	 * Constructs a new ambienttalk object parametrised by a lexical scope. The 
	 * object is thus not equipped with a pointer to a dynamic parent.
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 */
	public NATObject(ATObject lexicalParent) {
		this(NATNil._INSTANCE_, lexicalParent, _SHARES_A_);
	}	
	
	/**
	 * Constructs a new ambienttalk object based on a set of parent pointers. 
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent, boolean parentType) {
		super(lexicalParent);
		methodDictionary_ = new HashMap();
		dynamicParent_ = dynamicParent;
		parentPointerType_ = parentType;
	}
	
	/**
	 * Constructs a new ambienttalk object as a clone of an existing object.
	 */
	private NATObject(FieldMap map,
			         Vector state,
			         HashMap methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         boolean parentType) {
		super(map, state, lexicalParent);
		methodDictionary_ = methodDict;
		parentPointerType_ = parentType;
		dynamicParent_ = dynamicParent;
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */

	/**
	 * Asynchronous messages sent to an object ( o<-m( args )) are handled by the 
	 * actor in which the object is contained. This method is a hook 
	 * allowing intercepting of asynchronous message sends at the granularity of an object, 
	 * rather than at the level of an actor.
	 */
	public ATNil meta_send(ATMessage message) throws NATException {
		// assert(this == message.getReceiver());
//		 TODO Implement the ATActor interface     
//				ATActor actor = NATActor.currentActor();
//				ATAsyncMessageCreation message = actor.createMessage(msg.sender, this, msg.selector, msg.arguments);
//				actor.send(message);
		throw new RuntimeException("Not yet implemented: async message sending");
	}
	
	/**
	 * Invocations on an object ( o.m( args ) ) are handled by looking up the requested
	 * selector along the dynamic parent chain of o. The select operation should yield 
	 * a closure (a method paired with a proper context for evaluating its body). This 
	 * closure is then applied with the passed arguments. 
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		return meta_select(this, selector).asClosure().meta_apply(arguments);
	}
	
	/**
	 * An ambienttalk object can respond to a message if a corresponding method or field exists
	 * along the dynamic parent chain.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		try {
			meta_select(this, selector); // TODO: when events will be spawned for selection, make sure this does not trigger events...
			// if the select operation succeeds, then this object responds to the given selector
			return NATBoolean._TRUE_;
			
		} catch(XSelectorNotFound exception) {
			return NATBoolean._FALSE_;
		}
	}


	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * meta_select is used to evaluate code snippets like <tt>o.m</tt>
	 * 
	 * To select a field or method (a selector) from an object,
	 * the receiver object's fields and method dictionary are searched.
	 * If no match is found, the search is delegated to the dynamic parent.
	 * 
	 * If no match is found throughout the entire delegation chain, an XSelectorNotFound exception
	 * will be raised.
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		try {
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
				result = new NATClosure(method, this, this);
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
			// SHARES_A Relation : share the dynamic parent.
			dynamicParent = dynamicParent_;
		}
		
		return new NATObject(variableMap_,
				            (Vector) stateVector_.clone(),
				            methodDictionary_,
				            dynamicParent,
				            lexicalParent_,
				            parentPointerType_);

	}

	private ATObject extend(ATClosure code, boolean parentPointerType) throws NATException {
		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.getContext().getLexicalScope(),
				/* parent porinter type */
				parentPointerType);
		
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
		return NATNil._INSTANCE_;
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
