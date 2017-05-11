package edu.pitt.dbmi.tutor.modules;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class GenericTutor implements Tutor {
	// tutor component
	private InterfaceModule interfaceModule;
	private FeedbackModule feedbackModule;
	private PresentationModule presentationModule;
	private ReasoningModule reasoningModule;
	private ProtocolModule protocolModule;
	private BehavioralModule behavioralModule;
	private ExpertModule expertModule;
	private StudentModule studentModule;
	private List<TutorModule> tutorModules,unloadedModules;
	private CaseEntry currentCase;
	private boolean interactive,enabled;
	private String name,description,version,id,condition;
	private Icon icon;
	private Component component;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	/**
	 * initialize the tutor from already loaded config file
	 */
	public void initialize(String id) throws TutorException {
		this.id = id;
		tutorModules = new ArrayList<TutorModule>();
		unloadedModules = new ArrayList<TutorModule>();
		
		// check if such tutor exists
		String name = Config.getProperty("tutor."+id+".name");
		if(name.length() == 0)
			throw new TutorException("Could not find tutor "+id+" in configuration file "+Config.getConfigLocation());
		
		// initialize all of the fields
		setName(name);
		setDescription(Config.getProperty("tutor."+id+".description"));
		setVersion(Config.getProperty("tutor."+id+".version"));
		setCondition(Config.getProperty("tutor."+id+".condition"));
		setIcon(UIHelper.getIcon(Config.getProperty("tutor."+id+".icon")));
		
		// initialize modules
		try{
			setInterfaceModule((InterfaceModule) Config.loadTutorModule("tutor."+id+".interface.module"));
			setFeedbackModule((FeedbackModule) Config.loadTutorModule("tutor."+id+".feedback.module"));
			setPresentationModule((PresentationModule) Config.loadTutorModule("tutor."+id+".presentation.module"));
			setReasoningModule((ReasoningModule)Config.loadTutorModule("tutor."+id+".reasoning.module"));
			setBehavioralModule((BehavioralModule)Config.loadTutorModule("tutor."+id+".behavioral.module"));
			
			// add other misc modules
			for(TutorModule m: Config.loadTutorModules("tutor."+id+".other.modules"))
				addOtherModule(m);
			
		}catch(Exception ex){
			throw new TutorException("Unable to load components for "+id+" tutor",ex);
		}
		
		// add all of the modules to message listener
		//for(TutorModule m: tutorModules)
		//	Communicator.getInstance().addRecipient(m);
	}
	
	
	/**
	 * get tutor unique id (like short name)
	 * @return
	 */
	public String getId() {
		return id;
	}


	/**
	 * get split panel
	 * @param format  Ex: V|slide1|PresentationModule|0|300
	 * @return
	 */
	private Component getComponentPanel(String format) throws Exception {
		format = format.trim();
		String [] parts = Config.getProperty("tutor."+id+".layout."+format).split("\\|");
		
		// if there is only one thing in the list, then format is that thing
		if(parts.length == 1 && !TextHelper.isEmpty(parts[0]))
			format = parts[0];
		
		if(parts.length >= 3){
			// if split in three then it is a split string
			final JSplitPane split = new JSplitPane();
			split.setOrientation((parts[0].equalsIgnoreCase("V")?
			JSplitPane.VERTICAL_SPLIT:JSplitPane.HORIZONTAL_SPLIT));
			split.setLeftComponent(getComponentPanel(parts[1]));
			split.setRightComponent(getComponentPanel(parts[2]));
			if(parts.length > 3){
				try{
					split.setResizeWeight(Double.parseDouble(parts[3]));
				}catch(NumberFormatException ex){
					//ex.printStackTrace();
					Config.getLogger().warning("Could not parse resize weight of the splitter in the layout of "+id);
				}
			}
			if(parts.length > 4){
				try{
					final double div = Double.parseDouble(parts[4]);
					// add ui action
					split.addHierarchyListener(new HierarchyListener() {
						public void hierarchyChanged(HierarchyEvent e) {
							if((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) !=0 && split.isShowing()) {
								// if size is 0, do something damn it
								if(split.getSize().width == 0)
									split.setSize(getComponent().getSize());
								
								split.setDividerLocation(div);
								split.removeHierarchyListener(this);
							}
						}
					});
				}catch(NumberFormatException ex){
					//ex.printStackTrace();
					Config.getLogger().warning("Could not parse divider location of the splitter in the layout of "+id);
				}
			}
			return split;
		}else{
			// try to find matching component
			for(TutorModule module: tutorModules){
				if(UIHelper.isMatchingModule(module,format)){
					return ((InteractiveTutorModule) module).getComponent();
				}
			}
		}
		// oops, I guess this is where we die???
		return null;
	}
	
	
	/**
	 * return a component that represents this system
	 */
	public Component getComponent() {
		if(component == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			
			// parse the tutor layout
			try{
				panel.add(getComponentPanel("main"),BorderLayout.CENTER);
			}catch(Exception ex){
				ex.printStackTrace();
				Config.getLogger().severe("Could not load tutor layout for "+id);
			}
		
			// assign panel
			component = panel;
		}
		return component;
	}

	/**
	 * perform layout of the UI
	 */
	public void doLayout(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				JPanel panel = (JPanel) getComponent();
				// parse the tutor layout
				try{
					// reconfigure all components
					for(TutorModule module: tutorModules){
						if(module instanceof InteractiveTutorModule ){
							((InteractiveTutorModule) module).reconfigure();
						}
					}
					// redo the layout
					panel.removeAll();
					panel.add(getComponentPanel("main"),BorderLayout.CENTER);
					panel.revalidate();
				}catch(Exception ex){
					ex.printStackTrace();
					Config.getLogger().severe("Could not load tutor layout for "+id);
				}
			}
		});	
	}
	
	
	/**
	 * close case
	 */
	public void closeCase() {
		/*
		reasoningModule.reset();
		presentationModule.reset();
		interfaceModule.reset();
		feedbackModule.reset();
		
		// stop behavior module
		behavioralModule.stop();
		*/
		reset();
		setEnabled(false);
	}
	
	/**
	 * open case
	 */
	public void openCase(String name) {
		// close previous case
		if(isCaseOpen())
			closeCase();
		
		// get case entry
		currentCase = expertModule.getCaseEntry(name);

		//load case into components
		if(reasoningModule != null)
			reasoningModule.setCaseEntry(currentCase);
		if(presentationModule != null)
			presentationModule.setCaseEntry(currentCase);
		if(interfaceModule != null)
			interfaceModule.setCaseEntry(currentCase);
		if(feedbackModule != null)
			feedbackModule.setCaseEntry(currentCase);
		if(behavioralModule !=  null)
			behavioralModule.start();
		// enable case
		setEnabled(true);
		setInteractive(true);
		
		// notify of open case
	}
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);

	}
	
	public void dispose() {
		if(isCaseOpen())
			closeCase();
	}

	public void reset(){
		// reset all modules
		for(TutorModule m : new ArrayList<TutorModule>(tutorModules))
			m.reset();
		for(TutorModule m : new ArrayList<TutorModule>(unloadedModules))
			m.reset();
	}
	
	
	public void exit() {
		dispose();

	}

	public CaseEntry getCase() {
		return currentCase;
	}

	public String getCondition() {
		return condition;
	}

	public String getDescription() {
		return description;
	}

	public ExpertModule getExpertModule() {
		return expertModule;
	}

	public FeedbackModule getFeedbackModule() {
		return feedbackModule;
	}

	public InterfaceModule getInterfaceModule() {
		return interfaceModule;
	}

	public String getName() {
		return name;
	}

	public PresentationModule getPresentationModule() {
		return presentationModule;
	}

	public BehavioralModule getBehavioralModule(){
		return behavioralModule;
	}
	
	public ProtocolModule getProtocolModule() {
		return protocolModule;
	}

	public ReasoningModule getReasoningModule() {
		return reasoningModule;
	}

	public StudentModule getStudentModule() {
		return studentModule;
	}

	
	public TutorEventHandler getTutorEventHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getVersion() {
		return version;
	}

	public boolean isCaseOpen() {
		return currentCase != null;
	}

	
	public boolean isExiting() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isInteractive() {
		return interactive;
	}

	

	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);

	}

	public void setEnabled(boolean b) {
		enabled = true;
		if(interfaceModule != null)
			interfaceModule.setEnabled(b);
		if(feedbackModule != null)
			feedbackModule.setEnabled(b);
		if(presentationModule != null)
			presentationModule.setEnabled(b);
	}

	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public void setExpertModule(ExpertModule module) {
		if(module == null)
			return;
		
		if(expertModule != null){
			tutorModules.remove(expertModule);
			module.sync(expertModule);
		}
		
		module.load();
		expertModule = module;
		tutorModules.add(expertModule);
		Communicator.getInstance().addRecipient(module);
		
		// load expert modules into modules that care
		if(presentationModule != null)
			presentationModule.setExpertModule(module);
		if(interfaceModule != null)
			interfaceModule.setExpertModule(module);
		if(reasoningModule != null)
			reasoningModule.setExpertModule(module);
		if(feedbackModule != null)
			feedbackModule.setExpertModule(module);
	}

	public void setFeedbackModule(FeedbackModule module) {
		if(module == null)
			return;
		
		tutorModules.remove(feedbackModule);
		
		module.setTutor(this);
		module.load();
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		if(getExpertModule() != null)
			module.setExpertModule(getExpertModule());
		
		if(feedbackModule != null)
			module.sync(feedbackModule);
		
		feedbackModule = module;
	}

	/**
	 * add other modules
	 * @param module
	 */
	private void addOtherModule(TutorModule module){
		if(module == null)
			return;
		
		if(module instanceof InteractiveTutorModule)
			((InteractiveTutorModule)module).setTutor(this);
		module.load();
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
	}
	
	
	public void setInteractive(boolean b) {
		if(feedbackModule != null)
			feedbackModule.setInteractive(b);
		if(presentationModule != null)
			presentationModule.setInteractive(b);
		if(interfaceModule != null)
			interfaceModule.setInteractive(b);
		interactive = b;
	}

	public void setInterfaceModule(InterfaceModule module) {
		if(module == null)
			return;
		
		tutorModules.remove(interfaceModule);
		
		module.load();
		module.setTutor(this);
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		if(getExpertModule() != null)
			module.setExpertModule(getExpertModule());
		
		if(interfaceModule != null)
			module.sync(interfaceModule);
		
		interfaceModule = module;
	}

	public void setPresentationModule(PresentationModule module) {
		if(module == null)
			return;
		
		
		tutorModules.remove(presentationModule);
		
		module.load();
		module.setTutor(this);
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		if(getExpertModule() != null)
			module.setExpertModule(getExpertModule());
		
		if(presentationModule != null)
			module.sync(presentationModule);
		
		presentationModule = module;
	}

	public void setProtocolModule(ProtocolModule module) {
		if(module == null)
			return;
		
		tutorModules.remove(protocolModule);
		
		module.load();
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		//if(protocolModule != null)
		//	module.sync(protocolModule);
		
		protocolModule = module;
		
	}

	public void setReasoningModule(ReasoningModule module) {
		if(module == null)
			return;
		
		tutorModules.remove(reasoningModule);
		
		module.load();
		module.setTutor(this);
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		if(getExpertModule() != null)
			module.setExpertModule(getExpertModule());
		
		if(reasoningModule != null)
			module.sync(reasoningModule);
		
		reasoningModule = module;
	}
	
	public void setBehavioralModule(BehavioralModule module) {
		if(module == null)
			return;
		
		tutorModules.remove(behavioralModule);
		
		module.setTutor(this);
		module.load();
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		if(behavioralModule != null)
			module.sync(behavioralModule);
		
		behavioralModule = module;
	}
	

	public void setStudentModule(StudentModule module) {
		if(module == null)
			return;
		
		tutorModules.remove(studentModule);
		
		module.load();
		tutorModules.add(module);
		Communicator.getInstance().addRecipient(module);
		
		if(studentModule != null)
			module.sync(studentModule);
		
		studentModule = module;
	}

	
	public void setTutorManager(TutorManager m) {
		// TODO Auto-generated method stub

	}

	
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub

	}



	public Properties getDefaultConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions(){
		List<Action> actions = new ArrayList<Action>();
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_FINISH_CASE,"","Display <font color=green>Next Case</font> button to allow users to switch to the next case."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,"<tutor module class>",
				"Load a different tutor module in place of the one that is currently loaded. Do not use it, unless you know what you are doing."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,"<layout>",
				"Change the user interface layout of the tutor. This action is used internally. Do not use it, unless you know what you are doing."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_PROMPT_USER,"message","Display a popup dialog to a user with input message."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_PROMPT_USER,"question [prompt|yes;no]",
				"Display a popup dialog to a user with input question and yes/no or ok/cancel options."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_PROMPT_USER,"question [choice|opt1;opt2]",
				"Display a popup dialog to a user with a multiple choice question."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_PROMPT_USER,"question [text]",
				"Display a popup dialog to a user with input question and a text box for free text input."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_PROMPT_USER,"question [scale|min;max;lbl1;lbl2]",
				"Display a popup dialog to a user with the question and a Likert scale with optional label for minimum and maximum."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"true","Enable interactive mode in a tutor."));
		actions.add(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"false","Disable interactive mode in a tutor."));
		return actions.toArray(new Action [0]);
	}
	
	/**
	 * resolve tutor related action
	 * @param action
	 */
	public void resolveAction(Action action){
		final Action act = action;
		Operation oper = null;
				
		// figure out which operations to set
		if(POINTER_ACTION_FINISH_CASE.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					String str = act.getInput();
					if(TextHelper.isEmpty(str) || getId().equals(str))
						firePropertyChange(PROPERTY_CASE_DONE, null,getId());
				}
				public void undo(){}
			};
		}else if(POINTER_ACTION_SWITCH_TUTOR_MODULE.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					switchTutorModule(act.getInput());
				}
				public void undo(){}
			};
		}else if(POINTER_ACTION_DO_LAYOUT.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				Properties orig;
				public void run() {
					orig = Config.getProperties(getId()+".layout.");
					Properties p = new Properties();
					for(String s : act.getInput().split("\n")){
						String [] v = s.split("\\s*=\\s*");
						if(v.length > 1){
							p.setProperty(v[0].trim(),v[1].trim());
						}
					}
					// if properties are the same, don't do anything
					if(!orig.equals(p)){
						Config.setProperties(p);
						doLayout();
					}
				}
				public void undo(){
					Config.setProperties(orig);
					doLayout();
				}
			};
		}else if(POINTER_ACTION_PROMPT_USER.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					doPromptUser(act.getInput());
				}
				public void undo(){
		
				}
			};
		}else if(POINTER_ACTION_SET_INTERACTIVE.equalsIgnoreCase(action.getAction())){
			final boolean oldVal = isInteractive();
			//set viewer location
			oper = new Operation(){
				public void run() {
					setInteractive(Boolean.parseBoolean(act.getInput()));
				}
				public void undo(){
					setInteractive(oldVal);
				}
			};
		}
		
		// set operations
		action.setOperation(oper);
	}
	
	
	/**
	 * prompt user based on input condition
	 * @param input
	 */
	protected void doPromptUser(String input) {
		//TODO: perhaps in future show dialog, and close it
		if(!isInteractive())
			return;
		
		// extract format information
		Pattern pt = Pattern.compile("(.+)\\[(.+)\\]");
		Matcher mt = pt.matcher(input);
		
		// split question into text and format
		String text = input.trim();
		String format = null;
		
		if(mt.matches()){
			text = mt.group(1).trim();
			format = mt.group(2);
		}
		
		// notify 
		String reply = UIHelper.promptUser(text, format);
		if(reply != null)
			reply = reply.trim();
		
		String label = (reply != null)?text+" = "+reply:text;
		ClientEvent ce = ClientEvent.createClientEvent(null,TYPE_QUESTION,label,ACTION_ASK);
		ce.setSource(this.getClass().getSimpleName());
		Map<String,String> map = new LinkedHashMap<String,String>();
		map.put("Question",text);
		if(reply != null)
			map.put("Answer",reply);
		ce.setInput(map);
		
		// send request
		Communicator.getInstance().sendMessage(ce);
	}


	
	

	public void firePropertyChange(String prop, Object o, Object n) {
		pcs.firePropertyChange(prop,o, n);
		
	}

	
	/**
	 * set a generic tutor module from string descriptor
	 * this should set module based on type
	 * @param s
	 */
	public void switchTutorModule(String str){
		if(str == null)
			return;
		// indicate that modules are being switched out
		Config.getProperties().setProperty("temp.replacing.module",Boolean.TRUE.toString());
		
		// search already unloaded modules
		// if they match
		TutorModule tm = null;
		for(TutorModule t: unloadedModules){
			if(t.getClass().getName().equals(str)){
				tm = t;
			}
		}
		
		// if there is no unloaded module, lets initialize new one
		if(tm == null){
			tm = UIHelper.getTutorModule(str,TutorModule.class);
		}
		
		// set appropriate module
		if(tm instanceof InterfaceModule){
			InterfaceModule om = getInterfaceModule();
			// don't do anything if we are replacing a
			// similar module. s.a. Simip
			if(!om.getClass().getName().equals(str)){
				unloadedModules.add(om);
				setInterfaceModule((InterfaceModule)tm);
				replaceModuleComponent(om,getInterfaceModule());
			}			
		}else if(tm instanceof FeedbackModule){
			FeedbackModule om = getFeedbackModule();
			// don't do anything if we are replacing a
			// similar module. s.a. Simip
			if(!om.getClass().getName().equals(str)){
				unloadedModules.add(om);
				setFeedbackModule((FeedbackModule)tm);
				replaceModuleComponent(om,getFeedbackModule());
			}
		}else if(tm instanceof ReasoningModule){
			ReasoningModule om = getReasoningModule();
			// don't do anything if we are replacing a
			// similar module. s.a. Simip
			if(!om.getClass().getName().equals(str)){
				unloadedModules.add(om);
				setReasoningModule((ReasoningModule)tm);
			}
		}else if(tm instanceof PresentationModule){
			PresentationModule om = getPresentationModule();
			// don't do anything if we are replacing a
			// similar module. s.a. Simip
			if(!om.getClass().getName().equals(str)){
				unloadedModules.add(om);
				setPresentationModule((PresentationModule)tm);
				replaceModuleComponent(om,getPresentationModule());
			}
		}
		
		// operation was completed
		Config.getProperties().setProperty("temp.replacing.module",Boolean.FALSE.toString());
		
	}
	
	/**
	 * replace module component
	 * @param oldModule
	 * @param newModule
	 */
	private void replaceModuleComponent(InteractiveTutorModule oldModule, InteractiveTutorModule newModule){
		Container panel = oldModule.getComponent().getParent();
		if(panel != null){
			int x = -1;
			if(panel instanceof JSplitPane)
				x = ((JSplitPane) panel).getDividerLocation();
			
			int i = panel.getComponentZOrder(oldModule.getComponent());
			panel.remove(oldModule.getComponent());
			panel.add(newModule.getComponent(),i);
			panel.validate();
			
			if(panel instanceof JSplitPane)
				((JSplitPane) panel).setDividerLocation(x);
		}
	}
	
	
	/**
	 * @param args
	 *
	public static void main(String[] args) throws Exception{
		// load config file
		Config.load(System.getProperty("user.home")+"\\Desktop\\SlideTutor.conf");
		
		// initialize components
		DomainExpertModule expertModule = new DomainExpertModule();
		
		// prompt for domain
		DomainSelectorPanel p = (DomainSelectorPanel) expertModule.getComponent();
		JOptionPane.showMessageDialog(null,p,expertModule.getName(),JOptionPane.PLAIN_MESSAGE);
		System.out.println(((IOntology)p.getSelectedObject()).getURI());
		expertModule.openDomain(""+((IOntology)p.getSelectedObject()).getName());
		
		
		// create a silly little tutor
		Tutor tutor = new GenericTutor();
		tutor.initialize("SlideTutor");
		tutor.setExpertModule(expertModule);
		
		// create GUI frame
		JFrame frame = new JFrame("SlideTutor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(tutor.getComponent());
		Config.setMainFrame(frame);
		frame.pack();
		frame.setSize(new Dimension(1024,768));
		
		// create menubar
		JMenuBar menubar = new JMenuBar();
		menubar.add(tutor.getInterfaceModule().getMenu());
		frame.setJMenuBar(menubar);
		
		frame.setVisible(true);
	}
	*/

}
