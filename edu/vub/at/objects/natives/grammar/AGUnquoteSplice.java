/**
 * AmbientTalk/2 Project
 * AGUnquoteSplice.java created on 26-jul-2006 at 16:31:07
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIllegalQuote;
import edu.vub.at.exceptions.XIllegalUnquote;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of an unquote-splice AG element.
 */
public class AGUnquoteSplice extends AGExpression implements ATUnquoteSplice {

	private final ATExpression uqsExp_;
	
	public AGUnquoteSplice(ATExpression exp) {
		uqsExp_ = exp;
	}
	
	public ATExpression getExpression() { return uqsExp_; }

	/**
	 * An unquotation cannot be evaluated, but rather gives rise to an XIllegalUnquote exception.
	 * This is because an unquotation should always be nested within a quotation.
	 * 
	 * AGUQS(exp).eval(ctx) = ERROR
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		throw new XIllegalUnquote(uqsExp_);
	}

	/**
	 * Quoting a spliced unquotation means evaluating its contained expression, and returning
	 * its value as the result of the quote. A spliced unquotation can normally only
	 * be invoked where a table or begin is expected. The resulting table is replaced by the result of
	 * evaluating the unquote-splice's encapsulated expression.
	 * 
	 * It is the table's responsibility to query an AGUnquoteSplice element for its expression and deal with the inlining appropriately.
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		throw new XIllegalQuote(uqsExp_.meta_print().javaValue);
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("#@("+ uqsExp_.meta_print().javaValue + ")");
	}
	
	public boolean isUnquoteSplice() {
		return true;
	}
	
	public ATUnquoteSplice asUnquoteSplice() {
		return this;
	}

}
