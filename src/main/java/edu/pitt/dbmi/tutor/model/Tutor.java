package edu.pitt.dbmi.tutor.model;

import java.beans.*;
import java.awt.Component;
import javax.swing.Icon;


import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;

import java.util.Properties;

/**
 * This interface describes the behavior of ITS
 * that can be used as a stand alone system as well as 
 * the component of PathTutor
 * @author tseytlin
 *
 */
public interface Tutor extends PropertyChangeListener {
     
    /**
     * Initialize the tutor with a given tutor name.
     * The tutor assumes that the Config object already has
     * the properties file loaded.
     */
    public void initialize(String name) throws TutorException;
    
    /**
     * Get tutor name
     */
    public String getName();
    
	/**
	 * get tutor unique id (like short name)
	 * @return
	 */
	public String getId();
	
    
    
    /**
     * Get icon name
     */
    public Icon getIcon();
  
    /**
     * get description of this tutor
     * @return
     */
    public String getDescription();
    
    
    /**
     * get a version of this tutor
     * @return
     */
    public String getVersion();
    
    
    /**
     * Get experiment condition
     * @return condition
     */
    public String getCondition();

    /**
     * set condition
     * @param s
     */
    public void setCondition(String s);
    
	/**
	 * get UI component associated with this module
	 * if this module has no UI component, null is returned
	 * @return
	 */
	public Component getComponent();
    
	
	/**
	 * get interface module
	 * @return
	 */
	public InterfaceModule getInterfaceModule();
	
	/**
	 * set interface module
	 * @param module
	 */
	public void setInterfaceModule(InterfaceModule module);
	
	/**
	 * get feedback module
	 * @return
	 */
	public FeedbackModule getFeedbackModule();
	
	
	/**
	 * set feedback module
	 * @param module
	 */
	public void setFeedbackModule(FeedbackModule module);
	
		
	/**
	 * get presentation module
	 * @return
	 */
	public PresentationModule getPresentationModule();
		
	/**
	 * set presentation module
	 * @param module
	 */
	public void setPresentationModule(PresentationModule module);
	
	
	/**
	 * get reasoning module
	 * @return
	 */
	public ReasoningModule getReasoningModule();
	
	
	/**
	 * set reasoning module
	 * @param module
	 */
	public void setReasoningModule(ReasoningModule module);
	
	/**
	 * get expert module
	 * @return
	 */
	public ExpertModule getExpertModule();
	
	/**
	 * set expert module
	 */
	public void setExpertModule(ExpertModule module);
	
	
	/**
	 * get student module
	 * @return
	 */
	public StudentModule getStudentModule();
	
	/**
	 * set student module
	 * @param module
	 */
	public void setStudentModule(StudentModule module);
	
	/**
	 * get protocol module
	 * @return
	 */
	public ProtocolModule getProtocolModule();
		
	/**
	 * set protocol module
	 * @param module
	 */
	public void setProtocolModule(ProtocolModule module);
	
	/**
	 * get behavioral module
	 * @return
	 */
	public BehavioralModule getBehavioralModule();
	
	/**
	 * set behavioral module
	 * @return
	 */
	public void setBehavioralModule(BehavioralModule m);
	
	
	/**
	 * release any resources before disposing of resource
	 */
	public void dispose();
	
    /**
     * Enable/disable tutor
     * @param b
     */
    public void setEnabled(boolean b);
    
      
    /**
     * Start a new problem/case
     * @param  String case name, null if unknown case
     */
    public void openCase(String name);

    /**
     * Finish opened problem/case
     */
    public void closeCase();
    
    /**
     * Does tutor currently has a loaded problem?
     * @return
     */
    public boolean isCaseOpen();
    
    /**
     * get currently loaded case object
     * @return
     */
    public CaseEntry getCase();
    
    /**
     *  exit tutor
     */
    public void exit();
    
    /**
     * Is tutor in the process of exiting
     * @return true if tutor is exiting
     */
    public boolean isExiting();
    
    /**
     * Add property change listener
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * Add property change listener
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener reportTutor);
      
    /**
     * fire new property change event
     * @param prop
     * @param o
     * @param n
     */
    public void firePropertyChange(String prop, Object o, Object n);
    
    
    /**
     * Get Event handler for this tutor
     */
    public TutorEventHandler getTutorEventHandler();
    
    /**
     * sets tutor manager
     * @param m
     */
    public void setTutorManager(TutorManager m);
    
    /**
     * set tutor to interactive mode
     * when tutor is not in interactive mode, tutor shouldn't bring up
     * any modal dialog boxes
     * @param b
     */
    public void setInteractive(boolean b);
    
    /**
     * is tutor in interactive mode?
     * @return
     */
    public boolean isInteractive();
    
    /**
	 * get default properties that are available for this
	 * module
	 * @return
	 */
	public Properties getDefaultConfiguration();
	
	/**
	 * resolve tutor related action
	 * @param action
	 */
	public void resolveAction(Action action);
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions();
	
	/**
	 * reset all resources
	 */
	public void reset();
}
