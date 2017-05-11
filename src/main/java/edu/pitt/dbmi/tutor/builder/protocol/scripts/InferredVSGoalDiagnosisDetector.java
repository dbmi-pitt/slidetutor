package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.DIAGNOSES;


import java.io.PrintStream;
import java.util.*;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IInstance;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.IRestriction;


public class InferredVSGoalDiagnosisDetector implements ProtocolScript {
	private PrintStream output;
	private ProtocolModule source;
	private ExpertModule expert;
	private Map<String,List<String>> inferredButNotGoalDiagnoses = new HashMap<String, List<String>>();
	
	public String getDescription() {
		return "Find cases where there is a discrepency between goal and diagnosis and the ones inferred by the tutor";
	}

	public String getName() {
		return "Inferred vs Goal Diagnosis Detector";
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
		
		//get cases
		String caseName = session.getCase();
		
		// skip link and feature questions
		if(caseName.contains("PF") || caseName.contains("PL"))
			return true;
		
		// check for question type
		if(caseName.lastIndexOf("?") > -1){
			caseName = caseName.substring(0,caseName.lastIndexOf("?"));
		}
		
		// get delta diagnosis for the case
		List<String> deltaDx = inferredButNotGoalDiagnoses.get(caseName);
		if(deltaDx == null){
			// open case for the first time
			CaseEntry ce = expert.getCaseEntry(caseName);
			
			// get goal dx and inferred dx from given 
			List<String> goalDx  = new ArrayList<String>(ce.getConcepts(DIAGNOSES).keySet());
			Collection<String> inferredDx = getImpliedDiagnoses2(ce.createInstance(ontology));
			deltaDx = new ArrayList<String>(inferredDx);
			
			// find delta by looking at all inferred dx and removing goals
			for(ListIterator<String> li = deltaDx.listIterator();li.hasNext();){
				if(goalDx.contains(li.next()))
					li.remove();
			}
			inferredButNotGoalDiagnoses.put(caseName,deltaDx);
			System.out.println(caseName+" : "+goalDx+" vs "+inferredDx+" | delta: "+deltaDx);
		}
		
		//no check if user ever put a DX (confirmed or not) that is in delta
		
		// iterage over all client events
		for (ClientEvent ce : session.getClientEvents()) {
			// if we add diagnosis which is confirmed, check if it is cool
			if(TYPE_DIAGNOSIS.equals(ce.getType()) && ACTION_ADDED.equals(ce.getAction())){
				if(deltaDx.contains(ce.getLabel()))
					output.println(session.getUsername()+s+OntologyHelper.getCaseName(session.getCase())+s+session.getSessionID()+s+ce.getLabel());
			}
		}
		
		return true;
	}
	
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
	 * get a list of implided diagnoses
	 */
	public Collection<String> getImpliedDiagnoses2(IInstance inst) {
		// do the work
		Set<String> list = new HashSet<String>();
		for (IClass diseaseCls: expert.getDomainOntology().getClass(DIAGNOSES).getSubClasses()){
			// else continue
			ILogicExpression exp = diseaseCls.getEquivalentRestrictions();
			if (exp != null && !exp.isEmpty()) {
				// does the evidence as as fit the bill to call DX
				if(evaluateExpression2(exp,inst)){
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
	 * evaluate expression
	 * 
	 * @param exp
	 * @param inst
	 * @return
	 */
	private boolean evaluateExpression2(ILogicExpression exp, IInstance inst) {
		if (exp.getExpressionType() == ILogicExpression.OR) {
			for (Object o : exp) {
				if (evaluate2(evaluate2(o, inst),termCount(exp), inst))
					return true;
			}
			return false;
		} else {
			return evaluate2(evaluate2(exp, inst),termCount(exp), inst);
		}
	}
	
	/**
	 * evaluate based on number of hits (as oppose to logical operation)
	 * 
	 * @param hits
	 * @param inst
	 * @return
	 */
	private boolean evaluate2(int hits, int expTerms, IInstance inst) {
		
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
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 */
	private int evaluate2(Object exp, Object param) {
		int hits = 0;
		if (exp instanceof ILogicExpression) {
			// iterate over parameters
			for (Object obj : (ILogicExpression) exp) {
				hits += evaluate2(obj, param);
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
				if (evaluate2(value,values[i]) > 0)
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
}
