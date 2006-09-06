/**
 * AmbientTalk/2 Project
 * XParseError.java created on 29-jul-2006 at 14:36:07
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
 * @author tvc
 *
 * XParseError is thrown when illegal input is parsed by the AmbientTalk parser.
 */
public final class XParseError extends NATException {

	// for debugging purposes, this string indicates in which file the parse error occurred.
	private String originatingFile_ = null;
	
	public XParseError() {
		super();
	}

	public XParseError(String message, Throwable cause) {
		super(message, cause);
	}

	public XParseError(String message) {
		super(message);
	}

	public XParseError(Throwable cause) {
		super(cause);
	}
	
	public void setOriginatingFile(String file) {
		originatingFile_ = file;
	}
	
	public String getOriginatingFile() {
		return originatingFile_;
	}
	
	public String getMessage() {
		if (originatingFile_ == null)
			return super.getMessage();
		else
			return super.getMessage() + " In file " + originatingFile_;
	}

}