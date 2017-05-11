package edu.pitt.dbmi.tutor.modules.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.util.OrderedMap;

public class DatabaseSession implements Session {
	private DatabaseProtocolModule protocol;
	private String caseURL,domain,condition,configuration,experiment,outcome,username;
	private Date finishTime,startTime;
	private int session_id;
	private OrderedMap<Integer,InterfaceEvent> interfaceEvents;
	private OrderedMap<Integer,ClientEvent> clientEvents;
	private OrderedMap<Integer,TutorResponse> tutorResponses;
	private OrderedMap<Integer,NodeEvent> nodeEvents;
	private HashMap<Integer,List<TutorResponse>> ce2tr;
	private HashMap<Integer,List<NodeEvent>> tr2ne;
	private HashMap<Integer,List<InterfaceEvent>> ce2ie;
	
	public DatabaseSession(DatabaseProtocolModule p){
		protocol = p;
	}
	
	/**
	 *  refresh values
	 */
	public void refresh(){
		interfaceEvents = null;
		clientEvents = null;
		tutorResponses = null;
		nodeEvents = null;
		ce2tr = null;
		tr2ne = null;
		ce2ie = null;
	}
	
	public void setCase(String caseURL) {
		this.caseURL = caseURL;
	}


	public void setDomain(String domain) {
		this.domain = domain;
	}


	public void setCondition(String condition) {
		this.condition = condition;
	}


	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}


	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}


	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}


	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}


	public void setSessionID(int sessionId) {
		session_id = sessionId;
	}


	public String getCase() {
		return caseURL;
	}
	
	public String getCaseName(){
		Pattern pt = Pattern.compile("[a-zA-Z]+://.*/(\\w+).case");
		Matcher mt = pt.matcher(""+caseURL);
		if(mt.matches())
			return mt.group(1);
		return caseURL;
	}
	

	public List<ClientEvent> getClientEvents() {
		if(clientEvents == null){
			clientEvents = new OrderedMap<Integer, ClientEvent>();
			for(ClientEvent e: protocol.getClientEvents(Collections.singletonList(""+session_id)))
				clientEvents.put(e.getMessageId(),e);
		}
		return clientEvents.getValues();
	}

	public String getCondition() {
		return condition;
	}

	public String getConfiguration() {
		return configuration;
	}

	public String getDomain() {
		return domain;
	}

	public String getExperiment() {
		return experiment;
	}

	public Date getFinishTime() {
		return finishTime;
	}

	public List<InterfaceEvent> getInterfaceEvents() {
		if(interfaceEvents == null){
			interfaceEvents = new OrderedMap<Integer, InterfaceEvent>();
			ce2ie = new HashMap<Integer, List<InterfaceEvent>>();
			for(InterfaceEvent e: protocol.getInterfaceEvents(Collections.singletonList(""+session_id))){
				interfaceEvents.put(e.getMessageId(),e);
			
				// put stuff into link table
				List<InterfaceEvent> ies = ce2ie.get(e.getClientEventId());
				if(ies == null){
					ies = new ArrayList<InterfaceEvent>();
					ce2ie.put(e.getClientEventId(),ies);
				}
				ies.add(e);
			}
		}
		return interfaceEvents.getValues();
	}

	public String getOutcome() {
		return outcome;
	}

	public String getSessionID() {
		return ""+session_id;
	}

	public Date getStartTime() {
		return startTime;
	}

	public List<TutorResponse> getTutorResponses() {
		if(tutorResponses == null){
			tutorResponses = new OrderedMap<Integer, TutorResponse>();
			ce2tr = new HashMap<Integer, List<TutorResponse>>();
			for(TutorResponse e: protocol.getTutorResponses(Collections.singletonList(""+session_id))){
				tutorResponses.put(e.getMessageId(),e);
				
				// put stuff into link table
				List<TutorResponse> trs = ce2tr.get(e.getClientEventId());
				if(trs == null){
					trs = new ArrayList<TutorResponse>();
					ce2tr.put(e.getClientEventId(),trs);
				}
				trs.add(e);
			}
		}
		return tutorResponses.getValues();
	}
	
	public List<NodeEvent> getNodeEvents() {
		if(nodeEvents == null){
			nodeEvents = new OrderedMap<Integer,NodeEvent>();
			tr2ne = new HashMap<Integer, List<NodeEvent>>();
			for(NodeEvent e: protocol.getNodeEvents(Collections.singletonList(""+session_id))){
				nodeEvents.put(e.getMessageId(),e);
			
				// put stuff into link table
				List<NodeEvent> nes = tr2ne.get(e.getTutorResponseId());
				if(nes == null){
					nes = new ArrayList<NodeEvent>();
					tr2ne.put(e.getTutorResponseId(),nes);
				}
				nes.add(e);
			}
		}
		return nodeEvents.getValues();
	}


	public List<TutorResponse> getTutorResponses(ClientEvent e) {
		// make sure that tutor responses are there
		getTutorResponses();
		List<TutorResponse> trs = ce2tr.get(e.getMessageId());
		return (trs != null)?trs:Collections.EMPTY_LIST;
	}

	public String getUsername() {
		return username;
	}
	
	public String toString(){
		return session_id+" "+username+" "+getCaseName();
	}

	/**
	 * get message that represents a start problem event
	 * @return
	 */
	public List<ProblemEvent> getProblemEvents(){
		List<ProblemEvent> list = new ArrayList<ProblemEvent>();
		
		// start event
		ProblemEvent spe = new ProblemEvent();
		spe.setType(Constants.TYPE_START);
		spe.put("username",getUsername());
		spe.put("condition",getCondition());
		spe.put("case_url",getCase());
		spe.put("domain_url",getDomain());
		spe.put("config_url",getConfiguration());
		if(getStartTime() != null)
			spe.setTimestamp(getStartTime().getTime());
		list.add(spe);
		
		//end event
		ProblemEvent epe = new ProblemEvent();
		epe.setType(Constants.TYPE_END);
		epe.put("username",getUsername());
		epe.put("condition",getCondition());
		epe.put("case_url",getCase());
		epe.put("domain_url",getDomain());
		epe.put("config_url",getConfiguration());
		epe.put("outcome",getOutcome());
		if(getFinishTime() != null)
			epe.setTimestamp(getFinishTime().getTime());
		list.add(epe);
		
		return list;
	}



	public List<NodeEvent> getNodeEvents(TutorResponse e) {
		getNodeEvents();
		List<NodeEvent> nes = tr2ne.get(e.getMessageId());
		return (nes != null)?nes:Collections.EMPTY_LIST;
	}
	

	public List<InterfaceEvent> getInterfaceEvents(ClientEvent e) {
		getInterfaceEvents();
		List<InterfaceEvent> ies = ce2ie.get(e.getMessageId());
		return (ies != null)?ies:Collections.EMPTY_LIST;
	}
	
	/**
	 * delete session
	 */
	public boolean delete(){
		return protocol.removeSessions(Collections.singletonList((Session)this));
	}
	
}
