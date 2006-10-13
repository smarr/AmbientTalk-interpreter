/**
 * AmbientTalk/2 Project
 * XIllegalApplication.java created on 10-aug-2006 at 10:15:12
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

/**
 * XIllegalApplication is thrown when a natively implemented method is applied incorrectly.
 * This is often the case when an anonymous method (implemented in Java) is applied instead
 * of its surrounding closure.
 * 
 * @author tvc
 */
public final class XIllegalApplication extends InterpreterException {

	private static final long serialVersionUID = 1754890282401843233L;

	private final Class nativeImplementor_;
	
	/**
	 * @param message a description of the illegal application
	 * @param nativeImplementor the class of the anonymous native lambda that was erroneously invoked
	 */
	public XIllegalApplication(String message, Class nativeImplementor) {
		super(message + "(implementor: " + nativeImplementor.getName() + ")");
		nativeImplementor_ = nativeImplementor;
	}
	
	public Class getImplementorClass() {
		return nativeImplementor_;
	}

}
