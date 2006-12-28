/**
 * AmbientTalk/2 Project
 * QueueTransformator.java created on 28-dec-2006 at 11:29:16
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

import java.util.Vector;

/**
 * A helper interface used to define 'closures' that may transform the contents of
 * the event queue. QueueTransformator instances their transform method should
 * always be executed by the event loop whose event queue is being transformed.
 *
 * @author tvcutsem
 */
public interface QueueTransformator {
	
	/**
	 * Transforms the old content of the event queue into a new content.
	 * The new content will be merged together with new events that may
	 * have arrived while transforming the event queue.
	 * @param oldContent the contents of the event queue at the time transform is invoked.
	 * @return the tranformed event queue contents which will be merged with the current event queue's state.
	 */
	public Vector transform(Vector oldContent);
}
