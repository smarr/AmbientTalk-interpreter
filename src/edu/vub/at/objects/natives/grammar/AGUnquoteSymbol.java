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
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * AGUnquoteSymbol is a class encapsulating an unquoted expression in a position where the 
 * grammar of the language prescribes the presence of an { @link edu.vub.at.objects.grammar.ATSymbol}.
 * The implementation of this class is entirely identical to the one of its superclass 
 * { @link edu.vub.at.objects.natives.grammar.AGUnquote } with the difference that is ensures
 * that the value it returns when used in a quoted expression is indeed a symbol.
 *
 * @author smostinc
 */
public final class AGUnquoteSymbol extends AGUnquote implements ATSymbol {

	/**
	 * Constructor creating an unquoted expression which should evaluate to a symbol. 
	 * @param exp the expression that is to be evaluated to produce the required symbol
	 */
	public AGUnquoteSymbol(ATExpression exp) {
		super(exp);
	}
	
	/**
	 * Quoting an unquotation means evaluating its contained expression, and returning
	 * its value as the result of the quote. This method ensures that the result is 
	 * returned only if it is a proper symbol.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return super.meta_quote(ctx).asSymbol();
	}
	
	/**
	 * The use of an unquoted symbol as if it were the symbol itself indicates that the 
	 * unquotation was most probably not used in a quotation. Therefore the sematics are
	 * identical to the ones enforced in meta_eval, namely an error is reported.
	 * 
	 * @throws XIllegalUnquote
	 */
	public ATText base_getText() throws InterpreterException {
		throw new XIllegalUnquote(unqExp_);
	}

	/**
	 * The use of an unquoted symbol as if it were the symbol itself indicates that the 
	 * unquotation was most probably not used in a quotation. Therefore the sematics are
	 * identical to the ones enforced in meta_eval, namely an error is reported.
	 * 
	 * @throws XIllegalUnquote
	 */
	public boolean isAssignmentSymbol() throws InterpreterException {
		throw new XIllegalUnquote(unqExp_);
	}
	
	/**
	 * The use of an unquoted symbol as if it were the symbol itself indicates that the 
	 * unquotation was most probably not used in a quotation. Therefore the sematics are
	 * identical to the ones enforced in meta_eval, namely an error is reported.
	 * 
	 * @throws XIllegalUnquote
	 */
	public AGAssignmentSymbol asAssignmentSymbol() throws InterpreterException {
		throw new XIllegalUnquote(unqExp_);
	}
}
