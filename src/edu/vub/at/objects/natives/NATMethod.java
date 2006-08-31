/**
 * AmbientTalk/2 Project
 * NATMethod.java created on Jul 24, 2006 at 11:30:35 PM
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author smostinc
 *
 * NATMethod implements methods as named functions which are in fact simply containers
 * for a name, a table of arguments and a body.
 */
public class NATMethod extends NATNil implements ATMethod {

	private final ATSymbol 			name_;
	private final ATTable 			parameters_;
	private final ATAbstractGrammar	body_;
	
	
	public NATMethod(ATSymbol name, ATTable parameters, ATAbstractGrammar body) {
		name_ 		= name;
		parameters_ 	= parameters;
		body_ 		= body;
	}

	public ATSymbol getName() {
		return name_;
	}

	public ATTable getArguments() {
		return parameters_;
	}

	public ATAbstractGrammar getBody() {
		return body_;
	}
	
	/**
	 * To apply a function, first bind its parameters to the evaluated arguments within a new call frame.
	 * This call frame is lexically nested within the current lexical scope.
	 * 
	 * The implementation-level invariant to be maintained is that the caller of this method should be
	 * NATClosure.meta_apply. This method should *not* be invoked without passing through NATClosure.meta_apply first.
	 * If it is, the result will be a dynamically scoped function application.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context in which to evaluate the method body
	 * @return the value of evaluating the function body
	 */
	public ATObject meta_apply(ATTable arguments, ATContext ctx) throws NATException {
		NATCallframe scope = new NATCallframe(ctx.getLexicalScope());
		NATTable.bindArguments(name_.getText().asNativeText().javaValue, scope, parameters_, arguments, true);
		return body_.meta_eval(ctx.withLexicalEnvironment(scope));
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<method:"+name_.meta_print().javaValue+">");
	}

	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}

	public boolean isMethod() {
		return true;
	}

}
