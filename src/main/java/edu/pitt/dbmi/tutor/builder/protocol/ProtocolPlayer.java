package edu.pitt.dbmi.tutor.builder.protocol;

import java.util.List;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.modules.interfaces.report.StringPlayer;

/**
 * this class plays a bunch of events
 * @author tseytlin
 *
 */
public class ProtocolPlayer implements Runnable {
	private PlaybackPanel playbackPanel;
	private boolean stop,pause;
	private Thread thread;
	private double speed = 1.0;
	protected long lastTime = -666;
	private Object lock = "lock";
	private List<Message> messages;
	
	public ProtocolPlayer(PlaybackPanel manager, List<Message> list){
		playbackPanel = manager;
		messages = list;
	}
	
	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(double speed) {
		if(speed == 0)
			speed = -1;
		this.speed = speed;
	}


	public void slower(){
		if(speed > 0.015625){
			speed /= 2;
			StringPlayer.setSpeed(speed);
			playbackPanel.updateSpeed(speed);
			if(thread != null)
				thread.interrupt();
		}
	}
	
	public void faster(){
		if(speed < 64){
			speed *= 2;
			StringPlayer.setSpeed(speed);
			playbackPanel.updateSpeed(speed);
			if(thread != null)
				thread.interrupt();
		}
	}
	
	
	
	// destractor
	protected void finalize() throws Throwable {
		dispose();
	}
	
	/**
	 * Dispose of resources
	 *
	 */
	public void dispose(){
		stop();
	}
	
	/**
	 * Play input file
	 */
	public void play(){
		// stop prvious running Thread if any
		stop();
		stop = false;
		thread = new Thread(this);
		thread.start();
	}
	
	public void pause(boolean b){
		pause = b;
		//if(pauseButton != null)
		//	pauseButton.setSelected(pause);
		
		//	if pause is unselected
		if(!pause){
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	
	
	// do something before play
	private void playStart(){
		ITS.getInstance().setInteractive(false);
		playbackPanel.playStart();
	}
	// do something after play
	private void playStop(){
		playbackPanel.playStop();
		ITS.getInstance().setInteractive(true);
	}
	
	/**
	 * Stop current playback
	 *
	 */
	public void stop(){
		stop = true;
		synchronized (lock) {
			lock.notifyAll();
		}
		if(thread != null)
			thread.interrupt();
		/*
		if(thread != null && thread.isAlive()){
			try{
				thread.join();
			}catch(InterruptedException ex){}
		}*/
		thread = null;
	}
		
	
	/**
	 * Sleep for required number of ms if necessary
	 * @param last time
	 * @param current time 
	 */
	private void sleep(long lastTime, long time) {
		long delta = (long)((time - lastTime)/speed);
		//System.out.println("Sleeping for "+delta+" ms");
		// wait for delta ms
		if(delta > 0 && lastTime > 0){
			try{
				Thread.sleep(delta);
			}catch(InterruptedException ex){}
		}
	}
	
	/**
	 * This is where playback occurs
	 */
	public void run(){
		playStart();
		try{
			lastTime = -666;
			
			// set location slider
			if(playbackPanel.getPlaybackSlider() != null){
				playbackPanel.getPlaybackSlider().setMaximum(messages.size());
			}
			
			// iterate over entries in reader
			for(int iter=0;iter<messages.size() && !stop;iter++){
				Message msg = messages.get(iter);
				
				//	check for pause
				if(pause){
					synchronized (lock) {
						lock.wait();
					}
				}
				
				// set location slider
				if(playbackPanel.getPlaybackSlider() != null){
					playbackPanel.getPlaybackSlider().setValue(iter);
				}
				
				// process message
				processMessage(msg);
			
				
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		// notify listeners that playback is done
		playStop();	
	}
	
	/**
	 * process message
	 * @param msg
	 */
	public void processMessage(Message msg){
		//	get message time
		//Date time = new Date(msg.getTimestamp());
		long time = msg.getTimestamp();
		
		//calculate delay
		long delta = (long)((time - lastTime)/speed);
		
		// sleep the difference
		//NOTE: it was nice to do nice animation for presentation module
		// unfortunately it simply didn't work cause some modules PresentationModule would animate and take
		// it sweet time, while other modules processed those events and moved on.
		//if(!ACTION_VIEW_CHANGE.equals(msg.getAction()))
		sleep(lastTime,time);
		
		//System.out.println(msg);
		
		// send message the sender in this case is protocol module
		msg.setSender(playbackPanel.getSourceProtocolModule());
		msg.put("delay",""+delta);
		Communicator.getInstance().sendMessage(msg);
		
		// reset timer
		lastTime = time;
	}
	
}