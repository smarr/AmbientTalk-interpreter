package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.mirrors.JavaClosure;

/**
 * 
 * @author tvc
 *
 * This test case tests all the primitive base-level behaviour of native types.
 */
public class PrimitivesTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(PrimitivesTest.class);
	}
	
	private NATText TXTambienttalk_ = NATText.atValue("ambienttalk");
	private NATText TXTcommas_ = NATText.atValue("one, two, three");
	
	public void testTextPrimitives() {
		try {
			// "ambienttalk".explode() => [a, m, b, i, e, n, t, t, a, l, k]
			ATTable exploded = TXTambienttalk_.base_explode();
			printedEquals(exploded, "[a, m, b, i, e, n, t, t, a, l, k]");
			
			// "one, two, three".split(", ") => [ "one", "two", "three" ]
			printedEquals(TXTcommas_.base_split(NATText.atValue(", ")), "[one, two, three]");
			
			// "ambienttalk".find: "[aeiou]" do: { |vowel| buff << vowel; nil } => buff = "aiea"
			final StringBuffer buff = new StringBuffer();
			TXTambienttalk_.base_find_do_(NATText.atValue("[aeiou]"), new JavaClosure(null) {
				public ATObject meta_apply(ATTable arguments) throws NATException {
					buff.append(arguments.base_at(NATNumber.ONE).asNativeText().javaValue);
					return NATNil._INSTANCE_;
				}
			});
			assertEquals(buff.toString(), "aiea");
			
			// "ambienttalk".replace: "[aeiou]" by: { |vowel| vowel.toUpperCase() } => AmbIEnttAlk
			ATText replaced = TXTambienttalk_.base_replace_by_(NATText.atValue("[aeiou]"), new JavaClosure(null) {
				public ATObject meta_apply(ATTable arguments) throws NATException {
					return arguments.base_at(NATNumber.ONE).asNativeText().base_toUpperCase();
				}
			});
			printedEquals(replaced, "AmbIEnttAlk");
			
			// "A".toLowerCase() => "a"
			printedEquals(NATText.atValue("A").base_toLowerCase(), "a");
			
			// "ambienttalk".length => 11
			assertEquals(11, TXTambienttalk_.base_length().asNativeNumber().javaValue);
			
			// "ambient" + "talk" => "ambienttalk"
			assertEquals("ambienttalk", NATText.atValue("ambient").base__oppls_(NATText.atValue("talk")).asNativeText().javaValue);
			
			// "ambienttalk" <=> "ambienttalk" => 0
			assertEquals(NATNumber.ZERO, TXTambienttalk_.base__opltx__opeql__opgtx_(NATText.atValue("ambienttalk")));
			
			// "a" <=> "b" => -1
			assertEquals(NATNumber.MONE, NATText.atValue("a").base__opltx__opeql__opgtx_(NATText.atValue("b")));
			
			// "b" <=> "a" => 1
			assertEquals(NATNumber.ONE, NATText.atValue("b").base__opltx__opeql__opgtx_(NATText.atValue("a")));
			
			// "ambienttalk" ~= ".*tt.*" => true
			assertTrue(TXTambienttalk_.base__optil__opeql_(NATText.atValue(".*tt.*")).asNativeBoolean().javaValue);
			
			// "ambienttalk" ~= "java" => false
			assertFalse(TXTambienttalk_.base__optil__opeql_(NATText.atValue("java")).asNativeBoolean().javaValue);
		} catch (NATException e) {
			fail(e.getMessage());
		}
	}

}
