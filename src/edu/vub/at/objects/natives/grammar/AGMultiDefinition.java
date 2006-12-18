/**
 * AmbientTalk/2 Project
 * AGMultiDefinition.java created on 18-aug-2006 at 10:13:58
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

import edu.vub.at.actors.ATFarObject;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMultiDefinition;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a multiple definition AG element.
 */
public class AGMultiDefinition extends NATAbstractGrammar implements ATMultiDefinition {

	private final ATTable parameters_;
	private final ATExpression valueExp_;
	
	public AGMultiDefinition(ATTable par, ATExpression val) {
		parameters_ = par;
		valueExp_ = val;
	}

	public ATTable base_getParameters() { return parameters_; }
	
	public ATExpression base_getValueExpression() { return valueExp_; }
	
	/**
	 * To evaluate a multiple definition, evaluate the right hand side to a table
	 * and bind the parameters on the left hand side to the 'arguments' of the right hand side,
	 * exactly as if they were bound during a function call.
	 * 
	 * AGMULTIDEF(par,val).eval(ctx) = bind(ctx.scope, par, val.eval(ctx)) ; nil
	 * 
	 * @return NIL
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		Evaluator.defineParamsForArgs("multi-definition", ctx.base_getLexicalScope(), parameters_, valueExp_.meta_eval(ctx).base_asTable());
		return NATNil._INSTANCE_;
	}

	/**
	 * AGMULTIDEF(par,val).quote(ctx) = AGMULTIDEF(par.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGMultiDefinition(parameters_.meta_quote(ctx).base_asTable(), valueExp_.meta_quote(ctx).base_asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " + parameters_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}

    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * Passing a mutable and compound object implies making a new instance of the 
     * object while invoking pass on all its constituents.
     */
    public ATObject meta_pass(ATFarObject client) throws InterpreterException {
    		return new AGMultiDefinition(parameters_.meta_pass(client).base_asTable(), valueExp_.meta_pass(client).base_asExpression());
    }

    public ATObject meta_resolve() throws InterpreterException {
    		return new AGMultiDefinition(parameters_.meta_resolve().base_asTable(), valueExp_.meta_resolve().base_asExpression());
    }
}
