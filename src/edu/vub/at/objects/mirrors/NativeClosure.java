/**
 * AmbientTalk/2 Project
 * NativeClosure.java created on 10-aug-2006 at 8:30:08
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATText;

/**
 * A NativeClosure is a wrapper class for a piece of Java code. The Java code is
 * presented to the AmbientTalk system as a closure. A NativeClosure is represented
 * as follows:
 * 
 * - Its encapsulating method is a 'dummy' NativeMethod ATMethod, with the following properties:
 *   - name = "nativelambda" (to distinguish it from 'ordinary' lambdas)
 *   - arguments = [ \@args ] (it takes an arbitrary number of arguments)
 *   - body = "Native implementation in {class}" (a string telling an inspector that
 *     this closure is natively implemented in the given Java class)
 *   - applying a nativelambda directly (without going through this NativeClosure)
 *     results in an error
 * - Its enclosed context consists of a triple (obj, obj, obj.super), where 'obj'
 *   is the Java object (implementing ATObject) that created this NativeClosure.
 *   
 * The method and context fields of a NativeClosure are lazily generated on demand
 * for efficiency reasons. Most of the time, a NativeClosure will not be introspected,
 * but only applied.
 * 
 * A NativeClosure can be used in two ways by Java code:
 *  1) As a generator for anonymous classes to generate 'anonymous lambdas':
 *     new NativeClosure(this) {
 *       public ATObject meta_apply(ATTable args) throws NATException {
 *         ...
 *       }
 *     }
 *  2) As a wrapper for an already existing NativeMethod:
 *     new NativeClosure(this, aJavaMethod);
 * 
 * @author tvc
 */
public class NativeClosure extends NATClosure {

	protected final ATObject scope_;
	
	/**
	 * Create a new NativeClosure where meta_apply will be overridden by anonymous subclasses.
	 */
	public NativeClosure(ATObject scope) {
		this(scope, null);
	}
	
	/**
	 * Create a new NativeClosure where meta_apply will invoke the given Java Method.
	 * @param scope the object creating this NativeClosure.
	 */
	public NativeClosure(ATObject scope, ATMethod meth) {
		super(meth, null);
		scope_ = scope;
	}

	/**
	 * Overridden to allow for lazy instantiation of the method.
	 * 
	 * If receiver is an anonymous NativeClosure, an 'anonymous' NativeMethod is returned.
	 * @return a NativeMethod wrapped by this NativeClosure.
	 */
	public ATMethod base_getMethod() {
		if (method_ == null)
			method_ = new NativeAnonymousMethod(scope_.getClass());
		return method_;
	}

	/**
	 * Overridden to allow for lazy instantiation of the context.
	 * 
	 * A 'default' context is lazily constructed and returned.
	 */
	public ATContext base_getContext() throws InterpreterException {
		if (context_ == null)
			context_ = new NATContext(scope_, scope_, scope_.meta_getDynamicParent());
		return context_;
	}

	/**
	 * Apply the NativeClosure, which either gives rise to executing a native piece of
	 * code supplied by an anonymous subclass, or executes the wrapped NativeMethod.
	 */
	public ATObject base_apply(ATTable arguments) throws InterpreterException {
		if (method_ == null) {
			// this method is supposed to be overridden by an anonymous subclass
			throw new RuntimeException("NativeClosure's meta_apply not properly overridden by " + scope_.getClass());
		} else {
			return method_.base_apply(arguments, this.base_getContext());
		}
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<native closure:"+base_getMethod().base_getName().base_getText().asNativeText().javaValue+">");
	}
	
	/**
	 * Auxiliary method to more easily extract arguments from an ATTable
	 */
	public ATObject get(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n));
	}

	public int getNbr(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeNumber().javaValue;
	}
	
	public double getFrc(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeFraction().javaValue;
	}
	
	public String getTxt(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeText().javaValue;
	}
	
	public boolean getBln(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeBoolean().javaValue;
	}
	
	public Object[] getTab(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeTable().elements_;
	}
	
	public void checkArity(ATTable args, int required) throws InterpreterException {
		int provided = args.base_getLength().asNativeNumber().javaValue;
		if (provided != required) {
			throw new XArityMismatch(Evaluator._ANON_MTH_NAM_.toString(), required, provided);
		}
	}
	
}
