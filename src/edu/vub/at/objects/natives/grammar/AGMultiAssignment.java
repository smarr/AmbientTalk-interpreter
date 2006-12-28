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

import edu.vub.at.actors.ATFarReference;
import edu.vub.at.eval.PartialBinder;
import edu.vub.at.exceptions.InterpreterException;
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
	
	private final PartialBinder binderPartialFunction_;
	
	public AGMultiAssignment(ATTable par, ATExpression val) throws InterpreterException {
		parameters_ = par;
		valueExp_ = val;
		binderPartialFunction_ = PartialBinder.calculateResidual("multi-assignment", par);
	}

	public ATTable base_getParameters() { return parameters_; }
	
	public ATExpression base_getValueExpression() { return valueExp_; }
	
	/**
	 * To evaluate a multiple assignment, evaluate the right hand side to a table
	 * and assign the parameters on the left hand side to the 'arguments' of the right hand side,
	 * almost as if they were bound during a function call (the parameters are assigned instead of defined).
	 * 
	 * AGMULTIASS(par,val).eval(ctx) = assign(ctx.scope, par, val.eval(ctx)) ; nil
	 * 
	 * @return NIL
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		PartialBinder.assignArgsToParams(binderPartialFunction_, ctx, valueExp_.meta_eval(ctx).base_asTable());
		return NATNil._INSTANCE_;
	}

	/**
	 * AGMULTIASS(par,val).quote(ctx) = AGMULTIASS(par.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGMultiAssignment(parameters_.meta_quote(ctx).base_asTable(), valueExp_.meta_quote(ctx).base_asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(parameters_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}

    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * Passing a mutable and compound object implies making a new instance of the 
     * object while invoking pass on all its constituents.
     */
    public ATObject meta_pass(ATFarReference client) throws InterpreterException {
    		return new AGMultiAssignment(parameters_.meta_pass(client).base_asTable(), valueExp_.meta_pass(client).base_asExpression());
    }

    public ATObject meta_resolve() throws InterpreterException {
    		return new AGMultiAssignment(parameters_.meta_resolve().base_asTable(), valueExp_.meta_resolve().base_asExpression());
    }
}
