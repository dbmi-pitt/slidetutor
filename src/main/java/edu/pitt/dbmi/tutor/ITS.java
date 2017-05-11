package edu.pitt.dbmi.tutor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.plaf.metal.MetalLookAndFeel;


import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.modules.GenericTutor;
import edu.pitt.dbmi.tutor.modules.pedagogic.UserCaseSequence;
import edu.pitt.dbmi.tutor.ui.ConfigSelectorPanel;
import edu.pitt.dbmi.tutor.ui.FontPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.OrderedMap;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.dbmi.tutor.util.UIHelper.FocusTimer;
import static edu.pitt.dbmi.tutor.messages.Constants.*;


/**
 * ITS - intelligent tutoring system
 * is the main driver class for the generic Medical ITS
 * @author tseytlin
 */
public class ITS implements ActionListener, ChangeListener, PropertyChangeListener{
	private ExpertModule expertModule;
	private PedagogicModule pedagogicModule;
	private StudentModule studentModule;
	private ProtocolModule protocolModule;
	private UserCaseSequence caseSelector;
	private OrderedMap<String,Tutor> tutors;
	private String condition, domain, name,problem;
	private JTabbedPane tabPanel;
	private JMenuBar menubar;
	private JMenuItem openCase;
	private JMenu debugMenu;
	private JPanel nextPanel;
	private JLabel statusLabel;
	private JPanel statusPanel;
	private JProgressBar progress;
	private JFrame frame;
	private Component helpComponent;
	private static ITS instance;
	private static boolean standAlone;
	
	// some constunts 
	private final String ABOUT_MESSAGE = "<html><h2>SlideTutor ITS</h2>" +
										"<a href=\"http://slidetutor.upmc.edu/\">http://slidetutor.upmc.edu/</a><br>"+
										"Department of BioMedical Informatics<br>University of Pittsburgh";
	
	/**
	 * there can be only one
	 */
	private ITS(){
		// to fix MAC problems fix UI
    	Config.setJavaLookAndFeel();
	}
	
	
	/**
	 * get instance
	 * @return
	 */
	public static ITS getInstance(){
		if(instance == null)
			instance = new ITS();
		return instance;
	}
	
	/**
	 * initialize new tutor w/ a config file
	 * @param config
	 */
	public void initialize(Map config){
		try{
			// load config file
			Config.getProperties().putAll(config);
			init();
		}catch (Exception e) {
			e.printStackTrace();
			Config.getLogger().severe("Could not initialize ITS [ "+TextHelper.getErrorMessage(e.getCause())+" ]");
			String err = "<html>Could not initialize one of the tutor components: <br>"+
						"<font color=red>"+e.getMessage()+"</font><br><br>"+
			            "Please check your configuration file <br>" +
			            "<font color=blue>"+config+"</font>";
			JOptionPane.showMessageDialog(null,err,"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * initialize new tutor w/ a config file
	 * @param config
	 */
	public void initialize(String config){
		try{
			// load config file
			Config.load(config);
			init();
		}catch (Exception e) {
			e.printStackTrace();
			Config.getLogger().severe("Could not initialize ITS [ "+TextHelper.getErrorMessage(e.getCause())+" ]");
			String err = "<html>Could not initialize one of the tutor components: <br>"+
						"<font color=red>"+e.getMessage()+"</font><br><br>"+
			            "Please check your configuration file <br>" +
			            "<font color=blue>"+config+"</font>";
			JOptionPane.showMessageDialog(null,err,"Error",JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	/**
	 * do init
	 * @throws Exception
	 */
	private void init() throws Exception {
		// init variables
		condition = Config.getProperty("tutor.condition");
		domain    = Config.getProperty("tutor.domain");
		name      = Config.getProperty("tutor.name");  
		
		// init modules
		setExpertModule((ExpertModule) Config.loadTutorModule("tutor.expert.module"));
		setProtocolModule((ProtocolModule)Config.loadTutorModule("tutor.protocol.module"));
		setPedagogicModule((PedagogicModule) Config.loadTutorModule("tutor.pedagogic.module"));
		
		// get list of tutors and initialize them
		tutors = new OrderedMap<String,Tutor>();
		for(String name: Config.getListProperty("tutor.list")){
			Tutor tutor = new GenericTutor();
			tutor.addPropertyChangeListener(this);
			tutor.initialize(name);
			tutor.setCondition(condition);
			tutor.setEnabled(false);
			Communicator.getInstance().addRecipient(tutor);
			tutors.put(tutor.getId(),tutor);
		}
	
		// if no tutors, don't bother going further
		if(tutors.isEmpty())
			throw new TutorException("Could not load any of the tutors!");
		
		// load other modules
		for(TutorModule m : Config.loadTutorModules("tutor.other.modules")){
			Communicator.getInstance().addRecipient(m);
		}
		
		// if we have protocol module, then we want it to be the last, since
		// we can have other modules modifying messages
		if(protocolModule != null){
			Communicator.getInstance().removeRecipient(protocolModule);
			Communicator.getInstance().addRecipient(protocolModule);
		}
	}
	
	/**
	 * get list of tutors
	 * @return
	 */
	public List<Tutor> getTutors(){
		return (tutors == null)?Collections.EMPTY_LIST:tutors.getValues();
	}
	
	/**
	 * set tutor in interactive mode
	 * @param b
	 */
	public void setInteractive(boolean b){
		for(Tutor t: getTutors())
			t.setInteractive(b);
	}
	
	
	public ExpertModule getExpertModule() {
		return expertModule;
	}


	public void setExpertModule(ExpertModule e) {
		if(e == null)
			return;
		
		if(expertModule != null){
			e.sync(expertModule);
			Communicator.getInstance().removeRecipient(expertModule);
		}
		e.load();
		this.expertModule = e;
		Communicator.getInstance().addRecipient(expertModule);
	}


	public PedagogicModule getPedagogicModule() {
		return pedagogicModule;
	}


	public void setPedagogicModule(PedagogicModule module) {
		if(module == null)
			return;
		
		if(pedagogicModule != null){
			Communicator.getInstance().removeRecipient(pedagogicModule);
		}
		
		module.setExpertModule(getExpertModule());
		module.setStudentModule(getStudentModule());
		module.setProtocolModule(protocolModule);
		module.load();
		
		if(pedagogicModule != null)
			module.sync(pedagogicModule);
		
		pedagogicModule = module;
		Communicator.getInstance().addRecipient(pedagogicModule);
	}

	public void setProtocolModule(ProtocolModule module){
		if(module == null)
			return;
		
		if(protocolModule != null){
			Communicator.getInstance().removeRecipient(protocolModule);
		}
		module.load();
		protocolModule = module;
		// enable by default based on config
		protocolModule.setEnabled(Config.getBooleanProperty("tutor.protocol.enabled"));
		Communicator.getInstance().addRecipient(protocolModule);
	}
	
	public StudentModule getStudentModule() {
		return studentModule;
	}


	public void setStudentModule(StudentModule m) {
		if(m == null)
			return;
		
		if(studentModule != null){
			m.sync(studentModule);
			Communicator.getInstance().removeRecipient(studentModule);
		}
		m.load();
		this.studentModule = m;
		Communicator.getInstance().addRecipient(studentModule);
	}

	
	
	/**
	 * start using the ITS
	 */
	public void start(){
	
    	// prompt user for misc info if required
    	if(login()){
    		// create UI
	    	show();
	    	
	    	// start next case
	    	if(getPedagogicModule() != null)
	    		start(null);
    	}
	}
	
	/**
	 * start new case
	 * @param problem
	 */
	private void start(String prob){
		final String problem = prob;
		((new Thread(new Runnable(){
    		public void run(){
    	    	setBusy(true);
    	    	
    	    	// disable tutors
    			for(Tutor tutor: getTutors()){
    	    		tutor.setEnabled(false);
    	    	}
    			showNextButton(false);
    			
    	    	
    			// load domain if available right away
		    	getProgressBar().setString("Loading Domain "+domain+" ...");
	    	    openDomain(domain);
		    	
		    	// get next case
	    	    String prob = problem;
	    	    if(prob == null)
	    	    	prob = getPedagogicModule().getNextCase();
	    	    
	    	    // load case
    	    	getProgressBar().setString("Loading Case "+TextHelper.getName(prob)+" ...");
				openCase(prob);
    	    	
			    setBusy(false);
    		}
    	}))).start();
	}
	
	/**
	 * open domaing
	 * @param domain
	 */
	private void openDomain(String domain){
		if(TextHelper.isEmpty(domain))
			return;
		
		// load domain
    	expertModule.openDomain(domain);
    	for(Tutor tutor: getTutors()){
    		tutor.setExpertModule(expertModule);
    		tutor.setEnabled(true);
    	}
    	
    	// change status label
    	statusLabel.setText("<html><b>domain<b>: <font color=blue>"+
    					"</font> "+expertModule.getDomain());
	}
	
	/**
	 * is case open
	 * @return
	 */
	private boolean isCaseOpen(){
		if(tutors != null && !tutors.isEmpty())
			return tutors.getValues().get(0).isCaseOpen();
		return false;
	}
	
		
	/**
	 * close case
	 */
	private void closeCase(){
		// close previos case
		if(isCaseOpen()){
			String outcome = getCaseOutcome();
			
			// send event that case was opened
			Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(null,TYPE_END,outcome,ACTION_CLOSED));
			
			// close cases in tutors
			for(Tutor tutor: getTutors()){
	    		tutor.closeCase();
	    	}
			
			// notify the protocol
			ProblemEvent evt = ProblemEvent.createEndProblemEvent(outcome);
			for(ProtocolModule protocolModule : getProtocolModules())
				protocolModule.closeCaseSession(evt);
		}
	}

	public String getCase(){
		return problem;
	}
	
	/**
	 * open domain
	 * @param domain
	 */
	public void openCase(String problem){
		this.problem = problem;
		// check for validity
		if(problem == null){
			//JOptionPane.showMessageDialog(frame,
			//		"Tutor was unable to load next case!",
			//		"Error",JOptionPane.ERROR_MESSAGE);
			exit();
			return;
		}
		
		//close previous case
		closeCase();
		
		
		// disable next case
		showNextButton(false);
		
		
		// make sure that right domain is open
		if(UIHelper.isURL(problem)){
			String caseDomain = OntologyHelper.getDomainFromCase(problem);
			if(!caseDomain.equals(getExpertModule().getDomain())){
				getProgressBar().setString("Loading Domain "+caseDomain+" ...");
				openDomain(caseDomain);
			}
		}else{
			// if problem is not in url form, convert it to URL
			problem = ""+OntologyHelper.getCaseURL(problem,getExpertModule().getDomainOntology().getURI());
		}
		
		
		// load into protocol
		ProblemEvent evt = ProblemEvent.createStartProblemEvent(problem);
		for(ProtocolModule protocolModule : getProtocolModules())
			protocolModule.openCaseSession(evt);
		
		// load case for each tutor
		for(Tutor tutor: getTutors()){
    		tutor.openCase(problem);
    	}
	
		// send event that case was opened
		Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(null,TYPE_START,problem,ACTION_OPENED));
		
		
		// change status label
    	statusLabel.setText("<html><b>DOMAIN<b>: <font color=blue>"+
    			TextHelper.getName(expertModule.getDomain())+
    			"</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; " +
    			"<b>CASE</b>: <font color=green>"+TextHelper.getName(problem)+"</font>");
	}
	
	
	/**
	 * prompt for login info if necessary
	 */
	private boolean login(){
		if(getProtocolModule() != null && Config.getBooleanProperty("tutor.login.enabled")){
			String study = Config.getProperty("tutor.login.study");
			String error = null;
			do{
				String [] p = UIHelper.promptLogin(error);
				// exit on cancel
				if(p == null){
					exit();
					return false;
				}else{
					Config.setProperty("username",p[0]);
					Config.setProperty("password",p[1]);
				}
				error = "Authentication failed";
			}while(!getProtocolModule().authenticateUser(Config.getUsername(),Config.getPassword(),study));
		}
		return true;
	}
	
	/**
	 * exit the tutoring system
	 */
	public void exit(){
		String outcome = getCaseOutcome();
		
		for(Tutor tutor: getTutors()){
    		tutor.exit();
    	}
		
		// close case for protocol
		ProblemEvent evt = ProblemEvent.createEndProblemEvent(outcome);
		for(ProtocolModule protocolModule : getProtocolModules())
			protocolModule.closeCaseSession(evt);
		
		// dispose of all reciepients
		dispose();
		
		if(standAlone)
			System.exit(0);
		else if(frame != null){
			frame.dispose();
			frame = null;
			Config.setMainFrame(null);
		}
	}
	
	public void dispose(){
		// if null instances, then don't need to dispose twice
		if(instance == null)
			return;
		
		Communicator.getInstance().removeAllRecipients();
		for(Tutor tutor: getTutors()){
    		tutor.dispose();
    	}
	
		// dispose of all protocols as well
		for(ProtocolModule pm : getProtocolModules())
			pm.dispose();
		
		//instance = null;
		getDebugMenu().removeAll();
	}
	
	
	/**
	 * get outcome for a case, based on state of a 
	 * @return
	 */
	private String getCaseOutcome(){
		// if next case button is showing
		if(nextPanel != null && nextPanel.isShowing())
			return Constants.OUTCOME_FINISHED;
		
		// case is solved if all of the tutors agree that it is solved
		for(Tutor tutor: getTutors()){
			if(tutor.getReasoningModule() != null && !tutor.getReasoningModule().isSolved())
    			return Constants.OUTCOME_CLOSED;
    	}
		// else case is properly finished
		return Constants.OUTCOME_FINISHED;
	}
	
	
	
	/**
	 * create user interface
	 */
	private void createUI(){
		 // create frame
        frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                exit();
            }
        });
        // set logo
        Icon icon = UIHelper.getIcon(Config.getProperty("icon.general.tutor.logo"));
        if(icon != null && icon instanceof ImageIcon)
        	frame.setIconImage(((ImageIcon)icon).getImage());
        
        Component content = null;
        int last = tutors.size()-1;
        // if there is only one tutor, then we don't need a tabbed panel
        if(tutors.size() > 1){
	        JTabbedPane tabPanel = new JTabbedPane();
	        tabPanel.setPreferredSize(new Dimension(500,650));
	        tabPanel.setBackground(Color.white);
	        
	        // add component tutors
	        for(Tutor tutor: tutors.getValues()){
	        	tabPanel.addTab(tutor.getName(),tutor.getIcon(),tutor.getComponent());
	        }
	        content = null;
        }else if(tutors.isEmpty()){
        	//error has occured, so quit
        	exit();
        }else{
        	content = tutors.getValues().get(last).getComponent();
        }
      
        // add menu
        frame.setJMenuBar(getMenuBar());
        
	    // add selected 
	    setInterfaceMenu(tutors.getValues().get(0));
	    
	    // create status panel
	    statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout());
		statusLabel = new JLabel(" ");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN));
		statusPanel.add(statusLabel,BorderLayout.CENTER);
	    
		// progress bar
		progress = new JProgressBar();
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		progress.setIndeterminate(true);
		statusPanel.setPreferredSize(progress.getPreferredSize());
			    
	    // add components
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(content,BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel,BorderLayout.SOUTH); 
        
        // pack everything for main frame
        frame.pack(); 
        
        // resize
    	Dimension size = Config.parseDimension(Config.getProperty("tutor.size"),"tutor.size");
        // put in default size
    	if(size.width == 0)
        	size = new Dimension(1024,768);
	    frame.setSize(size);
    	Config.setMainFrame(frame);
	}
	
	/**
	 * show the interface
	 */
	public void show(){
		if(frame == null){
			createUI();
		}
		frame.setVisible(true);
        UIHelper.centerWindow(frame);
	}
	
	
	/**
	 * get menubar 
	 * @return
	 */
	private JMenuBar getMenuBar(){
		if(menubar == null){
			menubar = new JMenuBar();
			JMenu file = new JMenu("File");
			// add default entry to go to case
			openCase = UIHelper.createMenuItem("goto.case","Open Case ..",
					UIHelper.getIcon(Config.getProperty("icon.menu.open")),this);
			file.add(openCase);
			file.addSeparator();
			file.add(UIHelper.createMenuItem("exit","Exit",UIHelper.getIcon(Config.getProperty("icon.menu.exit")),this));
			
			JMenu help = new JMenu("Help");
			help.add(UIHelper.createCheckboxMenuItem("debug","Debug",null,this));
			help.addSeparator();
			help.add(UIHelper.createMenuItem("help","Help",UIHelper.getIcon(Config.getProperty("icon.menu.help")),this));
			help.add(UIHelper.createMenuItem("about","About",UIHelper.getIcon(Config.getProperty("icon.menu.about")),this));
			
			JMenu opts = new JMenu("Options");
			opts.add(UIHelper.createMenuItem("font-size","Change Font Size",UIHelper.getIcon(Config.getProperty("icon.menu.font")),this));
			
			menubar.add(file);
			menubar.add(opts);
			menubar.add(getDebugMenu());
			menubar.add(help);
			
			// enable open case
			openCase.setEnabled(Config.getBooleanProperty("tutor.open.case.enabled"));
		}
		return menubar;
	}
	
	/**
	 * get debug menu
	 * @return
	 */
	public JMenu getDebugMenu(){
		if(debugMenu == null){
			debugMenu = new JMenu("");
			debugMenu.setEnabled(false);
		}
		
		return debugMenu;
	}
	
	public ProtocolModule getProtocolModule(){
		return protocolModule;
	}

	/**
	 * get a list of protocol modules (yes you can have more then one)
	 * @return
	 */
	public List<ProtocolModule> getProtocolModules(){
		List<ProtocolModule> list = new ArrayList<ProtocolModule>();
		// add THE module first
		if(protocolModule != null)
			list.add(protocolModule);
		
		// add the rest of modules
		for(TutorModule m : Communicator.getInstance().getRegisteredModules()){
			if(m instanceof ProtocolModule && !m.equals(protocolModule))
				list.add((ProtocolModule)m);
		}
		
		return list;
	}
	
	/**
	 * set tutor interface menu
	 * @param t
	 */
	private void setInterfaceMenu(Tutor t){
		JMenu m = (t.getInterfaceModule() != null)?t.getInterfaceModule().getMenu():null;
	    if(m != null){
	    	for(int i=0;i<getMenuBar().getMenuCount();i++){
	    		if(m.getText().equals(getMenuBar().getMenu(i).getText())){
	    			getMenuBar().remove(getMenuBar().getMenu(i));
	    			break;
	    		}
	    	}
	    	getMenuBar().add(m,1);
	    }
	}
	
	/**
	 * action events
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(cmd.equalsIgnoreCase("exit")){
			exit();
		}else if(cmd.equalsIgnoreCase("next")){
			start(pedagogicModule.getNextCase());
		}else if(cmd.equalsIgnoreCase("debug")){
			doDebug((AbstractButton)e.getSource());
		}else if(cmd.equalsIgnoreCase("help")){
			doHelp();
		}else if(cmd.equalsIgnoreCase("about")){
			JOptionPane.showMessageDialog(frame,ABOUT_MESSAGE,"About",
			JOptionPane.PLAIN_MESSAGE,UIHelper.getIcon(Config.getProperty("icon.general.tutor.logo")));
		}else if(cmd.equalsIgnoreCase("goto.case")){
			if(caseSelector == null){
				caseSelector = new UserCaseSequence();
				caseSelector.setExpertModule(getExpertModule());
				caseSelector.setProtocolModule(getProtocolModule());
				caseSelector.load();
			}
			caseSelector.setEnableCasePreview(debugMenu.isEnabled());
			final String name = caseSelector.getNextCase();
			if(name != null){
				((new Thread(new Runnable(){
		    		public void run(){
		    			getProgressBar().setString("Loading Case "+TextHelper.getName(name)+" ...");
		    			setBusy(true);
		    	    	openCase(name);
		    	    	setBusy(false);
		    		}
		    	}))).start();
			}
		}else if(cmd.equals("font-size")){
			FontPanel.getInstance().showDialog(Config.getMainFrame());
		}
		
	}

	/**
	 * is debug mode enabled?
	 * @return
	 */
	public boolean isDebugEnabled(){
		return debugMenu.isEnabled();
	}
	
	
	
	private void doHelp(){
		JDialog d = new JDialog(frame,"User Manual");
		d.setModal(false);
		d.setResizable(true);
		d.getContentPane().add(getHelpComponent());
		d.pack();
		d.setVisible(true);
		UIHelper.centerWindow(frame,d);
	}
	
	/**
	 * create help panel
	 * @return
	 */
	private Component getHelpComponent(){
		if(helpComponent == null){
			final UIHelper.HTMLPanel panel = new UIHelper.HTMLPanel();
			panel.setPreferredSize(new Dimension(850,600));
			panel.setEditable(false);
			panel.addHyperlinkListener(new HyperlinkListener(){
				public void hyperlinkUpdate(HyperlinkEvent e){
					if(e.getEventType() == EventType.ACTIVATED){
						JEditorPane p = (JEditorPane) e.getSource();
						String ref = e.getDescription();
						if(ref.startsWith("#"))
							ref = ref.substring(1);
						p.scrollToReference(ref);
					}
				}
			});
			final Map<String,URL> moduleMap = new LinkedHashMap<String, URL>();
			ActionListener listener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(moduleMap.containsKey(e.getActionCommand())){
						try{
							panel.setPage(moduleMap.get(e.getActionCommand()));
						}catch(Exception ex){
							panel.setText("<center><h3>User Manual Not Available</h3></center><hr>"+ex.getMessage());
						}
						panel.setCaretPosition(0);
					}
				}
			};
			
			
			JPanel t = new JPanel();
			t.setLayout(new GridLayout(-1,1));
			t.setBorder(new TitledBorder("Content"));
			
			ButtonGroup grp = new ButtonGroup();
			String name = "Introduction";
			
			// first intro section
			AbstractButton bt = UIHelper.createToggleButton(name,"<html>View <b>"+name+"</b> Manual",
							Config.getIconProperty("icon.toolbar.bookmark"),-1,true,listener);
			bt.setHorizontalAlignment(SwingConstants.LEFT);
			grp.add(bt);
			t.add(new JLabel(""));
			t.add(bt);
			moduleMap.put(name,Config.getManual(getClass()));
			
			
			for(TutorModule tm : Communicator.getInstance().getRegisteredModules()){
				if(tm instanceof InteractiveTutorModule){
					InteractiveTutorModule itm = (InteractiveTutorModule) tm;
					name = itm.getName();
					
					// shorten the name
					if(itm instanceof PresentationModule){
						name = "Viewer";
					}else if(itm instanceof InterfaceModule){
						name = "Interface";
					}else if(itm instanceof FeedbackModule){
						name = "Feedback";
					}
					
					// get back to original if taken
					if(moduleMap.containsKey(name))
						name = itm.getName();
					
					
					bt = UIHelper.createToggleButton(name,"<html>View <b>"+name+"</b> Manual",
										Config.getIconProperty("icon.toolbar.bookmark"),-1,true,listener);
					bt.setHorizontalAlignment(SwingConstants.LEFT);
					grp.add(bt);
					t.add(bt);
					moduleMap.put(name,itm.getManual());
				}
			}
			t.add(new JLabel(""));
			
			JPanel side = new JPanel();
			side.setBorder(new BevelBorder(BevelBorder.RAISED));
			side.setLayout(new BorderLayout());
			side.add(t,BorderLayout.NORTH);
			side.add(Box.createVerticalGlue(),BorderLayout.CENTER);
			
			JLabel l = new JLabel(Config.getIconProperty("icon.general.tutor.logo"));
			l.setBorder(new EmptyBorder(new Insets(20,0,20,0)));
			
			JPanel side2 = new JPanel();
			side2.setLayout(new BorderLayout());
			side2.add(l,BorderLayout.NORTH);
			side2.add(side,BorderLayout.CENTER);
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(side2,BorderLayout.WEST);
			p.add(new JScrollPane(panel),BorderLayout.CENTER);
			
			// select the first page
			if(grp.getButtonCount() > 0){
				grp.getElements().nextElement().doClick();
			}
			
			helpComponent = p;
		}
		return helpComponent;
	}
	
	/**
	 * enable debuging interface
	 */
	private void doDebug(AbstractButton  enableDebug){
		if(enableDebug.isSelected()){
			JPasswordField debugPassword = new JPasswordField();
			(new FocusTimer(debugPassword)).start();
			int r = JOptionPane.showConfirmDialog(frame,debugPassword,"Enter Debug Password",JOptionPane.OK_CANCEL_OPTION);
			// if ok and authenticated
			String pswd = new String(debugPassword.getPassword());
			if(r == JOptionPane.OK_OPTION && pswd.equals(Constants.DEBUG_PASSWORD)){
				debugMenu.setText("Debug");
				debugMenu.setEnabled(true);
				// enable open case
				openCase.setEnabled(true);
			}else{
				enableDebug.setSelected(false);
			}
		}else{
			debugMenu.setText("");
			debugMenu.setEnabled(false);
			// enable open case
			openCase.setEnabled(Config.getBooleanProperty("tutor.open.case.enabled"));
		}
	}
	
	
	/**
	 * get progress bar
	 * @return
	 */
	public JProgressBar getProgressBar(){
		return progress;
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		if(frame == null)
			return;
		JComponent c = (JComponent)frame.getContentPane();
		if(busy){
			c.remove(statusPanel);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			progress.setIndeterminate(true);
			progress.setString(null);
			c.remove(progress);
			c.add(statusPanel,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * reset everything
	 */
	public void reset(){
		for(Tutor t: getTutors())
			t.reset();
	}
	

	/**
	 * show next button 
	 * @param b
	 */
	private void showNextButton(boolean visible){
		JFrame frame = (JFrame) Config.getMainFrame();
		
		// consult button
		if(nextPanel == null){
			JButton next = new JButton("Next Case");
			next.setBorder(new BevelBorder(BevelBorder.RAISED));
			next.setIcon(UIHelper.getIcon(Config.getProperty("icon.general.next")));
			next.setVerticalTextPosition(SwingConstants.BOTTOM);
			next.setHorizontalTextPosition(SwingConstants.CENTER);
			next.addActionListener(this);
			next.setActionCommand("next");
			next.setFont(next.getFont().deriveFont(Font.BOLD,16f));
			next.setPreferredSize(new Dimension(200,200));
			
			// consult panel
			nextPanel = new JPanel();
			nextPanel.setLayout(new GridBagLayout());
			nextPanel.setOpaque(false);
			//consultPanel.setPreferredSize(userReport.getPreferredSize());
			nextPanel.add(next,new GridBagConstraints());
		}
		
		// make sure that this is the same panel
		if(visible && !frame.getGlassPane().equals(nextPanel)){
			// set glass panel
			frame.setGlassPane(nextPanel);
		}
		
		// show glass panel
		frame.getGlassPane().setVisible(visible);
		
		// remove glass pane
		if(!visible){
			JPanel p = new JPanel();
			p.setOpaque(false);
			frame.setGlassPane(p);
		}
	}
	
	
	/**
	 * listen to property change
	 */
	public void propertyChange(PropertyChangeEvent e) {
		String cmd = e.getPropertyName();
		if(Constants.PROPERTY_CASE_DONE.equals(cmd)){
			String t  = ""+e.getNewValue();
			// if last tutor, then enable next
			if(tutors != null && tutors.getKeys().indexOf(t) == tutors.size()-1){
				showNextButton(true);
			}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		standAlone = true;
		String config = null;
		if(args.length > 0){
			config = args[0];
		}else {
			ConfigSelectorPanel cp = new ConfigSelectorPanel();
			cp.showChooserDialog();
			Object o = cp.getSelectedObject();
			if(o == null){
				System.err.println("ERROR: Configuration file was not selected");
				return;
			}
			config = ""+o;
		}
		// init tutor
		final ITS its = ITS.getInstance();
		its.initialize(config);
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				its.start();
			}
		});
		
		// try to exit gracefully during crashes (close DB connections and such)
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				its.dispose();
			}
		});
	}
}
