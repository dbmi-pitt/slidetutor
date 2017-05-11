package edu.pitt.dbmi.tutor.messages;

import edu.pitt.dbmi.tutor.util.TextHelper;

/**
 * this message represents NodeEvent
 * @author tseytlin
 */
public class NodeEvent extends TutorResponse {
	private int tutorResponseId;
	private transient TutorResponse tutorResponse;
	
	public int getTutorResponseId() {
		return (tutorResponse != null)?tutorResponse.getMessageId():tutorResponseId;
	}

	public void setTutorResponseId(int tutorResponseId) {
		this.tutorResponseId = tutorResponseId;
	}

	public TutorResponse getTutorResponse() {
		return tutorResponse;
	}

	public ClientEvent getClientEvent() {
		if(tutorResponse != null)
			return tutorResponse.getClientEvent();
		return null;
	}
	
	public void setTutorResponse(TutorResponse tutorResponse) {
		this.tutorResponse = tutorResponse;
	}
	
	
	public boolean isAbsent() {
		return "true".equalsIgnoreCase(get("is_absent"));
	}

	public void setAbsent(boolean absent) {
		put("is_absent",""+absent);
	}

	public String getOneToMany() {
		return get("one_to_many");
	}

	public void setOneToMany(Object oneToMany) {
		put("one_to_many",TextHelper.toString(oneToMany));
	}

	public String getManyToMany() {
		return get("many_to_many");
	}

	public void setManyToMany(Object manyToMany) {
		put("many_to_many",TextHelper.toString(manyToMany));
	}
	
	
	/**
	 * get a list of message fields
	 * @return
	 */
	public static String [] getMessageFields(){
		return new String [] {"type","label","action","parent","response","error","code","timestamp","is_absent","one_to_many","many_to_many",};
	}
	
	

	public String toString() {
		return super.toString()+" (tutor_response_id \""+getTutorResponseId()+"\") ";
	}
	
}
