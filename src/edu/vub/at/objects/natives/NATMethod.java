/**
 * AmbientTalk/2 Project
 * NATMethod.java created on Jul 24, 2006 at 11:30:35 PM
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

import edu.vub.at.eval.PartialBinder;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * NATMethod implements methods as named functions which are in fact simply containers
 * for a name, a table of arguments and a body.
 * 
 * @author smostinc
 * @author tvcutsem
 */
public class NATMethod extends NATByCopy implements ATMethod {

	private final ATSymbol 	name_;
	private final ATTable 	parameters_;
	private final ATBegin	body_;
	
	// partial function denoting a parameter binding algorithm specialized for this method's parameter list
	private final PartialBinder parameterBindingFunction_;
	
	public NATMethod(ATSymbol name, ATTable parameters, ATBegin body) throws InterpreterException {
		name_ 		= name;
		parameters_ = parameters;
		body_ 		= body;
		
		// calculate the parameter binding strategy to use using partial evaluation
		parameterBindingFunction_ =
			PartialBinder.calculateResidual(name_.base_getText().asNativeText().javaValue, parameters);
	}

	public ATSymbol base_getName() {
		return name_;
	}

	public ATTable base_getParameters() {
		return parameters_;
	}

	public ATBegin base_getBodyExpression() {
		return body_;
	}
	
	/**
	 * To apply a function, first bind its parameters to the evaluated arguments within a new call frame.
	 * This call frame is lexically nested within the current lexical scope.
	 * 
	 * This method is invoked via the following paths:
	 *  - either by directly 'calling a function', in which case this method is applied via NATClosure.base_apply.
	 *    The closure ensures that the context used is the lexical scope, not the dynamic scope of invocation.
	 *  - or by 'invoking a method' through an object, in which case this method is applied via NATObject.meta_invoke.
	 *    The enclosing object ensures that the context is properly initialized with the implementor, the dynamic receiver
	 *    and the implementor's parent.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context in which to evaluate the method body, where a call frame will be inserted first
	 * @return the value of evaluating the function body
	 */
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		NATCallframe cf = new NATCallframe(ctx.base_getLexicalScope());
		ATContext evalCtx = ctx.base_withLexicalEnvironment(cf);
		PartialBinder.defineParamsForArgs(parameterBindingFunction_, evalCtx, arguments);
		return body_.meta_eval(evalCtx);
	}
	
	/**
	 * Applies the method in the context given, without first inserting a call frame to bind parameters.
	 * Arguments are bound directly in the given lexical scope.
	 * 
	 * This method is often invoked via its enclosing closure when used to implement various language
	 * constructs such as object:, mirror:, extend:with: etc.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context in which to evaluate the method body, to be used as-is
	 * @return the value of evaluating the function body
	 */
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		PartialBinder.defineParamsForArgs(parameterBindingFunction_, ctx, arguments);
		return body_.meta_eval(ctx);
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<method:"+name_.meta_print().javaValue+">");
	}

	public ATMethod base_asMethod() throws XTypeMismatch {
		return this;
	}

	public boolean base_isMethod() {
		return true;
	}
	
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.of(NativeStripes._METHOD_);
    }

}
