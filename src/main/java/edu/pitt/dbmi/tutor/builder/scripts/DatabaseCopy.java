package edu.pitt.dbmi.tutor.builder.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.*;
import java.util.*;



public class DatabaseCopy {
	private static Map<String,Map<Integer,Integer>> keyMap;
	private static Boolean specifyColumnsForReturn;
	private static Writer writer;
	
	/**
	 * copy some data from production Oracle database to Postgres
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {
		// connection parameters
		String relationFile = System.getProperty("user.home")+File.separator+"DatabaseCopyIDTable.csv";
		
		String driver1 = "oracle.jdbc.driver.OracleDriver";
		String url1    = "jdbc:oracle:thin:@o1prd02.isdip.upmc.edu:1521:pwdb";
		String user1   = "slidetutor";
		String pass1   = "upci03st";
		
		String driver2 = "org.postgresql.Driver";
		String url2    = "jdbc:postgresql://cds-its01/protocol";
		String user2   = "slidetutor";
		String pass2   = "rotutedils";
		
		
		
		/*String driver2 = "com.mysql.jdbc.Driver";
		String url2    = "jdbc:mysql://localhost/protocol-test";
		String user2   = "user";
		String pass2   = "resu";*/
		
		keyMap = readForeginKeyMap(relationFile);
		
		Class.forName(driver1).newInstance();
		Class.forName(driver2).newInstance();
		
		Connection c1 = DriverManager.getConnection(url1,user1,pass1);
		Connection c2 = DriverManager.getConnection(url2,user2,pass2);
		//c2.setAutoCommit(false);
		
		//create tables
		//createTables(c2);
		
		
		// add tutor case ids
		addTutorCaseIds(c1, c2);
		getWriter().flush();
		// copy tables
/*		copyTable(c1,c2,"tutor_case");
		copyTable(c1,c2,"case_report");
		copyTable(c1,c2,"experiment_domain");
		copyTable(c1,c2,"tutor_case_test_list"); //student
		copyTable(c1,c2,"save_query");
		
		copyTable(c1,c2,"case_sequence");
		copyTable(c1,c2,"case_sequence_item");
		copyTable(c1,c2,"case_sequence_input");
		copyTable(c1,c2,"case_list"); // experiment_condition
		
		
		copyTable(c1,c2,"student_model");
		copyTable(c1,c2,"student_model_params");
		
		copyTable(c1,c2,"mc_error_summary"); // protocol_session
		copyTable(c1,c2,"mc_error"); // protocol_session
		copyTable(c1,c2,"hint_model"); // protocol_session
*/		
		copyTable(c1,c2,"problem_event"); // protocol_session
	
		//copyTable(c1,c2,"student_model_change"); // node_event
		//copyTable(c1,c2,"student_model_input");
		
		//updateNodeEvent(c1,c2);
		
		
		c1.close();
		c2.close();
	}
	
	private static void addTutorCaseIds(Connection c1, Connection c2) throws Exception {
		System.out.println("Adding Case IDs...");
		Statement s1 = c1.createStatement();
		ResultSet r1 = s1.executeQuery("SELECT case_id, case_name FROM  tutor_case");
		String names = null, values = null;
		int updatedRows = 0;
		while(r1.next()){
			// create new statement
			int old_id = r1.getInt("case_id");
			int new_id = -1;
			String name = r1.getString("case_name");
			
			PreparedStatement s2 = c2.prepareStatement("SELECT case_id FROM  tutor_case WHERE case_name = ?");
			s2.setString(1,name);
			ResultSet r2 = s2.executeQuery();
			if(r2.next()){
				new_id = r2.getInt("case_id");
			}
			r2.close();
			s2.close();
			
			if(new_id > -1){
				getWriter().write("tutor_case,"+old_id+","+new_id+"\n");
				saveKey(keyMap, "tutor_case", old_id, new_id);
			}
		}
		r1.close();
		s1.close();
	}
	
	/**
	 * get writer 
	 * @return
	 */
	private static Writer getWriter() throws Exception{
		if(writer == null){
			String f = System.getProperty("user.home")+File.separator+"DatabaseCopyIDTable.csv";
			writer = new BufferedWriter(new FileWriter(f,true));
		}
		return writer;
	}
	
	/**
	 * create tables
	 * @param c
	 */
	private static void createTables(Connection c) throws Exception {
		String in = "INTEGER";
		String vr = "VARCHAR";
		String fl = "FLOAT";
		String cb = "TEXT";
		String primeKey =  " serial primary key not null ";
		
		String [] SQL = new String [] {
				"create table tutor_case (case_id "+primeKey+", " +
				"case_name "+vr+"(256), " +
				"study_id "+vr+"(10), "+
				"original_diagnosis "+vr+"(255), "+
				"pattern_number "+in+", "+
				"original_accession "+vr+"(100), "+
				"case_source "+vr+"(255), "+
				"notes "+vr+"(500), "+
				"interscope_file_location "+vr+"(100), "+
				"aperio_file_location "+vr+"(100), "+
				"isscanned "+vr+"(1), "+
				"iscasequalitychecked "+vr+"(1), "+
				"isauthored "+vr+"(1), "+
				"isauthoringchecked "+vr+"(1), "+
				"ismoved "+vr+"(1), "+
				"istested "+vr+"(1), "+
				"authoringtime "+vr+"(8), "+
				"interscope_file_name "+vr+"(100), "+
				"aperio_file_name "+vr+"(100), "+
				"authored_diagnoses "+vr+"(2056), "+
				"authored_findings "+vr+"(2056) )",
				
				"create table case_report ( report_id "+primeKey+", case_id "+in+" "+
						"references tutor_case, ischecked "+vr+"(1), reporttext "+cb+" )",
				
				"create table experiment_domain (experiment_domain_id	"+primeKey+" , domain_name	"+vr+"(256))",
				
				"create table tutor_case_test_list ( "+
				   "tutor_case_test_list_id "+primeKey+", "+
				   "case_id "+in+" references tutor_case, "+
				   "student_id "+in+" references student, "+
				   "tutor_domain "+vr+"(256), "+
				   "component "+vr+"(256), "+
				   "status "+vr+"(24), "+
				   "time_stamp timestamp )", 
				   
				 "create table save_query (query_id "+primeKey+", query_value "+vr+"(4000), description "+vr+"(512))", 
				
				 "create table case_sequence(case_sequence_id "+primeKey+", case_sequence_name "+vr+"(256), required_cases "+in+")",
				 
				 "create table case_sequence_input (case_sequence_input_id "+primeKey+",time_interval "+fl+" default -1, case_number "+in+
				 	" default -1, name "+vr+"(128), value "+vr+"(4000), case_sequence_id "+in+" references case_sequence)",
				 
				 "create table case_sequence_item(case_sequence_item_id "+primeKey+", case_id "+in+" references tutor_case, " +
				 		"case_name "+vr+"(128), order_number "+in+",case_sequence_id "+in+" references case_sequence)",
				 "create table case_list (case_list_id "+primeKey+",case_id "+in+" references tutor_case ," +
						"experiment_condition_id "+in+" references experiment_condition,order_number "+in+")",
						
				"create table student_model (student_model_id "+primeKey+",model_name "+vr+"(256),subgoal_type "+vr+"(64),subgoal_name "+vr+"(64))",		
				"create table student_model_params (student_model_params_id "+primeKey+",param_name "+vr+"(256),param_value "+vr+"(4000), student_model_id "+in+" references student_model )",
		
				"create table student_model_change (student_model_change_id "+primeKey+", state "+vr+"(256), node_event_id "+in+" references node_event)",
				"create table student_model_input(student_model_input_id "+primeKey+",name "+vr+"(256), value "+vr+"(4000), student_model_change_id "+in+" references student_model_change)",
				
				"create table mc_error_summary (mc_error_summary_id "+primeKey+",session_id "+in+" references protocol_session, representativeness_counter "+vr+"(64))",
				"create table mc_error (mc_error_id "+primeKey+", mc_error_summary_id "+in+" references mc_error_summary, error_name "+vr+"(256), error_code "+vr+"(256), "+
						"error_message "+vr+"(4000), features "+vr+"(1024), diagnoses "+vr+"(1024), sequence "+vr+"(4000), counter "+fl+", session_id "+in+" references protocol_session)",
				"create table hint_model (hint_model_id "+primeKey+", session_id "+in+" references protocol_session,  num_hints "+vr+"(64), num_failures "+vr+"(64), num_confirms "+vr+"(64))",
		
				"create table problem_event ( problem_event_id "+primeKey+",  session_id "+in+" references protocol_session, " +
						"case_id "+in+" references tutor_case, problem_state "+vr+"(64), time_stamp timestamp)"
		};
		// catate SQL
		System.out.println("Creating tables ...");
		for(String s: SQL){
			Statement st = c.createStatement();
			st.executeUpdate(s);
			st.close();
		}
		
	}
	
	
	
	
	/**
	 * copy
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private static  Map<String,Map<Integer,Integer>> readForeginKeyMap(String file) throws Exception {
		 Map<String,Map<Integer,Integer>> map = new HashMap<String, Map<Integer,Integer>>();
		 BufferedReader reader = new BufferedReader(new FileReader(file));
		 for(String line = reader.readLine();line != null; line = reader.readLine()){
			 String [] l = line.split(",");
			 if(l.length == 3 && isInterested(line)){
				 String table = l[0].trim();
				 int oldid = Integer.parseInt(l[1].trim());
				 int newid = Integer.parseInt(l[2].trim());
				 
				 saveKey(map,table,oldid,newid);
			 }
		 }
		 reader.close();
		 
		 return map;
	}
	
	private static boolean isInterested(String l){
		return l.startsWith("protocol") || l.startsWith("tutor_case");
	}
	
	
	private static void saveKey( Map<String,Map<Integer,Integer>> map, String table, int oldid, int newid){
		 Map<Integer,Integer> m = map.get(table);
		 if(m == null){
			 m = new HashMap<Integer, Integer>();
			 map.put(table,m);
		 }
		 
		 m.put(oldid,newid);
	}
	
	
	/**
	 * copy table content 
	 * @param c1
	 * @param c2
	 * @param table
	 */
	private static void copyTable(Connection c1, Connection c2, String table) throws Exception{
		System.out.print("Copying table: "+table+" ..  ");
		Statement s1 = c1.createStatement();
		ResultSet r1 = s1.executeQuery("SELECT * FROM "+table);
		String names = null, values = null;
		int updatedRows = 0;
		while(r1.next()){
			// generate prepared statement
			if(names == null && values == null){
				StringBuffer nm  = new StringBuffer();
				StringBuffer val  = new StringBuffer();
				/// start from one to skip primary key
				for(int i=2;i<=r1.getMetaData().getColumnCount();i++){
					String name = r1.getMetaData().getColumnName(i);
					if(name.equalsIgnoreCase("timestamp"))
						name = "TIME_STAMP";
					nm.append(name+",");
					val.append("?,");
				}
				names = nm.substring(0,nm.length()-1);
				values = val.substring(0,val.length()-1);
			}
			// create new statement
			int fk = 0;
			PreparedStatement s2 = getPreparedStatement(c2,"INSERT INTO "+table+" ( "+names+"  ) VALUES ("+values+")");
			for(int i=2;i<=r1.getMetaData().getColumnCount();i++){
				int t = r1.getMetaData().getColumnType(i);
				switch(t){
				case Types.NUMERIC:
				case Types.INTEGER:fk = getForeignKey(r1.getMetaData().getColumnName(i),r1.getInt(i));
									s2.setInt(i-1,fk); break;
				case Types.FLOAT: 	s2.setFloat(i-1,r1.getFloat(i)); break;
				case Types.DOUBLE:	s2.setDouble(i-1,r1.getDouble(i)); break;
				case Types.VARCHAR:
				case Types.CHAR: 	s2.setString(i-1,r1.getString(i)); break;
				case Types.TIMESTAMP: s2.setTimestamp(i-1,r1.getTimestamp(i)); break;
				case Types.CLOB: s2.setClob(i-1,r1.getClob(i)); break;
				default: s2.setString(i-1,r1.getString(i));
				}
				if(r1.wasNull()){
					s2.setNull(i-1,t);
				}
				// if one foreign key is fucked, don't bother
				if(fk  == -666)
					break;
			}
			// foregin key to an entry that doesn't exist
			// don't bother with inserting this row
			if(fk  == -666){
				continue;
			}
			// execute statememt
			try{
				updatedRows += s2.executeUpdate();
			}catch(SQLException ex){
				//System.err.println(ex.getMessage());
			}
			// get new keys to the tables
			int newID = getGeneratedKey(s2);
			int oldID = r1.getInt(1);
			saveKey(keyMap, table, oldID, newID);
						
			s2.close();
		}
		s1.close();
		r1.close();
		//c2.commit();
		System.out.println(updatedRows+" were copied");
	}
	
	private static void updateNodeEvent(Connection c1,Connection c2) throws Exception{
		System.out.print("Updating node_event table:  ..  ");
		String SQL = "select * from node_event where jess_command is not null or slide_name is not null or " +
					"feedback_status is not null or probability_observed is not null or " +
					"implicitly_absent is not null or problem_feedback_mode is not null";
		Statement s1 = c1.createStatement();
		ResultSet r1 = s1.executeQuery(SQL);
		int updatedRows = 0;
		List<String> fields = Arrays.asList("jess_command","slide_name","feedback_status","probability_observed","implicitly_absent","problem_feedback_mode");
		while(r1.next()){
			int oldID = r1.getInt("node_event_id");
			for(String key: fields){
				String value = r1.getString(key);
				if(value != null){
					int newID = getForeignKey("node_event_id", oldID);
					if(newID != -666){
						PreparedStatement s2 = c2.prepareStatement("insert into node_event_input (name, value,node_event_id) values (?,?,?)");
						s2.setString(1,key);
						s2.setString(2,value);
						s2.setInt(3,newID);
						try{
						updatedRows += s2.executeUpdate();
						}catch(SQLException ex){
							
						}
						s2.close();
					}
				}
			}
		}
		s1.close();
		r1.close();
		//c2.commit();
		System.out.println(updatedRows+" were updated");
	}
	
	
	/**
	 * get generated key
	 * @param st
	 * @return
	 * @throws SQLException
	 */
	private static int getGeneratedKey(Statement st) throws SQLException{
		int id = -1;
		ResultSet result = st.getGeneratedKeys();
		if(result.next())
			id = result.getInt(1);
		result.close();
		return id;
	}
	
	/**
	 * get prepared statement
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	private static PreparedStatement getPreparedStatement(Connection conn, String sql) throws SQLException{
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
	 * potentially process foreign key
	 * @param name
	 * @param value
	 * @return
	 */
	private static int getForeignKey(String name, int value){
		name = name.toLowerCase();
		if(name.endsWith("_id")){
			// get name of table
			name = name.substring(0,name.length()-3);
			// get notable exceptions
			if(name.equals("case"))
				name = "tutor_case";
			else if(name.equals("session"))
				name = "protocol_session";
			// now lookup in the table
			Map<Integer,Integer> map = keyMap.get(name);
			if(map != null && map.containsKey(value)){
				int n = map.get(value);
				// fix for a goof
				if(name.equals("experiment_condition") && value > 12)
					n = n + 2;
				return n;
			}
			// default, no entry found in table
			return -666;
		}
		return value;
	}
	
	
}
