package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getCaseName;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.TextHelper;

public class TestQuestionFixer implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	private ExpertModule expert;
	private Map<String,ConceptEntry> findingMap;
	
	public void dispose() {
	}

	public String getDescription() {
		return "Split Finding into Feature/Attribute pair for several test questions.";
	}
	
	public String getName() {
		return "Test Question Fixer";
	}
	public String toString(){
		return getName();
	}
	
	public void initialize() {
		expert = new DomainExpertModule();
		expert.load();
	}


	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}

	
	
	public boolean process(Session s) {
		findingMap = new HashMap<String, ConceptEntry>();
		
		// for all questions to identify finding on a picture or differentiate between two diagnoses
		if(s.getCase().matches(".*P[FC]_?\\d+.*")){
			// reset id count to 100, so not to interere w/ other ids
			ConceptEntry.resetConceptIdCount(100);
			
			// load domain for resolving
			// load knowledge bases
			if(expert.getDomain().length() == 0 || !expert.getDomain().equals(s.getDomain())){
				expert.openDomain(s.getDomain());
			}
			
			// skip whole step if only 1 finding AND it is already a feature
			ConceptEntry fx = null;
			for(ClientEvent ce: s.getClientEvents()){
				if(TYPE_FINDING.equals(ce.getType())){
					if(fx == null){
						fx = ConceptEntry.getConceptEntry(ce.getObjectDescription());
					}else{
						fx = null;
						break;
					}
				}else if(TYPE_PRESENTATION.equals(ce.getType())){
					fx = null;
					break;
				}
			}
			// got only finding, do we need to split?
			if(fx != null){
				expert.resolveConceptEntry(fx);
				findingMap.put(fx.getObjectDescription(),fx);
				// if feature is the same as finding
				if(fx.getFeature().getName().equals(fx.getName())){
					return true;
				}
			}
			
			output.println("processing "+s.getUsername()+" "+getCaseName(s.getCase())+" (split findings) ...");
			
			// go over 
			ClientEvent lastClientEvent = null;
			for(ClientEvent ce: s.getClientEvents()){
				if(TYPE_FINDING.equals(ce.getType())){
					if(lastClientEvent != null && !ACTION_REMOVED.equals(ce.getAction()))
						addClientEvent(lastClientEvent,s,ACTION_REMOVED);
					addClientEvent(ce,s);
					for(TutorResponse tr: s.getTutorResponses(ce)){
						for(NodeEvent ne : s.getNodeEvents(tr)){
							deleteMessage(ne);
						}
						deleteMessage(tr);
					}
					deleteMessage(ce);
					lastClientEvent = ce;
				}else if(TYPE_PRESENTATION.equals(ce.getType())){
					// remove viewer movements
					for(TutorResponse tr: s.getTutorResponses(ce)){
						for(NodeEvent ne : s.getNodeEvents(tr)){
							deleteMessage(ne);
						}
						deleteMessage(tr);
					}
					deleteMessage(ce);
				}else{
					/*
					// else simply copy the event
					addClientEvent(ce,s);
					for(TutorResponse tr: s.getTutorResponses(ce)){
						for(NodeEvent ne : s.getNodeEvents(tr)){
							deleteMessage(ne);
						}
						deleteMessage(tr);
					}
					deleteMessage(ce);
					*/
				}
			}
		
		// for questions to identify diagnosis from a set of findings, mark all NON diagnoses w/ auto
		}else if(s.getCase().matches(".*PD_?\\d+.*")){
			output.println("processing "+s.getUsername()+" "+getCaseName(s.getCase())+" (add auto) ...");
			for(ClientEvent ce: s.getClientEvents()){
				if(!TYPE_DIAGNOSIS.equals(ce.getType()) && !TYPE_DONE.equals(ce.getType()) && !ce.isAuto()){
					addAutoFlag(ce);
					for(TutorResponse tr: s.getTutorResponses(ce)){
						for(NodeEvent ne: s.getNodeEvents(tr)){
							deleteMessage(ne);
						}
					}
				}
			}
		// for questions to link diagnoses to findings, mark all NON links as auto
		}else if(s.getCase().matches(".*PL_?\\d+.*")){
			output.println("processing "+s.getUsername()+" "+getCaseName(s.getCase())+" (add auto) ...");
			for(ClientEvent ce: s.getClientEvents()){
				if(!TYPE_SUPPORT_LINK.equals(ce.getType()) && !TYPE_DONE.equals(ce.getType())  && !ce.isAuto()){
					addAutoFlag(ce);
					for(TutorResponse tr: s.getTutorResponses(ce)){
						for(NodeEvent ne: s.getNodeEvents(tr)){
							deleteMessage(ne);
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * notify concept entry
	 * @param e
	 * @param action
	 */
	private void addClientEvent(ClientEvent ce, Session s){
		addClientEvent(ce, s, ce.getAction());
	}
	
	/**
	 * notify concept entry
	 * @param e
	 * @param action
	 */
	private void addClientEvent(ClientEvent ce, Session s,String action){
		TutorResponse tr = (!s.getTutorResponses(ce).isEmpty())?s.getTutorResponses(ce).get(0):null;
		NodeEvent ne = (!s.getNodeEvents(tr).isEmpty())?s.getNodeEvents(tr).get(0):null;
		
		if(TYPE_FINDING.equals(ce.getType())){
			ConceptEntry e = findingMap.get(ce.getObjectDescription());
			
			// resolve finding
			if(e == null){	
				e = ConceptEntry.getConceptEntry(ce.getObjectDescription());
				expert.resolveConceptEntry(e);
				findingMap.put(ce.getObjectDescription(),e);
			}
			
			
			int old_id = ce.getMessageId();
			
			// send message about this concept's feature
			e.getFeature().setParentEntry(e);
			ClientEvent c = getClientEvent(e.getFeature(),action,ce);
			
			// when finding is removed, send attribute deletes first
			if(!ACTION_REMOVED.equals(action)){
				
				insertMessage(c,s);
				TutorResponse r = getTutorResponse(tr,c);
				updateInterfaceEvents(old_id,c);
				insertMessage(r,s);
				insertMessage(getNodeEvent(ne,r),s);
			}
			// send messages in regards to the attributes
			for(ConceptEntry a: e.getAttributes()){
				a.setParentEntry(e);
				ClientEvent ac = getClientEvent(a,action,ce);
				insertMessage(ac,s);
				TutorResponse r = getTutorResponse(tr,ac);
				updateInterfaceEvents(old_id,ac);
				insertMessage(r,s);
				insertMessage(getNodeEvent(ne,r),s);
			}
			// when finding is removed, send attribute deletes first
			if(ACTION_REMOVED.equals(action)){
				insertMessage(c,s);
				TutorResponse r = getTutorResponse(tr,c);
				updateInterfaceEvents(old_id,c);
				insertMessage(r,s);
				insertMessage(getNodeEvent(ne,r),s);
			}
		}else{
			/*
			insertMessage(ce);
			TutorResponse r = getTutorResponse(tr,ce);
			updateInterfaceEvents(ce,ce);
			insertMessage(r);
			insertMessage(getNodeEvent(ne,r));
			*/
		}
		
	}
	
	
	
	
	
	/**
	 * get a client that describes this object for a given
	 * action
	 * @param action
	 * @return
	 */
	public ClientEvent getClientEvent(ConceptEntry e,String action, ClientEvent old){
		ClientEvent ce = new ClientEvent();
		ce.setType(e.getType());
		ce.setLabel(e.getLabel());
		ce.setAction(action);
		if(TYPE_ATTRIBUTE.equals(e.getType()))
			ce.setParent(e.getFeature().getObjectDescription());
		ce.setId(e.getId());
		ce.setObjectDescription(e.getObjectDescription());
		if(e.getParentEntry() != null)
			ce.setEntireConcept(e.getParentEntry().getObjectDescription());
		ce.setSource(old.getSource());
		ce.setTimestamp(old.getTimestamp());
		
		// set input based on action
		if(!TYPE_ATTRIBUTE.equals(e.getType()))
			ce.setInput(old.getInputMap());
		
		return ce;
	}
	
	/**
	 * get a client that describes this object for a given
	 * action
	 * @param action
	 * @return
	 */
	public TutorResponse getTutorResponse(TutorResponse t, ClientEvent c){
		if(t == null || c == null)
			return null;
		TutorResponse tr = new TutorResponse();
		tr.putAll(t);
		tr.setClientEvent(c);
		tr.setResponseConcept(c.getObjectDescription());
		return tr;
	}
	
	/**
	 * get a client that describes this object for a given
	 * action
	 * @param action
	 * @return
	 */
	public NodeEvent getNodeEvent(NodeEvent n, TutorResponse r){
		if(n == null || r == null)
			return null;
		NodeEvent ne = new NodeEvent();
		ne.putAll(n);
		ne.setTutorResponse(r);
		ne.setClientEvent(r.getClientEvent());
		ne.setType(r.getClientEvent().getType());
		ne.setLabel(r.getClientEvent().getLabel());
		ne.setParent(r.getClientEvent().getParent());
		ne.setObjectDescription(r.getClientEvent().getObjectDescription());
		ne.setAction(r.getClientEvent().getAction());
		return ne;
	}
	
	
	/**
	 * update interface events from old to new
	 * @param o
	 * @param n
	 */
	public void updateInterfaceEvents(int old_id, ClientEvent n){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String SQL = "UPDATE interface_event SET client_event_id = ? WHERE client_event_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setInt(1,n.getMessageId());
				st.setInt(2,old_id);	
				// execute
				st.executeUpdate();
				st.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void addAutoFlag(ClientEvent ce){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				//output.println("Auto: "+ce);
				ce.getInputMap().put("auto","true");
				
				// update tutor response
				String SQL = "UPDATE client_event SET input = ? WHERE client_event_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,ce.getInputString());
				st.setInt(2,ce.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
				
				SQL = "UPDATE client_event_input SET value = ? WHERE name = ? AND client_event_id = ?";
				conn = ((DatabaseProtocolModule) protocol).getConnection();
				st = conn.prepareStatement(SQL);
				st.setString(1,"true");
				st.setString(2,"auto");
				st.setInt(3,ce.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void insertMessage(Message ne,Session s){
		if(ne == null)
			return;
		
		if(protocol instanceof DatabaseProtocolModule){
			try {
				int session_id = Integer.parseInt(s.getSessionID());
				if(ne instanceof ClientEvent){
					int n = insertClientEvent((ClientEvent)ne,session_id);
					ne.setMessageId(n);
				}else if(ne instanceof InterfaceEvent){
					InterfaceEvent ie = (InterfaceEvent) ne;
					int n = insertInterfaceEvent(ie, session_id,ie.getClientEvent().getMessageId());
					ie.setMessageId(n);
				}else if(ne instanceof NodeEvent){
					NodeEvent tr = (NodeEvent) ne;
					int n = insertNodeEvent(tr, session_id,tr.getTutorResponse().getMessageId());
					tr.setMessageId(n);
				}else if(ne instanceof TutorResponse){
					TutorResponse tr = (TutorResponse) ne;
					int n = insertTutorResponse(tr, session_id,tr.getClientEvent().getMessageId());
					tr.setMessageId(n);
				}
				
				//output.println("Insert: "+ne);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void deleteMessage(Message ne){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				String type = null;
				if(ne instanceof ClientEvent)
					type = "client_event";
				else if(ne instanceof InterfaceEvent)
					type = "interface_event";
				else if(ne instanceof NodeEvent)
					type = "node_event";
				else if(ne instanceof TutorResponse)
					type = "tutor_response";
				else
					return;
				
				List<String> statements = new ArrayList<String>();
				statements.add("DELETE FROM "+type+" WHERE "+type+"_id = "+ne.getMessageId());
				statements.add("DELETE FROM "+type+" WHERE "+type+"_id = "+ne.getMessageId());
				// update tutor response
				for(String SQL: statements){
					Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
					Statement st = conn.createStatement();
					st.executeUpdate(SQL);
					st.close();
				}
				//output.println("Delete "+type+": "+ne);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private Connection getConnection() throws Exception {
		return ((DatabaseProtocolModule) protocol).getConnection();
	}
	
   /**
     * insert interface event
     * @param evt
     */
    private int insertClientEvent(ClientEvent msg, int session_id){
    	int id = -1;
    	try{
    		String SQL = 
    			"INSERT INTO client_event (type,label,action,parent,id,input,time_stamp,session_id,source,object_description) " +
    			"VALUES (?,?,?,?,?,?,?,?,?,?)";
    		
    		
    		// take care of parent entry
    		msg.addInput("entire_concept",msg.getEntireConcept());
    		
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL,new int [] {1});
			st.setString(1,filter(msg.getType(),256));
			st.setString(2,filter(msg.getLabel(),256));
			st.setString(3,filter(msg.getAction(),256));
			st.setString(4,filter(msg.getParent(),256));
			st.setString(5,filter(msg.getId(),256));
			st.setString(6,filter(msg.getInputString(),1024));
			//st.setTimestamp(7,new Timestamp(System.currentTimeMillis()));
			st.setTimestamp(7,new Timestamp(msg.getTimestamp()));
			st.setInt(8,session_id);
			st.setString(9,msg.getSource());
			st.setString(10,filter(msg.getObjectDescription(),512));
			
			// execute
			st.executeUpdate();
			
			// get id back
			ResultSet result = st.getGeneratedKeys();
			if(result.next())
				id = result.getInt(1);
			result.close();
			st.close();
			
			// create property map
			Map input = msg.getInputMap();
						
			// insert client event input
			if(!input.isEmpty() && id > -1){
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO client_event_input (name,value,client_event_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					// execute
					st.executeUpdate();
					st.close();
				}
				
			}
			
    	
    	}catch(Exception ex){
    		ex.printStackTrace();
		}
		return id;
    }  
    
    /**
     * update interface events
     * @param evt
     */
    private void updateInterfaceEvents(List<Integer> ieIDlist, int client_event_id){
    	try{
    		String s = ""+ieIDlist;
    		String SQL = "UPDATE interface_event SET client_event_id = "+client_event_id+
    					" WHERE interface_event_id IN "+s.replace('[','(').replace(']',')');
    		Connection conn = getConnection();
    		Statement st = conn.createStatement();
    		st.executeUpdate(SQL);
    		st.close();
    	}catch(Exception ex){
    		ex.printStackTrace();

    	}
    }
    
    /**
     * insert interface event
     * @param evt
     */
    private int insertInterfaceEvent(InterfaceEvent msg, int session_id, int client_event_id){
    	int id = -1;
    	try{
    		String sn = (client_event_id > -1)?",client_event_id":"";
    		String sq = (client_event_id > -1)?",?":"";
    		String SQL = 
    			"INSERT INTO interface_event (type,label,action,parent,id,input,time_stamp,session_id,source,object_description"+sn+") "+
    			"VALUES (?,?,?,?,?,?,?,?,?,?"+sq+")";
    		
    		// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL,new int [] {1});
			st.setString(1,filter(msg.getType(),256));
			st.setString(2,filter(msg.getLabel(),256));
			st.setString(3,filter(msg.getAction(),256));
			st.setString(4,filter(msg.getParent(),256));
			st.setString(5,filter(msg.getId(),256));
			st.setString(6,filter(msg.getInputString(),1024));
			//st.setTimestamp(7,new Timestamp(System.currentTimeMillis()));
			st.setTimestamp(7,new Timestamp(msg.getTimestamp()));
			st.setInt(8,session_id);
			st.setString(9,msg.getSource());
			st.setString(10,filter(msg.getObjectDescription(),512));
			if(client_event_id > -1)
				st.setInt(11,client_event_id);
			// execute
			st.executeUpdate();
			
			// get id back
			ResultSet result = st.getGeneratedKeys();
			if(result.next())
				id = result.getInt(1);
			result.close();
			st.close();
			
			// create property map
			Map input = msg.getInputMap();
			
			// insert client event input
			// I had a verly clever system in place to insert multiple rows at the same time, but
			// since Oracle doesn't support that syntax, I have to do one insert at a time
			if(!input.isEmpty() && id > -1){
				// set values
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO interface_event_input (name,value,interface_event_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					// execute
					st.executeUpdate();
					st.close();
				}
			}
    	}catch(Exception ex){
    		ex.printStackTrace();
			
		}
		return id;
    }
    
    /**
     * insert interface event
     * @param evt
     */
    private int insertTutorResponse(TutorResponse msg, int session_id, int client_event_id){
    	int id = -1;
    	try{
    		String sn = (client_event_id > -1)?",client_event_id":"";
    		String sq = (client_event_id > -1)?",?":"";
    		
    		String SQL = 
    			"INSERT INTO tutor_response (response_type,error_code,next_step_type,next_step_label," +
    			"next_step_action, next_step_parent,next_step_id,input,time_stamp,session_id,source,error_state"+sn+") " +
    			"VALUES (?,?,?,?,?,?,?,?,?,?,?,?"+sq+")";
    	
    		// add response concept to input
			msg.addInput("response_concept",msg.getResponseConcept());
    			
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL,new int [] {1});
			st.setString(1,filter(msg.getResponse(),256));
			st.setString(2,filter(msg.getCode(),256));
			st.setString(3,filter(msg.getType(),256));
			st.setString(4,filter(msg.getLabel(),256));
			st.setString(5,filter(msg.getAction(),256));
			st.setString(6,filter(msg.getParent(),256));
			st.setString(7,filter(msg.getId(),256));
			st.setString(8,filter(msg.getInputString(),1024));
			//st.setTimestamp(9,new Timestamp(System.currentTimeMillis()));
			st.setTimestamp(9,new Timestamp(msg.getTimestamp()));
			st.setInt(10,session_id);
			st.setString(11,msg.getSource());
			st.setString(12,filter(msg.getError(),512));
			if(client_event_id > -1)
				st.setInt(13,client_event_id);
			
			// execute
			st.executeUpdate();
			
			// get id back
			ResultSet result = st.getGeneratedKeys();
			if(result.next())
				id = result.getInt(1);
			result.close();
			st.close();
		
			// create property map
			Map input = msg.getInputMap();
				
			// insert client event input
			if(!input.isEmpty() && id > -1){
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO tutor_response_input (name,value,tutor_response_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					//execute
					st.executeUpdate();
					st.close();
				}
				
			}
    	}catch(Exception ex){
    		ex.printStackTrace();
		}
		return id;
    }


    /**
     * insert interface event
     * @param evt
     */
    private int insertNodeEvent(NodeEvent msg, int session_id, int tutor_response_id){
    	int id = -1;
    	try{
    		String sn = (tutor_response_id > -1)?",tutor_response_id":"";
    		String sq = (tutor_response_id > -1)?",?":"";

    		// check for old representation
    		String p = "";
    		//if(oldRepresentation)
    		//	p="node_event_";
    		
    		String SQL = 
    			"INSERT INTO node_event ("+p+"type,"+p+"label,action,"+p+"parent,response_type,error_state," +
    					"error_code,one_to_many,many_to_many,is_absent,input,source,time_stamp,session_id"+sn+") " +
    			"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?"+sq+")";
    		
    		// add entire concept to input
			msg.addInput("entire_concept",msg.getEntireConcept());
			msg.addInput("object_description",msg.getObjectDescription());
    		
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL,new int [] {1});
			st.setString(1,filter(msg.getType(),256));
			st.setString(2,filter(msg.getLabel(),256));
			st.setString(3,filter(msg.getAction(),256));
			st.setString(4,filter(msg.getParent(),256));
			
			st.setString(5,filter(msg.getResponse(),256));
			st.setString(6,filter(msg.getError(),512));
			st.setString(7,filter(msg.getCode(),256));
					
			st.setString(8,filter(msg.getOneToMany(),4000));
			st.setString(9,filter(msg.getManyToMany(),4000));
			st.setString(10,filter(""+msg.isAbsent(),16));
			
			st.setString(11,filter(msg.getInputString(),1024));
			st.setString(12,msg.getSource());
			
			st.setTimestamp(13,new Timestamp(msg.getTimestamp()));
			st.setInt(14,session_id);
			if(tutor_response_id > -1)
				st.setInt(15,tutor_response_id);
			
			// execute
			st.executeUpdate();
			
			// get id back
			ResultSet result = st.getGeneratedKeys();
			if(result.next())
				id = result.getInt(1);
			result.close();
			st.close();
		
			// create property map
			Map input = msg.getInputMap();
			
			// insert client event input
			if(!input.isEmpty() && id > -1){
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO node_event_input (name,value,node_event_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					//execute
					st.executeUpdate();
					st.close();
				}
				
			}
    	}catch(Exception ex){
    		ex.printStackTrace();
		}
		return id;
    }

    /**
     * filter input string
     * @param field
     * @param limit
     * @return
     */
    private String filter(String field, int limit){
    	if(field == null || limit < 0)
    		return field;
    	int l = field.length();
    	return field.substring(0,(l > limit)?limit:l);
    }
}
