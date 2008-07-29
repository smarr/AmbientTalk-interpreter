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

import edu.vub.at.eval.InvocationStack;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.util.Set;

/**
 * The native implementation of a variable assignment AG element.
 *  
 *  @author tvc
 */
public final class AGAssignVariable extends NATAbstractGrammar implements ATAssignVariable {

	private final ATSymbol variableName_;
	private final ATExpression valueExp_;
	
	public AGAssignVariable(ATSymbol nam, ATExpression val) {
		variableName_ = nam;
		valueExp_ = val;
	}
	
	public ATSymbol base_name() { return variableName_; }

	public ATExpression base_valueExpression() { return valueExp_; }
	
	/**
	 * To evaluate a variable assignment, evaluate the right hand side and ask
	 * the current object to assign that value to the field corresponding to the left hand side.
	 * 
	 * AGASSVAR(nam,val).eval(ctx) = ctx.scope.assignVariable(nam, val.eval(ctx))
	 * 
	 * @return the value assigned to the variable
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		NATTable arg = NATTable.of(valueExp_.meta_eval(ctx));
		ATObject result = null;
		InvocationStack stack = InvocationStack.getInvocationStack();
		try {
			stack.functionCalled(this, null, arg);
			result = ctx.base_lexicalScope().impl_callMutator(variableName_.asAssignmentSymbol(), arg);
		} finally {
			stack.funcallReturned(result);
		}
		return result;
	}

	/**
	 * AGASSVAR(nam,val).quote(ctx) = AGASSVAR(nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGAssignVariable(variableName_.meta_quote(ctx).asSymbol(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(variableName_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}
    
    public boolean isVariableAssignment() {
        return true;
    }
    
    public ATAssignVariable asVariableAssignment() throws XTypeMismatch {
        return this;
    }
    
	/**
	 * FV(var := valExp) = { var } U FV(valExp)
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvValExp = valueExp_.impl_freeVariables();
		fvValExp.add(variableName_);
		return fvValExp;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = valueExp_.impl_quotedFreeVariables();
		qfv.addAll(variableName_.impl_quotedFreeVariables());
		return qfv;
	}
}
