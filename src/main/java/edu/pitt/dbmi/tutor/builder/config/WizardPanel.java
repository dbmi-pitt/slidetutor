package edu.pitt.dbmi.tutor.builder.config;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Properties;

/**
 * this interface represents individual wizard panel
 * @author tseytlin
 */
public interface WizardPanel {
	/**
	 * add property change listener
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l);
	
	/**
	 * remove property change listener
	 * @param l
	 */
	public void removePropertyChangeListener(PropertyChangeListener l);
	
	/**
	 * get component that represents this panel
	 * @return
	 */
	public Component getComponent();
	
	/**
	 * get name of this panel
	 * @return
	 */
	public String getName();
	
	/**
	 * get name of this panel
	 * @return
	 */
	public String getShortName();
	
	/**
	 * get description of this panel
	 * @return
	 */
	public String getDescription();
	
	
	/**
	 * get properties modified by this panel
	 * @return
	 */
	public Properties getProperties();
	
	
	/**
	 * set properties this panel is responsible for
	 * @return
	 */
	public void setProperties(Properties map);
	
	
	/**
	 * revert to default values
	 */
	public void revert();
	
	/**
	 * save values
	 */
	public void apply();
	
	/**
	 * load values from properties
	 */
	public void load();
	
	/**
	 * was this wizard panel visited
	 * @param b
	 */
	public boolean isVisited();
}
