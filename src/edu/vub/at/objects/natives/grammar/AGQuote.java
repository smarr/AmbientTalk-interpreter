/**
 * AmbientTalk/2 Project
 * AGQuote.java created on 26-jul-2006 at 16:26:41
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
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATQuote;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author tvc
 *
 * The native implementation of a quotation AG element.
 */
public final class AGQuote extends AGExpression implements ATQuote {

	private final ATStatement stmt_;
	
	public AGQuote(ATStatement stmt) {
		stmt_ = stmt;
	}
	
	public ATStatement base_statement() { return stmt_; }

	/**
	 * To evaluate a quotation, start quoting its contained statement.
	 * This recursive quoting is necessary in order to properly unquote nested unquotations.
	 * 
	 * AGQUO(stmt).eval(ctx) = stmt.quote(ctx)
	 * 
	 * @return the quoted statement
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return stmt_.meta_quote(ctx);
	}

	/**
	 * Quoting a quotation results in a bare quotation, where quotations and unquotations are left unexpanded.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return this;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("`("+ stmt_.meta_print().javaValue + ")");
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		objectMap.incQuoteLevel();
		NATText code = NATText.atValue("`("+ stmt_.impl_asUnquotedCode(objectMap).javaValue + ")");
		objectMap.decQuoteLevel();
		return code;
	}
	
	public ATQuote asQuote() throws InterpreterException {
		return this;
	}
	
	/**
	 * FV(`exp) = FV_in_unquoted_expressions(exp)
	 */
	public Set impl_freeVariables() throws InterpreterException {
		return stmt_.impl_quotedFreeVariables();
	}
	
	/**
	 * The expression `( `(x + #(y)) ) has no free variables.
	 * It is assumed that the inner quotation is returned bare (without expansion)
	 */
	public Set impl_quotedFreeVariables() throws InterpreterException {
		return new HashSet();
	}

}
