package edu.pitt.dbmi.tutor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.ErrorEntry;
import edu.pitt.dbmi.tutor.beans.ScenarioEntry;
import edu.pitt.dbmi.tutor.model.FeedbackModule;
import edu.pitt.dbmi.tutor.modules.feedback.HelpManager;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;
import static  edu.pitt.dbmi.tutor.messages.Constants.*;

public class ReasonerStateExplorer extends JPanel {
	private JTabbedPane tabs;
	private JFrame frame;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	public ReasonerStateExplorer(){
		super();
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(300,700));
		tabs = new JTabbedPane();
		add(tabs,BorderLayout.CENTER);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	/**
	 * set content under given name
	 * @param name
	 * @param content
	 */
	public void setContent(String name, Collection content){
		boolean update = false;
		// check if such tab exists
		for(int i=0;i<tabs.getComponentCount();i++){
			if(tabs.getTitleAt(i).equals(name)){
				((StateList)tabs.getComponentAt(i)).setContent(content);
				update = true;
				break;
			}
		}
		if(!update)
			tabs.addTab(name,new StateList(content));
	}
	
	/**
	 * set content under given name
	 * @param name
	 * @param content
	 */
	public void setContent(String name, Map content){
		boolean update = false;
		// check if such tab exists
		for(int i=0;i<tabs.getComponentCount();i++){
			if(tabs.getTitleAt(i).equals(name)){
				((StateList)tabs.getComponentAt(i)).setContent(content);
				update = true;
				break;
			}
		}
		if(!update)
			tabs.addTab(name,new StateList(content));
	}
	
	/**
	 * refresh all tabs
	 */
	public void refresh(){
		for(int i=0;i<tabs.getComponentCount();i++){
			((StateList)tabs.getComponentAt(i)).refresh();
		}
	}
	
	public void showFrame(){
		if(frame == null){
			frame = new JFrame("Reasoner State Explorer");
			frame.getContentPane().add(this);
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					super.windowClosing(e);
					hideFrame();
				}
			});
			frame.pack();
		}
		frame.setVisible(true);
	}
	
	public void hideFrame(){
		if(frame != null){
			frame.setVisible(false);
		}
		pcs.firePropertyChange("HIDE_WINDOW",null,null);
	}
	
	private class StateList extends JPanel implements ActionListener{
		private Object content;
		private JList list;
		private UIHelper.HTMLPanel info;
	
		
		public StateList(Object c){
			super();
			setLayout(new BorderLayout());
			list = new JList();
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if(!e.getValueIsAdjusting()){
						info(list.getSelectedValue());
					}
					
				}
			});
			list.setCellRenderer(new DefaultListCellRenderer(){
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,boolean cellHasFocus) {
					JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					lbl.setForeground(Color.black);
					if(value instanceof ConceptEntry){
						ConceptEntry e = (ConceptEntry) value;
						if(ConceptEntry.INCORRECT == e.getConceptStatus())
							lbl.setForeground(Color.red);
						else if(e.isFound())
							lbl.setForeground(new Color(0,100,0));
						lbl.setText(e.getText());
					}
					return lbl;
				}
				
			});
			JToolBar t = new JToolBar();
			t.add(UIHelper.createButton("refresh","Refresh Content",Config.getIconProperty("icon.menu.refresh"),this));
			t.add(Box.createHorizontalGlue());
			t.add(UIHelper.createButton("clear","Delete All Content",Config.getIconProperty("icon.menu.delete"),this));
			info = new UIHelper.HTMLPanel();
			info.setEditable(false);
			JScrollPane s = new JScrollPane(info);
			s.setPreferredSize(new Dimension(200,200));
			add(t,BorderLayout.NORTH);
			add(new JScrollPane(list),BorderLayout.CENTER);
			add(s,BorderLayout.SOUTH);
			content = c;
			refresh();
		}
		
		public void setContent(Object c){
			content = c;
			refresh();
		}
		
		/**
		 * refresh list model
		 */
		public void refresh(){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if(content instanceof List)
						list.setModel(new UIHelper.ListModel((List)content));
					else if(content instanceof Collection)
						list.setModel(new UIHelper.ListModel(convert((Collection)content)));
					else if(content instanceof Map)
						list.setModel(new UIHelper.ListModel(convert(((Map)content).values())));
					list.repaint();
					info.setText("");
				}
			});
		}
		
		/**
		 * convert
		 * @param l
		 * @return
		 */
		private List convert(Collection in){
			List out = new ArrayList();
			for(Object o: in){
				if(o instanceof ConceptEntry){
					ConceptEntry e = (ConceptEntry)o;
					if(Arrays.asList(TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ATTRIBUTE).contains(e.getType()) && !e.equals(e.getParentEntry())){
						continue;
					}
				}
				out.add(o);
			}
			return out;
		}
		

		/**
		 * show object info
		 * @param o
		 */
		private void info(Object o){
			if(o == null)
				return;
			StringBuffer text = new StringBuffer();
			if(o instanceof ConceptEntry){
				ConceptEntry e = (ConceptEntry) o;
				text.append("<b>"+e.getText()+"</b><br>("+e.getId()+")<hr>");
				if(e.isFinding()){
					String st = (ConceptEntry.INCORRECT == e.getFeature().getConceptStatus())?"red":"black";
					text.append("feature: <b><font color=\""+st+"\">"+e.getFeature()+"</font> ("+e.getFeature().getId()+") </b><br>");
					List<ConceptEntry> list = e.getAttributes();
					if(list.isEmpty())
						list = e.getFeature().getAttributes();
					for(ConceptEntry a: list){
						st = (ConceptEntry.INCORRECT == a.getConceptStatus())?"red":"black";
						text.append("attribute: <b><font color=\""+st+"\">"+a+"</font> ("+a.getId()+") </b><br>");
					}
				}
			}else if(o instanceof ErrorEntry){
				ErrorEntry e = (ErrorEntry) o;
				text.append("error:      <b>"+e.getError()+"</b><br>");
				text.append("concept:    <b>"+e.getConceptEntry()+"</b><br>");
				text.append("concept id: <b>"+e.getConceptEntry().getId()+"</b><br>");
				
				if(!ITS.getInstance().getTutors().isEmpty()){
					FeedbackModule fm = ITS.getInstance().getTutors().get(0).getFeedbackModule();
					if(fm instanceof HelpManager){
						HelpManager hm = (HelpManager) fm;
						ScenarioEntry se = hm.getScenarioSet().getScenarioEntry(e.getError());
						if(se != null){
							text.append("description:<br>"+se.getDescription());
						}
					}
				}
			}else {
				text.append(""+o);
			}
			info.setText(text.toString());
		}
		
		public void actionPerformed(ActionEvent e) {
			if("refresh".equals(e.getActionCommand())){
				refresh();
			}else if("clear".equals(e.getActionCommand())){
				try{
					if(content instanceof Collection)
						((Collection)content).clear();
					else if(content instanceof Map)
						((Map)content).clear();
				}catch(Exception ex){}
				refresh();
			}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ReasonerStateExplorer st = new ReasonerStateExplorer();
		st.setContent("TEST",Arrays.asList("apple","oragne","banana"));
		JOptionPane.showMessageDialog(null,st,"",JOptionPane.PLAIN_MESSAGE);

	}
}
