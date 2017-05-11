package edu.pitt.dbmi.tutor.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * concept entry that combines several more specific entries
 * @author tseytlin
 *
 */
public class GeneralConceptEntry extends ConceptEntry {
	private List<ConceptEntry> children;
	private List<String> locations;
	private Map<String,String> examples;
	private boolean tooGeneral,incorrect;
	
	

	public GeneralConceptEntry(String name, String type){
		super(name,type);
	}
	
	
	public List<String> getLocations() {
		if(locations == null){
			locations = new ArrayList<String>();
			for(ConceptEntry e: getChildren()){
				locations.addAll(e.getLocations());
			}
		}
		return locations;
	}
	
	
	

	public Map<String, String> getExamples() {
		if(examples == null){
			examples = new HashMap<String, String>();
			for(ConceptEntry e: getChildren()){
				examples.putAll(e.getExamples());
			}
		}
		return examples;
	}


	public List<ConceptEntry> getChildren(){
		if(children == null)
			children = new ArrayList<ConceptEntry>();
		return children;
	}
	
	/**
	 * get list of children as text
	 * @return
	 */
	public String getChildrenText(){
		StringBuffer s = new StringBuffer();
		List<ConceptEntry> list = getChildren();
		String p = "";
		for(int i=0;i<list.size();i++){
			if(i > 0){
				p = ", ";
				if(i == (list.size()-1))
					p = "</b> or <b>";
			}
			s.append(p+list.get(i).getText());
		}
		return s.toString();
	}
	
	public void addChild(ConceptEntry e){
		getChildren().add(e);
		locations = null;
	}
	
	public void removeChild(ConceptEntry e){
		getChildren().remove(e);
		locations = null;
	}

	public boolean isIncorrect() {
		return incorrect;
	}


	public void setIncorrect(boolean incorrect) {
		this.incorrect = incorrect;
	}


	public boolean isTooGeneral() {
		return tooGeneral;
	}


	public void setTooGeneral(boolean tooGeneral) {
		this.tooGeneral = tooGeneral;
	}

}
