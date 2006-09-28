/**
 * AmbientTalk/2 Project
 * AGDefFunction.java created on 26-jul-2006 at 15:44:08
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
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefMethod;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a function definition abstract grammar element.
 * This AG element covers both method and closure definition.
 */
public final class AGDefFunction extends NATAbstractGrammar implements ATDefMethod {

	private final ATSymbol selectorExp_;
	private final ATTable argumentExps_;
	private final ATBegin bodyStmts_;
	
	public AGDefFunction(ATSymbol sel, ATTable args, ATBegin bdy) {
		selectorExp_ = sel;
		argumentExps_ = args;
		bodyStmts_ = bdy;
	}
	
	public ATSymbol getSelector() {
		return selectorExp_;
	}

	public ATTable getArguments() {
		return argumentExps_;
	}

	public ATBegin getBody() {
		return bodyStmts_;
	}

	/**
	 * Defines a new function (method or closure) in the current scope.
	 * If a function definition is executed in the context of an object,
	 * a new method is created. If, however, the function definition
	 * is executed in the context of a call frame (i.e. its definition
	 * is nested within a method/function) then a closure is created instead.
	 * 
	 * The return value of a function definition is always NIL.
	 * 
	 * AGDEFFUN(nam,par,bdy).eval(ctx) =
	 *   if ctx.scope.isCallFrame
	 *      ctx.scope.addFunction(nam, AGCLO(AGMTH(nam,par,bdy), ctx))
	 *   else
	 *      ctx.scope.addMethod(nam, AGMTH(nam,par,bdy))
	 *      
	 * TODO: closure definition: should they really be fields? fields are mutable?
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		if (ctx.getLexicalScope().isCallFrame()) {
			ctx.getLexicalScope().meta_defineField(
					selectorExp_,
					new NATClosure(new NATMethod(selectorExp_, argumentExps_, bodyStmts_),
							      ctx));
		} else {
			ctx.getLexicalScope().meta_addMethod(new NATMethod(selectorExp_, argumentExps_, bodyStmts_));
		}
		return NATNil._INSTANCE_;
	}

	/**
	 * Quoting a function definition results in a new quoted function definition.
	 * 
	 * AGDEFFUN(nam,par,bdy).quote(ctx) = AGDEFFUN(nam.quote(ctx), par.quote(ctx), bdy.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGDefFunction(selectorExp_.meta_quote(ctx).asSymbol(),
				              argumentExps_.meta_quote(ctx).asTable(),
				              bodyStmts_.meta_quote(ctx).asBegin());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("def " +
				selectorExp_.meta_print().javaValue +
				Evaluator.printAsList(argumentExps_).javaValue +
				" { " + bodyStmts_.meta_print().javaValue + " }");
	}

}
