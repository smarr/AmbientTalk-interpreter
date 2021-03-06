/**
 * 
 */
package edu.vub.at.objects.symbiosis;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.base.BaseClosure;
import edu.vub.at.objects.coercion.Async;
import edu.vub.at.objects.coercion.Coercer;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * Tests the coercion of both objects and native data types to an interface. The use of coercion is required not
 * only to absorb AmbientTalk objects and use them instead of native types (i.e. passing an object with at and 
 * atPut methods where a table is expected) but is also used to ensure the proper use of AmbientTalk objects 
 * which are passed outside their enclosing actor and handed to another Java thread.
 * 
 * @author smostinc
 */
public class CoercionTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(CoercionTest.class);
	}
	
	/** 
	 * Tests the coercion of an AmbientTalk object which is to be used as a native table type.
	 */
	public void testTypeCoercion() throws InterpreterException {
		ATObject cubbyhole = evalAndReturn(
				"def cubbyhole := object: { \n" +
				"  def content := nil; \n" +
				"  def at(i) { \n" +
				"    if: (i = 1) then: { content } else: { `error }; \n" +
				"  }; \n" +
				"  def atPut(i, val) { \n" +
				"    if: (i = 1) then: { content := val } else: { `error }; \n" +
				"  }; \n" +
				"} \n");
		
		// under normal circumstances the cubbyhole object would be implicitly coerced to a table once it is
		// passed to a function which expects a table as an argument. Here we explicitly coerce by performing 
		// such an invocation at the Java level.
		
		ATTable coercedCubbyhole = (ATTable)Coercer.coerce(cubbyhole, ATTable.class);
		ATObject result = coercedCubbyhole.base_at(NATNumber.ONE);
		
		assertEquals(Evaluator.getNil(), result);
	}

	/**
	 * Tests the coercion of an AmbientTalk object onto a classical Java Interface for symbiotic use.
	 * Such coercions typically happen when passing an AmbientTalk object to a Java method which expects 
	 * a given interface. At that point in time the passed AmbientTalk object will be implictly coerced 
	 * to the requested type. 
	 */
	public void testSymbioticCoercion() throws InterpreterException {
		ATObject listener = evalAndReturn(
			"def result := `error;" +
			"def listener := object: { \n" +
			"  def run() { result := `ok; }; \n" +
			"}; \n");
				
		Runnable coercedListener = (Runnable)Coercer.coerce(listener, Runnable.class);
		coercedListener.run();
		
		ATObject result = evalAndReturn("result");
		assertEquals(AGSymbol.jAlloc("ok"), result);
	}
	
	/**
	 * Tests the coercion of an AmbientTalk native type onto a Java Interface corresponding to its base-level 
	 * interface for symbiotic use. The reason to support this is that coercion ensures that invoking a method
	 * respects the event loop concurrency properties of AmbientTalk actors. 
	 * <p>
	 * Hence, the difference between <closure:lambda>.base_apply([]) and <coercer on:<closure:lambda> >.apply([])
	 * is that is the latter is called from a separate Java thread, it will schedule a message in the owning
	 * actor's queue whereas the latter would proceed and thus activate a second thread within the boundaries
	 * of a single actor.
	 */
	public void testSymbioticNativeCoercion() throws InterpreterException {
		ATObject lambda = evalAndReturn(
			"def result := `error;" +
			"def lambda := { result := `ok; }; \n");
		
		
		BaseClosure coercedListener = (BaseClosure)Coercer.coerce(lambda, BaseClosure.class);
		coercedListener.apply(NATTable.EMPTY);
		
		ATObject result = evalAndReturn("result");
		assertEquals(AGSymbol.jAlloc("ok"), result);
	}
	
	/**
	 * Tests whether a coercer is AT-equals and Java-equals to its original
	 * unwrapped AmbientTalk object.
	 */
	public void testEqualityOnCoercers() throws InterpreterException {
		ATObject unwrapped = new NATObject();
		ATObject coercedObject = (ATObject) Coercer.coerce(unwrapped, Runnable.class);
		// they should not be the same Java object
		assertFalse(unwrapped == coercedObject);
		// they should be Java-equals
		assertTrue(unwrapped.equals(coercedObject));
		assertTrue(coercedObject.equals(unwrapped));
		// they should be AmbientTalk-equals
		assertTrue(unwrapped.equals(coercedObject));
		assertTrue(coercedObject.equals(unwrapped));
		// their hashcodes should be equal as well
		assertTrue(unwrapped.hashCode() == coercedObject.hashCode());
	}
	
	interface AsyncInterfaceTest {
		@Async
		void sleepLong();
	};
	
	public void testAsyncInvocation() throws InterpreterException {
		ATObject sleeper = evalInActor(
				"def sleeper := object: { \n" +
				"  def sleepLong() { 1.to: 5000000 do: { |x| } }; \n" +
				"}; \n" +
				"def ait := jlobby.edu.vub.at.objects.symbiosis.CoercionTest.AsyncInterfaceTest;  \n" +
				"jlobby.edu.vub.at.objects.coercion.Coercer.coerce(sleeper, ait)");
		AsyncInterfaceTest ait = (AsyncInterfaceTest) sleeper;
		long then = System.currentTimeMillis();
		ait.sleepLong();
		long now = System.currentTimeMillis();
		assertTrue("sleepLong() should return immediately", now - then < 500);
	}
}
