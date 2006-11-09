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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.exceptions.XUserDefined;
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
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaObject;

/**
 * NATNil implements default semantics for all test and conversion methods.
 *
 * @author smostinc
 */
public class NATNil implements ATNil {
	
    protected NATNil() {};

    public final static NATNil _INSTANCE_ = new NATNil();

    /* ------------------------------
      * -- Message Sending Protocol --
      * ------------------------------ */

    /**
     * Asynchronous messages sent to an object ( o<-m( args )) are handled by the
     * actor in which the object is contained.
     */
    public ATNil meta_send(ATAsyncMessage message) throws InterpreterException {
         // TODO: nil <- m() => also do invoke-like deification?
        throw new RuntimeException("Not yet implemented: async message sends to native values");
    }

    /**
     * The default behaviour of meta_invoke for primitive non-object ambienttalk language values is
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
			return Reflection.upInvocation(receiver, jSelector, arguments);
		} catch (XSelectorNotFound e) {
			return receiver.meta_doesNotUnderstand(atSelector).base_asClosure().base_apply(arguments);
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
        String jSelector = null;

        try {
        	   jSelector = Reflection.upBaseFieldAccessSelector(selector);
            return Reflection.upFieldSelection(receiver, jSelector);
        } catch (XSelectorNotFound e) {
            jSelector = Reflection.upBaseLevelSelector(selector);

            try {
				return Reflection.upMethodSelection(receiver, jSelector, selector);
			} catch (XSelectorNotFound e2) {
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
        throw new XIllegalOperation("Cannot add fields to a sealed " + this.getClass().getName());
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
        try {
			return this.meta_assignField(this, name, value);
		} catch (XUndefinedField e) {
			// transform selector not found in undefined variable assignment
			throw new XUndefinedField("variable assignment", name.base_getText().asNativeText().javaValue);
		}
    }

    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
    	
        // try to invoke a native base_setName method
        try {
        	   String jSelector = Reflection.upBaseFieldMutationSelector(name);
        	   Reflection.upFieldAssignment(receiver, jSelector, value);
		} catch (XSelectorNotFound e) {
			// if such a method does not exist, the field assignment has failed
			throw new XUndefinedField("field assignment", name.base_getText().asNativeText().javaValue);
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

	protected ATObject createChild(ATClosure code, boolean parentPointerType) throws InterpreterException {

		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.base_getContext().base_getLexicalScope(),
				/* parent porinter type */
				parentPointerType);
			
		code.base_getMethod().base_apply(NATTable.EMPTY, new NATContext(extension, extension, this));
			
		return extension;
	}
	
	public ATObject meta_extend(ATClosure code) throws InterpreterException {
		return createChild(code, NATObject._IS_A_);
	}

	public ATObject meta_share(ATClosure code) throws InterpreterException {
		return createChild(code, NATObject._SHARES_A_);
	}
    
    /* ---------------------------------
      * -- Structural Access Protocol  --
      * --------------------------------- */

    public ATNil meta_addField(ATField field) throws InterpreterException {
        throw new XIllegalOperation("Cannot add fields to an object of type " + this.getClass().getName());
    }

    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        throw new XIllegalOperation("Cannot add methods to an object of type " + this.getClass().getName());
    }

    public ATField meta_grabField(ATSymbol fieldName) throws InterpreterException {
        return Reflection.downBaseLevelField(this, fieldName);
    }

    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        return Reflection.downBaseLevelMethod(this, methodName);
    }

    public ATTable meta_listFields() throws InterpreterException {
        return new NATTable(Reflection.downBaseLevelFields(this));
    }

    public ATTable meta_listMethods() throws InterpreterException {
    	    return new NATTable(Reflection.downBaseLevelMethods(this));
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
     * Only true extending objects have a dynamic pointer, others return nil
     * @throws InterpreterException 
     */
    public ATObject meta_getDynamicParent() throws InterpreterException {
        return NATNil._INSTANCE_;
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

    public boolean base_isClosure() {
        return false;
    }

    public boolean base_isSymbol() {
        return false;
    }

    public boolean base_isBoolean() {
        return false;
    }

    public boolean base_isTable() {
        return false;
    }

    public boolean base_isCallFrame() {
        return false;
    }

    public boolean base_isUnquoteSplice() {
        return false;
    }

    public boolean base_isSplice() {
        return false;
    }

    public boolean base_isMethod() {
        return false;
    }
    
    public boolean base_isMessageCreation() {
    	    return false;
    }

    public boolean isAmbientTalkObject() { 
    	    return false;
    }
    
    public boolean isJavaObjectUnderSymbiosis() {
    	    return false;
    }
    
    public boolean base_isMirror() {
        return false;
    }

    public boolean isNativeBoolean() {
        return false;
    }
    
    public boolean isNativeText() {
        return false;
    }

    public ATClosure base_asClosure() throws XTypeMismatch {
        throw new XTypeMismatch(ATClosure.class, this);
    }

    public ATSymbol base_asSymbol() throws XTypeMismatch {
        throw new XTypeMismatch(ATSymbol.class, this);
    }

    public ATTable base_asTable() throws XTypeMismatch {
        throw new XTypeMismatch(ATTable.class, this);
    }

    public ATBoolean base_asBoolean() throws XTypeMismatch {
        throw new XTypeMismatch(ATBoolean.class, this);
    }

    public ATNumber base_asNumber() throws XTypeMismatch {
        throw new XTypeMismatch(ATNumber.class, this);
    }

    public ATMessage base_asMessage() throws XTypeMismatch {
        throw new XTypeMismatch(ATMessage.class, this);
    }

    public ATField base_asField() throws XTypeMismatch {
        throw new XTypeMismatch(ATField.class, this);
    }

    public ATMethod base_asMethod() throws XTypeMismatch {
        throw new XTypeMismatch(ATMethod.class, this);
    }

    public ATMirror base_asMirror() throws XTypeMismatch {
        throw new XTypeMismatch(ATMirror.class, this);
    }
    
    public ATHandler base_asHandler() throws XTypeMismatch {
    	    throw new XTypeMismatch(ATHandler.class, this);
    }

    // Conversions for abstract grammar elements

    public ATStatement base_asStatement() throws XTypeMismatch {
        throw new XTypeMismatch(ATStatement.class, this);
    }

    public ATDefinition base_asDefinition() throws XTypeMismatch {
        throw new XTypeMismatch(ATDefinition.class, this);
    }

    public ATExpression base_asExpression() throws XTypeMismatch {
        throw new XTypeMismatch(ATExpression.class, this);
    }

    public ATBegin base_asBegin() throws XTypeMismatch {
        throw new XTypeMismatch(ATBegin.class, this);
    }

    public ATMessageCreation base_asMessageCreation() throws XTypeMismatch {
        throw new XTypeMismatch(ATMessageCreation.class, this);
    }

    public ATUnquoteSplice base_asUnquoteSplice() throws XTypeMismatch {
        throw new XTypeMismatch(ATUnquoteSplice.class, this);
    }

    public ATSplice base_asSplice() throws XTypeMismatch {
        throw new XTypeMismatch(ATSplice.class, this);
    }
    
    // Conversions for native values
    public NATObject asAmbientTalkObject() throws XTypeMismatch {
    	    throw new XTypeMismatch(NATObject.class, this);
    	}

    public NATNumber asNativeNumber() throws XTypeMismatch {
        throw new XTypeMismatch(NATNumber.class, this);
    }

    public NATFraction asNativeFraction() throws XTypeMismatch {
        throw new XTypeMismatch(NATFraction.class, this);
    }

    public NATText asNativeText() throws InterpreterException {
    		return this.meta_print();
        // throw new XTypeMismatch("Expected a native text, given: " + this.getClass().getName(), this);
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

    public ATBoolean base__opeql__opeql_(ATObject comparand) {
        return NATBoolean.atValue(this.equals(comparand));
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

}
