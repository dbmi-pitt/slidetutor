package edu.pitt.dbmi.tutor.modules.reasoning;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.student.StaticStudentModule;
import edu.pitt.dbmi.tutor.ui.ReasonerStateExplorer;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IInstance;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.slideviewer.ViewPosition;

/**
 * this class represents a simple reasoner that matches goals
 * 
 * @author tseytlin
 * 
 */
public class SimplePrognosticReasoner implements ReasoningModule, ActionListener {
	private Communicator com = Communicator.getInstance();
	private String currentImage;
	private ViewPosition currentView;
	private CaseEntry problem;
	private IOntology ontology;
	private ExpertModule expert;
	private StudentModule student;
	private Properties defaultConfig;
	private Set<String> levelHints;
	private ConceptEntry nextStep;//,currentFinding;
	private boolean nothingIsDoneHintWasAsked;
	private JCheckBoxMenuItem debug;
	private Tutor tutor;
	private ActionProcessor actionProcessor;
	private ReasonerStateExplorer stateExplorer;
	private Set<ConceptEntry> reportTemplate;
	private ScenarioSet supportedScenarios;
	// register concepts that come in
	private Map<String,ConceptEntry> registry;
	private Stack<ErrorEntry> problemStack;
	private Stack<ConceptEntry> incompleteFindings;
	
	/**
	 * init prognostic reasoner
	 */
	public SimplePrognosticReasoner() {
		// concept registry
		registry = new HashMap<String, ConceptEntry>();
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
		
		// stack of incomplete findings
		incompleteFindings = new Stack<ConceptEntry>();
		
		// instantiate actionProcssor
		actionProcessor = new ActionProcessor();
		actionProcessor.loadDefaults(this);
	}

	public void load(){
		addDebugOption();
	}
	
	
	/**
	 * not really applicable for prognosis, just return correct diagnoses
	 */
	public List<ConceptEntry> getImpliedDiagnoses() {
		return (problem != null) ? problem.getConcepts(OntologyHelper.DIAGNOSES).getValues() : Collections.EMPTY_LIST;
	}

	/**
	 * not applicable for prognosis
	 */
	public List<ConceptEntry> getImpliedHypotheses() {
		return Collections.EMPTY_LIST;
	}

	/**
	 * register concept entry
	 * @param e
	 */
	
	private void registerConceptEntry(ConceptEntry e){
		if(e == null) 
			return;
		
		// if entry in an attribute, then remove a finding from previous parent
		if(TYPE_ATTRIBUTE.equals(e.getType()) && registry.containsKey(e.getFeature().getId())){
			ConceptEntry oldF = registry.get(e.getFeature().getId());
			//remove old finding and replace a feature
			if(oldF.hasParentEntry()){
				e.getParentEntry().getAttributes().addAll(oldF.getParentEntry().getAttributes());
				registry.remove(oldF.getParentEntry().getId());	
			}
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
	
	
	/**
	 * unregister concept entry
	 * @param e
	 */
	private void unregisterConceptEntry(ConceptEntry e){
		if(e == null)
			return;
		
		// remove concept itself, if no such concept, then ...
		if(registry.remove(e.getId()) == null){
			
			// well we can have two identical findings with different IDs because
			// they were generated by separate calls to OntologyHelper.createFinding
			// yet they represent the same thing, this normally happens when multiple
			// attributes are being deleted, in this case check if there is an equivalent
			// finding in the registry, if features match, then we got our finding
			if(e.isFinding()){
				for(ConceptEntry c: registry.values()){
					if(c.equals(e) && c.getFeature().getId().equals(e.getFeature().getId())){
						registry.remove(c.getId());
						break;
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
		problemStack.push(e);
	}
	
	/*
	 * get  last error
	 */
	private ErrorEntry popError(){
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
	
	/**
	 * get problem stack
	 * @return
	 */
	private Collection<ErrorEntry> getProblemStack(){
		return problemStack;
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
		
		// if error is related to action, then check it
		if(err.getError().toLowerCase().matches(".*(observe|action|measure).*")){
			for(ActionEntry a : err.getConceptEntry().getActions())
				if(a.isActionComplete())
					return true;
		}
		
		if(entry instanceof ActionEntry)
			return ((ActionEntry)entry).isActionComplete();
		
			
		
		// check all findings associated with this error
		// this is for all those specify questions
		if(!err.getFindings().isEmpty()){
			// if error is related to action, then check it
			if(err.getError().toLowerCase().matches(".*(observe|action|measure).*")){
				for(ConceptEntry f: err.getFindings()){
					for(ActionEntry a : f.getActions())
						if(a.isActionComplete())
							return true;
				}
			}
			
			// check which findings were already mentioned
			// also filter out findings that don't fit entire concept
			List<ConceptEntry> candidates = new ArrayList<ConceptEntry>();
			for(ConceptEntry f: err.getFindings()){
				if(isSubClassOf(f,entry.getParentEntry(),ontology) && !checkConcept(f)){
					candidates.add(f);
				}
			}
			
			// if no candidates then this issue is resolved
			if(candidates.isEmpty())
				return true;
			
			// else issue is not resolved, hance pick the first one  for a tag
			String text = candidates.get(0).isAbsent()?UIHelper.getTextFromName(candidates.get(0).getName()):candidates.get(0).getText();
			err.getConceptEntry().getTagMap().put(TAG_FINDING,text);
		}
		
		return false;
	}
	
	/**
	 * resolve error in the problem stack
	 * @param candidate
	 * @param error
	 */
	private void resolveError(ConceptEntry candidate, String error){
		for(ErrorEntry err: getProblemStack()){
			if(err.getConceptEntry().getId().equals(candidate.getId())){
				if(error.equals(err.getError())){
					err.getConceptEntry().removeError(error);
					err.setResolved(true);
				}
				
			}
		}
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
			// check if there is a more sepcific OR more general concept that has been mentioned
			for(ConceptEntry e: getConcepts()){
				if(e.getType().equals(entry.getType())){
					if(e.getName().equals(entry.getName()) || isSubClassOf(e,entry,ontology)){
						status =  true;
						break;
					}else if(isSubClassOf(entry,e,ontology)){
						status =  e.getInferredFindings().contains(entry);
						break;
					}
				}
			}
		}
		// was it found
		entry.setFound(status);
		return status;
	}
	
	/**
	 * get all concepts that were mentioned by a student
	 * @return
	 */
	private Collection<ConceptEntry> getConcepts(){
		return registry.values();
	}
	
	
	/**
	 * return a concept that the reasoner thinks
	 * should be handled next
	 * @return
	 */
	public ConceptEntry getNextConcept(){
		if(problem == null)
			return null;
		
		// select a candidate w/ lowest power that has not been found
		ConceptEntry candidate = null;
	
		// get next concept entry
		for(ConceptEntry e : problem.getReportFindings()){
			// make sure that concept was not found already
			if(!checkConcept(e)){
				candidate = e;
				break;
			}
		}
		
		return candidate;
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
	 * 
	 * @return
	 */
	public boolean isSolved() {
		if (problem == null)
			return true;
		return getProblemConcept() == null && getNextConcept() == null;

	}

	/**
	 * load case information (open image)
	 */
	public void setCaseEntry(CaseEntry problem) {
		// reset previous problem state
		reset();

		this.problem = problem;

		// initialize instances
		ontology = expert.getDomainOntology();

		// set primary slide
		if (problem.getPrimarySlide() != null)
			currentImage = problem.getPrimarySlide().getName();
		
		// init action processor
		actionProcessor.setCaseEntry(problem);
	}

	/**
	 * load expert module
	 */
	public void setExpertModule(ExpertModule module) {
		expert = module;
	}

	/**
	 * add debug option
	 */
	private void addDebugOption() {
		JMenu debugM = ITS.getInstance().getDebugMenu();
		String text = "Show Reasoner State";
		if (!UIHelper.hasMenuItem(debugM, text)) {
			 debugM.addSeparator();
			 debug = UIHelper.createCheckboxMenuItem("show-state",text,Config.getIconProperty("icon.menu.debug"),this);
			 debugM.add(debug);
			 //debugM.add(UIHelper.createMenuItem("clear-state","Clear Error Stack",null,this));
		}
	}

	public void setStudentModule(StudentModule module) {
		student = module;
	}
	
	public StudentModule getStudentModule(){
		if(student == null){
			student = new StaticStudentModule();
			student.load();
		}
		return student;
	}

	public void sync(ReasoningModule tm) {
	}

	public void dispose() {
		reset();
		expert = null;
		student = null;
	}

	public void reset() {
		actionProcessor.reset();
		registry.clear();
		problemStack.clear();
		incompleteFindings.clear();
		levelHints = null;
		currentImage = null;
		currentView = null;
		problem = null;
		ontology = null;
		reportTemplate = null;
		nothingIsDoneHintWasAsked = false;
	}

	public Properties getDefaultConfiguration() {
		if (defaultConfig == null) {
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Reasons on prognostic information (information related to the expected outcome of a disease). " +
				"The Reasoner evaluates user's actions in the context of the chosen domain.";
	}

	public String getName() {
		return "Simple Prognostic Reasoner";
	}

	public Action[] getSupportedActions() {
		return new Action[0];
	}

	public Message[] getSupportedMessages() {
		return new Message[0];
	}

	public String getVersion() {
		return "1.0";
	}

	/**
	 * parse concept entry from message
	 */
	private ConceptEntry getConceptEntry(Message msg) {
		ConceptEntry candidate = ConceptEntry.getConceptEntry(msg.getObjectDescription());
		if (msg.getEntireConcept() != null) {
			ConceptEntry parent = ConceptEntry.getConceptEntry(msg.getEntireConcept());
			parent.setFeature(candidate.getFeature());
			candidate.setParentEntry(parent);
			if(TYPE_ATTRIBUTE.equals(candidate.getType())){
				parent.getAttributes().add(candidate);
			}
		}
		return candidate;
	}

	/**
	 * get findings from case that share a feature with the parameter
	 * 
	 * @param name
	 * @return
	 */
	private List<ConceptEntry> getMatchingDiagnoses(ConceptEntry e) throws Exception {
		// can't parse description
		if (e == null)
			throw new FindingIsNullException();
		
		// if dx is a root node, then too general
		ConceptEntry g = getMostGeneralEntry(new ConceptEntry(DIAGNOSES,TYPE_DIAGNOSIS));
		if(e.equals(g) || isSubClassOf(g,e,ontology))
			throw new FindingIsTooGeneralException();
		
		// go over differential in this case
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(ConceptEntry dx: getImpliedDiagnoses()){
			if(dx.equals(e))
				return Collections.singletonList(dx);
			
			if(isSubClassOf(dx,e,ontology)){
				list.add(dx);
			}
		}
		return list;
	}

	
	/**
	 * get the first divergent finding on the path from feature to a parameter finding
	 * @param finding
	 * @return
	 */
	private ConceptEntry getMostGeneralEntry(ConceptEntry e){
		IClass commonRoot = getConceptClass(e,ontology);
		
		//(iterate until it diverges
		while(commonRoot.getDirectSubClasses().length == 1)
			commonRoot = commonRoot.getDirectSubClasses()[0];
		
		// else return original
		return new ConceptEntry(commonRoot.getName(),e.getType());
	}
	
	
	
	/**
	 * get appropriate template entry
	 * @param e
	 * @return
	 */
	private ConceptEntry getTemplateEntry(ConceptEntry e){
		if(reportTemplate == null)
			reportTemplate = expert.getReportTemplate(problem.getConcepts());
		
		for(ConceptEntry c: reportTemplate){
			if(c.getName().equals(e.getName()) ||  hasSubClass(c,e,ontology)){
				return c;
			}
		}
		return null;
	}
	
	
	/**
	 * get findings from case that share a feature with the parameter
	 * 
	 * @param name
	 * @return
	 */
	private List<ConceptEntry> getMatchingFindings(ConceptEntry e) throws Exception {
		// can't parse description
		if (e == null)
			throw new FindingIsNullException();

		// find class for candidate
		IClass candidate = getConceptClass(e, ontology);
		IClass cfeature = getFeature(candidate);
		boolean sameF1 = cfeature.equals(candidate);
		
		// if candidate is still not there, we are fucked
		if (candidate == null)
			throw new FindingNotInDomainException();

		// now iterate over classes in case
		List<ConceptEntry> findings = new ArrayList<ConceptEntry>();
		boolean findingTooGeneral = false;
		for (String category : new String[] { DIAGNOSES, PROGNOSTIC_FEATURES, CLINICAL_FEATURES }) {
			for (ConceptEntry entry : problem.getConcepts(category).getValues()) {
				IClass c = ontology.getClass(entry.getName());

				// if candidate is a child of case concept's feature ...
				IClass f = getFeature(c);

				// check if the candidate is way too general
				if (candidate.hasSubClass(f)) {
					findingTooGeneral = true;
				} else if (f.equals(candidate) || f.hasSubClass(candidate)) {
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
		if (findings.size() > 1) {
			List<ConceptEntry> pfindings = new ArrayList<ConceptEntry>();
			for (ConceptEntry entry : new ArrayList<ConceptEntry>(findings)) {
				IClass c = ontology.getClass(entry.getName());
				// now will candidate match to finding
				if (c.equals(candidate) || candidate.hasSubClass(c) || candidate.hasSuperClass(c))
					pfindings.add(entry);
				}
			
			// at least get first level sibling to finding?
			if(pfindings.isEmpty()){
				for (ConceptEntry entry : new ArrayList<ConceptEntry>(findings)) {
					IClass c = ontology.getClass(entry.getName());
					if(!Collections.disjoint(Arrays.asList(c.getDirectSuperClasses()),Arrays.asList(candidate.getDirectSuperClasses()))){
						pfindings.add(entry);
					}
				}
			}

			// now reassign findings
			if (!pfindings.isEmpty())
				findings = pfindings;

		}

		// if we have empty findings list and tooGeneral flag
		// then throw exception, the problem w/ throwing exception right away
		// is that the matching algorithm may consider throwing this exception
		// even
		// if there is another finding that matches it, just fine.
		// that is why it is better to do the matching, and then to notify.
		if (findings.isEmpty() && findingTooGeneral)
			throw new FindingIsTooGeneralException();

		return findings;
	}

	/**
	 * remove entries from the list that were already covered
	 * @param findings
	 * @return
	 */
	private void refineMatchedFindings(List<ConceptEntry> findings){
		// now remove findings from that list that the user
		// already talked about,
		if(findings.size() > 1){
			// remove findings that we've talked about
			// if we are adding finding, we need to remove findings that were
			// already mentioned
			for(ConceptEntry fn: new ArrayList<ConceptEntry>(findings)){
				for(ConceptEntry e: getConcepts()){
					// if what we already mentioned is identical to a finding
					// or more general then a finding (but not a feature), then get rid of it
					//if(!entry.equals(fn.getFeature()) && (fn.equals(entry) || isSubClassOf(fn,entry))){
					//	findings.remove(fn);
					//}
					if(e.isFinding() && !e.equals(fn.getFeature()) && (fn.equals(e) || 
					  (isSubClassOf(fn,e,ontology) && fn.getFeature().equals(e.getFeature()))))
						findings.remove(fn);
					
				}
			}
			
			// if we still has candidates look at suggestions
			/*
			if(findings.size() > 1 && isSuggested(findings.get(0))){
				findings.clear();
				findings.add(suggestedFinding);
			}	
			*/
		}
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
	
	/*
	private void relinkAttributes(){
		// clear attributes
		for(ConceptEntry e: registry.values()){
			if(e.isFinding() && e.getParentEntry().equals(e)){
				e.getAttributes().clear();
			}
		}
		// relink attributes
		for(ConceptEntry a: registry.values()){
			if(TYPE_ATTRIBUTE.equals(a.getType())){
				ConceptEntry p = registry.get(a.getParentEntry().getId());
				if(p != null)
					p.addAttribute(a);
			}
		}
	}
	*/
	
	private void resolveGoals(){
		for(ConceptEntry e : problem.getReportFindings()){
			checkConcept(e);
		}
	}

	/**
	 * used for debugging
	 */
	private void debugShowState(){
		if(debug != null && debug.isSelected()){
			resolveErrors();
			resolveIncompleteFindings();
			resolveGoals();
			
			// initialize state explorer
			if(stateExplorer == null){
				stateExplorer = new ReasonerStateExplorer();
				stateExplorer.setContent("User",registry);
				stateExplorer.setContent("Errors",problemStack);
				stateExplorer.setContent("Incompete",incompleteFindings);
				stateExplorer.setContent("Goals",problem.getReportFindings());
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
	 * get next step
	 * @return
	 */
	public ConceptEntry getNextStep(){
		//long time = System.currentTimeMillis();
		ConceptEntry next = getProblemConcept();
		if(next == null)
			next = getNextConcept();
		
		if(next == null){
			next = new ConceptEntry(TYPE_DONE,TYPE_DONE);
		}
		//System.out.println("next step calc: "+(System.currentTimeMillis()-time));
		return next;
	}
	
	/**
	 * get a tutor response for an input concept
	 * 
	 * @param response
	 * @param error
	 * @return
	 */
	private void sendTutorResponse(Message msg, String error) {
		sendTutorResponse(msg, error, null);
	}

	/**
	 * get a tutor response for an input concept
	 * 
	 * @param response
	 * @param error
	 * @return
	 */
	private void sendTutorResponse(Message msg, String error, Object input) {
		// get response, based on student model
		sendTutorResponse(msg,getStudentModule().getResponse(error),error, input);
	}
	/**
	 * get a tutor response for an input concept
	 * 
	 * @param response
	 * @param error
	 * @return
	 */
	private void sendTutorResponse(Message msg, String response, String error, Object input) {
		// generate the generic response
		TutorResponse r = new TutorResponse();
		r.setResponse(response);
		r.setError(error);
		r.setClientEvent((ClientEvent)msg);
		r.setTimestamp(System.currentTimeMillis());
		r.setSender(this);

		// set next step fields based on input object
		// determine when msg should be changed
		if (input != null && input instanceof ConceptEntry) {
			ConceptEntry c = (ConceptEntry) input;
			
			// if request was a hint or a bug occured, then nextstep
			// is an object that was passed
			if(TYPE_HINT.equals(msg.getType()) || RESPONSE_FAILURE.equals(response))
				nextStep = c;
			
			
			// add message to problem stack
			if(RESPONSE_FAILURE.equals(response) && !ERROR_OK.equals(error)){
				pushError(new ErrorEntry(error,c));
			}
				
			// remember concept state
			c.setConceptStatus(response);	
			
			// set response concept
			r.setResponseConcept(c.getObjectDescription());
		}else{
			r.setResponseConcept(msg.getObjectDescription());
		}

		// set next step fields
		/*
		r.setType(msg.getType());
		r.setLabel(msg.getLabel());
		r.setId(msg.getId());
		r.setAction(msg.getAction());
		r.setParent(msg.getParent());
		r.setInput(input);
		*/
		
		// get next step if not provided
		if(nextStep == null)
			nextStep = getNextStep();
		
		// figure out action for next step
		String action = ACTION_NEXT_STEP;
		if(RESPONSE_FAILURE.equals(response))
			action = ACTION_REMOVED;
		else
			action = ACTION_ADDED;
		
		// set everything up for next step
		r.setType(nextStep.getType());
		r.setLabel(nextStep.getName());
		r.setAction(action);
		
		// set parent if appropriate
		if(TYPE_ATTRIBUTE.equals(nextStep.getType()))
			r.setParent(nextStep.getFeature().getObjectDescription());
		
		// if in response to failure, then use the id
		// next step for OK has no ID
		if(RESPONSE_FAILURE.equals(response))
			r.setId(nextStep.getId());
	
		r.setInput(input);
		r.setTimestamp(System.currentTimeMillis());
		r.setSender(this);
	
		// DEBUG
		//System.out.println(msg.getType().toUpperCase() + " " + msg.getAction().toLowerCase() + " \"" + msg.getLabel()
		//		+ "\" | " + r.getResponse() + " | " + r.getError());

		com.sendMessage(r);
	}

	public void resolveAction(Action action) {
	}

	/**
	 * some usefull exceptions
	 * 
	 * @author tseytlin
	 * 
	 */
	private class FindingIsTooGeneralException extends Exception {}
	private class FindingNotInDomainException extends Exception {}
	private class FindingIsNullException extends Exception {}

	/**
	 * receive a message and build a decision tree
	 */
	public void receiveMessage(Message msg) {
		if (msg instanceof ClientEvent) {
			if (TYPE_FINDING.equals(msg.getType())) {
				if (ACTION_ADDED.equals(msg.getAction()))
					doAddFinding(msg);
				else if (ACTION_REMOVED.equals(msg.getAction()))
					doRemoveConcept(msg);
				else if(ACTION_GLOSSARY.equals(msg.getAction()))
					sendTutorResponse(msg, ERROR_OK);
			} else if (TYPE_ATTRIBUTE.equals(msg.getType())) {
				if (ACTION_ADDED.equals(msg.getAction()))
					doAddAttribute(msg);
				else if (ACTION_REMOVED.equals(msg.getAction()))
					doRemoveConcept(msg);
				else if(ACTION_GLOSSARY.equals(msg.getAction()))
					sendTutorResponse(msg, ERROR_OK);
			} else if (TYPE_DIAGNOSIS.equals(msg.getType())) {
				if (ACTION_ADDED.equals(msg.getAction()))
					doAddDiagnosis(msg);
				else if (ACTION_REMOVED.equals(msg.getAction()))
					doRemoveConcept(msg);
				else if(ACTION_GLOSSARY.equals(msg.getAction()))
					sendTutorResponse(msg, ERROR_OK);
			} else if (TYPE_HINT.equals(msg.getType())) {
				doProcessHint(msg);
			} else if (TYPE_DONE.equals(msg.getType())) {
				doDone(msg);
			} else if (TYPE_HINT_LEVEL.equals(msg.getType())) {
				sendTutorResponse(msg, ERROR_OK);
			} else if (TYPE_PRESENTATION.equals(msg.getType()) || TYPE_ACTION.equals(msg.getType())) {
				boolean response = false;
				if (ACTION_IMAGE_CHANGE.equals(msg.getAction())) {
					currentImage = msg.getLabel();
					actionProcessor.processSlide(currentImage);
				} else if (ACTION_VIEW_CHANGE.equals(msg.getAction()) && !msg.isAuto()) {
					currentView = TextHelper.parseViewPosition(msg.getInputMap());
					actionProcessor.processView(currentView);
				} else if (ACTION_MEASURE.equals(msg.getAction()) || ACTION_REFINE.equals(msg.getAction())){
					actionProcessor.processRuler(TextHelper.parseLine(msg.getLabel()));
				}
				// send response
				for(ActionEntry a: actionProcessor.pop()){
					String res = getStudentModule().getResponse(a.getError());
					registry.put(a.getId(),a);
					if(!RESPONSE_IRRELEVANT.equals(res)){
						sendTutorResponse(msg,res, a.getError(),a);
						response = true;
					}
					
				}
				if(!response)
					sendTutorResponse(msg, ERROR_OK);
				
			}else if(TYPE_INFO.equals(msg.getType())){
				sendTutorResponse(msg, ERROR_OK);
			}
			

			// show state
			if(Arrays.asList(TYPE_FINDING,TYPE_ATTRIBUTE,TYPE_DIAGNOSIS,TYPE_HINT).contains(msg.getType()))
				debugShowState();
		}
	}

	
	/**
	 * add attribute to a feature
	 * @param msg
	 */
	private void doAddAttribute(Message msg) {
		ConceptEntry candidate = getConceptEntry(msg);
		registerConceptEntry(candidate);
		
		
		// get all findings in case that have the same feature
		List<ConceptEntry> findings = Collections.EMPTY_LIST;
		Exception error = null;
		try {
			// get matching findings that match parent entry
			// for new findings parent should be finding itself, for refines
			// it should provide clearer picture of what to expect
			findings = getMatchingFindings(candidate.getParentEntry());
		} catch (Exception ex) {
			error = ex;
		}
	
		// check if there were useful exception thrown
		if (error != null) {
			if (error instanceof FindingNotInDomainException) {
				sendTutorResponse(msg, ERROR_NO_SUCH_CONCEPT_IN_DOMAIN, candidate);
			}
		} else {
			// iterate through candidates
			boolean correctAttribute = false;
			boolean irrelevantAttribute = false;
			boolean invalidAttribute = false;
			boolean invalidAttributeSet = false;
			boolean completeFinding = false;
			boolean incorrectNegation = false;
			boolean incorrectNumericValue = false;
			boolean missingNegation = false;
			boolean inferAllFindings = false;
			boolean missingAction = false;
			String incorrectValueAttributeName = null;
			
			// check for absence
			for (ConceptEntry e : findings) {
				// resolve each finding
				expert.resolveConceptEntry(e);
				
				// check for completeness
				completeFinding |= e.equals(candidate.getParentEntry()) || isSubClassOf(candidate.getParentEntry(),e,ontology);
				
				// now check numeric value
				boolean correctNumericValue = false;
				if(candidate.hasNumericValue() && e.hasNumericValue()){
					correctNumericValue = OntologyHelper.compareValues(e.getNumericValue(),candidate.getNumericValue());
					incorrectNumericValue = !correctNumericValue;
				}
				
				// if candidate is in a list of correct findings attributes u r good
				if(e.getAttributes().contains(candidate) || (e.isAbsent() && LABEL_NO.equals(candidate.getName())) || correctNumericValue){
					correctAttribute = true;
				}else if(isSubClassOf(candidate.getParentEntry(),e,ontology)){
					irrelevantAttribute = true;
				}else if(!e.getPotentialAttributes().contains(candidate) && !candidate.hasNumericValue()){
					invalidAttribute = true;
				}
				
				
					
				// the finding is absent
				incorrectNegation = candidate.isAbsent() && !e.isAbsent();
				
				// check if negated that is parent finding is NOT absent, but should be and THIS attribute is not a NO
				missingNegation = (e.isAbsent() && !candidate.getParentEntry().isAbsent() && !LABEL_NO.equals(candidate.getName()));
				
				// check for invalid set
				String name = candidate.getParentEntry().getName(); 
				int x = name.indexOf(","); 
				if(x > -1 && name.indexOf(candidate.getName(),x) > -1){
					invalidAttributeSet = true;
				}else if(!correctAttribute){
					incorrectValueAttributeName = getAttributeName(candidate,e.getAttributes(),ontology);
					if(incorrectValueAttributeName != null)
						candidate.getTagMap().put(TAG_ATTRIBUTE,incorrectValueAttributeName.toLowerCase());
				}
				
				// go over actions
				for(ActionEntry a: e.getActions()){
					if(!a.isActionComplete()){
						missingAction = true;
					}
				}
				
				// now if we have multiple findings with correct attribute for both of them
				// yet those are incomplete findings we should infer the missing attribute
				// example: margins involved
				inferAllFindings = (findings.size() > 1 && correctAttribute && !completeFinding);
			}
			
			// add finding IF at least there is a parent in case, else it might be irrelevant
			if(findings.isEmpty() && candidate.getFeature().isAbsent()){
				irrelevantAttribute = true;
			}
			
			// get feature from registry
			//ConceptEntry feature = registry.get(candidate.getFeature().getId());
			
			
			// if in fact we infer all findings, then resolve issues associated with them
			if(inferAllFindings){
				// add to list of inferred findings
				ConceptEntry finding = registry.get(candidate.getParentEntry().getId());
				finding.setInferredFindings(findings);
			}
			
			// add incomplete finding to the stack
			if(!inferAllFindings && (!completeFinding || missingNegation)){
				addIncompleteFinding(candidate.getParentEntry());
			}
			
			// set as potential parent entry
			//candidate.setPotentialFindings(findings);
			
			//now decide on output for attribute
			if (correctAttribute) {
				sendTutorResponse(msg, ERROR_OK, candidate);
			}else if(missingAction){
				sendTutorResponse(msg,ERROR_ACTION_INCOMPLETE,candidate);
			}else if(incorrectNumericValue){
				if(!findings.isEmpty())
					candidate.getTagMap().put(TAG_CORRECT_VALUE,""+findings.get(0).getNumericValue());
				sendTutorResponse(msg, ERROR_ATTRIBUTE_NUMERIC_VALUE_IS_INCORRECT, candidate);
			}else if(incorrectNegation){
				sendTutorResponse(msg, ERROR_INCORRECT_NEGATION_ATTRIBUTE, candidate);
			}else if (irrelevantAttribute) {
				sendTutorResponse(msg, ERROR_ATTRIBUTE_NOT_IMPORTANT, candidate);
			}else if (incorrectValueAttributeName != null) {
				sendTutorResponse(msg, ERROR_ATTRIBUTE_VALUE_IS_INCORRECT, candidate);
			} else if (invalidAttribute) {
				sendTutorResponse(msg, ERROR_ATTRIBUTE_NOT_VALID, candidate);
			} else if (invalidAttributeSet) {
				String name = candidate.getParentEntry().getText();
				candidate.getTagMap().put(TAG_FINDING,name.substring(0,name.indexOf(",")));
				sendTutorResponse(msg, ERROR_ATTRIBUTE_SET_NOT_VALID, candidate);
			}else if(findings.isEmpty()){
				// incorrect attribute for finding that is not in a case
				// TODO:
				sendTutorResponse(msg,RESPONSE_IRRELEVANT,ERROR_OK,candidate);
			} else {
				sendTutorResponse(msg, ERROR_ATTRIBUTE_IS_INCORRECT, candidate);
			}
		}
	}
	
	/**
	 * remove concept entry
	 * @param msg
	 * @param entry
	 */
	private void doRemoveConcept(Message msg){
		ConceptEntry entry = getConceptEntry(msg);
		
		// do not remove something that is not there
		if(!registry.containsKey(entry.getId()))
			return;
		
		// remove as a problem or correct feature
		if(TYPE_ATTRIBUTE.equals(entry.getType())){
			
			// refine finding
			ConceptEntry parent = entry.getParentEntry();
			
			// get a new list of attributes from the case
			List<ConceptEntry> attributes = new ArrayList<ConceptEntry>();
			for(ConceptEntry a: getConcepts()){
				if(TYPE_ATTRIBUTE.equals(a.getType()) && 
				    a.getFeature().getId().equals(entry.getFeature().getId())){
					attributes.add(a);
				}
			}
			attributes.remove(entry);
			
			ConceptEntry child  = createFinding(entry.getFeature(),attributes,ontology);
			
			// if the new finding minus a given attribute returns the identical finding as an original
			// that means that attributes are inter linked and remaining attributes are only relevan
			// in the context of another. Since it is difficult to determine which attributes are related
			// and which are not, just remove all of the remaining attributes
			if(child == null || parent.equals(child) || child.equals(entry.getFeature())){
				child = entry.getFeature();
				child.setParentEntry(null);
			}else{
				
				// now do refine finding
				// place new concept on top of old concept
				parent.copyTo(child);
			}
				
			// remove  old concept from registry as well as its parent
			unregisterConceptEntry(entry);
			unregisterConceptEntry(parent);
		
			// add new concept to registry	
			registerConceptEntry(child);
			
			// set new parent entry for attribute (could be used later Ex: updateLinks code
			entry.setParentEntry(child);
			
			
			// after this finding can become incomplete
			addIncompleteFinding(child);
			
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
		
		sendTutorResponse(msg,ERROR_OK,entry);
	}
	
	/**
	 * add diagnosis
	 * @param msg
	 */
	private void doAddDiagnosis(Message msg) {
		ConceptEntry candidate = getConceptEntry(msg);
		registerConceptEntry(candidate);
		
		// get all findings in case that have the same feature
		List<ConceptEntry> diagnoses = Collections.EMPTY_LIST;
		Exception error = null;
		try {
			// get matching findings that match parent entry
			// for new findings parent should be finding itself, for refines
			// it should provide clearer picture of what to expect
			diagnoses = getMatchingDiagnoses(candidate.getParentEntry());
		} catch (Exception ex) {
			error = ex;
		}
		
		// check if there were useful exception thrown
		if (error != null) {
			if (error instanceof FindingIsTooGeneralException) {
				sendTutorResponse(msg, ERROR_DIAGNOSIS_IS_TOO_GENERAL, candidate);
			} else if (error instanceof FindingNotInDomainException) {
				sendTutorResponse(msg, ERROR_NO_SUCH_CONCEPT_IN_DOMAIN);
			}
			// if there are no relevant findings in case, then it is not in case
		} else if (diagnoses.isEmpty()) {
			// else the evidence is not in a case
			sendTutorResponse(msg,ERROR_DIAGNOSIS_IS_INCORRECT, candidate);
		} else {
			boolean findingIsComplete = false;
			boolean tooSpecific = false;
			ConceptEntry dx = null;
			
			// check for absence
			for (ConceptEntry f : diagnoses) {
				findingIsComplete |= candidate.equals(f);
				tooSpecific = !findingIsComplete && isSubClassOf(candidate,f,ontology);
				dx = f;
			}
			
			if(tooSpecific){
				sendTutorResponse(msg, ERROR_DIAGNOSIS_IS_TOO_SPECIFIC,candidate);
			}else if(!findingIsComplete){
				List<String> children = new ArrayList<String>();
				for(ConceptEntry e: getSubClasses(candidate,ontology))
					children.add(e.getText());
				Collections.sort(children);
				candidate.getTagMap().put(TAG_DIAGNOSIS,dx.getText());
				candidate.getTagMap().put(TAG_POTENTIAL_DIAGNOSES,TextHelper.toString(children));
				
				ErrorEntry err = new ErrorEntry(ERROR_DIAGNOSIS_NOT_SPECIFIC_ENOUGH,candidate);
				err.setPriority(1);
				pushError(err);
				
				sendTutorResponse(msg,RESPONSE_CONFIRM, ERROR_DIAGNOSIS_NOT_SPECIFIC_ENOUGH,candidate);
			} else {
				sendTutorResponse(msg, ERROR_OK, candidate);
			}
		}
		
		// this is a current finding
		//currentFinding = candidate;
	}
	
	/**
	 * process new finding
	 * 
	 * @param msg
	 */
	private void doAddFinding(Message msg) {
		ConceptEntry candidate = getConceptEntry(msg);
		
		// get all findings in case that have the same feature
		List<ConceptEntry> findings = new ArrayList<ConceptEntry>();
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
		refineMatchedFindings(findings);
		
		// register concept
		registerConceptEntry(candidate);
	
		// if you have a FindingIsTooGeneral while it is a template, then
		ConceptEntry templateEntry = getTemplateEntry(candidate);
		if(error != null && error instanceof FindingIsTooGeneralException){
			if(templateEntry != null && templateEntry.getName().equals(candidate.getName()))
				error = null;
		}
		
		// check if there were useful exception thrown
		if (error != null) {
			if (error instanceof FindingIsTooGeneralException) {
				candidate.getTagMap().put(TAG_CHILDREN,getChildren(candidate));
				candidate.getTagMap().put(TAG_FINDING, getCandidate(candidate));
				
				sendTutorResponse(msg, ERROR_FINDING_IS_TOO_GENERAL, candidate);
			} else if (error instanceof FindingNotInDomainException) {
				sendTutorResponse(msg, ERROR_NO_SUCH_CONCEPT_IN_DOMAIN, candidate);
			}
			// if there are no relevant findings in case, then it is not in case
		} else if (findings.isEmpty()) {
			//ConceptEntry templateEntry = getTemplateEntry(candidate);
			boolean irrelevant = false;
			// lets check again with a  more general findings that are not too general
			if(templateEntry != null){
				// now if we do have template and it is Macroscopic, the word is to geeneral and can occur anywhere
				// so lets just ignore it
				if(templateEntry.equals(candidate) && templateEntry.isHeaderFinding()){
					sendTutorResponse(msg,ERROR_OK,ERROR_FINDING_NOT_IMPORTANT,candidate);
					irrelevant = true;
				}else{
					for(ConceptEntry c: problem.getConcepts(PROGNOSTIC_FEATURES).getValues()){
						if(hasSubClass(templateEntry, c, ontology))
							findings.add(c);
					}
					if(!findings.isEmpty()){
						candidate.getTagMap().put(TAG_TEMPLATE,templateEntry.getText());
						sendTutorResponse(msg, ERROR_FINDING_IS_INCORRECT,candidate);
					}
				}
			}			
			// else the evidence is not in a case
			if(findings.isEmpty() && !irrelevant)
				sendTutorResponse(msg, ERROR_FINDING_NOT_IN_CASE, candidate);
		} else {
			boolean findingIsComplete = false;
			boolean missingNegation = false;
			
			// check for absence
			for (ConceptEntry f : findings) {
				findingIsComplete |= (candidate.getName().equals(f.getName()) || isSubClassOf(candidate,f,ontology));
				missingNegation = f.isAbsent();
			}
							
			// return absent
			sendTutorResponse(msg, ERROR_OK,candidate);
			
			// add incomplete finding to the stack
			if(!findingIsComplete || missingNegation){
				addIncompleteFinding(candidate.getParentEntry());
			}
		}
	}

	
	/**
	 * finding that has not been completed
	 * @param candidate
	 */
	
	private boolean doIncompleteFinding(Message msg) {
		// get next incomplete finding
		ConceptEntry candidate  = getIncompleteFinding();
		if(candidate == null)
			return false;
		
		// maybe gets the latest from the registry
		if(registry.containsKey(candidate.getId()))
			candidate = registry.get(candidate.getId());
		
		// now analyze it
		boolean findingIsComplete = false;
		boolean missingNegation = false;
		boolean missingValue = false;
		ActionEntry missingAction = null;
		boolean missingAttribute = false;
		
		List<ConceptEntry> findings = Collections.EMPTY_LIST;
		try {
			// get matching findings that match parent entry
			// for new findings parent should be finding itself, for refines
			// it should provide clearer picture of what to expect
			findings = getMatchingFindings(candidate);
		} catch (Exception ex) {
			//Should not happen here
		}
		
		// check for absence
		for (ConceptEntry f : findings) {
			findingIsComplete |= (candidate.equals(f) || isSubClassOf(candidate,f,ontology));
	
			// check if we are missing an action IF numeric value was not correctly reported already
			if(!candidate.hasNumericValue()){
				for(ActionEntry a: f.getActions()){
					if(!a.isActionComplete())
						missingAction = a;
				}
			}
			
			// set missing negation
			if(f.isAbsent() && findings.size() == 1){
				missingNegation = !candidate.getAttributes().contains(new ConceptEntry(LABEL_NO,TYPE_ATTRIBUTE));
			}else if(f.hasNumericValue() && findings.size() == 1 && !candidate.hasNumericValue()){
				missingValue = true;
			}
			
			// list of missing attributes
			List<ConceptEntry> attrs = new ArrayList<ConceptEntry>(f.getAttributes());
			attrs.removeAll(candidate.getAttributes());
			if(!attrs.isEmpty() && !MODIFIERS.equals(attrs.get(0).getCategory())){
				missingAttribute = true;
			}
		}
			
		// if finding is in fact it is complete, then we get back to square one
		if(findingIsComplete && !missingNegation){
			incompleteFindings.remove(candidate);
			return false;
		}
		
		candidate.setPotentialFindings(findings);
		
		if(findings.size() > 1){
			sendTutorResponse(msg,HINT_MISSING_DEFINING_ATTRIBUTE,candidate);
		}else if(missingAction != null){
			if(missingAction.isObserveAll()){
				sendTutorResponse(msg,HINT_MISSING_OBSERVE_ALL_ACTION,candidate);
			}else if(missingAction.isMeasureRuler()){
				sendTutorResponse(msg,HINT_MISSING_RULER_ACTION,candidate);
			}else if(missingAction.isMeasureHPF()){
				sendTutorResponse(msg,HINT_MISSING_HPF_ACTION,candidate);
			}else if(missingAction.isMeasureMM2()){
				sendTutorResponse(msg,HINT_MISSING_MM2_ACTION,candidate);
			}else
				sendTutorResponse(msg,HINT_MISSING_ACTION,candidate);
		}else if(missingValue){
			sendTutorResponse(msg,HINT_MISSING_VALUE,candidate);
		}else if(missingNegation){
			sendTutorResponse(msg,HINT_MISSING_NEGATION,candidate);
		}else if(missingAttribute){
			sendTutorResponse(msg,HINT_MISSING_ATTRIBUTE,candidate);
		}else{
			sendTutorResponse(msg,HINT_MISSING_MODIFIER,candidate);
		}
		return true;
	}

	/*
	private String getDefinedFeature(ConceptEntry finding){
		ConceptEntry f = finding.getFeature();
		ConceptEntry m = null;
		IOntology o = ontology;
		for(ConceptEntry a: finding.getAttributes()){
			if(getConceptClass(a,o).hasDirectSuperClass(o.getClass(MODIFIERS))){
				m = a;
				break;
			}
		}
		IClass c = OntologyHelper.getCommonChild(getConceptClass(f, o),getConceptClass(m,o));
		return c != null?TextHelper.getPrettyClassName(c.getName()):f.getText();
	}
	*/
	
	/**
	 * reseuest done
	 * 
	 * @param msg
	 */
	private void doDone(Message msg) {
		if (isSolved()) {
			sendTutorResponse(msg, HINT_CASE_SOLVED,getCaseSummary());
		} else {
			sendTutorResponse(msg, ERROR_CASE_NOT_SOLVED,getCaseSummary());
			
			// figure out why  are there diagnosis identified?
			/*
			if(checkConceptType(TYPE_DIAGNOSIS)){
				ConceptEntry e = getNextConcept();
				// if next concept is null WTF?, but I guess we are done
				if(e == null){
					//ConceptEntry p = getProblemConcept();
					ErrorEntry err = popError();
					if(err != null){
						com.sendMessage(getTutorResponse(msg,RESPONSE_FAILURE,err.getError(),err.getConceptEntry()));
					}else{
						com.sendMessage(getTutorResponse(msg,RESPONSE_CONFIRM,HINT_CASE_SOLVED));
					}
					return;
				}
				
				// else notify 
				if(TYPE_FINDING.equals(e.getType())){
					boolean refine = false;
					for(ConceptEntry c: getConcepts())
						if(isSubClassOf(e,c.getParentEntry())){
							refine = true;
							break;
						}
					// if refine then different error message
					if(refine)
						com.sendMessage(getTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_NEEDS_REFINED_FINDINGS));
					else		
						com.sendMessage(getTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_HAS_MORE_FINDINGS_TO_CONSIDER));
				}else{
					com.sendMessage(getTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_HAS_MORE_DIAGNOSES_TO_CONSIDER));
				}
			}else{
				com.sendMessage(getTutorResponse(msg, RESPONSE_FAILURE, ERROR_CASE_NEEDS_DIAGNOSIS_TO_FINISH));
			}
			*/
		}
	}

	
	/**
	 * create case summary
	 * @return
	 */
	private List<ConceptEntry> getCaseSummary() {
		List<ConceptEntry> issueList = new ArrayList<ConceptEntry>();
		
		// add error problems
		for(ErrorEntry e : problemStack){
			if(!checkResolved(e))
				issueList.add(e.getConceptEntry());
		}
		// add missing problems
		if(Config.getBooleanProperty(this,"show.missed.goals")){
			// get next concept entry
			for(ConceptEntry e : problem.getReportFindings()){
				// make sure that concept was not found already
				if(!checkConcept(e)){
					ConceptEntry ec = e.clone();
					String err = HINT_MISSING_FINDING;
					if(TYPE_DIAGNOSIS.equals(ec.getType()))
						err = HINT_MISSING_DIAGNOSIS;
					else if(ec.isHeaderFinding())
						err = HINT_MISSING_HEADER;
					else if(ec.isWorksheetFinding())
						err = HINT_MISSING_WORKSHEET;
					else if(ec.isAbsent())
						err = HINT_MISSING_ABSENT_FINDING;
					
					// check if another is in order
					if(e.isFinding()){
						ConceptEntry s = getSimilarFinding(e,issueList);
						if(s != null && !e.getName().equals(s.getName())){
							// if similar finding is a feature, then we are missing an attribute
							if(isSubClassOf(e,s,ontology)){
								// resolve the sibling
								expert.resolveConceptEntry(s);
								// if siblings feature is identical to it, then this is a first attribute
								// that needs to be added
								if(s.getFeature().equals(s)){
									err = HINT_MISSING_ATTRIBUTE;
								}else{
									err = HINT_MISSING_ANOTHER_ATTRIBUTE;
								}
							// if twe have a complete finding, but not negated, do negation bit
							}else if(e.isAbsent()){
								err = HINT_MISSING_NEGATION;
							}else{
								// else it could be another feature
								err = HINT_MISSING_SIMILAR_FINDING;
								e.getTagMap().put(TAG_SIBLING,UIHelper.getTextFromName(s.getName()));
							}
						}
					}
					
					ec.addError(err);
					
					issueList.add(ec);
				}else{
					issueList.add(e);
				}
			}
		}
		
		return issueList;
	}

	private ConceptEntry getIncompleteFinding(){
		resolveIncompleteFindings();
		if(incompleteFindings.isEmpty())
			return null;
		return incompleteFindings.peek();
	}
	
	private void addIncompleteFinding(ConceptEntry e){
		if(incompleteFindings.contains(e))
			incompleteFindings.remove(e);
		incompleteFindings.add(e);
	}
	
	private void resolveIncompleteFindings(){
		for(ConceptEntry e: new ArrayList<ConceptEntry>(incompleteFindings)){
			// is the concept in question even there?
			// if not, then error is resolved
			if(!registry.containsKey(e.getId())){
				incompleteFindings.remove(e);
			}else{
				// update from registry
				e = registry.get(e.getId());
				
				// check if there is a more specific finding that
				// was already mentinoned
				for(ConceptEntry en: getConcepts()){
					if(!en.equals(e) && isSubClassOf(en,e, ontology) && en.getFeature().getId().equals(e.getFeature().getId())){
						incompleteFindings.remove(e);
					}
				}
				if(!e.getInferredFindings().isEmpty())
					incompleteFindings.remove(e);
				
			}
		}
	}
	
	
	/**
	 * process finding
	 * @param name
	 * @param action
	 */
	private void doProcessHint(Message msg){
		//long time = System.currentTimeMillis();
		
		// get problem concept
		ErrorEntry errorEntry = popError();
		
		//if(entry != null && entry.hasErrors()){
		if(errorEntry != null){
			sendTutorResponse(msg,RESPONSE_HINT,errorEntry.getError(),errorEntry.getConceptEntry());
		}else if(isSolved()){
			sendTutorResponse(msg,RESPONSE_HINT,HINT_CASE_SOLVED,null);
		}else{
			// try to process incomplete findings firsted
			boolean processed = doIncompleteFinding(msg);
			
			// if no incomplete findings, then do your normal thing
			if(!processed){
				// get next concept
				ConceptEntry entry = getNextConcept();
							
				// check if there was anything done
				if(registry.isEmpty() && !nothingIsDoneHintWasAsked){
					nothingIsDoneHintWasAsked = true;
					sendTutorResponse(msg,RESPONSE_HINT,HINT_NOTHING_DONE_YET,entry);
				// check if dx is the one that is missing
				}else if(TYPE_DIAGNOSIS.equals(entry.getType())){
					entry.getTagMap().put(TAG_FINDINGS,getDiseaseRule(entry));
					sendTutorResponse(msg,RESPONSE_HINT,HINT_MISSING_DIAGNOSIS,entry);
				// is it a finding
				}else if(entry.isHeaderFinding()){
					sendTutorResponse(msg,RESPONSE_HINT,HINT_MISSING_HEADER,entry);
				}else if(entry.isWorksheetFinding()){
					sendTutorResponse(msg,RESPONSE_HINT,HINT_MISSING_WORKSHEET,entry);
				}else if(entry.isFinding()){
					// this is just a missing whole finding
					String err = HINT_MISSING_FINDING;
					// se if we already mentioned similar finding
					ConceptEntry s = getSimilarFinding(entry);
					if(s != null){
						// if similar finding is a feature, then we are missing an attribute
						if(isSubClassOf(entry,s,ontology)){
							incompleteFindings.add(s);
							doIncompleteFinding(msg);
						}else{
							// else it could be another feature
							err = HINT_MISSING_SIMILAR_FINDING;
							entry.getTagMap().put(TAG_SIBLING,UIHelper.getTextFromName(s.getName()));
						}
					}
					sendTutorResponse(msg,RESPONSE_HINT,err,entry);	
				}
			}
		}
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
	private String getChildren(ConceptEntry candidate){
		if(ontology == null)
			return "";
		IClass c = OntologyHelper.getConceptClass(candidate, ontology);
		if(c != null){
			Set<String> children = new TreeSet<String>();
			for(IClass child: c.getSubClasses()){
				children.add(UIHelper.getTextFromName(child.getName()));
			}
			return TextHelper.toString(children);
		}
		return "";
	}
	
	/**
	 * get a list of inconsistant findings, that don't fit the diagnosis
	 * @param dx
	 * @return
	 */
	private String getCandidate(ConceptEntry candidate){
		if(ontology == null)
			return "";
		IClass c = OntologyHelper.getConceptClass(candidate, ontology);
		if(c != null){
			Set<IClass> children = new HashSet<IClass>();
			for(IClass child: c.getSubClasses()){
				children.add(child);
			}
			for(ConceptEntry e: problem.getConcepts()){
				c = OntologyHelper.getConceptClass(e, ontology);
				if(c != null && children.contains(c))
					return UIHelper.getTextFromName(c.getName());
			}
		}
		return "";
	}
	
	
	/**
	 * is there a similar finding in case that was already asserted?
	 * @param finding
	 * @return
	 */
	private ConceptEntry getSimilarFinding(ConceptEntry finding){
		return getSimilarFinding(finding,getConcepts());
	}
	
	/**
	 * is there a similar finding in case that was already asserted?
	 * @param finding
	 * @return
	 */
	private ConceptEntry getSimilarFinding(ConceptEntry finding,Collection<ConceptEntry> concepts){
		// try to detect full findings
		IClass feature = getFeature(getConceptClass(finding,ontology));
		for(ConceptEntry e: concepts){
			// if this is an entire finding, then ...
			if(e.isFinding() && e.equals(e.getParentEntry())){
				IClass p = getConceptClass(e.getFeature(), ontology);
				if(p != null && p.equals(feature))
					return e;
			}
		}
		return null;
	}

	/**
	 * was similar finding suggested by level hint
	 * @param e
	 * @return
	 *
	private boolean isSuggested(ConceptEntry e){
		return false;
		//return suggestedFinding != null && suggestedFinding.getFeature().equals(e.getFeature());
	}
	*/
	

	public Tutor getTutor() {
		return tutor;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public ScenarioSet getSupportedScenarioSet(){
		if(supportedScenarios == null){
			supportedScenarios = new ScenarioSet();
			try {
				supportedScenarios.load(getClass().getResourceAsStream(OntologyHelper.DEFAULT_REPORT_HELP_FILE));
			} catch (IOException e) {
				Config.getLogger().severe(TextHelper.getErrorMessage(e));
			}
		}
		return supportedScenarios;
	}
	
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("show-state".equals(cmd)){
			debugShowState();
		}else if("clear-state".equals(cmd)){
			problemStack.clear();
			incompleteFindings.clear();
			JOptionPane.showMessageDialog(Config.getMainFrame(),"All Problems are Gone!");
			debugShowState();
		}
		
	}

}
