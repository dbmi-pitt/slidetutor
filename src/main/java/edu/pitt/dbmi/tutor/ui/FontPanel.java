package edu.pitt.dbmi.tutor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class FontPanel{
	private JPanel panel;
	private JSlider slider;
	private JLabel fontLabel;
	private Font font;
	private int originalSize,currentSize;
	private static FontPanel fp;
	
	private FontPanel(){
	}
	
	/**
	 * get instance of font panel
	 * @return
	 */
	public static FontPanel getInstance(){
		if(fp == null){
			fp = new FontPanel();
		}
		return fp;
	}
	
	/**
	 * get UI component 
	 * @return
	 */
	public JComponent getComponent(){
		if(panel == null){
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
			panel.setBorder(new TitledBorder("Set Font Size"));
			fontLabel = new JLabel("The quick brown fox jumped over the lazy dog");
			fontLabel.setHorizontalAlignment(JLabel.CENTER);
			font = fontLabel.getFont();
			originalSize = fontLabel.getFont().getSize();
			slider = new JSlider(8,32,originalSize);
			slider.setMajorTickSpacing(4);
			slider.setMinorTickSpacing(1);
			slider.setSnapToTicks(true);
			slider.setPaintLabels(true);
			slider.setPaintTicks(true);
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							Font f = fontLabel.getFont();
							fontLabel.setFont(f.deriveFont((float)slider.getValue()));
						}
					});
				}
			});
			JButton reset = new JButton("Reset to Default");
			reset.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					slider.setValue(originalSize);
				}
			});
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(reset,BorderLayout.CENTER);
			
			JPanel fontPanel = new JPanel();
			fontPanel.setLayout(new BorderLayout());
			fontPanel.setBackground(Color.white);
			fontPanel.add(fontLabel,BorderLayout.CENTER);
			fontPanel.setPreferredSize(new Dimension(400,100));
			
			panel.add(fontPanel);
			panel.add(slider);
			panel.add(p);
			
			
		}
		fontLabel.setFont(font.deriveFont(slider.getValue()));
		currentSize = slider.getValue();
		return panel;
	}
	
	
	/**
	 * get delta of font size change
	 * @return
	 */
	public float getFontDelta(){
		return slider.getValue() - currentSize;
	}
	
	
	
	/**
	 * show dialog with this component
	 * @param frame
	 */
	public void showDialog(Component parent){
		int r = JOptionPane.showConfirmDialog(parent,getComponent(),"Set Font Size",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if( r == JOptionPane.OK_OPTION){
			doResize();
		}
	}
	
	/**
	 * update font
	 */
	private void doResize(){
		// set font for future components
		UIDefaults defaults = UIManager.getDefaults();
		
		// save list of keys
		Set keys = new HashSet();
		for (Enumeration e = defaults.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			// look at fonts and reset them
			if(defaults.get(key) instanceof Font)
				keys.add(key);
		}
		
		// change font
		for (Object key: keys) {
			Font f = defaults.getFont(key);
			defaults.put(key,f.deriveFont(f.getSize2D()+getFontDelta()));				
		}
	
		// set font for existing components
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				for(Window w: Window.getWindows()){
					updateFont(w);
					w.pack();
				}
			}
		});				
	}
	
	/**
	 * update fonts in container
	 * @param cont
	 */
	private void updateFont(Container cont){
		if(cont == null)
			return;
		
		// update font in cotnainer
		Font f = cont.getFont();
		cont.setFont(f.deriveFont(f.getSize2D()+getFontDelta()));
		for(int i=0;i<cont.getComponentCount();i++){
			Component c = cont.getComponent(i);
			if(c instanceof JMenu){
				updateFont((JMenu) c);
			}else if(c instanceof Container){
				updateFont((Container) c);
			}else {
				f = c.getFont();
				c.setFont(f.deriveFont(f.getSize2D()+getFontDelta()));
			}
		}
	}
	
	/**
	 * update fonts in container
	 * @param cont
	 */
	private void updateFont(JMenu cont){
		if(cont == null)
			return;
		
		// update font in cotnainer
		Font f = cont.getFont();
		cont.setFont(f.deriveFont(f.getSize2D()+getFontDelta()));
		for(int i=0;i<cont.getItemCount();i++){
			Component c = cont.getMenuComponent(i);
			if(c instanceof JMenu){
				updateFont((JMenu) c);
			}else{
				f = c.getFont();
				c.setFont(f.deriveFont(f.getSize2D()+getFontDelta()));
			}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FontPanel fp = new FontPanel();
		fp.showDialog(null);
	}
}
