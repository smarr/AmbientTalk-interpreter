/**
 * AmbientTalk/2 Project
 * ATImport.java created on 6-mrt-2007 at 21:17:12
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
package edu.vub.at.objects.grammar;

import edu.vub.at.exceptions.InterpreterException;

/**
 * The public interface to a native AST component of the form
 * 'import <expression> (alias (symbol := symbol)+)? (exclude symbol (, symbol)*)?'
 *
 * Examples:
 * 'import o' => (import (symbol o) (table) (table))
 * 'import o alias foo := bar' => (import (symbol o) (table (table (symbol foo) (symbol bar))) (table))
 * 'import o exclude foo, bar:' => (import (symbol o) (table) (table (symbol foo) (symbol bar:)))
 * 'import o alias a: := b, c := d: exclude e, f =>
 *      (import (symbol o) (table (table (sym a:) (sym b)) (table (sym c) (sym d:))) (table (sym e) (sym f)))
 *
 * @author tvcutsem
 */
public interface ATImport extends ATStatement {
	
	/**
	 * Example: <code>`{ import o }.statements[1].importedObjectExpression == `o</code>
	 * @return the expression that should evaluate to the object to import.
	 */
	public ATExpression base_importedObjectExpression() throws InterpreterException;
	
	/**
	 * Example: <code>`{ import o alias foo := bar }.statements[1].aliasedSymbols == `[[foo, bar]]</code>
	 * @return a table of pairs (tables of size two) of symbols that defines the mapping of symbols to alias.
	 */
	public ATExpression base_aliasedSymbols() throws InterpreterException;
	
	/**
	 * Example: <code>`{ import o exclude a, b }.statements[1].excludedSymbols == `[a,b]</code>
	 * @return a table of symbols that represent the symbols to be excluded from the imported object.
	 */
	public ATExpression base_excludedSymbols() throws InterpreterException;

}
