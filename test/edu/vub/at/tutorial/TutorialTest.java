/**
 * AmbientTalk/2 Project
 * TutorialTest.java created on 26-feb-2007 at 10:47:24
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
package edu.vub.at.tutorial;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;

/**
 * TODO document the class TutorialTest
 *
 * @author tvcutsem
 */
public class TutorialTest extends AmbientTalkTest {

	
	public void testBasicProgramming() throws InterpreterException {
		
		//variables
		evalAndCompareTo("def x := 5", "5");
		evalAndCompareTo("def y := x + 2", "7");
		evalAndCompareTo("[x, y] := [y, x]", "[7, 5]");
		
		//tables
		evalAndCompareTo("def z := 0", "0");
		evalAndCompareTo("def t[5] { z := z + 1 }", "[1, 2, 3, 4, 5]");
		
		//functions
		evalAndReturn("def square (x) { x*x }");
		evalAndCompareTo("square(5)", "25");

		evalAndReturn("	def fac(n) {" +
			"def inner(n, result) { " +
			   "if: (n =0) then: { result } else: { inner( n-1, n * result)  }" +
			  "};" + 
			  "inner(n,1)" +
			"}");
		evalAndCompareTo("fac(5)", "120");

		evalAndCompareTo("def sum := 0", "0");
		evalAndCompareTo("sum := sum + 1", "1");
		evalAndReturn("sum := { | x, y| x + y }");
		evalAndCompareTo("sum(1,2)", "3");
	

	}
	
	
}
