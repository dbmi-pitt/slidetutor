package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class HandleOrphanedEvents implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	
	public void dispose() {
		}

	public String getDescription() {
		return "Handle orphaned events such as Interface Events, Tutor Responses and Node Events";
	}

	public String getName() {
		return "Handle Orphaned Events";
	}
	public String toString(){
		return getName();
	}

	public void initialize() {
			}

	public boolean process(Session session) {
		String s = session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase());
		// find orphaned node events
		/*
		for(NodeEvent ne : session.getNodeEvents()){
			if(ne.getTutorResponseId() <= 0){
				output.println("orphaned NE in "+s+" | "+ne);
				//deleteNodeEvent(session, ne);
			}
		}*/
		for(ClientEvent ce : session.getClientEvents()){
			if(session.getTutorResponses(ce).isEmpty()){
				output.println("no TR for CE in "+s+" | "+ce);
			}
		}
		

		return true;
	}

	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}

	private void deleteNodeEvent(Session s, NodeEvent ne){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				List<String> statements = new ArrayList<String>();
				statements.add("DELETE FROM node_event_input WHERE node_event_id = "+ne.getMessageId());
				statements.add("DELETE FROM node_event WHERE node_event_id = "+ne.getMessageId());
				// update tutor response
				for(String SQL: statements){
					Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
					Statement st = conn.createStatement();
					st.executeUpdate(SQL);
					st.close();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
