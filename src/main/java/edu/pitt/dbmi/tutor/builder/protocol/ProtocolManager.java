package edu.pitt.dbmi.tutor.builder.protocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.filechooser.FileFilter;
import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.presentation.SimpleViewerPanel;
import edu.pitt.dbmi.tutor.modules.protocol.ConsoleProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule;
import edu.pitt.dbmi.tutor.ui.ButtonTabComponent;
import edu.pitt.dbmi.tutor.ui.FontPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.Recorder;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

/**
 * handles for quering and managing protocol data
 * @author tseytlin
 *
 */

public class ProtocolManager implements ActionListener, ListSelectionListener{
	// constants
	//private final String ALL = "                ALL                ";
	private final String ALL = "ALL";
	//private final String DEFAULT_VIEWER_CONFIG = "/resources/defaults/ViewerPanel.properties";
	// some constunts 
	private final String ABOUT_MESSAGE = "<html><h2>Protocol Manager</h2>" +
										"<a href=\"http://slidetutor.upmc.edu/\">http://slidetutor.upmc.edu/</a><br>"+
										"Department of BioMedical Informatics<br>University of Pittsburgh";
	
	
	// GUI elements
	private UIHelper.ComboBox experimentBox;
	private UIHelper.List conditionBox, userBox, problemBox;
	private JPopupMenu playMenu,exportMenu;
	private JPanel actionPanel; 
	private JToggleButton play,export;
	private SessionTable sessionBox;
	private JTabbedPane resultTab;
	private Component controlPanel,main;
	private JMenuBar menubar;
	//private TableRowSorter<TableModel> sorter;
	private File file;
	private PlaybackPanel playbackPanel;
	private boolean blockAction;
	private JProgressBar progress;
	private JPanel statusPanel;
	private JLabel statusLabel,sessionStatusLine;
	private UserManager userManager;
	private JDialog userManagerDialog,transferDialog;
	private TransferWizard transferWizard;
	private ProtocolSelector protocolSelector;
	private Set<String> usersInCondition,conditionsInUser;
	
	// data model
	private ProtocolModule protocol;
	//private List<Session> sessionData;

	/**
	 * set protocol module
	 * @param m
	 */
	public void setProtocolModule(ProtocolModule m){
		// don't do for the same thing
		if(protocol == m)
			return;
		
		// first clean up old
		if(protocol != null)
			protocol.dispose();
		
		// then reset
		protocol = m;
		
		//then optionally reload
		if(main != null && main.isShowing()){
			load();
		}
	}
	
	public void dispose(){
		// first clean up old
		if(protocol != null)
			protocol.dispose();
	}
	
	protected void finalize() throws Throwable {
		dispose();
	}

	/**
	 * get protocol module
	 * @return
	 */
	public ProtocolModule getProtocolModule(){
		return protocol;
	}
	
	/**
	 * get main component
	 * @return
	 */
	public Component getComponent(){
		if(main == null){
			//Config.setLookAndFeel();
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			split.setLeftComponent(getControlPanel());
			split.setRightComponent(getResultPanel());
			split.setResizeWeight(0);
			
			
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
			
			
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(split,BorderLayout.CENTER);
			panel.add(statusPanel,BorderLayout.SOUTH);
			main = panel;
			
			// reset tooltip timer
			//ToolTipManager.sharedInstance().setInitialDelay(0);
			ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
			
		}
		return main;
	}
	
	/**
	 * load protocol related resources
	 */
	public void load(){
		// init
		getComponent();
		
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				
				// load values
				final List<String> conds = new ArrayList<String>(protocol.getExperiments());
				final List<String> users = new ArrayList<String>(protocol.getUsers(null));
				Collections.sort(conds);
				// load experiments
				Collections.sort(users, new Comparator<String>() {
					public int compare(String o1, String o2) {
						return (o1 != null && o2 != null)?TextHelper.compare(o1,o2):0;
					}
				});
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						experimentBox.clear();
						experimentBox.add(ALL);
						experimentBox.add(conds);
						
						userBox.clear();
						userBox.add(ALL);
						userBox.add(users);
						
						problemBox.clear();
						problemBox.add(ALL);
						
						conditionBox.clear();
						conditionBox.add(ALL);
						
						sessionBox.load(Collections.EMPTY_LIST);
					}
				});
				
				
				setBusy(false);
			}
		})).start();
		
		
	}
	
	/**
	 * get result panel
	 * @return
	 */
	private JTabbedPane getResultPanel(){
		if(resultTab == null){
			resultTab = new JTabbedPane();
			addQueryButton();
		}
		return resultTab;
	}
	
	/**
	 * add new result tab
	 */
	private void addQueryButton(){
		// consult button
		JButton consult = new JButton("Query Results");
		consult.setBorder(new BevelBorder(BevelBorder.RAISED));
		consult.setIcon(UIHelper.getIcon(Config.getProperty("icon.general.query")));
		consult.setVerticalTextPosition(SwingConstants.BOTTOM);
		consult.setHorizontalTextPosition(SwingConstants.CENTER);
		consult.addActionListener(this);
		consult.setActionCommand("Query");
		consult.setFont(consult.getFont().deriveFont(Font.BOLD,16f));
		consult.setPreferredSize(new Dimension(200,200));
		
		// consult panel
		JPanel consultPanel = new JPanel();
		consultPanel.setLayout(new GridBagLayout());
		consultPanel.setPreferredSize(new Dimension(700,700));
		consultPanel.add(consult,new GridBagConstraints());
		
		
		resultTab.addTab("Query",consultPanel);
	}
	
	
	/**
	 * Create control panel
	 * @return
	 */
	private Component getControlPanel(){
		if(controlPanel == null){
			// create experiment side
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1,0));
			//panel.setBorder(new LineBorder(Color.black));
			
			// experiment box
			experimentBox = new UIHelper.ComboBox();
			experimentBox.setBorder(new TitledBorder("Experiment"));
			experimentBox.addActionListener(this);
			experimentBox.setRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList arg0, Object cond, int arg2, boolean arg3,boolean arg4) {
					JLabel lb = (JLabel) super.getListCellRendererComponent(arg0, cond, arg2, arg3, arg4);
					if(ALL.equals(cond))
						lb.setForeground(Color.BLUE);
					return lb;
				}
			});			
			
			// condition box
			conditionBox = new UIHelper.List();
			conditionBox.getScrollPane().setBorder(new TitledBorder("Condition"));
			conditionBox.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			//conditionBox.addActionListener(this);
			conditionBox.setVisibleRowCount(4);
			conditionBox.setFixedCellWidth(150);
			conditionBox.addListSelectionListener(this);
			conditionBox.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList arg0, Object cond, int arg2, boolean arg3,boolean arg4) {
					JLabel lb = (JLabel) super.getListCellRendererComponent(arg0, cond, arg2, arg3, arg4);
					if(conditionsInUser != null){
						if(!conditionsInUser.contains(cond) && !ALL.equals(cond))
							lb.setForeground(Color.LIGHT_GRAY);
					}
					if(ALL.equals(cond))
						lb.setForeground(Color.BLUE);
					return lb;
				}
			});
			
			// user box
			userBox = new UIHelper.List();
			userBox.getScrollPane().setBorder(new TitledBorder("Student"));
			userBox.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			//userBox.addActionListener(this);
			userBox.setVisibleRowCount(4);
			userBox.setFixedCellWidth(150);
			userBox.addListSelectionListener(this);
			userBox.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList arg0, Object user, int arg2, boolean arg3,boolean arg4) {
					JLabel lb = (JLabel) super.getListCellRendererComponent(arg0, user, arg2, arg3, arg4);
					if(usersInCondition != null){
						if(!usersInCondition.contains(user) && !ALL.equals(user))
							lb.setForeground(Color.LIGHT_GRAY);
					}
					if(ALL.equals(user))
						lb.setForeground(Color.BLUE);
					return lb;
				}
			});
			
			// problem box
			problemBox = new UIHelper.List();;
			problemBox.getScrollPane().setBorder(new TitledBorder("Case"));
			problemBox.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			//problemBox.addActionListener(this);
			problemBox.setVisibleRowCount(4);
			problemBox.setFixedCellWidth(180);
			problemBox.addListSelectionListener(this);
			problemBox.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList arg0, Object arg1, int arg2, boolean arg3,boolean arg4) {
					JLabel lb = (JLabel) super.getListCellRendererComponent(arg0, arg1, arg2, arg3, arg4);
					lb.setText(OntologyHelper.getCaseName(""+arg1));
					if(ALL.equals(arg1))
						lb.setForeground(Color.BLUE);
					
					return lb;
				}
			});
			
			
			//panel.add(experimentBox);
			panel.add(conditionBox.getScrollPane());
			panel.add(userBox.getScrollPane());
			panel.add(problemBox.getScrollPane());
			JPanel pnl = new JPanel();
			pnl.setLayout(new BorderLayout());
			pnl.add(experimentBox,BorderLayout.NORTH);
			pnl.add(panel,BorderLayout.CENTER);
			
			// session box
			sessionBox = new SessionTable();
			sessionStatusLine = new JLabel(" ");
			sessionStatusLine.setHorizontalAlignment(JLabel.RIGHT);
			
			
			JScrollPane scroll = new JScrollPane(sessionBox);
			scroll.setBackground(Color.white);
			scroll.setBorder(new TitledBorder("Sessions"));
			//sessionBox.setCellRenderer(new Entry.EntryRenderer());
			
			// filter by tutor
			//filterByTutor = new JCheckBox("Only display "+((tutor != null)?tutor.getName():"")+" sessions",true);
			
			JPanel sessionPanel = new JPanel();
			sessionPanel.setLayout(new BorderLayout());
			sessionPanel.add(scroll,BorderLayout.CENTER);
			sessionPanel.add(sessionStatusLine,BorderLayout.SOUTH);
			//sessionPanel.add(filterByTutor,BorderLayout.SOUTH);
			
			
			JPanel cp = new JPanel();
			cp.setLayout(new BorderLayout());
			cp.add(getActionPanel(),BorderLayout.WEST);
			cp.add(getPlaybackPanel().getComponent(),BorderLayout.CENTER);
			
			getPlaybackPanel().setPlaybackControlsEnabled(false);
			
			// add to right panel
			JPanel rightPanel = new JPanel();
			rightPanel.setLayout(new BorderLayout());
			//rightPanel.add(pnl,BorderLayout.NORTH);
			rightPanel.add(sessionPanel,BorderLayout.CENTER);
			rightPanel.add(cp,BorderLayout.SOUTH);
			
			JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			split.setTopComponent(pnl);
			split.setBottomComponent(rightPanel);
			
			controlPanel = split;
		}
		return controlPanel;
	}
	/**
	 * sort column
	 * @param c
	 *
	private void sortColumn(int c){
		sorter.setSortable(c ,true);
		sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(c,SortOrder.ASCENDING)));
		sorter.sort();
	}
	*/
	
	
	/**
	 * get action panel
	 * @return
	 */
	private JPanel getActionPanel(){
		if(actionPanel == null){
			JPanel cp = new JPanel();
			cp.setBorder(new TitledBorder("Session Control"));
			cp.setLayout(new BorderLayout());
			
			// buttons
			JToolBar toolbar = new JToolBar();
			toolbar.setFloatable(false);
			cp.add(toolbar,BorderLayout.NORTH);
			
			// add play button 
			play = UIHelper.createToggleButton("play","Play Selected Session",UIHelper.getIcon(Config.getProperty("icon.toolbar.play")),this);
			playMenu = new JPopupMenu();
			playMenu.addPopupMenuListener(new PopupMenuListener(){
				public void popupMenuCanceled(PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							play.doClick();
						}
					});
				}
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
				
			});
			
			// add to the menu
			playMenu.add(UIHelper.createMenuItem("play-tutor","Tutor Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.play.tutor")),this));
			playMenu.add(UIHelper.createMenuItem("play-viewer","Viewer Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.play.viewer")),this));
			((JMenuItem)playMenu.getComponent(0)).setText(null);	
			((JMenuItem)playMenu.getComponent(1)).setText(null);
			
			// add export button 
			export = UIHelper.createToggleButton("export","Export Selected Session",UIHelper.getIcon(Config.getProperty("icon.toolbar.export")),this);
			exportMenu = new JPopupMenu();
			exportMenu.addPopupMenuListener(new PopupMenuListener(){
				public void popupMenuCanceled(PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							export.doClick();
						}
					});
				}
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
				
			});
		
			// add to the menu
			exportMenu.add(UIHelper.createMenuItem("export-tutor","Export Tutor Session",UIHelper.getIcon(Config.getProperty("icon.toolbar.export.tutor")),this));
			exportMenu.add(UIHelper.createMenuItem("export-summary","Export Session Summary",UIHelper.getIcon(Config.getProperty("icon.toolbar.export.summary")),this));
			exportMenu.add(UIHelper.createMenuItem("export-viewer","Export Viewer Session",UIHelper.getIcon(Config.getProperty("icon.toolbar.export.viewer")),this));
			((JMenuItem)exportMenu.getComponent(0)).setText(null);	
			((JMenuItem)exportMenu.getComponent(1)).setText(null);
			((JMenuItem)exportMenu.getComponent(2)).setText(null);
			
			// trip button size
			for(JPopupMenu m: new JPopupMenu [] {exportMenu,playMenu}){
				for(int i=0;i<m.getComponentCount();i++)
					((JComponent)m.getComponent(i)).setPreferredSize(new Dimension(32,32));
			}
			
			
			toolbar.add(play);
			toolbar.add(export);
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("session-summary","Session Summary",UIHelper.getIcon(Config.getProperty("icon.toolbar.session.summary")),this));
			toolbar.add(UIHelper.createButton("refresh","Refresh",UIHelper.getIcon(Config.getProperty("icon.toolbar.refresh")),this));
			toolbar.addSeparator();
			
		//toolbar.add()
			actionPanel = cp;
		}
		return actionPanel;
	}
	
	/**
	 * create playback panel
	 * @return
	 */
	private PlaybackPanel getPlaybackPanel(){
		if(playbackPanel == null){
			playbackPanel = new PlaybackPanel();
			playbackPanel.setPlaybackVisible(false);
			playbackPanel.setRecordVisible(false);
			playbackPanel.setSourceProtocolModule(getProtocolModule());
		}
		return playbackPanel;
	}
	
	
	
	/**
	 * get menu bar
	 * @return
	 */
	public JMenuBar getMenuBar(){
		if(menubar == null){
			menubar = new JMenuBar();
			
			JMenu file = new JMenu("File");
			JMenu edit = new JMenu("Edit");
			JMenu tools = new JMenu("Tools");
			JMenu opts  = new JMenu("Options");
			JMenu help = new JMenu("Help");
			
			file.add(UIHelper.createMenuItem("close","Exit",null,this));
			edit.add(UIHelper.createMenuItem("delete","Delete Sessions",UIHelper.getIcon(Config.getProperty("icon.menu.delete")),this));
			tools.add(UIHelper.createMenuItem("user-manager","User Manager",UIHelper.getIcon(Config.getProperty("icon.menu.user")),this));
			tools.add(UIHelper.createMenuItem("transfer","Transfer Wizard",UIHelper.getIcon(Config.getProperty("icon.menu.transfer")),this));
			tools.addSeparator();
			tools.add(UIHelper.createMenuItem("preferences","Preferences",UIHelper.getIcon(Config.getProperty("icon.menu.preferences")),this));
			help.add(UIHelper.createMenuItem("about","About",UIHelper.getIcon(Config.getProperty("icon.menu.about")),this));
			opts.add(UIHelper.createMenuItem("font-size","Change Font Size",UIHelper.getIcon(Config.getProperty("icon.menu.font")),this));
			
			menubar.add(file);
			menubar.add(edit);
			menubar.add(tools);
			menubar.add(opts);
			menubar.add(help);
		}
		return menubar;
	}

	
	/**
	 * handle experiment being selected
	 * @param experiment
	 */
	private void handleExperimentSelection(String study){
		//blockAction = true;
		final String experiment = study;
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				
				final List<String> conds = new ArrayList<String>();
				final List<String> users = new ArrayList<String>();
				
				// load conditions
				if(experiment != null && !ALL.equals(experiment)){
					conds.addAll(protocol.getConditions(experiment));
					Collections.sort(conds);
				}
									
				// load users
				users.addAll(protocol.getUsers((ALL.equals(experiment))?null:experiment));
				Collections.sort(users, new Comparator<String>() {
					public int compare(String o1, String o2) {
						return (o1 != null && o2 != null)?TextHelper.compare(o1,o2):0;
					}
				});
				
				// update UI
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						// re- initialize lists
						conditionBox.clear();
						userBox.clear();
						conditionBox.add(ALL);
						userBox.add(ALL);
						
						// load conditions
						if(!conds.isEmpty()){
							conditionBox.add(conds);
						}
						
						// load users
						userBox.add(users);
						//blockAction = false;
					}
				});
				
				
				
				setBusy(false);
			}
		})).start();
	}
	
	/**
	 * handle user selection
	 * @param user
	 * @param condition
	 *
	private void handleUserConditionSelection(){
		//blockAction = true;
		// get cases
		final Query query = new Query();
			if(!ALL.equals(userBox.getSelectedValue()))
			for(Object user: userBox.getSelectedValues()){
				query.addUsername(user.toString());
			}
		if(!ALL.equals(conditionBox.getSelectedValue()))
			for(Object condition: conditionBox.getSelectedValues()){
				query.addUsername(condition.toString());
			}
		String experiment = (String) experimentBox.getSelectedItem();
		if(!ALL.equals(experiment) && !TextHelper.isEmpty(experiment))
			query.addExperiment(experiment);
		
		// fetch sessions
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				final List<String> cases = protocol.getCases(query);
				Collections.sort(cases,new Comparator<String>() {
					public int compare(String o1, String o2) {
						return OntologyHelper.getCaseName(o1).compareTo(OntologyHelper.getCaseName(o2));
					}
				});
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						// remove stuff
						problemBox.clear();
						
						// load cases
						problemBox.add(ALL);
						problemBox.add(cases);
						//blockAction = false;
					}
				});
				setBusy(false);
			}
		})).start();
	}
	*/
	/**
	 * get query constraints from the control panel
	 * @return
	 */
	private Query getQuery(){
		Query q = new Query();
		
		// add experiment
		if(!ALL.equals(experimentBox.getSelectedItem())){
			q.addExperiment(experimentBox.getSelectedItem().toString());
		}
		
		// add users
		for(Object o: userBox.getSelectedValues()){
			// if ALL is one of the selections, then don't use as condition
			if(ALL.equals(o)){
				q.remove("username");
				break;
			}
			q.addUsername(o.toString());
		}
	
		// add conditions
		for(Object o: conditionBox.getSelectedValues()){
			if(ALL.equals(o)){
				q.remove("experiment_condition_name");
				break;
			}
			q.addCondition(o.toString());
		}
	
		
		// add cases
		for(Object o: problemBox.getSelectedValues()){
			if(ALL.equals(o)){
				q.remove("case_url");
				break;
			}
			q.addCase(o.toString());
		}
		return q;
	}
	
	
	/**
	 * handle session box
	 * @param problem
	 * @param user
	 * @param condition
	 */
	private void handleSessionBox(){
		//blockAction = true;
		final Query q = getQuery();	
		
		// don't do anything on an empty stomach
		if(q.isEmpty() || (q.size() == 1 && q.hasValue("experiment")))
			return;
		
		// fetch sessions
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				blockAction = true;
				
				// fetch a list of sessions
				final List<Session> sessions = protocol.getSessions(q);
				
				// get a list of cases
				final Set<String> cases = new TreeSet<String>(new Comparator<String>() {
					public int compare(String o1, String o2) {
						return OntologyHelper.getCaseName(o1).compareTo(OntologyHelper.getCaseName(o2));
					}
				});
				
				// add to list of cases
				for(Session s: sessions)
					cases.add(s.getCase());
				
				
				// update UI
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						sessionBox.load(sessions);
						sessionStatusLine.setText(sessions.size()+" sessions returned");
						
						// update problem list if no problem was selected
						if(!q.hasValue("case_url")){
							problemBox.clear();
							problemBox.add(ALL);
							problemBox.add(cases);
						}
						
						// update user conditions
						updateUserConditions(q,sessions);
						setBusy(false);
						blockAction = false;
					}
				});
			}
		})).start();
	}
	
	private void doDeleteSessions(){
		final List<Session> sessions = sessionBox.getSelectedSessions();
		if(sessions.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"No Protocol Sessions Selected","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		String s = "Are you sure you want to delete selected sessions?";
		if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(getComponent(),s,"Are you Sure",JOptionPane.YES_NO_OPTION)){
			// now raise HELL!
			if(Config.raiseRedAlert("<html>You about to delete actual data! You better be joking!<br>" +
								"If not, then type THE password to confirm your ill advised action<br>" +
								"and press <b>Retry</b> button.")){
				

				// fetch sessions
				(new Thread(new Runnable(){
					public void run(){
						String p = progress.getString();
						progress.setIndeterminate(true);
						progress.setMinimum(0);
						progress.setMaximum(sessions.size());
						
						setBusy(true);
						blockAction = true;
						
						int i=1;
						for(Session s: sessions){
							progress.setString("Deleting Session "+i+" out of "+sessions.size()+" ...");
							s.delete();
							progress.setIndeterminate(false);
							progress.setValue(i);
							i++;
						}
						
						setBusy(false);
						blockAction = false;
						progress.setString(p);
						
						// update session UI
						handleSessionBox();
					}
				})).start();
			}
		}
	}
	
	/**
	 * update condition list based on a set of sessions
	 * @param sessions
	 */
	private void updateUserConditions(Query query,List<Session> sessions){
		//blockAction = true;
		
		// figure out which mode we want to execute
		//final boolean experimentSelected = !query.hasValue("experiment_condition_name") && !query.hasValue("username");
		boolean userSelected = query.hasValue("username");
		boolean conditionSelected = query.hasValue("experiment_condition_name");
		
		// if both selected, then don't do highlighting
		if(userSelected && conditionSelected){
			usersInCondition = conditionsInUser = null;
		}else{
			// initialize lists
			usersInCondition = (conditionSelected)?new HashSet<String>():null;
			conditionsInUser = (userSelected)?new HashSet<String>():null;
			
			for(Session s: sessions){
				// if user was selected, create condition filter list
				if(conditionsInUser != null && s.getCondition() != null)
					conditionsInUser.add(s.getCondition());
				
				// if condition was selecte, create username filter list
				if(usersInCondition != null && s.getUsername() != null)
					usersInCondition.add(s.getUsername());
				
			}
		}
		
		// update UI
		conditionBox.repaint();
		userBox.repaint();
	}
	
	
	/**
	 * perform query
	 * @param sessions
	 */
	private void doQuery(List<Session> sessions){
		if(sessions.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		ResultPanel result = new ResultPanel(this,sessions);
		// prepare result panel
		JTabbedPane tabs = getResultPanel();
		int i = tabs.getSelectedIndex();
		tabs.removeTabAt(i);
		tabs.addTab("Result "+(resultTab.getTabCount()+1),result);
		tabs.setTabComponentAt(i,new ButtonTabComponent(tabs));

		addQueryButton();
		tabs.setSelectedIndex(i);
	
	
	}
	
	/**
	 * get list of selected sessions
	 * @return
	 */
	private List<Session> getSelectedSessions(){
		return sessionBox.getSelectedSessions();
	}
	
	/**
	 * handle actions
	 */
	public void actionPerformed(ActionEvent e) {
		if(blockAction)
			return;
		
		String cmd = e.getActionCommand();
		if(e.getSource() == experimentBox){
			handleExperimentSelection(""+experimentBox.getSelectedItem());
		}else if(e.getSource() == userBox){
		/*	Object uSel = userBox.getSelectedItem();
			Object cSel = conditionBox.getSelectedItem();
			handleUserConditionSelection(""+uSel,""+cSel);*/
		}else if(e.getSource() == conditionBox){
			/*Object uSel = userBox.getSelectedItem();
			Object cSel = conditionBox.getSelectedItem();
			//if(!ALL.equals(uSel))
			handleUserConditionSelection(""+uSel,""+cSel);*/
		}else if(e.getSource() == problemBox){
			/*Object eSel = experimentBox.getSelectedItem();
			Object pSel = problemList.getSelectedItem();
			Object uSel = userBox.getSelectedItem();
			Object cSel = conditionBox.getSelectedItem();
			handleSessionBox((String) eSel,(String)pSel,(String)uSel,(String)cSel);*/
		}else if("Query".equals(e.getActionCommand())){
			doQuery(sessionBox.getSelectedSessions());
		}else if(e.getSource() == play){
			if(play.isSelected()){
				playMenu.show(play,0,-playMenu.getPreferredSize().height);
			}else{
				playMenu.setVisible(false);
			}
		}else if(e.getSource() == export){
			if(export.isSelected()){
				exportMenu.show(export,0,-exportMenu.getPreferredSize().height);
			}else{
				exportMenu.setVisible(false);
			}
		}else if("session-summary".equals(cmd)){
			doSessionSummary(getSelectedSessions());
		}else if("play-tutor".equals(cmd)){
			doPlayTutor(getSelectedSessions());
		}else if("play-viewer".equals(cmd)){
			doPlayViewer(getSelectedSessions());
		}else if("export-tutor".equals(cmd)){
			doExportTutor(getSelectedSessions());
		}else if("export-summary".equals(cmd)){
			doExportSummary(getSelectedSessions());
		}else if("export-viewer".equals(cmd)){
			doExportViewer(getSelectedSessions());
		}else if(cmd.equals("close")){
			System.exit(0);
		}else if(cmd.equals("user-manager")){
			doUserManager();
		}else if(cmd.equals("transfer")){
			doTransferWizard();
		}else if(cmd.equals("preferences")){
			doPreferences();
		}else if(cmd.equals("about")){
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(getComponent()),ABOUT_MESSAGE,"About",
			JOptionPane.PLAIN_MESSAGE,UIHelper.getIcon(Config.getProperty("icon.toolbar.query")));
		}else if(cmd.equals("refresh")){
			handleSessionBox();
		}else if(cmd.equals("delete")){
			doDeleteSessions();
		}else if(cmd.equals("font-size")){
			FontPanel.getInstance().showDialog(getComponent());
		}
	}
	
	
	private void doUserManager(){
		if(userManager == null){
			userManager = new UserManager();
		}
	
		// create a dialog
		if(userManagerDialog == null){
			userManagerDialog = userManager.createDialog(getComponent());
			userManagerDialog.addWindowListener(new WindowAdapter() {
				public void windowDeactivated(WindowEvent e) {
					// reload
					load();
				}
			});
		}
		
		// don't do anything if already displayed
		if(!userManagerDialog.isShowing()){
			userManagerDialog.setVisible(true);
			UIHelper.centerWindow(getComponent(),userManagerDialog);
			
			//reset protocol if necessary
			if(!getProtocolModule().equals(userManager.getProtocolModule()))
				userManager.setProtocolModule(getProtocolModule());
			
			// load
			userManager.load();
		}
	}
	
	public void doTransferWizard(){
		// init wizard
		if(transferWizard == null){
			transferWizard = new TransferWizard();
		}
	
		// create a dialog
		if(transferDialog == null){
			transferDialog = transferWizard.createDialog(getComponent());
		}
		
		// don't do anything if already displayed
		if(!transferDialog.isShowing()){
			transferDialog.setVisible(true);
			UIHelper.centerWindow(getComponent(),transferDialog);
			
			//reset protocol if necessary
			if(!getProtocolModule().equals(transferWizard.getProtocolModule()))
				transferWizard.setProtocolModule(getProtocolModule());
			
			// load
			transferWizard.load();
		}
	}
	
	public void doPreferences(){
		if(protocolSelector == null){
			protocolSelector = new ProtocolSelector();
		}
		
		// reset protocol
		protocolSelector.setProtocolModule(getProtocolModule());
		
		if(protocolSelector.showDialog(getComponent())){
			ProtocolModule m = protocolSelector.getProtocolModule();
			if(m.isConnected()){
				setProtocolModule(m);
			}else{
				JOptionPane.showMessageDialog(getComponent(),
				"Could not connect to selected Protocol Module","Error",JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	


	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		JComponent c = (JComponent) getComponent();
		if(busy){
			c.remove(statusPanel);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			progress.setIndeterminate(true);
			//progress.setString(null);
			c.remove(progress);
			c.add(statusPanel,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	/**
	 * do summary of the session
	 * @param s
	 */
	private void doSessionSummary(List<Session> s){
		if(s.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		final List<Session> sessions = s;
		((new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				UIHelper.HTMLPanel p1 = new UIHelper.HTMLPanel();
				p1.setText(getSummaryText(sessions));
				p1.setEditable(false);
				UIHelper.HTMLPanel p2 = new UIHelper.HTMLPanel();
				p2.setEditable(false);
				p2.setText(getSequenceText(sessions));
				p2.addHyperlinkListener(new HyperlinkListener() {
					public void hyperlinkUpdate(HyperlinkEvent e) {
						if(e.getEventType() == EventType.ENTERED){
							((JComponent)e.getSource()).setToolTipText("<html><table width=\"500\"><tr><td>"+e.getDescription()+"</td></tr></table>");
						}else if(e.getEventType() == EventType.EXITED){
							((JComponent)e.getSource()).setToolTipText(null);
						}
					}
				});
				
				JTabbedPane tabs = new JTabbedPane();
				tabs.addTab("Session Problem Outcome",new JScrollPane(p1));
				tabs.addTab("Session Action Sequence",new JScrollPane(p2));
				
				JPanel pn = new JPanel();
				pn.setLayout(new BorderLayout());
				pn.setPreferredSize(new Dimension(600,700));
				pn.add(tabs,BorderLayout.CENTER);
				
				setBusy(false);
				JOptionPane p = new JOptionPane(pn,JOptionPane.PLAIN_MESSAGE);
				JDialog d = p.createDialog(getComponent(),"Session Summary");
				d.setModal(false);
				d.setResizable(true);
				d.setVisible(true);
			}
		}))).start();
		
	}
	
	
	/**
	 * get session summary for a given session
	 * @param s
	 * @return map where each key refers to type of evidence, and value is a map
	 * between object description and label
	 */
	private Map<String,Map<String,Evidence>> getSessionSummary(Session s){
		Map<String,Map<String,Evidence>> caseSummary = new HashMap<String, Map<String,Evidence>>();
		for(ClientEvent c: s.getClientEvents()){
			// don't do auto=true and stuf with object description
			if(c.getObjectDescription() != null && !c.isAuto()){
				Map<String,Evidence> m = caseSummary.get(c.getType());
				if(m == null){
					m = new HashMap<String, Evidence>();
					caseSummary.put(c.getType(),m);
				}
				// get responses for this event
				List<TutorResponse> trs = s.getTutorResponses(c);
				String r = (!trs.isEmpty())?trs.get(0).getResponse():RESPONSE_CONFIRM;
				String e = (!trs.isEmpty())?trs.get(0).getError():ERROR_OK;
				
				// now add or remove
				if(ACTION_ADDED.equalsIgnoreCase(c.getAction())){
					m.put(c.getObjectDescription(),new Evidence(c.getLabel(),r,e));
				}else if(ACTION_REMOVED.equalsIgnoreCase(c.getAction())){
					m.remove(c.getObjectDescription());
				// old ways of doing it
				}else if(c.getAction().equalsIgnoreCase(c.getType()) || 
						 c.getAction().toLowerCase().contains("evidence") || 
						 c.getAction().equalsIgnoreCase("AttributeValue") || c.getAction().equalsIgnoreCase("added")){
					m.put(c.getObjectDescription(),new Evidence(c.getLabel(),r,e));
				}else if(ACTION_SELF_CHECK.equalsIgnoreCase(c.getAction()) && m.containsKey(c.getObjectDescription())){
					m.get(c.getObjectDescription()).fok = c.getInputMap().get("fok");
				}
			}
		}
		return caseSummary;
	}
	
	/**
	 * class that represents evidence w/ feedback
	 * @author tseytlin
	 *
	 */
	private class Evidence implements Comparable<Evidence>{
		public String label, response, error,fok;
		public List<Evidence> attributes;
		public Evidence(String l, String r, String e){
			label = l;
			response = r;
			error = e;
			attributes = new ArrayList<Evidence>();
		}
		public String toString(){
			StringBuffer str = new StringBuffer((label == null)?"":label);
			for(Evidence e: attributes){
				str.append(", "+e);
			}
			return str.toString();
		}
		public boolean equals(Object o){
			return toString().equals(o.toString());
		}
		public int hashCode(){
			return toString().hashCode();
		}
		public int compareTo(Evidence o) {
			return toString().compareTo(o.toString());
		}
	}
	
	
	
	/**
	 * get summary text
	 * @param sessions
	 * @return
	 */
	private String getSummaryText(List<Session> sessions){
		StringBuffer b = new StringBuffer();
		for(Session s: sessions){
			b.append("<center><h2>");
			b.append("user: <font color=blue>"+s.getUsername()+"</font> ");
			b.append("problem: <font color=green>"+OntologyHelper.getCaseName(s.getCase())+"</font> ");
			Date d = s.getFinishTime();
			String duration = (d != null)?TextHelper.formatDuration(d.getTime()-s.getStartTime().getTime()):"";
			b.append("duration: <font color=red>"+duration+"</font> ");
			b.append("</h2></center><hr>");
			Map<String,Map<String,Evidence>> caseSummary = getSessionSummary(s);
			String [] types = new String [] {TYPE_FINDING,TYPE_ABSENT_FINDING,
					TYPE_HYPOTHESIS,TYPE_DIAGNOSIS,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK};
			for(String type : types){
				List<Evidence> list = (type.equals(TYPE_FINDING) || type.equals(TYPE_ABSENT_FINDING))?
									getFindingList(caseSummary, type):getList(caseSummary,type);
				//if(!list.isEmpty()){
					b.append("<h2>"+type.replaceAll("([a-z])([A-Z])","$1 $2")+"</h2>");
					b.append("<ul>");
					for(Evidence l: list ){
						StringBuffer lb = new StringBuffer();
						String c = (RESPONSE_CONFIRM.equalsIgnoreCase(l.response))?"black":"red";
						lb.append("<font color="+c+">"+l.label+"</font>");
						for(Evidence a: l.attributes){
							String ca = (RESPONSE_CONFIRM.equalsIgnoreCase(a.response))?"black":"red";
							lb.append(", <font color="+ca+">"+a.label+"</font>");
						}
						b.append("<li><b>"+lb+"</b></li>");
					}
					b.append("</ul>");
				//}
			}
		}
		return b.toString();
	}
	

	
	/**
	 * get list
	 * @param caseSummary
	 * @param type
	 * @return
	 */
	private List<Evidence> getList(Map<String,Map<String,Evidence>> caseSummary,String type){
		List<Evidence> list = new ArrayList<Evidence>();
		if(caseSummary.containsKey(type)){
			list.addAll(caseSummary.get(type).values());
			Collections.sort(list);
		}
		return list;
	}
	
	/**
	 * get list
	 * @param caseSummary
	 * @param type
	 * @return
	 */
	private List<Evidence> getFindingList(Map<String,Map<String,Evidence>> caseSummary,String type){
		List<Evidence> list = new ArrayList<Evidence>();
		Map<String,Evidence> am = caseSummary.get(TYPE_ATTRIBUTE);
		// try old way
		if(am == null)
			am = caseSummary.get("Attributes");
		
		if(caseSummary.containsKey(type)){
			for(String k: caseSummary.get(type).keySet()){
				Evidence value = caseSummary.get(type).get(k);
				if(am != null){
					for(String a: am.keySet()){
						if(a.startsWith(k)){
							//value.label += ", "+am.get(a);
							value.attributes.add(am.get(a));
						}
					}
				}
				list.add(value);
			}
			Collections.sort(list);
		}
		
		return list;
	}
	
	
	/**
	 * get summary text
	 * @param sessions
	 * @return
	 */
	private String getSequenceText(List<Session> sessions){
		StringBuffer b = new StringBuffer();
		for(Session s: sessions){
			b.append("<center><h2>");
			b.append("user: <font color=blue>"+s.getUsername()+"</font> ");
			b.append("problem: <font color=green>"+OntologyHelper.getCaseName(s.getCase())+"</font> ");
			Date d = s.getFinishTime();
			String duration = (d != null)?TextHelper.formatDuration(d.getTime()-s.getStartTime().getTime()):"";
			b.append("duration: <font color=red>"+duration+"</font> ");
			b.append("</h2></center><hr>");
			b.append("<table border=0 cellspacing=5>");
			
			ClientEvent hint = null;
			StringBuffer hintMessage = null;
			String hintCount = null;
			for(ClientEvent c: s.getClientEvents()){
				if(TYPE_HINT.equals(c.getType())){
					// write out stale hint
					if(hint != null){
						String text = (""+hintMessage).replaceAll("\"","'");
						b.append("<tr>");
						b.append("<td><font color=blue>"+hint.getAction().toLowerCase()+"</font></td> ");
						b.append("<td>"+hint.getType()+"</td>");
						b.append("<td><b><font color=blue><a href=\""+text+"\">"+hint.getLabel()+
								"</a></font> out of <font color=blue>"+hintCount+"</font></b>");
						b.append("</td></tr>");
						hint = null;
						hintCount = null;
					}
					
					hint = (ClientEvent) c.clone();
					hint.setLabel("Levels");
					hintMessage = null;
				}else if(TYPE_HINT_LEVEL.equals(c.getType()) && hint != null){
					String l = c.getLabel();
					int x = l.indexOf('/');
					if(x > -1){
						hintCount = l.substring(x+1).trim();
						l = l.substring(0,x).trim();
					}else{
						x = l.indexOf("of");
						if(x > -1){
							hintCount = l.substring(x+2).trim();
							l = l.substring(0,x).trim();
						}
					}
					hint.setLabel(hint.getLabel()+" "+l+" ");
					if(hintMessage == null){
						hintMessage = new StringBuffer();
					}else
						hintMessage.append("<hr>");
					hintMessage.append(getMessage(s,c));
				}else if(isUsefulType(c)){
					// write out hint
					if(hint != null){
						String text = (""+hintMessage).replaceAll("\"","'");
						b.append("<tr>");
						b.append("<td><font color=blue>"+hint.getAction().toLowerCase()+"</font></td> ");
						b.append("<td>"+hint.getType()+"</td>");
						b.append("<td><b><font color=blue><a href=\""+text+"\">"+hint.getLabel()+
								"</a></font> out of <font color=blue>"+hintCount+"</font></b>");
						b.append("</td></tr>");
						hint = null;
						hintCount = null;
					}
					// write out CE
					b.append("<tr>");
					b.append("<td><font color=blue>"+c.getAction().toLowerCase()+"</font></td> ");
					b.append("<td>"+c.getType()+"</td>");
					String color = (isCorrect(s,c))?"green":"red";
					String err = getMessage(s,c);
					if(!TextHelper.isEmpty(err))
						b.append("<td><b><font color="+color+"><a href=\""+err+"\">"+c.getLabel()+"</a></font></b>");
					else	
						b.append("<td><b><font color="+color+">"+c.getLabel()+"</font></b>");
					if(TYPE_ATTRIBUTE.equals(c.getType())){
						b.append(" to <b><font color=green>"+ConceptEntry.getConceptEntry(c.getParent())+"</font></b>");
					}
					//b.append("</td><td>");
					// write out entire concept
					//b.append(getEntireConcept(c.getInputString()));
					b.append(" [<a href=\""+getEntireConcept(c.getInputString())+"\">1</a>]");
					b.append("</td></tr>");
				}
			}
			b.append("</table>");
		}
		return b.toString();
	}
	
	
	/**
	 * extract entire concept
	 * @param input
	 * @return
	 */
	private String getEntireConcept(String input){
		String entireConcept = "";
		// extract parent feature and entire concept
		Map<String,String> map = (Map<String,String>) TextHelper.parseMessageInput(input);
		if(map != null && map.containsKey("entire_concept")){
			entireConcept = map.get("entire_concept");
			// parse object description
			String [] p = entireConcept.split("\\.");
			if(p.length == 3)
				entireConcept = p[1];
		}
		return entireConcept;
	}
	
	/**
	 * is usefull type
	 * @param c
	 * @return
	 */
	private boolean isUsefulType(ClientEvent c){
		String [] types = new String [] {TYPE_FINDING,TYPE_ABSENT_FINDING,
				TYPE_ATTRIBUTE,TYPE_HYPOTHESIS,TYPE_DIAGNOSIS,TYPE_DONE,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK,"Attributes"};
		for(String type: types){
			if(type.equals(c.getType()))
				return true;
		}
		return false;
	}
	
	/**
	 * was client event correct
	 * @param s
	 * @param c
	 * @return
	 */
	private boolean isCorrect(Session s, ClientEvent c){
		for(TutorResponse r :s.getTutorResponses(c)){
			if(!RESPONSE_FAILURE.equalsIgnoreCase(r.getResponse()))
				return true;
		}
		return false;
	}
	
	/**
	 * was client event correct
	 * @param s
	 * @param c
	 * @return
	 */
	private String getMessage(Session s, ClientEvent c){
		StringBuffer str = new StringBuffer();
		appendMessage(str,c.getInput());
		for(TutorResponse r :s.getTutorResponses(c)){
			appendMessage(str,r.getInput());
		}
		return str.toString();
	}
	
	
	/**
	 * append message
	 * @param str
	 * @param input
	 */
	private void appendMessage(StringBuffer str, Object input){
		if(input instanceof Map){
			Map map = (Map) input;
			for(Object key: map.keySet()){
				if(key.toString().startsWith("message-")){
					if(str.length() > 0)
						str.append("<hr>");
					str.append(map.get(key));
				}else if(key.toString().startsWith("Message")){
					// try to parse old messages
					boolean include = false;
					for(String part: map.get(key).toString().split("(TEXT:|POINTERS:)")){
						if(include){
							if(str.length() > 0)
								str.append("<hr>");
							str.append(part);
						}
						include = !include;
					}
				}
			}
		}
	}
	
	
	/**
	 * do summary of the session
	 * @param s
	 */
	private void doPlayTutor(List<Session> sessions){
		if(sessions.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		// bring up a viewer
		final Session s = sessions.get(0);
		
		// check session
		String cf = s.getConfiguration();
		try{
			new URL(cf);
		}catch(Exception ex){
			if(cf == null || !(new File((cf.indexOf("?") > -1)?cf.substring(0,cf.lastIndexOf("?")):cf)).exists()){
				JOptionPane.showMessageDialog(getComponent(),"<html>Can't load session configuration <font color=blue>"+s.getConfiguration()+"</font><p>"+
						"This can often happen when the session was generated by previous tutor archirecture","Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		// init tutor && load case
		final ITS its = ITS.getInstance();
		
		// now launch the events
		/*
		if(playback != null){
			playback.stop();
			// update speed
			updateSpeed(1.0);
		}
		*/
		getPlaybackPanel().reset();
		
		// exit previous
		its.exit();	
		
		// now init
		its.initialize(s.getConfiguration());
		
		// disable saving in protocol
		its.getProtocolModule().setEnabled(false);
		its.show();
		
		(new Thread(){
			public void run(){
				its.setBusy(true);
		    	its.openCase(s.getCase());
    	    	its.setBusy(false);
    	    	
    	    	// start playback
    			// we can skip sections w/out viewer movements
    			//playback = new ProtocolPlayer(ProtocolManager.this,getClientMessages(s));
    			//playback.play();
    	    	getPlaybackPanel().play(getClientMessages(s));
			}
		}).start();
	}
	
	/**
	 * do summary of the session
	 * @param s
	 */
	private void doPlayViewer(List<Session> sessions){
		if(sessions.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		// bring up a viewer
		Session s = sessions.get(0);
		try{
			Config.load(s.getConfiguration());
		}catch(Exception e){
			// try to load default viewer configuration
			//try{
			//	Config.load(getClass().getResource(DEFAULT_VIEWER_CONFIG));
		    //}catch(Exception ex){
			JOptionPane.showMessageDialog(getComponent(),"<html>Can't load session configuration <font color=blue>"+s.getConfiguration()+"</font><p>"+
					"This can often happen when the session was generated by previous tutor archirecture","Error",JOptionPane.ERROR_MESSAGE);
			return;
				//e.printStackTrace();
			//}
		}
		
		DomainExpertModule domain = new DomainExpertModule();
		SimpleViewerPanel viewer = new SimpleViewerPanel();
		viewer.setInteractive(false);
		Communicator.getInstance().addRecipient(viewer);
		
		JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(getComponent()));
		dialog.setResizable(true);
		dialog.setModal(false);
		dialog.getContentPane().add(viewer.getComponent());
		dialog.pack();
		dialog.setVisible(true);
		UIHelper.centerWindow(getComponent(),dialog);
		
		
		// now launch the events
		//if(playback != null)
		//	playback.stop();
		getPlaybackPanel().reset();
		
		// load case
		viewer.setCaseEntry(domain.getCaseEntry(s.getCase()));
		
		// start playback
		// we can skip sections w/out viewer movements
		playbackPanel.play(new ProtocolPlayer(playbackPanel,getClientMessages(s)){
			public void processMessage(Message msg){
				if(TYPE_PRESENTATION.equals(msg.getType())){
					super.processMessage(msg);
				}else{
					lastTime = msg.getTimestamp();
				}
			}
		});
	}
	
	/**
	 * do summary of the session
	 * @param s
	 */
	private void doExportTutor(List<Session> sessions){
		if(sessions.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		//TODO:
	}
	
	/**
	 * do summary of the session
	 * @param s
	 */
	private void doExportSummary(List<Session> sessions){
		if(sessions.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser(file);
		chooser.setFileFilter(new FileFilter() {
			public String getDescription() {
				return "Comma Separated Values (.csv)";
			}
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
			}
		});
		final CSVAccessoryPanel acc = new CSVAccessoryPanel();
		chooser.setAccessory(acc);
		
		// prompt user for output file
		if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(getComponent())){
			file = chooser.getSelectedFile();
			if(!file.getName().endsWith(".csv"))
				file = new File(file.getAbsolutePath()+".csv");
			final List<Session> slist = sessions;
			(new Thread(new Runnable(){
				public void run(){
					setBusy(true);
					// predefine evidence types
					final String S = acc.getSeparator(); //",";
					String [] types = new String [] 
					{TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_HYPOTHESIS,TYPE_DIAGNOSIS,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK};
					try{
						BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						// iterate over sessions
						for(Session s: slist ){
							Map<String,Map<String,Evidence>> caseSummary = getSessionSummary(s);
							
							// write out the lines
							for(String type : types){
								List<Evidence> list = (type.equals(TYPE_FINDING) || type.equals(TYPE_ABSENT_FINDING))?
													getFindingList(caseSummary, type):getList(caseSummary,type);
								if(!list.isEmpty()){
									// write out each line
									for(Evidence label: list){
										writer.write(s.getCondition()+S+s.getUsername()+S+TextHelper.getName(s.getCase())+S+
													 s.getSessionID()+S+type+S+"\""+label+"\""+S+label.response+S+label.error+
													 ((label.fok != null)?S+label.fok:"")+"\n");
										//TODO: take care of attributes
									}
								}
							}
							
						}
						writer.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
					setBusy(false);
				}
			})).start();
			
		}
	}
	
	
	/**
	 * get client events and interface events sorted by timestamp
	 * @param s
	 * @return
	 */
	private List<Message> getClientMessages(Session s){
		List<Message> messages = new ArrayList<Message>();
		messages.addAll(s.getInterfaceEvents());
		messages.addAll(s.getClientEvents());
		
		// sort by date
		Collections.sort(messages,new Comparator<Message>() {
			public int compare(Message o1, Message o2) {
				Date d1 = new Date(o1.getTimestamp());
				Date d2 = new Date(o2.getTimestamp());
				if(d1.equals(d2))
					return 0;
				if(d1.before(d2))
					return -1;
				return 1;
			}
		});
		return messages;
	}
	
	/**
	 * do summary of the session
	 * @param s
	 */
	private void doExportViewer(List<Session> s){
		if(s.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"You must select one or more sessions","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		JFileChooser chooser = new JFileChooser(file);
		if(JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(getComponent())){
			file = chooser.getSelectedFile();
			final List<Session> sessions = s;
			(new Thread(new Runnable(){
				public void run(){
					setBusy(true);
					Recorder recorder = new Recorder();
					try{
						recorder.setFile(file);
						recorder.setRecord(true);
					}catch(Exception e){
						JOptionPane.showMessageDialog(getComponent(),"Problem writing to file","Error",JOptionPane.ERROR_MESSAGE);
						return;
					}
					// iterate over sessions
					for(Session s: sessions ){
						// iterate over messages
						for(Message msg: getClientMessages(s)){
							if(TYPE_PRESENTATION.equals(msg.getType())){
								Date d = new Date(msg.getTimestamp());
								if(ACTION_IMAGE_CHANGE.equals(msg.getAction())){
									recorder.recordImageChange(msg.getLabel(),d);
								}else if(ACTION_VIEW_CHANGE.equals(msg.getAction())){
									recorder.recordViewObserve(TextHelper.parseViewPosition(msg.getLabel()),d);
								}else if(ACTION_VIEW_RESIZE.equals(msg.getAction())){
									recorder.recordViewResize(TextHelper.parseDimension(msg.getLabel()),d);
								}else if(ACTION_IDENTIFY.equals(msg.getAction())){
									//recorder.recordAnnotation(m,d);
								}
							}
						}
						recorder.dispose();
					}
					setBusy(false);
				}
			})).start();
			
		}
		
	}
	

	/**
	 * prompt for login info if necessary
	 */
	public void login(){
		if(protocol != null){
			String error = null;
			do{
				String [] p = UIHelper.promptLogin(error);
				// exit on cancel
				if(p == null){
					System.exit(0);
				}else{
					Config.setProperty("username",p[0]);
					Config.setProperty("password",p[1]);
				}
				error = "Authentication failed";
			}while(!protocol.authenticateAdministrator(Config.getUsername(),Config.getPassword()));
		}
	}
	
	

	public void valueChanged(ListSelectionEvent e) {
		if(blockAction)
			return;
		
		if(!e.getValueIsAdjusting()){
			if(e.getSource() == userBox || e.getSource() == conditionBox || e.getSource() == problemBox){
				handleSessionBox();
			}
		}
		
	}
	
	/**
	 * CSV related options
	 * @author tseytlin
	 */
	public static class CSVAccessoryPanel extends JPanel {
		private JTextField stext;
		public CSVAccessoryPanel(){
			super();
			setLayout(new BorderLayout());
			setBorder(new TitledBorder("Export Options"));
			JPanel p = new JPanel();
			p.setLayout(new FlowLayout());
			final JCheckBox sep = new JCheckBox("field seperator");
			sep.setToolTipText("Click here to define a custom field separator");
			sep.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					stext.setEditable(sep.isSelected());
				}
			});
			stext = new JTextField(2);
			stext.setHorizontalAlignment(JTextField.CENTER);
			stext.setText(",");
			stext.setEditable(false);
			p.add(sep);
			p.add(stext);
			String help = "<html><table width=\"190\" bgcolor=\"#FFFFCC\"><tr><td>"+
						  "If cell data contains commas or double quotes, using a custom <b>field separator</b> " +
						  "may be desirable. Good options include: |, semicolon or any other infrequently used character."+
						  "</td></tr></table>";
			JLabel lbl = new JLabel(help);
			lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
			add(lbl,BorderLayout.CENTER);
			add(p,BorderLayout.SOUTH);
		
		}
		public String getSeparator(){
			String s = stext.getText().trim();
			return (s.length() > 0)?s:",";
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// load custom config file if specifyied
		if(args.length > 0){
			Config.load(args[0]);
		}
		
		final ProtocolModule protocol = new DatabaseProtocolModule();
		//final ProtocolModule protocol = new FileProtocolModule();
		
		// close protocol when we are quitting
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				protocol.dispose();
			}
		});
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				ProtocolManager pm = new ProtocolManager();
				pm.setProtocolModule(protocol);
				
				
				// don't login if no admin group
				List<String> experiments = protocol.getExperiments();
				String admin = Config.getProperty(protocol,"protocol.admin.group");
				if(experiments.contains(admin))
					pm.login();
				
				// display
				JFrame frame = new JFrame("Protocol Manager");
				ImageIcon img = (ImageIcon) UIHelper.getIcon(Config.getProperty("icon.general.query"));
				if(img != null)
					frame.setIconImage(img.getImage());
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setJMenuBar(pm.getMenuBar());
				frame.getContentPane().add(pm.getComponent());
				frame.pack();
				frame.setVisible(true);
				UIHelper.centerWindow(frame);
				
				// load
				pm.load();
			}
		});
		
		
		
	}


}
