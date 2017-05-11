package edu.pitt.dbmi.tutor.builder.protocol.scripts;

import java.io.OutputStream;
import java.io.PrintStream;

import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;

/**
 * generic script wrapper
 * @author tseytlin
 *
 */
public interface ProtocolScript {
	/**
	 * get name of the script
	 * @return
	 */
	public String getName();
	
	/**
	 * get script description
	 * @return
	 */
	public String getDescription();
	
	
	/**
	 * this is where magic happens, this is where protocol session is given
	 * @param session
	 * @return
	 */
	public boolean process(Session session);
	
	
	/**
	 * set output stream for progress tracking
	 * @param os
	 */
	public void setOutput(PrintStream out);
	
	
	/**
	 * set the source protocol module
	 * @param m
	 */
	public void setProtocolModule(ProtocolModule m);
	
	/**
	 * initialize necessary script resources
	 */
	public void initialize();
	
	/**
	 * dispose necessary script resources
	 */
	public void dispose();
}
