/**
 * AmbientTalk/2 Project
 * AGBegin.java created on 26-jul-2006 at 12:26:48
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
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.natives.NATNumber;

/**
 * @author tvc
 *
 * AGBegin represents the abstract grammar element of a list of statements.
 * Examples:
 *  <tt>a ; b ; c</tt>
 */
public final class AGBegin extends NATAbstractGrammar implements ATBegin {

	private ATTable statements_;
	
	public AGBegin(ATTable statements) {
	  statements_ = statements;
	}
	
	/**
	 * eval(#(BEGIN (statement ; statements)),ctx) = eval(statement,ctx) ; eval(statements,ctx)
	 * eval(#(BEGIN (statement)), ctx) = eval(statement, ctx)
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		NATNumber siz = statements_.getLength().asNativeNumber();
		int lastIdx = siz.javaValue - 1;
		for (int i = 0; i < lastIdx; i++) {
			statements_.at(NATNumber.atValue(i)).asStatement().meta_eval(ctx);
		}
		return statements_.at(NATNumber.atValue(lastIdx)).asStatement().meta_eval(ctx);
	}

	/**
	 * @see edu.vub.at.objects.natives.NATNil#meta_quote(edu.vub.at.objects.ATContext)
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx) {
		// TODO Auto-generated method stub
		return super.meta_quote(ctx);
	}
	
	public ATTable getStatements() { return statements_; }

}
