/**
 * AmbientTalk/2 Project
 * CommunicationBus.java created on 2-apr-2007 at 19:30:12
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

import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.net.cmd.VMCommand;
import edu.vub.at.util.logging.Logging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For each AmbientTalk virtual machine, there exists one communication bus instance.
 * The communication bus can be in two conceptual states. It can be:
 * <ul>
 *  <li>Connected: when connected, the communication bus's {@link CommunicationBus#networkAddress_} field
 *  is set.
 *  <li>Disconnected: when disconnected, the {@link CommunicationBus#networkAddress_} field is
 *  set to null.
 * </ul>
 * 
 * A communication bus is always started in a disconnected state. Toggling between both
 * states is done using the {@link CommunicationBus#connect()} and
 * {@link CommunicationBus#disconnect()} methods.
 * <p>
 * The communication bus is the central coordinator of the network communication layer.
 * It encapsulates four important threads:
 * <ul>
 *  <li>The {@link MulticastServerThread} is responsible for sending 'heartbeat' messages
 * in a multicast fashion across the network, to notify other VMs in the network that
 * this VM is still 'alive'.
 *  <li>The {@link MulticastListenerThread} is responsible for listening for the
 *  'heartbeat' messages of other VMs, and for adding new connections in the
 *  connection table of the communication bus.
 *  <li>The {@link MasterConnectionThread} is responsible for opening a server
 *  socket that accepts incoming connections from so-called <i>slave</i> VMs.
 *   <li>A timer thread which runs a task at a fixed rate to checks the connection
 *   table for timed out VMs.
 * </ul>
 * 
 * The most important bookkeeping datastructure of the communication bus is the
 * {@link CommunicationBus#addressToConnection_} table, also referred to as the
 * <b>connection table</b>. This table stores all of the currently connected
 * VMs that the host VM knows about. Whenever a heartbeat is received, a VM's
 * connection registration in this table is updated. When a heartbeat is first
 * received, the VM is added. When a heartbeat has not been heard for longer
 * than the timeout interval, the VM is removed from the connection table.
 * <p>
 * Schematically, we have the following situation after establishing a connection
 * between a master and a slave VM (see the description of {@link MulticastListenerThread}):
 * 
 * <pre>
 *   Slave                             Master
 *    cs = new Socket(masterAddress)     ms = socket.accept()
 *       cs.in <-------------------------- ms.out
 *       cs.out -------------------------> ms.in
 * </pre>
 * 
 * It does not matter which socket is registered in the connection table. Hence,
 * once both VMs have set up a connection, the concept of 'master' and 'slave' is
 * no longer useful and both VMs become peers.
 * 
 * It is always the output stream of either socket that is used for sending messages to the other VM.
 * It is always the input stream of either socket that is used for receiving messages from the other VM.
 * 
 * @author tvcutsem
 */
public class CommunicationBus {

	/** the AmbientTalk VM that owns this communication bus */
	public final ELVirtualMachine host_;
	
	/** the name of the overlay network in which to discover AmbientTalk VMs */
	private final String groupName_;
	/** the ip address to which connect or ELVirtualMachine._DEFAULT_IP_ADDRESS_ if not specified*/
	private final String ipAddress_;
	
	/** if non-null, the communication bus is connected to the network */
	private volatile Address networkAddress_;
	
	private MulticastListenerThread mcListener_;
	private MulticastServerThread mcServer_;
	private MasterConnectionThread masterConnectionThread_;
	
	/**
	 * Maps the address of a currently connected VM to connection information
	 * such as the last time it was seen, and the socket connection.
	 * Also known as the "connection table".
	 * 
	 * This datastructure is modified by different threads:
	 *  <ul>
	 *   <li>Addition of master VMs by {@link MulticastListenerThread}
	 *   <li>Addition of slave VMs by {@link MasterConnectionThread}
	 *   <li>Removal of disconnected VMs by {@link CommandProcessor}
	 *   <li>Checking for timed out VMs by {@link CommunicationBus#timeoutDetector_}
	 *   <li>Lookup of connections by AmbientTalk event loops for message transmission
	 *  </ul>
	 * Hence, access to this datastructure must by <b>synchronized</b>!
	 */
	private final HashMap addressToConnection_;
	
	/**
	 * The timer used for checking 'stale' (i.e. timed out) connections
	 * in the connection table.
	 */
	private final Timer timeoutDetectorTimer_;
	
	/**
	 * The timer task used for removing timed out connections
	 * from the connections table.
	 */
	private TimeoutDetectorTask timeoutDetector_;
	
	/**
	 * Bookkeeping datastructure to store connection-related information
	 * of a connected VM. A connection entry is uniquely identified by
	 * means of the {@link Address} of its VM.
	 */
	private static class Connection {
		public final Socket socket_;
		/** this value is updated as new heartbeats are received */
		public long lastSeenAtTime_;
		public final ObjectOutputStream outgoing_;
		public final ObjectInputStream incoming_;
		public Connection(Socket s, long lastSeenAt) throws IOException {
			socket_ = s;
			lastSeenAtTime_ = lastSeenAt;
			// NOTE: apparently it is highly important that the ObjectOutputStream on the socket.getOutputStream() is
			// created BEFORE trying to create an ObjectInputStream on the socket.getInputStream()
			// switching the below two statements causes the master and the slave to deadlock!
			outgoing_ = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
			// The buffered output stream must be EXPLICITLY flushed, otherwise the buffered input stream
			// that is created below (but by the other VM) will block indefinitely
			outgoing_.flush();
			incoming_ = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
		}
		
		/**
		 * Closes the underlying socket. This will eventually trigger the command processor
		 * tied to this connection, which will cause this connection to be removed
		 * from the table.
		 */
		public void close() {
			try {
				socket_.close();
			} catch (IOException ioe) { }
		}
	}
	
	/**
	 * The timer task used for removing timed out connections
	 * from the connection table.
	 */
	private class TimeoutDetectorTask extends TimerTask {
		
		/**
		 * the maximum amount of time that a remote VM gets to send a new
		 * heartbeat before it is considered as being 'offline'
		 */
		private static final int MAX_RESPONSE_DELAY = 10000; // in milliseconds
		
		/**
		 * The rate at which to schedule this timer task
		 */
		public static final int DETECTION_RATE = 4000; // in milliseconds
		
		public void run() {
			closeConnectionOfMembersNotSeenIn(MAX_RESPONSE_DELAY);
		}
		
	}
	
	public CommunicationBus(ELVirtualMachine host, String ambientTalkNetworkName, String ipAddress) {
		host_ = host;
		groupName_ = ambientTalkNetworkName;
		ipAddress_ = ipAddress;
		addressToConnection_ = new HashMap();
		timeoutDetectorTimer_ = new Timer(true); // create a daemon timer
	}
	
	public String getIpAddress() {
		return ipAddress_;
	}
	
	public String getGroupName(){
		return groupName_;
	}
	/**
	 * Tries to connect the communication bus to the underlying network.
	 * @throws IOException if no server socket could be created to listen
	 * for incoming connections. If this exception is raised, it is
	 * guaranteed that the communication bus is left disconnected (i.e.
	 * it is not partially connected)
	 */
	public Address connect() throws NetworkException {
		if (networkAddress_ != null) {
			return networkAddress_; // if the bus is already connected, there is no need to connect it again
		}
		
		masterConnectionThread_ = new MasterConnectionThread(this);
		try {
			networkAddress_ = masterConnectionThread_.startServing();
		} catch (IOException e) {
			masterConnectionThread_ = null;
			throw new NetworkException("Could not connect to network:", e);
		}
		mcListener_ = new MulticastListenerThread(this, networkAddress_);
		mcServer_ = new MulticastServerThread(networkAddress_);
		mcListener_.start();
		mcServer_.start();
		
		// start detecting timed out VMs
		timeoutDetector_ = new TimeoutDetectorTask();
		timeoutDetectorTimer_.scheduleAtFixedRate(timeoutDetector_, 0, TimeoutDetectorTask.DETECTION_RATE);
		
		return networkAddress_;
	}
	
    /**
     * Called by the VM when it has disconnected from the underlying channel.
     * It gracefully shuts down all network threads, sets the network address
     * to null and removes all current connections from the connection table.
     */
	public void disconnect() {
		if (networkAddress_ == null) {
			return; // if the bus is already disconnected, there is no need to take it offline again
		}
		
		// once the bus is disconnected, the network address is set to null.
		// this ensures that no further incoming connections are allowed to be
		// registered in the addConnection method!
		networkAddress_ = null;
		masterConnectionThread_.stopServing();
		mcListener_.stopListening();
		mcServer_.stopBroadcasting();
		masterConnectionThread_ = null;
		mcListener_ = null;
		mcServer_ = null;
		
		// stop detecting timed out VMs
		timeoutDetector_.cancel();
		timeoutDetector_ = null;
		
		closeConnectionOfAllMembers();
	}
	
	/**
	 * Updates the time that the given virtual machine was last seen online.
	 * This method is invoked frequently by the {@link MulticastListenerThread}.
	 * 
	 * @param member the address of the detected virtual machine
	 * @return true if the VM was properly registered, false if the VM is not yet registered
	 * (i.e. it is not yet considered as 'online')
	 */
	protected boolean updateTimeLastSeen(Address member) {
		synchronized (addressToConnection_) {
			Connection conn = (Connection) addressToConnection_.get(member);
			if (conn != null) {
				conn.lastSeenAtTime_ = System.currentTimeMillis();
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Registers a new virtual machine connection for the given address.
	 * If this VM was a slave in the discovery process, this method is
	 * invoked by the {@link MulticastListenerThread}. If this VM was
	 * a master in the discovery process, this method is invoked by the
	 * {@link MasterConnectionThread}.
	 * 
	 * The socket's output stream will be stored in the connection table and is used
	 * for transmitting VM Commands to this VM. The socket's input stream will be
	 * coupled to a dedicated {@link CommandProcessor} which is responsible for
	 * processing incoming VM command objects.
	 * 
	 * Calling this method implicitly also triggers a memberJoined event on this VM
	 * 
	 * @throws IOException if no ObjectOutputStream can be created for the socket's output stream,
	 * or if no ObjectInputStream can be created for the socket's input stream.
	 * If this exception is raised, it is guaranteed the member is not registered in the connection table.
	 */
	protected void addConnection(Address newMember, Socket conn) throws IOException {
		if (networkAddress_ == null) {
			Logging.Network_LOG.debug("ignored connection to " + newMember + ": bus disconnected");
			return; // the bus has been disconnected, do not accept any new connections
		}
		
		// create a new connection object that can be used to send command objects to this member
		Connection registeredConnection = new Connection(conn, System.currentTimeMillis());
		
		// spawn a new command processor dedicated for handling the command objects received from this member
		CommandProcessor processor = new CommandProcessor(newMember, conn, registeredConnection.incoming_, this);

		synchronized (addressToConnection_) {
			// first check whether a connection for this member already exists
			// (may happen when an old connection for this member has not yet been deleted)
			// This case IS possible when a master receives a new connection from a slave,
			// but that master has not yet detected the disconnection of the slave's old connection
			Connection oldConnection = (Connection) addressToConnection_.get(newMember);
			if (oldConnection != null) {
				// explicitly close the connection we're about to override
				// Note: in this case, we should *not* signal to the VM that a new member joined,
				// because in the corresponding removeConnection, we will also not signal a member left
				// (i.e. the connection was restored quickly enough such that we can abstract over the disconnection)
				oldConnection.close();
			} else {
				// notify the VM of a new connection
				host_.event_memberJoined(newMember);
			}
			// register the new connection in the table (this may override an old connection)
			addressToConnection_.put(newMember, registeredConnection);
		}
		
		// only start the processor if the member is properly registered
		processor.start();
		
		Logging.Network_LOG.debug("successfully registered connection to " + newMember);
	}
	
	/**
	 * It is the responsibility of the {@link CommandProcessor} tied to the given VM
	 * to invoke this method when its connection has failed.
	 * 
	 * This is the <b>only</b> method responsible for removing entries from the
	 * connection table.
	 * 
	 * Calling this method implicitly also triggers a memberLeft event on this VM
	 */
	protected void removeConnection(Address oldMember, Socket connSocket) {
		synchronized (addressToConnection_) {
			Connection conn = (Connection) addressToConnection_.get(oldMember);
			if (conn != null) {
				// ONLY delete the connection if that connection was registered for the given
				// socket. It might be that the connection in the connection table is already
				// a NEWER connection that has OVERWRITTEN the old one.
				if (conn.socket_ == connSocket) {
					conn.close();
					addressToConnection_.remove(oldMember);
					host_.event_memberLeft(oldMember); // notify VM that the member has left
				}
				// Note: if the sockets do not match, then the connection to be
				// removed by this call has already been replaced by a more recent connection to the same address
				// See the code for addConnection for more details
				// Because the connection to be removed was already replaced, it is not necessary
				// to notify the VM that a member has left
			}
		}
	}
	
	/**
	 * The {@link MulticastListenerThread} invokes this method regularly to
	 * remove connections to VMs which have not responded for longer than the
	 * given timeout period.
	 * 
	 * Removal is done implicitly by closing timed out connections. The
	 * {@link CommandProcessor} tied to each connection is responsible for
	 * actually removing it.
	 * 
	 * Calling this method might also implicitly trigger one or more memberLeft
	 * events on this VM.
	 */
	protected void closeConnectionOfMembersNotSeenIn(long period) {
		synchronized (addressToConnection_) {
			long now = System.currentTimeMillis();
			Collection connections = addressToConnection_.values();
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				Connection c = (Connection) iter.next();
				if (now - c.lastSeenAtTime_ > period) {
					c.close(); // the entry will be deleted by the Command Processor
					Logging.Network_LOG.debug("Detected timed out VM");
				}
			}
		}
	}
	
	/**
	 * When the communication bus is explicitly disconnected from the
	 * network, the VM should be notified that all connected VMs
	 * have disconnected.
	 * 
	 * Removal is done implicitly by closing all connections. The
	 * {@link CommandProcessor} tied to each connection is responsible for
	 * actually removing them.
	 */
	private void closeConnectionOfAllMembers() {
		synchronized (addressToConnection_) {
			Collection connections = addressToConnection_.values();
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				Connection c = (Connection) iter.next();
				c.close(); // ensures the command processor will remove the entry eventually
			}
		}
	}
	
	/**
	 * Sends a VM Command object asynchronously to the recipient VM.
	 * There are no delivery guarantees for this message.
	 * If the recipient is offline, or the message times out, it is simply discarded
	 */
	public void sendAsyncUnicast(VMCommand msg, Address recipientVM) {
		Logging.Network_LOG.info("sending async unicast cmd " + msg + " to " + recipientVM);
		Connection conn;
		synchronized (addressToConnection_) {
			conn = (Connection) addressToConnection_.get(recipientVM);
		}
		if (conn != null) {
			sendOneAsyncMessage(conn, msg, recipientVM);
		}
	}
	
	/**
	 * Sends a VM Command object asynchronously to all connected VMs.
	 * There are no delivery guarantees for this message, nor is it guaranteed
	 * that all currently connected VMs will receive the message
	 */
	public void sendAsyncMulticast(VMCommand msg) {
		Logging.Network_LOG.debug("sending async multicast cmd: " + msg);
		
		// first clone the connection table such that we do not need to acquire the
		// lock for the entire duration of the multicast
		HashMap clonedConnections;
		synchronized (addressToConnection_) {
			clonedConnections  = (HashMap) addressToConnection_.clone();
		}
		
		for (Iterator iter = clonedConnections.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			Address recipient = (Address) entry.getKey();
			Connection conn = (Connection) entry.getValue();
			sendOneAsyncMessage(conn, msg, recipient);
		}
	}
	
	/**
	 * Sends the given {@link VMCommand} to the given member. With 'synchronous' transmission,
	 * it is meant that if this method returns gracefully, the caller can rest assured that
	 * the remote VM has correctly received the command object. It does not given any guarantees
	 * that the command object will have been executed remotely.
	 * 
	 * @throws NetworkException if either the given address is no longer connected, or
	 * an I/O error occurs during the transmission of the command object. If this exception is
	 * raised, the caller does not know whether the message was correctly received or not.
	 */
	public void sendSynchronousUnicast(VMCommand msg, Address recipientVM) throws NetworkException {
		Logging.Network_LOG.info("sending sync unicast cmd: " + msg + " to " + recipientVM);
		
		Connection conn;
		synchronized (addressToConnection_) {
			conn = (Connection) addressToConnection_.get(recipientVM);
		}
		
		if (conn == null) {
			throw new NetworkException("Recipient " + recipientVM + " is offline");
		}
		
		try {
			conn.outgoing_.writeObject(msg);
			conn.outgoing_.flush();
		} catch (IOException e) {
			// it is the sender's responsibility to close the connection's socket if something goes wrong
			// the corresponding CommandProcessor registered on this socket will remove the member from
			// the connection table
			conn.close();
			Logging.Network_LOG.error("Error while trying to send command " + msg + " to " + recipientVM, e);
			throw new NetworkException("Error while trying to transmit message " + msg, e);
		}
	}
	
	/**
	 * Note that an I/O exception during the transmission of a command object is <b>fatal</b>
	 * for the connection. The connection is immediately terminated by closing the socket.
	 * It is the responsibility of the {@link CommandProcessor} registered on the socket
	 * connection's input stream to remove the recipient VM from the connection table.
	 */
	private void sendOneAsyncMessage(Connection conn, VMCommand msg, Address recipientVM) {
		try {
			conn.outgoing_.writeObject(msg);
			conn.outgoing_.flush();
		} catch (IOException e) {
			// it is the sender's responsibility to close the connection's socket if something goes wrong
			// the CommandProcessor will remove the member from the connection table
			conn.close();
			Logging.Network_LOG.error("Could not send command " + msg + " to " + recipientVM + ", dropping.", e);
		}
	}
	
	public String toString() {
		return "communication bus";
	}
	
}
