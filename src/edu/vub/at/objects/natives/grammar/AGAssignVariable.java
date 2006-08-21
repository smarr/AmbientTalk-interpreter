/**
 * AmbientTalk/2 Project
 * AGAssignVariable.java created on 26-jul-2006 at 15:54:48
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
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a variable assignment AG element.
 * Examples:
 *  <tt>x := 5</tt>
 *  <tt>+ := m()</tt>
 */
public final class AGAssignVariable extends NATAbstractGrammar implements ATAssignVariable {

	private final ATSymbol variableName_;
	private final ATExpression valueExp_;
	
	public AGAssignVariable(ATSymbol nam, ATExpression val) {
		variableName_ = nam;
		valueExp_ = val;
	}
	
	public ATSymbol getName() { return variableName_; }

	public ATExpression getValue() { return valueExp_; }

	/**
	 * To evaluate a variable assignment, evaluate the right hand side and ask
	 * the current object to assign that value to the field corresponding to the left hand side.
	 * 
	 * AGASSVAR(nam,val).eval(ctx) = ctx.scope.assignVariable(nam, val.eval(ctx))
	 * 
	 * @return NIL
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		ctx.getLexicalScope().meta_assignVariable(variableName_, valueExp_.meta_eval(ctx));
		return NATNil._INSTANCE_;
	}

	/**
	 * AGASSVAR(nam,val).quote(ctx) = AGASSVAR(nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGAssignVariable(variableName_.meta_quote(ctx).asSymbol(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue(variableName_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}

}
