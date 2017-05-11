package edu.pitt.dbmi.tutor.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.*;

import edu.pitt.dbmi.tutor.beans.ConceptEntry;


public class EntryWidgetCellRenderer extends JLabel implements ListCellRenderer {
	public static final Color CONCEPT_SELECTION_COLOR = new Color(200,200,255,175);
	private Container parent;
	public EntryWidgetCellRenderer(){
		setOpaque(true);
	}
	
	public EntryWidgetCellRenderer(Container p){
		setOpaque(true);
		parent = p;
	}
	
	public Container getParent(){
		return (parent != null)?parent:super.getParent();
	}
	
	
	/**
	 * setup icon
	 * @param user
	 *
	private void setupIcon(String type){
		if(type.startsWith("Arrow"))
			setIcon(Icons.getIcon(Icons.ARROW,16));
		else if(type.startsWith("Rect") || type.startsWith("Para"))
			setIcon(Icons.getIcon(Icons.RECTANGLE,16));
		else if(type.startsWith("Poly"))
			setIcon(Icons.getIcon(Icons.POLYGON,16));
		else if(type.startsWith("Circle"))
			setIcon(Icons.getIcon(Icons.CIRCLE,16));
		else if(type.startsWith("Ruler"))
			setIcon(Icons.getIcon(Icons.RULER,16));
		else if(type.matches(".+\\.\\w{2,6}"))
			setIcon(Icons.getIcon(Icons.IMAGE,16));
		else
			setIcon(Icons.getIcon(Icons.TAG,16));
	}
	*/
	/**
	 * get list
	 * @param list
	 * @param value
	 * @param index
	 * @param isSelected
	 * @param cellHasFocus
	 * @return
	 */
	public Component getListCellRendererComponent(JList list, Object value, 
			int index, boolean isSelected, boolean cellHasFocus) {
		// set background
		setBackground(isSelected ? CONCEPT_SELECTION_COLOR : Color.white);
		
		// if icon is available
		if(value instanceof Icon){
			setIcon((Icon)value);
		}else if(value instanceof ConceptEntry){
			setIcon(((ConceptEntry)value).getIcon());
		}else{
			String str = ""+value;
			// setup value
			//setupIcon(str);
			// setup shape entry icon
			setText(str);
		}
		return this;
	}
}
