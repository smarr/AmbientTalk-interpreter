/**
 * AmbientTalk/2 Project
 * MTHExtend_with_.java created on Jul 31, 2006 at 11:20:47 AM
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
package edu.vub.at.methods;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author smostinc
 *
 * MTHObject_ implements the primitive method extend: with and follows the pattern of
 * all MTH classes, they are singletons with a private constructor and their instance
 * is an ATMethod, rather than the MTH object itself.
 * 
 * extend: object with: definitions is used to extend an existing object with a set  
 * of definitions which are used to create a child object. Extend creates is_a 
 * extensions implying that the parent object is to be considered an integral part 
 * of the created object.   
 */
public class MTHExtend_with_ extends NATPrimitiveMethod {

	public static final ATMethod _INSTANCE_ = 
		new NATMethod(
				AGSymbol.alloc("extend:with:"),
				new NATTable(new ATObject[] { 
						AGSymbol.alloc("object"),
						AGSymbol.alloc("definition")}),
				new MTHExtend_with_());
	
	private MTHExtend_with_() { };
	
	
	/**
	 * Body expression of the extend:with: primitive, which delegates to the extend
	 * meta operation on the parent object. 
	 * 
	 * @throws XTypeMismatch if the definition passed to it is not a closure.
	 * @throws NATException if raised inside the definition closure.
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		
		ATObject parent = getParameterValue("object", ctx);
		ATClosure definition = getParameterValue("definition", ctx).asClosure();
		
		return parent.meta_extend(definition);
	}
}
