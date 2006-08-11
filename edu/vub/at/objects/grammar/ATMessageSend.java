/**
 * AmbientTalk/2 Project
 * ATMessageSend.java created on 26-jul-2006 at 14:58:25
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
package edu.vub.at.objects.grammar;

import edu.vub.at.objects.ATNil;

/**
 * @author tvc
 *
 * The public interface to a synchronous or asynchronous message send AG element.
 * 
 * Message sends of the form <o.m()> are transformed into <MESSAGE-SEND o <METHOD-INVOCATION m []>>
 * Message sends of the form <o<-m()> are transformed into <MESSAGE-SEND o <ASYNC-MESSAGE m []>>
 */
public interface ATMessageSend extends ATExpression {

	public ATExpression meta_getReceiver();
	
	public ATNil meta_setReceiver(ATExpression rcv);
	
	public ATMessageCreation getMessage();
	
}
