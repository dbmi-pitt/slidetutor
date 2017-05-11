/**
 * This class represents an tag representing NodeResponse and parameters
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.beans;


import java.util.*;
import java.io.Serializable;
import org.w3c.dom.*;

import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class MessageEntry  implements Serializable {
	private String text,resolvedText;
	private List<Action> actions;
	private boolean error;
	private transient ConceptEntry conceptEntry;
	
		
	/**
	 * clone message
	 */
	public MessageEntry clone() {
		MessageEntry e = new MessageEntry();
		e.setConceptEntry(conceptEntry);
		e.setText(text);
		e.setActions(actions);
		e.setError(error);
		e.setResolvedText(resolvedText);
		return e;
	}

	/**
	 * get message entry text
	 * @return
	 */
	public String getText() {
		return text;
	}

	/**
	 * set message entry text
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * get list of tags in message text
	 * @return
	 */
	public List<String> getTags(){
		return TextHelper.getTextTags(text);
	}
	
	/**
	 * get actions associated with this entry
	 * @return
	 */
	public List<Action> getActions(){
		if(actions == null)
			actions = new ArrayList<Action>();
		return actions;
	}
	
	/**
	 * set actions associated with this entry
	 * @return
	 */
	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	/**
	 * create DOM representation of this object
	 * @param doc
	 * @return
	 */
	public Element createElement(Document doc){
		Element root = doc.createElement("MessageEntry");
		
		// describe message
		Element msg = doc.createElement("Message");
		root.appendChild(msg);
		msg.setTextContent(text);
		
		
		// iterate through actions
		for(Action action: getActions()){
			root.appendChild(action.createElement(doc));
		}
				
		return root;
	}
	
	/**
	 * parse message element
	 * @param element
	 */
	public void parseElement(Element element){
		Element e = UIHelper.getElementByTagName(element,"Message");
		if(e != null)
			setText(e.getTextContent().trim());
		
		NodeList list = element.getElementsByTagName("Action");
		List<Action> alist = new ArrayList<Action>();
		for(int i=0;i<list.getLength();i++){
			Element a = (Element) list.item(i);
			Action ac = new Action();
			ac.parseElement(a);
			alist.add(ac);
		}
		setActions(alist);
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public ConceptEntry getConceptEntry() {
		return conceptEntry;
	}

	public void setConceptEntry(ConceptEntry conceptEntry) {
		this.conceptEntry = conceptEntry;
	}
	
	public String toString(){
		return getText();
	}

	public boolean equals(Object e) {
		if(e instanceof MessageEntry){
			MessageEntry m = (MessageEntry)e;
			if(isResolved() && m.isResolved())
				return getResolvedText().equals(m.getResolvedText());
			
			// compare messages and concepts (make sure they are not null though
			if(text != null && m.getText() != null && text.equals(m.getText())){
				if(getConceptEntry() != null)
					return getConceptEntry().equals(m.getConceptEntry());
				else if(m.getConceptEntry() == null)
					return true;
			}
			return false;
			//return getText().equals(m.getText()) && getConceptEntry().equals(m.getConceptEntry());
		}
		return super.equals(e);
	}

	public int hashCode() {
		return (isResolved())?getResolvedText().hashCode():getText().hashCode() + getConceptEntry().hashCode();
	}
	
	public String getResolvedText() {
		return resolvedText;
	}

	public void setResolvedText(String resolvedText) {
		this.resolvedText = resolvedText;
	}

	public boolean isResolved(){
		return resolvedText != null;
	}

}
