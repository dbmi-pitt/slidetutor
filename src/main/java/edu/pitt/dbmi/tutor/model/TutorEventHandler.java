package edu.pitt.dbmi.tutor.model;

import java.util.Map;
/**
 * This interface is responsible for controlling a Tutor
 * @author tseytlin
 */
public interface TutorEventHandler {
	/**
	 * If there is anything that should be done
	 * before playback starts, do it here
	 */
	public void playbackStarted();
	
	/**
	 * If there is anything that should be done
	 * after playback is done, do it here
	 */
	public void playbackFinished();
	
	/**
	 * Handle ClientEvent
	 * Input is specially formated Map
	 * @param event
	 */
	public void handleClientEvent(Map event);
	
	/**
	 * Handle ClientEvent
	 * Input is specially formated Map
	 * @param event
	 */
	public void handleInterfaceEvent(Map event);
	
	/**
	 * this is used for message passing between tutors
	 * Input is specially formated Map
	 * @param event
	 */
	public void handleTutorEvent(Map event);
	
	/**
	 * Block event handling for X milliseconds
	 * This is useful, if Tutor know that it might recieve events
	 * from other tutors that are caused by Tutor itself
	 * @param millis
	 */
	public void block(int millis);
}
