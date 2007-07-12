package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.NativeClosure;
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
	private final AGSymbol atZ_ = AGSymbol.alloc(NATText.atValue("z"));
	private final AGSymbol atM_ = AGSymbol.alloc(NATText.atValue("m"));
	private final AGSymbol atFooBar_ = AGSymbol.alloc(NATText.atValue("foo:bar:"));
	
	/**
	 * Auxiliary construct to be used in the test suite to execute a closure within the
	 * scope of a given scope object. This is used to test the semantics of call and 
	 * lookup in particularly in combination with native and symbiotic objects. 
	 */
	private final ATClosure atWithScope_Do_ = new NativeClosure(NATNil._INSTANCE_) {
		public ATObject base_apply(ATTable arguments) throws InterpreterException {
			ATObject closureScope = arguments.base_at(NATNumber.ONE);
			ATClosure literalClosure = arguments.base_at(NATNumber.atValue(2)).asClosure();
			
			return new NATClosure(
					literalClosure.base_method(), 
					new NATContext(closureScope, literalClosure.base_context().base_self()))
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
		evalAndCompareTo("def abs := (-1).&abs", "<native closure:abs>");
		evalAndCompareTo("def len := [1, 2, 3].&length", "<native closure:length>");
		evalAndCompareTo("abs()", NATNumber.ONE);
		evalAndCompareTo("len()", atThree_);
		
		// selection gives up to date info, not stale one recorded at selection time
		// first we select an accessor for the receiver exp of an invocation
		evalAndReturn("def x := `(o.m()); \n" +
				      "def receiver := x.&receiverExpression");
		// subsequently assign the receiver expression with a new value
		evalAndReturn("x.receiverExpression := `object");
		// finally assert that the new value is correctly reported
		evalAndCompareTo("receiver()", AGSymbol.jAlloc("object"));
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
		evalAndCompareTo("def abs := withScope: (-1) do: { &abs }", "<native closure:abs>");
		evalAndCompareTo("def len := withScope: [1, 2, 3] do: { &length }", "<native closure:length>");
		evalAndCompareTo("abs()", NATNumber.ONE);
		evalAndCompareTo("len()", atThree_);
		
		// lookup gives up to date info, not stale one recorded at lookup time
		// first we create an accessor for the receiver exp of an invocation
		evalAndReturn("def x := `(o.m()); \n" +
				      "def receiver := withScope: x do: { &receiverExpression }");
		// subsequently assign the receiver expression with a new value
		evalAndReturn("x.receiverExpression := `object");
		// finally assert that the new value is correctly reported
		evalAndCompareTo("receiver()", AGSymbol.jAlloc("object"));
	}
	
	/**
	 * The correctness of assignments on native data types is verified by the four previous tests in
	 * the course of testing whether the returned accessors return up to date information rather than
	 * stale one recored when the accessor was created.
	 * 
	 * This test does not rely on the correct functioning of the accessor but instead manually checks 
	 * the output of the implementation-level accessor and is therefore complementary to the previous
	 * tests as it allows determining whether the semantics of assignment or of the created accessor
	 * are incorrect.
	 */
	public void testAssignmentOnNatives() throws InterpreterException {
		AGMethodInvocationCreation msgExp = new AGMethodInvocationCreation(
				/* selector = */	atM_, 
				/* arguments = */	NATTable.EMPTY, 
				/* annotations = */	NATTable.EMPTY);
		AGMessageSend sendExp= new AGMessageSend(
				/* receiver = */	atX_,
				/* message = */		msgExp);
		ctx_.base_lexicalScope().meta_defineField(AGSymbol.jAlloc("sendExp"), sendExp);

		evalAndReturn("sendExp.receiverExpression := `y");
		assertEquals(sendExp.base_receiverExpression(), atY_); 
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
		
		evalAndCompareTo("selSize",   NATNumber.ONE);
		evalAndCompareTo("selSize()", NATNumber.ONE);
		evalAndCompareTo("lexSize",   NATNumber.ONE);
		evalAndCompareTo("lexSize()", NATNumber.ONE);

		evalAndCompareTo("selElementCount",   NATNumber.ONE);
		evalAndCompareTo("selElementCount()", NATNumber.ONE);
		evalAndCompareTo("lexElementCount",   NATNumber.ONE);
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
		evalAndCompareTo("cplxm.select(cplx, `clofield)", "<closure:lambda>");
		
		// test whether explicit invocation on mirrors can abstract over fields
		// or methods or fields containing closures
		evalAndCompareTo("cplxm.invoke(cplx, `im, [])", "1");
		evalAndCompareTo("cplxm.invoke(cplx, `re, [])", "42");
		evalAndCompareTo("cplxm.invoke(cplx, `im:=, [4])", "4");
		evalAndCompareTo("cplxm.invoke(cplx, `re:=,[3])", "5");
		evalAndCompareTo("cplxm.invoke(cplx, `clofield, [])", "5");
	}
	
}
