/**
 * AmbientTalk/2 Project
 * NetworkException.java created on 2-apr-2007 at 19:35:35
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
package edu.vub.at.actors.net.comm;

import java.io.IOException;

/**
 * Raised when a VM command object could not properly be sent to its destination
 * or when the communication bus could not connect to the network.
 * 
 * @author tvcutsem
 */
public class NetworkException extends Exception {

	private final IOException cause_;
	
	public NetworkException(String msg) {
		super(msg);
		cause_ = null;
	}

	public NetworkException(String msg, IOException cause) {
		super(msg);
		cause_ = cause;
	}
	
	public IOException getCause() {
		return cause_;
	}
	
	public String getMessage() {
		if (cause_ == null) {
			return super.getMessage();
		} else {
			return getMessage() + " caused by: " + cause_.getMessage();
		}
	}

}