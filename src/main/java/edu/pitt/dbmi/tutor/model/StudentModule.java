package edu.pitt.dbmi.tutor.model;

/**
 * This module tracks progress of the student.
 * @author Eugene Tseytlin
 *
 */
public interface StudentModule extends TutorModule {
	public static final String NOVICE = "novice";
	public static final String INTERMEDIATE = "intermediate";
	public static final String EXPERT = "expert";
	
	
	/**
	 * set protocol module, to pull the data from
	 * @param module
	 */
	public void setProtocolModule(ProtocolModule module);
	
		
	/**
	 * get student skill level
	 * NOVICE, INTERMEDIATE, EXPERT
	 * @return
	 */
	public String getStudentLevel();
	
	
	/**
	 * get a mastery level of a student for
	 * a given skill, s.a. domain, finding, dx, case etc..
	 * @param skill
	 * @return mastery level from 0 to 1.0
	 */
	public double getMasteryLevel(String skill);
	
	
	/**
	 * return a list of observed cases that a student looked at in the past
	 * @return list of case URIs or names
	 */
	public String [] getObservedCases();
	
	/**
	 * return a list of observed domains that a student looked at in the past
	 * @return list of domain URIs 
	 */
	public String [] getObservedDomains();
	
	
	/**
	 * get tutor response for a given error/tutor state
	 * @param state
	 * @return
	 */
	public String getResponse(String state);
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(StudentModule tm);
}
