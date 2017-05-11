package edu.pitt.dbmi.tutor.modules.expert;



import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.ui.DomainSelectorPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.ontology.protege.PReasoner;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.client.OntologyTerminology;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import static edu.pitt.dbmi.tutor.messages.Constants.*;


/**
 * this module access to ontology repository 
 * @author tseytlin
 */
public class DomainExpertModule implements ExpertModule, IRepository {
	private Properties defaultConfig;
	private IOntology ontology;
	private DomainSelectorPanel domainSelector;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private URL server;
	private File curriculumDir;
	private Map<String,IOntology> ontologies;
	private String [] allCases;
	private boolean setup,pruneEmptyCategories,fetchCaseMetaData,gotCaseMetaData;
	private ConceptFilter filter;
	private int caseLimit;
	
	public void load(){
		setup();
	}
	
	private void setup(){
		// set custom plugin folder
		try{
			System.setProperty("protege.dir",Config.getWorkingDirectory().getAbsolutePath());
		}catch(Exception ex){
			// do nothing on security exception, not such a bid deal anyway
		}
		
		String loc = Config.getProperties().getProperty("curriculum.path",Config.getProperty(this,"curriculum.path"));
		curriculumDir = new File(loc);
		if(!curriculumDir.exists()){
			curriculumDir = null;
			try{
				server = new URL(Config.getProperty("file.manager.server.url"));
			}catch(MalformedURLException ex){
				//Config.getLogger().severe("Could not parse 'file.manager.server.url' property "+Config.getProperty("file.manager.server.url"));
				try {
					server = new URL(OntologyHelper.DEFAULT_FILE_MANAGER_SERVLET);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		setupGlobalRepository(curriculumDir);
		
		pruneEmptyCategories = Config.getBooleanProperty(this,"prune.empty.categories");
		caseLimit = Config.getIntegerProperty(this,"domain.case.limit");
		
		setup = true;
		
		// fetch metadata right away since we are doing caching
		fetchCaseMetaData = true;
		gotCaseMetaData = true;
	}
	
	
	
	/**
	 * save global repository file
	 */
	private void setupGlobalRepository(File curriculum){
		File directory = new File(Config.getWorkingDirectory()+
						File.separator+"plugins"+File.separator+
						"edu.stanford.smi.protegex.owl");
		// create if necessary
		if(!directory.exists())
			directory.mkdirs();
		
		// setup global repository
		File repository = new File(curriculum,KNOWLEDGE_FOLDER);
		try{
			File f = new File(directory,"global.repository");
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.write(repository.toURI()+"?forceReadOnly=true&Recursive=true\n");
			//writer.write(repository.toURI()+"?forceReadOnly=true\n");
			writer.close();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		
		// put a dependency .owl file
		File protege_dc = new File(directory,"protege-dc.owl");
		if(!protege_dc.exists()){
			try{
				InputStream is = getClass().getResourceAsStream("/resources/protege-dc.owl");
				if(is != null){
					BufferedWriter writer = new BufferedWriter(new FileWriter(protege_dc));
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					for(String line = reader.readLine();line != null; line=reader.readLine()){
						writer.write(line+"\n");
					}
					reader.close();
					writer.close();
					is.close();
				}
			}catch(IOException ex){
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * close domain
	 */
	public void closeDomain() {
		if(ontology != null){
			ontology.dispose();
			ontology = null;
		}
	}

	/**
	 * wait once cases with meta data become available
	 */
	public void waitForCaseMetaData(){
		getAvailableCases();
		while(!gotCaseMetaData)
			UIHelper.sleep(10);
	}
	
	/**
	 * get input stream from file or uri
	 * @param name
	 */
	private InputStream getInputStream(String name) throws Exception{
		// strip the query part of the name
		name = OntologyHelper.stripURLQuery(name);
		
		// is it a file
		File f = new File(name);
		if(f.exists() && f.canRead())
			return new FileInputStream(f);
	
		// curriculum definition takes presendence
		if(ontology != null && curriculumDir != null){
			String u = curriculumDir.getParentFile().getAbsolutePath()+
					getCasePath(ontology)+"/"+TextHelper.getName(name)+CASE_SUFFIX;
			u = u.replace('/',File.separatorChar);
			f = new File(u);
			if(f.exists() && f.canRead())
				return new FileInputStream(f);
		}
		
		// is it a url
		if(name.startsWith("http://") || name.startsWith("file://")){
			try{
				return (new URL(name)).openStream();
			}catch(Exception ex){
				// try to handle fucked up URLs
				// by getting a name
				name = name.substring(name.lastIndexOf('/'),name.lastIndexOf('.')); 
			}
		}
		// if it is just a name, build own URL
		if(ontology != null){
			if(curriculumDir != null){
				String u = curriculumDir.getParentFile().getAbsolutePath()+
						getCasePath(ontology)+"/"+name+CASE_SUFFIX;
				u = u.replace('/',File.separatorChar);
				f = new File(u);
				if(f.exists() && f.canRead())
					return new FileInputStream(f);
			}else{
				String u = DEFAULT_HOST_URL+getCasePath(ontology)+"/"+name+CASE_SUFFIX;
				return (new URL(u)).openStream();
			}
		}
		return null;
	}
	
	/**
	 * get case entry
	 */
	public CaseEntry getCaseEntry(String name) {
		// figure out case url if requred
		CaseEntry caseEntry = new CaseEntry();
		caseEntry.setExpertModule(this);
		try{
			caseEntry.load(getInputStream(name));
		}catch(Exception ex){
			Config.getLogger().severe("cannot load case "+name+" cause: "+TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
		}
		
		// load query properties
		caseEntry.getProperties().putAll(OntologyHelper.getURLQuery(name));
		
		// reset concept id count for everyone, so that the new case will start w/ 1
		ConceptEntry.resetConceptIdCount();
		
		
		// load various concept properties from ontology
		// load concepts into case
		if(ontology != null){
			//long time = System.currentTimeMillis();
			Set<String> parts = new LinkedHashSet<String>();
			//IProperty prop = ontology.getProperty(HAS_POWER);
			//IClass ALOC = ontology.getClass(ANATOMIC_LOCATION);
			//IClass NUM  = ontology.getClass(NUMERIC);
			
			for(String category: getConceptCategories()){
	    		for(ConceptEntry entry: new ArrayList<ConceptEntry>(caseEntry.getConcepts(category).values())){
	       			// resolve entry
	    			resolveConceptEntry(entry);
	    			// gather information on parts
	    			parts.addAll(entry.getParts());
	    		}
	    	}
			// insert the default part
			if(parts.isEmpty())
				parts.add("Part-1");
			
			// mark imprtant and unimportant features as well as appropriate parts
			IInstance caseInstance = caseEntry.createInstance(ontology);
			List<ConceptEntry> diagnoses = caseEntry.getConcepts(DIAGNOSES).getValues();
			for(ConceptEntry entry: caseEntry.getConcepts(DIAGNOSTIC_FEATURES).values()){
				// set importance flag
				entry.setImportant(isImportant(entry,diagnoses,caseInstance));
				
				// set default parts, if no parts are set
				if(entry.getParts().isEmpty())
					entry.getParts().addAll(parts);
			}
			
			// set important flag for prognostic features
			for(String type: Arrays.asList(DIAGNOSES,PROGNOSTIC_FEATURES)){
				for(ConceptEntry entry: caseEntry.getConcepts(type).values()){
					// set importance flag
					entry.setImportant(true);
					
					// reset the absence flag
					if(TYPE_ABSENT_FINDING.equals(entry.getType())){
						entry.setType(TYPE_FINDING);
						entry.setAbsent(true);
					}
				}
			}
			
			// set default parts for all concepts
			for(ConceptEntry entry: caseEntry.getConcepts()){
				// set default parts, if no parts are set
				if(entry.getParts().isEmpty())
					entry.getParts().addAll(parts);
			}
			
			
			//set the case
			caseEntry.setParts(parts);
			
			// reorganize prognostic findings
			List<ConceptEntry> findingList = new ArrayList<ConceptEntry>();
			findingList.addAll(caseEntry.getConcepts(DIAGNOSTIC_FEATURES).getValues());
			
			// sort by power, importance, absence etc
			Collections.sort(findingList, new Comparator<ConceptEntry>(){
				public int compare(ConceptEntry a, ConceptEntry b) {
					// check importance
					if(a.isImportant() && !b.isImportant())
						return -1;
					if(!a.isImportant() && b.isImportant())
						return 1;
					// check absence
					if(!a.isAbsent() && b.isAbsent())
						return -1;
					if(a.isAbsent() && !b.isAbsent())
						return 1;
					
					// now that whatever they are the are equal check power
					if(a.getPower() != null && b.getPower() != null && a.getPower().equals(b.getPower()))
						return 0;
					
					// if a is smaller power it is first
					if(TextHelper.isSmallerPower(a.getPower(),b.getPower()))
						return -1;
					else 
						return 1;
				}
				
			});
			
			// now re-add back the sorted
			caseEntry.getConcepts(DIAGNOSTIC_FEATURES).clear();
			for(ConceptEntry e: findingList)
				caseEntry.getConcepts(DIAGNOSTIC_FEATURES).put(e.getName(),e);

			
			
			// lets remove duplicates and re-order prognostic features based on a template
			IClass template = getMatchingTemplate(caseEntry.getConcepts());
			if(template != null){
				// get a ordered set of prognostic template
				Set<ConceptEntry> prognostic = new TreeSet<ConceptEntry>(getOrderComparator(template));
				getPrognostic(template.getNecessaryRestrictions(), null,prognostic);
				
				// now go over template and build a map of that order
				List<ConceptEntry> sourceEntries = new ArrayList<ConceptEntry>();
				sourceEntries.addAll(caseEntry.getConcepts(PROGNOSTIC_FEATURES).getValues());
				sourceEntries.addAll(caseEntry.getConcepts(DIAGNOSES).getValues());
				
				List<ConceptEntry> foundEntries = new ArrayList<ConceptEntry>();
				List<ConceptEntry> reportEntries = new ArrayList<ConceptEntry>();
				OrderedMap<String,ConceptEntry> targetEntries = caseEntry.getConcepts(PROGNOSTIC_FEATURES);
				
				// clear the target
				targetEntries.clear();
				
				
				// iterate over a template
				for(ConceptEntry templateEntry: prognostic){
					// find all matching findings
					ConceptEntry targetEntry = null;
					for(ConceptEntry entry: sourceEntries){
						if(templateEntry.getName().equals(entry.getName()) || hasSubClass(templateEntry,entry,ontology)){
							foundEntries.add(entry);
							// if no candidate OR new candidate is more specific
							if(targetEntry == null || hasSubClass(targetEntry,entry,ontology))
								targetEntry = entry;
						}
					}
					
					// now add candidate back to case
					if(targetEntry != null){
						// add finding to a target entries
						if(targetEntry.isFinding())
							targetEntries.put(targetEntry.getName(),targetEntry);
						reportEntries.add(targetEntry);
							
						// setup template entry
						targetEntry.setTemplateEntry(templateEntry);
						
						// setup action entry
						IClass cls = ontology.getClass(templateEntry.getName());
						if(cls != null){
							for(IRestriction r: cls.getRestrictions(ontology.getProperty(HAS_ACTION))){
								targetEntry.addAction(new ActionEntry(((IClass)r.getParameter().getOperand()).getName()));
							}
						}
					}
				}
				
				// what happens if we have some findings in case that are not part of template?
				// add them to the end
				if(sourceEntries.size() != foundEntries.size()){
					for(ConceptEntry entry: sourceEntries){
						if(!foundEntries.contains(entry) && entry.isFinding()){
							targetEntries.put(entry.getName(),entry);
						}
					}
				}	
				
				// set reportable item list
				caseEntry.setReportFindings(reportEntries);
				
				// go over reportable entries and set several flags
				for(ConceptEntry e: reportEntries){
					if(TYPE_DIAGNOSIS.equals(e.getType()))
						break;
					e.setHeaderFinding(true);
				}
				
			}
			//System.out.println(System.currentTimeMillis()-time);
		}
		return caseEntry;
	}

	/**
	 * extract concept entries from expression and put them into a list
	 * 
	 * @param exp
	 * @return
	 */
	private Set<ConceptEntry> getPrognostic(ILogicExpression exp, IProperty prop, Set<ConceptEntry> list) {
		for (Object obj : exp) {
			if (obj instanceof IRestriction) {
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				getPrognostic(r.getParameter(), p, list);
			} else if (obj instanceof IClass && prop != null) {
				// convert class to a concept entry
				IClass c = (IClass) obj;
				ConceptEntry entry = createConceptEntry(c,TYPE_FINDING);
				if (prop.getName().contains(OntologyHelper.HAS_PROGNOSTIC)) {
					//&& c.hasSuperClass(ontology.getClass(OntologyHelper.PROGNOSTIC_FEATURES))
					list.add(entry);
				}
			} else if (obj instanceof ILogicExpression) {
				// recurse into expression
				getPrognostic((ILogicExpression) obj, prop, list);
			}
		}
		return list;
	}
	
	/**
	 * get appropriate expression for a given diagnosis
	 * 
	 * @param diagnosis
	 * @return
	 */
	public IClass getMatchingTemplate(List<ConceptEntry> e) {
		
		// get all classes in case
		// = cas.getConcepts();
		IClass [] clses = new IClass [e.size()];
		for(int i=0;i<clses.length;i++)
			clses[i] = getConceptClass(e.get(i),ontology);
		
		
		// remember last template, to select the closest match
		IClass template  = null; 
		LogicExpression lastExp = null;
		
		// iterate over available schemas
		for (IClass schema : ontology.getClass(SCHEMAS).getDirectSubClasses()) {
			
			// create an expression from a template (all of triggers should be ORed together
			LogicExpression exp = new LogicExpression(ILogicExpression.AND);
			for(IRestriction r : schema.getRestrictions(ontology.getProperty(HAS_TRIGGER))){
				exp.add(r.getParameter());
			}
			
			// if expression is satisfied, AND it has more terms then previous template, then select it
			if (evaluate(exp,clses) >= exp.size() && (lastExp == null || exp.size() > lastExp.size())){
				template = schema;
				lastExp = exp;
			}
		}
		
		// else return empty expression
		return template;
	}
	
	
	/**
	 * get domain template that matches a given set of concepts
	 * @param concepts
	 * @return
	 */
	public Set<ConceptEntry> getReportTemplate(List<ConceptEntry> concepts){
		// lets remove duplicates and re-order prognostic features based on a template
		IClass template = getMatchingTemplate(concepts);
		if(template != null){
			// get a ordered set of prognostic template
			Set<ConceptEntry> prognostic = new TreeSet<ConceptEntry>(getOrderComparator(template));
			getPrognostic(template.getNecessaryRestrictions(), null,prognostic);
			// set flags in the template
			for(ConceptEntry e: prognostic){
				// set worksheet property
    			if(isWorksheet(getConceptClass(e, ontology)))
    				e.setWorksheetFinding(true);
    			// set header property
    			if(isHeader(getConceptClass(e, ontology)))
    				e.setHeaderFinding(true);
			}
			return prognostic;
		}
		return Collections.EMPTY_SET;
	}
	/**
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 */
	private int evaluate(Object exp, Object param) {
		int hits = 0;
		if (exp instanceof ILogicExpression) {
			ILogicExpression e = (ILogicExpression) exp;
			// check for not
			if(ILogicExpression.NOT == e.getExpressionType()){
				// invert the result
				//TODO: what is negation in this context?
				if(evaluate(e.getOperand(),param) <= 0){
					hits ++;
				}
			}else{
				// iterate over parameters
				for (Object obj : e) {
					hits += evaluate(obj, param);
				}
			}
			return hits;
		} else if (exp instanceof IRestriction && param instanceof IInstance) {
			IRestriction r = (IRestriction) exp;
			IInstance inst = (IInstance) param;
			Object[] values = inst.getPropertyValues(r.getProperty());
			if (values == null || values.length == 0)
				return 0;
			// if any of values fits, that we are good
			ILogicExpression value = r.getParameter();
			for (int i = 0; i < values.length; i++) {
				if (value.evaluate(values[i]))
					hits++;
			}
			return hits;
		} else if (exp instanceof IClass) {
			if(param instanceof IClass []){
				for(IClass c: (IClass []) param){
					if(((IClass) exp).evaluate(c))
						return 1;
					
				}
				return 0;
			}else
				return (((IClass) exp).evaluate(param)) ? 1 : 0;
		} else {
			return (exp.equals(param)) ? 1 : 0;
		}
	}
	
	
	/**
	 * is finding important for coming up with diagnosis
	 * @param finding
	 * @param diagnoses
	 * @return
	 */
	private boolean isImportant(ConceptEntry finding, List<ConceptEntry> diagnoses, IInstance inst){
		if(ontology != null){
			IClass fn = ontology.getClass(finding.getName());
			IProperty p = ontology.getProperty(finding.isAbsent()?HAS_NO_FINDING:HAS_FINDING);
			int count = 0;
			for(ConceptEntry d: diagnoses){
				IClass dx = ontology.getClass(d.getName());
				ILogicExpression exp = dx.getEquivalentRestrictions();
				
				// pick a pattern (if multipattern Dx)
				if(exp.getExpressionType() == ILogicExpression.OR){
					exp = OntologyHelper.getMatchingPatterns(dx, inst);
				}
				
				if(OntologyHelper.isFindingInDiagnosticRule(fn, p, exp)){
					count ++;
					// if we only have to have one hit, then break
					break;
				}
			}
			return (!diagnoses.isEmpty())?count > 0:true;
			//return (!diagnoses.isEmpty())?((double)count)/diagnoses.size() >= .5:true;
		}
		return false;
	}
	
	
	/**
	 * load instance values
	 * @param inst
	 */
	private CaseEntry loadCaseInstance(IInstance inst){
		IOntology ont  = inst.getOntology();
		
		// start caseEntry
		CaseEntry caseEntry = new CaseEntry();
		caseEntry.setName(inst.getName());
		
		// load report
		String text = (String) inst.getPropertyValue(ont.getProperty(HAS_REPORT));
		caseEntry.setReport((text != null)?text:"");
		
		// load slides 
		for(Object s: inst.getPropertyValues(ont.getProperty(HAS_SLIDE))){
			SlideEntry slide = new SlideEntry(""+s);
			caseEntry.addSlide(slide);
			
			// set first slide as primary
			if(caseEntry.getPrimarySlide() == null)
				caseEntry.setPrimarySlide(slide);
		}
		
		// put diagnostic entries
		for(String p: new String [] {
				HAS_FINDING,
				HAS_NO_FINDING}){
			for(Object o: inst.getPropertyValues(ont.getProperty(p))){
				if(o instanceof IInstance){
					ConceptEntry c = loadConceptInstance((IInstance) o);
					caseEntry.addConcept(DIAGNOSTIC_FEATURES,c);
				}
			}
		}
		// put prognostic & other entries
		String [][] types = new String [][] {{HAS_PROGNOSTIC,PROGNOSTIC_FEATURES},
											 {HAS_CLINICAL,  CLINICAL_FEATURES},
											 {HAS_ANCILLARY,ANCILLARY_STUDIES}};
		for(int i=0;i<types.length;i++){
			for(Object o: inst.getPropertyValues(
					ont.getProperty(types[i][0]))){
				if(o instanceof IInstance){
					ConceptEntry c = loadConceptInstance((IInstance) o);
					caseEntry.addConcept(types[i][1],c);
				}
			}
		}
		
		// put diagnoses
		for(Object o: inst.getDirectTypes()){
			if(o instanceof IClass){
				IClass c = (IClass) o;
				if(c.hasSuperClass(ont.getClass(OntologyHelper.DIAGNOSES))){
					caseEntry.addConcept(OntologyHelper.DIAGNOSES,createConceptEntry(c,TYPE_DIAGNOSIS));
				}
			}
		}
		return caseEntry;
	}
	
	/**
	 * load concept instance
	 * @param inst
	 * @return
	 */
	private ConceptEntry loadConceptInstance(IInstance inst){
		Boolean b = (Boolean) getPropertyValue(inst,IS_ABSENT);
		boolean absent = (b != null)?b.booleanValue():false;
		Float d = (Float) getPropertyValue(inst,HAS_NUMERIC_VALUE);
		double numericValue = (d != null)?d.floatValue():Double.MIN_VALUE;
		String type = (absent)?Constants.TYPE_ABSENT_FINDING:Constants.TYPE_FINDING;
		
		IClass [] types = inst.getDirectTypes();
		ConceptEntry concept = createConceptEntry(types[0],type);
		concept.setNumericValue(numericValue);
		return concept;
	}
	
	/**
	 * shortcut to get property value
	 * @param r
	 * @param prop
	 * @return
	 */
	public static Object getPropertyValue(IResource r, String prop){
		IProperty p = r.getOntology().getProperty(prop);
		return (p != null)?r.getPropertyValue(p):null;
	}
	
	/**
	 * get domain that is currently open, empty string
	 * if no domain is open
	 */
	public String getDomain() {
		return (ontology != null)?""+ontology.getURI():"";
	}
	
	
	/**
	 * get tree root
	 */
	public TreeNode getTreeRoot(String root) {
		return getTreeRoot(ontology.getClass(root),null);
	}
	
	/**
	 * get tree root
	 */
	public TreeNode getTreeRoot(String root, ConceptFilter filter) {
		return getTreeRoot(ontology.getClass(root),filter);
	}

	/**
	 * get tree root
	 */
	private TreeNode getTreeRoot(IClass cls,ConceptFilter filter) {
		if(cls != null){
			//a might be slow, be somehow show equivalent classes
			List<IClass> children =  new ArrayList<IClass>(Arrays.asList(cls.getDirectSubClasses()));
			List<IClass> parents = Arrays.asList(cls.getSuperClasses());
			
			// avoid having infinite loops
			children.removeAll(parents);
			
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(cls.getName());
			for(IClass child : children){
				// accept or reject concept
				boolean accept = (filter == null || filter.accept(cls.getName(),child.getName())) &&  getConceptFilter().accept(cls.getName(),child.getName());
				
				// if not itself, or if passed filter (if available)
				if(!child.equals(cls) && accept)
					// if we are in pruning mode, then don't add empty system classes
					if(!(pruneEmptyCategories && isSystemClass(child) && child.getDirectSubClasses().length == 0))
						node.add((MutableTreeNode)getTreeRoot(child,filter));
			}
			return node;
		}
		return null;
	}
	
	/**
	 * get a paths to root for a given concept
	 * @param name  - name of the class in question
	 * @return
	 */
	public List<TreePath> getTreePaths(String name){
		IClass cls = ontology.getClass(name);
		if(cls != null){
			
			// get paths to root
			List<List<String>> paths = new ArrayList<List<String>>();
			getPath(cls,new ArrayList<String>(), paths);
			
			// convert to TreePath form
			List<TreePath> tree = new ArrayList<TreePath>();
			for(List<String> path: paths){
				tree.add(new TreePath(path.toArray(new String [0])));
			}
			return tree;
		}
		return Collections.EMPTY_LIST;
	}
	
	/**
	 * get multiple paths to root
	 * @param cls
	 * @param path
	 * @param paths
	 */
	private void getPath(IClass cls,List<String> path, List<List<String>> paths){
		// add to paths if path is not in paths
		if(!paths.contains(path)){
			paths.add(path);
		}
		
		// add to current path
		path.add(0,cls.getName());
		// iterate over parents
		IClass [] parents = cls.getDirectSuperClasses();
		// if only one parent, then add it to path
		if(parents.length == 1){
			getPath(parents[0],path,paths);
		}else if(parents.length > 1){
			// else clone current path and start new ones
			for(int i=1;i<parents.length;i++){
				getPath(parents[i],new ArrayList<String>(path),paths);
			}
			getPath(parents[0],path,paths);
		}
	}
	/*
	public static void main(String [] args) throws Exception {
		IOntology ont = POntology.loadOntology("http://slidetutor.upmc.edu/curriculum/owl/skin/UPMC/Subepidermal.owl");
		ont.load();
		DomainExpertModule m = new DomainExpertModule();
		List<List<String>> paths = new ArrayList<List<String>>();
		m.getPath(ont.getClass("Subepidermal_Blister"),new ArrayList<String>(), paths);
		System.out.println(paths);
	}
	*/
	
	/**
	 * open domain
	 */
	public void openDomain(String name) {
		//if(!setup)
		//	setup();
		ontology = getOntology(URI.create(name));
		try{
			ontology.load();
		}catch(IOntologyException ex){
			ex.printStackTrace();
		}
		
		// reset feature map
		OntologyHelper.reset();
	}

	/**
	 * dispose of domain
	 */
	public void dispose() {
		closeDomain();
	}

	/**
	 * get component
	 */
	public Component getComponent() {
		if(domainSelector == null)
			domainSelector = new DomainSelectorPanel(this,false);
		return domainSelector;
	}
	
	/**
	 * get default config
	 */
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "The Expert Module provides access to case and ontology data from a centralized curriculum server.";
	}

	public String getName() {
		return "Domain Expert Module";
	}

	public ImageIcon getScreenshot() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getVersion() {
		return "1.0";
	}

	public void receiveMessage(Message msg) {
		// TODO Auto-generated method stub

	}

	public void reset() {
		//TODO:

	}
	

	/**
	 * domain ontology
	 */
	public IOntology getDomainOntology() {
		return ontology;
	}

	/**
	 * resolve an arbitrary action
	 * if action is understood, the module will
	 * "resolve" it, by assigning runnable code
	 * to it, for later execution
	 * @param action
	 */
	public void resolveAction(Action action){
		//TODO:
	}

	public void addOntology(IOntology ontology) {
		throw new IOntologyError("operation not allowed");
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void addTerminology(Terminology terminology) {
		throw new IOntologyError("operation not allowed");
		
	}

	public IOntology createOntology(URI path) throws IOntologyException {
		throw new IOntologyError("operation not allowed");
	}

	public void exportOntology(IOntology ontology, int format, OutputStream out) throws IOntologyException {
		ontology.write(out,format);
	}

	/**
	 * get a list of ontologies
	 */
	public IOntology[] getOntologies() {
		if(ontologies == null){
			ontologies = new HashMap<String, IOntology>();
			try{
				for(String u : list(KNOWLEDGE_FOLDER)){
					if(u.endsWith(OWL_SUFFIX)){
						IOntology ont = POntology.loadOntology(""+TextHelper.toURI(DEFAULT_BASE_URI+u));
						//ontologies.put(ont.getName(),ont);
						// look for ontology in local directory
						if(curriculumDir != null){
							String path = curriculumDir.getAbsolutePath()+"/"+KNOWLEDGE_FOLDER+"/"+u;
							path = path.replace('/',File.separatorChar);
							((POntology)ont).getResourceProperties().setProperty("location",path);
						}
						ontologies.put(""+ont.getURI(),ont);
					}
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		return (new TreeSet<IOntology>(ontologies.values())).toArray(new IOntology [0]);
	}

	/**
	 * get ontologies that are loaded in repository
	 * @return
	 */
	public IOntology [] getOntologies(String name){
		ArrayList<IOntology> onts = new ArrayList<IOntology>();
		for(IOntology o : getOntologies()){
			if(o.getURI().toString().contains(name)){
				onts.add(o);
			}
		}
		return onts.toArray(new IOntology [0]);
	}
	
	
	
	public IOntology getOntology(URI name) {
		if(ontologies == null){
			getOntologies();
		}
		IOntology ont = ontologies.get(""+name);
		// if ontology is not in a list, try to load it
		// directly
		if(ont == null){
			try {
				ont = POntology.loadOntology(name);
			} catch (IOntologyException e) {
				e.printStackTrace();
			}
		}
		return ont;
	}

	/**
	 * get reasoner that can handle this ontology
	 * you can configure the type of reasoner by 
	 * specifying reasoner class and optional URL
	 * in System.getProperties()
	 * reasoner.class and reasoner.url
	 * @return null if no reasoner is available
	 */
	public IReasoner getReasoner(IOntology ont){
		if(ont instanceof POntology){
			return new PReasoner((POntology)ont);
		}
		return null;
	}

	/**
	 * convinience method
	 * get resource from one of the loaded ontologies
	 * @param path - input uri
	 * @return resource or null if resource was not found
	 */
	public IResource getResource(URI path){
		String uri = ""+path;
		int i = uri.lastIndexOf("#");
		uri = (i > -1)?uri.substring(0,i):uri;
		// get ontology
		IOntology ont = getOntology(URI.create(uri));
		// if ontology is all you want, fine Girish
		if(i == -1)
			return ont;
		// 
		if(ont != null){
			return ont.getResource(""+path);
		}
		return null;
	}

	public Terminology[] getTerminologies() {
		return new Terminology[0];
	}

	public Terminology getTerminology(String path) {
		IOntology ont = getOntology(URI.create(path));
		return (ont != null)?new OntologyTerminology(ont):null;
	}

	public boolean hasOntology(String name) {
		if(ontologies == null){
			getOntologies();
		}
		return ontologies.containsKey(name) || getOntologies(name).length > 0;
	}

	public IOntology importOntology(URI path) throws IOntologyException {
		throw new IOntologyError("operation not allowed");
	}

	public void importOntology(IOntology ont) throws IOntologyException {
		throw new IOntologyError("operation not allowed");
	}

	public void removeOntology(IOntology ontology) {
		throw new IOntologyError("operation not allowed");
		
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
		
	}

	public void removeTerminology(Terminology terminology) {
		throw new IOntologyError("operation not allowed");
	}
	

	public String[] getAvailableCases(String domain) {
		String p = getCasePath(getOntology(URI.create(domain)));
		List<String> list = new ArrayList<String>();
		for(String s : getAvailableCases()){
			if(s.contains(p)){
				list.add(s);
			}
			// limit number of available cases if set
			if(caseLimit > 0 && list.size() >= caseLimit)
				break;
		}
		return list.toArray(new String [0]);
	}

	/**
	 * list content of 
	 * @param path
	 * @return
	 */
	private String [] list(String path){
		if(curriculumDir != null && curriculumDir.exists()){
			File f = new File(curriculumDir,path);
			return listRecursive(f.getAbsolutePath(),"").toArray(new String [0]);
		}else if(server != null){
			Map<String,String> map = new HashMap<String, String>();
			map.put("action","list");
			map.put("root",CURRICULUM_ROOT);
			map.put("path",path);
			map.put("recurse","true");
			try{
				String str = UIHelper.doGet(server,map);
				return str.split("\n");
			}catch(IOException ex){
				ex.printStackTrace();
			}
		}
		return new String [0];
	}
	
	/**
	 * list content of 
	 * @param path
	 * @return
	 */
	private String [] listCases(String path){
		if(curriculumDir != null && curriculumDir.exists()){
			File f = new File(curriculumDir,path);
			return listRecursive(f.getAbsolutePath(),"").toArray(new String [0]);
		}else if(server != null){
			Map<String,String> map = new HashMap<String, String>();
			map.put("action","list-cases");
			map.put("root",CURRICULUM_ROOT);
			map.put("path",path);
			if(fetchCaseMetaData)
				map.put("properties","status,question.type");
			try{
				String str = UIHelper.doGet(server,map);
				return str.split("\n");
			}catch(IOException ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				// make another attempt
				try{
					String str = UIHelper.doGet(server,map);
					return str.split("\n");
				}catch(IOException ex2){
					Config.getLogger().severe(TextHelper.getErrorMessage(ex2));
				}
			}
		}
		return new String [0];
	}
	
	
	/**
	 * list content of director
	 * @param filename
	 * @return
	 */
	private List<String> listRecursive(String filename, String prefix){
		File file = new File(filename);
		if(file.isDirectory()){
			List<String> buffer = new ArrayList<String>();
			for(File f: file.listFiles()){
				if(!f.isHidden() && !f.getName().startsWith(".")){
					if(f.isDirectory()){
						buffer.addAll(listRecursive(f.getAbsolutePath(),prefix+f.getName()+"/"));
					}else
						buffer.add(prefix+f.getName());
				}
			}
			return buffer;
		}
		return Collections.EMPTY_LIST;
	}
	
	
	/**
	 * get available cases
	 */
	public String[] getAvailableCases() {
		if(allCases == null){
			List<String> list = new ArrayList<String>();
			for(String u : listCases(CASES_FOLDER)){
				if(stripURLQuery(u).endsWith(CASE_SUFFIX)){
					list.add(DEFAULT_HOST_URL+"/"+CURRICULUM_ROOT+"/"+CASES_FOLDER+"/"+u);
				}
			}
			Collections.sort(list);
		
			// check filters
			String [] includeFilters = Config.getListProperty(this,"case.filter.include");
			String [] excludeFilters = Config.getListProperty(this,"case.filter.exclude");
			
			
			// get domains
			List<String> dlist = new ArrayList<String>();
		
			
			// check include filters
			for(String s : list){
				// if no filter is set, OR it matches available filters
				if(includeFilters == null || includeFilters.length == 0 || 
				    matchesFilter(s,includeFilters)){
					dlist.add(s);
				}
			}
			
			
			// remove domains excluded by filters
			if(excludeFilters != null && excludeFilters.length > 0){
				for(String s: new ArrayList<String>(dlist)){
					if(matchesFilter(s,excludeFilters)){
						dlist.remove(s);
					}
				}
			}
		
			// convert to array
			allCases = dlist.toArray(new String [0]);
		
			// now that we are happy that we fetched cases
			if(!fetchCaseMetaData){
				(new Thread(new Runnable(){
					public void run() {
						UIHelper.sleep(10);
						
						// forget about cases
						allCases = null;
						fetchCaseMetaData = true;
						
						// get available cases
						getAvailableCases();
						
						// notify that we got new case metadata
						gotCaseMetaData = true;
					}
				})).start();
			}
		}
		return allCases;
	}

	
	
	
	public String[] getAvailableDomains() {
		IOntology [] o = getOntologies();
		
		
		// check filters
		String [] includeFilters = Config.getListProperty(this,"domain.filter.include");
		String [] excludeFilters = Config.getListProperty(this,"domain.filter.exclude");
		
		
		// get domains
		List<String> dlist = new ArrayList<String>();
	
		
		// check include filters
		for(IOntology s : o){
			// if no filter is set, OR it matches available filters
			if(includeFilters == null || includeFilters.length == 0 || 
			    matchesFilter(""+s.getURI(),includeFilters)){
				dlist.add(""+s.getURI());
			}
		}
		
		
		// remove domains excluded by filters
		if(excludeFilters != null && excludeFilters.length > 0){
			for(String s: new ArrayList<String>(dlist)){
				if(matchesFilter(s,excludeFilters)){
					dlist.remove(s);
				}
			}
		}
		
		
		//String [] u = new String [o.length];
		//for(int i=0;i<u.length;i++)
		//	u[i] = ""+o[i].getURI();
				
		//return u;
		return (String []) dlist.toArray(new String [0]);
	}
	
	private boolean matchesFilter(String s, String [] filters){
		for(String f: filters){
			if(s.matches(".*"+f.trim()+".*")){
				return true;
			}
		}
		return false;
	}
	
	

	public Terminology getDomainTerminology() {
		return getTerminology(getDomain());
	}
	
	/**
	 * get all messages that this module supports
	 * @return
	 */
	public Message [] getSupportedMessages(){
		return new Message [0];
	}
	
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions(){
		return new Action [0];
	}
	
	
	/**
	 * resolve meta-info for a given concept entry
	 * setup Concept object, examples etc ...
	 * @param entry
	 */
	public void resolveConceptEntry(ConceptEntry entry){
		//System.out.println("Resolve "+entry+" "+entry.isResolved());
		// don't resolve the same thing twice
		if(!entry.isResolved() && ontology != null){
			IClass cls = OntologyHelper.getConceptClass(entry,ontology);
			
			if(cls != null){
				// feature / attributes only make sence in the context of finding
				// for diseases and attributes they make no sense
				if(isFeature(cls)){
					// assign feature to finding
					IClass f = OntologyHelper.getFeature(cls);
					if(!entry.getName().equals(f.getName())){
						entry.setFeature(createConceptEntry(f,entry.getType()));
						entry.getFeature().setConcept(f.getConcept());
					}else{
						entry.setFeature(entry);
					}
					
					
					// add attributes
					List<ConceptEntry> attributes = new ArrayList<ConceptEntry>();
					for(IClass c: OntologyHelper.getAttributes(cls)){
						ConceptEntry attr = createConceptEntry(c,Constants.TYPE_ATTRIBUTE);
						if(OntologyHelper.NUMERIC.equals(attr.getName())){
							attr.setNumericValue(entry.getNumericValue());
						}
						attributes.add(attr);
					}
					
					// remove attributes that should not be there
					for(ConceptEntry e: new ArrayList<ConceptEntry>(entry.getAttributes())){
						if(!attributes.contains(e) && !entry.getText().contains(e.getText())){
							entry.removeAttribute(e);
						}
					}
										
					// now add all attributes
					for(ConceptEntry e: attributes){
						entry.addAttribute(e);
					}
					
					// add potential attributes
					List<ConceptEntry> alist = new ArrayList<ConceptEntry>();
					for(IClass c: OntologyHelper.getPotentialAttributes(cls)){
						ConceptEntry a = createConceptEntry(c,Constants.TYPE_ATTRIBUTE);
						// parse numeric number
						if(OntologyHelper.isNumber(c) && !NUMERIC.equals(c.getName())){
							a.setNumericValue(TextHelper.parseDecimalValue(a.getText()));
						}
						alist.add(a);
					}
					entry.setPotentialAttributes(alist);
				
				
					// set definition
					List<IClass> ancestors = OntologyHelper.getFindingAncesstors(cls,new ArrayList<IClass>());
					for(IClass c: ancestors ){
						if(!TextHelper.isEmpty(c.getConcept().getDefinition())){
							entry.setDefinition(cls.getConcept().getDefinition());
							break;
						}
					}
					// set defined feature
					if(ancestors.size() > 1){
						// if next ancestor finding has a definition, then it is defining
						IClass ans = ancestors.get(ancestors.size()-2);
						if(ans.getComments().length > 0)
							entry.setDefiniedFeature(createConceptEntry(ans,TYPE_FINDING));
					}
				}
				
				// set concept entry
				entry.setConcept(cls.getConcept());
				
				// set power
				entry.setPower(getPower(cls));
				/*
				Object obj = cls.getPropertyValue(ontology.getProperty(HAS_POWER));
    			if(!TextHelper.isEmpty(""+obj))
    				entry.setPower(""+obj);
    			else if(cls.hasSuperClass(ontology.getClass(ARCHITECTURAL_FEATURES)))
    				entry.setPower(POWER_LOW);
    			else if(cls.hasSuperClass(ontology.getClass(CYTOLOGIC_FEATURES)))
    				entry.setPower(POWER_HIGH);
    			else{
    				// find power of parents
    				for(IClass p: cls.getDirectSuperClasses()){
    					if(OntologyHelper.isFeature(p)){
    						obj = p.getPropertyValue(ontology.getProperty(HAS_POWER));
    						if(!TextHelper.isEmpty(""+obj)){
    							entry.setPower(""+obj);
    							break;
    						}
    					}
    				}
    			}
    			*/
				
    			// set worksheet property
    			if(isWorksheet(cls))
    				entry.setWorksheetFinding(true);
    			// set header property
    			if(isHeader(cls))
    				entry.setHeaderFinding(true);
    			
				// set example images
				ArrayList<URL> examples = new ArrayList<URL>();
				URL url = OntologyHelper.getExampleURL(ontology);
				for(Object e: cls.getPropertyValues(ontology.getProperty(OntologyHelper.HAS_EXAMPLE))){
					try{
						examples.add(new URL(url+"/"+e));
					}catch(MalformedURLException ex){
						ex.printStackTrace();
					}
				}
				entry.setExampleImages(examples);
				entry.setResolved(true);
			}
		}
		
	}
	
	/**
	 * figre out right power for the class
	 * @param cls
	 * @return
	 */
	private String getPower(IClass cls){
		Object obj = cls.getPropertyValue(ontology.getProperty(HAS_POWER));
		if(!TextHelper.isEmpty(""+obj))
			return ""+obj;
		// good default
		else if(cls.equals(ontology.getClass(ARCHITECTURAL_FEATURES)))
			return POWER_LOW;
		else if(cls.equals(ontology.getClass(CYTOLOGIC_FEATURES)))
			return POWER_HIGH;
		
		// if not set check ancestors
		for(IClass p: cls.getDirectSuperClasses()){
			if(OntologyHelper.isFeature(p)){
				return getPower(p);
			}
		}
		return null;
	}
	

	public IOntology getOntology(URI name, String version) {
		return getOntology(name);
	}

	public String[] getVersions(IOntology ont) {
		return (ont != null)?new String [] {ont.getVersion()}:new String [0];
	}

	public void sync(ExpertModule tm) {
		//NOOP
	}

	/**
	 * see if we want to filter content
	 */
	public ConceptFilter getConceptFilter() {
		if(filter == null){
			// look for include exclude filter
			String include = Config.getProperty(this,"concept.filter.include");
			String exclude = Config.getProperty(this,"concept.filter.exclude");
			
			final List<String> includeList = new ArrayList<String>();
			final List<String> excludeList = new ArrayList<String>();
			
			
			// check if parameter is a reference or the list itself
			try{
				URL ui = new URL(include);
				String includeText = TextHelper.getText(ui.openStream());
				Collections.addAll(includeList,includeText.trim().split("\\s*\n\\s*"));
			}catch(Exception ex){
				if(!(ex instanceof MalformedURLException))
					Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				
				// is it maybe a file
				File fi = new File(include);
				if(fi.exists()){
					try{
						FileInputStream fii = new FileInputStream(fi);
						String includeText = TextHelper.getText(fii);
						fii.close();
						Collections.addAll(includeList,includeText.trim().split("\\s*\n\\s*"));
					}catch(IOException ex1){
						Config.getLogger().severe(TextHelper.getErrorMessage(ex1));
					}
				}
			}
			
			// exclude filter
			try{
				URL ue = new URL(exclude);
				String excludeText = TextHelper.getText(ue.openStream());
				Collections.addAll(excludeList,excludeText.trim().split("\\s*\n\\s*"));
			}catch(Exception ex){
				if(!(ex instanceof MalformedURLException))
					Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				// exclude filter
				File fe = new File(exclude);
				if(fe.exists()){
					try{
						FileInputStream fei = new FileInputStream(fe);
						String excludeText = TextHelper.getText(fei);
						fei.close();
						Collections.addAll(excludeList,excludeText.trim().split("\\s*\n\\s*"));
					}catch(IOException ex1){
						Config.getLogger().severe(TextHelper.getErrorMessage(ex1));
					}
				}
			}
			
			
			
			// else parse as list itself
			if(includeList.isEmpty())
				Collections.addAll(includeList,Config.parseList(include));
				
			if(excludeList.isEmpty())
				Collections.addAll(excludeList,Config.parseList(exclude));
			
			
			// now we have 
			filter = new ConceptFilter() {
				public boolean accept(String parent, String name) {
					// if we have include list
					if(!includeList.isEmpty() && getDomainOntology() != null){
						// isIncluded
						boolean isIncluded = false;
						IOntology o = getDomainOntology();
						IClass cls = o.getClass(name);
						
						if(cls != null){ 
							for(String s: includeList){
								IClass exC = o.getClass(s);
								if(exC != null && (cls.hasSubClass(exC) || cls.hasSuperClass(exC) || cls.equals(exC))){
									isIncluded = true;
									break;
								}
							}
						}
						
						if(isIncluded){
							// if exclude list is present, check that
							if(!excludeList.isEmpty())
								return !excludeList.contains(name);
							// else if no exclude list, then it is kosher
							return true;
						}else{
							// not in include list forget it
							return false;
						}
					}else if(!excludeList.isEmpty()){
						return !excludeList.contains(name);
					}
					return true;
				}
			};
		
		
		}
		return filter;
	}
	
	
	public ConceptEntry createConceptEntry(IClass cls,String type){
		ConceptEntry entry = new ConceptEntry(cls.getName(), type);
		entry.setCategory(cls.getDirectSuperClasses()[0].getName());
		return entry;
	}
}
