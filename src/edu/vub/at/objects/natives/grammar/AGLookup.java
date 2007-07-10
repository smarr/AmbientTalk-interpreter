/**
 * AmbientTalk/2 Project
 * AGApplication.java created on 10-jul-2007 at 10:13:31
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
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * The native implementation of a lexical lookup AG element.
 * 
 * @author smostinc
 */
public final class AGLookup extends AGExpression /* implements ATLookup */ {

	private final ATSymbol funName_;
	
	public AGLookup(ATSymbol fun) {
		funName_ = fun;
	}
	
	public ATExpression base_functionName() { return funName_; }

	/**
	 * To evaluate a lexical lookup, the function name is sought for in the lexical scope.
	 * 
	 * AGLKU(nam).eval(ctx) = ctx.lex.lookup(nam)
	 * 
	 * @return a closure containing the requested function.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return ctx.base_lexicalScope().impl_lookup(funName_.asSymbol());
	}

	/**
	 * Quoting an application results in a new quoted application.
	 * 
	 * AGAPL(sel,arg).quote(ctx) = AGAPL(sel.quote(ctx), arg.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGLookup(funName_.meta_quote(ctx).asSymbol());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("&" + funName_.meta_print().javaValue);
	}

}
