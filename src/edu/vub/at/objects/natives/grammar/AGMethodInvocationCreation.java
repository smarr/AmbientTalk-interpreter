/**
 * AmbientTalk/2 Project
 * AGMethodInvocationCreation.java created on 26-jul-2006 at 16:18:10
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
import edu.vub.at.objects.grammar.ATMethodInvocationCreation;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATMethodInvocation;

/**
 * @author tvcutsem
 *
 * The native implementation of a first-class message creation AG element.
 */
public final class AGMethodInvocationCreation extends AGMessageCreation
      										  implements ATMethodInvocationCreation {
	
	public AGMethodInvocationCreation(ATSymbol sel, ATTable args, ATExpression annotations) {
		super(sel, args, annotations);
	}

	/**
	 * To evaluate a first-class synchronous message creation AG element, transform the selector
	 * and evaluated arguments into a first-class MethodInvocation.
	 * It is important to note that the arguments are all eagerly evaluated.
	 * 
	 * AGMSG(sel,arg,ann).eval(ctx) = NATMSG(sel, map eval(ctx) over arg, map eval(ctx) over ann)
	 * 
	 * @return a first-class method invocation
	 */
	public ATMessage createMessage(ATContext ctx, ATSymbol selector, ATTable evaluatedArgs, ATTable annotations) throws InterpreterException {
		return new NATMethodInvocation(selector,
				                       evaluatedArgs,
				                       annotations);
	}
	

	protected ATObject newQuoted(ATSymbol quotedSel, ATTable quotedArgs, ATExpression quotedAnnotations) {
		return new AGMethodInvocationCreation(quotedSel, quotedArgs, quotedAnnotations);
	}
	
	protected String getMessageToken() { return "."; }

}
