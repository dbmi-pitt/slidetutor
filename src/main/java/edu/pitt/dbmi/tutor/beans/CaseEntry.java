package edu.pitt.dbmi.tutor.beans;



import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.ontology.*;
import edu.pitt.slideviewer.Viewer;


/**
 * this bean represents a case entry
 * @author tseytlin
 *
 */
public class CaseEntry {
	private OrderedMap<String,SlideEntry> slides;
	private Map<String,OrderedMap<String,ConceptEntry>> concepts;
	private SlideEntry primarySlide;
	private String report, name, clinicalInfo, domain;
	private Map<String,List<ShapeEntry>> annotationMap;
	private Map<String,ShapeEntry []> conceptAnnotations;
	private Set<String> parts;
	private Properties properties;
	private ExpertModule expert;
	private Collection<ConceptEntry> reportFindings;
	
	
	public void setExpertModule(ExpertModule e){
		expert =  e;
	}
	
	public ExpertModule getExpertModule(){
		return expert;
	}
	
	public Properties getProperties(){
		if(properties == null)
			properties = new Properties();
		return properties;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		// make sure we don't have Instances nonsense
		if(domain.endsWith(INSTANCES_ONTOLOGY))
			domain = domain.substring(0,domain.length()-INSTANCES_ONTOLOGY.length())+OWL_SUFFIX;
		this.domain = domain;
	}

	/**
	 * get list of slides
	 * @return
	 */
	public List<SlideEntry> getSlides() {
		if(slides == null)
			slides = new OrderedMap<String,SlideEntry>();
		return slides.getValues();
	}
	
	/**
	 * set slides
	 * @param slides
	 */
	public void setSlides(List<SlideEntry> list) {
		this.slides = new OrderedMap<String,SlideEntry>();
		for(SlideEntry s: list){
			slides.put(s.getSlideName(),s);
			
			// set primary slide
			if(primarySlide == null || s.isPrimarySlide())
				setPrimarySlide(s);
		}
	}
	
	public SlideEntry getOpenSlide(){
		for(SlideEntry e: getSlides()){
			if(e.isOpened())
				return e;
		}
		return null;
	}
	
	/**
	 * add slides
	 * @param slides
	 */
	public void addSlide(SlideEntry s) {
		if(slides == null)
			slides = new OrderedMap<String,SlideEntry>();
		slides.put(s.getSlideName(),s);
		
		// set primary slide
		if(primarySlide == null || s.isPrimarySlide())
			setPrimarySlide(s);
	}
	
	/**
	 * add slides
	 * @param slides
	 */
	public void removeSlide(SlideEntry s) {
		if(slides == null)
			slides = new OrderedMap<String,SlideEntry>();
		slides.remove(s.getSlideName());
	}
	
	/**
	 * get concepts of certain types
	 * @param type
	 * @return
	 */
	public OrderedMap<String,ConceptEntry> getConcepts(String category ) {
		OrderedMap<String,ConceptEntry> map = getConceptMap().get(category);
		if(map != null){
			return map;
		}
		return new OrderedMap<String,ConceptEntry>();
	}
	
	/**
	 * get concepts of certain types
	 * @param type
	 * @return
	 */
	public List<ConceptEntry> getConcepts() {
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(String category: getConceptMap().keySet()){
			list.addAll(getConceptMap().get(category).getValues());
		}
		return list;
	}
	
	
	/**
	 * get concept entry that matches concept name
	 * @param name
	 * @return
	 */
	public ConceptEntry getConcept(String name){
		for(String category : OntologyHelper.getConceptCategories()){
			OrderedMap<String,ConceptEntry> map = getConcepts(category);
			if(map.containsKey(name))
				return map.get(name);
		}
		return null;
	}
	
	
	
	/**
	 * add new concept
	 * @param category
	 * @param concept
	 */
	public void addConcept(String category ,ConceptEntry concept){
		OrderedMap<String,ConceptEntry> map = getConceptMap().get(category);
		if(map != null){
			map.put(concept.getName(),concept);
		}
	}
	
	/**
	 * add new concept
	 * @param type
	 * @param concept
	 */
	public void removeConcept(String category,ConceptEntry concept){
		OrderedMap<String,ConceptEntry> map = getConceptMap().get(category);
		if(map != null){
			map.remove(concept.getName());
		}
	}
	
	/**
	 * get concept map
	 * @return
	 */
	private Map<String,OrderedMap<String,ConceptEntry>> getConceptMap(){
		if(concepts == null){
			concepts = new HashMap<String, OrderedMap<String,ConceptEntry>>();
			for(String type: OntologyHelper.getConceptCategories())
				concepts.put(type,new OrderedMap<String, ConceptEntry>());
		}
		return concepts;
	}
	
	
	/**
	 * get primary slide
	 * @return
	 */
	public SlideEntry getPrimarySlide() {
		return primarySlide;
	}
	
	/**
	 * set primary slide
	 * @param primarySlide
	 */
	public void setPrimarySlide(SlideEntry primarySlide) {
		this.primarySlide = primarySlide;
	}
	
	/**
	 * get case report
	 * @return
	 */
	public String getReport() {
		return report;
	}
	
	public String getReportSection(String header){
		// backword compatibility hack
		if(concepts != null && OntologyHelper.PROCEDURE.equalsIgnoreCase(header)){
			for(ConceptEntry e: concepts.get(PROGNOSTIC_FEATURES).getValues()){
				if(OntologyHelper.PROCEDURE.equals(e.getTemplateEntry().getName())){
					return "\n"+e.getText();
				}
			}
		}
		return TextHelper.getSectionText(header,report);
	}
	
	
	/**
	 * get clinical information
	 * (extract from the report)
	 * @return
	 */
	public String getClinicalInfo(){
		if(clinicalInfo == null){
			String a = TextHelper.getSectionText(OntologyHelper.PATIENT_HISTORY,report);
			String b = TextHelper.getSectionText(OntologyHelper.GROSS_DESCRIPTION,report);
			clinicalInfo = OntologyHelper.PATIENT_HISTORY+":"+a+
					"\n\n"+OntologyHelper.GROSS_DESCRIPTION+":"+b;
		}
		return clinicalInfo;
	}
	
	
	/**
	 * set report
	 * @param report
	 */
	public void setReport(String report) {
		this.report = report;
	}
	
	/**
	 * get name
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * set name
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	/**
     * load
     * @param is
     * @param manager
     * @throws IOException
     */
    public void load(InputStream is) throws IOException {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	String line,field = null;
    	StringBuffer buffer = new StringBuffer();
    	Pattern pt = Pattern.compile("\\[([\\w\\.\\-]+)\\]");
    	Map<String,String> map = new HashMap<String,String>();
    	while((line = reader.readLine()) != null){
    		line = line.trim();
    		// skip comments
    		if(line.startsWith("#"))
    			continue;
    		// extract headers
    		Matcher mt = pt.matcher(line);
    		if(mt.matches()){
    			// save previous field
    			if(field != null){
    				map.put(field,buffer.toString());
    				buffer = new StringBuffer();
    			}
    			field = mt.group(1);
    		}else{
    			buffer.append(line+"\n");
    		}
    	}
    	// finish the last item
    	if(field != null && buffer.length() > 0){
    		map.put(field,buffer.toString());
    	}
    	reader.close();
    
    	
     	Set<String> exclude = new HashSet<String>();
     	//IOntology ont = instance.getOntology();
     	
    	// set case name
    	if(map.containsKey("CASE")){
    		exclude.add("CASE");
    		Properties p = TextHelper.getProperties(map.get("CASE"));
    		// get gace name
    		String name = p.getProperty("name");
    		setName(name);
    		
    		// get case domain
    		String domain = p.getProperty("domain");
    		setDomain(domain);
    		//if(domain != null && !domain.equals(this.domain)){
    		//	throw new IOException("Case file seems to belong to a different domain: "+domain);
    		//}
    		
    		// load meta data
    		for(Object key: p.keySet()){
    			// skip known entries
    			if(!"status".equals(key) && !"domain".equals(key) && !"name".equals(key)){
    				getProperties().setProperty(""+key,p.getProperty(""+key));
    			}
    		}
    		
    	}else
    		throw new IOException("Case file is missing a [CASE] header");	
    	
    	// set report
    	if(map.containsKey("REPORT")){
    		exclude.add("REPORT");
    		report = map.get("REPORT").trim();
    	}
    	
    	// load slides 
    	if(map.containsKey("SLIDES")){
    		exclude.add("SLIDES");
    		for(String s: map.get("SLIDES").trim().split("\n")){
    			if(s.length() > 0){
	    			SlideEntry slide = new SlideEntry(s);
	    			exclude.add(s);
	    			slide.setProperties(TextHelper.getProperties(map.get(s)));
	    			addSlide(slide);
    			}
			}
    	}
    	
    	
    	// load concepts into case
    	for(String key: OntologyHelper.getConceptCategories()){
    		exclude.add(key);
	    	if(map.containsKey(key)){
	    		for(String name: map.get(key).trim().split("\n")){
	    			if(name.length() > 0){
		    			exclude.add(name);
		    			ConceptEntry entry = new ConceptEntry(name,OntologyHelper.getConceptTypeForCategory(key));
		    			entry.setProperties(TextHelper.getProperties(map.get(name)));
		    			addConcept(key,entry);
	    			}
	    		}
	    	}
    	}
    	
    	// load all remaining annotations
    	//int num = 0;
    	//pt = Pattern.compile("[A-Za-z]+(\\d+)");
    	ArrayList<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
    	for(String key: map.keySet()){
    		// if not in exclude list, then it is an annotation
    		if(!exclude.contains(key)){
    			Properties p = TextHelper.getProperties(map.get(key));
    			// further make sure that it is a shape
    			if(p.containsKey("tag")){
    				ShapeEntry entry = new ShapeEntry();
    				entry.setProperties(p);
    				shapes.add(entry);
    				/*
    				Matcher mt = pt.matcher(entry.getName());
    				if(mt.matches()){
    					int x = Integer.parseInt(mt.group(1));
    					if(x > num)
    						num = x;
    				}*/
    			}
    		}
    	}
    	//manager.setAnnotationNumber(num);
    	Collections.sort(shapes);
    	for(ShapeEntry entry: shapes){
    		if(slides.containsKey(entry.getImage())){
				slides.get(entry.getImage()).addAnnotation(entry);
			}
    	}
    }
    
    /**
     * get annotation map for this case
     * @return
     */
    private Map<String,List<ShapeEntry>> getAnnotationMap(){
    	if(annotationMap == null){
    		annotationMap = new HashMap<String, List<ShapeEntry>>();
    		for(SlideEntry slide: getSlides()){
    			for(ShapeEntry shape: slide.getAnnotations()){
    				annotationMap.put(shape.getName(),Collections.singletonList(shape));
    				for(String tag : shape.getTags()){
    					List<ShapeEntry> list = annotationMap.get(tag);
    					if(list == null){
    						list = new ArrayList<ShapeEntry>();
    						annotationMap.put(tag,list);
    					}
    					list.add(shape);
    				}
    			}
    		}
    	}
    	return annotationMap;
    }
   
    /**
     * get annotation map for this case
     * @return
     */
    private Map<String,ShapeEntry []> getEntryAnnotationMap(){
    	if(conceptAnnotations == null){
    		conceptAnnotations = new HashMap<String, ShapeEntry[]>();
    	}
    	return conceptAnnotations;
    }
    
    /**
     * get all location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry [] getLocations(ConceptEntry concept){
    	return getLocations(concept,null);
    }
    
    /**
     * get all location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry [] getLocations(ConceptEntry concept, String image){
    	if(concept == null)
    		return new ShapeEntry [0];
    	
    	
    	// check if we already saved locations
    	if(getEntryAnnotationMap().containsKey(concept.getName()+"-"+image))
    		return getEntryAnnotationMap().get(concept.getName()+"-"+image);
    	
    	// else create     	
    	List<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
    	Map<String,List<ShapeEntry>> map = getAnnotationMap();
    	
    	// get list of locations, if
    	List<String> locations = concept.getLocations();
    	if(locations.isEmpty()){
    		locations = new ArrayList<String>();
    		// try to find equivalent concept in case
    		for(ConceptEntry e: OntologyHelper.getMatchingFindings(this,concept)){
    			locations.addAll(e.getLocations());
    		}
    	}
    	
    	
    	// exclude examples from that list
    	List<ShapeEntry> examples = new ArrayList<ShapeEntry>();
    	Collections.addAll(examples,getExamples(concept, image));
    	
    	// go over all locations
    	for(String loc: locations){
    		if(map.containsKey(loc)){
    			for(ShapeEntry sh : map.get(loc)){
    				// check current image
    				if(image == null || image.contains(sh.getImage())){
	    				//if(!sh.isArrow())
    					// if not an example
    					if(!examples.contains(sh) && sh.isLocation())
	    					shapes.add(sh);
    				}
    			}
    		}
    	}
    	
    	// return and save
    	ShapeEntry [] toret =  shapes.toArray(new ShapeEntry [0]);
    	getEntryAnnotationMap().put(concept.getName()+"-"+image,toret);
    	return toret;
    }
   

    /**
     * get all location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry [] getExamples(ConceptEntry concept){
    	return getExamples(concept,null);
    }
    
    /**
     * get all location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry [] getExamples(ConceptEntry concept,String image){
    	if(concept == null)
    		return new ShapeEntry [0];
    	
    	List<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
    	Map<String,List<ShapeEntry>> map = getAnnotationMap();
    	
    	
     	// get list of locations, if
    	Collection<String> locations = concept.getExamples().values();
    	if(locations.isEmpty()){
    		locations = new ArrayList<String>();
    		// try to find equivalent concept in case
    		// try to find equivalent concept in case
    		for(ConceptEntry e: OntologyHelper.getMatchingFindings(this,concept)){
    			locations.addAll(e.getExamples().values());
    		}
    	}
    	
    	// go over all locations
    	for(String loc: locations){
    		if(map.containsKey(loc)){
    			for(ShapeEntry e : map.get(loc)){
    				if(image == null || image.contains(e.getImage())){
    					// ruler should not be an example, since it is
    					// a measurement tool
    					//if(!"ruler".equalsIgnoreCase(e.getType()))
    					shapes.add(e);
    				}
    			}
    		}
    	}
    	
    	return shapes.toArray(new ShapeEntry [0]);
    }
    
    
    
    /**
     * get nearest location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry getNearestLocation(ConceptEntry concept, Viewer viewer){
    	return getNearesShape(getLocations(concept), viewer);
    }
    
    /**
     * get nearest location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry getNearestExample(ConceptEntry concept, Viewer viewer){
    	return getNearesShape(getExamples(concept), viewer);
    }
    
    /**
     * get nearest location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry getNearestShape(ConceptEntry concept, Viewer viewer){
    	return getNearestShape(concept,null, viewer);
    }
    
    /**
     * get nearest location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry getNearestShape(ConceptEntry concept, String type, Viewer viewer){
    	List<ShapeEntry> list = new ArrayList<ShapeEntry>();
    	for(ShapeEntry [] array: Arrays.asList(getExamples(concept),getLocations(concept))){
	    	for(ShapeEntry s: array){
	    		if(type == null || type.equalsIgnoreCase(s.getType()))
	    			list.add(s);
	    	}
    	}
    	return getNearesShape(list.toArray(new ShapeEntry [0]), viewer);
    }
    
    
    /**
     * get nearest location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry getNearesShape(ShapeEntry [] shapes, Viewer viewer){
    	ShapeEntry shape = null;
    	
    	// get all locations
    	double distance = Double.MAX_VALUE;
    	for(ShapeEntry s: shapes){
    		// if the same image
    		if(viewer.hasImage() && viewer.getImage().contains(s.getImage())){
    			// get centers of viewer rectangle and shape
    			Rectangle vr = viewer.getViewRectangle();
    			Rectangle sr = s.getShape().getBounds();
    			Point2D vp = new Point2D.Double(vr.getCenterX(),vr.getCenterY()); 
    			Point2D sp = new Point2D.Double(sr.getCenterX(),sr.getCenterY()); 
    			
    			// now measure a distance
    			double d = vp.distance(sp);
    			if(d < distance){
    				distance = d;
    				shape = s;
    			}
    			
    		}
    	}
    	
    	return shape;
    }
    
    /**
     * create an instance that represents this case 
     * this instance would be completly authored
     * @param ont
     * @return
     */
    public IInstance createInstance(IOntology ont){
    	String name = "authored_"+getName();
    	IInstance inst = getInstance(ont.getClass(CASES),name);
    	
    	for(String category: getConceptCategories()){
    		for(ConceptEntry e: getConcepts(category).getValues()){
    			if(DIAGNOSES.equals(category)){
    				IClass c = ont.getClass(e.getName());
    				if(!inst.hasType(c))
    					inst.addType(c);
    			}else{
	    			String prop = getPropertyForCategory(category, e.isAbsent());
	    			if(prop != null){
	    	    		// add value to new case instance (don't care if it is correct or not
						String iname = (e.isAbsent()?"no_"+e.getName():e.getName()).toLowerCase();
	    				IInstance i = getInstance(ont.getClass(e.getName()),iname);
						IProperty p = ont.getProperty(prop);
						if(!inst.hasPropetyValue(p,i))
							inst.addPropertyValue(p,i);
	    			}
    			}
			}
    	}
    	
    	return inst;
    }
    
    
    /**
     * get a set of parts that correspond to this case 
     * @return
     */
    public Set<String> getParts(){
    	if(parts == null)
    		parts = new LinkedHashSet<String>();
    	return parts;
    }
    
    
    /**
     * set parts
     * @param p
     */
    public void setParts(Set<String> p){
    	parts = p;
    }
    
    /**
     * get a list of reportable findings in correct order
     * (list may include both dx and prognostic findings
     * @return
     */
    public Collection<ConceptEntry> getReportFindings(){
    	if(reportFindings == null){
    		reportFindings = new ArrayList<ConceptEntry>();
    		reportFindings.addAll(concepts.get(DIAGNOSES).getValues());
    		reportFindings.addAll(concepts.get(PROGNOSTIC_FEATURES).getValues());
      	}
    	return reportFindings;
    }

    public void setReportFindings(Collection<ConceptEntry> reportFindings) {
		this.reportFindings = reportFindings;
	}

}
