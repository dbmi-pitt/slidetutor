package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;

public class H1LinkIEtoCE implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule protocol;
	
	public String getDescription() {
		return "In H1, InterfaceEvents are not tied to ClientEvents. This script fixes it on the selected dataset";
	}

	public String getName() {
		return "H1 Line InterfaceEvents to ClientEvents";
	}
	
	public String toString() {
		return getName();
	}
	public void initialize() {

	}
	
	public void dispose() {
	
	}

	public boolean process(Session session) {
		Queue<InterfaceEvent> interfaceEventQueue = new LinkedList<InterfaceEvent>();
		// go over client events
		List<Message> events = new ArrayList<Message>();
		events.addAll(session.getInterfaceEvents());
		events.addAll(session.getClientEvents());
		Collections.sort(events,new Comparator<Message>() {
			public int compare(Message o1, Message o2) {
				int x = (int) (o1.getTimestamp() - o2.getTimestamp());
				if(x == 0){
					if(o1 instanceof InterfaceEvent)
						return -1;
					else if(o2 instanceof InterfaceEvent)
						return 1;
				}
				return x;
			}
		});
		output.println("Copying session for user " + session.getUsername() + " id " + session.getSessionID() + " ....");
		for(Message msg : events){
			// queue IE
			if(msg instanceof InterfaceEvent){
				InterfaceEvent ie = (InterfaceEvent) msg;
				//IF CE ID not set, we want to queue it
				if(ie.getClientEventId() <= 0)
					interfaceEventQueue.add(ie);
			}else if(msg instanceof ClientEvent){
				ClientEvent ce = (ClientEvent) msg;
				
				// now if client event is a viewer event, than everything
				// in a queue (if anything is there is not related to it) hence should be cleared
				if(ce.getType().equalsIgnoreCase("TutorViewer")){
					interfaceEventQueue.clear();
					continue;
				}
								
				// now lets take a look at queued up interface events
				// most client events will start with button press
				while(true){
					InterfaceEvent ie = interfaceEventQueue.poll();
					if(ie == null)
						break;
					// check if this is one of independents :)
					if(!Arrays.asList("Start","Node","ColorBook","Stop").contains(ie.getType())){
						//	ie.getType().equalsIgnoreCase("start") || ie.getAction().equalsIgnoreCase("ImageChanged") || ie.getType().equalsIgnoreCase("Node"))){
						ie.setClientEventId(ce.getMessageId());
						// do SQL update, exit out if problem is detected
						if(!doUpdateIE(ie))
							return false;
						
						// now check, if this IE was a SURE/UNSURE button press, then we can only have one
						// this takes care of some cases where timestamps for button IE don't match up w/ CE correctly
						if("Button".equals(ie.getType()) && ie.getLabel().contains("sure.of"))
							break;
					}
				}
			}
		}
		
		return true;
	}
	
	private boolean doUpdateIE(InterfaceEvent ie){
		if(protocol instanceof DatabaseProtocolModule){
			try {
				String SQL = "UPDATE interface_event SET client_event_id = ? WHERE interface_event_id = ?";
				Connection conn = ((DatabaseProtocolModule) protocol).getConnection();
				PreparedStatement st = conn.prepareStatement(SQL);
				st.setInt(1,ie.getClientEventId());
				st.setInt(2,ie.getMessageId());	
				// execute
				st.executeUpdate();
				st.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
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

}
