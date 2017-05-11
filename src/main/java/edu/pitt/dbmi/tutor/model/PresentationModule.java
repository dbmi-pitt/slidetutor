package edu.pitt.dbmi.tutor.model;

import java.awt.Shape;

import edu.pitt.dbmi.tutor.beans.CaseEntry;

/**
 * This module is responsible for presenting a case
 * to a student. For example virtual microscope.
 * @author Eugene Tseytlin
 *
 */
public interface PresentationModule extends InteractiveTutorModule {
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setExpertModule(ExpertModule module);
	
	
	/**
	 * set current case entry
	 * @param problem
	 */
	public void setCaseEntry(CaseEntry problem);
	
	
	/**
	 * start feature identification process.
	 * Identify "something" of value inside
	 * presentation module
	 */
	public void startFeatureIdentification();
	
	
	/**
	 * set feature identification marker defined by type and shape.
	 * presentation module
	 */
	public void setIdentifiedFeature(Shape shape);
	
	/**
	 * stop feature identification process.
	 * Identify "something" of value inside
	 * presentation module
	 */
	public void stopFeatureIdentification();
	
	/**
	 * get identified feature as some object.
	 * It is up to calling module to know what this
	 * object will be.
	 * @return
	 */
	public Object getIdentifiedFeature();
	
	
	/**
	 * remove identified feature from the presentation module
	 * @param obj
	 */
	public void removeIdentifiedFeature(Object obj);
	
	
	/**
	 * has the feature been identified?
	 * @return
	 */
	public boolean isFeatureIdentified();
	
	
	/**
	 * set feature marker type for identifying
	 * a feature
	 * @param type
	 */
	public void setFeatureMarker(String type);
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(PresentationModule tm);
	
}
