package edu.pitt.dbmi.tutor.modules.protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.TextHelper;
import static edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule.*;


public class FileSession implements Session {
	private String caseURL,domain,condition,configuration,experiment,outcome,username;
	private Date finishTime,startTime;
	private File file;
	private OrderedMap<Integer,InterfaceEvent> interfaceEvents;
	private OrderedMap<Integer,ClientEvent> clientEvents;
	private OrderedMap<Integer,NodeEvent> nodeEvents;
	private OrderedMap<Integer,TutorResponse> tutorResponses;
	private HashMap<Integer,List<TutorResponse>> ce2tr;
	private HashMap<Integer,List<InterfaceEvent>> ce2ie;
	private HashMap<Integer,List<NodeEvent>> tr2ne;
	private List<ProblemEvent> problemEvents;
	private Queue<ClientEvent> clientEventQueue;
	private Queue<InterfaceEvent> interfaceEventQueue;
	private Queue<TutorResponse> tutorResponseQueue;
	
	// static fields
	public static final List<String> messageFields = Arrays.asList(Message.getMessageFields());
	public static final List<String> tutorResponseFields = Arrays.asList(TutorResponse.getMessageFields());
	//private static final List<String> problemEventFields = Arrays.asList(ProblemEvent.getMessageFields());
	
	public FileSession(File f){
		file = f;
		
		// extract som meta info from filename
		parseFilename(f);
	}
	
	/**
	 *  refresh values
	 */
	public void refresh(){
		interfaceEvents = null;
		clientEvents = null;
		tutorResponses = null;
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
			readLogFile();
		}
		return clientEvents.getValues();
	}

	public String getCondition() {
		return condition;
	}

	public String getConfiguration() {
		if(configuration == null)
			readLogFile();
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
			readLogFile();
		}
		return interfaceEvents.getValues();
	}

	public String getOutcome() {
		return outcome;
	}

	public String getSessionID() {
		return file.getName();
	}

	public Date getStartTime() {
		return startTime;
	}

	public List<TutorResponse> getTutorResponses() {
		if(tutorResponses == null){
			readLogFile();
		}
		return tutorResponses.getValues();
	}

	public List<TutorResponse> getTutorResponses(ClientEvent e) {
		// make sure that tutor responses are there
		getTutorResponses();
		List<TutorResponse> trs = ce2tr.get(e.getMessageId());
		return (trs != null)?trs:Collections.EMPTY_LIST;
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
	
	public String getUsername() {
		return username;
	}
	
	public String toString(){
		return file.getName();
	}

	/**
	 * get message that represents a start problem event
	 * @return
	 */
	public List<ProblemEvent> getProblemEvents(){
		if(problemEvents == null){
			readLogFile();
		}
		return problemEvents;
	}
	
	
	/**
	 * read log file to get all of the data
	 */
	private void readLogFile(){
		clientEvents = new OrderedMap<Integer, ClientEvent>();
		interfaceEvents = new OrderedMap<Integer, InterfaceEvent>();
		tutorResponses = new OrderedMap<Integer, TutorResponse>();
		nodeEvents = new OrderedMap<Integer, NodeEvent>();
		problemEvents = new ArrayList();
		ce2tr = new HashMap<Integer, List<TutorResponse>>();
		ce2ie = new HashMap<Integer, List<InterfaceEvent>>();
		tr2ne = new HashMap<Integer, List<NodeEvent>>();
		
		// read in the file
		try{
			// figure out the study
			for(File f = file.getParentFile(); f != null; f = f.getParentFile()){
				if(f.getName().startsWith(FileProtocolModule.STUDY_PREFIX)){
					experiment = f.getName().substring(FileProtocolModule.STUDY_PREFIX.length());
					break;
				}
			}
			
			// now read the log file
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for(String line = reader.readLine();line != null; line = reader.readLine()){
				line = line.trim();
				
				// skip blanks
				if(line.length() == 0)
					continue;
				
				// now parse node event
				if(line.startsWith(PE)){
					ProblemEvent pe = parseProblemEvent(line.substring(PE.length()).trim());
					problemEvents.add(pe);
				}else if(line.startsWith(IE)){
					InterfaceEvent ie = parseInterfaceEvent(line.substring(IE.length()).trim());
					interfaceEvents.put(ie.getMessageId(),ie);
					
					// put stuff into link table
					List<InterfaceEvent> ies = ce2ie.get(ie.getClientEventId());
					if(ies == null){
						ies = new ArrayList<InterfaceEvent>();
						ce2ie.put(ie.getClientEventId(),ies);
					}
					ies.add(ie);
				}else if(line.startsWith(CE)){
					ClientEvent ce = parseClientEvent(line.substring(CE.length()).trim());
					clientEvents.put(ce.getMessageId(),ce);
				}else if(line.startsWith(TR)){
					TutorResponse tr = parseTutorResponse(line.substring(TR.length()).trim());
					tutorResponses.put(tr.getMessageId(),tr);
					
					// put stuff into link table
					List<TutorResponse> trs = ce2tr.get(tr.getClientEventId());
					if(trs == null){
						trs = new ArrayList<TutorResponse>();
						ce2tr.put(tr.getClientEventId(),trs);
					}
					trs.add(tr);
				}else if(line.startsWith(NE)){
					NodeEvent ne = parseNodeEvent(line.substring(NE.length()).trim());
					nodeEvents.put(ne.getMessageId(),ne);
					
					// put stuff into link table
					List<NodeEvent> nes = tr2ne.get(ne.getTutorResponseId());
					if(nes == null){
						nes = new ArrayList<NodeEvent>();
						tr2ne.put(ne.getTutorResponseId(),nes);
					}
					nes.add(ne);
				}
			}
			
		}catch(IOException ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
		}
	}
	
	private ProblemEvent parseProblemEvent(String line) {
		// get message
		Map<String,String> msg = parseMessage(line);
		
		// copy info into event
		ProblemEvent pe = new ProblemEvent();
		for(String key: msg.keySet()){
			pe.put(key,msg.get(key));
		}
		
		// setup fields in session
		if("start".equalsIgnoreCase(pe.getType())){
			username = pe.getUsername();
			condition = pe.getCondition();
			caseURL = pe.getCaseURL();
			domain = pe.getDomainURL();
			configuration = pe.getConfigURL();
			startTime = new Date(pe.getTimestamp());
		}else if("end".equalsIgnoreCase(pe.getType())){
			outcome = pe.getOutcome();
			finishTime = new Date(pe.getTimestamp());
		}
	
		// set message id
		if(msg.containsKey("message_id")){
			pe.setMessageId(Integer.parseInt(msg.get("message_id")));
		}
		
		
		return pe;
	}

	private TutorResponse parseTutorResponse(String line) {
		// get message
		Map<String,String> msg = parseMessage(line);
		
		// copy info into event
		TutorResponse tr = new TutorResponse();
		for(String key: msg.keySet()){
			if(tutorResponseFields.contains(key)){
				tr.put(key,msg.get(key));
			//}else if("input".equalsIgnoreCase(key)){
			//tr.setInput(msg.get(key));
			}else{
				Object input = tr.getInput();
				// init input
				if(input == null){
					input = new HashMap<String,String>();
					tr.setInput(input);
				}
				
				// is input a map
				if(input instanceof Map){
					((Map)input).put(key,msg.get(key));
				}
			}
		}
		
		// set message id
		if(msg.containsKey("message_id")){
			tr.setMessageId(Integer.parseInt(msg.get("message_id")));
		}else{
			// poll the client event
			ClientEvent ce = getClientEventQueue().poll();
			if(ce == null){
				// if nothing is in the queue, then attach to the last client event in the list
				ce = clientEvents.getValues().get(clientEvents.getValues().size()-1);
			}
			
			// now assign id
			if(ce != null)
				tr.setClientEventId(ce.getMessageId());
			
		}
	
		
		
		if(msg.containsKey("client_event_id")){
			tr.setClientEventId(Integer.parseInt(msg.get("client_event_id")));
		}
		
		return tr;
	}

	private NodeEvent parseNodeEvent(String line) {
		// get message
		Map<String,String> msg = parseMessage(line);
		
		// copy info into event
		NodeEvent tr = new NodeEvent();
		for(String key: msg.keySet()){
			tr.put(key,msg.get(key));
		}
		
		// set message id
		if(msg.containsKey("message_id")){
			tr.setMessageId(Integer.parseInt(msg.get("message_id")));
		}
		if(msg.containsKey("tutor_response_id")){
			tr.setTutorResponseId(Integer.parseInt(msg.get("tutor_response_id")));
		}
		
		return tr;
	}
	
	
	private ClientEvent parseClientEvent(String line) {
		// get message
		Map<String,String> msg = parseMessage(line);
		
		// copy info into event
		ClientEvent ce = new ClientEvent();
		for(String key: msg.keySet()){
			if(messageFields.contains(key)){
				ce.put(key,msg.get(key));
			}else if("input".equalsIgnoreCase(key)){
				ce.setInput(msg.get(key));
			}else{
				Object input = ce.getInput();
				// init input
				if(input == null){
					input = new HashMap<String,String>();
					ce.setInput(input);
				}
				// is input a map
				if(input instanceof Map){
					((Map)input).put(key,msg.get(key));
				}
			}
		}
		
		// set message id
		if(msg.containsKey("message_id")){
			ce.setMessageId(Integer.parseInt(msg.get("message_id")));
		}else{
			// if we are here, that means it is an old log file where I forgot to write out IDs, oops
			getClientEventQueue().add(ce);
			
			// now lets take a look at queued up interface events
			// most client events will start with button press
			while(true){
				InterfaceEvent ie = getInterfaceEventQueue().poll();
				if(ie == null)
					break;
				// check if this is one of independents :)
				if(!(ie.getType().equalsIgnoreCase("start") || ie.getAction().equalsIgnoreCase("ImageChanged"))){
					ie.setClientEventId(ce.getMessageId());
				}
			}
		}
		
		return ce;
	}

	private InterfaceEvent parseInterfaceEvent(String line){
		// get message
		Map<String,String> msg = parseMessage(line);
		
		// copy info into event
		InterfaceEvent ie = new InterfaceEvent();
		for(String key: msg.keySet()){
			if(messageFields.contains(key)){
				ie.put(key,msg.get(key));
			}else if("input".equalsIgnoreCase(key)){
				ie.setInput(msg.get(key));
			}else{
				Object input = ie.getInput();
				// init input
				if(input == null){
					input = new HashMap<String,String>();
					ie.setInput(input);
				}
				// is input a map
				if(input instanceof Map){
					((Map)input).put(key,msg.get(key));
				}
			}
		}
		
		// set message id
		if(msg.containsKey("message_id")){
			ie.setMessageId(Integer.parseInt(msg.get("message_id")));
		}else{
			// if we are here, that means it is an old log file where I forgot to write out IDs, oops
			getInterfaceEventQueue().add(ie);
		}
		
		if(msg.containsKey("client_event_id")){
			ie.setClientEventId(Integer.parseInt(msg.get("client_event_id")));
		}
		
		
		return ie;
	}
	
	
	private Queue<InterfaceEvent> getInterfaceEventQueue() {
		if(interfaceEventQueue == null)
			interfaceEventQueue = new LinkedList<InterfaceEvent>();
		return interfaceEventQueue;
	}

	private Queue<ClientEvent> getClientEventQueue() {
		if(clientEventQueue == null)
			clientEventQueue = new LinkedList<ClientEvent>();
		return clientEventQueue;
	}
	
	
	public static Map<String,String> parseMessage(String line){
		Map<String,String> msg = new LinkedHashMap<String, String>();
		//Pattern pt = Pattern.compile("\\((\\w+)\\s*\"(.*)\"\\)");
		Pattern pt = Pattern.compile("\\(([\\w\\-]+)\\s+\"(.*?)\"\\)");
		Matcher mt = pt.matcher(line);
		
		// go through all field
		while(mt.find()){
			String key = mt.group(1);
			String val = mt.group(2);
			
			msg.put(key,val);
		}
		
		return msg;
	}
	
	public List<NodeEvent> getNodeEvents() {
		if(nodeEvents == null){
			readLogFile();
		}
		return nodeEvents.getValues();
	}
	
	/**
	 * parse metadata from file
	 * @param f
	 */
	private void parseFilename(File f){
		Pattern pt = Pattern.compile("([\\w]+)\\-([\\w]+)\\-([\\d\\-_]+)\\.log");
		Matcher mt = pt.matcher(f.getName());
		// we have a match
		if(mt.matches()){
			setUsername(mt.group(1));
			setCase(mt.group(2));
			setStartTime(TextHelper.parseDate(mt.group(3),"M_d_yy-HH_mm_ss"));
		}
		// set condition based on parent dir
		if(f.getParentFile().getName().startsWith(CONDITION_PREFIX)){
			setCondition(f.getParentFile().getName().substring(CONDITION_PREFIX.length()));
		}
		
		// check out experiment
		if(f.getAbsolutePath().contains(STUDY_PREFIX)){
			String s = f.getAbsolutePath();
			int i = s.indexOf(STUDY_PREFIX)+STUDY_PREFIX.length();
			setExperiment(s.substring(i,s.indexOf(File.separatorChar,i)));
		}
	}
	
	/**
	 * delete session
	 */
	public boolean delete(){
		return file.delete();
	}
	
	
	public static void main(String [] args){
		File f = new File("/home/tseytlin/.SlideTutor/protocol/STUDY_bogus/USER_me/CONDITION_Test/eugene-HA_1210-9_17_10-10_30_51.log");
		//File f = new File("/home/tseytlin/.SlideTutor/protocol/USER_eugene/CONDITION_NodularImmediate/eugene-HA_1210-9_17_10-10_30_51.log");
		FileSession s = new FileSession(f);
		System.out.println(s.getCase()+" "+s.getUsername()+" "+s.getCondition()+" "+s.getExperiment()+s.getStartTime());
		
		for(ClientEvent ce: s.getClientEvents())
			System.out.println(ce);
	}

}
