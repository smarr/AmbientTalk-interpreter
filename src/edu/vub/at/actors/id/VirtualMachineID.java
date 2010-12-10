/**
 * AmbientTalk/2 Project
 * VirtualMachineID.java created on 21-dec-2006 at 13:43:24
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
import java.util.Random;

/**
 * A VirtualMachineID is a serializable representation of the identity of an AmbientTalk Virtual Machine.
 *
 * @author tvcutsem
 */
public final class VirtualMachineID implements Serializable {
	
	private static final long serialVersionUID = -1876309467163127162L;
	
	private static final Random generator_ = new Random(System.currentTimeMillis());
	
	private final long id_;
	
	public VirtualMachineID() {
		id_ = generator_.nextLong();
	}
	
	public boolean equals(Object other) {
		return ((other instanceof VirtualMachineID) && ((VirtualMachineID) other).id_ == id_);
	}
	
	public int hashCode() {
		return (int) id_;
	}
	
	public long getID(){
		return id_;
	}
	
	public String toString() {
		return "vmid["+id_+"]";
	}
	
}