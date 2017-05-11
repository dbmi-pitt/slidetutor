package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.createFinding;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IOntologyException;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.text.tools.TextTools;

public class H5ProblemDetector implements ProtocolScript {
	private PrintStream output;
	private Exclusions exclusions;
	private boolean doCSVOutput = false;
	
	private final String ATTRIBUTE_HINT_AFTER_COMPLETE = "Type 1: Attribute Hint after Complete Finding";
	private final String LOCATION_HINT_AFTER_REFINE = "Type 2: Location Hint after Correct Refine";
	private final String INCORRECT_LOCATION_AFTER_HINT = "Type 3: Incorrect Location after Hint";
	private final String HINT_WITHOUT_LEVEL = "Type 4: Hint without Level";
	private final String FAILED_GOAL = "Type 5: Failed Goal";
	private final String FORGOTEN_DIAGNOSIS = "Type 6: Forgotten Diagnosis";
	private final String WRONG_DONE_MESSAGE = "Type 7: Wrong Done Message";
	private final String WRONG_DIAGNOSIS_MESSAGE = "Type 8: Wrong Diagnosis Message";
	private final String FINDING_IS_IN_CASE = "Type 9: Finding IS in case";
	private final String TUTOR_CRASHED = "Type X: Tutor Crashed";
	private final String EMPTY_SESSION = "Type Y: Empty Session";
	
	private final List<String> ERROR_LIST = Arrays.asList(ATTRIBUTE_HINT_AFTER_COMPLETE,LOCATION_HINT_AFTER_REFINE,
														INCORRECT_LOCATION_AFTER_HINT,HINT_WITHOUT_LEVEL,
														FAILED_GOAL,FORGOTEN_DIAGNOSIS,WRONG_DONE_MESSAGE,
														WRONG_DIAGNOSIS_MESSAGE,TUTOR_CRASHED,EMPTY_SESSION);
	
	
	private Map<String,IOntology> ontologyMap = new HashMap<String, IOntology>();
	private IOntology ontology;
	private CaseEntry caseEntry;
	
	public String getDescription() {
		return "Detect problems in H5 dataset. ";
	}

	public String getName() {
		return "H5 Problem Detector";
	}

	public void dispose() {
	
	}
	public String toString(){
		return getName();
	}
	
	public void initialize() {
		exclusions = new Exclusions();
	}


	public void setOutput(PrintStream out) {
		output = out;
	}
	
	public boolean process(Session session) {
		
		//if(getExclusionList().contains(session.getSessionID()))
		//	return true;
		
		// load knowledge bases
		ontology = ontologyMap.get(session.getDomain());
		if(ontology == null){
			try {
				ontology = POntology.loadOntology(session.getDomain());
				ontologyMap.put(session.getDomain(),ontology);
			} catch (IOntologyException e) {
				e.printStackTrace();
			}
		}
		
		caseEntry = new CaseEntry();
		try {
			caseEntry.load(new URL(session.getCase()).openStream());
		} catch (IOException e) {
			output.println(TextHelper.getErrorMessage(e));
			return false;
		}
		
		// lets get our goal list
		List<String> goalFindings = caseEntry.getConcepts(OntologyHelper.DIAGNOSTIC_FEATURES).getKeys();
		Set<String> goalFeatures = new LinkedHashSet<String>();
		for(ConceptEntry e: caseEntry.getConcepts(OntologyHelper.DIAGNOSTIC_FEATURES).getValues()){
			goalFeatures.add(OntologyHelper.getFeature(OntologyHelper.getConceptClass(e, ontology)).getName());
		}
		Map<String,Set<String>> sessionProblems = new LinkedHashMap<String,Set<String>>();
		
		// detect Incorrect Location not fixed by refine hint
		Set<String> confirmedLocatedFindings = new LinkedHashSet<String>();
		Set<String> hintedLocatedFindings = new LinkedHashSet<String>();
		Map<String,String> currentFindings = new LinkedHashMap<String, String>();
		Set<String> currentDiagnoses = new LinkedHashSet<String>();
		Map<String,String> featureMap = new LinkedHashMap<String, String>();
		TutorResponse previousHint = null;
		boolean hintLevel = false;
		
		// iterate over client events
		for(ClientEvent ce: session.getClientEvents()){
			
			// detect hints without levels
			// if we got previous hint and the next CE is not hint level, then raise flag
			if(previousHint != null && !hintLevel && !TYPE_HINT_LEVEL.equals(ce.getType())){
				//TEMP DISABLE
				addProblem(HINT_WITHOUT_LEVEL,"",sessionProblems);
			}
		
			
			// if not hint level clear previous hint
			if(!ce.getType().startsWith("Hint")){
				previousHint = null;
				hintLevel = false;
			}
			
			
			// look at all refine actions, remember confirms
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
				for(TutorResponse tr : session.getTutorResponses(ce)){
					// if hint is about incorrect location 
					if(tr.getError().matches("Incorrect .*Location")){
						// AND you have a confirmed finding that should have been fixed, then we got a winner
						if(confirmedLocatedFindings.contains(tr.getLabel())){
							addProblem(LOCATION_HINT_AFTER_REFINE,tr.getLabel(),sessionProblems);
						}
					// if hint is about missing attribute
					}else if(tr.getError().matches("Missing .*Attribute")){
						// extract finding from tutor response text
						String finding = getFindingFromText(tr);
						
						// if we get a hint in regard to missing attribute AND
						// we already have this finding on the screen, then we got hint statter
						if(currentFindings.values().contains(finding)){
							addProblem(ATTRIBUTE_HINT_AFTER_COMPLETE,finding,sessionProblems);
						}else{
							// make sure that we don't have a sub class of the label
							IClass cls = getClass(finding,ontology);
							if(cls != null){
								for(String s: currentFindings.values()){
									IClass c = getClass(s,ontology);
									if(cls.hasSubClass(c)){
										//System.err.println(c+" vs "+finding);
										addProblem(ATTRIBUTE_HINT_AFTER_COMPLETE,finding,sessionProblems);
										break;
									}else if(cls.hasSuperClass(c) && isGoal(c)){
										addProblem(ATTRIBUTE_HINT_AFTER_COMPLETE,finding,sessionProblems);
										break;
									}
									
								}
							}
						}
					// if hint talks about missing dx 
					}else if(tr.getError().equals(Constants.HINT_MISSING_DIAGNOSIS)){
						if(currentDiagnoses.contains(tr.getLabel())){
							addProblem(FORGOTEN_DIAGNOSIS, tr.getLabel(), sessionProblems);
						}
					}
					previousHint = tr;
				}
				
			}else if(TYPE_HINT_LEVEL.equals(ce.getType()) && previousHint != null){
				hintLevel = true;
				
				// if hint level mentions location then watch for it
				// get messages from each hint level
				List<String> messages = new ArrayList<String>();
				for(TutorResponse tr: session.getTutorResponses(ce)){
					Map<String,String> map = (Map<String,String>) TextHelper.parseMessageInput(tr.getInputString());
					if(map != null){
						messages.addAll(map.values());
					}
				}
				
				// now go over messages and see if goal finding is mentioned
				for(String msg: messages){
					if(msg.contains("This is the area of")){
						hintedLocatedFindings.add(previousHint.getLabel());
					}
				}
			}
			
			
			// try to detect missing attribute issues
			// first this code tracks all of the entire concepts throut session
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
					featureMap.put(entireConcept,getLabel(featureConcept));
					
					// check that newly added finding is in case, if it is a goal
					if(goalFindings.contains(entireConcept)){
						for(TutorResponse tr: session.getTutorResponses(ce)){
							if(RESPONSE_FAILURE.equals(tr.getResponse()) && ERROR_FINDING_NOT_IN_CASE.equals(tr.getError())){
								addProblem(FAILED_GOAL, entireConcept, sessionProblems);
								break;
							}
						}
					}
					
					
				}else if(ACTION_REMOVED.equals(ce.getAction())){
					// if attribute is removed, update entire concept
					// else just remove the feature
					if(TYPE_ATTRIBUTE.equals(ce.getType())){
						//currentFindings.put(featureConcept,entireConcept);
						currentFindings.put(featureConcept,removeFindingAttribute(ce));
						featureMap.put(entireConcept,getLabel(featureConcept));
					}else{
						currentFindings.remove(featureConcept);
						//featureMap.put(entireConcept,getLabel(featureConcept));
						//confirmedLocatedFindings.remove(current)
					}
						
				}
			}
			
			
			
			// check if incorrect location after hint
			if(TYPE_FINDING.equals(ce.getType()) && (ACTION_ADDED.equals(ce.getAction()) || ACTION_REFINE.equals(ce.getAction()))){
				boolean locationFailure = false;
				
				for(TutorResponse tr : session.getTutorResponses(ce)){
					if(RESPONSE_FAILURE.equals(tr.getResponse())){
						if(tr.getError().matches("Incorrect .* Location")){
							locationFailure = true;
							break;
						}
					}
				}
				
				// if wrong location
				if(locationFailure){
					// if finding is being added or refined
					// check if it was right after hint that showed location
					String entireConcept = getEntireConcept(ce.getInputString());
					
					// check if hint displayed the location before
					for(String finding: hintedLocatedFindings){
						if(finding.contains(entireConcept)){
							addProblem(INCORRECT_LOCATION_AFTER_HINT,entireConcept,sessionProblems);
							break;
						}
					}
				}
				
				
			}
			
			// check if not in case when it should
			if(TYPE_FINDING.equals(ce.getType()) && ACTION_ADDED.equals(ce.getAction())){
				for(TutorResponse tr : session.getTutorResponses(ce)){
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && ERROR_FINDING_NOT_IN_CASE.equals(tr.getError()) && goalFeatures.contains(ce.getLabel())){
						// now we have a problem
						addProblem(FINDING_IS_IN_CASE,getEntireConcept(ce.getInputString()),sessionProblems);
					}
				}
			}
			
			
			// keep track of dx
			if(TYPE_DIAGNOSIS.equals(ce.getType())){
				if(ACTION_ADDED.equals(ce.getAction())){
					currentDiagnoses.add(ce.getLabel());
				}else if(ACTION_REMOVED.equals(ce.getAction())){
					currentDiagnoses.remove(ce.getLabel());
				}
			}
			
			// see if we say something that we should not 
			if((TYPE_DIAGNOSIS.equals(ce.getType()) || TYPE_HYPOTHESIS.equals(ce.getType())) && ACTION_ADDED.equals(ce.getAction())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && ERROR_DIAGNOSIS_IS_TOO_GENERAL.equals(tr.getError())){
						IClass cls = getClass(ce.getLabel(), ontology);
						if(cls != null && cls.getEquivalentRestrictions().isEmpty()){
							addProblem(WRONG_DIAGNOSIS_MESSAGE,ce.getLabel(), sessionProblems);
						}
					}
				}
			}
			
			
			if(TYPE_DONE.equals(ce.getType())){
				for(TutorResponse tr: session.getTutorResponses(ce)){
					if(RESPONSE_FAILURE.equals(tr.getResponse()) && ERROR_CASE_NEEDS_DIAGNOSIS_TO_FINISH.equals(tr.getError())){
						if(!currentDiagnoses.isEmpty()){
							addProblem(WRONG_DONE_MESSAGE,"there is Dx present", sessionProblems);
						}
					}
				}
			}
			
		}
		
		// check if there is no end interface event at the end of the session
		if(session.getFinishTime() == null || session.getFinishTime().getTime() - session.getStartTime().getTime() >  24*60*60*1000){
			addProblem(TUTOR_CRASHED,"no finish timestamp", sessionProblems);
			
		}
		
		// check for other types of crashes
		if(session.getClientEvents().isEmpty())
			addProblem(EMPTY_SESSION,"",sessionProblems);
		
		// check last client event
		List<ClientEvent> ies = session.getClientEvents();
		if(!ies.isEmpty() && TYPE_SUPPORT_LINK.equals(ies.get(ies.size()-1).getType())){
			addProblem(TUTOR_CRASHED,"support link crash", sessionProblems);
		}
		
		
		
		// now report results
		if(!sessionProblems.isEmpty()){
			// do a CSV style output
			if(doCSVOutput){
				StringBuffer problems = new StringBuffer();
				String s = ",";
				// figure out problem types
				for(String key: ERROR_LIST)
					problems.append(s+(sessionProblems.containsKey(key)?1:0));
				output.println(session.getUsername()+s+OntologyHelper.getCaseName(session.getCase())+s+session.getSessionID()+problems);
			}else{
				// write out progress
				String e = exclusions.isExcludedSession(session.getSessionID())?"  (excluded) ":"";
				output.println("Analyzing "+session.getUsername()+" "+OntologyHelper.getCaseName(session.getCase())+" session "+session.getSessionID()+e+" ..");
				// now output list of findings
				for(String s: sessionProblems.keySet())
					output.println("\t"+s+" : "+sessionProblems.get(s));
			}
			
		}
		
		
		return true;
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
		
		ConceptEntry child  = createFinding(entry.getFeature(),attributes,ontology);
		
		// if the new finding minus a given attribute returns the identical finding as an original
		// that means that attributes are inter linked and remaining attributes are only relevan
		// in the context of another. Since it is difficult to determine which attributes are related
		// and which are not, just remove all of the remaining attributes
		if(child == null || parent.equals(child) || child.equals(entry.getFeature())){
			child = entry.getFeature();
		}
		
		
		return child.getName();
	}
	

	public IClass getConceptClass(String nm){
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
	
	
	
	/**
	 * extract entire concept
	 * @param input
	 * @return
	 */
	private String getEntireConcept(String input){
		String entireConcept = null;
		// extract parent feature and entire concept
		Map<String,String> map = (Map<String,String>) TextHelper.parseMessageInput(input);
		if(map != null && map.containsKey("entire_concept")){
			entireConcept = getLabel(map.get("entire_concept"));
		}
		return entireConcept;
	}
	
	private String getLabel(String str){
		String [] p = str.split("\\.");
		if(p.length == 3)
			str = p[1];
		return str;
	}
	
	private IClass getClass(String nm, IOntology ontology){
		IClass candidate = ontology.getClass(nm);
		if(candidate == null && nm.indexOf(",") > -1){
			// assume that first entry in a sequence is a valid class
			candidate= ontology.getClass(nm.substring(0,nm.indexOf(",")).trim());
		}
		return candidate;
	}
	
	
	/**
	 * add problem
	 * @param type
	 * @param info
	 * @param sessionProblems
	 */
	private void addProblem(String type, String info, Map<String,Set<String>> sessionProblems){
		Set<String> list = sessionProblems.get(type);
		if(list == null){
			list = new LinkedHashSet<String>();
			sessionProblems.put(type,list);
		}
		list.add(info);
	}
	
	
	/**
	 * get finding from text
	 * @param tr
	 * @return
	 */
	private String getFindingFromText(TutorResponse tr){
		String finding = tr.getLabel();
		Map<String,String> map = (Map<String,String>) TextHelper.parseMessageInput(tr.getInputString());
		if(map.containsKey("message-3")){
			String msg = map.get("message-3");
			Pattern pt = Pattern.compile("<b>([\\w- ]+)</b>");
			Matcher mt = pt.matcher(msg);
			if(mt.find()){
				finding = createClassName(mt.group(1));
			}
		}
		
		return finding;
	}

	
	private String createClassName(String name){
		// convert to protege friendly name first
		name = name.trim().replaceAll("\\s*\\(.+\\)\\s*","").replaceAll("[^\\w\\-]","_").replaceAll("_+","_");
		
		// if single-word and mixed case, then leave it be
		// this excludes weird names like pT2a or pMX
		if(!name.matches("[a-z]+[A-Z]+[a-z0-9]*")){
			// do camelBack notation
			StringBuffer nm = new StringBuffer();
			for(String n: name.toLowerCase().split("_")){
				String w = (TextTools.isStopWord(n))?n:(""+n.charAt(0)).toUpperCase()+n.substring(1);
				nm.append(w+"_");
			}
			name = nm.toString().substring(0,nm.length()-1);
		}
		return name;
	}
	
	public void setProtocolModule(ProtocolModule m) {
		// TODO Auto-generated method stub
		
	}
}
