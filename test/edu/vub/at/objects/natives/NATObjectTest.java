package edu.vub.at.objects.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.natives.grammar.AGSymbol;

public class NATObjectTest extends AmbientTalkTest {
	
	private class TestException extends RuntimeException {

		private static final long serialVersionUID = 7666632653525022022L;

		public int code;
		
		public TestException(String message, int code) {
			super(message);
			this.code = code;
		}
		
	}
	
	private NATObject original;
	private NATObject extension;
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATObjectTest.class);
	}
	
	public void setUp() throws Exception {
		original = new NATObject();
		
		original.meta_addMethod(
				new NATMethod(AGSymbol.alloc("defaultMethod"), NATTable.EMPTY, null) {
					public ATObject base_apply(ATTable arguments, ATContext ctx) throws NATException {
						throw new TestException("Application of this method is expected to fail", 0);
					}
				});
		
		extension = new NATObject(original.dynamicParent_, original.lexicalParent_, NATObject._IS_A_);
	}
	
	public void testisCloneOf() throws Exception {
		
		ATObject clone = original.meta_clone();
		
		original.meta_isCloneOf(clone).base_ifFalse_(
				new JavaClosure(clone) {
					public ATObject base_apply(ATTable arguments) throws NATException {
						fail("Cloning is not properly defined under the isCloneOf test.");
						return NATNil._INSTANCE_;
					}					
				});
			
		clone.meta_addMethod(
				new NATMethod(AGSymbol.alloc("addedMethod"), NATTable.EMPTY, null) {
					public ATObject base_apply(ATTable arguments, ATContext ctx) throws NATException {
						throw new TestException("This method needs to be visible in the clone", 1);
					}
				});
		
		try {
			clone.meta_invoke(clone, AGSymbol.alloc("addedMethod"), NATTable.EMPTY);
		} catch (TestException ae) { 
			// given the definition, this should happen!!!
		} catch (XSelectorNotFound se) {
			// implies the addMethod to the clone was not performed correctly
			fail("performing meta_addMethod did not add the method as expected");
		}
		
		original.meta_isCloneOf(clone).base_ifTrue_(
				new JavaClosure(clone) {
					public ATObject base_apply(ATTable arguments) throws NATException {
						fail("Adding fields to a clone should disrupt the isCloneOf test when comparing the original to the extended object.");
						return NATNil._INSTANCE_;
					}					
				});
		
		clone.meta_isCloneOf(original).base_ifFalse_(
				new JavaClosure(original) {
					public ATObject base_apply(ATTable arguments) throws NATException {
						fail("Adding fields to a clone should NOT disrupt the isCloneOf test when comparing the extended object to the original.");
						return NATNil._INSTANCE_;
					}					
				});
		
		extension.meta_isCloneOf(original).base_ifTrue_(
				new JavaClosure(original) {
					public ATObject base_apply(ATTable arguments) throws NATException {
						fail("Extensions should not return true to the isCloneOf test.");
						return NATNil._INSTANCE_;
					}					
				});
		
		extension.meta_isCloneOf(clone).base_ifTrue_(
				new JavaClosure(clone) {
					public ATObject base_apply(ATTable arguments) throws NATException {
						fail("Extensions should not return true to the isCloneOf test.");
						return NATNil._INSTANCE_;
					}					
				});
		
	
	}

}
