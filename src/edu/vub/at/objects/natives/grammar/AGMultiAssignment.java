/**
 * AmbientTalk/2 Project
 * AGMultiAssignment.java created on 18-aug-2006 at 9:31:38
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
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMultiAssignment;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a multiple assignment AG element.
 */
public final class AGMultiAssignment extends NATAbstractGrammar implements ATMultiAssignment {

	private final ATTable parameters_;
	private final ATExpression valueExp_;
	
	public AGMultiAssignment(ATTable par, ATExpression val) {
		parameters_ = par;
		valueExp_ = val;
	}

	public ATTable getParameters() { return parameters_; }
	
	public ATExpression getValue() { return valueExp_; }
	
	/**
	 * To evaluate a multiple assignment, evaluate the right hand side to a table
	 * and assign the parameters on the left hand side to the 'arguments' of the right hand side,
	 * almost as if they were bound during a function call (the parameters are assigned instead of defined).
	 * 
	 * AGMULTIASS(par,val).eval(ctx) = assign(ctx.scope, par, val.eval(ctx)) ; nil
	 * 
	 * @return NIL
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		Evaluator.assignArgsToParams("multi-assignment", ctx.base_getLexicalScope(), parameters_, valueExp_.meta_eval(ctx).asTable());
		return NATNil._INSTANCE_;
	}

	/**
	 * AGMULTIASS(par,val).quote(ctx) = AGMULTIASS(par.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGMultiAssignment(parameters_.meta_quote(ctx).asTable(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue(parameters_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}

}
