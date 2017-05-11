package edu.pitt.dbmi.tutor.util;
import javax.swing.*;

import java.awt.*;

/**
 * This class is a collection of easter eggs just for fun.
 * @author tseytlin
 */

public class Eggs {
    private static String dinoImg = "/icons/eggs/DrTrex.gif";
    private static String dinoRun = "/icons/eggs/dino";
    private static String nerdDoctor = "/icons/eggs/protege_md.gif";
    private static String nerdTeacher = "/icons/eggs/protege_nerd.gif";
    private static int dinoNum = 5;
    private static String DR_TREX = "[Dd]r\\.?\\s?[Tt]rex";
    private static String DINO_RUN = "dino(saur)? (run|race)";
    private static String CURSES = ".*\\b(fuck|shit|dick)\\b.*";
    private static String NERDS = "revenge of the nerds?";
    private static String [] messages = new String [] 
    {"If you feel frustrated with the system there is a 'Send Comment' button.",
     "Cursing is just not nice.","Yeah, like you can do better.",
     "Get back to work and stop playing with me!"};
    private static int messageCounter;
    private static JDialog dialog;
    private static boolean eggRunning;
    
    
    /**
     * Process input string and determine whether anything
     * should be activated
     * @param str
     */
    public static void processText(String str){
       if(str.matches(DR_TREX)){
           dinoRun((JFrame)Config.getMainFrame());
       }else if(str.matches(CURSES)){
           dontCurse((JFrame)Config.getMainFrame());
       }else if(str.matches(DINO_RUN)){
           dinoRun2((JFrame)Config.getMainFrame());
       }else if(str.matches(NERDS)){
           nerdRun((JFrame)Config.getMainFrame());
       }
    }
    
    // dino that is running
    synchronized static void dontCurse(JFrame frame){
        if(messageCounter<messages.length && (dialog == null || !dialog.isShowing())){
           int type = 0;
           switch(messageCounter){
               case(0): type = JOptionPane.QUESTION_MESSAGE;break;
               case(1): type = JOptionPane.INFORMATION_MESSAGE;break;
               case(2): type = JOptionPane.WARNING_MESSAGE;break;
               case(3): type = JOptionPane.ERROR_MESSAGE;break;
           }
           JOptionPane p = new JOptionPane(messages[messageCounter],type);
           dialog = p.createDialog(frame,"ReportTutor");
           dialog.setModal(false);
           dialog.setVisible(true);
           messageCounter ++;
        }
    }
    
    // dino that is running
    public static void dinoRun(JFrame frame){
       	final Dimension size = frame.getSize();
        if(frame == null || eggRunning)
            return;
        ImageIcon img = new ImageIcon(Eggs.class.getResource(dinoImg));
        final JLabel lbl = new JLabel(img);
        if(frame.getGlassPane() instanceof JPanel){
            // add label to glass panel
            final JPanel panel = (JPanel) frame.getGlassPane();
            final boolean vis = panel.isVisible();
                     
            // move label around
           (new Thread(new Runnable(){
                public void run(){
                	eggRunning = true;
                	panel.add(lbl);
                    int yi = size.height-lbl.getPreferredSize().height;
                    lbl.setLocation(new Point(0,yi));
                    panel.setVisible(true);
                    for(Point p=new Point(1,yi);p.x<size.width && p.y<size.height;p.x++){
                        lbl.setLocation(p);
                        try{
                            Thread.sleep(10);
                        }catch(Exception ex){
                            ex.printStackTrace();
                        }
                    }
                    panel.remove(lbl);
                    panel.setVisible(vis);
                    eggRunning = false;
                }
            })).start();
            
        }
    }
    
    // dino that is running
    public static void nerdRun(JFrame frame){
        if(frame == null)
            return;
        
        ImageIcon md = new ImageIcon(Eggs.class.getResource(nerdDoctor));
        ImageIcon tc = new ImageIcon(Eggs.class.getResource(nerdTeacher));
        // flip teacher
        Image img = UIHelper.getMirrorImage(tc.getImage());
        UIHelper.flashImage(frame, img);
        tc = new ImageIcon(img);
       
        
        runNerd(frame,md,1,1);
        runNerd(frame,tc,-1,1);
    }
    
    
    /**
     * run one nerd
     * @param p
     * @param img
     * @param x
     * @param y
     */
    private static void runNerd(JFrame frame,ImageIcon img,int x, int y){
    	if(!(frame.getGlassPane() instanceof JPanel))
    		return;
    	
    	if(eggRunning)
    		return;
    	    	
    	// init vars
    	final JPanel panel = (JPanel) frame.getGlassPane();
    	final ImageIcon image = img;
        final boolean vis = panel.isVisible();
        final int xi = x;
        final int yi = y;
        final Dimension size = frame.getSize();
        
    	 // move label around
        (new Thread(new Runnable(){
            public void run(){
            	eggRunning = true;
            	int xinc = xi,yinc = yi;
                JLabel mdLabel = new JLabel(image);
                mdLabel.setDoubleBuffered(true);
                Dimension d = new Dimension(image.getIconWidth(),image.getIconHeight());
                panel.add(mdLabel);
                mdLabel.setLocation(new Point(xi,yi));
                panel.setVisible(true);
                int x = (xinc > 0)?0:size.width-d.width;
                int y = (yinc > 0)?0:size.height-d.height;
                do {
                	mdLabel.setLocation(new Point(x,y));
                    try{
                        Thread.sleep(10);
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                    // move around
                    x = x + xinc;
                    y = y + yinc;
                    
                    // change direction
                    if(x == 0 || x+d.width == size.width){
                    	xinc = -xinc;
                    	mdLabel.setIcon(new ImageIcon(UIHelper.getMirrorImage(image.getImage())));
                    }
                    if(y == 0 || y+d.height+20 == size.height)
                    	yinc = -yinc;
                }while(y > 0);
                panel.remove(mdLabel);
                panel.setVisible(vis);
                eggRunning = false;
            }
        })).start();
    }
    
    
    // dino that is running
    public static void dinoRun2(JFrame frame){
        final Dimension size = frame.getSize();
        if(frame == null)
            return;
       
        if(eggRunning)
        	return;
        
        
        final JLabel [] lbl = new JLabel [dinoNum];
        for(int i=0;i<lbl.length;i++){
        	ImageIcon img = new ImageIcon(Eggs.class.getResource(dinoRun+(i+1)+".gif"));
        	lbl[i] = new JLabel(img);
        	lbl[i].setDoubleBuffered(true);
        }
        
        if(frame.getGlassPane() instanceof JPanel){
            // add label to glass panel
            final JPanel panel = (JPanel) frame.getGlassPane();
            final boolean vis = panel.isVisible();
            panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
            
            // move label around
            (new Thread(new Runnable(){
                public void run(){
                   eggRunning = true;
                    
                    // load up all of the dinos
                    for(int i=0;i<lbl.length;i++){
                    	panel.add(lbl[i]);
                    }
                    panel.validate();
                    panel.setVisible(true);
                    
                    // now move all of the dinos at the same time
                    Point p = null;
                    do{
                        for(int i=0;i<lbl.length;i++){
                        	p = new Point(lbl[i].getLocation());
                        	// increment
                        	p.x+= 2+(int)(10*Math.random())%7;
	                    	
                        	lbl[i].setLocation(p);
	                        try{
	                            Thread.sleep(10);
	                        }catch(Exception ex){
	                           // ex.printStackTrace();
	                        }
                        }
                    }while(p.x<size.width && p.y<size.height);
                 
                    for(int i=0;i<lbl.length;i++){
                    	panel.remove(lbl[i]);
                    }
                    panel.setVisible(vis);
                    eggRunning = false;
                }
            })).start();
            
        }
    }
}
