package edu.vub.at.objects.natives;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * A unit test for the NATNamespace class.
 * 
 * @author tvc
 */
public class NATNamespaceTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATNamespaceTest.class);
	}

	/* create the following directories and files for the test:
	 *  /tmp/at/
	 *  /tmp/at/test
	 *  /tmp/at/test/file1.at, containing the following code:
	 *     def x := 1;
	 *     def y := /.at;
	 *     def z := ~.file2.x;
	 *     x
	 *  /tmp/at/test/file2.at, containing the following code:
	 *     def x := 0;
	 *     def y := /.at.test.file1;
	 *     self
	 * When loading file1.at, then file2.at, file1.z is bound to 0 and file2.y is bound to nil
	 * When loading file2.at, then file1.at, an error will occur because ~.file2.x will result in evaluating nil.x
	 */
	private File at_;
	private File at_test_;
	private File at_test_file1_at;
	private File at_test_file2_at;
	
	public void setUp() {
		try {
			boolean success;
			at_ = new File("/tmp/at");
			success = at_.mkdir();
			at_test_ = new File("/tmp/at/test");
			success &= at_test_.mkdir();
			at_test_file1_at = new File("/tmp/at/test/file1.at");
			success &= at_test_file1_at.createNewFile();
			at_test_file2_at = new File("/tmp/at/test/file2.at");
			success &= at_test_file2_at.createNewFile();
			if (!success) {
				fail("could not create test directories and files");
			}
			
			FileWriter fw = new FileWriter(at_test_file1_at);
			fw.write("def x := 1; \n def y := /.at; \n def z := ~.file2.x; \n x");
			fw.close();
			
			fw = new FileWriter(at_test_file2_at);
			fw.write("def x := 0; \n def y := /.at.test.file1; \n self");
			fw.close();
			
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	public void tearDown() {
		boolean success = at_test_file2_at.delete();
		success &= at_test_file1_at.delete();
		success &= at_test_.delete();
		success &= at_.delete();
		if (!success) {
			fail("could not delete test directories and files");
		}
	}
	
	
	public void testNamespaces() {
		try {
			NATObject lobby = Evaluator.getLobbyNamespace();
			
			// create the namespace 'at' bound to the path /tmp/at
			NATNamespace atNS = new NATNamespace("/at", at_);
			// bind the name 'at' to the atNS namespace in the lobby
			lobby.meta_defineField(AGSymbol.alloc("at"), atNS);

			// now, try to select the 'at' slot from the lobby
			ATObject at = lobby.meta_select(lobby, AGSymbol.alloc("at"));
			// the at slot should equal a namespace object
			assertTrue(at instanceof NATNamespace);
			assertEquals("<ns:/at>", at.meta_print().javaValue);
			
			ATObject test = at.meta_select(at, AGSymbol.alloc("test"));
			// the test slot should equal a namespace object
			assertTrue(test instanceof NATNamespace);
			assertEquals("<ns:/at/test>", test.meta_print().javaValue);	
			
			// select at.test.file1 which should load file1 and return 1
			ATObject result = test.meta_select(test, AGSymbol.alloc("file1"));
			assertEquals(NATNumber.ONE, result);
			
			// ensure file1 is now really bound to 1 in the namespace 'test'
			assertTrue(test.meta_respondsTo(AGSymbol.alloc("file1")).asNativeBoolean().javaValue);
			
			// normally, by loading file1, file2 should have been loaded as well:
			assertTrue(test.meta_respondsTo(AGSymbol.alloc("file2")).asNativeBoolean().javaValue);
			
			// test.file2 should be a normaly object with a ~ slot bound to test
			ATObject fileScope = test.meta_select(test, AGSymbol.alloc("file2"));
			assertEquals(test, fileScope.meta_select(fileScope, AGSymbol.alloc("~")));
			
			// test.file2.y should equal nil
			assertEquals(NATNil._INSTANCE_, fileScope.meta_select(fileScope, AGSymbol.alloc("y")));
		} catch (NATException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * This test loads file2 before file1
	 */
	public void testReverseNamespaces() {
		try {
			NATObject lobby = Evaluator.getLobbyNamespace();
			
			// create the namespace 'at' bound to the path /tmp/at
			NATNamespace atNS = new NATNamespace("/at", at_);
			// bind the name 'at' to the atNS namespace in the lobby
			lobby.meta_assignField(lobby, AGSymbol.alloc("at"), atNS);

			// select '/.at.test'
			ATObject test = atNS.meta_select(atNS, AGSymbol.alloc("test"));
			// the test slot should equal a namespace object
			assertTrue(test instanceof NATNamespace);
			assertEquals("<ns:/at/test>", test.meta_print().javaValue);	
			
			// select at.test.file2 which should load file2 and raise an error
			// because ~.file2.x in file1.at will result in evaluating nil.x
			try {
				test.meta_select(test, AGSymbol.alloc("file2"));
			} catch(XSelectorNotFound e) {
				if (e.selector_.equals(AGSymbol.alloc("x")) && e.inObject_.equals(NATNil._INSTANCE_)) {
					// ok
					System.out.println("[expected]: "+e.getMessage());
				} else
					throw e;
			}
		} catch (NATException e) {
			fail(e.getMessage());
		}
	}
	
}
