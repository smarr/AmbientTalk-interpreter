/**
 * AmbientTalk/2 Project
 * AGDefMethod.java created on 26-jul-2006 at 15:44:08
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

import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefMethod;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author tvc
 *
 * The native implementation of a method definition abstract grammar element.
 */
public final class AGDefMethod extends NATAbstractGrammar implements ATDefMethod {

	private final ATSymbol selectorExp_;
	private final ATTable argumentExps_;
	private final ATBegin bodyStmts_;
	
	public AGDefMethod(ATSymbol sel, ATTable args, ATBegin bdy) {
		selectorExp_ = sel;
		argumentExps_ = args;
		bodyStmts_ = bdy;
	}
	
	public ATSymbol getSelector() {
		return selectorExp_;
	}

	public ATTable getArguments() {
		return argumentExps_;
	}

	public ATBegin getBody() {
		return bodyStmts_;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.natives.NATNil#meta_eval(edu.vub.at.objects.ATContext)
	 */
	public ATObject meta_eval(ATContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.natives.NATNil#meta_quote(edu.vub.at.objects.ATContext)
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

}
