/**
 * AmbientTalk/2 Project
 * Logger.java created on 5-apr-2007 at 10:50:03
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
package edu.vub.at.util.logging;

import java.util.Date;
import java.util.HashMap;

/**
 * A logger object modelled after the interface of the Log4J framework.
 * 
 * @author tvcutsem
 */
public class Logger {

	/**
	 *  A map for pooling all of the loggers.
	 */
	private static final HashMap _LOGS = new HashMap();
	
	private final String name_;
	
	/**
	 * Logs with a priority less than this will not get logged.
	 */
	private int leastPriority_;
	
	private String textPriority_;
	
	/**
	 * Access to the map should actually be synchronized, but this
	 * adds overhead to accessing the loggers.
	 * 
	 * Without synchronization, the possibility exists that same logger
	 * is added twice. On the other hand, as loggers are stateless, this is not
	 * really a problem (the new one will replace the old one without any harm)
	 */
	public static Logger getInstance(String name) {
		Logger logger = (Logger) _LOGS.get(name);
		if (logger == null) {
			logger = new Logger(name);
			_LOGS.put(name, logger);
		}
		return logger;
	}
	
	/** When loggers are initialised with this setting, all messages (including debug info) are being logged. */ 
	public static final int _DEBUG_LEVEL_ = 1;
	/** When loggers are initialised with this setting, all messages except for debug info are being logged. */ 
	public static final int _WARN_LEVEL_  = 2;
	/** When loggers are initialised with this setting, errors as well as info (e.g. regarding connection status) is being logged. */ 
	public static final int _INFO_LEVEL_  = 3;
	/** When loggers are initialised with this setting, all errors (including tolerable ones) are reported. */ 
	public static final int _ERROR_LEVEL_ = 4;
	/** When loggers are initialised with this setting, only fatal errors are reported. */ 
	public static final int _FATAL_LEVEL_ = 5;
	
	private Logger(String nam) {
		name_ = nam;
		leastPriority_ = _DEBUG_LEVEL_;
		textPriority_ = "DEBUG";
	}
	
	/**
	 * Set the priority of the logger.
	 * @param priority - one of 'DEBUG', 'WARN', 'INFO', 'ERROR' or 'FATAL'
	 * @throws IllegalArgumentException if the given argument is not one of the above logging levels.
	 */
	public void setPriority(String priority) throws IllegalArgumentException {
		leastPriority_ = textToLevel(priority);
		textPriority_ = priority;
	}
	
	/**
	 * Reports debugging information to be logged. Such messages are ignored when the logger has
	 * its priority set to be higher than or equal to WARNING. 
	 * @param msg the message to be logged.
	 */
	public void debug(String msg) {
		log(_DEBUG_LEVEL_, msg, null);
	}
	
	/**
	 * Reports debugging information to be logged. Such messages are ignored when the logger has
	 * its priority set to be higher than or equal to WARNING. 
	 * @param msg the message to be logged.
	 * @param exc the underlying exception that triggered the log request.
	 */
	public void debug(String msg, Throwable exc) {
		log(_DEBUG_LEVEL_, msg, exc);
	}
	
	/**
	 * Reports a warning (i.e a suspected inconsistency in the interpreter) to be logged. Such 
	 * messages are ignored when the logger has its priority set to be higher than or equal to 
	 * INFO. 
	 * @param msg the message to be logged.
	 */
	public void warn(String msg) {
		log(_WARN_LEVEL_, msg, null);
	}
	
	/**
	 * Reports a warning (i.e a suspected inconsistency in the interpreter) to be logged. Such 
	 * messages are ignored when the logger has its priority set to be higher than or equal to 
	 * INFO. 
	 * @param msg the message to be logged.
	 * @param exc the underlying exception that triggered the log request.
	 */
	public void warn(String msg, Throwable exc) {
		log(_WARN_LEVEL_, msg, exc);
	}
	
	/**
	 * Reports useful information on the interpreter's functioning (e.g. changes in connection 
	 * state) to be logged. Such messages are ignored when the logger has its priority set to be 
	 * higher than or equal to ERROR. 
	 * @param msg the message to be logged.
	 */
	public void info(String msg) {
		log(_INFO_LEVEL_, msg, null);
	}
	
	/**
	 * Reports useful information on the interpreter's functioning (e.g. changes in connection 
	 * state) to be logged. Such messages are ignored when the logger has its priority set to be 
	 * higher than or equal to ERROR. 
	 * @param msg the message to be logged.
	 * @param exc the underlying exception that triggered the log request.
	 */
	public void info(String msg, Throwable exc) {
		log(_INFO_LEVEL_, msg, exc);
	}
	
	/**
	 * Reports a tolerable error which occurred in the interpreter. Such messages are logged unless
	 * the logger set to FATAL. 
	 * @param msg the message to be logged.
	 */
	public void error(String msg) {
		log(_ERROR_LEVEL_, msg, null);
	}
	
	/**
	 * Reports a tolerable error which occurred in the interpreter. Such messages are logged unless
	 * the logger set to FATAL. 
	 * @param msg the message to be logged.
	 * @param exc the underlying exception that triggered the log request.
	 */
	public void error(String msg, Throwable exc) {
		log(_ERROR_LEVEL_, msg, exc);
	}
	
	/**
	 * Reports a fatal error in the interpreter. Such messages are always logged.
	 * @param msg the message to be logged.
	 */
	public void fatal(String msg) {
		log(_FATAL_LEVEL_, msg, null);
	}
	
	/**
	 * Reports a fatal error in the interpreter. Such messages are always logged.
	 * @param msg the message to be logged.
	 * @param exc the underlying exception that triggered the log request.
	 */
	public void fatal(String msg, Throwable exc) {
		log(_FATAL_LEVEL_, msg, exc);
	}
	
	private void log(int priority, String msg, Throwable exc) {
		if (priority >= leastPriority_) {
			// format: date priority logname - message
			System.err.println(new Date().toString() + " " + textPriority_ + " "+ name_ + " - " + msg);
			if (exc != null) {
				exc.printStackTrace(System.err);
			}
		}
	}
	
	private int textToLevel(String priority) throws IllegalArgumentException {
		if (priority.equalsIgnoreCase("DEBUG")) {
			return _DEBUG_LEVEL_;
		} else if (priority.equalsIgnoreCase("WARN")) {
			return _WARN_LEVEL_;
		} else if (priority.equalsIgnoreCase("INFO")) {
			return _INFO_LEVEL_;
		} else if (priority.equalsIgnoreCase("ERROR")) {
			return _ERROR_LEVEL_;
		} else if (priority.equalsIgnoreCase("FATAL")) {
			return _FATAL_LEVEL_;
		} else {
			throw new IllegalArgumentException("Illegal priority value: " + priority);
		}
	}
}
