package edu.pitt.dbmi.tutor.messages;

import edu.pitt.dbmi.tutor.model.TutorModule;

/**
 * this class represents a message that corresponds to
 * meaningful user action
 * @author Administrator
 */
public class ClientEvent extends Message {

	/**
	 * create a simple client event
	 * @param type
	 * @param label
	 * @param action
	 * @return
	 */
	public static ClientEvent createClientEvent(TutorModule sender,String type, String label, String action){
		ClientEvent ce = new ClientEvent();
		ce.setType(type);
		ce.setLabel(label);
		ce.setAction(action);
		ce.setSender(sender);
		ce.setTimestamp(System.currentTimeMillis());
		return ce;
	}
}
