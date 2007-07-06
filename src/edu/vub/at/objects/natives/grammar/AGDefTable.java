/**
 * AmbientTalk/2 Project
 * AGDefTable.java created on 26-jul-2006 at 15:50:45
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
import edu.vub.at.exceptions.XIllegalIndex;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a table definition AG element.
 */
public final class AGDefTable extends NATAbstractGrammar implements ATDefTable {

	private final ATSymbol tblName_;
	private final ATExpression sizExp_;
	private final ATBegin initExp_;
	
	public AGDefTable(ATSymbol nam, ATExpression siz, ATBegin ini) {
		tblName_ = nam;
		sizExp_ = siz;
		initExp_ = ini;
	}
	
	public ATSymbol base_name() { return tblName_; }

	public ATExpression base_sizeExpression() { return sizExp_; }

	public ATBegin base_initializer() { return initExp_; }

	/**
	 * Defining a table requires evaluating its index expression to a size s,
	 * then allocating a new table of size s and finally initializing it
	 * by evaluating its initializer body s times. The return value is the defined table.
	 * 
	 * The initializer body is evaluated in a new scope, so definitions inside the initializer
	 * are not visible to the outside (table defining) scope.
	 * 
	 * AGDEFTABLE(nam,siz,ini).eval(ctx) =
	 *   s = siz.eval(ctx)
	 *   t[s] = nil
	 *   inictx = ctx[cur -> callframe(ctx.cur)]
	 *   i.from: 0 to: s do: {
	 *     t[i] := ini.eval(ctx)
	 *   }
	 *   ctx.scope.defineField(nam, AGTABLE(t))
	 *   AGTABLE(t)
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		int siz = 0;
		try {
			siz = sizExp_.meta_eval(ctx).asNativeNumber().javaValue;
		} catch (XTypeMismatch e) {
			throw new XIllegalIndex(e.getMessage());
		}
		
		NATCallframe initscope = new NATCallframe(ctx.base_lexicalScope());
		ATContext initctx = ctx.base_withLexicalEnvironment(initscope);
		ATObject[] tab = new ATObject[siz];
		for (int i = 0; i < tab.length; i++) {
			tab[i] = initExp_.meta_eval(initctx);
		}
		
		NATTable tbl = NATTable.atValue(tab);
		ctx.base_lexicalScope().meta_defineField(tblName_, tbl);
		
		return tbl;
	}

	/**
	 * Quoting a table definition results in a new quoted table definition.
	 * 
	 * AGDEFTABLE(nam,siz,ini).quote(ctx) = AGDEFTABLE(nam.quote(ctx), siz.quote(ctx), ini.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefTable(tblName_.meta_quote(ctx).asSymbol(),
				              sizExp_.meta_quote(ctx).asExpression(),
				              initExp_.meta_quote(ctx).asBegin());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " +
				tblName_.meta_print().javaValue + "[" +
				sizExp_.meta_print().javaValue + "] { " +
				initExp_.meta_print().javaValue + "}");
	}

}
