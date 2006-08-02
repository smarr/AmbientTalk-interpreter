/**
 * AmbientTalk/2 Project
 * NATSuperObject.java created on Jul 27, 2006 at 4:37:25 PM
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author smostinc
 *
 * NATSuperObject is a decorator class for an ATObject when denoted using the super
 * pseudovariable. It is parameterised by two objects, namely the frame in which
 * all methods and fields are to be found and the late-bound receiver.
 */
public class NATSuperObject extends NATNil implements ATObject {
	
	private ATObject receiver_;
	private ATObject lookupFrame_;
	
	public NATSuperObject(ATObject receiver, ATObject lookupFrame) {
		receiver_ = receiver;
		lookupFrame_ = lookupFrame;
	}

	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */

	public ATNil meta_send(ATMessage msg) throws NATException {
		return lookupFrame_.meta_send(msg);
	}

	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException {
		return lookupFrame_.meta_invoke(receiver_, selector, arguments);
	}

	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		return lookupFrame_.meta_respondsTo(selector);
	}

	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		return lookupFrame_.meta_select(receiver_, selector);
	}

	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		return lookupFrame_.meta_lookup(selector);
	}

	public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException {
		return lookupFrame_.meta_assignField(name, value);
	}

	/* ------------------------------------
	 * -- Extension and cloning protocol --
	 * ------------------------------------ */

	public ATObject meta_clone() throws NATException {
		return lookupFrame_.meta_clone();
	}

	public ATObject meta_extend(ATClosure code) throws NATException {
		return lookupFrame_.meta_extend(code);
	}

	public ATObject meta_share(ATClosure code) throws NATException {
		return lookupFrame_.meta_share(code);
	}
	
	/* ---------------------------------
	 * -- Structural Access Protocol  --
	 * --------------------------------- */
	
	public ATNil meta_addField(ATField field) throws NATException {
		return lookupFrame_.meta_addField(field);
	}

	public ATNil meta_addMethod(ATMethod method) throws NATException {
		return lookupFrame_.meta_addMethod(method);
	}

	public ATField meta_getField(ATSymbol selector) throws NATException {
		return lookupFrame_.meta_getField(selector);
	}

	public ATMethod meta_getMethod(ATSymbol selector) throws NATException {
		return lookupFrame_.meta_getMethod(selector);
	}

	public ATTable meta_listFields() throws NATException {
		return lookupFrame_.meta_listFields();
	}

	public ATTable meta_listMethods() throws NATException {
		return lookupFrame_.meta_listMethods();
	}	

	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	public ATObject getDynamicParent() {
		return lookupFrame_.getDynamicParent();
	}

	public ATObject getLexicalParent() {
		return lookupFrame_.getLexicalParent();
	}

	/* ---------------------------------
	 * -- Accessors for JUnit tests   --
	 * --------------------------------- */	
	
	public ATObject getLookupFrame() {
		return this.lookupFrame_;
	}

	public ATObject getReceiver() {
		return this.receiver_;
	}

}
