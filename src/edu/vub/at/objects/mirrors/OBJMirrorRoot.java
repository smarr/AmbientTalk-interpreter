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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * OBJMirrorRoot is a singleton which is shared by as a parent by all NATIntercessiveMirrors,
 * It encodes the default behaviour to deal with invocation, selection and field assignment
 * along the dynamic parent chain. This behaviour can be extracted, since these operations 
 * are always parameterised by a receiver object, namely the intercessive mirror in question.
 *
 * @author smostinc
 */
public class OBJMirrorRoot extends NATByCopy {
	
	public static final ATSymbol _CLONE_ = AGSymbol.jAlloc("clone");
	public static final ATSymbol _MIRROR_ = AGSymbol.jAlloc("mirror");
	public static final OBJMirrorRoot _INSTANCE_ = new OBJMirrorRoot();
	
	/* PRIVATE CONSTRUCTOR - SINGLETON PATTERN */
	private OBJMirrorRoot() {};
	
	/**
	 * <p>The effect of invoking methods on a mirror (through meta_invoke) consists of
	 * checking whether the requested functionality is provided as a meta-operation
	 * by the principal that is wrapped by this mirror. Since such meta-operations
	 * are intercepted and forwarded to allow for interception, the mirage is expected
	 * to have a method for the given selector albeit prefixed with 'magic_'.</p>
	 *  
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
		
		NATMirage principal = (NATMirage)receiver.meta_select(receiver, NATObject._BASE_NAME_);

		// OBJMirrorRoot emulates it has a PrimitiveMethod clone
		if(atSelector == _CLONE_) {
			ATObject clone = receiver.meta_clone();
			clone.asAmbientTalkObject().setBase( principal.magic_clone(clone) );
			return clone;
		}
		
		// Same as upMetaLevelSelector but with magic_ instead of meta_
		// invoking a meta-level operation in the base would mean the operation
		// would be reified and passed to the customMirror resulting in an endless
		// loop. Therefore the magic_ methods, which are defined only on mirages
		// are used to cut off the infinite meta-regress.
		String jSelector = Reflection.upMagicLevelSelector(atSelector);
		
		try {
			return NATIntrospectiveMirror.atValue(
					Reflection.upInvocation(
									principal, // implementor and self
									jSelector,
									arguments));
		} catch (XSelectorNotFound e) {
			// Several meta_methods actually throw this exception to indicate they failed to select or lookup a
			// particular argument. Obviously these instances should be reported as is to the outside world. If,
			// however, the principal does not have a corresponding meta_level method for the given selector,
			// check whether the selector matches a base_level method of the mirror itself. This functionality 
			// readily is accessible using the super class.
			
			if(e.selector_ != atSelector) {
				throw e;
			} else {
				return super.meta_invoke(receiver, atSelector, arguments);
			}
		}
	}

	
	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */
	
	/**
	 * OBJMirrorRoot is a singleton object.
	 */
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}

	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */

	/**
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
	public ATObject meta_select(ATObject receiver, ATSymbol atSelector) throws InterpreterException {
		NATMirage principal = (NATMirage)receiver.meta_select(receiver, NATObject._BASE_NAME_);
		
		// Same as upMetaLevelSelector but with magic_ instead of meta_
		// invoking a meta-level operation in the base would mean the operation
		// would be reified and passed to the customMirror resulting in an endless
		// loop. Therefore the magic_ methods, which are defined only on mirages
		// are used to cut off the infinite meta-regress.
		String jSelector;
		
		try {
			jSelector = Reflection.upMagicFieldAccessSelector(atSelector);
			return NATIntrospectiveMirror.atValue(
					Reflection.upFieldSelection(
							principal,
							jSelector));
			
		} catch (XSelectorNotFound e) {
			try {
				jSelector = Reflection.upMagicLevelSelector(atSelector);

				return NATIntrospectiveMirror.atValue(
						Reflection.upMethodSelection(
								principal, 
								jSelector, atSelector));
			} catch (XSelectorNotFound e2) {
				// Principal does not have a corresponding meta_level field nor
				// method try for a base_level field or method of the mirror itself.
				return super.meta_select(receiver, atSelector);
			}
		}			
	}
	
	/**
	 * The effect of assigning a field on a mirror can be twofold. Either a meta_field
	 * of the reflectee is altered (in this case, the passed value must be a mirror to
	 * uphold stratification). Otherwise it is possible that a base field of the mirror
	 * itself is changed.
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
		NATMirage principal = (NATMirage)receiver.meta_select(receiver, NATObject._BASE_NAME_);
		
		String jSelector = Reflection.upMagicFieldMutationSelector(name);
		
		try{
			JavaInterfaceAdaptor.invokeNativeATMethod(
					principal.getClass(),
					principal,
					jSelector,
					new ATObject[] { name.equals(_MIRROR_)? value : value.meta_select(receiver, NATObject._BASE_NAME_) });
		} catch (XSelectorNotFound e) {
			// Principal does not have a corresponding meta_level method
			// OR the passed value is not a mirror object
			// try for a base_level method of the mirror itself.
			return super.meta_assignField(receiver, name, value);
		}			
		
		return NATNil._INSTANCE_;
	}
	
	/**
	 * After deserialization, ensure that the mirror root remains unique.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		return OBJMirrorRoot._INSTANCE_;
	}
	
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.of(NativeStripes._MIRROR_);
    }
	
}
