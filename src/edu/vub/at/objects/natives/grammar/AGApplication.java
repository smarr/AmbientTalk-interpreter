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

import edu.vub.at.actors.ATFarReference;
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

/**
 * @author tvc
 *
 * The native implementation of an application AG element.
 */
public final class AGApplication extends AGExpression implements ATApplication {

	private final ATExpression funExp_;
	private final ATTable arguments_;
	
	public AGApplication(ATExpression fun, ATTable arg) {
		funExp_ = fun;
		arguments_ = arg;
	}
	
	public ATExpression base_getFunction() { return funExp_; }

	public ATTable base_getArguments() { return arguments_; }

	/**
	 * To evaluate a function application, evaluate the receiver expression to a function, then evaluate the arguments
	 * to the function application eagerly and apply the function.
	 * 
	 * AGAPL(fun,arg).eval(ctx) = fun.eval(ctx).apply(map eval(ctx) over arg)
	 * 
	 * @return the return value of the applied function.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATClosure clo = funExp_.meta_eval(ctx).base_asClosure();
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
		//return clo.base_apply(Evaluator.evaluateArguments(arguments_.asNativeTable(), ctx));
	}

	/**
	 * Quoting an application results in a new quoted application.
	 * 
	 * AGAPL(sel,arg).quote(ctx) = AGAPL(sel.quote(ctx), arg.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGApplication(funExp_.meta_quote(ctx).base_asExpression(),
				                arguments_.meta_quote(ctx).base_asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(funExp_.meta_print().javaValue + Evaluator.printAsList(arguments_).javaValue);
	}
	
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * Passing a mutable and compound object implies making a new instance of the 
     * object while invoking pass on all its constituents.
     */
    public ATObject meta_pass(ATFarReference client) throws InterpreterException {
    		return new AGApplication(funExp_.meta_pass(client).base_asExpression(), arguments_.meta_pass(client).base_asTable());
    }

    public ATObject meta_resolve() throws InterpreterException {
		return new AGApplication(funExp_.meta_resolve().base_asExpression(), arguments_.meta_resolve().base_asTable());
    }
}
