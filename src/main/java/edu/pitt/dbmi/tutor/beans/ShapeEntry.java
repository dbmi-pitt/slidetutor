package edu.pitt.dbmi.tutor.beans;

import java.util.*;
import java.io.Serializable;
import java.awt.*;
import java.awt.geom.*;

import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.beans.*;
import edu.pitt.slideviewer.markers.*;
// protege stuff
//import edu.stanford.smi.protege.model.*;

public class ShapeEntry implements Serializable, Comparable {
	//private static final String prf = "T:";
	//private static final long serialVersionUID = 1874700664790753879L;
    public final static int	 CIRCLE_SHAPE	= 0;
    public final static int	 SQUARE_SHAPE	= 1;
    public final static int	 ARROW_SHAPE	= 2;
    public final static int	 POLYGON_SHAPE	= 3;
    public final static int	 CROSS_SHAPE	= 4;
    public final static int	 SIMPLE_ARROW_SHAPE= 5; //disappears under some conditions
	public final static int  RULER_SHAPE= 6; 
	public final static int PARALLELOGRAM_SHAPE  = 7;
	public final static int REGION_SHAPE  = 8;
	
	private int   viewX,viewY,xStart,yStart,xEnd,yEnd,width,height;
	private int [] xPoints,yPoints;
	private float viewZoom;
	private Color color;
	private String type,name,image;
	//private java.util.List features;
	private Set<String> tags;
	//private LexicalEntry concept;   // concept that is associated with this shape. s.a. attribute of lateral margin.
	private SlideEntry slide;
	
	// this value is set by programs
	private boolean observed;
	
	// this value is generated
	private transient Shape shape;
	private transient Annotation marker;
	private transient PolygonBean poly;
	
	public ShapeEntry(){}
	//////// creation methods //////////
	
	
	/**
	 * Create ShapeEntry from Annotation
	 */
	public static ShapeEntry createShapeEntry(Annotation tm){	
		ShapeEntry entry = new ShapeEntry();
		entry.setAnnotation(tm);
		entry.sync();
		return entry;
		
	}
	
	
	//////// utility methods /////////
	public Shape getShape(){
		// create shape		
		if(shape == null){
			if(type.equalsIgnoreCase("Polygon") || type.equalsIgnoreCase("Parallelogram")){
				shape = new Polygon(xPoints,yPoints,xPoints.length);
			}else if(type.equalsIgnoreCase("Oval") || type.equalsIgnoreCase("Circle")) {
				shape = new Ellipse2D.Double((double)xStart,(double)yStart,(double)width,(double)height);
			}else if(type.equalsIgnoreCase("Rectangle")){
				shape = new Rectangle(xStart,yStart,width,height);				
			}else if(type.equalsIgnoreCase("Arrow")){
				shape = new Line2D.Double((double)xStart,(double)yStart,(double)xEnd,(double) yEnd);	
			}else if(type.equalsIgnoreCase("Ruler")){
				shape = new Line2D.Double((double)xStart,(double)yStart,(double)xEnd,(double) yEnd);	
			}else if(type.equalsIgnoreCase("Cross")) {
				shape = new Ellipse2D.Double((double)xStart,(double)yStart,(double)width,(double)height);
			}
		}
		return shape;	
	}
	
	/**
	 * Create polygon bean from this shape
	 */
	public PolygonBean getPolygonBean(){
		if(poly == null){
			poly = new PolygonBean();	
			poly.setName(name);
			poly.setType(type);
			poly.setTag(getTag());
			poly.setZoom(""+viewZoom);
			poly.setViewX(""+viewX);
			poly.setViewY(""+viewY);
			poly.setColor(getColorName());
			poly.setXStart(""+xStart);
			poly.setYStart(""+yStart);
			poly.setWidth(""+width);
			poly.setHeight(""+height);
			poly.setXEnd(""+xEnd);
			poly.setYEnd(""+yEnd);
			poly.setXPoints(convertArrayToString(xPoints));
			poly.setYPoints(convertArrayToString(yPoints));
		}
		return poly;
	}
	
	/**
	 * get properties representation of this object
	 * @return
	 */
	public Properties getProperties(){
		sync();
		Properties p = new Properties();
		p.setProperty("name",name);
		p.setProperty("type",type);
		p.setProperty("tag",getTag());
		p.setProperty("image",getImage());
		p.setProperty("view.zoom",""+viewZoom);
		p.setProperty("view.x",""+viewX);
		p.setProperty("view.y",""+viewY);
		p.setProperty("color",UIHelper.getColor(color));
		p.setProperty("start.x",""+xStart);
		p.setProperty("start.y",""+yStart);
		p.setProperty("end.x",""+xEnd);
		p.setProperty("end.y",""+yEnd);
		p.setProperty("width",""+width);
		p.setProperty("height",""+height);
		p.setProperty("x.points",Arrays.toString(xPoints));
		p.setProperty("y.points",Arrays.toString(yPoints));
		//for(int x: xPoints)
		//	System.out.print(x+" ");
		return p;
	}
	
	/**
	 * set property values
	 * @param p
	 */
	public void setProperties(Properties p){
		try{
			setName(p.getProperty("name",name));
			setType(p.getProperty("type",type));
			setTag(p.getProperty("tag"));
			setImage(p.getProperty("image",image));
			
			viewZoom = Float.parseFloat(p.getProperty("view.zoom",""+viewZoom));
			viewX = Integer.parseInt(p.getProperty("view.x",""+viewX));
			viewY = Integer.parseInt(p.getProperty("view.y",""+viewY));
			String colorName = p.getProperty("color");
			color = UIHelper.getColor(colorName);
			xStart = Integer.parseInt(p.getProperty("start.x",""+xStart));
			yStart = Integer.parseInt(p.getProperty("start.y",""+yStart));
			xEnd = Integer.parseInt(p.getProperty("end.x",""+xEnd));
			yEnd = Integer.parseInt(p.getProperty("end.y",""+yEnd));
			width = Integer.parseInt(p.getProperty("width",""+width));
			height = Integer.parseInt(p.getProperty("height",""+height));
			String [] xp = TextHelper.parseList(p.getProperty("x.points"));
			String [] yp = TextHelper.parseList(p.getProperty("y.points"));
			xPoints = new int [xp.length];
			yPoints = new int [yp.length];
			// assert equal size
			for(int i=0;xp.length == yp.length && i<xp.length;i++){
				xPoints[i] = Integer.parseInt(xp[i]);
				yPoints[i] = Integer.parseInt(yp[i]);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * Create a tutor marker from ShapeEntry
	 */
	public Annotation getAnnotation(){
		return marker;
	}
	
	/**
	 * Create a tutor marker from ShapeEntry
	 */
	public Annotation getAnnotation(Viewer viewer){
		if(marker == null){
			// no marker manager, goodbye
			if(viewer == null)
				return null;
			
			// setup type
			int shapeType = AnnotationManager.convertType(type);
			// create marker
			Rectangle r = new Rectangle(xStart,yStart,width,height);
			marker = viewer.getAnnotationManager().createAnnotation(name,shapeType,r,getColor(),false);
			marker.setTags(tags);
			marker.setImage(image);
			marker.setViewer(viewer);
			//marker.setWasLoaded("true");
			
			// setup marker
			switch(shapeType){
				case ARROW_SHAPE:
				case RULER_SHAPE:
				  //marker.setImgXEnd(xEnd);
				  //marker.setImgYEnd(yEnd);
				  marker.addPoint(xEnd,yEnd);
				  break;
				case POLYGON_SHAPE:
				case PARALLELOGRAM_SHAPE:
				  for(int i=0;i<xPoints.length;i++){
					// marker.addImageAndViewPoint(xPoints[i],yPoints[i]);
					 marker.addVertex(xPoints[i],yPoints[i]);
				  }
				  break;
			}
			  
			//set viewer params
			//marker.setViewX(""+viewX);
			//marker.setViewY(""+viewY);
			//marker.setZoom(""+viewZoom);
			//marker.resetViewSize(width, height); //????
			marker.setViewPosition(new ViewPosition(viewX,viewY,viewZoom));
		}
		return marker;	
	}
	
	// gets a color object from string representation
	private static Color pickColor(String color){
		if(color == null)
			return null;
		
		Color c = null;
		String [] colors = new String []
		{"black","blue","cyan","darkGray","gray","green","lightGray","magenta","orange","pink","red","white","yellow"};	
		
		for(int i=0;i<colors.length;i++){
			if(color.equalsIgnoreCase(colors[i])){
				try{
					c =  (Color) Color.class.getField(colors[i]).get(null);
					break;
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
		return c;
	}	
	// converts an integer array back to string
	private static String convertArrayToString(int [] a){
		if(a != null && a.length > 0){
			StringBuffer str = new StringBuffer(""+a[0]);
			for(int i=1;i<a.length;i++){
				str.append(" "+a[i]);
			}
			return str.toString();
		}else
			return "";
	}
	
	

	/////// getters and setters //////
	public boolean isObserved(){
		return observed;	
	}
	public void setObserved(boolean b){
		observed = b;	
	}
	public String getName(){
		return name;
	}
	public void setName(String n){
		name = n;
	}
	public String getType(){
		return type;
	}
	
	
	public void setType(String t){
		type = t;
	}
	
	/**
	 * get one big tag
	 * @return
	 */
	public String getTag(){
		String s = getTags().toString();
		// remove paranthesis
		return s.substring(1,s.length()-1);
	}
	
	/**
	 * get a set of tags that were derived from a single tag
	 * @return
	 */
	public Set<String> getTags(){
		/*
		List<String> list = new ArrayList<String>();
		for(String s : tag.split(",")){
			s = s.trim();
			if(s.length() > 0)
				list.add(s);
		}
		return list.toArray(new String [0]);
		*/
		if(tags == null)
			tags = new LinkedHashSet<String>();
		return tags;
	}
	
	
	public void addTag(String t){
		/*
		if(tag == null || tag.length() == 0){
			setTag(t);
		}else if(tag.indexOf(t) < 0){
			tag = tag+", "+t;
			setTag(tag);
		}
		*/
		getTags().add(t);
		if(marker != null)
			marker.addTag(t);
	}
	public void removeTag(String t){
		/*
		tag = tag.replaceAll(t,"").replaceAll(",\\s?,",",");
		tag = tag.trim();
		if(tag.startsWith(","))
			tag = tag.substring(1).trim();
		if(tag.endsWith(","))
			tag = tag.substring(0,tag.length()-1).trim();
		setTag(tag);
		*/
		getTags().remove(t);
		if(marker != null)
			marker.removeTag(t);
	}
	
	public void setTag(String t){
		if(t == null)
			return;
		tags = new LinkedHashSet<String>();
		Collections.addAll(tags,t.split("\\s*,\\s*"));
		if(marker != null)
			marker.setTags(tags);
	}
	public void setShape(Shape s){
		shape = s;	
	}
	public Color getColor(){
		if(color == null){
			switch(AnnotationManager.convertType(type)){
				case CROSS_SHAPE:
					color = Color.yellow; break;
				case RULER_SHAPE:
					color = Color.orange; break;
				case POLYGON_SHAPE:
					color = Color.blue; break;	
				default:
					color = Color.green;
			}
		}
		return color;	
	}
	public void setColor(Color c){
		color = c;	
	}
	public int getViewX(){
		return viewX;
	}
	public void setViewX(int x){
		viewX = x;	
	}
	public int getViewY(){
		return viewY;
	}
	public void setViewY(int y){
		viewY = y;	
	}
	public float getViewZoom(){
		return viewZoom;
	}
	public void setViewZoom(float z){
		viewZoom = z;	
	}
	public int getXStart(){
		return xStart;
	}
	public void setXStart(int x){
		xStart = x;	
	}
	public int getYStart(){
		return yStart;
	}
	public void setYStart(int y){
		yStart = y;	
	}
	public int getXEnd(){
		return xEnd;
	}
	public void setXEnd(int x){
		xEnd = x;	
	}
	public int getYEnd(){
		return yEnd;
	}
	public void setYEnd(int y){
		yEnd = y;	
	}
	public int getWidth(){
		return width;	
	}
	public void setWidth(int w){
		width = w;	
	}
	public int getHeight(){
		return height;	
	}
	public void setHeight(int h){
		height = h;
	}
	//public void setConcept(LexicalEntry c){
	////	concept = c;	
	//}
	public String getColorName(){
		return UIHelper.getColor(getColor());
	}
	public void setColorName(String colorName){
		this.color = UIHelper.getColor(colorName);
	}
	//public LexicalEntry getConcept(){
	//	return concept;
	//}
	public int getArea(){
		return height*width;	
	}
	/*
	public java.util.List getFeatures(){
		return features;	
	}
	public void setFeatures(java.util.List f){
		features = f;	
	}
	*/
	//public void addFeature(DataEntry e){
	//	if(features == null)
	//		features = new ArrayList();
	//	features.add(e);
	//}
	public boolean isRectangle(){
		return type.equalsIgnoreCase("Rectangle") || type.equalsIgnoreCase("Parallelogram");
	}

	public boolean isArrow(){
		return type.equals("Arrow");
	}
	
	/**
	 * a shape is a location if it can have an area. Ex: circle, rectangle, polygon, 
	 * NOT arrow, cross or ruler
	 * @return
	 */
	public boolean isLocation(){
		return Arrays.asList("Rectangle","Parallelogram","Polygon","Oval","Circle","Region").contains(getType());
	}
	/**
	 * @return the slide
	 */
	public SlideEntry getSlide() {
		return slide;
	}

	/**
	 * @param slide the slide to set
	 */
	public void setSlide(SlideEntry slide) {
		this.slide = slide;
	}

	/**
	 * @return the image
	 */
	public String getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(String img) {
		image = img;
		
		// correct image name if necessary
		int i = image.lastIndexOf("/");
		if(i < 0){
			i = image.lastIndexOf('\\');
		}
		
		// if path, then strip it
		if(i > -1){
			image = image.substring(i+1);
		}
	}

	/**
	 * @return the xPoints
	 */
	public int[] getXPoints() {
		return xPoints;
	}

	/**
	 * @param points the xPoints to set
	 */
	public void setXPoints(int[] points) {
		xPoints = points;
	}
	/**
	 * @param points the xPoints to set
	 */
	public void setXPoints(String points) {
		xPoints = convertArray(points);
	}
	
	private int [] convertArray(String text){
		String [] txt = TextHelper.parseList(text);
		int [] a = new int [txt.length];
		for(int i=0;i<a.length;i++){
			a[i] = Integer.parseInt(txt[i]);
		}
		return a;
	}
	
	/**
	 * @param points the xPoints to set
	 */
	public void setYPoints(String points) {
		yPoints = convertArray(points);
	}
	
	/**
	 * @return the yPoints
	 */
	public int[] getYPoints() {
		return yPoints;
	}

	/**
	 * @param points the yPoints to set
	 */
	public void setYPoints(int[] points) {
		yPoints = points;
	}

	/**
	 * @param marker the marker to set
	 */
	public void setAnnotation(Annotation marker) {
		this.marker = marker;
	}
	
	/**
	 * display name
	 */
	public String toString(){
		return getName();
	}
	
	public boolean equals(Object obj){
		return obj.toString().equals(toString());
	}
	
	public int hashCode(){
		return toString().hashCode();
	}
	
	/**
	 * if tutor marker is available, sync its representation
	 * with this shape entry
	 */
	public void sync(){
		if(marker != null){
			setName(marker.getName());
			setType(marker.getType());
			setTag(marker.getTag());
			setImage(marker.getImage());
			ViewPosition vp = marker.getViewPosition();
			setViewZoom((float)vp.scale);
			setViewX(vp.x);
			setViewY(vp.y);
			setColor(marker.getColor());
			Rectangle r = marker.getBounds();
			Point p = marker.getEndPoint();
			setXStart(r.x);
			setYStart(r.y);
			setXEnd(p.x);
			setYEnd(p.y);
			setWidth(r.width);
			setHeight(r.height);
			if(marker.getShape() instanceof Polygon){
				Polygon poly = (Polygon) marker.getShape();
				//This causes array to contain 0 at the end
				//setXPoints(poly.xpoints);
				//setYPoints(poly.ypoints);
				xPoints  = new int [poly.npoints];
				yPoints  = new int [poly.npoints];
				System.arraycopy(poly.xpoints,0,xPoints,0,poly.npoints);
				System.arraycopy(poly.ypoints,0,yPoints,0,poly.npoints);
			}
		}
	}
	public int compareTo(Object a) {
		return toString().compareTo(""+a);
	}
	
	/**
	 * return a center position of a shape
	 * if shape is a polygon this will use 
	 * polygon's centroid
	 * the scale is shape's scale factor
	 * @return
	 */
	public ViewPosition getCenterPosition(){
		Shape s = getShape();
		int x = (int) s.getBounds().getCenterX();
		int y = (int) s.getBounds().getCenterY();
		float z = getViewZoom();
		if(s instanceof Polygon){
			PolygonUtils p = new PolygonUtils((Polygon)s);
			Point c = p.centroid();
			x = c.x;
			y = c.y;
		}
		return new ViewPosition(x,y,z);
	}
	
	
	/**
	 * return a center position of a shape
	 * if shape is a polygon this will use 
	 * polygon's centroid
	 * the scale is shape's scale factor
	 * @return
	 */
	public ViewPosition getCenterPosition(Viewer viewer){
		ViewPosition p = getCenterPosition();
		
		boolean arrow = getType().equalsIgnoreCase("arrow");	
		
		// calculate the best zoom
		Dimension d = viewer.getSize();
		Rectangle r = getShape().getBounds();
		
		
		// dow something special for arrows
		if(arrow){
			p.x = getXStart();
			p.y = getYStart();
			r.width  = r.width *2;
			r.height = r.height *2;
		}
		
		double zw = d.getWidth()/r.width;
		double zh = d.getHeight()/r.height;
		double z = Math.min(zw,zh);
		
		// adjust based on what is valid
		if(viewer.getScalePolicy() != null){
			z = viewer.getScalePolicy().getValidScale(z);
		}
		
		p.scale = z;
		
	
		
		return p;
	}
	
}
