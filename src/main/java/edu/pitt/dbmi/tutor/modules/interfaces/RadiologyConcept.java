package edu.pitt.dbmi.tutor.modules.interfaces;

import static edu.pitt.dbmi.tutor.messages.Constants.*;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.model.InterfaceModule;
import edu.pitt.dbmi.tutor.model.PresentationModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.MessageUtils;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.markers.Annotation;


public class RadiologyConcept extends DefaultMutableTreeNode {
	private InterfaceModule interfaceModule;
	private List<RadiologyConcept> attributes;
	private ConceptEntry conceptEntry;
	private boolean selected;
	private boolean notInferred;
	
	// misc settings
	private Color backgroundColor,errorColor,unknownColor,irrelevantColor,inferredColor;
	private Color sureColor,unsureColor;
	
	public RadiologyConcept(ConceptEntry e, InterfaceModule i){
		conceptEntry = e;
		interfaceModule = i;
		conceptEntry.addRepresentation(this);
		// add confidence
		if(e.getProperties().containsKey(MESSAGE_INPUT_CONFIDENCE)){
			add(new DefaultMutableTreeNode("confidence: "+e.getProperties().get(MESSAGE_INPUT_CONFIDENCE)));
		}
		
		// some init settings
		backgroundColor = Config.getColorProperty(i,"node.correct.color");
		errorColor = Config.getColorProperty(i,"node.error.color");
		irrelevantColor = Config.getColorProperty(i,"node.irrelevant.color");
		unknownColor = Config.getColorProperty(i,"node.unknown.color");
		sureColor = Config.getColorProperty(i,"node.sure.color");
		unsureColor = Config.getColorProperty(i,"node.unsure.color");
		inferredColor = Config.getColorProperty(i,"node.inferred.color");
	}
	/**
	 * create node concept
	 * @param e
	 * @param i
	 * @return
	 */
	public static RadiologyConcept createRadiologyConcept(ConceptEntry e, InterfaceModule i){
		RadiologyConcept c = new RadiologyConcept(e,i);
		return c;
	}

	public Object getUserObject() {
		return this;
	}

	public ConceptEntry getConceptEntry() {
		return conceptEntry;
	}

	public void setConceptEntry(ConceptEntry conceptEntry) {
		this.conceptEntry = conceptEntry;
	}

	public InterfaceModule getInterfaceModule() {
		return interfaceModule;
	}

	public void setInterfaceModule(InterfaceModule interfaceModule) {
		this.interfaceModule = interfaceModule;
	}

	public List<RadiologyConcept> getAttributes(){
		if(attributes == null){
			attributes = new ArrayList<RadiologyConcept>();
			for(ConceptEntry a: conceptEntry.getAttributes()){
				attributes.add(createRadiologyConcept(a,interfaceModule));
			}
		}
		return attributes;
	}
	
	public String getId(){
		return conceptEntry.getId();
	}
	/**
	 * flash concept to draw attention
	 */
	public void flash(){
		//TODO: flash concept to draw attention
	}
	
	public static RadiologyConcept getRadiologyConcept(ConceptEntry e, InterfaceModule i){
		for(Object o: e.getRepresentations()){
			if(o instanceof RadiologyConcept && ((RadiologyConcept)o).getInterfaceModule().equals(i)){
				return (RadiologyConcept) o;
			}
		}
		return null;
	}
	public String toString(){
		return conceptEntry.getText();
	}
	
	public boolean isRecommendation(){
		return TYPE_RECOMMENDATION.equals(conceptEntry.getType());
	}
	
	public boolean isFinding(){
		return conceptEntry.isFinding();
	}
	
	public boolean isWorksheet(){
		return conceptEntry.isWorksheetFinding();
	}
	
	/**
	 * get background color
	 */
	public Color getBackgroundColor(){
		Color c = unknownColor;
		ConceptEntry entry = conceptEntry;
		
			
		// if unknown color for concept, then try feature
		if(!TYPE_ATTRIBUTE.equals(conceptEntry.getType()) && entry.getConceptStatus() == ConceptEntry.UNKNOWN)
			entry = conceptEntry.getFeature();
		
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
		if(conceptEntry.getInput() != null && conceptEntry.getInput() instanceof Annotation){
			Annotation tm = (Annotation) conceptEntry.getInput();
			if(!tm.isSelected() && !tm.getColor().equals(c)){
				tm.setColor(c);
				if(tm.getViewer() != null)
					tm.getViewer().repaint();
			}
		}
	}
	
	public boolean isMetaColoring() {
		return false;
	}
	
	/**
	 * way to display the concepts
	 * @author tseytlin
	 *
	 */
	public static class CellRenderer  extends DefaultTreeCellRenderer {
		private Icon iconF = UIHelper.getIcon(Config.getProperty("icon.toolbar.cross"),16);
		private Icon iconR = Config.getIconProperty("icon.menu.paste");
		private Icon iconW = Config.getIconProperty("icon.menu.preferences");
		
		/**
	     * get cell render component
	     */
	    public Component getTreeCellRendererComponent(
	                        JTree tree,
	                        Object value,
	                        boolean sel,
	                        boolean expanded,
	                        boolean leaf,
	                        int row,
	                        boolean hasFocus) {
	        super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
	        if(value instanceof RadiologyConcept){
	        	RadiologyConcept r = (RadiologyConcept) value;
	        	if(r.isWorksheet())
	        		setIcon(iconW);
	        	else if(r.isFinding())
	        		setIcon(iconF);
	        	else if(r.isRecommendation())
	        		setIcon(iconR);
	        	
	        	//setForeground(r.getBackgroundColor().darker());
	        	setText(deriveText(r));
	        }
	        return this;
	    }
	    
	    
	    private String deriveText(RadiologyConcept r){
	    	String str = r.toString();
			for(RadiologyConcept a: r.getAttributes()){
				str = str.replaceAll(a.toString(),"<font color=\""+getHTMLColor(a.getBackgroundColor())+"\">"+a+"</font>");
			}
	    	return  "<html><font color=\""+getHTMLColor(r.getBackgroundColor())+"\">"+str+"</font>";
		}
	    
	    private String getHTMLColor(Color c){
	    	return "#"+toHex(c.getRed())+toHex(c.getGreen())+toHex(c.getBlue());
	    }
	    
	    private String toHex(int x){
	    	// make darker
	    	x = x - 100;
	    	if(x < 0)
	    		x = 0;
	    	// convert to hex
	    	String s = Integer.toHexString(x);
	    	if(s.length()  == 1)
	    		return "0"+s;
	    	return s;
	    }
	}

	public String getErrorText(){
		String s =  conceptEntry.getErrorText();
		// check attributes
		if(s == null){
			for(ConceptEntry a: conceptEntry.getAttributes()){
				String as = a.getErrorText();
				if(as != null){
					s = as;
					break;
				}
			}
		}
		return s;
	}
	
	
	/**
	 * point to identified feature that may belong to this node
	 */
	public void showIdentifiedFeature(){
		setSelected(true);
		// select appropriate input
		final Annotation tm = conceptEntry.getAnnotation();
		if(tm != null){
			if(tm.getViewer() == null && interfaceModule instanceof RadiologyInterface){
				tm.setViewer(((RadiologyInterface)interfaceModule).getViewer());
			}
						
			if(tm.getViewer() != null){
				tm.getViewer().firePropertyChange(AUTO_VIEW_EVENT,null,Boolean.TRUE);
				
				// check image, change if necessary
				if(!tm.getViewer().getImage().equals(tm.getImage())){
					if(interfaceModule instanceof RadiologyInterface){
						((RadiologyInterface)interfaceModule).openImage(tm.getImage());
					}
				}
				
				
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
	 * @return the selected
	 */
	public boolean isSelected() {
		return selected;
	}
	/**
	 * @param selected the selected to set
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
		Annotation a = conceptEntry.getAnnotation();
		if(a != null)
			a.setSelected(selected);
	}
	
}
