package edu.pitt.dbmi.tutor.modules.interfaces;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.Timer;


import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.LinkConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.markers.Annotation;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

public abstract class NodeConcept {
	protected ConceptEntry concept;
	protected InterfaceModule interfaceModule;
	private boolean flashing,selected;
	protected boolean flash;
	private Timer flashTimer;
	protected Rectangle bounds;
	protected Rectangle [] abounds;
	protected AlphaComposite composite;
	protected List<NodeConcept> attributes;
	private boolean behaviorAttributeFindingMode,notInferred;
	
	// misc settings
	protected boolean hasShadow,metaColoring;
	protected Color shadowColor,flashColor,selectColor;
	protected Color textColor,backgroundColor,errorColor,unknownColor,irrelevantColor,inferredColor;
	protected Color sureColor,unsureColor, attributeColor;
	protected int shadowOffset;
	protected Stroke selectStroke;
	protected Font textFont;
	
	
	/**
	 * create node concept
	 * @param e
	 * @param i
	 * @return
	 */
	public static NodeConcept createNodeConcept(ConceptEntry e, InterfaceModule i){
		if(TYPE_FINDING.equals(e.getType()))
			return new FindingConcept(e,i);
		else if(TYPE_ABSENT_FINDING.equals(e.getType()))
			return new AbsentFindingConcept(e,i);
		else if(TYPE_HYPOTHESIS.equals(e.getType()))
			return new HypothesisConcept(e,i);
		else if(TYPE_DIAGNOSIS.equals(e.getType()))
			return new DiagnosisConcept(e,i);
		else if(e instanceof LinkConceptEntry)
			return createLinkConcept((LinkConceptEntry)e,i);
		
		//default is finding (how can we have a concept entyr w/out type?)
		e.setType(TYPE_FINDING);
		return new FindingConcept(e,i);
	}
	
	
	/**
	 * create node concept
	 * @param e
	 * @param i
	 * @return
	 */
	public static NodeConcept createSupportLinkConcept(NodeConcept a, NodeConcept b, InterfaceModule i){
		if(a != null && b != null){
			LinkConceptEntry c = 
			LinkConceptEntry.createSupportLinkEntry(a.getConceptEntry(),b.getConceptEntry());
			
			// if concept was sccussfully created then 
			// create NodeConcept
			if(c != null){
				return new SupportLinkConcept(a,b,c,i);
			}
		}
		return null;
	}
	
	/**
	 * create node concept
	 * @param e
	 * @param i
	 * @return
	 */
	public static NodeConcept createRefuteLinkConcept(NodeConcept a, NodeConcept b, InterfaceModule i){
		if(a != null && b != null){
			LinkConceptEntry c = 
			LinkConceptEntry.createRefuteLinkEntry(a.getConceptEntry(),b.getConceptEntry());
			
			// if concept was sccussfully created then 
			// create NodeConcept
			if(c != null){
				return new RefuteLinkConcept(a,b,c,i);
			}
		}
		return null;
	}
	
	/**
	 * create node concept
	 * @param e
	 * @param i
	 * @return
	 */
	public static NodeConcept createLinkConcept(LinkConceptEntry l, InterfaceModule i){
		NodeConcept a = getNodeConcept(l.getSourceConcept(),i);
		NodeConcept b = getNodeConcept(l.getDestinationConcept(),i);
		if(a != null && b != null){
			if(TYPE_REFUTE_LINK.equals(l.getType()))
				return new RefuteLinkConcept(a, b,l, i);
			else
				return new SupportLinkConcept(a,b,l,i);
		}
		return null;
	}
	
	
	
	
	public static NodeConcept getNodeConcept(ConceptEntry e, InterfaceModule i){
		for(Object o: e.getRepresentations()){
			if(o instanceof NodeConcept && ((NodeConcept)o).getInterfaceModule().equals(i)){
				return (NodeConcept) o;
			}
		}
		return null;
	}

	
	/**
	 * 
	 * @param e
	 */
	public NodeConcept(ConceptEntry e,InterfaceModule i){
		this.concept = e;
		this.interfaceModule = i;
		
		// some init settings
		hasShadow = Config.getBooleanProperty(i,"node.shadow");
		shadowColor = Config.getColorProperty(i,"node.shadow.color");
		shadowOffset = Config.getIntegerProperty(i,"node.shadow.offset");
		flashColor = Config.getColorProperty(i,"node.flash.color");
		selectColor = Config.getColorProperty(i,"node.select.color");
		selectStroke = new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		int fontSize = Config.getIntegerProperty(i,"node.font.size");
		textFont = i.getComponent().getFont().deriveFont(Font.BOLD,fontSize);
		textColor = Config.getColorProperty(i,"node.font.color");
		errorColor = Config.getColorProperty(i,"node.error.color");
		irrelevantColor = Config.getColorProperty(i,"node.irrelevant.color");
		unknownColor = Config.getColorProperty(i,"node.unknown.color");
		sureColor = Config.getColorProperty(i,"node.sure.color");
		unsureColor = Config.getColorProperty(i,"node.unsure.color");
		attributeColor = Config.getColorProperty(i,"node.attribute.color");
		inferredColor = Config.getColorProperty(i,"node.inferred.color");
		
		float alpha = Config.getFloatProperty(i,"component.transparency");
		composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha);
		
		// add behavior modifiers
		behaviorAttributeFindingMode = true;
		//"attribute".equals(Config.getProperty(i,"behavior.finding.mode"));
		
		// init timer
		int delay = Config.getIntegerProperty(i,"node.flash.delay");
		flashTimer = new Timer(delay,new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				// makes sure that this works only during flashing
				if(flashing)
					flash = !flash;
				else
					flash = false;
				interfaceModule.getComponent().repaint();
			}
		});
		flashTimer.setRepeats(true);
		
		// load representation
		e.addRepresentation(this);
	}
	
	public String toString(){
		return concept.toString();
	}
	
	/**
	 * get concept entry
	 * @return
	 */
	public ConceptEntry getConceptEntry(){
		return concept;
	}
	/**
	 * getBounds().getLocation
	 * @return
	 */
	public Point getLocation(){
		return getBounds().getLocation();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Point getCenterPoint(){
		int x = (int) getBounds().getCenterX();
		int y = (int) getBounds().getCenterY();
		return new Point(x,y);
	}
	/**
	 * setBounds().setLocation
	 * @return
	 */
	public void setLocation(Point p){
		getBounds().setLocation(p);
	}
	/**
	 * get bounds of a given concept
	 * @return
	 */
	public Rectangle getBounds() {
		if(bounds == null){
			// figure out the size of text
			FontMetrics fm = interfaceModule.getComponent().getFontMetrics(textFont);
			int height = fm.getHeight()+10;
			int width = fm.stringWidth(concept.getText())+10;
			bounds = new Rectangle(0,0,width,height);
		}
		return bounds;
	}
	
	
	/**
	 * get attributes associated with this node
	 * @return
	 */
	public List<NodeConcept> getAttributes(){
		if(attributes == null){
			attributes = new Vector<NodeConcept>();
			if(behaviorAttributeFindingMode){
				for(ConceptEntry a : concept.getAttributes()){
					// make sure those attributes are part of a string
					if(concept.getText().contains(a.getText()))
						attributes.add(new AttributeConcept(this,a,interfaceModule));
				}
			}
			
		}
		return attributes;
	}
	
	/**
	 * draw concepts
	 * @param g
	 */
	public abstract void draw(Graphics g);

	
	/**
	 * get background color
	 */
	public Color getBackgroundColor(){
		Color c = unknownColor;
		ConceptEntry entry = concept;
		
			
		// if unknown color for concept, then try feature
		if(!TYPE_ATTRIBUTE.equals(concept.getType()) && entry.getConceptStatus() == ConceptEntry.UNKNOWN)
			entry = concept.getFeature();
		
		// pick coloring
		if(isMetaColoring()){
			if(entry.getConceptFOK() == ConceptEntry.SURE)
				c  = sureColor;
			else if(entry.getConceptFOK() == ConceptEntry.UNSURE)
				c  = unsureColor;
			else if(entry.getConceptFOK() == ConceptEntry.ERROR)
				c  = errorColor;
		} else {
			if(entry.getConceptStatus() == ConceptEntry.CORRECT)
				c  =  backgroundColor;
			else if(entry.getConceptStatus() == ConceptEntry.INCORRECT)
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
		if(concept.getInput() != null && concept.getInput() instanceof Annotation){
			Annotation tm = (Annotation) concept.getInput();
			if(!tm.isSelected() && !tm.getColor().equals(c)){
				tm.setColor(c);
				if(tm.getViewer() != null)
					tm.getViewer().repaint();
			}
		}
	}
	
	
	/**
	 * get interface module
	 * @return
	 */
	public InterfaceModule getInterfaceModule() {
		return interfaceModule;
	}
	
	/**
	 * is concept flashing
	 * @return
	 */
	public boolean isFlashing(){
		return flashing;
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
		if(flashing)
			flashTimer.start();
		else
			flashTimer.stop();

		// reset to original color
		flash = false;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				flash = false;
				interfaceModule.getComponent().repaint();
			}
		});
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		
		// select appropriate input
		Object input = concept.getInput();
		if(input != null && input instanceof Annotation){
			Annotation tm = (Annotation) input;
			tm.setSelected(selected);
		}
	}
	
	/**
	 * point to identified feature that may belong to this node
	 */
	public void showIdentifiedFeature(){
		setSelected(true);
		// select appropriate input
		Object input = concept.getInput();
		if(input != null && input instanceof Annotation){
			final Annotation tm = (Annotation) input;
			if(tm.getViewer() != null){
				tm.getViewer().firePropertyChange(AUTO_VIEW_EVENT,null,Boolean.TRUE);
				double scale = Math.max(ViewerHelper.convertLevelZoomToScale(concept.getPower()),tm.getViewPosition().scale);
				tm.getViewer().setCenterPosition(new ViewPosition(tm.getLocation(), scale));
				
				// notify client event of the action
				ClientEvent ce = concept.getFeature().getClientEvent(interfaceModule,ACTION_SHOW_LOCATION);
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
	
	
	public boolean isMetaColoring() {
		return metaColoring;
	}


	public void setMetaColoring(boolean metaColoring) {
		this.metaColoring = metaColoring;
	}

	
	
	/**
	 * Define individual concept classes
	 * @author Eugene Tseytlin
	 *
	 */
	
	// now define all specific classes
	public static class HypothesisConcept extends NodeConcept {
		private int arc = 20;
		private Stroke stroke;
		
		public HypothesisConcept(ConceptEntry e, InterfaceModule m){
			super(e,m);
			backgroundColor = Config.getColorProperty(m,"node.hypothesis.color");
			stroke = new BasicStroke(1);
		}
		
		/**
		 * draw node
		 */
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D) g; 
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setStroke(stroke);
			g2.setComposite(composite);
			
			Rectangle r = getBounds();
		    
			// draw shadow if necessary
			if (hasShadow) {
	            g2.setColor(shadowColor);
	            Rectangle b = new Rectangle(r);
	            b.translate(shadowOffset,shadowOffset);
	            g2.fillRoundRect(b.x, b.y, b.width, b.height, arc,arc);
	        }
			
			//flash if necessary
			if(flash)
				g2.setColor(flashColor);
			else
				g2.setColor(getBackgroundColor());
			
			// draw shape
			 g2.fillRoundRect(r.x,r.y,r.width,r.height,arc,arc);
			
			// select if necessary
			if(isSelected()){
				 g2.setColor(selectColor);
	             g2.setStroke(selectStroke);
			}else
				g2.setColor(Color.black);
		    
			// draw frame
		    g2.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);
		    
		    // draw text
		    g2.setComposite(AlphaComposite.Src);
		    g2.setColor(textColor);
		    g2.setFont(textFont);
			g2.drawString(concept.getText(),r.x+arc/4,r.y+r.height-arc/2+2);
		}

		
	}
	
	public static class DiagnosisConcept extends HypothesisConcept{
		public DiagnosisConcept(ConceptEntry e, InterfaceModule m){
			super(e,m);
			backgroundColor = Config.getColorProperty(m,"node.diagnosis.color");
		}
	}
	
	/**
	 * attribute concept
	 * @author tseytlin
	 *
	 */
	public static class AttributeConcept extends NodeConcept {
		private NodeConcept parent;
		private Rectangle bounds;
		public AttributeConcept(NodeConcept p, ConceptEntry e, InterfaceModule m){
			super(e,m);
			parent = p;
			backgroundColor = Config.getColorProperty(m,"node.attribute.color");
			composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.6f);
		}

		public NodeConcept getParentConcept(){
			return parent;
		}
		
		
		public boolean isMetaColoring() {
			return parent.isMetaColoring();
		}

		/**
		 * get bounds of attribute rectangle, given its offset within parent
		 */
		public Rectangle getBounds() {
			if(bounds == null){
				//System.out.println(parent.getConceptEntry().getText()+" "+concept.getText());
				FontMetrics fm = interfaceModule.getComponent().getFontMetrics(textFont);
				int height = fm.getHeight()+2;
				int offs = parent.getConceptEntry().getText().indexOf(concept.getText());
				int width = fm.stringWidth(concept.getText())+2;
				int offset = 2 + fm.stringWidth(parent.getConceptEntry().getText().substring(0,offs));
				bounds = new Rectangle(offset,5,width,height);
			}
			Point p = parent.getLocation();
			return new Rectangle(p.x+bounds.x,p.y+bounds.y,bounds.width,bounds.height);
		}
	

		public Color getBackgroundColor() {
			Color c = parent.getBackgroundColor();
			boolean irrelevantAttribute = concept.getConceptStatus() == ConceptEntry.CORRECT || parent.getConceptEntry().isAbsent();
			return (irrelevantColor.equals(c) && irrelevantAttribute)?irrelevantColor:super.getBackgroundColor();
		}
		
		
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D) g; 
			Rectangle r = getBounds();
			
			// do background rectangle
			g2.setComposite(composite);
			g2.setColor(getBackgroundColor());
			g2.fillRoundRect(r.x,r.y,r.width,r.height,4,4);
			
			// select if necessary
			Stroke stroke = g2.getStroke();
			if(isSelected()){
				 g2.setColor(selectColor);
	             g2.setStroke(selectStroke);
			}else{
				g2.setColor(Color.black);
			}
			g2.setComposite(parent.composite);
			g2.drawRoundRect(r.x,r.y,r.width,r.height,4,4);
			g2.setStroke(stroke);
			
		}
	}
	
	/**
	 * finding concept
	 * @author Eugene Tseytlin
	 */
	public static class FindingConcept extends NodeConcept {
		private Stroke stroke;
		public FindingConcept(ConceptEntry e, InterfaceModule m){
			super(e,m);
			backgroundColor = Config.getColorProperty(m,"node.finding.color");
			stroke = new BasicStroke(1);
		}
		
		/**
		 * draw finding
		 */
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D) g; 
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setStroke(stroke);
			Rectangle r = getBounds();
			g2.setComposite(composite);
			
			// draw shadow if necessary
			if (hasShadow) {
	            g2.setColor(shadowColor);
	            Rectangle b = new Rectangle(r);
	            b.translate(shadowOffset,shadowOffset);
	            g2.fillRect(b.x, b.y, b.width, b.height);
	        }
			
			//flash if necessary
			if(flash)
				g2.setColor(flashColor);
			else
				g2.setColor(getBackgroundColor());
			
			// draw shape
			g2.fillRect(r.x,r.y,r.width,r.height);
						
			// draw attributes rectangles
			if(attributes != null){
				synchronized(attributes){
					for(NodeConcept c: getAttributes())
						c.draw(g2);
				}
			}
			
			// select if necessary
			if(isSelected()){
				 g2.setColor(selectColor);
	             g2.setStroke(selectStroke);
			}else
				g2.setColor(Color.black);
		    
			// draw frame
		    g2.drawRect(r.x, r.y, r.width, r.height);
		    
		    // draw text
		    g2.setComposite(AlphaComposite.Src);
		    g2.setColor(textColor);
		    g2.setFont(textFont);
			g2.drawString(concept.getText(),r.x+4,r.y+r.height-7);
		}
	}
	
	/**
	 * finding concept
	 * @author Eugene Tseytlin
	 */
	public static class AbsentFindingConcept extends FindingConcept {
		public AbsentFindingConcept(ConceptEntry e, InterfaceModule m){
			super(e,m);
			backgroundColor = Config.getColorProperty(m,"node.absent.finding.color");
		}
	}
	
	/**
	 * generic link concept that connects two points
	 * @author Eugene Tseytlin
	 */
	public static class LinkConcept extends NodeConcept {
		private Point source, destination;
		private Stroke stroke;
		private boolean refute;
		private Rectangle bounds;
		
		/**
		 * create new link concept
		 */
		public LinkConcept(InterfaceModule m){
			this(false,m);
		}
		/**
		 * create new link concept
		 */
		public LinkConcept(boolean refute, InterfaceModule m){
			super(new ConceptEntry("link",(refute)?TYPE_REFUTE_LINK:TYPE_SUPPORT_LINK),m);
			this.refute = refute;
			if(refute){
				stroke = new BasicStroke(2,
				BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,
				10f,new float[]{10f},5f);
	    	}else
	    		stroke = new BasicStroke(2);
			backgroundColor = Config.getColorProperty(m,"node.link.color");
			bounds = new Rectangle(0,0,0,0);
		}
		
		public Rectangle getBounds(){
			return bounds;
		}
		/**
		 * is it refute link?
		 * @return
		 */
		public boolean isRefuteLink(){
			return refute;
		}
		public Point getSource() {
			return source;
		}
		public void setSource(Point source) {
			this.source = source;
		}
		public Point getDestination() {
			return destination;
		}
		public void setDestination(Point destination) {
			this.destination = destination;
		}
		
		/**
		 * draw line
		 */
		public void draw(Graphics g){
			Graphics2D g2 = (Graphics2D)g;
			if(source != null && destination != null){
				g2.setStroke(stroke);
				g2.setColor(getBackgroundColor());
				g2.drawLine(source.x,source.y,destination.x,destination.y);
			}
		}
	}
	
	/**
	 * generic link concept that connects two points
	 * @author Eugene Tseytlin
	 */
	public static class SupportLinkConcept extends NodeConcept {
		private NodeConcept source,destination;
		protected Stroke stroke;
		private Rectangle nobe;
		
		public SupportLinkConcept(NodeConcept a, NodeConcept b, ConceptEntry c, InterfaceModule i){
			super(c,i);
			stroke = new BasicStroke(2);
			nobe = new Rectangle(0,0,10,10);
			source = a;
			destination = b;
			backgroundColor = Config.getColorProperty(i,"node.link.color");
		}
		
		public Rectangle getBounds(){
			return nobe;
		}
		
		public NodeConcept getSource() {
			return source;
		}

		public NodeConcept getDestination() {
			return destination;
		}

		
		
		public void setSource(NodeConcept source) {
			this.source = source;
			// change the concept entry under it
			((LinkConceptEntry)getConceptEntry()).setSourceConcept(source.getConceptEntry());
		}

		public void setDestination(NodeConcept destination) {
			this.destination = destination;
			// change the destination entry under it
			((LinkConceptEntry)getConceptEntry()).setDestinationConcept(destination.getConceptEntry());
		}

		/**
		 * draw line
		 */
		public void draw(Graphics g){
			Graphics2D g2 = (Graphics2D)g;
			Composite alpha = g2.getComposite();
			
		
			// find points
			Point st = findClippingPoint(source.getCenterPoint(),destination.getBounds());
			Point en = findClippingPoint(destination.getCenterPoint(),source.getBounds());
		  
			// select if necessary
			if(isSelected()){
				 g2.setComposite(composite);
				 g2.setColor(selectColor);
	             g2.setStroke(selectStroke);
	             g2.drawLine(st.x,st.y,en.x,en.y);
			}
			
			//flash if necessary
			if(flash)
				g2.setColor(flashColor);
			else
				g2.setColor(getBackgroundColor());
			
			// draw line
			g2.setComposite(AlphaComposite.Src);
			g2.setStroke(stroke);
			g2.drawLine(st.x,st.y, en.x,en.y);
		    adjustRectPos(st, en);
		    g2.fill(nobe); 
		    g2.setComposite(alpha);
		}
		
		
		/**
		 * find right clipping point
		 * @param fromPt
		 * @param r
		 * @return
		 */
	    private Point findClippingPoint(Point fromPt, Rectangle r) {
	        double beta = Math.atan((double) r.height / (double) r.width);
	        Point center = new Point(r.x + r.width / 2, r.y + r.height / 2);
	        double newX = 0.0D;
	        double newY = 0.0D;
	        double xdiff = 0.0D;
	        double ydiff = 0.0D;
	        double theta;
	        if (center.x != fromPt.x)
	            theta = Math.atan(Math.abs(fromPt.y - center.y)
	                    / Math.abs(fromPt.x - center.x));
	        else
	            theta = 1.5707963267948966D;
	        if (theta > beta) {
	            xdiff = ((1.0D / Math.tan(theta)) * (double) r.height) / 2D;
	            if (center.x > fromPt.x)
	                xdiff *= -1D;
	            newX = (double) center.x + xdiff;
	            if (center.y > fromPt.y)
	                newY = r.y;
	            else
	                newY = r.y + r.height;
	        } else {
	            ydiff = (Math.tan(theta) * (double) r.width) / 2D;
	            if (center.y > fromPt.y)
	                ydiff *= -1D;
	            newY = (double) center.y + ydiff;
	            if (center.x > fromPt.x)
	                newX = r.x;
	            else
	                newX = r.x + r.width;
	        }
	        return new Point((int) newX, (int) newY);
	    }

	    /**
	     * adjust position of rectangle
	     * @param p1
	     * @param p2
	     */
	    private void adjustRectPos(Point p1, Point p2) {
	        int x = p1.x;
	        int y = p1.y;
	        int dx = (int) Math.abs(p1.x - p2.x);
	        int dy = (int) Math.abs(p1.y - p2.y);
	        if (p1.x > p2.x)
	            x = p2.x;
	        if (p1.y > p2.y)
	            y = p2.y;
	        x = x + dx / 2;
	        y = y + dy / 2;
	        nobe.setLocation(x - 5, y - 5);
	    }
	}
	
	/**
	 * generic link concept that connects two points
	 * @author Eugene Tseytlin
	 */
	public static class RefuteLinkConcept extends SupportLinkConcept {
		public RefuteLinkConcept(NodeConcept a, NodeConcept b, ConceptEntry c, InterfaceModule i){
			super(a,b,c,i);
			stroke = new BasicStroke(2,
					BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,
					10f,new float[]{10f},5f);
		}
	}
}
