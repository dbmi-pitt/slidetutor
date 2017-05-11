package edu.pitt.dbmi.tutor.modules.pedagogic;


import static edu.pitt.dbmi.tutor.messages.Constants.ACTION_OPENED;
import static edu.pitt.dbmi.tutor.messages.Constants.TYPE_DEBUG;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class StaticCaseSequence implements PedagogicModule {
	private final String DEFAULT = "default";
	private ExpertModule  expert;
	private ProtocolModule protocol;
	private int offset = 0;
	private Map<String,List<String>> sequences;
	private Properties defaultConfig;
	
	
	public StaticCaseSequence(){
		sequences = new HashMap<String, List<String>>();
	}
	
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setStudentModule(StudentModule module){
	}
	
	public Set<String> getSequenceKeys(){
		return sequences.keySet();
	}
	
	
	/**
	 * get sequence key 
	 * @return
	 */
	public String getSequenceKey(){
		String key = null;
		String condition = Config.getProperty("tutor.condition");
			
		// pick a right key in sequence file
		if(sequences.containsKey(condition)){
			key = condition;
		}else if(sequences.containsKey(DEFAULT)){
			key = DEFAULT;
		}else if(expert.getDomain() != null){ 
			if(sequences.containsKey(expert.getDomain())){
				key = expert.getDomain();
			}else if(sequences.containsKey(TextHelper.getName(expert.getDomain()))){
				key = TextHelper.getName(expert.getDomain());
			}
		}
		return key;
	}
	
	
	/**
	 * get next case in the sequence
	 * @return
	 */
	public String getNextCase(){
		// first check the sequence by condition
		
		String key = getSequenceKey();
		String problem = null;
		//int total = 0;
		
		// try load offset
		if(offset == 0)
			loadOffset();
		
		
		// if we have a key we found a sequence
		// else just use available cases
		if(key != null){
			int total = sequences.get(key).size();
			// check bounds
			if(offset >= total){
				if(Config.getBooleanProperty(this,"loop.sequence")){
					offset = 0;
				}else{
					if(Config.getBooleanProperty(this,"prompt.at.end"))
						JOptionPane.showMessageDialog(Config.getMainFrame(),"You have reached the end of problem sequence.");
					return null;
				}
			}
			problem = sequences.get(key).get(offset++);
		}else{
			String [] cases = expert.getAvailableCases(expert.getDomain()); 
			//total = cases.length;
			problem =  cases[(offset++)%cases.length];
			
			// strip metadata
			if(problem != null)
				problem = OntologyHelper.stripURLQuery(problem);
			
		}
		
		
		
		// return null by default
		return problem;
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
		String key = getSequenceKey();
		// if we have a key we found a sequence
		// else just use available cases
		if(key != null)
			return sequences.get(key).size();
		return 0;
	}
	
	/**
	 * get list of cases
	 * @return
	 */
	public List<String> getCases(String condition){
		return sequences.get(condition);
	}
	
	/**
	 * get list of cases
	 * @return
	 */
	public Map<String,List<String>> getSequenceMap(){
		return sequences;
	}
	
	
	
	/**
	 * get the offset of the current case in the sequence
	 * (if applicable)
	 * @return case offset or -1 if not applicable
	 */
	public int getCaseOffset(){
		// return decremented value, since the call to next offset
		// already would have incremented it
		return (offset > 0)?offset-1:offset;
	}
	
	
	public void dispose() {
		reset();
		sequences.clear();
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
		return "Returns cases in a preset sequence";
	}

	
	public String getName() {
		return "Static Case Sequence";
	}


	public String getVersion() {
		return "1.0";
	}

	
	public void receiveMessage(Message msg) {
		// TODO Auto-generated method stub

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
	
	public void reset() {
		offset = 0;
	}

	public void setExpertModule(ExpertModule module) {
		expert = module;
		
		addDebugOption();
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
	 * load from default location
	 */
	public void load() {
		String location = Config.getProperty("tutor.case.sequence");
		
		if(TextHelper.isEmpty(location))
			return;
		
		try{
			load(Config.getInputStream(location));
		}catch(Exception ex){
			Config.getLogger().severe("Could not load sequence file from "+location);
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
		}
	}
	
	/**
	 * try to load offset from protocol
	 */
	private void loadOffset(){
		// get offset of the last solved case
		String key = getSequenceKey();
		if(protocol != null && key != null && !Config.DEFAULT_USER.equals(Config.getUsername())){
			// get sessions
			Query query = new Query();
			query.addUsername(Config.getUsername());
			if(!DEFAULT.equals(key))
				query.addCondition(key);
			List<Session> sessions = protocol.getSessions(query);
			
			// normalize case names, so that matching would work
			List<String> cases = new ArrayList<String>();
			for(String s: sequences.get(key)){
				cases.add(TextHelper.getName(s));
			}
			
			// go from last to 
			for(int i=sessions.size()-1;i>= 0;i--){
				Session s = sessions.get(i);
				int n = cases.indexOf(TextHelper.getName(s.getCase()));
				if(n > -1){
					offset = n;
					// if case is finished, then next case should be loaded
					if(Constants.OUTCOME_FINISHED.equals(s.getOutcome())){
						offset ++;
					}
					break;
				}
			}
			
			// if during load I am already at the end, loop
			int total = sequences.get(key).size();
			if(offset >= total){
				offset = 0;
			}
		}
	}
	
	
	/**
     * load
     * @param is
     * @param manager
     * @throws IOException
     */
    public void load(InputStream is) throws IOException {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	String line,field = null;
    	Pattern pt = Pattern.compile("\\[([\\w\\.]+)\\]");
    	sequences = new HashMap<String,List<String>>();
    	while((line = reader.readLine()) != null){
    		line = line.trim();
    		// skip comments
    		if(line.startsWith("#") || line.length() == 0)
    			continue;
    		// extract headers
    		Matcher mt = pt.matcher(line);
    		if(mt.matches()){
    			// save previous field
    			field = mt.group(1);
    			sequences.put(field,new ArrayList<String>());
    		}else if(field != null){
    			sequences.get(field).add(line);
    		}else{
    			// create a default condition
    			field = "default";
    			sequences.put(field,new ArrayList<String>());
    			sequences.get(field).add(line);
    		}
    	}
    	reader.close();
    }

    /**
     * add debug option
     */
    private void addDebugOption(){
		JMenu debug = ITS.getInstance().getDebugMenu();
		String text = "Show Case Sequence ..";
		if(!UIHelper.hasMenuItem(debug,text)){
			debug.add(UIHelper.createMenuItem("",text,
					  UIHelper.getIcon(Config.getProperty("icon.menu.sequence")),
			  new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					doShowSequence();
				}
			}),0);
		}
    }
    
    
    /**
     * get case name for show sequence
     * @param name
     * @return
     */
    private String getCase(String name, int i){
    	StringBuffer str = new StringBuffer();
    	str.append("<td>"+(i+1)+". ");
		String nm = TextHelper.getName(name);
		if(i == offset - 1){
			//nm = "<b>"+nm+"</b>";
			nm = "<font color=green><b><a href=\""+name+"\">"+nm+"</a></b></font>";
		}else{
			nm = "<a href=\""+name+"\">"+nm+"</a>";
		}
		str.append(nm+"</td>");
		return str.toString();
    }
    
    private void doShowSequence(){
    	final String key = getSequenceKey();
    	if(key == null){
    		JOptionPane.showMessageDialog(Config.getMainFrame(),
    		"Case Sequence is not being used for this session","Case Sequence",JOptionPane.WARNING_MESSAGE);
    	}else{
    		
    		UIHelper.HTMLPanel panel = new UIHelper.HTMLPanel();
	    	panel.setEditable(false);
	    	panel.addHyperlinkListener(new HyperlinkListener() {
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if(e.getEventType() == EventType.ACTIVATED){
						final String problem =e.getDescription();
						offset = sequences.get(key).indexOf(problem);
						offset ++;
						((new Thread(new Runnable(){
				    		public void run(){
				    			ITS i = ITS.getInstance();
				    	    	i.getProgressBar().setString("loading "+problem+" ...");
				    			i.setBusy(true);
				    	    	i.openCase(problem);
				    	    	i.setBusy(false);
				    		}
				    	}))).start();
						UIHelper.getWindow((Component)e.getSource()).dispose();
		    			
					}
				}
			});
	    	JScrollPane scroll = new JScrollPane(panel);
	    	scroll.setPreferredSize(new Dimension(500,500));
	    	StringBuffer str = new StringBuffer();
	    	str.append("<center><h2><font color=green>"+key+"</font> case sequence</h2><hr></center><table width=100%>");
	    	List<String> list =  sequences.get(key);
	    	int limit = 15;
	    	// do a 3 column layout
	    	for(int i=0;i<list.size() && i< limit;i++){
	    		// offsets for 2nd and 3rd column
	    		int j = limit+i;
	    		int k = 2*limit+i;
	    		
	    		// do first column
	    		str.append("<tr>"+getCase(list.get(i),i));
	    		
	    		// do second column
	    		if(j < list.size()){
	    			str.append(getCase(list.get(j),j));
	    		}else
	    			str.append("<td></td>");
	    		
	    		// do third column
	    		// do second column
	    		if(k < list.size()){
	    			str.append(getCase(list.get(k),k));
	    		}else
	    			str.append("<td></td>");
	    		
	    		str.append("</th>");
	    		
	    	}
	    	str.append("</table>");
	    	panel.setText(str.toString());
	    	Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show Case Sequence",ACTION_OPENED));
	    	JOptionPane.showMessageDialog(Config.getMainFrame(),scroll,"Case Sequence",JOptionPane.PLAIN_MESSAGE);
    	}
    }
    
    
	public void sync(PedagogicModule tm) {	
	}
}
