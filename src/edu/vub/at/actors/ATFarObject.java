/**
 * AmbientTalk/2 Project
 * ATFarObject.java created on Dec 5, 2006 at 11:19:05 PM
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
package edu.vub.at.actors;

import java.net.InetSocketAddress;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;

/**
 * ATFarObject is an object reference denoting an object hosted by another actor.
 *
 * @author smostinc
 */
public interface ATFarObject extends ATObject {

	public int getActorId();
	public int getObjectId();
	public InetSocketAddress getVirtualMachineId();
	
	public ATBoolean meta_isHostedBy_(ATActor host) throws InterpreterException;
	
	public ATNil meta_flush(ATObject destination) throws InterpreterException;
}
