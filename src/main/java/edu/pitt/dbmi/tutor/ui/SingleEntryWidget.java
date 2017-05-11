package edu.pitt.dbmi.tutor.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.LineBorder;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;

/**
 * widget for storing a single entry
 * @author tseytlin
 */
public class SingleEntryWidget extends JPanel implements ActionListener, DropTargetListener  {
	public static final String ENTRY_CHANGED = "ENTRY_CHANGED";
	public static final Dimension SIZE = 
		new Dimension(Constants.CONCEPT_ICON_SIZE.width+5,
					  Constants.CONCEPT_ICON_SIZE.height+5); //184x28
	private JLabel label;
	private JList entry;
	private JPanel buttons;
	private boolean dynamicTitle = true;
	private EntryChooser entryChooser;
	
	/**
	 * create a widget
	 * @param title
	 */
	public SingleEntryWidget(String title){
		super();
		setLayout(new BorderLayout());
		label = new JLabel("                               ");
		if(title != null){
			label.setText(title);
			add(label,BorderLayout.NORTH);
		}
			
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		
		// init list
		entry = new JList(new DefaultListModel(){
			public void addElement(Object obj){
				removeAllElements();
				super.addElement(obj);
			}
		});
		entry.setBorder(new LineBorder(new Color(200,200,255)));
		entry.setVisibleRowCount(1);
		entry.setPreferredSize(SIZE);
		entry.setFixedCellWidth(SIZE.width);
		entry.setFixedCellHeight(SIZE.height);
		entry.setDragEnabled(true);
		entry.setCellRenderer(new EntryWidgetCellRenderer());
		new DropTarget(entry,this);
		p.add(entry,BorderLayout.CENTER);
		
		
		buttons = new JPanel();
		buttons.setLayout(new BorderLayout());
		JButton b = UIHelper.createButton("add","Add Entry",Config.getIconProperty("icon.menu.add"),this);
		b.setPreferredSize(new Dimension(24,24));
		b.setMaximumSize(new Dimension(24,24));
		buttons.add(b,BorderLayout.WEST);
		b = UIHelper.createButton("rem","Remove Entry",Config.getIconProperty("icon.menu.rem"),this);
		b.setPreferredSize(new Dimension(24,24));
		b.setMaximumSize(new Dimension(24,24));
		buttons.add(b,BorderLayout.EAST);
		p.add(buttons,BorderLayout.EAST);
		add(p,BorderLayout.CENTER);
	}
	

	/**
	 * @param entryChooser the entryChooser to set
	 */
	public void setEntryChooser(EntryChooser entryChooser) {
		this.entryChooser = entryChooser;
	}
	
	/**
	 * set entry
	 * @param obj
	 */
	public void setEntry(Object obj){
		if(obj != null)
			((DefaultListModel)entry.getModel()).addElement(obj);
	}
	
	/**
	 * get entry
	 * @return
	 */
	public Object getEntry(){
		if(entry.getModel().getSize() > 0)
			return ((DefaultListModel)entry.getModel()).getElementAt(0);
		return null;
	}
	
	/**
	 * is this thing editable
	 * @param b
	 */
	public void setEditable(boolean b){
		for(int i=0;i<buttons.getComponentCount();i++)
			buttons.getComponent(i).setEnabled(b);
	}
	
	public void setEnabled(boolean b){
		setEditable(b);
		entry.setBackground((b)?Color.white:getBackground());
		super.setEnabled(b);
	}
	
	public boolean isDynamicTitle() {
		return dynamicTitle;
	}


	public void setDynamicTitle(boolean dynamicTitle) {
		this.dynamicTitle = dynamicTitle;
	}

	
	
	/**
	 * button presses
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("add")){
			if(entryChooser != null){
				entryChooser.setSelectionMode(EntryChooser.SINGLE_SELECTION);
				entryChooser.showChooserDialog();
				if(entryChooser.isSelected()){
					setEntry(entryChooser.getSelectedObject());
					/*
					if(getEntry() instanceof ConceptEntry && isDynamicTitle()){
						ConceptEntry c = (ConceptEntry) getEntry();
						IClass [] par = c.getConceptClass().getDirectSuperClasses();
						String parent = (par.length > 0)?UIHelper.getPrettyClassName(""+par[0]):"Attribute";
						label.setText(parent);
						revalidate();
						
					}*/
				}
			}
		}else if(cmd.equals("rem")){
			((DefaultListModel)entry.getModel()).removeAllElements();
		}
		firePropertyChange(ENTRY_CHANGED,null,this);
	}
	
	/**
	 * drag-n-drop support, this is when drop occures
	 * @param dtde
	 */
	public void drop(DropTargetDropEvent dtde){
		if(dtde.getSource() instanceof DropTarget){
        	DropTarget droptarget = (DropTarget) dtde.getSource();
        	if(droptarget.getComponent() instanceof JList){
        		JList list = (JList) droptarget.getComponent();
        		try{
					//DataFlavor [] f = dtde.getTransferable().getTransferDataFlavors();
					//setEntry(dtde.getTransferable().getTransferData(f[0]));
        			String [] str = (""+dtde.getTransferable().getTransferData(DataFlavor.stringFlavor)).split("\n");
        			if(str.length > 0)
        				setEntry(str[0]);
        			list.repaint();
				}catch(Exception ex){
					ex.printStackTrace();
				}
        	}
        }
	}
	
	// don't need those methods for now
    public void dragEnter(DropTargetDragEvent dtde) {}
    public void dragExit(DropTargetEvent dte) {}
    public void dragOver(DropTargetDragEvent dtde) {}
    public void dropActionChanged(DropTargetDragEvent dtde) {}
	
	public static void main(String [] args){
		JOptionPane.showMessageDialog(null,new SingleEntryWidget("Test"));
	}
}
