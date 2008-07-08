/**
 * AmbientTalk/2 Project
 * OBJMirrorRoot.java created on Oct 3, 2006 at 3:26:08 PM
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.util.logging.Logging;

import java.io.IOException;

/**
 * This class denotes the root node of the intercessive mirrors delegation hierarchy.
 * 
 * Intercessive mirrors are always tied to a particular 'base' object.
 * The default intercessive mirror is named 'mirrorroot' and is an object
 * that understands all meta_* operations, implementing them using default semantics.
 * It can be thought of as being defined as follows:
 * 
 * def mirrorroot := object: {
 *   def base := object: { nil } mirroredBy: self // base of the mirror root is an empty mirage
 *   def init(b) {
 *     base := b
 *   }
 *   def invoke(@args) { <default native invocation behaviour on base> }
 *   def select(@args) { <default native selection behaviour on base> }
 *   ...
 * } taggedAs: [ Mirror ]
 * 
 * This object can then simply be extended / composed by other objects to deviate from the default semantics.
 * Note that the default semantics is applied to 'base' and *not* 'self.base', in other words:
 * although child mirrors can define their own 'base' field, it is not taken into consideration
 * by the mirror root. This also ensures that the mirror root is not abused to enact upon a mirage
 * for which it was not assigned to be the mirror.
 * 
 * Hence, 'mirrors' are simply objects with the same interface as this mirrorroot object: they should be
 * able to respond to all meta_* messages and have a 'base' field.
 * 
 * @author tvcutsem, smostinc
 */
public final class NATMirrorRoot extends NATByCopy implements ATObject {
	
	// The name of the field that points to the base_level representation of a custom mirror
	public static final AGSymbol _BASE_NAME_ = AGSymbol.jAlloc("base");
	
	// the native read-only 'base' field of the mirror root
	private NATMirage base_;
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			in.defaultReadObject();
		} catch(IOException e) {
			Logging.Actor_LOG.fatal("Failed to reconstruct an OBJMirrorRoot", e);
			throw e;
		}
	}
	
	/**
	 * Constructor used to initialize the initial mirror root prototype.
	 */
	public NATMirrorRoot() {
		base_ = new NATMirage(this);
	};

	/**
	 * Constructor used for cloning: creates a shallow copy of the mirror root.
	 * @param base the base field value of the original mirror root from which
	 * this new one will be cloned.
	 */
	private NATMirrorRoot(NATMirage base) {
		base_ = base;
	};

	/**
	 * OBJMirrorRoot's primitive 'init method, in pseudo-code:
	 * 
	 * def init(newBase) {
	 *   base := newBase
	 * }
	 */
	public ATObject base_init(ATObject[] initargs) throws InterpreterException {
		if (initargs.length != 1) {
			throw new XArityMismatch("init", 1, initargs.length);
		}
		
		NATMirage newBase = initargs[0].asMirage();
		// check whether the passed base field does not have a mirror assigned to it yet
		if (newBase.getMirror() == Evaluator.getNil()) {
			base_ = newBase;
			return newBase;
		} else {
			throw new XIllegalArgument("mirror root's init method requires an uninitialized mirage, found: " + newBase);
		}
	}

	/**
	 * This implementation is actually an ad hoc modification of the NATObject implementation
	 * of instance creation, dedicated for the mirror root. Using the NATObject implementation
	 * would work perfectly, but this one is more efficient.
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		ATObject[] initargz = initargs.asNativeTable().elements_;
		if (initargz.length != 1) {
			throw new XArityMismatch("newInstance", 1, initargz.length);
		}
		NATMirage newBase = initargz[0].asMirage();
		// check whether the passed base field does not have a mirror assigned to it yet
		if (newBase.getMirror() == Evaluator.getNil()) {
			return new NATMirrorRoot(newBase);
		} else {
			throw new XIllegalArgument("mirror root's init method requires an uninitialized mirage, found: " + newBase);
		}
	}
	
	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */
	
	/**
	 * The mirror root is cloned but the base field is only shallow-copied, i.e. it is shared
	 * between the clones! Normally, mirrors are instantiated rather than cloned when assigned
	 * to a new object, such that this new base field will be re-assigned to another mirage
	 * (in {@link NATMirrorRoot#base_init(ATObject[])}).
	 */
	public ATObject meta_clone() throws InterpreterException {
		return new NATMirrorRoot(base_);
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._MIRROR_);
    }
    
    public NATText meta_print() throws InterpreterException {
    	return NATText.atValue("<mirror on: "+base_+">");
    }
    
    public ATObject meta_pass() throws InterpreterException {
    	if ((base_.meta_isTaggedAs(NativeTypeTags._ISOLATE_)).asNativeBoolean().javaValue) {//.atValue()isFlagSet(_IS_ISOLATE_FLAG_)) {
    		return this;
    	} else {
    		return super.meta_pass();
    	}
    }
	/**
	 * The read-only field containing the mirror's base-level mirage.
	 */
	public NATMirage base_base() throws InterpreterException {
		return base_;
	}
	
    
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */

	/*
	 * <p>The effect of selecting fields or methods on a mirror (through meta_select) 
	 * consists of checking whether the requested selector matches a field of the 
	 * principal wrapped by this mirror. If this is the case, the principal's 
	 * ('meta_get' + selector) method will be invoked. Else the selector might 
	 * identify one of the principal's meta-operations. If this is the case, then
	 * an AmbientTalk representation of the Java method ('meta_' + selector) will 
	 * be returned. </p>
	 *  
	 * <p>Because an explicit AmbientTalk method invocation must be converted into 
	 * an implicit Java method invocation, the invocation must be deified ('upped').
	 * To uphold stratification of the mirror architecture, the result of this 
	 * operation should be a mirror on the result of the Java method invocation.</p>
	 * 
	 * <p>Note that only when the principal does not have a matching meta_level field 
	 * or method the mirror itself will be tested for a corresponding base_level 
	 * behaviour (e.g. for its base field or for operators such as ==). In the 
	 * latter case, stratification is not enforced. This is due to the fact that 
	 * the said fields and methods are not meta-level behaviour, rather they are 
	 * base-level operations which happen to be applicable on a mirror. An added 
	 * advantage of this technique is that it permits a mirror to have a field 
	 * referring to its principal.</p>
	 */
	
	/* ========================================================================
	 * OBJMirrorRoot has a base_x method for each meta_x method defined in ATObject.
	 * Each base_x method invokes NATObject's default behaviour on the base_ NATMirage
	 * via that mirage's magic_x methods.
	 * ======================================================================== */

	public ATObject base_clone() throws InterpreterException {
		return base_base().magic_clone();
	}
	
    public ATTable base_typeTags() throws InterpreterException {
		return base_base().magic_typeTags();
    }
    
    public NATText base_print() throws InterpreterException {
		return base_base().magic_print();
    }
	
	public ATObject base_pass() throws InterpreterException {
		return base_base().magic_pass();
	}

	public ATObject base_resolve() throws InterpreterException {
		return base_base().magic_resolve();
	}

	public ATNil base_addField(ATField field) throws InterpreterException {
		return base_base().magic_addField(field);
	}

	public ATNil base_addMethod(ATMethod method) throws InterpreterException {
		return base_base().magic_addMethod(method);
	}
	
	public ATNil base_addSlot(ATMethod slot) throws InterpreterException {
		return base_base().magic_addSlot(slot);
	}

	public ATNil base_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		return base_base().magic_defineField(name, value);
	}

	public ATObject base_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		return base_base().magic_doesNotUnderstand(selector);
	}

	public ATObject base_eval(ATContext ctx) throws InterpreterException {
		return base_base().magic_eval(ctx);
	}

	public ATBoolean base_isExtensionOfParent() throws InterpreterException {
		return base_base().magic_isExtensionOfParent();
	}

	public ATObject base_invokeField(ATObject rcv, ATSymbol sym) throws InterpreterException {
		return base_base().magic_invokeField(rcv, sym);
	}

	public ATField base_grabField(ATSymbol fieldName) throws InterpreterException {
		return base_base().magic_grabField(fieldName);
	}

	public ATMethod base_grabMethod(ATSymbol methodName) throws InterpreterException {
		return base_base().magic_grabMethod(methodName);
	}
	
	public ATMethod base_grabSlot(ATSymbol methodName) throws InterpreterException {
		return base_base().magic_grabSlot(methodName);
	}

	public ATObject base_invoke(ATObject receiver, ATMethodInvocation inv) throws InterpreterException {
		return base_base().magic_invoke(receiver, inv);
	}

	public ATBoolean base_isCloneOf(ATObject original) throws InterpreterException {
		return base_base().magic_isCloneOf(original);
	}

	public ATBoolean base_isRelatedTo(ATObject object) throws InterpreterException {
		return base_base().magic_isRelatedTo(object);
	}

	public ATBoolean base_isTaggedAs(ATTypeTag type) throws InterpreterException {
		return base_base().magic_isTaggedAs(type);
	}

	public ATTable base_listFields() throws InterpreterException {
		return base_base().magic_listFields();
	}

	public ATTable base_listMethods() throws InterpreterException {
		return base_base().magic_listMethods();
	}
	
	public ATTable base_listSlots() throws InterpreterException {
		return base_base().magic_listSlots();
	}

	public ATObject base_newInstance(ATTable initargs) throws InterpreterException {
		return base_base().magic_newInstance(initargs);
	}

	public ATObject base_quote(ATContext ctx) throws InterpreterException {
		return base_base().magic_quote(ctx);
	}

	public ATObject base_receive(ATAsyncMessage message) throws InterpreterException {
		return base_base().magic_receive(message);
	}

	public ATBoolean base_respondsTo(ATSymbol atSelector) throws InterpreterException {
		return base_base().magic_respondsTo(atSelector);
	}

	public ATObject base_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		return base_base().magic_select(receiver, selector);
	}

	public ATObject base_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException {
		return base_base().magic_send(receiver, message);
	}
	
}
