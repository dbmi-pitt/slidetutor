package edu.pitt.dbmi.tutor.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.IClass;

/**
 * create a tree dialog that returns selected values
 * @author tseytlin
 */
public class TreeDialog extends JDialog implements EntryChooser, ActionListener {
	public static int SINGLE_SELECTION = 1;
	public static int MULTIPLE_SELECTION = 2;
	
	private TreePanel treePanel;
	private JList selectionList;
	private JButton ok;
	private int selectionMode = MULTIPLE_SELECTION; 
	private String selectedNode, searchText;
	private JSplitPane splitPanel;
	private JPanel sidePanel;
	private Component buttonPanel;
	private Boolean selectedNodAdd;
	private String type;
	
	public String getDialogType() {
		return type;
	}


	public void setDialogType(String type) {
		this.type = type;
	}


	/**
	 * create dialog
	 * @param name
	 * @param p
	 */
	public static TreeDialog createDialog(Window frame) {
		if(frame instanceof Dialog)
			return new TreeDialog((Dialog) frame);
		else if(frame instanceof Frame)
			return new TreeDialog((Frame) frame);
		return null;
	}
	
	
	public void setBackground(Color c) {
		treePanel.setBackground(c);
	}

	/**
	 * create dialog
	 * @param name
	 * @param p
	 */
	public TreeDialog(Dialog frame) {
		super(frame);
		createGUI();
	}
	
	/**
	 * create dialog
	 * @param name
	 * @param p
	 */
	public TreeDialog(Frame frame) {
		super(frame);
		createGUI();
	}
	
		
	private void createGUI(){	
		setResizable(true);
		setModal(true);
		
		// setIconifiable(false);
		getContentPane().setLayout(new BorderLayout());
		treePanel = new TreePanel();

		// create buttons
		buttonPanel = createSelectCancelButtons();
		
		// create right component
		sidePanel = new JPanel();
		sidePanel.setMinimumSize(new Dimension(1,1));
		sidePanel.setLayout(new BorderLayout());
		setupSidePanel();

		splitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPanel.setTopComponent(treePanel);
		splitPanel.setBottomComponent(sidePanel);
		splitPanel.setResizeWeight(1);
		//split.setDividerLocation(700);
		getContentPane().add(splitPanel, BorderLayout.CENTER);
		pack();
		splitPanel.setDividerLocation(splitPanel.getPreferredSize().width - 230);

		// add window listener
		addWindowListener(new WindowAdapter() {
			public void windowDeactivated(WindowEvent e) {
				if (isVisible())
					toFront();
			}
			public void windowClosing(WindowEvent e) {
				((DefaultListModel)selectionList.getModel()).clear();
				setVisible(false);
			}
		});

		//set size
		//Dimension d = SlideTutor.getInstance().getFrame().getSize();
		//setSize(new Dimension(d.width-150,d.height-200));
		setPreferredSize(new Dimension(900, 650));
	}
	
	private void setupSidePanel(){
		if(sidePanel != null){
			sidePanel.removeAll();
			sidePanel.add(createAddRemoveButtons(),(selectionMode == MULTIPLE_SELECTION)?BorderLayout.NORTH:BorderLayout.WEST);
			sidePanel.add(createSelectionList(), BorderLayout.CENTER);
			sidePanel.add(buttonPanel, BorderLayout.SOUTH);
		}
	}

	/**
	 * set root (does it in a thread)
	 * @param cls
	 */
	public void setRoot(TreeNode node){
		treePanel.setBusy(true);
		final TreeNode cls = node;
		(new Thread(new Runnable(){
			public void run(){
				if(cls != null)
					treePanel.setRoot(cls);
				else
					treePanel.clear();
				treePanel.setBusy(false);
			}
		})).start();
	}
	
	/**
	 * set root (does it in a thread)
	 * @param cls
	 */
	public void setRoots(TreeNode [] node){
		if(node.length < 1)
			return;
		treePanel.setBusy(true);
		final TreeNode [] cls = node;
		(new Thread(new Runnable(){
			public void run(){
				treePanel.setRoots(cls);
				treePanel.setBusy(false);
			}
		})).start();
	}
	
	public void dispose(){
		treePanel.dispose();
		super.dispose();
	}
	
	/**
	 * get access to tree panel
	 * @return
	 */
	public TreePanel getTreePanel(){
		return treePanel;
	}
	
	
	public void setSelectedNode(String text){
		selectedNode = text;
		selectedNodAdd = Boolean.TRUE;
	}
	
	public void setHighlightedNode(String text){
		selectedNode = text;
		selectedNodAdd = Boolean.FALSE;
	}
	
	
	/**
	 * clear tree before display
	 */
	public void setVisible(boolean vis) {
		final boolean b = vis;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				if (b) {
					treePanel.initialize();
					((DefaultListModel) selectionList.getModel()).clear();
					
					// select node
					if(selectedNode != null && selectedNodAdd != null)
						treePanel.setSelectedNode(selectedNode,selectedNodAdd.booleanValue());
					
				
					if(searchText != null)
						treePanel.setSearchText(searchText);
				}else{
					treePanel.clearSelectedNodes();
					selectedNode = null;
					
				}
				
			}
		});
		if(b)
			UIHelper.centerWindow(TreeDialog.this.getOwner(),TreeDialog.this);
		TreeDialog.super.setVisible(b);
		
		if(searchText != null){
			treePanel.requestFocusOnSearch();
			searchText = null;
		}
	}
	
	public void setSearchText(String text){
		this.searchText = text;
	}
	
	
	/**
	 * create select/cancel buttons
	 * @return
	 */
	private Component createSelectCancelButtons() {
		JPanel bot = new JPanel();
		ok = new JButton("  OK  ");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// if nothing is selected, use the last selected noe
				if(getSelectedPaths().length == 0){
					TreePath [] path = treePanel.addSelectedTreeNodes();
					if(path != null){
						for(int i=0;i<path.length;i++)
							addTreePath(path[i]);
					}
				}
				setVisible(false);
			}
		});
		//ok.setEnabled(false);
		bot.setLayout(new FlowLayout());
		bot.add(ok);
		JButton cancel = new JButton("CANCEL");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				((DefaultListModel)selectionList.getModel()).clear();
				setVisible(false);
			}
		});
		bot.setLayout(new FlowLayout());
		bot.add(cancel);
		return bot;
	}

	/**
	 * create select/cancel buttons
	 * @return
	 */
	private Component createAddRemoveButtons() {
		JToolBar toolbar = new JToolBar();
		if(selectionMode == MULTIPLE_SELECTION){
			toolbar.add(UIHelper.createButton("add","Add to Selection",
					    UIHelper.getIcon(Config.getProperty("icon.toolbar.add"),24),this));
			toolbar.add(UIHelper.createButton("rem","Remove from Selection",
						UIHelper.getIcon(Config.getProperty("icon.toolbar.rem"),24),this));
			toolbar.add(Box.createHorizontalGlue());
		}
		return toolbar;
	}

	/**
	 * create action
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("add")){
			TreePath [] path = treePanel.addSelectedTreeNodes();
			if(path != null){
				for(int i=0;i<path.length;i++){
					addTreePath(path[i]);
				}
			}
		}else if(cmd.equals("rem")){
			DefaultListModel model = (DefaultListModel) selectionList.getModel();
			Object[] o = selectionList.getSelectedValues();
			TreePath [] path = new TreePath [o.length];
			for (int i = 0; i < o.length; i++) {
				PathIcon pi = (PathIcon) o[i];
				model.removeElement(pi);
				path[i] = pi.getPath();
			}
			treePanel.removeSelectedTreeNodes(path);
			//ok.setEnabled(!model.isEmpty());
		}
	}
	
	
	/**
	 * add TreePath to selection
	 * @param p
	 */
	public void addTreePath(TreePath p){
		if(p == null)
			return;
		DefaultListModel model = (DefaultListModel)selectionList.getModel();
		if(selectionMode == SINGLE_SELECTION)
			model.removeAllElements();
		PathIcon icon = new PathIcon(p);
		if(!model.contains(icon))
			model.addElement(icon);
		//ok.setEnabled(true);
	}
	
	public void removeTreePaths(){
		DefaultListModel model = (DefaultListModel) selectionList.getModel();
		Object[] o = model.toArray();
		TreePath [] path = new TreePath [o.length];
		for (int i = 0; i < o.length; i++) {
			PathIcon pi = (PathIcon) o[i];
			model.removeElement(pi);
			path[i] = pi.getPath();
		}
		treePanel.removeSelectedTreeNodes(path);
		model.clear();
		//ok.setEnabled(false);
	}
	
	public void doOK(){
		//if(ok.isEnabled())
		ok.doClick();
	}
	
	/**
	 * create select/cancel buttons
	 * @return
	 */
	private Component createSelectionList() {
		selectionList = new JList(new DefaultListModel());
		JScrollPane scroll = new JScrollPane(selectionList);
		scroll.setMinimumSize(new Dimension(200, 200));
		return scroll;
	}


	/**
	 * get tree paths
	 */
	public TreePath getSelectedPath(){
		TreePath [] paths = getSelectedPaths();
		return (paths.length > 0)?paths[0]:null;
	}
	
	/**
	 * get tree paths
	 */
	public TreePath [] getSelectedPaths(){
		DefaultListModel model = (DefaultListModel)selectionList.getModel();
		TreePath [] paths = new TreePath [model.getSize()];
		for(int i=0;i<model.getSize();i++){
			paths[i] = ((PathIcon)model.getElementAt(i)).getPath();
		}
		return paths;
	}
	
	
	/**
	 * Icon that represents a path to selection
	 * @author tseytlin
	 */
	public static class PathIcon implements Icon {
		private Color color = new Color(100,255,100,50);
		private Stroke stroke = new BasicStroke(2);
		private TreePath path;
		private String text,name;
		private Point offset = new Point(4,2);
		private Font font = new Font("Dialog",Font.PLAIN,12);
		private boolean trancated;
		
		// create PathIcon object
		public PathIcon(TreePath path){
			this.path = path;
			// extract name
			Object [] p = path.getPath();
			text = ""+p[p.length-1];
			// if general term, then name is next
			if(text.equals("*"))
				text = ""+p[p.length-2];
			name = text = UIHelper.getTextFromName(text);
		}
	
		
		/**
		 * equals to 
		 */
		public boolean equals(Object obj){
			if(obj instanceof PathIcon){
				return path.equals(((PathIcon) obj).getPath());
			}
			return false;
		}
		
		
		public int hashCode(){
			return path.hashCode();
		}
		
		/**
		 * get path that this represents
		 * @return
		 */
		public TreePath getPath(){
			return path;
		}
		
		// get height
		public int getIconHeight(){
			return 24;
		}
		// get width
		public int getIconWidth(){
			return 190;
		}
		
		// paint graphics
		public void paintIcon(Component c, Graphics g, int x, int y){
			int w = getIconWidth()-2*offset.x;
			int h = getIconHeight()-2*offset.y;
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setStroke(stroke);
			
			// fill rectangle
			g.setColor(color);
			g.fillRect(x+offset.x,y+offset.y,w,h);
			
			// truncate text
			if(!trancated){
				FontMetrics fm = c.getFontMetrics(font);
				int n = fm.stringWidth(text);
				if(n > getIconWidth()){
					String s = text;
					int k = (int)(w/fm.stringWidth("W"));
					do{
						s = text.substring(0,k);
					}while(k++ < text.length() &&  fm.stringWidth(s) < (w-20));
					text = s;
				}
				trancated = true;
			}
			// draw text
			g.setFont(font);
			g.setColor(Color.black);
			g.drawString(text,x+5+offset.x,y+h-7+offset.x);
			// do outline
			g.setColor(Color.green);
			g.drawRect(x+offset.x,y+offset.y,w,h);
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
	}
	
	/**
	 * set owner
	 *
	public void setOwner(Frame f){
		//TODO:
	}
	*/
	
	
	/**
	 * display chooser dialog for a given resource
	 */
	public void showChooserDialog(){
		setVisible(true);
	}
	
	/**
	 * set selection mode SINGLE_SELECTION vs MULTIPLE_SELECTION
	 * @param mode
	 */
	public void setSelectionMode(int mode){
		this.selectionMode = mode;
		treePanel.setSingleSelectionMode(mode == SINGLE_SELECTION);
		if(mode == SINGLE_SELECTION){
			setupSidePanel();
			splitPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);
			//getContentPane().remove(splitPanel);
			//getContentPane().add(treePanel,BorderLayout.CENTER);
			//getContentPane().add(buttonPanel,BorderLayout.SOUTH);
			pack();
			splitPanel.setDividerLocation(getPreferredSize().height-110);
		}else{
			setupSidePanel();
			splitPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			//getContentPane().remove(treePanel);
			//getContentPane().remove(buttonPanel);
			//getContentPane().add(splitPanel, BorderLayout.CENTER);
			pack();
			splitPanel.setDividerLocation(splitPanel.getPreferredSize().width - 230);
		}
	}
	
	/**
	 * set selection mode SINGLE_SELECTION vs MULTIPLE_SELECTION
	 * @param mode
	 */
	public int getSelectionMode(){
		return selectionMode;
	}
	
	/**
	 * was selection made in the dialog
	 * @return
	 */
	public boolean isSelected(){
		return getSelectedNodes() != null;
	}
	
	/**
	 * get objects that were selected 
	 * @return
	 */
	public TreePath [] getSelectedNodes(){
		return getSelectedPaths();
		/*
		TreePath [] paths =  getSelectedPaths();
		String [] e = new String [paths.length];
		for(int i=0;i<paths.length;i++){
			e[i] = ""+paths[i].getLastPathComponent();
		}
		return e;
		*/
	}
	
	/**
	 * get object that was selected 
	 * @return
	 */
	public TreePath getSelectedNode(){
		TreePath [] paths = getSelectedNodes();
		return (paths != null && paths.length > 0)?paths[0]:null;
	}


	public void setOwner(Frame frame) {
		//this.owner = frame;
	}
	
	/**
	 * get objects that were selected 
	 * @return
	 */
	public Object [] getSelectedObjects(){
		TreePath [] paths =  getSelectedPaths();
		ConceptEntry [] e = new ConceptEntry [paths.length];
		for(int i=0;i<paths.length;i++){
			e[i] = new ConceptEntry(paths[i],getDialogType());
		}
		return e;
	}
	
	/**
	 * get object that was selected 
	 * @return
	 */
	public Object getSelectedObject(){
		Object [] paths = getSelectedObjects();
		return (paths != null && paths.length > 0)?paths[0]:null;
	}
	
	
}
