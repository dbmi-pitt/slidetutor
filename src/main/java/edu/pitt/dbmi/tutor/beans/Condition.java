package edu.pitt.dbmi.tutor.beans;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.util.TextHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

/**
 * condition represents a condition for behavior module
 * @author tseytlin
 */
public class Condition {
	private String condition,operation,input,description;
	public Condition(){}
	public Condition(String c, String o, String i){
		condition = c;
		operation = o;
		input = i;
	}
	
	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}
	
	/**
	 * is condition measures time related tasks?
	 * @return
	 */
	public boolean isTimeCondition(){
		return getCondition().contains("Time");
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
	/**
	 * does this condition match given statistics?
	 * @param stat
	 * @return
	 */
	public boolean evaluate(SessionStatistics stat){
		// check against condition
		if(CONDITION_CASE_START.equalsIgnoreCase(condition)){
			return stat.currentTime == 0;
		}else if(CONDITION_CASE_FINISH.equalsIgnoreCase(condition)){
			return stat.finishTime != 0;
		}else if(CONDITION_CASE_COUNT.equalsIgnoreCase(condition)){
			return compare(stat.caseCount,operation,input);
		}else if(CONDITION_USER_ACTION_COUNT.equalsIgnoreCase(condition)){
			return compare(stat.userActionCount,operation,input);
		}else if(CONDITION_USER_TYPE_COUNT.equalsIgnoreCase(condition)){
			return compare(stat.userTypeCount,operation,input);
		}else if(CONDITION_TOTAL_TIME.equalsIgnoreCase(condition)){
			return compare(stat.totalTime,operation,input);
		}else if(CONDITION_CASE_TIME.equalsIgnoreCase(condition)){
			return compare(stat.currentTime - stat.startTime,operation,input);
		}else if(CONDITION_USER_ACTION.equalsIgnoreCase(condition)){
			return compare(stat.lastClientEvent,operation,input);
		}else if(CONDITION_TUTOR_RESPONSE.equalsIgnoreCase(condition)){
			return compare(stat.lastTutorResponse,operation,input);
		}
		return false;
	}
	
	/**
	 * get description of this condition
	 * @return
	 */
	public String getDescription() {
		if(description == null){
			// some default descriptions
			// check against condition
			if(CONDITION_CASE_START.equalsIgnoreCase(condition)){
				description = "This condition is true, when case starts";
			}else if(CONDITION_CASE_FINISH.equalsIgnoreCase(condition)){
				description =  "This condition is true, when case finishes";
			}else if(CONDITION_CASE_COUNT.equalsIgnoreCase(condition)){
				description =  "This condition is true, when a number of solved cases is equal to an input number";
			}else if(CONDITION_USER_ACTION_COUNT.equalsIgnoreCase(condition)){
				description =  "This condition is true, when a  number of user actions is equal to an input number";
			}else if(CONDITION_USER_TYPE_COUNT.equalsIgnoreCase(condition)){
				description =  "This condition is true, when a number of user actions for a given type is equal, not equal " +
					    "to an input: <action type:number>. For example: If input is Done:3, then condition will hold true" +
					    "on a third time that Done action is performed by a user.";
			}else if(CONDITION_TOTAL_TIME.equalsIgnoreCase(condition)){
				description =  "This condition is true, when a total time spent by a user thoughout his tutoring is equal to an input number in minutes.";
			}else if(CONDITION_CASE_TIME.equalsIgnoreCase(condition)){
				description =  "This condition is true, when a total time spent by a user on each case is equal to an input number in minutes.";
			}else if(CONDITION_USER_ACTION.equalsIgnoreCase(condition)){
				description =  "This condition is true, when a user action is equal or not equal to an input: (type|label|action):description." +
					   "For example: If input is type:Finding, then condition will hold true when finding is asserted by a user.";
			}else if(CONDITION_TUTOR_RESPONSE.equalsIgnoreCase(condition)){
				description = "This condition is true, when a response to user's action is equal or not equal to an input. Input can be either Confirm or Failure";
			}
		}
		return description;
	}

	
	/**
	 * compare two values based on operation
	 * @param a
	 * @param op
	 * @param input
	 * @return
	 */
	private boolean compare(double a, String op, String input){
		double b = 0;
		try{
			b = Double.parseDouble(input);
		}catch(NumberFormatException ex){}
		
		if(OPERATION_EQUALS.equalsIgnoreCase(op))
			return a == b;
		else if(OPERATION_GREATER_THEN.equalsIgnoreCase(op))
			return a > b;
		else if(OPERATION_LESS_THEN.equalsIgnoreCase(op))
			return a < b;
		else if(OPERATION_NOT_EQUALS.equalsIgnoreCase(op))
			return a != b;
		else if(OPERATION_INTERVAL.equalsIgnoreCase(op))
			return interval(a,input);
		
		return false;
	}
	
	/**
	 * compare two values based on operation
	 * @param a
	 * @param op
	 * @param input
	 * @return
	 */
	private boolean compare(Map<String,Integer> map, String op, String input){
		String [] p = input.split("\\:");
		
		// check for that type
		if(p.length != 2 || !map.containsKey(p[0].trim()))
			return false;
		
		double a = map.get(p[0].trim());
		double b = 0;
		try{
			b = Double.parseDouble(p[1]);
		}catch(NumberFormatException ex){}
		
		if(OPERATION_EQUALS.equalsIgnoreCase(op))
			return a == b;
		else if(OPERATION_GREATER_THEN.equalsIgnoreCase(op))
			return a > b;
		else if(OPERATION_LESS_THEN.equalsIgnoreCase(op))
			return a < b;
		else if(OPERATION_NOT_EQUALS.equalsIgnoreCase(op))
			return a != b;
		else if(OPERATION_INTERVAL.equalsIgnoreCase(op))
			return interval(a,input);
		
		return false;
	}
	
	/**
	 * compare two values based on operation
	 * @param a
	 * @param op
	 * @param input
	 * @return
	 */
	private boolean compare(Message msg, String op, String input){
		if(input == null)
			input = "";
		
		if(msg == null)
			return false;
		
		// defaults
		String a = msg.getType();
		String b = input;
		
		// parse input
		if(msg instanceof TutorResponse){
			a = ((TutorResponse) msg).getResponse();
		}else if(input.toLowerCase().startsWith("type:")){
			b = input.substring(input.indexOf(":")+1).trim();
		}else if(input.toLowerCase().startsWith("label:")){
			a = msg.getLabel();
			b = input.substring(input.indexOf(":")+1).trim();
		}else if(input.toLowerCase().startsWith("action:")){
			a = msg.getAction();
			b = input.substring(input.indexOf(":")+1).trim();
		}
	
		// now compare
		if(OPERATION_EQUALS.equalsIgnoreCase(op))
			return a.equalsIgnoreCase(b);
		else if(OPERATION_NOT_EQUALS.equalsIgnoreCase(op))
			return !a.equalsIgnoreCase(b);
		return false;
	}
	
	
	/**
	 * interval comparison
	 * @param n
	 * @param input
	 * @return
	 */
	private boolean interval(double n, String input){
		if(input == null)
			input = "";
		
		// parse interval (first number is interval, second is offset
		int i = 0;
		int [] nums = new int [2];
    	
		// split any string into potential rgb
		try{
			for(String num : input.split("[^\\d]")){
				if(num.length() > 0 && i < nums.length){
					 nums[i++] = Integer.parseInt(num);
				}
			}
		}catch(Exception ex){}
		
		// sanity check, if interval is 0, then this is invalid 
		if(nums[0] == 0)
			return false;
		
		// now lets check
		return (((int) n+nums[1]) % nums[0]) == 0 ;
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
		setCondition(a.getAttribute("condition"));
		setOperation(a.getAttribute("operation"));
		String input = a.getAttribute("input");
		if(input == null || input.length() == 0){
			input = a.getTextContent().trim();
		}
		setInput(input);	
	}
	
	public String toString(){
		return TextHelper.toString(getCondition())+" "+TextHelper.toString(getOperation())+" "+TextHelper.toString(getInput());
	}
}
