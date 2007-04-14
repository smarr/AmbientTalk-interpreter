/**
 * AmbientTalk/2 Project
 * XIOProblem.java created on 6-sep-2006 at 16:17:42
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

import java.io.IOException;

/**
 * The class XIOProblem is a wrapper class for Java IOExceptions. It is thrown when errors 
 * occur during the serialisation of ATObjects to Packets (and the inverse), to schedule 
 * messages for transmission to another actor. Another class that raises these exceptions 
 * is NATNamespace to report that it failed to read in a particular file to lazily load a
 * library.
 * 
 * @see edu.vub.at.actors.natives.Packet
 * @see edu.vub.at.objects.natives.NATNamespace
 * 
 * @author tvc
 */
public class XIOProblem extends InterpreterException {

	private static final long serialVersionUID = -2356169455943359670L;

	/**
	 * Constructor wrapping an IOExceptions to be reported to the AmbientTalk interpreter.
	 * @param cause the underlying exception to be reported.
	 */
	public XIOProblem(IOException cause) {
		super(cause);
	}

	public String getMessage() {
		return  getCause().getClass() + ": " + getCause().getMessage();
	}

	public ATStripe getStripeType() {
		return NativeStripes._IOPROBLEM_;
	}
}
