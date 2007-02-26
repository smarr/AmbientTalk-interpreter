/**
 * AmbientTalk/2 Project
 * AGSelf.java created on 27-jul-2006 at 12:57:41
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;

/**
 * @author tvc
 *
 * The abstract grammar element denoting the symbol 'self'
 */
public final class AGSelf extends AGSymbol {

	private static final String SELF_NAM = "self";
	
	public static final AGSelf _INSTANCE_ = new AGSelf();
	
	private AGSelf() { super(SELF_NAM); }
	
	/**
	 * To evaluate a self reference, simply select the current self from the current evaluation context.
	 * 
	 * AGSelf().eval(ctx) = ctx.self
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return ctx.base_getSelf();
	}

	/**
	 * Quoting a self reference results in the same self reference.
	 */
	public ATObject meta_quote(ATContext ctx) {
		return this;
	}
	
}
