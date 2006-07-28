/**
 * AmbientTalk/2 Project
 * NATClosure.java created on Jul 23, 2006 at 3:22:23 PM
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;

/**
 * @author smostinc
 *
 * TODO document the class NATClosure
 */
public class NATClosure extends NATNil implements ATClosure {

	private ATMethod 	method_;
	private ATContext	context_;
		
	/**
	 * This constructor creates a closure with an unbound dynamic receiver. This
	 * constructor is called when a closure is created to wrap a literal block and
	 * when a method is called using a receiverless message.  
	 * @param method the method being wrapped into a closure.
	 * @param implementor the object in which the definition is nested.
	 * 
	 * @deprecated only to be called in the lookup of NATObject 
	 */
	public NATClosure(ATMethod method, ATObject implementor) throws NATException {
		this(method, new NATContext(
				/* scope = implementor.addFrame() */
				new NATCallframe(implementor), 
				/* self = ` lexically defined ` */
				implementor, 
				/* super = implementor.getNext() */
				implementor.getDynamicParent()));
	}

	/**
	 * This constructor creates a closure with a bound dynamic receiver, and it is
	 * called after the succesful lookup of a receiverful message.
	 * @param method the method being wrapped into a closure.
	 * @param implementor the object in which the definition is nested.
	 * @param receiver the object where the lookup was initiated.
	 */
	public NATClosure(ATMethod method, ATObject implementor, ATObject receiver) {
		this(method, new NATContext(
				/* scope = implementor.addFrame() */
				new NATCallframe(implementor), 
				/* self = ` start of lookup ` */
				receiver, 
				/* super = implementor.getNext() */
				implementor.getDynamicParent()));
	}
	
	private NATClosure(ATMethod method, ATContext context) {
		method_	= method;
		context_	= context;
	}

	public ATObject meta_apply(ATTable arguments) throws NATException {
		return method_.getBody().meta_eval(context_);
	}

	public ATContext getContext() {
		return context_;
	}

	public ATMethod getMethod() {
		return method_;
	}

	public boolean isClosure() {
		return true;
	}

	public ATClosure asClosure() {
		return this;
	}

	
}
