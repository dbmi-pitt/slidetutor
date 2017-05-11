package edu.pitt.dbmi.tutor.beans;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

/**
 * defines a behvaioral task
 * @author tseytlin
 *
 */
public class Task {
	private ExpressionCondition expression;
	private List<Action> actions;
	public Task(Condition c, List<Action> l){
		setCondition(c);
		actions = l;
	}
	public Task(){
	}
	public List<Action> getActions(){
		if(actions == null)
			actions = new ArrayList<Action>();
		return actions;
	}
	public Condition getCondition(){
		return (getConditions().size() == 1)?getConditions().get(0):expression;
	}
	public List<Condition> getConditions(){
		if(expression == null)
			expression = new ExpressionCondition(Constants.OPERATION_AND);
		return expression.getOperands();
	}
	
	public void setCondition(Condition c){
		if(c instanceof ExpressionCondition)
			expression = (ExpressionCondition) c;
		else{
			expression = new ExpressionCondition(Constants.OPERATION_AND);
			expression.addOperand(c);
		}
	}
	
	public String toString(){
		return "if ( " +TextHelper.toString(getConditions())+" ) "+
			    "{ "+TextHelper.toString(getActions()) +" }";
	}
	
	/**
	 * create DOM representation of this object
	 * @param doc
	 * @return
	 */
	public Element createElement(Document doc){
		Element root = doc.createElement("Task");
		
		// describe message
		Element cond = getCondition().createElement(doc);
		root.appendChild(cond);
		
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
		Element e = UIHelper.getElementByTagName(element,"Condition");
		if(e != null){
			Condition c = new Condition();
			c.parseElement(e);
			setCondition(c);
		}
		NodeList list = element.getElementsByTagName("Action");
		for(int i=0;i<list.getLength();i++){
			Element a = (Element) list.item(i);
			Action ac = new Action();
			ac.parseElement(a);
			getActions().add(ac);
		}
	}
}
