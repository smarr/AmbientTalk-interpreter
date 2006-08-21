/**
 * AmbientTalk/2 Project
 * AGSuper.java created on 27-jul-2006 at 13:01:14
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATSuperObject;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * The abstract grammar element implementing the special pseudovariable reference named 'super'
 */
public final class AGSuper extends AGSymbol {

	private static final NATText SUPER_NAM = NATText.atValue("super");
	
	public static final AGSuper _INSTANCE_ = new AGSuper();
	
	private AGSuper() { super(SUPER_NAM); }
	
	/**
	 * To evaluate a super reference, wrap the current super from the current evaluation context.
	 * Wrapping is necessary in order to ensure that messages sent to this object are delegated:
	 * they are looked up in super, but the self remains unmodified.
	 * 
	 * AGSuper().eval(ctx) = AGSUPOBJ(ctx.self, ctx.super)
	 */
	public ATObject meta_eval(ATContext ctx) {
		return new NATSuperObject(ctx.getSelf(), ctx.getSuper());
	}
	
	/**
	 * Quoting a super reference results in the same super reference.
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return this;
	}
	
}
