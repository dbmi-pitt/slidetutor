package edu.pitt.dbmi.tutor.messages;

import edu.pitt.dbmi.tutor.model.TutorModule;

/**
 * this class represents a message that corresponds to
 * a user interface action that may lead to ClientEvent
 * @author tseytlin
 *
 */
public class InterfaceEvent extends Message {
	private int clientEventId;
	private transient ClientEvent clientEvent;
	
	/**
	 * get client event associated with it
	 * @return
	 */
	public ClientEvent getClientEvent() {
		return clientEvent;
	}
	
	
	/**
	 * set client event associated with it
	 * @param clientEvent
	 */
	public void setClientEvent(ClientEvent clientEvent) {
		this.clientEvent = clientEvent;
	}

	/**
	 * get client event id of the event
	 * that this interface event caused
	 * @return
	 */
	public int getClientEventId() {
		return (clientEvent != null)?clientEvent.getMessageId():clientEventId;
	}

	/**
	 * set client event id of the event
	 * that this is interface event caused
	 * @param clientEventId
	 */
	public void setClientEventId(int clientEventId) {
		this.clientEventId = clientEventId;
	}
	
	/**
	 * create a simple client event
	 * @param type
	 * @param label
	 * @param action
	 * @return
	 */
	public static InterfaceEvent createInterfaceEvent(TutorModule sender,String type, String label, String action){
		InterfaceEvent ce = new InterfaceEvent();
		ce.setType(type);
		ce.setLabel(label);
		ce.setAction(action);
		ce.setSender(sender);
		ce.setTimestamp(System.currentTimeMillis());
		return ce;
	}


	public String toString() {
		return super.toString()+" (client_event_id \""+getClientEventId()+"\") ";
	}
	
	
	
}
