package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class H5FixRefineResponseConcept implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule protocol;
	
	public void dispose() {
	}

	public String getDescription() {
		return "In H5 fix CE object_description inconsistency on Refine action with TR response_concept";
	}
	
	public String getName() {
		return "H5 Fix Refine Response Concept";
	}
	public String toString(){
		return getName();
	}
	
	public void initialize() {
	}

	public boolean process(Session session) {
		String s = session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase());
		output.println("processing "+s+" ...");
		boolean inHint = false;
		String [] nextStep = new String [3];
		
		for(ClientEvent ce: session.getClientEvents()){
			if(ACTION_REFINE.equals(ce.getAction()) || ACTION_ADDED.equals(ce.getAction())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					// change responce concept
					if(!ce.getObjectDescription().equals(tr.getResponseConcept())){ // && ce.getEntireConcept().equals(tr.getResponseConcept())){
						tr.setResponseConcept(ce.getObjectDescription());
						tr.getInputMap().put("response_concept",ce.getObjectDescription());
						output.println("  updating response to "+ce.getObjectDescription());
						updateResponseConcept(session,tr);
					}
					
					// now make sure that next step is also changed
					if(ACTION_REFINE.equals(tr.getAction()) && RESPONSE_FAILURE.equals(tr.getResponse()) && !ce.getLabel().equals(tr.getLabel())){
						tr.setLabel(ce.getLabel());
						tr.setId(ce.getId());
						output.println("  updating response next step for REFINE to "+ce.getObjectDescription());
						updateTutorResponse(session, tr,"label","id");
					}
				}
			}
			
			// remember failures for location
			if(Arrays.asList(ACTION_REFINE,ACTION_ADDED).contains(ce.getAction()) && TYPE_FINDING.equals(ce.getType())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && tr.getError().matches("Incorrect .* Location")){
						nextStep[0] = ce.getType();
						nextStep[1] = ce.getLabel();
						nextStep[2] = ce.getId();
					}
				}
			}
			
			// now look at hints
			if(TYPE_HINT.equals(ce.getType())){
				inHint = false;
				for(TutorResponse tr: session.getTutorResponses(ce)){
					if(tr.getError().matches("Incorrect .* Location")){
						inHint = true;
						if(tr.getType().equals(nextStep[0]) && tr.getLabel().equals(nextStep[1])){
							tr.setAction(ACTION_REFINE);
							tr.setId(nextStep[2]);
							output.println("  updating response next step on HINT "+ce.getObjectDescription()+" to "+nextStep[0]+"."+nextStep[1]+"."+nextStep[2]);
							updateTutorResponse(session, tr,"action","id");
						}
						
					}
				}
			}else if(inHint && TYPE_HINT_LEVEL.equals(ce.getType())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					if(tr.getType().equals(nextStep[0]) && tr.getLabel().equals(nextStep[1])){
						tr.setAction(ACTION_REFINE);
						tr.setId(nextStep[2]);
						output.println("  updating response next step on HINT_LEVEL "+ce.getObjectDescription()+" to "+nextStep[0]+"."+nextStep[1]+"."+nextStep[2]);
						updateTutorResponse(session, tr,"action","id");
					}
				}
			}
		}
	
		
		return true;
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
	
	private void updateTutorResponse(Session s, TutorResponse tr, String field1, String field2){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String SQL = "UPDATE tutor_response SET next_step_"+field1+" = ? , next_step_"+field2+" = ? WHERE tutor_response_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,tr.get(field1));
				st.setString(2,tr.get(field2));
				st.setInt(3,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}
}
