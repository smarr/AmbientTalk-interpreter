/**
 * AmbientTalk/2 Project
 * Packet.java created on 29-dec-2006 at 19:15:00
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
package edu.vub.at.actors.natives;

import edu.vub.at.actors.net.Logging;
import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Packet instances are the serialized representation of AmbientTalk messages.
 * They are sent in between both local and remote actors.
 *
 * @author tvcutsem
 */
public class Packet implements Serializable {
	
	private final byte[]	payload_;
	private final String	description_;

	public Packet(String description, ATObject object) throws XIOProblem {
		description_ = description;
		try {
			payload_ = serialize(object);
		} catch (IOException e) {
			throw new XIOProblem(e);
		}
	}
	
	public ATObject unpack() throws XIOProblem, XClassNotFound {
		try {
			return (ATObject) deserialize(payload_);
		} catch (IOException e) {
			throw new XIOProblem(e);
		} catch (ClassNotFoundException e) {
			throw new XClassNotFound(e.getMessage(), e);
		}
	}
	
	public String getDescription() { return description_; }
	
	/**
	 * To be overridden by subclasses to perform a useful action upon delivery
	 * of the packet at the destination VM host.
	 */
	public Object uponArrivalDo(ELVirtualMachine remoteHost) {
		Logging.VirtualMachine_LOG.error(this + ": unimplemented uponArrivalDo invoked on a packet");
		return null;
	}
	
	public static byte[] serialize(Object o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(out);
		stream.writeObject(o);
		stream.flush();
		stream.close();
		return out.toByteArray();
	}
	
	public static Object deserialize(byte[] b) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		ObjectInputStream instream = new ObjectInputStream(in);
		return instream.readObject();
	}
	
}
