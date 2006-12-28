package edu.vub.at.actors.natives;

import junit.framework.TestCase;
import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.events.VMEmittedEvents;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * VirtualMachineTest tests the response of a virtual machine to the various events
 * it may receive during its lifetime.
 *
 * @author smostinc
 */
public class VirtualMachineTest extends TestCase {

	private ELVirtualMachine testVM_;
	
	public void setUp() throws Exception {
		testVM_ = new ELVirtualMachine(NATText.atValue("/AmbientTalk/objects/"), NATText.atValue("/AmbientTalk/init/init.at"));
	}
	
	/**
	 * NATActorMirror creation is partially related to the virtual machine as an actor requires
	 * getObjectPathRoots() and getInitialisationCode() to return correct values. This
	 * test thus addresses the correct use of the synchronization in those methods of 
	 * ELVirtualMachine.
	 * 
	 * TODO this test needs to be rewritten to not print on success, but more importantly
	 * to not wait endlessly when something goes wrong in the initialisation.
	 */
	public void testActorCreation() throws Exception {
		final Object lock = new Object();
		
		ATActorMirror actor = new NATActorMirror(testVM_, new NativeClosure(testVM_) {
			public ATObject base_apply(ATTable arguments) throws InterpreterException {
				synchronized (lock) {
					System.out.println("NATActorMirror Initialised...");
					lock.notifyAll();
					
					return NATNil._INSTANCE_;
				}
			};
			
			public ATObject base_applyInScope(ATTable args, ATObject scope) throws InterpreterException {
				return this.base_apply(args);
			}
		});
		
		// Synchronisation over lock is necessary to force the test suite to wait for
		// the complete initialisation of the actor as the lobby and root object are 
		// only initialised during init - the last step of the process.
		synchronized (lock) {
			System.out.println(actor.base_getBehaviour());
			lock.wait();
		}
	}
	
	// TODO this test works according to the debugger (result is correctly given in
	// the call to _APPLY_), but Java fails for its lack of invocation support on
	// nested inner classes : this is presumably fixed by Tom
	// edu.vub.at.exceptions.XReflectionFailure: Native method base:apply not accessible.: Class edu.vub.at.objects.mirrors.JavaInterfaceAdaptor can not access a member of class edu.vub.at.actors.natives.VirtualMachineTest$3 with modifiers "public"
	public synchronized void notestAsyncMessageSending() throws Exception {
		final Object lock = new Object();

		ATActorMirror actor = new NATActorMirror(testVM_, new NativeClosure(testVM_) {
			public ATObject base_apply(ATTable arguments) throws InterpreterException {
				return NATNil._INSTANCE_;
			};
			
			public ATObject base_applyInScope(ATTable args, ATObject scope) throws InterpreterException {
				return this.base_apply(args);
			}
		});
		
		ATObject frontend = actor.base_getBehaviour();
		
		actor.base_upon_do_(NATActorMirror._PROCESSED_, new NativeClosure(testVM_) {
			public ATObject base_apply(ATTable arguments) throws InterpreterException {
				System.out.println(arguments.meta_print().javaValue);
				synchronized (lock) {
					System.out.println("Result computed...");
					lock.notifyAll();
					
					return NATNil._INSTANCE_;
				}
			};
			
			public ATObject base_applyInScope(ATTable args, ATObject scope) throws InterpreterException {
				return this.base_apply(args);
			}			
		});
		
		actor.base_scheduleEvent(VMEmittedEvents.acceptMessage(new NATAsyncMessage(frontend, frontend, AGSymbol.jAlloc("table"), NATTable.atValue(new ATObject[] { NATNumber.ONE }))));
		
		synchronized (lock) {
			System.out.println(actor.base_getBehaviour());
			lock.wait();
		}
	}
	
}
