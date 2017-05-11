package edu.pitt.dbmi.tutor.modules.pedagogic;

import java.util.*;

import javax.swing.JOptionPane;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.PedagogicModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.StudentModule;
import edu.pitt.dbmi.tutor.ui.DomainSelectorPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;


public class RandomCaseSequence implements PedagogicModule {
	private ExpertModule expert;
	private ProtocolModule protocol;
	private StudentModule student;
	private Properties defaultConfig;
	private DomainSelectorPanel domainSelector;
	private Set<String> solvedCases;
	private List<String> cases;
	private String previousDomain;
	
	
	public int getCaseCount() {
		return cases != null?cases.size():0;
	}

	public int getCaseOffset() {
		return -1;
	}

	
	/**
	 * get next case
	 */
	public String getNextCase() {
		String domain = expert.getDomain();
		
		// check if domain is chosen
		if(TextHelper.isEmpty(domain)){
			if(domainSelector == null){
				domainSelector = new DomainSelectorPanel(expert);
			}
			domainSelector.setVisitedCases(getSolvedCases());
			domainSelector.showChooserDialog();
			if(domainSelector.isSelected()){
				domain = (String) domainSelector.getSelectedObject();
			}else{
				return null;
			}
		}
		
		// get all available cases for a given domain  if domain changed
		if(cases == null || !domain.equals(previousDomain)){
			cases = new ArrayList<String>();
			Collections.addAll(cases,expert.getAvailableCases(domain));
		}
		
		// remove solved cases 
		cases.removeAll(getSolvedCases());
		
		// pick a random case
		String problem = null;
		if(!cases.isEmpty()){
			int x = Math.round((float)(1000*Math.random()))%cases.size();
			problem = cases.get(x);
		}else{
			int r = JOptionPane.showConfirmDialog(Config.getMainFrame(),
					"<html>You have solved all of the cases in the <font color=blue>"+TextHelper.getName(domain)+"</font> domain.\n"+
					"Would you like to select another domain?","Question",
					JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
			if(r == JOptionPane.YES_OPTION){
				expert.closeDomain();
				return getNextCase();
			}
		}
		
		// save domain
		previousDomain = domain;
		
		// add to solved cases
		if(problem != null)
			getSolvedCases().add(problem);		
		
		
		// strip metadata 
		if(problem != null)
			problem = OntologyHelper.stripURLQuery(problem);
		
		return problem;
	}

	/**
	 * get list of solved cases
	 * @return
	 */
	private Set<String> getSolvedCases(){
		if(solvedCases == null){
			solvedCases = new LinkedHashSet<String>();
			
			// load solved cases from the
			if(protocol != null && protocol.isEnabled() && !Config.DEFAULT_USER.equals(Config.getUsername()))
				lookupSolvedCases();
			
		}
		return solvedCases;
	}
	
	private void lookupSolvedCases(){
		(new Thread(new Runnable(){
			public void run(){
				// see if this use wanted to foget his case history
				Properties p = protocol.getUserInfo(Config.getUsername());
				Date t = null;
				if(p.containsKey(USER_INFO_FORGET_CASES_BEFORE))
					t = TextHelper.parseDate(p.getProperty(USER_INFO_FORGET_CASES_BEFORE));
				
				// load solved cases from the protocol
				for(Session s: protocol.getSessions(Query.createUsernameQuery(Config.getUsername()))){
					if(OUTCOME_FINISHED.equals(s.getOutcome()) && !TextHelper.isEmpty(s.getCase())){
						if(t == null || t.before(s.getStartTime()))
							solvedCases.add(s.getCase());
					}
				}
				
				// set visited cases
				if(domainSelector != null)
					domainSelector.setVisitedCases(getSolvedCases());
			}
		})).start();
	}
	
	
	public void load() {
		
	}

	public void setExpertModule(ExpertModule module) {
		expert = module;

	}

	public void setProtocolModule(ProtocolModule module) {
		protocol = module;

	}

	public void setStudentModule(StudentModule module) {
		student = module;

	}

	public void sync(PedagogicModule tm) {}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * get default configuration
	 */
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}


	public String getDescription() {
		return "Returns random non-solved cases in the selected domain";
	}

	public String getName() {
		return "Random Case Sequence";
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	public void receiveMessage(Message msg) {}

	public void reset() {}

	public void resolveAction(Action action) {}

}
