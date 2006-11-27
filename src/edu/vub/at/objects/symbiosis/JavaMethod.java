/**
 * AmbientTalk/2 Project
 * JavaMethod.java created on 5-nov-2006 at 20:08:39
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;

import java.lang.reflect.Method;
import java.util.Vector;

/**
 * JavaMethod is a wrapper class encapsulating one or more java.lang.reflect.Method objects
 * together with a receiver.
 * 
 * A null receiver indicates a static method.
 * All methods in the choices array should be overloaded versions of the same method
 * (i.e. they should have the same selector).
 *
 * @author tvcutsem
 */
public final class JavaMethod extends NATNil implements ATJavaMethod {
	
	private final ATObject wrapper_;
	private final Object receiver_;
	private final Method[] choices_;
	
	public JavaMethod(ATObject wrapper, Object rcvr, Method[] choices) {
		wrapper_ = wrapper;
		receiver_ = rcvr;
		choices_ = choices;
	}
	
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		return Symbiosis.symbioticInvocation(wrapper_, receiver_, choices_[0].getName(), choices_, arguments.asNativeTable().elements_);
	}
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments, ctx);
	}
	
	public ATBegin base_getBodyExpression() throws InterpreterException {
		// list all of the method signatures of the (possibly overloaded) Java method
		StringBuffer buff = new StringBuffer("Java implementation of: ");
		for (int i = 0; i < choices_.length; i++) {
			buff.append("\n");
			buff.append(choices_[i].toString());
		}
		buff.append("\n");
		return new AGBegin(NATTable.atValue(new ATObject[] { NATText.atValue(buff.toString()) }));
	}

	public ATSymbol base_getName() throws InterpreterException {
		return Reflection.downSelector(choices_[0].getName());
	}

	public ATTable base_getParameters() throws InterpreterException {
		return Evaluator._ANON_MTH_ARGS_;
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java method:"+choices_[0].getName()+">");
	}
	
	public ATMethod base_asMethod() throws XTypeMismatch {
		return this;
	}

	public boolean base_isMethod() {
		return true;
	}

	/**
	 * For each Method in choices_, check whether it is compatible with the given types.
	 * If so, add it to the choices_ array of the new JavaMethod.
	 */
	public JavaMethod base_cast(ATObject[] types) throws InterpreterException {
		// unwrap the JavaClass wrappers
		Class[] actualTypes = new Class[types.length];
		for (int i = 0; i < actualTypes.length; i++) {
			actualTypes[i] = types[i].asJavaClassUnderSymbiosis().getWrappedClass();
		}
		Vector matchingMethods = new Vector();
		
		for (int i = 0; i < choices_.length; i++) {
			if(matches(choices_[i].getParameterTypes(), actualTypes)) {
				matchingMethods.add(choices_[i]);
			}
		}
		return new JavaMethod(wrapper_, receiver_,
				             (Method[]) matchingMethods.toArray(new Method[matchingMethods.size()]));
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
