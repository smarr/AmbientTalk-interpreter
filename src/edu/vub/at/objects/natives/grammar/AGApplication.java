/**
 * AmbientTalk/2 Project
 * AGApplication.java created on 26-jul-2006 at 16:13:31
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.eval.InvocationStack;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATApplication;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.util.Set;

/**
 * The native implementation of an application AG element.
 * 
 * @author tvcutsem
 */
public final class AGApplication extends AGExpression implements ATApplication {

	private final ATExpression funExp_;
	private final ATTable arguments_;
	
	public AGApplication(ATExpression fun, ATTable arg) {
		funExp_ = fun;
		arguments_ = arg;
	}
	
	public ATExpression base_function() { return funExp_; }

	public ATTable base_arguments() { return arguments_; }

	/**
	 * To evaluate a function application, evaluate the receiver expression to a function, then evaluate the arguments
	 * to the function application eagerly and apply the function.
	 * 
	 * AGAPL(fun,arg).eval(ctx) = fun.eval(ctx).apply(map eval(ctx) over arg)
	 * 
	 * @return the return value of the applied function.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		// calls of the form f(x) are transformed into ctx.lex.call(`f, [x])
		if(funExp_.isSymbol()) {
			NATTable args = Evaluator.evaluateArguments(arguments_.asNativeTable(), ctx);
			ATObject result = null;
			InvocationStack stack = InvocationStack.getInvocationStack();
			try {
				stack.functionCalled(this, null, args);
				result = ctx.base_lexicalScope().impl_callAccessor(funExp_.asSymbol(), args);
			} finally {
				stack.funcallReturned(result);
			}
			return result;
		// calls of the form (expr)(x) are transformed into expr.eval(ctx).apply([x])
		} else {
			ATClosure clo = funExp_.meta_eval(ctx).asClosure();
			NATTable args = Evaluator.evaluateArguments(arguments_.asNativeTable(), ctx);
			ATObject result = null;
			InvocationStack stack = InvocationStack.getInvocationStack();
			try {
				stack.functionCalled(this, clo, args);
				result = clo.base_apply(args);
			} finally {
				stack.funcallReturned(result);
			}
			return result;
		}
	}

	/**
	 * Quoting an application results in a new quoted application.
	 * 
	 * AGAPL(sel,arg).quote(ctx) = AGAPL(sel.quote(ctx), arg.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGApplication(funExp_.meta_quote(ctx).asExpression(),
				                arguments_.meta_quote(ctx).asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(funExp_.meta_print().javaValue + Evaluator.printAsList(arguments_).javaValue);
	}
	
	/**
	 * FV(fExp(args)) = FV(fExp) U FV(args)
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvFunExp = funExp_.impl_freeVariables();
		fvFunExp.addAll(arguments_.impl_freeVariables());
		return fvFunExp;
	}

	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set fvFunExp = funExp_.impl_quotedFreeVariables();
		fvFunExp.addAll(arguments_.impl_quotedFreeVariables());
		return fvFunExp;
	}

}
