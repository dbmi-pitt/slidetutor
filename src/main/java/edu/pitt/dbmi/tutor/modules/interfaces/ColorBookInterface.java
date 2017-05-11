package edu.pitt.dbmi.tutor.modules.interfaces;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.LinkConceptEntry;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;


public class ColorBookInterface implements InterfaceModule, ActionListener, PropertyChangeListener{
	private Properties defaultConfig;
	private ConceptPanel conceptPanel;
	private JToolBar toolbar;
	private JToggleButton clear;
	private JPopupMenu popup;
	private JMenu menu;
	private ExpertModule expertModule;
	private CaseEntry caseEntry;
	private Tutor tutor;
	private boolean interactive;
	private Map<String,NodeConcept> registry;
	private int fok;
	private Cursor redCursor, greenCursor, yellowCursor;
	private JPanel component;

	
	public void load(){
		//NOOP:
	}
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	/**
	 * get menu for this interface
	 * @return
	 */
	public JMenu getMenu(){
		if(menu == null){
			menu = new JMenu("Interface");
			
			// create toolbar
			menu.add(UIHelper.createMenuItem("sure","Identify concepts you are sure about",
					UIHelper.getIcon(this,"icon.menu.sure",16),this));
			menu.add(UIHelper.createMenuItem("unsure","Identify concepts you are NOT sure about",
					UIHelper.getIcon(this,"icon.menu.unsure",16),this));
			if(Config.getBooleanProperty(this,"behavior.include.error"))
				menu.add(UIHelper.createMenuItem("error","Identify concepts you think might be incorrect",
						UIHelper.getIcon(this,"icon.menu.error",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("Done","Submit your answers and enter normal mode",
					UIHelper.getIcon(this,"icon.menu.submit"),16,this));
		}
		return menu;
	}

	/**
	 * get toolbar
	 * @return
	 */
	public JToolBar getToolBar(){
		return getToolBar(true);
	}
	
	
	
	
	/**
	 * get toolbar
	 * @return
	 */
	private JToolBar getToolBar(boolean horizontal){
		if(toolbar == null){
			toolbar = new UIHelper.ToolBar();
			
			// create toolbar
			ButtonGroup grp = new ButtonGroup();
			JToggleButton bt1 = UIHelper.createToggleButton("sure","Identify concepts you are sure about",
					UIHelper.getIcon(this,"icon.toolbar.sure",24),this);
			JToggleButton bt2 = UIHelper.createToggleButton("unsure","Identify concepts you are NOT sure about",
					UIHelper.getIcon(this,"icon.toolbar.unsure",24),this);
			JToggleButton bt3 = UIHelper.createToggleButton("error","Identify concepts you think might be incorrect",
					UIHelper.getIcon(this,"icon.toolbar.error",24),this);
			clear = new JToggleButton("clear");
			
			toolbar.add(bt1);
			toolbar.add(bt2);
			if(Config.getBooleanProperty(this,"behavior.include.error"))
				toolbar.add(bt3);
			
			grp.add(bt1);
			grp.add(bt2);
			grp.add(bt3);
			grp.add(clear);
			
			toolbar.add(Box.createGlue());
			toolbar.add(UIHelper.createButton("Done","Submit your answers and enter normal mode",
					UIHelper.getIcon(this,"icon.toolbar.submit"),-1,true,this));
			
			// change orientation
			toolbar.setOrientation((horizontal)?JToolBar.HORIZONTAL:JToolBar.VERTICAL);
		}
		return toolbar;
	}


	public void setCaseEntry(CaseEntry problem) {
		caseEntry = problem;
	}

	public void setExpertModule(ExpertModule module) {
		expertModule = module;	
	}

	public Component getComponent() {
		if(component == null){
			component = new JPanel();
			component.setLayout(new BorderLayout());
			component.setBackground(Config.getColorProperty(this,"component.background"));
			component.setOpaque(true);
			
			// initialize registry
			registry = new HashMap<String, NodeConcept>();
			
			conceptPanel  = new ConceptPanel(this);
			conceptPanel.addPropertyChangeListener(this);
			conceptPanel.setMetaColoring(true);
				
			// init cursors
			Toolkit tk = Toolkit.getDefaultToolkit();
			ImageIcon img = null;
			
			img = (ImageIcon) UIHelper.getIcon(this,"icon.cursor.sure");
			greenCursor = tk.createCustomCursor(img.getImage(),new Point(7,0),"green");
			
			img = (ImageIcon) UIHelper.getIcon(this,"icon.cursor.unsure");
			yellowCursor = tk.createCustomCursor(img.getImage(),new Point(7,0),"yellow");
			
			img = (ImageIcon) UIHelper.getIcon(this,"icon.cursor.error");
			redCursor = tk.createCustomCursor(img.getImage(),new Point(7,0),"red");
		      	
			
			// get orientation params
			boolean th = "horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation"));
			
			// add components
			component.add(getToolBar(th),(th)?BorderLayout.NORTH:BorderLayout.WEST);
			component.add(conceptPanel,BorderLayout.CENTER);
				
			// set preferred size
			component.setPreferredSize(Config.getDimensionProperty(this,"component.size"));
		}
		return component;
	}

	public Tutor getTutor() {
		return tutor;
	}

	/**
	 * register concept entry
	 * @param e
	 */
	
	private void registerConceptEntry(ConceptEntry e){
		e.addPropertyChangeListener(this);
		
		// register feature
		NodeConcept node = NodeConcept.getNodeConcept(e,this);
		registry.put(e.getId(),node);
		registry.put(e.getFeature().getId(),node);
		// register attribute
		for(NodeConcept a: node.getAttributes()){
			registry.put(a.getConceptEntry().getId(),a);
		}
		//System.out.println("REGISTER "+registry);
	}
	
	
	/**
	 * unregister concept entry
	 * @param e
	 */
	private void unregisterConceptEntry(ConceptEntry e){
		NodeConcept node = NodeConcept.getNodeConcept(e,this);
		registry.remove(e.getId());
		registry.remove(e.getFeature().getId());
		
		// unregister attribute
		if(node != null){
			for(NodeConcept a: node.getAttributes()){
				registry.remove(a.getConceptEntry().getId());
			}
		}
		
		e.removePropertyChangeListener(this);
		//registrySet.remove(e.getName());
		//System.out.println("UNREGISTER "+registry);
	}
	
	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b){
		if(component != null){
			component.setEnabled(b);
			UIHelper.setEnabled(getToolBar(),b);
			UIHelper.setEnabled(getMenu(),b);
		}
	}
	
	/**
	 * set component interactive flag
	 * @param b
	 */
	public void setInteractive(boolean b){
		interactive = b;
	}
	
	/**
	 * is interactive
	 * @return
	 */
	public boolean isInteractive(){
		return interactive;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}


	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Can be used with the Arc-n-Node Interface to allow users to rate their " +
				"feeling-of-knowing (FOK) for each item they identified.";
	}

	public String getName() {
		return "Coloring Book Interface";
	}
	
	public String getVersion() {
		return "1.0";
	}

	
	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public void dispose() {
		reset();
	}
	
	public Message[] getSupportedMessages() {
		// TODO Auto-generated method stub
		return new Message [0];
	}

	public void receiveMessage(Message msg) {
		// handle the playback messages
		if(msg.getSender() instanceof ProtocolModule){
			if(ACTION_SELF_CHECK.equals(msg.getAction())){
				if(registry.containsKey(msg.getId())){
					ConceptEntry e = registry.get(msg.getId()).getConceptEntry();
					e.setConceptFOKString(msg.getInputMap().get("fok"));
					conceptPanel.repaint();
				}
			}
		}
	}
	
	public void reset() {
		if(component != null){
			conceptPanel.reset();
			registry.clear();
		}
	}

	public void resolveAction(Action action) {
		final Action act = action;
		Operation oper = null;
		
		if(POINTER_ACTION_SET_INTERACTIVE.equalsIgnoreCase(action.getAction())){
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
		}else if(POINTER_ACTION_LOCK_INTERFACE_TO.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				String [] lockInterfaceExceptions;
				public void run() {
					lockInterfaceExceptions = act.getInput().split("\\s*,\\s*");
					UIHelper.setEnabled(getToolBar(), lockInterfaceExceptions,false);
					UIHelper.setEnabled(getMenu(), lockInterfaceExceptions,false);
					UIHelper.setEnabled(getPopupMenu(), lockInterfaceExceptions,false);
				}
				public void undo(){
					lockInterfaceExceptions = null;
					UIHelper.setEnabled(getToolBar(), true);
					UIHelper.setEnabled(getMenu(), true);
					UIHelper.setEnabled(getPopupMenu(), true);
				}
			};
		}
		action.setOperation(oper);
	}

	
	public Action[] getSupportedActions() {
		return new Action []{
				new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_LOCK_INTERFACE_TO,"button1,button2...",
						"Lock down the interface. Disable all buttons and menus with exception to whatever is in the input list."),
				new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"true","Enable interactive mode in a module."),
				new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"false","Disable interactive mode in a module.")	
		};
	}
	

	/**
	 * get menu for this interface
	 * @return
	 */
	public JPopupMenu getPopupMenu(){
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("sure","Sure about Item ",
					UIHelper.getIcon(this,"icon.menu.sure",16),this));
			popup.add(UIHelper.createMenuItem("unsure","Unsure about Item",
					UIHelper.getIcon(this,"icon.menu.unsure",16),this));
			if(Config.getBooleanProperty(this,"behavior.include.error"))
				popup.add(UIHelper.createMenuItem("error","May be Incorrect",
						UIHelper.getIcon(this,"icon.menu.error",16),this));
			
		}
		// unselect buttons
		clear.doClick();
		getComponent().setCursor(Cursor.getDefaultCursor());
		return popup;
	}


	/**
	 * handle actions
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		AbstractButton bt = (AbstractButton) e.getSource();
		
		if(e.getSource() instanceof AbstractButton){
			notifyButton((AbstractButton)e.getSource());
		}
		
		// check if this is submit
		if(cmd.equalsIgnoreCase("Done")){
			fok = ConceptEntry.UNKNOWN;
			component.setCursor(Cursor.getDefaultCursor());
			clear.doClick();
			getComponent().setCursor(Cursor.getDefaultCursor());
			
			// do done
			doSubmit();
			return;
		}
		
		// set feeling-of-knowing
		Cursor cursor = Cursor.getDefaultCursor();
		if(cmd.equalsIgnoreCase("sure")){
			fok = ConceptEntry.SURE; 
			cursor = greenCursor;
		}else if(cmd.equalsIgnoreCase("unsure")){
			fok = ConceptEntry.UNSURE; 
			cursor = yellowCursor;
		}else if(cmd.equalsIgnoreCase("error")){
			fok = ConceptEntry.ERROR; 
			cursor = redCursor;
		}
			
		// set cursor
		if(bt instanceof JToggleButton){
			if(bt.isSelected()){
				component.setCursor(cursor);
			}else {
				fok = ConceptEntry.UNKNOWN;
				component.setCursor(Cursor.getDefaultCursor());
			}
		}
		
		
		// if self check is selected from menu
		doSelfCheck(conceptPanel.getSelectedNode());
				
		// clear popup concept
		conceptPanel.clearPopupSelection();
		
		// clear default fok
		if(e.getSource() instanceof JMenuItem){
			fok = ConceptEntry.UNKNOWN;
		}
	}	
	
	/**
	 * do self check
	 * @param n
	 */
	private void doSelfCheck(NodeConcept n){
		if(n != null && fok != ConceptEntry.UNKNOWN){
			ConceptEntry c = n.getConceptEntry();
			
			// if finding is selcted, then we need to select a feature
			if(c.isFinding())
				c = c.getFeature();
			
			// set FOK
			c.setConceptFOK(fok);
			conceptPanel.repaint();
			
			// send message about this concept
			ClientEvent ce = c.getClientEvent(this,Constants.ACTION_SELF_CHECK);
        	MessageUtils.getInstance(this).flushInterfaceEvents(ce);
        	Communicator.getInstance().sendMessage(ce);
		}
		
	}
	
	private void doSubmit(){
		// check for coloring
		Set<String> cc = getUncoloredConcepts();
        if(!cc.isEmpty()){
            StringBuffer sb = new StringBuffer();
            sb.append("<html>You didn't color all concepts.<br>");
            sb.append("The following concepts were not colored:<ul>");
            for(String c: cc)
            	sb.append("<li>"+c+"</li>");
            sb.append("</ul>");
        	
            MessageUtils.getInstance(this).flushInterfaceEvents(null);
        	JOptionPane.showMessageDialog(Config.getMainFrame(),sb,"Error",JOptionPane.ERROR_MESSAGE);
        }else{
        	ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_DONE,getClass().getSimpleName(),ACTION_REQUEST);
        	MessageUtils.getInstance(this).flushInterfaceEvents(ce);
        	Communicator.getInstance().sendMessage(ce);
        }
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<String> getUncoloredConcepts(){
		// don't bother if don't have to
		if(!Config.getBooleanProperty(this,"behavior.enforce.color"))
			return Collections.EMPTY_SET;
		
		Set<String> toret = new LinkedHashSet<String>();
		for(ConceptEntry e: getConceptEntries()){
			if(e.isFinding())
				e = e.getFeature();
			if(e.getConceptFOK() == ConceptEntry.UNKNOWN)
				toret.add(e.getText());
		}
		
		return toret;
	}
	
	
	public void addConceptEntry(ConceptEntry e) {
		// handle null 
		if(e == null)
			return;
		
		// check for duplicates
		if(registry.containsKey(e.getId()))
			return;
		
		// register it
		e.addPropertyChangeListener(this);
		
		// add it
		NodeConcept c = NodeConcept.createNodeConcept(e,this);
		if(c == null)
			return;
		c.setMetaColoring(true);
				
		conceptPanel.addConcept(c);
		conceptPanel.layoutConcept(c);
		
		
		// register concept
		registerConceptEntry(e);
	}

	
	

	public void removeConceptEntry(ConceptEntry e) {
		// noop
	}
	
	
	
	/**
	 * update concept entries
	 * @param e
	 * @param im
	 */
	
	private void updateConceptEntry(ConceptEntry e, InterfaceModule im){
		//don't update attributes or just features
		if(TYPE_ATTRIBUTE.equals(e.getType()) || (e.isFinding() && !e.equals(e.getParentEntry()))){
			return;
		}
		
		// get concept node from the previous interface module
		NodeConcept n = NodeConcept.getNodeConcept(e,im);
		
		// if we don't have it, then add it
		if(!registry.containsKey(e.getId()))
			addConceptEntry(e);
		
		// update position
		NodeConcept c = NodeConcept.getNodeConcept(e,this);
		if(c != null)
			c.setLocation(n.getLocation());
				
		//repaint
		component.repaint();
	}
	
	

	public void sync(InterfaceModule tm) {
		getComponent().setPreferredSize(tm.getComponent().getSize());
		
		// remove all previous concepts
		reset();
		
		// get all links
		List<ConceptEntry> links = new ArrayList<ConceptEntry>();
		for(ConceptEntry e: tm.getConceptEntries()){
			if(e instanceof LinkConceptEntry)
				links.add(e);
		}
		
		// update concept entries
		for(ConceptEntry e: tm.getConceptEntries()){
			if(!links.contains(e))
				updateConceptEntry(e,tm);
		}
		
		// now add all links
		for(ConceptEntry e: links){
			updateConceptEntry(e,tm);
		}
	}

	public List<ConceptEntry> getConceptEntries() {
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(NodeConcept n: registry.values()){
			list.add(n.getConceptEntry());
		}
		return list;
	}

	
	
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if(PROPERTY_NODE_SELECTED.equals(prop)){
			NodeConcept n = (NodeConcept) evt.getNewValue();
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_NODE,n.getConceptEntry().getId(),ACTION_SELECTED);
			MessageUtils.getInstance(this).addInterfaceEvent(ie);
			doSelfCheck(n);
		}
	}

	/**
	 * Notify button press
	 * @param bt
	 */
	private void notifyButton(AbstractButton bt){
		String type = (bt instanceof JMenuItem)?TYPE_MENU_ITEM:TYPE_BUTTON;
		String label = bt.getText();
		if(TextHelper.isEmpty(label))
			label = bt.getToolTipText();
		//String action = (bt.isSelected() || bt instanceof JMenuItem)?ACTION_SELECTED:ACTION_DESELECTED;
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,type,label,ACTION_SELECTED);
		MessageUtils.getInstance(this).addInterfaceEvent(ie);
	}
	
	
	public void refineConceptEntry(ConceptEntry p, ConceptEntry e) {
		// TODO Auto-generated method stub
		
	}


	public void reconfigure() {
		// TODO Auto-generated method stub
		
	}


	public boolean isEnabled() {
		return (component != null)?component.isEnabled():false;
	}


	public ConceptEntry getConceptEntry(String id) {
		//look up based on id
		NodeConcept e = registry.get(id);
		
		//lookup based on name
		if(e == null){
			for(String key: registry.keySet()){
				if(key.endsWith(id)){
					e = registry.get(key);
					break;
				}
			}
		}
		
		return (e != null)?e.getConceptEntry():null;
	}


}
