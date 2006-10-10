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
import edu.vub.at.objects.mirrors.JavaField;
import edu.vub.at.objects.mirrors.JavaInterfaceAdaptor;
import edu.vub.at.objects.mirrors.Reflection;

/**
 * @author smostinc
 *
 * NATNil implements default semantics for all test and conversion methods.
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
    public ATNil meta_send(ATAsyncMessage message) throws NATException {
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
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws NATException {
        try {
			String jSelector = Reflection.upBaseLevelSelector(atSelector);
			return Reflection.downObject(Reflection.upInvocation(receiver, jSelector, arguments));
		} catch (XSelectorNotFound e) {
			return receiver.meta_doesNotUnderstand(atSelector);
		}
    }

    /**
     * An ambienttalk language value can respond to a message if it implements
     * a native Java method corresponding to the selector prefixed by 'base_'.
     */
    public ATBoolean meta_respondsTo(ATSymbol atSelector) throws NATException {
        String jSelector = Reflection.upBaseLevelSelector(atSelector);

        return NATBoolean.atValue(Reflection.upRespondsTo(this, jSelector));
    }

    /**
     * By default, when a selection is not understood by a primitive object, an error is raised.
     */
    public ATObject meta_doesNotUnderstand(ATSymbol selector) throws NATException {
        throw new XSelectorNotFound(selector, this);
    }

    /* ------------------------------------------
      * -- Slot accessing and mutating protocol --
      * ------------------------------------------ */

    /**
     * It is possible to select a method from any ambienttalk value provided that it
     * offers the method in its provided interface. The result is a JavaMethod wrapper
     * which encapsulates the reflective Method object as well as the receiver.
     * 
     * There exists a certain ambiguity in field selection on AmbientTalk implementation-level objects.
     * When nativeObject.m is evaluated, the corresponding Java class must have a method named either
     *  base_getM which means m is represented as a readable field, or
     *  base_m which means m is represented as a method
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
        String jSelector = null;

        try {
        	   jSelector = Reflection.upBaseFieldAccessSelector(selector);
            return Reflection.downObject(Reflection.upFieldSelection(receiver, jSelector));
        } catch (XSelectorNotFound e) {
            jSelector = Reflection.upBaseLevelSelector(selector);

            try {
				return Reflection.downObject(Reflection.upMethodSelection(receiver, jSelector));
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
    public ATObject meta_lookup(ATSymbol selector) throws NATException {
        try {
        	  return this.meta_select(this, selector);
        } catch(XSelectorNotFound e) {
        	  // transform selector not found in undefined variable access
        	  throw new XUndefinedField("variable access", selector.base_getText().asNativeText().javaValue);
        }
    }

    public ATNil meta_defineField(ATSymbol name, ATObject value) throws NATException {
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
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws NATException {
        try {
			return this.meta_assignField(this, name, value);
		} catch (XSelectorNotFound e) {
			// transform selector not found in undefined variable assignment
			throw new XUndefinedField("variable assignment", name.base_getText().asNativeText().javaValue);
		}
    }

    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws NATException {
    	
        // try to invoke a native base_setName method
        try {
        	   String jSelector = Reflection.upBaseFieldMutationSelector(name);
        	   Reflection.upFieldAssignment(this, jSelector, value);
		} catch (XSelectorNotFound e) {
			// if such a method does not exist, the field assignment has failed
			throw new XUndefinedField("field assignment", name.base_getText().asNativeText().javaValue);
		}
		
        return NATNil._INSTANCE_;
    }

    /* ------------------------------------
      * -- Extension and cloning protocol --
      * ------------------------------------ */

    public ATObject meta_clone() throws NATException {
        throw new XIllegalOperation("Cannot clone a native object of type " + this.getClass().getName());
    }

    public ATObject meta_newInstance(ATTable initargs) throws NATException {
        return Reflection.downObject(Reflection.upInstanceCreation(this, initargs));
    }

	protected ATObject createChild(ATClosure code, boolean parentPointerType) throws NATException {

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
	
	public ATObject meta_extend(ATClosure code) throws NATException {
		return createChild(code, NATObject._IS_A_);
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		return createChild(code, NATObject._SHARES_A_);
	}
    
    /* ---------------------------------
      * -- Structural Access Protocol  --
      * --------------------------------- */

    // TODO field access translated into getter/setter method invocations

    public ATNil meta_addField(ATField field) throws NATException {
        throw new XIllegalOperation("Cannot add fields to an object of type " + this.getClass().getName());
    }

    public ATNil meta_addMethod(ATMethod method) throws NATException {
        throw new XIllegalOperation("Cannot add methods to an object of type " + this.getClass().getName());
    }

    public ATField meta_getField(ATSymbol fieldName) throws NATException {
        return JavaField.createPrimitiveField(this, fieldName);
    }

    public ATMethod meta_getMethod(ATSymbol methodName) throws NATException {
        String selector = Reflection.upBaseLevelSelector(methodName);

        return JavaInterfaceAdaptor.wrapMethodFor(
                this.getClass(),
                this,
                selector).base_getMethod();
    }

    public ATTable meta_listFields() throws NATException {
        // TODO do we show all base_get methods here?
        return NATTable.EMPTY;
    }

    public ATTable meta_listMethods() throws NATException {
        // TODO filter out all base_get and show all base_?
        return NATTable.EMPTY;
    }

    /* ---------------------------------
      * -- Abstract Grammar Protocol   --
      * --------------------------------- */

    /**
     * All NATObjects which are not Abstract Grammar elements are self-evaluating.
     */
    public ATObject meta_eval(ATContext ctx) throws NATException {
        return this;
    }

    /**
     * Quoting a native object returns itself, except for pure AG elements.
     */
    public ATObject meta_quote(ATContext ctx) throws NATException {
        return this;
    }

    public NATText meta_print() throws NATException {
        return NATText.atValue("nil");
    }

    /* ------------------------------
      * -- ATObject Mirror Fields   --
      * ------------------------------ */

    /**
     * Only true extending objects have a dynamic pointer, others return nil
     * @throws NATException 
     */
    public ATObject meta_getDynamicParent() throws NATException {
        return NATNil._INSTANCE_;
    };

    /**
     * By default numbers, tables and so on do not have lexical parents,
     */
    public ATObject meta_getLexicalParent() throws NATException {
        return NATNil._INSTANCE_;
    }


    /* ---------------------------------
      * -- Value Conversion Protocol   --
      * --------------------------------- */

    public boolean isClosure() {
        return false;
    }

    public boolean isSymbol() {
        return false;
    }

    public boolean isBoolean() {
        return false;
    }

    public boolean isTable() {
        return false;
    }

    public boolean isCallFrame() {
        return false;
    }

    public boolean isUnquoteSplice() {
        return false;
    }

    public boolean isSplice() {
        return false;
    }

    public boolean isMethod() {
        return false;
    }
    
    public boolean isMessageCreation() {
    	    return false;
    }

    public boolean isAmbientTalkObject() { 
    	    return false;
    }
    
    public ATBoolean base_isMirror() {
        return NATBoolean._FALSE_;
    }

    public boolean isNativeBoolean() {
        return false;
    }
    
    public boolean isNativeText() {
        return false;
    }

    public ATClosure asClosure() throws XTypeMismatch {
        throw new XTypeMismatch(ATClosure.class, this);
    }

    public ATSymbol asSymbol() throws XTypeMismatch {
        throw new XTypeMismatch(ATSymbol.class, this);
    }

    public ATTable asTable() throws XTypeMismatch {
        throw new XTypeMismatch(ATTable.class, this);
    }

    public ATBoolean asBoolean() throws XTypeMismatch {
        throw new XTypeMismatch(ATBoolean.class, this);
    }

    public ATNumber asNumber() throws XTypeMismatch {
        throw new XTypeMismatch(ATNumber.class, this);
    }

    public ATMessage asMessage() throws XTypeMismatch {
        throw new XTypeMismatch(ATMessage.class, this);
    }

    public ATField asField() throws XTypeMismatch {
        throw new XTypeMismatch(ATField.class, this);
    }

    public ATMethod asMethod() throws XTypeMismatch {
        throw new XTypeMismatch(ATMethod.class, this);
    }

    public ATMirror asMirror() throws XTypeMismatch {
        throw new XTypeMismatch(ATMirror.class, this);
    }

    // Conversions for abstract grammar elements

    public ATStatement asStatement() throws XTypeMismatch {
        throw new XTypeMismatch(ATStatement.class, this);
    }

    public ATDefinition asDefinition() throws XTypeMismatch {
        throw new XTypeMismatch(ATDefinition.class, this);
    }

    public ATExpression asExpression() throws XTypeMismatch {
        throw new XTypeMismatch(ATExpression.class, this);
    }

    public ATBegin asBegin() throws XTypeMismatch {
        throw new XTypeMismatch(ATBegin.class, this);
    }

    public ATMessageCreation asMessageCreation() throws XTypeMismatch {
        throw new XTypeMismatch(ATMessageCreation.class, this);
    }

    public ATUnquoteSplice asUnquoteSplice() throws XTypeMismatch {
        throw new XTypeMismatch(ATUnquoteSplice.class, this);
    }

    public ATSplice asSplice() throws XTypeMismatch {
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

    public NATText asNativeText() throws NATException {
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

    public NATException asNativeException() throws XTypeMismatch {
    		return new XUserDefined(this);
    }
    
    public String toString() {
        return Evaluator.toString(this);
    }

    public ATBoolean base__opeql__opeql_(ATObject comparand) {
        return NATBoolean.atValue(this.equals(comparand));
    }
    
    public ATObject base_new(ATObject[] initargs) throws NATException {
    	    return this.meta_newInstance(NATTable.atValue(initargs));
    }
    
    public ATObject base_init(ATObject[] initargs) throws NATException {
    	    return NATNil._INSTANCE_;
    }

	public ATBoolean meta_isCloneOf(ATObject original) throws NATException {
		return NATBoolean.atValue(
				this.getClass() == original.getClass());
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws NATException {
		return this.meta_isCloneOf(object);
	}

}
