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
		evalAndCompareTo("mirrorOn1.invoke(mirrorOn1.base, `+, [2]).base", "3");
		evalAndTestException("mirrorOn1.invoke(mirrorOn1.base, `foo, [2]).base", XSelectorNotFound.class);
	}
	
	public void testRespondsTo() {
		evalAndCompareTo("mirrorOn1.respondsTo(`+).base", "true");
		evalAndCompareTo("mirrorOn1.respondsTo(`foo).base", "false");
		evalAndCompareTo("(reflect: mirrorOn1).respondsTo(`invoke).base", "true");
	}
	
	public void testSelection() {
		try {
			ATObject clo = evalAndReturn("mirrorOn1.select(mirrorOn1.base, `+).base");
			assertTrue(clo.base_isClosure());
			ATClosure c = clo.base_asClosure();
			ATMethod m = c.base_getMethod();
			assertEquals(NativeMethod.class, m.getClass());
			assertEquals(AGSymbol.jAlloc("+"), m.base_getName());
			assertEquals(NATNumber.atValue(2), c.base_apply(new NATTable(new ATObject[] { NATNumber.ONE })));
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testAddition() {
		evalAndTestException("mirrorOn1.addField(object: { nil })", XIllegalOperation.class);
		evalAndTestException("mirrorOn1.addMethod(mirrorOn1.grabMethod(`+).base)", XIllegalOperation.class);
	}
	
	public void testAcquisition() {
		try {
			ATMethod nativeMethod = evalAndReturn("mirrorOn1.grabMethod(`+).base").base_asMethod();
			assertEquals(NativeMethod.class, nativeMethod.getClass());
			assertEquals(AGSymbol.jAlloc("+"), nativeMethod.base_getName());
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testListing() {
		try {
			evalAndReturn("mirrorOn1.listMethods().base").base_asTable();
			evalAndCompareTo("mirrorOn1.listFields().base", "[]");
			// when mirroring a mirror and querying its fields, the field 'base' should always be present
			evalAndCompareTo("{ |exit| (reflect: mirrorOn1).listFields().base.each: { |field|" +
					         "  if: (field.name == `base) then: { exit(`foundit) } } }.escape()", "foundit");
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}

}
