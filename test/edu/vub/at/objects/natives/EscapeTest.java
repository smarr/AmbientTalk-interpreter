package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.XIllegalOperation;

/**
 * The purpose of this class is to test the escape() primitive method of block closures.
 * 
 * @author tvcutsem
 */
public class EscapeTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(EscapeTest.class);
	}
	
	public void testNominalCase() {
		evalAndCompareTo("{ |quit| 1 + 2; quit(3 + 4); 1/0 }.escape()", "7");
	}
	
	public void testNominalNilCase() {
		evalAndCompareTo("{ |quit| 1 + 2; quit(); 1/0 }.escape()", "nil");
	}
	
	public void testNestedCase() {
		evalAndCompareTo("{ |quit1| 1 + 2; { |quit2| quit1(3 + 4); 1/0 }.escape() ; 1/0 }.escape()", "7");
	}
	
	public void testDeepEscapeCase() {
		evalAndCompareTo("{ |quit| def foo(q) { bar(q); 1 }; def bar(q) { q(41); 2 }; foo(quit) }.escape() + 1", "42");
	}
	
	public void testNotInvokedCase() {
		evalAndCompareTo("{ |quit| 1 + 2; 3 + 4; 5 + 6 }.escape()", "11");
	}
	
	public void testFaultyCase() {
		evalAndTestException("def temp := nil; { |quit| temp := quit }.escape(); temp(5)", XIllegalOperation.class);
	}
	
	public void testFaultySelfEvalCase() {
		evalAndTestException("({ |quit| quit }.escape())()", XIllegalOperation.class);
	}
	
	public void testFaultyNestedCase() {
		evalAndTestException("def temp := nil; { |quit| { |quit2| temp := quit2; quit() }.escape() }.escape(); temp()", XIllegalOperation.class);
	}

}
