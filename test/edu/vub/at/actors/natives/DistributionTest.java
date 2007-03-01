package edu.vub.at.actors.natives;

import com.sun.tools.corba.se.idl.constExpr.EvaluationException;

import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.net.ConnectionListener;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATObjectClosureTest;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.NATParser;
import junit.framework.TestCase;

public class DistributionTest extends TestCase {

	private ELVirtualMachine virtual1_;
	private ELVirtualMachine virtual2_;
	
	
	private static final int _TIMEOUT_ = 10000;
	
	private boolean testResult_ = false;
	
	// used to avoid 'final' restriction for nested classes
	protected synchronized void setTestResult(boolean value) {
		testResult_ = value;
		
		// typically, we wait for a given timeout but can resume earlier when this event takes place
		this.notify();
	}
	
	// used to avoid 'final' restriction for nested classes
	protected boolean getTestResult() {
		return testResult_;
	}
	
	/**
	 * When a virtual machine joins the AmbientTalk JGroups channel, all virtual machines are 
	 * notified of this fact by the underlying distribution layer. Messages are sent to the 
	 * DiscoveryManager to notify it of a new vm which may host required services and to all
	 * (far reference) listeners waiting for the appearance of that VM.
	 * 
	 * This test registers a dedicated listener to test the dispatching of connected and 
	 * disconnected messages to such listeners when appropriate.
	 */
	public synchronized void testVirtualMachineDiscovery() {
		
		// We prevent any race conditions between the going online and offline, forcing both
		// handlers to be called. Therefore the test fails unless the disconnected handler
		// determines it was successful
		setTestResult(false);
		
		virtual1_.membershipNotifier_.addConnectionListener(
				virtual2_.getGUID(),
				new ConnectionListener() {
					public void connected() {
						setTestResult(true);
					}
					public void disconnected() { }
				});
		
		virtual1_.event_goOnline();
		virtual2_.event_goOnline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		if(! getTestResult())
			fail("Discovery notification of the VM has failed to arrive within " + _TIMEOUT_ /1000 + " sec.");
	}
	
	/**
	 * When a virtual machine leaves the AmbientTalk JGroups channel, all virtual machines are 
	 * notified of this fact by the underlying distribution layer. Messages are sent to the 
	 * DiscoveryManager and to all (far reference) listeners connected to that VM.
	 * 
	 * This test registers a dedicated listener to test the dispatching of disconnected 
	 * messages to such listeners when appropriate.
	 */
	public synchronized void testVirtualMachineDisconnection() {
		
		setTestResult(false);
		
		virtual2_.membershipNotifier_.addConnectionListener(
				virtual1_.getGUID(),
				new ConnectionListener() {
					public void connected() { }
					public void disconnected() { 
						setTestResult(true);
					}
				});
		
		virtual1_.event_goOnline();
		virtual2_.event_goOnline();

		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		
		virtual1_.event_goOffline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		if(! getTestResult())
			fail("Disconnection notification of the VM has failed to arrive within " + _TIMEOUT_ /1000 + " sec.");
	}
	
	/**
	 * This test registers a dedicated listener to test the dispatching of disconnected 
	 * messages. When the time lapse betwen connection and disconnection is too small,
	 * the test may be subject to race conditions, hence we provide a version where
	 * no wait is performed, to provoke them.
	 */
	public synchronized void testVirtualMachineDisconnectionRace() {
		
		// If the race occurs and neither the connected, nor the disconnected event listener
		// are triggered, the test should succeed, unless exceptions were raised.
		setTestResult(true);
		
		virtual2_.membershipNotifier_.addConnectionListener(
				virtual1_.getGUID(),
				new ConnectionListener() {
					public void connected() { 
						setTestResult(false);
					}
					public void disconnected() { 
						setTestResult(true);
					}
				});
		
		virtual1_.event_goOnline();
		virtual2_.event_goOnline();
		
		virtual1_.event_goOffline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		if(! getTestResult())
			fail("Disconnection notification of the VM has failed to arrive within " + _TIMEOUT_ /1000 + " sec.");
	}
	
	
	private void installSuccessTrigger(ELActor processor) throws Exception {
		processor.sync_event_performTest(new Callable() {
			public Object call(Object argument)throws InterpreterException {
				return Evaluator.getGlobalLexicalScope().meta_defineField(
						AGSymbol.jAlloc("success"),
						new NativeClosure(NATNil._INSTANCE_) {

							public ATObject base_apply(ATTable arguments) throws InterpreterException {
								setTestResult(true);
								
								return NATNil._INSTANCE_;
							}
						});
			}
		});
	}
	
	public synchronized void testServiceDiscovery() throws Exception {
		
		setTestResult(false);
		
		ELActor subscriber = initializeActor(virtual1_);
		ELActor provider   = initializeActor(virtual2_);
		
		// We define a closure to inform us the test succeeded
		installSuccessTrigger(subscriber);
		
		subscriber.sync_event_eval(
				NATParser.parse(
						"DistributionTest#testServiceDiscovery()",
						"defstripe Service; \n" +
						"when: Service discovered: { | ref | success() }"));
		
		provider.sync_event_eval(
				NATParser.parse(
						"DistributionTest#testServiceDiscovery()",
						"defstripe Service; \n" +
						"export: (object: { nil }) as: Service"));
		
		virtual1_.event_goOnline();
		virtual2_.event_goOnline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		if(! getTestResult())
			fail("Service Discovery notification has failed to arrive within " + _TIMEOUT_ /1000 + " sec.");
	}
	
	public synchronized void testConnectionObservers() throws Exception {
		
		setTestResult(false);
		
		ELActor subscriber = initializeActor(virtual1_);
		ELActor provider   = initializeActor(virtual2_);
		
		// We define a closure to inform us the test succeeded
		installSuccessTrigger(subscriber);
		
		subscriber.sync_event_eval(
				NATParser.parse(
						"DistributionTest#testConnectionObservers()",
						"defstripe Service; \n" +
						"when: Service discovered: { | ref |" +
						// explicitly triggering success, although we are not testing service discovery
						// allows to minimize the waiting time until we can go offline
						"  success(); " +
						"  when: ref disconnected: { success(); }; \n" +
						"  when: ref reconnected:  { success(); }; \n" +
						"} \n;"));
		
		provider.sync_event_eval(
				NATParser.parse(
						"DistributionTest#testConnectionObservers()",
						"defstripe Service; \n" +
						"export: (object: { nil }) as: Service"));
		
		virtual1_.event_goOnline();
		virtual2_.event_goOnline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		// reset the test condition
		setTestResult(false);
		
		virtual1_.event_goOffline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		if(! getTestResult())
			fail("Disconnection observer has failed to trigger within " + 2 * _TIMEOUT_ /1000 + " sec.");

		// reset the test condition
		setTestResult(false);

		virtual1_.event_goOnline();
		
		try {
			this.wait( _TIMEOUT_ );
		} catch (InterruptedException e) {};
		
		if(! getTestResult())
			fail("Reconnection observer has failed to trigger within " + 2 * _TIMEOUT_ /1000 + " sec.");
	
	}
	
	/*
	 * Creates an ELActor, hosted on a newly created VM.
	 */
	private ELActor initializeActor(ELVirtualMachine host) throws InterpreterException {
		return NATActorMirror.atValue(host,
				new Packet("behaviour", NATObject.createIsolate()),
				new NATActorMirror(host)
		  ).getFarHost();
	}
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NATObjectClosureTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();

		virtual1_ = new ELVirtualMachine(NATNil._INSTANCE_, new SharedActorField[] { });
		virtual2_ = new ELVirtualMachine(NATNil._INSTANCE_, new SharedActorField[] { });
	}
	
	protected void tearDown() throws Exception {
		if(virtual1_ != null) {
			virtual1_.event_goOffline();
			virtual1_.stopProcessing();
		}
		
		if(virtual2_ != null) {
			virtual2_.event_goOffline();
			virtual2_.stopProcessing();
		}
	}
	public void notestSimple() {
		try {
			ELActor alice = initializeActor(virtual1_);
			ELActor bob = initializeActor(virtual2_);
			
			alice.sync_event_eval(
					NATParser.parse(
							"CrossVMCommunicationTest#testSimple()", 
							"defstripe HelloWorld; \n" +
							"whenever: HelloWorld discovered: { | ref | \n" +
							"  ref <- hello(\"alice\"); \n" +
							"}; \n"));
			
			bob.sync_event_eval(
					NATParser.parse(
							"CrossVMCommunicationTest#testSimple()", 
							"defstripe HelloWorld; \n" +
							"def english := object: { \n" +
							"  def hello( name ) { \"hello \" + name }; \n" +
							"}; \n" +
							"def spanish := object: { \n" +
							"  def hello( name ) { \"hola \" + name }; \n" +
							"}; \n" +
							"export: english as: HelloWorld; \n" +
							"export: spanish as: HelloWorld; \n"));
			
			alice.host_.event_goOnline();
			bob.host_.event_goOnline();
			
			synchronized (this) {
				try {
					this.wait(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch (InterpreterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
