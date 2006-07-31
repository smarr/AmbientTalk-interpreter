/**
 * AmbientTalk/2 Project
 * ATAsyncMessageCreation.java created on Jul 24, 2006 at 7:30:17 PM
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

import edu.vub.at.objects.ATTable;

/**
 * @author smostinc
 *
 * An ATAsyncMessageCreation instance is created whenever an asynchronous message send 
 * <tt>o <- m()</tt> is performed, or when a first-class async msg is created using
 * code such as <tt><- m()</tt>.
 * 
 * This interface does not describe the interface to the actual first-class message (that is the ATAsyncMessage interface).
 * It only describes the interface to the AG component representing such a message.
 */
public interface ATAsyncMessageCreation extends ATMessageCreation {
	
	/**
	 * Messages always have a selector, a symbol denoting the field or method that 
	 * needs to be sought for.
	 * @return a symbol denoting the selector
	 */
	public ATSymbol getSelector();
	
	/**
	 * Messages may optionally have arguments if they represent invocations.
	 * @return the arguments passed to the invocation
	 */
	public ATTable getArguments();
}
