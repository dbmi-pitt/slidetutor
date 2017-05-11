package edu.pitt.dbmi.tutor.builder.behavior;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.builder.config.Names;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.GenericTutor;
import edu.pitt.dbmi.tutor.util.*;


public class BehaviorBuilder implements ActionListener, ItemListener {
	private Map<String,Set<String>> defaultActions, defaultActionInputs;
	private Set<String> defaultActionReceivers;
	private Set<String> defaultConditions;
	private Set<String> defaultOperations;
	private Map<String,Set<String>> defaultConditionInputs;
	private Map<String,Action> actionMap;
	
	private JComboBox receivers,actions,input;
	private JComboBox condition,operation,parameter;
	private Component component;
	private JLabel description;
	private JToolBar toolbar;
	private JList taskList,actionList,conditionList;
	private List<Task> tasks;
	private UIHelper.ListModel taskModel;
	private Task task;
	private File file;
	
	public BehaviorBuilder(){
		tasks = new ArrayList<Task>();
	}
	
	/**
	 * get component 
	 * @return
	 */
	public Component getComponent(){
		if(component == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			
			taskModel = new UIHelper.ListModel(tasks);
			taskList = new JList(taskModel);
			//taskList.setDragEnabled(true);
			taskList.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if(value instanceof Task){
						Task t = (Task) value;
						StringBuffer c = new StringBuffer();
						StringBuffer a = new StringBuffer();
						for(Condition o: t.getConditions())
							c.append(((c.length()==0)?"":" <b>and</b><br>")+o);
						for(Action o: t.getActions())
							a.append(((a.length()==0)?"":"<br>")+o);
						//String c = TextHelper.toString(t.getConditions()).replaceAll(","," <b>and</b><br>");
						//String a = TextHelper.toString(t.getActions()).replaceAll(",","<br>");
						lbl.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),new LineBorder(Color.blue)));
						lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
						lbl.setText("<html><table width=750><tr valign=top><td><b>IF</b></td><td width=\"100%\">"+c+"</td></tr>"+
									"<tr valign=top><td><b>THEN</b></td><td></td></tr>" +
									"<tr valign=top><td></td><td>"+a+"</td></tr></table></html>");
					}
					return lbl;
				}
				
			});
			taskList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if(e.getClickCount() == 2){
						Task t = (Task) taskList.getSelectedValue();
						if(t != null)
							doEdit(t);
					}
				}
				
			});
			//task
			JScrollPane s = new JScrollPane(taskList);
			s.setPreferredSize(new Dimension(800,600));
			
			JPanel p3 = new JPanel();
			p3.setLayout(new BorderLayout());
			p3.add(createToolBar("Task List","task"),BorderLayout.NORTH);
			p3.add(s,BorderLayout.CENTER);
			
			//
			panel.add(getToolBar(),BorderLayout.NORTH);
			panel.add(p3,BorderLayout.CENTER);
			component = panel;
		}
		return component;
	}
	
	/**
	 * get scenario set for a given domain
	 * @return
	 */
	private List<Task> loadTasks(String location){
		List<Task> tasks = new ArrayList<Task>();
		if(location.length() > 0){
			try{
				File f = new File(location);
				if(f.exists()){
					tasks = loadTasks(new FileInputStream(f));
					file = f;
				}else if(UIHelper.isURL(location)){
					URL url = new URL(location);
					tasks = loadTasks(url.openStream());
				}else {
					tasks = loadTasks(getClass().getResourceAsStream(location));
				}
			}catch(Exception ex){
				Config.getLogger().severe("Could not load help file from "+location);
				ex.printStackTrace();
			}
		}
		return tasks;
	}
	
	/**
	 * load tasks
	 * @param location
	 * @return
	 * @throws Exception
	 */
	private  List<Task> loadTasks(InputStream in) throws Exception{
		List<Task> tasks = new ArrayList<Task>();
		try {
			Document document = UIHelper.parseXML(in);
			
			//print out some useful info
			Element element = document.getDocumentElement();
			
			// iterate through tasts
			NodeList list = element.getElementsByTagName("Task");
			for(int i=0;i<list.getLength();i++){
				Node node = list.item(i);
				if(node instanceof Element){
					Element e = (Element) node;
					// load condition
					Element cond = UIHelper.getElementByTagName(e,"Condition");
					Condition condition = new Condition();
					condition.parseElement(cond);
					if(CONDITION_EXPRESSION.equals(condition.getCondition())){
						condition = new ExpressionCondition();
						condition.parseElement(cond);
					}
					
					// iterate through actions
					List<Action> actions = new ArrayList<Action>();
					NodeList alist = e.getElementsByTagName("Action");
					for(int j=0;j<alist.getLength();j++){
						Node anode = alist.item(j);
						if(anode instanceof Element){
							Action action = new Action();
							action.parseElement((Element) anode);
							actions.add(action);
						}
					}
					
					// save task			
					tasks.add(new Task(condition,actions));
				}
			}
			
		} catch (Exception ex) {
			throw new IOException(ex.getMessage());
		}
		
		return tasks;
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
			toolbar.add(UIHelper.createButton("new","Create New Behavior File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.new"),24),this));
			toolbar.add(UIHelper.createButton("open","Open Behavior File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.open"),24),this));
			toolbar.add(UIHelper.createButton("save","Save Behavior File",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.save"),24),this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("save.as","Save Behavior File As",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.save.as"),24),this));
			
		}
		return toolbar;
	}
	
	/**
	 * edit tasks
	 * @param t
	 */
	private boolean doEdit(Task t){
		task = t;
		
		// create edit panel
		conditionList = new JList();
		conditionList.setModel(new UIHelper.ListModel(t.getConditions()));
		JScrollPane cs = new JScrollPane(conditionList);
		cs.setPreferredSize(new Dimension(400,75));
		actionList = new JList();
		actionList.setModel(new UIHelper.ListModel(t.getActions()));
		JScrollPane as = new JScrollPane(actionList);
		as.setPreferredSize(new Dimension(400,75));
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(createToolBar("Actions","action"),BorderLayout.NORTH);
		p.add(as,BorderLayout.CENTER);
	
		
		JPanel p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(createToolBar("Conditions","condition"),BorderLayout.NORTH);
		p1.add(cs,BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(p1,BorderLayout.NORTH);
		panel.add(p,BorderLayout.SOUTH);
		
		
		boolean ret = false;
		int r = JOptionPane.showConfirmDialog(getComponent(),panel,"Edit Task",
		JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			ret = true;
		}
		
		actionList = null;
		conditionList = null;
		return ret;
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
		
		description = new JLabel(Names.getDescription("&nbsp;<br>&nbsp;<br>&nbsp;<br>&nbsp;",450,350));
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
	private boolean doEditCondition(Condition e){
		loadDefaultConditions();
		
		// create panel
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		//panel.setPreferredSize(new Dimension(500,75));
		
		condition = new JComboBox(defaultConditions.toArray());
		condition.setBorder(new TitledBorder("Condition"));
		condition.setSelectedItem(e.getCondition());
		condition.setEditable(true);
		condition.addItemListener(this);
		panel.add(condition);
		
		operation = new JComboBox(defaultOperations.toArray());
		operation.setBorder(new TitledBorder("Operation"));
		operation.setSelectedItem(e.getOperation());
		operation.setEditable(true);
		operation.addItemListener(this);
		panel.add(operation);
		
		parameter = new JComboBox();
		parameter.setBorder(new TitledBorder("Parameter"));
		parameter.setSelectedItem(e.getInput());
		parameter.setEditable(true);
		parameter.addItemListener(this);
		panel.add(parameter);
		
		
		description = new JLabel(Names.getDescription("&nbsp;<br>&nbsp;<br>&nbsp;<br>&nbsp;",450,350));
		description.setHorizontalAlignment(JLabel.CENTER);
			
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(-1,1));
		p.add(panel);
		p.add(description);
		
		
		int r = JOptionPane.showConfirmDialog(getComponent(),p,"Edit Condition",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			e.setCondition(""+condition.getSelectedItem());
			e.setOperation(""+operation.getSelectedItem());
			e.setInput(""+parameter.getSelectedItem());
			return true;
		}
		return false;
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
	private void loadDefaultConditions(){
		defaultConditions = new LinkedHashSet<String>();
		defaultConditions.add(CONDITION_CASE_START);
		defaultConditions.add(CONDITION_CASE_FINISH);
		defaultConditions.add(CONDITION_USER_TYPE_COUNT);
		defaultConditions.add(CONDITION_USER_ACTION_COUNT);
		defaultConditions.add(CONDITION_CASE_TIME);
		defaultConditions.add(CONDITION_TOTAL_TIME);
		defaultConditions.add(CONDITION_CASE_COUNT);
		defaultConditions.add(CONDITION_USER_ACTION);
		defaultConditions.add(CONDITION_TUTOR_RESPONSE);
		
		defaultOperations = new LinkedHashSet<String>();
		defaultOperations.add(OPERATION_EQUALS);
		defaultOperations.add(OPERATION_NOT_EQUALS);
		defaultOperations.add(OPERATION_GREATER_THEN);
		defaultOperations.add(OPERATION_LESS_THEN);
		defaultOperations.add(OPERATION_INTERVAL);
		
		defaultConditionInputs = new HashMap<String, Set<String>>();
		defaultConditionInputs.put(CONDITION_USER_TYPE_COUNT,Collections.singleton("type:<count>"));
		defaultConditionInputs.put(CONDITION_CASE_COUNT,Collections.singleton("<count>"));
		defaultConditionInputs.put(CONDITION_USER_ACTION_COUNT,Collections.singleton("<count>"));
		defaultConditionInputs.put(OPERATION_INTERVAL,new TreeSet<String>(Arrays.asList("(<period>)","(<period>,<offset>)")));
		defaultConditionInputs.put(CONDITION_CASE_TIME,Collections.singleton("<minutes>"));
		defaultConditionInputs.put(CONDITION_TOTAL_TIME,Collections.singleton("<minutes>"));
		defaultConditionInputs.put(CONDITION_USER_ACTION,new TreeSet<String>(Arrays.asList("type:<type>","label:<label>")));
		defaultConditionInputs.put(CONDITION_TUTOR_RESPONSE,new TreeSet<String>(Arrays.asList(RESPONSE_CONFIRM,RESPONSE_FAILURE,RESPONSE_HINT)));
	}
	
	public void doNew(){
		tasks.clear();
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				taskModel.sync(taskList);
			}
		});
	}
	
	private void doOpen(){
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(file);
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
			load(file.getAbsolutePath());
		}
	}
	
	/**
	 * load behavior from location
	 * @param location
	 */
	public void load(String location){
		tasks.clear();
		tasks.addAll(loadTasks(location));
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(taskModel != null)
					taskModel.sync(taskList);
			}
		});
	}
	
	public void doSave(){
		if(file != null){
			try{
				save(new FileOutputStream(file));
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(getComponent(),
						"Could not save help file "+file.getAbsolutePath(),
						"Error",JOptionPane.ERROR_MESSAGE);
			}
		}else{
			doSaveAs();
		}
	}
	
	private void doSaveAs(){
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(file);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".xml");
			}
			public String getDescription() {
				return "Behavior XML File (.xml)";
			}
		});
		int r = chooser.showSaveDialog(getComponent());
		if(r == JFileChooser.APPROVE_OPTION){
			file = chooser.getSelectedFile();
			if(!file.getName().endsWith(".xml"))
				file = new File(file.getParentFile(),file.getName()+".xml");
			try{
				save(new FileOutputStream(file));
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(getComponent(),
						"Could not save help file "+file.getAbsolutePath(),
						"Error",JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	/**
	 * save tasks
	 * @param os
	 * @throws Exception
	 */
	private void save(OutputStream os) throws Exception {
		// initialize document and root
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().newDocument();
		
		// create DOM object
		doc.appendChild(createElement(doc));
		
		// write out XML
		UIHelper.writeXML(doc, os);
		
	}
	
	/**
	 * get DOM element that represents this object
	 * @return
	 */
	private Element createElement(Document doc){
		Element root = doc.createElement("Behavior");
		
		// write out each scenario
		for(Task e: tasks){
			root.appendChild(e.createElement(doc));
		}
		
		return root;
	}
	
	
	public void actionPerformed(ActionEvent e) {
		if("new".equals(e.getActionCommand())){
			doNew();
		}else if("open".equals(e.getActionCommand())){
			doOpen();
		}else if("save".equals(e.getActionCommand())){
			doSave();
		}else if("save.as".equals(e.getActionCommand())){
			doSaveAs();
		}else if("add-task".equals(e.getActionCommand())){
			if(tasks != null){
				Task t = new Task();
				if(doEdit(t)){
					tasks.add(t);
					sync(taskList);
				}
			}
		}else if("remove-task".equals(e.getActionCommand())){
			if(tasks != null){
				for(Object o: taskList.getSelectedValues()){
					tasks.remove(o);
				}
				sync(taskList);
			}
		}else if("add-action".equals(e.getActionCommand())){
			Action action = new Action();
			if(doEditAction(action)){
				if(task != null){
					task.getActions().add(action);
					sync(actionList);
				}
			}
		}else if("remove-action".equals(e.getActionCommand())){
			if(actionList != null && task != null){
				for(Object o: actionList.getSelectedValues()){
					task.getActions().remove(o);
				}
				sync(actionList);
			}
		}else if("add-condition".equals(e.getActionCommand())){
			Condition condition = new Condition();
			if(doEditCondition(condition)){
				if(task != null){
					task.getConditions().add(condition);
					sync(conditionList);
				}
			}
		}else if("remove-condition".equals(e.getActionCommand())){
			if(conditionList != null && task != null){
				for(Object o: conditionList.getSelectedValues()){
					task.getConditions().remove(o);
				}
				sync(conditionList);
			}
		}
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
	
	

	public void itemStateChanged(ItemEvent e) {
		if(e.getStateChange() == ItemEvent.SELECTED){
			if(e.getSource() == receivers){
				actions.removeAllItems();
				//actions.addItem("");
				input.removeAllItems();
				String r = ""+receivers.getSelectedItem();
				if(defaultActions.containsKey(r)){
					for(String a: defaultActions.get(r)){
						actions.addItem(a);
					}
				}
				// display help				
				if(actionMap != null){
					Action a = actionMap.get(r+"."+actions.getSelectedItem()+"."+input.getSelectedItem());
					if(a != null){
						description.setText(Names.getDescription(a.getDescription(),450,300));
					}
				}
				
			
			}else if(e.getSource() == actions){
				input.removeAllItems();
				//input.addItem("");
				String r = ""+actions.getSelectedItem();
				if(defaultActionInputs.containsKey(r)){
					for(String a: defaultActionInputs.get(r)){
						input.addItem(a);
					}
				}
					
				// display help				
				if(actionMap != null){
					Action a = actionMap.get(receivers.getSelectedItem()+"."+r+"."+input.getSelectedItem());
					if(a != null){
						description.setText(Names.getDescription(a.getDescription(),450,300));
					}
				}
			}else if(e.getSource() == input){
				// display help				
				if(actionMap != null){
					Action a = actionMap.get(receivers.getSelectedItem()+"."+actions.getSelectedItem()+"."+input.getSelectedItem());
					if(a != null){
						description.setText(Names.getDescription(a.getDescription(),450,300));
					}
				}

			}else if(e.getSource() == condition){
				String c = ""+condition.getSelectedItem();
				if(Arrays.asList(CONDITION_CASE_START,CONDITION_CASE_FINISH).contains(c)){
					operation.setEnabled(false);
					parameter.setEnabled(false);
				}else{
					operation.setEnabled(true);
					parameter.setEnabled(true);
				}
				
				parameter.removeAllItems();
				if(defaultConditionInputs.containsKey(c)){
					for(String a: defaultConditionInputs.get(c)){
						parameter.addItem(a);
					}
				}
				
				description.setText(Names.getDescription(new Condition(c,null,null).getDescription(),450,300));				
			}else if(e.getSource() == operation){
				String c = ""+operation.getSelectedItem();
				parameter.removeAllItems();
				if(defaultConditionInputs.containsKey(c)){
					for(String a: defaultConditionInputs.get(c)){
						parameter.addItem(a);
					}
				}else{
					for(String a: defaultConditionInputs.get(condition.getSelectedItem())){
						parameter.addItem(a);
					}
				}
			}
		
				
			// disable the interface
			if(input != null)
				input.setEnabled(!(input.getModel().getSize() == 0 || (""+input.getModel().getElementAt(0)).length() == 0));
		
			
			// resize
			Window w = UIHelper.getWindow(input);
			if(w != null)
				w.pack();
			
		}
	}
	
	public File getFile(){
		return file;
	}
	
	public void setFile(File f){
		file = f;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BehaviorBuilder hb = new BehaviorBuilder();
		
		// display
		JFrame frame = new JFrame("Behavior Builder");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(hb.getComponent());
		frame.pack();
		frame.setVisible(true);
		
		// load file
		/*
		if(args.length > 0){
			File f = new File(args[0]);
			if(f.exists())
				hb.load(f);
		}else{
			hb.load(HelpBuilder.class.getResourceAsStream(OntologyHelper.DEFAULT_TUTOR_HELP_FILE));
		}
		*/

	}

}
