/**
 * AmbientTalk/2 Project
 * NATText.java created on 26-jul-2006 at 16:45:43
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
package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.natives.grammar.AGExpression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The native implementation of an AmbientTalk text string.
 * A text string is implemented by a Java String.
 * 
 * @author tvc
 */
public final class NATText extends AGExpression implements ATText {
		
		public final String javaValue;
		
		/**
		 * This method currently serves as a hook for text creation.
		 * Currently text objects are not reused, but this might change in the future.
		 */
		public static final NATText atValue(String javaString) {
			return new NATText(javaString);
		}
		
		private NATText(String javaString) {
			javaValue = javaString;
		}

		public boolean isNativeText() { return true; }
		public NATText asNativeText() throws XTypeMismatch { return this; }
		
		public NATText meta_print() throws InterpreterException {
	        return NATText.atValue("\"" + javaValue + "\"");
		}
		
		// comparison and identity operations
		
		public boolean equals(Object other) {
			return (other instanceof NATText) &&
			       ((NATText) other).javaValue.equals(this.javaValue);
		}
		
		public int hashCode() {
			return javaValue.hashCode();
		}
		
		// base-level interface
		
		/**
		 * Explodes a text string into a table of constinuent characters
		 */
		public ATTable base_explode() throws InterpreterException {
			ATObject[] chars = new ATObject[javaValue.length()];
			char[] rawchars = javaValue.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				chars[i] = NATText.atValue(Character.toString(rawchars[i]));
			}
			return NATTable.atValue(chars);
		}
		
		/**
		 * Split the string according to the given regular expression.
		 * For regular expression syntax, see the Java API.
		 */
		public ATTable base_split(ATText regexp) throws InterpreterException {
			String[] elements = null;
			 try {
				 elements = javaValue.split(regexp.asNativeText().javaValue);
			 } catch (PatternSyntaxException e) {
				throw new XIllegalArgument("Illegal argument to split: " + e.getMessage());
			 }
			
			ATObject[] tbl = new ATObject[elements.length];
			for (int i = 0; i < elements.length; i++) {
				tbl[i] = NATText.atValue(elements[i]);
			}
			return NATTable.atValue(tbl);
		}
		
		public ATNil base_find_do_(ATText regexp, ATClosure consumer) throws InterpreterException {
			 Pattern p = null;
			 
			 try {
				p = Pattern.compile(regexp.asNativeText().javaValue);
			 } catch (PatternSyntaxException e) {
				throw new XIllegalArgument("Illegal argument to find:do: " + e.getMessage());
			 }
			 
			 Matcher m = p.matcher(javaValue);
			 while (m.find()) {
				 consumer.base_apply(new NATTable(new ATObject[] { NATText.atValue(m.group()) }));
			 }
			 return NATNil._INSTANCE_;
		}
		
		public ATText base_replace_by_(ATText regexp, ATClosure transformer) throws InterpreterException {
			 Pattern p = null;
			 
			 try {
				p = Pattern.compile(regexp.asNativeText().javaValue);
			 } catch (PatternSyntaxException e) {
				throw new XIllegalArgument("Illegal argument to replace:by: " + e.getMessage());
			 }
			 
			 Matcher m = p.matcher(javaValue);
			 StringBuffer sb = new StringBuffer();
			 while (m.find()) {
				 ATObject replacement = transformer.base_apply(new NATTable(new ATObject[] { NATText.atValue(m.group()) }));
			     m.appendReplacement(sb, replacement.asNativeText().javaValue);
			 }
			 m.appendTail(sb);
			 return NATText.atValue(sb.toString());
		}
		
		public ATText base_toUpperCase() {
			return NATText.atValue(javaValue.toUpperCase());
		}
		
		public ATText base_toLowerCase() {
			return NATText.atValue(javaValue.toLowerCase());
		}
		
		public ATNumber base_length() {
			return NATNumber.atValue(javaValue.length());
		}
		
		public ATText base__oppls_(ATObject other) throws InterpreterException {
			return NATText.atValue(javaValue + other.asNativeText().javaValue);
		}
		
		public ATNumber base__opltx__opeql__opgtx_(ATText other) throws InterpreterException {
			int cmp = javaValue.compareTo(other.asNativeText().javaValue);
			if (cmp > 0)
			    return NATNumber.ONE;
			else if (cmp < 0)
				return NATNumber.MONE;
			else
				return NATNumber.ZERO;
		}
		
		public ATBoolean base__optil__opeql_(ATText other) throws InterpreterException {
			try {
				return NATBoolean.atValue(javaValue.matches(other.asNativeText().javaValue));
			} catch (PatternSyntaxException e) {
				throw new XIllegalArgument("Illegal regular expression for ~=: " + e.getMessage());
			}
		}
		
		public char asChar() throws XIllegalArgument {
			if (javaValue.length() == 1) {
				return javaValue.charAt(0);
			} else {
				throw new XIllegalArgument("Expected a character, given a string: " + javaValue);
			}
		}

}
