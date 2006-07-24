/**
 * AmbientTalk/2 Project
 * ATMessage.java created on Jul 24, 2006 at 7:30:17 PM
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
package edu.vub.at.objects;

import edu.vub.at.exceptions.SelectorNotFound;

/**
 * @author smostinc
 *
 * ATMessage embody different types of messages sent in ambienttalk. They range from
 * receiverless selections (m) to receiverful invocations (o.m() | o<-m()). These 
 * messages may be sent equally well as synchronous invocations (using .) and 
 * asynchronous message sends (using <-).
 */
public interface ATMessage extends ATObject {

	/**
	 * Each message has a sender, namely the object on whose behalf the message was
	 * sent. In other words the sender of a message corresponds to the self at the 
	 * site where the message was sent.
	 */
	public ATObject getSender();
	
	/**
	 * Tests whether the message is receiverful, in other words whether the receiver
	 * was explicitly designated. This is important to determine along which parent 
	 * chain the selector is to be sought for.
	 * @return true if the message has an explicit receiver.
	 */
	public ATBoolean hasReceiver();
	
	/**
	 * Messages may optionally have an explicitly named receiver, which signals lookup
	 * of the selector should follow the dynamic parent chain rather than the lexical
	 * one.
	 * @return the receiver of the message
	 * @throws SelectorNotFound if the message did not have a receiver
	 */
	public ATObject getReceiver() throws SelectorNotFound;
	
	/**
	 * Messages always have a selector, a symbol denoting the field or method that 
	 * needs to be sought for.
	 * @return a symbol denoting the selector
	 */
	public ATSymbol getSelector();
	
	/**
	 * Messages may optionally have arguments if they represent invocations.
	 * @return the arguments passed to the invocation
	 * @throws SelectorNotFound if the message is is a selection rather than an invocation 
	 */
	public ATTable getArguments() throws SelectorNotFound ;
}
