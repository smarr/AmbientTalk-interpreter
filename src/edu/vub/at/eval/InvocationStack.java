/**
 * AmbientTalk/2 Project
 * InvocationStack.java created on 10-okt-2006 at 14:15:49
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
package edu.vub.at.eval;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.parser.SourceLocation;
import edu.vub.at.trace.CallSite;
import edu.vub.at.trace.Trace;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

/**
 * An InvocationStack instance represents the stack of method invocations and function applications
 * that are currently activated in an actor's thread. It is mainly used for debugging purposes
 * (e.g. generating stack trace information)
 * 
 * @author tvc
 */
public final class InvocationStack implements Cloneable, Serializable {

	/**
	 * A thread-local variable is used to assign a unique invocation stack to
	 * each separate actor. Each actor that invokes the getInvocationStack()
	 * method receives its own separate copy of the invocation stack
	 */
	private static final ThreadLocal _INVOCATION_STACK_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	        return new InvocationStack();
	    }
	};
	
	public static final InvocationStack getInvocationStack() {
		return (InvocationStack) _INVOCATION_STACK_.get();
	}
	
	public static final InvocationStack captureInvocationStack() {
		return (InvocationStack) getInvocationStack().clone();
	}
	
	private static class InvocationFrame implements Serializable {
		public final ATAbstractGrammar invocation;
		public final ATObject receiver;
		public final ATTable arguments;
		
		public InvocationFrame(ATAbstractGrammar inv, ATObject rcvr, ATTable args) {
			invocation = inv;
			receiver = rcvr;
			arguments = args;
		}
		
		public String toString() {
			SourceLocation loc = invocation.impl_getLocation();
			return Evaluator.toString(invocation) +
			         ((loc == null) ? "  (unknown source)" : "  (" + loc + ")");
		}
	}
	
	private final Stack invocationStack_;
	
	protected InvocationStack() {
		invocationStack_ = new Stack();
	}
	
	private InvocationStack(Stack initstack) {
		invocationStack_ = initstack;
	}
	
	public void methodInvoked(ATAbstractGrammar methodInvocation, ATObject receiver, ATTable args) throws InterpreterException {
		invocationStack_.push(new InvocationFrame(methodInvocation, receiver, args));
	}
	
	public void functionCalled(ATAbstractGrammar funCall, ATClosure fun, ATTable evaluatedArgs) {
		invocationStack_.push(new InvocationFrame(funCall, fun, evaluatedArgs));
	}
	
	/**
	 * @param result if null, the method invocation was aborted via an exception
	 */
	public void methodReturned(ATObject result) {
		invocationStack_.pop();
	}
	
	/**
	 * @param result if null, the function call was aborted via an exception
	 */
	public void funcallReturned(ATObject result) {
		invocationStack_.pop();
	}
	
	public void printStackTrace(PrintStream s) {
		if(!invocationStack_.isEmpty()) {
			s.println("origin:");
			// iterator loops from bottom to top by default
			ListIterator i = invocationStack_.listIterator();
			while (i.hasNext()) { i.next(); } // skip to last element
			while(i.hasPrevious()) { // traverse stack top to bottom
				InvocationFrame frame = (InvocationFrame) i.previous();
				s.println("at "+frame);
			}
		}
	}
	
	public void printStackTrace(PrintWriter s) {
		if(!invocationStack_.isEmpty()) {
			s.println("origin:");
			// iterator loops from bottom to top by default
			ListIterator i = invocationStack_.listIterator();
			while (i.hasNext()) { i.next(); } // skip to last element
			while(i.hasPrevious()) { // traverse stack top to bottom
				InvocationFrame frame = (InvocationFrame) i.previous();
				s.println("at "+frame);
			}
		}
	}
	
	public Object clone() {
		return new InvocationStack((Stack) invocationStack_.clone());
	}
	
	/**
	 * Generate a stack trace that can be processed by a post-mortem
	 * debugger such as Causeway.
	 */
	public Trace generateTrace(Set sourceFilter) {
		Vector callsites = new Vector(invocationStack_.size());
		// iterator loops from bottom to top by default
		ListIterator i = invocationStack_.listIterator();
		
		while (i.hasNext()) { i.next(); } // skip to last element
		Loop: while(i.hasPrevious()) { // traverse stack top to bottom
			InvocationFrame frame = (InvocationFrame) i.previous();
			SourceLocation loc = frame.invocation.impl_getLocation();
			String source = null;
			int[][] span = null;
			if (loc != null) {
			  source = loc.fileName;
			  
			  // if (sourceFilter.contains(source)) { continue Loop; }
			  for (Iterator filterIt = sourceFilter.iterator(); filterIt.hasNext();) {
				String filteredFileName = (String) filterIt.next();
				if (source.endsWith(filteredFileName)) {
					continue Loop; // skip this file
				}
			}
			  
			  span = new int[][] { new int[] { loc.line, loc.column } };
			}
			String name = frame.invocation.toString();
			callsites.add(new CallSite(name, source, span));
		}
		return new Trace((CallSite[]) callsites.toArray(new CallSite[callsites.size()]));
	}
	
}
