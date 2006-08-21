/**
 * AmbientTalk/2 Project
 * ReflectionTest.java created on Jul 31, 2006 at 11:12:57 PM
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import junit.framework.TestCase;

/**
 * @author tvc
 * This unit test tests all of the methods of the Reflection class.
 */
public class ReflectionTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ReflectionTest.class);
	}
	
	private void compareSymbol(String str, ATSymbol sym) {
		try {
			assertEquals(str, sym.getText().asNativeText().javaValue);
		} catch (XTypeMismatch e) {
			fail(e.getMessage());
		}
	}
	
	public void testDownSelector() {
		compareSymbol("foo:", Reflection.downSelector("foo_"));
		compareSymbol("foo:bar:", Reflection.downSelector("foo_bar_"));
		compareSymbol("+", Reflection.downSelector("_oppls_"));
		compareSymbol("set!", Reflection.downSelector("set_opnot_"));
		compareSymbol("foo:<bar:", Reflection.downSelector("foo__opltx_bar_"));
		compareSymbol(":opbla:", Reflection.downSelector("_opbla_"));
	}
	
	public void testUpSelector() throws NATException {
		assertEquals("foo_", Reflection.upSelector(AGSymbol.alloc("foo:")));
		assertEquals("foo_bar_", Reflection.upSelector(AGSymbol.alloc("foo:bar:")));
		assertEquals("_oppls_", Reflection.upSelector(AGSymbol.alloc("+")));
		assertEquals("set_opnot_", Reflection.upSelector(AGSymbol.alloc("set!")));
		assertEquals("foo__opltx_bar_", Reflection.upSelector(AGSymbol.alloc("foo:<bar:")));
		assertEquals("_opbla_", Reflection.upSelector(AGSymbol.alloc(":opbla:")));
		assertEquals("yes?", Reflection.upSelector(AGSymbol.alloc("yes?")));
	}
	
	
}
