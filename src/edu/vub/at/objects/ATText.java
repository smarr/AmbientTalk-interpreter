/**
 * AmbientTalk/2 Project
 * ATText.java created on 26-jul-2006 at 15:18:43
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
package edu.vub.at.objects;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.grammar.ATExpression;

/**
 * ATText is the public interface to a native AmbientTalk string (a string of characters).
 * Extends the ATExpression interface since a Text can also be output by the parser as a literal.
 * 
 * @author tvc
 */
public interface ATText extends ATExpression {

	// base-level interface
	/**
	 * Explodes a text into a table of constituent characters.
	 * <p>
	 * Usage example:
	 * <code>"ambienttalk".explode()</code> returns <code> [a, m, b, i, e, n, t, t, a, l, k]</code>
	 * 
	 * @return an {@link ATTable} resulting of exploding the receiver text into a table of constituent characters.
	 */
	public ATTable base_explode() throws InterpreterException;
	
	/**
	 * Splits a text according to the given regular expression.
	 * <p>
	 * For regular expression syntax, see the Java regular-expression constructs in java.util.regex.Pattern.
	 * For regular expression syntax, see the Apache Regexp API of class
	 * <a href="http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html">RE</a>.
	 * <p>
	 * Usage example:
	 * <code>"one, two, three".split(", ")</code> returns <code>[ "one", "two", "three" ]</code>
	 * 
	 * @param regexpr a text representing the regular expression to apply in the split.
	 * @return an {@link ATTable} resulting of splitting the receiver text into a table according to the given regular expression.
	 * @throws XIllegalArgument if regular expression's syntax is invalid. 
	 */
	public ATTable base_split(ATText regexpr) throws InterpreterException;
	
	/**
	 * Evaluates a given closure on those elements of this text that match a given regular expression.
	 * <p>
	 * For regular expression syntax, see the Apache Regexp API of class
	 * <a href="http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html">RE</a>.
	 * <p>
	 * Usage example:
	 * <code>"ambienttalk".find: "[aeiou]" do: { |vowel| buff << vowel; nil }</code> returns <code>buff = "aiea"</code>
	 * 
	 * @param regexp a text representing the regular expression to be found in the text.
	 * @param consumer the closure code that is applied each time the regular expression is matched in the text.
	 * @return nil
	 * @throws XIllegalArgument if regular expression's syntax is invalid. 
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATNil base_find_do_(ATText regexp, ATClosure consumer) throws InterpreterException;
	
	/**
	 * Returns a new text replacing those elements of this text that match a given regular expression with the
	 * value resulting of the evaluation of a given closure.
	 * <p>
	 * For regular expression syntax, see the Apache Regexp API of class
	 * <a href="http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html">RE</a>.
	 * <p>
	 * Usage example:
	 * <code>"ambienttalk".replace: "[aeiou]" by: { |vowel| vowel.toUpperCase() }</code> returns <code>AmbIEnttAlk</code>
	 * 
	 * @param regexp a text representing the regular expression to be found in the text.
	 * @param transformer the closure code that is applied each time the regular expression matches.
	 * @return {@link ATText} replacing those elements of the table that match the regexpr pattern with the value resulting of the evaluation of the transformer closure.
	 * @throws XIllegalArgument if regular expression's syntax is invalid. 
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATText base_replace_by_(ATText regexp, ATClosure transformer) throws InterpreterException;
	
	/**
	 * Converts all of the characters in this text to upper case.
	 * 
	 * @return the {@link ATText} resulting of the conversion.
	 */
	public ATText base_toUpperCase() throws InterpreterException;
	
	/**
	 * Converts all of the characters in this text to lower case.
	 * 
	 * @return the {@link ATText} resulting of the conversion.
	 */
	public ATText base_toLowerCase() throws InterpreterException;
	
	/**
	 * Returns the length of this text.
	 * 
	 * @return the {@link ATNumber} representing the length of the sequence of characters of this text.
	 */
	public ATNumber base_length() throws InterpreterException;
	
	/**
	 * Concatenation infix operator. Returns the concatenation of the this text and the text representing a given object.
	 * <p>
	 * Usage example:
	 * <code>"ambient" + "talk"</code> returns <code>"ambienttalk"</code>
	 * 
	 * @param other an object whose text representation is concatenated to the receiver text.
	 * @return an ATText containing the elements of the receiver text and then the elements of text representing the other object.
	 */
	public ATText base__oppls_(ATObject other) throws InterpreterException;
	
	/**
	 * Returns the value of evaluating the generalized equality between this text and a given one.
	 * <p>
	 * The generalized equality returns:
	 * <ul>
	 * <li>-1 if the receiver is numerically greater than the text passed as argument.
	 * <li>1 if the receiver is numerically smaller than the text passed as argument.
	 * <li>0 if the receiver is numerically equal to the the text passed as argument.
	 * </ul>
	 * <p>
	 * Usage example:
	 * <code>"ambienttalk" <=> "ambienttalk"</code> returns <code>0</code>
	 * 
	 * @param other a text.
	 * @return a {@link ATNumber} resulting of applying the generalized equality between the receiver and other. 
	 */
	public ATNumber base__opltx__opeql__opgtx_(ATText other) throws InterpreterException;
	
	/**
	 * Attempts to match this text against a given regular expression.
	 * <p>
	 * For regular expression syntax, see the Apache Regexp API of class
	 * <a href="http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html">RE</a>.
	 * <p>
	 * Usage example:
	 * <code>"ambienttalk" ~= ".*tt.*"</code> returns <code>true</code>
	 * 
	 * @param other a text representing the regular expression to be found in the text.
	 * @return true if and only if, the receiver text matches completely the other text pattern.
	 * @throws XIllegalArgument if regular expression's syntax is invalid. 
	 */
	public ATBoolean base__optil__opeql_(ATText other) throws InterpreterException;
	
	/**
	 * Tries to convert the text into a numeric object (a number or a fraction).
	 * Example: <code>"1.0".parseNumeric() => 1.0</code>
	 * @return the numeric object denoted by this text
	 * @throws XIllegalArgument if the text cannot be converted into a number or a fraction
	 */
	public ATNumeric base_parseNumeric() throws InterpreterException;
}
