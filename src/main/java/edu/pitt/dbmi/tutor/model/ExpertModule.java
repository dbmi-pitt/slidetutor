package edu.pitt.dbmi.tutor.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ConceptFilter;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.ontology.IOntology;
import edu.pitt.terminology.Terminology;


/**
 * This module represents expert knowledge. It is a gateway to domain knowledge.
 * From here one can request case and knowledge information.
 * @author Eugene Tseytlin
 */
public interface ExpertModule extends TutorModule {
	
	/**
	 * get a list of available domains
	 * @return
	 */
	public String [] getAvailableDomains();
	
	/**
	 * get a list of available cases for a given domain
	 * @return
	 */
	public String [] getAvailableCases(String domain);
	
	/**
	 * get a list of ALL available cases across all domains
	 * @return
	 */
	public String [] getAvailableCases();
		
	
	/**
	 * open given domain ontology from some resource
	 * @param name/uri of domain to open
	 */
	public void openDomain(String name);
	
	/**
	 * close current domain ontology 
	 */
	public void closeDomain();
	
	/**
	 * get domain ontology name/uri that is currently loaded
	 * @return
	 */
	public String getDomain();
	
	
	/**
	 * get subtree from the domain ontology.
	 * @param root - name of the root class
	 * @return
	 */
	public TreeNode getTreeRoot(String root);
	
	/**
	 * get subtree from the domain ontology.
	 * @param root - name of the root class
	 * @param filter - prune concepts that don't match filter
	 * @return
	 */
	public TreeNode getTreeRoot(String root, ConceptFilter filter);
	
	/**
	 * get a paths to root for a given concept
	 * @param name  - name of the class in question
	 * @return
	 */
	public List<TreePath> getTreePaths(String name);
	
	
	/**
	 * there might be a case when expert module can offer it's own
	 * concept filter to accept/reject concepts from domain based
	 * on its own parameter set. 
	 * @return concept filter associated with this module 
	 * (if nothing special is setup, concept filter returned should be the
	 * one that accepts all concepts)
	 */
	public ConceptFilter getConceptFilter();
	
	
	/**
	 * get entire case information s.a. 
	 * findings, diagnosis, metadata, slides etc ...
	 * @param name
	 * @return
	 */
	public CaseEntry getCaseEntry(String name);


	/**
	 * resolve meta-info for a given concept entry
	 * setup Concept object, examples etc ...
	 * @param entry
	 */
	public void resolveConceptEntry(ConceptEntry entry);
	
	/**
	 * get current domain ontology
	 * @return
	 */
	public IOntology getDomainOntology();
	
	/**
	 * get a terminology for current domain
	 * @return
	 */
	public Terminology getDomainTerminology();
	
	
	/**
	 * get a set of reportable items for a given domain
	 * @param a set of concepts that help to determine which template applys
	 * @return a set of reportable items
	 */
	public Set<ConceptEntry> getReportTemplate(List<ConceptEntry> concepts);
	
	/**
	 * sync content, copy state from parameter
	 * to this module
	 * @param tm
	 */
	public void sync(ExpertModule tm);
}
