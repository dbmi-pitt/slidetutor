package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Arrays;

import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class FindingNoLocation implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	
	public void dispose() {
	}

	public String getDescription() {
		return "Find ClientEvents that have no location in input";
	}
	
	public String getName() {
		return "Finding No Location";
	}
	public String toString(){
		return getName();
	}
	
	public void initialize() {
	}


	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}

	
	
	public boolean process(Session session) {
		if(session.getCase().matches(".*P[FDLC]_?\\d+.*"))
			return true;
		
		for(ClientEvent ce: session.getClientEvents()){
			if(TYPE_FINDING.equals(ce.getType()) && Arrays.asList(ACTION_ADDED,ACTION_REFINE).contains(ce.getAction())){
				if(!ce.getInputMap().containsKey("location") && !ce.isAuto()){
					output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+session.getSessionID()+" "+
					ce.getMessageId()+" "+ce.getType()+" "+ce.getLabel()+" "+ce.getAction()+" "+ce.getInputString());
				}
			}
		}
		return true;
	}

	private void updateDB(Session s, InterfaceEvent ie, long time){
		if(protocol instanceof DatabaseProtocolModule){
			/*
			try {
				// update tutor response
				String SQL = "UPDATE interface_event SET time_stamp = ? WHERE interface_event_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setTimestamp(1,new Timestamp(time));
				st.setInt(2,ie.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
		}
	}
	

}
