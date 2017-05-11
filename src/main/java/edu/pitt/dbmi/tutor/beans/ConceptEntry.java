package edu.pitt.dbmi.tutor.beans;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.swing.Icon;
import javax.swing.tree.TreePath;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.model.TutorModule;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.markers.Annotation;
import edu.pitt.terminology.lexicon.Concept;




/**
 * this class represents a generic tutor concept
 * such as finding, diagnosis, link etc...
 * @author Eugene Tseytlin
 */
public class ConceptEntry implements PropertyChangeListener {
	public static final int UNKNOWN = 0;
	
	//concept status (cognitive)
	public static final int CORRECT = 1;
	public static final int INCORRECT = 2;
	public static final int IRRELEVANT = 3;
	
	// concept status (meta-congnitive)
	public static final int SURE = 11;
	public static final int ERROR = 12;
	public static final int UNSURE = 13;
	
	public static final double NO_VALUE = Double.MIN_VALUE;
	
	// counter
	private static int count = 1;
	
	// fields
	private String name,text,id,category,definition;
	private String type,power,resourceLink;
	private double numericValue = NO_VALUE ;
	private int conceptStatus,conceptFOK;
	private boolean important,incase,resolved,worksheetFinding,headerFinding,absent,found;
	private Object input;
	private List representations;
	private List<String> locations;
	private List<String> parts;
	private Map<String,String> examples,tags;
	private Stack<String> errors;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private Map<String,String> properties;
	private transient Concept concept;
	private transient List<URL> exampleImages;
	private transient String errorText;
	private transient Icon icon;
	private Annotation annotation;
	
	// related concepts
	private transient ConceptEntry feature, parent,template,definingFeature;
	private transient List<ActionEntry> actions;
	private transient List<ConceptEntry> attributes,potentialAttributes;
	private transient List<ConceptEntry> potentialFindings,inferredFindings;
	

	/**
	 * create new concept
	 * @param name
	 */
	public ConceptEntry(String name, String type){
		this.name = name;
		this.type = type;
		//setId(type+":"+name);
		
		// check if name contains a value, too
		int x = name.indexOf("=");
		if(x > -1){
			String k = name.substring(0,x).trim();
			String v = name.substring(x+1).trim();
			// now figure out the other part
			this.name = k;
			try{
				numericValue = Double.parseDouble(v);
			}catch(Exception ex){
				resourceLink = v;
			}
		}
	}
	
	/**
	 * create new concept
	 * @param name
	 */
	public ConceptEntry(TreePath path, String type){
		this(""+path.getLastPathComponent(),type);
	}

	
	/**
	 * clone concept entry
	 */
	public ConceptEntry clone(){
		ConceptEntry e = new ConceptEntry(name,type);
		copyTo(e);
		return e;
	}
	
	
	/**
	 * misc properties that can be attached to concepts client event
	 * @return
	 */
	public Map<String,String> getProperties(){
		if(properties == null)
			properties = new LinkedHashMap<String, String>();
		return properties;
	}
	
	public String getResourceLink() {
		return resourceLink;
	}

	public void setResourceLink(String resourceLink) {
		this.resourceLink = resourceLink;
	}

	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

	
	/**
	 * copy IDs for equivalent findings
	 * @param e
	 */
	public void copyIDsTo(ConceptEntry e){
		if(getFeature().equals(e.getFeature())){
			e.getFeature().setId(getFeature().getId());
			for(ConceptEntry a: e.getAttributes()){
				for(ConceptEntry oa: getAttributes()){
					if(oa.equals(a))
						a.setId(oa.getId());
				}
			}
		}
	}
	
	/**
	 * copy concept's content to concept entry
	 * @param e
	 */
	public void copyTo(ConceptEntry e){
		e.setConceptStatus(getConceptStatus());
		e.setConceptFOK(getConceptFOK());
		e.setLocations(new ArrayList<String>(getLocations()));
		e.setExamples(new HashMap<String,String>(getExamples()));
		e.setNumericValue(getNumericValue());
		e.clearErrors();
		e.getErrors().addAll(getErrors());
		e.setInput(getInput());
		e.setTagMap(new HashMap<String,String>(getTagMap()));
		e.setPower(getPower());
		e.setResourceLink(getResourceLink());
		e.setId(getId());
		//e.setConcept(getConcept());
		//e.setPower(getPower());
		//e.setExampleImages(getExampleImages());
		//e.setPotentialAttributes(getPotentialAttributes());
		//add attributes
		//for(ConceptEntry c: getAttributes())
		//	e.addAttribute(c);
	}	
	
	/**
	 * copy concept's content to concept entry
	 * @param e
	 */
	public void copyFrom(ConceptEntry e){
		e.copyTo(this);
	}
	
	public String getDefinition() {
		if(!TextHelper.isEmpty(definition))
			return definition;
		if(concept != null && !TextHelper.isEmpty(concept.getDefinition()))
			return concept.getDefinition();
		if(getFeature().getConcept() != null && !TextHelper.isEmpty(getFeature().getConcept().getDefinition()))
			return getFeature().getConcept().getDefinition();
		return "";
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}
	
	
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	public void setAuto(boolean b){
		if(b)
			getProperties().put("auto", "true");
		else
			getProperties().remove("auto");
	}
	
	public boolean isAuto(){
		return Boolean.parseBoolean(getProperties().get("auto"));
	}
	
	/**
	 * get list of errors associated with a concept
	 * @return
	 */
	public Stack<String> getErrors(){
		if(errors == null)
			errors = new Stack<String>();
		return errors;
	}
	
	/**
	 * get last error
	 * @return
	 */
	public String getError(){
		return (!getErrors().isEmpty())?getErrors().peek():null;
	}
	
	/**
	 * set errors
	 * @param e
	 */
	public void setErrors(Stack<String> e){
		errors =e;
	}
	
	public List<String> getParts() {
		if(parts == null)
			parts = new ArrayList<String>();
		return parts;
	}


	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	
	
	/**
	 * get error text
	 * @return
	 */
	public String getErrorText(){
		if(hasErrors()){
			if(errorText == null){
				StringBuffer b = new StringBuffer("<html><table width=400><tr><td><b>MISTAKES</b>");
				for(String s: getErrors()){
					b.append("<hr>"+s);
				}
				b.append("</td></tr></table>");
				errorText = b.toString();
			}
			return errorText;
		}
		return null;
	}
	
	
	/**
	 * add error
	 * @param str
	 */
	public void addError(String str){
		//if(!getErrors().contains(str))
		clearErrors();
		getErrors().push(str);
		errorText = null;
	}
	
	/**
	 * add insignificant error that should have the lowest priority
	 * @param str
	 */
	public void addHint(String str){
		addError(str);
		//clearErrors();
		//if(!getErrors().contains(str))
		//getErrors().add(0,str);
		//errorText = null;
	}
	
	/**
	 * add error
	 * @param str
	 */
	public void removeError(String str){
		getErrors().remove(str);
		errorText = null;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ConceptEntry> getPotentialFindings(){
		if(potentialFindings == null)
			potentialFindings = new ArrayList<ConceptEntry>();
		return potentialFindings;
	}
	
	public void setPotentialFindings(List<ConceptEntry> list){
		potentialFindings = list;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ConceptEntry> getInferredFindings(){
		if(inferredFindings == null)
			inferredFindings = new ArrayList<ConceptEntry>();
		return inferredFindings;
	}
	
	public void setInferredFindings(List<ConceptEntry> list){
		inferredFindings = list;
	}
	
	
	/**
	 * clear errors
	 * @param str
	 */
	public void clearErrors(){
		getErrors().clear();
		errorText = null;
	}
	
	/**
	 * does concept have arrors
	 * @return
	 */
	public boolean hasErrors(){
		return !getErrors().isEmpty();
	}
	

	public List<URL> getExampleImages() {
		if(exampleImages == null)
			exampleImages = new ArrayList<URL>();
		return exampleImages;
	}

	public void setExampleImages(List<URL> exampleImages) {
		this.exampleImages = exampleImages;
	}
	
	
	/**
	 * get name of the concept
	 * @return
	 */
	public String getName(){
		return name;
	}

	/**
	 * is concept absentConcept
	 * @return
	 */
	public boolean isAbsent() {
		return absent || TYPE_ABSENT_FINDING.equals(type) || LABEL_NO.equals(name);
	}
	
	public void setAbsent(boolean b){
		absent = b;
		text = null;
		//setType((b)?TYPE_ABSENT_FINDING:TYPE_FINDING);
	}
	
	

	public boolean isFinding(){
		return TYPE_FINDING.equals(type) || TYPE_ABSENT_FINDING.equals(type);
	}
	
	public boolean isAttribute(){
		return TYPE_ATTRIBUTE.equals(type);
	}
	
	
	public Concept getConcept() {
		return concept;
	}

	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	/**
	 * is this entry resolved (meta-data) available
	 * @return
	 */
	public boolean isResolved(){
		return resolved;
	}
	
	/**
	 * is this entry resolved (meta-data) available
	 * @return
	 */
	public void setResolved(boolean b){
		resolved = b;
	}
	
	
	
	/**
	 * get pretty name
	 * @return
	 */
	public String getText(){
		if(text == null){
			if(TYPE_HYPOTHESIS.equals(type) || TYPE_DIAGNOSIS.equals(type)){
				text = name.replaceAll("_"," ");
			}else if(isAnatomicLocation()){
				return UIHelper.getTextFromName(getAnatomicLocation());
			}else if(hasNumericValue()){
				text = UIHelper.getTextFromName(name).replaceAll("number",TextHelper.toString(getNumericValue()));
			}else{
				String n = (isAbsent() && !LABEL_NO.equals(name))?"NO ":"";
				text = n+UIHelper.getTextFromName(name);
			}
		}
		return text;
	}
	
	/**
	 * check if one item is equals to another
	 */
	public boolean equals(Object obj){
		if(obj instanceof ConceptEntry){
			ConceptEntry e = (ConceptEntry) obj;
			
			//  isAbsent() == e.isAbsent() && 
			return   getName().equals(e.getName()) && getType().equals(e.getType()) && 
				   	  OntologyHelper.compareValues(this,e) &&  
				   	  TextHelper.equals(getAnatomicLocation(),e.getAnatomicLocation());
		}
		return false;
	}
	
	
	/**
	 * generate unique hash code
	 */
	public int hashCode() {
		return (getType()+":"+getName()+":"+isAbsent()+":"+getNumericValue()+":"+getAnatomicLocation()).hashCode();
	}

	/**
	 * string representation
	 */
	public String toString(){
		return name;
	}

	public String getId() {
		if(id == null){
			// create unique id just-in-time
			synchronized(ConceptEntry.class){
				setId("Concept"+(count++));
			}
		}
		return id;
	}

	public boolean hasId(){
		return id != null;
	}
	
	/**
	 * reset concept id count 
	 */
	public static void resetConceptIdCount(){
		resetConceptIdCount(1);
	}
	
	/**
	 * reset concept id count 
	 */
	public static void resetConceptIdCount(int start){
		count = start;
	}
	
	public void setId(String id) {
		this.id = id;
		//text = null;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		//setId(type+":"+name);
		text = null;
	}

	public void setName(String name) {
		this.name = name;
		//setId(type+":"+name);
		text = null;
	}
	
	public double getNumericValue() {
		if(hasNumericValue())
			return numericValue;
		
		// if no finding, see if have attributes that have numerica values
		if(isFinding()){
			for(ConceptEntry a: getParentEntry().getAttributes()){
				if(a.hasNumericValue()){
					numericValue = a.getNumericValue();
					break;
				}
			}
		}
		return numericValue;
	}

	public void setNumericValue(double numericValue) {
		this.numericValue = numericValue;
	}

	public boolean hasNumericValue(){
		/* boolean b = numericValue != Double.MIN_VALUE;
		if(!b && isFinding() && attributes != null){
			for(ConceptEntry a: getAttributes()){
				if(a.hasNumericValue())
					return true;
			}
		}
		return b;*/
		return numericValue != Double.MIN_VALUE;
	}
	

	/**
	 * get concept status
	 * possible values are UNKNOWN, CORRECT, INCORRECT
	 * @param conceptStatus
	 */
	public int getConceptStatus() {
		if(isParentFinding())
			return getFeature().conceptStatus;
		return conceptStatus;
	}
	/**
	 * check if this concept is a parent finding container
	 * @return
	 */
	public boolean isParentFinding(){
		if(isFinding()){
			return feature != null && !equals(feature);
		}
		return false;
	}
	
	
	/**
	 * is correct diagnosis
	 * @return
	 */
	public boolean isCorrect(){
		return conceptStatus == CORRECT;
	}
	

	/**
	 * set concept response described by string
	 * @param response
	 */
	public void setConceptStatus(String response){
		if(RESPONSE_CONFIRM.equalsIgnoreCase(response))
			setConceptStatus(ConceptEntry.CORRECT);
		else if(RESPONSE_FAILURE.equalsIgnoreCase(response))
			setConceptStatus(ConceptEntry.INCORRECT);
		else if(RESPONSE_IRRELEVANT.equalsIgnoreCase(response))
			setConceptStatus(ConceptEntry.IRRELEVANT);
	}
	/**
	 * set concept status
	 * possible values are UNKNOWN, CORRECT, INCORRECT
	 * @param conceptStatus
	 */
	public void setConceptStatus(int conceptStatus) {
		// once something is irrelevant, don't ever change it to correct
		// WTF???? Why not???!?!
		//if(this.conceptStatus == IRRELEVANT && conceptStatus == CORRECT)
		//	return;
		
		// else reset concept status
		this.conceptStatus = conceptStatus;
		
		// clear errors if correct
		if(CORRECT == conceptStatus){
			clearErrors();
			
			// if this is feature clear errors for parent
			if(isFinding() && !isParentFinding())
				getParentEntry().clearErrors();
		}
		
		//System.out.println(getName()+" "+getConceptStatus()+" | "+getFeature()+" "+getFeature().getConceptStatus());
		if(TYPE_FINDING.equals(getType()) || TYPE_ABSENT_FINDING.equals(getType())){
			// if setting status of finding, do so to feature
			if(feature != null && !equals(feature) && conceptStatus != UNKNOWN){
				// if setting status of finding, do so to feature
				feature.setConceptStatus(conceptStatus);
				// set status for attributes, since finding status is about everything
				for(ConceptEntry a: getAttributes())
					a.setConceptStatus(conceptStatus);
			}
		}
	}
	
	
	/**
	 * concept copy status from one to another
	 * @param e
	 */
	public void copyConceptStatus(ConceptEntry entry){
		getFeature().setConceptStatus(entry.getFeature().getConceptStatus());
		for(ConceptEntry oa : entry.getAttributes()){
			for(ConceptEntry a: getAttributes()){
				if(a.equals(oa))
					a.setConceptStatus(oa.getConceptStatus());
			}
		}
	}
	
	
	/**
	 * get concept's FOK (feeling of knowing)
	 * UNKNOWN, SURE,UNSURE, INCORRECT
	 * @return
	 */
	public int getConceptFOK() {
		return conceptFOK;
	}

	public void setConceptFOK(int conceptFOK) {
		this.conceptFOK = conceptFOK;
		
		// if setting status of finding, do so to feature
		if(TYPE_FINDING.equals(getType()) || TYPE_ABSENT_FINDING.equals(getType())){
			if(!getFeature().equals(this) && conceptFOK != UNKNOWN)
				getFeature().setConceptFOK(conceptFOK);
		}
	}
	
	public void setConceptFOKString(String str){
		if(CONCEPT_SURE.equalsIgnoreCase(str))
			setConceptFOK(SURE);
		else if(CONCEPT_UNSURE.equalsIgnoreCase(str))
			setConceptFOK(UNSURE);
		else if(CONCEPT_ERROR.equalsIgnoreCase(str))
			setConceptFOK(ERROR);
	}

	
	/**
	 * get FOK string
	 * @return
	 */
	public String getFOKString(){
		switch(conceptFOK){
		case(SURE):
			return CONCEPT_SURE;
		case(UNSURE):
			return CONCEPT_UNSURE;
		case(ERROR):
			return CONCEPT_ERROR;
		}
		return CONCEPT_UNKNOWN;
	}
	
	
	/**
	 * get concept input
	 * @return
	 */
	public Object getInput() {
		return input;
	}

	public Annotation getAnnotation(){
		return annotation;
	}
	
	
	/**
	 * set concept input
	 * @param input
	 */
	public void setInput(Object input) {
		this.input = input;
		
		// do some setup
		if(input instanceof Annotation){
			Annotation tm = (Annotation) input;
			tm.setTagVisible(true);
			tm.addTag(getText());
			tm.addPropertyChangeListener(this);
			annotation = tm;
		}
	}

	
	/**
	 * remove this concept entry
	 */
	public void delete(){
		if(input != null && input instanceof Annotation){
			Annotation tm = (Annotation) input;
			tm.removePropertyChangeListener(this);
			// remove annotation
			tm.removeTag(getText());
			if(!tm.hasTag()){
				tm.getViewer().getAnnotationManager().removeAnnotation(tm);
				tm.delete();	
			}
		}
	}
	
	
	/**
	 * get locations where this feature can be observed
	 * those are annotation names or tags that will have to be
	 * resolved
	 * @return
	 */
	
	public List<String> getLocations() {
		return (locations != null)?locations:Collections.EMPTY_LIST;
	}
	
	
	/**
	 * set locations where this feature can be observed
	 * those are annotation names or tags that will have to be
	 * resolved
	 * @return
	 */
	public void setLocations(List<String> locations) {
		this.locations = locations;
	}
	
	/**
	 * add location
	 * @param s
	 */
	public void addLocation(String s){
		if(locations == null)
			locations = new ArrayList<String>();
		locations.add(s);
	}
	
	/**
	 * add location
	 * @param s
	 */
	public void addLocations(List<String> s){
		if(locations == null)
			locations = new ArrayList<String>();
		locations.addAll(s);
	}
	
	/**
	 * get example map
	 * @return
	 */

	public Map<String, String> getExamples() {
		if(examples == null){
			examples = new HashMap<String, String>();
		}
		return examples;
	}

	
	/**
	 * set examples 
	 * @param examples
	 */
	public void setExamples(Map<String, String> examples) {
		this.examples = examples;
	}
	
	/**
	 * set examples 
	 * @param examples
	 */
	public void addExamples(Map<String, String> e) {
		if(examples == null)
			examples = new HashMap<String, String>();
		examples.putAll(e);
	}
	
	
	/**
	 * add example
	 * @param feature
	 * @param example
	 */
	public void addExample(String feature, String example){
		if(examples == null)
			examples = new HashMap<String, String>();
		examples.put(feature,example);
	}
	
	
	/**
	 * get properties representation of this object
	 * @return
	 */
	public void setProperties(Properties p){
		p.setProperty("name",getName());
		
		// set value
		if(p.containsKey("numeric.value"))
			numericValue = Double.parseDouble(p.getProperty("numeric.value",""+NO_VALUE));
		
		// set anatomic location
		if(p.containsKey("resource.link"))
			resourceLink = p.getProperty("resource.link");
		
		// set absentConcept
		if(Boolean.parseBoolean(p.getProperty("is.absent",""+false))){
			setType(TYPE_ABSENT_FINDING);
		}
		
		// add locations		
		for(String s: TextHelper.parseList(p.getProperty("locations")))
			addLocation(s);
		
		//add example to feature and attributes
		for(Object k : p.keySet()){
			String key = ""+k;
			if(key.endsWith(".example")){
				String feature = key.substring(0,key.length()-8);
				String example = p.getProperty(key);
				addExample(feature, example);
			}
			
		}
		
		// add parts
		for(String s : TextHelper.parseList(p.getProperty("parts",""))){
			getParts().add(s);
		}
		
	}

	/**
	 * resolve tag
	 * @param tag
	 * @return
	 */
	public String resolveTag(String tag){
		// check if there is a custom tag
		if(getTagMap().containsKey(tag))
			return getTagMap().get(tag);
		
		// if we have concept, then just report on its name
		if(TAG_CONCEPT.equals(tag)){
			return (isAbsent())?UIHelper.getTextFromName(name):getText();
		}
		
		// check potential findings
		if(!getPotentialFindings().isEmpty()){
			return getPotentialFindings().get(0).resolveTag(tag);
		}
		
		// use parent to resolve tags
		if(this instanceof ActionEntry)
			return getParentEntry().resolveTag(tag);
		
		
		if(TAG_FEATURE.equals(tag)){
			ConceptEntry e = getFeature();
			if(e != null)
				return (isAbsent())?UIHelper.getTextFromName(e.getName()):e.getText();
		}else if(TAG_DEFINED_FEATURE.equals(tag)){
			return getParentEntry().getDefinedFeature().getText();
		}else if(TAG_FINDING.equals(tag)){
			return getText();
		}else if(TAG_POWER.equals(tag)){
			return (power == null)?getParentEntry().getPower():power;
		}else if(TAG_PARENT.equals(tag) || TAG_FINDING.equals(tag)){
			return getParentEntry().getText();
		}else if(TAG_POTENTIAL_ATTRIBUTES.equals(tag)){
			return TextHelper.toText(getPotentialAttributes());
		}else if(TAG_DEFINING_ATTRIBUTES.equals(tag)){
			return TextHelper.toText(getAttributeValues(OntologyHelper.MODIFIERS));
		}else if(TAG_ATTRIBUTES.equals(tag)){
			return TextHelper.toText(getAttributes());
		}else if(TAG_TEMPLATE.equals(tag)){
			return getTemplateEntry().getText();
		}else if(TAG_DEFINITION.equals(tag)){
			return getDefinition();
		}else if(TAG_ACTION.equals(tag)){
			String action = "observed";
			for(ActionEntry a: getParentEntry().getActions()){
				if(a.getName().toLowerCase().startsWith("measure"))
					action = "measured";
			}
			return action;
		}else if(TAG_VALUE.equals(tag)){
			return TextHelper.toString(getNumericValue());
		}else if(TAG_ATTRIBUTE.equals(tag)){
			String cat = getProbableAttributeCategory();
			return cat != null?cat.toLowerCase():"attribute";
		}else if(TAG_ATTRIBUTE_VALUES.equals(tag)){
			return TextHelper.toText(getAttributeValues(getProbableAttributeCategory()));
		}
		
		return (isAbsent())?UIHelper.getTextFromName(name):getText();
	}
	
	private String getProbableAttributeCategory(){
		for(ConceptEntry a :getAttributes()){
			if(!OntologyHelper.MODIFIERS.equals(a.getCategory()))
				return a.getCategory();
		}
		return null;
	}
	
	public ConceptEntry getDefinedFeature(){
		return definingFeature != null?definingFeature:getFeature();
	}
	
	public void setDefiniedFeature(ConceptEntry e){
		definingFeature = e;
	}
	
	/**
	 * get power
	 * @return
	 */
	public String getPower() {
		return power;
	}
	
	/**
	 * set power
	 * @param power
	 */
	public void setPower(String power) {
		this.power = power;
	}

	

	/**
	 * get message input
	 * @return
	 */
	public Map<String,String> getMessageInput(){
		// set input based on action
		Map<String,String> in = new HashMap<String, String>();
		// add location to input
		if(input != null){
			if(input instanceof Annotation){
				Annotation a = (Annotation) input;
				Map<String,String> map = new LinkedHashMap<String, String>();
				Rectangle r = a.getBounds();
				map.put("x",""+r.x);
				map.put("y",""+r.y);
				map.put("width",""+r.width);
				map.put("height",""+r.height);
				in.put(MESSAGE_INPUT_LOCATION,TextHelper.toString(map));
				in.put(MESSAGE_INPUT_POWER,ViewerHelper.convertScaleToLevelZoom(a.getViewPosition().scale));
			}else if(input instanceof ViewPosition){
				ViewPosition v = (ViewPosition) input;
				Map<String,String> map = new LinkedHashMap<String, String>();
				map.put("x",""+v.x);
				map.put("y",""+v.y);
				map.put("width",""+v.width);
				map.put("height",""+v.height);
				in.put(MESSAGE_INPUT_VIEW,TextHelper.toString(map));
				in.put(MESSAGE_INPUT_POWER,ViewerHelper.convertScaleToLevelZoom(v.scale));
			}
		}
		
		// add FOK
		if(getConceptFOK() != UNKNOWN){
			in.put(MESSAGE_INPUT_FOK,getFOKString());
		}
		
		// add resource link
		if(isAnatomicLocation())
			in.put(MESSAGE_INPUT_RESOURCE_LINK,resourceLink);
		
		// add number
		if(hasNumericValue())
			in.put(MESSAGE_INPUT_NUMERIC_VALUE,""+getNumericValue());
		
		// add misc properties
		in.putAll(getProperties());
		
		
		return in;
	}
	
	/**
	 * get a client that describes this object for a given
	 * action
	 * @param action
	 * @return
	 */
	public ClientEvent getClientEvent(TutorModule sender,String action){
		ClientEvent ce = new ClientEvent();
		ce.setType(getType());
		ce.setLabel(getLabel());
		ce.setAction(action);
		if(isIncludeParent())
			ce.setParent(getFeature().getObjectDescription());
		if(hasId())
			ce.setId(getId());
		ce.setObjectDescription(getObjectDescription());
		if(getParentEntry() != null)
			ce.setEntireConcept(getParentEntry().getObjectDescription());
		ce.setSender(sender);
		ce.setTimestamp(System.currentTimeMillis());
		
		// set input based on action
		ce.setInput(getMessageInput());
		
		return ce;
	}
	
	public String getParent(){
		if(TYPE_ATTRIBUTE.equals(getType()))
			return getFeature().getObjectDescription();
		return null;
	}

	/**
	 * get appropriate label for this entry
	 * @return
	 */
	public String getLabel(){
		if(hasNumericValue())
			return getName()+"="+getNumericValue();
		else if(isAnatomicLocation())
			return getName()+"="+getAnatomicLocation();
		return getName();
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		String cmd = e.getPropertyName();
		if(edu.pitt.slideviewer.Constants.UPDATE_SHAPE.equals(cmd)){
			pcs.firePropertyChange(cmd,null,this);
		}
	}

	/**
	 * get object representation in some interface
	 * @return the location
	 */
	public List getRepresentations() {
		if(representations == null)
			representations = new ArrayList();
		return representations;
	}

	/**
	 * @param location the location to set
	 */
	public void addRepresentation(Object r) {
		getRepresentations().add(r);
	}
	
	/**
	 * does node representing this concept have a location
	 * @return
	 */
	public boolean hasRepresentation(){
		return !getRepresentations().isEmpty();
	}

	
	/**
	 * is concept entry important in given case
	 * @return
	 */
	public boolean isImportant() {
		return important;
	}
	
	/**
	 * is concept entry important in given case
	 * @return
	 */
	public void setImportant(boolean important) {
		this.important = important;
	}

	/**
	 * is concept entry present in given case
	 * @return
	 */
	public boolean isInCase() {
		return incase;
	}

	/**
	 * is concept entry present in given case
	 * @return
	 */
	public void setInCase(boolean incase) {
		this.incase = incase;
	}
	
	/**
	 * get an arbitrary list of tag/value pairs that
	 * can be used to resolve some error tags
	 * @return
	 */
	public Map<String,String> getTagMap(){
		if(tags == null)
			tags = new HashMap<String, String>();
		return tags;	
	}
	
	public void setTagMap(Map<String,String> map){
		tags = map;
	}
	

	/**
	 * get parent entry (feature). This is a "feature" of the old
	 * that is the most general for this concept
	 * @param feature
	 */
	public ConceptEntry getFeature() {
		return (feature != null)?feature:this;
	}

	
	/**
	 * set parent entry. This is a "feature" of the old
	 * that is the most general for this concept
	 * @param feature
	 */
	public void setFeature(ConceptEntry f) {
		if(f == null)
			return;
		
		if(f.equals(feature)){
			// if we already have similar feature set
			// retain id and status infromation from it 
			// before replacing it
			if(feature.hasId())
				f.setId(feature.getId());
			f.setConceptStatus(feature.getConceptStatus());
		}else if(f.equals(this) && id == null){
			// if we are setting a feature of the entry to "itself" then we
			// might as well reset its ID and status information
			if(f.hasId())
				setId(f.getId());
			setConceptStatus(f.getConceptStatus());
			return;
		}
		
		feature = f;
		// if this is NOT an attribute set parent to this
		if(!Arrays.asList(TYPE_ATTRIBUTE,TYPE_ACTION).contains(getType()))
			feature.setParentEntry(this);
	}

	
	/**
	 * get parent entry. This is a finding that this concept belongs to
	 * @param feature
	 */
	public ConceptEntry getParentEntry() {
		if(parent == null)
			parent = this;
		return parent;
	}

	/**
//	 * does this finding have a parent entry?
	 * @return
	 */
	public boolean hasParentEntry(){
		return !equals(getParentEntry());
	}
	
	/**
	 * set parent entry. This is a finding that this concept belongs to
	 */
	public void setParentEntry(ConceptEntry parent) {
		this.parent = parent;
		if(parent != null)
			setAuto(parent.isAuto());
	}
	
	/**
	 * set parent entry. This is a finding that this concept belongs to
	 */
	public void setTemplateEntry(ConceptEntry parent) {
		this.template = parent;
	}

	public ConceptEntry getTemplateEntry(){
		return (template != null)?template:this;
	}
	
	public List<ConceptEntry> getAttributes() {
		if(attributes == null)
			attributes = new ArrayList<ConceptEntry>();
		return attributes;
	}
	
	public void addAttribute(ConceptEntry a){
		// what if such attribute is already there, then reuse its ID
		if(getAttributes().contains(a)){
			ConceptEntry old = attributes.remove(attributes.indexOf(a));
			a.setId(old.getId());
			a.setConceptStatus(old.getConceptStatus());
		}
		
		// now add this attribute
		getAttributes().add(a);
		a.setParentEntry(this);
		if(getFeature() != null)
			a.setFeature(getFeature());
	}
	public void removeAttribute(ConceptEntry a){
		getAttributes().remove(a);
	}

	public List<ConceptEntry> getPotentialAttributes() {
		if(potentialAttributes == null)
			potentialAttributes = new ArrayList<ConceptEntry>();
		
		Collections.sort(potentialAttributes,new Comparator<ConceptEntry>() {
			public int compare(ConceptEntry o1, ConceptEntry o2) {
				if(o1.hasNumericValue() && o2.hasNumericValue())
					return (int)o1.getNumericValue() - (int)o2.getNumericValue();
				return o1.getText().compareTo(o2.getText());
			}
		});
	
		return potentialAttributes;
	}

	public List<ConceptEntry> getAttributeValues(String attribute) {
		List<ConceptEntry> potentialModifiers = new ArrayList<ConceptEntry>();
		for(ConceptEntry a: getPotentialAttributes()){
			if(attribute == null || attribute.equals(a.getCategory()))
				potentialModifiers.add(a);
		}
		if(potentialModifiers.isEmpty())
			return getPotentialAttributes();
		return potentialModifiers;
	}
	
	
	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<ConceptEntry> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param potentialAttributes the potentialAttributes to set
	 */
	public void setPotentialAttributes(List<ConceptEntry> potentialAttributes) {
		this.potentialAttributes = potentialAttributes;
	}


	/**
	 * get object description for this concept
	 * @return
	 */
	public String getObjectDescription(){
		String s = getType()+"."+getLabel()+(hasId()?"."+getId():"");
		if(isIncludeParent())
			s = getFeature().getObjectDescription()+":"+s;
		return s;
	}

	private boolean isIncludeParent(){
		return (TYPE_ATTRIBUTE.equals(getType()) && !this.equals(getFeature()));
		//		TYPE_RECOMMENDATION.equals(getType());
	}
	
	
	/**
	 * get concept entry based on object description
	 * @param description in 
	 * @return
	 */
	public static ConceptEntry getConceptEntry(String description){
		if(description == null)
			return null;
		
		// check for parent parts of description
		String parent = null;
		int x = description.lastIndexOf(":");
		if(x > -1){
			parent      = description.substring(0,x);
			description = description.substring(x+1);
		}
		// get this concept entry
		String [] s = description.split("\\.");
		if(s.length < 2)
			return null;
		
		// check for links
		ConceptEntry e = null;
		if(parent != null && parent.contains(":")){
			int y = parent.indexOf(":");
			e = new LinkConceptEntry(
					getConceptEntry(parent.substring(0,y)),
					getConceptEntry(parent.substring(y+1)),s[0]);
			if(s.length > 2)
				e.setId(s[2]);
			return e;
		}else{
			String tp = s[0];
			String nm = s[1];
			String id = null;
			
			// if has an id then set it
			if(s.length > 2){
				// set id as last component
				id = s[s.length-1];
				
				// if we have more then three periods, we might have a period
				// in the name
				for(int i=2;i<s.length-1;i++)
					nm +="."+s[i];
			}
			
			// initialize concept entry
			e = new ConceptEntry(nm,tp);
			if(id != null)
				e.setId(id);
			if(parent != null)
				e.setFeature(getConceptEntry(parent));
		}
		return e;
	}

	public boolean isActionComplete() {
		for(ActionEntry a: getActions())
			if(!a.isActionComplete())
				return false;
		return true;
	}

	public boolean isHeaderFinding() {
		return headerFinding;
	}

	public boolean isWorksheetFinding() {
		return worksheetFinding;
	}
	
	public void setWorksheetFinding(boolean b){
		worksheetFinding = b;
	}
	public void setHeaderFinding(boolean b){
		headerFinding = b;
	}
	
	public boolean isAnatomicLocation(){
		return resourceLink != null;
	}
	public String getAnatomicLocation(){
		if(resourceLink != null){
			if(resourceLink.lastIndexOf("#") > -1)
				return resourceLink.substring(resourceLink.lastIndexOf("#")+1);
			else
				return resourceLink;
		}
		return null;
	}

	public void addAction(ActionEntry actionEntry) {
		getActions().add(actionEntry);
		actionEntry.setParentEntry(this);
		actionEntry.setFeature(getFeature());
	}
	
	public List<ActionEntry> getActions(){
		if(actions == null)
			actions = new ArrayList<ActionEntry>();
		return actions;
	}
	public void setActions(List<ActionEntry> a){
		actions = a;
	}
	
	/**
	 * get absentConcept concept0
	 * @return
	 */
	public static ConceptEntry getAbsentConcept(){
		return new ConceptEntry(LABEL_NO,TYPE_ATTRIBUTE);
	}
	
	/**
	 * get iconic representation
	 * @return
	 */
	public Icon getIcon(){
		if(icon == null){
			icon = new Icon() {
				private Color color = Constants.CONCEPT_ICON_COLOR;
				private Stroke stroke = new BasicStroke(2);
				private String text = getText();
				private Point offset = new Point(4,2);
				private Font font = new Font("Dialog",Font.PLAIN,12);
				private boolean trancated;
				
				// get height
				public int getIconHeight(){
					return Constants.CONCEPT_ICON_SIZE.height;
				}
				// get width
				public int getIconWidth(){
					return Constants.CONCEPT_ICON_SIZE.width;
				}
				
				// paint graphics
				public void paintIcon(Component c, Graphics g, int x, int y){
					int w = getIconWidth()-2*offset.x;
					int h = getIconHeight()-2*offset.y;
					Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
							RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
							RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setStroke(stroke);
					
					// fill rectangle
					g.setColor(color);
					g.fillRect(x+offset.x,y+offset.y,w,h);
					
					// truncate text
					if(!trancated){
						FontMetrics fm = c.getFontMetrics(font);
						int n = fm.stringWidth(text);
						if(n > getIconWidth()){
							String s = text;
							int k = (int)(w/fm.stringWidth("W"));
							do{
								s = text.substring(0,k);
							}while(k++ < text.length() &&  fm.stringWidth(s) < (w-20));
							text = s;
						}
						trancated = true;
					}
					// draw text
					g.setFont(font);
					g.setColor(Color.black);
					g.drawString(text,x+5+offset.x,y+h-7+offset.x);
					// do outline
					g.setColor(Color.green);
					g.drawRect(x+offset.x,y+offset.y,w,h);
				}
			};
		}
		return icon;
	}
}
