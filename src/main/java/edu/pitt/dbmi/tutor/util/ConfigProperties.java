package edu.pitt.dbmi.tutor.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.text.DateFormatter;

/**
 * config properties is properties that keeps order of properties
 * as well as have the ability to extract comments for individual keys
 * @author tseytlin
 */
public class ConfigProperties extends Properties {
	private Set keys = new LinkedHashSet();
	private Map<Object,String> comments = new HashMap<Object,String>();
	private Date date;
	
	
	/**
	 * clear all properties
	 */
	public synchronized void clear() {
		super.clear();
		keys.clear();
		comments.clear();
	}

	/**
	 * clone this object
	 */
	public ConfigProperties clone(){
		ConfigProperties p = new ConfigProperties();
		for(Object key: keySet()){
			p.put(key,get(key));
			if(comments.containsKey(key))
				p.setPropertyComment(""+key,comments.get(key));
		}
		return p;
	}
	
	/**
	 * get keys in the order that they were inserted
	 */
	public Set<Object> keySet() {
		return  Collections.synchronizedSet(keys);
	}
	
	
	/**
	 * return property comment or null if not available
	 * @param key
	 * @return
	 */
	public String getPropertyComment(String key){
		return comments.get(key);
	}
	
	/**
	 * return property comment or null if not available
	 * @param key
	 * @return
	 */
	public void setPropertyComment(String key,String value){
		comments.put(key,value);
	}
	
	/**
	 * return all property comment or empty collection
	 * @param key
	 * @return
	 */
	public Collection<String> getPropertyComments(){
		return comments.values();
	}
	
	/**
	 * get enumeration of keys
	 */
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(keys);
	}

	public synchronized Object remove(Object e) {
		keys.remove(e);
		comments.remove(e);
		return super.remove(e);
	}
	
	/**
	 * has property
	 * @param key
	 * @return
	 */
	public boolean hasProperty(String key){
		return containsKey(key);
	}
	
	/**
	 * insert key (mind the order)
	 */
	public synchronized Object put(Object key, Object val) {
		keys.add(key);
		return super.put(key, val);
	}

	/**
	 * if date was embedded in property file return it, or null if not
	 * @return
	 */
	public Date getPropertyDate(){
		return date;
	}
	
	/**
	 * load properties
	 */
	public synchronized void load(InputStream in) throws IOException {
		// load properties
		//super.load(in);
		// load comments
		boolean dateparse = false;
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new InputStreamReader(in));
			StringBuffer comment = null;
			for(String line = reader.readLine();line != null; line = reader.readLine()){
				line = line.trim();
				// skip blanks
				if(line.length() == 0)
					continue;
			
				// we got a comment
				if(line.startsWith("#") || line.startsWith("!")){
					if(comment == null)
						comment = new StringBuffer();
					
					// skip date comments if they are first 
					if(!dateparse){
						// dates are normally written out when properties are saved
						DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
					    try{
					    	date = (Date)formatter.parse(line.substring(1).trim());
					    } catch (ParseException e) {
					    	comment.append(line.substring(1).trim());
					    }
					    dateparse = true;
					}else{
						comment.append(" "+line.substring(1).trim());
					}
					
				}else {
					int x = line.indexOf('=');
					if(x > -1){
						// next key after comment is a key with comment
						String key   = line.substring(0,x).trim();
						String value = line.substring(x+1).trim();
						
						//set value
						put(key,value);
						
						// set comment
						if(comment != null){
							comments.put(key,comment.toString().trim());
							comment = null;
						}
					}
				}
			}
		
		}catch(IOException ex){
			throw ex;
		}finally{
			if(reader != null)
				reader.close();
		}
	}

	public void list(PrintStream out) {
		out.println("-- list properties --");
		for(Object key: keySet()){
			System.out.println(key+" = "+get(key));
		}
	}
	
	
}
