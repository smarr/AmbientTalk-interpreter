/**
 * AmbientTalk/2 Project
 * AGTabulation.java created on 26-jul-2006 at 16:19:54
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
import edu.vub.at.exceptions.XIllegalIndex;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATTabulation;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of a tabulaton AG element.
 */
public final class AGTabulation extends AGExpression implements ATTabulation {

	private final ATExpression tblExp_;
	private final ATExpression idxExp_;
	
	public AGTabulation(ATExpression tbl, ATExpression idx) {
		tblExp_ = tbl;
		idxExp_ = idx;
	}
	
	public ATExpression getTable() { return tblExp_; }

	public ATExpression getIndex() { return idxExp_; }

	/**
	 * To evaluate a tabulation, evaluate the tabulation expression to a table,
	 * evaluate the index expression into a valid index, and then access the table at the given index slot.
	 * 
	 * AGTBL(tbl,idx).eval(ctx) = tbl.eval(ctx).at(idx.eval(ctx))
	 * 
	 * @return the value of the indexed table slot.
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		ATTable tab = tblExp_.meta_eval(ctx).asTable();
		ATNumber idx = null;
		try {
			idx = idxExp_.meta_eval(ctx).asNumber();
		} catch (XTypeMismatch e) {
			throw new XIllegalIndex(e.getMessage());
		}
		return tab.base_at(idx);
	}

	/**
	 * Quoting a tabulation results in a new quoted tabulation
	 * 
	 * AGTBL(tbl,idx).quote(ctx) = AGTBL(tbl.quote(ctx),idx.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGTabulation(tblExp_.meta_quote(ctx).asExpression(),
				               idxExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue(tblExp_.meta_print().javaValue + "[" +
				              idxExp_.meta_print().javaValue + "]");
	}

}
