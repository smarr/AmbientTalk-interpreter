/**
 * AmbientTalk/2 Project
 * AGDelegationCreation.java created on 29-jan-2007 at 16:22:25
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
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATDelegation;

/**
 * The native implementation of a first-class delegating message send.
 *
 * @author tvcutsem
 */
public final class AGDelegationCreation extends AGMessageCreation {
	
	public AGDelegationCreation(ATSymbol sel, ATTable args, ATExpression annotations) {
		super(sel, args, annotations);
	}
	
	public ATMessage createMessage(ATContext ctx, ATSymbol selector, ATTable evaluatedArgs, ATTable annotations) throws InterpreterException {
		return new NATDelegation(ctx.base_receiver(), // the current 'self' is the delegator, to which 'self' should remain bound
				                 selector,
				                 evaluatedArgs,
				                 annotations);
	}

	protected ATObject newQuoted(ATSymbol quotedSel, ATTable quotedArgs, ATExpression quotedAnnotations) {
		return new AGDelegationCreation(quotedSel, quotedArgs, quotedAnnotations);
	}
	
	protected String getMessageToken() { return "^"; }

}