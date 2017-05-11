package edu.pitt.dbmi.tutor.modules.feedback;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.*;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.LinkConceptEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioSet;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.FeedbackModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.StudentModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.modules.interfaces.ArcNodeInterface;
import edu.pitt.dbmi.tutor.modules.interfaces.ColorBookInterface;
import edu.pitt.dbmi.tutor.modules.interfaces.NodeConcept;
import edu.pitt.dbmi.tutor.modules.interfaces.QuestionInterface;
import edu.pitt.dbmi.tutor.modules.interfaces.ReportInterface;
import edu.pitt.dbmi.tutor.modules.presentation.NoPresentation;
import edu.pitt.dbmi.tutor.modules.presentation.SimpleViewerPanel;
import edu.pitt.dbmi.tutor.modules.reasoning.SimplePrognosticReasoner;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import static java.awt.GridBagConstraints.*;

/**
 * manage instructions displayed to a user
 * @author tseytlin
 *
 */
public class InstructionManager implements FeedbackModule, ActionListener {
	private Properties defaultConfig;
	private Component component;
	private Tutor tutor;
	private ExpertModule expertModule;
	private StudentModule studentModule;
	private Properties instructions;
	private JEditorPane messagePane;
	private JButton done;
	private String type, mode;
	private CaseEntry caseEntry;
	private boolean colorMode;
	

	public Component getComponent() {
		if(component == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.setMinimumSize(new Dimension(200,200));
			
			// create message window
			messagePane = new UIHelper.HTMLPanel();
			messagePane.setEditable(false);
			messagePane.addHyperlinkListener( new HyperlinkListener(){
	            public void hyperlinkUpdate(HyperlinkEvent e){
	               if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	                    //String key = e.getDescription();
	                    //NOOP???
	                }
	            }
	        });
			JScrollPane scroll = new JScrollPane(messagePane);
			scroll.setBorder(new LineBorder(Color.green,2));
			panel.add(scroll,BorderLayout.CENTER);
			JPanel p = new JPanel();
			p.setBackground(Color.white);
			p.setLayout(new FlowLayout());
			done =  UIHelper.createButton("Finish Question","Finish this question and proceed to the next one",
					UIHelper.getIcon(this,"icon.finish.question"),-1, true, this);
			p.add(done,BorderLayout.CENTER);
			panel.add(p,BorderLayout.SOUTH);
			component = panel;	
		}
		return component;
	}
	
	
	
	/**
	 * add new concept
	 */
	private void doDone(){
		// check if you are done
		if(isDone()){
			if(QUESTION_TYPE_COLOR.equals(type) && !colorMode){
				colorMode = true;
				doColorQuestion(caseEntry);
			}else{
				// prompt questions if necessary
				if(!TextHelper.isEmpty(mode)){
					if(QUESTION_MODE_FOK.equals(mode)){
						promptFOK();
					}
				}
				
				// disable done button
				setEnabled(false);
				getTutor().getInterfaceModule().setEnabled(false);
				
				// we are done
				InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_BUTTON,done.getText(),ACTION_SELECTED);
				ClientEvent ce = ClientEvent.createClientEvent(this,TYPE_DONE,getClass().getSimpleName(),ACTION_REQUEST);
				ie.setClientEvent(ce);
				Communicator.getInstance().sendMessage(ie);
				Communicator.getInstance().sendMessage(ce);
			}
		}else{
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_BUTTON,done.getText(),ACTION_SELECTED);
			Communicator.getInstance().sendMessage(ie);
			JOptionPane.showMessageDialog(Config.getMainFrame(),Config.getProperty(this,"warning.not.done"),"Warning",JOptionPane.WARNING_MESSAGE);
		}
	}


	/**
	 * is this question done
	 * @return
	 */
	private boolean isDone(){
		InterfaceModule m = getTutor().getInterfaceModule();
		List<ConceptEntry> concepts = m.getConceptEntries();
		String answer = "";
		if(m instanceof QuestionInterface){
			answer = ((QuestionInterface)m).getAnswer();
		}else if(m instanceof ReportInterface){
			answer = ((ReportInterface)m).getReport();
		}
		
		if(QUESTION_TYPE_LINK.equals(type)){
			for(ConceptEntry e: concepts){
				if(e instanceof LinkConceptEntry)
					return true;
			}
			return false;
		}else if(QUESTION_TYPE_INFER.equals(type)){
			for(ConceptEntry e: concepts){
				if(TYPE_DIAGNOSIS.equals(e.getType()))
					return true;
			}
			return false;
		}else if(QUESTION_TYPE_REPORT.equals(type)){
			if(answer.length() > 0){
				((ReportInterface)m).notifyReport();
				return true;
			}
			return false;
		}else if(QUESTION_TYPE_IDENTIFY.equals(type)){
			return !concepts.isEmpty();
		}else if(QUESTION_TYPE_DIFFERENTIATE.equals(type)){
			return !concepts.isEmpty();
		}else if(QUESTION_TYPE_FREE_TEXT.equals(type)){
			if(answer.length() > 0){
				((QuestionInterface)m).notifyAnswer();
				return true;
			}
			return false;
		}else if(QUESTION_TYPE_POINT_OUT.equals(type)){
			return answer.length() > 0;
		}else if(QUESTION_TYPE_MULTIPLE_CHOICE.equals(type)){
			if(answer.length() > 0){
				((QuestionInterface)m).notifyAnswer();
				return true;
			}
			return false;
		}else if(QUESTION_TYPE_COLOR.equals(type) && colorMode){
			return ((ColorBookInterface)m).getUncoloredConcepts().isEmpty();
		}else { 
			//if(QUESTION_TYPE_SOLVE.equals(type))
			if(m instanceof ArcNodeInterface)
				return ((ArcNodeInterface)m).isDone();
		}
		// default is done
		return true;
	}
	
	
	public void reconfigure(){
		//TODO:
	}
	
	public int getFeedbackMode() {
		return 0;
	}

	public int getLevelCount() {
		return 0;
	}

	public void load() {
		setup();
	}
	
	/**
	 * init help manager
	 */
	private void setup(){
		//NOOP
	}
	

	public void requestHint() {
		//NOOP
	}

	public void requestLevel(int offset) {
		//NOOP

	}

	public void setCaseEntry(CaseEntry problem) {
		// this is where magic happens
		type = ""+problem.getProperties().getProperty("question.type");
		mode = null;
		done.setEnabled(true);
		
		// check for modifications
		int i = type.indexOf("+");
		if(i > -1){
			mode = type.substring(i+1);
			type = type.substring(0,i);
		}
		
		colorMode = false;
		caseEntry = problem;
		
		if(QUESTION_TYPE_LINK.equals(type)){
			doLinkQuestion(problem);
		}else if(QUESTION_TYPE_INFER.equals(type)){
			doInferQuestion(problem);
		}else if(QUESTION_TYPE_REPORT.equals(type)){
			doReportQuestion(problem);
		}else if(QUESTION_TYPE_IDENTIFY.equals(type)){
			doIdentifyQuestion(problem);
		}else if(QUESTION_TYPE_DIFFERENTIATE.equals(type)){
			doDifferentiateQuestion(problem);
		}else if(QUESTION_TYPE_FREE_TEXT.equals(type)){
			doFreeTextQuestion(problem);
		}else if(QUESTION_TYPE_POINT_OUT.equals(type)){
			doPointOutQuestion(problem);
		}else if(QUESTION_TYPE_MULTIPLE_CHOICE.equals(type)){
			doMultipleChoiceQuestion(problem);
		}else{
			// QUESTION_TYPE_SOLVE
			doSolveQuestion(problem);
		}
	}
	
	/**
	 * show question
	 * @param type
	 * @param n
	 * @param t
	 */
	private void showQuestion(String type){
		showQuestionText(Config.getProperty(this,"question."+type));
	}
	
	/**
	 * show question
	 * @param type
	 * @param n
	 * @param t
	 */
	private void showQuestionText(String text){
		int n = ITS.getInstance().getPedagogicModule().getCaseOffset()+1;
		messagePane.setText("<center><h2>Question "+n+"</h2></center><hr>"+text);
	}
	
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doLinkQuestion(CaseEntry pb){
		// display text
		final CaseEntry problem = pb;
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|InterfaceModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|PresentationModule|0.5|.5\n"+
						ArcNodeInterface.class.getSimpleName()+".component.orientation = vertical\n" +
						ArcNodeInterface.class.getSimpleName()+".toolbar.orientation = vertical";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,ArcNodeInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,NoPresentation.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
	
		showQuestion(QUESTION_TYPE_LINK);
		
		// init content
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
				String s = "support.link,Remove Link";
				doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_LOCK_INTERFACE_TO,s));
				
				// add findings
				for(ConceptEntry e: problem.getConcepts(DIAGNOSTIC_FEATURES).getValues()){
					e = e.clone();
					e.setAuto(true);
					doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT,e.getName()),e);
					doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_COLOR_CONCEPT,RESPONSE_CONFIRM),e);
				}
				// add hypothesis
				for(ConceptEntry e: problem.getConcepts(DIAGNOSES).getValues()){
					e = e.clone();
					e.setAuto(true);
					e.setType(TYPE_HYPOTHESIS);
					doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT,e.getName()),e);
					doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_COLOR_CONCEPT,RESPONSE_CONFIRM),e);
				}
			}
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doInferQuestion(CaseEntry pb){
		// display text
		final CaseEntry problem = pb;
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
				
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|InterfaceModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|PresentationModule|0.5|.5\n"+
						ArcNodeInterface.class.getSimpleName()+".component.orientation = vertical\n" +
						ArcNodeInterface.class.getSimpleName()+".behavior.diagnosis.mode = all\n" +
						ArcNodeInterface.class.getSimpleName()+".toolbar.orientation = vertical";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,ArcNodeInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,NoPresentation.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestion(QUESTION_TYPE_INFER);

		
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
				
				// disable buttons
				//String s = "hypothesis,diagnosis,support.link,refute.link,Remove Link, Remove Hypothesis, Remove Diagnosis";
				String s = "diagnosis, Remove Diagnosis";
				doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_LOCK_INTERFACE_TO,s));
								
				// add findings
				for(ConceptEntry e: problem.getConcepts(DIAGNOSTIC_FEATURES).getValues()){
					e = e.clone();
					e.setAuto(true);
					doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_ADD_CONCEPT,e.getName()),e);
					doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_COLOR_CONCEPT,RESPONSE_CONFIRM),e);
				}
			}
		});
		timer.setRepeats(false);
		timer.start();
	
		
	}
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doSolveQuestion(CaseEntry pb){
		// display text
		//final CaseEntry problem = pb;
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = H|split|InterfaceModule|1|.55\n" +
						"tutor."+getTutor().getId()+".layout.split = V|FeedbackModule|PresentationModule|0\n" +
					ArcNodeInterface.class.getSimpleName()+".component.orientation = vertical\n" +
					ArcNodeInterface.class.getSimpleName()+".toolbar.orientation = horizontal";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,ArcNodeInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestion(QUESTION_TYPE_SOLVE);	
		
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
				String s = "finding,absent.finding,hypothesis,diagnosis,support.link,refute.link,delete,specify,clinical.info";
				doAction(new Action(ArcNodeInterface.class.getSimpleName(),POINTER_ACTION_LOCK_INTERFACE_TO,s));
			}
		});
		timer.setRepeats(false);
		timer.start();
		
	}
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doColorQuestion(CaseEntry pb){
	
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = H|split|InterfaceModule|1|.55\n" +
						"tutor."+getTutor().getId()+".layout.split = V|FeedbackModule|PresentationModule|0\n" +
					ArcNodeInterface.class.getSimpleName()+".component.orientation = vertical\n" +
					ArcNodeInterface.class.getSimpleName()+".toolbar.orientation = horizontal";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,ColorBookInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestion(QUESTION_TYPE_COLOR);	
		
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
				String s = "sure,unsure,error";
				doAction(new Action(ColorBookInterface.class.getSimpleName(),POINTER_ACTION_LOCK_INTERFACE_TO,s));
			}
		});
		timer.setRepeats(false);
		timer.start();
		
	}
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doReportQuestion(CaseEntry pb){
		// display text
		final CaseEntry problem = pb;
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = H|split|InterfaceModule|1|.55\n" +
						"tutor."+getTutor().getId()+".layout.split = V|FeedbackModule|PresentationModule|0";
		doAction(new Action(Tutor.class.getSimpleName(),
				POINTER_ACTION_SWITCH_TUTOR_MODULE,ReportInterface.class.getName()),null);
		doAction(new Action(Tutor.class.getSimpleName(),
				POINTER_ACTION_SWITCH_TUTOR_MODULE,SimplePrognosticReasoner.class.getName()),null);
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestion(QUESTION_TYPE_REPORT);
		
		// switch panels
		/*
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
			
			}
		});
		timer.setRepeats(false);
		timer.start();
		*/
	}
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doFreeTextQuestion(CaseEntry problem){
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		String q = problem.getProperties().getProperty("question");
		
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|PresentationModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|InterfaceModule|0.5|.5";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,QuestionInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestionText(q);
	}
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doPointOutQuestion(CaseEntry problem){
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		String q = problem.getProperties().getProperty("question");
		
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|PresentationModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|InterfaceModule|0.5|.5";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,QuestionInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
	
		showQuestionText(q);
	}
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doMultipleChoiceQuestion(CaseEntry problem){
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		String q = problem.getProperties().getProperty("question");
	
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|PresentationModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|InterfaceModule|0.5|.5";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,QuestionInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestionText(q);
	}
	
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doIdentifyQuestion(CaseEntry problem){
		//String n = problem.getProperties().getProperty("number");
		//String t = problem.getProperties().getProperty("total");
		
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|PresentationModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|InterfaceModule|0.5|.5";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,QuestionInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,SimpleViewerPanel.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestion(QUESTION_TYPE_IDENTIFY);
		
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
				// display annotation
				doAction(new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL));
				doAction(new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"FALSE"));
			}
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	
	/**
	 * do link question
	 * @param problem
	 */
	private void doDifferentiateQuestion(CaseEntry problem){
	
		String text = Config.getProperty(this,"question."+type);
		// insert diagnoses into question text
		List<ConceptEntry> dxs = problem.getConcepts(DIAGNOSES).getValues();
		if(dxs.size() > 0)
			text = text.replaceAll("<DIAGNOSIS1>","<b>"+dxs.get(0).getText()+"</b>");
		if(dxs.size() > 1)
			text = text.replaceAll("<DIAGNOSIS2>","<b>"+dxs.get(1).getText()+"</b>");
		
		
		// switch layout
		String layout = "tutor."+getTutor().getId()+".layout.main  = V|split|PresentationModule|0|.3\n" +
						"tutor."+getTutor().getId()+".layout.split = H|FeedbackModule|InterfaceModule|0.5|.5";
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,QuestionInterface.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SWITCH_TUTOR_MODULE,NoPresentation.class.getName()));
		doAction(new Action(Tutor.class.getSimpleName(),POINTER_ACTION_DO_LAYOUT,layout));
		
		showQuestionText(text);
	
		Timer timer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent evt){
				Container c = (Container) getTutor().getPresentationModule().getComponent();
				getTutor().getPresentationModule().reset();
				c.add(((QuestionInterface) getTutor().getInterfaceModule()).getDifferentiateDiagram(),BorderLayout.CENTER);
				c.validate();
				c.repaint();
			}
		});
		timer.setRepeats(false);
		timer.start();
		
	}
	
	/**
	 * execute a single action
	 * @param act
	 */
	private void doAction(Action a){
		doAction(a,null);
	}
	
	/**
	 * execute a single action
	 * @param act
	 */
	private void doAction(Action a, ConceptEntry conceptEntry){
		// reset action concept
		a.setConceptEntry(conceptEntry);
		Communicator.getInstance().resolveAction(a);
		a.run();
	}
	
	
	
	

	/**
	 * load misc meta-data s.a. concept trees and
	 * case information, from given expert module
	 * @param ExpertModule module containing info
	 */
	public void setExpertModule(ExpertModule module){
		expertModule = module;
	}
	
	public void setFeedackMode(int mode) {}

	public void setStudentModule(StudentModule module) {
		studentModule = module;

	}

	public void sync(FeedbackModule tm) {}


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
		return false;
	}

	public void setEnabled(boolean b) {
		if(component != null){
			component.setEnabled(b);
			done.setEnabled(b);
		}
	}

	public void setInteractive(boolean b) {}

	public void setTutor(Tutor t) {
		tutor = t;

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "The Instruction Manager is used for creating tests. It presents questions/instructions to the user, as well as modifies " +
				"the tutor layout based on different question types. No feedback is provided to users about their tutor actions.";
	}

	public String getName() {
		return "Instruction Module";
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
		// TODO Auto-generated method stub

	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public void resolveAction(Action action) {
		//NOOP

	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand().toLowerCase();
		if("finish question".equals(cmd)){
			doDone();
		}
	}
	public ScenarioSet getScenarioSet() {
		return null;
	}
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	/**
	 * prompt for Feeling of Knowing Measure
	 */
	public void promptFOK() {
		final Map<ConceptEntry,ButtonGroup> fokMeasures = new LinkedHashMap<ConceptEntry, ButtonGroup>();
		final JButton ok = new JButton("OK");
		final List<ConceptEntry> concepts = new ArrayList<ConceptEntry>();
		
		// fill up concept entries
		for(ConceptEntry e : getTutor().getInterfaceModule().getConceptEntries()){
			if(!e.isAuto())
				concepts.add(e);
		}
		
		// alpha sort
		Collections.sort(concepts,new Comparator<ConceptEntry>(){
			public int compare(ConceptEntry o1, ConceptEntry o2) {
				return o1.getText().compareTo(o2.getText());
			}
		});
		
		
		GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,WEST,NONE,new Insets(2,2,2,2),0,0);
		GridBagLayout gb = new GridBagLayout();
		final JPanel cp = new JPanel();
		gb.setConstraints(cp,c);
		cp.setLayout(gb);
		cp.setBackground(Color.white);
		cp.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED),new EmptyBorder(5,5,5,5)));
		
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// count answers to enable OK
				int answers = 0;
				// reset FOK 
				for(ConceptEntry entry: fokMeasures.keySet()){
					if(fokMeasures.get(entry).getSelection() != null){
						answers ++;
						entry.setConceptFOKString(fokMeasures.get(entry).getSelection().getActionCommand());
					}
				}
			
				if(answers >= concepts.size())
					ok.setEnabled(true);
				
				cp.validate();
				cp.repaint();
			}
		};
		
		// now prompt
		int nodeHeight = -1,nodeWidth = -1;
		for(ConceptEntry entry : concepts){
			ButtonGroup g = new ButtonGroup();
			fokMeasures.put(entry,g);
			
			JRadioButton sure = new JRadioButton("sure");
			sure.setActionCommand("sure");
			sure.addActionListener(l);
			sure.setOpaque(false);
			g.add(sure);
			
			JRadioButton unsure = new JRadioButton("unsure");
			unsure.setActionCommand("unsure");
			unsure.addActionListener(l);
			unsure.setOpaque(false);
			g.add(unsure);
		
			
			final NodeConcept node = NodeConcept.createNodeConcept(entry,getTutor().getInterfaceModule());
			node.setMetaColoring(true);
			
			if(nodeHeight < 0)
				nodeHeight = node.getBounds().height+9;
			if(node.getBounds().width+9 > nodeWidth)
				nodeWidth = node.getBounds().width+9;
			
			// add components
			if(node != null){
				cp.add(new JLabel(new Icon(){
					public void paintIcon(Component c, Graphics g, int x, int y) {
						node.draw(g);
					}
					public int getIconWidth() {
						return node.getBounds().width+5;
					}
					public int getIconHeight() {
						return node.getBounds().height+5;
					}
				}),c);
			}else{
				cp.add(new JLabel(entry.getText()),c);
			}
			c.gridx++;
			cp.add(sure,c);
			c.gridx++;
			cp.add(unsure,c);
			c.gridx = 0; c.gridy++;
		}
		// limit the size
		JScrollPane scroll = new JScrollPane(cp);
		if(concepts.size() > 10){
			scroll.setPreferredSize(new Dimension(nodeWidth +200,nodeHeight * 10 +10));
		}
		
		int width = cp.getPreferredSize().width;
		String instructions = 
			"<html><table width="+width+"><tr><td>" +
			"Please indicate whether you are sure or unsure about each item by making an appropriate selection." +
			"</td></tr></table></html>";
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(new EmptyBorder(10,10,10,10));
		p.add(new JLabel(instructions),BorderLayout.NORTH);
		p.add(scroll,BorderLayout.CENTER);
		
			
		ok.setEnabled(false);
		JDialog d = UIHelper.createDialog(p, ok);
		d.setModal(true);
		d.setTitle("");
		d.setVisible(true);
		
		// notify answers
		for(ConceptEntry entry: fokMeasures.keySet()){
			Communicator.getInstance().sendMessage(entry.getClientEvent(this,Constants.ACTION_SELF_CHECK));
		}
	}

	
}
