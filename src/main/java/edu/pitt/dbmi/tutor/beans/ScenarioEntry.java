/**
 * This class represents a set of DataEntry concepts that represent entire case
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.beans;


import static edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule.CE;
import static edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule.IE;
import static edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule.NE;
import static edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule.PE;
import static edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule.TR;

import java.util.*;
import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.FileSession;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


/**
 * scenario entry represents the feedback scenario which
 * could be encountered by a tutoring system
 * @author Administrator
 *
 */
public class ScenarioEntry  implements Serializable, Comparable<ScenarioEntry> {
	private int priority,errorCode;
	private String type,name;
	private String description;
	private List<MessageEntry> hintMessages;
	private List<MessageEntry> errorMessages;
	private List<Action> actions;
	private List<Message> stepSequence;

	
	public ScenarioEntry(){
		this(null);
	}
	
	public ScenarioEntry(String name){
		this.name = name;
		hintMessages = new ArrayList<MessageEntry>();
		errorMessages = new ArrayList<MessageEntry>();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the value of priority.
	 */
	public int getPriority(){
		return priority;
	}

	/**
	 * Sets the value of priority.
	 * @param priority The value to assign priority.
	 */
	public void setPriority(int priority){
		this.priority = priority;
	}

	/**
	 * @return the actions
	 */
	public List<Action> getActions() {
		if(actions == null)
			actions = new ArrayList<Action>();
		return actions;
	}


	/**
	 * @param actions the actions to set
	 */
	public void setActions(List<Action> actions) {
		this.actions = actions;
	}


	/**
	 * Returns the value of type.
	 */
	public String getType(){
		return type;
	}

	/**
	 * Sets the value of type.
	 * @param type The value to assign type.
	 */
	public void setType(String type){
		this.type = type;
	}
	
	/**
	 * Returns the value of description.
	 */
	public String getDescription()	{
		return description;
	}

	/**
	 * Sets the value of description.
	 * @param description The value to assign description.
	 */
	public void setDescription(String description)	{
		this.description = description;
	}

	/**
	 * Returns the value of hint_messages.
	 */
	public List<MessageEntry> getHintMessages(){
		return hintMessages;
	}

	/**
	 * Sets the value of hint_messages.
	 * @param hint_messages The value to assign hint_messages.
	 */
	public void setHintMessages(List<MessageEntry> hint_messages){
		this.hintMessages = hint_messages;
	}

	/**
	 * Returns the value of bug_messages.
	 */
	public List<MessageEntry> getErrorMessages(){
		// set the error flag if not set from before
		if(!errorMessages.isEmpty() && !errorMessages.get(0).isError()){
			for(MessageEntry e: errorMessages)
				e.setError(true);
		}
		return errorMessages;
	}

	/**
	 * Sets the value of bug_messages.
	 * @param bug_messages The value to assign bug_messages.
	 */
	public void setErrorMessages(List<MessageEntry> bug_messages){
		this.errorMessages = bug_messages;
	}
	
	
	/**
	 * more likely then not, there is only one error message
	 * for each scenario, lets just get that
	 * @return
	 */
	public MessageEntry getErrorMessage(){
		return errorMessages.isEmpty()?null:errorMessages.get(0);
	}

	/**
	 * Returns the value of tutor_actions.
	 *
	public List getTutorActions(){
		return tutor_actions;
	}
	*/
	/**
	 * Sets the value of tutor_actions.
	 * @param tutor_actions The value to assign tutor_actions.
	 *
	public void setTutorActions(List tutor_actions){
		this.tutor_actions = tutor_actions;
	}
	*/
	
	
	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}



	
	/**
	 * create element that represents this document
	 */
	public Element createElement(Document doc){
		Element root = doc.createElement("ScenarioEntry");
		root.setAttribute("name",name);
		root.setAttribute("type",type);
		if(getErrorCode() != 0)
			root.setAttribute("code",""+getErrorCode());
		if(getPriority() != 0)
			root.setAttribute("priority",""+getPriority());
		
		
		// add description
		Element desc = doc.createElement("Description");
		root.appendChild(desc);
		desc.setTextContent(description);
		
		
		// add error messages
		Element errors = doc.createElement("ErrorMessages");
		root.appendChild(errors);
		
		// write out messages
		for(MessageEntry e: getErrorMessages()){
			errors.appendChild(e.createElement(doc));
		}
		
		// add hint messages
		Element hints = doc.createElement("HintMessages");
		root.appendChild(hints);
		
		// write out messages
		for(MessageEntry e: getHintMessages()){
			hints.appendChild(e.createElement(doc));
		}
				
		// iterate through actions
		for(Action action: getActions()){
			root.appendChild(action.createElement(doc));
		}
		
		// add hint messages
		Element steps = doc.createElement("StepSequence");
		root.appendChild(steps);
		// write out messages
		for(Message e: getStepSequence()){
			Element s = doc.createElement("Step");
			steps.appendChild(s);
			s.setTextContent(toString(e));
		}
		
		
		return root;
	}
	
	/**
	 * parse XML element
	 * @param element
	 */
	public void parseElement(Element element){
		// set attributes
		setName(element.getAttribute("name"));
		setType(element.getAttribute("type"));
		if(!TextHelper.isEmpty(element.getAttribute("code")))
			setErrorCode(Integer.parseInt(element.getAttribute("code")));
		if(!TextHelper.isEmpty(element.getAttribute("priority")))
			setPriority(Integer.parseInt(element.getAttribute("priority")));
		
		// set description
		Element d = UIHelper.getElementByTagName(element,"Description");
		if(d != null){
			setDescription(d.getTextContent().trim());
		}
		
		// parse errors
		parseMessageList(element,"ErrorMessages",errorMessages);
		
		// parse hints
		parseMessageList(element,"HintMessages",hintMessages);
	
		// parse steps
		// parse hints
		parseStepList(element,"StepSequence",getStepSequence());
		
		
		// parse actions
		//NodeList list = element.getElementsByTagName("Action");
		NodeList list = element.getChildNodes();
		List<Action> alist = new ArrayList<Action>();
		for(int i=0;i<list.getLength();i++){
			if(list.item(i) instanceof Element){
				Element a = (Element) list.item(i);
				if("Action".equals(a.getTagName())){
					Action ac = new Action();
					ac.parseElement(a);
					alist.add(ac);
				}
			}
		}
		setActions(alist);
	}
	

	
	/**
	 * parse message list
	 * @param tag
	 * @param msgList
	 */
	private void parseMessageList(Element element,String tag, List<MessageEntry> msgList){
		Element msg = UIHelper.getElementByTagName(element, tag);
		if(msg != null){
			// iterate over message entries
			NodeList list = msg.getElementsByTagName("MessageEntry");
			for(int j=0;j<list.getLength();j++){
				Node n = list.item(j);
				if(n instanceof Element){
					MessageEntry entry = new MessageEntry();
					entry.parseElement((Element)n);
					msgList.add(entry);
				}
			}
		}
	}
	
	/**
	 * parse message list
	 * @param tag
	 * @param msgList
	 */
	private void parseStepList(Element element,String tag, List<Message> msgList){
		Element msg = UIHelper.getElementByTagName(element, tag);
		if(msg != null){
			// iterate over message entries
			NodeList list = msg.getElementsByTagName("Step");
			for(int j=0;j<list.getLength();j++){
				Node n = list.item(j);
				if(n instanceof Element){
					Message st = parseStep((n.getTextContent().trim()));
					if(st != null)
						msgList.add(st);
				}
			}
		}
	}
	
	/**
	 * parse step
	 * @param line
	 * @return
	 */
	private Message parseStep(String line) {
		Message msg = null;
		Map<String,String> map = FileSession.parseMessage(line.substring(CE.length()).trim());
		
		// now parse node event
		if(line.startsWith(PE)){
			msg = new ProblemEvent();
			msg.putAll(map);
		}else if(line.startsWith(IE)){
			msg = new InterfaceEvent();
			for(String key: map.keySet()){
				if(FileSession.messageFields.contains(key)){
					msg.put(key,map.get(key));
				}else if("input".equalsIgnoreCase(key)){
					msg.setInput(map.get(key));
				}else{
					msg.getInputMap().put(key,map.get(key));
				}
			}
		}else if(line.startsWith(CE)){
			msg = new ClientEvent();
			for(String key: map.keySet()){
				if(FileSession.messageFields.contains(key)){
					msg.put(key,map.get(key));
				}else if("input".equalsIgnoreCase(key)){
					msg.setInput(map.get(key));
				}else{
					msg.getInputMap().put(key,map.get(key));
				}
			}
		}else if(line.startsWith(TR)){
			msg = new TutorResponse();
			for(String key: map.keySet()){
				if(FileSession.tutorResponseFields.contains(key)){
					msg.put(key,map.get(key));
				}else if("input".equalsIgnoreCase(key)){
					msg.setInput(map.get(key));
				}else{
					msg.getInputMap().put(key,map.get(key));
				}
			}
			if(map.containsKey("client_event_id"))
				((TutorResponse)msg).setClientEventId(Integer.parseInt(map.get("client_event_id")));
		}else if(line.startsWith(NE)){
			msg = new NodeEvent();
			msg.putAll(map);
			if(map.containsKey("tutor_response_id"))
				((NodeEvent)msg).setTutorResponseId(Integer.parseInt(map.get("tutor_response_id")));
			
		}
		// set id
		if(map.containsKey("message_id"))
			msg.setMessageId(Integer.parseInt(map.get("message_id")));
		return msg;
	}
	
	/**
	 * convert message to string
	 * @param msg
	 * @return
	 */
	private String toString(Message msg){
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
		return prefix+" "+msg;
	}
	
	
	// take a look at it
	public String toString(){
		//return type+"-"+code+":\n"+errorMessages+"\n"+hintMessages;
		if(TextHelper.isEmpty(getType()))
			return getName();
		return getType()+": "+getName();
	}


	/**
	 * get a sequence of steps (ClientEvents) to recreate this example scenario
	 * @return
	 */
	public List<Message> getStepSequence() {
		if(stepSequence == null)
			stepSequence = new ArrayList<Message>();
		return stepSequence;
	}
	
	/**
	 * set a sequence of steps (ClientEvents) to recreate this example scenario
	 * @return
	 */
	public void setStepSequence(List<Message> stepSequence) {
		this.stepSequence = stepSequence;
	}


	

	public int compareTo(ScenarioEntry o) {
		if(TextHelper.isEmpty(getType()))
			return 1;
		else if(TextHelper.isEmpty(o.getType()))
			return -1;
		return toString().compareTo(o.toString());
	}
}
