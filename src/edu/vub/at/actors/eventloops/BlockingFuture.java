/**
 * AmbientTalk/2 Project
 * BlockingFuture.java created on 21-dec-2006 at 10:16:26
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
package edu.vub.at.actors.eventloops;


/**
 * A BlockingFuture represents a synchronous, blocking future used by the AT/2 implementation
 * to synchronize between native threads.
 * 
 * Usage:
 *  <code>
 *    BlockingFuture future = object.doAsynchronousComputation();
 *    try {
 *      Object result = future.get();
 *    } catch(Exception e) {
 *      // async computation raised an exception
 *    }
 *  </code>
 *
 * Based on source code written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain.
 *
 * @author tvcutsem
 */
public final class BlockingFuture implements Future {
	
    private Object result;
    
    private Exception exception;
    
    private boolean fulfilled = false;
    
    /** Returns whether the future has been either resolved with a value or ruined by an exception */
    public synchronized boolean isDetermined() {
        return fulfilled;
    }
	
	public Object get() throws Exception {
		Object result;
		synchronized(this) {
			waitFor();
			result = getResult();
		}
    	// 'blocking future' pipelining...
    	if (result instanceof BlockingFuture) {
    		return ((BlockingFuture) result).get();
    	} else {
    		return result;
    	}
	}
	
    /**
     * Sets the result of this Future to the given value unless
     * this future has already been set or has been cancelled.
     * @param v the value
     */
    public void resolve(Object v) {
        setCompleted(v);
    }

    /**
     * Causes this future to report an <tt>ExecutionException</tt>
     * with the given throwable as its cause, unless this Future has
     * already been set or has been cancelled.
     * @param e the cause of failure
     */
    public void ruin(Exception e) {
        setFailed(e);
    }
    
    /**
     * Marks the task as completed.
     * @param result the result of a task.
     */
    private void setCompleted(Object result) {
        synchronized (this) {
            if (fulfilled) return;
            fulfilled = true;
            this.result = result;
            notifyAll();
        }
    }

    /**
     * Marks the task as failed.
     * @param exception the cause of abrupt completion.
     */
    private void setFailed(Exception exception) {
        synchronized (this) {
            if (fulfilled) return;
            fulfilled = true;
            this.exception = exception;
            notifyAll();
        }
    }
	
    /**
     * Waits for the task to complete.
     * PRE: lock owned
     */
    private void waitFor() {
        while (!isDetermined()) {
            try {
				wait();
			} catch (InterruptedException e) { }
        }
    }

    /**
     * Gets the result of the task.
     *
     * PRE: task completed
     * PRE: lock owned
     */
    private Object getResult() throws Exception {
        if (exception != null) {
            throw exception;
        }
        return result;
    }
    
}
