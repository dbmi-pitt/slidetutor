package edu.pitt.dbmi.tutor.builder.protocol;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import edu.pitt.dbmi.tutor.builder.protocol.scripts.*;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class ProtocolScriptManager implements ActionListener {
	// this is a list of available scripts, can be dynamic in future
	public final String [] SCRIPTS = new String [] {
			H5CleanProtocolTransfer.class.getName(),H3CleanProtocol.class.getName(),H5ProblemDetector.class.getName(),PhantomDeleteDetector.class.getName(),
			InferenceProblemDetector.class.getName(),InferredVSGoalDiagnosisDetector.class.getName(), HandleOrphanedEvents.class.getName(),H5FixRefineResponseConcept.class.getName(),
			H1LinkIEtoCE.class.getName(),H3ColorInconsistency.class.getName(),H3FixColorInconsistancy.class.getName(),H6EndEventTimestampFix.class.getName(),
			FindingNoLocation.class.getName(),FindDoubleNodeEvent.class.getName(),TestQuestionFixer.class.getName(),FixRefineClientEvent.class.getName(),H3SkillometerTime.class.getName(),
			FindDeleteAfterDone.class.getName(),H3NextStepFix.class.getName(),ClientEventDurations.class.getName(), TransferProduction.class.getName(),CopyUsersAndConditions.class.getName()};
	
	private Component main;
	private Selector experiment,condition, user;
	private SessionTable sessions;
	private JProgressBar progress;
	private JPanel statusPanel,transferPanel, logPanel,right;
	private JLabel statusLabel,sessionStatusLine;
	private JTextArea logArea,scriptDescription;
	private JButton logReturn;
	private JFrame dialog;
	private JComboBox scriptSelector;
	
	
	// data model
	private ProtocolModule protocol;

	/**
	 * set protocol module
	 * @param m
	 */
	public void setProtocolModule(ProtocolModule m){
		protocol = m;
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
			// selectors 
			experiment = new Selector("Experiment");
			condition = new Selector("Condition");
			user = new Selector("User");
			
			// top layer
			JPanel top = new JPanel();
			top.setLayout(new GridLayout(1,0));
			top.add(experiment);
			top.add(user);
			top.add(condition);
			top.setBorder(new TitledBorder("Source Protocol"));
			
			sessions = new SessionTable();
			sessionStatusLine = new JLabel(" ");
			sessionStatusLine.setHorizontalAlignment(JLabel.RIGHT);
			
			// center component
			JPanel mid = new JPanel();
			mid.setLayout(new BoxLayout(mid,BoxLayout.Y_AXIS));
			mid.add(new JScrollPane(sessions));
			mid.add(sessionStatusLine);
			mid.setBorder(new TitledBorder("Sessions to Transfer"));
			
			JPanel left = new JPanel();
			left.setLayout(new BorderLayout());
			left.add(top,BorderLayout.NORTH);
			left.add(mid,BorderLayout.CENTER);
			
			// right side
			right = new JPanel();
			right.setLayout(new BorderLayout());
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.setBorder(new TitledBorder("Select Protocol Script to Run"));
			
			
			scriptDescription = new JTextArea(5,200);
			scriptDescription.setLineWrap(true);
			scriptDescription.setWrapStyleWord(true);
			scriptDescription.setEditable(false);
			
			scriptSelector = new JComboBox();
			scriptSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if(e.getStateChange() == ItemEvent.SELECTED){
						Object o = scriptSelector.getSelectedItem();
						if(o != null && o instanceof ProtocolScript){
							scriptDescription.setText(((ProtocolScript)o).getDescription());
						}
					}
				}
			});
			scriptSelector.setMinimumSize(new Dimension(300,50));
			p.add(scriptSelector,BorderLayout.NORTH);
			p.add(new JScrollPane(scriptDescription),BorderLayout.CENTER);
			right.add(p,BorderLayout.NORTH);
			
			
			// consult button
			JButton transfer = new JButton("Run Script");
			transfer.setBorder(new BevelBorder(BevelBorder.RAISED));
			transfer.setIcon(UIHelper.getIcon(Config.getProperty("icon.toolbar.query")));
			transfer.setVerticalTextPosition(SwingConstants.BOTTOM);
			transfer.setHorizontalTextPosition(SwingConstants.CENTER);
			transfer.addActionListener(this);
			transfer.setActionCommand("run");
			transfer.setFont(transfer.getFont().deriveFont(Font.BOLD,16f));
			transfer.setPreferredSize(new Dimension(180,180));
			
			// consult panel
			transferPanel = new JPanel();
			transferPanel.setLayout(new GridBagLayout());
			transferPanel.setPreferredSize(new Dimension(700,700));
			transferPanel.add(transfer,new GridBagConstraints());
			right.add(transferPanel,BorderLayout.CENTER);
			
			// create log panel
			logPanel = new JPanel();
			logPanel.setLayout(new BorderLayout());
			logArea = new JTextArea();
			logArea.setEditable(false);
			logPanel.add(new JScrollPane(logArea),BorderLayout.CENTER);
			logReturn = new JButton("run another script");
			logReturn.setActionCommand("return");
			logReturn.addActionListener(this);
			logPanel.add(logReturn,BorderLayout.SOUTH);
			
			
			JSplitPane s = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			s.setLeftComponent(left);
			s.setRightComponent(right);
			s.setResizeWeight(1);
			
					
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
			panel.add(s,BorderLayout.CENTER);
			//panel.add(pPreview,BorderLayout.EAST);
			panel.add(statusPanel,BorderLayout.SOUTH);
			panel.setPreferredSize(new Dimension(1024,700));
			
			main = panel;
		}
		return main;
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
	 * load protocol related resources
	 */
	public void load(){
		// init
		getComponent();
		
		// load all experiments
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				experiment.load(protocol.getExperiments());
				user.load(protocol.getUsers(null));
				
				// load scripts
				scriptSelector.removeAllItems();
				//scriptSelector.addItem(null);
				for(String s: SCRIPTS){
					try{
						scriptSelector.addItem(Class.forName(s).newInstance());
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
				setBusy(false);
			}
		})).start();
	}
	
	/**
	 * selection has been made
	 * @param name
	 */
	private void selected(String name){
		if("experiment".equalsIgnoreCase(name)){
			List<String> users = new ArrayList<String>();
			for(String e : experiment.getSelectedValues())
				users.addAll(protocol.getUsers(e));
			user.load(users);
			sessions.load(Collections.EMPTY_LIST);
			condition.load(Collections.EMPTY_LIST);
		}else if("user".equalsIgnoreCase(name) && user.getSelectedValues().length > 0){
			Query query = new Query();
			for(String u: user.getSelectedValues())
				query.addUsername(u);
			loadSessions(query,true);
		}else if("condition".equalsIgnoreCase(name) && condition.getSelectedValues().length > 0){
			Query query = new Query();
			for(String u: user.getSelectedValues())
				query.addUsername(u);
			for(String c: condition.getSelectedValues())
				query.addCondition(c);
			loadSessions(query,false);
		}
	}
	
	// fetch sessions
	private void loadSessions(Query query, boolean u){
		final Query q = query;
		final boolean update = u;
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				final List<Session> s = protocol.getSessions(q);
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						sessions.load(s);
						sessionStatusLine.setText(s.size()+" sessions returned");
					
						// update conditions if user was selected
						if(update)
							updateUserConditions(s);
						
						setBusy(false);
					}
				});
			}
		})).start();
	}
	
	/**
	 * update condition list based on a set of sessions
	 * @param sessions
	 */
	private void updateUserConditions(List<Session> sessions){
		final Set<String> conds = new TreeSet<String>();
		for(Session s: sessions){
			if(s.getCondition() != null)
				conds.add(s.getCondition());
		}
		condition.load(new ArrayList<String>(conds));
	}
	

	/**
	 * show log panel
	 * @param show
	 */
	private void showLogPanel(boolean show){
		//final boolean show = s;
		//SwingUtilities.invokeLater(new Runnable(){
		//	public void run(){
		if(show){
			logReturn.setEnabled(false);
			right.remove(transferPanel);
			right.add(logPanel);
			logArea.setText("");
		}else{
			right.remove(logPanel);
			right.add(transferPanel);
		}
		//right.revalidate();
		//right.repaint();
		//	}
		//});
		getComponent().validate();
		getComponent().repaint();
	}
	
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("run".equals(cmd)){
			doRun();
		}else if("return".equals(cmd)){
			showLogPanel(false);
		}
	}
	
	
	/**
	 * do sveral checks and balances :)
	 * @return
	 */
	private boolean isValid(){
		// select script
		Object o = scriptSelector.getSelectedItem();
		if(o == null || !(o instanceof ProtocolScript)){
			JOptionPane.showMessageDialog(getComponent(),"No valid script was selected","Error",JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		// make sure there are users or sessions selected
		String [] users = user.getSelectedValues();
		if(users.length == 0){
			JOptionPane.showMessageDialog(getComponent(),"No users were selected","Error",JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		// get selected sessions
		int n = sessions.getSelectedSessions().size();
		if(n == 0)
			n = sessions.getAllSessions().size();
		
		// if no sessions 
		if(n == 0){
			JOptionPane.showMessageDialog(getComponent(),"No sessions were selected","Error",JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		String source = (protocol instanceof DatabaseProtocolModule)?
							protocol.getDefaultConfiguration().getProperty("protocol.url"):
							protocol.getDefaultConfiguration().getProperty("protocol.directory");
		
		// prompt if user is sure about this transfer
		/*
		StringBuffer msg = new StringBuffer();
		msg.append("<html><table width=550><tr><td colspan=2>Are you sure you want to run a script for <font color=red>"+n+"</font> sessions ");
		msg.append("for user(s): <font color=green>"+TextHelper.toString(users)+"</font></td></tr>");
		msg.append("<tr><td>on </td><td><font color=blue>"+source+"</font></td></tr></table>");
		int r = JOptionPane.showConfirmDialog(getComponent(),msg,"Transfer",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if(r != JOptionPane.YES_OPTION){
			return false;
		}
		*/
		return true;
	}
	

	
	/**
	 * user selector
	 * @author tseytlin
	 */
	private class Selector extends JPanel implements ActionListener, ListSelectionListener {
		private JList list;
		private String name;
		
		/**
		 * create concept selector
		 */
		public Selector(String title){
			super();
			name = title;
			setLayout(new BorderLayout());
			list = new JList();
			list.addListSelectionListener(this);
			add(createToolBar(title),BorderLayout.NORTH);
			add(new JScrollPane(list),BorderLayout.CENTER);
			setPreferredSize(new Dimension(200,150));
		}
		
		/**
		 * create tool bar
		 * @return
		 */
		private JToolBar createToolBar(String title){
			JToolBar toolbar = new JToolBar();
			toolbar.setFloatable(false);
			toolbar.setBackground(Color.white);
			toolbar.add(new JLabel(title));
			return toolbar;
		}

		public String getSelectedValue(){
			return (String) list.getSelectedValue();
		}
		public String [] getSelectedValues(){
			Object [] o = list.getSelectedValues();
			String [] s = new String [o.length];
			for(int i=0;i<s.length;i++)
				s[i] = (o[i] != null)?o[i].toString():"";
			return s;
		}
		
		public void load(List<String> data){
			final Object [] content = data.toArray();
			Arrays.sort(content,new Comparator() {
				public int compare(Object o1, Object o2) {
					return TextHelper.compare(""+o1,""+o2);
				}
			});
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					list.setListData(content);
					list.repaint();
				}
			});
		}
		
		public void valueChanged(ListSelectionEvent e) {
			if(!e.getValueIsAdjusting())
				selected(name);
			
		}

		public void actionPerformed(ActionEvent e) {
			
		}
		
	}
	
	public JFrame createDialog(Component parent){
		// create a dialog
		if(dialog == null){
			//JOptionPane op = new JOptionPane(getComponent(),JOptionPane.PLAIN_MESSAGE);
			dialog = new JFrame("Protocol Script Manager");
			dialog.getContentPane().setLayout(new BorderLayout());
			dialog.getContentPane().add(getComponent(),BorderLayout.CENTER);
			dialog.pack();
			dialog.setResizable(true);
			ImageIcon img = (ImageIcon) UIHelper.getIcon(Config.getProperty("icon.toolbar.query"));
			if(img != null)
				dialog.setIconImage(img.getImage());
		}
		return dialog;
	}
	
	/**
	 * log string in log window
	 * @param str
	 */
	private void log(String str){
		final String text = str;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				logArea.append(text);
			}
		});
		System.out.print(str);
		
	}
	
	/**
	 * this is where magic happens
	 */
	private void doRun(){
		if(isValid()){
			setBusy(true);
			showLogPanel(true);
			
			((new Thread(new Runnable(){
				public void run(){
					//get selected sessions
					List<Session> input = sessions.getSelectedSessions();
					if(input.isEmpty()){
						input = sessions.getAllSessions();
					}
			
					// get script to run
					ProtocolScript script = (ProtocolScript) scriptSelector.getSelectedItem();
					script.initialize();
					script.setProtocolModule(protocol);
					
					// set output for this script
					script.setOutput(new PrintStream(new OutputStream() {
						private StringBuffer str = new StringBuffer();
						public void write(int b) throws IOException {
							str.append(new String(new byte [] {(byte)b}));
						}
						public void flush() throws IOException {
							super.flush();
							log(str.toString());
							str = new StringBuffer();
						}
					},true));
						
					// setup  progress bar
					final int n = input.size();
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							progress.setMinimum(0);
							progress.setMaximum(n);
							progress.setIndeterminate(false);
						}
					});
					
					
					//now do actual work
					int x = 0;
					for(Session s: input){
//						log("Analyzing "+s.getUsername()+" "+
//							 OntologyHelper.getCaseName(s.getCase())+" session "+
//							 s.getSessionID()+" ..\n");
//						
						// copy a single session
						boolean status = script.process(s);
						if(!status){
							log("ERROR: A problem was encountered in the script...");
						}
						s.refresh();
						
						// update
						final int y = x++;
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								progress.setValue(y);
							}
						});
					}
					log("\nAll Done!");
					script.dispose();
					
					logReturn.setEnabled(true);
					setBusy(false);
					
				}
			}))).start();
			
		}
	}
	
	

	
	public static void main(String [] args){
		
		/*
		Config.setProperty("DatabaseProtocolModule.protocol.driver","oracle.jdbc.driver.OracleDriver");
		Config.setProperty("DatabaseProtocolModule.protocol.url","jdbc:oracle:thin:@o1prd02.isdip.upmc.edu:1521:pwdb");
		Config.setProperty("DatabaseProtocolModule.protocol.username","slidetutor");
		Config.setProperty("DatabaseProtocolModule.protocol.password","upci03st");
		*/
		/*
		Config.setProperty("DatabaseProtocolModule.protocol.driver","com.mysql.jdbc.Driver");
		Config.setProperty("DatabaseProtocolModule.protocol.url","jdbc:mysql://micron.remotes.upmc.edu/protocolX");
		Config.setProperty("DatabaseProtocolModule.protocol.username","user");
		Config.setProperty("DatabaseProtocolModule.protocol.password","resu");
		*/
		/*
		Config.setProperty("DatabaseProtocolModule.protocol.driver","oracle.jdbc.driver.OracleDriver");
		Config.setProperty("DatabaseProtocolModule.protocol.url","jdbc:oracle:thin:@o1dev01.isdip.upmc.edu:1521:upci");
		Config.setProperty("DatabaseProtocolModule.protocol.username","stutor_dev");
		Config.setProperty("DatabaseProtocolModule.protocol.password","dbmi10std");
		*/
		
		ProtocolModule protocol = new DatabaseProtocolModule();
		/*protocol.getDefaultConfiguration().setProperty("protocol.driver","oracle.jdbc.driver.OracleDriver");
		protocol.getDefaultConfiguration().setProperty("protocol.url","jdbc:oracle:thin:@o1dev01.isdip.upmc.edu:1521:upci");
		protocol.getDefaultConfiguration().setProperty("protocol.username","stutor_dev");
		protocol.getDefaultConfiguration().setProperty("protocol.password","dbmi10std");
		*/
		protocol.getDefaultConfiguration().setProperty("protocol.driver","oracle.jdbc.driver.OracleDriver");
		protocol.getDefaultConfiguration().setProperty("protocol.url","jdbc:oracle:thin:@o1prd02.isdip.upmc.edu:1521:pwdb");
		protocol.getDefaultConfiguration().setProperty("protocol.username","slidetutor");
		protocol.getDefaultConfiguration().setProperty("protocol.password","upci03st");
		protocol.getDefaultConfiguration().setProperty("protocol.query.input.tables","true");
		
		/*
		protocol.getDefaultConfiguration().setProperty("protocol.driver","oracle.jdbc.driver.OracleDriver");
		protocol.getDefaultConfiguration().setProperty("protocol.url","jdbc:oracle:thin:@o1prd02.isdip.upmc.edu:1521:pwdb");
		protocol.getDefaultConfiguration().setProperty("protocol.username","aslidetutor");
		protocol.getDefaultConfiguration().setProperty("protocol.password","upci03st");
		*/
		//protocol.getDefaultConfiguration().setProperty("protocol.driver","com.mysql.jdbc.Driver");
		//protocol.getDefaultConfiguration().setProperty("protocol.url","jdbc:mysql://micron.remotes.upmc.edu/protocolZ");
		//protocol.getDefaultConfiguration().setProperty("protocol.username","user");
		//protocol.getDefaultConfiguration().setProperty("protocol.password","resu");
		
		
		ProtocolScriptManager wizard = new ProtocolScriptManager();
		wizard.setProtocolModule(protocol);
		
		wizard.load();
		JFrame d = wizard.createDialog(null);
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//d.setModal(true);
		d.setVisible(true);
		UIHelper.centerWindow(d);
		
		
	}
}
