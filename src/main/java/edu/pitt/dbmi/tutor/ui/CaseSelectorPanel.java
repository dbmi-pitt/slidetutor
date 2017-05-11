package edu.pitt.dbmi.tutor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.ViewerHelper;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

public class CaseSelectorPanel extends JPanel implements ActionListener  {
	private ExpertModule expertModule;
	
	private JButton okButton,cancelButton,preview;
	private JOptionPane op;
	
	private DomainTableModel domains;
	private JTable table;
	private JTextField domainSearch,caseSearch;
	private JList cases;
	private TableRowSorter<TableModel> sorter;
	private JDialog dialog;
	private ViewerHelper.FilterDocument caseFilter;
	private List<String> visitedCases;
	
	/**
	 * create new module
	 * @param e
	 */
	public CaseSelectorPanel(ExpertModule e){
		expertModule = e;
		expertModule.load();
		createInterface();
	}
	
	public void setVisitedCases(List<String> list){
		visitedCases  = list;
	}
	
	private void createInterface(){
		// top panel
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		
		// create search box
		domainSearch = new JTextField();
		domainSearch.setBorder(new TitledBorder("Domain Search"));
		domainSearch.setDocument(new DefaultStyledDocument(){
			public void insertString(int arg0, String arg1, AttributeSet arg2) throws BadLocationException {
				super.insertString(arg0, arg1, arg2);
				filterTable(domainSearch.getText());
			}
			public void remove(int arg0, int arg1) throws BadLocationException {
				super.remove(arg0, arg1);
				filterTable(domainSearch.getText());
			}
			
		});
		
		// create domains table
		domains = new DomainTableModel();
		table = new JTable(domains);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));
		table.setShowVerticalLines(false);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting()){
					loadCases();
				}
			}
		});
		
		// create sorter object
		sorter = new TableRowSorter<TableModel>(table.getModel());
		table.setRowSorter(sorter);

		
		JScrollPane domainScroll = new JScrollPane(table);
		domainScroll.setPreferredSize(new Dimension(700,250));
		domainScroll.setBorder(new TitledBorder("Domains"));
		domainScroll.setBackground(Color.white);
		
		// add them
		top.add(domainSearch,BorderLayout.NORTH);
		top.add(domainScroll,BorderLayout.CENTER);
		
		
		// bottom panel
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());
		
		// case list
		cases = new JList();
		cases.setFont(cases.getFont().deriveFont(Font.PLAIN));
		cases.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		cases.setVisibleRowCount(0);
		cases.setCellRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(JList list, Object value, int i, boolean s,boolean f) {
				JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, i, s, f);
				lbl.setText(" "+TextHelper.getName(""+value)+" ");
				
				//check status meta parameter
				Properties p = OntologyHelper.getURLQuery(""+value);
				if(p.containsKey("status")){
					String st = p.getProperty("status");
					if(STATUS_TESTED.equals(st)){
						lbl.setForeground(Color.green.darker());
					}else if(STATUS_COMPLETE.equals(st)){
						lbl.setForeground(Color.blue.darker());
					}else if(STATUS_INCOMPLETE.equals(st)){
						lbl.setForeground(Color.red.darker());
					}
				}
				
				//check visited
				if(visitedCases != null && visitedCases.contains(value)){
					lbl.setBackground(new Color(230,230,230));
					lbl.setOpaque(true);
				}
				
				return lbl;
			}
			
		});
		cases.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting()){
					okButton.setEnabled(cases.getSelectedIndex() > -1);
				}		
			}
		});
		
		JScrollPane caseScroll = new JScrollPane(cases);
		caseScroll.setPreferredSize(new Dimension(500,250));
		caseScroll.setBorder(new TitledBorder("Cases"));
		caseScroll.setBackground(Color.white);
		
		
		// preview panel
		JPanel previewPanel = new JPanel();
		preview = new JButton(UIHelper.getIcon(Config.getProperty("icon.general.preview")));
		previewPanel.setLayout(new GridBagLayout());
		previewPanel.setPreferredSize(new Dimension(200,200));
		previewPanel.add(preview,new GridBagConstraints());
		previewPanel.setBorder(new TitledBorder("Case Preview"));
		preview.addActionListener(this);
		
		// create search box
		caseSearch = new JTextField();
		caseSearch.setBorder(new TitledBorder("Case Search"));
		// set search document
		caseFilter = new ViewerHelper.FilterDocument(caseSearch, cases,Collections.EMPTY_LIST);
		caseSearch.setDocument(caseFilter);
		// setup layout	
		bottom.add(caseScroll,BorderLayout.CENTER);
		bottom.add(previewPanel,BorderLayout.EAST);
		bottom.add(caseSearch,BorderLayout.SOUTH);
		
		
		// setup this panel
		setLayout(new BorderLayout());
		
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,top,bottom);
		split.setResizeWeight(.5);
		add(split,BorderLayout.CENTER);

		
		// create popup
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		okButton.setEnabled(false);
		cancelButton = new JButton("Close");
		cancelButton.addActionListener(this);
		op = new JOptionPane(this,JOptionPane.PLAIN_MESSAGE,
		JOptionPane.OK_CANCEL_OPTION,null,new Object []{okButton,cancelButton});
		
		// add dobulic click listener
		cases.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(cases.getSelectedValue() != null && e.getClickCount() == 2)
					okButton.doClick();
			}
		});
	}
	
	/**
	 * filter domains in table
	 */
	private void filterTable(String text) {
	    RowFilter<TableModel,Object> rf = null;
	    //If current expression doesn't parse, don't update.
	    try {
	        rf = RowFilter.regexFilter("(?i)"+text,0,1,2);
	    } catch (java.util.regex.PatternSyntaxException e) {
	        return;
	    }
	    sorter.setRowFilter(rf);
	}

	/**
	 * sort column
	 * @param c
	 */
	private void sortColumn(int c){
		sorter.setSortable(c ,true);
		sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(c,SortOrder.ASCENDING)));
		sorter.sort();
	}
	
	public void setEnableCasePreview(boolean b){
		preview.setEnabled(b);
	}
	
	/**
	 * load data into models
	 */
	public void load(){
		// if there are more then 0 rows, then we
		// already loaded all domains
		if(domains.getRowCount() > 0){
			return;
		}
	
		// load domains
		domains.addAll(expertModule.getAvailableDomains());
		
		
		// reload table
		table.revalidate();
		
		// sort by domain by default
		sortColumn(2);
	}
	
	
	/**
	 * load cases
	 */
	private void loadCases(){
		okButton.setEnabled(false);
		String url = getSelectedDomain();
		
		if(url == null)
			return;
		
		String [] list = expertModule.getAvailableCases(url);
	
		// set filter
		caseFilter.setSelectableObjects(Arrays.asList(list));
		
		// set data
		cases.setListData(list);
		cases.revalidate();
	}
	
	
	/**
	 * show dialog
	 * @param parent
	 */
	public void showDialog(Component parent) {
		load();
		
		dialog = op.createDialog(parent,"Select Next Case");
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setVisible(true);
		
	}
	
	/**
	 * get selected case, if case is not selected, null is returned
	 * @return
	 */
	public String getSelectedCase(){
		return (String) cases.getSelectedValue();
	}
	
	/**
	 * get selected case, if case is not selected, null is returned
	 * @return
	 */
	public List<String> getSelectedCases(){
		List<String> list = new ArrayList<String>();
		for(Object o : cases.getSelectedValues()){
			list.add((String)o);
		}
		return list;
	}

	
	/**
	 * get selected domain, return null if domain was not selected
	 * @return
	 */
	public String getSelectedDomain(){
		int row = table.getSelectedRow();
		if(row > -1){
			return domains.getValue(table.convertRowIndexToModel(row));
		}
		return null;
	}
	
	/**
	 * set selected domain
	 * @param url
	 */
	public void setSelectedDomain(String url){
		int i = domains.indexOf(url);
		if(i > -1){
			// filter 
			domainSearch.setText(TextHelper.getName(url));
			
			// select in table
			int n = table.convertRowIndexToView(i);
			table.setRowSelectionInterval(n,n);
		}
	}
	
	/**
	 * set selected case
	 * @param c
	 */
	public void setSelectedCase(String c){
		//TODO:
	}
	
	//actions
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(okButton)){
			if(dialog != null)
				dialog.dispose();
		}else if(e.getSource().equals(cancelButton)){
			if(dialog != null)
				dialog.dispose();
			cases.clearSelection();
		}else if(e.getSource().equals(preview)){
			displayCase(getSelectedCase());
		}
		
	}


	/**
	 * display case
	 * @param c
	 */
	public void displayCase(String url){
		if(url == null)
			return;
			
		// load info
		CaseEntry caseEntry = expertModule.getCaseEntry(url);
		
		// upload at least defaults if nothing is available 
		if(!ViewerFactory.getProperties().contains("image.server.type")){
			Properties p = new Properties();
			try {
				p.load(getClass().getResourceAsStream("/resources/defaults/ViewerPanel.properties"));
			} catch (IOException e) {
				Config.getLogger().severe(TextHelper.getErrorMessage(e));
			}
			ViewerFactory.setProperties(p);
		}
		
		
		// create dialog
		JOptionPane op = new JOptionPane(UIHelper.createCaseInfoPanel(caseEntry,true),JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(this,caseEntry.getName());
		d.setModal(false);
		d.setResizable(true);
		d.pack();
		d.setVisible(true);
		
		// load image
		//if(selected != null)
		//	selected.doClick();		
	}
	

	/**
	 * custom table model
	 * @author tseytlin
	 */
	private class DomainTableModel extends AbstractTableModel {
		private String [] columnNames = new String [] {"Institution","Organ","Domain"};
		private List<String[]> data = new ArrayList<String[]>();
		
		public int getColumnCount() {
			return 3;
		}
		
		public String getColumnName(int n){
			return columnNames[n];
		}

		public int getRowCount() {
			return data.size();
		}

		public Object getValueAt(int r, int c) {
			return data.get(r)[c];
		}
		
		public String getValue(int r){
			return data.get(r)[3];
		}
		
		/**
		 * add new domain
		 * @param d
		 */
		public void add(String url){
			String [] s = new String [4];
			s[3] = url;
			
			// skip Instances.owl files
			if(url.endsWith(OntologyHelper.INSTANCES_ONTOLOGY))
				return;
			
			// now parse url to extract relevant info
			// (it better be formated right)
			Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/(.+)\\.owl");
			Matcher m = pt.matcher(url);
			if(m.matches()){
				//baseURL = m.group(1);
				s[1] = m.group(2);
				s[0]  = m.group(3);
				s[2]  = m.group(4);
				
				// add to data
				data.add(s);
			}
		}
		
		/**
		 * add bulk
		 * @param d
		 */
		public void addAll(String [] d){
			for(String s: d){
				add(s);
			}
		}
		
		/**
		 * add bulk
		 * @param d
		 */
		public void addAll(List<String> d){
			for(String s: d){
				add(s);
			}
		}
		/**
		 * find index of domain
		 * @param url
		 * @return
		 */
		public int indexOf(String url){
			for(int i=0;i<data.size();i++){
				if(data.get(i)[3].equalsIgnoreCase(url))
					return i;
			}
			return -1;
		}
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		String d = System.getProperty("user.dir");
		String s = File.separator;
	
		// load config file
		Config.load(d+s+"resources"+s+"SlideTutor.conf");
		Config.load(d+s+"resources"+s+"defaults"+s+"SimpleViewerPanel.properties");
		ViewerFactory.setProperties(Config.getProperties());
		
		// init panel
		CaseSelectorPanel p = new CaseSelectorPanel(new DomainExpertModule());
		p.showDialog(null);
		
		// print output
		System.out.println(p.getSelectedDomain());
		System.out.println(p.getSelectedCase());

	}

}
