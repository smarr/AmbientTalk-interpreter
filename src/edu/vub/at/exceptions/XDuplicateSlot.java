/**
 * AmbientTalk/2 Project
 * XDuplicateSlot.java created on 11-aug-2006 at 14:33:12
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

import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;


/**
 * An XDuplicateSlot exception is raised when a field or a method is added to
 * an object that already contains a field or method with the same name.
 * 
 * @author tvc
 */
public final class XDuplicateSlot extends InterpreterException {

	private static final long serialVersionUID = -1256829498207088803L;

	private final ATSymbol slotName_;
	
	/**
	 * Constructor taking the name of the duplicate slot
	 * @param slotName the already existing slot name
	 */
	public XDuplicateSlot(ATSymbol slotName) {
		super("Duplicate slot definition for " + slotName);
		slotName_ = slotName;
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._DUPLICATESLOT_;
	}

	
	/**
	 * @return the already existing slot name which was being defined anew.
	 */
	public ATSymbol getSlotName() {
		return slotName_;
	}
}
