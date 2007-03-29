/**
 * AmbientTalk/2 Project
 * IATLogger.java created on 29-mrt-2007 at 19:54:27
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
package edu.vub.at.actors.net;

import java.io.OutputStreamWriter;

import org.apache.log4j.ConsoleAppender;

/**
 * Dedicated subclass of Log4J's ConsoleAppender because the
 * Log4JMini version of the framework for J2ME CDC does not correctly parse the
 * 'log4j.appender.AppenderName.target=System.err' property.
 * 
 * Hence, output always went to System.out (which is the default).
 * 
 * To circumvent this flaw of the Log4J mini-framework without reprogramming the framework
 * ourselves, we rather instantiate our own type of Appender.
 * An IATLogger is a ConsoleAppender that simply always logs to the error stream (System.err)
 * 
 * @author tvcutsem
 */
public class IATLogger extends ConsoleAppender {

	public IATLogger() {
		setWriter(new OutputStreamWriter(System.err));
		target = "System.err";
	}

}
