package edu.pitt.dbmi.tutor.modules.protocol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;

public class FileProtocolModule implements ProtocolModule {
	public static final String STUDY_PREFIX = "STUDY_";
	public static final String USER_PREFIX = "USER_";
	public static final String PASS_FILE = ".password";
	public static final String LOG_SUFFIX = ".log";
	public static final String CONDITION_PREFIX = "CONDITION_";
	public static final String IE = "IE: ";
	public static final String CE = "CE: ";
	public static final String TR = "TR: ";
	public static final String PE = "PE: ";
	public static final String NE = "NE: ";
	
	private Properties defaultConfig;
	private boolean protocol = true;
	private File directory,userDirectory;
	private Writer logFile;

	public void load(){
		//NOOP:
	}
	
	
	public void setProtocolDirectory(File dir){
		directory = dir;
	}
	
	public File getProtocolDirectory(){
		return directory;
	}
	
	/**
	 * authenticat and create a user directory
	 */
	public boolean authenticateUser(String username, String password, String study) {
		// make sure we are connected
		isConnected();
		
		File studyDir = directory;
		
		// make sure that username is aphanumeric, else quit
		if(!username.matches("\\w+"))
			return false;
		
		// create study directory if appropriate
		if(!TextHelper.isEmpty(study)){
			studyDir = new File(directory,STUDY_PREFIX+study);
			if(!studyDir.exists())
				studyDir.mkdirs();
		}
		
		// now create a user directory
		userDirectory = new File(studyDir,USER_PREFIX+username);
		if(!userDirectory.exists())
			userDirectory.mkdirs();
		
		// create a password file
		savePassword(userDirectory,password);
		
		
		// now create a condition directory
		if(!TextHelper.isEmpty(Config.getCondition())){
			userDirectory = new File(userDirectory,CONDITION_PREFIX+Config.getCondition());
			if(!userDirectory.exists())
				userDirectory.mkdirs();
		}
				
		return true;
	}
	
	/**
	 * save user's password
	 * @param dir
	 * @param content
	 */
	private void savePassword(File dir, String content){
		try{
			FileWriter writer = new FileWriter(new File(dir,PASS_FILE));
			writer.write(content);
			writer.flush();
			writer.close();
		}catch(IOException ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
		}
	}
	

	/**
	 * create a new session log file
	 */
	public void openCaseSession(ProblemEvent pe) {
		// reset just in case
		reset();
		
		// if not authenticated, authenticate
		if(userDirectory == null)
			authenticateUser(pe.getUsername(),Config.getPassword());
		
		// strip potential uri
		String name = OntologyHelper.getCaseName(pe.getCaseURL());
		
		// create filename and writer
		long time = pe.getTimestamp();
		String filename = pe.getUsername()+"-"+name+"-"+TextHelper.formatDate(new Date(time),"M_d_yy-HH_mm_ss")+LOG_SUFFIX;
		try{
			logFile = new BufferedWriter(new FileWriter(new File(userDirectory,filename)));
		}catch(IOException ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
		}
			
		// write out message
		receiveMessage(pe);
	}
	
	
	/**
	 * write out that session has ended and close the session
	 */
	public void closeCaseSession(ProblemEvent pe) {
		// write out message
		receiveMessage(pe);
		
		// close session
		reset();
	}

	public List<String> getCases(Query query) {
		Set<String> list = new TreeSet<String>();
		for(Session s: getSessions(query)){
			list.add(s.getCase());
		}
		return new ArrayList<String>(list);
	}
	
	
	private String filterValue(String val){
		if(val.startsWith("'") && val.endsWith("'"))
			return val.substring(1,val.length()-1).trim();
		return val;
	}
	
	private String filterValue(Set<String> values){
		String val = values.iterator().next();
		if(val.startsWith("'") && val.endsWith("'"))
			return val.substring(1,val.length()-1).trim();
		return val;
	}
	
	
	public List<Session> getSessions(Query query) {
		// make sure we are connected
		isConnected();
		
		// directories to look for sessions
		List<File> dirs = new ArrayList<File>();
		
		// check query object
		if(query.hasValue("username")){
			String exp = "";
			
			// add experiment
			if(query.hasValue("experiment"))
				exp = File.separator+STUDY_PREFIX+filterValue(query.get("experiment"));
			
			// iterate over users
			for(String user: query.get("username")){
				user = USER_PREFIX+filterValue(user);
				
				// create user dir
				File uf = new File(directory.getAbsolutePath()+exp+File.separator+user);
				if(!uf.exists())
					continue;
				
				//iterate over conditions
				if(query.hasValue("experiment_condition_name")){
					for(String cond: query.get("experiment_condition_name")){
						dirs.add(new File(uf,CONDITION_PREFIX+filterValue(cond)));
					}
				}else{
					// look at what is in the user directory
					dirs.add(uf);
					for(File f : uf.listFiles()){
						if(f.isDirectory() && f.getName().startsWith(CONDITION_PREFIX)){
							dirs.add(f);
						}
					}
				}
				
			}
		}
		
		
		// now go over all of the directories
		List<Session> sessions = new ArrayList<Session>();
		
		for(File dir: dirs){
			if(!dir.exists())
				continue;
			
			// now lets get the content of directory
			for(File file: dir.listFiles()){
				if(file.getName().endsWith(LOG_SUFFIX)){
					FileSession s = new FileSession(file);
					if(query.hasValue("case_url")){
						for(String cs: query.get("case_url")){
							cs = filterValue(cs);
							if(cs.contains(s.getCase())){
								sessions.add(s);
								break;
							}
						}
					}else{
						// add all files
						sessions.add(s);
					}
				}
			}
		}
		
		return sessions;
	}
	

	public List<String> getConditions(String experiment) {
		// make sure we are connected
		isConnected();
		
		if(TextHelper.isEmpty(experiment))
			return Collections.EMPTY_LIST;
		
		// find experiment directory
		File dir = new File(directory,STUDY_PREFIX+experiment);
		if(!dir.exists())
			return Collections.EMPTY_LIST;
		
		// recursive list all user folders in study 
		Set<String> list = new TreeSet<String>();
		recursiveList(dir,CONDITION_PREFIX,list);
		
		return new ArrayList<String>(list);
	}

	public List<String> getExperiments() {
		// make sure we are connected
		isConnected();
		
		List<String> result = new ArrayList<String>();
		for(String name : directory.list(getFilter(STUDY_PREFIX))){
			result.add(name.substring(STUDY_PREFIX.length()));
		}
		return result;
	}

	

	public Properties getUserInfo(String username) {
		return new Properties();
	}

	public List<String> getUsers(String experiment) {
		// make sure we are connected
		isConnected();
		
		List<String> result = new ArrayList<String>();
	
		// if experiment is defined
		if(!TextHelper.isEmpty(experiment)){
			// find experiment directory
			File dir = new File(directory,STUDY_PREFIX+experiment);
			if(!dir.exists())
				return Collections.EMPTY_LIST;
			
			// fetch users
			for(String name : dir.list(getFilter(USER_PREFIX))){
				result.add(name.substring(USER_PREFIX.length()));
			}
		// else do a recursive list for users
		}else{
			// recursive list all results
			Set<String> list = new TreeSet<String>();
			recursiveList(directory,USER_PREFIX,list);
			result.addAll(list);
		}
		return result;
	}

	public boolean isEnabled() {
		return protocol;
	}


	public void setEnabled(boolean b) {
		protocol = true;
	}

	public void dispose() {
		reset();
		directory = null;
	}

	public void receiveMessage(Message msg) {
		if(logFile != null){
			try {
				String prefix = "";
				if(msg instanceof ClientEvent){
					prefix = CE;
				}else if(msg instanceof NodeEvent){
					prefix = NE;
				}else if(msg instanceof TutorResponse){
					prefix = TR;
				}else if(msg instanceof InterfaceEvent){
					prefix = IE;
				}else if(msg instanceof ProblemEvent){
					prefix = PE;
				}
				logFile.write(prefix+msg+"\n");
				logFile.flush();
			}catch(IOException ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			}
		}
	}

	public void reset() {
		if(logFile != null){
			try{
				logFile.flush();
				logFile.close();
			}catch(IOException ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			}
		}
		logFile = null;
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
		return "Logs all user interactions with the tutor to a file that is saved on the user's computer";
	}

	public String getName() {
		return "File Protocol Module";
	}


	public String getVersion() {
		return "1.0";
	}
	
	/**
	 * get filename filter for prefix
	 * @param prefix
	 * @return
	 */
	private FilenameFilter getFilter(String p){
		final String prefix = p;
		return new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix);
			}
		};
	}
	
	
	/**
	 * recursive list of directory w/ filename filter
	 * @param dir
	 * @param filter
	 * @param result
	 */
	private void recursiveList(File dir, String prefix, Collection<String> result){
		// handle default
		if(dir == null || !dir.exists() || !dir.isDirectory())
			return;
		// now add results
		for(File f: dir.listFiles()){
			if(f.getName().startsWith(prefix))
				result.add(f.getName().substring(prefix.length()));
			if(f.isDirectory())
				recursiveList(f,prefix,result);
		}
	}
	
	
	// NOOP methods
	public void addCondition(String condition, String experiment) {}
	public void addExperiment(String experiment) {}
	public void addUser(String username, String password, String experiment, Properties p) {
		if(!getUsers(null).contains(username)){
			authenticateUser(username, password);
		}
		
	}
	public boolean authenticateAdministrator(String username, String password) {
		return true;
	}
	public boolean authenticateUser(String username, String password) {
		return authenticateUser(username, password,null);
	}
	public void resolveAction(Action action) {}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public boolean removeCondition(String condition, String experiment) {
		return false;
	}

	public boolean removeExperiment(String experiment) {
		return false;
		
	}

	public boolean removeUser(String username) {
		return false;
	}

	public boolean isConnected() {
		if(directory != null)
			return true;
		// else init directory
		String dir = Config.getFileProperty(this,"protocol.directory");
		// check if dir is absolute or relative, if doesn't start w/ root, then
		// it is relative to home directory
		boolean relative = true;
		for(File r : File.listRoots()){
			if(dir.startsWith(r.getAbsolutePath())){
				relative = false;
				break;
			}
		}
		// now init the directory
		directory = (relative)?new File(System.getProperty("user.home"),dir):new File(dir);
		if(!directory.exists())
			return directory.mkdirs();
		return true;
	}

	public void processMessage(Message msg) {
		receiveMessage(msg);
	}
	
	/**
	 * remove data for a list of given sessions
	 * @return true if successful / else otherwise
	 */
	public boolean removeSessions(List<Session> sessions){
		for(Session s: sessions)
			if(!s.delete())
				return false;
		return true;
	}
}
