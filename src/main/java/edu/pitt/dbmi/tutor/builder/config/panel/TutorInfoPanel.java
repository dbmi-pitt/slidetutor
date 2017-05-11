package edu.pitt.dbmi.tutor.builder.config.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import static java.awt.GridBagConstraints.*;
import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.builder.config.WizardPanel;
import edu.pitt.dbmi.tutor.util.ConfigProperties;


public class TutorInfoPanel implements WizardPanel {
	private ConfigProperties properties;
	private JPanel component;
	
	private JTextField nameText,versionText, conditionText;//, manualText;
	private JTextArea descriptionText;
	private JCheckBox openCaseCheck,useProtocolCheck,useLoginCheck;
	private boolean visited;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private KeyListener keyListener = new KeyAdapter() {
		public void keyTyped(KeyEvent e) {
			pcs.firePropertyChange(new PropertyChangeEvent(TutorInfoPanel.this,"CHANGE", null, null));
		}
	};
	private ActionListener changeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			pcs.firePropertyChange(new PropertyChangeEvent(TutorInfoPanel.this,"CHANGE", null, null));			
		}
	};
	
	
	/**
	 * add property change listener
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	
	/**
	 * remove property change listener
	 * @param l
	 */
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	
	
	public boolean isVisited() {
		return visited;
	}
	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	
	public Component getComponent() {
		if(component == null){
			JPanel panel = new JPanel();
			panel.setOpaque(false);
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1.0,1.0,CENTER,BOTH,new Insets(0,5,0,5),0,0);
			GridBagLayout l = new GridBagLayout();
			panel.setLayout(l);
			l.setConstraints(panel,c);
			
			// init components
			nameText = new JTextField();
			versionText = new JTextField();
			conditionText = new JTextField();
			//manualText = new JTextField();
			descriptionText = new JTextArea();
			descriptionText.setLineWrap(true);
			descriptionText.setWrapStyleWord(true);
			openCaseCheck = new JCheckBox("Enable users to select any available cases and domains");
			useProtocolCheck = new JCheckBox("Save all user data in a protocol");
			useLoginCheck = new JCheckBox("Require users to login in order to get into the system");
			openCaseCheck.setOpaque(false);
			useProtocolCheck.setOpaque(false);
			useLoginCheck.setOpaque(false);
			
			nameText.addKeyListener(keyListener);
			versionText.addKeyListener(keyListener);
			descriptionText.addKeyListener(keyListener);
			conditionText.addKeyListener(keyListener);
			openCaseCheck.addActionListener(changeListener);
			useProtocolCheck.addActionListener(changeListener);
			useLoginCheck.addActionListener(changeListener);
			
			
			// setup help
			nameText.setToolTipText("A descriptive title of your tutoring system.");
			versionText.setToolTipText("A version number of your tutoring system.");
			descriptionText.setToolTipText("A detailed description of your tutoring system.");
			conditionText.setToolTipText("<html>Some tutoring system configurations may be<br>" +
										"associated with a condition in an experiment.<br>" +
										"Having a condition is very useful when user data<br>" +
										"is being saved in the protocol.");
			//manualText.setToolTipText("<html>An optional URL to the user manual of your customized system configuration.<br>" +
			//							"If you do not provide a value, the system will compile<br>" +
			//							" a user manual from individual system components. ");
			useLoginCheck.setToolTipText("<html>Restricts tutoring system use to a set of predefined users.<br>" +
										"This option also allows educators to tell users apart if user data is collected.");
			openCaseCheck.setToolTipText("<html>Enables users to open arbitrary cases from within the system.<br>" +
										"Since case selection should be handled by Pedagogic Module,<br>" +
										"this option should be disabled in most cases.");
			
			// layout components
			panel.add(new JLabel("Name"),c); c.gridx ++;
			panel.add(new JLabel("Description"),c); c.gridx = 0; c.gridy ++;
			panel.add(nameText,c); c.gridx ++; c.gridheight = 5;
			panel.add( new JScrollPane(descriptionText),c); c.gridx = 0; c.gridheight = 1; c.gridy ++;
			panel.add(new JLabel("Condition"),c); c.gridy ++;
			panel.add(conditionText,c); c.gridy ++;
			panel.add(new JLabel("Version"),c); c.gridy ++;
			panel.add(versionText,c); c.gridy ++; c.gridwidth = 2;
			//panel.add(new JLabel("User Manual URL"),c); c.gridy ++;
			//component.add(manualText,c); c.gridy ++; c.gridwidth = 2;
			panel.add(useLoginCheck,c); c.gridy ++;
			panel.add(useProtocolCheck,c); c.gridy ++;
			panel.add(openCaseCheck,c); c.gridy ++;
			panel.add(Box.createVerticalStrut(400),c);
			
			component = new JPanel();
			component.setLayout(new BorderLayout());
			component.setBackground(Color.white);
			
			component.add(Names.createHeader(this),BorderLayout.NORTH);
			component.add(panel,BorderLayout.CENTER);
			
			// load defaults
			load();
			
		}
		return component;
	}

	
	public String getDescription() {
		return "Enter general information about your tutoring system such as name, description, and version.";
	}

	public String getName() {
		return "Tutor Information";
	}
	
	public String getShortName() {
		return "Description";
	}

	public Properties getProperties() {
		if(properties == null){
			properties = new ConfigProperties();
		
			// initialize properties that this 
			properties.put("file.manager.server.url","http://slidetutor.upmc.edu/domainbuilder/FileManagerServlet");
			properties.put("tutor.list","st");
			properties.put("tutor.name","Slide Tutor ITS");
			properties.put("tutor.condition","Deployment");
			properties.put("tutor.st.name","Diagnostic Tutor");
			properties.put("tutor.st.description","Diagnostic tutor that uses virtual slides");
			properties.put("tutor.st.version","1.0");
			properties.put("tutor.login.enabled","false");
			properties.put("tutor.protocol.enabled","false");
			properties.put("tutor.open.case.enabled","false");
		}
		return properties;
	}

	/**
	 * load values
	 */
	public void load(){
		setVisited(true);
		if(nameText != null){
			nameText.setText(getProperties().getProperty("tutor.name"));
			conditionText.setText(getProperties().getProperty("tutor.condition"));
			versionText.setText(getProperties().getProperty("tutor.st.version"));
			descriptionText.setText(getProperties().getProperty("tutor.st.description"));
			descriptionText.setCaretPosition(0);
			//manualText.setText(getProperties().getProperty("tutor.user.manual"));
			useLoginCheck.setSelected(Boolean.parseBoolean(getProperties().getProperty("tutor.login.enabled")));
			useProtocolCheck.setSelected(Boolean.parseBoolean(getProperties().getProperty("tutor.protocol.enabled")));
			openCaseCheck.setSelected(Boolean.parseBoolean(getProperties().getProperty("tutor.open.case.enabled")));
		}
	}
	
	
	
	public void setProperties(Properties map) {
		for(Object key: getProperties().keySet()){
			if(map.containsKey(key))
				getProperties().put(key,map.get(key));
		}
	}

	public void apply() {
		if(nameText != null){
			getProperties().put("tutor.name",nameText.getText());
			getProperties().put("tutor.condition",conditionText.getText());
			getProperties().put("tutor.st.version",versionText.getText());
			getProperties().put("tutor.st.description",descriptionText.getText());
			//getProperties().put("tutor.user.manual",manualText.getText());
			getProperties().put("tutor.login.enabled",""+useLoginCheck.isSelected());
			getProperties().put("tutor.protocol.enabled",""+useProtocolCheck.isSelected());
			getProperties().put("tutor.open.case.enabled",""+openCaseCheck.isSelected());
		}
	}

	public void revert() {
		properties = null;
		load();
	}
}
