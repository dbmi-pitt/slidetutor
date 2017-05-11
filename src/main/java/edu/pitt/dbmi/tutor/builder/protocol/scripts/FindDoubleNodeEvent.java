package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;


import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;

public class FindDoubleNodeEvent implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	
	public void dispose() {
	}

	public String getDescription() {
		return "Find Double NodeEvent";
	}
	
	public String getName() {
		return "Finding Double NodeEvent";
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

	
	
	public boolean process(Session s) {
		NodeEvent lastNE = null;
		boolean duplication = false;
		List<NodeEvent> nes = new ArrayList<NodeEvent>();
		for(NodeEvent ne: s.getNodeEvents()){
			if(lastNE != null && 
					lastNE.getType().equals(ne.getType()) && 
					lastNE.getLabel().equals(ne.getLabel()) && 
					lastNE.getAction().equals(ne.getAction()) && lastNE.isAbsent() == ne.isAbsent() &&
					!Arrays.asList(ACTION_REFINE,TYPE_HINT,ACTION_SELF_CHECK).contains(ne.getAction())){
				//if(!ne.getError().contains("Became Inconsistent"))
				if(!duplication){
					output.println(s.getUsername()+" "+getCaseName(s.getCase())+" "+s.getSessionID());
					output.println("\t"+lastNE);
				}
				duplication = true;
			}else{
				duplication = false;
			}
			
			if(duplication){
				output.println("\t"+ne);
				nes.add(ne);
			}
			lastNE = ne;
		}
		
		// now remove em
		if(!nes.isEmpty()){
			output.println("removing "+nes.size()+" node events ...");
			for(NodeEvent ne: nes)
				deleteNodeEvent(s, ne);
		}
		
		return true;
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
