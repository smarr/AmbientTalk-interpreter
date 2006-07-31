/**
 * AmbientTalk/2 Project
 * MTHObject_.java created on Jul 31, 2006 at 10:46:15 AM
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author smostinc
 *
 * MTHObject_ implements the primitive method object: and follows the pattern of all
 * MTH classes, they are singletons with a private constructor and their instance 
 * is an ATMethod, rather than the MTH object itself.
 * 
 * object: is used to create orphan objects, i.e. objects without a dynamic parent.
 */
public class MTHObject_ extends NATPrimitiveMethod {

	public static final ATMethod _INSTANCE_ = 
		new NATMethod(
				AGSymbol.alloc(NATText.atValue("object:")),
				new NATTable(new ATObject[] { 
						AGSymbol.alloc(NATText.atValue("definition")) }),
				new MTHObject_());
	
	private MTHObject_() { };
	
	/**
	 * Body expression of the object: primitive, implemented as base-level code.
	 * object: expects to be passed a closure such that it can extract the correct
	 * scope to be used as the object's lexical parent.
	 * 
	 * @throws XTypeMismatch if the definition passed to it is not a closure.
	 * @throws NATException if raised inside the definition closure.
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		ATClosure definition = getParameterValue("definition", ctx).asClosure();	
		
		ATContext definitionContext = definition.getContext();
		NATObject orphan = new NATObject(
				definitionContext.getLexicalScope().getLexicalParent());
		
		definition.getMethod().getBody().meta_eval(
				new NATContext(orphan, orphan, NATNil._INSTANCE_));
		
		return orphan;
	}

}
