/**
 * AmbientTalk/2 Project
 * TraceTest.java created on 22 nov 2009 at 20:30:13
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
package edu.vub.at.trace;

import edu.vub.at.actors.natives.NATActorMirror;
import edu.vub.at.actors.natives.NATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.eval.InvocationStack;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalIndex;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGMethodInvocationCreation;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.SourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import junit.framework.TestCase;

/**
 * @author tvcutsem
 *
 * Tests the tracing implementation for Causeway. 
 */
public class TraceTest extends TestCase {

	private Tracer log_;
	private StringWriter output_;
	
    private class TestMarker implements Marker {
    	private int turnCounter = 0;
        public Anchor apply() { return new Anchor(new Turn("testLoop", 0), turnCounter++); }
    }
	
	public void setUp() throws IOException {
		output_ = new StringWriter();
		log_ = new Tracer(output_, new TestMarker());
	}
	
	public void tearDown() {
		log_ = null;
		output_ = null;
	}
	
	/**
	 * Loads the content of a file and returns that content as a Java String.
	 */
	public static final String loadFile(Class forTestClass, String fileName) throws IOException {
		InputStream in = forTestClass.getResource(fileName).openStream();
		try {
			BufferedReader dis = new BufferedReader(new InputStreamReader(in));;
			StringBuffer fBuf = new StringBuffer();
			String line;

			while ( (line = dis.readLine()) != null) {
			  fBuf.append(line + "\n");
			}
		    return fBuf.toString();
		} finally {
			if (in != null) in.close();
		}
	}
	
	private final void assertOutputEquals(String fileName) throws IOException {
		log_.close();
		assertEquals(loadFile(this.getClass(), fileName).replaceAll("\\s", ""),
				     output_.toString().replaceAll("\\s", ""));
	}
	
	public void testComment() throws IOException {
		log_.comment("theComment");
		assertOutputEquals("comment.json");
	}
	
	public void testFulfilled() throws IOException {
		log_.fulfilled("theCondition", Evaluator.getNil(), Evaluator.getNil());
		assertOutputEquals("fulfilled.json");
	}
	
	public void testGot() throws IOException, InterpreterException {
		log_.got("theMessage",
				// <-name(arg)@[]
				new NATActorMirror.NATLetter(
						null,
						new NATObject(),
						new NATAsyncMessage(AGSymbol.jAlloc("selector"),
						            NATTable.of(NATNumber.ONE),
								    NATTable.EMPTY)));
		assertOutputEquals("got.json");
	}
	
	public void testProblem() throws IOException {
		log_.problem(new XIllegalOperation("theProblem", new XIllegalIndex("theCause")));
		assertOutputEquals("problem.json");
	}
	
	public void testProgressed() throws IOException {
		log_.progressed("theCondition");
		assertOutputEquals("progressed.json");
	}
	
	public void testRejected() throws IOException {
		log_.rejected("theCondition",
				new XIllegalOperation("theReason"),
				Evaluator.getNil(), Evaluator.getNil());
		assertOutputEquals("rejected.json");
	}
	
	public void testResolved() throws IOException {
		log_.resolved("theCondition");
		assertOutputEquals("resolved.json");
	}
	
	public void testReturned() throws IOException {
		log_.returned("theMessage");
		assertOutputEquals("returned.json");
	}
	
	public void testSent() throws IOException {
		log_.sent("theMessage");
		assertOutputEquals("sent.json");
	}
	
	public void testSentIf() throws IOException {
		log_.sentIf("theMessage", "theCondition");
		assertOutputEquals("sentif.json");
	}
	
	public void testStackTrace() throws IOException, InterpreterException {
		// invoked nil.name(arg := 1)
		InvocationStack.getInvocationStack().methodInvoked(
				new AGMethodInvocationCreation(
				  AGSymbol.jAlloc("name"),
	              NATTable.of(AGSymbol.jAlloc("arg")),
			      NATTable.EMPTY),
				Evaluator.getNil(),
				NATTable.of(NATNumber.ONE));
		AGMethodInvocationCreation inv = new AGMethodInvocationCreation(
				  AGSymbol.jAlloc("name"),
	              NATTable.of(AGSymbol.jAlloc("arg")),
			      NATTable.EMPTY);
		inv.impl_setLocation(new SourceLocation(42, 0, "foo.at"));
		InvocationStack.getInvocationStack().methodInvoked(
				inv,
				Evaluator.getNil(),
				NATTable.of(NATNumber.ONE));
		log_.comment("theComment");
		// clean up after ourselves (make sure InvocationStack is empty)
		InvocationStack.getInvocationStack().methodReturned(null);
		InvocationStack.getInvocationStack().methodReturned(null);
		assertOutputEquals("stacktrace.json");
	}
	
	public void testTwoComments() throws IOException {
		log_.comment("comment1");
		log_.comment("comment2");
		assertOutputEquals("comments.json");
	}
	
}
