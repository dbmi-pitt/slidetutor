package edu.pitt.dbmi.tutor.beans;

import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

/**
 * describe expression of condition
 * @author tseytlin
 *
 */
public class ExpressionCondition extends Condition {
	private List<Condition> operands;
	
	public ExpressionCondition(){
		operands = new ArrayList<Condition>();
	}
	
	public ExpressionCondition(String op){
		this();
		setOperation(op);
	}
	
	public List<Condition> getOperands(){
		return operands;
	}
	
	public void addOperand(Condition c){
		operands.add(c);
	}
	
	public void removeOperand(Condition c){
		operands.remove(c);
	}
	
	
	public boolean evaluate(SessionStatistics stat) {
		if(OPERATION_OR.equals(getOperation())){
			for(Condition c: operands){
				if(c.evaluate(stat))
					return true;
			}
		}else if(OPERATION_AND.equals(getOperation())){
			boolean b = true;
			for(Condition c: operands){
				b &= c.evaluate(stat);
			}
			return b;
		}else if(OPERATION_NOT.equals(getOperation()) && !operands.isEmpty()){
			return !operands.get(0).evaluate(stat);
		}
		return false;
	}

	public String getCondition() {
		return CONDITION_EXPRESSION;
	}

	
	/**
	 * create DOM representation of this object
	 * @param doc
	 * @return
	 */
	public Element createElement(Document doc){
		Element a = doc.createElement("Condition");
		a.setAttribute("condition", getCondition());
		a.setAttribute("operation", getOperation());
	
		// add operands
		for(Condition c: operands){
			a.appendChild(c.createElement(doc));
		}
		
		return a;
	}
	
	/**
	 * parse message element
	 * @param element
	 */
	public void parseElement(Element a){
		setCondition(a.getAttribute("condition"));
		setOperation(a.getAttribute("operation"));
		
		NodeList nlist = a.getChildNodes();
		for(int i=0;i<nlist.getLength();i++){
			if(nlist.item(i) instanceof Element){
				Element e = (Element) nlist.item(i);
				if("Condition".equals(e.getNodeName())){
					Condition c = new Condition();
					c.parseElement(e);
					
					// reparse element if expression
					if(CONDITION_EXPRESSION.equals(c.getCondition())){
						c = new ExpressionCondition();
						c.parseElement(e);
					}
					
					// add as operand
					addOperand(c);
				}
			}
		}
		
	}

	
	public String toString(){
		StringBuffer str = new StringBuffer();
		if(OPERATION_NOT.equals(getOperation()))
			str.append("!");
		String op = (OPERATION_AND.equals(getOperation()))?"and":"or";
		str.append("(");
		for(int i=0;i<operands.size();i++){
			str.append(operands.get(i)+((i<operands.size()-1)?" "+op+" ":""));
		}
		str.append(")");
		return str.toString();
	}

}
