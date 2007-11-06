/**
 * AmbientTalk/2 Project
 * Reflection.java created on 10-aug-2006 at 16:19:17
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
package edu.vub.at.objects.mirrors;

import java.lang.reflect.Method;
import java.util.Vector;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.objects.symbiosis.Symbiosis;
import edu.vub.util.Matcher;
import edu.vub.util.Pattern;

/**
 * Reflection is an auxiliary class meant to serve as a repository for methods
 * related to 'up' and 'down' Java values properly into and out of the AmbientTalk base level.
 * 
 * Keep the following mental picture in mind:
 * 
 *                ^        Java = implementation-level          |
 *  to deify      |        Java AT objects = meta-level         | to reify
 *  (= to up)     | ------------------------------------------  | (= to down)
 *  (= to absorb) |         AmbientTalk = base-level            v (= to reflect)
 * 
 * Although deification and reification are more accurate terms, we will use 'up' and 'down'
 * because they are the clearest terminology, and clarity matters.
 * 
 * In this class, the following conventions hold:
 *  - methods start with either 'up' or 'down', denoting whether they deify or reify something
 *  - arguments start with either 'j' or 'at', denoting whether they represent Java or AmbientTalk values
 *  With 'java values' is meant 'java objects representing mirrors'
 * 
 * @author tvc
 */
public final class Reflection {
	
	private static final String _BASE_PREFIX_ = "base_";
	private static final String _META_PREFIX_ = "meta_";
	
	/**
	 * A selector passed from the Java to the AmbientTalk level undergoes the following transformations:
	 * 
	 * - any pattern of the form _op{code}_ is transformed to a symbol corresponding to the operator code
	 *  Operator codes are:
	 *   pls -> +
	 *   mns -> -
	 *   tms -> *
	 *   div -> /
	 *   bsl -> \
	 *   not -> !
	 *   gtx -> >
	 *   ltx -> <
	 *   eql -> =
	 *   til -> ~
	 *   que -> ?
	 *   rem -> %
	 * - any underscores (_) are replaced by colons (:)
	 */
	public static final ATSymbol downSelector(String jSelector) {
		return AGSymbol.jAlloc(javaToAmbientTalkSelector(jSelector));
	}
	
	/**
	 * Transforms a Java selector prefixed with base_ into an AmbientTalk selector without the prefix.
	 */
	public static final ATSymbol downBaseLevelSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._BASE_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, Reflection._BASE_PREFIX_));
		} else if (jSelector.startsWith(Reflection._META_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, Reflection._META_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal base level selector to down: " + jSelector);
		}
	}
	
	/**
	 * Transforms a Java selector prefixed with meta_ into an AmbientTalk selector without the prefix.
	 */
	public static final ATSymbol downMetaLevelSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._META_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, Reflection._META_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal meta level selector to down: " + jSelector);
		}
	}
	
	/**
	 * A selector passed from the AmbientTalk to the Java level undergoes the following transformations:
	 * 
	 * - any colons (:) are replaced by underscores (_)
	 * - any operator symbol is replaced by _op{code}_ where code is generated as follows:
	 *  Operator codes are:
	 *   + -> pls
	 *   - -> mns
	 *   * -> tms
	 *   / -> div
	 *   \ -> bsl
	 *   ! -> not
	 *   > -> gtx
	 *   < -> ltx
	 *   = -> eql
	 *   ~ -> til
	 *   ? -> que
	 *   % -> rem
	 */
	public static final String upSelector(ATSymbol atSelector) throws InterpreterException {
		// : -> _
		String nam = atSelector.base_text().asNativeText().javaValue;
        // Backport from JDK 1.4 to 1.3
		// nam = nam.replaceAll(":", "_");
        nam = Pattern.compile(":").matcher(new StringBuffer(nam)).replaceAll("_");
        
		// operator symbol -> _op{code}_
		Matcher m = symbol.matcher(new StringBuffer(nam));
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			// find every occurence of a non-word character and convert it into a symbol
			String oprCode = symbol2oprCode(m.group(0));
			// only add the _op prefix and _ postfix if the code has been found...
			m.appendReplacement(sb, (oprCode.length() == 3) ? "_op" + oprCode  + "_" : oprCode);
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	/**
	 * Transforms an AmbientTalk selector into a Java-level selector prefixed with base_.
	 */
	public static final String upBaseLevelSelector(ATSymbol atSelector) throws InterpreterException {
		return Reflection._BASE_PREFIX_ + upSelector(atSelector);
	}

	/**
	 * Transforms an AmbientTalk selector into a Java-level selector prefixed with meta_.
	 */
	public static final String upMetaLevelSelector(ATSymbol atSelector) throws InterpreterException {
		return Reflection._META_PREFIX_ + upSelector(atSelector);
	}
		
	/**
	 * Constructs an AmbientTalk ATMethod from a Java method.
	 * Given an object obj and a String sel, it is checked whether obj has a method
	 * named sel. If so, the corresponding Java method is wrapped in a NativeMethod.
	 * If not, the downing fails.
	 *
	 * @param natObject the native AmbientTalk object in whose class the method should be found
	 * @param jSelector a selector which should yield a method in natObject
	 * @param origName the original AmbientTalk name of the method
	 * @return a reified method wrapping the Java method
	 * 
	 * Example:
	 *  eval "(reflect: tbl).getMethod('at')" where tbl is a NATTable
	 *  => downMethod(aNATTable, "base_at")
	 *  => NATTable must have a method named base_at
	 *  
	 * Callers should use the more specialised 'downBaseLevelMethod' and 'downMetaLevelMethod'
	 * methods to specify the prefix of the method to be found
	 */
	public static final ATMethod downMethod(ATObject natObject, String jSelector, ATSymbol origName) throws InterpreterException {
		return new NativeMethod(JavaInterfaceAdaptor.getNativeATMethod(natObject.getClass(), natObject, jSelector, origName),
				                origName,
				                natObject);
	}
	
	public static final ATMethod downBaseLevelMethod(ATObject natObject, ATSymbol atSelector) throws InterpreterException {
		return downMethod(natObject, upBaseLevelSelector(atSelector), atSelector);
	}
	
	public static final ATMethod downMetaLevelMethod(ATObject natObject, ATSymbol atSelector) throws InterpreterException {
		return downMethod(natObject, upMetaLevelSelector(atSelector), atSelector);
	}

	/**
	 * downInvocation takes an implicit Java invocation and turns it into an explicit
	 * AmbientTalk invocation process. This happens when Java code sends normal
	 * Java messages to AmbientTalk objects (wrapped by a mirage).
	 * 
	 * @param atRcvr the AmbientTalk object having received the Java method invocation
	 * @param jSelector the Java selector, to be converted to an AmbientTalk selector
	 * @param jArgs the arguments to the Java method invocation (normally all args are ATObjects)
	 * jArgs may be null, indicating that there are no arguments
	 * @return the return value of the AmbientTalk method invoked via the java invocation.
	 * 
	 * Example:
	 *  in Java: "tbl.base_at(1)" where tbl is an ATTable coercer wrapping aNATObject
	 *  => downInvocation(aNATObject, "base_at", ATObject[] { ATNumber(1) })
	 *  => aNATObject must implement a method named "at"
	 *  
	 * Depending on the prefix of the invoked Java method selector, the following translation should occur:
	 *  - obj.base_selector(args) => obj.meta_invoke(obj, selector, args)
	 *  - obj.base_selector() => obj.meta_invokeField(obj, selector)
	 *  - obj.meta_selector(args) => obj.meta_selector(args)
	 *  - obj.selector(args) => either obj.selector(args) if selector is understood natively
	 *                          or     obj.meta_invoke(obj, selector, args) otherwise
	 *  - obj.selector() => obj.meta_invokeField(obj, selector)
	 */
	public static final ATObject downInvocation(ATObject atRcvr, Method jMethod, ATObject[] jArgs) throws InterpreterException {
		String jSelector = jMethod.getName();
		if (jArgs == null) { jArgs = NATTable.EMPTY.elements_; }
		
		if (jSelector.startsWith(Reflection._BASE_PREFIX_)) {
			if (jArgs.length == 0) {
				// obj.base_selector() => obj.meta_invokeField(obj, selector)
				return atRcvr.meta_invokeField(atRcvr, downBaseLevelSelector(jSelector));
			} else {
				// obj.base_selector(args) => obj.meta_invoke(obj, selector, args)
				return atRcvr.meta_invoke(atRcvr, downBaseLevelSelector(jSelector), NATTable.atValue(jArgs));	
			}
		} else if (jSelector.startsWith(Reflection._META_PREFIX_)) {
			// obj.meta_selector(args) => obj.meta_selector(args)
			return JavaInterfaceAdaptor.invokeNativeATMethod(jMethod, atRcvr, jArgs);
		} else {
			// atRcvr can respond to the given method natively
			if (jMethod.getDeclaringClass().isInstance(atRcvr)) {
				return JavaInterfaceAdaptor.invokeNativeATMethod(jMethod, atRcvr, jArgs);
			} else {
				if (jArgs.length == 0) {
				    // obj.selector() => obj.meta_invokeField(obj, selector)
				    return atRcvr.meta_invokeField(atRcvr, downSelector(jSelector));
				} else {
				    // obj.selector(args) => obj.meta_invoke(obj, selector, args)
				    return atRcvr.meta_invoke(atRcvr, downSelector(jSelector), NATTable.atValue(jArgs));	
				}
			}
		}
	}

	/**
	 * upInvocation takes an explicit AmbientTalk method invocation and turns it into an
	 * implicitly performed Java invocation.
	 * 
	 * Depending on whether the AmbientTalk invocation happens at the base-level or the meta-level
	 * (i.e. the receiver denotes a base-level object or a mirror), the jSelector parameter will have
	 * a different prefix.
	 * 
	 * @param atOrigRcvr the original AmbientTalk object that received the invocation
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @param atArgs the arguments to the AmbientTalk method invocation
	 * @return the return value of the Java method invoked via the java invocation.
	 * 
	 * Example:
	 *  eval "tbl.at(1)" where tbl is a NATTable
	 *  => upInvocation(aNATTable, "base_at", ATObject[] { ATNumber(1) })
	 *  => NATTable must have a method named base_at
	 * 
	 * Example:
	 *  eval "(reflect: tbl).invoke(tbl, "at", [1])" where tbl is a NATTable
	 *  => upInvocation(aNATTable, "meta_invoke", ATObject[] { aNATTable, ATSymbol('at'), ATTable([ATNumber(1)]) })
	 *  => NATTable must have a method named meta_invoke
	 */
	public static final ATObject upInvocation(ATObject atOrigRcvr, String jSelector, ATSymbol atSelector, ATTable atArgs) throws InterpreterException {
		return JavaInterfaceAdaptor.invokeNativeATMethod(
				    atOrigRcvr.getClass(),
				    atOrigRcvr,
					jSelector,
					atSelector, atArgs.asNativeTable().elements_);
	}
	
	/**
	 * upRespondsTo transforms an explicit AmbientTalk respondsTo meta-level request
	 * into an implicit check whether the given jRcvr java object has a method
	 * corresponding to the given selector, prefixed with base_
	 * 
	 * @param jRcvr the Java object being queried for a certain selector
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @return a boolean indicating whether the jRcvr implements a method corresponding to base_ + atSelector
	 * 
	 * Example:
	 *  eval "(reflect: [1,2,3]).respondsTo("at")" where the receiver of repondsTo is a NATTable
	 *  => upRespondsTo(aNATTable, "at")
	 *  => NATTable must have a method named base_at
	 */
	public static final boolean upRespondsTo(ATObject jRcvr,String jSelector) throws InterpreterException {
		return JavaInterfaceAdaptor.hasApplicableJavaMethod(
				jRcvr.getClass(),
				jSelector);
	}
	
	/**
	 * upMethodSelection takes an explicit AmbientTalk field selection and checks whether 
	 * a Java method exists that matches the selector. If so, this method is wrapped in a 
	 * NativeClosure and returned.
	 * 
	 * @param atOrigRcvr the original AmbientTalk object that received the selection
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @return a closure wrapping the method selected via the AmbientTalk selection.
	 * 
	 * Example:
	 *  eval "[1,2,3].at"
	 *  => upSelection(aNATTable, "at")
	 *  => either NATTable must have a method base_at, which is then wrapped
	 */
	public static final NativeMethod upMethodSelection(ATObject atOrigRcvr, String jSelector, ATSymbol origSelector) throws InterpreterException {
		Method m = JavaInterfaceAdaptor.getNativeATMethod(atOrigRcvr.getClass(), atOrigRcvr, jSelector, origSelector);
		return new NativeMethod(m, origSelector, atOrigRcvr);
	}
	
	/**
	 * upInstanceCreation takes an explicit AmbientTalk 'new' invocation and turns it into an
	 * implicit Java instance creation by calling a constructor. The initargs are upped as well
	 * and are passed as arguments to the constructor.
	 * 
	 * @param jRcvr the Java object having received the call to new
	 * @param atInitargs the arguments to the constructor
	 * @return a new instance of a Java class
	 * @throws InterpreterException
	 */
	public static final ATObject upInstanceCreation(ATObject jRcvr, ATTable atInitargs) throws InterpreterException {
		ATObject[] args = atInitargs.asNativeTable().elements_;
		return JavaInterfaceAdaptor.createNativeATObject(jRcvr.getClass(), args);
	}
	
	public static final ATObject upExceptionCreation(InterpreterException jRcvr, ATTable atInitargs) throws InterpreterException {
		ATObject[] args = atInitargs.asNativeTable().elements_;
		return Symbiosis.symbioticInstanceCreation(jRcvr.getClass(), args);
	}

	/**
	 * Pass an AmbientTalk meta-level object into the base-level
	 */
	public static final ATObject downObject(ATObject metaObject) throws InterpreterException {
		return metaObject;
		/*if (metaObject.meta_isTaggedAs(NativeTypeTags._MIRROR_).asNativeBoolean().javaValue) {
			return metaObject.meta_select(metaObject, OBJMirrorRoot._BASE_NAME_);
		} else {
			return metaObject; // most native objects represent both the object at the base and at the meta-level
		}*/
	}
	
	/**
	 * Pass an AmbientTalk base-level object to the meta-level
	 */
	public static final ATObject upObject(ATObject baseObject) {
		if (baseObject instanceof NATMirage) {
			return ((NATMirage) baseObject).getMirror();
		} else {
			return baseObject;
		}
	}
	
	/**
	 * Returns, for a given AmbientTalk object atObj, an array of NativeMethod objects corresponding
	 * to all non-static methods of that object's class, where each method's name is prefixed with 'base_'
	 */
	public static final ATMethod[] downBaseLevelMethods(ATObject atObj) throws InterpreterException {
		Method[] allBaseMethods =
			JavaInterfaceAdaptor.allMethodsPrefixed(atObj.getClass(), Reflection._BASE_PREFIX_, false);
		Vector allATBaseMethods = new Vector();
		for (int i = 0; i < allBaseMethods.length; i++) {
			Method m = allBaseMethods[i];
			String nam = m.getName();
			allATBaseMethods.add(new NativeMethod(m, downBaseLevelSelector(nam), atObj));
		}
		return (ATMethod[]) allATBaseMethods.toArray(new ATMethod[allATBaseMethods.size()]);
	}
	
	/**
	 * Returns, for a given AmbientTalk object natObj, an array of NativeMethod objects corresponding
	 * to all non-static methods of that object's class, where each method's name is prefixed with 'meta_'
	 */
	public static final ATMethod[] downMetaLevelMethods(ATObject natObj) throws InterpreterException {
		Method[] allMetaMethods =
			JavaInterfaceAdaptor.allMethodsPrefixed(natObj.getClass(), Reflection._META_PREFIX_, false);
		Vector allATMetaMethods = new Vector();
		for (int i = 0; i < allMetaMethods.length; i++) {
			Method m = allMetaMethods[i];
			String nam = m.getName();
			allATMetaMethods.add(new NativeMethod(m, downMetaLevelSelector(nam), natObj));
		}
		return (ATMethod[]) allATMetaMethods.toArray(new ATMethod[allATMetaMethods.size()]);
	}
	
	private static final Pattern oprCode = Pattern.compile("_op(\\w\\w\\w)_"); //'_op', 3 chars, '_'
	private static final Pattern symbol = Pattern.compile("\\W"); //any non-word character
	
	private static String stripPrefix(String input, String prefix) {
		// \A matches start of input
	    // Backport from JDK 1.4 to 1.3
        // return input.replaceFirst("\\A"+prefix, "");
		return Pattern.compile("\\A"+prefix).matcher(new StringBuffer(input)).replaceFirst("");
	}
	
	private static final String oprCode2Symbol(String code) {
		switch (code.charAt(0)) {
		  case 'p': if (code.equals("pls")) { return "+"; } else break;
		  case 'm': if (code.equals("mns")) { return "-"; } else break;
		  case 't': if (code.equals("tms")) { return "*"; } else
			        if (code.equals("til")) { return "~"; } else break;
		  case 'd': if (code.equals("div")) { return "/"; } else break;
		  case 'b': if (code.equals("bsl")) { return "\\"; } else break;
		  case 'n': if (code.equals("not")) { return "!"; } else break;
		  case 'g': if (code.equals("gtx")) { return ">"; } else break;
		  case 'l': if (code.equals("ltx")) { return "<"; } else break;
		  case 'e': if (code.equals("eql")) { return "="; } else break;
		  case 'q': if (code.equals("que")) { return "?"; } else break;
		  case 'r': if (code.equals("rem")) { return "%"; } else break;
		}
		return "_op" + code + "_"; // no match, return original input
	}
	
	private static final String symbol2oprCode(String symbol) {
		switch (symbol.charAt(0)) {
		  case '+': return "pls";
		  case '-': return "mns";
		  case '*': return "tms";
		  case '/': return "div";
		  case '\\': return "bsl";
		  case '!': return "not";
		  case '>': return "gtx";
		  case '<': return "ltx";
		  case '=': return "eql";
		  case '~': return "til";
		  case '?': return "que";
		  case '%': return "rem";
		  default: return symbol; // no match, return original input
		}	
	}
	
	private static final String javaToAmbientTalkSelector(String jSelector) {
		// _op{code}_ -> operator symbol
		Matcher m = oprCode.matcher(new StringBuffer(jSelector));
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
             // find every occurence of _op\w\w\w_ and convert it into a symbol
			m.appendReplacement(sb, oprCode2Symbol(m.group(1)));
		}
		m.appendTail(sb);
		
		// _ -> :
        // Backport from JDK 1.4 to 1.3
		// return sb.toString().replaceAll("_", ":");
        return Pattern.compile("_").matcher(sb).replaceAll(":");
	}
	
}
