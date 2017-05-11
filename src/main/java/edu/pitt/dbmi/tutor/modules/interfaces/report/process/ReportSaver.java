/**
 * This process saves everything that is typed in the report.
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report.process;

import java.util.*;
import java.io.*;
import java.text.*;

import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportDocument;

/**
 * This processor simply saves everything that is typed or deleted into a log file
 * When newline is typed the string is saved in log file along with TimeStamp and Section Heading
 * When string is removed it is also saved in the log file
 */

public class ReportSaver implements ReportProcessor {
	private ReportDocument doc;   			// back reference to report document
	//private ReportProtocol protocol;
	
	private BufferedWriter writer;			// file writer
	private String documentFile,logFile;
	private StringBuffer insertBuffer;		// buffer for more efficient inserts
	//private StringBuffer removeBuffer;		// buffer for more efficient removes
	private String currentInsertSection;
	//private String currentRemoveSection;
	private long startTime, insertTime;
    private int  insertOffset;
	private boolean typing;
	//private int removeOffset = Integer.MAX_VALUE;
	//private boolean removeBack;  // direction in which characters are deleted
	private boolean initialized;
	
	public static final int FILE     = 1;  // save log & report to local files
	public static final int SERVER   = 2;  // save log & report to files on server
	public static final int PROTOCOL = 4;  // save typed text and report in protocol db
	public static final int REPORT   = 8;  // only save report on the server/file
	private int mode; //mode in which this system works
	
	
	/**
	 * Save report on the server
	 */	
	public ReportSaver(ReportDocument doc){
		this(doc,null,null);
		mode = PROTOCOL;
	}
		
	/**
	 * save report under with local logfile and textfile
	 */	
	public ReportSaver(ReportDocument doc, String logFile,String txtFile){
		this.doc   = doc;	
		this.documentFile = txtFile;
		this.logFile = logFile;
		
		// init buffers
		insertBuffer = new StringBuffer("");
		//removeBuffer = new StringBuffer("");
		mode = FILE;
		initialized = false;
	}
	
	/**
	 * setup files
	 */
	public void setLocalFiles(String logFile,String txtFile){
		this.documentFile = txtFile;
		this.logFile = logFile;	 
	}
	
	
	/**
	 * set operation mode
	 */	
	public void setMode(int m){
		mode = m;
	}
	
	/**
	 * add operation mode
	 */	
	public void addMode(int m){
		mode |= m;
	}
	
	// check if current mode is X
	private boolean isMode(int m){
		return (mode & m) == m;	
	}
	
	/**
	 * this method will be called to initialize files
	 * if local or remote files are used
	 */
	private void init(){
		if(isMode(FILE)){
			try{
				this.writer = new BufferedWriter(new FileWriter(logFile));
			}catch(IOException ex){
				ex.printStackTrace();	
			}
		}
		/*
		// server should init logfile automaticly
		if(isMode(SERVER)){
			// init log on server
			Parcel prcl = new Parcel("initLog",Util.getUsername(),null,null);
			Util.sendObject(prcl);
		}*/
		initialized = true;
	}
	
	/**
	 * flush insert or delete buffer;
	 */	
	private void flushInsertBuffer(){
		if(insertBuffer.length() > 0){
			processInfo(currentInsertSection,insertBuffer.toString(),insertOffset,insertTime,true);
			// delete buffer content
			insertBuffer = new StringBuffer("");
		}
	}
	
	//get time
	private String getTime(){
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis() - startTime);
		return nf.format(c.get(Calendar.MINUTE))+":"+nf.format(c.get(Calendar.SECOND));
	}
	
	
	// send information to approriate location
	private void processInfo(String section, String text, int offset, long itime, boolean op){
		String oper = "";
		
		if(!isMode(REPORT) && (isMode(FILE) || isMode(SERVER))){
			oper = (op)?"ADD  to  ":"REM from ";
			String time = getTime();
			processString(oper+section+" ("+time+"): "+text+"\n");
		}
		
		if(isMode(PROTOCOL)){
			oper = (op)?ACTION_ADDED:ACTION_REMOVED;
			//protocol.notifyText(text,oper,section,time,offset,itime);
			text = text.replaceAll("\n","\\\\n");
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(doc.getReportData().getReportInterface(),TYPE_TEXT,text,oper);
			ie.getInputMap().put("section",section);
			ie.getInputMap().put("offset",""+offset);
			ie.getInputMap().put("time",""+itime);
			ie.getInputMap().put("text",text);
			Communicator.getInstance().sendMessage(ie);
			
		}
	}
	
	
	// process string save in local file or send it to servler
	private void processString(String str){
		if(!initialized)
			init();
		
		// flash to file
		if(isMode(FILE)){
			try{
				writer.write(str);
				writer.flush();
			}catch(IOException ex){
				ex.printStackTrace();	
			}	
		}
	}
	
	public void insertString(int offset, String str, String section){
		typing = true;
		// flash remove buffer if set
		//flushRemoveBuffer();
		
		// remove special character that is used elsewhere
		if(str.indexOf("<") > -1)
			str = str.replaceAll("<"," ");
		
		
		// setup timer for the first time
		if(startTime == 0)
			startTime = System.currentTimeMillis();
		
		//remember offset when creating new buffer
		if(insertBuffer.length() == 0){
			insertOffset = offset;
			insertTime = System.currentTimeMillis();
		}
		
		// make sure that the next inserted thing is in fact 
		// contigous text, if not flush previous buffer 
		if(insertOffset + insertBuffer.length() != offset){
			flushInsertBuffer();
			insertOffset = offset;
			insertTime = System.currentTimeMillis();
		}
		
		
		currentInsertSection = section;
		insertBuffer.append(str);
		
		// flash buffer if newline is typed or there are more then 1 chars typed
		if(str.length() > 1 || str.endsWith("\n"))
			flushInsertBuffer();
		
	}
	
	public void removeString(int offset, String str, String section){
		
		// if user is typing then, characters must be removed due to backspace
		if(typing && insertBuffer.length() > 0){
			insertBuffer.append("<");
		}else{
			// flush buffert
			flushInsertBuffer();
			// process remove right away
			processInfo(section,str,offset,System.currentTimeMillis(),false);
		}
	}
		
	public void updateOffset(int offset){
		if(startTime == 0)
			startTime = System.currentTimeMillis();
		// flash buffers
		flushInsertBuffer();
		//flushRemoveBuffer();
		typing = false;
	}
	
	public void finishReport(){
		// flush buffers
		flushInsertBuffer();
		//flushRemoveBuffer();
		
		// finish writing log file
		finish();
	}
	
	// flush buffers
	private void finish(){
		try{
			if(!initialized)
				init();

			// get entire report
			String text = doc.getText(0,doc.getLength());	
			
			// do appropriate thing based on the method
			if(isMode(FILE)){
				// close log writer
				writer.flush();
				writer.close();
				writer = null;
				
				// write out entire report
				BufferedWriter writer = new BufferedWriter(new FileWriter(documentFile));
				writer.write(text);
				writer.flush();
				writer.close();
			}	
				
			if(isMode(PROTOCOL)){
				//protocol.notifyReport(text);
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(doc.getReportData().getReportInterface(),TYPE_REPORT,TYPE_REPORT,ACTION_SUBMIT);
				ie.getInputMap().put("text",text);
				Communicator.getInstance().sendMessage(ie);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
