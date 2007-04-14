/**
 * AmbientTalk/2 Project
 * SignalEscape.java created on 31-okt-2006 at 12:31:43
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
package edu.vub.at.exceptions.signals;

import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;

/**
 * A SignalEscape signal exception is raised whenever the 'quit' function of an escape block
 * is invoked. It causes control of the interpreter to return to the invocation of the escape block.
 * In other words, SignalEscape 'abuses' the Java exception handling mechanism to perform a non-local return.
 * 
 * @author tvcutsem
 */
public class SignalEscape extends Signal {
	
	private static final long serialVersionUID = -4808396148074816408L;
	
	/** The block which the escape command jumps out of */
	public ATClosure originatingBlock;
	
	/** the value being return via the escape function */
	public ATObject returnedValue;
	
	/**
	 * Constructor creating an Escape signal to prematurely exit a block and return a specific value.
	 * @param originatingBlock the current block one is jumping from.
	 * @param returnedValue the value being returned.
	 */
	public SignalEscape(ATClosure originatingBlock, ATObject returnedValue) {
		this.originatingBlock = originatingBlock;
		this.returnedValue = returnedValue;
	}

}
