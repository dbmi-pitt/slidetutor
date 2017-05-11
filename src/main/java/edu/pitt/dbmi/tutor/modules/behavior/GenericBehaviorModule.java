/**
 * 
 */
package edu.pitt.dbmi.tutor.modules.behavior;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.BehavioralModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

/**
 * Basic implementation of behavioural module.
 * It works of a sequence file that is composed of condition/action pairs
 * @author tseytlin
 *
 */
public class GenericBehaviorModule implements BehavioralModule, Runnable {
	private Properties defaultConfig;
	private boolean done;
	private Map<Condition,List<Action>> tasks;
	private SessionStatistics stats;
	private Tutor tutor;
		
	
	public Tutor getTutor() {
		return tutor;
	}


	public void setTutor(Tutor tutor) {
		this.tutor = tutor;
	}

	
	/**
	 * load resources associated w/ this module
	 */
	public void load(){
		stats = new SessionStatistics();
		//TODO: load some stats from DB???
		
		String location = Config.getProperty("tutor."+getTutor().getId()+".behavior");
		if(TextHelper.isEmpty(location))
			location = Config.getProperty(this,"tutor.behavior.location");
		tasks = loadTasks(location);
		
		// now, if we have no tasks we should insert the default behaviour
		// to finish case on done
		if(tasks.isEmpty()){
			// finish on correct done
			ExpressionCondition c = new ExpressionCondition(OPERATION_AND);
			c.addOperand(new Condition(CONDITION_USER_ACTION,OPERATION_EQUALS,TYPE_DONE));
			c.addOperand(new Condition(CONDITION_TUTOR_RESPONSE,OPERATION_EQUALS,RESPONSE_CONFIRM));
						
			// do action
			Action a = new Action(Tutor.class.getSimpleName(),POINTER_ACTION_FINISH_CASE,getTutor().getId());
			
			tasks.put(c,Collections.singletonList(a));
		}
		
	}
	
	/**
	 * get scenario set for a given domain
	 * @return
	 */
	private Map<Condition,List<Action>> loadTasks(String location){
		Map<Condition,List<Action>> tasks = new LinkedHashMap<Condition, List<Action>>();
		if(location.length() > 0){
			try{
				File f = new File(location);
				if(f.exists()){
					tasks = loadTasks(new FileInputStream(f));
				}else if(UIHelper.isURL(location)){
					URL url = new URL(location);
					tasks = loadTasks(url.openStream());
				}else {
					tasks = loadTasks(getClass().getResourceAsStream(location));
				}
			}catch(Exception ex){
				Config.getLogger().severe("Could not load help file from "+location);
				ex.printStackTrace();
			}
		}
		return tasks;
	}
	
	
	/**
	 * load tasks
	 * @param location
	 * @return
	 * @throws Exception
	 */
	private  Map<Condition,List<Action>> loadTasks(InputStream in) throws Exception{
		Map<Condition,List<Action>> tasks = new LinkedHashMap<Condition, List<Action>>();
		try {
			Document document = UIHelper.parseXML(in);
			
			//print out some useful info
			Element element = document.getDocumentElement();
			
			// iterate through tasts
			NodeList list = element.getElementsByTagName("Task");
			for(int i=0;i<list.getLength();i++){
				Node node = list.item(i);
				if(node instanceof Element){
					Element e = (Element) node;
					// load condition
					Element cond = UIHelper.getElementByTagName(e,"Condition");
					Condition condition = new Condition();
					condition.parseElement(cond);
					if(Constants.CONDITION_EXPRESSION.equals(condition.getCondition())){
						condition = new ExpressionCondition();
						condition.parseElement(cond);
					}
					
					// iterate through actions
					List<Action> actions = new ArrayList<Action>();
					NodeList alist = e.getElementsByTagName("Action");
					for(int j=0;j<alist.getLength();j++){
						Node anode = alist.item(j);
						if(anode instanceof Element){
							Action action = new Action();
							action.parseElement((Element) anode);
							actions.add(action);
						}
					}
					
					// save task			
					tasks.put(condition,actions);
				}
			}
			
		} catch (Exception ex) {
			throw new IOException(ex.getMessage());
		}
		
		return tasks;
	}
	
	
	/**
	 * run through tasks that match
	 */
	private void doTasks(SessionStatistics s){
		for(Condition c: tasks.keySet()){
			if(c.evaluate(s)){
				doActions(tasks.get(c));
			}
		}
	}
	
	/**
	 * do actions for a given message entry
	 * @param msg
	 * @param concept
	 */
	private void doActions(List<Action> actions){
		// execute actions
		for(Action a : actions){
			if(!a.isResolved())
				Communicator.getInstance().resolveAction(a);
			a.run();
		}
	}
	

	/**
     * start behavior module session
     */
    public void start(){
        done = false;
        
        // start timer
        if(hasTimeRelatedTasks())
        	(new Thread(this)).start();
        
        // modify stats
        stats.totalTime += (stats.finishTime - stats.startTime);
        stats.clear();
        stats.caseCount ++;
        stats.startTime = System.currentTimeMillis();
                
        // check actions that can be triggered during start
        doTasks(stats);
    }
    
    
    /**
     * stop behavior module session
     */
    public void stop(){
        done = true;
        
        // modify stats
        stats.finishTime = System.currentTimeMillis();
        
        // check actions that can be triggered during stop
        doTasks(stats);
    }

    private boolean hasTimeRelatedTasks(){
    	for(Condition c: tasks.keySet())
    		if(c.isTimeCondition())
    			return true;
    	return false;
    }
    
    
	/**
	 * dispose of resources 
	 */
	public void dispose() {
		reset();
		tasks.clear();
	}

	/**
	 * get default configuration
	 */
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Modify tutor behavior by utilizing a behavior file composed of condition/action(s) pairs";
	}

	public String getName() {
		return "Generic Behavior Module";
	}

	/** 
	 * get action supported by this module
	 */
	public Action[] getSupportedActions() {
		return new Action [0];
	}

	/**
	 * this module monitors all traffic
	 */
	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	/**
	 * monitor all traffic 
	 */
	public void receiveMessage(Message msg) {
		// set current time
		stats.currentTime = System.currentTimeMillis();
	
		// check for event related conditions
		if(msg instanceof ClientEvent){
			stats.lastClientEvent = (ClientEvent) msg;
		}else if(msg instanceof TutorResponse && ! (msg instanceof NodeEvent)){
			stats.lastTutorResponse = (TutorResponse) msg;
		}
		
		// run tasks after response
		if(isEventPair(stats)){
			// increment user actions
			stats.userActionCount ++;
			
			// increment user type count
			String type  = stats.lastClientEvent.getType();
			if(stats.userTypeCount.containsKey(type))
				stats.userTypeCount.put(type,stats.userTypeCount.get(type)+1);
			else
				stats.userTypeCount.put(type,1);
			
			// do relevant tasks
			doTasks(stats);
			
			// clear event pointers
			stats.lastClientEvent = null;
			stats.lastTutorResponse = null;
		}
	}
	
	
	/**
	 * is matching event pair
	 * @param s
	 * @return
	 */
	private boolean isEventPair(SessionStatistics s){
		if(s.lastClientEvent == null || s.lastTutorResponse == null)
			return false;
		return s.lastClientEvent.getMessageId() == s.lastTutorResponse.getClientEventId();
	}
	

	/**
	 * reset condition map
	 */
	public void reset() {
		stop();
		stats.clear();
	}

	public void resolveAction(Action action) {
		//NOOP
	}

	/**
	 * this is where magic happens
	 */
	public void run() {
		// check every 60s for time-related tasks
		while(!done){
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) { }
			
			// set current time
			stats.currentTime = System.currentTimeMillis();
			
			// now do tasks
			doTasks(stats);
		}
	}

	public void sync(BehavioralModule tm) {
		// TODO Auto-generated method stub
		
	}

}
