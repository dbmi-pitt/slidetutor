package edu.pitt.dbmi.tutor.model;
import java.util.Map;
/**
 * this class describes mult-tutor application that manages multiple tutors
 * @author tseytlin
 */
public interface TutorManager {
	/**
	 * add tutor to manager
	 * @param t
	 */
	public void addTutor(Tutor t);
	
	/**
	 * remove tutor from manager
	 * @param t
	 */
	public void removeTutor(Tutor t);
	
	/**
	 * get list of tutors 
	 * @param t
	 */
	public Tutor [] getTutors();
	
	
	/**
	 * notify all registered tutors
	 * @param message map contains content of the message
	 */
	public void notifyTutors(Map message);
	
	/**
	 * start problem for all registered tutors
	 * @param name
	 */
	public void startProblem(String name);
	
	/**
	 * finish problem for all registered tutors
	 */
	public void finishProblem();
}
