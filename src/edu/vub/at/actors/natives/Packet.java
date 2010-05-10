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

import edu.vub.at.actors.net.SerializationException;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.objects.ATObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * Packet instances are the serialized representation of AmbientTalk messages or other objects.
 * They are exchanged directly in between local actors and in a wrapped VMCommand object between
 * remote actors.
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
	
	public Packet(ATObject object) throws XIOProblem {
		this(object.toString(), object);
	}
	
	/**
	 * Deserialize this message using the default class loader. 
	 * In 99% of all cases this is okay, but on the Dalvik VM (used by Android)
	 * it needs a specific classloader. It also fails on java6 systems sometimes.
	 * Therefore, we try all three flavors of classloaders.
	 */
	public ATObject unpack() throws InterpreterException {
		try {
			ClassLoader c = this.getClass().getClassLoader();
			return unpackUsingClassLoader(c);
		} catch ( XClassNotFound e) {
			try {
				ClassLoader s = ClassLoader.getSystemClassLoader();	
				return unpackUsingClassLoader(s);
			} catch (XClassNotFound ef) {
				ClassLoader t = Thread.currentThread().getContextClassLoader();
				return unpackUsingClassLoader(t);
			}
		}
	}
	
	/** deserialize this message, using a custom class loader to load the classes into the JVM */
	public ATObject unpackUsingClassLoader(ClassLoader cld) throws InterpreterException {
		try {
			return (ATObject) deserialize(payload_, cld);
        } catch (SerializationException e) {
          throw e.getWrappedException();
		} catch (IOException e) {
			throw new XIOProblem(e);
		} catch (ClassNotFoundException e) {
			throw new XClassNotFound(e.getMessage(), e);
		} 
	}
	
	/**
	 * Beware: the same object serialized into 2 different packets results in
	 * 2 non-equal packets. It is NOT the case that
	 * <code>new Packet(o).equals(new Packet(o))</code>
	 */
	public boolean equals(Object other) {
		return ((other instanceof Packet) &&
				((Packet) other).payload_.equals(payload_));
	}
	
	public int hashCode() { return payload_.hashCode(); }
	
	public String toString() { return "packet["+description_+"]"; }
	
	private static byte[] serialize(Object o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(out);
		stream.writeObject(o);
		stream.flush();
		stream.close();
		return out.toByteArray();
	}
	
	private static Object deserialize(byte[] b) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		ObjectInputStream instream = new ObjectInputStream(in);
		return instream.readObject();
	}
	
	/**
	 * A dedicated subclass of {@link ObjectInputStream} that hooks into
	 * that class's facility to define additional sources to look for classes
	 * that are read from the input stream.
	 */
	private static class HookedObjectInputStream extends ObjectInputStream {
		private final ClassLoader loader_;
		protected HookedObjectInputStream(ClassLoader cld, InputStream is) throws IOException, SecurityException {
			super(is);
			loader_ = cld;
		}

		protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			return loader_.loadClass(desc.getName());
		}
		
	    /**
	     * Creates the proxy class that implements the interfaces specified in
	     * {@code interfaceNames}.
	     */
	    protected Class resolveProxyClass(String[] interfaceNames)
	            throws IOException, ClassNotFoundException {
	        Class[] interfaces = new Class[interfaceNames.length];
	        for (int i = 0; i < interfaceNames.length; i++) {
	            interfaces[i] = Class.forName(interfaceNames[i], false, loader_);
	        }
	        try {
	            return Proxy.getProxyClass(loader_, interfaces);
	        } catch (IllegalArgumentException e) {
	            throw new ClassNotFoundException(e.toString(), e);
	        }
	    }
	}
	
	// deserialize and use the given class loader to try and load any missing classes
	private static Object deserialize(byte[] b, ClassLoader cld) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		ObjectInputStream instream = new HookedObjectInputStream(cld, in);
		return instream.readObject();
	}
	
}
