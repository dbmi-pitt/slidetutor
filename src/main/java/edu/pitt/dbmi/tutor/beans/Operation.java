package edu.pitt.dbmi.tutor.beans;
/**
 * undoable ontology action
 * @author tseytlin
 */
public interface Operation extends  Runnable {
	/**
	 * undo action
	 */
	public void undo();
}