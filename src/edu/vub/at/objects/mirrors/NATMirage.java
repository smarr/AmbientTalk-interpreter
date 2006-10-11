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
import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.MethodDictionary;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.Vector;

/**
 * A NATMirage is an object that forwards all meta-operations invoked upon it (at
 * the java-level) to its designated mirror object. To cut off infinite meta-regress
 * it also has magic_ variants of them which delegate to the default implementation.
 *
 * @author smostinc
 */
public class NATMirage extends NATObject {

	protected NATIntercessiveMirror mirror_;
	
	public NATMirage(NATIntercessiveMirror mirror) {
		mirror_ = mirror;
	}
	
	public NATMirage(ATObject lexicalParent, NATIntercessiveMirror mirror) {
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
			         MethodDictionary methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags,
			         NATIntercessiveMirror mirror) {
		super(map, state, methodDict, dynamicParent, lexicalParent, flags);
		mirror_ = mirror;
	}
	
	// MAGIC METHODS 
	// Cut-off for infinite meta-regress
	
	public ATNil magic_setMirror(NATIntercessiveMirror mirror) {
		mirror_ = mirror;
		return NATNil._INSTANCE_;
	}
	
	public ATNil magic_addMethod(ATMethod method) throws NATException {
		return super.meta_addMethod(method);
	}

	public ATNil magic_assignField(ATObject receiver, ATSymbol selector, ATObject value) throws NATException {
		return super.meta_assignField(receiver, selector, value);
	}

	public NATMirage magic_clone() throws NATException {
		return (NATMirage)super.meta_clone();
	}

	// CLONING AUXILIARY METHODS
	
	// Called by the default NATObject Cloning algorithm
	protected NATObject createClone(FieldMap map,
			Vector state,
			MethodDictionary methodDict,
			ATObject dynamicParent,
			ATObject lexicalParent,
			byte flags) throws NATException {
		NATMirage clone = new NATMirage(map,
				state,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				mirror_.magic_clone());
		
		clone.mirror_.setBase(clone);
		
		return clone;
	}
	
	// CALLED BY meta_clone ON INTERCESSIVE MIRRORS
	NATIntercessiveMirror getMirror() {
		return mirror_;
	}
	
	public ATNil magic_defineField(ATSymbol name, ATObject value) throws NATException {
		return super.meta_defineField(name, value);
	}

	public ATObject magic_extend(ATClosure code) throws NATException {
		return super.meta_extend(code);
	}

	public ATMethod magic_getMethod(ATSymbol selector) throws NATException {
		return super.meta_getMethod(selector);
	}

	public ATObject magic_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		return super.meta_invoke(receiver, selector, arguments);
	}

	public ATTable magic_listMethods() throws NATException {
		return super.meta_listMethods();
	}

	public ATObject magic_lookup(ATSymbol selector) throws NATException {
		return super.meta_lookup(selector);
	}

	// TODO Do we still need newInstance?
	public ATObject magic_newInstance(ATTable initargs) throws NATException {
		return super.meta_newInstance(initargs);
	}

	public NATText magic_print() throws NATException {
		return super.meta_print();
	}

	public ATBoolean magic_respondsTo(ATSymbol selector) throws NATException {
		return super.meta_respondsTo(selector);
	}

	public ATObject magic_select(ATObject receiver, ATSymbol selector) throws NATException {
		return super.meta_select(receiver, selector);
	}

	public ATObject magic_share(ATClosure code) throws NATException {
		return super.meta_share(code);
	}

	public ATNil magic_addField(ATField field) throws NATException {
		return super.meta_addField(field);
	}


	public ATNil magic_assignVariable(ATSymbol name, ATObject value) throws NATException {
		return super.meta_assignVariable(name, value);
	}


	public ATObject magic_doesNotUnderstand(ATSymbol selector) throws NATException {
		return super.meta_doesNotUnderstand(selector);
	}


	public ATField magic_getField(ATSymbol selector) throws NATException {
		return super.meta_getField(selector);
	}


	public ATTable magic_listFields() throws NATException {
		return super.meta_listFields();
	}


	public ATNil magic_send(ATAsyncMessage message) throws NATException {
		return super.meta_send(message);
	}


	public ATObject magic_eval(ATContext ctx) throws NATException {
		return super.meta_eval(ctx);
	}


	public ATObject magic_quote(ATContext ctx) throws NATException {
		return super.meta_quote(ctx);
	}
	
	public ATObject magic_getDynamicParent() throws NATException {
		return super.meta_getDynamicParent();
	}

	public ATObject magic_getLexicalParent() throws NATException {
		return super.meta_getLexicalParent();
	}	

	
	// META Methods 
	// Forward to our designated mirror object


	public ATNil meta_addMethod(ATMethod method) throws NATException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("addMethod"),
				new NATTable(new ATObject[] { method })
				);
			
		return NATNil._INSTANCE_;
	}

	public ATNil meta_assignField(ATObject receiver, ATSymbol selector, ATObject value) throws NATException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("assignField"),
				new NATTable(new ATObject[] { receiver, selector, value })
				);
			
		return NATNil._INSTANCE_;
	}

	public ATObject meta_clone() throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("clone"),
				NATTable.EMPTY
				));
	}

	public ATNil meta_defineField(ATSymbol name, ATObject value) throws NATException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("defineField"),
				new NATTable(new ATObject[] { name, value })
				);
			
		return NATNil._INSTANCE_;
	}

	public ATObject meta_extend(ATClosure code) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("extend"),
				new NATTable(new ATObject[] { code })
				));
	}
	
	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("getMethod"),
				new NATTable(new ATObject[] { selector })
				)).asMethod();
	}

	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("invoke"),
				new NATTable(new ATObject[] { receiver, selector, arguments })
				));
	}

	public ATTable meta_listMethods() throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("listMethods"),
				NATTable.EMPTY
				)).asTable();
	}

	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("lookup"),
				new NATTable(new ATObject[] { selector })
				));
	}

	// TODO Do we still need newInstance?
	public ATObject meta_newInstance(ATTable initargs) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("newInstance"),
				new NATTable(new ATObject[] { initargs })
				));
	}

	public NATText meta_print() throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
					mirror_,
					AGSymbol.alloc("print"),
					NATTable.EMPTY
					)).asNativeText();
	}

	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("respondsTo"),
				new NATTable(new ATObject[] { selector })
				)).asBoolean();
	}

	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("select"),
				new NATTable(new ATObject[] { receiver, selector })
				));
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("share"),
				new NATTable(new ATObject[] { code })
				));
	}


	public ATNil meta_addField(ATField field) throws NATException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("addField"),
				new NATTable(new ATObject[] { field })
				);
			
		return NATNil._INSTANCE_;
	}


	public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws NATException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("assignVariable"),
				new NATTable(new ATObject[] { name, value })
				);
			
		return NATNil._INSTANCE_;
	}


	public ATObject meta_doesNotUnderstand(ATSymbol selector) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("doesNotUnderstand"),
				new NATTable(new ATObject[] { selector })
				));
	}


	public ATField meta_getField(ATSymbol selector) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("getField"),
				new NATTable(new ATObject[] { selector })
		)).asField();
	}


	public ATTable meta_listFields() throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("listFields"),
				NATTable.EMPTY
				)).asTable();
	}


	public ATNil meta_send(ATAsyncMessage message) throws NATException {
		mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("send"),
				new NATTable(new ATObject[] { message })
				);
			
		return NATNil._INSTANCE_;
	}


	public ATObject meta_eval(ATContext ctx) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("eval"),
				new NATTable(new ATObject[] { ctx })
				));
	}


	public ATObject meta_quote(ATContext ctx) throws NATException {
		return Reflection.downObject(mirror_.meta_invoke(
				mirror_,
				AGSymbol.alloc("quote"),
				new NATTable(new ATObject[] { ctx })
				));
	}

	public ATObject meta_getDynamicParent() throws NATException {
		return Reflection.downObject(mirror_.meta_select(
				mirror_,
				AGSymbol.alloc("dynamicParent")));
	}

	public ATObject meta_getLexicalParent() throws NATException {
		return Reflection.downObject(mirror_.meta_select(
				mirror_,
				AGSymbol.alloc("lexicalParent")));
	}	
	
	
	
	
}
