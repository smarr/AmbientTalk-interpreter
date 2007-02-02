package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Tests custom fields that can be added to an object.
 * 
 * @author tvcutsem
 */
public class CustomFieldsTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(CustomFieldsTest.class);
	}

	private NATObject testHost_;
	// a custom field that when set, increments the new value by 1
	private ATObject testField_;
	private AGSymbol foo_;
	
	// one-time setUp for all tests
	public void setUp() throws InterpreterException {
		// def testHost := object: { def x := 1 }
		testHost_ = new NATObject();
		testHost_.meta_defineField(AGSymbol.jAlloc("x"), NATNumber.ONE);

		foo_ = AGSymbol.jAlloc("foo");
		
		testField_ = evalAndReturn(
				"object: { def name := `foo;" +
				          "def host := nil; def init(newhost) { host := newhost; };" +
				          "def v := nil;" +
				          "def readField() { v };" +
				          "def writeField(n) { v := n+1 } }");
	}
	
	/**
	 * Tests whether a custom field can be added to a native object
	 */
	public void testCustomFieldAddition() throws Exception {
		assertNull(testHost_.customFields_);
		// (reflect: testHost).addField(testField)
		testHost_.meta_addField(testField_.base_asField());
		assertNotNull(testHost_.customFields_);
		assertEquals(1, testHost_.customFields_.size());
		assertTrue(testHost_.meta_respondsTo(foo_).asNativeBoolean().javaValue);
		ATObject foo = testHost_.meta_grabField(foo_);
		assertEquals(testHost_, foo.meta_select(foo, AGSymbol.jAlloc("host")));
	}
	
	/**
	 * Tests whether a custom field can be read via readField
	 */
	public void testCustomFieldRead() throws Exception {
		testHost_.meta_addField(testField_.base_asField());
		assertEquals(NATNil._INSTANCE_, testHost_.meta_select(testHost_, foo_));
	}

	/**
	 * Tests whether a custom field can be written via writeField
	 */
	public void testCustomFieldWrite() throws Exception {
		testHost_.meta_addField(testField_.base_asField());
		// testHost.foo := 1
		assertEquals(NATNil._INSTANCE_, testHost_.meta_assignField(testHost_, foo_, NATNumber.ONE));
		// testHost.foo == 2
		assertEquals(NATNumber.atValue(2), testHost_.meta_select(testHost_, foo_));
	}
	
	/**
	 * Tests that duplicate slots are still trapped, even with custom fields
	 */
	public void testCustomDuplicate() throws Exception {
		testHost_.meta_addField(testField_.base_asField());
		try {
			// try to add a native field for which a custom one is already defined
			testHost_.meta_defineField(foo_, NATNumber.ONE);
			fail("expected a duplicate slot exception");
		} catch (XDuplicateSlot e) {
			// expected exception: success
		}
		try {
			// try to add a custom field for which another custom one is already defined
			testHost_.meta_addField(testField_.meta_clone().base_asField());
			fail("expected a duplicate slot exception");
		} catch (XDuplicateSlot e) {
			// expected exception: success
		}
	}
	
	/**
	 * Tests whether custom fields appear in the listFields table
	 */
	public void testFieldListing() throws Exception {
		testHost_.meta_addField(testField_.meta_clone().base_asField());
		assertEquals(3, testHost_.meta_listFields().base_getLength().asNativeNumber().javaValue);
	}
	
	/**
	 * Tests whether the fields of clones are properly re-initialized
	 */
	public void testCloneFieldReinit() throws Exception {
		testHost_.meta_addField(testField_.meta_clone().base_asField());
		// set foo field of testHost to 1
		testHost_.meta_assignField(testHost_, foo_, NATNumber.ONE);
		ATObject clone = testHost_.meta_clone();
		// set foo field of clone to 55
		clone.meta_assignField(clone, foo_, NATNumber.atValue(55));
		// check whether original foo field of testHost is not modified (remember: writeField increments with + 1)
		assertEquals(2, testHost_.meta_select(testHost_, foo_).asNativeNumber().javaValue);
	}
	
	/**
	 * Tests whether native fields added to another object are not added as custom fields,
	 * but again as native fields
	 */
	public void testNativeFieldAdd() throws Exception {
		testHost_.meta_addField(testField_.meta_clone().base_asField());
		NATObject empty = new NATObject();
		assertNull(empty.customFields_);
		empty.meta_addField(testHost_.meta_grabField(AGSymbol.jAlloc("x")));
		assertNull(empty.customFields_);
		assertEquals(testHost_.meta_select(testHost_, AGSymbol.jAlloc("x")),
				empty.meta_select(empty, AGSymbol.jAlloc("x")));
		// only when custom fields are added does the customFields_ list grow
		empty.meta_addField(testHost_.meta_grabField(foo_));
		assertNotNull(empty.customFields_);
	}
	
}
