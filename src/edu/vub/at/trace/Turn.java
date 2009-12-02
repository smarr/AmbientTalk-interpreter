/**
 * AmbientTalk/2 Project
 * (c) Software Languages Lab, 2006 - 2009
 * Authors: Ambient Group at SOFT
 * 
 * The source code in this file is based on source code from Tyler Close's
 * Waterken server, Copyright 2008 Waterken Inc. Waterken's code is published
 * under the MIT license.
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
package edu.vub.at.trace;

import java.io.IOException;
import java.io.Serializable;

/**
 * An event loop turn identifier.
 */
public final class Turn implements Comparable<Turn>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * URI for the event loop
     */
    public final String loop;
    
    /**
     * local turn number
     */
    public final long number;
    
    /**
     * Constructs an instance.
     * @param loop      {@link #loop}
     * @param number    {@link #number}
     */
    public Turn(final String loop, final long number) {
        this.loop = loop;
        this.number = number;
    }
    
    // org.joe_e.Selfless interface
    
    public boolean equals(final Object o) {
        return null != o && Turn.class == o.getClass() &&
            number == ((Turn)o).number &&
            (null!=loop ? loop.equals(((Turn)o).loop) : null == ((Turn)o).loop);
    }
    
    public int hashCode() {
        return (null != loop ? loop.hashCode() : 0) +
               (int)(number ^ (number >>> 32)) +
               0x10097C42;
    }
    
    // java.lang.Comparable interface

    public int compareTo(final Turn o) {
        if (!(null != loop ? loop.equals(o.loop) : null == o.loop)) {
            throw new RuntimeException();
        }
        final long d = number - o.number;
        return d < 0L ? -1 : d == 0L ? 0 : 1;
    }
    
    /**
     *   {
     *     "loop" : "event-loop-name",
     *     "number" : n
     *   }
     */
    public void toJSON(JSONWriter json) throws IOException {
    	JSONWriter.ObjectWriter turn = json.startObject();
    	turn.startMember("loop").writeString(loop);
    	turn.startMember("number").writeLong(number);
    	turn.finish();
    }
}
