package edu.pitt.dbmi.tutor.builder.config.panel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.pitt.dbmi.tutor.builder.config.Names;
import edu.pitt.dbmi.tutor.builder.config.WizardPanel;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.ConfigProperties;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class TutorUIPanel implements WizardPanel, ListSelectionListener {
	private ConfigProperties properties;
	private JPanel component,panel;
	private JList uiSelector;
	private JTextField windowSize;
	private LayoutPanel uiLayout;
	private Map<String,LayoutPanel> layoutPanelMap;
	private Map<String,ImageIcon> screenshotMap;
	private boolean visited;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private KeyListener keyListener = new KeyAdapter() {
		public void keyTyped(KeyEvent e) {
			pcs.firePropertyChange(new PropertyChangeEvent(TutorUIPanel.this,"CHANGE", null, null));
		}
	};
	private Set<String> currentSelectionMap;
	
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

	public static final String [] INTERACTIVE_MODULES = new String [] {
			PresentationModule.class.getSimpleName(),
			InterfaceModule.class.getSimpleName(),
			FeedbackModule.class.getSimpleName()
	};
	
	
	/**
	 * get screenshot map
	 * @return
	 */
	public Map<String,ImageIcon> getScreenshotMap(){
		if(screenshotMap == null){
			screenshotMap = new HashMap<String, ImageIcon>();
			for(String s : Config.getRegisteredModules()){
				if(!s.matches(".*(interfaces|presentation|feedback).*"))
					continue;
				Object obj;
				try {
					obj = Class.forName(s).newInstance();
					if(obj instanceof PresentationModule && (!screenshotMap.containsKey(PresentationModule.class.getSimpleName()) || currentSelectionMap.contains(s))){
						if(((InteractiveTutorModule)obj).getScreenshot() != null)
							screenshotMap.put(PresentationModule.class.getSimpleName(),((InteractiveTutorModule)obj).getScreenshot());
					}else if(obj instanceof FeedbackModule && (!screenshotMap.containsKey(FeedbackModule.class.getSimpleName()) || currentSelectionMap.contains(s))){
						if(((InteractiveTutorModule)obj).getScreenshot() != null)
							screenshotMap.put(FeedbackModule.class.getSimpleName(),((InteractiveTutorModule)obj).getScreenshot());
					}else if(obj instanceof InterfaceModule && (!screenshotMap.containsKey(InterfaceModule.class.getSimpleName()) || currentSelectionMap.contains(s))){
						if(((InteractiveTutorModule)obj).getScreenshot() != null)
							screenshotMap.put(InterfaceModule.class.getSimpleName(),((InteractiveTutorModule)obj).getScreenshot());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}
		return screenshotMap;
	}
	
	
	public Component getComponent() {
		if(component == null){
			component = new JPanel();
			component.setLayout(new BorderLayout());
			component.setBackground(Color.white);
			
			uiSelector = new JList(createUILayouts());
			uiSelector.setVisibleRowCount(4);
			uiSelector.addListSelectionListener(this);
			windowSize = new JTextField(10);
			windowSize.setToolTipText("<html>Enter default window size of the tutor.<br>" +
					"Example: <b>1024</b> x <b>768</b> for exact dimension, <b>max</b> for maximized window.<br>" +
					"Leave blank to use default.");
			windowSize.addKeyListener(keyListener);
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new FlowLayout());
			p.add(new JLabel("Tutor Window Size  "));
			p.add(windowSize);
			
			panel = new JPanel();
			panel.setOpaque(false);
			panel.setLayout(new BorderLayout());
			
			JPanel u = new JPanel();
			u.setOpaque(false);
			u.setLayout(new BorderLayout());
			u.setBorder(new CompoundBorder(new TitledBorder("Interface Selection"),new EmptyBorder(new Insets(15,15,15,15))));
			u.setPreferredSize(new Dimension(500,500));
			u.add(panel,BorderLayout.CENTER);
			u.add(p,BorderLayout.SOUTH);
			
			JScrollPane scroll = new JScrollPane(uiSelector);
			scroll.setBorder(new TitledBorder("Layout"));
			scroll.setOpaque(false);
			component.add(Names.createHeader(this),BorderLayout.NORTH);
			component.add(scroll,BorderLayout.WEST);
			component.add(u,BorderLayout.CENTER);
			
			// load defaults
			load();
			
		}
		return component;
	}


	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			uiLayout = getLayoutPanel((UILayout)uiSelector.getSelectedValue());
			if(uiLayout == null)
				return;
			panel.removeAll();
			panel.add(uiLayout,BorderLayout.CENTER);
			panel.revalidate();
			panel.repaint();
			pcs.firePropertyChange(new PropertyChangeEvent(this,"CHANGE", null, null));
		}
	}
	
	/**
	 * get layout map
	 * @param layout
	 * @return
	 */
	private LayoutPanel getLayoutPanel(UILayout  layout){
		if(layout == null)
			return null;
		
		if(layoutPanelMap == null)
			layoutPanelMap = new HashMap<String, LayoutPanel>();
		
		if(layoutPanelMap.containsKey(layout.getLayoutString()))
			return layoutPanelMap.get(layout.getLayoutString());
		
		LayoutPanel lp = new LayoutPanel(layout.getLayoutString());
		layoutPanelMap.put(layout.getLayoutString(),lp);
		return lp;
	}
	
	
	public String getDescription() {
		return "You can customize the user interface of the SlideTutor ITS by changing the layout of its basic modules with respect to each other. " +
				"To do that, simply select a layout template from the selection list on the left-hand side, then select individual " +
				"components from the pulldown menus inside of the interface selection panel.";
	}

	public String getName() {
		return "User Interface Layout";
	}
	
	public String getShortName() {
		return "Layout";
	}

	public Properties getProperties() {
		if(properties == null){
			properties = new ConfigProperties();
		
			// initialize properties that this 
			properties.put("tutor.size","1024x768");
			properties.put("tutor.st.layout.main","H|split|InterfaceModule|1|.55");
			properties.put("tutor.st.layout.split","V|PresentationModule|FeedbackModule|1");
						
		}
		return properties;
	}

	
	
	/**
	 * load values
	 */
	public void load(){
		if(component == null)
			return;
		
		setVisited(true);
		windowSize.setText(getProperties().getProperty("tutor.size"));
		// figure out offset
		for(int i=0;i<uiSelector.getModel().getSize();i++){
			UILayout l = (UILayout) uiSelector.getModel().getElementAt(i);
			if(l.matches(getProperties())){
				uiSelector.setSelectedIndex(i);
				break;
			}
		}
		// now that we selected the right layout
		if(uiLayout != null){
			uiLayout.setProperties(getProperties());
		}
	}
	
	public void setProperties(Properties map) {
		for(Object key: getProperties().keySet()){
			if(map.containsKey(key))
				getProperties().put(key,map.get(key));
		}
	
		// preset some defaults for the sake of screenshots
		currentSelectionMap = new HashSet<String>();
		for(String key: Arrays.asList("tutor.st.interface.module","tutor.st.feedback.module","tutor.st.presentation.module")){
			if(map.containsKey(key))
				currentSelectionMap.add(""+map.get(key));
		}
		screenshotMap = null;
	}

	public void apply() {
		if(component == null)
			return;
		
		// set window size
		getProperties().setProperty("tutor.size",windowSize.getText());
		if(uiLayout != null){
			getProperties().putAll(uiLayout.getProperties());
		}
	}

	public void revert() {
		properties = null;
		load();
	}

	/**
	 * get layout objects
	 * @return
	 */
	private Vector createUILayouts(){
		Vector vec = new Vector();
		vec.add(new UILayout("_"));
		
		vec.add(new UILayout("H|_|_"));
		vec.add(new UILayout("V|_|_"));
	
		vec.add(new UILayout("H|split|_\nV|_|_"));
		vec.add(new UILayout("H|_|split\nV|_|_"));
		
		vec.add(new UILayout("V|split|_\nH|_|_"));
		vec.add(new UILayout("V|_|split\nH|_|_"));
		
		vec.add(new UILayout("V|split|split2\nH|_|_\nH|_|_"));
		vec.add(new UILayout("H|split|split2\nV|_|_\nV|_|_"));
	
		return vec;
	}
	
	/**
	 * clean layout string
	 * @param s
	 * @return
	 */
	private static String cleanLayoutString(String s){
		if(s == null)
			return null;
		
		// if no | then this is all we need
		if(s.indexOf('|') < 0)
			return s;
		// else 
		int x,count=0;;
		for(x=0;x<s.length();x++){
			if(s.charAt(x) == '|')
				count++;
			if(count > 2)
				break;
		}
		return s.substring(0,x);
	}
	
	/**
	 * return a list of modules in property string
	 * @param s
	 * @return
	 */
	private static List<String> extractLayoutModules(String s){
		List<String> list = new ArrayList<String>();
		if(s != null){
			for(String p: s.split("\\|")){
				if(!p.matches("(H|V|split2?|[\\.\\d]+)")){
					list.add(p);
				}
			}
		}
		return list;
	}
	
	
	/**
	 * return split params
	 * @param split
	 * @return
	 */
	private static String getSplitParams(JSplitPane split){
		if(split == null)
			return "";
		
		// get resize weight
		String rs = "";
		Dimension d = split.getSize();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		if(split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT){
			rs = "|"+nf.format(split.getDividerLocation() / d.getWidth());
		}else if(split.getOrientation() == JSplitPane.VERTICAL_SPLIT){
			rs = "|"+nf.format(split.getDividerLocation() / d.getHeight());
		}
		
		int x1 = getMinOffset(split.getTopComponent());
		int x2 = getMinOffset(split.getBottomComponent());
		String rw = "|0";
		if(x1 == x2)
			rw = "|.5";
		if(x1 < x2)
			rw = "|1";
		
		return rw+rs ;
	}
	
	
	/**
	 * get minimum offset of the component in hierarchy
	 * @param c
	 * @return
	 */
	private static int getMinOffset(Component c){
		if(c instanceof JSplitPane){
			JSplitPane s = (JSplitPane) c;
			int x1 = getMinOffset(s.getTopComponent());
			int x2 = getMinOffset(s.getBottomComponent());
			return Math.min(x1,x2);
		}else if(c instanceof InterfacePanel){
			InterfacePanel i1 = (InterfacePanel) c;
			int x;
			for(x=0;x<INTERACTIVE_MODULES.length && !INTERACTIVE_MODULES[x].equals(i1.getSelectedModule());x++);
			return x;
		}
		return -1;
	}
	
	
	/**
	 * display layout graphics
	 * @author tseytlin
	 *
	 */
	private class UILayout implements Icon {
		private String layout;
		private Stroke stroke = new BasicStroke(4);
		public UILayout(String l){
			layout = l;
		}
		public boolean matches(Properties p){
			//"tutor.st.layout.main","H|split|InterfaceModule|1|.55"
			//"tutor.st.layout.split","V|PresentationModule|FeedbackModule|1"
			String [] l = layout.split("\n");
			if(l.length > 0){
				String l1 = cleanLayoutString(p.getProperty("tutor.st.layout.main"));
				l1 = l1.replaceAll("[A-Z][A-Za-z]+","_");
				// first line is a match
				if(l[0].equals(l1)){
					// check second line
					if(l.length > 1){
						String l2 = cleanLayoutString(p.getProperty("tutor.st.layout.split"));
						if(l2 != null){
							l2 = l2.replaceAll("[A-Z][A-Za-z]+","_");
							// second line is a match
							if(l[1].equals(l2)){
								// check third line
								if(l.length > 2){
									String l3 = cleanLayoutString(p.getProperty("tutor.st.layout.split2"));
									if(l3 != null){
										l3 = l2.replaceAll("[A-Z][A-Za-z]+","_");
										// thrid line is a match
										if(l[2].equals(l3)){
											return true;
										}
									}
								}else{
									//we only had two lines and they did match
									return true;
								}
							}
						}
					}else{
						//we only had one line and it did match
						return true;
					}
						
				}
			}
			// default no match
			return false;
		}
		
		public int getIconHeight() {
			return 100;
		}
		public int getIconWidth() {
			return 100;
		}
		public String getLayoutString(){
			return new String(layout);
		}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			int offs = 10;
			int w = getIconWidth() - offs*2;
			int h = getIconHeight() - offs*2;
			
			((Graphics2D)g).setStroke(stroke);
			g.setColor(Color.black);
			g.drawRect(x+offs, y+offs, w, h);
			
			// now check out layout
			String [] l = layout.split("\n");
			if(l.length > 0){
				if(l[0].startsWith("H")){
					g.drawLine(offs+x+w/2,offs+y,offs+x+w/2,offs+y+h);
				}else if(l[0].startsWith("V")){
					g.drawLine(offs+x,offs+y+h/2,offs+x+w,offs+y+h/2);
				}
			}
			if(l.length > 1){
				int o = l[0].split("\\|")[1].equals("split")?0:w/2;
				if(l[1].startsWith("H")){
					g.drawLine(offs+x+w/2,offs+o+y,offs+x+w/2,offs+o+y+h/2);
				}else if(l[1].startsWith("V")){
					g.drawLine(offs+o+x,offs+y+h/2,offs+o+x+w/2,offs+y+h/2);
				}
			}
			if(l.length > 2){
				int o = l[0].split("\\|")[2].equals("split")?0:w/2;
				if(l[2].startsWith("H")){
					g.drawLine(offs+x+w/2,offs+o+y,offs+x+w/2,offs+o+y+h/2);
				}else if(l[2].startsWith("V")){
					g.drawLine(offs+o+x,offs+y+h/2,offs+o+x+w/2,offs+y+h/2);
				}
			}
			
		}
		
	}
	
	/**
	 * class represents layout panel
	 * @author tseytlin
	 */
	private class LayoutPanel extends JPanel {
		private String layout;
		private List<InterfacePanel> panels; 
		private JSplitPane main,split1,split2;    
		
		
		public LayoutPanel(String l){
			layout = l;
			panels = new ArrayList<InterfacePanel>();
			createUI();
		}
		
		public void setProperties(Properties p){
			String l1 = p.getProperty("tutor.st.layout.main");
			String l2 = p.getProperty("tutor.st.layout.split");
			String l3 = p.getProperty("tutor.st.layout.split2");
			
			// get properties
			List<String> comps = new ArrayList<String>();
			comps.addAll(extractLayoutModules(l1));
			comps.addAll(extractLayoutModules(l2));
			comps.addAll(extractLayoutModules(l3));
			
			// now reset panels assuming panel list is similar to component list
			//if(panels.size() == comps.size()){
			for(int i=0;i<comps.size() && i<panels.size();i++){
				panels.get(i).setSelectedModule(comps.get(i));
			}
			//}
			
			// figure out split weights and locations
			if(l1 != null){
				try{
					String [] lp = l1.split("\\|");
					if(lp.length > 3)
						main.setResizeWeight(Double.parseDouble(lp[3]));
					if(lp.length > 4)
						main.setDividerLocation(Double.parseDouble(lp[4]));
				}catch(Exception ex){}
			}
			
			if(l2 != null){
				try{
					String [] lp = l2.split("\\|");
					if(lp.length > 3)
						split1.setResizeWeight(Double.parseDouble(lp[3]));
					if(lp.length > 4)
						split1.setDividerLocation(Double.parseDouble(lp[4]));
				}catch(Exception ex){}
			}
			
			if(l3 != null){
				try{
					String [] lp = l3.split("\\|");
					if(lp.length > 3)
						split2.setResizeWeight(Double.parseDouble(lp[3]));
					if(lp.length > 4)
						split2.setDividerLocation(Double.parseDouble(lp[4]));
				}catch(Exception ex){}
			}
			
		}
		
		public Properties getProperties(){
			StringBuffer lb = new StringBuffer();
			String layout = getLayoutString();
			for(int i=0,j=0;i<layout.length();i++){
				if(layout.charAt(i) == '_'){
					lb.append(panels.get(j++).getSelectedModule());
				}else{
					lb.append(layout.charAt(i));
				}
			}
			String [] l = lb.toString().split("\n");
			ConfigProperties p = new ConfigProperties();
			if(l.length > 0){
				p.setProperty("tutor.st.layout.main",l[0]+getSplitParams(main));
				if(l.length > 1){
					p.setProperty("tutor.st.layout.split",l[1]+getSplitParams(split1));
					if(l.length > 2){
						p.setProperty("tutor.st.layout.split2", l[2]+getSplitParams(split2));
					}
				}
			}
			return p;
		}
		
		/**
		 * get layout string
		 * @return
		 */
		public String getLayoutString(){
			return layout;
		}
		
		/**
		 * get interface panels
		 * @return
		 */
		public List<InterfacePanel> getInterfacePanels(){
			return panels;
		}
		
		
		private void createUI(){
			setLayout(new BorderLayout());
			setBorder(new BevelBorder(BevelBorder.RAISED));
			setBackground(Color.white);
			
			String [] l = layout.split("\n");
			// take care of main level split
			if(l.length > 0){
				if(l[0].startsWith("H") || l[0].startsWith("V")){
					main = new JSplitPane(l[0].startsWith("H")?JSplitPane.HORIZONTAL_SPLIT:JSplitPane.VERTICAL_SPLIT);
					main.setResizeWeight(0.5);
					if(l[0].split("\\|")[1].equals("_")){
						InterfacePanel ip = new InterfacePanel(this);
						main.setLeftComponent(ip);
						panels.add(ip);
					}
					if(l[0].split("\\|")[2].equals("_")){
						InterfacePanel ip = new InterfacePanel(this);
						main.setRightComponent(ip);
						panels.add(ip);
					}
					add(main,BorderLayout.CENTER);
				}else{
					InterfacePanel ip = new InterfacePanel(this);
					panels.add(ip);
					add(ip,BorderLayout.CENTER);
				}
			}
			// take care second level split
			if(l.length > 1){
				JComponent comp = null;
				if(l[1].startsWith("H") || l[1].startsWith("V")){
					split1 = new JSplitPane(l[1].startsWith("H")?JSplitPane.HORIZONTAL_SPLIT:JSplitPane.VERTICAL_SPLIT);
					split1.setResizeWeight(0.5);
					if(l[1].split("\\|")[1].equals("_")){
						InterfacePanel ip = new InterfacePanel(this);
						split1.setLeftComponent(ip);
						panels.add(ip);
					}
					if(l[1].split("\\|")[2].equals("_")){
						InterfacePanel ip = new InterfacePanel(this);
						split1.setRightComponent(ip);
						panels.add(ip);
					}
					comp = split1;
				}else{
					InterfacePanel ip = new InterfacePanel(this);
					panels.add(ip);
					comp = ip;
				}
				
				// add to main split
				if(l[0].split("\\|")[1].equals("split")){
					main.setTopComponent(comp);
				}else if(l[0].split("\\|")[2].equals("split")){
					main.setBottomComponent(comp);
				}
			}
			
			// take care of thrird level split
			if(l.length > 2){
				JComponent comp = null;
				if(l[2].startsWith("H") || l[2].startsWith("V")){
					split2 = new JSplitPane(l[2].startsWith("H")?JSplitPane.HORIZONTAL_SPLIT:JSplitPane.VERTICAL_SPLIT);
					split2.setResizeWeight(0.5);
					if(l[2].split("\\|")[2].equals("_")){
						InterfacePanel ip = new InterfacePanel(this);
						split2.setLeftComponent(ip);
						panels.add(ip);
					}
					if(l[2].split("\\|")[2].equals("_")){
						InterfacePanel ip = new InterfacePanel(this);
						split2.setRightComponent(ip);
						panels.add(ip);
					}
					comp = split2;
				}else{
					InterfacePanel ip = new InterfacePanel(this);
					panels.add(ip);
					comp = ip;
				}
				
				// add to main split
				if(l[0].split("\\|")[1].equals("split2")){
					main.setTopComponent(comp);
				}else if(l[0].split("\\|")[2].equals("split2")){
					main.setBottomComponent(comp);
				}
			}
		}
	}
	
	/**
	 * this class represents an interface panel
	 * @author tseytlin
	 */
	private class InterfacePanel extends JPanel implements ItemListener {
		private LayoutPanel layout;
		private JComboBox selector;
		private JPanel panel;
		
		public InterfacePanel(LayoutPanel layout){
			this.layout = layout;
			setPreferredSize(new Dimension(250,250));
			setMinimumSize(new Dimension(100,100));
			setBackground(Color.yellow);
			setLayout(new BorderLayout());
			selector = new JComboBox(INTERACTIVE_MODULES);
			selector.setBorder(new CompoundBorder(new LineBorder(Color.black,2),new LineBorder(Color.white,8)));
			selector.addItemListener(this);
			panel = new JPanel(){
				public void paintComponent(Graphics g) {
					String key = ""+selector.getSelectedItem();
					// draw the rset of the stuff
					super.paintComponent(g);
					
					if(getScreenshotMap().containsKey(key)){
						Dimension sz = panel.getSize();
						Image img =  getScreenshotMap().get(key).getImage();
						int w = img.getWidth(null);
						int h = img.getHeight(null);
						
						if(w > sz.width){
							h = h * sz.width / w;
							w = sz.width;
						}
						if(h > sz.height){
							w = w * sz.height / h;
							h = sz.height;
						}
						
						
						int x = (sz.width - w) / 2;
						int y = (sz.height - h) / 2;
						
						g.drawImage(img, x, y, w+x, h+y,0,0,img.getWidth(null),img.getHeight(null),null);
					}
				}
			};
			//panel.setOpaque(false);
			panel.setBackground(Color.white);
			panel.setBorder(new EmptyBorder(new Insets(20,0,0,0)));
			panel.setLayout(new BorderLayout());
			panel.add(selector,BorderLayout.NORTH);
			add(panel,BorderLayout.CENTER);
			setBackground(Color.white);
			
			// pick unused module
			List<String> available = new ArrayList<String>(Arrays.asList(INTERACTIVE_MODULES));
			for(InterfacePanel i : layout.getInterfacePanels()){
				available.remove(i.getSelectedModule());
			}
			if(!available.isEmpty())
				selector.setSelectedItem(available.get(0));
		}
		
		public String getSelectedModule(){
			return ""+selector.getSelectedItem();
		}
		
		public void setSelectedModule(String s){
			selector.setSelectedItem(s);
		}
		
		public void itemStateChanged(ItemEvent e) {
			String s = ""+selector.getSelectedItem();
			if(PresentationModule.class.getSimpleName().equals(s)){
				setupPanel(s,new Color(255,255,230));
			}else if(FeedbackModule.class.getSimpleName().equals(s)){
				setupPanel(s,new Color(230,255,230));
			}else if(InterfaceModule.class.getSimpleName().equals(s)){
				setupPanel(s,new Color(230,230,255));
			}
			pcs.firePropertyChange(new PropertyChangeEvent(this,"CHANGE", null, null));
		}
		
		
	
		/**
		 * set panel
		 * @param key
		 * @param color
		 */
		private void setupPanel(String key, Color color){
			panel.setBackground(color);
			panel.repaint();
			/*
			if(panel.getComponentCount() > 1)
				panel.remove(1);
			
			if(getScreenshotMap().containsKey(key)){
				Dimension sz = panel.getSize();
				if(sz.width == 0)
					sz = new Dimension(250,250);
				Dimension d = new Dimension(sz.width-20,sz.height-80);
				panel.add(UIHelper.getImageComponent(getScreenshotMap().get(key).getImage(),d),BorderLayout.CENTER);
			}
			panel.revalidate();
			panel.repaint();
			*/
		}
	}
}
