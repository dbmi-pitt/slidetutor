package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;

import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

public class H3SkillometerTime implements ProtocolScript {
	private ProtocolModule protocol;
	private PrintStream output;
	private String LABEL = "Meta Cognitive Skillometer";
	
	public void dispose() {
	}

	public String getDescription() {
		return "H3 Calculate Skillometer Dilaog Gap";
	}
	
	public String getName() {
		return "H3 Skillometer Time";
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
		long time = 0;
		long duration = 0;
		boolean closed = false;
		
		for(InterfaceEvent ie: session.getInterfaceEvents()){
			time = ie.getTimestamp();
			if(TYPE_DIALOG.equals(ie.getType()) && LABEL.equals(ie.getLabel())){
				if(ACTION_OPENED.equals(ie.getAction())){
					duration = time;
				}else if(ACTION_CLOSED.equals(ie.getAction())){
					duration = time - duration;
					closed = true;
					break;
				}
			}
		}
		
		// what if we don't have close event, then simply use a timestamp of the last interface event
		// usually End
		if(!closed && duration > 0)
			duration = time - duration;
		
		int sec = (int) duration / 1000;
		output.println(session.getUsername()+","+OntologyHelper.getCaseName(session.getCase())+","+session.getSessionID()+","+sec);
		return true;
	}
}
