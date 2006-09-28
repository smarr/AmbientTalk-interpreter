package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.eval.Evaluator;

/**
 * @author tvc
 *
 * The unit test LexicalRootTest tests globally visible methods in the lexical root.
 */
public class LexicalRootTest extends AmbientTalkTest {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(LexicalRootTest.class);
	}
	
	//private OBJLexicalRoot root_ = (OBJLexicalRoot) OBJLexicalRoot.getGlobalLexicalScope().lexicalParent_;
	
	public void testLexicalRootFields() {
		evalAndCompareTo("nil", NATNil._INSTANCE_);
		evalAndCompareTo("true", NATBoolean._TRUE_);
		evalAndCompareTo("false", NATBoolean._FALSE_);
		evalAndCompareTo("/", Evaluator.getLobbyNamespace());
	}

}
