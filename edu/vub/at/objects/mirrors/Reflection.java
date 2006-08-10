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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tvc
 *
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
 */
public final class Reflection {
	
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
	 *   and -> &
	 *   car -> ^
	 *   not -> !
	 *   gtx -> >
	 *   ltx -> <
	 *   eql -> =
	 * - any underscores (_) are replaced by colons (:)
	 */
	public static final ATSymbol downSelector(String jSelector) {
		// _op{code}_ -> operator symbol
		Matcher m = oprCode.matcher(jSelector);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
             // find every occurence of _op\w\w\w_ and convert it into a symbol
			m.appendReplacement(sb, oprCode2Symbol(m.group(1)));
		}
		m.appendTail(sb);
		
		// _ -> :
		return AGSymbol.alloc(sb.toString().replaceAll("_", ":"));
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
	 *   & -> and
	 *   ^ -> car
	 *   ! -> not
	 *   > -> gtx
	 *   < -> ltx
	 *   = -> eql
	 */
	public static final String upSelector(AGSymbol atSelector) throws NATException {
		// : -> _
		String nam = atSelector.getText().asNativeText().javaValue;
		nam = nam.replaceAll(":", "_");
		
		// operator symbol -> _op{code}_
		Matcher m = symbol.matcher(nam);
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
	 * Constructs an AmbientTalk ATField from a pair of getter/setter methods of
	 * a Java object. Given an object obj and a String sel, it is checked whether
	 *  a) obj has a method named 'get' + Sel, if so, a field can be created
	 *  b) obj has a method named 'set' + Sel, if so, the field is mutable, otherwise it is read-only
	 *
	 * The getter method cannot take any arguments, the setter method must be unary.
	 *
	 * @param jObject the Java object in which the getter/setter methods should be found
	 * @param jSelector a selector which, when prefixed with 'get' or 'set' should yield a method in jObject
	 * @return a reified field, which may either be read-only or mutable depending on available methods
	 * 
	 * Example:
	 *  eval "(reflect: msg).getField('selector')" where msg is a NATMessage
	 *  => downField(aNATMessage, "selector")
	 *  => NATMessage must have a zero-arg method getSelector and optionally setSelector
	 */
	public static final ATField downField(ATObject jObject, String jSelector) {
		return null;
	}
	
	/**
	 * Constructs an AmbientTalk ATClosure from a Java method.
	 * Given an object obj and a String sel, it is checked whether obj has a method
	 * named 'base_' + sel. If so, the corresponding Java method is wrapped in a JavaClosure.
	 * If not, the downing fails.
	 *
	 * @param jObject the Java object in which the method should be found
	 * @param jSelector a selector which, when prefixed with 'base_' should yield a method in jObject
	 * @return a reified closure wrapping the Java method
	 * 
	 * Example:
	 *  eval "(reflect: tbl).getMethod('at')" where tbl is a NATTable
	 *  => downMethod(aNATTable, "at")
	 *  => NATTable must have a method named base_at
	 */
	public static final ATMethod downMethod(ATObject jObject, String jSelector) {
		return null;
	}

	/**
	 * downInvocation takes an implicit Java invocation and turns it into an explicit
	 * AmbientTalk invocation process. This happens when Java code sends normal
	 * Java messages to AmbientTalk objects (wrapped by a mirage).
	 * 
	 * @param atRcvr the AmbientTalk object having received the Java method invocation
	 * @param jSelector the Java selector, to be converted to an AmbientTalk selector
	 * @param jArgs the arguments to the Java method invocation (normally all args are ATObjects) 
	 * @return the return value of the AmbientTalk method invoked via the java invocation.
	 * 
	 * Example:
	 *  in Java: "tbl.base_at(1)" where tbl is an ATTable mirage wrapping aNATObject
	 *  => downInvocation(aNATObject, "at", ATObject[] { ATNumber(1) })
	 *  => aNATObject must implement a method named "at"
	 * The base_ prefix is normally stripped off by a mirage.
	 */
	public static final ATObject downInvocation(ATObject atRcvr, String jSelector, ATObject[] jArgs) {
		return null;
	}

	/**
	 * upInvocation takes an explicit AmbientTalk method invocation and turns it into an
	 * implicitly performed Java invocation.
	 * 
	 * @param jRcvr the Java object having received the AmbientTalk method invocation
	 * @param atSelector the AmbientTalk selector, to be converted to a Java selector
	 * @param atArgs the arguments to the AmbientTalk method invocation
	 * @return the return value of the Java method invoked via the java invocation.
	 * 
	 * Example:
	 *  eval "tbl.at(1)" where tbl is a NATTable
	 *  => upInvocation(aNATTable, "at", ATObject[] { ATNumber(1) })
	 *  => NATTable must have a method named base_at
	 */
	public static final Object upInvocation(ATObject jRcvr, AGSymbol atSelector, ATObject[] atArgs) {
		return null;
	}

	/**
	 * downSelection takes an implicit Java selection (in the guise of invoking a getter method)
	 * and turns it into an explicit AmbientTalk selection process. This happens when Java code sends normal
	 * Java messages to AmbientTalk objects (wrapped by a mirage).
	 * 
	 * @param atRcvr the AmbientTalk object having received the Java selection
	 * @param jSelector the Java selector, without the 'get' prefix, to be converted to an AmbientTalk selector
	 * @return the value of the AmbientTalk field selected by the java selection.
	 * 
	 * Example:
	 *  in Java: "msg.getSelector()" where msg is am ATMessage mirage wrapping a NATObject
	 *  => downSelection(aNATObject, "selector")
	 *  => aNATObject must implement a field named "selector"
	 * The get prefix is normally stripped off by a mirage
	 */
	public static final ATObject downSelection(ATObject atRcvr, String jSelector) {
		return null;
	}
	
	/**
	 * upSelection takes an explicit AmbientTalk field selection and turns it into an
	 * implicitly performed Java selection by invoking a getter method.
	 * 
	 * @param jRcvr the Java object having received the AmbientTalk field selection
	 * @param atSelector the AmbientTalk selector, to be converted to a Java getter method selector
	 * @return the return value of the Java getter method invoked via the AmbientTalk selection.
	 * 
	 * Example:
	 *  eval "msg.selector" where msg is a NATMessage
	 *  => upSelection(aNATMessage, "selector")
	 *  => NATMessage must have a zero-argument method named getSelector
	 */
	public static final Object upSelection(ATObject jRcvr, AGSymbol atSelector) {
		return null;
	}

	/**
	 * Convert a Java object into an AmbientTalk object.
	 * This is currently only possible if the Java object is a mirror (i.e. it implements ATObject)
	 * or if it is a 'native type'.
	 * 
	 * @param jObj the Java object representing a mirror or a native type
	 * @return the same object if it implements the ATObject interface
	 */
	public static final ATObject downObject(Object jObj) {
		// Our own "dynamic dispatch"
		// mirror
		if(jObj instanceof ATObject) {
			return (ATObject) jObj;
	    // integer
		} else if (jObj instanceof Integer) {
			return NATNumber.atValue(((Integer) jObj).intValue());
		// double
		} else if (jObj instanceof Double) {
			return NATFraction.atValue(((Double) jObj).doubleValue());
		// float
		} else if (jObj instanceof Float) {
			return NATFraction.atValue(((Float) jObj).floatValue());
		// string
		} else if (jObj instanceof String) {
			return NATText.atValue((String) jObj);
		// char
		} else if (jObj instanceof Character) {
			return NATText.atValue(((Character) jObj).toString());
		// boolean
		} else if (jObj instanceof Boolean) {
			return NATBoolean.atValue(((Boolean) jObj).booleanValue());
		// byte
		} else if (jObj instanceof Byte) {
			return NATNumber.atValue(((Byte) jObj).intValue());
		// short
		} else if (jObj instanceof Short) {
			return NATNumber.atValue(((Short) jObj).intValue());
		// Object[]
		} else if (jObj instanceof Object[]) {
			Object[] jArray = (Object[]) jObj;
			ATObject[] atTable = new ATObject[jArray.length];
			for (int i = 0; i < jArray.length; i++) {
				atTable[i] = downObject(jArray[i]);
			}
			return new NATTable(atTable);
		} else {
			throw new RuntimeException("Cannot wrap Java objects of type " + jObj.getClass());			
		}
	}

	/**
	 * Convert an AmbientTalk object into its Java equivalent.
	 * TODO: write a 'toJava' method that for NATNumber etc. return an Integer etc.
	 */
	public static final Object upObject(ATObject atObj) {
		return atObj;
	}
	
	
	private static final Pattern oprCode = Pattern.compile("_op(\\w\\w\\w)_"); //'_op', 3 chars, '_'
	private static final Pattern symbol = Pattern.compile("\\W"); //any non-word character
	
	private static String stripPrefix(String input, String prefix) {
		// \A matches start of input
		return input.replaceFirst("\\A"+prefix, "");
	}
	
	private static final String oprCode2Symbol(String code) {
		switch (code.charAt(0)) {
		  case 'p': if (code.equals("pls")) { return "+"; } else break;
		  case 'm': if (code.equals("mns")) { return "-"; } else break;
		  case 't': if (code.equals("tms")) { return "*"; } else break;
		  case 'd': if (code.equals("div")) { return "/"; } else break;
		  case 'b': if (code.equals("bsl")) { return "\\"; } else break;
		  case 'a': if (code.equals("and")) { return "&"; } else break;
		  case 'c': if (code.equals("car")) { return "^"; } else break;
		  case 'n': if (code.equals("not")) { return "!"; } else break;
		  case 'g': if (code.equals("gtx")) { return ">"; } else break;
		  case 'l': if (code.equals("ltx")) { return "<"; } else break;
		  case 'e': if (code.equals("eql")) { return "="; } else break;
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
		  case '&': return "and";
		  case '^': return "car";
		  case '!': return "not";
		  case '>': return "gtx";
		  case '<': return "ltx";
		  case '=': return "eql";
		  default: return symbol; // no match, return original input
		}	
	}
	
}