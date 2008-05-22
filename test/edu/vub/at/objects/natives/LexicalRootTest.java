package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XImportConflict;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGImport;
import edu.vub.at.objects.natives.grammar.AGSelf;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author tvcutsem
 *
 * The unit test LexicalRootTest tests globally visible methods in the lexical root.
 * 
 * TODO: finish me
 */
public class LexicalRootTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(LexicalRootTest.class);
	}
	
	//private OBJLexicalRoot root_ = (OBJLexicalRoot) OBJLexicalRoot.getGlobalLexicalScope().lexicalParent_;
	
	public void testLexicalRootFields() {
		evalAndCompareTo("nil", Evaluator.getNil());
		evalAndCompareTo("true", NATBoolean._TRUE_);
		evalAndCompareTo("false", NATBoolean._FALSE_);
		evalAndCompareTo("/", Evaluator.getLobbyNamespace());
	}

	private NATObject trait_;
	
	public static final AGSymbol atX_ = AGSymbol.jAlloc("x");
	public static final AGSymbol atM_ = AGSymbol.jAlloc("m");
	public static final AGSymbol atN_ = AGSymbol.jAlloc("n");
	
	/**
	 * Initialize the trait used for testing import:
	 * 
	 * def parent := object: { def n() { nil }; def m() { nil } }
	 * def trait := extend: parent with: {
	 *   def x := 0;
	 *   def m() { self }
	 * }
	 */
	public void setUp() throws Exception {
		super.setUp();
		NATObject parent = new NATObject();
		parent.meta_addMethod(new NATMethod(atN_, NATTable.EMPTY, new AGBegin(NATTable.of(Evaluator.getNil())), NATTable.EMPTY));
		parent.meta_addMethod(new NATMethod(atM_, NATTable.EMPTY, new AGBegin(NATTable.of(Evaluator.getNil())), NATTable.EMPTY));
		trait_ = new NATObject(parent, Evaluator.getGlobalLexicalScope(), NATObject._IS_A_);
		trait_.meta_defineField(atX_, NATNumber.ZERO);
		trait_.meta_addMethod(new NATMethod(atM_,
				                            NATTable.EMPTY,
				                            new AGBegin(NATTable.of(AGSelf._INSTANCE_)), NATTable.EMPTY));
	}
	
	/**
	 * Tests whether a basic import of fields and methods from a
	 * 'trait' object into a 'host' object works properly.
	 */
	public void testBasicImport() throws InterpreterException {
		// def host := object: { def test() { x } }
		NATObject host = new NATObject();
		AGSymbol atTest = AGSymbol.jAlloc("test");
		// test method accesses 'x' unqualified
		host.meta_addMethod(new NATMethod(atTest, NATTable.EMPTY,
				                  new AGBegin(NATTable.of(atX_)), NATTable.EMPTY));
		
		// < import trait > . eval(ctx[lex=host;self=host])
		new AGImport(trait_, NATTable.EMPTY, NATTable.EMPTY).meta_eval(new NATContext(host, host));
		
		// check whether host contains the appropriate fields and methods of the traits
		assertTrue(host.meta_respondsTo(atX_).asNativeBoolean().javaValue);
		assertTrue(host.meta_respondsTo(atM_).asNativeBoolean().javaValue);
		
		// check whether the methods and fields of the parents of traits are also present
		assertTrue(host.meta_respondsTo(atN_).asNativeBoolean().javaValue);
		
		// ensure that 'self' is correctly late bound to host when invoking m()
		assertEquals(host, host.meta_invoke(host, atM_, NATTable.EMPTY));
		// when invoking m() directly on the trait, self should be bound to the trait
		assertEquals(trait_, trait_.meta_invoke(trait_, atM_, NATTable.EMPTY));
		
		// when someone delegates m() to host, the trait's self should be bound to the original delegator
		NATObject delegator = new NATObject();
		assertEquals(delegator, host.meta_invoke(delegator, atM_, NATTable.EMPTY));
		
		// ensure that when invoking test() on host, it can access x unqualified
		assertEquals(NATNumber.ZERO, host.meta_invoke(host, atTest, NATTable.EMPTY));
		
		// when assigning x in host, trait's x field should not be modified
		host.meta_invoke(host, atX_.asAssignmentSymbol(), NATTable.of(NATNumber.ONE));
		assertEquals(NATNumber.ZERO, trait_.impl_invokeAccessor(trait_, atX_, NATTable.EMPTY));
		
		// host's primitive methods should be left untouched, i.e.
		// host != trait and host == host
		assertTrue(host.meta_invoke(host, NATObject._EQL_NAME_, NATTable.of(host)).asNativeBoolean().javaValue);
		assertFalse(host.meta_invoke(host, NATObject._EQL_NAME_, NATTable.of(trait_)).asNativeBoolean().javaValue);
	}
	
	/**
	 * Tests whether conflicts are successfully detected.
	 */
	public void testConflictingImport() throws InterpreterException {
		NATObject host = new NATObject();
		
		// host defines x itself, so import should fail
		host.meta_defineField(atX_, NATNumber.ONE);
		
		try {
			// < import trait > . eval(ctx[lex=host;self=host])
			new AGImport(trait_, NATTable.EMPTY, NATTable.EMPTY).meta_eval(new NATContext(host, host));
			fail("Expected an XImportConflict exception");
		} catch (XImportConflict e) {
			assertEquals(atX_, e.getConflictingNames().base_at(NATNumber.ONE));
		}
	}
	
	/**
	 * Tests whether traits can be transitively imported into objects.
	 */
	public void testTransitiveImport() throws InterpreterException {
		NATObject hostA = new NATObject();
		NATObject hostB = new NATObject();
		// < import trait > . eval(ctx[lex=hostA;self=hostA])
		new AGImport(trait_, NATTable.EMPTY, NATTable.EMPTY).meta_eval(new NATContext(hostA, hostA));
		// < import trait > . eval(ctx[lex=hostB;self=hostB])
		new AGImport(trait_, NATTable.EMPTY, NATTable.EMPTY).meta_eval(new NATContext(hostB, hostB));
		
		// check whether m() can be invoked from hostB and that self equals hostB
		assertEquals(hostB, hostB.meta_invoke(hostB, atM_, NATTable.EMPTY));
	}
	
	/**
	 * Tests whether aliasing works properly.
	 */
	public void testImportAndAliasing() throws InterpreterException {
		NATObject host = new NATObject();
		AGSymbol foo = AGSymbol.jAlloc("foo");
		NATTable alias = NATTable.of(NATTable.of(atM_, foo)); // [[m,foo]]
		// < import trait alias m := foo > . eval(ctx[lex=host;self=host])
		new AGImport(trait_, alias, NATTable.EMPTY).meta_eval(new NATContext(host, host));
		
		// check whether m() can be invoked as foo()
		assertTrue(host.meta_respondsTo(foo).asNativeBoolean().javaValue);
		assertEquals(host, host.meta_invoke(host, foo, NATTable.EMPTY));
	}
	
	/**
	 * Tests whether exclusion works properly.
	 */
	public void testImportAndExclusion() throws InterpreterException {
		NATObject host = new NATObject();
		NATTable exclude = NATTable.of(atN_);
		// < import trait exclude n > . eval(ctx[lex=host;self=host])
		new AGImport(trait_, NATTable.EMPTY, exclude).meta_eval(new NATContext(host, host));
		
		// check whether m is present and n is not present
		assertTrue(host.meta_respondsTo(atM_).asNativeBoolean().javaValue);
		assertFalse(host.meta_respondsTo(atN_).asNativeBoolean().javaValue);
	}
	
	/**
	 * Tests whether aliasing and exclusion work properly together.
	 */
	public void testImportAndAliasingAndExclusion() throws InterpreterException {
		NATObject host = new NATObject();
		AGSymbol foo = AGSymbol.jAlloc("foo");
		NATTable alias = NATTable.of(NATTable.of(atM_, foo)); // [[m,foo]]
		NATTable exclude = NATTable.of(atN_);
		// < import trait alias m := foo exclude n> . eval(ctx[lex=host;self=host])
		new AGImport(trait_, alias, exclude).meta_eval(new NATContext(host, host));
		
		// check whether m() can be invoked as foo()
		assertTrue(host.meta_respondsTo(foo).asNativeBoolean().javaValue);
		assertEquals(host, host.meta_invoke(host, foo, NATTable.EMPTY));
		
		// check whether n is not present
		assertFalse(host.meta_respondsTo(atN_).asNativeBoolean().javaValue);
	}
}
