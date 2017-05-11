package edu.pitt.dbmi.tutor.modules.interfaces;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ConceptFilter;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.beans.SlideEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.presentation.SimpleViewerPanel;
import edu.pitt.dbmi.tutor.modules.presentation.ViewerPanel;
import edu.pitt.dbmi.tutor.modules.protocol.ConsoleProtocolModule;
import edu.pitt.dbmi.tutor.modules.reasoning.RadiologyReasoner;
import edu.pitt.dbmi.tutor.ui.GlossaryManager;
import edu.pitt.dbmi.tutor.ui.MultipleEntryWidget;
import edu.pitt.dbmi.tutor.ui.SingleEntryWidget;
import edu.pitt.dbmi.tutor.ui.TreeDialog;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.slideviewer.ImageProperties;
import edu.pitt.slideviewer.ImageTransform;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.markers.Annotation;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

/**
 * this interface provides a minimal footprint and enables users select features
 * on the slides and suggest recommendations on them
 * @author tseytlin
 *
 */
public class RadiologyInterface implements InterfaceModule, ActionListener, PropertyChangeListener,TreeSelectionListener, TreeExpansionListener, ChangeListener {
	private final String FINDINGS = "FINDINGS";
	private final String RECOMMENDATIONS = "RECOMMENDATIONS";
	private final float BRIGHTNESS_INCREMENT = 255*.05f; // increment in 5% increments
	private final float CONTRAST_INCREMENT   = 1.05f;    // increment in 5% increments
	private boolean enabled, interactive = true, previewMode;
	private Tutor tutor;
	private Properties defaultConfig;
	private ExpertModule expertModule;
	private CaseEntry caseEntry;
	private JSplitPane component;
	private JToolBar toolbar;
	private JPanel previewPanel;
	private JTree tree;
	private DefaultMutableTreeNode root;
	private AbstractButton selectedButton;
	private Map<String,Map<String,String>> auxCEInputs;
	private boolean blockMessages;
	private Map<String,TreeNode> treeMap;
	private TreeDialog findingDialog,adviceDialog;
	private JDialog inputDialog;
	private SingleEntryWidget featureWidget,adviceWidget;
	private String currentDialog;
	private Map<String,RadiologyConcept> registry;
	private PresentationModule viewer;
	private JPopupMenu fpopup,rpopup,bpopup;
	private GlossaryManager glossaryManager;
	private Point lastLocation;
	private JPanel brightnessContrastPanel;
	private JSlider bSlider,cSlider;
	private ImageTransform transform;
	private UIHelper.RadioGroup radio;
	private JButton ok;
	private JComponent worksheet;
	private List<AbstractButton> worksheetButtons, resetButtons;
	private JTextField [] totalScoreText;
	private String [] lockInterfaceExceptions;
	
	
	public void setEnabled(boolean b) {
		enabled = b;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setInteractive(boolean b) {
		interactive = b;
	}

	public boolean isInteractive() {
		return interactive;
	}

	public Tutor getTutor() {
		return tutor;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Component getComponent() {
		if(component == null){
			
			JPanel comp = new JPanel();
			comp.setLayout(new BoxLayout(comp,BoxLayout.Y_AXIS));
			comp.add(getToolBar());
			
			previewPanel = new JPanel();
			previewPanel.setLayout(new GridLayout(-1,1));
			previewPanel.setBackground(Color.white);
			comp.add(previewPanel);
			
			// build a node panel
			root = new DefaultMutableTreeNode("Root");
			root.setAllowsChildren(true);
			tree = new JTree(new DefaultTreeModel(root)){
				public JToolTip createToolTip(){
					JToolTip tip = super.createToolTip();
					
					// we want a different background color for errors
					tip.setBackground(Color.yellow);
					tip.setBorder(new LineBorder(Color.black));
					
					return tip;
				}
			};
			tree.setRootVisible(false);
			tree.setShowsRootHandles(true);
			//tree.addTreeSelectionListener(this);
			tree.setExpandsSelectedPaths(true);
			tree.putClientProperty("JTree.lineStyle", "Horizontal");
			tree.setCellRenderer(new RadiologyConcept.CellRenderer());
			tree.setScrollsOnExpand(true);
			tree.setToggleClickCount(1);
			MouseAdapter adapter = new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					super.mousePressed(e);
					lastLocation = e.getPoint();
					int x = tree.getRowForLocation(e.getX(),e.getY());
					if(x > -1 && (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)){
						tree.setSelectionRow(x);
						JPopupMenu m = getPopupMenu();
						if(m != null)
							m.show(tree,e.getX(),e.getY());
					}
				}
				// change cursors
				public void mouseMoved(MouseEvent e) {
					tree.setCursor(Cursor.getDefaultCursor());
					tree.setToolTipText(null);
					int x = tree.getRowForLocation(e.getX(),e.getY());
					if(x > -1){
						TreePath p = tree.getPathForRow(x);
						if(p.getLastPathComponent() instanceof RadiologyConcept){
							RadiologyConcept r = (RadiologyConcept) p.getLastPathComponent();
							tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							tree.setToolTipText(r.getErrorText());
						}
					}
				}
			};
			tree.addMouseListener(adapter);
			tree.addMouseMotionListener(adapter);
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(new JScrollPane(tree),BorderLayout.CENTER);
			p.add(getBrightnessContrastPanel(),BorderLayout.SOUTH);
			
			JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			split.setTopComponent(comp);
			split.setBottomComponent(p);
			component = split;
			
			// setup image server
			ViewerFactory.setProperties(getViewerProperties());
		}
		return component;
	}

	public void reconfigure() {
		fpopup = rpopup = null;

	}

	public URL getManual() {
		return Config.getManual(getClass());
	}

	public String getName() {
		return "Radiology Interface";
	}

	public String getDescription() {
		return "Radiology Interface allows users to rapidly identify regions of interests on a medical image " +
				"and state their recommendations. It provides a minimal footprint on screen leaving most of the spcae " +
				"to display an actual medical image.";
	}

	public String getVersion() {
		return "1.0";
	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public void receiveMessage(Message msg) {
		// catch image change messages to apply transform
		if(TYPE_PRESENTATION.equals(msg.getType()) && ACTION_IMAGE_CHANGE.equals(msg.getAction())){
			// update transform
			updateTransform();
		}

	}

	public void resolveAction(Action action) {
		final Action act = action;
		Operation oper = null;
		
		if(POINTER_ACTION_COLOR_CONCEPT.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					ConceptEntry a = act.getConceptEntry();
					RadiologyConcept entry = registry.get(a.getId());
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
				private RadiologyConcept entry;
				public void run() {
					entry = registry.get(act.getConceptEntry().getId());
					if(entry != null){
						//entry.setFlashing(true);
					}
				}
				public void undo(){
					if(entry != null){
						//entry.setFlashing(true);
					}
				}
			};
		}else if(POINTER_ACTION_ADD_CONCEPT_ERROR.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				private RadiologyConcept entry;
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
					UIHelper.setEnabled((Container)getToolBar().getComponent(0), lockInterfaceExceptions,false);
					UIHelper.setEnabled(getMenu(), lockInterfaceExceptions,false);
					getPopupMenu();
					UIHelper.setEnabled(rpopup, lockInterfaceExceptions,false);
					UIHelper.setEnabled(fpopup, lockInterfaceExceptions,false);
					UIHelper.setEnabled(bpopup, lockInterfaceExceptions,false);
				}
				public void undo(){
					lockInterfaceExceptions = null;
					UIHelper.setEnabled((Container)getToolBar().getComponent(0), true);
					UIHelper.setEnabled(getMenu(), true);
					getPopupMenu();
					UIHelper.setEnabled(rpopup, true);
					UIHelper.setEnabled(fpopup, true);
					UIHelper.setEnabled(bpopup, true);
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

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void reset() {
		if(registry != null)
			registry.clear();
		lockInterfaceExceptions = null;

	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public void load() {
		ViewerFactory.setProperties(getViewerProperties());
		registry = new HashMap<String, RadiologyConcept>();
		setupConceptTrees();
	}
	
	/**
	 * setup selection trees
	 * 
	 */
	private void setupConceptTrees(){
		if(expertModule == null)
			return;
		
		ConceptFilter featureFilter = null;
		// fetch only feature findings, if in attribute mode
		//if(behaviorAttributeFindingMode)
		//	featureFilter = new OntologyHelper.FeatureFilter(expertModule.getDomainOntology());
		//else
		featureFilter = new OntologyHelper.FindingFilter(expertModule.getDomainOntology(),1);
		
		// clear all of the maps 
		treeMap = new HashMap<String,TreeNode>();
		
		//fetch standard trees
		treeMap.put(FINDINGS,expertModule.getTreeRoot(OntologyHelper.DIAGNOSTIC_FEATURES,featureFilter));
		treeMap.put(RECOMMENDATIONS,expertModule.getTreeRoot(OntologyHelper.RECOMMENDATIONS,featureFilter));
	}
	
	/**
	 * get viewer properties
	 * @return
	 */
	private Properties getViewerProperties(){
		Properties p = new Properties();
		// find viewer that is hopefull loaded
		for(TutorModule tm : Communicator.getInstance().getRegisteredModules()){
			if(tm instanceof SimpleViewerPanel || tm instanceof ViewerPanel){
				for(Object prop: tm.getDefaultConfiguration().keySet()){
					p.setProperty((String)prop,Config.getProperty(tm,(String)prop));
				}
				break;
			}
		}
		//useCaseServerInfo = Config.getBooleanProperty(this,"viewer.use.case.server.info");
		return p;
	}
	

	public void addConceptEntry(ConceptEntry e) {
		// handle null 
		if(e == null)
			return;
		
		// check for duplicates?
		if(e.isFinding()){
			RadiologyConcept o = getDuplicateConcept(e);
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
					getPresentationModule().removeIdentifiedFeature(e.getInput());
				}
				return;
			}
		}
		
		// if recommendation, remove previous recommendation
		if(TYPE_RECOMMENDATION.equals(e.getType())){
			for(String key: new ArrayList<String>(registry.keySet())){
				if(registry.containsKey(key)){
					if(registry.get(key).isRecommendation()){
						// remove recommendation
						removeConceptEntry(registry.get(key).getConceptEntry());
					}else if(registry.get(key).isWorksheet()){
						removeConceptEntry(registry.get(key).getConceptEntry());
					}
				}
			}
		}
		
				
		// resolve concept to check for attributes and feature
		expertModule.resolveConceptEntry(e);
		
		// add new concept
		RadiologyConcept c = RadiologyConcept.createRadiologyConcept(e,this);
		addConcept(c);
		
		// register it
		registerConceptEntry(e);
		
		
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
	
	private void addConcept(RadiologyConcept c){
		root.add(c);
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				((DefaultTreeModel)tree.getModel()).reload();
				//tree.repaint();
			}
		});
	
		
	}
	private void removeConcept(RadiologyConcept c){
		c.removeFromParent();
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				((DefaultTreeModel)tree.getModel()).reload();
				//tree.repaint();
			}
		});
	
		
	}

	public void refineConceptEntry(ConceptEntry p, ConceptEntry e) {
		// TODO Auto-generated method stub

	}

	public void removeConceptEntry(ConceptEntry e) {
		
		RadiologyConcept c = registry.get(e.getId());
		
		// remove concept
		if(c != null){
			
			// remove given concept
			removeConcept(c);
			unregisterConceptEntry(e);
			e.delete();
			
			// send message about this concept
			notifyConceptEntry(e,ACTION_REMOVED);
		}

	}

	public List<ConceptEntry> getConceptEntries() {
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(RadiologyConcept n: registry.values()){
			list.add(n.getConceptEntry());
		}
		return list;
	}

	public ConceptEntry getConceptEntry(String id) {
		//look up based on id
		RadiologyConcept e = registry.get(id);
		
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
	 * check for duplicates
	 * @param e
	 * @return
	 */
	private RadiologyConcept getDuplicateConcept(ConceptEntry e){
		for(RadiologyConcept n: registry.values()){
			if(n.getConceptEntry().equals(e)){
				return n;
			}
		}
		return null;
	}

	public void setExpertModule(ExpertModule module) {
		expertModule = module;
		load();
	}
	
	
	private void showPreviewPanel(){
		if(Config.getBooleanProperty(this,"behavior.show.slide.selector.on.start")){
			JFrame frame = (JFrame) Config.getMainFrame();
			if(frame != null){
				previewMode = true;
				previewPanel.setLayout(new GridLayout(1,-1));
				
				frame.setGlassPane(getPreviewPanel());
				frame.getGlassPane().setVisible(true);
			}
		}
	}

	private JPanel getPreviewPanel(){
		JLabel l = new JLabel("please select an image you want to start with");
		l.setHorizontalTextPosition(JTextField.CENTER);
		l.setHorizontalAlignment(JTextField.CENTER);
		l.setFont(l.getFont().deriveFont(30.0f));
		l.setBorder(new EmptyBorder(20,20,20,20));
		JPanel p = new JPanel();
		p.setBackground(Color.white);
		p.setBorder(new EmptyBorder(20,20,20,20));
		p.setLayout(new BorderLayout());
		p.add(l,BorderLayout.NORTH);
		p.add(previewPanel,BorderLayout.CENTER);
		p.add(Box.createRigidArea(new Dimension(60,60)),BorderLayout.SOUTH);		
		return p;
	}
	
	
	private void hidePreviewPanel(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				previewMode = false;
				((JFrame)Config.getMainFrame()).setGlassPane(new JPanel());
				((JFrame)Config.getMainFrame()).getGlassPane().setVisible(false);
				previewPanel.setLayout(new GridLayout(-1,1));
				((JComponent)component.getTopComponent()).add(previewPanel);
				((JComponent)component.getTopComponent()).revalidate();
			}
		});
	}
	
	public void setCaseEntry(CaseEntry problem) {
		reset();
		caseEntry = problem;
		transform = new ImageTransform();
		
		// show preview that obscures the view
		showPreviewPanel();
		
		// setup preview panel
		final List<SlideEntry> slides = problem.getSlides();
		final ButtonGroup bg = new ButtonGroup();
		for(SlideEntry slide : slides){
			// copy image transformations
			transform.setBrightness(slide.getImageTransform().getBrightness());
			transform.setContrast(slide.getImageTransform().getContrast());
			bSlider.setValue((int)(transform.getBrightness()/BRIGHTNESS_INCREMENT));
			cSlider.setValue((int)(Math.log(transform.getContrast())/Math.log(CONTRAST_INCREMENT)));
			
			if(slide.getThumbnail() == null){
				try{
					final ImageProperties im = ViewerFactory.getImageProperties(slide.getSlidePath());
					if(im != null && im.getThumbnail() != null){
						slide.setThumbnail(im.getThumbnail());
						slide.setImageSize(im.getImageSize());
					}
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							int of = 10;
							JPanel p = new JPanel();
							p.setLayout(new BorderLayout());
							PreviewButton pb = new PreviewButton(im);
							bg.add(pb);
							p.add(pb,BorderLayout.CENTER);
							p.setBorder(new EmptyBorder(of,of/2,of,of/2));
							p.setOpaque(false);
							previewPanel.add(p);
							previewPanel.revalidate();
							component.setDividerLocation(component.getDividerLocation()+pb.getPreferredSize().height);
							
							// make sure it is selected
							if(!previewMode && caseEntry.getPrimarySlide().getSlidePath().equals(im.getImagePath())){
								pb.setSelected(true);
							}
							
							
						}
					});
				}catch(Exception ex){
					Config.getLogger().severe(TextHelper.getErrorMessage(ex));
				}
			}
		}
	}
	
	/**
	 * register concept entry
	 * @param e
	 */
	
	private void registerConceptEntry(ConceptEntry e){
		RadiologyConcept node = RadiologyConcept.getRadiologyConcept(e,this);
		registry.put(e.getId(),node);
		
		if(e.isFinding()){
			e.addPropertyChangeListener(this);
			
			// register feature
			registry.put(e.getFeature().getId(),node);
			// register attribute
			for(RadiologyConcept a: node.getAttributes()){
				registry.put(a.getId(),a);
			}
		}
		// register attribute
		/*
		for(RadiologyConcept a: node.getRecommendations()){
			registry.put(a.getConceptEntry().getId(),a);
		}*/
		//System.out.println("REGISTER "+registry);
	}
	
	
	/**
	 * unregister concept entry
	 * @param e
	 */
	private void unregisterConceptEntry(ConceptEntry e){
		RadiologyConcept node = RadiologyConcept.getRadiologyConcept(e,this);
		registry.remove(e.getId());
		
		if(node.isFinding()){
			registry.remove(e.getFeature().getId());
			// unregister attribute
			if(node != null){
				for(RadiologyConcept a: node.getAttributes()){
					registry.remove(a.getId());
				}
				/*
				for(RadiologyConcept a: node.getRecommendations()){
					registry.remove(a.getConceptEntry().getId());
				}*/
			}
			e.removePropertyChangeListener(this);
		}else{
			
		}
		//registrySet.remove(e.getName());
		//System.out.println("UNREGISTER "+registry);
	}
	
	private boolean isDone(){
		for(RadiologyConcept e: registry.values()){
			if(e.isRecommendation())
				return true;
		}
		return false;
	}
	
	/**
	 * add new concept
	 */
	private void doDone(){
		if(blockMessages)
			return;
		
		if(isDone()){
			ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_DONE,getClass().getSimpleName(),ACTION_SUMMARY);
			if(!blockMessages)
				MessageUtils.getInstance(this).flushInterfaceEvents(ce);
			sendMessage(ce);
		}else{
			MessageUtils.getInstance(this).flushInterfaceEvents(null);
			String sb = "You have not provided the minimum information required to finish this case.";
        	JOptionPane.showMessageDialog(Config.getMainFrame(),sb,"Error",JOptionPane.ERROR_MESSAGE);
        }
	}
	
	private void doAssesment(){
		// show input dialog
		if(showAssesmentDialog()){
			unselectButton();
			// add feature
			ConceptEntry finding = ((ConceptEntry)adviceWidget.getEntry());
			
			// add birad feature
			String birad = getWorksheetString();
			finding.getProperties().put(MESSAGE_INPUT_WORKSHEET,UIHelper.getTextFromName(birad));
			addConceptEntry(finding);
			
			ConceptEntry b = new ConceptEntry(birad,TYPE_FINDING);
			addConceptEntry(b);
			
			// add one under the other
			RadiologyConcept.getRadiologyConcept(finding,this).add(RadiologyConcept.getRadiologyConcept(b,this));
			
			
		}else{
			unselectButton();	
			if(!blockMessages)
				MessageUtils.getInstance(RadiologyInterface.this).flushInterfaceEvents(null);
		}
	}
	
	
	
	
	
	/**
	 * get worksheet string
	 * @return
	 */
	public String getWorksheetString() {
		if (worksheetButtons != null) {
			Container tab = getWorksheet();
			for (int i = 0; i < worksheetButtons.size(); i++) {
				JRadioButton button = (JRadioButton) worksheetButtons.get(i);
				// if selected tab, has this button
				if(UIHelper.contains(tab,button)){
					if (button.isSelected()) {
						String cui = button.getActionCommand();
						// add string
						if (cui != null) {
							return cui;
						} else
							System.err.println("ERROR: Could not find Concept for " + cui);
					}
				}
			}
		}
	
		return "";
	}

	
	
	/**
	 * get worksheet instance
	 * @return
	 */
	private JComponent getWorksheet(){
		if(worksheet == null && expertModule != null){
			//JTabbedPane workPanel = new JTabbedPane();
			//workPanel.setPreferredSize(new Dimension(700,600));
			
			// build worksheet
			worksheetButtons = new ArrayList<AbstractButton>();
			resetButtons = new ArrayList<AbstractButton>();
			
			
			IOntology ont = expertModule.getDomainOntology();
			IClass w = ont.getClass(OntologyHelper.WORKSHEET);
			IClass [] children = w.getDirectSubClasses();
			totalScoreText = new JTextField [ children.length];
			
			// iterate over worksheets
			if(children.length > 0){
				IClass work = children[0];
				JPanel panel = new JPanel();
				// setup fonts
				Font defaultFont = panel.getFont();
				Font head = defaultFont.deriveFont(Font.BOLD);
				Font bold = defaultFont.deriveFont(Font.BOLD);
				Font plain = defaultFont.deriveFont(Font.PLAIN);
	
				// setup other panel attributes
				panel.setBackground(Color.white);
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				
				// iterate over features
				for (IClass feature: work.getDirectSubClasses()) {
					
					// if this feature is a general number panel
					if(!OntologyHelper.isNumber(feature)){
						// do normal worksheet entry
						JLabel featureLbl = createWorksheetLabel(feature,true);
						featureLbl.setFont(head);
						panel.add(featureLbl);
						ButtonGroup buttons = new ButtonGroup();
						
						// create a reset button that is not displayed
						JRadioButton bt = new JRadioButton("clear");
						buttons.add(bt);
						resetButtons.add(bt);
						
						// get direct children of scores
						IClass [] f = feature.getDirectSubClasses();
						Arrays.sort(f,new Comparator<IClass>(){
							public int compare(IClass o1, IClass o2) {
								return toText(o1).compareTo(toText(o2));
							}
						});
						
						// iterate over childrent
						for (IClass att: f){
							IClass [] a  = att.getDirectSubClasses();
							// iterate over grand children
							if (a.length > 0) {
								// sort
								Arrays.sort(a,new Comparator<IClass>(){
									public int compare(IClass o1, IClass o2) {
										return toText(o1).compareTo(toText(o2));
									}
								});
									
								JLabel attrLbl = createWorksheetLabel(att,false);
								attrLbl.setFont(bold);
								panel.add(attrLbl);
								for(int i=0;i<a.length;i++){
									AbstractButton button = createWorksheetButton(a[i]);
									button.setFont(plain);
									buttons.add(button);
									panel.add(button);
									worksheetButtons.add(button);
								}
							} else {
								AbstractButton button = createWorksheetButton(att);
								button.setFont(plain);
								buttons.add(button);
								panel.add(button);
								worksheetButtons.add(button);
							}
						}
						panel.add(new JLabel("   "));
					}
				}
				
				JScrollPane scroll = new JScrollPane(panel);
				scroll.getVerticalScrollBar().setUnitIncrement(30);
				scroll.setBorder(new EmptyBorder(5,5,5,5));
				
				// create panel
				worksheet = new JPanel();
				worksheet.setLayout(new BorderLayout());
				worksheet.setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5),new BevelBorder(BevelBorder.LOWERED)));
				JLabel l = new JLabel(UIHelper.getTextFromName(work.getName()));
				l.setHorizontalAlignment(JLabel.CENTER);
				l.setBorder(new EmptyBorder(5,5,5,5));
				worksheet.add(l,BorderLayout.NORTH);
				worksheet.add(scroll,BorderLayout.CENTER);
			}
		}
		return worksheet;
	}
	
	/**
	 * create button for worksheet
	 * @param cls
	 * @return
	 */
	private AbstractButton createWorksheetButton(IClass cls){
		// now create button
		JRadioButton button = new JRadioButton(TextHelper.formatString(toText(cls)+": "+cls.getDescription(), 80));
		button.setBackground(Color.white);
		button.setActionCommand(cls.getName());
		button.addActionListener(this);
		button.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		return button;
	}
	
	
	
	private JLabel createWorksheetLabel(IClass cls, boolean f){
		JLabel lbl;
		
		if(f)
			lbl = new UIHelper.Label(
					TextHelper.formatString("<b>"+ 
					UIHelper.getTextFromName(cls.getName()).toUpperCase()
					+":</b><br><i>" + cls.getDescription()+ "</i>", 60));
		else
			lbl = new UIHelper.Label(
					TextHelper.formatString("&nbsp;&nbsp;"+
					cls.getName() + ": "
					+ cls.getDescription(),80));
		lbl.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		return lbl;
	}
	
	
	/**
	 * convert class to string representation
	 * @param cls
	 * @return
	 */
	private String toText(IClass cls){
		String text = UIHelper.getTextFromName(cls.getName());
		// if class is numeric w/ concrete number, then get number
		if(OntologyHelper.isNumber(cls)){
			// extract number
			for(IClass c: cls.getDirectSuperClasses()){
				if(OntologyHelper.isNumber(c)){
					// convert to digits
					double d = TextHelper.parseDecimalValue(c.getName());
					if(d != OntologyHelper.NO_VALUE){
						text = TextHelper.toString(d);
					}
					break;
				}
			}
		}
		return text;
	}
	
	/**
	 * this is a preview button for an image
	 * @author tseytlin
	 */
	private class PreviewButton extends JToggleButton {
		public static final String OPEN_COMMAND = "openImage: ";
		private Image img,timg;
		private Border border;
		public PreviewButton(ImageProperties im){
			super();
			img = im.getThumbnail();
			setActionCommand(OPEN_COMMAND+im.getImagePath());
			setBackground(Color.white);
			addActionListener(RadiologyInterface.this);
			setPreferredSize(new Dimension(img.getWidth(null),img.getHeight(null)));
			border = getBorder();
		}
		
		protected void fireStateChanged() {
			super.fireStateChanged();
			setBorder((isSelected())?new LineBorder(Color.red,5):border);
		}
				
		public Dimension getPreferredSize(){
			int iw = img.getWidth(null);
			int ih = img.getHeight(null);
			int dw = 200;
			int dh = 200;
		
			// if landscape mode, then 
			if(iw < ih){
				dh = dw*ih/iw;
			}else{
				dw = dh*iw/ih;
			}
			
			return new Dimension(dw,dh);
		}
		
		public void updateImage(){
			timg = null;
			repaint();
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			int iw = img.getWidth(null);
			int ih = img.getHeight(null);
			int dw = getSize().width;
			int dh = getSize().height;
			Dimension d = getSize();
			// if landscape mode, then 
			if(ViewerHelper.isHorizontal(d,new Dimension(iw,ih))){
				dh = dw*ih/iw;
			}else{
				dw = dh*iw/ih;
			}
			int o = (dw>200)?10:2;
			int x = (d.width-dw)/2;
			int y = (d.height-dh)/2;
			
			
			if(timg == null){
				if(transform != null){
					timg = transform.getTransformedImage(img);
				}else{
					timg = img;
				}
			}
			g.drawImage(timg,x+o,y+o,dw+x-o,dh+y-o, 0,0, iw,ih,null);
		}
	}

	public JMenu getMenu() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * get menu for this interface
	 * @return
	 */
	public JPopupMenu getPopupMenu(){
		if(fpopup == null){
			fpopup = new JPopupMenu();
			if(Config.getBooleanProperty(this,"behavior.glossary.enabled"))
				fpopup.add(UIHelper.createMenuItem("glossary","Lookup Glossary",
						UIHelper.getIcon(this,"icon.menu.glossary",16),this));
			fpopup.add(UIHelper.createMenuItem("location","Show Location",
					UIHelper.getIcon(this,"icon.menu.location",16),this));
			fpopup.addSeparator();
			fpopup.add(UIHelper.createMenuItem("delete","Remove Feature",
					UIHelper.getIcon(this,"icon.menu.delete",16),this));
		}
		if(rpopup == null){
			rpopup = new JPopupMenu();
			if(Config.getBooleanProperty(this,"behavior.glossary.enabled")){
				rpopup.add(UIHelper.createMenuItem("glossary","Lookup Glossary",
						UIHelper.getIcon(this,"icon.menu.glossary",16),this));
				rpopup.addSeparator();
			}
			rpopup.add(UIHelper.createMenuItem("delete","Remove Assesment",
					UIHelper.getIcon(this,"icon.menu.delete",16),this));
		}
		if(bpopup == null && Config.getBooleanProperty(this,"behavior.glossary.enabled")){
			bpopup = new JPopupMenu();
			bpopup.add(UIHelper.createMenuItem("glossary","Lookup Glossary",
						UIHelper.getIcon(this,"icon.menu.glossary",16),this));
		}
		TreePath p = tree.getSelectionPath();
		if(p != null && p.getLastPathComponent() instanceof RadiologyConcept){
			if(((RadiologyConcept)p.getLastPathComponent()).isRecommendation()){
				return rpopup;
			}else if(((RadiologyConcept)p.getLastPathComponent()).isWorksheet()){
				return bpopup;
			}else{
				return fpopup;
			}
		}
		return null;
	}

	public JToolBar getToolBar() {
		if(toolbar == null){
			toolbar = new UIHelper.ToolBar();
			toolbar.setFloatable(false);
			//toolbar.add(UIHelper.createToggleButton("Identify","Identify Finding on an Image [Alt-F]",
			//		   UIHelper.getIcon(this,"icon.toolbar.finding",24),KeyEvent.VK_F,true,this));
			//toolbar.add(UIHelper.createButton("Assesment","Case Assesment",UIHelper.getIcon(this,"icon.toolbar.clinical.info",24),-1,true,this));
			//toolbar.add(UIHelper.createButton("Done","Finish Case",UIHelper.getIcon(this,"icon.toolbar.done"),-1,true,this));
			
			// square toolbar
			JPanel p = new JPanel();
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(2,2,2,2),0,0);
			GridBagLayout layout = new GridBagLayout();
			p.setLayout(layout);
			layout.setConstraints(p,c);
			p.add(UIHelper.createToggleButton("Identify","Identify Finding on an Image [Alt-F]",
					   UIHelper.getIcon(this,"icon.toolbar.finding",24),KeyEvent.VK_F,true,this),c);
			c.gridy ++;
			p.add(UIHelper.createButton("Assess","Case Assesment",UIHelper.getIcon(this,"icon.toolbar.assesment",24),-1,true,this),c);
			c.gridy=0;
			c.gridx++;
			c.gridheight = 2;
			JButton b = UIHelper.createButton("Done","Finish Case",UIHelper.getIcon(this,"icon.toolbar.done"),-1,true,this);
			b.setVerticalTextPosition(SwingConstants.BOTTOM);
			b.setHorizontalTextPosition(SwingConstants.CENTER);
			p.add(b,c);
			
			toolbar.add(p);
			
			
			
			// change orientation
			//boolean horizontal = false;
			//toolbar.setOrientation((horizontal)?JToolBar.HORIZONTAL:JToolBar.VERTICAL);
		}
		return toolbar;
	}

	private JPanel getBrightnessContrastPanel(){
		if(brightnessContrastPanel == null){
			JPanel p =  new JPanel();
			p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
			p.setBorder(new BevelBorder(BevelBorder.RAISED));
			
			
			// add brighness
			p.add(new JLabel("Brightness",UIHelper.getIcon(this,"icon.toolbar.brightness",24),JLabel.TRAILING));
			bSlider = new JSlider();
			bSlider.setSnapToTicks(true);
			bSlider.setMajorTickSpacing(20);
			bSlider.setMinorTickSpacing(1);
			bSlider.setPaintTicks(true);
			bSlider.addChangeListener(this);
			bSlider.setMinimum(-20);
			bSlider.setMaximum(20);
			bSlider.setValue(0);
			
			p.add(bSlider);
			
			// separator
			//p.add(Box.createRigidArea(new Dimension(40,40)));
			
			// add contrast
			p.add(new JLabel("Contrast",UIHelper.getIcon(this,"icon.toolbar.contrast",24),JLabel.TRAILING));
			cSlider = new JSlider();
			cSlider.setSnapToTicks(true);
			cSlider.addChangeListener(this);
			cSlider.setMajorTickSpacing(20);
			cSlider.setMinorTickSpacing(1);
			cSlider.setMinimum(-20);
			cSlider.setMaximum(20);
			cSlider.setValue(0);
			cSlider.setPaintTicks(true);
			p.add(cSlider);
			
			
			// setup preview
			//		JPanel panel = new JPanel();
			//		panel.setLayout(new BorderLayout());
			//		panel.add(p,BorderLayout.CENTER);
			//		panel.add(createPreviewPanel(),BorderLayout.EAST);
			//		
			brightnessContrastPanel = p;
		}
		return brightnessContrastPanel;
	}
	
	public Viewer getViewer(){
		if(getPresentationModule() instanceof SimpleViewerPanel){
			return ((SimpleViewerPanel)getPresentationModule()).getViewer();
		}else if(getPresentationModule() instanceof ViewerPanel){
			return ((ViewerPanel)getPresentationModule()).getViewer();
		}
		return null;
	}
	
	private ImageTransform getImageTransform(){
		Viewer v = getViewer();
		return (v != null)?v.getImageProperties().getImageTransform():new ImageTransform();
	}
	
	
	public void stateChanged(ChangeEvent e) {
		if(transform != null){
			if(e.getSource() == bSlider){
				transform.setBrightness(BRIGHTNESS_INCREMENT*bSlider.getValue());
			}else if(e.getSource() == cSlider){
				transform.setContrast((float)Math.pow(CONTRAST_INCREMENT,cSlider.getValue()));
			}
			// update thumbnails
			updatePreview();
			
			// update slides
			updateTransform();
		}
	}
	private void updatePreview(){
		// update thumbnails
		for(int i=0;i<previewPanel.getComponentCount();i++)
			((PreviewButton)((JPanel)previewPanel.getComponent(i)).getComponent(0)).updateImage();
	}
	
	
	private void updateTransform(){
		// apply to current transform
		getImageTransform().setBrightness(transform.getBrightness());
		getImageTransform().setContrast(transform.getContrast());
		
		final Viewer v = getViewer();
		if(v != null){
			final ViewPosition p = v.getViewPosition();
			v.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if(edu.pitt.slideviewer.Constants.IMAGE_TRANSFORM.equals(evt.getPropertyName())){
						v.setViewPosition(p);
						v.repaint();
						v.removePropertyChangeListener(this);
					}
				}
			});
			v.update();
			
		}
	}
	
	public void sync(InterfaceModule tm) {
		getComponent();
		setCaseEntry(tutor.getCase());
		for(ConceptEntry e: tm.getConceptEntries()){
			//TODO:
			//updateConceptEntry(e, tm);
		}
	}

	
	public void actionPerformed(ActionEvent e) {
		if(!isInteractive())
			return;
		
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
		if(cmd.equalsIgnoreCase("Identify")){
			selectedButton = (AbstractButton) e.getSource();
			if(selectedButton.isSelected())
				doFinding();
			else
				cancelFinding();
		}else if(cmd.equalsIgnoreCase("Done")){
			doDone();
		}else if(cmd.equalsIgnoreCase("Assess")){
			doAssesment();
		}else if(cmd.equalsIgnoreCase("delete")){
			doDelete();
		}else if(cmd.equalsIgnoreCase("glossary")){
			doGlossary();
		}else if(cmd.equalsIgnoreCase("recommend")){
		}else if(cmd.equalsIgnoreCase("location")){
			doLocation();
		}else if(cmd.startsWith(PreviewButton.OPEN_COMMAND)){
			// replace component if necessary
			if(previewMode){
				hidePreviewPanel();
			}
			// load slide
			String name = cmd.substring(PreviewButton.OPEN_COMMAND.length());
			Action action = new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_CHANGE_IMAGE,name);
			Communicator.getInstance().resolveAction(action);
			action.run();
			
		}else if(e.getSource() instanceof JRadioButton){
			// confidnce scale
			if(radio != null && featureWidget != null){
				ok.setEnabled(featureWidget.getEntry() != null && radio.getCommand().length() > 0);
			}else if(adviceWidget != null){
			// bi-rads worksheet
				ok.setEnabled(adviceWidget.getEntry() != null);
			}
		}
		
	}
	
	public void openImage(String name){
		// make sure that button is selected
		for(Component c: previewPanel.getComponents()){
			AbstractButton b = null;
			if(c instanceof AbstractButton){
				b = (AbstractButton) c;
			}else if(c instanceof JPanel && ((JPanel)c).getComponent(0) instanceof AbstractButton)
				b = (AbstractButton) ((JPanel)c).getComponent(0);
				
			if(b != null){
				if(b.getActionCommand().contains(name)){
					b.doClick();
				}
			}
		}
	}
	
	
	private void doDelete() {
		for(TreePath p : tree.getSelectionPaths()){
			removeConceptEntry(((RadiologyConcept)p.getLastPathComponent()).getConceptEntry());
		}
	}
	
	private void doLocation(){
		TreePath t = tree.getSelectionPath();
		if(t != null){
			RadiologyConcept node = (RadiologyConcept)t.getLastPathComponent();
			node.showIdentifiedFeature();
		}
	}
	
	/**
	 * show glossary
	 */
	private void doGlossary(){
		TreePath t = tree.getSelectionPath();
		if(t != null){
			RadiologyConcept node = (RadiologyConcept)t.getLastPathComponent();
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
				glossaryManager.setShowExampleImage(Config.getBooleanProperty(this,"behavior.glossary.show.example"));
			}
		
			// show glossary
			SwingUtilities.convertPointToScreen(lastLocation,tree);
			glossaryManager.showGlossary(e,getComponent(),lastLocation);
		}
	}

	private PresentationModule getPresentationModule(){
		if(viewer == null){
			if(getTutor() != null){
				viewer = getTutor().getPresentationModule();
			}else{
				for(TutorModule tm: Communicator.getInstance().getRegisteredModules()){
					if(tm instanceof PresentationModule){
						viewer = (PresentationModule) tm;
						break;
					}
				}
			}
		}
		return viewer;
	}
	
	/**
	 * display a finding/recommendation dialog
	 */
	private boolean showInputDialog(){
		// create input panel
		ok = new JButton("OK");
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel,BoxLayout.Y_AXIS));
		featureWidget = new SingleEntryWidget("Finding");
		featureWidget.setEntryChooser(getFindingDialog());
		featureWidget.setBorder(new EmptyBorder(5,5,5,5));
		featureWidget.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(SingleEntryWidget.ENTRY_CHANGED.equals(evt.getPropertyName())){
					ok.setEnabled(featureWidget.getEntry() != null && radio.getCommand().length() > 0);
						//adviceWidget.getEntries().length > 0);
				}
			}
		});
		inputPanel.add(featureWidget);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5),new BevelBorder(BevelBorder.LOWERED)));
		JLabel l =  new JLabel("How confident are you?");
		l.setHorizontalTextPosition(SwingConstants.CENTER);
		l.setHorizontalAlignment(JLabel.CENTER);
		l.setBorder(new EmptyBorder(5,5,5,5));
		radio = UIHelper.createConfidencePanel(this,1,5,"unsure","confident");
		p.add(l,BorderLayout.NORTH);
		p.add(radio.getComponent(),BorderLayout.CENTER);
		
		inputPanel.add(p);
		
		
		ok.setEnabled(false);
		inputDialog = UIHelper.createDialog(inputPanel,ok,new JButton("Cancel"));
		inputDialog.setVisible(true);
				
		return ok.isSelected();
	}
	
	/**
	 * display a finding/recommendation dialog
	 */
	private boolean showAssesmentDialog(){
		// create input panel
		ok = new JButton("OK");
		radio = null;
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel,BoxLayout.Y_AXIS));
		adviceWidget = new  SingleEntryWidget("Assessment");
		adviceWidget.setEntryChooser(getRecommendationDialog());
		adviceWidget.setBorder(new EmptyBorder(5,5,5,5));
		adviceWidget.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(MultipleEntryWidget.ENTRY_CHANGED.equals(evt.getPropertyName())){
					ok.setEnabled(adviceWidget.getEntry() != null && getWorksheetString().length() > 0);
				}
			}
		});
		inputPanel.add(adviceWidget);
		
		inputPanel.add(getWorksheet());
		
		// clear worksheet
		for(AbstractButton b: resetButtons)
			b.doClick();
		
		ok.setEnabled(false);
		inputDialog = UIHelper.createDialog(inputPanel,ok,new JButton("Cancel"));
		inputDialog.setVisible(true);
				
		return ok.isSelected();
	}
	
	
	/**
	 * add new concept
	 */
	private void doFinding(){
		// start the drawing
		(new Thread(new Runnable(){
			public void run(){
				PresentationModule viewer = getPresentationModule();
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
				
				// show input dialog
				if(showInputDialog()){
					unselectButton();
					// add fature
					ConceptEntry finding = ((ConceptEntry)featureWidget.getEntry());
					finding.setInput(input);
					finding.getProperties().put(MESSAGE_INPUT_CONFIDENCE,radio.getCommand());
					
					addConceptEntry(finding);
					
				}else{
					viewer.removeIdentifiedFeature(input);
					unselectButton();	
					if(!blockMessages)
						MessageUtils.getInstance(RadiologyInterface.this).flushInterfaceEvents(null);
				}
			}
		})).start();
			
		
	}
	
	/**
	 * add new concept
	 */
	private void cancelFinding(){
		// color component back to normal
		setRaiseCurtain(false);
		
		// stop feature identification
		PresentationModule viewer = getPresentationModule();
		if(viewer != null){
			viewer.stopFeatureIdentification();
		}
		
		unselectButton();
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
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getFindingDialog(){
		if(findingDialog == null){
			findingDialog = createDialog(FINDINGS);
			findingDialog.setDialogType(TYPE_FINDING);
			findingDialog.setSelectionMode(TreeDialog.SINGLE_SELECTION);
		}
		return findingDialog;
	}
	
	/**
	 * get finding dialog
	 * @return
	 */
	private TreeDialog getRecommendationDialog(){
		if(adviceDialog == null){
			adviceDialog = createDialog(RECOMMENDATIONS);
			adviceDialog.setDialogType(TYPE_RECOMMENDATION);
			adviceDialog.setSelectionMode(TreeDialog.MULTIPLE_SELECTION);
		}
		return adviceDialog;
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
	 * raise curtain during finding identification
	 * @param b
	 */
	private void setRaiseCurtain(boolean raise){
		PresentationModule viewer = getPresentationModule();
		Component c = (component.getComponentCount() > 0)?component.getComponent(0):component;
		if(raise){
			c.setBackground(new Color(240,240,240));
			c.setCursor(UIHelper.getChildCursor(viewer.getComponent()));
		}else{
			c.setBackground(Config.getColorProperty(RadiologyInterface.this,"component.background"));
			c.setCursor(Cursor.getDefaultCursor());
		}
		c.repaint();
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
	 * notify concept entry
	 * @param e
	 * @param action
	 */
	private void notifyConceptEntry(ConceptEntry e, String action){
		if(blockMessages)
			return;
		
		// send message about this concept's feature
		if(e.isFinding()){
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
		}else{
			// just send it 
			ClientEvent ce = e.getClientEvent(this,action);
			MessageUtils.getInstance(this).flushInterfaceEvents(ce);
			sendMessage(ce);
		}
	}
	
	private Map<String,Map<String,String>> getAuxInputMap(){
		if(auxCEInputs == null)
			auxCEInputs = new HashMap<String, Map<String,String>>();
		return auxCEInputs;
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
	 * @param args
	 */
	public static void main(String[] args) {
		ExpertModule expert  = new DomainExpertModule();
		//expert.getDefaultConfiguration().setProperty("curriculum.path", "/home/tseytlin/Work/curriculum");
		expert.load();
		expert.openDomain("http://slidetutor.upmc.edu/curriculum/owl/breast/UPMC/Radiology.owl");
		//expert.openDomain("http://slidetutor.upmc.edu/curriculum/owl/skin/PITT/VesicularDermatitis.owl");
		CaseEntry e = expert.getCaseEntry("test");
		//CaseEntry e = expert.getCaseEntry("AP_11");
		RadiologyInterface ri = new RadiologyInterface();
		ri.getDefaultConfiguration().setProperty("behavior.show.slide.selector.on.start","false");
		
		SimpleViewerPanel vp = new SimpleViewerPanel();
		vp.getDefaultConfiguration().setProperty("viewer.navigator.enabled","false");
		ConsoleProtocolModule cp = new ConsoleProtocolModule();
		RadiologyReasoner rr = new RadiologyReasoner();
		
		
		ri.setExpertModule(expert);
		vp.setExpertModule(expert);
		rr.setExpertModule(expert);
		ri.load();
		vp.load();
		rr.load();
		
		Communicator.getInstance().addRecipient(ri);
		Communicator.getInstance().addRecipient(vp);
		Communicator.getInstance().addRecipient(cp);
		Communicator.getInstance().addRecipient(rr);
		
		JSplitPane p = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,ri.getComponent(),vp.getComponent());
		
		JFrame frame = new JFrame();
		
		Config.setMainFrame(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(p);
		frame.setPreferredSize(new Dimension(1024,768));
		frame.pack();
		frame.setVisible(true);
		UIHelper.centerWindow(frame);
		
	
		ri.setCaseEntry(e);
		vp.setCaseEntry(e);
		rr.setCaseEntry(e);
	
	}

}
