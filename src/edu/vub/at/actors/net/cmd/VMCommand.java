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
import edu.vub.at.actors.net.Logging;

import java.io.Serializable;
import java.util.Vector;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;

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
	/** the default transmission timeout for JGroups synchronous message communication */
	public static final int _TRANSMISSION_TIMEOUT_ = 10000; // in milliseconds
	
	/**
	 * @param descr for debugging purposes
	 */
	public VMCommand(String descr) {
		description_ = descr;
	}
	
	public String toString() {
		return "VMCommand:"+description_;
	}
	
	/**
	 * To be overridden by subclasses to specify the behaviour to execute upon reception
	 * and execution of the command object at the recipient VM.
	 * 
	 * This code is still executed in a JGroups thread!
	 * 
	 * @param remoteHost the host at which the command arrived and is executed
	 * @param wrapper the JGroups message wrapper that was used to transport this command object
	 */
	public abstract Object uponReceiptBy(ELVirtualMachine remoteHost, Message wrapper) throws Exception;
	
	/**
	 * Sends this VM Command object asynchronously to the recipient VM.
	 */
	protected void sendAsyncUnicast(MessageDispatcher dispatcher, Address recipientVM) throws TimeoutException, SuspectedException {
		Logging.VirtualMachine_LOG.info("sending async unicast cmd: " + description_);
		// JGROUPS:castMessage(java.util.Vector dests, Message msg, int mode, long timeout)
		Vector recipients = new Vector(1);
		recipients.add(recipientVM);
		dispatcher.castMessage(
			recipients, // send to particular member
			// JGROUPS:Message.new(destination, source, Serializable)
			new Message(recipientVM, null, this),
			GroupRequest.GET_NONE, // asynchronous call, non-blocking
			0); // timeout is irrelevant
	}
	
	/**
	 * Sends this VM Command object asynchronously to all connected VMs.
	 */
	protected void sendAsyncMulticast(MessageDispatcher dispatcher) throws TimeoutException, SuspectedException {
		Logging.VirtualMachine_LOG.info("sending async multicast cmd: " + description_);
		// JGROUPS:castMessage(java.util.Vector dests, Message msg, int mode, long timeout)
		dispatcher.castMessage(
			null, // send to all members
			// JGROUPS:Message.new(destination, source, Serializable)
			new Message(null, null, this),
			GroupRequest.GET_NONE, // asynchronous call, non-blocking
			0); // timeout is irrelevant
	}
	
	/**
	 * Sends this VM Command object synchronously to the recipient VM. The recipient's
	 * address must be given, null is not allowed (i.e. broadcasting synchronously is not allowed)
	 */
	protected Object sendSynchronousUnicast(MessageDispatcher dispatcher, Address recipientVM) throws TimeoutException, SuspectedException {
		Logging.VirtualMachine_LOG.info("sending sync unicast cmd: " + description_);
		// send a discovery query message to the remote VM
		// JGROUPS:MessageDispatcher.sendMessage(destination, message, mode, timeout)
		return dispatcher.sendMessage(
			// JGROUPS:Message.new(destination, source, Serializable)
			new Message(recipientVM, null, this),
			GroupRequest.GET_FIRST,
			_TRANSMISSION_TIMEOUT_);
	}
	
}