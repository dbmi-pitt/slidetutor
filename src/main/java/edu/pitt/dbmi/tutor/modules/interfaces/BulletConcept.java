package edu.pitt.dbmi.tutor.modules.interfaces;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;

import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.model.InterfaceModule;

import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.markers.Annotation;


public class BulletConcept extends JPanel implements MouseListener, MouseMotionListener, PopupMenuListener{
	protected InterfaceModule interfaceModule;
	private boolean flashing,selected;
	protected boolean flash;
	private ConceptEntry conceptEntry;
	private final String bullet = "\u2022  ";
	private final String LOCATION = "LOCATION";
	private final String IN = "in ";
	private final String NO = "NO ";
	//protected JLabel noLabel, inLabel,conceptLabel;
	protected List<JLabel> conceptLabels;
	private Point mp;
	private boolean inDrag,startDrag,popupOpen;
	private Timer flashTimer;
	private List<BulletConcept> attributes;
	
	// misc settings
	protected boolean hasShadow,metaColoring,notInferred;
	protected Color shadowColor,flashColor,selectColor;
	protected Color textColor,backgroundColor,errorColor,unknownColor,irrelevantColor,correctColor,inferredColor;
	protected Color sureColor,unsureColor, attributeColor;
	protected int shadowOffset;
	protected Stroke selectStroke;
	protected Font textFont;

	
	/**
	 * 
	 * @param e
	 */
	public BulletConcept(ConceptEntry e,InterfaceModule i){
		this.conceptEntry = e;
		this.interfaceModule = i;
		
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
		setBackground(Color.white);
		setOpaque(false);
		
		// create original label
		//conceptLabels = new ArrayList<JLabel>();
		//conceptLabels.add(createLabel(e.getText()));
		
		// rebuild
		rebuild();
		
		// some init settings
		flashColor = Config.getColorProperty(i,"node.flash.color");
		selectColor = Config.getColorProperty(i,"node.select.color");
		selectStroke = new BasicStroke(8);
		
		int fontSize = Config.getIntegerProperty(i,"node.font.size");
		textFont = i.getComponent().getFont().deriveFont(Font.BOLD,fontSize);
		textColor = Config.getColorProperty(i,"node.font.color");
		errorColor = Config.getColorProperty(i,"node.error.color");
		
		correctColor = Config.getColorProperty(i,"node.correct.color");
		irrelevantColor = Config.getColorProperty(i,"node.irrelevant.color");
		unknownColor = Config.getColorProperty(i,"node.unknown.color");
		sureColor = Config.getColorProperty(i,"node.sure.color");
		unsureColor = Config.getColorProperty(i,"node.unsure.color");
		attributeColor = Config.getColorProperty(i,"node.attribute.color");
		inferredColor = Config.getColorProperty(i,"node.inferred.color");
		
		// add behavior modifiers
		//behaviorAttributeFindingMode = "attribute".equals(Config.getProperty(i,"behavior.finding.mode"));
		
		// init timer
		int delay = Config.getIntegerProperty(i,"node.flash.delay");
		
		flashTimer = new Timer(delay,new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				flash = !flash;
				interfaceModule.getComponent().repaint();
			}
		});
		flashTimer.setRepeats(true);
		
		// load representation
		e.addRepresentation(this);
	}
	
	public String toString(){
		return conceptEntry.toString();
	}
	
	/**
	 * create appropriate concept label
	 * @param text
	 * @return
	 */
	private JLabel createLabel(String text){
		JLabel conceptLabel = new UIHelper.Label(text){
			/**
			 * This method creates custom tooltip for errors vs definition
			 */
			public JToolTip createToolTip() {
				JToolTip tip = super.createToolTip();
				// we want a different background color for errors
				if (conceptEntry.hasErrors()) {
					tip.setBackground(Color.yellow);
					tip.setBorder(new LineBorder(Color.black));
				}
				return tip;
			}
		};
		conceptLabel.addMouseListener(this);
		conceptLabel.addMouseMotionListener(this);
		
		// set background color for highlight
		conceptLabel.setBackground(new Color(230,230,255));
		conceptLabel.setOpaque(false);
		return conceptLabel;
	}
	
	
	public String getText(){
		return conceptEntry.getText();
	}
	
	public InterfaceModule getInterfaceModule(){
		return interfaceModule;
	}
	
	
	public String getType(){
		return conceptEntry.getType();
	}
	public ConceptEntry getConceptEntry() {
		return conceptEntry;
	}

	public void setConceptEntry(ConceptEntry conceptEntry) {
		this.conceptEntry = conceptEntry;
	}
	
	public JLabel getConceptLabel(){
		return getConceptLabels().isEmpty()?null:conceptLabels.get(0);
	}
	
	public List<JLabel> getConceptLabels(){
		if(conceptLabels == null || conceptLabels.isEmpty()){
			conceptLabels = new ArrayList<JLabel>();
			conceptLabels.add(createLabel(conceptEntry.getText()));
		}
		return conceptLabels;
	}
	
	
	/**
	 * get attributes associated with this node
	 * @return
	 */
	public List<BulletConcept> getAttributes(){
		if(attributes == null){
			attributes = new ArrayList<BulletConcept>();
			//if(behaviorAttributeFindingMode){
			for(ConceptEntry a : conceptEntry.getAttributes()){
				// make sure those attributes are part of a string
				if(conceptEntry.getText().contains(a.getText()))
					attributes.add(new BulletAttributeConcept(this,a,interfaceModule));
			}
			//}
		}
		return attributes;
	}
	
	
	/**
	 * get background color
	 */
	public Color getBackgroundColor(){
		Color c = unknownColor;
		ConceptEntry entry = conceptEntry;
		
		// if unknown color for concept, then try feature
		//if(entry.getConceptStatus() == ConceptEntry.UNKNOWN)
		//	entry = conceptEntry.getFeature();
		// if unknown color for concept, then try feature
		if(!TYPE_ATTRIBUTE.equals(entry.getType()) && entry.getConceptStatus() == ConceptEntry.UNKNOWN)
			entry = entry.getFeature();
		
		
		if(metaColoring){
			if(entry.getConceptFOK() == ConceptEntry.CORRECT)
				c  = sureColor;
			else if(entry.getConceptFOK() == ConceptEntry.UNSURE)
				c  = unsureColor;
			else if(entry.getConceptFOK() == ConceptEntry.INCORRECT)
				c  = errorColor;
		}else{
			if(entry.getConceptStatus() == ConceptEntry.CORRECT){
				c  =  correctColor;
				removeErrors();
			}else if(entry.getConceptStatus() == ConceptEntry.INCORRECT)
				c  = errorColor;
			else if(entry.getConceptStatus() == ConceptEntry.IRRELEVANT)
				c  = irrelevantColor;
		}
		
		// if concept was inferred use that color unless inferred color was not set
		if(isInferredColor(entry))
			c = inferredColor;
		
		// set initial color for marker
		setAnnotationColor(c);
		
		return c;
	}
	
	
	/**
	 * should this concept be displayed with inferred color
	 * @param entry
	 * @return
	 */
	private boolean isInferredColor(ConceptEntry entry){
		if(notInferred)
			return false;
		
		// check settings 
		if(!Config.getBooleanProperty(interfaceModule,"behavior.show.inferred.color"))
			return false;
		
		
		// switcht to feature
		if(entry.isFinding())
			entry = entry.getFeature();
		
		// if entry is automatic, but there is a correct concept like that
		// then display its normal color
		if(Config.getBooleanProperty(interfaceModule,"behavior.show.inferred.correct.color")){
			if(entry.isAuto()){
				if(entry.isFinding()){
					for(ConceptEntry e: interfaceModule.getConceptEntries()){
						if(e.isFinding() && !e.isAuto()){
							e = e.getFeature();
							if(e.getName().equals(entry.getName()) && !e.getId().equals(entry.getId())){
								notInferred = true;
								return false;
							}
						}
					}
				}else if(TYPE_ATTRIBUTE.equals(entry.getType())){
					for(ConceptEntry e: interfaceModule.getConceptEntries()){
						if(TYPE_ATTRIBUTE.equals(e.getType()) && e.getName().equals(entry.getName()) && !e.getId().equals(entry.getId()) && 
						   e.getFeature().getName().equals(entry.getFeature().getName()) && !e.isAuto()){
							notInferred = true;
							return false;
						}
					}
				}
				
			}
		}
		
		return entry.isAuto();
	}
	
	/**
	 * set annotation color if approriate
	 * @param c
	 */
	private void setAnnotationColor(Color c){
		if( conceptEntry.getInput() != null && 
			conceptEntry.getInput() instanceof Annotation){
			Annotation tm = (Annotation) conceptEntry.getInput();
			if(!tm.isSelected() && !tm.getColor().equals(c)){
				tm.setColor(c);
				tm.getViewer().repaint();
			}
		}
	}
	
	
	/**
	 * flash a concept
	 */
	public void flash(){
		setFlashing(true);
		(new Thread(new Runnable(){
			public void run(){
				UIHelper.sleep(600);
				setFlashing(false);
			}
		})).start();
	}
	
	/**
	 * flash this concepts
	 */
	public void setFlashing(boolean b){
		flashing = b;
		/*
		if(flashing)
			flashTimer.start();
		else
			flashTimer.stop();
		 */
		// reset to original color
		flash = false;
		interfaceModule.getComponent().repaint();
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		
		// select appropriate input
		Object input = conceptEntry.getInput();
		if(input != null && input instanceof Annotation){
			Annotation tm = (Annotation) input;
			tm.setSelected(selected);
		}
		
		// register selection
		if(selected){
			((BulletInterface)interfaceModule).getSelectedNodes().add(this);
			//pcs.firePropertyChange(Constants.PROPERTY_NODE_SELECTED,null,this);
		}else
			((BulletInterface)interfaceModule).getSelectedNodes().remove(this);
		
		// select attributes
		for(BulletConcept a: getAttributes()){
			if(a.isSelected() != selected)
				a.setSelected(selected);
		}
		
		//repaint
		repaint();
	}
	
	/**
	 * point to identified feature that may belong to this node
	 */
	public void showIdentifiedFeature(){
		setSelected(true);
		// select appropriate input
		Object input = conceptEntry.getInput();
		if(input != null && input instanceof Annotation){
			final Annotation tm = (Annotation) input;
			if(tm.getViewer() != null){
				tm.getViewer().firePropertyChange(AUTO_VIEW_EVENT,null,Boolean.TRUE);
				double scale = Math.max(ViewerHelper.convertLevelZoomToScale(conceptEntry.getPower()),tm.getViewPosition().scale);
				tm.getViewer().setCenterPosition(new ViewPosition(tm.getLocation(), scale));
				
				// notify client event of the action
				ClientEvent ce = conceptEntry.getFeature().getClientEvent(interfaceModule,ACTION_SHOW_LOCATION);
				MessageUtils.getInstance(interfaceModule).flushInterfaceEvents(ce);
				Communicator.getInstance().sendMessage(ce);
				
				// flash annotation
				(new Thread(new Runnable() {
					public void run() {
						boolean b = true;
						for(int i=0;i< 10;i++){
							tm.setSelected(b);
							b ^= true;
							UIHelper.sleep(100);
						}
						tm.setSelected(isSelected());
					}
				})).start();
				
			}
		}
	}

	
	/**
	 * rebuild panel, should be invoked when content of panel
	 * might have changed, like adding or removing attributes
	 */
	public void rebuild(){
		removeAll();
		add(new UIHelper.Label(bullet));
		
		// now lets take care of attributes
		List<BulletConcept> list = getAttributes();
		
		// if no attributes, don't complicate your life
		if(list.isEmpty()){
			add(getConceptLabel());
		}else{
			// sort by offset
			Collections.sort(list,new Comparator<BulletConcept>() {
					public int compare(BulletConcept o1, BulletConcept o2) {
					return getText().indexOf(o1.getText()) - getText().indexOf(o2.getText());
				}
			}); 
			
			// now add all of the attributes
			int offs = 0;
			String text = getText();
			// reset label list
			conceptLabels = new ArrayList<JLabel>();
			for(BulletConcept a: list){
				int i = text.indexOf(a.getText());
				// if offset of attribute equals to current offset
				// then we found a place for the attribute
				// else if offset of attribute is greater then current offset
				// then we have a feature to concider
				if(i == offs){
					// add attribute label
					add(a.getConceptLabel());
					offs += a.getText().length();
				}else if(i > offs){
					// add feature label
					JLabel lbl = createLabel(text.substring(offs,i));
					conceptLabels.add(lbl);
					add(lbl);
					offs += lbl.getText().length();
					
					// add attribute label
					add(a.getConceptLabel());
					offs += a.getText().length();
				}
			}
			
			// finish up
			if(offs < text.length()-1){
				JLabel lbl = createLabel(text.substring(offs));
				conceptLabels.add(lbl);
				add(lbl);
			}
		}
		
		
		add(Box.createHorizontalGlue());
		validate();
		repaint();
			
		
	}
	
	public void addError(String str){
		conceptEntry.addError(str);
		for(JLabel l: getConceptLabels())
			l.setToolTipText(conceptEntry.getErrorText());
	}
	
	public void removeError(String str){
		conceptEntry.removeError(str);
		for(JLabel l: getConceptLabels())
			l.setToolTipText(null);
	}
	public void removeErrors(){
		for(JLabel l: getConceptLabels())
			l.setToolTipText(null);
	}
	
	
	public static BulletConcept getBulletConcept(ConceptEntry e, InterfaceModule i){
		for(Object o: e.getRepresentations()){
			if(o instanceof BulletConcept && ((BulletConcept)o).getInterfaceModule().equals(i)){
				return (BulletConcept) o;
			}
		}
		return null;
	}
	
	/**
	 * make sure that concept and label are synchronized
	 */
	private void sync(){
		// set label color
		for(JLabel conceptLabel: getConceptLabels()){
			conceptLabel.setForeground(getBackgroundColor());
			conceptLabel.setOpaque(isSelected());
		}
		
		// set label colors for attributes
		for(BulletConcept a: getAttributes())
			a.sync();
	}
	
	/**
	 * set foreground color
	 */
	public void setForeground(Color c){
		super.setForeground(c);
		/*
		if(concept == null)
			return;
		if(isAbsentFeature() && panel.getNoLabel() != null)
			getNoLabel().setForeground(c);
		if(isAttributeValue() && panel.getInLabel() != null)
			getInLabel().setForeground(c);		
			*/
	}
	
	/**
	 * extract relevant values
	 * @return
	 */
	public String getConceptText(){
		/*
		String txt = conceptEntry.getText();
		// if concept is attribute value, 
		// we just want the value
		if(concept instanceof AttValueConcept){
			ArrayList list = conceptEntry.getInput();
			txt = (list.size() > 1)?""+list.get(1):"";
		}
		
		// lowercase everything and add a space
		if(txt != null && !(concept instanceof HypoConcept))
			txt = txt.toLowerCase();
		
		// append space afterwords
		if(txt != null)
			txt = txt + " ";
		
		return txt;
		*/
		return null;
	}
	
	
	/**
	 * This method creates custom tooltip for errors vs definition
	 */
	public JToolTip createToolTip() {
		JToolTip tip = super.createToolTip();
		// we want a different background color for errors
		if (conceptEntry.hasErrors()) {
			tip.setBackground(Color.yellow);
			tip.setBorder(new LineBorder(Color.black));
		}
		return tip;
	}

	
	/**
	 * take care of drawing stuff
	 */
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D) g;
		// anti-aliasing on
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
       
        sync();
        super.paintComponent(g);
	}
	
	/**
	 * mouse events
	 */
	public void mouseEntered(MouseEvent e){
		for(JLabel conceptLabel: getConceptLabels()){
			conceptLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			conceptLabel.setOpaque(true);
			conceptLabel.repaint();
		}
	}
	public void mouseExited(MouseEvent e){
		for(JLabel conceptLabel: getConceptLabels()){
			conceptLabel.setCursor(Cursor.getDefaultCursor());
			if(!popupOpen)
				conceptLabel.setOpaque(false);
			conceptLabel.repaint();
		}
	}
	
	public void mouseClicked(MouseEvent e){
		//take viewer to a place where concept was identified
		if (e.getClickCount() > 1) {
			showIdentifiedFeature();
		}
	}
	public void mousePressed(MouseEvent e){
		// make sure that graph manager knows what is going on
		((BulletInterface) interfaceModule).setPopupNode(null);
		
		// remember mouse point
		mp = e.getPoint();
		
		//	 show popup
		if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
			Point p = e.getPoint();
			interfaceModule.getPopupMenu().addPopupMenuListener(this);
			((BulletInterface) interfaceModule).setPopupNode(this);
			interfaceModule.getPopupMenu().show(getConceptLabel(),p.x,p.y);
			//Point pt = SwingUtilities.convertPoint(this,p,interfaceModuel.getPanel());
			//SwingUtilities.convertPointToScreen(p,this);
			//conceptEntry.setLastXY(p.x,p.y);
			
		}
		
		startDrag = true;
		// re-add component
		//((BulletReasoningPanel)interfaceModuel.getPanel()).dragConcept(this,true);
	}
	
	
	//	
	public void mouseReleased(MouseEvent e){
		// check if object was dragged
		
		if(inDrag){
			BulletInterface rp = (BulletInterface)interfaceModule;
			boolean transfer = false;
			// see if we need to create a diagnosis
			if(TYPE_HYPOTHESIS.equals(getType())){
				Point pl = getLocation();
				JPanel dpanel = rp.getDiagnosisPanel();
				pl = SwingUtilities.convertPoint(rp.getComponent(),pl,dpanel);
				Rectangle r = new Rectangle(new Point(0,0),dpanel.getSize());
				if(r.contains(pl)){
					interfaceModule.addConceptEntry(new ConceptEntry(conceptEntry.getName(),TYPE_DIAGNOSIS));
					//getLastConcept().setHierarchical(conceptEntry.isHierarchical());
					transfer = true;
				}
			}
			
			rp.dragConcept(this,false,transfer);
			//	reinsert the concept
			rp.getComponent().validate();
			rp.getComponent().repaint();
			
			inDrag = false;
		}else if(((BulletInterface)interfaceModule).getPopupNode() == null){ 
			// select concept if not in drag :)
			boolean selected = !isSelected();
			setSelected(selected);
		}
		
		// show popup
		/*
		if(e.isPopupTrigger()){
			Point p = e.getPoint();
			interfaceModule.getPopupMenu().addPopupMenuListener(this);
			((BulletInterface) interfaceModule).setPopupNode(this);
			interfaceModule.getPopupMenu().show(this,p.x,p.y);
			//Point pt = SwingUtilities.convertPoint(this,p,interfaceModuel.getPanel());
			//SwingUtilities.convertPointToScreen(p,this);
			//conceptEntry.setLastXY(p.x,p.y);
			
			
		}
		*/
	}
	public void mouseMoved(MouseEvent e){}
	
	public void mouseDragged(MouseEvent e){
		// detect dragging
		if(isDraggingStarted(e.getPoint(),mp)){
			// re-add component
			BulletInterface rp= (BulletInterface) interfaceModule;
			rp.dragConcept(this,true,false);
			rp.getComponent().repaint();
			inDrag = true;
			startDrag = false;
		}
		
		if(inDrag){
			Component b = this;
			Component c = b.getParent();
			Point pt = SwingUtilities.convertPoint(b,e.getPoint(),c);
			pt.x = pt.x - mp.x;
			pt.y = pt.y - mp.y;
			b.setLocation(pt);
		}
	}
	
	/**
	 * popup menu events
	 */
	public void popupMenuCanceled(PopupMenuEvent e){
		setOpaque(false);
		((JPopupMenu) e.getSource()).removePopupMenuListener(this);
		repaint();
		popupOpen = false;
    	ToolTipManager.sharedInstance().setEnabled(true);
	}
	
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e){
    	//setOpaque(false);
    	((JPopupMenu) e.getSource()).removePopupMenuListener(this);
    	popupOpen = false;
    	ToolTipManager.sharedInstance().setEnabled(true);
    }
    
    public void popupMenuWillBecomeVisible(PopupMenuEvent e){
    	setOpaque(true);
    	repaint();
    	popupOpen = true;
    	ToolTipManager.sharedInstance().setEnabled(false);
    }
	
	/**
	 * this should be triggered when dragging motion has been started
	 * @param p1
	 * @param p2
	 * @return
	 */
	private boolean isDraggingStarted(Point p1, Point p2){
		if(!startDrag)
			return false;
		int dx = Math.abs(p1.x - p2.x);
		int dy = Math.abs(p1.y - p2.y);
		return dx > 5 || dy > 5;
	}
	
	
	/**
	 * this class represents an sttribute concept
	 * @author tseytlin
	 *
	 */
	public static class BulletAttributeConcept extends BulletConcept{
		private BulletConcept parentConcept;
		public BulletAttributeConcept(BulletConcept p,ConceptEntry e,InterfaceModule im){
			super(e,im);
			parentConcept = p;
		
			// change font
			//for(JLabel conceptLabel: getConceptLabels())
			//	conceptLabel.setFont(conceptLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));			
		}
		public BulletConcept getParentConcept(){
			return parentConcept;
		}
		
		
		
		public Color getBackgroundColor() {
			Color c = parentConcept.getBackgroundColor();
			return (irrelevantColor.equals(c))?irrelevantColor:super.getBackgroundColor();
		}
		
		/**
		 * don't need to do anything for attributes
		 */
		public void rebuild(){
			//NOOP
		}
		
		/*
		public void setSelected(boolean selected) {
			super.setSelected(selected);
			parentConcept.setSelected(selected);
		}
		*/
	}
}
