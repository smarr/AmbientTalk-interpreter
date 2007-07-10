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
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.Coercer;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGAssignmentSymbol;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.util.logging.Logging;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import sun.tools.tree.ThisExpression;

/**
 * Native implementation of a default ambienttalk object.
 * Although a native AmbientTalk object is implemented as a subtype of callframes,
 * the reality is that call frames are a special kind of object.
 * This is a pure form of implementation subclassing: we subclass NATCallframe only
 * for reusing the field definition/assignment protocol and for inheriting the
 * variable map, the state vector and the lexical parent.
 * <p>
 * NATObjects are one of the five native classes that (almost) fully implement the ATObject interface
 * (next to NATCallFrame, NATNil, NATMirage and JavaObject). The implementation is such that
 * a NATObject instance represents <b>both</b> a base-level AmbientTalk object, as well as a meta-level
 * AmbientTalk mirror on that object.
 * 
 * An AmbientTalk base-level object has the following structure:
 * <ul>
 *  <li> properties: a set of boolean flags denoting:
 *  <ul>
 *   <li> whether the dynamic parent is an IS_A or a SHARES_A parent
 *   <li> whether the object shares its variable map with clones
 *   <li> whether the object shares its method dictionary with clones
 *   <li> whether the object is an isolate (i.e. pass-by-copy)
 *  </ul>
 *  <li> a variable map, mapping variable names to indices into the state vector
 *  <li> a state vector, containing the field values of the object
 *  <li> a linked list containing custom field objects
 *  <li> a method dictionary, mapping selectors to methods
 *  <li> a dynamic object parent, to delegate select and invoke operations
 *   ( this parent slot is represented by a true AmbientTalk field, rather than by an instance variable )
 *  <li> a lexical object parent, to support lexical scoping
 *  <li> a table of type tags that were attached to this object (for classification purposes)
 * </ul>
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
			if (!arguments.base_length().equals(NATNumber.ONE)) {
				throw new XArityMismatch("==", 1, arguments.base_length().asNativeNumber().javaValue);
			}
			// primitive implementation uses pointer equality (as dictated by NATNil)
			return NATBoolean.atValue(ctx.base_lexicalScope() == arguments.base_at(NATNumber.ONE));
		}
	};
	/** def new(@initargs) { nil } */
	private static final PrimitiveMethod _PRIM_NEW_ = new PrimitiveMethod(
			_NEW_NAME_, NATTable.atValue(new ATObject[] { new AGSplice(AGSymbol.jAlloc("initargs")) })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ctx.base_lexicalScope().base_new(arguments.asNativeTable().elements_);
		}
	};
	/** def init(@initargs) { nil } */
	private static final PrimitiveMethod _PRIM_INI_ = new PrimitiveMethod(
			_INI_NAME_, NATTable.atValue(new ATObject[] { new AGSplice(AGSymbol.jAlloc("initargs")) })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ctx.base_lexicalScope().asAmbientTalkObject().prim_init(ctx.base_self(), arguments.asNativeTable().elements_);
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
	 * An empty type tag array shared by those objects that do not have any type tags.
	 */
	public static final ATTypeTag[] _NO_TYPETAGS_ = new ATTypeTag[0];
	
	/**
	 * The flags of an AmbientTalk object encode the following boolean information:
	 *  Format: 0b0000idap where
	 *   p = parent flag: if set, dynamic parent is 'is-a' parent, otherwise 'shares-a' parent
	 *   a = shares map flag: if set, the map of this object is shared between clones
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
	 * The types with which this object has been tagged.
	 */
	protected ATTypeTag[] typeTags_;
	
	/* ------------------
	 * -- Constructors --
	 * ------------------ */
	
	/**
	 * Creates an object tagged with the at.types.Isolate type.
	 * Such an object is called an isolate because:
	 *  - it has no access to an enclosing lexical scope (except for the root lexical scope)
	 *  - it can therefore be passed by copy
	 */
	public static NATObject createIsolate() {
		return new NATObject(new ATTypeTag[] { NativeTypeTags._ISOLATE_ });
	}
	
	/**
	 * Constructs a new AmbientTalk object whose lexical parent is the
	 * global scope and whose dynamic parent is the dynamic root.
	 */
	public NATObject() {
		this(Evaluator.getGlobalLexicalScope());
	}
	
	/**
	 * Construct a new AmbientTalk object directly tagged with the given type tags.
	 */
	public NATObject(ATTypeTag[] tags) {
		this(Evaluator.getGlobalLexicalScope(), tags);
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
	 * The object's dynamic parent is nil and is tagged with the given table of type tags
	 */
	public NATObject(ATObject lexicalParent, ATTypeTag[] tags) {
		this(NATNil._INSTANCE_, lexicalParent, _SHARES_A_, tags);
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
	 * The object has no types.
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent, boolean parentType) {
	   this(dynamicParent, lexicalParent, parentType, _NO_TYPETAGS_);
	}
	
	/**
	 * Constructs a new ambienttalk object based on a set of parent pointers.
	 * The object is typed with the given types.
	 * @param dynamicParent - the parent object of the newly created object
	 * @param lexicalParent - the lexical scope in which the object's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 * @param tags - the type tags attached to this object
	 */
	public NATObject(ATObject dynamicParent, ATObject lexicalParent, boolean parentType, ATTypeTag[] tags) {
		super(lexicalParent);
		
        // by default, an object has a shares-a parent, does not share its map
		// or dictionary and is no isolate, so all flags are set to 0
		flags_ = 0;
		
		typeTags_ = tags;
		
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
		
		if (parentType) { // parentType == _IS_A_)
			// requested an 'is-a' parent
			setFlag(_ISAPARENT_FLAG_); // set is-a parent flag to 1
		}
		
		try {
            // if this object is tagged as at.types.Isolate, flag it as an isolate
            // we cannot perform 'this.meta_isTypedAs(ISOLATE)' because this would trigger mirages too early
			if (isLocallyTaggedAs(NativeTypeTags._ISOLATE_)
			     || dynamicParent.meta_isTaggedAs(NativeTypeTags._ISOLATE_).asNativeBoolean().javaValue) {
				setFlag(_IS_ISOLATE_FLAG_);
				// isolates can only have the global lexical root as their lexical scope
				lexicalParent_ = Evaluator.getGlobalLexicalScope();
			}
		} catch (InterpreterException e) {
			// some custom type failed to match agains the Isolate type,
			// the object is not considered an Isolate
			Logging.Actor_LOG.error("Error testing for Isolate type, ignored:", e);
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
			         ATTypeTag[] types) throws InterpreterException {
		super(map, state, lexicalParent, null);
		methodDictionary_ = methodDict;
		
		flags_ = flags; //a cloned object inherits all flags from original
		
		// clone inherits all types (this implies that clones of isolates are also isolates)
		typeTags_ = types;
		
		// ==, new and init should already be present in the method dictionary
		
		// set the 'super' field to point to the new dynamic parent
		setLocalField(_SUPER_NAME_, dynamicParent);
		
		// re-initialize all custom fields
		if (originalCustomFields != null) {
			customFields_ = new LinkedList();
			Iterator it = originalCustomFields.iterator();
			while (it.hasNext()) {
				ATField field = (ATField) it.next();
				customFields_.add(field.base_new(new ATObject[] { this }).asField());
			}
		}
	}
	
	/**
	 * Initialize a new AmbientTalk object with the given closure.
	 * 
	 * The closure encapsulates:
	 *  - the code with which to initialize the object
	 *  - the lexical parent of the object (but that parent should already be set)
	 *  - the lexically inherited fields for the object (the parameters of the closure)
	 */
	public void initializeWithCode(ATClosure code) throws InterpreterException {
		NATTable copiedBindings = Evaluator.evalMandatoryPars(
				code.base_method().base_parameters(),
				code.base_context());
		code.base_applyInScope(copiedBindings, this);
	}

	/**
	 * Invoke NATObject's primitive implementation, such that Java invocations of this
	 * method have the same behaviour as AmbientTalk invocations.
	 */
    public ATObject base_init(ATObject[] initargs) throws InterpreterException {
    	return this.prim_init(this, initargs);
    }
	
	/**
	 * The primitive implementation of init in objects is to invoke the init
	 * method of their parent.
	 * @param self the object that originally received the 'init' message.
	 * 
	 * def init(@args) {
	 *   super^init(@args)
	 * }
	 */
    private ATObject prim_init(ATObject self, ATObject[] initargs) throws InterpreterException {
    	ATObject parent = base_super();
    	return parent.meta_invoke(self, Evaluator._INIT_, NATTable.atValue(initargs));
    }
    
    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
    	return this.meta_invoke(this, _EQL_NAME_, NATTable.of(comparand)).asBoolean();
    }
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * Implements slot (field or method) access.
	 * 
	 * This method is an implementation-level method that needs to be supported by any AmbientTalk object,
	 * although it is not part of the metaobject protocol. Therefore it has no meta_ but an impl_ prefix.
	 * 
	 * Slot access proceeds as follows:
	 *  - if selector is bound to a method, the method is invoked.
	 *  - if selector is bound to a field bound to a closure, the closure is invoked.
	 *  - if selector is bound to a field not bound to a closure, the field is treated as a zero-
	 *  arity closure which is immediately applied and returns the field value (this implements the uniform access principle).
	 *  - otherwise the slot access is delegated to the parent object.
	 */
	public ATObject impl_accessSlot(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, receiver));
		} else {
			if (this.hasLocalField(selector)) {
				// reuse code of call frame to try and treat a field as an accessor
				return super.impl_accessSlot(receiver, selector, arguments);
			} else {
				return base_super().impl_accessSlot(receiver, selector, arguments);
			}
		}
	}
	
	/**
	 * Implements slot assignment. This method expects its selector to be an {@link AGAssignmentSymbol}
	 * which either represents a method directly, or represents field assignment implicitly.
	 * 
	 * This method is an implementation-level method that needs to be supported by any AmbientTalk object,
	 * although it is not part of the metaobject protocol. Therefore it has no meta_ but an impl_ prefix.
	 * 
	 * Slot mutation proceeds as follows:
	 *  - if selector is bound to a method, the method is invoked.
	 *  - if selector \ { := } is bound to a field, that field is assigned to the given value
	 *  (this implements the uniform access principle).
	 *  - otherwise, the slot mutation is carried out in the parent object.
	 */
	public ATObject impl_mutateSlot(ATObject receiver, ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, receiver));
		} else {
			try {
				// try to treat a local field as a mutator
				return super.impl_mutateSlot(receiver, selector, arguments);
			} catch (XSelectorNotFound e) {
				// if no field matching the selector exists, delegate to the parent
				return base_super().impl_mutateSlot(receiver, selector, arguments);
			}
		}
	}
	
	/**
	 * An ambienttalk object can respond to a message if a corresponding field or method exists
	 * either in the receiver object locally, or in one of its dynamic parents.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
		if (this.hasLocalField(selector) || this.hasLocalMethod(selector)) {
			return NATBoolean._TRUE_;
		} else {
			if (selector.isAssignmentSymbol()) {
				if (this.hasLocalField(selector.asAssignmentSymbol().getFieldName())) {
					return NATBoolean._TRUE_;
				}
			}
		}
		return base_super().meta_respondsTo(selector);
	}

	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * Implements slot (field or method) accessor selection.
	 * 
	 * This method is an implementation-level method that needs to be supported by any AmbientTalk object,
	 * although it is not part of the metaobject protocol. Therefore it has no meta_ but an impl_ prefix.
	 * 
	 * Slot accessor retrieval proceeds as follows:
	 *  - if selector is bound to a method, the method is wrapped in a closure and this closure is returned.
	 *  - if selector is bound to a field bound to a closure, that closure is returned.
	 *  - if selector is bound to a field not bound to a closure, a zero-arity closure is returned which
	 *    upon invocation returns the field value (this implements the uniform access principle).
	 *  - otherwise the slot accessor retrieval is delegated to the parent object.
	 */
	public ATClosure impl_selectAccessor(ATObject receiver, final ATSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return new NATClosure(this.getLocalMethod(selector), this, receiver);
		} else {
			if (this.hasLocalField(selector)) {
				return super.impl_selectAccessor(receiver, selector);
			} else {
				return base_super().impl_selectAccessor(receiver, selector);
			}
		}
	}
	
	/**
	 * Implements slot mutator retrieval. This method expects its selector to be an {@link AGAssignmentSymbol}
	 * which either represents a method directly, or represents a field mutator implicitly.
	 * 
	 * This method is an implementation-level method that needs to be supported by any AmbientTalk object,
	 * although it is not part of the metaobject protocol. Therefore it has no meta_ but an impl_ prefix.
	 * 
	 * Slot mutator retrieval proceeds as follows:
	 *  - if selector is bound to a method, a closure wrapping that method is returned.
	 *  - if selector \ { := } is bound to a field, a 1-arity closure is returned which
	 *    upon invocation sets the field to the given value (this implements the uniform access principle).
	 *  - otherwise, the slot mutator retrieval is delegated to the parent object.
	 */
	public ATClosure impl_selectMutator(ATObject receiver, final ATAssignmentSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return new NATClosure(this.getLocalMethod(selector), this, receiver);
		} else {
			try {
				// try to wrap a local field in a mutator
				return super.impl_selectMutator(receiver, selector);
			} catch (XSelectorNotFound e) {
				// if no field matching the selector exists, delegate to the parent
				return base_super().impl_selectMutator(receiver, selector);
			}
		}
	}	
		
	public ATObject impl_accessVariable(ATSymbol selector, ATTable arguments) throws InterpreterException {
		if(this.hasLocalMethod(selector)) {
			// apply the method with a context ctx where 
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return this.getLocalMethod(selector).base_applyInScope(arguments, new NATContext(this, this));
		} else {
			return super.impl_accessVariable(selector, arguments);
		}
	}
	
	public ATObject impl_mutateVariable(ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		if(this.hasLocalMethod(selector)) {
			// apply the method with a context ctx where 
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return this.getLocalMethod(selector).base_applyInScope(arguments, new NATContext(this, this));
		} else {
			return super.impl_mutateVariable(selector, arguments);
		}
	}
	
	public ATClosure impl_lookupMutator(ATAssignmentSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return new NATClosure(this.getLocalMethod(selector), this, this);
		} else {
			// try to wrap a local field in a mutator
			// the super implementation will delegate to the lexical parent is necessary
			return super.impl_lookupMutator(selector);
		}
	}
	
	public ATClosure impl_lookupAccessor(ATSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return new NATClosure(this.getLocalMethod(selector), this, this);
		} else {
			// try to wrap a local field in an accessor
			// the super implementation will delegate to the lexical parent is necessary
			return super.impl_lookupAccessor(selector);
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
	 * @deprecated use invocation with assignment symbols instead (uniform access)
	 * @return NIL
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol selector, ATObject value) throws InterpreterException {
		if (this.setLocalField(selector, value)) {
			return NATNil._INSTANCE_;
		} else {
			return base_super().meta_assignField(receiver, selector, value);
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
			dynamicParent = base_super().meta_clone();
		} else {
			// SHARES_A Relation : share the dynamic parent.
			dynamicParent = base_super();
		}
		
		// ! set the shares flags of this object *and* of its clone
		// both this object and the clone now share the map and method dictionary
		setFlag(_SHARE_DCT_FLAG_);
		setFlag(_SHARE_MAP_FLAG_);
		
		NATObject clone = this.createClone(variableMap_,
				          (Vector) stateVector_.clone(), // shallow copy
				          customFields_, // must be re-initialized by clone!
				          methodDictionary_,
				          dynamicParent,
				          lexicalParent_,
				          flags_, typeTags_);
		
		return clone;
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

	public ATBoolean meta_isExtensionOfParent() throws InterpreterException {
		return NATBoolean.atValue(isFlagSet(_ISAPARENT_FLAG_));
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
		ATSymbol name = method.base_name();
		if (methodDictionary_.containsKey(name) && !isPrimitive(name)) {
			throw new XDuplicateSlot(name);
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
		if (typeTags_.length == 0) {
			return NATText.atValue("<object:"+this.hashCode()+">");
		} else {
			return NATText.atValue("<object:"+this.hashCode()+
					               Evaluator.printElements(typeTags_, "[", ",", "]").javaValue+">");
		}
	}
	
	public boolean isCallFrame() {
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
	         					  ATTypeTag[] types) throws InterpreterException {
		return new NATObject(map,
	            state,
	            originalCustomFields,
	            methodDict,
	            dynamicParent,
	            lexicalParent,
	            flags,
	            types);
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
						return scope_.base_super().meta_isRelatedTo(object);
					}
				}).asBoolean();
	}
	
    /* ---------------------------------
     * -- Type Testing and Querying --
     * --------------------------------- */
	
    /**
     * Check whether one of the type tags of this object is a subtype of the given type.
     * If not, then delegate the query to the dynamic parent.
     */
    public ATBoolean meta_isTaggedAs(ATTypeTag type) throws InterpreterException {
    	if (isLocallyTaggedAs(type)) {
    		return NATBoolean._TRUE_;
    	} else {
        	// no type tags match, ask the parent
        	return base_super().meta_isTaggedAs(type);
    	}
    }
    
    /**
     * Return the type tags that were directly attached to this object.
     */
    public ATTable meta_typeTags() throws InterpreterException {
    	// make a copy of the internal type tag array to ensure that the types
    	// of the object are immutable. Tables allow assignment!
    	if (typeTags_.length == 0) {
    		return NATTable.EMPTY;
    	} else { 
    		ATTypeTag[] types = new ATTypeTag[typeTags_.length];
        	System.arraycopy(typeTags_, 0, types, 0, typeTags_.length);
        	return NATTable.atValue(types);
    	}
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
	 * Coercion only happens if the object is tagged with the correct type.
	 */
	private Object coerce(ATTypeTag requiredType, Class providedInterface) throws InterpreterException {
		if (this.meta_isTaggedAs(requiredType).asNativeBoolean().javaValue) {
			return Coercer.coerce(this, providedInterface);
		} else {
			// if the object does not possess the right type tag, raise a type error
			throw new XTypeMismatch(providedInterface, this);
		}
	}
	
	public ATBoolean asBoolean() throws InterpreterException { return (ATBoolean) coerce(NativeTypeTags._BOOLEAN_, ATBoolean.class); }
	public ATClosure asClosure() throws InterpreterException { return (ATClosure) coerce(NativeTypeTags._CLOSURE_, ATClosure.class); }
	public ATField asField() throws InterpreterException { return (ATField) coerce(NativeTypeTags._FIELD_, ATField.class); }
	public ATMessage asMessage() throws InterpreterException { return (ATMessage) coerce(NativeTypeTags._MESSAGE_, ATMessage.class); }
	public ATMethod asMethod() throws InterpreterException { return (ATMethod) coerce(NativeTypeTags._METHOD_, ATMethod.class); }
	public ATHandler asHandler() throws InterpreterException { return (ATHandler) coerce(NativeTypeTags._HANDLER_, ATHandler.class); }
	public ATNumber asNumber() throws InterpreterException { return (ATNumber) coerce(NativeTypeTags._NUMBER_, ATNumber.class); }
	public ATTable asTable() throws InterpreterException { return (ATTable) coerce(NativeTypeTags._TABLE_, ATTable.class); }
    public ATAsyncMessage asAsyncMessage() throws InterpreterException { return (ATAsyncMessage) coerce(NativeTypeTags._ASYNCMSG_, ATAsyncMessage.class);}
    public ATActorMirror asActorMirror() throws InterpreterException { return (ATActorMirror) coerce(NativeTypeTags._ACTORMIRROR_, ATActorMirror.class); }
    public ATTypeTag asTypeTag() throws InterpreterException { return (ATTypeTag) coerce(NativeTypeTags._TYPETAG_, ATTypeTag.class); }
	
	public ATBegin asBegin() throws InterpreterException { return (ATBegin) coerce(NativeTypeTags._BEGIN_, ATBegin.class); }
	public ATStatement asStatement() throws InterpreterException { return (ATStatement) coerce(NativeTypeTags._STATEMENT_, ATStatement.class); }
    public ATUnquoteSplice asUnquoteSplice() throws InterpreterException { return (ATUnquoteSplice) coerce(NativeTypeTags._UQSPLICE_, ATUnquoteSplice.class); }
    public ATSymbol asSymbol() throws InterpreterException { return (ATSymbol) coerce(NativeTypeTags._SYMBOL_, ATSymbol.class); }
    public ATSplice asSplice() throws InterpreterException { return (ATSplice) coerce(NativeTypeTags._SPLICE_, ATSplice.class); }
	public ATDefinition asDefinition() throws InterpreterException { return (ATDefinition) coerce(NativeTypeTags._DEFINITION_, ATDefinition.class); }
	public ATMessageCreation asMessageCreation() throws InterpreterException { return (ATMessageCreation) coerce(NativeTypeTags._MSGCREATION_, ATMessageCreation.class); }
	
	// ALL isXXX methods return true (can be overridden by programmer-defined base-level methods)
	
	public boolean isAmbientTalkObject() { return true; }
	
	// objects can only be 'cast' to a native category if they are marked with
	// the appropriate native type
	public boolean isSplice() throws InterpreterException { return meta_isTaggedAs(NativeTypeTags._SPLICE_).asNativeBoolean().javaValue; }
	public boolean isSymbol() throws InterpreterException { return meta_isTaggedAs(NativeTypeTags._SYMBOL_).asNativeBoolean().javaValue; }
	public boolean isTable() throws InterpreterException { return meta_isTaggedAs(NativeTypeTags._TABLE_).asNativeBoolean().javaValue; }
	public boolean isUnquoteSplice() throws InterpreterException { return meta_isTaggedAs(NativeTypeTags._UQSPLICE_).asNativeBoolean().javaValue; }
	public boolean isTypeTag() throws InterpreterException { return meta_isTaggedAs(NativeTypeTags._TYPETAG_).asNativeBoolean().javaValue; }
	
	
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
		ATMethod result = ((ATObject) methodDictionary_.get(selector)).asMethod();
		if(result == null) {
			throw new XSelectorNotFound(selector, this);
		} else {
			return result;
		}
	}
	
	/**
	 * Performs a type test for this object locally.
	 * @return whether this object is tagged with a particular type tag or not.
	 */
	private boolean isLocallyTaggedAs(ATTypeTag tag) throws InterpreterException {
    	for (int i = 0; i < typeTags_.length; i++) {
			if (typeTags_[i].base_isSubtypeOf(tag).asNativeBoolean().javaValue) {
				// if one type matches, return true
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
		for (; obj != NATNil._INSTANCE_ ; obj = obj.base_super()) {
			ATObject[] localFields = obj.meta_listFields().asNativeTable().elements_;
			for (int i = 0; i < localFields.length; i++) {
				ATField field = localFields[i].asField();
				ATSymbol fieldName = field.base_name();
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
		for (; obj != NATNil._INSTANCE_ ; obj = obj.base_super()) {
			// fast-path for native objects
			if (obj instanceof NATObject) {
				Collection localMethods = ((NATObject) obj).methodDictionary_.values();
				for (Iterator iter = localMethods.iterator(); iter.hasNext();) {
					ATMethod localMethod = (ATMethod) iter.next();
					ATSymbol methodName = localMethod.base_name();
					if (!encounteredNames.contains(methodName)) {
						methods.add(localMethod);
						encounteredNames.add(methodName);
					}
				}
			} else {
				ATObject[] localMethods = obj.meta_listMethods().asNativeTable().elements_;
				for (int i = 0; i < localMethods.length; i++) {
					ATMethod localMethod = localMethods[i].asMethod();
					ATSymbol methodName = localMethod.base_name();
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
