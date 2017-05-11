package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.ACTION_SELF_CHECK;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class H3FixColorInconsistancy implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule protocol;
	
	public void dispose() {
	}

	public String getDescription() {
		return "In H3 fix if coloring the same thing with the same color produces different tutor response";
	}
	
	public String getName() {
		return "H3 Fix Color Response Inconsistency";
	}
	public String toString(){
		return getName();
	}
	
	public void initialize() {
	}

	public boolean process(Session session) {
		Map<String,String> colorMap = new HashMap<String,String>();
		for(ClientEvent ce: session.getClientEvents()){
			if(ACTION_SELF_CHECK.equals(ce.getAction())){
				String key = ce.getObjectDescription()+"="+ce.getInputMap().get("fok");
				String value = session.getTutorResponses(ce).get(0).getResponse();
				if(colorMap.containsKey(key)){
					// if response is not equal, shit output
					if(!value.equals(colorMap.get(key))){
						output.println("Fixing "+session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+ce.getObjectDescription());
						updateDB(session,session.getTutorResponses(ce).get(0),colorMap.get(key));
					}
				}else{
					colorMap.put(key,value);
				}
			}
		}
		
		return true;
	}

	private void updateDB(Session s, TutorResponse tr, String response){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				// update tutor response
				String SQL = "UPDATE tutor_response SET response_type = ? WHERE tutor_response_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setString(1,response);
				st.setInt(2,tr.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
				// update  node event
				NodeEvent ne = null;
				for(NodeEvent n: s.getNodeEvents(tr)){
					ne = n;
					break;
				}
				if(ne != null){
					SQL = "UPDATE node_event SET response_type = ? WHERE node_event_id = ?";
					conn = ((DatabaseProtocolModule) protocol).getConnection();
					st = conn.prepareStatement(SQL);
					st.setString(1,response);
					st.setInt(2,ne.getMessageId());	
					// execute
					st.executeUpdate();
					st.close();
				}
				
				
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
