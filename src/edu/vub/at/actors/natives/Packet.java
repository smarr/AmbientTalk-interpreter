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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATObject;

/**
 * Packet instances are the serialized representation of AmbientTalk messages.
 * They are sent in between both local and remote actors.
 *
 * @author tvcutsem
 */
public final class Packet implements Serializable {

	private static final int _UNSPECIFIED_ = 0;
	
	private final byte[]	payload_;
	private final String	description_;
	private final int	destination_;
	
	public Packet(String description, ATObject object) throws XIOProblem {
		this(_UNSPECIFIED_, description, object);
	}

	public Packet(int destination, String description, ATObject object) throws XIOProblem {
		destination_ = destination;
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
	
	public int getDestination() { return destination_; }

	public String getDescription() { return description_; }
	
	private byte[] serialize(Object o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(out);
		stream.writeObject(o);
		stream.flush();
		stream.close();
		return out.toByteArray();
	}
	
	private Object deserialize(byte[] b) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		ObjectInputStream instream = new ObjectInputStream(in);
		return instream.readObject();
	}
	
}
