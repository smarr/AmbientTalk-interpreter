/**
 * AmbientTalk/2 Project
 * Logging.java created on 20-feb-2007 at 10:05:26
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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;

/**
 * Auxiliary class used to group Log4J loggers.
 * 
 * @author tvcutsem
 */
public final class Logging {

	/**
	 * Logs information regarding all event loops in the AT/2 runtime.
	 */
	public static final Logger EventLoop_LOG = Logger.getInstance("at.eventloops");
	
	/**
	 * Logs information of all actor event loops in the AT/2 runtime.
	 */
	public static final Logger Actor_LOG = Logger.getInstance("at.eventloops.actors");

	/**
	 * Logs information of all remote reference event loops in the AT/2 runtime.
	 */
	public static final Logger RemoteRef_LOG = Logger.getInstance("at.eventloops.remoterefs");
	
	/**
	 * Logs information of the VM event loops of the AT/2 runtime.
	 */
	public static final Logger VirtualMachine_LOG = Logger.getInstance("at.eventloops.vm");

	/**
	 * Logs information related to the object path, init file, etc.
	 */
	public static final Logger Init_LOG = Logger.getInstance("at.init");
	
	
	static {
		// intialize the Log4J API and the loggers
		
		// see http://logging.apache.org/log4j/docs/api/org/apache/log4j/PatternLayout.html
		// for a listing of possible pattern variables
		// d = date, t = thread, p = priority, c = category, m = message, n = newline
		ConsoleAppender console = new ConsoleAppender(new PatternLayout("%d %-5p %c - %m%n"), "System.err");
		EventLoop_LOG.addAppender(console);
		Actor_LOG.addAppender(console);
		RemoteRef_LOG.addAppender(console);
		VirtualMachine_LOG.addAppender(console);
		Init_LOG.addAppender(console);
		
		Logger jGroupsLogger = Logger.getInstance("org.jgroups");
		jGroupsLogger.addAppender(console);
		
		Properties props = new Properties();
		try {
			props.load(Logging.class.getResourceAsStream("logging.props"));
			Enumeration allProperties = props.propertyNames();
			// interpret each KEY=VALUE pair in the log file as
			// Logger(KEY).setPriority(VALUE)
			// where KEY = the name of a logger and VALUE = a priority level (e.g. WARN, DEBUG, ...)
			while (allProperties.hasMoreElements()) {
				String loggerName = (String) allProperties.nextElement();
				Logger.getInstance(loggerName).setPriority(Priority.toPriority(props.getProperty(loggerName)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//PropertyConfigurator.configure(Logging.class.getResource("logging.props"));
	}
	
}
