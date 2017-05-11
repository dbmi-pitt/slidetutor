package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.*;
import java.util.*;

import edu.pitt.dbmi.tutor.util.TextHelper;


/**
 * this class reads a list of exclude client events to be used for filtering
 * @author tseytlin
 */
public class Exclusions{
	private final String FIELD_SEPARATOR = "\\|";
	private Map<Integer,String> clientEventMap;
	private Set<String> excludedSessions;
	
	
	public Exclusions(){
		clientEventMap = new HashMap<Integer, String>();
	}
	
	public void loadClientEventList(File f){
		parse(f);
	}
	
	private void parse(File f)  {
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader(f));
			for(String line = reader.readLine();line != null; line = reader.readLine()){
				String [] p = line.split(FIELD_SEPARATOR);
				if(p.length > 4 && !TextHelper.isEmpty(p[4])){
					try {
						String status = p[0];
						int id = Integer.parseInt(p[4]);
					
						if(!TextHelper.isEmpty(status)){
							clientEventMap.put(id,status);
						}
					}catch(NumberFormatException ex){
						//NOOP
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * check 
	 * @param id
	 * @return
	 */
	public boolean isUselessHint(int id){
		String s = clientEventMap.get(id);
		return "useless".equals(s);
	}
	
	/**
	 * check 
	 * @param id
	 * @return
	 */
	public boolean isMisleadingHint(int id){
		String s = clientEventMap.get(id);
		return "misleading".equals(s);
	}
	
	/**
	 * check 
	 * @param id
	 * @return
	 */
	public boolean isRemovedClientEvent(int id){
		String s = clientEventMap.get(id);
		return "remove".equals(s);
	}
	
	/**
	 * check 
	 * @param id
	 * @return
	 */
	public boolean isChangeTrueDoneClientEvent(int id){
		String s = clientEventMap.get(id);
		return "change to true done".equals(s);
	}
	
	
	/**
	 * get a set of client event states
	 * @return
	 */
	public Set<String> getClientEventStates(){
		return new HashSet(clientEventMap.values());
	}
	
	
	
	/**
	 * is session id is in exclude list
	 * @param id
	 * @return
	 */
	public boolean isExcludedSession(String id){
		return getExclusionList().contains(id);
	}
	
	/**
	 * get exclusion list
	 * @return
	 */
	private Set<String> getExclusionList(){
		if(excludedSessions == null){
			excludedSessions = new LinkedHashSet<String>();
			excludedSessions.add("1670");
			excludedSessions.add("1679");
			excludedSessions.add("1705");
			excludedSessions.add("2097");
			excludedSessions.add("2060");
			excludedSessions.add("2076");
			excludedSessions.add("2345");
			excludedSessions.add("2370");
			excludedSessions.add("2351");
			excludedSessions.add("2362");
			excludedSessions.add("2375");
			excludedSessions.add("2341");
			excludedSessions.add("2343");
			excludedSessions.add("2390");
			excludedSessions.add("2399");
			excludedSessions.add("2344");
			excludedSessions.add("2355");
			excludedSessions.add("1680");
			excludedSessions.add("1688");
			excludedSessions.add("1722");
			excludedSessions.add("2603");
			excludedSessions.add("2592");
			excludedSessions.add("2604");
			excludedSessions.add("2581");
			excludedSessions.add("2584");
			excludedSessions.add("2612");
			excludedSessions.add("2806");
			excludedSessions.add("2813");
			excludedSessions.add("2812");
			excludedSessions.add("2836");
			excludedSessions.add("2850");
			excludedSessions.add("3100");
			excludedSessions.add("3104");
			excludedSessions.add("3107");
			excludedSessions.add("3123");
			excludedSessions.add("1684");
			excludedSessions.add("3125");
			excludedSessions.add("3102");
			excludedSessions.add("3101");
			excludedSessions.add("3106");
			excludedSessions.add("3333");
			excludedSessions.add("3332");
			excludedSessions.add("3344");
			excludedSessions.add("3334");
			excludedSessions.add("3328");
			excludedSessions.add("3336");
			excludedSessions.add("3549");
			excludedSessions.add("3556");
			excludedSessions.add("3564");
			excludedSessions.add("3565");
			excludedSessions.add("3589");
			excludedSessions.add("3825");
			excludedSessions.add("3833");
			excludedSessions.add("3827");
			excludedSessions.add("3824");
			excludedSessions.add("3823");
			excludedSessions.add("3855");
			excludedSessions.add("4095");
			excludedSessions.add("4095");
			excludedSessions.add("4113");
			excludedSessions.add("1682");
			excludedSessions.add("4088");
			excludedSessions.add("4096");
			excludedSessions.add("4123");
			excludedSessions.add("4081");
			excludedSessions.add("4090");
			excludedSessions.add("4080");
			excludedSessions.add("4092");
			excludedSessions.add("4082");
			excludedSessions.add("4094");
			excludedSessions.add("4152");
			excludedSessions.add("4403");
			excludedSessions.add("4385");
			excludedSessions.add("4393");
			excludedSessions.add("4412");
			excludedSessions.add("4389");
			excludedSessions.add("4397");
			excludedSessions.add("1683");
			excludedSessions.add("4632");
			excludedSessions.add("1900");
			excludedSessions.add("1899");
			excludedSessions.add("1899");
			excludedSessions.add("1905");
			excludedSessions.add("1912");
			excludedSessions.add("1936");
			excludedSessions.add("2847");

		}
		
		return excludedSessions;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Exclusions ece = new Exclusions();
		ece.loadClientEventList(new File("/home/tseytlin/Download/H5 Problem Session Client Events.csv"));
		System.out.println(ece.getClientEventStates());
	}

}
