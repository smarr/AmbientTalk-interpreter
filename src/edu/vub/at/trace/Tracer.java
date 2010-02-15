/**
 * AmbientTalk/2 Project
 * (c) Software Languages Lab, 2006 - 2009
 * Authors: Ambient Group at SOFT
 * 
 * The source code in this file is based on source code from Tyler Close's
 * Waterken server, Copyright 2008 Waterken Inc. Waterken's code is published
 * under the MIT license.
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATLetter;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.eval.InvocationStack;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.parser.SourceLocation;
import edu.vub.at.util.logging.Logging;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * An instance of this class can be used to generate trace events
 * of sent and received messages. The tracelog emitted by this tracer
 * is in the JSON format defined by the Waterken server and can be
 * inspected by post-mortem distributed debuggers such as Causeway.
 */
public class Tracer {
	
    private final JSONWriter outer;
    private final JSONWriter.ArrayWriter out;
    private final Set filteredSources;
    private final Marker mark;

    /**
     * Constructs a trace event generator.
     * @param stderr    log event output stream
     * @param mark      event counter
     */
	public Tracer(final Writer stderr, final Marker mark) throws IOException {
	    outer = JSONWriter.make(stderr);
	    out = outer.startArray();
	    filteredSources = new HashSet();
	    this.mark = mark;
	}
        
    /**
     * Closes the log.
     */
	public void close() {
    	try {
			out.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to close Causeway log", e);
		}
    }
    
    /**
     * Filters out all stack traces from the given source file.
     */
    public void filter(String sourceFile) {
    	filteredSources.add(sourceFile);
    }
    
    
    /**
     * Logs a comment.
     * @param text  comment text
     * 
     * { "class"      : [ "org.ref_send.log.Comment", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "text"   : text }
     */
    public void comment(final String text) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, "Comment", mark.apply());
			json.startMember("text").writeString(text);
			writeTrace(json, Tracer.traceHere(filteredSources));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
        // stderr.apply(new Comment(mark.apply(),tracer.traceHere(),text));
    }
    
    /**
     * Logs an exception.
     * @param reason    problem reason
     * 
     * { "class"      : [ "org.ref_send.log.Problem", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "text"   : text
     *   "reason" : reason }
     */
    public void problem(final InterpreterException reason) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, "Problem", mark.apply());
			json.startMember("text").writeString(Tracer.readException(reason));
			writeException(json.startMember("reason"), reason);
			writeTrace(json, Tracer.traceException(reason));
			json.finish();
    	} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        //stderr.apply(new Problem(mark.apply(),
        //                         tracer.traceException(reason),
        //                         tracer.readException(reason), reason));
    }
    
    /**
     * Logs receipt of a message.
     * @param message   message identifier
     * @param message   a letter identifying an asynchronously sent AmbientTalk message
     * 
     * { "class"      : [ "org.ref_send.log.Got", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "message" : message }
     */
    public void got(final String message, final ATLetter letter) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, "Got", mark.apply());
			json.startMember("message").writeString(message);
			Tracer.traceAsyncMessage(letter).toJSON(json.startMember("trace"));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        //if (null != concrete && null != method &&
        //        !Modifier.isStatic(method.getModifiers())){
        //    try {
        //        method = Reflection.method(concrete, method.getName(),
        //                                   method.getParameterTypes());
        //    } catch (final NoSuchMethodException e) {}
        //}
        //stderr.apply(new Got(mark.apply(),
        //    null!=method ? tracer.traceMember(method) : null, message));
    }

    
    /**
     * Logs a message send.
     * @param message   sent message identifier
     * 
     * { "class"      : [ "org.ref_send.log.Sent", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "message": message }
     */
    public void sent(final String message) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, "Sent", mark.apply());
			json.startMember("message").writeString(message);
			writeTrace(json, Tracer.traceHere(filteredSources));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        // stderr.apply(new Sent(mark.apply(),tracer.traceHere(),message));
    }

    /**
     * Logs sending of a return value.
     * @param message   return message identifier
     * 
     * { "class"  : [ "org.ref_send.log.Returned", "org.ref_send.log.Sent", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "message": message }
     */
    public void returned(final String message) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, new String[] { "Returned", "Sent" }, mark.apply());
			json.startMember("message").writeString(message);
			writeTrace(json, Tracer.traceHere(filteredSources));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        // stderr.apply(new Returned(mark.apply(), null, message));
    }

    /**
     * Logs a conditional message send.
     * @param message   message identifier
     * @param condition condition identifier
     * 
     * { "class"      : [ "org.ref_send.log.SentIf", "org.ref_send.log.Sent", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "message": message
     *   "condition" : condition }
     */
    public void sentIf(final String message, final String condition) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, new String[] { "SentIf", "Sent" }, mark.apply());
			json.startMember("condition").writeString(condition);
			json.startMember("message").writeString(message);
			writeTrace(json, Tracer.traceHere(filteredSources));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
        // stderr.apply(new SentIf(mark.apply(), tracer.traceHere(),
        //                        message, condition));
    }

    /**
     * Logs resolution of a promise.
     * @param condition condition identifier
     * 
     * { "class"      : [ "org.ref_send.log.Resolved", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "condition" : condition }
     */
    public void resolved(final String condition) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, new String[] { "Resolved" }, mark.apply());
			json.startMember("condition").writeString(condition);
			writeTrace(json, Tracer.traceHere(filteredSources));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        //stderr.apply(new Resolved(mark.apply(), tracer.traceHere(),
        //                          condition));
    }

    /**
     * Logs fulfillment of a promise.
     * @param condition condition identifier
     * @param fromLetter an optional letter from which the value was derived
     * If letter given, the last expr in the source of the method it denotes
     * is prepended to the tracelog
     * 
     * { "class"      : [ "org.ref_send.log.Fulfilled", "org.ref_send.log.Resolved", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "condition" : condition }
     */
    public void fulfilled(final String condition, final ATObject fromLetter) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, new String[] { "Fulfilled", "Resolved" }, mark.apply());
			json.startMember("condition").writeString(condition);
			writeTrace(json, (fromLetter.equals(Evaluator.getNil())) ?
				Tracer.traceHere(filteredSources) :
				Tracer.traceHereStartingWith(fromLetter.asLetter(), filteredSources));
			json.finish();
		} catch (Exception e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        // stderr.apply(new Fulfilled(mark.apply(), tracer.traceHere(),
        //                           condition));
    }
    
    /**
     * Logs rejection of a promise.
     * @param condition condition identifier
     * @param fromLetter an optional letter from which the value was derived
     * 
     * If letter given, the last expr in the source of the method it denotes
     * is prepended to the tracelog
     * 
     * { "class"      : [ "org.ref_send.log.Rejected", "org.ref_send.log.Resolved", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "condition" : condition
     *   "reason" : reason }
     */
    public void rejected(final String condition, final InterpreterException reason, final ATObject fromLetter) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, new String[] { "Rejected", "Resolved" }, mark.apply());
			json.startMember("condition").writeString(condition);
			writeException(json.startMember("reason"), reason);
			writeTrace(json, (fromLetter.equals(Evaluator.getNil())) ?
					Tracer.traceHere(filteredSources) :
					Tracer.traceHereStartingWith(fromLetter.asLetter(), filteredSources));
			json.finish();
		} catch (Exception e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        // stderr.apply(new Rejected(mark.apply(), tracer.traceHere(),
        //                          condition, reason));
    }
    
    /**
     * Logs progress towards fulfillment of a promise.
     * @param condition condition identifier
     * 
     * { "class"      : [ "org.ref_send.log.Progressed", "org.ref_send.log.Resolved", "org.ref_send.log.Event" ]
     *   "anchor" : ...
     *   "trace"  : ...
     *   "condition" : condition }
     */
    public void progressed(final String condition) {
    	try {
			JSONWriter.ObjectWriter json = out.startElement().startObject();
			writeClassAndAnchor(json, new String[] { "Progressed", "Resolved" }, mark.apply());
			json.startMember("condition").writeString(condition);
			writeTrace(json, Tracer.traceHere(filteredSources));
			json.finish();
		} catch (IOException e) {
			Logging.EventLoop_LOG.warn("Unable to log Causeway event", e);
		}
    	
        // stderr.apply(new Progressed(mark.apply(), tracer.traceHere(),
        //                            condition));
    }
    
    /**
     * Gets the text message from an AmbientTalk exception.
     * @param e exception to extract message from
     */
    protected static String readException(final InterpreterException e) {
    	return e.getMessage();
    }  
       
    /**
     * Gets the stack trace for a given AmbientTalk exception.
     * @param e exception to trace
     */
    protected static Trace traceException(final InterpreterException e) {
    	return e.getAmbientTalkStackTrace().generateTrace(new HashSet());
    }
    
    /**
     * Produces a trace consisting of only the given asynchronous message
     */
    protected static Trace traceAsyncMessage(final ATLetter letter) {
    	
		String name = "unprintable message";
		ATClosure slot = null;
		SourceLocation loc = null;
		try {
			ATAsyncMessage msg = letter.base_message();
			name = msg.base_selector()+Evaluator.printAsList(msg.base_arguments()).javaValue;
			ATObject rcvr = letter.base_receiver();
			loc = rcvr.impl_getSourceOf(msg.base_selector());
			
			//slot = rcvr.meta_select(rcvr, msg.base_selector());
			//SourceLocation loc = (slot == null) ? null : slot.impl_getLocation();
		} catch (InterpreterException e) {}
		
		String source = null;
		int[][] span = null;
		if (loc != null) {
          source = loc.fileName;
		  span = new int[][] { new int[] { loc.line, loc.column } };
		}
		
    	return new Trace(new CallSite[] {
        		new CallSite(name,
        				     source,
        				     span) } );
    }
    
    /**
     * Gets the current stack trace.
     */
    protected static Trace traceHere(java.util.Set sourceFilter) {
    	return InvocationStack.captureInvocationStack().generateTrace(sourceFilter);
    }

    
    protected static Trace traceHereStartingWith(ATLetter letter, java.util.Set sourceFilter) {
		String name = "unprintable message";
		ATClosure slot = null;
		SourceLocation loc = null;
		try {
			ATAsyncMessage msg = letter.base_message();
			name = msg.base_selector()+Evaluator.printAsList(msg.base_arguments()).javaValue;
			ATObject rcvr = letter.base_receiver();
			slot = rcvr.meta_select(rcvr, letter.base_message().base_selector());
			ATBegin expr = slot.base_method().base_bodyExpression();
			if (expr.impl_getLocation() != null) {
				ATTable stmts = expr.base_statements();
				ATObject lastStatement = stmts.base_at(stmts.base_length());
				loc = lastStatement.impl_getLocation();
			}			
		} catch (InterpreterException e) {}
		
		String source = null;
		int[][] span = null;
		if (loc != null) {
          source = loc.fileName;
		  span = new int[][] { new int[] { loc.line, loc.column } };
		}
		
		Trace t = InvocationStack.captureInvocationStack().generateTrace(sourceFilter);
		// now prepend the callsite identified by loc to the runtime stack
		CallSite[] stack = t.calls;
		CallSite[] stackPlusLetter = new CallSite[stack.length+1];
		stackPlusLetter[0] = new CallSite(name, source, span);
		System.arraycopy(stack, 0, stackPlusLetter, 1, stack.length);
    	return new Trace(stackPlusLetter);	
    }

    private static void writeClassAndAnchor(JSONWriter.ObjectWriter json, String className, Anchor anchor) throws IOException {
    	writeClassAndAnchor(json, new String[] { className }, anchor);
    }
    
    private static void writeClassAndAnchor(JSONWriter.ObjectWriter json, String[] classNames, Anchor anchor) throws IOException {
    	JSONWriter.ArrayWriter classes = json.startMember("class").startArray();
    	for (int i = 0; i < classNames.length; i++) {
        	classes.startElement().writeString("org.ref_send.log."+classNames[i]);
		}
    	classes.startElement().writeString("org.ref_send.log.Event");
    	classes.finish();
    	anchor.toJSON(json.startMember("anchor"));
    }
    
    /**
     * "anchor" : {
     *   "number" : n,
     *   "turn" : {
     *     "loop" : "event-loop-name",
     *     "number" : n
     *   }
     * }
     * 
     * "trace" : {
     *   "calls" : [ {
     *      "name" : "foo()",
     *      "source" : "foo.at",
     *      "span" : [ [ lineNo ] ]
     *    } ]
     * }
     */
    private static void writeTrace(JSONWriter.ObjectWriter json, Trace trace) throws IOException {
    	trace.toJSON(json.startMember("trace"));
    }
        
    /**
     * { "type" : "Exception"
     *   "message" : "message"
     *   "cause" : ... }
     */
    private static void writeException(JSONWriter json, Throwable t) throws IOException {
    	JSONWriter.ObjectWriter exc = json.startObject();
    	exc.startMember("type").writeString(t.getClass().getSimpleName());
    	exc.startMember("message").writeString(t.getMessage());
    	if (t.getCause() != null) {
        	writeException(exc.startMember("cause"), t.getCause());            		
    	};
    	exc.finish();
    }
}
