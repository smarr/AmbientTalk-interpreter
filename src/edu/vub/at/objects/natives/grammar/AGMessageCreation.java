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
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * The common superclass of AGAsyncMessageCreation and AGMethodInvocationCreation.
 * This class serves as a common repository for both kinds of first-class message AG elements.
 * 
 * @author tvcutsem
 */
public abstract class AGMessageCreation extends AGExpression implements ATMessageCreation {

	private final ATSymbol selector_;
	private final ATTable arguments_;
	private final ATExpression annotations_;
	
	public AGMessageCreation(ATSymbol sel, ATTable args, ATExpression annotations) {
		selector_ = sel;
		arguments_ = args;
		annotations_ = annotations;
	}
	
	public ATSymbol base_selector() { return selector_; }

	public ATTable base_arguments() { return arguments_; }

	public ATExpression base_annotations() { return annotations_; }
	
	public boolean isMessageCreation() {
   	    return true;
    }
	
	public ATMessageCreation asMessageCreation() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * To evaluate a message send, transform the selector
	 * and evaluated arguments into a first-class Message object.
	 * It is important to note that the arguments are all eagerly evaluated.
	 * 
	 * The annotation to the message send is either included in the generated message
	 * as a table of type tags, if it evaluates to a table, or as a one-sized table of the
	 * evaluated annotation. This means that <code>o.m(x)@y</code> is
	 * evaluated as <code>o.m(x)@[y]</code>, for example.
	 * 
	 * AGMSG(sel,arg,ann).eval(ctx) = NATMSG(sel, map eval(ctx) over arg, (a:ann.eval(ctx).isTable? ? a : [a]))
	 * 
	 * @return a first-class method invocation
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		NATTable evaluatedArgs = Evaluator.evaluateArguments(arguments_.asNativeTable(), ctx);
		ATObject annotations = annotations_.meta_eval(ctx);
		return this.createMessage(ctx,
				                  selector_,
				                  evaluatedArgs,
				                  (annotations.isTable()) ? annotations.asTable() : NATTable.of(annotations));
	}
	
	/**
	 * Quoting a message creation element returns a new quoted message creation element.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return this.newQuoted(selector_.meta_quote(ctx).asSymbol(),
				              arguments_.meta_quote(ctx).asTable(),
				              annotations_.meta_quote(ctx).asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		if (annotations_ == NATTable.EMPTY) {
			return NATText.atValue(this.getMessageToken() +
		               selector_.meta_print().javaValue +
		               Evaluator.printAsList(arguments_).javaValue);
		} else {
			return NATText.atValue(this.getMessageToken() +
		               selector_.meta_print().javaValue +
		               Evaluator.printAsList(arguments_).javaValue +
		               "@" + annotations_.meta_print().javaValue);
		}
	}
	
	/**
	 * Subclasses must implement this method in order to return a new instance of themselves
	 * parameterized with their quoted arguments.
	 */
	protected abstract ATObject newQuoted(ATSymbol quotedSel, ATTable quotedArgs, ATExpression quotedAnnotations);
	
	/**
	 * Subclasses must implement this method such that the correct messaging token
	 * can be displayed when printing an AGMessageCreation element.
	 */
	protected abstract String getMessageToken();
	
	/**
	 * Subclasses must implement this method such that the correct kind of message is created
	 * for a given selector, evaluated arguments and annotation
	 */
	protected abstract ATMessage createMessage(ATContext ctx, ATSymbol selector, ATTable evaluatedArgs, ATTable annotations) throws InterpreterException;
	
}
