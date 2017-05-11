package edu.pitt.dbmi.tutor.builder.protocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class UserManager {
	private Component main;
	private JMenuBar menubar;
	private Selector experiment,condition, user;
	private JEditorPane userPreview;
	private JProgressBar progress;
	private JPanel statusPanel;
	private JLabel statusLabel;
	private JDialog userManagerDialog;
	
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
			experiment = new Selector("Experiment");
			condition = new Selector("Condition");
			user = new Selector("User");
			JSplitPane s = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			s.setLeftComponent(experiment);
			s.setRightComponent(condition);
			s.setResizeWeight(0.5);
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			split.setLeftComponent(s);
			split.setRightComponent(user);
			split.setResizeWeight(0.5);
			
			// create preview panel
			JPanel pPreview = new JPanel();
			pPreview.setLayout(new BorderLayout());
			
			JPanel lb = new JPanel();
			lb.setBorder(new BevelBorder(BevelBorder.LOWERED));
			lb.setBackground(Color.white);
			lb.setLayout(new BorderLayout());
			lb.setPreferredSize(new Dimension(32,32));
			JLabel l = new JLabel("User Information");
			l.setHorizontalAlignment(JLabel.CENTER);
			lb.add(l,BorderLayout.CENTER);
			
			userPreview = new UIHelper.HTMLPanel(); 
			userPreview.setEditable(false);
			userPreview.setPreferredSize(new Dimension(300,300));
			pPreview.add(lb,BorderLayout.NORTH);
			pPreview.add(new JScrollPane(userPreview),BorderLayout.CENTER);
			
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
			
			JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			sp.setLeftComponent(split);
			sp.setRightComponent(pPreview);
			sp.setResizeWeight(1.0);
			
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(sp,BorderLayout.CENTER);
			//panel.add(pPreview,BorderLayout.EAST);
			panel.add(statusPanel,BorderLayout.SOUTH);
			
			
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
				setBusy(false);
			}
		})).start();
	}
	
	private void selected(String what){
		if("Experiment".equals(what)){
			condition.load(protocol.getConditions(experiment.getSelectedValue()));
			user.load(protocol.getUsers(experiment.getSelectedValue()));
		}else if("Condition".equals(what)){
			
		}else if("User".equals(what)){
			Properties p = protocol.getUserInfo(user.getSelectedValue());
			if(p != null){
				StringBuffer b = new StringBuffer();
				for(Object k: p.keySet()){
					b.append("<p><b>"+k+"</b><br>");
					b.append(p.get(k)+"</p>");
				}
				userPreview.setText(b.toString());
			}else{
				userPreview.setText("");
			}
				
		}	
	}
	
	private void pressed(String what, String action){
		if("Experiment".equals(what)){
			if("Add".equals(action)){
				doAddExperiment();
			}else if("Remove".equals(action)){
				doRemoveExperiment();
			}
		}else if("Condition".equals(what)){
			if("Add".equals(action)){
				doAddCondition();
			}else if("Remove".equals(action)){
				doRemoveCondition();
			}
		}else if("User".equals(what)){
			if("Add".equals(action)){
				doAddUser();
			}else if("Remove".equals(action)){
				doRemoveUser();
			}
		}	
	}
	
	private void doAddUser(){
		// create fields
		JTextField experimentField = new JTextField(20);
		experimentField.setText(experiment.getSelectedValue());
		experimentField.setEditable(false);
		
		JTextArea instructions = new JTextArea(5,10);
		instructions.setEditable(false);
		instructions.setWrapStyleWord(true);
		instructions.setLineWrap(true);
		instructions.setBackground(new Color(255,255,200));
		instructions.setText("Create one or more users. You can specify more then one user by" +
				" using a number range in username and password.\n" +
				"Example: user1:20 will create user1 through user20");
		
		
		// create fields
		JTextField usernameField = new JTextField(20);
		JTextField passwordField = new JTextField(20);
		
		// create labels
		JLabel  experimentLabel = new JLabel("Experiment:   ", JLabel.RIGHT);
		JLabel  userNameLabel = new JLabel("Username:   ", JLabel.RIGHT);
		JLabel  passwordLabel = new JLabel("Password:   ", JLabel.RIGHT);
		JLabel  informationLabel = new JLabel("Information:   ", JLabel.RIGHT);
				
		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel,BoxLayout.X_AXIS));
		connectionPanel.setBorder(new EmptyBorder(10,10,10,10));
		
		JButton infoButton = new JButton("add additional information");
		infoButton.setEnabled(false);
		/*
		infoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextArea info = new JTextArea(5,25);
				int r = JOptionPane.showConfirmDialog(getComponent(),info,"User Information",JOptionPane.OK_CANCEL_OPTION);
				if(r == JOptionPane.OK_OPTION){
					
				}
			}
		});
		*/
		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0,1));
		namePanel.add(experimentLabel);
		namePanel.add(userNameLabel);
		namePanel.add(passwordLabel);
		namePanel.add(informationLabel);
		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridLayout(0,1));
		fieldPanel.add(experimentField);
		fieldPanel.add(usernameField);
		fieldPanel.add(passwordField);
		fieldPanel.add(infoButton);
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);
		
		JPanel pl = new JPanel();
		pl.setLayout(new BorderLayout());
		pl.add(connectionPanel,BorderLayout.CENTER);
		pl.add(instructions,BorderLayout.SOUTH);
		
		// prompt for password
		if(JOptionPane.OK_OPTION == 
		   JOptionPane.showConfirmDialog(getComponent(),pl,"Add New User",
		   JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
			String u = usernameField.getText();
			String p = passwordField.getText();
			
			if(TextHelper.isEmpty(u) || TextHelper.isEmpty(p)){
				JOptionPane.showMessageDialog(getComponent(),"You can't have a blank username or password","Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// see if we need to create a range of users
			Pattern pt = Pattern.compile("(\\w*)(\\d+):(\\d+)(\\w*)");
			Matcher mt = pt.matcher(u);
			if(mt.matches()){
				String prefix =  mt.group(1);
				String start  =  mt.group(2);
				String end    =  mt.group(3);
				String suffix =  mt.group(4);
				
				// by default the password is the same everywhere
				String pprefix = p;
				String psuffix = "";
				
				// check password
				Matcher m2 = pt.matcher(p);
				if(m2.matches()){
					pprefix        =  m2.group(1);
					String pstart  =  m2.group(2);
					String pend    =  m2.group(3);
					psuffix        =  m2.group(4);
					
					// make sure that range is the same
					if(!(start.equals(pstart) && end.equals(pend))){
						JOptionPane.showMessageDialog(getComponent(),
						"Username range: "+start+":"+end+" does not match the one in password: "+pstart+":"+pend,
						"Error",JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				
				
				int st = Integer.parseInt(start);
				int en = Integer.parseInt(end);
				
				// switch numbers if necessary
				if(st > en){
					int x = en;
					en = st;
					st = x;
				}
				
				// add users
				for(int i = st; i<= en; i++){
					String us = prefix+i+suffix;
					String ps = (pprefix.equals(p))?p:pprefix+i+psuffix;
					protocol.addUser(us,ps,experiment.getSelectedValue(),null);
				}
			}else{
				protocol.addUser(u, p,experiment.getSelectedValue(),null);
			}
			user.load(protocol.getUsers(null));
		}
	}
	
	public void doAddExperiment(){
		// create fields
		JTextField experimentField = new JTextField(20);
		
		JTextArea instructions = new JTextArea(3,10);
		instructions.setEditable(false);
		instructions.setWrapStyleWord(true);
		instructions.setLineWrap(true);
		instructions.setBackground(new Color(255,255,200));
		instructions.setText("Type name of new experiment or study. Each study can have a set of users " +
				"and conditions associated with it.");
		
		// create labels
		JLabel  experimentLabel = new JLabel("Experiment:   ", JLabel.RIGHT);
	
				
		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel,BoxLayout.X_AXIS));
		connectionPanel.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0,1));
		namePanel.add(experimentLabel);
		
		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridLayout(0,1));
		fieldPanel.add(experimentField);
	
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(connectionPanel,BorderLayout.CENTER);
		p.add(instructions,BorderLayout.SOUTH);
		
		
		// prompt for password
		if(JOptionPane.OK_OPTION == 
		   JOptionPane.showConfirmDialog(getComponent(),p,"Add New Experiment",
		   JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
			
			String text = experimentField.getText();
			if(TextHelper.isEmpty(text)){
				JOptionPane.showMessageDialog(getComponent(),"You can't have a blank experiment","Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// now add the thing
			protocol.addExperiment(text);
			experiment.load(protocol.getExperiments());
		}
	}
	
	public void doAddCondition(){
		if(experiment.getSelectedValue() == null){
			JOptionPane.showMessageDialog(getComponent(),
			"You need to select an experiment before creating a condition","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		// create fields
		JTextField experimentField = new JTextField(20);
		experimentField.setText(experiment.getSelectedValue());
		experimentField.setEditable(false);
		JTextField conditionField = new JTextField(20);
		
		JTextArea instructions = new JTextArea(3,10);
		instructions.setEditable(false);
		instructions.setWrapStyleWord(true);
		instructions.setLineWrap(true);
		instructions.setBackground(new Color(255,255,200));
		instructions.setText("Type name of new condition for a selected experiment. Each condition will be linked to user's session.");
		
		// create labels
		JLabel  experimentLabel = new JLabel("Experiment:   ", JLabel.RIGHT);
		JLabel  conditionLabel = new JLabel("Condition:   ", JLabel.RIGHT);
		
		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel,BoxLayout.X_AXIS));
		connectionPanel.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0,1));
		namePanel.add(experimentLabel);
		namePanel.add(conditionLabel);
		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridLayout(0,1));
		fieldPanel.add(experimentField);
		fieldPanel.add(conditionField);
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(connectionPanel,BorderLayout.CENTER);
		p.add(instructions,BorderLayout.SOUTH);
		
		
		// prompt for password
		if(JOptionPane.OK_OPTION == 
		   JOptionPane.showConfirmDialog(getComponent(),p,"Add New Condition",
		   JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
			
			String text = conditionField.getText();
			if(TextHelper.isEmpty(text)){
				JOptionPane.showMessageDialog(getComponent(),"You can't have a blank condition","Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// now add the thing
			protocol.addCondition(text,experimentField.getText());
			condition.load(protocol.getConditions(experimentField.getText()));
		}
	}
	public void doRemoveExperiment(){
		if(experiment.getSelectedValues().length == 0){
			JOptionPane.showMessageDialog(getComponent(),
			"You need to make a selection before deleting anything","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		int r = JOptionPane.showConfirmDialog(getComponent(),"Are you sure you would like to delete selected experiments?\n"+
				"Please note that experiments that have data attached to them won't be deleted.",
				"Question",JOptionPane.YES_NO_OPTION);
		if(r == JOptionPane.YES_OPTION){
			for(String s: experiment.getSelectedValues()){
				protocol.removeExperiment(s);
			}
			experiment.load(protocol.getExperiments());
		}
	}
	public void doRemoveCondition(){
		if(condition.getSelectedValues().length == 0){
			JOptionPane.showMessageDialog(getComponent(),
			"You need to make a selection before deleting anything","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		int r = JOptionPane.showConfirmDialog(getComponent(),"Are you sure you would like to delete selected conditions?\n"+
				"Please note that conditions that have data attached to them won't be deleted.",
				"Question",JOptionPane.YES_NO_OPTION);
		if(r == JOptionPane.YES_OPTION){
			for(String s: condition.getSelectedValues()){
				protocol.removeCondition(s,experiment.getSelectedValue());
			}
			experiment.load(protocol.getExperiments());
		}
	}
	public void doRemoveUser(){
		if(user.getSelectedValues().length == 0){
			JOptionPane.showMessageDialog(getComponent(),
			"You need to make a selection before deleting anything","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		int r = JOptionPane.showConfirmDialog(getComponent(),"Are you sure you would like to delete selected users?\n"+
				"Please note that users that have data attached to them won't be deleted.",
				"Question",JOptionPane.YES_NO_OPTION);
		if(r == JOptionPane.YES_OPTION){
			for(String s: user.getSelectedValues()){
				protocol.removeUser(s);
			}
			user.load(protocol.getUsers(experiment.getSelectedValue()));
		}
	}
	
	/**
	 * get menu bar
	 * @return
	 */
	public JMenuBar getMenuBar(){
		if(menubar == null){
			menubar = new JMenuBar();
		}
		return menubar;
	}

	
	/**
	 * user selector
	 * @author tseytlin
	 */
	private class Selector extends JPanel implements ActionListener, ListSelectionListener {
		private JList list;
		private JToolBar toolbar;
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
			toolbar = createToolBar(title);
			add(toolbar,BorderLayout.NORTH);
			add(new JScrollPane(list),BorderLayout.CENTER);
			setPreferredSize(new Dimension(300,300));
		}
		
		/**
		 * create tool bar
		 * @return
		 */
		private JToolBar createToolBar(String title){
			JToolBar toolbar = new JToolBar();
			toolbar.setFloatable(false);
			toolbar.setBackground(Color.white);
			toolbar.add(UIHelper.createButton("Add","Add "+title,Config.getIconProperty("icon.menu.add"),this));
			toolbar.add(UIHelper.createButton("Remove","Remove "+title,Config.getIconProperty("icon.menu.rem"),this));
			toolbar.addSeparator();
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
			// sort data
			Collections.sort(data,new Comparator<String>() {
				public int compare(String o1, String o2) {
					return TextHelper.compare(o1,o2);
				}
			});
			
			final Object [] content = data.toArray();
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
			pressed(name,e.getActionCommand());
		}
		
	}
	
	public JDialog createDialog(Component parent){
		// create a dialog
		if(userManagerDialog == null){
			//JOptionPane op = new JOptionPane(getComponent(),JOptionPane.PLAIN_MESSAGE);
			userManagerDialog = new JDialog(JOptionPane.getFrameForComponent(parent),"User Manager");
			userManagerDialog.getContentPane().setLayout(new BorderLayout());
			userManagerDialog.getContentPane().add(getComponent(),BorderLayout.CENTER);
			userManagerDialog.pack();
			//userManagerDialog = op.createDialog(JOptionPane.getFrameForComponent(parent),"User Manager");
			userManagerDialog.setModal(false);
			userManagerDialog.setResizable(true);
			ImageIcon img = (ImageIcon) UIHelper.getIcon(Config.getProperty("icon.toolbar.user"));
			if(img != null)
				userManagerDialog.setIconImage(img.getImage());
			/*
			userManagerDialog.addWindowListener(new WindowAdapter() {
				public void windowDeactivated(WindowEvent e) {
					// reload
					load();
				}
			});
			*/
		}
		return userManagerDialog;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		UserManager pm = new UserManager();
		pm.setProtocolModule(new DatabaseProtocolModule());
		
		// display
		JFrame frame = new JFrame("User Manager");
		ImageIcon img = (ImageIcon) UIHelper.getIcon(Config.getProperty("icon.general.query"));
		if(img != null)
			frame.setIconImage(img.getImage());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(pm.getMenuBar());
		frame.getContentPane().add(pm.getComponent());
		frame.pack();
		frame.setVisible(true);
		
		// load
		pm.load();

	}

}
