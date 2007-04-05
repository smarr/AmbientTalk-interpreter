/**
 * AmbientTalk/2 Project
 * CommandProcessor.java created on 3-apr-2007 at 15:02:51
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
package edu.vub.at.actors.net.comm;

import edu.vub.at.actors.net.cmd.VMCommand;
import edu.vub.at.util.logging.Logging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * A command processor is responsible for:
 *  <ul>
 *   <li>Reading and executing incoming VM Command objects from an incoming socket connection
 *   <li>Signalling to the communication bus that the connection has died upon a network exception
 *  </ul>
 * 
 * Command processors are spawned as threads by the {@link MasterConnectionThread} when a
 * slave VM connects to the master VM. The command processor is responsible for accepting
 * and executing incoming command objects sent by that slave.
 * 
 * From the moment the incoming connection raises any kind of exception, the command processor
 * is stopped and the communication bus is notified that the connected slave should be removed
 * from the connections table. At that point, the thread's body will stop executing, which
 * should cause the command processor to become subject to garbage collection (because the
 * {@link MasterConnectionThread} does not store any reference to the created processor).
 * 
 * @author tvcutsem
 */
public class CommandProcessor extends Thread {

	private final Address remoteVM_;
	private final Socket connection_;
	private final ObjectInputStream inputStream_;
	private final CommunicationBus communicationBus_;
	
	/**
	 * Create a new command object where:
	 * @param remoteVM is the incoming slave VM for which to process incoming {@link VMCommand} objects.
	 * @param connection is the socket connection on which to receive the command objects.
	 * @param inputStream is the wrapped input stream of the socket.
	 * @param bus is the communication bus to notify when communication with the slave becomes disrupted
	 * @throws IOException if the socket connection could not be wrapped in an ObjectInputStream.
	 */
	public CommandProcessor(Address remoteVM, Socket connection, ObjectInputStream inputStream, CommunicationBus bus) throws IOException {
		super("CommandProcessor for " + remoteVM);
		remoteVM_ = remoteVM;
		connection_ = connection;
		inputStream_ = inputStream;
		communicationBus_ = bus;
	}
	
	/**
	 * Reads {@link VMCommand} objects from the incoming socket connection and executes them.
	 * The thread does this eternally until network connection is disrupted, or the socket is
	 * closed by the communication bus.
	 * 
	 * Note: it is the responsibility of this thread to properly unregister and remove the
	 * connection from the communication bus when an I/O error of any kind has occured.
	 */
	public void run() {
		try {
			while (true) {
				VMCommand cmd = (VMCommand) inputStream_.readObject();
				Logging.VirtualMachine_LOG.info("CommandProcessor for " + remoteVM_ + " handling incoming command: " + cmd);
			    // allow the command to execute itself
			    cmd.uponReceiptBy(communicationBus_.host_, remoteVM_);
			}
		} catch(Exception e) {
			Logging.Network_LOG.debug(toString() + ": stopping processing because of:", e);
		} finally {
			communicationBus_.removeConnection(remoteVM_, connection_);
			Logging.Network_LOG.debug(toString() + " stopped.");
		}
	}
	
	public String toString() {
		return super.getName();
	}

}
