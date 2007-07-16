/**
 * AmbientTalk/2 Project
 * NATNil.java created on Jul 13, 2006 at 9:38:51 PM
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
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.natives.NATFarReference;
import edu.vub.at.actors.net.SerializationException;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
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
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.grammar.AGAssignmentSymbol;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaObject;
import edu.vub.at.objects.symbiosis.SymbioticATObjectMarker;
import edu.vub.at.util.logging.Logging;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This class implements default semantics for all test and conversion methods.
 * It also implements the default metaobject protocol semantics for native
 * AmbientTalk objects.
 * <p>
 * More specifically, this class encapsulates the behavior for:
 * <ul>
 *  <li>The behavior of all native objects.
 *  <li>The default behavior of all non-native objects.
 * </ul>
 * Native AmbientTalk objects contain no fields, only methods. Fields are represented
 * using accessor methods. Mutable fields also have a mutator method.
 * <p>
 * To allow for AmbientTalk language values to be unquoted into parsetrees,
 * a native object is considered to be a valid ambienttalk expression. 
 *
 * @author tvcutsem, smostinc
 */
public abstract class NativeATObject implements ATObject, ATExpression, Serializable {
	
    protected NativeATObject() {};

    /**
     * Asynchronous messages ( o<-m( args )) sent in the context of an object o (i.e. 
     * sent in a method or closure where the self pseudovariable is bound to o)  are 
     * delegated to the base-level send method of the actor in which the object o is 
     * contained.
     */
    public ATObject meta_send(ATAsyncMessage message) throws InterpreterException {
    	return OBJLexicalRoot._INSTANCE_.base_actor().base_send(message);
    }

    /**
     * By default, when an object receives an incoming asynchronous message, it tells
     * the message to process itself. The message's default behaviour is to subsequently
     * invoke the method corresponding to the message's selector on this object.
     */
    public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
    	return message.base_process(this);
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

    /**
     * By default, when a selection is not understood by a primitive object, an error is raised.
     */
    public ATClosure meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
        throw new XSelectorNotFound(selector, this);
    }
    
    /* ------------------------------------------
      * -- Slot accessing and mutating protocol --
      * ------------------------------------------ */

	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
        throw new XIllegalOperation("Cannot add fields to " + Evaluator.valueNameOf(this.getClass()));
    }

    /* ------------------------------------
      * -- Extension and cloning protocol --
      * ------------------------------------ */

    public ATObject meta_clone() throws InterpreterException {
        throw new XIllegalOperation("Cannot clone a native object of type " + this.getClass().getName());
    }

    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
        return Reflection.upInstanceCreation(this, initargs);
    }
    
    /* ---------------------------------
      * -- Structural Access Protocol  --
      * --------------------------------- */

    public ATNil meta_addField(ATField field) throws InterpreterException {
        throw new XIllegalOperation("Cannot add fields to " + Evaluator.valueNameOf(this.getClass()));
    }

    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        throw new XIllegalOperation("Cannot add methods to " + Evaluator.valueNameOf(this.getClass()));
    }

    public ATField meta_grabField(ATSymbol fieldName) throws InterpreterException {
    	throw new XSelectorNotFound(fieldName, this);
    }

    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        return Reflection.downBaseLevelMethod(this, methodName);
    }

    public ATTable meta_listFields() throws InterpreterException {
    	return NATTable.EMPTY;
    }

    public ATTable meta_listMethods() throws InterpreterException {
    	return NATTable.atValue(Reflection.downBaseLevelMethods(this));
    }

    /* ---------------------------------
      * -- Abstract Grammar Protocol   --
      * --------------------------------- */

    /**
     * All NATObjects which are not Abstract Grammar elements are self-evaluating.
     */
    public ATObject meta_eval(ATContext ctx) throws InterpreterException {
        return this;
    }

    /**
     * Quoting a native object returns itself, except for pure AG elements.
     */
    public ATObject meta_quote(ATContext ctx) throws InterpreterException {
        return this;
    }

    public abstract NATText meta_print() throws InterpreterException;

    /* ------------------------------
      * -- ATObject Mirror Fields   --
      * ------------------------------ */

    /**
     * Native objects have a SHARES-A parent link to 'nil', by default.
     */
    public ATBoolean meta_isExtensionOfParent() throws InterpreterException {
        return NATBoolean.atValue((NATObject._SHARES_A_));
    };

    /**
     * By default numbers, tables and so on have root as their lexical parent.
     */
    public ATObject impl_lexicalParent() throws InterpreterException {
        return Evaluator.getGlobalLexicalScope();
    }
    
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return NATBoolean.atValue(
				this.getClass() == original.getClass());
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return this.meta_isCloneOf(object);
	}
	
    /* ---------------------------------
     * -- Type Testing and Querying --
     * --------------------------------- */
	
    /**
     * Native objects implement the type test non-recursively: only the type tags
     * returned by {@link this#meta_getTypeTags()} are tested against.
     */
    public ATBoolean meta_isTaggedAs(ATTypeTag type) throws InterpreterException {
    	ATObject[] types = this.meta_typeTags().asNativeTable().elements_;
    	for (int i = 0; i < types.length; i++) {
			if (types[i].asTypeTag().base_isSubtypeOf(type).asNativeBoolean().javaValue) {
				return NATBoolean._TRUE_;
			}
		}
    	return NATBoolean._FALSE_;
    }
    
    /**
     * By default, a native object (and also nil) has no type tags.
     */
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.EMPTY;
    }
	
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * This method allows objects to decide which object should be serialized in their
     * stead when they are passed as argument in an asynchronous message send that
     * crosses actor boundaries.
     */
    public abstract ATObject meta_pass() throws InterpreterException;
	
	/**
	 * Delegate the responsibility of serialization to the AT/2 meta-level 
	 */
	public Object writeReplace() throws ObjectStreamException {
		try {
			return this.meta_pass();
		} catch(InterpreterException e) {
			throw new InvalidObjectException("Failed to pass object " + this + ": " + e.getMessage());
		}
	}
	
    public abstract ATObject meta_resolve() throws InterpreterException;
	
	/**
	 * Delegate the responsibility of deserialization to the AT/2 meta-level 
	 */
	public Object readResolve() throws ObjectStreamException {
		try {
			return this.meta_resolve();
		} catch(InterpreterException e) {
			throw new SerializationException(e);
		}
	}

    /* ---------------------------------
      * -- Value Conversion Protocol   --
      * --------------------------------- */

    public boolean isSymbol() throws InterpreterException {
        return false;
    }

    public boolean isTable() throws InterpreterException {
        return false;
    }

    public boolean isCallFrame() throws InterpreterException {
        return false;
    }

    public boolean isUnquoteSplice() throws InterpreterException {
        return false;
    }

    public boolean isVariableAssignment() throws InterpreterException {
        return false;
    }
    
    public boolean isSplice() throws InterpreterException {
        return false;
    }
    
    public boolean isMessageCreation() throws InterpreterException {
    	return false;
    }

    public boolean isAmbientTalkObject() { 
    	return false;
    }
    
    public boolean isJavaObjectUnderSymbiosis() {
    	return false;
    }

    public boolean isNativeBoolean() {
        return false;
    }
    
    public boolean isNativeText() {
        return false;
    }

    public boolean isNativeField() {
        return false;
    }
    
    public boolean isTypeTag() throws InterpreterException {
        return false;
    }
    
    public ATClosure asClosure() throws InterpreterException {
        throw new XTypeMismatch(ATClosure.class, this);
    }

    public ATSymbol asSymbol() throws InterpreterException {
        throw new XTypeMismatch(ATSymbol.class, this);
    }

    public ATTable asTable() throws InterpreterException {
        throw new XTypeMismatch(ATTable.class, this);
    }

    public ATBoolean asBoolean() throws InterpreterException {
        throw new XTypeMismatch(ATBoolean.class, this);
    }

    public ATNumber asNumber() throws InterpreterException {
        throw new XTypeMismatch(ATNumber.class, this);
    }

    public ATMessage asMessage() throws InterpreterException {
        throw new XTypeMismatch(ATMessage.class, this);
    }

    public ATField asField() throws InterpreterException {
        throw new XTypeMismatch(ATField.class, this);
    }

    public ATMethod asMethod() throws InterpreterException {
        throw new XTypeMismatch(ATMethod.class, this);
    }

    public ATHandler asHandler() throws InterpreterException {
    	    throw new XTypeMismatch(ATHandler.class, this);
    }

    public ATTypeTag asTypeTag() throws InterpreterException {
	    throw new XTypeMismatch(ATTypeTag.class, this);
    }
    
    // Conversions for concurrency and distribution related object
    public boolean isFarReference() {
    	return false;
    }
    
    public ATFarReference asFarReference() throws InterpreterException {
  	    throw new XTypeMismatch(ATFarReference.class, this);
  	}
    
    public ATAsyncMessage asAsyncMessage() throws InterpreterException {
  	    throw new XTypeMismatch(ATAsyncMessage.class, this);
  	}
    
    public ATActorMirror asActorMirror() throws InterpreterException {
    	throw new XTypeMismatch(ATActorMirror.class, this);
    }
    
    // Conversions for abstract grammar elements

    public ATStatement asStatement() throws InterpreterException {
        throw new XTypeMismatch(ATStatement.class, this);
    }

    public ATDefinition asDefinition() throws InterpreterException {
        throw new XTypeMismatch(ATDefinition.class, this);
    }

    public ATExpression asExpression() throws InterpreterException {
        return this;
    }

    public ATBegin asBegin() throws InterpreterException {
        throw new XTypeMismatch(ATBegin.class, this);
    }

    public ATMessageCreation asMessageCreation() throws InterpreterException {
        throw new XTypeMismatch(ATMessageCreation.class, this);
    }

    public ATUnquoteSplice asUnquoteSplice() throws InterpreterException {
        throw new XTypeMismatch(ATUnquoteSplice.class, this);
    }

    public ATAssignVariable asVariableAssignment() throws InterpreterException {
        throw new XTypeMismatch(ATAssignVariable.class, this);
    }
    
    public ATSplice asSplice() throws InterpreterException {
        throw new XTypeMismatch(ATSplice.class, this);
    }
    
    // Conversions for native values
    public NATObject asAmbientTalkObject() throws XTypeMismatch {
    	throw new XTypeMismatch(NATObject.class, this);
    }

    public NATMirage asMirage() throws XTypeMismatch {
    	throw new XTypeMismatch(NATMirage.class, this);
    }

    public NATNumber asNativeNumber() throws XTypeMismatch {
        throw new XTypeMismatch(NATNumber.class, this);
    }

    public NATFraction asNativeFraction() throws XTypeMismatch {
        throw new XTypeMismatch(NATFraction.class, this);
    }

    public NATText asNativeText() throws XTypeMismatch {
    	throw new XTypeMismatch(NATText.class, this);
    }

    public NATTable asNativeTable() throws XTypeMismatch {
        throw new XTypeMismatch(NATTable.class, this);
    }

    public NATBoolean asNativeBoolean() throws XTypeMismatch {
        throw new XTypeMismatch(NATBoolean.class, this);
    }
    
    public NATNumeric asNativeNumeric() throws XTypeMismatch {
        throw new XTypeMismatch(NATNumeric.class, this);
    }
    
    public NATFarReference asNativeFarReference() throws XTypeMismatch {
    	throw new XTypeMismatch(NATFarReference.class, this);
    }

    public JavaObject asJavaObjectUnderSymbiosis() throws XTypeMismatch {
    	    throw new XTypeMismatch(JavaObject.class, this);
    }
    
    public JavaClass asJavaClassUnderSymbiosis() throws XTypeMismatch {
	    throw new XTypeMismatch(JavaClass.class, this);
    }
    
    /**
     * Only true objects have a dynamic pointer, native objects denote 'nil' to
     * be their dynamic parent when asked for it. Note that, for native objects,
     * 'super' is a read-only field (i.e. only an accessor for the virtual field exists).
     */
    public ATObject base_super() throws InterpreterException {
        return OBJNil._INSTANCE_;
    };

    public String toString() {
        return Evaluator.toString(this);
    }
    
    /**
     * Java method invocations of equals are transformed into
     * AmbientTalk '==' method invocations.
     */
	public boolean equals(Object other) {
		try {
			if (other instanceof ATObject) {
				return this.base__opeql__opeql_((ATObject) other).asNativeBoolean().javaValue;
			} else if (other instanceof SymbioticATObjectMarker) {
				return this.base__opeql__opeql_(
						((SymbioticATObjectMarker) other)._returnNativeAmbientTalkObject()).asNativeBoolean().javaValue;
			}
		} catch (InterpreterException e) {
			Logging.Actor_LOG.warn("Error during equality testing:", e);
		}
		return false; 
	}
    
	/**
	 * By default, two AmbientTalk objects are equal if they are the
	 * same object, or one is a proxy for the same object.
	 */
    public ATBoolean base__opeql__opeql_(ATObject other) throws InterpreterException {
		// by default, ATObjects use pointer equality
		return NATBoolean.atValue(this == other);
    }
    
    public ATObject base_new(ATObject[] initargs) throws InterpreterException {
    	return this.meta_newInstance(NATTable.atValue(initargs));
    }
    
    public ATObject base_init(ATObject[] initargs) throws InterpreterException {
    	return OBJNil._INSTANCE_;
    }
    
	/**
	 * This method is used to evaluate code of the form <tt>selector</tt> within the scope
	 * of this object. A call frame resolves such a lookup request by checking whether
	 * a field corresponding to the selector exists locally. If it does, the result is
	 * returned. If it does not, the search continues recursively in the call frame's
	 * lexical parent.
	 */
	public ATObject impl_call(ATSymbol selector, ATTable arguments) throws InterpreterException {
		if (selector.isAssignmentSymbol()) {
			return this.impl_callMutator(selector.asAssignmentSymbol(), arguments);
		} else {
			return this.impl_callAccessor(selector, arguments);
		}
	}

    /**
	 * A lookup can only be issued at the base level by writing
	 * <tt>selector</tt> inside the scope of a particular object. For
	 * primitive language values, this should not happen as no AmbientTalk code
	 * can be possibly nested within native code. However, using
	 * meta-programming a primitive object could be installed as the lexical
	 * parent of an AmbientTalk object.
	 * 
	 * One particular case where this method will often be called is when a
	 * lookup reaches the lexical root, OBJLexicalRoot, which inherits this
	 * implementation.
	 * 
	 * In such cases a lookup is treated exactly like a selection, where the
	 * 'original receiver' of the selection equals the primitive object.
	 * 
	 * 
	 * An object resolves accessor call requests by checking whether
	 * a field corresponding to the selector exists locally. If it does, the result is
	 * returned. If it does not, the search continues recursively in the object's
	 * lexical parent.
	 */
	public ATObject impl_callAccessor(ATSymbol selector, ATTable arguments) throws InterpreterException {
		if(this.hasLocalMethod(selector)) {
			// apply the method with a context ctx where 
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, this));
		} else {
			if (this.hasLocalField(selector)) {
				ATObject fieldValue = this.getLocalField(selector);
				
				if (fieldValue.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
					return fieldValue.asClosure().base_apply(arguments);
				} else {
					NativeClosure.checkNullaryArguments(selector, arguments);
					return fieldValue;
				}
			} else {
				return this.impl_lexicalParent().impl_callAccessor(selector, arguments);
			}
		}
	}

	/**
	 * This method is used to evaluate code of the form <tt>selector := v</tt> within the scope
	 * of this object. An object resolves such a lookup request by checking whether
	 * a field corresponding to the selector exists locally. If it does, the field is assigned.
	 * If it does not, the search continues recursively in the call frame's
	 * lexical parent.
	 */
	public ATObject impl_callMutator(ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		if(this.hasLocalMethod(selector)) {
			// apply the method with a context ctx where 
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, this));
		} else {
			ATSymbol fieldSelector = selector.getFieldName();
			if (this.hasLocalField(fieldSelector)) {
				ATObject value = NativeClosure.checkUnaryArguments(selector, arguments);
				this.setLocalField(fieldSelector, value);
				return value;
			} else {
				return this.impl_lexicalParent().impl_callMutator(selector, arguments);
			}
		}
	}	
	
	public ATObject impl_callField(ATSymbol selector) throws InterpreterException {
		// if selector is bound to a method, treat 'm' as 'm()'
		if(this.hasLocalMethod(selector)) {
			// apply the method with a context ctx where 
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return this.getLocalMethod(selector).base_apply(NATTable.EMPTY, new NATContext(this, this));
		} else {
			if (this.hasLocalField(selector)) {
				// simply return the field's value, regardless of whether it is bound to a
				// closure or not
				return this.getLocalField(selector);
			} else {
				return this.impl_lexicalParent().impl_callField(selector);
			}
		}
	}
	
	/**
	 * invoke should dispatch to specific invocation primitives
	 * depending on whether or not the given selector denotes an assignment.
	 */
    public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
        // If the selector is an assignment symbol (i.e. `field:=) try to assign the corresponding field
		if (selector.isAssignmentSymbol()) {
			return this.impl_invokeMutator(receiver, selector.asAssignmentSymbol(), arguments);
		} else {
			return this.impl_invokeAccessor(receiver, selector, arguments);
		}
    }
    
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
	 *  
	 * Comments from call frame:
	 * Normally, call frames are not used in receiverful method invocation expressions.
	 * That is, normally, the content of call frames is accessed via the {@link this#meta_lookup(ATSymbol)} operation.
	 * 
	 * Invocation on call frames is much more ad hoc than on real objects.
	 * A call frame responds to an invocation by looking up the selector in its own fields (without delegating!)
	 * and by applying a potential closure bound to that field.
	 * 
	 * If the field is not bound to a closure, the field itself is treated as a zero-argument closure
	 * (implementing the UAP).
	 */
	public ATObject impl_invokeAccessor(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, receiver));
		} else {
			if (this.hasLocalField(selector)) {
				ATObject fieldValue = this.getLocalField(selector);
				
				if (fieldValue.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
					return fieldValue.asClosure().base_apply(arguments);
				} else {
					NativeClosure.checkNullaryArguments(selector, arguments);
					return fieldValue;
				}
			} else {
				return base_super().impl_invokeAccessor(receiver, selector, arguments);
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
	 *  
	 *  
	 * For native objects:
     * The default behaviour of 'invoke' for primitive non-object ambienttalk language values is
     * to check whether the requested functionality is provided by a native Java method
     * with the same selector, but prefixed with <tt>base_</tt>.
     * 
     * Because an explicit AmbientTalk method invocation must be converted into an implicit
     * Java method invocation, the invocation must be deified ('upped'). The result of the
     * upped invocation is a Java object, which must subsequently be 'downed' again.
     * 
     * According to the uniform access principle, access to fields (both for reading and mutating)
     * is handled by the invoke operation. A field is represented simply by a Java accessor
     * and/or mutator method. Mutator methods end in '__opeql_' which is the equivalent of ':='
     * at the Java level.
     *
     * If no method to invoke is found, doesNotUnderstand is invoked which should
     * return a closure to be invoked with the appropriate arguments.
     * 
     * comments from call frame:
	 * Invoking a mutator method on a call frame means the call frame requires a field
	 * corresponding to the assignment symbol \ {:=}. The field is then assigned, regardless
	 * of its value (i.e. if it's a closure, the closure is not in itself treated as a mutator).
	 * @throws XSelectorNotFound if a suitable field mutator cannot be found 
	 */
	public ATObject impl_invokeMutator(ATObject receiver, ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, receiver));
		} else {
			// try to treat a local field as a mutator
			ATSymbol fieldSelector = selector.getFieldName();
			if (this.hasLocalField(fieldSelector)) {
				ATObject value = NativeClosure.checkUnaryArguments(selector, arguments);
				this.setLocalField(fieldSelector, value);
				return value;
			} else {
				// if no field matching the selector exists, delegate to the parent
				return base_super().impl_invokeMutator(receiver, selector, arguments);
			}
		}
	}

	public ATObject meta_invokeField(ATObject receiver, ATSymbol selector) throws InterpreterException {
		// if selector is bound to a method, treat 'o.m' as 'o.m()'
		if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			return this.getLocalMethod(selector).base_apply(NATTable.EMPTY, new NATContext(this, receiver));
		} else {
			if (this.hasLocalField(selector)) {
				// return the field's value, regardless of whether it is a closure or not
				return this.getLocalField(selector);
			} else {
				return base_super().meta_invokeField(receiver, selector);
			}
		}
	}
	
	public ATClosure impl_lookup(ATSymbol selector) throws InterpreterException {
		if (selector.isAssignmentSymbol()) {
			return this.impl_lookupMutator(selector.asAssignmentSymbol());
		} else {
			return this.impl_lookupAccessor(selector);
		}
	}
	
	public ATClosure impl_lookupAccessor(final ATSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return this.getLocalMethod(selector).base_wrap(this, this);
		} else {
			// try to wrap a local field in an accessor
			if(this.hasLocalField(selector)) {
				final NativeATObject scope = this;
				return new NativeClosure.Accessor(selector, this) {
					public ATObject access() throws InterpreterException {
						return scope.getLocalField(selector);
					}
				};
			} else {
				return this.impl_lexicalParent().impl_lookupAccessor(selector);
			}
		}
	}

	public ATClosure impl_lookupMutator(ATAssignmentSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return this.getLocalMethod(selector).base_wrap(this, this);
		} else {
			final ATSymbol fieldSelector = selector.getFieldName();
			// try to wrap a local field in a mutator
			if(this.hasLocalField(fieldSelector)) {
				final NativeATObject scope = this;
				return new NativeClosure.Mutator(selector, this) {
					public ATObject mutate(ATObject arg) throws InterpreterException {
						scope.setLocalField(fieldSelector, arg);
						return arg;
					}
				};
			} else {
				return this.impl_lexicalParent().impl_lookupMutator(selector);
			}
		}
	}

	/**
	 * select should dispatch to specific selection primitives
	 * depending on whether or not the given selector denotes an assignment.
	 */
    public ATClosure meta_select(ATObject receiver, final ATSymbol selector) throws InterpreterException {
		if (selector.isAssignmentSymbol()) {
			return this.impl_selectMutator(receiver, selector.asAssignmentSymbol());
		} else {
			return this.impl_selectAccessor(receiver, selector);
		}
    }
    
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
    		return this.getLocalMethod(selector).base_wrap(this, receiver);
    	} else {
    		if (this.hasLocalField(selector)) {
    			final NativeATObject scope = this;
    			return new NativeClosure.Accessor(selector, this) {
    				public ATObject access() throws InterpreterException {
    					return scope.getLocalField(selector);
    				}
    			};
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
    		return this.getLocalMethod(selector).base_wrap(this, receiver);
    	} else {
    		// try to wrap a local field in a mutator
    		final ATSymbol fieldSelector = selector.getFieldName();
    		if (this.hasLocalField(fieldSelector)) {
    			final NativeATObject scope = this;
    			return new NativeClosure.Mutator(selector, this) {
    				public ATObject mutate(ATObject arg) throws InterpreterException {
    					scope.setLocalField(fieldSelector, arg);
    					return arg;
    				}
    			};
    		} else {
    			// if no field matching the selector exists, delegate to the parent
    			return base_super().impl_selectMutator(receiver, selector);
    		}
    	}
    }
	
	/** native objects have no fields */
	protected boolean hasLocalField(ATSymbol sym) throws InterpreterException {
		return false;
	}

	/**
	 * A native Java object has a local method if it implements a
	 * native Java method corresponding to the selector prefixed by 'base_'.
	 */
	protected boolean hasLocalMethod(ATSymbol atSelector) throws InterpreterException {
        String jSelector = Reflection.upBaseLevelSelector(atSelector);
        return Reflection.upRespondsTo(this, jSelector);
	}
	
	protected ATObject getLocalField(ATSymbol selector) throws InterpreterException {
		throw new XSelectorNotFound(selector, this);
	}
	
	protected ATMethod getLocalMethod(ATSymbol selector) throws InterpreterException {
		String methSelector = Reflection.upBaseLevelSelector(selector);
		return Reflection.upMethodSelection(this, methSelector, selector);
	}
	
	protected void setLocalField(ATSymbol selector, ATObject value) throws InterpreterException {
		throw new XSelectorNotFound(selector, this);
	}
	
}
