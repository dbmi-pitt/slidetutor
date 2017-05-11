package edu.pitt.dbmi.tutor.ui;

import java.awt.Frame;

/**
 * describes a browsable resource chooser
 * @author tseytlin
 */
public interface EntryChooser {
	public static int SINGLE_SELECTION = 1;
	public static int MULTIPLE_SELECTION = 2;

	/**
	 * display chooser dialog for a given resource
	 */
	public void showChooserDialog();
	
	/**
	 * set selection mode SINGLE_SELECTION vs MULTIPLE_SELECTION
	 * @param mode
	 */
	public void setSelectionMode(int mode);
	
	/**
	 * set selection mode SINGLE_SELECTION vs MULTIPLE_SELECTION
	 * @param mode
	 */
	public int getSelectionMode();
	
	/**
	 * was selection made in the dialog
	 * @return
	 */
	public boolean isSelected();
	
	/**
	 * get objects that were selected 
	 * @return
	 */
	public Object [] getSelectedObjects();
	
	/**
	 * get object that was selected 
	 * @return
	 */
	public Object getSelectedObject();
	
	/**
	 * set owner component
	 * @param frame
	 */
	public void setOwner(Frame frame);
	
}
