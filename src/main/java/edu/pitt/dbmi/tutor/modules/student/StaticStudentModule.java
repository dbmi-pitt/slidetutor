package edu.pitt.dbmi.tutor.modules.student;

import java.util.*;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.StudentModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

public class StaticStudentModule implements StudentModule{
	private Properties defaultConfig;
	private ProtocolModule protocol;
	
	public void load(){
		//NOOP:
	}
	
	
	public double getMasteryLevel(String skill) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String[] getObservedCases() {
		if(protocol != null){
			return protocol.getCases(Query.createUsernameQuery(Config.getUsername())).toArray(new String [0]);
		}
		return new String [0];
	}

	public String[] getObservedDomains() {
		Set<String> domains = new TreeSet<String>();
		if(protocol != null){
			// since list of cases contains case url, derive from there
			for(String c: getObservedCases()){
				domains.add(OntologyHelper.getDomainFromCase(c));
			}
		}
		return domains.toArray(new String [0]);
	}

	public String getResponse(String state) {
		// if no state, then OK
		if(state == null)
			return RESPONSE_CONFIRM;
		
		// check for confirm states
		for(String s : Config.getListProperty(this,"model.confirm.states")){
			if(state.matches(s.trim()))
				return RESPONSE_CONFIRM;
		}
		// check for irrelevant issues
		for(String s : Config.getListProperty(this,"model.irrelevant.states")){
			if(state.matches(s.trim()))
				return RESPONSE_IRRELEVANT;
		}
		/*
		if(ERROR_OK.equals(state) || state.startsWith("Missing") || state.endsWith("Yet") || state.endsWith("Done") ||  HINT_CASE_SOLVED.equals(state))
			return RESPONSE_CONFIRM;
		if(ERROR_FINDING_NOT_IN_CASE.equals(state))
			return RESPONSE_IRRELEVANT;
		*/
		return RESPONSE_FAILURE;
	}

	public String getStudentLevel() {
		return Config.hasProperty(this,"model.student.level")?Config.getProperty(this,"model.student.level"):NOVICE;
	}


	public void setProtocolModule(ProtocolModule module) {
		this.protocol = module;
		
	}

	public void sync(StudentModule tm) {
		// TODO Auto-generated method stub	
	}
	public void dispose() {
	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "This model utilizes a preset expertise level (novice, intermediate, expert) that does not change " +
				"throughout the tutoring session.";
	}

	public String getName() {
		return "Static Student Model";
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	public void receiveMessage(Message msg) {
		// TODO Auto-generated method stub
		
	}

	public void reset() {
		// TODO Auto-generated method stub
		
	}

	public void resolveAction(Action action) {
		// TODO Auto-generated method stub
		
	}

}
