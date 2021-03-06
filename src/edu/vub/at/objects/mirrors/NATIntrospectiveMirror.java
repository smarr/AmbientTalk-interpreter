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

import edu.vub.at.actors.net.OBJNetwork;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
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
	public static final ATObject atValue(ATObject objectRepresentation) throws XTypeMismatch, XIllegalOperation {
		if (objectRepresentation.isMirage()) {
			ATObject mirror = objectRepresentation.asMirage().getMirror();
			if (mirror.equals(Evaluator.getNil())) {
				// this case is triggered if an introspective mirror is being created
				// for a new, empty, mirage object. Example:
				//   object: {} mirroredBy: (reflect: (object:{}))
				// (here, reflect: (object:{}) returns an introspective mirror)
				//
				// Since mirages must be mirrored by intercessive mirrors, we simply return
				// a new instance of the defaultMirror (since introspective mirrors
				// encapsulate 'default' semantics anyway)
				return new NATMirrorRoot(objectRepresentation.asMirage());
			} else {
				return mirror;
			}
		} else {
			return new NATIntrospectiveMirror(objectRepresentation);
		}
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
	
	public NATIntrospectiveMirror asNativeIntrospectiveMirror() {
		return this;
	}

	public boolean isNativeIntrospectiveMirror() {
		return true;
	}

	/**
	 * This method is used to allow selecting the base field of an intercessive mirror using 
	 * the reflective selection of fields. This method is never invoked directly by the 
	 * implementation.
	 * @return the base-level entity this mirror reflects on
	 */
	public ATObject base_base() {
		return principal_;
	}
	
	public int hashCode() { return principal_.hashCode(); }
	
	public ATBoolean base__opeql__opeql_(ATObject other) throws InterpreterException {
		if (other.isNativeIntrospectiveMirror()) {
			return NATBoolean.atValue(principal_.equals(other.asNativeIntrospectiveMirror().principal_));
		} else {
			return NATBoolean._FALSE_;
		}
	}
	
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */

    
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
	 * Listing the methods of a mirror requires us to list all of the meta_ methods
	 * of the principal + all of the base_ methods of the mirror itself
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
	 * This method makes a new mirror object. However, since the link from a 
	 * mirror to its base object is immutable, this results in contacting the mirror
	 * factory, to create a (new) mirror for the requested object.
	 * @param initargs  an ATObject[] containing as its first element the object that needs to be reflects upon
	 * @return <b>another</b> (possibly new) mirror object 
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
    	int len = initargs.base_length().asNativeNumber().javaValue;
		if (len != 1)
			throw new XArityMismatch("init method of mirror", 1, len);
		ATObject reflectee = initargs.base_at(NATNumber.ONE);
		return atValue(reflectee);
	}
	
	/* ---------------------------------
	 * -- Abstract Grammar Protocol   --
	 * --------------------------------- */
		
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<mirror on:"+principal_.meta_print().javaValue+">");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
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

	/**
	 * For mirrors, first try to find a <tt>meta_</tt> method in the principal.
	 * If this fails, try to find a <tt>base_</tt> method in the introspective mirror itself.
	 */
	protected boolean hasLocalMethod(ATSymbol atSelector) throws InterpreterException {
        return Reflection.upRespondsTo(principal_, Reflection.upMetaLevelSelector(atSelector)) ||
               super.hasLocalMethod(atSelector);
	}
	
	/**
	 * For mirrors, first try to find a <tt>meta_</tt> method in the principal.
	 * If this fails, try to find a <tt>base_</tt> method in the introspective mirror itself.
	 */
	protected ATMethod getLocalMethod(ATSymbol selector) throws InterpreterException {
		try {
			String methSelector = Reflection.upMetaLevelSelector(selector);
			return Reflection.upMethodSelection(principal_, methSelector, selector);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(selector);
			return super.getLocalMethod(selector);
		}
	}

}
