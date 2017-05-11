package edu.pitt.dbmi.tutor.model;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.Query;

/**
 * This module is responsible for tracking and saving
 * all messages in database
 * @author Eugene Tseytlin
 *
 */
public interface ProtocolModule extends TutorModule {
	/**
	 * set protocol module on/off
	 * @param b
	 */
	public void setEnabled(boolean b);
	
	

	/**
	 * is module enabled
	 * @return
	 */
	public boolean isEnabled();

	
	/**
	 * open new session for a given case
	 * @param name
	 */
	public void openCaseSession(ProblemEvent start);
	
	
	/**
	 * close session for a given case
	 */
	public void closeCaseSession(ProblemEvent end);
	
	
	/**
	 * save message in protocol. Same as receiveMessage, but
	 * assures that this operation will be done in the same thread
	 * @param msg
	 */
	public void processMessage(Message msg);

	
	/**
	 * makes sure that protocol is connected to whatever
	 * the storage mechanism is, and returns true if it is and
	 * false if it could not establesh connection
	 * @return
	 */
	public boolean isConnected();
	
	/**
	 * get all registered experiments
	 * @return
	 */
	public List<String> getExperiments();
	
	
	/**
	 * get all conditions registered w/ experiment
	 * @return
	 */
	public List<String> getConditions(String experiment);
	
	/**
	 * get all users associated with an experiment
	 * @param experiment if null, then all users
	 * @return
	 */
	public List<String> getUsers(String experiment);
	
	
	/**
	 * get a distinct set of all casses solved by a user
	 * @param experiment
	 * @return
	 */
	public List<String> getCases(Query query);
	
	
	/**
	 * get all sessions for a given set of conditions
	 * @param map where key is a condition name 
	 * and value is a list of condtions to query
	 * Ex { "username"=["guest"], "case"=["AP_12.case", "AP_11.case"]
	 * @return
	 */
	public List<Session> getSessions(Query query);
	
	
	
	/**
	 * get misc user related information
	 * @param username
	 * @return
	 */
	public Properties getUserInfo(String username);

	/**
	 * authenticate user 
	 * @param name
	 * @param password
	 * @param return true if user is authenticated, false otherwise
	 */
	public boolean authenticateUser(String username, String password);
	
	/**
	 * authenticate user 
	 * @param name
	 * @param password
	 * @param study
	 * @param return true if user is authenticated, false otherwise
	 */
	public boolean authenticateUser(String username, String password, String study);
	
	/**
	 * authenticate a user with administrative privalages
	 * @param name
	 * @param password
	 * @param return true if user is authenticated, false otherwise
	 */
	public boolean authenticateAdministrator(String username, String password);
	
	/**
	 * add new user to the protocol
	 * @param name
	 * @param password
	 * @param experiment
	 * @param student information
	 */
	public void addUser(String username, String password, String experiment, Properties p);
	
	
	/**
	 * add new experiment
	 * @param experiment
	 */
	public void addExperiment(String experiment);
	
	/**
	 * add new experiment condition
	 * @param experiment
	 */
	public void addCondition(String condition, String experiment);
	
	
	/**
	 * add new user to the protocol
	 * @param name
	 * @param password
	 * @param experiment
	 * @param student information
	 */
	public boolean removeUser(String username);
	
	
	/**
	 * add new experiment
	 * @param experiment
	 */
	public boolean removeExperiment(String experiment);
	
	/**
	 * remove experiment condition
	 * @param experiment
	 */
	public boolean removeCondition(String condition, String experiment);
	
	/**
	 * remove data for a list of given sessions
	 * @return true if successful / else otherwise
	 */
	public boolean removeSessions(List<Session> sessions);
}
