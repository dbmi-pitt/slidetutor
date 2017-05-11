/**
 * This class draws a JLabel for each concept that it represents
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report;


import java.awt.Color;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import java.awt.Cursor;
import java.awt.Font;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import edu.pitt.dbmi.tutor.modules.interfaces.ReportInterface;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.terminology.lexicon.Concept;

//import java.io.Serializable;


public class ConceptLabel  implements Comparable{
	
	//private List values;
	private SimpleAttributeSet attr;
	private String text;
	private int offset; // offset within the document
	private Position pos;
	private transient JPopupMenu conceptMenu;
	private transient JMenuItem glossary,cut,copy,paste;
	private boolean debugStatus;
	private transient ReportInterface reportPanel;
	private Concept concept;
	private boolean deleted;
	
	// this variable controls blinking
	//boolean stopBlinking = true;

	/**
	 * Create JLabel
	 */
	public ConceptLabel(Concept c){
		this(c.getText(),c.getOffset());
		this.concept = c;
	}
	
	/**
	 * create label from text and offset
	 * @param text
	 * @param offset
	 */
	public ConceptLabel(String text, int offset){
		this.text = text;
		this.offset = offset;
		

		// add create attr set
		attr = new SimpleAttributeSet();
		//StyleConstants.setComponent(attr,this);
		attr.addAttribute("concept",Boolean.TRUE);
		StyleConstants.setForeground(attr,Color.blue);
		attr.addAttribute("object",this);
	}
	
	
	/**
	 * create remove concept attribute
	 * @return
	 */
	public static AttributeSet getRemoveConceptAttribute(){
		SimpleAttributeSet a = new SimpleAttributeSet();
		a.removeAttribute("concept");
		StyleConstants.setForeground(a,Color.black);
		a.removeAttribute("object");
		return a;
	}
	
	
	public Concept getConcept() {
		return concept;
	}
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	

	public void setReportPanel(ReportInterface reportPanel) {
		this.reportPanel = reportPanel;
	}

	
	// make sure we stop the thread before we finish
	protected void finalize(){
		//stopBlinking = true;
	}
	
	
	/**
	 * get attribute set
	 */
	public AttributeSet getAttributeSet(){
		return attr;	 
	}
	

	/**
	 * get label text
	 */
	public String getText(){
		return text;
	}
	
	/**
	 * set label text
	 */
	public void setColor(Color c){
		//setForeground((UIHelper.getConceptColorStatus())?c:recognizedColor);
		if(c != null){
			StyleConstants.setForeground(attr,c);
			repaint();
		}
	}
	
	public void repaint(){
		reportPanel.getReportDocument().setCharacterAttributes(getOffset(),getLength(),getAttributeSet(),true);
	}
	
	/**
	 * set label text
	 */
	public void setBackgroundColor(Color c){
		//setForeground((UIHelper.getConceptColorStatus())?c:recognizedColor);
		StyleConstants.setBackground(attr,c);
	}
	
	
	/**
	 * get label text
	 */
	public Color getColor(){
		//return getForeground();
		return StyleConstants.getForeground(attr);
	}
	
	/**
	 * update in document
	 * @param doc
	 */
	public void update(StyledDocument doc){
		// check if this label is deleted, this is when what is in document doesn't match
		if(isDeleted())
			doc.setCharacterAttributes(getOffset(),getLength(),ReportDocument.getNormalTextStyle(),true);
		else
			doc.setCharacterAttributes(getOffset(),getLength(),attr,true);
		
	}
	

	
	/**
	 * get concept label offset
	 */
	public int getOffset(){
		if(pos != null)
			return pos.getOffset(); 
		return offset;
	}
	
	/**
	 * set postiont
	 */
	public void setPosition(Position p){
		pos = p;	 
	}

	
	/**
	 * Call attention to message window
	 *
	public void blink() {
		timer = new javax.swing.Timer(800, new ActionListener() {
			public void actionPerformed( ActionEvent evt ) {
			   timer.stop();
			   setForeground(color);
			}
		});
		setForeground(Color.WHITE);
		timer.start();
	}
	*/
	
	/**
	 * get concept label length
	 */
	public int getLength(){
		if(text == null)
			return 0;
		return text.length(); 
	}
	
	// std string method for debuging
	public String toString(){
		return text+" "+getOffset();	
	}
	// labels are equals if the have the same text and offset
	public boolean equals(Object obj){
		if(obj instanceof ConceptLabel){
			ConceptLabel lbl = (ConceptLabel) obj;
			return text.equals(lbl.getText()) && getOffset() == lbl.getOffset();
		}
		return false;
	}
	
	// std hash function
	public int hashCode(){
		String str = text+getOffset();
		return str.hashCode();
	}

	// create popup menu with list of concepts attached to this label
	private JPopupMenu createConceptMenu(){
		JPopupMenu menu = new JPopupMenu();
	
		glossary = new JMenuItem("Glossary",UIHelper.getIcon(reportPanel,"icon.menu.glossary",16));
		glossary.addActionListener(reportPanel);
		menu.add(glossary);
	
		menu.add(new JSeparator());
		
		cut = new JMenuItem("Cut",UIHelper.getIcon(reportPanel,"icon.menu.cut",16));
		cut.setActionCommand("cut");
		cut.addActionListener(reportPanel);
		menu.add(cut);
		
		copy = new JMenuItem("Copy",UIHelper.getIcon(reportPanel,"icon.menu.copy",16));
		copy.setActionCommand("copy");
		copy.addActionListener(reportPanel);
		menu.add(copy);		
		
		paste = new JMenuItem("Paste",UIHelper.getIcon(reportPanel,"icon.menu.paste",16));
		paste.setActionCommand("paste");
		paste.addActionListener(reportPanel);
		menu.add(paste);
		
		return menu;
	}
	

	
    /*
	private class Blinker extends Thread {
		ConceptLabel lbl;
		public Blinker(ConceptLabel l){
			super(ReportScanner.getLabelGroup(),"Label-"+l.getOffset());
			lbl = l;	
		}
		public void run(){
			while(!lbl.stopBlinking){
				try{
					if(UIHelper.getConceptBlinkStatus()){
						lbl.setForeground(Color.MAGENTA);
						sleep(800);
						lbl.setForeground(lbl.color);
						sleep(800);
					}else{
						sleep(1600);
					}
				}catch(InterruptedException ex){
						ex.printStackTrace();	
				}
			}
		}
	}
	*/
	
    
    public int compareTo(Object o) {
    	if(o instanceof ConceptLabel){
    		ConceptLabel l = (ConceptLabel) o;
    		return getOffset() - l.getOffset();
    	}
    	return 1;
	}
}	

