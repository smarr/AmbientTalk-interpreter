/**
 * AmbientTalk/2 Project
 * Actorscript.java created on 2-mrt-2007 at 11:14:27
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
package edu.vub.at.actors.natives;

import edu.vub.at.AmbientTalkTest;
import edu.vub.at.objects.ATObject;

/**
 * The Actorscript tests several aspects of actor creation, most notably actor creation,
 * message sending and protocol installation.
 *
 * @author tvcutsem
 */
public class ActorTest extends AmbientTalkTest {

	public void setUp() throws Exception {
		
	}

	/**
	 * Tests whether an actor can be correctly created and whether it contains the appropriate
	 * initialized behaviour.
	 */
	public void testActorCreation() throws Exception {
		actorTest(new Actorscript() {
			public void test() throws Exception {
				// define a field x in the scope of the creating actor
				evalAndReturn("def x := 1");
				// define a new actor
				ATObject behaviour = evalAndReturn("actor: { |x| def m() { x } }");
				
				assertEquals(NATLocalFarRef.class, behaviour.getClass());
				
				// TODO: does not work because async messages return no value
				//assertEquals(NATNumber.ONE, evalAndReturn("<-m()").base_asAsyncMessage().base_sendTo(behaviour, NATNil._INSTANCE_));
			}
		});
	}
	
	public void testActorMessageReception() throws Exception {

	}
	
	public void testProtocolInstallation() throws Exception {

	}
	
}
