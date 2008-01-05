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
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATExpression;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ListIterator;
import java.util.Stack;

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
		public final ATExpression invocation;
		public final ATObject receiver;
		public final ATTable arguments;
		
		public InvocationFrame(ATExpression inv, ATObject rcvr, ATTable args) {
			invocation = inv;
			receiver = rcvr;
			arguments = args;
		}
	}
	
	private final Stack invocationStack_;
	
	private InvocationStack() {
		invocationStack_ = new Stack();
	}
	
	private InvocationStack(Stack initstack) {
		invocationStack_ = initstack;
	}
	
	public void methodInvoked(ATExpression methodInvocation, ATObject receiver, ATTable args) throws InterpreterException {
		invocationStack_.push(new InvocationFrame(methodInvocation, receiver, args));
	}
	
	public void functionCalled(ATExpression funCall, ATClosure fun, ATTable evaluatedArgs) {
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
				s.println("at "+Evaluator.toString(frame.invocation));
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
				s.println("at "+Evaluator.toString(frame.invocation));
			}
		}
	}
	
	public Object clone() {
		return new InvocationStack((Stack) invocationStack_.clone());
	}
	
}
