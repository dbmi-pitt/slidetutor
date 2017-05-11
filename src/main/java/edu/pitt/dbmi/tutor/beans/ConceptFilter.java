package edu.pitt.dbmi.tutor.beans;

/**
 * concept filter, used to filter concepts in tree
 * @author tseytlin
 */
public interface ConceptFilter {
	/**
	 * is concept acceptable to this filter
	 * @param name
	 * @return
	 */
	public boolean accept(String parent,String name);
}
