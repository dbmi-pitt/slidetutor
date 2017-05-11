package edu.pitt.dbmi.tutor.builder.config.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.*;

import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.builder.config.WizardPanel;
import edu.pitt.dbmi.tutor.builder.protocol.ProtocolSelector;
import edu.pitt.dbmi.tutor.builder.protocol.UserManager;
import edu.pitt.dbmi.tutor.builder.sequence.SequenceBuilder;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.ui.DomainSelectorPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class StartPanel implements WizardPanel, ActionListener {
	private final String DEFAULT_TAG = "DEFAULT";
	private final String CURRICULUM_OPTION_TAG = "CURRICULUM_";
	private final String INTERFACE_OPTION_TAG = "INTERFACE_";
	private final String FEEDBACK_OPTION_TAG = "FEEDBACK_";
	private final String DATABASE_OPTION_TAG = "DATABASE_";
	private final String DEFAULT_CONFIG = "/resources/DefaultConfiguration.conf";
	
	private Component component;
	private boolean visited;
	private Properties properties;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private SequenceBuilder sequenceBuilder;
	private ButtonGroup tutorGroup, feedbackGroup,curriculumGroup,databaseGroup;
	private JLabel tutorInterfaceDescription,tutorFeedbackDescription,databaseDescription;
	private JLabel tutorInterfaceScreenshot;
	private JTextField condition,domain, caseList;
	private Map<String,ConfigOptions> tutorConfigMap,curriculumConfigMap,databaseConfigMap,feedbackConfigMap;
	private JButton chooseDomain,chooseCases,configDB;
	private DomainSelectorPanel domainSelector;
	private ProtocolModule protocol;
	private List<JRadioButton> reset;
	
	/**
	 * defines configuration options for 
	 * @author tseytlin
	 */
	private class ConfigOptions {
		public String name,description,screenshot;
		public Properties config = new Properties();
		public ConfigOptions(Properties p){
			for(Object key: p.keySet()){
				if("name".equals(key)){
					name = (String) p.get(key);
				}else if("description".equals(key)){
					description = (String) p.get(key);
				}else if("screenshot".equals(key)){
					screenshot = (String) p.get(key);	
				}else{
					config.put(key,p.get(key));
				}
			}
		}
	}
	
	public StartPanel(){
		init();
	}
	
	private void init(){
		tutorConfigMap = new LinkedHashMap<String,ConfigOptions>();
		curriculumConfigMap = new LinkedHashMap<String,ConfigOptions>();
		databaseConfigMap = new LinkedHashMap<String,ConfigOptions>();
		feedbackConfigMap = new LinkedHashMap<String,ConfigOptions>();

		// initialize some default values for tutor
		try {
			loadDefaults(getClass().getResourceAsStream(DEFAULT_CONFIG));
		} catch (IOException e) {
			Config.getLogger().severe(TextHelper.getErrorMessage(e));
		}
		// diable red alert
		Config.setProperty("red.alert.enabled","false");
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	public void apply() {
		// load interface config
		if(tutorGroup.getSelection() != null){
			ConfigOptions op = tutorConfigMap.get(tutorGroup.getSelection().getActionCommand());
			if(op != null){
				getProperties().putAll(op.config);
			}
		}
		// curriculum
		if(curriculumGroup.getSelection() != null){
			ConfigOptions op = curriculumConfigMap.get(curriculumGroup.getSelection().getActionCommand());
			if(op != null){
				getProperties().putAll(op.config);
			}
		}
		
		// feedback
		if(feedbackGroup.getSelection() != null){
			ConfigOptions op = feedbackConfigMap.get(feedbackGroup.getSelection().getActionCommand());
			if(op != null){
				getProperties().putAll(op.config);
			}
		}
		
		// database
		if(databaseGroup.getSelection() != null){
			ConfigOptions op = databaseConfigMap.get(databaseGroup.getSelection().getActionCommand());
			if(op != null){
				getProperties().putAll(op.config);
			}
		}
		// set vals
		if(!TextHelper.isEmpty(condition.getText()))
			getProperties().setProperty("tutor.condition",condition.getText().trim());
		if(!TextHelper.isEmpty(domain.getText()))
			getProperties().setProperty("tutor.domain",domain.getText().trim());
		if(!TextHelper.isEmpty(caseList.getText()))
			getProperties().setProperty("tutor.case.sequence",caseList.getText().trim());
		
		
		// set protocol values if different from defaults
		if(protocol != null && configDB.isEnabled() && !isDeafultDatabase()){
			for(Object key: protocol.getDefaultConfiguration().keySet()){
				getProperties().put(DatabaseProtocolModule.class.getSimpleName()+"."+key,protocol.getDefaultConfiguration().get(key));
			}
		}
		
	}
	
	private boolean isDeafultDatabase(){
		if(protocol == null)
			return true;
		return ProtocolSelector.DEFAULT_DATABASE_PROPERTIES.getProperty("protocol.url").equals(protocol.getDefaultConfiguration().getProperty("protocol.url"));
	}
	

	public Component getComponent() {
		if(component == null){
			final String DEFAULT_CONDITION = "Deployment";
			Color color = Color.yellow;
			JPanel panel = new JPanel();
			component = panel;
			component.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					for(ButtonGroup gr: Arrays.asList(curriculumGroup,tutorGroup,feedbackGroup,databaseGroup)){
						Enumeration<AbstractButton> bt = gr.getElements();
						while(bt.hasMoreElements()){
							AbstractButton b = bt.nextElement();
							if(b.isSelected()){
								b.doClick();
								break;
							}
						}
					}
				}
			});
			
			reset = new ArrayList<JRadioButton>();
						
			Font f = panel.getFont().deriveFont(Font.PLAIN);
			if(f.getSize()-2 > 12)
				f = f.deriveFont(f.getSize2D()-2);
			
			panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
			panel.setBackground(Color.white);
			
			JPanel cp = new JPanel();
			cp.setBorder(new TitledBorder("Curriculum"));
			cp.setOpaque(false);
					
			// tutor chooser
			GridBagLayout l = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,
			GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,5,0,5),0,0);
			JPanel cchooser = new JPanel();
			l.setConstraints(cchooser,c);
			cchooser.setLayout(l);
			cchooser.setOpaque(false);
			curriculumGroup = new ButtonGroup();
			
			
			// load choices for tutors
			cchooser.add(new JLabel("Name of the Curriculum"),c); c.gridx++;
			condition = new JTextField(DEFAULT_CONDITION);
			condition.setHorizontalAlignment(JTextField.CENTER);
			condition.setPreferredSize(new Dimension(200,25));
			cchooser.add(condition,c); c.gridy++;c.gridx = 0;
			
			// add buttons
			for(String name : curriculumConfigMap.keySet()){
				JRadioButton bt = new JRadioButton(name);
				bt.addActionListener(this);
				bt.setActionCommand(name);
				bt.setOpaque(false);
				curriculumGroup.add(bt);
				cchooser.add(bt,c);c.gridy++;
			}
			JRadioButton r = new JRadioButton("Reset");
			reset.add(r);
			curriculumGroup.add(r);
			
			// add fields
			domain = new JTextField(30);
			domain.setPreferredSize(condition.getPreferredSize());
			domain.setForeground(Color.blue);
			c.gridy=1;c.gridx=1;
			cchooser.add(domain,c); c.gridx++;
			chooseDomain = UIHelper.createButton("Choose Domain","Select Default Domain",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.book"),16),-1,true,this);
			cchooser.add(chooseDomain,c);
			c.gridy++;c.gridx =1;
			caseList = new JTextField();
			caseList.setPreferredSize(condition.getPreferredSize());
			cchooser.add(caseList,c); c.gridx++;
			chooseCases = UIHelper.createButton("Choose Cases","Create a List of Cases",
					UIHelper.getIcon(Config.getProperty("icon.menu.sequence"),16),-1,true,this);
			cchooser.add(chooseCases,c);
			cp.add(cchooser,BorderLayout.CENTER);
			
			
			// tutor panel
			JPanel tp = new JPanel();
			tp.setBorder(new TitledBorder("Tutor Interface"));
			tp.setLayout(new BorderLayout());
			tp.setOpaque(false);
			// tutor chooser
			JPanel tchooser = new JPanel();
			tchooser.setLayout(new BoxLayout(tchooser,BoxLayout.Y_AXIS));
			tchooser.setOpaque(false);
			//tchooser.setBackground(Color.white);
			
			// load choices for tutors
			tutorGroup = new ButtonGroup();
			for(String name : tutorConfigMap.keySet()){
				JRadioButton bt = new JRadioButton(name);
				bt.addActionListener(this);
				bt.setActionCommand(name);
				bt.setOpaque(false);
				tutorGroup.add(bt);
				tchooser.add(bt);
			}
			r = new JRadioButton("Reset");
			reset.add(r);
			tutorGroup.add(r);
			tutorInterfaceDescription = new JLabel();
			tutorInterfaceDescription.setBorder(new EmptyBorder(5,5,5,5));
			tutorInterfaceDescription.setHorizontalAlignment(JLabel.CENTER);
			tutorInterfaceDescription.setFont(f);
			//tutorInterfaceDescription.setEditable(false);
			//tutorInterfaceDescription.setLineWrap(true);
			//tutorInterfaceDescription.setWrapStyleWord(true);
			//tutorInterfaceDescription.setBackground(color);
			
			
			tutorInterfaceScreenshot = new JLabel();
			tutorInterfaceScreenshot.setHorizontalAlignment(JLabel.RIGHT);
			tutorInterfaceScreenshot.setVerticalAlignment(JLabel.TOP);
			
			tchooser.add(tutorInterfaceDescription);
			
			// preview panel
			JPanel sp = new JPanel();
			sp.setLayout(new BorderLayout());
			sp.setPreferredSize(new Dimension(400,300));
			sp.setBorder(new EmptyBorder(5,5,5,5));
			sp.add(tutorInterfaceScreenshot,BorderLayout.CENTER);
			sp.setOpaque(false);
			
			tp.add(tchooser,BorderLayout.WEST);
			tp.add(sp,BorderLayout.CENTER);
						
			// feedback panel
			JPanel fp = new JPanel();
			fp.setBorder(new TitledBorder("Tutor Feedback"));
			fp.setLayout(new BorderLayout());
			fp.setOpaque(false);
			
			// tutor chooser
			JPanel fchooser = new JPanel();
			fchooser.setLayout(new BoxLayout(fchooser,BoxLayout.Y_AXIS));
			fchooser.setOpaque(false);
			//chooser.setBackground(Color.white);
			
			// load choices for tutors
			feedbackGroup = new ButtonGroup();
			for(String name : feedbackConfigMap.keySet()){
				JRadioButton bt = new JRadioButton(name);
				bt.addActionListener(this);
				bt.setActionCommand(name);
				bt.setOpaque(false);
				feedbackGroup.add(bt);
				fchooser.add(bt);
			}
			r = new JRadioButton("Reset");
			reset.add(r);
			feedbackGroup.add(r);
			fp.add(fchooser,BorderLayout.CENTER);
			tutorFeedbackDescription = new JLabel();
			tutorFeedbackDescription.setHorizontalAlignment(JLabel.CENTER);
			tutorFeedbackDescription.setFont(f);
			tutorFeedbackDescription.setBorder(new EmptyBorder(0,5,5,5));
			//tutorFeedbackDescription.setEditable(false);
			//tutorFeedbackDescription.setLineWrap(true);
			//tutorFeedbackDescription.setWrapStyleWord(true);
			//tutorFeedbackDescription.setBackground(color);
			
			fp.add(tutorFeedbackDescription,BorderLayout.EAST);
			
			
			JPanel dp = new JPanel();
			dp.setBorder(new TitledBorder("Data Collection"));
			dp.setOpaque(false);
			dp.setLayout(new BorderLayout());
			
			// tutor chooser
			JPanel dchooser = new JPanel();
			dchooser.setLayout(new BoxLayout(dchooser,BoxLayout.Y_AXIS));
			dchooser.setOpaque(false);
			//chooser.setBackground(Color.white);
			
			// load choices for tutors
			databaseGroup = new ButtonGroup();
			for(String name : databaseConfigMap.keySet()){
				JRadioButton bt = new JRadioButton(name);
				bt.addActionListener(this);
				bt.setActionCommand(name);
				bt.setOpaque(false);
				databaseGroup.add(bt);
				dchooser.add(bt);
			}
			r = new JRadioButton("Reset");
			reset.add(r);
			databaseGroup.add(r);
			configDB = UIHelper.createButton("Configure Database","Configure Custom Database",
					UIHelper.getIcon(Config.getProperty("icon.toolbar.database"),24),-1,true,this);
			dchooser.add(configDB);
			dp.add(dchooser,BorderLayout.WEST);
			databaseDescription = new JLabel();
			databaseDescription.setHorizontalAlignment(JLabel.CENTER);
			databaseDescription.setFont(f);
			databaseDescription.setBorder(new EmptyBorder(0,5,5,5));
			//databaseDescription.setEditable(false);
			//databaseDescription.setLineWrap(true);
			//databaseDescription.setWrapStyleWord(true);
			//databaseDescription.setBackground(color);
			dp.add(databaseDescription,BorderLayout.EAST);
			
			panel.add(wrap(cp,"1."));
			panel.add(wrap(tp,"2."));
			panel.add(wrap(fp,"3."));
			panel.add(wrap(dp,"4."));
			
			// do first selection
			curriculumGroup.getElements().nextElement().doClick();
			tutorGroup.getElements().nextElement().doClick();
			feedbackGroup.getElements().nextElement().doClick();
			databaseGroup.getElements().nextElement().doClick();
		}
		return component;
	}
	
	/**
	 * 
	 * @param c
	 * @param x
	 * @return
	 */
	private JComponent wrap(JComponent c, String x){
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(new EmptyBorder(10,10,10,10));
		p.setOpaque(false);
		p.add(c,BorderLayout.CENTER);
		
		JLabel l = new JLabel(x);
		l.setForeground(Color.LIGHT_GRAY);
		l.setBorder(new EmptyBorder(10,10,10,10));
		l.setFont(l.getFont().deriveFont(Font.BOLD,40));
		p.add(l,BorderLayout.WEST);
		
		return p;
	}
	
	private int getComponentWidth(){
		if(component != null){
			Component c = component;
			if(c.getParent() != null)
				c = c.getParent();
			if(c.getSize().getWidth() > 0)
				return c.getSize().width;
			return c.getPreferredSize().width;
		}
		return 0;
	}
	
	private int getRadioWidth(Component comp, Collection<String> names){
		String txt = null;
		for(String s: names){
			if(txt == null || txt.length() < s.length())
				txt = s;
		}
		FontMetrics fm = comp.getFontMetrics(comp.getFont());
		return fm.stringWidth(txt);
	}
	
	
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(tutorConfigMap.containsKey(cmd)){
			ConfigOptions op = tutorConfigMap.get(cmd);
			if(op.screenshot != null)
				tutorInterfaceScreenshot.setIcon(UIHelper.getIcon(op.screenshot));
			int w = getComponentWidth()-520;
			int rw = getRadioWidth(databaseDescription,databaseConfigMap.keySet())+60;
			if(rw > w)
				w = rw;
			tutorInterfaceDescription.setText(Names.getDescription(op.description,w,400));
		}else if(feedbackConfigMap.containsKey(cmd)){
			ConfigOptions op = feedbackConfigMap.get(cmd);
			int w = getRadioWidth(tutorFeedbackDescription,feedbackConfigMap.keySet())+160;
			tutorFeedbackDescription.setText(Names.getDescription(op.description,getComponentWidth()-w,300));
		}else if(curriculumConfigMap.containsKey(cmd)){
			//ConfigOptions op = curriculumConfigMap.get(cmd);
			boolean b = "Random Cases".equals(cmd);
			domain.setEditable(b);
			chooseDomain.setEnabled(b);
			caseList.setEditable(!b);
			chooseCases.setEnabled(!b);
		}else if(databaseConfigMap.containsKey(cmd)){
			ConfigOptions op = databaseConfigMap.get(cmd);
			int w = getRadioWidth(databaseDescription,databaseConfigMap.keySet())+180;
			databaseDescription.setText(Names.getDescription(op.description,getComponentWidth()-w,300));
			configDB.setEnabled("Data Collected to Database".equals(cmd));
		}else if("Choose Domain".equals(cmd)){
			doChooseDomain();
		}else if("Choose Cases".equals(cmd)){
			doChooseCases();
		}else if("Configure Database".equals(cmd)){
			doConfigDatabase();
		}
		pcs.firePropertyChange(new PropertyChangeEvent(this,"CHANGE", null, null));
	}

	public boolean doConfigDatabase() {
		if(protocol == null){
			protocol = new DatabaseProtocolModule();
		}
		
		ProtocolSelector prefs = new ProtocolSelector();
		prefs.setHideDefeaultConfiguration(true);
		prefs.setProtocolModule(protocol);

		
		if(prefs.showDialog(getComponent())){
			ProtocolModule m = prefs.getProtocolModule();
			if(m.isConnected()){
				protocol = m;
			}else{
				protocol = null;
				JOptionPane.showMessageDialog(getComponent(),"Could not connect to selected Protocol Module","Error",JOptionPane.ERROR_MESSAGE);
			}
			return true;
		}
		return false;
	}

	public void doUserManager(){
		// disable user management on default database
		if(isDeafultDatabase()){
			JOptionPane.showMessageDialog(getComponent(),"User Management for Default Database Configuration is Disabled","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// load user manager
		if(protocol != null){
			// load user manager
			UserManager pm = new UserManager();
			pm.setProtocolModule(protocol);
			JDialog  d= pm.createDialog(getComponent());
			UIHelper.centerWindow(JOptionPane.getFrameForComponent(getComponent()),d);
			d.setVisible(true);
			pm.load();
		}
	}
	
	private void doChooseCases() {
		if(sequenceBuilder == null){
			sequenceBuilder = new SequenceBuilder(Names.getDefaultExpertModule());
		}
		
		// load from whatever is in the textbox
		if(caseList.getText().length() > 0){
			sequenceBuilder.load(caseList.getText().trim());
		}
		
		// set condition
		sequenceBuilder.setCondition(condition.getText());
			
		
		//sequenceBuilder.setCondition(condition.getText().trim());
		int r = JOptionPane.showConfirmDialog(getComponent(),sequenceBuilder.getComponent(),
				"Create List of Cases",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			sequenceBuilder.doSave();
			if(sequenceBuilder.getFile() != null)
				caseList.setText(sequenceBuilder.getFile().getAbsolutePath());
		}
		
	}

	private void doChooseDomain() {
		if(domainSelector == null){
			domainSelector = new DomainSelectorPanel(Names.getDefaultExpertModule());
			domainSelector.setOwner(JOptionPane.getFrameForComponent(getComponent()));
		}
		domainSelector.showChooserDialog();
		if(domainSelector.isSelected())
			domain.setText(""+domainSelector.getSelectedObject());
		
	}

	public String getDescription() {
		return "This panel gives you an ability to quickly create a prebuilt system configuration.";
	}

	public String getName() {
		return "Select Default Configuration";
	}
	
	public String getShortName() {
		return "Defaults";
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
		
		// set other fields
		condition.setText(getProperties().getProperty("tutor.condition"));
		domain.setText(getProperties().getProperty("tutor.domain"));
		caseList.setText(getProperties().getProperty("tutor.case.sequence"));
				
		// select interface
		String in = ""+getProperties().getProperty("tutor.st.interface.module");
		for(Enumeration<AbstractButton> e = tutorGroup.getElements();e.hasMoreElements();){
			AbstractButton b = e.nextElement();
			ConfigOptions op = tutorConfigMap.get(b.getText());
			if(op != null && op.config.containsKey("tutor.st.interface.module")){
				if(op.config.get("tutor.st.interface.module").equals(in)){
					b.doClick();
					break;
				}
			}
		}
	
		// select curriculum
		String pd = ""+getProperties().getProperty("tutor.pedagogic.module");
		for(Enumeration<AbstractButton> e = curriculumGroup.getElements();e.hasMoreElements();){
			AbstractButton b = e.nextElement();
			ConfigOptions op = curriculumConfigMap.get(b.getText());
			if(op != null && op.config.containsKey("tutor.pedagogic.module")){
				if(op.config.get("tutor.pedagogic.module").equals(pd)){
					b.doClick();
					break;
				}
			}
		}
		
		// select feedback
		String fd = ""+getProperties().getProperty("HelpManager.feedback.mode");
		for(Enumeration<AbstractButton> e = feedbackGroup.getElements();e.hasMoreElements();){
			AbstractButton b = e.nextElement();
			ConfigOptions op = feedbackConfigMap.get(b.getText());
			if(op != null && op.config.containsKey("HelpManager.feedback.mode")){
				if(op.config.get("HelpManager.feedback.mode").equals(fd)){
					b.doClick();
					break;
				}
			}
		}
		
		// select databas curriculum
		String db = ""+getProperties().getProperty("tutor.protocol.module");
		for(Enumeration<AbstractButton> e = databaseGroup.getElements();e.hasMoreElements();){
			AbstractButton b = e.nextElement();
			ConfigOptions op = databaseConfigMap.get(b.getText());
			if(op != null && op.config.containsKey("tutor.protocol.module")){
				if(op.config.get("tutor.protocol.module").equals(db)){
					b.doClick();
					break;
				}
			}
		}
		
		// set database paramter
		if(configDB.isEnabled() && getProperties().containsKey("DatabaseProtocolModule.protocol.url")){
			protocol = new DatabaseProtocolModule();
			protocol.getDefaultConfiguration().setProperty("protocol.driver",getProperties().getProperty("DatabaseProtocolModule.protocol.driver"));
			protocol.getDefaultConfiguration().setProperty("protocol.url",getProperties().getProperty("DatabaseProtocolModule.protocol.url"));
			protocol.getDefaultConfiguration().setProperty("protocol.username",getProperties().getProperty("DatabaseProtocolModule.protocol.username"));
			protocol.getDefaultConfiguration().setProperty("protocol.password",getProperties().getProperty("DatabaseProtocolModule.protocol.password"));
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	public void revert() {
		properties = null;
		protocol = null;
		init();
		load();
	}

	public void setProperties(Properties map) {
		for(JRadioButton r: reset)
			r.doClick();
		
		// load all other properties
		for(Object key: getProperties().keySet()){
			if(map.containsKey(key))
				getProperties().put(key,map.get(key));
		}
		
		// go over properties
		String [] props = new String []{"tutor.st.interface.module","tutor.pedagogic.module","HelpManager.feedback.mode","tutor.protocol.module"};
		ButtonGroup [] groups = new ButtonGroup [] {tutorGroup,curriculumGroup,feedbackGroup,databaseGroup};
		Map<String,ConfigOptions> [] configs = new Map [] {tutorConfigMap,curriculumConfigMap,feedbackConfigMap,databaseConfigMap};
		
		// select interface and modify its respective properties
		for(int i=0;i<props.length;i++){
			String in = ""+getProperties().getProperty(props[i]);
			for(Enumeration<AbstractButton> e = groups[i].getElements();e.hasMoreElements();){
				AbstractButton b = e.nextElement();
				ConfigOptions op = configs[i].get(b.getText());
				if(op != null && op.config.containsKey(props[i])){
					if(op.config.get(props[i]).equals(in)){
						for(Object key: op.config.keySet()){
							if(map.containsKey(key))
								op.config.put(key,map.get(key));
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * load defaults
	 * @param is
	 * @throws IOException
	 */
    private void loadDefaults(InputStream is) throws IOException {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	String line,field = null;
    	StringBuffer buffer = new StringBuffer();
    	Pattern pt = Pattern.compile("\\[([\\w\\.\\-]+)\\]");
    	Map<String,String> map = new LinkedHashMap<String,String>();
    	while((line = reader.readLine()) != null){
    		line = line.trim();
    		// skip comments
    		if(line.startsWith("#"))
    			continue;
    		// extract headers
    		Matcher mt = pt.matcher(line);
    		if(mt.matches()){
    			// save previous field
    			if(field != null){
    				map.put(field,buffer.toString());
    				buffer = new StringBuffer();
    			}
    			field = mt.group(1);
    		}else{
    			buffer.append(line+"\n");
    		}
    	}
    	// finish the last item
    	if(field != null && buffer.length() > 0){
    		map.put(field,buffer.toString());
    	}
    	reader.close();
    	// now process the resulting map
    	
    	// load default configuration 
    	if(map.containsKey(DEFAULT_TAG)){
    		getProperties().putAll(TextHelper.getProperties(map.get(DEFAULT_TAG)));
    	}
    	
    	// get the resto fo the options
    	for(String key: map.keySet()){
    		if(DEFAULT_TAG.equals(key))
    			continue;
    		ConfigOptions op = new ConfigOptions(TextHelper.getProperties(map.get(key)));
    		if(key.startsWith(CURRICULUM_OPTION_TAG))
    			curriculumConfigMap.put(op.name,op);
    		else if(key.startsWith(INTERFACE_OPTION_TAG))
    			tutorConfigMap.put(op.name,op);
    		else if(key.startsWith(FEEDBACK_OPTION_TAG))
    			feedbackConfigMap.put(op.name,op);
    		else if(key.startsWith(DATABASE_OPTION_TAG))
    			databaseConfigMap.put(op.name,op);
    	}
    }
}
