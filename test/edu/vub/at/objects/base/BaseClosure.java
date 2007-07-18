/**
 * 
 */
package edu.vub.at.objects.base;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;

/**
 * A minimal interface to which a closure can be coerced such that it can be applied from another Java thread.
 * 
 * @author smostinc
 *
 */
public interface BaseClosure {
	
	/**
	 * Applies the closure to the given arguments, which are wrapped in a table.
	 * 
	 * @param args the evaluated arguments.
	 * @return the value of evaluating the method body in the context of the closure.
	 */
	public ATObject apply(ATTable args) throws InterpreterException;

}
