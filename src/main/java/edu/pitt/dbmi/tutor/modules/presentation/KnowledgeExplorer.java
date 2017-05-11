package edu.pitt.dbmi.tutor.modules.presentation;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.SlideEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ExpertModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.modules.expert.DomainExpertModule;
import edu.pitt.dbmi.tutor.modules.interfaces.NodeConcept;
import edu.pitt.dbmi.tutor.ui.TreePanel;
import edu.pitt.dbmi.tutor.ui.TreePanel.TextViewNode;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.OntologyHelper;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.ontology.*;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.spacetree.LayoutAlgorithm;
import edu.umd.cs.spacetree.PSpaceTree;
import edu.umd.cs.spacetree.PTreeNode;
import edu.umd.cs.spacetree.nodes.PTreeNodeFactory;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;

public class KnowledgeExplorer implements PresentationModule, ActionListener {
	private Properties defaultConfig;
	private boolean interactive = true;
	private Tutor tutor;
	private CaseEntry caseEntry;
	private JPanel component;
	private ExpertModule expertModule;
	private KnowledgeTree tree;
	private IOntology ontology;
	private DefaultMutableTreeNode root;
	
	

	////////////////////
	public static void main(String [] args) throws Exception {
		ExpertModule expert = new DomainExpertModule();
		expert.openDomain("http://slidetutor.upmc.edu/curriculum/owl/skin/PITT/VesicularDermatitis.owl");
		KnowledgeExplorer explorer = new KnowledgeExplorer();
		
		JDialog d = new JDialog();
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setModal(false);
		d.setResizable(true);
		d.setTitle("KnowledgeExplorer");
		d.getContentPane().add(explorer.getComponent());
		d.pack();
		d.setVisible(true);
		
		explorer.setExpertModule(expert);
		explorer.load();
		
		
		//JOptionPane.showMessageDialog(null,explorer.getComponent(),"",JOptionPane.PLAIN_MESSAGE);
	}
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	public Object getIdentifiedFeature() {
		return null;
	}

	public boolean isFeatureIdentified() {
		return false;
	}

	public void removeIdentifiedFeature(Object obj) {
	}

	public void setCaseEntry(CaseEntry problem) {
		this.caseEntry = problem;
	}

	public void setExpertModule(ExpertModule module) {
		expertModule = module;
		load();
	}

	public void setFeatureMarker(String type) {}
	public void startFeatureIdentification() {}
	public void stopFeatureIdentification() {}
	public void sync(PresentationModule tm) {}

	public Component getComponent() {
		if(component == null){
			component = new JPanel();
			component.setLayout(new BorderLayout());
			
			ToolTipManager.sharedInstance().setEnabled(true);
			ToolTipManager.sharedInstance().setInitialDelay(0);
			ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
			
			
			// init tree
			tree = new KnowledgeTree(this);
			component.add(tree,BorderLayout.CENTER);
		}
		return component;
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public Tutor getTutor() {
		return tutor;
	}

	public boolean isEnabled() {
		return (component == null)?false:component.isEnabled();
	}

	public void reconfigure() {}

	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b){
		if(component != null){
			setInteractive(b);
			component.setEnabled(b);
		}
	}


	/**
	 * set component interactive flag
	 * @param b
	 */
	public void setInteractive(boolean b){
		interactive = b;
	}
	
	/**
	 * is interactive
	 * @return
	 */
	public boolean isInteractive(){
		return interactive;
	}

	public void setTutor(Tutor t) {
		tutor = t;
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Provides a graphical representation of the domain space as a decision tree";
	}

	public String getName() {
		return "Knowledge Explorer";
	}

	public Action[] getSupportedActions() {
		return new Action [] {};
	}

	public Message[] getSupportedMessages() {
		return new Message [] {};
	}

	public String getVersion() {
		return "1.0";
	}

	

	public void receiveMessage(Message msg) {
		// TODO Auto-generated method stub

	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public void resolveAction(Action action) {
		// TODO Auto-generated method stub

	}

	/**
	 * load tree into it
	 */
	public void load() {
		// don't load again
		if(ontology != null && ontology.equals(expertModule.getDomainOntology()))
			return;
		
		// get ontology, quit if there is none
		ontology = expertModule.getDomainOntology();
		if(ontology == null)
			return;
			
		// get all of the rules into one list
		List<DiagnosisPath> diagnoses = new ArrayList<DiagnosisPath>();
		for(IClass dx: ontology.getClass(DIAGNOSES).getSubClasses()){
			ILogicExpression exp = dx.getEquivalentRestrictions();
			if(!exp.isEmpty()){
				diagnoses.add(new DiagnosisPath(dx));
			}
		}
		
		// build frequence counts for findings in diagnosis
		final Map<String,Integer> findingFrequencies = new HashMap<String,Integer>();
		for(DiagnosisPath dpath: diagnoses){
			for(String finding: dpath.getFindingSet()){
				if(!findingFrequencies.containsKey(finding)){
					findingFrequencies.put(finding,1);
				}else{
					findingFrequencies.put(finding,findingFrequencies.get(finding)+1);
				}
			}
		}
		
		
		// sort paths using power levels and frequenceis
		for(DiagnosisPath dpath: diagnoses){
			for(List<String> path: dpath.getFindingPaths()){
				Collections.sort(path,new Comparator<String>(){
					public int compare(String o1, String o2) {
						String p1 = getPowerLevel(o1);
						String p2 = getPowerLevel(o2);
							
						// handle power levels
						if(p1.length() != 0 && p2.length() == 0){
							return -1;
						}else if(p1.length() == 0 && p2.length() != 0){
							return 1;
						}else if(p1.length() == 0 && p2.length() == 0){
							// continue with comparison
						}else if(p1.equals(p2)){
							// continue with comparison
						}else if(TextHelper.isSmallerPower(p1,p2)){
							return -1;
						}else{
							return 1;
						}
						
						// if we are here, check frequencies
						return findingFrequencies.get(o2) - findingFrequencies.get(o1);
					}	
				});
			}
		}
		
		// now lets build a tree
		root = new DefaultMutableTreeNode("KNOWLEDGE");
		for(DiagnosisPath dp : diagnoses){
			// for each path in the diagnostic rule
			for(List<String> path: dp.getFindingPaths()){
				DefaultMutableTreeNode parent = root;
				
				// add features to the path
				path = getFeatureFindingPath(path);
				
				// for each finding in the path
				for(int i=0;i<path.size();i++){
					String finding = path.get(i);
					
					// find relevant parent node that is already in the tree
					DefaultMutableTreeNode p  = findParentNode(root,path.subList(0,i+1));
					if(p != null){
						// if we found a node, then we got our parent
						parent = p;
					}else{
						// didn't find parent node, well
						// now we just need to create it
						// add to the parent as a child unless of course it is 
						p =  new DefaultMutableTreeNode(finding);
						parent.add(p);
						parent = p;
					}
				}
				
				// now add diagnosis as a leaf
				parent.add(new DefaultMutableTreeNode(DIAGNOSES+":"+dp.getName()));
			}
		}
		
		if(tree != null){
			tree.setRoot(root);
		
			// add action listener
			tree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					TreePath path = e.getPath();
					String finding = ""+path.getLastPathComponent();
					System.out.println(finding+" power: "+getPowerLevel(finding)+" freq: "+findingFrequencies.get(finding));
				}
			});
		}
		
	}
	
	/**
	 * find a relevant parent node that matches the list
	 * @param root
	 * @param list
	 * @return
	 */
	private DefaultMutableTreeNode findParentNode(DefaultMutableTreeNode root, List<String> list){
		if(list.isEmpty())
			return root;
		
		// get list head
		String head = list.get(0);
		
		// iterate over children of root
		for(int i=0;i<root.getChildCount();i++){
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
			if(head.endsWith(""+node.getUserObject())){
				return findParentNode(node,list.subList(1,list.size()));
			}
		}
		
		return null;
	}
	
	/**
	 * convert a list of findings into a feature/finding list
	 * @param path
	 * @return
	 */
	private List<String> getFeatureFindingPath(List<String> path){
		List<String> newPath = new ArrayList<String>();
		for(String finding: path){
			String feature = getFeature(finding);
			String prefix = "";
			if(!finding.equals(feature)){
				newPath.add(feature);
				prefix = "*";
			}
			newPath.add(prefix+finding);
		}
		return newPath;
	}
	
	
	/**
	 * get power level of a given finding
	 * @param finding
	 * @return
	 */
	private String getPowerLevel(String finding){
		String [] fn = finding.split("\\:");
		if(fn.length == 2){
			IClass cls = ontology.getClass(fn[1]);
			if(cls != null){
				Object val = cls.getPropertyValue(ontology.getProperty(HAS_POWER));
				if(val != null)
					return ""+val;
				else if(ontology.getClass(ARCHITECTURAL_FEATURES).hasSubClass(cls))
					return POWER_LOW;
				else if(ontology.getClass(CYTOLOGIC_FEATURES).hasSubClass(cls))
					return POWER_HIGH;
			}
		}
		return "";
	}

	/**
	 * get feature of a finding
	 * @param finding
	 * @return
	 */
	private String getFeature(String finding){
		String [] fn = finding.split("\\:");
		if(fn.length == 2){
			IClass cls = ontology.getClass(fn[1]);
			if(cls != null){
				return fn[0]+":"+OntologyHelper.getFeature(cls).getName();
			}
		}
		return null;
	}
	
	
	
	/**
	 * this class represents a collection of paths to a given 
	 * diagnosis
	 * @author tseytlin
	 */
	private class DiagnosisPath{
		private IClass dx;
		private List<List<String>> paths,featurePaths;
		private Set<String> findingSet,featureSet;
		
		/**
		 * initialize new diagnosis path
		 * @param cls
		 */
		public DiagnosisPath(IClass cls){
			this.dx = cls;
		}
		
		
		/**
		 * get varies paths that lead to a given diagnosis
		 * @return
		 */
		public List<List<String>> getFindingPaths(){
			if(paths == null)
				paths = getFindingPatterns(dx.getEquivalentRestrictions());
			return paths;
		}
		
		/**
		 * get varies paths that lead to a given diagnosis
		 * @return
		 */
		public List<List<String>> getFeaturePaths(){
			if(featurePaths == null){
				featurePaths = new ArrayList<List<String>>();
				
				// go over finding paths
				for(List<String> p : getFindingPaths()){
					List<String> fp = new ArrayList<String>();
					for(String f: p){
						String s = getFeature(f);
						if(s != null)
							fp.add(s);
					}
					if(!featurePaths.contains(fp))
						featurePaths.add(fp);
				}
			}
			return featurePaths;
		}
		
		/**
		 * get a set of findings that are found in the rules
		 * @return
		 */
		public Set<String> getFindingSet(){
			if(findingSet == null){
				findingSet = new LinkedHashSet<String>();
				for(List<String> p: getFindingPaths())
					findingSet.addAll(p);
			}
			return findingSet;
		}
		
		
		
		/**
		 * get a set of findings that are found in the rules
		 * @return
		 */
		public Set<String> getFeatureSet(){
			if(featureSet == null){
				featureSet = new LinkedHashSet<String>();
				for(List<String> p: getFeaturePaths())
					featureSet.addAll(p);
			}
			return featureSet;
		}
		
		/**
		 * get name
		 * @return
		 */
		public String getName(){
			return dx.getName();
		}
		
		
		/**
		 * get string
		 */
		public String toString(){
			return getName();
		}
		
		/**
		 * break a single logic expression into finding patterns
		 * @param exp
		 * @return
		 */
		private List<List<String>> getFindingPatterns(ILogicExpression exp){
			List<List<String>> paths = new ArrayList<List<String>>();
			if(exp.getExpressionType() == ILogicExpression.OR){
				for(Object o: exp){
					if(o instanceof ILogicExpression)
						paths.addAll(getFindingPatterns((ILogicExpression)o,null,new ArrayList<List<String>>()));
				}
			}else{
				paths = getFindingPatterns(exp,null, new ArrayList<List<String>>());
			}
			return paths;
		}
		
		
		/**
		 * break a single logic expression into finding patterns
		 * @param exp
		 * @return
		 */
		private List<List<String>> getFindingPatterns(ILogicExpression exp,IProperty prop, List<List<String>> paths){
			// initialize the initial path
			if(paths.isEmpty() && ILogicExpression.OR != exp.getExpressionType()){
				paths.add(new ArrayList<String>());
			}
			
			// iterate over objects
			List<List<String>> newPaths = paths;
			List<List<String>> newPathPaths = new ArrayList<List<String>>();
		
			/// iterate over stuff
			for(Object o: exp){
				// now, if we have an or, things get tricky
				if(ILogicExpression.OR == exp.getExpressionType()){
					newPaths = new ArrayList<List<String>>();
					
					for(List<String> p: paths){
						List<String> newPath = new ArrayList<String>();
						newPath.addAll(p);
						newPaths.add(newPath);
					}
				}
				
				// handle different types of content
				if(o instanceof ILogicExpression){
					getFindingPatterns((ILogicExpression) o,prop, paths);
				}else if(o instanceof IRestriction){
					IRestriction r = (IRestriction) o;
					getFindingPatterns(r.getParameter(),r.getProperty(),paths);
				}else if(o instanceof IClass){
					IClass cls = (IClass) o;
					String type = HAS_FINDING;
					if(prop != null){
						type = prop.getName();
					}
					
					// add to all path
					for(List<String> p: newPaths)
						p.add(type+":"+cls.getName());
					
				}
				
				// now, reconsolidate with paths
				if(ILogicExpression.OR == exp.getExpressionType()){
					newPathPaths.addAll(newPaths);
				}
			}
			
			// now, new paths replace the old
			if(paths != newPaths){
				paths.clear();
				paths.addAll(newPathPaths);
			}
				
			return paths;
		}
	}
	
	
	
	/**
	 * this class wraps tree panel to modify its behavior
	 * @author tseytlin
	 *
	 */
	private static class KnowledgeTree extends TreePanel {
		private JPopupMenu popup;
		private ActionListener listener;
		
		public KnowledgeTree(ActionListener l){
			listener = l;
		}
		
		
		/**
		 * custom factory load
		 */
		protected void loadPreferences(PSpaceTree spaceTree) {
			// setup different layout algo
			spaceTree.setLayoutAlgorithm(new AjustableLayoutAlgorithm());
			
			// load default preferences
			super.loadPreferences(spaceTree);
			
			// reset TreeNodes to be a bit different
			final PSpaceTree tree = spaceTree;
			PTreeNodeFactory factory = new PTreeNodeFactory() {
				public PTreeNode createTreeNodeFor(String uri, String l, String q, Attributes a,PSpaceTree s) {
					String name = a.getValue("name");
					
					// now figure out which node
					boolean findingNode = false;
					if(name.startsWith("*")){
						findingNode = true;
						name = name.substring(1);
					}
					
					if(findingNode){
						return new FindingNode(uri,l, q, a, tree);
					}else if(name.startsWith(DIAGNOSES)){
						return new DiagnosisNode(uri,l, q, a, tree);
					}else{
						return new FeatureNode(uri,l, q, a, tree);
					}
				}
			};
			spaceTree.setTreeNodeFactory(factory);
		}
		
		
		public JPopupMenu getPopupMenu(){
			if(popup == null){
				popup = new JPopupMenu();
				popup.add(UIHelper.createMenuItem("glossary","Lookup Glossary",Config.getIconProperty("icon.menu.glossary"),listener));
				popup.addSeparator();
				popup.add(UIHelper.createMenuItem("show","Show All",Config.getIconProperty("icon.menu.search"),listener));
				
			}
			return popup;
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
			buffer.append("<node name=\""+node.toString()+"\" service=\"" + list.size() + "\">\n" + node.toString());
			for(TreeNode n: list) {
				buffer.append(treeNodeToString(n));
			}
			buffer.append("</node>");
			return buffer.toString();
		}
		
		/**
		 * this nodes represents a feature
		 * @author tseytlin
		 */
		public static class FeatureNode extends TextViewNode {
			protected ConceptEntry entry;
			
			// std constructor
			public FeatureNode(String uri, String localName, String qName, Attributes a, PSpaceTree spaceTree) {
				super(uri, localName, qName, a, spaceTree);
			}
			
			public ConceptEntry getConceptEntry(){
				if(entry == null)
					entry = new ConceptEntry(content,Constants.TYPE_FINDING);
				return entry;
			}
			
			/**
			 * create
			 * @return
			 */
			protected String createName(){
				String prefix = (isAbsent())?"NO ":"";
				return prefix + UIHelper.getTextFromName(content);
			}
			
			public boolean isAbsent(){
				String [] p = content.split("\\:");
				return p[0].contains(HAS_NO_FINDING);
			}
			
			 public float getLevelSeparation() {
				 return spaceTree.getLayoutAlgorithm().getLevelSeparation();
			 }
			
			/**
			 * take care of painting
			 */
			protected void paint(PPaintContext context) {
				if(text == null)
					text = createName();
				super.paint(context);
			}
			
			/**
			 * compute tree node bounds
			 */
			public void computeTreeNodeContentBounds() {
				if(text == null)
					text = createName();
				super.computeTreeNodeContentBounds();
			}
			
			
			
			public void adjustChindrenXPosition() {
				double attrsHeight = 0;
				float realNextXPos = (float) (getNextXPosition() + getBounds().getWidth());
				for (FeatureNode n : getFindingChildren()){
					if (Math.abs(n.getNextXPosition() - realNextXPos) > 2)
						n.setNextPosition(realNextXPos, n.getNextYPosition());
					
				}
			}

			/**
			 * If node has visual external attributes, adjust the heights.
			 * 
			 */
			public void adjustHeight() {
				double attrsHeight = 0;
				
				List<FindingNode> list = getFindingChildren();
				if (list.isEmpty())
					return;

				float realNextXPos = (float) (getNextXPosition() + getBounds().getWidth());
				FeatureNode firstNode = null;
				FeatureNode lastNode = null;
				for (FeatureNode n : list){
					if (Math.abs(n.getNextXPosition() - realNextXPos) > 2)
						n.setNextPosition(realNextXPos, n.getNextYPosition());

					if (firstNode == null)
						firstNode = n;
					lastNode = n;
					
				}
				if (firstNode != null)
					attrsHeight = lastNode.getNextYPosition()-firstNode.getNextYPosition()+lastNode.getBounds().getHeight();

				if (attrsHeight > 0 && attrsHeight != getBounds().getHeight()) {
					setBounds(0, 0, getBounds().getWidth(), attrsHeight);
					setNextPosition(getNextXPosition(), firstNode.getNextYPosition());
				}
			}

			
			/**
			 * These are PTreeNode children from database
			 * @return
			 */
			public List<FindingNode> getFindingChildren() {
				ArrayList<FindingNode> toret = new ArrayList();
				for(Object o: getTreeNodeDatabaseChildren()){
					if(o instanceof FindingNode){
						toret.add((FindingNode)o);
					}
				}
				return toret;
			}
		}
		
		
		/**
		 * this nodes represents a feature
		 * @author tseytlin
		 */
		public static class DiagnosisNode extends FeatureNode {
			// std constructor
			public DiagnosisNode(String uri, String localName, String qName, Attributes a, PSpaceTree spaceTree) {
				super(uri, localName, qName, a, spaceTree);
				content = localName;
			}
		
			public ConceptEntry getConceptEntry(){
				if(entry == null)
					entry = new ConceptEntry(content,Constants.TYPE_DIAGNOSIS);
				return entry;
			}
			
			/**
			 * take care of painting
			 */
			protected void paint(PPaintContext context) {
				if(text == null)
					text = UIHelper.getTextFromName(content);
				
				PBounds r = getBoundsReference();
				
				// draw borders
				drawBorderBoxInBounds(r, context);
				Graphics2D g2 = context.getGraphics();
				g2.setPaint(getPaint());
				Color c = g2.getColor();
				
				// draw oval
				int o = 2;
				//g2.setStroke(new BasicStroke(0.5f));
				g2.setColor(Color.black);
				g2.drawRoundRect((int)r.getX()+o,(int)r.getY()+o,(int)r.getWidth()-2*o,(int) r.getHeight()-2*o, 10, 10);
				g2.setColor(new Color(230,230,240));
				g2.fillRoundRect((int)r.getX()+o,(int)r.getY()+o,(int)r.getWidth()-2*o,(int) r.getHeight()-2*o, 10, 10);
				
				// draw string
				g2.setColor(c);
				g2.drawString(text,(float)getContentInsetPlusBorderWidth(),-yPos);
			}
			
			public List<FindingNode> getFindingChildren(){
				return Collections.EMPTY_LIST;
			}
		}
		
		/**
		 * this nodes represents a feature
		 * @author tseytlin
		 */
		public static  class FindingNode extends FeatureNode {
			public FindingNode(String uri, String localName, String qName, Attributes a, PSpaceTree spaceTree) {
				super(uri, localName, qName, a, spaceTree);
			}
			public float getLevelSeparation() {
				return 0;
			}
			public List<FindingNode> getFindingChildren(){
				return Collections.EMPTY_LIST;
			}
			
			public void computeTreeNodeContentBounds() {
				setBounds(0, 0,20,15);
			}
			
			/**
			 * take care of painting
			 */
			protected void paint(PPaintContext context) {
				if(text == null){
					text = createName();
					setToolTipText(text);
				}
				
				PBounds r = getBoundsReference();
				
				// draw borders
				drawBorderBoxInBounds(r, context);
				Graphics2D g2 = context.getGraphics();
				g2.setPaint(getPaint());
				//Color c = g2.getColor();
				
				// draw oval
				int o = 5;
				g2.drawRect((int)r.getX()+o,(int)r.getY()+o,(int)r.getWidth()-2*o,(int) r.getHeight()-2*o);

			}
		}
		
		/**
		 * @author Olga Medvedeva University of Pittsburgh School of Medicine
		 * @version 1.00 Feb 10, 2004
		 * 
		 */
		private class AjustableLayoutAlgorithm extends LayoutAlgorithm {
			private float topXAdjustment = 0;
			private float topYAdjustment = 0;

			public void secondWalk(PTreeNode aNode, int aLevel, float aXModifierSum, float aYModifierSum) {
				if (aLevel <= getMaxRecursionDepth()) {
					float x = topXAdjustment + aNode.prelim + aXModifierSum;
					float y = topYAdjustment + aYModifierSum;
					float yModifierOrientationAdjustment = 0;
					float nodeLayoutDimensionSize = 0;
					boolean isFlipped = false;

					switch (getRootOrientation()) {
					case NORTH:
					case SOUTH:
						yModifierOrientationAdjustment = fMaxNodeHightAtLevelTable[aLevel];
						nodeLayoutDimensionSize = (float) aNode.getHeight();
						break;

					case EAST:
					case WEST:
						yModifierOrientationAdjustment = getMaxNodeWidthAtLevel(aLevel);
						isFlipped = true;
						nodeLayoutDimensionSize = (float) aNode.getWidth();
						break;
					}

					switch (getNodeJustification()) {
					case TOP_JUSTIFICATION:
						aNode.setNextPosition(x, y);
						break;

					case CENTER_JUSTIFICATION:
						aNode.setNextPosition(x, y + ((yModifierOrientationAdjustment - nodeLayoutDimensionSize) / 2));
						break;

					case BOTTOM_JUSTIFICATION:
						aNode.setNextPosition(x, y + yModifierOrientationAdjustment - nodeLayoutDimensionSize);
						break;
					}

					if (isFlipped) {
						float temp = aNode.getNextXPosition();
						aNode.setNextPosition(aNode.getNextYPosition(), temp);
					}

					switch (getRootOrientation()) {
					case SOUTH:
						aNode.setNextPosition(aNode.getNextXPosition(), -aNode.getNextYPosition()
								- nodeLayoutDimensionSize);
						break;

					case EAST:
						aNode.setNextPosition(-aNode.getNextXPosition() - nodeLayoutDimensionSize, aNode
								.getNextYPosition());
						break;
					}

					if (aNode.getNumberOfTreeNodeChildren() != 0) {
						float levelDelta = getLevelSeparation();
						if (aNode instanceof FeatureNode) {
							levelDelta = ((FeatureNode) aNode).getLevelSeparation();
						}
						float aXMod = aXModifierSum + aNode.modifier;
						float aYMod = aYModifierSum + yModifierOrientationAdjustment + levelDelta;
						secondWalk(aNode.getLeftmostTreeNodeChild(), aLevel + 1, aXMod, aYMod);
					}
					PTreeNode rightSibling = aNode.getRightSibling();

					if(!((FeatureNode) aNode).getFindingChildren().isEmpty()) {
						((FeatureNode) aNode).adjustChindrenXPosition();
					}

					if (aNode instanceof FindingNode && rightSibling == null)
						((FeatureNode)aNode.getTreeNodeParent()).adjustHeight();

					if (rightSibling != null) {
						secondWalk(rightSibling, aLevel, aXModifierSum, aYModifierSum);
					}
				}
			}

		}

	}


	public void setIdentifiedFeature(Shape shape) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * show glossary
	 */
	private void doGlossary(){
		KnowledgeTree.FeatureNode node = (KnowledgeTree.FeatureNode) tree.getSelectedNode();
		if(node != null){
			ConceptEntry e = node.getConceptEntry();
			
			// notify of action
			ClientEvent ce = e.getClientEvent(this,Constants.ACTION_GLOSSARY);
			Communicator.getInstance().sendMessage(ce);
			
			// resolve node
			expertModule.resolveConceptEntry(e);
			
			// show glossary panel
			showGlossaryPanel(node);
		
		}
	}
	
	private void doShowAll(){
		KnowledgeTree.FeatureNode node = (KnowledgeTree.FeatureNode) tree.getSelectedNode();
		if(node != null){
			tree.setSearchText(node.getConceptEntry().getText());
			tree.doSearch();
		}
	}
	
	/**
	 * create tip panel
	 * @return
	 */
	private void showGlossaryPanel(KnowledgeTree.FeatureNode node){
		// setup parameters
		ConceptEntry entry = node.getConceptEntry();
		String name = (entry.isResolved())?entry.getConcept().getName():entry.getText();
		String definition = (entry.isResolved())?entry.getConcept().getDefinition():"no definition available";
		
		// if no descent definition is available, check the feature
		if(TextHelper.isEmpty(definition)){
			entry = entry.getFeature();
			expertModule.resolveConceptEntry(entry);
			name = (entry.isResolved())?entry.getConcept().getName():entry.getText();
			definition = (entry.isResolved())?entry.getConcept().getDefinition():"no definition available";
		}
		
		
		String text = "<table width=300><tr><td><b>"+name+"</b><br>"+definition+"</td></tr></table>";
		
		// adjust text to be well-formatted html with picture
		if(!entry.getExampleImages().isEmpty()){
			URL pic = entry.getExampleImages().get(0);
			// load picture to see ratio
			int w=0,h=0; 
			try{
				ImageIcon img = new ImageIcon(pic);
				w = img.getIconWidth();
				h = img.getIconHeight();
			}catch(Exception ex){}
			
			// adjust width/height to fit into limit
			if(w >= h && w > 300){
				h = (h * 300)/ w;
				w = 300;
			}else if( w < h && h > 200){
				w = (w * 200)/ h;
				h = 200;
			}
			int width = (w>=h)?300:350;	
			
			//reset text
			text = "<table width="+width+"><tr><td valign=top><b>"+name+"</b><br>"+
					definition+"</td>"+((w >= h)?"</tr><tr>":"")+
					"<td><a href=\""+pic+"\"><img src=\""+pic+"\" border=1 width="+w+
					" height="+h+" ></a></td></tr></table>";
		
		}
		final String glossaryText = text;
		
		
		
		// create glossary panel
		final JEditorPane glossaryTextPanel = new UIHelper.HTMLPanel();
		glossaryTextPanel.setText(glossaryText);
		glossaryTextPanel.setEditable(false);
		((JEditorPane) glossaryTextPanel).addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e){
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String str = e.getDescription();
					if(str.equals("reset")){
						glossaryTextPanel.setText(glossaryText);
					}else{
						glossaryTextPanel.setText("<center><a href=\"reset\"><img border=1 src=\""+str+"\"></a></center>");
					}
				}
			}
		});
	
		// create scroll panel
		JScrollPane sc = new JScrollPane(glossaryTextPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(LineBorder.createGrayLineBorder());
		panel.add(sc, "Center");
		JButton ok = new JButton("OK");
	
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		p.add(ok);
		panel.add(p, "South");
		
		
		// setup window
		Point pt = tree.getCanvas().getMousePosition();
		SwingUtilities.convertPointToScreen(pt,tree.getCanvas());
		final Popup glossaryWindow = PopupFactory.getSharedInstance().getPopup(component, panel, pt.x, pt.y);
		glossaryWindow.show();

		// setup close operation
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				glossaryWindow.hide();
			}
		});

		
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("glossary".equals(cmd)){
			doGlossary();
		}else if("show".equals(cmd)){
			doShowAll();
		}
		
	}
	
	
	
}
