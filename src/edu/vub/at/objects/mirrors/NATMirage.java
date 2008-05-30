/**
 * AmbientTalk/2 Project
 * NATMirage.java created on Oct 2, 2006 at 10:08:12 PM
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
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.MethodDictionary;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Vector;

/**
 * A NATMirage is an object that forwards all meta-operations invoked upon it (at
 * the java-level) to its designated mirror object. To cut off infinite meta-regress
 * it also has magic_ variants of them which delegate to the default implementation.
 *
 * @author smostinc
 */
public class NATMirage extends NATObject {

	// Whenever this field is set, the object should be tested for the _MIRROR_ native type tag
	private ATObject mirror_;
	
	public static NATMirage createMirage(ATClosure code, ATObject dynamicParent, boolean parentType, ATTypeTag[] types, ATObject mirror) throws InterpreterException {
		// create a new, uninitialized mirage
		NATMirage newMirage = new NATMirage(dynamicParent, code.base_context().base_lexicalScope(), parentType, types);
			
		ATObject theMirror;
		if (mirror.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
			// the mirror argument is a constructor closure: the mirror to be used
			// is the return value of applying the closure
			theMirror = mirror.asClosure().base_apply(NATTable.of(newMirage));
		} else {
			// create a new instance of the mirror prototype with the uninitialized mirage,
			// this implicitly clones the mirror and re-initializes it, setting the base field
			// to this new mirage
			// def mirrorClone := mirror.new(<uninitialized mirage>)
			theMirror = mirror.meta_invoke(mirror, NATNil._NEW_NAME_, NATTable.of(newMirage));	
		}

		if (theMirror.meta_isTaggedAs(NativeTypeTags._MIRROR_).asNativeBoolean().javaValue) {
			// set the mirage's mirror to the cloned mirror
			newMirage.initializeWithMirror(theMirror);
			return newMirage;
		} else {
			throw new XIllegalArgument("Object used as a mirror without having the Mirror type tag: " + theMirror);
		}
	}
	
	/**
	 * Dedicated constructor for creating the initial empty mirage tied to the mirror root prototype.
	 */
	protected NATMirage(NATMirrorRoot mirror) {
		super();
		mirror_ = mirror;
	}
	
	public NATMirage(ATObject dynamicParent, ATObject lexParent, boolean parentType, ATTypeTag[] types) {
		super(dynamicParent, lexParent, parentType, types);
		mirror_ = Evaluator.getNil(); // set to nil while not initialized
	}
	
	/**
	 * Private setter to be used in OBJMirrorRoot.init to break the chicken-and-egg cycle when
	 * having to create both a mirror and its mirage simultaneously. The sequence is as follows:
	 *  1) a new empty 'uninitialized' mirage is created, with mirror as nil
	 *  2) a mirror is instantiated, leading to the invocation of its init method
	 *  3) the initialization of a new OBJMirrorRoot assigns the uninitialized mirage to its 'base' field
	 *  4) the mirror field of the uninitialized mirage is set to the newly created mirror, using this method.
	 */
	private void initializeWithMirror(ATObject realMirror) {
		mirror_ = realMirror;
	}
	
	/**
	 * Constructs a new ambienttalk mirage as a clone of an existing one. This results in a new
	 * uninitialized mirage (i.e. a mirage whose mirror points to nil). The code that clones the
	 * mirage must ensure that the mirror is correctly bound to a new instance of the cloned mirage's mirror.
	
	 * 
	 * 
	 */
	protected NATMirage(FieldMap map,
			         Vector state,
			         LinkedList customFields,
			         MethodDictionary methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags,
			         ATTypeTag[] types) throws InterpreterException {
		super(map, state, customFields, methodDict, dynamicParent, lexicalParent, flags, types);
		mirror_ = Evaluator.getNil();
	}
	
    public boolean isMirage() {
    	return true;
    }
	
	public NATMirage asMirage() throws XTypeMismatch {
		return this;
	}
	
	// Called by the default NATObject Cloning algorithm
	protected NATObject createClone(FieldMap map,
			Vector state,
			LinkedList customFields,
			MethodDictionary methodDict,
			ATObject dynamicParent,
			ATObject lexicalParent,
			byte flags, ATTypeTag[] types) throws InterpreterException {
		NATMirage clonedMirage = new NATMirage(map,
				state,
				customFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				types);
        // clonedMirage.mirror := myMirror.new(clonedMirage)
		clonedMirage.mirror_ = mirror_.meta_invoke(mirror_, NATNil._NEW_NAME_, NATTable.of(clonedMirage));
		return clonedMirage;
	}
	
	/**
	 * Access to the mirage's mirror, to enable a mirage to be 'upped' to a mirror value.
	 */
	protected ATObject getMirror() {
		return mirror_;
	}
	
	// MAGIC METHODS 
	// Cut-off for infinite meta-regress
	
	public ATNil magic_addMethod(ATMethod method) throws InterpreterException {
		return super.meta_addMethod(method);
	}

	public ATObject magic_clone() throws InterpreterException {
		return super.meta_clone();
	}
	
	public ATNil magic_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		return super.meta_defineField(name, value);
	}

	public ATMethod magic_grabMethod(ATSymbol selector) throws InterpreterException {
		return super.meta_grabMethod(selector);
	}

	/**
	 * <tt>invoke</tt> deviates from the default super-calling behaviour because
	 * the inherited <tt>invoke</tt> implementation from {@link NATCallframe}
	 * performs a self-send to {@link this#impl_selectAccessor(ATObject, ATSymbol)}
	 * or {@link this#impl_selectMutator(ATObject, ATAssignmentSymbol)}. However,
	 * these methods are overridden in the mirage to transform the impl methods into
	 * <tt>invoke</tt> methods, which causes an infinite loop.
	 * To break the loop, the inherited implementation is duplicated here, and the
	 * self-sends are replaced by static super-sends, such that the native implementation
	 * of the impl methods can be reused.
	 */
	public ATObject magic_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		//return super.meta_invoke(receiver, selector, arguments);
		// If the selector is an assignment symbol (i.e. `field:=) try to assign the corresponding field
		if (selector.isAssignmentSymbol()) {
			return super.impl_invokeMutator(receiver, selector.asAssignmentSymbol(), arguments);
		} else {
			return super.impl_invokeAccessor(receiver, selector, arguments);
		}
	}

	public ATTable magic_listMethods() throws InterpreterException {
		return super.meta_listMethods();
	}

	public ATObject magic_newInstance(ATTable initargs) throws InterpreterException {
		return super.meta_newInstance(initargs);
	}

	public NATText magic_print() throws InterpreterException {
		return super.meta_print();
	}

	public ATObject magic_receive(ATAsyncMessage message) throws InterpreterException {
		return super.meta_receive(message);
	}

	public ATBoolean magic_respondsTo(ATSymbol selector) throws InterpreterException {
		return super.meta_respondsTo(selector);
	}

	/**
	 * <tt>select</tt> deviates from the default super-calling behaviour because
	 * the inherited <tt>select</tt> implementation from {@link NATCallframe}
	 * performs a self-send to {@link this#impl_selectAccessor(ATObject, ATSymbol)}
	 * or {@link this#impl_selectMutator(ATObject, ATAssignmentSymbol)}. However,
	 * these methods are overridden in the mirage to transform the impl methods into
	 * <tt>select</tt> methods, which causes an infinite loop.
	 * To break the loop, the inherited implementation is duplicated here, and the
	 * self-sends are replaced by static super-sends, such that the native implementation
	 * of the impl methods can be reused.
	 */
	public ATObject magic_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		//return super.meta_select(receiver, selector);
		if (selector.isAssignmentSymbol()) {
			return super.impl_selectMutator(receiver, selector.asAssignmentSymbol());
		} else {
			return super.impl_selectAccessor(receiver, selector);
		}
	}

	public ATNil magic_addField(ATField field) throws InterpreterException {
		return super.meta_addField(field);
	}


	public ATObject magic_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		return super.meta_doesNotUnderstand(selector);
	}


	public ATField magic_grabField(ATSymbol selector) throws InterpreterException {
		return super.meta_grabField(selector);
	}


	public ATTable magic_listFields() throws InterpreterException {
		return super.meta_listFields();
	}


	public ATObject magic_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException {
		return super.meta_send(receiver, message);
	}


	public ATObject magic_eval(ATContext ctx) throws InterpreterException {
		return super.meta_eval(ctx);
	}


	public ATObject magic_quote(ATContext ctx) throws InterpreterException {
		return super.meta_quote(ctx);
	}
	
	public ATBoolean magic_isExtensionOfParent() throws InterpreterException {
		return super.meta_isExtensionOfParent();
	}

	public ATObject magic_invokeField(ATObject rcv, ATSymbol sym) throws InterpreterException {
		return super.meta_invokeField(rcv, sym);
	}
	
	public ATObject magic_pass() throws InterpreterException {
		return super.meta_pass();
	}
	
	public ATObject magic_resolve() throws InterpreterException {
		return super.meta_resolve();
	}
	
	public ATBoolean magic_isTaggedAs(ATTypeTag type) throws InterpreterException {
		return super.meta_isTaggedAs(type);
	}
	
	public ATTable magic_typeTags() throws InterpreterException {
		return super.meta_typeTags();
	}

	public ATBoolean magic_isCloneOf(ATObject original) throws InterpreterException {
		return super.meta_isCloneOf(original);
	}

	public ATBoolean magic_isRelatedTo(ATObject object) throws InterpreterException {
		return super.meta_isRelatedTo(object);
	}
	
	/* ========================================================================
	 * Each meta_x method defined in ATObject is implemented in a mirage as a
	 * forwarding method that asks its mirror to perform the operation on itself
	 * instead.
	 * ======================================================================== */

	public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("addMethod"),
				NATTable.atValue(new ATObject[] { method })
				);
			
		return Evaluator.getNil();
	}

	public ATObject meta_clone() throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("clone"),
				NATTable.EMPTY);
	}

	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("defineField"),
				NATTable.atValue(new ATObject[] { name, value }));
		return Evaluator.getNil();
	}
	
	public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("grabMethod"),
				NATTable.atValue(new ATObject[] { selector })
				).asMethod();
	}

	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("invoke"),
				NATTable.atValue(new ATObject[] { receiver, selector, arguments }));
	}

	public ATTable meta_listMethods() throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("listMethods"),
				NATTable.EMPTY
				).asTable();
	}

	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("newInstance"),
				NATTable.atValue(new ATObject[] { initargs }));
	}

	public NATText meta_print() throws InterpreterException {
		return mirror_.meta_invoke(
					mirror_,
					AGSymbol.jAlloc("print"),
					NATTable.EMPTY).asNativeText();
	}

	public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("receive"),
				NATTable.atValue(new ATObject[] { message }));
	}
	
	public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("respondsTo"),
				NATTable.atValue(new ATObject[] { selector })
				).asBoolean();
	}

	public ATClosure meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("select"),
				NATTable.atValue(new ATObject[] { receiver, selector })).asClosure();
	}

	public ATNil meta_addField(ATField field) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("addField"),
				NATTable.atValue(new ATObject[] { field }));
		return Evaluator.getNil();
	}


	public ATClosure meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("doesNotUnderstand"),
				NATTable.atValue(new ATObject[] { selector })).asClosure();
	}


	public ATField meta_grabField(ATSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("grabField"),
				NATTable.atValue(new ATObject[] { selector })).asField();
	}


	public ATTable meta_listFields() throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("listFields"),
				NATTable.EMPTY).asTable();
	}


	public ATObject meta_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("send"),
				NATTable.atValue(new ATObject[] { receiver, message }));
	}


	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("eval"),
				NATTable.atValue(new ATObject[] { ctx }));
	}


	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("quote"),
				NATTable.atValue(new ATObject[] { ctx }));
	}

	public ATBoolean meta_isExtensionOfParent() throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("isExtensionOfParent"),
				NATTable.EMPTY).asBoolean();
	}

	public ATObject meta_invokeField(ATObject rcv, ATSymbol sym) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("invokeField"),
				NATTable.atValue(new ATObject[] { rcv, sym }));
	}
	
    public ATObject meta_pass() throws InterpreterException {
    	return mirror_.meta_invoke(
				mirror_, AGSymbol.jAlloc("pass"), NATTable.EMPTY);
    }
    
    public ATObject meta_resolve() throws InterpreterException {
    	return mirror_.meta_invoke(
				mirror_, AGSymbol.jAlloc("resolve"), NATTable.EMPTY);
    }
    
    public ATBoolean meta_isTaggedAs(ATTypeTag type) throws InterpreterException {
    	return mirror_.meta_invoke(
				mirror_, AGSymbol.jAlloc("isTaggedAs"), NATTable.of(type)).asBoolean();
    }
    
    public ATTable meta_typeTags() throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("typeTags"),
				NATTable.EMPTY).asTable();
    }
	
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("isCloneOf"),
				NATTable.atValue(new ATObject[] { original })).asBoolean();
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("isRelatedTo"),
				NATTable.atValue(new ATObject[] { object })).asBoolean();
	}

	/** implementation-level method is mapped onto regular MOP method */
	public ATObject impl_invokeAccessor(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("invoke"),
				NATTable.atValue(new ATObject[] { receiver, selector, arguments }));
	}

	/** implementation-level method is mapped onto regular MOP method */
	public ATObject impl_invokeMutator(ATObject receiver, ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("invoke"),
				NATTable.atValue(new ATObject[] { receiver, selector, arguments }));
	}

	/** implementation-level method is mapped onto regular MOP method */
	public ATClosure impl_selectAccessor(ATObject receiver, ATSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("select"),
				NATTable.atValue(new ATObject[] { receiver, selector })).asClosure();
	}

	/** implementation-level method is mapped onto regular MOP method */
	public ATClosure impl_selectMutator(ATObject receiver, ATAssignmentSymbol selector) throws InterpreterException {
		return mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("select"),
				NATTable.atValue(new ATObject[] { receiver, selector })).asClosure();
	}
	
	
	
}
