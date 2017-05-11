package edu.pitt.dbmi.tutor.ui;

import static edu.pitt.dbmi.tutor.util.OntologyHelper.getCasePath;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IOntologyException;
import edu.pitt.ontology.IRepository;
import edu.pitt.ontology.protege.ProtegeRepository;
import edu.pitt.ontology.ui.OntologyExplorer;


/**
 * domain selector
 * @author tseytlin
 */

public class DomainSelectorPanel extends JPanel 
	implements ListSelectionListener, ActionListener, ItemListener {
	private IRepository repository;
	private ExpertModule expert;
	private JList organs,domains;
	private JComboBox institution;
	private JTextField preview;
	private JProgressBar progress;
	private JButton okButton,cancelButton;
	private JDialog dialog;
	private JOptionPane op;
	private Frame frame;
	private boolean ok;
	private Map<String,Vector<String>> ontologyMap;
	private Map<String,Integer> caseCounts,solvedCounts;
	private Vector<String> organList,institutionList;
	private String baseURL = OntologyHelper.DEFAULT_BASE_URI;
	private static URI lockedOntology;

	
	public DomainSelectorPanel(IRepository repository){
		this(repository,false);
	}
	
	public DomainSelectorPanel(IRepository repository, boolean manage){
		super();
		this.repository = repository;
		createInterface(manage);
		load();
		caseCounts = new HashMap<String,Integer>();
		solvedCounts = new HashMap<String,Integer>();
	}
	
	
	public DomainSelectorPanel(ExpertModule expert){
		super();
		this.repository = null;
		this.expert = expert;
		createInterface(false);
		caseCounts = new HashMap<String,Integer>();
		solvedCounts = new HashMap<String,Integer>();
		//load();
	}
	
	
	/**
	 * create UI
	 */
	private void createInterface(boolean manage){
		setLayout(new BorderLayout());
		
		institution = new JComboBox(new DefaultComboBoxModel());
		institution.setBorder(new TitledBorder("Institiution"));
		institution.addItemListener(this);
		
		organs = new JList(new DefaultListModel());
		organs.addListSelectionListener(this);
		organs.setVisibleRowCount(15);
		domains = new JList();
		domains.setVisibleRowCount(15);
		domains.addListSelectionListener(this);
		domains.setCellRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,boolean cellHasFocus) {
				JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String text = value.toString();
				if(text.matches("http://.*\\.owl")){
					int n = -1;
					int r = 0;
					
					if(caseCounts.containsKey(text))
						n = caseCounts.get(text);
					if(solvedCounts.containsKey(text))
						r = solvedCounts.get(text);
					
					// do pretty count
					String count =  "";
					if(n > -1){
						if(n > 0)
							count = " <font color=\"green\">"+Math.max(n-r,0)+"</font> / "+n+" cases";
						else
							count = " <font color=\"red\">"+n+"</font> cases";
					}
					
					String fc = (n <= 0)?"gray":"black";
					c.setText("<html><table width=\"450\" cellspacing=0 cellpadding=0><tr><td>"+
							"<font color="+fc+">"+getPrettyName(TextHelper.getName(text))+"</font>"+
							"</td><td align=\"right\">"+count+"</td></tr></table>");
				}
				return c;
			}
			
		});
		
		preview = new JTextField();
		preview.setEditable(false);
		preview.setForeground(Color.blue);
		JScrollPane s1 = new JScrollPane(organs);
		s1.setPreferredSize(new Dimension(150,200));
		s1.setBorder(new TitledBorder("Organ"));
		JScrollPane s2 = new JScrollPane(domains);
		s2.setBorder(new TitledBorder("Domain"));
		s2.setPreferredSize(new Dimension(500,200));
		add(institution,BorderLayout.NORTH);
		add(s1,BorderLayout.WEST);
		add(s2,BorderLayout.CENTER);
		if(manage)
			add(createToolBar(),BorderLayout.EAST);
		add(preview,BorderLayout.SOUTH);
		
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		
		
		// create popup
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		okButton.setEnabled(false);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		op = new JOptionPane(this,JOptionPane.PLAIN_MESSAGE,
		JOptionPane.OK_CANCEL_OPTION,null,new Object []{okButton,cancelButton});
		
		// add dobulic click listener
		domains.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(domains.getSelectedValue() != null && e.getClickCount() == 2)
					okButton.doClick();
			}
		});
			
	}
	
	/**
	 * make a very pretty name by splitting camel back notation
	 * @param name
	 * @return
	 */
	private String getPrettyName(String name){
		return name.replaceAll("([a-z])([A-Z])","$1 $2");
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		JComponent c = this;
		if(busy){
			c.remove(preview);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			progress.setString(null);
			progress.setIndeterminate(true);
			c.remove(progress);
			c.add(preview,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	private JToolBar createToolBar(){
		JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
		toolbar.add(UIHelper.createButton("New",
				"Create New Domain Ontology",
				UIHelper.getIcon(Config.getProperty("icon.toolbar.new"),24),this));
		toolbar.add(UIHelper.createButton("Browse",
				"Browse Domain Ontology",
				UIHelper.getIcon(Config.getProperty("icon.toolbar.browse"),24),this));
		toolbar.add(UIHelper.createButton("Import",
				"Import Domain Ontology from OWL file",
				UIHelper.getIcon(Config.getProperty("icon.toolbar.import"),24),this));
		toolbar.add(UIHelper.createButton("Export",
				"Export Domain Ontology to OWL file",
				UIHelper.getIcon(Config.getProperty("icon.toolbar.export"),24),this));
		toolbar.add(UIHelper.createButton("Delete",
				"Remove Domain Ontology",
				UIHelper.getIcon(Config.getProperty("icon.toolbar.delete"),24),this));
		
				
		return toolbar;
	}
	
	/**
	 * load ontology data
	 */
	public void load(){
		// clear everything
		organList = new Vector<String>();
		institutionList = new Vector<String>();
		organs.setListData(organList);
		domains.setListData(new Vector());
		institution.removeAllItems();
		preview.setText("");
		
		// iterate over ontologies and create a map
		ontologyMap = new HashMap<String,Vector<String>>();
		Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
		
		for(String url: getOntologies()){
			// skip KnowledgeBase and instance ontologies
			//String url = ""+ont.getURI();
			if( url.endsWith(OntologyHelper.KNOWLEDGE_BASE) || 
			    url.endsWith(OntologyHelper.INSTANCES_ONTOLOGY)){
				continue;
			}
			// now parse url to extract relevant info
			// (it better be formated right)
			Matcher m = pt.matcher(url);
			if(m.matches()){
				baseURL = m.group(1);
				String organ = m.group(2);
				String inst  = m.group(3);
				// add to models
				if(!organList.contains(organ))
					organList.add(organ);
				if(!institutionList.contains(inst)){
					//institution.addItem(inst);
					institutionList.add(inst);
				}
				// put into hashmap
				Vector<String> list = ontologyMap.get(organ+"/"+inst);
				if(list == null){
					list = new Vector<String>();
					ontologyMap.put(organ+"/"+inst,list);
				}
				list.add(url);
				
				if(expert != null)
					caseCounts.put(url,getCaseCount(url));
			}
		}
		Collections.sort(institutionList);
		Collections.sort(organList);
		
		//organs.setListData(organList);
		//organs.setSelectedIndex(0);
		
		// load new data
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				//organs.setListData(organList);
				//domains.setListData(new Vector());
				institution.removeAllItems();
				preview.setText("");
				
				// add stuff to lists
				for(String inst: institutionList)
					institution.addItem(inst);
				organs.setListData(organList);
				
				//setup defaults
				organs.setSelectedIndex(0);
			}
		});
	
	}
	
	/**
	 * set all visited cases
	 * @param list
	 */
	public void setVisitedCases(Collection<String> list){
		if(solvedCounts == null)
			return;
		
		// create counts for visited cases
		solvedCounts.clear();
		
		// for all domains
		for(String domain: getOntologies()){
			int n = 0;
		
			String p = getCasePath(URI.create(domain));
			for(String s :list){
				if(s.contains(p)){
					n ++;
				}
			}
			solvedCounts.put(domain,n);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				domains.revalidate();
				domains.repaint();
			}
		});
		
	}
	
	/**
	 * get ontology URLs
	 * @return
	 */
	private String [] getOntologies(){
		if(repository != null){
			IOntology [] onts  = repository.getOntologies();
			String [] urls = new String [onts.length];
			for(int i=0;i<urls.length;i++){
				// skip KnowledgeBase and instance ontologies
				urls[i] = ""+onts[i].getURI();
			}
			return urls;
		}else if(expert != null){
			return expert.getAvailableDomains();
		}
		return new String [0];
	}
	
	/**
	 * get case count
	 * @param url
	 * @return
	 */
	private int getCaseCount(String url){
		if(expert != null){
			// wait for case meta data
			if(expert instanceof DomainExpertModule){
				((DomainExpertModule)expert).waitForCaseMetaData();
			}
			return expert.getAvailableCases(url).length;
		}
		return 0;
	}
	
	/**
	 * import ontology
	 * @param u
	 */
	private void importOntology(URI u){
		final URI uri = u;
		setBusy(true);
		(new Thread(new Runnable(){
			public void run(){
				try{
					repository.importOntology(uri);
				}catch(Exception ex){
					ex.printStackTrace();
					JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
				}
				load();
				setBusy(false);
			}
		})).start();
	}
	
	
	public Object getSelectedObject() {
		return domains.getSelectedValue();
	}

	public Object[] getSelectedObjects() {
		return new Object [] {getSelectedObject()};
	}

	public boolean isSelected() {
		return ok && !domains.isSelectionEmpty();
	}

	public void setOwner(Frame frame) {
		this.frame = frame;

	}

	public void setSelectionMode(int mode) {
		//do nothing
	}

	public void showChooserDialog() {
		load();
		dialog = op.createDialog(frame,"Open Domain");
		dialog.setVisible(true);
		//int r = JOptionPane.showOptionDialog(frame,this,"Open Domain",
		//		JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		//ok = r == JOptionPane.OK_OPTION;
	}
		
	/**
	 * show new domain dialog
	 */
	public boolean showNewDomainDialog(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JComboBox org = new JComboBox(organList);
		org.setEditable(true);
		org.setBorder(new TitledBorder("Organ"));
		org.setPreferredSize(new Dimension(100,45));
		JComboBox ins = new JComboBox(institutionList);
		ins.setEditable(true);
		ins.setBorder(new TitledBorder("Institution"));
		ins.setPreferredSize(new Dimension(100,45));
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextField name = new JTextField();
		p.setBorder(new TitledBorder("Domain"));
		p.setPreferredSize(new Dimension(200,45));
		p.add(name,BorderLayout.CENTER);
		panel.add(org);
		panel.add(ins);
		panel.add(p);
		int r = JOptionPane.showConfirmDialog(frame,panel,"Create Domain",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		ok = r == JOptionPane.OK_OPTION && name.getText().length() > 0;
		if(ok){
			createNewDoman(""+org.getSelectedItem(),""+ins.getSelectedItem(),name.getText());
		}
		return ok;
	}
	
	/**
	 * create new domain
	 * @param org
	 * @param ins
	 * @param name
	 */
	private void createNewDoman(String org,String ins,String n){
		final String organ = org;
		final String source = ins;
		final String name = n;
		setBusy(true);
		(new Thread(new Runnable(){
			public void run(){
				try{
					String nm = name.trim().replaceAll("\\W","_");
					URI uri = new URI(baseURL+organ+"/"+source+"/"+nm+".owl");
					IOntology ont = repository.createOntology(uri);
					ont.addImportedOntology(repository.getOntology(OntologyHelper.KNOWLEDGE_BASE_URI));
					repository.importOntology(ont);
					load();
					// show create ontology
					institution.setSelectedItem(source);
					organs.setSelectedValue(organ,true);
					domains.setSelectedValue(ont,true);
				}catch(Exception ex){
					JOptionPane.showMessageDialog(frame,
							"Problem Creating New Domain Ontology!",
							"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
				setBusy(false);
			}
		})).start();
	}
	
	/**
	 * load domains
	 * @param organ
	 */
	private void loadDomains(String organ,String inst){
		Vector<String> list = ontologyMap.get(organ+"/"+inst);
		if(list == null)
			list = new Vector();
		Collections.sort(list);
		domains.setListData(list);
		preview.setText("");
		//domains.setSelectedIndex(0);
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			if(e.getSource().equals(organs)){
				loadDomains(""+organs.getSelectedValue(),""+institution.getSelectedItem());
			}else if(e.getSource().equals(domains)){
				Object obj = domains.getSelectedValue();
				if(obj instanceof IOntology){
					preview.setText(""+((IOntology)obj).getURI());
				}
				okButton.setEnabled(obj != null);
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource().equals(institution)){
			loadDomains(""+organs.getSelectedValue(),""+institution.getSelectedItem());
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(e.getSource().equals(okButton)){
			ok = true;
			if(dialog != null)
				dialog.dispose();
		}else if(e.getSource().equals(cancelButton)){
			ok = false;
			if(dialog != null)
				dialog.dispose();
		}else if("New".equals(cmd)){
			showNewDomainDialog();
		}else if("Import".equals(cmd)){
			doImport();
		}else if("Export".equals(cmd)){
			doExport();
		}else if("Delete".equals(cmd)){
			doRemove();
		}else if("Browse".equals(cmd)){
			doBrowse();
		}
	}
	
	/**
	 * remove ontology
	 */
	private void doRemove(){
		IOntology ont = (IOntology) getSelectedObject();
		if(ont != null){
			// check if locked
			if(isLocked(ont)){
				JOptionPane.showMessageDialog(frame,"Cannot delete "+ont+
						" because it is currently open",
						"Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			// ask for permission
			int r = JOptionPane.showConfirmDialog(frame,"<html>Are you sure you want to delete  <b>"+
					ont.getName()+"</b> domain ontology?","Confirm",
					JOptionPane.OK_CANCEL_OPTION);
			// if not canceled, remove entry
			if(r != JOptionPane.CANCEL_OPTION){
				String url = OntologyHelper.getCaseBase(""+ont.getURI());
				IOntology inst = repository.getOntology(URI.create(url));
				if(inst != null){
					// remove entry
					repository.removeOntology(inst);
					// remove data
					inst.delete();
				}
				// remove entry
				repository.removeOntology(ont);
				// remove data
				ont.delete();
				load();
			}
		}
	}
	
	
	/**
	 * is this ontology locked
	 * @param ont
	 * @return
	 */
	private boolean isLocked(IOntology ont){
		return ont.getURI().equals(lockedOntology);
	}
	
	
	/**
	 * set ontology as locked
	 * @param o
	 */
	public static void setLockedOntology(URI o){
		lockedOntology = o;
	}
	
	/**
	 * import ontology from file
	 */
	private void doImport(){
		JFileChooser chooser = new JFileChooser();
		if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
			File f = chooser.getSelectedFile();
			if(f != null && f.canRead()){
				importOntology(f.toURI());
			}
		}
	}
	
	
	
	
	/**
	 * export ontology
	 */
	private void doExport(){
		final Object value = getSelectedObject();
		if(value != null && value instanceof IOntology){
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileFilter(){
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".owl");
				}
				public String getDescription() {
					return "OWL File";
				}
				
			});
			chooser.setSelectedFile(new File(value.toString()));
			if(chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION){
				final File f = chooser.getSelectedFile();
				setBusy(true);
				(new Thread(new Runnable(){
					public void run(){
						IOntology ont = (IOntology) value;
						try{
							ont.load();
							ont.write(new FileOutputStream(f),IOntology.OWL_FORMAT);
						}catch(Exception ex){
							ex.printStackTrace();
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);							
						}
						setBusy(false);
					}
				})).start();
			}
			
		}
	}
	
	/**
	 * browse ontology
	 */
	private void doBrowse(){
		final OntologyExplorer explorer = new OntologyExplorer();
		JFrame f = new JFrame("Ontology Explorer");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(explorer);
		f.pack();
		f.setVisible(true);
		// set root
		Object value = getSelectedObject();
		if(value instanceof IOntology){
			String url = OntologyHelper.getCaseBase(""+((IOntology) value).getURI());
			IOntology ont = repository.getOntology(URI.create(url));
			final IOntology ontology = (ont == null)?(IOntology)value:ont;
			explorer.setBusy(true);
			(new Thread(new Runnable(){
				public void run(){
					try{
						ontology.load();
					}catch(IOntologyException ex){
						ex.printStackTrace();
					}
					explorer.setRoot(ontology.getRootClasses());
					explorer.setBusy(false);
				}
			})).start();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost/repository";
		String user = "user";
		String pass = "resu";
		String table = "repository";
		String dir   = System.getProperty("user.home")+File.separator+".protegeRepository";
		IRepository r = new ProtegeRepository(driver,url,user,pass,table,dir);
		DomainSelectorPanel selector = new DomainSelectorPanel(r,true);
		selector.showChooserDialog();
		/*
		if(selector.showNewDomainDialog()){
			while(!selector.isSelected()){
				UIHelper.sleep(250);
			}
			System.out.println(selector.getSelectedObject());
		}
		*/
	}


}
