package edu.pitt.dbmi.tutor.beans;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.model.TutorModule;

/**
 * create a link concept entry between two concepts
 * @author Eugene Tseytlin
 */
public class LinkConceptEntry extends ConceptEntry {
	private ConceptEntry source, destination;
	
	/**
	 * create a new link concept
	 * @param a
	 * @param b
	 * @param im
	 */
	public LinkConceptEntry(ConceptEntry src, ConceptEntry dst, String type){
		super(createName(src,dst,type),type);
		//super(src.getFeature().getName()+" - "+dst.getName(),type);
		source = src;
		destination = dst;
	}

	public LinkConceptEntry clone(){
		LinkConceptEntry c = new LinkConceptEntry(source,destination,getType());
		return c;
	}
	
	public void setType(String type){
		super.setType(type);
		// rename concept
		//setName(createName(source,destination,type));
	}
	
	private String createName(){
		return createName(source,destination,getType());
	}
	
	
	private static String createName(ConceptEntry src, ConceptEntry dst, String type){
		//return src.getFeature().getName()+" - "+dst.getName();
		return src.getName()+" - "+dst.getName();
		/*
		return (src.isAbsent()?"NO_":"")+src.getName()+
				((TYPE_REFUTE_LINK.equals(type))?LINK_REFUTE:LINK_SUPPORT)+
				dst.getName();
			*/
	}
	
	
	/**
	 * get source concept
	 * @return
	 */
	public ConceptEntry getSourceConcept() {
		return source;
	}

	/**
	 * get destination concept
	 * @return
	 */
	public ConceptEntry getDestinationConcept() {
		return destination;
	}
		
	
	/**
	 * get source concept
	 * @return
	 */
	public void setSourceConcept(ConceptEntry e) {
		source = e;
		setName(createName());
	}

	/**
	 * get destination concept
	 * @return
	 */
	public void setDestinationConcept(ConceptEntry e) {
		destination = e;
		setName(createName());
	}
	
	/**
	 * get parent entry (feature). This is a "feature" of the old
	 * that is the most general for this concept
	 * @param feature
	 */
	public ConceptEntry getFeature() {
		return this;
	}

	
	/**
	 * get a client that describes this object for a given
	 * action
	 * @param action
	 * @return
	 */
	public ClientEvent getClientEvent(TutorModule sender,String action){
		ClientEvent ce = super.getClientEvent(sender, action);
		// reset parent
		ce.setParent(getParent());
		ce.setEntireConcept(source.getObjectDescription()+":"+destination.getObjectDescription()+":"+super.getObjectDescription());
		return ce;
	}
	
	/**
	 * get parent name
	 * @return
	 */
	public String getParent(){
		//return source.getFeature().getObjectDescription()+":"+destination.getObjectDescription();
		return source.getObjectDescription()+":"+destination.getObjectDescription();
	}
	
	/**
	 * get object description for this concept
	 * @return
	 */
	public String getObjectDescription(){
		return getParent()+":"+super.getObjectDescription();
	}
	
	/**
	 * create new link concept entry
	 * @return newly created concept entry, null if two concepts
	 * cannot be linked
	 */
	public static LinkConceptEntry createSupportLinkEntry(ConceptEntry a, ConceptEntry b){
		if(a == null || b == null)
			return null;
		
		if(TYPE_ATTRIBUTE.equals(a.getType()))
			a = a.getParentEntry();
		
		if(TYPE_ATTRIBUTE.equals(b.getType()))
			b = b.getParentEntry();
		
		
		// do some sanity checking
		if(a.isFinding() && TYPE_HYPOTHESIS.equals(b.getType())){
			return new LinkConceptEntry(a,b,TYPE_SUPPORT_LINK);
		}else if(b.isFinding() && TYPE_HYPOTHESIS.equals(a.getType())){
			return new LinkConceptEntry(b,a,TYPE_SUPPORT_LINK);
		}
		return null;
	}
	
	/**
	 * create new link concept entry
	 * @return newly created concept entry, null if two concepts
	 * cannot be linked
	 */
	public static LinkConceptEntry createRefuteLinkEntry(ConceptEntry a, ConceptEntry b){
		if(a == null || b == null)
			return null;
		
		if(TYPE_ATTRIBUTE.equals(a.getType()))
			a = a.getParentEntry();
		
		if(TYPE_ATTRIBUTE.equals(b.getType()))
			b = b.getParentEntry();
		
		// do some sanity checking
		if(a.isFinding() && TYPE_HYPOTHESIS.equals(b.getType())){
			return new LinkConceptEntry(a,b,TYPE_REFUTE_LINK);
		}else if(b.isFinding() && TYPE_HYPOTHESIS.equals( a.getType())){
			return new LinkConceptEntry(b,a,TYPE_REFUTE_LINK);
		}
		return null;
	}
	
	/**
	 * create new link concept entry
	 * @return newly created concept entry, null if two concepts
	 * cannot be linked
	 *
	public static LinkConceptEntry createSupportLinkEntry(String description){
		String [] link = description.split(LINK_SUPPORT);
		// if can't parse link
		if(link.length != 2)
			return null;
			
		// fetch classes for source and destination
		// parse absent findings as well
		boolean absent = false;
		if(link[0].startsWith("NO_")){
			link[0] = link[0].substring(3);
			absent = true;
		}
	
		// lookup classes
		ConceptEntry a = new ConceptEntry(link[0],
					(absent)?TYPE_ABSENT_FINDING:TYPE_FINDING);
		ConceptEntry b = new ConceptEntry(link[1],TYPE_HYPOTHESIS);
		
		// do some sanity checking
		if(a.isFinding() && TYPE_HYPOTHESIS.equals(b.getType())){
			return new LinkConceptEntry(a,b,TYPE_SUPPORT_LINK);
		}else if(b.isFinding() && TYPE_HYPOTHESIS.equals(a.getType())){
			return new LinkConceptEntry(b,a,TYPE_SUPPORT_LINK);
		}
		return null;
	}
	*/
	/**
	 * create new link concept entry
	 * @return newly created concept entry, null if two concepts
	 * cannot be linked
	 *
	public static LinkConceptEntry createRefuteLinkEntry(String description){
		String [] link = description.split(LINK_REFUTE);
		// if can't parse link
		if(link.length != 2)
			return null;
			
		// fetch classes for source and destination
		// parse absent findings as well
		boolean absent = false;
		if(link[0].startsWith("NO_")){
			link[0] = link[0].substring(3);
			absent = true;
		}
	
		// lookup classes
		ConceptEntry a = new ConceptEntry(link[0],
					(absent)?TYPE_ABSENT_FINDING:TYPE_FINDING);
		ConceptEntry b = new ConceptEntry(link[1],TYPE_HYPOTHESIS);
		
		// do some sanity checking
		if(a.isFinding() && TYPE_HYPOTHESIS.equals(b.getType())){
			return new LinkConceptEntry(a,b,TYPE_REFUTE_LINK);
		}else if(b.isFinding() && TYPE_HYPOTHESIS.equals(a.getType())){
			return new LinkConceptEntry(b,a,TYPE_REFUTE_LINK);
		}
		return null;
	}
	*/
	/**
	 * resolve tag
	 * @param tag
	 * @return
	 */
	public String resolveTag(String tag){
		if(TAG_FINDING.equals(tag))
			return source.getText(); //return source.resolveTag(tag);
		else if(TAG_HYPOTHESIS.equals(tag))
			return destination.getText(); //return destination.resolveTag(tag);
		return super.resolveTag(tag);
	}
}
