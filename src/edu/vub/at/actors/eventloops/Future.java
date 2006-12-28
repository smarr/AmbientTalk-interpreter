/**
 * AmbientTalk/2 Project
 * Future.java created on 27-dec-2006 at 15:57:34
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
package edu.vub.at.actors.eventloops;

/**
 * A future is a placeholder for a 'return value' that is not yet computed.
 * 
 * A future can either be resolved into a value or it can be ruined by an exception.
 * 
 * @author tvcutsem
 */
public interface Future {
	
	/**
	 * Used to resolve the future into the given result object.
	 * This will notify any threads waiting for the value.
	 */
	public void resolve(Object result);
	
	/**
	 * Used to ruin the future with the given exception.
	 * This will notify any threads waiting for the value.
	 */
	public void ruin(Exception exception);
	
	/**
	 * Used to retrieve the real value from the future.
	 * @return the resolved value of the future, once it is available.
	 * @throws Exception if the future has been ruined with an exception.
	 */
	public Object get() throws Exception;

}
