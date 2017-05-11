package edu.pitt.dbmi.tutor.model;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;

/**
 * This module is responsible for all types of feedback.
 * Hint messages, Mistakes, Interventions, etc...
 * @author Eugene Tseytlin
 */
public interface FeedbackModule extends InteractiveTutorModule {
	public static int HINT_FEEDBACK  = 1;
	public static int ERROR_FEEDBACK = 2;
	public static int COLOR_FEEDBACK = 4;
	public static int SUMMARY_FEEDBACK = 8;

	
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
	 * set current case entry
	 * @param problem
	 */
	public void setCaseEntry(CaseEntry problem);
	
	
	
	/**
	 * set feedback mode. The mode is a bitmap of several flags:
	 * HINT_FEEDBACK,ERROR_FEEDBACK,COLOR_FEEDBACK,
	 * SUMMARY_FEEDBACK, GLOSSARY_FEEDBACK
	 * @param mode
	 */
	public void setFeedackMode(int mode);
	
	/**
	 * get feedback mode. The mode is a bitmap of flags:
	 * HINT_FEEDBACK,ERROR_FEEDBACK,COLOR_FEEDBACK,
	 * SUMMARY_FEEDBACK, GLOSSARY_FEEDBACK
	 * @return
	 */
	public int getFeedbackMode();
	
	/**
	 * request new hint from the reasoning module.
	 * A hint is normally composed as a set of messages
	 */
	public void requestHint();
	
	/**
	 * request/display a level hint or bug with a given offset
	 * Use getHintLevelCount() to determine the number of levels
	 * @param i
	 */
	public void requestLevel(int offset);
	
	
	/**
	 * get number of hint levels in the current
	 * @return
	 */
	public int getLevelCount();
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(FeedbackModule tm);

	
	/**
	 * get currently loaded scenario set of error codes
	 * @return
	 */
	public ScenarioSet getScenarioSet();
}
