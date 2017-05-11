package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;


import java.io.PrintStream;
import java.util.Arrays;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class FindDeleteAfterDone implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	
	public void dispose() {
	}

	public String getDescription() {
		return "Find Delete After Test Case is Done";
	}
	
	public String getName() {
		return "Finding Delete After Done";
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
		//if(session.getCase().matches(".*P[FDLC]_?\\d+.*"))
		//	return true;
		
		boolean done = false;
		for(ClientEvent ce: session.getClientEvents()){
			if(done){
				output.println(session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" "+session.getSessionID()+" "+ce);
			}
			if(TYPE_DONE.equals(ce.getType())){
				done = true;
			}
			
		}
		return true;
	}

}
