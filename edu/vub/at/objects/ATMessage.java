/**
 * AmbientTalk/2 Project
 * ATMessageCreation.java created on 31-jul-2006 at 11:57:29
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author tvc
 *
 * Instances of the class ATMessage represent first-class AmbientTalk asynchronous messages.
 * They may be created explicitly using the <tt><-m(args)</tt> syntax, or implicitly during an
 * asynchronous message send of the form <tt>o<-m(args)</tt>.
 * 
 * This interface is not to be confused with the ATMessageCreation interface in the grammar subpackage.
 * That interface represents an abstract grammar object representing the syntax tree of message creation.
 * This interface is the interface to the actual runtime message object.
 */
public interface ATMessage extends ATObject {
	
	/**
	 * Messages always have a selector, a symbol denoting the field or method that 
	 * needs to be sought for.
	 * @return a symbol denoting the selector
	 */
	public ATSymbol getSelector();
	
	/**
	 * Messages may optionally have arguments.
	 * @return the arguments passed to the invocation
	 */
	public ATTable getArguments();
	
	/**
	 * Sends this message to a particular receiver object. The way in which the message
	 * send will be performed (synchronous or asynchronous) depends on the kind of message.
	 * 
	 * @param receiver the object receiving the message.
	 * @return the value of the method invocation or message send.
	 * @throws NATException if the method is not found or an error occurs while processing the method.
	 */
	public ATObject meta_sendTo(ATObject receiver) throws NATException;

}
