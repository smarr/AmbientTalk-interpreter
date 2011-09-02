/**
 * AmbientTalk/2 Project
 * Regexp.java created on 09-apr-2008 at 09:50:03
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
package edu.vub.util;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.util.Regexp.StringCallable;

import java.util.regex.*;

/**
 * Class with convenience methods for regular expressions
 * 
 * @author tvcutsem
 */
public class Regexp {
	    
    // serve as "closures" passed to the findAll and replaceAll methods
    
	public interface StringCallable {
		public String call(String input) throws InterpreterException;
	}
	public interface StringRunnable {
		public void run(String input) throws InterpreterException;
	}
	
	public static void findAll(String regex, String input, StringRunnable consumer) throws InterpreterException {
		Matcher matcher = Pattern.compile(regex).matcher(input);
		while (matcher.find()) {
			consumer.run(matcher.group());
		}
	}
	
	public static String replaceAll(String regex, String input, StringCallable replacer) throws InterpreterException {
		Matcher matcher = Pattern.compile(regex).matcher(input);
		StringBuffer sb = new StringBuffer();
		 while (matcher.find()) {
			 String replacement = replacer.call(matcher.group());
		     matcher.appendReplacement(sb, replacement);
		 }
		 matcher.appendTail(sb);
		 return sb.toString();
	}

	public static String replaceAll(Pattern pattern, String input, StringCallable replacer) throws InterpreterException {
		Matcher matcher = pattern.matcher(input);
		return replaceAll(matcher, input, replacer);
		
	}

	public static String replaceAll(Matcher matcher, String input, StringCallable replacer) throws InterpreterException {
		StringBuffer sb = new StringBuffer();
		 while (matcher.find()) {
			 String replacement = replacer.call(matcher.group());
		     matcher.appendReplacement(sb, replacement);
		 }
		 matcher.appendTail(sb);
		 return sb.toString();
	}
	
}
