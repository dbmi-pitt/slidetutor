package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.TextHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;


public class FixRefineClientEvent implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule protocol;
	
	public void dispose() {
		}

	public String getDescription() {
		return "Find Refine ClientEvents that have a comma in them and fix them";
	}

	public String getName() {
		return "Fix Refine ClientEvent";
	}

	public String toString(){
		return getName();
	}
	
	public void initialize() {
	
	}

	public boolean process(Session session) {
		Map<String,String> map = new HashMap<String, String>();
		for(ClientEvent ce : session.getClientEvents()){
			
			// have a map of entirefinding to feature 
			if(TYPE_FINDING.equals(ce.getType())){
				if(ACTION_ADDED.equals(ce.getAction())){
					//what if we have several findings w/ same feature?
					if(map.containsKey(ce.getLabel()))
						map.put(ce.getLabel(),map.get(ce.getLabel())+" & "+ce.getObjectDescription());
					else	
						map.put(ce.getLabel(),ce.getObjectDescription());
				}else if(ACTION_REMOVED.equals(ce.getAction())){
					map.remove(ce.getLabel());
				}
			}
			
			// find refines that have commas in label
			if(ACTION_REFINE.equals(ce.getAction())){
				String lbl = ce.getLabel();
				if(lbl.contains(",")){
					String fd = map.get(lbl.substring(0,lbl.indexOf(",")));
					if(fd.contains("&"))
						System.err.println("Shit!!!"+session.getUsername()+" "+getCaseName(session.getCase())+"| "+fd+" | "+ce);
					output.println(session.getUsername()+" "+getCaseName(session.getCase())+"| "+fd+" | "+ce);
					
					// correct client event
					ConceptEntry e = ConceptEntry.getConceptEntry(fd);
					ce.setLabel(e.getLabel());
					ce.setId(e.getId());
					ce.setObjectDescription(e.getObjectDescription());
					updateMessage(session, ce,Arrays.asList("label","id","object_description"));
					
					
					for(TutorResponse tr: session.getTutorResponses(ce)){
						tr.setResponseConcept(e.getObjectDescription());
						tr.getInputMap().put("response_concept",e.getObjectDescription());
						tr.setLabel(e.getLabel());
						tr.setId(e.getId());
						
						updateResponseConcept(session, tr);
						updateMessage(session,tr,Arrays.asList("label","id"));
						
						for(NodeEvent ne: session.getNodeEvents(tr)){
							ne.setLabel(e.getLabel());
							updateMessage(session,ne,Arrays.asList("label"));
						}
					}
				}
			}
			
			// see if we have weird response concepts on add
			/*
			if(!session.getTutorResponses(ce).isEmpty()){
				TutorResponse tr = session.getTutorResponses(ce).get(0);
				// we have mismatch between tutor respone concept and ce
				if(!ce.getObjectDescription().equals(tr.getResponseConcept()) && !ce.getObjectDescription().endsWith("null"))
					output.println("mismatch "+session.getUsername()+" "+getCaseName(session.getCase())+"|"+ce.getObjectDescription()+" | "+tr.getResponseConcept()+" "+ce);
			}
			*/
			
		}
		return true;
	}
	
	public void setOutput(PrintStream out) {
		output = out;
	}

	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}

	
	private void updateResponseConcept(Session s, TutorResponse tr){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String SQL = "UPDATE tutor_response SET input = ? WHERE tutor_response_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,tr.getInputString());
				st.setInt(2,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
				
				SQL = "UPDATE tutor_response_input SET value = ? WHERE name = ? AND tutor_response_id = ?";
				conn = ((DatabaseProtocolModule) protocol).getConnection();
				st = conn.prepareStatement(SQL);
				st.setString(1,tr.getResponseConcept());
				st.setString(2,"response_concept");
				st.setInt(3,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private void updateMessage(Session s, Message msg, List<String> fields){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				String type = null;
				if(msg instanceof ClientEvent)
					type = "client_event";
				else if(msg instanceof InterfaceEvent)
					type = "interface_event";
				else if(msg instanceof NodeEvent)
					type = "node_event";
				else if(msg instanceof TutorResponse)
					type = "tutor_response";
				else
					return;
				
				String prefix = ("tutor_response".equals(type))?"next_step_":"";
				
				// update tutor response
				StringBuffer SQL = new StringBuffer("UPDATE "+type+" SET ");
				//+field1+" = ? , "+field2+" = ? WHERE client_event_id = ?";
				for(String f : fields)
					SQL.append(prefix+f+" = ? ,");
				SQL.deleteCharAt(SQL.length()-1);
				SQL.append(" WHERE "+type+"_id = ?");
				
				
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL.toString());
				int n = 1;
				for(String f: fields)
					st.setString(n++,msg.get(f));
				st.setInt(n,msg.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
