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
 * A source code location.
 */
public class CallSite implements Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * call site's human meaningful name within the {@linkplain #source}
     */
    public final String name;

    /**
     * path to the source code containing the call site
     */
    public final String source;
    
    /**
     * call site's position within the {@linkplain #source} (optional)
     * <p>
     * The expected structure of this table defines a span from the start of the
     * relevant source code to the end. The first row in the table is the start
     * of the span and the second row is the end of the span. Each row lists the
     * line number followed by the column number. For example, a span of code
     * starting on line 5, column 8 and ending on line 6, column 12 is encoded
     * as:
     * </p>
     * <p>
     * <code>[ [ 5, 8 ], [ 6, 12 ] ]</code>
     * </p>
     * <p>
     * The delimited span is inclusive, meaning the character at line 6, column
     * 12 is included in the span defined above.
     * </p>
     * <p>
     * If the end of the span is unknown, it may be omitted. If the column
     * number is unknown, it may also be omitted. For example, in the case where
     * only the starting line number is known:
     * </p>
     * <p>
     * <code>[ [ 5 ] ]</code>
     * </p>
     * <p>
     * If source span information is unknown, this member is <code>null</code>.
     * </p>
     * <p>
     * Both lines and columns are numbered starting from one, so the first
     * character in a source file is at <code>[ 1, 1 ]</code>. A column is a
     * UTF-16 code unit, the same unit represented by a Java <code>char</code>.
     * Lines are separated by any character sequence considered a Unicode <a
     * href="http://en.wikipedia.org/wiki/Newline#Unicode">line terminator</a>.
     * </p>
     */
    public final int[][] span;
    
    /**
     * Constructs an instance.
     * @param name      {@link #name}
     * @param source    {@link #source}
     * @param span      {@link #span}
     */
    public CallSite(final String name,
                    final String source,
                    final int[][] span) {
        this.name = name;
        this.source = source;
        this.span = span;
    }
    
    /**
     * { "name" : "foo()",
     *   "source" : "foo.at",
     *   "span" : [ [ lineNo ] ] }
     */
    public void toJSON(JSONWriter json) throws IOException {
    	JSONWriter.ObjectWriter callsite = json.startObject();
    	callsite.startMember("name").writeString(name);
    	callsite.startMember("source").writeString(source == null ? "unknown source" : source);
    	if (span != null) {
        	JSONWriter.ArrayWriter startAndEnd = callsite.startMember("span").startArray();
        	for (int i = 0; i < span.length; i++) {
            	JSONWriter.ArrayWriter lineAndCol = startAndEnd.startElement().startArray();
    			for (int j = 0; j < span[i].length; j++) {
    				lineAndCol.startElement().writeInt(span[i][j]);
    			}
    			lineAndCol.finish();
    		}
        	startAndEnd.finish();	
    	}
    	callsite.finish();
    }
}
