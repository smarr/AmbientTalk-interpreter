/**
 * AmbientTalk/2 Project
 * NATMessage.java created on Jul 24, 2006 at 7:45:37 PM
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

import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author smostinc
 *
 * NATMessage implements the ATMessage interface natively. It is a container for the
 * message's sender, selector, and optionally a receiver and table of arguments.
 */
public class NATMessage extends NATNil implements ATMessage {

	private ATObject sender_ 		= null;
	private ATObject receiver_	= null;
	private ATSymbol selector_	= null;
	private ATTable  arguments_	= null;
	
	// TODO SM: Maybe we could include some abstract factory pattern to allow people to
	//      intercept message creation and supply their own objects instead ;-)
	// ---> The abstract factory is currently called ATMessageFactory and is situated in
	//      the package edu.vub.at.actors.hooks along with a ATSendStrategy. The goal
	//      is to have a vat point to one of each in this package. APPROVE?
	
	public NATMessage(ATObject sender, ATObject receiver, ATSymbol selector, ATTable arguments) {
		sender_ = sender;
		receiver_ = receiver;
		selector_ = selector;
		arguments_ = arguments;
	}

	public ATObject getSender() {
		return sender_;
	}

	public ATObject getReceiver() {
		return receiver_;
	}

	public ATSymbol getSelector() {
		return selector_;
	}

	public ATTable getArguments() throws XSelectorNotFound {
		if(arguments_ == null) {
			throw new XSelectorNotFound(
					AGSymbol.alloc(NATText.atValue("arguments")), this);
		} else {
			return arguments_;
		}
	}

}
