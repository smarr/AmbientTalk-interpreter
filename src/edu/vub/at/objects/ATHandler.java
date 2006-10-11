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

/**
 * Instances of the class ATHandler represent first-class exception handlers which 
 * have a filter object, describing the kind of exceptions caught by the handler and
 * a code block which acts as replacement code for the code that raised the exception.
 *
 * @author smostinc
 */
public interface ATHandler extends ATObject {
	
	/**
	 * @return the filter object of a handler specifying which exceptions it can handle. 
	 * The filter object of a handler is used for comparison on raised objects.
	 */
	public ATObject base_getFilter();
	
	/**
	 * @return the closure that can be applied given the raised exception.
	 */
	public ATClosure base_getHandler();
	
}
