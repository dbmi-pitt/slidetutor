package edu.pitt.dbmi.tutor.builder.protocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.MenuSelectionManager;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import edu.pitt.dbmi.tutor.builder.protocol.ProtocolManager.CSVAccessoryPanel;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.messages.NodeEvent;
import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.messages.TutorResponse;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;

/**
 * this class represents the results panel
 * @author tseytlin
 *
 */
public class ResultPanel extends JPanel implements ActionListener {
	private final String TUTOR_ERROR = "tutor_error";
	private JEditorPane status;
	private JLabel statusLabel;
	private JTable table;
	private JTextField filter;
	private List<List<String>> data;
	private List<String> columns;
	private List<Session> sessions;
	private boolean useCE,useIE,useTR,useNE;
	private TableRowSorter<TableModel> sorter;
	private ProtocolManager manager;
	private JToggleButton filterColumns;	
	private JPopupMenu columnMenu;
	private File file;
	private boolean pruneInput;
	
	/**
	 * 
	 * @param query
	 */
	public ResultPanel(ProtocolManager m, List<Session> query){
		super();
		manager = m;
		setup();
		sessions = query;
		setupStatus();
	}
	
	/**
	 * setup UI
	 */
	private void setup(){
		columns = new ArrayList<String>();
		data = new ArrayList<List<String>>();
		
		setLayout(new BorderLayout());
		status = new UIHelper.HTMLPanel();
		status.setEditable(false);
		statusLabel = new JLabel(" ");
		
		JScrollPane scroll = new JScrollPane(status);
		scroll.setPreferredSize(new Dimension(300,77));
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(new TitledBorder("Query"));
		
		JPanel epanel = new JPanel();
		epanel.setLayout(new FlowLayout());
		epanel.setBorder(new TitledBorder("Export"));
		JButton b = UIHelper.createButton("export","Export Results",UIHelper.getIcon(Config.getProperty("icon.toolbar.export")),this);
		b.setPreferredSize(new Dimension(40,40));
		epanel.add(b);	
		JPanel header = new JPanel();
		header.setLayout(new BorderLayout());
		header.add(scroll,BorderLayout.CENTER);
		header.add(epanel,BorderLayout.EAST);
		
		
		// init table
		//tableModel = new SimpleTableModel();
		table = new JTable();
		table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));
		table.setCellSelectionEnabled(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setFillsViewportHeight(true);
		table.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e){
				if(data.isEmpty())
					return;
				Point p = e.getPoint(); 
			    int r = table.rowAtPoint(p);
			    int c = table.columnAtPoint(p);
			    if(r < 0 || c < 0){
			    	table.setToolTipText(null);
			    }else{
					int row = table.convertRowIndexToModel(r);
				    int column = table.convertColumnIndexToModel(c);
			    	String v = ""+table.getModel().getValueAt(row,column);
			    	if(TextHelper.isEmpty(v))
				    	table.setToolTipText(null);
				    else
				    	table.setToolTipText((v.length() > 70)?"<html><table width=400><tr><td>"+v+"</td></tr></table></html>":v);
			    }
		    }
		});
		
		// highlight entries that have error
		table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				
				row = table.convertRowIndexToModel(row);
				
				// look at last two values in the row 
				String v1= ""+table.getModel().getValueAt(row,table.getModel().getColumnCount()-1);
				String v2= ""+table.getModel().getValueAt(row,table.getModel().getColumnCount()-2);
			
				// if either contains tutor error then
				if(v1.contains(TUTOR_ERROR) || v2.contains(TUTOR_ERROR))
					l.setBackground((isSelected)?table.getSelectionBackground():new Color(255,230,230));
				else
					l.setBackground((isSelected)?table.getSelectionBackground():Color.white);
				return l;
			}
		});
		
		
		JPanel tp = new JPanel();
		tp.setLayout(new BorderLayout());
		tp.setBorder(new TitledBorder("Data"));
		tp.add(createDataToolBar(),BorderLayout.NORTH);
		tp.add(new JScrollPane(table),BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(header,BorderLayout.NORTH);
		panel.add(createEventButtons(),BorderLayout.CENTER);
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(panel);
		split.setBottomComponent(tp);
		add(split,BorderLayout.CENTER);
		add(statusLabel,BorderLayout.SOUTH);
	}
	
	private JToolBar createDataToolBar(){
		JToolBar toolbar = new JToolBar();
		filter = new JTextField();
		filter.setActionCommand("Filter Rows");
		filter.addActionListener(this);
		filter.setToolTipText("<html><table width=400><tr><td>" +
				"Enter text to filter data by. " +
				"Use spaces between terms to get rows that contain <i>term 1</i> <b>or</b> <i>term 2</i> or both. " +
				"Add <b>not</b> in front of the query to negate the results of the query. " +
				"You can also use <b>and</b> inbetween filter terms to get only rows that contain all of the terms. "+
				"Use double quotes to specify a term that you want to match exactly.<br> " +
				"If criteria exactly matches one of the keys in the <i>input</i> column, " +
				"then that column will only display the value associated with that key."+
				"</td></tr></table>");
		toolbar.add(filter);
		toolbar.add(UIHelper.createButton("Filter Rows","Show Data that Matches the Query on the Left",
				UIHelper.getIcon(Config.getProperty("icon.menu.filter.row")),-1,true,this));
		toolbar.addSeparator();
		filterColumns = UIHelper.createToggleButton("Filter Columns","Show/Hide Data Columns",
				UIHelper.getIcon(Config.getProperty("icon.menu.filter.column")),-1,true,this);
		toolbar.add(filterColumns);
		toolbar.addSeparator();
		toolbar.add(UIHelper.createButton("refresh","Refresh Data",Config.getIconProperty("icon.menu.refresh"),this));
		return toolbar;
	}
	
	
	/**
	 * export content to comma separated value file
	 */
	private void doExport(){
		JFileChooser chooser = new JFileChooser(file);
		chooser.setFileFilter(new FileFilter() {
			public String getDescription() {
				return "Comma Separated Values (.csv)";
			}
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
			}
		});
		CSVAccessoryPanel acc = new CSVAccessoryPanel();
		chooser.setAccessory(acc);
		
		int r = chooser.showSaveDialog(this);
		if(r == JFileChooser.APPROVE_OPTION){
			file = chooser.getSelectedFile();
			try{
				if(!file.getName().endsWith(".csv"))
					file = new File(file.getAbsolutePath()+".csv");
				
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				// write out the header row
				writer.write(getRowString(columns,acc.getSeparator()));
				// write out data
				for(int i=0;i<data.size();i++){
					// check if row is not filtered
					if(table.convertRowIndexToView(i) > -1)
						writer.write(getRowString(data.get(i),acc.getSeparator()));
				}
				writer.close();
			}catch(IOException ex){
				ex.printStackTrace();
			}
			
		}
	}
	
	/**
	 * get a row that 
	 * @param d
	 * @return
	 */
	private String getRowString(List<String> d,String S){
		StringBuffer str = new StringBuffer();
		for(int i=0;i<d.size();i++){
			if(table.convertColumnIndexToView(i) > -1){
				str.append("\""+getFilteredValue(table.getModel().getColumnName(i),d.get(i))+"\""+S);
			}
		}
		return str.substring(0,str.length()-1)+"\n";
	}
	
	
	/**
	 * filter data by text
	 * @param text
	 */
	private void doFilter(String t){
		pruneInput = false;
		if(t == null)
			t = "";
		t = t.trim();
	
		// don't act on blanks
		if(t.length() == 0){
			SwingUtilities.invokeLater(new Runnable(){
		    	public void run(){
		    		if(sorter != null) 
		    			sorter.setRowFilter(null);
		    	}
			 });
			return ;
		}
		
		
		// handle negation
		boolean negation = false;
		if(t.toLowerCase().startsWith("not")){
			t = t.substring(3).trim();
			negation = true;
		}
		
		// a set of expressions
		final List<String> texts = new ArrayList<String>();
		
		// split text into chunks that are either quoted or don't have spaces
		Pattern pt = Pattern.compile("(\"[^\"]+\"|[^\\s]+)");
		Matcher mt = pt.matcher(t);
		StringBuffer str = new StringBuffer();
		while(mt.find()){
			// for each chunk
			String txt = mt.group();
			// if chunk is a logical operator, start another buffer
			if(txt.toLowerCase().equals("and")){
				if(str.length() > 0)
					texts.add("(?i)("+str.substring(0,str.length()-1)+")");
				str = new StringBuffer();
			}else{
				// take care of the quoted text
				if(txt.startsWith("\"") && txt.endsWith("\"")){
					txt = "^"+txt.substring(1,txt.length()-1)+"$";
				}
				if(txt.length() > 0)
					str.append(txt+"|");
			}
		}
		// add last bit of info
		if(str.length() > 0)
			texts.add("(?i)("+str.substring(0,str.length()-1)+")");
		
		// now filter things out
		final boolean not = negation;
		(new Thread(new Runnable(){
			public void run(){
				manager.setBusy(true);
				filterTable(texts,not);
				manager.setBusy(false);
			}
		})).start();
	}
	
	/**
	 * filter domains in table
	 */
	private void filterTable(List<String> text,boolean negation) {
	    RowFilter<TableModel,Object> rf = null;
	    //If current expression doesn't parse, don't update.
	    // do it for all columns
	    try {
	    	// pick the right rows
	    	int [] r = getColumnsOfInterest();
	    	
	    	// make a single filter or an end
	    	if(text.size() == 1){
	    		rf = RowFilter.regexFilter(text.get(0),r);
	    	}else if (text.size() > 1){
	    		List<RowFilter<Object,Object>> filters = new ArrayList<RowFilter<Object,Object>>();
	    		for(String t: text){
	    			filters.add(RowFilter.regexFilter(t,r));
	    		}
	    		rf = RowFilter.andFilter(filters);
	    	}
	        if(negation)
	        	rf = RowFilter.notFilter(rf);
	        
	    } catch (java.util.regex.PatternSyntaxException e) {
	        return;
	    }
	    
	    // update filter
	    final RowFilter filter = rf;
	    SwingUtilities.invokeLater(new Runnable(){
	    	public void run(){
	    		if(sorter != null) 
	    			sorter.setRowFilter(filter);
	    		pruneInput = true;
	    	}
	    });
	   
	}
	
	/**
	 * return columns of interest
	 * @return
	 */
	private int [] getColumnsOfInterest(){
		// get columns that are being displayed
		List<Integer> list = new ArrayList<Integer>();
		for(int i=0;i<columns.size();i++){
			int n = table.convertColumnIndexToView(i);
			if(n > -1)
				list.add(n);
		}
		int [] x = new int [list.size()];
		for(int i=0;i<x.length;i++)
			x[i] = list.get(i);
		return x;
	}
	
	
	/**
	 * do column filter
	 * @param column
	 * @param selected
	 */
	private void doColumnFilter(String column, boolean selected){
		if(selected){
			int i = columns.indexOf(column);
			table.addColumn(new TableColumn(i));
		}else{
			int i = table.convertColumnIndexToView(columns.indexOf(column));
			table.removeColumn(table.getColumnModel().getColumn(i));
		}
	}
	
	private JPopupMenu getColumnMenu(){
		if(columnMenu == null){
			JPopupMenu menu = new JPopupMenu();
			menu.addPopupMenuListener(new PopupMenuListener(){
				public void popupMenuCanceled(PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							filterColumns.doClick();
						}
					});
				}
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
				
			});
			for(String s: columns){
				JCheckBoxMenuItem m = UIHelper.createCheckboxMenuItem(s,s,null,this);
				m.setSelected(true);
				// disable menu from closing on click
				// http://forums.sun.com/thread.jspa?threadID=5366636
				m.setUI(new BasicCheckBoxMenuItemUI(){
					 protected void doClick(MenuSelectionManager msm) {
					      menuItem.doClick(0);
					   }
				});
				menu.add(m);
			}
			columnMenu = menu;
		}
		return columnMenu;
	}
	
	
	
	/**
	 * create event buttons
	 * @return
	 */
	private JPanel createEventButtons(){
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1,0));
		panel.setBorder(new TitledBorder("Events"));
		
		JToggleButton b1 = new JToggleButton("Interface Events");
		b1.setActionCommand(b1.getText());
		b1.addActionListener(this);
		panel.add(b1);
		
		JToggleButton b2 = new JToggleButton("Client Events");
		b2.setActionCommand(b2.getText());
		b2.addActionListener(this);
		panel.add(b2);
		
		JToggleButton b3 = new JToggleButton("Tutor Responses");
		b3.setActionCommand(b3.getText());
		b3.addActionListener(this);
		panel.add(b3);
		
		JToggleButton b4 = new JToggleButton("Node Events");
		b4.setActionCommand(b4.getText());
		b4.addActionListener(this);
		panel.add(b4);
		
		
		ButtonGroup grp = new ButtonGroup();
		grp.add(b1);
		grp.add(b2);
		grp.add(b3);
		grp.add(b4);
		
		return panel;
	}
	
	/**
	 * sync data
	 */
	public void sync(){
		// clear data
		columns.clear();
		data.clear();
		columnMenu = null;
		filter.setText("");
		
		//setup columns
		if(useIE || useCE || useTR || useNE){
			columns.add("user");
			columns.add("case");
			columns.add("session");
		}
		
		// what to do w/ IE
		// interface events
		if(useIE){
			columns.add("interface id");
			Collections.addAll(columns,Message.getMessageFields());
			columns.add("input");
			columns.add("client id");
							
			// go over client events
			for(Session s: sessions){
				for(InterfaceEvent ce: s.getInterfaceEvents()){
					List<String> row = new ArrayList<String>();
					row.add(s.getUsername());
					row.add(OntologyHelper.getCaseName(s.getCase()));
					row.add(s.getSessionID());
					row.add(""+ce.getMessageId());
					
					// input fields
					for(String key: Message.getMessageFields()){
						// special case for timestamp
						try{
							if("timestamp".equals(key))
								row.add(getDate(ce.get(key)));
							else
								row.add(TextHelper.toString(ce.get(key)));
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}
					row.add(ce.getInputString());
					row.add((ce.getClientEventId() > 0)?""+ce.getClientEventId():"");
					
					data.add(row);
				}
			}
		}
		
		
		// client events
		if(useCE){
			columns.add("client id");
			Collections.addAll(columns,Message.getMessageFields());
			columns.add("input");
			
			// get somo portions of tutor responses as well
			columns.add("response");
			columns.add("error");
			columns.add("code");
			columns.add("response input");
			
			// go over client events
			for(Session s: sessions){
				for(ClientEvent ce: s.getClientEvents()){
					List<String> row = new ArrayList<String>();
					row.add(s.getUsername());
					row.add(OntologyHelper.getCaseName(s.getCase()));
					row.add(s.getSessionID());
					row.add(""+ce.getMessageId());
					
					// input fields
					for(String key: Message.getMessageFields()){
						// special case for timestamp
						try{
							if("timestamp".equals(key))
								row.add(getDate(ce.get(key)));
							else
								row.add(TextHelper.toString(ce.get(key)));
						}catch(Exception ex){
							System.out.println(ce);
							ex.printStackTrace();
						}
					}
					row.add(ce.getInputString());
					
					// get tutor responses
					List<TutorResponse> response = s.getTutorResponses(ce);
					
					// if no, or single response, we just have one row
					if(response.isEmpty()){
						row.add("");
						row.add("");
						row.add("");
						row.add("");
					}else{
						TutorResponse r = response.get(0);
						row.add(r.getResponse());
						row.add(r.getError());
						row.add(r.getCode());
						row.add(r.getInputString());
					}
					data.add(row);
					
					// if more then a single response, add empty rows
					if(response.size() > 1){
						for(int i=1;i<response.size();i++){
							TutorResponse r = response.get(i);
							// create blank row
							row = new ArrayList<String>();
							for(int j=0;j<columns.size()-4;j++)
								row.add("");
							row.add(r.getResponse());
							row.add(r.getError());
							row.add(r.getCode());
							row.add(r.getInputString());
							
							// add blank row
							data.add(row);
						}
					}
				}
			}
		}
		
		// tutor responses
		if(useTR){
			columns.add("response id");
			String nextStep = "";
			for(String field: TutorResponse.getMessageFields()){
				// don't add prefix after ID
				if(field.equalsIgnoreCase("type"))
					 nextStep = "next step ";
				columns.add(nextStep+field);
				// don't add prefix after ID
				if(field.equalsIgnoreCase("id"))
					nextStep = "";
			}
			columns.add("input");
			columns.add("client id");
			
			// go over client events
			for(Session s: sessions){
				for(TutorResponse ce: s.getTutorResponses()){
					List<String> row = new ArrayList<String>();
					row.add(s.getUsername());
					row.add(OntologyHelper.getCaseName(s.getCase()));
					row.add(s.getSessionID());
					row.add(""+ce.getMessageId());
				
					for(String key: TutorResponse.getMessageFields()){
						try{
							// special case for timestamp
							if("timestamp".equals(key))
								row.add(getDate(ce.get(key)));
							else
								row.add(TextHelper.toString(ce.get(key)));
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}
					
					row.add(ce.getInputString());
					row.add((ce.getClientEventId() > 0)?""+ce.getClientEventId():"");
					
					data.add(row);
				}
			}
		}
		
		
		// use NodeEvents 
		if(useNE){
			columns.add("node id");
			Collections.addAll(columns,NodeEvent.getMessageFields());
			columns.add("input");
			columns.add("tutor response id");
			
			// go over client events
			for(Session s: sessions){
				for(NodeEvent ne: s.getNodeEvents()){
					List<String> row = new ArrayList<String>();
					row.add(s.getUsername());
					row.add(OntologyHelper.getCaseName(s.getCase()));
					row.add(s.getSessionID());
					row.add(""+ne.getMessageId());
					
					// input fields
					for(String key: NodeEvent.getMessageFields()){
						try{
							// special case for timestamp
							if("timestamp".equals(key))
								row.add(getDate(ne.get(key)));
							else
								row.add(TextHelper.toString(ne.get(key)));
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}
					row.add(ne.getInputString());
					row.add((ne.getTutorResponseId() > 0)?""+ne.getTutorResponseId():"");
					
					data.add(row);
				}
			}
		}
		
		
		// update table
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				table.setModel(new SimpleTableModel());
				// create sorter object
				sorter = new TableRowSorter<TableModel>(table.getModel()){
					private Comparator intCompare = new SessionTable.IntegerComparator();
					private Comparator dateCompare = new SessionTable.DateComparator();
					public Comparator<?> getComparator(int column) {
						// check value of first row, if fits, then use appropriate comparaotr
						if(data != null && !data.isEmpty()){
							List<String> list = data.get(0);
							if(list != null && column >= 0 && column < list.size()){
								String str = list.get(column);
								// check for integer
								try{
									Integer.parseInt(str);
									// if successfull parse, then compare integers
									return intCompare;
								}catch(NumberFormatException ex){}
								Date d = TextHelper.parseDate(str);
								if(d != null)
									return dateCompare;
							}
						}
						return super.getComparator(column);
					}
				};
				table.setRowSorter(sorter);
				table.revalidate();
				statusLabel.setText(data.size()+" rows returned");
			}
		});
		
	}
	
	/**
	 * parse input for date
	 * @param input
	 * @return
	 */
	private String getDate(String input){
		try{
			return TextHelper.formatDate(new Date(Long.parseLong(input)));
		}catch(Exception ex){
			//OOPS could not parse a date
		}
		return "";
	}
	
	private void setupStatus(){
		Set<String> users = new LinkedHashSet<String>();
		Set<String> sesss = new LinkedHashSet<String>();
		Set<String> cases = new LinkedHashSet<String>();
		Set<String> conds = new LinkedHashSet<String>();
		
		// gether usernams and sessions
		for(Session s: sessions){
			users.add(s.getUsername());
			sesss.add(s.getSessionID());
			cases.add(OntologyHelper.getCaseName(s.getCase()));
			conds.add(s.getCondition());
		}
		
		StringBuffer st = new StringBuffer();
		st.append("<table width=\"100%\"><tr>");
		st.append("<td valign=top width=100><b>Username: </b></td><td> "+TextHelper.filter(TextHelper.toString(users),60)+"</td>");
		//st.append("<td><b>Conditions: </b></td><td> "+TextHelper.toString(conds)+"</td>");
		st.append("</tr><tr>");
		st.append("<td valign=top width=100><b>Cases: </b></td><td> "+TextHelper.filter(TextHelper.toString(cases),60)+"</td>");
		//st.append("<td><b>Sessions: </b></td><td> "+TextHelper.toString(sesss)+"</td>");
		st.append("</tr></table>");
		status.setText(st.toString());
	}
	
	/**
	 * simple table model
	 * @author Eugene Tseytlin
	 */
	private class SimpleTableModel extends AbstractTableModel {
		public String getColumnName(int column) {
			return columns.get(column);
		}
		public int getColumnCount() {
			return columns.size();
		}
		public int getRowCount() {
			return data.size();
		}
		public Object getValueAt(int r, int c) {
			if(!check(r,c))
				return "";
			return getFilteredValue(getColumnName(c),data.get(r).get(c));
		}
		
		private final boolean check(int r, int c){
			return  0 <= c && c< getColumnCount() && 0 <= r && r < getRowCount(); 
		}
	}
	
	
	/**
	 * under normal circumstances just return the input, however if we are parsing
	 * input AND there is a filter that is just the KEY in the input map, then
	 * only return its value
	 * @param input
	 * @return
	 */
	private String getFilteredValue(String column,String input){
		// do special display for input if we search for something in particular
		// also check the pruneInput value for synchronization purposes
		if(pruneInput && "input".equalsIgnoreCase(column)){
			String ft = filter.getText();
			if(!TextHelper.isEmpty(ft)){
				if(input.contains(ft)){
					Object o = TextHelper.parseMessageInput(input);
					if(o instanceof Map){
						Map<String,String> map = (Map<String,String>) o;
						if(map.containsKey(ft)){
							return map.get(ft);
						}
					}
				}
			}
		}
		return input;
	}
	

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		AbstractButton bt = (e.getSource() instanceof AbstractButton)?(AbstractButton) e.getSource():null;
		
		// check if it is column filtering
		if(bt != null && bt instanceof JCheckBoxMenuItem){
			doColumnFilter(cmd,bt.isSelected());
			return;
		}
		
		// check for normal buttons
		if("Interface Events".equals(cmd)){
			useIE = bt.isSelected();
			if(useIE)
				useCE = useTR = useNE = false;
		}else if("Client Events".equals(cmd)){
			useCE = bt.isSelected();
			if(useCE)
				useTR = useIE = useNE = false;
		}else if("Tutor Responses".equals(cmd)){
			useTR = bt.isSelected();
			if(useTR)
				useCE = useIE = useNE = false;
		}else if("Node Events".equals(cmd)){
			useNE = bt.isSelected();
			if(useNE)
				useCE = useIE = useTR = false;
		}else if("Filter Rows".equals(cmd)){
			doFilter(filter.getText());
			return;
		}else if("Filter Columns".equals(cmd)){
			if(bt.isSelected()){
				Dimension d = bt.getSize();
				getColumnMenu().show(bt,0,d.height);
			}
			return;
		}else if("export".equals(cmd)){
			doExport();
			return;
		}else if("refresh".equals(cmd)){
			for(Session s: sessions){
				s.refresh();
			}
		}
		
		// sync data
		((new Thread(new Runnable() {
			public void run() {
				manager.setBusy(true);
				sync();
				manager.setBusy(false);
			}
		}))).start();
		
	}
}
