package edu.pitt.dbmi.tutor.builder.protocol;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.protocol.DatabaseProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.FileProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.TextHelper;

public class ProtocolSelector extends JPanel implements ItemListener, ActionListener {
	private static final String [] PROTOCOLS = new String [] {"Database Protocol","File Protocol"};
	private static final String [] DRIVERS = new String [] {"com.mysql.jdbc.Driver","oracle.jdbc.driver.OracleDriver","de.simplicit.vjdbc.VirtualDriver","org.postgresql.Driver"};
	private static Map<String,String> URLS;
	public static ConfigProperties DEFAULT_DATABASE_PROPERTIES = (ConfigProperties) (new DatabaseProtocolModule()).getDefaultConfiguration();;
	
	static {
		URLS = new HashMap<String, String>();
		URLS.put(DRIVERS[0],"jdbc:mysql://<server>/<database>");
		URLS.put(DRIVERS[1],"jdbc:oracle:thin:@<server>:1521:<database>");
		URLS.put(DRIVERS[2],"jdbc:vjdbc:servlet:http://<server>/vjdbc/vjdbc#<database>");
		URLS.put(DRIVERS[3],"jdbc:postgresql://<server>/<database>");
		
	}
	
	private JComboBox protocolChooser;
	private JCheckBox queryInputs;
	private JPanel filePanel,databasePanel,mainPanel;
	private JTextField protocolURL,protocolUser,protocolDirectory;
	private JPasswordField protocolPassword;
	private JComboBox protocolDriver;
	private File lastFile;
	private ProtocolModule protocol;
	private int protocolHash;
	private boolean hidden;
	
	
	
	/**
	 * create a gui
	 */
	public ProtocolSelector(){
		super();
		setLayout(new BorderLayout());
		
		// initialize UI
		protocolChooser = new JComboBox(PROTOCOLS);
		protocolChooser.addItemListener(this);
		protocolChooser.setBorder(new TitledBorder("Protocol Type"));
		add(protocolChooser,BorderLayout.NORTH);
		
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBorder(new CompoundBorder(new TitledBorder("Protocol Settings"),new EmptyBorder(5,20,20,20)));
		mainPanel.add(getDatabasePanel(),BorderLayout.CENTER);
		mainPanel.setPreferredSize(new Dimension(380,300));
		add(mainPanel,BorderLayout.CENTER);
	}
	
	public void setHideDefeaultConfiguration(boolean h){
		hidden = h;
	}
	
	private JPanel getFilePanel(){
		if(filePanel == null){
			filePanel = new JPanel();
			filePanel.setLayout(new BoxLayout(filePanel,BoxLayout.Y_AXIS));
			filePanel.add(createLabel("Directory "));
			
			protocolDirectory = new JTextField(30);
			protocolDirectory.setAlignmentX(LEFT_ALIGNMENT);
			protocolDirectory.setMaximumSize(new Dimension(350,15));
			filePanel.add(protocolDirectory);
			
			JButton browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("browse");
			browse.setAlignmentX(LEFT_ALIGNMENT);
			filePanel.add(browse);
			filePanel.add(Box.createVerticalGlue());
			
		}
		return filePanel;
	}
	
	private JPanel getDatabasePanel(){
		if(databasePanel == null){
			databasePanel = new JPanel();
			databasePanel.setLayout(new BoxLayout(databasePanel,BoxLayout.Y_AXIS));
			
			databasePanel.add(createLabel("Database Driver"));
			protocolDriver = new JComboBox(DRIVERS);
			protocolDriver.setAlignmentX(LEFT_ALIGNMENT);
			protocolDriver.addItemListener(this);
			protocolDriver.setEditable(true);
			databasePanel.add(protocolDriver);
			
			databasePanel.add(createLabel("Database URL"));
			protocolURL = new JTextField(30);
			protocolURL.setText(URLS.get(protocolDriver.getSelectedItem()));
			protocolURL.setAlignmentX(LEFT_ALIGNMENT);
			databasePanel.add(protocolURL);
			
			databasePanel.add(createLabel("Username"));
			protocolUser = new JTextField();
			protocolUser.setAlignmentX(LEFT_ALIGNMENT);
			databasePanel.add(protocolUser);
			
			databasePanel.add(createLabel("Password"));
			protocolPassword = new JPasswordField();
			protocolPassword.setAlignmentX(LEFT_ALIGNMENT);
			databasePanel.add(protocolPassword);
			
			JButton auth = new JButton("Test Connection");
			auth.addActionListener(this);
			auth.setActionCommand("authenticate");
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.setBorder(new EmptyBorder(10,0,0,0));
			p.add(auth,BorderLayout.CENTER);
			p.setAlignmentX(LEFT_ALIGNMENT);
			databasePanel.add(p);
			
			queryInputs = new JCheckBox("Query Input Table Data",false);
			queryInputs.setToolTipText("<html>If query input tables is checked, ProtocolManager will query data<br>" +
										"from input tables of respective events if input field is unavailable.<br>" +
										"This means that query time will increase, however input data from the old protocol data<br>" +
										"will be available.");
			
			databasePanel.add(queryInputs);
		}
		return databasePanel;
	}
	
	private JLabel createLabel(String name){
		JLabel l = new JLabel(name);
		l.setAlignmentX(LEFT_ALIGNMENT);
		l.setBorder(new EmptyBorder(10,0,0,0));
		return l;
	}
	
	private void switchProtocol(String protocol){
		//DATABASE
		if(PROTOCOLS[0].equals(protocol)){
			mainPanel.remove(getFilePanel());
			mainPanel.add(getDatabasePanel(),BorderLayout.CENTER);
			protocolChooser.setSelectedIndex(0);
		//FILE	
		}else if(PROTOCOLS[1].equals(protocol)){
			mainPanel.remove(getDatabasePanel());
			mainPanel.add(getFilePanel(),BorderLayout.CENTER);
			protocolChooser.setSelectedIndex(1);
		}
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	public void itemStateChanged(ItemEvent e) {
		if(e.getStateChange() == ItemEvent.SELECTED){
			if(e.getSource() == protocolChooser){
				switchProtocol(protocolChooser.getSelectedItem().toString());
			}else if(e.getSource() == protocolDriver){
				String drv = (String) protocolDriver.getSelectedItem();
				if(drv != null && URLS.containsKey(drv))
					protocolURL.setText(URLS.get(drv));
			}
		}
		
	}


	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("browse".equals(cmd)){
			JFileChooser fc = new JFileChooser(lastFile);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if(JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)){
				lastFile = fc.getSelectedFile();
				protocolDirectory.setText(lastFile.getAbsolutePath());
			}
		}else if("authenticate".equals(cmd)){
			doAuthenticate();
		}
		
	}
	
	public void doAuthenticate(){
		ProtocolModule m = getProtocolModule();
		if(m.isConnected()){
			JOptionPane.showMessageDialog(this,"Connection Successfull!",
										 "Success",JOptionPane.INFORMATION_MESSAGE);
		}else{
			JOptionPane.showMessageDialog(this,"Could not connect to "+protocolChooser.getSelectedItem(),
										"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isAuthenticated(){
		return false;
	}
	
	/**
	 * set default protocol module
	 * @param module
	 */
	public void setProtocolModule(ProtocolModule module){
		this.protocol = module;
		
		if(module instanceof DatabaseProtocolModule){
			// make sure that the default configuration of protocol is the real thing
			resetDefaultProperty(module,"protocol.driver");
			resetDefaultProperty(module,"protocol.url");
			resetDefaultProperty(module,"protocol.username");
			resetDefaultProperty(module,"protocol.password");
		
			// switch protocol
			switchProtocol(PROTOCOLS[0]);
			
			// set fields to given module
			protocolDriver.setSelectedItem(protocol.getDefaultConfiguration().getProperty("protocol.driver"));
			protocolURL.setText(getProtocolURL());
			protocolUser.setText(getProtocolUser());
			protocolPassword.setText(protocol.getDefaultConfiguration().getProperty("protocol.password"));
			
		}else if(module instanceof FileProtocolModule){
			// make sure that the default configuration of protocol is the real thing
			resetDefaultProperty(module,"protocol.directory");
			switchProtocol(PROTOCOLS[1]);
			protocolDirectory.setText(protocol.getDefaultConfiguration().getProperty("protocol.directory"));
		}
		
		protocolHash = calculateProtocolHash();
	}
	
	private String getProtocolURL(){
		String url = protocol.getDefaultConfiguration().getProperty("protocol.url");
		// if hidden and equals to default
		if(hidden && url.equals(DEFAULT_DATABASE_PROPERTIES.getProperty("protocol.url"))){
			url = url.replaceAll(".","•");
		}
		return url;
	}
	private String getProtocolUser(){
		String user = protocol.getDefaultConfiguration().getProperty("protocol.username");
		// if hidden and equals to default
		if(hidden && user.equals(DEFAULT_DATABASE_PROPERTIES.getProperty("protocol.username"))){
			user = user.replaceAll(".","•");
		}
		return user;
	}
	
	private String getTextURL(){
		String url = protocolURL.getText();
		if(url.contains("****"))
			url = DEFAULT_DATABASE_PROPERTIES.getProperty("protocol.url");
		return url;
	}
	
	private String getTextUser(){
		String url = protocolUser.getText();
		if(url.contains("****"))
			url = DEFAULT_DATABASE_PROPERTIES.getProperty("protocol.username");
		return url;
	}
	
	/**
	 * make sure that default property is set to whatever the global property
	 * is set and then change the default
	 * @param m
	 * @param property
	 */
	private void resetDefaultProperty(TutorModule m, String property){
		String key = m.getClass().getSimpleName()+"."+property;
		String val = Config.getProperty(key);
		if(!TextHelper.isEmpty(val)){
			m.getDefaultConfiguration().setProperty(property,val);
			Config.getProperties().remove(key);
		}
	}
	
	/**
	 * calculate protocol hash
	 * @return
	 */
	private int calculateProtocolHash(){
		int hash = 0;
		// db
		if(PROTOCOLS[0].equals(protocolChooser.getSelectedItem())){
			hash += protocolDriver.getSelectedItem().hashCode();
			hash += protocolURL.getText().hashCode();
			hash += protocolUser.getText().hashCode();
			hash += new String(protocolPassword.getPassword()).hashCode();
			hash += new Boolean(queryInputs.isSelected()).hashCode();
		// file	
		}else if(PROTOCOLS[1].equals(protocolChooser.getSelectedItem())){
			hash += protocolDirectory.getText().hashCode();
		}
		return hash;
	}
	
	
	/**
	 * get protocol module settings
	 * @return
	 */
	public ProtocolModule getProtocolModule(){
		// if nothing changed return same old module
		if(protocolHash == calculateProtocolHash())
			return protocol;
		
		// else create new protocol module and set it
		ProtocolModule pm = null;
		if(PROTOCOLS[0].equals(protocolChooser.getSelectedItem())){
			// init new database protocol module
			pm = new DatabaseProtocolModule();
			
			// make sure we don't have global settings that can overwrite this
			Config.getProperties().remove(pm.getClass().getSimpleName()+".protocol.driver");
			Config.getProperties().remove(pm.getClass().getSimpleName()+".protocol.url");
			Config.getProperties().remove(pm.getClass().getSimpleName()+".protocol.username");
			Config.getProperties().remove(pm.getClass().getSimpleName()+".protocol.password");
			
			// set to default to new values
			pm.getDefaultConfiguration().setProperty("protocol.driver",protocolDriver.getSelectedItem().toString());
			pm.getDefaultConfiguration().setProperty("protocol.url",getTextURL());
			pm.getDefaultConfiguration().setProperty("protocol.username",getTextUser());
			pm.getDefaultConfiguration().setProperty("protocol.password",new String(protocolPassword.getPassword()));
			pm.getDefaultConfiguration().setProperty("protocol.query.input.tables",""+queryInputs.isSelected());
		}else if(PROTOCOLS[1].equals(protocolChooser.getSelectedItem())){
			pm = new FileProtocolModule();
			Config.getProperties().remove(pm.getClass().getSimpleName()+".protocol.directory");
			pm.getDefaultConfiguration().setProperty("protocol.directory",protocolDirectory.getText());
		}
		
		// reset protocol module
		setProtocolModule(pm);
		
		// now return it
		return pm;
	}
	
	
	/**
	 * show this preference panel
	 * @param parent
	 * @return
	 */
	public boolean showDialog(Component parent){
		int r = JOptionPane.showConfirmDialog(parent,this,"Protocol Properties",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		return r == JOptionPane.OK_OPTION;
	}
	
	public static void main(String [] args){
		
		ProtocolSelector selector = new ProtocolSelector();
		selector.setHideDefeaultConfiguration(true);
		//selector.setProtocolModule(defaultDatabase);
		selector.showDialog(null);
		System.exit(0);
	}


	
}
