package edu.pitt.dbmi.tutor.modules.interfaces;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.messages.*;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.ui.GlossaryManager;
import edu.pitt.dbmi.tutor.ui.TreeDialog;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.markers.Annotation;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.createFinding;

public class BulletInterface implements InterfaceModule, ActionListener, PropertyChangeListener, TreeSelectionListener, TreeExpansionListener {
	private final String FINDINGS = "FINDINGS";
	private final String HYPOTHESES = "HYPOTHESES";
	private final String ATTRIBUTES = "QUALITIES";
	private final String DIAGNOSES = "DIAGNOSES";
	private JPanel conceptPanel, mainPanel;
	private Properties defaultConfig;
	private JPanel findings, hypothesis, diagnosis;
	private JToolBar toolbar;
	private JPopupMenu popup;
	private JMenu menu;
	private AbstractButton selectedButton;
	private Map<String,BulletConcept> registry;
	private Set<String> registrySet;
	private Map<String,TreeNode> treeMap;
	private TreeDialog findingDialog, hypothesisDialog,diagnosisDialog,attributeDialog;
	private DefaultMutableTreeNode diagnosisRoot;
	private ExpertModule expertModule;
	private CaseEntry caseEntry;
	private Tutor tutor;
	private boolean interactive;
	private ViewPosition view;
	private int tempIndex = -1;
	private List<BulletConcept> selectedConcepts;
	private BulletConcept popupConcept;
	private boolean blockMessages;
	private String [] lockInterfaceExceptions;
	private List<Action> annotationActions;
	private Color absentFindingColor;
	private String currentDialog;
	private GlossaryManager glossaryManager;
	
	// behavior modifiers
	private boolean behaviorAttributeFindingMode, behaviorAllAttributeMode,behaviorAllDiagnosisMode;
	private boolean behaviorShowGlossary,behaviorShowExample;
	
	
	public void load(){
		//NOOP:
	}
	
	
	/**
	 * reset everything for new problem
	 */
	public void dispose() {
		findings.removeAll();
		hypothesis.removeAll();
		diagnosis.removeAll();
	}
	
	
	/**
	 * layout panel verticly or horizontally
	 * @param vertical
	 */
	public void layout(boolean horizontal){
		mainPanel.removeAll();
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(0,1));
		p.setOpaque(false);
		conceptPanel.add(getScrollPane(findings),BorderLayout.CENTER);
		p.add(getScrollPane(hypothesis));
		p.add(getScrollPane(diagnosis));
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		p2.setOpaque(false);
		p2.add(p,BorderLayout.CENTER);
		conceptPanel.add(p2,(horizontal)?BorderLayout.EAST:BorderLayout.SOUTH);
	}
	
	
	private Component getScrollPane(Component p){
		JScrollPane s = new JScrollPane(p);
		s.setOpaque(false);
		s.getViewport().setOpaque(false);
		s.setBorder(null);
		s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return s;
	}
	
	/**
	 * create panel
	 * @param title
	 * @param bottomless border
	 * @return
	 */
	private JPanel createPanel(String title, boolean b){
		JPanel panel = new JPanel();
		TitledBorder border = new TitledBorder(title);
		border.setBorder(LineBorder.createGrayLineBorder());
		border.setTitleColor(Color.GRAY);

		// set magins
		Border margin = new EmptyBorder(0,0,(b)?-3:0,0);
		
		panel.setBorder(new CompoundBorder(margin,border));
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		panel.setBackground(Color.WHITE);
		panel.setOpaque(false);
		return panel;
	}
	
	/**
	 * add a concept to appropriate section
	 * @param concept
	 */
	public void addConcept(Component c){
		if(c instanceof BulletConcept){
			BulletConcept concept = (BulletConcept) c;
			if(TYPE_DIAGNOSIS.equals(concept.getType()))
				diagnosis.add(concept);
			else if(TYPE_HYPOTHESIS.equals(concept.getType()))
				hypothesis.add(concept);
			else 
				findings.add(concept);
			//return c;
		}
		getComponent().validate();
		//return super.add(c);
	}
	
	/**
	 * remove component from panel
	 */
	public void removeConcept(Component c){
		if(c instanceof BulletConcept){
			BulletConcept concept = (BulletConcept) c;
			if(TYPE_DIAGNOSIS.equals(concept.getType()))
				diagnosis.remove(concept);
			else if(TYPE_HYPOTHESIS.equals(concept.getType()))
				hypothesis.remove(concept);
			else  
				findings.remove(concept);
			
			// TODO attribute
			return;
		}
		//super.remove(c);
	}
	
	/**
	 * get index of components
	 * @param cont
	 * @param comp
	 * @return
	 */
	private int getIndex(Container cont, Component comp){
		Component [] comps = cont.getComponents();
		for(int i=0;i<comps.length;i++)
			if(comps[i] == comp)
				return i;
		return -1;
	}
	
	
	/**
	 * get popup concept
	 * @return
	 */
	public List<BulletConcept> getSelectedNodes(){
		if(popupConcept != null){
			return new ArrayList<BulletConcept>(Collections.singletonList(popupConcept));
		}
		
		if(selectedConcepts == null)
			selectedConcepts = new ArrayList<BulletConcept>();
		
		return selectedConcepts;
	}
	
	private void clearPopupSelection(){
		popupConcept = null;
	}
	
	/**
	 * get popup concept
	 * @return
	 */
	public BulletConcept getSelectedNode(){
		if(selectedConcepts == null)
			selectedConcepts = new ArrayList<BulletConcept>();
		
		if(popupConcept != null){
			return popupConcept;
		}else if(!selectedConcepts.isEmpty())
			return selectedConcepts.get(0);
		return null;
	}
	
	void setPopupNode(BulletConcept c){
		popupConcept = c;
	}
	
	BulletConcept getPopupNode(){
		return popupConcept;
	}
	
	/**
	 * Start/Stop component from being dragged
	 * @param c
	 * @param drag
	 */
	public void dragConcept(BulletConcept c, boolean drag, boolean transfer){
		Container container = mainPanel;
		// select original container
		if(TYPE_DIAGNOSIS.equals(c.getType()))
			container = diagnosis;
		else if(TYPE_HYPOTHESIS.equals(c.getType()))
			container = hypothesis;
		else 
			container = findings;
		
		// pick panel
		JPanel tempPanel = mainPanel;
		
		// if about to drag 
		if(drag){
			// remove component from container
			tempIndex = getIndex(container,c);
			container.remove(c);
			//add it to this panel
			tempPanel.add(c,BorderLayout.NORTH);
			//addConcept(c);
		}else{
			// remove from this panel
			tempPanel.remove(c);
			
			// figure out index
			int i = container.getComponentCount();
			if(tempIndex > -1 && transfer){
				i = tempIndex;
			}else{
				Point l = c.getLocation();
				l = SwingUtilities.convertPoint(getComponent(),l,container);
				for(i=0;i<container.getComponentCount();i++){
					Component comp = container.getComponent(i);
					if(l.y < comp.getLocation().y){
						break;
					}
				}
			}
			// readd back to appropriate panel
			if(i >= container.getComponentCount())
				container.add(c);
			else
				container.add(c,i);
		}
	}

	/**
	 * @return the diagnosis
	 */
	public JPanel getDiagnosisPanel() {
		return diagnosis;
	}

	/**
	 * @return the findings
	 */
	public JPanel getFindingsPanel() {
		return findings;
	}

	/**
	 * @return the hypothesis
	 */
	public JPanel getHypothesisPanel() {
		return hypothesis;
	}
	
	public void reconfigure(){
		dispose();
		mainPanel = null;
	}
	
	/**
	 * get component
	 */
	public Component getComponent() {
		if(mainPanel == null){
			boolean ch = "horizontal".equalsIgnoreCase(Config.getProperty(this,"component.orientation"));
			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			mainPanel.setBackground(Config.getColorProperty(this,"component.background"));
			mainPanel.setOpaque(true);
			
			registry = new HashMap<String, BulletConcept>();
			registrySet = new HashSet<String>();
			treeMap = new HashMap<String,TreeNode>();
			diagnosisRoot = new DefaultMutableTreeNode("DIAGNOSES");
				
			// create panels
			findings = createPanel(Config.getProperty(this,"title.findings"),!ch);
			hypothesis = createPanel(Config.getProperty(this,"title.hypotheses"),true);
			//hypothesis.setPreferredSize(new Dimension(250,100));
			diagnosis = createPanel(Config.getProperty(this,"title.diagnoses"),false);
			diagnosis.setPreferredSize(new Dimension(275,100));
			
			conceptPanel = new JPanel();
			conceptPanel.setLayout(new BorderLayout());
			conceptPanel.setOpaque(false);
			
			
			layout(ch);
			
			// add behavior modifiers
			behaviorAttributeFindingMode = "attribute".equals(Config.getProperty(this,"behavior.finding.mode"));
			behaviorAllAttributeMode     =  "all".equals(Config.getProperty(this,"behavior.attribute.mode"));
			behaviorAllDiagnosisMode = "all".equals(Config.getProperty(this,"behavior.diagnosis.mode"));
			absentFindingColor = Config.getColorProperty(this,"tree.absent.finding.color");
			behaviorShowExample = Config.getBooleanProperty(this,"behavior.glossary.show.example");
			behaviorShowGlossary = Config.getBooleanProperty(this,"behavior.glossary.enabled");
			
			// get orientation params
			boolean th = "horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation"));
			
			// add components
			mainPanel.add(getToolBar(th),(th)?BorderLayout.NORTH:BorderLayout.WEST);
			mainPanel.add(conceptPanel,BorderLayout.CENTER);
			//add(diagnoses,(ch)?BorderLayout.EAST:BorderLayout.SOUTH);
			
			// set preferred size
			mainPanel.setPreferredSize(Config.getDimensionProperty(this,"component.size"));

		}
		return mainPanel;
	}
	
	/**
	 * get toolbar
	 * @return
	 */
	public JToolBar getToolBar(){
		return getToolBar("horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation")));
	}
	
	/**
	 * get toolbar
	 * @return
	 */
	private JToolBar getToolBar(boolean horizontal){
		if(toolbar == null){
			toolbar = new UIHelper.ToolBar();
			
			// create toolbar
			toolbar.add(UIHelper.createToggleButton("finding","Identify Finding",
					UIHelper.getIcon(this,"icon.toolbar.finding",24),this));
			toolbar.add(UIHelper.createButton("absent.finding","Identify Absent Finding",
					UIHelper.getIcon(this,"icon.toolbar.absent.finding",24),this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("hypothesis","Identify Hypothesis",
					UIHelper.getIcon(this,"icon.toolbar.hypothesis",24),this));
			toolbar.add(UIHelper.createButton("diagnosis","Identify Diagnosis",
					UIHelper.getIcon(this,"icon.toolbar.diagnosis",24),this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("delete","Remove Concepts",
					UIHelper.getIcon(this,"icon.toolbar.delete",24),this));
			//toolbar.addSeparator();
			//toolbar.add(UIHelper.createButton("clinical.info","View Clinical Information",
			//		UIHelper.getIcon(this,"icon.toolbar.clinical.info",24),this));
			toolbar.add(Box.createGlue());
			toolbar.add(UIHelper.createButton("Done","Finish Case",
					UIHelper.getIcon(this,"icon.toolbar.done"),-1,true,this));
			
			// change orientation
			toolbar.setOrientation((horizontal)?JToolBar.HORIZONTAL:JToolBar.VERTICAL);
		}	
		return toolbar;
	}
	
	/**
	 * get menu for this interface
	 * @return
	 */
	public JMenu getMenu(){
		if(menu == null){
			menu = new JMenu("Interface");
			
			// create toolbar
			menu.add(UIHelper.createMenuItem("finding","Identify Finding",
					UIHelper.getIcon(this,"icon.menu.finding",16),this));
			menu.add(UIHelper.createMenuItem("absent.finding","Identify Absent Finding",
					UIHelper.getIcon(this,"icon.menu.absent.finding",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("hypothesis","Identify Hypothesis",
					UIHelper.getIcon(this,"icon.menu.hypothesis",16),this));
			menu.add(UIHelper.createMenuItem("diagnosis","Identify Diagnosis",
					UIHelper.getIcon(this,"icon.menu.diagnosis",16),this));
			menu.addSeparator();
			
			menu.add(UIHelper.createMenuItem("clinical.info","View Clinical Information",
					UIHelper.getIcon(this,"icon.menu.clinical.info",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("Done","Finish Case",
					UIHelper.getIcon(this,"icon.menu.done"),16,this));
			
			
			// put in debug options
			JMenu debug = ITS.getInstance().getDebugMenu();
			if(!UIHelper.hasMenuItem(debug,"Show Slide Annotations")){
				debug.add(UIHelper.createMenuItem("goal.list","Preview Case Information ..",
						  UIHelper.getIcon(Config.getProperty("icon.menu.preview")),this),0);
				debug.add(UIHelper.createCheckboxMenuItem("show.annotations","Show Slide Annotations",null,this),1);
			}
			
		}
		return menu;
	}

	/**
	 * get menu for this interface
	 * @return
	 */
	public JPopupMenu getPopupMenu(){
		if(popup == null){
			popup = new JPopupMenu();
			String specifyStr = (behaviorAttributeFindingMode)?"Describe Qualities":"Further Specify";
			popup.add(UIHelper.createMenuItem("specify",specifyStr,
					UIHelper.getIcon(this,"icon.menu.specify",16),this));
			popup.add(UIHelper.createMenuItem("glossary","Lookup Glossary",
					UIHelper.getIcon(this,"icon.menu.glossary",16),this));
			popup.add(UIHelper.createMenuItem("location","Show Location",
					UIHelper.getIcon(this,"icon.menu.location",16),this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("delete","Remove Concept",
					UIHelper.getIcon(this,"icon.menu.delete",16),this));
			
		}
		
		// change menu based on selected concept
		BulletConcept c = getSelectedNode();
		if(c != null){
			// enable location
			popup.getComponent(2).setEnabled(TYPE_FINDING.equals(c.getConceptEntry().getType()));
			
			if(TYPE_ATTRIBUTE.equals(c.getType())){
				popup.getComponent(0).setEnabled(false);
				popup.getComponent(1).setEnabled(false);	
			}else {
				popup.getComponent(0).setEnabled(true);
				popup.getComponent(1).setEnabled(true);
			}
			// set name for remove
			setButtonNames(c,popup);
			
			// disable glossary
			if(!behaviorShowGlossary)
				popup.getComponent(1).setEnabled(false);
		}
		
		
		
		return popup;
	}
	
	/**
	 * set case entry
	 */
	public void setCaseEntry(CaseEntry problem) {
		caseEntry = problem;
	}
	
	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b){
		if(mainPanel != null){
			mainPanel.setEnabled(b);
			UIHelper.setEnabled(getToolBar(),b);
			UIHelper.setEnabled(getMenu(),b);
		}
	}
	
	public boolean isEnabled(){
		return (mainPanel == null)?false:mainPanel.isEnabled();
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
	
	public Tutor getTutor() {
		return tutor;
	}

	public void setTutor(Tutor tutor) {
		this.tutor = tutor;
	}

	
	/**
	 * set remove button name
	 * @param c
	 * @param bt
	 */
	private void setButtonNames(BulletConcept c, Container container){
		AbstractButton dq = (AbstractButton)container.getComponent(0);
		AbstractButton rm = (AbstractButton)container.getComponent(4);
		
		// set default specify string
		if(behaviorAttributeFindingMode)
			dq.setText("Further Specify");
		
		if(TYPE_ATTRIBUTE.equals(c.getType())){
			rm.setText("Remove Modifier");
		}else if(TYPE_FINDING.equals(c.getType()) || TYPE_ABSENT_FINDING.equals(c.getType())){
			rm.setText("Remove Finding");
			if(behaviorAttributeFindingMode)
				dq.setText("Describe Qualities");
		}else if(TYPE_DIAGNOSIS.equals(c.getType())){
			rm.setText("Remove Diagnosis");
		}else if(TYPE_HYPOTHESIS.equals(c.getType())){
			rm.setText("Remove Hypothesis");
		}
	}

	
	/**
	 * get default configuration
	 */
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	/**
	 * get description
	 */
	public String getDescription() {
		return "Diagnostic Interface where findings, hypotheses and diagnoses" +
				" are presented in bulleted lists.";
	}

	/**
	 * get name of this component
	 */
	public String getName() {
		return "Bullet Interface";
	}


	/**
	 * get version
	 */
	public String getVersion() {
		return "1.0";
	}
		
	public void reset(){
		if(annotationActions != null){
			for(Action a: annotationActions){
				a.undo();
			}
			annotationActions = null;
		}
		if(glossaryManager != null){
			glossaryManager.reset();
		}
		findings.removeAll();
		hypothesis.removeAll();
		diagnosis.removeAll();
		registry.clear();
		registrySet.clear();
		if(selectedConcepts != null)
			selectedConcepts.clear();
		diagnosisRoot.removeAllChildren();
	}
	
	
	/**
	 * perform actions
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		// unselect previous button
		if(selectedButton != null){
			selectedButton.setSelected(false);
			selectedButton = null;
			cancelFinding();
		}
		
		// notify button press
		if(e.getSource() instanceof AbstractButton){
			notifyButton((AbstractButton)e.getSource());
		}
		
		
		// do actions
		if(cmd.equalsIgnoreCase("finding")){
			selectedButton = (AbstractButton) e.getSource();
			if(selectedButton.isSelected())
				doFinding();
			else
				cancelFinding();
		}else if(cmd.equalsIgnoreCase("absent.finding")){
			doAbsentFinding();
		}else if(cmd.equalsIgnoreCase("hypothesis")){
			doHypothesis();
		}else if(cmd.equalsIgnoreCase("diagnosis")){
			doDiagnosis();
		}else if(cmd.equalsIgnoreCase("done")){
			doDone();
		}else if(cmd.equalsIgnoreCase("clinical.info")){
			doClinicalInfo();
		}else if(cmd.equalsIgnoreCase("delete")){
			doDelete();
		}else if(cmd.equalsIgnoreCase("glossary")){
			doGlossary();
		}else if(cmd.equalsIgnoreCase("specify")){
			BulletConcept node = getSelectedNode();
			if(node != null){
				if(behaviorAttributeFindingMode && node.getConceptEntry().isFinding())
					doAttributes();
				else
					doSpecify();
			}
		}else if(cmd.equalsIgnoreCase("goal.list")){
			doShowCase();
		}else if(cmd.equalsIgnoreCase("location")){
			if(getSelectedNode() != null){
				getSelectedNode().showIdentifiedFeature();
			}
		}else if(cmd.equalsIgnoreCase("show.annotations")){
			doShowAnnotations(((AbstractButton)e.getSource()).isSelected());
		}		
		
		// clear
		clearPopupSelection();
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
	
	/**
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getFindingDialog(){
		if(findingDialog == null){
			findingDialog = createDialog(FINDINGS);
		}
		return findingDialog;
	}
	
	/**
	 * create dialog
	 * @param name
	 * @return
	 */
	private TreeDialog createDialog(String name){
		TreeDialog d = TreeDialog.createDialog(UIHelper.getWindow(getComponent()));
		d.getTreePanel().addTreeSelectionListener(this);
		d.getTreePanel().addPropertyChangeListener(this);
		d.getTreePanel().addTreeExpansionListener(this);
		d.setTitle(name);
		TreeNode root = treeMap.get(name);
		if(root != null){
			d.setRoot(root);
		}
		return d;
	}
	
	/**
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getAttributeDialog(){
		if(attributeDialog == null){
			attributeDialog = createDialog(ATTRIBUTES);
		}
		return attributeDialog;
	}
	
	
	/**
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getHypothesisDialog(){
		if(hypothesisDialog == null){
			hypothesisDialog = createDialog(HYPOTHESES);
		}
		return hypothesisDialog;
	}
	
	/**
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getDiagnosisDialog(){
		if(diagnosisDialog == null){
			diagnosisDialog = createDialog(DIAGNOSES);
		}
		// reset root every time
		if(!behaviorAllDiagnosisMode)
			diagnosisDialog.setRoot(diagnosisRoot);
		
		return diagnosisDialog;
	}
	
	/**
	 * raise curtain during finding identification
	 * @param b
	 */
	private void setRaiseCurtain(boolean raise){
		PresentationModule viewer = getTutor().getPresentationModule();
		if(raise){
			getComponent().setBackground(new Color(240,240,240));
			getComponent().setCursor(UIHelper.getChildCursor(viewer.getComponent()));
		}else{
			getComponent().setBackground(Config.getColorProperty(BulletInterface.this,"component.background"));
			getComponent().setCursor(Cursor.getDefaultCursor());
		}
		getComponent().repaint();
	}
	
	/**
	 * add new concept
	 */
	private void doFinding(){
		// start the drawing
		(new Thread(new Runnable(){
			public void run(){
				PresentationModule viewer = getTutor().getPresentationModule();
				viewer.startFeatureIdentification();
				setRaiseCurtain(true);
				
				while(!viewer.isFeatureIdentified()){
					UIHelper.sleep(200);
				}
				Object input = viewer.getIdentifiedFeature();
				viewer.stopFeatureIdentification();
				setRaiseCurtain(false);
				
				// if input is null and button unselected
				// means that action was canceled
				if(input == null)
					return;
								
				// get the dialog
				TreeDialog d = getFindingDialog();
				d.setTitle(FINDINGS);
				d.setBackground(Color.white);
				currentDialog = d.getTitle();
				d.setVisible(true);
				TreePath [] selection = d.getSelectedNodes();
				if(selection.length > 0){
					for(TreePath name: selection){
						ConceptEntry e = new ConceptEntry(name,TYPE_FINDING);
						e.setInput(input);
						addConceptEntry(e);
					}
				}else if(input != null){
					viewer.removeIdentifiedFeature(input);
					unselectButton();
				}
				
				if(d.getSelectedNodes().length == 0 && !blockMessages)
					MessageUtils.getInstance(BulletInterface.this).flushInterfaceEvents(null);
			}
		})).start();
			
		
	}
	
	/**
	 * add new concept
	 */
	private void doAbsentFinding(){
		TreeDialog d = getFindingDialog();
		d.setTitle("ABSENT "+FINDINGS);
		d.setBackground(absentFindingColor);
		currentDialog = d.getTitle();
		d.setVisible(true);
		for(TreePath name: d.getSelectedNodes()){
			ConceptEntry e = new ConceptEntry(name,TYPE_ABSENT_FINDING);
			e.setInput(view);
			addConceptEntry(e);
		}
		
		if(d.getSelectedNodes().length == 0 && !blockMessages)
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
	}
	
	/**
	 * add new concept
	 */
	private void cancelFinding(){
		// color component back to normal
		setRaiseCurtain(false);
		
		// stop feature identification
		PresentationModule viewer = getTutor().getPresentationModule();
		if(viewer != null){
			viewer.stopFeatureIdentification();
		}
		
		unselectButton();
	}
	
	/**
	 * add new concept
	 */
	private void doHypothesis(){
		TreeDialog d = getHypothesisDialog();
		d.setVisible(true);
		currentDialog = d.getTitle();
		for(TreePath name: d.getSelectedNodes())
			addConceptEntry(new ConceptEntry(name,TYPE_HYPOTHESIS));
		
		if(d.getSelectedNodes().length == 0 && !blockMessages)
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
	}
	
	/**
	 * add new concept
	 */
	private void doDiagnosis(){
		TreeDialog d = getDiagnosisDialog();
		d.setVisible(true);
		currentDialog = d.getTitle();
		for(TreePath name: d.getSelectedNodes())
			addConceptEntry(new ConceptEntry(name,TYPE_DIAGNOSIS));
		
		if(d.getSelectedNodes().length == 0 && !blockMessages)
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
	}
	
	
	/**
	 * add new concept
	 */
	private void doDone(){
		Communicator.getInstance().sendMessage(ClientEvent.createClientEvent(this,TYPE_DONE,getClass().getSimpleName(),ACTION_REQUEST));
	}
	
	
	/**
	 * show clinical info
	 */
	private void doClinicalInfo(){
		UIHelper.HTMLPanel infoPanel =  new UIHelper.HTMLPanel();
		infoPanel.setPreferredSize(new Dimension(350, 300));
		infoPanel.setEditable(false);
		infoPanel.setReport(caseEntry.getClinicalInfo());
		infoPanel.setCaretPosition(0);

		// display info in modal dialog
		JOptionPane.showMessageDialog(Config.getMainFrame(),
		new JScrollPane(infoPanel), "Clinical Case Information",JOptionPane.PLAIN_MESSAGE);
	}
	
	
	/**
	 * show glossary
	 */
	private void doGlossary(){
		BulletConcept node = getSelectedNode();
		if(node != null){
			ConceptEntry e = node.getConceptEntry().getFeature();
			
			// notify of action
			Communicator.getInstance().sendMessage(e.getClientEvent(this,Constants.ACTION_GLOSSARY));
			
			// resolve node
			expertModule.resolveConceptEntry(e);
			
			// show glossary panel
			//showGlossaryPanel(node);
			if(glossaryManager == null){
				glossaryManager = new GlossaryManager();
				glossaryManager.setShowExampleImage(behaviorShowExample);
			}
			Point pt = node.getLocationOnScreen();
			glossaryManager.showGlossary(e,getComponent(),new Point(pt.x+20,pt.y+node.getSize().height));
			
		}
	}
	
	
	/**
	 * do specify
	 */
	private void doSpecify(){
		BulletConcept node = getSelectedNode();
		if(node != null){
			ConceptEntry c = node.getConceptEntry();
			expertModule.resolveConceptEntry(c);
			ConceptEntry r = c.getFeature();
			TreeNode root = (node.getConceptEntry().isFinding())?treeMap.get(FINDINGS):treeMap.get(HYPOTHESES);
			TreeDialog d = TreeDialog.createDialog(UIHelper.getWindow(getComponent()));
			d.setRoot(OntologyHelper.getSubTree(root,r.getName()));
			d.setSelectionMode(TreeDialog.SINGLE_SELECTION);
			d.setSelectedNode(c.getName());
			d.setVisible(true);
			TreePath path = d.getSelectedNode();
			if(path != null){
				ConceptEntry e = new ConceptEntry(path,c.getType());
				e.setFeature(c.getFeature());
				for(ConceptEntry a: c.getAttributes())
					e.addAttribute(a);
				if(!c.equals(e))
					refineConceptEntry(c,e);
			}else if(!blockMessages){
				MessageUtils.getInstance(this).flushInterfaceEvents(null);
			}
		}
	}
	
	
	/**
	 * do specify
	 */
	private void doAttributes(){
		BulletConcept node = getSelectedNode();
		if(node != null){
			ConceptEntry c = node.getConceptEntry();
						
			TreeDialog d = null;
			if(behaviorAllAttributeMode){
				d = getAttributeDialog();
			}else{
				// resolve concept
				expertModule.resolveConceptEntry(c);
				
				// create a list of attribute paths
				List<TreePath> paths = new ArrayList<TreePath>();
				List<ConceptEntry> alist = c.getPotentialAttributes();
				alist.removeAll(c.getAttributes());
				for(ConceptEntry e: alist){
					paths.addAll(expertModule.getTreePaths(e.getName()));
				}
				d = TreeDialog.createDialog(UIHelper.getWindow(getComponent()));
				d.setRoot(OntologyHelper.getTree(paths,OntologyHelper.ATTRIBUTES));
			}
			currentDialog = d.getTitle();
			d.setVisible(true);
			
			if(d.getSelectedNodes().length > 0){
				// create list of attributes
				List<ConceptEntry> attributes = new ArrayList<ConceptEntry>();
				attributes.addAll(c.getAttributes());
				for(TreePath p : d.getSelectedNodes()){
					attributes.add(new ConceptEntry(p,TYPE_ATTRIBUTE));
				}	
				
				// create compound concept
				ConceptEntry e = createFinding(c.getFeature(),attributes,expertModule.getDomainOntology());
					
								
				// refine concept
				refineConceptEntry(node.getConceptEntry(),e);
			}else if(!blockMessages){
				MessageUtils.getInstance(this).flushInterfaceEvents(null);
			}
		}
	}
	
	/**
	 * delete concept
	 */
	private void doDelete(){
		List<BulletConcept> list = new ArrayList<BulletConcept>(getSelectedNodes());
		if(list.isEmpty()){
			JOptionPane.showMessageDialog(getComponent(),"No concepts were selected","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		// delete selection			
		for(BulletConcept c : list){
			removeConceptEntry(c.getConceptEntry());
		}
	}
	
	/**
	 * show case information
	 */
	private void doShowCase(){
		// create dialog
		JOptionPane op = new JOptionPane(UIHelper.createCaseInfoPanel(caseEntry,false),JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(Config.getMainFrame(),caseEntry.getName());
		d.pack();
		d.setModal(false);
		d.setResizable(true);
		d.setVisible(true);
	}
	
	/**
	 * show annotations in case
	 */
	private void doShowAnnotations(boolean show){
		if(annotationActions == null)
			annotationActions = new ArrayList<Action>();
		
		// undo previous actions
		for(Action a: annotationActions)
			a.undo();
		annotationActions.clear();
		
		// now display if selected
		if(show){
			// get selected concepts
			List<BulletConcept> nodes = getSelectedNodes();
			if(nodes.isEmpty()){
				annotationActions.add(new Action(PresentationModule.class.getSimpleName(),
									POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL));
			}else{
				for(BulletConcept node: nodes){
					for(ConceptEntry e : OntologyHelper.getMatchingFindings(caseEntry,node.getConceptEntry())){
						Action action = new Action(PresentationModule.class.getSimpleName(),
											POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL);
						action.setConceptEntry(e);
						annotationActions.add(action);
					}
				}
			}
			
			// now execute those actions
			for(Action a: annotationActions){
				Communicator.getInstance().resolveAction(a);
				a.run();
			}
		}
	}
	
	/**
	 * load expert module info 
	 * or name of the node
	 */
	public void setExpertModule(ExpertModule expert){
		expertModule = expert;
		setupConceptTrees();
	}
	
	/**
	 * setup selection trees
	 */
	private void setupConceptTrees(){
		ConceptFilter featureFilter = null;
		// fetch only feature findings, if in attribute mode
		if(behaviorAttributeFindingMode)
			featureFilter = new OntologyHelper.FeatureFilter(expertModule.getDomainOntology());
		else
			featureFilter = new OntologyHelper.FindingFilter(expertModule.getDomainOntology());
		
		// clear all of the maps 
		treeMap.clear();

		//fetch standard trees
		treeMap.put(FINDINGS,expertModule.getTreeRoot(OntologyHelper.DIAGNOSTIC_FEATURES,featureFilter));
		treeMap.put(HYPOTHESES,expertModule.getTreeRoot(OntologyHelper.DIAGNOSES));
		
		// set attribute tree
		if(behaviorAttributeFindingMode)
			treeMap.put(ATTRIBUTES,expertModule.getTreeRoot(OntologyHelper.ATTRIBUTES));
		
		// set diagnosis tree
		if(behaviorAllDiagnosisMode)
			treeMap.put(DIAGNOSES,treeMap.get(HYPOTHESES));
		
		// clear dialogs
		findingDialog = attributeDialog = diagnosisDialog = hypothesisDialog = null;
	}
	
	/**
	 * add to hypothesis tree
	 * @param child
	 */
	private void addToDxTree(ConceptEntry child){
		// add new hypothesis as potential diagnosis
		if(TYPE_HYPOTHESIS.equals(child.getType())) {
			diagnosisRoot.add(new DefaultMutableTreeNode(child.getName()));
		}
	}
	
	/**
	 * remove from dx tree
	 * @param e
	 */
	private void removeFromDxTree(ConceptEntry e){
		// remove hypothesis as potential diagnosis
		if(TYPE_HYPOTHESIS.equals(e.getType())) {
			for(int i=0;i<diagnosisRoot.getChildCount();i++){
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) diagnosisRoot.getChildAt(i);
				if(node.getUserObject().equals(e.getName())){
					diagnosisRoot.remove(node);
					break;
				}
			}
		}
	}
	
	/**
	 * add hypothesis
	 * @param name
	 */
	public void addConceptEntry(ConceptEntry e){
		//addConceptEntry(e,null);
		// unselect button if it was selected
		unselectButton();
		
		// handle null 
		if(e == null)
			return;
		
		// check for duplicates?
		BulletConcept o = getBulletConcept(e);
		if(o != null){
			o.flash();
			// if input is annotation
			if(e.getInput() instanceof Annotation){
				// first remove a tag for this entry
				Annotation a = (Annotation) e.getInput();
				a.removeTag(e.getText());
				// only if that was the only tag, remove X
				if(a.getTags().isEmpty())
					getTutor().getPresentationModule().removeIdentifiedFeature(a);
			}else{
				getTutor().getPresentationModule().removeIdentifiedFeature(e.getInput());
			}
			return;
		}
		
		// resolve concept to check for attributes and feature
		expertModule.resolveConceptEntry(e);
				
		
		// add new concept
		BulletConcept c = new BulletConcept(e,this);
		addConcept(c);
		
		// register it
		registerConceptEntry(e);
		
		// add hypothesis as potential diagnosis
		addToDxTree(e);
		
		// send message about this concept
		//Communicator.getInstance().sendMessage(e.getClientEvent(this,Constants.ACTION_ADDED));	
	
		// check if we need to generate an event for annotation
		if(e.getInput() != null && TYPE_FINDING.equals(e.getType())){
			// create Interface event for this action
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_PRESENTATION,e.getName(),ACTION_IDENTIFY);
			ie.setInput(e.getMessageInput());
			MessageUtils.getInstance(this).addInterfaceEvent(ie);
		}
		
		// send message about this concept's feature
		notifyConceptEntry(e,ACTION_ADDED);
	
	}
	
	/**
	 * notify concept entry
	 * @param e
	 * @param action
	 */
	private void notifyConceptEntry(ConceptEntry e, String action){
		if(blockMessages)
			return;
		
		// send message about this concept's feature
		e.getFeature().setParentEntry(e);
		ClientEvent ce = e.getFeature().getClientEvent(this,action);
		ce.setInput(e.getMessageInput());
		MessageUtils.getInstance(this).flushInterfaceEvents(ce);
		
		// when finding is removed, send attribute deletes first
		if(!ACTION_REMOVED.equals(action))
			sendMessage(ce);
		
		// send messages in regards to the attributes
		for(ConceptEntry a: e.getAttributes()){
			a.setParentEntry(e);
			sendMessage(a.getClientEvent(this,action));
		}
		
		// when finding is removed, send attribute deletes first
		if(ACTION_REMOVED.equals(action))
			sendMessage(ce);
	}
	
	/**
	 * send message
	 * @param ce
	 */
	private void sendMessage(ClientEvent ce){
		//augment inputs
		//if(getAuxInputMap().containsKey(ce.getType()))
		//	ce.getInputMap().putAll(getAuxInputMap().get(ce.getType()));
		Communicator.getInstance().sendMessage(ce);
	}
	
	/**
	 * unselect button
	 */
	private void unselectButton(){
		if(selectedButton != null){
			selectedButton.setSelected(false);
			selectedButton = null;
		}
	}
	
	/**
	 * register concept entry
	 * @param e
	 */
	
	private void registerConceptEntry(ConceptEntry e){
		// register it
		e.addPropertyChangeListener(this);
		
		// register feature
		BulletConcept node = BulletConcept.getBulletConcept(e,this);
		registry.put(e.getId(),node);
		registry.put(e.getFeature().getId(),node);
		
		// register attribute
		for(BulletConcept a: node.getAttributes()){
			registry.put(a.getConceptEntry().getId(),a);
		}
		
		//registrySet.add(e.getName());
	}
	
	
	/**
	 * unregister concept entry
	 * @param e
	 */
	private void unregisterConceptEntry(ConceptEntry e){
		//registry.remove(e.getId());
		//registrySet.remove(e.getName());
		BulletConcept node =  BulletConcept.getBulletConcept(e,this);
		registry.remove(e.getId());
		registry.remove(e.getFeature().getId());
		
		// unregister attribute
		if(node != null){
			for(BulletConcept a: node.getAttributes()){
				registry.remove(a.getConceptEntry().getId());
				a.getConceptEntry().removePropertyChangeListener(this);
			}
		}
		e.removePropertyChangeListener(this);
	}
	
	/**
	 * add hypothesis
	 * @param name
	 */
	public void refineConceptEntry(ConceptEntry parent, ConceptEntry child){
		// unselect button if it was selected
		unselectButton();
		
		// handle null 
		if(child == null || parent == null)
			return;
	
		// check for duplicates?
		// check for duplicates?
		BulletConcept o = getBulletConcept(child);
		if(o != null){
			// flash me
			o.flash();
			
			// well, if we are refining parent to child
			// and child exists already, then we should
			// just remove parent
			removeConceptEntry(parent);
			return;
		}
		
		// resolve both parent and child
		expertModule.resolveConceptEntry(parent);
		expertModule.resolveConceptEntry(child);
		
		// get parent Concept
		BulletConcept p = BulletConcept.getBulletConcept(parent,this);
				
			
		// add new concept
		BulletConcept c = new BulletConcept(child,this);
		addConcept(c);
	
		// place new concept on top of old concept
		parent.copyTo(child);
		//c.getConceptEntry().setFeature(parent);
		c.setLocation(p.getLocation());
		c.flash();
		
		//remove old concept
			
		// remove  old concept from registry
		removeConcept(p);
		unregisterConceptEntry(parent);
			
		
		// do clenup	
		getComponent().repaint();
		parent.delete();
			
		// remove old hypothesis as potential diagnosis
		removeFromDxTree(parent);
				
		// add new concept to registry		
		registerConceptEntry(child);
		
		// add new hypothesis as potential diagnosis
		addToDxTree(child);
		
		// send message about this concept
		//ClientEvent ce = child.getClientEvent(this,Constants.ACTION_REFINE);
		//ce.setParent(parent.getName());
		//Communicator.getInstance().sendMessage(ce);
		
		// if features are the same, then we are adding/removing attributes, else refining
		if(child.getFeature().equals(parent.getFeature())){
			if(!blockMessages){
				// send add events
				for(ConceptEntry a: getMissingAttributes(parent, child)){
					Communicator.getInstance().sendMessage(a.getClientEvent(this,ACTION_ADDED));
				}
				
				// send remove events
				for(ConceptEntry a: getMissingAttributes(child,parent)){
					Communicator.getInstance().sendMessage(a.getClientEvent(this,ACTION_REMOVED));
				}
			}
		}else{
			notifyConceptEntry(parent,ACTION_REMOVED);
			notifyConceptEntry(child,ACTION_ADDED);
		}
		
	}
	
	/**
	 * get list of attributes that is present in child, but unavailable in parent
	 * @param parent
	 * @param child
	 * @return
	 */
	private List<ConceptEntry> getMissingAttributes(ConceptEntry parent,ConceptEntry child){
		List<ConceptEntry> toadd = new ArrayList<ConceptEntry>();
		for(ConceptEntry a: child.getAttributes()){
			if(!parent.getAttributes().contains(a))
				toadd.add(a);
		}
		return toadd;
	}
	
	
	/**
	 * resolve an arbitrary action
	 * if action is understood, the module will
	 * "resolve" it, by assigning runnable code
	 * to it, for later execution
	 * @param action
	 */
	public void resolveAction(Action action){
		final Action act = action;
		Operation oper = null;
		
		if(POINTER_ACTION_COLOR_CONCEPT.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					ConceptEntry a = act.getConceptEntry();
					BulletConcept entry = registry.get(a.getId());
					
					if(entry != null){
						ConceptEntry e = entry.getConceptEntry();
						
						// check entry id
						if(!a.getId().equals(e.getId()))
							e = e.getFeature(); 
						
						// set the satus of a concept
						e.setConceptStatus(act.getInput());
					}
					getComponent().repaint();
				}
				public void undo(){
					
				}
			};
		}else if(POINTER_ACTION_FLASH_CONCEPT.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				private BulletConcept entry;
				public void run() {
					entry = registry.get(act.getConceptEntry().getId());
					if(entry != null){
						entry.setFlashing(true);
					}
				}
				public void undo(){
					if(entry != null){
						entry.setFlashing(true);
					}
				}
			};
		}else if(POINTER_ACTION_ADD_CONCEPT_ERROR.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				private BulletConcept entry;
				public void run() {
					entry = registry.get(act.getConceptEntry().getId());
					if(entry != null){
						entry.addError(act.getInput());
					}
				}
				public void undo(){
					if(entry != null){
						entry.removeError(act.getInput());
					}
				}
			};
		}else if(POINTER_ACTION_ADD_CONCEPT.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				public void run() {
					addConceptEntry(act.getConceptEntry());
				}
				public void undo(){
					removeConceptEntry(act.getConceptEntry());
				}
			};
		}else if(POINTER_ACTION_REMOVE_CONCEPT.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				public void run() {
					removeConceptEntry(act.getConceptEntry());
				}
				public void undo(){
					addConceptEntry(act.getConceptEntry());
				}
			};
		}else if(POINTER_ACTION_LOCK_INTERFACE_TO.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
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
		}else if(POINTER_ACTION_SET_INTERACTIVE.equalsIgnoreCase(action.getAction())){
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
		}
		action.setOperation(oper);
	}
	
	
	/**
	 * get all messages that this module supports
	 * @return
	 */
	public Message [] getSupportedMessages(){
		return new Message [0];
	}
	
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions(){
		return new Action []{
					//new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_COLOR_CONCEPT,"status","Color concept node based on its status: correct, incorrect etc."),
					//new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_FLASH_CONCEPT,"","Flash concept node to draw attention to it."),
					//new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT_ERROR,"error"),
					//new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT,""),
					//new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_REMOVE_CONCEPT,""),
					new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_ADD_MESSAGE_INPUT,"type: key=value",
							"Add an arbitrary key/value pair to an input field of client event messages of a given type."),
					new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_LOCK_INTERFACE_TO,"button1,button2...",
							"Lock down the interface. Disable all buttons and menus with exception to whatever is in the input list."),
					new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"true","Enable interactive mode in a module."),
					new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"false","Disable interactive mode in a module.")	
					
		};
		
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
			if(!blockMessages){
				
				// send message about this concept's feature
				e.getFeature().setParentEntry(e);
				ClientEvent ce = e.getFeature().getClientEvent(this,Constants.ACTION_REFINE);
				ce.setInput(e.getMessageInput());
				
				//ce.setParent(e.getFeature().getObjectDescription());
				MessageUtils.getInstance(this).flushInterfaceEvents(ce);
				sendMessage(ce);
			}
		}else if("SEARCH".equals(cmd) || "SEARCH_GO".equals(cmd)){
			String s = ""+evt.getNewValue();
			if(s.length() > 0){
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_TREE,""+evt.getNewValue(),
									"SEARCH".equals(cmd)?ACTION_SEARCHING:ACTION_SEARCH);
				ie.setParent(currentDialog);
				MessageUtils.getInstance(this).addInterfaceEvent(ie);
			}
		}
	}

	/**
	 * block messages generated by this module, usefull for playback
	 */
	private void setBlockMessages(boolean b){
		blockMessages = b;
	}
	
	
	/**
	 * recieve message
	 */
	public void receiveMessage(Message msg) {
		// handle the playback messages
		if(msg.getSender() instanceof ProtocolModule){
			setBlockMessages(true);
			
			// handle adding removing concepts
			for(String type: CONCEPT_TYPES){
				if(msg.getType().equals(type)){
					// get concept entry
					ConceptEntry e = ConceptEntry.getConceptEntry(msg.getObjectDescription());
					e.setAuto(msg.isAuto());
					
					if(msg.getEntireConcept() != null)
						e.setParentEntry(ConceptEntry.getConceptEntry(msg.getEntireConcept()));
					
					// action added
					if(ACTION_ADDED.equals(msg.getAction())){
						// take care of an attribute
						if(TYPE_ATTRIBUTE.equals(msg.getType())){
							ConceptEntry of = getConceptEntry(e.getFeature());
							if(of != null){
								of = of.getParentEntry();
							}
							
							//could not find the parent
							if(of == null){
								Config.getLogger().severe("Skipping playback of the message: "+msg);
								continue;
							}
								
							// create new finding
							//ConceptEntry nf = e.getParentEntry();
							//nf.setConceptStatus(of.getConceptStatus());
							//nf.addAttribute(e);
							
							// create list of attributes
							List<ConceptEntry> attributes = new ArrayList<ConceptEntry>();
							attributes.addAll(of.getAttributes());
							attributes.add(e);
							
							// create compound concept
							ConceptEntry nf = createFinding(e.getFeature(),attributes,expertModule.getDomainOntology());
							if(msg.getEntireConcept() != null)
								nf.setId(ConceptEntry.getConceptEntry(msg.getEntireConcept()).getId());
							
							// refine concept
							refineConceptEntry(of,nf);
						}else if(e instanceof LinkConceptEntry){
							LinkConceptEntry link = (LinkConceptEntry) e;
							
							// reset source and destination nodes
							ConceptEntry src = getConceptEntry(link.getSourceConcept());
							ConceptEntry dst = getConceptEntry(link.getDestinationConcept());
							
							if(src != null && dst != null){
								link.setSourceConcept(src);
								link.setDestinationConcept(dst);
								
								// should handle null
								addConceptEntry(link);
							}
						}else{
							// all other concepts are added
							if(msg.getType().equals(TYPE_FINDING)){
								PresentationModule v = getTutor().getPresentationModule();
								Object marker = v.getIdentifiedFeature();
								// if marker is there, it means it was added through interface event earlier
								// if not, then lets create it on the spot
								if(marker == null && msg.getInputMap().containsKey("location")){
									v.setIdentifiedFeature(TextHelper.parseRectangle(msg.getInputMap().get("location")));
									marker = v.getIdentifiedFeature();
								}
								e.setInput(marker);
								v.stopFeatureIdentification();
							}
							addConceptEntry(e);
						}
					}else if(ACTION_REMOVED.equals(msg.getAction())){
						ConceptEntry r = getConceptEntry(e);
						//r.setParentEntry(getConceptEntry(e.getParentEntry()));
						if(r != null)
							removeConceptEntry(r);
					}
				
					break;
				}
			}
			// handle moving of nodes
			if(TYPE_NODE.equals(msg.getType()) && ACTION_MOVED.equals(msg.getAction())){
				ConceptEntry e = getConceptEntry(msg.getLabel());
				if(e != null){
					Map<String,String> map = (Map<String,String>) msg.getInput();
					NodeConcept n = NodeConcept.getNodeConcept(e,this);
					if(n != null){
						n.setLocation(new Point(Integer.parseInt(map.get("x")),Integer.parseInt(map.get("y"))));
						conceptPanel.repaint();
					}
				}
			}
		
			setBlockMessages(false);
			return;
		}
		
		
		// catch viewer location change
		if(!(msg instanceof TutorResponse) && TYPE_PRESENTATION.equals(msg.getType()) && ACTION_VIEW_CHANGE.equals(msg.getAction())){
			Map map = (Map) msg.getInput();
			int x = Integer.parseInt(""+map.get("x"));
			int y = Integer.parseInt(""+map.get("y"));
			int w = Integer.parseInt(""+map.get("width"));
			int h = Integer.parseInt(""+map.get("height"));
			double s = Double.parseDouble(""+map.get("scale"));
			view = new ViewPosition(x,y,w,h,s);
		}
	}
	
	/**
	 * set concept status for inner components of outer concept
	 * @param o - outer concept
	 * @param i - inner concept
	 * @param r - response
	 */
	private void setConceptStatus(ConceptEntry o, ConceptEntry i, String r){
		System.out.println(i+" in "+o);
		// set feature status
		ConceptEntry f = o.getFeature();
		if(f != null && OntologyHelper.hasSubClass(f,i,expertModule.getDomainOntology()))
			f.setConceptStatus(r);
			
		// set attribute status
		for(ConceptEntry a : o.getAttributes()){
			if(OntologyHelper.hasSubClass(a,i,expertModule.getDomainOntology()))
				a.setConceptStatus(r);
		}
	}
	
	public List<ConceptEntry> getConceptEntries() {
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(BulletConcept n: registry.values()){
			list.add(n.getConceptEntry());
		}
		return list;
	}

	
	/**
	 * update concept entries
	 * @param e
	 * @param im
	 */
	
	private void updateConceptEntry(ConceptEntry e, InterfaceModule im){
		// add concept entry if it doesn't exist
		//addConceptEntry(e);
		
		// update things like locations
		BulletConcept n = BulletConcept.getBulletConcept(e,im);
		
		// if there is no concept to replace
		// do simple layout, else reset position
		if(n != null){
			BulletConcept.getBulletConcept(e,this).setLocation(n.getLocation());
		}
		
		//repaint
		getComponent().repaint();
	}
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	/**
	 * sync
	 */
	public void sync(InterfaceModule tm) {
		for(ConceptEntry e: tm.getConceptEntries()){
			updateConceptEntry(e, tm);
		}
	}

	public ConceptEntry getConceptEntry(String id) {
		//look up based on id
		BulletConcept e = registry.get(id);
		
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
	
	/**
	 * get concept entry that belongs to this interface
	 * @param f
	 * @return
	 */
	private ConceptEntry getConceptEntry(ConceptEntry f) {
		ConceptEntry entry = getConceptEntry(f.getId());
		if(entry == null){
			for(BulletConcept nc: registry.values()){
				ConceptEntry e = nc.getConceptEntry();
				if(e.getFeature().equals(f) || e.equals(f)){
					entry = e;
					break;
				}
			}
		}
		return entry;
	}
	
	/**
	 * check for duplicates
	 * @param e
	 * @return
	 */
	private BulletConcept getBulletConcept(ConceptEntry e){
		for(BulletConcept n: registry.values()){
			if(n.getConceptEntry().equals(e)){
				return n;
			}
		}
		return null;
	}
	
	/**
	 * add hypothesis
	 * @param name
	 */
	public void removeConceptEntry(ConceptEntry e){
		BulletConcept c = registry.get(e.getId());
		
		// remove concept
		if(c != null && !(c instanceof BulletConcept.BulletAttributeConcept)){
			// remove given concept
			removeConcept(c);
			unregisterConceptEntry(e);
			getComponent().repaint();
			e.removePropertyChangeListener(this);
			e.delete();
			
			// remove hypothesis as potential diagnosis
			removeFromDxTree(e);
			
			// send message about this concept
			notifyConceptEntry(e,ACTION_REMOVED);
		}else if(c instanceof BulletConcept.BulletAttributeConcept){
			// get finding, and remove the extra attribute
			ConceptEntry f = e.getParentEntry();
			
			// what if this is an attrribute
			ConceptEntry feature = e.getFeature();
			List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(f.getAttributes());
			attributes.remove(e);
			
			ConceptEntry n = createFinding(feature,attributes,expertModule.getDomainOntology());
			
			// if the new finding minus a given attribute returns the identical finding as an original
			// that means that attributes are inter linked and remaining attributes are only relevan
			// in the context of another. Since it is difficult to determine which attributes are related
			// and which are not, just remove all of the remaining attributes
			if(f.equals(n)){
				n = feature;
			}
			
			// now refine
			refineConceptEntry(f,n);
		}
	}
	
	/**
	 * tree selection listener
	 */
	public void valueChanged(TreeSelectionEvent e) {
		// notify tree selection
		TreePath p = e.getPath();
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_TREE,TextHelper.toString(p),
				(e.getNewLeadSelectionPath() != null)?ACTION_SELECTED:ACTION_DESELECTED);
		ie.setParent(currentDialog);
		MessageUtils.getInstance(this).addInterfaceEvent(ie);
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}


	public void treeCollapsed(TreeExpansionEvent event) {
		// TODO Auto-generated method stub
		
	}


	public void treeExpanded(TreeExpansionEvent event) {
		// notify tree selection
		TreePath p = event.getPath();
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_TREE,TextHelper.toString(p),ACTION_EXPANDED);
		ie.setParent(currentDialog);
		MessageUtils.getInstance(this).addInterfaceEvent(ie);
	}

}
