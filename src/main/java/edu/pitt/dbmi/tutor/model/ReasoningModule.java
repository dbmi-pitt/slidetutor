package edu.pitt.dbmi.tutor.model;

import java.util.List;
import java.util.Map;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;

/**
 * This module is responsible for reasoning within the tutor
 * on case level.
 * @author Eugene Tseytlin
 *
 */
public interface ReasoningModule extends TutorModule {
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setExpertModule(ExpertModule module);
	
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setStudentModule(StudentModule module);
	
	
	/**
	 * load case information (open image)
	 */
	public void setCaseEntry(CaseEntry problem);
	
	
	/**
	 * return a concept that the reasoner thinks
	 * should be added next by a student
	 * @return next level concept, or null if problem is done
	 */
	public ConceptEntry getNextConcept();
	
	
	/**
	 * return a problem concept that the reasoner thinks
	 * should be handled next
	 * @return next problem concept, or null if there are no problems
	 */
	public ConceptEntry getProblemConcept();
	
	/**
	 * has the current case been solved
	 * @return
	 */
	public boolean isSolved();
	
	/**
	 * get a list of implied hypotheses based on a given
	 * evidence
	 * @return
	 */
	public List<ConceptEntry> getImpliedHypotheses();
	
	/**
	 * get a list of implied hypotheses based on a given
	 * evidence
	 * @return
	 */
	public List<ConceptEntry> getImpliedDiagnoses();
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(ReasoningModule tm);
	
	
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
	 * get currently loaded scenario set of error codes
	 * @return
	 */
	public ScenarioSet getSupportedScenarioSet();
}
