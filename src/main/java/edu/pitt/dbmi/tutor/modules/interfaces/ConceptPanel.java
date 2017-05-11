package edu.pitt.dbmi.tutor.modules.interfaces;

import static edu.pitt.dbmi.tutor.messages.Constants.TYPE_DIAGNOSIS;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.border.LineBorder;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.LinkConceptEntry;
import edu.pitt.dbmi.tutor.messages.Constants;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;


/**
 * concept panel displays concept nodes
 * @author tseytlin
 */
public class ConceptPanel extends JPanel implements MouseListener, MouseMotionListener, ComponentListener{
	private List<NodeConcept> concepts;
	private BasicStroke borderStroke;
	private boolean horizontal,metaColoring;
	private String titleFindings,titleDiagnoses;
	private final int diagnosisOffset = 200;
	private final int conceptOffset = 20;
	private Font borderFont;
	private NodeConcept draggedConcept,popupConcept;
	private Point delta,orig;
	private Cursor pencilCursor;
	private NodeConcept.LinkConcept link;
	private List<NodeConcept> selectedConcepts;
	private InterfaceModule interfaceModule;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	/**
	 * create new concept panel
	 * @param title
	 */
	public ConceptPanel(InterfaceModule module){
		this.interfaceModule = module;
		
		concepts = new Vector<NodeConcept>();
		selectedConcepts = new ArrayList<NodeConcept>();
		
		setOpaque(false);
		
		titleFindings = Config.getProperty(interfaceModule,"title.findings");
		titleDiagnoses = Config.getProperty(interfaceModule,"title.diagnoses");
		
		horizontal = "horizontal".equalsIgnoreCase(
				Config.getProperty(interfaceModule,"component.orientation"));
		borderStroke = new BasicStroke(2);
		borderFont = getFont().deriveFont(Font.BOLD);
		pencilCursor = UIHelper.getCursor(interfaceModule,"component.cursor.pencil",new Point(9,22));
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
	}
	
	public void reconfigure(){
		titleFindings = Config.getProperty(interfaceModule,"title.findings");
		titleDiagnoses = Config.getProperty(interfaceModule,"title.diagnoses");
		
		horizontal = "horizontal".equalsIgnoreCase(
				Config.getProperty(interfaceModule,"component.orientation"));
		pencilCursor = UIHelper.getCursor(interfaceModule,"component.cursor.pencil",new Point(9,22));
		repaint();
	}
	
	
	public void addPropertyChangeListener(PropertyChangeListener l){
		if(pcs == null)
			pcs = new PropertyChangeSupport(this);
		pcs.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	public boolean isMetaColoring() {
		return metaColoring;
	}

	public void setMetaColoring(boolean metaColoring) {
		this.metaColoring = metaColoring;
	}
	
	/**
	 * get background color
	 */
	public Color getBackground(){
		if(getParent() != null)
			return getParent().getBackground();
		return super.getBackground();
	}
	
	
	/**
	 * get bounds of diagnostic rectangle
	 * @return
	 */
	private Rectangle getDiagnosisBounds(){
		Dimension d = getSize();
		int x = (horizontal)?d.width-diagnosisOffset:0;
		int y = (horizontal)?0:d.height-diagnosisOffset;
		int w = (horizontal)?diagnosisOffset:d.width;
		int h = (horizontal)?d.height:diagnosisOffset;
		return new Rectangle(x,y,w,h);
	}
	
	/**
	 * draw borders
	 * @param g
	 */
	private void drawBorders(Graphics2D g){
		int b = (int) borderStroke.getLineWidth();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		g.setStroke(borderStroke);
		g.setColor(Color.GRAY);
		Dimension d = getSize();
		Rectangle r = getDiagnosisBounds();
		g.setFont(borderFont);
		FontMetrics fm = g.getFontMetrics();
		
		// draw frame 
		g.drawRect(b/2,b/2+fm.getHeight()/2,d.width-b,d.height-b-fm.getHeight()/2);
		g.drawRect(r.x+b/2,r.y+b/2+fm.getHeight()/2,r.width-b,r.height-b-fm.getHeight()/2);
		
		// draw filler
		g.setColor(getBackground());
		g.fillRect(20,b/2,fm.stringWidth(titleFindings)+5,fm.getHeight()+10);
		g.fillRect(r.x+20,r.y+b/2-fm.getHeight()/2,fm.stringWidth(titleDiagnoses)+5,fm.getHeight()+10);
		
		// draw title
		g.setColor(Color.GRAY);
		g.drawString(titleFindings,22,fm.getHeight()-3);
		g.drawString(titleDiagnoses,r.x+22,r.y+fm.getHeight()-3);
	}
	
	/**
	 * add concepts
	 * @param c
	 */
	public void addConcept(NodeConcept c){
		synchronized(concepts){
			concepts.add(c);
		}
		repaint();
	}
	
	/**
	 * find a good place for a concept
	 * @param c
	 */
	public void layoutConcept(NodeConcept nc){
		Rectangle pb = new Rectangle(new Point(0,0),getBounds().getSize());
		Rectangle db = getDiagnosisBounds();
		
				
		// assign default location
		if(nc instanceof NodeConcept.FindingConcept)
			nc.setLocation(new Point(conceptOffset,conceptOffset));
		else if(nc instanceof NodeConcept.DiagnosisConcept)
			nc.setLocation(new Point(conceptOffset,db.y+conceptOffset));
		else if(nc instanceof NodeConcept.HypothesisConcept)
			nc.setLocation(new Point(pb.width-nc.getBounds().width-conceptOffset,conceptOffset));
		
		// now fix the overlap issue
		
		Rectangle r = null;
	    int attempt = 0;
	    do{
			r = getIntersectingRectangle(nc);
			if(r != null){
				int x = nc.getLocation().x;
				int y = r.y + r.height+5;
				
				// start second row if out of bounds
				nc.setLocation(new Point(x,y));
				
				// if we out of bounds, wrap
				if(!checkConceptBounds(nc,false)){
					Rectangle sr = nc.getBounds();
				
					// new coordinates
					x = pb.width-sr.width-conceptOffset;
					y = conceptOffset;
					
					if(nc instanceof NodeConcept.DiagnosisConcept)
						y += db.y;
					else if(nc instanceof NodeConcept.HypothesisConcept)
						x = conceptOffset;
					
					// set location
					nc.setLocation(new Point(x,y));
				}	
			}
			attempt ++;
	    }while(r != null && attempt < concepts.size());
	    
	    repaint();
	}
	
	/**
	 * get intersecting rectangle
	 * @param r
	 * @return
	 */
	private Rectangle getIntersectingRectangle(NodeConcept nc){
		for(NodeConcept gc: concepts){
	    	if(!gc.equals(nc) && gc.getBounds().intersects(nc.getBounds()))
	    		return gc.getBounds();
	    	
	    }
		return null;
	}	
	
	/**
	 * add concepts
	 * @param c
	 */
	public void removeConcept(NodeConcept c){
		synchronized(concepts){
			concepts.remove(c);
		}
		selectedConcepts.remove(c);
		repaint();
	}
	
	/**
	 * sketches support link
	 */
	public void sketchSupportLink(){
		setCursor(pencilCursor);
		link = new NodeConcept.LinkConcept(interfaceModule);
		addConcept(link);
	}
	
	/**
	 * sketches support link
	 */
	public void sketchRefuteLink(){
		setCursor(pencilCursor);
		link = new NodeConcept.LinkConcept(true,interfaceModule);
		addConcept(link);
	}
	
	/**
	 * sketches support link
	 */
	public void stopSketchLink(){
		if(link != null){
			setCursor(Cursor.getDefaultCursor());
			removeConcept(link);
			
			// create link node
			NodeConcept a = getConceptAt(link.getSource());
			NodeConcept b = getConceptAt(link.getDestination());
			
			// if one of concepts is null
			// then there is nothing to bee done
			ConceptEntry l = null;
			if(a != null && b != null){
				// attempt to create concept
				l = (link.isRefuteLink())?
				LinkConceptEntry.createRefuteLinkEntry(
						a.getConceptEntry(), b.getConceptEntry()):
				LinkConceptEntry.createSupportLinkEntry(
						a.getConceptEntry(), b.getConceptEntry());
			}
			// should handle null
			interfaceModule.addConceptEntry(l);
			link = null;
		}else{
			interfaceModule.addConceptEntry(null);
		}
	}
	
	
	/**
	 * reset
	 */
	public void reset(){
		concepts.clear();
		selectedConcepts.clear();
	}
	
	
	/**
	 * this is where painting is done
	 */
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
		// draw borders
		drawBorders(g2);
		
		// draw nodes
		synchronized(concepts){
			for(NodeConcept c: concepts){
				// skip attributes
				if(!(c instanceof NodeConcept.AttributeConcept))
					c.draw(g);
			}		
		}
	}



	/**
	 * This method creates custom tooltip for errors vs definition
	 */
	public JToolTip createToolTip(){
		JToolTip tip = super.createToolTip();
		
		// we want a different background color for errors
		tip.setBackground(Color.yellow);
		tip.setBorder(new LineBorder(Color.black));
		
		return tip;
	}
	
	
	public void mouseClicked(MouseEvent e) {
		if(e.getClickCount() == 2){
			NodeConcept c = getConceptAt(e.getPoint());
			if(c != null ){
				c.showIdentifiedFeature();
			}
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	/**
	 * pressed
	 */
	public void mousePressed(MouseEvent e) {
		popupConcept = null;
		NodeConcept c = getConceptAt(e.getPoint());
		if(c != null){
			if(e.getButton() == MouseEvent.BUTTON1){
				draggedConcept = c;
				orig = c.getLocation();
				delta = UIHelper.difference(e.getPoint(),orig);
				
				// link concepts 
				if(link != null){
					link.setSource(e.getPoint());
					draggedConcept = null;
				}
			// check popup	
			}else if(e.isPopupTrigger()){
				popupConcept = c;
				interfaceModule.getPopupMenu().show(this,e.getX(),e.getY());
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		if(draggedConcept != null){
			// select if was not really a drag :)
			boolean selectionOccured = false;
			if(orig != null && draggedConcept.getLocation().distance(orig) == 0){
				boolean selected = !draggedConcept.isSelected();
				selectionOccured = true;
				if(isMetaColoring()){
					pcs.firePropertyChange(Constants.PROPERTY_NODE_SELECTED,null,draggedConcept);
				}else{
					draggedConcept.setSelected(selected);
					if(selected){
						selectedConcepts.add(draggedConcept);
						pcs.firePropertyChange(Constants.PROPERTY_NODE_SELECTED,null,draggedConcept);
					}else{
						selectedConcepts.remove(draggedConcept);
						pcs.firePropertyChange(Constants.PROPERTY_NODE_DESELECTED,null,draggedConcept);
					}
				}
			}
						
			
			// snap back if out of bounds
			if(checkConceptBounds(draggedConcept,true) && !selectionOccured)
				pcs.firePropertyChange(Constants.PROPERTY_NODE_MOVED,null,draggedConcept);
			draggedConcept = null;
			
			repaint();
		}
	
		// check popup
		if(e.isPopupTrigger() && !interfaceModule.getPopupMenu().isShowing()){
			popupConcept = getConceptAt(e.getPoint());
			if(popupConcept != null)
				interfaceModule.getPopupMenu().show(this,e.getX(),e.getY());
		}
		
		// stop sketch link
		stopSketchLink();
	}
	
	
	/**
	 * get popup concept
	 * @return
	 */
	public List<NodeConcept> getSelectedNodes(){
		if(popupConcept != null){
			return Collections.singletonList(popupConcept);
		}
		return selectedConcepts;
	}
	
	/**
	 * get popup concept
	 * @return
	 */
	public NodeConcept getSelectedNode(){
		if(popupConcept != null){
			return popupConcept;
		}else if(!selectedConcepts.isEmpty())
			return selectedConcepts.get(0);
		return null;
	}
	
	
	public void clearPopupSelection(){
		popupConcept = null;
	}
	

	public void mouseDragged(MouseEvent e) {
		if(draggedConcept != null){
			Point p = e.getPoint();
			p.translate(-delta.x,-delta.y);
			draggedConcept.setLocation(p);
			repaint();
		}
		
		// link concepts 
		if(link != null){
			link.setDestination(e.getPoint());
			repaint();
		}
		
	}

	// change cursors
	public void mouseMoved(MouseEvent e) {
		// don't change cursors if in link mode
		if(link != null)
			return;
		
		
		//reset cursor
		if(!isMetaColoring()){
			NodeConcept c = getConceptAt(e.getPoint());
			if(c != null){
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				setToolTipText(c.getConceptEntry().getErrorText());
			}else{
				setCursor(getParent().getCursor());
				setToolTipText(null);
			}
		}
	}
	
	/**
	 * check concept bounds, jump back if necessary
	 * @param c
	 * @param r
	 */
	private boolean checkConceptBounds(NodeConcept c, boolean createDx){
		// snap back if out of bounds
		boolean b = true;
		Rectangle bounds = new Rectangle(new Point(0,conceptOffset/4),getBounds().getSize());
		Rectangle r = getDiagnosisBounds();
		if(!bounds.contains(c.getBounds())){
			if(orig != null)
				c.setLocation(orig);
			b = false;
		}else if(c instanceof NodeConcept.DiagnosisConcept){
			// snap back diagnosis
			if(!r.contains(c.getBounds())){
				if(orig != null)
					c.setLocation(orig);
				b = false;
			}
		}else if(c instanceof NodeConcept.HypothesisConcept){
			// create diagnosis if in diagnosis
			if(r.contains(c.getBounds()) || r.intersects(c.getBounds())){
				// the original location of a Hx
				Point pt = orig;
				
				// create dx
				if(createDx){
					ConceptEntry ce = c.getConceptEntry().clone();
					ce.setType(TYPE_DIAGNOSIS);
					
					// add new concept
					interfaceModule.addConceptEntry(ce);
				
					// set location 
					NodeConcept n = NodeConcept.getNodeConcept(ce,interfaceModule);
					if(n != null){
						n.setLocation(c.getLocation());
					}
				}
				if(pt != null)
					c.setLocation(pt);
				b = false;
			}
		}else if(c instanceof NodeConcept.FindingConcept){
			if(r.contains(c.getBounds()) || r.intersects(c.getBounds())){
				if(orig != null)
					c.setLocation(orig);
				b = false;
			}
		}
		orig = null;
		return b;
	}
	
	
	/**
	 * get concept under cursor
	 * @return
	 */
	private NodeConcept getConceptAt(Point pt){
		if(pt != null){
			// change cursor
			for(NodeConcept c: concepts){
				if(c.getBounds().contains(pt)){
					// check if the concept is within concept
					for(NodeConcept a: c.getAttributes())
						if(a.getBounds().contains(pt))
							return a;
					// else return parent concept
					return c;
				}
			}
		}
		return null;
	}

	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent e) {
		// disable if replacing modules
		if("true".equalsIgnoreCase(Config.getProperty("temp.replacing.module")))
			return;
		
		// mak sure Dx are in their pen
		// change cursor
		Rectangle o = getBounds();
		Rectangle r = getDiagnosisBounds();
		for(NodeConcept c: concepts){
			if(c instanceof NodeConcept.DiagnosisConcept){
				if(!r.contains(c.getBounds())){
					layoutConcept(c);
				}
			}else if(!o.contains(c.getBounds()) || r.contains(c.getBounds())){
				layoutConcept(c);
			}
		}
		repaint();
	}

	public void componentShown(ComponentEvent arg0) {
	}	
}