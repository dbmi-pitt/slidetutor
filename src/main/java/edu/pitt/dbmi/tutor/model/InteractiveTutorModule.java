package edu.pitt.dbmi.tutor.model;

import java.awt.Component;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JMenu;


/**
 * this tutor module is an intergral part of a tutor UI
 * @author tseytlin
 *
 */
public interface InteractiveTutorModule extends TutorModule {
	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b);
	
	/**
	 * is component enabled
	 * @return
	 */
	public boolean isEnabled();
	
	/**
	 * set component interactive flag
	 * @param b
	 */
	public void setInteractive(boolean b);
	
	/**
	 * is interactive
	 * @return
	 */
	public boolean isInteractive();
	
	
	/**
	 * get tutor
	 * @return
	 */
	public Tutor getTutor();
	
	
	/**
	 * set tutor
	 * @return
	 */
	public void setTutor(Tutor t);
	
	
	/**
	 * screenshot of given component
	 * @return
	 */
	public ImageIcon getScreenshot();
	
	/**
	 * get UI component associated with this module
	 * if this module has no UI component, null is returned
	 * @return
	 */
	public Component getComponent();
	
	/**
	 * reconfigure this module so that it could take into account
	 * potential changes in the configuration
	 */
	public void reconfigure();
	
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual();
}
