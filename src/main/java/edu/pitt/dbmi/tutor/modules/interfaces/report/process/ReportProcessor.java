/**
 * This interface define a processing resource for ReportDocument
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.tutor.modules.interfaces.report.process;

public interface ReportProcessor {
	/**
	 * Insert string at offset.(This is called after string is inserted)
	 * @param offset
	 * @param string to insert
	 * @param section where string is being inserted
	 */	
	public void insertString(int offset, String str, String section);
	/**
	 * Remove string at offset. (This is called before string is removed)
	 * @param offset
	 * @param string to remove
	 * @param section where string is being removed
	 */	
	public void removeString(int offset, String str, String section);
	/**
	 * When caret is moved without anything being typed, this method is invoked
	 * @param offset - this is new offset
	 */
	public void updateOffset(int offset);
	/**
	 * This signals the end of report editing, in case there are some files to be written too, or
	 * buffers need to be flashed
	 */	
	public void finishReport();
}
