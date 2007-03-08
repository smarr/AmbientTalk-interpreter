/**
 * AmbientTalk/2 Project
 * NATIntercessiveMirror.java created on Oct 2, 2006 at 8:58:09 PM
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.MethodDictionary;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Vector;

/**
 * <p>NATIntercessiveMirror extends the default NATIntrospectiveMirror to also allow 
 * programmers to supply their own code for the meta-operations defined on an object.
 * This offers the programmers a way to intercede the default semantics implemented 
 * in the language for the scope of a single object</p>
 * 
 * <p>To achieve this, a triplet of objects is needed : first of all, instead of 
 * using a default object (whose behaviour is hardwired for efficiency), a mirage 
 * needs to be used, to reify the meta-operations called upon an object. These 
 * meta-operations are invoked on an ordinary object, which delegates to an instance
 * of this class to implement the correct default behaviour.</p> 
 *
 *
 * @deprecated
 * 
 * @author smostinc
 */
public class NATIntercessiveMirror extends NATObject implements ATMirror {

	private static final AGSymbol _SYM_BASE_ = AGSymbol.jAlloc("base");
	
	protected NATMirage principal_;
	
	/**
	 * Constructs a new ambienttalk mirror based on a set of parent pointers. 
	 * @param lexicalParent - the lexical scope in which the mirror's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 */
	public NATIntercessiveMirror(ATObject lexicalParent, boolean parentType) {
		super(OBJMirrorRoot._INSTANCE_, lexicalParent, parentType);
		principal_ = new NATMirage(this);
	}
	
	/**
	 * Constructs a new ambienttalk mirror based on a set of parent pointers. 
	 * @param dynamicParent - the dynamic parent of the new mirror, which should also be a mirror
	 * @param lexicalParent - the lexical scope in which the mirror's definition was nested
	 * @param parentType - how this object extends its dynamic parent (is-a or shares-a)
	 */
	public NATIntercessiveMirror(NATIntercessiveMirror dynamicParent, ATObject lexicalParent, boolean parentType) {
		super(dynamicParent, lexicalParent, parentType);
		principal_ = dynamicParent.principal_;
	}
	
	
	/**
	 * Constructs a new ambienttalk mirage as a clone of an existing one.
	 */
	protected NATIntercessiveMirror(FieldMap map,
			         Vector state,
			         LinkedList customFields,
			         MethodDictionary methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags,
			         ATStripe[] stripes,
			         NATMirage base) throws InterpreterException {
		super(map, state, customFields, methodDict, dynamicParent, lexicalParent, flags, stripes);
		principal_ = base;
		
	}
	
	/* -----------------------
	 * -- ATMirror Protocol --
	 * ----------------------- */

//	// Initialises the base field for users of the mirror
//	private void initBaseField() {
//		try {
//			meta_addField(new NativeField(
//					this,
//					AGSymbol.jAlloc("base"),
//					NATIntercessiveMirror.class.getDeclaredMethod(
//							"base_getBase",
//							new Class[0]),
//					null));
//		} catch (InterpreterException e) {
//			// This should not happen as mmeta_addField is natively implemented
//			throw new RuntimeException("Initialisation of base field in an intercessive mirror failed.", e);
//		} catch (SecurityException e) {
//			// This should not happen as the method is public and thus visible
//			throw new RuntimeException("Initialisation of base field in an intercessive mirror failed.", e);
//		} catch (NoSuchMethodException e) {
//			// This should not happen as the method exists
//			throw new RuntimeException("Initialisation of base field in an intercessive mirror failed.", e);
//		}
//	}
	
	public ATObject base_getBase() { return principal_; }
	
	/** @return true */
	public boolean base_isMirror() { return true; }
	
	/** @return this */
	public ATMirror base_asMirror() { return this; }
	
	/*
	 * TODO: refactor the intercepting methods using custom fields. Unfortunately they need to be initialised in places other than
	 * the constructors, apparently. One problem is that the custom field is not rebound on cloning implkying that the base mirage
	 * is always the empty one created implicitly when using the mirror constructor
	 */
	public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
		if (selector == _SYM_BASE_) {
			return principal_;
		} else {
			return super.meta_lookup(selector);
		}
	}
	
	/*
	 * TODO: refactor the intercepting methods using custom fields. Unfortunately they need to be initialised in places other than
	 * the constructors, apparently. One problem is that the custom field is not rebound on cloning implying that the base mirage
	 * is always the empty one created implicitly when using the mirror constructor
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
		if (selector == _SYM_BASE_) {
			return principal_;
		} else {
			return super.meta_select(receiver, selector);
		}
	}
	
	public ATObject meta_clone() throws InterpreterException {
		NATIntercessiveMirror clone = (NATIntercessiveMirror)super.meta_clone();
		clone.mirror_initialiseBaseField(false);
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
		return new NATIntercessiveMirror(map,
				state,
				customFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				stripes,
				// correct value for base_ set by NATMirage#createClone
				principal_); 
	}
	
	/* ---------------------------------
	 * -- Abstract Grammar Protocol   --
	 * --------------------------------- */
		
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<mirror on:"+principal_.meta_print().javaValue+">");
	}
	
	public ATTable meta_getStripes() throws InterpreterException {
		return NATTable.of(NativeStripes._MIRROR_);
	}

	/* ----------------------------
	 * -- Mirror-Base Protocol   --
	 * ---------------------------- */
	
	/**
	 * When cloning a mirror, it needs to be provided with a new mirage. We distinguish between
	 * two cases: First of all, when cloning a mirror, the mirage should be an empty one. When 
	 * cloning a mirage on the other hand, the mirror needs to be cloned and a new mirage needs
	 * to be created which is a clone of the original one.
	 */
	public void mirror_initialiseBaseField(boolean cloneCurrentBase) throws InterpreterException {
		if(cloneCurrentBase) {
			setBase( ((NATMirage)base_getBase()).magic_clone(this) );
		} else { // create a new empty mirage
			setBase( new NATMirage(this) );
		}
	}
}
