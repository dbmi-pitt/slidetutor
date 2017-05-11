package edu.pitt.dbmi.tutor.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.umd.cs.spacetree.*;
import edu.umd.cs.spacetree.nodes.*;
import edu.umd.cs.spacetree.thumbnails.*;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.event.*;

/**
 * Most of this code was copy/pasted from ApplicationFrame class of SpaceTree project.
 * This was done as a workaround to limitations of SpaceTree API.
 * This panel 
 */

public class TreePanel extends JPanel implements ActionListener  {
	public static final int DEFINITION_MODE = 1;
	public static final int DIAGNOSTIC_RULE_MODE = 2;
	
	// sometimes parents of selected nodes need to be included, this is how
	private String[] exceptions; 
	public static int SEARCH_RESULTS_WARNING_LIMIT = 100;
	public static int MAX_NODES_IN_THUMB_MINTREE = 100;
	private final String HELP_STRING = "<html><b>right-click</b> adds selected concept to a list, <b>double-click</b> selects the concept and closes";
	
	
	protected JTextField searchField;//,selectionField;	
	protected boolean searchFieldJustGotFocus;
	protected JToolBar toolBar;
	protected ArrayList searchResults;
	protected JButton goButton;
	protected JButton resetButton;
	private JProgressBar progress;
	private JPanel status;
	
	//protected JLabel logo;
	protected ActionListener goActionListener;
	protected ActionListener resetActionListener;
	
	
	//protected SpaceTreeEventHandler eventHandler;
	//protected PCanvas canvas;
	//protected PSpaceTree spaceTree;
	private TreeContainer tree,preview;
		
	private boolean singleSelectionMode;
	private boolean editable,dirty;
	private int colorMode;
	private Vector treeSelectionListeners;
	private Vector treeExpansionListeners;
	private TreeDialog treeDialog;
	private JDialog previewDialog;
	//private JPopupMenu popup;
	private StringBuffer [] pasteBuffer;
	private List<TextViewNode> selectedNodes;
	private static List<TextViewNode> highlightedNodes = new ArrayList<TextViewNode>();
	
	// for sending events to protocol
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/****************************************************************
	 * Initialization
	 ****************************************************************/
	public TreePanel() {
		super();
		// create toolbar
		toolBar = createToolBar();
		
		// create canvase
		tree = createTreeContainer();
		tree.panel = this;
	
		// draw it up
		JPanel contentPane = this;
		contentPane.setLayout(new BorderLayout());
		contentPane.setPreferredSize(new Dimension(640, 480));
		contentPane.add(toolBar, BorderLayout.NORTH);
		contentPane.add(tree.canvas, BorderLayout.CENTER);

		status = new JPanel();
		status.setLayout(new BorderLayout());
		JLabel lbl = new JLabel(HELP_STRING);
		lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 10));
		status.add(lbl, BorderLayout.SOUTH);
		contentPane.add(status, BorderLayout.SOUTH);

		tree.canvas.requestFocus();

		// init listener vectors
		treeSelectionListeners = new Vector();
		treeExpansionListeners = new Vector();
		
		// progress bar
		progress = new JProgressBar();
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		progress.setIndeterminate(true);
		
		// register with change listener
	}
	
	
	/**
	 * create canvase
	 * @return
	 */
	private TreeContainer createTreeContainer(){
		// create canvase
		// this is a hack around mystery paint exceptions we keep getting
		PCanvas canvas = new PCanvas(){
			public void paintComponent(Graphics g) {
				try{
					super.paintComponent(g);
				}catch(Exception ex){
					System.err.println("WARNING: SpaceTree paint problem");
				}
			}
			public JToolTip createToolTip() {
				JToolTip tip = super.createToolTip();
			    tip.setBackground(new Color(255,255,150));
			    tip.setBorder(new LineBorder(Color.gray,2));
			    return tip;
			}
		};
		canvas.setPreferredSize(new Dimension(400, 400));
		
		// initialize	
		PSpaceTree spaceTree = new PSpaceTree(canvas.getCamera());
		canvas.getCamera().addLayer(spaceTree);
		canvas.getRoot().addChild(spaceTree);
		
		loadPreferences(spaceTree);
		canvas.removeInputEventListener(canvas.getZoomEventHandler());
		
		// init container
		TreeContainer tree = new TreeContainer();
		tree.canvas = canvas;
		tree.spaceTree = spaceTree;
		
		SpaceTreeEventHandler eventHandler = new SpaceTreeEventHandler(canvas,spaceTree);
		canvas.addInputEventListener(eventHandler);
		canvas.getRoot().getDefaultInputManager().setKeyboardFocus(eventHandler);
		tree.eventHandler = eventHandler;
		
		return tree;
	}
	
	
	/*************************************************************
	 * Preferences
	 *************************************************************/
	protected void loadPreferences(PSpaceTree spaceTree) {
		// setup some global settings
		PTreeNode.BOOKMARK_PAINT = Color.GREEN;
		PTreeNode.BORDER_WIDTH = 1;
		PSpaceTree.SPACE_TREE_INSET = 5;
		//PTreeNode.DRAW_BORDERS = false;

		if (spaceTree.getFocus() == null) {
			spaceTree.setFocus(spaceTree.getRootTreeNode());
		}

		spaceTree.setRootOrientation(3); // up,down,right,left
		spaceTree.setNodeJustification(0); // top,center,bottom

		// float values
		spaceTree.setSiblingSeparation(3);
		spaceTree.setSubtreeSeparation(10);
		spaceTree.setLevelSeparation(20);

		// boolean values
		spaceTree.setFoldChildrenLayout(true);
		spaceTree.setAdjustLayoutToShowPathToRoot(false);
		spaceTree.setCenterView(true);

		// long values
		spaceTree.setLayoutAnimationDuration(300);

		// thumbnail options
		// Arrow, Triangle, MiniatureAndTriangles, Numeric, MiniatureAndNumbers
		// substitute above values into P*ThumbnailNode
		spaceTree.setThumbnailFactory(new PThumbnailFactory() {
			public PThumbnailNode createThumbnailFor(PTreeNode treeNode) {
				//return new PMiniatureAndNumbersThumbnailNode(treeNode,100);
				return new PMiniatureAndTrianglesThumbnailNode(treeNode);
			}
		});

		// event handler delays (int)		
		SpaceTreeEventHandler.KEY_FOCUS_DELAY = 400;
		SpaceTreeEventHandler.MOUSE_FOCUS_DELAY = 50;

		// float values				
		PThumbnailNode.maxWidth = 50;
		PThumbnailNode.maxHeight = 50;

		// int value
		PThumbnailNode.numberScale = 0; // linear,sqrt,log
		//bool balues
		PThumbnailNode.relativeToRoot = true;
		PThumbnailNode.codeWidthHeightBySize = false;
		PThumbnailNode.codeDepth = true;
		PThumbnailNode.codeSize = true;
		PThumbnailNode.codeWidth = true;
		PThumbnailNode.drawBorder = true;

		if (PTreeNode.DRAW_BORDERS != PThumbnailNode.drawBorder) {
			PTreeNode.DRAW_BORDERS = PThumbnailNode.drawBorder;
			PTreeNode root = spaceTree.getRootTreeNode();

			if (root != null) {
				root.recomputeSubtreeNodeBounds();
				root.removeVisibleSubtreeThumbnails();
			}
		}

		if (spaceTree.getFocus() != null && spaceTree.getRoot() != null) {
			spaceTree.setFocus(spaceTree.getFocus());
		}

		PTreeNodeFactory factory = null;

		// node style
		// SingleLineTextViewNode, MultilineTextViewNode, CloseableTextViewNode, MultilineTextWithAttachments
		// use one of the above values P* in return
		final PSpaceTree tree = spaceTree;
		factory = new PTreeNodeFactory() {
			public PTreeNode createTreeNodeFor(String uri, String l, String q, Attributes a,PSpaceTree s) {
				TextViewNode node =  new TextViewNode(uri,l, q, a, tree);
				node.setTreePanel(TreePanel.this);
				node.setColorMode(colorMode);
				return node;
			}
		};
		spaceTree.setTreeNodeFactory(factory);
	}
	
	
	/**
	 * create buffer preview
	 * @return
	 */
	public JPanel getClipboardPanel(){
		if(preview == null){
			preview = createTreeContainer(); 
			preview.panel = new JPanel();
			preview.panel.setLayout(new BorderLayout());
			preview.panel.setPreferredSize(new Dimension(300,200));
			preview.panel.add(preview.canvas,BorderLayout.CENTER);
		}
		return preview.panel;
	}
	
	public void dispose(){
		// register with change listener
	}
	
	
	/**
	 * if there are parents that should be included in selection by default
	 * this method is a way to set it up
	 * @param args
	 */
	public void setIncludeParentFilter(String [] args){
		exceptions = args;
	}
	
	
	/**
	 * set color mode/ color concepts if they are 
	 * not defined
	 * @param b
	 */
	public void setColorMode(int b){
		colorMode = b;
	}
	
	/**
	 * is this tree editable
	 * @param b
	 */
	public void setEditable(boolean b){
		editable = b;
	}
	
	/**
	 * is this tree editable
	 * @return
	 */
	public boolean isEditable(){
		return editable;
	}
	
	
	private boolean isEditable(PSpaceTree t){
		return (tree.spaceTree.equals(t))?isEditable():false;
	}
	
	private boolean isSelectable(PSpaceTree t){
		return tree.spaceTree.equals(t);
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		if(busy){
			remove(status);
			add(progress,BorderLayout.SOUTH);
		}else{
			remove(progress);
			add(status,BorderLayout.SOUTH);
		}
		revalidate();
		repaint();
	}
	
	/**
	 * handle events in menu
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		TextViewNode node = (TextViewNode) getSelectedNode();
		if(node == null)
			return;
	}
	
	
	// aux methods to send events to ProtocolReader
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	// aux methods to send events to ProtocolReader
	public void firePropertyChange(String name, Object oldValue, Object newValue) {
		if (pcs != null)
			pcs.firePropertyChange(name, oldValue, newValue);
	}

	/**
	 * Add TreeSelectionListener
	 */
	public void addTreeSelectionListener(TreeSelectionListener listener) {
		treeSelectionListeners.add(listener);
	}

	/**
	 * Add TreeExpansionListener
	 */
	public void addTreeExpansionListener(TreeExpansionListener listener) {
		treeExpansionListeners.add(listener);
	}

	/**
	 * Notify all registered listeners that tree event has bee fired
	 */
	protected void fireTreeSelectionEvent(PTreeNode node, boolean selected) {
		TreePath path = getSelectedPath(node);
		TreeSelectionEvent event = new TreeSelectionEvent(tree.spaceTree, path,selected,(selected)?null:path,(selected)?path:null);
		for (Iterator i = treeSelectionListeners.iterator(); i.hasNext();) {
			TreeSelectionListener listener = (TreeSelectionListener) i.next();
			listener.valueChanged(event);
		}
	}

	/**
	 * Notify all registered listeners that tree event has bee fired
	 */
	protected void fireTreeExpansionEvent(PTreeNode node, boolean isExpanded) {
		TreePath path = getSelectedPath(node);
		TreeExpansionEvent event = new TreeExpansionEvent(tree.spaceTree, path);
		for (Iterator i = treeExpansionListeners.iterator(); i.hasNext();) {
			TreeExpansionListener listener = (TreeExpansionListener) i.next();
			if (isExpanded)
				listener.treeExpanded(event);
			else
				listener.treeCollapsed(event);

		}
	}

	/**
	 * get tree selectio source string
	 * @param src
	 * @return
	 */
	public static String sourceToString(Object src){
		if(src instanceof PSpaceTree){
			return ""+((PSpaceTree)src).getRootTreeNode();
		}
		return ""+src;
	}
	
	
	/**
	 * Get currently selected TreeNode
	 */
	public TextViewNode getSelectedNode() {
		return (TextViewNode)tree.eventHandler.getSelectedNode();
	}

	private List<TextViewNode> getSelectedNodes(){
		return (selectedNodes != null && !selectedNodes.isEmpty())?
				selectedNodes:Collections.singletonList(getSelectedNode());
	}
	
	public void clearSelectedNodes(){
		if(selectedNodes != null){
			for(PTreeNode node: selectedNodes){
				((TextViewNode)node).setSelected(false);
			}
			selectedNodes.clear();
		}
		tree.eventHandler.clearSelection();
		unhighlightNodes();
	}
	
	/**
	 * Return a TreePath for currently selected TreeNode
	 */
	public TreePath getSelectedPath() {
		return getSelectedPath(getSelectedNode());
	}

	/**
	 * Return a TreePath for currently selected TreeNode
	 *
	public TreePath[] getSelectedPaths() {
		// add currently selected node last
		//selection.add(getSelectedNode());
		
		 // make sure parents are first
		 ArrayList list = new ArrayList(selection);
		 Collections.sort(list, new Comparator(){
		 public int compare(Object o1, Object o2){
		 if(o1 instanceof PTreeNode && o2 instanceof PTreeNode){
		 PTreeNode n1 = (PTreeNode) o1;
		 PTreeNode n2 = (PTreeNode) o2;
		 return (isParentOf(n1,n2))?1:-1;
		 }
		 return 0;
		 }
		 });
		 
		 // build several paths
		 TreePath [] path = new TreePath[list.size()];
		 // reverse path order, so that last selected 
		 // would be first
		 int j=path.length-1;
		 for(Iterator i=list.iterator();i.hasNext();j--)
		 path[j] = getSelectedPath((PTreeNode) i.next());
		 return path;
		return null;
	}
	*/

	/**
	 * Return a TreePath for currently selected TreeNode
	 */
	public TreePath getSelectedPath(PTreeNode selectedNode) {
		if (selectedNode == null)
			return null;

		Vector stack = new Vector();
		for (PTreeNode node = selectedNode; node != null; node = node.getTreeNodeParent()) {
			stack.insertElementAt(node, 0);
		}
		if (stack.size() > 0)
			return new TreePath(stack.toArray());
		else
			return null;
	}

	public JToolBar getToolBar(){
		if(toolBar == null)
			toolBar = createToolBar();
		return toolBar;
	}
	
	
	/**
	 * create toolbar for 
	 * @return
	 */
	private JToolBar createToolBar() {
		JToolBar result = new JToolBar();
		result.setFloatable(false);
		result.add(new JLabel("Search: "));
		result.addSeparator();
		searchField = new JTextField();
		searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) searchField.getPreferredSize().getHeight()));
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSearch();
			}
		});
		searchField.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				if (searchFieldJustGotFocus) {
					searchFieldJustGotFocus = false;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							searchField.selectAll();
						}
					});
				}
			}
		});
		searchField.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				searchFieldJustGotFocus = true;
			}

			public void focusLost(FocusEvent e) {
				searchFieldJustGotFocus = false;
			}
		});

		searchField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			public void insertUpdate(DocumentEvent e) {
				updateSearchResults();
			}

			public void removeUpdate(DocumentEvent e) {
				updateSearchResults();
			}
		});
		result.add(searchField);
		result.addSeparator();

		goButton = new JButton("Go");
		goButton.setPreferredSize(new Dimension(64, 32));
		goActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSearch();
			}
		};
		goButton.addActionListener(goActionListener);
		result.add(goButton);
		goButton.setEnabled(false);

		resetButton = new JButton("Reset");
		resetButton.setPreferredSize(new Dimension(64, 32));
		resetActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		};
		resetButton.addActionListener(resetActionListener);
		result.add(resetButton);
		resetButton.setEnabled(false);

		JComponent spacer = new JLabel();
		spacer.setMinimumSize(new Dimension(3, 0));
		spacer.setPreferredSize(new Dimension(3, 0));
		result.add(spacer);

		//logo = new JLabel(new ImageIcon(getURLForResource("hcillogo.gif")));
		//result.add(logo);

		return result;
	}

	/**
	 * don't know what it does
	 * @return
	 */
	private Object createQueryObject() {
		return searchField.getText().toLowerCase().replaceAll("[\\W]","_");
	}

	/**
	 * set background color for tree
	 */
	public void setBackground(Color c) {
		if (tree != null && tree.canvas != null)
			tree.canvas.setBackground(c);
	}

	/**
	 * update search results
	 */
	private void updateSearchResults() {
		if (tree.spaceTree.getRootTreeNode() == null)
			return;

		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		if (queue.peekEvent(KeyEvent.KEY_PRESSED) == null || searchField.getText().length() == 0) {
			Object query = createQueryObject();
			searchResults = tree.spaceTree.getRootTreeNode().updateMatchesInDatabaseSubtree(query, new ArrayList());
			goButton.setEnabled(searchResults.size() > 0);
			tree.spaceTree.invalidatePaint();

			// I want to go become regular when there is no text in field
			if (searchResults.size() == 0)
				goButton.setBackground(resetButton.getBackground());

			// notify that search is in progress
			firePropertyChange("SEARCH", null, query);
		}
	}

	/**
	 * perform search
	 */
	public void doSearch() {
		if(searchResults == null)
			return;
		
		if (searchResults.size() == 0) {
			Toolkit.getDefaultToolkit().beep();
			goButton.setBackground(PTreeNode.BRIGHT_SEARCH_RESULTS_COLOR);
			RepaintManager.currentManager(goButton).paintDirtyRegions();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
			goButton.setBackground(resetButton.getBackground());
			return;
		}

		if (searchResults.size() > SEARCH_RESULTS_WARNING_LIMIT) {
			String question = "Showing this many search results may cause a delay.\nDo you wish to continue?";
			int result = JOptionPane.showConfirmDialog(this, question, searchResults.size() + " Search Results",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (result != JOptionPane.NO_OPTION) {
				return;
			}
		}

		forceShowSearchResults(true); // don't like behaviour was true
		tree.canvas.requestFocus();

		// notify that search is in progress
		firePropertyChange("SEARCH_GO", null, createQueryObject());
	}

	public void reset(){
		highlightedNodes.clear();
		searchField.setText("");
		forceShowSearchResults(false);
		tree.canvas.requestFocus();
	}
	
	
	/**
	 * display search results
	 * @param forceShow
	 */
	private void forceShowSearchResults(boolean forceShow) {
		if (tree.spaceTree.getRootTreeNode() == null)
			return;

		if (forceShow) {
			goButton.setBackground(PTreeNode.BRIGHT_SEARCH_RESULTS_COLOR);
			tree.spaceTree.setSearchResultsNodes(searchResults);
			tree.spaceTree.setFocus(null);
			resetButton.setEnabled(true);
		} else {
			goButton.setBackground(resetButton.getBackground());
			tree.spaceTree.getSearchResultsNodes().clear();

			PTreeNode f = tree.spaceTree.getFocus();
			if (f != null) {
				tree.spaceTree.setFocus(f);
			} else {
				tree.spaceTree.setFocus(tree.spaceTree.getRootTreeNode());
			}
			resetButton.setEnabled(false);
		}
	}

	/**
	 * get space tree instance
	 * @return
	 */
	public PSpaceTree getSpaceTree() {
		return tree.spaceTree;
	}

	public PCanvas getCanvas(){
		return tree.canvas;
	}
	
	
	/**
	 * set root to this tree node
	 */
	public void setRoot(TreeNode node) {
		setRoot(tree,UIHelper.toInputStream(treeNodeToString(node)));
	}
	
	/**
	 * set root to this tree node
	 */
	public void setRoots(TreeNode [] node) {
		setRoot(tree,UIHelper.toInputStream(treeNodeToString(node)));
	}
	
	/**
	 * clear this space tree
	 */
	public void clear(){
		tree.spaceTree.removeAllChildren();
		tree.canvas.getRoot().removeChild(tree.spaceTree);
		tree.canvas.getCamera().removeLayer(tree.spaceTree);
		tree.canvas.revalidate();
		tree.canvas.repaint();
	}
	
	
	/**
	 * try to find a node that matches given text 
	 * and select it
	 * @param text
	 */
	public void setSelectedNode(String t,boolean b){
		final String text = t;
		final boolean add = b;
		(new Thread(new Runnable(){
			public void run(){
				for(int i=0;tree.spaceTree.getRootTreeNode() == null && i<5;i++){
					try{
						Thread.sleep(100);
					}catch(InterruptedException ex){
						ex.printStackTrace();
					}
				}
				PTreeNode node  = tree.spaceTree.getRootTreeNode().findFirstMatchInSubtree(text.toLowerCase());
				if(node != null && node instanceof TextViewNode){
					node.forcePathToRootToBeVisible(true);
					((TextViewNode) node).setNodeHighlighted(true);
					tree.spaceTree.setFocus(node);
					if(add)
						addSelectedTreeNode(node);
					fireTreeSelectionEvent(node, true);
				}
			}
		})).start();
		
	}
	
	/**
	 * try to find a node that matches given text 
	 * and select it
	 * @param text
	 */
	public void setSelectedNode(String text){
		setSelectedNode(text,true);
	}
	
	/**
	 * set selected node
	 * @param node
	 */
	public void setSelectedNode(PTreeNode node){
		PTreeNode last = getSelectedNode();
		if(last != null && last instanceof TextViewNode){
			((TextViewNode)last).setNodeHighlighted(false);
		}
		node.forcePathToRootToBeVisible(true);
		node.setHighlighted(true);
		node.getSpaceTree().setFocus(node);
		addSelectedTreeNode(node);
	}
	
	public void setSearchText(String text){
		searchField.setText(text);
	}
	
	String getSearchText(){
		return searchField.getText();
	}
	
	
	void requestFocusOnSearch(){
		searchField.requestFocus();
	}
	
	/**
	 * is panel in single selection mode
	 * @param b
	 */
	public void setSingleSelectionMode(boolean b){
		singleSelectionMode = b;
	}
	
	
	/**
	 * convert TreeNode to file stream
	 */
	protected String treeNodeToString(TreeNode node) {
		StringBuffer buffer = new StringBuffer();
		List<TreeNode> list = Collections.list(node.children());
		Collections.sort(list,new Comparator<TreeNode>(){
			public int compare(TreeNode n1, TreeNode n2) {
				return (""+n1).compareTo(""+n2);
			}
		});
		buffer.append("<node service=\"" + list.size() + "\">\n" + node.toString());
		for(TreeNode n: list) {
			buffer.append(treeNodeToString(n));
		}
		buffer.append("</node>");
		return buffer.toString();
	}
	
	/**
	 * convert TreeNode to file stream
	 */
	private String treeNodeToString(TreeNode [] node) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<node service=\"" + node.length + "\">\n"+"CONCEPT");
		for (TreeNode n: node) {
			buffer.append(treeNodeToString(n));
		}
		buffer.append("</node>");
		return buffer.toString();
	}
	
	/**
	 * load from input stream
	 * @param in
	 */
	protected void setRoot(TreeContainer container, InputStream input) {
		// remove old space_treee
		//t.spaceTree.removeFromParent();
		//t.canvas.getCamera().removeLayer(t.spaceTree);
		final TreeContainer t = container;
		final InputStream  in = input;
		Runnable r = new Runnable(){
			public void run(){
				try{
					for(int i=0;i<t.canvas.getCamera().getLayerCount();i++)
						t.canvas.getCamera().removeLayer(i); 
					for(int i=0;i<t.spaceTree.getCameraCount();i++)
						t.spaceTree.removeCamera(i); 
				}catch(Exception ex){}
				
				
				InputStream ios = new BufferedInputStream(in);
				t.spaceTree.read(ios);
				try{
					ios.close();
				}catch(IOException ex){
					ex.printStackTrace();
				}
				
				t.canvas.getRoot().addChild(t.spaceTree);
				t.canvas.getCamera().addLayer(t.spaceTree);
				t.spaceTree.setFocus(t.spaceTree.getRootTreeNode());
				t.spaceTree.setFoldChildrenLayout(true);
				t.spaceTree.setCenterView(true);
				t.canvas.revalidate();
				t.canvas.repaint();

				//doubleClick = false;
				t.eventHandler.clearSelection();
				dirty = false;
			}
		};
		if(!SwingUtilities.isEventDispatchThread()){
			try{
				SwingUtilities.invokeAndWait(r);
			}catch(Exception ex){};
		}else{
			r.run();
		}
	}

	/**
	 * SpaceTree.java
	 * make only roots children visible, unless there is only one child,
	 * then show the entire tree.
	 */
	public void initialize() {
		// clear search string
		this.searchField.setText("");
		setCursor(Cursor.getDefaultCursor());
		
		
		// get root node
		PTreeNode root = tree.spaceTree.getRootTreeNode();
		if(root != null){
			setSelectedBranch(root,false);
			root.forceChildrenToBeVisible();
			try{
				tree.spaceTree.setFocus(root);
			}catch(Exception ex){
				// second attempt
				tree.spaceTree.setFocus(root);
			}
		}
		// clear selection
		//selection.clear();
		//selectionField.setText("");
	}

	/**
	 * select/deselect tree branch
	 * @param node
	 * @param b
	 */
	private void setSelectedBranch(PTreeNode node, boolean b){
		if(node instanceof TextViewNode){
			((TextViewNode)node).setSelected(b);
			for(Object child :node.getTreeNodeDatabaseChildren()){
				setSelectedBranch((PTreeNode)child,b);
			}
		}
	}
	
	/**
	 * add tree node to selection, get its path
	 */
	public TreePath [] addTreeNodes(PTreeNode selected){
		if(selected != null){
			((TextViewNode) selected).setSelected(true);
			if(shouldIncludeParent(selected)){
				PTreeNode parent  = selected.getTreeNodeParent();
				((TextViewNode) parent).setSelected(true);
				return new TreePath [] {getSelectedPath(selected),getSelectedPath(parent)};
			}
			
			// node has been selected
			fireTreeSelectionEvent(selected,true);
			
			return new TreePath [] {getSelectedPath(selected)};
		}
		return null;
	}
	
	/**
	 * add tree node to selection, get its path
	 */
	public TreePath [] addSelectedTreeNodes(){
		return addTreeNodes(getSelectedNode());
	}
	/**
	 * remove tree node from selection
	 */
	public void removeSelectedTreeNodes(TreePath  [] path){
		// get node
		for(int i=0;i<path.length;i++){
			Object [] p = path[i].getPath();
			PTreeNode node = null;
			if(p[p.length-1] instanceof PTreeNode)
				node = (PTreeNode) p[p.length-1];
			else
				node = (PTreeNode) p[p.length-2];
			// unselect
			((TextViewNode) node).setSelected(false);
			
			// node has been selected
			fireTreeSelectionEvent(node,false);
		}
	}
	
	/**
	 * try to find tree dialog
	 * @return
	 */
	private TreeDialog findTreeDialog(Container c){
		if(treeDialog != null)
			return treeDialog;
		if(c == null)
			return null;
		if(c instanceof TreeDialog){
			treeDialog = (TreeDialog) c;
			return treeDialog;
		}
		return findTreeDialog(c.getParent());
	}
	
	/**
	 * add tree node to selection, get its path
	 */
	void addSelectedTreeNode(PTreeNode node){
		// see if we are inside TreeDialog
		TreeDialog d = findTreeDialog(getParent());
		if(d != null){
			TreePath [] paths = addTreeNodes(node);
			if(paths != null)
				for(int i=0;i<paths.length;i++)
					d.addTreePath(paths[i]);
		}else{
			if(selectedNodes == null)
				selectedNodes = new ArrayList<TextViewNode>();
			TextViewNode textNode = (TextViewNode) node;
			// if such node exists, unselect it
			if(selectedNodes.contains(textNode)){
				selectedNodes.remove(textNode);
				textNode.setSelected(false);
			}else{
				selectedNodes.add(textNode);
				textNode.setSelected(true);
			}
		}
	}
	/**
	 * add tree node to selection, get its path
	 */
	void setSelectedTreeNode(PTreeNode node){
		// see if we are inside TreeDialog
		TreeDialog d = findTreeDialog(getParent());
		if(d != null){
			TreePath [] paths = addTreeNodes(node);
			if(paths != null && paths.length > 0){
				d.removeTreePaths();
				d.addTreePath(paths[0]);
			}
		}	
	}
	
	
	
	/**
	 * should parent node be included
	 * @param node
	 * @return
	 */
	private boolean shouldIncludeParent(PTreeNode node) {
		if (node == null)
			return false;
		if(exceptions != null){
			String parent = "" + node.getTreeNodeParent();
			for (int i = 0; i < exceptions.length; i++) {
				if (parent.startsWith(exceptions[i])) {
					return true;
				}
			}
		}
		return false;
	}
		
	
	
	/**
	 * update preview buffer 
	 */
	private void updateClipboard(){
		if(previewDialog == null){
			JPanel panel = getClipboardPanel();
			Frame frame = JOptionPane.getFrameForComponent(this);
			previewDialog = new JDialog(frame,"Clipboard");
			previewDialog.setContentPane(panel);
			previewDialog.pack();
			Point p = frame.getLocationOnScreen();
			previewDialog.setLocation(p.x+15,p.y+getSize().height-15-panel.getPreferredSize().height);
		}
		// show buffer 		
		if(pasteBuffer != null && pasteBuffer.length > 0){
			StringBuffer buf = convergeClipboard(pasteBuffer);
			setRoot(preview,UIHelper.toInputStream(buf));
			previewDialog.setVisible(true);
		}
	}
	/**
	 * display multiple nodes as one treea
	 */
	private StringBuffer convergeClipboard(StringBuffer [] buf){
		if(buf.length == 1)
			return buf[0];
		String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		StringBuffer paste = new StringBuffer(header);
		paste.append("<node>CLIPBOARD");
		for(StringBuffer b: buf){
			String s = b.toString();
			s = s.substring(header.length()).trim();
			paste.append(s);
		}
		paste.append("</node>");
		return paste;
	}
	
	
	/**
	 * save node in paste buffer
	 * @param node
	 */
	private StringBuffer convertNodeToBuffer(TextViewNode node){
		return convertNodeToBuffer(Collections.singletonList(node))[0];
	
	}
	/**
	 * save node in paste buffer
	 * @param node
	 */
	private StringBuffer [] convertNodeToBuffer(List<TextViewNode> nodes){
		StringBuffer [] buf = new StringBuffer [nodes.size()];
		for(int i=0;i<buf.length;i++){
			PTreeNode node = nodes.get(i);
			XMLOutputter out = new XMLOutputter();
			try {
				StringWriter writer = new StringWriter();
				out.output(new Document(node.toXML()),writer);
				writer.close();
				buf[i] = writer.getBuffer();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return buf;
	}
	
	
	/**
	 * get node saved in paste buffer
	 * @return
	 */
	private PTreeNode convertBufferToNode(String buffer){
		PTreeNode pasted = null;
		if(buffer != null && buffer.length() > 0){
			try {
				InputStream in = new ByteArrayInputStream(buffer.getBytes());
				PTreeNodeBuilder builder = new PTreeNodeBuilder(tree.spaceTree,tree.spaceTree.getTreeNodeFactory());
				pasted = builder.read(in);
				in.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return pasted;
	}

	/**
	 * custom one line node
	 * @author tseytlin
	 *
	 */
	public static class TextViewNode extends PSingleLineTextViewNode {
		//private boolean hideChildren = true;
		private final Color INCOMPLETE_PAINT = new Color(255,100,0);
		private boolean isSelected, highlighted,system,norules;
		private final PBounds temp_rect = new PBounds();
		private final Color selectedColor = new Color(230, 255, 230);
		protected String text;
		protected String [] synonyms;
		private AttributesImpl attributes;
		private TreePanel treePanel;
		private int infoHash;
		private int colorMode;
		private String tip;
		
		//private final Color origColor = new Color(0.8F, 0.8F, 1.0F);

		// std constructor
		public TextViewNode(String uri, String localName, String qName, Attributes a,PSpaceTree spaceTree) {
			super(uri, localName, qName, a, spaceTree);
			content = localName;
			this.attributes = (a != null)?new AttributesImpl(a):new AttributesImpl();
		}
		
		/**
		 * get tooltip text
		 * @return
		 */
		public String getToolTipText(){
			return tip;
		}
		
		public void setToolTipText(String t){
			tip = t;
		}
		
		
		public void setColorMode(int b){
			colorMode = b;
		}
		
		public int getInfoHash() {
			return infoHash;
		}

		public void setInfoHash(int infoHash) {
			this.infoHash = infoHash;
		}
		
		public String getName(){
			return content;
		}
		
		
		public void delete() {
			try{
				super.delete();
			}catch(Exception ex){
				System.err.println("WARNING: problem deleting node "+this+" because of "+ex.getMessage());
			}
		}

		public String getCode(){
			return attributes.getValue("code");
		}
		
		public String [] getSynonyms(){
			if(synonyms == null)
				synonyms = new String[0];
			return synonyms;
		}
		
		public void setSynonyms(String [] lbl){
			synonyms = (lbl != null)?lbl:new String [0];
			int i = attributes.getIndex("labels");
			String d = Arrays.toString(synonyms);
			if(i < 0){
				attributes.addAttribute("","","labels","string",d);
			}else
				attributes.setValue(i,d);
		}
		
		
		public void setTreePanel(TreePanel p){
			treePanel = p;
		}
		
		/**
		 * set node as selected
		 * @param s
		 */
		public void setSelected(boolean s) {
			// unset results of search so that selection be visible
			if (isFoundInSearch)
				isFoundInSearch = false;
			isSelected = s;
			isBookmarked = isSelected;
			invalidatePaint();
		}
		
		/**
		 * get content of this node
		 */
		public String toString() {
			return content;
		}

		/**
		 * get content of this node
		 */
		public String getValue() {
			return content;
		}
		
		/**
		 * compute tree node bounds
		 */
		public void computeTreeNodeContentBounds() {
			if(text == null)
				text = UIHelper.getTextFromName(content);
			try {
				double d = getContentInsetPlusBorderWidth();
				Rectangle2D rectangle2d = DEFAULT_FONT
						.getStringBounds(text, (PPaintContext.RENDER_QUALITY_HIGH_FRC));
				yPos = (float) (rectangle2d.getY() - d);
				rectangle2d.setRect(0.0, 0.0, rectangle2d.getWidth() + 2.0 * d, rectangle2d.getHeight() + 2.0 * d);
				setBounds(rectangle2d);
			} catch (RuntimeException ex) {
				ex.printStackTrace();
			}
		}
		
		
		/**
		 * does it match the query
		 */
		public boolean  matches(Object query){
			if(query == null || (""+query).length() == 0)
				return false;
			String mtext = (""+query).toLowerCase();
			
			// search self
			if(content.toLowerCase().contains(mtext)){
				return true;
			}
			
			// search synonyms
			for(String s: getSynonyms()){
				if(s.toLowerCase().contains(mtext)){
					return true;
				}
			}
			return false;
		}
		
		/**
		 * take care of painting
		 */
		protected void paint(PPaintContext context) {
			if(text == null)
				text = UIHelper.getTextFromName(content);
			//ONPATHTOFOCUS_COLOR =  (isSelected)?selectedColor:origColor;
			drawBorderBoxInBounds(getBoundsReference(), context);
			Graphics2D graphics2d = context.getGraphics();
			graphics2d.setPaint(getPaint());
			graphics2d.drawString(text,(float)getContentInsetPlusBorderWidth(),-yPos);
		}
		
		
		/**
		 * take care of border and coloring
		 */
		public void drawBorderBoxInBounds(PBounds pbounds, PPaintContext ppaintcontext) {
			Color color = BORDER_PAINT;
			Color color_50_ = BACKGROUND_PAINT;
			boolean bool = false;
			Graphics2D graphics2d = ppaintcontext.getGraphics();

			// found in search
			if (isFoundInSearch)
				color_50_ = SEARCH_RESULTS_COLOR;

			// picked node
			if (highlighted) {
				color = HIGHLIGHT_PAINT;
				color_50_ = ONPATHTOFOCUS_COLOR;
				//bool = true;
			}

			// selected
			if (isBookmarked) {
				color = BOOKMARK_PAINT;
				//color_50_ = ONPATHTOFOCUS_COLOR;
				color_50_ = selectedColor;
				bool = true;
			}

			// moused over
			if (isHighlighted) {
				color = HIGHLIGHT_PAINT;
				color_50_ = ONPATHTOFOCUS_COLOR;
				//bool = true;
			}
		
			//if (isIncomplete()) {
			///	color = INCOMPLETE_PAINT;
			//}
			
			
			//else if (isOnFocusPathToRoot)
			//	color_50_ = ONPATHTOFOCUS_COLOR;

			if (bool || DRAW_BORDERS) {
				graphics2d.setPaint(color);
				graphics2d.fill(pbounds);
				temp_rect.setRect(pbounds);
				graphics2d.setPaint(color_50_);
				if (!bool)
					graphics2d.fill(temp_rect.inset(BORDER_WIDTH, BORDER_WIDTH));
				else
					graphics2d.fill(temp_rect.inset(BORDER_WIDTH + 1, BORDER_WIDTH + 1));
			} else {
				graphics2d.setPaint(color_50_);
				graphics2d.fill(pbounds);
			}
		}

		/**
		 * @return the highlighted
		 */
		public boolean isNodeHighlighted() {
			return highlighted;
		}

		/**
		 * @param highlighted the highlighted to set
		 */
		public void setNodeHighlighted(boolean highlighted) {
			this.highlighted = highlighted;
			//setHighlighted(highlighted);
			if(highlighted)
				highlightedNodes.add(this);
			else
				highlightedNodes.remove(this);
		}
		
		
		/**
		 * return xml element
		 */
		public Element toXML(){
			Element e = super.toXML();
			if(attributes != null){
				for(int i=0;i<attributes.getLength();i++){
					String qname = attributes.getQName(i);
					e.setAttribute(qname,attributes.getValue(qname));
				}
			}
			return e;
		}
		
		/**
		 * read content
		 */
		public void readContent(String str){
			text = null;
			super.readContent(str);
		}
		
	}

	/**
	 * this class is responsible for tree behaviour
	 * @author tseytlin
	 */
	private class SpaceTreeEventHandler extends PSpaceTreeEventHandler {
		private PSpaceTree spaceTree;
		private PCanvas canvas;
		private PTreeNode pickedTreeNode; //, previousTreeNode;
		
		public SpaceTreeEventHandler(PCanvas c,PSpaceTree aTree) {
			super(aTree);
			setShowTooltips(false);
			canvas = c;
			spaceTree = aTree;
		}

		/**
		 * get selected node
		 * @return
		 */
		public PTreeNode getSelectedNode() {
			return pickedTreeNode;
		}		
		
		/**
		 * clear selection
		 */
		public void clearSelection() {
			if(pickedTreeNode != null)
				((TextViewNode)pickedTreeNode).setSelected(false);
			pickedTreeNode = null; // previousTreeNode = null;
		}

		
		/**
		 * take care of dragging
		 */
		public void mouseDragged(PInputEvent event) {
			if (!isDragging()) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
			super.mouseDragged(event);
		}
		
		public void mouseMoved (PInputEvent pinputevent) {
		    PTreeNode ptreenode = getPickedTreeNode (pinputevent);
		    if(ptreenode != highlightedNode){
		    	if (highlightedNode != null)
		    		highlightedNode.setHighlighted (false);
		
		    	highlightedNode = ptreenode;
		    	if (highlightedNode != null)  {
				   highlightedNode.setHighlighted (true);
			    
		    	}
		    }
		}
		
		public void mouseEntered(PInputEvent e) {
			super.mouseEntered(e);
			
			// get new node
			PTreeNode pickedTreeNode = getPickedTreeNode(e);
			
			if(pickedTreeNode != null && pickedTreeNode instanceof TextViewNode){
				if(((TextViewNode)pickedTreeNode).getToolTipText() != null){
					canvas.setToolTipText(((TextViewNode) pickedTreeNode).getToolTipText());
					canvas.repaint();
					return;
				}
			}
			// reset tooltip
			canvas.setToolTipText(null);
			
		}

		/**
		 * this is where selection happens
		 */
		public void mouseReleased(PInputEvent event) {
			boolean doubleClick = (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2);
			
			
			if ( event.getButton() == MouseEvent.BUTTON3 || event.isPopupTrigger() || doubleClick ||
				(event.getButton() == MouseEvent.BUTTON1 && event.isControlDown())) {

				//	 save previous node
				//previousTreeNode = pickedTreeNode;

				// unselect previous node
				/*
				if (previousTreeNode != null && previousTreeNode instanceof TextViewNode) {
					((TextViewNode) previousTreeNode).setNodeHighlighted(false);

					PTreeNode parent = previousTreeNode.getTreeNodeParent();
					if (parent != null)
						((TextViewNode) parent).setNodeHighlighted(false);
				}*/
				unhighlightNodes();
				
				
				
				// get new node
				pickedTreeNode = getPickedTreeNode(event);

				if (pickedTreeNode != null && pickedTreeNode instanceof TextViewNode) {
					((TextViewNode) pickedTreeNode).setNodeHighlighted(true);
				}

				// highlight parent node when appropriate
				if (shouldIncludeParent(pickedTreeNode)) {
					PTreeNode parent = pickedTreeNode.getTreeNodeParent();
					((TextViewNode) parent).setNodeHighlighted(true);
				}
				
				// if picked node open either edit or add to list
				if(pickedTreeNode != null){
					if(isSelectable(spaceTree)){
						//fireTreeSelectionEvent(pickedTreeNode, true);
						
						if(singleSelectionMode){
							setSelectedTreeNode(getSelectedNode());
						}else{
							addSelectedTreeNode(getSelectedNode());
						}
						
						// on double click doDone
						if(doubleClick){
							TreeDialog d = findTreeDialog(getParent());
							if(d != null)
								d.doOK();
						}
					}
				}
			} else if (event.getButton() == MouseEvent.BUTTON1) {
				event.getInputManager().setKeyboardFocus(this);

				// save previous node
				//previousTreeNode = pickedTreeNode;

				// deselect previous node
				/*
				if (previousTreeNode != null && previousTreeNode instanceof TextViewNode) {
					//if(!selection.contains(previousTreeNode))
					//((SingleLineTextViewNode) previousTreeNode).setSelected(false);
					((TextViewNode) previousTreeNode).setNodeHighlighted(false);

					PTreeNode parent = previousTreeNode.getTreeNodeParent();
					if (parent != null)
						((TextViewNode) parent).setNodeHighlighted(false);
				}*/
				unhighlightNodes();

				// select new node
				pickedTreeNode = getPickedTreeNode(event);
				//System.out.println("-------------------------------");

				if (pickedTreeNode != null) {
					if (pickedTreeNode instanceof TextViewNode) {
						((TextViewNode) pickedTreeNode).setNodeHighlighted(true);
					}

					setNextFocus(pickedTreeNode, MOUSE_FOCUS_DELAY);

					// fire events
					/*
					if (previousTreeNode != null) {
						// hide other sub-tree
						((TextViewNode) previousTreeNode).forceChildrenToBeNotVisible();
						canvas.repaint();
					}*/

					if (pickedTreeNode.getNumberOfTreeNodeDatabaseChildren() > 0)
						fireTreeExpansionEvent(pickedTreeNode, true);

					//fireTreeSelectionEvent(pickedTreeNode, true);

					// highlight parent node when appropriate
					if (shouldIncludeParent(pickedTreeNode)) {
						PTreeNode parent = pickedTreeNode.getTreeNodeParent();
						((TextViewNode) parent).setNodeHighlighted(true);
					}
					
					
					if(singleSelectionMode){
						setSelectedTreeNode(getSelectedNode());
					}
					
				}

			}

			//	display selection
			//displaySelection();

			if (isDragging()) {
				setCursor(Cursor.getDefaultCursor());
				super.mouseReleased(event);

			}
			canvas.repaint();
		}

		/**
		 * take care of scaling
		 */
		public void mouseWheelRotated(PInputEvent pinputevent) {
			if (pinputevent.getWheelRotation() > 0) {
				getSpaceTree().scale(0.9);
				getSpaceTree().repaint();
			} else {
				getSpaceTree().scale(1.1);
				getSpaceTree().repaint();
			}
		}

		public void mousePressed(PInputEvent event) {
			super.mousePressed(event);
			if(event.isPopupTrigger() && getPickedTreeNode(event) != null){
				JPopupMenu menu = getPopupMenu();
				if(menu != null){
					menu.show((PCanvas)event.getComponent(), 
							(int)event.getCanvasPosition().getX(),
							(int)event.getCanvasPosition().getY());
				}
			}
		}
		
		
		
	}
	
	private static void unhighlightNodes(){
		for(TextViewNode node: new ArrayList<TextViewNode>(highlightedNodes)){
			node.setNodeHighlighted(false);
		}
	}
	
	
	public JPopupMenu getPopupMenu() {
		return null;
	}


	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	/**
	 *  tree container
	 * @author tseytlin
	 */
	private static class TreeContainer {
		public PCanvas canvas;
		public PSpaceTree spaceTree;
		public SpaceTreeEventHandler eventHandler;
		public JPanel panel;
	}
	
	
}
