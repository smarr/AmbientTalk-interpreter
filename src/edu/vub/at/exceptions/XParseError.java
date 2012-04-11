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

import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;

import java.io.Reader;

/**
 * XParseError is thrown when illegal input is parsed by the AmbientTalk parser.
 * 
 * @author tvc
 */
public final class XParseError extends InterpreterException {

	private static final long serialVersionUID = 3910243526236096495L;

	private Reader erroneousCode_;
	
	// indicates the position of the parse error
	private final String fileName_;
	private final int line_;
	private final int column_;

	/**
	 * Convenience constructor which takes only a message and an underlying exception.
	 * @param message a message detailing the suspected reason for the parser to fail
	 * @param cause an underlying exception detailing the actual cause of this exception
	 */
	public XParseError(String message, Throwable cause) {
		super(message, cause);
		fileName_ = null;
		line_ = 0;
		column_ = 0;
	}
	
	/**
	 * Complete constructor taking information on both the error and the precise position in which
	 * the erroneous code was detected by the parser.
	 * @param erroneousCode the code snippet which could not be parsed
	 * @param message a message detailing the suspected reason for the parser to fail
	 * @param filename the file from which the parser was reading
	 * @param line the line at which the erroneous code is situated
	 * @param column the column at which the token that could not be parsed starts.
	 * @param cause an underlying exception detailing the actual cause of this exception
	 */
	public XParseError(Reader erroneousCode,
			          String message,
			          String filename,
			          int line,
			          int column,
			          Throwable cause) {
		super(message, cause);
		erroneousCode_ = erroneousCode;
		fileName_ = filename;
		line_ = line;
		column_ = column;
	}
	
	/**
	 * @return Returns the column.
	 */
	public int getColumn() {
		return column_;
	}

	/**
	 * @return Returns the fileName.
	 */
	public String getFileName() {
		return fileName_;
	}

	/**
	 * @return Returns the line.
	 */
	public int getLine() {
		return line_;
	}

	/**
	 * @return Returns the erroneous code.
	 */
	public Reader getErroneousCode() {
		return erroneousCode_;
	}
	
	/**
	 * Method overridden to provide a more elaborate message when information on the source file,
	 * the line number and column position were provided.
	 */
	public String getMessage() {
		if (fileName_ != null) {
			return fileName_ + ":" + line_ + ":" + column_ + ":" + super.getMessage();
		} else {
			return super.getMessage();
		}
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._PARSEERROR_;
	}

}
