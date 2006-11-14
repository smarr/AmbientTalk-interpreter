/**
 * AmbientTalk/2 Project
 * OBJUnit.java created on Aug 22, 2006 at 11:32:30 AM
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

package edu.vub.at;

import edu.vub.at.objects.mirrors.CoercionTest;
import edu.vub.at.objects.mirrors.InvocationTest;
import edu.vub.at.objects.mirrors.MirageTest;
import edu.vub.at.objects.mirrors.MirrorTest;
import edu.vub.at.objects.mirrors.MirrorsOnNativesTest;
import edu.vub.at.objects.mirrors.ReflectionTest;
import edu.vub.at.objects.natives.EscapeTest;
import edu.vub.at.objects.natives.ExceptionHandlingTest;
import edu.vub.at.objects.natives.LexicalRootTest;
import edu.vub.at.objects.natives.NATNamespaceTest;
import edu.vub.at.objects.natives.NATObjectClosureTest;
import edu.vub.at.objects.natives.NATObjectTest;
import edu.vub.at.objects.natives.PrimitivesTest;
import edu.vub.at.objects.natives.TestFieldMap;
import edu.vub.at.objects.natives.grammar.TestEval;
import edu.vub.at.objects.symbiosis.SymbiosisTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author tvcutsem
 * 
 * Runs all relevant test suites related to the interpreter (evaluation, reflection, natives)
 */
public class InterpreterTests {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(InterpreterTests.class);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("All AT2 Interpreter-related tests.");
		//$JUnit-BEGIN$
		suite.addTestSuite(InvocationTest.class);
		suite.addTestSuite(MirrorTest.class);
		suite.addTestSuite(MirageTest.class);
		suite.addTestSuite(ReflectionTest.class);
		suite.addTestSuite(NATObjectClosureTest.class);
		suite.addTestSuite(NATObjectTest.class);
		suite.addTestSuite(TestFieldMap.class);
		suite.addTestSuite(TestEval.class);
		suite.addTestSuite(PrimitivesTest.class);
		suite.addTestSuite(LexicalRootTest.class);
		suite.addTestSuite(NATNamespaceTest.class);
		suite.addTestSuite(CoercionTest.class);
		suite.addTestSuite(ExceptionHandlingTest.class);
		suite.addTestSuite(EscapeTest.class);
		suite.addTestSuite(MirrorsOnNativesTest.class);
		suite.addTestSuite(SymbiosisTest.class);
		//$JUnit-END$
		return suite;
	}

}
