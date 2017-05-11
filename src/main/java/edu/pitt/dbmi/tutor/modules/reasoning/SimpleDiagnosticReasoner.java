package edu.pitt.dbmi.tutor.modules.reasoning;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;


import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ErrorEntry;
import edu.pitt.dbmi.tutor.beans.LinkConceptEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.dbmi.tutor.beans.ShapeEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.BehavioralModule;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ReasoningModule;
import edu.pitt.dbmi.tutor.model.StudentModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.ui.ReasonerStateExplorer;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import edu.pitt.ontology.*;
import edu.pitt.slideviewer.ViewPosition;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
/**
 * basic "hello world" reasoner for diagnostic tutor
 * to get me going.
 * @author Administrator
 *
 */
public class SimpleDiagnosticReasoner implements ReasoningModule, ActionListener {
	private Properties defaultConfig;
	private CaseEntry problem;
	private ExpertModule expert;
	private StudentModule student;
	private Map<String,IInstance> instances;
	private IOntology ontology;
	private List<ConceptEntry> impliedHx, impliedDx;
	private Communicator com = Communicator.getInstance();
	private Set<String> levelHints;
	private Map<String,Object> findingInputMap;
	private Thread revalidateThread,nodeEventThread;
	private String currentImage;
	private ConceptEntry suggestedFinding;
	private ViewPosition currentView;
	private ConceptEntry nextStep = null;
	private LinkedList<NodeEvent> nodeEvents;
	private Tutor tutor;
	private JCheckBoxMenuItem debug;
	private ReasonerStateExplorer stateExplorer;
	
	// two flags that change the way Hx and Dx are inferred
	private boolean allHxMode, absoluteDxMode,refuteMode,autoAssertClinical;;
	private boolean interruptReasoning,doneOnDx,showImpliedDx;
	private ScenarioSet supportedScenarios;
	
	// register concepts that come in
	private Map<String,ConceptEntry> registry;
	private Stack<ErrorEntry> problemStack;
	
	// initialize behavior variables
	public SimpleDiagnosticReasoner(){
		// nodeEvents registry
		nodeEvents = new LinkedList<NodeEvent>();
		
		// concept registry
		registry = new ConcurrentHashMap<String, ConceptEntry>();
		problemStack = new Stack<ErrorEntry>(){
			public ErrorEntry push(ErrorEntry item) {
				// if empty, just push
				if(isEmpty())
					return super.push(item);
				
				// check last entry, to save time
				//if(item.getPriority() <= peek().getPriority())
				//	return super.push(item);
				
				// else see if there are higher priority elements out there
				int x;
				for(x = size() -1;x >= 0; x--){
					if(item.getPriority() <= elementAt(x).getPriority())
						break;
				}
				insertElementAt(item,x+1);
				return item;
			}
			
		};
	}
	
	/**
	 * register concept entry
	 * @param e
	 */
	
	private void registerConceptEntry(ConceptEntry e){
		if(e == null) 
			return;
	
		synchronized(registry){
			// if entry in an attribute, then remove a finding from previous parent
			if(TYPE_ATTRIBUTE.equals(e.getType()) && registry.containsKey(e.getFeature().getId())){
				ConceptEntry oldF = registry.get(e.getFeature().getId());
				//remove old finding and replace a feature
				if(oldF.hasParentEntry())
					registry.remove(oldF.getParentEntry().getId());	
				//registry.put(e.getFeature().getId(),e.getFeature());
				// make sure that this feature points to new parent entry
				oldF.setParentEntry(e.getParentEntry());
			}
			
			// register itself and its parent finding 
			// parent finding is often itself
			registry.put(e.getId(),e);
			if(e.hasParentEntry())
				registry.put(e.getParentEntry().getId(),e.getParentEntry());
		}
	}
	
	
	/**
	 * unregister concept entry
	 * @param e
	 */
	private void unregisterConceptEntry(ConceptEntry e){
		if(e == null)
			return;
		synchronized(registry){
		
			// remove concept itself, if no such concept, then ...
			if(registry.remove(e.getId()) == null){
				
				// well we can have two identical findings with different IDs because
				// they were generated by separate calls to OntologyHelper.createFinding
				// yet they represent the same thing, this normally happens when multiple
				// attributes are being deleted, in this case check if there is an equivalent
				// finding in the registry, if features match, then we got our finding
				if(e.isFinding()){
					for(ConceptEntry c: registry.values()){
						// isSubClassOf(e,c) this is to take care of refinement of one sibling to another
						// when several attributes were removed symalteniously
						if((c.equals(e) || isSubClassOf(e,c)) && c.getFeature().getId().equals(e.getFeature().getId())){
							registry.remove(c.getId());
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * add error 
	 * @param e
	 */
	private void pushError(ErrorEntry e){
		synchronized(problemStack){
			if(!problemStack.contains(e))
				problemStack.push(e);
		}
	}
	
	/*
	private boolean hasError(ErrorEntry e){
		return problemStack.contains(e);
	}
	*/
	
	/*
	 * get  last error
	 */
	private ErrorEntry popError(){
		synchronized(problemStack){
			// check for emtpty
			if(problemStack == null || problemStack.isEmpty())
				return null;
			
			// now look at the stack
			ErrorEntry e = problemStack.peek();
			if(e != null && checkResolved(e)){
				problemStack.remove(e);
				return popError();
			}
			return e;
		}
	}
	
	
	/**
	 * go through error list and clear resolved errors
	 */
	private void resolveErrors(){
		synchronized(problemStack){
			for(ErrorEntry e: new ArrayList<ErrorEntry>(problemStack)){
				if(checkResolved(e))
					problemStack.remove(e);
			}
		}
	}
	
	/**
	 * get problem stack
	 * @return
	 */
	private Collection<ErrorEntry> getProblemStack(){
		return new ArrayList<ErrorEntry>(problemStack);
	}
	
	
	/**
	 * has this error entry been resolved???
	 * @param err
	 * @return
	 */
	private boolean checkResolved(ErrorEntry err){
		if(err == null)
			return true;
	
		// get concept in question
		ConceptEntry entry = err.getConceptEntry();
		
		// is the concept in question even there?
		// if not, then error is resolved
		if(!registry.containsKey(entry.getId()))
			return true;
		
		// is entry set as resolved?
		if(err.isResolved())
			return true;
		
		
		// check all findings associated with this error
		// this is for all those specify questions
		if(!err.getFindings().isEmpty()){
			// first check for matching location
			ConceptEntry finding = null;
			for(ConceptEntry f: err.getFindings()){
				if(isCorrectLocation(f,findingInputMap.get(entry.getObjectDescription()))){
					finding = f;
					break;
				}
			}
			
			// now if there was no candidate check for what has been done
			if(finding == null){
				for(ConceptEntry f: err.getFindings()){
					if(isSubClassOf(f,entry.getParentEntry()) && !checkConcept(f)){
						finding = f;
						break;
					}
				}
			}else{
				// if we do have a candidate, check if is mentioned
				if(checkConcept(finding))
					finding = null;
			}
			
			// if after all of that finding is null, then we have nothing to do
			if(finding == null)
				return true;
			
			
			// else issue is not resolved, hance pick the first one  for a tag
			err.getConceptEntry().getTagMap().put(TAG_FINDING,finding.getText());
		}
		
		return false;
	}
	
	/**
	 * resolve error in the problem stack
	 * @param candidate
	 * @param error
	 */
	private void resolveError(ConceptEntry candidate, String error){
		synchronized(problemStack){
			for(ErrorEntry err: problemStack){
				if(err.getConceptEntry().getId().equals(candidate.getId())){
					if(error.equals(err.getError())){
						err.getConceptEntry().removeError(error);
						err.setResolved(true);
					}
					
				}
			}
		}
	}
	
	
	/**
	 * load expert module
	 */
	public void setExpertModule(ExpertModule module) {
		expert = module;
	}

	public void load(){
		allHxMode = "all".equals(Config.getProperty(this,"hypotheses.inference.mode"));
		absoluteDxMode = "absolute".equals(Config.getProperty(this,"diagnoses.inference.mode"));
		refuteMode = "refute".equals(Config.getProperty(this,"refute.inference.mode"));
		autoAssertClinical = "auto".equals(Config.getProperty(this,"clinical.inference.mode"));
		doneOnDx = "correct".equals(Config.getProperty(this,"solved.problem.mode"));
		
		addDebugOption();
	}
	
	
    /**
     * add debug option
     */
    private void addDebugOption(){
		JMenu debugM = ITS.getInstance().getDebugMenu();
		String text = "Show Reasoner State";
		if(!UIHelper.hasMenuItem(debugM,text)){
			debugM.addSeparator();
			debug = UIHelper.createCheckboxMenuItem("show-state",text,Config.getIconProperty("icon.menu.debug"),this);
			debugM.add(debug);
			//debugM.add(UIHelper.createCheckboxMenuItem("show-implied-dx","Show Implied Diagnoses",null,this));
			//debugM.add(UIHelper.createMenuItem("clear-problems","Clear Problem States",null,this));
		}
    }
    

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("show-state".equals(cmd)){
			debugShowState(System.currentTimeMillis());
			Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show Reasoner State",(debug.isSelected())?ACTION_SELECTED:ACTION_DESELECTED));
		}else if("show-implied-dx".equals(cmd)){
			showImpliedDx = ((AbstractButton)e.getSource()).isSelected();
			Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show Implied Diagnosis",(showImpliedDx)?ACTION_SELECTED:ACTION_DESELECTED));
		}else if("clear-problems".equals(cmd)){
			//problemConcepts.clear();
			problemStack.clear();
			debugShowState(System.currentTimeMillis());
			Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Clear Tutor Problems",ACTION_CLEAR));
			JOptionPane.showMessageDialog(Config.getMainFrame(),"Problems States were Cleared!");
		}
	}
	
	/**
	 * load case information (open image)
	 */
	public void setCaseEntry(CaseEntry problem){
		// reset previous problem state
		reset();
		long time = System.currentTimeMillis();
		this.problem = problem;
		
		// initialize instances
		ontology = expert.getDomainOntology();
		// start new instances map
		instances = new HashMap<String, IInstance>();
		for(String part : problem.getParts()){
			instances.put(part,getInstance(ontology.getClass(CASES),problem.getName()+"_"+part));
		}
		levelHints = null;
		findingInputMap = new HashMap<String, Object>();
		
		// assert clinical info
		if(autoAssertClinical){
			for(ConceptEntry e: problem.getConcepts(CLINICAL_FEATURES).getValues()){
				addConceptInstance(e);
			}
		}
		if(problem.getPrimarySlide() != null)
			currentImage = problem.getPrimarySlide().getName();
		debugShowState(time);
		// clear next step
		nextStep = null;
	}
	

	
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Reasons on diagnostic information (information related to coming to a diagnosis). " +
				"The Reasoner evaluates user's actions in the context of the chosen domain.";
	}

	
	public String getName() {
		return "Simple Diagnostic Reasoner";
	}

	public String getVersion() {
		return "2.0";
	}


	public void setStudentModule(StudentModule module) {
		student = module;
		
	}
	
	/**
	 * has this concept been mentioned by a student
	 */
	private boolean checkConcept(ConceptEntry entry){
		boolean status = false;
		// if it is in the list, then we are good to go
		if(getConcepts().contains(entry)){
			status =  true;
		}else{
			// check if there is a more specific concept that has been mentioned
			for(ConceptEntry e: new ArrayList<ConceptEntry>(getConcepts())){
				if(e.getType().equals(entry.getType()) && isSubClassOf(e,entry)){
					status =  true;
					break;
				}
			}
		}
		
		// was it found
		entry.setFound(status);
		return status;
	}
	
	
	/**
	 * has this concept been mentioned by a student
	 */
	private boolean checkConceptInCase(String category, ConceptEntry entry){
		// if it is in the list, then we are good to go
		if(problem.getConcepts(category).containsKey(entry.getName()))
			return true;
		
		// check if there is a more sepcific concept that has been mentioned
		for(ConceptEntry e: problem.getConcepts(category).getValues()){
			if(e.getType().equals(entry.getType()) && isSubClassOf(entry,e))
				return true;
		}
		return false;
	}
	
	
	/**
	 * get all concepts that were mentioned by a student
	 * @return
	 */
	private Collection<ConceptEntry> getConcepts(){
		synchronized(registry){
			return registry.values();
		}
	}
	
	/**
	 * get all mentioned hypothesis
	 * @return
	 */
	private List<ConceptEntry> getHypotheses(){
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(ConceptEntry e : getConcepts()){
			if(TYPE_HYPOTHESIS.equals(e.getType()))
				list.add(e);
		}
		return list;
	}
	/**
	 * get all mentioned diagnoses
	 * @return
	 */
	private List<ConceptEntry> getDiagnoses(){
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(ConceptEntry e : getConcepts()){
			if(TYPE_DIAGNOSIS.equals(e.getType()))
				list.add(e);
		}
		return list;
	}
	
	/**
	 * get registered concept
	 * @param id
	 * @return
	 */
	private ConceptEntry getRegisteredConcept(String id){
		return registry.get(id);
	}
	
	/**
	 * return a concept that the reasoner thinks
	 * should be handled next
	 * @return
	 */
	public ConceptEntry getNextConcept(){
		if(problem == null)
			return null;
		
		// check if there are unidentified concepts in case
		List<ConceptEntry> candidates = problem.getConcepts(DIAGNOSTIC_FEATURES).getValues();
		
		// select a candidate w/ lowest power that has not been found
		ConceptEntry candidate = null;
		for(ConceptEntry e : candidates){
			// make sure that concept was not found already
			if(!checkConcept(e) && e.isImportant()){
				candidate = e;
				break;
				// we need to select something w/ smallest power
				//NOTE: this is now done in DomainExpert module
				/*
				if(candidate == null){
					candidate = e;
				}else if(TextHelper.isSmallerPower(e.getPower(),candidate.getPower())){
					candidate = e;
				}
				*/
			}
		}
		
		// if no findings, then we should consider diagnoses
		if(candidate == null){
			candidates = problem.getConcepts(DIAGNOSES).getValues();
			// make sure that concept was not found already
			for(ConceptEntry e : candidates){
				// make sure that concept was not found already
				if(!checkConcept(e)){
					candidate = e;
					break;
				}
			}
		}
		
		
		// make sure next candidate is not already in the instance
		// if it is add it to correct concept
		if(candidate != null && candidate.isFinding() && hasConceptInstance(candidate)){
			registerConceptEntry(candidate);
			return getNextConcept();
		}
		return candidate;
	}
	
	
	/**
	 * get next step
	 * @return
	 */
	public ConceptEntry getNextStep(){
		//long time = System.currentTimeMillis();
		ConceptEntry next = null;
		
		// get top error and get is finding unless it 
		// is a an attribute hint
		ErrorEntry err = popError();
		if(err != null){
			if(!err.getFindings().isEmpty()){
				// if we have multiple candidates and have a configured tag finding 
				// use that value as the most acurate
				if(err.getFindings().size() > 1 && err.getConceptEntry().getTagMap().containsKey(TAG_FINDING)){
					String fn = err.getConceptEntry().getTagMap().get(TAG_FINDING);
					next = new ConceptEntry(TextHelper.getClassName(fn),err.getConceptEntry().getType());
				}else
					next = err.getFindings().get(0);
			}else
				next = err.getConceptEntry();
			
		}
		
		// get next finding if we don't have an error
		if(next == null)
			next = getNextConcept();
		
		if(next == null){
			next = new ConceptEntry(TYPE_DONE,TYPE_DONE);
		}
		//System.out.println("next step calc: "+(System.currentTimeMillis()-time));
		return next;
	}
	
	
	/**
	 * filter next step (break it up into feature attribute)
	 * @param entry
	 * @return
	 */
	private ConceptEntry filterNextStep(ConceptEntry entry){
		if(entry.isFinding()){
			// if this is a feture that came from error
			if(Arrays.asList(HINT_MISSING_ATTRIBUTE,HINT_MISSING_ANOTHER_ATTRIBUTE).contains(entry.getError())){
				if(entry.getTagMap().containsKey(TAG_FINDING)){
					entry = new ConceptEntry(TextHelper.createConceptName(false,entry.getTagMap().get(TAG_FINDING)),entry.getType());
				}
			}
			final String finding = entry.getName();
			
			// resolve
			expert.resolveConceptEntry(entry);
			
			// if this is just a feature
			if(entry.getFeature().equals(entry)){
				return entry;
			}
			
			// check what we already have
			List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(entry.getAttributes());
			// sort attributes based on their location in finding name
			Collections.sort(attributes,new Comparator<ConceptEntry>() {
				public int compare(ConceptEntry o1, ConceptEntry o2) {
					return finding.indexOf(o1.getName()) -finding.indexOf(o2.getName());
				}
			});
			List<ConceptEntry> candidates = new ArrayList<ConceptEntry>();
			candidates.add(entry.getFeature());
			candidates.addAll(attributes);
			for(ConceptEntry candidate : candidates){
				for(ConceptEntry c: getConcepts()){
					if(c.getName().equals(candidate.getName())){
						// account for same attribute name, but different feature
						if(!TYPE_ATTRIBUTE.equals(candidate.getType()) || c.getFeature().getName().equals(candidate.getFeature().getName())){
							candidate = null;
							break;
						}
					}
				}
				if(candidate != null){
					candidate.setFeature(entry.getFeature());
					candidate.setParentEntry(entry);
					return candidate;
				}
			}
			
		}
		return entry;
	}
	
	/**
	 * get a list of candidates that need to be put in one_by_one for a given finding (resolved)
	 * @param finding
	 * @return
	 */
	private List<ConceptEntry> getCandidateList(ConceptEntry entry){
		List<ConceptEntry> toRet = new ArrayList<ConceptEntry>();
		// check what we already have
		final String finding = entry.getName();
		List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(entry.getAttributes());
		// sort attributes based on their location in finding name
		Collections.sort(attributes,new Comparator<ConceptEntry>() {
			public int compare(ConceptEntry o1, ConceptEntry o2) {
				return finding.indexOf(o1.getName()) -finding.indexOf(o2.getName());
			}
		});
		List<ConceptEntry> candidates = new ArrayList<ConceptEntry>();
		candidates.add(entry.getFeature());
		candidates.addAll(attributes);
		for(ConceptEntry candidate : candidates){
			for(ConceptEntry c: getConcepts()){
				if(c.getName().equals(candidate.getName())){
					candidate = null;
					break;
				}
			}
			if(candidate != null){
				candidate.setFeature(entry.getFeature());
				candidate.setParentEntry(entry);
				toRet.add(candidate);
			}
		}
		return toRet;
	}
	
	
	/**
	 * return a problem concept that the reasoner thinks
	 * should be handled next
	 * @return next problem concept, or null if there are no problems
	 */
	public ConceptEntry getProblemConcept(){
		//return problemConcept;
		ErrorEntry err = popError();
		return (err != null)?err.getConceptEntry():null;
	}
	
	/**
	 * has the current case been solved
	 * @return
	 */
	public boolean isSolved(){
		if(problem == null)
			return true;
		
		// if only correct diagnosis is required, then
		// make sure that we have it
		if(doneOnDx){
			// go over all concepts in error stack
			synchronized(problemStack){
				for(ErrorEntry err: problemStack){
					if(err.getConceptEntry().getType() == TYPE_DIAGNOSIS){
						if(!checkResolved(err))
							return false;
					}
				}
			}
			
			// now check that we have ALL correct diagnosis
			for(ConceptEntry d : problem.getConcepts(DIAGNOSES).getValues()){
				if(!checkConcept(d))
					return false;
			}
			
			return true;
		}
		return popError() == null && getNextConcept() == null;
	}
	
	
	/**
	 * evalyate Hypothesis
	 * @param ex
	 * @param inst
	 * @return
	 *
	private boolean evaluateHx(ILogicExpression exp, IInstance inst){
		if(allHxMode)
			return evaluateExpression(exp, inst);
		return findInExpressionOld(exp, inst);
	}
	*/
	/**
	 * evalyate Hypothesis
	 * @param ex
	 * @param inst
	 * @return
	 */
	private boolean evaluateHx(ILogicExpression exp, IInstance inst){
		if(allHxMode)
			return evaluateExpression(exp, inst);
		return findInExpression(exp, inst);
	}
	
	/**
	 * evalyate Hypothesis
	 * @param ex
	 * @param inst
	 * @return
	 */
	private boolean evaluateDx(ILogicExpression exp, IInstance inst){
		if(absoluteDxMode)
			return exp.evaluate(inst);
		return evaluateExpression(exp, inst);
	}
	
	
	/**
	 * does hypothes
	 * @param h
	 * @return
	 *
	private boolean checkHypothesisHasAnyFindings(ConceptEntry h){
		IClass d = ontology.getClass(h.getName());
		for(IInstance instance: instances.values())
			if(findInExpression(d.getEquivalentRestrictions(),instance))
				return true;
		return false;
	}
	*/
	/**
	 * check if there are findings
	 * @return
	 */
	private boolean checkFindings(){
		// go over all concepts
		for(ConceptEntry e: getConcepts()){
			if(e.isFinding())
				return true;
		}
		return false;
	}
	
	/**
	 * get a list of implied hypotheses based on a given
	 * evidence
	 * @return
	 */
	public synchronized List<ConceptEntry> getImpliedHypotheses(){
		if(impliedHx == null){
			
			// don't bother doing the work if there aren't any findings
			if(interruptReasoning || !checkFindings()){
				impliedHx = Collections.EMPTY_LIST;
				return impliedHx;
			}
			//long time = System.currentTimeMillis();
			// do the work
			Set<ConceptEntry> list = new HashSet<ConceptEntry>();
			for (IClass diseaseCls: ontology.getClass(DIAGNOSES).getSubClasses()){
				// check for interrupt
				if(interruptReasoning)
					return impliedDx;
				//long time = System.currentTimeMillis();
				// else continue
				ILogicExpression exp = diseaseCls.getEquivalentRestrictions();
				if (exp != null && !exp.isEmpty()) {
					// set expression as CONJUNCTION so that partial evidence can be evaluated
					for(IInstance instance: instances.values()){
						if(evaluateHx(exp,instance)) {
							//olist.add(diseaseCls.getName());
							list.add(new ConceptEntry(diseaseCls.getName(),TYPE_HYPOTHESIS));
							
							// if direct children don't have rules add them too
							for(IClass dChild: diseaseCls.getDirectSubClasses()){
								if(dChild.getEquivalentRestrictions().isEmpty()){
									list.add(new ConceptEntry(dChild.getName(),TYPE_HYPOTHESIS));
								}
							}
						}
					}
				}
			}
				
			//System.out.println("hx implied "+(System.currentTimeMillis()-time));
			impliedHx =  new ArrayList<ConceptEntry>(list);
		}
		return impliedHx;
	}
	
	/**
	 * get a list of implied hypotheses based on a given finding
	 * evidence
	 * @return
	 */
	public List<ConceptEntry> getImpliedHypotheses(ConceptEntry finding){
		IClass f = getConceptClass(finding, ontology);
		IProperty p = ontology.getProperty(finding.isAbsent()?HAS_NO_FINDING:HAS_FINDING);
		//long time = System.currentTimeMillis();
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(IClass diseaseCls: ontology.getClass(DIAGNOSES).getSubClasses()){
			ILogicExpression exp = diseaseCls.getEquivalentRestrictions();
			if(!exp.isEmpty() && isFindingInDiagnosticRule(f,p,exp)){
				list.add(new ConceptEntry(diseaseCls.getName(),TYPE_HYPOTHESIS));
			}
		}
		//System.out.println("implided hx "+(System.currentTimeMillis()-time));
		return list;
	}
	
		
	/**
	 * get a list of implided diagnoses
	 */
	public synchronized List<ConceptEntry> getImpliedDiagnoses() {
		if(impliedDx == null){
			// don't bother doing the work if there aren't any findings
			if(interruptReasoning || !checkFindings()){
				impliedDx = Collections.EMPTY_LIST;
				return impliedDx;
			}
			//long time = System.currentTimeMillis();
			// do the work
			Set<ConceptEntry> list = new HashSet<ConceptEntry>();
			for (IClass diseaseCls: ontology.getClass(DIAGNOSES).getSubClasses()){
				// check for interrupt
				if(interruptReasoning)
					return impliedDx;
				// else continue
				ILogicExpression exp = diseaseCls.getEquivalentRestrictions();
				if (exp != null && !exp.isEmpty()) {
					// does the evidence as as fit the bill to call DX
					for(IInstance instance: instances.values()){
						if(evaluateDx(exp,instance)){
							list.add(new ConceptEntry(diseaseCls.getName(),TYPE_DIAGNOSIS));
						}
					}
				}
			}
			//System.out.println("dx implied "+(System.currentTimeMillis()-time));
			impliedDx =  new ArrayList<ConceptEntry>(list);
		}
		return impliedDx;
	}
	
	
	/**
	 * get a list of implied diagnosis based on features
	 * @return
	 */
	private Collection<ConceptEntry> getImpliedDiagnosisFromFeatures(){
		Set<ConceptEntry> list = new HashSet<ConceptEntry>();
		// create an instance and put in all of the features
		IInstance inst = getInstance(ontology.getClass(CASES),"tempcase2");
		for(IInstance instance: instances.values()){
			for(String prop: Arrays.asList(HAS_FINDING,HAS_NO_FINDING,HAS_CLINICAL)){
				for(Object o: instance.getPropertyValues(ontology.getProperty(prop))){
					if(o instanceof IInstance){
						IClass cls = ((IInstance)o).getDirectTypes()[0];
						addFinding(new ConceptEntry(getFeature(cls).getName(),TYPE_FINDING),inst,prop);
					}
				}
			}
		}
		
		// now go through all DX and calculate the implication
		for (IClass diseaseCls: ontology.getClass(DIAGNOSES).getSubClasses()){
			// else continue
			ILogicExpression exp = diseaseCls.getEquivalentRestrictions();
			if (exp != null && !exp.isEmpty()) {
				// does the evidence as as fit the bill to call DX
				if(evaluateExpression(exp, inst)){
					list.add(new ConceptEntry(diseaseCls.getName(),TYPE_DIAGNOSIS));
				}	
			}
		}	
		
		// remove instance
		inst.delete();
		
		return list;
	}
	
	/**
	 * get a list of implied diagnosis based on features
	 * @return
	 */
	private Collection<ConceptEntry> getImpliedFeatures(List<ConceptEntry> dxs){
		if(dxs.isEmpty())
			return Collections.EMPTY_SET;
		
		Set<ConceptEntry> list = new HashSet<ConceptEntry>();
		// all all elements of the first list
		list.addAll(getFeatures(dxs.get(0)));
		// now retain only the intersection
		for(int i=1;i<dxs.size();i++)
			list.retainAll(getFeatures(dxs.get(i)));
		
		return list;
	}
	
	/**
	 * get features that are part of the diagnostic rule
	 * @param dx
	 * @return
	 */
	private Set<ConceptEntry> getFeatures(ConceptEntry dx){
		Set<ConceptEntry> list = new LinkedHashSet<ConceptEntry>();
		for(ConceptEntry f: getDiagnosticFindings(dx, ontology)){
			list.add(new ConceptEntry(getFeature(getConceptClass(f,ontology)).getName(),f.getType()));
		}
		return list;
	}
	
	
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	private String getInconsistantFindings(ConceptEntry d){
		if(ontology == null)
			return "";
	
		StringBuffer buf = new StringBuffer();
		IClass dx = ontology.getClass(d.getName());
		for(String p: new String [] {HAS_FINDING, HAS_NO_FINDING}){
			IProperty prop  = ontology.getProperty(p);
			for(IInstance instance: instances.values()){
				for(Object o: instance.getPropertyValues(prop)){
					if(o instanceof IInstance){
						IClass fn = ((IInstance)o).getDirectTypes()[0];
						String n = (HAS_NO_FINDING.equals(p))?"NO ":"";
						// get contradicting finding
						if(!OntologyHelper.isFindingInDiagnosticRule(fn,prop, dx.getEquivalentRestrictions())){
							buf.append(n+UIHelper.getTextFromName(fn.getName())+", ");
						}
					}
				}
			}
		}
		String s = ""+buf;
		return (s.length()> 2)?s.substring(0,s.length()-2):s;
	}
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	private String getDiseaseRule(ConceptEntry d){
		if(ontology == null)
			return "";
		
		IClass dx = ontology.getClass(d.getName());
		IInstance inst = null;
		if(ILogicExpression.OR == dx.getEquivalentRestrictions().getExpressionType()){
			inst = problem.createInstance(ontology);
		}
		return OntologyHelper.getDiseaseRuleText(dx,inst);
	}
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	private boolean checkDiseaseRule(ConceptEntry d){
		if(ontology == null)
			return false;
		IClass dx = ontology.getClass(d.getName());
		boolean hasRule =  dx != null && !dx.getEquivalentRestrictions().isEmpty();
		
		// if no rule, check if parents have rules
		if(dx != null && !hasRule){
			for(IClass p: dx.getDirectSuperClasses()){
				if(!p.getEquivalentRestrictions().isEmpty())
					return true;
			}
		}
		return hasRule;
	}
	
	/**
	 * evaluate expression
	 * 
	 * @param exp
	 * @param inst
	 * @return
	 */
	private boolean evaluateExpression(ILogicExpression exp, IInstance inst) {
		if (exp.getExpressionType() == ILogicExpression.OR) {
			for (Object o : exp) {
				if (evaluate(evaluate(o, inst,new HashSet<String>()),(ILogicExpression)o,inst))
				//if (evaluate(evaluate(o, inst,new HashSet<String>()).size(),termCount((ILogicExpression)o), inst))
					return true;
			}
			return false;
		} else {
			return evaluate(evaluate(exp, inst,new HashSet<String>()),exp, inst);
			//return evaluate(evaluate(exp, inst,new HashSet<String>()).size(),termCount(exp), inst);
		}
	}

	/**
	 * evaluate expression
	 * 
	 * @param exp
	 * @param inst
	 * @return
	 *
	private boolean findInExpression(ILogicExpression exp, IInstance inst) {
		return evaluate(exp,inst) > 0;
	}
	*/
	/**
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 */
	private boolean findInExpression(Object exp, Object param) {
		if(interruptReasoning)
			return false;
		
		if (exp instanceof ILogicExpression) {
			// iterate over parameters
			for (Object obj : (ILogicExpression) exp) {
				if(findInExpression(obj, param))
					return true;
			}
			return false;
		} else if (exp instanceof IRestriction && param instanceof IInstance) {
			IRestriction r = (IRestriction) exp;
			IInstance inst = (IInstance) param;
			Object[] values = inst.getPropertyValues(r.getProperty());
			if (values == null || values.length == 0)
				return false;
			// if any of values fits, that we are good
			ILogicExpression value = r.getParameter();
			for (int i = 0; i < values.length; i++) {
				if(findInExpression(value,values[i]))
					return true;
			}
			return false;
		} else if (exp instanceof IClass) {
			IClass c = (IClass) exp;
			if(c.evaluate(param))
				return true;
			
			// we want to be a little bit more lenient
			// and allow say Blister in case to match Subepidermal_Blister in rule
			// In this scenario for WE are ok if rule finding is more specific
			// then what is in case as long as what is in case is at least as specific
			// as a feature
			if(param instanceof IInstance){
				IInstance i = (IInstance) param;
				IClass evidence = i.getTypes()[0];
				IClass feature = OntologyHelper.getFeature(c);
				if((feature.equals(evidence) || feature.hasSubClass(evidence)) && c.hasSuperClass(evidence)){
					return true;
				}
			}
			return false;
		} else {
			return exp.equals(param);
		}
	}
	/**
	 * count number of terms in expression
	 * @param exp
	 * @return
	 */
	private int termCount(ILogicExpression exp){
		//if we have an ANDed expression then we care about number of terms
		if(exp.getExpressionType() == ILogicExpression.AND){
			return exp.size();
		// if we have the ORed expression then we care about the smallest n
		// of its content
		}else if(exp.getExpressionType() == ILogicExpression.OR){
			int n = 777;
			for(Object o:exp){
				if(o instanceof ILogicExpression){
					int x = termCount((ILogicExpression) o);
					if(x < n)
						n = x;
				}
			}
			return n;
		}
		// default makes no sense
		return 777;
	}
	
	/**
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 *
	private int evaluate(Object exp, Object param) {
		if(interruptReasoning)
			return 0;
		
		int hits = 0;
		if (exp instanceof ILogicExpression) {
			// iterate over parameters
			for (Object obj : (ILogicExpression) exp) {
				hits += evaluate(obj, param);
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
				if (evaluate(value,values[i]) > 0)
					hits++;
			}
			return hits;
		} else if (exp instanceof IClass) {
			IClass c = (IClass) exp;
			if(c.evaluate(param))
				return 1;
			
			// we want to be a little bit more lenient
			// and allow say Blister in case to match Subepidermal_Blister in rule
			// In this scenario for WE are ok if rule finding is more specific
			// then what is in case as long as what is in case is at least as specific
			// as a feature
			if(param instanceof IInstance){
				IInstance i = (IInstance) param;
				IClass evidence = i.getTypes()[0];
				IClass feature = OntologyHelper.getFeature(c);
				if((feature.equals(evidence) || feature.hasSubClass(evidence)) && c.hasSuperClass(evidence)){
					return 1;
				}
			}
			return 0;
		} else {
			return (exp.equals(param)) ? 1 : 0;
		}
	}
	*/

	/**
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 */
	private Set<String> evaluate(Object exp, Object param, Set<String> matched) {
		if(interruptReasoning)
			return matched;
		
		if (exp instanceof ILogicExpression) {
			// iterate over parameters
			for (Object obj : (ILogicExpression) exp) {
				evaluate(obj, param, matched);
			}
			return matched;
		} else if (exp instanceof IRestriction && param instanceof IInstance) {
			IRestriction r = (IRestriction) exp;
			IInstance inst = (IInstance) param;
			Object[] values = inst.getPropertyValues(r.getProperty());
			if (values == null || values.length == 0)
				return matched;
			// if any of values fits, that we are good
			ILogicExpression value = r.getParameter();
			for (int i = 0; i < values.length; i++) {
				evaluate(value,values[i],matched);
			}
			return matched;
		} else if (exp instanceof IClass) {
			IClass c = (IClass) exp;
			if(c.evaluate(param)){
				matched.add(c.getName());
				return matched;
			}
			
			// we want to be a little bit more lenient
			// and allow say Blister in case to match Subepidermal_Blister in rule
			// In this scenario for WE are ok if rule finding is more specific
			// then what is in case as long as what is in case is at least as specific
			// as a feature
			if(param instanceof IInstance){
				IInstance i = (IInstance) param;
				IClass evidence = i.getTypes()[0];
				IClass feature = OntologyHelper.getFeature(c);
				if((feature.equals(evidence) || feature.hasSubClass(evidence)) && c.hasSuperClass(evidence)){
					matched.add(evidence.getName());
					return matched;
				}
			}
			return matched;
		} else {
			if(exp.equals(param))
				matched.add(exp.toString());
			return matched;
		}
	}
	
	
	/**
	 * evaluate based on number of hits (as oppose to logical operation)
	 * 
	 * @param hits
	 * @param inst
	 * @return
	 */
	private boolean evaluate(int hits, int expTerms, IInstance inst) {
		
		// we can be here only if we just processed expression
		IProperty p1 = inst.getOntology().getProperty(OntologyHelper.HAS_FINDING);
		IProperty p2 = inst.getOntology().getProperty(OntologyHelper.HAS_NO_FINDING);
		//IProperty p3 = inst.getOntology().getProperty(OntologyHelper.HAS_CLINICAL);
		//IProperty p4 = inst.getOntology().getProperty(OntologyHelper.HAS_ANCILLARY);
		
		// see if this instance has a value that fits this restriction
		Object[] v1 = inst.getPropertyValues(p1);
		Object[] v2 = inst.getPropertyValues(p2);
		//Object[] v3 = inst.getPropertyValues(p3);
		//Object[] v4 = inst.getPropertyValues(p4);
		
		// System.out.println(hits+" vs "+v1.length+"+"+v2.length);
		return (hits >= (v1.length + v2.length)) || hits >= expTerms;
	}
	
	/**
	 * evaluate based on number of hits (as oppose to logical operation)
	 * 
	 * @param hits
	 * @param inst
	 * @return
	 */
	private boolean evaluate(Set<String> matched, ILogicExpression exp, IInstance inst) {
		
		// we can be here only if we just processed expression
		IProperty p1 = inst.getOntology().getProperty(OntologyHelper.HAS_FINDING);
		IProperty p2 = inst.getOntology().getProperty(OntologyHelper.HAS_NO_FINDING);
		
		// see if this instance has a value that fits this restriction
		Object[] v1 = inst.getPropertyValues(p1);
		Object[] v2 = inst.getPropertyValues(p2);
		
		// System.out.println(hits+" vs "+v1.length+"+"+v2.length);
		int hits = matched.size();
		return (hits >= (v1.length + v2.length)) || (hits >= termCount(exp) && exp.evaluate(inst));
	}

	

	
	public void dispose() {
		reset();
		expert = null;
		student = null;
		//ontology = null;
	}

	
	public void reset() {
		// wait for revalidate thread to catch up
		//long time = System.currentTimeMillis();
		if(revalidateThread != null && revalidateThread.isAlive()){
			try{
				interruptReasoning = true;
				revalidateThread.join();
				revalidateThread = null;
			}catch(Exception ex){};
		}
		
		//wait for node event thread
		if(nodeEventThread != null && nodeEventThread.isAlive()){
			try {
				nodeEventThread.join();
			} catch (InterruptedException e) {
			}
			nodeEventThread = null;
		}
		
				
		suggestedFinding = null;
		impliedDx = impliedHx = null;
		registry.clear();
		problemStack.clear();
		findingInputMap = null;
		if(instances != null){
			for(IInstance instance: instances.values()){
				if(ontology.isLoaded())
					instance.delete();
			}
			instances.clear();
			instances = null;
		}
		problem = null;
		ontology = null;
		//System.out.println(System.currentTimeMillis()-time);
	}

	/**
	 * resolve an arbitrary action
	 * if action is understood, the module will
	 * "resolve" it, by assigning runnable code
	 * to it, for later execution
	 * @param action
	 */
	public void resolveAction(Action action){}
	
	
	/**
	 * try to get error code
	 * @param error
	 * @return
	 */
	private String getCode(String error){
		if(getTutor() != null && getTutor().getFeedbackModule()  != null){
			ScenarioSet set = getTutor().getFeedbackModule().getScenarioSet();
			if(set != null){
				ScenarioEntry entry = set.getScenarioEntry(error);
				return (entry != null)?""+entry.getErrorCode():"0";
			}
		}
		return null;
	}
	
	/**
	 * get a tutor response
	 * @param response
	 * @param error
	 * @return
	 */
	private void sendTutorResponse(Message msg,String response,String error){
		sendTutorResponse(msg, response, error,null);
	}
	
	/**
	 * get a tutor response
	 * @param response
	 * @param error
	 * @return
	 */
	private void sendTutorResponse(Message msg,String response,String error, Object input){
		TutorResponse r = new TutorResponse();
		r.setResponse(response);
		r.setError(error);
		r.setClientEvent((ClientEvent)msg);
		r.setCode(getCode(error));
		
		// set error code for correctly removing incorrect concept
		if(ACTION_REMOVED.equals(msg.getAction()) && ERROR_OK.equals(error))
			r.setCode("5");
		
		// clear next step if done is pressed or something is removed
		if(TYPE_DONE.equals(msg.getType()))
			nextStep = null;		
		
		// set next step fields based on input object
		// determine when msg should be changed
		if(input != null && input instanceof ConceptEntry && 
			Arrays.asList(ACTION_ADDED,ACTION_REMOVED,ACTION_REFINE).contains(msg.getAction())){ 
			ConceptEntry c = (ConceptEntry) input;
			
			// if request was a hint or a bug occured, then nextstep
			// is an object that was passed
			if(TYPE_HINT.equals(msg.getType()) || RESPONSE_FAILURE.equals(response))
				nextStep = c;
			else
				nextStep = null;
			
			// remember concept state
			// shit don't do it for everything!!!! just for adding stuff
			if(Arrays.asList(ACTION_ADDED,ACTION_REFINE,ACTION_REMOVED).contains(msg.getAction()))
				c.setConceptStatus(isIrrelevant(error)?RESPONSE_IRRELEVANT:response);	
			
			// add message to problem stack
			if(RESPONSE_FAILURE.equals(response) && !(isOK(error) || isIrrelevant(error))){
				pushError(new ErrorEntry(error,c));
			}
			/*
			 * Whya did I do that?????
			else if(!Arrays.asList(ERROR_OK,ERROR_DIAGNOSIS_IS_SUPPORTED_BUT_NOT_GOAL,ERROR_DIAGNOSIS_IS_CORRECT).contains(error)){
				// add error anyway even if it is just a tip
				c.addError(error);
			}
			*/
			
			// set response concept
			r.setResponseConcept(c.getObjectDescription());
		}else{
			r.setResponseConcept(msg.getObjectDescription());
		}
			
		
		// set next step fields
			
		// get next step if not provided
		if(nextStep == null)
			nextStep = getNextStep();
		
		// filter next step
		nextStep = filterNextStep(nextStep);
		
		// figure out action for next step
		String action = ACTION_NEXT_STEP;
		//if(RESPONSE_FAILURE.equals(response) && !TYPE_DONE.equals(msg.getType()))
		if(nextStep.hasErrors()) // why was it here?&& !TYPE_DONE.equals(msg.getType())
			action = ACTION_REMOVED;
		else
			action = ACTION_ADDED;
		
		// is it maybe refine?
		if(isRefineNextAction(nextStep,response))
			action = ACTION_REFINE;
		
		// set everything up for next step
		r.setType(nextStep.getType());
		r.setLabel(nextStep.getName());
		r.setAction(action);
		
		// set parent if appropriate
		if(nextStep.getParent() != null)
			r.setParent(nextStep.getParent());
		
		// if in response to failure, then use the id
		// next step for OK has no ID
		//if(RESPONSE_FAILURE.equals(response))
		if(nextStep.hasId())	
			r.setId(nextStep.getId());
	
		r.setInput(input);
		r.setTimestamp(System.currentTimeMillis());
		r.setSender(this);
		
		// write out response
		//if(debug && !TYPE_PRESENTATION.equals(msg.getType())){
		//	System.out.println(msg.getType().toUpperCase()+" "+msg.getAction().toLowerCase()+
		//					" \""+msg.getLabel()+"\" | "+r.getResponse()+" | "+r.getError());
		//}
		
		// send out tutor response 
		com.sendMessage(r);
		
		// generate node event
		if(TYPE_HINT.equals(msg.getType())){
			// if this is missing attribute hint we might want to spawn multiple node events
			if(TYPE_ATTRIBUTE.equals(nextStep.getType()) && 
			    Arrays.asList(HINT_MISSING_ATTRIBUTE,HINT_MISSING_ANOTHER_ATTRIBUTE).contains(error)){
				// spawn several events
				for(ConceptEntry a: getCandidateList(nextStep.getParentEntry()))
					doNodeEvent(msg,r,a);
			}else{
				doNodeEvent(msg,r,nextStep);
			}	
		}else
			doNodeEvent(msg,r,input);
			
		//return r;
	}

	
	/**
	 * check if we should bother creating a node event for a 
	 * given event type
	 * @param type
	 * @return
	 */
	private boolean isValidNodeType(Message msg){
		String eventType = msg.getType();
		// don't generate node events for automatic actions done by tutor
		if(msg.getInput() instanceof Map && "true".equalsIgnoreCase(""+((Map)msg.getInput()).get("auto")))
			return false;
		
		// don't care for self check
		//if(ACTION_SELF_CHECK.equals(msg.getAction()))
		//	return false;
		
		// check event type
		for(String type: new String [] {TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ATTRIBUTE,TYPE_HYPOTHESIS,
										TYPE_DIAGNOSIS,TYPE_HINT,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK}){
			if(type.equals(eventType)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * should next step action be refine
	 * @param e
	 * @return
	 */
	private boolean isRefineNextAction(ConceptEntry e, String err){
		//if(!RESPONSE_FAILURE.equals(err))
		//	return false;
		
		// if entry has errors and is finding and error is location
		if(e.isFinding() && e.hasErrors() &&  e.getError().toLowerCase().contains("location"))
			return true;
		
		// if an attribute of a finding that has location error
		if( TYPE_ATTRIBUTE.equals(e.getType()) && getProblemConcept() != null && 
			getProblemConcept().getId().equals(e.getFeature().getId()) && !getProblemConcept().equals(e)){
			return isRefineNextAction(getProblemConcept(),err);
		}
		
		return false;
	}
	
	
	/**
	 * generate node events based on input CE & TR pair
	 * @param ce
	 * @param tr
	 */
	private void doNodeEvent(Message clientEvent, TutorResponse tutorResponse, Object input){
		// valid node event, then continue
		if(isValidNodeType(clientEvent)){
			// reset input
			if(RESPONSE_HINT.equals(tutorResponse.getResponse())){
				// if we have a useless level hint, not need to 
				// have an event
				if(tutorResponse.getError().endsWith("Yet"))
					return;
				// now we need to deliver attribute node events appropriately
				//if(input != null && input instanceof ConceptEntry){
				//	input = filterNextStep((ConceptEntry) input);
				//}
			}
			
			// get input entry
			ConceptEntry entry = (input instanceof ConceptEntry)?(ConceptEntry)input:null;
			if(entry == null)
				return;
						
			// initialize NodeEvent
			final NodeEvent nodeEvent = new NodeEvent();
			nodeEvent.setTutorResponse(tutorResponse);
			
			nodeEvent.setType(entry.getType());
			nodeEvent.setLabel(entry.getName());
			nodeEvent.setAction(clientEvent.getAction());
		
			nodeEvent.setResponse(tutorResponse.getResponse());
			nodeEvent.setError(tutorResponse.getError());
			nodeEvent.setCode(tutorResponse.getCode());
			nodeEvent.setObjectDescription(entry.getObjectDescription());
			//nodeEvent.setEntireConcept(clientEvent.getEntireConcept());
			nodeEvent.setInput(entry);
			
			// setup parents
			String parent = clientEvent.getParent();
			if(TextHelper.isEmpty(parent) && TYPE_ATTRIBUTE.equals(entry.getType()))
				parent = entry.getFeature().getObjectDescription();
			// now do a setup
			if(!TextHelper.isEmpty(parent)){
				// remove concept id's from parent as well as switch absent for present finding
				nodeEvent.setParent(parent.replaceAll(TYPE_ABSENT_FINDING,TYPE_FINDING).replaceAll("\\.Concept[\\d]+",""));
			}
			
			// set the absence
			if(TYPE_ABSENT_FINDING.equals(clientEvent.getType())){
				nodeEvent.setAbsent(true);
				nodeEvent.setType(TYPE_FINDING);
			}
			
			// only log entries that don't repeat
			if(TYPE_FINDING.equals(nodeEvent.getType())){
				// if we have a finding that is confirmed
				// check if we mentioned this finding before
				if(RESPONSE_CONFIRM.equals(nodeEvent.getResponse())){
					// we can only add or remove a feature once (unless we have several findings in case)
					if(ACTION_ADDED.equals(nodeEvent.getAction()) || ACTION_REMOVED.equals(nodeEvent.getAction())){
						// determined where this finding is located
						ConceptEntry finding = getFindingAtLocation(entry);
						
						// go over all findings
						for(ConceptEntry e: getConcepts()){
							// if we have same feature, but different ID...
							if(e.getName().equals(entry.getName()) && !e.getId().equals(entry.getId())){
								// check if finding locations are different per case
								// if features have different locations, then we are cool
								if(finding != null){
									// get finding for a different feature
									ConceptEntry altfinding = getFindingAtLocation(e);
									if(altfinding != null){
										// if this is the same located finding, then we don't need this event
										if(finding.equals(altfinding)){
											return;
										}
									}
								}
							}
						}
					}			
				// reset label for next step hints
				}else if(RESPONSE_HINT.equals(nodeEvent.getResponse())){
					expert.resolveConceptEntry(entry);
					nodeEvent.setLabel(entry.getFeature().getName());
					nodeEvent.setAction(RESPONSE_HINT);
				}
			}else if(TYPE_ATTRIBUTE.equals(nodeEvent.getType())){
				// avoid adding repeating attributes
				if(RESPONSE_CONFIRM.equals(nodeEvent.getResponse())){
					// go over all findings
					for(ConceptEntry e: getConcepts()){
						// if we have same attribute, but different ID AND similar FEATURE
						if( e.getName().equals(entry.getName()) && !e.getId().equals(entry.getId()) && 
							e.getFeature().getName().equals(entry.getFeature().getName())){
							return;
						}
					}
				}else if(RESPONSE_HINT.equals(nodeEvent.getResponse())){
					nodeEvent.setAction(RESPONSE_HINT);
				}
			}else if(TYPE_HYPOTHESIS.equals(nodeEvent.getType()) || TYPE_DIAGNOSIS.equals(nodeEvent.getType())){
				// if client event type and node event type don't match, that means that
				// there was a tutor response generated for some new evidence, and sent to correct the status of
				// the Dx or Hx, hence we should not send a node event
				if(!nodeEvent.getType().equals(clientEvent.getType()))
					return;
				
				// if diagnosis is correct, but not supported, it really is a failure
				if(RESPONSE_CONFIRM.equals(nodeEvent.getResponse()) && nodeEvent.getError().equals(ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED)){
					nodeEvent.setResponse(RESPONSE_FAILURE);
				}
			}
			
			// if removed change response
			if(ACTION_REMOVED.equals(nodeEvent.getAction())){
				// check error code
				if(ERROR_REMOVED_CORRECT_CONCEPT.equals(tutorResponse.getError()))
					nodeEvent.setResponse(RESPONSE_FAILURE);
			}
			
			// add this node event to the queue to be processed
			synchronized(nodeEvents){
				nodeEvents.add(nodeEvent);
			}
		
			// start new thread only if the old one is dead
			if(nodeEventThread == null || !nodeEventThread.isAlive()){
				nodeEventThread = new Thread(){
					public void run(){
						List<NodeEvent> events = new ArrayList<NodeEvent>();
						
						// make a copy of the queue
						synchronized(nodeEvents){
							events.addAll(nodeEvents);
						}
						
						do{
							// iterate over messages
							for(NodeEvent e: events){
								doNodeEvent(e);
							}
						
							// now after we've done all of work
							// lets check the queue again
							synchronized(nodeEvents){
								// remove processed events from the queue
								nodeEvents.removeAll(events);
								// create a new (possibley empty) queue w/ the remainder
								events = new ArrayList<NodeEvent>(nodeEvents);
							}
							
						}while(!events.isEmpty());
					}
				};
				nodeEventThread.start();
			}
			
		}
	}
	
	/**
	 * attach one to many and many to many info
	 * @param evt
	 */
	private void doNodeEvent(NodeEvent nodeEvent){
		//long time = System.currentTimeMillis();
		
		// set one to many set
		if(ACTION_ADDED.equals(nodeEvent.getAction())){
			ConceptEntry entry = (ConceptEntry) nodeEvent.getInput();
			// calculate one to many set
			if(TYPE_FINDING.equals(nodeEvent.getType())){
				nodeEvent.setOneToMany(getImpliedHypotheses(entry));
				//nodeEvent.setManyToMany(getImpliedDiagnosisFromFeatures());
			}else if(TYPE_HYPOTHESIS.equals(nodeEvent.getType())){ 
				nodeEvent.setOneToMany(getFeatures(entry));
				//nodeEvent.setManyToMany(getImpliedFeatures(getHypotheses()));
			}else if(TYPE_DIAGNOSIS.equals(nodeEvent.getType())){ 
				nodeEvent.setOneToMany(getFeatures(entry));
				//nodeEvent.setManyToMany(getImpliedFeatures(getDiagnoses()));
			}
		}else if(ACTION_SELF_CHECK.equals(nodeEvent.getAction()) && nodeEvent.getClientEvent() != null){
			nodeEvent.addInput("fok",nodeEvent.getClientEvent().getInputMap().get("fok"));
		}
				
		
		// reset error code
		if(nodeEvent.getTutorResponse() != null){
			nodeEvent.setEntireConcept(nodeEvent.getTutorResponse().getEntireConcept());
			nodeEvent.setCode(nodeEvent.getTutorResponse().getCode());
		}
		
		// output node event
		nodeEvent.setSender(this);
		nodeEvent.setTimestamp(System.currentTimeMillis());
		
		com.sendMessage(nodeEvent);
	}
	
	

	/**
	 * process message requests
	 */
	public void receiveMessage(Message msg) {
		//long time = System.currentTimeMillis();
		if(!(msg instanceof ClientEvent))
			return;
		
		if(TYPE_FINDING.equals(msg.getType()))
			doProcessFinding(msg);
		else if(TYPE_ABSENT_FINDING.equals(msg.getType()))
			doProcessAbsentFinding(msg);
		else if(TYPE_ATTRIBUTE.equals(msg.getType()))
			doProcessAttribute(msg);
		else if(TYPE_HYPOTHESIS.equals(msg.getType()))
			doProcessHypothesis(msg);
		else if(TYPE_DIAGNOSIS.equals(msg.getType()))
			doProcessDiagnosis(msg);
		else if(TYPE_SUPPORT_LINK.equals(msg.getType()))
			doProcessSupportLink(msg);
		else if(TYPE_REFUTE_LINK.equals(msg.getType()))
			doProcessRefuteLink(msg);
		else if(TYPE_HINT.equals(msg.getType()))
			doProcessHint(msg);
		else if(TYPE_DONE.equals(msg.getType()))
			doDone(msg);
		else if(TYPE_HINT_LEVEL.equals(msg.getType()))
			doProcessHintLevel(msg);
		else if(TYPE_INFO.equals(msg.getType()))
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		else if(TYPE_PRESENTATION.equals(msg.getType())){
			if(ACTION_IMAGE_CHANGE.equals(msg.getAction())){
				currentImage = msg.getLabel();
			}else if(ACTION_VIEW_CHANGE.equals(msg.getAction())){
				Map map = (Map) msg.getInput();
				int x = Integer.parseInt(""+map.get("x"));
				int y = Integer.parseInt(""+map.get("y"));
				int w = Integer.parseInt(""+map.get("width"));
				int h = Integer.parseInt(""+map.get("height"));
				double s = Double.parseDouble(""+map.get("scale"));
				currentView = new ViewPosition(x,y,w,h,s);	
				sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
			}
		}else if(TYPE_QUESTION.equals(msg.getType())){
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// print time info
		//if(debug){
		//	System.out.println("process time "+(System.currentTimeMillis()-time)+" ms");
		//}
	}
	
	/**
	 * check that concept is in correct location
	 * @param entry
	 * @param input
	 * @return
	 */
	private boolean isCorrectLocation(ConceptEntry entry, Object input){
		if(input instanceof Map){
			Map map = (Map) input;
			if(map.containsKey("location")){
				String text = ""+((Map)input).get("location");
				Rectangle r = TextHelper.parseRectangle(text);
				// is input located in the location
				for(ShapeEntry e: problem.getLocations(entry,currentImage)){
					if(e.getShape().contains(r) || e.getShape().intersects(r))
						return true;
				}
				return false;
			}else if(map.containsKey("view")){
				String text = ""+((Map)input).get("view");
				Rectangle r = TextHelper.parseRectangle(text);
				// is input located in the location
				for(ShapeEntry e: problem.getLocations(entry,currentImage)){
					if(r.contains(e.getShape().getBounds()) || e.getShape().intersects(r))
						return true;
				}
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * reseuest done
	 * @param msg
	 */
	private void doDone(Message msg){
		long time = System.currentTimeMillis();
		// are we requesting from 
		
		
		
		// this is normal operation
		if(isSolved()){
			sendTutorResponse(msg,RESPONSE_CONFIRM,HINT_CASE_SOLVED);
		}else{
			// figure out why
			// are there diagnosis identified?
			if(checkConceptType(TYPE_DIAGNOSIS)){
				ConceptEntry e = getNextConcept();
				// if next concept is null WTF?, but I guess we are done
				if(e == null){
					//ConceptEntry p = getProblemConcept();
					ErrorEntry err = popError();
					if(err != null){
						sendTutorResponse(msg,RESPONSE_FAILURE,err.getError(),err.getConceptEntry());
					}else{
						sendTutorResponse(msg,RESPONSE_CONFIRM,HINT_CASE_SOLVED);
					}
					return;
				}
				
				// else notify 
				if(TYPE_FINDING.equals(e.getType()) || TYPE_ABSENT_FINDING.equals(e.getType())){
					boolean refine = false;
					for(ConceptEntry c: getConcepts())
						if(isSubClassOf(e,c.getParentEntry())){
							refine = true;
							break;
						}
					// if refine then different error message
					if(refine)
						sendTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_NEEDS_REFINED_FINDINGS);
					else		
						sendTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_HAS_MORE_FINDINGS_TO_CONSIDER);
				}else{
					sendTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_HAS_MORE_DIAGNOSES_TO_CONSIDER);
				}
			}else{
				sendTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_NEEDS_DIAGNOSIS_TO_FINISH);
			}
		}
		
		// show state
		debugShowState(time);
	}
	
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessHintLevel(Message msg){
		// get problem concept
		sendTutorResponse(msg,RESPONSE_HINT,ERROR_OK);
	}
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessHint(Message msg){
		long time = System.currentTimeMillis();
		
		// get problem concept
		ErrorEntry errorEntry = popError();
		
		//if(entry != null && entry.hasErrors()){
		if(errorEntry != null){
			sendTutorResponse(msg,RESPONSE_HINT,errorEntry.getError(),errorEntry.getConceptEntry());
		}else if(isSolved()){
			sendTutorResponse(msg,RESPONSE_HINT,HINT_CASE_SOLVED);
		}else{
			ConceptEntry entry = getNextConcept();
			
			// remember which finding was recomended by a level hint
			suggestedFinding = entry;
			
			// correct type of diagnostic concept
			// if hypothesis is not in correct concept list yet
			String type = entry.getType();
			if( type.equals(TYPE_DIAGNOSIS) && !checkConcept(new ConceptEntry(entry.getName(),TYPE_HYPOTHESIS))){
				type = TYPE_HYPOTHESIS;
			}
			
			
			// offer hint for specific finding
			if(TYPE_FINDING.equals(type)){
				String err = HINT_MISSING_FINDING;
				if(isLevelHint(TYPE_FINDING)){
					err = HINT_NO_FINDINGS_YET;
				}else{
					ConceptEntry s = getSimilarFinding(entry);
					if(s != null){
						// if similar finding is a feature, then we are missing an attribute
						if(isSubClassOf(entry,s)){
							// resolve the sibling
							expert.resolveConceptEntry(s);
							// if siblings feature is identical to it, then this is a first attribute
							// that needs to be added
							if(s.getFeature().equals(s)){
								err = HINT_MISSING_ATTRIBUTE;
							}else{
								err = HINT_MISSING_ANOTHER_ATTRIBUTE;
							}
						}else{
							// else it could be another feature
							err = HINT_MISSING_SIMILAR_FINDING;
							entry.getTagMap().put(TAG_SIBLING,UIHelper.getTextFromName(s.getName()));
						}
					}
				}
				sendTutorResponse(msg,RESPONSE_HINT,err,entry);
			}else if(TYPE_ABSENT_FINDING.equals(type)){
				String err = HINT_MISSING_ABSENT_FINDING;
				if(isLevelHint(TYPE_FINDING)){
					err = HINT_NO_FINDINGS_YET;
				}else{
					ConceptEntry s = getSimilarFinding(entry);
					if(s != null){
						// if similar finding is a feature, then we are missing an attribute
						if(isSubClassOf(entry,s)){
							// resolve the sibling
							expert.resolveConceptEntry(s);
							// if siblings feature is identical to it, then this is a first attribute
							// that needs to be added
							if(s.getFeature().equals(s)){
								err = HINT_MISSING_ATTRIBUTE;
							}else{
								err = HINT_MISSING_ANOTHER_ATTRIBUTE;
							}
						}else{
							// else it could be another feature
							err = HINT_MISSING_SIMILAR_ABSENT_FINDING;
							entry.getTagMap().put(TAG_SIBLING,UIHelper.getTextFromName(s.getName()));
						}
					}
				}
				sendTutorResponse(msg,RESPONSE_HINT,err,entry);
			}else if(TYPE_HYPOTHESIS.equals(type)){
				entry.getTagMap().put(TAG_FINDINGS,getDiseaseRule(entry));
				sendTutorResponse(msg,RESPONSE_HINT,
						(isLevelHint(TYPE_HYPOTHESIS))?HINT_NO_HYPOTHESES_YET:HINT_MISSING_HYPOTHESIS,entry);
			}else if(TYPE_DIAGNOSIS.equals(type)){
				entry.getTagMap().put(TAG_FINDINGS,getDiseaseRule(entry));
				sendTutorResponse(msg,RESPONSE_HINT,(isLevelHint(TYPE_DIAGNOSIS))?HINT_NO_DIAGNOSES_YET:HINT_MISSING_DIAGNOSIS,entry);
			}
		}
		
		// show state
		debugShowState(time);
	}
	
	/**
	 * has concept of given type been identified?
	 * @param type
	 * @return
	 */
	private boolean isLevelHint(String type){
		if(levelHints == null)
			levelHints = new HashSet<String>();
		
		// don't repeat level hints
		if(levelHints.contains(type))
			return false;
		
		// save level hint
		levelHints.add(type);
		
		// see if there is a need for a level hint
		return !checkConceptType(type);	
	}
	
	
	/**
	 * is there a similar finding in case that was already asserted?
	 * @param finding
	 * @return
	 */
	private ConceptEntry getSimilarFinding(ConceptEntry finding){
		// try to detect full findings
		IClass feature = getFeature(getConceptClass(finding,ontology));
		for(ConceptEntry e: getConcepts()){
			// if this is an entire finding, then ...
			//if(e.isFinding() && e.equals(e.getParentEntry())){
			if(e.isParentFinding()){
				IClass p = getConceptClass(e.getFeature(), ontology);
				if(p != null && p.equals(feature))
					return e;
			}
		}
		return null;
	}
	
	
	
	/**
	 * get a list of attributes mentioned by user for a given feature
	 * @param feature
	 * @return
	 */
	private List<ConceptEntry> getAttributes(ConceptEntry feature){
		List<ConceptEntry> attributes = new ArrayList<ConceptEntry>();
		for(ConceptEntry a: getConcepts()){
			if(TYPE_ATTRIBUTE.equals(a.getType()) && 
			    a.getFeature().getId().equals(feature.getId())){
				attributes.add(a);
			}
		}
		// sort attributes by id
		Collections.sort(attributes,new Comparator<ConceptEntry>(){
			public int compare(ConceptEntry o1, ConceptEntry o2) {
				return o1.getId().compareTo(o2.getId());
			}
			
		});
		
		return attributes;
	}
	
	
	/**
	 * remove concept entry
	 * @param msg
	 * @param entry
	 */
	private void doRemoveConcept(Message msg, ConceptEntry entry){
		// do not remove something that is not there
		if(!registry.containsKey(entry.getId())){
			// send message that OKs the remove regardless
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,entry);
			return;
		}
		
		// remember the concept status of a concept being removed
		int status = registry.get(entry.getId()).getConceptStatus();
		
		
		// remove as a problem or correct feature
		if(TYPE_ATTRIBUTE.equals(entry.getType())){
			
			// refine finding
			ConceptEntry parent = entry.getParentEntry();
			
			// get a new list of attributes from the case
			List<ConceptEntry> attributes = getAttributes(entry.getFeature());
			attributes.remove(entry);
			
			ConceptEntry child  = createFinding(entry.getFeature(),attributes,ontology);
			
			// if the new finding minus a given attribute returns the identical finding as an original
			// that means that attributes are inter linked and remaining attributes are only relevan
			// in the context of another. Since it is difficult to determine which attributes are related
			// and which are not, just remove all of the remaining attributes
			if(child == null || parent.equals(child) || child.equals(entry.getFeature())){
				child = entry.getFeature();
			}
			
			// now do refine finding
			// place new concept on top of old concept
			parent.copyTo(child);
				
			// remove  old concept from registry as well as its parent
			unregisterConceptEntry(entry);
			unregisterConceptEntry(parent);
		
			// if there is already such a child copy back the status
			if(registry.containsKey(child.getId())){
				child.setConceptStatus(registry.get(child.getId()).getConceptStatus());
			}
			
			// add new concept to registry	
			registerConceptEntry(child);
			
			// set new parent entry for attribute (could be used later Ex: updateLinks code
			entry.setParentEntry(child);
	
			// should we prompt that this finding is incomplete?
			// add the fact that another attribute is missing
			List<ConceptEntry> findings = null;
			try {
				findings = getMatchingFindings(child);
			} catch (Exception e) {}
			
			if(findings != null && !findings.isEmpty()){
				// well if the list of findings is in fact our child
				// then we are complete
				if(!(child.equals(findings.get(0)) || isSubClassOf(child,findings.get(0)))){
					// ok we need to specify further
					ErrorEntry err = new ErrorEntry(child.equals(entry.getFeature())?HINT_MISSING_ATTRIBUTE:HINT_MISSING_ANOTHER_ATTRIBUTE,child);
					err.setFindings(findings);
					err.setPriority(1);
					pushError(err);
				}
			}
							
		}else if(entry.isFinding()){
			unregisterConceptEntry(entry);
			
			//if concept is a finding, remove everything related to it
			for(ConceptEntry e: new ArrayList<ConceptEntry>(getConcepts())){
				if(entry.getId().equals(e.getFeature().getId()))
					unregisterConceptEntry(e);
			}
		}else{
			// else just unregister the thing
			unregisterConceptEntry(entry);
		}
		
		// decide which error are we going to report
		String err = (status == ConceptEntry.CORRECT)?ERROR_REMOVED_CORRECT_CONCEPT:ERROR_OK;
		
		// send message
		sendTutorResponse(msg,RESPONSE_CONFIRM,err,entry);
	}
	
	/**
	 * check links associated w/ this finding
	 * @param e
	 */
	private void updateFindingLinks(Message msg,ConceptEntry attribute){
		// this won't work if parent or finding are not there
		if(attribute == null)
			return;
		
		// check correct links first
		List<ConceptEntry> errorDelta = new ArrayList<ConceptEntry>();
		List<ConceptEntry> correctDelta = new ArrayList<ConceptEntry>();
		//ConceptEntry parent = null;
		
		// check correct and incorrect links for changes
		for(ConceptEntry en: getConcepts()){
			if(en instanceof LinkConceptEntry){
				LinkConceptEntry link = (LinkConceptEntry) en;
				if(link.getSourceConcept().getFeature().getId().equals(attribute.getFeature().getId())){
					//parent = link.getSourceConcept();
					link.setSourceConcept(attribute.getParentEntry());
					
					// check this link again
					if(TYPE_SUPPORT_LINK.equals(link.getType())){
						boolean result = testSupportLink(link);
						// if link is cool, but was not before
						if(result && link.hasErrors()){
							correctDelta.add(link);
						// if link is bad, but was cool
						}else if(!result && !link.hasErrors()){
							errorDelta.add(link);
						}
					}else if(TYPE_REFUTE_LINK.equals(link.getType())){
						// if link is no longer supported, oh well
						boolean result = testRefuteLink(link);
						// if link is cool, but was not before
						if(result && link.hasErrors()){
							correctDelta.add(link);
						// if link is bad, but was cool
						}else if(!result && !link.hasErrors()){
							errorDelta.add(link);
						}
					}
				}
			}
		}
		
		// resolve errors
		for(ConceptEntry link: correctDelta){
			resolveError(link,TYPE_SUPPORT_LINK.equals(link.getType())?
				ERROR_LINK_FINDING_NO_LONGER_SUPPORTS_HYPOTHESIS:ERROR_LINK_FINDING_NO_LONGER_REFUTES_HYPOTHESIS);
		}
		
		// handle incorrect first
		for(ConceptEntry e: errorDelta){
			if(TYPE_SUPPORT_LINK.equals(e.getType())){
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_LINK_FINDING_NO_LONGER_SUPPORTS_HYPOTHESIS,e);
			}else if(TYPE_REFUTE_LINK.equals(e.getType())){
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_LINK_FINDING_NO_LONGER_REFUTES_HYPOTHESIS,e);
			}
		}
		
		// handle correct
		for(ConceptEntry e: correctDelta){
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,e);
		}
	}
	
	
	/**
	 * check wheather some modification made hypothesis or diagnosis consistent or inconsistent
	 *
	 */
	private void revalidateHxAndDx(Message m){
		// if we have an an automaticly added finding, perhaps we should not revalidated diagnosis
		if(m.isAuto() && getNextConcept() != null && getNextConcept().isFinding())
			return;
		
		// wait for previous thread
		if(revalidateThread != null && revalidateThread.isAlive()){
			// interrupt existing thread, wait for end, create new thread
			//long time = System.currentTimeMillis();
			interruptReasoning = true;
			try{
				//revalidateThread.stop();
				revalidateThread.join();
			}catch(Exception ex){}
			clearImpliedHxAndDx();
			//System.out.println("Wait to finish "+(System.currentTimeMillis()-time));
		}
		final Message msg = m;
		
		// initialize validate thread
		interruptReasoning = false;
		revalidateThread = new Thread(){
				public void run(){
					//long time = System.currentTimeMillis();
					
					// if no evidence, nothing needs to be done
					if(!checkFindings())
						return;
					
					// look at implied hx and dx
					getImpliedHypotheses();
					getImpliedDiagnoses();
					
					if(interruptReasoning)
						return;
					
					// recalculate implied hx and dx
					
					// check for consistency first
					List<ConceptEntry> errorDelta = new ArrayList<ConceptEntry>();
					List<ConceptEntry> correctDelta = new ArrayList<ConceptEntry>();
				
					// for all concepts
					for(ConceptEntry e: getConcepts()){
						if(TYPE_HYPOTHESIS.equals(e.getType())){
							if(getImpliedHypotheses().contains(e)){
								if(e.hasErrors() && e.getConceptStatus() != ConceptEntry.CORRECT)
									correctDelta.add(e);
							}else{
								if(!e.hasErrors())
									errorDelta.add(e);
									
							}
						}else if(TYPE_DIAGNOSIS.equals(e.getType())){
							if(getImpliedDiagnoses().contains(e)){
								if(e.hasErrors() && e.getConceptStatus() != ConceptEntry.CORRECT)
									correctDelta.add(e);
							}else{
								// if previous error was in regard to 
								if(!e.hasErrors() || ERROR_DIAGNOSIS_NO_FINDINGS.equals(e.getError()))
									errorDelta.add(e);
									
							}
						}
					}
					
					// notify tutor of change
					for(ConceptEntry e: correctDelta){
						String error = (TYPE_HYPOTHESIS.equals(e.getType()))?ERROR_HYPOTHESIS_BECAME_INCONSISTANT:ERROR_DIAGNOSIS_BECAME_INCONSISTANT;
						resolveError(e, error);
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,e);
					}
					
					
					// notify tutor of change
					for(ConceptEntry e: errorDelta){
						String response = RESPONSE_FAILURE;
						String error = (TYPE_HYPOTHESIS.equals(e.getType()))?ERROR_HYPOTHESIS_BECAME_INCONSISTANT:ERROR_DIAGNOSIS_BECAME_INCONSISTANT;
					
						// make sure this is not a corect dx, but not implied
						if(checkConceptInCase(DIAGNOSES,e)){
							response = RESPONSE_CONFIRM;
							
							// if more specific then needs to be
							if(problem.getConcepts(DIAGNOSES).containsKey(e.getName()))
								error = ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED;
							else
								error = ERROR_DIAGNOSIS_IS_TOO_SPECIFIC;
						}
						
						// add a list of inconsitent findings
						if(RESPONSE_FAILURE.equals(response))
							e.getTagMap().put(TAG_FINDINGS,getInconsistantFindings(e));
						
						sendTutorResponse(msg,response,error,e);
					}
					
					// print out the implications
					if(showImpliedDx){
						System.out.println("\n-- Implied Diagnoses --");
						System.out.println((allHxMode)?getImpliedHypotheses():getImpliedDiagnoses()+"\n");
					}
					if(stateExplorer != null && debug.isSelected()){
						stateExplorer.setContent("Implied Dx",(allHxMode)?getImpliedHypotheses():getImpliedDiagnoses());
						stateExplorer.refresh();
					}
					
					// process node events when done
					/*
					(new Thread(){
						public void run(){
							synchronized(nodeEvents){
								while(!nodeEvents.isEmpty()){
									doNodeEvent(nodeEvents.removeFirst());
								}
							}
						}
					}).start();
					*/
					
				}
		};
		revalidateThread.start();
	}
	
	/**
	 * get matching finding at given location associated with a candidate
	 * @param name
	 * @return
	 */
	private ConceptEntry getFindingAtLocation(ConceptEntry entry){
		// determined where this finding is located
		List<ConceptEntry> findings = OntologyHelper.getMatchingFindings(problem,entry);
		ConceptEntry finding = null;
		/*
		try {
			findings = getMatchingFindings(entry);
		} catch (Exception e1) {}
		*/
		
		// see if there is ambiguity
		if(findings != null && findings.size() > 1){
			for(ConceptEntry f : findings){
				if(isCorrectLocation(f,findingInputMap.get(entry.getFeature().getObjectDescription()))){
					finding = f;
					break;
				}
			}
		}else if(!findings.isEmpty()){
			finding = findings.get(0);
		}
		
		return finding;
	}
	
	
	
	
	/**
	 * get findings from case that share a feature with the parameter
	 * @param name
	 * @return
	 */
	private List<ConceptEntry> getMatchingFindings(ConceptEntry e) throws Exception{
		// can't parse description
		if(e == null)
			throw new FindingIsNullException();
		
		// find class for candidate
		IClass candidate = getConceptClass(e,ontology);
		IClass cfeature = getFeature(candidate);
		boolean sameF1 = cfeature.equals(candidate);
		
		// if candidate is still not there, we are fucked
		if(candidate == null)
			throw new FindingNotInDomainException();
		
		//now iterate over classes in case
		List<ConceptEntry> findings = new ArrayList<ConceptEntry>();
		boolean findingTooGeneral = false;
		for(String category: new String []{DIAGNOSTIC_FEATURES,CLINICAL_FEATURES}){
			for(ConceptEntry entry: problem.getConcepts(category).getValues()){
				IClass c = ontology.getClass(entry.getName());
				
				// if candidate is a child of case concept's feature ...
				IClass f = getFeature(c);
				
				// check if the candidate is way too general	
				if(candidate.hasSubClass(f)){
					findingTooGeneral = true;
				}else if(f.equals(candidate) || f.hasSubClass(candidate)){
					boolean sameF2 = f.equals(c);
					// we know that candidate is a child of feature
					// just make sure that features are in fact shared
					
					// we also need to add a finding we are talking about general/specific features, not findings
					if(cfeature.equals(f) || (sameF1 && sameF2))
						findings.add(entry);
				}
			}
		}
		
		// do second pass to see if we can get more precise
		if(findings.size() > 1){
			List<ConceptEntry> pfindings = new ArrayList<ConceptEntry>();
			for(ConceptEntry entry: new ArrayList<ConceptEntry>(findings)){
				IClass c = ontology.getClass(entry.getName());
				
				// now will candidate match to finding
				if(c.equals(candidate) || candidate.hasSubClass(c) || candidate.hasSuperClass(c))
					pfindings.add(entry);
			}
			
			// now reassign findings
			if(!pfindings.isEmpty())
				findings = pfindings;
			
		}
		
		
		// if we have empty findings list and tooGeneral flag
		// then throw exception, the problem w/ throwing exception right away
		// is that the matching algorithm may consider throwing this exception even
		// if there is another finding that matches it, just fine.
		// that is why it is better to do the matching, and then to notify.
		if(findings.isEmpty() && findingTooGeneral)
			throw new FindingIsTooGeneralException();
		
		return findings;
	}

	/**
	 * add concept entry to the state of the problem
	 * @param entry
	 */
	private void addConceptInstance(ConceptEntry e){
		// in reallity we care about findings
		ConceptEntry entry = e.getParentEntry();
		if(entry == null)
			entry = e;
		
		// fix empty parts
		if(entry.getParts().isEmpty())
			entry.getParts().addAll(problem.getParts());
		
		// determine the property
		IClass c = getConceptClass(entry, ontology);
		String prop = HAS_FINDING;
		if(entry.isAbsent())
			prop = HAS_NO_FINDING;
		else if(isClinicalFeature(c))
			prop = HAS_CLINICAL;
		else if(isAncillaryStudy(c))
			prop = HAS_ANCILLARY;
		
		// add to all involved parts
		for(String part : entry.getParts()){
			addFinding(entry,instances.get(part),prop);
		}
	}

	/**
	 * add concept entry to the state of the problem
	 * @param entry
	 */
	private boolean hasConceptInstance(ConceptEntry e){
		// in reallity we care about findings
		ConceptEntry entry = e.getParentEntry();
		if(entry == null)
			entry = e;
		
		// fix empty parts
		if(entry.getParts().isEmpty())
			entry.getParts().addAll(problem.getParts());
		
		// determine the property
		IClass c = getConceptClass(entry, ontology);
		String prop = HAS_FINDING;
		if(entry.isAbsent())
			prop = HAS_NO_FINDING;
		else if(isClinicalFeature(c))
			prop = HAS_CLINICAL;
		else if(isAncillaryStudy(c))
			prop = HAS_ANCILLARY;
		
		// add to all involved parts
		for(String part : entry.getParts()){
			if(hasFinding(entry,instances.get(part),prop))
				return true;
		}
		return false;
	}
	
	
	/**
	 * remove concept entry 
	 * @param entry
	 */
	private void removeConceptInstance(ConceptEntry e){
		// in reallity we care about findings
		ConceptEntry entry = e.getParentEntry();
		if(entry == null)
			entry = e;

		// if removing an attribute, then we need to refine it, really
		ConceptEntry previous = null;
		if(TYPE_ATTRIBUTE.equals(e.getType())){
			//expert.resolveConceptEntry(entry);
			//List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(entry.getAttributes());
			List<ConceptEntry> attributes = getAttributes(entry.getFeature());
			attributes.remove(e);
			previous = createFinding(entry.getFeature(),attributes,ontology);
			
			// if the new finding minus a given attribute returns the identical finding as an original
			// that means that attributes are inter linked and remaining attributes are only relevan
			// in the context of another. Since it is difficult to determine which attributes are related
			// and which are not, just remove all of the remaining attributes
			if(entry.getParentEntry().equals(previous)){
				previous = entry.getFeature();
			}
			// change the finding
			// WHY DIDI I DO IT????
			//e.setParentEntry(previous);
		}
		
		// fix empty parts
		if(entry.getParts().isEmpty())
			entry.getParts().addAll(problem.getParts());
		
		// determine the property
		IClass c = getConceptClass(entry, ontology);
		String prop = HAS_FINDING;
		if(entry.isAbsent())
			prop = HAS_NO_FINDING;
		else if(isClinicalFeature(c))
			prop = HAS_CLINICAL;
		else if(isAncillaryStudy(c))
			prop = HAS_ANCILLARY;
		
		// add to all involved parts
		for(String part : entry.getParts()){
			// if remove was successfull, then re-add more general finding
			if(removeFinding(entry,instances.get(part),prop)){
				// re-add a more general finding if available
				if(previous != null)
					addFinding(previous,instances.get(part),prop);
			}else{
				// try to remove finding itself not just parent finding, in 
				// case that info is no longer correct
				removeFinding(e,instances.get(part),prop);
			}
		}	
	}
	
	
	
	/**
	 * add finding to case
	 * @param e
	 */
	private void addFinding(ConceptEntry finding, IInstance instance, String property){
		IClass cls = getConceptClass(finding,ontology);
		// shit, can't make it further
		if(cls == null)
			return;
		// got it lets make an instance			
		IInstance inst = getInstance(cls);
		IProperty p = ontology.getProperty(property);
		// don't add something that you don't need
		if(!instance.hasPropetyValue(p,inst)){
			// check for subsumption first
			for(Object o: instance.getPropertyValues(p)){
				if(o instanceof IInstance){
					IInstance i = (IInstance) o;
					// if a finding that is in instance is more general then
					// new finding, then remove it, since we are adding a more
					// specific finding
					if(i.getDirectTypes()[0].hasSubClass(cls)){
						instance.removePropertyValue(p,i);
						break;
					}
				}
			}
			instance.addPropertyValue(p,inst);
		}
	}
	
	/**
	 * add finding to case
	 * @param e
	 */
	private boolean hasFinding(ConceptEntry finding, IInstance instance, String property){
		IClass cls = getConceptClass(finding,ontology);
		
		// shit, can't make it further
		if(cls == null)
			return false;
		
		for(Object val: instance.getPropertyValues(ontology.getProperty(property))){
			if(val instanceof IInstance){
				IInstance inst = (IInstance) val;
				if(inst.hasType(cls))
					return true;
			}
		}
		return false;
	}
	
	
	/**
	 * add finding to case
	 * @param e
	 */
	private boolean removeFinding(ConceptEntry finding, IInstance instance, String property){
		String nm = finding.getName();
		IClass cls = ontology.getClass(nm);
		// if entry is fucked up, we may need to do something extra
		if(cls == null && nm.indexOf(",") > -1){
			cls = ontology.getClass(nm.substring(0,nm.indexOf(",")).trim());
		}
		// shit, can't make it further
		if(cls == null)
			return false;
		// got it lets make an instance			
		IInstance inst = getInstance(cls);
		IProperty p = ontology.getProperty(property);
		
		// don't add something that you don't need
		if(instance.hasPropetyValue(p,inst)){
			instance.removePropertyValue(p,inst);
			return true;
		}else{
			// we can have a scenario when we want to remove a node
			// that is more specific that is already in case
			// this happens when finding that has multiple attributes gets refined
			for(Object o : instance.getPropertyValues(p)){
				if(o instanceof IInstance){
					IInstance i = (IInstance) o;
					// if we find a finding that is more specific then what we want to remove
					if(i.getDirectTypes().length > 0 && cls.hasSuperClass(i.getDirectTypes()[0])){
						// make sure that finding is not a feature
						IClass fn = i.getDirectTypes()[0];
						IClass f = getFeature(fn);
						if(!fn.equals(f)){
							instance.removePropertyValue(p,i);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	
	
	/**
	 * used for debugging
	 */
	private void debugShowState(long time){
		/*
		if(debug && instances != null){
			resolveErrors();
			for(IInstance instance: instances.values()){
				System.out.println("\n--- instance ["+instance.getName()+"]---");
				System.out.println("clinical:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_CLINICAL))));
				System.out.println("findings:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_FINDING))));
				System.out.println("no findings:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_NO_FINDING))));
			}
			//System.out.println("correct concepts:\t"+getCorrectConcepts());
			//System.out.println("problem concepts:\t"+getProblemConcepts());
			//System.out.println("implied hypotheses:\t"+getImpliedHypotheses());
			System.out.println("mentioned concepts:\t"+registry);
			System.out.println("problem stack:\t"+problemStack);
			System.out.println("--- ("+(System.currentTimeMillis()-time)+" ms)---\n");
		}
		*/
		
		
		if(debug != null && debug.isSelected()){
			resolveErrors();
			resolveGoals();
			
			if(instances != null){
				for(IInstance instance: instances.values()){
					System.out.println("\n--- instance ["+instance.getName()+"]---");
					System.out.println("clinical:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_CLINICAL))));
					System.out.println("findings:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_FINDING))));
					System.out.println("no findings:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_NO_FINDING))));
					System.out.println("----\n");
				}
				
			}
			
			// initialize state explorer
			if(stateExplorer == null){
				List<ConceptEntry> goals = new ArrayList<ConceptEntry>();
				goals.addAll(problem.getConcepts(DIAGNOSTIC_FEATURES).getValues());
				goals.addAll(problem.getConcepts(DIAGNOSES).getValues());
				stateExplorer = new ReasonerStateExplorer();
				stateExplorer.setContent("User",registry);
				stateExplorer.setContent("Errors",problemStack);
				stateExplorer.setContent("Implied Dx",new ArrayList());
				stateExplorer.setContent("Goals",goals);
				stateExplorer.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						debug.setSelected(false);
					}
				});
			}
			
			// show if necessary
			if(!stateExplorer.isShowing())
				stateExplorer.showFrame();
			stateExplorer.refresh();
			
			//System.out.println("reasoner concept registry:\t"+registry);
			//System.out.println("reasoner problem stack:   \t"+problemStack);
			//System.out.println("incomplete findings:  \t\t"+incompleteFindings+"\n--");
		}else if(stateExplorer != null){
			stateExplorer.hideFrame();
		}
		
	}
	
	private void resolveGoals(){
		for(ConceptEntry e : problem.getReportFindings()){
			checkConcept(e);
		}
	}
	
	/**
	 * get concept entry with given name of type
	 * @param name
	 * @return
	 */
	private boolean checkConceptType(String type){
		for(ConceptEntry entry: getConcepts())
			if(entry.getType().equals(type))
				return true;
		return false;
	}
	
	
	/**
	 * clear implied states of Hx and Dx
	 */
	private void clearImpliedHxAndDx(){
		impliedDx = impliedHx = null;
	}
	
	/**
	 * is observed at too low power
	 * @param e
	 * @return
	 */
	private boolean isTooLowPower(ConceptEntry e, Object input){
		if(input instanceof Map){
			// get observed power
			String observedPower = (String) ((Map)input).get("power");
			if(observedPower != null){
				expert.resolveConceptEntry(e);
				return TextHelper.isSmallerPower(observedPower,e.getPower());
			}
		}
		return false;
	}

	
	
	
	/**
	 * is one concept entry subclass of another
	 */
	private boolean isSubClassOf(ConceptEntry child, ConceptEntry parent){
		IClass c = getConceptClass(child, ontology);
		IClass p = getConceptClass(parent, ontology);
		if(c ==null || p == null)
			return false;
		return p.hasSubClass(c);
	}
	
	
	
	/**
	 * parse concept entry from message
	 */
	private ConceptEntry getConceptEntry(Message msg){
		ConceptEntry candidate = ConceptEntry.getConceptEntry(msg.getObjectDescription());
		
		// if parent is available and not set by previous call, then set it manually
		if(msg.getParent() != null && candidate.equals(candidate.getFeature()))
			candidate.setFeature(ConceptEntry.getConceptEntry(msg.getParent()));
		
		// check for entire concept
		if(msg.getEntireConcept() != null){
			ConceptEntry parent = ConceptEntry.getConceptEntry(msg.getEntireConcept());
			parent.setFeature(candidate.getFeature());
			candidate.setParentEntry(parent);
		}
		return candidate;
	}
	
	
	
	
	
	/**
	 * process attribute message
	 * @param msg
	 */
	private void doProcessAttribute(Message msg){
		long time = System.currentTimeMillis();
		
		String action = msg.getAction();
		ConceptEntry candidate = getConceptEntry(msg);
		// shit, can't parse message, this should never happen
		if(candidate == null){
			sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_NO_SUCH_CONCEPT_IN_DOMAIN);
			return;
		}
		
		// clear implied state
		clearImpliedHxAndDx();
		
		if(ACTION_ADDED.equals(action)){
			// register hypothesis
			registerConceptEntry(candidate);
			
			// get all findings in case that have the same feature
			List<ConceptEntry> findings = Collections.EMPTY_LIST;
			try{
				findings = getMatchingFindings(candidate.getParentEntry()); 
			}catch(Exception ex){
				// behaviour should not changes in this case????
			}
			
			// iterate through candidates
			boolean correctAttribute = false;
			boolean irrelevantAttribute = false;
			boolean completeFinding = false;
			boolean invalidAttribute = false;
			boolean invalidAttributeSet = false;
			boolean incorrectLocation = false;
			String incorrectValueAttributeName = null;
			boolean findingIsAbsent = true;
			boolean findingIsImportant = false;
			
			ConceptEntry finding = null;
			for(ConceptEntry e: findings){
				// resolve each finding
				expert.resolveConceptEntry(e);
				
				// see if the more specific finding is absent and/or important
				findingIsAbsent &= e.isAbsent();
				findingIsImportant |= e.isImportant();
				
				// check for completness
				completeFinding |= e.equals(candidate.getParentEntry()) || isSubClassOf(candidate.getParentEntry(),e);
				
				// if candidate is in a list of correct findings attributes u r good
				if(e.getAttributes().contains(candidate)){
					correctAttribute = true;
				}else if(isSubClassOf(candidate.getParentEntry(),e)){
					irrelevantAttribute = true;
				}else if(!e.getPotentialAttributes().contains(candidate)){
					invalidAttribute = true;
				}
				
				// check for invalid set
				String name = candidate.getParentEntry().getName(); 
				int x = name.indexOf(","); 
				if(x > -1 && name.indexOf(candidate.getName(),x) > -1){
					invalidAttributeSet = true;
				}else if(!correctAttribute){
					incorrectValueAttributeName = getAttributeName(candidate,e.getAttributes(),ontology);
				}
				
				// pick right finding
				if(correctAttribute || isSubClassOf(e,candidate.getParentEntry())){
					finding = e;
					break;
				}
			}
			
			// if we have a finding that matches from case, then check location etc...
			// if not, then that means that the feature itself is wrong, hence the
			// attribute of wrong feature is correct :)
			if(finding != null && !isCorrectLocation(finding,findingInputMap.get(msg.getParent()))){
				// check location
				incorrectLocation = true;
				// if attribute location is incorrect, then check feature location as well
				// if feature location is also incorrect, it makes no sense to report on it twice
				if(!isCorrectLocation(finding.getFeature(),findingInputMap.get(msg.getParent())))
					incorrectLocation = false;
			}
			
						
			// add finding IF at least there is a parent in case, else it might be irrelevant
			if(findings.isEmpty() && candidate.getFeature().isAbsent()){
				irrelevantAttribute = true;
			}
			
			// get feature from registry
			ConceptEntry feature = registry.get(candidate.getFeature().getId());
			
			// add to problem state
			if(!irrelevantAttribute && (feature == null || feature.isImportant()))
				addConceptInstance(candidate);	
			
			// add the fact that another attribute is missing
			if(!completeFinding && feature != null && feature.isImportant()){
				ErrorEntry err = new ErrorEntry(HINT_MISSING_ANOTHER_ATTRIBUTE,candidate.getParentEntry());
				err.setFindings(findings);
				err.setPriority(1);
				pushError(err);
			}
					
			// adding attribute may change some responses to a finding, lets handle those
			// only location should determine the importance of the feature, not attributes
			/*
			if(findings.size() > 0 && !findingIsImportant && (feature != null && feature.isImportant())){
				com.sendMessage(getTutorResponse(msg,RESPONSE_CONFIRM,ERROR_FINDING_NOT_IMPORTANT,candidate.getParentEntry()));
				removeConceptInstance(candidate.getParentEntry());
				feature.setImportant(false);
			}
			*/
			
			// check for correctness of attribute
			if(irrelevantAttribute){
				String response = RESPONSE_CONFIRM;
				
				// if the feature has problems, then attribute cannot be correct :(
				if(feature.getConceptStatus() == ConceptEntry.INCORRECT)
					response = RESPONSE_FAILURE;
				
				sendTutorResponse(msg,response,ERROR_ATTRIBUTE_NOT_IMPORTANT,candidate);
			}else if(invalidAttribute){
				// incorrect attribute
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ATTRIBUTE_NOT_VALID,candidate);
			}else if(correctAttribute){
				// what if location is no longer valid?
				if(incorrectLocation && !candidate.getParentEntry().isAbsent()){
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ATTRIBUTE_IN_INCORRECT_LOCATION,candidate);
				}else{
					String response = RESPONSE_CONFIRM;
					
					// if the feature has problems, then attribute cannot be correct :(
					if(feature != null && feature.getConceptStatus() == ConceptEntry.INCORRECT)
						response = RESPONSE_FAILURE;
					
					// correct attribute hooray (well, maybe)
					sendTutorResponse(msg,response,ERROR_OK,candidate);
				}
				
			}else if(invalidAttributeSet){
				// attribute combination is invalid
				String name = candidate.getParentEntry().getText();
				candidate.getTagMap().put(TAG_FINDING,name.substring(0,name.indexOf(",")));
				// incorrect attribute
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ATTRIBUTE_SET_NOT_VALID,candidate);
			}else if(incorrectValueAttributeName != null){
				// incorrect attribute
				candidate.getTagMap().put(TAG_ATTRIBUTE,incorrectValueAttributeName);
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ATTRIBUTE_VALUE_IS_INCORRECT,candidate);
			}else if(findings.isEmpty()){
				// incorrect attribute for finding that is not in a case
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_OK,candidate);
			}else{
				// incorrect attribute
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ATTRIBUTE_IS_INCORRECT,candidate);
			}
		
		}else if(ACTION_REMOVED.equals(action)){
			removeConceptInstance(candidate);
			doRemoveConcept(msg, candidate);
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// now lets make sure that changes didn't trigger something else
		if(ACTION_ADDED.equals(msg.getAction()) || ACTION_REMOVED.equals(msg.getAction()))
			revalidateHxAndDx(msg);
		
		// now check for links
		updateFindingLinks(msg,candidate);
		
		// show state
		debugShowState(time);
	}
		
	
	/**
	 * remove entries from the list that were already covered
	 * @param findings
	 * @return
	 */
	private void refineMatchedFindings(List<ConceptEntry> findings, ConceptEntry candidate){
		// rearrange based on candidates, suggestsions etc...
		if(findings.size() > 1){
			// try to filter out findings that might not be relevant
			for(ConceptEntry e: findings){
				// if tutor just suggested it, then this should be the only consideration
				if(e.equals(suggestedFinding)){
					findings.clear();
					findings.add(suggestedFinding);
					return;
				// check if subclass of candidate
				//}else if(e.equals(candidate) || isSubClassOf(e,candidate)){
					// filtering by subclassing is diabled
					// because location is more important and by adding
					// attributed to the mix one can confuse the picture
				}
			}
			
			/*
			ConceptEntry finding = null;
			for(ConceptEntry e: findings){
				// check if subclass of candidate
				if(e.equals(candidate) || isSubClassOf(e,candidate))
					finding  = e;
				else if(e.equals(suggestedFinding)){
					finding = suggestedFinding;
				}
			}
			
			// re-arrange findings
			if(finding != null){
				findings.remove(finding);
				findings.add(0,finding);
			}
			*/
		}
	}
	
	
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessFinding(Message msg){
		long time = System.currentTimeMillis();
		
		String action = msg.getAction();
		ConceptEntry candidate = getConceptEntry(msg);
		
		// shit, can't parse message, this should never happen
		if(candidate == null){
			sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_NO_SUCH_CONCEPT_IN_DOMAIN);
			return;
		}
		
		// clear implied state
		clearImpliedHxAndDx();
		
		if(ACTION_ADDED.equals(action) ){
			// save the input of a finding
			findingInputMap.put(msg.getObjectDescription(),msg.getInput());
			
			// get all findings in case that have the same feature
			List<ConceptEntry> findings = Collections.EMPTY_LIST;
			Exception error = null;
			try{
				// get matching findings that match parent entry
				// for new findings parent should be finding itself, for refines
				// it should provide clearer picture of what to expect
				findings = getMatchingFindings(candidate.getFeature());
			}catch(Exception ex){
				error = ex;
			}
			
			// now remove findings from that list that the user
			// already talked about,
			refineMatchedFindings(findings,candidate.getParentEntry());
			
			// register concept
			registerConceptEntry(candidate);	
			
			boolean findingIsImportant = true;
			// check if there were useful exception thrown
			if(error != null){
				if(error instanceof FindingIsTooGeneralException){
					// we say something relevant, but it is too general to matter
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_IS_TOO_GENERAL,candidate);
				}else if(error instanceof FindingNotInDomainException){
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_NO_SUCH_CONCEPT_IN_DOMAIN);
				}
			// if there are no relevent findings in case, then it is not in case	
			}else if(findings.isEmpty()){
				List<String> diagnoses = new ArrayList<String>();
				// check if given evidence maybe part of some diagnosis that is correct in this case
				for(ConceptEntry dx: getConcepts()){
					if(TYPE_DIAGNOSIS.equals(dx.getType())){
						if(dx.isCorrect()){
							if(testSupportLink(dx,candidate)){
								diagnoses.add(dx.getText());
							}
						}
					}
				}
				if(!diagnoses.isEmpty()){
					candidate.getTagMap().put(TAG_DIAGNOSIS,TextHelper.toString(diagnoses));
					// else the evidence is not in a case
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_NOT_IN_CASE_BUT_SUPPORTS_DIAGNOSIS,candidate);
				}else{
					// else the evidence is not in a case
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_NOT_IN_CASE,candidate);
				}
			}else {
				// Now that we know we have some hits
				// gather some facts about findings that we got a hit for
				boolean findingIsAbsent = true;
				boolean findingInCorrectLocation = false;
				boolean findingInTooLowPower = false;
				findingIsImportant = false;
				boolean findingIsComplete = false;
				
				// iterate through candidates to select a right finding based on location
				ConceptEntry finding = null;
				for(ConceptEntry e: findings){
					if(isCorrectLocation(e,msg.getInput())){
						// if finding is null, cool, else we have both findings
						// in the same location being correct, oops
						if(finding == null)
							finding = e;
						else
							finding = null;
						// yeah we are in correct location
						findingInCorrectLocation = true;
					}
				}
				
				// if we have a feature in correct location for some finding,
				// make sure it is first in the list
				// AHHHH, on second though make it the only one in the list
				// Added in response to negative reviews :)
				if(finding != null){ // && !finding.equals(findings.get(0))){
					//findings.remove(finding);
					//findings.add(0,finding);
					findings = Collections.singletonList(finding);
				}
				
				// now that the list was hopefully narrowed down by location
				// we can do a composite 
				for(ConceptEntry e: findings){
					findingIsAbsent &= e.isAbsent();
					findingInTooLowPower |= isTooLowPower(e,msg.getInput());
					findingIsImportant |= e.isImportant();
					findingIsComplete |= candidate.equals(e);
					candidate.getTagMap().put(TAG_POWER,e.getPower());
					
				}
				
				
				// on incomplete finding
				if(!findingIsComplete){
					ErrorEntry err = new ErrorEntry(HINT_MISSING_ATTRIBUTE,candidate);
					err.setPriority(1);
					err.setFindings(findings);
					pushError(err);
				}
				
				// now lets build decision tree
				if(findingIsAbsent){
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_SHOULD_BE_ABSENT,candidate);
				}else if(!findingInCorrectLocation){
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_IN_INCORRECT_LOCATION,candidate);
				}else {
					if(findingInTooLowPower)
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_FINDING_OBSERVED_AT_LOW_POWER,candidate);
					if(!findingIsImportant)
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_FINDING_NOT_IMPORTANT,candidate);
					if(findingIsImportant && !findingInTooLowPower)
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,candidate);
				}
			}
			// now add finding to state
			if(findingIsImportant){
				addConceptInstance(candidate);
				candidate.setImportant(true);
			}
		}else if(ACTION_REMOVED.equals(action)){
			removeConceptInstance(candidate);
			doRemoveConcept(msg, candidate);
		}else if(ACTION_REFINE.equals(action)){
			// since the original location was saved on the feature level
			// lets do this on the feature level, even though finding was probably saved
			ConceptEntry feature = candidate.getFeature();
			ConceptEntry finding = candidate.getParentEntry();
			
			// save the input of a finding
			findingInputMap.put(feature.getObjectDescription(),msg.getInput());
			
			// get all findings in case that have the same feature
			List<ConceptEntry> findings = Collections.EMPTY_LIST;
			try{
				// get matching findings that match parent entry
				// for new findings parent should be finding itself, for refines
				// it should provide clearer picture of what to expect
				findings = getMatchingFindings(feature);
			}catch(Exception ex){
				//error = ex;
			}
			
			// since this is a refine, findings should not be empty
			// unless we are moving around incorrect feature anyway,
			// in that case any refinements should be consider OK
			if(findings.isEmpty()){
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_NOT_IN_CASE,candidate);
			}else {
				// Now that we know we have some hits
				// gather some facts about findings that we got a hit for
				boolean featureInCorrectLocation = false;
				boolean findingInCorrectLocation = false;
				boolean findingIsAbsent = true;
				boolean isImportent = false;
				
				// shortcut for single findings
				if(findings.size() == 1){
					ConceptEntry e = findings.get(0);
					featureInCorrectLocation =  isCorrectLocation(e,msg.getInput());
					findingInCorrectLocation = featureInCorrectLocation;
					findingIsAbsent = e.isAbsent();
					isImportent = e.isImportant();
				}else{
					// iterate through candidates
					for(ConceptEntry e: findings){
						boolean correctLocation =  isCorrectLocation(e,msg.getInput());
						featureInCorrectLocation |= correctLocation;
						
						ConceptEntry f = getFirstDivergentPathEntry(finding);
						
						// if candidate is on the path of whatever is in the case
						if(e.getName().equals(f.getName()) || isSubClassOf(e,f) || isSubClassOf(f,e)){
							findingInCorrectLocation |= correctLocation;
						}
						
						// if correct location, check importance and absence
						if(correctLocation || findings.size() == 1){
							findingIsAbsent &= e.isAbsent();
							isImportent |= e.isImportant();
						}
						
					}
				}
				
				// resolve errors to make sure that we don't respond to stale 
				// errors
				resolveErrors();
				
				
				// now lets build decision tree
				if(findingIsAbsent){
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_SHOULD_BE_ABSENT,candidate);
					// if feature was incorrect, I need to revalidate all of its attributes
					for(ConceptEntry a: getAttributes(candidate)){
						if(!a.hasErrors())
							sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_OK,a);
					}	
				// if finding is NOT in correct location, else
				}else if(!findingInCorrectLocation){
					// now this suddenly gets a bit more complicated since we have to determine whenter the finding is a
					// culprit or all of the attributes
					if(featureInCorrectLocation){
						// see if finding before was incorrect
						synchronized(problemStack){
							for(ErrorEntry e: getProblemStack()){
								if(e.getError().equals(ERROR_FINDING_IN_INCORRECT_LOCATION)){
									if(e.getConceptEntry().getId().equals(candidate.getId()) || e.getConceptEntry().getId().equals(feature.getId())){
										e.getConceptEntry().removeError(e.getError());
										e.setResolved(true);
										// notify
										sendTutorResponse(msg,RESPONSE_CONFIRM,(isImportent)?ERROR_OK:ERROR_FINDING_NOT_IMPORTANT,candidate);
									}
								}
							}
						}
						// if feature is in correct location then, mark all attributes as culprits of bad location
						// unless of course they are incorrect to begin with
						for(ConceptEntry a: getAttributes(feature)){
							if(!a.hasErrors() || ERROR_ATTRIBUTE_IN_INCORRECT_LOCATION.equals(a.getError()))
								sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ATTRIBUTE_IN_INCORRECT_LOCATION,a);
						}
						
						//change important for non important
						//if(isImportent ^ feature.isImportant())
						sendTutorResponse(msg,RESPONSE_CONFIRM,(isImportent)?ERROR_OK:ERROR_FINDING_NOT_IMPORTANT,candidate);
					}else{
						sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_IN_INCORRECT_LOCATION,candidate);
						
						// if feature in incorrect location invalidate all of the attributes
						// if feature was incorrect, I need to revalidate all of its attributes
						for(ConceptEntry a: getAttributes(candidate)){
							if(a.getConceptStatus() != ConceptEntry.INCORRECT)
								sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_OK,a);
						}	
					
					}
				}else{
					// get all errors that are related to incorrect location
					// for each related error send its own confirm
					boolean hasErrors = false;
					synchronized(problemStack){
						for(ErrorEntry e: getProblemStack()){
							if(e.getError().equals(ERROR_FINDING_IN_INCORRECT_LOCATION) || e.getError().equals(ERROR_FINDING_SHOULD_BE_ABSENT)){
								if(e.getConceptEntry().getId().equals(candidate.getId()) || e.getConceptEntry().getId().equals(feature.getId())){
									e.getConceptEntry().removeError(e.getError());
									e.setResolved(true);
									hasErrors = true;
									
									// notify to correct finding
									sendTutorResponse(msg,RESPONSE_CONFIRM,(isImportent)?ERROR_OK:ERROR_FINDING_NOT_IMPORTANT,candidate);
									
									// if feature was incorrect, I need to revalidate all of its attributes
									for(ConceptEntry a: getAttributes(candidate)){
										boolean irrelevant = isIrrelevant(a.getError());
										if(!a.hasErrors() || irrelevant)
											sendTutorResponse(msg,RESPONSE_CONFIRM,(irrelevant)?ERROR_ATTRIBUTE_NOT_IMPORTANT:ERROR_OK,a);
									}	
								}
							}else if(e.getError().equals(ERROR_ATTRIBUTE_IN_INCORRECT_LOCATION)){
								if(e.getConceptEntry().getFeature().getId().equals(feature.getId())){
									e.getConceptEntry().removeError(e.getError());
									e.setResolved(true);
									//hasErrors = true;
									
									// notify
									sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,e.getConceptEntry());
								}
							}
						}
					}
					// notify if no errors were reported
					if(!hasErrors)
						sendTutorResponse(msg,RESPONSE_CONFIRM,(isImportent)?ERROR_OK:ERROR_FINDING_NOT_IMPORTANT,candidate);
				}
			}
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// now lets make sure that changes didn't trigger something else
		if(ACTION_ADDED.equals(msg.getAction()) || ACTION_REMOVED.equals(msg.getAction()))
			revalidateHxAndDx(msg);
		
		// show state
		debugShowState(time);
	}
	
	
	/**
	 * do process FOK
	 * @param msg
	 */
	private void doProcessFOK(Message msg){
		if(ACTION_SELF_CHECK.equals(msg.getAction()) && msg.getId() != null){
			String r = RESPONSE_CONFIRM;
			
			// check whethere there is an agreement 
			ConceptEntry entry = registry.get(msg.getId());
			if(entry != null && msg.getInput() instanceof Map){
				int status = entry.getConceptStatus();
				String fok = (String) ((Map) msg.getInput()).get("fok");
				// check for consistency
				if("sure".equals(fok)){
					if(status == ConceptEntry.INCORRECT)
						r = RESPONSE_FAILURE;
				}else if("unsure".equals(fok) || "error".equals(fok)){
					if(status == ConceptEntry.CORRECT || status == ConceptEntry.IRRELEVANT)
						r = RESPONSE_FAILURE;
				}
			}
			
			// do ok for all other actions
			sendTutorResponse(msg,r,ERROR_OK,entry);
		}
	}
	
	
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessAbsentFinding(Message msg){
		long time = System.currentTimeMillis();
		String action = msg.getAction();
		ConceptEntry candidate = getConceptEntry(msg);
		
		// shit, can't parse message, this should never happen
		if(candidate == null){
			sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_NO_SUCH_CONCEPT_IN_DOMAIN);
			return;
		}
		
		// clear implied state
		clearImpliedHxAndDx();
		
		if(ACTION_ADDED.equals(action) ){
			// save the input of a finding
			findingInputMap.put(msg.getObjectDescription(),msg.getInput());
			
			// get all findings in case that have the same feature
			List<ConceptEntry> findings = Collections.EMPTY_LIST;
			Exception error = null;
			try{
				findings = getMatchingFindings(candidate.getFeature());
			}catch(Exception ex){
				error = ex;
			}
			boolean findingIsImportant = true;
			
			// now remove findings from that list that the user
			// already talked about,
			refineMatchedFindings(findings,candidate.getParentEntry());
			
			// register concept
			registerConceptEntry(candidate);			
			
			// check if there were useful exception thrown
			if(error != null){
				if(error instanceof FindingIsTooGeneralException){
					// we say something relevant, but it is too general to matter
					//getProblemConcepts().push(candidate);
					//candidate.addError(ERROR_FINDING_IS_TOO_GENERAL);
					
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_FINDING_IS_TOO_GENERAL,candidate);
				}else if(error instanceof FindingNotInDomainException){
					sendTutorResponse(msg,RESPONSE_FAILURE,"Not in domain "+msg.getLabel());
				}
			// if there are no relevent findings in case, then it is not in case	
			}else if(findings.isEmpty()){
				findingIsImportant = false;
				// else the evidence is not in a case
				//candidate.addError(ERROR_ABSENT_FINDING_NOT_IN_CASE);
				sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_ABSENT_FINDING_NOT_IN_CASE,candidate);
			}else {
				// Now that we know we have some hits
				// gather some facts about findings that we got a hit for
				boolean findingIsPresent = true;
				boolean findingInCorrectLocation = false;
				boolean findingInTooLowPower = false;
				findingIsImportant = false;
				boolean findingIsComplete = false;
				
				// iterate through candidates
				for(ConceptEntry e: findings){
					findingIsPresent &= !e.isAbsent();
					findingInCorrectLocation |= isCorrectLocation(e,msg.getInput());
					findingInTooLowPower |= isTooLowPower(e,msg.getInput());
					findingIsImportant |= e.isImportant();
					findingIsComplete |= candidate.equals(e);
				}
				
				// finding is not complete
				if(!findingIsComplete){
					ErrorEntry err = new ErrorEntry(HINT_MISSING_ATTRIBUTE,candidate);
					err.setPriority(1);
					err.setFindings(findings);
					pushError(err);
				}
				
				// now lets build decision tree
				if(findingIsPresent){
					//candidate.addError(ERROR_ABSENT_FINDING_SHOULD_BE_PRESENT);
					//getProblemConcepts().push(candidate);
					sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_ABSENT_FINDING_SHOULD_BE_PRESENT,candidate);
				} else {
					if(findingInTooLowPower)
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_ABSENT_FINDING_OBSERVED_AT_LOW_POWER,candidate);
					if(!findingInCorrectLocation)
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_ABSENT_FINDING_IN_INCORRECT_LOCATION,candidate);
					if(!findingInTooLowPower && findingInCorrectLocation){
						sendTutorResponse(msg,RESPONSE_CONFIRM,findingIsImportant?ERROR_OK:ERROR_ABSENT_FINDING_NOT_IMPORTANT,candidate);
					}
				}
			}
			
			// now add finding to state
			if(findingIsImportant)
				addConceptInstance(candidate);
			
		}else if(ACTION_REMOVED.equals(action)){
			removeConceptInstance(candidate);
			doRemoveConcept(msg, candidate);
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// now lets make sure that changes didn't trigger something else
		if(ACTION_ADDED.equals(msg.getAction()) || ACTION_REMOVED.equals(msg.getAction()))
			revalidateHxAndDx(msg);
		
		// show state
		debugShowState(time);
	}
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessHypothesis(Message msg){
		long time = System.currentTimeMillis();
		String action = msg.getAction();
		ConceptEntry entry = getConceptEntry(msg);
		
		if(ACTION_ADDED.equals(action)){
			// register hypothesis
			registerConceptEntry(entry);
			
			
			if(!checkDiseaseRule(entry)){
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_HYPOTHESIS_IS_TOO_GENERAL,entry);
			}else if(getImpliedHypotheses().contains(entry)){
				sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,entry);
			}else{
				// if there are no evidence, any Hx is valid
				if(!checkFindings()){
					//getCorrectConcepts().add(entry);
					sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,entry);
				// if Hx is final Dx, then it is correct w/ a message
				}else if(checkConceptInCase(DIAGNOSES,entry)){
					sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED,entry);
				// else error, based on mode, give error
				}else{
					String err = (allHxMode)?ERROR_HYPOTHESIS_CONTRADICTS_SOME_FINDING:ERROR_HYPOTHESIS_DOESNT_MATCH_ANY_FINDINGS;
					if(allHxMode)
						entry.getTagMap().put(TAG_FINDINGS,getInconsistantFindings(entry));
					sendTutorResponse(msg,RESPONSE_FAILURE,err,entry);
				}
			}
		}else if(ACTION_REMOVED.equals(action)){
			doRemoveConcept(msg, entry);
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// show state
		debugShowState(time);
	}
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessDiagnosis(Message msg){
		long time = System.currentTimeMillis();
		String action = msg.getAction();
		ConceptEntry entry = getConceptEntry(msg);
		
		if(ACTION_ADDED.equals(action)){
			// register hypothesis
			registerConceptEntry(entry);
			
			if(!checkDiseaseRule(entry)){
				sendTutorResponse(msg,RESPONSE_FAILURE,ERROR_DIAGNOSIS_IS_TOO_GENERAL,entry);
			}else  if(getImpliedDiagnoses().contains(entry)){
				String err = ERROR_OK;
				if(problem.getConcepts(DIAGNOSES).containsKey(entry.getName()))
					err = ERROR_DIAGNOSIS_IS_CORRECT;
				else
					err = ERROR_DIAGNOSIS_IS_SUPPORTED_BUT_NOT_GOAL;
				sendTutorResponse(msg,RESPONSE_CONFIRM,err,entry);
			}else{
				// check if correct DX
				if(checkConceptInCase(DIAGNOSES,entry)){
					// if more specific then needs to be
					if(problem.getConcepts(DIAGNOSES).containsKey(entry.getName()))
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED,entry);
					else
						sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_DIAGNOSIS_IS_TOO_SPECIFIC,entry);
				}else{
					String err = ERROR_DIAGNOSIS_DOESNT_MATCH_ALL_FINDINGS;
					if(!checkFindings()){
						err = ERROR_DIAGNOSIS_NO_FINDINGS;
					}else{
						ConceptEntry hx = new ConceptEntry(entry.getName(),TYPE_HYPOTHESIS);
						if(absoluteDxMode && allHxMode && getImpliedHypotheses().contains(hx)){
							err = ERROR_DIAGNOSIS_DOESNT_HAVE_ENOUGH_EVIDENCE;
							entry.getTagMap().put(TAG_FINDINGS,getDiseaseRule(entry));
						}else
							entry.getTagMap().put(TAG_FINDINGS,getInconsistantFindings(entry));
					}
					sendTutorResponse(msg,RESPONSE_FAILURE,err,entry);
				}
			}
		}else if(ACTION_REMOVED.equals(action)){
			doRemoveConcept(msg, entry);		
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// show state
		debugShowState(time);
	}
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessSupportLink(Message msg){
		long time = System.currentTimeMillis();
		String action = msg.getAction();
		LinkConceptEntry entry  =  (LinkConceptEntry) getConceptEntry(msg);
		
		// make sure link was parsed
		if(entry == null){
			sendTutorResponse(msg,RESPONSE_FAILURE,"CAN'T PARSE LINK COMPONENTS");
			return;
		}
		
		// replce source entry 
		ConceptEntry src = registry.get(entry.getSourceConcept().getId());
		if(src != null){
			entry.setSourceConcept(src);
		}
		
		
		// check actions
		if(ACTION_ADDED.equals(action)){
			// register hypothesis
			registerConceptEntry(entry);
			
			// test the link, which tests if feature supports it
			if(testSupportLink(entry)){
				//getCorrectConcepts().add(entry);
				sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,entry);
			}else{
				String err = ERROR_LINK_FINDING_DOES_NOT_SUPPORT_HYPOTHESIS;
				
				// check if parent is ok
				expert.resolveConceptEntry(entry.getSourceConcept());
				
				if(testSupportLink(entry,entry.getSourceConcept().getFeature())){
					err = ERROR_LINK_FINDING_PARENT_SUPPORTS_HYPOTHESIS;
					entry.getTagMap().put(TAG_PARENT,entry.getSourceConcept().getFeature().getText());
				}
				sendTutorResponse(msg,RESPONSE_FAILURE,err,entry);
			}
		}else if(ACTION_REMOVED.equals(action)){
			doRemoveConcept(msg, entry);
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// show state
		debugShowState(time);
	}
	
	
	/**
	 * is this link valid
	 * @param entry
	 * @return
	 */
	private boolean testSupportLink(LinkConceptEntry entry){
		return testSupportLink(entry,entry.getSourceConcept());
	}
	
	/**
	 * is this link valid
	 * @param entry
	 * @return
	 */
	private boolean testSupportLink(LinkConceptEntry entry,ConceptEntry finding){
		return testSupportLink(entry.getDestinationConcept(), finding);
	}
	
	/**
	 * is this link valid
	 * @param entry
	 * @return
	 */
	private boolean testSupportLink(ConceptEntry diagnosis, ConceptEntry finding){
		// lookup classes
		IClass hx   = getConceptClass(diagnosis,ontology); 
		
		// create a temporary instance w/ just one finding
		IInstance inst = getInstance(ontology.getClass(CASES),"tempcase");
		addFinding(finding,inst,(finding.isAbsent())?HAS_NO_FINDING:HAS_FINDING);
		boolean result =  evaluateExpression(hx.getEquivalentRestrictions(), inst);
		
		// cleanup
		inst.delete();
		
		return result;
	}
	
	
	/**
	 * is this link valid
	 * @param entry
	 * @return
	 */
	private boolean testRefuteLink(LinkConceptEntry entry){
		// switch finding arround
		ConceptEntry e = entry.getSourceConcept().clone();
		if(refuteMode)
			e.setType(e.isAbsent()?TYPE_FINDING:TYPE_ABSENT_FINDING);
		
		// see if evidence supports the Hx
		boolean result = testSupportLink(entry,e);
		
		return (refuteMode)?result:!result;
	}
	
	
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessRefuteLink(Message msg){
		long time = System.currentTimeMillis();
		String action = msg.getAction();
		LinkConceptEntry entry  =  (LinkConceptEntry) getConceptEntry(msg);
		
		// make sure link was parsed
		if(entry == null){
			sendTutorResponse(msg,RESPONSE_FAILURE,"CAN'T PARSE LINK COMPONENTS");
			return;
		}
		
		// replce source entry 
		ConceptEntry src = registry.get(entry.getSourceConcept().getId());
		if(src != null){
			entry.setSourceConcept(src);
		}
			
		if(ACTION_ADDED.equals(action)){
			// register hypothesis
			registerConceptEntry(entry);
		
			// check result
			if(testRefuteLink(entry)){
				sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK,entry);
			}else{
				// check if it is unsupported
				String err = ERROR_LINK_FINDING_DOES_NOT_REFUTE_HYPOTHESIS;
				if(refuteMode && !testSupportLink(entry)){
					err = ERROR_LINK_NO_EVIDENCE_TO_REFUTE_HYPOTHESIS;
				}
				sendTutorResponse(msg,RESPONSE_FAILURE,err,entry);
			}
		}else if(ACTION_REMOVED.equals(action)){
			doRemoveConcept(msg, entry);
		}else if(ACTION_SELF_CHECK.equals(msg.getAction())){
			doProcessFOK(msg);
		}else{
			// do ok for all other actions
			sendTutorResponse(msg,RESPONSE_CONFIRM,ERROR_OK);
		}
		
		// show state
		debugShowState(time);
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

	public void sync(ReasoningModule tm) {}
	
	public ScenarioSet getSupportedScenarioSet(){
		if(supportedScenarios == null){
			supportedScenarios = new ScenarioSet();
			try {
				supportedScenarios.load(getClass().getResourceAsStream(OntologyHelper.DEFAULT_TUTOR_HELP_FILE));
			} catch (IOException e) {
				Config.getLogger().severe(TextHelper.getErrorMessage(e));
			}
		}
		return supportedScenarios;
	}
	
	/**
	 * some usefull exceptions
	 * @author tseytlin
	 *
	 */
	private class FindingIsTooGeneralException extends Exception {}
	private class FindingNotInDomainException extends Exception {}
	private class FindingIsNullException extends Exception {}
	
	
	/**
	 * get the first divergent finding on the path from feature to a parameter finding
	 * @param finding
	 * @return
	 */
	private ConceptEntry getFirstDivergentPathEntry(ConceptEntry finding){
		IClass fn = getConceptClass(finding,ontology);
		
		// 1st get common root for all FAVS
		IClass commonRoot = getFeature(fn);
		//(iterate until it diverges
		while(commonRoot.getDirectSubClasses().length == 1)
			commonRoot = commonRoot.getDirectSubClasses()[0];
		
		// now lets look at immediate children of common root
		for(IClass c: commonRoot.getDirectSubClasses()){
			if(c.hasSubClass(fn)){
				return new ConceptEntry(c.getName(),finding.getType());
			}
			
		}
		// else return original
		return finding;
	}
	
	public Tutor getTutor() {
		return tutor;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}
}
