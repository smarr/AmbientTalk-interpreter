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
		assertTrue(map_.put(AGSymbol.jAlloc("x")));
		map_.put(AGSymbol.jAlloc("y"));
		map_.put(AGSymbol.jAlloc("z"));
		map_.put(AGSymbol.jAlloc("u"));
		assertTrue(map_.put(AGSymbol.jAlloc("v")));
		map_.put(AGSymbol.jAlloc("w"));
		map_.put(AGSymbol.jAlloc("a"));
		map_.put(AGSymbol.jAlloc("b"));
		assertFalse(map_.put(AGSymbol.jAlloc("x")));
	}

	public void testMap() {
		assertEquals(0, map_.get(AGSymbol.jAlloc("x")));
		assertEquals(1, map_.get(AGSymbol.jAlloc("y")));
		assertEquals(2, map_.get(AGSymbol.jAlloc("z")));
		assertEquals(3, map_.get(AGSymbol.jAlloc("u")));
		assertEquals(4, map_.get(AGSymbol.jAlloc("v")));
		assertEquals(5, map_.get(AGSymbol.jAlloc("w")));
		assertEquals(6, map_.get(AGSymbol.jAlloc("a")));
		assertEquals(7, map_.get(AGSymbol.jAlloc("b")));
		assertEquals(-1, map_.get(AGSymbol.jAlloc("c")));
	}

}
