package edu.pitt.dbmi.tutor.builder.protocol;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import edu.pitt.dbmi.tutor.messages.Session;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;


/**
 * special kind of table
 * @author tseytlin
 *
 */
public class SessionTable extends JTable {
	private SessionTableModel model;
	//private TableRowSorter<TableModel> sorter;
	
	public SessionTable(){
		super(new SessionTableModel());
		model = (SessionTableModel) getModel();
		
		// setup parameters
		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setFillsViewportHeight(true);
		setShowHorizontalLines(true);
		setShowVerticalLines(false);
		getTableHeader().setFont(getFont().deriveFont(Font.BOLD));
		setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				l.setHorizontalAlignment(JLabel.CENTER);
				if(value != null){
					String val = value.toString();
					if(column == 1){
						val = "<html><b><font color=green>"+val+"</font></b>";
					}else if(column == 3){
						val = "<html><font color=red>"+val+"</font>";
					}
					l.setText(val);
				}else{
					l.setText("");
				}
				
				// highlight problem sessions
				Session s = model.getData().get(table.convertRowIndexToModel(row));
				if(s != null){
					if(s.getOutcome() != null && s.getOutcome().toLowerCase().contains("error"))
						l.setBackground((isSelected)?getSelectionBackground():new Color(255,230,230));
					else
						l.setBackground((isSelected)?getSelectionBackground():Color.white);
				
					// set tooltip 
					if(isSelected)
						l.setToolTipText("session "+((s.getOutcome() != null)?s.getOutcome():"crashed"));
					else
						l.setToolTipText(null);
				}
				
				
				
				return l;
			}
		});
		getColumnModel().getColumn(2).setPreferredWidth(200);
		
		
		
		// create sorter object
		setRowSorter(createRowSorter());		
	}
	
	
	
	
	/**
	 * get list of selected sessions
	 * @return
	 */
	public List<Session> getSelectedSessions(){
		List<Session> list = new ArrayList<Session>();
		for(int i: getSelectedRows()){
			i = convertRowIndexToModel(i);
			if(i < model.getData().size())
				list.add(model.getData().get(i));
		}
		return list;
	}
	
	public List<Session> getAllSessions(){
		return model.getData();
	}
	
	
	/**
	 * create row sorter
	 * @return
	 */
	private TableRowSorter createRowSorter(){
		return new TableRowSorter<TableModel>(getModel()){
			private Comparator integerComparator = new IntegerComparator();
			private Comparator dateComparator    = new DateComparator();
			public Comparator<?> getComparator(int column) {
				// sort integers for sessions
				if(column == 0){
					return integerComparator;
				// sort dates	
				}else if(column == 4){
					return dateComparator;
				}
				return super.getComparator(column);
			}
			
		};
	}
	
	
	/**
	 * should be called from invoke later
	 * @param s
	 */
	public void load(List<Session> s){
		model.setData(s);
		
		// create sorter object
		setRowSorter(createRowSorter());		
		
		revalidate();
		repaint();
	}
	
	/**
	 * compare integers that are strings
	 * @author tseytlin
	 *
	 */
	public static class IntegerComparator implements Comparator<String>{
		public int compare(String o1, String o2) {
			try{
				return Integer.parseInt(o1) - Integer.parseInt(o2);
			}catch(NumberFormatException ex){}
			return o1.compareTo(o2);
		}	
	}
	/**
	 * compare dates that are strings
	 * @author tseytlin
	 *
	 */
	public static class DateComparator implements Comparator<String>{
		public int compare(String o1, String o2) {
			Date d1 = TextHelper.parseDate(o1);
			Date d2 = TextHelper.parseDate(o2);
			if(d1 != null && d2 != null)
				return d1.compareTo(d2);
			return o1.compareTo(o2);
		}	
	}
	
	/**
	 * session table model for sessions
	 * @author Eugene Tseytlin
	 */
	public static class SessionTableModel extends AbstractTableModel {
		private List<Session> sessionData;
		private String [] columns = new String [] {"id","user","condition","case","time","duration"};
		public String getColumnName(int column) {
			return columns[column];
		}
		public int getColumnCount() {
			return columns.length;
		}
		public int getRowCount() {
			return (sessionData == null)?0:sessionData.size();
		}

		public Object getValueAt(int r, int c) {
			if(sessionData != null && r < getRowCount()){
				Session s = sessionData.get(r);
				switch(c){
				case 0: return s.getSessionID();
				case 1: return s.getUsername();
				case 2: return s.getCondition();
				case 3: return OntologyHelper.getCaseName(s.getCase());
				case 4: return TextHelper.formatDate(s.getStartTime());
				case 5: {
					Date d = s.getFinishTime();
					return (d != null)?TextHelper.formatDuration(d.getTime()-s.getStartTime().getTime()):"";
					}
				}
			}
			return null;
		}
		
		/**
		 * set data
		 * @param data
		 */
		public void setData(List<Session> data){
			sessionData = data;
		}
		
		/**
		 * set data
		 * @param data
		 */
		public List<Session> getData(){
			return (sessionData != null)?sessionData:Collections.EMPTY_LIST;
		}
		
	}
}