/**
 * AmbientTalk/2 Project
 * XIllegalUnquote.java created on 28-jul-2006 at 16:48:23
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

import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATExpression;

/**
 * An XIllegalUnquote exception is raised when an unquotation is discovered in a non-quoted 
 * piece of source code. In other words, this exception is raised when trying to evaluate an
 * {@link edu.vub.at.objects.natives.grammar.AGUnquote} element rather than quoting it.
 * 
 * @author tvc
 */
public final class XIllegalUnquote extends InterpreterException {

	private static final long serialVersionUID = -396801039714856213L;

	private static final String _MESSAGE_ = "Unquoted expression in non-quoted expression";
	
	private final ATExpression unquotation_;
	
	/**
	 * Constructor reporting that an attempt was made to evaluate an unquoted piece of source code
	 * @param unquotation the unquoted source code being evaluated
	 * 
	 * @see edu.vub.at.objects.natives.grammar.AGUnquote#meta_eval(ATContext)
	 * @see edu.vub.at.objects.natives.grammar.AGUnquoteSplice#meta_eval(ATContext)
 	 */
	public XIllegalUnquote(ATExpression unquotation) {
		super(_MESSAGE_);
		unquotation_ = unquotation;
	}
	
	/**
	 * @return the unquoted grammar element that was being evaluated.
	 */
	public ATExpression getUnquotation() { return unquotation_; }

	public ATTypeTag getType() {
		return NativeTypeTags._ILLUQUOTE_;
	}
}
