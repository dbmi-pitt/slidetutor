package edu.pitt.dbmi.tutor.modules.presentation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.*;

import javax.swing.*;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.Action;
import edu.pitt.dbmi.tutor.beans.CaseEntry;
import edu.pitt.dbmi.tutor.beans.ConceptEntry;
import edu.pitt.dbmi.tutor.beans.Operation;
import edu.pitt.dbmi.tutor.beans.ShapeEntry;
import edu.pitt.dbmi.tutor.beans.SlideEntry;
import edu.pitt.dbmi.tutor.messages.ClientEvent;
import edu.pitt.dbmi.tutor.messages.Communicator;
import edu.pitt.dbmi.tutor.messages.InterfaceEvent;
import edu.pitt.dbmi.tutor.messages.Message;
import edu.pitt.dbmi.tutor.model.*;
import edu.pitt.dbmi.tutor.util.Config;
import edu.pitt.dbmi.tutor.util.TextHelper;
import edu.pitt.dbmi.tutor.util.UIHelper;
import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.AnnotationManager;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.markers.Annotation;
import edu.pitt.slideviewer.qview.QuickViewer;
import static edu.pitt.dbmi.tutor.messages.Constants.*;

/**
 * this is the basic viewer panel that supports one slide per case
 * @author tseytlin
 */
public class SimpleViewerPanel implements PresentationModule , PropertyChangeListener, ActionListener {
	private Viewer viewer; 
	private Annotation currentAnnotation;
	private Map<String,List<ShapeEntry>> annotationMap;
	private int featureMarker;
	private Properties defaultConfig;
	private boolean interactive = true;
	private Tutor tutor;
	private CaseEntry caseEntry;
	private SlideEntry slideEntry;
	private JButton info;
	private JPanel component;
	private boolean tutorMovement,useCaseServerInfo;
	
	public SimpleViewerPanel(){
		annotationMap = new HashMap<String, List<ShapeEntry>>();
	}
	
	public void load(){
	}
	
	/**
	 * get a manual page that represents this module
	 * @return
	 */
	public URL getManual(){
		return Config.getManual(getClass());
	}
	
	/**
	 * get viewer properties
	 * @return
	 */
	private Properties getViewerProperties(){
		Properties p = new Properties();
		/*
		String [] props = new String [] {
			"image.server.type","xippix.server.url","aperio.server.url",
			"openslide.server.url","xippix.image.dir","aperio.image.dir",
			"openslide.image.dir","image.list.server.url","simple.server.url","simple.image.dir"};
		for(String prop : props)
			p.setProperty(prop,Config.getProperty(this,prop));
		*/
		for(Object prop: getDefaultConfiguration().keySet()){
			p.setProperty((String)prop,Config.getProperty(this,(String)prop));
		}
		useCaseServerInfo = Config.getBooleanProperty(this,"viewer.use.case.server.info");
		return p;
	}
	
	public void dispose() {
		viewer.closeImage();
		viewer.dispose();
	}
	
	public void reconfigure(){
		if(viewer != null)
			setupViewerProperties(viewer);
	}
	
	public Component getComponent() {
		if(component == null){
			component = new JPanel();
			component.setLayout(new BorderLayout());
			
			// setup image server
			ViewerFactory.setProperties(getViewerProperties());
		
			// create virtual microscope panel
			setupViewer(QuickViewer.getViewerType(QuickViewer.APERIO_TYPE));
			
			// default feature marker
			setFeatureMarker(Config.getProperty(this,"default.feature.marker"));	
		
			
			// add debug feature
			JMenu debug = ITS.getInstance().getDebugMenu();
			if(!UIHelper.hasMenuItem(debug,"Reload Slide Image")){
				debug.addSeparator();
				debug.add(UIHelper.createMenuItem("reload.slide","Reload Slide Image",Config.getIconProperty("icon.menu.refresh"),this));
			}
		}
		return component;
	}
	

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Basic virtual slide viewer that supports one slide per case.";
	}

	public String getName() {
		return "Simple Slide Viewer";
	}

	public ImageIcon getScreenshot() {
		return Config.getScreenshot(getClass());
	}

	public String getVersion() {
		return "1.0";
	}

	public void reset() {
		if(viewer != null)
			viewer.closeImage();
		annotationMap.clear();
	}

	
	/**
	 * show clinical info
	 */
	private void doClinicalInfo(){
		UIHelper.HTMLPanel infoPanel =  new UIHelper.HTMLPanel();
		infoPanel.setPreferredSize(new Dimension(350, 300));
		infoPanel.setEditable(false);
		infoPanel.setReport(caseEntry.getClinicalInfo());
		infoPanel.setCaretPosition(0);

		// throw client event
		ClientEvent msg = ClientEvent.createClientEvent(this,TYPE_INFO,"CLINICAL INFO",ACTION_REQUEST);
		
		InterfaceEvent ie1 = InterfaceEvent.createInterfaceEvent(this,TYPE_BUTTON,info.getToolTipText(),ACTION_SELECTED);
		ie1.setClientEvent(msg);
		
		InterfaceEvent ie2 = InterfaceEvent.createInterfaceEvent(this,TYPE_DIALOG,"Clinical Case Information",ACTION_OPENED);
		ie2.setClientEvent(msg);
		
		// send two interface events and client event
		Communicator.getInstance().sendMessage(ie1);
		Communicator.getInstance().sendMessage(ie2);
		Communicator.getInstance().sendMessage(msg);
		
		
		// display info in modal dialog
		JOptionPane.showMessageDialog(Config.getMainFrame(),
		new JScrollPane(infoPanel), "Clinical Case Information",JOptionPane.PLAIN_MESSAGE);
		
		
		
		// throw interface event
		InterfaceEvent ie3 = InterfaceEvent.createInterfaceEvent(this,TYPE_DIALOG,"Clinical Case Information",ACTION_CLOSED);
		ie3.setClientEvent(msg);
		Communicator.getInstance().sendMessage(ie3);
	}
	
	
	/**
	 * Replace a current viewer w/ a viewer of different type
	 * @param type
	 */

	private void setupViewer(String type) {
		Dimension dim = Config.getDimensionProperty(this,"viewer.size");
		
		// remove previous viewer if exists
		if (viewer != null) {
			dim = viewer.getSize();
			viewer.removePropertyChangeListener(this);
			component.remove(viewer.getViewerPanel());
			viewer.dispose();
			viewer = null;
			System.gc();
		}
		
		// add new viewr
		Config.setProperty("image.server.type", type);
		
		// create virtual microscope panel
		viewer = ViewerFactory.getViewerInstance(type);
		viewer.addPropertyChangeListener(this);
		viewer.setSize(dim);
		setupViewerProperties(viewer);
		
		// add the info button
		info = UIHelper.createButton("clinical.info","View Clinical Information",
					   UIHelper.getIcon(this,"icon.toolbar.clinical.info",24),this);
		
		viewer.getViewerControlPanel().addSeparator(new Dimension(50,50));
		viewer.getViewerControlPanel().add(info);
		
		
		// add to viewer
		component.add(viewer.getViewerPanel(),BorderLayout.CENTER);
		component.revalidate();
		
		// set interactive flag
		setInteractive(interactive);
	}
	
	
	/**
	 * setup misc viewer properties
	 */
	private void setupViewerProperties(Viewer viewer){
		boolean nav = Config.getBooleanProperty(this,"viewer.navigator.enabled");
		boolean mag = Config.getBooleanProperty(this,"viewer.magnifier.enabled");
		boolean trans = Config.getBooleanProperty(this,"viewer.transforms.enabled");
		
		viewer.getViewerControlPanel().setNavigatorEnabled(nav);
		viewer.getViewerControlPanel().setMagnifierEnabled(mag);
		viewer.getViewerControlPanel().setTransformsEnabled(trans);
		
		// setup margins
		if(viewer instanceof QuickViewer){
			((QuickViewer)viewer).setMarginSize(Config.getIntegerProperty(this,"viewer.margin.size"));
		}
		
	}
	

	/**
	 * nothing from expert module needs to be loaded in this instance
	 */
	public void setExpertModule(ExpertModule module) {
		// NOOP
		
	}
	
	// return appropriate image directory
	private String getImageDir() {
		String type = Config.getProperty("image.server.type");
		if (Config.getProperty(this,type + ".image.dir") != null)
			return Config.getProperty(this,type + ".image.dir");
		return Config.getProperty(this,"image.dir");
	}
	
	/**
	 * load case information (open image)
	 */

	public void setCaseEntry(CaseEntry problem) {
		reset();
		caseEntry = problem;
			
		// get primary slide
		openImage(problem.getPrimarySlide());
	}
	
	
	/**
	 * open image
	 * @param slide
	 */
	private void openImage(SlideEntry slide){
		if(slide == null)
			return;
		slide.setOpened(true);
		
		if(slideEntry != null){
			slideEntry.setOpened(false);
			slideEntry.setViewPosition(viewer.getViewPosition());
			List<ShapeEntry> slist = new ArrayList<ShapeEntry>();
			for(Annotation a: viewer.getAnnotationManager().getAnnotations()){
				slist.add(ShapeEntry.createShapeEntry(a));
			}
			annotationMap.put(slideEntry.getSlidePath(),slist);
		}
		
		
		// check if we need to switch viewer based on image type
		String type = Config.getProperty("image.server.type");
		String rtype = ViewerFactory.recomendViewerType(slide.getSlideName());
		if (!type.equals(rtype) || checkSlideServerSettings(rtype, slide))
			setupViewer(rtype);
		
		// currently opend slide
		slideEntry = slide;
		
		// load image
		try {
			viewer.openImage(getImageDir() + slide.getSlidePath());
			//viewer.reset();
		} catch (ViewerException ex) {
			// ex.printStackTrace();
			Config.getLogger().severe(TextHelper.getErrorMessage(ex));
			JOptionPane.showMessageDialog(Config.getMainFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			
			// second attempt after the crash
			try {
				viewer.openImage(getImageDir() + slide.getSlidePath());
			
				// load transforms
				viewer.getImageProperties().setImageTransform(slide.getImageTransform());
				
			} catch (ViewerException ex2) {
				Config.getLogger().severe(TextHelper.getErrorMessage(ex2));
			}
		}
		
		//viewer.update();
		
		// load annotations
		if(annotationMap.containsKey(slideEntry.getSlidePath())){
			for(ShapeEntry s: annotationMap.get(slideEntry.getSlidePath())){
				s.setAnnotation(null);
				Annotation a = s.getAnnotation(viewer);
				viewer.getAnnotationManager().addAnnotation(a);
			}
		}
	}
	
	
	public Viewer getViewer(){
		return viewer;
	}
	
	/**
	 * check if server settings of the image are different from 
	 * the current setting
	 * @param slide
	 * @return true if settings were changed
	 */
	private boolean checkSlideServerSettings(String type, SlideEntry slide){
		if(!useCaseServerInfo)
			return false;
		
		if(!TextHelper.isEmpty(slide.getServer())){
			// if image server url doesn't equal the current setting, then
			if(!slide.getServer().equals(ViewerFactory.getProperties().getProperty(type+".server.url"))){
				ViewerFactory.getProperties().setProperty(type+".server.url",slide.getServer());
				//ViewerFactory.getProperties().setProperty(type+".image.dir",slide.getPath());
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b){
		if(component != null){
			info.setEnabled(b);
			setInteractive(b);
			component.setEnabled(b);
		}
	}
	
	
	/**
	 * set component interactive flag
	 * @param b
	 */
	public void setInteractive(boolean b){
		interactive = b;
		if(viewer != null){
			viewer.getViewerController().setNavigationEnabled(b);
			viewer.getViewerControlPanel().setEnabled(b);
		}
	}
	
	/**
	 * is interactive
	 * @return
	 */
	public boolean isInteractive(){
		return interactive;
	}
	
	/**
	 * start feature identification process.
	 * Identify "something" of value inside
	 * presentation module
	 */
	public void startFeatureIdentification(){
		AnnotationManager mm = viewer.getAnnotationPanel().getAnnotationManager();
		if(currentAnnotation != null)
			currentAnnotation.setSelected(false);
		currentAnnotation = mm.createAnnotation(featureMarker,true);
		mm.addAnnotation( currentAnnotation );
		// start sketch process
		viewer.getAnnotationPanel().sketchAnnotation(currentAnnotation );
	}
	
	public void setIdentifiedFeature(Shape s){
		AnnotationManager mm = viewer.getAnnotationPanel().getAnnotationManager();
		
		if(currentAnnotation != null)
			currentAnnotation.setSelected(false);
		currentAnnotation = null;
		
		// see if there is an existing annotation with the same bounds
		for(Annotation a: mm.getAnnotations()){
			if(a.getShape() != null && a.getShape().equals(s)){
				// we found identical annotation
				currentAnnotation = a;
				break;
			}
		}
		
		// create new annotation
		if(currentAnnotation == null){
			currentAnnotation = mm.createAnnotation(featureMarker,true);
			currentAnnotation.setBounds(s.getBounds());
			currentAnnotation.setViewPosition(viewer.getViewPosition());
			mm.addAnnotation( currentAnnotation );
		}
	}
	
	
	/**
	 * set feature marker (type of annotation) to be used during
	 * feature identification process
	 * @param str
	 */
	public void setFeatureMarker(String str){
		if(str != null && str.length() > 0){
			featureMarker = AnnotationManager.convertType(str);
		}else{
			featureMarker = AnnotationManager.CROSS_SHAPE;
		}
	}
	
	
	/**
	 * stop feature identification process.
	 * Identify "something" of value inside
	 * presentation module
	 */
	public void stopFeatureIdentification(){
		// stop sketch process
		viewer.getAnnotationPanel().sketchDone();
		viewer.getViewerComponent().setCursor(Cursor.getDefaultCursor());
		currentAnnotation = null;
	}
	
	/**
	 * get identified feature as some object.
	 * It is up to calling module to know what this
	 * object will be.
	 * @return
	 */
	public Object getIdentifiedFeature(){
		return currentAnnotation;
	}
	
	/**
	 * remove identified feature from the presentation module
	 * @param obj
	 */
	public void removeIdentifiedFeature(Object obj){
		if(obj instanceof Annotation){
			Annotation tm = (Annotation) obj;
			viewer.getAnnotationPanel().getAnnotationManager().removeAnnotation(tm);
			tm.delete();
		}
	}
	
	
	/**
	 * has the feature been identified?
	 * @return
	 */
	public boolean isFeatureIdentified(){
		return !viewer.getAnnotationPanel().isSketching();
	}
	
	public Tutor getTutor() {
		return tutor;
	}


	public void setTutor(Tutor tutor) {
		this.tutor = tutor;
	}


	/**
	 * Create a smoother animation of view changed
	 * @param delay
	 * @param target
	 */
    private void animateViewObserve(long delay, ViewPosition target){
    	ViewPosition init = viewer.getViewPosition();
    	
    	// if not the same scale, just do it, else try to do it smother
    	if(init.scale != target.scale){
    		UIHelper.sleep(delay);
    		viewer.setViewPosition(target);
    	}else{
        	final int delta = 40;
	    	int distx = target.x - init.x;
	    	int disty = target.y - init.y;
	    	int FRAME_NUM = (int)((Math.abs(distx) > Math.abs(disty))?
	    						   Math.abs(distx)/(delta/init.scale):
	    						   Math.abs(disty)/(delta/init.scale));
	    	if(FRAME_NUM < 1)
	    		FRAME_NUM = 1;
	    	
	    	int dx = distx / FRAME_NUM;
	    	int dy = disty / FRAME_NUM;
	    	
	    	// devide region into frames
	    	for(int i=0;i<FRAME_NUM;i++){
	    		UIHelper.sleep(delay/FRAME_NUM);
				
	    		// figure out delta
	    		init.x = init.x+dx;
	    		init.y = init.y+dy;
	    		viewer.setViewPosition(init);
	    	}
    	}
    }
    
   
	
	/**
	 * process recieved messages
	 */
	public void receiveMessage(Message msg) {
		if(msg.getSender() instanceof ProtocolModule){
			if(TYPE_PRESENTATION.equals(msg.getType())){
				if(ACTION_VIEW_CHANGE.equals(msg.getAction())){
					long time = 0;
					if(msg.containsKey("delay"))
						time = Long.parseLong(msg.get("delay"));
					viewer.setViewPosition(TextHelper.parseViewPosition(msg.getLabel()));
					//animateViewObserve(time,TextHelper.parseViewPosition(msg.getLabel()));
				}else if(ACTION_IDENTIFY.equals(msg.getAction())){
					Map<String,String> input = (Map<String,String>) msg.getInput();
					if(input.containsKey("location")){
						Rectangle r = TextHelper.parseRectangle(input.get("location"));
						AnnotationManager mm = viewer.getAnnotationPanel().getAnnotationManager();
						currentAnnotation = null;
						String name = UIHelper.getTextFromName(msg.getLabel());
						// check if this is a move vs create
						for(Annotation a: mm.getAnnotations()){
							if(featureMarker == AnnotationManager.convertType(a.getType()) && a.getTags().contains(name)){
								currentAnnotation = a;
								break;
							}
						}
						// create or move this annotation
						if(currentAnnotation == null){
							currentAnnotation = mm.createAnnotation(featureMarker,r,Color.yellow,true);
							currentAnnotation.setViewPosition(viewer.getViewPosition());
							mm.addAnnotation(currentAnnotation);
						}else{
							currentAnnotation.setLocation(r.getLocation());
						}
							
					}
				}
			}
		}
	}
	
	/**
	 * get all actions that this module supports
	 * @return
	 */
	public Action [] getSupportedActions(){
		return new Action [] {
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_MOVE_VIEWER,POINTER_INPUT_EXAMPLE,
						"Move virtual slide microscope to a nearest place with example annotation for a given finding."),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_MOVE_VIEWER,POINTER_INPUT_LOCATION,
						"Move virtual slide microscope to a nearest place with location annotation for a given finding."),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_EXAMPLE,
						"Show the nearest example annotation for a given finding and move virtual slide microscope to the best location to view it."),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_LOCATION,
						"Show the nearest location annotation for a given finding and move virtual slide microscope to the best location to view it."),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SHOW_ANNOTATION,POINTER_INPUT_ALL,
						"Show all annotations for a given finding"),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_CHANGE_IMAGE,"image path",
						"Load a specified digital slide into a virtual microscope"),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"true","Enable interactive mode in this module."),
				new Action(PresentationModule.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"false","Disable interactive mode in this module.")};
	}

	/**
	 * resolve an arbitrary action
	 * if action is understood, the module will
	 * "resolve" it, by assigning runnable code
	 * to it, for later execution
	 * @param action
	 */
	public void resolveAction(Action action){
		final Action act = action;
		Operation oper = null;
				
		// figure out which operations to set
		if(POINTER_ACTION_MOVE_VIEWER.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				public void run() {
					ShapeEntry shape = null;
					String in = ""+act.getInput();
					if(in.startsWith(POINTER_INPUT_LOCATION))
						shape = caseEntry.getNearestLocation(act.getConceptEntry(),viewer);
					else if(in.startsWith(POINTER_INPUT_EXAMPLE))
						shape = caseEntry.getNearestExample(act.getConceptEntry(),viewer);
					
					// move viewer
					if(shape != null){
						tutorMovement = true;
						viewer.setCenterPosition(shape.getCenterPosition(viewer));
					}
				}
				public void undo(){
					
				}
			};
		}else if(POINTER_ACTION_SHOW_ANNOTATION.equalsIgnoreCase(action.getAction())){
			//set viewer location
			oper = new Operation(){
				private List<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
				public void run() {
					if(shapes.isEmpty()){
						String in = ""+act.getInput();
						if(in.startsWith(POINTER_INPUT_LOCATION)){
							ShapeEntry s = caseEntry.getNearestLocation(act.getConceptEntry(),viewer);
							if(s != null){
								if(in.endsWith(POINTER_INPUT_SUFFIX_NOT_SEEN)){
									for(ShapeEntry e: caseEntry.getLocations(act.getConceptEntry(),viewer.getImage())){
										if(!e.isObserved()){
											shapes.add(e);
											break;
										}
									}
								}else
									shapes.add(s);
							}
						}else if(in.startsWith(POINTER_INPUT_EXAMPLE)){
							ShapeEntry s = caseEntry.getNearestExample(act.getConceptEntry(),viewer);
							if(s != null){
								if(in.endsWith(POINTER_INPUT_SUFFIX_NO_RULER)){
									if(!s.getType().equalsIgnoreCase("ruler")){
										shapes.add(s);
									}
								}else
									shapes.add(s);
							}
						}else if(in.startsWith(POINTER_INPUT_ALL)){
							if(act.getConceptEntry() != null){
								ConceptEntry e = caseEntry.getConcept(act.getConceptEntry().getName());
								if(e != null){
									Collections.addAll(shapes,caseEntry.getExamples(e,viewer.getImage()));
									Collections.addAll(shapes,caseEntry.getLocations(e,viewer.getImage()));
								}
							}else if(slideEntry != null){
								shapes = slideEntry.getAnnotations();
							}
						}else{
							ShapeEntry s = caseEntry.getNearestShape(act.getConceptEntry(),act.getInput(),viewer);
							if(s != null)
								shapes.add(s);
						}
					}
					
					// move viewer and display annotation
					if(!shapes.isEmpty()){
						for(ShapeEntry shape: shapes)
							viewer.getAnnotationManager().addAnnotation(shape.getAnnotation(viewer));
						
						if(shapes.size() == 1){
							tutorMovement = true;
							viewer.setCenterPosition(shapes.get(0).getCenterPosition(viewer));
						}
					}
				}
				public void undo(){
					for(ShapeEntry shape: shapes){
						viewer.getAnnotationManager().removeAnnotation(shape.getAnnotation());
					}			
				}
				
			};
		}else if(POINTER_ACTION_SET_INTERACTIVE.equalsIgnoreCase(action.getAction())){
			final boolean oldVal = isInteractive();
			//set viewer location
			oper = new Operation(){
				public void run() {
					setInteractive(Boolean.parseBoolean(act.getInput()));
				}
				public void undo(){
					setInteractive(oldVal);
				}
			};
		}else if(POINTER_ACTION_CHANGE_IMAGE.equalsIgnoreCase(action.getAction())){
			final String in = ""+action.getInput();
			oper = new Operation(){
				private SlideEntry previous,next;
				public void run() {
					previous = slideEntry;
					for(SlideEntry s: caseEntry.getSlides()){
						if(s.getSlidePath().equals(in)){
							next = s;
							break;
						}
					}
					openImage(next);
				}
				public void undo(){
					if(previous != null)
						openImage(previous);
				}
			};
			
		}
			
		// set operations
		action.setOperation(oper);
	}
	/**
	 * list to viewer changes
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if(AUTO_VIEW_EVENT.equals(prop)){
			tutorMovement = true;
		}else if(Constants.IMAGE_CHANGE.equals(prop) && evt.getNewValue() != null){
			// remmember slide position
			if(slideEntry != null){
				if(slideEntry.getViewPosition()  != null)
					viewer.setViewPosition(slideEntry.getViewPosition());
				else
					viewer.reset();
			}
			
			// send notifications of viewer movement
			String img  = ""+evt.getNewValue();
			
			// send notifications of viewer movement
			Dimension p = viewer.getSize();
			
			Map<String,String> map = new LinkedHashMap<String, String>();
			map.put("width",""+p.width);
			map.put("height",""+p.height);
			
			InterfaceEvent m = InterfaceEvent.createInterfaceEvent(this,TYPE_PRESENTATION,img,ACTION_IMAGE_CHANGE);
			m.setInput(map);
			Communicator.getInstance().sendMessage(m);
		}else if(Constants.VIEW_OBSERVE.equals(prop)){
			// send notifications of viewer movement
			ViewPosition p = (ViewPosition) evt.getNewValue();
			
			Map<String,String> map = new LinkedHashMap<String, String>();
			map.put("x",""+p.x);
			map.put("y",""+p.y);
			map.put("width",""+p.width);
			map.put("height",""+p.height);
			map.put("scale",""+Math.round(p.scale*100)/100.0);
			map.put("auto",""+tutorMovement);
			
			String label = "("+p.x+", "+p.y+", "+map.get("scale")+")";
			ClientEvent m = ClientEvent.createClientEvent(this,TYPE_PRESENTATION,label,ACTION_VIEW_CHANGE);
			m.setInput(map);
			Communicator.getInstance().sendMessage(m);
			tutorMovement = false;
		}else if(Constants.VIEW_RESIZE.equals(prop)){
			// send notifications of viewer movement
			Dimension p = (Dimension) evt.getNewValue();
			
			Map<String,String> map = new LinkedHashMap<String, String>();
			map.put("width",""+p.width);
			map.put("height",""+p.height);
						
			InterfaceEvent m = InterfaceEvent.createInterfaceEvent(this,TYPE_PRESENTATION,
					TextHelper.toString(map),ACTION_VIEW_RESIZE);
			m.setInput(map);
			Communicator.getInstance().sendMessage(m);
			
		}else if(Constants.UPDATE_SHAPE.equals(prop)){
		
		}else if(Constants.SKETCH_DONE.equals(prop)){
			
		}else if(Constants.NAVIGATOR.equals(prop)){
			String act  = ("OPEN".equals(evt.getNewValue()))?ACTION_OPENED:ACTION_CLOSED;
			InterfaceEvent m = InterfaceEvent.createInterfaceEvent(this,TYPE_PRESENTATION,Constants.NAVIGATOR,act);
			Communicator.getInstance().sendMessage(m);
		}else if(Constants.MAGNIFIER.equals(prop)){
			String act  = ("OPEN".equals(evt.getNewValue()))?ACTION_OPENED:ACTION_CLOSED;
			InterfaceEvent m = InterfaceEvent.createInterfaceEvent(this,TYPE_PRESENTATION,Constants.MAGNIFIER,act);
			Communicator.getInstance().sendMessage(m);
		}
		
	}	
	
	/**
	 * get all messages that this module supports
	 * @return
	 */
	public Message [] getSupportedMessages(){
		return new Message [0];
	}
	
	
	


	public void sync(PresentationModule tm) {
		// set case entry from  the interface
		getComponent();
		setCaseEntry(tutor.getCase());
	}


	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(cmd.equalsIgnoreCase("clinical.info")){
			doClinicalInfo();
		}else if(cmd.equals("reload.slide")){
			if(slideEntry != null){
				// load image
				try {
					viewer.openImage(getImageDir() + slideEntry.getSlidePath());
					//viewer.getViewerController().resetZoom();
				} catch (ViewerException ex) {
					Config.getLogger().severe(TextHelper.getErrorMessage(ex));
					JOptionPane.showMessageDialog(Config.getMainFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
				Communicator.getInstance().sendMessage(InterfaceEvent.createInterfaceEvent(this,TYPE_DEBUG,"Reload Digital Slide",ACTION_OPENED));
			}
		}
	}

	public boolean isEnabled() {
		return (component == null)?false:component.isEnabled();
	}
}
