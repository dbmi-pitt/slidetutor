package edu.pitt.dbmi.tutor.modules.protocol;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.TextHelper;



public class DatabaseProtocolModule implements ProtocolModule, Runnable {
	private final int ATTEMPT_LIMIT = 1;
	private Properties defaultConfig;
	private LinkedList<Message> messageQueue;
	private boolean stop = true;
	private int session_id;
	private Map<Integer,Integer> ce_table,tr_table;
	private Map<Integer,List<Integer>> ie_table;
	private Connection conn;
	private boolean protocol,oldRepresentation,realOldRepresetation;
	private int attempt = 0;
	private final String RED_ALERT_MESSAGE = "The system encountered a problem persisting protocol data.";
	private Boolean specifyColumnsForReturn;
	
	/**
	 * initialize message sender
	 */
	public DatabaseProtocolModule(){
		messageQueue = new LinkedList<Message>();
		protocol = true;
		session_id = -1;
	}
	
	protected void finalize() throws Throwable {
		dispose();
	}

	public void load(){
		//NOOP:
	}
	
	
	/**
	 * set protocol module on/off
	 * @param b
	 */
	public void setEnabled(boolean b){
		protocol = b;
	}
	
		/**
	 * is module enabled
	 * @return
	 */
	public boolean isEnabled(){
		return protocol;
	}
	

	public void dispose() {
		flush();
		stop();
		
		// close connection
		if(conn != null){
			try{
				conn.close();
			}catch(Exception ex){
				//NOOP
			}
		}
	}

	/**
	 * get prepared statement
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement getPreparedStatement(Connection conn, String sql) throws SQLException{
		PreparedStatement st = null;
		if(specifyColumnsForReturn == null || specifyColumnsForReturn){
			try{
				st = conn.prepareStatement(sql,new int [] {1});
				specifyColumnsForReturn = Boolean.TRUE;
			}catch(SQLException ex){
				st = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
				specifyColumnsForReturn = Boolean.FALSE;
		    }
		}else{
			st = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
		}
		return st;
	}
	
	
	/**
	 * get generated key
	 * @param st
	 * @return
	 * @throws SQLException
	 */
	private int getGeneratedKey(Statement st) throws SQLException{
		int id = -1;
		ResultSet result = st.getGeneratedKeys();
		if(result.next())
			id = result.getInt(1);
		result.close();
		return id;
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
		return "Logs all user interactions with the tutor to the database";
	}

	public String getName() {
		return "Database Protocol Module";
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
		if(!protocol)
			return;
		
		// start if not running
		if(!isAlive())
			start();
		
		synchronized (messageQueue) {
            messageQueue.addLast(msg);
            messageQueue.notify();
        }
	}

	public void reset() {
		flush();

	}

	public void resolveAction(Action action) {
		//NOOP
	}

	public boolean isAlive(){
		return !stop;
	}

	// keep sending messages in the background
	public void run() {
		while (!stop) {
			Message message = null;
			synchronized (messageQueue) {
				if (!messageQueue.isEmpty()) {
					message = messageQueue.removeFirst();
				} else {
					try {
						messageQueue.wait();
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
			
			// I decided to take this out of synchronized code to increase
			// performance
			if (message != null) {
				// I am having mystery NPE which I can't seem to track down
				// might as well make a system recoverable when something like that happens
				try{
					processMessage(message);
				}catch(Exception ex){
					Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				}
			}
		}
	}

	// stop this thread
	public void stop() {
		synchronized (messageQueue) {
			messageQueue.notifyAll();
		}
		stop = true;
	}
	
	/**
	 * start message sender
	 */
	public void start(){
		stop();
		stop = false;
		(new Thread(this)).start();
	}
	
	/**
     * end current session
     */
    public void flush(){
    	attempt = 0;
    	// flush message pipe
        try{
            while(true){
                synchronized(messageQueue){
                    if(messageQueue.isEmpty()){
                        Thread.sleep(500); // wait for server to catchup
                        break;
                    }
                }
                Thread.sleep(50);
            }
        }catch(Exception ex){
        	Config.getLogger().severe(TextHelper.getErrorMessage(ex));
        	//ex.printStackTrace();   
        }
    }
	
    
    
    /**
     * get current connection
     * @return
     */
    public Connection getConnection() throws Exception{
    	if(conn == null || conn.isClosed()){
    		String driver = Config.getProperty(this,"protocol.driver");
    		String url = Config.getProperty(this,"protocol.url");
    		String user = Config.getProperty(this,"protocol.username");
    		String pass = Config.getProperty(this,"protocol.password");
    		specifyColumnsForReturn = null;
    		
    		try{
	    		Class.forName(driver).newInstance();
			    conn = DriverManager.getConnection(url,user,pass);
			  
			    // create tables if not exists
			    if(!checkTables(conn)){
			    	try{
			    		createTables(conn);
			    	}catch(SQLException ex){
			    		// if we have a mysql driver and can't create tables
			    		// this maybe due to foreign key business, so lets try again
			    		// without them
			    		if(driver.contains("mysql")){
			    			Config.getLogger().severe("Can't create database schema at "+url+" (retrying). Reason: "+TextHelper.getErrorMessage(ex));
			    			Config.setProperty(getClass().getSimpleName()+".protocol.database.foreign.key", "false");
			    			createTables(conn);
			    		}else{
			    			throw ex;
			    		}
			    	}
			    }
			    
			    //reset attempt
			    //attempt = 0;
			    
			    // set flag that this is an old representation
			    oldRepresentation = checkOldRepresentation(conn);
			    if(oldRepresentation)
			    	realOldRepresetation = checkRealOldRepresentation(conn);
			    
			}catch(Exception ex){
    			Config.getLogger().severe("Can't connect to database at "+url+". Reason: "+TextHelper.getErrorMessage(ex));
    			//setEnabled(false);
    			//ex.printStackTrace();
    			throw ex;
    		}
    	}
    	return conn;
    }
    
   
    /**
	 * makes sure that protocol is connected to whatever
	 * the storage mechanism is, and returns true if it is and
	 * false if it could not establesh connection
	 * @return
	 */
	public boolean isConnected(){
		// if we have live connection, great
		if(conn != null)
			return true;
		
		try{
			getConnection();
		}catch(Exception ex){
			return false;
		}
		return true;
	}
    
    

	public List<String> getConditions(String experiment) {
		List<String> list = new ArrayList<String>();
		try{
			Connection conn = getConnection();
			PreparedStatement st = null;
			if(experiment != null){
				st = conn.prepareStatement("SELECT experiment_condition_name FROM experiment_condition WHERE " +
						"experiment_id = (SELECT experiment_id FROM experiment WHERE experiment_name = ? )");
				st.setString(1,experiment);
			}else{
				st = conn.prepareStatement("SELECT experiment_condition_name FROM experiment_condition");
			}
			ResultSet result = st.executeQuery();
			while(result.next()){
				list.add(result.getString(1));
			}
			result.close();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getConditions(experiment);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
			
		}
		return list;
	}


	public List<String> getExperiments() {
		List<String> list = new ArrayList<String>();
		try{
			Connection conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet result = st.executeQuery("SELECT experiment_name FROM experiment");
			while(result.next()){
				list.add(result.getString(1));
			}
			result.close();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getExperiments();
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		return list;
	}



	public List<String> getUsers(String experiment) {
		List<String> list = new ArrayList<String>();
		try{
			// if experiment is specified
			String suffix = "";
			if(experiment != null){
				suffix = " WHERE  experiment_id = (SELECT experiment_id FROM experiment " +
						 " WHERE experiment_name = '"+experiment+"')" ;
			}
			// pull all usernames
			Connection conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet result = st.executeQuery("SELECT username FROM student "+suffix);
			while(result.next()){
				list.add(result.getString(1));
			}
			result.close();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getUsers(experiment);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
			
		}
		return list;
	}

    
    /**
     * check if tables exists, this assumes that
     * if one talble exists, so will the others
     * @param conn
     * @return
     */
    private boolean checkTables(Connection conn){
    	Statement st = null;
    	boolean result = false;
    	try{
    		st = conn.createStatement();
			st.execute("SELECT * from protocol_session WHERE student_id=-1");
			result = true;
    	}catch(Exception ex){
    		result = false;
    	}finally{
    		if(st != null){
    			try {
					st.close();
				} catch (SQLException e) {}
    		}
    	}
    	return result;
    }
    
    /**
     * check if tables exists, this assumes that
     * if one talble exists, so will the others
     * @param conn
     * @return
     */
    private boolean checkOldRepresentation(Connection conn){
    	Statement st = null;
    	boolean result = false;
    	try{
    		// if problem events are there
    		st = conn.createStatement();
			st.execute("SELECT * from problem_event WHERE session_id = -1");
			result = true;
			if(result){
				st.close();
				st = conn.createStatement();
				st.execute("SELECT * from protocol_session WHERE time_stamp is null");
				result = true;
			}
		}catch(Exception ex){
    		result = false;
    	}finally{
    		if(st != null){
    			try {
					st.close();
				} catch (SQLException e) {}
    		}
    	}
    	return result;
    }
    
    /**
     * check if tables exists, this assumes that
     * if one talble exists, so will the others
     * @param conn
     * @return
     */
    private boolean checkRealOldRepresentation(Connection conn){
    	Statement st = null;
    	boolean result = false;
    	try{
    		st = conn.createStatement();
			ResultSet  rs = st.executeQuery("SELECT * from protocol_session WHERE session_id = -1");
			result = !getColumnNames(rs).contains("case_url");
    	}catch(Exception ex){
    		result = true;
    	}finally{
    		if(st != null){
    			try {
					st.close();
				} catch (SQLException e) {}
    		}
    	}
    	return result;
    }

    /**
     * get foreign key string
     * @param driver
     * @param table
     * @return
     */
    private String createForeignKey(boolean use, String table){
    	return createForeignKey(use,table+"_id",table);
    }
    
    /**
     * get foreign key string
     * @param driver
     * @param table
     * @return
     */
    private String createForeignKey(boolean use, String id, String table){
    	if(!use)
    		return "";
    	return ", FOREIGN KEY ("+id+") REFERENCES "+table;
    }
    
    
    /**
     * create protocol SQL tables if they don't exist
     * @param conn
     * @throws Exception
     */
    private void createTables(Connection conn) throws Exception{
    	// initialize implementation specific details
    	// database specific constants
    	String CHAR_TYPE     =  "VARCHAR";
    	String INT_TYPE      =  "INTEGER";
    	String TIMESTAMP_TYPE = "TIMESTAMP";
    	String CURRENT_TIME = "";
    	String IF_NOT_EXISTS = "";
    	String PRIMARY_KEY = INT_TYPE+" PRIMARY KEY NOT NULL";
    	boolean FOREIGN_KEY = false; 
    	
    	
    	// check if this is MySQL where auto increment is available
    	String driver = Config.getProperty(this,"protocol.driver");
    	if(driver.toLowerCase().contains("mysql")){
    		CURRENT_TIME = " DEFAULT CURRENT_TIMESTAMP";
    		IF_NOT_EXISTS = "IF NOT EXISTS";
    		CHAR_TYPE = "VARCHAR";
    		INT_TYPE = "INTEGER";
        	TIMESTAMP_TYPE = "TIMESTAMP";
    		PRIMARY_KEY = INT_TYPE+" PRIMARY KEY NOT NULL AUTO_INCREMENT";
    		if(Config.hasProperty(this,"protocol.database.foreign.key")){
    			FOREIGN_KEY = Config.getBooleanProperty(this,"protocol.database.foreign.key");
    		}else{
    			FOREIGN_KEY = true;
    		}
    	}else if(driver.toLowerCase().contains("postgresql")){
    		PRIMARY_KEY = "SERIAL PRIMARY KEY NOT NULL";
    		CHAR_TYPE = "VARCHAR";
    		INT_TYPE = "INTEGER";
        	TIMESTAMP_TYPE = "TIMESTAMP";
        	CURRENT_TIME = " DEFAULT now()";
    	}else if(driver.toLowerCase().contains("oracle")){
    		CHAR_TYPE = "VARCHAR2";
    		INT_TYPE = "INT";
        	TIMESTAMP_TYPE = "TIMESTAMP";
        	PRIMARY_KEY = INT_TYPE+" PRIMARY KEY NOT NULL";
    	}else{
    		FOREIGN_KEY = false;
    		PRIMARY_KEY = INT_TYPE+" PRIMARY KEY NOT NULL";
    	}
    	
    	
    	
    	
    	// table definitions
		List<String> STATEMENTS = new ArrayList<String>();
    	
		// experiment table
		STATEMENTS.add( 
    		   "CREATE TABLE "+IF_NOT_EXISTS+" experiment (" +
				"experiment_id    "+PRIMARY_KEY+", "+
				"experiment_name  "+CHAR_TYPE+"(256) UNIQUE NOT NULL )");
		
		// experiment condition table
		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" experiment_condition (" +
				"experiment_condition_id    "+PRIMARY_KEY+", "+
				"experiment_condition_name  "+CHAR_TYPE+"(512) NOT NULL," +
				"experiment_id              "+INT_TYPE+" REFERENCES experiment "+
				createForeignKey(FOREIGN_KEY,"experiment")+")");
		
		// student table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" student (" +
				"student_id    "+PRIMARY_KEY+", "+
				"username      "+CHAR_TYPE+"(64) UNIQUE NOT NULL, " +
				"password      "+CHAR_TYPE+"(64) , "+
				"experiment_id "+INT_TYPE+" REFERENCES experiment "+
				createForeignKey(FOREIGN_KEY,"experiment")+")");
		
		// student info table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" student_info (" +
				"student_info_id    "+PRIMARY_KEY+", "+
				"name       "+CHAR_TYPE+"(256), " +
				"value      "+CHAR_TYPE+"(4000), "+
				"student_id    "+INT_TYPE+" REFERENCES student "+
				createForeignKey(FOREIGN_KEY,"student")+")");
		
		// protocol session table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" protocol_session (" +
				"session_id    "+PRIMARY_KEY+", "+
				"student_id    "+INT_TYPE+" REFERENCES student, "+
				"experiment_condition_id "+INT_TYPE+" REFERENCES experiment_condition, " +
				"start_time   "+TIMESTAMP_TYPE+CURRENT_TIME+", "+
				"finish_time  "+TIMESTAMP_TYPE+", "+
				"outcome      "+CHAR_TYPE+"(512), "+ 
				"case_url     "+CHAR_TYPE+"(512), "+
				"domain_url   "+CHAR_TYPE+"(512), "+
				"config_url   "+CHAR_TYPE+"(512) "+
				createForeignKey(FOREIGN_KEY,"student")+
				createForeignKey(FOREIGN_KEY,"experiment_condition")+") ");
		
		// client event table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" client_event (" +
				"client_event_id    "+PRIMARY_KEY+", "+
				"type      "+CHAR_TYPE+"(256), "+ 
				"label     "+CHAR_TYPE+"(256), "+ 
				"action    "+CHAR_TYPE+"(256), "+ 
				"parent    "+CHAR_TYPE+"(256), "+ 
				"id        "+CHAR_TYPE+"(256), "+ 
				"input     "+CHAR_TYPE+"(1024), "+ 
				"source    "+CHAR_TYPE+"(256), "+ 
				"object_description   "+CHAR_TYPE+"(512), "+ 
				"time_stamp  "+TIMESTAMP_TYPE+CURRENT_TIME+", "+
				"session_id "+INT_TYPE+" REFERENCES protocol_session "+
				createForeignKey(FOREIGN_KEY,"session_id","protocol_session")+")");
		
		// client event input table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" client_event_input (" +
				"client_event_input_id  "+PRIMARY_KEY+", "+
				"name       "+CHAR_TYPE+"(256), " +
				"value      "+CHAR_TYPE+"(4000), "+
				"client_event_id  "+INT_TYPE+" REFERENCES client_event "+
				createForeignKey(FOREIGN_KEY,"client_event")+" )");
		
		
		// interface event table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" interface_event (" +
				"interface_event_id    "+PRIMARY_KEY+", "+
				"type      "+CHAR_TYPE+"(256), "+ 
				"label     "+CHAR_TYPE+"(256), "+ 
				"action    "+CHAR_TYPE+"(256), "+ 
				"parent    "+CHAR_TYPE+"(256), "+ 
				"id        "+CHAR_TYPE+"(256), "+ 
				"input     "+CHAR_TYPE+"(1024), "+ 
				"source    "+CHAR_TYPE+"(256), "+ 
				"object_description   "+CHAR_TYPE+"(512), "+ 
				"time_stamp  "+TIMESTAMP_TYPE+CURRENT_TIME+", "+
				"session_id "+INT_TYPE+" REFERENCES protocol_session, "+
				"client_event_id    "+INT_TYPE+" REFERENCES client_event "+
				createForeignKey(FOREIGN_KEY,"session_id","protocol_session")+
				createForeignKey(FOREIGN_KEY,"client_event")+")");
				
		// interface event input table
 		STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" interface_event_input (" +
				"interface_event_input_id  "+PRIMARY_KEY+", "+
				"name       "+CHAR_TYPE+"(256), " +
				"value      "+CHAR_TYPE+"(4000), "+
				"interface_event_id  "+INT_TYPE+" REFERENCES interface_event " +
				createForeignKey(FOREIGN_KEY,"interface_event")+" )");		
		
		// tutor response table
 		STATEMENTS.add( 
	 		   "CREATE TABLE "+IF_NOT_EXISTS+" tutor_response (" +
					"tutor_response_id    "+PRIMARY_KEY+", "+
					"response_type  "+CHAR_TYPE+"(256), "+ 
					"error_state    "+CHAR_TYPE+"(512), "+
					"error_code     "+CHAR_TYPE+"(256), "+ 
					"next_step_type      "+CHAR_TYPE+"(256), "+ 
					"next_step_label     "+CHAR_TYPE+"(256), "+ 
					"next_step_action    "+CHAR_TYPE+"(256), "+ 
					"next_step_parent    "+CHAR_TYPE+"(256), "+ 
					"next_step_id        "+CHAR_TYPE+"(256), "+ 
					"input     "+CHAR_TYPE+"(1024), "+ 
					"source    "+CHAR_TYPE+"(256), "+ 
					"time_stamp  "+TIMESTAMP_TYPE+CURRENT_TIME+", "+
					"session_id "+INT_TYPE+" REFERENCES protocol_session, "+
					"client_event_id    "+INT_TYPE+" REFERENCES client_event " +
					createForeignKey(FOREIGN_KEY,"client_event")+
					createForeignKey(FOREIGN_KEY,"session_id","protocol_session")+")");
		
		// interface event input table
	    STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" tutor_response_input (" +
				"tutor_response_input_id  "+PRIMARY_KEY+", "+
				"name       "+CHAR_TYPE+"(256), " +
				"value      "+CHAR_TYPE+"(4000), "+
				"tutor_response_id  "+INT_TYPE+" REFERENCES tutor_response " +
				createForeignKey(FOREIGN_KEY,"tutor_response")+")");		
		
		
	    
		// tutor response table
 		STATEMENTS.add( 
	 		   "CREATE TABLE "+IF_NOT_EXISTS+" node_event (" +
					"node_event_id    "+PRIMARY_KEY+", "+
					"type      "+CHAR_TYPE+"(256), "+ 
					"label     "+CHAR_TYPE+"(256), "+ 
					"action    "+CHAR_TYPE+"(256), "+ 
					"parent    "+CHAR_TYPE+"(256), "+ 
					"response_type  "+CHAR_TYPE+"(256), "+ 
					"error_state    "+CHAR_TYPE+"(512), "+
					"error_code     "+CHAR_TYPE+"(256), "+ 
					"one_to_many  "+CHAR_TYPE+"(4000), "+ 
					"many_to_many "+CHAR_TYPE+"(4000), "+ 
					"is_absent "+CHAR_TYPE+"(16), "+ 
					"input     "+CHAR_TYPE+"(1024), "+ 
					"source    "+CHAR_TYPE+"(256), "+ 
					"time_stamp  "+TIMESTAMP_TYPE+CURRENT_TIME+", "+
					"session_id "+INT_TYPE+" REFERENCES protocol_session, "+
					"tutor_response_id    "+INT_TYPE+" REFERENCES tutor_response" +
					createForeignKey(FOREIGN_KEY,"tutor_response")+
					createForeignKey(FOREIGN_KEY,"session_id","protocol_session")+" )");
		
		// interface event input table
	    STATEMENTS.add( 
 		   "CREATE TABLE "+IF_NOT_EXISTS+" node_event_input (" +
				"node_event_input_id  "+PRIMARY_KEY+", "+
				"name       "+CHAR_TYPE+"(256), " +
				"value      "+CHAR_TYPE+"(4000), "+
				"node_event_id  "+INT_TYPE+" REFERENCES node_event " +
				createForeignKey(FOREIGN_KEY,"node_event")+")");		
		
	    
	    
		// execute statements
		for(String SQL: STATEMENTS){
			Statement st = conn.createStatement();
			st.execute(SQL);
			st.close();
		}
		
		
		// if oracle create sequence and triggers
		if(driver.toLowerCase().contains("oracle")){
			String [] tables = 
				{"experiment","experiment_condition","student","student_info","protocol_session",
				 "client_event","client_event_input","interface_event",
				 "interface_event_input","tutor_response","tutor_response_input","node_event","node_event_input"};
			
			// now for each table create a sequence and a trigger
			for(String t : tables){
				// create a sequence for each table
				String SQL = "CREATE SEQUENCE "+t+"_seq START WITH 1 INCREMENT BY 1 NOMAXVALUE"; 
				Statement st = conn.createStatement();
				st.execute(SQL);
				st.close();
				
				// get the name for table ids
				String id = t+"_id";
				if("protocol_session".equals(t))
					id = "session_id";
				
				// create a trigger for each table
				SQL = "CREATE TRIGGER "+t+"_trigger BEFORE INSERT ON "+t+" FOR EACH ROW " +
					  "WHEN (new."+id+" IS NULL) "+		
					  "BEGIN SELECT "+t+"_seq.nextval INTO :new."+id+" FROM dual; END;"; 
				st = conn.createStatement();
				st.execute(SQL);
				st.close();
			}
		}
		
		// create special GUEST user 
		addUser(Config.DEFAULT_USER,Config.DEFAULT_USER ,"",null);
		
    }
  

	public void addCondition(String condition, String experiment) {
		try{
			String SQL = "INSERT INTO experiment_condition (experiment_condition_name, experiment_id) " +
					"VALUES (?,(SELECT experiment_id FROM experiment WHERE experiment_name = ?))";
			Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL);
			st.setString(1,filter(condition,512));
			st.setString(2,experiment);
			st.executeUpdate();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				addCondition(condition, experiment);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		
	}


	public void addExperiment(String experiment) {
		try{
			String SQL = "INSERT INTO experiment (experiment_name) VALUES (?) ";
			Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL);
			st.setString(1,filter(experiment,256));
			st.executeUpdate();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				addExperiment(experiment);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
	}


	public void addUser(String username, String password, String experiment, Properties p) {
		try{
			// insert student entry
			String SQL = 
				"INSERT INTO student (username,password,experiment_id) " +
				"VALUES (?,?,(SELECT experiment_id FROM experiment WHERE experiment_name = ?))";
			Connection conn = getConnection();
			PreparedStatement st = getPreparedStatement(conn,SQL);
			st.setString(1,filter(username,64));
			st.setString(2,filter(password,64));
			st.setString(3,experiment);
			st.executeUpdate();
			ResultSet result = st.getGeneratedKeys();
			int id = -1;
			if(result.next()){
				id = result.getInt(1);
			}
			result.close();
			st.close();
			
			// insert student information
			/*
			if(id > -1 && p != null && !p.isEmpty()){
				// build input string
				StringBuffer input = new StringBuffer();
				// iterate over properties
				for(int i=0;i<p.size();i++){
					input.append("(?,?,?), ");
				}
				// remove last comma and space
				input.delete(input.length()-2,input.length());
				
				// prepare statment
				SQL = "INSERT INTO student_info (name,value,student_id) VALUES "+input;
				st = conn.prepareStatement(SQL);
				// set values
				int i = 1;
				for(Object key : p.keySet()){
					String name = key.toString();
					String value = p.getProperty(name);
					// set values
					st.setString(i,filter(name,256));
					st.setString(i+1,filter(value,4000));
					st.setInt(i+2,id);
					// increment
					i+=3;
				}
				st.executeUpdate();
				st.close();
			}
			*/
			
			
			// insert student information
			if(id > -1 && p != null && !p.isEmpty()){
				for(Object key : p.keySet()){
					String name = key.toString();
					String value = p.getProperty(name);
					
					// skip fields that are known
					if(name.equals("experiment") || name.equals("username") || name.equals("password"))
						continue;
					
					// prepare statment
					SQL = "INSERT INTO student_info (name,value,student_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					// execute
					st.executeUpdate();
					st.close();
				}
				
			}
			
			
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				addUser(username, password, experiment, p);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
			
		}
		
		
	}


	public boolean authenticateUser(String username, String password) {
		// take care of a default
		if(Config.DEFAULT_USER.equals(username) && Config.DEFAULT_USER.equals(password))
			return true;
		
		// else do normal lookup
		return authenticateUser(username, password,null);
	}
	
	/**
	 * authenticate a user with administrative privalages
	 * @param name
	 * @param password
	 * @param return true if user is authenticated, false otherwise
	 */
	public boolean authenticateAdministrator(String username, String password){
		return authenticateUser(username, password,Config.getProperty(this,"protocol.admin.group"));
	}

	/**
	 * authenticate a user with administrative privalages
	 * @param name
	 * @param password
	 * @param return true if user is authenticated, false otherwise
	 */
	public boolean authenticateUser(String username, String password, String study){
		boolean r = false;
		
		// get admin group
		String group = Config.getProperty(this,"protocol.admin.group");
		if(!TextHelper.isEmpty(study) && study.equals(group))
			group = null;
		
		try{
			String SQL = "SELECT username FROM student WHERE username = ? AND password = ?";
			if(!TextHelper.isEmpty(study)){
				SQL += " AND experiment_id IN (SELECT experiment_id FROM experiment WHERE experiment_name IN ";
				SQL += (TextHelper.isEmpty(group))?"(?))":" (?,?))";
			}
			// pull all usernames
			Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL);
			st.setString(1,username);
			st.setString(2,password);
			if(!TextHelper.isEmpty(study)){
				st.setString(3,study);
				if(!TextHelper.isEmpty(group))
					st.setString(4,group);
			}
			ResultSet result = st.executeQuery();
			if(result.next()){
				r = true;
			}
			result.close();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return authenticateUser(username, password, study);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}	
		//}
		//return authenticateUser(username, password);
		return r;
	}
	
	

	public Properties getUserInfo(String username) {
		Properties p = new Properties();
		try{
			Connection conn = getConnection();
			String SQL = "SELECT s.username, s.password, e.experiment_name, i.name, i.value " +
			"FROM student s LEFT OUTER JOIN experiment e ON s.experiment_id = e.experiment_id " +
			"LEFT OUTER JOIN student_info i ON s.student_id = i.student_id "+
			"WHERE s.username = ?";
			PreparedStatement st = conn.prepareStatement(SQL);
			st.setString(1,username);
			ResultSet result = st.executeQuery();
			
			// values for default
			String user = null;
			String pass = null;
			String exp  = null;
				
			// iterate over results	
			while(result.next()){
				user = result.getString(1);
				pass = result.getString(2);
				exp  = result.getString(3);
				String name = result.getString(4);
				String val  = result.getString(5);
				if(name != null && val != null){
					p.setProperty(name,val);
				}
			}
			
			// set defaults
			if(user != null)
				p.setProperty("username",user);
			if(pass != null)
				p.setProperty("password",pass);
			if(exp != null)
				p.setProperty("experiment",exp);
			
			// cleanup
			result.close();
			st.close();
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getUserInfo(username);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		return p;
	}
	
	
	/**
	 * get cases based on a query object
	 */
	public List<String> getCases(Query query) {
		List<String> list = new ArrayList<String>();
		
		// first we need to decide whether this is 
		Set<String> columns = null;
		if(oldRepresentation){
			StringBuffer condition = new StringBuffer();
			
			//check username
			if(query.hasValue("username")){
				condition.append("s.student_id IN (SELECT student_id FROM student WHERE username IN "+query.getValue("username")+") AND ");
			}
			
			//check username
			if(query.hasValue("experiment")){
				condition.append("s.student_id IN (SELECT student_id FROM student WHERE experiment_id IN (SELECT experiment_id FROM experiment " +
						"WHERE experiment_name IN "+query.getValue("experiment")+")) AND ");
			}
			
			// add AND
			if(query.hasValue("experiment_condition_name")){
				condition.append("s.experiment_condition_id IN (SELECT experiment_condition_id FROM experiment_condition c, experiment e " +
						"WHERE c.experiment_id = e.experiment_id AND c.experiment_condition_name IN "+query.getValue("experiment_condition_name"));
				// filter by experiment
				if(query.hasValue("username"))
					condition.append(" AND e.experiment_id = (SELECT experiment_id FROM student WHERE username IN "+query.getValue("username")+")");
				
				condition.append(") AND ");		
			}
				
			String SQL = "SELECT DISTINCT t.case_name FROM protocol_session s, " +
							"problem_event e, tutor_case t WHERE "+condition+
							" s.session_id=e.session_id AND e.case_id=t.case_id AND " +
							" e.problem_state='started' ORDER BY t.case_name";
			
			// System.out.println(SQL);
			
			// now finally lets add some cases
			try{
				Connection conn = getConnection();
				Statement st = conn.createStatement();
				ResultSet result = st.executeQuery(SQL);
				columns = getColumnNames(result);
				while(result.next()){
					list.add(result.getString(1));
				}
				result.close();
				st.close();
			}catch(Exception ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				//ex.printStackTrace();
			}		
		}
		
		// now if there is something to be said about new representation
		// lets query again
		if(columns == null || columns.contains("case_url")){
			try{
				StringBuffer condition = new StringBuffer();
				
				//check username
				if(query.hasValue("username")){
					condition.append("AND student_id IN (SELECT student_id FROM student WHERE username IN "+query.getValue("username")+")");
				}
				
				// add condition
				if(query.hasValue("experiment_condition_name")){
					condition.append(" AND experiment_condition_id IN (SELECT experiment_condition_id FROM experiment_condition c, experiment e " +
							"WHERE c.experiment_id = e.experiment_id AND c.experiment_condition_name IN "+query.getValue("experiment_condition_name"));
					// filter by experiment
					if(query.hasValue("username"))
						condition.append(" AND e.experiment_id = (SELECT experiment_id FROM student WHERE username IN "+query.getValue("username")+")");
					
					condition.append(") ");		
				}
				
				String SQL = "SELECT DISTINCT case_url FROM protocol_session WHERE case_url IS NOT NULL "+condition+" ORDER BY case_url";
				//System.out.println(SQL);
				
				Connection conn = getConnection();
				Statement st = conn.createStatement();
				ResultSet result = st.executeQuery(SQL);
				while(result.next()){
					list.add(result.getString(1));
				}
				result.close();
				st.close();
			}catch(Exception ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				//ex.printStackTrace();
			}
		}
		return list;
	}
		
	/**
	 * get a set of column names from result set
	 * @param result
	 * @return
	 */
	private Set<String> getColumnNames(ResultSet result){
		Set<String> list = new LinkedHashSet<String>();
		try { 
			ResultSetMetaData rsmd = result.getMetaData(); 
			for (int i=1; i < rsmd.getColumnCount()+1; i++) {
				String columnName = rsmd.getColumnName(i); 
				//String tableName = rsmd.getTableName(i);
				list.add(columnName.toLowerCase());
			} 
		} catch (SQLException e) { } 
		
		return list;
	}
	
	
	/**
	 * get list of session objects
	 */
	
	public List<Session> getSessions(Query query) {
		//List<Session> list = new ArrayList<Session>();
		OrderedMap<String,Session> sessionMap = new OrderedMap<String, Session>();
		try{
			Connection conn = getConnection();
			
			StringBuffer condition = new StringBuffer();
			//check username
			if(query.hasValue("username")){
				condition.append(" WHERE u.student_id IN (SELECT student_id FROM student WHERE username IN "+query.getValue("username")+")");
			//check username
			}else if(query.hasValue("experiment")){
				condition.append(" WHERE s.student_id IN (SELECT student_id FROM student WHERE experiment_id IN (SELECT experiment_id FROM experiment " +
						"WHERE experiment_name IN "+query.getValue("experiment")+"))");
			}
			
			
			// check condition
			if(query.hasValue("experiment_condition_name")){
				String s = (condition.length() > 0)?" AND ":" WHERE ";
				condition.append(s+ "s.experiment_condition_id IN (SELECT experiment_condition_id FROM experiment_condition c, experiment e " +
						"WHERE c.experiment_id = e.experiment_id AND c.experiment_condition_name IN "+query.getValue("experiment_condition_name"));
				// filter by experiment
				if(query.hasValue("username"))
					condition.append(" AND e.experiment_id IN (SELECT experiment_id FROM student WHERE username IN "+query.getValue("username")+")");
				condition.append(") ");		
			}
			
			// check case
			if(query.hasValue("case_url")){
				String s = (condition.length() > 0)?" AND ":" WHERE ";
				//if(oldRepresentation){
				//	condition.append(s+" t.case_name IN "+query.getValue("case_url")+" ");
				//}else{
				condition.append(s+ "s.case_url IN "+query.getValue("case_url")+" ");
				//}
			}
			//TODO: timestamp filtering, config, domain etc ...
			// build super duper query for new representation
			List<String> SQLS = new ArrayList<String>();
			SQLS.add(   "SELECT s.session_id,u.username,e.experiment_name,c.experiment_condition_name,s.start_time,s.finish_time," +
					     "s.outcome,s.case_url,s.domain_url,s.config_url FROM protocol_session s LEFT OUTER JOIN student u ON s.student_id = u.student_id " +
						 "LEFT OUTER JOIN experiment_condition c ON s.experiment_condition_id = c.experiment_condition_id LEFT OUTER JOIN " +
						 "experiment e ON e.experiment_id = c.experiment_id "+condition+"ORDER BY s.session_id");
			// redo query for the old representation
			if(oldRepresentation){
				if(condition.length() == 0)
					condition.append(" WHERE ");
				else
					condition.append(" AND ");
				
				// fix condition to fit old ways
				if(query.hasValue("case_url")){
					condition = new StringBuffer(condition.toString().replaceAll("s.case_url","t.case_name"));
				}
				
				String tm = "time_stamp";
				if(realOldRepresetation){
					tm = "timestamp";
					SQLS.clear();
				}
				SQLS.add(0,"SELECT s.session_id,u.username,e.experiment_name,c.experiment_condition_name,s."+tm+" as start_time, " +
					  "p.timestamp as finish_time, p.problem_state as outcome,t.case_name as case_url, s.tutor_version as domain_url," +
					  "s.client_version as config_url FROM problem_event p, student u ,tutor_case t, protocol_session s LEFT OUTER JOIN " +
					  "experiment_condition c ON c.experiment_condition_id = s.experiment_condition_id  LEFT OUTER JOIN experiment e ON " +
					  "c.experiment_id = e.experiment_id "+condition+" t.case_id = p.case_id AND s.session_id = p.session_id AND " +
					  "s.student_id = u.student_id AND s.experiment_condition_id = c.experiment_condition_id ORDER BY s.session_id, problem_event_id");
			}
			
			for(String SQL : SQLS){
				Statement st = conn.createStatement();
				ResultSet result = st.executeQuery(SQL);
				//Set<String> columns = getColumnNames(result);
				DatabaseSession lastSession = null;
				while(result.next()){
					// set session object
					int id = result.getInt("session_id");
					DatabaseSession session = null;
					if(lastSession != null && lastSession.getSessionID().equals(""+id)){
						session = lastSession;
					}else{
						session = new DatabaseSession(this);
						//list.add(session);
						lastSession = session;
					}
					
					session.setSessionID(id);
					session.setUsername(result.getString("username"));
					session.setExperiment(result.getString("experiment_name"));
					session.setCondition(result.getString("experiment_condition_name"));
					session.setOutcome(result.getString("outcome"));
					session.setCase(result.getString("case_url"));
					session.setDomain(result.getString("domain_url"));
					session.setConfiguration(result.getString("config_url"));
					Timestamp t1 = result.getTimestamp("start_time");
					if(t1 != null)
						session.setStartTime(new Date(t1.getTime()));
					
					try{
						Timestamp t2 = result.getTimestamp("finish_time");
						if(t2 != null)
							session.setFinishTime(new Date(t2.getTime()));
					}catch(SQLException ex){
						//if invalid timestamp, then don't bother setting it
					}
					
					// now add session to map
					if(!sessionMap.containsKey(session.getSessionID())){
						sessionMap.put(session.getSessionID(),session);
					}else if(!session.equals(sessionMap.get(session.getSessionID()))){
						// if new session with same ID has case info, then we should use it instead
						if(!TextHelper.isEmpty(session.getCase())){
							sessionMap.put(session.getSessionID(),session);
						}
					}
					
				}
				result.close();
				st.close();
			}
			
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getSessions(query);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		return sessionMap.getValues();
	}
	

	/**
	 * open case session for this case
	 */
	public void openCaseSession(ProblemEvent pe) {
		if(!protocol)
			return;
		
		ie_table = new HashMap<Integer, List<Integer>>();
		ce_table = new HashMap<Integer, Integer>();
		tr_table = new HashMap<Integer,Integer>();
		session_id = insertProtocolSession(pe);
	}
    
	/**
	 * close this case session for this case
	 */
    public void closeCaseSession(ProblemEvent evt) {
    	if(!protocol)
			return;
    	
    	// flash out protocol
    	flush();
    	
    	updateProtocolSession(evt, session_id);
    	ie_table = null;
    	ce_table = null;
    	tr_table = null;
    	session_id = -1;
    }

    
    /**
     * this is where main work is going on
     * @param msg
     */
    public void processMessage(Message msg){
    	processProtocolMessage(msg);
    }
    
    /**
     * this is where main work is going on
     * @param msg
     */
    public int processProtocolMessage(Message msg){
    	if(!protocol)
			return -1;
    
    	// what to do in case there is no session id
    	// this is in case of emergency to prevent a crash or in case
    	// we are capturing events that have no cases
    	if(session_id == -1 && !(msg instanceof ProblemEvent)){
    		openCaseSession(ProblemEvent.createStartProblemEvent(" "));
    	}
    	
    	int id = -1;
    	
    	// process message
    	if(msg instanceof InterfaceEvent){
    		InterfaceEvent ie = (InterfaceEvent) msg;
    		
    		// if the ce is available (this will almost never happen)
    		int cid = -1;
    		if(ce_table != null && ce_table.containsKey(ie.getClientEventId()))
    			cid = ce_table.get(ie.getClientEventId());
    		
    		// interface events precede client events, hence there is no CE id 
    		// available, so we will need to do an update after words
    		id = insertInterfaceEvent(ie,session_id,cid);
    		
    		/// add this interface event to list of pending IEs to update
    		//  if ce was not available right away
    		if(ie.getClientEventId() > -1){
	    		List<Integer> ids = ie_table.get(ie.getClientEventId());
	    		if(ids == null){
	    			ids = new ArrayList<Integer>();
	    			ie_table.put(ie.getClientEventId(),ids);
	    		}
	    		ids.add(id);
    		}
    	}else if(msg instanceof ClientEvent){
    		// insert client event, and add the mapping to the table
    		id = insertClientEvent((ClientEvent) msg,session_id);
    		if(ce_table != null)
    			ce_table.put(msg.getMessageId(),id);
    		
    		// now update all of Interface events
    		if(ie_table != null && ie_table.containsKey(msg.getMessageId())){
    			updateInterfaceEvents(ie_table.get(msg.getMessageId()),id);
    		}
    	}else if(msg instanceof NodeEvent){
    		// at this point client event should be available
    		NodeEvent ne = (NodeEvent) msg;
    		id = (tr_table != null && tr_table.containsKey(ne.getTutorResponseId()))?tr_table.get(ne.getTutorResponseId()):-1;
    		insertNodeEvent(ne,session_id,id);
    	}else if(msg instanceof TutorResponse){
    		// at this point client event should be available
    		TutorResponse tr = (TutorResponse) msg;
    		int cid = (ce_table != null && ce_table.containsKey(tr.getClientEventId()))?ce_table.get(tr.getClientEventId()):-1;
    		id = insertTutorResponse(tr,session_id,cid);
    		if(tr_table != null)
    			tr_table.put(msg.getMessageId(),id);
    	}else if(msg instanceof ProblemEvent){
    		ProblemEvent pe = (ProblemEvent) msg;
    		if(Constants.TYPE_START.equalsIgnoreCase(pe.getType()))
    			openCaseSession(pe);
    		else if(Constants.TYPE_END.equalsIgnoreCase(pe.getType()))
    			closeCaseSession(pe);
    		id = session_id;
    	}
    	
    	return id;
    }
   
    /**
     * filter input string
     * @param field
     * @param limit
     * @return
     */
    private String filter(String field, int limit){
    	if(field == null || limit < 0)
    		return field;
    	int l = field.length();
    	return field.substring(0,(l > limit)?limit:l);
    }
    
    /**
     * insert protocol session 
     * @return
     */
    private int insertProtocolSession(ProblemEvent pe){
    	int id = -1;
    	try{
    		String SQL = 
    			"INSERT INTO protocol_session (student_id,experiment_condition_id,start_time,case_url,domain_url,config_url) " +
    			"VALUES ((SELECT student_id FROM student WHERE username = ?)," +
    			"(SELECT c.experiment_condition_id FROM experiment_condition c, experiment e, student s WHERE " +
    			"c.experiment_condition_name = ? AND c.experiment_id = e.experiment_id AND e.experiment_id = s.experiment_id AND " +
    			"s.username = ?),?,?,?,?)";
    		
    		String username  =  pe.getUsername();
    		String condition =  pe.getCondition();
    		
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = getPreparedStatement(conn,SQL);
			st.setString(1,username);
			st.setString(2,condition);
			st.setString(3,username);
			st.setTimestamp(4,new Timestamp(pe.getTimestamp()));
			st.setString(5,filter(pe.getCaseURL(),512));
			st.setString(6,filter(pe.getDomainURL(),512));
			st.setString(7,filter(pe.getConfigURL(),512));
			
			// execute
			st.executeUpdate();
			
			// get id back
			id = getGeneratedKey(st);
			st.close();
			
			
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				insertProtocolSession(pe);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
    	}
		return id;
    }
    
    
    /**
     * insert protocol session 
     * @return
     */
    private void updateProtocolSession(ProblemEvent pe, int session_id){
    	try{
    		if(session_id < 0)
    			return;
    		String SQL = "UPDATE protocol_session SET finish_time = ? , outcome = ? WHERE session_id = ?";
    		
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = conn.prepareStatement(SQL);
			st.setTimestamp(1,new Timestamp(pe.getTimestamp()));
			st.setString(2,filter(pe.getOutcome(),512));
			st.setInt(3,session_id);
		
			// execute
			st.executeUpdate();
			st.close();
			
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
    		if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				updateProtocolSession(pe, session_id);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
    }
    
    
    
    /**
     * insert interface event
     * @param evt
     */
    private int insertClientEvent(ClientEvent msg, int session_id){
    	int id = -1;
    	try{
    		String SQL = 
    			"INSERT INTO client_event (type,label,action,parent,id,input,time_stamp,session_id,source,object_description) " +
    			"VALUES (?,?,?,?,?,?,?,?,?,?)";
    		
    		
    		// take care of parent entry
    		msg.addInput("entire_concept",msg.getEntireConcept());
    		
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = getPreparedStatement(conn,SQL);
			st.setString(1,filter(msg.getType(),256));
			st.setString(2,filter(msg.getLabel(),256));
			st.setString(3,filter(msg.getAction(),256));
			st.setString(4,filter(msg.getParent(),256));
			st.setString(5,filter(msg.getId(),256));
			st.setString(6,filter(msg.getInputString(),1024));
			//st.setTimestamp(7,new Timestamp(System.currentTimeMillis()));
			st.setTimestamp(7,new Timestamp(msg.getTimestamp()));
			st.setInt(8,session_id);
			st.setString(9,msg.getSource());
			st.setString(10,filter(msg.getObjectDescription(),512));
			
			// execute
			st.executeUpdate();
			
			// get id back
			id = getGeneratedKey(st);
			st.close();
			
			
			// create property map
			Map input = msg.getInputMap();
						
			// insert client event input
			if(!input.isEmpty() && id > -1){
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO client_event_input (name,value,client_event_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					// execute
					st.executeUpdate();
					st.close();
				}
				
			}
			
    	
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return insertClientEvent(msg, session_id);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		return id;
    }  
    
    /**
     * update interface events
     * @param evt
     */
    private void updateInterfaceEvents(List<Integer> ieIDlist, int client_event_id){
    	try{
    		String s = ""+ieIDlist;
    		String SQL = "UPDATE interface_event SET client_event_id = "+client_event_id+
    					" WHERE interface_event_id IN "+s.replace('[','(').replace(']',')');
    		Connection conn = getConnection();
    		Statement st = conn.createStatement();
    		st.executeUpdate(SQL);
    		st.close();
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				updateInterfaceEvents(ieIDlist, client_event_id);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}

    	}
    }
    
    /**
     * insert interface event
     * @param evt
     */
    private int insertInterfaceEvent(InterfaceEvent msg, int session_id, int client_event_id){
    	int id = -1;
    	try{
    		String sn = (client_event_id > -1)?",client_event_id":"";
    		String sq = (client_event_id > -1)?",?":"";
    		String SQL = 
    			"INSERT INTO interface_event (type,label,action,parent,id,input,time_stamp,session_id,source,object_description"+sn+") "+
    			"VALUES (?,?,?,?,?,?,?,?,?,?"+sq+")";
    		
    		// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = getPreparedStatement(conn,SQL);
			st.setString(1,filter(msg.getType(),256));
			st.setString(2,filter(msg.getLabel(),256));
			st.setString(3,filter(msg.getAction(),256));
			st.setString(4,filter(msg.getParent(),256));
			st.setString(5,filter(msg.getId(),256));
			st.setString(6,filter(msg.getInputString(),1024));
			//st.setTimestamp(7,new Timestamp(System.currentTimeMillis()));
			st.setTimestamp(7,new Timestamp(msg.getTimestamp()));
			st.setInt(8,session_id);
			st.setString(9,msg.getSource());
			st.setString(10,filter(msg.getObjectDescription(),512));
			if(client_event_id > -1)
				st.setInt(11,client_event_id);
			// execute
			st.executeUpdate();
			
			// get id back
			id = getGeneratedKey(st);
			st.close();
			
			// create property map
			Map input = msg.getInputMap();
			
			// insert client event input
			// I had a verly clever system in place to insert multiple rows at the same time, but
			// since Oracle doesn't support that syntax, I have to do one insert at a time
			if(!input.isEmpty() && id > -1){
				// set values
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO interface_event_input (name,value,interface_event_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					// execute
					st.executeUpdate();
					st.close();
				}
			}
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
    		if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return insertInterfaceEvent(msg, session_id, client_event_id);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
			
		}
		return id;
    }
    
    /**
     * insert interface event
     * @param evt
     */
    private int insertTutorResponse(TutorResponse msg, int session_id, int client_event_id){
    	int id = -1;
    	try{
    		String sn = (client_event_id > -1)?",client_event_id":"";
    		String sq = (client_event_id > -1)?",?":"";
    		
    		String SQL = 
    			"INSERT INTO tutor_response (response_type,error_code,next_step_type,next_step_label," +
    			"next_step_action, next_step_parent,next_step_id,input,time_stamp,session_id,source,error_state"+sn+") " +
    			"VALUES (?,?,?,?,?,?,?,?,?,?,?,?"+sq+")";
    	
    		// add response concept to input
			msg.addInput("response_concept",msg.getResponseConcept());
    			
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = getPreparedStatement(conn,SQL);
			st.setString(1,filter(msg.getResponse(),256));
			st.setString(2,filter(msg.getCode(),256));
			st.setString(3,filter(msg.getType(),256));
			st.setString(4,filter(msg.getLabel(),256));
			st.setString(5,filter(msg.getAction(),256));
			st.setString(6,filter(msg.getParent(),256));
			st.setString(7,filter(msg.getId(),256));
			st.setString(8,filter(msg.getInputString(),1024));
			//st.setTimestamp(9,new Timestamp(System.currentTimeMillis()));
			st.setTimestamp(9,new Timestamp(msg.getTimestamp()));
			st.setInt(10,session_id);
			st.setString(11,msg.getSource());
			st.setString(12,filter(msg.getError(),512));
			if(client_event_id > -1)
				st.setInt(13,client_event_id);
			
			// execute
			st.executeUpdate();
			
			// get id back
			id = getGeneratedKey(st);
			st.close();
			
		
			// create property map
			Map input = msg.getInputMap();
				
			// insert client event input
			if(!input.isEmpty() && id > -1){
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO tutor_response_input (name,value,tutor_response_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					//execute
					st.executeUpdate();
					st.close();
				}
				
			}
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
    		if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return insertTutorResponse(msg, session_id, client_event_id);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		return id;
    }


    /**
     * insert interface event
     * @param evt
     */
    private int insertNodeEvent(NodeEvent msg, int session_id, int tutor_response_id){
    	int id = -1;
    	try{
    		String sn = (tutor_response_id > -1)?",tutor_response_id":"";
    		String sq = (tutor_response_id > -1)?",?":"";

    		// check for old representation
    		String p = "";
    		if(oldRepresentation)
    			p="node_event_";
    		
    		String SQL = 
    			"INSERT INTO node_event ("+p+"type,"+p+"label,action,"+p+"parent,response_type,error_state," +
    					"error_code,one_to_many,many_to_many,is_absent,input,source,time_stamp,session_id"+sn+") " +
    			"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?"+sq+")";
    		
    		// add entire concept to input
			msg.addInput("entire_concept",msg.getEntireConcept());
			msg.addInput("object_description",msg.getObjectDescription());
    		
	    	// insert into client event
    		Connection conn = getConnection();
			PreparedStatement st = getPreparedStatement(conn,SQL);
			st.setString(1,filter(msg.getType(),256));
			st.setString(2,filter(msg.getLabel(),256));
			st.setString(3,filter(msg.getAction(),256));
			st.setString(4,filter(msg.getParent(),256));
			
			st.setString(5,filter(msg.getResponse(),256));
			st.setString(6,filter(msg.getError(),512));
			st.setString(7,filter(msg.getCode(),256));
					
			st.setString(8,filter(msg.getOneToMany(),4000));
			st.setString(9,filter(msg.getManyToMany(),4000));
			st.setString(10,filter(""+msg.isAbsent(),16));
			
			st.setString(11,filter(msg.getInputString(),1024));
			st.setString(12,msg.getSource());
			
			st.setTimestamp(13,new Timestamp(msg.getTimestamp()));
			st.setInt(14,session_id);
			if(tutor_response_id > -1)
				st.setInt(15,tutor_response_id);
			
			// execute
			st.executeUpdate();
			
			// get id back
			id = getGeneratedKey(st);
			st.close();
			
			// create property map
			Map input = msg.getInputMap();
			
			// insert client event input
			if(!input.isEmpty() && id > -1){
				for(Object key : input.keySet()){
					// prepare statment
					SQL = "INSERT INTO node_event_input (name,value,node_event_id) VALUES (?,?,?)";
					st = conn.prepareStatement(SQL);
					
					String name = key.toString();
					String value = ""+input.get(key);
					// set values
					st.setString(1,filter(name,256));
					st.setString(2,filter(value,4000));
					st.setInt(3,id);
					
					//execute
					st.executeUpdate();
					st.close();
				}
				
			}
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return insertNodeEvent(msg, session_id, tutor_response_id);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
		return id;
    }

    
    /**
     * get list of client events for a given session id
     * @param session_id
     * @return
     */
    public List<ClientEvent> getClientEvents(List<String> sessions){
    	if(sessions == null || sessions.isEmpty())
    		return Collections.EMPTY_LIST;
    	List<ClientEvent> list = new ArrayList<ClientEvent>();
    	try{
    		String s = ""+sessions;
    		String SQL = "SELECT * FROM client_event WHERE session_id IN ("+s.substring(1,s.length()-1)+") ORDER BY client_event_id";
    		boolean noInputs = true;
    		
	    	// insert into client event
    		Connection conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet result = st.executeQuery(SQL);
			Set<String> columns = getColumnNames(result);
			while(result.next()){
				ClientEvent ce = new ClientEvent();
				ce.setMessageId(result.getInt("client_event_id"));
				ce.setType(result.getString("type"));
				ce.setLabel(result.getString("label"));
				ce.setAction(result.getString("action"));
				ce.setParent(result.getString("parent"));
				ce.setId(result.getString("id"));
				ce.setObjectDescription(result.getString("object_description"));
				
				if(columns.contains("time_stamp"))
					ce.setTimestamp(result.getTimestamp("time_stamp").getTime());
				else if(columns.contains("timestamp"))
					ce.setTimestamp(result.getTimestamp("timestamp").getTime());
				
				if(columns.contains("source"))
					ce.setSource(result.getString("source"));
				if(columns.contains("input")){
					String input = result.getString("input");
					if(input != null){
						noInputs = false;
						ce.setInput(TextHelper.parseMessageInput(input));
					}
				}
				
				// set parent concept
				if(ce.getInput() instanceof Map){
					Map<String,String> map = (Map<String,String>)ce.getInput();
					if(map.containsKey("entire_concept"))
						ce.setEntireConcept(map.get("entire_concept"));
				}
				list.add(ce);
			}
			result.close();
			st.close();
			
			
			// if there were no inputs, then fetch inputs from inputs :)
			if(noInputs){
				processInputValues(list,"client_event");
			}
			
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getClientEvents(sessions);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
    	return list;
    }
    
    /**
     * get input values into a list of messages
     * @param list
     * @param table
     */
    private void processInputValues(List list, String table) throws Exception {
    	if(list.isEmpty() || table == null)
    		return;
    	
    	// check settings
    	if(!Config.getBooleanProperty(this,"protocol.query.input.tables"))
    		return;
    	
    	// create a list of id's
		List<List<Integer>> id_list = new ArrayList<List<Integer>>();
		List<Integer> ids = new ArrayList<Integer>();
		id_list.add(ids);
		int x = 0;
		for(Object ce: list){
			if(ce instanceof Message){
				if(x > 999){
					ids = new ArrayList<Integer>();
					id_list.add(ids);
					x = 0;
				}
				ids.add(((Message)ce).getMessageId());
				x++;
			}
		}
		
		// go over messages in the list
		int i = 0;
		Message ce = (Message)list.get(i);
		
		
		// now query inputs by themselves
		for(List<Integer> idl : id_list){
			String s = ""+idl;
			String SQL = "SELECT * FROM "+table+"_input WHERE "+table+"_id IN ("+s.substring(1,s.length()-1)+") ORDER BY "+table+"_id";
			Statement st = conn.createStatement();
			ResultSet result = st.executeQuery(SQL);
		
			// go over result
			while(result.next()){
				int id = result.getInt(table+"_id");
				String name  = result.getString("name");
				String value = result.getString("value");
				
				// get to the right message
				while(ce != null && ce.getMessageId() != id){
					i++;
					ce = (Message) ((i<list.size())?list.get(i):null);
				}
				
				// add values to map
				if(ce != null){
					Map<String,String> map = null;
					if(ce.getInput() instanceof Map){
						map = (Map<String,String>)ce.getInput();
					}else{
						map = new HashMap<String, String>();
						ce.setInput(map);
					}
					
					map.put(name,value);
				}
			}
			result.close();
			st.close();
		}
    	
    }
    
    
    /**
     * get list of client events for a given session id
     * @param session_id
     * @return
     */
    public List<InterfaceEvent> getInterfaceEvents(List<String> sessions){
    	if(sessions == null || sessions.isEmpty())
    		return Collections.EMPTY_LIST;
    	List<InterfaceEvent> list = new ArrayList<InterfaceEvent>();
    	try{
    		String s = ""+sessions;
    		String SQL = "SELECT * FROM interface_event WHERE session_id IN ("+s.substring(1,s.length()-1)+") ORDER BY interface_event_id";
    		
	    	// insert into client event
    		boolean noInputs = true;
    		Connection conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet result = st.executeQuery(SQL);
			Set<String> columns = getColumnNames(result);
			while(result.next()){
				InterfaceEvent ce = new InterfaceEvent();
				ce.setMessageId(result.getInt("interface_event_id"));
				ce.setType(result.getString("type"));
				ce.setLabel(result.getString("label"));
				ce.setAction(result.getString("action"));
				ce.setParent(result.getString("parent"));
				ce.setId(result.getString("id"));
				ce.setObjectDescription(result.getString("object_description"));
				//ce.setTimestamp(result.getTimestamp("time_stamp").getTime());
				//ce.setSource(result.getString("source"));
				//ce.setInput(TextHelper.parseMessageInput(result.getString("input")));
				

				if(columns.contains("time_stamp"))
					ce.setTimestamp(result.getTimestamp("time_stamp").getTime());
				else if(columns.contains("timestamp"))
					ce.setTimestamp(result.getTimestamp("timestamp").getTime());
				
				if(columns.contains("source"))
					ce.setSource(result.getString("source"));
				if(columns.contains("input")){
					String input = result.getString("input");
					if(input != null){
						noInputs = false;
						ce.setInput(TextHelper.parseMessageInput(input));
					}
				}
				
				int id = result.getInt("client_event_id");
				if(id > -1)
					ce.setClientEventId(id);
				
				list.add(ce);
			}
			result.close();
			st.close();
			
			
			// if there were no inputs, then fetch inputs from inputs :)
			if(noInputs){
				processInputValues(list,"interface_event");
			}
			
			
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getInterfaceEvents(sessions);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
    	return list;
    }
    
    
    /**
     * get list of client events for a given session id
     * @param session_id
     * @return
     */
    public List<TutorResponse> getTutorResponses(List<String> sessions){
    	if(sessions == null || sessions.isEmpty())
    		return Collections.EMPTY_LIST;
    	List<TutorResponse> list = new ArrayList<TutorResponse>();
    	try{
    		String s = ""+sessions;
    		String SQL = "SELECT * FROM tutor_response WHERE session_id IN ("+s.substring(1,s.length()-1)+") ORDER BY tutor_response_id";
    		
    		// try it twice in case there is not there anything there
    		for(int t=0;t<2;t++){
    			// insert into client event
	    		boolean noInputs = true;
	    		Connection conn = getConnection();
				Statement st = conn.createStatement();
				ResultSet result = st.executeQuery(SQL);
				Set<String> columns = getColumnNames(result);
				while(result.next()){
					TutorResponse ce = new TutorResponse();
					ce.setMessageId(result.getInt("tutor_response_id"));
					ce.setResponse(result.getString("response_type"));
					if(columns.contains("error_state"))
						ce.setError(result.getString("error_state"));
					ce.setCode(result.getString("error_code"));
					ce.setType(result.getString("next_step_type"));
					ce.setLabel(result.getString("next_step_label"));
					if(columns.contains("next_step_action"))
						ce.setAction(result.getString("next_step_action"));
					ce.setParent(result.getString("next_step_parent"));
					ce.setId(result.getString("next_step_id"));
					//ce.setObjectDescription(result.getString("next_step_object_description"));
					
					//ce.setTimestamp(result.getTimestamp("time_stamp").getTime());
					//ce.setSource(result.getString("source"));
					//ce.setInput(TextHelper.parseMessageInput(result.getString("input")));
					
	
					if(columns.contains("time_stamp"))
						ce.setTimestamp(result.getTimestamp("time_stamp").getTime());
					else if(columns.contains("timestamp"))
						ce.setTimestamp(result.getTimestamp("timestamp").getTime());
					
					if(columns.contains("source"))
						ce.setSource(result.getString("source"));
					if(columns.contains("input")){
						String input = result.getString("input");
						if(input != null){
							noInputs = false;
							ce.setInput(TextHelper.parseMessageInput(input));
						}
					}
					
					int id = result.getInt("client_event_id");
					if(id > -1)
						ce.setClientEventId(id);
					
					// set parent concept
					if(ce.getInput() instanceof Map){
						Map<String,String> map = (Map<String,String>)ce.getInput();
						if(map.containsKey("response_concept"))
							ce.setResponseConcept(map.get("response_concept"));
					}	
					
					list.add(ce);
				}
				result.close();
				st.close();
				
				// if there were no inputs, then fetch inputs from inputs :)
				if(noInputs){
					processInputValues(list,"tutor_response");
				}
				
				// if no responses, then perhaps it is the earlier study 
				if(list.isEmpty()){
					SQL = "SELECT * FROM tutor_response WHERE client_event_id IN (SELECT client_event_id FROM client_event WHERE session_id IN ("+s.substring(1,s.length()-1)+")) ORDER BY tutor_response_id";
				}else{
					break;
				}
    		}
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
			if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getTutorResponses(sessions);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
    	return list;
    }

    
    
    /**
     * get list of client events for a given session id
     * @param session_id
     * @return
     */
    public List<NodeEvent> getNodeEvents(List<String> sessions){
    	if(sessions == null || sessions.isEmpty())
    		return Collections.EMPTY_LIST;
    	List<NodeEvent> list = new ArrayList<NodeEvent>();
    	try{
    		String s = ""+sessions;
    		String SQL = "SELECT * FROM node_event WHERE session_id IN ("+s.substring(1,s.length()-1)+") ORDER BY node_event_id";
    		
	    	// insert into client event
    		boolean noInputs = true;
    		String p = "";
    		if(oldRepresentation)
    			p = "node_event_";
    		
    		Connection conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet result = st.executeQuery(SQL);
			Set<String> columns = getColumnNames(result);
			
			// handle column names
			if(columns.contains("node_event_type"))
				p = "node_event_";
			else
				p = "";
			while(result.next()){
				NodeEvent ne = new NodeEvent();
				ne.setMessageId(result.getInt("node_event_id"));
				ne.setType(result.getString(p+"type"));
				ne.setLabel(result.getString(p+"label"));
				ne.setAction(result.getString("action"));
				ne.setParent(result.getString(p+"parent"));
			
				ne.setResponse(result.getString("response_type"));
				if(columns.contains("error_state"))
					ne.setError(result.getString("error_state"));
				ne.setCode(result.getString("error_code"));
								
				if(columns.contains("time_stamp"))
					ne.setTimestamp(result.getTimestamp("time_stamp").getTime());
				else if(columns.contains("timestamp"))
					ne.setTimestamp(result.getTimestamp("timestamp").getTime());
				
				if(columns.contains("source"))
					ne.setSource(result.getString("source"));
				
				if(columns.contains("input")){
					String input = result.getString("input");
					if(input != null){
						noInputs = false;
						ne.setInput(TextHelper.parseMessageInput(input));
					}
				}
				
				ne.setOneToMany(result.getString("one_to_many"));
				ne.setManyToMany(result.getString("many_to_many"));
				ne.setAbsent(Boolean.parseBoolean(result.getString("is_absent")));
				ne.setTutorResponseId(result.getInt("tutor_response_id"));
				
				// set parent concept
				if(ne.getInput() instanceof Map){
					Map<String,String> map = (Map<String,String>)ne.getInput();
					if(map.containsKey("entire_concept"))
						ne.setEntireConcept(map.get("entire_concept"));
					if(map.containsKey("object_description"))
						ne.setObjectDescription(map.get("object_description"));
				}
				
				list.add(ne);
			}
			result.close();
			st.close();
			
			// if there were no inputs, then fetch inputs from inputs :)
			if(noInputs && !oldRepresentation){
				processInputValues(list,"node_event");
			}
			
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		//ex.printStackTrace();
    		if(attempt++ < ATTEMPT_LIMIT || Config.raiseRedAlert(RED_ALERT_MESSAGE)){
				conn = null;
				return getNodeEvents(sessions);
			}else{
				Config.getLogger().severe("Protocol error encountered and ignored. Disabling protocol.");
				setEnabled(false);
			}
		}
    	return list;
    }
    
    
    
	public boolean removeCondition(String condition, String experiment) {
		if(TextHelper.isEmpty(experiment) || TextHelper.isEmpty(condition))
    		return false;
		int num = 0;
		try{
    		String SQL = "DELETE FROM experiment_condition WHERE experiment_condition_name = '"+condition+"' AND experiment_id IN "+
    					 "(SELECT experiment_id FROM experiment WHERE experiment_name = '"+experiment+"')";
    		
	    	// insert into client event
    		Connection conn = getConnection();
			Statement st = conn.createStatement();
			num = st.executeUpdate(SQL);
			st.close();
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		return false;
		}
    	return num > 0;
	}

	public boolean removeExperiment(String experiment) {
		if(TextHelper.isEmpty(experiment))
    		return false;
		int num = 0;
		try{
    		String SQL = "DELETE FROM experiment WHERE experiment_name = '"+experiment+"'";
    		
	    	// insert into client event
    		Connection conn = getConnection();
			Statement st = conn.createStatement();
			num = st.executeUpdate(SQL);
			st.close();
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		return false;
		}
    	return num > 0;
	}

	public boolean removeUser(String username) {
		if(TextHelper.isEmpty(username))
    		return false;
		int num = 0;
		try{
    		String SQL = "DELETE FROM student WHERE username = '"+username+"'";
    		
	    	// insert into client event
    		Connection conn = getConnection();
			Statement st = conn.createStatement();
			num = st.executeUpdate(SQL);
			st.close();
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		return false;
		}
    	return num > 0;
	}
	
	
	/**
	 * remove session
	 * @param session
	 * @return
	 */
	public boolean removeSessions(List<Session> sessions){
		if(sessions == null || sessions.isEmpty())
			return false;
		
		// create a list of sessions
		List<String> ss = new ArrayList<String>();
		for(Session s: sessions)
			ss.add(s.getSessionID());
		String session = "("+TextHelper.toString(ss)+")";
		
		// setup SQL expressions
		List<String> SQL_LIST = new ArrayList<String>();
		// fist remove all inputs
		SQL_LIST.add("DELETE FROM node_event_input WHERE node_event_id IN (SELECT node_event_id FROM node_event WHERE session_id IN "+session+" )");
		SQL_LIST.add("DELETE FROM client_event_input WHERE client_event_id IN (SELECT client_event_id FROM client_event WHERE session_id IN "+session+" )");
		SQL_LIST.add("DELETE FROM interface_event_input WHERE interface_event_id IN (SELECT interface_event_id FROM interface_event WHERE session_id IN "+session+" )");
		SQL_LIST.add("DELETE FROM tutor_response_input WHERE tutor_response_id IN (SELECT tutor_response_id FROM tutor_response WHERE session_id IN "+session+" )");
		// remove all events
		SQL_LIST.add("DELETE FROM node_event WHERE session_id IN "+session);
		SQL_LIST.add("DELETE FROM interface_event WHERE session_id IN "+session);
		SQL_LIST.add("DELETE FROM tutor_response WHERE session_id IN "+session);
		SQL_LIST.add("DELETE FROM client_event WHERE session_id IN "+session);
		// remove session 
		SQL_LIST.add("DELETE FROM protocol_session WHERE session_id IN "+session);
		
		// remove all inputs
		int num = 0;
		try{
    		for(String SQL : SQL_LIST){
    			Connection conn = getConnection();
				Statement st = conn.createStatement();
				num += st.executeUpdate(SQL);
				st.close();
    		}
    	}catch(Exception ex){
    		Config.getLogger().severe(TextHelper.getErrorMessage(ex));
    		return false;
		}
    	return num > 0;
	}
}
