package edu.pitt.dbmi.tutor.ui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.*;
import javax.swing.border.LineBorder;

import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;

/**
 * widget for storing a single entry
 * @author tseytlin
 */
public class MultipleEntryWidget extends JPanel implements ActionListener, DropTargetListener {
	public static final String ENTRY_CHANGED = "ENTRY_CHANGED";
	public static final Dimension SIZE = new Dimension(184,28);
	
	//, DragGestureListener, DragSourceListener {
	private JList entry;
	private JPanel buttons;
	//private DragSource source;
	private EntryChooser entryChooser;
	
	
	/**
	 * create a widget
	 * @param title
	 */
	public MultipleEntryWidget(String title){
		super();
		setLayout(new BorderLayout());
		// set label
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		if(title != null)
			p.add(new JLabel(title),BorderLayout.CENTER);
		
		buttons = new JPanel();
		buttons.setLayout(new BorderLayout());
		JButton b = UIHelper.createButton("add","Add Entry",Config.getIconProperty("icon.menu.add"),this);
		b.setPreferredSize(new Dimension(24,24));
		buttons.add(b,BorderLayout.WEST);
		b = UIHelper.createButton("rem","Remove Entry",Config.getIconProperty("icon.menu.rem"),this);
		b.setPreferredSize(new Dimension(24,24));
		buttons.add(b,BorderLayout.EAST);
		p.add(buttons,BorderLayout.EAST);
		add(p,BorderLayout.NORTH);
		// init list
		entry = new JList(new DefaultListModel());
		entry.setCellRenderer(new EntryWidgetCellRenderer());
		entry.setBorder(new LineBorder(new Color(200,200,255)));
		entry.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		//entry.setPreferredSize(new Dimension(150,120));
		entry.setDragEnabled(true);
		JScrollPane scroll = new JScrollPane(entry);
		scroll.setPreferredSize(new Dimension(150,120));
		new DropTarget(entry,this);
		//source = new DragSource();
		//source.createDefaultDragGestureRecognizer(entry,DnDConstants.ACTION_MOVE,this);
		add(scroll,BorderLayout.CENTER);
	}
	
	public void setDragEnabled(boolean b){
		entry.setDragEnabled(b);
	}
	
	public void setListLayout(int i){
		entry.setLayoutOrientation(i);
	}
	
	public void setVisibleRowCount(int r){
		entry.setVisibleRowCount(r);
	}
	
	/**
	 * set entry
	 * @param obj
	 */
	public void addEntry(Object o){
		final Object obj = o;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				((DefaultListModel)entry.getModel()).addElement(obj);
				revalidate();
				firePropertyChange(ENTRY_CHANGED,null,this);
			}
		});
	}
	

	/**
	 * set entry
	 * @param obj
	 */
	public void addEntries(Object [] o){
		final Object [] obj = o;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				for(int i=0;i<obj.length;i++)
					addEntry(obj[i]);
				revalidate();
				firePropertyChange(ENTRY_CHANGED,null,this);
			}
		});
	}
	
	/**
	 * set entry
	 * @param obj
	 */
	public void addEntries(Collection c){
		final Collection col = c;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				for(Object obj: col)
					addEntry(obj);
				revalidate();
				firePropertyChange(ENTRY_CHANGED,null,this);
			}
		});
	}
	
	/**
	 * get entry
	 * @return
	 */
	public Object []  getEntries(){
		return ((DefaultListModel)entry.getModel()).toArray();
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
	
	
	/**
	 * button presses
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("add")){
			if(entryChooser != null){
				entryChooser.setSelectionMode(EntryChooser.MULTIPLE_SELECTION);
				entryChooser.showChooserDialog();
				if(entryChooser.isSelected()){
					addEntries(entryChooser.getSelectedObjects());
				}
			}
		}else if(cmd.equals("rem")){
			final Object [] obj = entry.getSelectedValues();
			if(obj != null){
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						for(int i=0;i<obj.length;i++)
							((DefaultListModel)entry.getModel()).removeElement(obj[i]);
						revalidate();
						firePropertyChange(ENTRY_CHANGED,null,this);
					}
				});
			}
		}
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
					//addEntry(dtde.getTransferable().getTransferData(f[0]));
        			addEntry(dtde.getTransferable().getTransferData(DataFlavor.stringFlavor));
					list.repaint();
				}catch(Exception ex){
					ex.printStackTrace();
				}
        	}
        }
	}

	/**
	 * @param entryChooser the entryChooser to set
	 */
	public void setEntryChooser(EntryChooser entryChooser) {
		this.entryChooser = entryChooser;
	}
	
	/**
	 * Drag Gesture Handler
	 *
	public void dragGestureRecognized(DragGestureEvent dge) {
		Object obj = entry.getSelectedValue();
		// start with a valid move cursor:
		source.startDrag(dge,DragSource.DefaultMoveDrop,new EntryTransferable(obj),this);
	}
	*/
	
	// don't need those methods for now
    public void dragEnter(DropTargetDragEvent dtde) {}
    public void dragExit(DropTargetEvent dte) {}
    public void dragOver(DropTargetDragEvent dtde) {}
    public void dropActionChanged(DropTargetDragEvent dtde) {}
	
    //dragsource events
    /*
    public void dragEnter(DragSourceDragEvent dsde) {}
    public void dragExit(DragSourceEvent dse) {}
    public void dragOver(DragSourceDragEvent dsde) {}
    public void dropActionChanged(DragSourceDragEvent dsde) {}
    public void dragDropEnd(DragSourceDropEvent dsde) {}
    */
	public static void main(String [] args){
		JOptionPane.showMessageDialog(null,new MultipleEntryWidget("Test"));
	}


}