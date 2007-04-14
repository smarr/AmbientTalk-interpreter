/* Matcher.java -- Instance of a regular expression applied to a char sequence.
 Copyright (C) 2002, 2004 Free Software Foundation, Inc.

 This file is part of GNU Classpath.

 GNU Classpath is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2, or (at your option)
 any later version.

 GNU Classpath is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with GNU Classpath; see the file COPYING.  If not, write to the
 Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 02111-1307 USA.

 Linking this library statically or dynamically with other modules is
 making a combined work based on this library.  Thus, the terms and
 conditions of the GNU General Public License cover the whole
 combination.

 As a special exception, the copyright holders of this library give you
 permission to link this library with independent modules to produce an
 executable, regardless of the license terms of these independent
 modules, and to copy and distribute the resulting executable under
 terms of your choice, provided that you also meet, for each linked
 independent module, the terms and conditions of the license of that
 module.  An independent module is a module which is not derived from
 or based on this library.  If you modify this library, you may extend
 this exception to your version of the library, but you are not
 obligated to do so.  If you do not wish to do so, delete this
 exception statement from your version. */

package edu.vub.util;

import edu.vub.util.regexp.REMatch;

/**
 * Instance of a regular expression applied to a char sequence.
 *
 * @since 1.4
 */
public class Matcher {
	private Pattern pattern;

	private StringBuffer input;

	private int position;

	private int appendPosition;

	private REMatch match;

	Matcher(Pattern pattern, StringBuffer input) {
		this.pattern = pattern;
		this.input = input;
	}

	/**
	 * @param sb The target string buffer
	 * @param replacement The replacement string
	 *
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 * @exception IndexOutOfBoundsException If the replacement string refers
	 * to a capturing group that does not exist in the pattern
	 */
	public Matcher appendReplacement(StringBuffer sb, String replacement)
			throws IllegalStateException {
		assertMatchOp();
		sb.append(input.substring(appendPosition, match.getStartIndex())
				.toString());
		sb.append(match.substituteInto(replacement));
		appendPosition = match.getEndIndex();
		return this;
	}

	/**
	 * @param sb The target string buffer
	 */
	public StringBuffer appendTail(StringBuffer sb) {
		sb.append(input.substring(appendPosition, input.length()).toString());
		return sb;
	}

	/**
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 */
	public int end() throws IllegalStateException {
		assertMatchOp();
		return match.getEndIndex();
	}

	/**
	 * @param group The index of a capturing group in this matcher's pattern
	 *
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 * @exception IndexOutOfBoundsException If the replacement string refers
	 * to a capturing group that does not exist in the pattern
	 */
	public int end(int group) throws IllegalStateException {
		assertMatchOp();
		return match.getEndIndex(group);
	}

	/**
	 * Attempts to find the next subsequence of the input sequence that matches the pattern. 
	 * <p>
	 * This method starts at the beginning of the input sequence or, if a previous invocation of 
	 * the method was successful and the matcher has not since been reset, at the first character 
	 * not matched by the previous match.
	 * <p>
	 * If the match succeeds then more information can be obtained via the start, end, and group 
	 * methods.
	 * @return true if, and only if, a subsequence of the input sequence matches this matcher's pattern
	 */
	public boolean find() {
		boolean first = (match == null);
		match = pattern.getRE().getMatch(input, position);
		if (match != null) {
			int endIndex = match.getEndIndex();
			// Are we stuck at the same position?
			if (!first && endIndex == position) {
				match = null;
				// Not at the end of the input yet?
				if (position < input.length() - 1) {
					position++;
					return find(position);
				} else
					return false;
			}
			position = endIndex;
			return true;
		}
		return false;
	}

	/**
	 * @param start The index to start the new pattern matching
	 *
	 * @exception IndexOutOfBoundsException If the replacement string refers
	 * to a capturing group that does not exist in the pattern
	 */
	public boolean find(int start) {
		match = pattern.getRE().getMatch(input, start);
		if (match != null) {
			position = match.getEndIndex();
			return true;
		}
		return false;
	}

	/**
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 */
	public String group() {
		assertMatchOp();
		return match.toString();
	}

	/**
	 * @param group The index of a capturing group in this matcher's pattern
	 *
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 * @exception IndexOutOfBoundsException If the replacement string refers
	 * to a capturing group that does not exist in the pattern
	 */
	public String group(int group) throws IllegalStateException {
		assertMatchOp();
		return match.toString(group);
	}

	/**
	 * @param replacement The replacement string
	 */
	public String replaceFirst(String replacement) {
		reset();
		// Semantics might not quite match
		return pattern.getRE().substitute(input, replacement, position);
	}

	/**
	 * @param replacement The replacement string
	 */
	public String replaceAll(String replacement) {
		reset();
		return pattern.getRE().substituteAll(input, replacement, position);
	}

	/**
	 * Returns the number of capturing groups in this matcher's pattern.
	 * <p>
	 * Group zero denotes the entire pattern by convention. It is not included in this count.
	 * <p>
	 * Any non-negative integer smaller than or equal to the value returned by this method is 
	 * guaranteed to be a valid group index for this matcher.
	 *
	 * @return The number of capturing groups in this matcher's pattern
	 */
	public int groupCount() {
		return pattern.getRE().getNumSubs();
	}

	/**
	 * Attempts to match the input sequence, starting at the beginning, against the pattern.
	 * <p>
	 * Like the <code>matches</code> method, this method always starts at the beginning of the 
	 * input sequence; unlike that method, it does not require that the entire input sequence be 
	 * matched.
	 * <p>
	 * If the match succeeds then more information can be obtained via the <code>start</code>, 
	 * <code>end</code>, and <code>group</code> methods.
	 * 
	 * @return true if, and only if, a prefix of the input sequence matches this matcher's pattern
	 */
	public boolean lookingAt() {
		match = pattern.getRE().getMatch(input, 0);
		if (match != null) {
			if (match.getStartIndex() == 0)
				return true;
			match = null;
		}
		return false;
	}

	/**
	 * Attempts to match the entire input sequence against the pattern.
	 *
	 * If the match succeeds then more information can be obtained via the
	 * start, end, and group methods.
	 *
	 * @see #start
	 * @see #end
	 * @see #group
	 */
	public boolean matches() {
		return find(0);
	}

	/**
	 * Returns the Pattern that is interpreted by this Matcher
	 */
	public Pattern pattern() {
		return pattern;
	}

	/**
	 * Resets this matcher.
	 * <p>
	 * Resetting a matcher discards all of its explicit state information and sets its append 
	 * position to zero.
	 * 
	 * @return This matcher
	 */
	public Matcher reset() {
		position = 0;
		match = null;
		return this;
	}

	/**
	 * @param input The new input character sequence
	 */
	public Matcher reset(StringBuffer input) {
		this.input = input;
		return reset();
	}

	/**
	 * @param group The index of a capturing group in this matcher's pattern
	 *
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 */
	public int start() throws IllegalStateException {
		assertMatchOp();
		return match.getStartIndex();
	}

	/**
	 * @param group The index of a capturing group in this matcher's pattern
	 *
	 * @exception IllegalStateException If no match has yet been attempted,
	 * or if the previous match operation failed
	 * @exception IndexOutOfBoundsException If the replacement string refers
	 * to a capturing group that does not exist in the pattern
	 */
	public int start(int group) throws IllegalStateException {
		assertMatchOp();
		return match.getStartIndex(group);
	}

	private void assertMatchOp() {
		if (match == null)
			throw new IllegalStateException();
	}
}
