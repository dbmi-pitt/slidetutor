package edu.pitt.dbmi.tutor.builder.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.builder.behavior.BehaviorBuilder;
import edu.pitt.dbmi.tutor.builder.config.panel.FinishPanel;
import edu.pitt.dbmi.tutor.builder.config.panel.OtherModulesPanel;
import edu.pitt.dbmi.tutor.builder.config.panel.StartPanel;
import edu.pitt.dbmi.tutor.builder.config.panel.TutorInfoPanel;
import edu.pitt.dbmi.tutor.builder.config.panel.TutorModulePanel;
import edu.pitt.dbmi.tutor.builder.config.panel.TutorUIPanel;
import edu.pitt.dbmi.tutor.builder.help.HelpBuilder;
import edu.pitt.dbmi.tutor.builder.protocol.UserManager;
import edu.pitt.dbmi.tutor.builder.sequence.SequenceBuilder;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.modules.behavior.GenericBehaviorModule;
import edu.pitt.dbmi.tutor.modules.feedback.HelpManager;
import edu.pitt.dbmi.tutor.modules.interfaces.RadiologyInterface;
import edu.pitt.dbmi.tutor.modules.interfaces.ReportInterface;
import edu.pitt.dbmi.tutor.modules.pedagogic.StaticCaseSequence;
import edu.pitt.dbmi.tutor.modules.presentation.DynamicBook;
import edu.pitt.dbmi.tutor.modules.presentation.KnowledgeExplorer;
import edu.pitt.dbmi.tutor.modules.reasoning.RadiologyReasoner;
import edu.pitt.dbmi.tutor.modules.reasoning.SimpleDiagnosticReasoner;
import edu.pitt.dbmi.tutor.modules.reasoning.SimplePrognosticReasoner;
import edu.pitt.dbmi.tutor.ui.ConfigSelectorPanel;
import edu.pitt.dbmi.tutor.ui.FontPanel;
import edu.pitt.dbmi.tutor.util.Communicator;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

/**
 * main class for building configuration file for SlideTutor ITS
 * 
 * @author tseytlin
 * 
 */
public class ConfigurationBuilder implements ActionListener, PropertyChangeListener, ListSelectionListener {
	private final String ABOUT_MESSAGE = "<html><h2>Configuration Builder</h2>" +
											"<a href=\"http://slidetutor.upmc.edu/\">http://slidetutor.upmc.edu/</a><br>"+
											"Department of BioMedical Informatics<br>University of Pittsburgh";
	private JPanel configurationPanel, wizardPanel;
	private JMenuBar menubar;
	private JToggleButton tocButton, builderButton;
	private JPopupMenu tocMenu, builderMenu;
	private JToolBar toolbar, wizardbar;
	private JLabel statusLabel; //wizardName, wizardDescritpion,
	private JButton next, prev;
	private File configFile;
	private URL configURL;
	private Icon nextIcon;
	private JFrame helpFrame, behaviorFrame,sequenceFrame;
	private HelpBuilder helpBuilder;
	private BehaviorBuilder behaviorBuilder;
	private SequenceBuilder sequenceBuilder;
	private String helpFile;
	private JMenu contentMenu;
	private ConfigProperties configuration;
	private List<WizardPanel> wizardPanels;
	private JList tocList;
	private int wizardOffset;
	private ConfigSelectorPanel configSelectorPanel;
	private final FileFilter fileFilter = new FileFilter() {
		public String getDescription() {
			return "Tutor Configuration File (.conf)";
		}
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".conf");
		}
	};
	
	public ConfigurationBuilder(){
		Names.setConfigurationBuilder(this);
		Config.setLookAndFeel();
	}
	
	
	/**
	 * get component for this
	 * 
	 * @return
	 */
	public Component getComponent() {
		if (configurationPanel == null) {
			configurationPanel = new JPanel();
			configurationPanel.setLayout(new BorderLayout());

			/* JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			wizardName = new JLabel("Title");
			wizardName.setFont(wizardName.getFont().deriveFont(25f));
			wizardName.setHorizontalAlignment(JLabel.CENTER);
			wizardDescritpion = new JLabel(getDescription("Title Description of Title"));
			wizardDescritpion.setHorizontalAlignment(JLabel.CENTER);
			wizardDescritpion.setFont(wizardName.getFont().deriveFont(Font.PLAIN, 12f));
			p.add(wizardName, BorderLayout.NORTH);
			p.add(wizardDescritpion, BorderLayout.CENTER);
			*/
			JPanel pane = new JPanel();
			pane.setLayout(new BorderLayout());
			pane.setBorder(new BevelBorder(BevelBorder.RAISED));
			//pane.add(p, BorderLayout.NORTH);
			pane.add(getTableOfContentPanel(), BorderLayout.WEST);
			pane.add(getWizardPanel(), BorderLayout.CENTER);

			configurationPanel.add(getToolBar(), BorderLayout.NORTH);
			configurationPanel.add(pane, BorderLayout.CENTER);
			configurationPanel.add(getWizardButtons(), BorderLayout.SOUTH);

			// load the first screen
			loadPanel();

		}
		return configurationPanel;
	}

	
	/**
	 * content panel
	 * @return
	 */
	private Component getTableOfContentPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED),new EmptyBorder(new Insets(10,5,10,5))));
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
		ButtonGroup grp = new ButtonGroup();
		
		JRadioButton b1 = new JRadioButton("Simple",true);
		b1.setActionCommand("Simple");
		b1.addActionListener(this);
		grp.add(b1);
		p.add(b1);
		
		JRadioButton b2 = new JRadioButton("Advanced",false);
		b2.setActionCommand("Advanced");
		b2.addActionListener(this);
		grp.add(b2);
		p.add(b2);
		
		tocList = new JList();
		tocList.addListSelectionListener(this);
		tocList.setFixedCellHeight(30);
		tocList.setFixedCellWidth(120);
		JScrollPane s = new JScrollPane(tocList);
		panel.add(p,BorderLayout.NORTH);
		panel.add(s,BorderLayout.CENTER);
		
		// fill it up
		Vector<String> content = new Vector<String>();
		for (WizardPanel w : getWizardPanels()) {
			content.add(w.getShortName());
		}
		tocList.setListData(content);
		
		return panel;
	}
	
	private void update(){
		// fill it up
		final Vector<String> content = new Vector<String>();
		for (WizardPanel w : getWizardPanels()) {
			content.add(w.getShortName());
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tocList.setListData(content);
				loadPanel();
			}
		});
		
	}
	
	/**
	 * load wizard panels
	 * @param advanced
	 */
	private void loadWizardPanels(boolean advanced){
		wizardPanels = new ArrayList<WizardPanel>();
		wizardPanels.add(new StartPanel());
		if(advanced){
			wizardPanels.add(new TutorInfoPanel());
			wizardPanels.add(new TutorUIPanel());
			wizardPanels.add(new TutorModulePanel(this,ExpertModule.class));
			wizardPanels.add(new TutorModulePanel(this,PedagogicModule.class));
			wizardPanels.add(new TutorModulePanel(this,BehavioralModule.class));
			wizardPanels.add(new TutorModulePanel(this,StudentModule.class));
			wizardPanels.add(new TutorModulePanel(this,ProtocolModule.class));
			wizardPanels.add(new TutorModulePanel(this,PresentationModule.class));
			wizardPanels.add(new TutorModulePanel(this,InterfaceModule.class));
			wizardPanels.add(new TutorModulePanel(this,ReasoningModule.class));
			wizardPanels.add(new TutorModulePanel(this,FeedbackModule.class));
			wizardPanels.add(new OtherModulesPanel(this));
		}
		wizardPanels.add(new FinishPanel(this));

		// load defaults from panels
		for (WizardPanel p : wizardPanels) {
			getConfiguration().putAll(p.getProperties());
			p.addPropertyChangeListener(this);
		}
		wizardOffset = 0;
	}
	
	
	/**
	 * get wizard panels
	 * 
	 * @return
	 */
	public List<WizardPanel> getWizardPanels() {
		if (wizardPanels == null) {
			loadWizardPanels(false);
		}
		return wizardPanels;
	}

	/**
	 * pretty print description
	 * 
	 * @param text
	 * @return
	 */
	private String getDescription(String text) {
		return "<html><table width=700 height=200 cellpadding=10 bgcolor=\"#FFFFCC\">" + text + "</table></html>";
	}

	public JMenuBar getMenuBar() {
		if (menubar == null) {
			menubar = new JMenuBar();
			JMenu file = new JMenu("File");
			file.add(UIHelper.createMenuItem("New","New",Config.getIconProperty("icon.menu.new"),this));
			file.add(UIHelper.createMenuItem("Open","Open",Config.getIconProperty("icon.menu.open"),this));
			file.add(UIHelper.createMenuItem("Save","Save",Config.getIconProperty("icon.menu.save"),this));
			file.add(UIHelper.createMenuItem("Save As","Save As",Config.getIconProperty("icon.menu.save.as"),this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Import","Import",Config.getIconProperty("icon.menu.import"),this));
			file.add(UIHelper.createMenuItem("Publish","Publish",Config.getIconProperty("icon.menu.export"),this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Run", "Run",Config.getIconProperty("icon.menu.play"),this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Exit","Exit",null,this));
			menubar.add(file);
			
			
			//menubar.add(getContentMenu());
			
			JMenu builders = new JMenu("Editors");
			builders.add(UIHelper.createMenuItem("Help Builder", "Tutor Help Builder", UIHelper.getIcon(Config.getProperty("icon.menu.help")), this));
			builders.add(UIHelper.createMenuItem("Sequence Editor", "Static Sequence Editor", UIHelper.getIcon(Config.getProperty("icon.menu.sequence")), this));
			builders.add(UIHelper.createMenuItem("Behavior Editor", "Tutor Behavior Editor", UIHelper.getIcon(Config.getProperty("icon.menu.summary")), this));
			builders.add(UIHelper.createMenuItem("User Manager", "Database User Manager", UIHelper.getIcon(Config.getProperty("icon.toolbar.user")), this));
			menubar.add(builders);
			
			JMenu opts = new JMenu("Options");
			opts.add(UIHelper.createMenuItem("font-size","Change Font Size",UIHelper.getIcon(Config.getProperty("icon.menu.font")),this));
			menubar.add(opts);
			
			
			JMenu help = new JMenu("Help");
			help.add(UIHelper.createCheckboxMenuItem("debug","Debug",Config.getIconProperty("icon.menu.debug"),this));
			help.addSeparator();
			help.add(UIHelper.createMenuItem("Help","Help",UIHelper.getIcon(Config.getProperty("icon.menu.help")),this));
			help.add(UIHelper.createMenuItem("About","About",UIHelper.getIcon(Config.getProperty("icon.menu.about")),this));
			
			
			
			menubar.add(help);
		
		}
		return menubar;
	}

	private JPopupMenu getTableOfContent() {
		if (tocMenu == null) {
			tocMenu = new JPopupMenu();
			tocMenu.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							tocButton.doClick();
						}
					});
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				}

			});

			nextIcon = Config.getIconProperty("icon.menu.next");
			
			// create menu
			int i = 0;
			for (WizardPanel p : getWizardPanels()) {
				tocMenu.add(UIHelper.createMenuItem("offset:" + (i++), p.getName(), null, this));
			}
		}

		// clear all
		for (int i = 0; i < tocMenu.getComponentCount(); i++) {
			((JMenuItem) tocMenu.getComponent(i)).setIcon(null);
			((JMenuItem) tocMenu.getComponent(i)).setForeground(getWizardPanels().get(i).isVisited() ? Color.black
					: Color.gray);
		}
		// set offset
		if (wizardOffset >= 0 && wizardOffset < getWizardPanels().size()) {
			((JMenuItem) tocMenu.getComponent(wizardOffset)).setIcon(nextIcon);
		}

		return tocMenu;
	}
	
	private JMenu getContentMenu() {
		if (contentMenu == null) {
			contentMenu = new JMenu("Content");
			contentMenu.addMenuListener(new MenuListener() {
				public void menuSelected(MenuEvent e) {

					// clear all
					for (int i = 0; i < contentMenu.getMenuComponentCount(); i++) {
						((JMenuItem) contentMenu.getMenuComponent(i)).setIcon(null);
						((JMenuItem) contentMenu.getMenuComponent(i)).setForeground(getWizardPanels().get(i).isVisited() ? Color.black:Color.gray);
					}
					// set offset
					if (wizardOffset >= 0 && wizardOffset < contentMenu.getMenuComponentCount()) {
						((JMenuItem) contentMenu.getMenuComponent(wizardOffset)).setIcon(nextIcon);
					}

					
					contentMenu.validate();
				}
				public void menuDeselected(MenuEvent e) {}
				public void menuCanceled(MenuEvent e) {	}
			});
				
			nextIcon = Config.getIconProperty("icon.menu.next");
			
			// create menu
			int i = 0;
			for (WizardPanel p : getWizardPanels()) {
				contentMenu.add(UIHelper.createMenuItem("offset:" + (i++), p.getName(), null, this));
			}
		}

		return contentMenu;
	}
	
	

	private JPopupMenu getBuilderMenu() {
		if (builderMenu == null) {
			builderMenu = new JPopupMenu();
			builderMenu.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							builderButton.doClick();
						}
					});
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				}

			});
			// create menu
			builderMenu.add(UIHelper.createMenuItem("Help Builder", "Tutor Help Editor", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.help")), this));
			builderMenu.add(UIHelper.createMenuItem("Sequence Editor", "Case Sequence Editor", UIHelper
					.getIcon(Config.getProperty("icon.toolbar.sequence")), this));
			builderMenu.add(UIHelper.createMenuItem("Behavior Editor", "Tutor Behavior Editor", UIHelper
					.getIcon(Config.getProperty("icon.toolbar.summary")), this));
			builderMenu.add(UIHelper.createMenuItem("User Manager", "Database User Manager", UIHelper
					.getIcon(Config.getProperty("icon.toolbar.user")), this));
		}
		return builderMenu;
	}

	public JToolBar getToolBar() {
		if (toolbar == null) {
			toolbar = new JToolBar();
			toolbar.setOrientation(JToolBar.HORIZONTAL);

			// create menu for table of content
			/*
			tocButton = UIHelper.createToggleButton("TOC", "Table of Contents", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.toc")), null);
			tocButton.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (tocButton.isSelected()) {
						Dimension d = tocButton.getSize();
						getTableOfContent().show(tocButton, 0, d.height);
					}
				}
			});
			*/
			builderButton = UIHelper.createToggleButton("Editors",
					"A set of stand-alone editors to author specific file types used by some tutoring modules",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.edit")), -1, true, null);
			builderButton.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (builderButton.isSelected()) {
						Dimension d = builderButton.getSize();
						getBuilderMenu().show(builderButton, 0, d.height);
					}
				}
			});

			toolbar.add(UIHelper.createButton("New", "New Configuration", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.new")), this));
			toolbar.add(UIHelper.createButton("Open", "Open Configuration", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.open")), this));
			toolbar.add(UIHelper.createButton("Save", "Save Configuration", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.save")), this));
			//toolbar.add(UIHelper.createButton("Save As", "Save Configuration Under different name", UIHelper
			//		.getIcon(Config.getProperty("icon.toolbar.save.as")), this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("Import", "Import Configuration from Server", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.import")), this));
			toolbar.add(UIHelper.createButton("Publish", "Upload Configuration to a Server", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.export")), this));
			toolbar.addSeparator();
			//toolbar.add(tocButton);
			toolbar.add(UIHelper.createButton("Run", "Run SlideTutor ITS with this Configuration", UIHelper
					.getIcon(Config.getProperty("icon.toolbar.run")),-1,true,this));
			
			
			statusLabel = new JLabel();
			toolbar.addSeparator();
			toolbar.addSeparator();
			toolbar.add(statusLabel);
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(builderButton);
		}
		return toolbar;
	}

	/**
	 * get next previous buttons
	 * 
	 * @return
	 */
	public JComponent getWizardButtons() {
		if (wizardbar == null) {
			wizardbar = new JToolBar();
			wizardbar.setFloatable(false);
			wizardbar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

			next = UIHelper.createButton("   Next   ", null, UIHelper.getIcon(Config.getProperty("icon.toolbar.next")),
					-1, true, this);
			prev = UIHelper.createButton("   Back   ", null, UIHelper.getIcon(Config
					.getProperty("icon.toolbar.previous")), -1, true, this);
			next.setHorizontalTextPosition(JButton.LEFT);
			prev.setEnabled(false);

			wizardbar.add(next);
			wizardbar.add(prev);
			wizardbar.add(Box.createHorizontalGlue());
			wizardbar.add(UIHelper.createButton("Revert", "Revert the content of this panel to default values", UIHelper.getIcon(Config
					.getProperty("icon.toolbar.undo")), -1, true, this));

		}
		return wizardbar;
	}

	public JPanel getWizardPanel() {
		if (wizardPanel == null) {
			wizardPanel = new JPanel();
			wizardPanel.setLayout(new BorderLayout());
			//wizardPanel.setPreferredSize(new Dimension(820, 750));
		}
		return wizardPanel;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand().trim().toLowerCase();
		if ("next".equals(cmd)) {
			doNext();
		} else if ("back".equals(cmd)) {
			doBack();
		} else if ("new".equals(cmd)) {
			doNew();
		} else if ("open".equals(cmd)) {
			doOpen();
		} else if ("import".equals(cmd)) {
			doImport();
		} else if ("publish".equals(cmd)) {
			doPublish();
		} else if ("save".equals(cmd)) {
			doSave();
		} else if ("save as".equals(cmd)) {
			doSaveAs();
		} else if ("run".equals(cmd)) {
			doRun();
		} else if ("revert".equals(cmd)) {
			doRevert();
		} else if (cmd.startsWith("offset:")) {
			try {
				apply();
				wizardOffset = Integer.parseInt(cmd.substring("offset:".length()));
				loadPanel();
			} catch (Exception ex) {
			}
		} else if ("help builder".equals(cmd)) {
			doHelpBuilder();
		} else if ("behavior editor".equals(cmd)) {
			doBehaviorBuilder();
		} else if ("sequence editor".equals(cmd)) {
			doSequenceBuilder();
		} else if ("user manager".equals(cmd)) {
			doUserManager();
		}else if("about".equals(cmd)){
			JOptionPane.showMessageDialog(getComponent(),ABOUT_MESSAGE,"About",JOptionPane.PLAIN_MESSAGE,Config.getIconProperty("icon.general.tutor.logo"));
		}else if("exit".equals(cmd)){
			exit();
		}else if("simple".equals(cmd)){
			doSwitch(false);
		}else if("advanced".equals(cmd)){
			doSwitch(true);
		}else if("debug".equals(cmd)){
			Names.setShowAllProperties(((AbstractButton)e.getSource()).isSelected());
		}else if(cmd.equals("font-size")){
			FontPanel.getInstance().showDialog(getComponent());
		}else {
			JOptionPane.showMessageDialog(getComponent(), "Feature Not Implemented");
		}
	}

	private void doUserManager() {
		if(!getWizardPanels().isEmpty()){
			WizardPanel p = getWizardPanels().get(0);
			if(p instanceof StartPanel){
				if(((StartPanel)p).doConfigDatabase())
					((StartPanel)p).doUserManager();
			}
		}
	}


	private void doSwitch(boolean advanced){
		// apply previous changes
		apply();
		
		// save previous config
		Properties p = new Properties();
		p.putAll(getConfiguration());
		
		for (WizardPanel w : wizardPanels) {
			w.removePropertyChangeListener(this);
		}
		loadWizardPanels(advanced);
		configuration.putAll(p);
		update();
	}
	
	
	private void exit() {
		System.exit(0);
	}

	private void doBehaviorBuilder(){
		if (behaviorFrame == null) {
			behaviorBuilder = new BehaviorBuilder();

			// display
			behaviorFrame = new JFrame("Behavior Builder");
			behaviorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			behaviorFrame.getContentPane().add(behaviorBuilder.getComponent());
			behaviorFrame.pack();
			UIHelper.centerWindow(behaviorFrame);
		}
		
		String tasksLocation = null;
		// find help builder
		for(WizardPanel p : getWizardPanels()){
			if(p instanceof TutorModulePanel && GenericBehaviorModule.class.equals(((TutorModulePanel)p).getSelectedTutorModuleClass())){
				// found feedback module
				p.apply();
				Properties props = p.getProperties();
				if(props.containsKey("tutor.st.behavior")){
					tasksLocation = props.getProperty("tutor.st.behavior");
					break;
				}
			}
		}
	
		if(tasksLocation != null ){
			behaviorBuilder.load(tasksLocation);
		}
		
		behaviorFrame.setVisible(true);
	}
	
	private void doSequenceBuilder(){
		ExpertModule expert = Names.getDefaultExpertModule();
		String seqLocation = null;
		String condition = null;
		// find help builder
		for(WizardPanel p : getWizardPanels()){
			if(p instanceof TutorModulePanel && StaticCaseSequence.class.equals(((TutorModulePanel)p).getSelectedTutorModuleClass())){
				// found feedback module
				p.apply();
				Properties props = p.getProperties();
				if(props.containsKey("tutor.case.sequence")){
					seqLocation = props.getProperty("tutor.case.sequence");
					break;
				}
			}else if(p instanceof TutorModulePanel && ExpertModule.class.equals(((TutorModulePanel)p).getTutorModule())){
				p.apply();
				expert = (ExpertModule) ((TutorModulePanel)p).getSelectedTutorModuleObject();
			}else if(p instanceof TutorInfoPanel){
				p.apply();
				condition = ((TutorInfoPanel)p).getProperties().getProperty("tutor.condition");
			}
		}
		
		
		if (sequenceFrame == null) {
			sequenceBuilder = new SequenceBuilder(expert);

			// display
			sequenceFrame = new JFrame("Tutor Case Sequence Builder");
			sequenceFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			sequenceFrame.getContentPane().add(sequenceBuilder.getComponent());
			sequenceFrame.pack();
			UIHelper.centerWindow(sequenceFrame);
		}
		
	
		if(condition != null)
			sequenceBuilder.setCondition(condition);
		if(seqLocation != null )
			sequenceBuilder.load(seqLocation);
		
		sequenceFrame.setVisible(true);
	}
	
	private void doHelpBuilder() {
		if (helpFrame == null) {
			helpBuilder = new HelpBuilder();

			// display
			helpFrame = new JFrame("Help Builder");
			helpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			helpFrame.getContentPane().add(helpBuilder.getComponent());
			helpFrame.pack();
			UIHelper.centerWindow(helpFrame);
		}
		
		// load configuration
		apply();
		helpBuilder.setConfiguration(getConfiguration());
		
		// load file???
		/*
		try {
			// default
			String help = OntologyHelper.DEFAULT_TUTOR_HELP_FILE;
			
			// find help builder
			for(WizardPanel p : getWizardPanels()){
				if(p instanceof TutorModulePanel && HelpManager.class.equals(((TutorModulePanel)p).getSelectedTutorModuleClass())){
					// found feedback module
					p.apply();
					Properties props = p.getProperties();
					if(props.containsKey("tutor.st.help")){
						help = props.getProperty("tutor.st.help");
						break;
					}
				}
			}
			if(help != null && !help.equals(helpFile)){
				helpFile = help;
				try{
					// try as URL
					helpBuilder.load(new URL(help).openStream());
				}catch(MalformedURLException ex){
					File f = new File(help);
					// try as FILE
					if(f.exists() && f.isFile()){
						helpBuilder.load(f);
					}else{
						// else try as resource within JAR file
						helpBuilder.load(HelpBuilder.class.getResourceAsStream(help));
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		*/
		
		
		helpFrame.setVisible(true);

	}

	private void apply() {
		// save current
		if (wizardOffset >= 0 && wizardOffset < getWizardPanels().size()) {
			WizardPanel p = getWizardPanels().get(wizardOffset);
			p.apply();
			getConfiguration().putAll(p.getProperties());
		}
	}

	private void doRun() {
		apply();
		// init tutor
		if (Config.getMainFrame() == null || !Config.getMainFrame().isDisplayable()) {
			final ITS its = ITS.getInstance();
			its.initialize(getConfiguration());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					its.start();
				}
			});
		}
	}
	
	public void doPublish(){
		// initialize location of the server
		Config.setProperty("file.manager.server.url",OntologyHelper.DEFAULT_FILE_MANAGER_SERVLET);
		
		//first save
		doSave();
				
		if(configFile == null)
			return;
		
		// connect to server
		if(!Communicator.isConnected()){
			String [] login = UIHelper.promptLogin(true);
			if(login != null && login.length == 3){
				Properties p = new Properties();
				p.setProperty("repository.username",login[0]);
				p.setProperty("repository.password",login[1]);
				p.setProperty("repository.institution",login[2]);
				Config.getProperties().putAll(p);
				// authenticate
				if(!Communicator.authenticateWebsite(p)){
					String m = "Unable to login to central curriculum server";
					JOptionPane.showMessageDialog(getComponent(),m,"Error",JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		// do publish
		if(Communicator.isConnected()){
			ConfigSelectorPanel cp = getConfigSelectorPanel();
			if(cp.getInstitutionList() == null)
				cp.load();
			
			Vector studyList = cp.getStudyList();
			Vector instList =  cp.getInstitutionList();
			
			URL u = promptConfigurationURL(instList,studyList,configFile,Config.getProperty("repository.institution"));
			if(u != null){
				publish(u);
			}
		}
	}
	
	/**
	 * do publish to URL
	 */
	private void publish(URL u){
		if(u == null || configFile == null)
			return;
		
		
		// extract path from URL
		String path = u.getPath();
		if(path.startsWith("/"+OntologyHelper.CURRICULUM_ROOT))
			path = path.substring(OntologyHelper.CURRICULUM_ROOT.length()+2);
		if(path.endsWith(configFile.getName()))
			path = path.substring(0,path.length()-configFile.getName().length()-1);
	
		// init list of files to upload
		List<File> files = new ArrayList<File>();
		files.add(configFile);
	
		// substitute references to files in configurations with HTTP references
		Properties prop = configuration;
		for(Object key: prop.keySet()){
			// check if any property that starts w/ tutor. points to a file
			File f = new File(""+prop.get(key));
			// if value is indeed a file, hmmmm
			if(f.exists()){
				// add as a file to upload
				files.add(f);
				// substritute the original value
				prop.setProperty(key.toString(),OntologyHelper.DEFAULT_HOST_URL+"/"+OntologyHelper.CURRICULUM_ROOT+"/"+path+"/"+f.getName());
			
			}
		}
		
		// resave everything
		doSave();
				
		try {
			for(File f: files){
				Communicator.upload(f,OntologyHelper.CURRICULUM_ROOT,path);
			}
			
			// set configuration url
			configURL = u;
		
			// update
			getWizardPanels().get(getWizardPanels().size()-1).load();
			String m = "<html>Succesfully uploaded <font color=green>"+configFile.getName()+"</font> configuration as <br><a href=\"\">"+u+"</a>";
			JOptionPane.showMessageDialog(getComponent(),m,"Success",JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			Config.getLogger().severe(TextHelper.getErrorMessage(e));
			String m = "Could not upload file: "+TextHelper.getErrorMessage(e);
			JOptionPane.showMessageDialog(getComponent(),m,"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	/**
	 * prompt configuration file
	 * @param f
	 * @return
	 */
	private URL promptConfigurationURL(Vector institutionList, Vector studyList, File f, String inst){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JComboBox org = new JComboBox(studyList);
		org.setEditable(true);
		org.setBorder(new TitledBorder("Study"));
		org.setPreferredSize(new Dimension(200,45));
		JComboBox ins = new JComboBox(institutionList);
		ins.setEditable(true);
		ins.setBorder(new TitledBorder("Institution"));
		ins.setPreferredSize(new Dimension(100,45));
		ins.setSelectedItem(inst);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextField name = new JTextField();
		name.setText(f.getName());
		p.setBorder(new TitledBorder("Configuration"));
		p.setPreferredSize(new Dimension(200,45));
		p.add(name,BorderLayout.CENTER);
		panel.add(ins);
		panel.add(org);
		panel.add(p);
		
		// setup defaults from previos save
		if(configURL != null){
			Pattern pt = Pattern.compile("(http://.+/config/)(.+)/(.+)/.+");
			Matcher mt = pt.matcher(configURL.toString());
			if(mt.matches()){
				ins.setSelectedItem(mt.group(2));
				org.setSelectedItem(mt.group(3));
			}
		}
		
		
		int r = JOptionPane.showConfirmDialog(getComponent(),panel,"Create Configuration",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION && name.getText().length() > 0){
			try {
				return new URL(OntologyHelper.DEFAULT_CONFIG_URL+ins.getSelectedItem()+"/"+org.getSelectedItem()+"/"+name.getText());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return null;
		
	}
	
	
	/**
	 * get configuration URL
	 * @return
	 */
	public URL getConfigurationURL(){
		return configURL;
	}
	
	private void doSave() {
		if(configFile == null)
			doSaveAs();
		else	
			save(configFile);	
	}

	private void doSaveAs() {
		if(configFile == null && configURL != null){
			String n = configURL.toString();
			if(n.lastIndexOf('/') > -1){
				configFile = new File(n.substring(n.lastIndexOf('/')+1));
			}
		}
		
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(fileFilter);
		fc.setSelectedFile(configFile);
		if (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(getComponent())) {
			configFile = fc.getSelectedFile();
			if(!configFile.getName().endsWith(".conf"))
				configFile = new File(configFile.getParentFile(),configFile.getName()+".conf");
			save(configFile);
		}
	}

	/**
	 * save config file
	 */
	private void save(File configFile) {
		apply();
		FileWriter fw = null; 
		try { 
			fw = new FileWriter(configFile);
			getConfiguration().store(fw,"Created by Configuration Builder"); 
			
			// update status
			statusLabel.setText(configFile.getName());
			statusLabel.setForeground(new Color(0,100,0));
			
		}catch (IOException e) {
			Config.getLogger().severe(TextHelper.getErrorMessage(e)); 
		}finally{
			if(fw != null){
				try { 
					fw.close(); 
				} catch (IOException e) {
					Config.getLogger().severe(TextHelper.getErrorMessage(e));
				}
			}
		}
	}

	private void doOpen() {
		JFileChooser fc = new JFileChooser();
		fc.setSelectedFile(configFile);
		fc.setFileFilter(fileFilter);
		
		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(getComponent())) {
			configFile = fc.getSelectedFile();

			// load configuration file
			ConfigProperties conf = new ConfigProperties();
			try {
				conf.load(new FileReader(configFile));
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(getComponent(), "File " + configFile + " could not be found", "Error",
						JOptionPane.ERROR_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(getComponent(), "Could not read " + configFile
						+ ". Make sure it is a property file.", "Error", JOptionPane.ERROR_MESSAGE);
				Config.getLogger().severe(TextHelper.getErrorMessage(e));
			}

			// if tutor name is st, no conversion is required
			configuration = cleanProperties(conf);
			//configuration.list(System.out);
			// reload panel
			loadPanel();

		}
	}

	/**
	 * clean up properties to fit the tool format
	 * @param prop
	 * @return
	 */
	private ConfigProperties cleanProperties(ConfigProperties conf){
		// if tutor name is st, no conversion is required
		if ("st".equals(conf.getProperty("tutor.list"))) {
			return conf;
		} else {
			// fix SlideTutor name....
			String name = conf.getProperty("tutor.list", "").trim();
			ConfigProperties configuration = new ConfigProperties();
			for (Object key : conf.keySet()) {
				if (key.toString().startsWith("tutor." + name)) {
					configuration.put("tutor.st" + key.toString().substring(("tutor." + name).length()), conf.get(key));
				} else {
					configuration.put(key, conf.get(key));
				}
				configuration.setPropertyComment(""+key,conf.getPropertyComment(""+key));
			}
			configuration.put("tutor.list", "st");
			return configuration;
		}
	}
	
	private ConfigSelectorPanel getConfigSelectorPanel(){
		if(configSelectorPanel == null){
			configSelectorPanel = new ConfigSelectorPanel();
		}
		return configSelectorPanel;
	}
	
	
	private void doImport(){
		ConfigSelectorPanel cp = getConfigSelectorPanel();
		cp.showChooserDialog();
		Object o = cp.getSelectedObject();
		if(o != null){
			configFile = null;
			// load configuration file
			ConfigProperties conf = new ConfigProperties();
			try {
				configURL = new URL(""+o);
				conf.load(configURL.openStream());
			} catch (MalformedURLException e) {
				JOptionPane.showMessageDialog(getComponent(), "URL " + o + " could not be found", "Error", JOptionPane.ERROR_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(getComponent(), "Could not read " +o+ ". Make sure it is a property file.", "Error", JOptionPane.ERROR_MESSAGE);
				Config.getLogger().severe(TextHelper.getErrorMessage(e));
			}

			// if tutor name is st, no conversion is required
			configuration = cleanProperties(conf);
			// reload panel
			loadPanel();
		}
	}
	
	/**
	 * 
	 */
	private void doNew() {
		configFile = null;
		for (WizardPanel p : getWizardPanels())
			p.revert();
	}

	/**
	 * revert content
	 */
	private void doRevert() {
		if (wizardOffset < 0 || wizardOffset >= getWizardPanels().size())
			return;

		WizardPanel p = getWizardPanels().get(wizardOffset);
		p.revert();
	}

	/**
	 * load wizard panel
	 */
	private void loadPanel() {
		if (wizardOffset < 0 || wizardOffset >= getWizardPanels().size())
			return;

		// get selected wizard panel
		WizardPanel p = getWizardPanels().get(wizardOffset);
		//wizardName.setText(p.getName());
		//wizardDescritpion.setText(getDescription(p.getDescription()));
		getWizardPanel().removeAll();
		getWizardPanel().add(new JScrollPane(p.getComponent()), BorderLayout.CENTER);
		getWizardPanel().validate();
		getWizardPanel().repaint();

		// set properties for this panel
		p.setProperties(getConfiguration());
		p.load();

		// adjust buttons
		next.setEnabled(wizardOffset < getWizardPanels().size() - 1);
		prev.setEnabled(wizardOffset > 0);
		
		// adjust TOC
		tocList.setSelectedIndex(wizardOffset);
	}

	/**
	 * advance wizard
	 */
	public void doNext() {
		if (wizardOffset < getWizardPanels().size()) {
			// save current state
			apply();

			// advance to next
			wizardOffset++;
			loadPanel();
		}
	}

	/**
	 * go to previous page
	 */
	public void doBack() {
		if (wizardOffset > 0) {
			// save current state
			apply();

			wizardOffset--;
			loadPanel();
		}
	}

	/**
	 * get configuration content produced by this wizard
	 * 
	 * @return
	 */
	public ConfigProperties getConfiguration() {
		if (configuration == null)
			configuration = new ConfigProperties();
		return configuration;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if("CHANGE".equals(evt.getPropertyName())){
			// update status
			if(configFile == null)
				statusLabel.setText("< unnamed >");
			statusLabel.setForeground(new Color(100,0,0));
		}
		
	}
	
	/**
	 * are two modules compatible with eacho other
	 * @param module1
	 * @param module2
	 * @return
	 */
	
	public boolean isCompatibleWith(Class module1, Class module2){
		// choice of reasoner depends on choice of interface
		if( Arrays.asList(module1.getInterfaces()).contains(ReasoningModule.class) && 
		    Arrays.asList(module2.getInterfaces()).contains(InterfaceModule.class)){
			if(ReportInterface.class.equals(module2))
				return SimplePrognosticReasoner.class.equals(module1);
			else if(RadiologyInterface.class.equals(module2))
				return RadiologyReasoner.class.equals(module1);
			else 
				return SimpleDiagnosticReasoner.class.equals(module1);
		}
		
		// nothing works with dynamic book or knowledge explorer
		if(Arrays.asList(DynamicBook.class,KnowledgeExplorer.class).contains(module2)){
			for(Class c: Arrays.asList(InterfaceModule.class, FeedbackModule.class, ReasoningModule.class)){
				if(Arrays.asList(module1.getInterfaces()).contains(c))
					return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * if parameter of module1 depends on module2, then appropriate properties
	 * to the prop object, else do nothing
	 * @param modul1
	 * @param module2
	 * @param p
	 */
	public void setupProperty(Class module1, Class module2, Properties p){
		if(HelpManager.class.equals(module1)){
			if(SimpleDiagnosticReasoner.class.equals(module2)){
				if(OntologyHelper.DEFAULT_REPORT_HELP_FILE.equals(p.getProperty("tutor.help.location")))
					p.setProperty("tutor.help.location",OntologyHelper.DEFAULT_TUTOR_HELP_FILE);
			}else if(SimplePrognosticReasoner.class.equals(module2)){
				if(OntologyHelper.DEFAULT_TUTOR_HELP_FILE.equals(p.getProperty("tutor.help.location")))
					p.setProperty("tutor.help.location",OntologyHelper.DEFAULT_REPORT_HELP_FILE);
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConfigurationBuilder cb = new ConfigurationBuilder();

		// display
		JFrame frame = new JFrame("Tutor Configuration Builder");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(cb.getMenuBar());
		frame.getContentPane().add(cb.getComponent());

		frame.pack();
		UIHelper.centerWindow(frame);
		frame.setVisible(true);

	}


	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource() == tocList){
			if(!tocList.isSelectionEmpty() && wizardOffset != tocList.getSelectedIndex()){
				apply();
				wizardOffset = tocList.getSelectedIndex();
				loadPanel();
			}
		}
	}
}
