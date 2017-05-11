package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.WriteAbortedException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.OntologyHelper;

public class TransferProduction implements ProtocolScript {
	private ProtocolModule protocol,target;
	private PrintStream output;
	private Writer writer;
	
	public String getName() {
		return "Transfer Production Data";
	}

	public String getDescription() {
		return "Transfer Data from Production Database to new format as well as taking care of legacy data" ;
	}

	public boolean process(Session session) {
		output.println(session.getUsername()+"; "+OntologyHelper.getCaseName(session.getCase())+"; "+session.getSessionID()+" ..");
		try {
			copy(session,(DatabaseProtocolModule)getProtocolModule());
			getWriter().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public String toString(){
		return getName();
	}
	
	public void initialize() {
	}


	public void setOutput(PrintStream out) {
		output = out;

	}
	public void setProtocolModule(ProtocolModule m) {
		protocol = m;
	}


	public void dispose() {
		getProtocolModule().dispose();
		try {
			getWriter().close();
			writer = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * copy a single session to a protocol module
	 * @param s
	 * @param target
	 */
	public void copy(Session s, DatabaseProtocolModule target) throws Exception {
		// make sure that we have experiment, condition and user
		ProtocolModule source = protocol;
		
		String experiment = s.getExperiment();
		String condition  = s.getCondition();
		String user       = s.getUsername();
		
		target.getConnection().setAutoCommit(false);
		
		// check experiment
		if(!target.getExperiments().contains(experiment) && experiment != null){
			target.addExperiment(experiment);
		}
		
		// check username (for any experiment, that is why parameter is null)
		if(!target.getUsers(null).contains(user)){
			Properties p = (source != null)?source.getUserInfo(user):null;
			String password = (source != null)?p.getProperty("password"):s.getUsername();
			target.addUser(s.getUsername(),password,experiment,p);
			getWriter().write("student,"+getUserId(((DatabaseProtocolModule)source).getConnection(),user)+","+target.getUsers(null).size()+"\n");
		}else if(experiment == null){
			// if experiment is not provided, but user exists
			experiment = target.getUserInfo(user).getProperty("experiment");
		}
		
		// check condition
		if(!target.getConditions(experiment).contains(condition) && experiment != null){
			target.addCondition(condition,experiment);
			getWriter().write("experiment_condition,"+getExperimentId(((DatabaseProtocolModule)source).getConnection(),experiment,condition)+","+target.getConditions(null).size()+"\n");
		}
		
		// get problem events that represent start & end of the problem
		List<ProblemEvent> pes = s.getProblemEvents();
		if(pes.size() != 2)
			return;
		
		// now start session
		int id = target.processProtocolMessage(pes.get(0));
		getWriter().write(getTable(pes.get(0))+","+s.getSessionID()+","+id+"\n");
		
		// process all messages
		for(Message msg: getMessages(s)){
			id = target.processProtocolMessage(msg);
			getWriter().write(getTable(msg)+","+msg.getMessageId()+","+id+"\n");
		}
		
		// close session
		target.closeCaseSession(pes.get(1));
		
		// commit stuff
		target.getConnection().commit();
		target.getConnection().setAutoCommit(true);
	}
	
	private int getExperimentId(Connection c, String experiment, String condition) throws Exception {
		Statement s = c.createStatement();
		ResultSet r = s.executeQuery("select experiment_condition_id from experiment_condition where experiment_condition_name = '"+condition+"' AND " +
				" experiment_id = (select experiment_id from experiment where experiment_name = '"+experiment+"')");
		int id = -1;
		if(r.next()){
			id = r.getInt(1);
		}
		r.close();
		s.close();
		return id;
	}
	
	private int getUserId(Connection c, String user) throws Exception {
		Statement s = c.createStatement();
		ResultSet r = s.executeQuery("select student_id from student where username = '"+user+"'");
		int id = -1;
		if(r.next()){
			id = r.getInt(1);
		}
		r.close();
		s.close();
		return id;
	}
	
	
	private String getTable(Message msg) {
		if(msg instanceof InterfaceEvent){
    		return "interface_event";
    	}else if(msg instanceof ClientEvent){
    		return "client_event";
    	}else if(msg instanceof NodeEvent){
    		return "node_event";
    	}else if(msg instanceof TutorResponse){
    		return "tutor_response";
    	}else if(msg instanceof ProblemEvent){
    		return "protocol_session";
    	}
		return null;
	}

	/**
	 * get client events and interface events sorted by timestamp
	 * @param s
	 * @return
	 */
	private List<Message> getMessages(Session s){
		List<Message> messages = new ArrayList<Message>();
		messages.addAll(s.getInterfaceEvents());
		messages.addAll(s.getClientEvents());
		messages.addAll(s.getTutorResponses());
		messages.addAll(s.getNodeEvents());
		
		// sort by date
		Collections.sort(messages,new Message.TimeComparator());
		
		return messages;
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

	
	/**
	 * get writer 
	 * @return
	 */
	private Writer getWriter() throws Exception{
		if(writer == null){
			String f = System.getProperty("user.home")+File.separator+"DatabaseCopyIDTable.csv";
			writer = new BufferedWriter(new FileWriter(f,true));
		}
		return writer;
	}
	
	
}
