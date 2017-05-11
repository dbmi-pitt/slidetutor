package edu.pitt.dbmi.tutor.messages;

import java.util.*;

import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.util.TextHelper;

/**
 * this class represents a message that
 * is being used for all communications
 * @author Eugene Tseytlin
 */
public class Message extends LinkedHashMap<String,String>{
	private Object input;
	private transient TutorModule sender;
	private int messageId;
	private static int messageCount;
	
	public Message(){
		super();
		messageId = createMessageId();
	}
	
	/**
	 * create unique message id for this java session
	 * @return
	 */
	
	public static int createMessageId(){
		synchronized (Message.class) {
			return (messageCount++);
		}
	}
	
	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}


	public TutorModule getSender() {
		return sender;
	}

	public void setSender(TutorModule sender) {
		this.sender = sender;
		if(sender != null)
			setSource(sender.getClass().getSimpleName());
	}

	/**
	 * Get message type. Type of tutor event.
	 * @return
	 */
	public String getType() {
		return get("type");
	}

	/**
	 * Set message type. Type of tutor event.
	 * @return
	 */
	public void setType(String type) {
		if(type != null)
			put("type",type);
	}

	/**
	 * Get message label. Human readable label of tutor event.
	 * @return
	 */
	public String getLabel() {
		return get("label");
	}

	/**
	 * Set message label. Human readable label of tutor event.
	 * @return
	 */
	public void setLabel(String label) {
		if(label != null)
			put("label",label);
	}

	/**
	 * Get message id. Unique id of tutor event.
	 * @return
	 */
	public String getId() {
		return get("id");
	}

	
	/**
	 * Set message id. Unique id of tutor event.
	 * @return
	 */

	public void setId(String id) {
		if(id != null)
			put("id",id);
	}

	
	/**
	 * Get action. An action that was performed by tutor event.
	 * @return
	 */
	public String getAction() {
		return get("action");
	}

	/**
	 * Set action. An action that was performed by tutor event.
	 * @return
	 */
	public void setAction(String action) {
		if(action != null)
			put("action",action);
	}

	
	/**
	 * Get input. A set of input parameters for tutor event;
	 * @return
	 */
	public Object getInput() {
		if(input == null){
			input = new LinkedHashMap<String,String>();
		}
		return input;
	}

	
	/**
	 * Set input. A set of input parameters for tutor event;
	 * @return
	 */
	public void setInput(Object input) {
		this.input = input;
	}

	/**
	 * Get parent. A parent id for this tutor event;
	 * @return
	 */
	public String getParent() {
		return get("parent");
	}

	
	/**
	 * Set parent. A parent id for this tutor event;
	 * @return
	 */
	public void setParent(String parent) {
		if(parent != null){
			put("parent",parent);
			
			// reset object description, if parent is available
			//if(getObjectDescription() != null)
			//	setObjectDescription(getParent()+":"+getObjectDescription());
		}
	}
	
	/**
	 * get timestamp, (number of millis since epoch)
	 * @return
	 */
	public long getTimestamp(){
		long time = 0;
		try{
			time = Long.parseLong(get("timestamp"));
		}catch(Exception ex){
			//ex.printStackTrace();
			time = System.currentTimeMillis();
		}
		return time;
	}
	
	/**
	 * get timestamp, (number of millis since epoch)
	 * @return
	 */
	public void setTimestamp(long time){
		put("timestamp",""+time);
	}
	
	
	
	/**
	 * create string representation of this message
	 */
	public String toString(){
		StringBuffer buf = new StringBuffer();
		
		// add normal fields
		for(String key: keySet())
			buf.append("("+key+" \""+get(key)+"\") ");
		
		// add input
		buf.append(getInputString());
		
		// add message id
		buf.append("(message_id \""+getMessageId()+"\") ");
		
		return buf.toString();
	}
	
	/**
	 * get input as string
	 * @return
	 */
	public String getInputString(){
		StringBuffer buf = new StringBuffer();
		if(input != null){
			if(input instanceof Map){
				for(Object key: ((Map)input).keySet())
					buf.append("("+key+" \""+((Map)input).get(key)+"\") ");
			}else
				buf.append("(input \""+input+"\") ");
		}
		return buf.toString();
	}
	
	public Map<String,String> getInputMap(){
		if(input != null && !(input instanceof Map)){
			Map<String,String> map = new LinkedHashMap<String, String>();
			map.put("input",input.toString());
			setInput(map);
		}
		return (Map)getInput();
	}
	
	
	public void addInput(String key, String value){
		if(key != null && value != null)
			getInputMap().put(key,value);
	}
	
	/**
	 * get source of this message 
	 * @return
	 */
	public String getSource(){
		// if source set, use that
		if(containsKey("source"))
			return get("source");
		// true source is the sender
		if(sender != null)
			return sender.getClass().getSimpleName();
		return null;
	}
	
	/**
	 * set source of this message 
	 * @return
	 */
	public void setSource(String src){
		if(src != null)
			put("source",src);
	}
	
	/**
	 * get object description
	 * @return
	 */
	public String getObjectDescription(){
		String s = get("object_description");
		if(TextHelper.isEmpty(s)){
			s = getType()+"."+getLabel()+((getId() != null)?"."+getId():"");
			if(!TextHelper.isEmpty(getParent()))
				s = getParent()+":"+s;
		}
		return s;
	}
	
	/**
	 * set object description
	 * @return
	 */
	public void setObjectDescription(String desc){
		if(desc != null)
			put("object_description",desc);
	}
	
	
	/**
	 * get parent concept name that maybe referred to by this message
	 * Ex: name of finding that is referred to by misc parts of a concept
	 * @return
	 */
	public String getEntireConcept(){
		return get("entire_concept");
	}
	
	
	/**
	 * set parent concept name that maybe referred to by this message
	 * Ex: name of finding that is referred to by misc parts of a concept
	 * @return
	 */
	public void setEntireConcept(String name){
		if(name != null)
			put("entire_concept",name);
	}
	
	
	/**
	 * don't put nulls please
	 */
	public String put(String key, String value){
		if(key != null && value != null)
			return super.put(key,value);
		return null;
	}
	
	
	/**
	 * get a list of message fields
	 * @return
	 */
	public static String [] getMessageFields(){
		return new String [] {"type","label","action","parent","id","timestamp","object_description","entire_concept","source"};
	}
	
	
	/**
	 * compare messages based on timestamps
	 * @author tseytlin
	 */
	public static class TimeComparator implements Comparator<Message> {
		public int compare(Message o1, Message o2) {
			Date d1 = new Date(o1.getTimestamp());
			Date d2 = new Date(o2.getTimestamp());
			if(d1.before(d2))
				return -1;
			if(d1.after(d2))
				return 1;
			
			// if timestamps are equals, then client event always wins
			if(o1 instanceof ClientEvent && !(o2 instanceof ClientEvent))
				return -1;
			else if(o2 instanceof ClientEvent && !(o1 instanceof ClientEvent))
				return 1;
			
			// else plain equal
			return 0;
		}
	}
	
	
	public boolean isAuto(){
		if(input != null && input instanceof Map){
			Map m = (Map) input;
			if(m.containsKey("auto"))
				return Boolean.parseBoolean(""+m.get("auto"));
		}
		return false;
	}
}
