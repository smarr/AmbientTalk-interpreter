package edu.vub.at;

import edu.vub.at.actors.eventloops.Callable;
import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.natives.NATActorMirror;
import edu.vub.at.actors.natives.Packet;
import edu.vub.at.actors.natives.SharedActorField;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATIsolate;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.parser.NATParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

public abstract class AmbientTalkTest extends TestCase {

	protected final ATContext ctx_;
	
	private ELActor evalActor_ = null;
	
	protected ELActor evalActor() {
		if (evalActor_ == null) {
			try {
				ELVirtualMachine host = new ELVirtualMachine(NATNil._INSTANCE_, new SharedActorField[] { });
				evalActor_ = NATActorMirror.atValue(host,
						new Packet("behaviour",new NATIsolate()),
						new NATActorMirror(host)
				  ).getFarHost();
			} catch (InterpreterException e) {
				e.printStackTrace();
				fail(e.getMessage());
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return evalActor_;
	}
	
	public AmbientTalkTest() {
		ATObject supr = new NATObject();
		ATObject self =
			new NATObject(supr, NATObject._SHARES_A_); // self has root as lex parent and supr as dyn parent
		ATObject scope = new NATObject(self); // scope has no dyn parent and is nested within self
		ctx_ = new NATContext(scope, self);
	}

	/**
	 * Loads and evaluates the content of a code snippet file and returns the resulting AmbientTalk ATObject.
	 * Given a class Foo and the name "snippet", the code file consulted is the file named "Foo-snippet" which
	 * should be located in the same directory as the Foo.class file.
	 */
	public static final ATObject evalSnippet(Class forTestClass, String name, ATContext inContext) throws InterpreterException {
		try {
			File inFile = new File(new URI(forTestClass.getResource(Evaluator.getSimpleName(forTestClass) + "-" + name).toString()));
            // load the code from the file
			String code = Evaluator.loadContentOfFile(inFile);
		    
		    // parse and evaluate the code in the proper context and return its result
			ATAbstractGrammar source = NATParser.parse(inFile.getName(), code);
			return source.meta_eval(inContext);
		} catch (IOException e) {
			throw new XIOProblem(e);
		} catch (URISyntaxException e) {
			fail(e.getMessage());
			return NATNil._INSTANCE_;
		}
	}
	
	public ATObject evalAndReturn(String input) {
        try {
			ATAbstractGrammar ptree = 
				NATParser._INSTANCE_.base_parse(NATText.atValue(input));
			return ptree.meta_eval(ctx_);
		} catch (XParseError e) {
			fail("Parse error: "+e.getMessage());
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("Eval error: "+e.getMessage());
		}
		return null;
	}
	
	public void evalAndTestException(String input, Class interpreterExceptionClass) {
        try {
			ATAbstractGrammar ptree = 
				NATParser._INSTANCE_.base_parse(NATText.atValue(input));
			ptree.meta_eval(ctx_);
			fail("Expected an exception of type " + Evaluator.getSimpleName(interpreterExceptionClass)); // test should throw an exception
		} catch (InterpreterException ex) {
			if (!interpreterExceptionClass.isInstance(ex)) {
				ex.printStackTrace();
				fail("Unexpected exception: "+ex.getMessage());
			}
		}
	}
	
	public ATObject evalInActor(String input) {
        try {
			ATAbstractGrammar ptree = 
				NATParser._INSTANCE_.base_parse(NATText.atValue(input));
			return evalActor().sync_event_eval(ptree);
		} catch (XParseError e) {
			fail("Parse error: "+e.getMessage());
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("Eval error: "+e.getMessage());
		}
		return null;
	}
	
	public static abstract class ActorTest implements Callable {
		public Object call(Object arg) throws Exception {
			test();
			return null;
		}
		public abstract void test() throws Exception;
	}
	
	public void actorTest(ActorTest test) throws Exception {
        try {
			evalActor().sync_event_performTest(test);
		} catch (XParseError e) {
			fail("Parse error: "+e.getMessage());
		} catch (InterpreterException e) {
			e.printStackTrace();
			fail("Eval error: "+e.getMessage());
		}
	}
	
	public void evalAndCompareTo(String input, ATObject output) {
		ATObject result = evalAndReturn(input);
		if (result != null) {
			assertEquals(output, result);
		}
	}
	
	public void evalAndCompareTo(String input, String output) {
		try {
			ATObject result = evalAndReturn(input);
			if (result != null) {
				assertEquals(output, result.meta_print().javaValue);
			}
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
	public void printedEquals(ATObject input, String expected) {
		try {
			assertEquals(expected, input.meta_print().javaValue);
		} catch (InterpreterException e) {
			fail(e.getMessage());
		}
	}
	
}
