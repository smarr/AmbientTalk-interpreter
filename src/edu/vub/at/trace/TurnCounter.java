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

import java.io.Serializable;

/**
 * An event loop turn counter.
 */
public class
TurnCounter implements Serializable {
	
	public interface FlipI {
        public void flip();
	}
	
    static private final long serialVersionUID = 1L;
    
    /**
     * URI for the event loop
     */
    public final String loop;

    /**
     * increment the turn counter
     */
    public final FlipI flip;
    
    /**
     * increment the anchor counter
     */
    public final Marker mark;
    
    private TurnCounter(final String loop, final FlipI flip, final Marker mark) {
        this.loop = loop;
        this.flip = flip;
        this.mark = mark;
    }
    
    /**
     * Constructs an instance.
     * @param loop  {@link #loop}
     */
    static public TurnCounter make(final String loop) {
    	
        class State implements Serializable {
            static private final long serialVersionUID = 1L;

            long turns = 0;     // id of current turn
            long anchors = 0;   // id of next anchor
        }
        
        final State m = new State();
        
        class Flip implements FlipI, Serializable {
            static private final long serialVersionUID = 1L;

            public void flip() {
                m.turns += 1;
                m.anchors = 0;
            }
        }
        class Mark implements Marker, Serializable {
            static private final long serialVersionUID = 1L;

            public Anchor apply() { return new Anchor(new Turn(loop, m.turns), m.anchors++); }
        }
        return new TurnCounter(loop, new Flip(), new Mark());
    }
}
