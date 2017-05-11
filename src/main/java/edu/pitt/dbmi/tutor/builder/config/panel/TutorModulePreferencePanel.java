package edu.pitt.dbmi.tutor.builder.config.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class TutorModulePreferencePanel extends JPanel {
	private ConfigProperties origProperties;
	private JPanel panel;
	
	public TutorModulePreferencePanel(Properties p){
		super();
		panel = new JPanel();
		GridBagConstraints c = new GridBagConstraints(0,0,1,1,1.0,1.0,
		GridBagConstraints.CENTER, GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0);
		GridBagLayout l = new GridBagLayout();
		l.setConstraints(panel,c);
		panel.setLayout(l);
		
		setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		add(scroll,BorderLayout.CENTER);
		
		if(p instanceof ConfigProperties)
			origProperties = (ConfigProperties)p;
		
		// load properties
		for(Object key: new TreeSet(p.keySet())){
			String value = ""+p.get(key);
			String comment = null;
			if(p instanceof ConfigProperties){
				comment = ((ConfigProperties)p).getPropertyComment(""+key);
			}
			
			// skip if we are filtering everything
			if(Names.isShowAllProperties() || comment == null || !comment.startsWith("#")){
				continue;
			}
			
			JTextField keyText = new JTextField(20);
			keyText.setText(""+key);
			
			JLabel lbl = new JLabel("      =      ");
			lbl.setHorizontalTextPosition(JLabel.CENTER);
			
			JComponent valComp = null;
			if(value.toLowerCase().matches("(true|false)")){
				JComboBox valComb = new JComboBox(new String []{"false","true"});
				valComb.setPreferredSize(keyText.getPreferredSize());
				valComb.setSelectedItem(value.toLowerCase());
				valComb.setBackground(Color.white);
				valComp = valComb;
			}else if(key.toString().toLowerCase().matches(".*\\b(color|background)\\b.*") && (value.matches("[\\d\\s]+") || value.matches("\\s*\\w+\\s*"))){
				final JButton bt = new JButton(value);
				bt.setPreferredSize(keyText.getPreferredSize());
				bt.setBackground(UIHelper.getColor(value));
				bt.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Color c = JColorChooser.showDialog(TutorModulePreferencePanel.this,"Select Color",bt.getBackground());
						if(c != null){
							bt.setBackground(c);
							bt.setText(UIHelper.getColor(c));
						}
					}
				});
				valComp = bt;
			}else{
				Pattern pt = Pattern.compile(".*<([\\w\\|\\s]+)>.*");
				if(comment != null){
					Matcher mt = pt.matcher(comment);
					if(mt.matches()){
						Vector v = new Vector();
						for(String opt : mt.group(1).split("\\|"))
							v.add(opt.trim());
						JComboBox valComb = new JComboBox(v);
						valComb.setPreferredSize(keyText.getPreferredSize());
						valComb.setSelectedItem(value.toLowerCase());
						valComb.setBackground(Color.white);
						valComp = valComb;
					}
				}
				
				// by default represent as text field
				if(valComp == null){
					JTextField valText = new JTextField(20);
					valText.setText(value);
					valComp = valText;
				}
			}
			
			// block content not deemed safe
			if(!Names.isShowAllProperties() && comment != null && comment.contains("[BLOCK]")){
				if(valComp instanceof JTextField){
					JPasswordField valText = new JPasswordField(20);
					valText.setText(value);
					valComp = valText;
					//((JTextField)valComp).setText(value.replaceAll(".","*"));
				}
			}
			
			
			// filter comment
			if(comment != null){
				comment = "<html><table width=500><tr><td>"+comment.replaceAll("#","").trim()+"</td></tr></table></html>";
			}
			// set tooltips
			valComp.setToolTipText(comment);
			keyText.setToolTipText(comment);
			
			
			panel.add(keyText,c); c.gridx++;
			panel.add(lbl,c); c.gridx++;
			panel.add(valComp,c); c.gridy++;c.gridx = 0;
			
		}
		// enforce size if too big
		if(panel.getComponentCount()/3 > 24)
			scroll.setPreferredSize(new Dimension(550,460));
	}
	
	/**
	 * get all properties from compoents
	 */
	public ConfigProperties getProperties(){
		ConfigProperties p = (origProperties != null)?origProperties.clone():new ConfigProperties();
		for(int i=0;i<panel.getComponentCount();i+= 3){
			String key = ((JTextField)panel.getComponent(i)).getText();
			String val = "";
			
			if(panel.getComponent(i+2) instanceof JTextField)
				val = new String(((JPasswordField)panel.getComponent(i+2)).getPassword());
			else if(panel.getComponent(i+2) instanceof JTextField)
				val = ((JTextField)panel.getComponent(i+2)).getText();
			else if(panel.getComponent(i+2) instanceof JComboBox)
				val = ""+((JComboBox)panel.getComponent(i+2)).getSelectedItem();
			else if(panel.getComponent(i+2) instanceof JButton)
				val = ""+((JButton)panel.getComponent(i+2)).getText();
			
			p.put(key,val);
		}
		return p;
	}
}
