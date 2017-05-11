package edu.pitt.dbmi.tutor.model;

/**
 * This module describes pedagogic strategies across curriculum.
 * It suggests next case as long as additional learning materials.
 * @author Eugene Tseytlin
 *
 */
public interface PedagogicModule extends TutorModule {
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setStudentModule(StudentModule module);
	
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setExpertModule(ExpertModule module);
	
	
	/**
	 * set protocol module. Allows pedagogic decisions
	 * to ba made based on past student progress 
	 */
	public void setProtocolModule(ProtocolModule module);
	
	/**
	 * get next case in the sequence
	 * @return
	 */
	public String getNextCase();
	
	
	/**
	 * get total number of cases in the sequence
	 * (if applicable)
	 * @return case count or 0 if not applicable
	 */
	public int getCaseCount();
	
	
	/**
	 * get the offset of the current case in the sequence
	 * (if applicable)
	 * @return case offset or -1 if not applicable
	 */
	public int getCaseOffset();
	
	/**
	 * load necessary resources
	 */
	public void load();
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(PedagogicModule tm);
}
