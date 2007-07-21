/**
 * AmbientTalk/2 Project
 * SerializationTest.java created on 28-dec-2006 at 19:48:13
 * (c) Programming Technology Lab, 2006 - 2007
 * Authors: Tom Van Cutsem & Stijn Mostinckx
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.vub.at.actors.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGAssignField;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.objects.symbiosis.SymbioticATObjectMarker;
import edu.vub.at.parser.NATParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A test case for object serialization.
 *
 * @author tvcutsem
 */
public class SerializationTest extends AmbientTalkTest {

	private byte[] serialize(Object o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(out);
		stream.writeObject(o);
		stream.flush();
		stream.close();
		return out.toByteArray();
	}
	
	private Object deserialize(byte[] b) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		ObjectInputStream instream = new ObjectInputStream(in);
		return instream.readObject();
	}
	
	private Object copy(Object o) throws IOException, ClassNotFoundException {
		return deserialize(serialize(o));
	}
	
	public void testTXTSerialization() throws IOException, ClassNotFoundException {
		NATText boeTXT = NATText.atValue("boe");
		Object cpy = copy(boeTXT);
		System.out.println(cpy.getClass());
		NATText boeTXT2 = (NATText) cpy; 
		assertEquals(boeTXT.toString(), boeTXT2.toString());
	}
	
	public void testAGSerialization() throws IOException, ClassNotFoundException {
		NATText boeTXT = NATText.atValue("boe");
		AGSymbol fooSYM = AGSymbol.jAlloc("foo");
		AGAssignField ass = new AGAssignField(boeTXT, fooSYM, boeTXT);
		AGAssignField ass2 = (AGAssignField) copy(ass);
		assertEquals(ass.toString(), ass2.toString());
		assertTrue(ass.base_fieldName() == ass2.base_fieldName());
	}
	
	public void testParseTreeSerialization() throws InterpreterException {
		ATAbstractGrammar ag = NATParser.parse("test", "{ |x,y| x + y }");
		Packet p = new Packet("test", ag);
		ATObject o = p.unpack();
		assertEquals(ag.toString(), o.toString());
		assertFalse(ag == o);
	}
	
	/**
	 * Tests whether a coercer is correctly serialized such that upon
	 * deserialization it still holds that the deserialized value is an instance
	 * of the given Java type.
	 */
	public void testCoercerSerialization() throws InterpreterException {
		NATObject isolate = new NATObject(new ATTypeTag[] { NativeTypeTags._ISOLATE_, NativeTypeTags._TABLE_ });
		ATTable coercer = isolate.asTable();
		assertTrue(coercer instanceof SymbioticATObjectMarker);
		Packet p = new Packet("test", coercer);
		ATObject obj = p.unpack();
		assertTrue(obj instanceof ATTable);
		assertTrue(obj instanceof SymbioticATObjectMarker);
		assertFalse(obj == isolate);
	}
	
}
