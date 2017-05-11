package edu.pitt.dbmi.tutor.beans;

import java.util.HashMap;
import java.util.Map;

import edu.pitt.dbmi.tutor.messages.*;

/**
 * this object acts as a container for various 
 * measurable statistics related to a tutor session
 * @author tseytlin
 *
 */
public class SessionStatistics {
	public long totalTime,startTime,currentTime, finishTime;
	public int userActionCount, caseCount;
	public ClientEvent lastClientEvent;
	public TutorResponse lastTutorResponse;
	public Map<String,Integer> userTypeCount = new HashMap<String,Integer>();
	
	public void clear(){
		startTime = 0;
	    currentTime = 0;
	    finishTime  = 0;
	    userActionCount = 0;
	    userTypeCount.clear();
	    lastClientEvent = null;
	    lastTutorResponse = null;
	}
}
