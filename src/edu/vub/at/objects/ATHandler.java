/**
 * AmbientTalk/2 Project
 * ATHandler.java created on Sep 26, 2006 at 7:56:45 PM
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

/**
 * Instances of the class ATHandler represent first-class exception handlers which 
 * have a filter object, describing the kind of exceptions caught by the handler and
 * a code block which acts as replacement code for the code that raised the exception.
 *
 * @author smostinc
 */
public interface ATHandler extends ATObject {
	
	/**
	 * Used to determine whether a handler will be triggered when an exception is raised.
	 * Its primary use is to provide a hook to deviate from the default semantics, which is:
	 * 
	 * def canHandle(anException) {
	 *   (reflect: anException).isCloneOf(filter);
	 * };
	 * 
	 */
	public ATBoolean base_canHandle(ATObject anException) throws InterpreterException;
	
	/**
	 * When a handler has answered that it can handle an exception, the following
	 * method is invoked, asking the handler to handle the exception.
	 * The default semantics is simply to invoke the associated handler closure.
	 * 
	 * def handle(anException) {
	 *   handler(anException)
	 * };
	 * 
	 */
	public ATObject base_handle(ATObject anException) throws InterpreterException;
}
