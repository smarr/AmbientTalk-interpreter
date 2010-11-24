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

import edu.vub.at.eval.InvocationStack;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATAssignField;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

import java.util.HashMap;
import java.util.Set;

/**
 * The native implementation of a field assignment AG element.
 *  
 *  @author tvc
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
	
	public ATExpression base_receiverExpression() { return rcvExp_; }
	
	public ATSymbol base_fieldName() { return fieldName_; }

	public ATExpression base_valueExpression() { return valueExp_; }
	
	/**
	 * To evaluate a field assignment, evaluate the receiver expression, evaluate the right hand side and ask
	 * the receiver object to assign the RHS value to the field corresponding to the given field name.
	 * 
	 * In AmbientTalk using the uniform access principle, assignments are represented as message sends.
	 * For example <tt>o.x := 5</tt> is represented as <tt>o.x:=(5)</tt> where <tt>`x:=</tt> is a special
	 * selector identifying a field assignment.
	 * 
	 * AGASSFIELD(rcv,nam,val).eval(ctx) = rcv.eval(ctx).invoke(rcv,nam:=, [ val.eval(ctx) ])
	 * 
	 * @return the new value of the field
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATObject receiver = rcvExp_.meta_eval(ctx);
		NATTable arg = NATTable.of(valueExp_.meta_eval(ctx));
		ATObject result = null;
		InvocationStack stack = InvocationStack.getInvocationStack();
		try {
			stack.methodInvoked(this, receiver, arg);
			result = receiver.impl_invokeMutator(receiver, fieldName_.asAssignmentSymbol(), arg);
		} finally {
			stack.methodReturned(result);
		}
		return result;
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
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue("`" + this.meta_print().javaValue);
	}

	/**
	 * FV(rcvexp.x := exp) = FV(rcvexp) U FV(exp)
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvRcvExp = rcvExp_.impl_freeVariables();
		fvRcvExp.addAll(valueExp_.impl_freeVariables());
		return fvRcvExp;
	}
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set fvRcvExp = rcvExp_.impl_quotedFreeVariables();
		fvRcvExp.addAll(fieldName_.impl_quotedFreeVariables());
		fvRcvExp.addAll(valueExp_.impl_quotedFreeVariables());
		return fvRcvExp;
	}
	
}
