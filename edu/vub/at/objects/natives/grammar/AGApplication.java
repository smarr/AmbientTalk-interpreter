/**
 * AmbientTalk/2 Project
 * AGApplication.java created on 26-jul-2006 at 16:13:31
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATApplication;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of an application AG element.
 */
public final class AGApplication extends NATAbstractGrammar implements ATApplication {

	private final ATSymbol selector_;
	private final ATTable arguments_;
	
	public AGApplication(ATSymbol sel, ATTable arg) {
		selector_ = sel;
		arguments_ = arg;
	}
	
	public ATSymbol getSelector() { return selector_; }

	public ATTable getArguments() { return arguments_; }

	/**
	 * To evaluate a function application, evaluate the arguments to the function application eagerly
	 * and ask the current lexical context to call the function bound to the application's selector symbol.
	 * 
	 * AGAPL(sel,arg).eval(ctx) = ctx.scope.meta_call(sel, map eval(ctx) over arg)
	 * 
	 * @return the return value of the applied function.
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		return ctx.getLexicalScope().meta_call(selector_, NATTable.evaluateArguments(arguments_.asNativeTable(), ctx));
	}

	/**
	 * Quoting an application results in a new quoted application.
	 * 
	 * AGAPL(sel,arg).quote(ctx) = AGAPL(sel.quote(ctx), arg.quote(ctx))
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx) throws NATException {
		return new AGApplication(selector_.meta_quote(ctx).asSymbol(),
				                arguments_.meta_quote(ctx).asTable());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue(selector_.meta_print().javaValue + NATTable.printAsList(arguments_).javaValue);
	}

}
