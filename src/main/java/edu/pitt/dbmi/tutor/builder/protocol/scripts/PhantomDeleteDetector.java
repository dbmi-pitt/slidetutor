package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;

public class PhantomDeleteDetector implements ProtocolScript {
	private PrintStream output;
	
	public String getDescription() {
		return "Identify DELETE ClientEvents that were not in fact removed, because they were accessed afterwords";
	}

	public String getName() {
		return "Phantom Delete Detector";
	}
	
	public String toString(){
		return getName();
	}
	

	public void dispose() {
	
	}
	public void initialize() {
	}

	public boolean process(Session s) {
		Set<String> removedConcepts = new HashSet<String>();
		Set<String> addedConcepts = new HashSet<String>();
		
		// iterate over client events
		for(ClientEvent ce: s.getClientEvents()){
			String desc = ce.getObjectDescription();
			
			// if item was removed, save its object description
			if(ce.getAction().equalsIgnoreCase("delete")){
				removedConcepts.add(desc);
			}else if(removedConcepts.contains(desc)){
				// else something was mentioned about item, but it is considered to be removed
				output.println("SAME ID , "+s.getUsername()+" , "+s.getCase()+" , "+s.getSessionID()+" , "+desc);
			}
			
			// if this is an add action, then remember it
			if(ce.getAction().equalsIgnoreCase(ce.getType()) || ce.getAction().contains("Evidence") || ce.getAction().contains("Attribute")){
				addedConcepts.add(ce.getObjectDescription());
			}else if(ce.getAction().equalsIgnoreCase("delete") && !addedConcepts.contains(ce.getObjectDescription())){
				// if delete of a concept that is not
				output.println("NEW ID ,"+s.getUsername()+" , "+s.getCase()+" , "+s.getSessionID()+" , "+ce.getObjectDescription());
			}
			
			
			
			// if item is a support link, make sure that none of parts are "Deleted"
			/*
			if(ce.getType().toLowerCase().contains("link") && ce.getParent() != null){
				for(String p : ce.getParent().split(":")){
					if(removedConcepts.contains(p)){
						// else something was mentioned about item, but it is considered to be removed
						output.println(s.getUsername()+" , "+s.getCase()+" , "+s.getSessionID()+" , "+p);
					}
				}
			}*/
		}
		
		return true;
	}

	public void setOutput(PrintStream out) {
		output = out;
	}

	public void setProtocolModule(ProtocolModule m) {
		// TODO Auto-generated method stub
		
	}

}
