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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATNumeric;
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
        String jSelector = Reflection.upBaseLevelSelector(atSelector);

	        return Reflection.downObject(Reflection.upInvocation(this, receiver, jSelector, arguments));
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

            return Reflection.downObject(Reflection.upFieldSelection(this, receiver, jSelector));
        } catch (XSelectorNotFound e) {
            jSelector = Reflection.upBaseLevelSelector(selector);

            return Reflection.downObject(Reflection.upMethodSelection(this, receiver, jSelector));
        }
    }

    /**
     * A lookup can only be issued at the base level by writing <tt>selector</tt> inside the scope
     * of a particular object. For primitive language values, this should not happen
     * as no AmbientTalk code can be possibly nested within native code. However, using
     * meta-programming a primitive object could be installed as the lexical parent of an AmbientTalk object.
     *
     * In such cases a lookup is treated exactly like a selection, where the 'original receiver'
     * of the selection equals the primitive object.
     */
    public ATObject meta_lookup(ATSymbol selector) throws NATException {
        return this.meta_select(this, selector);
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
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws NATException {
        return this.meta_assignField(name, value);
    }

    public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException {
        String selector = Reflection.upBaseFieldMutationSelector(name);

        JavaInterfaceAdaptor.invokeJavaMethod(
            this.getClass(),
            this,
            selector,
            new ATObject[] { value });

        return NATNil._INSTANCE_;
    }

    /* ------------------------------------
      * -- Extension and cloning protocol --
      * ------------------------------------ */

    public ATObject meta_clone() throws NATException {
        throw new XIllegalOperation("Cannot clone an object of type " + this.getClass().getName());
    }

    public ATObject meta_new(ATTable initargs) throws NATException {
        return Reflection.downObject(Reflection.upInstanceCreation(this, initargs));
    }

    public ATObject meta_extend(ATClosure code) throws NATException {
        throw new XIllegalOperation("Cannot extend an object of type " + this.getClass().getName());
    }

    public ATObject meta_share(ATClosure code) throws NATException {
        throw new XIllegalOperation("Cannot share an object of type " + this.getClass().getName());
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
                selector).getMethod();
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

    public NATText meta_print() throws XTypeMismatch {
        return NATText.atValue("nil");
    }

    /* ------------------------------
      * -- ATObject Mirror Fields   --
      * ------------------------------ */

    /**
     * Only true extending objects have a dynamic pointer, others return nil
     */
    public ATObject getDynamicParent() {
        return NATNil._INSTANCE_;
    };

    /**
     * By default numbers, tables and so on do not have lexical parents,
     */
    public ATObject getLexicalParent() {
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

    public ATBoolean base_isMirror() {
        return NATBoolean._FALSE_;
    }

    public boolean isNativeBoolean() {
        return false;
    }

    public ATClosure asClosure() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a closure, given: " + this.getClass().getName(), this);
    }

    public ATSymbol asSymbol() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a symbol, given: " + this.getClass().getName(), this);
    }

    public ATTable asTable() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a table, given: " + this.getClass().getName(), this);
    }

    public ATBoolean asBoolean() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a boolean, given: " + this.getClass().getName(), this);
    }

    public ATNumber asNumber() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a number, given: " + this.getClass().getName(), this);
    }

    public ATMessage asMessage() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a first-class message, given: " + this.getClass().getName(), this);
    }

    public ATMethod asMethod() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a first-class method, given: " + this.getClass().getName(), this);
    }

    public ATMirror asMirror() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a mirror object, given: " + this.getClass().getName(), this);
    }

    // Conversions for abstract grammar elements

    public ATStatement asStatement() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a statement, given: " + this.getClass().getName(), this);
    }

    public ATDefinition asDefinition() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a definition, given: " + this.getClass().getName(), this);
    }

    public ATExpression asExpression() throws XTypeMismatch {
        throw new XTypeMismatch("Expected an expression, given: " + this.getClass().getName(), this);
    }

    public ATBegin asBegin() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a begin statement, given: " + this.getClass().getName(), this);
    }

    public ATMessageCreation asMessageCreation() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a first-class message creation, given: " + this.getClass().getName(), this);
    }

    public ATUnquoteSplice asUnquoteSplice() throws XTypeMismatch {
        throw new XTypeMismatch("Expected an unquote-splice abstract grammar, given: " + this.getClass().getName(), this);
    }

    public ATSplice asSplice() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a splice abstract grammar, given: " + this.getClass().getName(), this);
    }

    // Conversions for native values

    public NATNumber asNativeNumber() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a native number, given: " + this.getClass().getName(), this);
    }

    public NATFraction asNativeFraction() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a native fraction, given: " + this.getClass().getName(), this);
    }

    public NATText asNativeText() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a native text, given: " + this.getClass().getName(), this);
    }

    public NATTable asNativeTable() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a native table, given: " + this.getClass().getName(), this);
    }

    public NATBoolean asNativeBoolean() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a native boolean, given: " + this.getClass().getName(), this);
    }
    
    public NATNumeric asNativeNumeric() throws XTypeMismatch {
        throw new XTypeMismatch("Expected a native numeric (number or fraction), given: " + this.getClass().getName(), this);
    }

    public String toString() {
        try {
            return this.getClass().getName() + ": " + this.meta_print().javaValue;
        } catch (XTypeMismatch e) {
            e.printStackTrace();
            return this.getClass().getName() + ": " + e.getMessage();
        }
    }

    public ATObject base__optil_(ATMessage msg) throws NATException {
        return msg.meta_sendTo(this);
    }

    public ATBoolean base__opeql__opeql_(ATObject comparand) {
        return NATBoolean.atValue(this.equals(comparand));
    }

}