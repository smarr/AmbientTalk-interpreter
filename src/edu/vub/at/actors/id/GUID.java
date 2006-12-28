/**
 * AmbientTalk/2 Project
 * GUID.java created on 21-dec-2006 at 13:43:24
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
package edu.vub.at.actors.id;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class is used to generate globally unique identifiers.
 * Implementation adapted from AmbientTalk/1.
 * 
 * @author tvcutsem
 */
public final class GUID implements Serializable {

	private static final long serialVersionUID = -1876309467164128162L;
	
	//private InetAddress address_;
	private String guid_;

	public GUID() {
		try {
			InetAddress address = InetAddress.getLocalHost();
			guid_ = String.valueOf(System.currentTimeMillis()) + address.toString();
		} catch (UnknownHostException e) {
			// should not occur for the local host
			e.printStackTrace();
			guid_ = "";
		}
	}

	public String toString() {
		return guid_;
	}
	
	public int hashCode() {
		return guid_.hashCode();
	}
	
	public boolean equals(Object o) {
		if (o instanceof GUID) {
			return guid_.equals(((GUID)o).guid_);
		}
		return false;
	}
}