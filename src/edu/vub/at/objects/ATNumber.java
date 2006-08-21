/**
 * AmbientTalk/2 Project
 * ATNumber.java created on 26-jul-2006 at 15:15:59
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
package edu.vub.at.objects;

import edu.vub.at.exceptions.NATException;

/**
 * @author tvc
 *
 * The public interface to an AmbientTalk native number (an integer value).
 */
public interface ATNumber extends ATNumeric {

	// base-level interface
	
	public ATNumber base_inc();
	public ATNumber base_dec();
	public ATNumber base_abs();
	
	public ATNil base_doTimes_(ATClosure code) throws NATException;
	public ATNil base_to_do_(ATNumber end, ATClosure code) throws NATException;
	public ATNil base_to_step_do_(ATNumber end, ATNumber inc, ATClosure code) throws NATException;
	
	public ATTable base__opmul__opmul_(ATNumber end) throws NATException;
	public ATTable base__opmul__opmul__opmul_(ATNumber end) throws NATException;
	
	public ATFraction base__opque__opque_(ATNumber nbr) throws NATException;
	
	public ATNumber base__oprem_(ATNumber n) throws NATException;
	public ATNumber base__opdiv__opdiv_(ATNumber n) throws NATException;
	
}
