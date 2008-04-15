package edu.vub.util;

import edu.vub.at.exceptions.InterpreterException;

import org.apache.regexp.RE;
import org.apache.regexp.RECompiler;
import org.apache.regexp.REProgram;

/**
 * A thin wrapper around the Apache Regexp library. Primarily provides support for
 * finding or replacing all occurences of a regular expression in a string.
 * 
 * This is the only class that should explicitly manage the compilation of regexp patterns.
 * Instances of the class {@link RE} should never be shared between threads. However,
 * {@link REProgram} instances can be shared among threads. Compilation of an REProgram
 * into an RE however, must be synchronised between threads.
 * 
 * @author tvcutsem
 */
public class Regexp {
	
    private static final RECompiler _COMPILER_ = new RECompiler();
    
    // serve as "closures" passed to the findAll and replaceAll methods
    
	public interface StringCallable {
		public String call(String input) throws InterpreterException;
	}
	public interface StringRunnable {
		public void run(String input) throws InterpreterException;
	}
	
	public static REProgram compile(String regex) {
		// synchronize access to the compiler because it is not multiple-thread safe
		synchronized (_COMPILER_) {
			return _COMPILER_.compile(regex);
		}
	}
	
	public static void findAll(RE regex, String input, StringRunnable consumer) throws InterpreterException {
		while (regex.match(input)) {
			consumer.run(regex.getParen(0));
			input = input.substring(regex.getParenEnd(0), input.length());
		}
	}
	
	public static String replaceAll(RE regex, String input, StringCallable replacer) throws InterpreterException {
		StringBuffer out = new StringBuffer(input);
		int ofs = 0;
		while (regex.match(input)) {
			int  start = regex.getParenStart(0);
			String matched = regex.getParen(0);
			int end = regex.getParenEnd(0);
			String substitute = replacer.call(matched);
			// advance input string to the end of the current match
			// e.g. if input = "xxabyab" is matched against "(a*)b", then
			// input is set to "yab" after the first match
			input = input.substring(end, input.length());
			// in the output string, replace the matching part (start - end) by the substitute
			// i.e. if out = "xxabyab" and every matching string is replaced by * then out
			// is changed to "xx*yab"
			out.replace(ofs + start, ofs + end, substitute);
			// move offset in which to write to out to the end of the newly substituted part
			// in the example above, ofs is set to 0 + 2 + 1 = 3 such that the next replace
			// will occur relative to the string "yab"
			ofs = ofs + start + substitute.length();
		}
		return out.toString();
	}
	
}
