/**
 * AmbientTalk/2 Project
 * ATException.java created on 20 sep 2007 at 13:30:00
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
package edu.vub.at.objects;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.NativeATObject;

/**
 * The public interface to an AmbientTalk exception object.
 * Although potentially every object can be raised, normally a raised
 * object understands the messages defined in this interface.
 * 
 * @author tvcutsem
 */
public interface ATException extends ATObject {

	/** Returns a string-based text message describing the exception */
	public NATText base_message() throws InterpreterException;
	
	/** Returns the stack trace in a string-based representation */
	public NATText base_stackTrace() throws InterpreterException;
	
	/**
	 * Assigns a new stack trace to this exception. The stack trace is represented
	 * as a simple string
	 */
	public NativeATObject base_stackTrace__opeql_(NATText newTrace) throws InterpreterException;
	
}
