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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.mirrors.NATMirageFactory;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

/**
 * Native implementation of a default ambienttalk object.
 * 
 * Although a native AmbientTalk object is implemented as a subtype of callframes,
 * the reality is that call frames are a special kind of object.
 * 
 * This is a pure form of implementation subclassing: we subclass NATCallframe only
 * for reusing the field definition/assignment protocol and for inheriting the
 * variable map, the state vector and the lexical parent.
 * 
 * @author tvcutsem
 * @author smostinc
 */
public class NATObject extends NATCallframe implements ATObject{
	
	// Auxiliary static methods to support the type of dynamic parent
	public static final boolean _IS_A_ 		= true;
	public static final boolean _SHARES_A_ 	= false;
	
	/**
	 * This flag determines the type of parent pointer of this object. We distinguish two cases:
	 *  - 1: an is-a link, which results in a recursive cloning of the parent when this object is cloned.
	 *  - 0: a shares-a link, which ensures that clones of this object share the same parent.
	 */
	private static final byte _ISAPARENT_FLAG_ = 1<<0;
	
	/**
	 * This flag determines whether or not the field map of this object is shared by other objects:
	 *  - 1: the map is shared, so modifications must be performed on a copy
	 *  - 0: the map is not shared, modifications may be directly performed on it
	 *  
	 * This flag is important for maintaining the semantics that clones are self-sufficient objects:
	 * they share field names and methods only at the implementation-level.
	 */
	private static final byte _SHARE_MAP_FLAG_ = 1<<1;
	
	/**
	 * Similar to _SHARE_MAP_FLAG__ but for determining the shared status of the method dictionary.
	 */
	private static final byte _SHARE_DCT_FLAG_ = 1<<2;
	
	/**
	 * The flags of an AmbientTalk object encode the following boolean information:
	 *  Format: 0b00000dmp where
	 *   p = parent flag: if set, dynamic parent is 'is-a' parent, otherwise 'shares-a' parent
	 *   m = shares map flag: if set, the map of this object is shared between clones
	 *   d = shares dictionary flag: if set, the method dictionary of this object is shared between clones
	 */
	private byte flags_;
	
	// inherited from NATCallframe:
	// private FieldMap 	variableMap_;
	// private Vector	stateVector_;
	
	/**
	 * The method dictionary of this object. It maps method selectors to ATMethod objects.
	 */
	private HashMap methodDictionary_;
	
	/**
	 * The dynamic parent of this object (i.e. the delegation link).
	 * Note that the parent of an object is immutable.
	 */
	protected final ATObject dynamicParent_;
	
	/* ------------------
	 * -- Constructors --
	 * ------------------ */
	
	/**
	 * Constructs a new AmbientTalk object whose lexical parent is the
	 * global scope and whose dynamic parent is the dynamic root.
	 */
	public NATObject() {
		this(Evaluator.getGlobalLexicalScope());
	}
	
	/**
	 * Constructs a new ambienttalk object parametrised by a lexical scope. The 
	 * object is thus not equipped with a pointer to a dynamic parent.
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 */
	public NATObject(ATObject lexicalParent) {
		this(NATNil._INSTANCE_, lexicalParent, _SHARES_A_);
	}

	/**
	 * Constructs a new ambienttalk object with the given dynamic parent.
	 * The lexical parent is assumed to be the global scope.
	 * @param dynamicParent - the dynamic parent of the new object
	 * @param parentType - the type of parent link
	 */
	public NATObject(ATObject dynamicParent, boolean parentType) {
		this(dynamicParent, Evaluator.getGlobalLexicalScope(), parentType);
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
		flags_ = 0; // by default, an object has a shares-a parent and does not share its map/dictionary
		if (parentType) {
			// requested an 'is-a' parent
			setFlag(_ISAPARENT_FLAG_); // set is-a parent flag to 1
		}
	}
	
	/**
	 * Constructs a new ambienttalk object as a clone of an existing object.
	 * 
	 * The caller of this method *must* ensure that the shares flags are set.
	 */
	protected NATObject(FieldMap map,
			         Vector state,
			         HashMap methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags) {
		super(map, state, lexicalParent);
		methodDictionary_ = methodDict;
		dynamicParent_ = dynamicParent;
		flags_ = flags; //a cloned object inherits all flags from original
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */

	/*
	 * Inherited from NATCallframe
	 */
	//public ATNil meta_send(ATAsyncMessage message) throws NATException
	
	/**
	 * Invocations on an object ( <tt>o.m( args )</tt> ) are handled by looking up the requested
	 * selector in the dynamic parent chain of the receiver. This dynamic lookup process
	 * yields exactly the same result as a selection (e.g. <tt>o.m</tt>). The result
	 * ought to be a closure (a method and its corresponding evaluation context), which
	 * is applied to the provided arguments.
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		// THIS CODE IS EQUIVALENT TO THE FOLLOWING:
		//return this.meta_select(receiver, selector).asClosure().meta_apply(arguments);
		// BUT SPECIALIZED FOR PERFORMANCE REASONS (no unnecessary closure is created)
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector).asClosure().base_applyWithArgs(arguments);
		} else if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return this.getLocalMethod(selector).meta_apply(arguments, new NATContext(this, receiver, dynamicParent_));
		} else {
			return dynamicParent_.meta_invoke(receiver, selector, arguments);
		}
	}
	
	/**
	 * An ambienttalk object can respond to a message if a corresponding field or method exists
	 * either in the receiver object locally, or in one of its dynamic parents.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		if (this.hasLocalField(selector) || this.hasLocalMethod(selector))
			return NATBoolean._TRUE_;
		else
			return this.dynamicParent_.meta_respondsTo(selector);
	}


	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * meta_select is used to evaluate code of the form <tt>o.m</tt>.
	 * 
	 * To select a slot from an object:
	 *  - first, the list of fields of the current receiver ('this') is searched.
	 *    If a matching field exists, its value is returned.
	 *  - second, the list of methods of the current receiver is searched.
	 *    If a matching method exists, it is returned, but wrapped in a closure.
	 *    This wrapping is vital to ensure that the method is paired with the correct 'self'.
	 *    This 'self' does not necessarily equal 'this'.
	 *  - third, the search for the slot is carried out recursively in the dynamic parent.
	 *    As such, slot selection traverses the dynamic parent chain up to a dynamic root.
	 *    The dynamic root deals with an unbound slot by sending the 'doesNotUnderstand' message
	 *    to the original receiver.
	 *    
	 * @param receiver the original receiver of the selection
	 * @param selector the selector to look up
	 * @return the value of the found field, or a closure wrapping a found method
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector);
		} else if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return new NATClosure(this.getLocalMethod(selector), this, receiver);
		} else {
			return dynamicParent_.meta_select(receiver, selector);
		}
	} 
	
	/**
	 * This method corresponds to code of the form ( x ) within the scope of this 
	 * object. It searches for the requested selector among the methods and fields 
	 * of the object and its dynamic parents.
	 * 
	 * Overridden from NATCallframe to take methods into account as well.
	 */
	
	/**
	 * This method is used to evaluate code of the form <tt>selector</tt> within the scope
	 * of this object. An object resolves such a lookup request as follows:
	 *  - If a field corresponding to the selector exists locally, the field's value is returned.
	 *  - If a method corresponding to the selector exists locally, the method is wrapped
	 *    using the current object itself as implementor AND as 'self'.
	 *    The reason for setting the closure's 'self' to the implementor is because a lookup can only
	 *    be initiated by the object itself or a lexically nested one. Lexical nesting, however, has
	 *    nothing to do with dynamic delegation, and it would be wrong to bind 'self' to a nested object
	 *    which need not be a dynamic child of the implementor.
	 *    
	 *  - Otherwise, the search continues recursively in the object's lexical parent.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector);
		} else if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			//  ctx.super = the parent of the implementor
			return new NATClosure(this.getLocalMethod(selector), this, this);
		} else {
			return lexicalParent_.meta_lookup(selector);
		}
	}

	/**
	 * When a new field is defined in an object, it is important to check whether or not
	 * the field map is shared between clones or not. If it is shared, the map must be cloned first.
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws XDuplicateSlot, XTypeMismatch {
		if (this.isFlagSet(_SHARE_MAP_FLAG_)) {
			// copy the variable map
			variableMap_ = variableMap_.copy();
			// set the 'shares map' flag to false
			unsetFlag(_SHARE_MAP_FLAG_);
		}
		return super.meta_defineField(name, value);
	}
	
	/** 
	 * meta_assignField is used to evaluate code of the form <tt>o.m := v</tt>.
	 * 
	 * To assign a field in an object:
	 *  - first, the list of fields of the current receiver ('this') is searched.
	 *    If a matching field exists, its value is set.
	 *  - If the field is not found, the search for the slot is carried out recursively in the dynamic parent.
	 *    As such, field assignment traverses the dynamic parent chain up to a dynamic root.
	 *    The dynamic root deals with an unbound field by throwing an error.
	 *    
	 * @param selector the field to assign
	 * @param value the value to assign to the field
	 * @return NIL
	 */
	public ATNil meta_assignField(ATSymbol selector, ATObject value) throws NATException {
		if (this.setLocalField(selector, value)) {
			return NATNil._INSTANCE_;
		} else {
			return dynamicParent_.meta_assignField(selector, value);
		}
	}
	
	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	/**
	 * When cloning an object, it is first determined whether the parent
	 * has to be shared by the clone, or whether the parent must also be cloned.
	 * This depends on whether the dynamic parent is an 'is-a' parent or a 'shares-a'
	 * parent. This is determined by the _ISAPARENT_FLAG_ object flag.
	 * 
	 * A cloned object shares with its original both the variable map
	 * (to avoid having to copy space for field names) and the method dictionary
	 * (method bindings are constant and can hence be shared).
	 * 
	 * Should either the original or the clone later modify the map or the dictionary
	 * (at the meta-level), the map or dictionary will be copied first. Hence,
	 * sharing between clones is an implementation-level optimization: clones
	 * are completely self-sufficient and do not influence one another by meta-level operations.
	 */
	public ATObject meta_clone() throws NATException {
		ATObject dynamicParent;
		if(this.isFlagSet(_ISAPARENT_FLAG_)) {
			// IS-A Relation : clone the dynamic parent.
			dynamicParent = dynamicParent_.meta_clone();
		} else {
			// SHARES_A Relation : share the dynamic parent.
			dynamicParent = dynamicParent_;
		}
		
		// ! set the shares flags of this object *and* of its clone
		// both this object and the clone now share the map and method dictionary
		setFlag(_SHARE_DCT_FLAG_);
		setFlag(_SHARE_MAP_FLAG_);
		
		return createClone(variableMap_,
				          (Vector) stateVector_.clone(), // shallow copy
				          methodDictionary_,
				          dynamicParent,
				          lexicalParent_,
				          flags_);

	}
	
	/**
	 * When new is invoked on an object's mirror, the object is first cloned
	 * by the mirror, after which the method named 'init' is invoked on it.
	 * 
	 * meta_newInstance(t) = base_init(t) o meta_clone
	 * 
	 * Care should be taken that a shares-a child implements its own init method
	 * which does NOT perform a super-send. If this is not the case, then it is
	 * possible that a shared parent is accidentally re-initialized because a
	 * sharing child is cloned via new.
	 */
	public ATObject meta_newInstance(ATTable initargs) throws NATException {
		ATObject clone = this.meta_clone();
		clone.meta_invoke(clone, Evaluator._INIT_, initargs);
		return clone;
	}
	
	public ATObject meta_extend(ATClosure code) throws NATException {
		return createChild(code, _IS_A_);
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		return createChild(code, _SHARES_A_);
	}


	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */

	/**
	 * When a method is added to an object, it is first checked whether the method does not
	 * already exist. Also, care has to be taken that the method dictionary of an object
	 * does not affect clones. Therefore, if the method dictionary is shared, a copy
	 * of the dictionary is taken before adding the method.
	 */
	public ATNil meta_addMethod(ATMethod method) throws NATException {
		ATSymbol name = method.getName();
		if (methodDictionary_.containsKey(name)) {
			throw new XDuplicateSlot("method", name.getText().asNativeText().javaValue);			
		} else {
			// first check whether the method dictionary is shared
			if (this.isFlagSet(_SHARE_DCT_FLAG_)) {
				methodDictionary_ = (HashMap) methodDictionary_.clone();
				this.unsetFlag(_SHARE_DCT_FLAG_);
			}
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
		Collection methods = methodDictionary_.values();
		return new NATTable((ATObject[]) methods.toArray(new ATObject[methods.size()]));
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<object:"+this.hashCode()+">");
	}
	
	public boolean isCallFrame() {
		return false;
	}
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject getDynamicParent() {
		return dynamicParent_;
	};
	
	// protected methods, may be adapted by extensions
	
	protected NATObject createClone(FieldMap map,
	         					  Vector state,
	         					  HashMap methodDict,
	         					  ATObject dynamicParent,
	         					  ATObject lexicalParent,
	         					  byte flags) {
		return new NATObject(map,
	            state,
	            methodDict,
	            dynamicParent,
	            lexicalParent,
	            flags);
	}
	
	// private methods
	
	private boolean isFlagSet(byte flag) {
		return (flags_ & flag) != 0;
	}

	private void setFlag(byte flag) {
		flags_ = (byte) (flags_ | flag);
	}

	private void unsetFlag(byte flag) {
		flags_ = (byte) (flags_ & (~flag));
	}
	
	private boolean hasLocalMethod(ATSymbol selector) {
		return methodDictionary_.containsKey(selector);
	}
	
	private ATMethod getLocalMethod(ATSymbol selector) throws XSelectorNotFound {
		ATMethod result = (ATMethod) methodDictionary_.get(selector);
		if(result == null) {
			throw new XSelectorNotFound(selector, this);
		} else {
			return result;
		}
	}
	
	private ATObject createChild(ATClosure code, boolean parentPointerType) throws NATException {
		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.getContext().getLexicalScope(),
				/* parent porinter type */
				parentPointerType);
		
		ATAbstractGrammar body = code.getMethod().getBodyExpression();
		body.meta_eval(new NATContext(extension, extension, this));
		
		return extension;
	}

	/* ---------------------------------------
	 * -- Conversion and Testing Protocol   --
	 * --------------------------------------- */
	
	// Objects allow their methods to intercept the 'isXXX' and 'asXXX' calls
	
	public ATBegin asBegin() throws XTypeMismatch {
		try {
			return (ATBegin) meta_respondsTo(AGSymbol.alloc("asBegin")).base_ifTrue_(
				new JavaClosure(null) {
					public ATObject meta_apply(NATTable arguments) throws NATException {
						return NATMirageFactory.createMirageForInterface(
							this.meta_invoke(this, AGSymbol.alloc("asBegin"), NATTable.EMPTY),
							ATBegin.class);			
					}
			});
		} catch (NATException e) {
			return super.asBegin();
		}
	}

	public ATBoolean asBoolean() throws XTypeMismatch {
		try {
			return (ATBoolean) meta_respondsTo(AGSymbol.alloc("asBoolean")).base_ifTrue_(
				new JavaClosure(null) {
					public ATObject meta_apply(NATTable arguments) throws NATException {
						return NATMirageFactory.createMirageForInterface(
							this.meta_invoke(this, AGSymbol.alloc("asBoolean"), NATTable.EMPTY),
							ATBegin.class);			
					}
			});
		} catch (NATException e) {
			return super.asBoolean();
		}
	}

	public ATClosure asClosure() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asClosure();
	}

	public ATDefinition asDefinition() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asDefinition();
	}

	public ATExpression asExpression() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asExpression();
	}

	public ATMessage asMessage() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asMessage();
	}

	public ATMessageCreation asMessageCreation() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asMessageCreation();
	}

	public ATMethod asMethod() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asMethod();
	}

	public ATMirror asMirror() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asMirror();
	}

	public ATNumber asNumber() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asNumber();
	}

	public ATSplice asSplice() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asSplice();
	}

	public ATStatement asStatement() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asStatement();
	}

	public ATSymbol asSymbol() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asSymbol();
	}

	public ATTable asTable() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asTable();
	}

	public ATUnquoteSplice asUnquoteSplice() throws XTypeMismatch {
		// TODO Auto-generated method stub
		return super.asUnquoteSplice();
	}

	public ATBoolean base_isMirror() {
		// TODO Auto-generated method stub
		return super.base_isMirror();
	}

	public boolean isBoolean() {
		// TODO Auto-generated method stub
		return super.isBoolean();
	}

	public boolean isClosure() {
		// TODO Auto-generated method stub
		return super.isClosure();
	}

	public boolean isMethod() {
		// TODO Auto-generated method stub
		return super.isMethod();
	}

	public boolean isSplice() {
		// TODO Auto-generated method stub
		return super.isSplice();
	}

	public boolean isSymbol() {
		// TODO Auto-generated method stub
		return super.isSymbol();
	}

	public boolean isTable() {
		// TODO Auto-generated method stub
		return super.isTable();
	}

	public boolean isUnquoteSplice() {
		// TODO Auto-generated method stub
		return super.isUnquoteSplice();
	}
}
