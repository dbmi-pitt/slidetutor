package edu.pitt.dbmi.tutor.modules.interfaces;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ConceptFilter;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.ui.TreeDialog;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class QuestionInterface implements InterfaceModule, ActionListener, PropertyChangeListener, TreeSelectionListener, TreeExpansionListener {
	private Properties defaultConfig;
	private JPanel component,dPanel;
	private boolean interactive;
	private ExpertModule expertModule;
	private Tutor tutor;
	private CaseEntry problem;
	
	// interface specific
	private JTextArea answerText;
	private SingleConceptPanel answerPanel;
	private ButtonGroup answerGroup;
	private TreeDialog findingDialog;
	private NodeConcept answerFindig;
	private Object pointObject;
	
	
	public Component getComponent() {
		if(component == null){
			component = new JPanel();
			component.setLayout(new BorderLayout());
			component.setBackground(Color.white);
			//component.setPreferredSize(Config.getDimensionProperty(this,"component.size"));
			component.setBorder(new LineBorder(Color.blue,2));
		}
		return component;
	}
	
	
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
	 * get an answer from this interface
	 * @return
	 */
	public String getAnswer(){
		String type   = getType();
		if(QUESTION_TYPE_FREE_TEXT.equals(type) && answerText != null){
			return answerText.getText().trim();
		}else if(QUESTION_TYPE_MULTIPLE_CHOICE.equals(type) && answerGroup != null){
			if(answerGroup.getSelection() != null)
				return answerGroup.getSelection().getActionCommand();
		}else if(QUESTION_TYPE_POINT_OUT.equals(type)){
			return (pointObject != null)?pointObject.toString():"";
		}
		return "";
	}
	
	public void notifyAnswer(){
		String text = getAnswer();
		if(text.length() > 0){
			ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_ANSWER,text,ACTION_SUBMIT);
			Communicator.getInstance().sendMessage(ce);
		}
	}
	
	private String getType(){
		String type   = problem.getProperties().getProperty("question.type");
		if(type.indexOf("+") > -1)
			type = type.substring(0,type.indexOf("+"));
		return type;
	}
	
	
	public void setCaseEntry(CaseEntry problem) {
		getComponent();
		reset();
		this.problem = problem;
		
		// customize interface based on question type
		String type   = getType();
		
		if(QUESTION_TYPE_IDENTIFY.equals(type)){
			JPanel p = new JPanel();
			p.setBackground(Color.white);
			p.setLayout(new FlowLayout());
			p.add(UIHelper.createButton("Identify Finding","Identify the most specific finding",
				  UIHelper.getIcon(this,"icon.finding"),-1, true, this),BorderLayout.CENTER);
			component.add(p,BorderLayout.CENTER);
			answerPanel = new SingleConceptPanel();
			answerPanel.setOpaque(false);
			component.add(getLabel("Click Button Below to Select Correct Finding"),BorderLayout.NORTH);
			component.add(answerPanel,BorderLayout.SOUTH);
		}else if(QUESTION_TYPE_DIFFERENTIATE.equals(type)){
			JPanel p = new JPanel();
			p.setBackground(Color.white);
			p.setLayout(new FlowLayout());
			p.add(UIHelper.createButton("Identify Finding","Identify the most specific finding",
				  UIHelper.getIcon(this,"icon.finding"),-1, true, this),BorderLayout.CENTER);
			component.add(p,BorderLayout.CENTER);
			answerPanel = new SingleConceptPanel();
			answerPanel.setOpaque(false);
			component.add(getLabel("Click Button Below to Select Correct Finding"),BorderLayout.NORTH);
			component.add(answerPanel,BorderLayout.SOUTH);
		}else if(QUESTION_TYPE_POINT_OUT.equals(type)){
			JPanel p = new JPanel();
			p.setBackground(Color.white);
			p.setLayout(new FlowLayout());
			p.add(UIHelper.createButton("Point to Finding","Point to a finding on a digital slide",
				  UIHelper.getIcon(this,"icon.cross"),-1, true, this),BorderLayout.CENTER);
			component.add(getLabel("Click Button Below to Point on a Digital Slide"),BorderLayout.NORTH);
			component.add(p,BorderLayout.CENTER);
		}else if(QUESTION_TYPE_FREE_TEXT.equals(type)){
			answerText = new JTextArea();
			answerText.setLineWrap(true);
			answerText.setWrapStyleWord(true);
			answerText.requestFocus();
			JScrollPane scroll = new JScrollPane(answerText);
			scroll.setBorder(new LineBorder(Color.gray,5));
			component.add(getLabel("Type Your Answer Below"),BorderLayout.NORTH);
			component.add(scroll,BorderLayout.CENTER);
		}else if(QUESTION_TYPE_MULTIPLE_CHOICE.equals(type)){
			String [] answers = TextHelper.parseList(problem.getProperties().getProperty("answers"));
			if(answers.length == 0)
				answers = new String [] {"true","false"};
			answerGroup = new ButtonGroup();
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
			for(String answer: answers){
				JRadioButton bt = new JRadioButton(answer);
				bt.setOpaque(false);
				bt.setActionCommand(answer);
				answerGroup.add(bt);
				p.add(bt);
			}
			component.add(getLabel("Select an Appropriate Answer Below"),BorderLayout.NORTH);
			component.add(new JLabel("         "),BorderLayout.WEST);
			component.add(p,BorderLayout.CENTER);
		}
		component.revalidate();
		component.repaint();
	}

	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		// notify button press
		if(e.getSource() instanceof AbstractButton){
			notifyButton((AbstractButton)e.getSource());
		}
		
		if("Identify Finding".equals(cmd)){
			doIdentifyFinding();
		}else if("Point to Finding".equals(cmd)){
			doPointToFinding();
		}
		
	}
	
	/**
	 * add new concept
	 */
	private void doIdentifyFinding(){
		TreeDialog d = getFindingDialog();
		d.setVisible(true);
		TreePath name = d.getSelectedNode();
		if(name != null){
			if(answerFindig != null)
				removeConceptEntry(answerFindig.getConceptEntry());
			addConceptEntry(new ConceptEntry(name,TYPE_FINDING));
		}
		
		if(d.getSelectedNodes().length == 0)
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
	}
	
	/**
	 * add new concept
	 */
	private void doPointToFinding(){
		// start the drawing
		(new Thread(new Runnable(){
			public void run(){
				PresentationModule viewer = getTutor().getPresentationModule();
				viewer.startFeatureIdentification();
				
				while(!viewer.isFeatureIdentified()){
					UIHelper.sleep(200);
				}
				Object input = viewer.getIdentifiedFeature();
				viewer.stopFeatureIdentification();
				
				// if input is null and button unselected
				// means that action was canceled
				if(input == null)
					return;
								
				// send message about a single concept on the slide
				ConceptEntry e = new ConceptEntry(OntologyHelper.CONCEPTS,TYPE_FINDING);
				if(!problem.getConcepts(OntologyHelper.DIAGNOSTIC_FEATURES).isEmpty()){
					e = problem.getConcepts(OntologyHelper.DIAGNOSTIC_FEATURES).getValues().get(0);
				}
				e.setInput(input);
				pointObject = input;
				
				// create Interface event for this action
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(QuestionInterface.this,TYPE_PRESENTATION,e.getName(),ACTION_IDENTIFY);
				ie.setInput(e.getMessageInput());
				MessageUtils.getInstance(QuestionInterface.this).addInterfaceEvent(ie);
				
				// create client event
				ClientEvent ce = e.getClientEvent(QuestionInterface.this,Constants.ACTION_REFINE);
				MessageUtils.getInstance(QuestionInterface.this).flushInterfaceEvents(ce);
				Communicator.getInstance().sendMessage(ce);
			}
		})).start();
			
		
	}
	
	
	/**
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getFindingDialog(){
		if(findingDialog == null){
			// fetch only feature findings, if in attribute mode
			ConceptFilter featureFilter = null;
			if("feature".equalsIgnoreCase(Config.getProperty(this,"behavior.finding.tree.mode")))
				featureFilter = new OntologyHelper.FeatureFilter(expertModule.getDomainOntology());
			else
				featureFilter = new OntologyHelper.FindingFilter(expertModule.getDomainOntology());
			
			
			findingDialog =  TreeDialog.createDialog(UIHelper.getWindow(component));
			findingDialog.getTreePanel().addTreeSelectionListener(this);
			findingDialog.getTreePanel().addTreeExpansionListener(this);
			findingDialog.getTreePanel().addPropertyChangeListener(this);
			findingDialog.setTitle("FINDINGS");
			findingDialog.setSelectionMode(TreeDialog.SINGLE_SELECTION);
			findingDialog.setRoot(expertModule.getTreeRoot(OntologyHelper.DIAGNOSTIC_FEATURES,featureFilter));
		}
		return findingDialog;
	}
	
	/**
	 * tree selection listener
	 */
	public void valueChanged(TreeSelectionEvent e) {
		// notify tree selection
		TreePath p = e.getPath();
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_TREE,TextHelper.toString(p),
				(e.getNewLeadSelectionPath() != null)?ACTION_SELECTED:ACTION_DESELECTED);
		ie.setParent(getFindingDialog().getTitle());
		MessageUtils.getInstance(this).addInterfaceEvent(ie);
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		String cmd = evt.getPropertyName();
		if(edu.pitt.slideviewer.Constants.UPDATE_SHAPE.equals(cmd)){
			ConceptEntry e = (ConceptEntry) evt.getNewValue();
			
			// create Interface event for this action
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_PRESENTATION,e.getName(),ACTION_IDENTIFY);
			ie.setInput(e.getMessageInput());
			MessageUtils.getInstance(this).addInterfaceEvent(ie);
			
			// send message about this concept
			ClientEvent ce = e.getClientEvent(this,Constants.ACTION_REFINE);
			MessageUtils.getInstance(this).flushInterfaceEvents(ce);
			Communicator.getInstance().sendMessage(ce);
		}else if("SEARCH".equals(cmd) || "SEARCH_GO".equals(cmd)){
			String s = ""+evt.getNewValue();
			if(s.length() > 0){
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_TREE,""+evt.getNewValue(),
									"SEARCH".equals(cmd)?ACTION_SEARCHING:ACTION_SEARCH);
				ie.setParent(findingDialog.getTitle());
				MessageUtils.getInstance(this).addInterfaceEvent(ie);
			}
		}
	}
	
	/**
	 * get component that represents a label
	 * @param text
	 * @return
	 */
	private Component getLabel(String text){
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		p.setOpaque(false);
		JLabel l = new JLabel("<html><center><h3>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+
				 		text+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</h3><hr><center>");
		l.setHorizontalTextPosition(JLabel.CENTER);
		l.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		p.add(l);
		return p;
	}
	
	
	public void sync(InterfaceModule tm) {
		// set case entry from  the interface
		setCaseEntry(tutor.getCase());

	}
	
	public void reconfigure() {
		// TODO Auto-generated method stub

	}
	
	
	/**
	 * get a panel that pictorially represents
	 * differentiate question
	 * @return
	 */
	public JPanel getDifferentiateDiagram(){
		if(dPanel == null){
			// setup node concepts
			List<ConceptEntry> dxs = problem.getConcepts(OntologyHelper.DIAGNOSES).getValues();
			ConceptEntry h1 = new ConceptEntry((dxs.size() > 0)?dxs.get(0).getName():"Diagnosis 1",TYPE_HYPOTHESIS);
			ConceptEntry h2 = new ConceptEntry((dxs.size() > 1)?dxs.get(1).getName():"Diagnosis 2",TYPE_HYPOTHESIS);
		
			// setup all concepts
			final NodeConcept d1 = NodeConcept.createNodeConcept(h1,this);
			final NodeConcept d2 = NodeConcept.createNodeConcept(h2,this);
			final NodeConcept f0 = NodeConcept.createNodeConcept(new ConceptEntry("Finding",TYPE_FINDING),this);
			final NodeConcept.SupportLinkConcept l1 = (NodeConcept.SupportLinkConcept) NodeConcept.createSupportLinkConcept(f0,d1,this);
			final NodeConcept.SupportLinkConcept l2 = (NodeConcept.SupportLinkConcept) NodeConcept.createSupportLinkConcept(f0,d2,this);
			h1.setConceptStatus(ConceptEntry.CORRECT);
			h2.setConceptStatus(ConceptEntry.CORRECT);
			l1.getConceptEntry().setConceptStatus(ConceptEntry.CORRECT);
			l2.getConceptEntry().setConceptStatus(ConceptEntry.INCORRECT);
			f0.getConceptEntry().setConceptStatus(ConceptEntry.CORRECT);
			
			// setup panel
			dPanel = new JPanel(){
				public void paintComponent(Graphics g){
					super.paintComponent(g);
					final Dimension gap = new Dimension(300,100);
					Rectangle or = getBounds();
					
					Rectangle d1r = d1.getBounds();
					
					d1.setLocation(new Point((int)(or.getWidth()/2-gap.getWidth()/2-d1r.width),(int)(or.getHeight()/2+gap.getHeight()/2)));
					d2.setLocation(new Point((int)(or.getWidth()/2+gap.getWidth()/2),(int)(or.getHeight()/2+gap.getHeight()/2)));
		
					// this all depends on position
					if(answerFindig != null){
						NodeConcept c = NodeConcept.createNodeConcept(answerFindig.getConceptEntry(),QuestionInterface.this);
						l1.setSource(c);
						l2.setSource(c);
						Rectangle f0r = c.getBounds();
						c.setLocation(new Point((int)(or.getWidth()/2-f0r.getWidth()/2),(int)(or.getHeight()/2-gap.getHeight()/2-f0r.height)));
						c.draw(g);
					}else{
						Rectangle f0r = f0.getBounds();
						f0.setLocation(new Point((int)(or.getWidth()/2-f0r.getWidth()/2),(int)(or.getHeight()/2-gap.getHeight()/2-f0r.height)));
						f0.draw(g);
					}
					
					// draw necessary concepts
					d1.draw(g);
					d2.draw(g);
					l1.draw(g);
					l2.draw(g);
						
				}
			};
			dPanel.setLayout(new BorderLayout());
			dPanel.setBackground(Color.white);
			dPanel.setOpaque(false);
			dPanel.setPreferredSize(new Dimension(600,200));
		}
		return dPanel;		
	}
	
	
	
	
	public void addConceptEntry(ConceptEntry e) {
		// add new concept
		answerFindig = NodeConcept.createNodeConcept(e,this);
		answerPanel.setNodeConcept(answerFindig);
		if(dPanel != null)
			dPanel.repaint();
		
		// generate id
		e.getId();
		expertModule.resolveConceptEntry(e);
		notifyConceptEntry(e,ACTION_ADDED);
	}

	public List<ConceptEntry> getConceptEntries() {
		// TODO Auto-generated method stub
		return (answerFindig != null)?Collections.singletonList(answerFindig.getConceptEntry()):Collections.EMPTY_LIST;
	}

	public ConceptEntry getConceptEntry(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public JMenu getMenu() {
		return null;
	}

	public JPopupMenu getPopupMenu() {
		return null;
	}

	public JToolBar getToolBar() {
		return null;	
	}

	public void refineConceptEntry(ConceptEntry p, ConceptEntry e) {
		// TODO Auto-generated method stub

	}

	public void removeConceptEntry(ConceptEntry e) {
		// send message about this concept
		//ClientEvent ce = e.getClientEvent(this,Constants.ACTION_REMOVED);
		//MessageUtils.getInstance(this).flushInterfaceEvents(ce);
		//Communicator.getInstance().sendMessage(ce);
		expertModule.resolveConceptEntry(e);
		notifyConceptEntry(e,ACTION_REMOVED);
	}

	/**
	 * notify concept entry
	 * @param e
	 * @param action
	 */
	private void notifyConceptEntry(ConceptEntry e, String action){
		// send message about this concept's feature
		e.getFeature().setParentEntry(e);
		e.getFeature().getId();
		ClientEvent ce = e.getFeature().getClientEvent(this,action);
		ce.setInput(e.getMessageInput());
		MessageUtils.getInstance(this).flushInterfaceEvents(ce);
				
		// when finding is removed, send attribute deletes first
		if(!ACTION_REMOVED.equals(action))
			Communicator.getInstance().sendMessage(ce);
		
		// send messages in regards to the attributes
		for(ConceptEntry a: e.getAttributes()){
			a.getId();
			a.setParentEntry(e);
			Communicator.getInstance().sendMessage(a.getClientEvent(this,action));
		}
		
		// when finding is removed, send attribute deletes first
		if(ACTION_REMOVED.equals(action))
			Communicator.getInstance().sendMessage(ce);
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

	
	
	public void setExpertModule(ExpertModule module) {
		expertModule = module;
	}
		

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Tutor getTutor() {
		return tutor;
	}

	public boolean isEnabled() {
		return (component != null)?component.isEnabled():false;
	}

	public boolean isInteractive() {
		return interactive;
	}

	

	public void setEnabled(boolean b) {
		if(component != null){
			UIHelper.setEnableRecursive(component,b);
		}

	}

	public void setInteractive(boolean b) {
		interactive = b;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public void dispose() {	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Interface for displaying test questions";
	}

	public String getName() {
		return "Question Interface";
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	public void receiveMessage(Message msg) {
	
	}

	public void reset() {
		if(component != null){
			component.removeAll();
			answerText = null;
			answerGroup = null;
			answerPanel = null;
			answerFindig = null;
			pointObject = null;
			findingDialog = null;
			dPanel = null;
		}
	}

	public void resolveAction(Action action) {

	}

	private class SingleConceptPanel extends JPanel {
		private NodeConcept concept;
		
		public SingleConceptPanel(){
			super();
			//setBorder(new LineBorder(Color.black));
			setPreferredSize(new Dimension(150,75));
		}
		
		public void setNodeConcept(NodeConcept c){
			concept = c;	
			Rectangle or = getBounds();
			Rectangle ir = c.getBounds();
			int x = (int)((or.width - ir.width) / 2.0);
			int y = (int)((or.height - ir.height) / 2.0);
			c.setLocation(new Point(x,y));
			repaint();
		}
		
		/**
		 * this is where painting is done
		 */
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			if(concept != null)
				concept.draw(g);
				
		}
	}

	public JMenu getDebugMenu() {
		// TODO Auto-generated method stub
		return null;
	}


	public void treeCollapsed(TreeExpansionEvent event) {
		// TODO Auto-generated method stub
		
	}


	public void treeExpanded(TreeExpansionEvent event) {
		// notify tree selection
		TreePath p = event.getPath();
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_TREE,TextHelper.toString(p),ACTION_EXPANDED);
		ie.setParent(getFindingDialog().getTitle());
		MessageUtils.getInstance(this).addInterfaceEvent(ie);
	}
}
