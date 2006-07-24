/**
 * AmbientTalk/2 Project
 * NATNil.java created on Jul 13, 2006 at 9:38:51 PM
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
package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.IllegalOperation;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.TypeException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATSymbol;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.BaseNil;
import edu.vub.at.objects.mirrors.BaseInterfaceAdaptor;

/**
 * @author smostinc
 *
 * NATNil implements default semantics for all test and conversion methods.
 */
public class NATNil implements ATAbstractGrammar, BaseNil {

	private static NATNil _nil = null;
	
	protected NATNil() {};
	
	public static NATNil instance() {
		if(_nil == null) 
			_nil = new NATNil();
		return _nil;
	}
	
	public boolean isArray()  { return false; }
	public boolean isSymbol() { return false; }
	
	public ATTable asArray() throws TypeException {
		throw new TypeException("Object could not be transformed into an array.", this);
	}
	
	public ATSymbol asSymbol() throws TypeException {
		throw new TypeException("Object could not be transformed into a symbol.", this);		
	}

	public ATObject invoke(ATSymbol selector, ATTable arguments) throws NATException {
		return NATObject.cast(
			BaseInterfaceAdaptor.deifyInvocation(
				this.getBaseInterface(),
				this,
				selector,
				arguments));
	}
	
	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */

	/**
	 * Specifies an interface of methods which may be accessed directly from the 
	 * ambienttalk base-level. For "primitive" ambienttalk language values such as
	 * booleans, numbers, text, blocks, etc. this interface specifies the messages
	 * these values are capable of handling.
	 */
	protected Class getBaseInterface() {
		return BaseNil.class;
	}
	
	/**
	 * The default behaviour of meta_invoke for most ambienttalk language values is
	 * to check whether the requested functionality is provided in the interface 
	 * they export to the base-level. Therefore the BaseInterfaceAdaptor will try to 
	 * invoke the requested message, based on the passed selector and arguments.
	 */
	public ATObject meta_invoke(ATMessage msg) throws NATException {
		return NATObject.cast(
				BaseInterfaceAdaptor.deifyInvocation(
					this.getBaseInterface(),
					this,
					msg.getSelector(),
					msg.getArguments()));
	}

	/**
	 * An ambienttalk language value can respond to a message if this message is found
	 * in the interface it exports to the base-level.
	 */
	public ATBoolean meta_respondsTo(ATMessage msg) throws NATException {
		return NATBoolean.instance(BaseInterfaceAdaptor.hasApplicableMethod(
				this.getBaseInterface(),
				this,
				msg.getSelector(),
				msg.getArguments()));
	}


	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * It is possible to select a method from any ambienttalk value provided that it
	 * offers the method in its provided interface. The result is a JavaMethod wrapper
	 * which encapsulates the reflective Method object as well as the receiver.
	 */
	public ATObject meta_select(ATMessage msg) throws NATException {
//		if(meta_respondsTo(msg).isTrue()) {
//			return BaseInterfaceAdaptor.wrapMethodFor(
//					this.getBaseInterface(),
//					this,
//					msg.getSelector(),
//					msg.getArguments());
//		}
		return NATNil.instance();
	}

	public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException {
		throw new IllegalOperation("Cannot assign field " + name.toString() + " of an object of type " + this.getClass().getName());
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	public ATObject meta_clone() throws NATException {
		return this;
	}

	public ATObject meta_extend(ATClosure code) throws NATException {
		throw new IllegalOperation("Cannot extend an object of type " + this.getClass().getName());
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		throw new IllegalOperation("Cannot create a view upon an object of type " + this.getClass().getName());
	}
	
	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */
	
	public ATNil meta_addField(ATField field) throws NATException {
		throw new IllegalOperation("Cannot add fields to an object of type " + this.getClass().getName());
	}

	public ATNil meta_addMethod(ATMethod method) throws NATException {
		throw new IllegalOperation("Cannot add methods to an object of type " + this.getClass().getName());
	}

	public ATField meta_getField(ATSymbol selector) throws NATException {
		throw new IllegalOperation("Object of type " + this.getClass().getName() + " has no accessible fields");
	}

	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		// TODO @see select
		return null;
	}

	public ATTable meta_listFields() throws NATException {
		return NATTable.empty();
	}

	public ATTable meta_listMethods() throws NATException {
		// TODO @see BaseInterface
		return null;
	}

	/* ---------------------------------
	 * -- Abstract Grammar Protocol   --
	 * --------------------------------- */
	
	/**
	 * All NATObjects are self-evaluating.
	 */
	public ATObject meta_eval(ATContext ctx) {
		return this;
	}

	/**
	 * All NATobject are self-quoting.
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	/* ---------------------------------
	 * -- Value Conversion Protocol   --
	 * --------------------------------- */
	
	public boolean isClosure() {
		return false;
	}

	public ATClosure asClosure() throws TypeException {
		throw new TypeException("Expected a closure given :", this);
	}



}
