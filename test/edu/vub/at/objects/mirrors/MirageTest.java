/**
 * AmbientTalk/2 Project
 * MirageTest.java created on Aug 11, 2006 at 10:54:29 PM
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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;

/**
 * @author smostinc
 *
 * TODO document the class MirageTest
 */
public class MirageTest extends ReflectiveAccessTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirageTest.class);
	}
	
	public void testMirage() {
		try {
			evaluateInput(
					"def baseField := 0; \n" +
					"def fakeMirror := mirror: { \n" +
					"  def metaField := 0; \n" +
					"  def select(receiver, selector) { \n" +
					"    if: (selector == `(baseField)) then: { baseField } \n" +
					"  }; \n" +
					"  def assignField(selector, value) { \n" +
					"    if: (selector == `(baseField)) then:  \n" +
					"      { baseField := value } \n" +
					"  }; \n" +
					"  def invoke(receiver, selector, arguments) { \n" +
					"    unit.echo: selector; \n" +
					"    super.invoke(receiver, selector, arguments) \n" +
					"  }; \n" +
					"  def apply(arguments) { \n" +
					"    unit.echo: \"apply\";" +
					"    baseField := baseField + 1; \n" +
					"    unit.success() \n" +
					"  } \n" +
					"}; \n" +
					"def test := fakeMirror.newInstance([]).base; \n" +
					//"def recursive := at.mirrors.Factory.createMirror(test); \n" +
					" \n" +
					"fakeMirror.apply([]); \n" +
					"fakeMirror.select(test, `(baseField)); \n" +
					"fakeMirror.assignField(`(baseField), 0); \n" +
					" \n" +
					"test(); \n" +
					"test.baseField; \n" +
					"test.baseField := 0; \n" +
					" \n" //+
					/* "recursive.apply([]); \n" + */
					//"recursive.select(test, `(baseField)); \n" +
					/*"recursive.assignField(`(baseField), 0); \n"*/,
					new NATContext(new NATCallframe(lexicalRoot), lexicalRoot, NATNil._INSTANCE_)
					);
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception : " + e);
		}
	}
	
}
