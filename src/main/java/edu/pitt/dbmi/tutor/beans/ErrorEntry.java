package edu.pitt.dbmi.tutor.beans;

import java.util.Collections;
import java.util.List;

/**
 * this bean is a container for error related things
 * @author tseytlin
 *
 */
public class ErrorEntry {
	private ConceptEntry entry;
	private String error;
	//private List<ConceptEntry> findings;
	private int priority;
	private boolean resolved;
	
	
	public ErrorEntry(String err, ConceptEntry obj){
		this.error = err;
		this.entry = obj;
		
		// add error
		entry.addError(err);
	}
	
	
	public ConceptEntry getConceptEntry() {
		return entry;
	}


	public String getError() {
		return error;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
		
		// if resolved reset status
		if(resolved){
			entry.removeError(error);
			entry.setConceptStatus(ConceptEntry.CORRECT);
		}
	}


	
	/**
	 * lower the better, 0 prioryt means high
	 * @return
	 */
	public int getPriority(){
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}


	public List<ConceptEntry> getFindings() {
		//return (findings == null)?Collections.EMPTY_LIST:findings;
		return entry.getPotentialFindings();
	}


	public void setFindings(List<ConceptEntry> findings) {
		//this.findings = findings;
		entry.setPotentialFindings(findings);
	}

	
	/**
	 * check if this error is resolved
	 * @return
	 */
	public boolean isResolved(){
		return resolved;
	}
	
	public String toString(){
		return error+" | "+entry;
	}
	
	public boolean equals(Object o){
		return hashCode() == o.hashCode();
	}
	
	public int hashCode(){
		return (error+"|"+entry.getId()).hashCode();
	}
}
