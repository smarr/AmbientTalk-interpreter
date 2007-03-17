/**
 * AmbientTalk/2 Project
 * ActorID.java created on 17-mrt-2007 at 15:50:03
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
 * An ActorID is a serializable representation of the identity of an actor.
 * Meant to uniquely identify an actor on one AmbientTalk virtual machine.
 * Identity implemented by a pseudo-randomly generated integer.
 * 
 * @author tvcutsem
 */
public final class ActorID implements Serializable {

	private static final Random generator_ = new Random(System.currentTimeMillis());
	
	private final int id_;
	
	public ActorID() {
		id_ = generator_.nextInt();
	}
	
	public boolean equals(Object other) {
		return ((other instanceof ActorID) && ((ActorID) other).id_ == id_);
	}
	
	public int hashCode() {
		return id_;
	}
	
	public String toString() {
		return "actorid["+id_+"]";
	}
	
}
