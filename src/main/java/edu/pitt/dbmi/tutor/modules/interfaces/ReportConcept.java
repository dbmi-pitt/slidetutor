package edu.pitt.dbmi.tutor.modules.interfaces;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;


import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.modules.interfaces.NodeConcept.AttributeConcept;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ConceptLabel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.ontology.*;
import static edu.pitt.dbmi.tutor.messages.Constants.*;


/**
 * this class represents a report concept
 * @author tseytlin
 *
 */
public class ReportConcept {
	private ReportInterface reportInterface;
	private ConceptEntry conceptEntry;
	


	private IClass cls;
	//private String resourceLink;
	private SortedSet<ConceptLabel> labels;
	private String text;
	protected Color errorColor,unknownColor,irrelevantColor,correctColor;
	protected List<ReportConcept> attributes;
	protected ReportConcept parentConcept;
	protected ReportConcept negation;
	

	public ReportConcept getParentConcept() {
		return (parentConcept == null)?this:parentConcept;
	}


	public ReportConcept getNegation() {
		return negation;
	}

	public String getName(){
		return conceptEntry.getName();
	}
	
	public boolean isNegated(){
		return negation != null;
	}

	public void setNegation(ReportConcept negation) {
		this.negation = negation;
		conceptEntry.setAbsent(negation != null);
		if(negation != null){
			String id = conceptEntry.getFeature().getId();
			negation.getConceptEntry().setFeature(conceptEntry.getFeature());
			negation.getConceptEntry().setParentEntry(conceptEntry.getParentEntry());
			negation.setParentConcept(this);
			conceptEntry.getFeature().setId(id);
		}
		text = null;
	}


	public void setParentConcept(ReportConcept parentConcept) {
		this.parentConcept = parentConcept;
	}
	
	public void setConceptEntry(ConceptEntry conceptEntry) {
		this.conceptEntry = conceptEntry;
		this.conceptEntry.getRepresentations().remove(this);
		this.conceptEntry.addRepresentation(this);
	}

	public static ReportConcept getReportConcept(ConceptEntry e, InterfaceModule i){
		for(Object o: e.getRepresentations()){
			if(o instanceof ReportConcept && ((ReportConcept)o).getReportInterface().equals(i)){
				return (ReportConcept) o;
			}
		}
		return new ReportConcept(e,(ReportInterface)i);
	}

	
	public ReportConcept(ConceptEntry e,ReportInterface im){
		conceptEntry = e;
		reportInterface = im;
		e.addRepresentation(this);
		
		
		// some init settings
		errorColor = Config.getColorProperty(im,"node.error.color");
		correctColor = Config.getColorProperty(im,"node.correct.color");
		irrelevantColor = Config.getColorProperty(im,"node.irrelevant.color");
		unknownColor = Config.getColorProperty(im,"node.unknown.color");
		
	}
	
	public ReportConcept clone(){
		ReportConcept c = new ReportConcept(conceptEntry.clone(),reportInterface);
		if(parentConcept != null)
			c.parentConcept = parentConcept;
		if(negation != null)
			c.negation = negation;
		for(ConceptLabel lbl: getLabels()){
			c.addLabel(lbl);
		}
		return c;
	}
	
	public ReportInterface getReportInterface(){
		return reportInterface;
	}
	
	
	public ConceptEntry getConceptEntry(){
		return conceptEntry;
	}
	
	public IClass getConceptClass(){
		if(cls == null){
			IOntology ont = reportInterface.getDomainOntology();
			if(ont != null)
				cls = ont.getClass(conceptEntry.getName());
		}
		return cls;
	}
	
	public String getType(){
		return conceptEntry.getType();
	}
	
	
	public boolean isAttribute(){
		return TYPE_ATTRIBUTE.equals(conceptEntry.getType());
	}
	
	/**
	 * is this attribute a negation
	 * @return
	 */
	public boolean isNegation(){
		return isAttribute() && conceptEntry.getName().equals(LABEL_NO);
	}
	
	/**
	 * if concept in text get its offset
	 * @return
	 */
	public int getOffset(){
		if(labels != null && !labels.isEmpty()){
			return labels.first().getOffset();
		}
		return 0;
	}
	
	/**
	 * if concept in text get its offset
	 * @return
	 */
	public int getLength(){
		if(labels != null && !labels.isEmpty()){
			return labels.last().getOffset()-getOffset()+labels.last().getLength();
		}
		return 0;
	}
	
	/**
	 * get textrual representation of concept
	 * @return
	 */
	public String getText(){
		if(text == null ){
			List<ConceptLabel> lbls = getLabels();
			if(lbls.size() > 0){
				StringBuffer b = new StringBuffer();
				for(ConceptLabel l: lbls)
					b.append(l.getText()+" ");
				text = b.toString().trim();
			}else{
				text = conceptEntry.getText();
			}
		}
		return text;
	}
	
	/**
	 * get end position of a concept
	 * @return
	 */
	public int getEndOffset(){
		if(labels != null && !labels.isEmpty()){
			return labels.last().getOffset()+labels.last().getLength();
		}
		return 0;
	}
	
	/**
	 * is offset within concept text
	 * @param offset
	 * @return
	 */
	public boolean isInConceptText(int offset){
		if(labels != null && !labels.isEmpty()){
			return getOffset() <= offset && offset <= getEndOffset();
		}
		return false;
	}
	
	
	public void delete(){
		for(ConceptLabel l: getLabels()){
			l.setDeleted(true);
			l.update(reportInterface.getReportDocument());
		}
		labels.clear();
		text = null;
	}
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void addLabel(ConceptLabel lbl){
		if(labels == null){
			labels = new TreeSet<ConceptLabel>();
		}
		labels.add(lbl);
		text = null;
	}
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void addLabels(ConceptLabel [] lbl){
		if(labels == null){
			labels = new TreeSet<ConceptLabel>();
		}
		for(int i=0;i<lbl.length;i++)
			labels.add(lbl[i]);
		text = null;
	}
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void addLabels(Collection<ConceptLabel> lbl){
		if(labels == null){
			labels = new TreeSet<ConceptLabel>();
		}
		labels.addAll(lbl);
		text = null;
	}
	
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void removeLabel(ConceptLabel lbl){
		if(labels != null){
			labels.remove(lbl);
			lbl.setDeleted(true);
			lbl.update(reportInterface.getReportDocument());
		}
		text = null;
	}
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void removeLabels(){
		if(labels != null){
			for(ConceptLabel lbl: labels){
				lbl.setDeleted(true);
				lbl.update(reportInterface.getReportDocument());
			}
			labels.clear();
		}
		text = null;
	}
	
	public void setLabels(ConceptLabel [] lbl){
		labels = null;
		addLabels(Arrays.asList(lbl));
	}
	
	/**
	 * get all labels
	 * @return
	 */
	public List<ConceptLabel> getLabels(){
		// filter deleted labels
		List<ConceptLabel> list = new ArrayList<ConceptLabel>();
		if(labels != null){
			for(ConceptLabel lbl:labels){
				if(!lbl.isDeleted())
					list.add(lbl);
			}
		}else if(parentConcept != null && !parentConcept.getLabels().isEmpty()){
			// if we have no labels, but have a parent w/ labels, meybe
			// the parent can give us some :)
			labels = new TreeSet<ConceptLabel>();
			for(ConceptLabel lbl : parentConcept.getLabels()){
				if(OntologyHelper.isLabelCoversConcept(lbl,conceptEntry)){
					if(!lbl.isDeleted())
						labels.add(lbl);
				}
			}
			list = new ArrayList<ConceptLabel>(labels);
		}
		return list;		
	}
	
	/*
	public void setAbsent(boolean b){
		if(conceptEntry.isFinding()){
			conceptEntry.setType((b)?TYPE_ABSENT_FINDING:TYPE_FINDING);
		}
	}
	*/
	public double getNumericValue(){
		return conceptEntry.getNumericValue();
	}
	
	public void setNumericValue(double d){
		conceptEntry.setNumericValue(d);
	}
	
	public void setResourceLink(String l){
		//resourceLink = l;
		conceptEntry.setResourceLink(l);
	}
	
	public String getResourceLink(){
		//return resourceLink;
		return conceptEntry.getResourceLink();
	}
	
	public boolean  hasResourceLink(){
		//return resourceLink != null;
		return conceptEntry.getResourceLink() != null;
	}
	
	public String toString(){
		return conceptEntry.getText();
	}
	
	public boolean equals(Object obj){
		if(obj instanceof ReportConcept){
			ReportConcept r = (ReportConcept) obj;
			return conceptEntry.equals(r.getConceptEntry()) ;
		}
	
		return false;
	}
	
	public int hashCode(){
		return conceptEntry.hashCode();
	}
	
	
	
	/**
	 * get background color
	 */
	public Color getBackgroundColor(){
		Color c = unknownColor;
		ConceptEntry entry = conceptEntry;
		
		// if parent is present, then the entire concept color
		// should be from parent
		/*
		if(entry.getParentEntry() != null && 
		   (TYPE_FINDING.equals(entry.getType()) || 
			TYPE_ABSENT_FINDING.equals(entry.getType()))){
			entry = entry.getParentEntry();
		}
		*/
		//System.out.println(entry+" "+entry.getConceptStatus());
		
		if(entry.getConceptStatus() == ConceptEntry.CORRECT)
			c  = correctColor;
		else if(entry.getConceptStatus() == ConceptEntry.INCORRECT)
			c  = errorColor;
		else if(entry.getConceptStatus() == ConceptEntry.IRRELEVANT)
			c  = irrelevantColor;
		return c;
	}
	
	/**
	 * get attributes associated with this node
	 * @return
	 */
	public List<ReportConcept> getAttributes(){
		if(attributes == null){
			attributes = new Vector<ReportConcept>();
			for(ConceptEntry a : conceptEntry.getAttributes()){
				// make sure those attributes are part of a string
				if(conceptEntry.getText().contains(a.getText())){
					ReportConcept r = new ReportConcept(a,reportInterface);
					r.setParentConcept(this);
					attributes.add(r);
				}
			}
		}
		return attributes;
	}
	

	
	/**
	 * update colors
	 */
	public void repaint(){
		Color c = getBackgroundColor();
		for(ConceptLabel l: getLabels()){
			l.setColor(c);
			l.update(reportInterface.getReportDocument());
		}
		for(ReportConcept a: getAttributes())
			a.repaint();
		
		//reportInterface.getComponent().repaint();
	}
}
