package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author tvc
 *
 * A unit test for the NATNamespace class.
 */
public class NATNamespaceTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATNamespaceTest.class);
	}

	/* create the following directories and files for the test:
	 *  at/
	 *  at/test
	 *  at/test/test.at, containing the following code:
	 *     def x := 1 ;
	 *     self.x
	 */
	private File at_;
	private File at_test_;
	private File at_test_testfile_at;
	
	public void setUp() {
		try {
			boolean success;
			at_ = new File("/tmp/at");
			success = at_.mkdir();
			at_test_ = new File("/tmp/at/test");
			success &= at_test_.mkdir();
			at_test_testfile_at = new File("/tmp/at/test/testfile.at");
			success &= at_test_testfile_at.createNewFile();
			if (!success) {
				fail("could not create test directories and files");
			}
			
			FileWriter fw = new FileWriter(at_test_testfile_at);
			fw.write("def x := 1; \n self.x");
			fw.close();
			
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	public void tearDown() {
		boolean success = at_test_testfile_at.delete();
		success &= at_test_.delete();
		success &= at_.delete();
		if (!success) {
			fail("could not delete test directories and files");
		}
	}
	
	
	public void testNamespaces() {
		// create a 'root' namespace pointing to /tmp
		NATNamespace root = new NATNamespace("", "/tmp", OBJDynamicRoot._INSTANCE_);
		// now, try to select the 'at' slot
		try {
			ATObject at = root.meta_select(root, AGSymbol.alloc("at"));
			// the at slot should equal a namespace object
			assertTrue(at instanceof NATNamespace);
			assertEquals("<ns:/at>", at.meta_print().javaValue);
			
			ATObject test = at.meta_select(at, AGSymbol.alloc("test"));
			// the test slot should equal a namespace object
			assertTrue(test instanceof NATNamespace);
			assertEquals("<ns:/at/test>", test.meta_print().javaValue);	
			
			// select at.test.test which should load test.at and return 1
			ATObject result = test.meta_select(test, AGSymbol.alloc("testfile"));
			assertEquals(NATNumber.ONE, result);
		} catch (NATException e) {
			fail(e.getMessage());
		}
	}
	
}
