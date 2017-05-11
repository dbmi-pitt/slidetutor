package edu.pitt.dbmi.tutor.messages;


import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import edu.pitt.dbmi.tutor.model.TutorModule;

/**
 * this objects is used to notify all registered listeners of messages that are
 * being sent around
 * 
 * @author Administrator
 */
public class MessageSender implements Runnable {
	private Set<TutorModule> modules;
	private static MessageSender instance;
	private LinkedList<Message> messageQueue;
	private boolean stop = false;

	/**
	 * initialize message sender
	 */
	private MessageSender() {
		modules = new LinkedHashSet<TutorModule>();
		messageQueue = new LinkedList<Message>();
		start();
	}

	/**
	 * get a singleton instance of the message sender class
	 * 
	 * @return
	 */
	public static MessageSender getInstance() {
		if (instance == null) {
			instance = new MessageSender();
		}
		return instance;
	}

	/**
	 * add tutor module to a list of recipients
	 * 
	 * @param m
	 */
	public void addRecipient(TutorModule m) {
		modules.add(m);
	}

	/**
	 * add tutor module to a list of recipients
	 * 
	 * @param m
	 */
	public void removeRecipient(TutorModule m) {
		modules.remove(m);
	}

	/**
	 * add tutor module to a list of recipients
	 * 
	 * @param m
	 */
	public void removeAllRecipients() {
		modules.clear();
	}

	/**
	 * send message to all recipients
	 * 
	 * @param msg
	 */
	public void sendMessage(Message msg) {
		/*
		for (TutorModule m : modules) {
			if (!m.equals(msg.getSender())) {
				m.receiveMessage(msg);
			}
		}
		*/
		synchronized (messageQueue) {
            messageQueue.addLast(msg);
            messageQueue.notify();
        }
	}

	// keep sending messages in the background
	public void run() {
		while (!stop) {
			Message message = null;

			synchronized (messageQueue) {
				if (!messageQueue.isEmpty()) {
					message = messageQueue.removeFirst();
				} else {
					try {
						messageQueue.wait();
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}

			// I decided to take this out of synchronized code to increase
			// performance
			if (message != null) {
				for (TutorModule m : modules) {
					if (!m.equals(message.getSender())) {
						m.receiveMessage(message);
					}
				}
			}
		}
	}

	// stop this thread
	public void stop() {
		synchronized (messageQueue) {
			messageQueue.notifyAll();
		}
		stop = true;
	}
	
	/**
	 * start message sender
	 */
	public void start(){
		stop();
		(new Thread(this)).start();
	}
	
	 /**
     * end current session
     */
    public void flush(){
        // flush message pipe
        try{
            while(true){
                synchronized(messageQueue){
                    if(messageQueue.isEmpty()){
                        Thread.sleep(500); // wait for server to catchup
                        break;
                    }
                }
                Thread.sleep(50);
            }
        }catch(Exception ex){
            ex.printStackTrace();   
        }
    }
}
