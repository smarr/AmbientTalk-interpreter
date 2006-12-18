/**
 * AmbientTalk/2 Project
 * AGSplice.java created on 1-aug-2006 at 21:12:15
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

import edu.vub.at.actors.ATFarObject;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalSplice;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The native implementation of an AGSplice AG element.
 */
public class AGSplice extends AGExpression implements ATSplice {

	private final ATExpression splExp_;
	
	public AGSplice(ATExpression exp) {
		splExp_ = exp;
	}
	
	public ATExpression base_getExpression() { return splExp_; }

	/**
	 * A spliced element cannot be evaluated, but rather gives rise to an XIllegalSplice exception.
	 * This is because a splice should always be nested within either a formal parameter list or a table.
	 * 
	 * AGSPL(exp).eval(ctx) = ERROR
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		throw new XIllegalSplice(splExp_);
	}

	/**
	 * Quoting a splice means quoting its contained expression, and returning a new splice.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGSplice(splExp_.meta_quote(ctx).base_asExpression());
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("@"+ splExp_.meta_print().javaValue);
	}

	public boolean base_isSplice() { return true; }
	
	public ATSplice base_asSplice() throws XTypeMismatch {
		return this;
	}
	
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * Passing a mutable and compound object implies making a new instance of the 
     * object while invoking pass on all its constituents.
     */
    public ATObject meta_pass(ATFarObject client) throws InterpreterException {
    		return new AGSplice(splExp_.meta_pass(client).base_asExpression());
    }

    public ATObject meta_resolve() throws InterpreterException {
    		return new AGSplice(splExp_.meta_resolve().base_asExpression());
    }

}
