package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTestCase;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author tvc
 *
 * A Unit Test for the FieldMap class.
 * Tests the Field Map's behaviour for adding and looking up field names.
 */
public class TestFieldMap extends AmbientTalkTestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(TestFieldMap.class);
	}

	private FieldMap map_;

	/**
	 * Constructs the map
	 * [ x -> 0
	 *   y -> 1
	 *   z -> 2
	 *   u -> 3
	 *   v -> 4
	 *   w -> 5
	 *   a -> 6
	 *   b -> 7 ]
	 */
	protected void setUp() throws Exception {
		map_ = new FieldMap();
		assertTrue(map_.put(AGSymbol.alloc("x")));
		map_.put(AGSymbol.alloc("y"));
		map_.put(AGSymbol.alloc("z"));
		map_.put(AGSymbol.alloc("u"));
		assertTrue(map_.put(AGSymbol.alloc("v")));
		map_.put(AGSymbol.alloc("w"));
		map_.put(AGSymbol.alloc("a"));
		map_.put(AGSymbol.alloc("b"));
		assertFalse(map_.put(AGSymbol.alloc("x")));
	}

	public void testMap() {
		assertEquals(0, map_.get(AGSymbol.alloc("x")));
		assertEquals(1, map_.get(AGSymbol.alloc("y")));
		assertEquals(2, map_.get(AGSymbol.alloc("z")));
		assertEquals(3, map_.get(AGSymbol.alloc("u")));
		assertEquals(4, map_.get(AGSymbol.alloc("v")));
		assertEquals(5, map_.get(AGSymbol.alloc("w")));
		assertEquals(6, map_.get(AGSymbol.alloc("a")));
		assertEquals(7, map_.get(AGSymbol.alloc("b")));
		assertEquals(-1, map_.get(AGSymbol.alloc("c")));
	}

}
