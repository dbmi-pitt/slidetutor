package edu.pitt.dbmi.tutor.modules.interfaces.report;


/**
 * interactively type a string
 * @author tseytlin
 */
public class StringPlayer extends Thread {
	private final int CHAR_DELAY = 100;
	private String text;
	private int offset;
	private int duration;
	private boolean fastforward; 
	private ReportDocument doc;
	private static double speed = 1.0;
	
	public StringPlayer(String str,int offs, int dur, ReportDocument doc){
		text = str;
		offset = offs;
		duration = calculateTimeOnPhrase(str, dur);
		fastforward = false;
		this.doc = doc;
	}
	
	/**
	 * set playback speed
	 * @param s
	 */
	public static void setSpeed(double s){
		speed = s;
	}
	
	public void run(){
		//time = System.currentTimeMillis();
		// break down string into chars
		int timeOnPhrase = 0;
		for(int i=0,j=0;i<text.length();i++){
			char c = text.charAt(i);
			
			// check for newline
			if(c == '\\' && (i+1)<text.length() && text.charAt(i+1) == 'n'){
				c = '\n';
				i++;
			}
			
			timeOnPhrase += CHAR_DELAY;
			if(c == '<'){
				remove(offset+--j);
			}else{
				append(c,offset+j++);
				// pause between phrases
				if(isEndOfPhrase(c) && !fastforward){
					int delay = duration - timeOnPhrase;
					if(delay > 0 && i < text.length()-1){
						try{
							Thread.sleep((long)(delay/speed));
						}catch(InterruptedException ex){}
					}
				}
			}
		}
	}
	
	
	/**
	 * return true if end of phrase char
	 * @param c
	 * @return
	 */
	private boolean isEndOfPhrase(char c){
		return (""+c).matches("[\\.!\\?,\\n;:\\r]");
	}
	
	
	
	/**
	 * calculate average time on each phrase
	 * @param duration
	 * @return
	 */
	private int calculateTimeOnPhrase(String text, int dur){
		// get number of phrases in a given string
		char [] c = text.toCharArray();
		int j = 1;
		for(int i=0;i< c.length;i++){
			if(isEndOfPhrase(c[i]))
				j++;
		}
		// now we have j that is the number of phrases
		return dur/j;
	}
	
	
	/**
	 * flush remining text.
	 */
	public void flush(){
		fastforward = true;
	}
	
	
	/**
	 * append char to document
	 * @param c
	 */
	private void append(char c,int offs){
		try{
			doc.insertString(offs,""+c,null);
			//textPane.setCaretPosition(offs+1);
			if(!fastforward)
				Thread.sleep((long)(CHAR_DELAY/speed));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	/**
	 * remove last char from document
	 * @param c
	 */
	private void remove(int offs){
		try{
			doc.remove(offs,1);
			//textPane.setCaretPosition(offs-1);
			if(!fastforward)
				Thread.sleep((long)(CHAR_DELAY/speed));
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}