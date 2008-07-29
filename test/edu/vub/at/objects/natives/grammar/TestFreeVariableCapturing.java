/**
 * AmbientTalk/2 Project
 * TestFreeVariableCapturing.java created on 29 jul 2008 at 11:23:00
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.parser.NATParser;

import java.util.Set;

/**
 * Tests the algorithm that automatically deduces lexically free variables in a
 * piece of source code.
 *
 * @author tvcutsem
 */
public class TestFreeVariableCapturing extends AmbientTalkTest {

	private void ensureEmpty(Set variables) {
		assertTrue(variables.toString() + " is empty", variables.isEmpty());
	}
	
	private void ensurePresent(String name, Set variables) {
		assertTrue(name + " present in "+variables, variables.contains(AGSymbol.jAlloc(name)));
		assertTrue("no extra vars in "+variables, variables.size() == 1);
	}
	
	private void ensurePresent(String name1, String name2, Set variables) {
		assertTrue(name1 + " present in "+variables, variables.contains(AGSymbol.jAlloc(name1)));
		assertTrue(name2 + " present in "+variables, variables.contains(AGSymbol.jAlloc(name2)));
		assertTrue("no extra vars in "+variables, variables.size() == 2);
	}
	
	private void ensurePresent(String name1, String name2, String name3, Set variables) {
		assertTrue(name1 + " present in "+variables, variables.contains(AGSymbol.jAlloc(name1)));
		assertTrue(name2 + " present in "+variables, variables.contains(AGSymbol.jAlloc(name2)));
		assertTrue(name3 + " present in "+variables, variables.contains(AGSymbol.jAlloc(name3)));
		assertTrue("no extra vars in "+variables, variables.size() == 3);
	}
	
	private void ensurePresent(String[] names, Set variables) {
		for (int i = 0; i < names.length; i++) {
			assertTrue(names[i] + " present in "+variables, variables.contains(AGSymbol.jAlloc(names[i])));
		}
		assertTrue("no extra vars in "+variables, variables.size() == names.length);
	}
	
	private Set freeVarsOf(String text) throws InterpreterException {
		return NATParser.parse("TestFreeVariableCapture unit test", text).impl_freeVariables();
	}
	
	public Set introducedVarsOf(String definition) throws InterpreterException {
		ATBegin begin = NATParser.parse("unit test", definition).asBegin();
		ATObject[] stmts = begin.base_statements().asNativeTable().elements_;
		return stmts[0].asDefinition().impl_introducedVariables();
	}
	
	public void testElementaryExpressions() throws InterpreterException {
		ensureEmpty(freeVarsOf("1"));
		ensureEmpty(freeVarsOf("\"text\""));
		ensureEmpty(freeVarsOf("self"));
		ensureEmpty(freeVarsOf("{}"));
		ensureEmpty(freeVarsOf("[]"));
		ensureEmpty(freeVarsOf(".m(1)"));
	}
	
	public void testVariables() throws InterpreterException {
		ensurePresent("x", freeVarsOf("x"));
		ensurePresent("nil", freeVarsOf("nil"));
		ensurePresent("true", freeVarsOf("true"));
		ensurePresent("object:", freeVarsOf("object: { }"));
		ensurePresent("x", "y", freeVarsOf("{ x; y }"));
		ensurePresent("x", "y", freeVarsOf("[1,x,y,2]"));
		ensurePresent("x", freeVarsOf("<-m(x)"));
		ensurePresent("o","Foo","x", freeVarsOf("o<-m()@Foo(x)"));
		ensurePresent("f", freeVarsOf("f(1)"));
		ensurePresent("foo:bar:","x", freeVarsOf("foo: 1 bar: x"));
	}
	
	public void testCompositeExpressions() throws InterpreterException {
		ensurePresent("x", freeVarsOf("x+1"));
		ensurePresent("o", "x", freeVarsOf("o.m(x,1)"));
		ensurePresent("t", "i", "v", freeVarsOf("t[i+1] := t[i] - v"));
		ensurePresent("o", freeVarsOf("import o alias x := y exclude z"));
		ensurePresent("x", freeVarsOf("&x"));
		ensurePresent("head", "tail", freeVarsOf("[head, @tail]"));
	}
	
	public void testFreeVarsOfDefinitions() throws InterpreterException {
		ensureEmpty(freeVarsOf("def x := 1"));
		ensurePresent("v", freeVarsOf("def t[4] { 1 + v }"));
		ensureEmpty(freeVarsOf("{|x,y| x + y }"));
		ensurePresent("y", freeVarsOf("{ |x| x + y }"));
		ensureEmpty(freeVarsOf("def x := 1; def y[1] { 2 }; def z();"));
		ensurePresent("z", freeVarsOf("def x(y) { z + y }"));
		ensurePresent("t1","t2", freeVarsOf("def [x,y] := [t1,t2]"));
		ensurePresent("x","y","t", freeVarsOf("[x,y] := t"));
		ensurePresent("bar", freeVarsOf("deftype foo <: bar"));
		ensurePresent("x","y", freeVarsOf("def m(z) @ x { z + y + m() }"));
		ensurePresent("o", "y", freeVarsOf("def o.x := y"));
		ensurePresent("v", freeVarsOf("def m(x := v) { x }"));
		ensureEmpty(freeVarsOf("def f(x) { def g(y) { x + y } }"));
		ensurePresent("free", freeVarsOf("def f(x) { x }; def g(y) { f(free) + g(y) }"));
	    ensurePresent("free", freeVarsOf("def f(x) { def g(y) { {|z| x + g(y) + free + z } } }"));
	}

	public void testIntroducedVarsOfDefinitions() throws InterpreterException {
		ensurePresent("x", introducedVarsOf("def x := 1"));
		ensurePresent("y", introducedVarsOf("def y[1] { 2 }"));
		ensurePresent("z", introducedVarsOf("def z();"));
		ensureEmpty(introducedVarsOf("def o.z();"));
		ensureEmpty(introducedVarsOf("def o.x := y"));
		ensurePresent("foo", introducedVarsOf("deftype foo"));
		ensurePresent("x", "y", introducedVarsOf("def [x,y] := [1,2]"));
	}
	
	public void testQuotedFreeVars() throws InterpreterException {
		ensureEmpty(freeVarsOf("`x"));
		ensurePresent("foo", freeVarsOf("`{ def #(foo)(arg) { arg } }"));
		ensurePresent("y", freeVarsOf("`(x + #(y + 1))"));
		ensurePresent("t", freeVarsOf("`(x + #@t)"));
		ensurePresent("msg", freeVarsOf("`<-#msg(1)"));
		ensurePresent("msg", freeVarsOf("`(o.#msg(1))"));
	}
	
	public void testIsolateWithAutomaticLexicalScope() throws InterpreterException {
		evalAndCompareTo("def x := 42; def i := isolate: { def m() { x } }; i.m()", "42");
		// check whether explicit overriding still works
		evalAndTestException("def x2 := 42; def y  := 0; def i2 := isolate: { |y| def m() { x2 } }; i2.m()", XUndefinedSlot.class);
		
		// super should never be automatically imported
		evalAndCompareTo("def outer := 4; def obj := isolate: { def m() { outer }; super }; obj.m()", "4");

		// variables bound to methods are not automatically imported
		evalAndTestException("def f(); (isolate: { def m() { f() } }).m()", XUndefinedSlot.class);
		// but variables bound to closures are
		evalAndCompareTo("def g(); def gclo := &g; (isolate: { def m() { gclo() } }).m()", "nil");

		// global variables and functions should be automatically removed
		ensurePresent("isolate:", "!", "false", freeVarsOf("isolate: { !false }"));
		evalAndCompareTo("(isolate: { def myNot(b) { !b } }).myNot(true)","false");
	}
	
	public void testActorWithAutomaticLexicalScope() throws InterpreterException {
		evalAndCompareTo("def lex := 0; def a := actor: { lex }; nil", "nil");

		// global variables and functions should be automatically removed
		evalAndCompareTo("(actor: { def myNot(b) { !b } })<-myNot(true)","nil");
	}

}
