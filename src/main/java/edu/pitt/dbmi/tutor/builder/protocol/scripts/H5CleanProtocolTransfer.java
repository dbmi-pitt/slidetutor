package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ErrorEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.dbmi.tutor.beans.ShapeEntry;
import edu.pitt.dbmi.tutor.builder.protocol.TransferWizard;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.feedback.HelpManager;
import edu.pitt.dbmi.tutor.modules.feedback.InstructionManager;
import edu.pitt.dbmi.tutor.modules.interfaces.ArcNodeInterface;
import edu.pitt.dbmi.tutor.modules.interfaces.QuestionInterface;
import edu.pitt.dbmi.tutor.modules.presentation.SimpleViewerPanel;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseSession;
import edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.FileSession;
import edu.pitt.dbmi.tutor.modules.reasoning.SimpleDiagnosticReasoner;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IOntologyException;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.text.tools.TextTools;

/**
 * fix h5 protocol
 * 
 * @author tseytlin
 * 
 */
public class H5CleanProtocolTransfer implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule target,source;
	private TransferWizard transfer;
	private Exclusions exclusions;
	private ScenarioSet scenarios;
	private ExpertModule expert;
	private Map<String,ConceptEntry> conceptRegistry;
	private CaseEntry caseEntry;
	private Map<String,Object> findingInputMap;
	private Session currentSession;
	private TreeNode findingTree, diagnosisTree;
	private HashSet<String> skippedSessions;
	
	public final String ATTRIBUTE_HINT_AFTER_COMPLETE = "Stuck Attribute Hint";
	public final String LOCATION_HINT_AFTER_REFINE = "Stuck Location Hint";
	public final String FORGOTEN_DIAGNOSIS = "Forgotten Diagnosis";
	public final String WRONG_DONE_MESSAGE = "Wrong Done Message";
	public final String WRONG_DIAGNOSIS_MESSAGE = "Wrong Diagnosis Message";
	public final String REPEATED_SESSION = "Repeated Session";
	private final String TUTOR_ERROR = "tutor_error";
	
	
	public String getDescription() {
		return "Clean up of H5 dataset. Copies fixed version of protocol to destination specified in the code ";
	}

	public String getName() {
		return "H5 Clean Protocol Transfer";
	}

	public String toString() {
		return getName();
	}

	public void initialize() {
		// initialize target
		skippedSessions = new HashSet<String>();
		
		target = new FileProtocolModule();
		
		File targetDir = new File("/home/tseytlin/Output/H5-Protocol");
		if(!targetDir.exists())
			targetDir.mkdirs();
		((FileProtocolModule) target).setProtocolDirectory(targetDir);
		
		/*target = new DatabaseProtocolModule();
		target.getDefaultConfiguration().setProperty("protocol.driver","com.mysql.jdbc.Driver");
		target.getDefaultConfiguration().setProperty("protocol.url","jdbc:mysql://micron.remotes.upmc.edu/protocolZ");
		target.getDefaultConfiguration().setProperty("protocol.username","user");
		target.getDefaultConfiguration().setProperty("protocol.password","resu");*/
	
		// initialize transfer
		transfer = new TransferWizard();
	
		// initialize exclusions
		exclusions = new Exclusions();
		exclusions.loadClientEventList(new File("/home/tseytlin/Data/H6/H6 Problem Session Client Events.csv"));
		
		// expert module
		expert = new DomainExpertModule();
		expert.load();
		
	}
	

	public void dispose() {
	
	}

	public void setOutput(PrintStream out) {
		output = out;
	}
	/**
	 * remove client event from session
	 * @param session
	 */
	private void removeClientEvent(Session session,ClientEvent ce){
		// remove interface events and tutor responses associated with this event
		session.getInterfaceEvents().removeAll(session.getInterfaceEvents(ce));
		session.getTutorResponses().removeAll(session.getTutorResponses(ce));
		//DON"T worry about NodeEvents, they didn't exist back then
		//session.getNodeEvents().removeAll(session.getTutorResponses(ce));
		session.getClientEvents().remove(ce);
	}
	
	private void restampClientEvent(Session session, ClientEvent ce, long deltaTime){
		// update timestamps in CE and corresponding IEs and TRs
		// don't mind NE, since it has not been generated yet
		ce.setTimestamp(ce.getTimestamp()-deltaTime);
		for(InterfaceEvent ie: session.getInterfaceEvents(ce)){
			ie.setTimestamp(ie.getTimestamp()-deltaTime);
		}
		for(TutorResponse tr: session.getTutorResponses(ce)){
			tr.setTimestamp(tr.getTimestamp()-deltaTime);
		}
	}
	
	private void restampeOrphanedInterfaceEvents(Session session,ClientEvent ce,List<InterfaceEvent> orphanedInterfaceEvents, long deltaTime){
		// get a list of all events before a client event
		List<InterfaceEvent> ies = new ArrayList<InterfaceEvent>();
		for(InterfaceEvent ie: orphanedInterfaceEvents){
			if(ce == null || ie.getTimestamp() <= ce.getTimestamp())
				ies.add(ie);
			else
				break;
		}
		
		// update orphaned events
		if(deltaTime > 0){
			for(InterfaceEvent ie: ies){
				ie.setTimestamp(ie.getTimestamp()-deltaTime);
			}
		}
				
		//remove
		orphanedInterfaceEvents.removeAll(ies);
		
	}
	
	
	private long getFirstTimestamp(Session session, ClientEvent ce){
		//if(!session.getInterfaceEvents(ce).isEmpty())
		//	return session.getInterfaceEvents(ce).get(0).getTimestamp();
		return ce.getTimestamp();
	}
	
	/**
	 * this is where magic happens
	 */
	public boolean process(Session session) {
		// is session excluded?
		if (exclusions.isExcludedSession(session.getSessionID())) {
			skippedSessions.add(session.getUsername()+"-"+OntologyHelper.getCaseName(session.getCase()));
			return true;
		}

		// did session crash?
		// check if there is no end interface event at the end of the session
		if(session.getFinishTime() == null || session.getFinishTime().getTime() - session.getStartTime().getTime() >  24*60*60*1000){
			return true;
			
		}
		
		// check for other types of crashes
		if(session.getClientEvents().isEmpty())
			return true;
		
		
		currentSession = session;
		
		
		// load knowledge bases
		if(expert.getDomain().length() == 0 || !expert.getDomain().equals(session.getDomain())){
			expert.openDomain(session.getDomain());
			
			// setup tree nodes
			findingTree = expert.getTreeRoot(OntologyHelper.DIAGNOSTIC_FEATURES,new OntologyHelper.FindingFilter(expert.getDomainOntology()));
			diagnosisTree = expert.getTreeRoot(OntologyHelper.DIAGNOSES);
		}
	
		// load case
		caseEntry = expert.getCaseEntry(session.getCase());
		
			
		// load in help document
		if(scenarios == null){
			scenarios = new ScenarioSet();
			try {
				scenarios.load(getClass().getResource("/resources/TutorHelp.xml").openStream());
			} catch (IOException e) {
				output.println(TextHelper.getErrorMessage(e));
				return false;
			}
		}
		

		// reset all error codes and response concepts
		for (TutorResponse tr : session.getTutorResponses()) {
			// reset error code
			ScenarioEntry se = scenarios.getScenarioEntry(tr.getError());
			if (se != null)
				tr.setCode("" + se.getErrorCode());
			else
				tr.setCode("0");
			// reset response concept
			tr.setResponseConcept(tr.getObjectDescription());
			if(TextHelper.isEmpty(tr.getSource()))
				tr.setSource(SimpleDiagnosticReasoner.class.getSimpleName());
		}

		// reset sources
		for(ClientEvent ce: session.getClientEvents()){
			if(TextHelper.isEmpty(ce.getSource())){
				if(TYPE_PRESENTATION.equals(ce.getType()) || TYPE_INFO.equals(ce.getType()))
					ce.setSource(SimpleViewerPanel.class.getSimpleName());
				else if(ce.getType().startsWith("Hint"))
					ce.setSource(HelpManager.class.getSimpleName());
				else if("InstructionManager".equals(ce.getLabel()))
					ce.setSource(InstructionManager.class.getSimpleName());
				else if(session.getCase().matches(".*_PF\\d+.*"))
					ce.setSource(QuestionInterface.class.getSimpleName());
				else
					ce.setSource(ArcNodeInterface.class.getSimpleName());
			}
		}
		
		// reset interface sources
		for(InterfaceEvent ie: session.getInterfaceEvents()){
			if(TextHelper.isEmpty(ie.getSource())){
				if(TYPE_PRESENTATION.equals(ie.getType()) || ie.getLabel().contains("Clinical"))
					ie.setSource(SimpleViewerPanel.class.getSimpleName());
				else if(TYPE_START.equals(ie.getType()) || TYPE_END.equals(ie.getType()))
					ie.setSource(null);
				else if("Finish Question".equals(ie.getLabel()))
					ie.setSource(InstructionManager.class.getSimpleName());
				else if(session.getCase().matches(".*_PF\\d+.*"))
					ie.setSource(QuestionInterface.class.getSimpleName());
				else
					ie.setSource(ArcNodeInterface.class.getSimpleName());
			}
		}
		
		
		
		// reset tree expension events
		/*
		for (InterfaceEvent ie : session.getInterfaceEvents()) {
			if (TYPE_BUTTON.equals(ie.getType())) {
				ie.setAction(ACTION_SELECTED);
			}else if(TYPE_TREE.equals(ie.getType()) && ACTION_SELECTED.equals(ie.getAction())) {
				ie.setAction(ACTION_EXPANDED);
			}
		}
		*/
		
		// keep track of correct registry
		Set<String> confirmedLocatedFindings = new LinkedHashSet<String>();
		Set<String> correctConcepts = new HashSet<String>();
		Set<String> correctNotSupportedDx = new HashSet<String>();
		Map<String,String> featureMap = new HashMap<String, String>();
		Map<String,String> currentFindings = new LinkedHashMap<String, String>();
		Map<String,String> registry = new HashMap<String, String>();
		Set<String> currentDiagnoses = new LinkedHashSet<String>();
		Map<String,ClientEvent> deletedDiagnoses = new HashMap<String,ClientEvent>();
		Map<String,String> forgottenDiagnoses = new LinkedHashMap<String,String>();
		Map<String,ClientEvent> deletedConcepts = new HashMap<String,ClientEvent>();
		List<ClientEvent> attributeEvents = new ArrayList<ClientEvent>();
		conceptRegistry = new LinkedHashMap<String,ConceptEntry>();
		findingInputMap = new HashMap<String, Object>();
		Set<String> problems = new LinkedHashSet<String>();
		//ClientEvent lastCE = null;
		String [] nextStep = new String [6];
		long deltaTime = 0;
		long firstRemoveCETimestamp = 0;
		List<ClientEvent> viewerEvents = new ArrayList<ClientEvent>();
		List<InterfaceEvent> orphanedInterfaceEvents = new ArrayList<InterfaceEvent>();
		//int trOffset = 0;
		// create a list of interface events that are not linked to client events
		for(InterfaceEvent ie: session.getInterfaceEvents()){
			if(ie.getClientEventId() <= 0){
				orphanedInterfaceEvents.add(ie);
			}
		}
		
		
		// iterage over all client events
		for (ClientEvent ce : new ArrayList<ClientEvent>(session.getClientEvents())) {
			//trOffset+=session.getTutorResponses(ce).size();
			
			// remember viewer events if inside removal gap
			if(firstRemoveCETimestamp != 0 && TYPE_PRESENTATION.equals(ce.getType())){
				viewerEvents.add(ce);
			}
			
			// check if client event should be skipped
			if(exclusions.isRemovedClientEvent(ce.getMessageId())){
				// remove interface events and tutor responses associated with this event
				removeClientEvent(session, ce);
				
				// remember the first removed timestamp
				if(firstRemoveCETimestamp == 0)
					firstRemoveCETimestamp = getFirstTimestamp(session, ce);
				
				// save concept ID of the removed CE
				if(!TextHelper.isEmpty(ce.getId())){
					deletedConcepts.put(ce.getId(),ce);
					String s = ce.getEntireConcept();
					if(s != null && getId(s) != null)
						deletedConcepts.put(getId(s),ce);
				}
				
				// remove all accumulated viewer events that are in between deletes
				for(ClientEvent ve: viewerEvents){
					removeClientEvent(session, ve);
				}
				viewerEvents.clear();
				continue;
			// we get a CE that is after the gap AND not a presentation event that could be in between
			}else if(firstRemoveCETimestamp != 0 && !TYPE_PRESENTATION.equals(ce.getType())){
				long time = getFirstTimestamp(session, ce);
				
				// find non auto event that preceeds this event
				// and use that as a first event to do delta calc
				if(!viewerEvents.isEmpty()){
					for(ClientEvent ve: viewerEvents){
						if(!ve.isAuto()){
							time = getFirstTimestamp(session,ve);
							break;
						}
					}
				}
				
				// if we had a remove gap, then add those gaps up
				deltaTime += (time - firstRemoveCETimestamp);
				firstRemoveCETimestamp = 0;
				
				//reset timestamps for viewer events that were just after removal block
				for(ClientEvent ve: viewerEvents){
					// if viewer event was automatic, then remove else restamp
					if(ve.isAuto())
						removeClientEvent(session, ve);
					else
						restampClientEvent(session, ve, deltaTime);
				}
				viewerEvents.clear();
			}
					
			// update orphaned interface events 
			restampeOrphanedInterfaceEvents(session,ce,orphanedInterfaceEvents,deltaTime);
			
			
			// update timestamps in CE and corresponding IEs and TRs
			// don't mind NE, since it has not been generated yet
			restampClientEvent(session, ce, deltaTime);
			
			// reset hint label
			if ("HintLevel".equals(ce.getType()))
				ce.setLabel(ce.getLabel().replaceAll(" / ", " of "));

			// reset hint response type to HINT from previous CONFIRM
			if (ce.getType().startsWith("Hint")) {
				for (TutorResponse tr : session.getTutorResponses(ce)) {
					tr.setResponse(RESPONSE_HINT);
				}
			}
			
			// fix duplicate IE Button events AND replace action w/ selected
			boolean oneButtonFound = false;
			for(InterfaceEvent ie : new ArrayList<InterfaceEvent>(session.getInterfaceEvents(ce))){
				// remove duplicat button events
				if(TYPE_BUTTON.equals(ie.getType())){
					if(oneButtonFound){
						session.getInterfaceEvents(ce).remove(ie);
						session.getInterfaceEvents().remove(ie);
					}else{
						ie.setAction(ACTION_SELECTED);
						oneButtonFound = true;	
					}
				}
			}
			
			// spifify the tree events
			for(InterfaceEvent ie : session.getInterfaceEvents(ce)){
				// remove duplicat button events
				if(TYPE_TREE.equals(ie.getType())){
					String ec = getEntireConcept(ce.getInputString());
					if(ec != null && ie.getLabel().endsWith(ec)){
						ie.setAction(ACTION_SELECTED);
					}else{
						ie.setAction(ACTION_EXPANDED);
					}
					// reset label
					String path = getTreePath((ce.getType().contains("Finding")?findingTree:diagnosisTree),ie.getLabel());
					if(path != null)
						ie.setLabel(path);
				}
			}
			
			
			
			// remove extra tutor response on CE  (under certain conditions)
			/*
			List<TutorResponse> trs = session.getTutorResponses(ce);
			if(trs.size() > 1){
				// now remove any trs that are ERROR DIAGNOSIS IS CORRECT
				List<TutorResponse> torem = new ArrayList<TutorResponse>();
				for(int i=1;i<trs.size();i++){
					if(ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED.equals(trs.get(i).getError()))
						torem.add(trs.get(i));
				}
				// remove tutor responses
				trs.removeAll(torem);
				session.getTutorResponses().removeAll(torem);
			}
			*/
			
			// now check if this CE references a removed CE id
			// dont' worry about timestamps
			if(ce.getId() != null && deletedConcepts.containsKey(ce.getId())){
				// if removing a concept, then remove this CE cause it is stale delete this event
				removeClientEvent(session, ce);
				continue;
			}
			
			// concept ID of a parent is removed
			if(!TextHelper.isEmpty(ce.getParent())){
				String [] p = ce.getParent().split("[:\\.]");
				if(TYPE_SUPPORT_LINK.equals(ce.getType()) && p.length >= 6){
					if(deletedConcepts.containsKey(p[2]) || deletedConcepts.containsKey(p[5])){
						// if one of the link components is now missing, just get rid of it
						removeClientEvent(session, ce);
						continue;
					}
				}else if(TYPE_ATTRIBUTE.equals(ce.getType()) && p.length >= 3 && deletedConcepts.containsKey(p[2])){
					ConceptEntry similar = findSimilarFeature(ce,currentFindings);
					if(similar != null){
						ce.setParent(similar.getObjectDescription());
					}else{
						removeClientEvent(session, ce);
						continue;
					}
				}
			}
					
			

			// if adding finding/attribute or whatever
			if (ce.getAction().equals(ACTION_ADDED)) {
				// add if concept was correct
				if(!session.getTutorResponses(ce).isEmpty()) {
					TutorResponse tr = session.getTutorResponses(ce).get(0);
					if (RESPONSE_CONFIRM.equals(tr.getResponse())) {
						correctConcepts.add(ce.getObjectDescription());
					}
					registry.put(ce.getObjectDescription(),tr.getError());
					
					// register concept
					ConceptEntry e = ConceptEntry.getConceptEntry(ce.getObjectDescription());
					if(RESPONSE_FAILURE.equals(tr.getResponse()))
						e.addError(tr.getError());
					conceptRegistry.put(e.getId(),e);
				}
			} else if (ce.getAction().equals(ACTION_REMOVED)) {
				for (TutorResponse tr : session.getTutorResponses(ce)) {
					// check if removing correct findings
					if (correctConcepts.contains(ce.getObjectDescription())) {
						tr.setError(ERROR_REMOVED_CORRECT_CONCEPT);
						tr.setCode("555");
					}else{
						tr.setCode("5");
					}
				}
				registry.remove(ce.getObjectDescription());
				
				// unregister
				conceptRegistry.remove(ce.getId());
			}
			
			// create a mapping of entire concept to feature maps
			if(ce.getAction().equals(ACTION_ADDED)){
				if(ce.getType().equals(TYPE_FINDING)){
					featureMap.put(getEntireConcept(ce.getInputString()),ce.getObjectDescription());
				}else if(ce.getType().equals(TYPE_ATTRIBUTE)){
					featureMap.put(getEntireConcept(ce.getInputString()),ce.getParent());
				}
			}
			
			// try to detect missing attribute issues
			// first this code tracks all of the entire registry throut session
			if(Arrays.asList(TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ATTRIBUTE).contains(ce.getType())){
				String featureConcept = null;
				String entireConcept = getEntireConcept(ce.getInputString());
				
				if(TYPE_ATTRIBUTE.equals(ce.getType()))
					featureConcept = ce.getParent();
				else
					featureConcept = ce.getObjectDescription();
				
				// skip nulls
				if(featureConcept == null || entireConcept == null)
					continue;
				
				// now keep track of findings
				if(ACTION_ADDED.equals(ce.getAction())){
					// on add keep the most up2date entire concept
					currentFindings.put(featureConcept,entireConcept);
				}else if(ACTION_REMOVED.equals(ce.getAction())){
					// if attribute is removed, update entire concept
					// else just remove the feature
					if(TYPE_ATTRIBUTE.equals(ce.getType())){
						currentFindings.put(featureConcept,removeFindingAttribute(ce));
					}else{
						currentFindings.remove(featureConcept);
					}
						
				}
			}
			
			// now switch finding and feature places
			if(ACTION_REFINE.equals(ce.getAction()) && featureMap.containsKey(ce.getLabel())){
				String featureDescription = featureMap.get(ce.getLabel());
				
				ce.setObjectDescription(featureDescription);
				ce.setLabel(getLabel(featureDescription));
				ce.setId(getId(featureDescription));
			}
			
			// remove phantom finding delete on attribute remove
			if(ACTION_REMOVED.equals(ce.getAction()) && TYPE_ATTRIBUTE.equals(ce.getType())){
				List<TutorResponse> trs = new ArrayList<TutorResponse>(session.getTutorResponses(ce));
				if(trs.size() > 1){
					for(TutorResponse tr: trs){
						if(TYPE_FINDING.equals(tr.getType()) || TYPE_ABSENT_FINDING.equals(tr.getType())){
							session.getTutorResponses().remove(tr);
							session.getTutorResponses(ce).remove(tr);
						}
					}
				}
			}
			
			// look for repeated "Dx correct, but inconsistent
			if(ACTION_ADDED.equals(ce.getAction())){
				for(TutorResponse tr : new ArrayList<TutorResponse>(session.getTutorResponses(ce))){
					if(ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED.equals(tr.getError())){
						if(correctNotSupportedDx.contains(tr.getLabel())){
							session.getTutorResponses().remove(tr);
							session.getTutorResponses(ce).remove(tr);
						}
						correctNotSupportedDx.add(tr.getLabel());
					}
				}
			}
			
			// mark correct diagnosis for a case (not just inferred)
			if(ACTION_ADDED.equals(ce.getAction())  && TYPE_DIAGNOSIS.equals(ce.getType())){
				for(TutorResponse tr : session.getTutorResponses(ce)){
					if(RESPONSE_CONFIRM.equals(tr.getResponse())){
						if(caseEntry.getConcepts(DIAGNOSES).containsKey(ce.getLabel())){
							tr.setError(ERROR_DIAGNOSIS_IS_CORRECT);
							tr.setCode("500");
						}else{
							tr.setError(ERROR_DIAGNOSIS_IS_SUPPORTED_BUT_NOT_GOAL);
							tr.setCode("600");
						}	
					}
				}
			}
			
			// reassign incorrect attribute location error to attribute
			if(TYPE_ATTRIBUTE.equals(ce.getType()) && ACTION_ADDED.equals(ce.getAction())){
				List<TutorResponse> trs = session.getTutorResponses(ce);
				if(trs.size() > 1){
					// if first response is confirm, and second is Incorrect Attribute Location, then ...
					TutorResponse tr = trs.get(0);
					TutorResponse ctr = trs.get(1);
					if(RESPONSE_CONFIRM.equals(tr.getResponse())){
						if(RESPONSE_FAILURE.equals(ctr.getResponse()) && ERROR_ATTRIBUTE_IN_INCORRECT_LOCATION.equals(ctr.getError())){
							// remove the confirm
							session.getTutorResponses().remove(tr);
							session.getTutorResponses(ce).remove(tr);
						}
					}
					
					// now lets see if there were other attributes
					for(ClientEvent ace: attributeEvents){
						for(TutorResponse atr: session.getTutorResponses(ace)){
							atr.setResponse(ctr.getResponse());
							atr.setError(ctr.getError());
							atr.setCode(ctr.getCode());
						}
					}
				}else if(trs.size() == 1 && RESPONSE_CONFIRM.equals(trs.get(0).getResponse())){
					attributeEvents.add(ce);
				}
			}else{
				// reset array when it is not an added attribute
				attributeEvents.clear();
			}
			
			// add TR to info
			//if(TYPE_INFO.equals(ce.getType())){
				// if no TR
			if(session.getTutorResponses(ce).isEmpty()){
				// copy info from previous event
				TutorResponse r = new TutorResponse();
				r.setResponse(RESPONSE_CONFIRM);
				r.setError(ERROR_OK);
				r.setClientEvent(ce);
				r.setCode("0");
				r.setTimestamp(ce.getTimestamp());
				r.setResponseConcept(ce.getObjectDescription());
				
				r.setType(nextStep[0]);
				r.setLabel(nextStep[1]);
				r.setAction(nextStep[2]);
				r.setId(nextStep[3]);
				r.setParent(nextStep[4]);
				r.setSource(SimpleDiagnosticReasoner.class.getSimpleName());
			
				// add tr
				session.getTutorResponses().add(r);
				//session.getTutorResponses(ce).add(r);
			}
			//}
			
			
			// save finding location
			if(TYPE_FINDING.equals(ce.getType()) && (ACTION_ADDED.equals(ce.getAction()) || ACTION_REFINE.equals(ce.getAction()))){
				findingInputMap.put(ce.getObjectDescription(),ce.getInput());
			}
			
			
			/////////////////////////////////////////////////////////////////////////////////
			// mark problem sessions
			//
			
			// keep track of dx
			if(TYPE_DIAGNOSIS.equals(ce.getType())){
				if(ACTION_ADDED.equals(ce.getAction())){
					currentDiagnoses.add(ce.getLabel());
					
					// if DX that was removed was forgotten, mark it
					if(forgottenDiagnoses.containsKey(ce.getLabel()) && !session.getTutorResponses(ce).isEmpty())
						addError(session.getTutorResponses(ce).get(0),forgottenDiagnoses.get(ce.getLabel()));
					else if(problems.contains(WRONG_DONE_MESSAGE) || problems.contains(WRONG_DIAGNOSIS_MESSAGE) || problems.contains(FORGOTEN_DIAGNOSIS)){
						if(deletedDiagnoses.containsKey(ce.getLabel())){
							ClientEvent ce1 = deletedDiagnoses.get(ce.getLabel());
							if(!session.getTutorResponses(ce1).isEmpty())
								addError(session.getTutorResponses(ce1).get(0),WRONG_DONE_MESSAGE);
							if(!session.getTutorResponses(ce).isEmpty())
								addError(session.getTutorResponses(ce).get(0),WRONG_DONE_MESSAGE);
						}
					}
					deletedDiagnoses.remove(ce.getLabel());
				}else if(ACTION_REMOVED.equals(ce.getAction())){
					currentDiagnoses.remove(ce.getLabel());
					deletedDiagnoses.put(ce.getLabel(),ce);
					// if DX that was removed was forgotten, mark it
					if(forgottenDiagnoses.containsKey(ce.getLabel()) && !session.getTutorResponses(ce).isEmpty())
						addError(session.getTutorResponses(ce).get(0),forgottenDiagnoses.get(ce.getLabel()));
				}
			}
			
			if(TYPE_FINDING.equals(ce.getType()) && ACTION_REFINE.equals(ce.getAction())){
				for(TutorResponse tr : session.getTutorResponses(ce)){
					if(RESPONSE_CONFIRM.equals(tr.getResponse())){
						// add entire finding
						confirmedLocatedFindings.add(ce.getLabel());
						if(featureMap.containsKey(ce.getLabel()))
							confirmedLocatedFindings.add(featureMap.get(ce.getLabel()));
					}else if(RESPONSE_FAILURE.equals(tr.getResponse())){
						confirmedLocatedFindings.remove(ce.getLabel());
						if(featureMap.containsKey(ce.getLabel()))
							confirmedLocatedFindings.remove(featureMap.get(ce.getLabel()));
					}
				}
			}else if(TYPE_HINT.equals(ce.getType())){
				// if useless, mark it
				if(exclusions.isUselessHint(ce.getMessageId()))
					ce.getInputMap().put("useless","true");
				else if(exclusions.isMisleadingHint(ce.getMessageId()))
					ce.getInputMap().put("misleading","true");
				
				// see if it has problems
				for(TutorResponse tr : session.getTutorResponses(ce)){
					// if hint is about incorrect location 
					if(tr.getError().matches("Incorrect .*Location")){
						// AND you have a confirmed finding that should have been fixed, then we got a winner
						if(confirmedLocatedFindings.contains(tr.getLabel())){
							problems.add(LOCATION_HINT_AFTER_REFINE);
							addError(tr,LOCATION_HINT_AFTER_REFINE);
						}
					// if hint is about missing attribute
					}else if(tr.getError().matches("Missing .*Attribute")){
						// extract finding from tutor response text
						String finding = getFindingFromText(tr);
						
						// if we get a hint in regard to missing attribute AND
						// we already have this finding on the screen, then we got hint statter
						if(currentFindings.values().contains(finding)){
							problems.add(ATTRIBUTE_HINT_AFTER_COMPLETE);
							addError(tr,ATTRIBUTE_HINT_AFTER_COMPLETE);
						}else{
							// make sure that we don't have a sub class of the label
							IClass cls = getClass(finding,expert.getDomainOntology());
							if(cls != null){
								for(String s: currentFindings.values()){
									IClass c = getClass(s,expert.getDomainOntology());
									if(cls.hasSubClass(c)){
										problems.add(ATTRIBUTE_HINT_AFTER_COMPLETE);
										addError(tr,ATTRIBUTE_HINT_AFTER_COMPLETE);
										break;
									}else if(cls.hasSuperClass(c) && isGoal(c)){
										problems.add(ATTRIBUTE_HINT_AFTER_COMPLETE);
										addError(tr,ATTRIBUTE_HINT_AFTER_COMPLETE);
										break;
									}
									
								}
							}
						}
					// if hint talks about missing dx 
					}else if(tr.getError().equals(Constants.HINT_MISSING_DIAGNOSIS)){
						if(currentDiagnoses.contains(tr.getLabel())){
							problems.add(FORGOTEN_DIAGNOSIS);
							forgottenDiagnoses.put(tr.getLabel(),FORGOTEN_DIAGNOSIS);
							addError(tr,FORGOTEN_DIAGNOSIS);
						}
					}
				}
			}
			
			// see if we say something that we should not 
			if((TYPE_DIAGNOSIS.equals(ce.getType()) || TYPE_HYPOTHESIS.equals(ce.getType())) && ACTION_ADDED.equals(ce.getAction())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && ERROR_DIAGNOSIS_IS_TOO_GENERAL.equals(tr.getError())){
						IClass cls = getClass(ce.getLabel(), expert.getDomainOntology());
						if(cls != null && cls.getEquivalentRestrictions().isEmpty()){
							problems.add(WRONG_DIAGNOSIS_MESSAGE);
							forgottenDiagnoses.put(tr.getLabel(),WRONG_DIAGNOSIS_MESSAGE);
							addError(tr,WRONG_DIAGNOSIS_MESSAGE);
						}
					}
				}
			}
			
			// check done
			if(TYPE_DONE.equals(ce.getType())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					// check if true done????
					if(exclusions.isChangeTrueDoneClientEvent(ce.getMessageId())){
						tr.setResponse(RESPONSE_CONFIRM);
						tr.setError(ERROR_OK);
						tr.setCode("0");
					}
					// check for error
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && ERROR_CASE_NEEDS_DIAGNOSIS_TO_FINISH.equals(tr.getError())){
						if(!currentDiagnoses.isEmpty()){
							problems.add(WRONG_DONE_MESSAGE);
							forgottenDiagnoses.put(tr.getLabel(),WRONG_DONE_MESSAGE);
							addError(tr,WRONG_DONE_MESSAGE);
						}
					}
				}
			}
			
			
			/////////////////////////////////////////////////////////////////////////////////
			// figure out next step
			if(!session.getTutorResponses(ce).isEmpty()){
				for(TutorResponse tr:  session.getTutorResponses(ce)){
					//for(TutorResponse tr: session.getTutorResponses(ce)){
					// set response concept (this may skip stuff like multiple responses for single event)
					tr.setResponseConcept(cleanDescription(ce.getObjectDescription()));
					
					String origLabel = null;
					// next step is essentially correct on failures
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && !TYPE_DONE.equals(ce.getType())){
						nextStep[0] = tr.getType();
						nextStep[1] = tr.getLabel();
						nextStep[2] = (tr.getError().contains("Location"))?ACTION_REFINE:ACTION_REMOVED;
						nextStep[3] = tr.getId();
						nextStep[4] = tr.getParent();
						
						// overwrite response concept in case of failure
						tr.setResponseConcept(cleanDescription(tr.getObjectDescription()));
					// on hint we can reuse next step
					}else if(RESPONSE_HINT.equals(tr.getResponse()) && !TYPE_HINT_LEVEL.equals(tr.getType())){
						origLabel = tr.getLabel();
						nextStep[0] = getTypeFromError(tr.getError());
						nextStep[1] = TYPE_DONE.equals(nextStep[0])?TYPE_DONE:getLabelFromError(tr.getError(),tr.getLabel());
						nextStep[2] = (tr.getInputString().toLowerCase().contains("delete"))?ACTION_REMOVED:ACTION_ADDED;
						nextStep[3] = (tr.getError().contains("Missing") || tr.getError().contains("Yet") || ACTION_ADDED.equals(nextStep[2]))?null:tr.getId();
						nextStep[4] = tr.getParent();
						
						// figure out parent for attributes
						if(TYPE_ATTRIBUTE.equals(nextStep[0])){
							ConceptEntry a = getNextStepAttribute(tr, registry);
							nextStep[1] = a.getName();
							nextStep[4] = a.getProperties().get("parent");
						}
						
						// if hint is useless, then marke next step as useless
						if(ce.getInputMap().containsKey("useless")){
							nextStep[0] = "USELESS";
							nextStep[1] = "USELESS";
							nextStep[2] = "USELESS";
							nextStep[3] = null;
							nextStep[4] = null;
						}
						
					}else if(TYPE_HINT_LEVEL.equals(tr.getType())){
						// never recalculate next step on level hint
					// now figure out the next step for resolved goals unless ID is null, which means that 
					}else if(isNextStepResolved(nextStep,registry)){
						nextStep = getNextStep(caseEntry,currentFindings.values(),registry.keySet());
					}
					
					
					// reset type for hypos and diagnosis and for refines
					if(Arrays.asList(TYPE_DIAGNOSIS,TYPE_HYPOTHESIS).contains(nextStep[0]) || ACTION_REFINE.equals(nextStep[2]))
						nextStep[4] = null;
					
					// check if there is a problem with diagnosis
					//if(TYPE_DIAGNOSIS.equals(nextStep[0]) && currentDiagnoses.contains(nextStep[1])){
					// overwrite next step
					//	nextStep = getNextStep(caseEntry,currentFindings.values(),registry.keySet());
					//}
					
					
					// reset next step
					tr.setType(nextStep[0]);
					tr.setLabel(nextStep[1]);
					tr.setAction(nextStep[2]);
					if(nextStep[3] == null)
						tr.remove("id");
					else
						tr.setId(nextStep[3]);
					if(nextStep[4] == null)
						tr.remove("parent");
					else
						tr.setParent(nextStep[4]);
					tr.remove("object_description");
					
					
					
					
					// generate node event if necessary
					// generate node event
					if(TYPE_HINT.equals(ce.getType())){
						// if hint is not marked uselss generate NodeEvent
						if(!ce.getInputMap().containsKey("useless")){
							// if this is missing attribute hint we might want to spawn multiple node events
							if(TYPE_ATTRIBUTE.equals(nextStep[0]) && 
							    Arrays.asList(HINT_MISSING_ATTRIBUTE,HINT_MISSING_ANOTHER_ATTRIBUTE).contains(tr.getError())){
								// spawn several events 
								//TODO: what about absent findings????
								for(ConceptEntry a: getCandidateList(new ConceptEntry(origLabel,TYPE_FINDING)))
									doNodeEvent(ce,tr,a);
							}else{
								ConceptEntry ns = new ConceptEntry(nextStep[1],nextStep[0]);
								if(nextStep[3] != null)
									ns.setId(nextStep[3]);
								doNodeEvent(ce,tr,ns);
							}
						}
					}else
						doNodeEvent(ce,tr,ConceptEntry.getConceptEntry(ce.getObjectDescription()));
				}
			}
			
		}
		
		// if there is anything left in orphaned sessions, just update them
		restampeOrphanedInterfaceEvents(session, null, orphanedInterfaceEvents, deltaTime);
		
		// update session duration
		if(session instanceof DatabaseSession && deltaTime > 0)
			((DatabaseSession)session).setFinishTime(new Date(session.getFinishTime().getTime()-deltaTime));

		// is session a repeat
		if(skippedSessions.contains(session.getUsername()+"-"+OntologyHelper.getCaseName(session.getCase())))
			problems.add(REPEATED_SESSION);
				
		// if there were problems, mark the outcome to error
		if(!problems.isEmpty()){
			String outcome = "errors: "+TextHelper.toString(problems);
			if(session instanceof DatabaseSession)
				((DatabaseSession)session).setOutcome(outcome);
			else if(session instanceof FileSession)
				((FileSession)session).setOutcome(outcome);
			
		}
		
		
		// //////////////////////////////
		// NOW lets copy to destination
		output.println("Copying session " + session.getUsername() + " " + getProblem(session.getCase()) + " "
				+ session.getSessionID() + " ....");
		transfer.copy(session, target);

		return true;
	}
	
	/**
	 * find similar concept
	 * @param ce
	 * @return
	 */
	private ConceptEntry findSimilarFeature(ClientEvent ce,Map<String,String> currentFindings){
		String parent = ce.getParent();
		parent = parent.substring(0,parent.length()-getId(parent).length()-1);
		String entireConcept = getEntireConcept(ce.getInputString());
		
		// first lets find a
		for(ConceptEntry e: conceptRegistry.values()){
			// if concept's object description starts with Type.Label of CE's parent, we
			// have a matching feature
			if(e.getObjectDescription().startsWith(parent)){
				// now, for this feature, check if it can form an entire concept
				// get a new list of attributes from the case
				List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(); 
				// get entire finding for a given feature
				IClass fn = getConceptClass(currentFindings.get(e.getObjectDescription()));
				if(fn == null)
					continue;
				// get all attributes associated with this finding
				for(IClass a: OntologyHelper.getAttributes(fn)){
					attributes.add(new ConceptEntry(a.getName(),TYPE_ATTRIBUTE));
				}
				// add attribute in question
				attributes.add(getConceptEntry(ce));
				
				// get child
				ConceptEntry child  = createFinding(e.getFeature(),attributes,expert.getDomainOntology());
				// if the product is a valid entry in the tree, then
				if(entireConcept.equals(child.getName()))
					return e;
				
			}
		}
		return null;
	}
	
	
	private boolean isGoal(IClass c) {
		return caseEntry.getConcept(c.getName()) != null;
	}

	/**
	 * return a new finding after removal of the attribute
	 * @param finding
	 * @param feature
	 * @param attribute
	 * @return
	 */
	private String removeFindingAttribute(ClientEvent ce){
		// refine finding
		ConceptEntry entry = getConceptEntry(ce);
		ConceptEntry parent = entry.getParentEntry();
		
		// get a new list of attributes from the case
		List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(); 
		IClass fn = getConceptClass(parent.getName());
		if(fn == null)
			return getEntireConcept(ce.getInputString());
		
		for(IClass a: OntologyHelper.getAttributes(fn)){
			attributes.add(new ConceptEntry(a.getName(),TYPE_ATTRIBUTE));
		}
		attributes.remove(entry);
		
		ConceptEntry child  = createFinding(entry.getFeature(),attributes,expert.getDomainOntology());
		
		// if the new finding minus a given attribute returns the identical finding as an original
		// that means that attributes are inter linked and remaining attributes are only relevan
		// in the context of another. Since it is difficult to determine which attributes are related
		// and which are not, just remove all of the remaining attributes
		if(child == null || parent.equals(child) || child.equals(entry.getFeature())){
			child = entry.getFeature();
		}
		
		
		return child.getName();
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
	 * find node and return path
	 * @param root
	 * @param label
	 * @return
	 */
	private String getTreePath(TreeNode root, String label) {
		if(root instanceof DefaultMutableTreeNode){
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) root;
			if(n.getUserObject().equals(label))
				return TextHelper.toString(n.getPath());
			// if not there look in children
			for(int i=0;i<n.getChildCount();i++){
				String s = getTreePath(n.getChildAt(i),label);
				if(s != null)
					return s;
			}
		}
		return null;
	}

	/**
	 * add error
	 * @param msg
	 * @param error
	 */
	private void addError(Message msg, String error){
		if(msg.getInput() instanceof Map){
			((Map) msg.getInput()).put(TUTOR_ERROR,error);
		}else{
			Map<String,String> map = new LinkedHashMap<String, String>();
			map.put("input",""+msg.getInput());
			map.put(TUTOR_ERROR,error);
			msg.setInput(map);
		}
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
	
	private String cleanDescription(String desc){
		if(desc == null)
			return desc;
		if(desc.endsWith(".null"))
			return desc.substring(0,desc.length()-".null".length());
		return desc;
	}
	
	
	private boolean isNextStepResolved(String[] nextStep, Map<String,String> registry) {
		// if no next tep yet, then true
		if(nextStep[0] == null)
			return true;
		
		// check if goal was achieved for stuff that was not put in before
		if(nextStep[3] == null){
			for(String od: registry.keySet()){
				String nd = nextStep[0]+"."+nextStep[1];
				if(nextStep[4] != null)
					nd = nextStep[4]+":"+nd;
				if(od.startsWith(nd))
					return true;
			
			}
			return false;
		}
		
		// see if failed nextStep was resolved
		String od = nextStep[0]+"."+nextStep[1]+"."+nextStep[3];
		if(Arrays.asList(TYPE_ATTRIBUTE,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK).contains(nextStep[0])){
			od = nextStep[4]+":"+od;
		}
		
		
		String error = registry.get(od);
		if(error == null || ((ERROR_OK.equals(error) || Arrays.asList(IRRELEVANT_ERRORS).contains(error))))
			return true;
		return false;
	}

	
	private boolean contains(Collection<String> registry, ConceptEntry e){
		for(String s: registry){
			if(s.startsWith(e.getType()+"."+e.getLabel()))
				return true;
		}
		return false;
	}
	
	private ConceptEntry suggestCandidate(Collection<String> registry, ConceptEntry finding){
		expert.resolveConceptEntry(finding);
		ConceptEntry f = finding.getFeature();
		
		// find related features
		List<String> related = new ArrayList<String>();
		for(String s: registry){
			if(s.startsWith(f.getType()+"."+f.getLabel())){
				related.add(s);
			}
		}
		
		// 
		if(related.isEmpty())
			return finding;
		
		Collections.sort(related);
		
		String prefix = null, nakedPrefix = null;
		List<ConceptEntry> attrs = new ArrayList<ConceptEntry>(finding.getAttributes());
		for(String s: related){
			// if looks like a feature, make it a prefix
			if(s.indexOf(':') < 0){
				if(attrs.size() == finding.getAttributes().size()){
					prefix = s;
					if(nakedPrefix == null)
						nakedPrefix = s;
				}else
					break;
			// else an attribute
			}else if (prefix != null){
				// check attributes
				if(prefix.equals(nakedPrefix))
					nakedPrefix = null;
				boolean nomatch = true;
				for(ConceptEntry a: finding.getAttributes()){
					// if this attribute is in there
					if(s.contains(a.getName())){
						attrs.remove(a);
						nomatch = false;
					}
				}
				// if nomatch 
				if(nomatch)
					prefix = null;
			}
		}
		
		// check naked prefix
		if(prefix == null && nakedPrefix != null)
			prefix = nakedPrefix;
		
		// if we do have a prefix
		if(prefix != null && !attrs.isEmpty()){
			ConceptEntry a = attrs.get(0);
			a.getProperties().put("parent",prefix);
			return a;
		}
		
		return finding;
	}
	
	
	
	
	private boolean contains(Collection<String> registry, String e){
		if(registry.contains(e))
			return true;
		
		// check if something is a bit more specific then needs to be
		IClass cls = expert.getDomainOntology().getClass(e);
		if(cls != null){
			for(String s: registry){
				IClass c = expert.getDomainOntology().getClass(s);
				if(c != null){
					if(c.hasSuperClass(cls))
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * get next step from goal list
	 * @return
	 */
	private String[] getNextStep(CaseEntry problem, Collection<String> correctConcepts, Collection<String> registry) {
		// check if there are unidentified concepts in case
		List<ConceptEntry> candidates = problem.getConcepts(DIAGNOSTIC_FEATURES).getValues();
		candidates.addAll(problem.getConcepts(DIAGNOSES).getValues());
		
		// select a candidate w/ lowest power that has not been found
		ConceptEntry candidate = null;
		for(ConceptEntry e : candidates){
			// make sure that concept was not found already
			if(!(contains(correctConcepts,e.getName()) || contains(registry,e))  && (TYPE_DIAGNOSIS.equals(e.getType()) || e.isImportant())){
				candidate = e;
				break;
			}
		}
		
		// check if we already have a feture for a finding
		if(candidate != null && Arrays.asList(TYPE_FINDING,TYPE_ABSENT_FINDING).contains(candidate.getType())){
			candidate = suggestCandidate(registry, candidate);
		}
		
		
		String [] nextStep = new String [6];
		if(candidate != null){
			// chekc if it is a diagnosis and no hx
			if(TYPE_DIAGNOSIS.equals(candidate.getType())){
				ConceptEntry hx = candidate.clone();
				hx.setType(TYPE_HYPOTHESIS);
				if(!contains(registry,hx)){
					candidate = hx;
				}
			}
			
			nextStep[0] = candidate.getType();
			nextStep[1] = candidate.isFinding()?getFeature(getConceptClass(candidate.getName())).getName():candidate.getName();
			nextStep[2] = ACTION_ADDED;
			if(TYPE_ATTRIBUTE.equals(candidate.getType()))
				nextStep[4] = candidate.getProperties().get("parent");
		}else{
			nextStep[0] = TYPE_DONE;
			nextStep[1] = TYPE_DONE;
			nextStep[2] = ACTION_ADDED;
		}
		return nextStep;
	}
	
	
	public IClass getConceptClass(String nm){
		if(nm == null)
			return null;
		
		IOntology ontology = expert.getDomainOntology();
		// find class for candidate
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

	private String getLabelFromError(String error, String label){ //, Map<String,String> registry) {
		String type = getTypeFromError(error);
		IClass finding = getConceptClass(label);
		IClass feature = getFeature(finding);
		// if type finding/ get feature
		if(type.contains(TYPE_FINDING)){
			return feature.getName();
		}
		
		/*
		else if(type.equals(TYPE_ATTRIBUTE)){
			// get attributes for a finding
			for(IClass a : getAttributes(finding)){
				boolean contains = false;
				for(String od : registry.keySet()){
					// if object description of something in the registry
					// contains this attribute AND its feature
					if(!(od.contains(a.getName()) && od.contains(feature.getName()))){
						contains = true;
						break;
					}
				}
				if(!contains){
					return a.getName();
				}
			}
		}
		*/
		return label;
	}
	
	private ConceptEntry getNextStepAttribute(TutorResponse tr,Map<String,String> registry){
		// get what you have so far
		IClass finding = getConceptClass(tr.getLabel());
		IClass fullFinding = getConceptClass(getFindingFromText(tr));
		
		// get feature
		IClass feature = getFeature(finding);
		List<IClass> ca = getAttributes(finding);
		List<IClass> na = fullFinding != null?getAttributes(fullFinding):new ArrayList<IClass>();
		// remove current attributes from list of existing attributes
		na.removeAll(ca);
		
		String name = finding.getName();
		if(!na.isEmpty()){
			name = na.get(0).getName();
		}else if(!ca.isEmpty())
			name = ca.get(ca.size()-1).getName();
			
		ConceptEntry a = new ConceptEntry(name,TYPE_ATTRIBUTE);
		// set feature
		a.setFeature(new ConceptEntry(feature.getName(),TYPE_FINDING));
		for(String od : registry.keySet()){
			if(od.contains("."+feature.getName()+".")){
				a.getProperties().put("parent",od);
				break;
			}
		}
		return a;
		
	}
	
	
	
	private String getTypeFromError(String error) {
		if(error.toLowerCase().contains("support"))
			return TYPE_SUPPORT_LINK;
		else if(error.contains(TYPE_DIAGNOSIS) || error.contains("Diagnoses"))
			return TYPE_DIAGNOSIS;
		else if(error.contains(TYPE_HYPOTHESIS) || error.contains("Hypotheses"))
			return TYPE_HYPOTHESIS;
		else if(error.contains(TYPE_ATTRIBUTE))
			return TYPE_ATTRIBUTE;
		else if(error.contains("Absent Finding"))
			return TYPE_ABSENT_FINDING;
		else if(HINT_CASE_SOLVED.equals(error))
			return TYPE_DONE;
		return TYPE_FINDING;
	}

	/**
	 * extract entire concept
	 * 
	 * @param input
	 * @return
	 */
	private String getEntireConcept(String input) {
		String entireConcept = null;
		// extract parent feature and entire concept
		Map<String, String> map = (Map<String, String>) TextHelper.parseMessageInput(input);
		if (map != null && map.containsKey("entire_concept")) {
			entireConcept = getLabel(map.get("entire_concept"));
		}
		return entireConcept;
	}

	private String getLabel(String str) {
		String[] p = str.split("\\.");
		if (p.length == 3)
			str = p[1];
		return str;
	}
	
	private String getId(String str) {
		String[] p = str.split("\\.");
		if (p.length == 3)
			str = p[2];
		return str;
	}
	
	private String getProblem(String str) {
		int a = str.lastIndexOf("/");
		int b = str.lastIndexOf(".");
		if (a != -1 && b != -1)
			return str.substring(a + 1, b);
		return str;
	}
	
	private IClass getClass(String nm, IOntology ontology) {
		IClass candidate = ontology.getClass(nm);
		if (candidate == null && nm.indexOf(",") > -1) {
			// assume that first entry in a sequence is a valid class
			candidate = ontology.getClass(nm.substring(0, nm.indexOf(",")).trim());
		}
		return candidate;
	}
	
	/**
	 * get finding from text
	 * 
	 * @param tr
	 * @return
	 */
	private String getFindingFromText(TutorResponse tr) {
		String finding = tr.getLabel();
		Map<String, String> map = (Map<String, String>) TextHelper.parseMessageInput(tr.getInputString());
		if (map.containsKey("message-3")) {
			String msg = map.get("message-3");
			Pattern pt = Pattern.compile("<b>([\\w- ]+)</b>");
			Matcher mt = pt.matcher(msg);
			if (mt.find()) {
				finding = createClassName(mt.group(1));
			}
		}

		return finding;
	}
	

	private String createClassName(String name) {
		// convert to protege friendly name first
		name = name.trim().replaceAll("\\s*\\(.+\\)\\s*", "").replaceAll("[^\\w\\-]", "_").replaceAll("_+", "_");

		// if single-word and mixed case, then leave it be
		// this excludes weird names like pT2a or pMX
		if (!name.matches("[a-z]+[A-Z]+[a-z0-9]*")) {
			// do camelBack notation
			StringBuffer nm = new StringBuffer();
			for (String n : name.toLowerCase().split("_")) {
				String w = (TextTools.isStopWord(n)) ? n : ("" + n.charAt(0)).toUpperCase() + n.substring(1);
				nm.append(w + "_");
			}
			name = nm.toString().substring(0, nm.length() - 1);
		}
		return name;
	}

	public void setProtocolModule(ProtocolModule m) {
		source = m;
		
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
			final ConceptEntry entry = (input instanceof ConceptEntry)?(ConceptEntry)input:null;
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
			if(TYPE_ABSENT_FINDING.equals(entry.getType())){
				nodeEvent.setAbsent(true);
				nodeEvent.setType(TYPE_FINDING);
			}
			
			// set action hint
			if(RESPONSE_HINT.equals(nodeEvent.getResponse())){
				nodeEvent.setAction(RESPONSE_HINT);
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
				}
			}else if(TYPE_HYPOTHESIS.equals(nodeEvent.getType()) || TYPE_DIAGNOSIS.equals(nodeEvent.getType())){
				// if client event type and node event type don't match, that means that
				// there was a tutor response generated for some new evidence, and sent to correct the status of
				// the Dx or Hx, hence we should not send a node event
				if(!TYPE_HINT.equals(clientEvent.getType()) && !nodeEvent.getType().equals(clientEvent.getType()))
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
			// set one to many set
			if(ACTION_ADDED.equals(nodeEvent.getAction())){
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
				
				// set description
				nodeEvent.setObjectDescription(entry.getObjectDescription());
			}
			
			// reset error code
			if(nodeEvent.getTutorResponse() != null){
				nodeEvent.setEntireConcept(nodeEvent.getTutorResponse().getEntireConcept());
				nodeEvent.setCode(nodeEvent.getTutorResponse().getCode());
			}
			
			// output node event
			nodeEvent.setSource(SimpleDiagnosticReasoner.class.getSimpleName());
			nodeEvent.setTimestamp(nodeEvent.getTutorResponse().getTimestamp());
		
			//save event
			currentSession.getNodeEvents().add(nodeEvent);
			//currentSession.getNodeEvents(tutorResponse).add(nodeEvent);
			
		}
	}
	
	private ConceptEntry getFindingAtLocation(ConceptEntry entry) {
		// determined where this finding is located
		List<ConceptEntry> findings = OntologyHelper.getMatchingFindings(caseEntry,entry);
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
				for(ShapeEntry e: caseEntry.getLocations(entry)){
					if(e.getShape().contains(r))
						return true;
				}
				return false;
			}else if(map.containsKey("view")){
				String text = ""+((Map)input).get("view");
				Rectangle r = TextHelper.parseRectangle(text);
				// is input located in the location
				for(ShapeEntry e: caseEntry.getLocations(entry)){
					if(r.contains(e.getShape().getBounds()) || e.getShape().intersects(r))
						return true;
				}
				return false;
			}
		}
		return true;
	}
	
	
	private Collection<ConceptEntry> getConcepts() {
		return conceptRegistry.values();
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
		if(ACTION_SELF_CHECK.equals(msg.getAction()))
			return false;
		
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
	 * get features that are part of the diagnostic rule
	 * @param dx
	 * @return
	 */
	private Set<ConceptEntry> getFeatures(ConceptEntry dx){
		Set<ConceptEntry> list = new LinkedHashSet<ConceptEntry>();
		for(ConceptEntry f: getDiagnosticFindings(dx, expert.getDomainOntology())){
			list.add(new ConceptEntry(getFeature(OntologyHelper.getConceptClass(f,expert.getDomainOntology())).getName(),f.getType()));
		}
		return list;
	}
	
	/**
	 * get a list of implied hypotheses based on a given finding
	 * evidence
	 * @return
	 */
	public List<ConceptEntry> getImpliedHypotheses(ConceptEntry finding){
		IOntology ontology = expert.getDomainOntology();
		IClass f = OntologyHelper.getConceptClass(finding, ontology);
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
	
}
