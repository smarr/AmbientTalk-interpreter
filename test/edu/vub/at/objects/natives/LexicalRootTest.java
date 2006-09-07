package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;

/**
 * @author tvc
 *
 * The unit test LexicalRootTest tests globally visible methods in the root namespace.
 */
public class LexicalRootTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(LexicalRootTest.class);
	}
	
	private OBJLexicalRoot root_ = OBJLexicalRoot._INSTANCE_;
	
	public void testLexicalRootFields() {
		evalAndCompareTo("nil", NATNil._INSTANCE_);
		evalAndCompareTo("true", NATBoolean._TRUE_);
		evalAndCompareTo("false", NATBoolean._FALSE_);
		evalAndCompareTo("/", NATNamespace._ROOT_NAMESPACE_);
	}

}
