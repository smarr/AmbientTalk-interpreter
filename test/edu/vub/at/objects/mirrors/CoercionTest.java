package edu.vub.at.objects.mirrors;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import junit.framework.TestCase;

public class CoercionTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(CoercionTest.class);
	}
	
	public void testBaseLevelInvocations() {
		try {
			NATObject customClosure = new NATObject();
			customClosure.meta_defineField(AGSymbol.alloc("apply"), new JavaClosure(customClosure) {
				public ATObject base_apply(ATTable args) throws NATException {
					ATTable apply_args = get(args, 1).asTable();
					assertEquals(42, getNbr(apply_args, 1));
					return NATNil._INSTANCE_;
				}
			});
			customClosure.meta_defineField(AGSymbol.alloc("method"), new JavaClosure(customClosure) {
				public ATObject base_apply(ATTable args) throws NATException {
					return NATNil._INSTANCE_;
				}
			});
			ATClosure coercedObject = customClosure.asClosure();
			coercedObject.base_apply(new NATTable(new ATObject[] { NATNumber.atValue(42) }));
			assertTrue(coercedObject.meta_respondsTo(AGSymbol.alloc("apply")).asNativeBoolean().javaValue);
			assertEquals(customClosure.hashCode(), coercedObject.hashCode());
			
			// TODO: solve the problem of return value types
			//ATMethod m = coercedObject.base_getMethod();
		} catch (NATException e) {
			fail(e.getMessage());
		}
	}

}
