package edu.pitt.dbmi.tutor.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;

import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.model.TutorModule;

/**
 * Keeps track of global configuration details
 * @author Eugene Tseytlin
 */
public class Config {
	public static final String DEFAULT_USER = "guest";
	private static final String INTERFACE_CONFIG = "/resources/Interface.properties";
	private static final String MODULE_REGISTRY  = "/resources/module.registry";
	private static final String SCREENSHOT_DIR  = "/resources/screenshots/";
	private static final String HELP_DIR  = "/resources/help/";
	private static Logger log = Logger.getLogger("TutorBuilder");
	private static Properties config = new ConfigProperties();
	private static Frame mainFrame;
	private static String location;
	private static File workingDirectory;
	
	/**
	 * load configuration
	 */
	static {
		config = new Properties();
		try{
			InputStream is = Config.class.getResourceAsStream(INTERFACE_CONFIG);
			config.load(is);
			is.close();
		}catch(Exception ex){
			log.severe(INTERFACE_CONFIG+" read error: "+ex.getMessage());
		}
	}
	
	/**
	 * use system look and feel
	 */	
	public static void setLookAndFeel(){
		//String os = System.getProperty("os.name").toLowerCase();
		//if(os.contains("windows") || os.contains("mac"))
		//	setSystemLookAndFeel();
		//else
		setJavaLookAndFeel();
	}
	
	
	
	/**
	 * use system look and feel
	 */	
	public static void setSystemLookAndFeel(){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			//e.printStackTrace();
			getLogger().severe(TextHelper.getErrorMessage(e));
		}
	}
	
	
	/**
	 * use system look and feel
	 */	
	public static void setJavaLookAndFeel(){
		try {
			UIManager.setLookAndFeel(new MetalLookAndFeel());
		} catch (Exception e) {
			getLogger().severe(TextHelper.getErrorMessage(e));
		}
	}
	
	/**
	 * get location of the configuration file
	 * @return
	 */
	public static String getConfigLocation() {
		return location;
	}
	
	/**
	 * get location of the configuration file
	 * @return
	 */
	public static void setConfigLocation(String l) {
		location = l;
	}
	
	
	/**
	 * get screenshot for a given module
	 * @param module
	 * @return image icon object or null if not available
	 */
	public static ImageIcon getScreenshot(Class module){
		URL url = Config.class.getResource(SCREENSHOT_DIR+module.getSimpleName()+".jpg");
		return (url == null)?null: new ImageIcon(url);
	}
	
	/**
	 * get screenshot for a given module
	 * @param module
	 * @return image icon object or null if not available
	 */
	public static URL getManual(Class module){
		return Config.class.getResource(HELP_DIR+module.getSimpleName()+".html");
	}
	
	
	/**
	 * read default properties for given module
	 * @param module
	 * @return
	 */
	public static ConfigProperties getDefaultConfiguration(Class module){
		ConfigProperties defaultConfig = new ConfigProperties();
		String conf = Config.getDefaultConfigPath(module);
		try{
			defaultConfig.load(module.getResourceAsStream(conf));
		}catch(Exception ex){
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
		}
		return defaultConfig;
	}
	
	
	/**
	 * get working directory for this session
	 * @return
	 */
	public static File getWorkingDirectory(){
		if(workingDirectory == null){
			workingDirectory = new File(System.getProperty("user.home"),".SlideTutor");
			if(!workingDirectory.exists())
				workingDirectory.mkdir();
		}
		return workingDirectory;
	}

	/**
	 * load configuration options from the file
	 * @param file
	 * @throws IOException
	 */
	public static void load(File file) throws IOException{
		location = file.getAbsolutePath();
		InputStream is = new FileInputStream(file);
		config.load(is);
		is.close();
	}
	
	/**
	 * load configuration options from url
	 * @param file
	 * @throws IOException
	 */
	public static void load(URL url) throws IOException{
		location = ""+url;
		InputStream is = url.openStream();
		config.load(is);
		is.close();
	}
	
	/**
	 * load configuration options from file or url
	 * @param file
	 * @throws IOException
	 */
	public static void load(String loc) throws IOException{
		String url = OntologyHelper.stripURLQuery(loc);
		
		File file = new File(url);
		if(file.exists()){
			load(file);
		}else if(UIHelper.isURL(url)){
			load(new URL(url));
		}else
			throw new IOException("Invalid configuration file path or url "+url);
		
		// add extra parameters
		config.putAll(OntologyHelper.getURLQuery(loc));
		location = loc;
	}
	
	
	/**
	 * get all properties currently loaded
	 * @return
	 */
	public static Properties getProperties(){
		return config;
	}
	
	/**
	 * get all properties that contain a given key  
	 * @return
	 */
	public static Properties getProperties(String key){
		Properties p = new Properties();
		for(Object k: config.keySet()){
			if(k.toString().contains(key)){
				p.put(k,config.get(k));
			}
		}
		return p;
	}
	
	
	/**
	 * get default path of the config file for given component
	 * @param cls
	 * @return
	 */
	public static String getDefaultConfigPath(Class cls){
		return "/resources/defaults/"+cls.getSimpleName()+".properties";
	}
	
	
	/**
	 * get default logger
	 * @return
	 */
	public static Logger getLogger(){
		return log;
	}
	
	/**
	 * get property for given key
	 */
	public static String getProperty(String key){
		return config.getProperty(key,"").trim();
	}
	
	/**
	 * get property for given key
	 */
	public static void setProperty(String key,String value){
		config.setProperty(key,value);
	}
	
	/**
	 * set multiple properties
	 * @param p
	 */
	public static void setProperties(Properties p){
		config.putAll(p);
	}
	
	
	/**
	 * get property for given key
	 */
	public static String getProperty(TutorModule t, String k){
		String key = k;
		Properties p = null;
		if(t != null){
			key = t.getClass().getSimpleName()+"."+k;
			p = t.getDefaultConfiguration();
		}
		return config.getProperty(key,(p != null)?p.getProperty(k,""):"").trim();
	}
	
	/**
	 * check if property is set
	 * @param t
	 * @param k
	 * @return
	 */
	public static boolean hasProperty(TutorModule t, String k){
		return !TextHelper.isEmpty(getProperty(t,k));
	}
	
	
	/**
	 * get property for given key as a file
	 * NOTE: take care of file separator characters
	 */
	public static String getFileProperty(TutorModule t, String k){
		String file = getProperty(t, k);
		return file.replace('/',File.separatorChar);
	}
	
	/**
	 * get icon property
	 * @param key
	 * @return
	 */
	public static Icon getIconProperty(String key){
		return UIHelper.getIcon(getProperty(key));
	}
	
	/**
	 * get icon property
	 * @param key
	 * @return
	 */
	public static Icon getIconProperty(TutorModule tm, String key){
		return UIHelper.getIcon(getProperty(tm,key));
	}
	
	
	/**
	 * load module
	 * @param name
	 * @return
	 */
	public static TutorModule loadTutorModule(String key) throws Exception{
		String name = getProperty(key);
		if(name.length() > 0){
			Object obj = Class.forName(name).newInstance();
			if(obj instanceof TutorModule)
				return (TutorModule) obj;
			
		}
		return null;
	}
	
	/**
	 * load module
	 * @param name
	 * @return
	 */
	public static List<TutorModule> loadTutorModules(String key) throws Exception{
		List<TutorModule> list = new ArrayList<TutorModule>();
		String modules = getProperty(key);
		if(!TextHelper.isEmpty(modules)){
			for(String name : modules.split(",")){
				Object obj = Class.forName(name.trim()).newInstance();
				if(obj instanceof TutorModule)
					list.add((TutorModule)obj);
			}
		}
		return list;
	}
	
	
	/**
	 * get property for given key
	 */
	public static boolean getBooleanProperty(TutorModule t, String k){
		return parseBoolean(getProperty(t,k));
	}
	
	/**
	 * get property for given key
	 */
	public static boolean getBooleanProperty(String k){
		return parseBoolean(getProperty(k));
	}
	
	/**
	 * get property for given key
	 */
	public static int getIntegerProperty(TutorModule t, String k){
		return parseInteger(getProperty(t,k),k);
	}
	
	/**
	 * get property for given key
	 */
	public static float getFloatProperty(TutorModule t, String k){
		return parseFloat(getProperty(t,k),k);
	}
	
	/**
	 * get property for given key
	 */
	public static Dimension getDimensionProperty(TutorModule t, String k){
		return parseDimension(getProperty(t, k),k);
	}
	
	
	/**
	 * get property for given key
	 */
	public static Color getColorProperty(TutorModule t, String k){
		// get value
		return parseColor(getProperty(t, k),k);
	}

	/**
	 * get property for given key
	 */
	public static URL getURLProperty(TutorModule t, String k){
		try{
			return new URL(getProperty(t,k));
		}catch(Exception ex){
			getLogger().severe("ERROR: could parse URL from property "+k);
			return null;
		}
	}
	
	/**
	 * get property for given key
	 */
	public static String [] getListProperty(String k){
		return getListProperty(null,k);
	}
	
	/**
	 * get property for given key
	 */
	public static String [] getListProperty(TutorModule t, String k){
		return parseList(getProperty(t,k));
		
	}
	
	public static Frame getMainFrame() {
		return mainFrame;
	}

	public static void setMainFrame(Frame mainFrame) {
		Config.mainFrame = mainFrame;
	}
	
	
	/**
	 * get registered modules for interspection
	 * @return
	 */
	public static String [] getRegisteredModules(){
		try{
			String str = TextHelper.getText(Config.class.getResourceAsStream(MODULE_REGISTRY));
			return str.split("\n");
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return new String [0];
	}
	
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static int parseInteger(String value,String k){
		int x = 0;
		try{
			x = Integer.parseInt(value);
		}catch(NumberFormatException ex){
			log.severe("can't parse value "+value+" of property "+k+" Cause: "+ex.getMessage());
		}
		return x;
	}
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static float parseFloat(String value,String k){
		float x = 0;
		try{
			x = Float.parseFloat(value);
		}catch(NumberFormatException ex){
			log.severe("can't parse value "+value+" of property "+k+" Cause: "+ex.getMessage());
		}
		return x;
	}
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static boolean parseBoolean(String value){
		return "true".equalsIgnoreCase(value);
	}
	
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static Color parseColor(String value,String k){
		 // Find the field and value of colorName
       try{
       	Field field = Class.forName("java.awt.Color").getField(value.toUpperCase());
           return (Color) field.get(null);
       }catch(Exception ex){
       	try{
	        	int i = 0;
	    		int [] rgb = new int [3];
	    		// split any string into potential rgb
	    		for(String n : value.split("[^\\d]")){
	    			if(n.length() > 0 && i < rgb.length){
	    				 int x = Integer.parseInt(n);
	    				 if(x < 256)
	    					 rgb[i++] = x;
	    			}
	    		}
	    		return new Color(rgb[0],rgb[1],rgb[2]);
       	}catch(Exception exe){
       		log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
       	}
       }
       
       //default color
		return Color.white;	
	}
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static Dimension parseDimension(String value,String k){
		// check for max value for size
		if(value.toLowerCase().startsWith("max")){
			return Toolkit.getDefaultToolkit().getScreenSize();
		}
		try{
        	int i = 0;
    		int [] wh = new int [2];
    	
    		// split any string into potential rgb
    		for(String n : value.split("[^\\d]")){
    			if(n.length() > 0 && i < wh.length){
    				 wh[i++] = Integer.parseInt(n);
    			}
    		}
    		
    		return new Dimension(wh[0],wh[1]);
    	}catch(Exception exe){
    		log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
    	}
		return new Dimension(0,0);
	}
	

	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static String [] parseList(String value){
		if(value == null || value.trim().length() == 0)
			return new String [0];
		
		//trim
		value = value.trim();
		
		// strip parenthesis
		if(value.startsWith("["))
			value = value.substring(1);
		if(value.endsWith("]"))
			value = value.substring(0,value.length()-1);
		return value.trim().split("\\s*[,;\\|]\\s*");
	}
	
	
	/**
	 * get username of logged in user
	 * @return
	 */
	public static String getUsername(){
		return config.getProperty("username",DEFAULT_USER).trim();
	}
	
	/**
	 * get username of logged in user
	 * @return
	 */
	public static String getPassword(){
		return config.getProperty("password",DEFAULT_USER).trim();
	}
	
	/**
	 * get username of logged in user
	 * @return
	 */
	public static String getCondition(){
		return config.getProperty("tutor.condition","").trim();
	}
	
	
	/**
	 * display modal "red alert" message that needs to be handled
	 * by a research assistant. If red alert is disabled in configuration
	 * it does nothing.
	 * @param content of the message string displayed
	 * @return true if "retry" option was selected, "false" if user hit ignore
	 */
	public static boolean raiseRedAlert(String content){
		//return ignore if not set
		if(!getBooleanProperty("red.alert.enabled"))
			return false;
		
		// create dialog
		final JDialog d = new JDialog(getMainFrame());
		d.setModal(true);
		d.setTitle("Critical Error");
		d.setAlwaysOnTop(true);
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.getContentPane().setLayout(new BorderLayout());
		
		// instructions
		String MSG = "A critical system error was encountered.\n" +
					 "Please notify research assistant to resolve this situation.";
		JTextArea instructions = new JTextArea(3,35);
		instructions.setText(MSG);
		instructions.setLineWrap(true);
		instructions.setWrapStyleWord(true);
		instructions.setEditable(false);
		instructions.setBackground(new Color(255,255,230));
		instructions.setForeground(Color.red);
		
		// define final UI components
		final JButton retry = new JButton("Retry");
		final JButton ignore = new JButton("Ignore");
		final JPasswordField pass = new JPasswordField();
		
		// define listener
		final StringBuffer outcome = new StringBuffer();
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(pass.equals(e.getSource())){
					if(Constants.DEBUG_PASSWORD.equals(new String(pass.getPassword()))){
						retry.setEnabled(true);
						ignore.setEnabled(true);
					}
				}else{
					outcome.append(e.getActionCommand());
					d.dispose();
				}
			}
		};
		pass.addActionListener(listener);
		
		// instructor panel
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(instructions,BorderLayout.CENTER);
		p.add(pass,BorderLayout.SOUTH);
		p.setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5),new BevelBorder(BevelBorder.RAISED)));
		
		// button panel
		JPanel bp = new JPanel();
		bp.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		// define buttons
		retry.setEnabled(false);
		retry.setActionCommand("retry");
		retry.addActionListener(listener);
		bp.add(retry);
		
		ignore.setEnabled(false);
		ignore.setActionCommand("ignore");
		ignore.addActionListener(listener);
		bp.add(ignore);
		
		// write content
		JLabel lbl = new JLabel(content);
		lbl.setBorder(new EmptyBorder(10,5,10,5));
		
		d.getContentPane().add(lbl,BorderLayout.NORTH);
		d.getContentPane().add(p,BorderLayout.CENTER);
		d.getContentPane().add(bp,BorderLayout.SOUTH);
		d.pack();
		UIHelper.centerWindow(d);
		d.setVisible(true);
		return !"ignore".equals(outcome.toString());
	}
	
	
	/**
	 * get input stream from a given location
	 * location could is normally a property of some sort
	 * it can be either file, url or pointer to JAR file
	 * @param location
	 * @return
	 */
	public static InputStream getInputStream(String location){
		File f = new File(location);
		InputStream in = null;
		if(TextHelper.isEmpty(location))
			return null;
		try{
			if(f.exists()){
				in = new FileInputStream(f);
			}else if(UIHelper.isURL(location)){
				URL url = new URL(location);
				in = url.openStream();
			}else {
				in = Config.class.getResourceAsStream(location);
			}
		}catch(Exception ex){
			getLogger().severe(TextHelper.getErrorMessage(ex));
		}
		return in;
	}
	
	
	public static void main(String [] args){
		System.out.println(raiseRedAlert("The system encountered a problem persisting protocol data."));
	}
}
