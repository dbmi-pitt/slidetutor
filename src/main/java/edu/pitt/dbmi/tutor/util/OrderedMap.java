package edu.pitt.dbmi.tutor.util;

import java.util.*;

public class OrderedMap<K,V> extends Hashtable<K,V> {
	private List<K> keys;
	private List<V> values;
	
	public OrderedMap(){
		super();
		keys = new Vector<K>();
		values = new Vector<V>();
	}
	
	public void insert(K key, V value,int x){
		super.put(key, value);
		keys.add(x,key);
		values.add(x,value);
	}
	
	public void set(K key, V value,int x){
		super.put(key, value);
		K old = keys.get(x);
		keys.set(x,key);
		values.set(x,value);
		if(old != null && containsKey(old))
			remove(old);
	}
	
	/**
	 * put new values
	 */
	public V put(K key, V value){
		// don't put stuff that was already in the map
		if(super.keySet().contains(key)){
			return value;
		}
		keys.add(key);
		values.add(value);
		return super.put(key,value);
	}
	
	/**
	 * put new values
	 */
	public void putAll(Map<? extends K,? extends V> m){
		keys.addAll(m.keySet());
		values.addAll(m.values());
		super.putAll(m);
	}
	
	public V remove(Object key){
		V val = super.remove(key);
		keys.remove(key);
		values.remove(val);
		return val;
	}
	
	public List<K> getKeys(){
		return keys;
	}
	
	public List<V> getValues(){
		return values;
	}
	
	public synchronized void clear() {
		super.clear();
		keys.clear();
		values.clear();
	}

	public void sort(){
		Comparator c = new Comparator(){
			public int compare(Object a, Object b) {
				return a.toString().compareToIgnoreCase(b.toString());
			}
		};
		Collections.sort(keys,c);
		Collections.sort(values,c);
	}
}
