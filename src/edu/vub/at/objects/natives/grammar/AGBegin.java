/**
 * AmbientTalk/2 Project
 * AGBegin.java created on 26-jul-2006 at 12:26:48
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
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATText;

import java.util.HashSet;
import java.util.Set;

/**
 * AGBegin represents the abstract grammar element of a list of statements.
 * Examples:
 *  <tt>a ; b ; c</tt>
 *  
 * @author tvcutsem
 */
public final class AGBegin extends NATAbstractGrammar implements ATBegin {
	
	private final ATTable statements_;
	
	// contains a cached version of the expression's free variables
	private Set freeVars_;
	
	public AGBegin(ATTable statements) {
	  statements_ = statements;
	}
	
	/**
	 * AGBEGIN encapsulates a sequence of statements and evaluates each of these statements in turn.
	 * The return value is the value of the last statement.
	 * 
	 * AGBEGIN([statement | statements]).eval(ctx) = statement.eval(ctx) ; BEGIN(statements).eval(ctx)
	 * AGBEGIN([statement]).eval(ctx) = statement.eval(ctx)
	 * 
	 * Note that this implementation of begin is *not* tail-recursive.
	 * Tail-recursion is made impossible because Java will not allow a return
	 * to the caller until the last expression is also evaluated. Tail-recursion optimalisation
	 * requires a minimal form of explicit continuations.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		NATNumber siz = statements_.base_length().asNativeNumber();
		int lastIdx = siz.javaValue;
		if (lastIdx == 0) {
			return Evaluator.getNil();
		}
		for (int i = 1; i < lastIdx; i++) {
			statements_.base_at(NATNumber.atValue(i)).meta_eval(ctx);
		}
		return statements_.base_at(NATNumber.atValue(lastIdx)).meta_eval(ctx);
	}

	/**
	 * AGBEGIN(statements*).quote(ctx) = AGBEGIN((statements*).quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGBegin(statements_.meta_quote(ctx).asTable());
	}
	
	public ATTable base_statements() { return statements_; }
	
	public NATText meta_print() throws InterpreterException {
		return Evaluator.printAsStatements(statements_);
	}
	
	public ATBegin asBegin() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * FV({ stmt1; stmt2; ... }) = FV(stmt1) U FV(stmt2) U ... \ (IV(stmt1) U IV(stmt2) U ...) }
	 */
	public Set impl_freeVariables() throws InterpreterException {
		if (freeVars_ == null) {
			freeVars_ = new HashSet();
			final Set boundVars = new HashSet();
			statements_.base_each_(new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					ATAbstractGrammar stmt = this.get(args, 1).asAbstractGrammar();
					freeVars_.addAll(stmt.impl_freeVariables());
					if (stmt.isDefinition()) {
						boundVars.addAll(stmt.asDefinition().impl_introducedVariables());					
					}
					return stmt;
				}
			});
			freeVars_.removeAll(boundVars);
		}
		return freeVars_;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		return statements_.impl_quotedFreeVariables();
	}

}
