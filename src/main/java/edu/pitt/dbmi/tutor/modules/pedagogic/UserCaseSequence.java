package edu.pitt.dbmi.tutor.modules.pedagogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.PedagogicModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.StudentModule;
import edu.pitt.dbmi.tutor.ui.CaseSelectorPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;


public class UserCaseSequence implements PedagogicModule {
	private ExpertModule expert;
	private CaseSelectorPanel caseSelector;
	private ProtocolModule protocol;
	private Properties defaultConfig;
	private List<String> solvedCases;
	
	public String getNextCase() {
		loadSelector();
		
		// prompt for case
		caseSelector.setVisitedCases(getSolvedCases());
		caseSelector.setSelectedDomain(expert.getDomain());
		caseSelector.showDialog(Config.getMainFrame());
	
		String problem = caseSelector.getSelectedCase();
		
		// add to solved case
		getSolvedCases().add(problem);
		
		
		return (problem != null)?OntologyHelper.stripURLQuery(problem):null;
	}

	private void loadSelector(){
		if(caseSelector == null){
			caseSelector = new CaseSelectorPanel(expert);
			caseSelector.setEnableCasePreview(Config.getBooleanProperty(this,"case.preview.enabled"));
			caseSelector.load();
		}
	}
	
	/**
	 * get list of solved cases
	 * @return
	 */
	private List<String> getSolvedCases(){
		if(solvedCases == null)
			solvedCases = new ArrayList<String>();
		return solvedCases;
	}
	
	
	public void setEnableCasePreview(boolean b){
		loadSelector();
		caseSelector.setEnableCasePreview(b);
	}
	
	public void load() {
		// load solved cases from the
		if(protocol != null && !Config.DEFAULT_USER.equals(Config.getUsername())){
			for(Session s: protocol.getSessions(Query.createUsernameQuery(Config.getUsername()))){
				String c = s.getCase();
				int x = c.lastIndexOf("?");
				getSolvedCases().add((x > -1)?c.substring(0,x):c);
			}
		}
	}

	public void setExpertModule(ExpertModule module) {
		expert = module;

	}

	public void setStudentModule(StudentModule module) {
		//NOOP
	}

	public void dispose() {
		reset();
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
		return "Provides users with an interface to select cases manually";
	}

	public String getName() {
		return "User Case Sequence";
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

	public void receiveMessage(Message msg) {
		//NOOP
	}

	public void reset() {
		//NOOP
	}

	public void resolveAction(Action action) {
		//NOOP
	}

	
	/**
	 * set protocol module. Allows pedagogic decisions
	 * to be made based on past student progress 
	 */
	public void setProtocolModule(ProtocolModule module){
		protocol = module;
	}
	
	/**
	 * get total number of cases in the sequence
	 * (if applicable)
	 * @return case count or 0 if not applicable
	 */
	public int getCaseCount(){
		return 0;
	}
	
	
	/**
	 * get the offset of the current case in the sequence
	 * (if applicable)
	 * @return case offset or -1 if not applicable
	 */
	public int getCaseOffset(){
		return -1;
	}
	
	public void sync(PedagogicModule tm) {
	}

}
