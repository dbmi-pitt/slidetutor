package edu.pitt.dbmi.tutor.messages;

import java.util.Date;
import java.util.List;


/**
 * this object represents user protocol session
 * @author tseytlin
 *
 */
public interface Session {
	/**
	 * unique id of this session
	 * @return
	 */
	public String getSessionID();
	
	/**
	 * get username of a person who did this session
	 * @return
	 */
	public String getUsername();
	
	
	/**
	 * get experiment to which this user belongs to
	 * @return
	 */
	public String getExperiment();
	
	/**
	 * get condition under which this session was done
	 * @return
	 */
	public String getCondition();
	
	/**
	 * get case url that was solved during this session
	 * @return
	 */
	public String getCase();
	
		
	/**
	 * get domain url which this case belonged to
	 * @return
	 */
	public String getDomain();
	
	/**
	 * get the path of confiuration file used for this session
	 * @return
	 */
	public String getConfiguration();
	
	/**
	 * get start time of this session
	 * @return
	 */
	public Date getStartTime();
	
	/**
	 * get finish time of this session
	 * @return
	 */
	public Date getFinishTime();
	
	/**
	 * what is the outcome of this session
	 * finished/solved/closed etc...
	 * @return
	 */
	public String getOutcome();
	
	
	/**
	 * get all interface events associated with this session
	 * @return
	 */
	public List<InterfaceEvent> getInterfaceEvents();
	
	/**
	 * get all client events associated with this session
	 * @return
	 */
	public List<ClientEvent> getClientEvents();
	
	/**
	 * get all tutor responses associated with this session
	 * @return
	 */
	public List<TutorResponse> getTutorResponses();
	
	
	/**
	 * get all node events associated with this session
	 * @return
	 */
	public List<NodeEvent> getNodeEvents();
	
	/**
	 * get all interface events associated with a given interface event
	 * @return
	 */
	public List<InterfaceEvent> getInterfaceEvents(ClientEvent e);
	
	/**
	 * get all tutor responses associated with given client event
	 * @return
	 */
	public List<TutorResponse> getTutorResponses(ClientEvent e);

	/**
	 * get all node events associated with given tutor response
	 * @return
	 */
	public List<NodeEvent> getNodeEvents(TutorResponse e);
	
	/**
	 * get two problem event first represents start, second finish
	 * @return
	 */
	public List<ProblemEvent> getProblemEvents();
	
	/**
	 * refresh session
	 */
	public void refresh();
	
	/**
	 * delete this session from the protocol
	 * @return true on success
	 */
	public boolean delete();
}
