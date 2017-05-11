package edu.pitt.dbmi.tutor.builder.config.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.*;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.tutor.builder.config.ConfigurationBuilder;
import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.builder.config.WizardPanel;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class OtherModulesPanel implements WizardPanel, ActionListener {
	private final String CONFIG_MODULE = "Configure Module Preferences";
	//private String propertyPrefix = "tutor.st.";
	private ConfigProperties properties;
	private JPanel component,moduleConfig;
	private Class tutorModule;
	private Map<String,Object> tutorModuleMap;
	private List<PropertyPanel> propertyPanels;
	private Map<String,Properties> tutorModulePropertyMap;
	private List<AbstractButton> moduleList;
	private List<JLabel> labelList;
	private boolean visited;
	private ConfigurationBuilder configurationBuilder;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private String currentModule;
	
	private MouseAdapter mouseAdapter = new MouseAdapter() {
		private Color color = new Color(230,230,255);
		public void mouseClicked(MouseEvent e) {
			if(e.getSource() instanceof JLabel){
				String cmd = ((JLabel)e.getSource()).getText(); 
				if(tutorModuleMap.containsKey(cmd)){
					previewModule(cmd);
				}
			}
		}
		public void mouseEntered(MouseEvent e) {
			if(e.getSource() instanceof JLabel){
				((JLabel)e.getSource()).setBackground(color);
				((JLabel)e.getSource()).repaint();
			}
		}

		public void mouseExited(MouseEvent e) {
			if(e.getSource() instanceof JLabel){
				((JLabel)e.getSource()).setBackground(Color.white);
				((JLabel)e.getSource()).repaint();
			}
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

	
	
	public OtherModulesPanel(ConfigurationBuilder conf){
		tutorModule = TutorModule.class;
		configurationBuilder = conf;
		
		// init map of valid modules
		tutorModuleMap = new LinkedHashMap<String, Object>();
		for(String m : Config.getRegisteredModules()){
			try{
				Object o = Class.forName(m).newInstance();
				if(tutorModule.isInstance(o)){
					TutorModule tm = (TutorModule) o;
					if(Names.isOtherModule(tm.getClass().getSimpleName()))
						tutorModuleMap.put(tm.getName(),tm);
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	public Class getTutorModule(){
		return tutorModule;
	}
	
	
	/**
	 * get component representing this panel
	 */
	public Component getComponent() {
		if(component == null){
			component = new JPanel();
			//component.setPreferredSize(new Dimension(900,500));
			component.setLayout(new BorderLayout());
			component.setBackground(Color.white);
			
			JPanel chooser = new JPanel();
			chooser.setLayout(new BoxLayout(chooser,BoxLayout.Y_AXIS));
			chooser.setBackground(Color.white);
			
			// load choices
			moduleList = new ArrayList<AbstractButton>();
			labelList = new ArrayList<JLabel>();
			for(String name : tutorModuleMap.keySet()){
				TutorModule tm = (TutorModule) tutorModuleMap.get(name);
				
				JCheckBox bt = new JCheckBox();
				bt.addActionListener(this);
				bt.setActionCommand(tm.getName());
				bt.setOpaque(false);
				moduleList.add(bt);
				JLabel lbl = new JLabel(tm.getName());
				lbl.addMouseListener(mouseAdapter);
				lbl.setOpaque(true);
				lbl.setBackground(Color.white);
				labelList.add(lbl);
				JPanel p = new JPanel();
				p.setOpaque(false);
				p.setLayout(new FlowLayout(FlowLayout.LEFT));
				p.add(bt);
				p.add(lbl);
				FontMetrics fm = p.getFontMetrics(p.getFont());
				p.setMaximumSize(new Dimension(500,10+fm.getHeight()));
				chooser.add(p);
			}
			
			
			JScrollPane scroll = new JScrollPane(chooser);
			scroll.setBorder(new TitledBorder("Select "+getPrettyName()));
			scroll.setPreferredSize(new Dimension(280,200));
			scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.setOpaque(false);
			//scroll.setBackground(Color.white);
			component.add(scroll,BorderLayout.WEST);
			
			moduleConfig = new JPanel();
			moduleConfig.setOpaque(false);
			moduleConfig.setBorder(new CompoundBorder(new TitledBorder("Configure "+getPrettyName()),new EmptyBorder(5,5,5,5)));
			moduleConfig.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					//if(selectionGroup.getSelection() != null)
					//	previewModule(selectionGroup.getSelection().getActionCommand());
				}
			});
			component.add(Names.createHeader(this),BorderLayout.NORTH);
			component.add(moduleConfig,BorderLayout.CENTER);
			
			
		}
		return component;
	}

	public String getDescription() {
		return  "In addition to selecting appropriate modules for each category, you can specify and configure additional modules that " +
				"you would like to be part of the system. Those modules will be loaded as <b>Other Modules</b>. Behavior of each <b>Other Module</b> depends " +
				"on that module.";
	}

	public String getName() {
		return "Other Tutor Modules";
	}
	public String getShortName() {
		return "Other";
	}
	
	
	private String getPrettyName(){
		return tutorModule.getSimpleName().replaceAll("([a-z])([A-Z])","$1 $2");
	}
	
	public Properties getProperties() {
		if(properties == null){
			properties = new ConfigProperties();
			properties.setProperty("tutor.other.modules","");
			properties.setProperty("tutor.st.other.modules","");
		}
		return properties;
	}

	/**
	 * load values
	 */
	public void load(){
		setVisited(true);
		if(moduleList == null)
			return;
		
		// find tutor module that is in properties
		String otherGlobal = getProperties().getProperty("tutor.other.modules");
		String otherTutor = getProperties().getProperty("tutor.st.other.modules");
		
		// selected modules
		List<String> selected = new ArrayList<String>();
		if(!TextHelper.isEmpty(otherGlobal))
			Collections.addAll(selected,otherGlobal.split(","));
		if(!TextHelper.isEmpty(otherTutor))
			Collections.addAll(selected,otherTutor.split(","));
		
		// go over selected modules
		for(Object o : tutorModuleMap.values()){
			if(o instanceof TutorModule){
				TutorModule tm = (TutorModule) o;
				AbstractButton bt = getButton(tm.getName());
				if(bt != null){
					bt.setSelected(selected.contains(o.getClass().getName()));
					if(bt.isSelected()){
						// reset defaults if appropriate
						Properties p = new ConfigProperties();
						for(Object key: getProperties().keySet()){
							if(key.toString().startsWith(tm.getClass().getSimpleName()+".")){
								p.put(key.toString().substring(tm.getClass().getSimpleName().length()+2),getProperties().get(key));
							}
						}
						// save the modified map in local map
						if(!p.isEmpty()){
							// combine default and modified properties
							Properties p1 = new ConfigProperties();
							p1.putAll(tm.getDefaultConfiguration());
							p1.putAll(p);
							getTutorModulePropertyMap().put(tm.getName(),p1);
						}
					}
				}
			}
		}
		// filter modules
		filterTutorModules();
	}
	
	private void filterTutorModules(){
		if(moduleList == null)
			return;
		
		List<String> disabledModules = new ArrayList<String>();
		for(WizardPanel p : configurationBuilder.getWizardPanels()){
			if(p instanceof TutorModulePanel){
				TutorModulePanel tmp = (TutorModulePanel) p;
				String tm = tmp.getSelectedTutorModule();
				if(tm != null){
					disabledModules.add(tm);
				}
			}
		}
		// go over modules
		for(AbstractButton bt : moduleList){
			bt.setEnabled(!disabledModules.contains(bt.getActionCommand()));
		}
		for(JLabel lb : labelList){
			lb.setForeground(disabledModules.contains(lb.getText())?Color.lightGray:Color.black);
		}
	}
	
	
	/**
	 * get a button in the group with a name
	 * @param name
	 * @return
	 */
	private AbstractButton getButton(String name){
		if(name != null){
			for(AbstractButton bt: moduleList){
				if(name.equals(bt.getActionCommand()))
					return bt;
			}
		}
		return null;
	}
	
	/**
	 * get tutor module (based on class description)
	 * @param desc
	 * @return
	 */
	private TutorModule getTutorModule(String desc){
		for(String name: tutorModuleMap.keySet()){
			Object tm = tutorModuleMap.get(name);
			if(tm.getClass().getName().equals(desc)){
				return (TutorModule) tm;
			}
		}
		return null;
	}
	
	
	public void setProperties(Properties map) {
		for(Object key: getProperties().keySet()){
			if(map.containsKey(key))
				getProperties().put(key,map.get(key));
		}
		// load properties concerning modified default values
		// find tutor module that is in properties
		for(Object o : tutorModuleMap.values()){
			TutorModule tm = (TutorModule) o;
			
			// create custom properties
			Properties p = (Properties) tm.getDefaultConfiguration().clone();
			
			// now iterate over stuff that is inside global configuration
			// and see if there is anything relevant
			for(Object key: map.keySet()){
				//true default
				if(key.toString().startsWith(tm.getClass().getSimpleName()+".")){
					p.put(key.toString().substring(tm.getClass().getSimpleName().length()+1),map.get(key));
				}
			}
			
			// check some global properties
			for(Object key: tm.getDefaultConfiguration().keySet()){
				String gkey = Names.getGlobalProperty(key.toString());
				if(!gkey.equals(key) && map.containsKey(gkey)){
					p.put(gkey,map.get(gkey));
					getProperties().put(gkey,map.get(gkey));
				}
			}
						
			// save the modified map in local map
			if(!p.isEmpty()){
				getTutorModulePropertyMap().put(tm.getName(),p);
			}
		}
	}

	public void apply() {
		if(moduleList == null)
			return;
		StringBuffer otherGlobal = new StringBuffer();
		StringBuffer otherTutor  = new StringBuffer();
		// go over each button in the list
		for(AbstractButton bt: moduleList){
			if(bt.isSelected()){
				TutorModule tm = (TutorModule) tutorModuleMap.get(bt.getActionCommand());
				if(tm != null){
				
					// add to other modules
					if(Arrays.asList(ExpertModule.class,ProtocolModule.class,PedagogicModule.class).contains(tm.getClass())){
						otherGlobal.append(((otherGlobal.length() > 0)?",":"")+tm.getClass().getName());
					}else{
						otherTutor.append(((otherTutor.length() > 0)?",":"")+tm.getClass().getName());
					}
					
					// reset default attributes if available
					if(getTutorModulePropertyMap().containsKey(tm.getName())){
						Properties p = getTutorModulePropertyMap().get(tm.getName());
						for(Object key: p.keySet()){
							// for every key see if it is different from default
							String nval = p.getProperty(key.toString());
							String oval = tm.getDefaultConfiguration().getProperty(key.toString());
							if(!nval.equals(oval)){
								getProperties().setProperty(tm.getClass().getSimpleName()+"."+key,nval);
							}
						}
					}
				
					// add additional "global" properties
					if(propertyPanels != null){
						for(PropertyPanel p: propertyPanels){
							getProperties().setProperty(p.getGlobalPropertyKey(),p.getPropertyValue());
						}
					}
				}
			}
		}
		
		if(otherGlobal.length() > 0)
			getProperties().setProperty("tutor.other.modules",otherGlobal.toString());
		if(otherTutor.length() > 0)
			getProperties().setProperty("tutor.st.other.modules",otherTutor.toString());
	}

	public void revert() {
		properties = null;
		load();
	}

	public void actionPerformed(ActionEvent e) {
		if(tutorModuleMap.containsKey(e.getActionCommand())){
			previewModule(e.getActionCommand());
		}else{
			if(CONFIG_MODULE.equals(e.getActionCommand())){
				configureModule(currentModule);
			}
		}
		pcs.firePropertyChange(new PropertyChangeEvent(this,"CHANGE", null, null));
	}
	
	private Map<String,Properties> getTutorModulePropertyMap(){
		if(tutorModulePropertyMap == null)
			tutorModulePropertyMap = new HashMap<String, Properties>();
		return tutorModulePropertyMap;
	}
	
	/**
	 * configure module
	 * @param key
	 */
	private void configureModule(String key){
		if(key == null)
			return;
		TutorModule itm = (TutorModule) tutorModuleMap.get(key);
		if(itm != null){
			Properties prop = Names.getProperties((ConfigProperties)itm.getDefaultConfiguration());
			//if(prop.isEmpty()){
			//	JOptionPane.showMessageDialog(getComponent(),"<html><font color=green>"+itm.getName()+"</font> module does not have any properties to configure");
			//}else{
			if(getTutorModulePropertyMap().containsKey(itm.getName()))
				prop = getTutorModulePropertyMap().get(itm.getName());
			
			TutorModulePreferencePanel p = new TutorModulePreferencePanel(prop);
			if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(getComponent(),p,CONFIG_MODULE,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
				getTutorModulePropertyMap().put(itm.getName(),p.getProperties());
			}
		}
	}
	
	/**
	 * configure module
	 * @param key
	 */
	private void previewModule(String key){
		TutorModule itm = (TutorModule) tutorModuleMap.get(key);
		if(moduleConfig != null && itm != null){
			currentModule = key;
			if(propertyPanels != null){
				for(PropertyPanel p: propertyPanels)
					getProperties().remove(p.getPropertyKey());
			}
			
			moduleConfig.removeAll();
			moduleConfig.setLayout(new BoxLayout(moduleConfig,BoxLayout.Y_AXIS));
			moduleConfig.setBorder(new CompoundBorder(new TitledBorder("Configure "+getPrettyName()),new EmptyBorder(5,5,5,5)));
			moduleConfig.setPreferredSize(new Dimension(500,500));
			
			JLabel lbl = new JLabel(itm.getName()+" ("+itm.getVersion()+")");
			lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize2D()+4));
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			lbl.setAlignmentY(Component.TOP_ALIGNMENT);
			
			JLabel dsc = new JLabel(getDescription(itm.getDescription(),moduleConfig.getSize().width - 30));
			dsc.setAlignmentY(Component.TOP_ALIGNMENT);
			Font f = dsc.getFont().deriveFont(Font.PLAIN);
			if(f.getSize()-2 > 12)
				f = f.deriveFont(f.getSize2D()-2);
			dsc.setFont(f);
			
			
			ImageIcon sc = (itm instanceof InteractiveTutorModule)?((InteractiveTutorModule)itm).getScreenshot():null;
			JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
			p.setOpaque(false);
			p.setBorder(new EmptyBorder(10,0,10,0));
			p.setAlignmentX(Component.CENTER_ALIGNMENT);
			p.setAlignmentY(Component.TOP_ALIGNMENT);
			p.add(dsc);
			if(sc != null){
				JLabel comp = (JLabel) UIHelper.getImageComponent(sc.getImage(),new Dimension(250,250));
				dsc.setText(getDescription(itm.getDescription(),moduleConfig.getSize().width - comp.getPreferredSize().width - 30));
				comp.setAlignmentY(Component.TOP_ALIGNMENT);
				p.add(comp);
			}
			
			moduleConfig.add(lbl);
			moduleConfig.add(p);
			
			// prefix
			// add to other modules
			String prefix = "tutor.st.";
			if(Arrays.asList(ExpertModule.class,ProtocolModule.class,PedagogicModule.class).contains(itm.getClass())){
				prefix = "tutor.";
			}
			
			// add some optional parameters
			propertyPanels = new ArrayList<PropertyPanel>();
			for(Object k: itm.getDefaultConfiguration().keySet()){
				if(k.toString().startsWith("tutor.")){
					String name   = ""+k;
					PropertyPanel pnl = new PropertyPanel(name,prefix);
					String value  = getProperties().getProperty(pnl.getGlobalPropertyKey());
					if(TextHelper.isEmpty(value))
						value = itm.getDefaultConfiguration().getProperty(pnl.getPropertyKey());
					String tip = null;
					if(itm.getDefaultConfiguration() instanceof ConfigProperties){
						tip = ((ConfigProperties)itm.getDefaultConfiguration()).getPropertyComment(name);
					}
					pnl.setToolTipText(tip);
					pnl.setPropertyValue(value);
					propertyPanels.add(pnl);
					moduleConfig.add(pnl);
				}
			}
			
			
			JButton bt = UIHelper.createButton(CONFIG_MODULE,"Change Default Settings for this Module", Config.getIconProperty("icon.toolbar.preferences"),-1,true,this);
			bt.setAlignmentX(Component.CENTER_ALIGNMENT);
			bt.setAlignmentY(Component.TOP_ALIGNMENT);
			
			if(!Names.getProperties((ConfigProperties)itm.getDefaultConfiguration()).isEmpty())
				moduleConfig.add(bt);
			
			
			moduleConfig.validate();
			moduleConfig.repaint();
		}
	}
	
	/**
	 * pretty print description
	 * @param text
	 * @return
	 */
	private String getDescription(String text,int n){
		return "<html><table width="+n+" height="+n+" cellpadding=10 bgcolor=\"#FFFFFF\">"+text+"</table></html>";
	}
	
	
	private class PropertyPanel extends JPanel{
		private String key,globalKey;
		private JTextField text;
		public PropertyPanel(String key, String propertyPrefix){
			super();
			this.key = key;
			setLayout(new BorderLayout());
			setMaximumSize(new Dimension(1000,34));
			setBorder(new EmptyBorder(0,5,10,5));
			JLabel l = new JLabel(key);
			l.setHorizontalAlignment(JLabel.LEFT);
			add(l,BorderLayout.WEST);
			text = new JTextField(20);
			
			JPanel tp = new JPanel();
			tp.setLayout(new BorderLayout());
			tp.setBorder(new EmptyBorder(0,10,0,10));
			tp.add(text,BorderLayout.CENTER);
			add(tp,BorderLayout.CENTER);
			
			globalKey = key;
			if(key.startsWith("tutor."))
				globalKey = propertyPrefix+key.substring("tutor.".length());
			
			if(key.endsWith(".location")){
				globalKey = globalKey.substring(0,globalKey.length()-".location".length());
				JButton bt = new JButton("Browse");
				bt.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JFileChooser jf = new JFileChooser();
						jf.setSelectedFile(new File(getPropertyValue()));
						if(JFileChooser.APPROVE_OPTION == jf.showOpenDialog(OtherModulesPanel.this.getComponent())){
							setPropertyValue(jf.getSelectedFile().getAbsolutePath());
						}
					}
				});
				add(bt,BorderLayout.EAST);
			}
		}
		
		
		public void setToolTipText(String t) {
			text.setToolTipText(t);
		}


		public String getPropertyKey(){
			return key;
		}
		
		public String getGlobalPropertyKey(){
			return globalKey;
		}
		
		public void setPropertyValue(String s){
			text.setText(s);
		}
		
		public String getPropertyValue(){
			return text.getText();
		}
	}	
}
