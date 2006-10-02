/**
 * AmbientTalk/2 Project
 * AGClosureLiteral.java created on 26-jul-2006 at 16:59:04
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
import edu.vub.at.objects.grammar.ATClosureLiteral;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a literal closure AG element.
 */
public final class AGClosureLiteral extends AGExpression implements ATClosureLiteral {

	private final ATTable arguments_;
	private final ATBegin body_;
	
	public AGClosureLiteral(ATTable args, ATBegin body) {
		arguments_ = args;
		body_ = body;
	}
	
	public ATTable base_getArguments() { return arguments_; }

	public ATBegin base_getBodyExpression() { return body_; }

	/**
	 * To evaluate a closure literal, construct a closure pairing the literal's arguments and body with the
	 * current evaluation context.
	 * 
	 * AGCLOLIT(arg,bdy).eval(ctx) = AGCLO(AGMTH("lambda",arg,bdy), ctx)
	 * 
	 * @return a native closure closing over the current evaluation context.
	 */
	public ATObject meta_eval(ATContext ctx) {
		return new NATClosure(new NATMethod(Evaluator._LAMBDA_, arguments_, body_), ctx);
	}

	/**
	 * Quoting a closure literal results in a new quoted closure literal.
	 * 
	 * AGCLOLIT(arg,bdy).quote(ctx) = AGCLOLIT(arg.quote(ctx), bdy.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGClosureLiteral(arguments_.meta_quote(ctx).asTable(),
				                   body_.meta_quote(ctx).asBegin());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		// TODO: use ATBoolean.ifTrueifFalse instead of using a native boolean
		if (arguments_.base_isEmpty().asNativeBoolean().javaValue) {
		  return NATText.atValue("{ "+body_.meta_print().javaValue + " }");
		} else
		  return NATText.atValue(Evaluator.printElements(arguments_.asNativeTable(), "{ |", ", ", " | ").javaValue +
		                       body_.meta_print().javaValue+"}");
	}

}
