package edu.pitt.dbmi.tutor.modules.presentation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import edu.pitt.slideviewer.ImageProperties;
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
public class ViewerPanel implements PresentationModule , PropertyChangeListener, ActionListener {
	private JPanel viewerPanel;
	private Component component;
	private Viewer viewer; 
	private Annotation currentAnnotation;
	private int featureMarker;
	private Properties defaultConfig;
	private boolean interactive;
	private Tutor tutor;
	private CaseEntry caseEntry;
	private SlideEntry slideEntry;
	private JButton info;
	private JList slideSelector;
	private JPanel slideSelectorPanel;
	private JButton orderLevel, orderStain;
	private final Dimension pico = new Dimension(0,0);
	private boolean slideRequestEnabled;
	private boolean tutorMovement,useCaseServerInfo;
	private Map<String,List<ShapeEntry>> annotationMap;
	private SlideOrder slideOrder;
	//private String [] slideStains;
	
	public ViewerPanel(){
		annotationMap = new HashMap<String, List<ShapeEntry>>();
	}
	
	public void load(){
		//NOOP:
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
			"openslide.image.dir","image.list.server.url"};
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
		//TODO:
		if(viewer != null)
			setupViewerProperties(viewer);
	}
	
	public Component getComponent() {
		if(component == null){
			viewerPanel = new JPanel();
			viewerPanel.setLayout(new BorderLayout());
			
			// setup image server
			ViewerFactory.setProperties(getViewerProperties());
		
			// create virtual microscope panel
			setupViewer(QuickViewer.getViewerType(QuickViewer.APERIO_TYPE));
			
			// default feature marker
			setFeatureMarker(Config.getProperty(this,"default.feature.marker"));
			
			slideRequestEnabled = Config.getBooleanProperty(this,"viewer.slide.request.enabled");
			
			// setup component
			if(Config.getBooleanProperty(this,"viewer.slide.selector.enabled")){
				JSplitPane split = new JSplitPane();
				split.setOneTouchExpandable(true);
				int size = Config.getIntegerProperty(this,"viewer.slide.selector.size");
				
				// get position
				String position = Config.getProperty(this,"viewer.slide.selector.position");
				if("top".equals(position)){
					split.setOrientation(JSplitPane.VERTICAL_SPLIT);
					split.setTopComponent(getSlideSelector(false));
					split.setBottomComponent(viewerPanel);
					split.setDividerLocation(size);
					split.setResizeWeight(0);
				}else if("bottom".equals(position)){
					split.setOrientation(JSplitPane.VERTICAL_SPLIT);
					split.setTopComponent(viewerPanel);
					split.setBottomComponent(getSlideSelector(false));
					split.setResizeWeight(1);
					split.setDividerLocation(split.getPreferredSize().height-size);
				}else if("right".equals(position)){
					split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
					split.setLeftComponent(viewerPanel);
					split.setRightComponent(getSlideSelector(false));
					split.setResizeWeight(1);
					split.setDividerLocation(split.getPreferredSize().width-size);
				}else{
					split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
					split.setLeftComponent(getSlideSelector(true));
					split.setRightComponent(viewerPanel);
					split.setResizeWeight(0);
					split.setDividerLocation(size);
				}
				component = split;
			}else{
				component = viewerPanel;
			}
			
			
			// add debug feature
			JMenu debug = ITS.getInstance().getDebugMenu();
			if(!UIHelper.hasMenuItem(debug,"Reload Slide Image")){
				debug.addSeparator();
				debug.add(UIHelper.createMenuItem("reload.slide","Reload Slide Image",null,this));
			}
			
		}
		return component;
	}
	
	public Viewer getViewer(){
		return viewer;
	}
	
	/**
	 * create instance of slide selector
	 * @return
	 */
	private JComponent getSlideSelector(boolean horizontal){
		if(slideSelectorPanel == null){
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			//create slide list
			slideSelector = new JList(new DefaultListModel());
			slideSelector.setMinimumSize(pico);
			//slideSelector.setCellRenderer(new IconListCellRenderer(slideSelector));
			slideSelector.addListSelectionListener(new ListSelectionListener(){
				public void valueChanged(ListSelectionEvent e){
					if(!e.getValueIsAdjusting()){
						// close slide
						if(slideEntry != null && viewer.hasImage()){
							slideEntry.setOpened(false);
							slideEntry.setViewPosition(viewer.getViewPosition());
						}
						
						// open new slide
						openImage((SlideEntry) slideSelector.getSelectedValue());
					}
				}
			});
			if(!horizontal){
				slideSelector.setVisibleRowCount(1);
				slideSelector.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			}
			
			JScrollPane scroll = new JScrollPane(slideSelector);
			scroll.setMinimumSize(pico);
			
			// create order buttons
			//JToolBar tb = new JToolBar(JToolBar.VERTICAL);
			//tb.setFloatable(false);
			orderLevel = new JButton("Order Level",UIHelper.getIcon(this,"icon.toolbar.order.level"));
			orderStain = new JButton("Order Stain",UIHelper.getIcon(this,"icon.toolbar.order.stain"));
			orderLevel.addActionListener(this);
			orderStain.addActionListener(this);
			JPanel tb = new JPanel();
			tb.setLayout(new GridLayout(0,1));
			tb.add(orderLevel);
			tb.add(orderStain);
			orderLevel.setMinimumSize(pico);
			orderStain.setMinimumSize(pico);
			//tb.setMaximumSize(orderLevel.getPreferredSize());
			//slideSelector.setMaximumSize(orderLevel.getPreferredSize());
			panel.add(scroll,BorderLayout.CENTER);
			
			// do we want buttons???
			if(slideRequestEnabled)
				panel.add(tb,(horizontal)?BorderLayout.SOUTH:BorderLayout.EAST);
			slideSelectorPanel = panel;
		}
		return slideSelectorPanel;
	}
	

	public Properties getDefaultConfiguration() {
		if(defaultConfig == null){
			defaultConfig = Config.getDefaultConfiguration(getClass());
		}
		return defaultConfig;
	}

	public String getDescription() {
		return "Virtual slide viewer that supports multiple slides per case.";
	}

	public String getName() {
		return "Virtual Slide Viewer";
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
		if(slideSelector != null){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					DefaultListModel model = (DefaultListModel) slideSelector.getModel();
					model.removeAllElements();
					slideSelector.validate();
					slideSelector.repaint();
				}
			});
		}
		if(slideOrder != null){
			if(slideOrder != null){
				slideOrder.removePropertyChangeListener(this);
				slideOrder = null;
			}
		}
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

		// display info in modal dialog
		JOptionPane.showMessageDialog(Config.getMainFrame(),
		new JScrollPane(infoPanel), "Clinical Case Information",JOptionPane.PLAIN_MESSAGE);
		
		// throw client event
		ClientEvent msg = ClientEvent.createClientEvent(this,TYPE_INFO,"CLINICAL INFO",ACTION_REQUEST);
		Communicator.getInstance().sendMessage(msg);
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
			viewerPanel.remove(viewer.getViewerPanel());
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
		viewerPanel.add(viewer.getViewerPanel(),BorderLayout.CENTER);
		viewerPanel.revalidate();
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
		
		//load first slide
		openImage(problem.getPrimarySlide());
		
		// if we have a slide selector, then
		if(slideSelector != null){
			// load slides
			if(slideRequestEnabled){
				// init slide order
				slideOrder = new SlideOrder(problem.getSlides());
				slideOrder.addPropertyChangeListener(this);
			}else{
				// if no slide slector show all slides
				for(SlideEntry s: problem.getSlides()){
					s.setLoadedSlide(true);
				}
			}
			
			// load all available slides
			loadImages();
			/*
			final DefaultListModel model = (DefaultListModel) slideSelector.getModel();
			
			// if slide selector is disable, load all slides, else just the primary
			if(slideRequestEnabled){
				final SlideEntry slide = problem.getPrimarySlide();
				
				// open slide first to save on thumbnail info
				openImage(slide);
				
				if(slide.getThumbnail() == null){
					slide.setThumbnail(viewer.getImageProperties().getThumbnail());
					slide.setImageSize(viewer.getImageSize());
				}
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						model.addElement(slide);
					}
				});		
				
				// init slide order
				slideOrder = new SlideOrder(problem.getSlides());
				slideOrder.addPropertyChangeListener(this);
				//slideOrder.setSlideStains(slideStains);
				
				slideSelector.setSelectedValue(problem.getPrimarySlide(),true);
				
			}else{
				for(SlideEntry slide : problem.getSlides()){
					if(slide.getThumbnail() == null){
						try{
							// switch server properties if appropriate, also force viewer to be reset
							// be resetting type property
							if(checkSlideServerSettings(ViewerFactory.recomendViewerType(slide.getSlideName()),slide)){
								Config.setProperty("image.server.type","");
							}
							ImageProperties im = ViewerFactory.getImageProperties(slide.getSlidePath());
							if(im != null && im.getThumbnail() != null){
								slide.setThumbnail(im.getThumbnail());
								slide.setImageSize(im.getImageSize());
							}
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}
					final SlideEntry s = slide;
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							model.addElement(s);
							if(s.isPrimarySlide())
								slideSelector.setSelectedValue(s,true);
						}
					});
					
				}
			}
			*/
		}else
			openImage(problem.getPrimarySlide());
	}

	
	private void loadImages(){
		final DefaultListModel model = (DefaultListModel) slideSelector.getModel();
		for(SlideEntry slide : caseEntry.getSlides()){
			final SlideEntry s = slide;
			if(s.isLoadedSlide() || s.isPrimarySlide() && !model.contains(s)){
				if(slide.getThumbnail() == null){
					try{
						// switch server properties if appropriate, also force viewer to be reset
						// be resetting type property
						if(checkSlideServerSettings(ViewerFactory.recomendViewerType(slide.getSlideName()),slide)){
							Config.setProperty("image.server.type","");
						}
						ImageProperties im = ViewerFactory.getImageProperties(slide.getSlidePath());
						if(im != null && im.getThumbnail() != null){
							slide.setThumbnail(im.getThumbnail());
							slide.setImageSize(im.getImageSize());
						}
					}catch(Exception ex){
						Config.getLogger().severe(TextHelper.getErrorMessage(ex));
					}
				}
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						model.addElement(s);
						if(s.isPrimarySlide())
							slideSelector.setSelectedValue(s,true);
					}
				});
			}	
		}
	}
	
	/**
	 * open image
	 * @param slide
	 */
	private void openImage(SlideEntry slide){
		if(slide == null)
			return;
		slide.setOpened(true);
		
		// close slide
		if(slideEntry != null && viewer.hasImage()){
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
			} catch (ViewerException ex2) {
				Config.getLogger().severe(TextHelper.getErrorMessage(ex2));
			}
		}
		
		// load transforms
		viewer.getImageProperties().setImageTransform(slide.getImageTransform());
		
		// load annotations
		if(annotationMap.containsKey(slideEntry.getSlidePath())){
			for(ShapeEntry s: annotationMap.get(slideEntry.getSlidePath())){
				s.setAnnotation(null);
				Annotation a = s.getAnnotation(viewer);
				viewer.getAnnotationManager().addAnnotation(a);
			}
		}
	}

	/**
	 * enable/disable component
	 * @param b
	 */
	public void setEnabled(boolean b){
		if(info != null)
			info.setEnabled(b);
		if(orderLevel != null)
			orderLevel.setEnabled(b);
		if(orderStain != null)
			orderStain.setEnabled(b);
		setInteractive(b);
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
			
		}else if (evt.getPropertyName().equals(SlideOrder.ORDER_DONE)){
        	loadImages();
        }else if (evt.getPropertyName().equals(SlideOrder.ORDER_REQUEST)){
        	Map req = (Map) evt.getNewValue();
        	String label = req.get("block")+" : "+req.get("stain")+" : "+req.get("level");
			ClientEvent m = ClientEvent.createClientEvent(this,TYPE_PRESENTATION,label,ACTION_IMAGE_CHANGE);
			m.setInput(req);
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
				new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"true","Enable interactive mode in this module."),
				new Action(Tutor.class.getSimpleName(),POINTER_ACTION_SET_INTERACTIVE,"false","Disable interactive mode in this module.")};
	}


	public void sync(PresentationModule tm) {
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
			}
		}else if(e.getSource() == orderLevel){
			if(slideOrder != null)
				slideOrder.showOrderLevel(Config.getMainFrame());
		}else if(e.getSource() == orderStain){
			if(slideOrder != null)
				slideOrder.showOrderStain(Config.getMainFrame());
		}
	}
	
	public static void main(String [] args){
		ViewerPanel p = new ViewerPanel();
		JOptionPane.showMessageDialog(null,p.getComponent());
	}

	public boolean isEnabled() {
		return info.isEnabled();
	}
}
