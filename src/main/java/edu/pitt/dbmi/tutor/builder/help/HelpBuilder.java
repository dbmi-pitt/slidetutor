package edu.pitt.dbmi.tutor.builder.help;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.builder.protocol.PlaybackPanel;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.model.ReasoningModule;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.GenericTutor;
import edu.pitt.dbmi.tutor.ui.ConfigSelectorPanel;
import edu.pitt.dbmi.tutor.ui.FontPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


/*
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
*/


/**
 * this class is used for managing and building help files
 * @author Administrator
 *
 */
public class HelpBuilder implements ActionListener, ListSelectionListener, ItemListener, PropertyChangeListener{
	private final String DEFAULT_CONFIG = "http://slidetutor.upmc.edu/curriculum/config/PITT/Testing/SlideTutor.conf";

	
	private JList scenarioList,errorList,hintList,actionList,tutorActionList;
	private JTextField scenarioName,scenarioPriority,scenarioCode;
	private JComboBox scenarioType,receivers,actions,input;
	private Map<String,Action> actionMap;
	private JLabel description;
	private JTextArea scenarioDescription;
	private JMenuBar menubar;
	private Component component;
	private JToolBar toolbar;
	private ScenarioSet scenarioSet;
	private ScenarioEntry scenarioEntry;
	private MessageEntry messageEntry;
	private File file;
	private Set<String> defaultActionReceivers;
	private Map<String,Set<String>> defaultActions, defaultActionInputs;
	private JFrame frame;
	private PlaybackPanel playbackPanel;
	private static boolean standAlone;
	private Properties configuration;
	private ITS its;
	
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
			scenarioType = new JComboBox(Constants.SCENARIO_TYPES);
			scenarioPriority = new JTextField();
			scenarioPriority.setHorizontalAlignment(JTextField.CENTER);
			scenarioPriority.setFont(scenarioPriority.getFont().deriveFont(Font.BOLD));
			scenarioPriority.setDocument(new UIHelper.IntegerDocument());
			scenarioCode = new JTextField();
			scenarioCode.setHorizontalAlignment(JTextField.CENTER);
			scenarioCode.setFont(scenarioPriority.getFont().deriveFont(Font.BOLD));
			scenarioCode.setDocument(new UIHelper.IntegerDocument());
			scenarioDescription = new JTextArea();
			scenarioDescription.setLineWrap(true);
			scenarioDescription.setWrapStyleWord(true);
			scenarioDescription.setBackground(new Color(255,255,200));
			
			
			JPanel pa = new JPanel();
			pa.setLayout(new BorderLayout());
			pa.add(new JLabel("Name"),BorderLayout.NORTH);
			pa.add(scenarioName,BorderLayout.CENTER);
			
			JPanel pc = new JPanel();
			pc.setLayout(new BorderLayout());
			pc.add(new JLabel("Type"),BorderLayout.NORTH);
			pc.add(scenarioType,BorderLayout.CENTER);
			
			JPanel pb = new JPanel();
			pb.setLayout(new BorderLayout());
			pb.add(new JLabel("Description"),BorderLayout.NORTH);
			pb.add(new JScrollPane(scenarioDescription),BorderLayout.CENTER);
			
			JPanel pd = new JPanel();
			pd.setLayout(new BorderLayout());
			pd.add(new JLabel("Priority"),BorderLayout.NORTH);
			pd.add(scenarioPriority,BorderLayout.CENTER);
			
			JPanel pe = new JPanel();
			pe.setLayout(new BorderLayout());
			pe.add(new JLabel("Code"),BorderLayout.NORTH);
			pe.add(scenarioCode,BorderLayout.CENTER);
			
			JPanel px = new JPanel();
			px.setLayout(new GridLayout(0,3));
			px.add(pc);
			px.add(pe);
			px.add(pd);
			
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(pa,BorderLayout.NORTH);
			p.add(pb,BorderLayout.CENTER);
			p.add(px,BorderLayout.SOUTH);
			
			
			/*
			JButton bt = new JButton("Apply",UIHelper.getIcon(LOGO));
			bt.addActionListener(this);
			bt.setActionCommand("apply");
			bt.setVerticalTextPosition(SwingConstants.BOTTOM);
			*/
			
			tutorActionList = new JList();
			tutorActionList.setVisibleRowCount(3);
			JPanel pta = new JPanel();
			pta.setLayout(new BorderLayout());
			pta.add(createToolBar("Actions","tutor-action"),BorderLayout.NORTH);
			pta.add(new JScrollPane(tutorActionList),BorderLayout.CENTER);
			
			JPanel pp = new JPanel();
			pp.setLayout(new BorderLayout());
			pp.add(getPlaybackPanel().getComponent(),BorderLayout.NORTH);
			pp.add(pta,BorderLayout.CENTER);
						
			JSplitPane titleSplit = new JSplitPane();
			titleSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			titleSplit.setResizeWeight(1);
			titleSplit.setLeftComponent(p);
			titleSplit.setRightComponent(pp);
			
			errorList = new JList();
			errorList.setDragEnabled(true);
			errorList.setCellRenderer(new MessageRenderer());
			errorList.addMouseListener(new MessageEditListener(errorList));
			new DropTarget(errorList,new DropTargetAdapter() {
				public void drop(DropTargetDropEvent dtde) {
					Object obj = errorList.getSelectedValue();
					if(obj != null && obj instanceof MessageEntry){
						int i = errorList.locationToIndex(dtde.getLocation());
						if(i > -1 ){
							if(scenarioEntry != null){
								scenarioEntry.getErrorMessages().remove(obj);
								scenarioEntry.getErrorMessages().add(i,(MessageEntry)obj);
								
							}
							sync(errorList);
						}	
					}
				}
			});
			
			JPanel p2 = new JPanel();
			p2.setLayout(new BorderLayout());
			p2.add(createToolBar("Error Messages","error"),BorderLayout.NORTH);
			p2.add(new JScrollPane(errorList),BorderLayout.CENTER);
			
			hintList = new JList();
			hintList.setDragEnabled(true);
			hintList.setCellRenderer(new MessageRenderer());
			hintList.addMouseListener(new MessageEditListener(hintList));
			new DropTarget(hintList,new DropTargetAdapter() {
				public void drop(DropTargetDropEvent dtde) {
					Object obj = hintList.getSelectedValue();
					if(obj != null && obj instanceof MessageEntry){
						int i = hintList.locationToIndex(dtde.getLocation());
						if(i > -1 ){
							if(scenarioEntry != null){
								scenarioEntry.getHintMessages().remove(obj);
								scenarioEntry.getHintMessages().add(i,(MessageEntry)obj);
								
							}
							sync(hintList);
						}	
					}
				}
			});
			
			JPanel p3 = new JPanel();
			p3.setLayout(new BorderLayout());
			p3.add(createToolBar("Hint Messages","hint"),BorderLayout.NORTH);
			p3.add(new JScrollPane(hintList),BorderLayout.CENTER);
			
			JSplitPane messageSplit = new JSplitPane();
			messageSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			messageSplit.setResizeWeight(.5);
			messageSplit.setLeftComponent(p2);
			messageSplit.setRightComponent(p3);
			
			
			JSplitPane rightSplit = new JSplitPane();
			rightSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
			rightSplit.setTopComponent(titleSplit);
			rightSplit.setBottomComponent(messageSplit);
			//rightSplit.setResizeWeight(0.2);
			
			// main split
			JSplitPane split = new JSplitPane();
			split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			split.setLeftComponent(p0);
			split.setRightComponent(rightSplit);
			
			
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
	
	
	/**
	 * get playback panel
	 * @return
	 */
	public PlaybackPanel getPlaybackPanel(){
		if(playbackPanel == null){
			playbackPanel = new PlaybackPanel();
			playbackPanel.setPlaybackVisible(true);
			playbackPanel.setRecordVisible(false);
			playbackPanel.setSpeedVisible(false);
			playbackPanel.addPropertyChangeListener(this);
		}
		return playbackPanel;
	}
	
	
	private void doPlay() {
		// don't do anything
		if(getConfiguration() == null)
			return;
		
		// get new instance
		its = ITS.getInstance();
		
		// initialize tutor for the first time
		if (Config.getMainFrame() == null || !Config.getMainFrame().isDisplayable()) {
			its.initialize(getConfiguration());
		}
		// reset plaback panel
		getPlaybackPanel().reset();
			
		// disable saving in protocol
		its.getProtocolModule().setEnabled(false);
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
		return scenarioEntry.getStepSequence();
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
		return null;
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
	
	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(String title, String action){
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(Color.white);
		toolbar.add(UIHelper.createButton("add-"+action,"Add to "+title, 
		UIHelper.getIcon(Config.getProperty("icon.toolbar.add"),16),this));
		toolbar.add(UIHelper.createButton("remove-"+action,"Remove from "+title,
		UIHelper.getIcon(Config.getProperty("icon.toolbar.rem"),16),this));
		toolbar.addSeparator();
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
			toolbar.add(UIHelper.createButton("new","Create New Help System",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.new"),24),this));
			toolbar.add(UIHelper.createButton("open","Open Help System",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.open"),24),this));
			toolbar.add(UIHelper.createButton("save","Save Help System",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.save"),24),this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("add-default","Add Default Scenarios",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.add.default"),24),this));
			toolbar.addSeparator();
			
			toolbar.add(UIHelper.createButton("import","Import Help System",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.import"),24),this));
			toolbar.add(UIHelper.createButton("export","Export Help States",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.export"),24),this));
			
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
			file.add(UIHelper.createMenuItem("Import","Import",UIHelper.getIcon(Config.getProperty("icon.menu.import"),16),this));
			file.add(UIHelper.createMenuItem("Export","Export",UIHelper.getIcon(Config.getProperty("icon.menu.export"),16),this));
			
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
		
		// add default scenarios when appropriate
		//loadDefaultScenarios();
	}
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void loadScenarioEntry(ScenarioEntry e){
		scenarioEntry = e;
		scenarioName.setText(e.getName());
		scenarioDescription.setText(e.getDescription());
		scenarioType.setSelectedItem(e.getType());
		scenarioPriority.setText(""+e.getPriority());
		scenarioCode.setText(""+e.getErrorCode());
		hintList.setModel(new UIHelper.ListModel(e.getHintMessages()));
		errorList.setModel(new UIHelper.ListModel(e.getErrorMessages()));
		tutorActionList.setModel(new UIHelper.ListModel(e.getActions()));
		getPlaybackPanel().setEnabled(!e.getStepSequence().isEmpty());
	}
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void clearScenarioEntry(){
		scenarioEntry = null;
		scenarioName.setText("");
		scenarioDescription.setText("");
		scenarioType.setSelectedItem(null);
		scenarioPriority.setText("");
		scenarioCode.setText("");
		hintList.setModel(new UIHelper.ListModel(Collections.EMPTY_LIST));
		errorList.setModel(new UIHelper.ListModel(Collections.EMPTY_LIST));
		tutorActionList.setModel(new UIHelper.ListModel(Collections.EMPTY_LIST));
		getPlaybackPanel().setEnabled(false);
	}
	
	/**
	 * load scenario set into this builder
	 * @param set
	 */
	private void saveScenarioEntry(ScenarioEntry e){
		e.setName(scenarioName.getText());
		e.setDescription(scenarioDescription.getText());
		e.setType((String)scenarioType.getSelectedItem());
		e.setPriority(Integer.parseInt(scenarioPriority.getText()));
		e.setErrorCode(Integer.parseInt(scenarioCode.getText()));
		// lists should sync bythemsleves
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
		}else if("add-error".equalsIgnoreCase(cmd)){
			MessageEntry msg = new MessageEntry();
			if(doEdit(msg)){
				if(scenarioEntry != null)
					scenarioEntry.getErrorMessages().add(msg);
				sync(errorList);
			}
		}else if("remove-error".equalsIgnoreCase(cmd)){
			for(Object o: errorList.getSelectedValues()){
				scenarioEntry.getErrorMessages().remove(o);
			}
			sync(errorList);
		}else if("add-hint".equalsIgnoreCase(cmd)){
			MessageEntry msg = new MessageEntry();
			if(doEdit(msg)){
				//msg.setHint(true);
				if(scenarioEntry != null)
					scenarioEntry.getHintMessages().add(msg);
				sync(hintList);
			}
		}else if("remove-hint".equalsIgnoreCase(cmd)){
			for(Object o: hintList.getSelectedValues()){
				scenarioEntry.getHintMessages().remove(o);
			}
			sync(hintList);
		}else if("add-action".equalsIgnoreCase(cmd)){
			Action action = new Action();
			if(doEditAction(action)){
				if(messageEntry != null){
					messageEntry.getActions().add(action);
					sync(actionList);
				}
			}
		}else if("remove-action".equalsIgnoreCase(cmd)){
			if(actionList != null && messageEntry != null){
				for(Object o: actionList.getSelectedValues()){
					messageEntry.getActions().remove(o);
				}
				sync(actionList);
			}
		}else if("add-tutor-action".equalsIgnoreCase(cmd)){
			Action action = new Action();
			if(doEditAction(action)){
				if(scenarioEntry != null){
					scenarioEntry.getActions().add(action);
					sync(tutorActionList);
				}
			}
		}else if("remove-tutor-action".equalsIgnoreCase(cmd)){
			for(Object o: tutorActionList.getSelectedValues()){
				scenarioEntry.getActions().remove(o);
			}
			sync(tutorActionList);
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
		}else if(cmd.equals("font-size")){
			FontPanel.getInstance().showDialog(getComponent());
		}
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
	private void loadDefaultActions(){
		if(defaultActionReceivers == null || defaultActions == null || defaultActionInputs == null){
			defaultActionReceivers = new  TreeSet<String>();
			defaultActions = new HashMap<String, Set<String>>();
			defaultActionInputs = new HashMap<String, Set<String>>();
			actionMap = new HashMap<String, Action>();
			
			for(String mod: Config.getRegisteredModules()){
				try{
					Class cls = Class.forName(mod.trim());
					Object obj = cls.newInstance();
					if(obj instanceof TutorModule){
						TutorModule tm = (TutorModule) obj;
						for(Action act: tm.getSupportedActions()){
							defaultActionReceivers.add(act.getReceiver());
							// add actions
							Set<String> acts = defaultActions.get(act.getReceiver());
							if(acts == null){
								acts = new TreeSet<String>();
								defaultActions.put(act.getReceiver(),acts);
							}
							acts.add(act.getAction());
							// add inputs
							Set<String> ins = defaultActionInputs.get(act.getAction());
							if(ins == null){
								ins = new TreeSet<String>();
								defaultActionInputs.put(act.getAction(),ins);
							}
							ins.add(act.getInput());
							actionMap.put(act.getReceiver()+"."+act.getAction()+"."+act.getInput(),act);
						}
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			
			
			// add actions from tutor
			GenericTutor gt = new GenericTutor();
			for(Action act: gt.getSupportedActions()){
				defaultActionReceivers.add(act.getReceiver());
				// add actions
				Set<String> acts = defaultActions.get(act.getReceiver());
				if(acts == null){
					acts = new TreeSet<String>();
					defaultActions.put(act.getReceiver(),acts);
				}
				acts.add(act.getAction());
				// add inputs
				Set<String> ins = defaultActionInputs.get(act.getAction());
				if(ins == null){
					ins = new TreeSet<String>();
					defaultActionInputs.put(act.getAction(),ins);
				}
				ins.add(act.getInput());
				actionMap.put(act.getReceiver()+"."+act.getAction()+"."+act.getInput(),act);
			}
		}
	}
	
	/**
	 * generate a list of default scenarios
	 */
	private void loadDefaultScenarios(){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		panel.setBorder(new TitledBorder("Select Default Help Scenario"));
		ButtonGroup grp = new ButtonGroup();
		JRadioButton b1 = new JRadioButton("Diagnostic Tutor Help",true);
		b1.setActionCommand("/resources/TutorHelp.xml");
		JRadioButton b2 = new JRadioButton("Report Tutor Help");
		b2.setActionCommand("/resources/ReportHelp.xml");
		JRadioButton b3 = new JRadioButton("Radiology Tutor Help");
		b3.setActionCommand("/resources/RadiologyHelp.xml");
		grp.add(b1);
		grp.add(b2);
		grp.add(b3);
		panel.add(b1);
		panel.add(b2);
		panel.add(b3);
		
		int r = JOptionPane.showConfirmDialog(getComponent(),panel,"Select Default Help Scenario",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			doNew();
			try{
				load(getClass().getResource(grp.getSelection().getActionCommand()).openStream());
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
		/*
		// check configuration for a given reasoner
		Properties p = getConfiguration();
		if(p != null){
			for(Object o: p.keySet()){
				String key = (String) o;
				if(key.endsWith(".reasoning.module")){
					String val = p.getProperty(key);
					Class c;
					try {
						c = Class.forName(val);
							ReasoningModule r = (ReasoningModule) c.newInstance();
						if(r.getSupportedScenarioSet() != null){
							for(ScenarioEntry e : r.getSupportedScenarioSet().getScenarioEntries()){
								scenarioSet.addScenarioEntry(e);
							}
							sync(scenarioList);	
						}
					} catch (Exception e1) {
						Config.getLogger().severe(TextHelper.getErrorMessage(e1));
					}
					return;
				}
			}
		}
		
		//default action if other way could not be found
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
		*/
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
			File f = new File(location);
			if(f.exists())
				file = f;
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
				file = f;
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
		
		this.file = file;
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
	
	/**
	 * edit message entry
	 * @param e
	 */
	private boolean doEditAction(Action e){
		loadDefaultActions();
		
		// create panel
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		//panel.setPreferredSize(new Dimension(600,75));
		
		receivers = new JComboBox(defaultActionReceivers.toArray());
		receivers.setBorder(new TitledBorder("Receiver"));
		receivers.setSelectedItem(e.getReceiver());
		receivers.setEditable(true);
		receivers.addItemListener(this);
		panel.add(receivers);
		
		actions = new JComboBox();
		actions.setBorder(new TitledBorder("Action"));
		actions.setSelectedItem(e.getAction());
		actions.setEditable(true);
		actions.addItemListener(this);
		panel.add(actions);
		
		input = new JComboBox();
		input.setBorder(new TitledBorder("Input"));
		input.setSelectedItem(e.getInput());
		input.setEditable(true);
		input.addItemListener(this);
		panel.add(input);
		

		description = new JLabel(Names.getDescription("&nbsp;<br>&nbsp;<br>&nbsp;<br>&nbsp;",500,350));
		description.setHorizontalAlignment(JLabel.CENTER);
			
		JPanel p = new JPanel();
		//p.setLayout(new GridLayout(-1,1));
		p.setLayout(new BorderLayout());
		p.add(panel,BorderLayout.CENTER);
		p.add(description,BorderLayout.SOUTH);
		
		int r = JOptionPane.showConfirmDialog(getComponent(),p,"Edit Action",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			e.setReceiver(""+receivers.getSelectedItem());
			e.setAction(""+actions.getSelectedItem());
			e.setInput(""+input.getSelectedItem());
			return true;
		}
		return false;
	}
	
	/**
	 * edit message entry
	 * @param e
	 */
	private boolean doEdit(MessageEntry e){
		messageEntry = e;
		
		// create edit panel
		JTextArea text = new JTextArea(8,40);
		text.setLineWrap(true);
		text.setText(e.getText());
		
		actionList = new JList();
		actionList.setModel(new UIHelper.ListModel(e.getActions()));
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(createToolBar("Actions","action"),BorderLayout.NORTH);
		p.add(new JScrollPane(actionList),BorderLayout.CENTER);
	
		
		JLabel lbl = new JLabel("Message Text");
		lbl.setBackground(Color.white);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(lbl,BorderLayout.NORTH);
		panel.add(new JScrollPane(text),BorderLayout.CENTER);
		panel.add(p,BorderLayout.SOUTH);
		
		
		boolean ret = false;
		int r = JOptionPane.showConfirmDialog(getComponent(),panel,"Edit",
		JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			e.setText(text.getText());
			ret = true;
		}
		messageEntry = null;
		actionList = null;
		return ret;
	}
	
	
	/**
	 * listen for edit events
	 * @author Eugene Tseytlin
	 */
	private class MessageEditListener extends MouseAdapter {
		private JList list;
		public MessageEditListener(JList list){
			this.list = list;
		}
		public void mouseClicked(MouseEvent e){
			if(e.getClickCount() > 1){
				int i = list.locationToIndex(e.getPoint());
				List messages = ((UIHelper.ListModel)list.getModel()).getList();
				if(i > -1 && i < messages.size()){
					doEdit((MessageEntry)messages.get(i));
					sync(list);
				}
			}
		}
	}
	
	/**
	 * class for rendering messages
	 * @author Eugene Tseytlin
	 *
	 */
	private class MessageRenderer extends UIHelper.HTMLPanel 
					implements ListCellRenderer {
		private Color selectedColor;
		public MessageRenderer(){
			selectedColor = new Color(255,255,200);
			setPreferredSize(new Dimension(200,100));
			setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5),new LineBorder(Color.GRAY)));
		}
		/**
		 * get render component
		 */
		public Component getListCellRendererComponent(JList l, Object o,
				int s, boolean selected, boolean focus) {
			if(o instanceof MessageEntry){
				MessageEntry e = (MessageEntry) o;
				setText(e.getText()); //.replaceAll("(\\[\\w+\\])","<b>$1</b>"));
			}else{
				setText(o.toString());
			}
			setBackground((selected)?selectedColor:Color.white);
			return this;
		}
		
	}
	
	
	/**
	 * load help from legacy protege format
	 * @param proj
	 *
	private ScenarioSet loadHelp(Project helpProj){
		// read help
		ScenarioSet data = new ScenarioSet();
		
		// get knowledge base
		KnowledgeBase kb = helpProj.getKnowledgeBase();
		Set<String> tags = new HashSet<String>();
			
		// get all node_responses
		for(Iterator i=kb.getCls("NODE_RESPONSE").getDirectInstances().iterator();i.hasNext();){
			Instance nodeInst = (Instance) i.next();
			
			// get attributes
			int code = ((Integer) nodeInst.getOwnSlotValue(kb.getSlot("code"))).intValue();
			
			int priority = (kb.getSlot("priority") != null)?
					((Integer) nodeInst.getOwnSlotValue(kb.getSlot("priority"))).intValue():0;
			String type = (String) nodeInst.getOwnSlotValue(kb.getSlot("type"));
			String description = (String) nodeInst.getOwnSlotValue(kb.getSlot("description"));
			Collection hintInst = nodeInst.getOwnSlotValues(kb.getSlot("hint_message"));
			Collection bugInst = nodeInst.getOwnSlotValues(kb.getSlot("bug_message"));
			Collection actionInst = nodeInst.getOwnSlotValues(kb.getSlot("tutor_action"));
		
			
			// get messages
			List[] msgs = new List[] 
			 {new ArrayList<MessageEntry>(),new ArrayList<MessageEntry>()};
			Collection [] instances = new Collection [] {hintInst,bugInst};
			for(int j=0;j<instances.length;j++){
				Collection instList = instances[j];
				for(Object inst : instList){
					Instance msgInst = (Instance) inst;
					// get text
					String text = (String) msgInst.getOwnSlotValue(kb.getSlot("text"));
					
					// get old tags
					
					//List<String> oldTags = TextHelper.getTextTags(text);
					//List<String> newTags = new ArrayList<String>();
					//for(String t: oldTags){
					//	String n = t.replaceAll("<","\\[").replaceAll(">","\\]");
					//	text = text.replaceAll(t,n);
					//	newTags.add(n);
					//}
					
										
					// replace with new tags
					tags.addAll(TextHelper.getTextTags(text));
					
					Collection pointerInst = msgInst.getOwnSlotValues(kb.getSlot("pointer"));
					ArrayList pointers = null;
					
					if(pointerInst != null){
						pointers = new ArrayList();	
						for(Iterator k=pointerInst.iterator();k.hasNext();){
							Instance pointInst = (Instance) k.next();	
							String action = (String) pointInst.getOwnSlotValue(kb.getSlot("pointer_action"));
							String object = (String) pointInst.getOwnSlotValue(kb.getSlot("pointer_object_identifier"));
							
							// get params
							Properties props = null;
							Collection  names = pointInst.getOwnSlotValues(kb.getSlot("property_name"));
							Collection  values = pointInst.getOwnSlotValues(kb.getSlot("property_value"));
							if(names != null && values != null && names.size() > 0){
								props = new Properties();
								for(Iterator n=names.iterator(),v=values.iterator();n.hasNext();){
									props.put(n.next(),v.next());	
								}
							}
							
							// create Pointer
							pointers.add(new Action(object,action,""+props));	
						}
					}	
					
					MessageEntry msg = new MessageEntry();
					msg.setText(text);
					msg.setActions(pointers);
					msgs[j].add(msg);
				}
			}
			
			// get pointert
			ArrayList actions = new ArrayList();	
			if(actionInst != null){
				for(Iterator k=actionInst.iterator();k.hasNext();){
					Instance pointInst = (Instance) k.next();	
					String action = (String) pointInst.getOwnSlotValue(kb.getSlot("pointer_action"));
					String object = (String) pointInst.getOwnSlotValue(kb.getSlot("pointer_object_identifier"));
					
					// get params
					Properties props = null;
					Collection  names = pointInst.getOwnSlotValues(kb.getSlot("property_name"));
					Collection  values = pointInst.getOwnSlotValues(kb.getSlot("property_value"));
					if(names != null && values != null && names.size() > 0){
						props = new Properties();
						for(Iterator n=names.iterator(),v=values.iterator();n.hasNext();){
							props.put(n.next(),v.next());	
						}
					}
					
					// create Pointer
					actions.add(new Action(object,action,""+props));	
				}
			}
			
			// create node response
			ScenarioEntry node = new ScenarioEntry();
			node.setName(type+"-"+code);
			node.setPriority(priority);
			node.setType(type);
			node.setDescription(description);
			node.setHintMessages(msgs[0]);
			node.setErrorMessages(msgs[1]);
			node.setActions(actions);
			data.addScenarioEntry(node);
		}
		return data;
	}
	*/
	
	public void itemStateChanged(ItemEvent e) {
		if(e.getStateChange() == ItemEvent.SELECTED){
			input.setEnabled(true);
			if(e.getSource() == receivers){
				actions.removeAllItems();
				input.removeAllItems();
				String r = ""+receivers.getSelectedItem();
				if(defaultActions.containsKey(r)){
					for(String a: defaultActions.get(r)){
						actions.addItem(a);
					}
				}
			}else if(e.getSource() == actions){
				input.removeAllItems();
				String r = ""+actions.getSelectedItem();
				if(defaultActionInputs.containsKey(r)){
					for(String a: defaultActionInputs.get(r)){
						input.addItem(a);
					}
				}
			}
			
			// display help				
			if(actionMap != null){
				Action a = actionMap.get(receivers.getSelectedItem()+"."+actions.getSelectedItem()+"."+input.getSelectedItem());
				if(a != null){
					description.setText(Names.getDescription(a.getDescription(),500,300));
				}
			}
			
			// disable the interface
			if(input.getModel().getSize() == 0 || input.getModel().getElementAt(0).toString().length() == 0)
				input.setEnabled(false);
			
			// resize
			Window w = UIHelper.getWindow(actions);
			if(w != null)
				w.pack();
		}
		
	}
	public void propertyChange(PropertyChangeEvent evt) {
		if(PlaybackPanel.PLAY_SELECTED.equals(evt.getPropertyName())){
			doPlay();
		}
	}
	
	
	public void showFrame(){
		frame = new JFrame("Help Builder");
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		standAlone = true;
		HelpBuilder hb = new HelpBuilder();
		hb.showFrame();
		
		// load file
		if(args.length > 0){
			File f = new File(args[0]);
			if(f.exists())
				hb.load(f);
		}
		/*
		else{
			hb.load(HelpBuilder.class.getResourceAsStream(OntologyHelper.DEFAULT_TUTOR_HELP_FILE));
		}
		*/
		
	}
}
