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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import edu.pitt.dbmi.tutor.builder.config.ConfigurationBuilder;
import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.builder.config.WizardPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class FinishPanel implements WizardPanel, ActionListener{
	private Component component;
	private boolean visited;
	private JTextField configURLText, tutorURLText;
	private Properties properties;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private ConfigurationBuilder configurationBuilder;

	public FinishPanel(ConfigurationBuilder c){
		configurationBuilder = c;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}
	public void apply() {}
	
	public Component getComponent() {
		if(component == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.setBackground(Color.white);

			panel.add(Names.createHeader(this),BorderLayout.NORTH);
			
			// consult button
			JButton upload = new JButton("Publish");
			upload.setBorder(new BevelBorder(BevelBorder.RAISED));
			upload.setIcon(UIHelper.getIcon(Config.getProperty("icon.toolbar.upload")));
			upload.setVerticalTextPosition(SwingConstants.BOTTOM);
			upload.setHorizontalTextPosition(SwingConstants.CENTER);
			upload.addActionListener(this);
			upload.setActionCommand("publish");
			upload.setFont(upload.getFont().deriveFont(Font.BOLD,16f));
			upload.setPreferredSize(new Dimension(200,200));
			
			// consult panel
			JPanel uploadPanel = new JPanel();
			uploadPanel.setOpaque(false);
			uploadPanel.setLayout(new GridBagLayout());
			//uploadPanel.setPreferredSize(new Dimension(700,700));
			uploadPanel.add(upload,new GridBagConstraints());
			panel.add(uploadPanel,BorderLayout.CENTER);
			
			JPanel bottom = new JPanel();
			bottom.setOpaque(false);
			GridBagConstraints l = new GridBagConstraints(0, 0, 1, 1, 1, 1, 
			GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(5,5,5,5),0,0);
			GridBagLayout layout = new GridBagLayout();
			layout.setConstraints(bottom, l);
			bottom.setLayout(layout);
			
			configURLText = new JTextField(50);
			configURLText.setForeground(Color.blue);
			tutorURLText = new JTextField(50);
			tutorURLText.setForeground(Color.blue);
			
			bottom.add(new JLabel("Confguration URL"),l); l.gridx++;
			bottom.add(configURLText,l);l.gridx++;
			bottom.add(UIHelper.createButton("copy-config","Copy Configuation URL to Clipboard",Config.getIconProperty("icon.toolbar.copy"),this),l);
			l.gridx=0;l.gridy++;
			
			bottom.add(new JLabel("Tutor URL"),l); l.gridx++;
			bottom.add(tutorURLText,l);l.gridx++;
			bottom.add(UIHelper.createButton("copy-tutor","Copy Tutor URL to Clipboard",Config.getIconProperty("icon.toolbar.copy"),this),l);
			l.gridx=0;l.gridy+= 3;
			bottom.add(new JLabel(" "),l);
			panel.add(bottom,BorderLayout.SOUTH);
			component = panel;
		}
		return component;
	}

	public String getDescription() {
		return "At this point you are done with your tutor configuration. You can publish your configuration to the central Slidetutor server by pressing " +
				"the <b>Publsh</b> button. Publishing your configuration is the only way the SlideTutor ITS can find it. If you would like to create a hyper link to a " +
				"tutor that uses your configuration, copy/paste the URL below.";
	}

	public String getName() {
		return "Publish Configuration";
	}
	
	public String getShortName() {
		return "Publish";
	}

	public Properties getProperties() {
		if(properties == null)
			properties = new Properties();
		return properties;
	}


	public boolean isVisited() {
		return visited;
	}
	
	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public void load() {
		setVisited(true);
		if(configurationBuilder != null){
			if(configurationBuilder.getConfigurationURL() != null){
				if(configURLText != null){
					configURLText.setText(configurationBuilder.getConfigurationURL().toString());
					tutorURLText.setText(OntologyHelper.DEFAULT_JNLP_SERVLET+"?jnlp=ITS.jnlp&URL="+configurationBuilder.getConfigurationURL().toString());
				}
			}
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}
	public void revert() {
		properties = null;
	}
	public void setProperties(Properties map) {}
		
	public void actionPerformed(ActionEvent e) {	
		if("publish".equals(e.getActionCommand())){
			if(configurationBuilder != null)
				configurationBuilder.doPublish();
		}else if("copy-config".equals(e.getActionCommand())){
			configURLText.selectAll();
			configURLText.copy();
		}else if("copy-tutor".equals(e.getActionCommand())){
			tutorURLText.selectAll();
			tutorURLText.copy();
		}
	}
}
