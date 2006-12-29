/**
 * AmbientTalk/2 Project
 * NATSuperObject.java created on Jul 27, 2006 at 4:37:25 PM
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
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * NATSuperObject is a decorator class for an ATObject when denoted using the super
 * pseudovariable. It is parameterised by two objects, namely the super object in which
 * method lookup starts (lookupFrame_) and the late-bound receiver.
 * 
 * @author smostinc
 */
public class NATSuperObject extends NATByRef implements ATObject {

    private final ATObject receiver_;
    private final ATObject lookupFrame_;

    public NATSuperObject(ATObject receiver, ATObject lookupFrame) {
        receiver_ = receiver;
        lookupFrame_ = lookupFrame;
    }

    /* ------------------------------
      * -- Message Sending Protocol --
      * ------------------------------ */

    // TODO(send) Ensuring correct lookup and self bindings for send
    public ATObject meta_send(ATAsyncMessage msg) throws InterpreterException {
        return lookupFrame_.meta_send(msg);
    }

    /**
     * Invocation is treated differently for a SuperObject reference. While the method
     * lookup will start in the encapsulated lookupFrame_, the receiver will not be bound
     * to 'lookupFrame_', as would normally be the case, but to receiver_, implementing
     * 'late binding of self'.
     * TODO: equality test interception: can we rework this? make it more general, make it cleaner?
     * what should be the semantics of doing super == obj or obj == super?
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
    		if(selector == AGSymbol.jAlloc("==")) // intercept equality tests on this object
    			return super.meta_invoke(this, selector, arguments);
    		return lookupFrame_.meta_invoke(receiver_, selector, arguments);
    }

    public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
        return lookupFrame_.meta_respondsTo(selector);
    }

    public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
        return lookupFrame_.meta_doesNotUnderstand(selector);
    }

    /* ------------------------------------------
      * -- Slot accessing and mutating protocol --
      * ------------------------------------------ */

    /**
     * Selection is treated differently for a SuperObject reference. While the dynamic
     * lookup will start in the encapsulated lookupFrame_, the receiver will not be bound
     * to 'lookupFrame_', as would normally be the case, but to receiver_, implementing
     * 'late binding of self'.
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
        return lookupFrame_.meta_select(receiver_, selector);
    }

    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
        return lookupFrame_.meta_lookup(selector);
    }

    public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
        return lookupFrame_.meta_defineField(name, value);
    }

    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
        return lookupFrame_.meta_assignVariable(name, value);
    }

    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
        return lookupFrame_.meta_assignField(receiver_, name, value);
    }

    /* ------------------------------------
      * -- Extension and cloning protocol --
      * ------------------------------------ */

    public ATObject meta_clone() throws InterpreterException {
        return lookupFrame_.meta_clone();
    }

    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
        return lookupFrame_.meta_newInstance(initargs);
    }

    public ATObject meta_extend(ATClosure code) throws InterpreterException {
        return lookupFrame_.meta_extend(code);
    }

    public ATObject meta_share(ATClosure code) throws InterpreterException {
        return lookupFrame_.meta_share(code);
    }

    /* ---------------------------------
      * -- Structural Access Protocol  --
      * --------------------------------- */

    public ATNil meta_addField(ATField field) throws InterpreterException {
        return lookupFrame_.meta_addField(field);
    }

    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        return lookupFrame_.meta_addMethod(method);
    }

    public ATField meta_grabField(ATSymbol selector) throws InterpreterException {
        return lookupFrame_.meta_grabField(selector);
    }

    public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException {
        return lookupFrame_.meta_grabMethod(selector);
    }

    public ATTable meta_listFields() throws InterpreterException {
        return lookupFrame_.meta_listFields();
    }

    public ATTable meta_listMethods() throws InterpreterException {
        return lookupFrame_.meta_listMethods();
    }

    /* ---------------------
      * -- Mirror Fields   --
      * --------------------- */

    public ATObject meta_getDynamicParent() throws InterpreterException {
        return lookupFrame_.meta_getDynamicParent();
    }

    public ATObject meta_getLexicalParent() throws InterpreterException {
        return lookupFrame_.meta_getLexicalParent();
    }

    /* ---------------------------------
      * -- Accessors for JUnit tests   --
      * --------------------------------- */

    public ATObject getLookupFrame() {
        return this.lookupFrame_;
    }

    public ATObject getReceiver() {
        return this.receiver_;
    }

    /* -------------------------
      * -- Evaluation Protocol --
      * ------------------------- */

    public NATText meta_print() throws InterpreterException {
        return lookupFrame_.meta_print();
    }

	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return lookupFrame_.meta_isCloneOf(original);
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return lookupFrame_.meta_isRelatedTo(object);
	}
	
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */
    
	/**
	 * Normally, references to super are passed by reference, but if both
	 * the parent and child are isolates, the super reference may be passed
	 * by copy.
	 */
	public ATObject meta_pass() throws InterpreterException {
		if ((receiver_ instanceof NATIsolate) && (lookupFrame_ instanceof NATIsolate)) {
			return this;
		} else {
			return super.meta_pass();
		}
	}
	
	/**
	 * If super is passed by copy because it connects two isolates, return
	 * this super object itself upon deserialization.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		if ((receiver_ instanceof NATIsolate) && (lookupFrame_ instanceof NATIsolate)) {
			return this;
		} else {
			return super.meta_resolve();
		}
	}

}
