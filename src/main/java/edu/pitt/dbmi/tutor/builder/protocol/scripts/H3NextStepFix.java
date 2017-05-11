package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.ACTION_SELF_CHECK;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;


public class H3NextStepFix implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule protocol;
	
	public void dispose() {
	}

	public String getDescription() {
		return "In H3 sometimes you have TutorViewer or ClinicalInfo mess up next step";
	}
	
	public String getName() {
		return "H3 Fix NextStep Inconsistency";
	}
	public String toString(){
		return getName();
	}
	
	public void initialize() {
	}

	public boolean process(Session session) {
		String [] nextStep = new String [5];
		boolean done = false;
		List<Integer> list = new ArrayList<Integer>();
		for(ClientEvent ce: session.getClientEvents()){
			// give up after done
			if(TYPE_DONE.equals(ce.getType())){
				done = true;
			}
			
			// before 1st done
			if(!done){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					// if CE is something mundane like TutorViewer or INFO
					if(Arrays.asList(TYPE_PRESENTATION,TYPE_INFO).contains(ce.getType()) && nextStep[0] != null){
						// if next step for INFO and presentation doesn't match next step before, then
						if(!(eq(tr.getType(),nextStep[0]) && eq(tr.getLabel(),nextStep[1]) && 
							eq(tr.getAction(),nextStep[2]) && eq(tr.getParent(),nextStep[3]) && eq(tr.getId(),nextStep[4]))){
							//next step doesn't match , report
							if(tr.getType().equals(nextStep[0]) && tr.getLabel().equals(nextStep[1])){
								output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+
								tr.getMessageId()+" "+tr.getType()+"."+tr.getLabel()+"."+tr.getAction()+" "+tr.getParent()+" "+tr.getId()+" "+
										Arrays.toString(nextStep));
								updateDB(session, tr,nextStep[2],nextStep[4]);
							}else{
								//output.println("FUCK!!!");
							}
						}
					}else{
						// remember next step from real step
						nextStep[0] = tr.getType();
						nextStep[1] = tr.getLabel();
						nextStep[2] = tr.getAction();
						nextStep[3] = tr.getParent();
						nextStep[4] = tr.getId();
					}	
				}
			// after 1st done	
			}else{
				// reset next step for all other events after 1st done
				for(TutorResponse tr: session.getTutorResponses(ce)){
					list.add(tr.getMessageId());
				}
			}
		}
		
		// update with next step
		if(!list.isEmpty()){
			/*
			output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" set next step in "+
					list.size()+" TRs to "+Arrays.toString(nextStep));
			updateDB(session, nextStep, list);
			*/
		}
		
		
		return true;
	}
	private void updateDB(Session s, TutorResponse tr, String action, String id){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String SQL = "UPDATE tutor_response SET next_step_action = ? , next_step_id = ? WHERE tutor_response_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,filter(action));
				st.setString(2,filter(id));
				st.setInt(3,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void updateDB(Session s, String [] nextStep, List<Integer> list){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String ids = list.toString();
				ids = ids.substring(1,ids.length()-1);
				String SQL = "UPDATE tutor_response SET next_step_type = ?, next_step_label = ?, next_step_action = ? , " +
						"next_step_parent = ?, next_step_id = ? WHERE tutor_response_id IN ("+ids+" )";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,filter(nextStep[0]));
				st.setString(2,filter(nextStep[1]));
				st.setString(3,filter(nextStep[2]));
				st.setString(4,filter(nextStep[3]));
				st.setString(5,filter(nextStep[4]));
				
				// execute
				st.executeUpdate();
				st.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private String filter(String val){
		return val == null?"":val;
	}
	
	private boolean eq(String a, String b){
		if(a == null && b == null)
			return true;
		if(a == null ^ b == null)
			return false;
		return a.equals(b);
	}
	
	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}


}
