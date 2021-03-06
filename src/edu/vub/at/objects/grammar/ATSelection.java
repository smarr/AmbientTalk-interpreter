/**
 * AmbientTalk/2 Project
 * ATSelection.java created on 26-jul-2006 at 15:02:17
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

/**
 * The public interface to a selection AG element.
 * 
 * Example: <code>o.&x</code>
 * 
 * @author tvcutsem
 */
public interface ATSelection extends ATExpression {

	/**
	 * The receiver may be any valid AmbientTalk expression
	 * Example: <code>`(o.&x).reveiverExpression == `o</code>
	 * @return the reveiver expression
	 */
	public ATExpression base_receiverExpression();
	
	/**
	 * The selector must be a literal symbol
	 * Example: <code>`(o.&x).selector == `x</code>
	 * @return the selector
	 */
	public ATSymbol base_selector();
	
}
