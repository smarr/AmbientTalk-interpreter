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
	
	public void testMirageCloning() {
		try{
			evaluateInput(
					"def cloningMirror := \n" +
					"	mirror: { \n" +
					"		def IAmAClone := false; \n" +
					"		def clone() { IAmAClone := true;  super.clone() }; \n" +
					"		def doesNotUnderstand(@args) { \n" +
					"			if: IAmAClone then: { \n" +
					"				success(\"Field added after cloning was not visible\"); \n" +
					"			} else: { \n" +
					"				fail(\"Field was not properly added on the original\"); \n" +
					"			} \n" +
					"		} \n" +
					"	}; \n" +
					"\n" +
					"def original := \n" +
					"	cloningMirror.base; \n" +
					//"def original.sharedField := \"The field is shared.\"; \n" +
					"cloningMirror.defineField(`(sharedField) , \"The field is shared.\"); \n" +
					"def clone := clone: original; \n" +
					// "def clone := cloningMirror.clone().base; \n" +
					"clone.sharedField := \"But its value is not.\"; \n" +
					//"def original.nonSharedField := \"That's what happens when you clone too early.\"; \n" +
					"cloningMirror.defineField(`(nonSharedField) , \"That's what happens when you clone too early.\"); \n" +
					"echo: original.sharedField; \n" +
					"echo: clone.sharedField; \n" +
					"echo: original.nonSharedField; \n" +
					"echo: clone.nonSharedField;"
					, new NATContext(new NATCallframe(lexicalRoot), lexicalRoot, NATNil._INSTANCE_)
					);
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception : " + e);
		}
	};
	
	public void testMirageInvocation() {
		try {
			evaluateInput(
					"def loggingMirror := {" +
					"	def defaultSpacing := 2;" +
					"	def indentLevel := 0;" +
					"	def spaces() {" +
					"		def result := \"\";" +
					"		indentLevel.doTimes: { | i |" +
					"			defaultSpacing.doTimes: { | j |" +
					"				result := result + \" \";" +
					"			}" +
					"		};" +
					"		result;" +
					"	};" +
					"" +
					"	mirror: {" +
					"		def invoke(receiver, selector, args) {" +
					"			echo: (spaces() + \"Invocation of method \" + selector + \" with arguments \" + args + \" on \" + receiver);" +
					"			indentLevel := indentLevel + 1;" +
					"			def result := super.invoke(receiver, selector, args);" +
					"			indentLevel := indentLevel - 1;" +
					"			echo: (spaces() + \"Invocation of method \" + selector + \" yielded \" + result );" +
					"			result;" +
					"		}" +
					"	}" +
					"}();" +
					"" +
					"def mirroredParent := object: {" +
					"	def m() { self.n() };" +
					"	def n() { echo: \"ok\" };" +
					"} mirroredBy: loggingMirror;" +
					"" +
					"def unmirroredChild := " +
					"	extend: mirroredParent" +
					"	with: {" +
					"		def m() { " +
					"			echo: \"My parent will start logging now\";" +
					"			super.m();" +
					"		};" +
					"	};" +
					"" +
					"def mirroredChild := " +
					"	extend: mirroredParent" +
					"	with: {" +
					"		def n() { " +
					"			echo: \"Indentation of this call should be correct as the lexical scope is shared by both mirrors\";" +
					"			super.n();" +
					"		};" +
					"	} mirroredBy: loggingMirror;" +
					"" +
					"mirroredParent.m();"  +
					"unmirroredChild.m();" +
					"mirroredChild.m();"
					, new NATContext(new NATCallframe(lexicalRoot), lexicalRoot, NATNil._INSTANCE_)
					);
		} catch (NATException e) {
			e.printStackTrace();
			fail("exception : " + e);
		}		
	}
	
}
