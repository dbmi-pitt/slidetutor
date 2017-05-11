package edu.pitt.dbmi.tutor.messages;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.util.UIHelper;

/**
 * this objects is used to notify all registered
 * listeners of messages that are being sent around
 * @author Administrator
 */
public class Communicator  implements Runnable{
	private Set<TutorModule> modules;
	private Set<Tutor> tutors;
	private static Communicator instance;
	private LinkedList<Message> messageQueue;
	private boolean stop = false;
	
	/**
	 * initialize message sender
	 */
	private Communicator(){
		//modules = Collections.newSetFromMap(new ConcurrentHashMap<TutorModule,Boolean>());
		modules = new CopyOnWriteArraySet<TutorModule>();
		//new LinkedHashSet<TutorModule>();
		//tutors =  Collections.newSetFromMap(new ConcurrentHashMap<Tutor,Boolean>())
		tutors = new CopyOnWriteArraySet<Tutor>();
		//tutors =  Collections.newSetFromMap(new ConcurrentHashMap<Tutor,Boolean>());
		//	new LinkedHashSet<Tutor>();
		messageQueue = new LinkedList<Message>();
		start();
	}
	
	/**
	 * get a singleton instance of the message sender class
	 * @return
	 */
	public static Communicator getInstance(){
		if(instance == null){
			instance = new Communicator();
		}
		return instance;
	}
	
	/**
	 * add tutor module to a list of recipients
	 * @param m
	 */
	public void addRecipient(TutorModule m){
		modules.add(m);
	}
	
	/**
	 * add tutor module to a list of recipients
	 * @param m
	 */
	public void removeRecipient(TutorModule m){
		modules.remove(m);
	}
	
	/**
	 * get all registered modules
	 * @return
	 */
	public Set<TutorModule> getRegisteredModules(){
		return modules;
	}
	
	/**
	 * add tutor module to a list of recipients
	 * @param m
	 */
	public void addRecipient(Tutor m){
		tutors.add(m);
	}
	
	/**
	 * add tutor module to a list of recipients
	 * @param m
	 */
	public void removeRecipient(Tutor m){
		tutors.remove(m);
	}
	
	
	/**
	 * add tutor module to a list of recipients
	 * @param m
	 */
	public void removeAllRecipients(){
		modules.clear();
		tutors.clear();
	}
	
	/**
	 * send message to all recipients
	 * @param msg
	 *
	public void sendMessage(Message msg){
		for(TutorModule m: modules){
			if(!m.equals(msg.getSender())){
				m.receiveMessage(msg);
			}
		}
	}*/
	
	/**
	 * send message to all recipients
	 * 
	 * @param msg
	 */
	public void sendMessage(Message msg) {
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
		stop = false;
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
	
	
	/**
	 * resolve an action. Searches through modules
	 * finds a suitable receiver and hopes that
	 * receiver will be able to decipher action input
	 * @param action
	 */
	public void resolveAction(Action action){
		// resplve action for modules
		for(TutorModule m: modules){
			if(UIHelper.isMatchingModule(m,action.getReceiver())){
				m.resolveAction(action);
				if(action.isResolved())
					break;
			}
		}
		
		// resolve action for tutors
		for(Tutor  m: tutors){
			if(UIHelper.isMatchingTutor(m,action.getReceiver())){
				m.resolveAction(action);
				if(action.isResolved())
					break;
			}
		}
	}
}

