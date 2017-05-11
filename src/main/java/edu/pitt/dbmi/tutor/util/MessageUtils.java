package edu.pitt.dbmi.tutor.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.model.TutorModule;

/**
 * different helpful messages
 * @author tseytlin
 */
public class MessageUtils {
	private static Map<TutorModule,MessageUtils> utilMap;
	private List<InterfaceEvent> pendingInterfaceEvents;
	
	/**
	 * get instance of message utils for 
	 * @param tm
	 * @return
	 */
	public static MessageUtils getInstance(TutorModule tm){
		if(utilMap == null)
			utilMap = new HashMap<TutorModule, MessageUtils>();
		if(utilMap.containsKey(tm))
			return utilMap.get(tm);
		
		// create new one
		MessageUtils mu = new MessageUtils();
		utilMap.put(tm,mu);
		return mu;
	}
	
	
	/**
	 * add interface events to a queue
	 * @param ie
	 */
	public void addInterfaceEvent(InterfaceEvent ie){
		if(pendingInterfaceEvents == null)
			pendingInterfaceEvents = new ArrayList<InterfaceEvent>();
		pendingInterfaceEvents.add(ie);
	}
	
	/**
	 * flush all pending interface events
	 * @param id
	 */
	public void flushInterfaceEvents(ClientEvent ce){
		if(pendingInterfaceEvents == null)
			pendingInterfaceEvents = new ArrayList<InterfaceEvent>();
		for(InterfaceEvent ie: pendingInterfaceEvents){
			ie.setClientEvent(ce);
			Communicator.getInstance().sendMessage(ie);
		}
		pendingInterfaceEvents.clear();
	}
	
	/**
	 * clear all events
	 */
	public void clear(){
		pendingInterfaceEvents.clear();
	}
}
