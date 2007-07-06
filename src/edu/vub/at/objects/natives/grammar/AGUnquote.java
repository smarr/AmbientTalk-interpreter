/**
 * AmbientTalk/2 Project
 * AGUnquote.java created on 26-jul-2006 at 16:29:32
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalUnquote;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATUnquote;
import edu.vub.at.objects.natives.NATText;

/**
 * The native implementation of an unquotation AG element.
 *
 * @author tvc
 */
public class AGUnquote extends AGExpression implements ATUnquote {

	protected final ATExpression unqExp_;
	
	public AGUnquote(ATExpression exp) {
		unqExp_ = exp;
	}
	
	public ATExpression base_expression() { return unqExp_; }

	/**
	 * An unquotation cannot be evaluated, but rather gives rise to an XIllegalUnquote exception.
	 * This is because an unquotation should always be nested within a quotation.
	 * 
	 * AGUNQ(exp).eval(ctx) = ERROR
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		throw new XIllegalUnquote(unqExp_);
	}

	/**
	 * Quoting an unquotation means evaluating its contained expression, and returning
	 * its value as the result of the quote.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return unqExp_.meta_eval(ctx);
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("#("+ unqExp_.meta_print().javaValue + ")");
	}
	
}
