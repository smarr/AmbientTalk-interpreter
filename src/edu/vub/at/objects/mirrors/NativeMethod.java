/**
 * AmbientTalk/2 Project
 * NativeMethod.java created on Jul 27, 2006 at 1:35:19 AM
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
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.lang.reflect.Method;

/**
 * A NativeMethod is a wrapper around a Java method allowing it to be selected 
 * from native base-level objects and passed around as an ordinary object.
 * 
 * @author tvcutsem
 * @author smostinc
 */
public final class NativeMethod extends NATByRef implements ATMethod {

	private final Method javaMethod_;
	private final ATSymbol name_;
	
	/**
	 * Construct a new wrapper object from a Java method.
	 * @param javaMethod the java method to be wrapped.
	 * @param isMeta signifies whether or not the wrapped method signifies an AmbientTalk meta-level method
	 */
	public NativeMethod(Method javaMethod, ATSymbol name) {
		javaMethod_ = javaMethod;
		name_ = name;
	}

	/**
	 * The name of a wrapped Java method is the name of the Java method, converted to an
	 * AmbientTalk selector name.
	 */
	public ATSymbol base_getName() throws InterpreterException {
		return name_;
	}
	
	/**
	 * The parameters of a wrapped method are represented as symbols
	 * representing the class name of the parameter type.
	 */
	public ATTable base_getParameters() throws InterpreterException {
		Class[] paramTypes = javaMethod_.getParameterTypes();
		AGSymbol[] paramNames = new AGSymbol[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			paramNames[i] = AGSymbol.jAlloc(Evaluator.valueNameOf(paramTypes[i]));
		}
		return NATTable.atValue(paramNames);
	}

	public ATBegin base_getBodyExpression() {
		return new AGBegin(NATTable.atValue(new ATObject[] {
				NATText.atValue("Native implementation of " + javaMethod_.toString())}));
	}
	
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		return JavaInterfaceAdaptor.invokeNativeATMethod(javaMethod_, ctx.base_getLexicalScope(),
						                                arguments.asNativeTable().elements_);
	}
	
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments, ctx);
	}

	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}

	public boolean isMethod() {
		return true;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<native method:"+name_+">");
	}
	
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.of(NativeStripes._METHOD_);
    }
	
}
