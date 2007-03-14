package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Tests stripe definition, tagging, checking.
 * 
 * @author tvcutsem
 */
public class StripesTest extends AmbientTalkTest {

	// test fixture
	
	// defstripe A <: stripe;
	private NATStripe stripeA_ = NATStripe.atValue(AGSymbol.jAlloc("A"), NATTable.EMPTY);

	// defstripe B <: A;
	private NATStripe stripeB_ = NATStripe.atValue(AGSymbol.jAlloc("B"),
			NATTable.atValue(new ATObject[] { stripeA_ }));
	
	// defstripe C <: A;
	private NATStripe stripeC_ = NATStripe.atValue(AGSymbol.jAlloc("C"),
			NATTable.atValue(new ATObject[] { stripeA_ }));
	
	// defstripe D <: B, C;
	private NATStripe stripeD_ = NATStripe.atValue(AGSymbol.jAlloc("D"),
			NATTable.atValue(new ATObject[] { stripeB_, stripeC_ }));
	
	// object: { nil } stripedWith: [ B ]
	private NATObject striped_ = new NATObject(Evaluator.getGlobalLexicalScope(), new ATStripe[] { stripeB_ });
	
	// stripes defined without parent have the root stripe as single parent
	
	public void testParentlessStripe() throws InterpreterException {
		assertTrue(stripeA_.base_getParentStripes().base_getLength().equals(NATNumber.ONE));
		
		ATStripe root = stripeA_.base_getParentStripes().base_at(NATNumber.ONE).asStripe();
		assertEquals(NATStripe.OBJRootStripe._INSTANCE_, root);
	}
	
	// stripe subtyping:
	
	public void testPositiveSubstriping() throws InterpreterException {
		ATStripe root = NATStripe.OBJRootStripe._INSTANCE_;
        // root <: root
		assertTrue(root.base_isSubstripeOf(root).asNativeBoolean().javaValue);
        // A <: A
		assertTrue(stripeA_.base_isSubstripeOf(stripeA_).asNativeBoolean().javaValue);
		// A <: root
		assertTrue(stripeA_.base_isSubstripeOf(root).asNativeBoolean().javaValue);
		// B <: A
		assertTrue(stripeB_.base_isSubstripeOf(stripeA_).asNativeBoolean().javaValue);
		// D <: B
		assertTrue(stripeD_.base_isSubstripeOf(stripeB_).asNativeBoolean().javaValue);
		// D <: C
		assertTrue(stripeD_.base_isSubstripeOf(stripeC_).asNativeBoolean().javaValue);
		// D <: A
		assertTrue(stripeD_.base_isSubstripeOf(stripeA_).asNativeBoolean().javaValue);
		// D <: root
		assertTrue(stripeD_.base_isSubstripeOf(root).asNativeBoolean().javaValue);
	}
	
	public void testNegativeSubstriping() throws InterpreterException {
		ATStripe root = NATStripe.OBJRootStripe._INSTANCE_;
        // root <: A
		assertFalse(root.base_isSubstripeOf(stripeA_).asNativeBoolean().javaValue);
        // A <: D
		assertFalse(stripeA_.base_isSubstripeOf(stripeD_).asNativeBoolean().javaValue);
		// A <: B
		assertFalse(stripeA_.base_isSubstripeOf(stripeB_).asNativeBoolean().javaValue);
		// B <: C
		assertFalse(stripeB_.base_isSubstripeOf(stripeC_).asNativeBoolean().javaValue);
		// C <: B
		assertFalse(stripeC_.base_isSubstripeOf(stripeB_).asNativeBoolean().javaValue);
		// B <: D
		assertFalse(stripeB_.base_isSubstripeOf(stripeD_).asNativeBoolean().javaValue);
	}
	
	// object stripe testing:
	
	public void testObjectStripes() throws InterpreterException {
		// is: striped_ stripedWith: B => true
		assertTrue(striped_.meta_isStripedWith(stripeB_).asNativeBoolean().javaValue);
		// is: striped_ stripedWith: A => true
		assertTrue(striped_.meta_isStripedWith(stripeA_).asNativeBoolean().javaValue);
		// is: striped_ stripedWith: D => false
		assertFalse(striped_.meta_isStripedWith(stripeD_).asNativeBoolean().javaValue);
		// is: striped_ stripedWith: root => true
		assertTrue(striped_.meta_isStripedWith(NATStripe.OBJRootStripe._INSTANCE_).asNativeBoolean().javaValue);
		// is: striped_ stripedWith: C => false
		assertFalse(striped_.meta_isStripedWith(stripeC_).asNativeBoolean().javaValue);
		
	    // negative test: object has no stripes
		NATObject nostripes = new NATObject();
		assertFalse(nostripes.meta_isStripedWith(stripeB_).asNativeBoolean().javaValue);
	}
	
}
