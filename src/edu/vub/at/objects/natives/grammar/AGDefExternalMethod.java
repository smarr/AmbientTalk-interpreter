/**
 * AmbientTalk/2 Project
 * AGDefExternalMethod.java created on 15-nov-2006 at 19:33:18
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
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefExternalMethod;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosureMethod;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * The native implementation of an external method definition abstract grammar element.
 * 
 * @author tvcutsem
 */
public final class AGDefExternalMethod extends NATAbstractGrammar implements ATDefExternalMethod {

	private final ATSymbol rcvNam_;
	private final ATSymbol selectorExp_;
	private final ATTable argumentExps_;
	private final ATBegin bodyStmts_;
	
	public AGDefExternalMethod(ATSymbol rcv, ATSymbol sel, ATTable args, ATBegin bdy) {
		rcvNam_ = rcv;
		selectorExp_ = sel;
		argumentExps_ = args;
		bodyStmts_ = bdy;
	}

	public ATSymbol base_getReceiver() {
		return rcvNam_;
	}
	
	public ATSymbol base_getSelector() {
		return selectorExp_;
	}

	public ATTable base_getArguments() {
		return argumentExps_;
	}

	public ATBegin base_getBodyExpression() {
		return bodyStmts_;
	}

	/**
	 * Evaluates the receiver symbol to an object to which a new 'closure method' will be added.
	 * Such a closure captures the current lexical scope, but not the values for 'self' and 'super'.
	 * For an explanation of this rationale, see my interface description in ATDefExternalMethod.
	 * 
	 * The return value of an external method definition is always NIL.
	 * 
	 * AGDEFEXTMTH(rcv,nam,par,bdy).eval(ctx) =
	 *   rcv.eval(ctx).defineField(nam, NATCLOMTH(ctx.cur,nam,par,bdy))
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		rcvNam_.meta_eval(ctx).meta_addMethod(
				new NATClosureMethod(ctx.base_getLexicalScope(), new NATMethod(selectorExp_, argumentExps_, bodyStmts_)));
		return NATNil._INSTANCE_;
	}

	/**
	 * Quoting an external method definition results in a new quoted external method definition.
	 * 
	 * AGDEFEXTMTH(rcv,nam,par,bdy).quote(ctx) = AGDEFEXTMTH(rcv.quote(ctx),nam.quote(ctx), par.quote(ctx), bdy.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefExternalMethod(rcvNam_.meta_quote(ctx).base_asSymbol(),
				               selectorExp_.meta_quote(ctx).base_asSymbol(),
				               argumentExps_.meta_quote(ctx).base_asTable(),
				               bodyStmts_.meta_quote(ctx).base_asBegin());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " +
				rcvNam_.meta_print().javaValue + "." +
				selectorExp_.meta_print().javaValue +
				Evaluator.printAsList(argumentExps_).javaValue +
				" { " + bodyStmts_.meta_print().javaValue + " }");
	}

}
