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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.ProblemEvent;
import edu.pitt.dbmi.tutor.messages.Query;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.FileSession;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class TransferWizard implements ActionListener {
	private Component main;
	private Selector experiment,condition, user;
	private SessionTable sessions;
	private JProgressBar progress;
	private JPanel statusPanel,transferPanel, logPanel,right;
	private JLabel statusLabel,sessionStatusLine;
	private JTextArea logArea;
	private JButton logReturn;
	private ProtocolSelector protocolSelector;
	private JDialog dialog;
	
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
			
			protocolSelector = new ProtocolSelector();
			protocolSelector.setBorder(new TitledBorder("Target Protocol"));
			right.add(protocolSelector,BorderLayout.NORTH);
			
			// consult button
			JButton transfer = new JButton("Transfer");
			transfer.setBorder(new BevelBorder(BevelBorder.RAISED));
			transfer.setIcon(UIHelper.getIcon(Config.getProperty("icon.toolbar.transfer")));
			transfer.setVerticalTextPosition(SwingConstants.BOTTOM);
			transfer.setHorizontalTextPosition(SwingConstants.CENTER);
			transfer.addActionListener(this);
			transfer.setActionCommand("transfer");
			transfer.setFont(transfer.getFont().deriveFont(Font.BOLD,16f));
			transfer.setPreferredSize(new Dimension(150,150));
			
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
			logReturn = new JButton("do another transfer");
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
			panel.setPreferredSize(new Dimension(900,700));
			
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
	
	/**
	 * selection has been made
	 * @param name
	 */
	private void selected(String name){
		if("experiment".equalsIgnoreCase(name)){
			user.load(protocol.getUsers(experiment.getSelectedValue()));
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
		right.revalidate();
		right.repaint();
		//	}
		//});
	}
	
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("transfer".equals(cmd)){
			doTransfer();
		}else if("return".equals(cmd)){
			showLogPanel(false);
		}
	}
	
	
	/**
	 * do sveral checks and balances :)
	 * @return
	 */
	private boolean isValid(){
		// make sure we have a valid connect first
		ProtocolModule pm = protocolSelector.getProtocolModule();
		if(!pm.isConnected()){
			JOptionPane.showMessageDialog(getComponent(),"Target Protocol is not Valid","Error",JOptionPane.ERROR_MESSAGE);
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
		String target = (pm instanceof DatabaseProtocolModule)?
							pm.getDefaultConfiguration().getProperty("protocol.url"):
							pm.getDefaultConfiguration().getProperty("protocol.directory");
		
		// prompt if user is sure about this transfer
		StringBuffer msg = new StringBuffer();
		msg.append("<html><table width=550><tr><td colspan=2>Are you sure you want to transfer <font color=red>"+n+"</font> sessions ");
		msg.append("for user(s): <font color=green>"+TextHelper.toString(users)+"</font></td></tr>");
		msg.append("<tr><td>from </td><td><font color=blue>"+source+"</font></td></tr>");
		msg.append("<tr><td>to</td><td><font color=blue>"+target+"</font></td></tr></table>");
		int r = JOptionPane.showConfirmDialog(getComponent(),msg,"Transfer",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if(r != JOptionPane.YES_OPTION){
			return false;
		}
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
				public int compare(Object a, Object b) {
					return TextHelper.compare(""+a,""+b);
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
	
	public JDialog createDialog(Component parent){
		// create a dialog
		if(dialog == null){
			// JOptionPane op = new JOptionPane(getComponent(),JOptionPane.PLAIN_MESSAGE);
			dialog = new JDialog(JOptionPane.getFrameForComponent(parent),"Transfer Wizard");
			dialog.getContentPane().setLayout(new BorderLayout());
			dialog.getContentPane().add(getComponent(),BorderLayout.CENTER);
			dialog.pack();
			//dialog = op.createDialog(JOptionPane.getFrameForComponent(parent),"Transfer Wizard");
			//dialog.setModal(false);
			dialog.setResizable(true);
			ImageIcon img = (ImageIcon) UIHelper.getIcon(Config.getProperty("icon.toolbar.transfer"));
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
		
	}
	
	/**
	 * this is where magic happens
	 */
	private void doTransfer(){
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
					final int n = input.size();
					
					ProtocolModule pm = protocolSelector.getProtocolModule();
					
					// setup  progress bar
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							progress.setMinimum(0);
							progress.setMaximum(n);
							progress.setIndeterminate(false);
						}
					});
					
					
					//now do actual work
					String experiment = null;
					int x = 0;
					for(Session s: input){
						log("Copying "+s.getUsername()+" "+
							 OntologyHelper.getCaseName(s.getCase())+" session "+
							 s.getSessionID()+" ..\n");
						
						// check if experiment is available in file session
						if(TextHelper.isEmpty(s.getExperiment()) && s instanceof FileSession){
							// if not available either prompt or check
							if(experiment == null){
								//check if user is already in target database
								if(pm.getUsers(null).contains(s.getUsername())){
									experiment = pm.getUserInfo(s.getUsername()).getProperty("experiment");
								}else{
									experiment = JOptionPane.showInputDialog(dialog,"please provide the experiment name");
								}
							}
							
							// else just set previous value	
							if(experiment != null){
								((FileSession)s).setExperiment(experiment);
							}
						}
						
						// copy a single session
						copy(s,pm);
						
						// clear memory
						s.refresh();
						
						// update
						//progress.setValue(x++);
						final int y = x++;
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								progress.setValue(y);
							}
						});
					}
					log("\nAll Done!");
					
					
					logReturn.setEnabled(true);
					// disconnect from the target protocol
					pm.dispose();
					setBusy(false);
				}
			}))).start();
			
		}
	}
	
		
	/**
	 * copy a single session to a protocol module
	 * @param s
	 * @param target
	 */
	public void copy(Session s, ProtocolModule target){
		// make sure that we have experiment, condition and user
		ProtocolModule source = getProtocolModule();
		
		String experiment = s.getExperiment();
		String condition  = s.getCondition();
		String user       = s.getUsername();
		
		// check experiment
		if(!target.getExperiments().contains(experiment) && experiment != null){
			target.addExperiment(experiment);
		}
		
		// check username (for any experiment, that is why parameter is null)
		if(!target.getUsers(null).contains(user)){
			Properties p = (source != null)?source.getUserInfo(user):null;
			String password = (source != null)?p.getProperty("password"):s.getUsername();
			target.addUser(s.getUsername(),password,experiment,p);
		}else if(experiment == null){
			// if experiment is not provided, but user exists
			experiment = target.getUserInfo(user).getProperty("experiment");
		}
		
		// check condition
		if(!target.getConditions(experiment).contains(condition) && experiment != null){
			target.addCondition(condition,experiment);
		}
		
		// get problem events that represent start & end of the problem
		List<ProblemEvent> pes = s.getProblemEvents();
		if(pes.size() != 2)
			return;
		
		// now start session
		target.openCaseSession(pes.get(0));
		
		// process all messages
		for(Message msg: getMessages(s)){
			target.processMessage(msg);
		}
		
		// close session
		target.closeCaseSession(pes.get(1));
	}
		
	/**
	 * get client events and interface events sorted by timestamp
	 * @param s
	 * @return
	 */
	private List<Message> getMessages(Session s){
		List<Message> messages = new ArrayList<Message>();
		messages.addAll(s.getInterfaceEvents());
		messages.addAll(s.getClientEvents());
		messages.addAll(s.getTutorResponses());
		messages.addAll(s.getNodeEvents());
		
		// sort by date
		Collections.sort(messages,new Message.TimeComparator());
		
		return messages;
	}
	
	
	public static void main(String [] args){
		TransferWizard wizard = new TransferWizard();
		wizard.setProtocolModule(new DatabaseProtocolModule());
		wizard.load();
		JDialog d = wizard.createDialog(null);
		d.setModal(true);
		d.setVisible(true);
		System.exit(0);
		
	}
}
