package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.ACTION_ADDED;
import static edu.pitt.dbmi.tutor.messages.Constants.ACTION_REFINE;
import static edu.pitt.dbmi.tutor.messages.Constants.TYPE_FINDING;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class ClientEventDurations implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	
	public void dispose() {
	}

	public String getDescription() {
		return "Output all ClientEvents with durations for each event in seconds";
	}
	
	public String getName() {
		return "ClientEvent Durations";
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
		int n = session.getClientEvents().size();
		for(int i=0;i<n;i++){
			ClientEvent ce = session.getClientEvents().get(i);
			long next = session.getFinishTime().getTime();
			if(i < n-1)
				next = session.getClientEvents().get(i+1).getTimestamp();
			
			output.println(session.getUsername()+"; "+OntologyHelper.getCaseName(session.getCase())+"; "+session.getSessionID()+"; "+
					ce.getMessageId()+"; "+ce.getType()+"; "+ce.getLabel()+"; "+ce.getAction()+"; "+new Date(ce.getTimestamp())+"; "+((double)(next-ce.getTimestamp()))/1000.0);
		}
		return true;
	}

	

}
