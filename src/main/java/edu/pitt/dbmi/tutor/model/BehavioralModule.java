package edu.pitt.dbmi.tutor.model;

/**
 * 
 * This module can modify tutor behavior during operation of a tutor.
 * Ex: switch feedback mode, replace module, turn on features
 * It is meant to constantly run in background listening to messages for conditions
 * @author tseytlin
 *
 */
public interface BehavioralModule extends TutorModule {

	
	/**
	 * start behavioural module
	 */
	public void start();
	
	
	/**
	 * stop behavioural module
	 */
	public void stop();
	
	/**
	 * get tutor
	 * @return
	 */
	public Tutor getTutor();
	
	
	/**
	 * set tutor
	 * @return
	 */
	public void setTutor(Tutor t);
	
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(BehavioralModule tm);
	
}
