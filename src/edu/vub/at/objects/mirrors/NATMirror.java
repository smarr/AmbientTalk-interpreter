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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;

/**
 * <p>NATMirror represents  an ambienttalk object which is capable of offering the java 
 * meta-interface of any language value at the ambienttalk level. NATMirror is simply
 * a wrapper object around an ambienttalk object which deifies (ups) methods invoked
 * upon it.</p>
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
 */
public class NATMirror extends NATNil implements ATMirror {

	/**
	 * The prototypical mirror object reflects on nil. This mirror (or in effect any
	 * mirror in the system) can be used to create mirrors on objects. However, it is
	 * important to note that <code>mirror.new(object)</code> is merely a shorthand
	 * for a call to the mirror factory. This is in accordance with the principle 
	 * that all mirror creation happens through the mediation of the factory.
	 */
	public static NATMirror _PROTOTYPE_ = new NATMirror(NATNil._INSTANCE_);
	
	private ATObject principal_;
	
	
	/**
	 * A NATMirror is a wrapper which forwards a deified (upped) version of invoked 
	 * methods and field accesses to its principal. This principal is a Java object 
	 * representing an ambienttalk object. The deificiation process implies that 
	 * only the object's meta_level operations (implemented in Java) will be called
	 * directly by the mirror. 
	 */
	NATMirror(ATObject representation) {
		principal_ = representation;
	}
	
	/* -----------------------
	 * -- ATMirror Protocol --
	 * ----------------------- */

	public ATObject base_getBase() { return principal_; }

	/** @return true */
	public ATBoolean base_isMirror() { return NATBoolean._TRUE_; }
	
	/** @return this */
	public ATMirror asMirror() { return this; }
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * Asynchronous messages sent to an object ( o<-m( args )) are handled by the 
	 * actor in which the object is contained.
	 */
	public ATNil meta_send(ATAsyncMessage message) throws NATException {
         // TODO: nil <- m() => also do invoke-like deification?
		throw new RuntimeException("Not yet implemented: async message sends to mirrors");
	}
	
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
	public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws NATException {
		String jSelector = Reflection.upMetaLevelSelector(atSelector);
		
		try {
			return NATMirrorFactory._INSTANCE_.base_createMirror(
					Reflection.downObject(
							Reflection.upInvocation(
									principal_, // implementor
									principal_, // self
									jSelector,
									arguments)));
		} catch (XSelectorNotFound e) {
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
	public ATObject meta_select(ATObject receiver, ATSymbol atSelector) throws NATException {
		String jSelector = null;
		
		try {
			jSelector = Reflection.upMetaFieldAccessSelector(atSelector);
			return NATMirrorFactory._INSTANCE_.base_createMirror(
					Reflection.downObject(
							Reflection.upFieldSelection(
									principal_, 
									principal_, 
									jSelector)));
			
		} catch (XSelectorNotFound e) {
			try {
				jSelector = Reflection.upMetaLevelSelector(atSelector);

				return NATMirrorFactory._INSTANCE_.base_createMirror(
						Reflection.downObject(
								Reflection.upMethodSelection(
										principal_, 
										principal_, 
										jSelector)));
			} catch (XSelectorNotFound e2) {
				// Principal does not have a corresponding meta_level field nor
				// method try for a base_level field or method of the mirror itself.
				return super.meta_select(receiver, atSelector);
			}
		}			
	}
	
	
	/**
	 * The effect of assigning a field on a mirror can be twofold. Either a meta_field
	 * of the reflectee is altered (in this case, the passed value must be a mirror to)
	 * uphold stratification. Otherwise it is possible that a base field of the mirror
	 * itself is changed.
	 */
	public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException {
		String jSelector = null;
		
		try{
			jSelector = Reflection.upMetaFieldMutationSelector(name);

			JavaInterfaceAdaptor.invokeJavaMethod(
					principal_.getClass(),
					principal_,
					jSelector,
					new ATObject[] { value.asMirror().base_getBase() });
		} catch (XSelectorNotFound e) {
			// Principal does not have a corresponding meta_level method
			// OR the passed value is not a mirror object
			// try for a base_level method of the mirror itself.
			return super.meta_assignField(name, value);
		}			
		
		return NATNil._INSTANCE_;
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	/**
	 * Meta_clone will be called whenever someone invokes a new: operation on a 
	 * mirror. We return a prototypical mirror (initialized with the nil value)
	 * which can be properly initialised using base_init. The implementation of this
	 * method contacts the factory in order to create a new mirror.
	 */
	public ATObject meta_clone() throws NATException {
		return NATMirror._PROTOTYPE_;
	}


	private ATObject createChild(ATClosure code, boolean parentPointerType) throws NATException {
		NATObject extension = new NATObject(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.getContext().getLexicalScope(),
				/* parent porinter type */
				parentPointerType);
			
		ATAbstractGrammar body = code.getMethod().getBody();
		body.meta_eval(new NATContext(extension, extension, this));
			
		return extension;
	}
	
	/**
	 * <p>Extending a mirror with a custom object is possible, and creates a new 
	 * object which may override the meta_operations of the default mirror object. 
	 * However, the extension does not replace the mirror used by the interpreter: 
	 * to allow for objects to have a custom mirror, this mirror has to be supplied
	 * at creation time.</p>
	 */
	public ATObject meta_extend(ATClosure code) throws NATException {
		return createChild(code, NATObject._IS_A_);
	}

	/**
	 * <p>Sharing a mirror from a custom object is possible, and creates a new 
	 * object which may override the meta_operations of the default mirror object. 
	 * However, the extension does not replace the mirror used by the interpreter: 
	 * to allow for objects to have a custom mirror, this mirror has to be supplied
	 * at creation time.</p>
	 */
	public ATObject meta_share(ATClosure code) throws NATException {
		return createChild(code, NATObject._SHARES_A_);
	}
	
	/* ---------------------------------
	 * -- Abstract Grammar Protocol   --
	 * --------------------------------- */
		
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<mirror>");
	}
}
