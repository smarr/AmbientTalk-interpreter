package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.mirrors.NativeClosure;

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
			printedEquals(exploded, "[\"a\", \"m\", \"b\", \"i\", \"e\", \"n\", \"t\", \"t\", \"a\", \"l\", \"k\"]");
			
			// "one, two, three".split(", ") => [ "one", "two", "three" ]
			printedEquals(TXTcommas_.base_split(NATText.atValue(", ")), "[\"one\", \"two\", \"three\"]");
			
			// "ambienttalk".find: "[aeiou]" do: { |vowel| buff << vowel; nil } => buff = "aiea"
			final StringBuffer buff = new StringBuffer();
			TXTambienttalk_.base_find_do_(NATText.atValue("[aeiou]"), new NativeClosure(null) {
				public ATObject base_apply(ATTable arguments) throws InterpreterException {
					buff.append(arguments.base_at(NATNumber.ONE).asNativeText().javaValue);
					return NATNil._INSTANCE_;
				}
			});
			assertEquals(buff.toString(), "aiea");
			
			// "ambienttalk".replace: "[aeiou]" by: { |vowel| vowel.toUpperCase() } => AmbIEnttAlk
			ATText replaced = TXTambienttalk_.base_replace_by_(NATText.atValue("[aeiou]"), new NativeClosure(null) {
				public ATObject base_apply(ATTable arguments) throws InterpreterException {
					return arguments.base_at(NATNumber.ONE).asNativeText().base_toUpperCase();
				}
			});
			printedEquals(replaced, "\"AmbIEnttAlk\"");
			
			// "A".toLowerCase() => "a"
			printedEquals(NATText.atValue("A").base_toLowerCase(), "\"a\"");
			
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
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testNumericPrimitives() {
		try {
			// 5.cos().inc()
			assertEquals(NATFraction.atValue(Math.cos(5)+1), NATNumber.atValue(5).base_cos().base_inc());
			
			// 2.expt(3).round()
			assertEquals(8, NATNumber.atValue(2).base_expt(NATNumber.atValue(3)).base_round().asNativeNumber().javaValue);
		
		    // 1 + 2 => 3
			assertEquals(3, NATNumber.ONE.base__oppls_(NATNumber.atValue(2)).asNativeNumber().javaValue);
		    // 1.1 + 2.2 => 3.3
			assertEquals(3.3, NATFraction.atValue(1.1).base__oppls_(NATFraction.atValue(2.2)).asNativeFraction().javaValue, 0.000001);
		    // 1.0 + 2 => 3.0
			assertEquals(3.0, NATFraction.atValue(1.0).base__oppls_(NATNumber.atValue(2)).asNativeFraction().javaValue, 0.000001);
		    // 1 + 2.0 => 3.0
			assertEquals(3.0, NATNumber.ONE.base__oppls_(NATFraction.atValue(2.0)).asNativeFraction().javaValue, 0.000001);

		    // 1 - 2 => -1
			assertEquals(-1, NATNumber.ONE.base__opmns_(NATNumber.atValue(2)).asNativeNumber().javaValue);
		    // 1.1 - 2.2 => -1.1
			assertEquals(-1.1, NATFraction.atValue(1.1).base__opmns_(NATFraction.atValue(2.2)).asNativeFraction().javaValue, 0.000001);
		    // 1.0 - 2 => -1.0
			assertEquals(-1.0, NATFraction.atValue(1.0).base__opmns_(NATNumber.atValue(2)).asNativeFraction().javaValue, 0.000001);
		    // 1 - 2.0 => -1.0
			assertEquals(-1.0, NATNumber.ONE.base__opmns_(NATFraction.atValue(2.0)).asNativeFraction().javaValue, 0.000001);
			
		    // 1 * 2 => 2
			assertEquals(2, NATNumber.ONE.base__optms_(NATNumber.atValue(2)).asNativeNumber().javaValue);
		    // 1.1 * 2.2
			assertEquals(1.1 * 2.2, NATFraction.atValue(1.1).base__optms_(NATFraction.atValue(2.2)).asNativeFraction().javaValue, 0.000001);
		    // 1.0 * 2 => 2.0
			assertEquals(2.0, NATFraction.atValue(1.0).base__optms_(NATNumber.atValue(2)).asNativeFraction().javaValue, 0.000001);
		    // 1 * 2.0 => 2.0
			assertEquals(2.0, NATNumber.ONE.base__optms_(NATFraction.atValue(2.0)).asNativeFraction().javaValue, 0.000001);

		    // 1 / 2 => 0.5
			assertEquals(0.5, NATNumber.ONE.base__opdiv_(NATNumber.atValue(2)).asNativeFraction().javaValue, 0.0000001);
		    // 1.1 / 2.2
			assertEquals(1.1 / 2.2, NATFraction.atValue(1.1).base__opdiv_(NATFraction.atValue(2.2)).asNativeFraction().javaValue, 0.000001);
		    // 1.0 / 2 => 0.5
			assertEquals(0.5, NATFraction.atValue(1.0).base__opdiv_(NATNumber.atValue(2)).asNativeFraction().javaValue, 0.000001);
		    // 1 / 2.0 => 0.5
			assertEquals(0.5, NATNumber.ONE.base__opdiv_(NATFraction.atValue(2.0)).asNativeFraction().javaValue, 0.000001);
			
			// 1 < 2
			assertTrue(NATNumber.ONE.base__opltx_(NATNumber.atValue(2)).asNativeBoolean().javaValue);
			// 2.5 > 2
			assertTrue(NATFraction.atValue(2.5).base__opgtx_(NATNumber.atValue(2)).asNativeBoolean().javaValue);
             // 2.5 <= 2.5
			assertTrue(NATFraction.atValue(2.5).base__opltx__opeql_(NATFraction.atValue(2.5)).asNativeBoolean().javaValue);
             // 1 >= 1
			assertTrue(NATNumber.ONE.base__opgtx__opeql_(NATNumber.ONE).asNativeBoolean().javaValue);
			// 1.0 = 1
			assertTrue(NATFraction.atValue(1.0).base__opeql_(NATNumber.ONE).asNativeBoolean().javaValue);
			// 1 = 1.0
			assertTrue(NATNumber.ONE.base__opeql_(NATFraction.atValue(1.0)).asNativeBoolean().javaValue);
			// 1.1 != 1.0
			assertTrue(NATFraction.atValue(1.1).base__opnot__opeql_(NATFraction.atValue(1.0)).asNativeBoolean().javaValue);
             // ! 1.0 == 1
			assertFalse(NATFraction.atValue(1.0).base__opeql__opeql_(NATNumber.ONE).asNativeBoolean().javaValue);
			
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testNumberPrimitives() {
		try {
			// 1.inc() => 2
			assertEquals(2, NATNumber.ONE.base_inc().asNativeNumber().javaValue);
			
			// -1.abs() => 1
			assertEquals(1, NATNumber.MONE.base_abs().asNativeNumber().javaValue);
			
			// 3.doTimes: { |i| buff << i; nil } => buff = 123
			final StringBuffer buff = new StringBuffer();
			NATNumber.atValue(3).base_doTimes_(new NativeClosure(null) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					buff.append(getNbr(args, 1));
					return NATNil._INSTANCE_;
				}
			});
			assertEquals("123", buff.toString());
			
			// 3.to: 5 do: { |i| buff2 << i; nil } => buff2 = 34
			final StringBuffer buff2 = new StringBuffer();
			NATNumber.atValue(3).base_to_do_(NATNumber.atValue(5), new NativeClosure(null) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					buff2.append(getNbr(args, 1));
					return NATNil._INSTANCE_;
				}
			});
			assertEquals("34", buff2.toString());
			
			// 50.to: 0 step: 10 do: { |i| buff3 << i; nil } => buff3 = 5040302010
			final StringBuffer buff3 = new StringBuffer();
			NATNumber.atValue(50).base_to_step_do_(NATNumber.atValue(0), NATNumber.atValue(10), new NativeClosure(null) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					buff3.append(getNbr(args, 1));
					return NATNil._INSTANCE_;
				}
			});
			assertEquals("5040302010", buff3.toString());
			
			// 1 ** 4 => [1, 2, 3]
			printedEquals(NATNumber.ONE.base__optms__optms_(NATNumber.atValue(4)), "[1, 2, 3]");
			// 1 *** 4 => [1, 2, 3, 4]
			printedEquals(NATNumber.ONE.base__optms__optms__optms_(NATNumber.atValue(4)), "[1, 2, 3, 4]");
			// 4 ** 1 => [4, 3, 2]
			printedEquals(NATNumber.atValue(4).base__optms__optms_(NATNumber.ONE), "[4, 3, 2]");
			// 4 *** 1 => [4, 3, 2, 1]
			printedEquals(NATNumber.atValue(4).base__optms__optms__optms_(NATNumber.ONE), "[4, 3, 2, 1]");
			// -1 ** -1 => []
			printedEquals(NATNumber.MONE.base__optms__optms_(NATNumber.MONE), "[]");
			// -1 *** -1 => [-1]
			printedEquals(NATNumber.MONE.base__optms__optms__optms_(NATNumber.MONE), "[-1]");
			
			// 1 ?? 5 => [1, 5[
			double rand = NATNumber.ONE.base__opque__opque_(NATNumber.atValue(5)).asNativeFraction().javaValue;
			assertTrue((1 <= rand) && (rand < 5));
			
			// 8 % 3 => 2
			assertEquals(2, NATNumber.atValue(8).base__oprem_(NATNumber.atValue(3)).asNativeNumber().javaValue);
			
			// 9 /- 2 => 4
			assertEquals(4, NATNumber.atValue(9).base__opdiv__opmns_(NATNumber.atValue(2)).asNativeNumber().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}

	public void testFractionPrimitives() {
		try {
			// 1.4.round() => 1
			assertEquals(1, NATFraction.atValue(1.4).base_round().asNativeNumber().javaValue);
			// 1.8.round() => 2
			assertEquals(2, NATFraction.atValue(1.8).base_round().asNativeNumber().javaValue);
			// 1.5.round() => 2
			assertEquals(2, NATFraction.atValue(1.5).base_round().asNativeNumber().javaValue);
			
			// 1.8.floor() => 1
			assertEquals(1, NATFraction.atValue(1.8).base_floor().asNativeNumber().javaValue);
			// 1.4.ceiling() => 2
			assertEquals(2, NATFraction.atValue(1.4).base_ceiling().asNativeNumber().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testClosurePrimitives() {
		try {
			// whileTrue
			ATObject result = evalAndReturn("def i := 0; { i < 5 }.whileTrue: { i := i + 1 }; i");
			assertEquals(5, result.asNativeNumber().javaValue);
		} catch (XTypeMismatch e) {
			fail(e.getMessage());
		}
	}
	
	public void testTablePrimitives() {
		try {
			ATTable vowels = evalAndReturn("[\"a\", \"e\", \"i\", \"o\", \"u\"]").asTable();

			// vowels.length = 5
			assertEquals(5, vowels.base_length().asNativeNumber().javaValue);
			
			// vowels.at(1) = "a"
			assertEquals("a", vowels.base_at(NATNumber.ONE).asNativeText().javaValue);
			
			// vowels.atPut(1, "z")
			vowels.base_atPut(NATNumber.ONE, NATText.atValue("z"));
			assertEquals("z", vowels.base_at(NATNumber.ONE).asNativeText().javaValue);
			
			// vowels.isEmpty() => false
			assertFalse(vowels.base_isEmpty().asNativeBoolean().javaValue);
			
			// each: ablock
			evalAndCompareTo("def sum := 0; [1,2,3].each: { |i| sum := sum + i }; sum", "6");
			
			// map: ablock
			evalAndCompareTo("[1,2,3].map: { |i| i + 1 }", "[2, 3, 4]");
			
			// with: init collect: ablock
			evalAndCompareTo("[1,2,3].inject: 0 into: { |total, next| total + next }", "6");
			
			// filter: ablock
			evalAndCompareTo("[1,2,3].filter: {|e| e != 2 }", "[1, 3]");

			// find: ablock
			evalAndCompareTo("[`a, `b, `c].find: { |e| e == `b }", "2");
			evalAndCompareTo("[`a, `b, `c].find: { |e| e == `d }", NATNil._INSTANCE_);
			
			// vowels.implode() => "zeiou"
			assertEquals("zeiou", vowels.base_implode().asNativeText().javaValue);
			
             // vowels.join(",") => "z,e,i,o,u"
			assertEquals("z,e,i,o,u", vowels.base_join(NATText.atValue(",")).asNativeText().javaValue);
			
            // [].implode() => ""
			assertEquals("", NATTable.EMPTY.base_implode().asNativeText().javaValue);
			
            // [].join(",") => ""
			assertEquals("", NATTable.EMPTY.base_join(NATText.atValue(",")).asNativeText().javaValue);
			
			// vowels.select(2,5).implode() => "eio"
			assertEquals("eio", vowels.base_select(NATNumber.atValue(2), NATNumber.atValue(5)).base_implode().asNativeText().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void testBooleanPrimitives() {
		try {
			// (0 < 1).ifTrue: { 0 } => 0
			assertEquals(NATNumber.ZERO, NATNumber.ZERO.base__opltx_(NATNumber.ONE).base_ifTrue_(new NativeClosure(null) {
				public ATObject base_apply(ATTable args) {
					return NATNumber.ZERO;
				}
			}));
			
			// (0 < 1).ifFalse: { 0 } => nil
			assertEquals(NATNil._INSTANCE_, NATNumber.ZERO.base__opltx_(NATNumber.ONE).base_ifFalse_(new NativeClosure(null) {
				public ATObject base_apply(ATTable args) {
					return NATNumber.ZERO;
				}
			}));
			
			// false.and: { 1/0 } => false
			try {
				assertFalse(NATBoolean._FALSE_.base_and_(new NativeClosure(null) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						return NATNumber.ONE.base__opdiv_(NATNumber.ZERO);
					}
				}).asNativeBoolean().javaValue);
			} catch (XIllegalArgument e) {
				fail("short-circuit and: is broken.");
			}
			
			// true.or: { 1/0 } => true
			try {
				assertTrue(NATBoolean._TRUE_.base_or_(new NativeClosure(null) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						return NATNumber.ONE.base__opdiv_(NATNumber.ZERO);
					}
				}).asNativeBoolean().javaValue);
			} catch (XIllegalArgument e) {
				fail("short-circuit or: is broken.");
			}
			
			// false.or: { true } => true
			assertTrue(NATBoolean._FALSE_.base_or_(new NativeClosure(null) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					return NATBoolean._TRUE_;
				}
			}).asNativeBoolean().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
}
