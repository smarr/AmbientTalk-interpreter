/**
 * AmbientTalk/2 Project
 * AGAssignTable.java created on 26-jul-2006 at 15:57:37
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
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATAssignTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.natives.NATMethodInvocation;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

import java.util.HashMap;
import java.util.Set;

/**
 * The native implementation of a table assignment AG element.
 * 
 * @author tvc
 */
public final class AGAssignTable extends NATAbstractGrammar implements ATAssignTable {

	private final ATExpression tblExp_;
	private final ATExpression idxExp_;
	private final ATExpression valExp_;
	
	public AGAssignTable(ATExpression tbl, ATExpression idx, ATExpression val) {
		tblExp_ = tbl;
		idxExp_ = idx;
		valExp_ = val;
	}

	public ATExpression base_tableExpression() { return tblExp_; }

	public ATExpression base_indexExpression() { return idxExp_; }

	public ATExpression base_valueExpression() { return valExp_; }
	
	/**
	 * To evaluate a table assignment, evaluate its table expression to a valid ATTable.
	 * Next, evaluate its index expression into a valid number.
	 * Finally, evaluate its value expression and assign the result to the proper index slot of the table.
	 * 
	 * AGASSTABLE(tbl,idx,val).eval(ctx) =
	 *  tbl.eval(ctx).atPut(idx.eval(ctx), val.eval(ctx))
	 * 
	 * @return the value stored in the table
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATObject col = tblExp_.meta_eval(ctx);
		ATObject idx = idxExp_.meta_eval(ctx);
		ATObject val = valExp_.meta_eval(ctx);

		return col.meta_invoke(col, 
				new NATMethodInvocation(
						AGSymbol.jAlloc("atPut"), 
						NATTable.of(idx, val),
						NATTable.EMPTY));
	}

	/**
	 * Quoting a table assignment results in a new quoted table assignment.
	 * 
	 * AGASSTABLE(tbl,idx,val).quote(ctx) = AGASSTABLE(tbl.quote(ctx),idx.quote(ctx),val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGAssignTable(tblExp_.meta_quote(ctx).asExpression(),
				                 idxExp_.meta_quote(ctx).asExpression(),
				                 valExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(tblExp_.meta_print().javaValue + "[" +
				idxExp_.meta_print().javaValue + "] := " +
				valExp_.meta_print().javaValue);
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue(tblExp_.impl_asUnquotedCode(objectMap).javaValue + "[" +
				idxExp_.impl_asUnquotedCode(objectMap).javaValue + "] := " +
				valExp_.impl_asUnquotedCode(objectMap).javaValue);
	}	

	/**
	 * FV(tblExp[idxExp] := valExp) = FV(tblExp) U FV(idxExp) U FV(valExp)
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvTblExp = tblExp_.impl_freeVariables();
		fvTblExp.addAll(idxExp_.impl_freeVariables());
		fvTblExp.addAll(valExp_.impl_freeVariables());
		return fvTblExp;
	}
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = tblExp_.impl_quotedFreeVariables();
		qfv.addAll(idxExp_.impl_quotedFreeVariables());
		qfv.addAll(valExp_.impl_quotedFreeVariables());
		return qfv;
	}

}
