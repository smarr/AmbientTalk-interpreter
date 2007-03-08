/**
 * AmbientTalk/2 Project
 * AGImport.java created on 6-mrt-2007 at 21:34:54
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
import edu.vub.at.eval.Import;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATImport;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.util.HashSet;
import java.util.Hashtable;

/**
 *  The public interface to a native import AST component, which is of the form:
 * 'import <expression> (alias (symbol := symbol)+ )? (exclude symbol (, symbol)* )?'
 *
 * @see edu.vub.at.objects.grammar.ATImport
 *
 * @author tvcutsem
 */
public class AGImport extends NATAbstractGrammar implements ATImport {

	private final ATExpression importedObjectExp_;

	private final ATTable aliasDeclarations_;
	
	private final ATTable excludesDeclarations_;
	
	/**
	 * For efficiency purposes, the table form of the alias mapping is preprocessed
	 * into a hashtable format.
	 */
	private final Hashtable aliasedSymbols_; // maps ATSymbols to ATSymbols
	
	/**
	 * For efficiency purposes, the table form of the excluded symbols is preprocessed
	 * into a hashset format.
	 */
	private final HashSet excludedSymbols_; // contains ATSymbols
	
	public AGImport(ATExpression importedObjectExp, ATTable aliasDeclarations, ATTable excludesDeclarations) throws InterpreterException {
		importedObjectExp_ = importedObjectExp;
		aliasDeclarations_ = aliasDeclarations;
		excludesDeclarations_ = excludesDeclarations;
		aliasedSymbols_ = Import.preprocessAliases(aliasDeclarations_);
		excludedSymbols_ = Import.preprocessExcludes(excludesDeclarations_);
	}
	
	public ATTable base_getAliasedSymbols() throws InterpreterException {
		return aliasDeclarations_;
	}

	public ATTable base_getExcludedSymbols() throws InterpreterException {
		return excludesDeclarations_;
	}

	public ATExpression base_getImportedObjectExpression() throws InterpreterException {
		return importedObjectExp_;
	}

	/**
	 * AGIMPORT(exp,aliases,excludes).eval(ctx) =
	 *   import(exp.eval(ctx), ctx, preprocess(aliases), preprocess(excludes)) ;
	 *   NIL
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return Import.performImport(importedObjectExp_.meta_eval(ctx), ctx, aliasedSymbols_, excludedSymbols_);
	}

	/**
	 * Quoting an import statement results in a new quoted import statement.
	 * 
	 * AGIMPORT(exp,aliases,excludes).quote(ctx) = AGIMPORT(exp.quote(ctx), aliases.quote(ctx), excludes.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGImport(importedObjectExp_.meta_quote(ctx).base_asExpression(),
							aliasDeclarations_.meta_quote(ctx).base_asTable(),
							excludesDeclarations_.meta_quote(ctx).base_asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		StringBuffer expression = new StringBuffer("import " + importedObjectExp_.meta_print().javaValue);
		if (aliasDeclarations_ != NATTable.EMPTY) {
			expression.append(" alias ");
			ATObject[] aliases = aliasDeclarations_.asNativeTable().elements_;
			// append first alias
			printAliasBinding(expression, aliases[0].asNativeTable());
			for (int i = 1; i < aliases.length; i++) {
				// append rest of the aliases
				expression.append(",");
				printAliasBinding(expression, aliases[i].asNativeTable());
			}
		}
		if (excludesDeclarations_ != NATTable.EMPTY) {
			expression.append(" exclude ");
			expression.append(Evaluator.printElements(excludesDeclarations_.asNativeTable().elements_, "", ",", "").javaValue);
		}
		
		return NATText.atValue(expression.toString());
	}
	
	private void printAliasBinding(StringBuffer buff, NATTable aliasBinding) throws InterpreterException {
		ATObject[] binding = aliasBinding.elements_;
		if (binding.length != 2) {
			throw new XIllegalArgument("Alias binding of import statement is not a table of size two: " + aliasBinding.meta_print().javaValue);
		}
		buff.append(binding[0].meta_print().javaValue).append(" := ").append(binding[1].meta_print().javaValue);
	}
	
}
