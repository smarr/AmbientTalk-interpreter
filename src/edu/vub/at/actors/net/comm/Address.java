/**
 * AmbientTalk/2 Project
 * Address.java created on 2-apr-2007 at 10:52:36
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Instances of this class represent low-level network addresses of AmbientTalk
 * virtual machines.
 * 
 * An <tt>Address</tt> is frequently multicast across the network via UDP, serving
 * as the contents of a VM's <i>heartbeat</i>.
 * 
 * Implementation-wise, an <tt>Address</tt> encapsulates an IP address and a port number,
 * which is the address on which the {@link ServerSocket} of the {@link MasterConnectionThread}
 * is listening for incoming connections.
 * 
 * @author tvcutsem
 */
public class Address implements Serializable, Comparable {

	/** the maximum size of a serialized address to be transported over UDP */
	public static final int MAX_ADDRESS_BYTE_SIZE = 256;
	
	public final InetAddress ipAddress_;
	public final int port_;
	
	/** the name of the overlay network to which this AmbientTalk VM belongs */
	public final String ambientTalkNetworkName_;
	
	/** an address is serialized at construction time and cached because it is constant */
	public final byte[] serializedForm_;
	
	/** the string form is cached because it is frequently used in the compareTo method */
	private final String stringForm_;
	
	/**
	 * @throws RuntimeException if the network name is so long that the serialized form
	 * of this <tt>Address</tt> would exceed {@link Address#MAX_ADDRESS_BYTE_SIZE}.
	 */
	public Address(InetAddress ipAddress, int port, String ambientTalkNetworkName) {
		ipAddress_ = ipAddress;
		port_ = port;
		ambientTalkNetworkName_ = ambientTalkNetworkName;
		serializedForm_ = toBytes();
		stringForm_ = ipAddress.toString() + port;
		
		// check for overflow
		if (serializedForm_.length > MAX_ADDRESS_BYTE_SIZE) {
			throw new RuntimeException("Address too long: " + this.toString());
		}
	}
	
	/**
	 * Deserialize a network address manually from a byte stream.
	 */
	public static Address fromBytes(byte[] serializedForm) throws IOException {
		ByteArrayInputStream bin = new ByteArrayInputStream(serializedForm);
		DataInputStream din = new DataInputStream(bin);
		String address = din.readUTF();
		InetAddress ipAddress = InetAddress.getByName(address);
		int port = din.readInt();
		String groupName = din.readUTF();
		din.close();
		return new Address(ipAddress, port, groupName);
	}

	/**
	 * @return whether the receiver address and the parameter denote VMs that are
	 * connected to the same AmbientTalk overlay network.
	 */
	public boolean inSameNetwork(Address other) {
		return ambientTalkNetworkName_.equals(other.ambientTalkNetworkName_);
	}
	
	/**
	 * Compares two addresses based on their internal representation. There is no
	 * logical ordering defined on addresses. However, it is guaranteed that
	 * if <code>adr1.compareTo(adr2) > 0</code> then <code>adr2.compareTo(adr1) < 0</code>.
	 */
	public int compareTo(Object otherAddress) {
		return stringForm_.compareTo(((Address) otherAddress).stringForm_);
	}

	public boolean equals(Object other) {
		if (other instanceof Address) {
			Address otherAddress = (Address) other;
			return (ipAddress_.equals(otherAddress.ipAddress_) && port_ == otherAddress.port_)
			         && (ambientTalkNetworkName_.equals(otherAddress.ambientTalkNetworkName_));
		} else {
			return false;
		}
	}
	
	/**
	 * It is guaranteed that if two addresses are equal, then they map to the same hash code.
	 */
	public int hashCode() {
		return ipAddress_.hashCode() | port_;
	}
	
	public String toString() {
		return ipAddress_.toString() + ":" + port_ + "[" + ambientTalkNetworkName_ + "]";
	}
	
	private byte[] toBytes() {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream(bout);
			dout.writeUTF(ipAddress_.getHostAddress());
			dout.writeInt(port_);
			dout.writeUTF(ambientTalkNetworkName_);
			dout.close();
			return bout.toByteArray();
		} catch (IOException e) {
			Logging.VirtualMachine_LOG.fatal("Could not construct serialized address:", e);
			return null;
		}
	}
	
}
