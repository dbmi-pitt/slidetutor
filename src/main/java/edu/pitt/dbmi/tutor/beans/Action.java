package edu.pitt.dbmi.tutor.beans;


import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * action describes an action that can be performed 
 * @author tseytlin
 *
 */
public class Action {
	private String receiver;
	private String action;
	private String input;
	private String description;
	private transient Operation operation;
	private transient ConceptEntry concept;
	
	
	/**
	 * create new action
	 */
	public Action(){}
	
	/**
	 * create new action
	 * @param r
	 * @param a
	 * @param i
	 */
	public Action(String r, String a, String i){
		receiver = r;
		action = a;
		input = i;
	}
	
	/**
	 * create new action
	 * @param r
	 * @param a
	 * @param i
	 */
	public Action(String r, String a, String i,String desc){
		receiver = r;
		action = a;
		input = i;
		description = desc;
	}
	
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	
	/**
	 * reset action
	 */
	public void reset(){
		operation = null;
		concept = null;
	}
	
	/**
	 * get action for this action
	 * @return
	 */
	public String getAction() {
		return action;
	}
	
	/**
	 * set action for this action object
	 * @param action
	 */
	public void setAction(String action) {
		this.action = action;
	}
	/**
	 * @return the receiver
	 */
	public String getReceiver() {
		return receiver;
	}
	/**
	 * @param receiver the receiver to set
	 */
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * get concept that maybe involved in 
	 * this action
	 * @return the concept
	 */
	public ConceptEntry getConceptEntry() {
		return concept;
	}

	/**
	 * set concept that maby involved in 
	 * this action
	 * @param concept the concept to set
	 */
	public void setConceptEntry(ConceptEntry concept) {
		this.concept = concept;
	}

	/**
	 * get input
	 * @return
	 */
	public String getInput() {
		return input;
	}
	
	/**
	 * set input
	 * @param input
	 */
	public void setInput(String input) {
		this.input = input;
	}

	/**
	 * is this action resolved
	 * @return
	 */
	public boolean isResolved(){
		return operation != null;
	}
	
	/**
	 * set main operation that will execute an action
	 * @param operation
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}


	/**
	 * run main action
	 */
	public void run(){
		if(operation != null){
			operation.run();
		}
	}

	/**
	 * run undo action
	 */
	public void undo(){
		if(operation != null){
			operation.undo();
		}
	}
	
	public String toString(){
		return receiver+" . "+action+" ( "+input+" )";
	}
	
	
	/**
	 * create DOM representation of this object
	 * @param doc
	 * @return
	 */
	public Element createElement(Document doc){
		Element a = doc.createElement("Action");
		a.setAttribute("receiver", getReceiver());
		a.setAttribute("action", getAction());
		String s = getInput();
		if(s == null)
			s = "";
		
		// set input property OR text content depending on size
		if(s.length() < 100)
			a.setAttribute("input",s);
		else
			a.setTextContent(s);
		return a;
	}
	
	/**
	 * parse message element
	 * @param element
	 */
	public void parseElement(Element a){
		setReceiver(a.getAttribute("receiver"));
		setAction(a.getAttribute("action"));
		String input = a.getAttribute("input");
		if(input == null || input.length() == 0){
			input = a.getTextContent().trim();
		}
		setInput(input);	
	}
	
}
