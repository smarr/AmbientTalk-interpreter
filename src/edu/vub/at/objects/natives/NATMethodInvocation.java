/**
 * AmbientTalk/2 Project
 * NATMethodInvocation.java created on 31-jul-2006 at 12:40:42
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
package edu.vub.at.objects.natives;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * Instances of the class NATMethodInvocation represent first-class method invocations.
 * They encapsulate a selector and arguments and may be turned into an actual invocation by invoking meta_sendTo.
 * This method provides the invocation with a receiver to apply itself to.
 * 
 * @author tvc
 */
public final class NATMethodInvocation extends NATMessage implements ATMethodInvocation {

	public NATMethodInvocation(ATSymbol sel, ATTable arg) {
		super(sel, arg);
	}

	/**
	 * To evaluate a method invocation, invoke the method corresponding to the encapsulated
	 * selector to the given receiver with the encapsulated arguments.
	 * 
	 * @return the return value of the invoked method.
	 */
	public ATObject base_sendTo(ATObject receiver) throws InterpreterException {
		return receiver.meta_invoke(receiver, selector_, arguments_);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<method invocation:"+selector_+Evaluator.printAsList(arguments_).javaValue+">");
	}

}
