package edu.pitt.dbmi.tutor.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.SlideEntry;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.ViewerFactory;

/**
 * varies useful UI methods
 * 
 * @author tseytlin
 * 
 */
public class UIHelper {
	/** 
	 * This seems to be the only way to antialias fonts in labels.
	 */
	public static class Label extends JLabel{
		public Label(String text){
			super(text);	
		}
		public void paintComponent(Graphics g){
			Graphics2D g2 = (Graphics2D)g;
			
			// Enable antialiasing for text
		 	g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
		}
	}
	
	
	/**
	 * Jlist w/ easy add/remove fatures
	 * @author tseytlin
	 *
	 */
	public static class List extends JList {
		private DefaultListModel model;
		private JScrollPane scroll;
		public List(){
			super(new DefaultListModel());
			model = (DefaultListModel) getModel();
		}
		public void clear(){
			model.removeAllElements();
			revalidate();
		}
		public void add(Object o){
			model.addElement(o);
			revalidate();
		}
		public boolean remove(Object o){
			boolean b = model.removeElement(o);
			revalidate();
			return b;
		}
		public void add(Collection l){
			for(Object o: l){
				model.addElement(o);
			}
			revalidate();
		}
		public void set(Collection l){
			model.removeAllElements();
			add(l);
		}
		public JScrollPane getScrollPane(){
			if(scroll == null)
				scroll = new JScrollPane(this);
			return scroll;
		}
	}
	
	/**
	 * Jlist w/ easy add/remove fatures
	 * @author tseytlin
	 *
	 */
	public static class ComboBox extends JComboBox {
		private DefaultComboBoxModel model;
		public ComboBox(){
			super(new DefaultComboBoxModel());
			model = (DefaultComboBoxModel) getModel();
		}
		public void clear(){
			model.removeAllElements();
			revalidate();
		}
		public void add(Object o){
			model.addElement(o);
			revalidate();
		}
		public void remove(Object o){
			model.removeElement(o);
			revalidate();
		}
		public void add(Collection l){
			for(Object o: l){
				model.addElement(o);
			}
			revalidate();
		}
		public void set(Collection l){
			model.removeAllElements();
			add(l);
		}
	}
	
	 /**
	  * get index for given location, -1 if no cell selected
	  * @param list
	  * @param l
	  * @return
	  */
	 public static int getIndexForLocation(JList list, Point l){
		 Rectangle r = list.getCellBounds(0,list.getModel().getSize()-1);
		 if(r != null && r.contains(l))
			 return list.locationToIndex(l);
		 return -1;
	 }
	 
	
	/**
	 * only allow integer input
	 * @author Eugene Tseytlin
	 */
	public static class IntegerDocument extends DefaultStyledDocument{
		public void insertString(int i, String s, AttributeSet a) throws BadLocationException {
			if(s.matches("[0-9]+"))
				super.insertString(i, s, a);
		}
	}
	
	/**
	 * only allow integer input
	 * @author Eugene Tseytlin
	 */
	public static class DecimalDocument extends DefaultStyledDocument{
		public void insertString(int i, String s, AttributeSet a) throws BadLocationException {
			if(s.matches("[0-9\\.]+"))
				super.insertString(i, s, a);
		}
	}
	
	
	
	private static final String[] colors = { "BLACK", "BLUE", "CYAN", "GRAY",
			"GREEN", "MAGENTA", "ORANGE", "PINK", "RED", "WHITE", "YELLOW" };

	/**
	 * sleep for a time
	 * 
	 * @param time
	 */
	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
		}
	}
	
	/**
	 * Put current thread to sleep in minutes
	 * Same as Thread sleep
	 */
	public static void sleep4min(double time) {
		sleep((long) (time * 60000));
	}

	/**
	 * Put current thread to sleep in seconds
	 * Same as Thread sleep
	 */
	public static void sleep4sec(double time) {
		sleep((long) (time * 1000));
	}
	
	
	/**
	 * does parent contain child
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean contains(Container parent, Component child){
		if(child == null || parent == null)
			return false;
		
		if(child.getParent() == parent)
			return true;
		
		return contains(parent,child.getParent());
	}
	
	
	/**
	 * get property for given key
	 */
	public static Color getColor(String value) {
		// Find the field and value of colorName
		try {
			Field field = Class.forName("java.awt.Color").getField(
					value.toUpperCase());
			return (Color) field.get(null);
		} catch (Exception ex) {
			try {
				int i = 0;
				int[] rgb = new int[3];
				// split any string into potential rgb
				for (String n : value.split("[^\\d]")) {
					if (n.length() > 0 && i < rgb.length) {
						int x = Integer.parseInt(n);
						if (x < 256)
							rgb[i++] = x;
					}
				}
				return new Color(rgb[0], rgb[1], rgb[2]);
			} catch (Exception exe) {
				Config.getLogger().severe(
						"can't parse color value " + value + " Cause: "
								+ exe.getMessage());
			}
		}

		// default color
		return Color.white;
	}

	/**
	 * convert color to string
	 * 
	 * @param c
	 * @return
	 */
	public static String getColor(Color c) {
		try {
			for (int i = 0; i < colors.length; i++) {
				Field field = Class.forName("java.awt.Color").getField(
						colors[i]);
				if (c.equals(field.get(null)))
					return colors[i];
			}
		} catch (Exception ex) {
		}
		return c.getRed() + " " + c.getGreen() + " " + c.getBlue();
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, Icon icon,
			int m, ActionListener listener) {
		return createButton(name, tip, icon, m, false, listener);
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, Icon icon,
			int m, boolean showname, ActionListener listener) {
		JButton n = new JButton();
		if (showname)
			n.setText(name);
		if (icon != null)
			n.setIcon(icon);
		else
			n.setText(name);
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		if (m > -1)
			n.setMnemonic(m);
		return n;
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, Icon icon,
			ActionListener listener) {
		return createButton(name, tip, icon, -1, listener);
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JMenuItem createMenuItem(String name, String tip, Icon icon,ActionListener listener) {
		return createMenuItem(name, tip, icon, -1, listener);
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JMenuItem createMenuItem(String name, String tip, Icon icon, int m, ActionListener listener) {
		JMenuItem n = new JMenuItem(tip);
		if (icon != null)
			n.setIcon(icon);
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		if (m > -1)
			n.setMnemonic(m);
		return n;
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JCheckBoxMenuItem createCheckboxMenuItem(String name,
			String tip, Icon icon, ActionListener listener) {
		return createCheckboxMenuItem(name, tip, icon, false, listener);
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JCheckBoxMenuItem createCheckboxMenuItem(String name,
			String tip, Icon icon, boolean t, ActionListener listener) {
		JCheckBoxMenuItem n = new JCheckBoxMenuItem(tip);
		if (icon != null)
			n.setIcon(icon);
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		n.setSelected(t);
		return n;
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JToggleButton createToggleButton(String name, String tip,
			Icon icon, int m, boolean showname,ActionListener listener) {
		JToggleButton n = new JToggleButton(icon);
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		if(showname)
			n.setText(name);
		if (m > -1)
			n.setMnemonic(m);
		return n;
	}

	/**
	 * create button with na
	 * 
	 * @param name
	 *            /command
	 * @param tool
	 *            tip
	 * @param icon
	 * @return
	 */
	public static JToggleButton createToggleButton(String name, String tip,
			Icon icon, ActionListener listener) {
		return createToggleButton(name, tip, icon, -1,false, listener);
	}

	/**
	 * get icon from given property
	 */
	public static Icon getIcon(TutorModule tm, String key) {
		return getIcon(Config.getProperty(tm, key));
	}

	/**
	 * get icon from given property
	 */
	public static Icon getIcon(TutorModule tm, String key, int size) {
		return getIcon(Config.getProperty(tm, key), size);
	}

	/**
	 * get icon for given path
	 */
	public static Icon getIcon(String path) {
		try {
			return new ImageIcon(UIHelper.class.getResource(path));
		} catch (Exception ex) {
			// ex.printStackTrace();
			Config.getLogger().severe("Can't find icon " + path);
		}
		return null;
	}

	/**
	 * get icon for given path
	 */
	public static Icon getIcon(String path, int size) {
		// try to find if there is an appropriate size available
		ImageIcon icon = (ImageIcon) getIcon(path);
		if(icon == null)
			return null;
		
		if (icon.getIconWidth() == size)
			return icon;
		return new ImageIcon(icon.getImage().getScaledInstance(size, size,Image.SCALE_SMOOTH));
	}

	/**
	 * get cursor from given property
	 */
	public static Cursor getCursor(TutorModule tm, String key, Point p) {
		return getCursor(Config.getProperty(tm, key), p);
	}

	/**
	 * get custom cursor
	 * 
	 * @param path
	 * @param p
	 * @return
	 */
	public static Cursor getCursor(String path, Point p) {
		Image img = null;
		Toolkit tk = Toolkit.getDefaultToolkit();
		try {
			URL url = UIHelper.class.getResource(path);
			img = tk.getImage(url);
		} catch (Exception ex) {
			// ex.printStackTrace();
			Config.getLogger().severe("Can't find cursor image " + path);
			return null;
		}
		Cursor c = null;
		try {
			c = tk.createCustomCursor(img, p, "custom");
		} catch (IndexOutOfBoundsException e) {
			Config.getLogger()
					.severe(
							"Can't create cursor " + path + " Cause: "
									+ e.getMessage());
		}
		return c;
	}

	
	/**
	 * does this menu have menu item?
	 * @param menu
	 * @param str
	 * @return
	 */
	public static boolean hasMenuItem(JMenu menu, String str){
		for(Component c: menu.getMenuComponents()){
			if(c instanceof AbstractButton){
				if(((AbstractButton)c).getText().equals(str))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * does this toolbar have button?
	 * @param menu
	 * @param str
	 * @return
	 */
	public static boolean hasButton(JToolBar toolbar, String str){
		for(Component c: toolbar.getComponents()){
			if(c instanceof AbstractButton){
				AbstractButton b = (AbstractButton) c;
				if((""+b.getText()).equals(str) || (""+b.getActionCommand()).equals(str) )
					return true;
			}
		}
		return false;
	}
	
	/**
	 * does this menu have menu item?
	 * @param menu
	 * @param str
	 * @return
	 */
	public static AbstractButton getMenuItem(JMenu menu, String str){
		for(Component c: menu.getMenuComponents()){
			if(c instanceof AbstractButton){
				if(((AbstractButton)c).getText().contains(str))
					return (AbstractButton) c;
			}
		}
		return null;
	}
	
	/**
	 * does this toolbar have button?
	 * @param menu
	 * @param str
	 * @return
	 */
	public static AbstractButton getButton(JToolBar toolbar, String str){
		for(Component c: toolbar.getComponents()){
			if(c instanceof AbstractButton){
				AbstractButton b = (AbstractButton) c;
				if((""+b.getText()).contains(str) || (""+b.getActionCommand()).contains(str) )
					return b;
			}
		}
		return null;
	}
	
	
	/**
	 * toolbar that can handle change in orientation
	 * 
	 * @author Eugene Tseytlin
	 */
	public static class ToolBar extends JToolBar {
		private Map<AbstractButton, String> buttonText;

		/**
		 * listen for orientation change
		 */
		public void setOrientation(int x) {
			super.setOrientation(x);

			// init map
			if (buttonText == null)
				buttonText = new HashMap<AbstractButton, String>();

			if (x == JToolBar.VERTICAL) {
				// iterate through components
				for (Component c : getComponents()) {
					if (c instanceof AbstractButton) {
						AbstractButton b = (AbstractButton) c;
						String text = b.getText();
						if (text != null) {
							buttonText.put(b, text);
							b.setText(null);
						}
					}
				}
			} else {
				// iterate through components
				for (Component c : getComponents()) {
					if (c instanceof AbstractButton) {
						AbstractButton b = (AbstractButton) c;
						String text = buttonText.get(b);
						if (text != null) {
							b.setText(text);
						}
					}
				}
			}
			revalidate();
		}
	}

	/**
	 * Derive prettier version of a class name
	 * 
	 * @param name
	 * @return
	 */
	public static String getTextFromName(String name) {
		// strip prefix (if available)
		int i = name.indexOf(":");
		if (i > -1) {
			name = name.substring(i + 1);
		}

		// strip suffix
		// if(name.endsWith(OntologyHelper.WORD))
		// name = name.substring(0,name.length()-OntologyHelper.WORD.length());

		// possible lowercase values to make things look prettier
		if (!name.matches("[A-Z_\\-\\'0-9]+")
				&& !name.matches("[a-z][A-Z_\\-\\'0-9]+[a-z]*"))
			name = name.toLowerCase();

		// now replace all underscores with spaces
		return name.replaceAll("_", " ");
	}

	/**
	 * return difference between to points (a-b)
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static Point difference(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	/**
	 * get input stream for object
	 * 
	 * @param text
	 * @return
	 */
	public static InputStream toInputStream(Object text) {
		return new ByteArrayInputStream(text.toString().getBytes());
	}

	/**
	 * Center child window on top of parent window
	 * 
	 * @param Component
	 *            parent window
	 * @param Component
	 *            child window
	 */
	public static void centerWindow(Component parent, Component child) {
		try {
			Point p = parent.getLocationOnScreen();
			Dimension fs = parent.getSize();
			Dimension ds = child.getSize();
			if (p != null && fs != null && ds != null) {
				int x = p.x + (fs.width / 2) - (ds.width / 2);
				int y = p.y + (fs.height / 2) - (ds.height / 2);
				child.setLocation(x, y);
			}
		} catch (Exception ex) {
			// we don't care enough to center it to crash everything else
		}
	}

	/**
	 * Center child window on top of parent window
	 * 
	 * @param Component
	 *            parent window
	 * @param Component
	 *            child window
	 */
	public static void centerWindow(Component child) {
		try {
			Point p = new Point(0,0);
			Dimension fs = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension ds = child.getSize();
			if (p != null && fs != null && ds != null) {
				int x = p.x + (fs.width / 2) - (ds.width / 2);
				int y = p.y + (fs.height / 2) - (ds.height / 2);
				child.setLocation(x, y);
			}
		} catch (Exception ex) {
			// we don't care enough to center it to crash everything else
		}
	}
	
	/**
	 * get a non-system cursor from one of components children
	 * or return a system cursor for parent;
	 * @param c
	 * @return
	 */
	public static Cursor getChildCursor(Component c){
		Cursor cursor = c.getCursor();
		if(c instanceof Container){
			for(Component comp: ((Container)c).getComponents()){
				Cursor cur = getChildCursor(comp);
				if(!cur.equals(cursor)){
					cursor = cur;
					break;
				}
			}
		}
		return cursor;
	}
	
	/**
	 * get frame for component
	 * 
	 * @param c
	 * @return
	 */
	public static Window getWindow(Component c) {
		// return JOptionPane.getFrameForComponent(c);
		if (c == null)
			return null;
		if (c instanceof Window)
			return (Window) c;
		return getWindow(c.getParent());
	}

	/**
	 * This class is extension of JEditorPane with 2 changes 1) it sets content
	 * type to HTML 2) it has good looking anti-aliased fonts
	 * 
	 * @author tseytlin
	 */
	public static class HTMLPanel extends JEditorPane {
		private HTMLDocument doc;

		public HTMLPanel() {
			super();
			setContentType("text/html; charset=UTF-8");
			doc = (HTMLDocument) getDocument();
			doc.getStyleSheet().addRule("body { font-family: sans-serif;");
		}

		/**
		 * set font size
		 * 
		 * @param s
		 */
		public void setFontSize(int s) {
			doc.getStyleSheet().addRule(
					"body { font-family: sans-serif; font-size: " + s + ";");
		}

		// make antialiased text
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			// Enable antialiasing for text
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
		}

		/**
		 * Append text to panel
		 * 
		 * @param text
		 */
		public void append(String text) {
			int offset = getDocument().getEndPosition().getOffset();
			try {
				HTMLDocument doc = (HTMLDocument) getDocument();
				Element elem = doc.getCharacterElement(offset);
				doc.insertAfterEnd(elem, text);
				// doc.insertString(offset-1,text,null);
			} catch (BadLocationException ex) {
			} catch (IOException ex) {
			}

		}

		public void setText(String txt) {
			if(txt != null)
				super.setText(txt.replaceAll("\r?\n", "<br>"));
		}

		public void setReport(String txt) {
			if(txt != null)
				super.setText(convertToHTML(txt));
		}
			
		public String getSelectedText() {
			// get default selection
			String t = super.getSelectedText();
			if(t == null)
				return null;
			
			// get text as html
			StringWriter w = new StringWriter();
			try {
				getEditorKit().write(w, doc,getSelectionStart(),getSelectionEnd()-getSelectionStart());
			} catch (IOException e) {
			} catch (BadLocationException e) {
			}
			
			// replace all HTML formatting, then replace <br> with newlines, then delete replace all tags
			String x = w.toString().replaceAll("\n\\s*","").replaceAll("<br>","\n").replaceAll("</?\\w+\\s*(style=\".*\")?>","").trim();	
			
			// since we have to trim later, lets figure out if there are any spaces before or after
			String pr = "";
			for(int i=0;i<t.length();i++){
				if((""+t.charAt(i)).matches("\\s"))
					pr += " ";
				else
					break;
			}
			String en = "";
			for(int i=t.length()-1;i>=0;i--){
				if((""+t.charAt(i)).matches("\\s"))
					en += " ";
				else
					break;
			}
			return pr+x+en;
		}
		
		public String getText() {
			// get default selection
			String t = super.getText();
			if(t == null)
				return null;
			
			// get text as html
			StringWriter w = new StringWriter();
			try {
				getEditorKit().write(w, doc,0,t.length());
			} catch (IOException e) {
			} catch (BadLocationException e) {
			}
			
			// replace all HTML formatting, then replace <br> with newlines, then delete replace all tags
			//String x = w.toString().replaceAll("\n\\s*","").replaceAll("<br>","\n").replaceAll("</?\\w+\\s*(style=\".*\")?>","").replaceAll("<!--.*-->","").trim();	
			String x = w.toString().replaceAll("\n\\s*","").replaceAll("(<br>|<hr>|</?li>|</?p>)","\n").replaceAll("(?i)</?[^<>]+>","").replaceAll("<!--.*-->","").trim();	
			
			// since we have to trim later, lets figure out if there are any spaces before or after
			String pr = "";
			for(int i=0;i<t.length();i++){
				if((""+t.charAt(i)).matches("\\s"))
					pr += " ";
				else
					break;
			}
			String en = "";
			for(int i=t.length()-1;i>=0;i--){
				if((""+t.charAt(i)).matches("\\s"))
					en += " ";
				else
					break;
			}
			return pr+x+en;
		}
	}

	/**
	 * convert regular text report to HTML
	 * 
	 * @param txt
	 * @return
	 */
	private static String convertToHTML(String txt) {
		return txt.replaceAll("\r?\n", "<br>").replaceAll(
				"(^|<br>)([A-Z ]+:)<br>", "$1<b>$2</b><br>");
	}
	
	/**
	 * list model that wraps any java.util.List
	 * @author Eugene Tseytlin
	 */
	public static class ListModel extends AbstractListModel {
		public java.util.List list;
		public ListModel(java.util.List l){
			this.list = l;
		}
		
		/**
		 * get element at
		 */
		public Object getElementAt(int i) {
			return list.get(i);
		}

		/**
		 * get size of the list
		 */
		public int getSize() {
			return (list == null)?0:list.size();
		}
		
		/**
		 * get original list
		 * @return
		 */
		public java.util.List getList(){
			return list;
		}
		
		/**
		 * sync list content
		 * Component source (JList)
		 */
		public void sync(Component source){
			fireContentsChanged(source,0,getSize());
		}
	}
	
	
	/**
	 * format XML into human readable form
	 * @param document
	 * @param root
	 * @param tab
	 */
	private static void formatXML(Document document,org.w3c.dom.Element root, String tab) {
		NodeList children = root.getChildNodes();
		// save the nodes in the array first
		Node[] nodes = new Node[children.getLength()];
		for (int i = 0; i < children.getLength(); i++)
			nodes[i] = children.item(i);
		// insert identations
		for (int i = 0; i < nodes.length; i++) {
			root.insertBefore(document.createTextNode("\n" + tab), nodes[i]);
			if (nodes[i] instanceof org.w3c.dom.Element)
				formatXML(document, (org.w3c.dom.Element) nodes[i], "  " + tab);
		}
		root.appendChild(document.createTextNode("\n"
				+ tab.substring(0, tab.length() - 2)));
	}
	
	/**
	 * write out an XML file
	 * 
	 * @param doc
	 * @param os
	 * @throws TransformerException 
	 * @throws IOException 
	 */
	public static void writeXML(Document doc, OutputStream os) 
		throws TransformerException, IOException{
		// write out xml file
		TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        //indent XML properly
        formatXML(doc,doc.getDocumentElement(),"  ");

        //normalize document
        doc.getDocumentElement().normalize();

		 //write XML to file
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);
        transformer.transform(source, result);
        os.close();
	}
	
	/**
	 * parse XML document
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Document parseXML(InputStream in) throws IOException {
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			//builder.setErrorHandler(new XmlErrorHandler());
			//builder.setEntityResolver(new XmlEntityResolver());
			document = builder.parse(in);
			
			// close input stream
			in.close();
		}catch(Exception ex){
			throw new IOException(ex.getMessage());
		}
		return document;
	}
	
	/**
	 * get single element by tag name
	 * @param element
	 * @param tag
	 * @return
	 */
	public static org.w3c.dom.Element getElementByTagName(org.w3c.dom.Element element, String tag){
		NodeList list = element.getElementsByTagName(tag);
		for(int i=0;i<list.getLength();i++){
			Node node = list.item(i);
			if(node instanceof org.w3c.dom.Element){
				return (org.w3c.dom.Element) node;
			}
		}
		return null;
	}
	
	/**
	 * does given module fit a given description
	 * @param m
	 * @param name
	 * @return
	 */
	public static boolean isMatchingModule(TutorModule m, String name){
		Class cls = m.getClass();
		if(cls.getName().endsWith(name))
			return true;
		// check interfaces
		for(Class c: cls.getInterfaces())
			if(c.getName().endsWith(name))
				return true;
		// else, it doesn't match
		return false;
	}
	
	/**
	 * does given module fit a given description
	 * @param m
	 * @param name
	 * @return
	 */
	public static boolean isMatchingTutor(Tutor m, String name){
		Class cls = m.getClass();
		if(cls.getName().endsWith(name))
			return true;
		
		// check interfaces
		if(Tutor.class.getName().endsWith(name))
			return true;
		
		// check tutor name
		if(m.getName().equals(name))
			return true;
		// else, it doesn't match
		return false;
	}
	
	/**
	 * get string response from the input (should be text/plain)
	 * @param url
	 * @return
	 */
	public static String doGet(URL url) throws IOException{
		URLConnection conn = url.openConnection();
		// Turn off caching
		conn.setUseCaches(false);
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		
		// try to fetch several times
		InputStream is = null;
		try{
			is = conn.getInputStream();
		}catch(IOException ex){
			// try another time
			conn = url.openConnection();
			
			// Turn off caching
			conn.setUseCaches(false);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			// try to fetch several times
			try{
				is = conn.getInputStream();
			}catch(IOException ex1){
				throw ex1;
			}
		}
		return processTextStream(is);
	}
	
	
	/**
	 * get string response from the input (should be text/plain)
	 * @param url
	 * @return
	 */
	public static String doGet(URL url, Map map) throws IOException{
		StringBuffer u = new StringBuffer(""+url);
		String s = "?";
		for(Object k : map.keySet()){
			String key = ""+k;
			String val = ""+map.get(k);
			key = key.replaceAll(" ","%20");
			val = val.replaceAll(" ","%20");
			u.append(s+key+"="+val);
			s = "&";
		}
		return doGet(new URL(u.toString()));
	}
	
	
	/**
	 * get string response from the input (should be text/plain)
	 * @param url
	 * @return
	 */
	public static String doPost(URL url, Object obj) throws IOException{
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		
		// Turn off caching
		conn.setUseCaches(false);
		conn.setConnectTimeout(60000);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
	    
        // pure serialization
	    ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
	    objOut.writeObject(obj);
	    objOut.flush();
	    objOut.close();
       
		// read response
		return processTextStream(conn.getInputStream());
	}
	
	
	/**
	 * process text sream
	 * @param in
	 * @return
	 */
	private static String processTextStream(InputStream in) throws IOException{
		// get input
		StringBuffer buf = new StringBuffer();
	        
        //recieve object
        BufferedReader stream = null;
        try{
            stream = new BufferedReader(new InputStreamReader(in));
            for(String line=stream.readLine(); line != null; line=stream.readLine()){
                buf.append(line+"\n");
            }
        }catch(IOException ex){
            throw ex;
        }finally{
            if(stream != null){
                stream.close();
            }
            if(in != null){
            	in.close();
            }
            	
        }
        return buf.toString();
	}
	
	/**
	 * is string a url
	 * @param url
	 * @return
	 */
	public static boolean isURL(String url){
		return url.matches("[a-zA-Z]+://(.*)");
	}
	
	/**
	 * get an object 
	 * @param <T>
	 * @param name
	 */
	public static <T extends TutorModule> T getTutorModule(String name,Class<T> cls){
		Object obj = null;
		try {
			obj = Class.forName(name).newInstance();
		} catch (Exception e) {
			Config.getLogger().severe("Can't instantiate module "+name);
			e.printStackTrace();
		}
		return (T) obj;
	}
	
	
	
	/**
	 * enable/disable abstract buttons inside container unless
	 * they are part of exceptions
	 * @param cont
	 * @param exceptions
	 */
	public static void setEnabled(Container cont,boolean flag){
		setEnabled(cont,new String [0],flag);
	}
	
	/**
	 * enable/disable abstract buttons inside container unless
	 * they are part of exceptions
	 * @param cont
	 * @param exceptions
	 */
	public static void setEnabled(Container cont, String [] exceptions, boolean flag){
		if(cont == null)
			return;
		for(Component c: (cont instanceof JMenu)?((JMenu)cont).getMenuComponents():cont.getComponents()){
			if(!isException(c, exceptions)){
				if(c instanceof JMenu){
					setEnabled((Container)c, exceptions,flag);
				}else{
					c.setEnabled(flag);
				}
			}
		}
	}
	
	
	/**
	 * enable/disable abstract buttons inside container unless
	 * they are part of exceptions
	 * @param cont
	 * @param exceptions
	 */
	public static void setEnableRecursive(Container cont, boolean flag){
		for(Component c: (cont instanceof JMenu)?((JMenu)cont).getMenuComponents():cont.getComponents()){
			if(c instanceof JMenu){
				setEnabled((Container)c, flag);
			}else if(c instanceof JPanel){
				setEnabled((Container)c, flag);
			}else if(c instanceof JTextComponent){
				((JTextComponent)c).setEditable(flag);
			}else{
				c.setEnabled(flag);
			}
		}
		
	}
	/**
	 * is this component an exception
	 * @param c
	 * @param exceptions
	 * @return
	 */
	private static boolean isException(Component c, String [] exceptions){
		String name = null;
		String cmd = null;
		if(c instanceof AbstractButton){
			cmd  = ((AbstractButton)c).getActionCommand();
			name = ((AbstractButton)c).getText();
		}else if(c instanceof JMenuItem){
			name = ((JMenuItem)c).getText();
		}
		// if name is null then don't touch this component
		if(name == null && cmd == null)
			return true;
		
		// check all exceptions
		for(String ex: exceptions)
			if(ex.equalsIgnoreCase(name) || ex.equalsIgnoreCase(cmd))
				return true;
		return false;
	}
	
	
	/**
     * prompt for login informat
     * @param place - prompt for institution
     * @return
     */
    public static String [] promptLogin(){
    	return promptLogin(null);
    }

	/**
     * prompt for login informat
     * @param place - prompt for institution
     * @return
     */
    public static String [] promptLogin(String error){
    	// create fields
		JTextField usernameField = new JTextField(20);
		JTextField passwordField = new JPasswordField("",20);
		JTextField errorLabel = new JTextField(20);
		errorLabel.setEditable(false);
		if(error == null){
    		error = "Enter username and password";
		}else{
			errorLabel.setForeground(Color.red);
		}
    	errorLabel.setText(error);
		
		// create labels
		JLabel  userNameLabel = new JLabel("Username:   ", JLabel.RIGHT);
		JLabel  passwordLabel = new JLabel("Password:   ", JLabel.RIGHT);
		errorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
		JLabel 	statusLabel = new JLabel("Status:    ", JLabel.RIGHT);
				
		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel,BoxLayout.X_AXIS));
		connectionPanel.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0,1));
		namePanel.add(userNameLabel);
		namePanel.add(passwordLabel);
		namePanel.add(statusLabel);
		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridLayout(0,1));
		fieldPanel.add(usernameField);
		fieldPanel.add(passwordField);
		fieldPanel.add(errorLabel);
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);
    		
		// return 
    	String [] login = new String [2];
		String title = Config.getProperty("tutor.name");
		if(title.length() == 0)
			title = "Login";
    	
		// THIS IS A HACK TO GET AROUND SUN JAVA BUG
		(new FocusTimer(usernameField)).start();
		
		// prompt for password
		if(JOptionPane.OK_OPTION == 
		   JOptionPane.showConfirmDialog(null,connectionPanel,title,
		   JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
			login[0] = usernameField.getText();
			login[1] = passwordField.getText();
		}else{
			return null;
		}
		return login;
    }
    
    /**
     * prompt for login informat
     * @param place - prompt for institution
     * @return
     */
    public static String [] promptLogin(boolean place){
    	JLabel errorLabel = new JLabel("Enter name and password");
    	
    	// fetch institutions
    	Vector<String> places = new Vector<String>();
    	if(place){
    		Map map = new HashMap();
    		map.put("action","list");
    		map.put("root","config");
    		try{
	    		String str = Communicator.doGet(Communicator.getServletURL(),map);
	    		for(String p: str.split("\n")){
	    			if(p.endsWith("/"))
	    				p = p.substring(0,p.length()-1);
	    			places.add(p);
	    		}
    		}catch(Exception ex){
    			errorLabel.setText("<html><font color=red>"+ex.getMessage()+"</font>");
    		}
    	}
    	// create fields
		JTextField usernameField = new JTextField("");
		JTextField passwordField = new JPasswordField("",12);
		JComboBox institutionField = new JComboBox(places);
		institutionField.setPreferredSize(passwordField.getPreferredSize());
		
		
		// create labels
		JLabel  userNameLabel = new JLabel("Name:   ", JLabel.RIGHT);
		JLabel  passwordLabel = new JLabel("Password:   ", JLabel.RIGHT);
		JLabel  institutiondLabel = new JLabel("Institution:   ", JLabel.RIGHT);
		errorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
		JLabel 	statusLabel = new JLabel("Status:    ", JLabel.RIGHT);
				
		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel,BoxLayout.X_AXIS));
							
		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0,1));
		namePanel.add(userNameLabel);
		namePanel.add(passwordLabel);
		if(place)
			namePanel.add(institutiondLabel);
		namePanel.add(statusLabel);
		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridLayout(0,1));
		fieldPanel.add(usernameField);
		fieldPanel.add(passwordField);
		if(place)
			fieldPanel.add(institutionField);
		fieldPanel.add(errorLabel);
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);
    		
		// return 
    	String [] login = new String [3];
		
		// prompt for password
		if(JOptionPane.OK_OPTION == 
		   JOptionPane.showConfirmDialog(null,connectionPanel,"Login",
		   JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
			login[0] = usernameField.getText();
			login[1] = passwordField.getText();
			login[2] = ""+institutionField.getSelectedItem();
		}else{
			return null;
		}
		return login;
    }
    
    /**
	 * request timer to focus on given component
	 * can only invoke one instance at a time
	 * @author tseytlin
	 *
	 */
	public static class FocusTimer extends Timer {
		private static FocusTimer instance;
		public FocusTimer(JComponent comp){
			this(createListener(comp));
		}
		public FocusTimer(ActionListener listener){
			super(100,listener);
			setRepeats(true);
			instance = this;
		}
		
		public static ActionListener createListener(JComponent c){
			final JComponent component = c;
			return new ActionListener() {
				public void actionPerformed(ActionEvent e){
		            if(component != null){
						if(component.hasFocus()) {
							if(instance != null)
								instance.setRepeats(false);
			                return;
			            }
						component.requestFocusInWindow();
		            }
		        }
			};
		}
	}
    
    
	/**
	 * display case
	 * @param c
	 */
	public static JComponent createCaseInfoPanel(CaseEntry caseEntry, boolean includeSlides){
		return createCaseInfoPanel(caseEntry,new String [] {OntologyHelper.DIAGNOSES, OntologyHelper.DIAGNOSTIC_FEATURES},includeSlides);
	}
    
	/**
	 * display case
	 * @param c
	 */
	public static JComponent createCaseInfoPanel(CaseEntry caseEntry,String [] types, boolean includeSlides){
		// get list of slides
		String report = "No Report Available";
		//String [] slides = new String [0];
	
		// load info
		String name = caseEntry.getName();
		report = TextHelper.convertToHTML(caseEntry.getReport());
		java.util.List<SlideEntry> slides = caseEntry.getSlides();
		//slides = new String [slist.size()];
		//for(int i=0;i<slides.length;i++){
		//	slides[i] = slist.get(i).getSlideName();
		//}
		
		// construct summary
		StringBuffer info = new StringBuffer();
		/*
		info.append("<table><tr><td align=left><b>DIAGNOSIS</b><br>");
		info.append("<ul>");
		for(ConceptEntry dx : caseEntry.getConcepts(OntologyHelper.DIAGNOSES).getValues()){
			info.append("<li>"+dx.getText()+"</li>");
		}
		info.append("</ul><br><b>FINDINGS</b><br><ul>");
		for(ConceptEntry fn : caseEntry.getConcepts(OntologyHelper.DIAGNOSTIC_FEATURES).getValues()){
			info.append("<li>"+fn.getText()+"</li>");
		}
		info.append("</ul></td></tr></table>");
		*/
		info.append("<table><tr><td align=left>");
		for(String type : types){
			info.append("<br><b>"+type.replaceAll("_"," ")+"</b><br>");
			info.append("<ul>");
			for(ConceptEntry dx : caseEntry.getConcepts(type).getValues()){
				info.append("<li><font color="+((dx.isImportant())?"black":"gray")+">"+dx.getText()+"</font></li>");
			}
			info.append("</ul>");
		}
		info.append("</td></tr></table>");
		
		
		
		// init viewer if required
		JPanel panel = null;
		if(includeSlides){
			String type = (slides.size()>0)?ViewerFactory.recomendViewerType(slides.get(0).getSlideName()):"qview";
			final String dir = ViewerFactory.getProperties().getProperty(type+".image.dir","");
			final Viewer viewer = ViewerFactory.getViewerInstance(type);
			viewer.setSize(new Dimension(500,500));
			
			// init buttons
			JToolBar toolbar = new JToolBar();
			toolbar.setMinimumSize(new Dimension(0,0));
			ButtonGroup grp = new ButtonGroup();
			AbstractButton selected = null;
			for(int i=0;i<slides.size();i++){
				final String image = slides.get(i).getSlidePath();
				String text = slides.get(i).getSlideName();
				// strip suffic and prefix
				if(slides.size() > 1 && text.startsWith(name))
					text = text.substring(name.length()+1);
				if(text.lastIndexOf(".") > -1)
					text = text.substring(0,text.lastIndexOf("."));
				// create buttons
				AbstractButton bt = new JToggleButton(text);
				bt.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e){
						JToggleButton bt = (JToggleButton) e.getSource();
						if(bt.isSelected()){
							try{
								viewer.openImage(dir+image);
							}catch(ViewerException ex){
								ex.printStackTrace();
							}
						}
					}
				});
				grp.add(bt);
				toolbar.add(bt);
				
				// select entry
				if(selected == null && (text.contains("HE") || slides.size() == 1))
					selected = bt;
			}
			
			// do click
			if(selected != null){
				selected.addHierarchyListener(new HierarchyListener() {
					public void hierarchyChanged(HierarchyEvent e) {
						if((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) !=0 && e.getComponent().isShowing()) {
							((AbstractButton)e.getComponent()).doClick();
							e.getComponent().removeHierarchyListener(this);
						}
					}
				});
			}
			
			// create gui
			panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(toolbar,BorderLayout.NORTH);
			panel.add(viewer.getViewerPanel(),BorderLayout.CENTER);
			panel.setPreferredSize(new Dimension(500,600));
		}
		
		UIHelper.HTMLPanel text = new UIHelper.HTMLPanel();
		text.setEditable(false);
		//text.setPreferredSize(new Dimension(350,600));
		text.append(report);
		text.setCaretPosition(0);
		
		
		UIHelper.HTMLPanel summary = new UIHelper.HTMLPanel();
		summary.setEditable(false);
		//text.setPreferredSize(new Dimension(350,600));
		summary.append(""+info);
		summary.setCaretPosition(0);
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Case Summary", new JScrollPane(summary));
		tabs.addTab("Case Report", new JScrollPane(text));
				
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(tabs,BorderLayout.CENTER);
		p.setPreferredSize(new Dimension(400,600));
		
		// return either split panel with viewer, or tabs
		if(panel != null){
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,panel,p);
			split.setResizeWeight(1);
			return split;
		}
		return p;
	}
    
	
	
	/**
	 * confidence panel
	 * @return
	 */
	public static RadioGroup createConfidencePanel(ActionListener listener,int st, int en,String s1,String s2){
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(100,55));
		panel.setLayout(new BorderLayout());
		//panel.setBorder(new LineBorder(Color.black));
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		RadioGroup group = new RadioGroup();
		group.setComponent(panel);
		for(int i=st;i<=en;i++){
			JRadioButton bt = new JRadioButton(""+i);
			bt.setActionCommand(bt.getText());
			bt.addActionListener(listener);
			group.add(bt);
			p.add(bt);
		}
		
		panel.add(p,BorderLayout.CENTER);
		// optionally add labels
		if(s1 != null && s2 != null){
			JLabel l1 = new JLabel(s1) ;
			l1.setFont(l1.getFont().deriveFont(Font.PLAIN));
			JLabel l2 = new JLabel(s2) ;
			l2.setFont(l2.getFont().deriveFont(Font.PLAIN));
			
			JPanel labels = new JPanel();
			labels.setLayout(new FlowLayout());
			labels.add(l1);
			labels.add(Box.createRigidArea(new Dimension(120,2)));
			labels.add(l2);
			panel.add(labels,BorderLayout.SOUTH);
		}
		
		return group;
	}
	
	
	/**
	 * button group with component
	 * @author tseytlin
	 */
	public static class RadioGroup extends ButtonGroup {
		private Component component;
		private JTextComponent text;
		
		public void setTextComponent(JTextComponent text) {
			this.text = text;
		}
		public Component getComponent(){
			return component;
		}
		public void setComponent(Component c){
			component = c;
		}
		public String getCommand(){
			if(text != null){
				return text.getText();
			}else{
				ButtonModel model = getSelection();
				return (model != null)?model.getActionCommand():"";
			}
		}
	}
	
	/**
	 * create an answer panel
	 * @param format
	 * @return
	 */
	private static RadioGroup createAnswerPanel(ActionListener listener,String format){
		// split format 
		String [] f = format.split("[\\|;]");
		if("choice".equals(f[0])){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			//panel.setBorder(new LineBorder(Color.black));
			JPanel p = new JPanel();
			
			// check the size
			if(format.length() > 30){
				p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
			}else{
				p.setLayout(new FlowLayout(FlowLayout.CENTER));
			}
			
			RadioGroup group = new RadioGroup();
			group.setComponent(panel);
			for(int i=1;i<f.length;i++){
				JRadioButton bt = new JRadioButton(f[i].trim());
				bt.setActionCommand(bt.getText());
				bt.addActionListener(listener);
				group.add(bt);
				p.add(bt);
			}
			panel.add(p,BorderLayout.CENTER);
			return group;
		}else if("scale".equals(f[0])){
			int st = 0;
			int en = 5;
			String sl = null;
			String el = null;
			try{
				st = Integer.parseInt(f[1]);
				en = Integer.parseInt(f[2]);
				if(f.length > 4){
					sl = f[3].trim();
					el = f[4].trim();
				}
			}catch(Exception ex){}
			return createConfidencePanel(listener,st,en,sl,el);
		}else{
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			final JTextArea area = new JTextArea(5,25);
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			final ActionListener al = listener;
			area.setDocument(new DefaultStyledDocument(){
				public void insertString(int arg0, String arg1, AttributeSet arg2) throws BadLocationException {
					super.insertString(arg0, arg1, arg2);
					al.actionPerformed(new ActionEvent(area, 0, ""));
				}
			});
			RadioGroup r = new RadioGroup();
			p.add(new JScrollPane(area),BorderLayout.CENTER);
			r.setTextComponent(area);
			r.setComponent(p);
			return r;
		}
	}
	
	/**
	 * ask user a question in a popup modal dialog box
	 * @param text   - question text
	 * @param format - question foramt 
	 *    null or blank - for simple popup message
	 *    prompt|yes;no or prompt|OK;Cancel - regular prompt dialog
	 *    choice|answer1;anser2;answer3 - multiple choice question
	 *    text|                         - free text comment
	 *    scale|min;max;minLabel;maxLabel - some sort of scale (Likert scale) with optional labels
	 * @return answer selected by the user
	 */
	public static String promptUser(String text,String format){
		final JDialog dialog = new JDialog(Config.getMainFrame(),"Question");
		final JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				dialog.dispose();
			}
		});
		ok.setEnabled(false);
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2,1));
		JLabel label = new JLabel("<html>"+text);
		label.setBorder(new EmptyBorder(5,10,5,10));
		label.setFont(label.getFont().deriveFont(15.0f));
		p.add(label);
		ActionListener listener = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				ok.setEnabled(true);
			}
		};
		
		// default group
		RadioGroup group = null;
		
		// if no format do simple prompt
		if(TextHelper.isEmpty(format)){
			ok.setEnabled(true);
		}else if(format.startsWith("prompt")){
			int opts = format.equals("prompt|yes;no")?JOptionPane.YES_NO_OPTION:JOptionPane.OK_CANCEL_OPTION;
			int r = JOptionPane.showConfirmDialog(Config.getMainFrame(),label,"Question",opts,
					JOptionPane.PLAIN_MESSAGE,Config.getIconProperty("icon.general.tutor.logo"));
			return (JOptionPane.YES_NO_OPTION == opts)?(r== JOptionPane.YES_OPTION)?"yes":"no":(r == JOptionPane.OK_OPTION)?"ok":"cancel";
		}else{
			group = createAnswerPanel(listener,format);
			p.add(group.getComponent());
		}
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(ok);
		
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setModal(true);
		dialog.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(p,BorderLayout.CENTER);
		panel.add(buttons,BorderLayout.SOUTH);
		panel.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),new BevelBorder(BevelBorder.RAISED)));
		dialog.setContentPane(panel);
		dialog.pack();
		centerWindow(Config.getMainFrame(),dialog);
		dialog.setVisible(true);
		return (group != null)?group.getCommand():null;
	}
	

	
	/**
	 * create dialog that cannot be closed until some criteria is met
	 * @param component
	 * @param close button 
	 */

	public static JDialog createDialog(Component p, JButton ... oks){
		final JDialog dialog = new JDialog(Config.getMainFrame());
		ActionListener l = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				((AbstractButton)e.getSource()).setSelected(true);
				dialog.dispose();
			}
		};
		//ok.setEnabled(false);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		for(JButton ok: oks){
			ok.addActionListener(l);
			buttons.add(ok);
		}
			
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setModal(true);
		dialog.setResizable(false);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(p,BorderLayout.CENTER);
		panel.add(buttons,BorderLayout.SOUTH);
		panel.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),new BevelBorder(BevelBorder.RAISED)));
		dialog.setContentPane(panel);
		dialog.pack();
		centerWindow(Config.getMainFrame(),dialog);
		//dialog.setVisible(true);
		return dialog;
	}
	
	
	/**
	 * copy content from 
	 * @param panel
	 * @return
	 */
	public static void copy(Container src, Container dst){
		dst.removeAll();
		dst.setLayout(src.getLayout());
		dst.setPreferredSize(src.getPreferredSize());
		for(int i=0;i<src.getComponentCount();i++){
			Component comp = src.getComponent(i);
			Object c = null;
			
			// figure out contstraints
			if(src.getLayout() instanceof BorderLayout){
				c = ((BorderLayout) src.getLayout()).getConstraints(comp);
			}
			
			// add to destination
			if(c == null)
				dst.add(comp);
			else
				dst.add(comp,c);
		}
		dst.validate();
		dst.repaint();
	}
	
	
	/**
	 * return component that represents this image
	 * @param scr - image icon representing
	 * @return
	 */
	
	public static Component getImageComponent(Image img, Dimension max){
		if(img == null)
			return null;
		
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		
		if(max != null){
			if(w > max.width){
				h = h * max.width / w;
				w = max.width;
			}
			if(h > max.height){
				w = w * max.height / h;
				h = max.height;
			}
			
		}
		return new JLabel(new ImageIcon(img.getScaledInstance(w,h,Image.SCALE_SMOOTH)));
	}
	
	 /**
     * create mirrored image
     * @param tc
     * @return
     */
    public static Image getMirrorImage(Image tc ){
    	 BufferedImage bf = new BufferedImage(tc.getWidth(null),tc.getHeight(null),BufferedImage.TYPE_INT_ARGB);
         bf.createGraphics().drawImage(tc,0,0,null);
         // Flip the image horizontally
         AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
         tx.translate(-tc.getWidth(null), 0);
         AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
         bf = op.filter(bf, null);
         return bf;
    }
	
    public static void flashImage(Component c, Image img){
    	 MediaTracker tracker = new MediaTracker(c);
         tracker.addImage(img,0);
         try{
        	 tracker.waitForAll();
         }catch(Exception ex){}
    }
}
