package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.PrintStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.TextHelper;

public class CopyUsersAndConditions implements ProtocolScript {
	private ProtocolModule protocol,target;
	private PrintStream output;
	private Writer writer;
	
	public String getName() {
		return "Copy Users and Conditions";
	}

	public String getDescription() {
		return "Copy Users and Conditions";
	}

	public boolean process(Session session) {
		getProtocolModule();
		//copy Test conditions from target
		/*Set<String> conds = new TreeSet<String>();
		for(String exp: target.getExperiments()){
			conds.addAll(target.getConditions(exp));
		}
		// add conditions to Testing
		for(String c: conds){
			System.out.println("Testing -> "+c);
			target.addCondition(c,"Testing");
		}*/
		// add users to Deployment
		List<String> tusers = target.getUsers("Deployment");
		for(String user: protocol.getUsers("Deployment")){
			// skip existing users
			if(tusers.contains(user)){
				addUserInfo(user,protocol.getUserInfo(user));
			}else{
				// add remaining users along with their info
				Properties p = protocol.getUserInfo(user);
				System.out.println("Adding User: "+user+" "+p);
				if(p.containsKey("password") && p.containsKey("experiment"))
					target.addUser(user,p.getProperty("password"),p.getProperty("experiment"),p);
			}
		}
		return true;
	}


	private void addUserInfo(String user, Properties p) {
		try{
			Connection conn = ((DatabaseProtocolModule)target).getConnection();
			int id = -1;
			String SQL = "SELECT student_id FROM student WHERE username = ?";
			
			// pull a username id
			PreparedStatement st = conn.prepareStatement(SQL);
			ResultSet result = st.executeQuery();
			if(result.next()){
				id = result.getInt("student_id");
			}
			result.close();
			st.close();
			
			
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
					st.setString(1,name);
					st.setString(2,value);
					st.setInt(3,id);
					
					// execute
					st.executeUpdate();
					st.close();
				}
				
			}
		}catch(Exception ex){
			
		}
		
	}

	public void setOutput(PrintStream out) {
		output = out;

	}

	public void setProtocolModule(ProtocolModule m) {
		protocol = m;

	}

	public String toString(){
		return getName();
	}
	
	public void dispose() {
		getProtocolModule().dispose();
		
	}

	private ProtocolModule getProtocolModule() {
		if(target == null){
			target = new DatabaseProtocolModule();
			target.getDefaultConfiguration().setProperty("protocol.driver","org.postgresql.Driver");
			target.getDefaultConfiguration().setProperty("protocol.url","jdbc:postgresql://cds-its01/protocol");
			target.getDefaultConfiguration().setProperty("protocol.username","slidetutor");
			target.getDefaultConfiguration().setProperty("protocol.password","rotutedils");
			
			/*
			target.getDefaultConfiguration().setProperty("protocol.driver","com.mysql.jdbc.Driver");
			target.getDefaultConfiguration().setProperty("protocol.url","jdbc:mysql://localhost/protocol-test");
			target.getDefaultConfiguration().setProperty("protocol.username","user");
			target.getDefaultConfiguration().setProperty("protocol.password","resu");
			*/
			
		}
		return target;
	}

	public void initialize() {
	}

}
