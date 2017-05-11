package edu.pitt.dbmi.tutor.modules.presentation;

import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.DIAGNOSES;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.DIAGNOSTIC_FEATURES;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.HAS_EXAMPLE;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getConceptClass;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getDiagnosticFindings;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getExampleURL;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.getFeature;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.isDisease;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.isSystemClass;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.interfaces.RadiologyConcept;
import edu.pitt.dbmi.tutor.ui.DomainSelectorPanel;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.terminology.lexicon.Concept;



public class DynamicBook implements PresentationModule, ListSelectionListener, ActionListener {
	private Properties defaultConfig;
	private Tutor tutor;
	private JPanel panel;
	private ExpertModule expertModule;
	private boolean interactive;
	private JTextField search;
	private JList diagnosisList,findingsList, relatedList;
	private JEditorPane definition;
	private JProgressBar progress;
	private FilterDocument filter;
	private JScrollPane relationScroll;
	private IOntology ontology;
	private JDialog dialog;
	private JPopupMenu popup;
	
	public void setExpertModule(ExpertModule module) {
		expertModule = module;
		load();
	}

	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	public Component getComponent() {
		if(panel == null){
			// create left side
			JPanel leftPanel = new JPanel();
			leftPanel.setLayout(new BorderLayout());
			search = new JTextField();
			
			JPanel sp = new JPanel();
			sp.setLayout(new BorderLayout());
			sp.add(search,BorderLayout.CENTER);
			sp.setBorder(getTitle("Search"));
			leftPanel.add(sp,BorderLayout.NORTH);
			diagnosisList = new JList();
			diagnosisList.addListSelectionListener(this);
			//diagnosisList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			findingsList = new JList();
			findingsList.addListSelectionListener(this);
			
			filter = new FilterDocument(search);
			search.setDocument(filter);
			
			JScrollPane dscroll = new JScrollPane(diagnosisList);
			//dscroll.setBackground(Color.white);
			dscroll.setBorder(getTitle("Diagnoses"));
			dscroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			
			JScrollPane fscroll = new JScrollPane(findingsList);
			//fscroll.setBackground(Color.white);
			fscroll.setBorder(getTitle("Findings"));
			fscroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			
			JPanel lp = new JPanel();
			lp.setLayout(new BoxLayout(lp,BoxLayout.Y_AXIS));
			lp.add(dscroll);
			lp.add(fscroll);
			
			leftPanel.add(lp,BorderLayout.CENTER);
			
			
			// create right side
			definition = new UIHelper.HTMLPanel();
			definition.setEditable(false);
			definition.addHyperlinkListener(new HyperlinkListener() {
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if(HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())){
						String name = e.getURL().toString();
						if(name.lastIndexOf("/") > -1)
							name = name.substring(name.lastIndexOf("/")+1);
						JLabel lbl = new JLabel(new ImageIcon(e.getURL()));
						JScrollPane ip = new JScrollPane(lbl);
						ip.setPreferredSize(new Dimension(500,500));
						ImageScroller iscroller = new ImageScroller(ip);
						lbl.addMouseListener(iscroller);
						lbl.addMouseMotionListener(iscroller);
						
						doNotifyViewExample(name);
						
						JOptionPane.showMessageDialog(getComponent(),ip,name,JOptionPane.PLAIN_MESSAGE);
					}
				}
			});
			definition.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e) {
					if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
						getPopupMenu().show(definition,e.getX(),e.getY());
					}
				}
			});
			JScrollPane ds = new JScrollPane(definition);
			ds.setBorder(getTitle("Glossary Information"));
			
			
			// tree panel
			relatedList = new JList();
			//relatedList.addListSelectionListener(this);
			relatedList.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,boolean cellHasFocus) {
					JLabel lbl =  (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
					if(value instanceof String && value.toString().matches("\\[.*\\]")){
						lbl.setBackground(Color.lightGray);
					}else{
						lbl.setBackground(Color.white);
					}
					return lbl;
				}
			});
			relatedList.addMouseMotionListener(new MouseMotionAdapter(){
				public void mouseMoved(MouseEvent e) {
					relatedList.setToolTipText(null);
					int i = relatedList.locationToIndex(e.getPoint());
					if(i > -1){
						Object o = relatedList.getModel().getElementAt(i);
						if(o instanceof DynamicConcept && !((DynamicConcept)o).getEntireConcepts().isEmpty()){
							relatedList.setToolTipText(TextHelper.toString(((DynamicConcept)o).getEntireConcepts()));
						}
					}
				}
				
			});
			relatedList.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e) {
					if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
						getPopupMenu().show(relatedList,e.getX(),e.getY());
					}
				}
			});
			relationScroll = new JScrollPane(relatedList);
			relationScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			relationScroll.setBorder(getTitle("Related Concepts"));
			
			int WIDTH = 1000;
			
			JSplitPane rightPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,ds,relationScroll);
			rightPanel.setResizeWeight(.5);
			rightPanel.setDividerLocation(WIDTH/3);
			
			// create progress
			progress = new JProgressBar();
			progress.setIndeterminate(true);
			progress.setString("Compiling Glossary List, Please Wait ...");
			progress.setStringPainted(true);
			
			// create panel
			panel = new JPanel();
			panel.setBackground(Color.white);
			panel.setLayout(new BorderLayout());
			
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,leftPanel,rightPanel);
			split.setBackground(Color.white);
			split.setPreferredSize(new Dimension(WIDTH,600));
			split.setResizeWeight(.5);
			split.setDividerLocation(WIDTH/3);
			
			
			panel.add(progress,BorderLayout.SOUTH);
			panel.add(split,BorderLayout.CENTER);
		}
		return panel;
	}

	/**
	 * get menu for this interface
	 * @return
	 */
	public JPopupMenu getPopupMenu(){
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("Copy","Copy",Config.getIconProperty("icon.menu.copy"),this));
		}
		return popup;
	}
	
	/**
	 * scrolls the image with DND
	 * @author tseytlin
	 */
	private class ImageScroller extends MouseAdapter implements MouseMotionListener {
		private JScrollPane scroll;
		private JComponent comp;
		private Point st;
		public ImageScroller(JScrollPane scroll){
			this.scroll = scroll;
		}
		public void mousePressed(MouseEvent e){
			if(comp == null)
				comp = (JComponent) e.getSource();
			st = e.getPoint();
			comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		
		public void mouseDragged(MouseEvent e) {
			JViewport view = scroll.getViewport();
			Rectangle r = view.getBounds();
			Point p = e.getPoint();
			int x = view.getViewPosition().x + (st.x - p.x);
			int y = view.getViewPosition().y + (st.y - p.y);
			comp.scrollRectToVisible(new Rectangle(x,y,r.width,r.height));
		}
		public void mouseMoved(MouseEvent e){}
		public void mouseReleased(MouseEvent arg0) {
			comp.setCursor(Cursor.getDefaultCursor());
		}
		
	}
	
	
	/**
	 * selection changed in list
	 */
	public void valueChanged(ListSelectionEvent evt){
		// make sure we change things only once
		if(!evt.getValueIsAdjusting()){
			doDisplayConceptInfo(((JList)evt.getSource()).getSelectedValues());
				
			// unselect the other list
			if(!diagnosisList.equals(evt.getSource()))
				diagnosisList.clearSelection();
			if(!findingsList.equals(evt.getSource()))
				findingsList.clearSelection();
		}
	}
	
	public void doDisplayConceptInfo(DynamicConcept con){
		final DynamicConcept c = con;
	
		// create text
		StringBuffer str = new StringBuffer();
		
		if(c.getExample() != null){
			//load image to get dimensions
			ImageIcon img = new ImageIcon(c.getExample());
			int w = img.getIconWidth();
			int h = img.getIconHeight();
			String a = "";
			if(w > 250){
				h = 250 * h/w;
				w = 250;
				a = "<a href="+c.getExample()+">";
			}else if(h > 300){
				w = 300 * w / h;
				h = 300;
				a = "<a href="+c.getExample()+">";
			}
			str.append("<center>"+a+"<img border=2 width="+w+" height="+h+" src="+c.getExample()+">"+(a.length()>0?"</a>":"")+"</center><br>");
		}
			
			
		str.append("<h2>"+c.getName()+"</h2><hr>");
		str.append(c.getDefinition()+"<br>");
		
		
		//if(TYPE_FINDING.equals(c.getType())){
		str.append("<br><br>");
		str.append("<i>"+(TYPE_DIAGNOSIS.equals(c.getType())?"DIAGNOSES":"FINDINGS")+" MORE GENERAL THEN:</i>");
		str.append("<ul><li>"+c.getParent()+"</li></ul>");
		
		String children = ""+getTree(c.getConcept().getConceptClass());
		if(!TextHelper.isEmpty(children)){
			str.append("<i>"+(TYPE_DIAGNOSIS.equals(c.getType())?"DIAGNOSES":"FINDINGS")+" MORE SPECIFIC THEN:</i>");
			str.append(children);
		}
				
		// set definition
		definition.setText(str.toString());
		definition.setCaretPosition(0);
	
		// set related findings
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Vector data = new Vector();
				for(Set list : c.getRelatedConcepts()){
					data.addAll(list);
					data.add(" ");
				}
				relatedList.setListData(data);
				//relatedList.setListData(c.getRelatedConcepts().toArray());
				if(TYPE_DIAGNOSIS.equals(c.getType())){
					relationScroll.setBorder(getTitle("Potentially Associated Findings"));
				}else if(TYPE_FINDING.equals(c.getType())){
					relationScroll.setBorder(getTitle("Potentially Associated Diagnoses"));
				}
			}
		});
		doNotifyView(c);
	}
	
	
	private Border getTitle(String str){
		return new TitledBorder(new LineBorder(Color.gray),str,TitledBorder.CENTER,TitledBorder.ABOVE_TOP,new Font("SansSerif",Font.BOLD,15));
	}
	
	
	private StringBuffer getTree(IClass c) {
		StringBuffer str = new StringBuffer();
		// nothing to do 
		if(c.getDirectSubClasses().length == 0)
			return str;
		
		// else do the thing
		str.append("<ul>");
		IClass p = getFeature(c);
		for(IClass child: c.getDirectSubClasses()){
			IClass f = getFeature(child);
			if(isDisease(c) || p.equals(f))	
				str.append("<li>"+child.getConcept().getName()+getTree(child)+"</li>");
		}
		str.append("</ul>");
		return str;
	}

	/**
	 * display multiplse concepts
	 * @param objs
	 */
	public void doDisplayConceptInfo(Object [] objs){
		// don't do anything on regular selection
		if(objs.length == 0)
			return;
		
		// if only one do normal display
		if(objs.length == 1){
			doDisplayConceptInfo((DynamicConcept)objs[0]);
			return;
		}
	
		// iterate over objs
		List dxRules = new ArrayList();
		Set<DynamicConcept> data = new TreeSet<DynamicConcept>();
		Set<DynamicConcept> input = new TreeSet<DynamicConcept>();
		StringBuffer view = new StringBuffer();
		boolean empty = true;
		boolean isDx = false;
		for(Object o: objs){
			DynamicConcept c = (DynamicConcept) o;
			isDx = TYPE_DIAGNOSIS.equals(c.getType());
			input.add(c);
			
			// append to summary list
			view.append("<b>"+c.getName()+"</b><br>");
			
			// if diagnosis then it is a conjunction of findings
			if(empty || isDx){
				if(isDx){
					// fill in the rules
					dxRules.add("["+c.getName()+"]");
					for(Set list : c.getRelatedConcepts()){
						dxRules.addAll(list);
						dxRules.add(" ");
					}
				}else {
					for(Set list: c.getRelatedConcepts())
						data.addAll(list);
				}
				empty = false;
			// else if finding then it is a disjunction of diagnosis	
			}else {
				// now create a disjunction
				Set<DynamicConcept> ndata = new TreeSet<DynamicConcept>();
				for(DynamicConcept f: data){
					if(!c.getRelatedConcepts().isEmpty() && c.getRelatedConcepts().get(0).contains(f))
						ndata.add(f);
					
				}
				data = ndata;
			}
		}
		final String DX_BLURB = "When multiple diagnoses are selected, the list on the right " +
								"reflects their respective related findings.";
		final String FN_BLURB = "When multiple findings are selected, the list on the right "+
								"displays all diagnoses that can be inferred from the selected set of findings";
		final String title = (isDx)?"Potentially Associated Findings":"Potentially Associated Diagnoses";
		String blurb = "<br><center><table bgcolor=\"#FFFFCC\" width=\"95%\"><tr><td>"+((isDx)?DX_BLURB:FN_BLURB)+"</td></tr></table></center><br><hr>";
		
		// set definition
		definition.setText(blurb+view);
		definition.setCaretPosition(0);
		
		
		// set related findings
		final Object [] ldata = (isDx)?dxRules.toArray():data.toArray();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				relatedList.setListData(ldata);
				relationScroll.setBorder(getTitle(title));
			}
		});
				
		doNotifyInference(input,data);
	}
	
	/**
	 * load concepts and their definitions
	 */
	public void load() {
		// if we are connected to a tutor add a button to interface
		if(tutor != null && tutor.getInterfaceModule() != null){
			addButton(tutor.getInterfaceModule().getToolBar());
		}
		
		// load ontology
		if(expertModule != null && expertModule.getDomainOntology() != null)
			loadOntology();
	}
	
	/**
	 * add dynamic button to toolbar
	 * @param toolBar
	 */
	private void addButton(JToolBar toolbar) {
		if(UIHelper.hasButton(toolbar,"DynamicBook"))
			return;
		
		JButton bt = UIHelper.createButton("DynamicBook","Open Glossary Browser",
		UIHelper.getIcon(Config.getProperty("icon.toolbar.book"),24),new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// get expert module from tutor
				if(tutor != null && expertModule == null)
					setExpertModule(tutor.getExpertModule());
				
				// load everything
				if(expertModule != null){
					if(ontology == null || !ontology.equals(expertModule.getDomainOntology())){
						loadOntology();
					}
				
					// show dialog
					showDialog();
				}
				
			}
		});
		
		// insert it before done
		toolbar.add(Box.createRigidArea(new Dimension(10,10)),toolbar.getComponentCount()-2);
		toolbar.add(bt,toolbar.getComponentCount()-2);
	}

	/**
	 * load concepts and their definitions
	 */
	private void loadOntology() {
		//	 make sure we init interface
		getComponent();
		
		// now, load from server
		(new Thread(new Runnable(){
			public void run(){
				
				// iterate over tree 
				IOntology ont = expertModule.getDomainOntology();
				
				// get all diagnostic findings
				Map<IClass,DynamicConcept> findingMap= new HashMap<IClass, DynamicConcept>();
				for(IClass cls: ont.getClass(DIAGNOSTIC_FEATURES).getSubClasses()){
					if(!isSystemClass(cls) && expertModule.getConceptFilter().accept(DIAGNOSTIC_FEATURES,cls.getName()) && cls.equals(getFeature(cls))){
						findingMap.put(cls,new DynamicConcept(cls));
					}
				}
				final Set<DynamicConcept> findingsSet = new TreeSet(findingMap.values());
				filter.addListData(findingsList,findingsSet);
				
				// add all diagnosis
				Map<IClass,DynamicConcept> diagnosisMap= new HashMap<IClass, DynamicConcept>();
				for(IClass cls: ont.getClass(DIAGNOSES).getSubClasses()){
					if(!isSystemClass(cls) && expertModule.getConceptFilter().accept(DIAGNOSES,cls.getName())){
						diagnosisMap.put(cls,new DynamicConcept(cls));
					}
				}
				final Set<DynamicConcept> diagnosisSet = new TreeSet(diagnosisMap.values());
				filter.addListData(diagnosisList,diagnosisSet);
			
				
				// analyze diagnosis rules
				for(IClass dx: ont.getClass(DIAGNOSES).getSubClasses()){
					if(!dx.getEquivalentRestrictions().isEmpty()){
						DynamicConcept c = diagnosisMap.get(dx);
						if(c != null){
							// iterate over a list of related findings
							if(dx.getEquivalentRestrictions().getExpressionType() == ILogicExpression.OR){
								int i = 0;
								for(Object o: dx.getEquivalentRestrictions()){
									if(o instanceof ILogicExpression){
										for(ConceptEntry e: getDiagnosticFindings((ILogicExpression) o,ont)){
											IClass f = getFeature(getConceptClass(e,ont));
											DynamicConcept df = findingMap.get(f);
											if(df != null){
												DynamicConcept cd = df.clone();
												cd.getEntireConcepts().add(e.getText());
												if(e.isAbsent()){
													cd.setAbsent(true);
													c.addRelatedConcept(i,cd);
												}else
													c.addRelatedConcept(i,cd);
												
												if(!e.isAbsent())
													df.addRelatedConcept(0,c);
											}
										}
									}
									i++;
								}
							}else{
								for(ConceptEntry e: getDiagnosticFindings(new ConceptEntry(dx.getName(),TYPE_DIAGNOSIS),ont)){
									IClass f = getFeature(getConceptClass(e,ont));
									DynamicConcept df = findingMap.get(f);
									if(df != null){
										DynamicConcept cd = df.clone();
										cd.getEntireConcepts().add(e.getText());
										if(e.isAbsent()){
											cd.setAbsent(true);
											c.addRelatedConcept(0,cd);
										}else
											c.addRelatedConcept(0,cd);
										if(!e.isAbsent())
											df.addRelatedConcept(0,c);
									}
								}
							}
							
						}
					}
				}
				
				// remember ontology
				ontology = ont;
				
				// remove progress bar
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						((JPanel)getComponent()).remove(progress);
						
						// update filter
						findingsList.setListData(findingsSet.toArray());
						diagnosisList.setListData(diagnosisSet.toArray());
						
						// repaing
						((JPanel)getComponent()).revalidate();
						((JPanel)getComponent()).repaint();
					}
				});
				
			}
		})).start();
		
	}
	
	
	
	
	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Tutor getTutor() {
		return tutor;
	}

	public boolean isEnabled() {
		return (panel == null)?false:panel.isEnabled();
	}

	/**
	 * enable/disable panel
	 * @param b
	 */
	public void setEnabled(boolean b){
		if(panel != null){
			setInteractive(b);
			panel.setEnabled(b);
		}
	}


	/**
	 * set panel interactive flag
	 * @param b
	 */
	public void setInteractive(boolean b){
		interactive = b;
	}
	
	/**
	 * is interactive
	 * @return
	 */
	public boolean isInteractive(){
		return interactive;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Provides users with a separate window for browsing glossary information (definitions, pictures, etc.) " +
				"about the selected domain as well as relationships between findings and diagnoses.";
	}

	public String getName() {
		return "Dynamic Book";
	}

	public Action[] getSupportedActions() {
		return new Action [0];
	}

	public Message[] getSupportedMessages() {
		return new Message [0];
	}

	public String getVersion() {
		return "1.0";
	}

	public Object getIdentifiedFeature() {
		return null;
	}
	public boolean isFeatureIdentified() {
		return false;
	}
	public void receiveMessage(Message msg) {}
	public void removeIdentifiedFeature(Object obj) {}
	public void setCaseEntry(CaseEntry problem) {}
	public void setFeatureMarker(String type) {}
	public void setIdentifiedFeature(Shape shape) {}
	public void startFeatureIdentification() {}
	public void stopFeatureIdentification() {}
	public void sync(PresentationModule tm) {}
	public void reset() {}
	public void resolveAction(Action action) {}
	public void reconfigure() {	}
	public void dispose() {	}

	
	/**
	 * This class represents a concept
	 * @author tseytlin
	 */
	private class DynamicConcept implements Comparable {
		private String text, type;
		private List<String> entireConcepts;
		private URL example;
		private boolean absent;
		
		private Concept c;
		private List<Set<DynamicConcept>> relatedConcepts;
		
		
		// create a dynamic concept
		public DynamicConcept(IClass cls){
			c = cls.getConcept();
			text = c.getName();
			if(isDisease(cls))
				text = text.toUpperCase();
			relatedConcepts = new ArrayList<Set<DynamicConcept>>();//new TreeSet<DynamicConcept>();
			type = isDisease(cls)?TYPE_DIAGNOSIS:TYPE_FINDING;
			
			
			
			// set examples
			Object [] values = cls.getPropertyValues(cls.getOntology().getProperty(HAS_EXAMPLE));
			URL url = getExampleURL(cls.getOntology());
			if(values.length > 0){
				try {
					example = new URL(url+"/"+values[0]);
				} catch (MalformedURLException e) {
					//e.printStackTrace();
				}
			}
			
		}
		
		public DynamicConcept clone(){
			return new DynamicConcept(c.getConceptClass());
		}
		

		public boolean isAbsent() {
			return absent;
		}

		public List<String> getEntireConcepts() {
			if(entireConcepts == null)
				entireConcepts = new ArrayList<String>();
			return entireConcepts;
		}
		
		public void setAbsent(boolean absent) {
			this.absent = absent;
		}
		
		public ConceptEntry getConceptEntry(){
			return new ConceptEntry(c.getConceptClass().getName(),isAbsent()?TYPE_ABSENT_FINDING:type);
		}
		
		public void addRelatedConcept(int x, DynamicConcept r){
			if(x >= relatedConcepts.size())
				relatedConcepts.add(x,new TreeSet<DynamicConcept>());
			relatedConcepts.get(x).add(r);
		}
		
		public List<Set<DynamicConcept>> getRelatedConcepts(){
			return relatedConcepts;
		}
		
		public String getType() {
			return type;
		}
		public String getParent(){
			return c.getConceptClass().getDirectSuperClasses()[0].getConcept().getName();
		}
		
		// string representation of concepts
		public String toString(){
			return (isAbsent()?"NO ":"")+text;
		}
		// for sorting
		public int compareTo(Object o){
			return toString().compareToIgnoreCase(o.toString());
		}
		
		public Concept getConcept(){
			return c;
		}
		
		public boolean equals(Object o){
			if(o instanceof DynamicConcept){
				return c.equals(((DynamicConcept)o).getConcept());
			}
			return false;
		}
		public int hashCode() {
			return c.hashCode();
		}

		/**
		 * @return the definition
		 */
		public String getDefinition() {
			return c.getDefinition();
		}
	
		/**
		 * @return the name
		 */
		public String getName() {
			return toString();
		}
		
		/**
		 * @return the synonyms
		 */
		public String[] getSynonyms() {
			return c.getSynonyms();
		}
		
		/**
		 * @return the example
		 */
		public URL getExample() {
			return example;
		}		
		
		public String getOriginalName(){
			return c.getConceptClass().getName();
		}
	}
	
	/**
	 * notify search
	 * @param text
	 */
	private void doNotifySearch(String s){
		if(s.length() > 0){
			InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_BOOK,s,ACTION_SEARCHING);
			MessageUtils.getInstance(this).addInterfaceEvent(ie);
		}
	}
	
	/**
	 * notify search
	 * @param text
	 */
	private void doNotifyViewExample(String s){
		if(s.length() > 0){
			Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_BOOK,s,ACTION_VIEW_EXAMPLE));
		}
	}
	
	
	/**
	 * notify view
	 * @author tseytlin
	 *
	 */
	private void doNotifyView(DynamicConcept c){
		// create ce
		ClientEvent ce = new ClientEvent();
		ce.setType(c.getType());
		ce.setLabel(c.getOriginalName());
		ce.setAction(ACTION_GLOSSARY);
		ce.setSender(this);
		ce.setTimestamp(System.currentTimeMillis());
	
		MessageUtils.getInstance(this).flushInterfaceEvents(ce);
		Communicator.getInstance().sendMessage(ce);
	}
	
	/**
	 * notify view
	 * @author tseytlin
	 *
	 */
	private void doNotifyInference(Collection<DynamicConcept> input,Collection<DynamicConcept> data){
		if(input.isEmpty())
			return;
		
		String type = null;
		List<String> labels = new ArrayList<String>();
		List<String> related = new ArrayList<String>();
		// create labels
		for(DynamicConcept dc: input){
			type = dc.getType();
			labels.add(dc.getOriginalName());
		}
		
		// create related
		for(DynamicConcept dc : data){
			related.add(dc.getOriginalName());
		}
		
		// build client event
		ClientEvent ce = ClientEvent.createClientEvent(this,type,TextHelper.toString(labels),ACTION_INFERENCE);
		Map map = (Map) ce.getInput();
		if(TYPE_FINDING.equals(type)){
			map.put(TYPE_FINDING,TextHelper.toString(labels));
			map.put("Inferred"+TYPE_DIAGNOSIS,TextHelper.toString(related));
		}else if(TYPE_DIAGNOSIS.equals(type)){
			map.put(TYPE_DIAGNOSIS,TextHelper.toString(labels));
			map.put("Inferred"+TYPE_FINDING,TextHelper.toString(related));
		}
		Communicator.getInstance().sendMessage(ce);
	}
	
	/**
	 * show dialog
	 */
	public void showDialog(){
		// create frame
		if(dialog == null){
			Icon icon = UIHelper.getIcon(Config.getProperty("icon.toolbar.book"));
			JOptionPane op = new JOptionPane(getComponent(),JOptionPane.PLAIN_MESSAGE);
			dialog = op.createDialog(Config.getMainFrame(),getName());
			dialog.setModal(true);
			if(icon != null && icon instanceof ImageIcon)
	        	dialog.setIconImage(((ImageIcon)icon).getImage());
		}
		InterfaceEvent ie = InterfaceEvent.createInterfaceEvent(this,TYPE_DIALOG,getName(),ACTION_OPENED);
		Communicator.getInstance().sendMessage(ie);
		
		dialog.setVisible(true);
		
		// throw interface event
		InterfaceEvent ie2 = InterfaceEvent.createInterfaceEvent(this,TYPE_DIALOG,getName(),ACTION_CLOSED);
		Communicator.getInstance().sendMessage(ie2);
	}
	
	
	//	 this is for input project
	private class FilterDocument extends PlainDocument {
		private JTextField textField;
		private Map<JList,Collection> listMap;
		
		public FilterDocument(JTextField text){
			this.textField = text;
			this.listMap = new HashMap<JList,Collection>();
		}
		
		/**
		 * set list of available items
		 * @param l
		 */
		public void addListData(JList list, Collection data){
			listMap.put(list,data);
		}
		
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offs, str, a);
			if(str.length() == 1)
				filter();
 	    }
		public void remove(int offs,int len) throws BadLocationException {
			super.remove(offs,len);
			filter();
		}
		public void replace(int offs,int len,String txt,AttributeSet a) throws BadLocationException{
			super.remove(offs,len);
			insertString(offs,txt,a);
		}
		
		public void filter(){
			textField.setForeground(Color.black);
			String text = textField.getText().toLowerCase();	
			
			// iterate over lists
			boolean found = false;
			for(JList list: listMap.keySet()){
				final Vector v = new Vector();
				for(Object o : listMap.get(list)){
					if(compare(o,text))
						v.add(o);
				}
				
				if(!v.isEmpty())
					found = true;
				
				// update list
				final JList l = list;
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						l.setListData(v);
						if(v.size() == 1)
							l.setSelectedIndex(0);
					}
				});
				
			}
			
			if(!found)
				textField.setForeground(Color.red);
			
			// notify search
			doNotifySearch(text);
		}
		
		// compare object in list to text
		public boolean compare(Object obj, String text){
			//return obj.toString().toLowerCase().startsWith(text);
			if(obj.toString().toLowerCase().contains(text)){
				return true;
			}else if(obj instanceof DynamicConcept){
				// check parent
				DynamicConcept c = (DynamicConcept) obj;
				//if(c.getParent() != null && c.getParent().toLowerCase().contains(text))
				//	return true;
				// check synonms
				String [] syn = ((DynamicConcept) obj).getSynonyms();
				if(syn != null && syn.length >0){
					for(int i=0;i<syn.length;i++){
						if(syn[i].contains(text)){
							return true;
						}
					}
				}
			}
			return false;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config.setProperty("file.manager.server.url", "http://slidetutor.upmc.edu/domainbuilder/FileManagerServlet");
		
		ExpertModule expert = new DomainExpertModule();
		expert.load();
		DomainSelectorPanel domainSelector = new DomainSelectorPanel(expert);
		domainSelector.showChooserDialog();
		if(!domainSelector.isSelected())
			return;
		
		// open selected domain
		expert.openDomain((String) domainSelector.getSelectedObject());
		
		DynamicBook db = new DynamicBook();
		db.setExpertModule(expert);
				
		// create frame
		Icon icon = UIHelper.getIcon(Config.getProperty("icon.toolbar.book"));
		JFrame frame = new JFrame(db.getName()); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if(icon != null && icon instanceof ImageIcon)
        	frame.setIconImage(((ImageIcon)icon).getImage());
		frame.getContentPane().add(db.getComponent());
		frame.pack();
		frame.setVisible(true);
	}


	public void actionPerformed(ActionEvent e) {
		if("Copy".equals(e.getActionCommand())){
			StringBuffer buff = new StringBuffer();
			
			Component c = getPopupMenu().getInvoker();
			if(c == relatedList){
				// if related list selected
				for(int i=0;i<relatedList.getModel().getSize();i++)
					buff.append(relatedList.getModel().getElementAt(i)+"\n");
			}else if(c == definition){
				// if defintion
				String text = definition.getSelectedText();
				buff.append((text != null)?text:definition.getText());
			}
			
			// copy to clipboad
			StringSelection ss = new StringSelection(buff.toString());
		    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
		}
		
	}

}
