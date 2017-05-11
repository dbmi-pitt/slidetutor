package edu.pitt.dbmi.tutor.builder.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.ScenarioEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.dbmi.tutor.builder.protocol.PlaybackPanel;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.pedagogic.UserCaseSequence;
import edu.pitt.dbmi.tutor.ui.ConfigSelectorPanel;
import edu.pitt.dbmi.tutor.ui.FontPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;


public class TutorTester implements ActionListener, ListSelectionListener, ItemListener, PropertyChangeListener{
	private String DEFAULT_CONFIG = "http://slidetutor.upmc.edu/curriculum/config/PITT/Testing/SlideTutor.conf?ArcNodeInterface.behavior.attribute.mode=all";
	
	private JList scenarioList;
	private JTextField scenarioName;
	private JTextArea scenarioDescription;
	private JList console;
	private JMenuBar menubar;
	private Component component;
	private JToolBar toolbar;
	private ScenarioSet scenarioSet;
	private Properties configuration;
	private ScenarioEntry scenarioEntry;
	private File file;
	private JFrame frame;
	private PlaybackPanel playbackPanel;
	private static boolean standAlone;
	private UserCaseSequence caseSequence;
	private ITS its;
	private JPopupMenu popup;
	private List<Message> clipboard;
	
	private ProtocolModule recorder = new ProtocolModule() {
		private Message start,view;
		private boolean enabled;
		private long time;
		private final int DELTA = 200;
		
		
		public void resolveAction(Action action) {}
		public void reset() {}
		public void receiveMessage(Message msg) {
			// if enabled
			if(enabled){
				// add very first problem event
				if(getStepSequence().isEmpty() && start != null){
					// start from the time of recording
					time = msg.getTimestamp();
					start.setTimestamp(time);
					getStepSequence().add(start);
					sync(console);
				}
				// record all other client events
				if(msg instanceof ClientEvent && !msg.isAuto()){
					// remember viewer
					if(ACTION_VIEW_CHANGE.equals(msg.getAction())){
						view = msg;
					}else{
						// add last view if this is finding
						if(Arrays.asList(TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ACTION).contains(msg.getType()) && view != null){
							view.setTimestamp(time += DELTA);
							getStepSequence().add(view);
						}
						msg.setTimestamp(time += DELTA);
						getStepSequence().add(msg);
						sync(console);
					}
				}
			}
		}
		public void load() {}
		public String getVersion() {
			return "1.0";
		}
		public Message[] getSupportedMessages() {
			return new Message [0];
		}
		public Action[] getSupportedActions() {
			return new Action [0];
		}
		public String getName() {
			return "Record Actions";
		}
		public String getDescription() {
			return "";
		}
		public Properties getDefaultConfiguration() {
			return null;
		}
		public void dispose() {}
		public void setEnabled(boolean b) {
			enabled = b;
		}
		public boolean removeUser(String username) {
			return false;
		}
		public boolean removeSessions(List<Session> sessions) {
			return false;
		}
		public boolean removeExperiment(String experiment) {
			return false;
		}
		public boolean removeCondition(String condition, String experiment) {
			return false;
		}
		public void processMessage(Message msg) {}
		public void openCaseSession(ProblemEvent start) {
			this.start = start;
		}
		public boolean isEnabled() {
			return enabled;
		}
		public boolean isConnected() {
			return false;
		}
		public List<String> getUsers(String experiment) {
			return Collections.EMPTY_LIST;
		}
		public Properties getUserInfo(String username) {
			return null;
		}
		public List<Session> getSessions(Query query) {
			return Collections.EMPTY_LIST;
		}
		public List<String> getExperiments() {
			return Collections.EMPTY_LIST;
		}
		public List<String> getConditions(String experiment) {
			return Collections.EMPTY_LIST;
		}
		public List<String> getCases(Query query) {
			return Collections.EMPTY_LIST;
		}
		public void closeCaseSession(ProblemEvent end) {
			if(!getStepSequence().isEmpty() && !(getStepSequence().get(getStepSequence().size()-1) instanceof ProblemEvent)){
				end.setTimestamp(time += DELTA);
				getStepSequence().add(end);
				sync(console);
			}
		}
		public boolean authenticateUser(String username, String password, String study) {
			return false;
		}
		public boolean authenticateUser(String username, String password) {
			return false;
		}
		public boolean authenticateAdministrator(String username, String password) {
			return false;
		}
		public void addUser(String username, String password, String experiment, Properties p) {}
		public void addExperiment(String experiment) {}
		public void addCondition(String condition, String experiment) {}
	};
	
	/**
	 * get UI component of the help builder
	 * @return
	 */
	public Component getComponent(){
		if(component == null){
			// scenario list
			scenarioList = new JList();
			scenarioList.addListSelectionListener(this);
			JPanel p0 = new JPanel();
			p0.setLayout(new BorderLayout());
			p0.add(createToolBar("Feedback Scenarios","scenario"),BorderLayout.NORTH);
			p0.add(new JScrollPane(scenarioList),BorderLayout.CENTER);
			
			scenarioName = new JTextField();
			scenarioName.setEditable(false);
			scenarioDescription = new JTextArea();
			scenarioDescription.setLineWrap(true);
			scenarioDescription.setWrapStyleWord(true);
			scenarioDescription.setBackground(new Color(255,255,200));
			
			
			JPanel pa = new JPanel();
			pa.setLayout(new BorderLayout());
			pa.add(new JLabel("Name"),BorderLayout.NORTH);
			pa.add(scenarioName,BorderLayout.CENTER);
			
			JPanel pb = new JPanel();
			pb.setLayout(new BorderLayout());
			pb.add(new JLabel("Description"),BorderLayout.NORTH);
			pb.add(new JScrollPane(scenarioDescription),BorderLayout.CENTER);
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(pa,BorderLayout.NORTH);
			p.add(pb,BorderLayout.CENTER);
			
			JPanel pta = new JPanel();
			pta.setLayout(new BorderLayout());
			pta.add(new JLabel(Config.getIconProperty("icon.general.tutor.logo")),BorderLayout.CENTER);
			pta.add(getPlaybackPanel().getComponent(),BorderLayout.SOUTH);
			getPlaybackPanel().setEnabled(false);
			
			JSplitPane titleSplit = new JSplitPane();
			titleSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			titleSplit.setResizeWeight(0.5);
			titleSplit.setDividerLocation(400);
			titleSplit.setLeftComponent(p);
			titleSplit.setRightComponent(pta);
	
			console = new JList();
			console.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if(value instanceof ProblemEvent){
						ProblemEvent m = (ProblemEvent) value;
						String l = (TYPE_START.equals(m.getType()))?OntologyHelper.getCaseName(m.getCaseURL()):m.getOutcome();
						lbl.setText("<html><table><tr><td width=110><font color=\"blue\">"+m.getType().toLowerCase()+
									"</font></td><td><font color=\"green\">"+l+"</font></td></tr></table>");
					}else if(value instanceof Message){
						Message m = (Message) value;
						String p = "";
						if(!TextHelper.isEmpty(m.getParent())){
							String[] s = m.getParent().split("\\.");
							if (s.length == 3)
								p = " to <font color=green>"+s[1]+"</font>";
						}
						lbl.setText("<html><table><tr><td width=110><font color=\"blue\">"+m.getAction().toLowerCase()+"</font></td><td width=110>"+
								m.getType()+"</td><td><font color=\"green\">"+m.getLabel()+"</font></td><td>"+p+"</td></tr></table>");
					}
					return lbl;
				}
				
			});
			console.addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent e){
					if(e.getClickCount() == 2){
						int i = UIHelper.getIndexForLocation(console,e.getPoint());
						if(i > -1){
							doEdit(getStepSequence().get(i));
						}
					} 
				}
				public void mousePressed(MouseEvent e){
					if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
						int x = UIHelper.getIndexForLocation(console,e.getPoint());
						// set selection 
						if(x > -1){
							// if not within selection, then reset selection
							if(!isWithinSelection(x))
								console.setSelectedIndex(x);
						}
						getPopupMenu(x).show(console,e.getX(),e.getY());
						
					}
				}
				private boolean isWithinSelection(int x){
					for(int i:console.getSelectedIndices()){
						if(i == x)
							return true;
					}
					return false;
				}
			});
			JScrollPane cs = new JScrollPane(console);
			cs.setBorder(new TitledBorder("Step Sequence"));
			
			JSplitPane rightSplit = new JSplitPane();
			rightSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
			rightSplit.setTopComponent(titleSplit);
			rightSplit.setBottomComponent(cs);
			rightSplit.setResizeWeight(0);
			
			// main split
			JSplitPane split = new JSplitPane();
			split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			split.setLeftComponent(p0);
			split.setRightComponent(rightSplit);
			split.setDividerLocation(430);			
			
			JPanel main = new JPanel();
			main.setLayout(new BorderLayout());
			main.add(getToolBar(),BorderLayout.NORTH);
			main.add(split,BorderLayout.CENTER);
			component = main;
			
			// set size
			component.setPreferredSize(new Dimension(1024,768));
			
			//put in default
			doNew();
		}
		return component;
	}
	
	public PlaybackPanel getPlaybackPanel(){
		if(playbackPanel == null){
			playbackPanel = new PlaybackPanel();
			playbackPanel.setPlaybackVisible(true);
			playbackPanel.setRecordVisible(true);
			playbackPanel.setSpeedVisible(false);
			playbackPanel.setTargetProtocolModule(recorder);
			playbackPanel.addPropertyChangeListener(this);
		}
		return playbackPanel;
	}
	
	
	public JPopupMenu getPopupMenu(int x){
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("copy","Copy",Config.getIconProperty("icon.menu.copy"),this));
			popup.add(UIHelper.createMenuItem("paste","Paste",Config.getIconProperty("icon.menu.paste"),this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("edit","Edit Step",Config.getIconProperty("icon.menu.edit"),this));
			popup.add(UIHelper.createMenuItem("fix","Fix Time Interval",Config.getIconProperty("icon.menu.time"),this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("remove","Remove Step",Config.getIconProperty("icon.menu.rem"),this));
			popup.add(UIHelper.createMenuItem("clear","Clear All Steps",Config.getIconProperty("icon.menu.delete"),this));
		}
		popup.getComponent(0).setEnabled(x > -1);
		popup.getComponent(3).setEnabled(x > -1);
		popup.getComponent(6).setEnabled(x > -1);
		return popup;
	}
	
	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(String title, String action){
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(Color.white);
		//toolbar.add(UIHelper.createButton("add-"+action,"Add to "+title, 
		//UIHelper.getIcon(Config.getProperty("icon.toolbar.add"),16),this));
		//toolbar.add(UIHelper.createButton("remove-"+action,"Remove from "+title,
		//UIHelper.getIcon(Config.getProperty("icon.toolbar.rem"),16),this));
		//toolbar.addSeparator();
		toolbar.add(new JLabel(title));
		return toolbar;
	}
	
	/**
	 * get toolbar for component
	 * @return
	 */
	public JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new JToolBar();
			toolbar.add(UIHelper.createButton("new","Create New Testing File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.new"),24),this));
			toolbar.add(UIHelper.createButton("open","Open Testing File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.open"),24),this));
			toolbar.add(UIHelper.createButton("save","Save Testing File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.save"),24),this));
		}
		return toolbar;
	}
	
	public JMenuBar getMenuBar(){
		if(menubar == null){
			JMenu file = new JMenu("File");
			file.add(UIHelper.createMenuItem("New","New",UIHelper.getIcon(Config.getProperty("icon.menu.new"),16),this));
			file.add(UIHelper.createMenuItem("Open","Open",UIHelper.getIcon(Config.getProperty("icon.menu.open"),16),this));
			file.add(UIHelper.createMenuItem("Save","Save",UIHelper.getIcon(Config.getProperty("icon.menu.save"),16),this));
			file.add(UIHelper.createMenuItem("Save As","Save As",UIHelper.getIcon(Config.getProperty("icon.menu.save.as"),16),this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Preferences","Preferences",UIHelper.getIcon(Config.getProperty("icon.menu.preferences"),16),this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Close","Close",null,this));
			JMenu opts = new JMenu("Options");
			opts.add(UIHelper.createMenuItem("font-size","Change Font Size",UIHelper.getIcon(Config.getProperty("icon.menu.font")),this));
			menubar = new JMenuBar();
			menubar.add(file);
			menubar.add(opts);
		}
		return menubar;
	}
	
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void loadScenarioSet(ScenarioSet set){
		scenarioSet = set;
		final List<ScenarioEntry> list = set.getScenarioEntries();
		Collections.sort(list);
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				scenarioList.setModel(new UIHelper.ListModel(list));
				scenarioList.revalidate();
				clearScenarioEntry();
			}
		});
	}
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void loadScenarioEntry(ScenarioEntry e){
		getPlaybackPanel().setEnabled(true);
		scenarioEntry = e;
		scenarioName.setText(e.getName());
		scenarioDescription.setText(e.getDescription());
		console.setModel(new UIHelper.ListModel(e.getStepSequence()));
		sync(console);
	}
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void clearScenarioEntry(){
		scenarioEntry = null;
		scenarioName.setText("");
		scenarioDescription.setText("");
		console.setListData(new String [0]);
		getPlaybackPanel().setEnabled(false);
	}
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void saveScenarioEntry(ScenarioEntry e){
		e.setName(scenarioName.getText());
		e.setDescription(scenarioDescription.getText());
		// no need to save step sequence, since it is passed directly to a list model
	}
	
	/**
	 * sync list content
	 * @param l
	 */
	private void sync(JList l){
		final JList list = l;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				((UIHelper.ListModel)list.getModel()).sync(list);
			}
		});
	}
	
	/**
	 * create a unique name for scenario
	 * @param name
	 * @return
	 */
	private String createScenarioName(String name){
		String scenarioName = name;
		int count = 1;
		while(scenarioSet.getScenarioMap().containsKey(scenarioName)){
			scenarioName = name + "-"+(count++);
		}
		return scenarioName;
	}
	
	/**
	 * handle actions
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("add-scenario".equalsIgnoreCase(cmd)){
			scenarioSet.addScenarioEntry(new ScenarioEntry(createScenarioName("New Scenario")));
			sync(scenarioList);
		}else if("remove-scenario".equals(cmd)){
			for(Object o: scenarioList.getSelectedValues()){
				scenarioSet.removeScenarioEntry((ScenarioEntry) o);
			}
			sync(scenarioList);
			scenarioList.clearSelection();
			clearScenarioEntry();
		}else if("new".equalsIgnoreCase(cmd)){
			doNew();
		}else if("open".equalsIgnoreCase(cmd)){
			doOpen();
		}else if("save".equalsIgnoreCase(cmd)){
			doSave(file);
		}else if("save as".equalsIgnoreCase(cmd)){
			doSave(null);
		}else if("import".equalsIgnoreCase(cmd)){
			doImport();
		}else if("export".equalsIgnoreCase(cmd)){
			doExport();
		}else if("apply".equalsIgnoreCase(cmd)){
			saveScenarioEntry(scenarioEntry);
		}else if("add-default".equalsIgnoreCase(cmd)){
			loadDefaultScenarios();
		}else if("close".equalsIgnoreCase(cmd)){
			close();
		}else if("edit".equalsIgnoreCase(cmd)){
			doEdit(getStepSequence().get(console.getSelectedIndex()));
		}else if("remove".equalsIgnoreCase(cmd)){
			List torem = new ArrayList();
			for(int x : console.getSelectedIndices()){
				torem.add(getStepSequence().get(x));
			}
			getStepSequence().removeAll(torem);
			sync(console);
		}else if("clear".equalsIgnoreCase(cmd)){
			getStepSequence().clear();
			sync(console);
		}else if("copy".equalsIgnoreCase(cmd)){
			clipboard = new ArrayList();
			for(int x : console.getSelectedIndices()){
				clipboard.add(getStepSequence().get(x));
			}
		}else if("paste".equalsIgnoreCase(cmd)){
			if(clipboard != null){
				getStepSequence().addAll(clipboard);
				sync(console);
			}
		}else if("fix".equalsIgnoreCase(cmd)){
			doFixTimeDelay();
		}else if("preferences".equalsIgnoreCase(cmd)){
			doPreferences();
		}else if(cmd.equals("font-size")){
			FontPanel.getInstance().showDialog(getComponent());
		}
	}
	
	private void doFixTimeDelay(){
		long time = 0;
		for(Message msg: getStepSequence()){
			// init delay
			if(time == 0)
				time = msg.getTimestamp();
			else
				msg.setTimestamp(time);
			time+=200;
		}
	}
	
	private void doPreferences(){
		JPanel panel = new JPanel();
		panel.setBorder(new BevelBorder(BevelBorder.RAISED));
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(new JLabel(" Configuration "),BorderLayout.WEST);
		final JTextField configText = new JTextField(DEFAULT_CONFIG,30);
		configText.setForeground(Color.blue);
		p1.add(configText,BorderLayout.CENTER);
		JButton bt = new JButton("Select");
		bt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ConfigSelectorPanel cp = new ConfigSelectorPanel();
				cp.showChooserDialog();
				Object o = cp.getSelectedObject();
				if(o != null){
					configText.setText(""+o);
				}
			}
		});
		p1.add(bt,BorderLayout.EAST);
		panel.add(p1);
		
		int r = JOptionPane.showConfirmDialog(getComponent(), panel,"Preferences",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(JOptionPane.OK_OPTION == r){
			DEFAULT_CONFIG = configText.getText();
			configuration = null;
		}
	}
	
	private void doEdit(Message message) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * record tutor session
	 * @param b
	 */
	private void doRecord(boolean b) {
		// TODO Auto-generated method stub
		
	}
	
	
	private void doPlay() {
		Properties conf = getConfiguration();
		
		// don't do anything
		if(conf == null)
			return;
		
		// get new instance
		its = ITS.getInstance();
		
		// initialize tutor for the first time
		if (Config.getMainFrame() == null || !Config.getMainFrame().isDisplayable()) {
			its.initialize(conf);
			Communicator.getInstance().addRecipient(recorder);
		}
		// reset plaback panel
		getPlaybackPanel().reset();
			
		// disable saving in protocol
		//its.getProtocolModule().setEnabled(false);
		its.show();
			
		(new Thread(){
			public void run(){
				// open new case (maybe)
				String problem = getCase(); 
				if(problem == null)
					return;
				
				// if not the same case, reload case
				its.setBusy(true);
			    its.openCase(problem);
	    	    its.setBusy(false);
				
				// start playback
    			getPlaybackPanel().play(getStepSequence());
			}
		}).start();
	}
	
	
	/**
	 * get client messages
	 * @return
	 */
	private List<Message> getStepSequence() {
		return ((UIHelper.ListModel)console.getModel()).getList();
	}
	
	/**
	 * get case for a given scenario
	 * @return
	 */
	private String getCase() {
		// if first message is a problem event with a case,
		// then use that, else prompt for a case
		List<Message> messages = getStepSequence();
		if(messages != null && messages.size() > 0 && messages.get(0) instanceof ProblemEvent){
			return ((ProblemEvent)messages.get(0)).getCaseURL();
		}
		if(caseSequence == null){
			caseSequence = new UserCaseSequence();
			caseSequence.setExpertModule(new DomainExpertModule());
		}
		return caseSequence.getNextCase();
	}
	
	/**
	 * get configuration for a tutor
	 * @return
	 */
	public Properties getConfiguration(){
		if(configuration == null){
			String conf = DEFAULT_CONFIG;
			// if default is not set, then pick a configuration
			if(conf == null){
				ConfigSelectorPanel cp = new ConfigSelectorPanel();
				cp.showChooserDialog();
				Object o = cp.getSelectedObject();
				if(o != null){
					conf = ""+o;
					
				}
			}
			// if it was selected then load it
			if(conf != null){
				configuration = new Properties();
				InputStream in =Config.getInputStream(conf);
				try {
					configuration.load(in);
					in.close();
				} catch (IOException e) {
					Config.getLogger().severe(TextHelper.getErrorMessage(e));
				}
				configuration.putAll(OntologyHelper.getURLQuery(conf));
			}
		}
		
		// modify configuration setting based on states
		if(configuration != null){
			ScenarioEntry e = (ScenarioEntry) scenarioList.getSelectedValue();
			if(e != null){
				Pattern pt = Pattern.compile("##(.*)##");
				Matcher mt = pt.matcher(e.getDescription());
				if(mt.find()){
					String [] p = mt.group(1).split("=");
					if(p.length == 2){
						configuration.setProperty(p[0].trim(),p[1].trim());
					}
				}
			}
		}
		
		
		return configuration;
	}
	
	
	public void setConfiguration(Properties props){
		configuration = props;
	}
	
	
	private void close(){
		int r = JOptionPane.showConfirmDialog(getComponent(),"Would you like to save changes?","Save?",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		switch(r){
		case JOptionPane.CANCEL_OPTION: return;
		case JOptionPane.YES_OPTION: doSave(file); break;
		case JOptionPane.NO_OPTION:  break;
		}
		// die
		if(frame != null)
			frame.dispose();
		if(standAlone)
			System.exit(0);
	}
	
	/**
	 * switch between scenarios
	 */
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			ScenarioEntry v = (ScenarioEntry) scenarioList.getSelectedValue();
			// save previous node
			if(scenarioEntry != null)
				saveScenarioEntry(scenarioEntry);
			// load next node
			if(v != null)
				loadScenarioEntry(v);
		}
	}
	
	/**
	 * create new scenario set
	 */
	private void doNew(){
		loadScenarioSet(new ScenarioSet());
	}
	
	
	/**
	 * generate a list of default scenarios
	 */
	private void loadDefaultScenarios(){
		for(Field field: Constants.class.getFields()){
			if( field.getName().startsWith(Constants.SCENARIO_TYPE_ERROR) || 
				field.getName().startsWith(Constants.SCENARIO_TYPE_HINT)){
				// now add new field
				try {
					String value = ""+field.get(Constants.class);
					
					// skip OK
					if(Constants.ERROR_OK.equals(value))
						continue;
					
					// add new scenario
					ScenarioEntry entry = new ScenarioEntry(value);
					//if(field.getName().startsWith(Constants.SCENARIO_TYPE_ERROR))
					//	entry.setType(Constants.SCENARIO_TYPE_ERROR);
					//else if(field.getName().startsWith(Constants.SCENARIO_TYPE_HINT))
					//	entry.setType(Constants.SCENARIO_TYPE_HINT);
						
					//add 
					scenarioSet.addScenarioEntry(entry);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				
			}
		}
		sync(scenarioList);	
	}
	
	
	
	/**
	 * create new scenario set
	 */
	private void doOpen(){
		JFileChooser chooser = new JFileChooser(file);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".xml");
			}
			public String getDescription() {
				return "XML File";
			}
		});
		int r = chooser.showOpenDialog(getComponent());
		if(r == JFileChooser.APPROVE_OPTION){
			file = chooser.getSelectedFile();
			if(file.exists()){
				load(file);
			}else{
				JOptionPane.showMessageDialog(getComponent(),"File "+file.getAbsolutePath()+" does not exist");
			}
		}
	}
	
	/**
	 * load help file
	 * @param f
	 */
	public void load(File f){
		try{
			load(new FileInputStream(f));
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(getComponent(),
					"Could not load help file "+f.getAbsolutePath(),
					"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * load help file
	 * @param f
	 *
	public void load(Project p){
		loadScenarioSet(loadHelp(p));
	}
	*/
	/**
	 * load help file
	 * @param f
	 */
	public void load(URL u){
		try{
			load(u.openStream());
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(getComponent(),
					"Could not load help URL "+u,
					"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * load from location
	 * @param location
	 */
	public void load(String location){
		try{
			load(Config.getInputStream(location));
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
		}
	}
	
	
	/**
	 * load help file
	 * @param f
	 */
	public void load(InputStream in) throws IOException{
		getComponent();
		ScenarioSet s = new ScenarioSet();
		s.load(in);
		loadScenarioSet(s);
	}
	
	/**
	 * save scenario set
	 */
	public void doSave(){
		doSave(file);
	}
	
	/**
	 * save scenario set
	 */
	private void doSave(File file){
		// do save 
		if(scenarioEntry != null)
			saveScenarioEntry(scenarioEntry);
		
		if(file == null){
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileFilter(){
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".xml");
				}
				public String getDescription() {
					return "Help XML File (.xml)";
				}
			});
			chooser.setSelectedFile(new File("TutorHelp.xml"));
			int r = chooser.showSaveDialog(getComponent());
			if(r == JFileChooser.APPROVE_OPTION){
				File f = chooser.getSelectedFile();
				if(!f.getName().endsWith(".xml"))
					f = new File(f.getParentFile(),f.getName()+".xml");
				// file selected
				this.file = f;
			}
		}
		
		// do save
		if(file != null){
			try{
				scenarioSet.save(new FileOutputStream(file));
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(getComponent(),
						"Could not save help file "+file.getAbsolutePath(),
						"Error",JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	
	/**
	 * save scenario set
	 */
	private void doImport(){
		JFileChooser chooser = new JFileChooser(file);
		int r = chooser.showOpenDialog(getComponent());
		if(r == JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			ScenarioSet help = null;
			
			if(file.getName().endsWith(".pprj")){
				/*
				ArrayList errors = new ArrayList();
				Project p = Project.loadProjectFromFile(file.getAbsolutePath(),errors);
				if(!errors.isEmpty()){
					for(Object s: errors){
						System.err.println(s);
					}
				}
				help = loadHelp(p);
				*/
				
			}else if(file.getName().endsWith(".xml")){
				help = new ScenarioSet();
				try{
					help.load(new FileInputStream(file));
				}catch(Exception ex){
					ex.printStackTrace();
				}
				
			}
			// load scenario set
			loadScenarioSet(help);
		}
	}
	
	private void doExport(){
		JFileChooser chooser = new JFileChooser(file);
		chooser.setSelectedFile(new File("HelpScenarios.txt"));
		int r = chooser.showSaveDialog(getComponent());
		if(r == JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			try{
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				List<ScenarioEntry> list = scenarioSet.getScenarioEntries();
				Collections.sort(list);
				for(ScenarioEntry e: list){
					writer.write(e.toString()+" | "+e.getDescription()+"\n");
				}
				writer.close();
				
			}catch(Exception ex){
				JOptionPane.showMessageDialog(getComponent(),"Error exporting error states!");
				ex.printStackTrace();
			}
		}
	}
	
	public File getFile(){
		return file;
	}
	
	public void setFile(File f){
		file = f;
	}
	
	
	public void itemStateChanged(ItemEvent e) {
		
	}
	
	
	public void showFrame(){
		frame = new JFrame("Tutor Tester");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
				super.windowClosing(e);
			}
		});
		frame.getContentPane().add(getComponent());
		frame.setJMenuBar(getMenuBar());
		frame.pack();
		frame.setVisible(true);
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		if(PlaybackPanel.PLAY_SELECTED.equals(evt.getPropertyName())){
			doPlay();
		}else if(PlaybackPanel.RECORD_SELECTED.equals(evt.getPropertyName())){
			doRecord(true);
		}else if(PlaybackPanel.RECORD_UNSELECTED.equals(evt.getPropertyName())){
			doRecord(false);
		}
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		standAlone = true;
		TutorTester hb = new TutorTester();
		hb.showFrame();
		
		// load file
		if(args.length > 0){
			File f = new File(args[0]);
			if(f.exists())
				hb.load(f);
		}else{
			hb.load(TutorTester.class.getResourceAsStream(OntologyHelper.DEFAULT_TUTOR_HELP_FILE));
		}
		

	}


	
}
