/**
 * AmbientTalk/2 Project
 * JavaClosure.java created on 10-aug-2006 at 8:30:08
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
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATContext;

/**
 * @author tvc
 *
 * A JavaClosure is a wrapper class for a piece of Java code. The Java code is
 * presented to the AmbientTalk system as a closure. A JavaClosure is represented
 * as follows:
 * 
 * - Its encapsulating method is a 'dummy' JavaMethod ATMethod, with the following properties:
 *   - name = "nativelambda" (to distinguish it from 'ordinary' lambdas)
 *   - arguments = [ \@args ] (it takes an arbitrary number of arguments)
 *   - body = "Native implementation in {class}" (a string telling an inspector that
 *     this closure is natively implemented in the given Java class)
 *   - applying a nativelambda directly (without going through this JavaClosure)
 *     results in an error
 * - Its enclosed context consists of a triple (obj, obj, obj.super), where 'obj'
 *   is the Java object (implementing ATObject) that created this JavaClosure.
 *   
 * The method and context fields of a JavaClosure are lazily generated on demand
 * for efficiency reasons. Most of the time, a JavaClosure will not be introspected,
 * but only applied.
 * 
 * A JavaClosure can be used in two ways by Java code:
 *  1) As a generator for anonymous classes to generate 'anonymous lambdas':
 *     new JavaClosure(this) {
 *       public ATObject meta_apply(ATTable args) throws NATException {
 *         ...
 *       }
 *     }
 *  2) As a wrapper for an already existing JavaMethod:
 *     new JavaClosure(this, aJavaMethod);
 */
public class JavaClosure extends NATClosure {

	private final ATObject scope_;
	
	/**
	 * Create a new JavaClosure where meta_apply will be overridden by anonymous subclasses.
	 */
	public JavaClosure(ATObject scope) {
		this(scope, null);
	}
	
	/**
	 * Create a new JavaClosure where meta_apply will invoke the given Java Method.
	 * @param scope the object creating this JavaClosure.
	 */
	public JavaClosure(ATObject scope, JavaMethod meth) {
		super(meth, null);
		scope_ = scope;
	}

	/**
	 * Overridden to allow for lazy instantiation of the method.
	 * 
	 * If receiver is an anonymous JavaClosure, an 'anonymous' JavaMethod is returned.
	 * @return a JavaMethod wrapped by this JavaClosure.
	 */
	public ATMethod getMethod() {
		if (method_ == null)
			method_ = new JavaAnonymousMethod(scope_.getClass());
		return method_;
	}

	/**
	 * Overridden to allow for lazy instantiation of the context.
	 * 
	 * A 'default' context is lazily constructed and returned.
	 */
	public ATContext getContext() {
		if (context_ == null)
			context_ = new NATContext(scope_, scope_, scope_.getDynamicParent());
		return context_;
	}

	/**
	 * Apply the JavaClosure, which either gives rise to executing a native piece of
	 * code supplied by an anonymous subclass, or executes the wrapped JavaMethod.
	 */
	public ATObject meta_apply(ATTable arguments) throws NATException {
		if (method_ == null) {
			// this method is supposed to be overridden by an anonymous subclass
			throw new RuntimeException("JavaClosure's meta_apply not properly overridden by " + scope_.getClass());
		} else {
			return method_.meta_apply(arguments, this.getContext());
		}
	}

}
