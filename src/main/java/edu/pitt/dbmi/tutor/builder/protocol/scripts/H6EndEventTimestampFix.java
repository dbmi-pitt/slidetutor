package edu.pitt.dbmi.tutor.builder.protocol.scripts;


import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

public class H6EndEventTimestampFix implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	
	public void dispose() {
	}

	public String getDescription() {
		return "In H6 fix the timestamp of the End InterfaceEvent if timestamp doesn't match the one at the end";
	}
	
	public String getName() {
		return "H6 Fix End InterfaceEvent Timestamp";
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
		for(InterfaceEvent ie : session.getInterfaceEvents()){
			if(TYPE_END.endsWith(ie.getType())){
				// it timestamp of the end is greater then session finish time, trim session finish time
				if(ie.getTimestamp() > session.getFinishTime().getTime()){
					output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+session.getFinishTime().getTime()+" "+ie.getTimestamp());
					updateDB(session, ie, session.getFinishTime().getTime());
				}
				break;
			}
		}
		return true;
	}

	private void updateDB(Session s, InterfaceEvent ie, long time){
		if(protocol instanceof DatabaseProtocolModule){
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
		}
	}
	
}
