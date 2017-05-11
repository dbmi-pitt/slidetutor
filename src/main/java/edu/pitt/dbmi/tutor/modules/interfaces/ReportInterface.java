package edu.pitt.dbmi.tutor.modules.interfaces;


import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.interfaces.NodeConcept.SupportLinkConcept;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ConceptLabel;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportData;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportDocument;
import edu.pitt.dbmi.tutor.modules.interfaces.report.ReportEditorKit;
import edu.pitt.dbmi.tutor.modules.interfaces.report.StringPlayer;
import edu.pitt.dbmi.tutor.modules.interfaces.report.process.ReportSaver;
import edu.pitt.dbmi.tutor.modules.interfaces.report.process.ReportScanner;
import edu.pitt.dbmi.tutor.modules.interfaces.report.process.ReportSpellChecker;
import edu.pitt.dbmi.tutor.ui.GlossaryManager;
import edu.pitt.dbmi.tutor.ui.TreeDialog;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.markers.Annotation;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.lexicon.Concept;
import edu.pitt.text.tools.TextTools;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;

/**
 * report interface
 * @author tseytlin
 */
public class ReportInterface implements InterfaceModule, ActionListener, PropertyChangeListener {
	private final String WORKSHEET_SECTION = "FINAL DIAGNOSIS";
	private JTextPane textPane;
	private ReportDocument doc;
	private ReportData reportData;
	private ReportSpellChecker spellChecker;
	private ReportScanner reportScanner;
	private JToolBar toolbar;
	private JPopupMenu contextMenu;
	private JLabel status;
	private JPanel mainPanel;
	private boolean readOnly;
	private JComponent worksheet;
	private List<AbstractButton> worksheetButtons, resetButtons;
	private JTextField [] totalScoreText;
	private JMenu menu;
	private JToggleButton measureButton;
	private Properties defaultConfig;
	private ExpertModule expertModule;
	private CaseEntry caseEntry;
	private Tutor tutor;
	private boolean interactive;
	private Map<String,ReportConcept> registry;
	private ViewPosition view;
	private Annotation ruler;
	private List<Action> annotationActions;
	private boolean blockMessages,debug,textPlayback;
	private TreeDialog findingDialog;
	//private List<Popup> glossaryWindows;
	private boolean behaviorShowGlossary,behaviorShowExample;
	private GlossaryManager glossaryManager;
	private Point location;
	private Terminology ontTerm,anatTerm;
	private URL addTermURL;
	private StringPlayer stringPlayer;
	private Position currentPosition;
	
	/**
	 * load resources
	 * @param doc
	 */
	public void loadDocument(){
		// init registry
		registry = new HashMap<String, ReportConcept>();
		
		
        // start new document
        doc = new ReportDocument(textPane);
      
		// if report data and case entry already available add them
		if(reportData != null && caseEntry != null){
			doc.setReportData(reportData);
			doc.load(caseEntry);
		}else{
			doc.load();
		}
		
		doc.setStatusLabel(status);
		doc.setTextTools(new TextTools(Config.getURLProperty(this,"text.tools.server.url")));
		doc.setStatusLabel(status);
		
		
		if(expertModule != null)
			doc.setTerminology(getDomainTerminology());
				
		// load processing resources
		spellChecker = new ReportSpellChecker(doc);
		reportScanner = new ReportScanner(doc);
		
		//doc.addReportProcessor(spellChecker);
		doc.addReportProcessor(reportScanner);
		doc.addReportProcessor(new ReportSaver(doc));
		
		
		// set document to text panel
		textPane.setDocument(doc);
		textPane.addCaretListener(doc);
		//textPane.addContainerListener(reportData);
		
		addTermURL = Config.getURLProperty(this,"add.term.server.url");
		
		reloadWorksheet();
	}
	
	
	/**
	 * load module specific resource
	 */
	public void load(){
		
	}
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	/**
	 * get toolbar
	 * @return
	 */
	public JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new UIHelper.ToolBar();
			//toolbar.setBackground(Color.white);
			//toolbar.setFloatable(false);
			
			//toolbar.add(UIHelper.createButton("Process Report","Process Report",UIHelper.getIcon(this,"icon.toolbar.run",24),this));
			measureButton = UIHelper.createToggleButton("Measure","Measuring Tool",UIHelper.getIcon(this,"icon.toolbar.measure",24),this);
			toolbar.add(measureButton);
			toolbar.add(UIHelper.createButton("Worksheet","Workhseet",UIHelper.getIcon(this,"icon.toolbar.worksheet",24),this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createToggleButton("Spell","Automatic Spell Check",UIHelper.getIcon(this,"icon.toolbar.spell",24),this));
			toolbar.add(Box.createGlue());
			toolbar.add(UIHelper.createButton("Done","Finish Report",UIHelper.getIcon(this,"icon.toolbar.done"),-1,true,this));
			
			//setReadOnly(readOnly);
			// change orientation
			boolean th = "horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation"));
			toolbar.setOrientation((th)?JToolBar.HORIZONTAL:JToolBar.VERTICAL);
		}
		return toolbar;
	}

	
	/**
	 * get context menu
	 * @return
	 */
	public JPopupMenu getPopupMenu(){
		if(contextMenu == null){
			contextMenu = new JPopupMenu();
			contextMenu.add(UIHelper.createMenuItem("Add Finding","Add Finding",UIHelper.getIcon(this,"icon.menu.add"),this));
			contextMenu.add(UIHelper.createMenuItem("Glossary","Lookup Glossary",	UIHelper.getIcon(this,"icon.menu.glossary",16),this));
			contextMenu.addSeparator();
			contextMenu.add(UIHelper.createMenuItem("Cut","Cut",UIHelper.getIcon(this,"icon.menu.cut"),this));
			contextMenu.add(UIHelper.createMenuItem("Copy","Copy",UIHelper.getIcon(this,"icon.menu.copy"),this));
			contextMenu.add(UIHelper.createMenuItem("Paste","Paste",UIHelper.getIcon(this,"icon.menu.paste"),this));
			contextMenu.addSeparator();
			contextMenu.add(UIHelper.createMenuItem("Suggest Term","Suggest Term",UIHelper.getIcon(this,"icon.menu.suggest"),this));
		}
		return contextMenu;
	}
	
	// which mode is context menu in?
	private void contextMode(boolean textSelected,boolean conceptSelected){
		getPopupMenu().getComponent(0).setEnabled(!(textSelected || conceptSelected));
		getPopupMenu().getComponent(1).setEnabled(conceptSelected);
		
		getPopupMenu().getComponent(3).setEnabled(textSelected);
		getPopupMenu().getComponent(4).setEnabled(textSelected);
		getPopupMenu().getComponent(5).setEnabled(true);
		
		getPopupMenu().getComponent(7).setEnabled(textSelected);
	}
	
	
	/**
	 * add schema
	 */
	private void doAddFinding(){
		TreeDialog d = getFindingDialog();
		d.setSelectionMode(TreeDialog.MULTIPLE_SELECTION);
		d.setVisible(true);
		int offset = textPane.getCaretPosition();
		for(TreePath name: d.getSelectedNodes()){
			String text = UIHelper.getTextFromName(name.getLastPathComponent().toString())+"\n";
			doc.insertText(text, offset);
			offset += text.length();
		}
	}
	
	/**
	 * add schema
	 */
	private void doSuggestTerm(){
		String text = textPane.getSelectedText();
		TreeDialog d = getFindingDialog();
		d.setSelectionMode(TreeDialog.SINGLE_SELECTION);
		d.setVisible(true);
		TreePath t = d.getSelectedNode();
		if(t != null){
			addTerm(text,""+t.getLastPathComponent(),true);
		}
	}
	
	/**
	 * add ontTerm to a concept class
	 * @param text
	 * @param name
	 */
	private void addTerm(String text, String name, boolean upload){
		if(TextHelper.isEmpty(text) || TextHelper.isEmpty(name))
			return;
		
		IClass cls = getDomainOntology().getClass(name);
		if(cls != null){
			text = text.toLowerCase().trim();
			
			// add ontTerm to terminology now
			try {
				// initialize concept
				Concept c = cls.getConcept();
				c.initialize();
				// remove concept, before re adding it
				getDomainTerminology().removeConcept(c);
				
				// if synonym is already there, don't need to do anything
				if(Arrays.asList(c.getSynonyms()).contains(text))
					return;
				
				// must add this label to the class
				cls.addLabel(text);
				c.setSynonyms(cls.getLabels());
				
				// add concept to terminology
				getDomainTerminology().addConcept(c);
				
				// if it is misspelled, ignore it
				if(!spellChecker.spellWord(text))
					spellChecker.ignoreWord(text);
				
				// upload to server
				if(upload && addTermURL != null){
					Map<String,String> map = new LinkedHashMap<String, String>();
					map.put("term",TextHelper.escape(text));
					map.put("url",TextHelper.escape(cls.getURI().toString()));
					String res =UIHelper.doGet(addTermURL,map);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * get selected report concept
	 * @return
	 */
	public ReportConcept getSelectedReportConcept(){
		int pos = textPane.getCaretPosition();
		if(location != null)
			pos = textPane.viewToModel(location);
		AttributeSet a = doc.getCharacterElement(pos).getAttributes();
		if(a != null){
			Object o =  a.getAttribute("object");
			if(o instanceof ConceptLabel){
				ConceptLabel lbl = (ConceptLabel) o;
				for(ReportConcept r: registry.values()){
					if(r.getLabels().contains(lbl))
						return r;
				}
				
			}
		}
		return null;
	}
	

	/**
	 * show glossary
	 */
	private void doGlossary(){
		ReportConcept node = getSelectedReportConcept();
		
		if(node != null){
			ConceptEntry e = node.getConceptEntry();
			
			// notify of action
			if(!blockMessages){
				ClientEvent ce = e.getFeature().getClientEvent(this,Constants.ACTION_GLOSSARY);
				MessageUtils.getInstance(this).flushInterfaceEvents(ce);
				Communicator.getInstance().sendMessage(ce);
			}
			// resolve node
			expertModule.resolveConceptEntry(e);
			
			// show glossary panel
			//showGlossaryPanel(node);
			if(glossaryManager == null){
				glossaryManager = new GlossaryManager();
				glossaryManager.setShowExampleImage(behaviorShowExample);
			}
			if(location != null){
				Point loc = location;
				SwingUtilities.convertPointToScreen(loc,textPane);
				glossaryManager.showGlossary(e,getComponent(),loc);
			}
		}
	}
	
	
	private TreeDialog getFindingDialog(){
		if(findingDialog == null){
			findingDialog = new TreeDialog(Config.getMainFrame());
			findingDialog.setTitle("Reportable Items");
			TreeNode [] roots = new TreeNode [] {
					expertModule.getTreeRoot(OntologyHelper.DIAGNOSES),
					expertModule.getTreeRoot(OntologyHelper.PROGNOSTIC_FEATURES)};
			findingDialog.setRoots(roots);
		}
		return findingDialog;
	}
	
	
	
	/**
	 * mouse listener
	 * @author tseytlin
	 */
	private class ReportMouseAdapter extends MouseAdapter {
		public void mousePressed(MouseEvent e){
			if(readOnly)
				return;
			
			if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
				location = e.getPoint();
				// text is selected then do one thing, else
				if(textPane.getSelectedText() != null){
					contextMode(true,getSelectedReportConcept() != null);
					getPopupMenu().show(textPane,e.getX(),e.getY());
				}else{
					// move caret to position where I just clicked with left mouse button, now see if works
					textPane.setCaretPosition(textPane.viewToModel(e.getPoint()));
					// get popup menu if needed
					JPopupMenu menu = spellChecker.getSuggestionList(textPane.getCaretPosition());
					if(menu != null){
						menu.show(textPane,e.getX(),e.getY());
					}else{
						contextMode(false,getSelectedReportConcept() != null);
						getPopupMenu().show(textPane,e.getX(),e.getY());
					}
				}
			}
		}
	}
	
	
	/**
	 * show case information
	 */
	private void doShowCase(){
		// create dialog
		String [] types = new String [] {OntologyHelper.DIAGNOSES,OntologyHelper.PROGNOSTIC_FEATURES};
		JOptionPane op = new JOptionPane(UIHelper.createCaseInfoPanel(caseEntry,types,false),JOptionPane.PLAIN_MESSAGE);
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
			annotationActions.add(new Action(PresentationModule.class.getSimpleName(),
									POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL));
			
			// now execute those actions
			for(Action a: annotationActions){
				Communicator.getInstance().resolveAction(a);
				a.run();
			}
		}
	}
	
	public void addConceptEntry(ConceptEntry e) {
		
		// handle null 
		if(e == null)
			return;
		
		// else get representation
		ReportConcept c = ReportConcept.getReportConcept(e,this);
		if(c == null)
			return;
		
		// make sure that thereis in fact an entered label
		if(c.getLabels().size()  == 0){
			insertConceptEntry(e);
			return;
		}
		
		
		// we want to clone an entry 
		c = c.clone();
		e = c.getConceptEntry();
		
		
		if(c.isNegation()){
			// remove mention of it
			registry.put(e.getId(),c);
			
			// add it from the parent concept
			ReportConcept parent = c.getParentConcept();
			if(registry.containsKey(parent.getConceptEntry().getId())){
				parent = registry.get(parent.getConceptEntry().getId());
			}
			
			parent.setNegation(c);
			e.setParentEntry(parent.getConceptEntry());
			
			// send message about this concept
			Communicator.getInstance().sendMessage(e.getClientEvent(this,ACTION_ADDED));
		}else if(TYPE_ATTRIBUTE.equals(e.getType())){
			// what to do if attribute???
			// attributes by themselves don't make sense
			// lets ignore it
			
		}else{
			// split into feature/attributes
			expertModule.resolveConceptEntry(e);
			
			// not intereseted in identical concepts
			if(registry.containsKey(e.getId())){
				return;
			}
			
			//reset feature in negation attribute
			if(c.isNegated()){
				c.getNegation().getConceptEntry().setFeature(e.getFeature());
				c.getNegation().getConceptEntry().setParentEntry(e);
			}
			
			
			// now lets do it
			registerConceptEntry(e);
			
			// send message about this concept
			//Communicator.getInstance().sendMessage(e.getClientEvent(this,Constants.ACTION_ADDED));
			notifyConceptEntry(e,ACTION_ADDED);
		}
	}
	
	/**
	 * notify concept entry
	 * @param e
	 * @param action
	 */
	private void notifyConceptEntry(ConceptEntry e, String action){
		if(blockMessages)
			return;
		
		ReportConcept r = ReportConcept.getReportConcept(e,this);
		
		// get text
		String text = r.getText();
		int offset = r.getOffset();
		int length = r.getLength();
		if(r.isNegated()){
			ReportConcept n = r.getNegation();
			if(n.getOffset() < offset){
				text = n.getText()+" "+text;
				offset = n.getOffset();
				length = r.getLength()+n.getLength()+1;
			}else{
				text = text+" "+n.getText();
				length = r.getLength()+n.getLength()+1;
			}
		}
		
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_CONCEPT,r.getText(),ACTION_ADDED);
		ie.getInputMap().put("text",text);
		ie.getInputMap().put("offset",""+offset);
		ie.getInputMap().put("length",""+length);
		MessageUtils.getInstance(this).addInterfaceEvent(ie);
		
		// send message about this concept's feature
		ClientEvent ce = e.getFeature().getClientEvent(this,action);
		ce.setInput(e.getMessageInput());
		ce.getInputMap().put("text",text);
		ce.getInputMap().put("offset",""+offset);
		ce.getInputMap().put("length",""+length);
		MessageUtils.getInstance(this).flushInterfaceEvents(ce);
		//Communicator.getInstance().sendMessage(ce);
		
		// when finding is removed, send attribute deletes first
		if(!ACTION_REMOVED.equals(action))
			Communicator.getInstance().sendMessage(ce);
		
		// send messages in regards to the attributes
		for(ConceptEntry a: e.getAttributes()){
			Communicator.getInstance().sendMessage(a.getClientEvent(this,action));
		}
		
		// when finding is removed, send attribute deletes first
		if(ACTION_REMOVED.equals(action))
			Communicator.getInstance().sendMessage(ce);
		
		// notify in regards to negation
		if(r != null && r.isNegated()){
			Communicator.getInstance().sendMessage(r.getNegation().getConceptEntry().getClientEvent(this,action));
		}
		
	}
	
	

	public void refineConceptEntry(ConceptEntry parent, ConceptEntry child) {

		// handle null 
		if(child == null || parent == null)
			return;
		
		// get original parent
		parent = getConceptEntry(parent);
		//ReportConcept pr = ReportConcept.getReportConcept(parent,this);
		
		// clone child
		ConceptEntry oldChild = child;
		ReportConcept ch = ReportConcept.getReportConcept(child,this);
		if(ch != null)
			child = ch.clone().getConceptEntry();
		
		expertModule.resolveConceptEntry(child);
		
		// copyt properties to child
		parent.copyTo(child);
		parent.copyIDsTo(child);
		if(child.isParentFinding())
			child.setId(oldChild.getId());
		else
			oldChild.setId(child.getId());
		
		
		// remove parent
		unregisterConceptEntry(parent);
		
		// add child
		registerConceptEntry(child);
		
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
		// register feature
		ReportConcept node = ReportConcept.getReportConcept(e,this);
		registry.put(e.getId(),node);
		registry.put(e.getFeature().getId(),node); //ReportConcept.getReportConcept(e.getFeature(),this));
		
		// register attribute
		for(ReportConcept a: node.getAttributes()){
			registry.put(a.getConceptEntry().getId(),a);
		}
		
		// register negation
		if(node.isNegated()){
			registry.put(node.getNegation().getConceptEntry().getId(),node.getNegation());
		}
		
		//System.out.println("Register: "+registry);
	}
	
	/**
	 * unregister concept entry
	 * @param e
	 */
	private void unregisterConceptEntry(ConceptEntry e){
		ReportConcept node = ReportConcept.getReportConcept(e,this);
		registry.remove(e.getId());
		registry.remove(e.getFeature().getId());
		
		// unregister attribute
		if(node != null){
			for(ReportConcept a: node.getAttributes()){
				registry.remove(a.getConceptEntry().getId());
			}
			
			// unregister negation
			if(node.isNegated()){
				registry.remove(node.getNegation().getConceptEntry().getId());
			}
		}
		//System.out.println("Unregister: "+registry);
	}
	


	public List<ConceptEntry> getConceptEntries() {
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(ReportConcept n: registry.values()){
			list.add(n.getConceptEntry());
		}
		return list;
	}


	public ConceptEntry getConceptEntry(String id) {
		ReportConcept c = registry.get(id);
		return (c != null)?c.getConceptEntry():null;
	}

	public ConceptEntry getConceptEntry(ConceptEntry e) {
		ReportConcept c = registry.get(e.getId());
		if(c != null)
			return c.getConceptEntry();
		
		// else, well lets find similar enough finding
		ReportConcept re = ReportConcept.getReportConcept(e,this);
		for(ReportConcept r: registry.values()){
			if(r.getConceptEntry().equals(e) && r.getOffset() == re.getOffset())
				return r.getConceptEntry();
		}
		
		return null;
	}
	

	public JMenu getMenu() {
		if(menu == null){
			menu = new JMenu("Interface");
			menu.add(UIHelper.createMenuItem("Measure","Measuring Tool",UIHelper.getIcon(this,"icon.toolbar.measure",16),this));
			menu.add(UIHelper.createMenuItem("Worksheet","Workhseet",UIHelper.getIcon(this,"icon.toolbar.worksheet",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createCheckboxMenuItem("Spell","Automatic Spell Check",UIHelper.getIcon(this,"icon.toolbar.spell",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("Process Report","Process Report",UIHelper.getIcon(this,"icon.toolbar.run",16),this));
			menu.addSeparator();
			menu.add(UIHelper.createMenuItem("Done","Finish Report",UIHelper.getIcon(this,"icon.toolbar.done",16),this));
			
			// put in debug options
			JMenu debug = ITS.getInstance().getDebugMenu();
			if(!UIHelper.hasMenuItem(debug,"Show Slide Annotations")){
				debug.add(UIHelper.createMenuItem("goal.list","Preview Case Information ..",
						  UIHelper.getIcon(Config.getProperty("icon.menu.preview")),this),0);
				debug.add(UIHelper.createCheckboxMenuItem("show.annotations","Show Slide Annotations",null,this),1);
				debug.add(UIHelper.createCheckboxMenuItem("show.parsed.concepts","Show Parsed Concepts",null,this),1);
			}
			
		}
		return menu;
	}



	public void removeConceptEntry(ConceptEntry e) {
		// don't bother with suff that ain't there
		if(e == null || !registry.containsKey(e.getId()))
			return;
		
		ReportConcept c = registry.get(e.getId());
		e = c.getConceptEntry();
		
		if(c.isNegation()){
			// remove mention of it
			registry.remove(e.getId());
			// remove it from the parent concept
			c.getParentConcept().setNegation(null);
			
			// send message about this concept
			Communicator.getInstance().sendMessage(e.getClientEvent(this,ACTION_REMOVED));
		}else if(c.isAttribute()){
			// get finding, and remove the extra attribute
			ConceptEntry f = e.getParentEntry();
			
			// what if this is an attrribute
			ConceptEntry feature = e.getFeature();
			List<ConceptEntry> attributes = new ArrayList<ConceptEntry>(f.getAttributes());
			attributes.remove(e);
			
			// get more specific concept w/out attribute
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
			
		}else{
			// now lets do it
			unregisterConceptEntry(e);
			
			// send message about this concept
			//Communicator.getInstance().sendMessage(e.getClientEvent(this,Constants.ACTION_REMOVED));
			notifyConceptEntry(e,ACTION_REMOVED);
		}

	}

	public void setCaseEntry(CaseEntry problem) {
		reset();
		caseEntry = problem;
		textPlayback = false;
		// dispose???
		// load case info 
		// initialize report data
		reportData = new ReportData(this);
		if(doc != null){
			doc.setReportData(reportData);
			doc.load(caseEntry);
		}
		
		// pre -load anatomical site ontology
		(new Thread(new Runnable(){
			public void run(){
				getDomainTerminology();
				getAnatomicTerminology();
			}
		})).start();
	}
	
	
	public void setExpertModule(ExpertModule module) {
		expertModule = module;
		reloadWorksheet();
		if(doc != null)
			doc.setTerminology(getDomainTerminology());
	}

	
	public IOntology getDomainOntology(){
		if(expertModule != null){
			return expertModule.getDomainOntology();
		}
		return null;
	}
		
		
	public Terminology getDomainTerminology(){
		if(ontTerm == null){
			if(expertModule != null){
				ontTerm =  expertModule.getDomainTerminology();
				loadTerms();
			}
		}
		return ontTerm;
	}

	/**
	 * see if we can load terms 
	 */
	private void loadTerms(){
		if(expertModule != null && expertModule.getDomain() != null){
			new Thread(new Runnable(){
				public void run(){
					String o = expertModule.getDomain();
					String termsURL = o.substring(0,o.indexOf(OWL_SUFFIX))+TERMS_SUFFIX;
					try{
						URL u = new URL(termsURL);
						URLConnection c = u.openConnection();
						c.setConnectTimeout(100);
						c.setReadTimeout(1000);
						
						BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
						for(String line=reader.readLine();line != null;line = reader.readLine()){
							String [] t = line.split("\\|");
							if(t.length == 2){
								addTerm(t[0].trim(),t[1].trim(),false);
							}
						}
						reader.close();
					}catch(Exception ex){
						//no matter what happens, we don't really care enough to report it
					}
				}
			}).start();
		}
	}
	
	
	public Terminology getAnatomicTerminology(){
		if(anatTerm == null){
			if(expertModule != null && expertModule instanceof DomainExpertModule){
				anatTerm = ((DomainExpertModule) expertModule).getTerminology(""+OntologyHelper.ANATOMY_ONTOLOGY_URI);
			}
		}
		return anatTerm;
	}
	
	public void sync(InterfaceModule tm) {
		setCaseEntry(tutor.getCase());
		/*
		 *TODO: take care of syncing
		for(ConceptEntry e: tm.getConceptEntries()){
			if(TYPE_DIAGNOSIS.equals(e.getType())){
				addConceptEntry(e);
			}
		}
		*/
	}

	public void reconfigure(){
		dispose();
		behaviorShowExample = Config.getBooleanProperty(this,"behavior.glossary.show.example");
		behaviorShowGlossary = Config.getBooleanProperty(this,"behavior.glossary.enabled");
		mainPanel = null;
	}
	
	public Component getComponent() {
		if(mainPanel == null){
			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			textPane = new JTextPane();
			textPane.setEditorKit(new ReportEditorKit(textPane));
			textPane.addMouseListener(new ReportMouseAdapter());
			textPane.setAutoscrolls(true);
	        status = new JLabel(" ");			
			boolean th = "horizontal".equalsIgnoreCase(Config.getProperty(this,"toolbar.orientation"));
			mainPanel.add(getToolBar(),(th)?BorderLayout.NORTH:BorderLayout.WEST);
			mainPanel.add(new JScrollPane(textPane),BorderLayout.CENTER);
			mainPanel.add(status,BorderLayout.SOUTH);
		
			// load other non-GUI things
			loadDocument();
			
			behaviorShowExample = Config.getBooleanProperty(this,"behavior.glossary.show.example");
			behaviorShowGlossary = Config.getBooleanProperty(this,"behavior.glossary.enabled");
		}
		return mainPanel;
	}

	public ReportDocument getReportDocument(){
		return doc;
	}
	
	
	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Tutor getTutor() {
		return tutor;
	}


	public boolean isEnabled() {
		return (textPane != null)?textPane.isEditable():false;
	}

	public boolean isInteractive() {
		return interactive;
	}

	public void setEnabled(boolean b) {
		if(textPane != null){
			textPane.setEditable(b);
			UIHelper.setEnabled(getToolBar(),b);
			UIHelper.setEnabled(getMenu(),b);
		}

	}

	public void setInteractive(boolean b) {
		interactive = b;
		textPane.setEditable(b);
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public void dispose() {
		reset();
		reloadWorksheet();
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

	public String getDescription() {
		return "Report-writing interface with common headings where users write medical reports. The interface utilizes natural " +
				"language processing. ";
	}

	public String getName() {
		return "Report Interface";
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


	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	private void insertConceptEntry(ConceptEntry e){
		String en = "\n";
		IClass cls = OntologyHelper.getConceptClass(e,getDomainOntology());
		if(OntologyHelper.isOfParent(cls,TISSUE) || OntologyHelper.isAnatomicLocation(cls) || OntologyHelper.isWorksheet(cls)){
			en = ", ";
		}
		// insert finding
		doc.insertText( e.getText()+en,WORKSHEET_SECTION,false);
		
	}
	
	public void receiveMessage(Message msg) {
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
		
		// handle the playback messages
		if(msg.getSender() instanceof ProtocolModule){
			setBlockMessages(true);
			
			// handle adding removing concepts from client events
			// if text IE's are not available
			if(!textPlayback){
				for(String type: CONCEPT_TYPES){
					// we have one of the concept types
					if(msg.getType().equals(type) && msg.getInputMap().containsKey("text")){
						String en = "\n";
						// figure out separator
						ConceptEntry e = ConceptEntry.getConceptEntry(msg.getObjectDescription());
						IClass cls = OntologyHelper.getConceptClass(e,getDomainOntology());
						if(OntologyHelper.isOfParent(cls,TISSUE) || OntologyHelper.isAnatomicLocation(cls) || OntologyHelper.isWorksheet(cls)){
							en = ", ";
						}
						
						// insert finding
						String text = msg.getInputMap().get("text").replaceAll("\\\\n","\n")+en;
						int offset = Integer.parseInt(msg.getInputMap().get("offset"));
						int length = Integer.parseInt(msg.getInputMap().get("length"));
						if(ACTION_ADDED.equals(msg.getAction())){
							doc.insertText(text,WORKSHEET_SECTION,false);
						}else if(ACTION_REMOVED.equals(msg.getAction())){
							try {
								String content = doc.getText(0,doc.getLength());
								offset = content.indexOf(text);
								if(offset > -1)
									doc.remove(offset,length);
							} catch (BadLocationException ex) {
								Config.getLogger().severe(TextHelper.getErrorMessage(ex));
							}
						}
					}
				}
			}
			
			// handle moving of nodes
			if(TYPE_TEXT.equals(msg.getType())){
				textPlayback = true;
				String text = msg.getInputMap().get("text").replaceAll("\\\\n","\n");
				if(text == null){
					return;
				}
				int offset = Integer.parseInt(msg.getInputMap().get("offset"));
				long t1 = Long.parseLong(msg.getInputMap().get("time"));
				long t2 = msg.getTimestamp();
				
				// do insert
				if(ACTION_ADDED.equals(msg.getAction())){
					int dur = (int) (t2 - t1);
					if(stringPlayer != null && stringPlayer.isAlive()){
						stringPlayer.flush();
						try{
							stringPlayer.join();
						}catch(InterruptedException ex){}
					}
					stringPlayer = new StringPlayer(text,offset,dur,getReportDocument());
					stringPlayer.start();
				}else if(ACTION_REMOVED.equals(msg.getAction())){
					try {
						getReportDocument().remove(offset,text.length());
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}else if(TYPE_ACTION.equals(msg.getType()) && ACTION_MEASURE.equals(msg.getAction())){
				Object input = null;
				
				PresentationModule viewer = getTutor().getPresentationModule();
				viewer.setFeatureMarker(ANNOTATION_RULER);
				viewer.setIdentifiedFeature(TextHelper.parseLine(msg.getLabel()));
				input = viewer.getIdentifiedFeature();
				
				
				// if input is null and button unselected
				// means that action was canceled
								
				// now remove the previous ruler
				if(ruler != null){
					viewer.removeIdentifiedFeature(ruler);
					ruler.removePropertyChangeListener(ReportInterface.this);
				}
				
				// save new ruler
				ruler = (Annotation) input;
				ruler.addPropertyChangeListener(ReportInterface.this);
				//measureButton.setSelected(false);
				
				// notify ruler
				//notifyRuler();
			}else if(TYPE_DONE.equals(msg.getType())){
				doDone();
			}
			
			setBlockMessages(false);
			return;
		}
	}

	public void reset() {
		// empty out the repoprt panel
		if(registry != null)
			registry.clear();
		if(glossaryManager != null)
			glossaryManager.reset();
		if(doc != null)
			doc.clear();
		textPlayback = false;

	}

	/**
	 * set concept status for inner components of outer concept
	 * @param o - outer concept
	 * @param i - inner concept
	 * @param r - response
	 */
	private void setConceptStatus(ConceptEntry o, ConceptEntry i, String r){
		//System.out.println(i+" in "+o);
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
					ReportConcept entry = registry.get(a.getId());
					
					// if we can't find appropriate entry in the
					// registry, maybe it is a more specific form of
					// the parent entry, lets check
					if(entry == null && a.getFeature() != null){
						ReportConcept c = registry.get(a.getFeature().getId());
						if(c != null){
							setConceptStatus(c.getConceptEntry(),a,act.getInput());
							c.repaint();
						}
					}
					if(entry != null){
						entry.getConceptEntry().setConceptStatus(act.getInput());
						entry.repaint();
					}
					//repaint();
				}
				public void undo(){
					
				}
			};
		}else if(POINTER_ACTION_FLASH_CONCEPT.equalsIgnoreCase(action.getAction())){
			/*
			oper = new Operation(){
				private ReportConcept entry;
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
			*/
		}else if(POINTER_ACTION_ADD_CONCEPT_ERROR.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				private ReportConcept entry;
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

	
	private void doAutoSpellCheck(Object o){
		//spellChecker.doSpellCheck();
		boolean check = false;
		if(o instanceof JToggleButton){
			JToggleButton b = (JToggleButton) o;
			check = b.isSelected();
			AbstractButton m = UIHelper.getMenuItem(getMenu(),"Spell");
			if(m != null)
				m.setSelected(check);
		}else if(o instanceof JCheckBoxMenuItem){
			JCheckBoxMenuItem b = (JCheckBoxMenuItem) o;
			check = b.isSelected();
			AbstractButton m = UIHelper.getButton(getToolBar(),"Spell");
			if(m != null)
				m.setSelected(check);
		}
		if(check){
			doc.addReportProcessor(spellChecker);
			new Thread(new Runnable(){
				public void run(){
					spellChecker.spellDocument();
				}
			}).start();
		}else{
			doc.removeReportProcessor(spellChecker);
			new Thread(new Runnable(){
				public void run(){
					spellChecker.clear();
				}
			}).start();
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(cmd.equals("Process Report")){
			doRun();
		}else if(cmd.equals("Worksheet")){
			doWorksheet();
		}else if(cmd.equals("Spell")){
			doAutoSpellCheck(e.getSource());
		}else if(cmd.equals("Measure")){
			if(measureButton.isSelected()){
				doMeasure();
			}else if(getTutor().getPresentationModule() != null){
				getTutor().getPresentationModule().stopFeatureIdentification();
				// now remove the previous ruler
				if(ruler != null){
					getTutor().getPresentationModule().removeIdentifiedFeature(ruler);
					ruler.removePropertyChangeListener(ReportInterface.this);
				}
			}
		}else if(cmd.equals("Add Report")){
			//doImport();
		}else if(cmd.equals("Auto Spell Check")){
			if(((AbstractButton)e.getSource()).isSelected()){
				doc.addReportProcessor(spellChecker);				
			}else{
				doc.removeReportProcessor(spellChecker);
			}
		}else if(cmd.equals("Cut")){
			textPane.cut();
		}else if(cmd.equals("Copy")){
			textPane.copy();
		}else if(cmd.equals("Paste")){
			textPane.paste();
		}else if(cmd.equals("Add Finding")){
			doAddFinding();
		}else if(cmd.equals("Suggest Term")){
			doSuggestTerm();
		}else if(cmd.equals("Glossary")){
			doGlossary();	
		}else if(e.getSource() instanceof JRadioButton){
			//doTotalScore();
		}else if(cmd.equalsIgnoreCase("goal.list")){
			doShowCase();
		}else if(cmd.equalsIgnoreCase("show.annotations")){
			doShowAnnotations(((AbstractButton)e.getSource()).isSelected());
		}else if(cmd.equalsIgnoreCase("show.parsed.concepts")){
			debug = ((JCheckBoxMenuItem)e.getSource()).isSelected();
		}else if(cmd.equals("Done")){
			doDone();
		}
		
	}
	
	/**
	 * add new concept
	 */
	private void doDone(){
		ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_DONE,getClass().getSimpleName(),ACTION_SUMMARY);
		MessageUtils.getInstance(this).flushInterfaceEvents(ce);
		Communicator.getInstance().sendMessage(ce);
	}
	
	private void doRun(){
		(new Thread(new Runnable(){
			public void run(){
				processReport();
			}
		})).start();
	}
	
	public void processReport(){
		// remove as a resouce
		reportData.clear();
		reportScanner.scanDocument();
		reportData.processDocument();
		
		// clear out stuff in registry that is no longer in report
		/*for(ReportConcept r: new ArrayList<ReportConcept>(registry.values())){
			if(!reportData.getProcessedConcepts().contains(r)){
				removeConceptEntry(r.getConceptEntry());
			}
		}*/
	}
	
	public ReportScanner getReportScanner(){
		return reportScanner;
	}
	
	private void doWorksheet(){
		JComponent worksheet = getWorksheet();
		if(worksheet instanceof JTabbedPane){
			JTabbedPane t = (JTabbedPane) worksheet;
			if(t.getTabCount() == 0){
				JOptionPane.showMessageDialog(Config.getMainFrame(),"Worksheet was not setup for current domain");
				return;
			}
		}
	
		
		// non-modal worksheet
		final JOptionPane op = new JOptionPane(worksheet,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
		JDialog d = op.createDialog(Config.getMainFrame(),"Worksheet");
		d.setResizable(true);
		d.setModal(false);
		d.addWindowListener(new WindowAdapter(){
			public void windowDeactivated(WindowEvent e) {
				Object o = op.getValue();
				if(o instanceof Integer && ((Integer)o).intValue() == JOptionPane.OK_OPTION)
					doc.insertText(getWorksheetString(),WORKSHEET_SECTION,false);
			}
		});
		d.setVisible(true);
	}
	
	/**
	 * raise curtain during finding identification
	 * @param b
	 */
	private void setRaiseCurtain(boolean raise){
		PresentationModule viewer = getTutor().getPresentationModule();
		if(raise){
			mainPanel.setBackground(new Color(240,240,240));
			mainPanel.setCursor(UIHelper.getChildCursor(viewer.getComponent()));
		}else{
			mainPanel.setBackground(Config.getColorProperty(ReportInterface.this,"component.background"));
			mainPanel.setCursor(Cursor.getDefaultCursor());
		}
		mainPanel.repaint();
	}
	
	private void doMeasure(){
		// start the drawing
		(new Thread(new Runnable(){
			public void run(){
				PresentationModule viewer = getTutor().getPresentationModule();
				viewer.setFeatureMarker(ANNOTATION_RULER);
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
								
				// now remove the previous ruler
				if(ruler != null){
					viewer.removeIdentifiedFeature(ruler);
					ruler.removePropertyChangeListener(ReportInterface.this);
				}
				
				// save new ruler
				ruler = (Annotation) input;
				ruler.addPropertyChangeListener(ReportInterface.this);
				//measureButton.setSelected(false);
				
				// notify ruler
				notifyRuler();
			}
		})).start();
	}
	
	private void notifyRuler(){
		if(ruler != null){
			Point s = ruler.getLocation();
			Point e = ruler.getEndPoint();
			
			// build
			Map<String,String> map = new LinkedHashMap<String, String>();
			map.put("x1",""+s.x);
			map.put("y2",""+s.y);
			map.put("x2",""+e.x);
			map.put("y2",""+e.y);
		
			
			// notify reasoner
			String label = ruler.getType()+": ("+s.x+", "+s.y+") ("+e.x+", "+e.y+")";
			ClientEvent ce = ClientEvent.createClientEvent(ReportInterface.this,TYPE_ACTION,label,ACTION_MEASURE);
			ce.setInput(map);
			Communicator.getInstance().sendMessage(ce);
		}
	}
	
	/**
	 * get worksheet instance
	 * @return
	 */
	private JComponent getWorksheet(){
		if(worksheet == null)
			worksheet = createWorksheet();
		return worksheet;
	}
	
	public void reloadWorksheet(){
		worksheet = null;
	}
	
	/**
	 * clear worksheet
	 */
	public void clearWorksheetSelection() {
		if (resetButtons != null) {
			for (int i = 0; i < resetButtons.size(); i++) {
				JRadioButton button = (JRadioButton) resetButtons.get(i);
				button.setSelected(true);
			}
		}
	}
	
	/**
	 * get worksheet string
	 * @return
	 */
	public String getWorksheetString() {
		StringBuffer worksheetString = new StringBuffer();
		if (worksheetButtons != null) {
			JTabbedPane tabPane = (JTabbedPane) getWorksheet().getComponent(0);
			Container tab = (Container) tabPane.getSelectedComponent();
			int sel = tabPane.getSelectedIndex();
			for (int i = 0; i < worksheetButtons.size(); i++) {
				JRadioButton button = (JRadioButton) worksheetButtons.get(i);
				// if selected tab, has this button
				if(UIHelper.contains(tab,button)){
					if (button.isSelected()) {
						String cui = button.getActionCommand();
						// add string
						if (cui != null) {
							String sep = (worksheetString.length() == 0) ? "" : ",";
							worksheetString.append(sep + UIHelper.getTextFromName(cui));
						} else
							System.err.println("ERROR: Could not find Concept for " + cui);
					}
				}
			}
			
			// add total score
			if(totalScoreText[sel] != null){
				worksheetString.append("\n"+tabPane.getTitleAt(sel)+": "+totalScoreText[sel].getText());
			}
		}
	
		return "\n"+worksheetString.toString();
	}

	
	/**
	 * get report 
	 * @return
	 */
	public String getReport(){
		return textPane.getText();
	}

	public void notifyReport(){
		String text = getReport();
		if(text.length() > 0){
			ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_REPORT,text,ACTION_SUBMIT);
			ce.setInput(text);
			Communicator.getInstance().sendMessage(ce);
		}
	}
	
	
	/**
	 * set read-only flag
	 * @param b
	 *
	public void setReadOnly(boolean b){
		readOnly = b;
		// disable buttons
		if(toolbar != null){
			UIHelper.setEnabled(toolbar,new String [0],!b);
		}
		// disable report
		doc.setReadOnly(b);
	}*/

	/**
	 * Create worksheet
	 */
	private JComponent createWorksheet() {
		JTabbedPane workPanel = new JTabbedPane();
		workPanel.setPreferredSize(new Dimension(700,600));
		
		// build worksheet
		worksheetButtons = new ArrayList<AbstractButton>();
		resetButtons = new ArrayList<AbstractButton>();
		
		
		IOntology ont = getDomainOntology();
		IClass w = ont.getClass(OntologyHelper.WORKSHEET);
		IClass [] children = w.getDirectSubClasses();
		totalScoreText = new JTextField [ children.length];
		
		// iterate over worksheets
		for(int tab=0;tab < children.length; tab++){
			IClass work = children[tab];
			JPanel panel = new JPanel();
			// setup fonts
			Font defaultFont = panel.getFont();
			Font head = defaultFont.deriveFont(Font.BOLD);
			Font bold = defaultFont.deriveFont(Font.BOLD);
			Font plain = defaultFont.deriveFont(Font.PLAIN);

			// setup other panel attributes
			panel.setBackground(Color.white);
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			// get components
			IClass totalScore = null;
			
			// iterate over features
			for (IClass feature: work.getDirectSubClasses()) {
				
				// if this feature is a general number panel
				if(OntologyHelper.isNumber(feature)){
					totalScore = work;
				} else {
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
			
			// add total score 
			if(totalScore != null){
				JLabel featureLbl = createWorksheetLabel(totalScore,true);
				featureLbl.setFont(head);
								
				// totalScore
				totalScoreText[tab] = new JTextField(10);
				totalScoreText[tab].setDocument(new UIHelper.IntegerDocument());
				totalScoreText[tab].setEditable(false);
				totalScoreText[tab].setBackground(Color.white);
				totalScoreText[tab].setMaximumSize(new Dimension(200,25));
				totalScoreText[tab].setHorizontalAlignment(JTextField.CENTER);
				totalScoreText[tab].setAlignmentX(JComponent.CENTER_ALIGNMENT);
				
				// add panel
				//JPanel p  = new JPanel();
				//p.setLayout(new FlowLayout());
				//p.add(featureLbl);
				//p.add(totalScoreText);
				panel.add(featureLbl);
				panel.add(totalScoreText[tab]);
			}
			
			
			
			JScrollPane scroll = new JScrollPane(panel);
			//scroll.setPreferredSize(new Dimension(700,600));
			scroll.getVerticalScrollBar().setUnitIncrement(30);
			workPanel.addTab(UIHelper.getTextFromName(work.getName()),scroll);
		}
		//return workPanel;
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(workPanel,BorderLayout.CENTER);
		return p;
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
	 * block messages generated by this module, usefull for playback
	 */
	private void setBlockMessages(boolean b){
		blockMessages = b;
	}
	
	
	public void debug(Object obj){
		if(debug){
			System.out.println(obj);
		}
	}
	
	public void debug(){
		debug("interface concept registry:\t"+registry);
	}


	public void propertyChange(PropertyChangeEvent e) {
		if(edu.pitt.slideviewer.Constants.UPDATE_SHAPE.equals(e.getPropertyName())){
			notifyRuler();
		}
	}
}
