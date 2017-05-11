package edu.pitt.dbmi.tutor.beans;
import static edu.pitt.dbmi.tutor.messages.Constants.*;
import static edu.pitt.dbmi.tutor.util.OntologyHelper.*;
/**
 * action entry describes an action that can be performed 
 * @author tseytlin
 *
 */
public class ActionEntry extends ConceptEntry {
	private boolean actionStarted, actionComplete, actionCorrect;
	public ActionEntry(String name){
		super(name,TYPE_ACTION);
	}
	public boolean isActionStarted() {
		return actionStarted;
	}
	public void setActionStarted(boolean actionStarted) {
		this.actionStarted = actionStarted;
	}
	public boolean isActionComplete() {
		return actionComplete;
	}
	public void setActionComplete(boolean actionComplete) {
		this.actionComplete = actionComplete;
		if(actionComplete){
			actionStarted = true;
			actionCorrect = true;
		}
	}
	public boolean isActionCorrect() {
		return actionCorrect;
	}
	public void setActionCorrect(boolean actionCorrect) {
		this.actionCorrect = actionCorrect;
	}
	
	public boolean isObserveAll(){
		return ACTION_OBSERVE_ALL.equals(getName());
	}
	public boolean isObserveSome(){
		return ACTION_OBSERVE_SOME.equals(getName());
	}
	public boolean isObserveSlide(){
		return ACTION_OBSERVE_SLIDE.equals(getName());
	}
	public boolean isMeasureRuler(){
		return ACTION_MEASURE_RULER.equals(getName());
	}
	public boolean isMeasureHPF(){
		return ACTION_MEASURE_HPF.equals(getName()) || ACTION_MEASURE_10HPF.equals(getName());
	}
	public boolean isMeasureMM2(){
		return ACTION_MEASURE_MM2.equals(getName());
	}
	public boolean isMeasure(){
		return getName().startsWith("Measure");
	}
	public boolean isObserve(){
		return getName().startsWith("Observe");
	}
}
