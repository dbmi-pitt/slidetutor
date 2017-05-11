package edu.pitt.dbmi.tutor.messages;

import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class ProblemEvent extends Message {
	public String getUsername(){
		return get("username");
	}
	
	public String getCondition(){
		return get("condition");
	}
	
	public String getCaseURL(){
		return get("case_url");
	}
	
	public String getDomainURL(){
		return get("domain_url");
	}
	
	public String getConfigURL(){
		return get("config_url");
	}
	
	public String getOutcome(){
		return get("outcome");
	}
	
	/**
	 * create a simple client event
	 * @param problem url
	 * @return problem event
	 */
	public static ProblemEvent createStartProblemEvent(String problem){
		ProblemEvent ce = new ProblemEvent();
		ce.setType(Constants.TYPE_START);
		ce.put("case_url",problem);
		ce.put("username",Config.getUsername());
		ce.put("condition",Config.getCondition());
		ce.put("config_url",Config.getConfigLocation());
		ce.put("domain_url",OntologyHelper.getDomainFromCase(problem));
		ce.setTimestamp(System.currentTimeMillis());
		return ce;
	}
	
	/**
	 * create a simple client event
	 * @param type
	 * @param label
	 * @param action
	 * @return
	 */
	public static ProblemEvent createEndProblemEvent(String outcome){
		ProblemEvent ce = new ProblemEvent();
		ce.setType(Constants.TYPE_END);
		ce.put("outcome",outcome);
		ce.put("username",Config.getUsername());
		ce.put("condition",Config.getCondition());
		ce.put("config_url",Config.getConfigLocation());
		ce.setTimestamp(System.currentTimeMillis());
		return ce;
	}
	
	/**
	 * get a list of message fields
	 * @return
	 */
	public static String [] getMessageFields(){
		return new String [] {"type","username","condition","case_url","domain_url","config_url","outcome","timestamp"};
	}
}
