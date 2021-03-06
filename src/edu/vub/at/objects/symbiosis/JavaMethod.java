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
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * JavaMethod is a wrapper class encapsulating one or more java.lang.reflect.Method objects.
 * The receiver is to be supplied during method application.
 * 
 * All methods in the choices array should be overloaded versions of the same method
 * (i.e. they should have the same selector). The choices array should never be empty!
 *
 * JavaMethod objects must be constant, they are globally cached for all actors to use.
 *
 * @author tvcutsem
 */
public final class JavaMethod extends NATByRef implements ATMethod {
	
	protected final Method[] choices_;
	
	public JavaMethod(Method[] choices) {
		// assertion
		if (choices.length == 0) { throw new RuntimeException("assertion failed: JavaMethod initialized with 0 methods"); }
		choices_ = choices;
	}
	
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		ATObject wrapper = ctx.base_receiver();
		Object receiver;
		
		// if the method is static, the receiver can be null
		if ((choices_[0].getModifiers() & Modifier.STATIC) != 0) {
			receiver = null;
		} else {
			receiver = Symbiosis.ambientTalkToJava(wrapper, choices_[0].getDeclaringClass());
		}
		return Symbiosis.symbioticInvocation(wrapper, receiver, choices_[0].getName(), this, arguments.asNativeTable().elements_);
	}
	
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments, ctx);
	}
	
	public ATBegin base_bodyExpression() throws InterpreterException {
		// list all of the method signatures of the (possibly overloaded) Java method
		StringBuffer buff = new StringBuffer("Java implementation of: ");
		for (int i = 0; i < choices_.length; i++) {
			buff.append("\n");
			buff.append(choices_[i].toString());
		}
		buff.append("\n");
		return new AGBegin(NATTable.atValue(new ATObject[] { NATText.atValue(buff.toString()) }));
	}

	public ATSymbol base_name() throws InterpreterException {
		return Reflection.downSelector(choices_[0].getName());
	}

	public ATTable base_parameters() throws InterpreterException {
		return Evaluator._ANON_MTH_ARGS_;
	}

	public ATTable base_annotations() throws InterpreterException {
		return NATTable.EMPTY;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java method:"+choices_[0].getName()+">");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._METHOD_);
    }
	
	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}
	
	public boolean isJavaMethodUnderSymbiosis() { return true; }
	public JavaMethod asJavaMethodUnderSymbiosis() { return this; }
	
	/**
	 * Two JavaMethod instances are equal if they both represent a set of methods
	 * from the same declaring class with the same selector.
	 */
    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
		if (comparand.isJavaMethodUnderSymbiosis()) {
			JavaMethod mth = comparand.asJavaMethodUnderSymbiosis();
			return NATBoolean.atValue(
					(mth.choices_[0].getDeclaringClass().equals(choices_[0].getDeclaringClass())) &&
			         mth.choices_[0].getName().equals(choices_[0].getName()));
		} else {
			return NATBoolean._FALSE_;
		}
    }

	public ATClosure base_wrap(ATObject lexicalScope, ATObject dynamicReceiver) {
		return new JavaClosure(lexicalScope, this);
	}
	
}
