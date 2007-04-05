/**
 * AmbientTalk/2 Project
 * VMCommand.java created on 22-feb-2007 at 13:03:56
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
package edu.vub.at.actors.net.cmd;

import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.comm.Address;

import java.io.Serializable;

/**
 * VM Command objects are the message objects that AT/2 virtual machines send to one another.
 * They are control objects sent by one VM and received by another. The receiving VM
 * executes the VM Command object's uponReceiptBy method, which allows the command object to
 * encapsulate the reception code itself.
 * 
 * By convention, all VM Command classes are prefixed with CMD.
 *
 * @author tvcutsem
 */
public abstract class VMCommand implements Serializable {
	
	private final String description_;
	
	/**
	 * @param descr for debugging purposes
	 */
	public VMCommand(String descr) {
		description_ = descr;
	}
	
	public String toString() {
		return "VMCMD["+description_+"]";
	}
	
	/**
	 * To be overridden by subclasses to specify the behaviour to execute upon reception
	 * and execution of the command object at the recipient VM.
	 * 
	 * This code is still executed in a communication thread!
	 * 
	 * @param remoteHost the host at which the command arrived and is executed
	 * @param senderAddress the address of the VM that sent this VM command object
	 */
	public abstract void uponReceiptBy(ELVirtualMachine remoteHost, Address senderAddress);
	
}