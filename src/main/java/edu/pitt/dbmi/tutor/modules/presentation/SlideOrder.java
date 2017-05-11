package edu.pitt.dbmi.tutor.modules.presentation;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.*;

import edu.pitt.dbmi.tutor.beans.SlideEntry;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;


/**
 * this object takes care of slide ordering
 * @author tseytlin
 *
 */
public class SlideOrder {
	//constunts
	private final int DELAY = 300;
	public final String ORDER_PENDING = "<html><font color=olive>pending...</font>";
	public final String ORDER_OK = "<html><font color=green>OK</font>";
	public final String ORDER_FAILED = "<html><font color=red>N/A</font>";
	public final String ORDER_PARTIAL = "<html><font color=purple>partial</font>";
	private final String LEVEL_STAIN = "HE/RHHE";
	public static final String ORDER_DONE = "OrderDone";
	public static final String ORDER_REQUEST = "OrderRequest";
	// vars
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private java.util.List tissueOrderList;
	private JPanel orderLevelPanel, orderStainPanel;
	private JTable tissueOrderTable;
	private String [] slideBlocks, slideStains;
	private java.util.List slides;//, availableStains;
	private AbstractButton orderLevel,orderStain;
	
	/**
	 * initialize slide order object
	 */
	public SlideOrder() {}
	
	/**
	 * initialize slide order object
	 */
	public SlideOrder(Collection slides) {
		setSlides(slides);
	}

	
	/**
	 * set slides
	 * @param Collection<SlideEntry>
	 */
	public void setSlides(Collection slides){
		this.slides = new ArrayList(slides);
		Collections.sort(this.slides);
		//availableStains = new ArrayList();
		//Collections.addAll(availableStains,createSlideStains());
	}
	
	
	/**
	 * add listener to this class
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	
	/**
	 * add listener to this class
	 * @param l
	 */
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	/**
	 * extract all possible blocks from slides
	 * @return
	 */
	private String [] createSlideBlocks(){
		if(slides == null)
			return new String[] { "A", "B", "C" };
		Set blocks = new HashSet();
		for(Iterator i=slides.iterator();i.hasNext();){
			SlideEntry slide = (SlideEntry) i.next();
			blocks.add(slide.getBlock());
		}
		return (String []) blocks.toArray(new String [0]);
	}
	
	/**
	 * extract all possible blocks from slides
	 * @return
	 */
	private String [] createSlideStains(){
		if(slides == null)
			return new String[] { "S100", "Melana A", "Font Mass", "Stain C" };
		Set blocks = new HashSet();
		for(Iterator i=slides.iterator();i.hasNext();){
			SlideEntry slide = (SlideEntry) i.next();
			blocks.add(slide.getStain());
		}
		return (String []) blocks.toArray(new String [0]);
	}
	
	/**
	 * get available slide blocks
	 * @return
	 */
	public String[] getSlideBlocks() {
		if(slideBlocks == null)
			slideBlocks = createSlideBlocks();
		return slideBlocks;
	}

	/**
	 * get available slide blocks
	 * @return
	 */
	public String[] getSlideStains() {
		if(slideStains == null)
			slideStains = createSlideStains();
		return slideStains;
	}

	/**
	 * show order level dialog
	 */
	public void showOrderLevel(Component parent) {
		JComponent top = getOrderLevelPanel();
		JComponent table = getOrderTable();
		JScrollPane bottom = new JScrollPane(table);
		bottom.setPreferredSize(new Dimension(300, 200));
		JComponent c = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
		JOptionPane.showMessageDialog(parent, c, "Order Levels", JOptionPane.PLAIN_MESSAGE);
		orderLevel.doClick();
		processOrder();
	}

	/**
	 * show order level dialog
	 */
	public void showOrderStain(Component parent) {
		JComponent top = getOrderStainPanel();
		JComponent table = getOrderTable();
		JScrollPane bottom = new JScrollPane(table);
		bottom.setPreferredSize(new Dimension(300, 200));
		JComponent c = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
		JOptionPane.showMessageDialog(parent, c, "Order Stain", JOptionPane.PLAIN_MESSAGE);
		orderStain.doClick();
		processOrder();
	}

	/**
	 * show order status
	 * @param p
	 */
	public void showOrderStatus(Component p){
		JComponent table = getOrderTable();
		JScrollPane c = new JScrollPane(table);
		c.setPreferredSize(new Dimension(300, 300));
		JOptionPane.showMessageDialog(p,c,"Order Status", JOptionPane.PLAIN_MESSAGE);
	}
	
	
	/**
	 * process order for given map
	 * @param map
	 */
	private void processOrder(Map map, boolean load){
		String block  = ""+map.get("block");
		String stain  = ""+map.get("stain");
		Integer levels = (Integer) map.get("levels");
		int lvl = levels.intValue();
		if(slides != null){
			for(Iterator i=slides.iterator();i.hasNext();){
				SlideEntry slide = (SlideEntry) i.next();
				String immuno = null;
				if(slide.getImmuno() != null){
					//if(slide.getImmuno() instanceof LexicalEntry)
					//	immuno = ((LexicalEntry)slide.getImmuno()).getName();
					//else 
						immuno = ""+slide.getImmuno();
				}
				
				// found relevant block
				if(block.equals(slide.getBlock())){
					// check stains or levels
					if(stain.equals(LEVEL_STAIN)){
						// more levels are requested
						//if(slide.getStain().endsWith("HE")){
						if(immuno == null || immuno.length() == 0 || immuno.endsWith("HE")){
							// if slide is already loaded don't bother
							if(!slide.isLoadedSlide() && lvl > 0){
								//	found level
								if(load)
									slide.setLoadedSlide(true);
								lvl --;
							}
						}
					}else{
						// immuno is requested
						if(immuno != null && stain.equals(immuno)){
							// found immuno
							if(load)
								slide.setLoadedSlide(true);
							lvl = 0;
						}
					}
				}
			}
		}
		// set status
		String status = ORDER_FAILED;
		if(lvl == 0)
			status = (load)?ORDER_OK:ORDER_PENDING;
		else if(lvl < levels.intValue())
			status = (load)?ORDER_PARTIAL:ORDER_PENDING;
		map.put("status",status);
	}
	
	
	/**
	 * get list of pending orders 
	 * Map has keys: block, stain, levels, status
	 * levels is Integer the rest are Strings
	 * @return List<Map> pending orders
	 */
	private java.util.List getPendingOrders(){
		ArrayList list = new ArrayList();
		synchronized (tissueOrderList) {
			// find pending orders
			for (int i = 0; i < tissueOrderList.size(); i++) {
				Map map = (Map) tissueOrderList.get(i);
				if (ORDER_PENDING.equals(map.get("status"))){
					list.add(map);
				}
			}
		}
		return list;
	}
	
	/**
	 * process order 
	 */
	private void processOrder() {
		// get pending orders
		final java.util.List pending = getPendingOrders();
		
		// place order on them
		if(pending.size() > 0){
			//	notify of request
			//pcs.firePropertyChange(ORDER_REQUEST,null,pending);
			ActionListener taskPerformer = new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					// process all pending orders
					for (int i = 0; i < pending.size(); i++) {
						Map map = (Map) pending.get(i);
						processOrder(map,true);
					}
					// notify that everything is done
					pcs.firePropertyChange(ORDER_DONE,null,null);
				}
			};
			// start a timer
			javax.swing.Timer timer = new javax.swing.Timer(DELAY, taskPerformer);
			timer.setRepeats(false);
			timer.start();
		}
	}

	/**
	 * get order level panel
	 * @return
	 */
	public JPanel getOrderLevelPanel() {
		if (orderLevelPanel == null)
			orderLevelPanel = createOrderLevelPanel();
		return orderLevelPanel;
	}

	/**
	 * create order level panel
	 * @return
	 */
	private JPanel createOrderLevelPanel() {
		String[] blocks = getSlideBlocks();
		final Map orderLevelMap = new HashMap();
		JPanel panel = new JPanel();
		panel.setBorder(LineBorder.createGrayLineBorder());
		panel.setLayout(new GridLayout(blocks.length, 3));
		// iterate over blocks
		for (int i = 0; i < blocks.length; i++) {
			JCheckBox chk = new JCheckBox("<html>block <b>" + blocks[i] + "</b>");
			chk.setFont(chk.getFont().deriveFont(Font.PLAIN));
			JTextField txt = new JTextField(3);
			JLabel lbl = new JLabel("# of levels:");
			lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
			txt.setDocument(new NumberDocument());
			txt.setHorizontalAlignment(JTextField.RIGHT);
			panel.add(chk);
			panel.add(lbl);
			panel.add(txt);
			// add components to map
			orderLevelMap.put(blocks[i] + "check", chk);
			orderLevelMap.put(blocks[i] + "text", txt);
		}

		// ceate order button
		JButton order = new JButton("Add to Order");
		order.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] blocks = getSlideBlocks();
				for (int i = 0; i < blocks.length; i++) {
					JCheckBox chck = (JCheckBox) orderLevelMap.get(blocks[i] + "check");
					JTextField text = (JTextField) orderLevelMap.get(blocks[i] + "text");
					
					// if there is something in text field, then select chck
					if(text.getText().length() > 0)
						chck.setSelected(true);
					
					// add to order list of block was selected
					if (chck != null && text != null && chck.isSelected()) {
						String txt = text.getText();
						if (txt.length() == 0)
							txt = "1";
						Map map = new HashMap();
						map.put("block", blocks[i]);
						map.put("stain", LEVEL_STAIN);
						map.put("levels", new Integer(txt));
						map.put("status", ORDER_PENDING);
						
						// clear text field and check box
						text.setText("");
						chck.setSelected(false);
						
						// check availability 
						processOrder(map,false);
						// notify of order request
						pcs.firePropertyChange(ORDER_REQUEST,null,map);
						tissueOrderList.add(map);
					}
				}
				getOrderTable().revalidate();
				//processOrder();
			}
		});
		orderLevel = order;

		// create uber panel
		JPanel pnl = new JPanel();
		pnl.setLayout(new BorderLayout());
		pnl.add(panel, BorderLayout.CENTER);
		pnl.add(order, BorderLayout.SOUTH);
		return pnl;
	}

	/**
	 * get order level panel
	 * @return
	 */
	public JPanel getOrderStainPanel() {
		if (orderStainPanel == null)
			orderStainPanel = createOrderStainPanel();
		return orderStainPanel;
	}

	/**
	 * create order stain component
	 * @return
	 */
	private JPanel createOrderStainPanel() {
		JPanel panel = new JPanel();
		BoxLayout box = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(box);
		panel.setBorder(LineBorder.createGrayLineBorder());

		// block selector
		final JComboBox blockCombo = new JComboBox(getSlideBlocks());
		final JComboBox stainCombo = new JComboBox(new DefaultComboBoxModel());
		stainCombo.setEditable(true);
		// take care of auto-completion
		final JTextField text = (JTextField) stainCombo.getEditor().getEditorComponent();
		text.addKeyListener(new KeyListener() {
			private StringBuffer s;
			public void keyPressed(KeyEvent evt) {}
			public void keyTyped(KeyEvent evt) {}
			
			public void keyReleased(KeyEvent evt) {
				// clear buffer if nothing is in
				if(s == null || text.getText().length() == 0 || stainCombo.getItemCount() == 0)
					s = new StringBuffer();
				// get keycode and append to buffer
				int  c = evt.getKeyCode();
				char h = evt.getKeyChar();
				
				// if special char or word char
				if(c == KeyEvent.VK_BACK_SLASH || c == KeyEvent.VK_BACK_QUOTE || (""+h).matches("\\w")){
					s.append(""+h);
					sync(""+s);
				}
			}
			

			//sync combobox w/ what is typed in
			private void sync(String str) {
				//str = ""+stainCombo.getSelectedItem();
				stainCombo.removeAllItems();
				stainCombo.hidePopup();
				String[] stains = getSlideStains();
				for (int i = 0; i < stains.length; i++) {
					if(stains[i].toLowerCase().startsWith(str.toLowerCase())) {
						// add stains that starts w/ what you typed
						stainCombo.addItem(stains[i]);
					}else if(str.equals("\\")){
						// add ALL possible stains (debug mode)
						stainCombo.addItem(stains[i]);
					}else if(str.equals("`") && slideAvailable(stains[i])){
						// add only available stains
						stainCombo.addItem(stains[i]);
					}
				}
				stainCombo.revalidate();
				stainCombo.showPopup();
			}
			
			// check if slide w/ stain is available
			private boolean slideAvailable(String stain){
				if(slides == null)
					return false;
				
				for(Iterator i=slides.iterator();i.hasNext();){
					SlideEntry se = (SlideEntry) i.next();
					if(se.getImmuno() != null){
						String immuno = null;
						//if(se.getImmuno() instanceof LexicalEntry)
						//	immuno = ((LexicalEntry)se.getImmuno()).getName();
						//else 
							immuno = ""+se.getImmuno();
						if(immuno.toLowerCase().equals(stain.toLowerCase()))
							return true;
					}
				}
				return false;
			}
			
		});
		panel.add(new JLabel("block:  "));
		panel.add(blockCombo);
		panel.add(new JLabel("  immuno:  "));
		panel.add(stainCombo);

		//	ceate order button
		JButton order = new JButton("Add to Order");
		order.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String block = ""+blockCombo.getSelectedItem();
				String stain = (String) stainCombo.getSelectedItem();

				// add to table
				if(stain != null && stain.length() > 0){
					Map map = new HashMap();
					map.put("block", block);
					map.put("stain", stain);
					map.put("levels", new Integer(1));
					map.put("status", ORDER_PENDING);
					
					// check availability 
					processOrder(map,false);
					//notify
					pcs.firePropertyChange(ORDER_REQUEST,null,map);
					
					// reset chooser
					stainCombo.setSelectedItem("");
					stainCombo.removeAllItems();
					
					tissueOrderList.add(map);
					getOrderTable().revalidate();
				}
				//processOrder();
			}
		});
		orderStain = order;

		// create uber panel
		JPanel pnl = new JPanel();
		pnl.setLayout(new BorderLayout());
		pnl.add(panel, BorderLayout.CENTER);
		pnl.add(order, BorderLayout.SOUTH);
		return pnl;
	}

	/**
	 * get tissue order table
	 * @return
	 */
	private JTable getOrderTable() {
		if (tissueOrderTable == null)
			tissueOrderTable = createOrderTable();
		return tissueOrderTable;
	}

	/**
	 * create tissue order panel
	 */
	private JTable createOrderTable() {
		tissueOrderList = new ArrayList();
		JTable table = new JTable(new TissueOrderTableModel());
		return table;
	}

	/**
	 * table model for displaying tissue order status
	 * @author tseytlin
	 */
	private class TissueOrderTableModel extends AbstractTableModel {
		public String[] keys = new String[] { "block", "stain", "levels", "status" };

		/**
		 * get value at location
		 */
		public Object getValueAt(int row, int col) {
			Map map = (Map) tissueOrderList.get(row);
			Object value = map.get(keys[col]);
			return value;
		}

		/**
		 * get number of rows;
		 */
		public int getRowCount() {
			return tissueOrderList.size();
		}

		/**
		 * get number of columns
		 * @return
		 */
		public int getColumnCount() {
			return keys.length;
		}

		/**
		 * get name of a column
		 */
		public String getColumnName(int col) {
			return keys[col];
		}
	}

	/**
	 * filter text input to allow only numbers
	 * @author tseytlin
	 */
	private class NumberDocument extends DefaultStyledDocument {
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			if (str.matches("\\d*"))
				super.insertString(offs, str, a);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		SlideOrder order = new SlideOrder();
		order.showOrderLevel(null);
		order.showOrderStain(null);
		
		Thread.sleep(7000);
		order.showOrderStatus(null);
	}

	/**
	 * @param slideBlocks the slideBlocks to set
	 */
	public void setSlideBlocks(String[] slideBlocks) {
		this.slideBlocks = slideBlocks;
	}

	/**
	 * @param slideStains the slideStains to set
	 */
	public void setSlideStains(String[] slideStains) {
		this.slideStains = slideStains;
	}

}
