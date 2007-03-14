/**
 * AmbientTalk/2 Project
 * AGMessageCreation.java created on 10-aug-2006 at 14:10:17
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
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The common superclass of AGAsyncMessageCreation and AGMethodInvocationCreation.
 * This class serves as a common repository for both kinds of first-class message AG elements.
 */
public abstract class AGMessageCreation extends AGExpression implements ATMessageCreation {

	protected final ATSymbol selector_;
	protected final ATTable arguments_;
	
	public AGMessageCreation(ATSymbol sel, ATTable args) {
		selector_ = sel;
		arguments_ = args;
	}
	
	public ATSymbol base_getSelector() { return selector_; }

	public ATTable base_getArguments() { return arguments_; }

	public boolean isMessageCreation() {
   	    return true;
    }
	
	public ATMessageCreation asMessageCreation() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * Quoting a message creation element returns a new quoted message creation element.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return this.newQuoted(selector_.meta_quote(ctx).asSymbol(),
				             arguments_.meta_quote(ctx).asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(this.getMessageToken() +
				               selector_.meta_print().javaValue +
				               Evaluator.printAsList(arguments_).javaValue);
	}
	
	/**
	 * Subclasses must implement this method in order to return a new instance of themselves
	 * parameterized with their quoted arguments.
	 */
	protected abstract ATObject newQuoted(ATSymbol quotedSel, ATTable quotedArgs);
	
	/**
	 * Subclasses must implement this method such that the correct messaging token
	 * can be displayed when printing an AGMessageCreation element.
	 */
	protected abstract String getMessageToken();
	
}
