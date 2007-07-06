package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Tests type definition, tagging, checking.
 * 
 * @author tvcutsem
 */
public class TypeTagTest extends AmbientTalkTest {

	// test fixture
	
	// deftype A <: Type;
	private NATTypeTag typeA_ = NATTypeTag.atValue(AGSymbol.jAlloc("A"), NATTable.EMPTY);

	// deftype B <: A;
	private NATTypeTag typeB_ = NATTypeTag.atValue(AGSymbol.jAlloc("B"),
			NATTable.atValue(new ATObject[] { typeA_ }));
	
	// deftype C <: A;
	private NATTypeTag typeC_ = NATTypeTag.atValue(AGSymbol.jAlloc("C"),
			NATTable.atValue(new ATObject[] { typeA_ }));
	
	// deftype D <: B, C;
	private NATTypeTag typeD_ = NATTypeTag.atValue(AGSymbol.jAlloc("D"),
			NATTable.atValue(new ATObject[] { typeB_, typeC_ }));
	
	// object: { nil } taggedAs: [ B ]
	private NATObject typed_ = new NATObject(Evaluator.getGlobalLexicalScope(), new ATTypeTag[] { typeB_ });
	
	// types defined without supertypes have the root type as single supertype
	
	public void testParentlessType() throws InterpreterException {
		assertTrue(typeA_.base_superTypes().base_length().equals(NATNumber.ONE));
		
		ATTypeTag root = typeA_.base_superTypes().base_at(NATNumber.ONE).asTypeTag();
		assertEquals(NATTypeTag.OBJRootType._INSTANCE_, root);
	}
	
	// type tag subtyping:
	
	public void testPositiveSubtyping() throws InterpreterException {
		ATTypeTag root = NATTypeTag.OBJRootType._INSTANCE_;
        // root <: root
		assertTrue(root.base_isSubtypeOf(root).asNativeBoolean().javaValue);
        // A <: A
		assertTrue(typeA_.base_isSubtypeOf(typeA_).asNativeBoolean().javaValue);
		// A <: root
		assertTrue(typeA_.base_isSubtypeOf(root).asNativeBoolean().javaValue);
		// B <: A
		assertTrue(typeB_.base_isSubtypeOf(typeA_).asNativeBoolean().javaValue);
		// D <: B
		assertTrue(typeD_.base_isSubtypeOf(typeB_).asNativeBoolean().javaValue);
		// D <: C
		assertTrue(typeD_.base_isSubtypeOf(typeC_).asNativeBoolean().javaValue);
		// D <: A
		assertTrue(typeD_.base_isSubtypeOf(typeA_).asNativeBoolean().javaValue);
		// D <: root
		assertTrue(typeD_.base_isSubtypeOf(root).asNativeBoolean().javaValue);
	}
	
	public void testNegativeSubtyping() throws InterpreterException {
		ATTypeTag root = NATTypeTag.OBJRootType._INSTANCE_;
        // root <: A
		assertFalse(root.base_isSubtypeOf(typeA_).asNativeBoolean().javaValue);
        // A <: D
		assertFalse(typeA_.base_isSubtypeOf(typeD_).asNativeBoolean().javaValue);
		// A <: B
		assertFalse(typeA_.base_isSubtypeOf(typeB_).asNativeBoolean().javaValue);
		// B <: C
		assertFalse(typeB_.base_isSubtypeOf(typeC_).asNativeBoolean().javaValue);
		// C <: B
		assertFalse(typeC_.base_isSubtypeOf(typeB_).asNativeBoolean().javaValue);
		// B <: D
		assertFalse(typeB_.base_isSubtypeOf(typeD_).asNativeBoolean().javaValue);
	}
	
	// object type testing:
	
	public void testObjectTypes() throws InterpreterException {
		// is: typed_ taggedAs: B => true
		assertTrue(typed_.meta_isTaggedAs(typeB_).asNativeBoolean().javaValue);
		// is: typed_ taggedAs: A => true
		assertTrue(typed_.meta_isTaggedAs(typeA_).asNativeBoolean().javaValue);
		// is: typed_ taggedAs: D => false
		assertFalse(typed_.meta_isTaggedAs(typeD_).asNativeBoolean().javaValue);
		// is: typed_ taggedAs: root => true
		assertTrue(typed_.meta_isTaggedAs(NATTypeTag.OBJRootType._INSTANCE_).asNativeBoolean().javaValue);
		// is: typed_ taggedAs: C => false
		assertFalse(typed_.meta_isTaggedAs(typeC_).asNativeBoolean().javaValue);
		
	    // negative test: object has no types
		NATObject notypes = new NATObject();
		assertFalse(notypes.meta_isTaggedAs(typeB_).asNativeBoolean().javaValue);
	}
	
}
