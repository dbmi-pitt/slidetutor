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
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

public class TutorModulePanel implements WizardPanel, ActionListener {
	private final String CONFIG_MODULE = "Configure Module Preferences";
	private String propertyPrefix = "tutor.st.";
	private ConfigProperties properties;
	private Properties origProperties;
	private JPanel component,moduleConfig;
	private Class tutorModule;
	private Map<String,Object> tutorModuleMap;
	private List<PropertyPanel> propertyPanels;
	private Map<String,Properties> tutorModulePropertyMap;
	private ButtonGroup selectionGroup;
	private boolean visited;
	private ConfigurationBuilder configurationBuilder;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
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

	
	
	public TutorModulePanel(ConfigurationBuilder c, Class in){
		tutorModule = in;
		configurationBuilder = c;
		
		// fix prefix
		if(Arrays.asList(ExpertModule.class,ProtocolModule.class,PedagogicModule.class).contains(in)){
			propertyPrefix = "tutor.";
		}
		// init map of valid modules
		tutorModuleMap = new LinkedHashMap<String, Object>();
		for(String m : Config.getRegisteredModules()){
			try{
				Object o = Class.forName(m).newInstance();
				if(tutorModule.isInstance(o)){
					TutorModule tm = (TutorModule) o;
					if(!Names.isExcludedModule(tm.getClass().getSimpleName()))
						tutorModuleMap.put(Names.getName(tm),tm);
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
			component.setPreferredSize(new Dimension(500,500));
			component.setLayout(new BorderLayout());
			component.setBackground(Color.white);
			
			JPanel chooser = new JPanel();
			chooser.setLayout(new BoxLayout(chooser,BoxLayout.Y_AXIS));
			chooser.setBackground(Color.white);
			chooser.setOpaque(true);
			
			// load choices
			int max = 100;
			selectionGroup = new ButtonGroup();
			for(String name : tutorModuleMap.keySet()){
				TutorModule tm = (TutorModule) tutorModuleMap.get(name);
				
				JRadioButton bt = new JRadioButton(Names.getName(tm));
				bt.addActionListener(this);
				bt.setActionCommand(Names.getName(tm));
				bt.setOpaque(false);
				selectionGroup.add(bt);
				chooser.add(bt);
				
				FontMetrics fm = bt.getFontMetrics(bt.getFont());
				int x = fm.stringWidth(bt.getText())+50;
				if(x > max)
					max = x;
			}
			
			
			JScrollPane scroll = new JScrollPane(chooser);
			scroll.setBorder(new TitledBorder("Select "+Names.getName(tutorModule)));
			scroll.setPreferredSize(new Dimension(max,250));
			scroll.setOpaque(false);
			component.add(scroll,BorderLayout.WEST);
			
			moduleConfig = new JPanel();
			moduleConfig.setOpaque(false);
			moduleConfig.setBorder(new CompoundBorder(new TitledBorder("Configure "+Names.getName(tutorModule)),new EmptyBorder(5,5,5,5)));
			moduleConfig.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					if(selectionGroup.getSelection() != null)
						previewModule(selectionGroup.getSelection().getActionCommand());
				}
			});
			component.add(Names.createHeader(this),BorderLayout.NORTH);
			component.add(moduleConfig,BorderLayout.CENTER);
			
			
		}
		return component;
	}

	public String getDescription() {
		return "Select the "+Names.getName(tutorModule)+" that is going to be used by the tutor. Each individual " +
				"module can be tweaked further by adjusting its default properties. Some modules may have " +
				"additional configuration options that are specific to them.";
	}

	public String getName() {
		return Names.getName(tutorModule)+" Configuration";
	}
	
	public String getShortName() {
		String s =  Names.getName(tutorModule);
		if(s.endsWith("Module"))
			return s.substring(0,s.length()-"Module".length()).trim();
		return s;
	}
	
	private String getPrettyPropertyName(){
		return propertyPrefix+tutorModule.getSimpleName().replaceAll("([a-z])([A-Z])","$1.$2").toLowerCase();
	}
	
	public Properties getProperties() {
		if(properties == null){
			properties = new ConfigProperties();
		
			// initialize properties with first entry in the map
			String d = getDefaultSelection();
			if(d != null)
				properties.put(getPrettyPropertyName(),d);
		}
		return properties;
	}

	private String getDefaultSelection(){
		// initialize properties with first entry in the map
		if(tutorModuleMap.keySet().iterator().hasNext()){
			return tutorModuleMap.get(tutorModuleMap.keySet().iterator().next()).getClass().getName();
		}
		return null;
	}
	
	private String getDefaultSelectionName(){
		// initialize properties with first entry in the map
		if(tutorModuleMap.keySet().iterator().hasNext()){
			return tutorModuleMap.keySet().iterator().next();
		}
		return null;
	}
	private Class getDefaultSelectionClass(){
		// initialize properties with first entry in the map
		if(tutorModuleMap.keySet().iterator().hasNext()){
			return tutorModuleMap.get(tutorModuleMap.keySet().iterator().next()).getClass();
		}
		return null;
	}
	private TutorModule getDefaultSelectionObject(){
		// initialize properties with first entry in the map
		if(tutorModuleMap.keySet().iterator().hasNext()){
			return (TutorModule) tutorModuleMap.get(tutorModuleMap.keySet().iterator().next());
		}
		return null;
	}
	
	/**
	 * load values
	 */
	public void load(){
		setVisited(true);
		if(selectionGroup == null)
			return;
				
		// find tutor module that is in properties
		String value = getProperties().getProperty(getPrettyPropertyName());
		TutorModule tm = getTutorModule(value);
		if(tm != null){
			// select the button	
			AbstractButton b = getButton(tm.getName());
			if(b != null){
				b.doClick();
			}
			// reset defaults if appropriate
			Properties p = new ConfigProperties();
			for(Object key: getProperties().keySet()){
				if(key.toString().startsWith(tm.getClass().getSimpleName()+".")){
					p.put(key.toString().substring(tm.getClass().getSimpleName().length()+1),getProperties().get(key));
				}
			}
			
			// save the modified map in local map
			if(!p.isEmpty()){
				// combine default and modified properties
				ConfigProperties p1 = new ConfigProperties();
				p1.putAll(tm.getDefaultConfiguration());
				for(Object key: tm.getDefaultConfiguration().keySet()){
					p1.put(key,tm.getDefaultConfiguration().get(key));
					p1.setPropertyComment(key.toString(),((ConfigProperties)tm.getDefaultConfiguration()).getPropertyComment(key.toString()));
				}
				p1.putAll(p);
				getTutorModulePropertyMap().put(tm.getName(),p1);
			}
		}
		
		filterTutorModules();
	}
	
	private void filterTutorModules(){
		if(selectionGroup == null)
			return;
		
		// gather a list of selected modules (before this panel)
		List<Class> selectedModules = new ArrayList<Class>();
		for(WizardPanel p : configurationBuilder.getWizardPanels()){
			// stop looking after oneself
			if(p.equals(this))
				break;
			
			if(p instanceof TutorModulePanel){
				TutorModulePanel tmp = (TutorModulePanel) p;
				Class tm = tmp.getSelectedTutorModuleClass();
				if(tm != null){
					selectedModules.add(tm);
				}
			}
		}
		
		
		// go over all modules in the list
		boolean reselect = false;
		for(String n1: tutorModuleMap.keySet()){
			Class m1 = tutorModuleMap.get(n1).getClass();
			AbstractButton bt = getButton(n1);
			bt.setEnabled(true);
			for(Class m2: selectedModules){
				if(!configurationBuilder.isCompatibleWith(m1,m2)){
					// this module is not compatible w/ something
					bt.setEnabled(false);
					if(bt.isSelected())
						reselect = true;
				}
				// setup interlinked properties (most of the time a noop)
				configurationBuilder.setupProperty(m1,m2,((TutorModule)tutorModuleMap.get(n1)).getDefaultConfiguration());
			}
		}
		
		// make sure no disabled button is selected
		if(reselect){
			for(Enumeration<AbstractButton> e = selectionGroup.getElements();e.hasMoreElements();){
				AbstractButton b = e.nextElement();
				if(b.isEnabled()){
					b.doClick();
					break;
				}
			}
		}
	}
	
	
	
	/**
	 * get a button in the group with a name
	 * @param name
	 * @return
	 */
	private AbstractButton getButton(String name){
		if(selectionGroup == null)
			return null;
		// select appropriate 
		for(Enumeration<AbstractButton> e = selectionGroup.getElements();e.hasMoreElements();){
			AbstractButton b = e.nextElement();
			if(name.equals(b.getActionCommand())){
				return b;
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
	
	
	/**
	 * get selected tutor module
	 * @return
	 */
	public String getSelectedTutorModule(){
		if(selectionGroup != null && selectionGroup.getSelection() != null){
			return ((TutorModule)tutorModuleMap.get(selectionGroup.getSelection().getActionCommand())).getName();
		}
		return getDefaultSelectionName();
	}
	
	/**
	 * get selected tutor module
	 * @return
	 */
	public Class getSelectedTutorModuleClass(){
		if(selectionGroup != null && selectionGroup.getSelection() != null){
			return ((TutorModule)tutorModuleMap.get(selectionGroup.getSelection().getActionCommand())).getClass();
		}
		return getDefaultSelectionClass();
	}
	
	/**
	 * get selected tutor module
	 * @return
	 */
	public TutorModule getSelectedTutorModuleObject(){
		if(selectionGroup != null && selectionGroup.getSelection() != null){
			return (TutorModule)tutorModuleMap.get(selectionGroup.getSelection().getActionCommand());
		}
		return getDefaultSelectionObject();
	}
	
	
	public void setProperties(Properties map) {
		origProperties = map;
		// load properties concerning main module selection
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
		if(selectionGroup != null && selectionGroup.getSelection() != null){
			TutorModule tm = (TutorModule) tutorModuleMap.get(selectionGroup.getSelection().getActionCommand());
			if(tm != null){
				// set the main property in question
				getProperties().setProperty(getPrettyPropertyName(),tm.getClass().getName());
				
				// reset default attributes if available
				if(getTutorModulePropertyMap().containsKey(tm.getName())){
					Properties p = getTutorModulePropertyMap().get(tm.getName());
					for(Object key: p.keySet()){
						// for every key see if it is different from default
						String nval = p.getProperty(key.toString());
						String oval = tm.getDefaultConfiguration().getProperty(key.toString());
						if(oval != null && !nval.equals(oval) || (origProperties != null && origProperties.containsKey(tm.getClass().getSimpleName()+"."+key))){
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

	public void revert() {
		properties = null;
		load();
	}

	public void actionPerformed(ActionEvent e) {
		if(tutorModuleMap.containsKey(e.getActionCommand())){
			previewModule(e.getActionCommand());
		}else{
			if(CONFIG_MODULE.equals(e.getActionCommand())){
				if(selectionGroup.getSelection() != null)
					configureModule(selectionGroup.getSelection().getActionCommand());
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
		TutorModule itm = (TutorModule) tutorModuleMap.get(key);
		if(itm != null){
			Properties prop = Names.getProperties((ConfigProperties)itm.getDefaultConfiguration());
			if(getTutorModulePropertyMap().containsKey(itm.getName()))
				prop = getTutorModulePropertyMap().get(itm.getName());
			
			TutorModulePreferencePanel p = new TutorModulePreferencePanel(prop);
			if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(getComponent(),p,CONFIG_MODULE,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
				getTutorModulePropertyMap().put(itm.getName(),p.getProperties());
			}
			//}
		}
	}
	
	/**
	 * configure module
	 * @param key
	 */
	private void previewModule(String key){
		TutorModule itm = (TutorModule) tutorModuleMap.get(key);
		if(moduleConfig != null && itm != null){
			if(propertyPanels != null){
				for(PropertyPanel p: propertyPanels)
					getProperties().remove(p.getPropertyKey());
			}
			
			moduleConfig.removeAll();
			moduleConfig.setLayout(new BoxLayout(moduleConfig,BoxLayout.Y_AXIS));
			moduleConfig.setBorder(new CompoundBorder(new TitledBorder("Configure "+Names.getName(tutorModule)),new EmptyBorder(5,5,5,5)));
			moduleConfig.setPreferredSize(new Dimension(500,500));
			moduleConfig.setOpaque(false);
			
			JLabel lbl = new JLabel(itm.getName()+" ("+itm.getVersion()+")");
			lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize2D()+4));
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			lbl.setAlignmentY(Component.TOP_ALIGNMENT);
			
			JLabel dsc = new JLabel(Names.getDescription(itm.getDescription(),moduleConfig.getSize().width - 30));
			Font f = dsc.getFont().deriveFont(Font.PLAIN);
			if(f.getSize()-2 > 12)
				f = f.deriveFont(f.getSize2D()-2);
			dsc.setFont(f);
			dsc.setAlignmentY(Component.TOP_ALIGNMENT);
			
			
			
			ImageIcon sc = (itm instanceof InteractiveTutorModule)?((InteractiveTutorModule)itm).getScreenshot():null;
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
			p.setBorder(new EmptyBorder(10,0,10,0));
			p.setAlignmentX(Component.CENTER_ALIGNMENT);
			p.setAlignmentY(Component.TOP_ALIGNMENT);
			p.add(dsc);
			if(sc != null){
				JLabel comp = (JLabel) UIHelper.getImageComponent(sc.getImage(),new Dimension(250,250));
				dsc.setText(Names.getDescription(itm.getDescription(),moduleConfig.getSize().width - comp.getPreferredSize().width - 30));
				comp.setAlignmentY(Component.TOP_ALIGNMENT);
				p.add(comp);
			}
			
			moduleConfig.add(lbl);
			moduleConfig.add(p);
			
			// add some optional parameters
			propertyPanels = new ArrayList<PropertyPanel>();
			for(Object k: itm.getDefaultConfiguration().keySet()){
			//for(Object k: getTutorModulePropertyMap().get(itm.getName()).keySet()){
				if(k.toString().startsWith("tutor.")){
					String name   = ""+k;
					PropertyPanel pnl = new PropertyPanel(name);
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
			
			Properties props = Names.getProperties((ConfigProperties)itm.getDefaultConfiguration());
			if(!props.isEmpty())
				moduleConfig.add(bt);
			
			
			moduleConfig.validate();
			moduleConfig.repaint();
		}
	}
	
	/**
	 * pretty print description
	 * @param text
	 * @return
	 *
	private String getDescription(String text,int n){
		return "<html><table width="+n+" height="+n+" cellpadding=10 bgcolor=\"#FFFFFF\">"+text+"</table></html>";
	}
	*/
	
	private class PropertyPanel extends JPanel{
		private String key,globalKey;
		private JTextField text;
		public PropertyPanel(String key){
			super();
			this.key = key;
			setLayout(new BorderLayout());
			setMaximumSize(new Dimension(1000,34));
			setBorder(new EmptyBorder(0,5,10,5));
			JLabel l = new JLabel(Names.getEditorLabel(key));
			l.setHorizontalAlignment(JLabel.LEFT);
			add(l,BorderLayout.WEST);
			text = new JTextField(20);
			setOpaque(false);
			JPanel tp = new JPanel();
			tp.setLayout(new BorderLayout());
			tp.setBorder(new EmptyBorder(0,10,0,10));
			tp.add(text,BorderLayout.CENTER);
			tp.setOpaque(false);
			add(tp,BorderLayout.CENTER);
			
			globalKey = key;
			if(key.startsWith("tutor."))
				globalKey = propertyPrefix+key.substring("tutor.".length());
			
			if(key.endsWith(".location")){
				globalKey = globalKey.substring(0,globalKey.length()-".location".length());
				/*
				JButton bt = new JButton("Browse");
				bt.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JFileChooser jf = new JFileChooser();
						jf.setSelectedFile(new File(getPropertyValue()));
						if(JFileChooser.APPROVE_OPTION == jf.showOpenDialog(TutorModulePanel.this.getComponent())){
							setPropertyValue(jf.getSelectedFile().getAbsolutePath());
						}
					}
				});
				*/
				add(Names.getEditorButton(key,text),BorderLayout.EAST);
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
