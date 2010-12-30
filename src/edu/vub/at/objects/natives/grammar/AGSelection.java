/**
 * AmbientTalk/2 Project
 * AGSelection.java created on 26-jul-2006 at 16:16:07
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
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSelection;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * The native implementation of a selection AG element <tt>o.&m</tt>.
 * @author tvcutsem
 */
public final class AGSelection extends AGExpression implements ATSelection {

	private final ATExpression rcvExp_;
	private final ATSymbol selector_;
	
	public AGSelection(ATExpression rcv, ATSymbol sel) {
		rcvExp_ = rcv;
		selector_ = sel;
	}
	
	public ATExpression base_receiverExpression() { return rcvExp_; }

	public ATSymbol base_selector() { return selector_; }

	/**
	 * To evaluate a selection, evaluate the receiver expression to an object and select
	 * the slot with the given name from that object.
	 * 
	 * AGSEL(rcv,sel).eval(ctx) = rcv.eval(ctx).meta_select(sel)
	 * 
	 * @return the value of the selected slot.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATObject receiver = rcvExp_.meta_eval(ctx);
		return receiver.meta_select(receiver, selector_);
	}

	/**
	 * Quoting a selection results in a new quoted selection.
	 * 
	 * AGSEL(rcv,sel).quote(ctx) = AGSEL(rcv.quote(ctx), sel.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGSelection(rcvExp_.meta_quote(ctx).asExpression(),
				              selector_.meta_quote(ctx).asSymbol());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(rcvExp_.meta_print().javaValue + ".&" + selector_.meta_print().javaValue);
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue(rcvExp_.impl_asUnquotedCode(objectMap).javaValue + ".&" + selector_.impl_asUnquotedCode(objectMap).javaValue);
	}
	
	/**
	 * FV(rcvExp.&selector) = FV(rcvExp)
	 */
	public Set impl_freeVariables() throws InterpreterException {
		return rcvExp_.impl_freeVariables();
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = rcvExp_.impl_quotedFreeVariables();
		qfv.addAll(selector_.impl_quotedFreeVariables());
		return qfv;
	}

}
