package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.presentation.DynamicBook;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;


public class H3CleanProtocol implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	private ExpertModule expert;
	
	public void dispose() {
	}

	public String getDescription() {
		return "Address H3 problems and fix them in place";
	}

	public String getName() {
		return "H3 Clean Protocol";
	}
	public String toString(){
		return getName();
	}
	public void initialize() {
		expert = new DomainExpertModule();
		expert.load();
	}

	public boolean process(Session session) {
		String s = session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase());
		
		// find orphaned node events
		/*
		for(NodeEvent ne : session.getNodeEvents()){
			if(ne.getTutorResponseId() <= 0){
				//find corressponding tutor response
				for(TutorResponse tr: session.getTutorResponses()){
					if(tr.getTimestamp() == ne.getTimestamp()){
						//output.println("updating node event: "+s);
						updateNodeEvent(session, ne,tr);
					}
				}
				
					
			}
		}
		*/
		output.println("processing "+s+" ...");
		
		// fix carried over next step
		List<Integer> header = new ArrayList<Integer>();
		for(TutorResponse tr: session.getTutorResponses()){
			if(tr.getResponseConcept().startsWith("TutorViewer") || tr.getResponseConcept().startsWith("Question") || tr.getResponseConcept().startsWith("Info")){
				header.add(tr.getMessageId());
			}else{
				break;
			}
		}
		// update trs from overflow
		if(!header.isEmpty()){
			TutorResponse etr = getFirstNextStep(session);
			if(etr != null)
				updateTutorResponseNextStep(session,etr,header);
			//output.println("updating tutor events with next step "+header);
		}
		
		
		// fix next step from coloring book
		/*
		header.clear();
		TutorResponse lastTR = null;
		for(ClientEvent ce: session.getClientEvents()){
			// on self check 
			if(ACTION_SELF_CHECK.equals(ce.getAction())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					header.add(tr.getMessageId());
				}
			}else{
				if(!header.isEmpty())
					break;
				for(TutorResponse tr: session.getTutorResponses(ce)){
					lastTR = tr;
				}
			}
		}
		
		if(lastTR != null && !header.isEmpty()){
			updateTutorResponseNextStep(session,lastTR,header);
			//output.println("updating tutor events with next step "+header);
		}
		*/
		
		// fix delete next step concept id issue 
		/*
		String conceptId = null;
		for(TutorResponse tr: session.getTutorResponses()){
			if(ACTION_REMOVED.equals(tr.getAction())){
				if(TextHelper.isEmpty(tr.getId()) && conceptId != null){
					updateTutorResponse(session,tr,"next_step_id",conceptId);
					output.println("updating tutor response: "+tr+" with "+conceptId);
				}else{
					conceptId = tr.getId();
				}
			}else if(ACTION_ADDED.equals(tr.getAction()) && RESPONSE_FAILURE.equals(tr.getResponse()) && !TextHelper.isEmpty(tr.getId())){
				// in this case just switch add to remove
				updateTutorResponse(session,tr,"next_step_action",ACTION_REMOVED);
				output.println("updating tutor response: "+tr+" with "+ACTION_REMOVED);
			}else if(ACTION_ADDED.equals(tr.getAction()) && !TextHelper.isEmpty(tr.getId())){
				updateTutorResponse(session,tr,"next_step_id",null);
				output.println("add w/ id "+tr);
			}
		}
		
		// fix support link parent issue
		String parent = null;
		for(TutorResponse tr: session.getTutorResponses()){
			if(TYPE_SUPPORT_LINK.equals(tr.getType())){
				// if support link and failure and response concept is link, then remember
				if(RESPONSE_FAILURE.equals(tr.getResponse()) && tr.getResponseConcept().contains(":SupportLink")){
					parent = tr.getResponseConcept();
					parent = parent.substring(0,parent.indexOf(":SupportLink"));
				}
				
				if(TextHelper.isEmpty(tr.getParent()) && parent != null){
					updateTutorResponse(session,tr,"next_step_parent",parent);
					output.println("updating tutor response: "+tr+" with "+parent);
				}
			}
		}
		*/
		
	
		
		return true;
	}


	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}
	
	/**
	 * get first next step
	 * @param s
	 * @return
	 */
	private TutorResponse getFirstNextStep(Session s){
		// if domain is different
		if(!s.getDomain().equals(expert.getDomain())){
			expert.openDomain(s.getDomain());
		}
		CaseEntry e = expert.getCaseEntry(s.getCase());
		if(e != null && !e.getConcepts(DIAGNOSTIC_FEATURES).isEmpty()){
			ConceptEntry f = e.getConcepts(DIAGNOSTIC_FEATURES).getValues().get(0);
			//expert.resolveConceptEntry(f);
			
			TutorResponse tr = new TutorResponse();
			tr.setType(f.getType());
			tr.setLabel(getFeature(getConceptClass(f,expert.getDomainOntology())).getName());
			tr.setAction(ACTION_ADDED);
			
			return tr;
			
		}
		return null;
	}
	
	
	/**
	 * update node event
	 * @param s
	 * @param ne
	 * @param tr
	 * @return
	 */
	private boolean updateNodeEvent(Session s, NodeEvent ne,TutorResponse tr){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				String SQL = "UPDATE node_event SET tutor_response_id = ? WHERE node_event_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setInt(1,tr.getMessageId());
				st.setInt(2,ne.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	
	private boolean updateTutorResponse(Session s, TutorResponse tr, String field, String value){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				String SQL = "UPDATE tutor_response SET "+field+" = ? WHERE tutor_response_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,value);
				st.setInt(2,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * update node event
	 * @param s
	 * @param ne
	 * @param tr
	 * @return
	 */
	private boolean updateTutorResponseNextStep(Session s, TutorResponse tr, List<Integer> tr_ids){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				String next = "next_step_type = ? , next_step_label = ?, next_step_action = ? , next_step_parent = ?, next_step_id = ?";
				String SQL = "UPDATE tutor_response SET "+next+" WHERE tutor_response_id IN ("+TextHelper.toString(tr_ids)+")";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,tr.getType());
				st.setString(2,tr.getLabel());
				st.setString(3,tr.getAction());
				st.setString(4,tr.getParent());
				st.setString(5,tr.getId());
				// execute
				st.executeUpdate();
				st.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	

	/**
	 * update node event
	 * @param s
	 * @param ne
	 * @param tr
	 * @return
	 */
	private boolean insertTutorResponse(Session s, TutorResponse tr){
		if(protocol instanceof DatabaseProtocolModule){
			//((DatabaseProtocolModule) protocol).insertTutorResponse(tr,Integer.parseInt(s.getSessionID()),tr.getClientEventId());
		}
		return true;
	}


}
