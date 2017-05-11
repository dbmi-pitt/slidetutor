package edu.pitt.dbmi.tutor.model;

import java.util.Properties;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.Message;


/**
 * This class describes a generic tutor module.
 * @author Eugene Tseytlin
 */
public interface TutorModule {
	
	/**
	 * get name of this module
	 * @return
	 */
	public String getName();
	
	/**
	 * get description of this module
	 * @return
	 */
	public String getDescription();
	
	/**
	 * get version of this module
	 * @return
	 */
	public String getVersion();
	
	
	/**
	 * get default properties that are available for this
	 * module
	 * @return
	 */
	public Properties getDefaultConfiguration();
	
	/**
	 * receive generic message
	 * @return
	 */
	public void receiveMessage(Message msg);
	
	
	/**
	 * resolve an arbitrary action
	 * if action is understood, the module will
	 * "resolve" it, by assigning runnable code
	 * to it, for later execution
	 * @param action
	 */
	public void resolveAction(Action action);
	
	
	/**
	 * release any resources before disposing of resource
	 */
	public void dispose();
	
	
	/**
	 * reset a state of the tutor module
	 */
	public void reset();
	
	
	/**
	 * get all messages that this module supports
	 * @return
	 */
	public Message [] getSupportedMessages();
	
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions();
	
	/**
	 * load resources associated w/ this module
	 */
	public void load();
}
