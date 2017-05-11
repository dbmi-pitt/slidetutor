package edu.pitt.dbmi.tutor.messages;


/**
 * this class corresponds to tutor response
 * to a client event
 * @author Administrator
 */
public class TutorResponse extends Message {
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
	 * that this is response for
	 * @param clientEventId
	 */
	public void setClientEventId(int clientEventId) {
		this.clientEventId = clientEventId;
	}
	
	/**
	 * get tutor response
	 * @return
	 */
	public String getResponse(){
		return get("response");
	}
	
	/**
	 * set tutor response
	 * @return
	 */
	public void setResponse(String r){
		put("response",r);
	}
	
	/**
	 * get error code associated with response
	 * @return
	 */
	public String getError(){
		return get("error");
	}
	
	/**
	 * get error code associated with response
	 * @return
	 */
	public void setError(String e){
		put("error",e);
	}
	
	/**
	 * get error code associated with response
	 * @return
	 */
	public String getCode(){
		return get("code");
	}
	
	/**
	 * get error code associated with response
	 * @return
	 */
	public void setCode(String e){
		put("code",e);
	}
	
	/**
	 * get object description of the response
	 * Ex: name of finding that is referred to by misc parts of a concept
	 * @return
	 */
	public String getResponseConcept(){
		// if response concept is not set, then use next step
		if(!containsKey("response_concept"))
			return getObjectDescription();
		return get("response_concept");
	}
	
	
	/**
	 * set object description of the response
	 * Ex: name of finding that is referred to by misc parts of a concept
	 * @return
	 */
	public void setResponseConcept(String name){
		if(name != null)
			put("response_concept",name);
	}
	
	/**
	 * get a list of message fields
	 * @return
	 */
	public static String [] getMessageFields(){
		/*
		final String [] mFields = Message.getMessageFields();
		final String [] fields = new String [4+mFields.length];
		int i=0;
		fields[i++] = "response";
		fields[i++] = "error";
		fields[i++] = "code";
		fields[i++] = "response_concept";
		for(String f: mFields)
			fields[i++] = f;
		return fields;
		*/
		return new String [] {"response","error","code","response_concept","type","label","action","parent","id","timestamp","source"};
	}
	
	public String toString() {
		return super.toString()+" (client_event_id \""+getClientEventId()+"\") ";
	}
	
}
