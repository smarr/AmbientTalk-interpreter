/**
 * AmbientTalk/2 Project
 * NATFraction.java created on 26-jul-2006 at 16:42:48
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

import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;

/**
 * @author tvc
 *
 * The native implementation of an AmbientTalk fraction.
 * A fraction is implemented by a Java double.
 */
public final class NATFraction extends NATAbstractGrammar implements ATFraction {

	public final NATFraction INFTY = new NATFraction(Double.POSITIVE_INFINITY);
	
	public final double javaValue;
	
	/**
	 * This method currently serves as a hook for fraction creation.
	 * Currently fraction objects are not reused, but this might change in the future.
	 */
	public static final NATFraction atValue(int javaFrc) {
		return new NATFraction(javaFrc);
	}
	
	private NATFraction(double javaFrc) {
		javaValue = javaFrc;
	}
	
	public boolean equals(Object other) {
		return (other instanceof NATFraction) &&
			   (javaValue == ((NATFraction) other).javaValue);
	}
}
