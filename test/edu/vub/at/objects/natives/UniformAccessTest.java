package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.grammar.AGMessageSend;
import edu.vub.at.objects.natives.grammar.AGMethodInvocationCreation;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Unit test class to verify whether the uniform access principle is upheld both for invocations and lexical
 * function calls. This test covers both the invocation and selection (resp. call and lookup) operations.
 * 
 * @author smostinc
 */
public class UniformAccessTest extends AmbientTalkTest {
	
	/**
	 * Used to run this test suite independently of the InterpreterTests Suite
	 */
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(UniformAccessTest.class);
	}
	
	/**
	 * Constructor which installs the <tt>withScope: scope do: closure</tt> construct in the 
	 * scope in which tests are executed. 
	 */
	public UniformAccessTest() throws InterpreterException {
		super(); // sets up the ctx_ properly
		
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("withScope:do:"), atWithScope_Do_);
	}
	
	private final NATNumber atThree_ = NATNumber.atValue(3);
	private final AGSymbol atX_ = AGSymbol.alloc(NATText.atValue("x"));
	private final AGSymbol atY_ = AGSymbol.alloc(NATText.atValue("y"));
	private final AGSymbol atM_ = AGSymbol.alloc(NATText.atValue("m"));
	
	/**
	 * Auxiliary construct to be used in the test suite to execute a closure within the
	 * scope of a given scope object. This is used to test the semantics of call and 
	 * lookup in particularly in combination with native and symbiotic objects. 
	 */
	private final ATClosure atWithScope_Do_ = new NativeClosure(Evaluator.getNil()) {
		public ATObject base_apply(ATTable arguments) throws InterpreterException {
			ATObject closureScope = arguments.base_at(NATNumber.ONE);
			ATClosure literalClosure = arguments.base_at(NATNumber.atValue(2)).asClosure();
			
			return literalClosure.base_method().base_wrap(closureScope, literalClosure.base_context().base_receiver())
			  .base_apply(NATTable.EMPTY);
		}
	};
		
	/**
	 * Tests whether the invoke operation treats fields (such as the length of a table) 
	 * and nullary methods (to e.g. compute the absolute value of a number) equally
	 * when invoked upon native data types.
	 * 
	 * The invocation is performed both using a field acessing and a canonical invocation
	 * syntax, and illustrates that both are equivalent.
	 */
	public void testUniformInvokeOnNatives() throws InterpreterException {
		// (-1).abs -> (-1).abs() -> 1
		// [1, 2, 3].length -> [1, 2, 3].length() -> 3
		evalAndCompareTo("(-1).abs", NATNumber.ONE);
		evalAndCompareTo("(-1).abs()", NATNumber.ONE);
		evalAndCompareTo("[1, 2, 3].length", atThree_);
		evalAndCompareTo("[1, 2, 3].length()", atThree_);
	}
	
	/**
	 * Tests whether the call operation treats fields (such as the length of a table) 
	 * and nullary methods (to e.g. compute the absolute value of a number) equally
	 * when invoked upon native data types.
	 * 
	 * The call is performed both using a field acessing and a canonical invocation
	 * syntax, and illustrates that both are equivalent.
	 */
	public void testUniformCallOnNatives() throws InterpreterException {
		// (-1).abs -> (-1).abs() -> 1
		// [1, 2, 3].length -> [1, 2, 3].length() -> 3
		evalAndCompareTo("withScope: (-1) do: { abs }", NATNumber.ONE);
		evalAndCompareTo("withScope: (-1) do: { abs() }", NATNumber.ONE);
		evalAndCompareTo("withScope: [1, 2, 3] do: { length }", atThree_);	
		evalAndCompareTo("withScope: [1, 2, 3] do: { length() }", atThree_);	
	}
	
	/**
	 * Tests the uniform selection of both fields and methods from native data types. The
	 * selection is uniform in the sense that both return closures which can subsequently be
	 * applied. 
	 * 
	 * When selecting a field, the resulting closure is in fact an implictly created accessor
	 * which provides access to the "current" value of the field, not the one when the field
	 * was selected.
	 */
	public void testUniformSelectionOnNatives() throws InterpreterException {
		// (-1).&abs -> <native impl> <+ apply([]) -> 1
		// [1, 2, 3].&length -> <native impl> <+ apply([]) -> 3
		evalAndCompareTo("def abs := (-1).&abs", "<closure:abs>");
		evalAndCompareTo("def len := [1, 2, 3].&length", "<closure:length>");
		evalAndCompareTo("abs()", NATNumber.ONE);
		evalAndCompareTo("len()", atThree_);
		
		// selection gives up to date info, not stale one recorded at selection time
		// first we select an accessor for an object attribute
		evalAndReturn("def x := object: { def val := 0 }; \n" +
				      "def attribute := x.&val");
		// subsequently assign the attribute to a new value
		evalAndReturn("x.val := 1");
		// finally assert that the new value is correctly reported
		evalAndCompareTo("attribute()", NATNumber.ONE);
	}
	
	/**
	 * Tests the uniform lookup of both variables and functions in native data types. The
	 * lookup is uniform in the sense that both return closures which can subsequently be
	 * applied. 
	 * 
	 * When looking up a variable, the resulting closure is in fact an implictly created 
	 * accessor which provides access to the "current" value of the variable, not the one 
	 * when the accessor was created.
	 */
	public void testUniformLookupOnNatives() throws InterpreterException {
		// (-1).&abs -> <native impl> <+ apply([]) -> 1
		// [1, 2, 3].&length -> <native impl> <+ apply([]) -> 3
		evalAndCompareTo("def abs := withScope: (-1) do: { &abs }", "<closure:abs>");
		evalAndCompareTo("def len := withScope: [1, 2, 3] do: { &length }", "<closure:length>");
		evalAndCompareTo("abs()", NATNumber.ONE);
		evalAndCompareTo("len()", atThree_);
		
		// selection gives up to date info, not stale one recorded at selection time
		// first we select an accessor for an object attribute
		evalAndReturn("def x := object: { def val := 0 }; \n" +
				      "def attribute := withScope: x do: { &val }");
		// subsequently assign the attribute to a new value
		evalAndReturn("x.val := 1");
		// finally assert that the new value is correctly reported
		evalAndCompareTo("attribute()", NATNumber.ONE);
	}
	
	/**
	 * Tests lexical field mutator access.
	 */
	public void testLexicalMutatorAccess() throws InterpreterException {
		evalAndReturn("def testobj := object: { def x := 5; def m() { nil }; def c := { 5 } }");
		evalAndCompareTo("withScope: testobj do: { &x:= }", "<native closure:x:=>");
		// there is no implicit mutator for nullary methods
		evalAndTestException("withScope: testobj do: { &m:= }", XUnassignableField.class);
		evalAndCompareTo("withScope: testobj do: { &c:= }", "<native closure:c:=>");
	}

	/**
	 * The amalgamation of fields and methods by the uniform access principle allows the definition
	 * of setter methods which can be used as if assigning ordinary fields. This test verifies that
	 * the assignment of two fields (one standard field and one implemented with a custom accessor
	 * and mutator pair) can be performed in an identical fashion.
	 */
	public void testUniformAssignmentOnObjects() {
		evalAndReturn(
				"def time := object: { \n" +
				"  def elapsed := 12752; \n" +
				"\n" +
				"  def seconds() { (elapsed %  60) }; \n" +
				"  def minutes() { (elapsed /- 60) % 60 }; \n" +
				"  def hours()   { (elapsed /- 3600) }; \n" +
				"\n" +
				"  def seconds:=(newSeconds) { \n" +
				"    elapsed := elapsed - seconds + newSeconds; \n" +
				"    newSeconds \n" +
				"  }; \n" +
				"  def minutes:=(newMinutes) { \n" +
				"    elapsed := elapsed - (minutes * 60) + (newMinutes * 60); \n" +
				"    newMinutes \n" +
				"  }; \n" +
				"  def hours:=(newHours) {  \n" +
				"    elapsed := elapsed - (hours * 3600) + (newHours * 3600); \n" +
				"    newHours \n" +
				"  }; \n" +
				"}");
		evalAndCompareTo("time.hours", atThree_);
		evalAndCompareTo("time.hours := 1", NATNumber.ONE);
		
		evalAndCompareTo("time.elapsed", "5552");
		evalAndCompareTo("time.elapsed := 12752", "12752");
		
		evalAndCompareTo("withScope: time do: { hours }", atThree_);
		evalAndCompareTo("withScope: time do: { hours := 1 }", NATNumber.ONE);
		
		evalAndCompareTo("withScope: time do: { elapsed }", "5552");
		evalAndCompareTo("withScope: time do: { elapsed := 12752 }", "12752");
	}
	
	/**
	 * Tests both field and variable access as well as method invocation and function calls occurring
	 * in the scope of a symbiotic JavaObject wrapper. Both the canonical application and variable access 
	 * syntax are tested.
	 * 
	 * This test uses both the size method and the elementCount java field to illustrate that both can
	 * be used interchangibly in AmbientTalk.
	 */
	public void testUniformAccessOnSymbionts() {
		// use a variant of vector which has a public field instead of a protected one
		evalAndReturn("def jVector := jlobby.edu.vub.at.objects.natives.VectorProxy.new()");
		
		evalAndCompareTo("jVector.size", NATNumber.ZERO);
		evalAndCompareTo("jVector.size()", NATNumber.ZERO);
		evalAndCompareTo("jVector.elementCount", NATNumber.ZERO);
		evalAndCompareTo("jVector.elementCount()", NATNumber.ZERO);
		
		evalAndCompareTo("withScope: jVector do: { size }", NATNumber.ZERO);
		evalAndCompareTo("withScope: jVector do: { size() }", NATNumber.ZERO);
		evalAndCompareTo("withScope: jVector do: { elementCount }", NATNumber.ZERO);
		evalAndCompareTo("withScope: jVector do: { elementCount() }", NATNumber.ZERO);
	}
	
	/**
	 * Tests both the selection and lookup of both methods and implict field accessors in the scope of 
	 * a symbiotic JavaObject wrapper. Both the canonical application and variable access syntax are tested.
	 * 
	 */
	public void testUniformSelectionOnSymbionts() {
		evalAndReturn("def jVector := jlobby.edu.vub.at.objects.natives.VectorProxy.new()");
		
		evalAndCompareTo(
				"def selSize := jVector.&size", 
				"<java closure:size>");
		evalAndCompareTo(
				"def lexSize := \n" +
				  "withScope: jVector do: { &size }", 
				"<java closure:size>");
		evalAndCompareTo(
				"def selElementCount := jVector.&elementCount", 
				"<native closure:elementCount>");
		evalAndCompareTo(
				"def lexElementCount := \n" +
				  "withScope: jVector do: { &elementCount }", 
				"<native closure:elementCount>");
		
		evalAndReturn("jVector.add( [4, 8, 15, 16, 23, 42] )");
		
		evalAndCompareTo("selSize",   "<java closure:size>");
		evalAndCompareTo("selSize()", NATNumber.ONE);
		evalAndCompareTo("lexSize",   "<java closure:size>");
		evalAndCompareTo("lexSize()", NATNumber.ONE);

		evalAndCompareTo("selElementCount",   "<native closure:elementCount>");
		evalAndCompareTo("selElementCount()", NATNumber.ONE);
		evalAndCompareTo("lexElementCount",   "<native closure:elementCount>");
		evalAndCompareTo("lexElementCount()", NATNumber.ONE);	
	}
	
	/**
	 * Tests whether abstraction can be made over the accessor or a slot,
	 * independent of whether a slot is implemened as a field or as a pair
	 * of methods.
	 */
	public void testMutatorSelection() {
		evalAndReturn("def v; def pair := object: {" +
				"def x := 1;" +
				"def y() {v};" +
				"def y:=(v2) { v := v2; v } }");
		
		// test mutator for field x
		evalAndCompareTo("def xmutator := pair.&x:=", "<native closure:x:=>");
		evalAndCompareTo("xmutator(2)", "2");
		evalAndTestException("xmutator()", XArityMismatch.class);
		evalAndCompareTo("pair.x", "2");
		
		// test mutator for virtual field y
		evalAndCompareTo("def ymutator := pair.&y:=", "<closure:y:=>");
		evalAndCompareTo("ymutator(2)", "2");
		evalAndTestException("ymutator()", XArityMismatch.class);
		evalAndCompareTo("pair.y", "2");
	}
	
	/**
	 * Tests how invocation, calling, lookup and select interact with
	 * fields, methods and fields bound to closures. Primarly, the uniform
	 * access principle is demonstrated by its ability to abstract over whether
	 * a field is implemented as a genuine field or as a set of methods.
	 */
	public void testUniformAccessPrinciple() {
		evalAndReturn(
				"def o := object: {" +
				"  def c := { 1 };" +
				"  def x := 2;" +
				"  def m() { 3 };" +
				"  def m:=(v) { 4 }" +
				"}");
		
		// dynamic uniform access
		evalAndCompareTo("o.x", NATNumber.atValue(2));
		evalAndCompareTo("o.m", atThree_); // for methods: o.m == o.m()
		evalAndCompareTo("o.x()", NATNumber.atValue(2));
		evalAndCompareTo("o.m()", atThree_);
		evalAndCompareTo("o.c", "<closure:lambda>"); // for closures: o.c != o.c()
		evalAndCompareTo("o.c()", NATNumber.ONE);
		
		evalAndCompareTo("o.x := 2", "2"); // assigns the field
		evalAndCompareTo("o.m := 0", "4"); // invokes the mutator
		evalAndCompareTo("o.c := { 1 }", "<closure:lambda>"); // assigns the field
		
		evalAndCompareTo("o.&x", "<native closure:x>"); // for fields: & returns accessor
		evalAndCompareTo("o.&m", "<closure:m>"); // for methods: & returns method closure
		evalAndCompareTo("o.&c", "<native closure:c>"); // for closures: & returns accessor
		evalAndCompareTo("o.&x:=", "<native closure:x:=>"); // for fields: & returns mutator
		evalAndCompareTo("o.&m:=", "<closure:m:=>"); // for methods: & returns method closure
		evalAndCompareTo("o.&c:=", "<native closure:c:=>"); // for closures: & returns mutator
		
		// lexical uniform access
		evalAndCompareTo("withScope: o do: { x }", "2");
		evalAndCompareTo("withScope: o do: { m }", "3"); // for methods: m == m()
		evalAndCompareTo("withScope: o do: { x() }", "2");
		evalAndCompareTo("withScope: o do: { m() }", "3");
		evalAndCompareTo("withScope: o do: { c }", "<closure:lambda>"); // for closures: c != c()
		evalAndCompareTo("withScope: o do: { c() }", "1");
		
		evalAndCompareTo("withScope: o do: { x := 2 }", "2"); // assigns the field
		evalAndCompareTo("withScope: o do: { m := 0 }", "4"); // invokes the mutator
		evalAndCompareTo("withScope: o do: { c := { 1 } }", "<closure:lambda>"); // assigns the field
		
		evalAndCompareTo("withScope: o do: { &x }", "<native closure:x>"); // for fields: & returns accessor
		evalAndCompareTo("withScope: o do: { &m }", "<closure:m>"); // for methods: & returns method closure
		evalAndCompareTo("withScope: o do: { &c }", "<native closure:c>"); // for closures: & returns accessor
		evalAndCompareTo("withScope: o do: { &x:= }", "<native closure:x:=>"); // for fields: & returns mutator
		evalAndCompareTo("withScope: o do: { &m:= }", "<closure:m:=>"); // for methods: & returns method closure
		evalAndCompareTo("withScope: o do: { &c:= }", "<native closure:c:=>"); // for closures: & returns mutator
		
	}

	/**
	 * Tests whether abstraction can be made over the accessor and mutator
	 * of a slot at the meta-level, independent of whether a slot is implemened
	 * as a field or as a pair of methods.
	 */
	public void testUniformAccessViaMirrors() {
		evalAndReturn("def rre := 42; def cplx := object: {" +
				"def clofield := { 5 };" +
				"def im := 1;" +
				"def re() { rre };" +
				"def re:=(v) { rre := v; rre+2 }" +
				"}");
		evalAndReturn("def cplxm := reflect: cplx");
		
		// test whether selection on mirrors can abstract over fields or methods
		// or fields containing closures
		evalAndCompareTo("cplxm.select(cplx, `im)", "<native closure:im>");
		evalAndCompareTo("cplxm.select(cplx, `re)", "<closure:re>");
		evalAndCompareTo("cplxm.select(cplx, `im:=)", "<native closure:im:=>");
		evalAndCompareTo("cplxm.select(cplx, `re:=)", "<closure:re:=>");
		evalAndCompareTo("cplxm.select(cplx, `clofield)", "<native closure:clofield>");
		
		// test whether explicit invocation on mirrors can abstract over fields
		// or methods or fields containing closures
		evalAndCompareTo("cplxm.invoke(cplx, .im())", "1");
		evalAndCompareTo("cplxm.invoke(cplx, .re())", "42");
		evalAndCompareTo("cplxm.invoke(cplx, `(.#(`im:=)(4)))", "4"); // cplxm.invoke(cplx, .im:=(4)) but parser does not support this (yet)
		evalAndCompareTo("cplxm.invoke(cplx, `(.#(`re:=)(3)))", "5");
		evalAndCompareTo("cplxm.invoke(cplx, .clofield())", "5"); // cplx.clofield() = 5
		evalAndCompareTo("cplxm.invokeField(cplx, `clofield)", "<closure:lambda>"); // cplx.clofield = <lambda>
	}
	
	/**
	 * This test is written following a bug report where the following happened:
	 * <code>
	 * def clo() { 5 }
	 * &clo<-apply([])@FutureMessage
	 * </code>
	 * 
	 * The interpreter complained that "apply" cound not be found in "5". Hence,
	 * it applied the closure 'too early' and sent apply to the return value of the
	 * closure instead.
	 * 
	 * The cause: the future message annotation caused the actual message
	 * being sent to be a real AmbientTalk object that was coerced into an
	 * {@link ATAsyncMessage}. However, the
	 * {@link Reflection#downInvocation(ATObject, java.lang.reflect.Method, ATObject[])}
	 * method failed to recognize a nullary method invocation as a field access
	 * and hence treated 'msg.receiver' as 'msg.receiver()'. Since 'receiver'
	 * was bound to a closure, the closure was automatically applied, rather
	 * than simply being returned.
	 */
	public void testCoercedFieldAccess() throws InterpreterException {
		// originally, the test was performed on actual asynchronous messages because
		// they encapsulated their receiver. This is no longer true, hence we
		// test the same principle (whether downInvocation gets it right) on an ATField instead
		
		// construct a coerced asynchronous message
		// ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("AsyncMsg"), NativeTypeTags._ASYNCMSG_);
		// ATAsyncMessage msg = evalAndReturn("object: { def receiver := { 5 } } taggedAs: [AsyncMsg]").asAsyncMessage();
		// rcv should be { 5 }, not 5
		// ATObject rcv = msg.base_receiver();
		
		// construct a coerced field object
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("Field"), NativeTypeTags._FIELD_);
		ATField fld = evalAndReturn("object: { def readField := { 5 } } taggedAs: [Field]").asField();
		// readField should be { 5 }, not 5
		ATObject val = fld.base_readField();
		
		assertFalse(val.meta_isTaggedAs(NativeTypeTags._NUMBER_).asNativeBoolean().javaValue);
		assertTrue(val.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue);
		assertEquals(NATNumber.atValue(5), val.asClosure().base_apply(NATTable.EMPTY));
	}
	
}
