/**
 * AmbientTalk/2 Project
 * JavaClosure.java created on 9-dec-2006 at 21:16:12
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSymbiosisFailure;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Vector;

/**
 * A JavaClosure pairs a JavaMethod (a bundle of native overloaded java.lang.reflect.Method objects)
 * together with a Java object receiver.
 * 
 * It also provides the possibility of casting the JavaMethod instances to fit a
 * narrower static type signature.
 * 
 * @author tvcutsem
 */
public final class JavaClosure extends NATClosure implements ATJavaClosure {

	private final ATObject scope_;
	
	public JavaClosure(ATObject scope, JavaMethod meth) {
		super(meth, null);
		scope_ = scope;
	}

	/**
	 * Overridden to allow for lazy instantiation of the context.
	 * 
	 * A 'default' context is lazily constructed and returned.
	 */
	public ATContext base_getContext() throws InterpreterException {
		if (context_ == null)
			context_ = new NATContext(scope_, scope_);
		return context_;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java closure:"+base_getMethod().base_getName().base_getText().asNativeText().javaValue+">");
	}

	/**
	 * For each Method in the wrapped JavaMethod's choices_, check whether it is compatible with
	 * the given types. If so, add it to the choices_ array of the new JavaMethod.
	 */
	public ATClosure base_cast(ATObject[] types) throws InterpreterException {
		Method[] choices = ((JavaMethod) method_).choices_;
		
		// unwrap the JavaClass wrappers
		Class[] actualTypes = new Class[types.length];
		for (int i = 0; i < actualTypes.length; i++) {
			// Array types may be represented as one-arg tables of a type: [Type]
			// TODO: properly refactor the instanceof test
			// problem: cannot do base_isTable because JavaObject/JavaClass objects will say yes!
			if (types[i] instanceof NATTable) {
				// Array.newInstance([Type][1],0).getClass()
				actualTypes[i] = Array.newInstance(types[i].asTable().
				    base_at(NATNumber.ONE).asJavaClassUnderSymbiosis().getWrappedClass(), 0).getClass();
			} else {
				actualTypes[i] = types[i].asJavaClassUnderSymbiosis().getWrappedClass();
			}
		}
		Vector matchingMethods = new Vector();
		
		for (int i = 0; i < choices.length; i++) {
			if(matches(choices[i].getParameterTypes(), actualTypes)) {
				matchingMethods.add(choices[i]);
			}
		}
		
		Method[] matches = (Method[]) matchingMethods.toArray(new Method[matchingMethods.size()]);
		if (matches.length > 0) {
			return new JavaClosure(scope_, new JavaMethod(matches));
		} else {
			throw new XSymbiosisFailure(scope_, choices[0].getName(), types);
		}
	}
	
	/**
	 * Compares two Class arrays and returns true iff both arrays have equal size and all members are the same.
	 */
	private static final boolean matches(Class[] formals, Class[] actuals) {
		if (formals.length != actuals.length)
			return false;
		
		for (int i = 0; i < formals.length; i++) {
			if (!(formals[i] == actuals[i])) {
				return false;
			}
		}
		
		return true;
	}

}
