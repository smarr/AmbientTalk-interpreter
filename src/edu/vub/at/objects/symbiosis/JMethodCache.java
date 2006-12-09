/**
 * AmbientTalk/2 Project
 * JMethodCache.java created on 9-dec-2006 at 17:14:55
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
package edu.vub.at.objects.symbiosis;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

/**
 * A singleton object that represents a method cache. It is used to speed up
 * the symbiosis with Java. Every time a JavaMethod is successfully created from
 * an array of java.lang.reflect.Method objects, it is stored in the cache.
 * The cache uses weak references such that JavaMethod instances can be safely recollected
 * when they are no longer used. Note that this caching scheme only works because:
 *  - Java classes are unmodifyable at runtime
 *  - JavaMethod objects are constant, so may be safely shared between different actors
 * 
 * The method cache is implemented as a hash map, mapping pairs
 * [ java.lang.Class class, java.lang.String methodname, boolean isStatic ] => soft ref to JavaMethod
 * 
 * If a JavaMethod has been garbage collected, its entry is removed from the cache.
 * Soft references rather than weak references are used to point to the JavaMethod entries.
 * Soft references are like weak references, but normally only get reclaimed when the
 * JVM runs low on memory. Hence, they are much more suitable for caching.
 * 
 * The cache is synchronized to ensure proper synchronization between different actors.
 * 
 * @author tvcutsem
 */
public final class JMethodCache {
	
	public static final JMethodCache _INSTANCE_ = new JMethodCache();
	
	private final Map cache_;
	
	private JMethodCache() {
		cache_ = Collections.synchronizedMap(new Hashtable());
	}
	
	/**
	 * Add a JavaMethod instance to the cache.
	 */
	public void put(Class cls, String methName, boolean isStatic, JavaMethod entry) {
		cache_.put(new CacheKey(cls, methName, isStatic), new SoftReference(entry));
	}
	
	/**
	 * Retrieve a JavaMethod entry from the cache.
	 * @return the entry, or null if the cache does not contain the entry
	 */
	public JavaMethod get(Class cls, String methName, boolean isStatic) {
		CacheKey key = new CacheKey(cls, methName, isStatic);
		SoftReference ref = (SoftReference) cache_.get(key);
		if (ref == null) {
			return null; // cache miss
		} else {
			JavaMethod entry = (JavaMethod) ref.get();
			if (entry != null) {
				// cache hit
				return entry;
			} else {
				// entry was garbage-collected, clean up the mapping
				cache_.remove(key);
				return null; // cache miss
			}
		}
	}
	
	private static class CacheKey {
		private final Class class_;
		private final String methodName_;
		private final boolean isStatic_;
		public CacheKey(Class c, String mname, boolean isStatic) {
			class_ = c;
			methodName_ = mname;
			isStatic_ = isStatic;
		}
		
		public boolean equals(Object other) {
		    // no check on type of other for performance reasons:
			// these CacheKeys are not compared to anything else
			CacheKey otherKey = (CacheKey) other;
			return ((class_.equals(otherKey.class_))
					&& (methodName_.equals(otherKey.methodName_))
					&& (isStatic_ == otherKey.isStatic_));
		}
		
		public int hashCode() {
			return (class_.hashCode() | methodName_.hashCode()) << ((isStatic_) ? 1 : 0);
		}
	}

}
