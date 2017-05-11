package edu.pitt.dbmi.tutor.modules.interfaces;



import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
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
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.modules.interfaces.NodeConcept.SupportLinkConcept;
import edu.pitt.dbmi.tutor.ui.GlossaryManager;
import edu.pitt.dbmi.tutor.ui.TreeDialog;
import edu.pitt.dbmi.tutor.ui.TreePanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.markers.Annotation;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;

/**
 * this is our beloved arc-n-node interface
 * @author Eugene Tseytlin
 *
 */
public class ArcNodeInterface implements InterfaceModule, ActionListener, PropertyChangeListener, TreeSelectionListener, TreeExpansionListener {
	private final String FINDINGS = "FINDINGS";
	private final String HYPOTHESES = "HYPOTHESES";
	private final String ATTRIBUTES = "QUALITIES";
	private final String DIAGNOSES = "DIAGNOSES";
	
	private Properties defaultConfig;
	private ConceptPanel conceptPanel;
	private JToolBar toolbar;
	private JPopupMenu popup;
	private JMenu menu;
	private AbstractButton selectedButton;
	private Map<String,NodeConcept> registry;
	private Map<String,TreeNode> treeMap;
	private TreeDialog findingDialog, hypothesisDialog,diagnosisDialog,attributeDialog;
	private DefaultMutableTreeNode diagnosisRoot;
	private ExpertModule expertModule;
	private CaseEntry caseEntry;
	private Tutor tutor;
	private boolean interactive;
	private ViewPosition view;
	private JPanel component;
	private String currentDialog;
	//private List<InterfaceEvent> pendingInterfaceEvents;
	private List<Action> annotationActions;
	private boolean blockMessages;
	private Color absentFindingColor;
	// behavior modifiers
	private boolean behaviorAttributeFindingMode, behaviorAllAttributeMode,behaviorAllDiagnosisMode;
	private boolean behaviorShowGlossary,behaviorShowExample,behaviorShowRefute;
	private String [] lockInterfaceExceptions;
	private Map<String,Map<String,String>> auxCEInputs;
	//private List<Popup> glossaryWindows;
	private GlossaryManager glossaryManager;
	
	public void load(){
		//NOOP:
		treeMap = new HashMap<String,TreeNode>();
	}
	
	
	
	/**
	 * get component
	 */
	public Component getComponent() {
		if(component == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.setBackground(Config.getColorProperty(this,"component.background"));
			panel.setOpaque(true);
			
			
			if(conceptPanel == null){
				registry = new HashMap<String, NodeConcept>();
				diagnosisRoot = new DefaultMutableTreeNode("DIAGNOSES");
				
				conceptPanel  = new ConceptPanel(this);
				conceptPanel.addPropertyChangeListener(this);
			}
			
			// create listener
			//listener = new ConceptListener();
			//addMouseListener(listener);
			//addMouseMotionListener(listener);
			
			// add behavior modifiers
			behaviorAttributeFindingMode = "attribute".equals(Config.getProperty(this,"behavior.finding.mode"));
			behaviorAllAttributeMode     =  "all".equals(Config.getProperty(this,"behavior.attribute.mode"));
			behaviorAllDiagnosisMode = "all".equals(Config.getProperty(this,"behavior.diagnosis.mode"));
			behaviorShowExample = Config.getBooleanProperty(this,"behavior.glossary.show.example");
			behaviorShowGlossary = Config.getBooleanProperty(this,"behavior.glossary.enabled");
			behaviorShowRefute = Config.getBooleanProperty(this,"behavior.refute.link.enabled");
			absentFindingColor = Config.getColorProperty(this,"tree.absent.finding.color");
			
			
			// get orientation params
			boolean th = "horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation"));
			
			// add components
			panel.add(getToolBar(th),(th)?BorderLayout.NORTH:BorderLayout.WEST);
			panel.add(conceptPanel,BorderLayout.CENTER);
			//add(diagnoses,(ch)?BorderLayout.EAST:BorderLayout.SOUTH);
			
			// set preferred size
			panel.setPreferredSize(Config.getDimensionProperty(this,"component.size"));	
			
			// create component panel
			component = new JPanel();
			component.setLayout(new CardLayout());
			component.add(panel,"main");
		}
		return component;
	}
	
	public void reconfigure(){
		// add behavior modifiers
		behaviorAttributeFindingMode = "attribute".equals(Config.getProperty(this,"behavior.finding.mode"));
		behaviorAllAttributeMode     =  "all".equals(Config.getProperty(this,"behavior.attribute.mode"));
		behaviorAllDiagnosisMode = "all".equals(Config.getProperty(this,"behavior.diagnosis.mode"));
		behaviorShowExample = Config.getBooleanProperty(this,"behavior.glossary.show.example");
		behaviorShowGlossary = Config.getBooleanProperty(this,"behavior.glossary.enabled");
		
		if(component != null){
			// get orientation params
			boolean th = "horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation"));
		
			// change orientation
			JToolBar t = getToolBar();
			t.setOrientation((th)?JToolBar.HORIZONTAL:JToolBar.VERTICAL);
			
			// add components
			JPanel panel = (JPanel) component.getComponent(0);
			panel.remove(t);
			panel.add(t,(th)?BorderLayout.NORTH:BorderLayout.WEST);
			
			// set preferred size
			component.setPreferredSize(Config.getDimensionProperty(this,"component.size"));	
			conceptPanel.reconfigure();
		}
		
		// reset trees based on changes
		setExpertModule(expertModule);
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
			toolbar.add(UIHelper.createToggleButton("support.link","Draw Support Link",
					UIHelper.getIcon(this,"icon.toolbar.support.link",24),this));
			if(behaviorShowRefute)
				toolbar.add(UIHelper.createToggleButton("refute.link","Draw Refute Link",
						UIHelper.getIcon(this,"icon.toolbar.refute.link",24),this));
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
			menu.add(UIHelper.createMenuItem("support.link","Draw Support Link",
					UIHelper.getIcon(this,"icon.menu.support.link",16),this));
			if(behaviorShowRefute)
				menu.add(UIHelper.createMenuItem("refute.link","Draw Refute Link",
						UIHelper.getIcon(this,"icon.menu.refute.link",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("clinical.info","View Clinical Information",
					UIHelper.getIcon(this,"icon.menu.clinical.info",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("Done","Finish Case",
					UIHelper.getIcon(this,"icon.menu.done",16),this));
			
			
			// put in debug options
			JMenu debug = ITS.getInstance().getDebugMenu();
			if(!UIHelper.hasMenuItem(debug,"Show Slide Annotations")){
				debug.add(UIHelper.createMenuItem("goal.list","Preview Case Information ..",
						  Config.getIconProperty("icon.menu.preview"),this),0);
				debug.add(UIHelper.createCheckboxMenuItem("show.annotations","Show Slide Annotations",
						  Config.getIconProperty("icon.menu.annotation"),this),1);
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
		NodeConcept c = conceptPanel.getSelectedNode();
		if(c != null){
			if(c instanceof NodeConcept.AttributeConcept ||
			   c instanceof NodeConcept.SupportLinkConcept){
				popup.getComponent(0).setEnabled(false);
				popup.getComponent(1).setEnabled(false);	
			}else {
				popup.getComponent(0).setEnabled(true);
				popup.getComponent(1).setEnabled(true);
			}
			
			// enable location
			popup.getComponent(2).setEnabled(TYPE_FINDING.equals(c.getConceptEntry().getType()));
			
			// set name for remove
			setButtonNames(c,popup);
			
			// disable some buttons that don't fit the exceptions
			// set in the action
			if(lockInterfaceExceptions != null){
				UIHelper.setEnabled(popup,true);
				UIHelper.setEnabled(popup,lockInterfaceExceptions,false);
			}
			
			// disable glossary
			if(!behaviorShowGlossary)
				popup.getComponent(1).setEnabled(false);
		}
		
		return popup;
	}
	
	/**
	 * set remove button name
	 * @param c
	 * @param bt
	 */
	private void setButtonNames(NodeConcept c, Container container){
		AbstractButton dq = (AbstractButton)container.getComponent(0);
		AbstractButton rm = (AbstractButton)container.getComponent(4);
		
		// set default specify string
		if(behaviorAttributeFindingMode)
			dq.setText("Further Specify");
		
		if(c instanceof NodeConcept.AttributeConcept){
			rm.setText("Remove Modifier");
			if(behaviorAttributeFindingMode)
				dq.setText("Describe Qualities");
		}else if(c instanceof NodeConcept.FindingConcept){
			rm.setText("Remove Finding");
			if(behaviorAttributeFindingMode)
				dq.setText("Describe Qualities");
		}else if(c instanceof NodeConcept.DiagnosisConcept){
			rm.setText("Remove Diagnosis");
		}else if(c instanceof NodeConcept.HypothesisConcept){
			rm.setText("Remove Hypothesis");
		}else if(c instanceof NodeConcept.SupportLinkConcept){
			rm.setText("Remove Link");
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
				" are represented as colored nodes on a graph connected by " +
				"support and refute links";
	}

	/**
	 * get name of this component
	 */
	public String getName() {
		return "Arc and Node Interface";
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	/**
	 * get version
	 */
	public String getVersion() {
		return "1.0";
	}


	/**
	 * dispose of resources
	 */
	public void dispose() {
		reset();
		treeMap.clear();
		//conceptPanel = null;
	}
		
	public void reset(){
		if(annotationActions != null){
			for(Action a: annotationActions){
				a.undo();
			}
			annotationActions = null;
		}
		if(glossaryManager != null)
			glossaryManager.reset();
		lockInterfaceExceptions = null;
		if(conceptPanel != null)
			conceptPanel.reset();
		if(registry != null)
			registry.clear();
		if(diagnosisRoot != null)
			diagnosisRoot.removeAllChildren();
		getAuxInputMap().clear();
	}
	
	/**
	 * perform actions
	 */
	public void actionPerformed(ActionEvent e) {
		if(!isInteractive())
			return;
		
		String cmd = e.getActionCommand();
		
		// unselect previous button
		if(selectedButton != null){
			selectedButton.setSelected(false);
			selectedButton = null;
			conceptPanel.stopSketchLink();		
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
		}else if(cmd.equalsIgnoreCase("support.link")){
			selectedButton = (AbstractButton) e.getSource();
			if(selectedButton.isSelected())
				conceptPanel.sketchSupportLink();
			else 
				conceptPanel.stopSketchLink();
		}else if(cmd.equalsIgnoreCase("refute.link")){
			selectedButton = (AbstractButton) e.getSource();
			if(selectedButton.isSelected())
				conceptPanel.sketchRefuteLink();
			else 
				conceptPanel.stopSketchLink();
		}else if(cmd.equalsIgnoreCase("done")){
			doDone();
		}else if(cmd.equalsIgnoreCase("clinical.info")){
			doClinicalInfo();
		}else if(cmd.equalsIgnoreCase("delete")){
			doDelete();
		}else if(cmd.equalsIgnoreCase("glossary")){
			doGlossary();
		}else if(cmd.equalsIgnoreCase("location")){
			if(conceptPanel.getSelectedNode() != null){
				conceptPanel.getSelectedNode().showIdentifiedFeature();
			}
		}else if(cmd.equalsIgnoreCase("specify")){
			NodeConcept node = conceptPanel.getSelectedNode();
			if(node != null){
				if(behaviorAttributeFindingMode && 
				   node instanceof NodeConcept.FindingConcept)
					doAttributes();
				else
					doSpecify();
			}
		}else if(cmd.equalsIgnoreCase("goal.list")){
			doShowCase();
		}else if(cmd.equalsIgnoreCase("show.annotations")){
			doShowAnnotations(((AbstractButton)e.getSource()).isSelected());
		}
		
		// clear popup concept
		conceptPanel.clearPopupSelection();
	}
	
	
	/**
	 * show case information
	 */
	private void doShowCase(){
		// create dialog
		Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show Case Information",ACTION_OPENED));
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
			List<NodeConcept> nodes = conceptPanel.getSelectedNodes();
			if(nodes.isEmpty()){
				annotationActions.add(new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL));
				Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show All Annotations",ACTION_SELECTED));
			}else{
				for(NodeConcept node: nodes){
					for(ConceptEntry e : OntologyHelper.getMatchingFindings(caseEntry,node.getConceptEntry())){
						Action action = new Action(PresentationModule.class.getSimpleName(),
											POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL);
						action.setConceptEntry(e);
						annotationActions.add(action);
						// notify debug
						InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show Annotation",ACTION_SELECTED);
						ie.setObjectDescription(e.getObjectDescription());
						Communicator.getInstance().sendMessage(ie);
					}
				}
			}
			
			// now execute those actions
			for(Action a: annotationActions){
				Communicator.getInstance().resolveAction(a);
				a.run();
			}
		}else{
			Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Show All Annotations",ACTION_DESELECTED));
		}
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
		TreeDialog d = TreeDialog.createDialog(UIHelper.getWindow(component));
		d.getTreePanel().addTreeSelectionListener(this);
		d.getTreePanel().addTreeExpansionListener(this);
		d.getTreePanel().addPropertyChangeListener(this);
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
			diagnosisDialog.setRoot((diagnosisRoot.getChildCount() > 0)?diagnosisRoot:null);
		
		return diagnosisDialog;
	}
	
	/**
	 * raise curtain during finding identification
	 * @param b
	 */
	private void setRaiseCurtain(boolean raise){
		PresentationModule viewer = getTutor().getPresentationModule();
		Component c = (component.getComponentCount() > 0)?component.getComponent(0):component;
		if(raise){
			c.setBackground(new Color(240,240,240));
			c.setCursor(UIHelper.getChildCursor(viewer.getComponent()));
		}else{
			c.setBackground(Config.getColorProperty(ArcNodeInterface.this,"component.background"));
			c.setCursor(Cursor.getDefaultCursor());
		}
		c.repaint();
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
					List<ConceptEntry> entries = new ArrayList<ConceptEntry>();
					for(TreePath name: selection){
						ConceptEntry e = new ConceptEntry(name,TYPE_FINDING);
						e.setInput(input);
						entries.add(e);
					}
					// do a two step process to handle multiple X tag issue on duplicated nodes
					for(ConceptEntry e: entries)
						addConceptEntry(e);
				}else if(input != null){
					viewer.removeIdentifiedFeature(input);
					unselectButton();
				}
				
				// notify if this was canceled
				if(selection.length == 0 && !blockMessages)
					MessageUtils.getInstance(ArcNodeInterface.this).flushInterfaceEvents(null);
			
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
		currentDialog =  d.getTitle();
		d.setVisible(true);
		for(TreePath name: d.getSelectedNodes()){
			ConceptEntry e = new ConceptEntry(name,TYPE_ABSENT_FINDING);
			e.setInput(view);
			addConceptEntry(e);
		}
		
		// notify if this was canceled
		if(d.getSelectedNodes().length == 0 && !blockMessages){
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
		}
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
		currentDialog = d.getTitle();
		d.setVisible(true);
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
		currentDialog = d.getTitle();
		d.setVisible(true);
		for(TreePath name: d.getSelectedNodes())
			addConceptEntry(new ConceptEntry(name,TYPE_DIAGNOSIS));
		
		// notify if this was canceled
		if(d.getSelectedNodes().length == 0 && !blockMessages)
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
		
	}
	
	
	/**
	 * add new concept
	 */
	private void doDone(){
		if(blockMessages)
			return;
		
		if(isDone()){
			ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_DONE,getClass().getSimpleName(),ACTION_REQUEST);
			if(!blockMessages)
				MessageUtils.getInstance(this).flushInterfaceEvents(ce);
			sendMessage(ce);
		}else{
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
			String sb = "You have not provided the minimum information required to finish this case.";
        	JOptionPane.showMessageDialog(Config.getMainFrame(),sb,"Error",JOptionPane.ERROR_MESSAGE);
        }
	}
	
	
	/**
	 * is user allowed to submit the case
	 * @return
	 */
	public boolean isDone() {
		// check settings
		boolean enforceFindings = Config.getBooleanProperty(this,"behavior.enforce.finding.on.done");
		boolean enforceDiagnosis = Config.getBooleanProperty(this,"behavior.enforce.diagnosis.on.done");
		
		// if nothing is enforced, great!
		if(!enforceFindings && ! enforceDiagnosis)
			return true;
		
		// looka at what is asserted
		boolean ef = !enforceFindings, ed = !enforceDiagnosis;
		for(ConceptEntry e: getConceptEntries()){
			if(enforceFindings && TYPE_FINDING.equals(e.getType()))
				ef = true;
			if(enforceDiagnosis && TYPE_DIAGNOSIS.equals(e.getType()))
				ed = true;
			
			// if both findings and diagnosis are present
			if(ef && ed)
				return true;
				
		}
		return false;
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
		NodeConcept node = conceptPanel.getSelectedNode();
		if(node != null){
			ConceptEntry e = node.getConceptEntry().getFeature();
			
			// notify of action
			if(!blockMessages){
				ClientEvent ce = e.getClientEvent(this,Constants.ACTION_GLOSSARY);
				MessageUtils.getInstance(this).flushInterfaceEvents(ce);
				sendMessage(ce);
			}
			// resolve node
			expertModule.resolveConceptEntry(e);
			
			// show glossary panel
			//showGlossaryPanel(node);
			if(glossaryManager == null){
				glossaryManager = new GlossaryManager();
				glossaryManager.setShowExampleImage(behaviorShowExample);
			}
		
			// show glossary
			Point loc = node.getLocation();
			SwingUtilities.convertPointToScreen(loc,conceptPanel);
			glossaryManager.showGlossary(e,getComponent(),loc);
		}
	}
	
	/**
	 * do specify
	 */
	private void doSpecify(){
		NodeConcept node = conceptPanel.getSelectedNode();
		if(node != null){
			ConceptEntry c = node.getConceptEntry();
			expertModule.resolveConceptEntry(c);
			ConceptEntry r = c.getFeature();
			TreeNode root = (node instanceof NodeConcept.HypothesisConcept)?treeMap.get(HYPOTHESES):treeMap.get(FINDINGS);
			TreeDialog d = TreeDialog.createDialog(UIHelper.getWindow(component));
			d.setRoot(OntologyHelper.getSubTree(root,r.getName()));
			d.setSelectionMode(TreeDialog.SINGLE_SELECTION);
			d.setSelectedNode(c.getName());
			d.setVisible(true);
			TreePath path = d.getSelectedNode();
			if(path != null){
				ConceptEntry e = new ConceptEntry(path,c.getType());
				if(e.isFinding()){
					e.setFeature(c.getFeature());
					for(ConceptEntry a: c.getAttributes()){
						e.addAttribute(a);
						// reset the parent finding back to the original
						// undoes the previous call, this is useful if there
						// are some attributes that belong to parent, but not child (sibling)
						a.setParentEntry(c);
					}
					if(!c.equals(e))
						refineConceptEntry(c,e);
				}else{
					refineConceptEntry(c, e);
				}
			}
		}
	}
	
	
	/**
	 * do specify
	 */
	private void doAttributes(){
		NodeConcept node = conceptPanel.getSelectedNode();
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
				d = TreeDialog.createDialog(UIHelper.getWindow(component));
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
			}
			
			// notify if this was canceled
			if(d.getSelectedNodes().length == 0 && !blockMessages){
				MessageUtils.getInstance(this).flushInterfaceEvents(null);
			}
		}
	}
	
	/**
	 * delete concept
	 */
	private void doDelete(){
		List<NodeConcept> list = new ArrayList<NodeConcept>(conceptPanel.getSelectedNodes());
		if(list.isEmpty()){
			JOptionPane.showMessageDialog(component,"No concepts were selected","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		// delete selection			
		for(NodeConcept c : list){
			removeConceptEntry(c.getConceptEntry());
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
			treeMap.put(ATTRIBUTES,expertModule.getTreeRoot(OntologyHelper.ATTRIBUTES,new OntologyHelper.AttributeFilter(expertModule.getDomainOntology())));
		
		// set diagnosis tree
		if(behaviorAllDiagnosisMode)
			treeMap.put(DIAGNOSES,treeMap.get(HYPOTHESES));
		
		// clear dialogs
		findingDialog = attributeDialog = diagnosisDialog = hypothesisDialog = null;
	}
	
	
	/**
	 * check for duplicates
	 * @param e
	 * @return
	 */
	private NodeConcept getNodeConcept(ConceptEntry e){
		// check if such concept already exists
		/*
		if(registrySet.contains(e.getName())){
			// the only way we can add something that has
			// the same name is if it is a diagnosis
			if(Constants.TYPE_DIAGNOSIS.equals(e.getType())){
				for(NodeConcept n: registry.values()){
					if( n instanceof NodeConcept.DiagnosisConcept && 
						n.getConceptEntry().getName().equals(e.getName())){
						return true;
					}
				}
				return false;
			}else{
				return true;
			}
			
		}
		*/
		for(NodeConcept n: registry.values()){
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
	public void addConceptEntry(ConceptEntry e){
		// unselect button if it was selected
		unselectButton();
		
		// handle null 
		if(e == null)
			return;
		
		// check for duplicates?
		NodeConcept o = getNodeConcept(e);
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
		
		// check if there is an opposite link between the same values
		removeSimilarLink(e);
				
		// resolve concept to check for attributes and feature
		expertModule.resolveConceptEntry(e);
		
		// add new concept
		NodeConcept c = NodeConcept.createNodeConcept(e,this);
		conceptPanel.addConcept(c);
		conceptPanel.layoutConcept(c);
			
		// register it
		registerConceptEntry(e);
		
		// add hypothesis as potential diagnosis
		addToDxTree(e);
		
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
	 * unselect button
	 */
	private void unselectButton(){
		if(selectedButton != null){
			selectedButton.setSelected(false);
			selectedButton = null;
		}
	}
	
	
	/**
	 * remove similar link if it exists.
	 * @param e
	 */
	private void removeSimilarLink(ConceptEntry e){
		// check if there is an opposite link between the same values
		if(e instanceof LinkConceptEntry){
			// create an invers of this link
			ConceptEntry dup = e.clone();
			dup.setType(TYPE_SUPPORT_LINK.equals(dup.getType())?TYPE_REFUTE_LINK:TYPE_SUPPORT_LINK);
			
			// check if inverse exist
			ConceptEntry link = getConceptEntry(dup.getType(),dup.getName());
			if(link != null){
				removeConceptEntry(link);
			}
		}
	}
	
	/**
	 * relink existing links
	 * @param p
	 * @param c
	 */
	private void updateLinks(NodeConcept p, NodeConcept c){
		// check if there are links that concept to old concept
		List<NodeConcept.SupportLinkConcept> slinked  = new ArrayList<NodeConcept.SupportLinkConcept>();
		List<NodeConcept.SupportLinkConcept> dlinked  = new ArrayList<NodeConcept.SupportLinkConcept>();
		if(!(c instanceof SupportLinkConcept)){
			for(NodeConcept n: registry.values()){
				if(n instanceof NodeConcept.SupportLinkConcept){
					NodeConcept.SupportLinkConcept link = (NodeConcept.SupportLinkConcept) n;
					if(p.equals(link.getSource()))
						slinked.add(link);
					else if(p.equals(link.getDestination()))
						dlinked.add(link);		
				}
			}
		}
		
		// move links to new concept
		for(NodeConcept.SupportLinkConcept link: slinked){
			unregisterConceptEntry(link.getConceptEntry());
			// re-register links
			link.setSource(c);
			registerConceptEntry(link.getConceptEntry());
		}
		
		// move links to new concept
		for(NodeConcept.SupportLinkConcept link: dlinked){
			registerConceptEntry(link.getConceptEntry());
			link.setDestination(c);
			unregisterConceptEntry(link.getConceptEntry());
		}
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
	 * remove from Dx tree
	 * @param parent
	 */
	private void removeFromDxTree(ConceptEntry parent){
		// remove old hypothesis as potential diagnosis
		if(TYPE_HYPOTHESIS.equals(parent.getType())) {
			for(int i=0;i<diagnosisRoot.getChildCount();i++){
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) diagnosisRoot.getChildAt(i);
				if(node.getUserObject().equals(parent.getName())){
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
	public void refineConceptEntry(ConceptEntry parent, ConceptEntry child){
		// unselect button if it was selected
		unselectButton();
		
		// handle null 
		if(child == null || parent == null || parent.equals(child))
			return;
			
		// check for duplicates?
		NodeConcept o = getNodeConcept(child);
		if(o != null){
			// flash me
			o.flash();
			
			// well, if we are refining parent to child
			// and child exists already, then we should
			// just remove parent
			parent.setAuto(true);
			for(ConceptEntry a: parent.getAttributes())
				if(child.getAttributes().contains(a))
					a.setAuto(true);
			
			// ok now remove parent
			removeConceptEntry(parent);
			return;
		}
		
		// resolve both parent and child
		expertModule.resolveConceptEntry(parent);
		expertModule.resolveConceptEntry(child);
		
		// get parent Concept
		NodeConcept p = NodeConcept.getNodeConcept(parent,this);
					
		// add new concept
		NodeConcept c = NodeConcept.createNodeConcept(child,this);
		conceptPanel.addConcept(c);
	
		// place new concept on top of old concept
		parent.copyTo(child);
		//c.getConceptEntry().setParentEntry(parent);
		c.setLocation(p.getLocation());
		c.flash();
			
		// check if there are links that concept to old concept
		updateLinks(p,c);
			
		// remove  old concept from registry
		conceptPanel.removeConcept(p);
		unregisterConceptEntry(parent);
			
		
		// do clenup	
		conceptPanel.repaint();
		parent.delete();
			
		// remove old hypothesis as potential diagnosis
		removeFromDxTree(parent);
				
		// add new concept to registry	
		registerConceptEntry(child);
		
		// add new hypothesis as potential diagnosis
		addToDxTree(child);
		
		// if features are the same, then we are adding/removing attributes, else refining
		if(child.isFinding() && parent.isFinding() && child.getFeature().equals(parent.getFeature())){
			if(!blockMessages){
				// send remove events
				for(ConceptEntry a: getMissingAttributes(child,parent)){
					Communicator.getInstance().sendMessage(a.getClientEvent(this,ACTION_REMOVED));
				}
								
				// send add events
				for(ConceptEntry a: getMissingAttributes(parent, child)){
					Communicator.getInstance().sendMessage(a.getClientEvent(this,ACTION_ADDED));
				}
			}
		}else{
			notifyConceptEntry(parent,ACTION_REMOVED);
			notifyConceptEntry(child,ACTION_ADDED);
		}
		
		
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
	 * remove orphaned links
	 * @param c
	 */
	private void removeOrphanedLinks(NodeConcept c){
		// check if links to a concept
		List<ConceptEntry> linked  = new ArrayList<ConceptEntry>();
		if(!(c instanceof SupportLinkConcept)){
			for(NodeConcept n: registry.values()){
				if(n instanceof NodeConcept.SupportLinkConcept){
					NodeConcept.SupportLinkConcept link = (NodeConcept.SupportLinkConcept) n;
					if(c.equals(link.getSource()) || c.equals(link.getDestination())){
						linked.add(link.getConceptEntry());
					}
				}
			}
		}
		// remove linked
		for(ConceptEntry link: linked)
			removeConceptEntry(link);	
	}
	
	
	/**
	 * add hypothesis
	 * @param name
	 */
	public void removeConceptEntry(ConceptEntry e){
	
		NodeConcept c = registry.get(e.getId());
		
		// remove concept
		if(c != null && !(c instanceof NodeConcept.AttributeConcept)){
			// check if links to a concept
			removeOrphanedLinks(c);
			
			// remove given concept
			conceptPanel.removeConcept(c);
			unregisterConceptEntry(e);
			conceptPanel.repaint();
			e.delete();
			
			// remove hypothesis as potential diagnosis
			removeFromDxTree(e);
			
			// send message about this concept
			notifyConceptEntry(e,ACTION_REMOVED);
			
		}else if(c instanceof NodeConcept.AttributeConcept){
			// get finding, and remove the extra attribute
			ConceptEntry f = e.getParentEntry();
			
			// what if this is an attrribute
			ConceptEntry feature = e.getFeature();
			List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(f.getAttributes());
			attributes.remove(e);
			
			// get more specific concept w/out attribute
			//ConceptEntry n  = new ConceptEntry(OntologyHelper.getCompoundConcept(
			//	expertModule.getDomainOntology(),feature,attributes),f.getType());
			// resolve the entry and reset all unique IDs
			//n.setFeature(feature);
			//n.setAttributes(attributes);
			
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
	 * set case entry
	 */
	public void setCaseEntry(CaseEntry problem) {
		caseEntry = problem;
		//reset();
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
			UIHelper.setEnabled(getPopupMenu(),b);
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
	
	public Tutor getTutor() {
		return tutor;
	}

	public void setTutor(Tutor tutor) {
		this.tutor = tutor;
	}

	/**
	 * set concept status for inner components of outer concept
	 * @param o - outer concept
	 * @param i - inner concept
	 * @param r - response
	 *
	private void setConceptStatus(ConceptEntry o, ConceptEntry i, String r){
		System.out.println(i+" in "+o);
		// set feature status
		ConceptEntry f = o.getParentEntry();
		if(f != null && OntologyHelper.contains(f.getName(),i.getName(),expertModule.getDomainOntology()))
			f.setConceptStatus(r);
			
		// set attribute status
		for(ConceptEntry a : o.getAttributes()){
			if(OntologyHelper.contains(a.getName(),i.getName(),expertModule.getDomainOntology()))
				a.setConceptStatus(r);
		}
	}
	*/

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
			}else if(TYPE_DONE.equals(msg.getType())){
				doDone();
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
					NodeConcept entry = registry.get(a.getId());
					if(entry != null){
						ConceptEntry e = entry.getConceptEntry();
						
						// check entry id
						if(!a.getId().equals(e.getId()))
							e = e.getFeature(); 
						
						// set the satus of a concept
						e.setConceptStatus(act.getInput());
					}
					component.repaint();
				}
				public void undo(){
					
				}
			};
		}else if(POINTER_ACTION_FLASH_CONCEPT.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				private NodeConcept entry;
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
				private NodeConcept entry;
				public void run() {
					entry = registry.get(act.getConceptEntry().getId());
					if(entry != null){
						entry.getConceptEntry().addError(act.getInput());
					}
				}
				public void undo(){
					if(entry != null){
						entry.getConceptEntry().removeError(act.getInput());
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
		}else if(POINTER_ACTION_ADD_MESSAGE_INPUT.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					//parse input
					Pattern pt = Pattern.compile("\\s*([\\w]+)\\s*\\:([\\w\\-\\s\\.]+)=(.+)");
					Matcher mt = pt.matcher(act.getInput());
					if(mt.matches()){
						String type = mt.group(1).trim();
						String key  = mt.group(2).trim();
						String val  = mt.group(3).trim();
						
						Map<String,String> map = getAuxInputMap().get(type);
						if(map == null){
							map = new HashMap<String, String>();
							getAuxInputMap().put(type,map);
						}
						map.put(key,val);
					}
				}
				public void undo(){
					//parse input
					Pattern pt = Pattern.compile("\\s*([\\w]+)\\s*\\:([\\w\\-\\s\\.]+)=(.+)");
					Matcher mt = pt.matcher(act.getInput());
					if(mt.matches()){
						String type = mt.group(1).trim();
						String key  = mt.group(2).trim();
						
						Map<String,String> map = getAuxInputMap().get(type);
						if(map == null){
							map = new HashMap<String, String>();
							getAuxInputMap().put(type,map);
						}
						map.remove(key);
					}
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
		}else if(PROPERTY_NODE_MOVED.equals(cmd)){
			if(evt.getNewValue() instanceof NodeConcept && !blockMessages){
				NodeConcept c = (NodeConcept) evt.getNewValue();
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_NODE,c.getConceptEntry().getId(),ACTION_MOVED);
				ie.setObjectDescription(c.getConceptEntry().getObjectDescription());
				Point p = c.getLocation();
				Map<String,String> map = new LinkedHashMap<String, String>();
				map.put("x",""+p.x);
				map.put("y",""+p.y);
				ie.setInput(map);
				Communicator.getInstance().sendMessage(ie);	
			}
		}else if(PROPERTY_NODE_SELECTED.equals(cmd)){
			if(evt.getNewValue() instanceof NodeConcept && !blockMessages){
				NodeConcept c = (NodeConcept) evt.getNewValue();
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_NODE,c.getConceptEntry().getId(),ACTION_SELECTED);
				ie.setObjectDescription(c.getConceptEntry().getObjectDescription());
				Communicator.getInstance().sendMessage(ie);	
				MessageUtils.getInstance(this).addInterfaceEvent(ie);
			}
		}else if(PROPERTY_NODE_DESELECTED.equals(cmd)){
			if(evt.getNewValue() instanceof NodeConcept && !blockMessages){
				NodeConcept c = (NodeConcept) evt.getNewValue();
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_NODE,c.getConceptEntry().getId(),ACTION_DESELECTED);
				ie.setObjectDescription(c.getConceptEntry().getObjectDescription());
				Communicator.getInstance().sendMessage(ie);	
				MessageUtils.getInstance(this).addInterfaceEvent(ie);
			}
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

	/**
	 * send message
	 * @param ce
	 */
	private void sendMessage(ClientEvent ce){
		//augment inputs
		if(getAuxInputMap().containsKey(ce.getType()))
			ce.getInputMap().putAll(getAuxInputMap().get(ce.getType()));
		Communicator.getInstance().sendMessage(ce);
	}
	
	
	private Map<String,Map<String,String>> getAuxInputMap(){
		if(auxCEInputs == null)
			auxCEInputs = new HashMap<String, Map<String,String>>();
		return auxCEInputs;
	}
	
	public List<ConceptEntry> getConceptEntries() {
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(NodeConcept n: registry.values()){
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
		NodeConcept n =  NodeConcept.getNodeConcept(e,im);
		NodeConcept nn = NodeConcept.getNodeConcept(e,this);
		// if there is no concept to replace
		// do simple layout, else reset position
		if(n != null && nn != null){
			nn.setLocation(n.getLocation());
		}
		
		//repaint
		component.repaint();
	}
	
	
	/**
	 * sync
	 */
	public void sync(InterfaceModule tm) {
		getComponent();
		setCaseEntry(tutor.getCase());
		for(ConceptEntry e: tm.getConceptEntries()){
			updateConceptEntry(e, tm);
		}
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
	
	private ConceptEntry getConceptEntry(String type,String name) {
		for(NodeConcept nc: registry.values()){
			ConceptEntry e = nc.getConceptEntry();
			if(e.getType().equals(type) && e.getName().equals(name)){
				return e;
			}
		}
		return null;
	}
	
	
	
	/**
	 * get concept entry that belongs to this interface
	 * @param f
	 * @return
	 */
	private ConceptEntry getConceptEntry(ConceptEntry f) {
		ConceptEntry entry = getConceptEntry(f.getId());
		if(entry == null){
			for(NodeConcept nc: registry.values()){
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
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
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

	public boolean isEnabled() {
		return (component != null)?component.isEnabled():false;
	}
	/*
	private void addInterfaceEvent(InterfaceEvent ie){
		if(pendingInterfaceEvents == null)
			pendingInterfaceEvents = new ArrayList<InterfaceEvent>();
		pendingInterfaceEvents.add(ie);
	}
	
	private void flushInterfaceEvents(ClientEvent ce){
		if(blockMessages)
			return;
		
		if(pendingInterfaceEvents == null)
			pendingInterfaceEvents = new ArrayList<InterfaceEvent>();
		for(InterfaceEvent ie: pendingInterfaceEvents){
			ie.setClientEvent(ce);
			Communicator.getInstance().sendMessage(ie);
		}
		pendingInterfaceEvents.clear();
	}
	*/
}
