package edu.pitt.dbmi.tutor.messages;

import java.util.*;

import edu.pitt.dbmi.tutor.util.TextHelper;

/**
 * encompasses diffent query parameters for Session object
 * @author tseytlin
 */
public class Query extends LinkedHashMap<String,Set<String>>{
	private final String q = "'";
	
	/**
	 * convinience method to get simple queries 
	 * @param user
	 * @return
	 */
	public static Query createUsernameQuery(String user){
		Query q = new Query();
		q.addUsername(user);
		return q;
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void addUsername(String user){
		// check for null
		if(TextHelper.isEmpty(user))
			return;
		
		Set<String> list = get("username");
		if(list == null){
			list = new LinkedHashSet<String>();
			put("username",list);
		}
		list.add(q+user+q);
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void removeUsername(String user){
		Set<String> list = get("username");
		if(list != null){
			list.remove(q+user+q);
			if(list.isEmpty())
				remove("username");
		}
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void addExperiment(String exp){
		// check for null
		if(TextHelper.isEmpty(exp))
			return;
		
		Set<String> list = get("experiment");
		if(list == null){
			list = new LinkedHashSet<String>();
			put("experiment",list);
		}
		list.add(q+exp+q);
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void removeExperiment(String exp){
		Set<String> list = get("experiment");
		if(list != null){
			list.remove(q+exp+q);
			if(list.isEmpty())
				remove("experiment");
		}
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void addCase(String c){
		// check for null
		if(TextHelper.isEmpty(c))
			return;
		
		Set<String> list = get("case_url");
		if(list == null){
			list = new LinkedHashSet<String>();
			put("case_url",list);
		}
		list.add(q+c+q);
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void removeCase(String c){
		Set<String> list = get("case_url");
		if(list != null){
			list.remove(q+c+q);
			if(list.isEmpty())
				remove("case_url");
		}
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void addCondition(String cond){
		// check for null
		if(TextHelper.isEmpty(cond))
			return;
		
		Set<String> list = get("experiment_condition_name");
		if(list == null){
			list = new LinkedHashSet<String>();
			put("experiment_condition_name",list);
		}
		list.add(q+cond+q);
	}
	
	/**
	 * add username to query
	 * @param user
	 */
	public void removeCondition(String cond){
		Set<String> list = get("experiment_condition_name");
		if(list != null){
			list.remove(q+cond+q);
			if(list.isEmpty())
				remove("experiment_condition_name");
		}
	}
	
	/**
	 * has some value 
	 * @param key
	 * @return
	 */
	public boolean hasValue(String key){
		return containsKey(key);
	}

	/**
	 * get value as some useful string
	 * @param key
	 * @return
	 */
	public String getValue(String key){
		Set<String> list = get(key);
		if(list != null){
			String s = ""+list;
			return "("+s.substring(1,s.length()-1)+")";
		}
		return "( )";
	}
}
