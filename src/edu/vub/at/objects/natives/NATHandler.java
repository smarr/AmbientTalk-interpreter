/**
 * AmbientTalk/2 Project
 * NATHandler.java created on Oct 14, 2006 at 8:06:53 AM
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATObject;

/**
 * 
 * TODO document the class NATHandler
 *
 * @author smostinc
 */
public class NATHandler extends NATNil implements ATHandler {

	private final ATObject filter_;
	private final ATClosure handler_;
	
	public NATHandler(ATObject filter, ATClosure handler) {
		this.filter_ = filter;
		this.handler_ = handler;
	}

	public ATBoolean base_canHandle(ATObject anException) throws InterpreterException {
		return anException.meta_isRelatedTo(filter_);
	}
	
	public ATObject base_handle(ATObject anException) throws InterpreterException {
		return handler_.base_apply(NATTable.atValue(new ATObject[] { anException }));
	}
	
	public ATHandler base_asHandler() {
		return this;
	}

}
