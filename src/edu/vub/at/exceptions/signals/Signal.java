/**
 * AmbientTalk/2 Project
 * Signal.java created on 31-okt-2006 at 12:29:13
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
package edu.vub.at.exceptions.signals;

/**
 * @author tvcutsem
 *
 * Signal is the abstract superclass of all AT/2 interpreter Signals.
 * 
 * Signals are exceptions which *cannot* be caught at the AmbientTalk base-level.
 * They cannot even be caught at the meta-level, but are accessible to the implementation only.
 * 
 * Signals pervade the AT/2 interpreter, which is why they are declared to be unchecked exceptions,
 * i.e. they subclass RuntimeException. This signifies that these exceptions should always be
 * caught by the lowest-level evaluation loops in the runtime, such that they cannot cause the
 * interpreter to crash unexpectedly.
 */
public abstract class Signal extends RuntimeException {

}
