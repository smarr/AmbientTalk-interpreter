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
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import sun.security.action.GetBooleanAction;

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
public class NATIntercessiveMirror extends NATIntrospectiveMirror {

	public NATIntercessiveMirror(NATMirage representation) {
		// the base object reflected upon by an intercessive mirror is always a mirage 
		super(representation);
	}
	
	/*
	 * Intercessive Mirrors inherit the default meta_extend and meta_share method 
	 * from their Introspective counterparts. The chief difference is that these 
	 * extensions can be used to intercept meta-level calls by invoking setMirror(this)
	 * from within the scope of the newly created mirror.
	 */
//	public ATObject meta_extend(ATClosure code) throws NATException {
//		return super.meta_extend(code);
//	}
//
//	public ATObject meta_share(ATClosure code) throws NATException {
//		return super.meta_share(code);
//	}



	/**
	 * <p>The effect of invoking methods on a mirror (through meta_invoke) consists of
	 * checking whether the requested functionality is provided as a meta-operation
	 * by the principal that is wrapped by this mirror. Since such meta-operations
	 * are intercepted and forwarded to allow for interception, the mirage is expected
	 * to have a method for the given selector albeit prefixed with 'magic_'.</p>
	 *  
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws NATException {
		
//		if(atSelector.equals(AGSymbol.alloc("clone"))){
//			
//			// CLONES
//			// 1) Custom mirror object
//			// 2) This default mirror object (which is its IS-A parent)
//			// 3) The base object (due to our meta_clone semantics)
//			ATObject customMirrorClone = receiver.meta_clone();
//			
//			// The final link to be made goes from base (3) to the customMirror (1)
//			// We use the *magic* setMeta operation defined on mirages to do this.
//			// NOTE : invocation needs to be sent to the clone !!!
//			// NOTE : this meta-message cannot be intercepted as it is sent to the NATIntercessiveMirror
//			//        this is done to allow forbidding setMirror without harming the cloning process.
//			customMirrorClone.meta_getDynamicParent().meta_invoke(this, AGSymbol.alloc("setMirror"), new NATTable(new ATObject[] { receiver }));
//			
//			return customMirrorClone;
//			
//		} else {
			
			// Same as upMetaLevelSelector but with magic_ instead of meta_
			// invoking a meta-level operation in the base would mean the operation
			// would be reified and passed to the customMirror resulting in an endless
			// loop. Therefore the magic_ methods, which are defined only on mirages
			// are used to cut off the infinite meta-regress.
			String jSelector = Reflection.upMagicLevelSelector(atSelector);
			
			try {
				return NATMirrorFactory._INSTANCE_.base_createMirror(
						Reflection.downObject(
								Reflection.upInvocation(
										principal_, // implementor and self
										jSelector,
										arguments)));
			} catch (XSelectorNotFound e) {
				// Principal does not have a corresponding meta_level method
				// try for a base_level method of the mirror itself. Note that 
				// we cannot delegate to the super implementation, which would 
				// imply invoking meta_ and thus an endless loop.
				try {
					
					jSelector = Reflection.upBaseLevelSelector(atSelector);
					return Reflection.downObject(Reflection.upInvocation(receiver, jSelector, arguments));
				
				} catch (XSelectorNotFound e2) {
					
					return receiver.meta_doesNotUnderstand(atSelector);
				}
			}
//		}
	}
	
	// Invoke the actual cloning method of the mirage
	public ATObject meta_clone() throws NATException {
		NATIntercessiveMirror clone = new NATIntercessiveMirror(
				((NATMirage)base_getBase()).mirage_clone());
		
		clone.meta_invoke(clone, AGSymbol.alloc("setMirror"), NATTable.EMPTY);
		return clone;
	}


}
