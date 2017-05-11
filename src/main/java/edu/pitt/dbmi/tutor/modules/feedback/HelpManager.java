package edu.pitt.dbmi.tutor.modules.feedback;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.ActionEntry;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.MessageEntry;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.beans.ScenarioEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.dbmi.tutor.beans.ShapeEntry;
import edu.pitt.dbmi.tutor.beans.SlideEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.FeedbackModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.ReasoningModule;
import edu.pitt.dbmi.tutor.model.StudentModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.ui.GlossaryManager;
import edu.pitt.dbmi.tutor.util.Config;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import edu.pitt.dbmi.tutor.util.Eggs;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.dbmi.tutor.util.UIHelper.HTMLPanel;

/**
 * this class takes care of feedback presentation
 * @author Administrator
 */
public class HelpManager implements FeedbackModule, ActionListener {
	private JPanel messageComponent;
	private JEditorPane messagePane, errorPane;
	private JTextField levelCounter;
	private JButton hint,next,prev,clear;
	private Properties defaultConfig;
	private boolean enabled = true,interactive = true;
	private ScenarioSet scenarioSet;
	//private ScenarioEntry scenarioEntry;
	private List<MessageEntry> messageEntries;
	private List<Action> actions;
	private MessageEntry messageEntry;
	//private ConceptEntry conceptEntry;
	private CaseEntry caseEntry;
	private int currentLevel;
	private String currentResponse;
	private Tutor tutor;
	private StudentModule student;
	private boolean hintMode,flashConcept;
	private int feedbackMode;
	private boolean errorPopupMode, errorPanelMode,animateHint,flashPanel;
	private String noFeedbackText;
	private String emptyText = "",emptyText2 = "";
	private Collection<Message> tutorResponses;
	private Map<Integer,ConceptEntry> tutorResponseConcepts;
	//private Map<Integer,String> clientEventTypes;
	private Icon hintIcon, animateIcon;
	private ExpertModule expertModule;
	private boolean loaded,autoHint,flushingResponses,showMore,behaviorShowExample;
	private Boolean interactiveRequest;
	private Object buttonSource;
	private GlossaryManager glossaryManager;
	
	// auto hint options
	private Thread hintThroughThread;
	private boolean hintThroughResponse;
	private double HINT_DELAY_SLOW = 3;
	private double HINT_DELAY_FAST = .25;
	private int BOLD_TAG_LIMIT = 100;
	private int eggHintCount = 0;
	private Timer eggHintTimer;
	private Point location;
	
	/**
	 * init help manager
	 */
	private void setup(){
		int mode = 0;
		
		
		// setup feedback mode
		if(Config.hasProperty(this,"feedback.mode")){
			mode = parseFeedbackMode(Config.getProperty(this,"feedback.mode"));
		}else{
			if(Config.getBooleanProperty(this,"feedback.hint"))
				mode |= HINT_FEEDBACK;
			if(Config.getBooleanProperty(this,"feedback.error"))
			mode |= ERROR_FEEDBACK;
			if(Config.getBooleanProperty(this,"feedback.color.concepts"))
			mode |= COLOR_FEEDBACK;
		}
		if(Config.getBooleanProperty(this,"feedback.summary"))
			mode |= SUMMARY_FEEDBACK;
		
			
		// setup some config values
		String str = Config.getProperty(this,"feedback.error.mode");
		errorPopupMode = "popup".equalsIgnoreCase(str);
		errorPanelMode = "panel".equalsIgnoreCase(str);
		//flashConcept = Config.getBooleanProperty(this,"feedback.flash.concepts");
		animateHint = Config.getBooleanProperty(this,"no.feedback.animation");
		noFeedbackText = "<br><center><font size='6' color='gray'><b>"+
				Config.getProperty(this,"no.feedback.text")+"</b></font></center>";
		flashPanel = Config.getBooleanProperty(this,"feedback.flash.panel");
		showMore = Config.getBooleanProperty(this,"show.more.in.panel");
		behaviorShowExample = Config.getBooleanProperty(this,"behavior.glossary.show.example");
		
		// setup responses
		tutorResponses = new Stack<Message>();
		tutorResponseConcepts = new HashMap<Integer,ConceptEntry>();
		messageEntries = new ArrayList<MessageEntry>();
		actions = new ArrayList<Action>();
		//clientEventTypes = new HashMap<Integer, String>();
		
		// set feedback mode
		setFeedackMode(mode);
		
		eggHintTimer = new Timer(3000,new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eggHintCount = 0;
			}
		});
		eggHintTimer.setRepeats(false);
	}
	
	
	public void reconfigure(){
		setup();
	}
	
	/**
	 * do glossary
	 */
	private void doGlossary(){
		ConceptEntry e = messageEntry.getConceptEntry();
		
		if(e != null){
			// notify of action
			ClientEvent ce = e.getDefinedFeature().getClientEvent(this,Constants.ACTION_GLOSSARY);
			MessageUtils.getInstance(this).flushInterfaceEvents(ce);
			Communicator.getInstance().sendMessage(ce);
			
			// resolve node
			expertModule.resolveConceptEntry(e.getDefinedFeature());
			
			// show glossary panel
			if(glossaryManager == null){
				glossaryManager = new GlossaryManager();
				glossaryManager.setShowExampleImage(behaviorShowExample);
			}
			
			if(location != null){
				Point loc = location;
				SwingUtilities.convertPointToScreen(loc,messagePane);
				glossaryManager.showGlossary(e.getDefinedFeature(),getComponent(),loc);
			}
		}
	}
	
	/**
	 * Get/create GUI component
	 * 
	 */
	public Component getComponent() {
		if(messageComponent == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.setMinimumSize(new Dimension(200,200));
			
			// create message window
			messagePane = new UIHelper.HTMLPanel();
			messagePane.setEditable(false);
			messagePane.addHyperlinkListener( new HyperlinkListener(){
	            public void hyperlinkUpdate(HyperlinkEvent e){
	               if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	            	   try{
	                	   location = messagePane.modelToView(e.getSourceElement().getStartOffset()).getLocation();
	                   }catch(Exception ex){
	                	   //ex.printStackTrace();
	                   }
	            	    //JEditorPane edit = (JEditorPane) e.getSource();
	                    String key = e.getDescription();
	                    
	                    // send interface event
	                    MessageUtils.getInstance(HelpManager.this).addInterfaceEvent(
	                    InterfaceEvent.createInterfaceEvent(HelpManager.this,TYPE_HYPERLINK,key,ACTION_SELECTED));
	                    
	                    // react to hyperlinks
	                    if(key.equalsIgnoreCase("more")){
	                    	if(currentLevel < getLevelCount()-1)
	            				requestLevel(++currentLevel);
	                    }else if(key.startsWith("slide=")){
	                    	/*
	                    	String slide = key.substring(6);
	                    	if(tutor.getHelpManager().getCurrentMessageEntry() != null){
	                    		System.out.println("point to different slide "+slide);
	                    		tutor.setImage(slide);
	                    		//tutor.getHelpManager().getCurrentMessageEntry().point();
	                    	}
	                    	*/
	                    }else if(key.startsWith("order=")){
	                    	/*
	                    	String slide = key.substring(6);
	                    	if(tutor.getHelpManager().getCurrentMessageEntry() != null){
	                    		System.out.println("order different slide "+slide);
	                    		tutor.getSlideOrder().showOrderLevel(tutor.getFrame());
	                    		//tutor.getHelpManager().getCurrentMessageEntry().point();
	                    	}
	                    	*/
	                    }else if(key.contains("definition")){
	                    	doGlossary();
	                    }
	                }
	            }
	        });
			emptyText = messagePane.getText();
			
			if(errorPopupMode){
				errorPane = new UIHelper.HTMLPanel();
				errorPane.setEditable(false);
			}
			
			// create side panel
			Icon icon =  UIHelper.getIcon(this,"icon.hint");
			Dimension size = new Dimension(icon.getIconWidth()+10,80);
			hint = new JButton("Hint",icon);
			hint.setVerticalTextPosition(SwingConstants.BOTTOM);
			hint.setHorizontalTextPosition(SwingConstants.CENTER);
			hint.setPreferredSize(size);
			hint.setMaximumSize(size);
			hint.setToolTipText("Request Hint");
			hint.setEnabled(enabled);
			hint.addActionListener(this);
			
			// create bottom panel
			JPanel bpanel = new JPanel();
			bpanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			
			prev = new JButton("Previous",UIHelper.getIcon(this,"icon.previous"));
			prev.setToolTipText("Previous Hint");
			setButtonParams(prev);
			bpanel.add(prev);
			
			levelCounter = new JTextField(4);
			levelCounter.setEditable(false);
			levelCounter.setHorizontalAlignment(JTextField.CENTER);
			levelCounter.setBorder(new EmptyBorder(0,0,0,0));
			bpanel.add(levelCounter);
			
			next = new JButton("Next",UIHelper.getIcon(this,"icon.next"));
			next.setToolTipText("Next Hint");
			next.setHorizontalTextPosition(SwingConstants.LEFT);
			setButtonParams(next);
			bpanel.add(next);
			
			clear = new JButton("Clear");
			clear.setToolTipText("Clear Message Window");
			setButtonParams(clear);
			bpanel.add(new JLabel("     "));
			bpanel.add(clear);
			
			// add to panel
			panel.add(hint,BorderLayout.WEST);
			panel.add(new JScrollPane(messagePane),BorderLayout.CENTER);
			panel.add(bpanel,BorderLayout.SOUTH);
			
			// disable buttons by default
			next.setEnabled( false );
			prev.setEnabled( false );
			clear.setEnabled( false );
			
			
			// add to debug menu
			JMenu debugM = ITS.getInstance().getDebugMenu();
			if(!UIHelper.hasMenuItem(debugM,"Auto Hint Through")){
				debugM.addSeparator();
				debugM.add(UIHelper.createMenuItem("auto-hint-through","Auto Hint Through",Config.getIconProperty("icon.menu.help"),this));
			}
			
			
			messageComponent = panel;
		}
		return messageComponent;
	}
	
	// set button size
	private void setButtonParams( JButton b ) {
		Dimension d = new Dimension(115,35); // 90,25
		b.setMaximumSize(d);
		b.setMinimumSize(d);
		b.setPreferredSize(d);
		b.addActionListener(this);
	}
	
	public void dispose() {
	}
	
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setStudentModule(StudentModule module){
		student = module;
	}

	
	/**
	 * set feedback mode. The mode is a bitmap of several flags:
	 * HINT_FEEDBACK,ERROR_FEEDBACK,COLOR_FEEDBACK,
	 * SUMMARY_FEEDBACK, GLOSSARY_FEEDBACK
	 * @param mode
	 */
	public void setFeedackMode(int mode){
		feedbackMode = mode;
		flushResponses();
		setHintEnabled(isMode(HINT_FEEDBACK));
	}
	
	/**
	 * get feedback mode. The mode is a bitmap of flags:
	 * HINT_FEEDBACK,ERROR_FEEDBACK,COLOR_FEEDBACK,
	 * SUMMARY_FEEDBACK, GLOSSARY_FEEDBACK
	 * @return
	 */
	public int getFeedbackMode(){
		return feedbackMode;
	}

	
	
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "This module provides users with feedback in the form of hint messages and error messages. " +
			   "It instucts the interface to provide visual cues on which user actions were correct and" +
			   "incorrect.";
	}

	public String getName() {
		return "Help Manager";
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public String getVersion() {
		return "1.0";
	}

	/**
	 * clear current window
	 */
	public void reset() {
		clear();
	}
	
	
	private void clear() {
		undoActions(actions);
		String oldText = messagePane.getText();
		
		for(MessageEntry m: messageEntries)
			undoActions(m.getActions());
		
		if(!isMode(HINT_FEEDBACK) && animateHint){
			messagePane.setText(noFeedbackText);
			emptyText2 = messagePane.getText();
		}else
			messagePane.setText("");
		levelCounter.setText("");
		currentLevel = 0;
		
		messageEntries.clear();
		actions.clear();
		
		syncButtons();
		
		// generate IE Event for button clearance
		if(caseEntry != null && !emptyText.equals(oldText) && !emptyText2.equals(oldText)){
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_BUTTON,clear.getText(),ACTION_SELECTED);
			if(buttonSource != clear)
				((Map)ie.getInput()).put("auto","true");
			sendMessage(ie);
		}
	}
	
	
	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b){
		this.enabled = b;
		if(messageComponent != null){
			animateHint = false;
			setHintEnabled(b && isMode(HINT_FEEDBACK));
			animateHint = Config.getBooleanProperty(this,"no.feedback.animation");
		}
	}
	
	/**
	 * is component enabled
	 * @return
	 */
	public boolean isEnabled(){
		return enabled;
	}
	
	/**
	 * set component interactive flag
	 * @param b
	 */
	public void setInteractive(boolean b){
		interactive = (interactiveRequest != null)?interactiveRequest.booleanValue():b;
	}
	
	/**
	 * is interactive
	 * @return
	 */
	public boolean isInteractive(){
		return interactive;
	}
	
	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setExpertModule(ExpertModule module){
		expertModule = module;
	}
	
	/**
	 * load resources associated w/ this module
	 */
	public void load(){
		if(loaded)
			return;
		setup();
		
		// fetch help location
		String location = Config.getProperty("tutor."+getTutor().getId()+".help");
		if(TextHelper.isEmpty(location))
			location = Config.getProperty(this,"tutor.help.location");
		
		scenarioSet = getScenarioSet(location);
		loaded = true;
	}
	
	/**
	 * get scenario set for a given domain
	 * @return
	 */
	private ScenarioSet getScenarioSet(String location){
		ScenarioSet scenarios = new ScenarioSet();
		try{
			File f = new File(location);
			if(f.exists()){
				scenarios.load(new FileInputStream(f));
			}else if(UIHelper.isURL(location)){
				URL url = new URL(location);
				scenarios.load(url.openStream());
			}else {
				scenarios.load(getClass().getResourceAsStream(location));
			}
		}catch(Exception ex){
			Config.getLogger().severe("Could not load help file from "+location);
			ex.printStackTrace();
		}
		return scenarios;
	}
	
	
	/**
	 * get current scenario set
	 */
	public ScenarioSet getScenarioSet(){
		return scenarioSet;
	}
	
	
	/**
	 * set current case entry
	 * @param problem
	 */
	public void setCaseEntry(CaseEntry problem){
		caseEntry = problem;
		interactiveRequest = null;
		// reset client events and tutor hintThroughResponse
		//clientEventTypes.clear();
		tutorResponses.clear();
		tutorResponseConcepts.clear();
	}
	
	
	/**
	 * is in some mode
	 * @param mode
	 * @return
	 */
	private boolean isMode(int mode){
		return (mode & feedbackMode) == mode;
	}
	
	
	/**
	 * request new hint from the reasoning module.
	 * A hint is normally composed as a set of messages
	 */
	public void requestHint(){
		requestHint(TYPE_HINT);
	}
	
	
	/**
	 * request new hint from the reasoning module.
	 * @param String label - request a specific hint s.a. in regards to a concept name
	 * A hint is normally composed as a set of messages
	 */
	public void requestHint(String label){
		// send message about this concept
		if(isMode(HINT_FEEDBACK)){
			// generate IE Event for button clearance
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_BUTTON,hint.getText(),ACTION_SELECTED);
			if(buttonSource != hint)
				((Map)ie.getInput()).put("auto","true");
			
			ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_HINT,label,ACTION_REQUEST);
			if(autoHint)
				((Map)ce.getInput()).put("auto","true");
			ie.setClientEvent(ce);
			
			sendMessage(ie);
			sendMessage(ce);
			
		}
		// notify of hints
		flash();
	}
	
	/**
	 * send out message
	 * @param ce
	 */
	private void sendMessage(Message ce){
		//clientEventTypes.put(ce.getMessageId(),ce.getType());
		Communicator.getInstance().sendMessage(ce);
	}
	
	/**
	 * request/display a level hint or bug with a given offset
	 * Use getHintLevelCount() to determine the number of levels
	 * @param i
	 */
	public void requestLevel(int offset){
		if(!messageEntries.isEmpty()){
			// sync buttons
			syncButtons();
			
			// cap the offset
			if(offset < 0)
				offset = 0;
			if(offset >= getLevelCount())
				offset = getLevelCount()-1;
			
			// notify
			String label = (offset+1)+" of "+getLevelCount();
			
			// display
			if(hintMode){
				// send message about this action
				if(isMode(HINT_FEEDBACK)){
					// generate IE Event for button clearance
					InterfaceEvent ie = null;
					if(buttonSource != null && buttonSource instanceof JButton){
						ie = InterfaceEvent.createInterfaceEvent(this,TYPE_BUTTON,((JButton)buttonSource).getText(),ACTION_SELECTED);
					}
					ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_HINT_LEVEL,label,ACTION_REQUEST);
					if(autoHint)
						((Map)ce.getInput()).put("auto","true");
					if(ie != null){
						ie.setClientEvent(ce);
						sendMessage(ie);
					}
					sendMessage(ce);
				}
				displayHint(offset);
			}else{
				displayError(offset);
			}
			// display count
			levelCounter.setText(label);
		}
	}
	
	
	/**
	 * get number of hint levels in the current
	 * @return
	 */
	public int getLevelCount(){
		return messageEntries.size();
	}
	
	
	/**
	 * enable/disable buttons appropriately
	 */
	private void syncButtons(){
		prev.setEnabled(currentLevel > 0);
		next.setEnabled(currentLevel < getLevelCount()-1);
		clear.setEnabled(getLevelCount() > 0);
		messagePane.setBorder(null);
	}
	
	
	/**
	 * actions
	 */
	public void actionPerformed(ActionEvent e) {
		if(!isInteractive())
			return;
		buttonSource = e.getSource();
		if(e.getSource() == hint){
			runEgg();
			if(!messageEntries.isEmpty() && currentLevel < getLevelCount()-1){
				requestLevel(++currentLevel);
	        }else{
	        	requestHint();
	        }
		}else if(e.getSource() == next){
			if(currentLevel < getLevelCount()-1)
				requestLevel(++currentLevel);
		}else if(e.getSource() == prev){
			if(currentLevel > 0)
				requestLevel(--currentLevel);
		}else if(e.getSource() == clear){
			clear();
		}else if("auto-hint-through".equals(e.getActionCommand())){
			doAutoHint(null);
		}
		buttonSource = null;
	}	
	
	private void runEgg(){
		eggHintCount ++;
		eggHintTimer.stop();
		eggHintTimer.start();
		if(eggHintCount > 10){
			Eggs.nerdRun((JFrame)Config.getMainFrame());
			eggHintCount = 0;
		}
	}
	
	
	public Tutor getTutor() {
		return tutor;
	}


	public void setTutor(Tutor tutor) {
		this.tutor = tutor;
	}


	/**
	 * display a hint portion of the scenario entry
	 */
	private void displayHint(int offset){
		if(!messageEntries.isEmpty()){
			// undo previous msg
			if(messageEntry != null)
				undoActions(messageEntry.getActions());
			
			// get message
			messageEntry = messageEntries.get(offset);
			
			// set text
			String text = resolveMessageText(messageEntry);

			// add 'more' link to each hint
			if(showMore && offset < (getLevelCount()-1))
				text = text + " [<a href=\"more\">more..</a>]";
			
			messagePane.setText(text);
			
			// execute actions
			doActions(messageEntry.getActions(),messageEntry.getConceptEntry());
		}
	}
	
	/**
	 * display an error portion of the scenario entry
	 */
	private void displayError(int offset){
		if(!messageEntries.isEmpty()){
			// undo previous msg
			if(messageEntry != null)
				undoActions(messageEntry.getActions());
			
			// get message
			messageEntry = messageEntries.get(offset);
			
			// execute actions
			doActions(messageEntry.getActions(),messageEntry.getConceptEntry());
			
			// set text
			String text = resolveMessageText(messageEntry);
			
			// set it in message panel if in panel mode
			if(errorPanelMode || !isInteractive()){
				// add red color to errors
				if(messageEntry.isError()){
					text = "<font color=\"red\">"+text+"</font>";
					messagePane.setBorder(new LineBorder(Color.red,3));
				}else
					messagePane.setBorder(new LineBorder(Color.green,3));
				
				// add 'more' link to each hint
				String ttext = text;
				if(showMore && offset < (getLevelCount()-1))
					ttext = ttext + " [<a href=\"more\">more..</a>]";
				
				messagePane.setText(ttext);
				flash();
			}
			
			// do a pupup if in popup mode
			if(errorPopupMode && isInteractive()){
				StringBuffer buf = new StringBuffer();
				for(MessageEntry e: messageEntries)
					buf.append(resolveMessageText(e)+"<hl>");
				errorPane.setText(buf.toString());
				JScrollPane scroll = new JScrollPane(errorPane);
				scroll.setPreferredSize(new Dimension(300,200));
				JOptionPane.showMessageDialog(Config.getMainFrame(),scroll,"Mistake",JOptionPane.ERROR_MESSAGE);
			}	
			
			
			// add messages to concept entry
			if(messageEntry.isError())
				doAction(new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT_ERROR,text),messageEntry.getConceptEntry());
		}
		
	}
	
	/**
	 * Flash message area
	 */	
	private void flash() {
		if(!flashPanel)
			return;
		
		javax.swing.Timer timer = new javax.swing.Timer( 800, new ActionListener() {
			   public void actionPerformed( ActionEvent evt ) {
			      messagePane.setBackground( Color.WHITE );
			   }
		   }
		);
		messagePane.setBackground( Color.GREEN );
        timer.setRepeats(false);
		timer.start();
	}
	
	
	/**
	 * enable & disable hints
	 * @param enabled
	 */
	private void setHintEnabled(boolean enabled){
		// build component, if not there yet
		getComponent();
		
		// don't do anything if there is no change in state
		if(hint.isEnabled() == enabled)
			return;
		
		// reset
		reset();
        		
		// lets do cool effect when we animate
		if(animateHint){
		    // setup icons
			if(hintIcon == null)
		    	hintIcon = UIHelper.getIcon(this,"icon.hint");
			if(animateIcon == null)
				animateIcon = UIHelper.getIcon(this,"icon.hint.animation");
			
			// if enabled just do the boring thing
			if(enabled){
		        hint.setIcon(hintIcon);
                hint.setEnabled(true);
                hint.setText("Hint");
                flash();
            }else{
            	SwingUtilities.invokeLater(new Runnable(){
            		public void run(){
            			hint.setText("");
            			hint.setIcon(animateIcon);
                    	hint.setEnabled(true);
        	            hint.revalidate();
        	            
        	            // clear icon in 800 ms
        	            javax.swing.Timer timer = new javax.swing.Timer(800,new ActionListener() {
        	                   public void actionPerformed( ActionEvent evt ) {
        	                	  // SwingUtilities.invokeLater(new Runnable(){
	        	                   //		public void run(){
		        	                	   hint.setIcon(null);
		        	                       messagePane.setText(noFeedbackText);
		        	                       emptyText2 = messagePane.getText();
		        	                       hint.setEnabled(false);
		        	                       flash();
	        	                   //		}
        	                	   //});
        	                   }
        	               }
        	            );
        	            timer.setRepeats(false);
        	            timer.start();
            		}
            	});
            }
            
        }else{
        	messagePane.setText("");
            hint.setEnabled(enabled);
            hint.setText((enabled)?"Hint":"");
        }
	}
	
	
	/**
	 * do actions for a given message entry
	 * @param msg
	 * @param concept
	 */
	private void doActions(List<Action> actions,ConceptEntry c){
		// execute actions
		for(Action a : actions){
			doAction(a,c);
		}
	}
	
	/**
	 * do actions for a given message entry
	 * @param msg
	 * @param concept
	 */
	private void undoActions(List<Action> actions){
		// execute actions
		for(Action a : actions ){
			undoAction(a);
		}
	}
	
	/**
	 * execute a single action
	 * @param act
	 */
	private void doAction(Action a, ConceptEntry conceptEntry){
		if(conceptEntry == null)
			return;
		
		// reset action concept
		a.setConceptEntry(conceptEntry);
		Communicator.getInstance().resolveAction(a);
		a.run();
	}
	
	/**
	 * execute a single action
	 * @param act
	 */
	private void undoAction(Action a){
		if(a.getConceptEntry() == null)
			return;
		
		if(a.isResolved())
			a.undo();
	}
	
	
	/**
	 * resolve message
	 * @param msg
	 * @param concept
	 */
	private String resolveMessageText(MessageEntry msg){
		//if(msg.isResolved())
		//	return msg.getResolvedText();
		// now resolve
		ConceptEntry concept = msg.getConceptEntry();
		if(concept == null)
			return msg.getText();
	
		// else resolve tags
		expertModule.resolveConceptEntry(concept);
		String text = msg.getText();
		if(concept != null){
			for(String tag: msg.getTags()){
				String tagText = concept.resolveTag(tag);
				if(tagText != null)
					text = text.replaceAll(tag,(tagText.length() > BOLD_TAG_LIMIT || TAG_DEFINITION.equals(tag))?tagText:"<b>"+tagText+"</b>");
			}
		}
		return text;
	}
	
	
	/**
	 * if there are messages on the queue flash them
	 * if appropriate
	 */
	private void flushResponses(){
		// dont flush responses twice
		if(flushingResponses)
			return;
	
		if(isMode(ERROR_FEEDBACK)){
			flushingResponses = true;
			boolean b = isInteractive();
			setInteractive(false);
			for(Message tr: new ArrayList<Message>(tutorResponses)){
				clear();
				receiveMessage(tr);
			}
			tutorResponses.clear();
			tutorResponseConcepts.clear();
			setInteractive(b);
			flushingResponses = false;
		}
	}
	
	
	/**
	 * recieve a message
	 */
	public void receiveMessage(Message msg) {
		// if we are doing playback from protocol, then playback hintlevel client events
		if(msg.getSender() instanceof ProtocolModule && TYPE_HINT_LEVEL.equals(msg.getType())){
			try{
				String s = msg.getLabel().contains(" of ")?" of ":"/";
				int level = Integer.parseInt(msg.getLabel().split(s)[0].trim());
				if(level > 1)
					requestLevel(level);
			}catch(Exception ex){
				Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			}
			return;
		}

		//////////////////////////////////
		//if(msg instanceof ClientEvent){
		//	clientEventTypes.put(msg.getMessageId(),msg.getType());
		//}
		
		// now lets do the real stuff
		if(msg instanceof ClientEvent && !TYPE_PRESENTATION.equals(msg.getType())){
			// even if there is no error, reset window
			clear();
		}else if(msg instanceof TutorResponse && !(msg instanceof NodeEvent)){
			TutorResponse r = (TutorResponse) msg;
		
			// set auto hint flat
			hintThroughResponse = true;
			
			// get type of what this tutor hintThroughResponse is all about
			String type = r.getType();
			String action = r.getAction();
			
			// lookup client event type
			if(r.getClientEvent() != null){
				type = r.getClientEvent().getType();
				action = r.getClientEvent().getAction();
			}
			
			
			// if we respond to hint level don't do anything, just 
			// fill in the messages
			if(TYPE_HINT_LEVEL.equals(type) && currentLevel < messageEntries.size()){
				Map<String,String> map = new LinkedHashMap<String,String>();
				map.put("message-"+(currentLevel+1),resolveMessageText(messageEntries.get(currentLevel)));
				msg.setInput(map);
				return;
			// ignore confirms for TYPE_PRESENTATIONS
			}else if(TYPE_PRESENTATION.equals(type)){
				// if viewer movenents that are just OK, then don't do anything
				if(ERROR_OK.equals(r.getError()))
					return;
				// if viewer movements produce some sort of action, clear window to make room for resonse
				else
					clear();
			// if action was from self-check don't hanlde
			}else if(Arrays.asList(ACTION_GLOSSARY,ACTION_SELF_CHECK).contains(action)){
				//TODO: maybe in future present coloring feedback as well
				return;
			}
		
			
			// clear before HINT or if responses don't match
			hintMode = false;
			if(TYPE_HINT.equals(type)){
				clear();
				hintMode = true;
			}
			
			// check if summary is in order
			if(ACTION_SUMMARY.equals(action) && isMode(SUMMARY_FEEDBACK)){
				doSummary(r);
				return;
			}
			
			
			// lookup next concept
			ConceptEntry conceptEntry = getNextStep(r);
			if(conceptEntry instanceof ActionEntry)
				conceptEntry = conceptEntry.getParentEntry();
		
			// lookup scenario entry
			ScenarioEntry scenarioEntry = scenarioSet.getScenarioEntry(r.getError());
			
			// color concepts first based on feedback
			//TODO: what to do when irrelevant follows ERROR
			if(isMode(COLOR_FEEDBACK) && conceptEntry != null && Arrays.asList(ACTION_ADDED,ACTION_REFINE,ACTION_REMOVED).contains(action)){
				String input = r.getResponse();
				if(isIrrelevant(r.getError()))
					input = RESPONSE_IRRELEVANT;
				doAction(new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_COLOR_CONCEPT,input),conceptEntry);
			}
			
			// deliver hint or error feedback, if not OK
			if(!isOK(r.getError())){
				List<MessageEntry> currentMessages = new ArrayList<MessageEntry>();
				
				if(scenarioEntry != null){
					// save scenario entry code
					r.setCode(""+scenarioEntry.getErrorCode());
					
					// if hint, then add hint messages
					if(hintMode){
						for(MessageEntry e: scenarioEntry.getHintMessages()){
							MessageEntry clone = e.clone();
							clone.setConceptEntry(conceptEntry);
							clone.setError(false);
							currentMessages.add(clone);
							//messageEntries.add(clone);
						}
					// else add error messages
					}else {
						for(MessageEntry e: scenarioEntry.getErrorMessages()){
							
							// set resolved message in a clone
							MessageEntry clone = e.clone();
							clone.setConceptEntry(conceptEntry);
							clone.setError(RESPONSE_FAILURE.equals(r.getResponse()));
							clone.setResolvedText(resolveMessageText(clone));
							
							// if not alread there
							if(!currentMessages.contains(clone)){
								if(clone.isError() && !isIrrelevant(r.getError()))
									currentMessages.add(0,clone);
								else
									currentMessages.add(clone);
							}else if(isMode(ERROR_FEEDBACK)){
								// if a message is already there we 
								// still want to display the message attached to node
								doAction(new Action(InterfaceModule.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT_ERROR,
										 "<font color=\"red\">"+clone.getResolvedText()+"</font>"),conceptEntry);
							}
						}
					}
				}else if(!isIrrelevant(r.getError())){
					// if scenario not found, put in debug code in text
					/*
					MessageEntry e = new MessageEntry();
					e.setText("SCENARIO CODE: "+r.getError());
					e.setError(RESPONSE_FAILURE.equals(r.getResponse()));
					e.setConceptEntry(conceptEntry);
					if(isMode(ERROR_FEEDBACK))
						currentMessages.add(e.clone());
					*/
				}
				
				
				// filter messges
				filterMessages(currentMessages);
				
				// new if appropriate add all of the messages 
				if((hintMode && isMode(HINT_FEEDBACK)) || (!hintMode && isMode(ERROR_FEEDBACK)))
					messageEntries.addAll(currentMessages);
				
				// load appropriate hintThroughResponse
				requestLevel(currentLevel);
				
				// before we reset input, lets save the REAL concept entry if available
				if(!isMode(COLOR_FEEDBACK|ERROR_FEEDBACK) && r.getInput() instanceof ConceptEntry)
					tutorResponseConcepts.put(msg.getMessageId(),(ConceptEntry)msg.getInput());
				
				// reset input in TR to contain messages ????
				int i = 1;
				for(MessageEntry e: currentMessages){
					r.addInput("message-"+(i++),resolveMessageText(e));
				}
					
				// resolve scenari actions
				if((hintMode && isMode(HINT_FEEDBACK)) || (!hintMode && isMode(ERROR_FEEDBACK))){
					doActions(scenarioEntry.getActions(),conceptEntry);
				}
				
			}else if(scenarioEntry != null){
				// save scenario entry code
				r.setCode(""+scenarioEntry.getErrorCode());
			}
	
			// save tutor hintThroughResponse
			if(!isMode(COLOR_FEEDBACK|ERROR_FEEDBACK)){
				tutorResponses.add(msg);
			}
		}

	}
	
	
	/**
	 * do summary feedback
	 * @param r
	 */

	private void doSummary(TutorResponse r) {
		if(r.getInput() instanceof List){
			final List<ConceptEntry> list = (List<ConceptEntry>) r.getInput();
			(new Thread(new Runnable(){
				public void run(){
					// some summary variables
					int completeFeatures = 0,missingFeatures = 0, incompleteFeatures = 0,numberOfProblems = 0;
					
					
					// report outstanding problems first
					StringBuffer buffer = new StringBuffer();
					buffer.append("<h3>Outstanding problems</h3><ol type='1' compact>");
					boolean missedGoals = false;
					for(ConceptEntry entry: list){
						String error  = entry.getError();
						if(error == null){
							completeFeatures ++;
							continue;
						}
						
						// lookup scenario entry
						ScenarioEntry scenarioEntry = scenarioSet.getScenarioEntry(error);
						if(scenarioEntry != null){
							List<MessageEntry> msgs = scenarioEntry.getErrorMessages();
							if(msgs.isEmpty()){
								msgs = scenarioEntry.getHintMessages();
							}
							
							// switch over to missed goal
							if(entry.getConceptStatus() == ConceptEntry.UNKNOWN && !missedGoals){
								missedGoals = true;
								buffer.append("</ol>");
								buffer.append("<h3>Missed features</h3><ol type='1' compact>");
							}
							
							// calculate some values
							if(missedGoals){
								// don't show some errors as missing findings
								if(Arrays.asList(HINT_MISSING_ATTRIBUTE,HINT_MISSING_NEGATION,
												 HINT_MISSING_ANOTHER_ATTRIBUTE).contains(entry.getError())){
									incompleteFeatures ++;
									continue;
								}
								
								missingFeatures++;
							}else
								numberOfProblems ++;
							
							// get relevant message
							if(!msgs.isEmpty()){
								MessageEntry msg = msgs.get(msgs.size()-1);
								msg = msg.clone();
								msg.setConceptEntry(entry);
								buffer.append("<li>"+resolveMessageText(msg)+"</li>");
							}else{
								System.out.println("Error:"+scenarioEntry.getName());
							}
						}
					}
					buffer.append("</ol>");
					
					int totalFeatures = completeFeatures+incompleteFeatures+missingFeatures;
					//caseEntry.getReportFindings().size();
					//completeFeatures = totalFeatures - missingFeatures-incompleteFeatures;
					
					StringBuffer str = new StringBuffer();
					str.append("<h1>Case Summary</h1><hr><p>");
					str.append("Correctly identified features: "+completeFeatures+" out of "+totalFeatures+"<br>");
					str.append("Number of missed features: "+missingFeatures+"<br>");
					str.append("Number of encountered problems: "+numberOfProblems+"<p>");
					str.append(buffer);
					
					
					// create dialog
					HTMLPanel panel = new HTMLPanel();
					panel.setText(str.toString());
					JScrollPane scroll = new JScrollPane(panel);
					scroll.setPreferredSize(new Dimension(600,600));
						
					JOptionPane.showMessageDialog(Config.getMainFrame(),scroll,"Case Summary",JOptionPane.PLAIN_MESSAGE);
				}
			})).start();
			
		}
	}


	/**
	 * drop messages, that cannot be appropriately displayed
	 * due to the fact that some concept will lack shapes
	 * @param msg
	 */
	private void filterMessages(List<MessageEntry> list){
		// go over each message
		for(MessageEntry msg: new ArrayList<MessageEntry>(list)){
			boolean toremove = false;
			ConceptEntry e = msg.getConceptEntry();
			if(e != null){
				List<Action> actions = msg.getActions();
				// if we have message w/ concept entry and actions
				if(!actions.isEmpty()){
					for(Action a: actions){
						String in = ""+a.getInput();
						if(in.startsWith(POINTER_INPUT_EXAMPLE)){
							if(in.endsWith(POINTER_INPUT_SUFFIX_NO_RULER)){
								toremove = true;
								for(ShapeEntry s: caseEntry.getExamples(e)){
									if(!"Ruler".equalsIgnoreCase(s.getType())){
										toremove = false;
										break;
									}
								}
							}else
								toremove = caseEntry.getExamples(e).length == 0;
							break;
						}else if(in.startsWith(POINTER_INPUT_LOCATION)){
							toremove = caseEntry.getLocations(e).length == 0;
							break;
						}
					}
				}
				
				// remove message
				if(toremove){
					list.remove(msg);
				}
			}
		}
	}
	
	/**
	 * should hint be delivered?
	 * @param tr
	 * @return
	 *
	private boolean isHint(TutorResponse tr){
		return  isMode(HINT_FEEDBACK) && RESPONSE_CONFIRM.equals(tr.getResponse()) && 
		 		scenarioSet.getScenarioEntry(tr.getError()) != null;
	}
	*/
	
	/**
	 * should hint be delivered?
	 * @param tr
	 * @return
	 *
	private boolean isError(TutorResponse tr){
		return  isMode(ERROR_FEEDBACK) && RESPONSE_FAILURE.equals(tr.getResponse()) && 
		 		scenarioSet.getScenarioEntry(tr.getError()) != null;
	}
	*/
	/**
	 * should concept be colored based on flags and TUTOR RESPONSE
	 * @param tr
	 * @return
	 *
	private boolean isColor(TutorResponse tr){
		return isMode(COLOR_FEEDBACK) && 
			  (ACTION_ADDED.equals(tr.getAction()) || 
			   ACTION_REMOVED.equals(tr.getAction()) ||
			   ACTION_REFINE.equals(tr.getAction()));
	}
	*/
	/**
	 * get next step
	 * @param tr
	 * @return
	 */
	private ConceptEntry getNextStep(TutorResponse tr){
		// if input is a concept entry, then just use it
		if(tr.getInput() != null && tr.getInput() instanceof ConceptEntry)
			return (ConceptEntry) tr.getInput();
		
		// if input is a list of concept entries, then just use it
		if(tr.getInput() != null && tr.getInput() instanceof List){
			if(!((List)tr.getInput()).isEmpty()){
				Object o = ((List)tr.getInput()).get(0);
				if(o instanceof ConceptEntry)
					return (ConceptEntry) o;
			}
		}
		
		// see if we have saved it from before
		if(tutorResponseConcepts.containsKey(tr.getMessageId()))
			return tutorResponseConcepts.get(tr.getMessageId());
		
		//return null;
		// MAYBE IT IS after all
		//THIS IS NO LONGER A GOOD IDEA, sicne NextStep is truly a next step
		// if concept is not attached, rebuild it
		ConceptEntry entry = ConceptEntry.getConceptEntry(tr.getResponseConcept());
			
		// add error to it
		if(entry != null && !ERROR_OK.equals(tr.getError()))
			entry.addError(tr.getError());
		
		return entry;
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
				
		// figure out which operations to set
		if(POINTER_ACTION_FEEDBACK_MODE.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				private int mode;
				public void run() {
					mode = getFeedbackMode();
					setFeedackMode(parseFeedbackMode(act.getInput()));
				}
				public void undo(){
					setFeedackMode(mode);
				}
			};
		}else if(POINTER_ACTION_AUTO_HINT.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					doAutoHint(act.getInput());
				}
				public void undo(){
				}
			};
		}else if(POINTER_ACTION_SET_INTERACTIVE.equalsIgnoreCase(action.getAction())){
			final boolean oldVal = isInteractive();
			//set viewer location
			oper = new Operation(){
				public void run() {
					interactiveRequest = new Boolean(act.getInput());
					setInteractive(Boolean.parseBoolean(act.getInput()));
				}
				public void undo(){
					setInteractive(oldVal);
				}
			};
		}
		
		// set operations
		action.setOperation(oper);
	}
	
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions(){
		List<Action> actions = new ArrayList<Action>();
		actions.add(new Action(FeedbackModule.class.getSimpleName(),POINTER_ACTION_FEEDBACK_MODE,"all","Instant feedback mode. " +
				"All tutor feedback to user's actions is immediatly delivered to a user. Hinting is available."));
		actions.add(new Action(FeedbackModule.class.getSimpleName(),POINTER_ACTION_FEEDBACK_MODE,"hint|error|color|summary",
				"Each flag that is passed enables the appropriate feedback mode. For example passing <font color=green>error|color</font> will " +
				"enable error feedback and color user's nodes, however hints will not be available."));
		actions.add(new Action(FeedbackModule.class.getSimpleName(),POINTER_ACTION_FEEDBACK_MODE,"none","Delayed feedback mode. " +
				"All tutor feedback to user's actions is suppressed. Hinting is not available."));
		actions.add(new Action(FeedbackModule.class.getSimpleName(),POINTER_ACTION_AUTO_HINT,"[instant]",
				"Hint through a case and assert missing evidence and diagnosis. " +
				"If <font color=green>instant</font> flag is used, the hint through is done really fast and does not draw user's attention to hint messages."));
		actions.add(new Action(FeedbackModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"true","Enable interactive mode in a tutor."));
		actions.add(new Action(FeedbackModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"false","Disable interactive mode in a tutor."));
		return actions.toArray(new Action [0]);
	}
	
	/**
	 * parse feedback mode as a number or as a string
	 * 
	 * @param x
	 * @return
	 */
	private int parseFeedbackMode(String x){
		// return default
		if(TextHelper.isEmpty(x))
			return HINT_FEEDBACK | ERROR_FEEDBACK | COLOR_FEEDBACK;
		
		x = x.trim().toLowerCase();
		
		// if input is a number, parse it
		if(x.matches("\\d+")){
			return Integer.parseInt(x);
		}
		
		// if input is none, return no-feedback
		if(x.equals("none"))
			return 0;
		else if(x.equals("all"))
			return HINT_FEEDBACK | ERROR_FEEDBACK | COLOR_FEEDBACK ;
		
		// detailed feedback mode
		int mode = 0;
		for(String s: x.split("[\\|,.:;]")){
			s = s.trim();
			if(s.equals("hint"))
				mode |= HINT_FEEDBACK;
			else if(s.equals("error"))
				mode |= ERROR_FEEDBACK;
			else if(s.equals("color"))
				mode |= COLOR_FEEDBACK;
			else if(s.equals("summary"))
				mode |= SUMMARY_FEEDBACK;
		}
		return mode;
	}
	
	/**
	 * get all messages that this module supports
	 * @return
	 */
	public Message [] getSupportedMessages(){
		return new Message [0];
	}
	
	
	public void sync(FeedbackModule tm) {
	}
	
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	
	/**
	 * do auto hint through
	 */
	private void doAutoHint(String mode){
		// modify properties
		HINT_DELAY_SLOW = Config.getFloatProperty(this,"auto.hint.slow.hint.delay");
		HINT_DELAY_FAST = Config.getFloatProperty(this,"auto.hint.fast.hint.delay");
		
		
		// don't start another one
		if(hintThroughThread != null && hintThroughThread.isAlive())
			return;
				
		// remember old mode
		final int oldMode = getFeedbackMode();
		setFeedackMode(HINT_FEEDBACK | ERROR_FEEDBACK | COLOR_FEEDBACK);
		
		// make sure it is in instant feedback
		if(!isMode(HINT_FEEDBACK|ERROR_FEEDBACK)){
			JOptionPane.showMessageDialog(Config.getMainFrame(),
					"Auto Hint Through feature only works in immediate tutor","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		final String autoHintMode = ""+mode;
		
		// start new one
		hintThroughThread = new Thread(){
			public void run(){
				// get the first tutor in a sequence
				List<Tutor> tutors = ITS.getInstance().getTutors();
				if(tutors.isEmpty())
					return;
				
				// now we have a tutor
				Tutor tutor = tutors.get(0);
				FeedbackModule feedback = tutor.getFeedbackModule();
				ReasoningModule reasoning = tutor.getReasoningModule();
				InterfaceModule intmodule = tutor.getInterfaceModule();
				PresentationModule viewer = tutor.getPresentationModule();
				boolean interactiveStatus = tutor.isInteractive();
				tutor.setInteractive(false);
				
				// check modes
				boolean instant = autoHintMode.contains("instant");
				autoHint = true;
				ConceptEntry previous = null;
				
				// now iterate through hints
				while(!reasoning.isSolved()){
					
					hintThroughResponse = false;
					
					// ask for hint
					if(!instant){
						feedback.requestHint();
						feedback.requestHint();
						
						// wait
						while(!hintThroughResponse)
							UIHelper.sleep(50);
					}
					
					// get next step
					ConceptEntry next = reasoning.getNextConcept();
					// we r done then
					if(next == null)
						break;
					
					// next concept is just like previous one, what gives???
					if(previous != null && next.equals(previous))
						continue;
					previous = next;
					
					
					// makr this as auto
					next.setAuto(true);
					
					// go over hint levels
					if(!instant){
						int n = feedback.getLevelCount();
						for(int i=1;i<n;i++){
							// request level
							feedback.requestLevel(i);
							
							// now sleep after it
							boolean doSleep = false;
							
							// decide on which hint we want to sleep
							if(TYPE_FINDING.equals(next.getType()))
								doSleep = n - 2 == i;
							else
								doSleep = n - 1 == i;
							
							// sleep for some levels
							UIHelper.sleep4sec(doSleep?HINT_DELAY_SLOW:HINT_DELAY_FAST);
						}
					}
					
					
					// add location if appropriate
					if(TYPE_FINDING.equals(next.getType())){
						SlideEntry img = caseEntry.getOpenSlide();
						Point p = OntologyHelper.findLocationMarker(caseEntry,next,(img != null)?img.getName():null); 
						if(p != null){
							viewer.setIdentifiedFeature(new Rectangle(p.x,p.y,10,10));
							next.setInput(viewer.getIdentifiedFeature());
						}
					}
					
					
					// now assert the next concept
					hintThroughResponse = false;
					intmodule.addConceptEntry(next);
					
					// wait for response from the add
					while(!hintThroughResponse)
						UIHelper.sleep(50);
					
					// additional wait???
					if(!instant)
						UIHelper.sleep4sec(1);
				}
				
				// ask last hint
				if(!instant)
					feedback.requestHint();
				tutor.setInteractive(interactiveStatus);
				setFeedackMode(oldMode);
				autoHint = false;
			}
		};
		hintThroughThread.start();
	}
}
