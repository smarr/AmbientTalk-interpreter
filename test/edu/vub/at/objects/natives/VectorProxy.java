/**
 * 
 */
package edu.vub.at.objects.natives;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 *  Auxiliary class which forwards all operations to the default Vector but has elementCount as a public field
 */
public class VectorProxy {
	public int elementCount;

	private java.util.Vector impl;

	public VectorProxy() {
		impl = new java.util.Vector();
	}
	
	public void add(int index, Object element) {
		impl.add(index, element);
		elementCount++;
	}

	public synchronized boolean add(Object o) {
		elementCount++;
		return impl.add(o);
	}

	public synchronized boolean addAll(Collection c) {
		elementCount += c.size();
		return impl.addAll(c);
	}

	public synchronized boolean addAll(int index, Collection c) {
		elementCount += c.size();
		return impl.addAll(index, c);
	}

	public synchronized void addElement(Object obj) {
		elementCount++;
		impl.addElement(obj);
	}

	public int capacity() {
		return impl.capacity();
	}

	public void clear() {
		elementCount = 0;
		impl.clear();
	}

	public synchronized Object clone() {
		return impl.clone();
	}

	public boolean contains(Object elem) {
		return impl.contains(elem);
	}

	public synchronized boolean containsAll(Collection c) {
		return impl.containsAll(c);
	}

	public synchronized void copyInto(Object[] anArray) {
		impl.copyInto(anArray);
	}

	public synchronized Object elementAt(int index) {
		return impl.elementAt(index);
	}

	public Enumeration elements() {
		return impl.elements();
	}

	public synchronized void ensureCapacity(int minCapacity) {
		impl.ensureCapacity(minCapacity);
	}

	public synchronized boolean equals(Object o) {
		return impl.equals(o);
	}

	public synchronized Object firstElement() {
		return impl.firstElement();
	}

	public synchronized Object get(int index) {
		return impl.get(index);
	}

	public synchronized int hashCode() {
		return impl.hashCode();
	}

	public synchronized int indexOf(Object elem, int index) {
		return impl.indexOf(elem, index);
	}

	public int indexOf(Object elem) {
		return impl.indexOf(elem);
	}

	public synchronized void insertElementAt(Object obj, int index) {
		impl.insertElementAt(obj, index);
	}

	public boolean isEmpty() {
		return impl.isEmpty();
	}

	public synchronized Object lastElement() {
		return impl.lastElement();
	}

	public synchronized int lastIndexOf(Object elem, int index) {
		return impl.lastIndexOf(elem, index);
	}

	public int lastIndexOf(Object elem) {
		return impl.lastIndexOf(elem);
	}

	public synchronized Object remove(int index) {
		elementCount--;
		return impl.remove(index);
	}

	public boolean remove(Object o) {
		boolean result = impl.remove(o);
		if(result) elementCount--;
		return result;
	}

	public synchronized boolean removeAll(Collection c) {
		boolean result = impl.removeAll(c);
		elementCount = impl.size();
		return result;
	}

	public synchronized void removeAllElements() {
		elementCount = 0;
		impl.removeAllElements();
	}

	public synchronized boolean removeElement(Object obj) {
		boolean result = impl.removeElement(obj);
		if(result) elementCount--;
		return result;
	}

	public synchronized void removeElementAt(int index) {
		elementCount--;
		impl.removeElementAt(index);
	}

	public synchronized boolean retainAll(Collection c) {
		boolean result = impl.retainAll(c);
		elementCount = impl.size();
		return result;
	}

	public synchronized Object set(int index, Object element) {
		return impl.set(index, element);
	}

	public synchronized void setElementAt(Object obj, int index) {
		impl.setElementAt(obj, index);
	}

	public synchronized void setSize(int newSize) {
		impl.setSize(newSize);
	}

	public int size() {
		return impl.size();
	}

	public List subList(int fromIndex, int toIndex) {
		return impl.subList(fromIndex, toIndex);
	}

	public synchronized Object[] toArray() {
		return impl.toArray();
	}

	public synchronized Object[] toArray(Object[] a) {
		return impl.toArray(a);
	}

	public synchronized String toString() {
		return impl.toString();
	}

	public synchronized void trimToSize() {
		impl.trimToSize();
	} 
}