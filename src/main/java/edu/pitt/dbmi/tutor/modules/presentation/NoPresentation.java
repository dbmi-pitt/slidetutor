package edu.pitt.dbmi.tutor.modules.presentation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Shape;
import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.util.Config;


/**
 * doesn't do anything, just a blank screen
 * @author tseytlin
 *
 */
public class NoPresentation implements PresentationModule {
	private Tutor tutor;
	private Component component;
	private Properties defaults;
	
	public Object getIdentifiedFeature() {
		return null;
	}
	public boolean isFeatureIdentified() {
		return false;
	}
	public void removeIdentifiedFeature(Object obj) {}

	public void setCaseEntry(CaseEntry problem) {
		reset();
	}

	public void setExpertModule(ExpertModule module) {}

	public void setFeatureMarker(String type) {}

	public void startFeatureIdentification() {}

	public void stopFeatureIdentification() {}

	public void sync(PresentationModule tm) {}

	public Component getComponent() {
		if(component == null){
			JPanel panel = new JPanel();
			panel.setBackground(Color.white);
			panel.setLayout(new BorderLayout());
			panel.add(new JLabel(Config.getIconProperty("icon.general.big.tutor.logo")),BorderLayout.CENTER);
			component = panel;
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
	
	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Tutor getTutor() {
		return tutor;
	}

	public boolean isEnabled() {
		return false;
	}

	public boolean isInteractive() {
		return false;
	}

	public void reconfigure() {}

	public void setEnabled(boolean b) {}

	public void setInteractive(boolean b) {}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public void dispose() {	}
	
	public Properties getDefaultConfiguration() {
		if(defaults == null)
			defaults = Config.getDefaultConfiguration(getClass());
		return defaults;
	}

	public String getDescription() {
		return "This is a placeholder presentation layer when there is nothing to present. It is normally used " +
				"in conjunction with the <b>Instruction Module</b>.";
	}

	public String getName() {
		return "No Presentation";
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

	public void receiveMessage(Message msg) {}

	public void reset() {
		if(component != null && component instanceof JPanel){
			((JPanel)component).removeAll();
			component.validate();
		}
	}

	public void resolveAction(Action action) {}
	public void setIdentifiedFeature(Shape shape) {
		// TODO Auto-generated method stub
		
	}

}
