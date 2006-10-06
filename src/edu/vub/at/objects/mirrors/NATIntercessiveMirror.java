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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.util.HashMap;
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
 * @author smostinc
 */
public class NATIntercessiveMirror extends NATObject implements ATMirror {

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
			         HashMap methodDict,
			         ATObject dynamicParent,
			         ATObject lexicalParent,
			         byte flags,
			         NATMirage base) {
		super(map, state, methodDict, dynamicParent, lexicalParent, flags);
		principal_ = base;

	}
	
	/* -----------------------
	 * -- ATMirror Protocol --
	 * ----------------------- */

	public ATObject base_getBase() { return principal_; }

	// not a base_ method => not exposed to the ambienttalk programmer.
	public ATNil setBase(NATMirage base) {
		principal_ = base;
		return NATNil._INSTANCE_;
	} 
	
	/** @return true */
	public ATBoolean base_isMirror() { return NATBoolean._TRUE_; }
	
	/** @return this */
	public ATMirror asMirror() { return this; }
	
	public ATObject meta_clone() throws NATException {
		return principal_.magic_clone().getMirror();
	}
	
	// CLONING AUXILIARY METHODS
	
	// Perform the actual cloning of this mirror object alone
	// Cut-off for ping-pong of messages sent between mirror and mirage.
	public NATIntercessiveMirror magic_clone() throws NATException {
		return (NATIntercessiveMirror)super.meta_clone();
	}

	// Called by the default NATObject Cloning algorithm
	protected NATObject createClone(FieldMap map,
			Vector state,
			HashMap methodDict,
			ATObject dynamicParent,
			ATObject lexicalParent,
			byte flags) throws NATException {
		return new NATIntercessiveMirror(map,
				state,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				// correct value for base_ set by NATMirage#createClone
				principal_); 
	}
	
	// Called by the default NATObject Extension algorithm
	protected ATObject createChild(ATClosure code, boolean parentPointerType) throws NATException {

		NATIntercessiveMirror extension = new NATIntercessiveMirror(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.base_getContext().base_getLexicalScope(),
				/* parent porinter type */
				parentPointerType);
			
		code.base_getMethod().base_apply(NATTable.EMPTY, new NATContext(extension, extension, this));
			
		return NATMirrorFactory._INSTANCE_.createMirror(extension);
	}
	
	/* ---------------------------------
	 * -- Abstract Grammar Protocol   --
	 * --------------------------------- */
		
	public NATText meta_print() throws NATException {
		return NATText.atValue("<mirror on:"+principal_.meta_print().javaValue+">");
	}
	
}
