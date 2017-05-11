package edu.pitt.dbmi.tutor.builder.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import edu.pitt.dbmi.tutor.builder.behavior.BehaviorBuilder;
import edu.pitt.dbmi.tutor.builder.help.HelpBuilder;
import edu.pitt.dbmi.tutor.builder.sequence.SequenceBuilder;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.feedback.DialogManager;
import edu.pitt.dbmi.tutor.modules.interfaces.ColorBookInterface;
import edu.pitt.dbmi.tutor.modules.interventions.Interventions;
import edu.pitt.dbmi.tutor.modules.interventions.MetaCognitiveSkillometer;
import edu.pitt.dbmi.tutor.modules.presentation.DynamicBook;
import edu.pitt.dbmi.tutor.modules.presentation.KnowledgeExplorer;
import edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.UIHelper;


/**
 * this class handles name lookups for different modules
 * @author tseytlin
 */
public class Names {
	private static boolean showAllProperties;
	private static ExpertModule expert;
	private static Map<String,String> nameMap = new HashMap<String,String>();
	private static List<String> excludedModules = new ArrayList<String>();
	private static List<String> otherModules = new ArrayList<String>();
	private static ConfigurationBuilder configuratioBuilder;
	
	static {
		// right now temporarily hard coded
		//TODO:
		excludedModules.add(DialogManager.class.getSimpleName());
		excludedModules.add(DynamicBook.class.getSimpleName());
		excludedModules.add(KnowledgeExplorer.class.getSimpleName());
		excludedModules.add(ColorBookInterface.class.getSimpleName());
		
		otherModules.addAll(excludedModules);
		otherModules.add(Interventions.class.getSimpleName());
		otherModules.add(MetaCognitiveSkillometer.class.getSimpleName());
		otherModules.add(FileProtocolModule.class.getSimpleName());
	}
	
	
	/**
	 * save instance of configuration builder
	 * @param b
	 */
	public static void setConfigurationBuilder(ConfigurationBuilder b){
		configuratioBuilder = b;
	}
	
	/**
	 * get default expert module
	 * @return
	 */
	public static ExpertModule getDefaultExpertModule(){
		if(expert == null){
			expert = new DomainExpertModule();
			expert.load();
		}
		return expert;
	}
	
	
	public static void setShowAllProperties(boolean showAllProperties) {
		Names.showAllProperties = showAllProperties;
	}

	
	public static boolean isShowAllProperties() {
		return showAllProperties;
	}

	/**
	 * get name for a given module class
	 * @param tutorModule
	 * @return
	 */
	public static String getName(Class tutorModule){
		if(nameMap.containsKey(tutorModule.getSimpleName()))
			return nameMap.get(tutorModule.getSimpleName());
		return tutorModule.getSimpleName().replaceAll("([a-z])([A-Z])","$1 $2");
	}
	
	
	/**
	 * get name for a given module class
	 * @param tutorModule
	 * @return
	 */
	public static String getName(TutorModule tutorModule){
		if(nameMap.containsKey(tutorModule.getClass().getSimpleName()))
			return nameMap.get(tutorModule.getClass().getSimpleName());
		return tutorModule.getName();
	}
	
	
	/**
	 * get name for given property name
	 * @param prefix
	 * @param tutorModule
	 * @return
	 */
	public static String getPropertyName(String prefix, Class tutorModule){
		return prefix+tutorModule.getSimpleName().replaceAll("([a-z])([A-Z])","$1.$2").toLowerCase();
	}
	
	
	/**
	 * pretty print description
	 * 
	 * @param text
	 * @return
	 */
	public static String getDescription(String text,int w, int h) {
		if(text == null)
			text = "&nbsp;<br>&nbsp;";
		return "<html><table width="+w+" height="+h+" cellpadding=10 bgcolor=\"#FFFFCC\">" + text + "</table></html>";
	}
	
	/**
	 * pretty print description
	 * 
	 * @param text
	 * @return
	 */
	public static String getDescription(String text,int w) {
		return "<html><table width="+w+" cellpadding=10 >" + text + "</table></html>";
	}
	
	
	/**
	 * create header panel for a given wizard panel
	 * @param w
	 * @return
	 */
	public static JPanel createHeader(WizardPanel w){
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setBorder(new EmptyBorder(new Insets(10, 10, 30, 10)));
		p.setLayout(new BorderLayout());
		JLabel wizardName = new JLabel(w.getName());
		Font f = wizardName.getFont();
		wizardName.setFont(wizardName.getFont().deriveFont(f.getSize()+10f));
		wizardName.setHorizontalAlignment(JLabel.CENTER);
		JLabel wizardDescritpion = new JLabel(Names.getDescription(w.getDescription(),700,300));
		wizardDescritpion.setHorizontalAlignment(JLabel.CENTER);
		wizardDescritpion.setFont(wizardName.getFont().deriveFont(Font.PLAIN,(float)f.getSize()));
		p.add(wizardName, BorderLayout.NORTH);
		p.add(wizardDescritpion, BorderLayout.CENTER);
		return p;
	}
	
	
	/**
	 * check whether module should not be listed along with its siblings in category
	 * @param name
	 * @return
	 */
	public static boolean isExcludedModule(String name){
		return excludedModules.contains(name);
	}
	
	/**
	 * check whether module should be included as other category
	 * @param name
	 * @return
	 */
	public static boolean isOtherModule(String name){
		return otherModules.contains(name);
	}
	
	
	/**
	 * get properties that should be shown in the configuration builder
	 * @param p
	 * @return
	 */
	public static ConfigProperties getProperties(ConfigProperties p){
		if(showAllProperties)
			return p;
		
		// copy only relevant properties
		ConfigProperties np = new ConfigProperties();
		for(Object key: p.keySet()){
			String com = p.getPropertyComment(""+key);
			if(com != null && com.startsWith("#")){
				np.put(key,p.get(key));
				np.setPropertyComment(""+key,com.replaceAll("#","").trim());
			}
		}
		return np;
	}
	
	
	/**
	 * some properties are global, that is while there is a local equivalent, 
	 * they are defined globally in config file
	 * @param key
	 * @return
	 */
	public static String getGlobalProperty(String key){
		if("tutor.case.sequence.location".equals(key))
			return "tutor.case.sequence";
		else if("tutor.behavior.location".equals(key))
			return "tutor.st.behavior";
		else if("tutor.help.location".equals(key))
			return "tutor.st.help";
		return key;
	}
	
	
	
	
	/**
	 * create a button that will lunch an editor or a browse button
	 * @param key
	 * @param value
	 * @return
	 */
	public static String getEditorLabel(String key){
		if("tutor.case.sequence.location".equals(key))
			return "Case List";
		else if("tutor.behavior.location".equals(key))
			return "Behavior File";
		else if("tutor.help.location".equals(key))
			return "Help File";
		return key.replaceAll("tutor.","").replaceAll("([a-z])\\.([a-z])","$1 $2");
	}
	
	
	/**
	 * show non modal dialog that will execute a given action
	 * @param c
	 * @param title
	 * @param action
	 * @return
	 */
	private static boolean showDialog(Component parent, Component c, String title){
		JOptionPane p = new JOptionPane(c,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
		JDialog d = p.createDialog(JOptionPane.getFrameForComponent(parent), title);
		d.setModal(false);
		d.setVisible(true);
		while(d.isShowing()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return Integer.valueOf(JOptionPane.OK_OPTION).equals(p.getValue());
	}
	
	/**
	 * create a button that will lunch an editor or a browse button
	 * @param key
	 * @param value
	 * @return
	 */
	public static JButton getEditorButton(String key, JTextField value){
		final JTextField textField = value;
		JButton bt = null;
		
		// check 
		if("tutor.case.sequence.location".equals(key)){
			bt = new JButton("Editor");
			bt.setIcon(UIHelper.getIcon(Config.getProperty("icon.menu.sequence"),16));
			bt.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable(){
						public void run(){
							SequenceBuilder sequenceBuilder = new SequenceBuilder(getDefaultExpertModule());
							
							// load from whatever is in the textbox
							if(textField.getText().length() > 0){
								sequenceBuilder.load(textField.getText().trim());
							}
							if(configuratioBuilder != null)
								sequenceBuilder.setCondition(configuratioBuilder.getConfiguration().getProperty("tutor.condition"));
							
							if(showDialog(textField,sequenceBuilder.getComponent(),"Sequence Editor")){
								sequenceBuilder.doSave();
								if(sequenceBuilder.getFile() != null)
									textField.setText(sequenceBuilder.getFile().getAbsolutePath());
							}
						}
					}).start();
				}
			});
		}else if("tutor.behavior.location".equals(key)){
			bt = new JButton("Editor");
			bt.setIcon(UIHelper.getIcon(Config.getProperty("icon.menu.preview"),16));
			bt.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable(){
						public void run(){
							BehaviorBuilder behaviorBuilder = new BehaviorBuilder();
							
							// load from whatever is in the textbox
							if(textField.getText().length() > 0){
								behaviorBuilder.load(textField.getText().trim());
							}
							if(showDialog(textField,behaviorBuilder.getComponent(),"Behavior Editor")){
								behaviorBuilder.doSave();
								if(behaviorBuilder.getFile() != null)
									textField.setText(behaviorBuilder.getFile().getAbsolutePath());
							}
						}
					}).start();
					
				}
			});
		}else if("tutor.help.location".equals(key)){
			bt = new JButton("Editor");
			bt.setIcon(UIHelper.getIcon(Config.getProperty("icon.menu.help"),16));
			bt.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable(){
						public void run(){
							HelpBuilder helpManager  = new HelpBuilder();
							if(configuratioBuilder != null)
								helpManager.setConfiguration(configuratioBuilder.getConfiguration());
							
							// load from whatever is in the textbox
							if(textField.getText().length() > 0){
								helpManager.load(textField.getText().trim());
							}
							if(showDialog(textField,helpManager.getComponent(),"Tutor Help Editor")){
								helpManager.doSave();
								if(helpManager.getFile() != null)
									textField.setText(helpManager.getFile().getAbsolutePath());
							}		
						}
					}).start();
							
				}
			});
		}else {
			bt = new JButton("Browse");
			bt.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser jf = new JFileChooser();
					jf.setSelectedFile(new File(textField.getText()));
					if(JFileChooser.APPROVE_OPTION == jf.showOpenDialog(JOptionPane.getFrameForComponent(textField))){
						textField.setText(jf.getSelectedFile().getAbsolutePath());
					}
				}
			});
		}
		return bt;
	}
}
