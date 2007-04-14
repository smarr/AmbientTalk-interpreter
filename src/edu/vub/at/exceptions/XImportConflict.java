/**
 * AmbientTalk/2 Project
 * XImportConflict.java created on 1-mrt-2007 at 14:21:56
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
package edu.vub.at.exceptions;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;

/**
 * An XImportConflict exception is raised when an import: native fails
 * because the importing object already defines one or more methods or
 * fields available in the imported object. The exception provides more
 * information about which names caused conflicts.
 *
 * @author tvcutsem
 */
public class XImportConflict extends InterpreterException {

	private final ATSymbol[] conflictingNames_;
	
	/**
	 * Constructor documenting which duplicate slots were being imported. 
	 * @param conflictingNames the names of the slots which were being imported but which already existed in the importing scope
	 * @throws InterpreterException if the slot names cannot be printed (e.g. when using custom symbols which do not provide an adequate print meta-method).
	 */
	public XImportConflict(ATSymbol[] conflictingNames) throws InterpreterException {
		super("Conflicting names during import: " + Evaluator.printElements(conflictingNames, "", ",", "").javaValue);
		conflictingNames_ = conflictingNames;
	}
	
	/**
	 * @return the names of the slots which were being imported but which already existed in the importing scope
	 */
	public ATTable getConflictingNames() {
		return NATTable.atValue(conflictingNames_);
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._IMPORTCONFLICT_;
	}

}
