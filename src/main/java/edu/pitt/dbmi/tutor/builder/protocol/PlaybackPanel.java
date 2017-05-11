package edu.pitt.dbmi.tutor.builder.protocol;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.ProtocolModule;
import edu.pitt.dbmi.tutor.modules.protocol.ConsoleProtocolModule;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.UIHelper;

public class PlaybackPanel implements ActionListener{
	public static final String PLAY_SELECTED = "PLAY_SELECTED",RECORD_SELECTED="RECORD_SELECTED";
	public static final String RECORD_UNSELECTED="RECORD_UNSELECTED",PLAYBACK_FINISHED = "PLAYBACK_FINISHED";
	private JTextField speedTxt;
	private JLabel statusLbl;
	private JSlider locationSlider;
	private ProtocolPlayer playback;
	private JToggleButton record;
	private ProtocolModule source,target;
	private JPanel component;
	private boolean playbackVisible,recordVisible,recordEnabled,speedVisible = true;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	public boolean isRecordEnabled() {
		return recordEnabled;
	}

	public void setRecordEnabled(boolean recordEnabled) {
		if(record != null)
			record.setEnabled(recordEnabled);
	}

	public void setPlaybackVisible(boolean playbackVisible) {
		this.playbackVisible = playbackVisible;
	}
	
	public void setRecordVisible(boolean recordVisible) {
		this.recordVisible = recordVisible;
	}
	
	public boolean isSpeedVisible() {
		return speedVisible;
	}

	public void setSpeedVisible(boolean speedVisible) {
		this.speedVisible = speedVisible;
	}
	
	/**
	 * get component
	 * @return
	 */
	public JPanel getComponent(){
		if(component == null ){
			// play button
			JPanel cp = new JPanel();
			cp.setBorder(new TitledBorder("Playback Control"));
			cp.setLayout(new BorderLayout());
			
			// buttons
			JToolBar toolbar = new JToolBar();
			toolbar.setFloatable(false);
			
			// stopButton
			if(playbackVisible)
				toolbar.add(UIHelper.createButton("play","Start Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.play")),this));
			toolbar.add(UIHelper.createButton("stop","Stop Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.stop")),this));
			toolbar.add(UIHelper.createToggleButton("pause","Pause Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.pause")),this));
			if(recordVisible){
				record = UIHelper.createToggleButton("record","Start Recording",UIHelper.getIcon(Config.getProperty("icon.toolbar.record")),this);
				toolbar.add(record);
			}
			toolbar.add(Box.createHorizontalGlue());
			if(speedVisible)
				toolbar.add(UIHelper.createButton("slower","Slow Down Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.rewind")),this));
			
			// counter
			int speed = 1;
			speedTxt = new JTextField("    "+(int)speed+" X ");
			speedTxt.setEditable(false);
			speedTxt.setMaximumSize(new Dimension(50,50));
			speedTxt.setToolTipText("Playback Speed");
			if(speedVisible){
				toolbar.add(speedTxt);
				toolbar.add(UIHelper.createButton("faster","Speed Up Playback",UIHelper.getIcon(Config.getProperty("icon.toolbar.fast.forward")),this));
			}
			cp.add(toolbar,BorderLayout.NORTH);
			
			// slider
			locationSlider = new JSlider(0,1);
			locationSlider.setToolTipText("Playback Location");
			locationSlider.setValue(0);
			locationSlider.setEnabled(false);
			cp.add(locationSlider,BorderLayout.CENTER);
			
			// status
			statusLbl = new JLabel(" ");
			cp.add(statusLbl,BorderLayout.SOUTH);
			component = cp;
		}
		return component;
	}
	
	
	
	public void setPlaybackControlsEnabled(boolean b){
		for(Component c: getComponent().getComponents()){
			if(c instanceof Container)
				UIHelper.setEnabled((Container)c,b);
		}
	}
	
	// do something before play
	public void playStart(){
		setPlaybackControlsEnabled(true);
		locationSlider.setValue(0);
		statusLbl.setText("<html><font color=green>playing</font>");
		
	}
	// do something after play
	public void playStop(){
		if(!playbackVisible)
			setPlaybackControlsEnabled(false);
		statusLbl.setText(" ");
		locationSlider.setValue(locationSlider.getMaximum());
		pcs.firePropertyChange(PLAYBACK_FINISHED,null,null);
	}
	
	public void updateSpeed(double speed){
		int x = (int) speed;
		if(x >= 1){
			speedTxt.setText("  "+x+" X ");
		}else{
			x = (int) (1/speed);
			speedTxt.setText(" 1/"+x+" X ");
		}
	}
	
	public void reset(){
		if(playback != null)
			playback.stop();
		locationSlider.setValue(0);
		updateSpeed(1.0);
		statusLbl.setText(" ");
	}
	
	
	public JSlider getPlaybackSlider(){
		return locationSlider;
	}
		
	
	public void play(List<Message> list){
		playback = new ProtocolPlayer(this,list);
		playback.play();
	}
	
	public void play(ProtocolPlayer player){
		playback = player;
		playback.play();
	}
	

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("play".equals(cmd)){
			pcs.firePropertyChange(PLAY_SELECTED,null,null);
		}else if(cmd.equals("stop")){
			if(playback != null){
				playback.stop();
				playback.dispose();
				playback = null;
			}
		}else if(cmd.equals("pause")){
			if(playback != null)
				playback.pause(((AbstractButton)e.getSource()).isSelected());
		}else if(cmd.equals("slower")){
			if(playback != null)
				playback.slower();
		}else if(cmd.equals("faster")){
			if(playback != null)
				playback.faster();
		}else if(cmd.equals("record")){
			AbstractButton bt = (AbstractButton) e.getSource();
			pcs.firePropertyChange(bt.isSelected()?RECORD_SELECTED:RECORD_UNSELECTED,null,null);
			getTargetProtocolModule().setEnabled(bt.isSelected());
			statusLbl.setText(bt.isSelected()?"<html><font color=red>recording</font>":" ");
		}
	}
	
	public void setEnabled(boolean b){
		setPlaybackControlsEnabled(b);
	}
	
	
	public void setSourceProtocolModule(ProtocolModule p){
		source = p;
	}
	public ProtocolModule getSourceProtocolModule(){
		if(source == null){
			source = new ConsoleProtocolModule();
			source.setEnabled(false);
		}
		return source;
	}
	
	public void setTargetProtocolModule(ProtocolModule p){
		target = p;
	}
	public ProtocolModule getTargetProtocolModule(){
		if(target == null)
			target = new ConsoleProtocolModule();
		return target;
	}
	
}
