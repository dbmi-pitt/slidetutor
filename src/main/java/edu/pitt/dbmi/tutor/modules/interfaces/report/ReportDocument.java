 /**
 * This is a document representation of the report content
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report;

import java.awt.Component;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.*;
import java.util.*;
import java.util.regex.*;
import java.beans.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.event.*;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.modules.interfaces.report.process.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.terminology.Terminology;
import edu.pitt.text.tools.TextTools;

public class ReportDocument extends DefaultStyledDocument implements CaretListener {
	private final String TEMPLATE = "/resources/ReportTemplate.xml";
	private static MutableAttributeSet headerStyle, disabledTextStyle, textStyle, backgroundStyle;
	private static Font textFont;

	private boolean typing = false; // is typing/deleteing in progress 
	private boolean templateProtection = true; // enable template protection
	private boolean defaultOp = false; // when true, insert and remove behave with no customizations
	private boolean readOnly;
	
	// processing resource list
	private List<ReportProcessor> reportProcessors;
	private Set<String> sections,editableSections;

	// report data structure
	private JLabel status;
	private ReportData reportData;
	private JTextPane textEditor;
	private TextTools textTools;
	private Terminology terminology;
	private transient NodeList reportTemplate;
	//private ReportProtocol protocol;
	
	//public volatile boolean attachInProgress;


	/**
	 * Initialize ReportDocument
	 * DataSet data, 
	 */
	public ReportDocument(JTextPane text) {
		super();
		//super(new StringContent(), new StyleContext());
		textEditor = text;

		// setup styles for template
		setupStyles();
		
		// init processor report list
		reportProcessors = new ArrayList<ReportProcessor>();

		//ceate list of sections
		sections = new LinkedHashSet<String>();
		editableSections = new LinkedHashSet<String>();
	}

	/**
	 * get frame that can be used
	 * @return
	 */
	public Frame getFrame(){
		return JOptionPane.getFrameForComponent(textEditor);
	}

	/**
	 * set status label
	 * @param s
	 */
	public void setStatusLabel(JLabel s){
		status = s;
	}
	
	/**
	 * get status label
	 * @return
	 */
	public JLabel getStatusLabel(){
		return status;
	}
	
	public TextTools getTextTools() {
		return textTools;
	}


	public void setTextTools(TextTools textTools) {
		this.textTools = textTools;
	}


	public Terminology getTerminology() {
		return terminology;
	}


	public void setTerminology(Terminology terminology) {
		this.terminology = terminology;
	}
	
	/**
	 * Set tab size. I don't know why it has to be so damn complicated in java
	 * this code was shamelessly copied from: 
	 * http://forum.java.sun.com/thread.jspa?threadID=585006&messageID=3002940
	 */
	public void setTabs(int charactersPerTab) {
		Component comp = textEditor;
		FontMetrics fm = comp.getFontMetrics(textFont);
		int charWidth = fm.charWidth('w');
		int tabWidth = charWidth * charactersPerTab;

		TabStop[] tabs = new TabStop[1];

		for (int j = 0; j < tabs.length; j++) {
			int tab = j + 1;
			tabs[j] = new TabStop(tab * tabWidth);
		}

		TabSet tabSet = new TabSet(tabs);
		SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setTabSet(attributes, tabSet);
		setParagraphAttributes(0, getLength(), attributes, false);
	}

	/**
	 * register processing resource
	 */
	public void addReportProcessor(ReportProcessor pr) {
		reportProcessors.add(pr);
	}

	/**
	 * register processing resource
	 */
	public void removeReportProcessor(ReportProcessor pr) {
		reportProcessors.remove(pr);
	}

	/**
	 * remove all processors
	 */
	public void removeReportProcessors() {
		reportProcessors.clear();
	}

	/** 
	 * setup text styles for report interface
	 */
	private void setupStyles() {
		// figure out fonts
		UIDefaults uidefs = UIManager.getLookAndFeelDefaults();
		textFont = (Font) uidefs.get("TextPane.font");
		String family = textFont.getFamily();
		int size = textFont.getSize();

		// setup text font for concepts
		//ConceptLabel.setTextFont(textFont);

		// section heading
		headerStyle = new SimpleAttributeSet();
		headerStyle.addAttribute("isheader", Boolean.TRUE);
		StyleConstants.setBold(headerStyle, true);
		//StyleConstants.setFontFamily(headerStyle,family);
		//StyleConstants.setFontSize(headerStyle,size+2);

		// disabled default text
		disabledTextStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(disabledTextStyle, Color.gray); // lightGray
		StyleConstants.setFontFamily(disabledTextStyle, family);
		StyleConstants.setFontSize(disabledTextStyle, size);
		disabledTextStyle.addAttribute("editable", Boolean.FALSE);

		// plain text
		textStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(textStyle, Color.black);
		StyleConstants.setFontFamily(textStyle, family);
		StyleConstants.setFontSize(textStyle, size);
		textStyle.addAttribute("editable", Boolean.TRUE);
		textStyle.addAttribute("locked", Boolean.FALSE);
		
		// background
		backgroundStyle = new SimpleAttributeSet();
		StyleConstants.setBackground(backgroundStyle,Color.white);
	}

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	// aux methods to send events to ProtocolReader
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	// aux methods to send events to ProtocolReader
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	// aux methods to send events to ProtocolReader
	public void firePropertyChange(String name, Object oldValue, Object newValue) {
		pcs.firePropertyChange(name, oldValue, newValue);
	}

	/**
	 * Load template
	 */
	public void loadTemplate(InputStream file) {
		loadTemplate(file,null);
	}
	
	/**
	 * Load template
	 */
	public void loadTemplate(InputStream file,CaseEntry c) {
		org.w3c.dom.Document document = null;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			//builder.setErrorHandler(new XmlErrorHandler());
			//builder.setEntityResolver(new XmlEntityResolver());
			document = builder.parse(file);

			//print out some usefull info
			org.w3c.dom.Element element = document.getDocumentElement();
			//StringBuffer buffer = new StringBuffer();
			// clear document
			templateProtection = false; // disable template protection
			reportTemplate = element.getChildNodes();
			readTemplate(reportTemplate,c);
			templateProtection = true; // enable template protection
			//textPane.setText(buffer.toString());
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
		} catch (SAXException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * read in XML document
	 */
	private void readTemplate(NodeList elements,CaseEntry info) {

		if (elements == null)
			return;
		// loop over elements
		for (int i = 0; i < elements.getLength(); i++) {
			Node node = (Node) elements.item(i);
			// check which element it is
			if (node instanceof Text) {
				String text = ((Text) node).getData().trim();
				// skip emptry lines
				if (text.length() > 0)
					append(text + "\n\n", disabledTextStyle);
			} else if (node instanceof org.w3c.dom.Element) {
				// get attributes
				String name = ((org.w3c.dom.Element) node).getAttribute("name");
				String edit = ((org.w3c.dom.Element) node).getAttribute("editable");
				String value = ((org.w3c.dom.Element) node).getAttribute("value");

				// get start position
				int start_offset = getEndPosition().getOffset();

				// append section heading
				append(name + ":", headerStyle);

				// if there is a value associated with a section, then extract it
				if (value != null && value.length() > 0 && !Boolean.parseBoolean(edit)) {
					if(info != null){
						append(info.getReportSection(name)+ "\n\n", disabledTextStyle);
					}else{
						append("\nno info\n\n", disabledTextStyle);
					}
					//edit = ""+disabledTextStyle.getAttribute("editable");
				}else{
					append("\n\n\n", textStyle);
				}

				// if section is editable then insert couple of newlines
				//if (edit != null && Boolean.valueOf(edit).booleanValue())
				//	append("\n\n\n", textStyle);

				// recurse into other elements
				readTemplate(node.getChildNodes(),info);

				// get end position
				int end_offset = getEndPosition().getOffset();

				// set "logical" paragraph for each section
				MutableAttributeSet editableStyle = new SimpleAttributeSet();
				editableStyle.addAttribute("name", name);
				editableStyle.addAttribute("editable", Boolean.valueOf(edit));

				setParagraphAttributes(start_offset, end_offset - start_offset, editableStyle, false);
			
				if (Boolean.valueOf(edit).booleanValue())
					editableSections.add(name);
				sections.add(name);
			}
		}
	}

	
	/**
	 * clear report
	 */
	public void clear(){
		// remove all content
		try{
			defaultOp  = true;
			remove(0,getEndPosition().getOffset()-1);
			defaultOp = false;
		}catch(BadLocationException ex){
			//ex.printStackTrace();
		}
		//reload template
		/*
		if(reportTemplate != null){
			templateProtection = false; // disable template protection
			readTemplate(reportTemplate,null);
			templateProtection = true; // enable template protection
		}
		*/
	}
	
	
	/**
	 * load case entry information
	 * @param entry
	 */
	public void load(){
		load(null);
	}
	
	/**
	 * load case entry information
	 * @param entry
	 */
	public void load(CaseEntry entry){
		clear();
		String str = TEMPLATE;
		if(reportData != null){
			str = Config.getProperty(reportData.getReportInterface(),"report.template");
		}
		loadTemplate(getClass().getResourceAsStream(str),entry);
	}
	
	
	/**
	 * append string to the end of document
	 */
	private void append(String str, AttributeSet a) {
		try {
			int offset = getEndPosition().getOffset() - 1;
			if (a == null)
				a = disabledTextStyle;

			super.insertString(offset, str, a);
			// flag that typing is not in progress for caret
			typing = false;
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * append string to the end of document
	 */
	public void appendText(String str) {
		try {
			int offset = getEndPosition().getOffset() - 1;
			// try to get an offset that is not preseded by many newlines
			String text = getText(0, offset);
			for (int i = offset - 1; i >= 0; i--) {
				if (text.charAt(i) != '\n') {
					// return location that precedes non-newline by 2
					if (offset >= i + 2)
						offset = i + 2;
					break;
				}
			}
			insertString(offset, str, null);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * get offset of the place right after section heading or
	 * at the end of the section
	 * @param section which section
	 * @return
	 */
	public int getOffset(String section) {
		return getOffset(section, true);
	}

	/**
	 * get offset of the place right after section heading or
	 * at the end of the section
	 * @param section which section
	 * @param start at the begining or the end of section
	 * @return
	 */
	public int getOffset(String section, boolean start) {
		try {
			section = section.toUpperCase();
			if (!section.endsWith(":"))
				section = section + ":";

			// find if section exists
			int offset = getEndPosition().getOffset();
			// try to get an offset that is not preseded by many newlines
			String text = getText(0, offset);
			// try to find offset of a section
			int index = text.indexOf(section);
			// no section heading found if -1
			if (index < 0)
				return -1;

			//determine offset
			offset = index + section.length();

			if (!start) {
				// find next section
				Pattern pt = Pattern.compile("([A-Z ]+:)");
				Matcher mt = pt.matcher(text);
				if(mt.find(offset)){
					while(mt.find(offset)){
						offset = mt.start() - 1;
						if(isEditable(mt.start()))
							offset = mt.start() + mt.group(1).length() +1;
						else
							break;	
					}
				}else{
					offset = getEndPosition().getOffset();
				}
			}
			return offset;
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	/**
	 * insert text into document
	 * @param text    - text to be inserted
	 * @param section - section headding under which text should go
	 * @param start   - true insert right after heading, false - append to the end of section
	 */
	public void insertText(String str, int offset) {
		try {
			insertString(offset, str, null);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * insert text into document
	 * @param text    - text to be inserted
	 * @param section - section headding under which text should go
	 * @param start   - true insert right after heading, false - append to the end of section
	 */
	public void insertText(String str, String section) {
		insertText(str, section, true);
	}

	/**
	 * insert text into document
	 * @param text    - text to be inserted
	 * @param section - section headding under which text should go
	 * @param start   - true insert right after heading, false - append to the end of section
	 */
	public void insertText(String str, String section, boolean start) {
		try {
			int offset = getOffset(section, start);
			if (offset > -1){
				insertString(offset,((start)?"\n":"")+str, null);
			}
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * insert text into document
	 * @param text    - text to be inserted
	 * @param section - section heading under which text should go
	 * @param start   - true insert right after heading, false - append to the end of section
	 */
	public void setText(String str, String section) {
		try {
			int st = getOffset(section,true);
			int en = getOffset(section,false);
			if(en == getEndPosition().getOffset())
				en = en -1;
			if (st > -1 && en > st){
				replaceText(st,en-st,str);
			}
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}
	/**
	 * is editable attribute
	 * @param offs
	 * @return
	 */
	public boolean isEditableAttribute(int offs){
		AttributeSet attr1 = getParagraphElement(offs).getAttributes();
		Object obj1 = attr1.getAttribute("editable");
		return (obj1 != null && ((Boolean) obj1).booleanValue());
	}
	
	/**
	 * Determin whether this section is editable
	 */
	public boolean isEditable(int offs) {
		// to allow appends we should be able to edit at the end of the document
		if (!templateProtection)
			return true;

		// get section and character attributes
		AttributeSet attr1 = getParagraphElement(offs).getAttributes();
		AttributeSet attr2 = getCharacterElement(offs).getAttributes();
		AttributeSet attr3 = getCharacterElement(offs).getAttributes();

		// check if we can edit this section
		Object obj1 = attr1.getAttribute("editable");
		Object obj2 = attr2.getAttribute("isheader");
		Object obj3 = attr3.getAttribute("locked");

		boolean editable = (obj1 != null && ((Boolean) obj1).booleanValue());
		boolean isheader = (obj2 != null && ((Boolean) obj2).booleanValue());
		boolean locked = (obj3 != null && ((Boolean) obj3).booleanValue());

		//System.out.println(obj1+" "+obj2+" "+obj3+" for offset "+offs);
		return (!isheader && editable && !locked);
		//return (!isheader &&  !locked);
	}

	/**
	 * Determin whether this section can be deleted
	 */
	private boolean isDeletable(int offs, int length) {
		// if we can't edit it, we can't delete it
		if (!isEditable(offs))
			return false;

		// we don't want to delete last newline of editable text
		int next = offs + length;
		// check whether we are at the end
		if (next <= getEndPosition().getOffset() && !isEditable(next))
			return false;

		// now we want to make sure that we don't delete data across sections
		String name_s = (String) getParagraphElement(offs).getAttributes().getAttribute("name");
		String name_e = (String) getParagraphElement(next).getAttributes().getAttribute("name");
		if (name_s == null || name_e == null || !name_s.equals(name_e))
			return false;

		return true;
	}

	/**
	 * when template protection property is set, then text that is marked
	 * editable = false in tamplate won't be editable.
	 */
	public void setTemplateProtection(boolean val) {
		templateProtection = val;
	}

	/**
	 * Keep truck of pointers
	 */
	public void remove(int offs, int length) throws BadLocationException {
		// if in default mode, do as before
		if (defaultOp) {
			super.remove(offs, length);
			return;
		}

		// don't do anything if in rad-only mode
		if(readOnly)
			return;
		
		// flag that typing is in progress for caret
		typing = true;

		// check if we can delete it (we don't want to delete last editable newline
		if (!isDeletable(offs, length))
			return;

		// area to remove
		int new_offset = offs;
		int new_length = length;

		// if we have conceps are being removed, then we need to re-organize removal range 
		Element element1 = getCharacterElement(offs);
		Element element2 = getCharacterElement(offs + length - 1);
		Object obj1 = element1.getAttributes().getAttribute("concept");
		Object obj2 = element2.getAttributes().getAttribute("concept");

		// if start element is a concept
		if (obj1 != null && ((Boolean) obj1).booleanValue()) {
			new_offset = element1.getStartOffset();
			new_length = Math.max(element1.getEndOffset() - new_offset, length + offs - new_offset);
		}

		// if end element is a concept
		if (obj2 != null && ((Boolean) obj2).booleanValue()) {
			new_offset = Math.min(new_offset, element2.getStartOffset());
			new_length = element2.getEndOffset() - new_offset;
		}

		// go through registered processors
		String text = getText(new_offset, new_length);
		String section = (String) getParagraphElement(new_offset).getAttributes().getAttribute("name");

		// if we are deleting a concept, notify reportData
		//if((new_offset != offs || new_length != length) && reportData != null)
		if(reportData != null)
			reportData.processDeletion(text,offs);
		//reportData.setDeletingText(text.trim().length() > 0);

		// if any of the processors objects, them we can't remove
		//int [] offs_len = new int [] {offs,length};
		for (int i = 0; i < reportProcessors.size(); i++) {
			((ReportProcessor) reportProcessors.get(i)).removeString(new_offset, text, section);
		}

		// remove concept formatting from involved text
		setCharacterAttributes(new_offset,new_length,getNormalTextStyle().copyAttributes(),true);
		
		// now do a remove
		//super.remove(new_offset, new_length);
		super.remove(offs,length);
	}
	

	// check if there is a concept at this offset
	private boolean isConcept(int offs) {
		if (offs > 0) {
			Object obj1 = getCharacterElement(offs).getAttributes().getAttribute("concept");
			Object obj2 = getCharacterElement(offs - 1).getAttributes().getAttribute("concept");
			boolean c1 = (obj1 != null && ((Boolean) obj1).booleanValue());
			boolean c2 = (obj2 != null && ((Boolean) obj2).booleanValue());
			return c1 && c2;
		} else {
			return false;
		}
	}

	/**
	 * Here we detect what is typed, and try to parse sentences.
	 */
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
		// if in default mode, do as before
		if (defaultOp) {
			super.insertString(offs, str, a);
			return;
		}

		// don't do anything if in rad-only mode
		if(readOnly)
			return;
		
		
		// flag that typing is in progress for caret
		typing = true;

		// check if we are allowed to insert into this section
		if (!isEditable(offs))
			return;

		// make sure we don't insert text in the middle of the label
		if (isConcept(offs))
			return;

		//if(str == null || str.length() == 0)
		//	System.err.println("Empty string?");

		// extract section name that is being edited
		//String section  = (String) a.getAttribute("name");
		// in java 1.5 null is passed as an attribute, hence we should get it from document
		String section = (String) getParagraphElement(offs).getAttributes().getAttribute("name");

		// now insert the string (ignore supplied attributeSet
		super.insertString(offs, str,(isEditableAttribute(offs))?textStyle:disabledTextStyle); //a textStyle
		
	
		// go through registered processors
		for (int i = 0; i < reportProcessors.size(); i++)
			reportProcessors.get(i).insertString(offs, str, section);
		
	}

	/**
	 * Detect when caret is updated.
	 * Now, we want to distingish between caret change due to mouse event vs. typing
	 * since caret change happens after string insertion, we check for that
	 */
	public void caretUpdate(CaretEvent e) {
		int offs = e.getDot();
		if (!typing) {
			// if caret is in the middle of the concept
			// then move caret to one of the edges
			/*
			if (isConcept(offs)) {
				Element el = getCharacterElement(offs);
				//System.out.println("caret update? "+offs+" "+el.getStartOffset()+" "+el.getStartOffset());
				if ((offs - el.getStartOffset()) > (el.getEndOffset() - offs)) {
					textEditor.setCaretPosition(el.getStartOffset());
				} else {
					textEditor.setCaretPosition(el.getEndOffset());
				}
			} else {
			*/
				//lastOffset = e.getDot();
				// go through registered processors
				for (int i = 0; i < reportProcessors.size(); i++)
					((ReportProcessor) reportProcessors.get(i)).updateOffset(offs);
			//}
		}
		typing = false;
	}

	/**
	 * Handle text replacement, this should be use to attach label to text, instead of setCharacterAttribute
	 */
	public void replaceText(int offs, int length, String text, AttributeSet attr) throws BadLocationException {
		//System.out.println("caret before: "+textEditor.getCaretPosition());
		defaultOp = true;
		int p = textEditor.getCaretPosition();
		super.replace(offs, length, text,(attr!=null)?attr:(isEditableAttribute(offs))?textStyle:disabledTextStyle);
		textEditor.setCaretPosition(p);
		defaultOp = false;
		//System.out.println("caret after: "+textEditor.getCaretPosition());
	}

	
	/**
	 * set default operation
	 * @param b
	 */
	void setDefaultOp(boolean b){
		defaultOp = b;
	}
	
	
	/**
	 * Handle text replacement, unlike similar method it should process its input
	 */
	public void replaceText(int offs, int length, String text) throws BadLocationException {
		//defaultOp = true;
		int p = textEditor.getCaretPosition();
		super.replace(offs, length, text, null);
		textEditor.setCaretPosition(p);
		//defaultOp = false;
	}

	/**
	 * This is invoked when report is finished
	 */
	public void finishReport() {
		// go through registered processors
		for (int i = 0; i < reportProcessors.size(); i++)
			((ReportProcessor) reportProcessors.get(i)).finishReport();
	}

	/**
	 * Get report data object
	 */
	public ReportData getReportData() {
		return reportData;
	}
	
	/**
	 * Get report data object
	 */
	public void setReportData(ReportData r) {
		reportData = r;
	}
	
	public String[] getEditableSections() {
		return editableSections.toArray(new String[0]);
	}
	
	public String[] getSections() {
		return sections.toArray(new String[0]);
	}
	
	
	/**
	 * @return the disabledTextStyle
	 */
	public static MutableAttributeSet getDisabledTextStyle() {
		return disabledTextStyle;
	}

	/**
	 * @return the headerStyle
	 */
	public static MutableAttributeSet getHeaderTextStyle() {
		return headerStyle;
	}

	/**
	 * @return the textStyle
	 */
	public static MutableAttributeSet getNormalTextStyle() {
		return textStyle;
	}

	/**
	 * @return the textEditor
	 */
	public JTextPane getTextEditor() {
		return textEditor;
	}

	/**
	 * clear background
	 */
	public void clearBackground(){
		setCharacterAttributes(0,getLength(),backgroundStyle,false);
	}
	
	/**
	 * read only flag
	 * @param b
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
	}
}
