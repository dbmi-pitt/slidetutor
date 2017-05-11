package edu.pitt.dbmi.tutor.modules.reasoning;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.*;

import edu.pitt.dbmi.tutor.ITS;
import edu.pitt.dbmi.tutor.beans.*;
import edu.pitt.dbmi.tutor.model.Tutor;
import edu.pitt.dbmi.tutor.model.TutorModule;
import edu.pitt.dbmi.tutor.modules.presentation.SimpleViewerPanel;
import edu.pitt.dbmi.tutor.modules.presentation.ViewerPanel;
import edu.pitt.dbmi.tutor.util.Config;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.ViewerHelper;


/**
 * prognostic reasoner helper class to process actions
 * @author tseytlin
 *
 */
public class ActionProcessor {
	private CaseEntry caseEntry;
	private String currentSlide;
	private ViewPosition currentView;
	private List<ConceptEntry> observeSome, measureRuler,observeSlide;
	private Map<ConceptEntry,List<Rectangle>> observeAll, measureHPF, measureSquare;
	private List<ActionEntry> processedActions = new ArrayList<ActionEntry>();
	private double pixelSize;
	
	//defaults
	private double RULER_LENGTH_TOLERANCE = .3;
	private int    RULER_ANGLE_TOLERANCE  = 45;
	private int    RECTANGLE_MARGIN_TOLERANCE = 50;
	public  double MEASURE_HPF_THRESHOLD = .9;
	public  double MEASURE_HPF_COUNT = 10;
	
	
	// load defaults
	public void loadDefaults(TutorModule module){
		RULER_ANGLE_TOLERANCE = Config.getIntegerProperty(module,"measure.ruler.angle.tolerance");
		RULER_LENGTH_TOLERANCE = Config.getFloatProperty(module,"measure.ruler.length.tolerance");
		RECTANGLE_MARGIN_TOLERANCE = Config.getIntegerProperty(module,"measure.ruler.container.margin.tolerance");
		MEASURE_HPF_THRESHOLD = Config.getFloatProperty(module,"measure.hpf.tolerance");
		MEASURE_HPF_COUNT = Config.getFloatProperty(module,"measure.hpf.count");
	}
	
	public CaseEntry getCaseEntry() {
		return caseEntry;
	}
	public List<ConceptEntry> getObserveSome() {
		if(observeSome == null)
			observeSome = new ArrayList<ConceptEntry>();
		return observeSome;
	}
	public List<ConceptEntry> getObserveSlide() {
		if(observeSlide == null)
			observeSlide = new ArrayList<ConceptEntry>();
		return observeSlide;
	}
	public List<ConceptEntry> getMeasureRuler() {
		if(measureRuler == null)
			measureRuler = new ArrayList<ConceptEntry>();
		return measureRuler;
	}
	public Map<ConceptEntry, List<Rectangle>> getObserveAll() {
		if(observeAll == null)
			observeAll = new HashMap<ConceptEntry, List<Rectangle>>();
		return observeAll;
	}
	public Map<ConceptEntry, List<Rectangle>> getMeasureHPF() {
		if(measureHPF == null)
			measureHPF = new HashMap<ConceptEntry, List<Rectangle>>();
		return measureHPF;
	}
	
	/**
	 * pop accumulated ActionEntries that were completed
	 * @return
	 */
	public List<ActionEntry> pop(){
		List<ActionEntry> copy = processedActions;
		processedActions = new ArrayList<ActionEntry>();
		return copy;
	}
	
	/**
	 * set case entry
	 * @param caseEntry
	 */
	public void setCaseEntry(CaseEntry caseEntry) {
		this.caseEntry = caseEntry;
		
		// initialize lists for analysis
		for(ConceptEntry e : caseEntry.getConcepts(PROGNOSTIC_FEATURES).getValues()){
			for(ActionEntry a: e.getActions()){
				if(ACTION_OBSERVE_ALL.equals(a.getName())){
					getObserveAll().put(e,new LinkedList<Rectangle>());
				}else if(ACTION_OBSERVE_SOME.equals(a.getName())){
					getObserveSome().add(e);
				}else if(ACTION_OBSERVE_SLIDE.equals(a.getName())){
					getObserveSlide().add(e);
				}else if(ACTION_MEASURE_RULER.equals(a.getName())){
					getMeasureRuler().add(e);
				}else if(ACTION_MEASURE_HPF.equals(a.getName()) || ACTION_MEASURE_10HPF.equals(a.getName()) || ACTION_MEASURE_MM2.equals(a.getName())){
					getMeasureHPF().put(e,new LinkedList<Rectangle>());
				}
			}
		}
		
		// set current slide
		currentSlide = caseEntry.getPrimarySlide().getSlidePath();
	}

	public void reset(){
		observeSome = measureRuler = observeSlide = null;
		observeAll = measureHPF = measureSquare = null;
	}
	
	
	/**
	 * process view positon 
	 * @param view
	 */
	public void processView(ViewPosition vp){
		Rectangle view = vp.getRectangle();
		currentView = vp;
		
		// for all features that need to be partially observed
		for(ConceptEntry e : getObserveSome()){
			
			// skip if actions are complete
			if(e.isActionComplete())
				continue;
			
			// check feature power level
			// if observational view position is at power smaller then 
			// what feature can be observed, then skip this feature
			if(e.getPower() != null){
				if(vp.scale < ViewerHelper.convertLevelZoomToScale(e.getPower()))
					continue;
			}
			
			// now check right actions
			for(ActionEntry a: e.getActions()){
				if(ACTION_OBSERVE_SOME.equals(a.getName())){
					for(ShapeEntry location: caseEntry.getLocations(e,currentSlide)){
						Shape s = location.getShape();
						// NOW we need to observe a a shape 
						if(s.contains(view) || s.intersects(view) || view.contains(s.getBounds())){
							a.setActionComplete(true);
							location.setObserved(true);
							break;
						}
					}
				}
			}
		}
		
		
		// if this finding is observe all
		// for all features that need to be partially observed
		for(ConceptEntry e : getObserveAll().keySet()){
			
			// skip if actions are complete
			if(e.isActionComplete())
				continue;
			
			// check feature power level
			// if observational view position is at power smaller then 
			// what feature can be observed, then skip this feature
			if(e.getPower() != null){
				if(vp.scale < ViewerHelper.convertLevelZoomToScale(e.getPower()))
					continue;
			}
			
			// now check right actions
			for(ActionEntry a: e.getActions()){
				if(ACTION_OBSERVE_ALL.equals(a.getName())){
					boolean allObserved = true;
					
					// iterate over shapes
					for(ShapeEntry location: caseEntry.getLocations(e,currentSlide)){
						Shape s = location.getShape();
						
						// skip already observed shapes
						if(location.isObserved())
							continue;
						
						//  To observe ALL we must observe entirely every shape
						
						// if entire shape is visible, then it was observed, else add the view to stack
						if(view.contains(s.getBounds())){
							location.setObserved(true);
							
							// check if this location contains "involved" attribute
							// if so, mark ObserveAll as done
							if(location.getTag().toLowerCase().contains(INVOLVED)){
								a.setActionComplete(true);
								a.addError(HINT_OBSERVE_ALL_DONE);
								processedActions.add(a);
							}
						}else if(s.contains(view) || s.intersects(view)){
							
							// get all past views associated with this feature
							List<Rectangle> views =  getObserveAll().get(e);
							Rectangle union = null;
						
							// iterate over views
							if(views.size() > 0){
								for(ListIterator<Rectangle> k= views.listIterator();k.hasNext();){
									Rectangle rect = k.next();
									
									// if it happens that view contains shape, then great!
									if(rect.contains(s.getBounds())){
										union = rect;
										break;
									}
		
									// if new view is inside old view, then discard it
									if(rect.contains(view)){
										break;
									}
									// if old view is outside new view, then keep new one
									if (view.contains(rect)){
										k.set(view);
									// if new view intersects old one, then recompute view
									}else if(view.intersects(rect)){
										// produce a union of new view + old view
										union = rect.union(view);
										
										// remove old view
										k.remove();
									// else this view doesn't touch any other view
									}else{
										k.add(view);	
									}
								}
								
								// if union was done then add it to views
								if(union != null){
									// checked if union contains shape, else add it back to views
									if(union.contains(s.getBounds())){
										location.setObserved(true);
										
										// check if this location contains "involved" attribute
										// if so, mark ObserveAll as done
										if(location.getTag().toLowerCase().contains(INVOLVED)){
											a.setActionComplete(true);	
											a.addError(HINT_OBSERVE_ALL_DONE);
											processedActions.add(a);
										}
										
										views.remove(union);
									}else{
										views.add(union);
									}
								}
							}else{
								// add first view
								views.add(view);
							}
							
							
							// when we observe the first region, mark action as initialized
							if(!views.isEmpty()){
								a.setActionStarted(true);
							}
						}
						
						// all observed will become false if at least one shape was not observed
						allObserved &= location.isObserved();
					}
					
					
					// if all shapes are observed, then we are done
					if(allObserved){
						a.setActionComplete(true);
						a.addError(HINT_OBSERVE_ALL_DONE);
						processedActions.add(a);
					}
				}
			}
		}
		
		
		/////////////////////////////////////////////////////////////////////////////
		// if we are in high power, we might be looking at mitotic high power index
		if(vp.scale >= MEASURE_HPF_THRESHOLD){
			processHPF(vp);
		}
	}
	
	
	/**
	 * This method determines whether given action constitutes a goal.
	 */	
	private void processHPF(ViewPosition p){
		// Now since user will not carefully examine entire view area
		// the actual view should be a smaller rectangle withing viewing rect 
		// in this case 10% reduction
		//view.setBounds((int)(view.x+view.width*.05),(int)(view.y+view.height*.05),
		//				(int)(view.width*.9),(int)(view.height*.9));
		
		// we want to make a smallish square to make detection of mitosis slightly easier
		Rectangle view = p.getRectangle();
		
		int s = (int)(((view.width > view.height)?view.height:view.width)*.8);
		view.setBounds(view.x+(view.width-s)/2,view.y+(view.height-s)/2,s,s);
		
		
		// user is expected to spend some time looking at this view
		
		// for each HPF feature
		for(ConceptEntry feature : getMeasureHPF().keySet()){
			List<Rectangle> views = getMeasureHPF().get(feature);
			
			// see if index was already counted
			ActionEntry a = feature.getActions().get(0);
			if(a.isActionComplete())
				continue;
			
			
			// if no shapes no point in measuring anything
			if(caseEntry.getLocations(feature).length == 0){
				a.setActionComplete(true);
				continue;			
			}else if(caseEntry.getLocations(feature,currentSlide).length == 0){
				continue;
			}
			
			
			boolean inside = false;
			// check if this view is within any of the feature shapes
			for(ShapeEntry location : caseEntry.getLocations(feature,currentSlide)){
				if(location.getShape().contains(view) || location.getShape().intersects(view)){
					inside = true;
					break;
				}
			}
			
			// if view is not within shape then quit now
			if(!inside)
				break;
			
			// if it is inside see if it intersects with other views	
			boolean intersects = false;
			for(int j=0;j<views.size();j++){
				Rectangle rect = (Rectangle) views.get(j);
				if(rect.intersects(view)){
					if(a.isMeasureMM2())
						view = view.union(rect);
					else	
						intersects = true;				
					break;
				}
			}
			// if view intersects then quit now
			if(intersects)
				break;
			
			// else add to list of views 
			views.add(view);
			
			// and check index status.
			// now depending on action lets do different things
			if(a.isMeasureHPF() && views.size() >= MEASURE_HPF_COUNT){
				a.setActionComplete(true);	
				a.addError(HINT_MEASURE_HPF_DONE);
				processedActions.add(a);
			}else if(a.isMeasureMM2() && isMM2(views)){
				a.setActionComplete(true);	
				a.addError(HINT_MEASURE_MM2_DONE);
				processedActions.add(a);
			}else if(views.size() == 1){
				a.setActionStarted(true);
			}
			
		}
	}
	
	/**
	 * is measured HPF constitute a mm2
	 * @param views
	 * @return
	 */
	private boolean isMM2(List<Rectangle> views) {
		if(getPixelSize() > 0){
			double area = 0;
			for(Rectangle r : views){
				area += r.getWidth()*getPixelSize()*r.getHeight()*getPixelSize();
			}	
			if(area >= 1.0)
				return true;
		}
		return false;
	}

	/**
	 * process view positon 
	 * @param view
	 */
	public void processSlide(String slide){
		currentSlide = slide;
		for(ConceptEntry e : getObserveSlide()){
			// skip if actions are complete
			if(e.isActionComplete())
				continue;
			
			// now check right actions
			for(ActionEntry a: e.getActions()){
				if(ACTION_OBSERVE_SLIDE.equals(a.getName())){
					for(String s : e.getLocations()){
						if(slide.contains(s)){
							a.setActionComplete(true);
						}
					}
				}
			}
		}
		// reset pixel size with respec to new image
		pixelSize = 0;
	}
	
	/**
	 * process view positon 
	 * @param view
	 */
	public void processRuler(Line2D line){
		// get ruler coordinates
		int xst = (int) line.getX1();
		int yst = (int) line.getY1();
		int xen = (int) line.getX2();
		int yen = (int) line.getY2();
		//int len = (int)Math.sqrt(Math.pow(xst-xen,2)+Math.pow(yst-yen,2));
		
		// create ruler shape
		Shape ruler = new Polygon(new int [] {xst,xst+1,xen+1,xen},new int [] {yst,yst,yen,yen},4);
		
		// has any feature been really measured??
		boolean featureDetected = false;
			
		// find feature that have 'measure' action
		for(ConceptEntry feature : getMeasureRuler()){
			// check if feature was already measured
			if(feature.isActionComplete())
				continue;
			
			// int descrption variables
			boolean containerFound=false,wrongAngle = false, wrongLength = false;
			// if feature is mesurable and no container or intersected shapes were found
			for(ActionEntry a : feature.getActions()){
				if(a.isMeasureRuler()){
					// check that ruler's rectangle is withing shapes enlarged bounds
					// if ruler intersects or is contained in shape THEN
					// check angle if OFF, report angle issue
					// check size of OK the ruler within shape or an example ruler
					// if OK, then we got it, else report SIZE issue
					for(ShapeEntry location : caseEntry.getLocations(feature,currentSlide)){
						Shape r = location.getShape();
						Rectangle sr = new Rectangle(r.getBounds());
						
						// find enlarged container w/ 50 relative pixel margin
						int d = (currentView != null)?(int)(RECTANGLE_MARGIN_TOLERANCE/currentView.scale):RECTANGLE_MARGIN_TOLERANCE;
						sr.setBounds(sr.x-d,sr.y-d,sr.width+2*d,sr.height+2*d);
						
						// if enlarged container has the ruler we are on the right track
						if(sr.contains(ruler.getBounds())){
							//now make sure that we are contained or intersect the ruler itself
							if(r.contains(ruler.getBounds()) || r.intersects(ruler.getBounds())){
								containerFound = true;
								
								//calculate angle and length
								int angle = calculateAngle(xst,yst,xen,yen);
								int length = calculateLength(xst,yst,xen,yen);
								double slope = calculateSlope(xst,yst,xen,yen);
								
								
								// now that we found a container lets check angle and length
								ShapeEntry eruler = getRulerAnnotation(feature, location);
								if(eruler != null){
									int correctAngle  = calculateAngle(eruler.getXStart(),eruler.getYStart(),eruler.getXEnd(),eruler.getYEnd());
									int correctLength = calculateLength(eruler.getXStart(),eruler.getYStart(),eruler.getXEnd(),eruler.getYEnd());
									double correctSlope = calculateSlope(eruler.getXStart(),eruler.getYStart(),eruler.getXEnd(),eruler.getYEnd());
									
									// check angle
									wrongAngle = (Math.abs(correctAngle - angle) > RULER_ANGLE_TOLERANCE) || (slope > 0 ^ correctSlope > 0);
									wrongLength = (Math.abs(correctLength - length) > correctLength*RULER_LENGTH_TOLERANCE);
								
								}else if(getPixelSize() > 0){
									// if example ruler not found we can only check length against the value (I guess)
									int correctLength = (int)(feature.getNumericValue()/getPixelSize());
									wrongLength = (Math.abs(correctLength - length) > correctLength*RULER_LENGTH_TOLERANCE);
								}
								
								// we found a container, why bother searching for more?
								break;
							}
						}
					}
					
					// now analyze what we have
					if(containerFound){
						featureDetected = true;
						if(wrongAngle){
							//TODO: not that it is suboptimal, just angle is wrong
							// right tissue measured, not optimal location
							a.setActionCorrect(false);
							a.addError(ERROR_MEASURE_RULER_INCORRECT_ANGLE);
							processedActions.add(a);
						}else if(wrongLength){
							// size is wrong
							a.setActionCorrect(false);
							a.addError(ERROR_MEASURE_RULER_INCORRECT_SIZE);
							processedActions.add(a);
						}else{
							//bingo, we have a correctly measured tissue
							a.setActionComplete(true);
							a.addError(HINT_MEASURE_RULER_DONE);
							processedActions.add(a);
						}
					}else if(caseEntry.getLocations(feature).length == 0){
						// if there are no shapes then you measured it :)
						a.setActionComplete(true);
					}
				}
			}
		}
		
		// no appropriate feature was measured
		// not sure what is being measured
		if(!featureDetected){
			for(ConceptEntry e: getMeasureRuler()){
				for(ActionEntry a: e.getActions()){
					if(!a.isActionComplete() && a.isMeasureRuler()){
						a.setActionCorrect(false);
						a.addError(ERROR_MEASURE_RULER_INCORRECT_LOCATION);
						processedActions.add(a);
						break;
					}
				}
			}
		}
	}

	private double getPixelSize(){
		if(pixelSize == 0){
			for(Tutor t : ITS.getInstance().getTutors()){
				if(t.getPresentationModule() != null){
					if(t.getPresentationModule() instanceof SimpleViewerPanel){
						pixelSize = ((SimpleViewerPanel)t.getPresentationModule()).getViewer().getPixelSize();
					}else if(t.getPresentationModule() instanceof ViewerPanel){
						pixelSize = ((ViewerPanel)t.getPresentationModule()).getViewer().getPixelSize();
					}
				}
			}
		}
		return pixelSize;
	}

	/**
	 * Calculate angle of a line in degrees.
	 * @param int x1
	 * @param int y1
	 * @param int x2
	 * @param int y2
	 * @return int angle
	 */ 	
	public static int calculateAngle(int x1,int y1,int x2,int y2){	
		double length = Math.sqrt(Math.pow(y2-y1,2)+Math.pow(x2-x1,2));
		double angle = Math.asin((double)Math.abs(y2-y1)/length);
		return (int) Math.round(Math.toDegrees(angle));
	}
	
	
	/**
	 * Calculate angle of a line in degrees.
	 * @param int x1
	 * @param int y1
	 * @param int x2
	 * @param int y2
	 * @return int angle
	 */ 	
	public static double calculateSlope(int x1,int y1,int x2,int y2){	
		return ((double)(y2-y1))/((double)(x2-x1));
	}
	
	
	
	
	/**
	 * Calculate length of a line.
	 * @param int x1
	 * @param int y1
	 * @param int x2
	 * @param int y2
	 * @return int length
	 */ 	
	public static int calculateLength(int x1,int y1,int x2,int y2){
		return (int) Math.round(Math.sqrt(Math.pow(y2-y1,2)+Math.pow(x2-x1,2)));
	}
	
	
	/**
	 * try to get specific type of annotation
	 * @param feature
	 * @param type
	 * @return
	 */
	private ShapeEntry getRulerAnnotation(ConceptEntry feature, ShapeEntry location){
		// gather all the rulers
		List<ShapeEntry> rulers = new ArrayList<ShapeEntry>();
		for(ShapeEntry a: caseEntry.getExamples(feature,currentSlide)){
			if(a.getType().equalsIgnoreCase("Ruler"))
				rulers.add(a);
		}
		for(ShapeEntry a: caseEntry.getLocations(feature,currentSlide)){
			if(a.getType().equalsIgnoreCase("Ruler"))
				rulers.add(a);
		}
		// go over the rulers and see if we have something that is relevant to annotation
		Rectangle lr = location.getShape().getBounds();
		for(ShapeEntry r: rulers){
			Rectangle rr = r.getShape().getBounds();
			if(lr.contains(rr) || lr.intersects(rr)){
				return r;
			}
		}
		
		return !rulers.isEmpty()?rulers.get(0):null;
	}

	
}
