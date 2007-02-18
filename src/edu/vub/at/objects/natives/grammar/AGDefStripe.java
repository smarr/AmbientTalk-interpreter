/**
 * AmbientTalk/2 Project
 * AGDefStripe.java created on 18-feb-2007 at 14:09:27
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
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATDefStripe;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATStripe;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * The native AST node for the code:
 *
 * def stripename;
 * def stripename < superstripename;
 *
 * @author tvcutsem
 */
public final class AGDefStripe extends NATAbstractGrammar implements ATDefStripe {

	private final ATSymbol stripeName_;
	private final ATTable parentStripeExpressions_;
	
	public AGDefStripe(ATSymbol stripeName, ATTable parentStripeExpressions) {
		stripeName_ = stripeName;
		parentStripeExpressions_ = parentStripeExpressions;
	}

	public ATTable base_getParentStripeExpressions() {
		return parentStripeExpressions_;
	}

	public ATSymbol base_getStripeName() {
		return stripeName_;
	}
	
	/**
	 * Defines a new stripe in the current scope. The return value is
	 * the new stripe.
	 * 
	 * AGDEFSTRIPE(nam,parentExps).eval(ctx) =
	 *   ctx.scope.addField(nam, STRIPE(nam, map eval(ctx) over parentExps))
	 */
	public ATObject meta_eval(final ATContext ctx) throws InterpreterException {
		// parentStripeExpressions_.map: { |parent| (reflect: parent).eval(ctx).base.asStripe() }
		ATTable parentStripes = parentStripeExpressions_.base_map_(new NativeClosure(this) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				return get(args,1).meta_eval(ctx).base_asStripe();
			}
		});
		
		ATStripe stripe = NATStripe.atValue(stripeName_, parentStripes);
		ctx.base_getLexicalScope().meta_defineField(stripeName_, stripe);
		return stripe;
	}

	/**
	 * Quoting a stripe definition results in a new quoted stripe definition.
	 * 
	 * AGDEFSTRIPE(nam,parentExps).quote(ctx) = AGDEFSTRIPE(nam.quote(ctx), parentExps.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefStripe(stripeName_.meta_quote(ctx).base_asSymbol(), parentStripeExpressions_.meta_quote(ctx).base_asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		if (parentStripeExpressions_ == NATTable.EMPTY) {
			return NATText.atValue("defstripe " + stripeName_.meta_print().javaValue);
		} else {
			return NATText.atValue("defstripe " + stripeName_.meta_print().javaValue + " <: " +
					Evaluator.printElements(parentStripeExpressions_.asNativeTable(), "", ",", "").javaValue);
		}
	}
	
}
