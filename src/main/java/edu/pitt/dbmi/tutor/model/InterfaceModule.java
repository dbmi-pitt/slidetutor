package edu.pitt.dbmi.tutor.model;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;


/**
 * This module is responsible for student-tutor interaction.
 * @author Eugene Tseytlin
 */
public interface InterfaceModule extends InteractiveTutorModule {
	/**
	 * add concept entry to interface
	 * @param name
	 */
	public void addConceptEntry(ConceptEntry e);
	
	
	/**
	 * refine concept entry 
	 * @param parent
	 * @param child
	 */
	public void refineConceptEntry(ConceptEntry p,ConceptEntry e);
	
	/**
	 * remove concept from interface
	 * @param name
	 */
	public void removeConceptEntry(ConceptEntry e);
	
	
	/**
	 * get a list of concept entries loaded in this
	 * module
	 * @return
	 */
	public List<ConceptEntry> getConceptEntries();
	
	/**
	 * get concept entry by id or name
	 * @param name
	 * @return
	 */
	public ConceptEntry getConceptEntry(String id);
	
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
	 * get a sub-menu for this component
	 * @return
	 */
	public JMenu getMenu();
	
	/**
	 * get popup menu associated w/ this interface
	 * @return
	 */
	public JPopupMenu getPopupMenu();
	
	
	/**
	 * get a tool bar for this component
	 * @return
	 */
	public JToolBar getToolBar();
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(InterfaceModule tm);
}
