/**
 * AmbientTalk/2 Project
 * AGAssignField.java created on 17-aug-2006 at 13:45:32
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
import edu.vub.at.objects.grammar.ATAssignField;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a field assignment AG element.
 * Examples:
 *  <tt>o.x := 5</tt>
 *  <tt>1.+ := m()</tt>
 */
public final class AGAssignField extends NATAbstractGrammar implements ATAssignField {

	private final ATExpression rcvExp_;
	private final ATSymbol fieldName_;
	private final ATExpression valueExp_;
	
	public AGAssignField(ATExpression rcv, ATSymbol nam, ATExpression val) {
		rcvExp_ = rcv;
		fieldName_ = nam;
		valueExp_ = val;
	}
	
	public ATExpression base_getReceiverExpression() { return rcvExp_; }
	
	public ATSymbol base_getFieldName() { return fieldName_; }

	public ATExpression base_getValueExpression() { return valueExp_; }

	/**
	 * To evaluate a field assignment, evaluate the receiver expression, evaluate the right hand side and ask
	 * the receiver object to assign the RHS value to the field corresponding to the given field name.
	 * 
	 * AGASSFIELD(rcv,nam,val).eval(ctx) = rcv.eval(ctx).assignField(nam, val.eval(ctx))
	 * 
	 * @return NIL
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATObject receiver = rcvExp_.meta_eval(ctx);
		receiver.meta_assignField(receiver, fieldName_, valueExp_.meta_eval(ctx));
		return NATNil._INSTANCE_;
	}

	/**
	 * AGASSFIELD(rcv,nam,val).quote(ctx) = AGASSFIELD(rcv.quote(ctx), nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGAssignField(rcvExp_.meta_quote(ctx).asExpression(), fieldName_.meta_quote(ctx).asSymbol(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(rcvExp_.meta_print().javaValue + "." + fieldName_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}

}
