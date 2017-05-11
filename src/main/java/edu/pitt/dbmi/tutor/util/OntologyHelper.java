package edu.pitt.dbmi.tutor.util;


import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ConceptFilter;
import edu.pitt.dbmi.tutor.beans.ShapeEntry;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ConceptLabel;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IInstance;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.IResource;
import edu.pitt.ontology.IRestriction;
import edu.pitt.ontology.LogicExpression;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.ontology.ui.OntologyExplorer;
import edu.pitt.slideviewer.markers.PolygonUtils;
import edu.pitt.text.tools.TextTools;

/**
 * varies ontology related methods
 * @author tseytlin
 */
public class OntologyHelper {
	// report sections
	public static final String PATIENT_HISTORY = "PATIENT HISTORY";
	public static final String GROSS_DESCRIPTION = "GROSS DESCRIPTION";
	
	
	// ontology names
	public static final String EVS_TERMINOLOGY = "Enterprise Vocabulary Service";
	public static final String ONTOLOGY_TERMINOLOGY = "Ontology Terminology";
	public static final String LUCENE_TERMINOLOGY = "Lucene Terminology";
	public static final String REMOTE_TERMINOLOGY = "Remote Terminology";
	public static final String KNOWLEDGE_BASE = "KnowledgeBase.owl";
	public static final String ANATOMY_ONTOLOGY = "AnatomicalSites.owl";
	public static final String OWL_SUFFIX = ".owl";
	public static final String CASE_SUFFIX = ".case";
	public static final String TERMS_SUFFIX = ".terms";
	public static final String CONFIG_SUFFIX = ".conf";
	public static final String INSTANCES_ONTOLOGY = "Instances"+OWL_SUFFIX;
	public static final String EXAMPLES_FOLDER = "examples";
	public static final String CASES_FOLDER = "cases";
	public static final String SPREADSHEET_FOLDER = "spreadsheets";
	public static final String DEFAULT_HOST_URL = "http://slidetutor.upmc.edu";
	public static final String DEFAULT_BASE_URI = "http://slidetutor.upmc.edu/curriculum/owl/";
	public static final String DEFAULT_TUTOR_HELP_FILE = "/resources/TutorHelp.xml";
	public static final String DEFAULT_REPORT_HELP_FILE = "/resources/ReportHelp.xml";
	public static final String CURRICULUM_ROOT = "curriculum";
	public static final String KNOWLEDGE_FOLDER = "owl";
	public static final String CONFIG_FOLDER = "config";
	public static final URI KNOWLEDGE_BASE_URI = URI.create(DEFAULT_BASE_URI+KNOWLEDGE_BASE);
	public static final URI ANATOMY_ONTOLOGY_URI = URI.create(DEFAULT_BASE_URI+ANATOMY_ONTOLOGY);
	public static final String DEFAULT_FILE_MANAGER_SERVLET = DEFAULT_HOST_URL+"/domainbuilder/servlet/FileManagerServlet";
	public static final double NO_VALUE = Double.MIN_VALUE;
	public static final String DEFAULT_JNLP_SERVLET = DEFAULT_HOST_URL+"/its/JNLPServlet";
	public static final String DEFAULT_CONFIG_URL = DEFAULT_HOST_URL+"/curriculum/config/";
	
	// class names
	public static final String CONCEPTS = "CONCEPTS";
	public static final String CASES = "CASES";
	public static final String SCHEMAS = "TEMPLATES";
	//public static final String LEXICON = "LEXICON";
	public static final String ACTIONS = "ACTIONS";
	public static final String WORKSHEET = "WORKSHEET";
	public static final String NUMERIC = "Number";
	public static final String PROCEDURE = "Procedure";
	public static final String ANATOMIC_LOCATION = "Anatomic_Location";
	public static final String INVOLVED = "involved";
	public static final String TISSUE = "Tissue";
	
	
	public static final String DIAGNOSES = "DIAGNOSES";
	public static final String RECOMMENDATIONS = "RECOMMENDATIONS";
	public static final String ANCILLARY_STUDIES = "ANCILLARY_STUDIES";
	public static final String DIAGNOSTIC_FEATURES = "DIAGNOSTIC_FINDINGS";
	public static final String PROGNOSTIC_FEATURES = "PROGNOSTIC_FINDINGS";
	public static final String CLINICAL_FEATURES = "CLINICAL_FINDINGS";
	public static final String ARCHITECTURAL_FEATURES = "ARCHITECTURAL_FEATURES";
	public static final String CYTOLOGIC_FEATURES = "CYTOLOGIC_FEATURES";
	
	public static final String FEATURES = "FINDINGS";
	public static final String ATTRIBUTES = "ATTRIBUTES";
	public static final String MODIFIERS = "MODIFIERS";
	public static final String LOCATIONS = "LOCATION";
	public static final String VALUES = "VALUES";
	
	public static final String ACTION_OBSERVE_ALL = "Observe_all";
	public static final String ACTION_OBSERVE_SOME = "Observe_some";
	public static final String ACTION_OBSERVE_SLIDE = "Observe_slide";
	public static final String ACTION_MEASURE_MM2   = "Measure_with_mm2";
	public static final String ACTION_MEASURE_RULER = "Measure_with_ruler";
	public static final String ACTION_MEASURE_HPF   = "Measure_with_HPF";
	public static final String ACTION_MEASURE_10HPF   = "Measure_with_10HPF";
	
	
	public static final String HAS_CLINICAL = "hasClinicalFinding";
	public static final String HAS_ANCILLARY = "hasAncillaryStudies";
	public static final String HAS_FINDING = "hasFinding";
	public static final String HAS_NO_FINDING = "hasAbsentFinding";
	public static final String HAS_PROGNOSTIC = "hasPrognostic";
	public static final String HAS_TRIGGER    = "hasTrigger";
	public static final String HAS_ACTION 	  = "hasAction";
	
	public static final String HAS_CONCEPT_CODE = "code";
	public static final String HAS_EXAMPLE = "example";
	public static final String HAS_REPORT = "hasReport";
	public static final String HAS_SLIDE = "hasImage";
	public static final String HAS_POWER = "power";
	public static final String HAS_NUMERIC_VALUE = "hasNumericValue";
	public static final String IS_ABSENT = "isAbsent";
	public static final String HAS_ORDER = "order";
	
	public static final String POWER_LOW = "low";
	public static final String POWER_MEDIUM = "medium";
	public static final String POWER_HIGH = "high";
	
	
	private static Map<IClass,IClass> featureMap;
	
	
	/**
	 * reset static resources s.a feature map
	 */
	public static void reset(){
		featureMap = null;
	}
	
	/**
	 * Get instance with given name, if it doesn't exist
	 * create it
	 * @param name
	 * @return
	 */
	public static IInstance getInstance(IClass cls, String name){
		IInstance inst = cls.getOntology().getInstance(name);
		if(inst == null)
			inst = cls.createInstance(name);
		return inst;
	}
	
	/**
	 * Get instance with given name, if it doesn't exist
	 * create it
	 * @param name
	 * @return
	 */
	public static IInstance getInstance(IClass cls){
		return getInstance(cls,cls.getName().toLowerCase());
	}
	
	/**
	 * get case base ontology from knowledge base
	 * @param ont
	 * @return
	 */
	public static String getCaseBase(String u){
		if(u.endsWith(".owl"))
			u = u.substring(0,u.length()-4);
		return u + INSTANCES_ONTOLOGY;
	}
	
	/**
	 * get concept categories
	 * @return
	 */
	public static String [] getConceptCategories(){
		return new String [] {DIAGNOSTIC_FEATURES,PROGNOSTIC_FEATURES,
						CLINICAL_FEATURES,ANCILLARY_STUDIES,DIAGNOSES,RECOMMENDATIONS};
	}
	
	
	/**
	 * get matching property for category
	 * @param category
	 * @param absent
	 * @return
	 */
	public static String getPropertyForCategory(String category,boolean absent){
		if(DIAGNOSTIC_FEATURES.equals(category)){
			return (absent)?HAS_NO_FINDING:HAS_FINDING;
		}else if(PROGNOSTIC_FEATURES.equals(category)){
			return HAS_PROGNOSTIC;
		}else if(CLINICAL_FEATURES.equals(category)){
			return (absent)?HAS_NO_FINDING:HAS_CLINICAL;
		}else if(ANCILLARY_STUDIES.equals(category)){
			return HAS_ANCILLARY;
		}	
		return null;
	}
	
	/**
	 * get concept type for categories
	 * @param category
	 * @return
	 */
	public static String getConceptTypeForCategory(String category){
		if(DIAGNOSES.equals(category))
			return Constants.TYPE_DIAGNOSIS;
		if(RECOMMENDATIONS.equals(category))
			return Constants.TYPE_RECOMMENDATION;
		return Constants.TYPE_FINDING;
	}
	
	/**
	 * get base URL for ontology 
	 * Example: for ontology http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/Melanocytic.owl
	 * it returns http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/
	 * @param ont
	 * @return
	 */
	public static URL getExampleURL(IOntology ont){
		try{
			String path = ""+ont.getURI();
			
			// strip the owl suffix
			if(path.endsWith(OWL_SUFFIX))
				path = path.substring(0,path.length()-OWL_SUFFIX.length());
			
			// replace /owl/ with /CASE/
			path = path.replaceAll("/owl/","/"+EXAMPLES_FOLDER+"/");
			
			// replace /domainbuilder/ with /curriculum/
			// for backword compatibility
			path = path.replaceAll("/domainbuilder/","/curriculum/");
		
			return new URL(path);
		}catch(MalformedURLException ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * find a subtree in the root node and return it, based on exact match
	 * @param root
	 * @param name
	 * @return
	 */
	public static TreeNode getSubTree(TreeNode root, String name){
		// if root is what we are looking for, get it
		if((""+root).equals(name))
			return root;
		// else go into children (depth-first)
		TreeNode node = null;
		for(int i=0; i< root.getChildCount(); i++){
			node = getSubTree(root.getChildAt(i), name);
			if(node != null)
				break; 
		}
		return node;
	}
	
	/**
	 * constract tree from list of paths
	 * @param paths
	 * @return
	 */
	public static TreeNode getTree(List<TreePath> paths){
		return getTree(paths,null);
	}
	/**
	 * constract tree from list of paths
	 * @param paths
	 * @return
	 */
	public static TreeNode getTree(List<TreePath> paths, String root){
		// check for empty list
		if(paths == null || paths.isEmpty())
			return null; //new DefaultMutableTreeNode("EMPTY");
		
		// iterate over paths
		Map<String,DefaultMutableTreeNode> map = new LinkedHashMap<String,DefaultMutableTreeNode>();
		for(TreePath path: paths){
			DefaultMutableTreeNode parent = null;
			for(Object n: path.getPath()){
				DefaultMutableTreeNode node = map.get(""+n);
				if(node == null){
					node = new DefaultMutableTreeNode(""+n);
					map.put(""+n,node);
				}
				// add as child to parent
				if(parent != null)
					parent.add(node);
				parent = node;
			}
		}
		// the root should be the very first entry in linked table
		return (map.containsKey(root))?map.get(root):map.get(map.keySet().iterator().next());
	}
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getDomainFromCase(String problem){
		String domain = problem;
		domain = domain.replaceAll("/"+CASES_FOLDER+"/","/"+KNOWLEDGE_FOLDER+"/");
		int i = domain.lastIndexOf("/");
		if(i > -1){
			domain = domain.substring(0,i);
		}
		return domain+OWL_SUFFIX;
	}
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getCasePath(URI uri){
		String path = uri.getPath();
		// strip the owl suffix
		if(path.endsWith(OWL_SUFFIX))
			path = path.substring(0,path.length()-OWL_SUFFIX.length());
		
		// replace /owl/ with /CASE/
		path = path.replaceAll("/"+KNOWLEDGE_FOLDER+"/","/"+CASES_FOLDER+"/");
		
		// replace /domainbuilder/ with /curriculum/
		// for backword compatibility
		//path = path.replaceAll("/domainbuilder/","/curriculum/");
		
		return path;
	}
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getCasePath(IOntology ont){
		return getCasePath(ont.getURI());
	}
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getSpreadsheetPath(IOntology ont){
		String path = ont.getURI().getPath();
		// strip the owl suffix
		if(path.endsWith(ont.getName()))
			path = path.substring(0,path.length()-ont.getName().length());
		
		// replace /owl/ with /CASE/
		path = path.replaceAll("/"+KNOWLEDGE_FOLDER+"/","/"+SPREADSHEET_FOLDER+"/");
		
		// replace /domainbuilder/ with /curriculum/
		// for backword compatibility
		//path = path.replaceAll("/domainbuilder/","/curriculum/");
		
		return path;
	}
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getExamplePath(IOntology ont){
		String path = ont.getURI().getPath();
		// strip the owl suffix
		if(path.endsWith(OWL_SUFFIX))
			path = path.substring(0,path.length()-OWL_SUFFIX.length());
		
		// replace /owl/ with /CASE/
		path = path.replaceAll("/"+KNOWLEDGE_FOLDER+"/","/"+EXAMPLES_FOLDER+"/");
		
		// replace /domainbuilder/ with /curriculum/
		// for backword compatibility
		//path = path.replaceAll("/domainbuilder/","/curriculum/");
		
		return path;
	}
	
	
	/**
	 * get relational distance between two classes
	 * if c1 is a direct parent of c2 then distance is 1
	 * if c1 == c2 distance is 0
	 * if c1 is not related to c2, then answer is max int
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static int getDistance(IClass c1, IClass c2){
		if(c1.equals(c2))
			return 0;
		
		// check if c2 is a child of c1
		if(c1.hasSubClass(c2)){
			for(IClass c: c2.getDirectSuperClasses())
				if(c1.equals(c) || c1.hasSubClass(c))
					return 1 + getDistance(c1, c);
		}
		
		// check if c2 is a parent of c1
		if(c1.hasSuperClass(c2)){
			for(IClass c: c1.getDirectSuperClasses())
				if(c2.equals(c) || c2.hasSubClass(c))
					return 1 + getDistance(c,c2);
		}
		
		return Integer.MAX_VALUE;
	}
	
	
	/**
	 * convert power to integer
	 * @param pow
	 * @return
	 */
	public static int powerToInteger(String pow){
		if(POWER_LOW.equalsIgnoreCase(pow)){
			return 1;
		}else if(POWER_MEDIUM.equalsIgnoreCase(pow)){
			return 2;
		}else if(POWER_HIGH.equalsIgnoreCase(pow)){
			return 3;
		}
		return 0;
	}
	
	
	
	/**
	 * is something a knowledge base class
	 * @param cls
	 * @return
	 */
	public static boolean isSystemClass(IClass cls){
		return cls != null && cls.getNameSpace().contains(KNOWLEDGE_BASE);
	}
	
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isAttribute(IClass c){
		return isOfParent(c,ATTRIBUTES);
	}
	
	
	
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isModifier(IClass c){
		return isOfParent(c,MODIFIERS);
	}
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isValue(IClass c){
		return (isOfParent(c,VALUES) && !NUMERIC.equals(c.getName())) && !isFeature(c);
	}
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isNumber(IClass c){
		return (NUMERIC.equals(c.getName()) || isOfParent(c,NUMERIC)) && !isFeature(c);
	}
	

	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isAnatomicLocation(IClass c){
		return ANATOMIC_LOCATION.equals(c.getName()) || isOfParent(c,ANATOMIC_LOCATION);
	}
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isWorksheet(IClass c){
		return isOfParent(c,WORKSHEET);
	}
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isHeader(IClass c){
		return c != null && c.hasDirectSuperClass(c.getOntology().getClass(PROGNOSTIC_FEATURES)) && isSystemClass(c);
	}
	
	/**
	 * is something a location
	 * @param c
	 * @return
	 */
	public static boolean isLocation(IClass c){
		return isOfParent(c,LOCATIONS);
	}
	
	/**
	 * is something a location
	 * @param c
	 * @return
	 */
	public static boolean isDirectLocation(IClass c){
		return c.hasDirectSuperClass(c.getOntology().getClass(LOCATIONS));
	}
	
	
	/**
	 * check if this entry is a feature NOTE that FAVs are also features
	 * @return
	 */
	public static boolean isFeature(IClass c){
		return isOfParent(c,FEATURES);
	}
	
	
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isDisease(IClass c){
		return isOfParent(c,DIAGNOSES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isDiagnosticFeature(IClass cls){
		return isOfParent(cls,DIAGNOSTIC_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isArchitecturalFeature(IClass cls){
		return isOfParent(cls,ARCHITECTURAL_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isCytologicFeature(IClass cls){
		return isOfParent(cls,CYTOLOGIC_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isPrognosticFeature(IClass cls){
		return isOfParent(cls,PROGNOSTIC_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isClinicalFeature(IClass cls){
		return isOfParent(cls,CLINICAL_FEATURES);
	}
	
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isAncillaryStudy(IClass cls){
		return isOfParent(cls,ANCILLARY_STUDIES);
	}
	
	
	
	/**
	 * is this class an attribute category
	 * @param cls
	 * @return
	 */
	public static boolean isAttributeCategory(IClass cls){
		return isAttribute(cls) && !isDisease(cls) && cls.getName().matches("[A-Z_]+");
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isOfParent(IClass cls,String parent){
		if(cls == null)
			return false;
		IOntology o = cls.getOntology();
		IClass p = o.getClass(parent);
		return cls.equals(p) || cls.hasSuperClass(p);
	}
	
	
	/**
	 * is given class a feature or diagnoses, but not an attribute
	 * @param c
	 * @return
	 */
	public static boolean isNamedFeature(IClass c){
		return (isFeature(c) || isDisease(c)) && !isSystemClass(c) && (!isAttribute(c) || isLocation(c));
	}
	
	/**
	 * is given class a attribute, but not finding
	 * @param c
	 * @return
	 */
	public static boolean isNamedAttribute(IClass parent){
		return (isAttribute(parent) && (!isSystemClass(parent) || isValue(parent)) && !isAttributeCategory(parent) &&
				  (!(isFeature(parent) || isDisease(parent)) || isDirectLocation(parent)));
	}
	
	
	/**
	 * get concept entry type for a class
	 * @param cls
	 * @return
	 */
	public static String getEntryType(IClass cls){
		if(isDisease(cls))
			return Constants.TYPE_DIAGNOSIS;
		else if(isFeature(cls))
			return Constants.TYPE_FINDING;
		else if(isAttribute(cls))
			return Constants.TYPE_ATTRIBUTE;
		return Constants.TYPE_FINDING;
	}
	
	/**
	 * find a feature/disease inside a potential finding
	 * @param cls
	 * @return
	 *
	public static IClass findFeature(IClass cls){
		if(cls == null)
			return null;
		
		Queue<IClass> queue = new LinkedList<IClass>();
		queue.add(cls);
		
		// bredth first search
		while(!queue.isEmpty()){
			IClass c = queue.poll();
			if(isNamedFeature(c) && isDescribed(c)){
				return c;
			}
			
			// look at next level
			for(IClass p: c.getDirectSuperClasses()){
				queue.add(p);
			}
			
		}
		return null;
	}*/
	
	/**
	 * find a feature/disease inside a potential finding
	 * @param cls
	 * @return
	 */
	public static IClass getFeature(IClass cls){
		if(cls == null)
			return null;
		
		// init table
		if(featureMap == null)
			featureMap = new HashMap<IClass, IClass>();
		
		// short cut for a feature
		if(featureMap.containsKey(cls))
			return featureMap.get(cls);
		
		
		// feature is the class itself by default
		IClass parent = cls;
		for(IClass p: cls.getDirectSuperClasses()){
			if(OntologyHelper.isSystemClass(p))
				continue;
			
			// if direct super class is more general, then lets look further
			// once in a blue moon, we have a direct superclass not being in general form, but its parent is
			// Ex:  Infectious_Cause -> Bacterial_Infectious_Cause -> Actinomycotic_Infectious_Cause
			if(isFeature(p) && (isGeneralForm(p,cls,false) || isGeneralForm(getFeature(p),cls,false))){
				// reset feature if it is equal to class or it is NOT preposition
				if(parent.equals(cls) || isGeneralForm(p,cls,true))
					parent = getFeature(p);
				//break;
			}
		}
		
		// at this point we know the parent, lets save this
		featureMap.put(cls,parent);
		
		return parent;
	}
	
	/**
	 * get a list of attributes/modifiers belong to this finding
	 * @param cls
	 * @return
	 */
	public static List<IClass> getAttributes(IClass cls){
		List<IClass> list = new ArrayList<IClass>();
		
		// if feature is itself, don't bother
		IClass feature = getFeature(cls);
		if(feature.equals(cls))
			return list;
		
		for(IClass p: cls.getSuperClasses()){
			if(isNamedAttribute(p) || isNumber(p)){
				// make sure that list contains only the most specific attributes
				// we don't want four AND number appearing here
				IClass torem = null;
				for(IClass c: list){
					if(p.hasSubClass(c)){
						// do not insert a more general class
						torem = p;
					}else if(p.hasSuperClass(c)){
						// remove more general class
						torem = c;
					}
				}
				// add new item, remove old item (or itself)
				list.add(p);
				list.remove(torem);
			}
		}
		// if feature is itself an attribute, we want to exclude it
		list.remove(feature);
		
		return list;
	}

	
	/**
	 * get a list of available attributes/modifiers for a given finding
	 * @param cls
	 * @return
	 */
	public static List<IClass> getPotentialAttributes(IClass cls){
		Set<IClass> list = new LinkedHashSet<IClass>();
		IClass feature = getFeature(cls);
		for(IClass p: feature.getSubClasses()){
			if(feature.equals(getFeature(p)))
				list.addAll(getAttributes(p));
		}
		return new ArrayList<IClass>(list);
	}
	
	
	
	/**
	 * is parent a more general version of child?
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean isGeneralForm(IClass parent, IClass child){
		return isGeneralForm(parent, child,true);
	}
	
	/**
	 * is parent a more general version of child?
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean isGeneralForm(IClass parent, IClass child, boolean filterPrepositionalFeature){
		// shortcut to save time
		if(child.getName().contains(parent.getName()) && !filterPrepositionalFeature){
			// number of words in child should exceed the number of words in parent
			if(TextHelper.getSequenceCount(child.getName(),"_") > TextHelper.getSequenceCount(parent.getName(),"_"))
				return true;
		}
		
		// get words from parents and children
		String [] pnames = TextTools.getWords(UIHelper.getTextFromName(parent.getName()));
		String [] cnames = TextTools.getWords(UIHelper.getTextFromName(child.getName()));
		
		// normalize words
		List<String> plist = new ArrayList<String>();
		for(String s: pnames){
			plist.add(TextTools.stem(s));
		}
		// this is a map, to make lookup constant
		Map<String,String> clist = new LinkedHashMap<String,String>();
		for(String s: cnames){
			clist.put(TextTools.stem(s),"");
		}
		
		// now check for general form
		boolean general = true;
		for(String s: plist){
			general &= clist.containsKey(s);
		}
		
		// now check for prepositions in features
		// if we have a positive match
		if(general && filterPrepositionalFeature){
			// if in front of first parent word there is a preposition
			// then this maybe a false positive
			boolean preposition = false;
			for(String c: clist.keySet()){
				if(TextTools.isPrepositionWord(c)){
					preposition = true;
				}
				
				// as soon as we have a feature (contained in parent list)
				// check preposition, if preposition is true, then false positive
				if(plist.contains(c)){
					general = !preposition;
					break;
				}
			}
		}
		
		
		return general;
	}
	
	/**
	 * filter to filter out everything, but features
	 * @author tseytlin
	 *
	 */
	public static class FeatureFilter implements ConceptFilter {
		private IOntology ont;
		public FeatureFilter(IOntology ont){
			this.ont = ont;
		}
		public boolean accept(String parent,String name) {
			IClass cls = ont.getClass(name);
			return cls.equals(getFeature(cls));
		}
	}
	
	
	/**
	 * filter to filter out everything, but features
	 * @author tseytlin
	 *
	 */
	public static class FindingFilter implements ConceptFilter {
		private IOntology ont;
		private int level = 0;
		public FindingFilter(IOntology ont){
			this.ont = ont;
		}
		public FindingFilter(IOntology ont,int level){
			this.ont = ont;
			this.level = level;
		}
		public boolean accept(String parent, String name) {
			IClass pcls = ont.getClass(parent);
			IClass cls = ont.getClass(name);
			IClass f = getFeature(cls);
			//return (!cls.equals(f))?getFeature(pcls).equals(f):true;
			// if this class is not a feature, then
			if(!cls.equals(f)){
				// if feature of parent equals to feature of class in question
				if(getFeature(pcls).equals(f)){
					if(level > 0){
						// start with a feature
						IClass c = f;
						for(int i=0;i<level;i++){
							// go over direct children 
							for(IClass ch: c.getDirectSubClasses()){
								// if our class is a child, then we are good
								if(ch.equals(cls)){
									return true;
								}else if(cls.hasSuperClass(ch)){
									c = ch;
								}
							}
						}
						return false;
					}
					return true;
				}else
					return false;
			}
			// else class is a feature
			return true;
		}
	}
	
	
	
	/**
	 * filter to filter out everything, but features
	 * @author tseytlin
	 *
	 */
	public static class AttributeFilter implements ConceptFilter {
		private IOntology ont;
		public AttributeFilter(IOntology ont){
			this.ont = ont;
		}
		public boolean accept(String parent, String name) {
			IClass cls = ont.getClass(name);
			// if direct parent is one of the attribute categories or it is not a feature
			return Arrays.asList(LOCATIONS,MODIFIERS,VALUES).contains(parent) || !isFeature(cls);
		}
	}

	
	/**
	 * find a list of attributes
	 * @param cls
	 * @return
	public static List<IClass> findAttributes(IClass cls){
		return findAttributes(cls,findFeature(cls));
	}
	*/
	/**
	 * find a list of attributes
	 * @param cls
	 * @return
	 *
	public static List<IClass> findAttributes(IClass cls, IClass feature){
		List<IClass> list = new ArrayList<IClass>();
				
		// if class is null, or class is just a feature
		if(cls == null || cls.equals(feature))
			return list;
		
		Queue<IClass> queue = new LinkedList<IClass>();
		queue.add(cls);
		
		// bredth first search
		boolean stop = false;
		Set<IClass> set = new LinkedHashSet<IClass>();
		while(!queue.isEmpty()){
			IClass c = queue.poll();
			// if named attribute, add
			// if parent is feature, stop recursion further
			if(isNamedAttribute(c)){
				set.add(c);
			}else if(c.equals(feature)){
				stop = true;
			}
			
			// look at next level
			if(!stop && !isNamedAttribute(c)){
				for(IClass p: c.getDirectSuperClasses()){
					queue.add(p);
				}
			}
			
		}
		// now add the set to list
		list.addAll(set);
		return list;
	}
	*/
	
	/**
	 * is class described (aka has glossary description, examples, etc
	 * @param cls
	 * @return
	 */
	public static boolean isDescribed(IClass cls){
		if(cls == null)
			return false;
		
		return cls.getComments().length > 0;
	}
	
	
	/**
	 * is finding in diagnostic rule of dx, on property prop
	 * @param fn
	 * @param prop
	 * @param dx
	 * @return
	 */
	public static boolean isFindingInDiagnosticRule(IClass fn,ILogicExpression rule){
		return isFindingInDiagnosticRule(fn, null, rule);
	}
	
	/**
	 * is finding in diagnostic rule of dx, on property prop
	 * @param fn
	 * @param prop
	 * @param dx
	 * @return
	 */
	public static boolean isFindingInDiagnosticRule(IClass fn, IProperty prop, ILogicExpression rule){
		for(Object obj: rule){
			if(obj instanceof ILogicExpression){
				if(isFindingInDiagnosticRule(fn, prop,(ILogicExpression) obj))
					return true;
			}else if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				if(prop == null || r.getProperty().equals(prop)){
					if(isFindingInDiagnosticRule(fn, prop,r.getParameter()))
						return true;
				}
			}else if(obj instanceof IClass){
				IClass c = (IClass) obj;
				// we want to be a little bit more lenient
				// and allow say Blister in case to match Subepidermal_Blister in rule
				// In this scenario for WE are ok if rule finding is more specific
				// then what is in case as long as what is in case is at least as specific
				// as a feature
				IClass feature = OntologyHelper.getFeature(c);
				if(c.equals(fn) || c.hasSubClass(fn) ||
				   ((feature.equals(fn) || feature.hasSubClass(fn)) && c.hasSuperClass(fn)))
					return true;
			}
		}
		return false;
	}
	
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	public static String getDiseaseRuleText(IClass dx){
		return getDiseaseRuleText(dx,null);
	}
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	public static String getDiseaseRuleText(IClass dx, IInstance inst){
		if(dx == null)
			return "";
		
		//return getDiseaseRuleText(dx.getOntology(),dx.getEquivalentRestrictions());
		String s = null;
		ILogicExpression exp = dx.getEquivalentRestrictions();
		if(inst != null){
			exp = getMatchingPatterns(dx, inst);
		}
		
		
		if(exp.getExpressionType() == ILogicExpression.OR && exp.size() > 1){
			StringBuffer bf = new StringBuffer();
			for(Object o: exp){
				if(o instanceof ILogicExpression){
					bf.append("["+getDiseaseRuleText(dx.getOntology(),(ILogicExpression) o)+"]\n");
				}
			}
			s = bf.toString();
		}else{
			s = getDiseaseRuleText(dx.getOntology(), exp);
		}
		return s;
		
		//return TextHelper.formatExpression(dx.getEquivalentRestrictions());
	}
	
	
	/**
	 * find out which pattern does this case match
	 * @param evidence
	 * @param dpatterns
	 * @return
	 */
	public static ILogicExpression getMatchingPatterns(IClass dx, IInstance inst){
		// check for multi-pattern
		ILogicExpression result = new LogicExpression(ILogicExpression.OR);
		ILogicExpression exp = dx.getEquivalentRestrictions();
		if(exp.getExpressionType() == ILogicExpression.OR){
			for(int i=0;i<exp.size();i++){
				if(exp.get(i) instanceof ILogicExpression){
					ILogicExpression e = (ILogicExpression) exp.get(i);
					// if this pattern matches
					if(e.evaluate(inst)){
						result.add(exp.get(i));
					}
				}
			}
		}
		return result;
	}
	
	
	/**
	 * get direct parent
	 * @param c
	 * @return
	 */
	private static IClass getDirectParent(IClass c){
		// check if direct parent is already part of the rule
		IClass parent = null;
		for(IClass p: c.getDirectSuperClasses()){
			if(isDiagnosticFeature(p)){
				parent = p;
				break;
			}
		}
		return parent;
	}
	
	
	
	/**
	 * remove repetition in string
	 * @param c
	 * @param str
	 * @return
	 */
	private static StringBuffer removeRepetition(IClass c, StringBuffer str){
		// check if direct parent is already part of the rule
		IClass parent = getDirectParent(c);
		
		if(parent != null){
			String ps =  UIHelper.getTextFromName(parent.getName());
			if(str.toString().contains(ps)){
				str = new StringBuffer(str.toString().replaceAll(ps+" ",""));
			}else{
				return removeRepetition(parent,str);
			}
		}
		return str;
	}
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	public static String getDiseaseRuleText(IOntology ont,ILogicExpression exp){
		StringBuffer str  = new StringBuffer();
		String s =(exp.getExpressionType() == ILogicExpression.OR)?" or ":", ";
		
		for(Object o: exp){
			if(o instanceof ILogicExpression){
				ILogicExpression l = (ILogicExpression) o;
				//String pre = "";
				//if(str.length() > 0 && l.getExpressionType() == ILogicExpression.OR){
				//	pre = " and ";
				//}
				str.append(getDiseaseRuleText(ont,l)+s);
			}else if(o instanceof IRestriction){
				IRestriction r = (IRestriction) o;
				IProperty p = r.getProperty();
				//ILogicExpression l = r.getParameter();
				
				String pre = "";
				if(ont.getProperty(HAS_NO_FINDING).equals(p)){
					pre += "no ";
				}
				
				if(ont.getProperty(HAS_FINDING).equals(p) || ont.getProperty(HAS_NO_FINDING).equals(p)){
					ILogicExpression c = r.getParameter();
					if(c.size() == 1 && c.get(0) instanceof IClass){
						// check if direct parent is already part of the rule
						str = removeRepetition((IClass) c.get(0),str);
					}
					str.append(pre+getDiseaseRuleText(ont,c)+s);
				}
			}else if(o instanceof IResource){
				String name = UIHelper.getTextFromName(((IResource)o).getName());
				/*
				if(o instanceof IClass){
					// check if direct parent is already part of the rule
					// if we have attributes as prefix, then this behaviour is desirable
					// if we have attributes as suffix, then we want to removeRepetition from name
					//TODO: sometimes text is still fucked up
					IClass oc = (IClass) o;
					IClass p = getDirectParent(oc);
					if(str.length() > 0 && p != null && 
					    str.toString().contains(UIHelper.getTextFromName(p.getName())) && oc.getName().startsWith(""+p))
						name = ""+removeRepetition(oc,new StringBuffer(name));
					else
						str = removeRepetition(oc,str);	
				}*/
				str.append(name+s);
			}
		}
		// remove last s
		if(str.length() > s.length())
			str.delete(str.length()-s.length(),str.length());
		return str.toString();
	}
	
	
	
	/**
	 * create finding from a given feature and a set of attributes
	 * if no such findings exists in the knowledge base, then the name is a comma separated list
	 * @param feature
	 * @param attributes
	 * @param ont
	 * @return
	 */
	public static ConceptEntry createFinding(ConceptEntry feature, List<ConceptEntry> attributes, IOntology ont){
		IClass f = getConceptClass(feature,ont);
		List<ConceptEntry> concepts = new ArrayList<ConceptEntry>(attributes);
		
		if(f == null)
			return null;
		
		// see if we can specify
		IClass finding = f;
		int previousSize = 0;
		while(concepts.size() != previousSize){
			previousSize = concepts.size();
			for(ConceptEntry attribute : new ArrayList<ConceptEntry>(concepts)){
				IClass a = ont.getClass(attribute.getName());
				if(a != null){
					List<IClass> common = getDirectCommonChildren(finding,a);
					
					// only one common child, no confusion
					// if multiple, try our luck next time
					if(common.size() == 1){
						finding = common.get(0);
						concepts.remove(attribute);
					}
					
				}
			}
		}
		
		// if we were completly unsucsessful on the first pass, lets see
		// if we can be more lenient this time around
		//TODO: either use this fix or not
		/*
		if(finding.equals(f)){
			previousSize = 0;
			while(concepts.size() != previousSize){
				previousSize = concepts.size();
				for(ConceptEntry attribute : new ArrayList<ConceptEntry>(concepts)){
					IClass a = ont.getClass(attribute.getName());
					if(a != null){
						IClass  c = getCommonChild(finding,a);
						
						// only one common child, no confusion
						// if multiple, try our luck next time
						if(c != null){
							finding = c;
							concepts.remove(attribute);
						}
					}
				}
			}
		}
		*/
		
		// create name for new entry
		String name = finding.getName();
		for(ConceptEntry e: concepts){
			name += ", "+e.getName();
		}

		// create finding
		ConceptEntry entry = new ConceptEntry(name,feature.getType());
		entry.setFeature(feature);
		for(ConceptEntry a: attributes)
			entry.addAttribute(a);
		
		return entry;
	}
	
	
	
	/**
	 * get a more sepcific concept name, from a set of selected attributes
	 * @param paths
	 * @return
	 *
	public static String getCompoundConcept(IOntology ont, String feature, TreePath [] attributes){
		List<String> list = new ArrayList<String>();
		list.add(ont.getClass(feature).getName());
		for(TreePath p: attributes)
			list.add(""+p.getLastPathComponent());
		return getCompoundConcept(ont,list);
	}
	*/
	/**
	 * get a more sepcific concept name, from a set of selected attributes
	 * @param paths
	 * @return
	 *
	public static String getCompoundConcept(IOntology ont, String feature, List<ConceptEntry> attributes){
		List<String> list = new ArrayList<String>();
		list.add(ont.getClass(feature).getName());
		for(ConceptEntry p: attributes)
			list.add(p.getName());
		return getCompoundConcept(ont,list);
	}
	*/
	/**
	 * get a more sepcific concept name, from a set of selected attributes
	 * @param paths
	 * @return
	 *
	public static String getCompoundConcept(IOntology ont, ConceptEntry feature, List<ConceptEntry> attributes){
		return getCompoundConcept(ont,feature.getName(),attributes);
	}
	*/
	
	/**
	 * get a more sepcific concept name, from a set of selected attributes
	 * @param paths
	 * @return
	 *
	public static String getCompoundConcept(IOntology ont, List<String> parts){
		List<IClass> list = new ArrayList<IClass>();
		for(String p: parts)
			list.add(ont.getClass(p));
		
		// narrow down the list to the most specific concept
		list = getCommonConcepts(list);
		
		// convert to string
		String s = ""+list;
		return s.substring(1,s.length()-1).trim();
	}
	*/
	
	/**
	 * process a list of simple concepts and attempts to merge
	 * them into a meaningful compunt concepts
	 */
	public static List<IClass> getCommonConcepts(List<IClass> concepts){
		// compact concept list until it stops changing size
		int previousSize = 0;
		while(concepts.size() != previousSize){
			previousSize = concepts.size();
			IClass previous = null;
			for(IClass entry: new ArrayList<IClass>(concepts)){
				// check if you can merge concepts
				if(previous != null){
					//check if there is a common higher level concept
					IClass common = mergeConcepts(previous, entry,concepts);
					if(common == null){
						//if not,check if there is another higher level concept that can be 
						//constructed with components of current concept
					}else{
						entry = common;
					}
				}
				previous = entry;
			}
		}
		
		// sort that most specific concept first
		Collections.sort(concepts,new Comparator<IClass>() {
			public int compare(IClass a, IClass b) {
				return b.getName().compareTo(a.getName());
			}
		});
		return concepts;
	}
	
		
	
	/**
	 * attempt to merge two concepts
	 * @param previous
	 * @param entry
	 * @return
	 */
	private static IClass mergeConcepts(IClass pc, IClass ec, Collection<IClass> concepts){
		IClass common = getDirectCommonChild(pc,ec);
		if(common != null){
			// update list
			concepts.remove(ec);
			concepts.remove(pc);
			concepts.add(common);
			return common;
		}
		return null;
	}
	
	
	/**
	 * get common parent of two classes
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static IClass getDirectCommonChild(IClass c1, IClass c2){
		// take care of base conditions
		if(c1.equals(c2))
			return c1;
		if(c1.hasDirectSubClass(c2))
			return c2;
		if(c2.hasDirectSubClass(c1))
			return c1;
		
		// check direct children
		List<IClass> c1c = Arrays.asList(c1.getDirectSubClasses());
		List<IClass> c2c = Arrays.asList(c2.getDirectSubClasses());
		for(IClass c: c1c){
			if(c2c.contains(c))
				return c;
		}
		return null;
	}
	
	/**
	 * get common parent of two classes
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static List<IClass> getDirectCommonChildren(IClass c1, IClass c2){
		// take care of base conditions
		if(c1.equals(c2))
			return Collections.singletonList(c1);
		if(c1.hasDirectSubClass(c2))
			return Collections.singletonList(c2);
		if(c2.hasDirectSubClass(c1))
			return Collections.singletonList(c1);
		
		// check direct children
		List<IClass> c1c = Arrays.asList(c1.getDirectSubClasses());
		List<IClass> c2c = Arrays.asList(c2.getDirectSubClasses());
		List<IClass> result = new ArrayList<IClass>();
		for(IClass c: c1c){
			if(c2c.contains(c))
				result.add(c);
		}
		return result;
	}
	
	
	/**
	 * get direct common parent class
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static IClass getDirectCommonParent(IClass c1, IClass c2){
		for(IClass p: c1.getDirectSuperClasses()){
			if(c2.hasDirectSuperClass(p))
				return p;
		}
		return null;
	}
	
	/**
	 * get common child of two classes
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static IClass getCommonChild(IClass c1, IClass c2){
		// take care of base conditions
		if(c1 == null)
			return c2;
		if(c2 == null)
			return c1;
		if(c1.equals(c2))
			return c1;
		if(c1.hasSubClass(c2))
			return c2;
		if(c2.hasSubClass(c1))
			return c1;
		
		// check direct children
		List<IClass> c1c = Arrays.asList(c1.getSubClasses());
		List<IClass> c2c = Arrays.asList(c2.getSubClasses());
		
		// pick the most specific class
		List<IClass> cc = new ArrayList<IClass>();
		for(IClass c: c1c){
			if(c2c.contains(c)){
				//return c;
				cc.add(c);
			}
		}
		if(cc.size() == 1)
			return cc.get(0);
		else if(cc.size() > 1){
			IClass g = null;
			for(IClass c: cc){
				if(g == null || c.hasSubClass(g))
					g = c;
			}
			return g;
		}
		return null;
	}
	/**
	 * get the most specific common parent of two classes
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static IClass getCommonParent(IClass c1, IClass c2){
		return getCommonParent(c1, c2,null);
	}
	
	/**
	 * get the most specific common parent of two classes
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static IClass getCommonParent(IClass c1, IClass c2,IClass branch){
		// take care of base conditions
		if(c1.equals(c2))
			return c1;
		if(c1.hasSubClass(c2))
			return c1;
		if(c2.hasSubClass(c1))
			return c2;
		
		// if direct parents were not found, recurse further
		IClass parent = null;
		for(IClass c : c1.getDirectSuperClasses()){
			IClass p = getCommonParent(c,c2);
			// keep the most specific parent
			if(parent == null || parent.hasSubClass(p) || (branch != null && branch.hasSubClass(p)))
				parent = p;
		}
		
		return parent;
		
	}
	
	/**
	 * is inner class a part-of outer class 
	 * Ex: Blister is part-of Subepidermal_Blister
	 * @param inner
	 * @param outer
	 * @param o
	 * @return
	 */
	public static boolean hasSubClass(ConceptEntry inner, ConceptEntry outer, IOntology ont){
		IClass i = ont.getClass(inner.getName());
		IClass o = ont.getClass(outer.getName());
		//System.out.println("Is "+i+" a parent of "+o);
		if(i != null && o != null)
			return i.hasSubClass(o);
		return false;
	}
	
	/**
	 * get subclasses of a concept
	 */
	public static List<ConceptEntry> getSubClasses(ConceptEntry entry, IOntology ont){
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		IClass cls = getConceptClass(entry,ont);
		if(cls != null){
			for(IClass c: cls.getSubClasses()){
				list.add(new ConceptEntry(c.getName(),entry.getType()));
			}
		}
		return list;
	}
	
	
	
	public static URL getCaseURL(String name,URI u){
		try{
			if(UIHelper.isURL(name))
				return new URL(name);
			
			// strip the query part of the name
			int i = name.lastIndexOf("?");
			String query = "";
			if(i > -1){
				query = name.substring(i);
				name = name.substring(0,i); 
			}
			
			return new URL(DEFAULT_HOST_URL+getCasePath(u)+"/"+name+CASE_SUFFIX+query);
		}catch(MalformedURLException ex){
			//NOOP
		}
		return null;
	}
	
	public static String getCaseName(String name){
		if(TextHelper.isEmpty(name))
			return "";
		Pattern pt = Pattern.compile("[a-zA-Z]+://.*/(\\w+).case.*");
		Matcher mt = pt.matcher(name);
		if(mt.matches())
			return mt.group(1);
		return name;
	}
	
	/**
	 * get case name query component
	 * @param name
	 * @return
	 */
	public static Properties getURLQuery(String name){
		Properties p = new Properties();
		int i = name.lastIndexOf("?");
		if(i > -1){
			String query  = name.substring(i+1);
			String [] q = query.split("&");
			for(String str: q){
				String [] s = str.split("=");
				if(s.length == 2){
					p.setProperty(s[0].trim(),s[1].trim());
				}
			}
		}
		return p;
	}
	
	/**
	 * strip the query part if available
	 * @param name
	 * @return
	 */
	public static String stripURLQuery(String name){
		if(name != null){
			// strip the query part of the name
			int i = name.lastIndexOf("?");
			if(i > -1)
				name = name.substring(0,i);
		}
		return name;
	}
	
	/**
	 * get concept class for given concept entry
	 * @param entry
	 * @param ontology
	 * @return
	 */
	public static IClass getConceptClass(ConceptEntry e, IOntology ontology){
		if(e == null)
			return null;
		
		// find class for candidate
		String nm = e.getName();
		IClass candidate = ontology.getClass(nm);
		
		if(candidate == null){
			// is there extra infor tacked along?
			int i = -1;
			for(String s: Arrays.asList(",","=")){
				i = nm.indexOf(s);
				if(i > -1)
					break;
			}		
			//check for separator
			if( i > -1){
				// assume that first entry in a sequence is a valid class
				candidate= ontology.getClass(nm.substring(0,i).trim());
			}
		}
		return candidate;
	}
	
	
	/**
	 * get list of matching findings from case
	 * @param caseEntry
	 * @param concept
	 * @return
	 */
	public static List<ConceptEntry> getMatchingFindings(CaseEntry caseEntry, ConceptEntry concept){
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(ConceptEntry e: caseEntry.getConcepts()){
			// if concept is identical (or more specific then target), or same feature && more general then correct finding
			if(e.equals(concept) || OntologyHelper.hasSubClass(e,concept,caseEntry.getExpertModule().getDomainOntology()))
				return Collections.singletonList(e);
			else if(e.getFeature().equals(concept.getFeature()) && 
				OntologyHelper.hasSubClass(concept,e,caseEntry.getExpertModule().getDomainOntology())){
				list.add(e);
			}
		}
		return list;
	}
	
	/**
	 * is one concept entry subclass of another
	 */
	public static boolean isSubClassOf(ConceptEntry child, ConceptEntry parent, IOntology ontology){
		IClass c = getConceptClass(child, ontology);
		IClass p = getConceptClass(parent, ontology);
		if(c ==null || p == null)
			return false;
		return p.hasSubClass(c);
	}
	
	/**
	 * is finding irrelevant
	 * @param e
	 * @return
	 */
	public static boolean isIrrelevant(String e){
		for(String s: IRRELEVANT_ERRORS)
			if(s.equals(e))
				return true;
		return false;
	}
	
	/**
	 * is error state OK
	 * @param e
	 * @return
	 */
	public static boolean isOK(String e){
		return ERROR_OK.equals(e) || ERROR_DIAGNOSIS_IS_CORRECT.equals(e) || ERROR_REMOVED_CORRECT_CONCEPT.equals(e);
	}
	
	
	/**
	 * get concept that represents an attribute of a given value Ex: QUANTITY
	 * @param candidate
	 * @param ontology
	 * @return
	 */
	public static String getAttributeName(ConceptEntry candidate,List<ConceptEntry> attributes,IOntology ontology){
		IClass a = getConceptClass(candidate,ontology);
		// if candidate is not a direct modifier
		if(a != null && !a.hasDirectSuperClass(ontology.getClass(MODIFIERS))){
			for(ConceptEntry en: attributes){
				IClass ea = getConceptClass(en, ontology);
				if(ea != null){
					IClass direct = getDirectCommonParent(a,ea);
					if(direct != null){
						return UIHelper.getTextFromName(direct.getName());
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * get list of attributes that is present in child, but unavailable in parent
	 * @param parent
	 * @param child
	 * @return
	 */
	public static List<ConceptEntry> getMissingAttributes(ConceptEntry parent,ConceptEntry child){
		List<ConceptEntry> toadd = new ArrayList<ConceptEntry>();
		for(ConceptEntry a: child.getAttributes()){
			if(!parent.getAttributes().contains(a))
				toadd.add(a);
		}
		return toadd;
	}
	
	/**
	 * get comparator that will comare entries based ont he order 
	 * assigned in a given class
	 * @param cls
	 * @return
	 */
	public static Comparator getOrderComparator(IClass cls){
		// re-order the list
		final Map<String,Integer> map = new HashMap<String, Integer>();
		IOntology ont = cls.getOntology();
		for(Object o: cls.getPropertyValues(ont.getProperty(HAS_ORDER))){
			String [] s = (""+o).split("\\s*:\\s*");
			if(s.length == 2 && s[1].matches("\\d+")){
				map.put(s[0],new Integer(s[1]));
			}
		}
		return new Comparator() {
			public int compare(Object o1, Object o2) {
				if(o1 instanceof IResource && o2 instanceof IResource){
					IResource r1 = (IResource) o1;
					IResource r2 = (IResource) o2;
					int n1 = (map.containsKey(r1.getName()))?map.get(r1.getName()):0;
					int n2 = (map.containsKey(r2.getName()))?map.get(r2.getName()):0;
					return n1 - n2;
				}else if(o1 instanceof ConceptEntry && o2 instanceof ConceptEntry){
					ConceptEntry r1 = (ConceptEntry) o1;
					ConceptEntry r2 = (ConceptEntry) o2;
					int n1 = (map.containsKey(r1.getName()))?map.get(r1.getName()):0;
					int n2 = (map.containsKey(r2.getName()))?map.get(r2.getName()):0;
					return n1 - n2;
				}
				return 0;
			}
		};
	}
	
	
	
	/**
	 * get features that are part of the diagnostic rule
	 * @param dx
	 * @return
	 */
	public static Set<ConceptEntry> getDiagnosticFindings(ConceptEntry dx, IOntology ontology){
		Set<ConceptEntry> list = new LinkedHashSet<ConceptEntry>();
		IClass d = getConceptClass(dx, ontology);
		if(d != null)
			getDiagnosticFindings(d.getEquivalentRestrictions(),null,ontology, list);
		return list;
	}
	
	/**
	 * get features that are part of the diagnostic rule
	 * @param dx
	 * @return
	 */
	public static Set<ConceptEntry> getDiagnosticFindings(ILogicExpression exp, IOntology ontology){
		Set<ConceptEntry> list = new LinkedHashSet<ConceptEntry>();
		getDiagnosticFindings(exp,null,ontology, list);
		return list;
	}
	
	
	/**
	 * get features that are part of the diagnostic rule
	 * @param dx
	 * @return
	 */
	private static void getDiagnosticFindings(ILogicExpression exp,IProperty p,IOntology ontology,Set<ConceptEntry> list){
		for(Object o: exp){
			if(o instanceof ILogicExpression){
				getDiagnosticFindings((ILogicExpression)o,p,ontology,list);
			}else if(o instanceof IRestriction){
				IRestriction r = (IRestriction)o;
				getDiagnosticFindings(r.getParameter(),r.getProperty(), ontology, list);
			}else if(o instanceof IClass && p != null){
				IClass c = (IClass) o;
				if(HAS_FINDING.equals(p.getName()))
					list.add(new ConceptEntry(c.getName(),TYPE_FINDING));
				else if(HAS_NO_FINDING.equals(p.getName()))
					list.add(new ConceptEntry(c.getName(),TYPE_ABSENT_FINDING));			
			}
		}
	}
	
	
	/**
	 * fuzzy compare two double values
	 * the candidate has to be in 1sd withing goal's range
	 * @param goal
	 * @param candidate
	 * @return
	 */
	
	public static boolean compareValues(double goal, double candidate){
		// if both are integers, then check if they are withing at least 1-2 of eachother
		if(goal == Math.round(goal) && candidate == Math.round(candidate)){
			return Math.abs(goal - candidate) < 2;
		}
		return Math.abs(goal - candidate) < 0.2;
	}
	
	/**
	 * fuzzy compare two double values
	 * the candidate has to be in 1sd withing goal's range
	 * @param goal
	 * @param candidate
	 * @return
	 */
	
	public static boolean compareValues(ConceptEntry e1, ConceptEntry e2){
		// if both are integers, then check if they are withing at least 1-2 of eachother
		if(e1.hasNumericValue() && e2.hasNumericValue()){
			return compareValues(e1.getNumericValue(), e2.getNumericValue());
		}
		return true;
	}
	
	
	/**
	 * does text in label cover given concept
	 * @param label
	 * @param entry
	 * @return
	 */
	public static boolean isLabelCoversConcept(ConceptLabel label, ConceptEntry entry){
		// get "broad" class of this label
		IClass lc = label.getConcept().getConceptClass();
		if(lc == null)
			return false;
		IClass ec = getConceptClass(entry,lc.getOntology());
		if(ec == null)
			return false;
		
		// now lets see if text matches
		String text = label.getText();
		for(String syn : ec.getConcept().getSynonyms()){
			// check regexp match
			if(syn.matches("/.*/")){
				if(text.matches(syn.substring(1,syn.length()-1)))
					return true;
			}else if(syn.equalsIgnoreCase(text)){
				// if dead on match, great
				return true;
			}else if(TextTools.normalize(text,true,false).equals(TextTools.normalize(syn,true,false))){
				// if normalized stemmed strings matched
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * find a point inside some location for a feature
	 * @param problem
	 * @param finding
	 * @return
	 */
	public static Point findLocationMarker(CaseEntry problem, ConceptEntry finding){
		return findLocationMarker(problem,finding,null);
	}
	
	/**
	 * find a point inside some location for a feature
	 * @param problem
	 * @param finding
	 * @return
	 */
	public static Point findLocationMarker(CaseEntry problem, ConceptEntry finding, String image){
		//TODO: take care of current image
		// maybe look at the best example first?
		for(ShapeEntry se: problem.getExamples(finding,image)){
			if(se.getType().equalsIgnoreCase("Arrow"))
				return new Point(se.getXStart(),se.getYStart());
		}
		
		// go over locations
		for(ShapeEntry se: problem.getLocations(finding,image)){
			Shape s = se.getShape();
			Rectangle r = s.getBounds();
			Point p = new Point((int)r.getCenterX(),(int)r.getCenterY());
			
			// if point is not in shape
			if(!s.contains(p) && s instanceof Polygon){
				PolygonUtils pu = new PolygonUtils((Polygon)s);
				return pu.centroid();
			}
			return p;
		}
		
		return null;
	}
	

	/**
	 * get location of window
	 * @param comp
	 * @return
	 */
	public static Point getWindowLocation(JComponent comp, Point p) {
		Rectangle win = Config.getMainFrame().getBounds();
		Dimension compBounds = comp.getSize();
		Dimension screenSize = new Dimension(win.x+win.width,win.y+win.height);
		int delta = 20;
		int nextX = p.x + delta;
		int nextY = p.y + delta;
		if (nextX + compBounds.width > screenSize.width) { // if it is at the
			// right edge of the
			// screen...
			nextX = screenSize.width - compBounds.width - 1 - delta;
		}
		if (nextY + compBounds.height > screenSize.height - 10) { // if it is at
			// the bottom
			// edge of the
			// screen...
			nextY = nextY - compBounds.height - 1;
		}
		return new Point(nextX, nextY);
	}
	
	
	/**
	 * get ancesstors of a class
	 * @return
	 */
	public static List<IClass> getFindingAncesstors(IClass cls, List<IClass> list){
		if(isFeature(cls)){
			list.add(cls);
			if(!cls.equals(getFeature(cls))){
				List<IClass> dp = new ArrayList<IClass>();
				for(IClass p: cls.getDirectSuperClasses()){
					if(isFeature(p))
						dp.add(p);
				}
				if(dp.size() == 1)
					return getFindingAncesstors(dp.get(0),list);
				else if(dp.size() > 1){
					// best match is the one has definition
					for(IClass p: dp){
						if(p.getComments().length > 0)
							return getFindingAncesstors(p,list);
					}
					// else just return the first one
					return getFindingAncesstors(dp.get(0),list);
				}
			}
		}
		return list;
	}
}
