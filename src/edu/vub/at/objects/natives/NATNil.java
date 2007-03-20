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
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XObjectOffline;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.exceptions.XUserDefined;
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
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaObject;
import edu.vub.at.objects.symbiosis.SymbioticATObjectMarker;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * NATNil implements default semantics for all test and conversion methods.
 *
 * @author smostinc
 */
public class NATNil implements ATNil, Serializable {
	
    protected NATNil() {};

    public final static NATNil _INSTANCE_ = new NATNil();

    /* ------------------------------
      * -- Message Sending Protocol --
      * ------------------------------ */

	/**
     * Asynchronous messages ( o<-m( args )) sent in the context of an object o (i.e. 
     * sent in a method or closure where the self pseudovariable is bound to o)  are 
     * delegated to the base-level send method of the actor in which the object o is 
     * contained.
     */
    public ATObject meta_send(ATAsyncMessage message) throws InterpreterException {
    	return OBJLexicalRoot._INSTANCE_.base_getActor().base_send(message);
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
     * The default behaviour of 'delegate' for primitive non-object ambienttalk language values is
     * to check whether the requested functionality is provided by a native Java method
     * with the same selector, but prefixed with 'base_'.
     *
     * Because an explicit AmbientTalk method invocation must be converted into an implicit
     * Java method invocation, the invocation must be deified ('upped'). The result of the
     * upped invocation is a Java object, which must subsequently be 'downed' again.
     * 
     * If no method to invoke is found, doesNotUnderstand is invoked which should
     * return a closure to be invoked with the appropriate arguments.
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
        try {
			String jSelector = Reflection.upBaseLevelSelector(atSelector);
			return Reflection.upInvocation(this /*receiver*/, jSelector, atSelector, arguments);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			return receiver.meta_doesNotUnderstand(atSelector).asClosure().base_apply(arguments);
		}
    }

    /**
     * An ambienttalk language value can respond to a message if it implements
     * a native Java method corresponding to the selector prefixed by 'base_'.
     */
    public ATBoolean meta_respondsTo(ATSymbol atSelector) throws InterpreterException {
        String jSelector = Reflection.upBaseLevelSelector(atSelector);
        return NATBoolean.atValue(Reflection.upRespondsTo(this, jSelector));
    }

    /**
     * By default, when a selection is not understood by a primitive object, an error is raised.
     */
    public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
        throw new XSelectorNotFound(selector, this);
    }
    
    /* ------------------------------------------
      * -- Slot accessing and mutating protocol --
      * ------------------------------------------ */

    /**
     * It is possible to select a method from any ambienttalk value provided that it
     * offers the method in its provided interface. The result is a NativeMethod wrapper
     * which encapsulates the reflective Method object as well as the receiver.
     * 
     * There exists a certain ambiguity in field selection on AmbientTalk implementation-level objects.
     * When nativeObject.m is evaluated, the corresponding Java class must have a method named either
     *  base_getM which means m is represented as a readable field, or
     *  base_m which means m is represented as a method
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
        String jSelector = Reflection.upBaseFieldAccessSelector(selector);
        try {
            return Reflection.upFieldSelection(this, jSelector, selector);
        } catch (XSelectorNotFound e) {
        	e.catchOnlyIfSelectorEquals(selector);
            jSelector = Reflection.upBaseLevelSelector(selector);

            try {
				return Reflection.upMethodSelection(this, jSelector, selector);
			} catch (XSelectorNotFound e2) {
				e2.catchOnlyIfSelectorEquals(selector);
				return receiver.meta_doesNotUnderstand(selector);
			}
        }
    }

    /**
     * A lookup can only be issued at the base level by writing <tt>selector</tt> inside the scope
     * of a particular object. For primitive language values, this should not happen
     * as no AmbientTalk code can be possibly nested within native code. However, using
     * meta-programming a primitive object could be installed as the lexical parent of an AmbientTalk object.
     *
     * One particular case where this method will often be called is when a lookup reaches
     * the lexical root, OBJLexicalRoot, which inherits this implementation.
     *
     * In such cases a lookup is treated exactly like a selection, where the 'original receiver'
     * of the selection equals the primitive object.
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
        try {
        	  return this.meta_select(this, selector);
        } catch(XSelectorNotFound e) {
        	  // transform selector not found in undefined variable access
        	  throw new XUndefinedField("variable access", selector.base_getText().asNativeText().javaValue);
        }
    }

    public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
        throw new XIllegalOperation("Cannot add fields to " + Evaluator.valueNameOf(this.getClass()));
    }

    /**
     * Normally, a variable assignment cannot be performed on a native AmbientTalk object.
     * This is because a variable assignment can normally be only raised by performing
     * an assignment in the lexical scope of an object. However, using metaprogramming
     * a native object could be installed as the lexical parent of an AT object. In such
     * cases, variable assignment is treated as field assignment.
     * 
     * One particular case where this method will often be called is when a variable assignment reaches
     * the lexical root, OBJLexicalRoot, which inherits this implementation.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
        return this.meta_assignField(this, name, value);
    }

    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
    	String jSelector = Reflection.upBaseFieldMutationSelector(name);
    	
        // try to invoke a native base_setName method
        try {	   
           Reflection.upFieldAssignment(receiver, jSelector, name, value);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(name);
			// if such a method does not exist, the field assignment has failed
			throw new XUnassignableField(name.base_getText().asNativeText().javaValue);
		}
		
        return NATNil._INSTANCE_;
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
        return Reflection.downBaseLevelField(this, fieldName);
    }

    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        return Reflection.downBaseLevelMethod(this, methodName);
    }

    public ATTable meta_listFields() throws InterpreterException {
        return NATTable.atValue(Reflection.downBaseLevelFields(this));
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

    public NATText meta_print() throws InterpreterException {
        return NATText.atValue("nil");
    }

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
     * By default numbers, tables and so on do not have lexical parents,
     */
    public ATObject meta_getLexicalParent() throws InterpreterException {
        return NATNil._INSTANCE_;
    }

    /* ---------------------------------
      * -- Value Conversion Protocol   --
      * --------------------------------- */

    public boolean isClosure() throws InterpreterException {
        return false;
    }

    public boolean isSymbol() throws InterpreterException {
        return false;
    }

    public boolean isBoolean() throws InterpreterException {
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

    public boolean isMethod() throws InterpreterException {
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
    
    public boolean isStripe() throws InterpreterException {
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

    public ATStripe asStripe() throws InterpreterException {
	    throw new XTypeMismatch(ATStripe.class, this);
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
        throw new XTypeMismatch(ATExpression.class, this);
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
    
    public NATException asNativeException() throws XTypeMismatch {
    		return new NATException(new XUserDefined(this));
    }
    
    public String toString() {
        return Evaluator.toString(this);
    }
    
    /**
     * Only true objects have a dynamic pointer, native objects denote 'nil' to
     * be their dynamic parent when asked for it. Note that, for native objects,
     * 'super' is a read-only field (i.e. there is no base_setSuper).
     */
    public ATObject base_getSuper() throws InterpreterException {
        return NATNil._INSTANCE_;
    };


	public boolean equals(Object other) {
		try {
			if (other instanceof ATObject) {
				return this.base__opeql__opeql_((ATObject) other).asNativeBoolean().javaValue;
			} else if (other instanceof SymbioticATObjectMarker) {
				return this.base__opeql__opeql_(
						((SymbioticATObjectMarker) other)._returnNativeAmbientTalkObject()).asNativeBoolean().javaValue;
			}
		} catch (InterpreterException e) { }
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
    	return NATNil._INSTANCE_;
    }

	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return NATBoolean.atValue(
				this.getClass() == original.getClass());
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return this.meta_isCloneOf(object);
	}
	
    /* ---------------------------------
     * -- Stripe Testing and Querying --
     * --------------------------------- */
	
    /**
     * Native objects implement the stripe test non-recursively: only the stripes
     * returned by meta_getStripes are tested against.
     */
    public ATBoolean meta_isStripedWith(ATStripe stripe) throws InterpreterException {
    	ATObject[] stripes = this.meta_getStripes().asNativeTable().elements_;
    	for (int i = 0; i < stripes.length; i++) {
			if (stripes[i].asStripe().base_isSubstripeOf(stripe).asNativeBoolean().javaValue) {
				return NATBoolean._TRUE_;
			}
		}
    	return NATBoolean._FALSE_;
    }
    
    /**
     * By default, a native object (and also nil) has no stripes.
     */
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.EMPTY;
    }
	
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * This method allows objects to decide which object should be serialized in their
     * stead when they are passed as argument in an asynchronous message send that
     * crosses actor boundaries. By default, nil decides that it is safe to serialize
     * the object itself. Special objects such as e.g. closures should override this
     * method and return a far reference encoding instead.
     */
    public ATObject meta_pass() throws InterpreterException {
    	return this;
    }
    
    /**
     * After deserialization, ensure that nil remains unique.
     */
    public ATObject meta_resolve() throws InterpreterException {
    	return NATNil._INSTANCE_;
    }
	
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
    
	/**
	 * Delegate the responsibility of deserialization to the AT/2 meta-level 
	 */
	public Object readResolve() throws ObjectStreamException {
		try {
			return this.meta_resolve();
		} catch(XObjectOffline e) {
			InvalidObjectException e2 = new InvalidObjectException("Failed to resolve an expired object " + this + ": " + e.getMessage());
			e2.initCause(e);
			throw e2; 
		} catch(InterpreterException e) {
			throw new InvalidObjectException("Failed to resolve object " + this + ": " + e.getMessage());
		}
	}

}
