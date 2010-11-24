package edu.vub.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSerializationError;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATText;

/**
 * Uniquely maps ATObjects to their serialized version
 * and links them to a temporary name
 * 
 * @author kpinte
 */
public class TempFieldGenerator {
	
	private static final String PREFIX = "tmp_";
	
	private Vector<ATObject> objects_;
	private HashMap<ATObject, NATText> objectToCode_;
	private HashMap<ATObject, NATText> objectToName_;
	private int nameCounter_;
	
	public TempFieldGenerator() {
		objects_ = new Vector<ATObject>();
		objectToCode_ = new HashMap<ATObject, NATText>();
		objectToName_ = new HashMap<ATObject, NATText>();
		nameCounter_ = 0;
	}
	
	private synchronized String generateName() {
		nameCounter_++;
		return PREFIX + nameCounter_;
	}
	
	public NATText put(ATObject object, NATText code) {
		NATText name;
		if (this.contains(object)){
			name = this.getName(object);
		} else {
			name = NATText.atValue(this.generateName());
			objects_.add(object);
		}
		objectToCode_.put(object, code);
		objectToName_.put(object, name);
		return name;
	}
	
	public NATText getName(ATObject key) {
		return objectToName_.get(key);
	}
	
	public NATText getCode(ATObject key) {
		return objectToCode_.get(key);
	}
	
	public Boolean contains(ATObject object) {
		return objectToName_.containsKey(object);
	}
	
	public NATText generateCode(NATText targetName) throws InterpreterException {
		StringBuffer out = new StringBuffer("{");
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
