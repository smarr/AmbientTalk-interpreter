/**
 * AmbientTalk/2 Project
 * AGAssignField.java created on 26-jul-2006 at 15:54:48
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
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATAssignField;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a field assignment AG element.
 * Examples:
 *  <tt>x := 5</tt>
 *  <tt>+ := m()</tt>
 */
public final class AGAssignField extends NATAbstractGrammar implements ATAssignField {

	private final ATSymbol fieldName_;
	private final ATExpression valueExp_;
	
	public AGAssignField(ATSymbol nam, ATExpression val) {
		fieldName_ = nam;
		valueExp_ = val;
	}
	
	public ATSymbol getName() { return fieldName_; }

	public ATExpression getValue() { return valueExp_; }

	/**
	 * To evaluate a field definition, evaluate the right hand side and ask
	 * the current object to assign that value to the field corresponding to the left hand side.
	 * 
	 * AGASSFIELD(nam,val).eval(ctx) = ctx.scope.assignField(nam, val.eval(ctx))
	 * 
	 * @return the value of the right hand side.
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		ATObject val = valueExp_.meta_eval(ctx);
		ctx.getLexicalScope().meta_assignField(fieldName_, val);
		return val;
	}

	/**
	 * AGASSFIELD(nam,val).quote(ctx) = AGASSFIELD(nam.quote(ctx), val.quote(ctx))
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx) throws NATException {
		return new AGAssignField(fieldName_.meta_quote(ctx).asSymbol(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue(fieldName_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}

}
