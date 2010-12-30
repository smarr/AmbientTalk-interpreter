package edu.vub.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSerializationError;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.natives.NATText;

/**
 * Uniquely maps ATObjects to their serialized version
 * and links them to a temporary name
 * 
 * @author kpinte
 */
public class TempFieldGenerator {
	
	private static final String PREFIX = "_v";
	

	
	private final Vector<ATObject> objects_;
	private final HashMap<ATObject, NATText> objectToCode_;
	private final HashMap<ATObject, NATText> objectToName_;
	private final HashMap<ATTypeTag, NATText> typeToCode_;
	private final HashMap<ATTypeTag, NATText> typeToName_;
	private int nameCounter_;
	private int quoteLevel_;
	
	public TempFieldGenerator() {
		objects_ = new Vector<ATObject>();
		objectToCode_ = new HashMap<ATObject, NATText>();
		objectToName_ = new HashMap<ATObject, NATText>();
		typeToCode_ = new HashMap<ATTypeTag, NATText>();
		typeToName_ = new HashMap<ATTypeTag, NATText>();
		nameCounter_ = 0;
		quoteLevel_ = 0;
	}
	
	private synchronized String generateName() {
		nameCounter_++;
		return PREFIX + nameCounter_;
	}
	
	public boolean inQuote() {
		return (quoteLevel_ > 0);
	}
	
	public boolean incQuoteLevel() {
		quoteLevel_++;
		return inQuote();
	}
	
	public boolean decQuoteLevel() throws XSerializationError {
		if (quoteLevel_ <= 0) {
			throw new XSerializationError("quote level below 0");
		}
		quoteLevel_--;
		return inQuote();
	}
	
	public NATText put(ATObject object, NATText code) {
		NATText name;
		if (this.contains(object)){
			name = objectToName_.get(object);
		} else {
			name = NATText.atValue(this.generateName());
			objects_.add(object);
		}
		objectToCode_.put(object, code);
		objectToName_.put(object, name);
		if (this.inQuote()) {
			return NATText.atValue("#" + name.javaValue);
		} else {
			return name;
		} 
	}
	
	public NATText putType(ATTypeTag tt, NATText name, NATText code) {
		typeToCode_.put(tt, code);
		typeToName_.put(tt, name);
		if (this.inQuote()) {
			return NATText.atValue("#" + name.javaValue);
		} else {
			return name;
		} 
	}
	
	public NATText getName(ATObject key) {
		NATText name = objectToName_.get(key);
		if (this.inQuote()) {
			return NATText.atValue("#" + name.javaValue);
		} else {
			return name;
		} 
	}
	
	public NATText getTypeName(ATTypeTag tt) {
		NATText name = typeToName_.get(tt);
		if (this.inQuote()) {
			return NATText.atValue("#" + name.javaValue);
		} else {
			return name;
		} 
	}
	
	public NATText getTypeCode(ATTypeTag tt) {
		return typeToCode_.get(tt);
	}
	
	public NATText getCode(ATObject key) {
		return objectToCode_.get(key);
	}
	
	public Boolean contains(ATObject object) {
		return objectToName_.containsKey(object);
	}
	
	public Boolean containsType(ATTypeTag tt) {
		return typeToName_.containsKey(tt);
	}
	
	public NATText generateCode(NATText targetName) throws InterpreterException {
		StringBuffer out = new StringBuffer("{");
		Iterator<ATTypeTag> tit = typeToName_.keySet().iterator();
		while (tit.hasNext()) {
			ATTypeTag tt = tit.next();
			NATText name = this.getTypeName(tt);
			NATText code = this.getTypeCode(tt);
			out.append(code.javaValue + "; ");
		};
			
		Iterator<ATObject> it = objects_.iterator();
		while(it.hasNext()) {
			ATObject key = it.next();
			NATText name = this.getName(key);
			NATText code = this.getCode(key);
			out.append("def " + name.javaValue + " := ");
			out.append(code.javaValue);
			out.append("; ");
		}
		out.append(targetName.javaValue);
		out.append("}()");
		return NATText.atValue(out.toString());
	}

}
