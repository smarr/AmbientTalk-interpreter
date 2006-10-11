/**
 * AmbientTalk/2 Project
 * AGAsyncMessageCreation.java created on Jul 24, 2006 at 7:45:37 PM
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATAsyncMessage;

/**
 * AGAsyncMessageCreation implements the ATAsyncMessageCreation interface natively. It is a container for the
 * message's sender, selector, and optionally a receiver and table of arguments.
 * 
 * @author smostinc
 */
public class AGAsyncMessageCreation extends AGMessageCreation {

	public AGAsyncMessageCreation(ATSymbol selector, ATTable arguments) {
		super(selector, arguments);
	}
	
	/**
	 * To evaluate an async message creation element, simply return a new native asynchronous message.
	 * It is important to note that the arguments are all eagerly evaluated.
	 * 
	 * TODO: upcall to actor here? createMessage hook.
	 * 
	 * AGAMS(sel,arg).eval(ctx) = NATAMS(ctx.self,sel, map eval(ctx) over arg)
	 * 
	 * @return a first-class asynchronous message
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		return new NATAsyncMessage(ctx.base_getSelf(), this.base_getSelector(),
				                   Evaluator.evaluateArguments(this.base_getArguments().asNativeTable(), ctx));
	}
	
	protected ATObject newQuoted(ATSymbol quotedSel, ATTable quotedArgs) {
		return new AGAsyncMessageCreation(quotedSel, quotedArgs);
	}

	protected String getMessageToken() { return "<-"; }

}
