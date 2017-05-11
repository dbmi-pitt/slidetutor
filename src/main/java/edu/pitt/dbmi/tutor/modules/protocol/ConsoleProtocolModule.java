package edu.pitt.dbmi.tutor.modules.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;

/**
 * this module simple outputs all messages
 * @author Eugene Tseytlin
 *
 */
public class ConsoleProtocolModule implements ProtocolModule {
	private boolean protocol = true;
	private Message lastMessage;
	private Properties defaultConfig;
	private boolean showIE, showViewer;
	
	public void load(){
		showIE = Config.getBooleanProperty(this,"show.interface.events");
		showViewer = Config.getBooleanProperty(this,"show.presentation.events");
	}
	
	
	public void receiveMessage(Message msg) {
		if(!isEnabled())
			return;
		
		// skip interface events
		if((!showIE && msg instanceof InterfaceEvent) || (!showViewer && Constants.TYPE_PRESENTATION.equals(msg.getType())))
			return;
		
		// skip responses to viewer events
		if(msg instanceof TutorResponse && ((TutorResponse)msg).getResponseConcept().startsWith(Constants.TYPE_PRESENTATION))
			return;
		
		// do newline on client events
		if(msg instanceof ClientEvent && lastMessage!= null && lastMessage instanceof TutorResponse)
			System.out.println("");
		
		// print message content
		String prefix = "";
		if(msg instanceof ClientEvent){
			prefix = FileProtocolModule.CE;
		}else if(msg instanceof NodeEvent){
			prefix = FileProtocolModule.NE;
		}else if(msg instanceof TutorResponse){
			prefix = FileProtocolModule.TR;
		}else if(msg instanceof InterfaceEvent){
			prefix = FileProtocolModule.IE;
		}else if(msg instanceof ProblemEvent){
			prefix = FileProtocolModule.PE;
		}
		System.out.println(prefix+" "+msg);
		
		lastMessage = msg;
	}
	/**
	 * set protocol module on/off
	 * @param b
	 */
	public void setEnabled(boolean b){
		protocol = b;
	}
	
		/**
	 * is module enabled
	 * @return
	 */
	public boolean isEnabled(){
		return protocol;
	}

	public void dispose() {	}

	/**
	 * get default configuration
	 */
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Logs some of the user's interactions with the tutor to the Java console";
	}

	public String getName() {
		return "Console Protocol Module";
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}
	public void reset() {}

	public void resolveAction(Action action) {}
	public void closeCaseSession(ProblemEvent msg) {
		receiveMessage(msg);
	}
	public List<String> getConditions(String experiment) {
		return Collections.EMPTY_LIST;
	}
	public List<String> getExperiments() {
		return Collections.EMPTY_LIST;
	}
	public List<Session> getSessions(Query query) {
		return Collections.EMPTY_LIST;
	}
	public List<Session> getSessions(String username) {
		return Collections.EMPTY_LIST;
	}
	public List<String> getUsers(String experiment) {
		return Collections.EMPTY_LIST;
	}
	public void openCaseSession(ProblemEvent msg) {
		// write out message
		receiveMessage(msg);
	}
	public void addCondition(String condition, String experiment) {
	}
	public void addExperiment(String experiment) {
	}
	public void addUser(String username, String password, String experiment, Properties p) {
	}
	public boolean authenticateUser(String username, String password) {
		return true;
	}
	public boolean authenticateAdministrator(String username, String password) {
		return true;
	}
	public Properties getUserInfo(String username) {
		return new Properties();
	}
	public List<String> getCases(Query query) {
		return Collections.EMPTY_LIST;
	}
	public boolean authenticateUser(String username, String password, String study) {
		return true;
	}
	public boolean removeCondition(String condition, String experiment) {
		return false;
	}
	public boolean removeExperiment(String experiment) {
		return false;
	}
	public boolean removeUser(String username) {
		return false;
	}
	public boolean isConnected() {
		return true;
	}
	public void processMessage(Message msg) {
		receiveMessage(msg);
	}
	public boolean removeSessions(List<Session> sessions){
		return false;
	}
}
