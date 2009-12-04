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
 * A marker for a point in an event loop turn where an event originated.
 */
public class Anchor implements Comparable, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * event loop turn in which the event originated
     */
    public final Turn turn;
    
    /**
     * intra-{@linkplain #turn turn} event number
     */
    public final long number;
    
    /**
     * Constructs an instance.
     * @param turn      {@link #turn}
     * @param number    {@link #number}
     */
    public Anchor(final Turn turn, final long number) {
        this.turn = turn;
        this.number = number;
    }
    
    // org.joe_e.Selfless interface
    
    public boolean equals(final Object o) {
        return null != o && Anchor.class == o.getClass() &&
            number == ((Anchor)o).number &&
            (null!=turn?turn.equals(((Anchor)o).turn):null==((Anchor)o).turn);
    }
    
    public int
    hashCode() {
        return (null != turn ? turn.hashCode() : 0) +
               (int)(number ^ (number >>> 32)) +
               0x7C42A2C4;
    }

    // java.lang.Comparable interface
    
    public int compareTo(final Object o) {
    	if (!(o instanceof Anchor)) { throw new IllegalArgumentException(); };
        final int major = turn.compareTo(((Anchor)o).turn);
        if (0 != major) { return major; }
        final long minor = number - ((Anchor)o).number;
        return minor < 0L ? -1 : minor == 0L ? 0 : 1;
    }
    
    /**
     * "anchor" : {
     *   "number" : n,
     *   "turn" : {
     *     "loop" : "event-loop-name",
     *     "number" : n
     *   }
     * }
     */
    public void toJSON(JSONWriter json) throws IOException {
    	JSONWriter.ObjectWriter anchor = json.startObject();
    	anchor.startMember("number").writeLong(number);
    	turn.toJSON(anchor.startMember("turn"));
    	anchor.finish();
    }
}
