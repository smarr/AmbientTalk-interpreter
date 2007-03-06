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

import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.net.Logging;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.Coercer;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
 * NATObjects are one of the five native classes that fully implement the ATObject interface
 * (next to NATCallFrame, NATNil, NATMirage and NATSuperObject). The implementation is such that
 * a NATObject instance represents *both* a base-level AmbientTalk object, as well as a meta-level
 * AmbientTalk mirror on that object.
 * 
 * An AmbientTalk base-level object has the following structure:
 * - properties: a set of boolean flags denoting:
 *   - whether the dynamic parent is an IS_A or a SHARES_A parent
 *   - whether the object shares its variable map with clones
 *   - whether the object shares its method dictionary with clones
 *   - whether the object is an isolate (i.e. pass-by-copy)
 * - a variable map, mapping variable names to indices into the state vector
 * - a state vector, containing the field values of the object
 * - a linked list containing custom field objects
 * - a method dictionary, mapping selectors to methods
 * - a dynamic object parent, to delegate select and invoke operations
 *   ( this parent slot is represented by a true AmbientTalk field, rather than by an instance variable )
 * - a lexical object parent, to support lexical scoping
 * - a table of stripes that were attached to this object (for classification purposes)
 * 
 * @author tvcutsem
 * @author smostinc
 */
public class NATObject extends NATCallframe implements ATObject {
	
	// The name of the field that points to the dynamic parent
	public static final AGSymbol _SUPER_NAME_ = AGSymbol.jAlloc("super");
	
	// The names of the primitive methods
	public static final AGSymbol _EQL_NAME_ = AGSymbol.jAlloc("==");
	public static final AGSymbol _NEW_NAME_ = AGSymbol.jAlloc("new");
	public static final AGSymbol _INI_NAME_ = AGSymbol.jAlloc("init");
	
	// The primitive methods themselves
	
	/** def ==(comparand) { nil } */
	private static final PrimitiveMethod _PRIM_EQL_ = new PrimitiveMethod(
			_EQL_NAME_, NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("comparand")})) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ctx.base_getLexicalScope().base__opeql__opeql_(arguments.base_at(NATNumber.ONE));
		}
	};
	/** def new(@initargs) { nil } */
	private static final PrimitiveMethod _PRIM_NEW_ = new PrimitiveMethod(
			_NEW_NAME_, NATTable.atValue(new ATObject[] { new AGSplice(AGSymbol.jAlloc("initargs")) })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ctx.base_getLexicalScope().base_new(arguments.asNativeTable().elements_);
		}
	};
	/** def init(@initargs) { nil } */
	private static final PrimitiveMethod _PRIM_INI_ = new PrimitiveMethod(
			_INI_NAME_, NATTable.atValue(new ATObject[] { new AGSplice(AGSymbol.jAlloc("initargs")) })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ctx.base_getLexicalScope().base_init(arguments.asNativeTable().elements_);
		}
	};
	
	/**
	 * Does the selector signify a 'primitive' method, present in each AmbientTalk object?
	 */
	public static boolean isPrimitive(ATSymbol name) {
		return name.equals(_EQL_NAME_) || name.equals(_NEW_NAME_) || name.equals(_INI_NAME_);
	}
	
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
	 * This flag determines whether or not the object is an isolate and hence pass-by-copy:
	 *  - 1: the object is an isolate, pass-by-copy and no lexical parent except for the root
	 *  - 0: the object is pass-by-reference and can have any lexical parent
	 */
	private static final byte _IS_ISOLATE_FLAG_ = 1<<3;
	
	/**
	 * An empty stripe array shared by those objects that do not have any stripes.
	 */
	public static final ATStripe[] _NO_STRIPES_ = new ATStripe[0];
	
	/**
	 * The flags of an AmbientTalk object encode the following boolean information:
	 *  Format: 0b0000idmp where
	 *   p = parent flag: if set, dynamic parent is 'is-a' parent, otherwise 'shares-a' parent
	 *   m = shares map flag: if set, the map of this object is shared between clones
	 *   d = shares dictionary flag: if set, the method dictionary of this object is shared between clones
	 *   i = is isolate flag: if set, the object is passed by copy in inter-actor communication
	 */
	private byte flags_;
	
	// inherited from NATCallframe:
	// private FieldMap 	variableMap_;
	// private Vector	stateVector_;
	// private LinkedList customFields_;
	
	/**
	 * The method dictionary of this object. It maps method selectors to ATMethod objects.
	 */
	private MethodDictionary methodDictionary_;
	
	/**
	 * The dynamic parent of this object (i.e. the delegation link).
	 * Note that the parent of an object is immutable.
	 * 
	 * REPLACED by a true AmbientTalk field
	 */
	//protected final ATObject dynamicParent_;
	
	/**
	 * The stripes under which this object has been classified
	 */
	protected ATStripe[] stripes_;
	
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
	 * Creates an object striped with the at.stripes.Isolate stripe.
	 * Such an object is called an isolate because:
	 *  - it has no access to an enclosing lexical scope (except for the root lexical scope)
	 *  - it can therefore be passed by copy
	 */
	public static NATObject createIsolate() {
		return new NATObject(new ATStripe[] { NativeStripes._ISOLATE_ });
	}
	
	public NATObject(ATStripe[] stripes) {
		this(Evaluator.getGlobalLexicalScope(), stripes);
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
	 * Constructs a new ambienttalk object parametrised by a lexical scope.
	 * The object's dynamic parent is nil and is striped with the given table of stripes
	 */
	public NATObject(ATObject lexicalParent, ATStripe[] stripes) {
		this(NATNil._INSTANCE_, lexicalParent, _SHARES_A_, stripes);
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
	 * The object has no stripes.
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent, boolean parentType) {
	   this(dynamicParent, lexicalParent, parentType, _NO_STRIPES_);
	}
	
	/**
	 * Constructs a new ambienttalk object based on a set of parent pointers.
	 * The object is striped with the given stripes.
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 * @param stripes - the stripes attached to this object
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent, boolean parentType, ATStripe[] stripes) {
		super(lexicalParent);
		
        // by default, an object has a shares-a parent, does not share its map
		// or dictionary and is no isolate, so all flags are set to 0
		flags_ = 0;
		
		stripes_ = stripes;
		
		// if this object is striped as at.stripes.Isolate, flag it as an isolate
		try {
			if (isLocallyStripedWith(NativeStripes._ISOLATE_)) {
				setFlag(_IS_ISOLATE_FLAG_);
				// isolates can only have the global lexical root as their lexical scope
				lexicalParent_ = Evaluator.getGlobalLexicalScope();
			}
		} catch (InterpreterException e) {
			// some custom stripe failed to match agains the Isolate stripe,
			// the object is not considered an Isolate
			Logging.Actor_LOG.error("Error testing for Isolate stripe, ignored:", e);
		}
		
		methodDictionary_ = new MethodDictionary();
		
		// bind the dynamic parent to the field named 'super'
		// we don't pass via meta_defineField as this would trigger mirages too early
		variableMap_.put(_SUPER_NAME_);
		stateVector_.add(dynamicParent);
			
		// add ==, new and init to the method dictionary directly
		// we don't pass via meta_addMethod as this would trigger mirages too early
		methodDictionary_.put(_EQL_NAME_, _PRIM_EQL_);
		methodDictionary_.put(_NEW_NAME_, _PRIM_NEW_);
		methodDictionary_.put(_INI_NAME_, _PRIM_INI_);
		
		if (parentType) {
			// requested an 'is-a' parent
			setFlag(_ISAPARENT_FLAG_); // set is-a parent flag to 1
		}
	}
	
	/**
	 * Constructs a new ambienttalk object as a clone of an existing object.
	 * 
	 * The caller of this method *must* ensure that the shares flags are set.
	 * 
	 * This constructor is responsible for manually re-initialising any custom field
	 * objects, because the init method of such custom fields is parameterized by the
	 * clone, which only comes into existence when this constructor runs.
	 */
	protected NATObject(FieldMap map,
			         Vector state,
			         LinkedList originalCustomFields,
			         MethodDictionary methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags,
			         ATStripe[] stripes) throws InterpreterException {
		super(map, state, lexicalParent, null);
		methodDictionary_ = methodDict;
		
		flags_ = flags; //a cloned object inherits all flags from original
		
		// clone inherits all stripes (this implies that clones of isolates are also isolates)
		stripes_ = stripes;
		
		// ==, new and init should already be present in the method dictionary
		
		// set the 'super' field to point to the new dynamic parent
		setLocalField(_SUPER_NAME_, dynamicParent);
		
		// re-initialize all custom fields
		if (originalCustomFields != null) {
			customFields_ = new LinkedList();
			Iterator it = originalCustomFields.iterator();
			while (it.hasNext()) {
				ATField field = (ATField) it.next();
				customFields_.add(field.base_new(new ATObject[] { this }).base_asField());
			}
		}
		
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * Invocations on an object ( <tt>o.m( args )</tt> ) are handled by looking up the requested
	 * selector in the dynamic parent chain of the receiver. This dynamic lookup process
	 * yields exactly the same result as a selection (e.g. <tt>o.m</tt>). The result
	 * ought to be a closure (a method and its corresponding evaluation context), which
	 * is applied to the provided arguments.
	 * 
	 * The code for meta_invoke is actually equivalent to
	 * <code>return this.meta_select(receiver, selector).asClosure().meta_apply(arguments);</code>
	 * but has a specialized implementation for performance reasons (no unnecessary closure is created)
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector).base_asClosure().base_apply(arguments);
		} else if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, receiver));
		} else {
			return meta_getDynamicParent().meta_invoke(receiver, selector, arguments);
		}
	}
	
	/**
	 * An ambienttalk object can respond to a message if a corresponding field or method exists
	 * either in the receiver object locally, or in one of its dynamic parents.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
		if (this.hasLocalField(selector) || this.hasLocalMethod(selector))
			return NATBoolean._TRUE_;
		else
			return meta_getDynamicParent().meta_respondsTo(selector);
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
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
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
			return meta_getDynamicParent().meta_select(receiver, selector);
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
	public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
		if (this.hasLocalField(selector)) {
			return this.getLocalField(selector);
		} else if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return new NATClosure(this.getLocalMethod(selector),this, this);
		} else {
			return lexicalParent_.meta_lookup(selector);
		}
	}
	
	/**
	 * When a new field is defined in an object, it is important to check whether or not
	 * the field map is shared between clones or not. If it is shared, the map must be cloned first.
	 * @throws InterpreterException 
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
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
	 * @param value the value to assign to the field
	 * @param selector the field to assign
	 *    
	 * @return NIL
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol selector, ATObject value) throws InterpreterException {
		if (this.setLocalField(selector, value)) {
			return NATNil._INSTANCE_;
		} else {
			return meta_getDynamicParent().meta_assignField(receiver, selector, value);
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
	public ATObject meta_clone() throws InterpreterException {
		ATObject dynamicParent;
		if(this.isFlagSet(_ISAPARENT_FLAG_)) {
			// IS-A Relation : clone the dynamic parent.
			dynamicParent = meta_getDynamicParent().meta_clone();
		} else {
			// SHARES_A Relation : share the dynamic parent.
			dynamicParent = meta_getDynamicParent();
		}
		
		// ! set the shares flags of this object *and* of its clone
		// both this object and the clone now share the map and method dictionary
		setFlag(_SHARE_DCT_FLAG_);
		setFlag(_SHARE_MAP_FLAG_);
		
		return createClone(variableMap_,
				          (Vector) stateVector_.clone(), // shallow copy
				          customFields_, // must be re-initialized by clone!
				          methodDictionary_,
				          dynamicParent,
				          lexicalParent_,
				          flags_, stripes_);

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
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		ATObject clone = this.meta_clone();
		clone.meta_invoke(clone, Evaluator._INIT_, initargs);
		return clone;
	}
	
	public ATObject meta_extend(ATClosure code) throws InterpreterException {
		return createChild(code, _IS_A_);
	}

	public ATObject meta_share(ATClosure code) throws InterpreterException {
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
	 * 
	 * One exception to method addition are primitive methods: if the method added
	 * would conflict with a primitive method, the primitive is replaced by the new
	 * method instead.
	 */
	public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
		ATSymbol name = method.base_getName();
		if (methodDictionary_.containsKey(name) && !isPrimitive(name)) {
			throw new XDuplicateSlot(XDuplicateSlot._METHOD_, name);
		} else {
			// first check whether the method dictionary is shared
			if (this.isFlagSet(_SHARE_DCT_FLAG_)) {
				methodDictionary_ = (MethodDictionary) methodDictionary_.clone();
				this.unsetFlag(_SHARE_DCT_FLAG_);
			}
			methodDictionary_.put(name, method);
		}
		return NATNil._INSTANCE_;
	}

	public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException {
		ATMethod result = (ATMethod)methodDictionary_.get(selector);
		if(result == null) {
			throw new XSelectorNotFound(selector, this);
		} else {
			return result;
		}
	}

	public ATTable meta_listMethods() throws InterpreterException {
		Collection methods = methodDictionary_.values();
		return NATTable.atValue((ATObject[]) methods.toArray(new ATObject[methods.size()]));
	}
	
	public NATText meta_print() throws InterpreterException {
		if (stripes_ == _NO_STRIPES_) {
			return NATText.atValue("<object:"+this.hashCode()+">");
		} else {
			return NATText.atValue("<object:"+this.hashCode()+
					               Evaluator.printElements(stripes_, "[", ",", "]").javaValue+">");
		}
	}
	
	public boolean base_isCallFrame() {
		return false;
	}
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	// protected methods, may be adapted by extensions
	
	protected NATObject createClone(FieldMap map,
	         					  Vector state,
	         					  LinkedList originalCustomFields,
	         					  MethodDictionary methodDict,
	         					  ATObject dynamicParent,
	         					  ATObject lexicalParent,
	         					  byte flags,
	         					  ATStripe[] stripes) throws InterpreterException {
		return new NATObject(map,
	            state,
	            originalCustomFields,
	            methodDict,
	            dynamicParent,
	            lexicalParent,
	            flags,
	            stripes);
	}
	
	/**
	 * When creating children of objects, care must be taken that an extension
	 * of an isolate itself remains an isolate.
	 */
	protected ATObject createChild(ATClosure code, boolean parentPointerType) throws InterpreterException {
		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent -> isolates don't inherit outer scope! */
				isFlagSet(_IS_ISOLATE_FLAG_) ?
						Evaluator.getGlobalLexicalScope() :
						code.base_getContext().base_getLexicalScope(),
				/* parent pointer type */
				parentPointerType);
		
		NATTable copiedBindings = Evaluator.evalMandatoryPars(
				code.base_getMethod().base_getParameters(),
				code.base_getContext());
		code.base_applyInScope(copiedBindings, extension);
		return extension;
	}
	
    /* ----------------------------------
     * -- Object Relational Comparison --
     * ---------------------------------- */
    
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		if(original instanceof NATObject) {
			MethodDictionary originalMethods = ((NATObject)original).methodDictionary_;
			FieldMap originalVariables = ((NATObject)original).variableMap_;
			
			return NATBoolean.atValue(
					methodDictionary_.isDerivedFrom(originalMethods) &
					variableMap_.isDerivedFrom(originalVariables));
		} else {
			return NATBoolean._FALSE_;
		}
	}

	public ATBoolean meta_isRelatedTo(final ATObject object) throws InterpreterException {
		return this.meta_isCloneOf(object).base_or_(
				new NativeClosure(this) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						return scope_.meta_getDynamicParent().meta_isRelatedTo(object);
					}
				}).base_asBoolean();
	}
	
    /* ---------------------------------
     * -- Stripe Testing and Querying --
     * --------------------------------- */
	
    /**
     * Check whether one of the stripes of this object is a substripe of the given stripe.
     * If not, then delegate the query to the dynamic parent.
     */
    public ATBoolean meta_isStripedWith(ATStripe stripe) throws InterpreterException {
    	for (int i = 0; i < stripes_.length; i++) {
			if (stripes_[i].base_isSubstripeOf(stripe).asNativeBoolean().javaValue) {
				// if one stripe matches, return true
				return NATBoolean._TRUE_;
			}
		}
    	// no stripes match, ask the parent
    	return meta_getDynamicParent().meta_isStripedWith(stripe);
    }
    
    /**
     * Return the stripes that were directly attached to this object.
     */
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.atValue(stripes_);
    }
    
    
	// NATObject has to duplicate the NATByCopy implementation
	// because NATObject inherits from NATByRef, and because Java has no
	// multiple inheritance to override that implementation with that of
    // NATByCopy if this object signifies an isolate.
	
    /**
     * An isolate object does not return a proxy representation of itself
     * during serialization, hence it is serialized itself. If the object
     * is not an isolate, invoke the default behaviour for by-reference objects
     */
    public ATObject meta_pass() throws InterpreterException {
    	if (isFlagSet(_IS_ISOLATE_FLAG_)) {
    		return this;
    	} else {
    		return super.meta_pass();
    	}
    }
	
    /**
     * An isolate object represents itself upon deserialization.
     * If this object is not an isolate, the default behaviour for by-reference
     * objects is invoked.
     */
    public ATObject meta_resolve() throws InterpreterException {
    	if (isFlagSet(_IS_ISOLATE_FLAG_)) {
    		// re-bind to the new local global lexical root
    		lexicalParent_ = Evaluator.getGlobalLexicalScope();
    		return this;
    	} else {
    		return super.meta_resolve();
    	}
    }

	/* ---------------------------------------
	 * -- Conversion and Testing Protocol   --
	 * --------------------------------------- */
	
	public NATObject asAmbientTalkObject() { return this; }
	
	/**
	 * ALL asXXX methods return a coercer object which returns a proxy of the correct interface that will 'down'
	 * subsequent Java base-level invocations to the AmbientTalk level.
	 * 
	 * Coercion only happens if the object is striped with the correct stripe.
	 */
	private Object coerce(ATStripe requiredStripe, Class providedInterface) throws InterpreterException {
		if (this.meta_isStripedWith(requiredStripe).asNativeBoolean().javaValue) {
			return Coercer.coerce(this, providedInterface);
		} else {
			// if the object does not possess the right stripe, raise a type error
			throw new XTypeMismatch(providedInterface, this);
		}
	}
	
	public ATBoolean base_asBoolean() throws InterpreterException { return (ATBoolean) coerce(NativeStripes._BOOLEAN_, ATBoolean.class); }
	public ATClosure base_asClosure() throws InterpreterException { return (ATClosure) coerce(NativeStripes._CLOSURE_, ATClosure.class); }
	public ATExpression base_asExpression() throws InterpreterException { return (ATExpression) coerce(NativeStripes._EXPRESSION_, ATExpression.class); }
	public ATField base_asField() throws InterpreterException { return (ATField) coerce(NativeStripes._FIELD_, ATField.class); }
	public ATMessage base_asMessage() throws InterpreterException { return (ATMessage) coerce(NativeStripes._MESSAGE_, ATMessage.class); }
	public ATMethod base_asMethod() throws InterpreterException { return (ATMethod) coerce(NativeStripes._METHOD_, ATMethod.class); }
	public ATMirror base_asMirror() throws InterpreterException { return (ATMirror) coerce(NativeStripes._MIRROR_, ATMirror.class); }
	public ATHandler base_asHandler() throws InterpreterException { return (ATHandler) coerce(NativeStripes._HANDLER_, ATHandler.class); }
	public ATNumber base_asNumber() throws InterpreterException { return (ATNumber) coerce(NativeStripes._NUMBER_, ATNumber.class); }
	public ATTable base_asTable() throws InterpreterException { return (ATTable) coerce(NativeStripes._TABLE_, ATTable.class); }
    public ATAsyncMessage base_asAsyncMessage() throws InterpreterException { return (ATAsyncMessage) coerce(NativeStripes._ASYNCMSG_, ATAsyncMessage.class);}
    public ATActorMirror base_asActorMirror() throws InterpreterException { return (ATActorMirror) coerce(NativeStripes._ACTORMIRROR_, ATActorMirror.class); }
    public ATStripe base_asStripe() throws InterpreterException { return (ATStripe) coerce(NativeStripes._STRIPE_, ATStripe.class); }
	
	public ATBegin base_asBegin() throws InterpreterException { return (ATBegin) coerce(NativeStripes._BEGIN_, ATBegin.class); }
	public ATStatement base_asStatement() throws InterpreterException { return (ATStatement) coerce(NativeStripes._STATEMENT_, ATStatement.class); }
    public ATUnquoteSplice base_asUnquoteSplice() throws InterpreterException { return (ATUnquoteSplice) coerce(NativeStripes._UQSPLICE_, ATUnquoteSplice.class); }
    public ATSymbol base_asSymbol() throws InterpreterException { return (ATSymbol) coerce(NativeStripes._SYMBOL_, ATSymbol.class); }
    public ATSplice base_asSplice() throws InterpreterException { return (ATSplice) coerce(NativeStripes._SPLICE_, ATSplice.class); }
	public ATDefinition base_asDefinition() throws InterpreterException { return (ATDefinition) coerce(NativeStripes._DEFINITION_, ATDefinition.class); }
	public ATMessageCreation base_asMessageCreation() throws InterpreterException { return (ATMessageCreation) coerce(NativeStripes._MSGCREATION_, ATMessageCreation.class); }
    
	// ALL isXXX methods return true (can be overridden by programmer-defined base-level methods)
	
	public boolean isAmbientTalkObject() { return true; }
	
	// objects can only be 'cast' to a native category if they are marked with
	// the appropriate native stripe
	public boolean base_isMirror() throws InterpreterException { return meta_isStripedWith(NativeStripes._MIRROR_).asNativeBoolean().javaValue; }
	public boolean base_isBoolean() throws InterpreterException { return meta_isStripedWith(NativeStripes._BOOLEAN_).asNativeBoolean().javaValue; }
	public boolean base_isClosure() throws InterpreterException { return meta_isStripedWith(NativeStripes._CLOSURE_).asNativeBoolean().javaValue; }
	public boolean base_isMethod() throws InterpreterException { return meta_isStripedWith(NativeStripes._METHOD_).asNativeBoolean().javaValue; }
	public boolean base_isSplice() throws InterpreterException { return meta_isStripedWith(NativeStripes._SPLICE_).asNativeBoolean().javaValue; }
	public boolean base_isSymbol() throws InterpreterException { return meta_isStripedWith(NativeStripes._SYMBOL_).asNativeBoolean().javaValue; }
	public boolean base_isTable() throws InterpreterException { return meta_isStripedWith(NativeStripes._TABLE_).asNativeBoolean().javaValue; }
	public boolean base_isUnquoteSplice() throws InterpreterException { return meta_isStripedWith(NativeStripes._UQSPLICE_).asNativeBoolean().javaValue; }
	public boolean base_isStripe() throws InterpreterException { return meta_isStripedWith(NativeStripes._STRIPE_).asNativeBoolean().javaValue; }
	
	
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
	
	private ATMethod getLocalMethod(ATSymbol selector) throws InterpreterException {
		ATMethod result = ((ATObject) methodDictionary_.get(selector)).base_asMethod();
		if(result == null) {
			throw new XSelectorNotFound(selector, this);
		} else {
			return result;
		}
	}
	
	/**
	 * Performs a stripe test for this object locally.
	 * @return whether this object is striped with a particular stripe or not.
	 */
	private boolean isLocallyStripedWith(ATStripe stripe) throws InterpreterException {
    	for (int i = 0; i < stripes_.length; i++) {
			if (stripes_[i].base_isSubstripeOf(stripe).asNativeBoolean().javaValue) {
				// if one stripe matches, return true
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Auxiliary method to access the fields of an object and all of its super-objects up to (but excluding) nil.
	 * Overridden fields of parent objects are not included.
	 */
	public static ATField[] listTransitiveFields(ATObject obj) throws InterpreterException {
		Vector fields = new Vector();
		HashSet encounteredNames = new HashSet(); // to filter duplicates
		for (; obj != NATNil._INSTANCE_ ; obj = obj.meta_getDynamicParent()) {
			ATObject[] localFields = obj.meta_listFields().asNativeTable().elements_;
			for (int i = 0; i < localFields.length; i++) {
				ATField field = localFields[i].base_asField();
				ATSymbol fieldName = field.base_getName();
				if (!encounteredNames.contains(fieldName)) {
					fields.add(field);
					encounteredNames.add(fieldName);
				}
			}
		}
		return (ATField[]) fields.toArray(new ATField[fields.size()]);
	}
	
	/**
	 * Auxiliary method to access the methods of an object and all of its super-objects up to (but excluding) nil.
	 * Overridden methods of parent objects are not included.
	 */
	public static ATMethod[] listTransitiveMethods(ATObject obj) throws InterpreterException {
		Vector methods = new Vector();
		HashSet encounteredNames = new HashSet(); // to filter duplicates		
		for (; obj != NATNil._INSTANCE_ ; obj = obj.meta_getDynamicParent()) {
			// fast-path for native objects
			if (obj instanceof NATObject) {
				Collection localMethods = ((NATObject) obj).methodDictionary_.values();
				for (Iterator iter = localMethods.iterator(); iter.hasNext();) {
					ATMethod localMethod = (ATMethod) iter.next();
					ATSymbol methodName = localMethod.base_getName();
					if (!encounteredNames.contains(methodName)) {
						methods.add(localMethod);
						encounteredNames.add(methodName);
					}
				}
			} else {
				ATObject[] localMethods = obj.meta_listMethods().asNativeTable().elements_;
				for (int i = 0; i < localMethods.length; i++) {
					ATMethod localMethod = localMethods[i].base_asMethod();
					ATSymbol methodName = localMethod.base_getName();
					if (!encounteredNames.contains(methodName)) {
						methods.add(localMethod);
						encounteredNames.add(methodName);
					}
				}
			}
		}
		return (ATMethod[]) methods.toArray(new ATMethod[methods.size()]);
	}
	
}
