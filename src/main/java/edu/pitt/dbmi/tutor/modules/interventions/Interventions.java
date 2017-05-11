package edu.pitt.dbmi.tutor.modules.interventions;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.InteractiveTutorModule;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class Interventions implements InteractiveTutorModule {
	private Tutor tutor;
	private Properties defaultConfig;
	private JButton solveCaseButton;
	
	public void load(){	}
	public void dispose() {
		reset();
	}
	
	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "This module provides a set of hard-coded interventions that can be accessed via " +
				"the Behavioral Module to augment the functionality of the tutor.";
	}

	public String getName() {
		return "Interventions";
	}


	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	public void receiveMessage(Message msg) {
		if(TYPE_BUTTON.equals(msg.getType()) && "Solve Case".equals(msg.getLabel())){
			// this is for playback
			if(solveCaseButton != null && solveCaseButton.isShowing())
				solveCaseButton.doClick();
		}
	}
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public void resolveAction(Action action) {
		final Action act = action;
		Operation oper = null;
				
		// figure out which operations to set
		if(POINTER_ACTION_SOLVE_CASE_BUTTON.equalsIgnoreCase(action.getAction())){
			oper = new Operation(){
				public void run() {
					doSolveCaseButton(act.getInput());
				}
				public void undo(){
				}
			};
		}
		// set operations
		action.setOperation(oper);
	}
	
	public Action[] getSupportedActions() {
		return new Action [] {
				new Action(getClass().getSimpleName(),POINTER_ACTION_SOLVE_CASE_BUTTON,"message",
						"Display a <font color=green>Solve Case</font> button that obscures the reasoning interface. " +
						"When user clicks it, he or she is prompted with a popup dialog to a user with input message."),
				new Action(getClass().getSimpleName(),POINTER_ACTION_SOLVE_CASE_BUTTON,"question [prompt|yes;no]",
						"Display a <font color=green>Solve Case</font> button that obscures the reasoning interface. " +
						"When user clicks it, he or she is a popup dialog to a user with input question and yes/no or ok/cancel options."),
				new Action(getClass().getSimpleName(),POINTER_ACTION_SOLVE_CASE_BUTTON,"question [choice|opt1;opt2]",
						"Display a <font color=green>Solve Case</font> button that obscures the reasoning interface. " +
						"When user clicks it, he or she is a popup dialog to a user with a multiple choice question."),
				new Action(getClass().getSimpleName(),POINTER_ACTION_SOLVE_CASE_BUTTON,"question [text]",
						"Display a <font color=green>Solve Case</font> button that obscures the reasoning interface. " +
						"When user clicks it, he or she is a popup dialog to a user with input question and a text box for free text input."),
				new Action(getClass().getSimpleName(),POINTER_ACTION_SOLVE_CASE_BUTTON,"question [scale|min;max;lbl1;lbl2]",
						"Display a <font color=green>Solve Case</font> button that obscures the reasoning interface. " +
						"When user clicks it, he or she is a popup dialog to a user with the question and a Likert scale with optional labels for minimum and maximum.")};
		
	}


	/**
	 * overaly solve case button over the interface
	 */
	private void doSolveCaseButton(String in) {
		final String input = in;
		final JPanel p = new JPanel();
		
		// solve case button
		JButton button = new JButton("Solve Case");
		solveCaseButton = button;
		button.setBorder(new BevelBorder(BevelBorder.RAISED));
		button.setIcon(Config.getIconProperty(this,"icon.general.solve"));
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// notify interface event
				Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(Interventions.this,TYPE_BUTTON,"Solve Case",ACTION_SELECTED));
				
				// if there is input prompt user
				if(!TextHelper.isEmpty(input)){
					Action a = new Action(Tutor.class.getSimpleName(),POINTER_ACTION_PROMPT_USER,input);
					Communicator.getInstance().resolveAction(a);
					a.run();
				}
			
				// put everything back the way it was
				InterfaceModule im = getTutor().getInterfaceModule();
				JPanel c = (JPanel) im.getComponent();
				c.remove(p);
				c.revalidate();
				c.repaint();
			}
		});
		button.setActionCommand("solve");
		button.setFont(button.getFont().deriveFont(Font.BOLD,16f));
		button.setPreferredSize(new Dimension(200,200));
		
		// get interface module
		InterfaceModule im = getTutor().getInterfaceModule();
		JPanel c = (JPanel) im.getComponent();
		
		// consult panel
		p.setLayout(new GridBagLayout());
		p.add(button,new GridBagConstraints());
		p.setOpaque(true);
		p.setBackground(new Color(240,240,220));
		
		if(c.getLayout() instanceof CardLayout){
			c.add(p,"solve");
			c.revalidate();
			c.repaint();
			((CardLayout)c.getLayout()).show(c,"solve");
		}else if(c.getLayout() instanceof OverlayLayout){
			c.add(p,0);
			c.revalidate();
			c.repaint();
		}
	}
	
	public Component getComponent() {
		return null;
	}
	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}
	public Tutor getTutor() {
		return tutor;
	}
	public boolean isEnabled() {
		return true;
	}
	public boolean isInteractive() {
		return true;
	}
	public void reconfigure() {}
	public void setEnabled(boolean b) {}
	public void setInteractive(boolean b) {}
	public void setTutor(Tutor t) {
		tutor = t;
	}
}
