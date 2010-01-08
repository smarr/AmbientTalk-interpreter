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

import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.util.logging.Logging;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * This thread is responsible for opening up a {@link ServerSocket} connection that listens
 * for incoming <i>slave</i> VMs. An incoming slave connection is registered with the
 * communication bus such that a {@link CommandProcessor} can further handle communication with the slave.
 * 
 * @author jededeck
 * @author tvcutsem
 */
public class MasterConnectionThread extends Thread {
	
	private volatile ServerSocket listenSocket_;
	
	private volatile boolean isActive_ = true;
	
	private CommunicationBus communicationBus_;
	
	public MasterConnectionThread(CommunicationBus owner) {
		super("MasterConnectionThread for " + owner);
		communicationBus_ = owner;
	}
	
	/**
	 * Tries to create a local server socket connection to handle incoming
	 * slave connection requests.
	 * @param onNetwork needed for initializing the proper {@link Address} (tells me on which overlay network I should host)
	 * @return the {@link Address} encapsulating connection information to my socket
	 * @throws IOException if the local socket cannot be created. It is guaranteed that, if this
	 * exception is raised, this thread will <b>not</b> have started.
	 */
	public Address startServing() throws IOException { 
		InetAddress myAddress = InetAddress.getByName(getCurrentEnvironmentNetworkIp(communicationBus_.getIpAddress()));
		listenSocket_ = new ServerSocket(0, 50, myAddress); // create a socket that will listen on any free port
		this.start();
		return new Address(myAddress, listenSocket_.getLocalPort(), communicationBus_.getGroupName());
	}

	public void stopServing() {
		isActive_ = false;
		// we need to force the master to quit, it might be blocked on the server socket
		try {
			if (listenSocket_ != null) {
				listenSocket_.close();
			}
		} catch (IOException e) { }
	}

	/**
	 * Perpetually listen for incoming slave connections on my server socket.
	 * If a slave connects, read its address and register the incoming connection
	 * with the {@link CommunicationBus}.
	 */
	public void run() {
		try {
			Socket slave = null;
			while (isActive_) {
				
				try {
                    // accept an incoming slave connection
					slave = listenSocket_.accept();
					
					// read the slave's Address
					DataInputStream din = new DataInputStream(slave.getInputStream());
					int addressLength = din.readInt();
					byte[] address = new byte[addressLength];
					din.read(address);
					
					Address slaveAddress = Address.fromBytes(address);
					
					Logging.Network_LOG.debug("Detected incoming slave connection to " + slaveAddress);
					
					// only signal the connection of a slave if everything went OK so far
					communicationBus_.addConnection(slaveAddress, slave);
					
					slave = null;
				} catch(IOException e) {
					Logging.Network_LOG.warn(toString() + ": error setting up connection with slave: " + e.getMessage());
					// explicitly close the connection with the slave if one was created
					try {
						if (slave != null) { slave.close(); }
					} catch (IOException ioe) { }
				}
			}
		} finally {
			if (listenSocket_ != null) {
				try {
					listenSocket_.close();
				} catch (IOException e) { }
			}
			Logging.Network_LOG.debug(toString() + " shutting down.");
		}
	}
	
    /**
     * @param ipAddress is the ip address to which connect or ELVirtualMachine._DEFAULT_IP_ADDRESS_ 
     * if it was not specified. In the later case a valid IP address is calculated. 
     * @return the current environment's IP address, taking into account the Internet connection to any of the available
     * machine's Network interfaces. Examples of the outputs can be in octet or in IPV6 format.
     * Based on source code by Marcello de Sales (marcello.sales@gmail.com)
     * from <tt>http://www.jguru.com/faq/view.jsp?EID=15835</tt> (adapted from Java 1.5 to 1.4)
     * 
     * Main change on code: !addr.isSiteLocalAddress() test removed because of inconsistencies in 
     * linux and Android platforms when running under a private network configuration.
     * This change implies that devices connected to a private and public network interface,
     * getCurrentEnvironmentNetworkIp returns the first one found in the network interfaces 
     * which may not be the desired one. For those cases it is recommended to use 
     * -ip iat option to pass the desired IP address directly.
     */
    private static String getCurrentEnvironmentNetworkIp(String ipAddress) {
    	if (ELVirtualMachine._DEFAULT_IP_ADDRESS_.equals(ipAddress)) {
    		try {
    			Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
    			while (netInterfaces.hasMoreElements()) {
    				NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
    				Enumeration address = ni.getInetAddresses();
    				while (address.hasMoreElements()) {
    					InetAddress addr = (InetAddress) address.nextElement();
    					// !addr.isSiteLocalAddress() removed, see note above.
    					if (!addr.isLoopbackAddress() 
                                && !(addr.getHostAddress().indexOf(":") > -1)) {
                            return addr.getHostAddress();
                        }
    				}
    			}
    		} catch (SocketException e) { }

    		try {
    			return InetAddress.getLocalHost().getHostAddress();
    		} catch (UnknownHostException e) {
    			return "127.0.0.1";
    		}
    	} else{
    		return ipAddress;
    	}
    }

	
	public String toString() {
		return super.getName();
	}
}
