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
import edu.vub.at.actors.ATLetter;
import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.actors.natives.NATFarReference;
import edu.vub.at.actors.natives.NATFarReference.NATOutboxLetter;
import edu.vub.at.actors.natives.NATRemoteFarRef;
import edu.vub.at.actors.net.SerializationException;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.eval.Import.DelegateMethod;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XSerializationError;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMethodInvocation;
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
import edu.vub.at.objects.grammar.ATQuote;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.NATIntrospectiveMirror;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaMethod;
import edu.vub.at.objects.symbiosis.JavaObject;
import edu.vub.at.parser.SourceLocation;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.TempFieldGenerator;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
     * Asynchronous messages ( <tt>o<-m( args )</tt> ) sent in the context of an object o (i.e. 
     * sent in a method or closure where the self pseudovariable is bound to o)  are 
     * delegated to the base-level send method of the actor in which the object o is 
     * contained.
     */
    public ATObject meta_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException {
    	return OBJLexicalRoot._INSTANCE_.base_reflectOnActor().base_send(receiver, message);
    }

    /**
     * When a local object reference is used as an eventual reference by sending it an
     * asynchronous message, the object simply schedules the message in its owner's
     * message queue and returns <tt>nil</tt> immediately. The message is later transformed
     * into a method invocation (in a later actor execution "turn").
     * 
     * Note: in pre-2.9 versions of AmbientTalk, this method was invoked in the later
     * execution turn and immediately performed the message processing. Now, this method
     * is invoked by the sender of an async message and allows custom eventual references
     * to return a value other than <tt>nil</tt> for an async message send.
     */
    public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
    	ELActor.currentActor().acceptSelfSend(this, message);
		return Evaluator.getNil();
    }
    
	/**
	 * An ambienttalk object can respond to a message if a corresponding field or method exists
	 * either in the receiver object locally, or in one of its dynamic parents.
	 * Fields also implicitly define a mutator whose name has the form <tt>field:=</tt>.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
		if (this.hasLocalField(selector) || this.hasLocalMethod(selector)) {
			return NATBoolean._TRUE_;
		} else {
			if (selector.isAssignmentSymbol()) {
				if (this.hasLocalField(selector.asAssignmentSymbol().base_fieldName())) {
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
    
    public ATObject meta_removeSlot(ATSymbol selector) throws InterpreterException {
		throw new XIllegalOperation("Cannot remove slot "+selector+" from native object "+this);
    }

    public ATTable meta_listFields() throws InterpreterException {
    	return NATTable.EMPTY;
    }

    public ATTable meta_listMethods() throws InterpreterException {
    	return NATTable.atValue(Reflection.downBaseLevelMethods(this));
    }
    
    public ATNil meta_addSlot(ATMethod method) throws InterpreterException {
    	return this.meta_addMethod(method);
    }
    
    public ATMethod meta_grabSlot(ATSymbol methodName) throws InterpreterException {
        try {
        	return this.meta_grabMethod(methodName);
        } catch(XSelectorNotFound e) {
        	e.catchOnlyIfSelectorEquals(methodName);
        	if (methodName.isAssignmentSymbol()) {
            	ATField mutator = this.meta_grabField(methodName.asAssignmentSymbol().base_fieldName());
            	return mutator.base_mutator();
        	} else {
            	ATField accessor = this.meta_grabField(methodName);
            	return accessor.base_accessor();
        	}
        }
    }
    
    public ATTable meta_listSlots() throws InterpreterException {
    	ATObject[] fields = this.meta_listFields().asNativeTable().elements_;
    	ATObject[] methods = this.meta_listMethods().asNativeTable().elements_;
    	
    	int fieldLength = fields.length * 2;
    	ATObject[] slots = new ATObject[fieldLength + methods.length];
    	int i = 0;
    	// add field accessors and mutators
    	for (; i < fieldLength ; i+=2) {
    		ATField field = (ATField) fields[i/2];
    		slots[i] = field.base_accessor();
        	slots[i+1] = field.base_mutator();
    	}
    	// add regular methods
    	for (; i < fieldLength + methods.length ; i++) {
    		slots[i] = methods[i - fieldLength];
    	}
    	
    	return NATTable.atValue(slots);
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
    
    // start of serialization
    // create a new temp field generator and start recursive serialization
    public NATText meta_asCode() throws InterpreterException {
    	TempFieldGenerator objectMap = new TempFieldGenerator();
		NATText name = this.impl_asCode(objectMap);
		return objectMap.generateCode(name);
    };
    
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    	throw new XSerializationError("Unable to serialize object: " + this.meta_print().javaValue + " (type: " + this.getClass() + ")");
    };
    
    // abstract grammar elements can be serialized without a quote
    // for other objects this defaults to impl_asCode
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
    	return this.impl_asCode(objectMap);
    };

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
			Object o = this.meta_pass();
			// egb: code for bugFix#53.
			int looped = 0;
			// if meta pass returned a mirage, we need to call meta_pass again 
			// otherwise we will serialize NATMirage instead of what the programmer defined in pass()
			while( o instanceof NATMirage && (! ((NATMirage) o).isIsolate() ) && !(o.equals(this)) ){
			  o = ((NATMirage) o).meta_pass();
			  //TODO: While we don't have cycle detection on serialization, warn the developer.		 
			  looped++;
			  if ( looped == 20) Logging.Actor_LOG.warn("Pass chained called more than 20 times on " + o);
			}
			return o;
		} catch(InterpreterException e) {
			throw new InvalidObjectException("Failed to pass object " + this + ": " + e.getMessage());
		}
	}
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
		
		oos.defaultWriteObject();
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

    public boolean isDefinition() throws InterpreterException {
        return false;
    }
    
    public boolean isMessageCreation() throws InterpreterException {
    	return false;
    }

    public boolean isAmbientTalkObject() { 
    	return false;
    }
    
    public boolean isMirage() {
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
    
	public boolean isNativeNumber() {
		return false;
	}

    public boolean isNativeField() {
        return false;
    }
    
    public boolean isTypeTag() throws InterpreterException {
        return false;
    }
    
	public boolean isNativeFarReference() {
		return false;
	}
	
	public boolean isNativeAbstractGrammar () { return false; };
    
    // Conversions for concurrency and distribution related object
    public boolean isFarReference() throws InterpreterException {
    	return false;
    }
    
    public boolean isNativeFraction() { return false; }
    public boolean isNativeIntrospectiveMirror() { return false; }
	public boolean isJavaClassUnderSymbiosis() { return false; }
	public boolean isJavaMethodUnderSymbiosis() { return false; }
	public boolean isNativeDelegateMethod() { return false; }
	
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
    
    public ATMethodInvocation asMethodInvocation() throws InterpreterException {
        throw new XTypeMismatch(ATMethodInvocation.class, this);
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

    public ATAbstractGrammar asAbstractGrammar() throws InterpreterException {
        return this;
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

    public ATQuote asQuote() throws InterpreterException {
        throw new XTypeMismatch(ATQuote.class, this);
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
    
    public ATLetter asLetter() throws InterpreterException {
    	throw new XTypeMismatch(ATLetter.class, this);
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
    
    public JavaMethod asJavaMethodUnderSymbiosis() throws XTypeMismatch {
	    throw new XTypeMismatch(JavaMethod.class, this);
    }
    
    public DelegateMethod asNativeDelegateMethod() throws XTypeMismatch {
	    throw new XTypeMismatch(DelegateMethod.class, this);
    }
    
    public NATIntrospectiveMirror asNativeIntrospectiveMirror() throws XTypeMismatch {
	    throw new XTypeMismatch(NATIntrospectiveMirror.class, this);
    }
    
    // added for ref pool
    public NATRemoteFarRef asNativeRemoteFarReference() throws XTypeMismatch {
    	throw new XTypeMismatch(NATRemoteFarRef.class, this);
    }
	public NATOutboxLetter asNativeOutboxLetter() throws XTypeMismatch{
    	throw new XTypeMismatch(NATOutboxLetter.class, this);
    }
	
    /**
     * Only true objects have a dynamic pointer, native objects denote 'nil' to
     * be their dynamic parent when asked for it. Note that, for native objects,
     * 'super' is a read-only field (i.e. only an accessor for the virtual field exists).
     */
    public ATObject base_super() throws InterpreterException {
        return Evaluator.getNil();
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
    		}
    	} catch (InterpreterException e) {
    		Logging.Actor_LOG.warn("Error during equality testing:", e);
    	}
    	return false; 
    }
    
    public ATBoolean impl_identityEquals(ATObject other) throws InterpreterException {
    	return NATBoolean.atValue(other == this);
    }
    
	/**
	 * By default, two AmbientTalk objects are equal if they are the
	 * same object, or one is a proxy for the same object.
	 */
    public ATBoolean base__opeql__opeql_(ATObject other) throws InterpreterException {
		// by default, ATObjects use pointer equality
		return NATBoolean.atValue(this == other);
    }
    
    /**
     * Java method invocations of hashCode() are forwarded to
     * AmbientTalk 'hashCode' method invocation if the object is tagged as Hashable
     */
    public int hashCode() {
    	try {
    		if (this.meta_isTaggedAs(NativeTypeTags._HASHABLE_).asNativeBoolean().javaValue) {
    			if (this.meta_respondsTo(NATNil._HASHC_NAME_).asNativeBoolean().javaValue) {
    				try {
    					return this.base_hashCode().asNativeNumber().javaValue;
    				} catch (InterpreterException e) {
    					// something went wrong when computing the hashcode (AT level)
    					// return 0 in an attempt to signal this to the programmer asap
    					Logging.Actor_LOG.error("Error executing base_hashCode:", e);
    					return 0; 
    				}
    			} else {
    				Logging.Actor_LOG.warn("Object is tagged as Hashable but does not implement hashCode, falling back to Java hashCode.");
    				return super.hashCode();
    			}
    		} else {
    			return super.hashCode();
    		}
    	} catch (InterpreterException e) {
    		Logging.Actor_LOG.warn("Error in hashCode (falling back to Java hashcode):", e);
    		return super.hashCode();
    	}
    }
    
    /**
     * By default, the hashcode is the Java hashcode
     */
    public ATNumber base_hashCode() throws InterpreterException {
    	return NATNumber.atValue(super.hashCode());
    }
    
    /*public ATObject base_new(ATObject[] initargs) throws InterpreterException {
    	return this.meta_newInstance(NATTable.atValue(initargs));
    }*/
    
    /*public ATObject base_init(ATObject[] initargs) throws InterpreterException {
    	return Evaluator.getNil();
    }*/
    
	/**
	 * This method is used to evaluate code of the form <tt>selector(args)</tt>
	 * or <tt>selector := arg</tt> within the scope of this object.
	 * 
	 * It dispatches to other implementation-level methods based on the selector.
	 */
	public ATObject impl_call(ATSymbol selector, ATTable arguments) throws InterpreterException {
		if (selector.isAssignmentSymbol()) {
			return this.impl_callMutator(selector.asAssignmentSymbol(), arguments);
		} else {
			return this.impl_callAccessor(selector, arguments);
		}
	}

    /**
	 * Implements the interpretation of <tt>f(arg)</tt> inside the scope of a
	 * particular object.
	 * 
	 * - if f is bound to a local method, the method is applied
	 * - if f is bound to a field:
	 *   - if the field contains a closure, the closure is applied
	 *   - otherwise, the field is treated as a nullary closure and 'applied'
	 * - otherwise, the search for the selector continues in the lexical parent
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
	 * Implements the interpretation of <tt>x := arg</tt> inside the scope of a
	 * particular object.
	 * 
	 * - if x:= is bound to a local method, the method is applied
	 * - if x is bound to a field, the field is assigned if exactly one argument is given
	 * - otherwise, the search for the selector continues in the lexical parent
	 */
	public ATObject impl_callMutator(ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		if(this.hasLocalMethod(selector)) {
			// apply the method with a context ctx where 
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the receiver, being in this case again the implementor
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, this));
		} else {
			ATSymbol fieldSelector = selector.base_fieldName();
			if (this.hasLocalField(fieldSelector)) {
				ATObject value = NativeClosure.checkUnaryArguments(selector, arguments);
				this.setLocalField(fieldSelector, value);
				return value;
			} else {
				return this.impl_lexicalParent().impl_callMutator(selector, arguments);
			}
		}
	}	
	
    /**
	 * Implements the interpretation of <tt>x</tt> inside the scope of a
	 * particular object.
	 * 
	 * - if x is bound to a local method, the method is applied to <tt>[]</tt>
	 * - if x is bound to a field, the field's value is returned (even if it
	 *   contains a closure)
	 * - otherwise, the search for the selector continues in the lexical parent
	 */
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
	 * This method simply deconstructs the method invocation object and passes it on to
	 * the native implementation.
	 */
    public ATObject meta_invoke(ATObject delegate, ATMethodInvocation invocation) throws InterpreterException {
        return this.impl_invoke(delegate, invocation.base_selector(), invocation.base_arguments());
    }
    
	/**
	 * This method dispatches to specific invocation primitives
	 * depending on whether or not the given selector denotes an assignment.
	 */
    public ATObject impl_invoke(ATObject delegate, ATSymbol selector, ATTable arguments) throws InterpreterException {
        // If the selector is an assignment symbol (i.e. `field:=) try to assign the corresponding field
		if (selector.isAssignmentSymbol()) {
			return this.impl_invokeMutator(delegate, selector.asAssignmentSymbol(), arguments);
		} else {
			return this.impl_invokeAccessor(delegate, selector, arguments);
		}
    }
    
    /**
	 * Implements the interpretation of <tt>o.m(arg)</tt>.
	 * 
	 * - if m is bound to a local method of o, the method is applied
	 * - if m is bound to a field of o:
	 *   - if the field contains a closure, the closure is applied
	 *   - otherwise, the field is treated as a nullary closure and 'applied'
	 * - otherwise, the search for the selector continues in the dynamic parent
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
	 * Implements the interpretation of <tt>o.x := arg</tt>.
	 * 
	 * - if x:= is bound to a local method of o, the method is applied to the arguments.
	 * - if x is bound to a field of o, the field is treated as a unary mutator method
	 *   that assigns the field and 'applied' to the given arguments.
	 * - otherwise, the search for the selector continues in the dynamic parent
	 */
	public ATObject impl_invokeMutator(ATObject receiver, ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// immediately execute the method in the context ctx where
			//  ctx.scope = the implementing scope, being this object, under which an additional callframe will be inserted
			//  ctx.self  = the late bound receiver, being the passed receiver
			return this.getLocalMethod(selector).base_apply(arguments, new NATContext(this, receiver));
		} else {
			// try to treat a local field as a mutator
			ATSymbol fieldSelector = selector.base_fieldName();
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

    /**
	 * Implements the interpretation of <tt>o.x</tt>.
	 * 
	 * - if x is bound to a local method of o, the method is applied to <tt>[]</tt>
	 * - if x is bound to a field of o, the field's value is returned (even if it
	 *   contains a closure).
	 * - otherwise, the search for the selector continues in the dynamic parent
	 */
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
	
    /**
	 * This operation dispatches to more specific implementation-level methods depending
	 * on the type of the selector.
	 */
	public ATClosure impl_lookup(ATSymbol selector) throws InterpreterException {
		if (selector.isAssignmentSymbol()) {
			return this.impl_lookupMutator(selector.asAssignmentSymbol());
		} else {
			return this.impl_lookupAccessor(selector);
		}
	}
	
    /**
	 * Implements the interpretation of <tt>&f</tt> in the scope of this object.
	 * 
	 * - if f is bound to a local method, a closure wrapping the method is returned
	 * - if f is bound to a field of o, an accessor closure is returned which yields
	 * the field's value upon application.
	 * - otherwise, the search for the selector continues in the lexical parent
	 */
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
					protected ATObject access() throws InterpreterException {
						return scope.getLocalField(selector);
					}
				};
			} else {
				return this.impl_lexicalParent().impl_lookupAccessor(selector);
			}
		}
	}

    /**
	 * Implements the interpretation of <tt>&f:=</tt> in the scope of this object.
	 * 
	 * - if f:= is bound to a local method, a closure wrapping the method is returned
	 * - if f is bound to a field of o, a mutator closure is returned which assigns
	 * the field upon application.
	 * - otherwise, the search for the selector continues in the lexical parent
	 */
	public ATClosure impl_lookupMutator(ATAssignmentSymbol selector) throws InterpreterException {
		if (this.hasLocalMethod(selector)) {
			// return a new closure (mth, ctx) where
			//  mth = the method found in this object
			//  ctx.scope = the implementing scope, being this object
			//  ctx.self  = the late bound receiver, being the passed receiver
			//  ctx.super = the parent of the implementor
			return this.getLocalMethod(selector).base_wrap(this, this);
		} else {
			final ATSymbol fieldSelector = selector.base_fieldName();
			// try to wrap a local field in a mutator
			if(this.hasLocalField(fieldSelector)) {
				final NativeATObject scope = this;
				return new NativeClosure.Mutator(selector, this) {
					protected ATObject mutate(ATObject arg) throws InterpreterException {
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
	 * Implements the interpretation of <tt>o.&m</tt>.
	 * 
	 * - if m is bound to a local method, a closure wrapping the method is returned
	 * - if m is bound to a field of o, an accessor closure is returned which yields
	 * the field's value upon application.
	 * - otherwise, the search for the selector continues in the dynamic parent
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
    				protected ATObject access() throws InterpreterException {
    					return scope.getLocalField(selector);
    				}
    			};
    		} else {
    			return base_super().impl_selectAccessor(receiver, selector);
    		}
    	}
    }

    /**
	 * Implements the interpretation of <tt>o.&m:=</tt>.
	 * 
	 * - if m:= is bound to a local method, a closure wrapping the method is returned
	 * - if m is bound to a field of o, a mutator closure is returned which assigns
	 * the field upon application.
	 * - otherwise, the search for the selector continues in the dynamic parent
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
    		final ATSymbol fieldSelector = selector.base_fieldName();
    		if (this.hasLocalField(fieldSelector)) {
    			final NativeATObject scope = this;
    			return new NativeClosure.Mutator(selector, this) {
    				protected ATObject mutate(ATObject arg) throws InterpreterException {
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
    
	public ATTable base_freeVariables() throws InterpreterException {
		Set freeVars = this.impl_freeVariables();
		ATSymbol[] tbl = (ATSymbol[]) freeVars.toArray(new ATSymbol[freeVars.size()]);
		return NATTable.atValue(tbl);
	}
    
	/** FV(nativeObj) = { } */
    public Set impl_freeVariables() throws InterpreterException { return new HashSet(); }
	
	public Set impl_quotedFreeVariables() throws InterpreterException { return new HashSet(); }
    
    
    // The following methods are useful for a debugger.
	// They allow the attachment of source location to arbitrary objects
	// (AST nodes descendant from {@link NATAbstractGrammar} as well as
	// other objects, e.g. NATMessage and NATMethod)
    
    public SourceLocation impl_getLocation() { return null; }
    public void impl_setLocation(SourceLocation loc) {}
    public SourceLocation impl_getSourceOf(ATSymbol sel) throws InterpreterException {
      ATClosure slot = this.meta_select(this, sel);
      return (slot == null) ? null : slot.impl_getLocation();	
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
