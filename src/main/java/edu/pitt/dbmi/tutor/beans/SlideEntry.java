package edu.pitt.dbmi.tutor.beans;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.*;
import java.util.regex.*;

import javax.swing.Icon;
import javax.swing.JList;


//import edu.pitt.pathtutor.pointer.Pointer;
import edu.pitt.slideviewer.ImageTransform;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.Viewer;
//import edu.pitt.ontology.*;

public class SlideEntry implements Serializable, Icon, Comparable, Runnable {
	private static final int HISTORY_COUNT = 4;
	private static final int iconOffset = 4;
	private static final Dimension iconSize = new Dimension(115,55);
	//private static final String prf = "T:";
	private String text, slideName, stain, block,path,server,type, level,part;
	private int id;
	private boolean loaded, primarySlide, loadedSlide;
	private Dimension imageSize;
	private transient Image thumbnail;
	private transient Image [] history;
	private Point historyOffset;
	private Color [] historyColor;
	private float [] historyShade;
	private ViewPosition viewPosition, initialPosition;
	private final Font font = new Font("Sans",Font.BOLD,11);
	//private final Color shadeColor = new Color(150,200,150,75);
	private final Color flashColor = new Color(200,200,150,150);
	private boolean flashing,flash;
	private Object  immuno;
	private ImageTransform imageTransform;
	private transient Component component;
	private transient java.util.List<ShapeEntry> annotations = new ArrayList<ShapeEntry>();
	//private transient java.util.List pointers = new ArrayList();
	
	public SlideEntry(String name){
		slideName = name;
		
		// strip path if it is part of the image name
		int i = slideName.lastIndexOf("/");
		if(i < 0){
			i = slideName.lastIndexOf('\\');
		}
		
		// if path, then strip it
		if(i > -1){
			setPath(slideName.substring(0,i));
			slideName = slideName.substring(i+1);
		}
		
		// parse slides
		parseSlideName(slideName);
	}

	
	/**
	 * guess some settigns from the slide name
	 */
	private void parseSlideName(String name){
		//		 gues some of the settings
    	////////////////////////////
    	//guess stain
    	String ptrn = "[A-Z]+_\\d+_([A-Z0-9]+)(_\\w+)?\\.[A-Za-z]+";
    	Pattern p = Pattern.compile(ptrn);
    	Matcher m = p.matcher(name);
    	// part strings
    	String stain = null;
    	String other = null;
    	String level = null;
    	String block = null;
    	String part = null;
    	//Integer lvl = null;
    	//Integer prt = null;
    	
    	if(m.matches()){
    		stain = m.group(1);
    		other = m.group(2);
    	}
    	// get stain
    	if(stain != null){
    		this.stain = stain;
    	}
    	
    	//  get level block and part
    	if(other != null){
    		// get level
        	p = Pattern.compile("_L(\\d+)(_\\w+)?");
        	m = p.matcher(other);
        	
        	String block_part = null;
        	// if matches, we have level and maybe block_part
        	// else check if we just have block_part
        	if(m.matches()){
        		level = m.group(1);
        		block_part = m.group(2);
        	}else if(other.matches("_\\w+")){
        		block_part = other;
        	}
    		
        	//parse block_part
        	if(block_part != null){
        		p = Pattern.compile("_(\\d+)?([A-Za-z]+)");
        		m = p.matcher(block_part);
        		if(m.matches()){
        			part = m.group(1);
        			block = m.group(2);
        		}
        	}
        	
        	if(level != null){
        		//lvl = new Integer(level);
        		//this.level = lvl.intValue();
        		this.level = level;
        	}
        	if(part != null){
        		//prt = new Integer(part);
        		//this.part = prt.intValue();
        		this.part = part;
        	}
        	if(block != null){
        		this.block = block;
        	}
    	}
	}
	
	
	
	/**
	 * dispose of resources
	 */
	public void dispose(){
		setFlash(false);
	}
	
	/**
	 * height of icon
	 * @return
	 */
	public int getIconHeight(){
		return iconSize.height+iconOffset*2;
	}
	
	/**
	 * height of icon
	 * @return
	 */
	public int getIconWidth(){
		return iconSize.width+iconOffset*2;
	}
	
	public ImageTransform getImageTransform() {
		if(imageTransform == null)
			imageTransform = new ImageTransform();
		return imageTransform;
	}


	public void setImageTransform(ImageTransform imageTransform) {
		this.imageTransform = imageTransform;
	}

	
	/**
	 * create slide entry from map
	 * @param slide
	 * @return
	 */
	public static SlideEntry createSlideEntry(Map info){
		// get info out of slides
		String name = ""+info.get("slide_name");
		String stain = (String) info.get("has_stain");
		String part =  ""+ info.get("has_part");
		String block = (String) info.get("has_block");
		String level = ""+ info.get("has_level");
		Boolean ps = new Boolean(""+info.get("is_primary_slide"));
		Boolean ls = new Boolean(""+info.get("is_loaded_slide"));
        Integer xst = (Integer) info.get("xstart");
        Integer yst = (Integer) info.get("ystart");
        Integer scl = (Integer) info.get("init_scale");
		String immuno = (String) info.get("has_immuno");
		
        //create slide entry
		SlideEntry se = new SlideEntry(name);
		// set all fields that are set
		se.setStain(stain);
		se.setBlock(block);
        se.setImmuno(immuno);
		se.setLevel((level.length() > 0 && !level.equals("null"))?level:null);
        se.setPart((part.length() > 0 && !part.equals("null"))?part:null);
        se.setPrimarySlide((ps != null)?ps.booleanValue():false);
        se.setLoadedSlide((ls != null)?ls.booleanValue():false);
        if(xst != null && yst != null && scl != null){
        	int x = xst.intValue();
        	int y = yst.intValue();
        	double s = ((double)scl.intValue())/100;
        	se.setInitialPosition(new ViewPosition(x,y,s));
        }
		
		return se;	
	}

	
	/**
	 * @return the block
	 */
	public String getBlock() {
		return block;
	}
	/**
	 * @param block the block to set
	 */
	public void setBlock(String block) {
		this.block = block;
	}
	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}
	/**
	 * Is slide currently loaded
	 * @return the loaded
	 */
	public boolean isOpened() {
		return loaded;
	}
	/**
	 * @param loaded the loaded to set
	 */
	public void setOpened(boolean loaded) {
		this.loaded = loaded;
	}
	
	/**
	 * is slide currently being viewed
	 * @param v
	 * @return
	 */
	public boolean isCurrentlyOpen(Viewer v){
		return getImageName(slideName).equals(getImageName(v.getImage()));
	}
	
	/**
	 * generate name
	 * @return
	 */
	private String createName(){
		//		System.out.println(info);
		// create a name for a slide
		StringBuffer buff = new StringBuffer();
	
		if(part  != null)
			buff.append(part);
		if(block != null)
			buff.append(block);
		if(stain != null)
			buff.append(((buff.length() > 0)?" ":"")+stain);
		if(level != null)
			buff.append(" L"+level);
		
		// default behaviour if everything else is null
		if(buff.length() == 0)
			buff.append(slideName);
		
		return buff.toString();
	}
	
	
	/**
	 * @return the name
	 */
	public String getText() {
		// compose unique name if name is unavailable
		if(text == null)
			text = createName();
		return text;
	}

	/**
	 * @param name the name to set
	 */
	public void setText(String name) {
		this.text = name;
	}
	/**
	 * @return the part
	 */
	public String getPart() {
		return part;
	}
	/**
	 * @param part the part to set
	 */
	public void setPart(String part) {
		this.part = part;
	}
	/**
	 * @return the slideName
	 */
	public String getSlideName() {
		return slideName;
	}
	
	
	/**
	 * @return the slideName
	 */
	public String getName() {
		return slideName;
	}
	
	/**
	 * @param slideName the slideName to set
	 */
	public void setName(String name) {
		this.slideName = name;
	}
	
	
	/**
	 * @param slideName the slideName to set
	 */
	public void setSlideName(String slideName) {
		this.slideName = slideName;
	}
	/**
	 * @return the stain
	 */
	public String getStain() {
		//if(stain == null)
		//	stain = "HE";
		return stain;
	}
	/**
	 * @param stain the stain to set
	 */
	public void setStain(String stain) {
		this.stain = stain;
	}
	/**
	 * @return the thumbnail
	 */
	public Image getThumbnail() {
		return thumbnail;
	}
	
	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(Image thumb) {
		BufferedImage img = new BufferedImage(iconSize.width,iconSize.height,BufferedImage.TYPE_INT_RGB);
		// figure out offset
		int bw = img.getWidth();
		int bh = img.getHeight();
		int tw = thumb.getWidth(null);
		int th = thumb.getHeight(null);
		
		// image offset coordinages
		int w = bw;
		int h = bh;
		
		//adjust width or height
		if(((double)bw/bh) < ((double)tw/th)){
			h = (int)(((double)w*th)/tw);
		}else{
			w = (int)(((double)h*tw)/th);
		}
		
		// set origin
		int x = (w < bw)?bw-w:0;
		int y = 0;
		
		// draw image on buffer
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(240,240,250));
		g.fillRect(0,0,bw,bh);
		g.drawImage(thumb,x,y,w,h,Color.white,null);
		g.setColor(Color.black);
		g.drawRect(0,0, bw-1, bh-1);
		
		this.thumbnail = img;
		
		// create history images
		initHistoryImages(x,y,w,h);
	}
	/**
	 * @return the viewPosition
	 */
	public ViewPosition getViewPosition() {
		return viewPosition;
	}
	
	/**
	 * @param viewPosition the viewPosition to set
	 */
	public void setViewPosition(ViewPosition viewPosition) {
		this.viewPosition = viewPosition;
	}
	
	/**
	 * return slideName 
	 */
	public String toString(){
		return slideName;
	}

	/**
	 * @return the loadedSlide
	 */
	public boolean isLoadedSlide() {
		return loadedSlide || primarySlide;
	}

	/**
	 * @param loadedSlide the loadedSlide to set
	 */
	public void setLoadedSlide(boolean loadedSlide) {
		this.loadedSlide = loadedSlide;
	}

	/**
	 * @return the primarySlide
	 */
	public boolean isPrimarySlide() {
		return primarySlide;
	}

	/**
	 * @param primarySlide the primarySlide to set
	 */
	public void setPrimarySlide(boolean primarySlide) {
		this.primarySlide = primarySlide;
	}
	
	/**
	 * paint this icon
	 */
	public void paintIcon(Component c, Graphics g, int x, int y){
		this.component = c;
		int w = iconSize.width;
		int h = iconSize.height;
		if(thumbnail != null){
			g.drawImage(thumbnail,x+iconOffset,y+iconOffset,w,h,Color.white,null);
		}else{
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(x+iconOffset,y+iconOffset,w,h);
		}
		
		// draw viewed shade or flash
		if(flash){
			g.setColor(flashColor);
			g.fillRect(x+iconOffset,y+iconOffset,w,h);
		}else if(loaded && !flashing){
			//draw viewed shade
			//g.setColor(shadeColor);
			//g.fillRect(x+iconOffset,y+iconOffset,w,h);
			if(historyOffset != null && history != null){
				int hx = x+iconOffset+historyOffset.x;
				int hy = y+iconOffset+historyOffset.y;
				for(int i=0;i<history.length;i++){
					g.drawImage(history[i],hx,hy,null);
				}
			}
		}
		
		// paint text on top
		paintText(c,g,getText(),x+iconOffset+3,y+iconOffset+h+6);
	}
	
	
	 /* paint */
	private void paintText(Component c, Graphics g, String text, int xo, int yo) {
		Graphics2D g2 = (Graphics2D) g;
		// Enable antialiasing for text
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		char[] chars = text.toCharArray();

		FontMetrics fm = c.getFontMetrics(font);
		int h = yo - fm.getAscent();
		g.setFont(font);

		for (int i=0,x=xo,w=0; i < chars.length; i++, x+=w) {
			char ch = chars[i];
			w = fm.charWidth(ch);

			g.setColor(Color.white);
			g.drawString("" + chars[i], x - 1, h + 1);
			g.drawString("" + chars[i], x + 1, h + 1);
			g.drawString("" + chars[i], x, h - 1);
			g.drawString("" + chars[i], x - 1, h - 1);
			g.setColor(Color.black);
			g.drawString(""+chars[i],x,h);
		}
	}
	
	/**
	 * @return the initialPosition
	 */
	public ViewPosition getInitialPosition() {
		return initialPosition;
	}

	/**
	 * @param initialPosition the initialPosition to set
	 */
	public void setInitialPosition(ViewPosition initialPosition) {
		this.initialPosition = initialPosition;
	}
	
	
	/**
	 * @return the immuno
	 */
	public Object getImmuno() {
		return immuno;
	}

	/**
	 * @param immuno the immuno to set
	 */
	public void setImmuno(Object immuno) {
		this.immuno = immuno;
	}

	
	/**
	 * compare this slide to other
	 */
	public int compareTo(Object obj){
		return toString().compareTo(obj.toString());
	}
	
	/**
	 * here for flashing purposes
	 */
	public void run(){
		Component comp = getParent(component);
		while(flashing){
			flash = !flash;
			try{
				Thread.sleep(200);
			}catch(InterruptedException ex){}
			
			if(comp != null){
				comp.repaint();
			}else{
				comp = getParent(component);
			}
		}
	}
	
	/**
	 * get parent component of this entry
	 * @param c
	 * @return
	 */
	private Component getParent(Component c){
		if(c == null)
			return null;
		if(c instanceof JList)
			return c;
		return getParent(c.getParent());
	}
	
	
	/**
	 * start/stop flashing of a thumbnail
	 * @param b
	 */
	public void setFlash(boolean b){
		// start new thread if not started yet
		if(b && !flashing){
			flashing = b;
			(new Thread(this)).start();
		}
		// set flashing
		flashing = b;
		flash = b;
	}
	
	/**
	 * add view position to history
	 * @param vp
	 */
	public void addViewPosition(ViewPosition vp){
        // get appropriate image
		int index = getHistoryImageIndex(vp.scale);
		if(index > -1){
			Image img   = history[index];
			Color color = historyColor[index];
			float shade = historyShade[index];
			// draw viewport
	        Dimension idim = getImageSize();
	        Rectangle r = vp.getRectangle();
	        int w = (int)(r.width*img.getWidth(null)/idim.width);
	        int h = (int)(r.height*img.getHeight(null)/idim.height);
	        int x = (int)(r.x * img.getWidth(null)/idim.width);
	        int y = (int)(r.y * img.getHeight(null)/idim.height);
	        Rectangle rect = new Rectangle(x,y,w,h);
	      
	        // draw  on mold
	        Graphics2D mg = (Graphics2D) img.getGraphics();
	        mg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,shade));
	        mg.setColor(color);
	        mg.fillRect(rect.x,rect.y,rect.width,rect.height);
	        
	        // update view window
	        Component comp = getParent(component);
	        if(comp != null)
	        	comp.repaint();
	   }
	}
	
	/**
	 * get history image
	 * @param scale
	 */
	private int getHistoryImageIndex(double scl){
	  	if(history == null)
	  		return -1;
		if(scl >= .5)
    		return history.length - 1;
    	else if(scl >= .2)
    		return history.length - 2;
    	else if(scl >= .1)
    		return history.length - 3;
    	else if(scl >= .05)
    		return history.length - 4;
	  	return -1;
	}
	
	/**
	 * initialize history images
	 * @param w
	 * @param h
	 */
	private void initHistoryImages(int x, int y, int w, int h){
		Image [] img = new Image [HISTORY_COUNT];
		Color [] clr = new Color [img.length];
		float [] shd = new float [img.length];
		for(int i=0;i<img.length;i++){
			img[i] = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
			//clr[i] = new Color(50,255,0,20*(i+1));
			clr[i] = Color.green;
			shd[i] = 0.1f*(i+1);
		}
		this.history = img;
		this.historyShade = shd;
		this.historyColor = clr;
		this.historyOffset = new Point(x,y);
	}

	/**
	 * @return the imageSize
	 */
	public Dimension getImageSize() {
		return imageSize;
	}

	/**
	 * @param imageSize the imageSize to set
	 */
	public void setImageSize(Dimension imageSize) {
		this.imageSize = imageSize;
	}
	
	/**
	 * @return the annotations
	 */
	public java.util.List<ShapeEntry> getAnnotations() {
		return (annotations != null)?annotations:Collections.EMPTY_LIST;
	}
	
	/**
	 * add annotation to slide
	 * @param tm
	 */
	public void addAnnotation(ShapeEntry tm){
		if(annotations == null)
			annotations = new ArrayList<ShapeEntry>();
		if(!annotations.contains(tm))
			annotations.add(tm);
		tm.setSlide(this);
	}
	
	/**
	 * add annotation to slide
	 * @param tm
	 */
	public void removeAnnotation(ShapeEntry tm){
		if(annotations == null)
			annotations = new ArrayList<ShapeEntry>();
		annotations.remove(tm);
	}
	
	/**
	 * get image name from image path
	 * @param name
	 * @return
	 */
	public static String getImageName(String name){
		if(name == null)
			return null;
		
		// check forward slash
		int i = name.lastIndexOf("/");
		// check backslash
		if(i < 0)
			i = name.lastIndexOf("\\");
		// if found get substring
		if(i > -1 && i<name.length()-1)
			return name.substring(i+1);
		else
			return name;
	}
	
	/**
	 * get properties representation of this object
	 * @return
	 */
	public Properties getProperties(){
		Properties p = new Properties();
		p.setProperty("slide.name",slideName);
		p.setProperty("is.primary",""+primarySlide);
		p.setProperty("stain",""+stain);
		p.setProperty("block",""+block);
		p.setProperty("level",""+level);
		p.setProperty("part",""+part);
		p.setProperty("server",(server != null)?server:"");
		p.setProperty("path",(path != null)?path:"");
		return p;
	}
	
	/**
	 * set properties representation of this object
	 * @return
	 */
	public void setProperties(Properties p){
		try{
			slideName = p.getProperty("slide.name",slideName);
			primarySlide = Boolean.parseBoolean(p.getProperty("is.primary"));
			stain = getValue(p,"stain");
			block = getValue(p,"block");
			//level = Integer.parseInt(p.getProperty("level",""+level));
			//part  = Integer.parseInt(p.getProperty("part",""+part));
			level = getValue(p,"level");
			part  = getValue(p,"part");
			server = getValue(p,"server");
			path = getValue(p,"path");
			
			ImageTransform it = getImageTransform();
			it.setProperties(p);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private String getValue(Properties p, String key){
		String v = p.getProperty(key);
		return (v != null && v.length() > 0 && !v.equals("null"))?v:null;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	public String getSlidePath(){
		return (path != null && path.length() > 0)?path+"/"+slideName:slideName;
	}
	
}
