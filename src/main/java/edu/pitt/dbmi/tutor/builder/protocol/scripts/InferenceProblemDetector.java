package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.DIAGNOSES;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.HAS_ANCILLARY;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.HAS_CLINICAL;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.HAS_FINDING;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.HAS_NO_FINDING;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.createFinding;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getConceptClass;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getFeature;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getInstance;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.isAncillaryStudy;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.isClinicalFeature;

import java.io.PrintStream;
import java.util.*;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IInstance;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.IRestriction;


public class InferenceProblemDetector implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule source;
	private ExpertModule expert;
	
	public String getDescription() {
		return "Detect Confirmed Diagnosis that are not inferred";
	}

	public String getName() {
		return "Inference Problem Detector";
	}

	public String toString() {
		return getName();
	}

	public void initialize() {
		expert = new DomainExpertModule();
		expert.load();
		
	}

	public void dispose() {
	
	}
	public void setOutput(PrintStream out) {
		output = out;
	}
	
	public void setProtocolModule(ProtocolModule m) {
		source = m;
		
	}
	
	/**
	 * this is where magic happens
	 */
	public boolean process(Session session) {
		final String s = ", ";
		
		// load knowledge bases
		if(expert.getDomain().length() == 0 || !expert.getDomain().equals(session.getDomain())){
			expert.openDomain(session.getDomain());
		}
		
		// keep track of instance in 
		IOntology  ontology = expert.getDomainOntology();
		IInstance inst = ontology.getClass(OntologyHelper.CASES).createInstance();
		Set<String> problems = new LinkedHashSet<String>();
		
		// iterage over all client events
		for (ClientEvent ce : session.getClientEvents()) {
			
			// keep track of what findings we currently have
			if(Arrays.asList(TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ATTRIBUTE).contains(ce.getType())){
				boolean important = true;
				
				ConceptEntry entry = getConceptEntry(ce);
				
				// check if we got Not Important feedback
				if(!session.getTutorResponses(ce).isEmpty())
					important = !OntologyHelper.isIrrelevant(session.getTutorResponses(ce).get(0).getError());
				
				
				// add/remove instance
				if(ACTION_ADDED.equals(ce.getAction())){
					if(important)
						addConceptInstance(entry, inst);
				}else if(ACTION_REMOVED.equals(ce.getAction())){
					removeConceptInstance(entry,inst);
				}
			}
			
			
			// if we add diagnosis which is confirmed, check if it is cool
			if(TYPE_DIAGNOSIS.equals(ce.getType()) && ACTION_ADDED.equals(ce.getAction())){
				if(!session.getTutorResponses(ce).isEmpty()){
					TutorResponse tr = session.getTutorResponses(ce).get(0);
					if(RESPONSE_CONFIRM.equals(tr.getResponse()) && ERROR_OK.equals(tr.getError())){
						// now we have confiremed "inferred" response
						//debugShowState(inst);
						Collection<String> inferred = getImpliedDiagnoses(inst);
						//System.out.println("inferred dx:\t"+inferred);
						
						// if DX not there, we have a problem
						if(!inferred.contains(ce.getLabel())){
							problems.add(session.getUsername()+s+OntologyHelper.getCaseName(session.getCase())+s+session.getSessionID()+s+ce.getLabel());
						}
					}
					
				}
			}
		}
		inst.delete();
		
		for(String problem: problems)
			output.println(problem);
		
		return true;
	}
	
	/**
	 * used for debugging
	 */
	private void debugShowState(IInstance instance){
		IOntology ontology = instance.getOntology();
		System.out.println("\n--- instance ["+instance.getName()+"]---");
		System.out.println("clinical:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_CLINICAL))));
		System.out.println("findings:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_FINDING))));
		System.out.println("no findings:\t"+Arrays.toString(instance.getPropertyValues(ontology.getProperty(HAS_NO_FINDING))));
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
	 * add concept entry to the state of the problem
	 * @param entry
	 */
	private void addConceptInstance(ConceptEntry e, IInstance inst){
		IOntology ontology = expert.getDomainOntology();
		// in reallity we care about findings
		ConceptEntry entry = e.getParentEntry();
		if(entry == null)
			entry = e;
		
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
		addFinding(entry,inst,prop);
		
	}

	/**
	 * add concept entry to the state of the problem
	 * @param entry
	 */
	private boolean hasConceptInstance(ConceptEntry e, IInstance inst){
		IOntology ontology = expert.getDomainOntology();
		// in reallity we care about findings
		ConceptEntry entry = e.getParentEntry();
		if(entry == null)
			entry = e;
		
			
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
		if(hasFinding(entry,inst,prop))
			return true;
		
		return false;
	}
	
	
	/**
	 * remove concept entry 
	 * @param entry
	 */
	private void removeConceptInstance(ConceptEntry e, IInstance inst){
		IOntology ontology = expert.getDomainOntology();
		// in reallity we care about findings
		ConceptEntry entry = e.getParentEntry();
		if(entry == null)
			entry = e;

		// if removing an attribute, then we need to refine it, really
		ConceptEntry previous = null;
		if(TYPE_ATTRIBUTE.equals(e.getType())){
			expert.resolveConceptEntry(entry);
			List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(entry.getAttributes());
			//List<ConceptEntry> attributes = getAttributes(entry.getFeature());
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
			
		// determine the property
		IClass c = getConceptClass(entry, ontology);
		String prop = HAS_FINDING;
		if(entry.isAbsent())
			prop = HAS_NO_FINDING;
		else if(isClinicalFeature(c))
			prop = HAS_CLINICAL;
		else if(isAncillaryStudy(c))
			prop = HAS_ANCILLARY;
		
		// if remove was successfull, then re-add more general finding
		if(removeFinding(entry,inst,prop)){
			// re-add a more general finding if available
			if(previous != null)
				addFinding(previous,inst,prop);
		}else{
			// try to remove finding itself not just parent finding, in 
			// case that info is no longer correct
			removeFinding(e,inst,prop);
		}
			
	}
	
	
	
	/**
	 * add finding to case
	 * @param e
	 */
	private void addFinding(ConceptEntry finding, IInstance instance, String property){
		IOntology ontology = expert.getDomainOntology();
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
		IOntology ontology = expert.getDomainOntology();
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
		IOntology ontology = expert.getDomainOntology();
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
	 * get a list of attributes mentioned by user for a given feature
	 * @param feature
	 * @return
	 *
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
	*/
	
	/**
	 * get a list of implided diagnoses
	 */
	public Collection<String> getImpliedDiagnoses(IInstance inst) {
		// do the work
		Set<String> list = new HashSet<String>();
		for (IClass diseaseCls: expert.getDomainOntology().getClass(DIAGNOSES).getSubClasses()){
			// else continue
			ILogicExpression exp = diseaseCls.getEquivalentRestrictions();
			if (exp != null && !exp.isEmpty()) {
				// does the evidence as as fit the bill to call DX
				if(evaluateExpression(exp,inst)){
					list.add(diseaseCls.getName());
				}
			}
		}
		return list;
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
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 */
	private Set<String> evaluate(Object exp, Object param, Set<String> matched) {
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
}
