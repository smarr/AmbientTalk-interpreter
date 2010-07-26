package edu.vub.at.objects.mirrors;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * This class tests introspective mirror facilities on native objects.
 * 
 * @author tvcutsem
 */
public class MirrorsOnNativesTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(MirrorsOnNativesTest.class);
	}
	
	public void setUp() {
		evalAndReturn("def mirrorOn1 := (reflect: 1)");
	}
	
	public void testInvocation() {
		evalAndCompareTo("mirrorOn1.invoke(mirrorOn1.base, .+(2))", "3");
		evalAndTestException("mirrorOn1.invoke(mirrorOn1.base, .foo(2))", XSelectorNotFound.class);
	}
	
	public void testRespondsTo() {
		evalAndCompareTo("mirrorOn1.respondsTo(`+)", "true");
		evalAndCompareTo("mirrorOn1.respondsTo(`foo)", "false");
		evalAndCompareTo("(reflect: mirrorOn1).respondsTo(`invoke)", "true");
	}
	
	public void testSelection() {
		try {
			ATObject clo = evalAndReturn("mirrorOn1.select(mirrorOn1.base, `+)");
			ATClosure c = clo.asClosure();
			ATMethod m = c.base_method();
			//assertEquals(NativeMethod.class, m.getClass());
			assertEquals(AGSymbol.jAlloc("+"), m.base_name());
			assertEquals(NATNumber.atValue(2), c.base_apply(NATTable.atValue(new ATObject[] { NATNumber.ONE })));
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testAddition() {
		evalAndTestException("mirrorOn1.addField(object: { nil })", XIllegalOperation.class);
		evalAndTestException("mirrorOn1.addMethod(mirrorOn1.grabMethod(`+))", XIllegalOperation.class);
	}
	
	public void testAcquisition() {
		try {
			ATMethod nativeMethod = evalAndReturn("mirrorOn1.grabMethod(`+)").asMethod();
			assertEquals(NativeMethod.class, nativeMethod.getClass());
			assertEquals(AGSymbol.jAlloc("+"), nativeMethod.base_name());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testListing() {
		try {
			evalAndReturn("mirrorOn1.listMethods()").asTable();
			evalAndCompareTo("mirrorOn1.listFields()", "[]");
			// when mirroring a mirror and querying its methods, the accessor method 'base' should always be present
			evalAndCompareTo("{ |exit| (reflect: mirrorOn1).listMethods().each: { | method |" +
					         "  if: (method.name == `base) then: { exit(`foundit) } } }.escape()", "foundit");
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}

}
