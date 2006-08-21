/**
 * AmbientTalk/2 Project
 * NATMessage.java created on 31-jul-2006 at 12:31:31
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

import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author tvc
 *
 * Instances of the class NATMessage represent first-class messages.
 * A NATMessage is an abstract class, as it can be either a synchronous method invocation or an asynchronous message send.
 */
public abstract class NATMessage extends NATNil implements ATMessage {

	protected final ATSymbol selector_;
	protected ATTable  arguments_;
	
	public NATMessage(ATSymbol sel, ATTable arg) {
		selector_ = sel;
		arguments_ = arg;
	}

	public ATSymbol base_getSelector() {
		return selector_;
	}

	public ATTable base_getArguments() {
		return arguments_;
	}
	
	public ATNil base_setArguments(ATTable arguments) {
		arguments_ = arguments;
		return NATNil._INSTANCE_;
	}
	
	public ATMessage asMessage() {
		return this;
	}

}
