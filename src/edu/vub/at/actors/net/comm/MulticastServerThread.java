/**
 * AmbientTalk/2 Project
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
import java.io.*;
import java.net.*;

import edu.vub.at.util.logging.Logging;

/**
 * This thread is responsible for broadcasting a network message at a fixed
 * time rate.
 * The network message acts as a 'heartbeat' that signals to nearby virtual
 * machines that this VM is still alive.
 * 
 * The network packet that is broadcast (via UDP) contains the {@link Address}
 * by which this VM is known on the communication bus.
 * 
 * When two virtual machines 'hear' one another's heartbeat, and they were not
 * acquainted yet, one VM becomes 'master' and the other becomes 'slave':
 * the slave first connects to the master's socket, after which the master
 * connects to the slave. Determining who becomes master and who becomes
 * slave is done by comparing each other's address: the 'larger' address
 * becomes master.
 * 
 * @author jededeck
 * @author tvcutsem
 */
public class MulticastServerThread extends Thread {
	
	// this is the time interval in which a broadcast message is repeated
	public static final int REPEAT_TIME = 2000; // in milliseconds
	
	public static InetAddress MC_ADDR;
	
	static {
		try {
			MC_ADDR = InetAddress.getByName("224.0.0.1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/** this is the port onto which broadcasts are posted */
	public static final int MC_PORT = 4446;
	
	/**
	 * marked volatile such that all threads see the actual value of the variable,
	 * rather than a cached copy (in a local register) which may contain a stale value
	 */
	private volatile boolean isActive_ = true;
	
	/** the socket over which to broadcast heartbeats */
	private MulticastSocket socket_;
	
	/** the address that is emitted by each broadcast */
	private final byte[] myAddress_;

	public MulticastServerThread(Address myAddress) {
		super("MulticastServerThread for " + myAddress);
		myAddress_ = myAddress.serializedForm_;
	}
	
	public void stopBroadcasting() {
		isActive_ = false;
	}
	
	public void run() {
		try {
			while (isActive_) {
				try {
					// wait until sending another heartbeat
					try {
						sleep(REPEAT_TIME);
					} catch (InterruptedException e) { }
					
					// if socket not yet initialized, create a new one
					if (socket_ == null) {
						socket_ = new MulticastSocket(MC_PORT);
					}
					
					// send a UDP packet containing the address
					DatagramPacket packet = new DatagramPacket(myAddress_, myAddress_.length, MC_ADDR, MC_PORT);
					socket_.send(packet);
				} catch (SocketException e) {
					// considered fatal exception, reset socket
					Logging.Network_LOG.error(toString() + ": socket creation error: " + e.getMessage());
					socket_ = null;
				} catch (IOException ioe) {
					Logging.Network_LOG.error(toString() + ": error broadcasting key: " + ioe.getMessage());
				}
			}
		} finally {
			if (socket_ != null) {
				socket_.close();
			}
			Logging.Network_LOG.debug(toString() + ": shutting down.");
		}
	}
	
	public String toString() {
		return super.getName();
	}
	
}

