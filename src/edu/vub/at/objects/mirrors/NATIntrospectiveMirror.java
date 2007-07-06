/**
 * AmbientTalk/2 Project
 * NATMirror.java created on Aug 13, 2006 at 10:09:29 AM
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
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * <p>NATIntrospectiveMirror is a default mirror to represent an ambienttalk object 
 * which is capable of offering the java meta-interface of any language value at the 
 * ambienttalk level. This allows introspection into the language value's internals
 * as well as invoking some meta-level operations on it. Technically, NATMirror is 
 * simply a wrapper object around an ambienttalk object which deifies (ups) methods 
 * invoked upon it.</p>
 * 
 * <p>Note that whereas the mirror can offer e.g. an apply method when reifying a 
 * closure, this does not affect its own meta-interface. A NATMirror is always an 
 * object and can thus not be applied at the ambienttalk level.</p>
 * 
 * Example:
 * <pre>
 * def clo := { | x | x * 2 };
 * def  m  := at.mirrors.Factory.createMirror(clo);
 * 
 * clo( 5 )     <i> => 10 (legal) </i>
 * m.apply([5]) <i> => 10 (legal) </i>
 * m( 5 )       <i> => error (Application expected a closure, given a mirror) </i>
 * </pre>
 * 
 * @author smostinc
 * @author tvcutsem
 */

public class NATIntrospectiveMirror extends NATByRef {

	/** the object reflected on. This object is NOT a NATMirage */
	private final ATObject principal_;
	
	/**
	 * Return a mirror on the given native or custom AmbientTalk object.
	 * 
	 * @param objectRepresentation the object to reflect upon
	 * @return either an introspective mirror (if the passed object is native), otherwise
	 * a custom intercessive mirror.
	 */
	public static final ATObject atValue(ATObject objectRepresentation) throws XTypeMismatch {
		if(objectRepresentation instanceof NATMirage)
			return objectRepresentation.asMirage().getMirror();
		else
			return new NATIntrospectiveMirror(objectRepresentation);		
	}
	
	/**
	 * An introspective mirror is a wrapper which forwards a deified (upped) version of invoked 
	 * methods and field accesses to its principal. This principal is a Java object 
	 * representing an ambienttalk object. The deificiation process implies that 
	 * only the object's meta_level operations (implemented in Java) will be called
	 * directly by the mirror.
	 * 
	 * @param representation - the object to reflect upon, which is *not* a NATMirage
	 */
	private NATIntrospectiveMirror(ATObject representation) {
		principal_ = representation;
	}
	
	/**
	 * This method is used to allow selecting the base field of an intercessive mirror using 
	 * the reflective selection of fields. This method is never invoked directly by the 
	 * implementation.
	 * @return the base-level entity this mirror reflects on
	 */
	public ATObject base_getBase() {
		return principal_;
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * <p>The effect of invoking methods on a mirror (through meta_invoke) consists of
	 * checking whether the requested functionality is provided as a meta-operation
	 * by the principal that is wrapped by this mirror. This implies the requested 
	 * selector is sought for at the java-level, albeit prefixed with 'meta_'.</p>
	 *  
	 * <p>Because an explicit AmbientTalk method invocation must be converted into an 
	 * implicit Java method invocation, the invocation must be deified ('upped').
	 * To uphold stratification of the mirror architecture, the result of this 
	 * operation should be a mirror on the result of the Java method invocation.</p>
	 * 
	 * <p>Note that only when the principal does not have a matching meta_level method
	 * the mirror itself will be tested for a corresponding base_level method (e.g. 
	 * used for operators such as ==). In the latter case, stratification is not 
	 * enforced. This is due to the fact that these operations are not active at the
	 * mirror level, they are base-level operations which happen to be applied on a 
	 * mirror. An added advantage of this technique is that it permits a mirror to 
	 * give out a reference to its principal.</p>
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
		String jSelector = Reflection.upMetaLevelSelector(atSelector);
		
		try {
			return Reflection.upInvocation(
								principal_, // implementor and self
								jSelector,
								atSelector,
								arguments);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			// Principal does not have a corresponding meta_level method
			// try for a base_level method of the mirror itself.
			return super.meta_invoke(receiver, atSelector, arguments);
		}	
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
	public ATClosure meta_select(ATObject receiver, final ATSymbol atSelector) throws InterpreterException {
		final String jSelector = Reflection.upMetaFieldAccessSelector(atSelector);
		
		try {
			final ATObject val = Reflection.upFieldSelection(principal_, jSelector, atSelector);
			if (val.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
				return val.asClosure();
			} else {
				return new NativeClosure(this) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						return Reflection.upFieldSelection(principal_, jSelector, atSelector);
					}
				};
			}
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			try {
				String jMutatorSelector = Reflection.upMetaLevelSelector(atSelector);

				return Reflection.upMethodSelection(
								principal_, 
								jMutatorSelector,
								atSelector);
			} catch (XSelectorNotFound e2) {
				e2.catchOnlyIfSelectorEquals(atSelector);
				// Principal does not have a corresponding meta_level field nor
				// method try for a base_level field or method of the mirror itself.
				return super.meta_select(receiver, atSelector);
			}
		}			
	}
	
    /**
     * A mirror responds to a message m if and only if:
     *  - either its principal has a method named meta_m
     *  - or the mirror itself implements a method named base_m
     */
    public ATBoolean meta_respondsTo(ATSymbol atSelector) throws InterpreterException {
        String jSelector = Reflection.upMetaLevelSelector(atSelector);
        boolean metaResponds = Reflection.upRespondsTo(principal_, jSelector);
        if (metaResponds) {
          return NATBoolean._TRUE_;
        } else {
          return super.meta_respondsTo(atSelector);
        }
    }
	
	/**
	 * The effect of assigning a field on a mirror can be twofold. Either a meta_field
	 * of the reflectee is altered (in this case, the passed value must be a mirror to
	 * uphold stratification). Otherwise it is possible that a base field of the mirror
	 * itself is changed.
	 */
	public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
		String jSelector = Reflection.upMetaFieldMutationSelector(name);
		try{
			Reflection.upFieldAssignment(principal_, jSelector, name, value);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(name);
			// Principal does not have a corresponding meta_level method
			// OR the passed value is not a mirror object
			// try for a base_level method of the mirror itself.
			return super.meta_assignField(receiver, name, value);
		}			
		
		return NATNil._INSTANCE_;
	}
	
    public ATField meta_grabField(ATSymbol fieldName) throws InterpreterException {
        try {
        	    // try to find a meta_get / meta_set field in the principal_
			return Reflection.downMetaLevelField(principal_, fieldName);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(fieldName);
			// try to find a base_get / base_set field in the mirror
			return super.meta_grabField(fieldName);
		}
    }
    
    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        try {
        	    // try to find a meta_ method in the principal
			return Reflection.downMetaLevelMethod(principal_, methodName);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(methodName);
			// try to find a base_ method in the mirror
			return super.meta_grabMethod(methodName);
		}
    }
    
	/**
	 * Listing the fields of a mirror requires us to list all of the meta_get methods
	 * of the principal + all of the base_get methods of the mirror itself
	 */
	public ATTable meta_listFields() throws InterpreterException {
    	ATField[] principalMetaFields = Reflection.downMetaLevelFields(principal_);
    	ATField[] mirrorBaseFields = Reflection.downBaseLevelFields(this);
        return NATTable.atValue(NATTable.collate(principalMetaFields, mirrorBaseFields));
    }

	/**
	 * Listing the methods of a mirror requires us to list all of the meta_ methods
	 * of the principal (excluding meta_get/set methods) + all of the base_ methods
	 * (excluding base_get/set methods) of the mirror itself
	 */
    public ATTable meta_listMethods() throws InterpreterException {
   	    ATMethod[] principalMetaMethods = Reflection.downMetaLevelMethods(principal_);
   	    ATMethod[] mirrorBaseMethods = Reflection.downBaseLevelMethods(this);
        return NATTable.atValue(NATTable.collate(principalMetaMethods, mirrorBaseMethods));
    }
	
	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	/**
	 * This method allows re-initialise a mirror object. However, since the link from a 
	 * mirror to its base object is immutable, this results in contacting the mirror
	 * factory, to create a (new) mirror for the requested object.
	 * @param initargs - an ATObject[] containing as its first element the object that needs to be reflects upon
	 * @return <b>another</b> (possibly new) mirror object 
	 */
	public ATObject meta_newInstance(ATTable init) throws XArityMismatch, XTypeMismatch {
		ATObject[] initargs = init.asNativeTable().elements_;
		if(initargs.length != 1) {
			ATObject reflectee = initargs[0];
			return atValue(reflectee);
		} else {
			throw new XArityMismatch("init", 1, initargs.length);
		}
		
	}
	
	/* ---------------------------------
	 * -- Abstract Grammar Protocol   --
	 * --------------------------------- */
		
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<mirror on:"+principal_.meta_print().javaValue+">");
	}
	
    public ATTable meta_getTypeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._MIRROR_);
    }
	
	/**
	 * OBSOLETE: introspective mirrors are now pass-by-reference.
	 * This has the following repercussions:
	 *  - when passing the mirror of an isolate, a remote ref to the mirror is passed instead.
	 *    The isolate is not copied.
	 *  - mirrors on far references can still be created, by passing the principal by ref
	 *    and by reflecting on the obtained far reference
	 * 
	 * An introspective mirror is pass-by-copy.
     * When an introspective mirror is deserialized, it will become a mirror on
     * its deserialized principal. This means that, if the principal is passed
     * by copy, the mirror will be a local mirror on the copy. If the principal is passed
     * by reference, the mirror will be a local mirror on the far reference.
	 */
	/*public ATObject meta_resolve() throws InterpreterException {
		if(principal_ instanceof NATMirage)
			return ((NATMirage)principal_).getMirror();
		else
			return this;
	}*/

}
