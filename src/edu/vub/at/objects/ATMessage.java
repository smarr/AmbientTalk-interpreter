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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * ATMessage represents a first-class AmbientTalk asynchronous message.
 * <p>
 * Asynchronous messages may be created explicitly using the <tt><-m(args)</tt> syntax, or implicitly during an
 * asynchronous message send of the form <tt>o<-m(args)</tt>.
 * 
 * This interface has not to be confused with the {@link ATMessageCreation} interface in the grammar subpackage.
 * That interface represents an abstract grammar object representing the syntax tree of message creation.
 * This interface is the interface to the actual runtime message object.
 * 
 * @author tvcutsem
 */
public interface ATMessage extends ATObject {
	
	/**
	 * Returns the selector of message.
	 * <p>
	 * Messages always have a selector, a symbol denoting the field or method that needs to be sought for.
	 * 
	 * @return an {@link ATSymbol} denoting the selector
	 */
	public ATSymbol base_selector() throws InterpreterException;
	
	/**
	 * Returns the arguments passed to the invocation, if any.
	 * <p>
	 * Messages may optionally have a table of arguments.
	 * 
	 * @return an {@link ATTable} containing the arguments passed to the invocation.
	 * 
	 */
	public ATTable base_arguments() throws InterpreterException;
	
	/**
	 * Assigns the arguments passed of a first class method. 
	 * 
	 * @param arguments a table containing the arguments to be assigned.
	 * @return nil
	 */
	public ATNil base_arguments__opeql_(ATTable arguments) throws InterpreterException;
	
	/**
	 * Sends this message to a particular receiver object. The way in which the message
	 * send will be performed (synchronous or asynchronous) depends on the kind of message.
	 * 
	 * @param receiver the object receiving the message denoted by this ATMessage.
	 * @param sender the object sending the message denoted by this ATMessage.
	 * @return the {@link ATObject} representing the value of the method invocation or message send.
	 * @throws InterpreterException if the method is not found or an error occurs while processing the method.
	 */
	public ATObject base_sendTo(ATObject receiver, ATObject sender) throws InterpreterException;
	
	/**
	 * Primitive implementation of {@link #base_sendTo(ATObject, ATObject)}.
	 * @param self the AmbientTalk object to which 'sendTo' was originally sent
	 */
	public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException;

}
