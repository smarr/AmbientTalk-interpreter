/**
 * AmbientTalk/2 Project
 * NATNumber.java created on 26-jul-2006 at 16:32:54
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
package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;

/**
 * @author tvc
 *
 * The native implementation of an AmbientTalk number.
 * A number is implemented by a Java int.
 */
public final class NATNumber extends NATAbstractGrammar implements ATNumber {
	
	public static final NATNumber ZERO = new NATNumber(0);
	public static final NATNumber ONE = new NATNumber(1);
	
	public final int javaValue;
	
	/**
	 * This method currently serves as a hook for number creation.
	 * Currently number objects are not reused, but this might change in the future.
	 */
	public static final NATNumber atValue(int javaNumber) {
		return new NATNumber(javaNumber);
	}
	
	private NATNumber(int javaNumber) {
		javaValue = javaNumber;
	}
	
	
	public boolean equals(Object other) {
		return (other instanceof NATNumber) &&
			   (javaValue == ((NATNumber) other).javaValue);
	}
	
	public NATNumber asNativeNumber() { return this; }
	
	public NATText meta_print() throws XTypeMismatch {
        return NATText.atValue(String.valueOf(javaValue));
	}

}
