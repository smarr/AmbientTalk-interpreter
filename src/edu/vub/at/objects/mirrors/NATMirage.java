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

import java.util.LinkedList;
import java.util.Vector;

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.MethodDictionary;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * A NATMirage is an object that forwards all meta-operations invoked upon it (at
 * the java-level) to its designated mirror object. To cut off infinite meta-regress
 * it also has magic_ variants of them which delegate to the default implementation.
 *
 * Mirages can currently only be created for 'objects', not for 'isolates'.
 * Allowing isolates to be mirrored would require the introduction of 'isolate mirrors', since an isolate
 * can only be copied if its mirror can be copied.
 *
 * @author smostinc
 */
public class NATMirage extends NATObject {

	protected ATMirror mirror_;
	
	public NATMirage(ATMirror mirror) {
		mirror_ = mirror;
	}
	
	public NATMirage(ATObject lexicalParent, ATMirror mirror) {
		super(lexicalParent);
		mirror_ = mirror;
	}
	
	public NATMirage(ATObject dynamicParent, ATObject lexicalParent, NATIntercessiveMirror mirror, boolean parentType) {
		super(dynamicParent, lexicalParent, parentType);
		mirror_ = mirror;
	}
	
	/**
	 * Constructs a new ambienttalk mirage as a clone of an existing one.
	 */
	protected NATMirage(FieldMap map,
			         Vector state,
			         LinkedList customFields,
			         MethodDictionary methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags,
			         ATStripe[] stripes,
			         ATMirror mirror) throws InterpreterException {
		super(map, state, customFields, methodDict, dynamicParent, lexicalParent, flags, stripes);
		mirror_ = mirror;
	}
	
	// MAGIC METHODS 
	// Cut-off for infinite meta-regress
	
	public ATNil magic_addMethod(ATMethod method) throws InterpreterException {
		return super.meta_addMethod(method);
	}

	public ATNil magic_assignField(ATObject receiver, ATSymbol selector, ATObject value) throws InterpreterException {
		return super.meta_assignField(receiver, selector, value);
	}

	public NATMirage magic_clone(ATObject cloneMirror) throws InterpreterException {
		NATMirage clone = (NATMirage)super.meta_clone();

		clone.mirror_ = cloneMirror.base_asMirror();
		
		return clone;

	}

	// CLONING AUXILIARY METHODS
	
	// Called by the default NATObject Cloning algorithm
	protected NATObject createClone(FieldMap map,
			Vector state,
			LinkedList customFields,
			MethodDictionary methodDict,
			ATObject dynamicParent,
			ATObject lexicalParent,
			byte flags, ATStripe[] stripes) throws InterpreterException {
		return new NATMirage(map,
				state,
				customFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				stripes,
				mirror_);
	}
	
	// CALLED BY meta_clone ON INTERCESSIVE MIRRORS
	ATMirror getMirror() {
		return mirror_;
	}
	
	public ATNil magic_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		return super.meta_defineField(name, value);
	}

	public ATObject magic_extend(ATClosure code) throws InterpreterException {
		return super.meta_extend(code);
	}

	public ATMethod magic_getMethod(ATSymbol selector) throws InterpreterException {
		return super.meta_grabMethod(selector);
	}

	public ATObject magic_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		return super.meta_invoke(receiver, selector, arguments);
	}

	public ATTable magic_listMethods() throws InterpreterException {
		return super.meta_listMethods();
	}

	public ATObject magic_lookup(ATSymbol selector) throws InterpreterException {
		return super.meta_lookup(selector);
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

	public ATObject magic_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		return super.meta_select(receiver, selector);
	}

	public ATObject magic_share(ATClosure code) throws InterpreterException {
		return super.meta_share(code);
	}

	public ATNil magic_addField(ATField field) throws InterpreterException {
		return super.meta_addField(field);
	}


	public ATNil magic_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
		return super.meta_assignVariable(name, value);
	}


	public ATObject magic_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		return super.meta_doesNotUnderstand(selector);
	}


	public ATField magic_getField(ATSymbol selector) throws InterpreterException {
		return super.meta_grabField(selector);
	}


	public ATTable magic_listFields() throws InterpreterException {
		return super.meta_listFields();
	}


	public ATObject magic_send(ATAsyncMessage message) throws InterpreterException {
		return super.meta_send(message);
	}


	public ATObject magic_eval(ATContext ctx) throws InterpreterException {
		return super.meta_eval(ctx);
	}


	public ATObject magic_quote(ATContext ctx) throws InterpreterException {
		return super.meta_quote(ctx);
	}
	
	public ATObject magic_getDynamicParent() throws InterpreterException {
		return super.meta_getDynamicParent();
	}

	public ATObject magic_getLexicalParent() throws InterpreterException {
		return super.meta_getLexicalParent();
	}
	
	public ATObject magic_pass() throws InterpreterException {
		return super.meta_pass();
	}
	
	public ATObject magic_resolve() throws InterpreterException {
		return super.meta_resolve();
	}
	
	public ATBoolean magic_isStripedWith(ATStripe stripe) throws InterpreterException {
		return super.meta_isStripedWith(stripe);
	}
	
	public ATTable magic_getStripes() throws InterpreterException {
		return super.meta_getStripes();
	}

	public ATBoolean magic_isCloneOf(ATObject original) throws InterpreterException {
		return super.meta_isCloneOf(original);
	}

	public ATBoolean magic_isRelatedTo(ATObject object) throws InterpreterException {
		return super.meta_isRelatedTo(object);
	}
	
	// META Methods 
	// Forward to our designated mirror object


	public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("addMethod"),
				NATTable.atValue(new ATObject[] { method })
				);
			
		return NATNil._INSTANCE_;
	}

	public ATNil meta_assignField(ATObject receiver, ATSymbol selector, ATObject value) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("assignField"),
				NATTable.atValue(new ATObject[] { receiver, selector, value })
				);
			
		return NATNil._INSTANCE_;
	}

	public ATObject meta_clone() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("clone"),
				NATTable.EMPTY
				));
	}

	public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("defineField"),
				NATTable.atValue(new ATObject[] { name, value })
				);
			
		return NATNil._INSTANCE_;
	}

	public ATObject meta_extend(ATClosure code) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("extend"),
				NATTable.atValue(new ATObject[] { code })
				));
	}
	
	public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("grabMethod"),
				NATTable.atValue(new ATObject[] { selector })
				)).base_asMethod();
	}

	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("invoke"),
				NATTable.atValue(new ATObject[] { receiver, selector, arguments })
				));
	}

	public ATTable meta_listMethods() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("listMethods"),
				NATTable.EMPTY
				)).base_asTable();
	}

	public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("lookup"),
				NATTable.atValue(new ATObject[] { selector })
				));
	}

	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("newInstance"),
				NATTable.atValue(new ATObject[] { initargs })
				));
	}

	public NATText meta_print() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
					mirror_,
					AGSymbol.jAlloc("print"),
					NATTable.EMPTY
					)).asNativeText();
	}

	public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("receive"),
				NATTable.atValue(new ATObject[] { message })));
	}
	
	public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("respondsTo"),
				NATTable.atValue(new ATObject[] { selector })
				)).base_asBoolean();
	}

	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("select"),
				NATTable.atValue(new ATObject[] { receiver, selector })
				));
	}

	public ATObject meta_share(ATClosure code) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("share"),
				NATTable.atValue(new ATObject[] { code })
				));
	}


	public ATNil meta_addField(ATField field) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("addField"),
				NATTable.atValue(new ATObject[] { field })
				);
			
		return NATNil._INSTANCE_;
	}


	public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("assignVariable"),
				NATTable.atValue(new ATObject[] { name, value })
				);
			
		return NATNil._INSTANCE_;
	}


	public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("doesNotUnderstand"),
				NATTable.atValue(new ATObject[] { selector })
				));
	}


	public ATField meta_grabField(ATSymbol selector) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("grabField"),
				NATTable.atValue(new ATObject[] { selector })
		)).base_asField();
	}


	public ATTable meta_listFields() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("listFields"),
				NATTable.EMPTY
				)).base_asTable();
	}


	public ATObject meta_send(ATAsyncMessage message) throws InterpreterException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("send"),
				NATTable.atValue(new ATObject[] { message })
				);
			
		return NATNil._INSTANCE_;
	}


	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("eval"),
				NATTable.atValue(new ATObject[] { ctx })
				));
	}


	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("quote"),
				NATTable.atValue(new ATObject[] { ctx })
				));
	}

	public ATObject meta_getDynamicParent() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_select(
				mirror_,
				AGSymbol.jAlloc("dynamicParent")));
	}

	public ATObject meta_getLexicalParent() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_select(
				mirror_,
				AGSymbol.jAlloc("lexicalParent")));
	}
	
    public ATObject meta_pass() throws InterpreterException {
    	return Reflection.downObject(mirror_.meta_invoke(
				mirror_, AGSymbol.jAlloc("pass"), NATTable.EMPTY));
    }
    
    public ATObject meta_resolve() throws InterpreterException {
    	return Reflection.downObject(mirror_.meta_invoke(
				mirror_, AGSymbol.jAlloc("resolve"), NATTable.EMPTY));
    }
    
    public ATBoolean meta_isStripedWith(ATStripe stripe) throws InterpreterException {
    	return Reflection.downObject(mirror_.meta_invoke(
				mirror_, AGSymbol.jAlloc("isStripedWith"), NATTable.of(stripe))).base_asBoolean();
    }
    
    public ATTable meta_getStripes() throws InterpreterException {
		return Reflection.downObject(mirror_.meta_select(
				mirror_,
				AGSymbol.jAlloc("stripes"))).base_asTable();
    }
	
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("isCloneOf"),
				NATTable.atValue(new ATObject[] { original })
				)).base_asBoolean();
	}

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.jAlloc("isRelatedTo"),
				NATTable.atValue(new ATObject[] { object })
				)).base_asBoolean();
	}

	public boolean base_isMirror() {
		return false;
	}
	
	public ATMirror base_asMirror() throws InterpreterException {
		throw new XTypeMismatch(ATMirror.class, this);
	}
	
	// when passing a mirage as a parameter in an asynchronous message send,
	// make the mirror decide what object to read or write
	
	/*public Object writeReplace() throws ObjectStreamException {
		try {
			// TODO: what to pass as client?
			return meta_pass();
		} catch (InterpreterException e) {
			throw new WriteAbortedException("Mirage's mirror failed to pass", e);
		}
	}
	
	public Object readResolve() throws ObjectStreamException {
		try {
			return meta_resolve();
		} catch (InterpreterException e) {
			throw new WriteAbortedException("Mirage's mirror failed to resolve", e);
		}
	}*/
	
}
