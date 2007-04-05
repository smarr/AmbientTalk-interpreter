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
import edu.vub.at.util.logging.Logging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * This thread is responsible for listening for the heartbeats of other VMs
 * and for setting up the connection with a newly discovered VM.
 * 
 * The heartbeat of a remote VM is the {@link Address} on which its {@link MasterConnectionThread}
 * is currently listening for incoming connections. When this thread hears such
 * an address (different from its own, and in the same AmbientTalk overlay network)
 * that address is compared to its own address. This comparison entitles one
 * VM address to be the <b>master</b> and one VM address to be the <b>slave</b>.
 * 
 * The rule is that the slave is always responsible for establishing an explicit
 * connection with the master. If this VM is the slave, a connection is opened
 * to the remote VM's {@link MasterConnectionThread} and that socket connection
 * is registered with the {@link CommunicationBus}. If this VM is a master,
 * it does nothing at all (except for updating the time the slave VM was last seen),
 * knowing that the slave will contact him via the {@link MasterConnectionThread}.
 * In that case, it is the socket connection defined in that thread which will
 * be registered with the {@link CommunicationBus}.
 * 
 * @author jededeck
 * @author tvcutsem
 */
public class MulticastListenerThread extends Thread {

	/**
	 * marked volatile such that all threads see the actual value of the variable,
	 * rather than a cached copy (in a local register) which may contain a stale value
	 */
	private volatile boolean isActive_ = true;
	
	/** the current network address of my communication bus */ 
	private final Address myHostAddress_;
	
	private volatile MulticastSocket socket_;
	
	private final CommunicationBus communicationBus_;
	
	public MulticastListenerThread(CommunicationBus owner, Address myHostAddress) {
		super("Multicast Listener for " + owner);
		communicationBus_ = owner;
		myHostAddress_ = myHostAddress;
	}
	
	/**
	 * Gracefully shut down the thread. Note that even after this method is invoked,
	 * the multicast listener may still accept an incoming heartbeat. Hence, care
	 * has to be taken in the communication bus that any incoming calls from this
	 * thread are ignored once this method has been invoked.
	 */
	public void stopListening() {
		isActive_ = false;
		// close the socket to ensure that no more incoming heartbeats are received,
		// and to ensure that the listener does not remain blocked on the incoming
		// socket forever
		if (socket_ != null) {
			socket_.close();
		}
	}
	
	public void run() {
		try {
			DatagramPacket packet;
			byte[] addressBuffer = new byte[Address.MAX_ADDRESS_BYTE_SIZE];
			
			while (isActive_) {
				try {
					
					// if socket is not yet initialized, initialize it now
					if (socket_ == null) {
						socket_ = new MulticastSocket(MulticastServerThread.MC_PORT);
						socket_.joinGroup(MulticastServerThread.MC_ADDR);
					}
					
					// try to receive a broadcast heartbeat
					packet = new DatagramPacket(addressBuffer, addressBuffer.length);
					socket_.receive(packet);
					
					processIncomingAddress(packet.getData());
				} catch (SocketException e) {
					// considered fatal exception for the socket, so reset the socket
					Logging.Network_LOG.warn(toString() + ": could not create socket to master:" + e.getMessage());
					socket_ = null;
				} catch (IOException e) {
					Logging.Network_LOG.warn(toString() + ": error receiving heartbeat:" + e.getMessage());
				}
			}
			
			// getting here means the listener thread is asked to stop
		} finally {
			if (socket_ != null) {
				socket_.close();
			}
			Logging.Network_LOG.debug(toString() + " shutting down");
		}
	}
	
	/**
	 * An address was received via the multicast socket.
	 * If the address is not my own address, and it is an address from the same
	 * AmbientTalk overlay network, update the time the address was last seen.
	 * If it is a new address, register it with the communication bus and
	 * determine whether this VM should play the role of master or slave in
	 * establishing a new connection.
	 */
	private void processIncomingAddress(byte[] receivedData) throws IOException {
		Address receivedAddress = Address.fromBytes(receivedData);
		
		// if the address is not of the same AmbientTalk network, disregard it
		if (!myHostAddress_.inSameNetwork(receivedAddress)) {
			Logging.Network_LOG.debug("Ignored incoming address of other network: " + receivedAddress);
			return;
		}
		
		// did I hear myself?
		if (myHostAddress_.equals(receivedAddress)) {
			return;
		}
		
		//Logging.Network_LOG.debug("Updated time last seen: " + receivedAddress);
		
		// try to update the time that this address was last seen
		boolean alreadyRegistered = communicationBus_.updateTimeLastSeen(receivedAddress);
		
		// detected a new VM: open connection to this VM
		if (!alreadyRegistered) {
		    if (isMaster(receivedAddress)) {
		    	Logging.Network_LOG.debug("Master VM detected: " + receivedAddress);
		    	// the other VM is the master: I am the slave, so I should connect to him
			    connectSlaveToMaster(receivedAddress);
		    } else {
		    	Logging.Network_LOG.debug("Slave VM detected: " + receivedAddress);
		    }
		}	
	}
	
	/**
	 * Check whether the given address is 'larger' than my address. If so,
	 * the VM denoted by this address is the master and I am slave. 
	 */
	private boolean isMaster(Address receivedAddress) {
		int value = myHostAddress_.compareTo(receivedAddress);
		if (value > 0)
			return true;
		if (value == 0)
			throw new RuntimeException("Could not determine master because address " +
					    myHostAddress_ + " is equal to " + receivedAddress);
		return false;
	}
	
	/**
	 * When two VMs detect one another, both VMs independently decide which one plays
	 * the role of master and which one plays the role of slave. The slave is responsible
	 * for opening up a connection to the master and for sending its own address explicitly
	 * to the master. When the master receives the address, it will in turn register a connection
	 * with the slave VM.
	 */
	private void connectSlaveToMaster(Address masterAddress) {
		InetAddress masterIp = masterAddress.ipAddress_;
		int masterPort = masterAddress.port_;
		Socket master = null;
		
		try {
			// connect to the master
			master = new Socket(masterIp, masterPort);
			
			// send my own address
			DataOutputStream dout = new DataOutputStream(master.getOutputStream());
			byte[] addr = myHostAddress_.serializedForm_;
			dout.writeInt(addr.length);
			dout.write(addr);
			dout.flush();
			
			// ONLY add a connection to the bus if everything went OK so far
			// this will also spawn a new command processor dedicated for handling the command objects received from the master
			communicationBus_.addConnection(masterAddress, master);
		} catch (IOException e) {
			Logging.Network_LOG.warn(toString() + ": error setting up connection with master: " + e.getMessage());
			try {
				if (master != null) {
					master.close();
				}
			} catch (IOException ioe) { }
		}
	}
	
	public String toString() {
		return super.getName();
	}
}
